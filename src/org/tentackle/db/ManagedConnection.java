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

// $Id: ManagedConnection.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * A jdbc connection managed by the ConnectionManager.<br>
 *
 * The connection provides some additional features
 * such as a prepared statement cache
 * and translates SQLExceptions to DbRuntimeExceptions.
 * The ConnectionManager is responsible to attach and detach
 * the connection to a Db.
 *
 * @author harald
 */
public class ManagedConnection {
  
  private ConnectionManager manager;                      // the manager that created this connection
  private Connection connection;                          // the wrapped connection
  private PreparedStatementWrapper[] preparedStatements;  // all prepared statements for this connection
  private Db db;                                          // currently attached Db, null = free connection
  private long establishedSince;                          // connection established establishedSince... (epochal [ms])
  private long expireAt;                                  // connection shutdown at (epochal [ms]), 0 = forever
  private long detachedSince;                             // detached since when...
  private int index;                                      // connection index given by connection manager, -1 not used
  private int attachCount;                                // 0 = not attached, else number of times db is attached
  private int maxCountForClearWarnings;                   // trigger when to clearWarning() on a connection (0 = disabled)
  private int counterForClearWarnings;                    // current counter
  private boolean dead;                                   // connection is dead (comlink error detected)
  
  
  /**
   * Creates a managed connection.
   *
   * @param manager the connection manager
   * @param connection the low level JDBC connection
   */
  public ManagedConnection(ConnectionManager manager, Connection connection) {
    this.manager = manager;
    this.connection = connection;
    if (connection == null) {
      throw new IllegalArgumentException("connection is null");
    }
    establishedSince = System.currentTimeMillis();
    detachedSince = establishedSince;
    index = -1;
  }
  
  
  /**
   * Gets the connection manager
   * 
   * @return the manager
   */
  public ConnectionManager getManager() {
    return manager;
  }
  
  
  /**
   * Gets the low level connection.
   * 
   * @return the physical connection
   */
  public Connection getConnection() {
    return connection;
  }
  
  
  /**
   * Gets the epochal time when this connection was established.
   *
   * @return the time establishedSince in ms
   */
  public long getEstablishedSince() {
    return establishedSince;
  }
  
  
  /**
   * Sets the epochal time when this connection should be closed, if unused.
   *
   * @param expireAt the time in [ms], 0 = forever
   */
  public void setExpireAt(long expireAt) {
    this.expireAt = expireAt;
  }
  
  /**
   * Gets the epochal time when this connection should be closed, if unused.
   *
   * @return the time in [ms], 0 = forever
   */
  public long getExpireAt() {
    return expireAt;
  }


  /**
   * Gets the epochal time when this connection was detached.
   * <p>
   * Note that newly created conntections get their detach time initialized
   * from the current system time.
   *
   * @return the epochal time of last detach, 0 if attached
   */
  public long getDetachedSince() {
    return detachedSince;
  }
  
  
  /**
   * Sets the connection index.
   * Connection managers use that to manage connection lists.
   *
   * @param index the connection index
   */
  public void setIndex(int index) {
    this.index = index;
  }
  
  /**
   * Gets the connection index.
   *
   * @return the connection index.
   */
  public int getIndex() {
    return index;
  }
  
  
  /**
   * Marks a connection being dead.
   * <p>
   * Marking connections dead allows connection managers like
   * {@link MpxConnectionManager} to re-open connections before
   * being attached next time. Notice that not all connection managers
   * honour the dead-flag (makes only sense in servers, anyway).
   * <p>
   * Whenever the {@link DefaultErrorHandler} detects a communication link error,
   * it marks a connection dead.
   * @param dead true if marked dead, false not dead
   */
  public void setDead(boolean dead) {
    this.dead = dead;
  }
  
  /**
   * Returns whether connection is marked dead
   * @return true if dead
   */
  public boolean isDead() {
    return dead;
  }


