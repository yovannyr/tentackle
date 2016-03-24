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

// $Id: AppDbObjectCache.java 466 2009-07-24 09:16:17Z svn $


package org.tentackle.appworx;


import org.tentackle.db.Db;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbObject;
import org.tentackle.db.ModificationThread;
import org.tentackle.util.ApplicationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.tentackle.db.DbRuntimeException;
import org.tentackle.util.Compare;
import org.tentackle.util.StringHelper;




/**
 * Cache for {@link AppDbObject}s.<br>
 * 
 * The cache works both client- and server side.
 * There can be one cache per class.
 * The cache may hold any number of unique indexes (see {@link AppDbObjectCacheIndex})
 * that can be added or removed at runtime. At ID-index is mandatory
 * and automatically added. Objects may be preloaded on first access (all
 * objects in the current context) or loaded on demand. The cache is also
 * aware of object-lists. For example, a selectAllCached will return a
 * cached list of all objects. And many more features...
 *
 * @param <T> the {@link AppDbObject} class
 * @author harald
 */
public class AppDbObjectCache<T extends AppDbObject> {
  
  private Class<T> clazz;                           // the AppDbObject class
  private List<AppDbObjectCacheIndex<T,?>> indexes; // the indexes
  private AppDbObjectCacheIndex<T,Long> idIndex;    // special ID-index (cannot be removed!)
  private Set<ContextDb> dbSet;                     // if preload: set of contextDb's used in cache (null if no preloading)
  private Map<ContextDb, List<T>> lists;            // lists for selectAll
  private Set<List<T>> expiredLists;                // lists that contain expired objects
  private long minTableSerial;                      // min tableserial to use for update check
  private long maxTableSerial;                      // max tableserial to use for update check
  private long tableSerial;                         // highest tableserial of all objects in cache
  private long expiredTableSerial;                  // > 0 if delayed expire check
  private boolean enabled;                          // true if cache enabled
  private int maxSize;                              // maximum size, 0 = unlimited (default)
  private int strategy;                             // caching strategy if maxSize != 0
  private int keepQuota;                            // percentage of entries to keep when applying caching strategy. Default is 50.
  
  private boolean inToString;                       // avoid recursion in logging
  
  private static boolean allEnabled = true;         // false = all caches disabled. default is true
  
  // cache eviction strategies
  /** forget all entries if maxSize reached (default) **/
  public static final int FORGET = 0;
  /** least recently used **/
  public static final int LRU    = 1;
  /** least frequently used **/
  public static final int LFU    = 2;
  
  
  // ------------------ all AppDbObjectCaches register globally per application ------------
  
  /**
   * list of all caches
   */
  public static List<AppDbObjectCache<? extends AppDbObject>> cacheList = new ArrayList<AppDbObjectCache<? extends AppDbObject>>();
  
  
  /**
   * Creates an instance of an AppDbObjectCache and registers
   * in a global cache-list (see {@link #removeObjectsForDbInAllCaches}).
   * This is the preferred method to create a cache!
   *
   * @param <T> the data object class
   * @param clazz is the AppDbObject-class managed by the cache.
   * @param preload is true if preload all objects in contextDb of cache.
   * @return the cache
   */
  @SuppressWarnings("unchecked")
  public static <T extends AppDbObject> AppDbObjectCache<T> createCache(Class<T> clazz, boolean preload) {
    AppDbObjectCache<T> cache = new AppDbObjectCache(clazz, preload);
    cacheList.add(cache);
    return cache;
  }
  
  /**
   * Removes all objects in ALL caches that refer to a given db.
   * Useful after having closed a db-connection in an RMI-Server, for example.
   *
   * @param db is the db-connection (probably closed)
   */
  public static void removeObjectsForDbInAllCaches(Db db)  {
    for (AppDbObjectCache<? extends AppDbObject> cache: cacheList) {
      cache.removeObjectsForDb(db);
    }
  }
  
  // ---------------------------------------------------------------------------------------
  
  
  
  

  
  /**
   * Creates an instance of an AppDbObjectCache.
   *
   * @param objectClazz is the AppDbObject-class managed by the cache.
   * @param preload is true if preload all objects in contextDb of cache.
   */
  public AppDbObjectCache(Class<T> objectClazz, boolean preload) {
    
    this.clazz = objectClazz;
    
    indexes         = new ArrayList<AppDbObjectCacheIndex<T,?>>();  // first index created below
    dbSet           = preload ? new TreeSet<ContextDb>() : null;    // no contextDbs preloaded so far
    lists           = new TreeMap<ContextDb, List<T>>();            // lists from selectAll
    expiredLists    = new HashSet<List<T>>();                       // expired lists
    enabled         = true;                                         // initially enabled
    keepQuota       = 50;                                           // keep 50% when shrinking
    minTableSerial  = -1;                                           // initialize the next cache-access
    
    if (AppworxGlobal.logger.isFineLoggable()) {
      AppworxGlobal.logger.fine("creating cache for " + clazz);
    }
    
    // create default index by object-ID.
    idIndex = new AppDbObjectCacheIndex<T,Long>(StringHelper.getClassBaseName(clazz) + ":ID") {
      public Long extract(T object) {
        return object.getId();
      }
      public T select(ContextDb db, Long id)  {
        try {
          return AppDbObject.select(db, clazz, id);
        } catch (Exception ex)  {
          // what can we do??
          DbGlobal.errorHandler.severe(db.getDb(), ex,
                  "creating cache object failed for " + clazz + ", ID=" + id);
          return null;    // not reached
        }
      }
    };
    addIndex(idIndex);
  }
  
  
  /**
   * Creates a cache without preloading.
   * 
   * @param objectClazz is the AppDbObject-class managed by the cache.
   */
  public AppDbObjectCache(Class<T> objectClazz)  {
    this(objectClazz, false);
  }
  
  
  
