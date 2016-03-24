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

// $Id: DbObjectClassVariables.java 470 2009-07-31 07:14:05Z svn $


package org.tentackle.db;

import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import org.tentackle.db.rmi.DbObjectRemoteDelegate;
import org.tentackle.util.StringHelper;

/**
 * Holds static class variables for classes derived from DbObject.
 * This is a "singleton per class".
 * All classvariables register to a static {@link Map} that can be queried by apps
 * by tablename.
 *
 * @author harald
 */
public class DbObjectClassVariables {
  
  
  // all classes register here:
  private static Map<String,DbObjectClassVariables> clazzMap = 
          new TreeMap<String,DbObjectClassVariables>();
  
  
  
  
  /**
   * the class
   */
  public Class<? extends DbObject> clazz;
  
  /**
   * the full classname
   */
  public String className;
  
  /**
   * the base-classname
   */
  public String classBaseName;
  
  /**
   * class properties
   */
  public Properties properties;
  
  /**
   * database table name
   */
  public String tableName;
  
  /**
   * name for a single object
   */
  public String singleName;
  
  /**
   * name for multiple objects
   */
  public String multiName;
  
  /**
   * number of db-columns, 0 = not known so far
   */
  public int fieldCount;
  
  /**
   * ID for the remote delegate for this class.
   */
  public int remoteDelegateId;
  
  /**
   * true if prepared statements should always be prepared (i.e. if
   * the statement is changing for some reasons, e.g. the tablename).
   */
  public boolean alwaysPrepare;
  
  /**
   * prepared statement ID for select()
   */
  public int selectStatementId;
  
  /**
   * prepared statement ID for selectAll()
   */  
  public int selectAllStatementId;
  
  /**
   * prepared statement ID for selectLocked()
   */
  public int selectLockedStatementId;
  
  /**
   * prepared statement ID for selectSerial()
   */
  public int selectSerialStatementId;
  
  /**
   * prepared statement ID for insert()
   */
  public int insertStatementId;
  
  /**
   * prepared statement ID for update()
   */
  public int updateStatementId;
  
  /**
   * prepared statement ID for delete()
   */
  public int deleteStatementId;
  
  /**
   * prepared statement ID for dummyUpdate()
   */
  public int dummyUpdateStatementId;
  
  /**
   * prepared statement ID for updateSerial()
   */
  public int updateSerialStatementId;
  
  /**
   * prepared statement ID for updateTableSerial()
   */
  public int updateTableSerialStatementId;
  
  /**
   * prepared statement ID for updateSerialAndTableSerial()
   */
  public int updateSerialAndTableSerialStatementId;
  
  /**
   * prepared statement ID for selectExpiredTableSerials()
   */
  public int selectExpiredTableSerials1StatementId;
  public int selectExpiredTableSerials2StatementId;
  
  /**
   * table serial expiration backlog
   */
  public TableSerialExpirationBacklog expirationBacklog;
  
  /**
   * the source for obtaining a new ID
   */
  public int idSourceId;
  
  
  /**
   * General purpose instance (or ID) counter.
   * Use {@link #countInstance} to get a new unique number.
   */
  public long instanceCount;
  
  /**
   * Gets a unique instance number for pseudo-objects
   * (e.g. {@link org.tentackle.appworx.AppDbPseudoObject}
   * to simulate a unique ID.
   * Or for other purposes.
   * 
   * @return the instance number
   */
  public synchronized long countInstance() {
    return ++instanceCount;
  }
  
  
  
  /**
   * Gets the delegateId of the class, i.e. subclass of DbObject.
   * If the remoteDelegateId is 0, it will be prepared
   * The delegateId is unique for each class. It is valid only in remote connections and
   * is the same for all remote Db's.
   * The RMI-server creates a delegate for each subclass of DbObject (DbObjectRemoteDelegateImpl resp.)
   *
   * @return the delegate id
   */
  public int getRemoteDelegateId() {
    if (remoteDelegateId == 0)  {
      remoteDelegateId = Db.prepareRemoteDelegate(clazz);
    }
    return remoteDelegateId;
  }
  
  /**
   * Gets the RemoteDelegate for the class and db.
   * @param db the db connection
   * @return the delegate
   */
  public DbObjectRemoteDelegate getRemoteDelegate(Db db) {
    return (DbObjectRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId());
  }
  
  
  /**
   * Gets a class property.
   * 
   * @param key the property key
   * @return the property value
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }
  
  
  /**
   * constructs a classvariable.
   * Throws IllegalStateException if already constructed.
   *
   * @param clazz is the class of the derived DbObject
   * @param tableName is the SQL tablename
   * @param singleName text for a single object
   * @param multiName text for multiple objects
   */
  public DbObjectClassVariables(Class<? extends DbObject> clazz, String tableName, String singleName, String multiName)  {
    
    this.clazz      = clazz;
    this.tableName  = tableName;
    this.singleName = singleName;
    this.multiName  = multiName;
    
    className       = clazz.getName();
    classBaseName   = StringHelper.getClassBaseName(clazz);
    
    DbObjectClassVariables classVar = clazzMap.put(tableName, this);
    if (classVar != null) {
      /**
       * Replacing the class with an extended class is okay.
       * This is because the class hierarchy will register along the extension path.
       * In all other cases the tablename must be unique.
       */
      if (classVar.clazz.isAssignableFrom(clazz) == false) {
        throw new IllegalStateException(
                "classvariables for '" + tableName + "' already registered, old=" + classVar.className +
                ", new=" + className);
      }
    }
    
    // load properties, if any from file
    // Resourcebundle isn't serializable, but Properties are.
    properties = new Properties();
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(className);
      for (String key: bundle.keySet()) {
        properties.setProperty(key, bundle.getString(key));
      }
    }
    catch (Exception e) {
      // missing props is okay
    }
    
    // create the backlog (not knowing whether it will ever be used)
    expirationBacklog = new TableSerialExpirationBacklog();
  }
  
  
  @Override
  public String toString() {
    return className + "/" + tableName;
  }
  
  
  /**
   * Gets the classvariables for a given tablename.
   *
   * @param tableName is the database tablename
   * @return the classvariables or null if no such tablename
   */
  public static DbObjectClassVariables getVariables(String tableName) {
    return clazzMap.get(tableName);
  }

}
