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

// $Id: AppDbObjectTreeExtensionEditor.java 336 2008-05-09 14:40:20Z harald $
// Created on February 13, 2004, 3:21 PM

package org.tentackle.appworx;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Interface editors in AppDbObjectTreeExtension must implement.
 * The editor must provide a constructor that holds the reference to
 * the object being edited.
 *
 * @author  harald
 */
public interface AppDbObjectTreeExtensionEditor {
  
  /**
   * Invokes the editor on the given object defined by the node.<br>
   * The editor is responsible to update the tree!
   * 
   * @param tree the object tree
   * @param node is the node in the tree for the current object to edit
   * @param modal is true if editor must not return until editing is finished 
   * @param showOnly is true if editor should not allow the object to change 
   */
  public void showEditor(AppDbObjectTree tree, DefaultMutableTreeNode node, boolean modal, boolean showOnly);
  
}
