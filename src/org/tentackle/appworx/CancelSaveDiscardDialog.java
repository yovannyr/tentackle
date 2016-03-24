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

// $Id: CancelSaveDiscardDialog.java 336 2008-05-09 14:40:20Z harald $
// Created on January 4, 2003, 9:07 PM

package org.tentackle.appworx;

import java.awt.Component;
import java.awt.event.KeyEvent;
import org.tentackle.plaf.PlafGlobal;


/**
 * A dialog to prompt the user for cancel, save or discard.
 */
public class CancelSaveDiscardDialog extends org.tentackle.ui.FormDialog {
  

  // any other key than these invokes CANCEL
  /** key codes for "save" **/
  public static String saveKeys    = Locales.bundle.getString("saveKeys");
  /** key codes for "discard" **/
  public static String discardKeys = Locales.bundle.getString("discardKeys");
  
  
  /** return code for "cancel" **/
  public static final int CANCEL  = 0;
  /** return code for "save" **/
  public static final int SAVE    = 1;
  /** return code for "discard" **/
  public static final int DISCARD = 2;
  

  /**
   * Creates a question dialog and waits for user's answer.
   *
   * @param question the question text
   * @param discard the text of the discard button, null if default
   * @param save the text of the save button, null if default
   * @param cancel the text of the cancel button, null if default
   * @return {@link #CANCEL}, {@link #SAVE} or {@link #DISCARD}
   */
  public static int getAnswer (String question, String discard, String save, String cancel) {
    return new CancelSaveDiscardDialog().showDialog(question, discard, save, cancel);
  }


  /**
   * Creates a question dialog and waits for user's answer.
   *
   * @param question the question text
   * @return {@link #CANCEL}, {@link #SAVE} or {@link #DISCARD}
   */
  public static int getAnswer (String question) {
    return getAnswer (question, null, null, null);
  }
  
  
  /**
   * Creates the default "data has been modified"-dialog and waits for user's answer.
   *
   * @return {@link #CANCEL}, {@link #SAVE} or {@link #DISCARD}
   */
  public static int getAnswer() {
    return getAnswer(Locales.bundle.getString("data_has_been_modified!_Discard,_save_or_cancel?"));
  }
  

  
  
  
  
  private int answer;                 // the user's choice
  private Component messageComp;      // the message component if not the default
  
  
  public CancelSaveDiscardDialog() {
    initComponents();
    messageComp = messagePanel;
  }
  
  /**
   * Shows the modal dialog and returns the user's answer.
   * 
   * @param question the question text
   * @param discard the text of the discard button, null if default
   * @param save the text of the save button, null if default
   * @param cancel the text of the cancel button, null if default
   * @return {@link #CANCEL}, {@link #SAVE} or {@link #DISCARD}
   */
  public int showDialog (String question, String discard, String save, String cancel) {
    if (question != null) {
      messageField.setText(question);
      messageField.setSize(messageField.getOptimalSize());
    }
    if (discard != null) {
      discardButton.setText(discard);
    }
    if (save    != null) {
      saveButton.setText(save);
    }
    if (cancel  != null) {
      cancelButton.setText(cancel);
    }
    answer = CANCEL;
    pack();
    setVisible(true);
    return answer;
  }
  
  /**
   * Shows the modal dialog and returns the user's answer.<br>
   * If {@link #setMessageComponent} is used. 
   * 
   * @return {@link #CANCEL}, {@link #SAVE} or {@link #DISCARD}
   */
  public int showDialog() {
    return showDialog(null, null, null, null); 
  }
  
  
  /**
   * Replaces the default center panel with some other Component.
   * 
   * @param comp the component
   */
  public void setMessageComponent(Component comp)  {
    getContentPane().remove(messageComp);
    messageComp = comp;
    getContentPane().add(messageComp, java.awt.BorderLayout.CENTER);
  }
  
  /**
   * Gets the message component.
   * @return the component
   */
  public Component getMessageComponent()  {
    return messageComp; 
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
    discardButton = new org.tentackle.ui.FormButton();
    saveButton = new org.tentackle.ui.FormButton();
    cancelButton = new org.tentackle.ui.FormButton();

    setModal(true);
    setAutoPosition(true);
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

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    discardButton.setIcon(PlafGlobal.getIcon("ok"));
    discardButton.setText(Locales.bundle.getString("discard")); // NOI18N
    discardButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        discardButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(discardButton, gridBagConstraints);

    saveButton.setIcon(PlafGlobal.getIcon("save"));
    saveButton.setText(Locales.bundle.getString("save")); // NOI18N
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(saveButton, gridBagConstraints);

    cancelButton.setIcon(PlafGlobal.getIcon("cancel"));
    cancelButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(cancelButton, gridBagConstraints);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
    answer = SAVE;
    dispose();
  }//GEN-LAST:event_saveButtonActionPerformed

  private void formKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyTyped
    // notice: works only if no component in the panel ever gets the keyboard focus!
    char key = evt.getKeyChar();
    if (key != KeyEvent.CHAR_UNDEFINED) {
      if (discardKeys.indexOf(key) >= 0)  {
        discardButton.doClick();
      }
      else if (saveKeys.indexOf(key) >= 0)     {
        saveButton.doClick();
      }
      else  {
        cancelButton.doClick();
      }
    }
  }//GEN-LAST:event_formKeyTyped

  private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    dispose();
  }//GEN-LAST:event_cancelButtonActionPerformed

  private void discardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardButtonActionPerformed
    answer = DISCARD;
    dispose();
  }//GEN-LAST:event_discardButtonActionPerformed
  

  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel buttonPanel;
  private org.tentackle.ui.FormButton cancelButton;
  private org.tentackle.ui.FormButton discardButton;
  private javax.swing.JLabel iconLabel;
  private org.tentackle.ui.FormTextArea messageField;
  private javax.swing.JPanel messagePanel;
  private org.tentackle.ui.FormButton saveButton;
  // End of variables declaration//GEN-END:variables
  
}
