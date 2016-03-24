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

// $Id: FormComponent.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Window;

/**
 * Interface all Tentackle form components must implement.
 * <p>
 * Tentackle beans derived from Swing components that the user interacts
 * with in order to edit some data are {@code FormComponent}s.
 * FormComponents are aware of the binding between the data and the GUI
 * and provide a standardized way to update the view and the data, vice versa.
 * This simplifies the development and design of the GUI significantly.
 *
 * @author harald
 */
public interface FormComponent {

  /**
   * Sets a value in this component.<br>
   * The method is used to update the view if the data has changed.
   *
   * @param object is the object to set
   */
  public void setFormValue (Object object);

  /**
   * Retrieves the value from this component.<br>
   * The method is used to update the data if the view has changed because
   * the user edited it.
   *
   * @return  the object from the form
   */
  public Object getFormValue();

  
  /**
   * Sets a savepoint for the data of this component.<br>
   * Allows to decide whether the user has changed the data associated
   * with this component or not.
   * @see #isValueChanged() 
   */
  public void saveValue();
  
  
  /**
   * Checks whether the contents have been changed since the last savepoint.
   * 
   * @return true if changed
   * @see #saveValue()
   */
  public boolean isValueChanged();
  
  
  /**
   * Promotes the fact that value changed to all {@link FormContainer}s this
   * component is a child of.
   */
  public void triggerValueChanged();
  
  
  /**
   * Adds a value listener that implements the binding between this component
   * and the data.
   *
   * @param listener the value listener to add
   */
  public void addValueListener (ValueListener listener);

  /**
   * Remove a value listener.
   *
   * @param listener the value listener to remove
   */
  public void removeValueListener (ValueListener listener);

  
  /**
   * Fires all value listeners registered on this component
   * invoking their {@code valueChanged}-handler.<br>
   * This method is invoked whenever the data has been changed
   * and that change should be reflected in the view.
   */
  public void fireValueChanged();

  
  /**
   * Fires all listeners registered on this component
   * invoking their {@code valueEntered}-handler.<br>
   * This method is invoked whenever the user edited the view
   * and that change should be reflected in the data.
   */
  public void fireValueEntered();

  
  /**
   * Set the component to be changeable.<br>
   * This is a unified abstraction and will be translated to setEditable or
   * setEnabled, whatever is appropriate to this component.
   * 
   * @param changeable true the user can edit the data, false if show only
   */
  public void setChangeable(boolean changeable);

  /**
   * Returns if this component is changeable.
   * 
   * @return true the user can edit the data, false if show only
   */
  public boolean isChangeable();

  
  /**
   * Sets whether this component should honour the changeable-attribute or not.<br>
   * The default is true.
   * Notice: if this component does not honour the changeable flag,
   * isValueChanged() always returns false and it will not perform any saveValue().
   * 
   * @param honourChangeable true if honour the changeable attribute, false if not
   */
  public void setHonourChangeable(boolean honourChangeable);
  
  /**
   * Returns whether this component should honour the changeable-attribute or not.
   * @return true if honour the changeable attribute, false if not
   */
  public boolean isHonourChangeable();
  
  
  /**
   * Sets a component's flag that a fire-method is currently running.<br>
   * Used by the framework to minimize valueChanged/valueEntered processing.
   * 
   * @param fireRunning true if some fire-method is running
   */
  public void setFireRunning(boolean fireRunning);

  /**
   * Checks whether a fire-method is running.
   * 
   * @return true if some fire-method is running
   */
  public boolean isFireRunning();

  
  /**
   * Returns whether this component lost the keyboard focus due to {@link #transferFocus()}.
   * @return true if focus lost due to transferFocus
   */
  public boolean wasTransferFocus();
  
  
  /**
   * Returns whether this component lost the keyboard focus due to {@link #transferFocusBackward()}.
   * @return true if focus lost due to transferFocusBackward
   */
  public boolean wasTransferFocusBackward();
  
  
  /**
   * Returns whether this component lost the keyboard focus due to {@link java.awt.event.KeyEvent#VK_ENTER}.
   * @return true if pressing the Enter/Return key caused a focus lost
   */
  public boolean wasTransferFocusByEnter();
  
  
  /**
   * Returns whether the keyboard focus was gained due to {@link #transferFocus()}
   * in the opposite component.
   * @return true if this component gained the focus from the logically previous component
   * @see FormFocusTraversalPolicy
   */
  public boolean wasFocusGainedFromTransfer();
  
