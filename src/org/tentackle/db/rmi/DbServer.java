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

// $Id: DbServer.java 471 2009-08-02 15:55:04Z harald $

package org.tentackle.db.rmi;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.UserInfo;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.StringHelper;
import static org.tentackle.db.UserInfo.*;



/**
 * A generic db-RMI-DbServer.<br>
 * 
 * The db properties file is parsed for the following keywords:
 * <ul?
 * <li>
 * <tt>service=service-name</tt>: 
 *  defaults to the basename of the DbServer-class instance, i.e. -> rmi://localhost:1099/<Basename>
 * </li>
 * 
 * <li>
 * <tt>createregistry</tt>: 
 *  creates a local registry (on the port according to service, defaulting to 1099)
 * </li>
 * 
 * <li>
 * <tt>connectionclass=connection-class</tt>:
 *  defaults to org.tentackle.db.rmi.RemoteConnectionImpl (which is only for testing. Real apps need another class)
 * </li>
 *
 *
 * <li>
 * <tt>timeoutinterval=timeout-polling-interval-in-milliseconds</tt>:
 *  The polling interval for dead sessions in milliseconds. Defaults to 1000ms.
 *  0 turns off the cleanup thread completely (risky!).
 * </li>
 *
 * <li>
 * <tt>timeout=session-timeout</tt>:
 *  The default session timeout (in polling-intervals) for dead client connections (see Db -> keepAlive).
 *  Defaults to 0, i.e. no timeout (sessions may request an individual timeout).
 * </li>
 *
 * <li>
 * <tt>port=port</tt>: for the connection object>
 *  default is 0, i.e. system default (or from fixed ports)
 * </li>
 * 
 * <li>
 * Fixed ports:<br>
 * <tt>ports=28000</tt>: plain=28000, compressed=28001, ssl=28002, compressed+ssl=28003<br>
 *  is the same as:<br>
 * <tt>ports=28000,28001,28002,28003</tt><br>
 * Default is: <tt>ports=serviceport+0,serviceport+1,serviceport+2,serviceport+3</tt> if the service port is not
 * the default registry port, else <tt>ports=0,0,0,0</tt>.<br>
 * Use -1 to disable service at this port and 0 to use a system default port, i.e.
 * "ports=-1,-1,28002,28003" means: ssl only, with or without compression.
 * </li>
 * 
 * <li>
 * <tt>compressed</tt>: connection is compressed (initially)
 * </li>
 * 
 * <li>
 * <tt>ssl</tt>: connection is with SSL (initially)
 * </li>
 * 
 * <li>
 * <tt>keystore=keystore</tt>: default is null if not provided by command line
 * </li>
 * 
 * <li>
 * <tt>truststore=truststore</tt>: for client auth, default is null if not provided by command line
 * </li>
 * 
 * <li>
 * <tt>keystorepassword=keystore-password</tt>: no default if not provided by command line
 * </li>
 * 
 * <li>
 * <tt>truststorepassword=truststore-password</tt>: no default if not provided by command line
 * Notice that servers need either both (key- and truststore) set or none.
 * Otherwise weird error messages will happen when closing a connection.
 * </li>
 * 
 * <li>
 * <tt>ciphersuites=...</tt>: comma separated list of enabled cipher suites
 * </li>
 * 
 * <li>
 * <tt>protocols=...</tt>: comma separated list of enabled protocols
 * </li>
 * 
 * <li>
 * <tt>clientauth</tt>: set if server requires client authentication
 * </li>
 * 
 * 
 * </ul>
 * 
 * @author harald
 */
public class DbServer {
  
  private UserInfo serverInfo;                            // server db userinfo, null = create from clientInfo
  private String service;                                 // name of the RMI service
  private boolean createRegistry;                         // true to create a local registry
  private Class<? extends RemoteDbConnectionImpl> connectionClass;  // class for connection object
  private RemoteDbConnectionImpl connectionObject;        // the connection object (and to keep the object referenced!)
  private int sessionTimeout;                             // default session timeout in seconds
  private long sessionTimeoutInterval;                    // polling interval for session timeout in milliseconds, 0 = no polling at all
  private int port;                                       // port of connection object
  private RMIClientSocketFactory csf;                     // client socket factory for connection object
  private RMIServerSocketFactory ssf;                     // server socket factory for connection object
  private Registry registry;                              // local registry
  
