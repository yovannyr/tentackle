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

// $Id: ModificationLog.java 480 2009-09-07 17:09:32Z harald $


package org.tentackle.db;


import java.util.ArrayList;
import java.util.List;
import org.tentackle.db.rmi.ModificationLogRemoteDelegate;
import java.sql.Timestamp;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.StringHelper;



// @> $mapfile
// # modification table for async coupling
// #
// # CREATE INDEX modlog_next on modlog (errorcode, id);
// # CREATE INDEX modlog_object on modlog (objectid, objectclass);
// # CREATE INDEX modlog_user on modlog (moduser);
//
// long       0       objectId        objectid        object id [READONLY]
// String     64      objectClass     objectclass     object classname [READONLY]
// long       0       txId            txid            transaction id (optional)
// String     64      txName          txname          transaction name (optional)
// char       0       modType         modtype         modification type [READONLY]
// Timestamp  0       when            modtime         time of event [READONLY]
// String     32      user            moduser         name of user [READONLY]
// int        0       errorCode       errorcode       error number (0=ok)
// String     0       message         message         optional informational or error message
// long       0       extraId         extraid         some optional id for any purpose
// @<



/**
 * Logging for object modifications.<br>
 * 
 * Modifications to {@link DbObject}s can be logged to a so-called modification log (aka: modlog).<br>
 * Most applications will use the modlog for asynchroneous database coupling.
 * <p>
 * Note: the txId is only valid (&gt; 0) if the db-connection has {@link Db#isLogModificationTxEnabled}, 
 * i.e. begin and commit records are logged as well. If the {@link IdSource} of the modlog is
 * transaction-based, transactions will not overlap in the modlog because obtaining
 * the id for the modlog is part of the transaction. However, if the idsource is
 * remote (poolkeeper rmi-client, for example), transactions may overlap!
 * In such cases the txid is necessary to separate the modlog sequences into
 * discrete transactions. (see the PoolKeeper project)
 */
public class ModificationLog extends DbObject {
  
  private static final long serialVersionUID = 7997968053729155282L;
  
  /** database tablename **/
  public static final String TABLENAME = "modlog";
  
  private static DbObjectClassVariables classVariables = 
    new DbObjectClassVariables(ModificationLog.class, TABLENAME, "Modification Log", "Modification Logs");



  
  /**
   * The {@link DbObject} the log belongs to. null = unknown. Speeds up {@link #getObject}
   * in distributed applications (see the poolkeeper framework).
   */
  protected DbObject lazyObject;
  
  
  /**
   * Creates an empty modification log for a given db.
   * Useful for reading the log or as an RMI-proxy.
   * 
   * @param db the database connection
   */
  public ModificationLog(Db db) {
    super(db);
  }
  
