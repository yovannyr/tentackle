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

// $Id: BMoneyFormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.text.ParseException;
import javax.swing.text.Document;
import org.tentackle.util.BMoney;
import org.tentackle.util.DMoney;
import org.tentackle.util.StringHelper;



/**
 * FormField to edit a BMoney object.
 *
 * @author harald
 */
public class BMoneyFormField extends NumberFormField {
  
  static private final String defFormat = StringHelper.moneyPattern;   // default format
  
  /** application-wide default setting for the auto-comma feature **/
  public static boolean defaultAutoComma = false;
  
  private boolean autoComma;      // true if auto comma at scale
  private char commaChar;         // the comma character
  private boolean parseDMoney;    // true if return DMoney instead of BMoney
  

  /**
   * Creates an empty BMoneyFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param columns the number of columns, 0 = minimum width
   * @param parseDMoney true if getFormValue() returns DMoney, false if BMoney
   */
  public BMoneyFormField (Document doc, int columns, boolean parseDMoney) {
    super (doc, columns);
    this.parseDMoney = parseDMoney;
    autoComma = defaultAutoComma;
    setValidChars (getValidChars() + ",.");   // add delimiters to valid chars
    format.applyPattern(defFormat);           // generate default format
    // set the commaChar and the scale from the current locale
    commaChar = format.getDecimalFormatSymbols().getDecimalSeparator();
    setScale(BMoney.currency.getDefaultFractionDigits());
  }

  
  /**
   * Creates an empty BMoneyFormField with the default document model and
   * given column width.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   * @param parseDMoney true if getFormValue() returns DMoney, false if BMoney
   */
  public BMoneyFormField (int columns, boolean parseDMoney)  {
    this (null, columns, parseDMoney);
  }

  
  /**
   * Creates an empty BMoneyFormField with the default document model,
   * and minimum column width.<br>
   * 
   * @param parseDMoney true if getFormValue() returns DMoney, false if BMoney
   */
  public BMoneyFormField (boolean parseDMoney) {
    this (0, parseDMoney);
  }
  
  
  /**
   * Creates an empty BMoneyFormField with the default document model,
   * a minimum column width for a BMoney.<br>
   */
  public BMoneyFormField () {
    this (false);
  }
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the scale from the money value
   */
  @Override
  public void setFormValue (Object object) {
    if (object instanceof BMoney) {
      setScale(((BMoney)object).scale());   // set scale!
    }
    super.setFormValue(object);
  }
  

  @Override
  public BMoney getFormValue() {
    String str = getText();
    if (str != null) {
     str = str.replace(getFiller(), ' ').trim();
      if (str.length() > 0) {
        // check for autocomma if no comma in input
        if (autoComma && getScale() > 0 && str.indexOf(commaChar) < 0)  {
          // insert comma
          int len = str.length();
          int prec = getScale();
          while (len <= prec) {
            str = "0" + str;
            len++;
          }
          str = str.substring(0, len - prec) + commaChar + str.substring(len - prec);
        }
        try {
          // convert
          return parseDMoney ?
                  new DMoney(format.parse(str).doubleValue(), getScale()) :
                  new BMoney(format.parse(str).doubleValue(), getScale());
        }
        catch (ParseException e)  {
          errorOffset = e.getErrorOffset();
        }
      }
    }
    return null;
  }
  
  
  /**
   * Gets the value as a DMoney.
   *
   * @return the money value or null if field is empty
   */
  public DMoney getDMoney() {
    if (parseDMoney) {
      return (DMoney)getFormValue();
    }
    else  {
      BMoney val = getFormValue();
      return val == null ? null : new DMoney(val);
    }
  }
  
  
  /**
   * Sets whether getFormValue() returns BMoney or DMoney.
   *
   * @param parseDMoney 
   */
  public void setParseDMoney(boolean parseDMoney) {
    this.parseDMoney = parseDMoney;
  }
  
  /**
   * Gets the flag whether getFormValue() will return DMoney or BMoney.
   *
   * @return true if DMoney, else BMoney (default)
   */
  public boolean isParseDMoney() {
    return parseDMoney;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden cause of comma-char.
   */
  @Override
  public void setFormat(String pattern) {
    super.setFormat(pattern);
    commaChar = format.getDecimalFormatSymbols().getDecimalSeparator();
  }

  
  /**
   * Sets the auto-comma feature.<br>
   * Allows "business" keyboard input, i.e. comma is set according to the
   * property scale if no comma has been entered by the user.<br>
   * <pre>
   * Example: "1122" --> "11.22" if scale was 2.
   * 
   * @param autoComma true if automatically set the comma, false if not (default)
   * @see #defaultAutoComma
   */
  public void setAutoComma(boolean autoComma)  {
    this.autoComma = autoComma;
  }
  
  /**
   * Returns the auto-comma setting.
   * @return true if automatically set the comma according to scale
   */
  public boolean isAutoComma() {
    return autoComma;
  }

}
