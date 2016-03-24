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

// $Id: PlafGlobal.java 473 2009-08-07 17:19:36Z harald $

package org.tentackle.plaf;

import java.awt.Color;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.tentackle.util.Logger;
import org.tentackle.util.LoggerFactory;

/**
 * Plaf globals.
 * 
 * @author harald
 */
public class PlafGlobal {

  /** Default logger for the appworx-package */
  public static Logger logger = LoggerFactory.getLogger("org.tentackle.plaf");


  
  @SuppressWarnings("unchecked")
  private static void installPLAF(String clazzName) {
    String name = null;
    try {
      // this will also check all dependencies, whether the plaf is in the classpath, etc...
      Class clazz = Class.forName(clazzName);
      // figure out the name by invoking the class'es static name func
      name = clazzName.substring(clazzName.lastIndexOf('.') + 1, clazzName.length() - 11);
      // translate the name to what the method get<PLAF>Name() returns
      name = (String) clazz.getMethod("get" + name + "Name").invoke(null);
    }
    catch (NoClassDefFoundError e) {
      // some reference missing
      return;
    }
    catch (Exception e) {
      // some other config error
      System.err.println(e);
      return;
    }
    UIManager.installLookAndFeel(name, clazzName);
  }
  
  /**
   * Installs all Tentackle Look and Feels
   */
  public static void installTentackleLookAndFeels() {
    installPLAF("org.tentackle.plaf.tmetal.TMetalLookAndFeel");
    installPLAF("org.tentackle.plaf.tmetal.TOceanLookAndFeel");
    installPLAF("org.tentackle.plaf.tplastic.TPlasticLookAndFeel");
    installPLAF("org.tentackle.plaf.tlooks.TLooksLookAndFeel");
    installPLAF("org.tentackle.plaf.tlooks.TLooksBlueLookAndFeel");
    installPLAF("org.tentackle.plaf.tsubstance.TSubstanceLookAndFeel");
    installPLAF("org.tentackle.plaf.tinylaf.TTinyLookAndFeel");
  }
  

  /**
   * Gets all installed tentackle Look and Feels
   * @return the installed tentackle plafs
   */
  public static LookAndFeelInfo[] getInstalledTentackleLookAndFeels() {
    LookAndFeelInfo[] ilafs = UIManager.getInstalledLookAndFeels();
    int count = 0;
    for (LookAndFeelInfo lf: ilafs) {
      if (lf.getClassName().startsWith("org.tentackle.plaf.")) {
        count++;
      }
    }
    LookAndFeelInfo[] rv = new LookAndFeelInfo[count];
    count = 0;
    for (LookAndFeelInfo lf: ilafs) {
      if (lf.getClassName().startsWith("org.tentackle.plaf.")) {
        rv[count++] = lf;
      }
    }    
    return rv;
  }
  
  
  /**
   * Configures the animated keyboard focus for non-text components that can grab
   * the keyboard focus, such as comboboxes, radio buttons or check boxes.
   * By default, the animation is enabled. For non-tentackle plafs this setting
   * has no effect.
   *
   * @param flag the boolean value which is true to enable the animation, false to turn it off
   */
  public static void setFocusAnimated(boolean flag) {
    LookAndFeel plaf = UIManager.getLookAndFeel();
    if (plaf instanceof TentackleLookAndFeel) {
      ((TentackleLookAndFeel)plaf).setFocusAnimated(flag);
    }
  }
  
  
  /**
   * Retrievs the current setting for the focus animation.
   *
   * @return true if focus is animated
   */
  public static boolean isFocusAnimated() {
    LookAndFeel plaf = UIManager.getLookAndFeel();
    return plaf instanceof TentackleLookAndFeel ?
          ((TentackleLookAndFeel)plaf).isFocusAnimated() : false;
  }
  
  
  
  
  /**
   * system colors
   */
  public static Color alarmColor;                             // for alarms, errors, etc...
  public static Color alarmBackgroundColor;                   // background error panel color
  public static Color listSelectedForegroundColor;            // foreground color of cells in lists if editable and selected
  public static Color listUnselectedForegroundColor;          // foreground color of cells in lists if editable and unselected
  public static Color listSelectedDisabledForegroundColor;    // foreground color of cells in lists if not editable and selected
  public static Color listUnselectedDisabledForegroundColor;  // foreground color of cells in lists if not editable and unselected
  public static Color tableForegroundColor;                   // foreground color of cells in tables
  public static Color tableBackgroundColor;                   // background color of cells in tables
  public static Color tableFocusCellForegroundColor;          // foreground color of cells in tables if focused
  public static Color tableFocusCellBackgroundColor;          // background color of cells in tables if focused
  public static Color tableSelectionForegroundColor;          // foreground color of cells in tables if selected
  public static Color tableSelectionBackgroundColor;          // background color of cells in tables if selected
  public static Color dropFieldActiveColor;                   // drop destination color
  public static Color dropFieldInactiveColor;                 // drop destination color when field inactive (no drop possible)
  public static Color tableEditCellBorderColor;               // border color for edited cells in tables
  public static Color textFieldBackgroundColor;               // textfield background color
  public static Color textFieldInactiveBackgroundColor;       // textfield inactive background color
  
