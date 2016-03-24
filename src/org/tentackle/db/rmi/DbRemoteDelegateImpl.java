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

// $Id: DbRemoteDelegateImpl.java 336 2008-05-09 14:40:20Z harald $
// Created on November 18, 2003, 7:54 PM

package org.tentackle.db.rmi;

import java.util.List;
import org.tentackle.db.Db;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.tentackle.db.ModificationLog;

/**
 * Remote delegate implementation for {@link Db}.
 * 
 * @author  harald
 */
public class DbRemoteDelegateImpl extends UnicastRemoteObject
                                  implements DbRemoteDelegate {
  
  private static final long serialVersionUID = -1222772401350267129L;
  
  private Db db;                          // the local Db-connection
  private RemoteDbSessionImpl session;    // the current session
                                    
                                    
  public DbRemoteDelegateImpl(RemoteDbSessionImpl session) throws RemoteException {
    
    super(session.getPort(),
          session.getClientSocketFactory(),
          session.getServerSocketFactory());
    
    this.db = session.getDb();
    this.session = session;
  }

  
  public RemoteDbSessionImpl getSession() {
    return session;
  }
  
  public Db getDb() {
    return db;
  }
  
  public int getConnectionId() throws RemoteException {
    try {
      return db.getConnectionId();
    }
    catch (Exception ex)  {
      throw new RemoteException("remote getConnectionId failed", ex);
    }           
  }
  
  public void setGroupId(int number) throws RemoteException {
    try {
      db.setGroupId(number);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setGroupId failed", ex);
    }         
  }
  
  public int getGroupId() throws RemoteException {
    try {
      return db.getGroupId();
    }
    catch (Exception ex)  {
      throw new RemoteException("remote getGroupId failed", ex);
    }         
  }
  
  public boolean isAlive() throws RemoteException {
    try {
      return db.isAlive();
    }
    catch (Exception ex)  {
      throw new RemoteException("remote isAlive failed", ex);
    }             
  }
  
  public void setAlive(boolean alive) throws RemoteException {
    try {
      db.setAlive(alive);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setAlive failed", ex);
    }             
  }
  
  public boolean commit(boolean oldCommit) throws RemoteException {
    try {
      return db.commit(oldCommit);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote commit failed", ex);
    }             
  }
  
  public boolean rollback(boolean oldCommit) throws RemoteException {
    try {
      return db.rollback(oldCommit);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote rollback failed", ex);
    }             
  }
  
  public long begin(String txName) throws RemoteException {
    try {
      return db.begin(txName) ? db.getTxCount() : 0;
    }
    catch (Exception ex)  {
      throw new RemoteException("remote begin failed", ex);
    }    
  }  
  
  public void setCountModificationAllowed(boolean flag) throws RemoteException  {
    try {
      db.setCountModificationAllowed(flag);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setCountModificationAllowed failed", ex);
    }             
  }
  
  public void setLogModificationAllowed(boolean flag) throws RemoteException  {
    try {
      db.setLogModificationAllowed(flag);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setLogModificationAllowed failed", ex);
    }             
  }
  
  public void setLogModificationTxEnabled(boolean flag) throws RemoteException  {
    try {
      db.setLogModificationTxEnabled(flag);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setLogModificationTxEnabled failed", ex);
    }             
  }
  
  public void setLogModificationTxId(long txId) throws RemoteException {
    try {
      db.setLogModificationTxId(txId);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setLogModificationTxId failed", ex);
    }         
  }
  
  public long getLogModificationTxId() throws RemoteException {
    try {
      return db.getLogModificationTxId();
    }
    catch (Exception ex)  {
      throw new RemoteException("remote getLogModificationTxId failed", ex);
    }         
  }
  
  public void setLogModificationDeferred(boolean flag) throws RemoteException  {
    try {
      db.setLogModificationDeferred(flag);
    }
    catch (Exception ex)  {
      throw new RemoteException("remote setLogModificationDeferred failed", ex);
    }             
  }
  
  public List<ModificationLog> getDeferredModificationLogList() throws RemoteException {
    try {
      return db.getDeferredModificationLogList();
    }
    catch (Exception ex)  {
      throw new RemoteException("remote getDeferredModificationLogList failed", ex);
    }         
  }
  
}
