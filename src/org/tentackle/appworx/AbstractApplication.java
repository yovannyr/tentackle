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

// $Id: AbstractApplication.java 440 2008-09-23 16:28:35Z harald $

package org.tentackle.appworx;

import java.rmi.RemoteException;
import java.util.Properties;
import javax.jnlp.ServiceManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.tentackle.appworx.rmi.AppRemoteDbSession;
import org.tentackle.db.Db;
import org.tentackle.db.DbPreferencesFactory;
import org.tentackle.db.ModificationThread;
import org.tentackle.ui.FormHelper;
import org.tentackle.util.ApplicationException;



/**
 * Common code shared by all different kinds of Tentackle applications,
 * such as {@link Application}, {@link ApplicationServer} or {@link WebApplication}
 * @author harald
 */
public abstract class AbstractApplication {
  
  private static AbstractApplication running = null;   // != null if an application is running in this JVM (classloader)
  
  private Properties  props;      // the application's properties
  private ContextDb   contextDb;  // the server's connection context
  private AppUserInfo userInfo;   // server's user info
  private boolean deployedByEE;   // true if application is running within an EE container
  private boolean deployedByJNLP; // true if application has been started via WebStart

  
  /**
   * Super constructor for all derived classes.
   * Detects whether application is running within EE or JNLP.
   */
  public AbstractApplication() {
    detectEE();
    detectJNLP();
  }
  
  
  /**
   * Registers the application.
   * Makes sure that only one application is running at a time.
   * Should be invoked in the start-method.
   * 
   * @throws ApplicationException if an application is already running.
   */
  protected void register() throws ApplicationException {
    synchronized(AbstractApplication.class) {   // not getClass() because this applies to all kinds of applications!
      if (running != null) {
        throw new ApplicationException("Application " + running + " already running");
      }
      running = this;
    }
  }
  
  /**
   * Unregisters the application.
   * Makes sure that only one application is running at a time.
   * Should be invoked in the stop-method.
   * 
   * @throws ApplicationException if an application is already running.
   */
  protected void unregister() throws ApplicationException {
    synchronized(AbstractApplication.class) {   // not getClass() because this applies to all kinds of applications!
      if (running != this) {
        throw new ApplicationException("Application " + this + " is not running");
      }
      running = null;
    }
  }
  
  
  /**
   * Gets the (singleton) application instance currently running.
   * <p>
   * Notice: the method is not synchronized because no serious application
   * will invoke getApplication() before getting itself up and running.
   * So, we leave off these singleton maniac codings here.
   * 
   * @return the application, null if no application started
   */
  public static AbstractApplication getRunningApplication() {
    return running;
  }
  
  
  
  /**
   * Indicates whether this application is running within an EE-container.
   * 
   * @return true if running in an EE container, false if not
   */
  public boolean isDeployedByEE() {
    return deployedByEE;
  }
  
  
  /**
   * Detects whether this application is running within an EE-container.
   * The result can be retrieved by {@link #isDeployedByEE()}.
   */
  protected void detectEE() {
    deployedByEE = false;
    try {
      new InitialContext().lookup("java:comp/env");
      // the lookup worked, we're in a web container
      deployedByEE = true;
    }
    catch (NamingException ex) {}    
  }
  
  
  /**
   * Indicates whether this application has been started by JNLP (Java Webstart).
   * 
   * @return true if invoked by WebStart, false if not
   */
  public boolean isDeployedByJNLP() {
    return deployedByJNLP;
  }
  
  
  /**
   * Detects whether this application has been started by JNLP (Java Webstart).
   * The result can be retrieved by {@link #isDeployedByJNLP()}.
   */
  protected void detectJNLP() {
    deployedByJNLP = false;
    try {
      ServiceManager.lookup("javax.jnlp.BasicService");
      deployedByJNLP = true;
    }
    catch (Exception ex) {}
    catch (Error err) {}
  }
  
  
  
