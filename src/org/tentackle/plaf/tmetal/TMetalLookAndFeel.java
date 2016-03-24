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

// $Id: TMetalLookAndFeel.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tmetal;

import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalTheme;
import org.tentackle.plaf.TentackleLookAndFeel;



/**
 * Extended Tentackle Metal Look And Feel.
 * 
 * @author harald
 */
public class TMetalLookAndFeel extends MetalLookAndFeel implements TentackleLookAndFeel {
  
  
  private static MetalTheme currentTheme = null;
  
  private boolean focusAnimated = true;       // focus animation for non-textfield components
  
  
  /**
   * Creates a new instance of TMetalLookAndFeel
   */
  public TMetalLookAndFeel() {
    currentTheme = null;    // this does the trick that switching between plafs resets the theme!
  }
  
  
  
  public static String getTMetalName() {
      return "TMetal";
  }


  public static String getTMetalDescription() {
      return "Enhanced Metal LnF for Tentackle";
  }  
  
  public String getName() {
      return getTMetalName();
  }
  

  public String getDescription() {
      return getTMetalDescription();
  }


  public String getID() {
    return getName();
  }


  protected void createDefaultTheme() {
    if (currentTheme == null) {
      setCurrentTheme(new TMetalTheme());
    }
  }
  
  
  /**
   * Sets the current color theme.
   */
  public static void setCurrentTheme(MetalTheme theme) {
    MetalLookAndFeel.setCurrentTheme(theme);
    currentTheme = theme;
  }
  
  

  /**
   * UIManager.setLookAndFeel calls this method just before we're
   * replaced by a new default look and feel.   Subclasses may 
   * choose to free up some resources here.
   *
   * @see #initialize
   */
  public void uninitialize() {
    setCurrentTheme(new DefaultMetalTheme());
  }

  
  /**
   * overridden to install special TMetal UI
   */
  
  protected void initClassDefaults(UIDefaults table)
  {
      super.initClassDefaults(table);
      
      final String tMetalPackageName = "org.tentackle.plaf.tmetal.";

      Object[] uiDefaults = {
                 "ButtonUI",      tMetalPackageName + "TMetalButtonUI",       // replaced!
                 "ComboBoxUI",    tMetalPackageName + "TMetalComboBoxUI",
                 "RadioButtonUI", tMetalPackageName + "TMetalRadioButtonUI",
                 "CheckBoxUI",    tMetalPackageName + "TMetalCheckBoxUI",
                 "TextFieldUI",   tMetalPackageName + "TMetalTextFieldUI",
                 "TableUI",       tMetalPackageName + "TMetalTableUI",
      };

      table.putDefaults(uiDefaults);
      
      

  }

  protected void initComponentDefaults(UIDefaults table) {
      super.initComponentDefaults( table );
      //
      // DEFAULTS TABLE
      //
      if (currentTheme != null)  {
        Object userTextValue = new FontActiveValue(currentTheme, TMetalTheme.USER_TEXT_FONT);
        Object controlTextValue = new FontActiveValue(currentTheme, TMetalTheme.CONTROL_TEXT_FONT);

        // changes the values so that all user-input fields are marked bold and
        // background data is plain.
        Object[] defaults = {
          "Label.font", userTextValue,
          "TextField.font", controlTextValue,
          "TextArea.font", controlTextValue,
          "PasswordField.font", controlTextValue,
          "Table.font", controlTextValue,
          "TitledBorder.font", userTextValue,
        };
      
        table.putDefaults(defaults);
      }
  }
  
  
  
  /**
   * FontActiveValue redirects to the appropriate metal theme method.
   */
  private static class FontActiveValue implements UIDefaults.ActiveValue {
      private int type;
      private MetalTheme theme;

      FontActiveValue(MetalTheme theme, int type) {
          this.theme = theme;
          this.type  = type;
      }

      public Object createValue(UIDefaults table) {
          Object value = null;
          switch (type) {
          case TMetalTheme.CONTROL_TEXT_FONT:
              value = theme.getControlTextFont();
              break;
          case TMetalTheme.SYSTEM_TEXT_FONT:
              value = theme.getSystemTextFont();
              break;
          case TMetalTheme.USER_TEXT_FONT:
              value = theme.getUserTextFont();
              break;
          case TMetalTheme.MENU_TEXT_FONT:
              value = theme.getMenuTextFont();
              break;
          case TMetalTheme.WINDOW_TITLE_FONT:
              value = theme.getWindowTitleFont();
              break;
          case TMetalTheme.SUB_TEXT_FONT:
              value = theme.getSubTextFont();
              break;
          }
          return value;
      }
  }
  
  
  
  
  /**
   * Implements TentackleLookAndFeel.
   */
  public ImageIcon loadImageIcon(String name) throws MissingResourceException {
    URL url = getClass().getResource("icons/" + name + ".png");
    if (url == null) {
      // try GIF
      url = getClass().getResource("icons/" + name + ".gif");
    }
    if (url == null) {
      throw new MissingResourceException("no such icon '" + name + "'", getClass().getName(), name);
    }
    return new ImageIcon(url);
  }
  

  /**
   * Configures the animated keyboard focus for non-text components that can grab
   * the keyboard focus, such as comboboxes, radio buttons or check boxes.
   * By default, the animation is enabled.
   *
   * @param flag the boolean value which is true to enable the animation, false to turn it off
   */
  public void setFocusAnimated(boolean flag) {
    focusAnimated = flag;
  }
  
  
  /**
   * Retrievs the current setting for the focus animation.
   *
   * @return true if focus is animated
   */
  public boolean isFocusAnimated() {
    return focusAnimated;
  }
}
