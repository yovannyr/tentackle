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

// $Id: TMetalTheme.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tmetal;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * Blue theme for TMetal.
 * 
 * @author harald
 */
public class TMetalTheme extends DefaultMetalTheme {
  
  /**
   * Creates a new instance of TMetalTheme
   */
  public TMetalTheme() {
    super();
  }
  
  private static final ColorUIResource primary1 = new ColorUIResource(89, 122, 193);
  private static final ColorUIResource primary2 = new ColorUIResource(110, 151, 239);
  private static final ColorUIResource primary3 = new ColorUIResource(170, 197, 255);

  private static final ColorUIResource secondary1 = new ColorUIResource(102, 102, 128);
  private static final ColorUIResource secondary2 = new ColorUIResource(150, 150, 188);
  private static final ColorUIResource secondary3 = new ColorUIResource(222, 222, 232);

  protected ColorUIResource getPrimary1() { return primary1; } 
  protected ColorUIResource getPrimary2() { return primary2; }
  protected ColorUIResource getPrimary3() { return primary3; }

  protected ColorUIResource getSecondary1() { return secondary1; }
  protected ColorUIResource getSecondary2() { return secondary2; }
  protected ColorUIResource getSecondary3() { return secondary3; }  
 
  
  // Contants identifying the various Fonts that are Theme can support
  static final int CONTROL_TEXT_FONT = 0;
  static final int SYSTEM_TEXT_FONT = 1;
  static final int USER_TEXT_FONT = 2;
  static final int MENU_TEXT_FONT = 3;
  static final int WINDOW_TITLE_FONT = 4;
  static final int SUB_TEXT_FONT = 5;
}
