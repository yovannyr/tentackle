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

// $Id: CachedDbCursor.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.db;

import java.util.ArrayList;
import java.util.List;
import org.tentackle.db.rmi.RemoteDbCursor;

/**
 * A caching cursor.
 * Objects are kept in a list.
 *
 * @param <T> the data object class
 */
public class CachedDbCursor<T extends DbObject> extends SimpleDbCursor<T> {

  private List<T> cache;      // the cached data objects

  
  
  /**
   * Creates a caching cursor.
   *
   * @param db the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   * @param withLinkedObjects true if load linked objects, false otherwise
   */
  public CachedDbCursor(Db db, Class<T> dbClass, ResultSetWrapper rs, boolean withLinkedObjects) {
    super (db, dbClass, rs, withLinkedObjects);
    cache = new ArrayList<T>(getRowCount());
  }
  
  
  /**
   * Creates a caching cursor.
   *
   * @param db the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   */
  public CachedDbCursor(Db db, Class<T> dbClass, ResultSetWrapper rs) {
    this(db, dbClass, rs, true);
  }
  
  
  /**
   * Creates a cached remote Cursor on the client-side.
   * 
   * @param db the db connection
   * @param rc the remote cursor
   */
  @SuppressWarnings("unchecked")
  public CachedDbCursor(Db db, RemoteDbCursor rc)  {
    super(db, rc);
    cache = new ArrayList<T>(getRowCount());
  }
  
  

  /**
   * overridden close
   */
  @Override
  public void close() {
    super.close();
    cache = null;
  }

  /**
   * Gets the cached object at the current row.
   * 
   * @return the object
   */
  @Override
  public T getObject() {
    int r = getRow();
    if (r >= 0) {
      alignCache(r);
      T obj = cache.get(r);
      if (obj == null) {
        // object not in cache: load it
        obj = super.getObject();
        if (obj != null) {
          cache.set(r, obj);
        }
      }
      else  {
        object = obj;
      }
      return obj;
    }
    return null;
  }

  /**
   * Updates the contents of an object at the current row.
   * 
   * @param object the object to be updated
   * @return true if update done, false if failed
   */
  @Override
  public boolean updateObjectOnly (T object) {
    if (super.updateObjectOnly(object)) {
      int r = getRow();
      alignCache(r);
      cache.set(r, object);
      return true;
    }
    return false;
  }


  /**
   * makes the cache large enough to hold objects upto index
   */
  private void alignCache(int index)  {
    int missing = index - cache.size() + 1;
    while (missing > 0)  {
      cache.add(null);
      missing--;
    }
  }

}

