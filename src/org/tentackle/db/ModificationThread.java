/**
 * Tentackle - a framework for java desktop applications
 * Copyright (C) 2001-2008 Harald Krake, harald@krake.de, +49 7722 9508-0
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// $Id: ModificationThread.java 470 2009-07-31 07:14:05Z svn $

package org.tentackle.db;

import org.tentackle.db.rmi.ModificationThreadRemoteDelegate;
import java.awt.EventQueue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.tentackle.util.Compare;


/**
 * Thread to monitor the modification table.<br>
 * Runs on a separate db connection and invokes runnables if modifications are detected.
 *
 * @author harald
 */
public class ModificationThread extends Thread {
  
  private Db                      db;                 // db for "modification"-table and all Runnables
  private long                    mseconds;           // number of milliseconds to pause between checks
  private long                    masterSerial;       // last master serial for ID 0
  private List<ModificationEntry> entries;            // list of modification entries
  private List<Runnable>          runnables;          // extra runnables to execute on each poll
  private List<Runnable>          msRunnables;        // extra runnables invoked if master-Serial changed
  private boolean                 stop;               // true if stop requested
  private List<Runnable>          runOnce;            // runnables to be run once in modification thread
  private List<Runnable>          sdRunnables;        // runnables to be run once when thread is stopped
  private boolean                 idle;               // true if thread should "idle", i.e. do nothing
  private boolean                 cloneDb;            // true if thread should run on a cloned db (default)
  private boolean                 dummy;              // true if this is a dummy modthread
  
  // statement IDs
  private static int  selectIdStatementId;              // prepared statement id for getting ID for a tablename
  private static int  selectTableNameStatementId;       // prepared statement id for getting tablename for an ID
  private static int  selectMasterSerialStatementId;    // prepared statement id for getting the master-Serial
  private static int  selectAllSerialStatementId;       // prepared statement id for reading serials of all tables
  
  
  private static ModificationThread thread = new ModificationThread();  // single modthread, initially the dummy thread
  
  /**
   * Creates a modification thread as a singleton.
   * 
   * @param db the database connection
   * @param mseconds polling interval in milliseconds
   * @param cloneDb true if clone db connection on start
   * @return the thread (singleton)
   * @throws DbRuntimeException if already created and was not the dummy thread
   */
  public static synchronized ModificationThread createThread(Db db, long mseconds, boolean cloneDb) {
    
    if (thread.isDummy())  {
      ModificationThread oldThread = thread;
      thread = new ModificationThread(db, mseconds, cloneDb);
      // take over all runnables from dummy thread and log that
      for (ModificationEntry entry: oldThread.entries)  {
        if (entry.runnable != null) {
          thread.registerTable(entry.tableName, entry.runnable);
        }
        else if (entry.serialRunnable != null)  {
          thread.registerTable(entry.tableName, entry.serialRunnable);
        }
      }
    }
    else  {
      DbGlobal.errorHandler.severe(db, null, "modification thread already created");
    }
    return thread;
  }
  
  
  /**
   * Creates a modification thread as a singleton
   * with a cloned db connection.
   * 
   * @param db the database connection
   * @param mseconds polling interval in milliseconds
   * @return the thread (singleton)
   * @throws DbRuntimeException if already created and was not the dummy thread
   */
  public static ModificationThread createThread(Db db, long mseconds)  {
    return createThread(db, mseconds, true);
  }
  
  
  /**
   * Gets the single modification thread.<br>
   * This is the method applications should refer to the modthread.
   *  
   * @return the modification thread
   */
  public static synchronized ModificationThread getThread() {
    return thread;
  }
  
  
  /**
   * Creates a modification thread.
   * 
   * @param db the database connection
   * @param mseconds polling interval in milliseconds
   * @param cloneDb true if clone db connection on start
   */
  protected ModificationThread(Db db, long mseconds, boolean cloneDb)  {
    super("Tentackle Modification Daemon");
    this.db       = db;
    this.mseconds = mseconds;
    this.cloneDb  = cloneDb;
    entries       = new ArrayList<ModificationEntry>();
    runnables     = new ArrayList<Runnable>();
    msRunnables   = new ArrayList<Runnable>();
    sdRunnables   = new ArrayList<Runnable>();
    runOnce       = new ArrayList<Runnable>();
    masterSerial  = selectMasterSerial();
    setPriority(NORM_PRIORITY);
    setDaemon(true);
  }
  
  
  /**
   * Creates a dummy thread.<br>
   * Will never start and just log what would be registered
   * It is used at startup for db-classes that register runnables
   * before the real modthread is created. E.g. a table "users"
   * that must be read for authentication.
   */
  protected ModificationThread() {
    dummy        = true;
    entries      = new ArrayList<ModificationEntry>();    // to keep dummy regs!
  }
  
  
  
