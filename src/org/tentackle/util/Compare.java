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

// $Id: Compare.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

/**
 * Some handy static methods to compare two Comparables even if
 * one or both are null-pointers.
 *
 * @author harald
 */
public class Compare {

  /**
   * Compares two objects.<br>
   * {@code null} is being treated as smallest value.
   * @param <T> the class type
   * @param o1 the first object
   * @param o2 the second object
   * @return a negative integer, zero, or a positive integer as o1
   *         is less than, equal to, or greater than o2.
   */
  public static <T extends Comparable<? super T>> int compare (T o1, T o2)  {
    if (o1 == null) {
      return o2 == null ? 0 : -1;
    }
    // o1 != null
    if (o2 == null) {
      return 1;
    }
    return o1.compareTo(o2);
  }
  
  /**
   * Compares two objects.<br>
   * {@code null} is being treated as largest value.
   * @param <T> the class type
   * @param o1 the first object
   * @param o2 the second object
   * @return a negative integer, zero, or a positive integer as o1
   *         is less than, equal to, or greater than o2.
   */
  public static <T extends Comparable<? super T>> int compareWithNullLargest (T o1, T o2)  {
    if (o1 == null) {
      return o2 == null ? 0 : 1;
    }
    // o1 != null
    if (o2 == null) {
      return -1;
    }
    return o1.compareTo(o2);
  }

  
  /**
   * Checks whether two objects are equal.
   * 
   * @param o1 the first object
   * @param o2 the second object
   * @return true if objects are equal.
   */
  public static boolean equals(Object o1, Object o2)  {
    if (o1 == null) {
      return o2 == null ? true : false;
    }
    // o1 != null
    if (o2 == null) {
      return false;
    }
    return o1.equals(o2);    
  }
  
  
  
  /**
   * Compares two long integers.<br>
   * 
   * Notice when checking object-ids: make sure that values are provided
   * from getId() because deleted objects change their id (negate), 
   * but don't change their identity!
   * @param val1 the first value
   * @param val2 the second value
   * @return a -1, 0, or +1 integer as val1
   *         is less than, equal to, or greater than val2.
   */
  public static int compareLong(long val1, long val2) {
    return val1 < val2 ? -1 : (val1 == val2 ? 0 : 1);
  }
  
  
}