  @Override
  public String toString()  {
    return "cache '" + clazz + "'";
  }
  
  
  /**
   * Assigns an index to this cache.
   * Note: if already added to another cache the db-errorhandler is invoked.
   */
  private void assignIndex(AppDbObjectCacheIndex<T,?> index)  {
    try {
      index.assignCache(this);    // assign it
    } 
    catch (ApplicationException ex)  {
      // severe application error
      DbGlobal.errorHandler.severe(null, ex, "assigning index failed");
      // not reached
    }
  }
  
  /**
   * Resigns an index from this cache.
   * If not assigned to this cache: invoke errorhandler
   */
  private void resignIndex(AppDbObjectCacheIndex<T,?> index)  {
    // idIndex is private, so it cannot be removed!
    try {
      index.assignCache(null);    // detach it
    } 
    catch (ApplicationException ex)  {
      // severe application error
      DbGlobal.errorHandler.severe(null, ex, "removing index failed");
      // not reached
    }
  }
  
  
  /**
   * Add an index to cache.
   * The application need not invoke addIndex explicitly.
   * Assigning the index to the cache will be done automatically on
   * its first use. This is known as deferred index assignment and
   * has the advantage that the index is managed by the cache
   * if really used by the application.
   * However, the application can assign the index explicitly.
   * This will ensure that the index cannot be assigned to another
   * cache accidently.
   *
   * @param index is the index to add
   */
  public void addIndex(AppDbObjectCacheIndex<T,?> index)  {
    synchronized (this) {
      assignIndex(index);       // assign index to cache
      index.clear();            // clear index for sure
      index.clearCacheStats();
      indexes.add(index);       // add index to List (this will also build the index-map for already added objects)
      if (AppworxGlobal.logger.isFineLoggable()) {
        AppworxGlobal.logger.fine(this + ": adding " + index);
      }
      if (index != (AppDbObjectCacheIndex<T,?>)idIndex) {
        // if not the ID-index: add objects in cache so far
        try {
          for (T object: idIndex.getObjects()) {
            index.addUnique(object);
          }
        } 
        catch (ApplicationException e)  {
          // unique violation detected: log that and invalidate cache
          AppworxGlobal.logger.warning(e.getAllMessages());
          invalidateImpl();
        }
      }
    }
  }
  
  
  /**
   * Adds an index if not already added.
   * 
   * @param index the index to add
   */
  public void addIndexIfNotAssigned(AppDbObjectCacheIndex<T,?> index)  {
    if (index.isAssignedToCache(this) == false) {
      addIndex(index);
    }
  }
  
  
  /**
   * removes an index.
   *
   * @param index to remove
   */
  public void removeIndex(AppDbObjectCacheIndex<T,?> index) {
    resignIndex(index);       // resign index from cache
    indexes.remove(index);    // remove from index list
  }
  
  
  /**
   * get cache-stats
   */
  private String printCacheStats()  {
    String stats = "";
    for (AppDbObjectCacheIndex<T,?> index: indexes)  {
      stats += "\n" + index.printCacheStats();
    }
    return stats;
  }
  
  
  /**
   * get cache-stats
   */
  private void clearCacheStats()  {
    for (AppDbObjectCacheIndex<T,?> index: indexes)  {
      index.clearCacheStats();
    }
  }
  
  
  /**
   * impl of invalidate without synchronization
   */
  private void invalidateImpl() {
    for (AppDbObjectCacheIndex<T,?> index: indexes)  {
      index.clear();
    }
    lists.clear();
    if (dbSet != null)  {
      dbSet.clear();
    }
    tableSerial = 0;  // no object in cache -> tableSerial = 0
  }
  
  
  
  /**
   * get the indexes as a fast array.
   * Speeds up when walking through a collection of objects
   * and performing an operation on all indexes at a time.
   */
  @SuppressWarnings("unchecked")
  private AppDbObjectCacheIndex<T,?>[] getIndexArray()  {
    return (AppDbObjectCacheIndex<T,?>[])indexes.toArray(new AppDbObjectCacheIndex[indexes.size()]);
  }
  
  
  
