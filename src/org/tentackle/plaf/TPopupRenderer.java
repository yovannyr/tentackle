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

// $Id: TPopupRenderer.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.plaf;

import org.tentackle.util.ShortLongText;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serializable;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * Special PopupRenderer that renders two columns according
 * to {@code ShortLongText} objects.
 *
 * @author harald
 * @see org.tentackle.ui.FormComboBox#setShortLongPopupEnabled(boolean) 
 */
public class TPopupRenderer extends JPanel implements ListCellRenderer, Serializable {
  
    
    private JLabel leftLabel;
    private JLabel rightLabel;

    private int minLeftWidth;
    private int minRightWidth;
    
    
    /**
     * Creates a popup renderer.
     */
    public TPopupRenderer() {
      super();
      setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));
      
      leftLabel = new JLabel() {
      @Override
        public Dimension getPreferredSize() {
          Dimension d = super.getPreferredSize();
          if (d.width < minLeftWidth) {
            d.width = minLeftWidth;
          }
          return d;
        }
      };
      
      rightLabel = new JLabel() {
      @Override
        public Dimension getPreferredSize() {
          Dimension d = super.getPreferredSize();
          if (d.width < minRightWidth) {
            d.width = minRightWidth;
          }
          return d;
        }
      };
      
      add(leftLabel);
      add(rightLabel);
    }
    
    
    
    public void setMinLeftWidth(int width) {
      minLeftWidth = width;
    }
    
    public void setMinRightWidth(int width) {
      minRightWidth = width;
    }
    
    public int getLeftWidth() {
      return leftLabel.getPreferredSize().width;
    }
    
    public int getRightWidth()  {
      return rightLabel.getPreferredSize().width;
    }
    
    
    
    public Component getListCellRendererComponent(JList list, 
                                                  Object value,
                                                  int index, 
                                                  boolean isSelected, 
                                                  boolean cellHasFocus)
    {

        if (isSelected) {
          setBackground(list.getSelectionBackground());
          setForeground(list.getSelectionForeground());
        }
        else {
          setBackground(list.getBackground());
          setForeground(list.getForeground());
        }

        leftLabel.setFont(list.getFont());
        leftLabel.setForeground(getForeground());
        rightLabel.setFont(list.getFont());
        rightLabel.setForeground(getForeground());
        
        if (value instanceof ShortLongText) {
          leftLabel.setText(((ShortLongText)value).getShortText());
          rightLabel.setText(((ShortLongText)value).getLongText());
        }
        else  {
          leftLabel.setText((value == null) ? "" : value.toString());
          rightLabel.setText("");
        }

        
        return this;
    }

}
