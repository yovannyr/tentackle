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

// $Id: AppDbObjectDialogPool.java 464 2009-07-18 19:02:05Z harald $

package org.tentackle.appworx;


import org.tentackle.ui.FormHelper;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;



/**
 * Pool for {@link AppDbObjectDialog}s.
 * <p>
 * The pool reduces memory consumption and dialog startup time. Furthermore,
 * it makes sure that each object is being edited only once.
 * <p>
 * Notice that the pool is not mt-safe (as it is the case with Swing in general).
 *
 * @author harald
 */
public class AppDbObjectDialogPool {

  private List<AppDbObjectDialog> dialogList; // pool of dialogs
  private boolean singleObjectEdit;           // true if same object can be edited only once at a time (true = default)
  

  /**
   * Creates a dialog pool
   */
  public AppDbObjectDialogPool() {
    dialogList = new ArrayList<AppDbObjectDialog>();
    singleObjectEdit = true;
  }
  
  

  /**
   * Returns whether an object is allowed to be edited only once at a time.
   * 
   * @return true if objects must not be edited more than once (default)
   */
  public boolean isSingleObjectEdit() {
    return singleObjectEdit;
  }
  
  /**
   * Sets whether an object is allowed to be edited only once at a time.
   * 
   * @param singleObjectEdit  true if objects must not be edited more than once (default)
   */
  public void setSingleObjectEdit(boolean singleObjectEdit) {
    this.singleObjectEdit = singleObjectEdit;
  }
  

  /**
   * Determines whether a given object is currently being edited
   * by some other dialog.
   * 
   * @param object the database object
   * @param exceptMe the dialog to exclude from the check, null = none
   * @param comp the optional comparator, null if "equals"
   * @return the dialog that is already editing given object
   */
  public AppDbObjectDialog isObjectBeingEdited(AppDbObject object, AppDbObjectDialog exceptMe, Comparator<? super AppDbObject> comp)  {
    if (object != null && !object.isNew()) {
      for (AppDbObjectDialog d: dialogList) {
        if (exceptMe != d && d.isVisible() && d.isAllChangeable() &&
            d.getDbObjectClass() == object.getClass()) {
          AppDbObject editedObject = d.getObject();
          if (editedObject != null) {
            if (comp != null) {
              if (comp.compare(editedObject, object) == 0) {
                return d;
              }
            }
            else {
              if (editedObject.equals(object)) {
                return d;
              }
            }
          }
        }
      }
    }
    return null;
  }
  
  /**
   * Determines whether a given object is currently being edited
   * by some other dialog.
   * 
   * @param object the database object
   * @param exceptMe the dialog to exclude from the check, null = none
   * @return the dialog that is already editing given object
   */
  public AppDbObjectDialog isObjectBeingEdited(AppDbObject object, AppDbObjectDialog exceptMe)  {
    return isObjectBeingEdited(object, exceptMe, null);
  }
  
  /**
   * Determines whether a given object is currently being edited
   * by some other dialog.
   * 
   * @param object the database object
   * @return the dialog that is already editing given object
   */
  public AppDbObjectDialog isObjectBeingEdited(AppDbObject object) {
    return isObjectBeingEdited(object, null, null);
  }

  
  /**
   * Checks whether an unused dialog can be used from the pool.
   * 
   * @param objectClass the data object class
   * @param modal true if dialog must be modal
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @return the dialog, null if no such dialog in pool
   */
  public AppDbObjectDialog getDialog (Class objectClass, boolean modal, boolean allChangeable)  {
    // check if dialog already created, not visible and of correct modal-type
    for (AppDbObjectDialog d: dialogList) {
      if (d != null && d.isModal() == modal && d.isVisible() == false &&
          d.isAllChangeable() == allChangeable && d.getDbObjectClass().equals(objectClass))  {
        return d;
      }
    }
    // keiner verfügbar
    return null;
  }

  
  /**
   * Creates a new AppDbObjectDialog.<br>
   * 
   * Override this method if your application uses an extended AppDbObjectDialog.
   * This is sufficient since tentackle never invokes new AppDbObjectDialog() directly
   * but goes via the dialog-dialogList always!
   *
   * @param comp the component to determine the window owner, null if none
   * @param object the object template to create a dialog for
   * @param modal true if dialog should be modal
   * @return the created dialog
   */
  public AppDbObjectDialog newAppDbObjectDialog(Component comp, AppDbObject object, boolean modal) {
    return new AppDbObjectDialog(FormHelper.getParentWindow(comp), object, modal);
  }
  
  
  /**
   * Adds a dialog to the pool.
   * 
   * @param d the dialog to add
   */
  public void addDialog (AppDbObjectDialog d) {
    if (d != null && d.getDbObjectClass() != null && d.getObjectPanel() != null)  {
      dialogList.add(d);
    }
  }
  
  
  /**
   * Removes a dialog from the pool.
   * 
   * @param d the dialog to remove
   */
  public void removeDialog (AppDbObjectDialog d)  {
    d.setVisible(false);              // this will remove all listeners too
    d.getContentPane().removeAll();   // alle Components entfernen
    dialogList.remove(d);                   // remove from dialogList
  }

  
  /**
   * Clears the pool.
   */
  public void clear() {
    for (AppDbObjectDialog d: dialogList) {
      removeDialog(d);
    }
    dialogList.clear();
  }
  
  
  /**
   * Disposes all dialogs.
   */
  public void disposeAllDialogs()  {
    for (AppDbObjectDialog d: dialogList) {
      if (d.isVisible())  {
        d.dispose();
      }
    }    
  }
  

