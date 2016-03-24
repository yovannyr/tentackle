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

// $Id: SecurityDialog.java 425 2008-09-14 16:20:54Z harald $


package org.tentackle.appworx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import org.tentackle.db.DbObject;
import org.tentackle.ui.AcceptDenyCheckBox;
import org.tentackle.ui.FormDialog;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormTable;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.StringHelper;
import org.tentackle.util.TrackedArrayList;





/**
 * Dialog to edit security rules.<br>
 * The dialog edits the class rules in one tab and the optional object rules
 * in a second.
 * The user can create, modify, delete and re-arrange rules to setup
 * the desired ACLs.
 */
public class SecurityDialog extends FormDialog {
  
  /** transaction name for "save rules" **/
  public static final String TX_SAVE_RULES  = "save rules";
  
  /** database context **/
  protected ContextDb contextDb;
  /** managed class **/
  protected Class clazz;
  /** classname (basename or full) **/
  protected String className;
  /** managed object **/
  protected long id;
  /** panel for to edit the class rules **/
  protected AppDbObjectTablePanel clazzPanel;
  /** panel to edit the object rules **/
  protected AppDbObjectTablePanel objPanel;
  /** security rules for the class **/
  protected TrackedArrayList<Security> clazzList;
  /** security rules for the object **/
  protected TrackedArrayList<Security> objectList;
  /** permissiontype **/
  protected int permissionType;
  /** security manager **/
  protected SecurityManager manager;
  /** true if current user has the write permission to security rules **/
  protected boolean writeAllowed;
  /** true if clazz is an AppDbObject.class **/
  protected boolean isAppDbObjectClass;
  
  
  /**
   * Creates a dialog for editing security rule sets.<br>
   * The constructors are protected because the SecurityDialog should
   * be invoked indirectly by the SecurityManager.
   *
   * @param contextDb the database context
   * @param clazz   the class to set security rules for.
   *                (use org.tentackle.appworx.SecurityDialog.class if working on security itself)
   * @param permissionType the kind of permission set. If 0, will be determined from clazz (if AppDbObject)
   *                or set to {@link Security#TYPE_PROGRAM} otherwise.
   * @param id      he object id in clazz (if clazz is a AppDbObject) or null
   *                if all objects or clazz is not a AppDbObject.
   *
   * @throws ApplicationException is setup failed
   */
  protected SecurityDialog(ContextDb contextDb, int permissionType, Class clazz, long id)  throws ApplicationException {
    
    this.contextDb      = contextDb;
    this.permissionType = permissionType;
    this.clazz          = clazz;
    this.id             = id;
    
    setup();
  }
  
  
  
