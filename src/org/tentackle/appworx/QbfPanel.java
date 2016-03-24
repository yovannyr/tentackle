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

// $Id: QbfPanel.java 336 2008-05-09 14:40:20Z harald $
// Created on September 18, 2002, 4:45 PM

package org.tentackle.appworx;

import org.tentackle.ui.FormPanel;
import java.awt.event.ActionEvent;



/**
 * A FormPanel with a QbfParameter suitable for a QbfPlugin.
 *
 * @author harald
 */
public class QbfPanel extends FormPanel {
  
  private QbfParameter  parameter;  // the qbf parameter
  
  
  /**
   * Creates a QbfPanel for a given qbf parameter.
   *
   * @param parameter the qbf parameter
   */
  public QbfPanel(QbfParameter parameter)  {
    this();
    setParameter(parameter);
  }
  
  
  /**
   * Creates an empty QbfPanel (for UI Designers).
   */
  public QbfPanel() {
    setName("QbfPanel");
  }
  
  
  /**
   * Sets the initial focus.<br>
   * The default implementation does nothing.
   * Needs to be overridden!
   */
  public void setInitialFocus() {
  }
  
  
  /**
   * Signals the Panel that results have been displayed.<br>
   * {@link AppDbObjectSearchDialog} invokes this method as soon as
   * the {@link AppDbObjectNaviPanel} has been installed. Can be used to set up listeners etc...
   * The default implementation does nothing.
   */
  public void resultsShown()  {
  }
    
  
  /**
   * Sets the qbf parameter.
   *
   * @param parameter the qbf parameter
   */
  public void setParameter(QbfParameter parameter)  {
    this.parameter = parameter;
  }
  
  
  /**
   * Gets the current parameter
   *
   * @return the parameter
   */
  public QbfParameter getParameter()  {
    return parameter;
  }
  
  
  /**
   * Notifies all ActionListeners (usually only one!) that the query will start
   */
  protected void fireActionPerformed () {
    fireActionPerformed(new ActionEvent (this, ActionEvent.ACTION_PERFORMED, "query"));
  }

  
}
