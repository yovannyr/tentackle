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

// $Id: RemoteDbSession.java 374 2008-07-25 12:58:09Z harald $
// Created on November 14, 2003, 2:35 PM

package org.tentackle.db.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import org.tentackle.util.Logger.Level;



/**
 * Application server session.<br>
 * The session will create all other delegates for the client.
 *
 * @author harald
 */
public interface RemoteDbSession extends Remote {
  
  
  /**
   * Closes a session.
   * 
   * @throws RemoteException 
   */
  public void close() throws RemoteException;


  /**
   * Sends text based logging infos to the RMI-Server.
   * 
   * @param level 
   * @param message
   * @throws RemoteException 
   */
  public void log(Level level, String message) throws RemoteException;
  
  
  /**
   * Gets the delegate for the remote db-connection.
   * 
   * @return the delegate
   * @throws RemoteException 
   */
  public DbRemoteDelegate getDbRemoteDelegate() throws RemoteException;
  
  /**
   * Gets the delegate for a given classname.<br>
   * Per class the rmi-clients request a remote access there must be
   * a RemoteDelegate. The delegate is determined from the classname of the
   * client class as follows:<br>
   * <tt>&lt;package&gt;.rmi.&lt;last-element-of-classname&gt;.class</tt> on the client-side and<br>
   * <tt>&lt;package&gt;.rmi.&lt;last-element-of-classname&gt;Impl.class</tt> on the server-side.<br>
   *
   * The delegates are cached on the client side in order to speed up access.
   * Delegates are bound to a session, i.e. there will be one delegate for each
   * session and class.
   *
   * @param classname is the name of class
   * @param delegateId is the client-side ID of the delegate
   * @return the delegate
   * @throws RemoteException 
   */
  public RemoteDelegate getRemoteDelegate(String classname, int delegateId) throws RemoteException;
  
}
