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

// $Id: History.java 452 2009-03-13 14:01:46Z harald $

package org.tentackle.appworx;

import org.tentackle.appworx.rmi.HistoryRemoteDelegate;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.PreparedStatementWrapper;
import org.tentackle.db.ResultSetWrapper;
import org.tentackle.db.SqlHelper;
import org.tentackle.ui.FormTableEntry;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.tentackle.db.DbObject;

/**
 * Class to read and write history records for a given AppDbObject-class.
 *
 * There are two options to run history-based tables:
 *
 * <ol>
 * 
 * <li> History-tables are managed by the dbms itself. Usually they are created
 *      as triggers like this (postgres):
 * <pre>
 * # usage: makeHistoryRules.sh <tablename>
 *
 * newtable=h_"$1"
 * echo '
 * CREATE TABLE' "$newtable" 'AS SELECT * FROM' "$1" 'WHERE ID=-1;
 * ALTER TABLE' "$newtable" 'ADD h_time TIMESTAMP;
 * ALTER TABLE' "$newtable" 'ADD h_user TEXT;
 * ALTER TABLE' "$newtable" 'ADD h_type CHAR(1);
 * CREATE INDEX' "$newtable"_id 'ON' "$newtable" '(id, serial, h_time);
 * GRANT ALL ON' "$newtable" 'TO PUBLIC;
 * CREATE RULE insert_'"$newtable" 'AS ON INSERT TO' "$1" 'DO INSERT INTO' "$newtable" 'VALUES (new.*,current_timestamp,current_user,'"'I'"');
 * CREATE RULE update_'"$newtable" 'AS ON UPDATE TO' "$1" 'DO INSERT INTO' "$newtable" 'VALUES (new.*,current_timestamp,current_user,'"'U'"');
 * CREATE RULE delete_'"$newtable" 'AS ON DELETE TO' "$1" 'DO INSERT INTO' "$newtable" 'VALUES (old.*,current_timestamp,current_user,'"'D'"');
 * </pre>
 * </li>
 * <li> Tentacle itself is responsible for creating the history records.
 *    In this case AppDbObject.isLoggingHistory() must be overridden to return true.
 *    Notice however that tentackle-bases history usually runs slower and not all
 *    modifications will be caught, e.g. deleteAll() or editedBy-changes.
 *    An AppDbObject that provides access to its history needs to subclass 'History' as follows:
 * <pre>
 * public class BlaHistory extends History {
 *
 *   public BlaHistory(Blah blah)  {
 *     super(blah);
 *   }
 *
 *   public BlaHistory() {
 *     super();
 *   }
 *   
 *   public AppDbObjectClassVariables getAppDbObjectClassVariables() {
 *     return classVariables;
 *   }  
 *
 *  private static AppDbObjectClassVariables classVariables = 
 *     new AppDbObjectClassVariables(BlaHistory.class, HISTORY_TABLENAME_PREFIX + 
 *          Bla.TABLENAME, "History Entry", "History Entries");
 *   }
 * </pre>
 * </li>
 * </ol>
 * 
 * In order to automatically enable the GUI-Features to access the history, the AppDbObject
 * must override two methods: allowsHistory and getHistory() as follows:
 *
 * <pre>
 *   public boolean allowsHistory() {
 *     return true;
 *   }
 *   
 *   public History getHistory() {
 *     return new BlaHistory(this);
 *   }
 * </pre>
 * 
 * The wurblet "{@code History}" does all that automatically.
 * 
 */
public abstract class History extends AppDbObject {
  

  /** prefix prepended to the table name of history objects **/
  public final static String HISTORY_TABLENAME_PREFIX = "h_";
  
  // additional FIELDs to the original table
  /** database column name for the timestamp when modification took place **/
  public final static String FIELD_TIME = "h_time";
  /** database column name for the user who did the modification **/
  public final static String FIELD_USER = "h_user";
  /** database column name for the modification type **/
  public final static String FIELD_TYPE = "h_type";
  
  
  /**
   * Converts a modification type to a localized string.
   * 
   * @param type the modification type (INSERT, UPDATE or DELETE)
   * @return the localized string
   */
  public static String typeToString(char type)  {
    switch (type) {
      case INSERT:  return Locales.bundle.getString("insert");
      case UPDATE:  return Locales.bundle.getString("update");
      case DELETE:  return Locales.bundle.getString("delete");
    }
    return "?" + type + "?";
  }
 

  
  protected AppDbObject appDbObject;  // holds the AppDbObject this History refers to
  private   Timestamp   time;         // timestamp
  private   String      user;         // username
  private   char        type;         // modification type


