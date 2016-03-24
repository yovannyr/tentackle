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

// $Id: DoublePrintLabel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.print;

import org.tentackle.util.StringHelper;


/**
 * Printing bean for float and double values.<br>
 * Used to print also floats.
 *
 * @author harald
 */

public class DoublePrintLabel extends NumberPrintLabel {
  
  static private String defFormat = StringHelper.floatDoublePattern; // default format
  

  /**
   * Creates a double print label.
   * 
   * @param columns the width in columns, 0 = minimum width
   */
  public DoublePrintLabel (int columns) {
    super (columns);
    format.applyPattern(defFormat);
  }
  
  /**
   * Creates a double print label with minimum width.
   */
  public DoublePrintLabel () {
    this (0);
  }
  
  
  // ------------------- implements NumberPrintLabel -----------------------

  public String doFormat(Object value) {
    if (value instanceof Number) {
      double dval = ((Number)value).doubleValue();
      if (!isBlankZero() || dval != 0.0) {
        return format.format(dval);
      }
    }
    return null;    
  }

}