  /**
   * force the cache to be cleared next access
   */
  public void invalidate() {
    synchronized (this)  {
      if (AppworxGlobal.logger.isFineLoggable()) {
        AppworxGlobal.logger.fine(this + ", invalidating cache " + printCacheStats());
      }
      invalidateImpl();
    }
  }
  
  
  /**
   * Expires object in cache with an ID of a given set.
   * 
   * @param expireSet is an array of long-pairs (id/tableserial), sorted by tableSerial, representing the the objects to set expired
   * @param curSerial is the current table serial (from Modification-table), 0 = don't check
   */
  public void expireByExpirationInfo(long[] expireSet, long curSerial)  {
    
    synchronized(this)  {
      
      /**
       * Build a set of IDs and check for gaps in tableSerial
       */
      boolean gapFound = false;
      long prevSerial  = -1;
      Set<Long> idSet  = new TreeSet<Long>();

      // detect gaps and align minTableSerial for the next check
      if (curSerial > minTableSerial) {
        minTableSerial = curSerial;
      }
      
      int expNdx = 0;
      while (expNdx < expireSet.length) {
        long expId = expireSet[expNdx++];
        long expTableSerial = expireSet[expNdx++];
        if (prevSerial != -1 && expTableSerial - prevSerial > 1) {
          gapFound = true;
        }
        idSet.add(expId);
        prevSerial = expTableSerial;
        if (prevSerial > minTableSerial) {
          minTableSerial = prevSerial;
        }
      }
      

      if (curSerial > 0 && curSerial - prevSerial > 1) {
        /**
         * if curSerial given:
         * no expirations at all (prevSerial == -1) or 
         * a gap indicates that some objects have been deleted
         */
        gapFound = true;
      }
      
      if (gapFound) {
        /**
         * A gap was found, i.e. some objects have been deleted, or no modified objects
         * found at all or the current serial does not match the last tableserial of
         * modified objects:
         *
         * a) objects have been deleted. Deleted objects cannot appear in idSet
         *    simply because they arent in the table anymore.
         *
         * b) a rare condition when an object has been modified (or appended) from
         *    another application and we loaded that object into cache and got the
         *    expiration notice too late.
         * 
         * It's always safe to assume a).
         * Because we cannot tell which of our objects have been deleted,
         * we must invalidate the whole cache.
         * A solution to this problem might be examining the History or ModificationLog, but
         * this isn't worth the effort.
         */
        if (AppworxGlobal.logger.isFinerLoggable()) {
          AppworxGlobal.logger.finer(this + ": some objects deleted -> invalidate all");
        }
        invalidate();
        return;
      }
      
      /**
       * expire objects in cache and count how many such objects found
       */
      Set<Long> foundSet = new TreeSet<Long>(); // holds the IDs that were found in cache
      List<T> objList = getObjects();           // check all objects in cache
      for (T object: objList) {
        Long id = object.getId();
        if (idSet.contains(id))  {
          object.setExpired(true);
          foundSet.add(id);
          if (AppworxGlobal.logger.isFinerLoggable() && !inToString) {
            inToString = true;    // avoid recursion cause contextDb.toString
            AppworxGlobal.logger.finer(object.getSingleName() + " '" + object + "', ID=" + object.getId() +
                    ", context=" + object.getContextDb().getInfo() + ", expired in " + this);
            inToString = false;
          }
        }
      }
      
      int foundSetSize = foundSet.size();
      boolean expireAllLists = foundSetSize < idSet.size();  // not all IDs found in cache
      
      int numLists = lists.size();
      if (!expireAllLists && numLists > 0)  {
        // all Objects in expireSet were found in cache.
        // There are lists: check that all lists have the same size.
        // If they have the same size, it is guaranteed that they contain the same IDs
        // because all objects of all lists are in getObjects() as well.
        int size = -1;
        for (List<T> list: lists.values())  {
          if (size == -1) {
            size = list.size();
          }
          else if (size != list.size()) {
            expireAllLists = true;
            break;            
          }
        }
      }

      if (expireAllLists) {
        if (isPreloading())  {
          // this is a preloading cache: invalidate all
          if (AppworxGlobal.logger.isFinerLoggable()) {
            AppworxGlobal.logger.finer(this + ": some uncached objects expired or objects deleted in preloaded cache -> invalidate all");
          }
          invalidate();
        }
        else  {
          lists.clear();
          if (AppworxGlobal.logger.isFinerLoggable()) {
            AppworxGlobal.logger.finer(this + ": some uncached objects expired or objects deleted -> all lists expired");
          }
        }
      }
      else  {
        /**
         * _ALL_ objects in idSet were in cache and are now marked expired
         * because there is not a single uncached and all lists are the same size.
         * Thus we can keep all lists.
         * However, we should mark the lists with an expire-flag, i.e.
         * on the next selectAllInContext() the expired objects in the lists
         * are reloaded.
         * Notice: because there is one list per contextDb it is guaranteed
         * that an object is part of no more than one list at a time.
         */
        expiredLists.addAll(lists.values());  // set all lists expired        
      }
      
    }
  }
  
  
  