  /**
   * Gets the db connection for this thread.
   * 
   * @return the db connection
   */
  public Db getDb() {
    return db; 
  }
  
  /**
   * Determines whether this is the dummy thread.
   * 
   * @return true if this is the dummy thread
   * @see #ModificationThread()
   */
  public boolean isDummy()  {
    return dummy;
  }


  
  /**
   * Runs a runnable within the modification thread's context.
   * 
   * @param runnable the runnable to run
   */
  public void runOnce(Runnable runnable) {
    if (isAlive())  {
      synchronized (runOnce) {   // NOT synchronized "this" because runnables might trigger registerTable
        runOnce.add(runnable);
        interrupt();
      }
    }
    else  {
      // modthread is not running: execute in callers thread
      invokeRunOnce();  
    }
  }
  
  
  /**
   * Runs a runnable within the modification thread's context
   * and waits for completion.
   * <p>
   * Notice: if other runnables are already registered but not executed
   * so far, those runnables will be executed *before* the runnable
   * passed by this method.
   * 
   * @param runnable the runnable to run
   * @throws InterruptedException 
   */
  public void runOnceAndWait(Runnable runnable) throws InterruptedException {
    if (isAlive())  {
      synchronized (runOnce) {   // NOT synchronized "this" because runnables might trigger registerTable
        runOnce.add(runnable);
        interrupt();
        runOnce.wait();
      }
    }
    else  {
      // modthread is not running: execute in callers thread
      invokeRunOnce();
      runnable.run();
    }
  }
  
  
  /**
   * run all runnables registered in runOnce
   */
  private void invokeRunOnce()  {
    synchronized(runOnce) {
      for (Runnable r: runOnce) {
        r.run();
      }
      runOnce.clear();
      runOnce.notifyAll();
    }
  }
  
  
  
  /**
   * Request to stop the thread.
   * Applications should better use {@link #terminate} instead.
   * 
   * @see #terminate() 
   */
  public void requestToStop()  {
    stop = true;
    interrupt();
  }
  
  
  /**
   * Terminates this thread.<br>
   * Will request to stop and wait until stopped.
   * 
   * @see #requestToStop() 
   */
  public void terminate() {
    requestToStop();
    try {
      join();
    }
    catch (InterruptedException e)  {}
  }
  
  
  
  /**
   * runs the shutdown runnables
   */
  private void invokeShutdownRunnables() {
    for (Runnable r: sdRunnables) {
      r.run();
    }
  }
  
  
  
