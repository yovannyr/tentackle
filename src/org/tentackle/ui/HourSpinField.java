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

// $Id: HourSpinField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;



/**
 * SpinField for hours.
 *
 * @author harald
 */
public class HourSpinField extends FormSpinField implements SpinListener {

  private IntegerFormField  hourField = new IntegerFormField();

  
  /**
   * Creates an hour spin field.
   * 
   * @param hour the initial hour (0-23)
   */
  public HourSpinField(int hour) {
    hourField.setColumns(2);
    hourField.setFormat("00");
    setFormField(hourField);
    setHour(hour);
    addSpinListener(this);
  }

  /**
   * Creates an hour spin field.
   */
  public HourSpinField()  {
    this(0);
  }


  
  @Override
  public void valueEntered (ValueEvent e)  {
    super.valueEntered(e);
    setHour(getHour());
  }

  
  /**
   * Sets the hour.
   * Will be aligned by modulo 24.
   * 
   * @param hour the hour
   */
  public void setHour(int hour)  {
    hour %= 24; // align
    if (hour < 0) {
      hour += 24;
    }
    hourField.setIntValue(hour);
  }


  /**
   * Gets the hour
   * @return the hour 0-23
   */
  public int getHour()  {
    return hourField.getIntValue();
  }


  public void increment (SpinEvent e, FormComponent c)  {
    setHour(getHour() + 1);
  }

  public void decrement (SpinEvent e, FormComponent c)  {
    setHour(getHour() - 1);
  }
  
}