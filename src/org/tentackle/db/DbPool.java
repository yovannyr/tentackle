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

// $Id: DbPool.java 379 2008-07-31 16:55:28Z harald $

package org.tentackle.db;

/**
 * A pool of logical Db connections.
 * <p>
 * The DbPool manages a pool of {@link Db} instances.
 * From an application's point of view, a DbPool resembles to what
 * is known as a "connection pool" in traditional (mostly J2EE) server-based applications.
 * However, because a Db does not necessarily correspond to a physical connection
 * (this is up to a {@link ConnectionManager}), a DbPool is one abstraction level
 * above a typical connection pool. 
 * In fact, Tentackle servers use a DbPool, but only to optimize caching.
 * The Db instances will run on an {@link MpxConnectionManager},
 * which actually does the multiplexing of physical connections. 
 * So don't mix up a DbPool with a multiplexing connection manager!<br>
 * Of course, you can use a DbPool with a non-multiplexing connection manager.
 * This can make sense in pure web-applications providing their own session management
 * (JSP, for example). For fat-client application servers, however, this is
 * not a good idea, because the client is associated with a Db (in fact two Dbs)
 * for as long as it is logged in and not just for the duration of a rather
 * short-timed http-request/response roundtrip.
 *
 * @author harald
 */
public interface DbPool {
  
  /**
   * Gets the maximum poolsize.
   *
   * @return the max. number of concurrent Db instances, 0 = unlimited
   */
  public int getMaxSize();
  
  
  /**
   * Gets the current number of Db instances.
   *
   * @return the number of Db managed by this pool
   */
  public int getSize();
  
  
  /**
   * Gets a Db instance from the pool.
   *
   * @return an open Db ready for use, never null
   * @throws DbRuntimeException if pool is exhausted
   */
  public Db getDb() throws DbRuntimeException;
  
  
  /**
   * Returns a Db instance to the pool.
   * <p>
   * Notice: returning a Db to the pool more than once is allowed.
   *
   * @param db the Db instance
   * @throws DbRuntimeException if the Db cannot be returned
   */
  public void putDb(Db db) throws DbRuntimeException;
  
  
  /**
   * Closes all databases in the pool, cleans up and makes the pool unusable.
   */
  public void shutdown();
  
}
