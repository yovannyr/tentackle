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

// $Id: TTinyComboBoxUI.java 440 2008-09-23 16:28:35Z harald $

package org.tentackle.plaf.tinylaf;

import de.muntjak.tinylookandfeel.TinyComboBoxUI;
import javax.swing.UIManager;
import org.tentackle.plaf.TComboPopup;
import org.tentackle.plaf.TFlasherListener;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.ComboPopup;
import org.tentackle.plaf.TentackleLookAndFeel;


/**
 * UI for JComboBox/FormComboBox/FormFieldComboBox.
 * 
 * @author harald
 */
public class TTinyComboBoxUI extends TinyComboBoxUI {
  
  
  // ********************************
  //        Create PlAF
  // ********************************
  public static ComponentUI createUI(JComponent c) {
      return new TTinyComboBoxUI();    // one per component
  }
    
  @Override
  protected void installListeners() {
    super.installListeners();
    if (!comboBox.isEditable() && ((TentackleLookAndFeel)UIManager.getLookAndFeel()).isFocusAnimated()) {
      comboBox.addFocusListener(new TComboBoxFlasherListener());
    }
  }

  @Override
  protected void uninstallListeners() {
    super.uninstallListeners();
    TFlasherListener.removeFlasherListener(comboBox);
  }

  
  
  /**
   * Overridden to install special popup that recognizes
   * FormComboBoxRenderer.
   *
   * @return an instance of <code>ComboPopup</code>
   * @see ComboPopup
   */
  @Override
  protected ComboPopup createPopup() {
    return new TComboPopup(comboBox);
  }
  
  
  
  
  private class TComboBoxFlasherListener extends TFlasherListener {
    
    public TComboBoxFlasherListener() {
      super(comboBox);
    }

    @Override
    public void actionPerformed(ActionEvent e)  {
      if (!comboBox.isEditable()) {
        flasherVisible = !flasherVisible;
        if (!flasherVisible)  {
          Graphics g = arrowButton.getGraphics();
          if (g != null)  {
            // remove the focus
            g.setColor(arrowButton.getBackground());
            Insets insets = arrowButton.getInsets();
            int width = arrowButton.getWidth() - (insets.left + insets.right);
            int height = arrowButton.getHeight() - (insets.top + insets.bottom);
            if (height > 0 && width > 0) {
              int left = insets.left;
              int top = insets.top;
              g.drawRect(left, top, width-1, height-1);
            }
          }
        }
        else  {
          arrowButton.repaint();
        }
      }
    }
  }
}
