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

// $Id: TTinyLookAndFeel.java 442 2008-09-23 16:34:52Z harald $

package org.tentackle.plaf.tinylaf;

import de.muntjak.tinylookandfeel.Theme;
import de.muntjak.tinylookandfeel.TinyLookAndFeel;
import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.plaf.TentackleLookAndFeel;



/**
 * Extended Tentackle TinyLAF.
 * <p>
 * Needs at least tinylaf 1.4.0
 * 
 * @author harald
 */
public class TTinyLookAndFeel extends TinyLookAndFeel implements TentackleLookAndFeel {
  
  private boolean focusAnimated = true;       // focus animation for non-textfield components
  
  
  public TTinyLookAndFeel() {
    super();
    loadInitialTheme();
  }


  /**
   * Sets up the initial theme.<p>
   * The default implementation loads "org/tentackle/plaf/tinylaf/tentackle.theme".
   */
  public void loadInitialTheme() {
    URL url = TTinyLookAndFeel.class.getResource("tentackle.theme");
    if (url != null) {
      try {
        Theme.loadTheme(url);
      }
      catch (Throwable t) {
        // something incompatibe
        PlafGlobal.logger.severe("incompatible tinylaf theme");
      }
    }
  }
  
  
  public static String getTTinyName() {
    return "TTiny";
  }


  public static String getTTinyDescription() {
    return "Enhanced TinyLAF for Tentackle";
  }  
  
  @Override
  public String getName() {
    return getTTinyName();
  }
  
  @Override
  public String getDescription() {
    return getTTinyDescription();
  }
  
  @Override
  public String getID() {
    return getName();
  }


  
  @Override
  protected void initClassDefaults(UIDefaults table)
  {
    super.initClassDefaults(table);

    final String tTinyPackageName = "org.tentackle.plaf.tinylaf.";

    Object[] uiDefaults = {
               "ButtonUI",      tTinyPackageName + "TTinyButtonUI",       // replaced!
               "ComboBoxUI",    tTinyPackageName + "TTinyComboBoxUI",
               "RadioButtonUI", tTinyPackageName + "TTinyRadioButtonUI",
               "CheckBoxUI",    tTinyPackageName + "TTinyCheckBoxUI",
               "TextFieldUI",   tTinyPackageName + "TTinyTextFieldUI",
               "TableUI",       tTinyPackageName + "TTinyTableUI",
    };

    table.putDefaults(uiDefaults);
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