  /**
   * Returns whether the keyboard focus was gained due to {@link #transferFocusBackward()}
   * in the opposite component.
   * 
   * @return true if this component gained the focus from the logically next component
   * @see FormFocusTraversalPolicy
   */
  public boolean wasFocusGainedFromTransferBackward();  
  
  
  /**
   * Requests the keyboard focus for this component by appending
   * an event to the end of the event queue.<br>
   * Applications should not use requestFocus() or requestFocusInWindow()
   * unless the order in which the events are processed is unimportant.
   * This method guarantees that this component gets the focus when
   * all events queued so far have been processed.
   */
  public void requestFocusLater();
  
  
  /**
   * Sets the window that will fire a {@link FormWrapEvent} when this
   * components loses the keyboard focus. When fired the reference
   * to the window is cleared.
   * 
   * @param window the window (usually the parent window of this component).
   */
  public void setFormWrapWindow(FormWindow window);
  
  
  /**
   * Gets the parent window of this component.
   * 
   * @return the parent window, null if none
   */
  public Window getParentWindow();
  
  
  /**
   * Invalidates all container-relevant (cached) information
   * for the hierarchy of this component's parents.<br>
   * For optimization purposes. 
   */
  public void invalidateParentInfo();
  
  
  /**
   * Sets the online-help URL.<br>
   * If a help url is set for this component, a browser will
   * be launched to show that url. If there is no url
   * the next higher level container will determine the url,
   * and so forth.
   * 
   * @param helpURL the help url
   * @see FormHelper#openHelpURL(Component) 
   */
  public void setHelpURL(String helpURL);
  
  /**
   * Gets the online-help URL.
   * @return the help url, default is null
   */
  public String getHelpURL();
  
  
  /**
   * Displays online help for this component.<br>
   * Usually triggered by KeyEvent.VK_F1 or VK_HELP or a button.
   * If help is not configured for this component, the parent's
   * help is used.
   */
  public void showHelp();

  
  /**
   * Sets a hint that this component is used as a cell editor.<br>
   * Some components behave differently in tables than in forms, for example.
   * 
   * @param cellEditorUsage true if component is a cell editor, false if not.
   */
  public void setCellEditorUsage(boolean cellEditorUsage);
  
  /**
   * Returns whether this component is used as a cell editor.
   * 
   * @return true true if component is a cell editor, false if not.
   */
  public boolean isCellEditorUsage();
  
  
  /**
   * Prepares for FOCUS_LOST.<br>
   * Gives this component the opportunity to invoke fireValueEntered() earlier
   * than FOCUS_LOST is really received. Useful because the next focused component
   * is determined before FOCUS_LOST is sent to this component. Thus, if this component
   * changes the focusability of the next component, the focus policy would
   * probably pick the wrong component otherwise.
   */
  public void prepareFocusLost();
  
  
  /**
   * Sets whether this component is form traversable or not, i.e.
   * gets the focus or is skipped.
   *
   * @param formTraversable true if traversable, false if not
   */
  public void setFormTraversable(boolean formTraversable);
  
  /**
   * Returns whether this component is form traversable or not.
   * 
   * @return the focus traversable flag
   */
  public boolean isFormTraversable();
  
  
  
  
  // ------------------- Swing standard methods ----------------------
  
  
  /**
   * Returns whether this component has the keyboard focus.
   * @return true if has focus
   */
  public boolean hasFocus();
  
  
  /**
   * Transfers keyboard focus forward to the logically next component.
   * @see FormFocusTraversalPolicy
   */
  public void transferFocus();
  
  
  /**
   * Transfers keyboard focus forward to the logically previous component.
   * @see FormFocusTraversalPolicy
   */
  public void transferFocusBackward();
  
}