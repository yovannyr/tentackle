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

// $Id: RemoteDbCursor.java 361 2008-06-19 13:40:23Z harald $
// Created on November 21, 2003, 12:25 PM

package org.tentackle.db.rmi;

import org.tentackle.db.DbObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote delegate interface for {@link org.tentackle.db.DbCursor}.
 * @author  harald
 */
public interface RemoteDbCursor extends Remote {
  
  public void close() throws RemoteException;

  public int getRowCount() throws RemoteException;

  public int getRow() throws RemoteException;

  public boolean setRow(int row) throws RemoteException;

  public int first() throws RemoteException;

  public int last() throws RemoteException;

  public boolean next() throws RemoteException;

  public boolean previous() throws RemoteException;

  public void beforeFirst() throws RemoteException;
  
  public int afterLast() throws RemoteException;
  
  public boolean isBeforeFirst()  throws RemoteException;
  
  public boolean isAfterLast() throws RemoteException;
  
  public DbObject getObject() throws RemoteException;
  
  public boolean updateObjectOnly (DbObject object) throws RemoteException;

  public boolean updateObject(DbObject object) throws RemoteException;

  public boolean deleteObject() throws RemoteException;

  public DbObject getObjectAt(int row) throws RemoteException;

  public boolean updateObjectAt(DbObject object, int row) throws RemoteException;

  public boolean deleteObjectAt(int row) throws RemoteException;
  
  public List<? extends DbObject> toList() throws RemoteException;
  
  public String getDbClassName() throws RemoteException;
  
  public String getName() throws RemoteException;
  
  public void setFetchSize(int rows) throws RemoteException;
  
  public int getFetchSize() throws RemoteException;
  
  public void setFetchDirection(int direction) throws RemoteException;
  
  public int getFetchDirection() throws RemoteException;
  
  public List<? extends DbObject> fetch() throws RemoteException;
  
}
