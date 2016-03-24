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

// $Id: TSubstanceLookAndFeel.java 402 2008-08-31 14:09:08Z harald $



package org.tentackle.plaf.tsubstance;

import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import org.jvnet.lafwidget.animation.FadeConfigurationManager;
import org.jvnet.lafwidget.animation.FadeKind;
import org.jvnet.substance.skin.SubstanceModerateLookAndFeel;
import org.tentackle.plaf.TentackleLookAndFeel;



/**
 *
 * @author harald
 */
public class TSubstanceLookAndFeel extends SubstanceModerateLookAndFeel implements TentackleLookAndFeel {
  
  protected static MetalTheme currentTheme = null;
  
  private boolean focusAnimated = true;       // focus animation for non-textfield components
  
  
  public TSubstanceLookAndFeel() {
    configureTSubstanceLookAndFeel();
    currentTheme = null;    // this does the trick that switching between plafs resets the theme!
  }
  
  
  public void configureTSubstanceLookAndFeel() {
    FadeConfigurationManager.getInstance().allowFades(FadeKind.ICON_GLOW);
    FadeConfigurationManager.getInstance().allowFades(FadeKind.GHOSTING_ICON_ROLLOVER);
    FadeConfigurationManager.getInstance().allowFades(FadeKind.GHOSTING_BUTTON_PRESS);
    setFocusAnimated(focusAnimated);
  }
  
  
  public static String getTSubstanceName() {
      return "TSubstance";
  }


  public static String getTSubstanceDescription() {
      return "Enhanced Substance LnF for Tentackle";
  }  
  
  public String getName() {
      return getTSubstanceName();
  }
  

  public String getDescription() {
      return getTSubstanceDescription();
  }
  
  public String getID() {
    return getName();
  }


  public boolean isNativeLookAndFeel() {
    return false;
  }


  public boolean isSupportedLookAndFeel() {
    return true;
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
   */
  public void uninitialize() {
    setCurrentTheme(new DefaultMetalTheme());
  }

  
  /**
   * overridden to install special TSubstance UI
   */
  protected void initClassDefaults(UIDefaults table)
  {
    super.initClassDefaults(table);

    final String tLooksPackageName = "org.tentackle.plaf.tsubstance.";

    Object[] uiDefaults = {
               "ComboBoxUI",    tLooksPackageName + "TSubstanceComboBoxUI",
               "TextFieldUI",   tLooksPackageName + "TSubstanceTextFieldUI",
               "TableUI",       tLooksPackageName + "TSubstanceTableUI",
    };

    table.putDefaults(uiDefaults);
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
   * For the substance plaf we simply enable/disable the focus loop.
   *
   * @param flag the boolean value which is true to enable the animation, false to turn it off
   */
  public void setFocusAnimated(boolean flag) {
    focusAnimated = flag;
    if (flag) {
      FadeConfigurationManager.getInstance().allowFades(FadeKind.FOCUS_LOOP_ANIMATION);
    }
    else  {
      FadeConfigurationManager.getInstance().disallowFades(FadeKind.FOCUS_LOOP_ANIMATION);
    }
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
