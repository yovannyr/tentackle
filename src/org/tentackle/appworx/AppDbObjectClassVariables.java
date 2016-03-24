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

// $Id: AppDbObjectClassVariables.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.appworx;

import java.util.Collection;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbObjectClassVariables;
import org.tentackle.db.DbRuntimeException;
import org.tentackle.util.ApplicationException;



/**
 * Extends {@link DbObjectClassVariables} for {@link AppDbObject}s.
 *
 * @author harald
 */
public class AppDbObjectClassVariables extends DbObjectClassVariables {
  
  
  /**
   * constructs a classvariable
   *
   * @param clazz is the class of the derived DbObject
   * @param tableName is the SQL tablename
   * @param singleName text for a single object
   * @param multiName text for multiple objects
   * @param checkSecurity is true to check the READ/WRITE-Security on each access.
   */
  public AppDbObjectClassVariables(Class<? extends AppDbObject> clazz, String tableName, 
                                   String singleName, String multiName, boolean checkSecurity)  {
    super(clazz, tableName, singleName, multiName);
    this.checkSecurity = checkSecurity;
  }
  
  
  /**
   * Constructs a classvariable.
   * By default, the security check is turned on for servers.
   * It can be overwridden by the property "checksecurity=on|off|true|false|enabled|disabled|0|1"
   * in the class property file. 
   *
   * @param clazz is the class of the derived DbObject
   * @param tableName is the SQL tablename
   * @param singleName text for a single object
   * @param multiName text for multiple objects
   */
  public AppDbObjectClassVariables(Class<? extends AppDbObject> clazz, String tableName, 
                                   String singleName, String multiName)  {
    super(clazz, tableName, singleName, multiName);
    
    String prop = getProperty("checksecurity");
    if (prop != null) {
      checkSecurity = !(prop.equals("no") || prop.equals("off") || prop.equals("false") ||
                        prop.equals("disabled") || prop.equals("0"));
    }
    else  {
      checkSecurity = DbGlobal.isServer();
    }
  }
  

  
  /**
   * Check the read security for this class.
   */
  private boolean checkClassPermission(ContextDb contextDb, int permission) {
    try {
      return !checkSecurity || 
             (contextDb.getAppUserInfo().getSecurityManager().privilege(clazz, contextDb, permission).isAccepted() &&
              !contextDb.getAppUserInfo().getSecurityManager().privilege(clazz, contextDb, -1, permission).isDenied());
    }
    catch (Exception ex) {
      // either context not set or security manager or whatever...
      return false;
    }    
  }
          
          
  /**
   * Check the read security for this class.
   * The implementation checks that the class rules will accept
   * and that no object rule denies.
   * <p>
   * Notice that {@link Security} objects are always readable!
   *
   * @param contextDb the current context, null = all
   * @return false if operation (select) is denied.
   */
  public boolean isReadAllowed(ContextDb contextDb) {
    return clazz.isAssignableFrom(Security.class) ||
            checkClassPermission(contextDb, Security.READ);
  }
  
  /**
   * Check the read security for this class in all contexts.
   *
   * @return false if operation (select) is denied.
   */
  public boolean isReadAllowed() {
    return clazz.isAssignableFrom(Security.class) ||
            checkClassPermission(null, Security.READ);
  }
  

  
  /**
   * Check the write security for this class.
   * The implementation checks that the class rules will accept
   * and that no object rule denies.
   *
   * @param contextDb the current context, null = all
   * @return false if operation (delete, update or insert) is denied.
   */
  public boolean isWriteAllowed(ContextDb contextDb) {
    return checkClassPermission(contextDb, Security.WRITE);
  }
  
  /**
   * Check the write security for this class in all contexts.
   * 
   * @return false if operation (delete, update or insert) is denied.
   */
  public boolean isWriteAllowed() {
    return checkClassPermission(null, Security.WRITE);
  }
  
  
  /**
   * Check the read security for a single object.
   * <p>
   * Notice that {@link Security} objects are always readable!
   *
   * @param object the object to check the security rules for.
   *
   * @return false if operation (select) is denied.
   */
  public boolean isReadAllowed(AppDbObject object) {
    return !checkSecurity ||
           object instanceof Security ||
           object.isPermissionAccepted(Security.READ);
  }
  
