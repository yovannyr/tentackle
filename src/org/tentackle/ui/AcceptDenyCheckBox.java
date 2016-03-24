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

// $Id: AcceptDenyCheckBox.java 440 2008-09-23 16:28:35Z harald $
// Created on September 15, 2002, 2:45 PM

package org.tentackle.ui;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.tentackle.plaf.PlafGlobal;




/**
 * A simple checkbox for ok/yes/allow (green) and cancel/no/deny (red).
 * 
 * @author harald
 */
public class AcceptDenyCheckBox extends FormCheckBox {
  
  
  private boolean invert;     // invert boolean value
  
  
  /**
   * Creates an AcceptDenyCheckBox.
   * 
   * @param invert true to invert the logic 
   */
  public AcceptDenyCheckBox(boolean invert) {
    setInvert(invert);
    setIcon(PlafGlobal.getIcon("cancel_mini"));
    setSelectedIcon(PlafGlobal.getIcon("ok_mini"));
  }
  
  /**
   * Creates an AcceptDenyCheckBox.
   */
  public AcceptDenyCheckBox() {
    this(false);
  }
  
  
  
  /**
   * Returns whether the logic is inverted.
   * 
   * @return true if inverted
   */
  public boolean isInvert() {
    return invert;
  }

  /**
   * Sets the inversion of the logic.
   * 
   * @param invert true to invert the logic, false is default
   */
  public void setInvert(boolean invert) {
    this.invert = invert;
  }

  
  
  
  /**
   * Creates a cell renderer using an AcceptDenyCheckBox.
   * 
   * @param invert true to invert the logic
   * @return the cell renderer
   */
  public static TableCellRenderer getTableCellRenderer(boolean invert) {
    return new BooleanTableCellRenderer(invert);
  }
  
  /**
   * Creates a cell renderer using an AcceptDenyCheckBox.
   * 
   * @return the cell renderer
   */
  public static TableCellRenderer getTableCellRenderer() {
    return getTableCellRenderer(false);
  }
  

  /**
   * Creates a table cell editor using an AcceptDenyCheckBox.
   * 
   * @param invert true to invert the logic
   * @return the cell renderer
   */
  public static TableCellEditor getTableCellEditor(boolean invert) {
    return new FormComponentCellEditor(new AcceptDenyCheckBox(invert));
  }
  
  /**
   * Creates a table cell editor using an AcceptDenyCheckBox.
   * 
   * @return the cell renderer
   */
  public static TableCellEditor getTableCellEditor() {
    return getTableCellEditor(false);
  }



  // Boolean cell renderer

  private static class BooleanTableCellRenderer extends DefaultTableCellRenderer {
            
    private boolean invert;
    private Icon cancelIcon;
    private Icon okIcon;
    
    BooleanTableCellRenderer(boolean invert) {
      super();
      this.invert = invert;
      cancelIcon = PlafGlobal.getIcon("cancel_mini");
      okIcon = PlafGlobal.getIcon("ok_mini");
      setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent (JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column)  {

      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setText("");
      setIcon((value != null && ((Boolean)value).booleanValue()) ^ invert ? okIcon : cancelIcon);
      return this;
    }

  }

  
}
