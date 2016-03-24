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

// $Id: DbOperationRemoteDelegateImpl.java 410 2008-09-03 12:42:26Z harald $

package org.tentackle.db.rmi;

import java.lang.reflect.Modifier;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.RemoteException;
import org.tentackle.db.Db;
import org.tentackle.db.DbOperation;


/**
 * Base class for the remote delegate of DbOperation.<br>
 * All other subclasses of DbOperation should extend DbOperationRemoteDelegateImpl to reflect
 * the class hierarchy below DbOperation.
 *
 * @param <T> the database operation class
 * @author harald
 */
public abstract class DbOperationRemoteDelegateImpl<T extends DbOperation> 
                      extends RemoteDelegateImpl<T>
                      implements DbOperationRemoteDelegate {
  
  
  protected T dbObject;    // operation object associated to current db (used in generated methods)
  
  
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
  public DbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz,
                                       int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
    super(session, clazz, port, csf, ssf);
    setup();
  }
  
  
  /**
   * Creates a delegate on the session socket.
   *
   * @param session is the RMI session
   * @param clazz is the subclass of DbOperation
   * @throws RemoteException 
   */
  public DbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
    setup();
  }
  
  
  /**
   * Creates a delegate on a non-default session socket.
   * 
   * @param session is the RMI session
   * @param clazz is the subclass of DbOperation
   * @param socketConfig is of RemoteDbSessionImpl.SOCKETCONFIG_...
   * @throws RemoteException 
   */
  public DbOperationRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz, int socketConfig) throws RemoteException {
    super(session, clazz, socketConfig);
    setup();
  }
  
  
  
  
  /**
   * Instantiates a singe object and connect it to the db. (if class is not abstract).
   * The object is used for all methods not returning a new object and thus
   * minimizes object construction.
   * 
   * @throws java.rmi.RemoteException
   */
  protected void setup() throws RemoteException {
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
      return clazz.getConstructor(Db.class).newInstance(db);
    } 
    catch (Exception e) {
      throw new RemoteException("creating object for class '" + clazz + "' failed", e); 
    }
  }
  
}
