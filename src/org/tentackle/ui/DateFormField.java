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

// $Id: DateFormField.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.ui;

import org.tentackle.util.StringHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JTextField;
import javax.swing.text.Document;
import org.tentackle.util.Compare;




/**
 * FormField to edit a {@link Date} object.
 * <p>
 * The date can be entered in the specified format or as a shortcut.
 * The following shortcuts are defined:
 * <ul>
 * <li><tt>"5/29/"</tt>: expands to May 29 of the current year (midnight).</li>
 * <li><tt>"0529"</tt>: dto.</li>
 * <li><tt>"052906"</tt>: if the year is 4-digits in length the century will be added
 *  which is closest to the current date, i.e.:</li>
 * <li><tt>"5/29/99"</tt>: will be expanded to "05/29/1999" and _not_ "05/29/2099".</li>
 * <li><tt>"7:00"</tt>: today at 7:00am</li>
 * </ul>
 * 
 * Furthermore, the date can determined in relation
 * to a reference date by certain commands. By default,
 * the reference date is the current time:
 * 
 * <ul>
 * <li><tt>"/"</tt>: current date</li>
 * <li><tt>":"</tt>: current time</li>
 * <li><tt>"+3d"</tt>: today 0:00:00 plus 3 days</li>
 * <li><tt>"-2y"</tt>: today 2 years ago.</li>
 * <li><tt>"17"</tt>: the "smallest" unit of the field will be set to 17.
 *  For dates this means the 17th of the current month.
 *  If the smalles unit are minutes, it means current hour 17 minutes.</li>
 * <li><tt>"+14"</tt>: same as above but the value will be added (or subtracted if negative)
 *    to (from) the current time. For date fields, for example, this is again shorter than "+14d".</li>
 * <li><tt>"4m"</tt>: the unit according to the letter following the number will be set _and_
 *  the next "smaller" unit set to its minimum.
 *  In this example, the time (if it is a time field) will be set to 4 minutes and 0 seconds.
 *  Likewise, "6y" would mean "January 1st, 2006". Consequently, "8h" is an even shorter
 *  way to express "today at 8am" than "8:00".</li>
 * </ul>
 * 
 * The units are the same as described in {@link SimpleDateFormat} with some
 * minor differences:
 * <ul> 
 * <li><tt>"y" or "Y"</tt>: year(s)
 * <li><tt>"M"</tt>: month(s). In date fields without minutes a lowercase "m" works as well.
 * <li><tt>"w or W"</tt>: week(s) or calendar week. For example: "-2w" means "two weeks ago"
 *   but "30w" means the first day of week 30.
 * <li><tt>"d oder D"</tt>: day(s)
 * <li><tt>"h oder H"</tt>: hour(s). Notice that "-24h" means "24 hours ago" and is not
 *   the dame as "-1d" which means "yesterday 0am".
 * <li><tt>"m"</tt>: minute(s)
 * <li><tt>"s oder S"</tt>: second(s)
 * </ul>
 * 
 * 
 * The shortcuts (except the units) are locale dependent. In German, for example, the
 * shortcuts are as follows:
 * <ul>
 * <li><tt>"29.5."</tt>: ergänzt zum 29.Mai des aktuellen Jahres 0 Uhr.</li>
 * <li><tt>"2905"</tt>: dto.</li>
 * <li><tt>"290506"</tt>: dto. falls die Jahreszahl 4-stellig erwartet wird. Dabei wird
 *  das Jahrhundert so ergänzt, dass es möglichst nah am aktuellen Datum liegt, d.h.:</li>
 * <li><tt>"29.5.99"</tt>: wird auf "29.05.1999" erweitert und nicht auf "29.05.2099".</li>
 * <li><tt>"7:00"</tt>: heute um 7:00 Uhr</li>
 * </ul>
 *
 * @author harald
 */
public class DateFormField extends FormField {
  
  
  // default format
  private static String defFormat = StringHelper.shortDatePattern;
  
  static {
    FormHelper.registerLocaleRunnable(new Runnable() {
      public void run() {
        StringHelper.loadFormats();
        defFormat = StringHelper.shortDatePattern;
      }
    });
  }
  
  private String            format;           // format set by application, null if default format
  private String            oldDefaultFormat; // initially used default format
  private SimpleDateFormat  dateFormat;       // formatting
  private String            dateDelimiters;   // usually . or /
  private boolean           lenient;          // default format.setLenient()
  private Date              referenceDate;    // reference date for arith-funcs, null = now
  