  /**
   * IconProviders implement an abstraction layer for loading icons.
   * Each plaf may provide its own icon set which the application
   * can refer to in a portable way. Furthermore, applications can
   * register their own icon-provider. This allows applications to
   * adapt their icon sets if the user selects another plaf.
   * 
   */
  
  /**
   * The namespace of the icons for tentackle (can be used by apps)
   */
  public static final String TENTACKLE_ICONREALM = "tentackle";
  
  
  // the icon provider map
  private static final Map<String,IconProvider> iconProviderMap = new TreeMap<String,IconProvider>();
  
  
  /**
   * Registers an IconProvider.
   * Applications can register their own providers for the their namespaces.
   * 
   * @param iconProvider the provider to register
   * @return null if there was no provider for the provider's realm, the old provider otherwise
   */
  public static IconProvider addIconProvider(IconProvider iconProvider) {
    return iconProviderMap.put(iconProvider.getRealm(), iconProvider);
  }
  
  
  /**
   * Unregisters an IconProvider for a given realm.
   * 
   * @param realm the realm to remove the provider for
   * @return null if there was no provider for the provider's realm, the old provider otherwise
   */
  public static IconProvider removeIconProvider(String realm) {
    return iconProviderMap.remove(realm);
  }  
  
  
  /**
   * Icons are cached for the currently selected look and feel.
   * The key is constructed from the realm and icon name as {@code "realm/name"}.
   */
  private static Map<String,ImageIcon> iconMap;

  
  /**
   * Gets an icon for the current look-and-feel, a given realm and icon name.
   * 
   * @param realm the icon's namespace
   * @param name the name if the icon
   * @return the icon
   * @throws MissingResourceException if no such icon
   */
  public static ImageIcon getIcon(String realm, String name) throws MissingResourceException {
    
    ImageIcon icon = null;
    String key = realm + "/" + name;
    
    if (iconMap == null) {
      iconMap = new TreeMap<String,ImageIcon>();
    }
    else  {
      // check if icon is already loaded
      icon = iconMap.get(key);
    }
   
    if (icon == null) {
      // not in cache: load it
      IconProvider iconProvider = iconProviderMap.get(realm);
      if (iconProvider == null) {
        throw new MissingResourceException("no icon provider for realm '" + realm + "'", PlafGlobal.class.getName(), realm);
      }
      icon = iconProvider.loadImageIcon(UIManager.getLookAndFeel(), name);
      iconMap.put(key, icon);
    }
    
    return icon;
  }
  
  
  /**
   * Gets the icon for the tentackle default realm.
   * 
   * @param name the name if the icon
   * @return the icon
   * @throws MissingResourceException if no such icon
   */
  public static ImageIcon getIcon(String name) throws MissingResourceException { 
    return getIcon(TENTACKLE_ICONREALM, name);
  }
  

