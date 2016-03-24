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

// $Id: TLooksLookAndFeel.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tlooks;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import org.tentackle.plaf.TentackleLookAndFeel;



/**
 * Extended JGoodies Look-And-Feel for Tentackle.
 * 
 * @author harald
 */
public class TLooksLookAndFeel extends Plastic3DLookAndFeel implements TentackleLookAndFeel {
  
  
  private boolean focusAnimated = true;       // focus animation for non-textfield components
  
  
  
  public TLooksLookAndFeel() {
    super();
    configureTLooksLookAndFeel();
  }
  
  
  public void configureTLooksLookAndFeel() {
    PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
    PlasticLookAndFeel.set3DEnabled(true);
    Options.setPopupDropShadowEnabled(true);
    UIManager.put(Options.PLASTIC_MICRO_LAYOUT_POLICY_KEY, TLooksMicroLayoutPolicies.getDefaultPlasticPolicy());
  }
  
  
  public static String getTLooksName() {
      return "TLooks";
  }


  public static String getTLooksDescription() {
      return "Enhanced JGoodies LnF for Tentackle";
  }  
  
  @Override
  public String getName() {
      return getTLooksName();
  }
  

  @Override
  public String getDescription() {
      return getTLooksDescription();
  }
  
  
  @Override
  public String getID() {
    return getName();
  }

  
  @Override
  protected void initClassDefaults(UIDefaults table)
  {
    super.initClassDefaults(table);

    final String tLooksPackageName = "org.tentackle.plaf.tlooks.";

    Object[] uiDefaults = {
               "ButtonUI",      tLooksPackageName + "TLooksButtonUI",       // replaced!
               "ComboBoxUI",    tLooksPackageName + "TLooksComboBoxUI",
               "RadioButtonUI", tLooksPackageName + "TLooksRadioButtonUI",
               "CheckBoxUI",    tLooksPackageName + "TLooksCheckBoxUI",
               "TextFieldUI",   tLooksPackageName + "TLooksTextFieldUI",
               "TableUI",       tLooksPackageName + "TLooksTableUI",
    };

    table.putDefaults(uiDefaults);
  }
  
  
  @Override
  protected void initComponentDefaults(UIDefaults table) {
    super.initComponentDefaults(table);
    
    String iconPrefix = "icons/" + (LookUtils.IS_LOW_RESOLUTION ? "32x32/" : "48x48/");
    
    Object[] defaults = {
      "OptionPane.errorIcon",       loadLooksIcon(iconPrefix + "dialog-error.png"),
      "OptionPane.informationIcon", loadLooksIcon(iconPrefix + "dialog-information.png"),
      "OptionPane.questionIcon",    loadLooksIcon(iconPrefix + "dialog-question.png"),
      "OptionPane.warningIcon",     loadLooksIcon(iconPrefix + "dialog-warning.png"),
      "FileView.computerIcon",      loadLooksIcon("icons/Computer.gif"),
      "FileView.directoryIcon",     loadLooksIcon("icons/TreeClosed.gif"),
      "FileView.fileIcon",          loadLooksIcon("icons/File.gif"),
      "FileView.floppyDriveIcon",   loadLooksIcon("icons/FloppyDrive.gif"),
      "FileView.hardDriveIcon",     loadLooksIcon("icons/HardDrive.gif"),
      "FileChooser.homeFolderIcon", loadLooksIcon("icons/HomeFolder.gif"),
      "FileChooser.newFolderIcon",  loadLooksIcon("icons/NewFolder.gif"),
      "FileChooser.upFolderIcon",   loadLooksIcon("icons/UpFolder.gif"),
      "Tree.closedIcon",            loadLooksIcon("icons/TreeClosed.gif"),
      "Tree.openIcon",              loadLooksIcon("icons/TreeOpen.gif"),
      "Tree.leafIcon",              loadLooksIcon("icons/TreeLeaf.gif"),
    };
    
    table.putDefaults(defaults);
  }
  
  
  /**
   * Loads an icon.
   * This is the "missing" method in PlasticLookAndFeel. Override this method
   * if you don't want the icons to be loaded from the Looks archive.
   * @param name the icon name
   * @return the icon
   */
  protected Object loadLooksIcon(String name) {
    return makeIcon(PlasticLookAndFeel.class, name);
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