  /**
   * Expire dirty objects in cache.
   * If the cache does not provide a tableSerial it will be invalidated (i.e. all objects removed).
   * Otherwise, no objects are removed and those marked expired that were changed
   * in the persistance layer. This is done by obtaining the IDs of all
   * objects in the db-table with a tableSerial > cache.tableSerial.
   * See DbObject on how to enable tableSerial for DbObjects.
   * If the cache is preloaded or any selectAllInContext was done,
   * the selectAllLists() will be removed if not all modified objects
   * are in the lists.
   *
   * @param db is the db to use, null = delay expiration check until next select
   * @param maxSerial is the max. tableSerial to scan for updates, 0 = clazz provides no tableSerial
   */
  public void expire(Db db, long maxSerial)  {
    synchronized(this)  {
      
      if (maxSerial > maxTableSerial) {
        // remember upper bound of all requests
        maxTableSerial = maxSerial;
      }
      
      if (tableSerial > 0)  {   
        // table provides a tableSerial
        if (expiredTableSerial == 0) {   // if not already triggered
          if (db != null) {
            // run expire check immediately
            expireObjects(db, tableSerial, maxTableSerial);
          }
          else  {
            // delay expire check
            expiredTableSerial = tableSerial; // set lower bound requested (once!)
          }
          if (AppworxGlobal.logger.isFineLoggable()) {
            AppworxGlobal.logger.fine("expire requested on " + this + 
                               ", tableSerial=" + tableSerial + "/" + maxSerial +
                               (db == null ? ", delayed" : (", Db=" + db)));
          }
        }
        else  {
          if (AppworxGlobal.logger.isFineLoggable()) {
            AppworxGlobal.logger.fine("expire requested on " + this + 
                               ", tableSerial=" + tableSerial + "/" + maxSerial +
                               (db == null ? ", delayed" : (", Db=" + db)) +
                               " -> ignored because already requested for tableSerial=" + 
                               expiredTableSerial + "/" + maxTableSerial);
          }          
        }
      }
      else  {
        if (AppworxGlobal.logger.isFineLoggable()) {
          AppworxGlobal.logger.fine("expire requested on " + this + ", tableSerial=" + tableSerial +
                             (db == null ? ", delayed" : (", Db=" + db)) +
                             " -> invalidate cache because objects don't provide a tableSerial");
        }          
        invalidateImpl();
      }
    }
  }
  
  
  
  /**
   * Same as expire but delayed until the next select.
   *
   * @param maxSerial is the max. tableSerial to scan for updates, 0 = clazz provides no tableSerial
   */
  public void expire(long maxSerial)  {
    expire(null, maxSerial);
  }
  
  
  
  
  /**
   * expire objects by examining the tableSerial.
   * Notice: invoke from within synchronized block only!
   *
   * @param db is the db-connection to use
   * @param oldSerial is highest tableserial objects are kept in cache
   * @param maxSerial is the max. tableSerial to scan for, 0 = up to end
   */
  private void expireObjects(Db db, long oldSerial, long maxSerial)  {
    try {

      DbObject obj = DbObject.newByClass(db, clazz);
      
      // align oldserial to minTableSerial
      if (oldSerial < minTableSerial) {
        oldSerial = minTableSerial;
      }
      
      // load info of objects that have expired
      long[] expireSet = maxSerial > 0 ?
              obj.getExpiredTableSerials(oldSerial, maxSerial) :
              obj.selectExpiredTableSerials(oldSerial);
      
      // process expiration info and update minTableSerial
      expireByExpirationInfo(expireSet, maxSerial);
      
    } 
    catch (Exception e) {
      AppworxGlobal.logger.warning(ApplicationException.getStackTraceAsString(e));
      invalidateImpl();
    }
  }
  
  
  
  /**
   * check if a delayed expiration has been triggered.
   * If so, expire.
   * Notice: invoke from withing synchronized block only!
   */
  private void expireObjects(Db db)  {
    if (expiredTableSerial > 0) {
      expireObjects(db, expiredTableSerial, maxTableSerial);
      expiredTableSerial = 0;
    }
  }
  
  
  
  /**
   * initialize minTableSerial if not yet done.
   */
  private void initializeMinTableSerial(Db db)  {
    if (minTableSerial < 0) {
      // initialize just before the first physical db-access.
      minTableSerial = selectCurrentTableSerial(db);
    }
  }
  
  
  /**
   * update minTableSerial
   */
  private long selectCurrentTableSerial(Db db) {
    try {
      if (db.isRemote()) {
        /**
         * If the db is remote, we can assume a modthread in the server. (because
         * caches don't work without a modthread). This will ask the server's value
         * of the tableserial instead of the database backend.
         */
        ModificationThread mt = ModificationThread.getThread();
        if (!mt.isDummy()) {
          return mt.selectIdSerialForName(DbObject.newByClass(clazz).getTableName())[1];
        }
      }
      // else standard select
      return DbObject.newByClass(db, clazz).selectModification();
    } 
    catch (Exception ex) {
      DbGlobal.errorHandler.severe(db, ex, "selecting current tableSerial failed for " + this);
      return 0;   // not reached
    }    
  }
  
  
  
