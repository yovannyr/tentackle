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

// $Id: NumberFormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.text.DecimalFormat;
import javax.swing.SwingConstants;
import javax.swing.text.Document;
import org.tentackle.util.BMoney;
import org.tentackle.util.StringHelper;




/**
 * Abstract base class for all numeric form fields.
 *
 * @author harald
 */
abstract public class NumberFormField extends FormField {
  
  
  static private final String defValidChars = "-0123456789";  // default valid chars
  static private final String defFormat = StringHelper.integerPattern; // default format
  
  private int scale;            // digits after comma
  private boolean unsigned;     // true if only positive integers allowed
  private boolean blankZero;    // true if zero should be an empty field
  
  /** the decimal format **/
  protected DecimalFormat format;



  /**
   * Creates a NumberFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param columns the number of columns, 0 = minimum width
   */
  public NumberFormField (Document doc, int columns) {
    super (doc, null, columns);
    // default is only digits */
    setValidChars (defValidChars);
    setHorizontalAlignment(SwingConstants.TRAILING);
    format = new DecimalFormat(defFormat);
  }


  /**
   * Creates a NumberFormField with the default document model and
   * given column width.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public NumberFormField (int columns)  {
    this (null, columns);
  }

  /**
   * Creates an empty NumberFormField with the default document model,
   * and minimum column width.<br>
   */
  public NumberFormField () {
    this (0);
  }

  
  
  /**
   * @see DecimalFormat
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

  public String getFormat ()  {
    return format.toPattern();
  }
  

  public void setFormValue (Object object)  {
    super.setText (doFormat(object));
  }
  
  
  public String doFormat (Object object)  {

    if (object != null) {

      if (blankZero)  {
        if (object instanceof Double) {
          if (((Double)object).doubleValue() == 0.0d) {
            return StringHelper.emptyString;
          }
        }
        else if (object instanceof Float)  {
          if (((Float)object).floatValue() == 0.0f) {
            return StringHelper.emptyString;
          }
        }
        else if (object instanceof BMoney) {
          if (((BMoney)object).isZero()) {
            return StringHelper.emptyString;
          }
        }
        else if (object instanceof Number) {
          if (((Number)object).longValue() == 0l) {
            return StringHelper.emptyString;
          }
        }
      }

      // else standard formatting */
      return format.format(object);
    }

    return StringHelper.emptyString;
  }


  
  /**
   * Sets field to unsigned.
   * 
   * @param unsigned true if unsigned, false if signed
   */
  public  void  setUnsigned(boolean unsigned)  {
    
    this.unsigned = unsigned;
    
    String vchars = getValidChars();
    
    if (unsigned) {
      // remove minus sign from valid chars, if any
      vchars = getValidChars().replace('-', getFiller());
    }
    else  {
      // replace fillers if any
      vchars = getValidChars().replace(getFiller(), '-');
      // add minus sign if not already done
      if (vchars.indexOf("-") < 0) {
        vchars += "-";
      }
    }
    
    super.setValidChars (vchars);
  }

  /**
   * Returns whether format is unsigned
   *
   * @return true if field is unsigned, else false
   */
  public boolean isUnsigned() {
    return unsigned;
  }
  
  
  
  
  @Override
  public void setValidChars(String validChars) {
    super.setValidChars(validChars);
    unsigned = validChars != null && validChars.indexOf('-') < 0;
  }


  
  
  /**
   * Sets whether zero fields are displayed as empty fields.
   * 
   * @param blankZero true if zero values are displayed as empty field, default is false
   */
  public  void  setBlankZero (boolean blankZero)  {
    this.blankZero = blankZero;
  }

  /**
   * Returns the blankZero attribute.
   * 
   * @return true if zero values are displayed as empty field
   */
  public boolean isBlankZero()  {
    return blankZero;
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

}
