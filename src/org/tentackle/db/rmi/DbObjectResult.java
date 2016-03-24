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

// $Id: DbObjectResult.java 439 2008-09-18 15:09:32Z harald $
// Created on November 19, 2003, 5:46 PM

package org.tentackle.db.rmi;

import java.io.Serializable;

/**
 * Result used to return the possibly new objectid, changed serial and a 
 * result code for success or failure.
 * 
 * @author harald
 */
public class DbObjectResult implements Serializable {
  
  private static final long serialVersionUID = 3143529012167549556L;
  
  /** the object ID **/
  public long id;
  /** the serial number **/
  public long serial;
  /** the table serial **/
  public long tableSerial;
  /** the result code: true or false **/
  public boolean result;
  /** if failed: was it a unique violation? **/
  public boolean uniqueViolation;
  
  
  /**
   * Creates a result for the client.
   * 
   * @param id the object ID
   * @param serial the object's serial
   * @param tableSerial the table serial
   * @param result true=ok or false=failed
   * @param uniqueViolation true if failed due to a unique violation
   */
  public DbObjectResult(long id, long serial, long tableSerial, boolean result, boolean uniqueViolation) {
    this.id = id;
    this.serial = serial;
    this.tableSerial = tableSerial;
    this.result = result;
    this.uniqueViolation = uniqueViolation;
  }
  
}
