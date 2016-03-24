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

// $Id: InteractiveError.java 387 2008-08-12 09:17:59Z harald $
// Created on December 11, 2004, 6:31 PM

package org.tentackle.appworx;

import java.io.Serializable;
import org.tentackle.ui.FormComponent;
import org.tentackle.util.Logger.Level;



/**
 * Description of an error.
 * <p>
 * {@link InteractiveError}s provide a semantic link between the
 * data- and the presentation-layer. Usually, the data is verified
 * at the data-layer and then decorated at the presentation-layer
 * by examining the error (e.g. assign a GUI-component).<br>
 * The {@link TooltipAndErrorPanel} is able to deal with a list
 * of InteractiveErrors.<br>
 * InteractiveErrors are serializable, so they can be generated at
 * the server and sent to the client. The GUI-component, which can be any
 * object, is transient, however (doesn't make any sense at the server-side anyway).
 *
 * @author harald
 */
public class InteractiveError implements Serializable {
  
  private static final long serialVersionUID = 4843189055769682985L;
  
  private Level level;              // error level
  private String text;              // error text
  private int errorNumber;          // optional error number
  
  private transient Object guiComponent;  // optional GUI component that causes the error, null if none
  
  
  /**
   * Creates an error.
   * 
   * @param text the error text
   * @param component the form component related to the error, null if none
   * @param level the severity (either {@link Level#WARNING} or {@link Level#SEVERE})
   */
  public InteractiveError(String text, FormComponent component, Level level)  {
    setText(text);
    setFormComponent(component);
    setLevel(level);
  }
  
  /**
   * Creates an error with the default {@link Level#WARNING}.
   * 
   * @param text the error text
   * @param component the form component related to the error
   */
  public InteractiveError(String text, FormComponent component) {
    this(text, component, Level.WARNING);
  }
  
  /**
   * Creates an error with the default {@link Level#WARNING}
   * and no related component.
   * 
   * @param text the error text
   */
  public InteractiveError(String text)  {
    this (text, null);
  }
  
  
  /**
   * Creates an error with a unique error code.
   * 
   * @param errorCode the error code
   * @param text the error text
   * @param level the severity (either {@link Level#WARNING} or {@link Level#SEVERE})
   */
  public InteractiveError(int errorCode, String text, Level level)  {
    setErrorNumber(errorCode);
    setText(text);
    setLevel(level);
  }
  
  /**
   * Creates an error with a unique error code.
   * 
   * @param errorCode the error code
   * @param text the error text
   */
  public InteractiveError(int errorCode, String text)  {
    this(errorCode, text, Level.WARNING);
  }
  
  /**
   * Creates an error with a unique error code and default {@link Level#WARNING}.
   * 
   * @param errorCode the error code
   */
  public InteractiveError(int errorCode)  {
    this(errorCode, null);
  }
  
  
  
  
  
  @Override
  public String toString()  {
    return getText();
  }
  
  
  /**
   * Gets the severity level.
   * 
   * @return one of {@link Level#WARNING} or {@link Level#SEVERE}
   */
  public Level getLevel() {
    return level;
  }  
  
  /**
   * Sets the severity level.
   * 
   * @param level  one of {@link Level#WARNING} or {@link Level#SEVERE}
   */
  public void setLevel(Level level) {
    this.level = level;
  }
  
  
  /**
   * Gets the error message.
   * 
   * @return the error text
   */
  public String getText() {
    return text;
  }
  
  /**
   * Sets the error message.
   * 
   * @param text the error text
   */
  public void setText(String text) {
    this.text = text;
  }
  
  
  /**
   * Gets the optional error number.
   * 
   * @return the error number
   */
  public int getErrorNumber() {
    return errorNumber;
  }
  
  /**
   * Sets the optional error number.
   * 
   * @param errorNumber the error number.
   */
  public void setErrorNumber(int errorNumber) {
    this.errorNumber = errorNumber;
  }
  
  
  /**
   * Gets the related swing GUI-component.
   * 
   * @return the component, null if none or guiComponent is not a FormComponent.
   */
  public FormComponent getFormComponent() {
    return guiComponent instanceof FormComponent ? (FormComponent)guiComponent : null;
  }
  
  /**
   * Sets the related swing GUI-component.
   * 
   * @param formComponent  the swing component, null if none
   */
  public void setFormComponent(FormComponent formComponent) {
    guiComponent = formComponent;
  }
  
  
  /**
   * Gets the related GUI-component.
   * Can be anything, depending on the presentation layer.
   * 
   * @return the component, null if none
   */
  public Object getGuiComponent() {
    return guiComponent;
  }
  
  /**
   * Sets the related GUI-component.
   * Can be anything, depending on the presentation layer.
   * 
   * @param guiComponent  the component, null if none
   */
  public void setGuiComponent(Object guiComponent) {
    this.guiComponent = guiComponent;
  }
  
}
