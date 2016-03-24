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

// $Id: FormCalendar.java 440 2008-09-23 16:28:35Z harald $

package org.tentackle.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import org.tentackle.plaf.PlafGlobal;




/**
 * A calendar panel.
 * 
 * @author harald
 */
public class FormCalendar extends FormPanel {

  // time modes
  /** show only date (default) **/
  public static final int  SHOW_DATE          = 0;
  /** show date and hour **/
  public static final int  SHOW_HOUR          = 1;
  /** show date, hour and minutes **/
  public static final int  SHOW_MINUTE        = 2;
  /** show date, hour, minutes and seconds **/
  public static final int  SHOW_SECOND        = 3;


  private Locale locale;                    // locale (z.B. de_DE)
  private GregorianCalendar calendar;       // current calendar
  private String[] dayNames;                // Sun, Mon, ....
  private int timeMode;                     // >0 if show times
  private int firstDayIndex;                // offset for first Day in Month
  private JToggleButton firstClickButton;   // button that got the first click
  
  // Swing components
  private static Insets noInsets = new Insets(0, 0, 0, 0);
  private static Border noBorder = BorderFactory.createEmptyBorder();
  private JPanel calendarPanel = new JPanel();
  private JPanel selectPanel = new JPanel();
  private BorderLayout borderLayout1 = new BorderLayout();
  private GridLayout gridLayout1 = new GridLayout();
  private MonthComboBox monthComboBox;
  private YearSpinField yearSpinField;
  private ButtonGroup dayButtonGroup = new ButtonGroup();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private Component component1;
  private HourSpinField hourSpinField = new HourSpinField();
  private MinSecSpinField minSpinField = new MinSecSpinField();
  private MinSecSpinField secSpinField = new MinSecSpinField();

  // button actions
  private static final String ACTION_THISMONTH      = "M";
  private static final String ACTION_PREVIOUSMONTH  = "P";
  private static final String ACTION_NEXTMONTH      = "N";


  
  /**
   * Creates a calendar panel.
   * 
   * @param locale the locale, null if default locale
   * @param day the date, null if "today"
   * @param timeMode whether and how to display the time, one of SHOW_...
   */
  public FormCalendar(Locale locale, Date day, int timeMode) {
    this.locale = locale == null ? Locale.getDefault() : locale;
    if (day == null) {
      day = new Date();
    }
    this.timeMode = timeMode;
    calendar = new GregorianCalendar(this.locale);
    calendar.setTime(day);
    if (timeMode < SHOW_SECOND) {
      calendar.set(GregorianCalendar.SECOND, 0);
    }
    if (timeMode < SHOW_MINUTE) {
      calendar.set(GregorianCalendar.MINUTE, 0);
    }
    if (timeMode < SHOW_HOUR) {
      calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
    }
    calendar.set(GregorianCalendar.MILLISECOND, 0);
    
    String[] days = new DateFormatSymbols(locale).getWeekdays();
    dayNames = new String[7];
    // week starts on monday!
    for (int i=0; i < 6; i++) {
      dayNames[i] = days[i+2].substring(0,2);
    }
    dayNames[6] = days[1].substring(0,2);

    monthComboBox = new MonthComboBox(locale);
    yearSpinField = new YearSpinField();
    initComponents();
    if (timeMode < SHOW_SECOND) {
      secSpinField.setVisible(false);
    }
    if (timeMode < SHOW_MINUTE) {
      minSpinField.setVisible(false);
    }
    if (timeMode < SHOW_HOUR) {
      hourSpinField.setVisible(false);
    }
      
    // setup the calsheet
    setupCalSheet();
  }

  /**
   * Creates a calendar panel in the current locale.
   * 
   * @param day the date, null if "today"
   * @param timeMode whether and how to display the time, one of SHOW_...
   */
  public FormCalendar (Date day, int timeMode) {
    this(null, day, timeMode);
  }

  /**
   * Creates a calendar panel in current locale, no time.
   * 
   * @param day the date, null if "today"
   */
  public FormCalendar (Date day) {
    this(day, SHOW_DATE);
  }

  /**
   * Creates a calendar panel in current locale, no time, today.
   */
  public FormCalendar() {
    this((Date)null);
  }



