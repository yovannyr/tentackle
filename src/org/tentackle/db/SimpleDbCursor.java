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

// $Id: SimpleDbCursor.java 379 2008-07-31 16:55:28Z harald $

package org.tentackle.db;

import java.rmi.RemoteException;
import org.tentackle.db.rmi.RemoteDbCursor;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple implementation of a DbCursor.<br>
 * The cursor operates on a {@link ResultSetWrapper} for local connections
 * and on a {@link RemoteDbCursor} for remote connections to an
 * application server.
 *
 * @param <T> the database object class
 * @author harald
 */
public class SimpleDbCursor<T extends DbObject> implements DbCursor<T> {

  protected Db                db;                   // database connection
  protected Class<T>          dbClass;              // object class
  protected ResultSetWrapper  rs;                   // result set
  protected RemoteDbCursor    rc;                   // remote cursor
  protected String            rcName;               // name of remote cursor
  protected boolean           withLinkedObjects;    // load linked objects
  protected int               row;                  // current row, 0 = not set yet
  protected int               rows;                 // number of rows, -1 not known yet
  protected T                 object;               // current Object, null = none
  
  
  private boolean firstInvoked;   // true = first() has been invoked
  

  /**
   * Creates a cursor.
   *
   * @param db the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   * @param withLinkedObjects true if load linked objects, false otherwise
   */
  public SimpleDbCursor(Db db, Class<T> dbClass, ResultSetWrapper rs, boolean withLinkedObjects) {
    
    this.db                 = db;
    this.dbClass            = dbClass;
    this.rs                 = rs;
    this.withLinkedObjects  = withLinkedObjects;
    
    rows = -1;  // not known yet
  }
  
  /**
   * Creates a cursor on a result set for local connections.
   *
   * @param db the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   */
  public SimpleDbCursor(Db db, Class<T> dbClass, ResultSetWrapper rs) {
    this(db, dbClass, rs, true);
  }
  
  
  /**
   * Creates a remote Cursor on the client-side for remote connections.
   * 
   * @param db the remote db connection
   * @param rc the remote cursor
   */
  @SuppressWarnings("unchecked")
  public SimpleDbCursor(Db db, RemoteDbCursor rc)  {
    this.db = db;
    this.rc = rc;
    try {
      dbClass = (Class<T>)Class.forName(rc.getDbClassName());   // unchecked
    }
    catch (Exception e) {
      DbGlobal.errorHandler.severe(db, e, "remote getDbClassName failed");
    }
  }
  
  
  /**
   * Gets the local/remote type of the cursor.
   * 
   * @return true if cursor is remote, else local
   */
  public boolean isRemote() {
    return rc != null; 
  }
  
  
  /**
   * {@inheritDoc}
   * This frees also the result set (local or remote).
   */
  public void close() {
    if (isRemote())  {
      try {
        if (rc != null) {
          rc.close();
          rc = null;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote close failed"); 
      }
    }
    else  {
      if (rs != null) {
        rs.close();
        rs = null;
      }
    }
    object  = null;
    rows    = -1;
    row     = 0;
  }
  
  
  /**
   * Checks whether cursor is open.
   * 
   * @return true if cursor is open
   */
  public boolean isOpen() {
    return rc != null || rs != null;
  }
  
  
  /**
   * Overridden to close forgotten cursors.
   */
  @Override
  protected void finalize() throws Throwable {
    if (isOpen()) {
      DbGlobal.errorHandler.warning(db, new DbRuntimeException("pending cursor " + this + " closed"), null);
      close();
    }
    super.finalize();
  }
  

  @Override
  public String toString() {
    if (rs != null) {
      return getClass().getName() + " " + rs.toString();
    }
    else if (rc != null) {
      if (rcName == null) {   // get it only once
        try {
          rcName = rc.getName();
        } 
        catch (RemoteException e) {
          DbGlobal.errorHandler.severe(db, e, "remote getName failed"); 
        }
      }
      return "remote " + rcName;
    }
    else  {
      return getClass().getName() + " (closed)";
    }
  }
  

  /**
   * {@inheritDoc}
   * <p>
   * The implementation will move to the end of the cursor to
   * determine the number of rows, if not known yet. Subsequent
   * invocations will use the cached value.
   */
  public int getRowCount() {
    if (isRemote())  {
      try {
        rows = rc.getRowCount();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getRowCount failed");
      }
    }
    else  {
      if (rows == -1) {
        // determine number of rows the first time
        int oldrow = row;
        last();
        if (oldrow > 0) {
          setRow(oldrow);
        }
      }
    }
    return rows;
  }

  
  /**
   * {@inheritDoc}
   */
  public int getRow() {
    if (isRemote())  {
      try {
        row = rc.getRow();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getRow failed");
      }
    }
    return row;
  }
  
  

  /**
   * {@inheritDoc}
   */
  public boolean setRow(int row)  {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.setRow(row);
        if (rv) {
          this.row = row;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote setRow failed"); 
      }
    }
    else  {
      if (rs.absolute(row)) {
        this.row = rs.getRow();
        rv = true;
      }
      // stay where you are
    }
    firstInvoked = false;
    return rv;
  }
  

