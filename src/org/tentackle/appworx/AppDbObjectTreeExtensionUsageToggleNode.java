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

// $Id: AppDbObjectTreeExtensionUsageToggleNode.java 351 2008-06-08 16:59:36Z harald $

package org.tentackle.appworx;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import org.tentackle.plaf.PlafGlobal;


/**
 * A toggle node to display the "usage of..." an {@link AppDbObject}.
 *
 * @author harald
 */
public class AppDbObjectTreeExtensionUsageToggleNode extends AppDbObjectTreeExtensionToggleNode {
  
  public static final int TOGGLENODEID_TREEUSAGE = 1;
  
  private static final String TEXT_SHOW_TREEUSGE = Locales.bundle.getString("referenced_by_...");
  private static final String TEXT_HIDE_TREEUSGE = Locales.bundle.getString("collapse_references");
  
  
  private JMenuItem inUseItem;    // the toggle menu item 
  
  
  
  /**
   * Creates a usage toggle node
   */
  public AppDbObjectTreeExtensionUsageToggleNode() {
    super(TOGGLENODEID_TREEUSAGE);
    inUseItem = new JMenuItem(new AbstractAction("treeusage") {   // NOI18N
      public void actionPerformed(ActionEvent e)  {
        if (inUseItem.getText().equals(TEXT_HIDE_TREEUSGE)) {
          getTree().hideInUseTree(getChildIndex());
        }
        else  {
          getTree().showInUseTree();
        }
      }
    });
  }

  
  public JMenuItem getMenuItem(boolean toggleNodeDisplayed) {
    inUseItem.setText(toggleNodeDisplayed ? TEXT_HIDE_TREEUSGE : TEXT_SHOW_TREEUSGE);
    return inUseItem;
  }

  
  public AppDbTreeToggleNodeObject getToggleNodeObject(Object popupObject) {
    return new AppDbTreeToggleNodeObject(
            TOGGLENODEID_TREEUSAGE,
            MessageFormat.format(Locales.bundle.getString("Usage_of_{0}"), ((AppDbObject)popupObject).getTreeText()),
            PlafGlobal.getIcon("treeusage"));
  }

}
