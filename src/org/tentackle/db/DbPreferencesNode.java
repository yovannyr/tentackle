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

// $Id: DbPreferencesNode.java 477 2009-08-09 18:54:26Z harald $


package org.tentackle.db;

import java.util.Set;
import java.util.TreeSet;
import javax.swing.ImageIcon;
import org.tentackle.db.rmi.DbPreferencesNodeRemoteDelegate;
import java.util.ArrayList;
import java.util.List;
import org.tentackle.plaf.PlafGlobal;



// @> $mapfile
// # a preferences node
// #
// # CREATE INDEX prefnode_parentid on prefnode (parentid);
// # CREATE UNIQUE INDEX prefnode_nodename on prefnode (nodename,username);
// # CREATE INDEX prefnode_tableserial on prefnode (tableserial);
//
// String  32     user            username        name of user, null = system [MAPNULL]
// String  128    name            nodename        name of the node
// long    0      parentId        parentid        ID of parent node, 0 = rootnode
// long    0      tableSerial     tableserial     tableserial [NOMETHOD,NODECLARE]
// @<




/**
 * Preferences Node stored in the database.
 *
 * @author harald
 */
public class DbPreferencesNode extends DbObject {

  private static final long serialVersionUID = 4677525106428955014L;
  
  /** database tablename **/
  public static final String TABLENAME = "prefnode";
  
  
  private static DbObjectClassVariables classVariables = 
    new DbObjectClassVariables(DbPreferencesNode.class, TABLENAME, "Preferences Node", "Preferences Nodes");

  
  
  static {
    ModificationThread.getThread().registerTable(newByClassWrapped(DbPreferencesNode.class).getTableName(), new ModificationThread.SerialRunnable() {
      public void run(Db db, long serial) {
        if (DbPreferences.isAutoSync()) {
          DbPreferences.expireNodes(db, serial);
        }
      }
    });
  }
  
  
  
  

  /**
   * Creates a node.
   * 
   * @param db the db connection
   */
  public DbPreferencesNode (Db db)    {
    super(db);
  }
  
  /**
   * Creates a node (without db).
   */
  public DbPreferencesNode() {
    super();
  }
  

  
  @Override
  public boolean isCountingModification(int modType) {
    return true;
  }
  
  
  @Override
  public boolean isTableSerialValid() {
    return true;
  }
  
  
  @Override
  public ImageIcon getIcon() {
    return PlafGlobal.getIcon("preferences");
  }

  
  @Override
  public DbObjectClassVariables getDbObjectClassVariables() {
    return classVariables;
  }  


  @Override
  public boolean getFields(ResultSetWrapper rs)  {
    // @wurblet getFields DbGetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:getFields

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
        COLUMN_USER = rs.findColumn(FIELD_USER);
        updateFieldCount(COLUMN_USER);
        COLUMN_NAME = rs.findColumn(FIELD_NAME);
        updateFieldCount(COLUMN_NAME);
        COLUMN_PARENTID = rs.findColumn(FIELD_PARENTID);
        updateFieldCount(COLUMN_PARENTID);
        COLUMN_TABLESERIAL = rs.findColumn(FIELD_TABLESERIAL);
        updateFieldCount(COLUMN_TABLESERIAL);
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

    user = rs.getString(COLUMN_USER, true);
    name = rs.getString(COLUMN_NAME);
    parentId = rs.getLong(COLUMN_PARENTID);
    setTableSerial(rs.getLong(COLUMN_TABLESERIAL));
    setId(rs.getLong(COLUMN_ID));
    setSerial(rs.getLong(COLUMN_SERIAL));

    // End of wurblet generated code.//GEN-END:getFields
    return true;
  }


  @Override
  public int setFields(PreparedStatementWrapper st)	{
    // @wurblet setFields DbSetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:setFields

    int ndx = 0;
    st.setString(++ndx, user, true); 
    st.setString(++ndx, name); 
    st.setLong(++ndx, parentId); 
    st.setLong(++ndx, getTableSerial()); 
    st.setLong(++ndx, getId());
    st.setLong(++ndx, getSerial());

    // End of wurblet generated code.//GEN-END:setFields
    return ndx;
  }



  /**
   * Selects a node by its ID.
   */
  @Override
  public DbPreferencesNode select (long id)  {
    return (DbPreferencesNode)super.select(id);
  }


