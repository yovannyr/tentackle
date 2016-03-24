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

// $Id: LoginDialog.java 365 2008-07-19 16:21:00Z harald $
// Created on February 17, 2004, 4:40 PM

package org.tentackle.appworx;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.SplashScreen;
import javax.swing.Icon;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import org.tentackle.ui.FormHelper;

/**
 * A generic login dialog.
 * 
 * @author harald
 * @see Application
 */
public class LoginDialog extends org.tentackle.ui.FormDialog {
  
  private AppUserInfo ui;             // returned login information
  private boolean     waitLogin;      // true = login info not completed yet
  private boolean     ok;             // true if user pressed ok-Button
  private boolean     promptLogin;    // prompt user for login?
  private Icon        logo;           // logo icon
  private Object      monitor;        // locking object, null if getUserInfo invoked from dispatch thread
  
  
  /**
   * Creates a login dialog.
   *
   * @param ui the user info, null if default (see {@link AppUserInfo})
   * @param promptLogin true if prompt for username/password, else show only
   * @param logo the application's logo icon, null = tentackle default icon
   */
  public LoginDialog(AppUserInfo ui, boolean promptLogin, Icon logo) {
    this.ui = ui == null ? new AppUserInfo(null, null, null) : ui;
    this.promptLogin = promptLogin;
    this.logo = logo;
    
    initComponents();
    
    if (logo == null) {
      logo = new ImageIcon(getClass().getResource("/org/tentackle/appworx/images/loginLogo.png"));   // NOI18N
    }
    
    // layered pane has no layout manager, so we layout explicitly
    Dimension size = imageLabel.getPreferredSize();
    int width = size.width;
    int height = size.height;
    layeredPane.setPreferredSize(size);
    imageLabel.setBounds(0, 0, width, height);
    loginPanel.setBounds(0, 0, width, height);
    logoPanel.setBounds(0, 0, width, height);
    statusPanel.setBounds(0, 0, width, height);
    statusPanel.setVisible(false);
    int logoWidth = logo.getIconWidth();
    int logoHeight = logo.getIconHeight();
    logoLabel.setIcon(logo);
    logoLabel.setBounds((width/2 - logoWidth)/2, (height - logoHeight)/2, logoWidth, logoHeight);
  }

  /**
   * Creates a login dialog.
   * The user will be prompted for username/password if ui or ui.username
   * or ui.password is null.
   *
   * @param ui the user info, null if default (see {@link AppUserInfo})
   * @param logo the application's logo icon, null = tentackle default icon
   */
  public LoginDialog(AppUserInfo ui, Icon logo) {
    this(ui, ui == null || ui.getUsername() == null || ui.getPassword() == null, logo);
  }
  
