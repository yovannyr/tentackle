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

// $Id: ApplicationServer.java 459 2009-07-02 08:25:29Z svn $

package org.tentackle.appworx;

import java.io.FileNotFoundException;
import java.util.Properties;
import org.tentackle.db.ConnectionManager;
import org.tentackle.db.Db;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbPool;
import org.tentackle.db.DefaultDbPool;
import org.tentackle.db.ModificationThread;
import org.tentackle.db.MpxConnectionManager;
import org.tentackle.db.rmi.DbServer;
import org.tentackle.db.rmi.RemoteDbConnectionImpl;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.CommandLine;
import org.tentackle.util.StringHelper;



/**
 * Application Server.
 * 
 * @author harald
 */
public class ApplicationServer extends AbstractApplication {
  
  private Class<? extends RemoteDbConnectionImpl> connectionClass;    // the connection class
  private CommandLine                             cmdLine;            // command line
  private DbServer                                dbServer;           // the RMI server instance
  
  
  /**
   * Creates an instance of an application server.
   *
   * @param connectionClass the class of the connection object to instantiate, null = default or from serverInfo's properties file
   */
  public ApplicationServer(Class<? extends RemoteDbConnectionImpl> connectionClass) {
    this.connectionClass = connectionClass;
  }
  
  
  /**
   * Gets the command line.
   * 
   * @return the command line
   */
  public CommandLine getCommandLine() {
    return cmdLine;
  }
  
  
  /**
   * Gets the RMI server.
   * 
   * @return the RMI dbserver
   */
  public DbServer getDbServer() {
    return dbServer;
  }
  
  
  
  /**
   * Starts the application server.
   *
   * @param args the arguments (usually from commandline)
   */
  public void start(String[] args) {
    
    if (args != null) {
      cmdLine = new CommandLine(args);
      setProperties(cmdLine.getOptionsAsProperties());
    }
    
    try {
      
      // make sure that only one application is running at a time
      register();
      
      // doInitialize environment
      doInitialize();
      
      // login to the database server
      doLogin();
      
      // configure the server
      doConfigureApplication();
      
      // finish startup and start the RMI service
      doFinishStartup();
      
      // start the RMI-server
      doStartDbServer();
    }
    
    catch (Exception e) {
      AppworxGlobal.logger.logStacktrace(e);
      // doStop with error
      doStop(3);
    }
    
  }
  
  
  /**
   * Starts the application server without further arguments.
   */
  public void start() {
    start(null);
  }
  
  
  /**
   * Gracefully terminates the application server.
   */
  public void stop() {
    try {
      unregister();   // not really necessary cause of System.exit in doStop...
      doStop(0);
    } 
    catch (Exception e) {
      AppworxGlobal.logger.logStacktrace(e);
      // doStop with error
      doStop(4);
    }
  }
  
  
  
  
  
