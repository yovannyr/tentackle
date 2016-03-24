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

// $Id: RemoteDbCursorImpl.java 361 2008-06-19 13:40:23Z harald $
// Created on November 21, 2003, 2:12 PM

package org.tentackle.db.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import org.tentackle.db.DbCursor;
import org.tentackle.db.DbObject;



/**
 * Remote delegate implementation for {@link DbCursor}.
 * 
 * @author  harald
 */
public class RemoteDbCursorImpl extends UnicastRemoteObject implements RemoteDbCursor {
  
  private static final long serialVersionUID = -7839407317969020233L;
  
  private DbCursor<DbObject> cursor;      // local cursor
  
  
  @SuppressWarnings("unchecked")
  public RemoteDbCursorImpl(RemoteDelegateImpl parentDelegate, DbCursor<? extends DbObject> cursor) throws RemoteException {
    
    super(parentDelegate.getPort(), 
          parentDelegate.getClientSocketFactory(), 
          parentDelegate.getServerSocketFactory());
    
    this.cursor = (DbCursor<DbObject>)cursor;
  }
  
  
  /**
   * Gets the local cursor.
   * 
   * @return the local cursor
   */
  public DbCursor<DbObject> getCursor() {
    return cursor;
  }
  
  
  public String getName() throws RemoteException {
    return cursor.toString();
  }
  
  public int afterLast() throws RemoteException {
    try {
      cursor.afterLast();
      return cursor.getRowCount();
    }
    catch (Exception e) {
      throw new RemoteException("remote afterLast failed", e);
    }
  }
  
  public void beforeFirst() throws RemoteException {
    try {
     cursor.beforeFirst();
    }
    catch (Exception e) {
      throw new RemoteException("remote beforeFirst failed", e);
    }
  }
  
  public void close() throws RemoteException {
    try {
      cursor.close();
    }
    catch (Exception e) {
      throw new RemoteException("remote close failed", e);
    }
  }
  
  public boolean deleteObject() throws RemoteException {
    try {
      return cursor.deleteObject();
    }
    catch (Exception e) {
      throw new RemoteException("remote deleteObject failed", e);
    }
  }
  
  public boolean deleteObjectAt(int row) throws RemoteException {
    try {
      return cursor.deleteObjectAt(row);
    }
    catch (Exception e) {
      throw new RemoteException("remote deleteObjectAt failed", e);
    }
  }
  
  public int first() throws RemoteException {
    try {
      return cursor.first() ? cursor.getRow() : -1;
    }
    catch (Exception e) {
      throw new RemoteException("remote first failed", e);
    }
  }
  
  public String getDbClassName() throws RemoteException {
    try {
      Class clazz = cursor.getDbClass();
      return clazz == null ? null : clazz.getName();
    }
    catch (Exception e) {
      throw new RemoteException("remote getDbClassName failed", e);
    }
  }
  
  public DbObject getObject() throws RemoteException {
    try {
      return cursor.getObject();
    }
    catch (Exception e) {
      throw new RemoteException("remote getObject failed", e);
    }
  }
  
  public DbObject getObjectAt(int row) throws RemoteException {
    try {
      return cursor.getObjectAt(row);
    }
    catch (Exception e) {
      throw new RemoteException("remote getObjectAt failed", e);
    }
  }
  
  public int getRow() throws RemoteException {
    try {
      return cursor.getRow();
    }
    catch (Exception e) {
      throw new RemoteException("remote getRow failed", e);
    }
  }
  
  public int getRowCount() throws RemoteException {
    try {
      return cursor.getRowCount();
    }
    catch (Exception e) {
      throw new RemoteException("remote getRowCount failed", e);
    }
  }
  
  public boolean isAfterLast() throws RemoteException {
    try {
      return cursor.isAfterLast();
    }
    catch (Exception e) {
      throw new RemoteException("remote isAfterLast failed", e);
    }
  }
  
  public boolean isBeforeFirst() throws RemoteException {
    try {
      return cursor.isBeforeFirst();
    }
    catch (Exception e) {
      throw new RemoteException("remote isBeforeFirst failed", e);
    }
  }
  
  public int last() throws RemoteException {
    try {
      return cursor.last() ? cursor.getRow() : -1;
    }
    catch (Exception e) {
      throw new RemoteException("remote last failed", e);
    }
  }
  
  public boolean next() throws RemoteException {
    try {
      return cursor.next();
    }
    catch (Exception e) {
      throw new RemoteException("remote next failed", e);
    }
  }
  
  public boolean previous() throws RemoteException {
    try {
      return cursor.previous();
    }
    catch (Exception e) {
      throw new RemoteException("remote previous failed", e);
    }
  }
  
  public boolean setRow(int row) throws RemoteException {
    try {
      return cursor.setRow(row);
    }
    catch (Exception e) {
      throw new RemoteException("remote setRow failed", e);
    }
  }
  
  public List<? extends DbObject> toList() throws RemoteException {
    try {
      return cursor.toList();
    }
    catch (Exception e) {
      throw new RemoteException("remote toList failed", e);
    }
  }
  
  public boolean updateObjectOnly(DbObject object) throws RemoteException {
    try {
      return cursor.updateObjectOnly(object);
    }
    catch (Exception e) {
      throw new RemoteException("remote updateObjectOnly failed", e);
    }
  }
  
  public boolean updateObject(DbObject object) throws RemoteException {
    try {
      return cursor.updateObject(object);
    }
    catch (Exception e) {
      throw new RemoteException("remote updateObject failed", e);
    }
  }
  
  public boolean updateObjectAt(DbObject object, int row) throws RemoteException {
    try {
      return cursor.updateObjectAt(object, row);
    }
    catch (Exception e) {
      throw new RemoteException("remote updateObjectAt failed", e);
    }
  }
  
  public void setFetchSize(int rows) throws RemoteException {
    try {
      cursor.setFetchSize(rows);
    }
    catch (Exception e) {
      throw new RemoteException("remote setFetchSize failed", e);
    }
  }
  
  public int getFetchSize() throws RemoteException {
    try {
      return cursor.getFetchSize();
    }
    catch (Exception e) {
      throw new RemoteException("remote getFetchSize failed", e);
    }
  }
  
  public void setFetchDirection(int direction) throws RemoteException {
    try {
      cursor.setFetchDirection(direction);
    }
    catch (Exception e) {
      throw new RemoteException("remote setFetchDirection failed", e);
    }
  }
  
  public int getFetchDirection() throws RemoteException {
    try {
      return cursor.getFetchDirection();
    }
    catch (Exception e) {
      throw new RemoteException("remote getFetchDirection failed", e);
    }
  }
  
  public List<? extends DbObject> fetch() throws RemoteException {
    try {
      return cursor.fetch();
    }
    catch (Exception e) {
      throw new RemoteException("remote fetch failed", e);
    }
  }
  
}
