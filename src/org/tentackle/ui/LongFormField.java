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

// $Id: LongFormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.text.ParseException;
import javax.swing.text.Document;


/**
 * FormField to edit a long value.
 *
 * @author harald
 */
public class LongFormField extends NumberFormField {


  /**
   * Creates an empty LongFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param columns the number of columns, 0 = minimum width
   */
  public LongFormField (Document doc, int columns) {
    super (doc, columns);
  }


  /**
   * Creates an empty LongFormField with the default document model and
   * given column width.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public LongFormField (int columns)  {
    this (null, columns);
  }

  
  /**
   * Creates an empty LongFormField with the default document model,
   * a minimum column width.<br>
   */
  public LongFormField () {
    this (0);
  }
  
  
  /**
   * @return the long value, null if empty
   */
  @Override
  public Long getFormValue ()  {
    String str = getText();
    if (str != null) {
      str = str.replace(getFiller(), ' ').trim();
      if (str.length() > 0) {
        try {
          return new Long(format.parse(str).longValue());
        } 
        catch (ParseException e)  {
          errorOffset = e.getErrorOffset();
        }
      }
    }
    return null;
  }

  
  /**
   * Gets the long value.
   * 
   * @return the long value, 0.0 if empty.
   */
  public long getLongValue() {
    Long value = getFormValue();
    return value == null ? 0L : value.longValue();
  }

  /**
   * Sets the long value.
   * 
   * @param value the long value
   */
  public void setLongValue (long value) {
    setFormValue (new Long(value));
  }
  
}