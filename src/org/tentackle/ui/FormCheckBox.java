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

// $Id: FormCheckBox.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import javax.swing.Icon;
import javax.swing.JCheckBox;



/**
 * A checkbox aware of forms.

 * @author harald
 */
public class FormCheckBox extends JCheckBox
       implements FormComponent, ActionListener {
  
  // keyboard shortcuts
  private static final String selectChars   = Locales.bundle.getString("1JYjy");
  private static final String unselectChars = Locales.bundle.getString("0Nn");
  
  
  private String helpURL;                   // != null for online help
  private FormWindow formWrapWindow;        // window to receive wrap event
  private boolean fireRunning;              // true if some fire-.. method running
  private boolean savedValue;               // the saved state
  private Color selectedColor;              // color for "checked"
  private Color unselectedColor;            // color for "not checked"
  private KeyEvent lastKeyEvent;            // last pressed key
  private boolean tableCellEditorUsage;     // true if used as a cell editor
  private boolean skipNextFocusLost;        // true if ignore next focus lost
  private Window parentWindow;              // parent window (cached)
  private TooltipDisplay tooltipDisplay;    // tooltip display
  private boolean transferFocusDone;        // transfer focus forward done
  private boolean transferFocusBackwardDone;// transfer focus backward done
  private boolean transferFocusByEnter;     // focus lost due to ENTER
  private boolean focusGainedFromTransfer;  // focus gained by transfer focus forward
  private boolean focusGainedFromTransferBackward; // focus gained by transfer focus backward
  private boolean formTraversable = true;   // true if checkbox gets keyboard focus
  private boolean honourChangeable = true;  // honour the changeable-request
  

  /**
   * Creates a check box with text and icon,
   * and specifies whether or not it is initially selected.
   *
   * @param text the text of the check box.
   * @param icon  the Icon image to display
   * @param selected a boolean value indicating the initial selection
   *        state. If <code>true</code> the check box is selected
   */
  public FormCheckBox(String text, Icon icon, boolean selected) {
    super (text, icon, selected);
    // listen to action events
    addActionListener(this);
    // setup some handy key-bindings
    FormHelper.setupDefaultBindings(this);
  }
  
  /**
   * Creates a check box with text
   * and specifies whether or not it is initially selected.
   *
   * @param text the text of the check box.
   * @param selected a boolean value indicating the initial selection
   *        state. If <code>true</code> the check box is selected
   */
  public FormCheckBox (String text, boolean selected)  {
    this (text, null, selected);
  }

  /**
   * Creates a check box with text, unselected.
   *
   * @param text the text of the check box.
   */
  public FormCheckBox (String text) {
    this (text, false);
  }

  /**
   * Creates a check box without text, unselected.
   */
  public FormCheckBox () {
    this (null);
  }
  
  
  
  
  /**
   * Gets the color for "checked".
   * @return the selected color
   */
  public Color getSelectedColor() {
    return selectedColor;
  }  
  
  /**
   * Sets the color when item is selected.<br>
   * If the selected color is set, the deselected color should
   * be set too. If the tentackle-plafs are used,
   * these colors will be used regardless whether the component
   * is enabled or not. This is a nice feature to circumvent
   * the half-intensity display of disabled components.
   * 
   * @param selectedColor the selected color, null if default
   */
  public void setSelectedColor(Color selectedColor) {
    this.selectedColor   = selectedColor;
  }  
  
  
  /**
   * Gets the color for "unchecked".
   * @return the unselected color
   */
  public Color getUnselectedColor() {
    return unselectedColor;
  }  

  /**
   * Sets the color when item is unselected.<br>
   * If the unselected color is set, the selected color should
   * be set too. If the tentackle-plafs are used,
   * these colors will be used regardless whether the component
   * is enabled or not. This is a nice feature to circumvent
   * the half-intensity display of disabled components.
   * 
   * @param unselectedColor the unselected color, null if default
   */
  public void setUnselectedColor(Color unselectedColor) {
    this.unselectedColor = unselectedColor;
  }  
  


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to show tooltip in tooltipdisplay _or_ via mouse hover
   * but _not_ both.
   */
  @Override
  public String getToolTipText() {
    return getTooltipDisplay() == null ? super.getToolTipText() : null;
  }
  
  
  @Override
  public void transferFocus() {
    transferFocusDone = true;
    super.transferFocus();
  }

  @Override
  public void transferFocusBackward() {
    transferFocusBackwardDone = true;
    super.transferFocusBackward();
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to implement keyboard shortcuts.
   */
  @Override
  protected void processKeyEvent(KeyEvent e) {
    
    if (e.getID() == KeyEvent.KEY_PRESSED)  {
    
      lastKeyEvent = e;
      
      if (e.getModifiers() == 0) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
            e.consume();
            transferFocusBackward();
            return;
          case KeyEvent.VK_DOWN:
            e.consume();
            transferFocus();
            return;
        }
      }
      
      char key = e.getKeyChar();

      if (key != KeyEvent.CHAR_UNDEFINED) {
        if (unselectChars.indexOf(key) >= 0) {
          e.consume();
          setSelected(true);
          doClick();
          return;
        }           
        else if (selectChars.indexOf(key) >= 0) {
          e.consume();
          setSelected(false);
          doClick();
          return;
        }        
      }

    }
    
    super.processKeyEvent(e);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden for advanced focus handling
   */
  @Override
  protected void processFocusEvent(FocusEvent e) {
    super.processFocusEvent(e);
    if (e.isTemporary() == false) {
      if (e.getID() == FocusEvent.FOCUS_GAINED) {
        performFocusGained(e.getOppositeComponent());
      }
      else if (e.getID() == FocusEvent.FOCUS_LOST)  {
        if (skipNextFocusLost)  {
          skipNextFocusLost = false;
          performWrapEvent();
        }
        else  {
          performFocusLost();
        }
      }
    }
  }
  
  
  
  
  private void performFocusGained (Component opposite)  {
    if (opposite instanceof FormComponent)  {
      focusGainedFromTransfer         = ((FormComponent)opposite).wasTransferFocus();
      focusGainedFromTransferBackward = ((FormComponent)opposite).wasTransferFocusBackward();
    }
    transferFocusDone = false;
    transferFocusBackwardDone = false;
    formWrapWindow = null;
    showTooltip(super.getToolTipText());
  }

  private void performFocusLost ()  {
    transferFocusByEnter = lastKeyEvent != null && 
                           lastKeyEvent.getKeyCode() == KeyEvent.VK_ENTER && lastKeyEvent.getModifiers() == 0;
    performWrapEvent();
    showTooltip(null);
  }
  
  private void performWrapEvent() {
    if (formWrapWindow != null)  {
      formWrapWindow.fireFormWrappedFocus(new FormWrapEvent(this));
      formWrapWindow = null;
    }    
  }

  

  
  private void showTooltip(String text)  {
    try {
      getTooltipDisplay().setTooltip(text);
    }
    catch (NullPointerException e)  {  
      // ok if not such display 
    }
  }
  
  
  private TooltipDisplay getTooltipDisplay() {
    if (tooltipDisplay == null) {
      try {
        // getParentWindow is fast, because its cached!
        tooltipDisplay = ((FormWindow)getParentWindow()).getTooltipDisplay();
      }
      catch (Exception e)  {
        // if no parentwindow or not a FormWindow.
      }
    }
    return tooltipDisplay;
  }
  
  
  
  // --------------------------- implements FormComponent ---------------------
  
  public void requestFocusLater() {
    FormHelper.requestFocusLater(this);
  }
  
  
  public void actionPerformed (ActionEvent evt) {
    fireValueEntered ();
  }


  public synchronized void addValueListener (ValueListener l) {
     listenerList.add (ValueListener.class, l);
  }

  public synchronized void removeValueListener (ValueListener l) {
     listenerList.remove (ValueListener.class, l);
  }

  public void fireValueChanged () {
    FormHelper.doFireValueChanged (this, listenerList.getListenerList());
  }

  public void fireValueEntered () {
    FormHelper.doFireValueEntered (this, listenerList.getListenerList());
  }


  /**
   * @param selected a Boolean object, else the checkbox is unselected
   */
  public void setFormValue (Object selected)  {
    setSelected (selected instanceof Boolean && ((Boolean)selected).booleanValue());
  }

  /**
   * @return either {@link Boolean#TRUE} or {@link Boolean#FALSE}
   */
  public Boolean getFormValue ()  {
    return isSelected() ? Boolean.TRUE : Boolean.FALSE;
  }

  
  public void saveValue() {
    if (honourChangeable) {
      savedValue = super.isSelected();
    }
  }
  
  public boolean isValueChanged() {
    if (!honourChangeable) {
      return false;
    }
    boolean value = super.isSelected();
    return value != savedValue;
  }
  
  public void triggerValueChanged()   {
    FormHelper.triggerValueChanged(this);
  }
  
  
  public void setChangeable (boolean flag)  {
    if (honourChangeable) {
      setEnabled(flag);
    }
  }

  public boolean isChangeable() {
    return isEnabled();
  }
  
  
  public void setHonourChangeable(boolean flag) {
    this.honourChangeable = flag;
  }
  
  public boolean isHonourChangeable() {
    return this.honourChangeable;
  }
  
  
  public void setFireRunning(boolean running) {
    this.fireRunning = running;
  }

  public boolean isFireRunning()  {
    return fireRunning;
  }
  
  
  public void prepareFocusLost()  {
    if (tableCellEditorUsage == false) {
      performFocusLost();
      skipNextFocusLost = true;
    }
    // else: triggered by FOCUS_LOST
  }
  
  public void setFormWrapWindow(FormWindow parent)  {
    formWrapWindow = parent;
  }
  
  
  public String getHelpURL() {
    return helpURL;
  }  
  
  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }
  
  public void showHelp()  {
    FormHelper.openHelpURL(this);
  }

  
  public Window getParentWindow() {
    if (parentWindow == null) {
      parentWindow = FormHelper.getParentWindow(this);
    }
    return parentWindow;
  }
  
  public void invalidateParentInfo()  {
    parentWindow = null;
    tooltipDisplay = null;
  }
  

  public boolean wasTransferFocus() {
    return transferFocusDone;
  }
  
  public boolean wasTransferFocusBackward() {
    return transferFocusBackwardDone;
  }
  
  
  public boolean wasFocusGainedFromTransfer()  {
    return focusGainedFromTransfer;
  }
  
  public boolean wasFocusGainedFromTransferBackward()  {
    return focusGainedFromTransferBackward;
  }
  
  public boolean wasTransferFocusByEnter() {
    return transferFocusByEnter;
  }
  
  
  public void setCellEditorUsage(boolean flag) {
    tableCellEditorUsage = flag;
  }
  
  public boolean isCellEditorUsage() {
    return tableCellEditorUsage;
  }
  
  
  public void setFormTraversable(boolean formTraversable) {
    this.formTraversable = formTraversable;
  }
  
  public boolean isFormTraversable() {
    return formTraversable;
  }

}


