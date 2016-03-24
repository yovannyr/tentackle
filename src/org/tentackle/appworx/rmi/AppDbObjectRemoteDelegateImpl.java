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

// $Id: AppDbObjectRemoteDelegateImpl.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.appworx.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.sql.Timestamp;
import java.util.List;
import org.tentackle.appworx.AppDbObject;
import org.tentackle.appworx.ContextDb;
import org.tentackle.appworx.QbfParameter;
import org.tentackle.db.DbCursor;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbObject;
import org.tentackle.db.rmi.DbObjectRemoteDelegateImpl;
import org.tentackle.db.rmi.DbObjectResult;
import org.tentackle.db.rmi.RemoteDbCursor;
import org.tentackle.db.rmi.RemoteDbCursorImpl;
import org.tentackle.db.rmi.RemoteDbSessionImpl;



/**
 * Implementation of the remote delegate for {@link AppDbObject}.<br>
 * Never instantiated.
 *
 * @param <T> the {@code AppDbObject} class
 * @author harald
 */
public abstract class AppDbObjectRemoteDelegateImpl<T extends AppDbObject>
                      extends DbObjectRemoteDelegateImpl<T>
                      implements AppDbObjectRemoteDelegate {
  
  
  /**
   * Creates delegate.
   *
   * @param session the RMI session
   * @param clazz the class the delegate provides service for
   * @param port the port number on which the remote object receives calls,
   *        0 = system default
   * @param csf the client-side socket factory for making calls to the
   *        remote object, null = system default
   * @param ssf the server-side socket factory for receiving remote calls, 
   *        null = system default
   * @throws java.rmi.RemoteException 
   */
  public AppDbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz,
                                       int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
    super(session, clazz, port, csf, ssf);
    setup();
  }
  
  
  /**
   * Creates a delegate on the session socket.
   *
   * @param session the RMI session
   * @param clazz the subclass of DbObject
   * @throws java.rmi.RemoteException 
   */
  public AppDbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
    setup();
  }
  
  
  /**
   * Create a delegate on a non-default session socket.
   *
   * @param session is the RMI session
   * @param clazz the subclass of DbObject
   * @param socketConfig is of RemoteDbSessionImpl.SOCKETCONFIG_...
   * @throws java.rmi.RemoteException 
   */
  public AppDbObjectRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz, int socketConfig) throws RemoteException {
    super(session, clazz, socketConfig);
    setup();
  }
  
  
  
  private void setup() {
    /**
     * Try to set a default contextDb.
     * This will fail in apps that extend the contextdb, but is useful for other apps.
     * We ignore any thrown exception safely.
     */
    try {
      T appDbObject = newObject();
      appDbObject.setContextDb(new ContextDb(db));
      dbObject = appDbObject;
    }
    catch (Exception e) {
      /**
       * Application requires extended contextdb.
       * Leave the dbObject as it is for simple methods.
       * In all other cases, the application must pass the contextDb via the remote method
       * and set the context explicitly.
       */
    }    
  }
  
  
  
  /**
   * Sets the contextdb in the default object.
   * 
   * @param cb the db context
   */
  protected void setContextDb(ContextDb cb) {
    // set the db in the contextDb first as this will be copied to the object below
    cb.setDb(db);   // db in contextDb is transient, so we must set it here
    dbObject.setContextDb(cb);
  }
  
  
  
  /**
   * Sets the contextdb in the default object with the Db set to DbGlobal.serverDb.
   * 
   * @param cb the db context
   */
  protected void setServerContextDb(ContextDb cb) {
    // set the db in the contextDb first as this will be copied to the object below
    cb.setDb(DbGlobal.serverDb);   // db of the server 
    dbObject.setContextDb(cb);
  }
  
  
  
  /**
   * Creates a new instance of the required class in the given context
   * for methods that return a new object.
   * 
   * @param cb the db context
   * @return the created object
   * @throws java.rmi.RemoteException 
   */
  protected T newObject(ContextDb cb) throws RemoteException {
    try {
      cb.setDb(db);
      return AppDbObject.newByClass(cb, clazz);
    } 
    catch (Exception e) {
      throw new RemoteException("creating object for class '" + clazz + "' in failed", e); 
    }
  }
  
  
  // ----------- overridden due to security checks against malicious clients ----------------
  
  /**
   * Checks if write is allowed for overridden methods from DbObject.
   * This check is only necessary in case a malicious remote client tries
   * to circumvent the security checks and intentionally uses DbObject methods
   * for secured AppDbObjects.
   */
  private boolean isWriteAllowed(DbObject obj) {
    return ((AppDbObject)obj).getAppDbObjectClassVariables().isWriteAllowed((AppDbObject)obj);
  }
  
  @Override
  public boolean updatePlain(DbObject obj) throws RemoteException {
    return isWriteAllowed(obj) && super.updatePlain(obj);
  }
  
  @Override
  public boolean dummyUpdate(DbObject obj) throws RemoteException {
    return isWriteAllowed(obj) && super.dummyUpdate(obj);
  }
  
  @Override
  public boolean updateSerial(long id, long serial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      return isWriteAllowed(dbObject) && dbObject.updateSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateSerial failed", e); 
    }        
  }  
  
  @Override
  public boolean updateTableSerial(long id) throws RemoteException {
    try {
      dbObject.setId(id);
      return isWriteAllowed(dbObject) && dbObject.updateTableSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateTableSerial failed", e); 
    }        
  }
  
  @Override
  public boolean updateSerialAndTableSerial(long id, long serial, long tableSerial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      dbObject.setTableSerial(tableSerial);
      return isWriteAllowed(dbObject) && dbObject.updateSerialAndTableSerial();
    }
    catch (Exception e) {
      throw new RemoteException("updateSerialAndTableSerial failed", e); 
    }        
  }
  
  @Override
  public boolean deletePlain(long id, long serial) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setSerial(serial);
      return isWriteAllowed(dbObject) && dbObject.deletePlain();
    }
    catch (Exception e) {
      throw new RemoteException("deletePlain failed", e); 
    }            
  }

  @Override
  public boolean insertPlain(DbObject obj) throws RemoteException {
    try {
      obj.setDb(db);
      return isWriteAllowed(obj) && obj.insertPlain();
    }
    catch (Exception e) {
      throw new RemoteException("insertPlain failed", e); 
    }        
  }
  
  @Override
  public DbObjectResult updateObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = isWriteAllowed(obj) && obj.updateObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("updateObject failed", e); 
    }      
  }
  
  @Override
  public DbObjectResult insertObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = isWriteAllowed(obj) && obj.insertObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("insertObject failed"); 
    }      
  }
  
  @Override
  public DbObjectResult deleteObject(DbObject obj, boolean withLinkedObjects) throws RemoteException {
    try {
      obj.setDb(db);
      boolean ok = isWriteAllowed(obj) && obj.deleteObject(withLinkedObjects);
      return new DbObjectResult(obj.getId(), obj.getSerial(), obj.getTableSerial(), ok, db.isUniqueViolation());
    }
    catch (Exception e) {
      throw new RemoteException("deleteObject failed", e); 
    }      
  }
  
  // --------------- end malicious client checks ----------------
  
  
  
  
  @Override
  public List<? extends AppDbObject> selectAll(boolean withLinkedObjects) throws RemoteException {
    try {
      return dbObject.selectAll(withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("selectAll failed", e); 
    }    
  }
  
  
  public AppDbObject select(ContextDb cb, long id, boolean withLinkedObjects) throws RemoteException {
    try {
      return newObject(cb).select(id, withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("select failed", e); 
    }
  }
  
  public AppDbObject selectLocked(ContextDb cb, long id, boolean withLinkedObjects) throws RemoteException {
    try {
      return newObject(cb).selectLocked(id, withLinkedObjects);
    }
    catch (Exception e) {
      throw new RemoteException("selectLocked failed", e); 
    }    
  }

  public List<? extends AppDbObject> selectByNormText(ContextDb cb, String normText) throws RemoteException {
    try {
      setContextDb(cb);   // set the context
      return dbObject.selectByNormText(normText);
    }
    catch (Exception e) {
      throw new RemoteException("selectByNormText failed", e); 
    }
  }
  
  @SuppressWarnings("unchecked")  // see note in DbObjectRemoteDelegate
  public RemoteDbCursor selectByNormTextCursor(ContextDb cb, String normText) throws RemoteException {
    try {
      setContextDb(cb);   // set the context
      return new RemoteDbCursorImpl(this, (DbCursor<? extends DbObject>)dbObject.selectByNormTextCursor(normText));
    }
    catch (Exception e) {
      throw new RemoteException("selectByNormTextCursor failed", e); 
    }
  }
  
  public List<? extends AppDbObject> selectAllInContext(ContextDb cb) throws RemoteException {
    try {
      setContextDb(cb);  // set the context
      return dbObject.selectAllInContext();
    }
    catch (Exception e) {
      throw new RemoteException("selectAllInContext failed", e); 
    }
  }
  
  public List<? extends AppDbObject> selectAllInContextFromServerCache(ContextDb cb) throws RemoteException {
    try {
      if (DbGlobal.isServer()) {    
        // read from server cache
        setServerContextDb(cb);
        return dbObject.selectAllInContextCached();
      }
      else  {
        // read from storage
        setContextDb(cb);
        return dbObject.selectAllInContext();
      }
    }
    catch (Exception e) {
      throw new RemoteException("selectAllInContextFromServerCache failed", e); 
    }
  }
  
  @SuppressWarnings("unchecked")  // see note in DbObjectRemoteDelegate
  public RemoteDbCursor selectAllInContextCursor(ContextDb cb) throws RemoteException {
    try {
      setContextDb(cb);  // set the context
      return new RemoteDbCursorImpl(this, dbObject.selectAllInContextCursor());
    }
    catch (Exception e) {
      throw new RemoteException("selectAllInContextCursor failed", e); 
    }
  }
  
  @SuppressWarnings("unchecked")  // see note in DbObjectRemoteDelegate
  public QbfCursorResult searchQbfCursor(QbfParameter par) throws RemoteException {
    try {
      par.clazz = clazz;              // class in QbfParameter is transient
      setContextDb(par.contextDb);    // set the context
      return new QbfCursorResult(new RemoteDbCursorImpl(this, dbObject.searchQbfCursor(par)), par.estimatedRowCount);
    }
    catch (Exception e) {
      throw new RemoteException("searchQbfCursor failed", e); 
    }
  }
  
  public List<Object> getTreeParentObjects(AppDbObject obj) throws RemoteException  {
    try {
      obj.setDb(db);
      return obj.getTreeParentObjects();
    }
    catch (Exception e) {
      throw new RemoteException("getTreeParentObjects failed", e); 
    }    
  }
  
  public List<Object> getTreeParentObjects(AppDbObject obj, Object parentObject) throws RemoteException  {
    try {
      obj.setDb(db);
      if (parentObject instanceof AppDbObject) {
        ((AppDbObject)parentObject).setDb(db);
      }
      return obj.getTreeParentObjects(parentObject);
    }
    catch (Exception e) {
      throw new RemoteException("getTreeParentObjects failed", e); 
    }    
  }
  
  public List<Object> getTreeChildObjects(AppDbObject obj) throws RemoteException  {
    try {
      obj.setDb(db);
      return obj.getTreeChildObjects();
    }
    catch (Exception e) {
      throw new RemoteException("getTreeChildObjects failed", e); 
    }    
  }
  
  public List<Object> getTreeChildObjects(AppDbObject obj, Object parentObject) throws RemoteException  {
    try {
      obj.setDb(db);
      if (parentObject instanceof AppDbObject) {
        ((AppDbObject)parentObject).setDb(db);
      }
      return obj.getTreeChildObjects(parentObject);
    }
    catch (Exception e) {
      throw new RemoteException("getTreeChildObjects failed", e); 
    }    
  }
  
  public boolean isReferenced(ContextDb cb, long id) throws RemoteException {
    try {
      return newObject(cb).select(id).isReferenced();
    }
    catch (Exception e) {
      throw new RemoteException("isReferenced failed", e); 
    }
  }
  
  public AppDbObjectRemoteDelegate.BeingEditedToken updateEditedBy(long id, Timestamp tokenExpiry, long userId, Timestamp curTime) throws RemoteException  {
    try {
      AppDbObjectRemoteDelegate.BeingEditedToken token = new AppDbObjectRemoteDelegate.BeingEditedToken();
      dbObject.setId(id);
      token.success = dbObject.updateEditedBy(tokenExpiry, userId, curTime);
      token.editedBy = dbObject.getEditedBy();
      token.editedSince = dbObject.getEditedSince();
      token.editedExpiry = dbObject.getEditedExpiry();
      return token; 
    }
    catch (Exception e) {
      throw new RemoteException("updateBeingEditedToken failed", e); 
    }          
  }
  
  public AppDbObjectRemoteDelegate.BeingEditedToken transferEditedBy(long id) throws RemoteException  {
    try {
      AppDbObjectRemoteDelegate.BeingEditedToken token = new AppDbObjectRemoteDelegate.BeingEditedToken();
      dbObject.setId(id);
      token.success = dbObject.transferEditedBy(id);
      token.editedBy = dbObject.getEditedBy();
      token.editedSince = dbObject.getEditedSince();
      token.editedExpiry = dbObject.getEditedExpiry();
      return token; 
    }
    catch (Exception e) {
      throw new RemoteException("updateBeingEditedToken failed", e); 
    }          
  }

  public boolean updateEditedByOnly(long id, long editedBy, Timestamp editedSince, Timestamp editedExpiry) throws RemoteException {
    try {
      dbObject.setId(id);
      dbObject.setEditedBy(editedBy);
      dbObject.setEditedSince(editedSince);
      dbObject.setEditedExpiry(editedExpiry);
      return dbObject.updateEditedByOnly();
    }
    catch (Exception e) {
      throw new RemoteException("updateEditedByOnly failed", e);
    }
  }
  
}
