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

// $Id: PasswordDialog.java 347 2008-06-05 09:23:59Z harald $

package org.tentackle.appworx;

import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormQuestion;
import org.tentackle.util.Compare;
import org.tentackle.util.StringHelper;

/**
 * Password dialog.
 * <p>
 * The user is prompted for the old password and enters the
 * new password twice. The passwords are assumed to be
 * in MD5-format by default. Otherwise the method
 * encryptPassword must be overwritten.
 * 
 * @author harald
 */
public class PasswordDialog extends org.tentackle.ui.FormDialog {
  
  private boolean checkOld;         // true if check old password
  private boolean okFlag;           // false if user canceled the dialog
  private String oldPassword;       // the old (encrypted) password
  private String password;          // the new (encrypted) password
  private int passwordLength = 8;   // max. length of password (unencrypted)
  
  
  /**
   * Creates a password dialog.
   * 
   * @param oldPassword the old password (encrypted), null if none
   */
  public PasswordDialog(String oldPassword) {
    this.oldPassword = oldPassword;
    checkOld = oldPassword != null;
    initComponents();
    // cursor movement and ENTER-key
    FormHelper.setupDefaultBindings(oldPasswordField);
    FormHelper.setupDefaultBindings(new1PasswordField);
    FormHelper.setupDefaultBindings(new2PasswordField);
  }
  
  /**
   * Creates a password dialog and prompts for the new password.
   */
  public PasswordDialog() {
    this(null);
  }
  
  
  /**
   * Sets the length of the password fields.
   * This is the unencrypted length.
   * 
   * @param passwordLength the password length
   */
  public void setPasswordLength(int passwordLength) {
    this.passwordLength = passwordLength;
  }
  
  /**
   * Gets the length of the password fields.
   * This is the unencrypted length.
   * 
   * @return the password length, default is 8.
   */
  public int getPasswordLength() {
    return passwordLength;
  }
  
  
  
  /**
   * Shows the modal dialog.
   * 
   * @return true if user wants to change the password, false if user cancelled the dialog
   */
  public boolean showDialog()  {
    okFlag = false;
    oldPasswordField.setVisible(checkOld);
    oldPasswordLabel.setVisible(checkOld);
    oldPasswordField.setColumns(passwordLength);
    new1PasswordField.setColumns(passwordLength);
    new2PasswordField.setColumns(passwordLength);
    pack();
    setVisible(true);
    return okFlag;
  }
  
  
  /**
   * Gets the new password.
   * 
   * @return null if no password (cleared), else the (encrypted) password.
   */
  public String getPassword() {
    return password;
  }
  
  
  /**
   * Encrypts the password.
   * The default implementation calculates an MD5-hash.
   * 
   * @param password the password to be encrypted (may be null)
   * @return the encrypted password, null if password was null
   * @see StringHelper#md5sum(char[]) 
   */
  public String encryptPassword(char[] password) {
    return StringHelper.md5sum(password);
  }
  
  
  
  // checks the old password
  private boolean checkOldPassword()  {
    if (checkOld)  {
      String passwd = encryptPassword(oldPasswordField.getPassword());
      if (Compare.compare(passwd, oldPassword) != 0) {
        errorPanel.setErrors(new InteractiveError(Locales.bundle.getString("Old_password_does_not_match!")));
        return false;
      }
    }
    return true;  // ok
  }
  
  // encrypts the new password(s)
  private String encrypt(char[] pass) {
    if (pass != null && pass.length == 0) {
      pass = null;
    }
    String passwd = encryptPassword(pass);    
    if (pass != null)  {
      for (int i=0; i < pass.length; i++)  {
        pass[i] = 0;   // scratch from memory
      }
    }
    return passwd;
  }
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    passwordPanel = new org.tentackle.ui.FormPanel();
    dataPanel = new org.tentackle.ui.FormPanel();
    oldPasswordLabel = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    jLabel3 = new javax.swing.JLabel();
    oldPasswordField = new javax.swing.JPasswordField();
    new1PasswordField = new javax.swing.JPasswordField();
    new2PasswordField = new javax.swing.JPasswordField();
    errorPanel = new org.tentackle.appworx.TooltipAndErrorPanel();
    buttonPanel = new org.tentackle.ui.FormPanel();
    okButton = new org.tentackle.ui.FormButton();
    cancelButton = new org.tentackle.ui.FormButton();

    setAutoPosition(true);
    setHelpURL("#passworddialog");
    setTitle(Locales.bundle.getString("Change_Password")); // NOI18N
    setModal(true);

    passwordPanel.setLayout(new java.awt.BorderLayout());

    oldPasswordLabel.setText(Locales.bundle.getString("old_password:")); // NOI18N

    jLabel2.setText(Locales.bundle.getString("new_password:")); // NOI18N

    jLabel3.setText(Locales.bundle.getString("new_password_repeated:")); // NOI18N

    oldPasswordField.setColumns(8);

    new1PasswordField.setColumns(8);

    new2PasswordField.setColumns(8);

    javax.swing.GroupLayout dataPanelLayout = new javax.swing.GroupLayout(dataPanel);
    dataPanel.setLayout(dataPanelLayout);
    dataPanelLayout.setHorizontalGroup(
      dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(dataPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jLabel3)
          .addComponent(jLabel2)
          .addComponent(oldPasswordLabel))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(oldPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(new1PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(new2PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    dataPanelLayout.setVerticalGroup(
      dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(dataPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(oldPasswordLabel)
          .addComponent(oldPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(new1PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3)
          .addComponent(new2PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    passwordPanel.add(dataPanel, java.awt.BorderLayout.CENTER);
    passwordPanel.add(errorPanel, java.awt.BorderLayout.SOUTH);

    getContentPane().add(passwordPanel, java.awt.BorderLayout.CENTER);

    okButton.setIcon(PlafGlobal.getIcon("ok"));
    okButton.setText(Locales.bundle.getString("ok")); // NOI18N
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(okButton);

    cancelButton.setIcon(PlafGlobal.getIcon("cancel"));
    cancelButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(cancelButton);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    dispose();
  }//GEN-LAST:event_cancelButtonActionPerformed

  private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    if (checkOldPassword()) {
      password = encrypt(new1PasswordField.getPassword());
      String passwd2 = encrypt(new2PasswordField.getPassword());
      if (Compare.compare(password, passwd2) != 0) {
        errorPanel.setErrors(new InteractiveError(Locales.bundle.getString("New_passwords_don't_match!")));
      }
      else if (FormQuestion.yesNo(password == null ?
                                    Locales.bundle.getString("Really_clear_the_password?") :
                                    Locales.bundle.getString("Really_set_the_new_password?"))) {
        okFlag = true;
        dispose();
      }
    }
  }//GEN-LAST:event_okButtonActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormPanel buttonPanel;
  private org.tentackle.ui.FormButton cancelButton;
  private org.tentackle.ui.FormPanel dataPanel;
  private org.tentackle.appworx.TooltipAndErrorPanel errorPanel;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPasswordField new1PasswordField;
  private javax.swing.JPasswordField new2PasswordField;
  private org.tentackle.ui.FormButton okButton;
  private javax.swing.JPasswordField oldPasswordField;
  private javax.swing.JLabel oldPasswordLabel;
  private org.tentackle.ui.FormPanel passwordPanel;
  // End of variables declaration//GEN-END:variables
  
}
