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

// $Id: FormFieldDocument.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.tentackle.util.StringConverter;
import org.tentackle.util.Toolkit;


/**
 * Document for {@link FormFieldComponent}s.
 * 
 * @author harald
 */
public class FormFieldDocument extends PlainDocument {
  
  
  private StringConverter     converter;    // optional string converter
  private FormFieldComponent  field;        // set by the constructor
  private boolean             eraseFirst;   // true if first insert() erases contents
  

  /**
   * Creates a form field document.
   * 
   * @param field the form field component
   */
  public FormFieldDocument(FormFieldComponent field)  {
    this.field = field;   // remember formtextfield for checks below
  }
  


  /**
   * Trigger an autoerase before the first change on the contents.<br>
   * Used in FormTableCellEditor if autoSelect-Feature turned on
   * and the field is edited in semi-editing mode.
   */
  void setEraseFirst(boolean erasefirst) {
    this.eraseFirst = erasefirst;
  }

  
  /**
   * Gets the current converter. Default is null.
   *
   * @return the converter.
   */
  public StringConverter getConverter() {
    return converter;
  }
  
  /**
   * Sets the string converter.
   *
   * @param converter the string converter
   */
  public void setConverter(StringConverter converter) {
    this.converter = converter;
  }

  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to implement autoSelect, character conversion, valid and
   * invalid character check, filler, insert/override, maxColumns, adjustment,
   * etc...
   */
  @Override
  public void insertString (int offs, String str, AttributeSet a)
          throws BadLocationException {

    if (eraseFirst) {
      if (field.isAutoSelect() == false ||   // if not enabled for whatever reason
          field.hasFocus() == false)  {       // or only if autoSelect wouldn't work
        super.remove(0, getLength());
        offs = 0;
        // this will cause selectAll to be inhibited if eraseFirst is called after
        // the first delivered KeyEvent in a JTable
        field.setInhibitAutoSelect(true);
        // signal change to container if not yet done
        field.triggerValueChanged();
      }
      eraseFirst = false;
    }

    // convert input string, if converter set
    if (converter != null) {
      str = converter.convert(str);
    }
    
    if (field == null)  {
      // called with wrong constructor??
      super.insertString(offs, str, a);
    }
    
    else if (str != null) {

      // check constraints:
      boolean valid     = true;                   // default is okay
      int maxColumns    = field.getMaxColumns();  // max cols
      boolean override = field.isOverwrite();    // insert/override
      char filler       = field.getFiller();      // fill char

      // check max. number of chars
      if (maxColumns > 0 &&
          override == true &&
          offs + str.length() > maxColumns)  {
        valid = false;
        // else: no check in insert mode because we cut at the end
        // see below
      }

      if (filler != ' ') {
        // translate fillers
        str = str.replace (' ', filler);
      }

      // do character conversions
      switch (field.getConvert()) {

        case FormField.CONVERT_UC:
          // convert to uppercase
          str = str.toUpperCase();
          break;

        case FormField.CONVERT_LC:
          // convert to lowercase
          str = str.toLowerCase();
          break;
      }


      // check valid chars
      if (field.getValidChars() != null)  {
        String vchars = field.getValidChars();
        for (int i=0; i < str.length(); i++) {
          // filler is always allowed!
          if (str.charAt(i) == filler) {
            continue;
          }
          // check that char is in valid string
          if (vchars.indexOf(str.charAt(i)) < 0) {
            valid = false;
            break;
          }
        }
      }

      // check invalid chars
      if (field.getInvalidChars() != null)  {
        String ichars = field.getInvalidChars();
        for (int i=0; i < str.length(); i++) {
          if (ichars.indexOf(str.charAt(i)) >= 0) {
            valid = false;
            break;
          }
        }
      }

      if (valid)  {
        
        if (field.isOverwrite() == true) {
          // override mode: remove first
          int olen = getLength();
          int rlen = str.length();
          if (olen - offs < rlen) {
            rlen = olen - offs;
          }
          if (rlen > 0) {
            super.remove(offs, rlen);
          }
        }
        // everything is okay, safe to insert
        super.insertString(offs, str, a);

        // cut chars at end
        if (override == false &&    // insert-mode
            maxColumns > 0 &&          // maxcolumns set
            getLength() > maxColumns) {
          // cut at end
          super.remove (maxColumns, getLength() - maxColumns);
          Toolkit.beep();  // beep
        }

        // check for maxautonext
        if (maxColumns > 0 && field.isAutoNext() &&
            field.getCaretPosition() >= maxColumns)  {
            field.transferFocus();
        }
        
        // signal value changed to container
        field.triggerValueChanged();
      }

      else    {
        Toolkit.beep();  // beep
      }
    }
  }
  
  

  @Override
  public void remove (int offs, int len) throws BadLocationException {

    // do the job in the parent class first
    super.remove (offs, len);
    
    // signal value changed to container
    field.triggerValueChanged();
  }

}
