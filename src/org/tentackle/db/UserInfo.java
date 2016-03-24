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

// $Id: UserInfo.java 461 2009-07-10 16:19:38Z svn $

package org.tentackle.db;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import org.tentackle.util.StringHelper;


/**
 * Information about the user connecting to a database.
 *
 * @author harald
 */
public class UserInfo implements Serializable, Cloneable {

  static final long serialVersionUID = 6002689783347165834L;
  
  private String username;                // name of the user
  private char[] password;                // user's password
  private String application;             // an optional application name
  private long since;                     // logged in since
  private String dbPropertiesName;        // filename of db properties
  private Properties dbProperties;        // db properties (send over via RMI)
  private boolean cloned;                 // true if user info is cloned
  
  
  /**
   * Create a db connection info from a username, password and
   * property file holding the connection parameters.
   * 
   * @param username is the name of the user, null if {@code System.getProperty("user.name")}
   * @param password is the password, null if none
   * @param dbPropertiesName name of the db-properties, null if {@code "Db"}
   */
  public UserInfo (String username, char[] password, String dbPropertiesName)  {
    this.username         = username == null ? System.getProperty("user.name") : username;
    this.password         = password;
    this.dbPropertiesName = dbPropertiesName == null ? "Db" : dbPropertiesName;
  }

  
  
  /**
   * Binds the UserInfo to a logical db connection.
   * For the default UserInfo this method does nothing.
   * However, extended classes might need that, especially
   * when sent over an rmi comlink.
   * 
   * @param db the db connection 
   */
  public void bindDb(Db db) {
    // nothing to do.
  }
  
  
  /**
   * Sets the epochal time when the user logged in.
   * 
   * @param since logged in since
   */
  public void setSince(long since) {
    this.since = since;
  }
  
  /**
   * Gets the time since when logged in.
   * 
   * @return logged in since, null if not logged in
   */
  public long getSince() {
    return since;
  }


  /**
   * Sets the username
   * 
   * @param username the username
   */
  public void setUsername (String username)  {
    this.username = username;
  }
  
  
  /**
   * Gets the username.
   * 
   * @return the username
   */
  public String getUsername ()  {
    return username;
  }

  

  /**
   * Sets the password.
   * 
   * @param password the password
   */
  public void setPassword (char[] password)  {
    this.password = password;
  }

  
  /**
   * Clears the password.
   * Password should be cleared when no more used
   * to remove them physically from memory.
   * This is the reason why we store passwords as a
   * character array and not a string.<p>
   * Sadly enough, the JDBC api requires passwords as strings :-(
   */
  public void clearPassword ()  {
    if (password != null) {
      for (int i=0; i < password.length; ++i) {
        password[i] = 0;
      }
      password = null;
    }
  }

  
  /**
   * Gets the password.
   * 
   * @return the password
   */
  public char[] getPassword ()  {
    return password;
  }

  /**
   * Gets the password as a string.
   * The password is stored as a character array.
   * If the password is null the empty string will be returned
   * bevaise some dbms drivers will nullp otherwise.
   * 
   * @return the password, never null
   */
  public String getPasswordAsString ()  {
    return password == null ? StringHelper.emptyString : new String(password);
  }


  @Override
  public String toString()  {
    return "[" + username + "/" + (password == null ? Locales.bundle.getString("<no_passwd>") : Locales.bundle.getString("<passwd>")) + 
           (application != null ? ("@" + application) : "") + "]";
  }
  
  
  /**
   * Clones a userinfo.
   * The password will be copied if not null.
   */
  @Override
  public UserInfo clone() {
    UserInfo ui;
    try {
      ui = (UserInfo)super.clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new InternalError();    // should never happen
    }
    if (password != null) {
      // physically copy the password (as it might be cleared)
      ui.password = new char[password.length];
      for (int i=0; i < password.length; i++) {
        ui.password[i] = password[i];
      }
    }
    ui.cloned = true; 
    return ui;
  }
  
  
  /**
   * Checks whether this UserInfo is cloned.
   * 
   * @return true if cloned
   */
  public boolean isCloned() {
    return cloned;
  }
  
  /**
   * Cleares the cloned flag.
   * Useful if the userinfo should no longer be treated as cloned.
   */
  public void clearCloned() {
    cloned = false;
  }


  /**
   * Gets the name of the property file.
   * 
   * @return the filename
   */
  public String getDbPropertiesName() {
    return dbPropertiesName;
  }

  
  /**
   * Sets the name of the property file, i.e.
   * without the extension {@code .properties}.
   * 
   * @param dbPropertiesName the filename
   */
  public void setDbPropertiesName(String dbPropertiesName) {
    this.dbPropertiesName = dbPropertiesName;
  }

  
  