  /**
   * Creates a modification log for a given db and modification type.<br>
   * 
   * @param db the database connection
   * @param modType is the modification type (BEGIN or COMMIT)
   */
  public ModificationLog(Db db, char modType) {
    this(db);
    this.modType = modType;
    txName = db.getTxName();
    txId   = db.getLogModificationTxId();
    when   = SqlHelper.now();
    user   = db.getUserInfo().getUsername();
    if (user.length() > LENGTH_USER) {
      user = user.substring(0, LENGTH_USER); // cut! (it's just informational)
    }
  }
  
  
  /**
   * Creates a modification log from an object.
   * 
   * @param object is the logged object
   * @param modType is the modification type (INSERT, UPDATE...)
   */
  public ModificationLog(DbObject object, char modType)  {
    this(object.getDb(), modType);
    
    if (modType == BEGIN || modType == COMMIT) {
      DbGlobal.errorHandler.severe(getDb(), null, "illegal BEGIN or COMMIT in object logging");
    }
    if (modType != DELETEALL) {
      objectId = object.getId();
    }
    
    /**
     * The modlog's serial should reflect the serial of the object.
     * The modlog is inserted _after_ the object has been modified in the db.
     * Because the modlog's serial will be incremented during save(), we need to subtract 1
     * from the serial. However, during update, the serial will be incremented _after_
     * creating the modlog (see DbObject.updateObject()), so we need to subtract 1 only
     * for the other modlog types.
     */
    setSerial(object.getSerial() - (modType == UPDATE ? 0 : 1));
    objectClass = object.getClassName();
    
    if (modType == INSERT || modType == UPDATE) {
      // keep object for RMI transfers (not DELETE as this will be loaded on the servers side)
      lazyObject = object;
    }
  }
  
  
  /**
   * Creates a modlog from another modlog, but a different type.
   * 
   * @param template the modlog template
   * @param modType is the modification type (INSERT, UPDATE...)
   */
  public ModificationLog(ModificationLog template, char modType) {
    super(template.getDb());
    objectId = template.objectId;
    objectClass = template.objectClass;
    txId = template.txId;
    txName = template.txName;
    when = template.when;
    user = template.user;
    errorCode = template.errorCode;
    message = template.message;
    this.modType = modType;
  }

  
  /**
   * Creates an empty modlog.
   * Constructor only provided for {@link Class#newInstance}.
   */
  public ModificationLog() {
    super();
  }
  
  
  /**
   * {@inheritDoc}.<br>
   * Overridden to set the db in lazyObject too (if unmarshalled from remote db)
   */
  @Override
  public void setDb(Db db) {
    super.setDb(db);
    if (lazyObject != null) {
      lazyObject.setDb(db);
    }
  }
  
  
  /**
   * Clears the lazyObject.
   * Necessary for replaying modlogs that should not copy the lazyObject
   * to a remote db.
   */
  public void clearLazyObject() {
    lazyObject = null;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to check for deferred logging.
   */
  @Override
  public boolean save() {
    boolean rv = true;
    if (getDb().isLogModificationDeferred()) {
      newId();    // obtain an Id to trigger BEGIN only once
      getDb().appendDeferredModificationLog(this);
      setSerial(getSerial() + 1); // increment serial as if it has been saved
    }
    else  {
      if (getDb().isRemote()) {
        lazyObject = null;    // don't transfer this to the remote server
      }
      rv = super.save();
    }
    return rv;
  }

  
  @Override
  public int setFields(PreparedStatementWrapper st) {
    // @wurblet setfields DbSetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:setfields

    int ndx = 0;
    st.setLong(++ndx, objectId); 
    st.setString(++ndx, objectClass); 
    st.setLong(++ndx, txId); 
    st.setString(++ndx, txName); 
    st.setChar(++ndx, modType); 
    st.setTimestamp(++ndx, when); 
    st.setString(++ndx, user); 
    st.setInt(++ndx, errorCode); 
    st.setString(++ndx, message); 
    st.setLong(++ndx, extraId); 
    st.setLong(++ndx, getId());
    st.setLong(++ndx, getSerial());

    // End of wurblet generated code.//GEN-END:setfields
    return ndx;
  }
  
  
  @Override
  public boolean getFields(ResultSetWrapper rs) {
    // @wurblet getfields DbGetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:getfields

    if (columnsValid == false)  {
      if (!isGettingFieldCount() && (rs.getColumnOffset() > 0 || isPartial())) {
        // invoked within joined select the first time
        getFieldCount(); // get column indexes with offset 0
        if (columnsValid == false) {
          DbGlobal.errorHandler.severe(rs.getDb(), null, 
                "initial getFieldCount() failed in " + getTableName()); 
        }
      }
      else {
        COLUMN_OBJECTID = rs.findColumn(FIELD_OBJECTID);
        updateFieldCount(COLUMN_OBJECTID);
        COLUMN_OBJECTCLASS = rs.findColumn(FIELD_OBJECTCLASS);
        updateFieldCount(COLUMN_OBJECTCLASS);
        COLUMN_TXID = rs.findColumn(FIELD_TXID);
        updateFieldCount(COLUMN_TXID);
        COLUMN_TXNAME = rs.findColumn(FIELD_TXNAME);
        updateFieldCount(COLUMN_TXNAME);
        COLUMN_MODTYPE = rs.findColumn(FIELD_MODTYPE);
        updateFieldCount(COLUMN_MODTYPE);
        COLUMN_WHEN = rs.findColumn(FIELD_WHEN);
        updateFieldCount(COLUMN_WHEN);
        COLUMN_USER = rs.findColumn(FIELD_USER);
        updateFieldCount(COLUMN_USER);
        COLUMN_ERRORCODE = rs.findColumn(FIELD_ERRORCODE);
        updateFieldCount(COLUMN_ERRORCODE);
        COLUMN_MESSAGE = rs.findColumn(FIELD_MESSAGE);
        updateFieldCount(COLUMN_MESSAGE);
        COLUMN_EXTRAID = rs.findColumn(FIELD_EXTRAID);
        updateFieldCount(COLUMN_EXTRAID);
        COLUMN_SERIAL = rs.findColumn(FIELD_SERIAL);
        updateFieldCount(COLUMN_SERIAL);
        COLUMN_ID = rs.findColumn(FIELD_ID);
        updateFieldCount(COLUMN_ID);
        columnsValid = true;
      }
    }

    if (rs.getRow() <= 0) {
      return false;   // no valid row
    }

    objectId = rs.getLong(COLUMN_OBJECTID);
    objectClass = rs.getString(COLUMN_OBJECTCLASS);
    txId = rs.getLong(COLUMN_TXID);
    txName = rs.getString(COLUMN_TXNAME);
    modType = rs.getChar(COLUMN_MODTYPE);
    when = rs.getTimestamp(COLUMN_WHEN);
    user = rs.getString(COLUMN_USER);
    errorCode = rs.getInt(COLUMN_ERRORCODE);
    message = rs.getString(COLUMN_MESSAGE);
    extraId = rs.getLong(COLUMN_EXTRAID);
    setId(rs.getLong(COLUMN_ID));
    setSerial(rs.getLong(COLUMN_SERIAL));

    // End of wurblet generated code.//GEN-END:getfields
    return true;
  }  
  
  
  @Override
  public int prepareInsertStatement() {
    // @wurblet insert DbInsert $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:insert

    int stmtId = getInsertStatementId();
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "INSERT INTO " + getTableName()
            + " (" + FIELD_OBJECTID
            + ","  + FIELD_OBJECTCLASS
            + ","  + FIELD_TXID
            + ","  + FIELD_TXNAME
            + ","  + FIELD_MODTYPE
            + ","  + FIELD_WHEN
            + ","  + FIELD_USER
            + ","  + FIELD_ERRORCODE
            + ","  + FIELD_MESSAGE
            + ","  + FIELD_EXTRAID
            + ","  + FIELD_ID
            + ","  + FIELD_SERIAL + ") VALUES (" +
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +
            "?)");
      setInsertStatementId(stmtId);
    }

