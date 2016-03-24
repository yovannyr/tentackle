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

// $Id: FormTelnoDialog.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.Telno;




/**
 * Telefon-Nummern Dialog
 *
 * @author harald
 * @version 1.0
 */
public class FormTelnoDialog extends FormDialog {

  private final FormTelnoField    telnoField = new FormTelnoField();   // telephone-field
  private int               telnoNum;     // number of telnos
  private String            newText;      // returned, possibly modified text
  private JLabel[]          labels;       // dynamically created labels
  private StringFormField[] telnos;       // dynamically created telno-Fields
  private StringFormField[] comments;     // dynamically created comment-Fields
  private FormButton[]      dialButtons;  // dynamically created dial-buttons
  private Process           process;      // to keep the reference... (java bug?)
  // initialization section
  private static final String TELNO_PROPERTIES = "telno.properties";    // NOI18N
  private static String     phoneCommand; // shell command to dial a phone-number
  private static String     faxCommand;   // shell command to dial a fax-number
  private static String     cellCommand;  // shell command to dial a mobile-phone
  private static String     modemCommand; // shell command to dial a modem-number
  //
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel numberPanel = new JPanel();
  private JPanel buttonPanel = new JPanel();
  private FormButton cancelButton = new FormButton();
  private FormButton okButton = new FormButton();
  private GridBagLayout numberLayout = new GridBagLayout();
  private StringFormField newTelnoStringField = new StringFormField();
  private StringFormField newCommentStringField = new StringFormField();
  private FormButton newButton = new FormButton();

  

  /**
   * load config file (once)
   *
   * P: dialcommand to phone...
   * F: fax
   * C: cell/mobile phone
   * M: modem
   *
   */
  static {
    // load configuration settings
    InputStream in = FormTelnoDialog.class.getResourceAsStream (TELNO_PROPERTIES);
    if (in != null) {
      try {
        PropertyResourceBundle rs = new PropertyResourceBundle (in);
        Enumeration keys = rs.getKeys();
        while (keys.hasMoreElements())  {
          String key  = (String)keys.nextElement();
          String prop = rs.getString(key);
          if (key.compareTo("" + FormTelnoField.PHONE) == 0) {
            phoneCommand = prop;
          }
          else if (key.compareTo("" + FormTelnoField.FAX) == 0)  {
            faxCommand = prop;
          }
          else if (key.compareTo("" + FormTelnoField.MODEM) == 0)  {
            modemCommand = prop;
          }
          else if (key.compareTo("" + FormTelnoField.CELL) == 0)  {
            cellCommand = prop;
          }
        }
        in.close();
      } catch (IOException e) {
        // not found, leave them 'null'
        UIGlobal.logger.warning("loading " + TELNO_PROPERTIES + " failed");
      }
    }
    else  {
      UIGlobal.logger.warning("no config-file: " + TELNO_PROPERTIES);
    }
  }



  



  /**
   * Creates a telno dialog.<br>
   * The window owner is determined from the telnoField.
   * 
   * @param telnoField the telno field, null if default
   * @return the dialog
   */
  public static FormTelnoDialog createFormTelnoDialog(FormTelnoField telnoField) {
    return new FormTelnoDialog(FormHelper.getParentWindow(telnoField), telnoField);
  }


  