  /**
   * Invoke this method whenever the plaf has changed.
   */
  public static void triggerLookAndFeelUpdated() {
    
    // colors are created as non-UIResource colors because otherwise most
    // plaf implementation will ignore them.
    textFieldBackgroundColor = new Color(((Color)UIManager.getDefaults().get("TextField.background")).getRGB());
    textFieldInactiveBackgroundColor = new Color(((Color)UIManager.getDefaults().get("TextField.inactiveBackground")).getRGB());
    alarmColor = Color.red;   // not depending on plaf
    alarmBackgroundColor = new Color(255, 192, 192);
    // we use new Color because UIManager usually returns UIResource colors,
    // which are not honoured by most lafs.
    listSelectedForegroundColor = new Color(((Color)UIManager.getDefaults().get("List.selectionForeground")).getRGB());
    listUnselectedForegroundColor = new Color(((Color)UIManager.getDefaults().get("List.foreground")).getRGB());
    listUnselectedDisabledForegroundColor = new Color(((Color)UIManager.getDefaults().get("TextField.inactiveForeground")).getRGB());
    Color color1 = new Color(((Color)UIManager.getDefaults().get("List.selectionForeground")).getRGB());
    Color color2 = new Color(((Color)UIManager.getDefaults().get("List.selectionBackground")).getRGB());
    listSelectedDisabledForegroundColor = new Color((color1.getRed() + color2.getRed()) >> 1,
                                        (color1.getGreen() + color2.getGreen()) >> 1,
                                        (color1.getBlue() + color2.getBlue()) >> 1);

    tableForegroundColor = new Color(((Color)UIManager.getDefaults().get("Table.foreground")).getRGB());
    tableBackgroundColor = new Color(((Color)UIManager.getDefaults().get("Table.background")).getRGB());
    tableFocusCellForegroundColor = new Color(((Color)UIManager.getDefaults().get("Table.focusCellForeground")).getRGB());
    tableFocusCellBackgroundColor = new Color(((Color)UIManager.getDefaults().get("Table.focusCellBackground")).getRGB());
    tableSelectionForegroundColor = new Color(((Color)UIManager.getDefaults().get("Table.selectionForeground")).getRGB());
    tableSelectionBackgroundColor = new Color(((Color)UIManager.getDefaults().get("Table.selectionBackground")).getRGB());


    color1 = new Color(((Color)UIManager.getDefaults().get("TextField.selectionBackground")).getRGB());
    color2 = new Color(((Color)UIManager.getDefaults().get("TextField.inactiveBackground")).getRGB());
    
    dropFieldActiveColor = new Color((color1.getRed() + color2.getRed()) >> 1,
                                     (color1.getGreen() + color2.getGreen()) >> 1,
                                     (color1.getBlue() + color2.getBlue()) >> 1);
    float[] hsb = new float[3];
    Color.RGBtoHSB(dropFieldActiveColor.getRed(), dropFieldActiveColor.getGreen(), dropFieldActiveColor.getBlue(), hsb);
    hsb[1] = 0.2f;    // fixed saturation 20%
    hsb[2] = 0.9f;    // fixed brightness 90%
    dropFieldActiveColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    
    dropFieldInactiveColor = new Color(((Color)UIManager.getDefaults().get("TextField.inactiveBackground")).getRGB());
    
    Color.RGBtoHSB(listSelectedDisabledForegroundColor.getRed(), 
                   listSelectedDisabledForegroundColor.getGreen(),
                   listSelectedDisabledForegroundColor.getBlue(), hsb);
    hsb[1] *= 0.2f;
    tableEditCellBorderColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    
    // clear the icon cache
    iconMap = null;
  }
  
    
  static  {
    triggerLookAndFeelUpdated();
    addIconProvider(new TentackleIconProvider());
  }
  
}
