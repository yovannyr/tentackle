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

// $Id: FormTextAreaTableCellRenderer.java 336 2008-05-09 14:40:20Z harald $
// Created on October 4, 2002, 6:54 PM

package org.tentackle.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.Serializable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;





/**
 * A multi line text area table cell renderer.
 *
 * @author harald
 */
public class FormTextAreaTableCellRenderer extends FormTextArea
              implements TableCellRenderer, Serializable    {
  
  // the borders
  private Border focusBorder;
  private static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 
  
  // flag to adjust the rowHeight of the JTable automatically
  private boolean autoRowHeight;
  
  // the display font
  private Font  font;
  
  // colors (will be retrieved from formtable)
  private Color selectedForeground; 
  private Color selectedBackground;
  private Color unselectedForeground; 
  private Color unselectedBackground;
  private Color focusedForeground; 
  private Color focusedBackground; 

  
  
  
  /**
   * Creates a text area table cell renderer.
   */
  public FormTextAreaTableCellRenderer() {
    super();
    setLineWrap(true);
    setWrapStyleWord(true);
    setAutoRowHeight(true);
    setOpaque(true);
    setCellEditorUsage(true);
    setSmartEnter(false);
    setEditable(false);
    setBorder(noFocusBorder);
    focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
  }
    
  
  /**
   * Sets the color for the selected foreground.
   * @param c the foreground color
   */
  public void setSelectedForeground(Color c) {
    selectedForeground = c; 
  }
  
  /**
   * Gets the color for the selected foreground.
   * @return the foreground color
   */
  public Color getSelectedForeground()  {
    return selectedForeground;
  }

  /**
   * Sets the color for the selected background.
   * @param c the background color
   */
  public void setSelectedBackground(Color c) {
    selectedBackground = c; 
  }
  
  /**
   * Gets the color for the selected background.
   * @return the background color
   */
  public Color getSelectedBackground()  {
    return selectedBackground;
  }
  
  
  /**
   * Sets the color for the unselected foreground.
   * @param c the unselected foreground color
   */
  public void setUnselectedForeground(Color c) {
    unselectedForeground = c; 
  }
  
  /**
   * Gets the color for the unselected foreground.
   * @return the unselected foreground color
   */
  public Color getUnselectedForeground()  {
    return unselectedForeground;
  }

  /**
   * Sets the color for the unselected background.
   * @param c the unselected background color
   */
  public void setUnselectedBackground(Color c) {
    unselectedBackground = c; 
  }
  
  /**
   * Gets the color for the unselected background.
   * @return the unselected background color
   */
  public Color getUnselectedBackground()  {
    return unselectedBackground;
  }
  
  
  /**
   * Sets the color for the focused foreground.
   * @param c the focused foreground color
   */
  public void setFocusedForeground(Color c) {
    focusedForeground = c; 
  }
  
  /**
   * Gets the color for the focused foreground.
   * @return the focused foreground color
   */
  public Color getFocusedForeground()  {
    return focusedForeground;
  }

  /**
   * Sets the color for the focused background.
   * @param c the focused background color
   */
  public void setFocusedBackground(Color c) {
    focusedBackground = c; 
  }
  
  /**
   * Gets the color for the focused background.
   * @return the focused background color
   */
  public Color getFocusedBackground()  {
    return focusedBackground;
  }
  
  
  /**
   * Sets the rendering default font.
   * @param font the font
   */
  public void setRenderingFont(Font font)  {
    this.font = font;
  }
  
  /**
   * Gets the rendering default font.
   * @return the font
   */
  public Font getRenderingFont() {
    return font;
  }
    
  

      
  /**
   * Enables or disables automatic adjustment of the row height.
   * 
   * @param autoRowHeight true if enabled
   */
  public void setAutoRowHeight(boolean autoRowHeight)  {
    this.autoRowHeight = autoRowHeight;
  }

  /**
   * Returns whether automatic adjustment of the row height is enabled.
   * 
   * @return true if enabled
   */
  public boolean isAutoRowHeight() {
    return autoRowHeight;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to assign
   * the unselected-foreground color to the specified color.
   */
  @Override
  public void setForeground(Color c) {
    super.setForeground(c); 
    unselectedForeground = c; 
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to assign
   * the unselected-background color to the specified color.
   */
  @Override
  public void setBackground(Color c) {
    super.setBackground(c); 
    unselectedBackground = c; 
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to clear
   * the background anf foreground colors.
   */
  @Override
  public void updateUI() {
    super.updateUI(); 
    setForeground(null);
    setBackground(null);
  }


  
  public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
      
    if (table instanceof FormTable) {

      FormTable formTable = (FormTable)table;      
      // set the colors, if not default
      if (isSelected) {
        super.setForeground(selectedForeground == null ? formTable.getSelectedForeground() : selectedForeground);
        super.setBackground(selectedBackground == null ? formTable.getSelectedBackground() : selectedBackground);
      }
      else {
        super.setForeground(unselectedForeground == null ? formTable.getUnselectedForeground() : unselectedForeground);
        super.setBackground(unselectedBackground == null ? formTable.getUnselectedBackground() : unselectedBackground);
      }

      super.setFont(font == null ? table.getFont() : font);

      if (hasFocus) {
        setBorder(focusBorder);
        if (table.isCellEditable(row, column)) {
            super.setForeground(focusedForeground == null ? formTable.getFocusedForeground() : focusedForeground);
            super.setBackground(focusedBackground == null ? formTable.getFocusedBackground() : focusedBackground);
        }
      } else {
        setBorder(noFocusBorder);
      }
    }
    
    else  {
      
      if (isSelected) {
         super.setForeground(table.getSelectionForeground());
         super.setBackground(table.getSelectionBackground());
      }
      else {
          super.setForeground((unselectedForeground != null) ? unselectedForeground 
                                                             : table.getForeground());
          super.setBackground((unselectedBackground != null) ? unselectedBackground 
                                                             : table.getBackground());
      }

      setFont(table.getFont());

      if (hasFocus) {
          setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
          if (table.isCellEditable(row, column)) {
              super.setForeground( UIManager.getColor("Table.focusCellForeground") );
              super.setBackground( UIManager.getColor("Table.focusCellBackground") );
          }
      } else {
          setBorder(noFocusBorder);
      }      
    }


    setFormValue(value);

    // fix for JRE 1.4: sets the width in order to allow getPreferredSize() to work
    if (getWidth() == 0)  {
      // get the width of the table-column
      int width = table.getColumnModel().getColumn(column).getWidth();
      // set the width with some reasonable height. We use the current row-height
      setSize(width, table.getRowHeight(row));
    }

    if (autoRowHeight)  {
      int height = getPreferredSize().height;
      // make row height large enough.
      // the "<" instead of "!=" will enlarge the row to the largest
      // column (if more than one FormTextAreaTableCellRenderer in a table)
      if (table != null && table.getRowHeight(row) < height)  {
        table.setRowHeight(row, height);
      }
    }

    return this;
  }

  
  
  
  

  /**
   * Overridden for performance reasons.
   */
  @Override
  public void validate() {}

  /**
   * Overridden for performance reasons.
   */
  @Override
  public void revalidate() {}

  /**
   * Overridden for performance reasons.
   */
  @Override
  public void repaint(long tm, int x, int y, int width, int height) {}

  /**
   * Overridden for performance reasons.
   */
  @Override
  public void repaint(Rectangle r) {}


}


