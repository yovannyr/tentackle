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

// $Id: MpxConnectionManager.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;

import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import org.tentackle.util.StringHelper;



/**
 * Multiplexing connection manager.<br>
 * 
 * A connection manager for applications with a large number of Db instances,
 * e.g. application servers. The manager will multiplex N Db instances against
 * M connections, allowing N &gt; M. This is not to be mixed up with db-pooling, as connection
 * multiplexing is completely transparent to the application whereas pooling requires
 * an explicit relation to something like a session.
 * Note that the authentication must be done at the application level because
 * the pool's connections are derived from the userinfo of a server db.
 *
 * @author harald
 */
public class MpxConnectionManager extends DefaultConnectionManager {
  
  // args
  protected Db  serverDb;             // server Db to clone
  protected int incSize;              // increment size
  protected int minSize;              // minimum size
  protected int maxSize;              // maximum size
  protected int minMinutes;           // min hours to close connections
  protected int maxMinutes;           // max hours to close connections
  
  // local
  protected int[] unConList;          // free list for unattached connections
  protected int unConCount;           // number of entries in unConList
  protected Random random;            // randomizer
  
  // connect thread
  private boolean shutdownRequested;            // true if shutdown procedure initiated
  private Thread connectThread;                 // thread to bring up connections
  private final Object connectGoMutex;          // mutex to trigger connect thread to go for more connections
  private final Object connectDoneMutex;        // mutex to signal waiting threads that connections were brought up
  private volatile int conRequestCount;         // number of new connections requested
  
  
 
  
  /**
   * Creates a new connection manager.
   * 
   * @param name the name of the connection manager
   * @param serverDb the root db to use for creating connections (may be open or closed)
   * @param maxDb the maximum number of Db instances, 0 = no limit
   * @param idOffset the offset for connection ids (> 0)
   * @param iniSize the initial size of the connection pool
   * @param incSize the number of connections to add if all in use
   * @param minSize the minimum number of connections
   * @param maxSize the maximum number of connections
   * @param minMinutes minimum minutes a connection should be used
   * @param maxMinutes maximum minutes a connection should be used
   */
  public MpxConnectionManager(String name, Db serverDb, int maxDb, int idOffset, int iniSize, int incSize, 
                              int minSize, int maxSize, int minMinutes, int maxMinutes) {
    
    super(name, iniSize, maxSize, idOffset);
    
    if (minSize > maxSize ||
        minMinutes > maxMinutes ||
        incSize < 1 ||
        incSize > (maxSize - iniSize) ||
        (maxDb > 0 && maxDb < iniSize)) {
      throw new IllegalArgumentException("illegal or conflicting parameters");
    }
    
    this.serverDb   = serverDb;
    this.maxDbSize  = maxDb;
    this.incSize    = incSize;
    this.minSize    = minSize;
    this.maxSize    = maxSize;
    this.minMinutes = minMinutes;
    this.maxMinutes = maxMinutes;
    
    random = new Random();
    
  
    // setup initial connections
    unConList = new int[iniSize];
    createConnections(iniSize);
    
    // create and start the connect thread
    connectGoMutex   = new Object();
    connectDoneMutex = new Object();
    
    // connections are brought up in an extra thread
    connectThread    = new Thread() {
      
      @Override
      public void run() {
        while (!shutdownRequested) {
          synchronized(connectGoMutex) {
            try {
              connectGoMutex.wait();
            } 
            catch (InterruptedException ex) {}
          }
          if (!shutdownRequested) {
            /**
             * Bring up connections.
             * This will probably throw exceptions that we catch here and log them.
             * There's not much more we can do.
             */
            try {
              // create missing connections
              int count = createConnections(conRequestCount);

              synchronized(MpxConnectionManager.this) {
                conRequestCount = count == 0 ? -1 : 0;    // -1 = max. connections exhausted
              }
            }
            catch (Exception e) {
              DbGlobal.errorHandler.warning(e, MpxConnectionManager.this + ": creating connections failed");
            }
            // signal all waiting threads that connections were brought up
            synchronized(connectDoneMutex) {
              connectDoneMutex.notifyAll();
            }
          }
        }
      }
    };
    connectThread.start();
    
  }
  
  
  /**
   * Creates a connection manager with reasonable values for most servers.
   * 
   * Maximum of 1000 Db instances (500 clients).
   * Start with 8 connections.
   * Add 2 connections at a time if all connections are in use.
   * Don't drop below 4 connections.
   * Maximum of 100 connections, i.e. concurrent db operations (i.e. info above 25, warning above 50)
   * Within 12 to 36h (approx. once a day), close and reopen connections
   * (allows updates of the database servers's QEPs, prepared statements cache, etc...).
   * 
   * The idOffset will be the connectionId of the serverDb + 1 (i.e. normally 2 for application servers)
   * 
   * @param serverDb the root db to use for creating connections (may be open or closed).
   */
  public MpxConnectionManager(Db serverDb) {
    this("<mpx-default>", serverDb, 1000, serverDb.getConnectionId() + 1, 8, 2, 4, 100, 720, 2160);
  }
  
  
  
  
  /**
   * Shuts down this connection manager.<br>
   * All connections are closed and the threads stopped.
   * Application servers should invoke this method when shut down.
   */
  @Override
  public void shutdown() {
    shutdownRequested = true;
    connectThread.interrupt();
    try {
      connectThread.join();   // wait until connect thread terminates
    } 
    catch (InterruptedException ex) {
      DbGlobal.errorHandler.warning(ex, this + ": stopping the connect thread failed");
    }
    // close all connections
    synchronized(this) {
      super.shutdown();
      unConCount = 0;
    }
  }
  
  
  