  /**
   * updates the tableserial
   */
  private void updateTableSerial(T obj) {
    long ts = obj.getTableSerial();
    if (ts > tableSerial) {
      tableSerial = ts;
    }
  }
  
  
  /**
   * @return the highest tableSerial of objects in cache
   */
  public long getTableSerial()  {
    return tableSerial;
  }
  
  
  /**
   * apply caching strategy
   */
  public void shrinkCache()  {
    if (strategy == FORGET || keepQuota <= 0) {
      // FORGET
      invalidate();
    } 
    else {
      synchronized (this)  {
        try {
          long millis = 0;    // to determine the duration
          if (AppworxGlobal.logger.isFineLoggable()) {
            millis = System.currentTimeMillis();
            AppworxGlobal.logger.fine(this + ": shrinking cache... " + printCacheStats());
          }
          Set<T> objects = new TreeSet<T>(strategy == LFU ? new LFUComparator() : new LRUComparator());
          // add all objects to the set and sort by LRU or LFU
          objects.addAll(getObjects());
          // compute objects to keep
          int size = objects.size();
          int keep = maxSize * keepQuota / 100;
          if (keep >= size) {
            keep = size - 1; // special case: remove at least one!
          }
          AppDbObjectCacheIndex<T,?>[] indexArray = getIndexArray();
          // remove or invalidate and add?
          if (keep > size/2)  {
            // remove
            int remove = size - keep;
            for (T obj: objects)  {
              if (remove > 0)  {
                for (AppDbObjectCacheIndex<T,?> index: indexArray)  {
                  index.removeExisting(obj);    // must exist!
                }
              }
              remove--;
            }
          } 
          else  {
            // invalidate and add again
            invalidateImpl();
            int skip = size - keep;
            for (T obj: objects)  {
              if (skip <= 0)  {
                // add object
                for (AppDbObjectCacheIndex<T,?> index: indexArray)  {
                  index.addUnique(obj);    // complain if not unique
                }
              }
              skip--;
            }
          }
          if (AppworxGlobal.logger.isFineLoggable()) {
            clearCacheStats();
            AppworxGlobal.logger.fine(this + ", kept=" + getSize() + ", strategy=" + (strategy == LFU ? "LFU" : "LRU") +
                    ", duration=" + (System.currentTimeMillis() - millis) + "ms");
          }
        } 
        catch (ApplicationException e)  {
          // unique violation detected: log that and invalidate cache
          AppworxGlobal.logger.warning(e.getAllMessages());
          invalidateImpl();
        }
      }
    }
  }
  
  
  
  /**
   * Get the number of objects stored in cache.
   *
   * @return the number of objects in cache.
   */
  public int getSize()  {
    return idIndex.size();     // same for all indexes
  }
  
  
  /**
   * Gets all objects in cache.<br>
   * Because the objects may live in different db-contexts (and different
   * db-connections!) this method should be used with great care in apps
   * as it returns the objects "as is", i.e. without checking for
   * key change or expiration.
   * Better use selectAllInContext or select( ... fromKey, toKey).
   * 
   * @return the list of objects
   */
  public List<T> getObjects()  {
    try {
      return idIndex.getObjects();
    } 
    catch (ApplicationException e)  {
      // key change detected: log that and invalidate cache
      AppworxGlobal.logger.warning(e.getAllMessages());
      invalidate();
      // start over
      return getObjects();    // empty Collection
    }
  }
  
  
  
  
  
