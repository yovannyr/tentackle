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

// $Id: StringHelper.java 450 2009-01-14 10:28:23Z harald $


package org.tentackle.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.tentackle.ui.FormHelper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

/**
 * Some handy methods for strings.
 *
 * @author  harald
 */
public class StringHelper {
  
  /** default integer format string **/
  public static final String integerPattern = "#0";
  /** default float/double format string **/
  public static final String floatDoublePattern = "#0.0#";
  /** default money format string **/
  public static final String moneyPattern = ",##0";
  
  
  /** localized format string for a timestamp **/
  public static String timestampPattern;
  /** localized format string for a date **/
  public static String datePattern;
  /** localized format string for a time **/
  public static String timePattern;
  /** localized format string for a short timestamp **/
  public static String shortTimestampPattern;
  /** localized format string for a short date **/
  public static String shortDatePattern;
  /** localized format string for a short time **/
  public static String shortTimePattern;
  
  /** localized {@link DateFormat} for a timestamp **/
  public static DateFormat timestampFormat;
  /** localized {@link DateFormat} for a date **/
  public static DateFormat dateFormat;
  /** localized {@link DateFormat} for a time **/
  public static DateFormat timeFormat;
  /** localized {@link DateFormat} for a short timestamp **/
  public static DateFormat shortTimestampFormat;
  /** localized {@link DateFormat} for a short date **/
  public static DateFormat shortDateFormat;
  /** localized {@link DateFormat} for a short time **/
  public static DateFormat shortTimeFormat;

  /** short letter for "debit" (soll) **/
  public static char debitLetter;
  private static String debitString;    // used in betragSH
  
  /** short letter for "credit" (haben) **/
  public static char creditLetter;
  private static String creditString;
  
  
  /**
   * Loads the localized formats.
   * Invoked whenever the {@link java.util.Locale} changes.
   */
  public static void loadFormats() {
    timestampPattern      = Locales.bundle.getString("timestampFormat");
    datePattern           = Locales.bundle.getString("dateFormat");
    timePattern           = Locales.bundle.getString("timeFormat");
    shortTimestampPattern = Locales.bundle.getString("shortTimestampFormat");
    shortDatePattern      = Locales.bundle.getString("shortDateFormat");
    shortTimePattern      = Locales.bundle.getString("shortTimeFormat");   
    
    timestampFormat       = new SimpleDateFormat(timestampPattern);
    dateFormat            = new SimpleDateFormat(datePattern);
    timeFormat            = new SimpleDateFormat(timePattern);
    shortTimestampFormat  = new SimpleDateFormat(shortTimestampPattern);
    shortDateFormat       = new SimpleDateFormat(shortDatePattern);
    shortTimeFormat       = new SimpleDateFormat(shortTimePattern);
    
    debitLetter = Locales.bundle.getString("debitLetter").charAt(0);
    debitString = " " + debitLetter + "    ";
    creditLetter = Locales.bundle.getString("creditLetter").charAt(0);
    creditString = " " + creditLetter;
  }
  
  
  static {
    loadFormats();
  }
  
  
  /** the empty string **/
  public static final String emptyString = "";
  
  /** the newline string **/
  public static final String lineSeparatorString = "\n";
  
  // fixed maps for converting single-char diacrits
  private static final String CRITS = "ÀÁÂÃÅÇÈÉÊËÌÍÎÏÐÑÒÓÔÕ×ØÙÚÛÝÞàáâãåçèéêëìíîïðñòóôõøùúûýþÿ";
  private static final String NORMS = "AAAAACEEEEIIIIDNOOOOXOUUUYBaaaaaceeeeiiiionooooouuuyby";
  private static char[] critMap;    // fast map built once from CRITS and NORMS
  
  
  
