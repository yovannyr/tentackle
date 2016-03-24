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

// $Id: FormComponentPanel.java 336 2008-05-09 14:40:20Z harald $
// Created on December 21, 2004, 7:36 PM

package org.tentackle.ui;

import java.awt.Window;

/**
 * A FormPanel with an embedded FormComponent.
 * 
 * @author harald
 */
public class FormComponentPanel extends FormPanel implements FormComponent {
  
  private FormComponent comp;       // the component to be embedded by the panel

  
  /**
   * Creates a {@code FormComponentPanel} for a given {@code FormComponent}.
   * 
   * @param comp the component
   */
  public FormComponentPanel(FormComponent comp) {
    setFormComponent(comp);
  }

  /**
   * Creates a {@code FormComponentPanel} for a {@link StringFormField}.
   */
  public FormComponentPanel() {
    this(new StringFormField());
  }


  /**
   * Gets the embedded form component.
   * 
   * @return the component
   */
  public FormComponent getFormComponent() {
    return comp;
  }

  /**
   * Sets the embedded form field component.
   * 
   * @param comp the component.
   */
  public void setFormComponent(FormComponent comp)  {
    this.comp = comp;
  }


  // ---------------------- implements FormComponent --------------------
  

  public void setFireRunning(boolean running) {
    comp.setFireRunning(running);
  }

  public boolean isFireRunning()  {
    return comp.isFireRunning();
  }

  public synchronized void addValueListener (ValueListener l) {
     comp.addValueListener(l);
  }

  public synchronized void removeValueListener (ValueListener l) {
     comp.removeValueListener(l);
  }

  public void fireValueChanged () {
    comp.fireValueChanged();
  }

  public void fireValueEntered () {
    comp.fireValueEntered();
  }

  public void setFormValue (Object obj) {
    comp.setFormValue(obj);
  }

  public Object getFormValue () {
    return comp.getFormValue();
  }

  public void setChangeable (boolean flag)  {
    comp.setChangeable(flag);
  }

  public boolean isChangeable() {
    return comp.isChangeable();
  }
  
  public void setHonourChangeable (boolean flag)  {
    comp.setHonourChangeable(flag);
  }

  public boolean isHonourChangeable() {
    return comp.isHonourChangeable();
  }

  public void requestFocusLater()  {
    comp.requestFocusLater();
  }

  public void setFormWrapWindow(FormWindow parent) {
    comp.setFormWrapWindow(parent);
  }  

  public boolean isValueChanged() {
    return comp.isValueChanged();
  }
  
  public void saveValue() {
    comp.saveValue();
  }
  
  public void triggerValueChanged() {
    comp.triggerValueChanged();
  }
  
  @Override
  public String getHelpURL() {
    return comp.getHelpURL();
  }
  
  @Override
  public void setHelpURL(String helpURL) {
    comp.setHelpURL(helpURL);
  }
  
  @Override
  public void showHelp()  {
    comp.showHelp();
  }

  @Override
  public boolean hasFocus() {
    return comp.hasFocus();
  }

  public boolean wasTransferFocus() {
    return comp.wasTransferFocus();
  }
  
  public boolean wasTransferFocusBackward() {
    return comp.wasTransferFocusBackward();
  }
  
  public boolean wasFocusGainedFromTransfer()  {
    return comp.wasFocusGainedFromTransfer();
  }
  
  public boolean wasFocusGainedFromTransferBackward()  {
    return comp.wasFocusGainedFromTransferBackward();
  }
  
  public boolean wasTransferFocusByEnter() {
    return comp.wasTransferFocusByEnter();
  }
  
  @Override
  public Window getParentWindow() {
    return comp.getParentWindow();
  }
  
  @Override
  public void invalidateParentInfo()  {
    comp.invalidateParentInfo();
  }
  
  public void setCellEditorUsage(boolean flag) {
    comp.setCellEditorUsage(flag);
  }
  
  public boolean isCellEditorUsage() {
    return comp.isCellEditorUsage();
  }
  
  public void prepareFocusLost() {
    comp.prepareFocusLost();
  }

  public void setFormTraversable(boolean formTraversable) {
    comp.setFormTraversable(formTraversable);
  }
  
  public boolean isFormTraversable() {
    return comp.isFormTraversable();
  }
  
}