  /**
   * The workhorse.<br>
   * Clones the db connection, sets the db-group and then
   * in loop periodically {@link #invokeRunOnce} and {@link #poll}.
   * On {@link #requestToStop} closes the db connection and terminates the thread.
   */
  @Override
  public void run() {
    
    stop = false;
    
    if (!dummy) {
      
      if (cloneDb)  {
        // clone the db-connection
        Db oldDb = db;
        try {
          db = oldDb.clone();
        }
        catch (Exception ex) {
          invokeShutdownRunnables();
          throw new DbRuntimeException("cloning db failed", ex);
        }
        db.clearPassword(); // no more necessary
        
        // allocate a new db-group if not yet done.
        if (oldDb.getGroupId() == 0) {
          oldDb.setGroupId(oldDb.getConnectionId());
        }
        db.setGroupId(oldDb.getGroupId());
      }
      
      int retryCount = 0;

      while (!interrupted() && !stop) {
        
        try {
          invokeRunOnce();

          try {
            if (!isIdle()) {
              // poll and invoke runnables
              poll();
            }
            sleep (mseconds);
            retryCount = 0;   // clear retries
          }
          catch (InterruptedException e)  { // sleep interrupted
            if (stop) {
              break;
            }
            // continue ...
          }
        }
        catch (DbRuntimeException ex) {
          if (DbGlobal.serverDb == db) {
            // if in server mode: try to recover
            retryCount++;
            if (retryCount > 10) {
              DbGlobal.logger.severe("!!! Excessive database errors! (last was " + ex.getMessage() + 
                                     "). Terminating ModificationThread !!!");
              // shutdown the server, no chance to recover
              invokeShutdownRunnables();
              throw ex;
            }
          }
          else  {
            DbGlobal.logger.severe("!!! Database error: " + ex.getMessage() +
                                   ". Terminating ModificationThread !!!");
            // shutdown, no chance to recover
            invokeShutdownRunnables();
            throw ex;
          }
        }
        catch (Exception ex) {
          // other exceptions
          DbGlobal.logger.severe("!!! Severe error: " + ex.getMessage() + 
                                 ". Terminating ModificationThread !!!");
          // shutdown, no chance to recover
          invokeShutdownRunnables();
          break; 
        }
      }
      
      // stopped
      
      if (cloneDb)  {
        // close cloned db-connection
        db.close();
        db = null;
      }
    }
  }
    
  
  
  
  /**
   * Runs one poll and invokes the {@link Runnable}s if a modification is detected.
   * The implementation keeps the sync locks as short as possible.
   */
  public void poll() {
    
    // invoke the runnables first
    for (Runnable r: runnables)  {
      r.run();    // RUNS IN THE MODIFICATION-THREAD!
    }
    
    // check the master serial
    long serial = selectMasterSerial();
    
    if (serial != getMasterSerial()) {    // some table changed
      
      // create a copy (synchronized)
      long[] ids;                       // table IDs
      long[] oldSerials;                // old table serials to detect changes
      ModificationEntry[] modEntries;   // a copy of the modentries list (not the entries!)
      synchronized(this) {
        masterSerial = serial;      // update master serial
        ids          = new long[entries.size()];
        oldSerials   = new long[ids.length];
        modEntries   = new ModificationEntry[ids.length];
        Iterator<ModificationEntry> iter=entries.iterator();
        for (int i=0; i < ids.length && iter.hasNext(); i++)  {
          ModificationEntry entry = iter.next();
          ids[i]        = entry.id;
          oldSerials[i] = entry.serial;
          modEntries[i] = entry;
        }
      }
      
      // get new serials for each entry
      long[] serials = readSerials(ids);
      
      // check if serials changed
      for (int i=0; i < ids.length; i++)  {
        // update serial
        if (oldSerials[i] != serials[i])  {
          ModificationEntry entry = modEntries[i];
          synchronized(this) {    // sync. in case invoked from applic
            entry.serial = serials[i];
          }
          if (DbGlobal.logger.isFineLoggable())  {
            DbGlobal.logger.fine("modification detected for table '" + entry.tableName + "'");
          }
          if (entry.runnable != null)  {
            /** 
             * if not the first time and serial changed:
             * execute Runnable in GUI-Thread, don't use the
             * db-connection of the modification-thread!
             */
            EventQueue.invokeLater(entry.runnable);   // RUNS IN THE GUI-THREAD!
          }
          else if (entry.serialRunnable != null)  {
            entry.serialRunnable.run(db, entry.serial);   // run in my thread
          }
        }
      }
      // invoke master serial runnables
      for (Runnable r: msRunnables)  {
        r.run();    // RUNS IN THE MODIFICATION-THREAD!
      }      
    }
  }
  
  
  