  /**
   * Converts special unicode characters (so-called diacrits) to standard ascii.
   * Supports also special german and northern european "umlauts".
   *
   * @param str the string to be converted
   * @return the converted string
   */
  public static String unDiacrit(String str)  {
    
    // build maptable if not yet done
    int mapLen = 0;
    try {
      mapLen = critMap.length >> 1;    // causes Nullp if not prepared
    }
    catch (NullPointerException e)  {
      // prepare fast map
      mapLen = CRITS.length();
      if (mapLen != NORMS.length()) {
        throw new IllegalStateException();
      }
      critMap = new char[mapLen + mapLen];
      for (int i=0; i < mapLen; i++) {
        critMap[i]          = CRITS.charAt(i);
        critMap[i + mapLen] = NORMS.charAt(i);
      }
    }    
    
    if (str != null)  {
      
      StringBuilder buf = new StringBuilder();
      int len = str.length();
      
      for (int i=0; i < len; i++) {
        
        char c = str.charAt(i);
        
        // check for german umlaute and ß
        if (c == 'Ä' || c == 'Æ') {
          buf.append(i < len-1 && Character.isUpperCase(str.charAt(i+1)) ? "AE" : "Ae");
          continue;
        }
        else if (c == 'Ü')  {
          buf.append(i < len-1 && Character.isUpperCase(str.charAt(i+1)) ? "UE" : "Ue");
          continue;
        }
        else if (c == 'Ö')  {
          buf.append(i < len-1 && Character.isUpperCase(str.charAt(i+1)) ? "OE" : "Oe");
          continue;
        }
        else if (c == 'ä' || c == 'æ') {
          buf.append("ae");
          continue;
        }
        else if (c == 'ü') {
          buf.append("ue");
          continue;
        }
        else if (c == 'ö') {
          buf.append("oe");
          continue;
        }
        else if (c == 'ß') {
          buf.append("ss");
          continue;
        }

        // convert possible diacrit (single char)
        for (int m=0; m < mapLen; m++) {
          if (critMap[m] == c)  {
            c = critMap[m+mapLen];
            break;
          }
        }
        
        if (c >= 32 && c <= 127)  {
          // append legal character
          buf.append(c);
        }
      }
      return buf.toString();
    }
    return null;
  }
  
  
  
  /**
   * The default normalizer used within the application.
   * Defaults to DefaultStringNormalizer.
   */
  public static StringConverter stringNormalizer = new DefaultStringNormalizer();

  /**
   * Normalizes a string (phonetically) for use as {@link org.tentackle.appworx.AppDbObject}.normText.
   * 
   * @param str the string to be normalized
   * @param normalizer the normalizer to use, null = global default
   * @return the normalized string
   */
  public static String normalize (String str, StringConverter normalizer)  {
    return normalizer == null ? stringNormalizer.convert(str) : normalizer.convert(str);
  }
  
