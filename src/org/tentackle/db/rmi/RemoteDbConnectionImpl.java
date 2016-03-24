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

// $Id: RemoteDbConnectionImpl.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.db.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.UserInfo;



/**
 * Server side RMI-connection implementation.
 * <p>
 * Overview of what happens in a Tentackle RMI application server:
 * <ul>
 * <li>client gets a reference to a RemoteDbConnection in the server</li>
 * <li>client invokes login() on the connection and gets a reference to 
 *   a RemoteDbSession.</li>
 * <li>each session runs in its own thread (server & client) and the server
 *   session gets its own Db.</li>
 * <li>all further client requests work through the session object and
 *     the remote delegates created within the session</li>
 * <li>the client invokes logout() to close the session and Db.</li>
 * <li>in case the client terminates abnormally (without invoking logout())
 *   a timeout runs logout() via finalize().</li>
 * </ul>
 * @author harald
 */


public class RemoteDbConnectionImpl extends UnicastRemoteObject
                                    implements RemoteDbConnection {

  private static final long serialVersionUID = -1008588987968414154L;
  
  private DbServer server;              // the server
  private int port;                     // default port for all sessions
  private RMIClientSocketFactory csf;   // default client socket factory for all sessions
  private RMIServerSocketFactory ssf;   // default server socket factory for all sessions
  

  /**
   * Creates a connection.<br>
   *
   * @param server the DbServer
   * @param port the tcp-port, 0 = system default
   * @param csf the client socket factory, null = system default
   * @param ssf the server socket factory, null = system default
   * @throws RemoteException 
   * @see DbServer
   */
  public RemoteDbConnectionImpl(DbServer server,
                                int port, 
                                RMIClientSocketFactory csf,
                                RMIServerSocketFactory ssf) throws RemoteException {
    super(port, csf, ssf);
    
    this.server = server;
    this.port   = port;
    this.csf    = csf;
    this.ssf    = ssf;
  }
  
  
  /**
   * Gets the DbServer.
   * 
   * @return the db server
   */
  public DbServer getServer() {
    return server;
  }

  
  /**
   * Gets the tcp port for this connection.
   *
   * @return the tcp-port for this connection, 0 = system default
   */
  public int getPort() {
    return port;
  }
  
  
  /**
   * Gets the client socket factory.
   * 
   * @return the client socket factory for this connection, null = system default
   */
  public RMIClientSocketFactory getClientSocketFactory() {
    return csf;
  }
  
  
  /**
   * Gets the server socket factory.
   * 
   * @return the server socket factory for this connection, null = system default
   */
  public RMIServerSocketFactory getServerSocketFactory() {
    return ssf;
  }
  
  
  /**
   * Gets the server UserInfo.<br>
   * If it is not provided for this connection, it will be cloned from the given clientInfo.
   * 
   * @param clientInfo the default user info if no server info
   * @return the server info
   * @throws RemoteException 
   */
  protected UserInfo getServerUserInfo(UserInfo clientInfo) throws RemoteException {
    if (server.getServerUserInfo() == null) {
      // create the session with serverInfo cloned from given clientInfo (Properties cleared!)
      return clientInfo.clone();
    }
    return server.getServerUserInfo();
  }

  
  /**
   * Overridden to detect unwanted garbage collection as this
   * should never happen.
   * If using DbServer, this will not happen because it keeps a reference
   * to the connection object.
   */
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    DbGlobal.logger.warning("Connection object " + getClass().getName() + " finalized:\n" + this);
  }
  
  
  
  // ------------------ implements RemoteDbConnection -----------------
  
  public RemoteDbSession login(UserInfo clientInfo) throws RemoteException {
    // create the session
    return new RemoteDbSessionImpl(this, clientInfo, getServerUserInfo(clientInfo));
  }

  
  public void logout(RemoteDbSession session) throws RemoteException {
    session.close();
  }


}