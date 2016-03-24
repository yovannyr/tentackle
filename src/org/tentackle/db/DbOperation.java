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

// $Id: DbOperation.java 470 2009-07-31 07:14:05Z svn $

package org.tentackle.db;

import org.tentackle.db.rmi.DbOperationRemoteDelegate;

/**
 * A {@code DbOperation} provides methods that are not part of {@link DbObject}s
 * and is associated to a {@link Db}-connection. Complex transactions are usually
 * {@code DbOperation}s. {@code DbOperation}s are remote capabable.
 * 
 * @author harald
 */
public abstract class DbOperation {
  
  private Db db;                    // the bound db connection
  
  
  /**
   * Creates a db operation.
   * 
   * @param db the logical db connection
   */
  public DbOperation(Db db) {
    setDb(db);
  }


  /**
   * Gets the some attributes and variables common to all objects of the same class.
   * Class variables for classes derived from {@link DbOperation} are kept in an
   * instance of {@link DbOperationClassVariables}.
   *
   * @return the class variables
   */
  public abstract DbOperationClassVariables getDbOperationClassVariables();
  

  /**
   * Sets the logical db connection for this operation.
   * 
   * @param db the db connection
   */
  public void setDb (Db db)  {
    this.db = db;
  }

  
  /**
   * Get the logical db connection for this operation.
   * 
   * @return the db connection
   */
  public Db getDb ()  {
    return db;
  }
  
  
  /**
   * Gets the delegate for remote connections.<br>
   * Each class has its own delegate.
   * 
   * @return the delegate for this object
   */
  public DbOperationRemoteDelegate getRemoteDelegate()  {
    return getDbOperationClassVariables().getRemoteDelegate(getDb());
  }
  
}
