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

// $Id: FormError.java 473 2009-08-07 17:19:36Z harald $

// Created on January 4, 2003, 9:07 PM

package org.tentackle.ui;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import org.tentackle.util.Logger;
import org.tentackle.util.Logger.Level;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.ReflectionHelper;
import org.tentackle.util.Toolkit;



/**
 * An Error Dialog.<br>
 * Replacement for JOptionDialog providing multiline messages (without
 * HTML-hacks) and improved keyboard handling.
 * 
 * @author harald
 */
public class FormError extends FormDialog {
  
  private Component messageComp;      // the component showing the message
  
  
  /**
   * Creates an error dialog
   * with a default message component.
   */
  public FormError() {
    initComponents();
    messageComp = messagePanel;
  }
  
  
  /**
   * Shows the modal error dialog and waits for user's "ok".
   * 
   * @param message the message to display (may contain newlines).
   * @param title the window title, null for default title
   */
  public void showDialog(String message, String title) {
    if (message != null) {
      messageField.setText(message);
      messageField.setSize(messageField.getOptimalSize());
    }
    if (title != null) {
      setTitle(title);
    }
    pack();
    Toolkit.beep();  // beep
    setVisible(true);
  }
  
  /**
   * Shows the modal error dialog and waits for user's "ok".
   * The message component must have been set before.
   */
  public void showDialog() {
    showDialog(null, null); 
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
   * Creates an error dialog, shows a message and waits for user's ok.
   * 
   * @param message the error message
   * @param abort true if abort application
   * @param logger the logger, null if none
   */
  static public void print(String message, boolean abort, Logger logger) {
    
    if (message == null) {
      message = ReflectionHelper.getApplicationInvocationAsString();
    }
    
    if (logger != null) {
      if (abort) {
        logger.severe(message);
      }
      else {
        logger.warning(message);
      } 
    }
    FormHelper.getEventQueue().dropKeyEvents();  // mintime for user to read
    
    new FormError().showDialog(message, Locales.bundle.getString("Fehler"));
    
    if (abort) {
      abort(logger);
    }
  }
  
  
  /**
   * Creates an error dialog, shows a message and waits for user's ok.
   * The default logger is used.
   * 
   * @param message the error message
   * @param abort true if abort application
   */
  static public void print (String message, boolean abort)  {
    print(message, abort, UIGlobal.logger); 
  }

  /**
   * Creates an error dialog, shows a message and waits for user's ok.
   * The default logger is used. No abort.
   * 
   * @param message the error message
   */
  static public void print(String message)  {
    print (message, false);
  }
  
  
  

  /**
   * Prints an exception.<br>
   * The method checks for headless and does not show a dialog, just logs,
   * as apps sometimes contain shared error handling code between GUI- and daemons.
   * 
   * @param message the error message, null if {@link ReflectionHelper#getApplicationInvocationString()}
   * @param ex the exception, null if none
   * @param abort true if abort application
   * @param logger the logger
   */
  static public void printException(String message, Exception ex, boolean abort, Logger logger) {
    
    if (message == null) {
      message = ReflectionHelper.getApplicationInvocationAsString();
    }
    
    // show some meaningful message to the user
    String extraMsg = null;
    if (ex instanceof ApplicationException)  {
      extraMsg = ((ApplicationException)ex).getAllMessages();
    }
    else if (ex != null) {
      extraMsg = ex.getMessage();
    }
    
    if (extraMsg != null) {
      message += "\n" + extraMsg;
    }
    
    if (!UIGlobal.isHeadless) {
      // show some meaningful message to the user
      print(message);
    }
    
    if (logger != null) {
      // log detailed error message
      if (ex instanceof SQLException)  {
        SQLException se = (SQLException) ex;
        while (se != null) {
          message += "\n>>>SQL>>> " + ex.getMessage() +
               "\n>>>Code>> " + ((SQLException)ex).getErrorCode() +
               "\n>>>State> " + ((SQLException)ex).getSQLState();
          se = ((SQLException)ex).getNextException();
        }
      }

      if (ex != null) {
        // append stacktrace
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(os));
        message += "\n" + os.toString();
      }
      
      logger.log(abort ? Level.SEVERE : Level.WARNING, message, null);
    }
    
    if (abort)  {
      abort(logger);
    }
  }
  
  
  /**
   * Prints an exception using the default logger.<br>
   * 
   * @param message the error message, null if {@link ReflectionHelper#getApplicationInvocationString()}
   * @param ex the exception, null if none
   * @param abort true if abort application
   */
  static public void printException (String message, Exception ex, boolean abort)  {
    printException(message, ex, abort, UIGlobal.logger); 
  }

  /**
   * Prints an exception using the default logger, no abort<br>
   * 
   * @param message the error message, null if {@link ReflectionHelper#getApplicationInvocationString()}
   * @param ex the exception, null if none
   */
  static public void printException (String message, Exception ex) {
    printException (message, ex, false);
  }

  /**
   * Prints an exception using the default logger, default message.<br>
   * 
   * @param ex the exception, null if none
   * @param abort true if abort application
   */
  static public void printException (Exception ex, boolean abort) {
    printException(ex.getClass().getName(), ex, abort);
  }

  /**
   * Prints an exception using the default logger, no abort, default message<br>
   * 
   * @param ex the exception, null if none
   */
  static public void printException (Exception ex) {
    printException (ex, false);
  }
  
  
  
  
  private static void abort(Logger logger)  {
    if (logger != null) {
      // print stacktrace to logger
      CharArrayWriter writer = new CharArrayWriter();
      new Exception("Application Aborted! Stacktrace:").printStackTrace(new PrintWriter(writer));
      logger.severe(writer.toString());
    }
    throw new GUIRuntimeException("FormError");
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
    setTitle(Locales.bundle.getString("Fehler")); // NOI18N
    messagePanel.setLayout(new java.awt.GridBagLayout());

    iconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iconLabel.setIcon(PlafGlobal.getIcon("ErrorDialog"));
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