  /**
   * Loads the properties
   * according to {@link #getDbPropertiesName()}.
   * <p>
   * Does not {@link #setDbProperties(java.util.Properties)} !
   * 
   * @param asResource true if load from the classpath, false if from filesystem
   * @return the properties
   * @throws FileNotFoundException if no such property file
   * @throws IOException if reading the property file failed
   */
  public Properties loadDbProperties(boolean asResource) throws FileNotFoundException, IOException {
    Properties dbProps = new Properties();
    String filename = getDbPropertiesName();
    if (filename.indexOf('.') < 0) {
      filename += ".properties";
    }
    InputStream is = null;
    try {
      if (asResource) {
        is = getClass().getResourceAsStream(filename);
        if (is == null) {
          // try other variant
          is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
          if (is == null) {
            throw new FileNotFoundException("no such resource");
          }
        }
      }
      else  {
        is = new FileInputStream(filename);
      }
      dbProps.load(is);
    } 
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {}
      }
    }
    return dbProps;
  }
  
  
  
  /**
   * Gets the connection properties.<br>
   * The the properties are not set so far, the method will load
   * the properties file by adding the extension {@code ".properties"}
   * to {@code dbPropertiesName} (if it does not contain an extension already).<br>
   * If there is no such file, the properties will be read as a resource according
   * to the classpath.<br>
   * If all failes the db errorhandler
   * will be invoked (which will usually terminate the application).
   * 
   * @return the connection properties, never null
   */
  public Properties getDbProperties() {
    if (dbProperties == null) {
      try {
        try {
          dbProperties = loadDbProperties(false);
        } 
        catch (FileNotFoundException e1) {
          // try as resource
          try {
            dbProperties = loadDbProperties(true);
          }
          catch (FileNotFoundException e2) {
            DbGlobal.errorHandler.severe(null, "db properties '" + getDbPropertiesName() + "' not found");
          }
        }
      }
      catch (IOException ex) {
        DbGlobal.errorHandler.severe(ex, "reading db properties '" + getDbPropertiesName() + "' failed");
      }      
    }
    return dbProperties;
  }
  
  
  /**
   * Sets the connection properties.
   * 
   * @param dbProperties the connection properties.
   */
  public void setDbProperties(Properties dbProperties) {
    this.dbProperties = dbProperties;
  }
  
  
  
  /**
   * Gets the application name.
   * 
   * @return the name
   */
  public String getApplication() {
    return application;
  }

  
  /**
   * Sets the application name.
   * Application server may need that to classify the client.
   * 
   * @param application the name
   */
  public void setApplication(String application) {
    this.application = application;
  }
  
  
  
  private static final String KEYSTORE = "javax.net.ssl.keyStore";
  private static final String KEYSTOREPASS = "javax.net.ssl.keyStorePassword";
  private static final String TRUSTSTORE = "javax.net.ssl.trustStore";
  private static final String TRUSTSTOREPASS = "javax.net.ssl.trustStorePassword";
  
  /**
   * Configures the SSL system properties according to the settings in the dbProperties file.
   * Needs to be invoked before any method used from javax.net.ssl.*
   *
   * keystore=<keystore>    , default is null if not provided by command line
   * truststore=<truststore>  for client auth, default is null if not provided by command line
   * keystorepassword=<keystore password>, no default if not provided by command line
   * truststorepassword=<truststore password>, no default if not provided by command line
   */
  public void configureSsl() {
    // set ssl system properties before referencing javax.net....
    String keyStore = System.getProperty(KEYSTORE);
    String keyStorePassword = System.getProperty(KEYSTOREPASS);
    String trustStore = System.getProperty(TRUSTSTORE);
    String trustStorePassword = System.getProperty(TRUSTSTOREPASS);

    // override from dbProperties file
    Properties props = getDbProperties();
    String val = props.getProperty("keystore");
    if (val != null)  {
      keyStore = val;
    }
    val = props.getProperty("keystorepassword");
    if (val != null)  {
      keyStorePassword = val;
    }
    val = props.getProperty("truststore");
    if (val != null) {
      trustStore = val;
    }
    val = props.getProperty("truststorepassword");
    if (val != null) {
      trustStorePassword = val;
    }

    // set ssl properties
    if (keyStore != null) {
      System.setProperty(KEYSTORE, keyStore);
    }
    if (keyStorePassword != null) {
      System.setProperty(KEYSTOREPASS, keyStorePassword);
    }
    if (trustStore != null) {
      System.setProperty(TRUSTSTORE, trustStore);
    }
    if (trustStorePassword != null) {
      System.setProperty(TRUSTSTOREPASS, trustStorePassword);
    }
  }
  
  
  
  
  /**
   * use session config (default for all RemoteDelegates)
   */
  public static final int SOCKETCONFIG_SESSION    = 0xFF;
  
  /**
   * plain socket (no compression, no ssl)
   */
  public static final int SOCKETCONFIG_PLAIN      = 0x00;
  
  /**
   * compressed socket (no ssl)
   */
  public static final int SOCKETCONFIG_COMPRESSED = 0x01;
  
  /**
   * ssl socket (no compression)
   */
  public static final int SOCKETCONFIG_SSL        = 0x02;
  
  /**
   * ssl + compression
   */
  public static final int SOCKETCONFIG_COMPRESSED_SSL = SOCKETCONFIG_COMPRESSED | SOCKETCONFIG_SSL;
  
  
  
  /**
   * Gets the socket config from according to given connection properties.
   * 
   * @param props the connection properties
   * @return the socket config (one of SOCKETCONFIG_...)
   */
  public static int getSocketConfig(Properties props) {
    
    int socketConfig = 0;
    
    boolean plain = props.getProperty("uncompressed") != null ||
                    props.getProperty("nossl") != null ||
                    props.getProperty("plain") != null;

    if (props.getProperty("compressed") != null) {
      socketConfig |= SOCKETCONFIG_COMPRESSED;
    }
    if (props.getProperty("ssl") != null) {
      socketConfig |= SOCKETCONFIG_SSL;
    }
    
    if (socketConfig == 0) {
      // neither ssl nor compressed requested
      socketConfig = plain ? SOCKETCONFIG_PLAIN : SOCKETCONFIG_SESSION;
    }
    return socketConfig;
  }
  
  
  /**
   * Gets the socket config from according to the current connection properties.
   * 
   * @return the socket config (one of SOCKETCONFIG_...)
   */
  public int getSocketConfig() {
    return getSocketConfig(getDbProperties());
  }

}
