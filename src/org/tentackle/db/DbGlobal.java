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

// $Id: DbGlobal.java 473 2009-08-07 17:19:36Z harald $

package org.tentackle.db;

import org.tentackle.util.Logger;
import org.tentackle.util.LoggerFactory;


/**
 * Db globals.
 *
 * @author harald
 */
public class DbGlobal {
  
  /**
   * the default connection manager if not explicitly given for a Db
   */
  public static ConnectionManager connectionManager = new DefaultConnectionManager();
  
  /**
   * If this is an application server, the server's Db should be set here to
   * enable certain optimizations between the clients and the server.
   * The default is null, i.e. client or no application server.
   */
  public static Db serverDb = null;
  
  /**
   * If this is an application server and if a DbPool is used, it must be set here.
   */
  public static DbPool serverDbPool = null;
  
  
  /**
   * Determines whether this is an application server.
   *
   * @return true if this is an RMI application server
   */
  public static boolean isServer() {
    return serverDb != null;
  }
  
  /** 
   * Default logger for the db-package
   */
  public static Logger logger = LoggerFactory.getLogger("org.tentackle.db");

  /**
   * the error handler
   */
  public static ErrorHandler errorHandler = new DefaultErrorHandler();
}