  /**
   * Creates a login dialog with a default AppUserInfo, default Icon and prompts the
   * user for name/password.
   */
  public LoginDialog() {
    this(null, true, null);
  }
  
  
  /**
   * Displays the dialog and prompts for username and password.<br>
   * 
   * If the method is invoked from within the GUI thread, the
   * dialog is modal and will dispose after login data entered.
   * Otherwise the dialog remains open and showStatus() can be
   * used to further update the login process.
   *
   * @return  null if user aborts
   */
  public AppUserInfo getUserInfo()  {
    
    monitor = EventQueue.isDispatchThread() ? null : new Object();
    
    if (monitor != null) {
      // unblocking...
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          prepareDialog();
        }
      });
      
      // wait for data entered or a button pressed
      waitLogin = true;
      
      synchronized(monitor) {
        while (waitLogin) {
          try {
            monitor.wait();
          } 
          catch (InterruptedException ex) {
            // nothing to do
          }
        }
      }
      // login data entered...

      FormHelper.removeWindow(this);    // don't change GUI etc...

      EventQueue.invokeLater(new Runnable() {
        public void run() {
          setLoginPanelVisible(false);
          statusPanel.setVisible(true);          
        }
      });
    }
    else  {
      prepareDialog();    // wait here for dispose the modal dialog...
    }
    
    if (ok) {
      char[] pass = passField.getPassword();
      if (pass != null && pass.length == 0) {
        pass = null;
      }
      ui.setPassword(pass);
      ui.setUsername(nameField.getText());
      return ui;
    }
    
    return null;    // aborted
  }
  
  
  /**
   * Shows a message status.
   * 
   * @param status the message text
   */
  public void showStatus(final String status)  {
    if (monitor != null) {
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          setStatus(status);
        }
      });
    }
    else  {
      setStatus(status);
    }
  }
  
 
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden in case setUndecorated() did not work and/or
   * the window is closed somehow.
   */
  @Override
  protected void processWindowEvent(java.awt.event.WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      waitLogin = false;
    }
    super.processWindowEvent(e);    // this will close finally
  }
  
  
  
  private void setStatus(String status) {
    if (!statusPanel.isVisible()) {
      statusPanel.setVisible(true);
    }
    statusLabel.setText(status);
  }
  
  
  private void setLoginPanelVisible(boolean visible) {
    loginPanel.setVisible(visible);
    cancelStatusButton.setVisible(visible);   // remove 2nd statusbutton if loginpanel is invisible
  }
  

  private void prepareDialog() {
    
    nameField.setText(ui.getUsername());
    passField.setText(null);
    passField.setCaretPosition(0);
    
    if (promptLogin)  {
      setLoginPanelVisible(true);
      nameField.requestFocusLater();
    }
    else  {
      setLoginPanelVisible(false);
      statusPanel.setVisible(true);
      promptLogin = true;   // next round we will prompt again!
    }
    
    pack();
    
    SplashScreen splash = SplashScreen.getSplashScreen();
    if (splash != null) {
      splash.close();   // close splashscreen, if any
    }
      
    setModal(monitor == null);  // if invoked from within application: dispose if user/password entered
    
    ok = false;
    
    setVisible(true);           // this will block if in dispatch thread, else will not block!    
  }
  
  
  private void setOk(boolean ok) {
    this.ok = ok;
    waitLogin = false;
    if (monitor == null) {
      dispose();
    }
    else  {
      synchronized(monitor) {
        monitor.notifyAll();
      }
    }
  }
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    layeredPane = new javax.swing.JLayeredPane();
    imageLabel = new javax.swing.JLabel();
    loginPanel = new org.tentackle.ui.FormPanel();
    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    nameField = new org.tentackle.ui.StringFormField();
    passField = new javax.swing.JPasswordField();
    okButton = new org.tentackle.ui.FormButton();
    cancelLoginButton = new org.tentackle.ui.FormButton();
    statusPanel = new org.tentackle.ui.FormPanel();
    statusLabel = new javax.swing.JLabel();
    cancelStatusButton = new org.tentackle.ui.FormButton();
    logoPanel = new org.tentackle.ui.FormPanel();
    logoLabel = new javax.swing.JLabel();

    setAutoPosition(true);
    setUndecorated(true);
    layeredPane.setBackground(new java.awt.Color(2, 2, 206));
    layeredPane.setOpaque(true);
    imageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/tentackle/appworx/images/login.png")));
    imageLabel.setBounds(0, 0, -1, -1);
    layeredPane.add(imageLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

    loginPanel.setOpaque(false);
    jLabel1.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel1.setForeground(new java.awt.Color(204, 204, 255));
    jLabel1.setText(Locales.bundle.getString("Username:")); // NOI18N

    jLabel2.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel2.setForeground(new java.awt.Color(204, 204, 255));
    jLabel2.setText(Locales.bundle.getString("Password:")); // NOI18N

    nameField.setAutoSelect(true);
    nameField.setBackground(new java.awt.Color(204, 204, 255));
    nameField.setColumns(8);
    nameField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 255), 2));
    nameField.setFont(new java.awt.Font("SansSerif", 1, 12));

    passField.setBackground(new java.awt.Color(204, 204, 255));
    passField.setColumns(8);
    passField.setFont(new java.awt.Font("SansSerif", 1, 12));
    passField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 255), 2));
    passField.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        passFieldActionPerformed(evt);
      }
    });

    okButton.setBackground(new java.awt.Color(204, 204, 255));
    okButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
    okButton.setText(Locales.bundle.getString("login")); // NOI18N
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });

    cancelLoginButton.setBackground(new java.awt.Color(204, 204, 255));
    cancelLoginButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
    cancelLoginButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelLoginButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelLoginButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout loginPanelLayout = new javax.swing.GroupLayout(loginPanel);
    loginPanel.setLayout(loginPanelLayout);
    loginPanelLayout.setHorizontalGroup(
      loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, loginPanelLayout.createSequentialGroup()
        .addContainerGap(293, Short.MAX_VALUE)
        .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jLabel1)
          .addComponent(jLabel2))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(passField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addGap(41, 41, 41))
      .addGroup(loginPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(cancelLoginButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap(436, Short.MAX_VALUE))
    );
    loginPanelLayout.setVerticalGroup(
      loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, loginPanelLayout.createSequentialGroup()
        .addContainerGap(137, Short.MAX_VALUE)
        .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addGap(23, 23, 23)
        .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(passField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel2))
        .addGap(26, 26, 26)
        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(77, 77, 77)
        .addComponent(cancelLoginButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );
    loginPanel.setBounds(0, 0, 500, 355);
    layeredPane.add(loginPanel, javax.swing.JLayeredPane.PALETTE_LAYER);

    statusPanel.setOpaque(false);
    statusLabel.setForeground(java.awt.Color.orange);
    statusLabel.setText(Locales.bundle.getString("login...")); // NOI18N

    cancelStatusButton.setBackground(new java.awt.Color(204, 204, 255));
    cancelStatusButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
    cancelStatusButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelStatusButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelStatusButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
    statusPanel.setLayout(statusPanelLayout);
    statusPanelLayout.setHorizontalGroup(
      statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(statusPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(cancelStatusButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
        .addContainerGap())
    );
    statusPanelLayout.setVerticalGroup(
      statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
        .addContainerGap(317, Short.MAX_VALUE)
        .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(cancelStatusButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(statusLabel))
        .addContainerGap())
    );
    statusPanel.setBounds(0, 0, 500, 350);
    layeredPane.add(statusPanel, javax.swing.JLayeredPane.MODAL_LAYER);

    logoPanel.setLayout(null);

    logoPanel.setOpaque(false);
    logoLabel.setIconTextGap(0);
    logoPanel.add(logoLabel);
    logoLabel.setBounds(60, 110, 130, 145);

    logoPanel.setBounds(0, 0, 500, 350);
    layeredPane.add(logoPanel, javax.swing.JLayeredPane.POPUP_LAYER);

    getContentPane().add(layeredPane, java.awt.BorderLayout.CENTER);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void cancelStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelStatusButtonActionPerformed
    setOk(false);
  }//GEN-LAST:event_cancelStatusButtonActionPerformed

  private void cancelLoginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelLoginButtonActionPerformed
    setOk(false);
  }//GEN-LAST:event_cancelLoginButtonActionPerformed

  private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    setOk(true);
  }//GEN-LAST:event_okButtonActionPerformed

  private void passFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passFieldActionPerformed
    okButton.doClick();
  }//GEN-LAST:event_passFieldActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormButton cancelLoginButton;
  private org.tentackle.ui.FormButton cancelStatusButton;
  private javax.swing.JLabel imageLabel;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLayeredPane layeredPane;
  private org.tentackle.ui.FormPanel loginPanel;
  private javax.swing.JLabel logoLabel;
  private org.tentackle.ui.FormPanel logoPanel;
  private org.tentackle.ui.StringFormField nameField;
  private org.tentackle.ui.FormButton okButton;
  private javax.swing.JPasswordField passField;
  private javax.swing.JLabel statusLabel;
  private org.tentackle.ui.FormPanel statusPanel;
  // End of variables declaration//GEN-END:variables
  
}
