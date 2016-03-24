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

// $Id: ShortLongText.java 336 2008-05-09 14:40:20Z harald $
// Created on November 24, 2005, 6:40 PM

package org.tentackle.util;

/**
 * A short and a long text.<br>
 * 
 * A ShortLongText is something very simple and common:
 * a short String and a longer more detailed description of
 * an object.
 * <p>
 * see FormComboBox.setExtendedPopup().
 * 
 * @author harald
 * @see org.tentackle.ui.FormComboBox#setShortLongPopupEnabled(boolean) 
 */
public interface ShortLongText {

  /**
   * Gets the short text.<br>
   * Usually implemented as {@link Object#toString()}.
   * @return the short text
   */
  public String getShortText();
  
  
  /**
   * Gets the long text.<br>
   * Usually something like {@code object.getName()}.
   * @return the long text
   */
  public String getLongText();
  
  
}
