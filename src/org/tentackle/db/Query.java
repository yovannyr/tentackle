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
 */// $Id: Query.java 452 2009-03-13 14:01:46Z harald $
package org.tentackle.db;

import org.tentackle.util.ApplicationException;
import org.tentackle.util.BMoney;
import org.tentackle.util.DMoney;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;



/**
 * A database query.<br>
 * 
 * Combines the generation of an sql-string and parameter set for
 * the ResultSetWrapper. Useful for one-time queries entered by the user.<br>
 * Notice that by default the underlying prepared statement is closed
 * when the resultset (or cursor) is closed by the application.
 * You can change that behaviour, however.
 *
 * @author harald
 */
public class Query {

  private List<QueryItem> items;          // query parameters
  private int fetchSize;                  // fetchsize, 0 = default
  private int maxRows;                    // max. number of rows fetched at all, 0 = no limit
  private boolean closeStatementOnClose;  // true = close the prepared statement if the result set is closed (default)
  private int offset;                     // != 0 if add an offset clause
  private int limit;                      // != 0 if add a limit clause

  
  /**
   * Creates a query. 
   */
  public Query() {
    items = new ArrayList<QueryItem>();
    closeStatementOnClose = true;
  }

  
  /**
   * Sets an offset, i.e. number of rows to skip in a query.
   * By default the offset is 0.
   * @param offset the offset, 0 to disable
   */
  public void setOffset(int offset) {
    this.offset = offset;
  }

  /**
   * Gets the offset of this query.
   * 
   * @return the number of rows to skip, 0 if no offset
   */
  public int getOffset() {
    return offset;
  }

  
  /**
   * Sets the maximum number of rows to retrieve for this query.
   * 
   * @param limit the maximum number of rows, 0 if ulimited (default)
   */
  public void setLimit(int limit) {
    this.limit = limit;
  }

  /**
   * Gets the maximum number of rows for this query.
   * 
   * @return the maximum number of rows, 0 if ulimited (default)
   */
  public int getLimit() {
    return limit;
  }

  
  /**
   * Appends an sql-part and corresponding parameters to this query.
   * <pre>
   * Example: add(" AND FIELD_ID=?", object.getId());
   *          add(" AND FIELD_MONEY=?", object.getAmount());
   * </pre>
   *
   * @param sql is the SQL-string
   * @param data is an array of parameters
   */
  public void add(String sql, Object... data) {
    items.add(new QueryItem(sql, data));
  }

  
  /**
   * Sets whether to close the statement when resultset is closed.
   * The default is to close.
   * 
   * @param closeStatementOnClose true if statement is closed, false = left open
   */
  public void setCloseStatementOnClose(boolean closeStatementOnClose) {
    this.closeStatementOnClose = closeStatementOnClose;
  }

  /**
   * Returns whether statement is closed when result set is closed.
   * 
   * @return true if statement is closed, false = left open
   */
  public boolean isCloseStatementOnClose() {
    return closeStatementOnClose;
  }

  
  /**
   * Sets the optional fetchsize. 0 = drivers default.
   * 
   * @param fetchSize the fetchsize
   * @see ResultSetWrapper#setFetchSize(int) 
   */
  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  /**
   * Gets the fetchsize.
   * 
   * @return the fetchsize
   * @see ResultSetWrapper#getFetchSize() 
   */
  public int getFetchSize() {
    return fetchSize;
  }

  
  /**
   * Sets the optional maximum row count for this cursor.
   * 
   * @param maxRows the max rows, 0 = no limit
   */
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

  /**
   * Gets the maximum row count for this cursor
   * 
   * @return the max rows, 0 = no limit
   */
  public int getMaxRows() {
    return maxRows;
  }

  
  /**
   * Executes the query.
   *
   * @param db is the database connection
   * @param resultSetType is one of ResultSet.TYPE_...
   * @param resultSetConcurrency is one of ResultSet.CONCUR_..
   *
   * @return the result set
   */
  public ResultSetWrapper execute(Db db, int resultSetType, int resultSetConcurrency) {

    PreparedStatementWrapper st = db.getPreparedStatement(getSql(db), resultSetType, resultSetConcurrency);

    // apply parameters to statement
    apply(db, st);

    /**
     * set fetchsize and maxrows if != 0
     */
    if (fetchSize != 0) {
      st.setFetchSize(fetchSize);
    }
    if (maxRows != 0) {
      st.setMaxRows(maxRows);
    }

    /**
     * for postgres: run the query in an extra tx. Otherwise postgres will ignore fetchsize
     * and load everything into memory.
     */
    ResultSetWrapper rs;

    if (db.isPostgres() && fetchSize != 0 &&
            resultSetType == ResultSet.TYPE_FORWARD_ONLY &&
            resultSetConcurrency == ResultSet.CONCUR_READ_ONLY) {
      rs = st.executeQuery(true);   // true = commit tx when closing resultset
    }
    else {
      // standard dbms
      rs = st.executeQuery();
    }

    if (closeStatementOnClose) {
      rs.setCloseStatementOnclose(true);
    }

    return rs;
  }

  /**
   * Executes the query with ResultSet.CONCUR_READ_ONLY.
   *
   * @param db is the database connection
   * @param resultSetType is one of ResultSet.TYPE_...
   *
   * @return the result set
   */
  public ResultSetWrapper execute(Db db, int resultSetType) {
    return execute(db, resultSetType, ResultSet.CONCUR_READ_ONLY);
  }

