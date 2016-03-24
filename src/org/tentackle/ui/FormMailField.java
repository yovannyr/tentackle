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

// $Id: FormMailField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.URLHelper;


/**
 * FormField to edit a mail adress.<br>
 * By pressing the mail-button the mailer program
 * will be opened.
 *
 * @author harald
 */
public class FormMailField extends FormFieldComponentPanel {

  private StringFormField mailField;             // the formfield, contains the mail-address
  private String subject;                        // optional subject
  private String body;                           // optional body
  
  private FormButton mailButton = new FormButton();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();


  /**
   * Creates a form mail field.
   */
  public FormMailField() {
    super(new StringFormField());
    mailField = (StringFormField)getFormFieldComponent();
    initComponents();
  }
  
  
  
  /**
   * Gets the subject of the mail.
   * 
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }
  
  /**
   * Sets the subject of the mail that will
   * be passed to the mailer program when pressing the button.
   * 
   * @param subject the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }
  
  /**
   * Gets the body of the mail.
   * 
   * @return the body.
   */
  public String getBody() {
    return body;
  }
  
  /**
   * Sets the body of the mail that will
   * be passed to the mailer program when pressing the button.
   * 
   * @param body the subject
   */
  public void setBody(String body) {
    this.body = body;
  }
  


  private void initComponents() {
    mailButton.setIcon(PlafGlobal.getIcon("mail"));   // NOI18N
    mailButton.setMargin(new Insets(0, 0, 0, 0));
    this.setLayout(gridBagLayout1);
    mailButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mailButton_actionPerformed(e);
      }
    });
    this.add(mailField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 1), 0, 0));
    this.add(mailButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  
  private void mailButton_actionPerformed(ActionEvent e) {
    if (getText() != null && getText().length() > 0)  {
      try {
        // startet den Mailer
        URLHelper.mailTo(getText(), subject, body);
      }
      catch (Exception ex) {
        FormError.printException(Locales.bundle.getString("Mailer_could_not_be_launched"), ex);
      }
    }
  }


}