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

// $Id: AppDbTreeObject.java 464 2009-07-18 19:02:05Z harald $

package org.tentackle.appworx;

import javax.swing.ImageIcon;

/**
 * Object wrapper associated to a node in an {@link AppDbObjectTree}.
 *
 * @author harald
 */
public class AppDbTreeObject {

  private Object  object;                     // the node's object
  private Object  parentObject;               // the parent object in the tree
  private boolean isAppDbObject;              // true if object is an AppDbObject
  private boolean isAppDbObjectTreeExtension; // true if object is an AppDbObjectTreeExtension
  private boolean expanded;                   // true if all referenced objects loaded
  private boolean stopTreeWillExpand;         // true = stop expanding the tree at this node (in expansionlistener)
  private boolean stopExpandPath;             // true = stop expansion if in doExpandPath()
  private ImageIcon icon;                     // != null if use this icon instead of default
  private String toolTipText;                 // cached tooltiptext
  private boolean toolTipTextLoaded;
  private String treeText;                    // cached toString()
  private boolean treeTextLoaded;
  
  

  /**
   * Creates a tree object.
   * 
   * @param object the data object wrapped by this tree object
   * @param parentObject the parent object of the given object, null = none
   */
  public AppDbTreeObject(Object object, Object parentObject)  {
    this.parentObject = parentObject;
    setObject(object);
  }
  
  
  /**
   * Gets the parent object.
   * 
   * @return the parent object, null = none
   */
  public Object getParentObject() {
    return parentObject;
  }
  
  
  @Override
  public boolean equals(Object obj) {
    try {
      return obj instanceof AppDbTreeObject &&
             object.equals(((AppDbTreeObject)obj).object);
    }
    catch (Exception e) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + (this.object != null ? this.object.hashCode() : 0);
    return hash;
  }
  
  
  /**
   * Gets the icon.
   * 
   * @return the icon, null if none
   */
  public ImageIcon getIcon()  {
    return icon;
  }

  
  /**
   * Sets the icon.
   * 
   * @param icon the icon, null if none
   */
  public void setIcon(ImageIcon icon) {
    this.icon = icon;
  }
  
  
  
  /**
   * Gets the text for the tooltip.
   * 
   * @return the tooltip, null if none
   */
  public String getToolTipText()  {
    if (toolTipTextLoaded == false) {
      toolTipText = null;
      if (isAppDbObject)  {
        toolTipText = ((AppDbObject)object).getToolTipText(parentObject);
      }
      else if (isAppDbObjectTreeExtension) {
        toolTipText = ((AppDbObjectTreeExtension)object).getToolTipText();
      }
      toolTipTextLoaded = true;
    }
    if (toolTipText != null && toString() != null)  {
      if (treeText.equals(toolTipText)) {
        // show tooltip only if more info than ordinary treetext
        return null;     
      }
    }
    return toolTipText;
  }
  
  
  
  @Override
  public String toString()  {
    if (treeTextLoaded == false)  {
      if (isAppDbObject) {
        treeText = ((AppDbObject)object).getTreeText(parentObject);
      }
      else {
        try {
          treeText = object.toString();
        }
        catch (Exception e2)  {
          treeText = object == null ? "<NULL>" : ("<" + e2.getMessage() + ">");
        }
      }
      treeTextLoaded = true;
    }
    return treeText;
  }
  
  
  
  /**
   * Gets the expanded state.
   * 
   * @return true if node is expanded
   */
  public boolean isExpanded() {
    return expanded;
  }
  
  /**
   * Sets the expanded state.
   * 
   * @param expanded true if node is expanded
   */
  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }
  
  
  /**
   * Gets the object wrapped by this tree object.
   * 
   * @return the wrapped object
   */
  public Object getObject() {
    return object;
  }
  
  /**
   * Sets the object wrapped by this tree object.
   * 
   * @param object the wrapped object
   */
  public void setObject(Object object) {
    this.object = object;
    toolTipTextLoaded = false;
    treeTextLoaded = false;
    isAppDbObject = object instanceof AppDbObject;
    if (isAppDbObject)  {
      isAppDbObjectTreeExtension = false;
      stopExpandPath = ((AppDbObject)object).stopTreeExpansion();
      icon = ((AppDbObject)object).getIcon();
    }
    else  {
      isAppDbObjectTreeExtension = object instanceof AppDbObjectTreeExtension;
      if (isAppDbObjectTreeExtension) {
        icon = ((AppDbObjectTreeExtension)object).getIcon();
      }
      // else: use default icon of tree
    }
  }
  

  /** 
   * Returns whether this object's node should be further expanded.
   * 
   * @return true to stop expansion
   */
  public boolean isStopTreeWillExpand() {
    return stopTreeWillExpand;
  }
  
  /** 
   * Defines whether this object's node should be further expanded.
   * 
   * @param stopExpansion true to stop expansion
   */
  public void setStopTreeWillExpand(boolean stopExpansion) {
    this.stopTreeWillExpand = stopExpansion;
  }
  
  
  /**
   * Returns whether {@code AppDbObjectTree.doExpandPath} should stop expansion (not treeWillExpand).
   * The user will still be able to expand the node by clicking on it!
   * 
   * @return true if stop expansion
   * @see AppDbObjectTree#doExpandPath(int, int, org.tentackle.appworx.AppDbObject, javax.swing.tree.TreePath) 
   */
  public boolean isStopExpandPath() {
    return stopExpandPath;
  }

  /**
   * Sets whether {@code AppDbObjectTree.doExpandPath} should stop expansion (not treeWillExpand).
   * The user will still be able to expand the node by clicking on it!
   * 
   * @param stopExpandPath true if stop expansion
   * @see AppDbObjectTree#doExpandPath(int, int, org.tentackle.appworx.AppDbObject, javax.swing.tree.TreePath) 
   */
  public void setStopExpandPath(boolean stopExpandPath) {
    this.stopExpandPath = stopExpandPath;
  }

  
  /**
   * Gets the text displayed in the tree for this object.
   * 
   * @return the tree text
   */
  public String getTreeText() {
    return treeText;
  }

  /**
   * Sets the text displayed in the tree for this object.
   * 
   * @param treeText  the tree text
   */
  public void setTreeText(String treeText) {
    this.treeText = treeText;
    treeTextLoaded = true;
  }
  
}