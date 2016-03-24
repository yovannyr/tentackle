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

// $Id: ErrorHandler.java 336 2008-05-09 14:40:20Z harald $
// Created on May 21, 2003, 5:15 PM

package org.tentackle.db;

/**
 * Interface for an error handler, i.e. some db-error that will cause
 * the application to stop and/or log info.
 * The handler provides the opportunity to stop it gracefully for
 * severe errors that cannot be handled by the application (e.g. missing
 * column).
 * 
 * @author harald
 */
public interface ErrorHandler {
  
  
  /**
   * Handles a fatal error.<br>
   * Will terminate the application usually by throwing a RuntimeException.
   * 
   * @param db is the db that caused the error (may be null)
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void severe(Db db, Exception e, String msg);
  
  /**
   * Handles a fatal error.<br>
   * Will terminate the application usually by throwing a RuntimeException.
   * 
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void severe(Exception e, String msg);
  
  
  /**
   * Handles a non-fatal error.<br>
   * Will not terminate the application but log the error.
   * 
   * @param db is the db that caused the error (may be null)
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void warning(Db db, Exception e, String msg);  
  
  /**
   * Handles a non-fatal error.<br>
   * Will not terminate the application but log the error.
   * 
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void warning(Exception e, String msg);
  
  
  
  /**
   * Handles information logging (no warning, no error, no stop).
   *
   * @param db is the db that caused the error (may be null)
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void info(Db db, Exception e, String msg);  
  
  /**
   * Handles information logging (no warning, no error, no stop).
   *
   * @param e is the exception (may be null)
   * @param msg is an error message (may be null)
   */
  public void info(Exception e, String msg);
  
}