  /**
   * Adds the index of an unused connection to the freelist.
   *
   * @param index the index of the connection in the connections list
   */
  protected void pushUnattached(int index) {
    if (unConCount >= unConList.length) {
      // list is full, enlarge it
      int[] nUnConList = new int[unConList.length << 1];
      System.arraycopy(unConList, 0, nUnConList, 0, unConList.length);
      for (int j=unConList.length; j < nUnConList.length; j++) {
        nUnConList[j] = -1;
      }
      unConList = nUnConList;
    }
    unConList[unConCount++] = index;    // add to freelist    
  }
  
  
  /**
   * Gets a connection from the unattached freelist.
   * 
   * @return the index to the connections list, -1 if no more unattached found
   */
  protected int popUnattached() {
    return unConCount > 0 ? unConList[--unConCount] : -1;
  }
  
  
  
  /**
   * Create spare connections.
   *
   * @param count the number of connections to create
   * @return the number of connections created
   */
  protected int createConnections(int count) {
    try {
      int conSize;
      synchronized(this) {
        conSize = getConnectionCount();
      }
      if (conSize + count < minSize) {
        // at least to minsize
        count = minSize - conSize;
      }
      // align count before we run into DbRuntimeException in addConnection()
      if (maxConSize > 0) {
        if (conSize + count > maxConSize) {
          count = maxConSize - conSize;
        }
        if (count == 0) {
          DbGlobal.logger.severe(this + ": *** maximum number of connections reached: " + maxConSize + " ***");
        }
        else  {
          if (conSize + count > maxConSize/4) {
            if (conSize + count > maxConSize/2) {
              // half of the connections concurrently in use in most cases tells us that the db-server
              // is reaching its limits, i.e. the operations take too long. Or maxConSize is simply too low.
              DbGlobal.logger.warning(this + ": increasing number of connections to " + (conSize + count) + " of " + maxConSize);
            }
            else if (DbGlobal.logger.isInfoLoggable()) {
              // pre warning
              DbGlobal.logger.info(this + ": increasing number of connections to " + (conSize + count) + " of " + maxConSize);
            }
          }
        }
      }
      for (int i=0; i < count; i++) {
        ManagedConnection con = new ManagedConnection(this, serverDb.connect());    // can take some time dep. on the db backend
        con.setExpireAt(con.getEstablishedSince() + (minMinutes*60 + random.nextInt((maxMinutes - minMinutes)*60)) * 1000L);
        if (DbGlobal.logger.isInfoLoggable()) {
          DbGlobal.logger.info(this + ": open connection " + con +
                               ", valid until " + StringHelper.timestampFormat.format(new Date(con.getExpireAt())));
        }
        synchronized(this) {
          pushUnattached(addConnection(con));   // add to established connections and unattached freelist
        }
      }
      return count;
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException(this + ": creating connection failed", ex);
    }
  }
  
  
  
  
  
