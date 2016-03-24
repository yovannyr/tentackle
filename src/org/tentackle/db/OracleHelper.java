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

// $Id: OracleHelper.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.db;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Oracle-specific utility methods.<p>
 *
 * Date-conversion: Oracle does not use an index on a DATE-column if passed as a Timestamp-variable
 * via a prepared statement, even if used in conjunction with an SQL-hint /*+ INDEX(...)
 * In such cases the TO_DATE-function must be explicitly used.
 *
 <pre>
 Example:
 
      if (getDb().isOracle()) {
        query.add(" AND m." + FIELD_ZEITPUNKT + ">=" + OracleHelper.timestampString(par.vonZeitpunkt));
      }
      else  {
        query.add(" AND m." + FIELD_ZEITPUNKT + ">=?", par.vonZeitpunkt);
      }
 
 
 or:
      if (getDb().isOracle()) {
        query.add(" AND m." + FIELD_ZEITPUNKT + ">=TO_DATE(?,'" + OracleHelper.ORA_TIMESTAMP_FORMAT + "')", 
                  OracleHelper.timestampFormat.format(par.vonZeitpunkt));
      }
      else  {
        query.add(" AND m." + FIELD_ZEITPUNKT + ">=?", par.vonZeitpunkt);
      }
 </pre>
 *
 *
 * Rule: if possible use TIMESTAMP column type for a Timestamp-java attribute. In such a case
 * at least Oracle 10 uses the correct index and you don't need OracleHelper.
 *
 * @author harald
 */
public class OracleHelper {
  
  // oracle format strings
  public static final String ORA_DATE_FORMAT      = "YYYY-MM-DD";
  public static final String ORA_TIME_FORMAT      = "HH24:MI:SS";
  public static final String ORA_TIMESTAMP_FORMAT = ORA_DATE_FORMAT + " " + ORA_TIME_FORMAT;
  
  private static final String ORA_TO_DATE_LEAD     = "TO_DATE('";
  private static final String ORA_TO_DATE_SEP      = "','";
  private static final String ORA_TO_DATE_TAIL     = "')";
  
  // java format strings
  private static final String JAVA_DATE_FORMAT      = "yyyy-MM-dd";
  private static final String JAVA_TIME_FORMAT      = "HH:mm:ss";
  private static final String JAVA_TIMESTAMP_FORMAT = JAVA_DATE_FORMAT + " " + JAVA_TIME_FORMAT;
  
  // java formats
  public static final DateFormat dateFormat      = new SimpleDateFormat(JAVA_DATE_FORMAT);
  public static final DateFormat timeFormat      = new SimpleDateFormat(JAVA_TIME_FORMAT);
  public static final DateFormat timestampFormat = new SimpleDateFormat(JAVA_TIMESTAMP_FORMAT);
  
  // oracle empty string (special because oracle treats "" as NULL)
  public static final String emptyString = " ";   // one blank
  
  
  
  /**
   * Converts a date to a string.
   * 
   * @param date the date
   * @return a TO_DATE-date-string
   */
  public static String dateString(Date date)  {
    StringBuilder sb = new StringBuilder();
    sb.append(ORA_TO_DATE_LEAD);
    sb.append(dateFormat.format(date));
    sb.append(ORA_TO_DATE_SEP);
    sb.append(ORA_DATE_FORMAT);
    sb.append(ORA_TO_DATE_TAIL);
    return sb.toString();
  }
  
 
  /**
   * Converts a time to a string.
   * 
   * @param time the time
   * @return a TO_DATE-time-string
   */
  public static String timeString(Time time)  {
    StringBuilder sb = new StringBuilder();
    sb.append(ORA_TO_DATE_LEAD);
    sb.append(timeFormat.format(time));
    sb.append(ORA_TO_DATE_SEP);
    sb.append(ORA_TIME_FORMAT);
    sb.append(ORA_TO_DATE_TAIL);
    return sb.toString();
  }
  
  
  /**
   * Converts a timestamp to a string.
   * 
   * @param timestamp the timestamp
   * @return a TO_DATE-timestamp-string
   */
  public static String timestampString(Timestamp timestamp)  {
    StringBuilder sb = new StringBuilder();
    sb.append(ORA_TO_DATE_LEAD);
    sb.append(timestampFormat.format(timestamp));
    sb.append(ORA_TO_DATE_SEP);
    sb.append(ORA_TIMESTAMP_FORMAT);
    sb.append(ORA_TO_DATE_TAIL);
    return sb.toString();
  }
  
}
