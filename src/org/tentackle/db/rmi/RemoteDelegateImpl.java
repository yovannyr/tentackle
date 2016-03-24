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

// $Id: RemoteDelegateImpl.java 374 2008-07-25 12:58:09Z harald $
// Created on November 22, 2003, 12:53 PM


package org.tentackle.db.rmi;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import org.tentackle.db.Db;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.tentackle.db.DbGlobal;
import org.tentackle.util.Logger.Level;

/**
 * All remote delegates must extend this class
 *
 * @param <T> the class handled by this delegate
 * @author  harald
 */
public abstract class RemoteDelegateImpl<T> extends UnicastRemoteObject implements RemoteDelegate {
  
  
  /** the server session **/
  protected RemoteDbSessionImpl session;
  /** the local db-connection **/
  protected Db db;
  /** class handled by this delegate **/
  protected Class<T> clazz;
  /** the connection port **/
  protected int port;
  /** the client socket factory **/
  protected RMIClientSocketFactory csf;
  /** the server socket factory **/
  protected RMIServerSocketFactory ssf;
  
  
  
  /**
   * Creates a delegate.
   *
   * @param session the RMI session
   * @param clazz the class the delegate provides service for
   * @param port the port number on which the remote object receives calls,
   *        0 = system default
   * @param csf the client-side socket factory for making calls to the
   *        remote object, null = system default
   * @param ssf the server-side socket factory for receiving remote calls, 
   *        null = system default
   * @throws RemoteException 
   */
  public RemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz,
                            int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) 
         throws RemoteException {
    
    super(port, csf, ssf);

    if (DbGlobal.logger.isLoggable(Level.FINE)) {
      DbGlobal.logger.fine("Delegate created for session=" + session + ", class=" + clazz.getName() + ", port=" + port +
                         ", csf=" + (csf == null ? "<default>" : csf.getClass().getName()) +
                         ", ssf=" + (ssf == null ? "<default>" : ssf.getClass().getName()));
    }
    
    this.session = session;
    this.db      = session.getDb();
    this.clazz   = clazz;
    this.port    = port;
    this.csf     = csf;
    this.ssf     = ssf;
  }  
  
  
  /**
   * Creates delegate on the session socket.
   * This should be the default constructor.
   *
   * @param session is the RMI session
   * @param clazz is the class the delegate provides service for
   * @throws RemoteException 
   */
  public RemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    this(session, clazz,
         session.getPort(),
         session.getClientSocketFactory(),
         session.getServerSocketFactory());
  }
  
  
  /**
   * Creates delegate running on one of the four different predefined session sockets.
   * Useful, for example, if the session usually is non-ssl but certain data needs ssl.
   *
   * @param session is the RMI session
   * @param clazz is the class the delegate provides service for
   * @param socketConfig is of RemoteDbSessionImpl.SOCKETCONFIG_...
   * @throws RemoteException 
   */
  public RemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz, int socketConfig) 
         throws RemoteException {
    this(session, clazz, 
         session.getPort(socketConfig), 
         session.getClientSocketFactory(socketConfig), 
         session.getServerSocketFactory(socketConfig));
  }
  
  
  /**
   * Gets the server session.
   * 
   * @return the session
   */
  public RemoteDbSessionImpl getSession() {
    return session;
  }
  
  
  /**
   * Gets the port number.
   * 
   * @return the port for this delegate
   */
  public int getPort() {
    return port;
  }
  
  
  /**
   * Gets the client socket factory.
   * 
   * @return the csf for this delegate
   */
  public RMIClientSocketFactory getClientSocketFactory() {
    return csf;
  }
  
  
  /**
   * Gets the server socket factory.
   * 
   * @return the ssf for this delegate
   */
  public RMIServerSocketFactory getServerSocketFactory() {
    return ssf;
  }
  
}