  /**
   * Creates the DbServer instance (but does not start it).<br>
   * The default implementation creates a {@link DbServer}.
   * 
   * @param connectionClass the class of the connection object to instantiate, null = default or from serverInfo's properties file
   * @return the created DbServer
   * @throws ApplicationException
   */
  protected DbServer createDbServer(Class<? extends RemoteDbConnectionImpl> connectionClass) throws ApplicationException  {
    return new DbServer(getUserInfo(), connectionClass);
  }
  

  
  /**
   * Creates the connection manager for the client sessions.
   * The default creates an MpxConnectionManager.
   * 
   * @return the connection manager
   */
  public ConnectionManager createConnectionManager() {
    return new MpxConnectionManager(DbGlobal.serverDb);
  }
  
  
  /**
   * Creates the logical DbPool.
   * The default implementation creates a DefaultDbPool.
   * 
   * @return the database pool, null if don't use a pool
   */
  public DbPool createDbPool() {
    return new DefaultDbPool(getUserInfo()) {
      @Override
      protected void closeDb(Db db) {
        super.closeDb(db);
        AppDbObjectCache.removeObjectsForDbInAllCaches(db);
      }
    };
  }
  
  
  

  
  /**
   * Connects the server to the database backend.
   *
   * @throws org.tentackle.util.ApplicationException 
   */
  protected void doLogin() throws ApplicationException {
    
    String username = getProperty("username");   // NOI18N
    char[] password = StringHelper.toCharArray(getProperty("password"));   // NOI18N
    String propName = getProperty("db");         // NOI18N
    
    AppUserInfo userInfo = createUserInfo(username, password, propName);

    // load properties
    Properties dbProps = null;
    try {
      // try from filesystem first
      dbProps = userInfo.loadDbProperties(false);
    }
    catch (FileNotFoundException e1) {
      try {
        // try from classpath
        dbProps = userInfo.loadDbProperties(true);
      }
      catch (FileNotFoundException e2) {
        // neither properties file nor in classpath: set props
        userInfo.setDbProperties(getProperties());
      }
      catch (Exception e3) {
        throw new ApplicationException("loading db-properties from classpath failed", e3);
      }
    }
    catch (Exception e4) {
      throw new ApplicationException("loading db-properties from file failed", e4);
    }

    if (dbProps != null) {
      // merge (local properties override those from file or classpath)
      for (String key: getProperties().stringPropertyNames()) {
        dbProps.setProperty(key, getProperties().getProperty(key));
      }
      userInfo.setDbProperties(dbProps);
    }
    
    userInfo.setApplication(StringHelper.getClassBaseName(getClass()));
    setUserInfo(userInfo);
    
    if (DbGlobal.serverDb != null) {
      throw new ApplicationException("only one server application instance allowed");
    }
    DbGlobal.serverDb = createDb(userInfo);
    
    /**
     * If the db-properties file contained the login data (which is very often the case)
     * copy that login data to the userinfo.
     */
    username = DbGlobal.serverDb.getDbUser();
    if (username != null) {
      userInfo.setUsername(username);
    }
    // password doesn't matter
      
    // open the database connection
    if (DbGlobal.serverDb.open() == false) {
      throw new ApplicationException("open the server database connection failed");
    }
      
    // create the default context
    ContextDb contextDb = createContextDb(DbGlobal.serverDb);
    if (contextDb == null) {
      throw new ApplicationException("creating the database context failed");
    }
    
    setContextDb(contextDb);
  }
  

  
  /**
   * Finishes the startup.<br>
   * The default implementation starts the modification thread, unless
   * {@code "--nomodthread"} given, creates the connection manager, the Db pool,
   * and the DbServer instance.
   * 
   * @throws org.tentackle.util.ApplicationException 
   */
  @Override
  protected void doFinishStartup() throws ApplicationException {
    
    super.doFinishStartup();
    
   // add a shutdown handler in case the modthread terminates unexpectedly
    ModificationThread.getThread().registerShutdownRunnable(new Runnable() {
      public void run() {
        AppworxGlobal.logger.severe("*** emergency shutdown ***");
        stop();
      }
    });
    
    DbGlobal.connectionManager = createConnectionManager();
    DbGlobal.serverDbPool = createDbPool();
    
    dbServer = createDbServer(connectionClass);
  }
  
  
  /**
   * Starts the RMI-server.
   * The default implementation just does {@code dbServer.start()}.
   * @throws ApplicationException 
   */
  protected void doStartDbServer() throws ApplicationException {
    dbServer.start();
  }
  
  
  /**
   * Terminates the application server gracefully.
   * 
   * @param exitValue the doStop value for System.exit()
   */
  protected void doStop(int exitValue) {
    
    // terminate watcher thread
    if (!ModificationThread.getThread().isDummy()) {
      ModificationThread.getThread().terminate();
    }
    
    if (DbGlobal.serverDbPool != null) {
      DbGlobal.serverDbPool.shutdown();
    }
    
    if (DbGlobal.serverDb != null) {
      DbGlobal.serverDb.close();
    }
    
    DbGlobal.connectionManager.shutdown();
    
    try {
      if (dbServer != null) {
        dbServer.stop();
      }
    }
    catch (Exception ex) {
      AppworxGlobal.logger.logStacktrace(ex);
    }
    
    if (!isDeployedByEE()) {
      System.exit(exitValue);
    }
  }
  

}
