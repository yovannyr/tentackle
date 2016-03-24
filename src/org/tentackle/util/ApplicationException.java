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

// $Id: ApplicationException.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Application Exception.<br>
 * 
 * Same as Exception but will be shown to the user in FormError
 * and can be used as a tagged exception for all internal application
 * errors.
 *
 * @author  harald
 */
public class ApplicationException extends Exception {
  
  /** the default error code **/
  public static final int ERROR_DEFAULT = 1;
  
  /**
   * Returns the stacktrace of a given exception as a string.
   * 
   * @param cause the exception
   * @return the printable stacktrace
   */
  public static String getStackTraceAsString(Throwable cause)  {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bs);
    cause.printStackTrace(ps);
    ps.flush();
    String str = bs.toString();
    ps.close();  
    return str;
  }
  
  
  private int errorCode;    // the errorcode of this exception
  
  
  /**
   * Creates an ApplicationException.
   * 
   * @param message the message, null if none
   * @param errorCode the errorcode
   * @param cause the chained cause, null if none
   */  
  public ApplicationException(String message, int errorCode, Throwable cause)  {
    super(message, cause);
    this.errorCode = errorCode;
  }
  
  /**
   * Creates an ApplicationException.
   * 
   * @param message the message, null if none
   * @param errorCode the errorcode
   */  
  public ApplicationException(String message, int errorCode) {
    super(message);
    setErrorCode(errorCode);
  }
  
  /**
   * Creates an ApplicationException.
   * 
   * @param message the message, null if none
   */  
  public ApplicationException(String message) {
    this(message, ERROR_DEFAULT); 
  }
  
  /**
   * Creates an ApplicationException.
   * 
   * @param message the message, null if none
   * @param cause the chained cause, null if none
   */  
  public ApplicationException(String message, Throwable cause) {
    this(message, ERROR_DEFAULT, cause); 
  }
  
  
  /** 
   * Gets the errorcode.
   * @return the errorcode
   */
  public int getErrorCode() {
    return errorCode;
  }  
  
  /** 
   * Sets the errorcode.
   * @param errorCode  the errorcode
   */
  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }
  
  
  /**
   * Returns the stacktrace of this exception as a string.
   * 
   * @return the printable stacktrace
   */
  public String getStackTraceAsString()  {
    return getStackTraceAsString(this);
  }
  
  
  /**
   * Gets the chain of all messages (if exception is cascaded).<br>
   * 
   * Any Exception other than ApplicationException will be stacktraced.
   * 
   * @return the message string
   */
  public String getAllMessages()  {
    String message = getMessage();
    if (message == null) {
      message = StringHelper.emptyString;
    }
    String closing = StringHelper.emptyString;
    Throwable cause = getCause();
    while (cause != null)  {
      message += " [" + cause.getMessage();
      if (!(cause instanceof ApplicationException)) {
        // some severe programming error
        message += "\n" + getStackTraceAsString();
      }
      closing += "]";
      cause = cause.getCause();
    }
    return message + closing;
  }
  
}
