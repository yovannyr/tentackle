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

// $Id: FormQuestion.java 336 2008-05-09 14:40:20Z harald $
// Created on January 4, 2003, 9:07 PM

package org.tentackle.ui;

import java.awt.Component;
import org.tentackle.plaf.PlafGlobal;


/**
 * A question dialog.<br>
 * Replacement for JOptionDialog providing multiline messages (without
 * HTML-hacks) and improved keyboard handling.
 * 
 * @author harald
 */
public class FormQuestion extends FormDialog {
  
  /** key-chars for "yes" **/
  public static String yesKeys = Locales.bundle.getString("1YJ");
  /** key-chars for "no" **/
  public static String noKeys  = Locales.bundle.getString("0N");
  
  
  
  private boolean okFlag;           // true user answered "ok"
  private Component messageComp;    // the component showing the message
  
  
  /**
   * Creates a question dialog with a default message component.
   */
  public FormQuestion() {
    initComponents();
    messageComp = messagePanel;
  }
  
  
  /**
   * Shows the modal question dialog and waits for user's answer.
   * 
   * @param question the message to display (may contain newlines).
   * @param yes the text for the "yes"-button, null if default
   * @param no the text for "no"-button, null if default
   * @return true if user answered "yes", else "no"
   */
  public boolean showDialog (String question, String yes, String no) {
    if (question != null) {
      messageField.setText(question);
      messageField.setSize(messageField.getOptimalSize());
    }
    messageField.setBackground(buttonPanel.getBackground());
    if (yes != null) {
      yesButton.setText(yes);
    }
    if (no != null) {
      noButton.setText(no);
    }
    okFlag = false;
    pack();
    setVisible(true);
    return okFlag;
  }
  
  /**
   * Shows the modal question dialog and waits for user's "ok".
   * The message component must have been set before.
   */
  public boolean showDialog() {
    return showDialog(null, null, null); 
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
   * Creates a question dialog and waits for the user's answer.
   *
   * @param question the message to display (may contain newlines).
   * @param yes the text for the "yes"-button, null if default
   * @param no the text for "no"-button, null if default
   * @return true if user answered "yes", else "no"
   */
  public static boolean yesNo (String question, String yes, String no) {
    FormHelper.getEventQueue().dropKeyEvents();  // mintime for user to read
    return new FormQuestion().showDialog(question, yes, no);
  }


  /**
   * Creates a question dialog and waits for the user's answer.
   *
   * @param question the message to display (may contain newlines).
   * @return true if user answered "yes", else "no"
   */
  public static boolean yesNo (String question) {
    return (yesNo (question, null, null));
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
    yesButton = new org.tentackle.ui.FormButton();
    noButton = new org.tentackle.ui.FormButton();

    setAutoPosition(true);
    setModal(true);
    setTitle(Locales.bundle.getString("Frage")); // NOI18N
    addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(java.awt.event.KeyEvent evt) {
        formKeyTyped(evt);
      }
    });

    messagePanel.setLayout(new java.awt.GridBagLayout());

    iconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iconLabel.setIcon(PlafGlobal.getIcon("QuestionDialog"));
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

    yesButton.setIcon(PlafGlobal.getIcon("ok"));
    yesButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        yesButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(yesButton);

    noButton.setIcon(PlafGlobal.getIcon("cancel"));
    noButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        noButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(noButton);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void formKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyTyped
    // notice: works only if no component in the panel ever gets the keyboard focus!
    char key = Character.toUpperCase(evt.getKeyChar());
    if (yesKeys.indexOf(key) >= 0) {
      yesButton.doClick();
    }
    if (noKeys.indexOf(key) >= 0) {
      noButton.doClick();
    }
  }//GEN-LAST:event_formKeyTyped

  private void noButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noButtonActionPerformed
    dispose();
  }//GEN-LAST:event_noButtonActionPerformed

  private void yesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yesButtonActionPerformed
    okFlag = true;
    dispose();
  }//GEN-LAST:event_yesButtonActionPerformed
  

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JLabel iconLabel;
  private org.tentackle.ui.FormTextArea messageField;
  private javax.swing.JPanel messagePanel;
  private org.tentackle.ui.FormButton noButton;
  private org.tentackle.ui.FormButton yesButton;
  // End of variables declaration//GEN-END:variables
  
}