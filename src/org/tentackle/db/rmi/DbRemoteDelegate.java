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

// $Id: DbRemoteDelegate.java 336 2008-05-09 14:40:20Z harald $
// Created on November 18, 2003, 7:52 PM

package org.tentackle.db.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import org.tentackle.db.ModificationLog;

/**
 * Remote delegate interface for {@link org.tentackle.db.Db}.
 * 
 * @author harald
 */
public interface DbRemoteDelegate extends Remote {
  
  public int getConnectionId() throws RemoteException;
  
  public void setGroupId(int number) throws RemoteException;
  
  public int getGroupId() throws RemoteException;
  
  public boolean isAlive() throws RemoteException;
  
  public void setAlive(boolean alive) throws RemoteException;
  
  public long begin(String txName) throws RemoteException;
  
  public boolean commit(boolean oldCommit) throws RemoteException;
  
  public boolean rollback(boolean oldCommit) throws RemoteException;

  public void setCountModificationAllowed(boolean flag) throws RemoteException;
  
  public void setLogModificationAllowed(boolean flag) throws RemoteException;
 
  public void setLogModificationTxEnabled(boolean flag) throws RemoteException;
  
  public void setLogModificationTxId(long txId) throws RemoteException;
  
  public long getLogModificationTxId() throws RemoteException;
  
  public void setLogModificationDeferred(boolean flag) throws RemoteException;
  
  public List<ModificationLog> getDeferredModificationLogList() throws RemoteException;
  
}
