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

// $Id: AppDbObjectCacheIndex.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.appworx;

import org.tentackle.util.ApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Cache index.<br>
 * 
 * Holds the mapping of keys to objects.
 *
 * @param <T> the {@link AppDbObject} class
 * @param <C> the {@link Comparable} class
 * @author harald
 */
public abstract class AppDbObjectCacheIndex<T extends AppDbObject, C extends Comparable<? super C>> {
  
    
  private String name;                          // symbolic name of the index
  private AppDbObjectCache<T> cache;            // the associated cache, null if not already added
  private TreeMap<CacheKey<C>, T> cacheMap;     // index tree for AppDbObjects
  private long accessCount;                     // for statistics if fineLoggable
  private long missCount;                       // number of cache-misses
  private boolean inToString;                   // to avoid recursive calls (the object could be part of the toString() evaluation)
  
  
  /**
   * Create a new index.
   * The constructor takes an argument to enforce invocation of super()
   * in order to create the cacheMap.
   *
   * @param name is the symbolic name of the index (for diagnostics only).
   * 
   */
  public AppDbObjectCacheIndex(String name)  {
    this.name      = name;
    cacheMap       = new TreeMap<CacheKey<C>, T>();
  }

  
  /**
   * Select an AppDbObject by key from db.
   * This method needs to be implemented for each index.
   *
   * @param db is the contextDb to select the object from
   * @param key is the Comparable used to uniquely identify the object (can be null)
   *
   * @return the selected object or null if it does not exist.
   */
  abstract public T select(ContextDb db, C key);
  
  
  /**
   * Extract the Comparable that uniquely identifies the object from that object.
   * This method needs to be implemented for each index.
   *
   * @param object is the object to extract the key from (never null)
   * 
   * @return the Comparable (can be null)
   * 
   */
  abstract public C extract(T object);
  
  
  
  /**
   * maintain the link to the cache
   * (package scope because implementation detail that cannot be changed)
   *
   * @param cache is the cache to assign this index to. null = remove index from cache
   * @throws ApplicationException if already assigned to another cache or not assigned if cache==null
   */
  void assignCache(AppDbObjectCache<T> cache) throws ApplicationException {
    if (cache != null)  {
      if (this.cache != null) {
        throw new ApplicationException(this + " is already assigned to " + this.cache);
      }
    }
    else  {
      // clear assignment
      if (this.cache == null) {
        throw new ApplicationException(this + " is not assigned to any cache");
      }
    }
    this.cache = cache;
  }
  
  
  /**
   * check if index is assigned to cache
   */
  boolean isAssignedToCache(AppDbObjectCache<T> cache)  {
    return this.cache == cache;
  }
  

  
  @Override
  public String toString()  {
    return "index '" + name + "'";
  }
  
  
  /**
   * clear contents of the cache index.
   */
  protected void clear()  {
    cacheMap.clear();
  }
  
  
  /**
   * Gets the number of objects in this index.
   * 
   * @return the number of objects
   */
  protected int size()  {
    return cacheMap.size();
  }
  
  
  /**
   * Gets the cache index statistics as a printable string.
   * 
   * @return the string for cache stats
   */
  protected String printCacheStats()  {
    int rate = (int)(100L - missCount * 100L / (accessCount == 0 ? 1 : accessCount));
    return "(size=" + size() + ", accesses=" + accessCount + ", misses=" + missCount + ", hitrate=" + rate + "%)";
  }
  
  
  /**
   * Clears the cache statistics.
   */
  protected void clearCacheStats()  {
    accessCount = 0;
    missCount   = 0;
  }
  
  
  
