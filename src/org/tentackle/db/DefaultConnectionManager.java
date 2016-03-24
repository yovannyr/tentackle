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

// $Id: DefaultConnectionManager.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;

import java.sql.SQLException;


/**
 * The default implementation of a connection manager.<br>
 * Each Db will get its own physical connection.
 * This kind of manager is useful for 2-tier applications directly connecting
 * to a database backend.
 * <p>
 * Although this manager implements a strict 1:1 mapping between dbs and connections
 * it can be easily extended to implememt a M:N mapping, see the {@link MpxConnectionManager}.
 *
 * @author harald
 */
public class DefaultConnectionManager implements ConnectionManager {
  
  /** name of the connection manager **/
  protected String name;
  /** initial size **/
  protected int iniSize;
  /** offset for connection IDs **/
  protected int idOffset;
  
  /** {@link Db}s logged in **/
  protected Db[] dbList;
  /** maximum number of {@link Db}s, 0 = unlimited **/
  protected int maxDbSize;
  /** free list for dbList (unused entries in dbList) **/
  protected int[] freeDbList;
  /** number of entries in freeDbList **/
  protected int freeDbCount;
  
  /** managed connections **/
  protected ManagedConnection[] conList;
  /** maximum number of connections, 0 = unlimited **/
  protected int maxConSize;
  /** free list for conList (unused entries in conList) **/
  protected int[] freeConList;
  /** number of entries in freeConList **/
  protected int freeConCount;
  
  
  private int maxCountForClearWarnings = 1000; // trigger when to clearWarning() on a connection (enabled by default)
  
  
  /**
   * Creates a new connection manager.
   * 
   * @param name the name of the connection manager
   * @param iniSize the initial iniSize of the db table
   * @param maxSize the maximum number of connections, 0 = unlimited (dangerous!)
   * @param idOffset the offset for connection ids (> 0)
   */
  public DefaultConnectionManager(String name, int iniSize, int maxSize, int idOffset) {
    
    if (iniSize < 1) {
      throw new IllegalArgumentException("initial size must be > 0");
    }
    if (idOffset < 1) {
      throw new IllegalArgumentException("connection ID offset must be > 0");
    }
    if (maxSize != 0 && maxSize < iniSize) {
      throw new IllegalArgumentException("maxSize must be 0 or >= iniSize");
    }
    
    this.name       = name;
    this.iniSize    = iniSize;
    this.idOffset   = idOffset;
    this.maxDbSize  = maxSize;     // Db and Cons use the same max-setting!
    this.maxConSize = maxSize;
    
    dbList  = new Db[iniSize];
    conList = new ManagedConnection[iniSize];
    freeDbList = new int[iniSize];
    freeConList = new int[iniSize];
    
    for (int i=iniSize-1; i >= 0; i--) {
      freeDbList[freeDbCount++] = i;
      freeConList[freeConCount++] = i;
      dbList[i] = null;
      conList[i] = null;
    }
  }
  
  /**
   * Creates a new connection manager
   * with an initial size of 2, a maximum of 8 concurrent connections and an id offset of 1.
   * This is the default connection manager for 2-tier client applications.
   * The max connections will prevent ill behaving applications from tearing down the dbserver
   * by opening connections excessively. The usual application holds 2 connections and temporarily
   * 1 or 2 more. If you need more, change the connection manager in Db.
   */
  public DefaultConnectionManager() {
    this("<default>", 2, 8, 1);
  }
  
  
  
  /**
   * Gets the name of the manager.
   */
  @Override
  public String toString() {
    return name;
  }
  
  

  
  /**
   * Sets the countForClearWarnings trigger, 0 = app must eat the warnings!
   * 
   * @param maxCountForClearWarnings the maxcount
   */
  public void setMaxCountForClearWarnings(int maxCountForClearWarnings) {
    this.maxCountForClearWarnings = maxCountForClearWarnings;
  }
  