  /**
   * Checks whether connection is still valid.<p>
   *
   * Implemented via a "SELECT 1" query.
   * If the check fails the connection is marked dead.
   *
   * @return true if connection still valid, false if invalid
   */
  public boolean verifyConnection() {
    try {
      Statement stmt = connection.createStatement();
      stmt.executeQuery("SELECT 1");
      stmt.close(); // closes also the result set of executeQuery
      return true;
    }
    catch (Exception ex) {
      setDead(true);
      return false;
    }
  }

  
  /**
   * Attaches a connection to a Db.
   * Connections must be attached before it can be used by statements
   * or starting a tx.
   * The method must only be invoked by a connection manager!
   *
   * @param db the logical Db to attach
   */
  public void attachDb(Db db) {
    if (db == null) {
      throw new IllegalArgumentException("db is null");
    }
    if (this.db != null) {
      if (this.db.equals(db)) {
        attachCount++;
        if (db.getConnection() != this) {
          throw new DbRuntimeException("db " + db + " lost current connection " + this + ", count=" + attachCount);
        }
      }
      else  {
        throw new DbRuntimeException("connection " + this + " already attached to " + this.db);
      }
    }
    else  {
      if (attachCount != 0) {
        throw new DbRuntimeException("attach count of unattached connection " + this + " is not 0, but " + attachCount);
      }
      this.db = db;
      db.setConnection(this);
      attachCount = 1;
      detachedSince = 0;
    }
    if (DbGlobal.logger.isFinerLoggable()) {
      DbGlobal.logger.finer(db + " attached to " + this + ", count=" + attachCount);
    }
  }
  
  
  /**
   * Checks whether a Db is attached to this connection.
   *
   * @return true if attached, else false.
   */
  public boolean isAttached() {
    return db != null;
  }
  
  
  
  /**
   * Detaches a connection.
   * Connections must be detached before they can be used by another Db.
   * The method must only be invoked by a connection manager!
   *
   * @param db the db to detach
   */
  public void detachDb(Db db) {
    if (db == null) {
      throw new IllegalArgumentException("db is null");
    }
    if (this.db != db) {
      throw new DbRuntimeException("connection " + this + " not attached to " + db + " (instead attached to " + this.db + ")");
    }
    if (DbGlobal.logger.isFinerLoggable()) {
      DbGlobal.logger.finer(db + " detached from " + this + ", count=" + attachCount);
    }
    
    if (--attachCount == 0) {
      this.db = null;
      db.setConnection(null);
      detachedSince = System.currentTimeMillis();
    }
    else {
      if (db.getConnection() != this) {
        throw new DbRuntimeException("db " + db + " lost current connection " + this + ", count=" + attachCount);
      }
    }
  }
  
  
  /**
   * Gets tha attached db.
   *
   * @return the db, null if not attached
   */
  public Db getDb() {
    return db;
  }
  
  
  /**
   * Gets the string representation of this connection.
   */
  @Override
  public String toString() {
    return connection == null ? "<closed connection>" : connection.toString();
  }
  
  
  
  /**
   * asserts that a connection is attached
   */
  private void assertAttached() {
    if (db == null) {
      throw new DbRuntimeException("connection " + this + " not attached to any Db");
    }
  }
  

  
  /**
   * Sets the autocommit feature.
   *
   * @param autoCommit true to enable autocomit, false to disable.
   */
  public void setAutoCommit(boolean autoCommit) {
    assertAttached();
    try {
      connection.setAutoCommit(autoCommit);
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("setting autocommit failed", ex);
    }
  }
  
  
  /**
   * Gets the autocommit value.
   *
   * @return the autocommit value.
   */
  public boolean getAutoCommit() {
    try {
      return connection.getAutoCommit();
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("getting autocommit failed", ex);
    }
  }
  
  
  /**
   * Performs a commit.
   */
  public void commit() {
    assertAttached();
    try {
      connection.commit();
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("commit failed", ex);
    }
  }
  
  
  /**
   * Performs a rollback.
   */
  public void rollback() {
    assertAttached();
    try {
      connection.rollback();
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("rollback failed", ex);
    }
  }
  
  
  
  
  
