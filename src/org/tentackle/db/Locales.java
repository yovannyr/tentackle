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

// $Id: Locales.java 336 2008-05-09 14:40:20Z harald $
// Created on July 12, 2002, 4:57 PM

package org.tentackle.db;

import java.util.ResourceBundle;
import org.tentackle.ui.FormHelper;

/**
 * Localized strings for the db package.
 * 
 * @author harald
 */
public class Locales {

  /** the bundle **/
  public static ResourceBundle bundle = loadBundle();

  static {
    FormHelper.registerLocaleRunnable(new Runnable() {
      
      @Override
      public void run() {
        reload();
      }
    });
  }
  
  
  /**
   * Loads the bundle.
   * 
   * @return the loaded bundle
   */
  public static ResourceBundle loadBundle() {
    return ResourceBundle.getBundle("org/tentackle/db/Locales");
  }
  
  
  /**
   * Reloads the bundle in case default locale has changed.
   */
  public static void reload() {
    bundle = loadBundle();
  }
  
}
