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

// $Id: MonthComboBox.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.text.DateFormatSymbols;
import java.util.Locale;



/**
 * A combobox for months as a spinfield.
 * 
 * @author harald
 */
public class MonthComboBox extends FormSpinField implements SpinListener {

  private FormFieldComboBox monthCombo = new FormFieldComboBox();

  
  /**
   * Creates a month combobox for the given locale.
   * 
   * @param locale the locale, null if default
   */
  public MonthComboBox(Locale locale) {
    if (locale == null) {
      locale = Locale.getDefault();
    }
    String[] months = new DateFormatSymbols(locale).getMonths();
    for (int i=0; i < 12; i++)  {
      monthCombo.addItem(months[i]);
    }
    setFormField(monthCombo);
    addSpinListener(this);
  }
  

  /**
   * Creates a month combobox for the default locale.
   */
  public MonthComboBox()  {
    this(null);
  }

  /**
   * Sets the month.
   * 
   * @param month the month value 0-11, -1 to deselect
   */
  public void setMonth(int month) {
    monthCombo.setFireRunning(true);
    monthCombo.setSelectedIndex(month);
    monthCombo.setFireRunning(false);
  }

  /**
   * Gets the month.
   * 
   * @return the month value 0-11, -1 if nothing selected
   */
  public int getMonth() {
    return monthCombo.getSelectedIndex();
  }

  
  
  public void increment (SpinEvent e, FormComponent c)  {
    int index = monthCombo.getSelectedIndex();
    if (index >= 0 && index < 11) {
      monthCombo.setSelectedIndex(index + 1);
    }
  }

  public void decrement (SpinEvent e, FormComponent c)  {
    int index = monthCombo.getSelectedIndex();
    if (index > 0) {
      monthCombo.setSelectedIndex(index - 1);
    }
  }
  
}
