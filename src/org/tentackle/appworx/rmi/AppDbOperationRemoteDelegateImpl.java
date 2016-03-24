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

// $Id: AppDbOperationRemoteDelegateImpl.java 410 2008-09-03 12:42:26Z harald $

package org.tentackle.appworx.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import org.tentackle.appworx.AppDbOperation;
import org.tentackle.appworx.ContextDb;
import org.tentackle.db.rmi.DbOperationRemoteDelegateImpl;
import org.tentackle.db.rmi.RemoteDbSessionImpl;



/**
 * Implementation of the remote delegate for {@link AppDbOperation}.<br>
 * Never instantiated.
 *
 * @param <T> the {@code AppDbOperation} class
 * @author harald
 */
public abstract class AppDbOperationRemoteDelegateImpl<T extends AppDbOperation>
                      extends DbOperationRemoteDelegateImpl<T>
                      implements AppDbOperationRemoteDelegate {
  
  
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
  public AppDbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz,
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
  public AppDbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
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
  public AppDbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz, int socketConfig) throws RemoteException {
    super(session, clazz, socketConfig);
    setup();
  }
  
  
  /**
   * Instantiates a singe object and connect it to the database context.
   * The object is used for all methods not returning a new object and thus
   * minimizes object construction.
   */
  @Override
  protected void setup() throws RemoteException {
    /**
     * Try to set a default contextDb.
     * This will fail in apps that extend the contextdb, but is useful for other apps.
     */
    try {
      dbObject = newObject(new ContextDb(db));
    }
    catch (Exception e) {
      /**
       * Application requires an extended contextdb.
       * Try construction with a db only.
       */
      dbObject = newObject();
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
      return clazz.getConstructor(ContextDb.class).newInstance(cb);
    } 
    catch (Exception e) {
      throw new RemoteException("creating object for class '" + clazz + "' in failed", e); 
    }
  }
  
}
