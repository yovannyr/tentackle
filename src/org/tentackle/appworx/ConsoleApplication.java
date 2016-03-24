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

// $Id: ConsoleApplication.java 439 2008-09-18 15:09:32Z harald $

package org.tentackle.appworx;

import org.tentackle.db.Db;
import org.tentackle.db.DbPreferencesFactory;
import org.tentackle.db.ModificationThread;
import org.tentackle.db.rmi.LoginFailedException;
import org.tentackle.ui.FormHelper;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.CommandLine;
import org.tentackle.util.StringHelper;



/**
 * Abstract class to handle the application's lifecycle for a console application (no gui).
 * Tentackle applications should extend this class and invoke {@link #start}.
 * To shutdown gracefully, application should invokd {@link #stop}.
 * <p>
 * The subclass just needs to provide a main-method, for example:
 * <pre>
 *  MyApplication app = new MyApplication();
 *  app.start(args);
 *  
 *  // do something...
 *  
 *  app.stop();
 * </pre>
 *
 * @author harald
 */
public abstract class ConsoleApplication extends AbstractApplication {
  
  private String name;                  // the application's name
  private CommandLine cmdLine;          // command line
  
  
  /**
   * Creates an application.
   *
   * @param name the application's name
   */
  public ConsoleApplication(String name) {
    this.name = name;
  }
  
  
  
  /**
   * Launches the application.
   *
   * @param args the arguments (usually from commandline)
   */
  public void start(String[] args) {
    
    cmdLine = new CommandLine(args);
    setProperties(cmdLine.getOptionsAsProperties());
    
    try {
      
      // make sure that only one application is running at a time
      register();
      
      // doInitialize environment
      doInitialize();           
      
      // connect to database/application server
      if (doLogin() == null) {
        // no connection, doStop immediately
        System.exit(1);     
      }
      
      // configure the application
      doConfigureApplication();
      
      // finish startup
      doFinishStartup();
      
    }
    catch (Exception e) {
      // print message to user, if GUI in window, else if headless to console
      AppworxGlobal.logger.logStacktrace(e);
      // doStop with error
      doStop(3);
    }
  }
  
  
  /**
   * Gracefully terminates the application.
   * Usually invoked from an exit-Button or when window is closed.
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
   * Gets the application's name.
   * @return the name 
   */
  @Override
  public String toString() {
    return name;
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
   * Gets the application name.
   * 
   * @return the application name
   */
  public String getName() {
    return name;
  }
  
  
  /**
   * Creates the modification thread ready for being started.
   * The default implementation creates the modthread with a polling interval of 2 seconds
   * and which runs on a cloned db-connection.
   */
  @Override
  public ModificationThread createModificationThread() {
    return ModificationThread.createThread(getContextDb().getDb(), 2000);
  }
  
  
  /**
   * Installs the preferences backend.<br>
   * The default implementation installs the {@link DbPreferencesFactory} unless
   * {@code "--nodbprefs"} is given.
   * The option {@code "--sysprefs"} forces usage of system preferences only.
   * {@code "--roprefs"} sets the preferences to readonly.
   */
  @Override
  protected void installPreferences() {
    super.installPreferences();
    FormHelper.useSystemPreferencesOnly = getProperty("sysprefs") != null;   // NOI18N
  }
  
  
  /**
   * installs the tentackle security manager
   */
  @Override
  protected void installSecurityManager() {
    // create a disabled security manager
    super.installSecurityManager();
  }
  
  


  /**
   * Connects to the database backend (or application server).
   *
   * @return the connected context db, null if login aborted or authentication failed
   * @throws org.tentackle.util.ApplicationException 
   */
  protected ContextDb doLogin() throws ApplicationException {
    
    String username     = cmdLine.getOptionValue("username");   // NOI18N
    char[] password     = StringHelper.toCharArray(cmdLine.getOptionValue("password"));   // NOI18N
    String dbPropsName  = cmdLine.getOptionValue("db");         // NOI18N

    AppUserInfo userInfo = createUserInfo(username, password, dbPropsName);
    userInfo.setApplication(StringHelper.getClassBaseName(getClass()));
    setUserInfo(userInfo);
    Db db = createDb(userInfo);
    
    // open the database connection
    if (db.open() == false) {
      String msg = null;
      if (db.getLoginFailedCause() instanceof LoginFailedException) {
        throw new ApplicationException("login failed", db.getLoginFailedCause());
      }
    }

    // create the default context
    ContextDb contextDb = createContextDb(db);
    if (contextDb == null)  {
      throw new ApplicationException("login refused");
    }
    setContextDb(contextDb);
    
    updateUserId();
    
    return contextDb;
  }
  
  
  
  /**
   * Finishes the startup.<br>
   * Invoked after all has been displayed.
   * The default implementation starts the modification thread, unless
   * {@code "--nomodthread"} given.
   * 
   * @throws org.tentackle.util.ApplicationException 
   */
  @Override
  protected void doFinishStartup() throws ApplicationException {
    
    // add a shutdown handler in case the modthread terminates unexpectedly
    ModificationThread.getThread().registerShutdownRunnable(new Runnable() {
      public void run() {
        AppworxGlobal.logger.severe("*** emergency shutdown ***");
        stop();
      }
    });
    
    super.doFinishStartup();
  }
  
  
  /**
   * Terminates the application gracefully.
   * (this is the only do.. method that does not throw ApplicationException)
   * 
   * @param exitValue the doStop value for System.exit()
   */
  protected void doStop(int exitValue) {
    
    // terminate watcher thread
    if (!ModificationThread.getThread().isDummy()) {
      ModificationThread.getThread().terminate(); // this will also close the threads local db-connection
    }
    
    // close db
    ContextDb contextDb = getContextDb();
    if (contextDb != null) {
      Db db = contextDb.getDb();
      if (db != null) {
        db.close();
      }    
    }
    
    // terminate runtime
    System.exit(exitValue);
  }

}
