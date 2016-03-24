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

// $Id: ModificationCounter.java 439 2008-09-18 15:09:32Z harald $

package org.tentackle.db;

/**
 * Counter to track modifications for a class/table.<br>
 * There is one ModificationCounter-object per {@link DbObject}-class *AND* {@link Db}-connection.
 * The Db maintains a list of ModificationCounter-objects, one for each table.
 * <p>
 * The physical count for UPDATE and INSERT (i.e. update to the database) is minimized
 * within transactions. If there are only updates and inserts the counter
 * (i.e. the tableSerial) increments by 1.
 * However, each DELETE gets its own tableSerial which will allow other JVMs to detect
 * that "some objects have been deleted" by gaps in the sequence of tableSerials.
 * {@link org.tentackle.appworx.AppDbObjectCache} and {@link DbPreferences} make use of that trick.
 * 
 * @author harald
 */
public class ModificationCounter {
  
  // for the modification table
  private static int countModificationStatementId;
  private static int selectTableSerialStatementId;
  private static int selectModificationStatementId;
  

  /** the modification-table's name **/
  public static final String TABLENAME = "modification";
  
  /** column name for the tablename **/
  public static final String FIELD_TABLENAME  = "tablename"; 
  
  
  private Db db;                                // the connection this object lives in
  private String tableName;                     // the name of the table
  private long lastTxCount;                     // txCount when countModification() was invoked last
  private long lastSerial;                      // last serial no for this table
  private boolean isTableSerialValid;           // true if table serial is managed by table
  
  
  
  /**
   * Creates a modification counter for a given tablename.
   * 
   * @param db the db connection
   * @param tableName the tablename
   */
  public ModificationCounter(Db db, String tableName) {
    this.db = db;
    this.tableName = tableName;
    
    DbObjectClassVariables clazzVar = DbObjectClassVariables.getVariables(tableName);
    if (clazzVar == null) {
      throw new IllegalStateException("no clazzvariables registered for " + tableName);
    }
    try {
      isTableSerialValid = DbObject.newByClass(clazzVar.clazz).isTableSerialValid();
    } 
    catch (Exception ex) {
      throw new IllegalStateException("can't evaluate isTableSerialValid() for " + clazzVar, ex);
    }
  }
  
  
  
  /**
   * Counts the modification for the table this counter belongs to.<br>
   * If the counter does not exist in the modification table, an appropriate
   * entry will be created.
   * 
   * @param checkModificationSerial is true if the new tableSerial should be returned
   * @param optimize is true if counting should be optimized within transactions. Otherwise
   *        this and the succeeding invocation performs a physical count.
   * @return the table serial
   */
  public long countModification (boolean checkModificationSerial, boolean optimize)  {
    db.assertNotRemote();
    // check if not already counted in current transaction
    if (db.isAutoCommit() || !optimize || db.getTxCount() > lastTxCount) {
      // either not in tx, or no optimization or really a new tx
      lastTxCount = optimize ? db.getTxCount() : 0;    // remember current tx
      // prepare statement if not yet done
      if (countModificationStatementId == 0) {
        countModificationStatementId = db.prepareStatement(
            "UPDATE " + TABLENAME + " SET " + DbObject.FIELD_SERIAL + "=" + DbObject.FIELD_SERIAL + 
            "+1 WHERE " + FIELD_TABLENAME + "=? OR " + DbObject.FIELD_ID + "=?");
      }
      PreparedStatementWrapper st = db.getPreparedStatement(countModificationStatementId);
      st.setString(1, tableName);
      st.setLong(2, 0);       // this is for the master!
      if (st.executeUpdate() != 2)  {
        // probably the record doesn't exist? create it!
        addModificationTable();
        // try again
        // prepared statement used twice: we must attach again
        db.getConnectionManager().attach(db.getConnectionId());
        st.markReady();
        if (st.executeUpdate() != 2)  {
          DbGlobal.errorHandler.severe(db, null, Locales.bundle.getString("Mod-Table_Fehler"));
        }
      }
      
      if (checkModificationSerial) {
        // read back the serial 
        if (selectTableSerialStatementId == 0)  {
          selectTableSerialStatementId = db.prepareStatement(
                  "SELECT " + DbObject.FIELD_SERIAL + " FROM " + TABLENAME + 
                  " WHERE " + FIELD_TABLENAME + "=?");
        }
        st = db.getPreparedStatement(selectTableSerialStatementId);
        st.setString(1, tableName);

        ResultSetWrapper rs = st.executeQuery();
        if (rs.next())  {
          lastSerial = rs.getLong(1);
        }
        else  {
          DbGlobal.errorHandler.severe(db, null, "can't get serial for table " + tableName);
        }
        rs.close();
      }
    }
    return lastSerial;
  }
  
  
  /**
   * Adds an entry for this counter (== database object class)
   * to the modification table.
   * The tableserial is derived from the highest serial of all objects (i.e. class)
   * this counter refers to.
   */
  public void addModificationTable()  {
    StatementWrapper st = db.createStatement();
    if (st.executeUpdate (
        db.isMysql() ?
          // MySQL does not allow updates with expressions referring to the table being updated. 
          // This does the trick though ;-)
          "INSERT INTO " + TABLENAME + " SET " + FIELD_TABLENAME + "='" + tableName + "', " + 
          DbObject.FIELD_ID + "=(SELECT * FROM (SELECT MAX(" + DbObject.FIELD_ID + ")+1 FROM " + 
          TABLENAME + ") AS x), " + DbObject.FIELD_SERIAL + "=" + 
          (isTableSerialValid ? ("(SELECT COALESCE(MAX(" + DbObject.FIELD_TABLESERIAL + "),0) FROM " + tableName + ")") : "1" ) 
        :
          // ordinary dbms 
          "INSERT INTO " + TABLENAME + " (" + FIELD_TABLENAME + "," + DbObject.FIELD_ID + "," + DbObject.FIELD_SERIAL +
          ") VALUES ('" + 
          tableName + 
          "',(SELECT MAX(" + DbObject.FIELD_ID + ")+1 FROM " + TABLENAME + ")," + 
          (isTableSerialValid ? ("(SELECT " + (db.isInformix() ? "NVL" : "COALESCE") + 
          "(MAX(" + DbObject.FIELD_TABLESERIAL + "),0) FROM " + tableName + ")") : "1" ) + 
          ")") != 1)  {
      DbGlobal.errorHandler.warning(db, null, Locales.bundle.getString("Mod-Table_Fehler"));
    }
    st.close();
  }
  
  
  
  /**
   * Get the current modification count by tablename.
   * @return the modification count
   */
  public long selectModification () {
    db.assertNotRemote();
    if (selectModificationStatementId == 0)  {
      selectModificationStatementId = db.prepareStatement(
        "SELECT " + DbObject.FIELD_SERIAL + " FROM " + TABLENAME + " WHERE " + FIELD_TABLENAME + "=?");
    }
    PreparedStatementWrapper st = db.getPreparedStatement(selectModificationStatementId);
    st.setString(1, tableName);
    ResultSetWrapper rs = st.executeQuery();
    long counter = 0;
    if (rs.next())  {
      counter = rs.getLong(DbObject.FIELD_SERIAL);
    }
    rs.close();
    return counter;
  }
  
}
