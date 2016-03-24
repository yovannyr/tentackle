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

// $Id: AppDbObjectTransferData.java 336 2008-05-09 14:40:20Z harald $
// Created on August 22, 2002, 8:28 PM

package org.tentackle.appworx;

import org.tentackle.db.Db;
import java.io.Serializable;

/**
 * Transfer data for an {@link AppDbObject}.
 * <p>
 * Used in drag and drop.
 * 
 * @author harald
 */
public class AppDbObjectTransferData implements Serializable {
  
  private static final long serialVersionUID = -3764286209206994624L;
  
  private long   id;              // the object id
  private String classBaseName;   // the (base-)name of the class
  
  
  /**
   * Creates a transfer data object.
   * 
   * @param classBaseName the basename of the database object class
   * @param id the object ID
   */
  public AppDbObjectTransferData(String classBaseName, long id) {
    this.classBaseName = classBaseName;
    this.id            = id;
  }
  
  /**
   * Creates a transfer data object.
   * 
   * @param obj the database object
   */
  public AppDbObjectTransferData(AppDbObject obj) {
    this(obj.getClassBaseName(), obj.getId());
  }
  
  
  
  /**
   * Gets the object ID of the database object.
   * 
   * @return the object ID
   */
  public long getId()  {
    return id;
  }
  
  
  /**
   * Gets the basename of the database object class.
   * 
   * @return the class basenanme
   */
  public String getClassBaseName() {
    return classBaseName;
  }
  
  
  /**
   * Gets the class of the object.
   * 
   * @return the class
   * @throws ClassNotFoundException 
   */
  public Class<? extends AppDbObject> loadClass() throws ClassNotFoundException {
    return AppDbObject.loadClass(classBaseName);
  }
  
  
  /**
   * Selects the object from the db and sets its context.
   * 
   * @param db the database connection
   * @return the database object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  public AppDbObject getAppDbObject(Db db) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return AppDbObject.selectInValidContext(db, loadClass(), id);
  }
  
}
