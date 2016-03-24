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

// $Id: LoggerOutputStream.java 453 2009-03-14 15:38:44Z harald $
// Created on September 16, 2003, 2:43 PM

package org.tentackle.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.tentackle.util.Logger.Level;

/**
 * Output stream for a logger.<br>
 * Nice to catch e.printStackTrace(Stream)
 *
 * @author harald
 */
public class LoggerOutputStream extends ByteArrayOutputStream {

  private Logger logger;    // the logger
  private Level  level;     // one of LogLevel
  
  
  /**
   * Creates a logger output stream.
   * 
   * @param logger the logger
   * @param level one of {@link org.tentackle.util.Level#SEVERE}, etc...
   * @param size the buffersize
   */
  public LoggerOutputStream(Logger logger, Level level, int size) {
    super(size);
    this.logger = logger;
    this.level  = level;
  }
  
  /**
   * Creates a logger output stream with a default buffersize.
   * 
   * @param logger the logger
   * @param level one of {@link org.tentackle.util.Level#SEVERE}, etc...
   */
  public LoggerOutputStream(Logger logger, Level level) {
    this(logger, level, 512);
  }
  
  /**
   * Creates a logger output stream with a default buffersize
   * and {@link org.tentackle.util.Level#SEVERE}.
   * 
   * @param logger the logger
   */
  public LoggerOutputStream(Logger logger)  {
    this(logger, Level.SEVERE); 
  }
  
  @Override
  public void flush() {
    logger.log(level, toString(), null);    // print Bytearray output stream
    reset();
  }
  
  @Override
  public void close() {
    flush();
  }
  
  
  /**
   * Logs an exception stacktrace to the logger.
   * 
   * @param e the exception
   * @param logger the logger
   * @param level the logging level
   * @param size the buffersize
   */
  public static void logException(Exception e, Logger logger, Level level, int size)  {
    // print exception and stacktrace
    PrintStream ps = new PrintStream(new LoggerOutputStream(logger, level, size));
    ps.println(e);
    e.printStackTrace(ps);
    ps.close(); 
  }
  
  /**
   * Logs an exception stacktrace to the logger with a default buffersize
   * and {@link org.tentackle.util.Level#SEVERE}.
   * 
   * @param e the exception
   * @param logger the logger
   */
  public static void logException(Exception e, Logger logger)  {
    logException(e, logger, Level.SEVERE, 512);
  }

  /**
   * Logs an exception stacktrace to the logger with a default buffersize
   * and {@link org.tentackle.util.Level#SEVERE} to
   * the default logger of the util package.
   * 
   * @param e the exception
   */
  public static void logException(Exception e)  {
    logException(e, UtilGlobal.logger, Level.SEVERE, 512);
  }
  
}