  private static int currentYear;     // current year
  private static int currentCentury;  // current century, e.g. 2000
  private static int pastCentury;     // past century, e.g. 1900
  
  
  
  /**
   * Creates an empty DateFormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param columns the number of columns, 0 = minimum width
   */
  public DateFormField (Document doc, int columns) {
    super (doc, null, columns);
    if (currentYear == 0) {   // not initialized so far
      GregorianCalendar currentCalendar = new GregorianCalendar();
      currentCalendar.setTime(new Date());    // set current date to get year.
      currentYear    = currentCalendar.get(GregorianCalendar.YEAR);
      currentCentury = (currentYear / 100) * 100; // e.g. 2000
      pastCentury    = currentCentury - 100;      // e.g. 1900
    }
    setHorizontalAlignment(JTextField.CENTER);
  }

  /**
   * Creates an empty DateFormField with the default document model.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public DateFormField (int columns)  {
    this (null, columns);
  }

  /**
   * Creates an empty DateFormField with the default document model
   * and minimum width.<br>
   */
  public DateFormField () {
    this (0);
  }

  /**
   * Creates an empty DateFormField with the default document model,
   * mininum width and a given format.<br>
   * 
   * @param pattern the date format string
   */
  public DateFormField (String pattern) {
    this (0);
    setFormat(pattern);
  }

  

  public void setFormValue (Object date)  {
    setText (doFormat(date));
  }

  public String doFormat (Object date)  {
    return (date == null ? "" : getDateFormat().format((Date)date));
  }
  
  

  
  
  private void setMidnight(GregorianCalendar greg)  {
    greg.set(GregorianCalendar.HOUR_OF_DAY, 0);
    greg.set(GregorianCalendar.MINUTE, 0);
    greg.set(GregorianCalendar.SECOND, 0);
    greg.set(GregorianCalendar.MILLISECOND, 0);
  }


