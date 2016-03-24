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

// $Id: AppDbOperation.java 470 2009-07-31 07:14:05Z svn $

package org.tentackle.appworx;

import org.tentackle.db.Db;
import org.tentackle.db.DbOperation;


/**
 * A {@code AppDbOperation} provides methods that are not part of {@link AppDbObject}s
 * and is associated to a {@link ContextDb}. Complex transactions are usually
 * {@code AppDbOperation}s. {@code AppDbOperation}s are remote capabable.
 * 
 * @author harald
 */
public abstract class AppDbOperation extends DbOperation {
  
  private ContextDb contextDb;   // application database context
  
  
  /**
   * Creates a db operation from a database context.
   * 
   * @param contextDb the database context
   */
  public AppDbOperation(ContextDb contextDb) {
    super(contextDb.getDb());
    this.contextDb = contextDb;
  }
  
  
  /**
   * Creates a db operation from a db connection.
   * This constructor is used in the RemoteDelegate
   * if the construction by the default ContextDb has failed.
   * In such cases methods of this AppDbOperation must get the
   * contextDb as an argument.
   * 
   * @param db the logical db connection
   */
  public AppDbOperation(Db db) {
    super(db);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the Db in ContextDb as well.
   */
  @Override
  public void setDb(Db db)  {
    if (contextDb != null)  {
      contextDb.setDb(db);
    }
    super.setDb(db);
  }

  
  /**
   * Sets the database context.
   * 
   * @param contextDb the context
   */
  public void setContextDb(ContextDb contextDb)  {
    this.contextDb = contextDb;
    if (contextDb != null)  {
      super.setDb(contextDb.getDb());
    }
  }
  
  /**
   * Gets the database context.
   * 
   * @return the database context
   */
  public ContextDb getContextDb() {
    return contextDb;
  }
  
}