  // ssl config
  private String[] enabledCipherSuites;
  private String[] enabledProtocols;
  private boolean needClientAuth;
  
  
  // fixed ports. 0 = no limitation
  private int plainPort;          // port for plain sockets, i.e. no ssl, no compression
  private int compressedPort;     // port for compressed sockets
  private int sslPort;            // port for ssl sockets
  private int compressedSslPort;  // port for compressed ssl sockets
  
  
  
  /**
   * Creates an instance of an RMI-db-server.
   *
   * @param serverInfo the servers db-connection user info
   * @param connectionClass the class of the connection object to instantiate, null = default or from serverInfo's properties file
   *
   * @throws ApplicationException if some configuration error
   */
  @SuppressWarnings("unchecked")
  public DbServer(UserInfo serverInfo, Class<? extends RemoteDbConnectionImpl> connectionClass) throws ApplicationException {
    
    this.serverInfo = serverInfo;
    this.connectionClass = connectionClass == null ? RemoteDbConnectionImpl.class : connectionClass;
    
    Properties props = serverInfo.getDbProperties();
    
    // check connection class
    String val = props.getProperty("connectionclass");
    if (val != null) {
      try {
        connectionClass = (Class<RemoteDbConnectionImpl>)Class.forName(val);
      } 
      catch (ClassNotFoundException ex) {
        throw new ApplicationException("connection class '" + val + "' not found");
      }
    }

    val = props.getProperty("service");
    if (val != null) {
      service = val;
    }
    else  {
      service = "rmi://localhost:" + Registry.REGISTRY_PORT + "/" +
                StringHelper.getClassBaseName(this.getClass());
    }

    // set the default ports, if not the REGISTRY_PORT.
    try {
      URI uri = new URI(service);
      int servicePort = uri.getPort();
      if (servicePort != Registry.REGISTRY_PORT)  {
        plainPort         = servicePort;
        compressedPort    = servicePort + 1;
        sslPort           = servicePort + 2;
        compressedSslPort = servicePort + 3;
      }
    }
    catch (URISyntaxException ex) {
      throw new ApplicationException("malformed service url", ex);
    }

    val = props.getProperty("createregistry");
    if (val != null) {
      createRegistry = val.isEmpty() || Boolean.valueOf(val);
    }

    val = props.getProperty("timeout");
    if (val != null) {
      sessionTimeout = Integer.valueOf(val);
    }

    val = props.getProperty("timeoutinterval");
    if (val != null) {
      sessionTimeoutInterval = Long.valueOf(val);
    }
    else  {
      sessionTimeoutInterval = 1000;
    }

    // check for default ports
    val = props.getProperty("ports");
    if (val != null) {
      StringTokenizer stok = new StringTokenizer(val, " \t,;");
      int pos = 0;
      while (stok.hasMoreTokens()) {
        int p = Integer.valueOf(stok.nextToken());
        switch (pos) {
          case 0: plainPort         = p; break;
          case 1: compressedPort    = p; break;
          case 2: sslPort           = p; break;
          case 3: compressedSslPort = p; break;
          default: throw new ApplicationException("malformed 'ports = " + val + "'");
        }
        pos++;
      }
      if (pos == 0) {
        throw new ApplicationException("missing port numbers in 'ports = " + val + "'");
      }
      else if (pos == 1) {
        // short form
        compressedPort    = plainPort + 1;
        sslPort           = plainPort + 2;
        compressedSslPort = plainPort + 3;
      }
      else if (pos < 4) {
        throw new ApplicationException("either one or all four ports must be given in 'ports = " + val + "'");
      }
      // check port range
      checkPort(plainPort);
      checkPort(compressedPort);
      checkPort(sslPort);
      checkPort(compressedSslPort);
    }

    serverInfo.configureSsl();    // configure SSL system properties

    // more server side ssl properties
    val = props.getProperty("ciphersuites");
    if (val != null) {
      StringTokenizer stok = new StringTokenizer(val, " \t,;");
      enabledCipherSuites = new String[stok.countTokens()];
      int i = 0;
      while (stok.hasMoreTokens()) {
        enabledCipherSuites[i++] = stok.nextToken();
      }
    }

    val = props.getProperty("protocols");
    if (val != null) {
      StringTokenizer stok = new StringTokenizer(val, " \t,;");
      enabledProtocols = new String[stok.countTokens()];
      int i = 0;
      while (stok.hasMoreTokens()) {
        enabledProtocols[i++] = stok.nextToken();
      }
    }

    val = props.getProperty("clientauth");
    if (val != null)  {
      needClientAuth = true;
    }


    // switch socket factories
    int socketConfig = serverInfo.getSocketConfig();

    if      (socketConfig == SOCKETCONFIG_COMPRESSED) {
      csf = new CompressedClientSocketFactory();
      ssf = new CompressedServerSocketFactory();
    }
    else if (socketConfig == SOCKETCONFIG_SSL) {
      csf = new SslRMIClientSocketFactory();
      ssf = new SslRMIServerSocketFactory(enabledCipherSuites, enabledProtocols, needClientAuth);
    }
    else if (socketConfig == SOCKETCONFIG_COMPRESSED_SSL) {
      csf = new CompressedSslClientSocketFactory();
      ssf = new CompressedSslServerSocketFactory(enabledCipherSuites, enabledProtocols, needClientAuth);
    }
    // else: leave it null -> plain

    // special factories
    try {
      val = props.getProperty("csf");
      if (val != null) {
        // load csf class
        csf = (RMIClientSocketFactory)Class.forName(val).newInstance();
      }
      val = props.getProperty("ssf");
      if (val != null) {
        // load ssf class
        ssf = (RMIServerSocketFactory)Class.forName(val).newInstance();
      }
    }
    catch (Exception ex) {
      throw new ApplicationException("creating socket factories failed", ex);
    }

    val = props.getProperty("port");
    // notice: ssl and/or compressed requires another port than the original serverport
    if (val != null) {
      port = Integer.valueOf(val);
      checkPort(port);
    }

    // verify port agains fixed ports for sure
    port = getPort(port, csf, ssf);
  }
  
  
  
  
  /**
   * Creates an instance of an RMI-db-server with default connection object
   * (or configured entirely by db properties file)
   *
   * @param serverInfo the servers db-connection user info
   *
   * @throws ApplicationException if some configuration error
   */
  @SuppressWarnings("unchecked")
  public DbServer(UserInfo serverInfo) throws ApplicationException {
    this(serverInfo, null);
  }
  
  
  
