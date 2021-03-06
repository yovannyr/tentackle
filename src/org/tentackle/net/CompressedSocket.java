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

// $Id: CompressedSocket.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.tentackle.util.CompressedInputStream;
import org.tentackle.util.CompressedOutputStream;


/**
 * Socket with compressing streams.
 * Based on tentackles CompressedInput/OutputStream.
 *
 * @author harald
 */
public class CompressedSocket extends Socket {

  private CompressedOutputStream out;
  private CompressedInputStream in;
  

  /**
   * Creates a compressed stream socket and connects it to the specified port
   * number on the named host.
   * 
   * @param host the host name, or <code>null</code> for the loopback address.
   * @param port the port number.
   * @exception  IOException  if an I/O error occurs when creating the socket.
   * @see Socket#Socket(java.lang.String, int) 
   */
  public CompressedSocket(String host, int port) throws IOException {
    super(host, port);
  }


  /**
   * Creates an unconnected socket, with the
   * system-default type of SocketImpl.
   *
   * @see Socket#Socket() 
   */
  public CompressedSocket() { 
    super();
  }

  
  
  @Override
  public InputStream getInputStream() throws IOException {
    if (in == null) {
      in = new CompressedInputStream(super.getInputStream());
    }
    return in;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    if (out == null) {
      // buffer should be as large as possible!
      out = new CompressedOutputStream(super.getOutputStream());
    }
    return out;
  }

  @Override
  public void close() throws IOException {
    if (out != null) {
      out.close();
      out = null;
    } 
    if (in != null) {
      in.close();
      in = null;
    }
    /**
     * notice that super.close() may invoke out.close() and in.close() again
     * depending on the implementation of SocketImpl in Socket.
     * However, the CompressedIn/OutputStream can deal with that.
     */
    super.close();
  }
  
}
