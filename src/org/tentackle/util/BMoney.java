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

// $Id: BMoney.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Locale;
import org.tentackle.ui.FormHelper;

/**
 * Money value derived from BigDecimal.<br>
 * 
 * A money value has a distinct and fixed scale that cannot change.
 *
 * @author harald
 */
public class BMoney extends BigDecimal implements Cloneable {

  private static final long serialVersionUID = 3669978240319189746L;
  
  /**
   * yields amount "in words" for checks and alike
   */
  private static final String[] numberWords = {
    Locales.bundle.getString("zero"),
    Locales.bundle.getString("one"),
    Locales.bundle.getString("two"),
    Locales.bundle.getString("three"),
    Locales.bundle.getString("four"),
    Locales.bundle.getString("five"),
    Locales.bundle.getString("six"),
    Locales.bundle.getString("seven"),
    Locales.bundle.getString("eight"),
    Locales.bundle.getString("nine")
  };
  
  
  
  /**
   * The default currency of the current locale.
   */
  public static Currency currency;
  
  private static void loadCurrency() {
    currency = Currency.getInstance(Locale.getDefault());
  }
  
  static {
    loadCurrency();
    FormHelper.registerLocaleRunnable(new Runnable() {
      public void run() {
        loadCurrency();
      }
    });
  }
  
  
  
  /**
   * Creates a BMoney value.
   * 
   * @param val double-representation of the number
   * @param scale the digits after comma the value should be rounded to
   */
  public BMoney (double val, int scale)   {
    super(new BigDecimal(val).setScale(scale, ROUND_HALF_UP).unscaledValue(), scale);
  }

  /**
   * Creates a BMoney value.
   * 
   * @param  val     the string
   * @param  scale   the digits after comma the value should be rounded to
   */
  public BMoney (String val, int scale)  {
    super(new BigDecimal(val).setScale(scale, ROUND_HALF_UP).unscaledValue(), scale);
  }

  /**
   * Creates a zero BMoney value.
   * 
   * @param  scale the digits after comma
   */
  public BMoney (int scale)  {
    super(new BigInteger(0, new byte[] {}), scale);
  }
  
  /**
   * Creates a BMoney value from a BigInteger.<br>
   * 
   * The scale sets the comma within the given integer.
   * 
   * @param intVal the big integer
   * @param scale  the digits after comma
   */
  public BMoney (BigInteger intVal, int scale) {
    super(intVal, scale); 
  }

  
  /**
   * Creates a BMoney from a BMoney (useful to rescale)
   * 
   * @param val the BMoney value
   * @param scale the digits after comma the value should be rounded to
   */
  public BMoney (BMoney val, int scale) {
    super(val.setScale(scale, ROUND_HALF_UP).unscaledValue(), scale);
  }
  
  
  /**
   * Creates a BMoney from a BigDecimal (userful for SQL)
   * @param val the decimal value
   */
  public BMoney (BigDecimal val)  {
    super(val.unscaledValue(), val.scale()); 
  }
  
  
  /**
   * Creates a zero BMoney with a scale ccording to
   * the currency of the current locale.
   */
  public BMoney ()  {
    this(currency.getDefaultFractionDigits()); 
  }
  

  /**
   * Clones a BMoney value.
   */
  @Override
  public BMoney clone() {
    try {
      return (BMoney)super.clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new InternalError();    // should never happen
    }    
  }
  
  
  /**
   * Checks if this money value is negative.
   * 
   * @return true if number is negative
   */
  public boolean isNegative() {
    return signum() < 0; 
  }

  /**
   * Checks if this money value is positive.
   * 
   * @return true if number is positive
   */
  public boolean isPositive() {
    return signum() > 0; 
  }

  /**
   * Checks if this money value is zero.
   * 
   * @return true if number is zero
   */
  public boolean isZero() {
    return signum() == 0; 
  }


  
  /**
   * Adds a BMoney to this value and returns a new object.
   * This object remains unchanged.
   * 
   * @param val the money value to add
   * @return the sum of this and given value
   */
  public BMoney add(BMoney val)  {
    return new BMoney(unscaledValue().add(alignScale(val).unscaledValue()), scale());
  }
  
  
  /**
   * Subtracts a BMoney from this value and returns a new object.
   * This object remains unchanged.
   * 
   * @param val the money value to subtract
   * @return this minus the given value
   */
  public BMoney subtract(BMoney val)  {
    return new BMoney(unscaledValue().subtract(alignScale(val).unscaledValue()), scale());
  }
  
  
  /**
   * Multiplies this BMoney by a double and returns a new object.
   * This object remains unchanged.
   * 
   * @param val the double to multiply with
   * @return the product of this and given value
   */
  public BMoney multiply(double val)  {
    return new BMoney(doubleValue() * val, scale());
  }
  
  
  /**
   * Divides a BMoney by this value and returns a new object.
   * This object remains unchanged.
   * 
   * @param val the money value to multiply
   * @return the quotient of this and given value
   */
  public double divide(BMoney val) {
    return doubleValue() / val.doubleValue();
  }
  
  
  /**
   * Divides a BMoney by double and returns a new object.
   * This object remains unchanged.
   * 
   * @param val the value to divide this BMoney by
   * @return the quotient of this and given value
   */
  public BMoney divide(double val)  {
    return new BMoney(doubleValue() / val, scale());
  }
  
  
  /**
   * Inverts this money value.
   * 
   * @return the negated money value
   */
  public BMoney invert() {
    return new BMoney(unscaledValue().negate(), scale());
  }
  

  /**
   * Gets the absolute value.
   * 
   * @return the absolute (positive) value 
   */
  public BMoney absolute()  {
    return isNegative() ? invert() : this;
  }
  
  
  /**
   * Returns the smalles positive value (i.e. if scale is 2 --> 0.01 will be returned)
   * @return the smalles positive value
   */
  public BMoney smallestPositive()  {
    return new BMoney(new BigDecimal(new BigInteger(new byte[] { 1 })).movePointLeft(scale())); 
  }
  

  
  /**
   * Converts a money value to a string of digits as words.<br>
   * The digits after the comma are ignored.
   * Nice to print checks.
   * <pre>
   * Example: 234,77 will become "two three four"
   * </pre>
   * 
   * @return the value as a words
   */
  public String toWords() {
    String digits = setScale(0, ROUND_DOWN).toString();
    int len = digits.length();
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (int i=0; i < len; i++) {
      char c = digits.charAt(i);
      if (Character.isDigit(c)) {
        if (first) {
          first = false;
        }
        else {
          buf.append(' ');
        }
        buf.append(numberWords[c - '0']);
      }
    }
    return buf.toString();
  }
  
  
  
  /**
   * Check the scale of the given money value.
   * If it does not match the scale of this object,
   * scale it to the current scale of this object.
   *
   * @param  val the value to be checked
   * @return the re-scaled value or the value itself if no rescaling necessary
   */
  protected BMoney alignScale(BMoney val)  {
    return val.scale() == scale() ? val : new BMoney(val.unscaledValue(), scale()); 
  }

}