  /**
   * {@inheritDoc}
   */
  public boolean first() {
    
    boolean rv = false;
    
    if (isRemote())  {
      try {
        int r = rc.first();
        if (r >= 0) {
          rv = true;
          row = r;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote first failed"); 
      }
    }
    else  {
      /**
       * some drivers don't allow positioning in resultsets that are not TYPE_FORWARD_ONLY.
       * However, first() is a convenient way to test whether the result set contains some
       * data at all. If the cursor is at the beginning or no access made at all, we will
       * do nothing or perform a next() instead.
       */
      if (row == 0) {
        // no access made yet
        rv = next();
      }
      else if (row == 1) {
        rv = true;    // already at first record
      }
      // rewind
      else if (rs.first()) {
        row = 1;
        rv = true;
      }
      else  {
        // else not on a valid row means: no rows
        row  = 0;
        rows = 0;   // no rows!
      }
    }

    firstInvoked = rv;    // last positioning of row was through first()
    
    return rv;
  }
  

  /**
   * {@inheritDoc}
   */  
  public boolean last() {
    boolean rv = false;
    if (isRemote())  {
      try {
        int r = rc.last();
        if (r >= 0) {
          rv = true;
          rows = r;
          row  = r;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote last failed"); 
      }
    }
    else  {
      if (rs.last()) {
        rows = rs.getRow();   // also updates row-counter!
        row  = rows;
        rv = true;
      }
      else  {
        // else not on a valid row means: no rows
        row  = 0;
        rows = 0;   // no rows!
      }
    }
    
    firstInvoked = false;
    return rv;
  }

  
  /**
   * {@inheritDoc}
   */
  public boolean next() {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.next();
        if (rv) {
          row++;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote next failed"); 
      }
    }
    else  {
      if (rs.next()) {
        row++;
        rv = true;
      }
      // no more rows: stay where you are now
    }
    firstInvoked = false;
    return rv;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public boolean previous() {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.previous();
        if (rv) {
          row--;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote previous failed"); 
      }
    }
    else  {
      if (rs.previous()) {
        row--;
        rv = true;
      }
      else  {
        row = 0;    // before first row now (off the result set)
      }
    }
    firstInvoked = false;
    return rv;
  }

  
  /**
   * {@inheritDoc}
   */
  public void beforeFirst()  {
    if (isRemote())  {
      try {
        rc.beforeFirst();
        row = 0;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote beforeFirst failed"); 
      }
    }
    else  {
      rs.beforeFirst();
      row = 0;
    }
    firstInvoked = false;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public void afterLast() {
    if (isRemote())  {
      try {
        rows = rc.afterLast();
        row  = rows + 1;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote afterLast failed"); 
      }
    }
    else  {
      rs.afterLast();
      row = getRowCount() + 1;
    }
    firstInvoked = false;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public boolean isBeforeFirst()  {
    if (isRemote())  {
      try {
        return rc.isBeforeFirst();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote isBeforeFirst failed");
        return false;  // not reached
      }
    }
    else  {
      return rs.isBeforeFirst();
    }
  }
  
  
  /**
   * {@inheritDoc}
   */
  public boolean isAfterLast()  {
    if (isRemote())  {
      try {
        return rc.isAfterLast();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote isAfterLast failed");
        return false;  // not reached
      }
    }
    else  {
      return rs.isAfterLast();
    }
  }
  
  
  /**
   * Creates a new object.
   * The object will just be instantiated. The db is no set.
   * 
   * @return the new object 
   */
  public T newObject() {
    try {
      return DbObject.newByClass(dbClass);
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(db, e, "creating new object failed");
      return null;  // not reached.
    }
  }
  
  
  /**
   * Sets the db-context for objects retrieved from the remote cursor.
   *
   * @param object the DbObject to set the db for, never null
   */
  public void setDbContext(T object) {
    object.setDb(db);
  }
  
  
  /**
   * Updates the db context in the object after the object has been loaded.
   * Apps may override the method to do further setup.
   * The default implementation does nothing.
   * 
   * @param object the DbObject to update the context for, never null
   */
  public void updateDbContext(T object) {
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * The remote server may refuse the delivery of the object
   * due to security constraints. In this case, null is returned.
   */
  @SuppressWarnings("unchecked")
  public T getObject() {
    if (isRemote())  {
      try {
        object = (T)rc.getObject();    // unchecked
        if (object != null) {
          setDbContext(object);
          updateDbContext(object);
        }
        return object;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getObject failed");
      }
    }
    else  {
      if (row > 0) {
        T newObject = newObject();
        setDbContext(newObject);
        if (newObject.readFromResultSetWrapper(rs, withLinkedObjects)) {
          object = newObject;
          updateDbContext(object);
          return newObject;
        }
      }
    }
    return null;
  }
  

  /**
   * {@inheritDoc}
   */
  public boolean updateObjectOnly (T object) {
    if (isRemote())  {
      try {
        return rc.updateObjectOnly(object);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote updateObjectOnly failed");
      }
    }
    else  {
      if (row > 0) {
        this.object = object;
        return object.updateFields(rs) >= 0;
      }
    }
    return false;
  }

  
  /**
   * Updates the current row with the given object
   */
  private boolean updateRow (T object) {
    if (row > 0) {
      this.object = object;
      boolean oldcommit = db.begin();    // start transaction!
      if (rs.updateRow() && (!withLinkedObjects || object.saveLinkedObjects()))  {
        db.commit(oldcommit);
        return true;
      }
      db.rollback(oldcommit);
    }
    return false;
  }

  
  /**
   * {@inheritDoc}
   */
  public boolean updateObject(T object) {
    if (isRemote())  {
      try {
        return rc.updateObject(object);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote updateObject failed");
        return false;  // not reached
      }
    }
    else  {
      return updateObjectOnly(object) && updateRow(object);
    }
  }

  
  /**
   * {@inheritDoc}
   */
  public boolean deleteObject() {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.deleteObject();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote deleteObject failed");
      }
    }
    else  {
      T obj;
      if (row > 0 && (obj = getObject()) != null) {
        boolean oldcommit = db.begin();    // start transaction!
        if (rs.deleteRow() && obj.deleteLinkedObjects())  {
          db.commit(oldcommit);
          rv = true;
        }
        else  {
          db.rollback(oldcommit);
        }
      }
      // delete failed, stay where you are
    }
    
    if (rv && rows > 0) {
      // rows are valid
      rows--;
    }
    
    return rv;
  }

  
  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public T getObjectAt(int row) {
    T obj = null;
    if (isRemote())  {
      try {
        obj = (T)rc.getObjectAt(row);   // unchecked
        if (obj != null) {
          setDbContext(obj);
          updateDbContext(obj);
          this.row = row;
          firstInvoked = false;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getObjectAt failed");
      }
    }
    else  {
      setRow(row);
      obj = getObject();
    }
    return obj;
  }

  
  /**
   * {@inheritDoc}
   */
  public boolean updateObjectAt(T object, int row) {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.updateObjectAt(object, row);
        if (rv) {
          this.row = row;
          firstInvoked = false;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote updateObjectAt failed");
      }
    }
    else  {
      rv = setRow(row) && updateObject(object);
    }
    return rv;
  }

  
  /**
   * {@inheritDoc}
   */
  public boolean deleteObjectAt(int row) {
    boolean rv = false;
    if (isRemote())  {
      try {
        rv = rc.deleteObjectAt(row);
        if (rv) {
          this.row = row;
          if (rows > 0) {
            rows--;
          }
          firstInvoked = false;
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote deleteObjectAt failed");
      }
    }
    else  {
      rv = setRow(row) && deleteObject();
    }
    return rv;
  }
  
  
  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public List<T> toList()  {
    List<T> list = null;
    if (isRemote())  {
      try {
        // this is faster then a getObject()-loop because it saves rmi-roundtrips
        list = (List<T>)rc.toList();    // unchecked
        if (list != null) {
          for (T obj: list) {
            setDbContext(obj);
            updateDbContext(obj);
          }
        }
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote toList failed");
      }
    }
    else  {
      list = new ArrayList<T>();
      boolean exists = first();
      while (exists)  {
        T obj = getObject();
        if (obj != null) {
          list.add(obj);
        }
        exists = next();
      }
    }
    return list;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public List<T> toListAndClose() {
    List<T> list = toList();
    close();
    return list;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public Class getDbClass() {
    return dbClass;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public void setFetchSize(int rows) {
    if (isRemote())  {
      try {
        rc.setFetchSize(rows);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote setFetchSize failed");
      }
    }
    else  {
      rs.setFetchSize(rows);
    }
  }
  
  
  /**
   * {@inheritDoc}
   */
  public int getFetchSize() {
    if (isRemote())  {
      try {
        return rc.getFetchSize();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getFetchSize failed");
        return 0;   // not reached
      }
    }
    else  {
      return rs.getFetchSize();
    }
  }
  
  
  /**
   * {@inheritDoc}
   */
  public void setFetchDirection(int direction) {
    if (isRemote())  {
      try {
        rc.setFetchDirection(direction);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote setFetchDirection failed");
      }
    }
    else  {
      rs.setFetchDirection(direction);
    }
  }
  
  
  /**
   * {@inheritDoc}
   */
  public int getFetchDirection() {
    if (isRemote())  {
      try {
        return rc.getFetchDirection();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote getFetchDirection failed");
        return 0;   // not reached
      }
    }
    else  {
      return rs.getFetchDirection();
    }
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * If the fetchsize is 0 (auto), the method will fall back
   * to toList.
   * After having read the last row the cursor is closed!
   * This saves two roundtrips for remote dbs.
   */
  @SuppressWarnings("unchecked")
  public List<T> fetch() {
    
    List<T> list = null;
    
    if (isRemote())  {
      if (rc != null) {  // if not already closed
        try {
          // this is faster then a getObject()-loop because it saves rmi-roundtrips
          list = (List<T>)rc.fetch();    // unchecked
          if (list != null) {
            for (T obj: list) {
              if (obj != null) {
                setDbContext(obj);
                updateDbContext(obj);
              }
            }
            row += list.size();
            if (firstInvoked) {
              firstInvoked = false;
              row--;    // subtract cause row started at 1, not 0
            }
            if (list instanceof FetchList && ((FetchList)list).closed) {
              rc = null;  // don't close it remote!
              close();    // already closed on remote side
            }
          }
          else  {
            rc = null;  // don't close it remote!
            close();    // already closed on remote side
          }
        }
        catch (Exception e) {
          DbGlobal.errorHandler.severe(db, e, "remote fetch failed");
        }
      }
    }
    
    else  {
      
      if (rs != null) {   // if not already closed
      
        int fetchMax = getFetchSize();

        if (fetchMax < 1) {
          list = toList();    // fallback
          close();            // close it
        }
        else  {
          // load next fetch block
          list = new FetchList<T>();
          // special optimization if first() has been invoked before (to prevent beforeFirst()
          // called on FORWARD_ONLY cursors, some dbms don't support that, i.e. Oracle)
          if (firstInvoked && row == 1) {
            T obj = getObject();
            if (obj != null) {
              list.add(obj);
            }
            firstInvoked = false;
          }
          // load the rest
          fetchMax = ((row / fetchMax) * fetchMax) + fetchMax;
          while (row < fetchMax) {
            if (rs.next()) {
              row++;
              T obj = getObject();
              if (obj != null) {
                list.add(obj);
              }
            }
            else  {
              // end reached: close cursor
              close();
              ((FetchList)list).closed = true;
              break;
            }
          }
        }

        if (list.isEmpty()) {
          list = null;
          close();
        }
      }
    }
    
    return list;
  }
  
  
  /**
   * adds a closed flag to save a roundtrip.
   */
  private static class FetchList<T> extends ArrayList<T> {
    private static final long serialVersionUID = 8683452581122892189L;
    
    boolean closed;
  }
  
}
