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

// $Id: DefaultLogger.java 479 2009-08-14 07:29:22Z svn $

package org.tentackle.util;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * Pluggable logger using the standard java logging using <tt>java.util.logging</tt>.
 * 
 * @author harald
 */
public class DefaultLogger implements Logger {

  // classnames to exclude from stacktrace
  private static final String[] EXCLUDE_CLASSNAMES = new String [] {
    DefaultLogger.class.getName()
  };
  

  // cached loggers
  private static final HashMap<String,DefaultLogger> loggers = new HashMap<String,DefaultLogger>();
  
  /**
   * Gets the DefaultLogger for given name.
   * If a logger with that name already exists, it will be re-used.
   * 
   * @param name the name of the logger
   * @return the logger
   */
  public static DefaultLogger getLogger(String name) {
    synchronized(loggers) {
      DefaultLogger logger = loggers.get(name);
      if (logger == null) {
        // create a new one
        logger = new DefaultLogger(name);
        loggers.put(name, logger);
      }
      return logger;
    }
  }
  
  
  private java.util.logging.Logger logger;    // Java SE standard logger
  
  
  /**
   * Creates a logger.
   * 
   * @param name the name of the logger
   */
  public DefaultLogger(String name) {
    logger = java.util.logging.Logger.getLogger(name);
  }



  public Object getLoggerImpl() {
    return logger;
  }
  
  
  private java.util.logging.Level translateLevel(Level level) {
    switch (level) {
      case FINEST:  return java.util.logging.Level.FINEST;
      case FINER:   return java.util.logging.Level.FINER;
      case FINE:    return java.util.logging.Level.FINE;
      case INFO:    return java.util.logging.Level.INFO;
      case WARNING: return java.util.logging.Level.WARNING;
      default:      return java.util.logging.Level.SEVERE;
    }
  }

  @Override
  public boolean isLoggable(Level level) {
    return logger.isLoggable(translateLevel(level));
  }

  
  private void doLog(Level level, String message, Throwable cause) {
    StackTraceElement se = ReflectionHelper.getInvocation(EXCLUDE_CLASSNAMES);
    if (cause == null) {
      logger.logp(translateLevel(level), se.getClassName(), se.getMethodName(), message);
    }
    else {
      logger.logp(translateLevel(level), se.getClassName(), se.getMethodName(), message, cause);
    }
  }

  @Override
  public void log(Level level, String message, Throwable cause) {
    doLog(level, message, cause);
  }


  @Override
  public void finest(String message, Throwable cause) {
    doLog(Level.FINEST, message, cause);
  }

  @Override
  public void finer(String message, Throwable cause) {
    doLog(Level.FINER, message, cause);
  }

  @Override
  public void fine(String message, Throwable cause) {
    doLog(Level.FINE, message, cause);
  }

  @Override
  public void info(String message, Throwable cause) {
    doLog(Level.INFO, message, cause);
  }

  @Override
  public void warning(String message, Throwable cause) {
    doLog(Level.WARNING, message, cause);
  }

  @Override
  public void severe(String message, Throwable cause) {
    doLog(Level.SEVERE, message, cause);
  }


  @Override
  public void finest(String message) {
    finest(message, null);
  }

  @Override
  public void finer(String message) {
    finer(message, null);
  }

  @Override
  public void fine(String message) {
    fine(message, null);
  }

  @Override
  public void info(String message) {
    info(message, null);
  }

  @Override
  public void warning(String message) {
    warning(message, null);
  }

  @Override
  public void severe(String message) {
    severe(message, null);
  }


  @Override
  public boolean isFinestLoggable() {
    return isLoggable(Level.FINEST);
  }

  @Override
  public boolean isFinerLoggable() {
    return isLoggable(Level.FINER);
  }

  @Override
  public boolean isFineLoggable() {
    return isLoggable(Level.FINE);
  }

  @Override
  public boolean isInfoLoggable() {
    return isLoggable(Level.INFO);
  }

  @Override
  public boolean isWarningLoggable() {
    return isLoggable(Level.WARNING);
  }

  @Override
  public boolean isSevereLoggable() {
    return isLoggable(Level.SEVERE);
  }


  /**
   * Logs the stacktrace of a throwable.
   *
   * @param level   the logging level
   * @param cause   the Throwable to log the stacktrace for
   */
  public void logStacktrace(Level level, Throwable cause) {
    PrintStream ps = new PrintStream(new LoggerOutputStream(this, level));
    cause.printStackTrace(ps);
    ps.close();
  }

  /**
   * Logs the stacktrace of a throwable with a logging level of SEVERE.
   *
   * @param cause   the Throwable to log the stacktrace for
   */
  public void logStacktrace(Throwable cause) {
    logStacktrace(Level.SEVERE, cause);
  }

}