  /**
   * Check the write security for a single object.
   *
   * @param object the object to check the security rules for.
   *
   * @return false if operation (delete, update or insert) is denied.
   */
  public boolean isWriteAllowed(AppDbObject object) {
    return !checkSecurity || object.isPermissionAccepted(Security.WRITE);
  }
  
  
  /**
   * Checks the read security for a collection of objects of this class.<br>
   * The returned collection is of the same type as the original collection.
   * Notice that this method is provided for applications that circumvent
   * readFromResultsetWrapper() somehow (which checks the read permission).
   * Tentackle is not using this method.
   * <p>
   * Notice that {@link Security} objects are always readable!
   *
   * @param <T> AppDbObject class
   * @param <C> Collection class
   * @param objects the collection to check the security rules for.
   *
   * @return the objects with permission accepted.
   */
  @SuppressWarnings("unchecked")
  public <T extends AppDbObject, C extends Collection<T>> C isReadAllowed(C objects) {
    if (checkSecurity && objects != null && !clazz.isAssignableFrom(Security.class))  {
      try {
        C newObjects = (C)objects.getClass().newInstance();
        for (T object: objects) {
          if (object.isPermissionAccepted(Security.READ)) {
            newObjects.add(object);
          }
        }
        objects = newObjects;
      } 
      catch (Exception ex) {
        throw new DbRuntimeException("can't instantiate collection for checking read security rules", ex);
      }
    }
    return objects;
  }
  
  
  /**
   * Checks the write security for a collection of objects of this class.<br>
   * The returned collection is of the same type as the original collection.
   * Notice that this method is provided for applications that circumvent
   * deleteObject, updateObject or insertObject somehow (all of them check the write permission).
   * Tentackle is not using this method.
   * Furthermore, special care must be taken for "deleteBy..." methods that
   * delete objects by an sql-statement via "DELETE ...".
   *
   * @param <T> AppDbObject class
   * @param <C> Collection class
   * @param objects the collection to check the security rules for.
   *
   * @return the objects with permission accepted.
   */
  @SuppressWarnings("unchecked")
  public <T extends AppDbObject, C extends Collection<T>> C isWriteAllowed(C objects) {
    if (checkSecurity && objects != null)  {
      try {
        C newObjects = (C)objects.getClass().newInstance();
        for (T object: objects) {
          if (object.isPermissionAccepted(Security.WRITE)) {
            newObjects.add(object);
          }
        }
        objects = newObjects;
      } 
      catch (Exception ex) {
        throw new DbRuntimeException("can't instantiate collection for checking write security rules", ex);
      }
    }
    return objects;
  }
  
  
  
  
  /**
   * Flag is true to check the security rules for each access to objects of this class.
   * The rules will be checked in local db access only (i.e. the check will be applied
   * already at the RMI-side *before* the data is leaving the client!)
   * Because this check can be somewhat time consuming, it's turned off by default.
   * The flag cannot be changed at runtime anymore!
   */
  public final boolean checkSecurity;
  
  
  
  
  /**
   * prepared statement ID for {@link AppDbObject#selectByNormText}
   */
  public int normTextStatementId;
  
  /**
   * prepared statement ID for {@link AppDbObject#selectAllInContext}
   */  
  public int allInContextStatementId;
  
  /**
   * prepared statement ID for {@link AppDbObject#selectByNormTextCursor}
   */
  public int normTextCursorStatementId;
  
  /**
   * prepared statement ID for {@link AppDbObject#selectAllInContextCursor}
   */  
  public int allInContextCursorStatementId;
  
  /**
   * prepared statement ID for {@link AppDbObject#updateEditedBy}
   */
  public int updateEditedByStatementId;

  /**
   * prepared statement ID for {@link AppDbObject#updateEditedByOnly}
   */
  public int updateEditedByOnlyStatementId;
  
  /**
   * prepared statement ID for select in {@link AppDbObject#updateEditedBy}
   */
  public int selectEditedByStatementId;
  
  /**
   * prepared statement ID for {@link AppDbObject#transferEditedBy}
   */
  public int transferEditedByStatementId;
  
}