  // setup GUI
  private void initComponents() {
    component1 = Box.createHorizontalStrut(8);
    this.setLayout(borderLayout1);
    calendarPanel.setLayout(gridLayout1);
    gridLayout1.setRows(7);
    gridLayout1.setColumns(8);
    yearSpinField.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(ValueEvent e) {
      }
      public void valueEntered(ValueEvent e) {
        yearSpinField_valueEntered(e);
      }
    });
    monthComboBox.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(ValueEvent e) {
      }
      public void valueEntered(ValueEvent e) {
        monthComboBox_valueEntered(e);
      }
    });
    selectPanel.setLayout(gridBagLayout1);
    hourSpinField.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(ValueEvent e) {
      }
      public void valueEntered(ValueEvent e) {
        hourSpinField_valueEntered(e);
      }
    });
    minSpinField.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(ValueEvent e) {
      }
      public void valueEntered(ValueEvent e) {
        minSpinField_valueEntered(e);
      }
    });
    secSpinField.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(ValueEvent e) {
      }
      public void valueEntered(ValueEvent e) {
        secSpinField_valueEntered(e);
      }
    });
    this.add(selectPanel, BorderLayout.NORTH);
    selectPanel.add(monthComboBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    selectPanel.add(component1, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    selectPanel.add(hourSpinField, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    selectPanel.add(minSpinField, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
    selectPanel.add(secSpinField, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
    selectPanel.add(yearSpinField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    this.add(calendarPanel, BorderLayout.CENTER);
  }


  private void setupCalSheet() {
      
    // set year
    yearSpinField.setYear(calendar.get(GregorianCalendar.YEAR));
    // set month
    monthComboBox.setMonth(calendar.get(GregorianCalendar.MONTH));
    // set hour
    hourSpinField.setHour(calendar.get(GregorianCalendar.HOUR_OF_DAY));
    // set minute
    minSpinField.setMinSec(calendar.get(GregorianCalendar.MINUTE));
    // set seconds
    secSpinField.setMinSec(calendar.get(GregorianCalendar.SECOND));

    // remove all buttons and labels from calPanel
    calendarPanel.removeAll();
    calendarPanel.add(new JLabel(""), null);  // week column
    for (int i=0; i < 7; i++) {
      JLabel dayLabel = new JLabel(dayNames[i]);
      dayLabel.setHorizontalAlignment(JLabel.CENTER);
      calendarPanel.add(dayLabel, null);
    }

    // figure out weekday of the first of the month
    GregorianCalendar firstCal = (GregorianCalendar)(calendar.clone());
    firstCal.set(GregorianCalendar.DAY_OF_MONTH, 1);
    firstDayIndex = firstCal.get(GregorianCalendar.DAY_OF_WEEK) - 2;
    // this gives at least one day before the 1st
    if (firstDayIndex <= 0) {
      firstDayIndex += 7;
    }
    firstCal.add(GregorianCalendar.DATE, -firstDayIndex); // go back to monday

    GregorianCalendar today = new GregorianCalendar(locale);  // current date
    Font thisMonthButtonFont  = new JToggleButton().getFont().deriveFont(Font.BOLD);
    Font otherMonthButtonFont = new JToggleButton().getFont().deriveFont(Font.ITALIC + Font.PLAIN);

    Color thisMonthBackgroundColor = PlafGlobal.tableSelectionBackgroundColor;
    Color thisMonthForegroundColor = PlafGlobal.tableSelectionForegroundColor;
    Color otherMonthBackgroundColor = PlafGlobal.tableBackgroundColor;
    Color otherMonthForegroundColor = PlafGlobal.tableForegroundColor;
    
    // add day-buttons
    for (int compCount = 0; compCount < 48; compCount++)  {

      if (compCount % 8 == 0) {
        // insert week of year
        JLabel weekLabel = new JLabel(Integer.toString(firstCal.get(GregorianCalendar.WEEK_OF_YEAR)));
        weekLabel.setHorizontalAlignment(JLabel.CENTER);
        calendarPanel.add(weekLabel, null);
        continue;
      }

      JToggleButton dayButton = new CalToggleButton(Integer.toString(firstCal.get(GregorianCalendar.DAY_OF_MONTH)));

      if (firstCal.get(GregorianCalendar.MONTH) ==
          calendar.get(GregorianCalendar.MONTH)) {
        dayButton.setBackground(thisMonthBackgroundColor);
        dayButton.setForeground(thisMonthForegroundColor);
        dayButton.setFont(thisMonthButtonFont);
        dayButton.setActionCommand(ACTION_THISMONTH);  // this month
      }
      else  {
        dayButton.setBackground(otherMonthBackgroundColor);
        dayButton.setForeground(otherMonthForegroundColor);
        dayButton.setFont(otherMonthButtonFont);
        if (compCount <= firstDayIndex)  {    // +1 bec. of week of year above!
          dayButton.setActionCommand(ACTION_PREVIOUSMONTH);  // previous month
        }
        else  {
          dayButton.setActionCommand(ACTION_NEXTMONTH);  // next month
        }
      }
      // check if today
      if (today.get(GregorianCalendar.YEAR) == firstCal.get(GregorianCalendar.YEAR) &&
          today.get(GregorianCalendar.DAY_OF_YEAR) == firstCal.get(GregorianCalendar.DAY_OF_YEAR))  {
        dayButton.setForeground(Color.blue);
      }
      dayButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dayButton_actionPerformed(e);
        }
      });
      dayButtonGroup.add(dayButton);
      calendarPanel.add(dayButton, null);
      firstCal.add(GregorianCalendar.DATE, 1);
    }

    selectDay();  // select the togglebutton for the current day of month
  }


  private void selectDay()  {
    Component[] comps = calendarPanel.getComponents();
    int buttonCount = 1;
    int day = calendar.get(GregorianCalendar.DAY_OF_MONTH);
    Border border = BorderFactory.createLineBorder(PlafGlobal.alarmColor);
    for (int i=0; i < comps.length; i++)  {
      if (comps[i] instanceof JToggleButton)  {
        JToggleButton button = (JToggleButton)(comps[i]);
          if (button.getActionCommand() == ACTION_THISMONTH)  {
          if (buttonCount == day) {
            button.setSelected(true);
            button.setBorder(border);
            button.setBorderPainted(true);
          }
          else  {
            button.setBorder(null);
            button.setBorderPainted(false);
          }
          buttonCount++;
        }
      }
    }
  }


  /**
   * Returns the current selected calendar value.
   * 
   * @return the calendar value
   */
  public GregorianCalendar getCalendar()  {
    return calendar;
  }

  /**
   * Sets the current calendar value.
   * 
   * @param cal the calendar value
   */
  public void setCalendar(GregorianCalendar cal)  {
    this.calendar = cal;
    setupCalSheet();
  }

  private void yearSpinField_valueEntered(ValueEvent e) {
    // user changed the year
    int yearDiff = yearSpinField.getYear() - calendar.get(GregorianCalendar.YEAR);
    if (yearDiff != 0)  {
      calendar.add(GregorianCalendar.YEAR, yearDiff);
      setupCalSheet();
    }
  }

  private void dayButton_actionPerformed(ActionEvent e) {
    JToggleButton button = (JToggleButton)(e.getSource());
    String cmd = button.getActionCommand();   // one of P, M, N
    NumberFormat format = DecimalFormat.getNumberInstance();
    try {
      int day = format.parse(button.getText()).intValue();
      if (cmd == ACTION_THISMONTH)  {
        calendar.set(GregorianCalendar.DAY_OF_MONTH, day);
        selectDay();
      }
      else if (cmd == ACTION_PREVIOUSMONTH) {
        calendar.add(GregorianCalendar.MONTH, -1);
        calendar.set(GregorianCalendar.DAY_OF_MONTH, day);
        setupCalSheet();
      }
      else if (cmd == ACTION_NEXTMONTH) {
        calendar.add(GregorianCalendar.MONTH, 1);
        calendar.set(GregorianCalendar.DAY_OF_MONTH, day);
        setupCalSheet();
      }
    } catch (ParseException pe)  {
      // nothing to do?
    }
  }

  private void monthComboBox_valueEntered(ValueEvent e) {
    // user changed the month
    int monthDiff = monthComboBox.getMonth() - calendar.get(GregorianCalendar.MONTH);
    if (monthDiff != 0) {
      calendar.add(GregorianCalendar.MONTH, monthDiff);
      setupCalSheet();
    }
  }

  private void hourSpinField_valueEntered(ValueEvent e) {
    calendar.set(GregorianCalendar.HOUR_OF_DAY, hourSpinField.getHour());
  }

  private void minSpinField_valueEntered(ValueEvent e) {
    calendar.set(GregorianCalendar.MINUTE, minSpinField.getMinSec());
  }

  private void secSpinField_valueEntered(ValueEvent e) {
    calendar.set(GregorianCalendar.SECOND, minSpinField.getMinSec());
  }
  
  
  


  /**
   * notify all ActionListeners
   */
  private void fireActionPerformed (String action) {
    fireActionPerformed(new ActionEvent (this, ActionEvent.ACTION_PERFORMED, action));
  }

  
  
  
  /**
   * special button with double-click recognition
   */
  private class CalToggleButton extends JToggleButton {
    
    CalToggleButton(String text) {
      super(text);
      setMargin(noInsets);
      setBorderPainted(false);
      setBorder(noBorder);
    }

    protected void processMouseEvent(MouseEvent e) {
      if (e.getID() == MouseEvent.MOUSE_PRESSED)  {
        if (e.getClickCount() == 2)  {
          if (firstClickButton == this && getActionCommand() == ACTION_THISMONTH) {
            // double click on this month's button
            FormCalendar.this.fireActionPerformed(getText());
          }
          else  {
            // don't allow double click for previous/next month buttons
            e.consume();
            return;
          }
        }
        else  {
          firstClickButton = this;  // remember first clicked button
        }
      }
      super.processMouseEvent(e);
    }
  }

}