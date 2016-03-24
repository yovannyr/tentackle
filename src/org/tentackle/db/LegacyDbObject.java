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

// $Id: LegacyDbObject.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class defining some essential basics for objects
 * *not* derived from {@link DbObject}.<br>
 * Useful to access non-tentackle-based database tables.
 *
 * @author harald
 */
public abstract class LegacyDbObject {
  
  private Db db;          // db-connection
  
  /**
   * Creates a legacy db object.
   * 
   * @param db the db connection
   */
  public LegacyDbObject(Db db) {
    this.db = db;
  }
  
  
  /**
   * Sets the logical db connection for this object.
   * 
   * @param db the db connection
   */
  public void setDb (Db db)  {
    this.db = db;
  }

  /**
   * Get the logical db connection for this object.
   * 
   * @return the db connection
   */
  public Db getDb ()  {
    return db;
  }


  /**
   * Gets the basename of the class of this object.<br>
   * The basename is the class name without the package name.
   * 
   * @return the basename of the Objects class
   */
  public String getClassBaseName ()  {
    return getLegacyDbObjectClassVariables().classBaseName;
  }
  
  
  /**
   * Gets the some attributes and variables common to all objects of the same class.
   * Class variables for classes derived from {@link LegacyDbObject} are kept in an
   * instance of {@link LegacyDbObjectClassVariables}.
   * 
   * @return the class variables
   */
  public abstract LegacyDbObjectClassVariables getLegacyDbObjectClassVariables();
  
  
  /**
   * Column names may be preceeded by a prefix. This is especially useful
   * for joined tables auch as "table-per-class". The prefix must contain
   * a trailing dot, just the name.
   * 
   * The default is null.
   * 
   * @return the SQL prefix
   */
  public String getSqlPrefix()  {
    return null;
  }
  
  
  /**
   * Gets the prefix with a trailing dot or "" if no prefix.
   * 
   * @return the SQL prefix
   */
  public String getSqlPrefixWithDot() {
    String prefix = getSqlPrefix();
    return prefix == null ? "" : (prefix + ".");
  }
  
  
  /**
   * Returns true if object is only a partial implementation.
   * This is used in conjunction with PartialAppDbObject and the wurblet AppDbSuper
   * for abstract super classes in a table-per-class mapping.
   * 
   * @return true if partial implementation
   */
  public boolean isPartial() {
    return false;
  }
  
  
  /**
   * Retrieves the values of all fields (all columns of the database table)
   * in the given {@link ResultSetWrapper} and stores them in the object's attributes.
   * 
   * @param rs the result set
   * @return true if all fields retrieved
   */
  abstract public boolean getFields (ResultSetWrapper rs);
  
  
  /**
   * Sets the values of all fields (all columns of the database table)
   * in the given {@link PreparedStatementWrapper} from the object's attributes.
   *
   * @param st the statement
   * @return the number of fields set, -1 if error.
   */
  public int setFields (PreparedStatementWrapper st) {
    throw new DbRuntimeException("setFields() not implemented");
  }
  
  
  /**
   * Prepares the insert statement.<br>
   * The default implementation throws a DbRuntimeException.
   */
  public void prepareInsertStatement() {
    throw new DbRuntimeException("prepareInsertStatement() not implemented");
  }
  
  
  /**
   * Prepares the update statement.<br>
   * The default implementation throws a DbRuntimeException.
   */
  public void prepareUpdateStatement() {
    throw new DbRuntimeException("prepareUpdateStatement() not implemented");
  }
  
  
  /**
   * Prepares the delete statement.<br>
   * The default implementation throws a DbRuntimeException.
   */
  public void prepareDeleteStatement() {
    throw new DbRuntimeException("prepareDeleteStatement() not implemented");
  }
  
  
  /**
   * Prepare the exists statement.<br>
   * The default implementation throws a DbRuntimeException.
   */
  public void prepareExistsStatement() {
    throw new DbRuntimeException("prepareExistsStatement() not implemented");
  }

  
  /**
   * Determines whether prepared statements of this class should always
   * be prepared each time when the statement used.
   * @return true if always prepare
   */
  public boolean alwaysPrepare() {
    return getLegacyDbObjectClassVariables().alwaysPrepare;
  }
  
  
  /**
   * Sets the always prepare flag.
   * 
   * @param alwaysPrepare true if always prepare
   */
  public void setAlwaysPrepare(boolean alwaysPrepare) {
    getLegacyDbObjectClassVariables().alwaysPrepare = alwaysPrepare;
  }
  

  
  /**
   * Gets the prepared statement id for {@link #selectAll}.
   * @return the statement id
   */
  protected int getSelectAllStatementId() {
    return getLegacyDbObjectClassVariables().selectAllStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectAll}.
   * @param id the statement id
   */
  protected void setSelectAllStatementId (int id) {
    getLegacyDbObjectClassVariables().selectAllStatementId = id;
  }
  
  
  /**
   * Gets the prepared statement id for {@link #insert}.
   * @return the statement id
   */
  protected int getInsertStatementId() {
    return getLegacyDbObjectClassVariables().insertStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #insert}.
   * @param id the statement id
   */
  protected void setInsertStatementId (int id) {
    getLegacyDbObjectClassVariables().insertStatementId = id;
  }
    
  
  /**
   * Gets the prepared statement id for {@link #update}.
   * @return the statement id
   */
  protected int getUpdateStatementId() {
    return getLegacyDbObjectClassVariables().updateStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #update}.
   * @param id the statement id
   */
  protected void setUpdateStatementId (int id) {
    getLegacyDbObjectClassVariables().updateStatementId = id;
  }
  
  
  /**
   * Gets the prepared statement id for {@link #delete}.
   * @return the statement id
   */
  protected int getDeleteStatementId() {
    return getLegacyDbObjectClassVariables().deleteStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #delete}.
   * @param id the statement id
   */
  protected void setDeleteStatementId (int id) {
    getLegacyDbObjectClassVariables().deleteStatementId = id;
  }

  
  /**
   * Gets the prepared statement id for {@link #exists}.
   * @return the statement id
   */
  protected int getExistsStatementId() {
    return getLegacyDbObjectClassVariables().existsStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #exists}.
   * @param id the statement id
   */
  protected void setExistsStatementId (int id) {
    getLegacyDbObjectClassVariables().existsStatementId = id;
  }
  
  
  /**
   * Gets the database table name for the class of this object.
   * @return the table name
   */
  public String getTableName () {
    return getLegacyDbObjectClassVariables().tableName;
  }

  
  /**
   * Gets the human readable name for one (1) object of this class.
   * @return the name
   */
  public String getSingleName () {
    return getLegacyDbObjectClassVariables().singleName;
  }

  
  /**
   * Gets the human readable name for multiple (&gt; 1) objects of this class.
   * @return the name
   */
  public String getMultiName () {
    return getLegacyDbObjectClassVariables().multiName;
  }

  
  
  /**
   * Creates a new object with the same class and same logical
   * db connection as this object.
   * Must be overridden if further setup is necessary.
   * 
   * @return the new object 
   */
  public LegacyDbObject newObject() {
    try {
      LegacyDbObject obj = (LegacyDbObject)(getClass().newInstance());
      obj.setDb(getDb());
      return obj;
    }
    catch (Exception e) {
      DbGlobal.errorHandler.severe(getDb(), e, "creating new legacy object failed");
      return null;
    }
  }
  
  
  
  /**
   * Gets the SQL-string to select all columns of this class 
   * (may be for joined multiple tables)
   *
   * @return the sql-string for "SELECT * FROM ..."
   */
  public String getSqlSelectAllFields() {
    return "SELECT * FROM " + getTableName() + Db.WHEREALL_CLAUSE;
  }
  

  /**
   * Reads from a result-set into 'this' object.
   * For compatability with wurblets.
   *
   * @param rs is the result set (wrapper)
   * @return true if values read, false if some error
   */
  public boolean readFromResultSetWrapper(ResultSetWrapper rs)  {
    return getFields(rs);
  }
  
  
  /**
   * Selects all objects and returns the ResultSetWrapper.
   * 
   * @return the result set
   */
  public ResultSetWrapper selectAllResultSet() {
    int statementId = getSelectAllStatementId();
    if (statementId == 0) {
      statementId = getDb().prepareStatement(getSqlSelectAllFields());
      setSelectAllStatementId(statementId);
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(statementId);
    return st.executeQuery();    
  }
  
  
  /**
   * Selects the next object from the resultset.
   * 
   * @param rs the result set
   * @return null if end of set (app should close rs hereafter!)
   */
  public LegacyDbObject selectNext(ResultSetWrapper rs)  {
    if (rs.next() && getFields(rs))  {
      return this;
    }
    return null;
  }

  
  /**
   * Selects all objects of this class.
   * 
   * @return the list of objects
   */
  public List<? extends LegacyDbObject> selectAll() {
    List<LegacyDbObject> list = new ArrayList<LegacyDbObject>();   /* returned list of objects */
    ResultSetWrapper rs = selectAllResultSet();
    LegacyDbObject obj;
    while ((obj = newObject().selectNext(rs)) != null)  {
      list.add(obj);
    }
    rs.close();
    return list;
  }
  
  
  /**
   * Inserts this object into the database.
   *
   * @return true if inserted, false if duplicate or not saveable.
   */
  public boolean insert()  {
    prepareInsertStatement();
    PreparedStatementWrapper st = getDb().getPreparedStatement(getLegacyDbObjectClassVariables().insertStatementId);

    boolean oldcommit = getDb().begin();

    setFields (st);         // set fields

    if (st.executeUpdate() != 1) {
      getDb().rollback(oldcommit);
      return false;
    }
    getDb().commit(oldcommit);
    return true;
  }
  
  
  /**
   * Sets the unique keys.<br>
   * If {@link #update}, {@link #delete} or {@link #exists} are implemented
   * this method must be overridden to set the unique key-value(s).
   * Furthermore, the DbUpdate-wurblet must reflect these keys in a where-clause
   * with options --maponly --append=" WHERE ... "
   *
   * @param st the prepared statement
   * @param ndx the starting index in st
   * @return the new index in st
   */
  public int setKeyFields(PreparedStatementWrapper st, int ndx) {
    throw new DbRuntimeException("setKeyFields() not implemented");
  }
  
  
  /**
   * Updates this object to the database.<br>
   *
   * @return true if done, false if duplicate or object is not saveable.
   */
  public boolean update()  {
    prepareUpdateStatement();
    PreparedStatementWrapper st = getDb().getPreparedStatement(getLegacyDbObjectClassVariables().updateStatementId);

    boolean oldcommit = getDb().begin();

    int ndx = setFields (st);         // set fields
    setKeyFields(st, ndx);            // set key fields

    if (st.executeUpdate() != 1) {
      getDb().rollback(oldcommit);
      return false;
    }
    getDb().commit(oldcommit);
    return true;
  }
  
  
  
  /**
   * Removes this object from the database.
   *
   * @return true if removed, false if failed
   */
  public boolean delete()  {
    prepareDeleteStatement();
    PreparedStatementWrapper st = getDb().getPreparedStatement(getLegacyDbObjectClassVariables().deleteStatementId);

    boolean oldcommit = getDb().begin();

    setKeyFields(st, 0);            // set key fields

    if (st.executeUpdate() != 1) {
      getDb().rollback(oldcommit);
      return false;
    }
    getDb().commit(oldcommit);
    return true;
  }
  

  /**
   * Gets the number of db-columns.<br>
   * Will perform a dummy select if not known so far.
   * (assumes getFields() will compute the number of columns!)
   *
   * @return the number of db-columns
   */
  public int getFieldCount()  {
    int count = getLegacyDbObjectClassVariables().fieldCount;
    if (count <= 0) {
      gettingFieldCount = true;
      // perform a dummy select (this happens only once, hence no prepared statement)
      ResultSetWrapper rs = getDb().createStatement().executeQuery(
              getSqlSelectAllFields() + " AND 0=1");
      getFields(rs);   // this will set the fieldCount
      rs.close();
      if ((count = getLegacyDbObjectClassVariables().fieldCount) <= 0) {
        // still not set: invoke errorhandler
        DbGlobal.errorHandler.severe(getDb(), null, "can not determine fieldCount for " + getTableName());
      }
      gettingFieldCount = false;
    }
    return count;
  }
  
  /**
   * as getFieldCount() is invoked from getFields() we must prevent loops and heap overflow.
   */
  private boolean gettingFieldCount;
  
  
  /**
   * as getFieldCount() is invoked from getFields() we must prevent loops and heap overflow.
   * @return true if within getFieldCount
   */
  public boolean isGettingFieldCount() {
    return gettingFieldCount;
  }
  
  
  /**
   * Update fieldCount if index greater than current fieldcount.
   *
   * @param columnIndex is the result-set column index (>=1)
   */
  protected void updateFieldCount(int columnIndex)  {
    LegacyDbObjectClassVariables dbvar = getLegacyDbObjectClassVariables();
    if (dbvar.fieldCount < columnIndex) {
      dbvar.fieldCount = columnIndex;
    }
  }
  
  
  /**
   * Tests if an object exists in the database.<br>
   * Because LegacyDbObjects have no serial and no id, there is no generic
   * way to implement isNew().
   * However, we can query the database to check if the object exists.
   * For this method to work, prepareExistsStatement() and setKeyFields() must be implemented.
   * 
   * @return true if object exists
   */
  public boolean exists() {
    prepareExistsStatement();
    PreparedStatementWrapper st = getDb().getPreparedStatement(getLegacyDbObjectClassVariables().existsStatementId);
    setKeyFields(st, 0);            // set key fields
    ResultSetWrapper rs = st.executeQuery();
    boolean exists = rs.next();
    rs.close();
    return exists;
  }
  
  
  /**
   * Updates the object if it exists, else inserts it.<br>
   * Because transactions abort on unique violations or update failures and there is
   * no generic concept for isNew(), we cannot provide a generic method save().
   * But we can check for existance and updateOrInsert().
   * 
   * @return true if updated or inserter
   */
  public boolean updateOrInsert() {
    return exists() ? update() : insert();
  }
  
}