  /**
   * We can not be sure that the application did not alter an object which is already
   * in cache in such a way that this would alter the ordering with respect to the
   * cacheMap. Furthermore, the CacheKey might be a copy of a primitive type since
   * it must have been converted to a Comparable (e.g. long -> Long).
   * As a consequence, we MUST check every object retrieved from cache that
   * its key has NOT been altered. If so, the complete cache MUST be invalidated
   * (cause the ordering of the tree is corrupted in an unknown way).
   * This is done in AppDbObjectCache.
   */
  private void assertKeyIsUnchanged(T object, CacheKey<C> ck) throws ApplicationException {
    // assert that key was not changed in object by application
    CacheKey<C> objck = new CacheKey<C>(object.getContextDb(), extract(object));
    if (objck.compareTo(ck) != 0) {
      throw new ApplicationException(
        "modified key detected in " + this + " for " + 
        object.getSingleName() + " '" + object +
        "', ID=" + object.getId() + ", expected='" + ck + "', found='" + objck + "'");
    }    
  }
  
  
  /**
   * log invalid CacheKey ApplicationException
   */
  private void logInvalidKey(ApplicationException e)  {
    AppworxGlobal.logger.warning("illegal access to " + this +
                          "\n" + ApplicationException.getStackTraceAsString(e));    
    // e.getMessage() is part of getStackTraceAsString()
  }

  
  
  /**
   * get object from cache by key.
   *
   * @param db is the contextdb
   * @param key is the Comparable that uniquely identifies the object
   *
   * @return the object or null if not in cache
   *
   * @throws ApplicationException if the objects key has been changed by application
   */
  protected T get(ContextDb db, C key) throws ApplicationException {
    
    T obj = null;
    CacheKey<C> ck;
    
    try {
      ck = new CacheKey<C>(db, key);
    }
    catch (ApplicationException e)  {
      // no need to invalidate the whole cache, but a hint to application prob
      logInvalidKey(e);
      return null;
    }
      
    if (AppworxGlobal.logger.isFineLoggable())  {
      accessCount++;
      obj = cacheMap.get(ck);
      if (obj == null)  {
        missCount++;
        if (AppworxGlobal.logger.isFinerLoggable())  {
          AppworxGlobal.logger.finer(this + ": cache miss for '" + ck + "'");
          if (AppworxGlobal.logger.isFinestLoggable()) {
            AppworxGlobal.logger.finest(printCacheStats());
          }
        }
      }
      else  {
        if (AppworxGlobal.logger.isFinerLoggable())  {
          AppworxGlobal.logger.finer(this + ": cache hit for '" + ck + "'");
          if (AppworxGlobal.logger.isFinestLoggable()) {
            AppworxGlobal.logger.finest(printCacheStats());
          }
        }
      }
    }
    else  {
      obj = cacheMap.get(ck);
    }

    if (obj != null)  {
      // this will throw an Exception and forces invalidation hkrof the cache!
      assertKeyIsUnchanged(obj, ck);
    }

    return obj;
  }

  
  /**
   * get all objects from cache index
   *
   * @return the objects in a collection
   *
   * @throws ApplicationException if a key of one of the objects has been changed by application
   */
  protected List<T> getObjects() throws ApplicationException {
    List<T> col = new ArrayList<T>(cacheMap.size());
    // check keys for not changed
    for (Map.Entry<CacheKey<C>, T> entry: cacheMap.entrySet()) {
      T object = entry.getValue();
      assertKeyIsUnchanged(object, entry.getKey());
      col.add(object);
    }
    return col;
  }
  
  
  /**
   * get a subset of objects
   *
   * @param db is the contextDb
   * @param fromKey is the start of range (including)
   * @param toKey is the end of range (exluding)
   * @return the objects with fromKey <= object < toKey.
   *
   * @throws ApplicationException if a key of one of the objects has been changed by application
   */
  protected List<T> getObjects(ContextDb db, C fromKey, C toKey) throws ApplicationException {
    
    CacheKey<C> fromCk;
    CacheKey<C> toCk;
    
    try {
      fromCk = new CacheKey<C>(db, fromKey);
      toCk   = new CacheKey<C>(db, toKey);
    }
    catch (ApplicationException e)  {
      // log and stacktrace to error in application source
      logInvalidKey(e);
      return null;
    }
    
    Set<Map.Entry<CacheKey<C>, T>> entries = cacheMap.subMap(fromCk, toCk).entrySet();
    List<T> list = new ArrayList<T>();    // entries.size() will iterate to count size. too expensive!
    
    for (Map.Entry<CacheKey<C>, T> entry: entries) {
      CacheKey<C> ck = entry.getKey();
      T object = entry.getValue();
      assertKeyIsUnchanged(object, entry.getKey());
      list.add(object);
    }
    
    return list;
  }
  