  /**
   * @return the date, null if field is empty
   */
  @Override
  public Date getFormValue ()  {

    // decode format string, retry twice
    for (int loop=0; loop < 3; loop++)  {

      errorOffset = -1;

      String str = getText().replace(getFiller(), ' ').trim();
      int slen = str.length();
      
      if (slen == 0)  { /* empty */
          return null;
      }
      
      String fmt = getFormat();   // this will also initialize the format if not yet done
      
      if (slen == 1 && dateDelimiters.indexOf(str.charAt(0)) >= 0)  {
        // a single delimiting character means: current
        return new Date();
      }
      
      if (str.indexOf('-') == 0 || str.indexOf('+') == 0 || (slen <= 2 && StringHelper.isAllDigits(str)) ||
          "sSmMhHdDwWyY".indexOf(str.charAt(slen-1)) >= 0) {
        /**
         * current +/-Nt expression, i.e. current time plus or minus
         * some seconds, minutes, hours, days, weeks, months or years.
         * E.g.: +1d
         * The type defaults to the least significant value according
         * to the format. I.e. if the format is dd.MM.yy hh:mm,
         * +7 means plus 7 minutes.
         * The + can also be ommitted for 1 or 2-digit numbers and means
         * 'set' instead of 'add'. 
         * I.e. 17 means 17th of current month (if date-format) or
         * 12h means 12:00
         */
        boolean setValue = Character.isDigit(str.charAt(0));  // true = set instead of add
        try {
          GregorianCalendar greg = new GregorianCalendar();
          if (referenceDate != null)  {
            greg.setTime(referenceDate);
          }
          char type = str.charAt(slen-1);
          int value = 0;
          if (Character.isDigit(type))  {
            // determine according to format
            if      (fmt.indexOf('s') >= 0) {
              type = 's';
            }
            else if (fmt.indexOf('m') >= 0) {
              type = 'm';
            }
            else if (fmt.indexOf('h') >= 0) {
              type = 'h';
            }
            else if (fmt.indexOf('H') >= 0) {
              type = 'H';
            }
            else if (fmt.indexOf('d') >= 0) {
              type = 'd';
            }
            else if (fmt.indexOf('M') >= 0) {
              type = 'M';
            }
            else if (fmt.indexOf('Y') >= 0) {
              type = 'y';
            }
            value = Integer.parseInt(str.charAt(0) == '+' ? str.substring(1) : str);
          }
          else  {
            value = Integer.parseInt(str.substring(str.charAt(0) == '+' ? 1 : 0, slen-1));
          }
          
          if (setValue) {
            switch (type) {
              case 's':
              case 'S':
                greg.set(GregorianCalendar.SECOND, value);
                break;
              case 'm':
                if (fmt.indexOf('m') == -1) {
                  // meant month (m entered instead of M)
                  greg.set(GregorianCalendar.MONTH, value - 1);
                }
                else  {
                  greg.set(GregorianCalendar.MINUTE, value);
                  greg.set(GregorianCalendar.SECOND, 0);
                }
                break;
              case 'h':
              case 'H':
                greg.set(GregorianCalendar.HOUR_OF_DAY, value);
                greg.set(GregorianCalendar.MINUTE, 0);
                greg.set(GregorianCalendar.SECOND, 0);
                break;
              case 'd':
              case 'D':
                greg.set(GregorianCalendar.DAY_OF_MONTH, value);
                setMidnight(greg);
                break;
              case 'w':
              case 'W':
                greg.set(GregorianCalendar.WEEK_OF_YEAR, value);
                greg.set(GregorianCalendar.DAY_OF_WEEK, greg.getFirstDayOfWeek());
                setMidnight(greg);
                break;
              case 'M':
                greg.set(GregorianCalendar.MONTH, value - 1);
                greg.set(GregorianCalendar.DAY_OF_MONTH, 1);
                setMidnight(greg);
                break;
              case 'y':
              case 'Y':
                if (value < 100)  {
                  if (value < 50) {
                    value += currentCentury;
                  }
                  else {
                    value += pastCentury;
                  }                  
                }
                greg.set(GregorianCalendar.YEAR, value);
                greg.set(GregorianCalendar.DAY_OF_YEAR, 1);
                setMidnight(greg);
                break;
            }
          }
          else  {
            switch (type) {
              case 's':
              case 'S':
                greg.add(GregorianCalendar.SECOND, value);
                break;
              case 'm':
                if (fmt.indexOf('m') == -1) {
                  // meant month (m entered instead of M)
                  greg.add(GregorianCalendar.MONTH, value);
                }
                else  {
                  greg.add(GregorianCalendar.MINUTE, value);
                }
                break;
              case 'h':
              case 'H':
                greg.add(GregorianCalendar.HOUR, value);
                break;
              case 'd':
              case 'D':
                greg.add(GregorianCalendar.DATE, value);
                setMidnight(greg);
                break;
              case 'w':
              case 'W':
                greg.add(GregorianCalendar.WEEK_OF_YEAR, value);
                setMidnight(greg);
                break;
              case 'M':
                greg.add(GregorianCalendar.MONTH, value);
                setMidnight(greg);
                break;
              case 'y':
              case 'Y':
                greg.add(GregorianCalendar.YEAR, value);
                setMidnight(greg);
                break;
            }
          }
          return greg.getTime();
        }
        catch (Exception e) {
          // fall through...
        }
      }

      try {
        Date date = new Date (getDateFormat().parse(str).getTime());
        // expand 66 to 1966 and 02 to 2002
        GregorianCalendar greg = new GregorianCalendar();
        greg.setTime(date);
        int year = greg.get(GregorianCalendar.YEAR);
        if (year/100 == 0)  {
          // user entered 66 instead of 1966
          if (year < 50) {
            greg.set(GregorianCalendar.YEAR, year + currentCentury);
          }
          else {
            greg.set(GregorianCalendar.YEAR, year + pastCentury);
          }
          return greg.getTime();
        }
        // else user entered 2001
        return (date);
      }

      catch (ParseException e) {
        errorOffset = e.getErrorOffset();
        // check for user entered 1.1. and meant 1.1.<current year>
        if (errorOffset > 0 && errorOffset == slen &&
            dateDelimiters.indexOf(str.charAt(errorOffset-1)) >= 0) {
          // last char was a date-delimiter: try appending current year
          setText(str + currentYear);
        }
        else { // check for user omitted the delimiters at all, e.g. 0105
          String newstr = "";                 // new generated input
          int dlen = dateDelimiters.length(); // length of delimiters
          int spos = 0;                       // index in user input
          int dpos = 0;                       // index in format
          while (spos < slen) {
            char c = str.charAt(spos);
            if (dateDelimiters.indexOf(c) >= 0)  {
              break; // some delimiter, real error
            }
            if (dpos < dlen && spos > 0 && spos % 2 == 0)  {
              // insert delimiter
              newstr += dateDelimiters.charAt(dpos++);
            }
            newstr += c;
            spos++;
          }
          if (spos == slen) {   // delimiters inserted
            if (slen % 2 == 0 && dpos < dlen) {
              newstr += dateDelimiters.charAt(dpos);
            }
            if (newstr.length() == 6) { // day + month. and year missing?
              newstr += currentYear;
            }
            setText(newstr);
          }
          else  {
            // try if time entered only: add current date.
            // the colon is international (at least in western countries)
            boolean timeOnly = true;
            int colonCount = 0;
            for (int i=0; i < slen; i++)  {
              char c = str.charAt(i);
              if (c == ':') {
                colonCount++;
              }
              else if (Character.isDigit(c) == false)  {
                timeOnly = false;
                break;
              }
            }
            if (timeOnly) {
              try {
                GregorianCalendar greg = new GregorianCalendar();
                greg.setTime(colonCount == 1 ? StringHelper.shortTimeFormat.parse(str) : StringHelper.timeFormat.parse(str));
                int hour = greg.get(GregorianCalendar.HOUR_OF_DAY);
                int minute = greg.get(GregorianCalendar.MINUTE);
                int second = greg.get(GregorianCalendar.SECOND);
                greg.setTime(new Date());   // today
                greg.set(GregorianCalendar.HOUR_OF_DAY, hour);
                greg.set(GregorianCalendar.MINUTE, minute);
                greg.set(GregorianCalendar.SECOND, second);
                errorOffset = -1;
                return greg.getTime();
              }
              catch (ParseException ex) {
                // did not work
              }
            }
            else  {
              // try appending 00:00:00 if only date entered (there is a small chance ;-)
              newstr = str + " 00:00:00";
              try {
                Date date = new Date (getDateFormat().parse(newstr).getTime());
                // worked!
                setText(newstr);
              }
              catch (ParseException ex) {
                // nice try, but didn't work out
              }
            }
          }
        }
      }
    }
    return null;
  }


  
  /**
   * Gets the SQL-Date.
   * 
   * @return the java.sql.Date, null if field is empty
   */
  public java.sql.Date getDate() {
    Date date = getFormValue();
    return date == null ? null : new java.sql.Date(date.getTime());
  }
  
  
  /**
   * Gets the SQL-Timestamp.
   * 
   * @return the java.sql.Timestamp, null if field is empty
   */
  public java.sql.Timestamp getTimestamp() {
    Date date = getFormValue();
    return date == null ? null : new java.sql.Timestamp(date.getTime());
  }
  
