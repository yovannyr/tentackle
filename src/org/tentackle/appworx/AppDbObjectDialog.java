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

// $Id: AppDbObjectDialog.java 464 2009-07-18 19:02:05Z harald $


package org.tentackle.appworx;

import java.text.MessageFormat;
import org.tentackle.db.SqlHelper;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormContainer;
import org.tentackle.ui.FormDialog;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormInfo;
import org.tentackle.ui.FormPanel;
import org.tentackle.ui.FormQuestion;
import org.tentackle.print.PrintHelper;
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Date;
import java.util.List;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.RepaintManager;
import javax.swing.event.EventListenerList;
import org.tentackle.util.StringHelper;


/**
 * Edit dialog for {@link AppDbObject}s.
 * <p>
 * The dialog provides the following generic features:
 * <ul>
 * <li>edit</li>
 * <li>view-only</li>
 * <li>search (with QBF preset)</li>
 * <li>print</li>
 * <li>new</li>
 * <li>delete</li>
 * <li>save</li>
 * <li>load by id</li>
 * <li>edit security rules</li>
 * <li>copy object</li>
 * <li>insert object</li>
 * <li>duplicate object (insert a copy)</li>
 * <li>drag and drop</li>
 * <li>and more...</li>
 * </ul>
 * 
 * 
 * @author harald
 */
public class AppDbObjectDialog extends FormDialog implements DropTargetListener {
  
  /** global default: true if object should be cleared after save **/
  public static boolean defaultNewAfterSave = false;
  
  // transaction names
  /** tx name for save **/
  public static final String TX_SAVE    = "dialog save";
  /** tx name for delete **/
  public static final String TX_DELETE  = "dialog delete";
  
  // Action Strings
  /** action string for "new" **/
  public static final String ACTION_NEW    = "new";
  /** action string for "search" **/
  public static final String ACTION_SEARCH = "search";
  /** action string for "delete" **/
  public static final String ACTION_DELETE = "delete";
  /** action string for "save" **/
  public static final String ACTION_SAVE   = "save";
  /** action string for "cancel" **/
  public static final String ACTION_CANCEL = "cancel";
  /** action string for "previous" **/
  public static final String ACTION_PREVIOUS = "previous";
  /** action string for "next" **/
  public static final String ACTION_NEXT = "next";
  
  
  private static final String NO_ACCESS_PANEL = "noAccess";
  private static final String NO_INFO_PANEL   = "noInfo";
  private static final String DATA_PANEL      = "dataPanel";
  
  

  private ContextDb         db;                       // current database application context
  private ContextDb         baseContext;              // mininum context for this object
  private AppDbObject       dbObject;                 // database object being edited/shown
  private Class<? extends AppDbObject> dbObjectClass; // class of object
  private AppDbObject       lastObject;               // saved object
  private AppDbObjectPanel  dbObjectPanel;            // object's panel
  private EventListenerList listenerList = new EventListenerList(); // for actionPerformed-Listeners.
  private DataFlavor        dndFlavor;                // DnD Flavor
  private boolean           disposeOnDeleteOrSave;    // true if close dialog if delete or save pressed
  private boolean           noPersistance;            // true = dont save!
  private SecurityManager   manager;                  // Security Manager
  private boolean           newAfterSave = defaultNewAfterSave; // clearObject() after save() ?
  private List              objectList;               // List of objects connected to this dialog
  private int               objectListIndex = -1;     // index of current object in list (-1 if no list or not in list)
  
  
  // object permissions
  /** permission: true if updating object is allowed **/
  protected boolean writeAllowed;
  /** permission: true if viewing object is allowed **/
  protected boolean readAllowed;
  /** permission: true if deleting the object is allowed **/
  protected boolean deleteAllowed;
  /** permission: true if user may create a new object **/
  protected boolean instantiatable;
  
  
  


