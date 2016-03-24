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

// $Id: DbCursor.java 362 2008-07-08 14:09:11Z harald $

package org.tentackle.db;


import java.util.List;

/**
 * A database cursor.<br>
 * Provides a cursor operating on a set of data objects retrieved
 * from a server. As opposed to standard JDBC, however, the server may
 * be a database server <em>or</em> a middle trier application server.
 *
 * @param <T> the database object class
 * @author harald
 */
public interface DbCursor<T extends DbObject> {

  /**
   * Closes the cursor.<br>
   * The cursor is opened in its constructor.
   * Closing an already closed cursor is allowed.
   */
  public void close();

  /**
   * Gets the number of rows.<br>
   * Caution: depending on the implementation this method
   * will have to read all data from the server in order to count the rows.
   * Hence, applications should avoid getRowCount() if possible.
   * 
   * @return the number of rows accessible by the cursor 
   */
  public int getRowCount();

  /**
   * Gets the current row.
   * Row numbers start at 1.
   *
   * @return the current row, 0 if before first row
   */
  public int getRow();

  /**
   * Sets the cursor to a given row.
   * Row numbers start at 1.
   * 
   * @param row the row number (must be > 0)
   * @return true if done, false if no such row
   */
  public boolean setRow (int row);

  /**
   * Rewinds the cursor to the first row.
   * 
   * @return true if rewound, false if cursor is empty
   */
  public boolean first();

  /**
   * Positions the cursor on the last row.
   * 
   * @return true if positioned, false if cursor is empty
   */
  public boolean last();

  /**
   * Moves the cursor to the next row.
   * If there are no more rows the current row remains unchanged.
   * 
   * @return true if moved, false if no more rows
   */
  public boolean next();

  /**
   * Moves the cursor to the previous row.
   * If we are already at the beginning, the cursor remains unchanged.
   * 
   * @return true if advanced, false if already at the beginning
   */
  public boolean previous();

  /**
   * Positions the cursor before the first row.
   * Works even for empty cursors.
   */
  public void beforeFirst();
  
  /**
   * Positions the cursor after the last row.
   * Works even for empty cursors.
   */
  public void afterLast();
  
  /**
   * Checks whether the cursor is before the first row.
   * 
   * @return true if before first
   */
  public boolean isBeforeFirst();
  
  /**
   * Checks whether the cursor is after the last row.
   * 
   * @return true if after last
   */
  public boolean isAfterLast();
  
  /**
   * Gets the data object of the current row.
   *
   * @return the object, null if invalid row or no such object 
   */
  public T getObject();
  
  /**
   * Updates the data object of the current row.
   * The data will <em>not</em> be written to the server!
   * The object may refuse to be updated.
   * 
   * @param object the data object
   * @return true if updated, false if failed
   */
  public boolean updateObjectOnly (T object);

  /**
   * Updates the data object of the current row and persist the
   * change to the server.
   * The object may refuse to be updated and the server update
   * may fail.
   * 
   * @param object the data object
   * @return true if updated, false if failed
   */
  public boolean updateObject (T object);

  /**
   * Removes the object of the current row from the cursor and the server.
   * The next object becomes the current object. If there is no next object
   * the cursor is positioned to the previous row.
   * The operation may fail if the server cannot delete the object
   * for whatever reason.
   * 
   * @return true if deleted, false if failed
   */
  public boolean deleteObject ();

  /**
   * Gets an the object at a given row. 
   * The current row will not be changed.
   * 
   * @param row the row starting at 1
   * @return the object, null if row out of range
   */
  public T getObjectAt (int row);

  /**
   * Updates the data object at a given row and persist the
   * change to the server. The current row will not be changed.
   * The object may refuse to be updated and the server update
   * may fail.
   * 
   * @param object the data object
   * @param row the row starting at 1
   * @return true if updated, false if failed
   */
  public boolean updateObjectAt (T object, int row);

  /**
   * Removes the object a given row from the cursor and the server.
   * The current row will not be changed, except when it's the last
   * row (or after last) and the last row is removed.
   * The operation may fail if the server cannot delete the object
   * for whatever reason.
   * 
   * @param row the row starting at 1
   * @return true if deleted, false if failed
   */
  public boolean deleteObjectAt (int row);

  /**
   * Returns the objects of this cursor as a list.
   * 
   * @return the list (never null)
   */
  public List<T> toList();
  
  /**
   * Returns the objects of this cursor as a list and closes this cursor.
   * 
   * @return the list (never null)
   */
  public List<T> toListAndClose();
  
  /**
   * Gets the class of the objects in the cursor.
   * 
   * @return the object class 
   */
  public Class getDbClass();
  
  
  /**
   * Sets the fetchsize.
   * This is the number of rows the cursor will fetch
   * from the server in one batch.
   * A fetchsize of 0 means server default.
   * 
   * @param rows the fetchsize
   */
  public void setFetchSize(int rows);
  
  /**
   * Gets the fetchsize.
   * 
   * @return the fetchsize
   */
  public int getFetchSize();
  
  /**
   * Sets the fetch direction.
   * 
   * @param direction the direction, see {@link java.sql.ResultSet#setFetchDirection}.
   */
  public void setFetchDirection(int direction);
  
  /**
   * Gets the fetch direction.
   * 
   * @return the direction
   */ 
  public int getFetchDirection();
  
  /**
   * Fetches the next objects up to the fetchsize.
   * This method is provided to minimize the number of
   * roundtrips especially for remote cursors.
   * The cursor is closed at the end of the cursor.
   *
   * @return the list of objects, null if no more objects found
   */
  public List<T> fetch();
}