  @Override
  public int login(Db db) throws DbRuntimeException {
    synchronized(this) {
      int id = addDb(db) + idOffset;
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine(db + " logged into " + this + ", id=" + id);
      }
      return id;
    }
  }
  

  @Override
  public Db logout(int id) throws DbRuntimeException {
    synchronized(this) {
      id -= idOffset;
      Db db = removeDb(id); // remove the db
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine(db + " logged out from " + this + ", id=" + id);
      }
      // check that connection is not attached, if so, detach it!
      ManagedConnection con = db.getConnection();
      if (con != null) {
        con.forceDetached();
        if (con.isDead()) {
          // remove connection if dead and check to reopen
          cleanupDeadConnection(con);
          /**
           * Client crashed due to a dead server connection.
           * This is a strong indicator that the database is facing some severe
           * problems. Because we don't know how many unattached connections are down
           * we will probe them here. This is a little time consuming but better
           * than waiting for other clients to crash.
           */
          int[] newUnConList = new int[unConList.length];
          int newUnConCount  = 0;
          for (int i=0; i < unConCount; i++) {
            ManagedConnection c = conList[unConList[i]];
            c.verifyConnection();
            if (c.isDead()) {
              cleanupDeadConnection(c);
            }
            else  {
              newUnConList[newUnConCount++] = c.getIndex();
            }
          }
          unConList  = newUnConList;
          unConCount = newUnConCount;
          
          // open any missing connections
          reopenConnections();
        }
        else  {
          // add connection to freelist
          pushUnattached(con.getIndex());
        }
      }
      return db;
    }
  }

  
  @Override
  public ManagedConnection attach(int id) throws DbRuntimeException {
    
    id -= idOffset;
    Db db = dbList[id];
    ManagedConnection con = db.getConnection();
    
    int loopCount = 0;   // number of retries so far
    
    while (con == null) {   // while not attached

      synchronized(this) {
        // find an unattached connection
        int index = popUnattached();
        if (index >= 0) {
          con = conList[index];
          /**
           * If connection is detached for a long time (minMinutes/2), verify the connection first.
           * This is mainly for databases like MySQL that close connections after a certain time
           * of inactivity. For those databases it is recommended to set the idle-timeout to minTime.
           */
          if (System.currentTimeMillis() - con.getDetachedSince() > minMinutes * 30000L) {
            /**
             * Although this will block all threads from attaching/detaching a connection,
             * this is the preferred solution compared to an extra "verification thread",
             * because that thread would need some synchronisation on the connection object
             * to prevent attachment while running the verification statement.
             * However, verification happens very rarely, so the performance would suffer
             * more from permanent and mostly unnecessary syncs than rare blocking verifications --
             * hopefully... ;)
             */
            con.verifyConnection();
          }
          
          if (con.isDead()) {
            cleanupDeadConnection(con);
            con = null;
            continue;
          }
          else  {
            break;
          }
        }
      }
      
      /**
       * In order not to stop all clients for the duration of establishing new connections
       * this is done in the connecThread. We will just trigger the thread and wait.
       */
      boolean request = false;
      synchronized(this) {
        if (conRequestCount == 0) {
          // no request running: request more connections
          conRequestCount = incSize;
          request = true;
        }
        else if (conRequestCount < 0) {
          // max connections exhausted
          conRequestCount = 0;
          loopCount++;
          if (loopCount > 20) {    // we tried 20 times and it took us about a minute. Sorry :-(
            // this will probably close the client unfriendly, but what else can we do?
            throw new DbRuntimeException(this + ": max. number of concurrent connections in use: " + maxConSize);
          }
          try {
            // sleep a randomly between 1 and 5 seconds (3 average)
            long ms = 1000 + random.nextInt(4000);
            DbGlobal.logger.warning(this + ": Running out of connections! Putting " + db + " to sleep for " + ms + " ms, loop " + loopCount);
            Thread.sleep(ms);
          } catch (InterruptedException ex) {}
        }
        // else: some request is already running
      }
      if (request) {
        // start a new request for bringing up connections
        synchronized(connectGoMutex) {
          connectGoMutex.notifyAll();
        }
      }
      // wait for connections to be brought up 
      synchronized(connectDoneMutex) {
        try {
          connectDoneMutex.wait(10000);     // 10 seconds max.
        } catch (InterruptedException ex) {}
      }
    }
    
    con.attachDb(db);
    return con;
  }

  
  @Override
  public void detach(int id) throws DbRuntimeException {
    
    id -= idOffset;
    Db db = dbList[id];
    ManagedConnection con = db.getConnection();
    
    if (con == null) {
      throw new DbRuntimeException(this + ": no connection attached to " + db);
    }
    
    con.detachDb(db);
    
    if (!con.isAttached()) {
      boolean closed = false;
      synchronized(this) {
        if (con.isDead()) {
          cleanupDeadConnection(con);
        }
        else if (con.getExpireAt() > 0 && con.getExpireAt() < con.getDetachedSince()) {
          // dead connection or connection time elapsed: close it
          removeConnection(con.getIndex());
          closed = true;
        }
        else  {
          // add unattached connection
          pushUnattached(con.getIndex());
        }
      }

      if (closed) {
        if (DbGlobal.logger.isInfoLoggable()) {
          DbGlobal.logger.info(this + ": closing connection " + con +
                               ", open since " + StringHelper.timestampFormat.format(new Date(con.getEstablishedSince())));
        }
        con.close();
        reopenConnections();
      }
    }
  }


  /**
   * closes a dead connection and removes it from the
   * connection list.
   */
  private void cleanupDeadConnection(ManagedConnection con) {
    // marked dead: close it (hard close) and remove it from connection list
    removeConnection(con.getIndex());
    try {
      DbGlobal.logger.warning(this + ": closing **DEAD** connection " + con);
      con.close();
    }
    catch (DbRuntimeException ex) { }
  }



  /**
   * checks whether the number of connections dropped below minSize.
   * If so, reopen missing connections.
   */
  private void reopenConnections() {
    boolean reopen = false;

    synchronized(this) {
      if (conRequestCount == 0) {
        // no connect running
        int num = minSize - getConnectionCount();
        if (num > 0) {
          // we dropped below minSize: open connection(s)
          reopen = true;
          conRequestCount = num;
        }
      }
    }
    
    if (reopen) {
      synchronized(connectGoMutex) {
        connectGoMutex.notifyAll();   // fire connect, but don't wait for completion
      }
    }
  }
  
}