  /**
   * Sets the properties to configure the application.
   * <p>
   * Must be set before starting the application.<br/>
   * By default the following properties will be honoured:
   * <ul>
   * <li>nodbprefs: if defined disables the configuration of the DbPreferences as the Preferences backing store</li>
   * <li>userprefs: if defined the user-scope preferences are used (with fallback to system-scope if not defined)</li>
   * <li>secman=...: the java runtime security manager if not the default</li>
   * <li>nomodthread: if given the modification thread will not be started (but still created!)</li>
   * <li></li>
   * <li></li>
   * </ul>
   * 
   * @param props the properties to configure the application
   */
  public void setProperties(Properties props) {
    this.props = props;
  }
  
  /**
   * Gets the current properties.
   * 
   * @return the properties
   */
  public Properties getProperties() {
    return props;
  }
  
  
  /**
   * Gets a property.
   * 
   * @param key the property's name
   * @return the value of the key, null if no such property, the empty string if no value for this property.
   */
  public String getProperty(String key) {
    return props == null ? null : props.getProperty(key);
  }
  

  
  
  /**
   * Gets the db connection.
   * 
   * @return the db connection, null if not yet configured
   */
  public Db getDb() {
    return contextDb == null ? null : contextDb.getDb();
  }
  
  
  /**
   * Sets the context db.<br>
   * Some apps change the context during runtime.
   * They should set the context here, whenever changed.
   * 
   * @param contextDb the new context
   */
  public void setContextDb(ContextDb contextDb) {
    this.contextDb = contextDb;
  }
  
  
  /**
   * Gets the server's database context.
   * 
   * @return the database context
   */
  public ContextDb getContextDb() {
    return contextDb;
  }
  
  
  /**
   * Gets the user info.
   * 
   * @return the user info
   */
  public AppUserInfo getUserInfo() {
    return userInfo;
  }
  
  
  /**
   * Sets the user info.
   * Some applications may change the userinfo.
   * 
   * @param userInfo the user info
   */
  public void setUserInfo(AppUserInfo userInfo) {
    this.userInfo = userInfo;
  }
  
  
  /**
   * Updates the user id of the userinfo after logged into a remote server.
   * @throws ApplicationException 
   */
  public void updateUserId() throws ApplicationException {
    AppRemoteDbSession session = (AppRemoteDbSession)contextDb.getDb().getRemoteSession();
    if (session != null) {
      // if remote
      try {
        userInfo.setUserId(session.getUserId());
      }
      catch (RemoteException ex) {
        throw new ApplicationException("cannot retrieve user-ID from server", ex);
      }
    }
  }
  
  

  /**
   * Gets the AppDbObject corresponding to the object-ID of a user entity.
   * <p>
   * Should be overridden if application provides a user entity.
   * The default implementation returns null.
   * @param userId the user id
   * @return the user object, null if unknown
   */
  public AppDbObject getUser(long userId) {
    return null;
  }
  
  
  /**
   * Gets the AppDbObject corresponding to the userId in {@link AppUserInfo}.
   * <p>
   * The default implementation invokes {@link #getUser(long)}.
   * 
   * @return the logged in user object, null if unknown
   */
  public AppDbObject getUser() {
    return getUser(userInfo == null ? 0 : userInfo.getUserId());
  }
  
  
  
  /**
   * Creates a user info.
   * The default implementation creates an AppUserInfo.
   * If the username is null, the current system user will be used.
   * Override this method if the application uses a subclass of AppUserInfo.
   * 
   * @param username is the name of the user, null if {@code System.getProperty("user.name")}
   * @param password is the password, null if none
   * @param dbPropertiesBaseName is the resource bundle basename of the db-property file, null if {@code "Db"}
   * @return the user info
   */
  public AppUserInfo createUserInfo(String username, char[] password, String dbPropertiesBaseName) {
    return new AppUserInfo(username, password, dbPropertiesBaseName);
  }
  
  

 
  /**
   * Creates a db (still closed).
   * The default implementation creates a standard Db.
   * 
   * @param userInfo the user info
   * @return the created Db
   */
  public Db createDb(AppUserInfo userInfo) {
    return new Db(userInfo);
  }
  
  
  /**
   * Creates the contextdb.
   * Override this method if the application uses a subclass of ContextDb.
   * 
   * @param db the database connection
   * @return the db context
   */
  public ContextDb createContextDb(Db db) {
    return new ContextDb(db);
  }
  
  
  