  /**
   * Gets a modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param comp the component to determine the owner window for
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @param noPersistance true if NO changes must be made to persistance layer
   * @return the dialog, never null
   */
  public AppDbObject useModalDialog(Component comp, 
                                    AppDbObject object, 
                                    boolean allChangeable,
                                    boolean noPersistance)  {
    
    if (object != null) {
      
      AppDbObjectDialog d = null;
      
      if (singleObjectEdit && allChangeable) {
        // if only one dialog at a time for editing the object
        d = isObjectBeingEdited(object);
        if (d != null)  {
          if (d.isModal() == false) {
            d.dispose();        // close it first.
            d.setModal(true);   // make it modal.
          }
          else  {
            d.toFront();  // bring to front and set object below (could be changed)
          }
        }
      }
      
      if (d == null) {
        // no object-related dialog, try to find some for objects class
        d = getDialog(object.getClass(), true, allChangeable);
      }
      
      if (d == null)  {
        // no such dialog: create new one
        d = newAppDbObjectDialog(comp, object, true);
        if (allChangeable == false) {
          d.setAllChangeable(false);
        }
        addDialog(d);
      }
      else  {
        // dialog exists already
        d.setObject(object);
      }
      
      // show Dialog and wait for dispose
      object = d.showDialog(noPersistance);
    }
    return object;
  }
  
  /**
   * Gets a modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param comp the component to determine the owner window for
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @return the dialog, never null
   */
  public AppDbObject useModalDialog(Component comp, AppDbObject object, boolean allChangeable)  {
    return useModalDialog(comp, object, allChangeable, false);
  }
  
  /**
   * Gets a modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @return the dialog, never null
   */
  public AppDbObject useModalDialog(AppDbObject object, boolean allChangeable)  {
    return useModalDialog(null, object, allChangeable, false);
  }

  
  
  /**
   * Gets a non-modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param comp the component to determine the owner window for
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @param noPersistence true if NO changes must be made to persistance layer
   * @param disposeOnDeleteOrSave true if dispose dialog after delete or save
   * @return the dialog, never null
   */
  public AppDbObjectDialog useNonModalDialog(Component comp, 
                                             AppDbObject object, 
                                             boolean allChangeable, 
                                             boolean noPersistence,
                                             boolean disposeOnDeleteOrSave)  {
    
    if (object != null) {
      
      AppDbObjectDialog d = null;
      
      if (singleObjectEdit && allChangeable) {
        // if only one dialog at a time for editing the object
        d = isObjectBeingEdited(object);
        if (d != null)  {
          if (d.isModal()) {
            // if d.isModal() something is wrong in the app-logic!
            throw new IllegalStateException("dialog is modal?");
          }
          d.toFront();                    // bring to front and set object below (could be changed)
        }
      }
      
      if (d == null) {
        // no object-related dialog, try to find some for objects class
        d = getDialog(object.getClass(), false, allChangeable);
      }
      
      if (d == null)  {
        // no such dialog: create new one
        d = newAppDbObjectDialog(comp, object, false);
        if (allChangeable == false) {
          d.setAllChangeable(false);
        }
        addDialog(d);
      }
      else  {
        // dialog already exists, reuse it
        d.setObject(object);
      }
      
      // show Dialog
      d.setDisposeOnDeleteOrSave(disposeOnDeleteOrSave);
      if (d.isVisible() == false) {
        d.showDialog(noPersistence); // non-modal
      }
      return d;
    }
    return null;
  }
  
  /**
   * Gets a non-modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @param disposeOnDeleteOrSave true if dispose dialog after delete or save
   * @return the dialog, never null
   */
  public AppDbObjectDialog useNonModalDialog(AppDbObject object, boolean allChangeable, boolean disposeOnDeleteOrSave) {
    return useNonModalDialog(null, object, allChangeable, false, disposeOnDeleteOrSave);
  }
  
  /**
   * Gets a non-modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param comp the component to determine the owner window for
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @return the dialog, never null
   */
  public AppDbObjectDialog useNonModalDialog(Component comp, AppDbObject object, boolean allChangeable)  {
    return useNonModalDialog(comp, object, allChangeable, false, false);
  }
  
  /**
   * Gets a non-modal dialog ready for use.
   * Will re-use a pooled dialog if possible, otherwise creates a new one.
   *
   * @param object the database object
   * @param allChangeable true if dialog must be changeable, false if view-only dialog
   * @return the dialog, never null
   */
  public AppDbObjectDialog useNonModalDialog(AppDbObject object, boolean allChangeable)  {
    return useNonModalDialog(null, object, allChangeable, false, false);
  }

}