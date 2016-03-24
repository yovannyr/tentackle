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

// $Id: WebApplication.java 439 2008-09-18 15:09:32Z harald $

package org.tentackle.appworx;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.tentackle.db.ConnectionManager;
import org.tentackle.db.Db;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbPool;
import org.tentackle.db.DefaultDbPool;
import org.tentackle.db.ModificationThread;
import org.tentackle.db.MpxConnectionManager;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.StringHelper;

/**
 * Web Application.
 * <p>
 * Web applications usually run in a container such as glassfish or
 * in case of JRuby/Rails in a pack of mongrels or a single webrick.
 * Because we cannot make any assumptions about the threading model
 * (single thread/single instance/single jvm as with JRuby/Webrick or
 * full-blown multithreaded as in glassfish or half-way in between
 * like JRuby with mongrel_jcluster) the WebApplication provides a
 * logical Db-pool on top of multiplexed physical database connections.
 * The initial size of the logical Db pool is 2 on 1 physical connection.
 * This will not waste resources in single thread per JVM setups
 * (like Rails/Webrick) but will dynamically grow otherwise.
 * <p>
 * Web applications need some mapping between sessions and
 * {@link AppUserInfo}-objects that in turn carry the user id
 * and a {@link SecurityManager}. For the time of a web-roundtrip
 * a logical Db is picked from the pool and associated with that
 * user info (i.e. "session"). Notice that there may be more than
 * one session per user (but each gets its own user info). That's
 * why user infos are mapped by a session key object and not by
 * the user id. Furthermore, depending on the container's session
 * model, it is not sure that the container's session carries
 * the user id at all.
 *
 * @author harald
 */
public class WebApplication extends AbstractApplication {
  
  /**
   * Gets the current web application instance.<p>
   * 
   * This is just a convenience method to {@link AbstractApplication#getRunningApplication()}.
   * 
   * @return the application, null if not started yet
   */
  public static WebApplication getWebApplication() {
    return (WebApplication)getRunningApplication();
  }
  
  

  
  /**
   * Gets the current web application instance or
   * starts a new instance if not already running.
   * <p>
   * Web containers should use this method to make sure
   * that only one instance is started per JVM/classloader-context.
   * <p>
   * If the application runs in a container (tomcat, glassfish, etc...) additional
   * properties may be appended/overwritten to/in the given <tt>props</tt>-argument.
   * In <tt>web.xml</tt> add the following lines:
   * <pre>
       &lt;env-entry&gt;
          &lt;env-entry-name&gt;tentackle.properties&lt;/env-entry-name&gt;
          &lt;env-entry-value&gt;../myapplication.properties&lt;/env-entry-value&gt;
          &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
        &lt;/env-entry&gt; 
   * </pre>
   * 
   * This will load the properties-file <tt>myapplication.properties</tt> from
   * the <tt>WEB-INF</tt>-directory (without <tt>"../"</tt> from <tt>WEB-INF/classes</tt>)
   * and modify the properties given by <tt>props</tt>.
   * 
   * @param clazz the application class
   * @param props the properties to configure the application
   * @return the application
   * @throws ApplicationException if failed
   */
  public static WebApplication getInstance(Class<? extends WebApplication> clazz, Properties props) 
         throws ApplicationException {
    synchronized(AbstractApplication.class) {
      WebApplication app = getWebApplication();
      if (app == null) {
        try {
          // create new instance
          app = clazz.newInstance();
          if (app.isDeployedByEE()) {
            // if in EE container: add extra properties
            Context env = (Context) new InitialContext().lookup("java:comp/env"); // must work!
            try {
              // find <env-entry> for tentackle.properties in web.xml
              String filename = (String) env.lookup("tentackle.properties");
              InputStream is = null;
              try {
                // load relative to WEB-INF/classes
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
                Properties extraProps = new Properties();
                extraProps.load(is);
                // set the properties
                for (String key: extraProps.stringPropertyNames()) {
                  props.setProperty(key, extraProps.getProperty(key));
                }
              } 
              finally {
                if (is != null) {
                  is.close();
                }
              }
            }
            catch (Exception ex) {
              // no extra properties
            }
          }
          
          // start the app
          app.start(props);
        }
        
        catch (Exception ex) {
          AppworxGlobal.logger.logStacktrace(ex);
          if (ex instanceof ApplicationException) {
            throw (ApplicationException)ex;
          }
          if (app != null) {
            app.doStop();
          }
          throw new ApplicationException("creating application instance failed: " + ex.getMessage(), ex);
        }
      }
      return app;
    }
  }
  
  
  // the user info map, key is the session-key
  private ConcurrentHashMap<Object,SoftReference<AppUserInfo>> userInfoMap;    
  private CleanupThread cleanupThread;
  
  
  // thread to clean up weak references
  private class CleanupThread extends Thread {
    
    private long ms;
    private boolean stop;
    
    CleanupThread(long ms) {
      this.ms = ms;
      setDaemon(true);
    }
    
    void requestToStop()  {
      stop = true;
      interrupt();
    }
    
    @Override
    public void run() {
      while (!interrupted() && !stop) {
        try {
          sleep(ms);
          for (Iterator<SoftReference<AppUserInfo>> iter = userInfoMap.values().iterator(); iter.hasNext(); ) {
            SoftReference<AppUserInfo> ref = iter.next();
            if (ref.get() == null) {
              iter.remove();
            }
          }
        } 
        catch (InterruptedException ex) {
          if (stop) {
            break;
          }
          // continue
        }
      }
    }
  }
  
  
  
