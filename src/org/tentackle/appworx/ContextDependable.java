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

// $Id: ContextDependable.java 416 2008-09-10 11:53:57Z harald $
// Created on September 6, 2002, 2:06 PM

package org.tentackle.appworx;


/**
 * Interface all AppDbObjects must implement.<br>
 * In multitenant applications the interface must be subclassed.
 * 
 * @see ContextDb
 * @author harald
 */
public interface ContextDependable {
  
  /**
   * Gets the database application context this object belongs to.
   * 
   * @return the context
   */
  public ContextDb getContextDb();
  
  
  /**
   * Sets this object's database application context.
   * 
   * @param contextDb
   */
  public void setContextDb(ContextDb contextDb);
  
  
  /**
   * Determines the object-ID(s) of the root object(s) of the context
   * and sets the attributes of the database object accordingly.
   */
  public void setContextId();
  
  
  /**
   * Gets the ID of the root object describing the context of this object.
   * 
   * @return the ID of the root object, 0 if in default context (no context)
   */
  public long getContextId();
  
  
  /**
   * Gets the additional condition to be used in the WHERE clause describing
   * the context.
   * <pre>
   * Example: return " AND " + FIELD_TENANTID + "=?";
   * </pre>
   * 
   * @return the condition, null if none necessary
   */
  public String getSqlContextCondition();
  
  
  /**
   * Gets the minimum context, i.e. the one that is sufficient for this object.
   * Objects may have a "higher" context they live in.
   * 
   * @return the base context, never null 
   */
  public ContextDb getBaseContext();
  
  
  /**
   * Creates a valid context for this object.<br>
   * Useful if the object has been loaded without a context or
   * to make the least significant context the object can live in.
   *
   * @return the new context, never null
   */
  public ContextDb makeValidContext();

}
