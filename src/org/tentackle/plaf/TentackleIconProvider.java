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

// $Id: TentackleIconProvider.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.plaf;

import java.net.URL;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.LookAndFeel;
import static org.tentackle.plaf.PlafGlobal.TENTACKLE_ICONREALM;



/**
 * Default implementation of an {@link IconProvider}.
 * This provider loads the icons for the "plaf"-realm of all supported plafs.
 * 
 * @author harald
 */
public class TentackleIconProvider implements IconProvider {

  
  /**
   * Creates a new TentackleIconProvider
   */
  public TentackleIconProvider() {
    // something to do?
  }
  
  
  /**
   * Gets the namespace for this icon provider.
   * 
   * @return the namespace for the plaf-space
   */
  public String getRealm() {
    return TENTACKLE_ICONREALM;
  }
  

  /**
   * Loads the icon.
   * All plaf-icons by default are PNG files. If there is no PNG file,
   * a GIF-file is tried.
   * They are located in "org/tentackle/plaf/<look-and-feel-dir>/icons".
   * If the look and feel is not a TentackleLookAndFeel the icons are
   * loaded from a fallback set in "org/tentackle/plaf/icons".
   * 
   * @param plaf the plaf
   * @param name the name if the icon
   * @return the icon
   * @throws MissingResourceException if no such icon
   */
  public ImageIcon loadImageIcon(LookAndFeel plaf, String name) throws MissingResourceException {
    if (plaf instanceof TentackleLookAndFeel) {
      return ((TentackleLookAndFeel)plaf).loadImageIcon(name);
    }
    else  {
      // non Tentackle LookAndFeel: load default icons from "org/tentackle/plaf/icons"
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
  }

}