  /**
   * Registers a table with a runnable that will be invoked in the GUI-Thread.
   * 
   * @param tableName is the name of the table to watch for
   * @param runnable to execute if something in table changed (invoked by EventQueue.invokeLater)
   */    
  public void registerTable(String tableName, Runnable runnable)  {
    synchronized(this)  {
      ModificationEntry entry = new ModificationEntry(tableName, runnable);
      entries.add(entry);
      if (DbGlobal.logger.isFineLoggable())  {
        if (dummy)  {
          DbGlobal.logger.fine("!!! Runnable for table '" + tableName + "' DUMMY-registered !!!");
        }
        else  {
          DbGlobal.logger.fine("Runnable for table '" + tableName + "' registered");
        }
      }
    }
  }
  
  
  /**
   * Registers a table with a runnable invoked from the ModificationThread.
   * 
   * @param tableName is the name of the table to watch for
   * @param serialRunnable to execute if something in table changed.
   */
  public void registerTable(String tableName, SerialRunnable serialRunnable)  {
    synchronized(this)  {
      ModificationEntry entry = new ModificationEntry(tableName, serialRunnable);
      entries.add(entry);
      if (DbGlobal.logger.isFineLoggable())  {
        if (dummy)  {
          DbGlobal.logger.fine("!!! SerialRunnable for table '" + tableName + "' DUMMY-registered !!!");
        }
        else  {
          DbGlobal.logger.fine("SerialRunnable for table '" + tableName + "' registered");
        }
      }
    }
  }



  /**
   * Unegisters a table with a runnable that will be invoked in the GUI-Thread.
   *
   * @param tableName is the name of the table to watch for
   * @param runnable to execute if something in table changed (invoked by EventQueue.invokeLater)
   * @return true if runnable found, false if no such runnable registered
   */
  public boolean unregisterTable(String tableName, Runnable runnable)  {
    synchronized(this)  {
      ModificationEntry entry = new ModificationEntry(tableName, runnable);
      if (entries.remove(entry)) {
        if (DbGlobal.logger.isFineLoggable())  {
          if (dummy)  {
            DbGlobal.logger.fine("!!! Runnable for table '" + tableName + "' DUMMY-unregistered !!!");
          }
          else  {
            DbGlobal.logger.fine("Runnable for table '" + tableName + "' unregistered");
          }
        }
        return true;
      }
      return false;
    }
  }


  /**
   * Unegisters a table with a runnable invoked from the ModificationThread.
   *
   * @param tableName is the name of the table to watch for
   * @param serialRunnable to execute if something in table changed.
   * @return true if runnable found, false if no such runnable registered
   */
  public boolean unregisterTable(String tableName, SerialRunnable serialRunnable)  {
    synchronized(this)  {
      ModificationEntry entry = new ModificationEntry(tableName, serialRunnable);
      if (entries.remove(entry)) {
        if (DbGlobal.logger.isFineLoggable())  {
          if (dummy)  {
            DbGlobal.logger.fine("!!! SerialRunnable for table '" + tableName + "' DUMMY-unregistered !!!");
          }
          else  {
            DbGlobal.logger.fine("SerialRunnable for table '" + tableName + "' unregistered");
          }
        }
        return true;
      }
      return false;
    }
  }


  
  
  /**
   * Registers a pure runnable to be executed in the modification thread.
   * @param runnable the runnable
   */
  public void registerRunnable(Runnable runnable)  {
    synchronized(this)  {
      runnables.add(runnable);
    }
  }
  
  
  /**
   * Registers a pure runnable to be executed if the master serial has changed
   * @param runnable the runnable
   */
  public void registerMasterSerialRunnable(Runnable runnable)  {
    synchronized(this)  {
      msRunnables.add(runnable);
    }
  }
  
  
  /**
   * Registers a pure runnable to be executed if the ModificationThread is terminated
   * due to severe errors.
   * 
   * @param runnable the runnable
   */
  public void registerShutdownRunnable(Runnable runnable)  {
    synchronized(this)  {
      sdRunnables.add(runnable);
    }
  }


