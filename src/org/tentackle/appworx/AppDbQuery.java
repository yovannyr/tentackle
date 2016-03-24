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

// $Id: AppDbQuery.java 385 2008-08-05 18:35:31Z harald $

package org.tentackle.appworx;

import org.tentackle.db.Query;
import org.tentackle.db.ResultSetWrapper;
import java.sql.ResultSet;

/**
 * Extends {@link Query} for application database objects.
 * 
 * @param <T> the database object class
 * @author harald
 */
public class AppDbQuery<T extends AppDbObject> extends Query {

  private QbfParameter qbfPar;      // the qbf parameter
  
  
  /**
   * Construct a default query from a QbfParameter.<br>
   * 
   * This is just a legacy constructor to save typing.
   * Use this constructor if the query should not be initialized with
   * the default query.
   *
   * @param qbfPar the qbfParameter
   */
  public AppDbQuery(QbfParameter qbfPar)  {
    super();
    this.qbfPar = qbfPar;
    setFetchSize(qbfPar.fetchSize);
    setMaxRows(qbfPar.maxRowCount);
    setOffset(qbfPar.offset);
    setLimit(qbfPar.limit);
  }
  
  
  /**
   * Construct a default query from a {@code QbfParameter} for this kind of object.<br>
   * Initializes the query with default query for the given object.
   *
   * @param qbfPar the qbfParameter
   * @param object the AppDbObject to create the default query for
   */
  public AppDbQuery(QbfParameter qbfPar, T object)  {
    this(qbfPar);
    add(object.getSqlSelectAllFields());
    if (qbfPar.pattern != null) {
      add(" AND " + object.getSqlPrefixWithDot() + AppDbObject.FIELD_NORMTEXT + " LIKE ?", 
          qbfPar.patternAsLikeSql());
    }
  }
  
  
  
  /**
   * Executes the query.
   *
   * @param resultSetType one of {@code ResultSet.TYPE_...}
   * @param resultSetConcurrency one of {@code ResultSet.CONCUR_...}
   * @return the result set
   */
  public ResultSetWrapper execute(int resultSetType, int resultSetConcurrency) {
    return super.execute(qbfPar.contextDb.getDb(), resultSetType, resultSetConcurrency);
  }
  
  /**
   * Executes the query with {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @param resultSetType one of {@code ResultSet.TYPE_...}
   * @return the result set
   */
  public ResultSetWrapper execute(int resultSetType) {
    return execute(resultSetType, ResultSet.CONCUR_READ_ONLY);
  }
  

  /**
   * Executes the query with {@link ResultSet#TYPE_FORWARD_ONLY}
   * and {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @return the result set
   */
  public ResultSetWrapper execute() {
    return execute(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
  }

  
  
  /**
   * Executes the query and returns a cursor.
   *
   * @param clazz the optional clazz, null = take from qbfPar
   * @param resultSetType one of {@code ResultSet.TYPE_...}
   * @param resultSetConcurrency one of {@code ResultSet.CONCUR_...}
   * @return the cursor
   */
  @SuppressWarnings("unchecked")
  public AppDbCursor<T> cursor(Class clazz, int resultSetType, int resultSetConcurrency)  {
    
    if (qbfPar.withEstimatedRowCount) {
      qbfPar.estimatedRowCount = getEstimatedRowCount(qbfPar.contextDb.getDb());
      qbfPar.withEstimatedRowCount = false;
    }
    
    // execute the query and return the cursor
    AppDbCursor<T> cursor = new AppDbCursor<T>(qbfPar.contextDb, 
                                               clazz == null ? (Class<T>)qbfPar.clazz : clazz, 
                                               execute(resultSetType, resultSetConcurrency));
    // set warning and limit parameters
    cursor.applyQbfParameter(qbfPar);
    return cursor;
  }
  
  /**
   * Executes the query and returns a cursor with {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @param clazz the optional clazz, null = take from qbfPar
   * @param resultSetType one of {@code ResultSet.TYPE_...}
   * @return the cursor
   */
  public AppDbCursor<T> cursor(Class clazz, int resultSetType)  {
    return cursor(clazz, resultSetType,  ResultSet.CONCUR_READ_ONLY);
  }
  
  /**
   * Executes the query and returns a cursor with {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @param resultSetType one of {@code ResultSet.TYPE_...}
   * @return the cursor
   */
  public AppDbCursor<T> cursor(int resultSetType)  {
    return cursor(null, resultSetType,  ResultSet.CONCUR_READ_ONLY);
  }
  
  
  /**
   * Executes the query and returns a cursor with {@link ResultSet#TYPE_FORWARD_ONLY}
   * and {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @param clazz the optional clazz, null = take from qbfPar
   * @return the cursor
   */
  public AppDbCursor<T> cursor(Class clazz)  {
    return cursor(clazz, ResultSet.TYPE_FORWARD_ONLY,  ResultSet.CONCUR_READ_ONLY);
  }
  
  /**
   * Executes the query and returns a cursor with {@link ResultSet#TYPE_FORWARD_ONLY}
   * and {@link ResultSet#CONCUR_READ_ONLY}.
   *
   * @return the cursor
   */
  public AppDbCursor<T> cursor()  {
    return cursor(null, ResultSet.TYPE_FORWARD_ONLY,  ResultSet.CONCUR_READ_ONLY);
  }
  
}
