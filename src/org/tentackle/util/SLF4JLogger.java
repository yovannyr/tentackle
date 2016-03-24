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

// $Id: SLF4JLogger.java 479 2009-08-14 07:29:22Z svn $

package org.tentackle.util;

import java.io.PrintStream;
import java.util.HashMap;

import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.slf4j.Marker;

/**
 * Pluggable logger using <tt>org.slf4j</tt>.
 * <p>
 * To use SLF4J logging, simply set {@link LoggerFactory#defaultLoggerClassname} <tt>= "org.tentackle.util.SLF4JLogger"</tt>
 * and make sure that the required SLF4J jars are in your classpath.
 * 
 * @author harald
 */
public class SLF4JLogger implements Logger {

  // classname to exclude from stacktrace
  private static final String EXCLUDE_CLASSNAME = SLF4JLogger.class.getName();

  // cached loggers
  private static final HashMap<String,SLF4JLogger> loggers = new HashMap<String,SLF4JLogger>();
  
  /**
   * Gets the Log4JLogger for given name.
   * If a logger with that name already exists, it will be re-used.
   * 
   * @param name the name of the logger
   * @return the logger
   */
  public static SLF4JLogger getLogger(String name) {
    synchronized(loggers) {
      SLF4JLogger logger = loggers.get(name);
      if (logger == null) {
        // create a new one
        logger = new SLF4JLogger(name);
        loggers.put(name, logger);
      }
      return logger;
    }
  }
  
  
  private org.slf4j.Logger logger;    // the SLF4J logger
  private boolean locationAware;      // true if instanceof LocationAwareLogger
  
  
  /**
   * Creates a logger.
   * 
   * @param name the name of the logger
   */
  public SLF4JLogger(String name) {
    logger = org.slf4j.LoggerFactory.getLogger(name);
    locationAware = logger instanceof org.slf4j.spi.LocationAwareLogger;
  }


  public Object getLoggerImpl() {
    return logger;
  }


  private int translateLevel(Level level) {
    switch (level) {
      case FINEST:  return org.slf4j.spi.LocationAwareLogger.TRACE_INT;
      case FINER:   return org.slf4j.spi.LocationAwareLogger.TRACE_INT;
      case FINE:    return org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
      case INFO:    return org.slf4j.spi.LocationAwareLogger.INFO_INT;
      case WARNING: return org.slf4j.spi.LocationAwareLogger.WARN_INT;
      default:      return org.slf4j.spi.LocationAwareLogger.ERROR_INT;
    }
  }

  @Override
  public boolean isLoggable(Level level) {
    switch (level) {
      case FINEST:  return isFinestLoggable();
      case FINER:   return isFinerLoggable();
      case FINE:    return isFineLoggable();
      case INFO:    return isInfoLoggable();
      case WARNING: return isWarningLoggable();
      default:      return isSevereLoggable();
    }
  }



  private void doLog(Level level, String message, Throwable cause) {
    /*if (locationAware) {
      ((org.slf4j.spi.LocationAwareLogger) logger) .
              log(null, EXCLUDE_CLASSNAME, translateLevel(level), message, cause);
    }
    else  {*/
      // probably the SimpleLogger...
      switch (level) {
        case FINEST:
        case FINER:
          logger.trace(message, cause);
          break;
        case FINE:
          logger.debug(message, cause);
          break;
        case INFO:
          logger.info(message, cause);
          break;
        case WARNING:
          logger.warn(message, cause);
          break;
        default:
          logger.error(message, cause);
          break;
      }
    //}
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
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isFinerLoggable() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isFineLoggable() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoLoggable() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isWarningLoggable() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isSevereLoggable() {
    return logger.isErrorEnabled();
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
