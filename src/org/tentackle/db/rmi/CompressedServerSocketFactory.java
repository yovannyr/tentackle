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

// $Id: CompressedServerSocketFactory.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.db.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import org.tentackle.net.CompressedServerSocket;

/**
 * Zip-compressed server socket factory.
 *
 * @author harald
 */
public class CompressedServerSocketFactory implements RMIServerSocketFactory {
  
  private int hshCode; // the hashcode (for RMI to distinguish the socket types)
  
  
  /** Creates a new instance of CompressedServerSocketFactory */
  public CompressedServerSocketFactory() {
    hshCode = getClass().hashCode();
  }

  public ServerSocket createServerSocket(int port) throws IOException {
    return new CompressedServerSocket(port);
  }

  @Override
  public int hashCode() {
    return hshCode;
  }
  
  @Override
  public boolean equals(Object obj) {
    // must be exactly of the same class, not an extension!
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return this.getClass().equals(obj.getClass());
  }
  
}
