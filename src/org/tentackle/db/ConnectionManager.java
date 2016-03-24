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

// $Id: ConnectionManager.java 348 2008-06-06 15:21:47Z harald $

package org.tentackle.db;

/**
 * Connection Manager for local connections.<br>
 *
 * JDBC connections are never used directly. Instead Db-instances refer to a ConnectionManager
 * in order to use a connection. The usage of a connection is initiated by an attach operation.
 * After completion of the task (transaction, one-shot, etc...) the Db will be detached.
 * The connection manager as a broker in between the Db and a connection gives the opportunity to
 * multiplex connections. This is especially useful in application servers.
 * Note that connection managers are not used in remote Db instances.
 *
 * @author harald
 */
public interface ConnectionManager {
  
  /**
   * Logs in a {@link Db} connection.<br>
   * It is up to the manager how to verify whether the Db is allowed to open,
   * a real connection is initiated or just an application level authorization
   * is performed. Note that the ID may be recycled by the manager after a
   * Db is logged out.
   *
   * @param db the db to login
   * @return the connection ID of the db (> 0)
   * @throws DbRuntimeException if login failed.
   */
  public int login(Db db) throws DbRuntimeException;
  
  
  /**
   * Logs out a {@link Db} connection.
   * The Db is not allowed to attach anymore. If the Db is still attached,
   * a rollback of any pending tx is done and an exception thrown.
   *
   * @param id the connection ID of the db
   * @return the logged out Db
   * @throws DbRuntimeException if logout failed.
   */
  public Db logout(int id) throws DbRuntimeException;
  
  
  /**
   * Attaches a {@link Db} to a connection.<br>
   * A Db must be attached before it can use any statements.
   * The framework will attach at the begining of a transaction or when getting a prepared
   * statement or when getting a one-shot non-prepared statement.
   * Note that attachments can be nested to any depth, i.e. only the first attach really binds
   * the connection to the Db. 
   *
   * @param id the connection ID of the db
   * @return the connection attached to be used by subsequent operations
   * @throws DbRuntimeException if attach failed.
   */
  public ManagedConnection attach(int id) throws DbRuntimeException;
  
  
  /**
   * Detaches a connection from a {@link Db}.<br>
   * A Db must be detached to release the connection for use of other Db instances.
   * The framework will detach the db on every commit or rollback, after executeUpdate
   * or after a resultset is closed for an executeQuery.
   * Note that attachments can be nested to any depth, i.e. only the last detach really
   * unbinds the connection from the Db.
   *
   * @param id the connection ID of the db
   * @throws DbRuntimeException if detach failed.
   */
  public void detach(int id) throws DbRuntimeException;
  
  
  /**
   * Gets the maximum number of allowed logins.
   *
   * @return max. number of concurrent logins, 0 = unlimited
   */
  public int getMaxLogins();
  
  
  /**
   * Gets the maximum number of connections.
   *
   * @return max. number of concurrent connections, 0 = unlimited
   */
  public int getMaxConnections();
  
  
  /**
   * Shuts down this connection manager.<br>
   * All connections are closed and the threads stopped.
   * Application servers should invoke this method when shut down.
   */
  public void shutdown();
}
