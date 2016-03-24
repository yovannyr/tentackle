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

// $Id: QbfParameter.java 385 2008-08-05 18:35:31Z harald $
// Created on September 18, 2002, 3:14 PM

package org.tentackle.appworx;

import org.tentackle.db.SqlHelper;
import org.tentackle.util.StringHelper;
import java.io.Serializable;


/**
 * Query parameters for QbfPlugin.
 *
 * @author harald
 */
public class QbfParameter implements Serializable {
  
  private static final long serialVersionUID = 6421500310182487469L;
  
  /** 
   * The object's class. null means "all classes".
   * @see AppDbObject#setClassPath(java.lang.String[]) 
   */
  public transient Class<? extends AppDbObject> clazz;
  
  /** database context **/
  public ContextDb contextDb;
  
  /** display only objects with granted read permission **/
  public transient boolean checkReadPermission;
  
  /** searchpattern **/
  public String pattern;

  /** number of rows to skip, 0 = default **/
  public int offset;
  
  /** max. number of rows to retrieve, 0 = unlimited (default) **/
  public int limit;
  
  /** 
   * Flag to retrieve the estimated number of rows in a separate SELECT COUNT(*)-query.
   * The flag is cleared after the query and the row count is stored in estimatedRowCount.
   * Useful in conjunction with offset/limit.
   */
  public boolean withEstimatedRowCount;
  
  /**
   * Optional estimated number of rows if withEstimatedRowCount was true.
   */
  public transient int estimatedRowCount;    // not transferred
  
  /**
   * The fetchsize for the query.<br>
   * If maxRowCount is set the fetchSize should be set to a reasonable value too,
   * because some JDBC-drivers interpret a fetchsize of 0 to retrieve all data in one bulk.
   * As a rule of thumb set fetchSize = maxRowCount / 2.
   * fetchSize will be passed to the remote server! It's not transient!
   */
  public int fetchSize;
  
  
  
  // hints for the GUI (not serialized), ignored if headless
  
  /** start search without querying the parameters **/
  public transient boolean searchImmediate;
  /** show dialog if searchImmediate failes **/
  public transient boolean showDialogIfSearchImmediateFailes; // 
  /** QbfPanel should not be visible (requires searchImmediate = true) **/
  public transient boolean qbfPanelInvisible;
  /** run the search in an extra thread (not the GUI-thread, i.e. modificationThread) **/
  public transient boolean searchInExtraThread;
  /** initially show table-view instead of tree-view **/
  public transient boolean showTable;
  /** view should be rebuilt for each new display of search results **/
  public transient boolean rebuildView;
  /** != null if use non-default formTableName for preferences **/
  public transient String  tableName;
  /** number of rows displayable without warning, 0 = unlimited (default)  **/
  public transient int     warnRowCount;
  /** maximum number of rows displayable, 0 = unlimited (default) **/
  public transient int     maxRowCount;
  /** time [ms] to sleep between warnRowCount packets, 0 = no sleep (default) **/
  public transient long    warnSleep;
  
  
  
  
  /**
   * Creates a QbfParameter.
   *
   * @param clazz the data object class
   * @param contextDb the database context
   */
  public QbfParameter(Class<? extends AppDbObject> clazz, ContextDb contextDb) {
    this.clazz = clazz;
    this.contextDb = contextDb.clone();    // cloned because db may be switched (see AppDbObjectSearchDialog)
  }
  
  
  /**
   * Check if QBF-parameter is completely empty, i.e. leads to a selectAll...
   *
   * @return true if parameter is empty
   */
  public boolean isEmpty()  {
    return getClass() == QbfParameter.class && (pattern == null || pattern.length() == 0);
  }

  
  /** 
   * Checks whether the parameter is valid for a query.
   * The default implementation returns tr
   *
   * @return if ok for query
   */
  public boolean isValid() {
    return true;
  }
  
  
  /**
   * Clears the parameter (user's input).
   */
  public void clear() {
    pattern = null;
  }
  
  
  @Override
  public String toString()  {
    return Locales.bundle.getString("Suchtext=") + (pattern == null || pattern.length() == 0 ? Locales.bundle.getString("<alles>") : pattern); 
  }
  
  
  /**
   * Gets the pattern as an "sql-LIKE"-string.
   *
   * @return the SQL-like string
   */
  public String patternAsLikeSql()  {
    return SqlHelper.toLikeString(StringHelper.normalize(pattern));
  }

}
