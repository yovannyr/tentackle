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

// $Id: FormContainer.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Window;


/**
 * Tentackle GUI-containers must implement this interface.
 * <p>
 * FormContainers are aware of the data binding and provide
 * some more features like online help.
 * 
 * @author harald
 */
public interface FormContainer {

  /**
   * set the (optional) title.<br>
   * If the container is a window, the title is the window-title.
   * If the container is a panel, the title is "internal", i.e. may be used
   * by the application to trigger setting the window title, for example.
   * 
   * @param title the container's title, null if none
   */
  public void setTitle(String title);
  
  /**
   * Gets the container's title.
   * 
   * @return the container's title, null if none
   */
  public String getTitle();
  
  
  /**
   * Sets the values in all form components in this container and all
   * sub containers.<br>
   * Used to update the view from the data according to the data binding.
   */
  public void setFormValues();
  
  
  /**
   * Retrieves the values from all components in this container and
   * all sub containers.<br>
   * Used to update the data from the view according to the data binding.
   */
  public void getFormValues();

  
  /**
   * Sets the values in all unchanged form components in this container and
   * all sub containers.<br>
   * All changed components are left untouched.
   * 
   * @see FormComponent#isValueChanged() 
   */
  public void setFormValuesKeepChanged();


  /**
   * Sets a savepoint for all components in this container and
   * all sub containers.
   * @see FormComponent#saveValue()
   */
  public void saveValues();
  
  
  /**
   * Checks whether the contents of some components in this or sub containers
   * have changed since the last savepoint.
   * 
   * @return true if changed
   */
  public boolean areValuesChanged();
  
  
  /**
   * Signals that values in the components of the container may have changed.
   * The container typically uses that to invoke areValuesChanged() and enabling/disabling
   * a button (e.g. "save", see AppDbObjectDialog).
   */
  public void triggerValuesChanged();
  
  
  /**
   * Sets the changeable attribute of all components of this container and
   * all sub containers.
   * 
   * @param allChangeable true if all changeable (default)
   */
  public void setAllChangeable(boolean allChangeable);

  /**
   * Returns whether this container has the all-changeable attribute set.
   * 
   * @return true if all changeable (default)
   */
  public boolean isAllChangeable();
  
  
  /**
   * Sets whether the container should honour the allChangeable attribute.
   * 
   * @param honourAllChangeable true to honour allchangeable (default)
   */
  public void setHonourAllChangeable(boolean honourAllChangeable);
  
  /**
   * Returns whether the container should honour the allChangeable attribute.
   * 
   * @return true if honour allchangeable (default)
   */
  public boolean isHonourAllChangeable();
  
  
  /**
   * Gets the parent window.
   * 
   * @return the parent window, null if none
   */
  public Window getParentWindow();
  
  
  /**
   * Invalidates all container-relevant (cached) information
   * for the hierarchy of this container's parents.<br>
   * For optimization purposes. 
   */
  public void invalidateParentInfo();

  
  /**
   * Sets the online-help URL.<br>
   * If a help url is set for this container, a browser will
   * be launched to show that url if there is no help url
   * for the component the help was requested for.
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
   * Displays online help for this container.
   */
  public void showHelp();
  
}