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

// $Id: CompressedSslServerSocketFactory.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.db.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import org.tentackle.net.CompressedSocketWrapper;

/**
 * Zip-compressed server socket factory.
 *
 * @author harald
 */
public class CompressedSslServerSocketFactory extends SslRMIServerSocketFactory {
  
  private int hshCode; // the hashcode (for RMI to distinguish the socket types)
  
  
  /** 
   * Creates a new instance of CompressedServerSocketFactory.
   * 
   * @param enabledCipherSuites the enabled cipher suites, null for default
   * @param enabledProtocols the enabled protocols, null for default
   * @param needClientAuth true if server request SSL-client-authentication
   */
  public CompressedSslServerSocketFactory(String[] enabledCipherSuites,
                                          String[] enabledProtocols,
                                          boolean needClientAuth) {
    
    super(enabledCipherSuites, enabledProtocols, needClientAuth);
    hshCode = getClass().hashCode();
  }
  
  
  /**
   * Creates a CompressedServerSocketFactory with default cipher suites,
   * protocols and without client authentication.
   */
  public CompressedSslServerSocketFactory() {
    this(null, null, false);
  }
  

  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    
    final SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    
    return new ServerSocket(port) {
      
      @Override
      public Socket accept() throws IOException {
        Socket socket = super.accept();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
        sslSocket.setUseClientMode(false);
        if (getEnabledCipherSuites() != null) {
          sslSocket.setEnabledCipherSuites(getEnabledCipherSuites());
        }
        if (getEnabledProtocols() != null) {
          sslSocket.setEnabledProtocols(getEnabledProtocols());
        }
        sslSocket.setNeedClientAuth(getNeedClientAuth());
        // wrap the ssl-socket by a compressed socket!
        return new CompressedSocketWrapper(sslSocket);
      }
    };
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
