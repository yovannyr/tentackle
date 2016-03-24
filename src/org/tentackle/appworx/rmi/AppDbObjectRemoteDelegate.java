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

// $Id: AppDbObjectRemoteDelegate.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.appworx.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.List;
import org.tentackle.appworx.AppDbObject;
import org.tentackle.appworx.ContextDb;
import org.tentackle.appworx.QbfParameter;
import org.tentackle.db.rmi.DbObjectRemoteDelegate;
import org.tentackle.db.rmi.RemoteDbCursor;



/**
 * Remote delegate for {@link AppDbObject}.
 * 
 * @author harald
 */
public interface AppDbObjectRemoteDelegate extends DbObjectRemoteDelegate {
  
  /**
   * Holds the "edited-by"-token and a success-code.<br>
   * Return value for {@code updateEditedBy()} and {@code transferEditedBy()}.
   */
  public static class BeingEditedToken implements Serializable {
    
    private static final long serialVersionUID = 438202973417850958L;
    
    /** object ID of the user holding the token, 0 = unlocked **/
    public long editedBy;
    /** timestamp when editing started **/
    public Timestamp editedSince;
    /** timestamp when token will expire **/
    public Timestamp editedExpiry;
    /** true if success, false if operation failed **/
    public boolean success;
  }
  
  
  public AppDbObject select(ContextDb cb, long id, boolean withLinkedObjects) throws RemoteException;
  
  public AppDbObject selectLocked(ContextDb cb, long id, boolean withLinkedObjects) throws RemoteException;
  
  public List<? extends AppDbObject> selectAll(boolean withLinkedObjects) throws RemoteException;
  
  public List<? extends AppDbObject> selectByNormText(ContextDb cb, String normText) throws RemoteException;
  
  public RemoteDbCursor selectByNormTextCursor(ContextDb cb, String normText) throws RemoteException;
  
  public List<? extends AppDbObject> selectAllInContext(ContextDb cb) throws RemoteException;
  
  public List<? extends AppDbObject> selectAllInContextFromServerCache(ContextDb cb) throws RemoteException;
  
  public RemoteDbCursor selectAllInContextCursor(ContextDb cb) throws RemoteException;
  
  public QbfCursorResult searchQbfCursor(QbfParameter par) throws RemoteException;
  
  public List<Object> getTreeParentObjects(AppDbObject obj) throws RemoteException;
  
  public List<Object> getTreeParentObjects(AppDbObject obj, Object parentObject) throws RemoteException;
  
  public List<Object> getTreeChildObjects(AppDbObject obj) throws RemoteException;
  
  public List<Object> getTreeChildObjects(AppDbObject obj, Object parentObject) throws RemoteException;
  
  public boolean isReferenced(ContextDb cb, long id) throws RemoteException;
  
  public AppDbObjectRemoteDelegate.BeingEditedToken updateEditedBy(long id, Timestamp tokenExpiry, long userId, Timestamp curTime) throws RemoteException;
  
  public AppDbObjectRemoteDelegate.BeingEditedToken transferEditedBy(long id) throws RemoteException;

  public boolean updateEditedByOnly(long id, long editedBy, Timestamp editedSince, Timestamp editedExpiry) throws RemoteException;

}
