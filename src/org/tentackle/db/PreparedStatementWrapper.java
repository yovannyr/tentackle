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

// $Id: PreparedStatementWrapper.java 457 2009-05-10 12:48:42Z harald $


package org.tentackle.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import org.tentackle.util.BMoney;
import org.tentackle.util.DMoney;
import org.tentackle.util.StringHelper;



/**
 * A wrapper for prepared statements.<br>
 * Will catch and report SQLExceptions and
 * keep track of being used only once after {@link Db#getPreparedStatement(int)}.
 *
 * @author harald
 */
public class PreparedStatementWrapper extends StatementWrapper {
  
  
  private int columnOffset;   // offset to add to column index, default is 0
       
  

  /**
   * Creates a wrapper for a prepared statement.
   * 
   * @param con
   * @param stmt
   */
  public PreparedStatementWrapper(ManagedConnection con, PreparedStatement stmt) {
    super(con, stmt);
    Db db = getDb();
    // if not a one-shot statement
    if (db.getFetchSize() != 0) {
      // set fixed fetchsize (0 = use drivers default)
      setFetchSize(db.getFetchSize());
    }

    if (db.getMaxRows() != 0) {
      // set maximum rows, 0 = no limit
      setMaxRows(db.getMaxRows());
    }
  }
  
  
  /**
   * Gets the wrapped prepared statement.
   * 
   * @return the prepared statement, null if closed
   */
  @Override
  public PreparedStatement getStatement()  {
    return (PreparedStatement)super.stmt;
  }
  
  
  /**
   * Sets the column offset.
   * Useful for eager loading or joining in general.
   *
   * @param columnOffset (default is 0)
   */
  public void setColumnOffset(int columnOffset) {
    this.columnOffset = columnOffset;
  }
  
  /**
   * Gets the column offset.
   * 
   * @return the current columnOffset
   */
  public int getColumnOffset()  {
    return columnOffset;
  }
  
  
  /**
   * Overridden because the sql string is ignored.
   */
  @Override
  int executeUpdateImpl(String sql) throws SQLException {
    unmarkReady(); // check for being marked ready and mark consumed
    getDb().setAlive(true);
    int count = ((PreparedStatement)stmt).executeUpdate();
    if (count > 0) {
      getDb().addToUpdateCount(count);
    }
    detachDb();
    return count;
  }
  
  
  /**
   * Executes the update.
   *
   * @return the row count
   */
  public int executeUpdate() {
    return super.executeUpdate(null);
  }
  
  
  /**
   * Implementation of executeQuery.
   */
  @Override
  ResultSet executeQueryImpl(String sql) throws SQLException {
    getDb().setAlive(true);
    return ((PreparedStatement)stmt).executeQuery();
  }
  
  

  /**
   * Executes the query.
   *
   * @param withinTx is true if start a transaction for this query.
   *
   * @return the result set as a ResultSetWrapper
   */
  public ResultSetWrapper executeQuery (boolean withinTx) {
    return super.executeQuery(null, withinTx);
  }
  
  
  /**
   * Executes the query.
   *
   * @return the result set as a ResultSetWrapper
   */
  public ResultSetWrapper executeQuery () {
    return executeQuery(false);
  }
  

  

  // ----------------------------------- the setters -----------------------------------