  /**
   * Creates the modification thread ready for being started.
   * The default implementation creates the modthread with a polling interval of 2 seconds
   * which does not clone the db connection.
   * 
   * @return the created modification thread
   */
  public ModificationThread createModificationThread() {
    return ModificationThread.createThread(getContextDb().getDb(), 2000, false);
  }
  
  
  
  
  /**
   * Installs the preferences backend.<br>
   * The default implementation installs the {@link DbPreferencesFactory} unless
   * the property {@code "nodbprefs"} is defined. The server uses the system preferences.
   * The property {@code "userprefs"} forces usage of user preferences (default is system prefs for servers).
   * The preferences are set to be readonly by default.
   */
  protected void installPreferences() {
    // install preferences handler to use the db as backing store
    if (props.getProperty("nodbprefs") == null)  {   // NOI18N
      DbPreferencesFactory.installPreferencesFactory(contextDb.getDb(), true);
    }
    FormHelper.useSystemPreferencesOnly = getProperty("userprefs") == null;   // NOI18N
    FormHelper.preferencesAreReadOnly = getProperty("roprefs") != null;       // NOI18N
  }
  
  
  
  /**
   * installs the tentackle security manager
   */
  protected void installSecurityManager() {
    // create a disabled security manager
    userInfo.setSecurityManager(new SecurityManager());    
  }
  
  
  /**
   * Invoked whenever the security rules change.
   */
  protected void securityRulesChanged() {
    SecurityManager.invalidateAll();
  }
  
  
  
  /**
   * Initializes the application.
   * <p>
   * This is the first step when an application is launched.
   * <p>
   * The default implementation first parses the properties for system properties.
   * System properties start with {@code "SYSTEM_"} followed by the original property name.<br>
   * Then the Java security manager is installed if the property {@code "secman=...."} is given.
   * Overrides should invoke super.doInitialize() and perform any other
   * necessary steps before construction of the GUI.
   * 
   * @throws ApplicationException if initialization failes
   */
  protected void doInitialize() throws ApplicationException {
    // set system properties
    if (props != null) {
      for (String key: props.stringPropertyNames()) {
        if (key.startsWith("SYSTEM_")) {
          System.setProperty(key.substring(7), props.getProperty(key));
        }
      }
    }
    // install optional security manager
    String secman = getProperty("secman");   // NOI18N
    if (secman != null && !secman.isEmpty()) {
      try {
        System.setSecurityManager((java.lang.SecurityManager)(Class.forName(secman).newInstance()));
      } 
      catch (Exception ex) {
        throw new ApplicationException(Locales.bundle.getString("can't_install_java_runtime_security_manager"), ex);
      }
    }
  }
  
  
  
  
  /**
   * Do anything what's necessary after the connection has been established.
   * Setup preferences, etc...
   * The default creates the modification thread (but does not start it),
   * installs the Preferences and tentackle's SecurityManager.
   * 
   * @throws org.tentackle.util.ApplicationException 
   */
  protected void doConfigureApplication() throws ApplicationException {
    createModificationThread();
    installPreferences();
    installSecurityManager();
  }
  
  
  
  
  /**
   * Finishes the startup.<br>
   * The default implementation starts the modification thread, unless
   * the property {@code "nomodthread"} is given.
   * 
   * @throws org.tentackle.util.ApplicationException 
   */
  protected void doFinishStartup() throws ApplicationException {
    
    // register runnable to check for security rules changed
    ModificationThread.getThread().registerTable(
            getUserInfo().getSecurityManager().newSecurityInstance().getTableName(), new Runnable()  {
      public void run() {
        securityRulesChanged();
      }
    });
    
    // start the modification thread
    if (getProperty("nomodthread") == null) {   // NOI18N
      ModificationThread.getThread().start();
    }
    else  {
      AppDbObjectCache.setAllEnabled(false);    // disable caching globally
    }
  }
  

}
