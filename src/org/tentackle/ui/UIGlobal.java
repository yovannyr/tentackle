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

// $Id: UIGlobal.java 473 2009-08-07 17:19:36Z harald $
// Created on May 8, 2003, 11:46 AM

package org.tentackle.ui;

import java.awt.GraphicsEnvironment;
import org.tentackle.util.Logger;
import org.tentackle.util.LoggerFactory;


/**
 * UI globals.
 * 
 * @author  harald
 */
public class UIGlobal {
  
  /** Default logger for the ui-package */
  public static Logger logger = LoggerFactory.getLogger("org.tentackle.ui");
  
  
  /** true if headless, i.e. no GUI */
  public static boolean isHeadless = GraphicsEnvironment.isHeadless();
  
}
