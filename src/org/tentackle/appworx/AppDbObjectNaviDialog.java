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

// $Id: AppDbObjectNaviDialog.java 336 2008-05-09 14:40:20Z harald $
// Created on August 6, 2002, 3:19 PM

package org.tentackle.appworx;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.List;


/**
 * Navigation dialog for a list of {@link AppDbObject}s.
 * <p>
 * This is just a dialog around a {@link AppDbObjectNaviPanel}.
 */
public class AppDbObjectNaviDialog extends org.tentackle.ui.FormDialog {
  
  private AppDbObjectNaviPanel naviPanel;     // the navigation panel
  private AppDbObject selectedObject;         // the selected object
  
  

  /**
   * Creates a navigation dialog for a list of objects.
   * 
   * @param owner the owner window of this dialog, null if none
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of {@code AppDbObjectNaviPanel.SHOW_...}
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviDialog(Window owner, List<? extends AppDbObject> list, Class[] selectClasses, int buttonMode, boolean showTable)  {
    super(owner);
    naviPanel = new AppDbObjectNaviPanel(list, selectClasses, buttonMode, showTable);
    setup();
  }
  
  /**
   * Creates a navigation dialog for a list of objects.
   * 
   * @param owner the owner window of this dialog, null if none
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviDialog(Window owner, List<? extends AppDbObject> list, Class[] selectClasses)  {
    this(owner, list, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, false);
  }
  
  /**
   * Creates a navigation dialog for a list of objects.
   * 
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviDialog(List<? extends AppDbObject> list, Class[] selectClasses, boolean showTable)  {
    this(null, list, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, showTable);
  }
  
  /**
   * Creates a navigation dialog for a list of objects.
   * 
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviDialog(List<? extends AppDbObject> list, Class[] selectClasses)  {
    this(null, list, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, false);
  }  

  
  /**
   * Creates a navigation dialog for a single object.
   * 
   * @param owner the owner window of this dialog, null if none
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of {@code AppDbObjectNaviPanel.SHOW_...}
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviDialog(Window owner, AppDbObject obj, Class[] selectClasses, int buttonMode, boolean showTable)  {
    super(owner);
    naviPanel = new AppDbObjectNaviPanel(obj, selectClasses, buttonMode, showTable);
    setup();
  }
  
  /**
   * Creates a navigation dialog for a single object.
   * 
   * @param owner the owner window of this dialog, null if none
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviDialog(Window owner, AppDbObject obj, Class[] selectClasses)  {
    this(owner, obj, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, false);
  }
  
  /**
   * Creates a navigation dialog for a single object.
   * 
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviDialog(AppDbObject obj, Class[] selectClasses, boolean showTable)  {
    this(null, obj, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, showTable);
  }
  
  /**
   * Creates a navigation dialog for a single object.
   * 
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviDialog(AppDbObject obj, Class[] selectClasses)  {
    this(null, obj, selectClasses, AppDbObjectNaviPanel.SHOW_BUTTONS, false);
  }  



  
  /**
   * Gives access to the navigation panel.
   * 
   * @return the navigation panel
   */
  public AppDbObjectNaviPanel getNaviPanel() {
    return naviPanel;
  }


  
  /**
   * Displays the dialog.<br>
   * Waits for selection if the dialog is modal. (default)
   *
   * @return the selected AppDbObject, null if no selection or non-modal
   */
  public AppDbObject showDialog() {
    setVisible(true);
    return selectedObject;
  }
  
  
  

  private void setup()  {
    initComponents();
    naviPanel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        naviPanel_actionPerformed(e);
      }
    });
    this.getContentPane().add(naviPanel, BorderLayout.CENTER);
    
    Class<? extends AppDbObject> clazz = naviPanel.getTableClass();
    if (clazz != null)  {
      // all objects of the same class -> table view allowed
      try {
        setTitle(MessageFormat.format(Locales.bundle.getString("{0}-Browser"), AppDbObject.getSingleName(clazz)));
      }
      catch (Exception ex)  {
        setTitle(Locales.bundle.getString("Browser")); 
      }
    }
    pack();
  }


  private void naviPanel_actionPerformed(ActionEvent e) {
    selectedObject = naviPanel.getSelectedObject();
    dispose();
  }

  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setAutoPosition(true);
    setTitle(Locales.bundle.getString("Browser")); // NOI18N
    setModal(true);

    pack();
  }// </editor-fold>//GEN-END:initComponents
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
  
}
