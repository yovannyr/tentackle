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

// $Id: ResultSetWrapper.java 411 2008-09-06 19:35:28Z harald $


package org.tentackle.db;

import org.tentackle.util.BMoney;
import org.tentackle.util.DMoney;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.tentackle.util.StringHelper;


/**
 * A wrapper for {@link ResultSet}s.
 *
 * @author harald
 */
public class ResultSetWrapper {

  private ResultSet rs;                   // the wrapped result set
  private StatementWrapper stmt;          // the prepared statement wrapper
  private Db db;                          // db from statement
  private int columnOffset;               // offset to add to column index (e.g. for eager loading / joins)
  private boolean commitOnClose;          // true = issue a commit() before close()
  private boolean closeStatementOnClose;  // true = close the statement after resultset is closed

  
  /**
   * Creates a wrapper for the given result set.
   * 
   * @param stmt is the prepared statement wrapper that created this resultset
   * @param rs the original result-set
   */
  public ResultSetWrapper (StatementWrapper stmt, ResultSet rs) {
    
    this.stmt = stmt;
    this.rs   = rs;
    
    db = stmt.getDb();
    
    // disable autoclosing the statement if it is a prepared statement
    closeStatementOnClose = !(stmt instanceof PreparedStatementWrapper);
  }
  
  
  
  /**
   * Gets the db associated to the result set
   *
   * @return the db
   */
  public Db getDb() {
    return db;
  }
  
  
  @Override
  public String toString() {
    return stmt == null ? "(closed resultset)" : stmt.toString();
  }
  

  /**
   * Closes the resultset.
   */
  public void close ()  {
    try {
      stmt.unmarkReady();  // mark statement consumed
      if (commitOnClose)  {
        db.commit(true);
        commitOnClose = false;
      }
      if (closeStatementOnClose)  {
        stmt.close();  // this will also close the result set!
      }
      else  {
        rs.close();   // this is ok even if closed (according to the API)
      }
      stmt.detachDb();
      stmt   = null;  // to GC
      db     = null;
      rs     = null;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_Schliessen_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Determines whether the result set is closed.
   * 
   * @return true if closed
   */
  public boolean isClosed() {
    if (rs == null) {
      return true;    // the application closed it already
    }
    try {
      /**
       * Some dbms already implement isClosed() (it's available since 1.6).
       */
      return db.sqlResultSetIsClosedSupported() && rs.isClosed();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "isClosed() failed");
      return false;   // not reached
    }
  }

  
  /**
   * Overridden to close forgotten resultsets.
   */
  @Override
  protected void finalize() throws Throwable {
    if (!isClosed()) {
      DbGlobal.errorHandler.warning(db, new DbRuntimeException("pending resultset '" + this + "' closed"), null);
      close();
    }
    super.finalize();
  }

  
  /**
   * Set whether to commit() before close() or not.<br>
   * Some dbms (e.g. Postgres) cannot use cursors outside of a transaction.
   * In this case a transaction must be started before executeQuery() 
   * (see PreparedStatementWrapper and Query).
   * and closed when the resultset (or the cursor) is closed.
   * The default is not to commit on close.
   * 
   * @param commitOnClose true if commit on close 
   */
  public void setCommitOnClose(boolean commitOnClose) {
    this.commitOnClose = commitOnClose;
  }
  
  /**
   * Gets the commit-on-close flag.
   * 
   * @return true if commit on close
   */
  public boolean isCommitOnClose()  {
    return commitOnClose;
  }
  
  
  
  /**
   * Set the statement to be closed when result set is closed.
   * By default the statement remains open.
   * 
   * @param closeStatementOnClose true if close statement on close
   */
  public void setCloseStatementOnclose(boolean closeStatementOnClose) {
    this.closeStatementOnClose = closeStatementOnClose;
  }
  
  /**
   * Gets the statement-on-close flag.
   * 
   * @return true if close statement on close
   */
  public boolean isCloseStatementOnclose()  {
    return closeStatementOnClose;
  }
  
  
  
  
  
  /**
   * Sets the column offset.
   * Useful for eager loading or joining in general.
   *
   * @param columnOffset (default is 0)
   */
  public void setColumnOffset(int columnOffset) {
    this.columnOffset = columnOffset;
  }
  
  /**
   * Gets the column offset.
   * 
   * @return the current columnOffset
   */
  public int getColumnOffset()  {
    return columnOffset;
  }
  
  
  /**
   * Finds the column index to a string.
   * 
   * @param name the name of the field (column)
   * @return the index in the result set
   */
  public int findColumn(String name) {
    try {
      return rs.findColumn(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "db-error while rs.findColumn()");
      return -1;    // not reached
    }
  }
  
  
  /**
   * Gives the JDBC driver a hint as to the number of rows that should 
   * be fetched from the database when more rows are needed for this 
   * <code>ResultSet</code> object.
   * If the fetch size specified is zero, the JDBC driver 
   * ignores the value and is free to make its own best guess as to what
   * the fetch size should be.  The default value is set by the 
   * <code>Statement</code> object
   * that created the result set.  The fetch size may be changed at any time.
   *
   * @param rows the number of rows to fetch
   * @see #getFetchSize
   */
  public void setFetchSize(int rows) {
    try {
      rs.setFetchSize(rows);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "db-error while rs.setFetchSize()");
    }
  }
  
