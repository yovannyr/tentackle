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

// $Id: ObjectIdRemoteDelegateImpl.java 336 2008-05-09 14:40:20Z harald $
// Created on November 19, 2003, 4:13 PM

package org.tentackle.db.rmi;

import org.tentackle.db.ObjectId;
import java.rmi.RemoteException;

/**
 * Remote delegate implementation for {@link ObjectId}.
 * @param <T> the ObjectId class
 * @author  harald
 */
public class ObjectIdRemoteDelegateImpl<T extends ObjectId>
       extends DbObjectRemoteDelegateImpl<T>
       implements ObjectIdRemoteDelegate {
                              
  private static final long serialVersionUID = 6405347750236598078L;
  

  public ObjectIdRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
  }
  
  public long nextId() throws RemoteException {
    try {
      return dbObject.nextId();
    }
    catch (Exception e) {
      throw new RemoteException("nextId() failed", e);
    }
  }

}
