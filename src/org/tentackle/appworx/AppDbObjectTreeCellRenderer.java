/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tentackle.appworx;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;



/**
 * The default renderer for an {@link AppDbObjectTree}.
 * @see AppDbObjectTree#createDefaultRenderer
 * @author harald
 */
public class AppDbObjectTreeCellRenderer extends DefaultTreeCellRenderer {
  
  AppDbTreeObject tobj;   // != null if node holds an AppDbTreeObject


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to determine the {@link AppDbTreeObject} and the icon.
   * 
   * @param tree the tree
   * @param value the object to render
   * @param selected true if node is selected
   * @param expanded true if node is expanded
   * @param leaf true if node is a leaf
   * @param row the row number in the tree
   * @param hasFocus true if tree has focus
   * @return the component (i.e. "this")
   */
  @Override
  public Component getTreeCellRendererComponent (JTree tree, Object value,
                  boolean selected, boolean expanded, boolean leaf,
                  int row, boolean hasFocus)  {

    super.getTreeCellRendererComponent(tree, value, selected, expanded,
                                       leaf, row, hasFocus);

    tobj = null;
    ImageIcon icon = null;

    if (value != null &&
        value instanceof DefaultMutableTreeNode)  {
      Object obj = ((DefaultMutableTreeNode)value).getUserObject();
      if (obj != null && obj instanceof AppDbTreeObject) {
        tobj = (AppDbTreeObject)obj;
        icon = tobj.getIcon();
      }
    }

    setIcon(icon);
    return this;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to determine the tooltip from the {@link AppDbObject}.
   */
  @Override
  public String getToolTipText()  {
    return tobj != null ? tobj.getToolTipText() : null;
  }

}
