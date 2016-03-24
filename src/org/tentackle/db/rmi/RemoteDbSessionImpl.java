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

// $Id: RemoteDbSessionImpl.java 467 2009-07-25 14:37:06Z harald $

package org.tentackle.db.rmi;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import org.tentackle.db.Db;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.UserInfo;
import org.tentackle.util.Logger.Level;
import static org.tentackle.db.UserInfo.*;



/**
 * User session within the application server.
 *
 * @author  harald
 */
public class RemoteDbSessionImpl extends UnicastRemoteObject
                                 implements RemoteDbSession {
  
  private static final long serialVersionUID = 2805986467738001409L;
  
  
  // ------------------- static section ------------------------
  
  
  /**
   * We keep an internal set of all sessions via WeakReferences, so the
   * sessions still will be finalized when the session isn't used anymore.
   * The set of sessions is used to determine stale sessions, i.e. ones
   * with timed out db-connections. These are typically caused by clients
   * not properly closing the rmi-session.
   */
  protected static final Set<WeakReference<RemoteDbSessionImpl>> sessions = new HashSet<WeakReference<RemoteDbSessionImpl>>();
  
  /**
   * Cleans up the sessions.
   * Finds all db-groups or non-grouped db that are not alive anymore
   * and closes their sessions.
   * If _all_ dbs of a group have timed out, the whole group is closed.
   * Usually the ModificationThread of a client connection will keep up
   * a db-group alive. If the application uses only a single db-session,
   * it must set the db-alive manually.
   */
  public static void cleanupSessions() {
    
    Set<Integer> aliveGroups = new TreeSet<Integer>();   // numbers of db groups that are still alive
    
    for (Iterator<WeakReference<RemoteDbSessionImpl>> iter = sessions.iterator(); iter.hasNext(); ) {
      RemoteDbSessionImpl session = iter.next().get();
      if (session == null) {
        iter.remove();    // was removed by GC: remove it from set too
      }
      else  {
        if (session.isOpen()) {
          int group = session.db.getGroupId();
          if (session.hasTimedOut()) {
            // database has timed out
            if (group == 0) {
              // database without a group: close session immediately
              DbGlobal.logger.info("disconnect dead " + session + ", ungrouped");
              session.cleanup(true);
              session.closeDb();
              iter.remove();
              continue; // don't run into polled() below (would cause a nullp)
            }
            // else: grouped connections are handled below
          }
          else {
            if (group > 0) {
              // belongs to a group and at least one connection still alive
              aliveGroups.add(group);    // append group if not yet done
            }
          }
          session.polled();   // reset alive flag
        }
      }
    }
    
    // close all grouped connections not belonging to alive groups
    for (Iterator<WeakReference<RemoteDbSessionImpl>> iter = sessions.iterator(); iter.hasNext(); ) {
      RemoteDbSessionImpl session = iter.next().get();
      if (session == null) {
        iter.remove();    // was removed by GC: remove it from set too
      }
      else  {
        if (session.isOpen()) {
          int group = session.db.getGroupId();
          // notice: group>0 necessary cause setAlive(false) already applied above
          if (group > 0 && aliveGroups.contains(group) == false) {
            // close the db of the group
            DbGlobal.logger.info("disconnect dead " + session + ", group=" + group);
            session.cleanup(true);
            session.closeDb();
            iter.remove();
          }
        }
      }
    }
  }

  
  /**
   * Thread to find dead sessions
   */
  private static class CleanupThread extends Thread {
    
    private long ms;    // sleep time in ms
    
    public CleanupThread(long ms) {
      this.ms = ms;
      setDaemon(true);    // stop JVM if the last user process terminates
    }
    
    @Override
    public void run() {
      for (;;) {
        try {
          sleep(ms);
          cleanupSessions();
        } 
        catch (InterruptedException ex) {
          // ignore
        }
      }
    }
  }
  
  
  /**
   * Starts the optional cleanup thread that will
   * monitor the sessions for db-activity.
   *
   * @param ms is the interval in ms
   */
  public static void startCleanupThread(long ms) {
    new CleanupThread(ms).start();
  }
  
  
  // each session gets a unique number (only for identification in log files)
  private static int sessionNumber;
  
  private static synchronized int newSessionNumber() {
    return ++sessionNumber;
  }
  
  
  
  

  
  // ------------------- end static section ------------------------
  
  
  private RemoteDbConnectionImpl con;   // the connection object
  private int sessionNo;                // session number
  private int timeout;                  // timeout in polling intervals of the cleanupthread.
  private int timeoutCount;             // consecutive timeouts
  private UserInfo clientInfo;          // saved client info
  private UserInfo serverInfo;          // the server info for creating a new Db connection
  private String clientHost;            // the client host string
  private Db db;                        // the local Db-connection
  private Class[] remoteClasses;        // the classes according to the delegate handle
  
  private int port;                     // default port for all sessions
  private RMIClientSocketFactory csf;   // default client socket factory for all delegates
  private RMIServerSocketFactory ssf;   // default server socket factory for all delegates
  

  
  
  /**
   * Creates a session on a given connection.
   * 
   * @param con the connection
   * @param clientInfo the UserInfo from the client
   * @param serverInfo the UserInfo to establish the connection to the database server
   *
   * @throws RemoteException if the session could not initiated.
   */
  public RemoteDbSessionImpl(RemoteDbConnectionImpl con, UserInfo clientInfo, UserInfo serverInfo) throws RemoteException {
    
    // runs on the same socket as the connection object
    super(con.getPort(), con.getClientSocketFactory(), con.getServerSocketFactory());
    
    this.con = con;
    this.clientInfo = clientInfo;
    this.serverInfo = serverInfo;
    
    port = con.getPort();
    csf  = con.getClientSocketFactory();
    ssf  = con.getServerSocketFactory();
    sessionNo = newSessionNumber();
    
    try {
      
      clientHost = getClientHost();
      
      DbGlobal.logger.info("connect " + this);
      
      db = openDb();
      
      // switch socket factories for delegates if requested by client in dbprops
      int socketConfig = clientInfo.getSocketConfig();
      
      boolean plainRequested = false;
      
      if (socketConfig != SOCKETCONFIG_SESSION) {
        port = 0;   // pick port according to servers config
        if (socketConfig == SOCKETCONFIG_PLAIN) {
          // switch back to system default sockets
          csf = null;
          ssf = null;
          plainRequested = true;          
        }
      }
      
      csf = getClientSocketFactory(socketConfig);
      ssf = getServerSocketFactory(socketConfig);
      
      Properties props = clientInfo.getDbProperties();
      
      // special factories
      String val = props.getProperty("csf");
      if (val != null) {
        // load csf class
        csf = (RMIClientSocketFactory)Class.forName(val).newInstance();
      }
      val = props.getProperty("ssf");
      if (val != null) {
        // load ssf class
        ssf = (RMIServerSocketFactory)Class.forName(val).newInstance();
      }
      
      val = props.getProperty("port");
      // notice: ssl and/or compressed requires another port then the original serverport
      if (val != null) {
        port = Integer.valueOf(val);
      }
      
      // verify port agains fixed ports for sure
      if (plainRequested && csf == null && ssf == null) {
        port = con.getServer().getPlainPort();
      }
      else  {
        port = con.getServer().getPort(port, csf, ssf);
      }

      val = props.getProperty("timeout");
      if (val != null) {
        timeout = Integer.valueOf(val);
      }
      else  {
        timeout = con.getServer().getSessionTimeout();
      }

      // log session params
      DbGlobal.logger.info("Session " + getClass().getName() + " started:" +
                           "\nclient socket factory = " + (csf == null ? "<system default>" : csf.getClass().getName()) +
                           "\nserver socket factory = " + (ssf == null ? "<system default>" : ssf.getClass().getName()) +
                           "\ntcp-port = " + (port == 0 ? "<system default>" : port) + ", timeout=" + timeout);
    }
    catch (Exception ex)  {
      closeDb();
      if (ex instanceof LoginFailedException) {
        throw (LoginFailedException)ex;
      }
      throw new RemoteException("RemoteDbSessionImpl<init> failed", ex); 
    }
    
    // add session to the set
    synchronized(sessions) {
      sessions.add(new WeakReference<RemoteDbSessionImpl>(this));
    }
  }
  
  
  
  /**
   * gets the DbObject class by delegateId.
   * This is not a remote function (only called in server-part of the delegates in case
   * it needs to know the id of a delegate of another class)
   * @param delegateId the class for the delegate id
   * @return the class
   */
  public Class getDbObjectClass(int delegateId) {
    return remoteClasses[delegateId];
  }
  
  
  /**
   * Gets the session db connection
   * @return the db connection
   */
  public Db getDb() {
    return db; 
  }
  
  
  /**
   * Gets the server connection.
   * 
   * @return the connection
   */
  public RemoteDbConnectionImpl getConnection() {
    return con;
  }
  
  
  /**
   * Gets the client user info.
   * 
   * @return the client user info
   */
  public UserInfo getClientUserInfo() {
    return clientInfo;
  }

  
  /**
   * Gets the server user info.
   * 
   * @return the server user info
   */
  public UserInfo getServerUserInfo() {
    return serverInfo;
  }

  
  /**
   * Gets the default port for all delegates.
   * 
   * @return the default port for all delegates
   */
  public int getPort() {
    return port;
  }
  
  
  /**
   * Gets the default client socket factory for all delegates.
   * 
   * @return the default csf for all delegates
   */
  public RMIClientSocketFactory getClientSocketFactory() {
    return csf;
  }
  
  
  /**
   * Gets the default server socket factory for all delegates.
   * 
   * @return the default ssf for all delegates
   */
  public RMIServerSocketFactory getServerSocketFactory() {
    return ssf;
  }
  
  
  /**
   * Gets the predefined port for a given socket type.
   *
   * @param socketConfig is of SOCKETCONFIG_...
   * @return the port
   */
  public int getPort(int socketConfig) {
    if (socketConfig == SOCKETCONFIG_COMPRESSED_SSL) {
      return con.getServer().getCompressedSslPort();
    }
    else if (socketConfig == SOCKETCONFIG_COMPRESSED) {
      return con.getServer().getCompressedPort();
    }
    else if (socketConfig == SOCKETCONFIG_SSL) {
      return con.getServer().getSslPort();
    }
    else if (socketConfig == SOCKETCONFIG_PLAIN) {
      return con.getServer().getPlainPort();
    }
    else  {
      return port;    // session's port
    }
  }
  
  
  /**
   * Gets the csf for a given socket type.
   *
   * @param socketConfig is of SOCKETCONFIG_...
   * @return the default csf for all delegates
   */
  public RMIClientSocketFactory getClientSocketFactory(int socketConfig) {
    if (socketConfig == SOCKETCONFIG_COMPRESSED_SSL) {
      return new CompressedSslClientSocketFactory();
    }
    else if (socketConfig == SOCKETCONFIG_COMPRESSED) {
      return new CompressedClientSocketFactory();
    }
    else if (socketConfig == SOCKETCONFIG_SSL) {
      return new SslRMIClientSocketFactory();
    }
    else if (socketConfig == SOCKETCONFIG_PLAIN)  {
      return null;    // system default (uncompressed, no ssl)
    }
    else  {
      return csf;
    }
  }
  
  
  /**
   * Gets the ssf for a given socket type.
   *
   * @param socketConfig is of SOCKETCONFIG_...
   * @return the default ssf for all delegates
   */
  public RMIServerSocketFactory getServerSocketFactory(int socketConfig) {
    if (socketConfig == SOCKETCONFIG_COMPRESSED_SSL) {
      return new CompressedSslServerSocketFactory(con.getServer().getEnabledCipherSuites(),
                                                  con.getServer().getEnabledProtocols(),
                                                  con.getServer().getNeedClientAuth());
    }
    else if (socketConfig == SOCKETCONFIG_COMPRESSED) {
      return new CompressedServerSocketFactory();
    }
    else if (socketConfig == SOCKETCONFIG_SSL) {
      return new SslRMIServerSocketFactory(con.getServer().getEnabledCipherSuites(),
                                           con.getServer().getEnabledProtocols(),
                                           con.getServer().getNeedClientAuth());
    }
    else if (socketConfig == SOCKETCONFIG_PLAIN) {
      return null;    // system default (uncompressed, no ssl)
    }
    else  {
      return ssf;
    }
  }
  
  
  /**
   * Opens a new Db.
   * The default implementation opens a new Db.
   * Can be overridden if, for example, pools are used instead.
   * @return the db connection
   * @throws LoginFailedException if opening the db failed
   */
  protected Db openDb() throws LoginFailedException {
    if (DbGlobal.serverDbPool != null) {
      // if pooled
      try {
        return DbGlobal.serverDbPool.getDb();
      }
      catch (Exception ex) {
        throw new LoginFailedException("open Db failed", ex);
      }      
    }
    else  {
      Db newDb = new Db(serverInfo);
      if (newDb.open() == false) {
        LoginFailedException e = new LoginFailedException("open Db failed", newDb.getLoginFailedCause());
        newDb = null;
        throw e;
      }
      return newDb;
    }
  }


  /**
   * Cleanup the session.
   * <p>
   * The method is invoked whenever the session is closed
   * due to an ordinary logout or client crash.
   * The default implementation rolls back any pending transaction.
   *
   * @param crashed true if client crashed, else regular logout
   */
  protected void cleanup(boolean crashed) {
    if (db != null && !db.isAutoCommit()) {
      DbGlobal.logger.warning("rolling back transaction " + db.getTxCount() + "/" + db.getTxName());
      db.rollback(true);
    }
  }
  
  
  /**
   * Closes the database connection (and thus rolls back any pending transaction).
   * If the db is pooled, it will be returned to the pool instead of being closed.
   */
  protected void closeDb() {
    if (db != null) {
      if (db.isPooled()) {
        if (db.isAutoCommit() == false) {
          // some transaction running: rollback
          db.rollback(true);
        }
        // only return the db to the pool, don't close it
        db.getPool().putDb(db);
      }
      else  {
        db.close();
      }
      db = null;    // closed -> to GC
    }
  }
  
  
  @Override
  public String toString() {
    return "session=" + sessionNo + ", user=" + clientInfo + ", host=" + clientHost;
  }
  

  /**
   * Determines whether the session is open.
   * 
   * @return true if session is open
   */
  public boolean isOpen() {
    return db != null;
  }


  /**
   * Cleanup in case someone forgot to logoff()
   */
  @Override
  public void finalize()  {
    try {
      close();    // cleanup in case client forgot
    }
    catch (Exception ex)  {
      DbGlobal.logger.log(Level.SEVERE, "finalize close() session failed", ex);
    }
  }


  // ----------------- implements RemoteDbSession ------------------------
  

  public void close() throws RemoteException {
    try {
      if (db != null) {
        if (DbGlobal.logger.isInfoLoggable()) {
          DbGlobal.logger.info("disconnect " + this);
        }
        cleanup(false);
        closeDb();
      }
    }
    catch (Exception ex)  {
      throw new RemoteException("closing db failed", ex); 
    }
  }


  public void log(Level level, String message) throws RemoteException {
    try {
      DbGlobal.logger.log(level, message, null);
    }
    catch (Exception ex)  {
      throw new RemoteException("log() failed", ex);
    }         
  }
  

  public RemoteDelegate getRemoteDelegate(String classname, int delegateId) throws RemoteException  {
    try {
      /**
       * keep a cache of DbObject-Classes for fast instantiation (by delegateId)
       */
      if (remoteClasses == null)  {
        remoteClasses = new Class[16];    // start with a reasonable size
        for (int i=0; i < remoteClasses.length; i++)  {
          remoteClasses[i] = null; 
        }
      }
      // double the size until delegateId fits
      while (remoteClasses.length <= delegateId) {
        Class[] old = remoteClasses;
        remoteClasses = new Class[old.length + old.length];
        for (int i=0; i < old.length; i++)  {
          remoteClasses[i] = old[i];
        }
        // set the rest to null
        for (int i=old.length; i < remoteClasses.length; i++)  {
          remoteClasses[i] = null;
        }        
      }
      
      // remember the class
      Class<?> clazz = Class.forName(classname);
      remoteClasses[delegateId] = clazz;
      Class<?> delegateClazz = null;
      ClassNotFoundException nfe = null;    // first exception thrown
      
      // try to find remote class.
      // Use superclass if no direct implementation found
      for (;;) {
        /** 
         * the name of the delegate is 
         * "<package>.rmi.<classname>RemoteDelegateImpl" (we need the implementation!!!)
         */
        String clazzName = clazz.getName();
        int ndx = clazzName.lastIndexOf('.');
        String pkgName = clazzName.substring(0, ndx);
        String clsName = clazzName.substring(ndx + 1);
      
        try {
          delegateClazz = Class.forName(pkgName + ".rmi." + clsName + "RemoteDelegateImpl");
          break;    // found
        }
        catch (ClassNotFoundException e) {
          if (clazz == Object.class) {
            // abort with first exception thrown
            throw nfe;
          }
          if (nfe == null) {
            nfe = e;    // remember
          }
          // try superclass
          clazz = clazz.getSuperclass();
        }
      }
      // get the constructor with args (RemoteDbSessionImpl session, Class clazz)
      Constructor<?> constructor = delegateClazz.getConstructor(RemoteDbSessionImpl.class, Class.class);
      // create instance of delegate for the session db
      RemoteDelegate delegate = (RemoteDelegate)constructor.newInstance(this, remoteClasses[delegateId]);
      // return reference for delegate to client
      return delegate;    
    }
    catch (Exception ex)  {
      throw new RemoteException("coudn't create delegate for " + classname, ex);
    }            
  }

  
  public DbRemoteDelegate getDbRemoteDelegate() throws RemoteException  {
    try {
      return new DbRemoteDelegateImpl(this);
    }
    catch (Exception ex)  {
      throw new RemoteException("coudn't create delegate for " + db, ex);
    }    
  }






  /**
   * Checks for timeout.
   * Will internally increment a counter until timeout has reached.
   */
  private boolean hasTimedOut() {
    if (db.isAlive()) {
      timeoutCount = 0;
    }
    else  {
      timeoutCount++;
    }
    return timeoutCount > timeout;
  }


  /**
   * Sets this session as being polled for timeout.
   */
  private void polled() {
    db.setAlive(false);
  }
  
}
