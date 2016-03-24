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

// $Id: DbOperationClassVariables.java 470 2009-07-31 07:14:05Z svn $


package org.tentackle.db;

import org.tentackle.db.rmi.DbOperationRemoteDelegate;

/**
 * Holds static class variables for classes derived from DbOperation.
 * This is a "singleton per class".
 *
 * @author harald
 */
public class DbOperationClassVariables {
  
  
  /**
   * the class
   */
  public Class<? extends DbOperation> clazz;
  
  /**
   * the full classname
   */
  public String className;
  
  /**
   * ID for the remote delegate for this class.
   */
  public int remoteDelegateId;
  
  
  /**
   * Gets the delegateId of the class, i.e. subclass of DbOperation.
   * If the remoteDelegateId is 0, it will be prepared
   * The delegateId is unique for each class. It is valid only in remote connections and
   * is the same for all remote Db's.
   * The RMI-server creates a delegate for each subclass of DbOperation (DbOperationRemoteDelegateImpl resp.)
   *
   * @return the delegate id
   */
  public int getRemoteDelegateId() {
    if (remoteDelegateId == 0)  {
      remoteDelegateId = Db.prepareRemoteDelegate(clazz);
    }
    return remoteDelegateId;
  }
  
  /**
   * Gets the RemoteDelegate for the class and db.
   * @param db the db connection
   * @return the delegate
   */
  public DbOperationRemoteDelegate getRemoteDelegate(Db db) {
    return (DbOperationRemoteDelegate)db.getRemoteDelegate(getRemoteDelegateId());
  }
  
  
  /**
   * constructs a classvariable.
   * Throws IllegalStateException if already constructed.
   *
   * @param clazz is the class of the derived DbOperation
   */
  public DbOperationClassVariables(Class<? extends DbOperation> clazz)  {
    this.clazz      = clazz;
    className       = clazz.getName();
  }
  
  
  @Override
  public String toString() {
    return className;
  }

}
