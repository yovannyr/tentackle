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

// $Id: Application.java 465 2009-07-19 15:18:09Z harald $

package org.tentackle.appworx;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.tentackle.db.Db;
import org.tentackle.db.DbPreferencesFactory;
import org.tentackle.db.ModificationThread;
import org.tentackle.db.rmi.LoginFailedException;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormInfo;
import org.tentackle.ui.FormWindow;
import org.tentackle.ui.GUIExceptionHandler;
import org.tentackle.print.PrintHelper;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.CommandLine;
import org.tentackle.util.StringHelper;



/**
 * Abstract class to handle the application's lifecycle.
 * Tentackle applications should extend this class and invoke {@link #start}.
 * To shutdown gracefully, application should invokd {@link #stop}.
 * At minimum, the method doCreateWindow() must be implemented.
 * <p>
 * The subclass just needs to provide a main-method, for example:
 * <pre>
 * public static void main(String args[]) {
 *   new MyApplication().start(args);
 * }
 * </pre>
 *
 * @author harald
 */
public abstract class Application extends AbstractApplication {
  
  private String name;                  // the application's name
  private Icon logo;                    // the application's logo icon
  private CommandLine cmdLine;          // command line
  private FormWindow window;            // the application's window
  private LoginDialog loginDialog;      // the doLogin dialog
  
  
  /**
   * Creates an application.
   *
   * @param name the application's name
   * @param logo the application's logo icon
   */
  public Application(String name, Icon logo) {
    this.name = name;
    this.logo = logo;
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
      
      // create the application window
      window = doCreateWindow();
      if (StringHelper.isAllWhitespace(window.getTitle())) {
        window.setTitle(name);
      }
      
      // show the application's window
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          try {
            doShowWindow();
          }
          catch (Exception e) {
            doStop(2);
          }
        }
      });
      
      // wait for application to be shown
      FormHelper.waitForEmptyEventQueue();
      
      // finish startup
      doFinishStartup();
      
    }
    catch (Exception e) {
      // print message to user, if GUI in window, else if headless to console
      FormError.printException (MessageFormat.format(Locales.bundle.getString("launching_{0}_failed"), name), e, false, AppworxGlobal.logger);
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
   * Gets the logo.
   * 
   * @return the logo
   */
  public Icon getLogo() {
    return logo;
  }
  
  
  /**
   * Gets the application's window.
   *
   * @return the window, null if not yet defined.
   */
  public FormWindow getWindow() {
    return window;
  }
  

  /**
   * Sets the look and feel.
   *
   * @param name the name of the look and feel
   */
  public void setLookAndFeel(String name) {
    if (name != null && UIManager.getLookAndFeel().getClass().getName().compareTo(name) != 0)  {
      try {
        UIManager.setLookAndFeel(name);
        PlafGlobal.triggerLookAndFeelUpdated();
        FormHelper.updateUIofAllWindows();
      } 
      catch (UnsupportedLookAndFeelException ex) {
        FormError.print(Locales.bundle.getString("this_look-and-feel_is_not_supported_on_this_computer"));
      } 
      catch (Exception ex) {
        FormError.printException (Locales.bundle.getString("changing_the_look-and-feel_failed"), ex);
      }
    }
  }
  
  
  /**
   * Changes the look and feel.
   * This method should be invoked if the user changed the look and feel intentionally.
   * The default implementation just invokes setLookAndFeel, but applications can override
   * it to save the setting, e.g. in the preferences.
   * @param name the name of the l&f
   */
  public void changeLookAndFeel(String name) {
    setLookAndFeel(name);
  }
  
  
  /**
   * Creates a menu to select the look and feel.
   * The current implementation returns only the tentackle look and feels.
   * 
   * @return a JMenu with all available look and feels
   */
  public JMenu createLookAndFeelMenu() {
    
    final JMenu guiMenu = new JMenu(Locales.bundle.getString("GUI_style_..."));
    
    LookAndFeelInfo[] looks = PlafGlobal.getInstalledTentackleLookAndFeels();
    
    // Menueintraege erzeugen
    if (looks != null)  {
      for (int i=0; i < looks.length; i++)  {
        JRadioButtonMenuItem lnfitem = new JRadioButtonMenuItem (looks[i].getName());
        lnfitem.setActionCommand(looks[i].getClassName());
        lnfitem.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
              changeLookAndFeel(e.getActionCommand());
            }
        });
        guiMenu.add(lnfitem);
      }
      guiMenu.addMenuListener(new MenuListener()  {
        public void menuSelected(MenuEvent e) {
          String name = UIManager.getLookAndFeel().getClass().getName();
          // update radio buttons to show selecion
          Component[] items = guiMenu.getMenuComponents();
          for (int i=0; i < items.length; i++)  {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem)items[i];
            item.setSelected(item.getActionCommand().equals(name));
          }            
        }
        public void menuDeselected(MenuEvent e) {}
        public void menuCanceled(MenuEvent e) {}    
      });
    }
    
    return guiMenu;
  }
  
  
  /**
   * Displays a message during login.
   * If the login dialog is visible, the message will be shown there.
   * Otherwise it will simply print printed to the console.
   * 
   * @param msg the status message
   */
  public void showLoginStatus(String msg) {
    if (loginDialog != null && loginDialog.isShowing()) {
      loginDialog.showStatus(msg);
    }
    else  {
      System.out.println(msg);
    }
  }
  
  
  
  /**
   * Brings up an edit dialog for a given object.
   *
   * @param comp some component of the owner window, null if none
   * @param object the object to create the dialog for
   * @param modal true if modal, else non-modal
   *
   * @return the edited object if modal, else null
   * @throws org.tentackle.util.ApplicationException 
   */
  public AppDbObject showEditDialog (Component comp, AppDbObject object, boolean modal) throws ApplicationException {
    if (modal)  {
      return Hook.hook().editModal(object, comp);
    }
    else  {
      Hook.hook().edit(object, comp, false);
      return null;
    }
  }
  
  
  /**
   * Brings up an edit dialog for a given class.
   *
   * @param comp some component of the owner window, null if none
   * @param clazz is the object's class
   * @param modal true if modal, else non-modal
   *
   * @return the edited object if modal, else null
   * @throws org.tentackle.util.ApplicationException 
   */
  public AppDbObject showEditDialog (Component comp, Class<? extends AppDbObject> clazz, boolean modal) throws ApplicationException {
    try {
      return showEditDialog(comp, AppDbObject.newByClass(getContextDb(), clazz), modal);
    } 
    catch (ApplicationException ex) {
      throw ex;
    } 
    catch (Exception ex) {
      throw new ApplicationException(MessageFormat.format(Locales.bundle.getString("can't_create_database_object_for_class_{0}"), clazz), ex);
    }
  }
  
  
  /**
   * Brings up a non-modal edit dialog for a given class.
   * The owner window is the application's frame.
   * This is the preferred method to be used in edit menus of application frames.
   *
   * @param clazz is the object's class
   */
  public void showEditDialog (Class<? extends AppDbObject> clazz) {
    try {
      showEditDialog((Window)window, clazz, false);
    } 
    catch (ApplicationException ex) {
      FormError.printException(MessageFormat.format(Locales.bundle.getString("cannot_create_edit_dialog_for_class_{0}"), clazz), ex);
    } 
  }
  
  
  /**
   * Brings up a search dialog for a given class.
   * Searchdialogs have no owner but use the owner as the related window.
   *
   * @param w is the related window, null if none
   * @param clazz is the object's class
   */
  public void showSearchDialog (FormWindow w, Class<? extends AppDbObject> clazz)  {
    AppDbObjectSearchDialog sd = AppDbObjectSearchDialog.createAppDbObjectSearchDialog(
            null, getContextDb(), clazz, new Class[] { clazz }, false, false);
    // set the related window
    sd.setRelatedWindow(w);
    sd.showDialog();
  }
  
  
  /**
   * Brings up a search dialog for a given class.
   * The owner is the application frame.
   * This is the preferred method to be used in search menus of application frames.
   *
   * @param clazz is the object's class
   */
  public void showSearchDialog (Class<? extends AppDbObject> clazz)  {
    showSearchDialog(window, clazz);
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
    showLoginStatus(Locales.bundle.getString("installing_preferences..."));
    super.installPreferences();
    FormHelper.useSystemPreferencesOnly = getProperty("sysprefs") != null;   // NOI18N
  }
  
  
  /**
   * Installs the available look and feels.
   * The default implementation installs all Tentackle Plafs.
   */
  protected void installLookAndFeels() {
    // get all supported tentackle look and feels
    showLoginStatus(Locales.bundle.getString("installing_look_and_feels..."));
    PlafGlobal.installTentackleLookAndFeels();    
  }
  
  
  /**
   * installs the tentackle security manager
   */
  @Override
  protected void installSecurityManager() {
    showLoginStatus(Locales.bundle.getString("installing_security_manager_..."));
    // create a disabled security manager
    super.installSecurityManager();
  }
  
  


  /**
   * Connects to the database backend (or application server).
   * <p>
   * Notice: if the application is started via JNLP (Java WebStart) and the
   * commandline option {@code "--db="} is given, it is interpreted as a URL
   * to the db-properties file relative to the JNLP codebase. If it starts with
   * {@code "&lt;protocol&gt;://"} it is taken as an absolute URL.<br>
   * Example:
   * <pre>
   * --db=http://www.tentackle.org/Invoicer/Db.properties
   * 
   * is the same as:
   * 
   * --db=Db.properties
   * 
   * if the codebase is http://www.tentackle.org/Invoicer.
   * </pre>
   *
   * @return the connected context db, null if login aborted or authentication failed
   * @throws org.tentackle.util.ApplicationException 
   */
  protected ContextDb doLogin() throws ApplicationException {
    
    String username     = cmdLine.getOptionValue("username");   // NOI18N
    char[] password     = StringHelper.toCharArray(cmdLine.getOptionValue("password"));   // NOI18N
    String dbPropsName  = cmdLine.getOptionValue("db");         // NOI18N

    AppUserInfo userInfo = createUserInfo(username, password, dbPropsName);
    
    if (isDeployedByJNLP() && dbPropsName != null) {
      // load the db properties relative to the JNLP codebase
      InputStream is = null;
      try {
        URL dbURL;
        if (dbPropsName.indexOf("://") > 0) {
          dbURL = new URL(dbPropsName);
        }
        else  {
          URL codeBase = ((javax.jnlp.BasicService)javax.jnlp.ServiceManager.lookup("javax.jnlp.BasicService")).getCodeBase();
          dbURL = new URL(codeBase + "/" + dbPropsName);
        }
        Properties dbProps = new Properties();
        is = dbURL.openStream();
        dbProps.load(is);
        userInfo.setDbProperties(dbProps);
      }
      catch (MalformedURLException ex) {
        throw new ApplicationException("malformed URL for db properties", ex);
      }
      catch (IOException ex) {
        throw new ApplicationException("loading db properties '" + dbPropsName + "' failed", ex);
      }
      catch (Exception ex) {
        throw new ApplicationException("cannot determine JNLP codebase", ex);
      }
      finally {
        if (is != null) {
          try {
            is.close();
          }
          catch (IOException ex) {}
        }
      }
    }
    
    loginDialog = new LoginDialog(userInfo, logo);
    ContextDb contextDb = null;
    Db db = null;
    int retry = 4;
    
    while (--retry >= 0) {
      
      if (username == null || password == null) {
        userInfo = loginDialog.getUserInfo();
        if (userInfo == null) {
          return null;
        }
      }
      else  {
        loginDialog.setVisible(true);   // show only
        userInfo = createUserInfo(username, password, dbPropsName);
      }
    
      loginDialog.showStatus(Locales.bundle.getString("connecting_to_server..."));
      
      userInfo.setApplication(StringHelper.getClassBaseName(getClass()));
      setUserInfo(userInfo);
      
      db = createDb(userInfo);
      
      // open the database connection
      if (db.open() == false) {
        String msg;
        if      (retry == 2)    {
          msg = Locales.bundle.getString("Login_failed!_(2_more_retries)");
        }
        else if (retry == 1)    {
          msg = Locales.bundle.getString("Login_failed!_(last_retry)");
        }
        else                    {
          msg = Locales.bundle.getString("Login_failed!");
        }
        userInfo.setPassword(null);
        if (db.getLoginFailedCause() instanceof LoginFailedException) {
          msg = db.getLoginFailedCause().getMessage();
        }
        showLoginStatus(msg);
        continue;
      }
      
      // create the default context
      contextDb = createContextDb(db);
      if (contextDb != null)  {
        break;    // login successful
      }

      // next round
      db.clearPassword();
      db.close();
    }
    
    if (retry < 0) {
      FormInfo.print(Locales.bundle.getString("Login_refused!_Please_check_your_username_and_password"));
      loginDialog.dispose();
      contextDb = null;
    }
    
    setContextDb(contextDb);
    
    updateUserId();
    
    return contextDb;
  }
  
  
  /**
   * Do anything what's necessary after the connection has been established.
   * Setup preferences, etc...
   * The default creates the modification thread (but does not start it),
   * installs the Preferences, tentackle's SecurityManager and the look and feels.
   * 
   * @throws org.tentackle.util.ApplicationException 
   */
  @Override
  protected void doConfigureApplication() throws ApplicationException {
    super.doConfigureApplication();
    installLookAndFeels();
  }
  
  
  /**
   * Creates the top level window.
   * The method must not setVisible(true)!
   *
   * @return the toplevel window, ready for showing
   * @throws org.tentackle.util.ApplicationException 
   */
  protected abstract FormWindow doCreateWindow() throws ApplicationException;
  
  
  /**
   * Shows the window.
   * The default implementation invokes setVisible(true).
   * If the window is a FormContainer, setFormValue() and saveValues() will be invoked as well.
   * @throws org.tentackle.util.ApplicationException 
   */
  protected void doShowWindow() throws ApplicationException {
    FormHelper.getEventQueue();   // initialize EventQueue
    window.setFormValues();
    window.saveValues();
    ((Window)window).setVisible(true);
    if (loginDialog.isShowing()) {
      loginDialog.dispose();
    }
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
    
    // instructs the PrintHelper to turn off mod thread while printing
    PrintHelper.setRunBeforePrint(new Runnable()  {
      public void run() {
        ModificationThread.getThread().setIdle(true);
      }
    });
    PrintHelper.setRunAfterPrint(new Runnable()  {
      public void run() {
        ModificationThread.getThread().setIdle(false);
      }
    });
    
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
    
    // dispose the main window if not yet done
    if (window != null && ((Window)window).isShowing()) {
      ((Window)window).dispose();
    }
    
    // close all AppDbObjectDialogs (clears editedBy/Since as well)
    Hook.hook().getDialogPool().disposeAllDialogs();
    
    // run final closing of dialogs. if any
    GUIExceptionHandler.runSaveState();
    
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