  /**
   * Retrieves the fetch size for this 
   * <code>ResultSet</code> object.
   *
   * @return the current fetch size for this <code>ResultSet</code> object
   * @see #setFetchSize
   */
  public int getFetchSize() {
    try {
      return rs.getFetchSize();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "db-error while rs.getFetchSize()");
      return 0;    // not reached
    }
    
  }
  
  
  
  /**
   * Gives a hint as to the direction in which the rows in this
   * <code>ResultSet</code> object will be processed. 
   * The initial value is determined by the 
   * <code>Statement</code> object
   * that produced this <code>ResultSet</code> object.
   * The fetch direction may be changed at any time.
   *
   * @param direction an <code>int</code> specifying the suggested
   *        fetch direction; one of <code>ResultSet.FETCH_FORWARD</code>, 
   *        <code>ResultSet.FETCH_REVERSE</code>, or
   *        <code>ResultSet.FETCH_UNKNOWN</code>
   * @see StatementWrapper#setFetchDirection
   * @see #getFetchDirection
   */
  public void setFetchDirection(int direction) {
    try {
      rs.setFetchDirection(direction);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "db-error while rs.setFetchDirection()");
    }
  }
  
  /**
   * Retrieves the fetch direction for this 
   * <code>ResultSet</code> object.
   *
   * @return the current fetch direction for this <code>ResultSet</code> object 
   * @see #setFetchDirection
   */
  public int getFetchDirection() {
    try {
      return rs.getFetchDirection();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, "db-error while rs.getFetchDirection()");
    }
    return ResultSet.FETCH_UNKNOWN;     // not reached  
  }
  
  
  /**
   * Moves the cursor froward one row from its current position.
   * A <code>ResultSet</code> cursor is initially positioned
   * before the first row; the first call to the method
   * <code>next</code> makes the first row the current row; the
   * second call makes the second row the current row, and so on. 
   * <p>
   * When a call to the <code>next</code> method returns <code>false</code>,
   * the cursor is positioned after the last row. Any
   * invocation of a <code>ResultSet</code> method which requires a  
   * current row will result in a <code>SQLException</code> being thrown.
   *  If the result set type is <code>TYPE_FORWARD_ONLY</code>, it is vendor specified 
   * whether their JDBC driver implementation will return <code>false</code> or
   *  throw an <code>SQLException</code> on a 
   * subsequent call to <code>next</code>.
   *
   * <p>If an input stream is open for the current row, a call
   * to the method <code>next</code> will
   * implicitly close it. A <code>ResultSet</code> object's
   * warning chain is cleared when a new row is read.
   *
   * @return <code>true</code> if the new current row is valid; 
   * <code>false</code> if there are no more rows 
   */
  public boolean next()  {
    try {
      return rs.next();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.next()"));
    }
    return false;
  }

  
  /**
   * Moves the cursor to the previous row in this
   * <code>ResultSet</code> object.
   *<p>
   * When a call to the <code>previous</code> method returns <code>false</code>, 
   * the cursor is positioned before the first row.  Any invocation of a 
   * <code>ResultSet</code> method which requires a current row will result in a 
   * <code>SQLException</code> being thrown.
   *<p>
   * If an input stream is open for the current row, a call to the method 
   * <code>previous</code> will implicitly close it.  A <code>ResultSet</code>
   *  object's warning change is cleared when a new row is read.
   *
   * @return <code>true</code> if the cursor is now positioned on a valid row; 
   * <code>false</code> if the cursor is positioned before the first row
   */  
  public boolean previous ()  {
    try {
      return rs.previous();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.previous()"));
    }
    return false;
  }

  
  /**
   * Moves the cursor to the first row in
   * this <code>ResultSet</code> object.
   *
   * @return <code>true</code> if the cursor is on a valid row;
   * <code>false</code> if there are no rows in the result set
   */
  public boolean first ()  {
    try {
      return rs.first();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.first()"));
    }
    return false;
  }

  
  /**
   * Moves the cursor to the last row in
   * this <code>ResultSet</code> object.
   *
   * @return <code>true</code> if the cursor is on a valid row;
   * <code>false</code> if there are no rows in the result set
   */
  public boolean last ()  {
    try {
      return rs.last();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.last()"));
    }
    return false;
  }
  
  
  /**
   * Moves the cursor to the front of
   * this <code>ResultSet</code> object, just before the
   * first row. This method has no effect if the result set contains no rows.
   */ 
  public void beforeFirst()  {
    try {
      rs.beforeFirst();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.beforeFirst()"));
    }
  }
  
  
  /**
   * Moves the cursor to the end of
   * this <code>ResultSet</code> object, just after the
   * last row. This method has no effect if the result set contains no rows.
   */
  public void afterLast()  {
    try {
      rs.afterLast();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.afterLast()"));
    }    
  }
  

  /**
   * Retrieves whether the cursor is before the first row in 
   * this <code>ResultSet</code> object.
   * <p>
   * <strong>Note:</strong>Support for the <code>isBeforeFirst</code> method 
   * is optional for <code>ResultSet</code>s with a result 
   * set type of <code>TYPE_FORWARD_ONLY</code>
   *
   * @return <code>true</code> if the cursor is before the first row;
   * <code>false</code> if the cursor is at any other position or the
   * result set contains no rows
   */
  public boolean isBeforeFirst()  {
    try {
      return rs.isBeforeFirst();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.isBeforeFirst()"));
    }
    return false;    
  }
  
  
  /**
   * Retrieves whether the cursor is after the last row in 
   * this <code>ResultSet</code> object.
   * <p>
   * <strong>Note:</strong>Support for the <code>isAfterLast</code> method 
   * is optional for <code>ResultSet</code>s with a result 
   * set type of <code>TYPE_FORWARD_ONLY</code>
   *
   * @return <code>true</code> if the cursor is after the last row;
   * <code>false</code> if the cursor is at any other position or the
   * result set contains no rows
   */  
  public boolean isAfterLast()  {
    try {
      return rs.isAfterLast();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.isAfterLast()"));
    }
    return false;       
  }
  

  /**
   * Retrieves the current row number.  The first row is number 1, the
   * second number 2, and so on.  
   * <p>
   * <strong>Note:</strong>Support for the <code>getRow</code> method 
   * is optional for <code>ResultSet</code>s with a result 
   * set type of <code>TYPE_FORWARD_ONLY</code>
   *
   * @return the current row number; <code>0</code> if there is no current row
   */
  public int getRow ()  {
    try {
      return rs.getRow();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getRow()"));
      return 0; // not reached
    }
    
  }
  

  /**
   * Moves the cursor to the given row number in
   * this <code>ResultSet</code> object.
   *
   * <p>If the row number is positive, the cursor moves to 
   * the given row number with respect to the
   * beginning of the result set.  The first row is row 1, the second
   * is row 2, and so on. 
   *
   * <p>If the given row number is negative, the cursor moves to
   * an absolute row position with respect to
   * the end of the result set.  For example, calling the method
   * <code>absolute(-1)</code> positions the 
   * cursor on the last row; calling the method <code>absolute(-2)</code>
   * moves the cursor to the next-to-last row, and so on.
   *
   * <p>An attempt to position the cursor beyond the first/last row in
   * the result set leaves the cursor before the first row or after 
   * the last row.
   *
   * <p><B>Note:</B> Calling <code>absolute(1)</code> is the same
   * as calling <code>first()</code>. Calling <code>absolute(-1)</code> 
   * is the same as calling <code>last()</code>.
   *
   * @param row the number of the row to which the cursor should move.
   *        A positive number indicates the row number counting from the
   *        beginning of the result set; a negative number indicates the
   *        row number counting from the end of the result set
   * @return <code>true</code> if the cursor is moved to a position in this
   * <code>ResultSet</code> object; 
   * <code>false</code> if the cursor is before the first row or after the
   * last row
   */
  public boolean absolute (int row)  {
    try {
      return rs.absolute(row);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.absolute()"));
    }
    return false;
  }

  
  /**
   * Moves the cursor a relative number of rows, either positive or negative.
   * Attempting to move beyond the first/last row in the
   * result set positions the cursor before/after the
   * the first/last row. Calling <code>relative(0)</code> is valid, but does
   * not change the cursor position.
   *
   * <p>Note: Calling the method <code>relative(1)</code>
   * is identical to calling the method <code>next()</code> and 
   * calling the method <code>relative(-1)</code> is identical
   * to calling the method <code>previous()</code>.
   *
   * @param rows an <code>int</code> specifying the number of rows to
   *        move from the current row; a positive number moves the cursor
   *        forward; a negative number moves the cursor backward
   * @return <code>true</code> if the cursor is on a row;
   *         <code>false</code> otherwise
   */
  public boolean relative (int rows)  {
    try {
      return rs.relative(rows);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.relative()"));
    }
    return false;
  }

  
  /**
   * Updates the underlying database with the new contents of the
   * current row of this <code>ResultSet</code> object.
   * This method cannot be called when the cursor is on the insert row.
   *
   * @return true if updated
   */
  public boolean updateRow() {
    try {
      rs.updateRow();
      // the current implementation always returns true
      return true;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.updateRow()"));
      return false;   // not reached
    }
  }

  
  /**
   * Deletes the current row from this <code>ResultSet</code> object 
   * and from the underlying database.  This method cannot be called when
   * the cursor is on the insert row.
   *
   * @return true if deleted
   */
  public boolean deleteRow() {
    try {
      rs.deleteRow();
      // the current implementation always returns true
      return true;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.deleteRow()"));
      return false;
    }
  }

  
  /**
   * Refreshes the current row with its most recent value in 
   * the database.  This method cannot be called when
   * the cursor is on the insert row.
   *
   * <p>The <code>refreshRow</code> method provides a way for an 
   * application to 
   * explicitly tell the JDBC driver to refetch a row(s) from the
   * database.  An application may want to call <code>refreshRow</code> when 
   * caching or prefetching is being done by the JDBC driver to
   * fetch the latest value of a row from the database.  The JDBC driver 
   * may actually refresh multiple rows at once if the fetch size is 
   * greater than one.
   * 
   * <p> All values are refetched subject to the transaction isolation 
   * level and cursor sensitivity.  If <code>refreshRow</code> is called after
   * calling an updater method, but before calling
   * the method <code>updateRow</code>, then the
   * updates made to the row are lost.  Calling the method
   * <code>refreshRow</code> frequently will likely slow performance.
   */
  public void refreshRow() {
    try {
      rs.refreshRow();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.refreshRow()"));
    }
  }

  
  /**
   * Retrieves whether the current row has been updated.  The value returned 
   * depends on whether or not the result set can detect updates.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowUpdated</code> method is optional with a result set 
   * concurrency of <code>CONCUR_READ_ONLY</code>
   * @return <code>true</code> if the current row is detected to 
   * have been visibly updated by the owner or another; <code>false</code> otherwise
   */
  public boolean rowUpdated() {
    try {
      return rs.rowUpdated();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.rowUpdated()"));
      return false; // not reached
    }
    
  }

  
  /**
   * Retrieves whether the current row has had an insertion.
   * The value returned depends on whether or not this
   * <code>ResultSet</code> object can detect visible inserts.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowInserted</code> method is optional with a result set 
   * concurrency of <code>CONCUR_READ_ONLY</code>
   * @return <code>true</code> if the current row is detected to 
   * have been inserted; <code>false</code> otherwise
   */
  public boolean rowInserted() {
    try {
      return rs.rowInserted();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.rowInserted()"));
      return false; // not reached
    }
  }

  
  /**
   * Retrieves whether a row has been deleted.  A deleted row may leave
   * a visible "hole" in a result set.  This method can be used to
   * detect holes in a result set.  The value returned depends on whether 
   * or not this <code>ResultSet</code> object can detect deletions.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowDeleted</code> method is optional with a result set 
   * concurrency of <code>CONCUR_READ_ONLY</code>
   * @return <code>true</code> if the current row is detected to 
   * have been deleted by the owner or another; <code>false</code> otherwise
   */
  public boolean rowDeleted() {
    try {
      return rs.rowDeleted();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.rowDeleted()"));
      return false; // not reached
    }
  }


  /**
   * Reports whether
   * the last column read had a value of SQL <code>NULL</code>.
   * Note that you must first call one of the getter methods
   * on a column to try to read its value and then call
   * the method <code>wasNull</code> to see if the value read was
   * SQL <code>NULL</code>.
   *
   * @return <code>true</code> if the last column value read was SQL
   *         <code>NULL</code> and <code>false</code> otherwise
   */
  public boolean wasNull() {
    try {
      return rs.wasNull();
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.wasNull()"));
      return false; // not reached
    }
  }



  // ---------------------- get values by column label ---------------------------
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param mapNull if empty strings should be treated as null values
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public String getString (String name, boolean mapNull)  {
    try {
      String str = rs.getString (name);
      if (mapNull && str != null && 
              (db.isOracle() ? str.equals(OracleHelper.emptyString) : str.length() == 0)) {
        return null;
      }
      return str;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getString()"));
      return null;  // not reached
    }
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @see #getString(java.lang.String, boolean) 
   */
  public String getString (String name) {
    return getString(name, false);
  }



  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>boolean</code> in the Java programming language. 
   *  
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>false</code>
   */
  public boolean getBoolean (String name) {
    try {
      return rs.getBoolean(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getBoolean()"));
      return false; // not reached
    }
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Boolean</code> in the Java programming language. 
   *  
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Boolean getABoolean (String name)  {
    boolean value = getBoolean(name);
    return wasNull() ? null : new Boolean(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>float</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public float getFloat (String name) {
    try {
      return rs.getFloat(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getFloat()"));
      return 0.0f;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Float</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Float getAFloat (String name)  {
    float value = getFloat(name);
    return wasNull() ? null : new Float(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public double getDouble (String name) {
    try {
      return rs.getDouble(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getDouble()"));
      return 0.0d;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Double</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Double getADouble (String name)  {
    double value = getDouble(name);
    return wasNull() ? null : new Double(value);
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.math.BigDecimal</code> with full precision.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public BigDecimal getBigDecimal (String name) {
    try {
      return rs.getBigDecimal(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getBigDecimal()"));
    }
    return null;
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>BMoney</code> with full precision.<br>
   * Notice that BMoney fields use two fields:
   * one for the value and
   * one for the scale (= name + "P")
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public BMoney getBMoney (String name) {
    double value = getDouble(name);
    return wasNull() ? null : new BMoney (value, getInt(name + "P"));
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>DMoney</code> with full precision.<br>
   * Notice that DMoney fields use two fields:
   * one for the value and
   * one for the scale (= name + "P")
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public DMoney getDMoney (String name) {
    try {
      BigDecimal decimal = rs.getBigDecimal(name);
      if (!wasNull()) {
        // set scale
        return new DMoney(decimal.movePointLeft(getInt(name + "P")));
      }
    }
    catch (SQLException e) {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getBigDecimal()"));
    }
    return null;
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public byte getByte (String name) {
    try {
      return rs.getByte(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getByte()"));
      return 0; // not reached
    }
  }
  

/**
 * Retrieves the value of the designated column in the current row
 * of this <code>ResultSet</code> object as
 * a <code>Byte</code> in the Java programming language.
 *
 * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
 * @return the column value; if the value is SQL <code>NULL</code>, the
 * value returned is <code>null</code>
 */
  public Byte getAByte (String name)  {
    byte value = getByte(name);
    return wasNull() ? null : new Byte(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>char</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code> or the empty string, the
   * value returned is <code>0</code>
   */
  public char getChar (String name) {
    try {
      String val = rs.getString(name);
      return val == null || val.length() == 0 ? 0 : val.charAt(0);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getChar()"));
    }
    return 0;
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Character</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column.
   * @param mapNull if blanks should be treated as null values
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>. If the value is the empty string the returned
   * value is <code>new Character(0)</code>.
   */
  public Character getCharacter (String name, boolean mapNull)  {
    char value = getChar(name);
    return wasNull() ? null : (mapNull && value == ' ' ? null : new Character(value));
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Character</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column.
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>. If the value is the empty string the returned
   * value is <code>new Character(0)</code>.
   */
  public Character getCharacter (String name)  {
    return getCharacter(name, false);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>short</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public short getShort (String name) {
    try {
      return rs.getShort(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getShort()"));
      return 0; // not reached
    }
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Short</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Short getAShort (String name)  {
    short value = getShort(name);
    return wasNull() ? null : new Short(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>int</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public int getInt (String name) {
    try {
      return rs.getInt(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getInt()"));
      return 0; // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Integer</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Integer getInteger (String name)  {
    int value = getInt(name);
    return wasNull() ? null : new Integer(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>long</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public long getLong (String name)  {
    try {
      return rs.getLong(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getLong()"));
    }
    return 0;
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Long</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Long getALong (String name)  {
    long value = getLong(name);
    return wasNull() ? null : new Long(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Date</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param mapNull true if 1.1.1970 should be mapped to null
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Date getDate(String name, boolean mapNull)  {
    try {
      Date date = rs.getDate(name);
      if (mapNull && date != null && date.equals(SqlHelper.minDate))  {
        // mindate is translated back to null
        return null;
      }
      return date;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getDate()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Date</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Date getDate(String name)  {
    return getDate(name, false);
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param mapNull true if 1.1.1970 00:00:00.000 should be mapped to null
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Timestamp getTimestamp(String name, boolean mapNull)  {
    try {
      Timestamp ts = rs.getTimestamp(name);
      if (mapNull && ts != null && ts.equals(SqlHelper.minTimestamp))  {
        // mintimestamp is translated back to null
        return null;
      }
      return ts;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getTimestamp()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Timestamp getTimestamp(String name)  {
    return getTimestamp(name, false);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Time</code> in the Java programming language.
   *
   * @param name the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Time getTime(String name)  {
    try {
      // no minTime (makes no sense, see PreparedStatementWrapper)
      return rs.getTime(name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getTime()"));
      return null;  // not reached
    }
  }

  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a {@link Binary} in the Java programming language.
   * 
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param bufSize the initial buffersize for the Binary
   * @return the binary read from db
   */
  public Binary getBinary(String name, int bufSize)  {
    try {
      return Binary.createBinary(rs.getBinaryStream(name), bufSize);
    } 
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("getBinary_failed"));
      return null;  // not reached
    }
  }



  // ------------------- getter with positions -----------------------------
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @param mapNull if empty strings should be treated as null values
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public String getString (int pos, boolean mapNull)  {
    try {
      String str = rs.getString (pos + columnOffset);
      if (mapNull && str != null && 
              (db.isOracle() ? str.equals(OracleHelper.emptyString) : str.length() == 0)) {
        return null;
      }
      return str;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getString()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @see #getString(java.lang.String, boolean) 
   */
  public String getString (int pos) {
    return getString(pos, false);
  }
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>boolean</code> in the Java programming language. 
   *  
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>false</code>
   */
  public boolean getBoolean (int pos) {
    try {
      return rs.getBoolean(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getBoolean()"));
      return false; // not reached
    }
  }
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Boolean</code> in the Java programming language. 
   *  
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT 
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Boolean getABoolean (int pos)  {
    boolean value = getBoolean(pos + columnOffset);
    return wasNull() ? null : new Boolean(value);
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>float</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public float getFloat (int pos) {
    try {
      return rs.getFloat(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getFloat()"));
      return 0.0f;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Float</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Float getAFloat (int pos)  {
    float value = getFloat(pos + columnOffset);
    return wasNull() ? null : new Float(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public double getDouble (int pos) {
    try {
      return rs.getDouble(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getDouble()"));
      return 0.0d;  // not reached
    }
  }
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Double</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Double getADouble (int pos)  {
    double value = getDouble(pos + columnOffset);
    return wasNull() ? null : new Double(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.math.BigDecimal</code> with full precision.
   *
   * @param pos the parameter index in the result set
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public BigDecimal getBigDecimal (int pos) {
    try {
      return rs.getBigDecimal(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getBigDecimal()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>BMoney</code> with full precision.<br>
   * Notice that BMoney fields use two fields:
   * one for the value and
   * one for the scale (= name + "P")
   *
   * @param pos the parameter index for the amount in the result set
   * @param ppos the parameter index for the scale in the result set
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public BMoney getBMoney (int pos, int ppos) {
    double value = getDouble(pos + columnOffset);
    return wasNull() ? null : new BMoney (value, getInt(ppos + columnOffset));
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>DMoney</code> with full precision.<br>
   * Notice that DMoney fields use two fields:
   * one for the value and
   * one for the scale (= name + "P")
   *
   * @param pos the parameter index for the amount in the result set
   * @param ppos the parameter index for the scale in the result set
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   */
  public DMoney getDMoney (int pos, int ppos) {
    BigDecimal value = getBigDecimal(pos + columnOffset);
    return wasNull() ? null : new DMoney (value.movePointLeft(getInt(ppos + columnOffset)));
  }
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public byte getByte (int pos) {
    try {
      return rs.getByte(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getByte()"));
      return 0; // not reached
    }
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Byte</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Byte getAByte (int pos)  {
    byte value = getByte(pos + columnOffset);
    return wasNull() ? null : new Byte(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>char</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code> or the empty string, the
   * value returned is <code>0</code>
   */
  public char getChar (int pos) {
    try {
      String val = rs.getString(pos + columnOffset);
      return val == null || val.length() == 0 ? 0 : val.charAt(0);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getChar()"));
      return 0;
    }
  }
  

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Character</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @param mapNull if blanks should be treated as null values
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>. If the value is the empty string the returned
   * value is <code>new Character(0)</code>.
   * 
   */
  public Character getCharacter (int pos, boolean mapNull)  {
    char value = getChar(pos + columnOffset);
    return wasNull() ? null : (mapNull && value == ' ' ? null : new Character(value));
  }
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Character</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>. If the value is the empty string the returned
   * value is <code>new Character(0)</code>.
   * 
   */
  public Character getCharacter (int pos) {
    return getCharacter(pos, false);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>short</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public short getShort (int pos) {
    try {
      return rs.getShort(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getShort()"));
      return 0; // not reached
    }
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Short</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Short getAShort (int pos)  {
    short value = getShort(pos + columnOffset);
    return wasNull() ? null : new Short(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>int</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public int getInt (int pos) {
    try {
      return rs.getInt(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getInt()"));
      return 0; // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Integer</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Integer getInteger (int pos)  {
    int value = getInt(pos + columnOffset);
    return wasNull() ? null : new Integer(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>long</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  public long getLong (int pos)  {
    try {
      return rs.getLong(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getLong()"));
      return 0; // not reached
    }
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Long</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Long getALong (int pos)  {
    long value = getLong(pos + columnOffset);
    return wasNull() ? null : new Long(value);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Date</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @param mapNull true if 1.1.1970 should be mapped to null
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Date getDate(int pos, boolean mapNull)  {
    try {
      Date date = rs.getDate(pos + columnOffset);
      if (mapNull && date != null && date.equals(SqlHelper.minDate))  {
        // mindate is translated back to null
        return null;
      }
      return date;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getDate()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Date</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Date getDate(int pos)  {
    return getDate(pos, false);
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @param mapNull true if 1.1.1970 00:00:00.000 should be mapped to null
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Timestamp getTimestamp(int pos, boolean mapNull)  {
    try {
      Timestamp ts = rs.getTimestamp(pos + columnOffset);
      if (mapNull && ts != null && ts.equals(SqlHelper.minTimestamp))  {
        // mindate is translated back to null
        return null;
      }
      return ts;
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getTimestamp()"));
      return null;  // not reached
    }
  }
  
  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Timestamp getTimestamp(int pos)  {
    return getTimestamp(pos, false);
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Time</code> in the Java programming language.
   *
   * @param pos the parameter index in the result set
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   */
  public Time getTime(int pos)  {
    try {
      // no mapNull possible
      return rs.getTime(pos + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_ResultSet.getTime()"));
      return null;  // not reached
    }
  }

  
  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a {@link Binary} in the Java programming language.
   * 
   * @param pos the parameter index in the result set
   * @param bufSize the initial buffersize for the Binary
   * @return the binary read from db
   */
  public Binary getBinary(int pos, int bufSize)  {
    try {
      return Binary.createBinary(rs.getBinaryStream(pos + columnOffset), bufSize);
    } 
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("getBinary_failed"));
      return null;  // not reached
    }
  }

  


  // ------------------------- update by column name ---------------------------------

  
  /**
   * Updates the designated column with a <code>null</code> value.
   * 
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code>
   * or <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   */
  public void updateNull (String name) {
    try {
      rs.updateNull (name);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateNull()_des_SQL-ResultSets"));
    }
  }


  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param s the new column value
   * @param mapNull true if null values should be mapped to the empty string, else SQL NULL
   */
  public void updateString (String name, String s, boolean mapNull) {
    try {
      if (s == null) {
        if (mapNull)  {
          rs.updateString(name, 
                  getDb().isOracle() ? OracleHelper.emptyString : StringHelper.emptyString);
        }
        else  {
          rs.updateNull(name);
        }
      }
      else {
        rs.updateString(name, s);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateString()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param s the new column value
   */
  public void updateString (String name, String s) {
    updateString(name, s, false);
  }



  /**
   * Updates the designated column with a <code>boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param b the new column value
   */
  public void updateBoolean (String name, boolean b) {
    try {
      rs.updateBoolean (name, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBoolean()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param b the new column value, null for SQL NULL
   */
  public void updateBoolean (String name, Boolean b) {
    if (b == null) {
      updateNull(name);
    }
    else {
      updateBoolean(name, b.booleanValue());
    }
  }


  /**
   * Updates the designated column with a <code>byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param b the new column value
   */
  public void updateByte (String name, byte b) {
    try {
      rs.updateByte (name, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateByte()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param b the new column value, null for SQL NULL
   */
  public void updateByte (String name, Byte b) {
    if (b == null) {
      updateNull(name);
    }
    else {
      updateByte(name, b.byteValue());
    }
  }


  /**
   * Updates the designated column with a <code>short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param s the new column value
   */
  public void updateShort (String name, short s) {
    try {
      rs.updateShort (name, s);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateShort()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param s the new column value, null for SQL NULL
   */
  public void updateShort (String name, Short s) {
    if (s == null) {
      updateNull(name);
    }
    else {
      updateShort(name, s.shortValue());
    }
  }


  /**
   * Updates the designated column with an <code>int</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param i the new column value
   */
  public void updateInt (String name, int i) {
    try {
      rs.updateInt (name, i);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateInt()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with an <code>Integer</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param i the new column value, null for SQL NULL
   */
  public void updateInteger (String name, Integer i) {
    if (i == null) {
      updateNull(name);
    }
    else {
      updateInt(name, i.intValue());
    }
  }


  /**
   * Updates the designated column with a <code>long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param l the new column value
   */
  public void updateLong (String name, long l) {
    try {
      rs.updateLong (name, l);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateLong()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param l the new column value, null for SQL NULL
   */
  public void updateLong (String name, Long l) {
    if (l == null) {
      updateNull(name);
    }
    else {
      updateLong(name, l.longValue());
    }
  }


  /**
   * Updates the designated column with a <code>float</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param f the new column value
   */
  public void updateFloat (String name, float f) {
    try {
      rs.updateFloat (name, f);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateFloat()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Float</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param f the new column value, null for SQL NULL
   */
  public void updateFloat (String name, Float f) {
    if (f == null) {
      updateNull(name);
    }
    else {
      updateFloat(name, f.floatValue());
    }
  }


  /**
   * Updates the designated column with a <code>double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param d the new column value
   */
  public void updateDouble (String name, double d) {
    try {
      rs.updateDouble (name, d);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateDouble()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param d the new column value, null for SQL NULL
   */
  public void updateDouble (String name, Double d) {
    if (d == null) {
      updateNull(name);
    }
    else {
      updateDouble(name, d.doubleValue());
    }
  }


  /**
   * Updates the designated column with a <code>BMoney</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * A BMoney will not be stored as a DECIMAL, but as 2 fields:
   * <ol>
   * <li>a double representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param m the money amount
   */
  public void updateBMoney (String name, BMoney m) {
    try {
      if (m == null) {
        rs.updateNull (name);
        rs.updateNull (name + "P");
      }
      else  {
        rs.updateDouble (name, m.doubleValue());
        rs.updateInt    (name + "P", m.scale());
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBMoney()_des_SQL-ResultSets"));
    }
  }


  /**
   * Updates the designated column with a <code>DMoney</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * A BMoney will not be stored as a DECIMAL, but as 2 fields:
   * <ol>
   * <li>a BigDecimal with a scale of 0 representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param m the money amount
   */
  public void updateDMoney (String name, DMoney m) {
    try {
      if (m == null) {
        rs.updateNull (name);
        rs.updateNull (name + "P");
      }
      else  {
        rs.updateBigDecimal (name, m.movePointRight(m.scale()));
        rs.updateInt        (name + "P", m.scale());
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBMoney()_des_SQL-ResultSets"));
    }
  }
  

  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param d the new column value
   * @param mapNull to map null values to 1.1.1970 (epochal time zero), else SQL NULL
   */
  public void updateDate(String name, Date d, boolean mapNull) {
    try {
      if (d == null)  {
        if (mapNull)  {
          rs.updateDate (name, SqlHelper.minDate);
        }
        else  {
          rs.updateNull (name);
        }
      }
      else  {
        rs.updateDate (name, d);
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateDate()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param d the new column value, null for SQL NULL
   */
  public void updateDate(String name, Date d) {
    updateDate(name, d, false);
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param ts the new column value
   * @param mapNull to map null values to 1.1.1970 00:00:00.000 (epochal time zero), else SQL NULL
   */
  public void updateTimestamp(String name, Timestamp ts, boolean mapNull) {
    try {
      if (ts == null)  {
        if (mapNull)  {
          rs.updateTimestamp (name, SqlHelper.minTimestamp);
        }
        else  {
          rs.updateNull (name);
        }
      }
      else  {
        rs.updateTimestamp (name, ts);
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateTimestamp()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param ts the new column value, null for SQL NULL
   */
  public void updateTimestamp(String name, Timestamp ts) {
    updateTimestamp(name, ts, false);
  }

  
  /**
   * Updates the designated column with a <code>java.sql.Time</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param t the new column value, null for SQL NULL
   */
  public void updateTime(String name, Time t) {
    try {
      if (t == null)  {
        rs.updateNull (name);
      }
      else  {
        rs.updateTime (name, t);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateTime()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a {@link Binary} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.<br>
   * The implementation translates the Binary into an Inputstream and invokes
   * {@link ResultSet#updateBinaryStream(String, java.io.InputStream, int)}.
   *
   * @param name the label for the column specified with the SQL AS clause.
   *        If the SQL AS clause was not specified, then the label is the name of the column
   * @param b the new column value, null for SQL NULL
   */
  public void updateBinary(String name, Binary b) {
    try {
      if (b == null)  {
        rs.updateNull (name);
      }
      else  {
        rs.updateBinaryStream(name, b.getInputStream(), b.getLength());
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("updateBinary_failed"));
    }
  }



  // ------------------------- update by index ---------------------------------

  
  /**
   * Updates the designated column with a <code>null</code> value.
   * 
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code>
   * or <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   */
  public void updateNull (int p) {
    try {
      rs.updateNull (p + columnOffset);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateNull()_des_SQL-ResultSets"));
    }
  }


  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param s the new column value
   * @param mapNull true if null values should be mapped to the empty string, else SQL NULL
   */
  public void updateString (int p, String s, boolean mapNull) {
    try {
      if (s == null) {
        if (mapNull)  {
          rs.updateString(columnOffset + p, 
                  getDb().isOracle() ? OracleHelper.emptyString : StringHelper.emptyString);
        }
        else  {
          rs.updateNull(p + columnOffset);
        }
      }
      else {
        rs.updateString(p + columnOffset, s);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateString()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param s the new column value
   */
  public void updateString (int p, String s) {
    updateString(p, s, false);
  }



  /**
   * Updates the designated column with a <code>boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param b the new column value
   */
  public void updateBoolean (int p, boolean b) {
    try {
      rs.updateBoolean (p + columnOffset, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBoolean()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param b the new column value, null for SQL NULL
   */
  public void updateBoolean (int p, Boolean b) {
    if (b == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateBoolean(p + columnOffset, b.booleanValue());
    }
  }


  /**
   * Updates the designated column with a <code>byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param b the new column value
   */
  public void updateByte (int p, byte b) {
    try {
      rs.updateByte (p + columnOffset, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateByte()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param b the new column value, null for SQL NULL
   */
  public void updateByte (int p, Byte b) {
    if (b == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateByte(p + columnOffset, b.byteValue());
    }
  }


  /**
   * Updates the designated column with a <code>short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param s the new column value
   */
  public void updateShort (int p, short s) {
    try {
      rs.updateShort (p + columnOffset, s);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateShort()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param s the new column value, null for SQL NULL
   */
  public void updateShort (int p, Short s) {
    if (s == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateShort(p + columnOffset, s.shortValue());
    }
  }


  /**
   * Updates the designated column with an <code>int</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param i the new column value
   */
  public void updateInt (int p, int i) {
    try {
      rs.updateInt (p + columnOffset, i);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateInt()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with an <code>Integer</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param i the new column value, null for SQL NULL
   */
  public void updateInteger (int p, Integer i) {
    if (i == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateInt(p + columnOffset, i.intValue());
    }
  }


  /**
   * Updates the designated column with a <code>long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param l the new column value
   */
  public void updateLong (int p, long l) {
    try {
      rs.updateLong (p + columnOffset, l);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateLong()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param l the new column value, null for SQL NULL
   */
  public void updateLong (int p, Long l) {
    if (l == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateLong(p + columnOffset, l.longValue());
    }
  }


  /**
   * Updates the designated column with a <code>float</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param f the new column value
   */
  public void updateFloat (int p, float f) {
    try {
      rs.updateFloat (p + columnOffset, f);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateFloat()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Float</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param f the new column value, null for SQL NULL
   */
  public void updateFloat (int p, Float f) {
    if (f == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateFloat(p + columnOffset, f.floatValue());
    }
  }


  /**
   * Updates the designated column with a <code>double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param d the new column value
   */
  public void updateDouble (int p, double d) {
    try {
      rs.updateDouble (p + columnOffset, d);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateDouble()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a <code>Double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param d the new column value, null for SQL NULL
   */
  public void updateDouble (int p, Double d) {
    if (d == null) {
      updateNull(p + columnOffset);
    }
    else {
      updateDouble(p + columnOffset, d.doubleValue());
    }
  }


  /**
   * Updates the designated column with a <code>BMoney</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * A BMoney will not be stored as a DECIMAL, but as 2 fields:
   * <ol>
   * <li>a double representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * @param pos the positional index of the amount
   * @param ppos the positional index of the scale
   * @param m the money amount
   */
  public void updateBMoney (int pos, int ppos, BMoney m) {
    try {
      if (m == null) {
        rs.updateNull (pos + columnOffset);
        rs.updateNull (ppos + columnOffset);
      }
      else  {
        rs.updateDouble (pos + columnOffset, m.doubleValue());
        rs.updateInt    (ppos + columnOffset, m.scale());
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBMoney()_des_SQL-ResultSets"));
    }
  }

  

  /**
   * Updates the designated column with a <code>DMoney</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * A BMoney will not be stored as a DECIMAL, but as 2 fields:
   * <ol>
   * <li>a BigDecimal with a scale of 0 representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * @param pos the positional index of the amount
   * @param ppos the positional index of the scale
   * @param m the money amount
   */
  public void updateDMoney (int pos, int ppos, DMoney m) {
    try {
      if (m == null) {
        rs.updateNull (pos + columnOffset);
        rs.updateNull (ppos + columnOffset);
      }
      else  {
        rs.updateBigDecimal (pos + columnOffset, m.movePointRight(m.scale()));
        rs.updateInt        (ppos + columnOffset, m.scale());
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateBMoney()_des_SQL-ResultSets"));
    }
  }


  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param d the new column value
   * @param mapNull to map null values to 1.1.1970 (epochal time zero), else SQL NULL
   */
  public void updateDate(int p, Date d, boolean mapNull) {
    try {
      if (d == null)  {
        if (mapNull)  {
          rs.updateDate (columnOffset + p, SqlHelper.minDate);
        }
        else  {
          rs.updateNull (p + columnOffset);
        }
      }
      else  {
        rs.updateDate (p + columnOffset, d);
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateDate()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param d the new column value, null for SQL NULL
   */
  public void updateDate(int p, Date d) {
    updateDate(p, d, false);
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param ts the new column value
   * @param mapNull to map null values to 1.1.1970 00:00:00.000 (epochal time zero), else SQL NULL
   */
  public void updateTimestamp(int p, Timestamp ts, boolean mapNull) {
    try {
      if (ts == null)  {
        if (mapNull)  {
          rs.updateTimestamp (columnOffset + p, SqlHelper.minTimestamp);
        }
        else  {
          rs.updateNull (p + columnOffset);
        }
      }
      else  {
        rs.updateTimestamp (p + columnOffset, ts);
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateTimestamp()_des_SQL-ResultSets"));
    }
  }
  
  
  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param ts the new column value, null for SQL NULL
   */
  public void updateTimestamp(int p, Timestamp ts) {
    updateTimestamp(p, ts, false);
  }

  
  /**
   * Updates the designated column with a <code>java.sql.Time</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param t the new column value, null for SQL NULL
   */
  public void updateTime(int p, Time t) {
    try {
      if (t == null)  {
        rs.updateNull (p + columnOffset);
      }
      else  {
        rs.updateTime (p + columnOffset, t);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("Datenbankfehler_beim_updateTime()_des_SQL-ResultSets"));
    }
  }

  
  /**
   * Updates the designated column with a {@link Binary} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not 
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.<br>
   * The implementation translates the Binary into an Inputstream and invokes
   * {@link ResultSet#updateBinaryStream(int, java.io.InputStream, int)}.
   *
   * @param p the first column is 1, the second is 2, ...
   * @param b the new column value, null for SQL NULL
   */
  public void updateBinary(int p, Binary b) {
    try {
      if (b == null)  {
        rs.updateNull (p + columnOffset);
      }
      else  {
        rs.updateBinaryStream(p + columnOffset, b.getInputStream(), b.getLength());
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(db, e, Locales.bundle.getString("updateBinary_failed"));
    }
  }

}
