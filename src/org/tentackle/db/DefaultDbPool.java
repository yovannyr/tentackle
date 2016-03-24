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

// $Id: DefaultDbPool.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;


/**
 * An implementation of a database pool.<br>
 * It allows min/max sizes, fixed increments and timeouts for unused instances.
 * <p>
 * The pool can be used with any ConnectionManager.
 * If used with the {@link DefaultConnectionManager}, each {@link Db} instance corresponds to a
 * physical JDBC-connection. If used with an {@link MpxConnectionManager}, the {@link Db} instances
 * map to virtual connections that will be attached temporarily during db-operations,
 * i.e. the {@link org.tentackle.appworx.AppDbObjectCache} will retain its data across sessions
 * (if not closed due to idle timeout).
 * This is the preferred configuration in server applications with a lot of clients.
 * In order to clear the AppDbCache on db-close, you have to override the closeDb method.
 */
public class DefaultDbPool implements DbPool {
  
  // managed Db slot
  private class PooledDb {
    private Db db;
    private long unusedSince;
    private PooledDb() {
      /**
       * Important: userInfo must be cloned because otherwise changes
       * to the userinfo would affect all instances simultaneously.
       */
      db = new Db(conMgr, userInfo.clone());
      if (db.open() == false) {
        throw new DbRuntimeException("cannot open new db for " + DefaultDbPool.this);
      }
      db.setPool(DefaultDbPool.this);
    }
    private void close() {
      closeDb(db);
    }
  }
  

  private String name;                // the pool's name
  private ConnectionManager conMgr;   // the connection manager
  private UserInfo userInfo;          // user info
  private int incSize;                // increment size
  private int minSize;                // min pool size
  private int maxSize;                // max pool size
  private int maxMinutes;             // timeout in [minutes]
  
  private PooledDb[] pool;            // database pool
  private int[] freeList;             // free entries in the pool
  private int freeCount;              // number of entries in freeList
  private int[] unusedList;           // unused Db instances in the pool
  private int unusedCount;            // number of unused Db instances
  
  private Thread timeoutThread;       // watching for timed out Db instances to close and for min pool size
  private boolean shutdownRequested;  // true if shutdown procedure initiated
  
  
  /**
   * Creates a pool.
   * 
   * @param name the name of the pool
   * @param conMgr the connection manager to use for new Db instances
   * @param userInfo the userinfo for the created Db
   * @param iniSize the initial poolsize
   * @param incSize the number of Db instances to enlarge the pool if all in use
   * @param minSize the minimum number of Db instances to keep in pool
   * @param maxSize the maximum number of Db instances, 0 = unlimited
   * @param maxMinutes the timeout in minutes to close unused Db instances, 0 = never close
   */
  public DefaultDbPool (String name, ConnectionManager conMgr, UserInfo userInfo, 
                        int iniSize, int incSize, int minSize, int maxSize, int maxMinutes) {
    
    if (maxSize > 0 && (maxSize < iniSize || maxSize < minSize) ||
        minSize < 1 ||
        incSize < 1 ||
        iniSize < 1) {
      throw new IllegalArgumentException("illegal size parameters");
    }
    
    this.name       = name;
    this.conMgr     = conMgr;
    this.userInfo   = userInfo;
    this.incSize    = incSize;
    this.minSize    = minSize;
    this.maxSize    = maxSize;
    this.maxMinutes = maxMinutes;
    
    // setup the pool
    pool = new PooledDb[iniSize];
    freeList = new int[iniSize];
    unusedList = new int[iniSize];
    for (int i=0; i < iniSize; i++) {
      pool[i] = null;
      freeList[freeCount++] = i;
      unusedList[i] = -1;
    }
    
    // bring up the initial pool
    createDbInstances(iniSize);
    
    timeoutThread = new Thread() {
      
      @Override
      public void run() {
        while (!shutdownRequested) {
          try {
            sleep(60000);    // wait for a minute
          } catch (InterruptedException ex) {}
          if (!shutdownRequested) {
            /**
             * bring down timed out unused Db instances.
             */
            long curtime = System.currentTimeMillis();
            synchronized(DefaultDbPool.this) {
              for (int i=0; i < unusedCount; i++) {
                int index = unusedList[i];
                // if used at all and unused interval elapsed
                if (pool[index].unusedSince != 0 &&
                    pool[index].unusedSince + DefaultDbPool.this.maxMinutes*60000 < curtime) {
                  // timed out
                  removeDbInstance(index);
                  i--;    // start over at same slot
                }
              }
              // check if we need some Db to bring up for minSize
              int size = getSize();
              if (size < DefaultDbPool.this.minSize) {
                createDbInstances(DefaultDbPool.this.minSize - size);
              }
            }
          }
        }
      }
    };
    timeoutThread.start();
  }

  
  /**
   * Creates a pool useful for most servers.<br>
   * Using the default connection manager.
   * Starts with 8 Db instances, increments by 2, minSize 4, maxSize from connection manager.
   * Timeout 1 hour.
   * 
   * @param ui the userinfo for the created Db
   */
  public DefaultDbPool (UserInfo ui) {
    this("<default>", DbGlobal.connectionManager, ui, 8, 2, 4, DbGlobal.connectionManager.getMaxLogins(), 60);
  }
  
  
  
  /**
   * Gets the pool's name
   */
  @Override
  public String toString() {
    return name;
  }
  
  
  
