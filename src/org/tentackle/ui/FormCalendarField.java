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

// $Id: FormCalendarField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Date;
import org.tentackle.plaf.PlafGlobal;



/**
 * A date field with a calendar button.
 *
 * @author harald
 */
public class FormCalendarField extends FormFieldComponentPanel {
  
  /** show only date (default) **/
  public static final int SHOW_AUTO  = -1; // set timeMode from format

  private DateFormField dateField;
  private int timeMode;
  private FormButton calButton = new FormButton();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();

  
  
  /**
   * Creates a calendar field.
   */
  public FormCalendarField() {
    super(new DateFormField());
    dateField = (DateFormField)getFormFieldComponent();
    timeMode = SHOW_AUTO;    // derive from format
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  

  /**
   * Sets the time mode.<br>
   * One of {@code FormCalendar.SHOW_...}.
   * 
   * @param timeMode the time mode, -1 if from date format (default)
   * @see FormCalendar
   */
  public void setTimeMode(int timeMode) {
    this.timeMode = timeMode;
  }

  /**
   * Gets the time mode.<br>
   * One of {@link FormCalendar}.SHOW_...
   * 
   * @return the time mode, -1 if from date format (default)
   */
  public int getTimeMode()  {
    return timeMode;
  }
  
  
  /**
   * Sets the "lenient" flag in date format.
   * 
   * @param lenient true if lenient
   */
  public void setLenient(boolean lenient) {
    dateField.setLenient(lenient);
  }

  /**
   * Gets the lenient flag.
   * 
   * @return the lenient flag
   */
  public boolean isLenient() {
    return dateField.isLenient();
  }
  
  
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    checkButtonEnabled();
  }
  
  @Override
  public void setChangeable(boolean changeable) {
    super.setChangeable(changeable);
    checkButtonEnabled();
  }
  
  @Override
  public void setFormValue(Object value)  {
    super.setFormValue(value);
    checkButtonEnabled();
  }
  
  @Override
  public Date getFormValue()  {
    return dateField.getFormValue();
  }
  
  /**
   * Gets the {@code java.sql.Date}.
   * 
   * @return the SQL date
   */
  public java.sql.Date getDate() {
    return dateField.getDate();
  }
  
  
  /**
   * Gets the {@code java.sql.Timestamp}.
   * @return the SQL timestamp
   */
  public java.sql.Timestamp getTimestamp() {
    return dateField.getTimestamp();
  }
  
  /**
   * Gets the {@code java.sql.Time}.
   * @return the SQL time
   */
  public java.sql.Time getTime() {
    return dateField.getTime();
  }
  
  
  /**
   * Gets the reference date.
   * 
   * @return the reference date, null = today
   */
  public Date getReferenceDate() {
    return dateField.getReferenceDate();
  }

  /**
   * Sets the reference date.
   * 
   * @param referenceDate the reference date, null = today
   */
  public void setReferenceDate(Date referenceDate) {
    dateField.setReferenceDate(referenceDate);
  }
  
  
  private void checkButtonEnabled() {
    calButton.setEnabled(getFormValue() != null || (isEnabled() && isChangeable()));
  }
  

  private void jbInit() throws Exception {
    calButton.setIcon(PlafGlobal.getIcon("calendar_mini"));
    calButton.setMargin(new Insets(0, 0, 0, 0));
    calButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        calButton_actionPerformed(e);
      }
    });
    this.setLayout(gridBagLayout1);
    this.add(dateField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 1), 0, 0));
    this.add(calButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
  }



  private void calButton_actionPerformed(ActionEvent e) {
    // determine timemode from date
    int tmode = timeMode;
    if (tmode == SHOW_AUTO) {
      // from format
      tmode = FormCalendar.SHOW_DATE;
      String fmt = dateField.getFormat();
      if (fmt != null) { // can it be null?
        if (fmt.indexOf('H') >= 0 || fmt.indexOf('K') >= 0 ||
            fmt.indexOf('h') >= 0 || fmt.indexOf('k') >= 0) {
          tmode = FormCalendar.SHOW_HOUR;
          if (fmt.indexOf('m') >= 0) {
            tmode = FormCalendar.SHOW_MINUTE;
            if (fmt.indexOf('s') >= 0) {
              tmode = FormCalendar.SHOW_SECOND;
            }
          }
        }
      }
    }
    // open the dialog
    Date date = FormCalendarDialog.createFormCalendarDialog(
                    this, getLocale(), getFormValue(), tmode).showDialog();
    if (isChangeable() && isEnabled() && date != null) {
      setFormValue(date);
      fireValueEntered();
      requestFocusLater();
    }
  }

}