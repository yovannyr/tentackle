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

// $Id: YearSpinField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.util.Date;
import java.util.GregorianCalendar;


/**
 * SpinField for years. (4 digits)
 *
 * @author harald
 */
public class YearSpinField extends FormSpinField
       implements SpinListener {

  private IntegerFormField  yearField = new IntegerFormField();

  
  /**
   * Creates a year spin field.
   * 
   * @param year the initial year, 4-digit value!
   */
  public YearSpinField(int year) {
    yearField.setColumns(4);
    setFormField(yearField);
    setYear(year);
    addSpinListener(this);
  }

  /**
   * Creates a year spin field for the current year.
   */
  public YearSpinField()  {
    this(new GregorianCalendar().get(GregorianCalendar.YEAR));
  }


  /**
   * Sets the year.
   * 
   * @param year the year, 4-digit value!
   */
  public void setYear (int year)  {
    yearField.setIntValue(year);
  }

  /**
   * Gets the year.
   * 
   * @return the year, 4-digit value!
   */
  public int getYear()  {
    return yearField.getIntValue();
  }


  public void increment (SpinEvent e, FormComponent c)  {
    setYear(getYear() + 1);
  }

  public void decrement (SpinEvent e, FormComponent c)  {
    setYear(getYear() - 1);
  }
}