  /**
   * Reads all warnings, logs them and clear.
   */
  public void logAndClearWarnings() {
    try {
      // log connection warning
      SQLWarning warning = connection.getWarnings();
      while (warning != null) {
        DbGlobal.logger.warning(warning.getMessage());
        warning = warning.getNextWarning();
      }
      connection.clearWarnings();  // release memory used by warnings
      if (preparedStatements != null)  {
        // log statement warnings
        for (int i=0; i < preparedStatements.length; i++) {
          if (preparedStatements[i] != null)  {
            PreparedStatementWrapper stmt = preparedStatements[i];
            if (!stmt.isClosed()) { // if not closed
              warning = stmt.getStatement().getWarnings();
              while (warning != null) {
                DbGlobal.logger.warning(warning.getMessage());
                warning = warning.getNextWarning();
              }
              stmt.getStatement().clearWarnings();
            }
          }
        }
      }
    } 
    catch (Exception e) {
      throw new DbRuntimeException ("reading warnings failed", e); 
    }    
  }
  
  
  
  /**
   * Sets the countForClearWarnings trigger.
   * 
   * @param max the maxcount, 0 = app must eat the warnings
   */
  public void setMaxCountForClearWarnings(int max) {
    maxCountForClearWarnings = max;
  }
  
  /**
   * Gets the current setting for clearWarnings() trigger.
   * 
   * @return the maxcount, 0 = app must eat the warnings
   */
  public int getMaxCountForClearWarnings()  {
    return maxCountForClearWarnings; 
  }
  
  
  
  /**
   * Increments a counter and empties the warnings on the connection
   * and all prepared statements if a trigger value is reached. 
   * This is necessary for apps that don't
   * clearWarnings() on their own, otherwise filling up memory might occur.
   */
  public void countForClearWarnings() {
    if (maxCountForClearWarnings > 0) {
      counterForClearWarnings++;
      if (counterForClearWarnings >= maxCountForClearWarnings)  {
        logAndClearWarnings();
        counterForClearWarnings = 0;
      }
    }
  }
  
  
  
  /**
   * Forces a detach of the db connection.
   * The method is used from connection managers only, hence package scope.
   */
  void forceDetached() {
    if (isAttached()) {
      // database is still attached
      DbGlobal.logger.warning("connection " + this + " still attached to " + db);
      // close pending statements
      closeAllPreparedStatements(true);
      if (!isClosed() && getAutoCommit() == false) {
        // in transaction!!! rollback
        rollback();
        DbGlobal.logger.severe("pending transaction rolled back for " + db);
      }
      db.setConnection(null);
      db = null;
      attachCount = 0;
    }
    // else already detached
  }
  
  
  /**
   * Closes the connection.
   */
  public void close() {
    if (connection == null) {
      throw new DbRuntimeException("connection already closed");
    }
    logAndClearWarnings();
    forceDetached();
    closeAllPreparedStatements(false);
    try {
      connection.close();
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("closing the connection failed", ex);
    }
    connection = null;    // to GC
  }
  
  
  /**
   * Gets the connection's closed state.
   *
   * @return true if connection is closed
   */
  public boolean isClosed() {
    return connection == null;
  }
  
  
  /**
   * Closes all prepared statements.
   * 
   * @param onlyMarked true if close only pending statements
   */
  public void closeAllPreparedStatements(boolean onlyMarked) {
    if (preparedStatements != null)  {
      // close all statements
      if (!isClosed()) {
        for (int i=0; i < preparedStatements.length; i++) {
          if (preparedStatements[i] != null)  {
            PreparedStatementWrapper stmt = preparedStatements[i];
            if (!stmt.isClosed() &&   // if not already closed
                (!onlyMarked || stmt.isMarkedReady())) { // or all or only pending
              stmt.close();
            }
          }
        }
      }
      preparedStatements = null;  // to GC
    }
  }
  
  
  
