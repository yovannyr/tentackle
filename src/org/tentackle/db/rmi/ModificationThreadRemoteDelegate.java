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

// $Id: ModificationThreadRemoteDelegate.java 336 2008-05-09 14:40:20Z harald $
// Created on November 14, 2003, 2:35 PM

package org.tentackle.db.rmi;

import java.rmi.RemoteException;



/**
 * Remote delegate interface for {@link org.tentackle.db.ModificationThread}.
 * @author  harald
 */
public interface ModificationThreadRemoteDelegate extends RemoteDelegate {

  public long selectMasterSerial() throws RemoteException;
  
  public long[] selectIdSerialForName(String className, String tableName) throws RemoteException;
  
  public String selectNameForId(long id) throws RemoteException;
  
  public long[] readSerials(long[] ids) throws RemoteException;
  
}
