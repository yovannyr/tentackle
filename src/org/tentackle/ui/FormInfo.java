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

// $Id: FormInfo.java 374 2008-07-25 12:58:09Z harald $
// Created on January 4, 2003, 9:07 PM

package org.tentackle.ui;

import java.awt.Component;
import java.awt.EventQueue;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.Logger;
import org.tentackle.util.Toolkit;



/**
 * An Info Dialog.<br>
 * Replacement for JOptionDialog providing multiline messages (without
 * HTML-hacks) and improved keyboard handling.
 * 
 * @author harald
 */
public class FormInfo extends FormDialog {
  
  
  private Component messageComp;        // the component showing the message
  
  
  /**
   * Creates an info dialog with a default message component.
   */
  public FormInfo() {
    initComponents();
    messageComp = messagePanel;
  }
  
  
  /**
   * Shows the info dialog and waits for user's "ok".
   * 
   * @param message the message to display (may contain newlines).
   * @param title the window title, null for default title
   * @param timeout the timeout in milliseconds for the dialog to disappear automatically. Also sets this
   * dialog to non-modal.
   */
  public void showDialog (String message, String title, final long timeout) {
    
    if (message != null) {
      messageField.setText(message);
      messageField.setSize(messageField.getOptimalSize());
    }
    setTitle(title);
    
    if (timeout > 0)  {
      setModal(false);
      buttonPanel.setVisible(false);
      new Thread() {
        @Override
        public void run() {
          try {
            sleep(timeout);
          } 
          catch (InterruptedException ex) {
            //
          }
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              FormInfo.this.dispose();
            }
          });
        };
      }.start();
    }
    
    pack();
    Toolkit.beep();  // beep
    setVisible(true);
  }
  
  
  
  /**
   * Shows the modal info dialog and waits for user's "ok".
   * The message component must have been set before.
   */
  public void showDialog() {
    showDialog(null, null, 0); 
  }
  
  
  
  
  /**
   * Sets the message component.<br>
   * Replaces the center panel with some other component.
   * 
   * @param comp the message component
   */
  public void setMessageComponent(Component comp)  {
    getContentPane().remove(messageComp);
    messageComp = comp;
    getContentPane().add(messageComp, java.awt.BorderLayout.CENTER);
  }
  
  /**
   * Gets the message component
   * @return the component
   */
  public Component getMessageComponent()  {
    return messageComp; 
  }
  
  
  
  
  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param logger the logger, null if none
   * @param timeout the timeout in milliseconds for the dialog to disappear automatically. Also sets this
   * dialog to non-modal.
   */
  static public void print (String message, Logger logger, long timeout) {
    if (logger != null)  {
      // log to console
      logger.info(message);
    }
    FormHelper.getEventQueue().dropKeyEvents();    // mintime for user to read
    new FormInfo().showDialog(message, Locales.bundle.getString("Hinweis"), timeout);
  }
  
  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param logger the logger, null if none
   */
  static public void print (String message, Logger logger)  {
    print(message, logger, 0);
  }
  
  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param log true if log the message via the default logger
   * @param timeout the timeout in milliseconds for the dialog to disappear automatically. Also sets this
   * dialog to non-modal.
   */
  static public void print (String message, boolean log, long timeout) {
    print(message, log ? UIGlobal.logger : null, timeout); 
  }
  
  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param log true if log the message via the default logger
   */
  static public void print (String message, boolean log)  {
    print(message, log, 0);
  }

  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param timeout the timeout in milliseconds for the dialog to disappear automatically. Also sets this
   * dialog to non-modal.
   */
  static public void print (String message, long timeout)  {
    print (message, false, timeout);
  }
  
  /**
   * Creates an info dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   */
  static public void print (String message) {
    print(message, false);
  }
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    messagePanel = new javax.swing.JPanel();
    iconLabel = new javax.swing.JLabel();
    messageField = new org.tentackle.ui.FormTextArea();
    buttonPanel = new javax.swing.JPanel();
    okButton = new org.tentackle.ui.FormButton();

    setAutoPosition(true);
    setModal(true);
    setTitle(Locales.bundle.getString("Hinweis")); // NOI18N
    messagePanel.setLayout(new java.awt.GridBagLayout());

    iconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iconLabel.setIcon(PlafGlobal.getIcon("InformationDialog"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    messagePanel.add(iconLabel, gridBagConstraints);

    messageField.setEditable(false);
    messageField.setLineWrap(true);
    messageField.setWrapStyleWord(true);
    messageField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    messageField.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    messagePanel.add(messageField, gridBagConstraints);

    getContentPane().add(messagePanel, java.awt.BorderLayout.CENTER);

    okButton.setClickOnEnter(true);
    okButton.setFormTraversable(true);
    okButton.setIcon(PlafGlobal.getIcon("ok"));
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(okButton);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    dispose();
  }//GEN-LAST:event_okButtonActionPerformed
  

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JLabel iconLabel;
  private org.tentackle.ui.FormTextArea messageField;
  private javax.swing.JPanel messagePanel;
  private org.tentackle.ui.FormButton okButton;
  // End of variables declaration//GEN-END:variables
  
}