  /**
   * Gets the SQL-Time.
   * 
   * @return the java.sql.Time, null if field is empty
   */
  public java.sql.Time getTime() {
    Date date = getFormValue();
    return date == null ? null : new java.sql.Time(date.getTime());
  }
  

  
  
  
  private void setDateFormat(SimpleDateFormat fmt) {
    dateFormat = fmt;
    dateFormat.setLenient(lenient);

    // extract date-delimiters
    dateDelimiters = "";
    String f = fmt.toPattern();
    for (int i=0; i < f.length(); i++)  {
      char c = f.charAt(i);
      if (Character.isLetterOrDigit(c) == false)  {
        dateDelimiters += c;
      }
    }    
  }
  

  
  private SimpleDateFormat getDateFormat() {
    if (dateFormat == null ||
        (format == null && Compare.equals(oldDefaultFormat, defFormat) == false)) {
      /**
       * dateFormat not set or format not set by application and 
       * default format has changed due to locale change
       */
      setDateFormat(new SimpleDateFormat(defFormat));
      oldDefaultFormat = defFormat;
    }
    return dateFormat;
  }
  
  
  /**
   * @see SimpleDateFormat
   */
  public void setFormat (String pattern)  {
    // set the format string
    this.format = pattern;   // remember that it has been set by the application!
    setDateFormat(new SimpleDateFormat(pattern));
  }
  
  public String getFormat ()  {
    return format != null ? format : getDateFormat().toPattern();
  }
  
  
  /**
   * Sets the "lenient" flag for the date format.
   * 
   * @param lenient true if lenient
   * @see SimpleDateFormat
   */
  public void setLenient(boolean lenient) {
    getDateFormat().setLenient(lenient);
    lenient = dateFormat.isLenient();
  }

  /**
   * Returns the lenient flag for the date format
   * @return true if lenient
   */
  public boolean isLenient() {
    lenient = getDateFormat().isLenient();
    return lenient;
  }
  
  
  /**
   * Gets the reference date for the input shortcuts.
   * 
   * @return the reference date for input shortcuts, null = now (default)
   */
  public Date getReferenceDate() {
    return referenceDate;
  }

  /**
   * Sets the reference date for the input shortcuts.
   * 
   * @param referenceDate reference date for input shortcuts, null = now 
   */
  public void setReferenceDate(Date referenceDate) {
    this.referenceDate = referenceDate;
  }

}
