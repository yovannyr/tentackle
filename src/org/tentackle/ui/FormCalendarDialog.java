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

// $Id: FormCalendarDialog.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Locale;
import javax.swing.JPanel;
import org.tentackle.plaf.PlafGlobal;

/**
 * Calendar dialog.
 *
 * @author harald
 */
public class FormCalendarDialog extends FormDialog {
  
  /**
   * Creates a calendar dialog associated to a given component
   * (usually a date field).
   * 
   * @param comp the component, null if none
   * @param locale the locale, null if default locale
   * @param day the date, null if "today"
   * @param timeMode whether and how to display the time, one of SHOW_...
   * @return the created modal dialog
   */
  public static FormCalendarDialog createFormCalendarDialog (Component comp, Locale locale, Date day, int timeMode) {
    return new FormCalendarDialog(FormHelper.getParentWindow(comp), locale, day, timeMode);
  }

  
  private FormCalendar calendar;      // the calendar panel
  private Date day;                   // selected date
  
  // swing components
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel buttonPanel = new JPanel();
  private FormButton okButton = new FormButton();
  private FormButton cancelButton = new FormButton();


  /**
   * Creates a calendar dialog.
   * 
   * @param owner the owner window, null if none
   * @param locale the locale, null if default locale
   * @param day the date, null if "today"
   * @param timeMode whether and how to display the time, one of SHOW_...
   */
  public FormCalendarDialog(Window owner, Locale locale, Date day, int timeMode) {
    super(owner);
    calendar = new FormCalendar(locale, day, timeMode);
    // listener for double click on date
    calendar.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        okButton.doClick();
      }
    });
    initComponents();
  }

  /**
   * Creates a calendar dialog in current locale for today.
   * 
   * @param owner the owner window, null if none
   * @param timeMode whether and how to display the time, one of SHOW_...
   */
  public FormCalendarDialog(Window owner, int timeMode) {
    this(owner, null, null, timeMode);
  }
  
  /**
   * Creates a calendar dialog for today with no parent window, in
   * current locale, without time.
   */
  public FormCalendarDialog() {
    this(null, FormCalendar.SHOW_DATE);
  }

  
  /**
   * Shows the modal dialog.
   * 
   * @return the date, null if cancel
   */
  public Date showDialog()  {
    pack();
    day = null;
    setVisible(true);
    return day;
  }

  


  private void initComponents() {
    this.setModal(true);
    this.setAutoPosition(true);
    this.getContentPane().setLayout(borderLayout1);
    okButton.setIcon(PlafGlobal.getIcon("ok"));
    okButton.setMargin(new Insets(0, 5, 0, 5));
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    cancelButton.setIcon(PlafGlobal.getIcon("cancel"));
    cancelButton.setMargin(new Insets(0, 5, 0, 5));
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    this.getContentPane().add(calendar, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
  }

  private void okButton_actionPerformed(ActionEvent e) {
    day = calendar.getCalendar().getTime();
    dispose();
  }

  private void cancelButton_actionPerformed(ActionEvent e) {
    dispose();
  }
}