  /**
   * Removes all objects with isObjectCacheable() == false.
   * Useful if the cacheable attribute changes temporarily.
   *
   * @return the number of objects removed
   */
  public int removeNonCacheables() {
    synchronized (this)  {
      int count = 0;
      try {
        AppDbObjectCacheIndex<T,?>[] indexArray = getIndexArray();
        for (Iterator<T> iter = getObjects().iterator(); iter.hasNext();) {
          T obj = iter.next();
          if (obj.isCacheable() == false) {
            // remove from indexes
            for (AppDbObjectCacheIndex<T,?> index: indexArray)  {
              index.removeExisting(obj);   // complain if vanished!
            }
            iter.remove();    // remove from collection
            count++;
          }
        }
        if (AppworxGlobal.logger.isFineLoggable()) {
          AppworxGlobal.logger.fine(this + ", " + count + " non-cacheables removed");
        }
      } 
      catch (ApplicationException e)  {
        // unique violation detected: log that and invalidate cache
        AppworxGlobal.logger.warning(e.getAllMessages());
        count = getObjects().size();
        invalidateImpl();
      }
      return count;
    }
  }
  
  
  
  
  
  
  
  
  /**
   * Retrieves an object via cache.
   *
   * @param <C> the Comparable class
   * @param index the cache index to use
   * @param db the contextDb
   * @param key is the Comparable used as a key
   * @param loadIfMissing is true if the object should be loaded from storage if not in cache
   *
   * @return the object or null if no such object
   */
  public <C extends Comparable<? super C>> T select(AppDbObjectCacheIndex<T,C> index, ContextDb db, C key, boolean loadIfMissing)  {
    
    synchronized (this)  {
      
      initializeMinTableSerial(db.getDb());   // preset minTableSerial if not yet done
      addIndexIfNotAssigned(index);           // make sure index is setup and belongs to this cache
      expireObjects(db.getDb());              // check for delayed expiration
      
      // check if cache size limits reached
      if (maxSize > 0 && getSize() > maxSize) {
        shrinkCache();
      }
      
      // preloading is not allowed during a transaction
      boolean preload = isPreloading() && db.getDb().isAutoCommit();
      
      if (preload && dbSet.contains(db) == false)  {
        /**
         * if not already done for this contextDb
         * load all objects in contextDb for all indexes
         */
        selectAllInContext(db);
        // add contextDb
        dbSet.add(db);
      }
      
      if (enabled && allEnabled)  {
        // check if the object is in cache
        try {
          T obj = index.get(db, key);
          
          if (obj != null) {    // found in cache
            if (obj.isExpired())  {
              remove(obj);    // remove it from indexes (but leave in lists!)
              if (AppworxGlobal.logger.isFinerLoggable() && !inToString) {
                inToString = true;    // avoid recursion cause contextDb.toString
                AppworxGlobal.logger.finer("expired object " + obj.getSingleName() + " '" + obj + "', ID=" + obj.getId() +
                        ", context=" + obj.getContextDb().getInfo() + ", removed from " + this);
                inToString = false;
              }
              obj     = null;   // treat as if not in cache
              preload = false;  // even if preloading: reload from cache (notice: the lists will be updated separately!)
            }
            // else: not expired: use it!
          }
          
          if (obj == null && preload == false && loadIfMissing)  {
            // not in cache (or expired) and no preloading: get it from db
            obj = index.select(db, key);
            if (obj != null && obj.isCacheable())  {
              // add to all indexes
              for (AppDbObjectCacheIndex<T,?> ndx: indexes)  {
                ndx.addUnique(obj);     // complain if not unique
              }
              updateTableSerial(obj);   // update tableSerial if higher
              if (AppworxGlobal.logger.isFinerLoggable() && !inToString) {
                inToString = true;    // avoid recursion cause contextDb.toString
                AppworxGlobal.logger.finer("added object " + obj.getSingleName() + " '" + obj + "', ID=" + obj.getId() +
                        ", context=" + obj.getContextDb().getInfo() + ", to " + this);
                inToString = false;
              }
            }
          }
          
          if (obj != null)  {
            obj.markCacheAccess();
          }
          
          return obj;
        } 
        catch (ApplicationException e)  {
          // unique violation or key change detected: log that and invalidate cache
          AppworxGlobal.logger.warning(e.getAllMessages());
          invalidateImpl();
          // start over
          return select(index, db, key, loadIfMissing);
        }
      } 
      else  {
        // always read from storage
        return index.select(db, key);
      }
    }
  }
  
  
  /**
   * Retrieve object via cache. Load from storage if not in cache.
   *
   * @param <C> the Comparable class
   * @param index the cache index to use
   * @param db the contextDb
   * @param key is the Comparable used as a key
   *
   * @return the object or null if no such object
   */
  
  public <C extends Comparable<C>> T select(AppDbObjectCacheIndex<T,C> index, ContextDb db, C key)  {
    return select(index, db, key, true);
  }
  
  
  /**
   * common select by ID.
   *
   * @param db the contextDb
   * @param id is the object-ID
   * @param loadIfMissing is true if the object should be loaded from storage if not in cache
   *
   * @return the object or null if no such object
   */
  public T select(ContextDb db, long id, boolean loadIfMissing)  {
    return id > 0 ? select(idIndex, db, new Long(id), loadIfMissing) : null;
  }
  
  
  /**
   * common select by ID.
   * Always loads if missing in cache.
   *
   * @param db the contextDb
   * @param id is the object-ID
   *
   * @return the object or null if no such object
   */
  public T select(ContextDb db, long id)  {
    return select(db, id, true);
  }
  
  
  /**
   * add an object to the cache.
   * Can be used by apps to add an object explicitly.
   * Will rollback if object (i.e. at least one key) is
   * already in cache.
   *
   * (notice: no initializeMinTableSerial() because we cannot guarantee that 
   * the table hasn't been updated between fetching the object and invokation
   * of this method)
   *
   * @param obj is the AppDbObject to add
   * @return true if added, false if object already in cache
   */
  public boolean add(T obj)  {
    if (enabled && allEnabled)  {
      if (maxSize > 0 && getSize() > maxSize) {
        shrinkCache();
      }
      synchronized (this)  {
        // add to indexes
        for (int i=0; i < indexes.size(); i++)  {
          if (indexes.get(i).add(obj) == false)  {
            // unique violation: rollback
            while(--i >= 0) {
              indexes.get(i).remove(obj);
            }
            return false;
          }
        }
        obj.markCacheAccess();
        updateTableSerial(obj);
        return true;
      }
    } 
    else  {
      return false; // not added cause disabled
    }
  }
  
  
  