  /**
   * Normalizes a string (phonetically) for use as {@link org.tentackle.appworx.AppDbObject}.normText
   * using {@link #stringNormalizer}.
   * 
   * @param str the string to be normalized
   * @return the normalized string
   */
  public static String normalize (String str)  {
    return normalize(str, null);
  }
  
  
  
  
  /**
   * Takes a string, surrounds it with double-quotes and escapes all double-quotes
   * already in the string according to Unix rules.
   * Example:
   * <pre>
   * Length 5" --> "Length 5\""
   * </pre>
   * 
   * @param str the string
   * @return the string in double quotes
   */
  public static String toDoubleQuotes(String str) {
    StringBuilder buf = new StringBuilder();
    buf.append('"');
    if (str != null)  {
      int len = str.length();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if (c == '"') {
          buf.append('\\');
        }
        else if (Character.isISOControl(c)) {
          c = ' ';    // transform any controls to spaces
        }
        buf.append(c);
      }
    }
    buf.append('"');
    return buf.toString();
  }
  
  
  
  /**
   * Takes a string, surrounds it with double-quotes and escapes all double-quotes
   * already in the string according to Windows-rules.
   * Example:
   * <pre>
   * Length 5" --> "Length 5""
   * </pre>
   * 
   * @param str the string
   * @return the string in double quotes
   */
  
  public static String toWindowsDoubleQuotes(String str) {
    StringBuilder buf = new StringBuilder();
    buf.append('"');
    if (str != null)  {
      int len = str.length();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if (c == '"') {
          buf.append('"');
        }
        else if (Character.isISOControl(c)) {
          c = ' ';    // transform any controls to spaces
        }
        buf.append(c);
      }
    }
    buf.append('"');
    return buf.toString();
  }
  
  
  
  /**
   * Takes a string and returns one with a given length, cutting or
   * filling up with fillchars, whatever appropriate.
   * 
   * @param str the string
   * @param length the length of the returned string
   * @param filler the character to fill up if str is too short
   * @return the string with the desired length
   */
  public static String toFixedLength(String str, int length, char filler)  {
    int len = 0;
    StringBuilder buf = new StringBuilder();
    if (str != null)  {
      len = str.length();
      if (len > length) {
        buf.append(str.substring(0, length));
        len = length;
      }
      else  {
        buf.append(str);
      }
    }
    while (len < length)  {
      buf.append(filler);
      len++;
    }
    return buf.toString();
  }
  
  /**
   * Takes a string and returns one with a given length, cutting or
   * filling up with spaces, whatever appropriate.
   * 
   * @param str the string
   * @param length the length of the returned string
   * @return the string with the desired length
   */
  public static String toFixedLength(String str, int length)  {
    return toFixedLength(str, length, ' ');
  }
  
  
  /**
   * Takes a string and returns one with a given length, cutting or
   * filling up with fillchars from the left, whatever appropriate.
   * 
   * @param str the string
   * @param length the length of the returned string
   * @param filler the character to fill up if str is too short
   * @return the string with the desired length
   */
  public static String toFixedLengthLeftFill(String str, int length, char filler)  {
    int len = 0;
    StringBuilder buf = new StringBuilder();
    if (str != null)  {
      len = str.length();
      if (len > length) {
        buf.append(str.substring(0, length));
        len = length;
      }
      else  {
        buf.append(str);
      }
    }
    while (len < length)  {
      buf.insert(0, filler);
      len++;
    }
    return buf.toString();
  }
  
  /**
   * Takes a string and returns one with a given length, cutting or
   * filling up with spaces from the left, whatever appropriate.
   * 
   * @param str the string
   * @param length the length of the returned string
   * @return the string with the desired length
   */
  public static String toFixedLengthLeftFill(String str, int length)  {
    return toFixedLengthLeftFill(str, length, ' ');
  }
    
  
  
  
  
  /**
   * Filters illegal chars for DTA-Files (German "DATENAUSTAUSCH" banking format).
   * 
   * @param str the string
   * @return the DTA conform string
   */
  public static String toDTA(String str)  {
    if (str != null)  {
      str = str.toUpperCase();
      int len = str.length();
      StringBuilder sbuf = new StringBuilder();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if ((c >= '0' && c <= '9') ||
            (c >= 'A' && c <= 'Z') ||
            c == ' ' || c == '.' || c == ',' ||
            c == '&' || c == '-' || c == '/' ||
            c == '+' || c == '*' || c == '$' ||
            c == '%' || c == 'Ä' || c == 'Ö' ||
            c == 'Ü' || c == 'ß') {
          sbuf.append(c);     
        }
        else  {
          sbuf.append(' ');
        }
      }
      str = sbuf.toString();
    }
    return str;
  }
  
  
  /**
   * Filters illegal chars for Java variable names.
   * 
   * @param str the string
   * @return the java conform string
   */
  public static String toVarName(String str)  {
    if (str != null)  {
      int len = str.length();
      StringBuilder sbuf = new StringBuilder();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if ((c >= '0' && c <= '9') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= 'a' && c <= 'z')) {
          sbuf.append(c);     
        }
        else if (c == '_' || Character.isWhitespace(c)) {
          sbuf.append('_');
        }
      }
      str = sbuf.toString();
    }
    return str;
  }
  
  
  /**
   * Checks if a string contains only digits or whitespaces, i.e.
   * no illegal char in a number string.
   *
   * @param str the string to check
   * @param whitespaceAllowed true if whitespaces are allowed
   *
   * @return true if no illegal char detected, false otherwise
   */
  public static boolean isAllDigits(String str, boolean whitespaceAllowed) {
    if (str != null)  {
      int len = str.length();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if (Character.isDigit(c) == false &&
            (whitespaceAllowed == false || Character.isWhitespace(c) == false)) {
          return false;
        }
      }
      return true;    // all digits or whitespaces
    }
    return true;    // null string is the same as empty string
  }
  
  
  /**
   * checks if a string contains only digits, i.e.
   * no non-number char in string, even no whitespace.
   *
   * @param str the string to check
   *
   * @return true if no illegal char detected, false otherwise
   */
  public static boolean isAllDigits(String str) {
    return isAllDigits(str, false);
  }
  
  
  
  
  private static DecimalFormat betragFormat = new DecimalFormat(",##0");

  
  /**
   * Translates a money value to a string with a suffix
   * indicating whether debit or credit. Commonly used in financial
   * accounting applications.
   * 
   * @param money the amount of money
   * @param debit true if the amount is debit
   * @return the formatted value
   */
  public static String debitCreditToString(BMoney money, boolean debit) {
    if (money == null) {
      return emptyString;
    }
    FormHelper.setScale(betragFormat, money.scale());
    return debit ? (betragFormat.format(money) + debitString) : 
                   ("    " + betragFormat.format(money) + creditString);     
  }
  
  
  
  /**
   * Trims a string.
   * The method is nullpointer safe.
   * 
   * @param s the string, may be null
   * @param max the maximum number of characters, 0 = minimum length
   * @return the trimmed string, null if s == null
   */
  public static String trim(String s, int max) {
    if (s != null)  {
      s = s.trim();
      if (max > 0 && s.length() > max)  {
        s = s.substring(0, max);
      }
    }
    return s;
  }

  
  /**
   * Trims a string.
   * The method is nullpointer safe.
   * 
   * @param s the string, may be null
   * @return the trimmed string, null if s == null
   * @see #trim(java.lang.String, int) 
   */
  public static String trim(String s) {
    return trim(s, 0);
  }
  

  
  /**
   * Checks if string contains only whitespaces.
   *
   * @param str the string to check
   * @return true if null, empty or all whitespace, false if at least one non-whitespace-character found
   */
  public static boolean isAllWhitespace(String str) {
    if (str != null)  {
      int len = str.length();
      for (int i=0; i < len; i++) {
        char c = str.charAt(i);
        if (Character.isWhitespace(c) == false) {
          return false;
        }
      }
      return true;    // all whitespaces
    }
    return true;  // no non-whitespace at all
  }
  
  
  /**
   * Gets the first line from a multi-line string.
   * Nice in tables.
   * @param str the multiline string
   * @return the first line, null if str == null
   */
  public static String firstLine(String str) {
    if (str != null) {
      int ndx = str.indexOf('\n');
      return ndx < 0 ? str : str.substring(0, ndx);
    }
    return null;
  }
  
  
  

  private static final char[] hexDigits = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'
  };
  
  /**
   * Creates a human-readable hex-String out of a byte-array (e.g. from MessageDigest MD5sum).
   * 
   * @param binaryData the data, may be null
   * @return the formatted hex string , null if data was null
   */
  public static String toHexString(byte[] binaryData)  {
    if (binaryData != null) {
      char[] text = new char[binaryData.length << 1];
      int j = 0;
      byte b;

      for (int i=0; i < binaryData.length; ++i) {
        b = binaryData[i];
        text[j++] = hexDigits[(b & 0xf0) >> 4];
        text[j++] = hexDigits[b & 0x0f];
      }
      return new String(text);
    }
    return null;
  }


  /**
   * Converts a single (unicode) char to a byte-array.
   * @param c the character
   * @return the byte[2] array
   */
  public static byte[] toBytes(char c)  {
    return new byte[] { (byte)(c & 0xff), (byte)((c >> 8) & 0xff) };
  }  
  
  
  /**
   * Converts a char-array to a byte-array.
   * @param chars the character array
   * @return the byte array
   */
  public static byte[] toBytes(char[] chars)  {
    if (chars != null) {
      byte[] b = new byte[chars.length << 1];
      int j = 0;
      char c;
      for (int i=0; i < chars.length; ++i)  {
        c = chars[i];
        b[j++] = (byte)(c & 0xff);
        b[j++] = (byte)((c >> 8) & 0xff);
      }
      return b;
    }
    return null;
  }




  
  
  /**
   * Builds an MD5-sum from an array of chars as used in password-fields.<br>
   * Note that this method converts the characters to bytes via {@link #toBytes(char[])}
   * before applying the md5 hash.
   * By this we achieve an enhanced security against md5 crackers, which assume
   * ordinary strings. In order to enhance security even more, however, you should
   * add some application-specific salt.
   *
   * @param salt the "salt", null if plain MD5.
   * @param input is the input array of chars
   * @return the md5sum as a string, null if input == null or conversion failed (error is logged)
   * @see #md5sum(char[])
   * @see #MD5SALT
   */
  public static String md5sum(char[] salt, char[] input) {
    if (input != null) {
      try {
        MessageDigest md5Helper = MessageDigest.getInstance("MD5");
        byte[] inputBytes = toBytes(input);
        if (salt != null && salt.length > 0 && input.length > 0) {
          byte[] saltyBytes = toBytes(salt);
          byte[] saltyInputBytes = new byte[saltyBytes.length + inputBytes.length];
          System.arraycopy(saltyBytes, 0, saltyInputBytes, 0, saltyBytes.length);
          System.arraycopy(inputBytes, 0, saltyInputBytes, saltyBytes.length, inputBytes.length);
          inputBytes = saltyInputBytes;
        }
        return toHexString(md5Helper.digest(inputBytes));
      }
      catch (NoSuchAlgorithmException e)  {
        UtilGlobal.logger.severe(e.toString());
      }
    }
    return null;
  }



  /**
   * Salt for {@link #md5sum(char[])}.<br>
   * The default is null, i.e. no salt.
   * Applications should set an individual salt to enhance security.
   */
  public static char[] MD5SALT = null;

  /**
   * Builds an MD5-sum from an array of chars with a default salt
   * given by {@link #MD5SALT}.
   *
   * @param input is the input array of chars
   * @return the md5sum as a string, null if input == null or conversion failed (error is logged)
   */
  public static String md5sum(char[] input) {
    return md5sum(MD5SALT, input);
  }
  

  
  /**
   * Maps null to the empty string.
   * Simple but essential ;)
   * @param str the string to test against null
   * @return str or the emptystring if str is null
   */
  public static String toString(String str) {
    return str == null ? emptyString : str;
  }
  
  
  
  /**
   * Gets the basename of a classname(-like) string.
   * The basename is the last name of a pathname with dots.
   * 
   * @param str the classname
   * @return the basename
   */
  public static String getClassBaseName(String str)  {
    int ndx = str.lastIndexOf('.');
    return (ndx >= 0 ? str.substring(ndx+1) : str);    
  }

  
  /**
   * Gets the basename of a class.
   * The basename is the class name without the package.
   * 
   * @param clazz the class
   * @return the basename
   * @see #getClassBaseName(java.lang.String) 
   */
  public static String getClassBaseName(Class<?> clazz)  {
    return getClassBaseName(clazz.getName());
  }
  

  
  /**
   * Creates a string from an object array.
   *
   * @param objArray the array of objects
   * @param separator the string between two objects
   * @return the string
   */
  public static String objectArrayToString(Object[] objArray, String separator) {
    StringBuilder buf = new StringBuilder();
    if (objArray != null) {
      boolean addSeparator = false;
      for (Object obj: objArray) {
        if (addSeparator) {
          buf.append(separator);
        }
        buf.append(obj.toString());
        addSeparator = true;
      }
    }
    return buf.toString();
  }
  
  
  
  /**
   * Transforms a string to a char-array.
   * nullp-safe.
   * 
   * @param str the string
   * @return the character array
   */
  public static char[] toCharArray(String str) {
    return str == null ? null : str.toCharArray();
  }

  
  
  /**
   * Converts a multiline string to an HTML-string that
   * can be displayed in a label.
   * Useful to print multiline labels.
   * 
   * @param text the input string
   * @return the HTML string
   */
  public static String toHTML(String text) {
    StringBuilder buf = new StringBuilder("<HTML>");
    if (text != null) {
      buf.append(text.replace("\n", "<BR>"));
    }
    buf.append("</HTML>");
    return buf.toString();
  }
  
}
