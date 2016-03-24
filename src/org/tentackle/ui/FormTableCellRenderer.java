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

// $Id: FormTableCellRenderer.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import org.tentackle.util.BMoney;
import org.tentackle.util.StringHelper;


/**
 * Cell Renderer in FormTables.
 *
 * @author harald
 */
public class FormTableCellRenderer extends DefaultTableCellRenderer {
        
  // the borders
  private Border focusBorder;
  
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
   * Creates a table cell renderer.
   */
  public FormTableCellRenderer() {
    super();
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
   * {@inheritDoc}
   * <p>
   * Ovewritten due to formatting.
   */
  @Override
  public Component getTableCellRendererComponent (JTable table,
              Object value, boolean isSelected, boolean hasFocus,
              int row, int column)  {



    if (table instanceof FormTable) {

      FormTable formTable = (FormTable)table;
      int modelColumn = formTable.convertColumnIndexToModel(column);
      
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

      // set the default value
      setValue(value);
      
      int hAlign = JLabel.LEFT;
      int vAlign = JLabel.CENTER;

      try {

        if (value instanceof Number) {

          hAlign = SwingConstants.TRAILING;

          if (formTable.isBlankZero(modelColumn)) {
            boolean blankZero = false;
            if      (value instanceof Double) {
              if (((Double)value).doubleValue() == 0.0d) {
                blankZero = true;
              }
            }
            else if (value instanceof Float)  {
              if (((Float)value).floatValue() == 0.0f) {
                blankZero = true;
              }
            }
            else if (value instanceof BMoney) {
              if (((BMoney)value).isZero()) {
                blankZero = true;
              }
            }
            else if (value instanceof Number) {
              if (((Number)value).longValue() == 0l) {
                blankZero = true;
              }
            }

            if (blankZero)  {
              setText("");
              return this;    // do the rest of formatting in the finally clause
            }
          }

          if (value instanceof BMoney) {
            String fmt = formTable.getFormat(modelColumn);
            if (fmt == null)  {
              formTable.setFormat(modelColumn, StringHelper.moneyPattern);
            }
            DecimalFormat format = (DecimalFormat)formTable.getNumberFormat(modelColumn);
            FormHelper.setScale(format, ((BMoney)value).scale());
            setText(format.format(value));
            if (fmt == null) {
              formTable.setFormat(modelColumn, format.toPattern());
            }
          }
          else  {
            if (value instanceof Float || value instanceof Double)  {
              String fmt = formTable.getFormat(modelColumn);
              if (fmt == null)  {
                formTable.setFormat(modelColumn, StringHelper.floatDoublePattern); 
              }
            }
            setText (value == null ? StringHelper.emptyString : formTable.getNumberFormat(modelColumn).format((Number)value));
          }
          return this;
        }
        else  {
          // not a number
          if (value instanceof Timestamp) {
            hAlign = JLabel.CENTER;
            setText (value == null ? StringHelper.emptyString : formTable.getDateFormat(modelColumn, true).format((Date)value));            
          }
          else if (value instanceof Date)  {
            hAlign = JLabel.CENTER;
            setText (value == null ? StringHelper.emptyString : formTable.getDateFormat(modelColumn).format((Date)value));
          }
          return this;
        }
      }

      finally {
        // do some other formatting stuff
        switch (formTable.getConvert(modelColumn)) {
        case FormField.CONVERT_LC:  setText(getText().toLowerCase());
                                    break;
        case FormField.CONVERT_UC:  setText(getText().toUpperCase());
                                    break;
        }
        int alignment = formTable.getHorizontalAlignment(modelColumn);
        if (alignment != -1 && alignment != hAlign) {
          hAlign = alignment;
        }
        // if not default alignment
        setHorizontalAlignment(hAlign);
        
        alignment = formTable.getVerticalAlignment(modelColumn);
        if (alignment != -1 && alignment != vAlign) {
          vAlign = alignment;
        }
        // if not default alignment
        setVerticalAlignment(vAlign);        
      }
    }
    
    else  { // not a formtable
      // do the default jobs of setting the colors, etc...
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                          row, column);      
    }

    return this;
  }

}