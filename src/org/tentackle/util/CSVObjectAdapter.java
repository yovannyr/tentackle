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

// $Id: CSVObjectAdapter.java 336 2008-05-09 14:40:20Z harald $
// Created on December 8, 2004, 3:45 PM

package org.tentackle.util;

/**
 * An adapter for a CSVObject.
 * 
 * @author harald
 */
public class CSVObjectAdapter implements CSVObject {
  
  public String formatCSVField(int index) throws ApplicationException {
    return null;
  }  
  
  public int getCSVFieldCount() throws ApplicationException {
    return 0;
  }
  
  public void parseCSVField(int index, String value) throws ApplicationException {
  }
  
}