  /**
   * Creates a history object.
   * 
   * @param appDbObject the database object
   */
  public History(AppDbObject appDbObject) {
    this(appDbObject.getContextDb());
    this.appDbObject = appDbObject;
    this.type = ' '; // unknown
  }
  
  /**
   * Creates an empty history object.
   * 
   * @param db the database context
   */
  public History(ContextDb db) {
    super (db);
  }
  
  /**
   * Creates an empty history object.
   */
  public History() {
    super();
  }
  


  /**
   * Gets the modification time.
   *
   * @return the modification time
   */
  public Timestamp getTime() {
    return time;
  }  

  
  /**
   * Gets the username. 
   *
   * @return the username
   */
  public String getUser() {
    return user;
  }
  
  
  /**
   * Gets the modification type.
   *
   * @return the modification type.
   */
  public char getType() {
    return type;
  }


  /**
   * Gets the referenced object.
   *
   * @return the database object
   */
  public AppDbObject getAppDbObject() {
    return appDbObject;
  }  
  
  
  
  @Override
  public AppDbObject newObject() {
    History history = (History)super.newObject();
    history.appDbObject = appDbObject.newObject();
    return history;
  }    
  
  
  /**
   * Selects all history objects for a given object id, sorted by time.<br>
   * In order not to confuse the user, suppresses changes on the data not visible to the user.
   * 
   * @param objectId the object ID
   * @return the list of History objects
   */
  public List<History> selectByObjectId(long objectId) {
    if (getDb().isRemote())  {
      try {
        List<History> list = ((HistoryRemoteDelegate)getRemoteDelegate()).selectByObjectId(objectId);
        ContextDb.applyToCollection(getContextDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByObjectId failed");
        return null;
      }
    }
    else  {
      List<History> list = new ArrayList<History>();   // returned list of objects
      PreparedStatementWrapper st = getDb().getPreparedStatement(getSqlSelectAllFields() + 
                  " AND " + FIELD_ID + "=? ORDER BY " + FIELD_ID + "," + FIELD_SERIAL + "," + FIELD_TIME);
      st.setLong(1, objectId);
      ResultSetWrapper rs = st.executeQuery();

      HistoryTableEntry previous = null;
      
      while (rs.next()) {
        try {
          History history = (History)newObject();
          if (history.readFromResultSetWrapper(rs)) {
            HistoryTableEntry next = new HistoryTableEntry(history);
            if (previous == null || appDbObject.isLoggingHistory() || 
                next.isVisiblyEqual(previous) == false) {
              /**
               * Notice:
               * if isLoggingHistory we don't need to suppress noise like editedBy-stuff,
               * hence we don't need expensive isVisiblyEqual.
               */
              list.add(history);
            }
            previous = next;
          }
        }
        catch (Exception ex)  {
          DbGlobal.errorHandler.severe(getDb(), ex, null); 
        }
      }
      rs.close();
      return list;
    }
  }





  /**
   * Restores an object for a given revision, i.e. serial.<br>
   * For composites this requires that the parent object's methods {@link DbObject#isTxObject()}
   * and {@link DbObject#isUpdatingSerialEvenIfNotModified()} return true
   * as this will force all updated childs of the composite parent to get the serial of the parent.
   * Important: the method will not load the childs and has to be overwridden to do so (which
   * depends on the application's semantics).
   * @param objectId the object's ID
   * @param objectSerial the object's revision/serial
   * @return the object at given revision or older, null if none
   */
  public AppDbObject restoreObjectForSerial(long objectId, long objectSerial) {

    if (getDb().isRemote())  {
      try {
        appDbObject = ((HistoryRemoteDelegate)getRemoteDelegate()).restoreObjectForSerial(getContextDb(), objectId, objectSerial);
        ContextDb.applyToContextDependable(getContextDb(), appDbObject);
        return appDbObject;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote restoreObjectForSerial failed");
        return null;
      }
    }
    else  {
      StringBuilder sql = new StringBuilder(getSqlSelectAllFields() +
                      " AND " + FIELD_ID + "=? AND " + FIELD_SERIAL + "<=? ORDER BY " + FIELD_SERIAL + " DESC");
      sql = getDb().addLimitOffsetToSql(sql, 1, 0);   // limit by 1

      PreparedStatementWrapper st = getDb().getPreparedStatement(sql.toString());

      int ndx = 1;
      ndx = getDb().prependLimitOffsetToPreparedStatement(ndx, st, 1, 0);
      st.setLong(ndx++, objectId);
      st.setLong(ndx++, objectSerial);
      getDb().appendLimitOffsetToPreparedStatement(ndx, st, 1, 0);

      ResultSetWrapper rs = st.executeQuery();

      if (rs.next() && appDbObject.readFromResultSetWrapper(rs)) {
        rs.close();
        return appDbObject;
      }
      rs.close();

      return null;
    }
  }




  
  
  /**
   * Creates a history log for the current object and saves
   * it to the database.<br>
   * The username is taken from the database {@code UserInfo}.
   * The time is the current system time.
   * 
   * @param type the modification type
   * @return true if this History object saved
   */
  public boolean createHistoryLog(char type)  {
    this.type = type;
    user = getDb().getUserInfo().getUsername();
    time = SqlHelper.now();
    return save();
  }
  

  public boolean getFields(ResultSetWrapper rs)  {
    time = rs.getTimestamp(FIELD_TIME);
    user = rs.getString(FIELD_USER);
    type = rs.getChar(FIELD_TYPE);
    return true;
  }

  
  @Override
  public boolean readFromResultSetWrapper(ResultSetWrapper rs, boolean withLinkedObjects)  {
    return super.readFromResultSetWrapper(rs, withLinkedObjects) &&       // this reads time, user and type
           appDbObject.readFromResultSetWrapper(rs, withLinkedObjects);   // this reads the referenced object
  }


  public int setFields(PreparedStatementWrapper st)	{
    DbObject txObject = appDbObject.getDb().getTxObject();
    int ndx;
    if (txObject != null) {
      // take serial of txObject (for restauration of older versions from history)
      long oldSerial = appDbObject.getSerial();
      appDbObject.setSerial(txObject.getSerial());
      ndx = appDbObject.setFields(st);
      appDbObject.setSerial(oldSerial);
    }
    else  {
      ndx = appDbObject.setFields(st);
    }
    if (ndx >= 0) {
      st.setTimestamp(++ndx, time); 
      st.setString(++ndx, user); 
      st.setChar(++ndx, type);
    }
    return ndx;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to take the sql-string from the {@link AppDbObject} and
   * modify it. This method relies on the special format
   * of an insert-statement! Make sure to let the insert-statement
   * be created by the insert-wurblet!!!
   */
  public int prepareInsertStatement ()  {
    if (getAppDbObjectClassVariables().insertStatementId == 0)  {
      // prepare it
      appDbObject.prepareInsertStatement();   // if not yet done
      String sql = getDb().getSqlText(appDbObject.getAppDbObjectClassVariables().insertStatementId);
      
      try {
        sql = "INSERT INTO " + getAppDbObjectClassVariables().tableName + " (" +
              SqlHelper.extractColumnsFromInsertStatement(sql) +
              "," + FIELD_TIME +
              "," + FIELD_USER +
              "," + FIELD_TYPE + ") VALUES (?,?,?," +
              SqlHelper.extractValuesFromInsertStatement(sql) + ")";

        getAppDbObjectClassVariables().insertStatementId = getDb().prepareStatement(sql);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, 
                "prepare insert history failed in " + getClassName() + " for " + appDbObject.getClassName());
      }
    }
    return getAppDbObjectClassVariables().insertStatementId;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Invokes the errorhandler because History objects cannot be updated.
   */
  @Override
  public int prepareUpdateStatement() {
    // no update allowed
    DbGlobal.errorHandler.severe(getDb(), null, 
        Locales.bundle.getString("illegal_update_of_history_object_for_") + " " + getTableName());
    return 0;   // not reached
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Invokes the errorhandler because History objects cannot be deleted.
   */
  @Override
  public int prepareDeleteStatement() {
    // no delete allowed
    DbGlobal.errorHandler.severe(getDb(), null, 
        Locales.bundle.getString("illegal_delete_of_history_object_for_") + " " + getTableName());
    return 0; // not reached
  }


  @Override
  public String toString()  {
    return typeToString(type) + ", " + time + ", " + user + ": " + appDbObject;
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Returns a {@link HistoryTableEntry}.
   */
  @Override
  public FormTableEntry getFormTableEntry() {
    return new HistoryTableEntry(this);
  }  

}