  /**
   * add an object to the index.
   * Does not throw an exception if it is already in cache.
   *
   * @param object is the object to append (never null)
   *
   * @return true if added, false if object already in index or key in object evaluated to null
   */
  protected boolean add(T object) {
    try {
      return cacheMap.put(new CacheKey<C>(object.getContextDb(), extract(object)), object) == null;
    }
    catch (ApplicationException e)  {
      logInvalidKey(e);
      return false;
    }
  }



  /**
   * add an unique object to the index.
   * Unique violations usually indicate an error in comparing
   * contextDb's or that the context of a cached object was changed
   * accidently.
   *
   * @param object is the object to append (never null)
   *
   * @throws ApplicationException if unique violation
   */
  protected void addUnique(T object) throws ApplicationException {
    CacheKey<C> ck;
    try {
      ck = new CacheKey<C>(object.getContextDb(), extract(object));
    }
    catch (ApplicationException e)  {
      logInvalidKey(e);
      return;   // don't add to index (but no reason to invalidate the cache)
    }    
    if (cacheMap.put(ck, object) != null) {
      throw new ApplicationException(
          "unique cache violation detected in " + this + " for " + 
          object.getSingleName() + " '" + object +
          "', ID=" + object.getId() + ", key='" + ck + "'");
    }
  }
  
  


  
  /**
   * remove object from cache.
   * Does not throw an exception if it not in cache.
   * For use by AppDbObjectCache.remove() only.
   *
   * @param object the object to remove (never null)
   *
   * @return true if object removed
   */
  protected boolean remove(T object) {
    try {
      return cacheMap.remove(new CacheKey<C>(object.getContextDb(), extract(object))) != null;
    }
    catch (ApplicationException e)  {
      logInvalidKey(e);
      return false;
    }
  }
  
  
  /**
   * remove object from cache.
   * Assumes that the object really is in cache.
   *
   * @param object the object to remove (never null)
   *
   * @throws ApplicationException if not in cache
   */
  protected void removeExisting(T object) throws ApplicationException {
    CacheKey<C> ck;
    try {
      ck = new CacheKey<C>(object.getContextDb(), extract(object));
    }
    catch (ApplicationException e)  {
      logInvalidKey(e);
      return;   // nothing removed (but no reason to invalidate cache)
    }    
    if (cacheMap.remove(ck) == null) {
      throw new ApplicationException(
          "remove from cache failed from " + this + " for " + 
          object.getSingleName() + " '" + object +
          "', ID=" + object.getId() + ", key='" + ck + "'");
    }
  }
  
  
  
  
  /**
   * key for the cache map.
   * We must compare C with ontextDb, because the same object may live in different
   * contexts. The contextDb is essential, especially when it comes to different
   * Db (database connections)!
   * The CacheKey does not allow null contextDb or null keys and throws ApplicationException
   * if it detects such.
   */
  private class CacheKey<C extends Comparable<? super C>> implements Comparable<CacheKey<C>> {
    
    private ContextDb db;       // the db the object lives in
    private C key;              // the comparable that uniquely identifies the object
    
    
    // for objects
    public CacheKey(ContextDb db, C key) throws ApplicationException {
      if (db == null) {
        throw new ApplicationException("null context");
      }
      if (key == null)  {
        throw new ApplicationException("null key");
      }
      this.db  = db;
      this.key = key;
    }
    
    public int compareTo (CacheKey<C> obj) {
      // db first cause of getObjects( fromKey, toKey )
      int rv = db.compareTo(obj.db);  // never null
      if (rv == 0)  {
        rv = key.compareTo(obj.key);    // never null
      }
      return rv;
    }
    
    @Override
    public synchronized String toString()  {
      if (!inToString)  {
        inToString = true;
        String str = db.getInfo() + ":<" + key + ">";
        inToString = false;
        return str;
      }
      return "?";
    }
  }
  
  
}