  /**
   * Gets the database context.
   * 
   * @return the database context
   */
  public ContextDb getContextDb() {
    return contextDb;
  }
  
  
  /**
   * Sets up the userlabel.
   * By default the userlabel gets the username from the UserInfo.
   */
  protected void setupUserLabel() {
    try {
      userLabel.setText(MessageFormat.format(Locales.bundle.getString("You_are_logged_in_as_{0}"), 
                        manager.getContextDb().getAppUserInfo().getUsername()));
    } 
    catch (ApplicationException ex) {
      // leave it as it is
    }
  }
  
  
  /**
   * Sets up the dialog.<br>
   * 
   * @throws ApplicationException if setup failed
   */
  @SuppressWarnings("unchecked")
  protected void setup() throws ApplicationException {
    
    try {
      
      isAppDbObjectClass = AppDbObject.class.isAssignableFrom(clazz);
      className = isAppDbObjectClass ? StringHelper.getClassBaseName(clazz) : clazz.getName();
      manager = contextDb.getAppUserInfo().getSecurityManager();
      writeAllowed = contextDb.getAppUserInfo().getSecurityManager().getSecurityDialogPrivilege().isAccepted();
      
      // if permission type not set, get it from clazz
      if (permissionType == 0)  {
        permissionType = isAppDbObjectClass ? 
            // unchecked:
            AppDbObject.newByClass(contextDb, (Class<AppDbObject>)clazz).permissionType() : 
            Security.TYPE_PROGRAM;
      }
      
      initComponents();
      setupUserLabel();
      
      // create empty instance of a Security object to load the rules
      Security sec = manager.newSecurityInstance();
      sec.setPermissionType(permissionType);
      
      // load class rules
      clazzList = sec.selectByObject(0, className);
      
      // create class rules table
      clazzPanel = new AppDbObjectTablePanel(sec.getFormTableEntry(), clazzList, true, "securityDialogTable");
      configureTable(clazzPanel.getFormTable());
      clazzPanel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (e.getActionCommand() == AppDbObjectTablePanel.ACTION_SAVE) {  // == is ok here
            doSave();
          }
          else  {
            doCancel();
          }
        }
      });
      
      // determine the tab name
      String tabName = StringHelper.getClassBaseName(clazz);
      if (permissionType != Security.TYPE_PROGRAM) {    // AppDbObject?
        try {
          tabName = ((Class<AppDbObject>)clazz).newInstance().getSingleName(); // unchecked
        }
        catch (Exception e) {
          // leave it as it is
        }
      }
      topicPane.addTab(tabName, clazzPanel);

      if (id != 0)  {   // object id given: load object rules
        // load object rules
        objectList = sec.selectByObject(id, className);
        // create object rules table
        objPanel = new AppDbObjectTablePanel(sec.getFormTableEntry(), objectList, true, "securityDialogTable");
        configureTable(objPanel.getFormTable());
        objPanel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == AppDbObjectTablePanel.ACTION_SAVE) { // == is ok here
              doSave();
            }
            else  {
              doCancel();
            }
          }
        });
        
        // determine the name of the object tab
        try {
          tabName = AppDbObject.selectCached(contextDb, (Class<AppDbObject>)clazz, id).toString(); // unchecked
        }
        catch (Exception e) {
          tabName = className + "[" + id + "]";
        }
        
        topicPane.addTab(tabName, objPanel); 
      }
      
      setFormValues();    // in case something else shown
      saveValues();       // to determine changes
    }
    
    catch (Exception ex)  {
      throw new ApplicationException(Locales.bundle.getString("setting_up_security_dialog_failed"), ex); 
    }
  }
  

  /**
   * Shows the (modal) dialog.
   */
  public void showDialog()  {
    pack();
    setVisible(true);
  }
  
  
  /**
   * Saves the rules.
   *
   * @return true if done, else false if error
   */
  protected boolean saveRules()  {
    
    boolean oldcommit = contextDb.getDb().begin(TX_SAVE_RULES);   // large transaction!
    boolean wasError  = false;
    
    // all class settings
    if (clazzList.isObjectRemoved()) {
      DbObject.deleteList(clazzList.getRemovedObjects());
    }
    int prioIndex = 0;
    for (Security sec: clazzList) {
      sec.setObjectClass(className);
      sec.setObjectId(0);
      sec.setPriority(prioIndex++);
      sec.setPermissionType(permissionType);
      if (sec.isModified() && sec.save() == false) {
        wasError = true;
        break;
      }
    }
    
    if (wasError == false && objectList != null) {
      // object settings
      if (objectList.isObjectRemoved()) {
        DbObject.deleteList(objectList.getRemovedObjects());
      }
      prioIndex = 0;
      for (Security sec: objectList) {
        sec.setPriority(prioIndex++);
        sec.setPermissionType(permissionType);
        sec.setObjectClass(className);
        sec.setObjectId(id);
        if (sec.isModified() && sec.save() == false) {
          wasError = true;
          break;
        }
      }
    }
    
    if (wasError) {
      contextDb.getDb().rollback(oldcommit);
      return false;
    }
    else  {
      contextDb.getDb().commit(oldcommit);
      return true;
    }
  }
  
  

  /**
   * {@inheritDoc}
   * <p>
   * Overritten so we can exit when window is closed:
   * do some stuff before exit.
   */
  @Override
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)  {
      doCancel();
      return;
    }
    super.processWindowEvent(e);
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to enable/disable the save-button.
   */
  @Override
  public void triggerValuesChanged()  {
    super.triggerValuesChanged();
    boolean enabled = areValuesChanged() && writeAllowed;
    clazzPanel.getSaveButton().setEnabled(enabled);
    if (objPanel != null) {
      objPanel.getSaveButton().setEnabled(enabled);
    }
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to update the save-button.
   */
  @Override
  public void saveValues()  {
    super.saveValues();
    clazzPanel.getSaveButton().setEnabled(false);
    if (objPanel != null) {
      objPanel.getSaveButton().setEnabled(false);
    }
  }

  
  
  /**
   * Configures a table.
   * 
   * @param table the table (object rules or class rules) 
   */
  protected void configureTable(FormTable table) {
    table.setDefaultRenderer(Boolean.class, AcceptDenyCheckBox.getTableCellRenderer());
    table.setDefaultEditor(Boolean.class, AcceptDenyCheckBox.getTableCellEditor());
    table.setSurrendersFocusOnKeystroke(true);
    table.setCellTraversal(FormTable.CELLTRAVERSAL_COLUMN | FormTable.CELLTRAVERSAL_WRAPINLINE);
  }
  
  
  
  /**
   * save the rules and print errormessage if failed.
   * Dispose if save ok.
   */
  private void doSave() {
    if (saveRules() == false) {
      FormError.print(Locales.bundle.getString("error_saving_rules"));
    }
    else  {
      dispose(); 
    }
  }
  
  

  /**
   * Cancels the dialog.
   */
  private void doCancel() {
    if (clazzPanel.isDataChanged() || 
        objPanel != null && objPanel.isDataChanged()) {
      int answer = CancelSaveDiscardDialog.getAnswer();
      if (answer == CancelSaveDiscardDialog.DISCARD) {
        dispose();
      }
      else if (answer == CancelSaveDiscardDialog.SAVE) {
        doSave();
      }
      else  {
        return;   // do nothing
      }
    }
    else  {
      dispose();
    }
  }
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    headerPanel = new org.tentackle.ui.FormPanel();
    userLabel = new javax.swing.JLabel();
    topicPane = new javax.swing.JTabbedPane();

    setAutoPosition(true);
    setHelpURL("#securitymanager");
    setModal(true);
    setTitle(Locales.bundle.getString("Security_Rules")); // NOI18N
    headerPanel.setLayout(new java.awt.GridBagLayout());

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 20);
    headerPanel.add(userLabel, gridBagConstraints);

    getContentPane().add(headerPanel, java.awt.BorderLayout.NORTH);

    getContentPane().add(topicPane, java.awt.BorderLayout.CENTER);

    pack();
  }// </editor-fold>//GEN-END:initComponents
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormPanel headerPanel;
  private javax.swing.JTabbedPane topicPane;
  protected javax.swing.JLabel userLabel;
  // End of variables declaration//GEN-END:variables
  
}
