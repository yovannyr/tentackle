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

// $Id: Db.java 477 2009-08-09 18:54:26Z harald $


package org.tentackle.db;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.tentackle.db.rmi.DbRemoteDelegate;
import org.tentackle.db.rmi.LoginFailedException;
import org.tentackle.db.rmi.RemoteDbConnection;
import org.tentackle.db.rmi.RemoteDbSession;
import org.tentackle.db.rmi.RemoteDbSessionImpl;
import org.tentackle.db.rmi.RemoteDelegate;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.CharConverter;
import org.tentackle.util.DefaultObfuscator;
import org.tentackle.util.StringHelper;
import org.tentackle.util.TrackedArrayList;


/**
 * A logical database connection.
 * 
 * Db connections are an abstraction for a connection between a client
 * and a server, whereas "server" is not necessarily a database server,
 * because Db connections can be either local or remote.<br>
 * A local Db talks to a database backend via a {@link ConnectionManager} 
 * which is responsible for the physical JDBC-connection. However, there
 * is no 1:1 relationship between a Db and a physical connection. This is up
 * to the connection manager.
 * Local Db connections are used in client applications running in 2-tier mode
 * or in application servers.<br>
 * Remote Db connections are connected to a Tentackle application server
 * via RMI. Client applications running in 3-tier mode (more precise n-tier, with n >= 3)
 * use remote connections.<br>
 * With the abstraction of a logical Db connection, Tentackle applications
 * are able to run both in 2- or n-tier mode without a single modification
 * to the source code. It's just a small change in a config file.
 * <p>
 * The configuration is achieved by a property file, which is located
 * by the {@link UserInfo} passed to the Db.
 * <p>
 * Local connections can be either a DataSource bound via JNDI or a direct
 * JDBC connection. JNDI is used in application servers, for example running
 * a JRuby on Rails {@link org.tentackle.appworx.WebApplication} from within Glassfish.
 * JDBC connections are for standalone 2-tier applications or Tentackle application servers.
 * <br>
 * For local connections via JDBC the file contains at least the following properties:
 * 
 * <pre>
 * driver=jdbc-driver
 * url=jdbc-url
 * 
 * Example:
 * driver=org.postgresql.Driver
 * url=jdbc:postgresql://gonzo.krake.local/erp
 * </pre>
 * 
 * Local connections via JNDI need only the url. The url starts with
 * "<tt>jndi:</tt>" and is followed by the JNDI-name. The optional database backend type is
 * only necessary if it cannot be determined from the connection's metadata. The following
 * types are currently supported: <tt>db2, ingres, informix, mssql, mysql, oracle and postgres</tt>.
 * 
 * <pre>
 * url=jndi:name[:database-type]
 * 
 * Example:
 * url=jndi:jdbc/erpPool
 * </pre>
 * 
 * For remote connections only one line is required at minimum:
 * 
 * <pre>
 * url=rmi://hostname[:port]/service
 * 
 * Example:
 * url=rmi://gonzo.krake.local:28004/ErpServer
 * </pre>
 * 
 * The port is optional and defaults to 1099 (the default RMI registry). 
 * For setting up an application server please refer to {@link rmi.DbServer}.
 * <p>
 * Optionally, the following properties can be defined:
 * <p>
 * For local connections via JDBC:
 * <ul>
 *  <li>
 *  JDBC-drivers usually send the username and password in cleartext to the 
 *  database backend. This is a security risk. If you don't have the option to use ssl for your backend
 *  you can "obfuscate" the login. This is simply a text transformation. Of course,
 *  you must change the names and passwords of the database accounts according to
 *  the transformation. The obfuscator can be changed by setting {@link #usernameObfuscator}.
 *  Obfuscation is also an effective solution to prevent users from connecting
 *  with popular tools like Access to the database backend. Anyway, for larger sites,
 *  consider going n-tier.
 *  <pre>
 *  obfuscate=yes|no
 * 
 *  The default is no.
 *  </pre>
 *  </li>
 *  <li>
 *  A fixed user and password is useful for small sites
 *  if the authentication is done on the application level and all users
 *  connect to the backend using the same account. By this, users can easily
 *  be added or removed and their passwords changed without access to special
 *  database administration tools.
 *  <pre>
 *  dbuser=fixed-user
 *  dbpasswd=fixed-password
 * 
 *  Example:
 *  dbuser=harald
 *  dbpasswd=xyzzy
 *  </pre>
 *  </li>
 *  <li>
 *  Because the fixed user and fixed password is stored in the properties file, it is
 *  far too easy for the users to get it known. For those people (with no java-skills)
 *  you can provide a class extending {@link UserInfo} that provides the username
 *  and password. This class can do the weirdest things, for example connect
 *  to an LDAP-server. For skilled users, however, this isn't safe
 *  enough either. Again, consider going n-tier.
 *  <pre>
 *  dbuserinfo=classname
 *  
 *  Example:
 *  dbuserinfo=de.krake.lager.auth.LdapUserInfo
 *  </pre>
 *  </li>
 * </ul>
 * <p>
 * For local connections via JDBC or JNDI: 
 * <ul>
 *  <li>
 *  Each object persistable to the database (database object for short) 
 *  gets a unique object ID which is generated
 *  by an {@link IdSource}. The default source is {@link ObjectId}.
 *  <pre>
 *  idsource=id-source-descriptor
 *  </pre>
 *  See {@link IdSourceConfigurator} for details.
 *  </li>
 * </ul>
 * 
 * <p>
 * For remote connections, the property file will be sent to the server.
 * This can be used to set some application specific options
 * or to tune the connection. For example, if the server requires an SSL connection,
 * the client can request to turn off SSL after successful login. Or clients using
 * slow communication lines may request compression. 
 * The following properties are already predefined:
 * <ul>
 *  <li>
 *  If not provided as a command line option (see the java runtime documentation)
 *  you can put the keystore and truststore configuarion within the property file:
 *  <pre>
 *  keystore=name of the keystore file
 *  keystorepassword=keystore password
 *  truststore=name of the truststore file
 *  truststorepassword=truststore password
 *  </pre>
 *  The truststore is only necessary for servers requesting client authentication.
 *  </li>
 *  <li>
 *  Request compression (after login):
 *  <pre>
 *  compressed
 *  </pre>
 *  </li>
 *  <li>
 *  Request SSL (after login):
 *  <pre>
 *  ssl
 *  </pre>
 *  </li>
 *  <li>
 *  Turn off compression (after login):
 *  <pre>
 *  uncompressed
 *  </pre>
 *  </li>
 *  <li>
 *  Turn off SSL (after login):
 *  <pre>
 *  nossl
 *  </pre>
 *  </li>
 *  <li>
 *  Turn off compression and SSL, i.e. force plain communication (after login):
 *  <pre>
 *  plain
 *  </pre>
 *  </li>
 *  <li>
 *  By default, the socket factories are determined according to the communication type.
 *  However, you can provide your own factories.
 *  <pre>
 *  csf=client socket factory
 *  ssf=server socket factory
 *  </pre>
 *  See {@link RemoteDbSessionImpl} for more details.
 *  </li>
 * </ul>
 * 
 * @author harald
 */
public class Db implements Cloneable, Comparable<Db> {
  
  private String  url;                                    // JDBC Url, e.g. "jdbc:postgresql:muz"
  private String  driver;                                 // JDBC Driver to load, e.g. "org.postgresql.Driver"
  private boolean remote;                                 // true if remote connection
  private DataSource jndiDataSource;                      // != null if JNDI DataSource
  private Exception loginFailedCause;                     // if login failes: holds the possible cause
  private IdSourceConfigurator idConfig;                  // configuration of IdSource
  private ConnectionManager conMgr;                       // connection manager for local connections
  private int conId;                                      // local connection ID
  private int groupConId;                                 // ID of the connection group, 0 = none
  private ManagedConnection con;                          // connection if attached, null = detached
  private DbPool dbPool;                                  // != null if this Db is managed by a DbPool
  private int poolId;                                     // the poolid if dbPool != null
  private UserInfo ui;                                    // user information
  private boolean obfuscatedLogin;                        // obfuscate username and password for db-login
  private Db clonedFromDb;                                // the original db if this is a cloned one
  private String dbUser;                                  // fixed Db-User, null = dbuser same as connect user
  private char[] dbPasswd;                                // fixed Db-Passwd
  private boolean autoCommit;                             // true if autocommit on, else false
  private boolean countModificationAllowed = true;        // allow modification count
  private Map<String, ModificationCounter> modMap;        // map of ModificationCounter objects (tables) for this connection
  private boolean logModificationAllowed = true;          // allow modification log 
  
  private IdSource defaultIdSource;                       // default ID-Source for db-connection (if not overridden in derivates)
  private IdSource[] idSources;                           // ID sources for this db-connection
  private static int nextIdSourceId;                      // next global free slot-ID for IdSource

  private int fetchSize;                                  // default fetchsize, 0 = drivers default
  private int maxRows;                                    // maximum rows per resultset, 0 = no limit
  
  private boolean uniqueViolation;                        // true if last insert or update returned false due to a unique-violation
  private boolean logUniqueViolation = true;              // true if unique-violations are logged (default)
  
  private boolean logModificationTxEnabled;               // true if log transaction begin/commit in modification log, default is false 
  private long    logModificationTxId;                    // transaction id. !=0 if 'commit' is pending in modification log
  private boolean logModificationDeferred;                // true if log to memory rather than dbms
  private List<ModificationLog> modificationLogList;      // in-memory modification-log (not written to dbms)
  
