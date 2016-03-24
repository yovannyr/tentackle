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

// $Id: SqlHelper.java 336 2008-05-09 14:40:20Z harald $
// Created on September 19, 2002, 10:44 AM

package org.tentackle.db;

import org.tentackle.util.ApplicationException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Some common methods for SQL-Processing.
 *
 * @author harald
 */
public class SqlHelper {
  
  /** epochal date zero: 1.1.1970 00:00:00 **/
  public static final Date       minDate       = new Date(0);
  /** midnight 00:00:00 **/
  public static final Time       minTime       = new Time(0);
  /** one second before midnight: 23:59:59 **/
  public static final Time       maxTime       = new Time(86399000);
  /** epochal timestamp zero: 1.1.1970 00:00:00.000 **/
  public static final Timestamp  minTimestamp  = new Timestamp(0);

  
  /**
   * Converts a string to a string that can be used as a parameter
   * for a LIKE-clause.
   * The following rules apply to the source-string:
   * <ul>
   * <li>"bla"  -&gt; "%bla%"</li>
   * <li>"=bla" -&gt; "bla%"</li>
   * <li>"bla=" -&gt; "%bla"</li>
   * <li>"="    -&gt; "%=%" (pathological case)</li>
   * </ul>
   * 
   * @param pattern the search pattern
   * @return the LIKE string
   */
  public static String toLikeString(String pattern) {
  
    String sqlPattern;    // returned LIKE-string
    
    if (pattern == null || pattern.length() == 0)  {
      sqlPattern = "%";
    }
    else  {
      if (pattern.compareTo("=") == 0)  {
        // pathological case!
        sqlPattern = "%=%";
      }
      else  {
        sqlPattern = pattern;
      }
    }

    char c = sqlPattern.charAt(0);
    if (c == '=') {
      // cut off leading '=' as it is not SQL-syntax
      sqlPattern = sqlPattern.substring(1);
    }
    else if (c != '%') {
      // prepend LIKE '%' if not '=' given
      sqlPattern = "%" + sqlPattern;
    }
    c = sqlPattern.charAt(sqlPattern.length()-1);
    if (c == '=') {
      // cut off trailing '=' as it is not SQL-syntax
      sqlPattern = sqlPattern.substring(0, sqlPattern.length()-1);
    }
    else if (c != '%') {
      // append LIKE '%' if not '=' given
      sqlPattern += '%';
    }

    return sqlPattern;
  }
  
  
  /**
   * Converts a string to a string that can be used as a parameter
   * for a LIKE-clause, nullpointer safe.
   * 
   * @param pattern the search pattern
   * @return the LIKE string
   * 
   * @see #toLikeString(java.lang.String) 
   */
  public static String toLikeStringWithNull(String pattern) {
    if (pattern != null && pattern.trim().length() > 0) {
      return toLikeString(pattern); 
    }
    return null;
  }
  
  
  /**
   * Converts an SQL-like-String to a regular expression.
   * <pre>
   * Example: "%blah_foo" -&gt; ".*blah.foo"
   * </pre>
   * 
   * @param likeString the LIKE-string
   * @return the regex
   */
  public static Pattern likeStringToPattern(String likeString)  {
    StringBuilder regex = new StringBuilder();
    if (likeString != null) {
      int len = likeString.length();
      for (int i=0; i < len; i++) {
        char c = likeString.charAt(i);
        if (c == '%') {
          regex.append(".*"); 
        }
        else if (c == '_') {
          regex.append(".");
        }
        else  {
          regex.append(c); 
        }
      }
    }
    return Pattern.compile(regex.toString());
  }
  
  
  
