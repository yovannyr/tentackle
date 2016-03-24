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

// $Id: FormPanel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import javax.swing.JPanel;




/**
 * Extended {@code JPanel} implementing {@code FormContainer}.
 *
 * @author harald
 */

public class FormPanel extends JPanel implements FormContainer, ContainerListener {

  
  private String helpURL;                       // default help for this container
  private String  title;                        // title is null by default
  private boolean honourAllChangeable = true;   // honour the allChangeable-request
  private boolean allChangeable = true;         // last operation from setAllChangeable()
  private Window parentWindow;                  // the (cached) parent window
  private boolean parentWindowLoaded;           // true if parent window cached
  
  
  /**
   * Creates a new FormPanel with the specified layout manager and buffering
   * strategy.
   *
   * @param layout the LayoutManager to use
   * @param doubleBuffered true for double-buffering, which
   *        uses additional memory space to achieve fast, flicker-free 
   *        updates (default)
   */
  public FormPanel(LayoutManager layout, boolean doubleBuffered) {
    super(layout, doubleBuffered);
    // setup
    FormHelper.setDefaultFocusTraversalPolicy(this);    // set traversal policy
    addContainerListener(this);
  }

  /**
   * Creates a new FormPanel with the default FlowLayout manager
   * and buffering strategy.
   *
   * @param doubleBuffered true for double-buffering, which
   *        uses additional memory space to achieve fast, flicker-free 
   *        updates (default)
   */
  public FormPanel(boolean doubleBuffered) {
    this (new FlowLayout(), doubleBuffered);
  }

  /**
   * Creates a new FormPanel with the specified layout manager and
   * double buffering.
   *
   * @param layout the LayoutManager to use
   */
  public FormPanel(LayoutManager layout) {
    this(layout, true);
  }

  /**
   * Creates a new FormPanel with the default FlowLayout manager and
   * double buffering.
   */
  public FormPanel()  {
    this(true);
  }

  
  
  
  /**
   * Adds an ActionListener.<br>
   * The listener can be invoked by the application on demand.
   * Currently there is no events that will trigger fireActionPerformed()
   * automatically. This depends on the application.
   *
   * @param listener the field listener to add
   */
  public synchronized void addActionListener (ActionListener listener) {
     listenerList.add (ActionListener.class, listener);
  }

  /**
   * Removes an ActionListener
   *
   * @param listener the field listener to remove
   */
  public synchronized void removeActionListener (ActionListener listener) {
     listenerList.remove (ActionListener.class, listener);
  }
  
  
  /**
   * Invokes all actionListeners for this panel.
   * 
   * @param e the action event
   */
  public void fireActionPerformed(ActionEvent e)  {
    if (listenerList != null)  {
      for (ActionListener l: listenerList.getListeners(ActionListener.class)) {
        l.actionPerformed(e);
      }
    }    
  }
  
  

  

  // ----------------------- implements ContainerListener --------------------
  
  
  public void componentAdded(ContainerEvent e)  {
    if (getParent() != null)  {
      // this optimization delays invalidateParentInfo() until the
      // panel is added in a chain that ends at a toplevel-window
      componentRemoved(e);
    }
  }

  public void componentRemoved(ContainerEvent e)  {
    // same for add and remove
    Component comp = e.getChild();
    if (comp instanceof FormComponent)  {
      // tell child that next invocation of getParentWindow() should walk up the tree again
      ((FormComponent)comp).invalidateParentInfo();
    }
    else {
      FormHelper.invalidateParentInfo(comp);
    }
  }


  
  // -------------------- implements FormContainer ---------------------------
  
  
  public Window getParentWindow() {
    if (!parentWindowLoaded) {
      parentWindow = FormHelper.getParentWindow(this);
      parentWindowLoaded = true;
    }
    return parentWindow;
  }
  
  public void invalidateParentInfo()  {
    parentWindowLoaded = false;
    FormHelper.invalidateParentInfo(this);
  }
    
  
  public void setFormValues ()  {
    Window p = getParentWindow();
    boolean keepChanged = false;
    if (p instanceof FormWindow)  {
      keepChanged = ((FormWindow)p).getKeepChangedValues();
    }
    if (keepChanged) {
      FormHelper.setFormValueKeepChanged(this);
    }
    else {
      FormHelper.setFormValue(this);
    }
  }

  
  public void setFormValuesKeepChanged ()  {
    FormHelper.setFormValueKeepChanged (this);
  }
  

  public void getFormValues ()  {
    FormHelper.getFormValue (this);
  }
  

  public void saveValues()  {
    FormHelper.saveValue(this);
  }
  
  public boolean areValuesChanged() {
    return FormHelper.isValueChanged(this); 
  }
  
  
  public void triggerValuesChanged()  {
    // no action
  }


  public void setAllChangeable (boolean flag) {
    if (honourAllChangeable)  {
      FormHelper.setChangeable (this, flag);
      allChangeable = flag;
    }
  }

  public boolean isAllChangeable()  {
    return allChangeable;
  }

  public void setHonourAllChangeable(boolean flag) {
    this.honourAllChangeable = flag;
  }
  
  public boolean isHonourAllChangeable() {
    return this.honourAllChangeable;
  }
  

  public String getTitle() {
    return title;
  }  

  public void setTitle(String title) {
    this.title = title;
  }  
  
  public void setHelpURL(String helpURL)  {
    this.helpURL = helpURL;
  }
  
  public String getHelpURL() {
    return helpURL;
  }
  
  public void showHelp() {
    FormHelper.openHelpURL(this);
  }

}