  /**
   * remove an object from cache.
   * Can be used by apps to remove an object explicitly.
   * Does NOT remove the object from any list!
   *
   * @param obj is the AppDbObject to remove
   * @return true if removed, false if object not in cache
   */
  public boolean remove(T obj)  {
    synchronized (this)  {
      // remove from indexes
      boolean rv = false;
      for (AppDbObjectCacheIndex<T,?> index: indexes)  {
        rv |= index.remove(obj);
      }
      return rv;
    }
  }
  
  
  /**
   * removes all objects and lists for a given Db.
   *
   * @param db is the db-connection (probably closed)
   */
  public void removeObjectsForDb(Db db)  {
    synchronized(this)  {
      // remove all objects for this db
      for (T obj: getObjects()) {
        if (obj.getDb() == db)  { // "==" is ok here
          remove(obj);
        }
      }
      // create the list of all contexts belonging to db
      List<ContextDb> cbList = new ArrayList<ContextDb>();
      for (ContextDb contextDb: lists.keySet()) {
        if (contextDb.getDb() == db)  {  // "==" is ok here
          cbList.add(contextDb);
        }
      }
      // remove all lists for those contexts
      for (ContextDb contextDb: cbList) {
        lists.remove(contextDb);
      }
      // remove all preloadings for those contexts
      if (dbSet != null)  {
        for (ContextDb contextDb: cbList) {
          dbSet.remove(contextDb);
        }      
      }
    }
  }
  
  
  
  
  /**
   * add a list for given context.
   *
   * @param db is the context
   * @param list is the list to append
   */
  private void addList(ContextDb db, List<T> list)  {
    if (lists.put(db, list) != null) {
      DbGlobal.errorHandler.severe(db.getDb(), null,
              "unique cache violation detected for " +
              AppDbObjectCache.this.clazz + ", list for context='" + db.getInfo() + "'");
    }
  }
  
  
  
  
  /**
   * Retrieve a range of objects from cache.
   * Objects not in cache so far will NOT be loaded from storage!
   * Furthermore, expired objects will NOT be reloaded from storage and
   * expired objects will be returned in list with isExpired() == true.
   * Thus, reasonably works only if preloading is enabled or a
   * selectAllInContext has been invoked before.
   *
   * @param <C> the Comparable class
   * @param index the cache index to use
   * @param db the contextDb
   * @param fromKey starting key, inclusive
   * @param toKey ending key, exclusive
   * @return the list of objects
   */
  public <C extends Comparable<? super C>> List<T> select(AppDbObjectCacheIndex<T,C> index, ContextDb db, C fromKey, C toKey)  {
    
    synchronized (this)  {
      
      addIndexIfNotAssigned(index); // make sure index is setup and belongs to this cache
      expireObjects(db.getDb());    // check for delayed expiration
      
      if (isPreloading() && dbSet.contains(db) == false)  {
        // this will preload all objects in contextDb for all indexes
        selectAllInContext(db);
        // add contextDb
        dbSet.add(db);
      }
      
      try {
        return index.getObjects(db, fromKey, toKey);
      } 
      catch (ApplicationException e)  {
        // unique violation or key change detected: log that and invalidate cache
        AppworxGlobal.logger.warning(e.getAllMessages());
        invalidateImpl();
        // start over
        return select(index, db, fromKey, toKey);
      }
    }
  }
  
  
  
