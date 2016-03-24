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

// $Id: LoggerFactory.java 479 2009-08-14 07:29:22Z svn $

package org.tentackle.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Factory for pluggable loggers.
 *
 * @author harald
 */
public class LoggerFactory {

  /** the logger classname used in {@link #getLogger} */
  public static String defaultLoggerClassname = DefaultLogger.class.getName();


  // cached static methods
  private static final HashMap<Class<Logger>,Method> methods = new HashMap<Class<Logger>,Method>();

  // cached constructors
  private static final HashMap<Class<Logger>,Constructor<Logger>> constructors = new HashMap<Class<Logger>,Constructor<Logger>>();

  
  /**
   * Gets a logger for the given name.
   * <p>
   * The implementation first tries to invoke the static method <tt>"clazz.getLogger(name)"</tt>.
   * If there is no such method, <tt>"new clazz(name)"</tt> will be invoked.
   * If all else failes, it returns a new {@link DefaultLogger} for the given <tt>name</tt>.
   * <p>
   * If <tt>clazz</tt> is <tt>null</tt> the class given by {@link #defaultLogger} will be used.
   *
   * @param name the logger name
   * @param loggerClass the class implementing {@link Logger},
   *        null if use class specified in {@link #defaultLogger}.
   * @return the logger
   */
  @SuppressWarnings("unchecked")
  public static Logger getLogger(String name, Class<? extends Logger> loggerClass) {

    if (loggerClass == null) {
      try {
        loggerClass = (Class<? extends Logger>) Class.forName(defaultLoggerClassname);
      }
      catch (ClassNotFoundException e) {
        Logger logger = DefaultLogger.getLogger(name);
        logger.warning("could not load logger '" + name + "': " + e);
        return logger;
      }
    }

    Constructor<Logger> constructor = null;
    Method method = methods.get(loggerClass);
    if (method == null) {
      constructor = constructors.get(loggerClass);
    }

    Exception methodException = null;
    Exception constructorException = null;

    // check if cached
    if (method == null && constructor == null) {
      // find method
      try {
        method = loggerClass.getDeclaredMethod("getLogger", String.class);
        if (Modifier.isStatic(method.getModifiers())) {
          methods.put((Class<Logger>)loggerClass, method);
        }
      }
      catch (NoSuchMethodException e) {
        methodException = e;
      }

      if (method == null) {
        // find constructor
        try {
          constructor = (Constructor<Logger>) loggerClass.getConstructor(String.class);
          constructors.put((Class<Logger>)loggerClass, constructor);
        }
        catch (NoSuchMethodException e) {
          constructorException = e;
        }
        catch (SecurityException e) {
          constructorException = e;
        }
      }
    }


    if (method != null) {
      try {
        return (Logger)method.invoke(null, name);
      }
      catch (Exception e) {
        methodException = e;
      }
    }
    else if (constructor != null) {
      try {
        return constructor.newInstance(name);
      }
      catch (Exception e) {
        constructorException = e;
      }
    }

    // could not load logger
    String msg = "Could not load logger " + loggerClass;
    if (methodException != null) {
      msg += " (via static method: " + methodException + ")";
    }
    if (constructorException != null) {
      msg += " (via constructor: " + constructorException;
    }

    // return the default logger
    Logger logger = DefaultLogger.getLogger(name);
    logger.warning(msg);
    return logger;
  }


  
  /**
   * Gets a logger for the given name.
   * <p>
   * The method loads the class given by {@link #LOGGER_CLASSNAME} and looks
   * for a static method <tt>getLogger(String)</tt>. If no such method is
   * found, it will instantiate the class using a constructor with the name
   * as its single argument. If all else failes, it returns a new {@link DefaultLogger}
   * for that name.
   *
   * @param name the logger name
   * @return the logger
   */
  public static Logger getLogger(String name) {
    return getLogger(name, null);
  }


  /**
   * Gets a logger for the given classname.
   * <p>
   * The method invokes {@link #getLogger(java.lang.String)} with the
   * name of the given class.
   *
   * @param clazz the clazz to create a logger for
   * @return the logger
   */
  public static Logger getLogger(Class clazz) {
    return getLogger(clazz.getName(), null);
  }

}
