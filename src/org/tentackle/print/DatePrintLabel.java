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

// $Id: DatePrintLabel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.print;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.tentackle.ui.FormHelper;
import org.tentackle.util.StringHelper;



/**
 * Printing bean for date.
 *
 * @author harald
 */
public class DatePrintLabel extends PrintLabel  {
  
  // default format
  private static String defFormat = StringHelper.shortDatePattern;
  
  static {
    FormHelper.registerLocaleRunnable(new Runnable() {
      public void run() {
        defFormat = StringHelper.shortDatePattern;
      }
    });
  }
  
  
  private SimpleDateFormat format;    // the format

  

  /**
   * Creates a date print field.
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public DatePrintLabel(int columns) {
    super (columns);
    setFormat(defFormat);
    setPrintValue(new Date());
  }

  /**
   * Creates a date print field with minimum width
   */
  public DatePrintLabel () {
    this (0);
  }

  

  @Override
  public void setPrintValue (Object value)  {
    this.value = value;
    setText (value == null ? null : doFormat(value));
  }

  /**
   * Renders an object according a format.<br>
   * This function does *not* set the text in the field.
   * It can be used to return the rendered String of *any* object of this kind.
   * 
   * @param value the object to format
   * @return the formatted object as a string
   */
  public String doFormat(Object value)  {
    return (value instanceof Date ? format.format((Date)value) : null);
  }

  
  /**
   * Sets the date format.
   * 
   * @param pattern the format string
   */
  public void setFormat (String pattern)  {
    // set the format string
    format = new SimpleDateFormat (pattern);
    // show new value (usually nice for GUI-Builders)
    setPrintValue(value);
  }

  /**
   * Gets the date format string.
   * 
   * @return the format string 
   */
  public String getFormat ()  {
    return format.toPattern();
  }

}
