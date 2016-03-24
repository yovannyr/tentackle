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

// $Id: TPlasticLookAndFeel.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tplastic;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import org.tentackle.plaf.TentackleLookAndFeel;



/**
 * Extended Tentackle Kunststoff Look-And-Feel.
 * <p>
 * Notice that Kunststoff is a discontinued project.
 * The recommended plaf is TLooks.
 * 
 * @author harald
 */
public class TPlasticLookAndFeel extends KunststoffLookAndFeel implements TentackleLookAndFeel {
  
  private static MetalTheme currentTheme = null;
  
  private boolean focusAnimated = true;       // focus animation for non-textfield components
  
  
  
  
  public TPlasticLookAndFeel() {
    currentTheme = null;    // this does the trick that switching between plafs resets the theme!
  }
  
  
  public static String getTPlasticName() {
    return "TPlastic";
  }


  public static String getTPlasticDescription() {
    return "Enhanced Kunststoff LnF for Tentackle";
  }  
  
  @Override
  public String getName() {
    return getTPlasticName();
  }
  
  @Override
  public String getDescription() {
    return getTPlasticDescription();
  }
  
  @Override
  public String getID() {
    return getName();
  }

  @Override
  protected void createDefaultTheme() {
    super.createDefaultTheme();
    if (currentTheme == null) {
      setCurrentTheme(new TPlasticTheme());
      setCurrentGradientTheme(new TPlasticGradientTheme());
    }
  }
  
  public static void setCurrentTheme(MetalTheme theme) {
    MetalLookAndFeel.setCurrentTheme(theme);
    currentTheme = theme;
  }
  
  @Override
  public void uninitialize() {
    setCurrentTheme(new DefaultMetalTheme());
  }

  
  @Override
  protected void initClassDefaults(UIDefaults table)
  {
    super.initClassDefaults(table);

    final String tPlasticPackageName = "org.tentackle.plaf.tplastic.";

    Object[] uiDefaults = {
               "ButtonUI",      tPlasticPackageName + "TPlasticButtonUI",       // replaced!
               "ComboBoxUI",    tPlasticPackageName + "TPlasticComboBoxUI",
               "RadioButtonUI", tPlasticPackageName + "TPlasticRadioButtonUI",
               "CheckBoxUI",    tPlasticPackageName + "TPlasticCheckBoxUI",
               "TextFieldUI",   tPlasticPackageName + "TPlasticTextFieldUI",
               "TableUI",       tPlasticPackageName + "TPlasticTableUI",
    };

    table.putDefaults(uiDefaults);
  }
  
  
  @Override
  protected void initComponentDefaults(UIDefaults table) {

    super.initComponentDefaults( table );

    //
    // DEFAULTS TABLE
    //
    if (currentTheme != null)  {

      Object userTextValue    = new FontActiveValue(currentTheme, TPlasticTheme.USER_TEXT_FONT);
      Object controlTextValue = new FontActiveValue(currentTheme, TPlasticTheme.CONTROL_TEXT_FONT);

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
      case TPlasticTheme.CONTROL_TEXT_FONT:
          value = theme.getControlTextFont();
          break;
      case TPlasticTheme.SYSTEM_TEXT_FONT:
          value = theme.getSystemTextFont();
          break;
      case TPlasticTheme.USER_TEXT_FONT:
          value = theme.getUserTextFont();
          break;
      case TPlasticTheme.MENU_TEXT_FONT:
          value = theme.getMenuTextFont();
          break;
      case TPlasticTheme.WINDOW_TITLE_FONT:
          value = theme.getWindowTitleFont();
          break;
      case TPlasticTheme.SUB_TEXT_FONT:
          value = theme.getSubTextFont();
          break;
      }
      return value;
    }
  }

  
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
  
  public void setFocusAnimated(boolean flag) {
    focusAnimated = flag;
  }
  
  public boolean isFocusAnimated() {
    return focusAnimated;
  }
}