  @Override
  public int prepareInsertStatement ()  {
    // @wurblet insert DbInsert $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:insert

    int stmtId = getInsertStatementId();
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "INSERT INTO " + getTableName()
            + " (" + FIELD_USER
            + ","  + FIELD_NAME
            + ","  + FIELD_PARENTID
            + ","  + FIELD_TABLESERIAL
            + ","  + FIELD_ID
            + ","  + FIELD_SERIAL + ") VALUES (" +
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
  public int prepareUpdateStatement () {
    // @wurblet update DbUpdate $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:update

    int stmtId = getUpdateStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      stmtId = getDb().prepareStatement(
            "UPDATE " + getTableName() + " SET "
            +       FIELD_USER + "=?"
            + "," + FIELD_NAME + "=?"
            + "," + FIELD_PARENTID + "=?"
            + "," + FIELD_TABLESERIAL + "=?"
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
   * Gets all nodes belonging to a parent node.
   * 
   * @param parentId the ID of the parent node, 0 = rootnode
   * @return List of nodes
   *
   * @wurblet selectByParentId DbSelectList $mapfile $remote parentId
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectByParentId

  public List<DbPreferencesNode> selectByParentId(long parentId) {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        List<DbPreferencesNode> list = ((DbPreferencesNodeRemoteDelegate)getRemoteDelegate()).selectByParentId(parentId);
        Db.applyToCollection(getDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByParentId failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectByParentIdStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(getSqlSelectAllFields() 
                    + " AND " + FIELD_PARENTID + "=?");
      selectByParentIdStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setLong(ndx++, parentId);
    ResultSetWrapper rs = st.executeQuery();
    List<DbPreferencesNode> list = new ArrayList<DbPreferencesNode>();
    boolean derived = getClass() != DbPreferencesNode.class;
    while (rs.next()) {
      DbPreferencesNode obj = derived ? (DbPreferencesNode)newObject() : new DbPreferencesNode(getDb());
      if (obj.readFromResultSetWrapper(rs))  {
        list.add(obj);
      }
    }
    rs.close();
    return list;
  }

  private static int selectByParentIdStatementId;


  // End of wurblet generated code.//GEN-END:selectByParentId

  
  
  
  /**
   * Selects the object IDs of all childs nodes.
   * 
   * @return the IDs
   */
  public Set<Long> selectChildIds() {
    if (getDb().isRemote())  {
      try {
        return ((DbPreferencesNodeRemoteDelegate)getRemoteDelegate()).selectChildIds(getId());
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectChildIds failed");
        return null;
      }
    }
    else  {
      if (selectChildIdsStatementId == 0)  {
          // prepare it */
          selectChildIdsStatementId = getDb().prepareStatement(
                  "SELECT " + FIELD_ID + " FROM " + TABLENAME + 
                  " WHERE " + FIELD_PARENTID + "=?");
      }
      PreparedStatementWrapper st = getDb().getPreparedStatement(selectChildIdsStatementId);
      st.setLong(1, getId());
      ResultSetWrapper rs = st.executeQuery();
      Set<Long> set = new TreeSet<Long>();
      while (rs.next()) {
        set.add(rs.getALong(1));
      }
      rs.close();
      return set;
    }
  }

  private static int selectChildIdsStatementId;
  
  
  
  /**
   * Selects a node by user and name.
   * 
   * @param user is the username, null = system
   * @param name is the absolute pathname of the node, ""=root
   * @return the node
   * 
   * @wurblet selectByUserAndName DbSelectUnique $mapfile $remote user name
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectByUserAndName

  public DbPreferencesNode selectByUserAndName(String user, String name) {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        DbPreferencesNode obj = ((DbPreferencesNodeRemoteDelegate)getRemoteDelegate()).selectByUserAndName(user, name);
        Db.applyToDbObject(getDb(), obj);
        return obj;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByUserAndName failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectByUserAndNameStatementId;
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      stmtId = getDb().prepareStatement(getSqlSelectAllFields() 
                    + " AND " + FIELD_USER + "=?"
                    + " AND " + FIELD_NAME + "=?");
      selectByUserAndNameStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setString(ndx++, user, true);
    st.setString(ndx++, name);
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

  private static int selectByUserAndNameStatementId;


  // End of wurblet generated code.//GEN-END:selectByUserAndName



  



  // generated GETTER/SETTER
  
  // @wurblet methods DbMethods $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:methods


  /**
   * Gets the db attribute user
   *
   * @return name of user, null = system
   */
  public String getUser()    {
    return user;
  }

  /**
   * Sets the db attribute user
   *
   * @param user name of user, null = system
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Gets the db attribute name
   *
   * @return name of the node
   */
  public String getName()    {
    return name;
  }

  /**
   * Sets the db attribute name
   *
   * @param name name of the node
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the db attribute parentId
   *
   * @return ID of parent node, 0 = rootnode
   */
  public long getParentId()    {
    return parentId;
  }

  /**
   * Sets the db attribute parentId
   *
   * @param parentId ID of parent node, 0 = rootnode
   */
  public void setParentId(long parentId) {
    this.parentId = parentId;
  }

  /**
   * no accessor methods for tableSerial
   * tableserial
   */

  // End of wurblet generated code.//GEN-END:methods

  

  /**
   * Gets the basename of the node.
   * 
   * @return the node's basename
   */
  public String getBaseName() {
    int lastSlash = name.lastIndexOf('/');
    return lastSlash >= 0 ? name.substring(lastSlash + 1) : name; 
  }

  
  @Override
  public String toString()  {
    return "ID=" + getId() + ", parentId=" + parentId +
           (user == null ? ", <system>" : (", user='" + user + "'")) + 
           ", name='" + name + "'";
  }


  

  // record members
  // @wurblet declare DbDeclare $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:declare


  /** name of user, null = system **/
  private String user;

  /** name of the node **/
  private String name;

  /** ID of parent node, 0 = rootnode **/
  private long parentId;

  // End of wurblet generated code.//GEN-END:declare
  


  // @wurblet fieldNames DbFieldNames $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:fieldNames

  private static boolean columnsValid;    // true if COLUMN_.... are valid for getFields()
  /** database column name for user **/
  public  static final String FIELD_USER = "username";
  private static       int    COLUMN_USER;
  /** database column name for name **/
  public  static final String FIELD_NAME = "nodename";
  private static       int    COLUMN_NAME;
  /** database column name for parentId **/
  public  static final String FIELD_PARENTID = "parentid";
  private static       int    COLUMN_PARENTID;
  private static       int    COLUMN_TABLESERIAL;
  private static       int    COLUMN_ID;
  private static       int    COLUMN_SERIAL;

  // End of wurblet generated code.//GEN-END:fieldNames

}

