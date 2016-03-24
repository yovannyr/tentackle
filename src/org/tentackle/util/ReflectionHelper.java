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

// $Id: ReflectionHelper.java 476 2009-08-08 16:10:45Z harald $

package org.tentackle.util;


/**
 * Methods related to the reflection API.
 *
 * @author harald
 */
public class ReflectionHelper {

  private static final String RH_CLASSNAME = ReflectionHelper.class.getName();

  // the default package names to exclude
  private static final String[] excludedPackages = { 
    "org.tentackle.",
    "java.",
    "javax.",
    "sun."
  };
  
  
  
  /**
   * Gets the stack trace element for some invoking method.
   *
   * @param ndx the index on the stack
   * @return the StackTraceElement
   */
  public static StackTraceElement getInvocation(int ndx) {
    return new Throwable().getStackTrace()[ndx];
  }

  /**
   * Gets the stack trace element for the invoking method of the invoking method.
   * Nice to figure out, what invokes a method.
   *
   * @return the StackTraceElement
   */
  public static StackTraceElement getInvocation() {
    return getInvocation(2);
  }




  /**
   * Checks if a classname is in skipnames or is ReflectionHelper itself.
   *
   * @param className the classname to check
   * @param skipNames the skip names
   * @return true if classname matches
   */
  private static boolean isClassInNames(String className, String[] skipNames) {
    boolean found = false;
    if (className.equals(RH_CLASSNAME)) {
      found = true;
    }
    else  {
      for (String name: skipNames) {
        if (name != null) {
          if (name.endsWith(".")) {
            found = className.startsWith(name);
          }
          else  {
            found = className.equals(name);
          }
          if (found) {
            break;
          }
        }
      }
    }
    return found;
  }

  
  /**
   * Gets the first invocation that does not start with any of the given
   * package- or classnames.
   *
   * @param skipNames list of names to exclude.
   *        Names ending with a dot are considered to be package names,
   *        else classnames.
   * @return the trace element, null if no trace element outside given names
   */
  public static StackTraceElement getInvocation(String[] skipNames) {

    if (skipNames == null) {
      throw new NullPointerException("skipNames must not be null");
    }

    StackTraceElement[] stackTrace = new Throwable().getStackTrace();

    int i = 0;
    boolean found = false;
    StackTraceElement e = null;

    // find first match in set
    while (i < stackTrace.length && !found) {
      e = stackTrace[i];
      found = isClassInNames(e.getClassName(), skipNames);
      ++i;
    }

    // skip matches in set (if any found so far)
    while (i < stackTrace.length && found) {
      e = stackTrace[i];
      found = isClassInNames(e.getClassName(), skipNames);
      ++i;
    }

    return e;
  }





  
  /**
   * Gets the first invocation that does not belong to some tentackle classes.<br>
   * This is nice to find the root cause in applications.
   *
   * @return the trace element, null if not within application
   */
  public static StackTraceElement getApplicationInvocation() {
    return getInvocation(excludedPackages);
  }
  
  /**
   * Gets the first invocation that does not belong to some tentackle classes.<br>
   * This is nice to find the root cause in applications.
   * @return the string representing the stacktrace element, null if not within application
   */
  public static String getApplicationInvocationAsString() {
    StackTraceElement e = getApplicationInvocation();
    return e == null ? "(stacktrace not within application code)" : e.toString();
  }
  
  
  
  
  /**
   * create a source-usable string from clazz.getName().
   * e.g. "[[[B" will be transformed to "byte[][][]"
   * 
   * @param classname the classname
   * @return the converted string
   */
  public static String makeDeclareString(String classname) {
    if (classname.startsWith("["))  {
      // array
      String arr = "";
      String type = null;
      int len = classname.length();
      int i = 0;
      while (i < len) {
        if (classname.charAt(i) == '[') {
          arr += "[]";
          ++i;
        }
        else  {
          break;
        }
      }
      type = classname.substring(i);
      if (type.endsWith(";")) {
        // object
        type = type.substring(0, len - 1);
      }
      else if ("Z".equals(type)) {
        type = "boolean";
      }
      else if ("B".equals(type))  {
        type = "byte";
      }
      else if ("C".equals(type))  {
        type = "char";
      }
      else if ("D".equals(type))  {
        type = "double";
      }
      else if ("F".equals(type))  {
        type = "float";
      }
      else if ("I".equals(type))  {
        type = "int";
      }
      else if ("J".equals(type))  {
        type = "long";
      }
      else if ("S".equals(type))  {
        type = "short";
      }
      
      classname = type + arr;       
    }
    
    return classname;
  }

  
}
