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

// $Id: DbObjectRemoteDelegate.java 439 2008-09-18 15:09:32Z harald $


package org.tentackle.db.rmi;

import org.tentackle.db.DbObject;
import java.rmi.RemoteException;
import java.util.List;



/**
 * Delegate for the DbObject class.<br>
 * Notice: in this class we cut off generics!
 * The reason is DbObject.getRemoteDelegate() which can't
 * be made generic without making DbObject generic.
 * This would end up with such silly things like "new Customer&lt;Customer&gt;()",
 * because java provides no runtime type information of generic type parameters.
 *
 * @author harald
 */
public interface DbObjectRemoteDelegate extends RemoteDelegate {

  public DbObject select(long id, boolean withLinkedObjects) throws RemoteException;
  
  public DbObject selectLocked(long id, boolean withLinkedObjects) throws RemoteException;
  
  public DbObject selectInValidContext(long id, boolean withLinkedObjects) throws RemoteException;
  
  public List<? extends DbObject> selectAll(boolean withLinkedObjects) throws RemoteException;
  
  public long selectSerial(long id) throws RemoteException;

  public boolean deletePlain(long id, long serial) throws RemoteException;
  
  public boolean insertPlain(DbObject obj) throws RemoteException;
  
  public boolean updatePlain(DbObject obj) throws RemoteException;
  
  public boolean dummyUpdate(DbObject obj) throws RemoteException;
  
  public boolean updateSerial(long id, long serial) throws RemoteException;
  
  public boolean updateTableSerial(long id) throws RemoteException;
  
  public boolean updateSerialAndTableSerial(long id, long serial, long tableSerial) throws RemoteException;
  
  public long[] selectExpiredTableSerials(long oldSerial) throws RemoteException;
  
  public long[] selectExpiredTableSerials(long oldSerial, long maxSerial) throws RemoteException;
  
  public long[] getExpirationBacklog(long minSerial, long maxSerial) throws RemoteException;
  
  public long[] getExpiredTableSerials(long oldSerial, long maxSerial) throws RemoteException;
  
  public DbObjectResult insertObject(DbObject obj, boolean withLinkedObjects) throws RemoteException;
  
  public DbObjectResult updateObject(DbObject obj, boolean withLinkedObjects) throws RemoteException;
  
  public DbObjectResult save(DbObject obj) throws RemoteException;
  
  public DbObjectResult deleteObject(DbObject obj, boolean withLinkedObjects) throws RemoteException;
  
  public long selectModification() throws RemoteException;

  RemoteDbCursor selectAllCursor(boolean withLinkedObjects) throws RemoteException;
  
  public boolean isReferenced(long id) throws RemoteException;
}
