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

// $Id: IdSourceConnector.java 336 2008-05-09 14:40:20Z harald $
// Created on October 16, 2004, 10:59 AM

package org.tentackle.db;

import org.tentackle.util.ApplicationException;


/**
 * The connector establishes a connection to an {@link IdSource}.
 * Typically, the connector is an IdPoolAgent or a local IdPool from PoolKeeper.
 * However, other implementations are possible as well.
 *
 * @author harald
 */
public interface IdSourceConnector {
  
  /**
   * Connects the {@link IdSource}.
   *
   * @param db is the db-connection
   * @param url rmi-URL if the IdSource is remote, null = local
   * @param space namespace
   * @param name poolname
   * @param realm poolrealm, null = none.
   *
   * @return the id source
   * 
   * @throws ApplicationException if connection could not be established.
   */
  public IdSource connect(Db db, String url, String space, String name, String realm) 
          throws ApplicationException;
  
}
