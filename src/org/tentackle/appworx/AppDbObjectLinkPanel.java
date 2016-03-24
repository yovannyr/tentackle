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

// $Id: AppDbObjectLinkPanel.java 336 2008-05-09 14:40:20Z harald $
// Created on September 1, 2002, 4:28 PM

package org.tentackle.appworx;


import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormFieldComponentPanel;
import org.tentackle.ui.FormQuestion;
import org.tentackle.ui.StringFormField;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import org.tentackle.util.StringHelper;



/**
 * A panel containing a non-editable FormField representing showing the short text (unique key)
 * of the data object, an optional info field (long text) and buttons for editing, search (link)
 * and clear (unlink).
 * 
 * @see AppDbObjectFieldPanel
 */
public class AppDbObjectLinkPanel extends FormFieldComponentPanel implements DropTargetListener {
  
  private QbfPlugin   plugin;                 // search Qbf-Plugin
  private long        linkedId;               // the original Id of the object
  private AppDbObject linkedObject;           // the linked Object, null = none
  private DataFlavor  dndFlavor;              // DnD Flavor
  private DropTarget  dropTarget;             // droptarget
  private boolean     changeable;             // true if field is changeable
  

  /**
   * Creates an application database object link panel.
   */
  public AppDbObjectLinkPanel() {
    initComponents();
    // objectField is derived from getFormField(), see below in blue section
    // make objectField a drop-target
    dropTarget = new DropTarget (objectField, this);
    dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);
    changeable = true;
    // load the object
    loadObject();
  }
  

  
  
  /**
   * Sets the link.
   *
   * @param plugin  the QbfPlugin to be used
   * @param linkedId  the original, i.e. current ID of the linked object
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
      } catch (Exception ex) {} // treated as "object not found"
      if (linkedObject == null) {
        this.linkedId = 0;
        fireValueEntered();     // cut link!
      }
    }
    loadObject();
  }
  
  
  /**
   * Sets the link object (if plugin matches).
   * 
   * @param object the database object
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
   * Set the link with default plugin.
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
   * Set the link with default plugin.
   *
   * @param clazz the class of the linked object, e.g. Konto.class
   * @param db is the db-connection with context
   * @param linkedId  the original, i.e. current Id of the linked object
   */
  public void setLink(Class<? extends AppDbObject> clazz, ContextDb db, long linkedId)  {
    setLink(clazz, db, linkedId, false);
  }
  
  
  /**
   * Gets the object ID of the link.
   * 
   * @return the object ID, 0 if none
   */
  public long getLinkId() {
    return linkedId;
  }
  
  
  /**
   * Gets the linked object.
   * 
   * @return the object, null if none
   */
  public AppDbObject getLink() {
    return linkedObject;
  }
  
  
  /**
   * Gets the QBF parameter.
   * <br>
   * Handy to modify the Qbf-parameter-set
   * @return the qbf parameter
   */
  public QbfParameter getQbfParameter() {
    return plugin.getParameter();
  }
  

  
  
  
  
  @Override
  public void setChangeable(boolean flag) {
    this.changeable = flag;
    loadObject();   // load again
  }  
  
  @Override
  public boolean isChangeable() {
    return changeable;
  }  
  
  
  @Override
  public boolean requestFocusInWindow() {
    if (linkedObject == null) {
      return linkButton.requestFocusInWindow();
    }
    else  {
      return editButton.requestFocusInWindow();
    }
  }
  
  
  
  
  // -------------------- implements DropTargetListener ------------------------

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
                plugin.getParameter().contextDb, 
                plugin.getParameter().clazz).selectCached(((AppDbObjectTransferData)transferData).getId());
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

  
  
  /**
   * loads the object
   */
  private void loadObject() {
    linkButton.setEnabled(changeable);
    if (linkedObject == null) {
      objectField.setText("?");
      linkedId = 0;
      linkButton.setIcon(PlafGlobal.getIcon("link"));
      linkButton.setToolTipText(Locales.bundle.getString("link"));
      editButton.setEnabled(false);
    }
    else  {
      linkedId = linkedObject.getId();
      objectField.setText(linkedObject.toString());
      linkButton.setIcon(PlafGlobal.getIcon("unlink"));
      linkButton.setToolTipText(Locales.bundle.getString("unlink"));
      editButton.setEnabled(changeable);
    }
   
    if (isChangeable() && linkedId == 0 && plugin != null && plugin.getParameter().clazz != null) {
      // create accepted data flavour
      dndFlavor = new DataFlavor(plugin.getParameter().clazz, StringHelper.getClassBaseName(plugin.getParameter().clazz));
      dropTarget.setActive(true);     // allow drop here
      objectField.setBackground(PlafGlobal.dropFieldActiveColor); 
    }
    else  {
      dropTarget.setActive(false);    // no plugin or object already set: no drop-target
      objectField.setBackground(PlafGlobal.dropFieldInactiveColor);
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

    linkButton = new org.tentackle.ui.FormButton();
    editButton = new org.tentackle.ui.FormButton();
    objectField = (StringFormField)getFormFieldComponent();

    setLayout(new java.awt.GridBagLayout());

    setToolTipText("");
    linkButton.setFormTraversable(true);
    linkButton.setIcon(PlafGlobal.getIcon("link"));
    linkButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    linkButton.setToolTipText(Locales.bundle.getString("link")); // NOI18N
    linkButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        linkButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    add(linkButton, gridBagConstraints);

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

    objectField.setEditable(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 1);
    add(objectField, gridBagConstraints);

  }// </editor-fold>//GEN-END:initComponents

  private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
    // modal dialog
    if (linkedObject != null)  {
      if (Hook.hook().editModal(linkedObject) != null) {
        // object was updated, display new text
        loadObject();
        fireValueEntered();   // could be changed somehow
      }
    }
  }//GEN-LAST:event_editButtonActionPerformed

  private void linkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linkButtonActionPerformed
    if (linkedObject == null) {
      // search and link to new object
      if (plugin != null) {
        try {
          linkedObject = AppDbObjectSearchDialog.createAppDbObjectSearchDialog (
                              this, plugin, new Class[] { plugin.getParameter().clazz }, true, true).showDialog();
        }
        catch (Exception ex) {
          FormError.printException(Locales.bundle.getString("search_failed"), ex);
        }
        loadObject();
        fireValueEntered();
      }
    }
    else  {
      // unlink object
      if (FormQuestion.yesNo(MessageFormat.format(Locales.bundle.getString("remove_link_to_{0}_{1}_?"), 
                linkedObject.getSingleName(), linkedObject))) {
        linkedObject = null;
        loadObject();
        fireValueEntered();
      }
    }
  }//GEN-LAST:event_linkButtonActionPerformed


  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormButton editButton;
  private org.tentackle.ui.FormButton linkButton;
  private org.tentackle.ui.StringFormField objectField;
  // End of variables declaration//GEN-END:variables
  
}

