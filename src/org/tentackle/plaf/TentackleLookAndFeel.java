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

// $Id: TentackleLookAndFeel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.plaf;

import java.util.MissingResourceException;
import javax.swing.ImageIcon;

/**
 * Interface all extended Tentackle look-and-feels must implement.
 * <p>
 * Tentackle provides wrappers for well known look and feels like metal or looks.
 * The wrappers extend the swing look and feel by features such as:
 *
 * <ul>
 *  <li>blinking or animated keyboard focus for *ALL* components (not only text fields)</li>
 *  <li>added vertical alignment for JTextField (necessary in autoheight tables)</li>
 *  <li>added functionality on JTable (dynamically merged cells, formatting, etc...)</li>
 *  <li>added popup renderer for comboboxes</li>
 *  <li>portable icon management</li>
 *  <li>etc...</li>
 * </ul>
 *
 * @author harald
 */
public interface TentackleLookAndFeel {
  
  /**
   * Loads an image icon for the look and feel.
   * The method is invoked by {@link PlafGlobal}.
   * It allows each look and feel to provide its own icon set.
   *
   * @param name the icon's name (without extension like gif or png!)
   *
   * @return the loaded image icon
   * @throws MissingResourceException if no such icon
   */
  public ImageIcon loadImageIcon(String name) throws MissingResourceException;
  
  
  /**
   * Configures the animated keyboard focus for non-text components that can grab
   * the keyboard focus, such as comboboxes, radio buttons or check boxes.
   * By default, the animation is enabled.
   *
   * @param flag the boolean value which is true to enable the animation, false to turn it off
   */
  public void setFocusAnimated(boolean flag);
  
  
  /**
   * Retrievs the current setting for the focus animation.
   *
   * @return true if focus is animated
   */
  public boolean isFocusAnimated();
  
}
