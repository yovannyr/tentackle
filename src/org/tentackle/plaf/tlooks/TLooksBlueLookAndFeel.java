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

// $Id: TLooksBlueLookAndFeel.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tlooks;

/**
 * Cross platform TLooks with a pleasent blue color theme.
 * 
 * @author harald
 */
public class TLooksBlueLookAndFeel extends TLooksLookAndFeel {
  
  /** Creates a new instance of TLooksBlueLookAndFeel */
  public TLooksBlueLookAndFeel() {
    super();
  }
  
  
  @Override
  protected void createDefaultTheme() {
    setCurrentTheme(new TLooksBlueTheme());
  }
  
  
  public static String getTLooksBlueName() {
      return "TLooks-Blue";
  }

  public static String getTLooksBlueDescription() {
      return "Enhanced Blue JGoodies LnF for Tentackle";
  }  
  
  @Override
  public String getName() {
      return getTLooksBlueName();
  }
  

  @Override
  public String getDescription() {
      return getTLooksBlueDescription();
  }
  
}
