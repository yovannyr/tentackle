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

// $Id: BMoneyPrintLabel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.print;

import org.tentackle.ui.FormHelper;
import org.tentackle.util.BMoney;
import org.tentackle.util.StringHelper;


/**
 * Printing bean for a money object.
 *
 * @author harald
 */
public class BMoneyPrintLabel extends NumberPrintLabel {
  
  static private String defFormat = StringHelper.moneyPattern;   // default format

  
  /**
   * Creates a money print label.
   * 
   * @param columns the width in columns, 0 = minimum width
   */  
  public BMoneyPrintLabel (int columns)  {
    super (columns);
    // generate default format
    format.setGroupingUsed(true);
    format.setGroupingSize(3);
    format.applyPattern(defFormat);
    setScale(BMoney.currency.getDefaultFractionDigits());
    setPrintValue(new BMoney());
  }

  /**
   * Creates a money print label with minimum width.
   */  
  public BMoneyPrintLabel () {
    this (0);
  }
  
  
 
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the scale from the money value
   */
  @Override
  public void setPrintValue (Object value) {
    if (value instanceof BMoney) {
      setScale(((BMoney)value).scale());   // set scale!
    }
    super.setPrintValue(value);
  }

  
  // ------------------- implements NumberPrintLabel -----------------------

  public String doFormat(Object value) {
    if (value instanceof BMoney &&
        (isBlankZero() == false || ((BMoney)value).isZero() == false))  {
      return format.format(((BMoney)value).doubleValue());
    }
    return null;    
  }

}