  private long txCount;                                   // transaction counter
  private String txName;                                  // optional transaction name
  private DbObject txObject;                              // optional top-level object initiating the transaction
  private List<CommitTxRunnable> commitTxRunnables;       // runnables to be executed upon commit (see ObjectId for an example)
  private List<RollbackTxRunnable> rollbackTxRunnables;   // runnables to be executed upon rollback (see poolkeeper.IdRange for an example)

  private boolean alive;                                  // true = connection is still in use, false = connection has timed out
  private int     updateCount;                            // number of object modified by executeUpdate since the last begin()
  
  // flags for databases that need special treatment
  private boolean ingres;                                 // true if backend is ingres
  private boolean postgres;                               // true if backend is postgres
  private boolean oracle;                                 // true if backend is oracle
  private boolean informix;                               // true if backend is informix
  private boolean mysql;                                  // true if backend is mysql
  private boolean db2;                                    // true if backend is db2
  private boolean mssql;                                  // true if backend is MS SQL
  
  
  // for RMI remote connections (per Db)
  private RemoteDbConnection        rcon;                 // != null if remote connection established to RMI-server
  private RemoteDbSession           rses;                 // remote database session if logged in to RMI-server
  private DbRemoteDelegate          rdel;                 // remote database delegate
  private RemoteDelegate[]          delegates;            // the delegates per session
  // for RMI remote connections (all Db)
  private static Class[]            remoteClasses;        // the classes Objects for the delegates provide service for
  private static int                nextDelegateId;       // next handle per class
  
  private int instanceNumber;             // each Db gets a unique instance number ...
  private static int instanceCount;       // ...  to make it a Comparable
  
  private static synchronized int countInstance() {
    return ++instanceCount;
  }
  
  
  // special construct to avoid check for "AND/WHERE"
  
  /** "WHERE 1=1" allows AND to be added **/
  public static final String WHEREALL_CLAUSE = " WHERE 1=1";
  /** WHEREALL_CLAUSE + AND will be replaced by... WHERE_CLAUSE **/
  public static final String WHEREAND_CLAUSE = WHEREALL_CLAUSE + " AND ";
  /** WHEREALL_CLAUSE + OR will be replaced by... WHERE_CLAUSE **/
  public static final String WHEREOR_CLAUSE = WHEREALL_CLAUSE + " OR ";
  /** "WHERE" **/
  public static final String WHERE_CLAUSE = " WHERE ";
  
  

  
  // determine the database backend according to the url
  private void determineBackendType(String url) throws ApplicationException {
    // figure out kind of local db
    if (url.indexOf("ingres") >= 0) {
      ingres = true;  
    }
    else if (url.indexOf("postgres") >= 0) {
      postgres = true;  
    }
    else if (url.indexOf("oracle") >= 0)  {
      oracle = true;
    }
    else if (url.indexOf("informix") >= 0)  {
      informix = true;
    }
    else if (url.indexOf("mysql") >= 0)  {
      mysql = true;
    }
    else if (url.indexOf("db2") >= 0)  {
      db2 = true;
    }
    else if (url.indexOf("microsoft.jdbc.") >= 0 ||
             url.indexOf("mssql") >= 0)  {    // mssql provided for JNDI
      mssql = true;
    }
    else  {
      throw new ApplicationException("unsupported database backend type");
    }
  }
  
  
  
  /**
   * Creates an instance of a logical db connection.
   *
   * @param conMgr the connection manager to use for this db
   * @param ui user information
   */
  public Db(ConnectionManager conMgr, UserInfo ui) {

    instanceNumber = countInstance();
    
    this.conMgr = conMgr;
    
    setUserInfo(ui);
    
    // establish connection to the database server
    try {
      
      // load configuration settings
      Properties dbProperties = ui.getDbProperties();
      
      url = dbProperties.getProperty("url");
      if (url == null) {
        throw new ApplicationException("url=<jdbc-url> missing in " + ui.getDbPropertiesName());
      }
      
      if (url.startsWith("rmi:")) {
        remote = true;                    // this is a remote connection!
        driver = getClass().getName();    // driver is the Db class ;)
        ui.configureSsl();                // setup the ssl settings if not yet done
      }
      
      else  {
        
        if (url.startsWith("jndi:")) {
          int cndx = url.lastIndexOf(':');
          jndiDataSource = (DataSource)new InitialContext().lookup(
                  cndx < 6 ? url.substring(5) : url.substring(5, cndx));
          if (cndx < 6) {
            // database type not given: open temporary connection to get the URL
            Connection tempCon = null;
            try {
              tempCon = jndiDataSource.getConnection();
              DatabaseMetaData metaData = tempCon.getMetaData();
              if (metaData == null) {
                throw new ApplicationException(
                   "Metadata of JNDI-connection cannot be determined. Please add the backend type to the connection url!");
              }
              String jndiURL = metaData.getURL();
              if (jndiURL == null) {
                throw new ApplicationException(
                   "URL of JNDI-connection cannot be determined. Please add the backend type to the connection url!");
              }
              determineBackendType(jndiURL);
            }
            finally {
              if (tempCon != null) {
                tempCon.close();    // return to pool. The connections are managed by the ConnectionManager
              }
            }
          }
          else  {
            determineBackendType(url);
          }
        }
        else  {
          driver = dbProperties.getProperty("driver");
          if (driver == null) {
            throw new ApplicationException("driver=<jdbc-driver> missing in " + ui.getDbPropertiesName());
          }
          determineBackendType(url);
        }
      }
      
      String val = dbProperties.getProperty("obfuscate");
      if (val != null && val.toUpperCase().equals("YES")) {
        obfuscatedLogin = true;
      } 
      
      val = dbProperties.getProperty("idsource");
      if (val != null) {
        idConfig = new IdSourceConfigurator(val);
      }
      
      dbUser = dbProperties.getProperty("dbuser");
      val = dbProperties.getProperty("dbpasswd");
      if (val != null) {
        dbPasswd = val.toCharArray();
      }
      
      val = dbProperties.getProperty("dbuserinfo");
      if (val != null) {
        UserInfo dbUserInfo = (UserInfo)Class.forName(val).newInstance();
        dbUser   = dbUserInfo.getUsername();
        dbPasswd = dbUserInfo.getPassword();
      }
      
    } 
    catch (Exception e) {
      DbGlobal.errorHandler.severe(this, e, "database configuration failed: " + e.getMessage());
    }
  }
  
  
  /**
   * Creates an instance of a db-connection with the default connection manager
   * as defined by {@link DbGlobal#connectionManager}.
   *
   * @param ui user information
   */
  public Db(UserInfo ui) {
    this(DbGlobal.connectionManager, ui);
  }
  
  
  
  /**
   * for logging
   */
  @Override
  public String toString()  {
    return "Db" + instanceNumber + "[" + conId + 
           (groupConId > 0 ? "g" + groupConId : "") + 
           "]=" + url;
  }
  
  
  
  /** 
   * Compares two db instances.
   * Implemented for trees, caches, etc... because they need a Comparable.
   * We simply use the instanceNumber. Because the instanceNumber
   * is unique, we don't need to override equals/hash.
   *
   * @param db the db connection to compare this db with
   * @return  a negative integer, zero, or a positive integer as this object
   *          is less than, equal to, or greater than the specified object.
   */
  public int compareTo(Db db) {
    return instanceNumber - db.instanceNumber;
  }

  
  /**
   * Gets the connection manager for this Db.
   *
   * @return the connection manager
   */
  public ConnectionManager getConnectionManager() {
    return conMgr;
  }
 
  
  /**
   * Sets the pool manager.
   * The method is invoked from a DbPool when the Db is created.
   *
   * @param dbPool the db pool, null = not pooled
   */
  public void setPool(DbPool dbPool) {
    this.dbPool = dbPool;
  }
  
  /**
   * Gets the pool manager.
   *
   * @return the db pool, null = not pooled
   */
  public DbPool getPool() {
    return dbPool;
  }
  
  
  /**
   * Checks whether this Db is pooled.
   *
   * @return true if pooled, false if not pooled
   */
  public boolean isPooled() {
    return dbPool != null;
  }
  
  
  /**
   * Sets the pool id.
   * The method is invoked from a DbPool when the Db is used in a pool.
   *
   * @param poolId the ID given by the pool (> 1), 0 = not used (free Db)
   */
  public void setPoolId(int poolId) {
    this.poolId = poolId;
  }
  
  /**
   * Gets the poolid.
   *
   * @return the Id given by the pool if used, 0 = not used.
   */
  public int getPoolId() {
    return poolId;
  }
  
  
  /**
   * Gets the remote connection object.
   * 
   * @return the connection object, null if local
   */
  public RemoteDbConnection getRemoteConnection() {
    return rcon;
  }
  
  
  /**
   * Gets the remote session object.
   * @return the session, null if local
   */
  public RemoteDbSession getRemoteSession() {
    return rses;
  }
 
  
  /**
   * The current password obfuscator
   */
  public static CharConverter passwordObfuscator = DefaultObfuscator.createObfuscator('P');
  
  /**
   * The current username obfuscator
   */
  public static CharConverter usernameObfuscator = DefaultObfuscator.createObfuscator('@');
  
