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

// $Id: AppDbTreeToggleNodeObject.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.appworx;

import javax.swing.ImageIcon;

/**
 * Extended {@link AppDbTreeObject} for displaying subtrees as childnodes
 * in an {@link AppDbObjectTree}.
 *
 * @author harald
 */
public class AppDbTreeToggleNodeObject extends AppDbTreeObject {

  private int toggleNodeId;   // the unique toggle node id
  
  
  /**
   * Creates a toggle node object.
   * 
   * @param toggleNodeId the unique toggle node id
   * @param text some text which is displayed for the node, null = none
   * @param icon the icon for the node, null = none
   */
  public AppDbTreeToggleNodeObject(int toggleNodeId, String text, ImageIcon icon)  {
    super(text, null);
    setIcon(icon);
    this.toggleNodeId = toggleNodeId;
  }
  
  
  /**
   * Gets the toggle node id.
   * 
   * @return the unique toggleNodeId
   */
  public int getToggleNodeId() {
    return toggleNodeId;
  }
  
  
  /**
   * Gets the tooltip text.<br>
   * The default implementation returns null.
   * 
   * @return the tooltip
   */
  @Override
  public String getToolTipText()  {
    return null;
  }
  
  
  /** 
   * Gets the object.<br>
   * The default implementation returns "this".
   */
  @Override
  public Object getObject() {
    return this;
  }
  
}