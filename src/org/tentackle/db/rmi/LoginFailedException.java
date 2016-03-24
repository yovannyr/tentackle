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

// $Id: LoginFailedException.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.db.rmi;

import java.rmi.RemoteException;

/**
 * RMI servers should throw a LoginFailedException if authentication fails (or
 * any other reason)
 * 
 * @author harald
 */
public class LoginFailedException extends RemoteException {
  
  private static final long serialVersionUID = -6175651458048862623L;
  
  /**
   * Creates a <code>LoginFailedException</code>.
   */
  public LoginFailedException() {
    super();
  }

    /**
     * Constructs a <code>LoginFailedException</code> with the specified
     * detail message.
     *
     * @param s the detail message
     */
  public LoginFailedException(String s) {
    super(s);
  }

    /**
     * Constructs a <code>LoginFailedException</code> with the specified detail
     * message and cause.  This constructor sets the {@link #detail}
     * field to the specified <code>Throwable</code>.
     *
     * @param s the detail message
     * @param cause the cause
     */
  public LoginFailedException(String s, Throwable cause) {
    super(s, cause);
  }
}