  /**
   * read list from storage
   */
  @SuppressWarnings("unchecked")
  private List<T> selectAllFromStorage(ContextDb db) {
    // read from storage
    try {
      if (db.getDb().isRemote()) {
        /**
         * get list from server cache.
         * Because the modthread gets its serials from the server, the server cache
         * will already be marked invalid or (this is the optimization!) another
         * db connection loaded the cache already.
         */
        List<T> list = (List<T>)AppDbObject.newByClass(db, clazz).getRemoteDelegate().selectAllInContextFromServerCache(db);
        ContextDb.applyToCollection(db, list);
        return list;
      }
      else  {
        return AppDbObject.selectAllInContext(db, clazz);
      }
    } 
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(db.getDb(), ex,
              "uncached selectAllInContext failed for " + clazz + " in " + this + ", context=" + db.getInfo());
      return null;    // not reached
    }
  }
  
  
  /**
   * Retrieve all objects for a context via cache.
   * Will replace expired objects.
   * 
   * @param db the database context
   * @return the list of objects
   */
  @SuppressWarnings("unchecked")
  public List<T> selectAllInContext(ContextDb db)  {
    
    if (enabled && allEnabled)  {
      
      synchronized (this)  {
        
        expireObjects(db.getDb());      // check for delayed expiration
        
        /*
         * check if context already loaded.
         * The list of all objects in a given context is stored in a special
         * object in index 0 with a reference to ID=MIN_VALUE.
         */
        List<T> list = lists.get(db);
        
        if (list == null)  {
          // not in cache
          list = selectAllFromStorage(db);
          
          List<T> cList = new ArrayList<T>(list.size());    // cacheables only
          
          // add cacheable objects to all indexes
          AppDbObjectCacheIndex<T,?>[] indexArray = getIndexArray();
          for (T object: list)  {
            if (object.isCacheable()) {
              cList.add(object);
              for (AppDbObjectCacheIndex<T,?> index: indexArray)  {
                index.add(object);    // don't complain if object is already in cache!
              }
              object.markCacheAccess();
              updateTableSerial(object);
            }
          }
          
          // add to the List of lists
          addList(db, cList);
          
          if (isPreloading())  {
            dbSet.add(db);    // if preloading enabled, add contextDb
          }
        }
        
        else  {
          /**
           * list is cached.
           * We cannot verify that there was no keychange in the list.
           * However, if the list is marked to be checked for expired objects,
           * we must scan the list for expired objects.
           */
          if (expiredLists.contains(list))  {
            int size = list.size();
            T object;
            for (int i=0; i < size; i++) {  
              object = list.get(i); // list is an ArrayList(): get(i) is fast!
              if (object.isExpired()) {
                object = (T)object.reload();    // unchecked
                if (object == null) { // vanished, i.e. object has been deleted in db
                  list.remove(i--);   // remove from list too (costly but better than loading the whole list!)
                  // notice: this will only happen for explicitly expired objects because deleted objects cannot
                  // be found by DbObject.selectExpiredTableSerials()!
                  size--;
                  if (AppworxGlobal.logger.isFinerLoggable() && !inToString) {
                    inToString = true;    // avoid recursion cause contextDb.toString
                    AppworxGlobal.logger.finer("object " + object.getSingleName() + " '" + object + "', ID=" + object.getId() +
                        " removed from list for context '" + db.getInfo());
                    inToString = false;
                  }
                }
                else  {
                  // replace in list
                  list.set(i, object);
                  // replace in cache (this keeps lists in syncObject with index cache)
                  remove(object);
                  add(object);
                  updateTableSerial(object);
                  if (AppworxGlobal.logger.isFinerLoggable() && !inToString) {
                    inToString = true;    // avoid recursion cause contextDb.toString
                    AppworxGlobal.logger.finer("object " + object.getSingleName() + " '" + object + "', ID=" + object.getId() +
                        " reloaded in list for context '" + db.getInfo());
                    inToString = false;
                  }
                }
              }
            }
            // check done, remove it
            if (!expiredLists.remove(list)) {
              throw new DbRuntimeException("expired list not found");
            }
          }
        }
        return list;
      }
    } 
    else  {
      // read from storage
      return selectAllFromStorage(db);
    }
  }
  
  
  
  /**
   * Check whether all caches are enabled at all.
   *
   * @return Value of property enabled.
   */
  public static boolean isAllEnabled() {
    return allEnabled;
  }
  
  /**
   * Set all caches enabled or disabled.
   *
   * @param enabled New value of property enabled.
   */
  public static void setAllEnabled(boolean enabled) {
    allEnabled = enabled;
  }
  
  
  
  /**
   * Check whether cache is enabled at all.
   *
   * @return Value of property enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }
  
  /**
   * Set cache enabled or disabled.
   * When disabled the cache always reads from storage.
   *
   * @param enabled New value of property enabled.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  
  /**
   * @return true if this is a preloading cache
   */
  public boolean isPreloading() {
    return dbSet != null;
  }
  
  
  /**
   * Get the maximum cache size.
   *
   * @return Value of property maxSize.
   */
  public int getMaxSize() {
    return maxSize;
  }
  
  /**
   * Set the maximum cache size.
   * Default is 0 = unlimited.
   * If the size is limited the cache will invalidate if exceeded (except in selectAll)
   *
   * @param maxSize New value of property maxSize.
   */
  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }
  
  
  
  /**
   * Set the caching strategy.
   * The strategy can be changed at any time.
   *
   * @param strategy is one of FORGET, LRU or LFU
   */
  public void setStrategy(int strategy) {
    this.strategy = strategy;
  }
  
  /**
   * @return the cache strategy. Default is FORGET.
   */
  public int getStrategy()  {
    return strategy;
  }
  
  
  /**
   * Set the keep quota in percent for the caching strategy.
   * Will be aligned to [0...100]!
   * Special: 0 is the same as FORGET
   *        100 deletes only one object (this is slow when cache gets full, don't use it!)
   * Reasonable values are 25 up to 75.
   *
   * @param keepQuota in percent
   */
  public void setKeepQuota(int keepQuota) {
    if (keepQuota < 0) {
      keepQuota = 0;
    }
    else if (keepQuota > 100) {
      keepQuota = 100;
    }
    this.keepQuota = keepQuota;
  }
  
  /**
   * Gets the current keep quota.
   * 
   * @return the current keep quota, default is 50
   */
  public int getKeepQuota() {
    return keepQuota;
  }
  
  
  
  
  
  
  // Least Recently Used Comparator
  private static class LRUComparator implements Comparator<AppDbObject> {
    /**
     * Compares by last access time.
     */
    public int compare(AppDbObject o1, AppDbObject o2) {
      // compare access time (the newer the higher, i.e. TreeSet begins with oldest entry)
      int rv = Compare.compareLong(o1.getCacheAccessTime(), o2.getCacheAccessTime());
      if (rv == 0)  {
        // take accesscount
        rv = Compare.compareLong(o1.getCacheAccessCount(), o2.getCacheAccessCount());
        if (rv == 0)  {
          // still the same: take the ID (can't be the same)
          rv = Compare.compareLong(o1.getId(), o2.getId());
        }
      }
      return rv;
    }
  }
  
  
  
  // Least Frequently Used Comparator
  private static class LFUComparator implements Comparator<AppDbObject> {
    /**
     * Compares by access count
     */
    public int compare(AppDbObject o1, AppDbObject o2) {
      // compare access count
      int rv = Compare.compareLong(o1.getCacheAccessCount(), o2.getCacheAccessCount());
      if (rv == 0)  {
        // take access time
        rv = Compare.compareLong(o1.getCacheAccessTime(), o2.getCacheAccessTime());
        if (rv == 0)  {
          // still the same: take the ID (can't be the same)
          rv = Compare.compareLong(o1.getId(), o2.getId());
        }
      }
      return rv;
    }
  }
  
  
}
