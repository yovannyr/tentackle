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

// $Id: PermissionComboBox.java 336 2008-05-09 14:40:20Z harald $
// Created on September 13, 2002, 8:22 AM

package org.tentackle.appworx;

import org.tentackle.ui.FormComboBox;


/**
 * ComboBox to edit permissions of {@link Security} rules.
 *
 * @author harald
 */
public class PermissionComboBox extends FormComboBox {
  
  /** the permission type **/
  protected int permissionType;
  
  
  /**
   * Sets the permission type.<br>
   * The permission type determines the allowed permissions.
   * 
   * @param permissionType the permission type
   * @see Security
   */
  public void setPermissionType(int permissionType)  {
    this.permissionType = permissionType;
    removeAllItems();
    if (permissionType == Security.TYPE_PROGRAM) {
      loadProgramItems();
    }
    else if (permissionType == Security.TYPE_DATA) {
      loadDataItems();
    }
  }

  /**
   * Gets the permission type.
   * 
   * @return the permission type
   * @see Security
   */
  public int getPermissionType() {
    return permissionType;
  }  
  
  
  /**
   * Sets the permission for the current permission type.
   * 
   * @param permission the permission
   */
  public void setPermission (int permission)  {
    setFormValue(new Permission(permission));
  }

  /**
   * Gets the permission for the current permission type.
   * 
   * @return the permission
   */
  public int getPermission() {
    Object obj = getFormValue();
    try {
      return ((Permission)obj).permission;
    }
    catch (Exception e) {
      return 0;
    }
  }
  
  
  /**
   * Loads all items of the permission type is {@link Security#TYPE_PROGRAM}.
   */
  protected void loadProgramItems() {
    addItem(new Permission(Security.NONE));
    addItem(new Permission(Security.EXEC));
    setFormValueIndex(-1);
  }
  
  /**
   * Loads all items of the permission type is {@link Security#TYPE_DATA}.
   */
  protected void loadDataItems() {
    addItem(new Permission(Security.NONE));
    addItem(new Permission(Security.READ));
    addItem(new Permission(Security.WRITE));
    addItem(new Permission(Security.READ | Security.WRITE));
    setFormValueIndex(-1);
  }
  
  
  /**
   * Wraps the permission value.
   */
  protected class Permission  {
    private int permission;
    private String text;
    public Permission(int permission) {
      this.permission = permission;
      this.text = Security.permissionToString(permissionType, permission);
    }
    @Override
    public String toString() {
      return text;
    }
    @Override
    public boolean equals(Object obj) {
      try {
        return ((Permission)obj).permission == permission;
      }
      catch (Exception e) {
        return false;
      }
    }
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 13 * hash + this.permission;
      return hash;
    }
  }
  

}
