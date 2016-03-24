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

// $Id: AppRemoteDbSessionImpl.java 439 2008-09-18 15:09:32Z harald $

package org.tentackle.appworx.rmi;

import java.rmi.RemoteException;
import org.tentackle.appworx.AppDbObjectCache;
import org.tentackle.appworx.AppUserInfo;
import org.tentackle.db.UserInfo;
import org.tentackle.db.rmi.RemoteDbConnectionImpl;
import org.tentackle.db.rmi.RemoteDbSessionImpl;


/**
 * Extends {@link RemoteDbSessionImpl} using {@link AppUserInfo} and {@link AppDbObjectCache}.
 *
 * @author  harald
 */
public class AppRemoteDbSessionImpl extends RemoteDbSessionImpl implements AppRemoteDbSession {
  
  
  /**
   * Creates a session on a given connection.
   * 
   * @param con the connection
   * @param clientInfo the UserInfo from the client
   * @param serverInfo the UserInfo to establish the connection to the database server
   *
   * @throws RemoteException if the session could not initiated.
   */
  public AppRemoteDbSessionImpl(RemoteDbConnectionImpl con, UserInfo clientInfo, UserInfo serverInfo) throws RemoteException {
    
    super(con, clientInfo, serverInfo);
    
    if (!(clientInfo instanceof AppUserInfo)) {
      throw new RemoteException("AppUserInfo required, got " + clientInfo.getClass());
    }
  }
  
  
  
  public long getUserId() throws RemoteException {
    try {
      return ((AppUserInfo)getClientUserInfo()).getUserId();
    }
    catch (Exception ex) {
      throw new RemoteException("getUserId() failed", ex);
    }
  }
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * If the db is pooled, it will be returned to the pool instead of being closed.
   * Otherwise all entries in the AppDbObjectCache will be removed for that db.
   */
  @Override
  protected void closeDb() {
    if (getDb() != null && !getDb().isPooled())  {
      // remove all objects for this db in all caches
      AppDbObjectCache.removeObjectsForDbInAllCaches(getDb());
      // @todo: run this in an extra thread with low priority
    }
    super.closeDb();
  }

}