  /**
   * Executes the query with ResultSet.TYPE_FORWARD_ONLY and ResultSet.CONCUR_READ_ONLY.
   *
   * @param db is the database connection
   *
   * @return the result set
   */
  public ResultSetWrapper execute(Db db) {
    return execute(db, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
  }

  
  /**
   * Gets the number of objects returned by this query.
   * <p>
   * This is not done by retrieving all rows but by a {@code "SELECT COUNT(*)"}.
   * Applications may use this in conjunction with limit and offset for pagination
   * (in web pages, for example).
   * 
   * @param db the db connection
   * @return the number of rows for this query
   */
  public int getEstimatedRowCount(Db db) {
    StringBuilder buf = buildSql();
    buf.insert(0, "SELECT COUNT(*) FROM (");
    buf.append(") AS F_O_O");
    
    PreparedStatementWrapper st = db.getPreparedStatement(buf.toString());
    applyParameters(st, 1);
    ResultSetWrapper rs = st.executeQuery();
    rs.setCloseStatementOnclose(true);
    try {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    finally {
      rs.close();
    }
    return 0;
  }
  
  
  /**
   * Gets the SQL-String of this query.<br>
   * Optionally modifies the sql query according to limit and offset.
   * 
   * @param db the db connection
   * @return the sql string
   */
  public String getSql(Db db) {
    StringBuilder sql = buildSql();
    db.addLimitOffsetToSql(sql, limit, offset);
    return sql.toString();
  }

  
  /**
   * Applies the query parameters to the statement.<br>
   * Optionally applies limit, offset as well.
   * 
   * @param db the database connection
   * @param st the prepared statement
   */
  public void apply(Db db, PreparedStatementWrapper st) {
    int ndx = 1;
    ndx = db.prependLimitOffsetToPreparedStatement(ndx, st, limit, offset);   // optionally prepend limit/offset
    ndx = applyParameters(st, ndx);                                           // set the query parameters
    db.appendLimitOffsetToPreparedStatement(ndx, st, limit, offset);          // optionally append limit/offset
  }
  
  
  /**
   * Apply the query parameters only.
   *
   * @param st the statement
   * @param ndx the starting index
   * @return the next paramater index
   */
  private int applyParameters(PreparedStatementWrapper st, int ndx) {

    for (QueryItem item : items) {
      for (int dataIndex = 0; dataIndex < item.data.length; dataIndex++) {
        Object data = item.data[dataIndex];
        if (data instanceof Number) {
          if (data instanceof DMoney) {
            st.setDMoney(ndx, (DMoney) data);
            ndx += 2;
          }
          else if (data instanceof BMoney) {
            st.setBMoney(ndx, (BMoney) data);
            ndx += 2;
          }
          else if (data instanceof Double) {
            st.setDouble(ndx++, (Double) data);
          }
          else if (data instanceof Float) {
            st.setFloat(ndx++, (Float) data);
          }
          else if (data instanceof Long) {
            st.setLong(ndx++, (Long) data);
          }
          else if (data instanceof Integer) {
            st.setInteger(ndx++, (Integer) data);
          }
          else if (data instanceof Short) {
            st.setShort(ndx++, (Short) data);
          }
          else if (data instanceof Byte) {
            st.setByte(ndx++, (Byte) data);
          }
          else if (data instanceof BigDecimal) {
            st.setBigDecimal(ndx++, (BigDecimal) data);
          }
        }
        else if (data instanceof String) {
          st.setString(ndx++, (String) data);
        }
        else if (data instanceof Character) {
          st.setChar(ndx++, (Character) data);
        }
        else if (data instanceof Boolean) {
          st.setBoolean(ndx++, (Boolean) data);
        }
        else if (data instanceof Timestamp) {
          st.setTimestamp(ndx++, (Timestamp) data);
        }
        else if (data instanceof Date) {
          st.setDate(ndx++, (Date) data);
        }
        else if (data instanceof Time) {
          st.setTime(ndx++, (Time) data);
        }
        else if (data instanceof Binary) {
          st.setBinary(ndx++, (Binary) data);
        }
        else {
          // unsupported type
          if (data == null) {
            DbGlobal.errorHandler.severe(new ApplicationException(
                    "null value in '" + item + "' arg[" + dataIndex + "]"),
                    "can't determine type");
          }
          else {
            DbGlobal.errorHandler.severe(new ApplicationException(
                    "unsupported type " + data.getClass() + " in '" + item + "' arg[" + dataIndex + "]"),
                    "can't convert data");
          }
        }
      }
    }

    return ndx;
  }
  
  
  
  // builds the "core" of the sql query string
  private StringBuilder buildSql() {
    StringBuilder buf = new StringBuilder();
    for (QueryItem item : items) {
      buf.append(item.sql);
    }
    return buf;
  }
  
  

  /**
   * bundles sql + parameters
   */
  private static class QueryItem {

    String sql;         // sql string
    Object[] data;      // data items

    QueryItem(String sql, Object... data) {
      this.sql = sql;
      this.data = data;
    }

    @Override
    public String toString() {
      return sql;
    }
  }
}