  /**
   * Gets the current setting for clearWarnings() trigger
   * 
   * @return the countForClearWarnings trigger, 0 = app must eat the warnings!
   */
  public int getMaxCountForClearWarnings()  {
    return maxCountForClearWarnings; 
  }
  
  

  
  /**
   * Adds a Db to the list.
   *
   * @param db the db to add
   * @return the index of db in the dblist
   */
  protected int addDb(Db db) {
    if (freeDbCount == 0) {
      // no more free Db entries: double the list size
      if (maxDbSize > 0 && dbList.length >= maxDbSize) {
        throw new DbRuntimeException(this + ": max. number of Db instances exceeded (" + maxDbSize + ")");
      }
      int newSize = dbList.length << 1;
      if (maxDbSize > 0 && newSize > maxDbSize) {
        newSize = maxDbSize;
      }
      Db[] nDbList = new Db[newSize];
      System.arraycopy(dbList, 0, nDbList, 0, dbList.length);
      int[] nFreeDbList = new int[newSize];
      System.arraycopy(freeDbList, 0, nFreeDbList, 0, dbList.length);
      for (int i=newSize-1; i >= dbList.length; i--) {
        nFreeDbList[freeDbCount++] = i;
        nFreeDbList[i] = -1;
        nDbList[i] = null;
      }
      dbList = nDbList;
      freeDbList = nFreeDbList;
    }
    
    int index = freeDbList[--freeDbCount];
    dbList[index] = db;
    return index;
  }
    
    
  /**
   * Removes a Db from the list.
   *
   * @param index the index of db in the dblist
   * @return the removed db
   */
  protected Db removeDb(int index) {
    Db db = dbList[index];
    dbList[index] = null;
    freeDbList[freeDbCount++] = index;
    return db;
  }
    
  
  /**
   * Gets the number of valid db entries.
   *
   * @return the number of db entries
   */
  public int getDbCount() {
    return dbList.length - freeDbCount;
  }
  
  
  /**
   * Adds a connection to the list.
   *
   * @param con the connection to add
   * @return the index of connection in the conlist
   */
  protected int addConnection(ManagedConnection con) {
    if (freeConCount == 0) {
      // no more free connection entries: double the list size
      if (maxConSize > 0 && conList.length >= maxConSize) {
        throw new DbRuntimeException(this + ": max. number of connections exceeded (" + maxConSize + ")");
      }
      // no more free Db entries: double the list size
      int newSize = conList.length << 1;
      if (maxConSize > 0 && newSize > maxConSize) {
        newSize = maxConSize;
      }
      ManagedConnection[] nConList = new ManagedConnection[newSize];
      System.arraycopy(conList, 0, nConList, 0, conList.length);
      int[] nFreeConList = new int[newSize];
      System.arraycopy(freeConList, 0, nFreeConList, 0, conList.length);
      for (int i=newSize-1; i >= conList.length; i--) {
        nFreeConList[freeConCount++] = i;
        nFreeConList[i] = -1;
        nConList[i] = null;
      }
      conList = nConList;
      freeConList = nFreeConList;
    }
    
    int index = freeConList[--freeConCount];
    conList[index] = con;
    con.setIndex(index);
    return index;
  }
    
    
  /**
   * Removes a connection from the list.
   *
   * @param index the index of connection in the conlist
   * @return the removed connection
   */
  protected ManagedConnection removeConnection(int index) {
    ManagedConnection con = conList[index];
    conList[index] = null;
    freeConList[freeConCount++] = index;
    con.setIndex(-1);
    return con;
  }
  
  
  /**
   * Gets the number of established connections
   *
   * @return the number of connections
   */
  public int getConnectionCount() {
    return conList.length - freeConCount;
  }
  
  
 
  // ---------------- implements ConnectionManager -----------------
  
  
  public int getMaxLogins() {
    return maxDbSize;
  }
  
  
  public int getMaxConnections() {
    return maxConSize;
  }
  
  
  // creates a connection for a given db
  private ManagedConnection createConnection(Db db) {
    ManagedConnection con;
    
    try {
      con = new ManagedConnection(this, db.connect());
    }
    catch (SQLException e) {
      throw new DbRuntimeException(e);
    }      
     
    if (con.getAutoCommit() == false) {
      con.close();
      throw new DbRuntimeException(this + ": connection " + con + " is not in autoCommit mode");
    }
    con.setMaxCountForClearWarnings(maxCountForClearWarnings);
    return con;
  }
  
  
  
  public int login(Db db) throws DbRuntimeException {
    
    ManagedConnection con = createConnection(db);
    
    synchronized(this) {
      int id = addDb(db);
      /**
       * because we add the connections in the same order as the db (1:1 mapping), the
       * index returned after adding the conncetion must be the same as for the db.
       */
      if (addConnection(con) != id) {
        con.close();
        removeDb(id);
        throw new DbRuntimeException(this + ": db- and connection-list out of sync");
      }
      
      id += idOffset;
      
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine(this + ": assigned " + db + " to connection " + con + ", id=" + id);
      }
      return id;
    }
  }


  public Db logout(int id) throws DbRuntimeException {
    synchronized(this) {
      id -= idOffset;
      Db db = removeDb(id);                           // remove the db
      ManagedConnection con = removeConnection(id);   // remove connection
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine(this + ": released " + db + " from connection " + con + ", id=" + id);
      }
      con.close();                                    // close the removed connection
      return db;
    }
  }

  
  public ManagedConnection attach(int id) throws DbRuntimeException {
    id--;
    ManagedConnection con = conList[id];
    
    if (con.isDead()) {
      // try to reopen
      DbGlobal.logger.warning(this + ": closing **DEAD** connection " + con);
      try {
        con.close();
      }
      catch (DbRuntimeException ex) {}
      // reopen the connection
      con = createConnection(dbList[id]);
      conList[id] = con;
      DbGlobal.logger.warning(this + ": connection " + con + " reopened");
    }
    
    con.attachDb(dbList[id]);
    return con;
  }

  
  public void detach(int id) throws DbRuntimeException {
    id--;
    ManagedConnection con = conList[id];
    con.detachDb(dbList[id]);
  }
  
  
  public void shutdown() {
    // close all connections
    synchronized(this) {
      for (int i=0; i < conList.length; i++) {
        if (conList[i] != null) {
          ManagedConnection con = removeConnection(i);
          con.close();
        }
      }
    }
  }
}
