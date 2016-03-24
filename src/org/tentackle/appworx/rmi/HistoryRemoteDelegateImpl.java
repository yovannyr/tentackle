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

// $Id: HistoryRemoteDelegateImpl.java 452 2009-03-13 14:01:46Z harald $

package org.tentackle.appworx.rmi;

import org.tentackle.appworx.History;
import org.tentackle.db.rmi.RemoteDbSessionImpl;
import java.rmi.RemoteException;
import java.util.List;
import org.tentackle.appworx.AppDbObject;
import org.tentackle.appworx.ContextDb;

/**
 * Implementation of the history remote delegate.
 * 
 * @param <T> the history class
 * @author harald
 */
public class HistoryRemoteDelegateImpl<T extends History> 
             extends AppDbObjectRemoteDelegateImpl<T> 
             implements HistoryRemoteDelegate {
  
  /**
   * Creates a history delegate on the session socket.
   *
   * @param session the RMI session
   * @param clazz the subclass of DbObject
   * @throws java.rmi.RemoteException 
   */
  public HistoryRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
  }
  
  public List<History> selectByObjectId(long objectId) throws RemoteException  {
    try {
      return dbObject.selectByObjectId(objectId);
    }
    catch (Exception e) {
      throw new RemoteException("selectByObjectId failed", e); 
    }                  
  }

  public AppDbObject restoreObjectForSerial(ContextDb cb, long objectId, long objectSerial) throws RemoteException  {
    try {
      return newObject(cb).restoreObjectForSerial(objectId, objectSerial);
    }
    catch (Exception e) {
      throw new RemoteException("restoreObjectForSerial failed", e);
    }
  }
    
}