  /**
   * Unregisters a pure runnable to be executed in the modification thread.
   * @param runnable the runnable
   * @return true if runnable found, false if no such runnable registered
   */
  public boolean unregisterRunnable(Runnable runnable)  {
    synchronized(this)  {
      return runnables.remove(runnable);
    }
  }


  /**
   * Unegisters a pure runnable to be executed if the master serial has changed
   * @param runnable the runnable
   * @return true if runnable found, false if no such runnable registered
   */
  public boolean unregisterMasterSerialRunnable(Runnable runnable)  {
    synchronized(this)  {
      return msRunnables.remove(runnable);
    }
  }


  /**
   * Unregisters a pure runnable to be executed if the ModificationThread is terminated
   * due to severe errors.
   *
   * @param runnable the runnable
   * @return true if runnable found, false if no such runnable registered
   */
  public boolean unregisterShutdownRunnable(Runnable runnable)  {
    synchronized(this)  {
      return sdRunnables.remove(runnable);
    }
  }

  
  
  /**
   * Gets the current master serial.
   * Used in remote connections.
   *
   * @return the current master serial
   */
  public long getMasterSerial() {
    synchronized(this) {
      db.setAlive(true);    // remote connection still alive
      return masterSerial;
    }
  }
  
  
  /**
   * Reads the master-serial from the database.
   * @return the master serial
   */
  public long selectMasterSerial() {
   long serial = getMasterSerial();
   if (db.isRemote())  {
      try {
        return ((ModificationThreadRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId())).selectMasterSerial();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote selectIdForName failed");
      }
    }
    else  {
      if (selectMasterSerialStatementId == 0)  {
        selectMasterSerialStatementId = db.prepareStatement(
                "SELECT " + DbObject.FIELD_SERIAL + " FROM " + ModificationCounter.TABLENAME +
                " WHERE " + DbObject.FIELD_ID + "=?");
      }
      PreparedStatementWrapper st = db.getPreparedStatement(selectMasterSerialStatementId);
      st.setLong(1, 0);   // ID=0 is the master
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next() == false) {
        DbGlobal.errorHandler.severe(db, null,
                "can't read master serial from " + ModificationCounter.TABLENAME);
      }
      serial = rs.getLong(1);
      rs.close();
    }
    return serial;
  }

  
  
  
  /**
   * Special runnable invoked from within the ModificationThread
   * whenever a serial on a table has changed.
   * Don't mix this up with Runnables invoked by EventQueue.invokeLater()!
   */
  public static interface SerialRunnable {
    
    /**
     * Runs some application code within the modification thread.
     * 
     * @param db the thread's db-connection
     * @param serial the new serial of the table (== class)
     */
    public void run(Db db, long serial);
    
  }
  

  
  
  /**
   * Gets all ModificationEntries (synchronized).
   * Used in remote connections.
   * 
   * @param ids the table IDs to read the serials for
   * @return the serials
   */
  public long[] getSerials(long[] ids) {
    synchronized(this) {
      long[] serials = new long[ids.length];
      for (int i=0; i < ids.length; i++) {
        serials[i] = -1;    // -1 = table not monitored
        for (ModificationEntry entry: entries) {
          if (ids[i] == entry.id) {
            // the first entry that matches is sufficient (all others have the same serial)
            serials[i] = entry.serial;
            break;
          }
        }
      }
      return serials;
    }
  }
  
  
  
  /**
   * Gets the pair of id/serial for a given tablename.
   * Used in remote connections.
   *
   * @param tableName the table to lookup
   * @return the id/serial pair for the tablename
   */
  public long[] getIdSerialForName(String tableName) {
    synchronized(this) {
      long[] idser = new long[2];
      for (ModificationEntry entry: entries) {
        if (entry.tableName.equals(tableName)) {
          // the first entry that matches is sufficient (all others have the same id/serial)
          idser[0] = entry.id;
          idser[1] = entry.serial;
          return idser;
        }
      }
    }
    // no such table: configure it
    return selectIdSerialForName(tableName);
  }
  
  
  
  
  
  /**
   * Selects the pair of id/serial for a given tablename.
   * If there is no modification entry so far in the db, it will be created.
   *
   * @param tableName the table to lookup
   * @return the id/serial pair for the tablename
   */
  public long[] selectIdSerialForName(String tableName) {
    long[] idser = new long[2];
    if (db.isRemote())  {
      try {
        // we must determine the classname to make sure it is loaded in RMI server
        DbObjectClassVariables clazzVar = DbObjectClassVariables.getVariables(tableName);
        if (clazzVar == null) {
          throw new IllegalStateException("no clazzvariables registered for " + tableName);
        }
        return ((ModificationThreadRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId())).selectIdSerialForName(clazzVar.className, tableName);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote selectIdSerialForName failed");
      }
    }
    else  {
      if (selectIdStatementId == 0) {
        selectIdStatementId = db.prepareStatement(
                "SELECT " + DbObject.FIELD_ID + "," + DbObject.FIELD_SERIAL + 
                " FROM " + ModificationCounter.TABLENAME + " WHERE " +
                ModificationCounter.FIELD_TABLENAME + "=?");
      }
      PreparedStatementWrapper st = db.getPreparedStatement(selectIdStatementId);
      st.setString(1, tableName);
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next()) {
        idser[0] = rs.getLong(1);
        idser[1] = rs.getLong(2);
      }
      else  {
        // table not configured: configure it!
        db.getModificationCounter(tableName).addModificationTable();
        // try again
        rs.close();
        // prepared statement used twice: we must attach again
        db.getConnectionManager().attach(db.getConnectionId());
        // and fake it being marked ready as if retrieved by getPreparedStatement
        st.markReady();
        rs = st.executeQuery();
        rs.next();    // must work
        idser[0] = rs.getLong(1);
        idser[1] = rs.getLong(2);       
      }
      rs.close();      
    }
    return idser;
  }
  
  
  
  /**
   * Selects the tablename for a given ID.
   * 
   * @param id the table ID
   * @return the tablename, null if the such an ID does not exist
   */
  public String selectNameForId(long id) {
    String tableName = null;
    if (db.isRemote())  {
      try {
        return ((ModificationThreadRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId())).selectNameForId(id);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote selectNameForId failed");
      }
    }
    else  {
      if (selectTableNameStatementId == 0) {
        selectTableNameStatementId = db.prepareStatement(
                "SELECT " + ModificationCounter.FIELD_TABLENAME + " FROM " + 
                ModificationCounter.TABLENAME +
                " WHERE " + DbObject.FIELD_ID + "=?");
      }
      PreparedStatementWrapper st = db.getPreparedStatement(selectTableNameStatementId);
      st.setLong(1, id);
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next()) {
        tableName = rs.getString(1);
      }
      rs.close();
    }
    return tableName;
  }
  
  
  
  /**
   * Reads serials for given ids.
   * If a table ID is not monitored, the returned serial is -1.
   *
   * @param ids the table IDs
   * @return the serials
   */
  public long[] readSerials(long[] ids) {
    if (db.isRemote())  {
      try {
        return ((ModificationThreadRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId())).readSerials(ids);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote readSerials failed");
        return null;
      }
    }
    else  {
      if (selectAllSerialStatementId == 0) {
        selectAllSerialStatementId = db.prepareStatement(
                "SELECT " + DbObject.FIELD_ID + "," + DbObject.FIELD_SERIAL +
                " FROM " + ModificationCounter.TABLENAME);
      }
      PreparedStatementWrapper st = db.getPreparedStatement(selectAllSerialStatementId);
      ResultSetWrapper rs = st.executeQuery();
      long[] serials = new long[ids.length];
      for (int i=0; i < serials.length; i++)  {
        serials[i] = -1; // table not monitored so far
      }
      while (rs.next()) {
        long id = rs.getLong(1);
        long serial = rs.getLong(2);
        for (int i=0; i < ids.length; ++i)  {
          if (ids[i] == id) {
            serials[i] = serial;
          }
        }
      }
      rs.close();
      return serials;
    }
  }
  
  
  
  
  /**
   * Sets the polling interval.
   * 
   * @param mseconds the polling interval in milliseconds
   */
  public void setInterval(long mseconds) {
    this.mseconds = mseconds;
  }
  
  /**
   * Gets the polling interval.
   * 
   * @return the polling interval in milliseconds
   */
  public long getInterval()  {
    return mseconds;
  }
  
  
  /** 
   * Determines whether this is idle.
   * 
   * @return true if thread is currently set to idle
   */
  public boolean isIdle() {
    synchronized (this) {
      return idle;
    }
  }
  
  /** 
   * Sets the idle state of this thread.<br>
   * Application threads using the primary db-connection (i.e. not running in
   * the GUI-Thread) should setIdle(true) for the time they are running
   * and setIdle(false) when finished.
   * This is because the modification thread runs the runnables in the GUI-Thread
   * via invokeLater and this might interfere with application threads.
   *
   * @param idle true if thread should *not* process the modification table.
   */
  public void setIdle(boolean idle) {
    synchronized (this) {
      this.idle = idle;
    }
  }
  
  
  
  /**
   * Gets the remote delegate.
   * 
   * @return the delegate
   */
  protected int getRemoteDelegateId() {
    if (delegateId == 0)  {
      delegateId = Db.prepareRemoteDelegate(ModificationThread.class);
    }
    return delegateId;
  }
  
  private static int delegateId;
  
  
  
  
  
  
  
  /**
   * Entry for each table (or class) to watch for modifications.
   */
  public class ModificationEntry implements Serializable {
    
    private static final long serialVersionUID = -5376699998876159454L;
    
    private String tableName;                         // the table name
    private long serial;                              // last serial
    private long id;                                  // unique id
    private transient SerialRunnable serialRunnable;  // != null: runnable invoked from the ModificationThread
    private transient Runnable runnable;              // != null: runnable in GUI-Thread to execute when serial changed
    
    
    /**
     * Creates a modification entry.
     * 
     * @param tableName the tablename
     * @param runnable the runnable to execute in case of a modification
     */
    public ModificationEntry(String tableName, Runnable runnable) {
      this.tableName = tableName;
      this.runnable  = runnable;
      setup();
    }
 
    /**
     * Creates a modification entry.
     * 
     * @param tableName the tablename
     * @param serialRunnable the runnable to execute in the modthread in case of a modification
     */
    public ModificationEntry(String tableName, SerialRunnable serialRunnable) {
      this.tableName      = tableName;
      this.serialRunnable = serialRunnable;
      setup();
    }
    
    private void setup()  {
      serial = -1;                        // -1 = no check so far
      if (db != null) { // if not the dummy thread
        long[] nums = selectIdSerialForName(tableName);    // determine id and/or create entry
        id     = nums[0];
        serial = nums[1];
      }
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ModificationEntry &&
             ((ModificationEntry) obj).tableName.equals(tableName) &&
             ((ModificationEntry) obj).runnable == runnable &&
             ((ModificationEntry) obj).serialRunnable == serialRunnable;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 37 * hash + (this.tableName != null ? this.tableName.hashCode() : 0);
      hash = 37 * hash + (this.serialRunnable != null ? this.serialRunnable.hashCode() : 0);
      hash = 37 * hash + (this.runnable != null ? this.runnable.hashCode() : 0);
      return hash;
    }
  }
  
  
}
