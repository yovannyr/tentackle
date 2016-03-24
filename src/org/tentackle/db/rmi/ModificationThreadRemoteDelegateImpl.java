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

// $Id: ModificationThreadRemoteDelegateImpl.java 336 2008-05-09 14:40:20Z harald $
// Created on November 19, 2003, 6:40 PM

package org.tentackle.db.rmi;

import org.tentackle.db.Db;
import org.tentackle.db.ModificationThread;
import java.rmi.RemoteException;
import org.tentackle.util.ApplicationException;

/**
 * Remote delegate implementation for {@link ModificationThread}. 
 * @param <T> the modification thread class
 * @author  harald
 */
public class ModificationThreadRemoteDelegateImpl<T extends ModificationThread>
             extends RemoteDelegateImpl<T>
             implements ModificationThreadRemoteDelegate {
  
  private static final long serialVersionUID = 133621589808120162L;
  
  /**
   * hack to circumvent Singleton (only used if there is not thread running in server)
   */
  private static class ProxyThread extends ModificationThread {
    ProxyThread(Db db)  {
      super(db, 0, false);
    }
  }
  
  private ModificationThread mt;            // the server's modthread
  private ModificationThread mtProxy;       // != null if there is no modthread running in server (i.e. only the dummy thread)
  
  
  public ModificationThreadRemoteDelegateImpl(RemoteDbSessionImpl session, Class<T> clazz) throws RemoteException {
    super(session, clazz);
    mt = ModificationThread.getThread();
    if (mt.isDummy()) {
      mtProxy = new ProxyThread(db);    // don't "run" this!!!
    }
  }
  
  public long selectMasterSerial() throws RemoteException {
    try {
      return mtProxy != null ? mtProxy.selectMasterSerial() : mt.getMasterSerial();
    }
    catch (Exception ex)  {
      throw new RemoteException("selectMasterSerial failed", ex);
    }           
  }
  
  public long[] selectIdSerialForName(String className, String tableName) throws RemoteException {
    try {
      // make sure class is loaded, i.e. classVariables are setup properly
      Class.forName(className);
      if (mtProxy != null) {
        return mtProxy.selectIdSerialForName(tableName);
      }
      // real mt running in server, save physical db-roundtrip
      long[] idser = mt.getIdSerialForName(tableName);
      if (idser[1] == -1) {
        // table not monitored in server so far: create a dummy entry
        mt.registerTable(tableName, (Runnable)null);
        idser = mt.getIdSerialForName(tableName);   // must work now
        if (idser[1] == -1) {
          throw new ApplicationException("serial for table " + tableName + " still unknown");
        }
      }
      return idser;
    }
    catch (Exception ex)  {
      throw new RemoteException("selectIdSerialForName failed", ex);
    }           
  }
  
  public String selectNameForId(long id) throws RemoteException {
    try {
      return mtProxy != null ? mtProxy.selectNameForId(id) : mt.selectNameForId(id);
    }
    catch (Exception ex)  {
      throw new RemoteException("selectNameForId failed", ex);
    }               
  }
  
  public long[] readSerials(long[] ids) throws RemoteException {
    try {
      if (mtProxy != null) {
        return mtProxy.readSerials(ids);
      }
      // real mt running in server, save physical db-roundtrip
      long[] serials = mt.getSerials(ids);
      /**
       * Check that every table is really monitored by the server.
       * If not, add it.
       * Because clients when registering a table will always create
       * a modification record in the db, we can simply select by the table ID.
       * If for some reason the table does not exist, we will throw an Exception (should never happen)
       */
      for (int i=0; i < serials.length; i++) {
        if (serials[i] == -1) {
          // table not monitored in server so far
          long id = ids[i];
          String tableName = mt.selectNameForId(id);
          if (tableName == null) {
            throw new ApplicationException("table ID=" + id + " not registered");
          }
          // create a dummy entry
          mt.registerTable(tableName, (Runnable)null);
          long[] idser = mt.getIdSerialForName(tableName);   // must work now
          long serial = idser[1];
          if (serial == -1) {
            throw new ApplicationException("serial for table " + tableName + " still unknown");
          }
          // set the serials for this id
          for (int j=0; j < serials.length; j++) {
            if (ids[j] == id) {
              serials[j] = serial;
            }
          }
        }
      }
      return serials;
    }
    catch (Exception ex)  {
      throw new RemoteException("readSerials failed", ex);
    }           
  }

}
