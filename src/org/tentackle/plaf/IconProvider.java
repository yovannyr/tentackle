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

// $Id: IconProvider.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.plaf;

import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.LookAndFeel;

/**
 * Provides icons and images depending on the look-and-feel.
 * <p>
 * Swing does not provide an abstraction layer
 * for loading icons according to the current look and feel.
 * As a consequence, applications either must stick to a single look and feel
 * or will break the icon theme if the user selects another look and feel.
 * An {@code IconProvider} implements such an abstraction as it loads
 * icons (for a single namespace, aka "realm") according to the current LookAndFeel and 
 * the icon's name.
 * 
 * @author harald
 */
public interface IconProvider {
  
  /**
   * Gets the namespace (realm) this icon provider is responsible for.
   * 
   * @return the realm
   */
  public String getRealm();
  
  
  /**
   * Loads an image icon for the given look and feel.
   * This allows each look and feel to provide its own icon set.
   * Furthermore, applications can add their own realm-provider (see {@link PlafGlobal})
   *
   * @param plaf the look and feel
   * @param name the icon's name (without extension like gif or png!)
   *
   * @return the loaded image icon
   * @throws MissingResourceException if no such icon
   */
  public ImageIcon loadImageIcon(LookAndFeel plaf, String name) throws MissingResourceException;

}
