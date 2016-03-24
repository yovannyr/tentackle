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

// $Id: Logger.java 479 2009-08-14 07:29:22Z svn $

package org.tentackle.util;

/**
 * A pluggable logger.
 * 
 * @author harald
 */
public interface Logger {

  /**
   * Logging levels.
   * <p>
   * Provided are:
   * <ul>
   *  <li>FINEST</li>
   *  <li>FINER</li>
   *  <li>FINE</li>
   *  <li>INFO</li>
   *  <li>WARNING</li>
   *  <li>SEVERE</li>
   * </ul>
   *
   */
  public static enum Level { FINEST, FINER, FINE, INFO, WARNING, SEVERE };




  /**
   * Checks if a message of the given level would actually be logged
   * by this logger.
   *
   * @param level the logging level
   * @return true if the given logging level is currently being logged
   */
  public boolean isLoggable(Level level);



  /**
   * Checks if logger logs level FINEST.
   *
   * @return true if logger will log this level
   */
  public boolean isFinestLoggable();

  /**
   * Checks if logger logs level FINER.
   *
   * @return true if logger will log this level
   */
  public boolean isFinerLoggable();

  /**
   * Checks if logger logs level FINE.
   *
   * @return true if logger will log this level
   */
  public boolean isFineLoggable();

  /**
   * Checks if logger logs level INFO.
   *
   * @return true if logger will log this level
   */
  public boolean isInfoLoggable();

  /**
   * Checks if logger logs level WARNING.
   *
   * @return true if logger will log this level
   */
  public boolean isWarningLoggable();

  /**
   * Checks if logger logs level SEVERE.
   *
   * @return true if logger will log this level
   */
  public boolean isSevereLoggable();




  /**
   * Logs a message.
   *
   * @param level   the logging level
   * @param message the message
   * @param cause   the Throwable associated with log message, null if none
   */
  public void log(Level level, String message, Throwable cause);


  /**
   * Logs a message with level FINEST.
   *
   * @param message the message
   */
  public void finest(String message);

  /**
   * Logs a message with level FINER.
   *
   * @param message the message
   */
  public void finer(String message);

  /**
   * Logs a message with level FINE.
   *
   * @param message the message
   */
  public void fine(String message);

  /**
   * Logs a message with level INFO.
   *
   * @param message the message
   */
  public void info(String message);

  /**
   * Logs a message with level WARNING.
   *
   * @param message the message
   */
  public void warning(String message);

  /**
   * Logs a message with level SEVERE.
   *
   * @param message the message
   */
  public void severe(String message);


  /**
   * Logs a message with level FINEST.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void finest(String message, Throwable cause);

  /**
   * Logs a message with level FINER.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void finer(String message, Throwable cause);

  /**
   * Logs a message with level FINE.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void fine(String message, Throwable cause);

  /**
   * Logs a message with level INFO.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void info(String message, Throwable cause);

  /**
   * Logs a message with level WARNING.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void warning(String message, Throwable cause);

  /**
   * Logs a message with level SEVERE.
   *
   * @param message the message
   * @param cause the throwable
   */
  public void severe(String message, Throwable cause);




  /**
   * Logs the stacktrace of a throwable.
   *
   * @param level   the logging level
   * @param cause   the Throwable to log the stacktrace for
   */
  public void logStacktrace(Level level, Throwable cause);

  /**
   * Logs the stacktrace of a throwable with a logging level of SEVERE.
   *
   * @param cause   the Throwable to log the stacktrace for
   */
  public void logStacktrace(Throwable cause);


  /**
   * Gets the concrete logger implementation.<br>
   * Useful to access the logging-backend.
   *
   * @return the logger object
   */
  public Object getLoggerImpl();

}