  /**
   * Creates a telno dialog.
   * 
   * @param owner the owner window, null if none
   * @param telnoField the telno field, null if default
   */
  public FormTelnoDialog(Window owner, FormTelnoField telnoField) {
    
    super(owner);
    
    // make a copy, because we return a string if changed
    if (telnoField != null) {
      this.telnoField.setText(telnoField.getText());
      this.telnoField.setTelnoType(telnoField.getTelnoType());
      this.telnoField.setChangeable(telnoField.isChangeable());
      this.telnoField.setDefaultCountry(telnoField.getDefaultCountry());
    }

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    setTitle(this.telnoField.getText());

    initTelnos();

    newTelnoStringField.setColumns(15);
    newTelnoStringField.addValueListener(new ValueListener() {
      public void valueEntered(ValueEvent e)  {
        Telno no = new Telno();
        no.setDefaultCountry(FormTelnoDialog.this.telnoField.getDefaultCountry());
        if (no.setTelno(newTelnoStringField.getText()) == false)  {
          FormInfo.print(no.getError());
          newTelnoStringField.requestFocusLater();
        }
        else {
          newTelnoStringField.setText(no.getTelno());
        }
      }
      public void valueChanged(ValueEvent e)  {
      }
    });
    newCommentStringField.setColumns(20);
    newButton.setIcon(PlafGlobal.getIcon("new"));
    newButton.setMargin(new Insets(0, 5, 0, 5));
    newButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // append string to telno
        String newTelno   = newTelnoStringField.getText();
        String newComment = newCommentStringField.getText();
        if (newTelno != null && newTelno.trim().length() > 0) {
          String tel = buildTelno();
          if (tel.length() > 0) {
            tel += ", ";
          }
          tel += newTelno;
          if (newComment != null && newComment.trim().length() > 0) {
            tel += " " + newComment;
          }
          newTelnoStringField.clearText();
          newCommentStringField.clearText();
          FormTelnoDialog.this.telnoField.setText(tel);
          FormTelnoDialog.this.telnoField.getText();
          initTelnos();
          pack();
        }
      }
    });
  }

  /**
   * Creates a telno dialog.
   * 
   * @param telnoField the telno field, null if default
   */
  public FormTelnoDialog(FormTelnoField telnoField) {
    this (null, telnoField);
  }

  /**
   * Creates a telno dialog.
   */
  public FormTelnoDialog() {
    this (null);
  }

  


  /**
   * Shows the modal dialog.
   * 
   * @return the formatted phone number(s), null if none
   */
  public String showDialog()  {
    pack();
    setVisible(true);
    return newText;
  }





  private String buildTelno ()  {
    // construct newText
    StringBuilder text = new StringBuilder();
    for (int i=0; i < telnoNum; i++)  {
      String telno = telnos[i].getText();
      if (telno != null && telno.length() > 0) {
        if (i > 0) {
          text.append(", ");
        }
        text.append(telnos[i].getText());
        String comment = comments[i].getText();
        if (comment != null && comment.length() > 0)  {
          text.append(" " + comment);
        }
      }
    }
    return text.toString();
  }


  private void initTelnos() {

    telnoNum    = telnoField.getTelnoNum();
    labels      = new JLabel[telnoNum];
    telnos      = new StringFormField[telnoNum];
    comments    = new StringFormField[telnoNum];
    dialButtons = new FormButton[telnoNum];

    numberPanel.removeAll();

    int telnoNdx = 0;

    while (telnoNdx < telnoNum) {

      labels[telnoNdx]   = new JLabel("" + (telnoNdx + 1));
      
      final StringFormField tField = new StringFormField(telnoField.getTelno(telnoNdx).toString(), 15);
      tField.addValueListener(new ValueListener() {
        public void valueEntered(ValueEvent e)  {
          Telno no = new Telno();
          no.setDefaultCountry(telnoField.getDefaultCountry());
          if (no.setTelno(tField.getText()) == false) {
            FormInfo.print(no.getError());
            tField.requestFocusLater();
          }
          else  {
            tField.setText(no.getTelno());
          }
        }
        public void valueChanged(ValueEvent e)  {
        }
      });
      telnos[telnoNdx]   = tField;
      comments[telnoNdx] = new StringFormField(telnoField.getComment(telnoNdx), 20);

      if (telnoField.isChangeable() == false) {
        telnos[telnoNdx].setChangeable(false);
        comments[telnoNdx].setChangeable(false);
      }

      switch (telnoField.getTelnoType())  {
      case FormTelnoField.MODEM:
          dialButtons[telnoNdx]   = new FormButton(PlafGlobal.getIcon("modem"));
          if (modemCommand == null) {
        dialButtons[telnoNdx].setEnabled(false);
      }
          break;
      case FormTelnoField.FAX:
          dialButtons[telnoNdx]   = new FormButton(PlafGlobal.getIcon("fax"));
          if (faxCommand == null) {
        dialButtons[telnoNdx].setEnabled(false);
      }
          break;
      case FormTelnoField.CELL:
          dialButtons[telnoNdx]   = new FormButton(PlafGlobal.getIcon("cellphone"));
          if (cellCommand == null) {
        dialButtons[telnoNdx].setEnabled(false);
      }
          break;
      default:
          dialButtons[telnoNdx]   = new FormButton(PlafGlobal.getIcon("phone"));
          if (phoneCommand == null) {
        dialButtons[telnoNdx].setEnabled(false);
      }
      }

      dialButtons[telnoNdx].setMargin(new Insets(0, 5, 0, 5));
      dialButtons[telnoNdx].setActionCommand("" + telnoNdx);
      dialButtons[telnoNdx].addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // make sure that data is correct
          telnoField.setText(buildTelno());
          // actionCommand holds the index
          String dialNumber = telnoField.getTelno(new Integer(e.getActionCommand()).intValue()).toString();
          /**
           * here comes the system-dependant hooks
           */
          String command = null;
          switch (telnoField.getTelnoType())  {
            case FormTelnoField.MODEM:
              if (modemCommand != null) {
                command = modemCommand + " " + dialNumber;
              }
              break;
            case FormTelnoField.CELL:
              if (cellCommand != null) {
                command = cellCommand + " " + dialNumber;
              }
              break;
            case FormTelnoField.FAX:
              if (faxCommand != null) {
                command = faxCommand + " " + dialNumber;
              }
              break;
            default:
              if (phoneCommand != null) {
                command = phoneCommand + " " + dialNumber;
              }
              break;
          }
          if (command != null)  {
            try {
              UIGlobal.logger.fine(command);
              process = Runtime.getRuntime().exec(command);
              // don't wait for completion
            } catch (Exception ex) {
              // nothing to do, if dialing failes...
              UIGlobal.logger.warning("dialing " + telnoField.getTelnoType() + ":" + dialNumber + " failed");
            }
          }
        }
      });

      numberPanel.add(labels[telnoNdx], new GridBagConstraints(0, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
      numberPanel.add(telnos[telnoNdx], new GridBagConstraints(1, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
      numberPanel.add(comments[telnoNdx], new GridBagConstraints(2, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
      numberPanel.add(dialButtons[telnoNdx], new GridBagConstraints(3, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
      telnoNdx++;
    }

    if (telnoField.isChangeable())  {
      numberPanel.add(newTelnoStringField, new GridBagConstraints(1, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
      numberPanel.add(newCommentStringField, new GridBagConstraints(2, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
      numberPanel.add(newButton, new GridBagConstraints(3, telnoNdx, 1, 1, 0.0, 0.0
              ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    }
  }



  private void jbInit() throws Exception {
    this.setModal(true);
    this.setAutoPosition(true);
    this.getContentPane().setLayout(borderLayout1);
    cancelButton.setIcon(PlafGlobal.getIcon("cancel"));
    cancelButton.setMargin(new Insets(0, 5, 0, 5));
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    okButton.setIcon(PlafGlobal.getIcon("ok"));
    okButton.setMargin(new Insets(0, 5, 0, 5));
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    numberPanel.setLayout(numberLayout);
    this.getContentPane().add(numberPanel, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
  }


  private void okButton_actionPerformed(ActionEvent e) {
    newText = buildTelno();
    dispose();
  }


  private void cancelButton_actionPerformed(ActionEvent e) {
    newText = null;
    dispose();
  }

}