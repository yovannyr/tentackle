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

// $Id: AppDbObjectFieldPanel.java 472 2009-08-07 08:30:36Z svn $

package org.tentackle.appworx;


import java.awt.event.KeyEvent;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormFieldComponentPanel;
import org.tentackle.ui.StringFormField;
import org.tentackle.util.ShortLongText;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.lang.reflect.Modifier;
import org.tentackle.util.StringHelper;



/**
 * A panel containing a FormField representing the key to select the data object
 * and buttons for editing/viewing/searching.
 * 
 * @see AppDbObjectLinkPanel
 */
public class AppDbObjectFieldPanel extends FormFieldComponentPanel implements DropTargetListener {
  
  private QbfPlugin     plugin;         // search Qbf-Plugin
  private long          linkedId;       // the original Id of the object
  private AppDbObject   linkedObject;   // the linked Object, null = none
  private DataFlavor    dndFlavor;      // DnD Flavor
  private DropTarget    dropTarget;     // droptarget
  
  
  /**
   * Creates an application database object field panel.
   * <p>
   * Pressing {@code F2} in the key field will open a search dialog. 
   * {@code F3} will edit the object.
   * Drag and drop is supported as well.<br>
   * By default, the editing component is a {@link StringFormField}.
   */
  public AppDbObjectFieldPanel() {
    setFormFieldComponent(new StringFormField() {
      @Override
      protected void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
          if (e.getKeyCode() == KeyEvent.VK_F2) {
            runSearch();
          }
          else if (e.getKeyCode() == KeyEvent.VK_F3) {
            runEdit();
          }
        }
        super.processKeyEvent(e);
      }
    });
    initComponents();
    // codeField is derived from getFormField(), see below in blue section
    // make infoField a drop-target
    dropTarget = new DropTarget (infoField, this);
    dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);
    // load the object
    loadObject();
  }
  
  
  
  
  /**
   * Sets the link.
   *
   * @param plugin is the QbfPlugin to be used
   * @param linkedId  the original, i.e. current Id of the linked object
   */
  public void setLink(QbfPlugin plugin, long linkedId)  {
    
    this.plugin   = plugin;
    this.linkedId = linkedId;
    
    if (linkedId == 0 || plugin == null)  {
      linkedObject  = null;
      this.linkedId = 0;
    }
    else  {
      try {
        linkedObject = AppDbObject.selectCached(
                plugin.getParameter().contextDb, plugin.getParameter().clazz, linkedId);
      } 
      catch (Exception ex) {} // treated as "object not found"
      
      if (linkedObject == null) {
        this.linkedId = 0;
        fireValueEntered();     // cut link!
      }
    }
    loadObject();
  }
  
  
  /**
   * sets the link object (if plugin matches)
   *
   * @param object the data object
   */
  public void setLink(AppDbObject object)  {
    if (object != null && plugin != null && plugin.getParameter().clazz.equals(object.getClass()))  {
      setLink(plugin, object.getId());
    }
    else  {
      setLink(plugin, 0);
    }
  }
  
  
  /**
   * Sets the link with default plugin.
   *
   * @param clazz the class of the linked object, e.g. Konto.class
   * @param db is the db-connection with context
   * @param linkedId  the original, i.e. current Id of the linked object
   * @param keepPlugin is true if keep plugin if already initialized
   */
  public void setLink(Class<? extends AppDbObject> clazz, ContextDb db, long linkedId, boolean keepPlugin)  {
    try {
      if (db != null && clazz != null)  {
        if (keepPlugin && plugin != null) {
          setLink(plugin, linkedId);
        }
        else  {
          if (Modifier.isAbstract(clazz.getModifiers())) {
            setLink(AppDbObject.makeQbfPlugin(clazz, db), linkedId);
          }
          else  {
            setLink (AppDbObject.newByClass(db, clazz).makeQbfPlugin(), linkedId);
          }
        }
        return;
      }
    } catch (Exception ex) {} // treated as "clear"
    // else clear link
    setLink (null, 0);
  }
  
  
  /**
   * Sets the link with default plugin.
   *
   * @param clazz the class of the linked object, e.g. Konto.class
   * @param db is the db-connection with context
   * @param linkedId  the original, i.e. current Id of the linked object
   */  
  public void setLink(Class<? extends AppDbObject> clazz, ContextDb db, long linkedId)  {
    setLink(clazz, db, linkedId, false);
  }

  
  /**
   * Gets the object Id of the link.
   *
   * @return the object id, 0 = no object linked
   */
  public long getLinkId() {
    return linkedId;
  }
  
  
  /**
   * Gets the object.
   *
   * @return the data object, null = no object linked
   */
  public AppDbObject getLink() {
    return linkedObject;
  }
  
  
  /**
   * Gets the qbf parameter.
   * 
   * @return the qbf parameter
   */
  public QbfParameter getQbfParameter() {
    return plugin.getParameter();
  }
  
  
  /**
   * Gets the plugin.
   *
   * @return the qbf plugin
   */
  public QbfPlugin getQbfPlugin() {
    return plugin;
  }
  
  
  /**
   * Sets columns of the info field.
   *
   * @param col the columns
   */
  public void setInfoColumns(int col) {
    infoField.setColumns(col);
  }
  
  /**
   * Get columns of info field
   *
   * @return the columns
   */
  public int getInfoColumns() {
    return infoField.getColumns();
  }
  
  
  /**
   * Updates both the code- and the info field.
   * Override this if the default does not match your objects behaviour!
   *
   * @param linkedObject the data object
   */
  public void updateCodeAndInfoField(AppDbObject linkedObject) {
    if (linkedObject instanceof ShortLongText) {
      codeField.setText(((ShortLongText)linkedObject).getShortText());
      infoField.setText(((ShortLongText)linkedObject).getLongText());
    }
    else {
      codeField.setText(linkedObject.toString());
      infoField.setText(linkedObject.getTreeText());
    }
  }
  

  /**
   * Sets the infofield's visibility.
   * Sometimes useful if getTreeText() is not appropriate for non-ShortLongText objects.
   *
   * @param visible true if info field is visible (default)
   */
  public void setInfoFieldVisible(boolean visible) {
    infoField.setVisible(visible);
  }
  
  /**
   * Gets the infofield's visibility.
   *
   * @return true if visible
   */
  public boolean isInfoFieldVisible() {
    return infoField.isVisible();
  }
  
  
  

  @Override
  public void setChangeable(boolean changeable) {
    super.setChangeable(changeable);
    loadObject();   // load again
  }  
  
  
  
  /**
   * Loads the object
   */
  private void loadObject() {
    if (linkedObject == null) {
      infoField.setText("?");
      codeField.clearText();
      linkedId = 0;
      editButton.setEnabled(false);
      searchButton.setFormTraversable(true);
    }
    else  {
      linkedId = linkedObject.getId();
      updateCodeAndInfoField(linkedObject);
      editButton.setEnabled(isChangeable());
      searchButton.setFormTraversable(false);
    }
    searchButton.setEnabled(isChangeable());
   
    if (isChangeable() && linkedId == 0 && plugin != null && plugin.getParameter().clazz != null) {
      // create accepted data flavour
      dndFlavor = new DataFlavor(plugin.getParameter().clazz, StringHelper.getClassBaseName(plugin.getParameter().clazz));
      dropTarget.setActive(true);     // allow drop here
      infoField.setBackground(PlafGlobal.dropFieldActiveColor); 
    }
    else  {
      dropTarget.setActive(false);    // no plugin or object already set: no drop-target
      infoField.setBackground(PlafGlobal.dropFieldInactiveColor);
    }
  }
  
  
  /**
   * Runs the search
   */
  public void runSearch() {
    if (plugin != null) {
      try {
        AppDbObject obj = AppDbObjectSearchDialog.createAppDbObjectSearchDialog (
                            this, plugin, new Class[] { plugin.getParameter().clazz }, true, true).showDialog();
        if (obj != null) {
          setLink(obj);
          fireValueEntered();
          searchButton.transferFocus();
        }
      }
      catch (Exception ex) {
        FormError.printException(Locales.bundle.getString("search_failed"), ex);
      }
    }    
  }
  
  
  /**
   * Edits the object
   */
  public void runEdit() {
    // modal dialog
    if (linkedObject != null)  {
      if (Hook.hook().editModal(linkedObject) != null) {
        // object was updated, display new text
        loadObject();
        fireValueEntered();   // could be changed somehow
      }
    }
  }
  
  
  

  

  // --------------- implements DropTargetListener ----------------------------
  
  public void dragEnter (DropTargetDragEvent event)  {
    if (!isDragAcceptable(event)) {
      event.rejectDrag();
    }
    /**
     * we can't do this because of a bug in Win32-JVM.
     * see: http://developer.java.sun.com/developer/bugParade/bugs/4217416.html
    else  {
      event.acceptDrag(DnDConstants.ACTION_COPY);
    }
    */
  }

  public void dragExit (DropTargetEvent event)  {
  }

  public void dragOver (DropTargetDragEvent event)  {
    if (!isDragAcceptable(event)) {
      event.rejectDrag();
    }
    // see comment above!
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
          AppDbObject object = AppDbObject.newByClass(
                plugin.getParameter().contextDb, plugin.getParameter().clazz).selectCached(((AppDbObjectTransferData)transferData).getId());
          setLink(object);
          fireValueEntered();
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


  private boolean isDragAcceptable(DropTargetDragEvent event) {
    return ((event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0 &&
            event.isDataFlavorSupported(dndFlavor));
  }

  private boolean isDropAcceptable(DropTargetDropEvent event) {
    return ((event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0 &&
            event.isDataFlavorSupported(dndFlavor));
  }


  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    codeField = (StringFormField)getFormFieldComponent();
    editButton = new org.tentackle.ui.FormButton();
    infoField = new org.tentackle.ui.StringFormField();
    searchButton = new org.tentackle.ui.FormButton();

    setLayout(new java.awt.GridBagLayout());

    setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 1);
    add(codeField, gridBagConstraints);

    editButton.setIcon(PlafGlobal.getIcon("edit"));
    editButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    editButton.setToolTipText(Locales.bundle.getString("edit")); // NOI18N
    editButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        editButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    add(editButton, gridBagConstraints);

    infoField.setEditable(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    add(infoField, gridBagConstraints);

    searchButton.setIcon(PlafGlobal.getIcon("search"));
    searchButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    searchButton.setToolTipText(Locales.bundle.getString("search")); // NOI18N
    searchButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        searchButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    add(searchButton, gridBagConstraints);

  }// </editor-fold>//GEN-END:initComponents

  private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
    runSearch();
  }//GEN-LAST:event_searchButtonActionPerformed

  private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
    runEdit();
  }//GEN-LAST:event_editButtonActionPerformed

  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.StringFormField codeField;
  private org.tentackle.ui.FormButton editButton;
  private org.tentackle.ui.StringFormField infoField;
  private org.tentackle.ui.FormButton searchButton;
  // End of variables declaration//GEN-END:variables
  
}

