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

// $Id: AppDbObjectTreeExtensionToggleNode.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.appworx;

import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * A childnode for nodes in an {@link AppDbObjectTree} to add a popup-menu-item 
 * for toggling the display of subtrees.
 *
 * @author harald
 */
public abstract class AppDbObjectTreeExtensionToggleNode {
  
  private int toggleNodeId;                     // the unique toggle-node ID
  private AppDbObjectTree tree;                 // the tree
  private DefaultMutableTreeNode popupNode;     // the popupnode
  private TreePath popupPath;                   // path for popupnode
  private int childIndex;                       // index in child-nodes of popupNode
          
          
  /**
   * Creates a toggle node.
   * <p>
   * Each of toggle node must get its own unique identifier to determine
   * whether it is already displayed in the tree or not.
   * 
   * @param toggleNodeId is some unique(!) id 
   */
  public AppDbObjectTreeExtensionToggleNode(int toggleNodeId) {
    this.toggleNodeId = toggleNodeId;
  }
  
  
  /**
   * Gets the toggle node id.
   * 
   * @return the unique id
   */
  public int getToggleNodeId() {
    return toggleNodeId;
  }
  
  
  /**
   * Gets the tree this toggle node is associated to.
   * 
   * @return the tree
   */
  public AppDbObjectTree getTree() {
    return tree;
  }
  
  
  /**
   * Gets the popup node this toggle node is associated to.
   * 
   * @return the popupNode
   */
  public DefaultMutableTreeNode getPopupNode() {
    return popupNode;
  }
  
  
  /**
   * Gets the index of this toggle-node withing the childs of the popup node.
   * 
   * @return the index 
   */
  public int getChildIndex() {
    return childIndex;
  }
  
  
  /**
   * Creates an AppDbTreeToggleNodeObject to be inserted into the tree.
   * Needs to be implemented!
   *
   * @param popupObject is the object of the popupNode (usually an AppDbObject, but not necessarily and may be null)
   * @return the toggle node object to be inserted as a child of the popupNode
   */
  public abstract AppDbTreeToggleNodeObject getToggleNodeObject(Object popupObject);
  
  
  /**
   * Gets a JMenuItem for the popup-Menu.
   * Needs to be implemented!
   * 
   * @param toggleNodeDisplayed is true if the toggle-node is displayed in the tree, false if not
   * @return the array of menu items, null if none.
   */
  public abstract JMenuItem getMenuItem(boolean toggleNodeDisplayed);
  
  
  /**
   * Creates a JMenuItem for the popup-Menu.
   * 
   * @param tree the tree
   * @param popupNode is the node the popupMenu is displayed for
   * @param popupPath is the treepath for the popupNode
   * @return the menu item
   */
  public JMenuItem getMenuItem(AppDbObjectTree tree, DefaultMutableTreeNode popupNode, TreePath popupPath) {
    
    // save info for inserting and removing the toggle node
    this.tree      = tree;
    this.popupNode = popupNode;
    this.popupPath = popupPath;
    
    /**
     * check if togglenode is already displayed.
     * All togglenodes appear at the beginning of the node's children.
     * So we stop at the first non-togglenode.
     */
    boolean toggleNodeDisplayed = false;
    int childNum = popupNode.getChildCount();
    for (childIndex=0; childIndex < childNum; childIndex++) {
      TreeNode childNode = popupNode.getChildAt(childIndex);
      if (childNode instanceof DefaultMutableTreeNode) {
        Object userObject = ((DefaultMutableTreeNode)childNode).getUserObject();
        if (userObject instanceof AppDbTreeToggleNodeObject) {
          if (((AppDbTreeToggleNodeObject)userObject).getToggleNodeId() == toggleNodeId) {
            toggleNodeDisplayed = true;
            break;
          }
        }
        else  {
          // non toggle node
          break;
        }
      }
    }
    
    return getMenuItem(toggleNodeDisplayed);
  }
  
  
  /**
   * Sets the list of objects into this toggle node and
   * inserts the toggle node into the tree (invokes {@code showToggleNode()}.
   * @param list the list of objects
   */
  public void insertObjects(List list) {
    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    // insert the toggle node
    DefaultMutableTreeNode toggleNode = showToggleNode();
    // insert the list of objects into the toggle node
    if (list != null && list.isEmpty() == false) {
      for (Object obj: list) {
        if (tree.isObjectAppendable(obj))  {
          AppDbTreeObject to = new AppDbTreeObject(obj, null);
          DefaultMutableTreeNode childnode = new DefaultMutableTreeNode(to);
          childnode.setAllowsChildren(
            (obj instanceof AppDbObjectTreeExtension && ((AppDbObjectTreeExtension)obj).allowsTreeChildObjects()) ||
            (obj instanceof AppDbObject && ((AppDbObject)obj).allowsTreeChildObjects()));
          model.insertNodeInto(childnode, toggleNode, toggleNode.getChildCount());
        }
      }
    }
    tree.doExpandPath(0, 1, null, popupPath);    // expand one level if not yet done to show the togglenode
    tree.doExpandPath(0, 1, null, popupPath.pathByAddingChild(toggleNode));   // expand the togglenode 
  }
  
  
  /**
   * Inserts the toggle-node as the first childnode.
   *
   * @return the toggle node
   */
  public DefaultMutableTreeNode showToggleNode() {
    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    Object popupObject = ((AppDbTreeObject)popupNode.getUserObject()).getObject();
    // insert the toggle node
    DefaultMutableTreeNode toggleNode = new DefaultMutableTreeNode(getToggleNodeObject(popupObject));
    model.insertNodeInto(toggleNode, popupNode, 0);   // insert at top of tree
    return toggleNode;
  }
  
  
  /**
   * Removes the togglenode from the tree.
   */
  public void hideToggleNode() {
    ((DefaultTreeModel)tree.getModel()).removeNodeFromParent((DefaultMutableTreeNode)popupNode.getChildAt(childIndex));
  }
  
}
