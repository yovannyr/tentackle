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

// $Id: DoubleFormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.text.ParseException;
import javax.swing.text.Document;
import org.tentackle.util.StringHelper;


/**
 * FormField to edit a double value.
 *
 * @author harald
 */
public class DoubleFormField extends NumberFormField {
  
  static private final String defFormat = StringHelper.floatDoublePattern; // default format
  

  /**
   * Creates an empty DoubleFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param columns the number of columns, 0 = minimum width
   */
  public DoubleFormField (Document doc, int columns) {
    super (doc, columns);
    setValidChars (getValidChars() + "dDeE.,");   // add delimiters to valid chars
    format.applyPattern(defFormat);               // generate default format
  }


  /**
   * Creates an empty DoubleFormField with the default document model and
   * given column width.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public DoubleFormField (int columns)  {
    this (null, columns);
  }

  
  /**
   * Creates an empty DoubleFormField with the default document model,
   * a minimum column width.<br>
   */
  public DoubleFormField () {
    this (0);
  }
  
  
  /**
   * @return the double value, null if empty
   */
  @Override
  public Double getFormValue ()  {
    String str = getText();
    if (str != null) {
      str = str.replace(getFiller(), ' ').trim();
      if (str.length() > 0) {
        try {
          return new Double(format.parse(str).doubleValue());
        } 
        catch (ParseException e)  {
          errorOffset = e.getErrorOffset();
        }
      }
    }
    return null;
  }

  
  /**
   * Gets the double value.
   * 
   * @return the double value, 0.0 if empty.
   */
  public double getDoubleValue() {
    Double value = getFormValue();
    return value == null ? 0.0f : value.doubleValue();
  }

  /**
   * Sets the double value.
   * 
   * @param value the double value
   */
  public void setDoubleValue (double value) {
    setFormValue (new Double(value));
  }

}