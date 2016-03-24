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

// $Id: TOceanLookAndFeel.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tmetal;

import javax.swing.plaf.metal.OceanTheme;

/**
 * Extended Tentackle Ocean Look And Feel.
 * 
 * @author harald
 */
public class TOceanLookAndFeel extends TMetalLookAndFeel {
  
  /** Creates a new instance of TOceanLookAndFeel */
  public TOceanLookAndFeel() {
    super();
  }
  
  
  public static String getTOceanName() {
      return "TOcean";
  }


  public static String getTOceanDescription() {
      return "Enhanced Ocean LnF for Tentackle";
  }  
  
  @Override
  public String getName() {
      return getTOceanName();
  }
  
  @Override
  public String getDescription() {
      return getTOceanDescription();
  }
  
  @Override
  public void initialize() {
    super.initialize();
    TMetalLookAndFeel.setCurrentTheme(new OceanTheme());
  }
  
}