  /**
   * Creates a physical low level connection to the local db server via JDBC
   * using this Db (authentication by userinfo or fixed user/password).
   *
   * @return the low level JDBC connection
   * @throws SQLException if connection failed
   */
  public Connection connect() throws SQLException {
    
    String username = dbUser   != null ? dbUser   : ui.getUsername();
    char[] passwd   = dbPasswd != null ? dbPasswd : ui.getPassword();

    if (obfuscatedLogin)  {
      username = username == null ? null : new String(usernameObfuscator.convert(username.toCharArray()));
      passwd   = passwordObfuscator.convert(passwd);
    }

    Connection connection = null;
    
    if (jndiDataSource != null) {
      connection = jndiDataSource.getConnection();
    }
    else  {
      // special handling for informix
      if (informix) {
        connection = DriverManager.getConnection (url +
                    ";user=" + username + 
                    ";password=" + (passwd == null ? "" : new String(passwd)));
      }
      else  {
        // standard way
        connection = DriverManager.getConnection (url, username, (passwd == null ? "" : new String(passwd)));
      }
    }
    
    if (obfuscatedLogin && passwd != null)  {
      for (int i=0; i < passwd.length; i++) {
        passwd[i] = 0;
      }
    }

    if (connection.getAutoCommit() == false) {
      connection.setAutoCommit(true);   // set to autocommit mode for sure
    }
    
    return connection;
  }
  
  
  /**
   * Checks whether the db connection is still in use.
   * Whenever a {@link StatementWrapper} or {@link PreparedStatementWrapper} is used
   * (i.e executeQuery or executeUpdate), the db connection is set to be alive.
   * Some other thread may clear this flag regulary and check whether it has
   * been set in the meantime.
   *
   * @return true if connection still in use, false if not used since last setAlive(false).
   */
  public boolean isAlive() {
    if (isRemote()) {
      try {
        return rdel.isAlive();
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "isAlive failed");
      }
    }
    return alive;
  }
  
  /**
   * Sets the db connection's alive state.
   *
   * @param alive is true to signal it's alive, false to clear
   */
  public void setAlive(boolean alive) {
    if (isRemote()) {
      try {
        rdel.setAlive(alive);
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "setAlive failed");
      }
    }
    else {
      this.alive = alive;
    }
  }
  

  
  /**
   * Sets the ID-Source configurator.
   * 
   * @param idConfig the configurator
   */
  public void setIdSourceConfigurator(IdSourceConfigurator idConfig) {
    this.idConfig = idConfig;
  }
  
  /**
   * Gets the ID-Source configurator.
   * 
   * @return the configurator
   */
  public IdSourceConfigurator getIdSourceConfigurator() {
    return idConfig;
  }
  
  
  /**
   * create the ID source
   */
  private void setupIdSource() throws ApplicationException {
    if (idConfig != null) {
      setDefaultIdSource(idConfig.connect(this));
    }
    else  {
      setDefaultIdSource(new ObjectId(this));
    }
  }

  
  /**
   * Handles a connect exception (in open or clone).
   * The method returns for warnings and terminates the application
   * for severe failures the user cannot handle.
   */
  private void handleConnectException(Exception e) {
    
    if (e instanceof DbRuntimeException) {
      // get original SQLException if runtime exception thrown
      Throwable t = ((DbRuntimeException)e).getCause();
      if (t instanceof SQLException) {
        e = (Exception) t;
      }
    }
    
    // warning causes
    if (e instanceof SQLException)  {
      loginFailedCause = e;
      DbGlobal.errorHandler.warning(this, e, Locales.bundle.getString("Verbindung_abgelehnt"));
      return;
    }
    
    if (e instanceof RemoteException)  {
      // check if chain contains a LoginFailedException
      Throwable t = e;
      do {
        if (t instanceof LoginFailedException) {
          loginFailedCause = (LoginFailedException)t;
          DbGlobal.errorHandler.warning(this, loginFailedCause, Locales.bundle.getString("Verbindung_abgelehnt"));
          return;
        }
        t = t.getCause();
      } while (t != null);
      
      // else some other server error
      loginFailedCause = e;
      DbGlobal.errorHandler.severe(this, e, Locales.bundle.getString("Verbindung_abgelehnt"));
      return;   // notreached        
    }
    
    // severe causes (will usually terminate the application)
    if (e instanceof ClassNotFoundException)  {
      loginFailedCause = e;
      DbGlobal.errorHandler.severe(this, e, Locales.bundle.getString("Datenbank-Treiber_nicht_gefunden"));
      return;   // notreached
    }
    
    // terminate application
    loginFailedCause = e;
    DbGlobal.errorHandler.severe(this, e, Locales.bundle.getString("connection_failed"));
  }
  
  
  
  
  /**
   * Open a database connection.
   * If the login failes due to wrong passwords
   * or denied access by the application server,
   * the method {@link #handleConnectException} is invoked.
   * If the application is not terminated due a severe error,
   * {@link #getLoginFailedCause()} holds the reason,
   * for example "already logged in".
   *
   * @return true if open, else false
   */
  public boolean open () {
    
    loginFailedCause = null;
    
    try {
      
      if (remote)  {
        // database is remote!
        
        // get connection to RMI-server
        rcon = (RemoteDbConnection) Naming.lookup(url);   // throws exception if failes
        
        // get session
        rses = rcon.login(ui);    // throws exception if login denied
        
        // get delegate
        rdel = rses.getDbRemoteDelegate();
        
        // get the remote connection id
        conId = rdel.getConnectionId();
      }
      
      else  {
        // direct connection to database
      
        // load Driver
        if (driver != null) {   // if not via JNDI
          Class.forName(driver);
        }

        // connect to database
        conId = conMgr.login(this);
      }
      
      // fresh connections are always in autoCommit mode
      autoCommit = true;
      
      setupIdSource();                              // create the ID source
      ui.setSince(System.currentTimeMillis());      // set the login time
      
      alive = true;   // all db start alive!
    
      if (DbGlobal.logger.isInfoLoggable() && !isCloned()) {
        DbGlobal.logger.info("connection '" + this + "' established");
      }
      
      return true;
    }
    
    // warning causes
    catch (Exception e)  {
      handleConnectException(e);
      return false;
    }    
  }
  
  
  /**
   * Gets the Exception that caused the last open() to fail.
   * 
   * @return the cause
   */
  public Exception getLoginFailedCause() {
    return loginFailedCause;
  }
  
  /**
   * Sets the cause for login failed.
   *
   * @param loginFailedCause the Exception
   */
  public void setLoginFailedCause(Exception loginFailedCause) {
    this.loginFailedCause = loginFailedCause;
  }
  
  
  
  /**
   * Clears all passwords (stored in char[]-arrays) so
   * that they are no more visible in memory.
   */
  public void clearPassword() {
    // once in UserInfo
    ui.clearPassword();
    // second for fixed DbUser
    if (dbPasswd != null) {
      for (int i=0; i < dbPasswd.length; i++) {
        dbPasswd[i] = 0;
      }
      dbPasswd = null;
    }
  }
  

  /**
   * Close a database connection.
   */
  public void close () {
    try {
      if (isRemote())  {
        if (rcon != null && rses != null) {
          rcon.logout(rses);      // close the session
          if (DbGlobal.logger.isInfoLoggable()) {
            DbGlobal.logger.info("remote db '" + this + "' closed");
          }
        }
      }
      else  {
        if (conId > 0) {
          if (con != null) {
            con.closeAllPreparedStatements(true);   // cleanup all pending statements
          }
          if (conMgr.logout(conId) != this) {
            throw new DbRuntimeException("connection manager corrupted");
          }
          if (DbGlobal.logger.isInfoLoggable()) {
            DbGlobal.logger.info("db '" + this + "' closed");
          }
        }
      }
      clearMembers(this);   // clear members for re-open
    } 
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, Locales.bundle.getString("Fehler_beim_Schliessen_der_Datenbank"));
    }
  }
  
  
  /**
   * finalizer if connection is broken
   */
  @Override
  public void finalize()  {
    try {
      close();    // cleanup for sure
    }
    catch (Exception ex)  {
      // nothing we can do
    }
  }


  /**
   * Gets the connection state.
   *
   * @return true if db is open, else false
   */
  public boolean isOpen() {
    if (isRemote()) {
      return rdel != null;
    }
    else  {
      return conId > 0;
    }
  }
  
  
  /**
   * Transactions get a unique transaction number by
   * counting the transactions per Db instance.
   * 
   * @return the current transaction counter
   */
  public long getTxCount()  {
    return txCount;
  }
  
  
  /**
   * Gets the optional transaction name.
   * Useful to distinguish transactions in logModification or alike.
   * The tx-name is cleared after commit or rollback.
   *
   * @return the transaction name, null if no tx set.
   */
  public String getTxName() {
    return txName;
  }



  /**
   * Sets the optional transaction object.<br>
   * By default, whenever a transaction is initiated by a persistence operation of
   * a {@link DbObject} or {@link DbObject#isTxObject} returns true, that object becomes the "parent" of the
   * transaction. The {@code txObject}
   * is mainly used for logging and enhanced auditing (partial history) during transactions.
   * The {@code txObject} is cleared at the end of the transaction.
   *
   * @param txObject the transaction object, null to clear
   */
  public void setTxObject(DbObject txObject) {
    this.txObject = txObject;
  }


  /**
   * Gets the optional transaction object.
   *
   * @return the transaction object, null if none
   */
  public DbObject getTxObject() {
    return txObject;
  }
  
  
  
  /**
   * Starts a transaction.<br>
   * Does nothing if a transaction is already running!
   *
   * @param txName is the optional transaction name, null if none
   *
   * @return the old commit value, true if a new transaction was begun
   */
  public boolean begin(String txName)  {
    
    if (DbGlobal.logger.isFinerLoggable()) {
      DbGlobal.logger.finer("begin transaction on " + this + ", txName=" + txName);
    }
    
    boolean begun = false;
    
    if (isRemote()) {
      try {
        long newTxCount = rdel.begin(txName);
        if (newTxCount > 0) {
          begun = true;             // new transaction begun
          txCount = newTxCount;     // remember tx count
          this.txName = txName;     // set transaction name
          updateCount = 0;          // clear update counter
          autoCommit = false;       // we are now within a tx
        }
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "begin failed");
      }
    }
    else {
      if (setAutoCommit(false)) {
        begun = true;               // new transaction begun
        txCount++;                  // count tx
        this.txName = txName;       // set optional transaction name
        commitTxRunnables = null;   // remove pending runnables if any
        rollbackTxRunnables = null;
      }
    }
    
    return begun;
  }


  /**
   * Starts a transaction.<br>
   * Does nothing if a transaction is already running!
   *
   * @return the old commit value, true if a new transaction was begun
   */
  public boolean begin()  {
    return begin(null);
  }

  
  
  /**
   * Creates a begin modification log if necessary.
   * This is invoked from DbObject.logModification and should not
   * be used in apps, hence the method is in package scope only.
   */ 
  void logBeginTx() {
    if (logModificationTxEnabled && logModificationTxId == 0) {
      ModificationLog log = new ModificationLog(this, DbObject.BEGIN);
      if (log.save() == false) {
        DbGlobal.errorHandler.severe(this, null, "creating BEGIN log failed");
      }
      logModificationTxId = log.getId();
    }    
  }
  
  
  
  /**
   * Gets the number of objects modified since the last begin().
   * The method is provided to check whether objects have been
   * modified at all.
   *
   * @return the number of modified objects
   */
  public int getUpdateCount() {
    return updateCount;
  }
  
  
  /**
   * Add to updateCount.
   * 
   * @param count the number of updates to add
   */
  void addToUpdateCount(int count) {
    updateCount += count;
  }
  
  
  
  
  /**
   * Registers a {@link CommitTxRunnable} to be invoked just before
   * committing a transaction.
   * 
   * @param txRunnable the runnable to register
   */
  public void registerCommitTxRunnable(CommitTxRunnable txRunnable) {
    if (commitTxRunnables == null)  {
      commitTxRunnables = new ArrayList<CommitTxRunnable>();
    }
    commitTxRunnables.add(txRunnable);
  }
  
  
  /**
   * Registers a {@link RollbackTxRunnable} to be invoked just before
   * rolling back a transaction.
   * 
   * @param txRunnable the runnable to register
   */
  public void registerRollbackTxRunnable(RollbackTxRunnable txRunnable) {
    if (rollbackTxRunnables == null)  {
      rollbackTxRunnables = new ArrayList<RollbackTxRunnable>();
    }
    rollbackTxRunnables.add(txRunnable);
  } 
  
  
  
  
  /**
   * Commits a transaction if the corresponding begin() has started it.
   * The corresponding begin() is determined by the autoCommit parameter.
   * If autoCommit is true (which is the value begin() returned), the
   * tx is committed. If false, the begin() was nested within an already
   * running tx, so it is ignored.
   *
   * @param autoCommit the old autoCommit state before begin()
   *
   * @return true if tx was really committed, false if not.
   */
  public boolean commit(boolean autoCommit)  {
    
    if (DbGlobal.logger.isFinerLoggable()) {
      DbGlobal.logger.finer("commit on " + this + ", autoCommit=" + autoCommit);
    }
    
    boolean committed = false;
    
    if (isRemote()) {
      try {
        committed = rdel.commit(autoCommit);
        if (committed) {
          txName = null;
          txObject = null;
          this.autoCommit = true;   // now outside tx again
        }
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "commit failed");
      }
    }
    
    else  {
      if (autoCommit) {
        if (!this.autoCommit) {
          // within a transaction: commit!
          
          if (logModificationTxId != 0) {
            if (logModificationTxEnabled) {
              new ModificationLog(this, DbObject.COMMIT).save();
            }
            logModificationTxId = 0;
          }

          if (commitTxRunnables != null) {
            /**
             * execute pending runnables.
             * Notice: the runnables should throw DbRuntimeException on failure!
             */
            for (CommitTxRunnable r: commitTxRunnables)  {
              r.commit();
            }
            commitTxRunnables = null;
          }

          setAutoCommit(true);    // according to the specs: this will commit!

          rollbackTxRunnables = null;
          txName = null;
          txObject = null;
          committed = true;
        }
        // else: not in a transaction: do nothing (this avoids dbms-warnings)
      }
      else  {
        if (this.autoCommit)  {
          // not within a transaction, i.e. nothing to commit, but autoCommit changed (somehow)
          setAutoCommit(false);
        }
        // else: within a transaction but autoCommit should not change: ignore commit()
      }
    }
    
    return committed;
  }

  
  
  
  /**
   * Rolls back a transaction if the corresponding begin() has started it.
   * The corresponding begin() is determined by the autoCommit parameter.
   * If autoCommit is true (which is the value begin() returned), the
   * tx is rolled back. If false, the begin() was nested within an already
   * running tx, so it is ignored.
   *
   * @param autoCommit the old autoCommit state before begin()
   *
   * @return true if tx was really rolled back, false if not.
   */
  public boolean rollback(boolean autoCommit)  {
    
    if (DbGlobal.logger.isFinerLoggable()) {
      DbGlobal.logger.finer("rollback on " + this + ", autoCommit=" + autoCommit);
    }
    
    boolean rolledBack = false;
    
    if (isRemote()) {
      try {
        rolledBack = rdel.rollback(autoCommit);
        if (rolledBack) {
          txName = null;
          txObject = null;
          this.autoCommit = true;   // now outside tx again
        }
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "rollback failed");
      }
    }
    else {
      if (autoCommit) {
        // begin() started a new tx
        if (!this.autoCommit) {
          // within a transaction
          if (rollbackTxRunnables != null) {
            /**
             * execute pending runnables.
             * Notice: the runnables should throw DbRuntimeException on failure!
             */
            for (RollbackTxRunnable r: rollbackTxRunnables)  {
              r.rollback();
            }
            rollbackTxRunnables = null;
          }
          con.rollback();         // avoid a commit ...
          setAutoCommit(true);    // ... in setAutoCommit
          logModificationTxId = 0;
          commitTxRunnables = null;
          txName = null;
          txObject = null;
          rolledBack = true;
        }
        // else: not whithin a transaction: do nothing (this avoids dbms-warnings)
      }
      else  {
        // begin() was invoked within a tx
        if (this.autoCommit)  {
          // not within a transaction, i.e. nothing to commit, but autoCommit changed
          setAutoCommit(false);
        }
        // else: within a transaction but autoCommit should not change: ignore that!
      }
    }
    
    return rolledBack;
  }
  
  
  /**
   * Sets the current connection.
   * This method is package scope and invoked whenever a connection
   * is attached or detached to/from a Db by the ConnectionManager.
   */
  void setConnection(ManagedConnection con) {
    this.con = con;
  }
  
  
  /**
   * Gets the current connection.
   *
   * @return the connection, null = not attached
   */
  ManagedConnection getConnection() {
    return con;
  }
  

  /**
   * Gets the connection id.
   * This is a unique number assigned to this Db by the ConnectionManager.
   *
   * @return the connection id, 0 = Db is new and not connected so far
   */
  public int getConnectionId() {
    return conId;
  }
  
  
  /**
   * Sets the group number for this db.
   * This is an optional number describing groups of db-connections,
   * which is particulary useful in rmi-servers: if one connection
   * fails, all others should be closed as well.
   * Groups are only meaningful for local db-connections, i.e.
   * for remote dbs the group instance refers to that of the rmi-server.
   *
   * @param number is the group number, 0 = no group
   */
  public void setGroupId(int number) {
    if (isRemote()) {
      try {
        rdel.setGroupId(number);
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "setGroupId failed");
      }
    }
    groupConId = number;
  }
  
  
  /**
   * Gets the group instance number.
   *
   * @return the instance number, 0 = no group.
   */
  public int getGroupId() {
    if (isRemote()) {
      try {
        return rdel.getGroupId();
      }
      catch (RemoteException e)  {
        DbGlobal.errorHandler.severe(this, e, "getGroupId failed");
      }
    }
    return groupConId;
  }
  
  
  
  // attaches the connection
  private ManagedConnection attach() {
    if (conMgr == null) {
      DbGlobal.errorHandler.severe(this, null, "no connection manager");
      return null;    // NOTREACHED
    }
    else if (!isOpen()) {
      DbGlobal.errorHandler.severe(this, null, "db is closed");
      return null;    // NOTREACHED
    }
    else  {
      return conMgr.attach(conId);
    }
  }
  

  /**
   * Creates a non-prepared statement.
   * <p>
   * Non-prepared statements attach the db as soon as they
   * are instatiated! The db is detached after executeUpdate or after
   * executeQuery when its result set is closed.
   * 
   * @return the statement wrapper
   */
  public StatementWrapper createStatement () {
    try {
      assertNotRemote();
      attach();
      StatementWrapper stmt = new StatementWrapper(con, con.createStatement(this));
      stmt.markReady();
      return stmt;
    } 
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, Locales.bundle.getString("SQL-Statement_Erzeugungsfehler!"));
    }
    return null;
  }

  
  
  
  /**
   * Optimize WHERE 1=1 AND/OR to WHERE.
   * Any remaining WHERE 1=1 will be removed too.
   * If you don't want your statements getting optimized, use lowercase.
   */
  private String optimizeSql(String sql)  {
    return sql.replace(WHEREAND_CLAUSE, WHERE_CLAUSE).
            replace(WHEREOR_CLAUSE, WHERE_CLAUSE).
              replace(WHEREALL_CLAUSE, StringHelper.emptyString);
  }
  
  
  
  /**
   * Creates a prepared statement.<br>
   * Statements are bound to the db (i.e. connection).
   * If the statement is already prepared, it will not be prepared again,
   * but the ID of the existing statement returned.
   * Statements are unique for sql+resultSetType+resultSetConcurrency.
   * The statement ID is unique among all DB-instances.
   * The ID starts at 1 (not 0).
   *
   * @param sql the SQL string
   * @param resultSetType is one of ResultSet.TYPE_...
   * @param resultSetConcurrency is one of ResultSet.CONCUR_..
   * @return the statement ID
   */
  public int prepareStatement (String sql, int resultSetType, int resultSetConcurrency) {
    assertNotRemote();
    return PooledPreparedStatement.prepareStatement(optimizeSql(sql), resultSetType, resultSetConcurrency);
  }
  
  
  
  /**
   * Creates a prepared statement with ResultSet.CONCUR_READ_ONLY. This is
   * the default for 99% of all applications.
   *
   * @param sql the SQL string
   * @param resultSetType is one of ResultSet.TYPE_...
   * @return statement id
   */
  public int prepareStatement (String sql, int resultSetType) {
    return prepareStatement(sql, resultSetType, ResultSet.CONCUR_READ_ONLY);
  }
  
  
  /**
   * Creates a prepared statement with ResultSet.TYPE_FORWARD_ONLY and ResultSet.CONCUR_READ_ONLY.
   *
   * @param sql the SQL string
   * @return statement id
   */
  public int prepareStatement (String sql) {
    return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
  }
  
  
  
  /**
   * Gets the resultSetType of a prepared statement.
   * Once prepared, you cannot change the resultSetType.
   *
   * @param stmtId the statement id
   * @return the resultset type
   */
  public int getResultSetType(int stmtId)  {
    return PooledPreparedStatement.getStatement(stmtId).getResultSetType();
  }
  
  
  /**
   * Get the resultSetConcurrency of a prepared statement.
   * Once prepared, you cannot change the resultSetConcurrency.
   *
   * @param stmtId the statement id
   * @return the concurrency
   */
  public int getResultSetConcurrency(int stmtId)  {
    return PooledPreparedStatement.getStatement(stmtId).getResultSetConcurrency();
  }
  
  
  /**
   * Gets the sql-string of a prepared statement.
   *
   * @param stmtId the statement id
   * @return the sql string
   */
  public String getSqlText(int stmtId)  {
    return PooledPreparedStatement.getStatement(stmtId).getSql();
  }
  
  
  
  /**
   * Get the ID of a prepared statement by its SQL-string, result set type
   * and concurrency.
   * A convenient way to re-use prepared statements for example in qbf searchs.
   * 
   * @param sql the sql string
   * @param resultSetType the result set type
   * @param resultSetConcurrency the concurrency
   * @return the statement ID or 0 if no such statement
   * @see java.sql.ResultSet
   */
  public int getStatementId(String sql, int resultSetType, int resultSetConcurrency) {
    assertNotRemote();
    return PooledPreparedStatement.getStatementId(optimizeSql(sql), resultSetType, resultSetConcurrency);
  }

  
  /**
   * Get the ID of a prepared statement by its SQL-string and result set type.
   * The concurrency is assumed as {@link ResultSet#CONCUR_READ_ONLY}.
   * 
   * @param sql the sql string
   * @param resultSetType the result set type
   * @return the statement ID or 0 if no such statement
   * @see java.sql.ResultSet
   */
  public int getStatementId(String sql, int resultSetType)  {
    return getStatementId(sql, resultSetType, ResultSet.CONCUR_READ_ONLY);
  }
  
  
  /**
   * Get the ID of a prepared statement by its SQL-string.
   * The result set type is assumed as {@link ResultSet#TYPE_FORWARD_ONLY} 
   * and the concurrency as {@link ResultSet#CONCUR_READ_ONLY}.
   * 
   * @param sql the sql string
   * @return the statement ID or 0 if no such statement
   * @see java.sql.ResultSet
   */
  public int getStatementId(String sql) {
    return getStatementId(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
  }
  
  

  /**
   * Gets the prepared statement.
   * <p>
   * Getting the prepared statement will attach the db to a connection.
   * The db will be detached after executeUpdate() or executeQuery when its
   * result set is closed.
   *
   * @param id the statement id
   *
   * @return the prepared statement for this db
   */
  public PreparedStatementWrapper getPreparedStatement (int id)  {
    assertNotRemote();
    setConnection(attach());
    PreparedStatementWrapper stmt = con.getPreparedStatement(id);
    stmt.markReady(); // mark ready for being used once
    return stmt;
  }
  
  
  
  /**
   * Combination of prepareStatement(sql,...) and getPreparedStatement(stmtId).
   * Often used in searchQbf-methods.
   *
   * @param sql the SQL string
   * @param resultSetType is one of ResultSet.TYPE_...
   * @param resultSetConcurrency is one of ResultSet.CONCUR_..
   * @return the statement
   */
  
  public PreparedStatementWrapper getPreparedStatement (String sql, int resultSetType, int resultSetConcurrency) {
    return getPreparedStatement(prepareStatement(sql, resultSetType, resultSetConcurrency));
  }
  
  
  /**
   * Get prepared statement with ResultSet.CONCUR_READ_ONLY.
   * This is the method most often used in searchQbf!
   *
   * @param sql the SQL string
   * @param resultSetType is one of ResultSet.TYPE_...
   * @return the statement
   */
  public PreparedStatementWrapper getPreparedStatement (String sql, int resultSetType) {
    return getPreparedStatement(prepareStatement(sql, resultSetType));
  }
  
  
  /**
   * Get prepared statement with ResultSet.TYPE_FORWARD_ONLY and ResultSet.CONCUR_READ_ONLY.
   * @param sql the SQL string
   * @return the statement
   */
  public PreparedStatementWrapper getPreparedStatement (String sql) {
    return getPreparedStatement(prepareStatement(sql));
  }
  
  
  

  /**
   * Sets autoCommit feature.
   * <p>
   * The method is package scope and is provided for local db connections only.
   * Applications must use begin/commit instead.
   * Furthermore, the database will be attached and detached.
   * <p>
   * The method differs from the JDBC-method: a commit is *NOT* issued
   * if the autoCommit boolean value wouldn't change.
   * This allows nested setAutoCommit(false) in large transactions.
   *
   * @param autoCommit the new commit value
   * @return the old(!) value of autoCommit
   */
  boolean setAutoCommit (boolean autoCommit) {
    try {
      if (this.autoCommit != autoCommit)  {
        if (autoCommit && sqlRequiresExtraCommit()) {
          // some dbms need a commit before setAutoCommit(true);
          con.commit();   
        }
        if (!autoCommit) {
          // starting a tx
          attach();
        }
        con.setAutoCommit(autoCommit);
        if (autoCommit) {
          // ending a tx
          conMgr.detach(conId);
        }
        this.autoCommit = autoCommit;
        if (DbGlobal.logger.isFinestLoggable()) {
          DbGlobal.logger.finest("physically setAutoCommit(" + autoCommit + ") >>> " + 
                               (autoCommit ? "COMMIT" : "BEGIN") + " TRANSACTION <<<");
        }
        return !autoCommit;     // was toggled
      }
      else  {
        return autoCommit;    // not changed
      }
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, "setAutoCommit failed");
      return false;   // not reached anyway...
    }
  }

  
  /**
   * Gets the current autocommit state.
   * 
   * @return false if in between a transaction, true otherwise
   */
  public boolean isAutoCommit() {
    return autoCommit;
  }

  

  /**
   * Gets the current user info.
   *
   * @return the UserInfo
   */

  public UserInfo getUserInfo() {
    return ui;
  }

  
  /**
   * Sets the userInfo.
   * This will *NOT* send the userinfo to the remote server
   * if this is a remote db.
   *
   * @param ui the userinfo.
   */
  public void setUserInfo (UserInfo ui) {
    this.ui = ui;
    if (ui != null) {
      ui.bindDb(this);
    }
  }


  /**
   * Gets the url if this is a remote connection
   *
   * @return  the url, null if local
   */
  public String getUrl () {
    return url;
  }
  

  /**
   * Gets the driver of the connection.
   *
   * @return the string of the driver, the name of the Db-class for remote connections
   */
  public String getDriver ()  {
    return driver;
  }
  

  
  /**
   * clears the db local data for close/clone
   */
  private static void clearMembers(Db db) {
    if (db.isRemote()) {
      // cloning a remote connection involves creating an entire new connection via open()!
      // notice that a remote db is always open (otherwise it wouldn't be remote ;-))
      db.rdel               = null;
      db.rses               = null;
      db.rcon               = null;
      db.delegates          = null;
      // returning to GC will also GC on server-side (if invoked from close())
    }
    else  {
      db.defaultIdSource     = null;
      db.idSources           = null;
      db.modMap              = null;
      db.commitTxRunnables   = null;
      db.rollbackTxRunnables = null;
      db.modMap              = null;
      db.modificationLogList = null;
    }
    
    db.groupConId  = 0;
    db.conId       = 0;
    
    if (db.ui != null) {
      db.ui.setSince(0);    // not logged in anymore
    }
  }
  
  

  /**
   * Clones a logical connection.
   * <p>
   * Connections may be cloned, which results in a new connection using
   * the cloned userinfo of the original connection. 
   * If the old db is already open, the new db will be opened as well.
   * If the old db is closed, the cloned db will be closed.
   */
  @Override
  public Db clone() {
    
    try {
      
      Db newDb = (Db)super.clone();
      
      clearMembers(newDb);
      
      newDb.instanceNumber = countInstance();
      newDb.clonedFromDb = this;
      
      if (ui != null) { // we need a new UserInfo cause some things may be stored here!
        newDb.setUserInfo(ui.clone());
        newDb.getUserInfo().setSince(System.currentTimeMillis());
      }
      
      if (isRemote()) {
        if (newDb.open() == false)  {
          throw new DbRuntimeException("remote clone failed", newDb.getLoginFailedCause());
        }
      }
      else  {
        if (isOpen())  {
          newDb.conId = conMgr.login(newDb);
          // new connections always start with autocommit on!
          newDb.autoCommit = true;
          newDb.setupIdSource();
        }
      }
      
      if (DbGlobal.logger.isInfoLoggable()) {
        DbGlobal.logger.info("connection '" + newDb + "' cloned from '" + this + "', state=" + (newDb.isOpen() ? "open" : "closed"));
      }
      
      return newDb;
    }
    catch (Exception e)  {
      handleConnectException(e);
      return null;
    }
  }

  
  /**
   * Gets the original db if this db is cloned.
   *
   * @return the orginal db, null if this db is not cloned.
   */
  public Db getClonedFromDb() {
    return clonedFromDb;
  }
  
  
  /**
   * Clears the cloned state.
   * Useful if the information is no longer needed.
   */
  public void clearCloned() {
    this.clonedFromDb = null;
  }
  
  
  /**
   * Gets the cloned state.
   *
   * @return true if this db is cloned
   */
  public boolean isCloned() {
    return clonedFromDb != null;
  }
  
  
  /** 
   * Gets the default fetchsize
   *
   * @return the default fetchSize.
   */
  public int getFetchSize() {
    return fetchSize;
  }  
  
  /**
   * Sets the default fetchsize for all "wrapped" statements
   * (PreparedStatementWrapper and StatementWrapper)
   * 
   * @param fetchSize the new default fetchSize
   *
   */
  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }  
  
  
  /** 
   * gets the maximum number of rows in resultsets.
   *
   * @return the max rows, 0 = no limit
   */
  public int getMaxRows() {
    return maxRows;
  }  
  
  /**
   * sets the maximum number of rows in resultsets.
   * @param maxRows the max rows, 0 = no limit (default)
   *
   */
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }  
  

  /**
   * Gets the type of the logical connection.
   * 
   * @return true if remote, false if local
   */
  public boolean isRemote() {
    return remote;
  }
  
  
  /**
   * asserts that this is not a remote connection
   */
  public void assertNotRemote() {
    if (isRemote()) {
      throw new DbRuntimeException("operation not allowed for remote db connections");
    } 
  }
  
  /**
   * asserts that this is a remote connection
   */
  public void assertRemote() {
    if (!isRemote()) {
      throw new DbRuntimeException("operation not allowed for local db connections");
    } 
  }
  
  
  
  
  /**
   * Prepares a {@link RemoteDelegate}.
   * <p>
   * The delegates for the DbObject-classes "live" in the db, i.e. the remote session.
   * Thus, delegates are unique per class AND db!
   * In order for the DbObject-derived classes to quickly map to the corresponding delegate,
   * the delegates get a unique handle, i.e. an index to an array of delegates, which in
   * turn is unique among ALL sessions.
   *
   * @param clazz is the DbObject-class
   * @return the handle
   */
  public synchronized static int prepareRemoteDelegate (Class clazz) {
    if (remoteClasses == null)  {
      remoteClasses = new Class[16];    // start with a reasonable size
    }
    if (nextDelegateId >= remoteClasses.length) {
      Class[] old = remoteClasses;
      remoteClasses = new Class[old.length << 1];    // double size
      System.arraycopy(old, 0, remoteClasses, 0, old.length);
    }
    remoteClasses[nextDelegateId++] = clazz;
    return nextDelegateId;    // start at 1
  }
  
  
  /**
   * Gets the remote delegate by its id.
   * 
   * @param id is the handle for the delegate
   * @return the delegate for this session
   */
  public RemoteDelegate getRemoteDelegate (int id)  {
    
    assertRemote();        // only allowed on remote connections!
    
    id--; // starting from 0
    
    if (id < 0 || id >= remoteClasses.length) {
      DbGlobal.errorHandler.severe(this, null, "delegate handle out of range");
    }

    // enlarge if necessary
    if (delegates == null) {
      delegates = new RemoteDelegate[remoteClasses.length];
      for (int i=0; i < delegates.length; i++)  {
        delegates[i] = null;
      }      
    }
    if (id >= delegates.length) {
      RemoteDelegate[] old = delegates;
      delegates = new RemoteDelegate[remoteClasses.length];
      for (int i=0; i < old.length; i++)  {
        delegates[i] = old[i];
      }
      // set the rest to null
      for (int i=old.length; i < delegates.length; i++)  {
        delegates[i] = null;
      }
    }

    // check if delegate already fetched from RMI-server
    if (delegates[id] == null) {
      // we need to prepare it
      try {
        RemoteDelegate delegate = rses.getRemoteDelegate(remoteClasses[id].getName(), id);
        delegates[id] = delegate;
        return delegate;
      } 
      catch (Exception e)  {
        DbGlobal.errorHandler.severe(this, e, "can't retrieve delegate");
        return null;
      }
    }
    // already created
    return delegates[id];
  }  
  
  
  
  /** 
   * Checks whether objects are allowed to count modifications.
   * The default is true.
   * 
   * @return true if objects are allowed to count modifications
   */
  public boolean isCountModificationAllowed() {
    return countModificationAllowed;
  }  
  
  
  /** 
   * Defines whether objects are allowed to count modifications.
   * Useful to turn off modcount for a special (temporary) connection doing
   * certain tasks that should not be counted.
   *
   * @param countModificationAllowed true if allowed, false if turned off
   *
   */
  public void setCountModificationAllowed(boolean countModificationAllowed) {
    try {
      if (isRemote()) {
        rdel.setCountModificationAllowed(countModificationAllowed);
      }
      this.countModificationAllowed = countModificationAllowed;
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, "setCountModificationAllowed failed");
    }
  }
  
  
  
  /**
   * Gets the {@link ModificationCounter} for a given tablename.
   * If a counter does not exists, a new one will be created.
   * 
   * @param tableName is the name of the table
   * @return the ModificationCounter object for this table and db
   */
  public synchronized ModificationCounter getModificationCounter(String tableName)  {
    // this is the only method that deals with modMap, so it's sufficient to make
    // the method synchronized
    ModificationCounter mod = null;
    if (modMap == null) {
      modMap = new TreeMap<String,ModificationCounter>();
    }
    else  {
      mod = modMap.get(tableName);
    }
    if (mod == null)  {
      mod = new ModificationCounter(this, tableName);
      modMap.put(tableName, mod);
    }
    return mod;
  }
  
  

  /**
   * Sets the modification allowed state.
   * Useful to turn off modlog for a special (temporary) connection doing
   * certain tasks that should not be logged.
   * The state will be handed over to the remote db-connection as well.
   *
   * @param logModificationAllowed true to allow, false to deny
   */
  public void setLogModificationAllowed(boolean logModificationAllowed) {
    try {
      if (isRemote()) {
        rdel.setLogModificationAllowed(logModificationAllowed);
      }
      this.logModificationAllowed = logModificationAllowed;
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, "setLogModificationAllowed failed");
    }
  }
  
    
  /** 
   * Gets the state of logModificationAllowed.
   *
   * @return true if modification logging is allowed (default).
   */
  public boolean isLogModificationAllowed() {
    return logModificationAllowed;
  }
  
  

  
  /**
   * Sets the modification deferred state.<br>
   * In deferred mode the {@link ModificationLog} objects are not written to the database.
   * Instead they are kept in memory and can be processed later via
   * {@link #getDeferredModificationLogList()}.
   * The state will be handed over to the remote db-connection as well.
   * <p>
   * Note: deferred logs are used to minimize roundtrips in distributed
   * applications. See the PoolKeeper project for more details.
   *
   * @param logModificationDeferred true to allow, false to deny
   */
  public void setLogModificationDeferred(boolean logModificationDeferred) {
    try {
      if (isRemote()) {
        rdel.setLogModificationDeferred(logModificationDeferred);
      }
      this.logModificationDeferred = logModificationDeferred;
      modificationLogList = null;   // clear
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, "setLogModificationDeferred failed");
    }
  }
  
    
  /** 
   * Gets the state for logModificationDeferred.
   *
   * @return true if modification logging is deferred. Default is not deferred.
   */
  public boolean isLogModificationDeferred() {
    return logModificationDeferred;
  }
  
  
  /**
   * Appends a {@link ModificationLog} object to the deferred log list.
   * 
   * @param log the modlog object to append
   */
  public void appendDeferredModificationLog(ModificationLog log) {
    if (modificationLogList == null) {
      modificationLogList = new ArrayList<ModificationLog>();
    }
    modificationLogList.add(log);
  }
  
  
  /**
   * Gets the deferred {@link ModificationLog}s.
   * Upon return the list of deferred logs is always cleared.
   *
   * @return the deferred logs, null = no logs
   */
  public List<ModificationLog> getDeferredModificationLogList() {
    List<ModificationLog> list = modificationLogList;
    if (isRemote()) {
      try {
        list = rdel.getDeferredModificationLogList();
        applyToCollection(this, list);
      }
      catch (Exception e)  {
        DbGlobal.errorHandler.severe(this, e, "getDeferredModificationLogList failed");
      }
    }
    else  {
      modificationLogList = null;
    }
    return list;
  }
  
  
  
  /**
   * Sets the db in a DbObject.<br>
   * The method invokes <tt>obj.setDb()</tt> only if the db really differs.
   * This prevents infinite loops in object circular references.
   * 
   * @param db the db connection 
   * @param obj the database object, null if ignore
   */
  public static void applyToDbObject(Db db, DbObject obj) {
    if (obj != null && obj.getDb() != db)  {
      obj.setDb(db);
    } 
  }
  
  
  /**
   * Sets the context in a collection of DbObjects.
   * 
   * @param db the db connection
   * @param list the collection of database objects
   */
  @SuppressWarnings("unchecked")
  public static void applyToCollection(Db db, Collection<? extends DbObject> list)  {
    if (list != null) {
      for (DbObject obj: list)  {
        applyToDbObject(db, obj);
      }
      if (list instanceof TrackedArrayList) {
        List<DbObject> removedObjects = ((TrackedArrayList<DbObject>)list).getRemovedObjects();
        if (removedObjects != null) {
          for (DbObject obj: removedObjects) {
            applyToDbObject(db, obj);
          }
        }
      }
    }
  }
  
  
  
  /**
   * Gets the default {@link IdSource}.
   *
   * @return the idSource
   */
  public IdSource getDefaultIdSource() {
    return defaultIdSource;
  }  
  
  
  /**
   * Set the default {@link IdSource}.<br>
   * This is the source that is used to generate unique object IDs
   * if classes did not configure their own source.
   * 
   * @param idSource New value of property idSource.
   */
  public void setDefaultIdSource(IdSource idSource) {
    this.defaultIdSource = idSource;
  }
  
  
  
  /**
   * insure that idSources[] is large enough for id
   */
  private void alignIdSources(int id) {
    if (idSources == null || id >= idSources.length) {
      IdSource[] old = idSources;   // save old copy
      int oldLength = old == null ? 0  : old.length;
      int newLength = old == null ? 16 : old.length;    // start with a reasonable size
      while (id >= newLength) {
        newLength <<= 1;    // double size until large enough
      }
      idSources = new IdSource[newLength];
      if (old != null) {
        System.arraycopy(old, 0, idSources, 0, oldLength);
      }
      // initialize the rest with nulls
      for (int i=oldLength; i < newLength; i++) {
        idSources[i] = null;
      }      
    }    
  }
  
  
  
  /**
   * Subclasses get their default ID-Source from the Db-connection.
   * However, the classVariables get initialized only once (when the class is loaded),
   * so multiple db-connections will have to provide their own IdSource PER db!
   * Same trick as with prepared statements: IdSources are mapped by a static index.
   * This method adds an id-source to the db-local pool and returns an index to it.
   *
   * @param idSource source to get a handle for
   * @return the handle
   */
  public int addIdSource(IdSource idSource) {
    alignIdSources(nextIdSourceId);
    idSources[nextIdSourceId++] = idSource;
    return nextIdSourceId;    // start at 1
  }
  
  
  /**
   * Gets the idsource by handle local to this db
   * 
   * @param id handle of the IdSource
   * @return the IdSource or null if not prepared yet for this db-connection
   */
  public IdSource getIdSource(int id) {
    id--;
    return id < 0 || idSources == null || id >= idSources.length ? null : idSources[id];
  }
  
  
  /**
   * Sets the idsource by handle local to this db.
   * Apps need to set this in MT-applications.
   * The idSources[] will be enlarged if not large enough.
   * 
   * @param id handle of the IdSource
   * @param idSource the IdSource or null if not prepared yet for this db-connection
   */
  public void setIdSource(int id, IdSource idSource) {
    id--;
    alignIdSources(id);
    idSources[id] = idSource;
  }
  
  
  
  /**
   * When updates or inserts fail this might be due to a unique voilation.
   * In such a case no exception is thrown but the corresponding methods
   * return false and set the unique violation flag.
   * 
   * @return true if last insert/update returned false due to a unique violation.
   */
  public boolean isUniqueViolation() {
    return uniqueViolation;
  }  
  
  /**
   * Sets the unique violation flag.
   * 
   * @param uniqueViolation true if violation, false if not
   */
  public void setUniqueViolation(boolean uniqueViolation) {
    this.uniqueViolation = uniqueViolation;
  }
  
  

  /**
   * Checks whether unique violations are logged.
   * 
   * @return true if unique violations are logged
   */
  public boolean isUniqueViolationLogEnabled() {
    return logUniqueViolation;
  }

  
  /**
   * By default, unique violations are logged. However,
   * this feature can be turned off (usually temporarily).
   * 
   * @param logUniqueViolation true if unique violations are logged (default)
   */
  public void setUniqueViolationLogEnabled(boolean logUniqueViolation) {
    this.logUniqueViolation = logUniqueViolation;
  }
  
  
  
  /**
   * Returns the current transaction id from the last BEGIN modification log.
   * The tx-ID is only available if logModificationTx is true.
   * 
   * @return the tx ID, 0 if no transaction is pending.
   */
  public long getLogModificationTxId() {
    if (isRemote()) {
      try {
        return rdel.getLogModificationTxId();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(this, e, "getLogModificationTxId failed");
        return 0;   // not reached
      }
    }
    else  {
      return logModificationTxId;
    }
  }
  
  
  /**
   * Sets the transaction id.
   * Normally the tx-ID is derived from the id of the BEGIN-modlog
   * so it's not necessary to invoke this method from an application.
   * (Poolkeeper's replication layer will do so!)
   * 
   * @param logModificationTxId the transaction ID
   */
  public void setLogModificationTxId(long logModificationTxId) {
    if (isRemote()) {
      try {
        rdel.setLogModificationTxId(logModificationTxId);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(this, e, "setLogModificationTxId failed");
      }
    }
    else  {
      this.logModificationTxId = logModificationTxId;
    }
  }
  
  
  /**
   * Gets the value of logModificationTx.
   *
   * @return true if transaction begin/end is logged in the modification log, false if not (default)
   */
  public boolean isLogModificationTxEnabled() {
    return logModificationTxEnabled;
  }  
  
  
  /**
   * Turn transaction-logging on or off. Default is off.<br>
   * With enabled logging the transactions are logged in the {@link ModificationLog}
   * as well.
   *
   * @param logModificationTxEnabled true to turn on transaction logging.
   */
  public void setLogModificationTxEnabled(boolean logModificationTxEnabled) {
    try {
      if (isRemote()) {
        rdel.setLogModificationTxEnabled(logModificationTxEnabled);
      }
      this.logModificationTxEnabled = logModificationTxEnabled;
    }
    catch (Exception e)  {
      DbGlobal.errorHandler.severe(this, e, "setLogModificationTxEnabled failed");
    }
    
  }  
  
  
  /**
   * Gets the name of the effective database user.
   * Nice to tell whether password authentication must be done in application, if any.
   *
   * @return dbUser or null if user is the same as in userInfo.
   */
  public String getDbUser() {
    return dbUser;
  }
  
  
  /**
   * Gets the fixed password
   *
   * @return the fixed password, null if from userinfo
   */
  public char[] getDbPassword() {
    return dbPasswd;
  }

  

  /**
   * Gets the obfuscated login flag (a feature that will be removed soon...)
   * 
   * @return true if login is obfuscated 
   */
  public boolean isObfuscatedLogin() {
    return obfuscatedLogin;
  }


  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is Postgres.
   */
  public boolean isPostgres() {
    return postgres;
  }  
  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is Ingres.
   */
  public boolean isIngres() {
    return ingres;
  }

  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is MySQL.
   */
  public boolean isMysql()  {
    return mysql;
  }
  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is Oracle.
   */
  public boolean isOracle() {
    return oracle;
  }  
  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is Informix.
   */
  public boolean isInformix() {
    return informix;
  }
  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is Microsoft-SQL-Server.
   */
  public boolean isMssql() {
    return mssql;
  }
  
  /**
   * For applications that need to know the kind of database backend.
   * 
   * @return true if the database backend is DB2.
   */
  public boolean isDb2() {
    return db2;
  }


  
  // rules applying to more than one database
  
  
  /**
   * According to the JDBC-specs {@link java.sql.Connection#setAutoCommit}(true)
   * should commit, but some backends require an extra {@link java.sql.Connection#commit}.
   * 
   * @return true if the database needs an extra commit
   */
  public boolean sqlRequiresExtraCommit() {
    return ingres | oracle;
  }
  
  
  /**
   * The top/limit/first clause has to be appended by some dbms and
   * to be prepended by others.
   *
   * @return true if dbms requires the limit clause to be appended, false if prepended
   */
  public boolean sqlRequiresLimitAppended() {
    return mysql | postgres | db2;
  }
  
  
  /**
   * Returns true if limit clause requires a nested select.
   * Notice: oracle needs a special nested select, so you have to explictly
   * check for isOracle()! (see wurblets)
   * 
   * @return true if database requires nested select
   */
  public boolean sqlRequiresNestedLimitSelect() {
    return !mysql && !postgres && !ingres && !informix && !db2 && !mssql;
  }
  
  
  /**
   * Returns the limit clause formatted for the given database.
   * Works for all except oracle.
   *
   * @param par is either a hard-coded parameter or '?' in prepared statements
   * @return the formatted sql string
   */
  public String sqlFormatLimitClause(String par) {
    if (ingres || informix) {
      return " FIRST " + par;
    }
    if (db2) {
      return " FETCH FIRST " + par + " ROWS ONLY";
    }
    if (mssql) {
      return " TOP " + par;
    }
    if (mysql | postgres) {
      return " LIMIT " + par;
    }
    throw new DbRuntimeException("dbms requires nested select for limit clause");
  }
  
  
  /**
   * The offset/skip clause has to be appended by some dbms and
   * to be prepended by others.
   *
   * @return true if dbms requires the limit clause to be appended, false if prepended
   */
  public boolean sqlRequiresOffsetAppended() {
    return mysql | postgres;
  }
  
  /**
   * Returns true if offset clause requires a nested select.
   * Notice: oracle needs a special nested select, so you have to explictly
   * check for isOracle()! (see wurblets)
   * 
   * @return true if database requires nested select 
   */
  public boolean sqlRequiresNestedOffsetSelect() {
    return !mysql && !postgres && !informix;
  }
  
  
  /**
   * Returns the limit clause formatted for the given database.
   * Works for all except oracle.
   *
   * @param par is either a hard-coded parameter or '?' in prepared statements
   * @return the formatted sql string
   */
  public String sqlFormatOffsetClause(String par) {
    if (informix) {
      return " SKIP " + par;
    }
    if (postgres) {
      return " OFFSET " + par;
    }
    if (mysql) {
      return " LIMIT 99999999 OFFSET " + par;
    }
    throw new DbRuntimeException("dbms requires nested select for offset clause");
  }
  
  
  /**
   * The limit + offset/skip clause has to be appended by some dbms and
   * to be prepended by others.
   *
   * @return true if dbms requires the limit clause to be appended, false if prepended
   */
  public boolean sqlRequiresLimitOffsetAppended() {
    return mysql | postgres;
  }
  
  /**
   * The limit + offset/skip clause has to be appended by some dbms and
   * to be prepended by others.
   * 
   * @return true if limit parameter comes first, then the offset
   */
  public boolean sqlRequiresLimitBeforeOffset() {
    return mysql | postgres;
  }
  
  
  /**
   * Returns true if limit+offset clause requires a nested select.
   * Notice: oracle needs a special nested select, so you have to explictly
   * check for isOracle()! (see wurblets)
   * 
   * @return true if database requires nested select
   */
  public boolean sqlRequiresNestedLimitOffsetSelect() {
    return !mysql && !postgres && !informix;
  }
  
  
  /**
   * Returns the limit clause formatted for the given database.
   * Works for all except oracle.
   *
   * @param limitPar the sql string for the limit parameter value
   * @param offsetPar the sql string for the offset parameter value
   * @return the formatted sql string
   */
  public String sqlFormatLimitOffsetClause(String limitPar, String offsetPar) {
    if (informix) {
      return " SKIP " + offsetPar + " FIRST " + limitPar;
    }
    if (mysql | postgres) {
      return " LIMIT " + limitPar + " OFFSET " + offsetPar;
    }
    throw new DbRuntimeException("dbms requires nested select for limit+offset clause");
  }
  
  
  /**
   * @return true if the JDBC driver supports ResultSet.isClosed()
   */
  public boolean sqlResultSetIsClosedSupported() {
    return db2;
  }





  private void assertSqlStartsWithSelect(String sql) {

  }



  /**
   * Adds a limit and offset clause to an SQL statement
   * according to the database backend in use.
   *
   * @param sql the original SQL string buffer
   * @param limit the limit value, &le; 0 if no limit clause
   * @param offset the offset value, &le; 0 if no offset clause
   * @return the modified sql string buffer
   * @see #prependLimitOffsetToPreparedStatement(int, org.tentackle.db.PreparedStatementWrapper, int, int)
   * @see #appendLimitOffsetToPreparedStatement(int, org.tentackle.db.PreparedStatementWrapper, int, int)
   */
  public StringBuilder addLimitOffsetToSql(StringBuilder sql, int limit, int offset) {
    if (limit > 0 || offset > 0) {

      if (sql.length() < 7 || !sql.substring(0, 7).equalsIgnoreCase("SELECT ")) {
        throw new DbRuntimeException("cannot apply limit/offset to statement not beginning with 'SELECT '");
      }

      if (limit > 0 && offset > 0) {
        if (oracle) {
          sql.insert(7, "/*+ FIRST_ROWS */ * FROM (SELECT ");
        }
        else if (sqlRequiresNestedLimitOffsetSelect()) {
          sql.insert(7, "* FROM (SELECT F_O_O.*, ROW_NUMBER() OVER() AS R_O_W FROM (SELECT ");
        }
        else {
          if (!sqlRequiresLimitOffsetAppended()) {
            sql.insert(7, sqlFormatLimitOffsetClause("?", "?") + " ");
          }
        }
        if (oracle) {
          sql.append(") WHERE WHERE ROWNUM > ? AND ROWNUM <= ?");
        }
        else if (sqlRequiresNestedLimitOffsetSelect()) {
          sql.append(") AS F_O_O) AS B_A_R WHERE R_O_W > ? AND R_O_W <= ?");
        }
        else if (sqlRequiresLimitOffsetAppended()) {
          sql.append(sqlFormatLimitOffsetClause("?", "?"));
        }
      }

      else if (limit > 0) {
        if (oracle) {
          sql.insert(7, "/*+ FIRST_ROWS */ * FROM (SELECT ");
        }
        else if (sqlRequiresNestedLimitSelect()) {
          sql.insert(7, "* FROM (SELECT F_O_O.*, ROW_NUMBER() OVER() AS R_O_W FROM (SELECT ");
        }
        else {
          if (!sqlRequiresLimitAppended()) {
            sql.insert(7, sqlFormatLimitClause("?") + " ");
          }
        }
        if (oracle) {
          sql.append(") WHERE ROWNUM <= ?");
        }
        else if (sqlRequiresNestedLimitSelect()) {
          sql.append(") AS F_O_O) AS B_A_R WHERE R_O_W <= ?");
        }
        else if (sqlRequiresLimitAppended()) {
          sql.append(sqlFormatLimitClause("?"));
        }
      }

      else { // offset > 0
        if (oracle) {
          sql.insert(7, "/*+ FIRST_ROWS */ * FROM (SELECT ");
        }
        else if (sqlRequiresNestedOffsetSelect()) {
          sql.insert(7, "* FROM (SELECT F_O_O.*, ROW_NUMBER() OVER() AS R_O_W FROM (SELECT ");
        }
        else {
          if (!sqlRequiresOffsetAppended()) {
            sql.insert(7, sqlFormatOffsetClause("?") + " ");
          }
        }
        if (oracle) {
          sql.append(") WHERE ROWNUM > ?");
        }
        else if (sqlRequiresNestedOffsetSelect()) {
          sql.append(") AS F_O_O) AS B_A_R WHERE R_O_W > ?");
        }
        else if (sqlRequiresOffsetAppended()) {
          sql.append(sqlFormatOffsetClause("?"));
        }
      }
    }

    return sql;
  }
    


  /**
   * Prepends limit and offset parameters to a prepared statement
   * according to the database in use.
   *
   * @param ndx starting parameter index, usually 1
   * @param st the prepared statement
   * @param limit the limit value, &le; 0 if no limit clause
   * @param offset the offset value, &le; 0 if no offset clause
   * @return the possibly changed parameter index
   * @see #addLimitOffsetToSql(java.lang.String, int, int)
   */
  public int prependLimitOffsetToPreparedStatement(int ndx, PreparedStatementWrapper st, int limit, int offset) {

    if (limit > 0 && offset > 0) {
      if (!sqlRequiresNestedLimitOffsetSelect() && !sqlRequiresLimitOffsetAppended()) {
        if (sqlRequiresLimitBeforeOffset()) {
          st.setInt(ndx++, limit);
          st.setInt(ndx++, offset);
        }
        else {
          st.setInt(ndx++, offset);
          st.setInt(ndx++, limit);
        }
      }
    }
    else if (limit > 0) {
      if (!sqlRequiresNestedLimitSelect() && !sqlRequiresLimitAppended()) {
        st.setInt(ndx++, limit);
      }
    }
    else if (offset > 0) {
      if (!sqlRequiresNestedOffsetSelect() && !sqlRequiresOffsetAppended()) {
        st.setInt(ndx++, offset);
      }
    }

    return ndx;
  }


  /**
   * Appends limit and offset parameters to a prepared statement
   * according to the database in use.
   *
   * @param ndx starting parameter index
   * @param st the prepared statement
   * @param limit the limit value, &le; 0 if no limit clause
   * @param offset the offset value, &le; 0 if no offset clause
   * @return the possibly changed parameter index
   * @see #addLimitOffsetToSql(java.lang.String, int, int)
   */
  public int appendLimitOffsetToPreparedStatement(int ndx, PreparedStatementWrapper st, int limit, int offset) {

    if (limit > 0 && offset > 0) {
      if (sqlRequiresNestedLimitOffsetSelect() || sqlRequiresLimitOffsetAppended()) {
        if (sqlRequiresLimitBeforeOffset()) {
          st.setInt(ndx++, limit);
          st.setInt(ndx++, offset);
        }
        else {
          st.setInt(ndx++, offset);
          st.setInt(ndx++, limit);
        }
      }
    }
    else if (limit > 0) {
      if (sqlRequiresNestedLimitSelect() || sqlRequiresLimitAppended()) {
        st.setInt(ndx++, limit);
      }
    }
    else if (offset > 0) {
      if (sqlRequiresNestedOffsetSelect() || sqlRequiresOffsetAppended()) {
        st.setInt(ndx++, offset);
      }
    }

    return ndx;
  }
  


  
  
  /**
   * Creates a simple thread that keeps this db connection "alive", for
   * example while the user is prompted to answer a question from within
   * {@link ModificationThread#runOnce(java.lang.Runnable)}.<br>
   * If remote connections don't receive an alive signal within a given timeout the server
   * usually assumes a dead session and closes it.
   * <pre>
   * Example: 
   *    Thread keepAliveThread = db.createKeepAliveThread(5000);
   *    keepAliveThread.start();
   *    ...
   *    ...
   *    keepAliveThread.interrupt();
   *    keepAliveThread.join();
   * </pre>
   * 
   * @param interval the interval in [ms] to trigger the alive signal
   * @return the thread, not started yet
   */
  public Thread createKeepAliveThread(final long interval) {
    return new Thread() {
      @Override
      public void run() {
        while (!interrupted()) {
          try {
            sleep(interval);
            setAlive(true);
          } 
          catch (InterruptedException ex) {
            break;
          }
        }
      }
    };
  }
  
}