  /**
   * Creates a database object dialog.
   * <p>
   * Note: applications should use {@link Application#showEditDialog} instead, because
   * this will make use of dialog pooling!
   *
   * @param owner the owner window, null if no owner
   * @param dbObject is the database object
   * @param modal is true if modal, else false
   */
  protected AppDbObjectDialog(Window owner, AppDbObject dbObject, boolean modal) {
    
    super(owner, null, modal);

    enableEvents(AWTEvent.WINDOW_EVENT_MASK); // for process.. below

    initComponents();

    setTooltipDisplay(tipAndErrorPanel);

    buttonPanel.setBackground(PlafGlobal.dropFieldActiveColor);

    // create object's panel
    FormContainer panel = dbObject.newPanel();

    if (panel != null)  {
      // override the title if panel brings its own
      String title = panel.getTitle();
      if (title != null)  {
        setTitle(title);
      }
    }

    if (panel instanceof AppDbObjectPanel)  {
      // standard case
      dbObjectPanel = (AppDbObjectPanel)panel;
      dataPanel.add(dbObjectPanel, DATA_PANEL);
      ((CardLayout)dataPanel.getLayout()).show(dataPanel, DATA_PANEL);

      // tell that we are the parent
      dbObjectPanel.setAppDbObjectDialog(this);

      // set the object
      if (setObject(dbObject) == false && dbObject != null) {
        // Panel loeschen, falls was drin ist
        setObject(dbObject.newObject());
      }

      // create drop target. We use the button-panel because this is
      // seen on all types of dialogs.
      DropTarget target = new DropTarget (buttonPanel, this);
      target.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

      // create accepted data flavour
      dndFlavor = new DataFlavor(dbObject.getClass(), dbObject.getClassBaseName());

      buttonPanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          checkPopup(e);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
          checkPopup(e);
        }
      });
    }
    
    else  {
      // no AppDbObjectPanel?
      if (panel instanceof FormPanel) {
        // panel just shows the data, no editing possible
        dataPanel.add((FormPanel)panel, DATA_PANEL);
        /**
         * check read permission:
         * 1. object permissions first
         * 2. class permissions next
         */
        SecurityResult sr = dbObject.getSecurityResult(Security.READ);
        if (sr != null && sr.isDenied())  {
          noAccessLabel.setText(StringHelper.toHTML(sr.explain(
                  Locales.bundle.getString("you_don't_have_permission_to_view_the_data"))));
          ((CardLayout)dataPanel.getLayout()).show(dataPanel, NO_ACCESS_PANEL);
        }
        else  {
          ((CardLayout)dataPanel.getLayout()).show(dataPanel, DATA_PANEL);
          panel.setAllChangeable(false);
        }
      }
      else  {
        // no suitable panel found
        ((CardLayout)dataPanel.getLayout()).show(dataPanel, NO_INFO_PANEL);
      }

      // disable all buttons
      previousButton.setVisible(false);
      nextButton.setVisible(false);
      idButton.setVisible(false);
      browserButton.setVisible(false);
      newButton.setVisible(false);
      saveButton.setVisible(false);
      deleteButton.setVisible(false);
      searchButton.setVisible(false);
      printButton.setVisible(false);
    }
  }




  /**
   * Links to a list of objects.<p>
   * If this dialog is linked to a list of objects, two navigation
   * buttons appear to walk through the list.
   * @param objectList the list of objects, null if none (default)
   */
  public void setObjectList(List objectList) {
    this.objectList = objectList;
    updateObjectListIndex();
    setupButtons();
  }


  /**
   * Gets the linked objects list.
   * @return the list of objects, null if none (default)
   */
  public List getObjectList() {
    return objectList;
  }


  /**
   * Gets the object list index.
   * @return the index of the object being displayed in the objectList, -1 if not in list
   */
  public int getObjectListIndex() {
    return objectListIndex;
  }
  
  
  /**
   * Clears the object.<br>
   * Implemented by replacing the current object with a new object.
   * The current object is saved.
   * 
   * @see #getLastObject() 
   */
  public void clearObject () {
    if (dbObject != null) {
      // altes Object sichern
      lastObject = dbObject;
      // neues leeres Objekt erzeugen
      dbObject = lastObject.newObject();
      // Objekt setzen
      setObject (dbObject);
    }
  }

  
  /**
   * Returns whether the object is readable (viewable).
   * 
   * @return true if dialog consideres object as readable
   */
  public boolean isReadAllowed() {
    return readAllowed;
  }
  
  
  /**
   * Returns whether the object is writable (editable).
   * 
   * @return true if dialog consideres object as modifyable by the user
   */
  public boolean isWriteAllowed() {
    return writeAllowed;
  }
  
  
  /**
   * Returns whether the object is deletable.
   * 
   * @return true if dialog consideres object removeable by the user
   */
  public boolean isDeleteAllowed() {
    return deleteAllowed;
  }
  
  
  /**
   * Returns whether new objects can be created.
   * 
   * @return true if dialog consideres object as createable by the user
   */
  public boolean isCreateAllowed() {
    return instantiatable;
  }
  
  


  /**
   * Sets the object.
   * 
   * @param dbObject the database object (must be the same class as current object!)
   * @param requestFocus true if initial focus should be requested
   * @return true if object accepted
   * @see AppDbObjectPanel#setInitialFocus
   */
  public boolean setObject (AppDbObject dbObject, boolean requestFocus)  {
    
    if (dbObjectPanel == null)  {
      return false;
    }
    
    if (this.dbObject != null)  {
      dbObjectPanel.prepareCancel();  // do any cleanup if setobject called more than once!
      clearEditedBy();
    }
    
    // get the panel object to be displayed. Can be different (e.g. address translates to a company)
    if (dbObject != null) {
      dbObject = dbObject.getPanelObject();
      if (dbObject != null && dbObject.isNew() == false)  {
        // may be from cache: reload for sure
        AppDbObject oldObject = dbObject;
        dbObject = (AppDbObject)dbObject.reload();
        if (dbObject != null) {
          dbObject.copyLazyValues(oldObject);   // save lazy info
        }
      }
    }

    if (dbObject != null && (dbObjectClass == null || dbObjectClass == dbObject.getClass()))  {
      
      this.dbObject      = dbObject;
      this.db            = dbObject.getContextDb();
      this.dbObjectClass = dbObject.getClass();
      this.baseContext   = dbObject.getBaseContext();
      
      determinePermissions(dbObject);
      
      // get token lock, if not only show item
      if (dbObject.isNew() == false && writeAllowed)  {
        
        if (Hook.hook().getDialogPool().isSingleObjectEdit()) {
          // check if object is being edited by another dialog.
          final AppDbObjectDialog otherDialog = Hook.hook().getDialogPool().isObjectBeingEdited(dbObject, this);
          if (otherDialog != null)  {  // writeallowed means possibly softlock-token set!
            FormInfo.print(Locales.bundle.getString("Object_is_used_in_another_dialog._Please_finish_that_dialog_first!"));
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                otherDialog.toFront();
              }
            });
            writeAllowed = false;
          }
        }        
        
        if (writeAllowed) {
          if (dbObject.getUpdateEditedByTimeout() > 0)  {
            String msg = applyLock(dbObject);
            if (msg != null) {
              FormInfo.print(msg);
              // token refused: disable write and delete
              writeAllowed  = false;
              deleteAllowed = false;
            }
          }
        }
      }

      StringBuilder idText = new StringBuilder(Long.toString(dbObject.getId()));
      while(idText.length() < 8)  {
        idText.insert(0, ' ');
      }
      idButton.setText(idText.toString());

      updateObjectListIndex();
      setupButtons();
      
      if (isModal()) {
        disposeOnDeleteOrSave = true;  // need to setDisposeOnDeleteOrSave() AFTER setObject() if otherwise
      }
      else  {
        disposeOnDeleteOrSave = false; // need to setDisposeOnDeleteOrSave() AFTER setObject() if otherwise
      }
      
      if (readAllowed) {
        ((CardLayout)dataPanel.getLayout()).show(dataPanel, DATA_PANEL);
      }
      else          {
        ((CardLayout)dataPanel.getLayout()).show(dataPanel, NO_ACCESS_PANEL);
      }
      
      if (dbObjectPanel.setObject(dbObject) == false)  {
        // the panel refused the object for whatever reason.
        return false;
      }
      
      // set the window title
      String title = dbObjectPanel.getTitle();
      if (title != null) {
        setTitle(title);
      }

      // set initial focus
      if (requestFocus) {
        dbObjectPanel.setInitialFocus();
      }
      
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          saveValues(); // remember values for check for change
        }
      });
      
      tipAndErrorPanel.clearErrors();   // clear any errors in tip-panel
      
      return true;  // object accepted
    }

    clearObject();
    
    return false;   // object refused
  }

  
  /**
   * Sets the object and requests the initial focus.
   * 
   * @param dbObject the database object (must be the same class as current object!)
   * @return true if object accepted
   * @see #setObject(org.tentackle.appworx.AppDbObject, boolean) 
   */
  public boolean setObject (AppDbObject dbObject) {
    return setObject(dbObject, true);
  }


  /**
   * Gets the current object
   * @return the current object
   */
  public AppDbObject getObject()  {
    return dbObject;
  }

  
  /**
   * Gets the last object (after save(), delete(), etc...).
   * 
   * @return the previous (last) object
   */
  public AppDbObject getLastObject() {
    return lastObject;
  }

  
  /**
   * Gets the object panel.
   * 
   * @return the database object panel
   */
  public AppDbObjectPanel getObjectPanel()  {
    return dbObjectPanel;
  }
  
  
  /**
   * Gets the object's class.
   * 
   * @return the database object class
   */
  public Class<? extends AppDbObject> getDbObjectClass() {
    return dbObjectClass;
  }


  /**
   * Shows the dialog.<br>
   * 
   * The dialog can be either modal or non-modal. If modal the method
   * blocks until the dialog is closed.
   * 
   * @param noPersistance true if NO changes must be made to persistance layer
   * @return the current object
   */
  public AppDbObject showDialog (boolean noPersistance) {
    this.noPersistance = noPersistance;
    disableButtonsIfNoPersistance();
    pack();
    setVisible(true);
    return dbObject;
  }

  
  /**
   * Shows the dialog.<br>
   * 
   * The dialog can be either modal or non-modal. If modal the method
   * blocks until the dialog is closed.
   * 
   * @return the current object
   */
  public AppDbObject showDialog ()  {
    return showDialog(false);
  }
  
  
  /**
   * Returns whether the object is cleared after save.
   * 
   * @return true if set new object after save
   */
  public boolean isNewAfterSave() {
    return newAfterSave;
  }  

  /**
   * Sets whether the object is cleared after save.
   * 
   * @param newAfterSave  true if set new object after save
   * @see #defaultNewAfterSave
   */
  public void setNewAfterSave(boolean newAfterSave) {
    this.newAfterSave = newAfterSave;
  }
  

  /** 
   * Returns whether the dialog is disposed after delete or save.
   * 
   * @return true if dispose after delete or save
   */
  public boolean isDisposeOnDeleteOrSave() {
    return disposeOnDeleteOrSave;
  }  
  
  /** 
   * Sets whether the dialog will be disposed after delete or save.
   * 
   * @param disposeOnDeleteOrSave  true if dispose after delete or save
   */
  public void setDisposeOnDeleteOrSave(boolean disposeOnDeleteOrSave) {
    this.disposeOnDeleteOrSave = disposeOnDeleteOrSave;
  }  
  

  /**
   * Gets the tooltip- and error-panel.<br>
   * By default the panel is invisible.
   * 
   * @return the tooltip and error panel
   */
  public TooltipAndErrorPanel getTooltipAndErrorPanel() {
    return tipAndErrorPanel;
  }
  
  
  /**
   * Adds an action listener that will be invoked as soon as
   * delete, save or cancel has been pressed.
   * 
   * @param listener the listener to add
   */
  public synchronized void addActionListener (ActionListener listener)  {
    listenerList.add (ActionListener.class, listener);
  }

  
  /**
   * Removes an action Listener.
   * 
   * @param listener the listener to remove
   */
  public synchronized void removeActionListener (ActionListener listener) {
     listenerList.remove (ActionListener.class, listener);
  }

  
  /**
   * Removes all Listeners.
   */
  public synchronized void removeAllListeners ()  {
    listenerList = new EventListenerList(); // this will move all to GC
  }

  
  /**
   * Notifies all Listeners that some button was pressed.
   * @param e the action event
   */
  public void fireActionPerformed (ActionEvent e) {
    ActionEvent evt = null;
    Object[] listeners = listenerList.getListenerList();
    if (listeners != null)  {
      for (int i = listeners.length-2; i >= 0; i -= 2)  {
        if (listeners[i] == ActionListener.class)  {
          if (evt == null)  {
            evt = new ActionEvent (this, e.getID(), e.getActionCommand());
          }
          ((ActionListener)listeners[i+1]).actionPerformed(evt);
        }
      }
    }
  }


  // ----------------------------- implements DropTargetListener ---------------

  public void dragEnter (DropTargetDragEvent event)  {
    if (!isDragAcceptable(event)) {
      event.rejectDrag();
    }
    /**
     * @todo: check that bug in Win32-JVM is really fixed.
     * see: http://developer.java.sun.com/developer/bugParade/bugs/4217416.html
     * --> fixed against 1.3.1
     */
    else  {
      event.acceptDrag(DnDConstants.ACTION_COPY);
    }
  }

  public void dragExit (DropTargetEvent event)  {
  }

  public void dragOver (DropTargetDragEvent event)  {
    if (!isDragAcceptable(event)) {
      event.rejectDrag();
    }
    // see comment above!
    else  {
      event.acceptDrag(DnDConstants.ACTION_COPY);
    }
  }
  
  public void dropActionChanged (DropTargetDragEvent event)  {
  }
  
  public void drop (DropTargetDropEvent event)  {
    if (isDropAcceptable(event)) {
      event.acceptDrop(DnDConstants.ACTION_COPY);
      Transferable trans = event.getTransferable();
      try {
        Object transferData = trans.getTransferData(dndFlavor);
        if (transferData instanceof AppDbObjectTransferData) {
          AppDbObject droppedObject = ((AppDbObjectTransferData)transferData).getAppDbObject(db.getDb());
          if (droppedObject != null)  {
            setObject(droppedObject);
          }
        }
      } catch (Exception e) {
        FormError.printException(Locales.bundle.getString("Drop_error:"), e);
      }
      event.dropComplete(true);
    }
    else  {
      event.rejectDrop();
    }
  }




  
  

  
  
  /**
   * {@inheritDoc}
   * <p>
   * Adds " (modal)" to the title if dialog is modal 
   */
  @Override
  public void setTitle(String title) {
    if (isModal())  {
      if (title == null) {
        title = Locales.bundle.getString("(modal)");
      }
      else {
        title += " " + Locales.bundle.getString("(modal)");
      }
    }
    super.setTitle(title);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to update the buttons.
   */
  @Override
  public void setAllChangeable(boolean allChangeable)  {
    super.setAllChangeable(allChangeable);
    setupButtons();
  }

  
  @Override
  public boolean areValuesChanged() {
    // pasted objects are always changed
    return dbObject != null && dbObject.getCopiedObject() != null || super.areValuesChanged();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to enable/disable the save-button and the {@link TooltipAndErrorPanel}.
   */
  @Override
  public void triggerValuesChanged()  {
    super.triggerValuesChanged();
    if (saveButton.isVisible()) {
      saveButton.setEnabled(noPersistance || (writeAllowed && areValuesChanged()));
      if (saveButton.isEnabled() == false)  {
        /**
         * remove pending errors from tooltippanel.
         * This code gets called if saveButton is disabled due to a change
         * in a field that previously caused an error an now is set back
         * to the old (non-error) value. In this case prepareSave() never
         * gets called (actionPerformed of saveButton is not invoked), so
         * the errors would remain displayed, which confuses the user.
         */
        TooltipAndErrorPanel ttPanel = getTooltipAndErrorPanel();
        if (ttPanel != null) {
          ttPanel.clearErrors();
        }
      }
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
    saveButton.setEnabled(noPersistance || dbObject.getCopiedObject() != null); 
    // always enabled if noPersistance to enable prepareSave-check
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to remove all Listeners.
   */
  @Override
  public void dispose() {
    super.dispose();
    removeAllListeners();
    objectList = null;
  }
  
  


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to catch WINDOW_CLOSING event.
   */
  @Override
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)  {
      closeButton.doClick();
    }
    else {
      super.processWindowEvent(e);
    }
  }

  
  
  /**
   * Determines the permissions readAllowed, writeAllowed, deleteAllowed
   * and loads the SecurityManager.<br>
   * The permissions will be determined "lazily" where possible.
   * They will be verified again in the TX.
   * @param dbObject the database object
   */
  protected void determinePermissions(AppDbObject dbObject) {
    if (dbObject == null) {
      // no object, no permissions.
      readAllowed     = false;
      writeAllowed    = false;
      deleteAllowed   = false;
      instantiatable  = false;
      manager         = null;
    }
    else  {
      // default rules from OR-layer
      instantiatable = dbObject.isInstantiatable();
      readAllowed    = dbObject.isShowableLazy();
      writeAllowed   = readAllowed && isAllChangeable() && dbObject.isEditableLazy();
      deleteAllowed  = readAllowed && isAllChangeable() && dbObject.isRemovableLazy();        

      // ask the security manager.
      SecurityResult sr = null;
      try {
        manager = dbObject.getSecurityManager();
        sr = manager.privilege(dbObject, baseContext, Security.READ);
        // display reason to user if access is denied for this object
        if (sr != null && sr.isDenied())  {
          noAccessLabel.setText(StringHelper.toHTML(
                  sr.explain(Locales.bundle.getString("you_don't_have_permission_to_view_the_data"))));
          readAllowed   = false;
          writeAllowed  = false;
          deleteAllowed = false;
        }
        // other denied permissions silently disable buttons only
        if ((writeAllowed || deleteAllowed) && 
            manager.privilege(dbObject, baseContext, Security.WRITE).isDenied())  {
          writeAllowed  = false;
          deleteAllowed = false;
        }
      } 
      catch (Exception ex) {
        // missing security-manager == GRANT
        manager = null;
      }       
    }
  }
  
  
  /**
   * Updates the write permission.<br>
   * Invoked before performing the TX.
   * @param object the database object
   * @return true if permission granted
   */
  protected boolean updateWritePermission(AppDbObject object) {
    if (writeAllowed) {
      writeAllowed = db.getDb().isAutoCommit() ? dbObject.isEditableLazy() : dbObject.isEditable();
      if (writeAllowed && manager != null && manager.privilege(dbObject, baseContext, Security.WRITE).isDenied()) {
        writeAllowed  = false;
        deleteAllowed = false;
      }
    }
    return writeAllowed;
  }
  
  
  /**
   * Updates the delete permission.<br>
   * Invoked before performing the TX.
   * @param object the database object
   * @return true if permission granted
   */
  protected boolean updateDeletePermission(AppDbObject object) {
    if (deleteAllowed) {
      deleteAllowed = db.getDb().isAutoCommit() ? dbObject.isRemovableLazy() : dbObject.isRemovable();
      if (deleteAllowed && manager != null && manager.privilege(dbObject, baseContext, Security.WRITE).isDenied()) {
        deleteAllowed = false;
        writeAllowed  = false;
      }
    }
    return deleteAllowed;
  }
  
  
  /**
   * Applies a token lock to the object.
   * This method is invoked from setObject() and is only
   * provided to be overridden (see poolkeeper.replication).
   *
   * @param object the object to apply the lock on
   * @return null if applied, else an errormessage.
   */
  protected String applyLock(AppDbObject object) {
    if (dbObject.updateEditedBy(SqlHelper.now(object.getUpdateEditedByTimeout())) == false) {
      // create the error message
      Date editedSince = dbObject.getEditedSince();
      if (editedSince == null) {
        editedSince = new Date();
      }
      return MessageFormat.format(Locales.bundle.getString("object_{0}_being_edited_since_{1}_by_{2}"), 
                                  dbObject.getSingleName(), StringHelper.shortTimestampFormat.format(editedSince), dbObject.getEditedByObject());
    }
    else  {
      return null;
    }
  }
  
  
  /**
   * Revokes a token lock to the object that has not been saved.
   * This method is is only provided to be overridden (see poolkeeper.replication).
   * 
   * @param object the object to revoke the lock from
   * @return true if a lock has been revoked or there was no lock, false if removing the lock failed
   */
  protected boolean revokeLock(AppDbObject object) {
    return object.updateEditedBy(null);
  }
  
  
  /**
   * Saves the object.
   * Default implementation simply updates the write permission
   * and invokes object.save().
   * @param object the database object
   * @return true if saved
   */
  protected boolean saveObject(AppDbObject object) {
    return updateWritePermission(dbObject) && object.save(); // this will also release editedBy-token if set!
  }
  

  /**
   * Saves the current object.<br>
   * 
   * Will start a transaction, check for prepare, apply the editedby token, 
   * save and remove the token, commit -- or rollback.
   * If the update fails, the user will be told so in a message window.
   * The update may fail due to the following reasons:
   * <ol>
   * <li>unique violation</li>
   * <li>serial-no has changed</li>
   * <li>write permission revoked</li>
   * <li>the lock timed out and another user grabbed the object</li>
   * <li>object modified in the meantime (if no lock)</li>
   * </ol>
   * 
   * @return true if saved
   */
  protected boolean save()  {
    
    // we are starting the transaction here to allow prepareSave() for saving and
    // updating related objects
    boolean oldcommit = dbObject.getDb().begin(TX_SAVE);
    
    if (dbObjectPanel.prepareSave() == true)  {
      
      if (noPersistance)  {
        dbObject.getDb().rollback(oldcommit);
        return true;
      }
      
      long editedBy = dbObject.getEditedBy();   // thats me, if I held a token. Else 0.
      
      if (saveObject(dbObject))  { 
        // saving ok.
        lastObject = dbObject;          // save bec. could be cleared by clearObject below
        dbObject.getDb().commit(oldcommit);
        return true;    // DONE!
      }
      
      /**
       * Update or insert failed.
       *
       * a) unique violation
       * b) serial-no has changed
       * c) write permission revoked
       * d) the lock timed out and another user grabbed the object
       * e) object modified in the meantime (if no lock)
       */
      
      dbObject.getDb().rollback(oldcommit);   // rollback before user-message
      
      if (writeAllowed == false) {
        FormError.print(MessageFormat.format(Locales.bundle.getString("{0}_lost_its_write_permission"), dbObject.getSingleName()));
      }
      else if (dbObject.getDb().isUniqueViolation())  {
        FormError.print(MessageFormat.format(Locales.bundle.getString("{0}_already_exists"), dbObject.getSingleName()));
      }
      else  {
        if (dbObject.getEditedBy() != editedBy) {
          if (dbObject.getEditedBy() != 0) {
            FormError.print(MessageFormat.format(
                    Locales.bundle.getString("{0}_locked_by_{1}_meanwhile._Your_lock_timed_out."), 
                                             dbObject.getSingleName(), dbObject.getEditedByObject()));
          }
          else  {
            // some other error: usually db error
            FormError.print(MessageFormat.format(Locales.bundle.getString("{0}_could_not_be_saved"), dbObject.getSingleName()));
          }
        }
        else  {
          if (editedBy != 0) {
            // our token timed out and after that another user edited the object
            FormError.print(MessageFormat.format(
                    Locales.bundle.getString("{0}_modified_by_another_user_meanwhile._Your_lock_timed_out."), dbObject.getSingleName()));            
          }
          else  {
            FormError.print(MessageFormat.format(
                    Locales.bundle.getString("{0}_modified_by_other_user_meanwhile"), dbObject.getSingleName()));
          }
        }
        Object transData = dbObject.getTransientData();
        AppDbObject obj = (AppDbObject)dbObject.reload();
        if (obj != null) {
          obj.setTransientData(transData);
          obj.copyLazyValues(dbObject);
          setObject(obj);
        }
      }
    }
    else  {
      // prepareSave failed
      dbObject.getDb().rollback(oldcommit);
    }
    
    return false;
  }

  
  /**
   * Deletes the current object.
   * The default implementation updates the delete permission and then deletes.
   * @param dbObject the database object to delete
   * @return true if deleted
   */
  protected boolean deleteObject(AppDbObject dbObject) {
    return updateDeletePermission(dbObject) && dbObject.delete();
  }
  
  
  /**
   * Deletes the current database object.
   * Will start a transaction, prepares thre delete and deletes the object.
   * @return true if object deleted
   */
  protected boolean delete()  {
    boolean oldcommit = dbObject.getDb().begin(TX_DELETE);
    if (dbObjectPanel.prepareDelete() &&
        deleteObject(dbObject)) {
      // DONE!
      dbObject.getDb().commit(oldcommit);
      return true;
    }
    dbObject.getDb().rollback(oldcommit);
    return false;
  }
  
  
  

  private void updateObjectListIndex() {
    if (objectList != null && dbObject != null) {
      objectListIndex = objectList.indexOf(dbObject);
    }
    else  {
      objectListIndex = -1;
    }
  }

  
  
  /**
   * disable buttons if noPersistance is enabled
   */
  private void disableButtonsIfNoPersistance() {
    if (noPersistance)  {
      previousButton.setVisible(false);
      nextButton.setVisible(false);
      browserButton.setVisible(false);
      deleteButton.setVisible(false);
      idButton.setVisible(false);
      newButton.setVisible(false);
      printButton.setVisible(false);
      searchButton.setVisible(false);
      securityButton.setVisible(false);
    }    
  }
  

  /**
   * sets visibility of buttons
   */
  private void setupButtons()  {
    
    if (dbObject != null) {

      if (objectList != null && objectList.size() > 1) {
        previousButton.setVisible(true);
        nextButton.setVisible(true);
        previousButton.setEnabled(objectListIndex > 0);
        nextButton.setEnabled(objectListIndex < objectList.size() - 1);
      }
      else  {
        previousButton.setVisible(false);
        nextButton.setVisible(false);
      }
      
      // read-allowed as an additional attribute!
      deleteButton.setEnabled(deleteAllowed && !dbObject.isNew());
      browserButton.setEnabled(readAllowed && !dbObject.isNew());
      saveButton.setEnabled(writeAllowed);
      printButton.setEnabled(readAllowed);
      printButton.setVisible(true);
      securityButton.setEnabled(true);    // visibility is controlled below
      idButton.setEnabled(true);          // dto.
      
      boolean visible = false;
      try {
        visible = manager.isEnabled() && manager.getSecurityDialogPrivilege().isAccepted();
      } 
      catch (Exception ex) {}   // missing is ok, i.e. no security
      
      securityButton.setVisible(visible);
      
      boolean allChangeable = isAllChangeable();
      if (isModal()) {
        newButton.setVisible(allChangeable && instantiatable);
        deleteButton.setVisible(allChangeable && instantiatable);
        searchButton.setVisible(allChangeable);
        idButton.setVisible(allChangeable);
      }
      else  {
        newButton.setVisible(instantiatable);
        deleteButton.setVisible(instantiatable);
        searchButton.setVisible(true);
        idButton.setVisible(true);
        
        newButton.setEnabled(allChangeable);
        searchButton.setEnabled(true);        
      }
      saveButton.setVisible(allChangeable); 
      browserButton.setVisible(true);
      
      disableButtonsIfNoPersistance();
    }      

    else  {
      // no buttons except cancel
      previousButton.setVisible(false);
      nextButton.setVisible(false);
      securityButton.setVisible(false);
      deleteButton.setVisible(false);
      browserButton.setVisible(false);
      saveButton.setVisible(false);
      printButton.setVisible(false);
      searchButton.setVisible(false);
      idButton.setVisible(false);
      newButton.setVisible(false);
    }
    
    closeButton.setEnabled(true);  // always visible and enabled
  }

  
  /**
   * asks user whether cancel is okay or not if data has been modified.
   *
   * @return SAVE, DISCARD or CANCEL
   */
  private int discardOk() {
    if (writeAllowed == false ||
        areValuesChanged() == false) {
      // discard always ok if not modified
      return CancelSaveDiscardDialog.DISCARD;
    }
    // ask user
    return CancelSaveDiscardDialog.getAnswer();
  }
  
  
  /**
   * Closes the dialog.
   */
  private void doClose (ActionEvent e) {
    if (dbObjectPanel != null)  {
      int answer = discardOk();
      if (answer == CancelSaveDiscardDialog.SAVE) {
        if (doSave(e)) {
          dispose();
        }
      }
      else if (answer == CancelSaveDiscardDialog.DISCARD && dbObjectPanel.prepareCancel())  {
        clearEditedBy();  // Bearbeitungsinfo löschen, falls Option eingeschaltet
        lastObject = dbObject;
        dbObject   = null;
        fireActionPerformed (e);
        dispose();
      }
      // else: answer was CANCEL, i.e. stay in form!
    }
    else  {
      dispose();
    }
  }

  
  /**
   * runs the save operation
   */
  private boolean doSave(ActionEvent evt)  {
    if (save()) {
      fireActionPerformed (evt);
      if (disposeOnDeleteOrSave)  {
        dispose();
      }
      else  {
        if (newAfterSave) {
          clearObject();
        }
        else  {
          setObject(getObject());
        }
      }
      return true;
    }
    return false;
  }
  


  
  /**
   * löscht editedBy falls notwendig
   */
  private void clearEditedBy()  {
    if (dbObject != null &&
        dbObject.isNew() == false &&
        dbObject.getUpdateEditedByTimeout() > 0 &&
        dbObject.getEditedBy() != 0)  {
      revokeLock(dbObject);
    }
  }


  
  /**
   * check for Popup
   */

  private void checkPopup(InputEvent e) {
    if (e instanceof MouseEvent && ((MouseEvent)e).isPopupTrigger())  {
      Point p = ((MouseEvent)e).getPoint();
      copyItem.setEnabled(dbObject != null && !dbObject.isNew());
      pasteAsCopyItem.setEnabled(newButton.isEnabled() && newButton.isVisible());
      clipboardMenu.show(buttonPanel, p.x, p.y);      
    }
  }
  

  private boolean isDragAcceptable(DropTargetDragEvent event) {
    return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0 &&
            event.isDataFlavorSupported(dndFlavor);
  }

  private boolean isDropAcceptable(DropTargetDropEvent event) {
    return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0 &&
            event.isDataFlavorSupported(dndFlavor);
  }


  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    idDialog = new FormDialog(this);
    jLabel1 = new javax.swing.JLabel();
    idNumberField = new org.tentackle.ui.LongFormField();
    idSearchButton = new org.tentackle.ui.FormButton();
    idCancelButton = new org.tentackle.ui.FormButton();
    clipboardMenu = new javax.swing.JPopupMenu();
    copyItem = new javax.swing.JMenuItem();
    pasteItem = new javax.swing.JMenuItem();
    pasteAsCopyItem = new javax.swing.JMenuItem();
    dataPanel = new org.tentackle.ui.FormPanel();
    noAccessPanel = new javax.swing.JPanel();
    noAccessLabel = new javax.swing.JLabel();
    noInfoPanel = new javax.swing.JPanel();
    jLabel3 = new javax.swing.JLabel();
    buttonPanel = new org.tentackle.ui.FormPanel();
    searchButton = new org.tentackle.ui.FormButton();
    newButton = new org.tentackle.ui.FormButton();
    saveButton = new org.tentackle.ui.FormButton();
    deleteButton = new org.tentackle.ui.FormButton();
    printButton = new org.tentackle.ui.FormButton();
    closeButton = new org.tentackle.ui.FormButton();
    idButton = new org.tentackle.ui.FormButton();
    jSeparator1 = new javax.swing.JSeparator();
    securityButton = new org.tentackle.ui.FormButton();
    browserButton = new org.tentackle.ui.FormButton();
    tipAndErrorPanel = new org.tentackle.appworx.TooltipAndErrorPanel();
    previousButton = new org.tentackle.ui.FormButton();
    nextButton = new org.tentackle.ui.FormButton();

    idDialog.setAutoPosition(true);
    idDialog.setTitle(Locales.bundle.getString("Read_object_by_ID")); // NOI18N
    idDialog.setModal(true);
    idDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

    jLabel1.setText(Locales.bundle.getString("ID-No:")); // NOI18N
    idDialog.getContentPane().add(jLabel1, new java.awt.GridBagConstraints());

    idNumberField.setAutoSelect(true);
    idNumberField.setColumns(10);
    idNumberField.setUnsigned(true);
    idNumberField.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        idNumberFieldActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    idDialog.getContentPane().add(idNumberField, gridBagConstraints);

    idSearchButton.setText(Locales.bundle.getString("read")); // NOI18N
    idSearchButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        idSearchButtonActionPerformed(evt);
      }
    });
    idDialog.getContentPane().add(idSearchButton, new java.awt.GridBagConstraints());

    idCancelButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    idCancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        idCancelButtonActionPerformed(evt);
      }
    });
    idDialog.getContentPane().add(idCancelButton, new java.awt.GridBagConstraints());

    copyItem.setText(Locales.bundle.getString("copy")); // NOI18N
    copyItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        copyItemActionPerformed(evt);
      }
    });
    clipboardMenu.add(copyItem);

    pasteItem.setText(Locales.bundle.getString("paste")); // NOI18N
    pasteItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        pasteItemActionPerformed(evt);
      }
    });
    clipboardMenu.add(pasteItem);

    pasteAsCopyItem.setText(Locales.bundle.getString("paste_as_new")); // NOI18N
    pasteAsCopyItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        pasteAsCopyItemActionPerformed(evt);
      }
    });
    clipboardMenu.add(pasteAsCopyItem);

    setAutoPosition(true);

    dataPanel.setLayout(new java.awt.CardLayout());

    noAccessPanel.setLayout(new java.awt.BorderLayout());

    noAccessLabel.setForeground(java.awt.Color.red);
    noAccessLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    noAccessLabel.setText(Locales.bundle.getString("You_don't_have_permission_to_view_this_kind_of_data!")); // NOI18N
    noAccessPanel.add(noAccessLabel, java.awt.BorderLayout.CENTER);

    dataPanel.add(noAccessPanel, "noAccess");

    noInfoPanel.setLayout(new java.awt.BorderLayout());

    jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel3.setText(Locales.bundle.getString("no_further_information")); // NOI18N
    noInfoPanel.add(jLabel3, java.awt.BorderLayout.CENTER);

    dataPanel.add(noInfoPanel, "noInfo");

    getContentPane().add(dataPanel, java.awt.BorderLayout.CENTER);

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    searchButton.setIcon(PlafGlobal.getIcon("search"));
    searchButton.setMnemonic(Locales.bundle.getString("searchMnemonic").charAt(0));
    searchButton.setText(Locales.bundle.getString("search")); // NOI18N
    searchButton.setActionCommand(ACTION_SEARCH);
    searchButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    searchButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        searchButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(searchButton, gridBagConstraints);

    newButton.setIcon(PlafGlobal.getIcon("new"));
    newButton.setMnemonic(Locales.bundle.getString("newMnemonic").charAt(0));
    newButton.setText(Locales.bundle.getString("new")); // NOI18N
    newButton.setActionCommand(ACTION_NEW);
    newButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    newButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        newButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(newButton, gridBagConstraints);

    saveButton.setIcon(PlafGlobal.getIcon("save"));
    saveButton.setMnemonic(Locales.bundle.getString("saveMnemonic").charAt(0));
    saveButton.setText(Locales.bundle.getString("save")); // NOI18N
    saveButton.setActionCommand(ACTION_SAVE);
    saveButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(saveButton, gridBagConstraints);

    deleteButton.setIcon(PlafGlobal.getIcon("delete"));
    deleteButton.setMnemonic(Locales.bundle.getString("deleteMnemonic").charAt(0));
    deleteButton.setText(Locales.bundle.getString("delete")); // NOI18N
    deleteButton.setActionCommand(ACTION_DELETE);
    deleteButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    deleteButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        deleteButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(deleteButton, gridBagConstraints);

    printButton.setIcon(PlafGlobal.getIcon("print"));
    printButton.setMnemonic(Locales.bundle.getString("printMnemonic").charAt(0));
    printButton.setText(Locales.bundle.getString("print")); // NOI18N
    printButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    printButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        printButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 10;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(printButton, gridBagConstraints);

    closeButton.setIcon(PlafGlobal.getIcon("close"));
    closeButton.setMnemonic(Locales.bundle.getString("cancelMnemonic").charAt(0));
    closeButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    closeButton.setActionCommand(ACTION_CANCEL);
    closeButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    closeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        closeButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 11;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
    buttonPanel.add(closeButton, gridBagConstraints);

    idButton.setText("0");
    idButton.setFont(new java.awt.Font("DialogInput", 1, 12));
    idButton.setMargin(new java.awt.Insets(1, 0, 1, 0));
    idButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        idButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 0);
    buttonPanel.add(idButton, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 50);
    buttonPanel.add(jSeparator1, gridBagConstraints);

    securityButton.setIcon(PlafGlobal.getIcon("security"));
    securityButton.setMargin(new java.awt.Insets(1, 0, 1, 0));
    securityButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        securityButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(securityButton, gridBagConstraints);

    browserButton.setIcon(PlafGlobal.getIcon("browser"));
    browserButton.setMnemonic(Locales.bundle.getString("treeMnemonic").charAt(0));
    browserButton.setText(Locales.bundle.getString("tree")); // NOI18N
    browserButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    browserButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        browserButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(browserButton, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 2);
    buttonPanel.add(tipAndErrorPanel, gridBagConstraints);

    previousButton.setIcon(PlafGlobal.getIcon("up"));
    previousButton.setActionCommand(ACTION_PREVIOUS);
    previousButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    previousButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        previousButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(previousButton, gridBagConstraints);

    nextButton.setIcon(PlafGlobal.getIcon("down"));
    nextButton.setActionCommand(ACTION_NEXT);
    nextButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    nextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        nextButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    buttonPanel.add(nextButton, gridBagConstraints);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
  }// </editor-fold>//GEN-END:initComponents

  private void pasteAsCopyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteAsCopyItemActionPerformed
    try {
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable trans = clip.getContents(this);
      Object transferData = trans.getTransferData(dndFlavor);
      if (transferData instanceof AppDbObjectTransferData) {
        AppDbObject droppedObject = ((AppDbObjectTransferData)transferData).getAppDbObject(db.getDb());
        if (droppedObject != null)  {
          // even if same context, need a full copy with propably more complicated procedure
          setObject(droppedObject.createCopyInContextDb(dbObject.getContextDb()));
        }
      }
    }
    catch (Exception e) {
      FormError.printException (Locales.bundle.getString("couldn't_insert_new_copy"), e);
    }
  }//GEN-LAST:event_pasteAsCopyItemActionPerformed

  private void copyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyItemActionPerformed
    if (dbObject != null) {
      try {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        AppDbObjectTransferable trans = new AppDbObjectTransferable(dbObject);
        clip.setContents(trans, trans);
      }
      catch (Exception e) {
        AppworxGlobal.logger.warning(e.getMessage());
        FormInfo.print(Locales.bundle.getString("couldn't_copy"));
      }    
    }
  }//GEN-LAST:event_copyItemActionPerformed

  private void pasteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteItemActionPerformed
    try {
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable trans = clip.getContents(this);
      Object transferData = trans.getTransferData(dndFlavor);
      if (transferData instanceof AppDbObjectTransferData) {
        AppDbObject droppedObject = ((AppDbObjectTransferData)transferData).getAppDbObject(db.getDb());
        if (droppedObject != null)  {
          setObject(droppedObject);
        }
      }
    }
    catch (Exception e) {
      AppworxGlobal.logger.warning(e.getMessage());
      FormInfo.print(Locales.bundle.getString("couldn't_insert"));
    }
  }//GEN-LAST:event_pasteItemActionPerformed

  private void browserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browserButtonActionPerformed
    AppDbObjectNaviDialog d = new AppDbObjectNaviDialog(this, dbObject, null);
    d.setTitle(MessageFormat.format(Locales.bundle.getString("Browser_for_'{0}'_in_{1}"), dbObject, dbObject.getContextDb()));
    d.getNaviPanel().getNaviTree().expandTree(2);   // max. 2 Levels
    d.pack();
    d.setVisible(true);
  }//GEN-LAST:event_browserButtonActionPerformed

  private void securityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_securityButtonActionPerformed
    try {
      manager.newSecurityDialogInstance(baseContext, dbObject.permissionType(), dbObject.getClass(), dbObject.getId()).showDialog();
    } 
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Couldn't_launch_Security_Manager"), ex); 
    }
  }//GEN-LAST:event_securityButtonActionPerformed

  private void idCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idCancelButtonActionPerformed
    idDialog.dispose();
  }//GEN-LAST:event_idCancelButtonActionPerformed

  private void idSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idSearchButtonActionPerformed
    long newId = idNumberField.getLongValue();
    if (newId > 0)  {
      AppDbObject newAppDbObject = dbObject.newObject().select(newId);
      if (newAppDbObject != null)  {
        newAppDbObject.setContextDb(newAppDbObject.makeValidContext());
        setObject(newAppDbObject);
      }
      else  {
        FormError.print(Locales.bundle.getString("no_object_with_such_an_ID"));
      }
    }
    idDialog.dispose();
  }//GEN-LAST:event_idSearchButtonActionPerformed

  private void idNumberFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idNumberFieldActionPerformed
    idSearchButton.doClick();
  }//GEN-LAST:event_idNumberFieldActionPerformed

  private void idButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idButtonActionPerformed
    // Suchen nach ID-Nummern
    if (dbObject != null && !isModal()) {
      idDialog.pack();
      idNumberField.setLongValue(dbObject.getId());
      idNumberField.requestFocusLater();
      idDialog.setVisible(true);    // modal!
    }
  }//GEN-LAST:event_idButtonActionPerformed

  private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    doClose(evt);
  }//GEN-LAST:event_closeButtonActionPerformed

  private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
    PrinterJob printerJob = PrinterJob.getPrinterJob();
    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
    boolean isAppDbObjectPanel = false;
    
    Pageable pageable = dbObjectPanel.getPageable(printerJob);
    if (pageable != null) {
      printerJob.setPageable(pageable);
      isAppDbObjectPanel = pageable instanceof AppDbObjectPanel;
    }
    else {
      Printable printable = dbObjectPanel.getPrintable(printerJob);
      if (printable != null)  {
        printerJob.setPrintable(printable);
        isAppDbObjectPanel = printable instanceof AppDbObjectPanel;
      }
      else  {
        printButton.setEnabled(false);
        return;     // no printing function available
      }
    }
    
    PageFormat format = PrintHelper.print(printerJob, getTitle(), aset);
    if (format != null) {
      if (isAppDbObjectPanel) {
        // screen hardcopy
        try {
          RepaintManager currentManager = RepaintManager.currentManager(dbObjectPanel);
          currentManager.setDoubleBufferingEnabled(false);
          printerJob.print(aset);   // this does the trick: pass aset!
          currentManager.setDoubleBufferingEnabled(true);
        } 
        catch(PrinterException pe) {
          FormError.printException(Locales.bundle.getString("Printer_error"), pe);
        }
      }
      else  {
        // real printout
        PrintHelper.printJob(printerJob, aset);
        dbObjectPanel.markPrinted();
      }
    }
  }//GEN-LAST:event_printButtonActionPerformed

  private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
    if (FormQuestion.yesNo(Locales.bundle.getString("are_you_sure_to_delete_this_object?"))) {
      if (delete()) {
        fireActionPerformed (evt);
        if (disposeOnDeleteOrSave)  {
          dispose();
        }
        else  {
          clearObject();
        }
      }
      else  {
        FormError.print(Locales.bundle.getString("deleting_object_failed!"));
      }
    }
  }//GEN-LAST:event_deleteButtonActionPerformed

  private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
    doSave(evt);
  }//GEN-LAST:event_saveButtonActionPerformed

  private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
    int answer = discardOk();
    if (answer == CancelSaveDiscardDialog.SAVE) {
      saveButton.doClick();
    }
    else if (answer == CancelSaveDiscardDialog.DISCARD && dbObjectPanel.prepareNew())  {
      clearEditedBy();
      clearObject();
      fireActionPerformed (evt);
    }
  }//GEN-LAST:event_newButtonActionPerformed

  private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
    /**
     * search.
     * Notice: we don't check for discardOk because data will always be modified when
     * user enters search criteria *before* pressing the search button (see preset... in AppDbObject).
     * If you don't like that, override prepareSearch()!)
     */
    if (dbObjectPanel.prepareSearch() == true)  {
      try {
        AppDbObjectSearchDialog sd = new AppDbObjectSearchDialog (this, db, dbObjectClass,
                                          new Class[] { dbObjectClass }, false, true);
        sd.setMultiSelection(false);
        AppDbObject oldObject = dbObject;
        if (dbObject != null) {
          if (dbObject.isNew() == false)  {
            // mask with new Object keeping old settings
            setKeepChangedValues(true);
            setObject(dbObject.newObject(), false);
            setKeepChangedValues(false);
            getFormValues();    // get kept values back to new object
          }
          // preset the searchdialog with parameters entered so far
          dbObject.presetQbfParameter(sd.getQbfParameter());
        }

        // open the searchdialog
        AppDbObject newAppDbObject = sd.showDialog();

        if (newAppDbObject != null)  {
          // show new Object, discarding old one
          if (setObject(newAppDbObject) == false)  {
            clearObject();    // object refused
          }
          fireActionPerformed (evt);
        }
        else  {
          // no selection, keep object
          setObject(oldObject);
        }
      }
      catch (Exception ex)  {
        FormError.printException(Locales.bundle.getString("could_not_launch_search_dialog"), ex); 
      }
    }
  }//GEN-LAST:event_searchButtonActionPerformed


  private void prevNextHandler(java.awt.event.ActionEvent evt, int offset) {
    int answer = discardOk();
    if (answer == CancelSaveDiscardDialog.SAVE) {
      saveButton.doClick();
    }
    else if (answer == CancelSaveDiscardDialog.DISCARD && dbObjectPanel.prepareCancel())  {
      int oldIndex = objectListIndex;
      clearEditedBy();
      clearObject();
      if (objectList != null) {
        objectListIndex = oldIndex;
        for (int ndx = objectListIndex + offset; ndx >= 0 && ndx < objectList.size(); ndx += offset) {
          Object obj = objectList.get(ndx);
          if (obj instanceof AppDbObject && setObject((AppDbObject)obj)) {
            break;  // accepted
          }
        }
      }
      fireActionPerformed (evt);
    }
  }


  private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
    prevNextHandler(evt, 1);
}//GEN-LAST:event_nextButtonActionPerformed

  private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousButtonActionPerformed
    prevNextHandler(evt, -1);
}//GEN-LAST:event_previousButtonActionPerformed

  

  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormButton browserButton;
  private org.tentackle.ui.FormPanel buttonPanel;
  private javax.swing.JPopupMenu clipboardMenu;
  private org.tentackle.ui.FormButton closeButton;
  private javax.swing.JMenuItem copyItem;
  private org.tentackle.ui.FormPanel dataPanel;
  private org.tentackle.ui.FormButton deleteButton;
  private org.tentackle.ui.FormButton idButton;
  private org.tentackle.ui.FormButton idCancelButton;
  private org.tentackle.ui.FormDialog idDialog;
  private org.tentackle.ui.LongFormField idNumberField;
  private org.tentackle.ui.FormButton idSearchButton;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JSeparator jSeparator1;
  private org.tentackle.ui.FormButton newButton;
  private org.tentackle.ui.FormButton nextButton;
  private javax.swing.JLabel noAccessLabel;
  private javax.swing.JPanel noAccessPanel;
  private javax.swing.JPanel noInfoPanel;
  private javax.swing.JMenuItem pasteAsCopyItem;
  private javax.swing.JMenuItem pasteItem;
  private org.tentackle.ui.FormButton previousButton;
  private org.tentackle.ui.FormButton printButton;
  private org.tentackle.ui.FormButton saveButton;
  private org.tentackle.ui.FormButton searchButton;
  private org.tentackle.ui.FormButton securityButton;
  private org.tentackle.appworx.TooltipAndErrorPanel tipAndErrorPanel;
  // End of variables declaration//GEN-END:variables
  
}