  /**
   * Creates Db instances
   *
   * @param num the number of instances to add to the pool
   */
  private void createDbInstances(int num) {
    if (num > freeCount) {
      // enlarge arrays
      int nSize = pool.length + num - freeCount;
      if (maxSize > 0 && nSize > maxSize) {
        nSize = maxSize - pool.length;
      }
      if (nSize <= 0) {
        throw new DbRuntimeException("cannot create more Db instances, max. poolsize " + maxSize + " reached");
      }
      PooledDb[] nPool  = new PooledDb[nSize];
      int[] nFreeList   = new int[nSize];
      int[] nUnusedList = new int[nSize];
      System.arraycopy(pool, 0, nPool, 0, pool.length);
      System.arraycopy(freeList, 0, nFreeList, 0, pool.length);
      System.arraycopy(unusedList, 0, nUnusedList, 0, pool.length);
      for (int i=pool.length; i < nSize; i++) {
        nPool[i] = null;
        nFreeList[freeCount++] = i;
        nUnusedList[i] = -1;
      }
      pool = nPool;
      freeList = nFreeList;
      unusedList = nUnusedList;
    }
           
    // free is large enough: get from freelist
    while (num > 0) {
      int index = freeList[--freeCount];
      pool[index] = new PooledDb();
      unusedList[unusedCount++] = index;
      num--;
    }

    // now we have at least num unused Db instances
  }
  
  
  
  /**
   * Closes a Db instance and removes it from the pool.
   *
   * @param index the pool index
   */
  private void removeDbInstance(int index) {
    pool[index].db.close();         // this will also check for pending attach/tx and rollback if necessary
    pool[index] = null;
    freeList[freeCount++] = index;  // add to freelist
    // check if index was in the unused list. If so, remove it
    for (int i=0; i < unusedCount; i++) {
      if (unusedList[i] == index) {
        // found: 
        for (int j=i+1; j < unusedCount; j++) {
          unusedList[j-1] = unusedList[j];
        }
        unusedCount--;
        break;
      }
    }
  }
  
  
  /**
   * Closes a db.<br>
   * The method can be overridden if there is something to do after/before close.
   * For example, cleaning up the cache, etc...
   *
   * @param db the Db instance to close
   */
  protected void closeDb(Db db) {
    db.close();
  }
  
  
  /**
   * Clones the masterDb to create a new Db instance.<br>
   * The method can be overridden if there is something to do after/before close.
   *
   * @param db the master Db instance to clone
   * @return the cloned db
   */
  protected Db cloneDb(Db db) {
    return db.clone();
  }
  

  /**
   * Closes all databases in the pool, cleans up and makes the pool unusable.
   */
  public void shutdown() {
    shutdownRequested = true;
    timeoutThread.interrupt();
    try {
      timeoutThread.join();
    } 
    catch (InterruptedException ex) {
      throw new DbRuntimeException("shutdown " + this + " failed", ex);
    }
    synchronized(this) {
      for (PooledDb pdb: pool) {
        if (pdb != null) {
          pdb.close();
        }
      }
      pool = null;
      freeList = null;
      unusedList = null;
    }
  }

  
  
  // ------------------- implements DbPool ------------------------------
  
  
  public int getMaxSize() {
    return maxSize;
  }
  
  
  public int getSize() {
    return pool == null ? 0 : pool.length - freeCount;
  }
  
  
  public Db getDb() throws DbRuntimeException {
    synchronized(this) {
      if (unusedCount == 0) {
        createDbInstances(incSize); // enlarge the pool (will throw Exception if pool is exhausted)
      }
      int poolId = unusedList[--unusedCount];
      Db db = pool[poolId].db;
      if (!db.isOpen()) {
        throw new DbRuntimeException(this + ": Db " + db + " has been closed unexpectedly");
      }
      pool[poolId].unusedSince = 0;
      db.setPoolId(poolId+1);   // starting at 1
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine(this + ": Db " + db + " assigned to pool id " + poolId);
      }
      return db;
    }
  }
  
  
  public void putDb(Db db) throws DbRuntimeException {
    synchronized(this) {
      if (!db.isOpen()) {
        throw new DbRuntimeException(this + ": Db " + db + " has been closed unexpectedly");
      }
      if (db.getPool() != this) {
        throw new DbRuntimeException("Db " + db + " is not pooled by " + this);
      }
      int poolId = db.getPoolId();
      if (poolId < 0 || poolId > pool.length) {
        throw new DbRuntimeException(this + ": Db " + db + " has invalid poolid " + poolId);
      }
      if (poolId != 0) { // if pooled
        // check if there are no pending transactions
        ManagedConnection con = db.getConnection();
        if (con != null) {
          con.closeAllPreparedStatements(true); // cleanup all pending statements
          removeDbInstance(poolId-1);           // remove from pool, logout and rollback if necessary
        }
        else  {
          if (DbGlobal.logger.isFineLoggable()) {
            DbGlobal.logger.fine(this + ": Db " + db + " returned to pool, id " + poolId);
          }
          poolId--;
          pool[poolId].unusedSince = System.currentTimeMillis();
          unusedList[unusedCount++] = poolId;
        }
        db.setPoolId(0);    // no more in pool
        db.setGroupId(0);   // clear group
      }
    }
  }

}
