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

// $Id: LegacyDbObjectClassVariables.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.db;

import java.util.Map;
import java.util.TreeMap;
import org.tentackle.util.StringHelper;

/**
 * Minimal class variables for LegacyDbObjects.
 *
 * @author harald
 */
public class LegacyDbObjectClassVariables {
  
  // all classes register here:
  private static Map<String,LegacyDbObjectClassVariables> clazzMap = new TreeMap<String,LegacyDbObjectClassVariables>();
  
  
  
  
  /**
   * the class
   */
  public Class<? extends LegacyDbObject> clazz;
  
  /**
   * the base-classname
   */
  public String classBaseName;
  
  /**
   * database table name
   */
  public String tableName;
  
  /**
   * name for a single object
   */
  public String singleName;
  
  /**
   * name for multiple objects
   */
  public String multiName;
  
  /**
   * number of db-columns, 0 = not known so far
   */
  public int fieldCount;
  
  /**
   * true if prepared statements should always be prepared (i.e. if
   * the statement is changing for some reasons, e.g. the tablename).
   */
  public boolean alwaysPrepare;
  
  /**
   * prepared statement ID for selectAll()
   */  
  public int selectAllStatementId;
  
  /**
   * prepared statement ID for insert()
   */
  public int insertStatementId;
  
  /**
   * prepared statement ID for update()
   */
  public int updateStatementId;
  
  /**
   * prepared statement ID for delete()
   */
  public int deleteStatementId;
  
  /**
   * prepared statement ID for exists()
   */
  public int existsStatementId;
  
  
  /**
   * Constructs a classvariable.
   *
   * @param clazz is the class of the derived DbObject
   * @param tableName is the SQL tablename
   * @param singleName text for a single object
   * @param multiName text for multiple objects
   * 
   * @throws IllegalStateException if already constructed
   */
  public LegacyDbObjectClassVariables(Class<? extends LegacyDbObject> clazz, String tableName, String singleName, String multiName)  {
    
    this.clazz = clazz;
    this.tableName = tableName;
    this.singleName = singleName;
    this.multiName = multiName;
    
    classBaseName = StringHelper.getClassBaseName(clazz);
    
    if (clazzMap.put(tableName, this) != null) {
      throw new IllegalStateException("classvariables for '" + tableName + "' already registered");
    }
  }
  
  
  /**
   * Gets the classvariables for a given tablename
   *
   * @param tableName is the database tablename
   * @return the classvariables or null if no such tablename
   */
  public static LegacyDbObjectClassVariables getVariables(String tableName) {
    return clazzMap.get(tableName);
  }
  
}
