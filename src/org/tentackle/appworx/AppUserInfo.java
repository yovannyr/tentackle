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

// $Id: AppUserInfo.java 438 2008-09-16 14:40:15Z harald $
// Created on November 21, 2003, 9:44 AM

package org.tentackle.appworx;

import org.tentackle.db.Db;
import org.tentackle.db.UserInfo;

/**
 * Extended {@link UserInfo} for applications.
 * <p>
 * Adds:
 * <ul>
 * <li>a reference to the {@link SecurityManager}</li>
 * <li>an object ID for the user (usually implemented as an {@link AppDbObject})</li>
 * </ul>
 *
 * @author harald
 */
public class AppUserInfo extends UserInfo {
  
  private static final long serialVersionUID = -3462863252064547769L;
  
  // current security manager. The default is a disabled one
  private transient SecurityManager securityManager = new SecurityManager();
  
  // ID of the AppDbObject representing the current user
  private long userId = 0;
  
  
  
  /**
   * Creates an application user info.
   * 
   * @param username is the name of the user, null if {@code System.getProperty("user.name")}
   * @param password is the password, null if none
   * @param dbPropertiesBaseName is the resource bundle basename of the db-property file, null if {@code "Db"}
   */
  public AppUserInfo (String username, char[] password, String dbPropertiesBaseName)  {
    super(username, password, dbPropertiesBaseName);
  }

  
  
  /**
   * Creates an application user info from another user info.
   * @param ui the userinfo to copy
   */
  public AppUserInfo(UserInfo ui) {
    super(ui.getUsername(), ui.getPassword(), ui.getDbPropertiesName());
  }
  
  
  /**
   * Creates an application user info.
   * Constructor mainly used in servers where there are
   * no password and no db-properties since the server
   * uses a connection pool.
   * 
   * @param username
   * @param userId
   * @param securityManager 
   */
  public AppUserInfo(String username, long userId, SecurityManager securityManager) {
    super(username, null, null);
    setUserId(userId);
    setSecurityManager(securityManager);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Cloned AppUserInfo always get a disabled {@link SecurityManager} assigned
   * (if there was any assigned at all).
   * This makes sure that cloned user infos assigned to different {@link Db}
   * do not accidently use a different Db-connection for updating the security rules.
   * Applications that need a security manager on a cloned userinfo must set
   * the security manager explicitly.
   */
  @Override
  public AppUserInfo clone() {
    AppUserInfo ui = (AppUserInfo)super.clone();
    if (securityManager != null) {
      ui.securityManager = new SecurityManager();
    }
    return ui;
  }
  

  /**
   * Gets the security manager.
   * 
   * @return the security manager
   */
  public SecurityManager getSecurityManager() {
    return securityManager;
  }
  
  /**
   * Sets the security manager.<br>
   * 
   * @param securityManager the security manager
   * @see Application
   */
  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }
  
  
  
  /**
   * Gets the user id.
   * 
   * @return the object Id of the current user
   */
  public long getUserId() {
    return userId;
  }  
  
  /** 
   * Sets the user id.
   * 
   * @param userId the ID representing the AppDbObject of the current user
   */
  public void setUserId(long userId) {
    this.userId = userId;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the Db in the SecurityManager.
   * 
   * @param db the db connection 
   */
  @Override
  public void bindDb(Db db) {
    super.bindDb(db);
    if (securityManager != null) {
      ContextDb contextDb = securityManager.getContextDb();
      if (contextDb != null) {
        contextDb.setDb(db);
      }
    }
  }
  
}
