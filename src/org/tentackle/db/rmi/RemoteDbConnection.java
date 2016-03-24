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

// $Id: RemoteDbConnection.java 336 2008-05-09 14:40:20Z harald $
// Created on November 14, 2003, 2:31 PM

package org.tentackle.db.rmi;

import org.tentackle.db.UserInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * RMI remote connection.<br>
 * This is the first remote object passed to the client when it connects
 * to the application server. The connection object handles the login
 * and logout to/from a session. The socket factories used for the
 * connection object are defined by the server. The session, however,
 * and all subsequent remote objects can get different socket factories
 * depending on what the client requests. See {@link DbServer} for more details.
 *
 * @author harald
 */

public interface RemoteDbConnection extends Remote {
  
  /**
   * Login to remote RMI-server.
   *
   * @param clientInfo the user information
   * @return the session
   * @throws RemoteException if some error, for example wrong passord
   */
  public RemoteDbSession login(UserInfo clientInfo) throws RemoteException;


  /**
   * Logout from remote RMI-Server.
   *
   * @param session to be closed
   * @throws RemoteException if some error
   */
  public void logout(RemoteDbSession session) throws RemoteException;

}