  // check port range
  private void checkPort(int port) throws ApplicationException {
    if (port < -1 || (port > 0 && port < 1024)) {
      throw new ApplicationException("illegal port number " + port + ". Possible values: -1, 0, >= 1024");
    }
  }
  
  
  
  /**
   * @return the server info
   */
  public UserInfo getServerUserInfo() {
    return serverInfo;
  }
  
  
  /**
   * Gets the enables cipher suites
   * @return the suites
   */
  public String[] getEnabledCipherSuites() {
    return enabledCipherSuites;
  }

  /**
   * Gets the enabled protocols
   * @return the protocols
   */
  public String[] getEnabledProtocols() {
    return enabledProtocols;
  }
  
  /**
   * Determines whether the client need authentication as well.
   * 
   * @return true if server needs client authentication
   */
  public boolean getNeedClientAuth() {
    return needClientAuth;
  }
  
  
  /**
   * Gets the tcp port for a new remote object (e.g. session) according to
   * the fixed ports, if any set.
   *
   * @param port the requested port by the delegate, 0 = use system default
   * @param csf the client socket factory, null = same as connection
   * @param ssf the server socket factory, null = same as connection
   *
   * @return the granted port, 0 = use system default
   *
   * @throws ApplicationException if requested port could not be granted
   */
  public int getPort(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws ApplicationException {
    
    checkPort(port);
    
    int p = 0;    // granted port, 0 = all
    
    if (csf == null) {
      csf = this.csf;
    }
    if (ssf == null) {
      ssf = this.ssf;
    }
    
    if (ssf == null) {
      if (csf != null) {
        throw new ApplicationException("default ssf does not correspond to requested csf"); 
      }
      // plain port requested
      p = plainPort;
    }
    else if (ssf.getClass().equals(CompressedServerSocketFactory.class)) {
      if (csf.getClass().equals(CompressedClientSocketFactory.class) == false) {
        throw new ApplicationException("compressed ssf does not correspond to requested csf"); 
      }
      p = compressedPort;
    }
    else if (ssf.getClass().equals(SslRMIServerSocketFactory.class)) {
      if (csf.getClass().equals(SslRMIClientSocketFactory.class) == false) {
        throw new ApplicationException("ssl ssf does not correspond to requested csf"); 
      }
      p = sslPort;
    }
    else if (ssf.getClass().equals(CompressedSslServerSocketFactory.class)) {
      if (csf.getClass().equals(CompressedSslClientSocketFactory.class) == false) {
        throw new ApplicationException("compressed ssl ssf does not correspond to requested csf"); 
      }
      p = compressedSslPort;
    }
    // else: some unknown socket factories -> at application's risk!
    
    if (p == 0) {
      // no fixed port: requested one is ok
      p = port;
    }
    
    if (port != 0 && port != p) {
      throw new ApplicationException("protocol for requested port " + port + " is fixed to " + p);
    }
    
    if (p < 0) {
      throw new ApplicationException("service at this port is disabled");
    }
    
    return p;
  }
  
  
  
  /**
   * Get the fixed port for plain communication.
   *
   * @return the port number, 0 = not fixed, i.e. system default
   */
  public int getPlainPort() {
    return plainPort;
  }
  
  /**
   * Get the fixed port for compressed communication
   *
   * @return the port number, 0 = not fixed, i.e. system default
   */
  public int getCompressedPort() {
    return compressedPort;
  }
  
  /**
   * Get the fixed port for ssl communication
   *
   * @return the port number, 0 = not fixed, i.e. system default
   */
  public int getSslPort() {
    return sslPort;
  }
  
  /**
   * Get the fixed port for compressed+ssl communication
   *
   * @return the port number, 0 = not fixed, i.e. system default
   */
  public int getCompressedSslPort() {
    return compressedSslPort;
  }
  
  
  
  
  /**
   * Gets the port the server is listening on
   * @return the port
   */
  public int getPort() {
    return port;
  }
  
  /**
   * Gets the server's csf
   * @return the client socket factory
   */
  public RMIClientSocketFactory getClientSocketFactory() {
    return csf;
  }
  
  /**
   * Gets the server's ssf
   * @return the server socket factory
   */
  public RMIServerSocketFactory getServerSocketFactory() {
    return ssf;
  }


  /**
   * Gets the default session timeout.
   * @return the timeout in polling intervals.
   */
  public int getSessionTimeout() {
    return sessionTimeout;
  }
  
  
  /**
   * Starts the server.
   * 
   * @throws ApplicationException if startup failed
   */
  public void start() throws ApplicationException {
    try {
      // instantiate connection object
      Constructor<? extends RemoteDbConnectionImpl> constructor = this.connectionClass.getConstructor(
              DbServer.class, Integer.TYPE, RMIClientSocketFactory.class, RMIServerSocketFactory.class);
      connectionObject = constructor.newInstance(this, port, csf, ssf);
      
      int registryPort = 0;
      if (createRegistry) {
        URI uri = new URI(service);
        registryPort = uri.getPort();
        if (registryPort <= 0)  {
          registryPort = Registry.REGISTRY_PORT;  // default port (1099)
        }
        registry = LocateRegistry.createRegistry(registryPort);
      }

      // log
      DbGlobal.logger.info("\nTentackle RMI-server " + getClass().getName() +
                           "\nclient socket factory = " + (csf == null ? "<system default>" : csf.getClass().getName()) +
                           "\nserver socket factory = " + (ssf == null ? "<system default>" : ssf.getClass().getName()) +
                           "\nservice = " + service + (createRegistry ? (", registry created on port " + registryPort) : "") +
                           "\nlogin port = " + (port == 0 ? "<system default>" : port) + 
                           ", session timeout = " + sessionTimeout + "*" + sessionTimeoutInterval + "ms");
      
      // bind to service
      Naming.rebind(service, connectionObject);
      
      // start cleanup thread
      if (sessionTimeoutInterval > 0) {
        RemoteDbSessionImpl.startCleanupThread(sessionTimeoutInterval);
      }
    }
    catch (Exception e) {
      throw new ApplicationException("server startup failed", e);
    }
  }
  
  
  /**
   * Stops the server.
   * <p>
   * Unbinds the connection object.
   * @throws ApplicationException 
   */
  public void stop() throws ApplicationException {
    try {
      Naming.unbind(service);
      // is there anything we can do to stop the registry?
    }
    catch (Exception e) {
      throw new ApplicationException("server shutdown failed", e);
    }
  }
  
}