  /**
   * Creates an instance of a web application.
   */
  public WebApplication() {
    // something to do?
    super();
  }
  
  
  /**
   * Adds a mapping between a session and a user info.
   * This is usually done in the login controller.
   * If a session with that key already exists, the user info
   * will be replaced.
   * 
   * @param sessionKey the (unique) session key
   * @param userInfo the user info
   */
  public void addSession(Object sessionKey, AppUserInfo userInfo) {
    userInfoMap.put(sessionKey, new SoftReference<AppUserInfo>(userInfo));
  }
  
  
  /**
   * Removes a mapping between a session and a user info.
   * This is usually done in the logout controller.
   * If there is no such session, the method will do nothing.
   * 
   * @param sessionKey the (unique) session key
   */
  public void removeSession(Object sessionKey) {
    userInfoMap.remove(sessionKey);
  }
  
  

  
  /**
   * Gets a logical db connection by a session key.
   * 
   * @param sessionKey the session key
   * @return the attached Db or null if no such session
   */
  public Db getDb(Object sessionKey) {
    SoftReference<AppUserInfo> userInfoRef = userInfoMap.get(sessionKey);
    if (userInfoRef != null) {
      AppUserInfo userInfo = userInfoRef.get();
      if (userInfo != null) {
        // not cleared so far: we can use it
        Db db = DbGlobal.serverDbPool.getDb();
        db.setUserInfo(userInfo); // attach userinfo
        return db;
      }
    }
    return null;
  }
  
  
  /**
   * Release a logical db connection.
   * Should be invoked after sending/rendering the response to the web browser.
   * @param db the db to release
   */
  public void putDb(Db db) {
    /**
     * dereference user info (possible target for the GC).
     * If (due to tight memory or timeout) the userinfo gets garbage
     * collected the user must login again.
     */
    db.setUserInfo(null);   
    DbGlobal.serverDbPool.putDb(db);
  }
  
  
  
  

  /**
   * Starts the application.
   *
   * @param props the properties to configure the application
   * @throws ApplicationException if startup failed
   * @see AbstractApplication#setProperties(java.util.Properties)
   */
  public void start(Properties props) throws ApplicationException {
    
    userInfoMap = new ConcurrentHashMap<Object,SoftReference<AppUserInfo>>();
    
    setProperties(props);
    
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
    
  }
  
  
  
  /**
   * Gracefully terminates the web application server.
   */
  public void stop() {
    try {
      unregister();
    } 
    catch (ApplicationException e) {
      AppworxGlobal.logger.logStacktrace(e);
      // doStop with error
    }
    doStop();
  }
  
  
  
  
  /**
   * Creates the connection manager for the client sessions.
   * The default creates an MpxConnectionManager with initially 1 open
   * connection, max 100.
   * 
   * @return the connection manager
   */
  public ConnectionManager createConnectionManager() {
    return new MpxConnectionManager("web", DbGlobal.serverDb, 100, DbGlobal.serverDb.getConnectionId() + 1, 
                                    1, 1, 1, 100, 720, 2160);
  }
  
  
  
  /**
   * Creates the logical DbPool.
   * The default implementation creates a DefaultDbPool with a
   * 2 pre-opened Db, increment by 1, don't drop below 2.
   * The maximum number of Db instances is derived from the
   * connection manager. Because of the nature of web applications,
   * it doesn't make sense to allow more Db instances than connections
   * (web sessions are short lived as opposed to desktop sessions).
   * 
   * @return the database pool, null if don't use a pool
   */
  public DbPool createDbPool() {
    return new DefaultDbPool("web", DbGlobal.connectionManager, 
                             getUserInfo(), 2, 1, 2, DbGlobal.connectionManager.getMaxLogins(), 60) {
      @Override
      protected void closeDb(Db db) {
        super.closeDb(db);
        AppDbObjectCache.removeObjectsForDbInAllCaches(db);
      }
    };
  }
  
  

  
  /**
   * Connects the server to the database backend.
   * The database properties may be either given by the "db" property
   * or if this is missing the application's properties will be used.
   *
   * @throws org.tentackle.util.ApplicationException 
   */
  protected void doLogin() throws ApplicationException {
    
    String username = getProperty("username");   // NOI18N
    char[] password = StringHelper.toCharArray(getProperty("password"));   // NOI18N
    String dbProps  = getProperty("db");         // NOI18N
    
    AppUserInfo userInfo = createUserInfo(username, password, dbProps);
    if (dbProps == null || dbProps.isEmpty()) {
      // get the connection properties from the local properties
      userInfo.setDbProperties(getProperties());
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
      throw new ApplicationException("open the database connection failed");
    }

    // create the default context
    ContextDb contextDb = createContextDb(DbGlobal.serverDb);
    if (contextDb == null) {
      throw new ApplicationException("creating the database context failed");
    }

    setContextDb(contextDb);
    
    updateUserId();
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
    
    // start the session pool cleanup thread
    cleanupThread = new CleanupThread(300000);    // 5 minutes
    cleanupThread.start();
  }
  
  
  /**
   * Terminates the application gracefully.
   */
  protected void doStop() {
    
    // terminate watcher thread
    if (!ModificationThread.getThread().isDummy()) {
      ModificationThread.getThread().terminate();
    }
    
    if (cleanupThread != null) {
      cleanupThread.requestToStop();
      try {
        cleanupThread.join();
      } 
      catch (InterruptedException ex) {
        // nothing we can do...
      }
      cleanupThread = null;
    }
    
    if (DbGlobal.serverDbPool != null) {
      DbGlobal.serverDbPool.shutdown();
      DbGlobal.serverDbPool = null;
    }

    if (DbGlobal.serverDb != null) {
      DbGlobal.serverDb.close();
      DbGlobal.serverDb = null;
    }

    DbGlobal.connectionManager.shutdown();
    
    userInfoMap = null;
  }

}
