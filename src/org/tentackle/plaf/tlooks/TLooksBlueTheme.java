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

// $Id: TLooksBlueTheme.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tlooks;

import com.jgoodies.looks.plastic.PlasticTheme;
import javax.swing.plaf.ColorUIResource;

/**
 * Blue theme.
 * 
 * @author harald
 */
public class TLooksBlueTheme extends PlasticTheme {
  
  private static final ColorUIResource primary1 = new ColorUIResource(89, 122, 193);        // pale blue
  private static final ColorUIResource primary2 = new ColorUIResource(140, 172, 239);       // blue
  private static final ColorUIResource primary3 = new ColorUIResource(170, 197, 255);       // light blue

  private static final ColorUIResource secondary1 = new ColorUIResource(122, 122, 132);     // pale grey
  private static final ColorUIResource secondary2 = new ColorUIResource(162, 162, 172);     // grey
  private static final ColorUIResource secondary3 = new ColorUIResource(230, 230, 238);     // light grey

  @Override
  protected ColorUIResource getPrimary1() {
    return primary1;
  }
  
  @Override
  protected ColorUIResource getPrimary2() {
    return primary2;
  }
  
  @Override
  protected ColorUIResource getPrimary3() {
    return primary3;
  }

  @Override
  protected ColorUIResource getSecondary1() {
    return secondary1;
  }
  
  @Override
  protected ColorUIResource getSecondary2() {
    return secondary2;
  }
  
  @Override
  protected ColorUIResource getSecondary3() {
    return secondary3;
  }  
  
}