  /**
   * Gets the column names of an insert statement.<br>
   * (see {@link org.tentackle.appworx.History} for an example)
   *
   * @param sql is the original sql string
   * @return the column names as a comma separated string
   *
   * @throws ApplicationException of malformed sql
   */
  public static String extractColumnsFromInsertStatement(String sql) 
         throws ApplicationException {
    // first parentheses of: INSERT INTO table (column, column, column, ....) VALUES (...)
    int start = sql.indexOf('(');
    int end   = sql.indexOf(')');
    if (start < 0 || end < start) {
      throw new ApplicationException("malformed INSERT statement '" + sql + "'");
    }
    return sql.substring(start+1, end);
  }
  
  
  /**
   * Gets the values of an insert statement.<br>
   * (see {@link org.tentackle.appworx.History} for an example)
   *
   * @param sql is the original sql string
   * @return the extracted values as a comma separated string
   *
   * @throws ApplicationException of malformed sql
   */
  public static String extractValuesFromInsertStatement(String sql) throws ApplicationException {
    int start = sql.lastIndexOf('(');
    int end   = sql.lastIndexOf(')');    // last parentheses of: INSERT INTO table (....) VALUES (?,?.?,...?)
    if (start < 0 || end < start) {
      throw new ApplicationException("malformed INSERT statement '" + sql + "'");
    }
    return sql.substring(start+1, end);
  }
  
  
  /**
   * Gets only the first n-2 of extracted comma-separated list.<br>
   * Used to get all fields except ID and SERIAL.
   * 
   * @param sql is the original sql string
   * @return the extracted values as a comma separated string
   */
  public static String extractAllExceptIdAndSerial(String sql)  {
    StringTokenizer stok = new StringTokenizer(sql, ", ");
    StringBuilder buf = new StringBuilder(sql.length());
    int tokens = stok.countTokens();
    while (tokens-- > 2) {
      buf.append(stok.nextToken());
      buf.append(",");
    }
    return buf.toString();
  }
  
  
  
  /**
   * Converts a <tt>java.util.Date</tt> into an <tt>java.sql.Date</tt>.
   * @param date the util date
   * @return the sql date
   */
  public static Date toDate(java.util.Date date)  {
    return date == null ? null : new Date(date.getTime());
  }
  
  /**
   * Converts a GregorianCalendar into a  java.sql.Date.
   * @param cal the calendar
   * @return the date
   */
  public static Date toDate(java.util.GregorianCalendar cal)  {
    return cal == null ? null : new Date(cal.getTime().getTime());
  }
  
  
  /**
   * Converts a java.util.Date into a java.sql.Time.
   * 
   * @param date the date
   * @return the time of the day in date
   */
  public static Time toTime(java.util.Date date)  {
    return date == null ? null : new Time(date.getTime());
  }
  
  
  /**
   * Converts a GregorianCalendar into a java.sql.Time.
   * @param cal the calendar
   * @return the time of day
   */
  public static Time toTime(java.util.GregorianCalendar cal)  {
    return cal == null ? null : new Time(cal.getTime().getTime());
  } 
 
  
  /**
   * Converts a java.util.Date into a java.sql.Timestamp.
   * @param date the date
   * @return the timestamp
   */
  public static Timestamp toTimestamp(java.util.Date date)  {
    return date == null ? null : new Timestamp(date.getTime());
  }
  
  
  /**
   * Converts a GregorianCalendar into a java.sql.Timestamp.
   * @param cal the calendar
   * @return the timestamp
   */
  public static Timestamp toTimestamp(java.util.GregorianCalendar cal)  {
    return cal == null ? null : new Timestamp(cal.getTime().getTime());
  }
  
  
  /**
   * Gets the current date.
   * 
   * @return current Date
   */
  public static Date today()  {
    return new Date(System.currentTimeMillis());
  }
  
  
  /**
   * Gets the current time.
   * 
   * @return current Time
   */
  public static Time daytime()  {
    return new Time(System.currentTimeMillis());
  }
  
  
  /**
   * Gets the current system time plus an optional offset.
   *
   * @param offsetMillis the offset to the current system time in milliseconds
   * @return current Timestamp
   */
  public static Timestamp now(long offsetMillis)  {
    return new Timestamp(System.currentTimeMillis() + offsetMillis);
  }
  
  
  /**
   * Gets the current system time.
   *
   * @return current Timestamp
   */
  public static Timestamp now()  {
    return now(0);
  }
  

}
