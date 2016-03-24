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

// $Id: AppDbPseudoObject.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.appworx;

import org.tentackle.db.DbRuntimeException;
import org.tentackle.db.PreparedStatementWrapper;




/**
 * Read-Only database object class.<br>
 * 
 * Handy for queries or database operations that don't correspond
 * to a physical database table, e.g. queries with aggregate functions
 * or queries returning data from different tables or special
 * updates/inserts/deletes, etc...
 * AppDbPseudoObjects will also work on remote db-connections!
 * <p>
 * Hint for queries: use searchQbfCursor().
 */

public abstract class AppDbPseudoObject extends AppDbObject {
  
  
  /**
   * Creates a pseudo application database object for read only.
   * @param contextDb the database context
   */
  public AppDbPseudoObject(ContextDb contextDb) {
    super(contextDb);
    // each pseudo object gets its own unique "pseudo-ID"
    setId(getAppDbObjectClassVariables().countInstance());
  }
  
 /**
   * Creates a pseudo application database object for read only.
   */
  public AppDbPseudoObject() {
    super();
  }


  public int setFields(PreparedStatementWrapper st) {
    throw new DbRuntimeException(getClassName() + " is readonly!");
  }

  public int prepareInsertStatement() {
    throw new DbRuntimeException(getClassName() + " is readonly!");
  }

  public int prepareUpdateStatement() {
    throw new DbRuntimeException(getClassName() + " is readonly!");
  }
  
}
