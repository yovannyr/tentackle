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

// $Id: DbObjectRemoteDelegateImpl.java 439 2008-09-18 15:09:32Z harald $

package org.tentackle.db.rmi;

import java.lang.reflect.Modifier;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.RemoteException;
import java.util.List;
import org.tentackle.db.DbCursor;
import org.tentackle.db.DbObject;


/**
 * Base class for the remote delegate of DbObject.<br>
 * All other subclasses of DbObject should extend DbObjectRemoteDelegateImpl to reflect
 * the class hierarchy below DbObject.
 *
 * @param <T> the database object class
 * @author harald
 */
public abstract class DbObjectRemoteDelegateImpl<T extends DbObject> 
                      extends RemoteDelegateImpl<T>
                      implements DbObjectRemoteDelegate {
  
  
  protected T dbObject;    // object associated to current db (used in generated methods)
  
  
  /**
   * Creates delegate.
   *
   * @param session is the RMI session
   * @param clazz is the class the delegate provides service for
   * @param port the port number on which the remote object receives calls,
   *        0 = system default
   * @param csf the client-side socket factory for making calls to the
   *        remote object, null = system default
   * @param ssf the server-side socket factory for receiving remote calls, 
   *        null = system default
   * @throws RemoteException 
   */
  public DbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz,
                                    int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
    super(session, clazz, port, csf, ssf);
    setup();
  }
  
  
  /**
   * Creates a delegate on the session socket.
   *
   * @param session is the RMI session
   * @param clazz is the subclass of DbObject
   * @throws RemoteException 
   */
  public DbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
    setup();
  }
  
  
  /**
   * Creates a delegate on a non-default session socket.
   * 
   * Example:
     <pre>
      public FirmaRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
        super(session, clazz, UserInfo.getSocketConfig(new Firma().getDbObjectClassVariables().properties));
      }
     </pre>
   *
   * @param session is the RMI session
   * @param clazz is the subclass of DbObject
   * @param socketConfig is of RemoteDbSessionImpl.SOCKETCONFIG_...
   * @throws RemoteException 
   */
  public DbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz, int socketConfig) throws RemoteException {
    super(session, clazz, socketConfig);
    setup();
  }
  
  
  
  
  private void setup() throws RemoteException {
    /**
     * instantiate a singe object and connect to db. (if class is not abstract).
     * This object is used for all methods not returning a new object and thus
     * minimizes object construction.
     */
    if (Modifier.isAbstract(clazz.getModifiers()) == false) {
      dbObject = newObject();
    }    
  }
  
  
  
  /**
   * Creates a new instance of the required class
   * for methods that return a new object.
   * @return the new object
   * @throws RemoteException 
   */
  protected T newObject() throws RemoteException {
    try {
      return DbObject.newByClass(db, clazz);
    } 
    catch (Exception e) {
      throw new RemoteException("creating object for class '" + clazz + "' failed", e); 
    }
  }
  
  
  public DbObject select(long id, boolean withLinkedObjects) throws RemoteException {
    try {
      return newObject().select(id, withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("select failed", e); 
    }
  }
  
  public DbObject selectLocked(long id, boolean withLinkedObjects) throws RemoteException {
    try {
      return newObject().selectLocked(id, withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("selectLocked failed", e); 
    }    
  }
  
  public DbObject selectInValidContext(long id, boolean withLinkedObjects) throws RemoteException {
    try {
      return DbObject.newByClass(db, clazz).selectInValidContext(id, withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("selectInValidContext failed", e); 
    }
  }
  
  public List<? extends DbObject> selectAll(boolean withLinkedObjects) throws RemoteException {
    try {
      return dbObject.selectAll(withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("selectAll failed", e); 
    }    
  }
  
  public long selectSerial(long id) throws RemoteException {
    try {
      return dbObject.selectSerial(id);
    }
    catch (Exception e) {
      throw new RemoteException("selectSerial failed", e); 
    }        
  }
  
  public boolean updatePlain(DbObject obj) throws RemoteException {
    try {
      obj.setDb(db);
      return obj.updatePlain();
    }
    catch (Exception e) {
      throw new RemoteException("updatePlain failed", e); 
    }        
  }
  
  public boolean dummyUpdate(DbObject obj) throws RemoteException {
    try {
      obj.setDb(db);
      return obj.dummyUpdate();
    }
    catch (Exception e) {
      throw new RemoteException("dummyUpdate failed", e); 
    }        
  }
  
  public boolean updateSerial(long id, long serial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      return dbObject.updateSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateSerial failed", e); 
    }        
  }  
  
  public boolean updateTableSerial(long id) throws RemoteException {
    try {
      dbObject.setId(id);
      return dbObject.updateTableSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateTableSerial failed", e); 
    }        
  }
  
  public boolean updateSerialAndTableSerial(long id, long serial, long tableSerial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      dbObject.setTableSerial(tableSerial);
      return dbObject.updateSerialAndTableSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateSerialAndTableSerial failed", e); 
    }        
  }
  
  public long[] selectExpiredTableSerials(long oldSerial) throws RemoteException {
    try {
      return dbObject.selectExpiredTableSerials(oldSerial);
    }
    catch (Exception e) {
      throw new RemoteException("selectExpiredTableSerials failed", e); 
    }            
  }
  
  public long[] selectExpiredTableSerials(long oldSerial, long maxSerial) throws RemoteException {
    try {
      return dbObject.selectExpiredTableSerials(oldSerial, maxSerial);
    }
    catch (Exception e) {
      throw new RemoteException("selectExpiredTableSerials failed", e); 
    }            
  }
  
  public long[] getExpirationBacklog(long minSerial, long maxSerial) throws RemoteException {
    try {
      return dbObject.getExpirationBacklog(minSerial, maxSerial);
    }
    catch (Exception e) {
      throw new RemoteException("getExpirationBacklog failed", e); 
    }         
  }
  
  public long[] getExpiredTableSerials(long oldSerial, long maxSerial) throws RemoteException {
    try {
      return dbObject.getExpiredTableSerials(oldSerial, maxSerial);
    }
    catch (Exception e) {
      throw new RemoteException("getExpiredTableSerials failed", e); 
    }           
  }
  
  public boolean deletePlain(long id, long serial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      return dbObject.deletePlain();
    }
    catch (Exception e) {
      throw new RemoteException("deletePlain failed", e); 
    }            
  }

  public boolean insertPlain(DbObject obj) throws RemoteException {
    try {
      obj.setDb(db);
      return obj.insertPlain();
    }
    catch (Exception e) {
      throw new RemoteException("insertPlain failed", e); 
    }        
  }
  
  public DbObjectResult updateObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = obj.updateObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("updateObject failed", e); 
    }      
  }
  
  public DbObjectResult insertObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = obj.insertObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("insertObject failed"); 
    }      
  }
  
  public DbObjectResult save(DbObject obj) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = obj.save();
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("save failed", e); 
    }      
  }
  
  public DbObjectResult deleteObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = obj.deleteObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("deleteObject failed", e); 
    }      
  }

  public long selectModification() throws RemoteException {
    try {
      return dbObject.selectModification();
    }
    catch (Exception e) {
      throw new RemoteException("selectModification failed", e); 
    }     
  }
  
  public RemoteDbCursor selectAllCursor(boolean withLinkedObjects) throws RemoteException {
    try {
      return new RemoteDbCursorImpl(this, (DbCursor<? extends DbObject>)dbObject.selectAllCursor(withLinkedObjects));
    }
    catch (Exception e) {
      throw new RemoteException("selectAllCursor failed", e); 
    }
  }
  
  public boolean isReferenced(long id) throws RemoteException {
    try {
      // object must exist because method is only invoked if !isNew()
      return newObject().select(id).isReferenced();
    }
    catch (Exception e) {
      throw new RemoteException("isReferenced failed", e); 
    }    
  }
  
}