    // End of wurblet generated code.//GEN-END:insert

    return stmtId;
  }
  

  @Override
  public int prepareUpdateStatement() {
    // @wurblet update DbUpdate $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:update

    int stmtId = getUpdateStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      stmtId = getDb().prepareStatement(
            "UPDATE " + getTableName() + " SET "
            +       FIELD_TXID + "=?"
            + "," + FIELD_TXNAME + "=?"
            + "," + FIELD_ERRORCODE + "=?"
            + "," + FIELD_MESSAGE + "=?"
            + "," + FIELD_EXTRAID + "=?"
            + "," + FIELD_SERIAL + "=" + FIELD_SERIAL + "+1"
            + " WHERE " + FIELD_ID + "=?"
            + " AND " + FIELD_SERIAL + "=?"
            );
      setUpdateStatementId(stmtId);
    }


    // End of wurblet generated code.//GEN-END:update

    return stmtId;
  }
  
  
  /**
   * Just for logging.
   */
  @Override
  public String toString()  {
    String str = "<" + getId() + "/" + modType + ":" + user + "," + txId + (txName == null ? "" : ("/" + txName)) +
                 "," + StringHelper.timestampFormat.format(when) + ">";
    if (objectClass != null && objectId != 0) {
      str += " " + objectClass + "[" + objectId + "]";
    }
    if (errorCode != 0) {
      str += " *** ERROR " + errorCode + " ***";
    }
    if (message != null) {
      str += " (" + message + ")";
    }
    if (extraId != 0) {
      str += " [extra=" + extraId + "]";
    }
    return str;
  }
  
  
  
  /**
   * Gets the object referenced by this ModificationLog.<br>
   * The object is lazily cached if the given db equals
   * the db of this modlog.
   *
   * @param db is the db-connection from which to load the object.
   * @return the object or null if not found.
   * @throws ApplicationException if instantiation failed.
   */
  public DbObject getObject(Db db) throws ApplicationException {
    
    if (lazyObject != null && lazyObject.getDb().equals(db)) {
      return lazyObject;  // already lazily cached
    }
    
    try {
      lazyObject = objectId != 0 ? DbObject.select(db, objectClass, objectId) : null;
      return lazyObject;
    } 
    catch (Exception ex) {
      throw new ApplicationException("can't load object " + objectClass + "[" + objectId + "] from " + db, ex);
    }
  }
  
  
  /**
   * Gets the object referenced by this ModificationLog.
   * The object is lazily cached.
   *
   * @return the object or null if not found.
   * @throws ApplicationException if instantiation failed.
   */
  public DbObject getObject() throws ApplicationException {
    return getObject(getDb());
  }
  
  
  /**
   * Selects the next record to process, i.e. the one with the lowest ID
   * and errorCode == 0.<br>
   * When processed the modlog should be deleted.
   *
   * @return the modlog, null if no unprocessed log found
   * @wurblet selectFirst DbSelectUnique $mapfile $remote errorCode:=:0 --append="\" ORDER BY \" + FIELD_ID"
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectFirst

  public ModificationLog selectFirst() {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        ModificationLog obj = ((ModificationLogRemoteDelegate)getRemoteDelegate()).selectFirst();
        Db.applyToDbObject(getDb(), obj);
        return obj;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectFirst failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectFirstStatementId;
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      String sql = "SELECT ";
      if (getDb().isOracle()) {
        sql += "/*+ FIRST_ROWS */ * FROM (SELECT " + getSqlAllFields() 
                    + " AND " + FIELD_ERRORCODE + "=?";
      }
      else if (getDb().sqlRequiresNestedLimitSelect()) {
        sql += "* FROM (SELECT F_O_O.*, ROW_NUMBER() OVER() AS R_O_W FROM (SELECT " + getSqlAllFields() 
                    + " AND " + FIELD_ERRORCODE + "=?";
      }
      else {
        if (getDb().sqlRequiresLimitAppended()) {
          sql += getSqlAllFields() 
                    + " AND " + FIELD_ERRORCODE + "=?";
        }
        else  {
          sql += getDb().sqlFormatLimitClause("1") + " " + getSqlAllFields() 
                    + " AND " + FIELD_ERRORCODE + "=?";
        }          
      }  
      sql += " ORDER BY " + FIELD_ID;
      if (getDb().isOracle()) {
        sql += ") WHERE ROWNUM <= 1";
      }
      else if (getDb().sqlRequiresNestedLimitSelect()) {
        sql += ") AS F_O_O) AS B_A_R WHERE R_O_W <= 1";
      }
      else if (getDb().sqlRequiresLimitAppended()) {
        sql += getDb().sqlFormatLimitClause("1");
      }
      stmtId = getDb().prepareStatement(sql);
      selectFirstStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setInt(ndx++, 0);
    ResultSetWrapper rs = st.executeQuery();
    try {
      if (rs.next() && readFromResultSetWrapper(rs)) {
        return this;  // found
      }
      else {
        return null;  // not found
      }
    }
    finally {
      rs.close();
    }
  }

  private static int selectFirstStatementId;


  // End of wurblet generated code.//GEN-END:selectFirst

  
  
  
  /**
   * Gets the modlogs for a given object.
   *
   * @param objectClass the object's class name
   * @param objectId the object's ID
   * @return the list of modlogs
   * @wurblet selectByObject DbSelectList $mapfile $remote objectClass objectId --append="\" ORDER BY \" + FIELD_ID"
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectByObject

  public List<ModificationLog> selectByObject(String objectClass, long objectId) {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        List<ModificationLog> list = ((ModificationLogRemoteDelegate)getRemoteDelegate()).selectByObject(objectClass, objectId);
        Db.applyToCollection(getDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByObject failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectByObjectStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      String sql = "SELECT ";
      sql += getSqlAllFields() 
                    + " AND " + FIELD_OBJECTCLASS + "=?"
                    + " AND " + FIELD_OBJECTID + "=?";
      sql += " ORDER BY " + FIELD_ID;
      stmtId = getDb().prepareStatement(sql);
      selectByObjectStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setString(ndx++, objectClass);
    st.setLong(ndx++, objectId);
    ResultSetWrapper rs = st.executeQuery();
    List<ModificationLog> list = new ArrayList<ModificationLog>();
    boolean derived = getClass() != ModificationLog.class;
    while (rs.next()) {
      ModificationLog obj = derived ? (ModificationLog)newObject() : new ModificationLog(getDb());
      if (obj.readFromResultSetWrapper(rs))  {
        list.add(obj);
      }
    }
    rs.close();
    return list;
  }

  private static int selectByObjectStatementId;


  // End of wurblet generated code.//GEN-END:selectByObject

  /**
   * Checks if there are logs for a given object.
   *
   * @param db the db connection
   * @param objectClass the object's classname
   * @param objectId the object's ID
   * @return true if there are logs
   * @wurblet isReferencingUser DbIsReferencing $mapfile $remote user --static
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:isReferencingUser

  public static boolean isReferencingUser(Db db, String user) {
    if (db.isRemote())  {
      // invoke remote method
      try {
        return ((ModificationLogRemoteDelegate)classVariables.getRemoteDelegate(db)).isReferencingUser(user);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote isReferencingUser failed");
        return false;
      }
    }
    // else: local mode
    int stmtId = isReferencingUserStatementId;
    if (stmtId == 0 || classVariables.alwaysPrepare) {
      // prepare it
      String sql = FIELD_USER + "=?";
      if (db.isOracle()) {
        stmtId = db.prepareStatement(
              "SELECT /*+ FIRST_ROWS */ " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
              sql + " AND ROWNUM <=1");
      }
      else if (db.sqlRequiresNestedLimitSelect()) {
        stmtId = db.prepareStatement(
              "SELECT * FROM (SELECT ROW_NUMBER() OVER () AS R_O_W, " + FIELD_ID + " FROM " + classVariables.tableName + ") AS F_O_O WHERE " +
              sql + " AND R_O_W <=1");        
      }
      else {
        if (db.sqlRequiresLimitAppended()) {
          stmtId = db.prepareStatement(
                "SELECT " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
                sql + db.sqlFormatLimitClause("1"));
        }
        else  {
          stmtId = db.prepareStatement(
                "SELECT" + db.sqlFormatLimitClause("1") + " " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
                sql);          
        }
      }
      isReferencingUserStatementId = stmtId;
    }
    PreparedStatementWrapper st = db.getPreparedStatement(stmtId);
    int ndx = 1;
    st.setString(ndx++, user);
    ResultSetWrapper rs = st.executeQuery();
    boolean ref = rs.next();
    rs.close();
    return ref;
  }

  private static int isReferencingUserStatementId;


  /**
   * Checks whether references exist.
   * @param user name of user
   * @return true if referencing
   */
  public boolean isReferencingUser(String user) {
    return isReferencingUser(getDb(), user);
  }

  // End of wurblet generated code.//GEN-END:isReferencingUser



  
  /**
   * Checks if there are logs for a given object.
   *
   * @param db the db connection
   * @param objectClass the object's classname
   * @param objectId the object's ID
   * @return true if there are logs
   * @wurblet isReferencingObject DbIsReferencing $mapfile $remote objectClass objectId --static
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:isReferencingObject

  public static boolean isReferencingObject(Db db, String objectClass, long objectId) {
    if (db.isRemote())  {
      // invoke remote method
      try {
        return ((ModificationLogRemoteDelegate)classVariables.getRemoteDelegate(db)).isReferencingObject(objectClass, objectId);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(db, e, "remote isReferencingObject failed");
        return false;
      }
    }
    // else: local mode
    int stmtId = isReferencingObjectStatementId;
    if (stmtId == 0 || classVariables.alwaysPrepare) {
      // prepare it
      String sql = "(" + FIELD_OBJECTCLASS + "=?" + " AND " + FIELD_OBJECTID + "=?" + ")";
      if (db.isOracle()) {
        stmtId = db.prepareStatement(
              "SELECT /*+ FIRST_ROWS */ " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
              sql + " AND ROWNUM <=1");
      }
      else if (db.sqlRequiresNestedLimitSelect()) {
        stmtId = db.prepareStatement(
              "SELECT * FROM (SELECT ROW_NUMBER() OVER () AS R_O_W, " + FIELD_ID + " FROM " + classVariables.tableName + ") AS F_O_O WHERE " +
              sql + " AND R_O_W <=1");        
      }
      else {
        if (db.sqlRequiresLimitAppended()) {
          stmtId = db.prepareStatement(
                "SELECT " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
                sql + db.sqlFormatLimitClause("1"));
        }
        else  {
          stmtId = db.prepareStatement(
                "SELECT" + db.sqlFormatLimitClause("1") + " " + FIELD_ID + " FROM " + classVariables.tableName + " WHERE " +
                sql);          
        }
      }
      isReferencingObjectStatementId = stmtId;
    }
    PreparedStatementWrapper st = db.getPreparedStatement(stmtId);
    int ndx = 1;
    st.setString(ndx++, objectClass);
    st.setLong(ndx++, objectId);
    ResultSetWrapper rs = st.executeQuery();
    boolean ref = rs.next();
    rs.close();
    return ref;
  }

  private static int isReferencingObjectStatementId;


  /**
   * Checks whether references exist.
   * @param objectClass object classname
   * @param objectId object id
   * @return true if referencing
   */
  public boolean isReferencingObject(String objectClass, long objectId) {
    return isReferencingObject(getDb(), objectClass, objectId);
  }

  // End of wurblet generated code.//GEN-END:isReferencingObject

  
  
  public DbObjectClassVariables getDbObjectClassVariables() {
    return classVariables;
  }  


  // @wurblet methods DbMethods $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:methods


  /**
   * Gets the db attribute objectId
   *
   * @return object id
   */
  public long getObjectId()    {
    return objectId;
  }

  /**
   * objectId is readonly, no set-method.
   */  

  /**
   * Gets the db attribute objectClass
   *
   * @return object classname
   */
  public String getObjectClass()    {
    return objectClass;
  }

  /**
   * objectClass is readonly, no set-method.
   */  

  /**
   * Gets the db attribute txId
   *
   * @return transaction id (optional)
   */
  public long getTxId()    {
    return txId;
  }

  /**
   * Sets the db attribute txId
   *
   * @param txId transaction id (optional)
   */
  public void setTxId(long txId) {
    this.txId = txId;
  }

  /**
   * Gets the db attribute txName
   *
   * @return transaction name (optional)
   */
  public String getTxName()    {
    return txName;
  }

  /**
   * Sets the db attribute txName
   *
   * @param txName transaction name (optional)
   */
  public void setTxName(String txName) {
    this.txName = txName;
  }

  /**
   * Gets the db attribute modType
   *
   * @return modification type
   */
  public char getModType()    {
    return modType;
  }

  /**
   * modType is readonly, no set-method.
   */  

  /**
   * Gets the db attribute when
   *
   * @return time of event
   */
  public Timestamp getWhen()    {
    return when;
  }

  /**
   * when is readonly, no set-method.
   */  

  /**
   * Gets the db attribute user
   *
   * @return name of user
   */
  public String getUser()    {
    return user;
  }

  /**
   * user is readonly, no set-method.
   */  

  /**
   * Gets the db attribute errorCode
   *
   * @return error number (0=ok)
   */
  public int getErrorCode()    {
    return errorCode;
  }

  /**
   * Sets the db attribute errorCode
   *
   * @param errorCode error number (0=ok)
   */
  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Gets the db attribute message
   *
   * @return optional informational or error message
   */
  public String getMessage()    {
    return message;
  }

  /**
   * Sets the db attribute message
   *
   * @param message optional informational or error message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the db attribute extraId
   *
   * @return some optional id for any purpose
   */
  public long getExtraId()    {
    return extraId;
  }

  /**
   * Sets the db attribute extraId
   *
   * @param extraId some optional id for any purpose
   */
  public void setExtraId(long extraId) {
    this.extraId = extraId;
  }

  // End of wurblet generated code.//GEN-END:methods



  // @wurblet declare DbDeclare $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:declare


  /** object id **/
  private long objectId;

  /** object classname **/
  private String objectClass;

  /** transaction id (optional) **/
  private long txId;

  /** transaction name (optional) **/
  private String txName;

  /** modification type **/
  private char modType;

  /** time of event **/
  private Timestamp when;

  /** name of user **/
  private String user;

  /** error number (0=ok) **/
  private int errorCode;

  /** optional informational or error message **/
  private String message;

  /** some optional id for any purpose **/
  private long extraId;

  // End of wurblet generated code.//GEN-END:declare




  // @wurblet fieldlenghts DbFieldNames $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:fieldlenghts

  private static boolean columnsValid;    // true if COLUMN_.... are valid for getFields()
  /** database column name for objectId **/
  public  static final String FIELD_OBJECTID = "objectid";
  private static       int    COLUMN_OBJECTID;
  /** database column name for objectClass **/
  public  static final String FIELD_OBJECTCLASS = "objectclass";
  private static       int    COLUMN_OBJECTCLASS;
  /** database column name for txId **/
  public  static final String FIELD_TXID = "txid";
  private static       int    COLUMN_TXID;
  /** database column name for txName **/
  public  static final String FIELD_TXNAME = "txname";
  private static       int    COLUMN_TXNAME;
  /** database column name for modType **/
  public  static final String FIELD_MODTYPE = "modtype";
  private static       int    COLUMN_MODTYPE;
  /** database column name for when **/
  public  static final String FIELD_WHEN = "modtime";
  private static       int    COLUMN_WHEN;
  /** database column name for user **/
  public  static final String FIELD_USER = "moduser";
  private static       int    COLUMN_USER;
  /** database column name for errorCode **/
  public  static final String FIELD_ERRORCODE = "errorcode";
  private static       int    COLUMN_ERRORCODE;
  /** database column name for message **/
  public  static final String FIELD_MESSAGE = "message";
  private static       int    COLUMN_MESSAGE;
  /** database column name for extraId **/
  public  static final String FIELD_EXTRAID = "extraid";
  private static       int    COLUMN_EXTRAID;
  private static       int    COLUMN_ID;
  private static       int    COLUMN_SERIAL;

  // End of wurblet generated code.//GEN-END:fieldlenghts


  
  // @wurblet fieldnames DbFieldLengths $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:fieldnames

  /** maximum number of characters for objectClass **/
  public static final int LENGTH_OBJECTCLASS = 64;
  /** maximum number of characters for txName **/
  public static final int LENGTH_TXNAME = 64;
  /** maximum number of characters for user **/
  public static final int LENGTH_USER = 32;

  // End of wurblet generated code.//GEN-END:fieldnames
  
  
  

  /**
   * A single ModificationLog as a global hook for the methods below.
   * The global log object is created by a factory method and is provided
   * for the replay methods.
   */
  private static ModificationLog modlog;
  
  /**
   * The classname of the global modlog.
   * Change this in your app if you're extending ModificationLog.
   */
  public static String modlogClassname = "org.tentackle.db.ModificationLog";
  
  /**
   * Gets the global modification logger object.
   * (notice: no singleton hype here, it's just a hook for the methods below)
   * @return the modlog 
   */
  public static ModificationLog instance() {
    if (modlog == null) {
      try {
        Class clazz = Class.forName(modlogClassname);
        modlog = (ModificationLog)clazz.newInstance();
      }
      catch (Exception ex) {
        DbGlobal.errorHandler.severe(ex, "instantiating modlog object for " + modlogClassname + " failed");
      } 
    }
    return modlog;
  }
  
  
  
  
  /**
   * Applies a modification to another db.<br>
   * The method is not static to allow overriding (e.g. to extend with more transaction types).
   * Method is invoked within a tx, so no begin/commit/rollback necessary
   *
   * @param modlog the modification log to replay
   * @param toDb the db the logs will be applied to
   *
   * @return true if replayed, false if some db-error
   *
   * @throws ApplicationException if applied to BEGIN/COMMIT or object does not exist or integrity rules failed
   */
  public boolean replay(ModificationLog modlog, Db toDb) throws ApplicationException {


    if (DbGlobal.logger.isFineLoggable()) {
      DbGlobal.logger.fine("replaying modlog " + modlog + " to " + toDb);
    }

    // load the object (lazily during rmi, because the object is already switched to local db)
    DbObject object = modlog.getObject(modlog.modType == DELETE ? toDb : modlog.getDb());
    
    if (object == null) {
      throw new ApplicationException("object " + modlog.objectClass + "[" + modlog.objectId + "] does not exist");
    }
    
    if (modlog.modType != DELETE) {
      object.setDb(toDb);
    }

    boolean rv;
    
    try {
      // perform any preprocessing
      rv = object.initModification(modlog.modType);
      
      if (rv) {
        if      (modlog.modType == INSERT) {
          // se the modlog's serial (usually 1), in case multiple updates follow
          object.setSerial(modlog.getSerial());
          rv = object.insertPlain();
        }
        else if (modlog.modType == UPDATE) {
          if (object.getSerial() > modlog.getSerial()) {
            // update only the serial
            object.setSerial(modlog.getSerial() - 1);
            rv = object.updateSerial();
          }
          else  {
            // this is the final update
            object.setSerial(object.getSerial() - 1);
            rv = object.updatePlain();
          }
        }
        else if (modlog.modType == DELETE) {
          // se the modlog's serial, in case insert and further updates follow
          object.setSerial(modlog.getSerial());
          rv = object.deletePlain();
        }
        else  {
          throw new ApplicationException("illegal modType " + modlog.modType + " for replay");
        }

        // perform any post processing
        if (rv) {
          rv = object.finishModification(modlog.modType);
          if (!rv) {
            DbGlobal.logger.warning("finishModification for " + modlog + " failed");
          }
        }
        else  {
          DbGlobal.logger.warning("modification operation failed for " + modlog);
        }
      }
      else {
        DbGlobal.logger.warning("initModification for " + modlog + " failed");
      }
    }
    finally {
      if (modlog.modType != DELETE) {
        object.setDb(modlog.getDb());
      }
    }
    
    return rv;
  }
  
  
  
  /**
   * Replays a list of modlogs within a single transaction.<br>
   * It will also create new txId if the modlogs are copied.
   * The method is not static to allow overriding (e.g. to extend with more transaction types).
   *
   * @param modList the list of log objects from the source db
   * @param copyLog true to copy the logs as well
   * @param toDb the db the logs will be applied to
   *
   * @return the txId if copyLog was true and a BEGIN log found, 0 otherwise
   *
   * @throws ApplicationException if replay failed
   */
  public long replay(List<ModificationLog> modList, boolean copyLog, Db toDb) throws ApplicationException {
    
    boolean logEnabled = toDb.isLogModificationAllowed();
    toDb.setLogModificationAllowed(false);    // don't log twice!
    
    boolean oldCommit = toDb.begin();
    
    long pendingTxId = 0;    // != 0 if txId pending
    
    try {
      for (ModificationLog log: modList) {
        // replay object's modification
        if (log.modType != BEGIN && log.modType != COMMIT &&    // ignore BEGIN/COMMIT
            replay(log, toDb) == false) {                       // replat next modlog
          throw new ApplicationException("replaying modlog <" + log + "> failed");
        }
        if (copyLog) {
          if (DbGlobal.logger.isFineLoggable()) {
            DbGlobal.logger.fine("copying modlog " + modlog + " to " + toDb);
          }
          log.clearLazyObject();    // no more needed, and don't change db in lazyObject and don't transfer to remote db
          Db oldDb = log.getDb();
          log.setDb(toDb);
          log.setId(0);
          log.newId();        // get a new Id for toDb!
          log.txId = pendingTxId;    // set the txId if possible
          if (log.insertPlain() == false) {
            throw new ApplicationException("copying modlog " + log + " failed");
          }
          if (log.modType == BEGIN) {
            pendingTxId = log.getId();
          }
          else if (log.modType == COMMIT) {
            pendingTxId = 0;   // clear pending id
          }
          log.setDb(oldDb);
        }
      }
      toDb.commit(oldCommit);
      toDb.setLogModificationAllowed(logEnabled);
      return pendingTxId;
    }
    catch (Exception e) {
      toDb.rollback(oldCommit);
      toDb.setLogModificationAllowed(logEnabled);
      if (e instanceof ApplicationException)  {
        throw (ApplicationException)e;
      }
      else  {
        throw new ApplicationException("replay modList failed", e);
      }
    }
  }
  
  
  
}
