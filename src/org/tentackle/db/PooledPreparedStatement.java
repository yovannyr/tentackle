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

// $Id: PooledPreparedStatement.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

  
/**
 * Pooled prepared statement.
 * <p>
 * Prepared statements are always pooled. Once prepared they can be used by any db.
 * Each statement gets a unique statement-id (the index in an array + 1) that is valid
 * for all Db and connections. The physical preparation is done when the statement is actually
 * used with a connection. Applications reference statements only by their id and because
 * the id is the same for all threads/Db/connections, it can be stored in a static context.
 * <p>
 * Note: we don't provide the holdability (since JDBC 3.0) cause there is no default
 * behaviour defined so far. Rule of thumb: always close cursors *before* the
 * the end of transaction.
 */
public class PooledPreparedStatement implements Comparable<PooledPreparedStatement> {
  
  // fast mapping of statements
  private static Map<PooledPreparedStatement, PooledPreparedStatement> stmtMap = new TreeMap<PooledPreparedStatement,PooledPreparedStatement>();
  private static int nextId;  // next Statement-ID (== count)
  private static PooledPreparedStatement[] statements = new PooledPreparedStatement[256];   // all statements
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  /**
   * Gets the current allocation size
   * 
   * @return the number of statements allocated
   */
  public static int getAllocationSize() {
    return statements.length;
  }

  
  
  private int     stmtId;                   // index in statement array (statement-ID)
  private String  sql;                      // the sql-string
  private int     resultSetType;            // one of ResultSet.TYPE_... (default is TYPE_FORWARD_ONLY)
  private int     resultSetConcurrency;     // one of ResultSet.CONCUR_...  (default is CONCUR_READ_ONLY)
  private String  str;                      // toString value
  private int     hash;                     // hashcode

  
  /**
   * Creates a new prepared statement.
   *
   * @param sqk the sql string to prepare
   * @param resultSetType a result set type; one of 
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @param resultSetConcurrency a concurrency type; one of
   *        <code>ResultSet.CONCUR_READ_ONLY</code> or
   *        <code>ResultSet.CONCUR_UPDATABLE</code>
   */
  private PooledPreparedStatement(String sql, int resultSetType, int resultSetConcurrency)  {
    
    this.stmtId               = nextId + 1;     // starting at 1, not 0!!
    this.sql                  = sql;
    this.resultSetType        = resultSetType;
    this.resultSetConcurrency = resultSetConcurrency;
    
    str = "ID=" + stmtId + ", SQL='" + sql + "'" + 
          ", resultSetType=" + resultSetType + 
          ", resultSetConcurrency=" + resultSetConcurrency;
    hash = str.hashCode();
  }
  
  
  /**
   * Gets the ID of this statement.
   * 
   * @return the statement id
   */
  public int getStatementId() {
    return stmtId;
  }
  
  /**
   * Gets the SQL string for this statement
   * 
   * @return the sql string
   */
  public String getSql() {
    return sql;
  }
  
  /**
   * @return the resultset type
   */
  public int getResultSetType() {
    return resultSetType;
  }
  
  /**
   * @return the resultset concurrency
   */
  public int getResultSetConcurrency() {
    return resultSetConcurrency;
  }
  

  /**
   * Compares two statements.
   * Statements are unique to sql + resultSetType + resultSetConcurrency
   * @param obj the statement to compare with
   * @return 0 if equal to this statement, else logically less or greater than obj
   */
  public int compareTo(PooledPreparedStatement obj) {
    int rv = sql.compareTo(obj.sql);
    if (rv == 0)  {
      rv = resultSetType - obj.resultSetType;
      if (rv == 0)  {
        rv = resultSetConcurrency - obj.resultSetConcurrency;
      }
    }
    return rv;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof PooledPreparedStatement && compareTo((PooledPreparedStatement)obj) == 0;
  }

  @Override
  public int hashCode() {
    return hash;
  }
  
  /**
   * @return the logging info
   */
  @Override
  public String toString()  {
    return str;
  }

  
  
  /**
   * Prepares a statement.
   *
   * @param sql the SQL string
   * @param resultSetType is one of ResultSet.TYPE_...
   * @param resultSetConcurrency is one of ResultSet.CONCUR_..
   * @return the statement ID (starting at 1)
   */
  public static int prepareStatement (String sql, int resultSetType, int resultSetConcurrency) {
    lock.writeLock().lock();
    try {
      PooledPreparedStatement newStmt = new PooledPreparedStatement(sql, resultSetType, resultSetConcurrency);
      PooledPreparedStatement oldStmt = stmtMap.get(newStmt);   // check if already prepared

      if (oldStmt != null)  {
        if (DbGlobal.logger.isFineLoggable()) {
          DbGlobal.logger.fine("re-use prepared statement " + oldStmt);
        }
        return oldStmt.stmtId;    // use already prepared statement
      }

      // statement is really new: append to array and increment ID-handle
      if (nextId >= statements.length) {
        // enlarge if necessary
        PooledPreparedStatement[] old = statements;
        statements = new PooledPreparedStatement[old.length << 1];    // double size
        System.arraycopy(old, 0, statements, 0, old.length);
      }
      oldStmt = stmtMap.put(newStmt, newStmt);
      if (oldStmt != null) {
        // lock didn't work??
        throw new IllegalStateException();
      }
      statements[nextId++] = newStmt;
      if (DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine("new statement prepared " + newStmt);
      }

      // new handle (always index + 1, starts at 1)
      return nextId;
    }
    finally {
      lock.writeLock().unlock();
    }
  }
  
  
  /**
   * Gets the statement according to the id.
   * @param stmtId the global ID of the statement
   * @return the statement
   */
  public static PooledPreparedStatement getStatement(int stmtId) {
    stmtId--; // starting from 0
    lock.readLock().lock();
    try {
      if (stmtId < 0 || stmtId >= statements.length) {
        throw new DbRuntimeException ("statement ID out of bounds [" + stmtId + "/" + statements.length + "]");
      }
      return statements[stmtId];
    }
    finally {
      lock.readLock().unlock();
    }
  }
  
  
  /**
   * Gets the ID of a prepared statement.
   * Use this function to re-use one-time prepared statements (i.e. for qbf)
   * @param sql the sql string
   * @param resultSetType the resultset type
   * @param resultSetConcurrency the resultset concurrency
   * @return the statement ID or 0 if no such statement
   */
  public static int getStatementId(String sql, int resultSetType, int resultSetConcurrency) {
    PooledPreparedStatement key = new PooledPreparedStatement(sql, resultSetType, resultSetConcurrency);
    lock.readLock().lock();
    try {
      PooledPreparedStatement stmt = stmtMap.get(key);
      return stmt == null ? 0 : stmt.stmtId;
    }
    finally {
      lock.readLock().unlock();
    }
  }
  
}
  
