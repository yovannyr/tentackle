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

// $Id: NumberPrintLabel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.print;


import java.text.DecimalFormat;
import javax.swing.SwingConstants;
import org.tentackle.ui.FormHelper;
import org.tentackle.util.StringHelper;

/**
 * Abstract printing bean for formatting numbers.<br>
 * Must be subclassed for the concrete data type.
 *
 * @author harald
 */
abstract public class NumberPrintLabel extends PrintLabel {

  static private final String defFormat = StringHelper.integerPattern; // default format

  /** the decimal format **/
  protected DecimalFormat format;
  
  private boolean blankZero;        // true if zero should be an empty field
  private int scale;                // digits after comma
  
  
  /**
   * Creates a number print label.
   * 
   * @param columns the width in columns, 0 = minimum width
   */
  public NumberPrintLabel(int columns) {
    super(columns, SwingConstants.TRAILING);
    // default format
    format = new DecimalFormat(defFormat);
    setPrintValue(0);
  }
  
  /**
   * Creates a number print label with minimum width.
   */
  public NumberPrintLabel() {
    this(0);
  }


  /**
   * convert the number to the Form (re-implemented from PrintLabel)
   */
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
  abstract public String doFormat (Object value);



  /**
   * Sets the decimal format.
   * 
   * @param pattern the format string
   */
  public void setFormat (String pattern)  {
    format = new DecimalFormat (pattern);
    scale = 0;
    int dotNdx = pattern.lastIndexOf('.');
    if (dotNdx >= 0)  {
      // count zeros after dot
      for (++dotNdx; dotNdx < pattern.length(); dotNdx++)  {
        if (pattern.charAt(dotNdx) != '0') {
          break;
        }
        scale++;
      }
    }
  }

  /**
   * Gets the decimal format string.
   * 
   * @return the format string 
   */
  public String getFormat ()  {
    return format.toPattern();
  }

  
  /**
   * Changes the format according to the given scale.
   *
   * @param scale the number of digits after the comma
   */
  public void setScale(int scale) {
    if (this.scale != scale) {
      FormHelper.setScale(format, scale);
      this.scale = scale;
    }
  }

  /**
   * Gets the current scale.
   * 
   * @return the scale
   */
  public int getScale() {
    return scale;
  }
  
  /**
   * Sets zero suppression.
   * 
   * @param blankZero true if zero will result in an empty field
   */
  public  void  setBlankZero (boolean blankZero)  {
    this.blankZero = blankZero;
  }

  /**
   * Gets zero suppression.
   * 
   * @return true if zero will result in an empty field
   */
  public boolean isBlankZero()  {
    return blankZero;
  }
  
}