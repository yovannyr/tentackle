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

// $Id: FormUrlField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.URLHelper;



/**
 * Form field to edit a URL.
 * <p>
 * A button press opens the browser to the given URL.
 *
 * @author harald
 */
public class FormUrlField extends FormFieldComponentPanel {

  private StringFormField urlField;             // the formfield
  //
  private FormButton urlButton = new FormButton();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();


  /**
   * Creates an empty url field.
   */
  public FormUrlField() {
    super(new StringFormField());
    urlField = (StringFormField)getFormFieldComponent();
    initComponents();
  }


  private void initComponents() {
    urlButton.setIcon(PlafGlobal.getIcon("url"));
    urlButton.setMargin(new Insets(0, 0, 0, 0));
    this.setLayout(gridBagLayout1);
    urlButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        urlButton_actionPerformed(e);
      }
    });
    this.add(urlField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 1), 0, 0));
    this.add(urlButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void urlButton_actionPerformed(ActionEvent e) {
    if (getText() != null && getText().length() > 0)  {
      try {
        // startet den Mailer
        URLHelper.openURL(getText());
      }
      catch (Exception ex) {
        FormError.printException("Can't open URL '" + getText() + "'", ex);
      }
    }
  }

}