  /**
   * Creates a one-shot statement.
   * One-shot statements (i.e. non-prepared statements) must attach the db as soon as they
   * are instantiated. The db is detached after executeUpdate or after executeQuery when
   * its result set is closed.
   *
   * @param db the Db to use for this one-shot
   * @param resultSetType a result set type; one of 
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @param resultSetConcurrency a concurrency type; one of
   *        <code>ResultSet.CONCUR_READ_ONLY</code> or
   *        <code>ResultSet.CONCUR_UPDATABLE</code>
   * @return a new <code>Statement</code> object that will generate
   *         <code>ResultSet</code> objects with the given type and
   *         concurrency
   */
  public Statement createStatement (Db db, int resultSetType, int resultSetConcurrency) {
    try {
      return connection.createStatement(resultSetType, resultSetConcurrency);
    } 
    catch (SQLException ex) {
      throw new DbRuntimeException("creating statement failed", ex);
    }
  }
  
  
  /**
   * Creates a one-shot statement.
   *
   * @param db the Db to use for this one-shot
   * @param resultSetType a result set type; one of 
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return a new <code>Statement</code> object that will generate
   *         <code>ResultSet</code> objects with the given type and
   *         concurrency CONCUR_READ_ONLY
   */
  public Statement createStatement (Db db, int resultSetType) {
    return createStatement(db, resultSetType, ResultSet.CONCUR_READ_ONLY);
  }
  
  
  /**
   * Creates a one-shot statement.
   * @param db the Db to use for this one-shot
   * @return a new <code>Statement</code> object that will generate
   *         <code>ResultSet</code> objects with type TYPE_FORWARD_ONLY and
   *         concurrency CONCUR_READ_ONLY
   */
  public Statement createStatement (Db db) {
    return createStatement(db, ResultSet.TYPE_FORWARD_ONLY);
  }

  
  
  
  /**
   * Gets a prepared statement.
   * The statement will be reused if already prepared.
   * Else it will be prepared according to the statement definition
   * in PooledPreparedStatement.
   * 
   * @param stmtId is the global statement id (> 0)
   * @return the prepared statement for this connection
   */
  public PreparedStatementWrapper getPreparedStatement (int stmtId)  {
    
    assertAttached();
    
    int size = PooledPreparedStatement.getAllocationSize();   // allocated DbStatements
    
    if (stmtId < 1 || stmtId > size) {
      throw new DbRuntimeException("internal error: stmtId out of range");
    }
    
    // enlarge if necessary
    if (preparedStatements == null) {
      preparedStatements = new PreparedStatementWrapper[size];
      for (int i=0; i < preparedStatements.length; i++)  {
        preparedStatements[i] = null;
      }      
    }
    if (size >= preparedStatements.length) {
      PreparedStatementWrapper[] old = preparedStatements;
      preparedStatements = new PreparedStatementWrapper[size];
      System.arraycopy(old, 0, preparedStatements, 0, old.length);
      // set the rest to null
      for (int i=old.length; i < preparedStatements.length; i++)  {
        preparedStatements[i] = null;
      }
    }


    // check if statement already prepared
    PreparedStatementWrapper prepStmt = preparedStatements[stmtId-1];
      
    try {
      if (prepStmt == null || prepStmt.isClosed()) {
        // we need to prepare it
        PooledPreparedStatement stmt = PooledPreparedStatement.getStatement(stmtId);
        if (DbGlobal.logger.isFineLoggable()) {
          DbGlobal.logger.fine("physically prepare statement " + stmt + " on " + this);
        }
        prepStmt = new PreparedStatementWrapper(this, 
                connection.prepareStatement(stmt.getSql(), stmt.getResultSetType(), stmt.getResultSetConcurrency()));
        preparedStatements[stmtId-1] = prepStmt;
        if (DbGlobal.logger.isFinerLoggable()) {
          DbGlobal.logger.finer("statement " + prepStmt + " prepared on " + this);
        }
      }
      else  {
        // already created
        if (DbGlobal.logger.isFinerLoggable()) {
          DbGlobal.logger.finer("use prepared statement " + PooledPreparedStatement.getStatement(stmtId) + " on " + this);
        }         
      }
    } 
    catch (SQLException e)  {
      throw new DbRuntimeException("creating prepared statement failed", e);
    }
    
    return prepStmt;
  }
  

  
}
