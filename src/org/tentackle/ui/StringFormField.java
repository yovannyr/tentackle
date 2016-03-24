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

// $Id: StringFormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import javax.swing.text.Document;
import org.tentackle.util.StringHelper;



/**
 * A FormField for editing strings.
 * 
 * @author harald
 */
public class StringFormField extends FormField {

  private boolean nullIfEmpty = true;   // getText() returns null if string is empty
  
  
  /**
   * Creates a StringFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param str the initial text, null = empty
   * @param columns the number of columns, 0 = minimum width
   */
  public StringFormField (Document doc, String str, int columns) {
    super (doc, str, columns);
  }

  /**
   * Creates a StringFormField using the default document model.<br>
   * 
   * @param str the initial text, null = empty
   * @param columns the number of columns, 0 = minimum width
   */
  public StringFormField (String str, int columns) {
    super (null, str, columns);
  }
  
  /**
   * Creates an empty StringFormField using the default document model.<br>
   *
   * @param columns the number of columns, 0 = minimum width
   */
  public StringFormField (int columns)  {
    this (null, columns);
  }

  /**
   * Creates an empty StringFormField with miminum width
   * using the default document model.<br>
   */
  public StringFormField () {
    this (0);
  }

  
  
  
  /**
   * Sets whether empty strings should be returned as null.<br>
   * 
   * @param nullIfEmpty true if zero-length input is treated as null (default)
   *
   */
  public void setNullIfEmpty(boolean nullIfEmpty) {
    this.nullIfEmpty = nullIfEmpty;
  }
  
  /** 
   * Returns whether empty strings should be returned as null.<br>
   * @return true if zero-length input is treated as null (default)
   */
  public boolean isNullIfEmpty() {
    return nullIfEmpty;
  }  
  
  

  /**
   * {@inheritDoc}
   * <p>
   * Sets the form to the string value of given object
   */
  public void setFormValue (Object object)  {
    if (object != null)  {
      setText (object.toString());   // this allows all objects to setFormValue()
    }
    else  {
      clearText();
    }
  }
  
  /**
   * @return the textfields contents as a string
   */
  public String getFormValue ()  {
    return getText();
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to replace the null string with the empty string
   */
  @Override
  public void setText (String str)  {
    if (str == null) {
      str = StringHelper.emptyString;
    }
    super.setText(str);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because of nullIfEmpty
   */
  @Override
  public String getText() {
    String text = super.getText();
    if (nullIfEmpty && text != null && text.length() == 0) {
      text = null;
    }
    return text;
  }
  

  /**
   * The format will be ignored in StringFormFields.
   */
  public void setFormat (String pattern) {
    // do nothing
  }

  /**
   * The format will be ignored in StringFormFields.
   * @return the empty string
   */
  public String getFormat ()  {
    return StringHelper.emptyString;
  }

  /**
   * The format will be ignored in StringFormFields.
   * @return just returns the given string
   */
  public String doFormat (Object object) {
    return object == null ? null : object.toString();
  }

}
