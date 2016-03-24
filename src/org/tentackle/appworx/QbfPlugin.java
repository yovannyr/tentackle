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

// $Id: QbfPlugin.java 336 2008-05-09 14:40:20Z harald $
// Created on September 18, 2002, 11:48 AM

package org.tentackle.appworx;

import org.tentackle.db.DbCursor;
import java.util.Collection;


/**
 * Interface for a "plugin" to a search dialog.
 * <p>
 * The plugin determines a panel to enter the search criteria, 
 * performs its validation and executes the query.
 *
 * @author harald
 */
public interface QbfPlugin {
  
  /**
   * Initializes the search criteria.
   *
   * @param parameter the qbf parameter
   */
  public void setParameter(QbfParameter parameter);
  
  
  /**
   * Gets the search criteria (usually entered by the user).
   *
   * @return the qbf parameter
   */
  public QbfParameter getParameter();
  
  
  /**
   * Determines the panel to enter the search criteria.
   *
   * @return the search panel
   */
  public QbfPanel getPanel();
  
  
  /**
   * Performs a validation of the search criteria.
   *
   * @return true if running the query is ok
   */
  public boolean isParameterValid();
  
  
  /**
   * Executes the query.
   *
   * @return true if some records found, false if nothing found
   */
  public boolean executeQuery();
  
  
  /*
   * Gets the query result as a cursor.
   * The cursor is optional, i.e. if no cursor is available the
   * method getObjects() must be used.
   *
   * @return the cursor or null if no cursor
   */
  public DbCursor<? extends AppDbObject> getCursor();
  
  
  /**
   * Gets the query result as a collection of objects.
   * 
   * @return the collection, never null
   */
  public Collection<? extends AppDbObject> getObjects();
  
  
  /**
   * Gets the number of objects.
   * Keep in mind that using this method on a cursor based query
   * will read all objects to determine the size of the result set.
   *
   * @return the number of objects
   */
  public int getRowCount();
  
  
  /**
   * Checks whether the result is displayable, i.e. will not cause
   * any further problems if displayed.
   * 
   * @return true if result is displayable
   */
  public boolean isResultDisplayable();
  
  
  /**
   * Closes the plugin and release all ressources (esp. the cursor, if any)
   */
  public void cleanup();
  
  
  /**
   * Creates a new object in the database context of the plugin.
   *
   * @return the newly created object
   */
  public AppDbObject newAppDbObject();
  
  
  /**
   * Returns a message for "no such data found..." or whatever.
   *
   * @return the message
   */
  public String notFoundMessage();
  
}