  /**
   * Sets the designated parameter to SQL <code>NULL</code>.
   *
   * @param pos the first parameter is 1, the second is 2, ...
   * @param type the SQL type code defined in <code>java.sql.Types</code>
   */
  public void setNull (int pos, int type) {
    try {
      ((PreparedStatement)stmt).setNull (columnOffset + pos, type);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setNull()_des_SQL-PreparedStatements"));
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>String</code> value. 
   * The driver converts this
   * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
   * (depending on the argument's
   * size relative to the driver's limits on <code>VARCHAR</code> values)
   * when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param s the parameter value
   * @param mapNull true if null values should be mapped to the empty string, else SQL NULL
   */
  public void setString (int p, String s, boolean mapNull) {
    try {
      if (s == null) {
        if (mapNull)  {
          ((PreparedStatement)stmt).setString(columnOffset + p, 
                  getDb().isOracle() ? OracleHelper.emptyString : StringHelper.emptyString);
        }
        else  {
          ((PreparedStatement)stmt).setNull(columnOffset + p, Types.VARCHAR);
        }
      }
      else {
        ((PreparedStatement)stmt).setString (columnOffset + p, s);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setString()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>String</code> value. 
   * The driver converts this
   * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
   * (depending on the argument's
   * size relative to the driver's limits on <code>VARCHAR</code> values)
   * when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param s the parameter value, null if the value should be set to SQL NULL
   */
  public void setString (int p, String s) {
    setString(p, s, false);
  }

  
  /**
   * Sets the designated parameter to the given Java <code>boolean</code> value.
   * The driver converts this
   * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param b the parameter value
   */
  public void setBoolean (int p, boolean b) {
    try {
      ((PreparedStatement)stmt).setBoolean (columnOffset + p, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setBoolean()_des_SQL-PreparedStatements"));
    }
  }

  
  /**
   * Sets the designated parameter to the given Java <code>Boolean</code> value.
   * The driver converts this
   * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param b the parameter value, null if the value should be set to SQL NULL
   */
  public void setBoolean (int p, Boolean b) {
    if (b == null) {
      setNull(columnOffset + p, Types.BIT);
    }
    else {
      setBoolean(columnOffset + p, b.booleanValue());
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>byte</code> value.  
   * The driver converts this
   * to an SQL <code>TINYINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param b the parameter value
   */
  public void setByte (int p, byte b) {
    try {
      ((PreparedStatement)stmt).setByte (columnOffset + p, b);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setByte()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Byte</code> value.  
   * The driver converts this
   * to an SQL <code>TINYINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param b the parameter value, null if the value should be set to SQL NULL
   */
  public void setByte (int p, Byte b) {
    if (b == null) {
      setNull(columnOffset + p, Types.TINYINT);
    }
    else {
      setByte(columnOffset + p, b.byteValue());
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>char</code> value. 
   * The driver converts this
   * to an SQL <code>VARCHAR</code>
   * when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param c the parameter value
   */
  public void setChar (int p, char c) {
    try {
      ((PreparedStatement)stmt).setString(columnOffset + p, String.valueOf(c));
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setChar()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Character</code> value. 
   * The driver converts this
   * to an SQL <code>VARCHAR</code>
   * when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param c the parameter value, null if the value should be set to SQL NULL
   * @param mapNull true if null values should be mapped to BLANK, else SQL NULL
   */
  public void setCharacter (int p, Character c, boolean mapNull) {
    if (c == null) {
      if (mapNull)  {
        setChar(columnOffset + p, ' ');
      }
      else  {
        setNull(columnOffset + p, Types.CHAR);
      }
    }
    else {
      setChar(columnOffset + p, c.charValue());
    }
  }
  
  /**
   * Sets the designated parameter to the given Java <code>Character</code> value. 
   * The driver converts this
   * to an SQL <code>VARCHAR</code>
   * when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param c the parameter value, null if the value should be set to SQL NULL
   */
  public void setCharacter (int p, Character c) {
    setCharacter(p, c, false);
  }


  /**
   * Sets the designated parameter to the given Java <code>short</code> value. 
   * The driver converts this
   * to an SQL <code>SMALLINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param s the parameter value
   */
  public void setShort (int p, short s) {
    try {
      ((PreparedStatement)stmt).setShort (columnOffset + p, s);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setShort()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Short</code> value. 
   * The driver converts this
   * to an SQL <code>SMALLINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param s the parameter value, null if the value should be set to SQL NULL
   */
  public void setShort (int p, Short s) {
    if (s == null) {
      setNull(columnOffset + p, Types.SMALLINT);
    }
    else {
      setShort(columnOffset + p, s.shortValue());
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>int</code> value.  
   * The driver converts this
   * to an SQL <code>INTEGER</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param i the parameter value
   */
  public void setInt (int p, int i) {
    try {
      ((PreparedStatement)stmt).setInt (columnOffset + p, i);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setInt()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Integer</code> value.  
   * The driver converts this
   * to an SQL <code>INTEGER</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param i the parameter value, null if the value should be set to SQL NULL
   */
  public void setInteger (int p, Integer i) {
    if (i == null) {
      setNull(columnOffset + p, Types.INTEGER);
    }
    else {
      setInt(columnOffset + p, i.intValue());
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>long</code> value. 
   * The driver converts this
   * to an SQL <code>BIGINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param l the parameter value
   */
  public void setLong (int p, long l) {
    if (getDb().isIngres()) {
      /**
       * long is translated to varchar in update statements by the Ingres JDBC-driver for some obscure reason.
       * Insert statements, however, work with long.
       * So, we simply translate it here to int (which is not ok, but there's no other workaround so far)
       */
      setInt (p, (int)l); 
    }
    else  {
      try {
        ((PreparedStatement) stmt).setLong(columnOffset + p, l);
      }
      catch (SQLException e)  {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setLong()_des_SQL-PreparedStatements"));
      }
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Long</code> value. 
   * The driver converts this
   * to an SQL <code>BIGINT</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param l the parameter value, null if the value should be set to SQL NULL
   */
  public void setLong (int p, Long l) {
    if (l == null) {
      setNull(columnOffset + p, Types.BIGINT);
    }
    else {
      setLong(columnOffset + p, l.longValue());
    }
  }

  
  /**
   * Sets the designated parameter to the given Java <code>float</code> value. 
   * The driver converts this
   * to an SQL <code>REAL</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param f the parameter value
   */
  public void setFloat (int p, float f) {
    try {
      ((PreparedStatement)stmt).setFloat (columnOffset + p, f);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setFloat()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Float</code> value. 
   * The driver converts this
   * to an SQL <code>REAL</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param f the parameter value, null if the value should be set to SQL NULL
   */
  public void setFloat (int p, Float f) {
    if (f == null) {
      setNull(columnOffset + p, Types.FLOAT);
    }
    else {
      setFloat(columnOffset + p, f.floatValue());
    }
  }


  /**
   * Sets the designated parameter to the given Java <code>double</code> value.  
   * The driver converts this
   * to an SQL <code>DOUBLE</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param d the parameter value
   */
  public void setDouble (int p, double d) {
    try {
      ((PreparedStatement)stmt).setDouble (columnOffset + p, d);
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setDouble()_des_SQL-PreparedStatements"));
    }
  }

  /**
   * Sets the designated parameter to the given Java <code>Double</code> value.  
   * The driver converts this
   * to an SQL <code>DOUBLE</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param d the parameter value, null if the value should be set to SQL NULL
   */
  public void setDouble (int p, Double d) {
    if (d == null) {
      setNull(columnOffset + p, Types.DOUBLE);
    }
    else {
      setDouble(columnOffset + p, d.doubleValue());
    }
  }
  
  
  /**
   * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value.  
   * The driver converts this to an SQL <code>NUMERIC</code> value when
   * it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param d the parameter value, null if the value should be set to SQL NULL
   */
  public void setBigDecimal (int p, BigDecimal d) {
    if (d == null) {
      setNull(columnOffset + p, Types.DECIMAL);
    }
    else  {
      try {
        ((PreparedStatement)stmt).setBigDecimal(columnOffset + p, d);
      }
      catch (SQLException e)  {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setDouble()_des_SQL-PreparedStatements"));
      }
    }
  }


  /**
   * Sets the designated parameter to a <code>BMoney</code> value.<br>
   * A BMoney will not be stored as a single field but as two fields:
   * <ol>
   * <li>a double representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * 
   * @param p the sql position
   * @param m the money value, null to set SQL NULL
   * @see #setDMoney(int, org.tentackle.util.DMoney) 
   */
  public void setBMoney (int p, BMoney m) {
    try {
      if (m == null) {
        ((PreparedStatement)stmt).setNull (columnOffset + p,   Types.DOUBLE);
        ((PreparedStatement)stmt).setNull (columnOffset + p+1, Types.INTEGER);
      }
      else  {
        ((PreparedStatement)stmt).setDouble (columnOffset + p,   m.doubleValue());  // set the value
        ((PreparedStatement)stmt).setInt    (columnOffset + p+1, m.scale());        // set the scale
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setBMoney()_des_SQL-PreparedStatements"));
    }
  }
  
  
  /**
   * Sets the designated parameter to a <code>DMoney</code> value.<br>
   * A DMoney will not be stored as a single field but as two fields:
   * <ol>
   * <li>a BigDecimal with a scale of 0 representing the value</li>
   * <li>an int representing the scale</li>
   * </ol>
   * This is due to most DBMS can't store arbitrary scaled decimals in
   * a single column, i.e. all values in the column must have the same scale.
   * 
   * @param p the sql position
   * @param m the money value, null to set SQL NULL
   * @see #setBMoney(int, org.tentackle.util.BMoney) 
   */
  public void setDMoney (int p, DMoney m) {
    try {
      if (m == null) {
        ((PreparedStatement)stmt).setNull (columnOffset + p,   Types.DECIMAL);
        ((PreparedStatement)stmt).setNull (columnOffset + p+1, Types.INTEGER);
      }
      else  {
        ((PreparedStatement)stmt).setBigDecimal(columnOffset + p, m.movePointRight(m.scale()));  // set the value
        ((PreparedStatement)stmt).setInt       (columnOffset + p+1, m.scale());                  // set the scale
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setDMoney()_des_SQL-PreparedStatements"));
    }
  }


  /**
   * Sets the designated parameter to the given <code>java.sql.Date</code> value
   * using the default time zone of the virtual machine that is running
   * the application. 
   * The driver converts this
   * to an SQL <code>DATE</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param d the parameter value
   * @param mapNull to map null values to 1.1.1970 (epochal time zero), else SQL NULL
   */
  public void setDate(int p, Date d, boolean mapNull) {
    try {
      if (d == null)  {
        if (mapNull)  {
          ((PreparedStatement)stmt).setDate (columnOffset + p, SqlHelper.minDate);
        }
        else  {
          ((PreparedStatement)stmt).setNull (columnOffset + p, Types.DATE);
        }
      }
      else  {
        ((PreparedStatement)stmt).setDate (columnOffset + p, d);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setDate()_des_SQL-PreparedStatements"));
    }
  }
  
  /**
   * Sets the designated parameter to the given <code>java.sql.Date</code> value
   * using the default time zone of the virtual machine that is running
   * the application. 
   * The driver converts this
   * to an SQL <code>DATE</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param d the parameter value, null if the value should be set to SQL NULL
   */
  public void setDate(int p, Date d)  {
    setDate(p, d, false);
  }

  
  /**
   * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.  
   * The driver
   * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
   * database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param ts the parameter value
   * @param mapNull to map null values to 1.1.1970 00:00:00.000 (epochal time zero), 
   *        else SQL NULL
   */
  public void setTimestamp(int p, Timestamp ts, boolean mapNull) {
    try {
      if (ts == null)  {
        if (mapNull)  {
          ((PreparedStatement)stmt).setTimestamp (columnOffset + p, SqlHelper.minTimestamp);
        }
        else  {
          ((PreparedStatement)stmt).setNull (columnOffset + p, Types.TIMESTAMP);
        }
      }
      else  {
        ((PreparedStatement)stmt).setTimestamp (columnOffset + p, ts);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setTimestamp()_des_SQL-PreparedStatements"));
    }
  }
  
  /**
   * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.  
   * The driver
   * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
   * database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param ts the parameter value, null if the value should be set to SQL NULL
   */
  public void setTimestamp(int p, Timestamp ts) {
    setTimestamp(p, ts, false);
  }
  
  
  /**
   * Sets the designated parameter to the given <code>java.sql.Time</code> value.  
   * The driver converts this
   * to an SQL <code>TIME</code> value when it sends it to the database.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param t the parameter value, null if the value should be set to SQL NULL
   */
  public void setTime(int p, Time t) {
    try {
      if (t == null)  {
        ((PreparedStatement)stmt).setNull (columnOffset + p, Types.TIME);
      }
      else  {
        ((PreparedStatement)stmt).setTime (columnOffset + p, t);
      }
    } 
    catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("Datenbankfehler_beim_setTime()_des_SQL-PreparedStatements"));
    }
  }
  
  
  /**
   * Sets the designated parameter to the given {@link Binary} value.
   * will 
   * The driver converts this to an SQL <code>BLOB</code> value when it sends it to the
   * database.
   * The implementation translates the Binary into an Inputstream and invokes
   * {@link PreparedStatement#setBinaryStream(int, java.io.InputStream, int)}.
   *
   * @param p the first parameter is 1, the second is 2, ...
   * @param b the parameter value, null if the value should be set to SQL NULL
   */
  public void setBinary(int p, Binary b) {
    try {
      if (b == null || b.getLength() == 0)  { // "empty" binaries are treated as null
        ((PreparedStatement)stmt).setNull (columnOffset + p, Types.LONGVARBINARY);
      }
      else  {
        ((PreparedStatement)stmt).setBinaryStream (columnOffset + p, b.getInputStream(), b.getLength());
      }
    } catch (SQLException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("setBinary_failed"));
    }
  }


}
