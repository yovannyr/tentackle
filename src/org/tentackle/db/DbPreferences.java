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

// $Id: DbPreferences.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import org.tentackle.util.Base64;
import org.tentackle.util.Compare;
import org.tentackle.util.PreferencesSupport;



/**
 * Database backend for the {@link Preferences} API.<br>
 * 
 * Tentackle preferences are stored in a database rather than a file(unix) or registry(windows).
 * The persistent classes are:
 * <ol>
 * <li>{@link DbPreferencesNode}: the nodes representing the preferences hierarchy.</li>
 * <li>{@link DbPreferencesKey}: the key/value pairs corresponding to the nodes.</li>
 * </ol>
 * 
 * Unlike the default JDK-backends, tentackle preferences are synchronized between
 * JVMs, i.e. a (flushed) change in one JVM will show up in all other JVMs. 
 * As a consequence, NodeChangeEvents and PreferenceChangeEvents work 
 * across JVM-boundaries. 
 * The synchronization is based on the {@link ModificationThread} by use of the tableSerial.
 * Notice that events in the local JVM are enqueued at the time of change (i.e. put(), 
 * remove(), node() or removeNode()) while other JVMs will become aware of the changes
 * only after the changes are flush()ed to persistent storage.
 * The underlying mechanism works as follows:
 * <ul>
 * <li>a key is modified: the corresponding key is updated to the db and all other JVMs will identify
 *   this by its tableSerial. If the corresponding key is already loaded in other JVMs, it is reloaded
 *   from the db and the optional PreferenceChangeListeners invoked for its node.
 * </li>
 * <li>a key is added or removed: the key is deleted from or added to the db and its node is updated allowing
 *   other JVMs to detect the node by its tableSerial and to update its keys. 
 *   If PreferenceChangeListeners are registered, an Event will be triggered.
 * </li>
 * <li>a node is added or removed: the node is inserted into or removed from the db and its parent is
 *   updated to allow detection by its tableserial. NodeChangeEvents are triggered accordingly.
 *   The fact that "some" nodes have been deleted is detected by gaps in the retrieved tableSerials
 *   (see {@link ModificationCounter} for details).
 * </li>
 * </ul>
 * 
 * Compared to {@link java.util.prefs.AbstractPreferences} (which we can not simply extend 
 * due to various design issues), the DbPreferences differ slightly in the following aspects:
 * <ul>
 * <li>PreferenceChangeEvents are only triggered if there really is a change. Thus, invoking put() without
 *   an effective change will *not* trigger an event.
 * </li>
 * <li>there is only little difference between syncObject() and flush() if autoSync=true, because a syncObject 
 *   is automatically performed "in background" by the ModificationThread.
 *   If autoSync=false, syncObject() has to be invoked explicitly to reflect changes made by other JVMs.
 *   AutoSync is the recommended mode for most applications.
 * </li>
 * <li>nodeExists() only returns true, if the node is persistent (regardless whether we accessed it yet
 *   or created by another JVM). However, the semantics of nodeExists("") remain, i.e. "return true if not deleted".
 *   Thus, nodeExists("") == false just means that the node has been removed by removeNode(),
 *   either by the current or another JVM. However, true does *NOT* mean, that the node exists, 
 *   i.e. is persistent in the database. It just means, that it has not been removed.
 * </li>
 * </ul>
 * 
 * @author harald
 */
public class DbPreferences extends Preferences {
  
    
  /**
   * true if read-only operation (write attempt of key/value pairs throws exception)
   */
  private static boolean read_Only = false;
  
  
  /**
   * Sets the preferences to read only globally.
   * @param readOnly true for read only, false for read/write.
   */
  public static void setReadOnly(boolean readOnly) {
    read_Only = readOnly; 
  }
  
  /**
   * Determines whether preferences are read only.
   * Note: not static to allow overriding.
   * 
   * @return true if read only
   */
  public boolean isReadOnly() {
    return read_Only;
  }
  
  /** transaction name for flush **/
  public static final String TX_FLUSH       = "flush";
  /** transaction name for remove node **/
  public static final String TX_REMOVE_NODE = "remove node";
  
  
  
  
  

  private static Db db;                       // database connection
  private static boolean autoSync;            // true = keep in syncObject with other JVMs
  private static Object lock;                 // we need a single lock for all DbPreferences cause of Mod.Thread
  private static long nodeTableSerial = -1;   // highest tableSerial of _all_ DbPreferencesNodes
  private static long keyTableSerial  = -1;   // highest tableSerial of _all_ DbPreferencesKeys
  
  // for expiration processing: all persistent nodes and keys indexed by id
  private static Map<Long,DbPreferences>     prefIdMap = new TreeMap<Long,DbPreferences>();
  private static Map<Long,DbPreferencesKey>  keyIdMap  = new TreeMap<Long,DbPreferencesKey>();
  
  // dto. indexed by user + name
  private static Map<NodeIndex,DbPreferences> prefNameMap = new TreeMap<NodeIndex,DbPreferences>();
  
  // key for preNameMap
  private static class NodeIndex implements Comparable<NodeIndex> {
    
    private String user;
    private String name;
    
    private NodeIndex(String user, String name) {
      this.user = user;
      this.name = name;
    }
    
    private NodeIndex(DbPreferencesNode node)  {
      this.user = node.getUser();
      this.name = node.getName();
    }
    
    public int compareTo(NodeIndex o) {
      int rv = Compare.compare(user, o.user);
      if (rv == 0)  {
        rv = Compare.compare(name, o.name);
      }
      return rv;
    }
    
    @Override
    public boolean equals(Object obj) {
      return obj instanceof NodeIndex && compareTo((NodeIndex)obj) == 0;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 97 * hash + (user != null ? user.hashCode() : 0);
      hash = 97 * hash + (name != null ? name.hashCode() : 0);
      return hash;
    }
  }
  
  
  
  // for DbPreferencesFactory:
  private static Preferences systemRoot;        // the system root
  private static Preferences userRoot;          // the user root
  private static DbPreferencesFactory factory;  // the factory
  
  /**
   * initialize the preferences.
   * Throws IllegalStateException if already initialized
   *
   * @param prefDb the database connection
   * @param sync true for autoSync, else syncObject() must be invoked explicitly
   */
  public static void initialize(Db prefDb, boolean sync) {
    // works only once!
    if (lock != null) {
      throw new IllegalStateException("DbPreferences already initialized");
    }
    
    lock      = new Object();
    db        = prefDb;
    autoSync  = sync;
    
    // create the factory if not yet done (for example in EE-servers after redeploy)
    if (factory == null) {
      try {
        factory = (DbPreferencesFactory)
                    Class.forName(System.getProperty("java.util.prefs.PreferencesFactory")).newInstance();
      }
      catch (Exception ex) {
        throw new DbRuntimeException("cannot instantiate preferences factory: " + ex.getMessage(), ex);
      }
    }
    
    // preset tableserials
    nodeTableSerial = factory.createNode(db).selectModification();
    keyTableSerial  = factory.createKey(db).selectModification();
  }
  
  
  /**
   * Gets the system root object.
   * Package scope!
   * 
   * @return the system root
   */
  static Preferences getSystemRoot()  {
    if (systemRoot == null) {
      systemRoot = factory.createPreferences(false);
    }
    return systemRoot;
  }
  
  /**
   * Gets the user root object.
   * Package scope!
   * 
   * @return the user root
   */
  static Preferences getUserRoot()  {
    if (userRoot == null) {
      userRoot = factory.createPreferences(true);
    }
    return userRoot;
  }
  
  
  /**
   * Gets the database connection.
   * 
   * @return the db connection
   */
  public static Db getDb() {
    return db;
  }

  
  /**
   * Determines whether the preferences are automatically sync'd
   * to the database.
   * 
   * @return true if auto sync
   */
  public static boolean isAutoSync() {
    return autoSync;
  }
  
  
  
  /**
   * Expire the preferences keys.<br>
   * Invoked from the ModificationThread (registered in DbPreferencesKey)
   *
   * @param db is the Modthreads db connection
   * @param maxSerial is the current tableserial
   */   
  public static void expireKeys(Db db, long maxSerial) {
    synchronized(lock)  {
      // get tableserial/id-pairs
      long[] expireSet = factory.createKey(db).getExpiredTableSerials(keyTableSerial, maxSerial);
      
      int expNdx=0;
      while (expNdx < expireSet.length) {
        long expId = expireSet[expNdx++];
        long expTableSerial = expireSet[expNdx++];
        // process only keys that are modified by other jvms and in our keyMap
        DbPreferencesKey key = keyIdMap.get(expId);
        if (key != null && key.getTableSerial() < expTableSerial)  {
          // update value, if not same jvm (the rest cannot change!)
          DbPreferencesKey nkey = factory.createKey(db).select(expId);
          // update key value and status info
          key.setValue(nkey.getValue());
          key.setSerial(nkey.getSerial());
          key.setTableSerial(nkey.getTableSerial());
          // fire listeners if any for this node
          DbPreferences pref = prefIdMap.get(key.getNodeId());
          if (pref != null) {
            if (DbGlobal.logger.isFineLoggable()) {
              DbGlobal.logger.fine("key updated in " + pref + ": " + key);
            }
            pref.enqueuePreferenceChangeEvent(key.getKey(), key.getValue());
          }
        }
      }
      keyTableSerial = maxSerial;
    }
  }
  
  
  

  /**
   * Expires the preferences nodes.<br>
   * Invoked from ModificationThread (registered in DbPreferencesNode)
   *
   * @param db is the Modthreads db connection
   * @param maxSerial is the current tableserial
   */ 
  public static void expireNodes(Db db, long maxSerial)  {
    synchronized(lock)  {
      // get tableserial/id-pairs
      long[] expireSet = factory.createNode(db).getExpiredTableSerials(nodeTableSerial, maxSerial);

      // determine whether some nodes have been removed or not
      // see ModificationCounter why this trick works ;-)
      boolean someNodesRemoved = false;
      long lastSerial = -1;
      int expNdx=0;
      while (expNdx < expireSet.length) {
        long expId = expireSet[expNdx++];
        long expTableSerial = expireSet[expNdx++];
        if (lastSerial != -1 && expTableSerial - lastSerial > 1) {
          // gap in tableSerials: delete() invoked at least once
          someNodesRemoved = true;
          break;
        }
        lastSerial = expTableSerial;
      }
      if (maxSerial - lastSerial > 1) {
        someNodesRemoved = true;  // lastserial == -1 (no expired nodes found) or gap at end
      }
      
      if (someNodesRemoved && DbGlobal.logger.isFineLoggable()) {
        DbGlobal.logger.fine("some nodes have been removed");
      }
      
      expNdx=0;
      while (expNdx < expireSet.length) {
        long expId = expireSet[expNdx++];
        long expTableSerial = expireSet[expNdx++];
        // load the node
        DbPreferencesNode expiredNode = factory.createNode(db).select(expId);
        
        DbPreferences pref = prefIdMap.get(expId);    // check if node is already loaded
        
        if (pref == null) {
          // node is not known so far: possibly it does not have an ID yet, so giv'em an ID if new
          pref = prefNameMap.get(new NodeIndex(expiredNode));
          if (pref != null && pref.node.isNew()) {
            pref.node.setId(expiredNode.getId());   // serial and tableserial will be updated below
            pref.node.setParentId(expiredNode.getParentId());
            prefIdMap.put(pref.node.getId(), pref);
            if (pref.parent != null) {
              pref.parent.childIds.add(expiredNode.getId());
            }
            if (DbGlobal.logger.isFineLoggable()) {
              DbGlobal.logger.fine("assigned ID(s) to node " + pref.node);
            }
          }
        }
        
        if (pref != null && pref.node != null) {
          if (pref.node.getTableSerial() < expTableSerial)  {
            // check for added/removed keys
            if (pref.keys != null)  {  // keys != null if preflisteners registered
              List<DbPreferencesKey> keyList = factory.createKey(db).selectByNodeId(expId);
              // check for added keys
              for (DbPreferencesKey key: keyList) {
                if (pref.keys.containsKey(key.getKey()) == false)  {
                  // key is new: add to keys
                  key.setDb(DbPreferences.db);        // switch to Preferences-Db
                  pref.keys.put(key.getKey(), key);   // add to node key list
                  keyIdMap.put(key.getId(), key);     // add to key-cache
                  pref.enqueuePreferenceChangeEvent(key.getKey(), key.getValue());
                  if (DbGlobal.logger.isFineLoggable()) {
                    DbGlobal.logger.fine("key added to node " + pref.node + ": " + key);
                  }
                }
              }
              // check for removed keys
              for (Iterator<DbPreferencesKey> iter=pref.keys.values().iterator(); iter.hasNext(); ) {
                DbPreferencesKey key = iter.next();
                if (keyList.contains(key) == false) {
                  // key removed
                  iter.remove();                  // this will also remove from the map
                  keyIdMap.remove(key.getId());   // remove from global keymap too
                  pref.enqueuePreferenceChangeEvent(key.getKey(), null);
                  if (DbGlobal.logger.isFineLoggable()) {
                    DbGlobal.logger.fine("key removed from node " + pref.node + ": " + key);
                  }
                }
              }
            }
          }
          
          if (expiredNode != null) {
            // update serial and tableserial
            pref.node.setSerial(expiredNode.getSerial());
            pref.node.setTableSerial(expiredNode.getTableSerial());
          }
          
          /**
           * Added nodes only matter if there's a nodelistener registered on the parent.
           * Removed nodes matter if there's a nodelistener on the parent _or_ some nodes
           * have been deleted (that are already loaded)
           */
          if (someNodesRemoved || pref.areNodeListenersRegistered())  {
            // get child nodes to check for added or deleted nodes
            List<DbPreferencesNode> currentNodes = factory.createNode(db).selectByParentId(pref.node.getId());
            if (pref.areNodeListenersRegistered()) {
              // check for added nodes
              for (DbPreferencesNode node: currentNodes)  {
                if (pref.childIds.contains(node.getId()) == false)  {
                  try {
                    // node has been added
                    node.setDb(DbPreferences.db);
                    DbPreferences child = factory.createPreferences(pref, node);
                    prefIdMap.put(node.getId(), child);
                    prefNameMap.put(new NodeIndex(node), child);
                    pref.childPrefs.put(child.name, child);
                    pref.childIds.add(node.getId());
                    pref.enqueueNodeAddedEvent(child);
                    if (DbGlobal.logger.isFineLoggable()) {
                      DbGlobal.logger.fine("node " + child + " added to " + pref);
                    }
                  } 
                  catch (BackingStoreException ex) {
                    DbGlobal.errorHandler.severe(db, ex, "loading added node " + node + " failed");
                  }
                }
              }
            }
            if (someNodesRemoved) {
              // check for deleted nodes
              for (Long id: pref.childIds)  {
                boolean removed = true;
                for (DbPreferencesNode node: currentNodes)  {
                  if (id.longValue() == node.getId()) {
                    removed = false;
                    break;
                  }
                }
                if (removed)  {
                  try {
                    DbPreferences child = prefIdMap.get(id);    // must exist
                    child.removeNode();
                    pref.enqueueNodeRemovedEvent(child);
                    if (DbGlobal.logger.isFineLoggable()) {
                      DbGlobal.logger.fine("node " + child + " removed from " + pref);
                    }
                  } 
                  catch (Exception ex) {
                    DbGlobal.errorHandler.severe(db, ex, "removing deleted node " + pref.node + " failed");
                  }
                }
              }
            }
          }
        }
      }
      
      nodeTableSerial = maxSerial;    // next check starts at maxSerial
    }
  }
  

  
  
  
  
  private String name;                                // name relative to the parent
  private String absolutePath;                        // absolute pathname
  private boolean userMode;                           // true if this is a user-node (speeds up)
  private DbPreferences parent;                       // parent node, null = this is the root
  private DbPreferences root;                         // the root preference (non-static because of user and system-scope)
  private DbPreferencesNode node;                     // the database node (backing store), null = deleted
  private Map<String, DbPreferencesKey> keys;         // list of keys associated with this node (lazy if no PreferenceChangeListeners registered)
  private Set<Long> childIds;                         // IDs of all child nodes (only if NodeChangeListeners registered)
  private Map<String, DbPreferences> childPrefs;      // accessed child preferences so far. Key is the name relative to the parent.
  private PreferenceChangeListener[] prefListeners;   // preference change listeners
  private NodeChangeListener[] nodeListeners;         // node change listeners


  
  

  /**
   * Creates a preference node with the specified parent and the specified
   * name relative to its parent. Don't use this constructor for root-nodes!
   * 
   * @param parent the parent of this preference node
   * @param name the name of this preference node, relative to its parent
   * @throws IllegalArgumentException if <tt>name</tt> contains a slash
   *          (<tt>'/'</tt>),  or <tt>parent</tt> is <tt>null</tt> and
   *          name isn't <tt>""</tt>.
   */
  public DbPreferences(DbPreferences parent, String name) {
    
    if (parent == null) {
      throw new IllegalArgumentException("illegal constructor for root node");
    }
    
    if (name.indexOf('/') != -1) {
      throw new IllegalArgumentException("name '" + name + "' contains '/'");
    }
    if (name.equals("")) {
      throw new IllegalArgumentException("illegal name: empty string");
    }
    absolutePath = parent.parent == null ? 
                      "/" + name : 
                      parent.absolutePath() + "/" + name;
    
    this.name     = name;
    this.parent   = parent;
    this.userMode = parent.userMode;
    this.root     = parent.root;
    
    initializeNode();
  }
  
  
  
  /**
   * Special constructor for roots (both user and system).
   * This constructor will only be called twice by the static intializer.
   * Package scope!
   * 
   * @param userMode true if user mode, else system mode
   */
  protected DbPreferences(boolean userMode) {
    
    this.userMode = userMode;
    
    name = "";
    absolutePath = "/";
    root = this;
    
    if (nodeTableSerial < 0)  {
      /**
       * load max. tableSerials once from db
       */
      nodeTableSerial = factory.createNode(db).selectModification();
      keyTableSerial  = factory.createKey(db).selectModification();
    }
    
    initializeNode();
  }
  
  
  
  
  
  
  /**
   * Constructs DbPreferences from a DbPreferencesNode.
   * Only used from loadNodes().
   * Package scope!
   * @param parent the parent preferences
   * @param node the node
   * @throws BackingStoreException 
   */
  protected DbPreferences(DbPreferences parent, DbPreferencesNode node) throws BackingStoreException {
    
    if (node.getParentId() != parent.node.getId())  {
      throw new BackingStoreException(
                        "parent-ID mismatch in node '" + node + "': got ID=" +
                        node.getParentId() + ", expected ID=" + parent.node.getId());
    }
    
    this.node         = node;
    this.absolutePath = node.getName();
    this.name         = node.getBaseName();
    this.parent       = parent;
    this.userMode     = parent.userMode;
    this.root         = parent.root;
    
    updateNodeTableSerial(node.getTableSerial());
    
    childPrefs = new TreeMap<String, DbPreferences>();
    childIds   = node.selectChildIds();
  }
  
  
  
  

  /**
   * initializes the node
   */
  private void initializeNode() {
    childPrefs = new TreeMap<String, DbPreferences>();
    
    node = factory.createNode(db).selectByUserAndName(
                    userMode ? db.getUserInfo().getUsername() : null, 
                    absolutePath);
    if (node == null) {
      childIds = new TreeSet<Long>();
      node = factory.createNode(db);
      node.setUser(userMode ? db.getUserInfo().getUsername() : null);
      node.setName(absolutePath);
      if (parent != null) {
        node.setParentId(parent.node.getId());
      }
    }
    else  {
      childIds = node.selectChildIds();
      updateNodeTableSerial(node.getTableSerial());
      prefIdMap.put(node.getId(), this);
    }
    prefNameMap.put(new NodeIndex(node), this);
  }
  
  
  /**
   * updates the node table serial
   */
  private static void updateNodeTableSerial(long newSerial) {
    if (newSerial > nodeTableSerial)  {
      nodeTableSerial = newSerial;
    }
  }
  
  
  /**
   * updates the keys table serial
   */
  private static void updateKeyTableSerial(long newSerial) {
    if (newSerial > keyTableSerial)  {
      keyTableSerial = newSerial;
    }
  }
  
  
  
  /**
   * loads all keys from the backing store for the current node
   */
  private void loadKeys() {
    keys = new TreeMap<String, DbPreferencesKey>();
    for (DbPreferencesKey k: factory.createKey(db).selectByNodeId(node.getId())) {
      keys.put(k.getKey(), k);
      updateKeyTableSerial(k.getTableSerial());
      keyIdMap.put(k.getId(), k);
    }
  }
  
  /**
   * unloads all keys
   */
  private void unloadKeys() {
    if (keys != null) {
      for (DbPreferencesKey key: keys.values()) {
        keyIdMap.remove(key.getId());
      }
    }
    keys = null;
  }
  
  
  
  
  /**
   * loads all child-nodes from the backing store.
   */
  private void loadNodes() {
    List<DbPreferencesNode> childNodes = factory.createNode(db).selectByParentId(node.getId());
    // remove from maps before clear
    for (DbPreferences child: childPrefs.values()) {
      if (child.node != null) {
        prefIdMap.remove(child.node.getId());
        prefNameMap.remove(new NodeIndex(child.node));
      }
    }
    childPrefs.clear();
    childIds.clear();
    for (DbPreferencesNode n: childNodes)  {
      String basename = n.getBaseName();
      try {
        childPrefs.put(basename, factory.createPreferences(this, n));   // append to cache
        childIds.add(n.getId());                            // remember IDs
      } 
      catch (BackingStoreException ex) {
        throw new IllegalStateException("loading child nodes failed for " + this, ex);
      }
      // append to maps
      prefIdMap.put(n.getId(), this);
      prefNameMap.put(new NodeIndex(n), this);
    }
  }
  
  
  
  /**
   * gets a reference to a key
   */
  private DbPreferencesKey getKey(String key) {
    if (keys == null) {
      loadKeys();
    }
    return keys.get(key);
  }
  
  
  
  /**
   * assert that node is not deleted
   */
  private void assertNotRemoved() throws IllegalStateException {
    if (node == null) {
      // node has been deleted
      throw new IllegalStateException("node '" + absolutePath + "' has been removed");
    }
  }
  
  
  /**
   * Updates the serial and tableserial of a parent node.
   * The method is invoked whenever a key or child node is added or removed.
   *
   * @param node the parent node to update
   * @throws BackingStoreException if update failed
   */
  protected void updateParentNode(DbPreferencesNode node) throws BackingStoreException {
    if (node.save() == false) {
      throw new BackingStoreException("updating parentnode " + node + " failed");
    }    
  }
  
  
  /**
   * update parent node to trigger other jvms
   */
  private void updateParentNode() throws BackingStoreException {
    if (parent != null && parent.node != null)  {
      // update parent to trigger events in other jvms
      DbPreferences parentPref = prefIdMap.get(node.getParentId());
      if (parentPref == null) {
        throw new BackingStoreException("parent of " + node + " not in cache");
      }
      DbPreferencesNode parentNode = parentPref.node;
      if (parentNode == null) {
        throw new BackingStoreException("parentpref of " + node + " has no node");
      }
      updateParentNode(parentNode);
    }
  }
  
  
    
  /**
   * Recursive implementation of flush.
   * 
   * @throws java.util.prefs.BackingStoreException
   */
  private void flushImpl() throws BackingStoreException {

    if (node == null) {
      throw new BackingStoreException("node already removed"); 
    }

    // save node if new
    boolean nodeIsNew = node.isNew();

    if (nodeIsNew) {
      // node is new
      if (parent != null) {
        // set parent node if not root
        node.setParentId(parent.node.getId());    // node must exist (see flush())
      }
      if (node.save() == false) {
        /**
         * Either somebody else already created that node or there really
         * is a db-error.
         */
        throw new BackingStoreException("saving node '" + node + "' failed");
      }
      // append new node to map
      prefIdMap.put(node.getId(), this);
      // update parent to trigger events in other jvms
      updateParentNode();
    }

    // save the keys (if loaded)
    if (keys != null) {
      boolean updateNode = false;
      for (Iterator<DbPreferencesKey> iter=keys.values().iterator(); iter.hasNext(); )  {
        DbPreferencesKey key = iter.next();
        if (key.isDeleted())  {
          // remove it
          key.setId(key.getId());   // set the positive ID first
          if (key.delete() == false)  {
            throw new BackingStoreException("removing key '" + key + "' failed");
          }
          iter.remove();    // remove key physically
          // remove key from map
          keyIdMap.remove(key.getId());
          updateNode = true;    // some key has been deleted: update node to trigger other jvms
        }
        else if (key.isModified()) {
          if (key.isNew())  {
            // some key has been added: update node to trigger other jvms
            updateNode = true;
          }
          if (key.save() == false) { // save new or modified key
            /**
             * Either somebody else already created that key or there really
             * is a db-error. We can't update the key if db.isUniqueViolation()
             * because the backend usually terminates the transaction upon
             * such an error.
             */
            throw new BackingStoreException("can't save key '" + key + "'");
          }
          // update keymap
          keyIdMap.put(key.getId(), key);
        }
      }
      if (updateNode && !nodeIsNew) {
        // keys have been added or deleted (and node already exists): trigger other jvms modthread
        updateParentNode(node);
      }
    }

    // process child nodes
    for (DbPreferences child: childPrefs.values()) {
      child.flushImpl();
    }
  }


    
  
  /**
   * recover this node and all its descendents.
   * Necessary after BackingStoreException in flush().
   * Invoked from within flush().
   *
   * Notice: the listeners are not recovered, i.e. lost
   */
  private void recover()  {
    
    DbGlobal.logger.warning("*** recovering node " + this + " ***");

    // remove the keys for this node
    if (keys != null) {
      for (DbPreferencesKey key: keys.values()) {
        keyIdMap.remove(key.getId());
      }
      keys = null;
    }

    // remove this node from its parent
    if (parent != null) {
      if (node != null) {
        parent.childIds.remove(node.getId());
        
      }
      parent.childPrefs.remove(name);
    }
    
    // remove the node from the global pool
    if (node != null) {
      prefIdMap.remove(node.getId());
      prefNameMap.remove(new NodeIndex(node));
    }
    
    node = null;
    nodeListeners = null;
    prefListeners = null;
    
    // do that for all childs (need a copy cause of remove() in collection)
    DbPreferences[] childs = new DbPreferences[childPrefs.size()];
    childPrefs.values().toArray(childs);
    
    for (DbPreferences child: childs)  {
      child.recover();
    }
  }
    

  /**
   * tokenizer contains <name> {'/' <name>}*
   */
  private Preferences node(StringTokenizer path) {
    
    String token = path.nextToken();
    
    if (token.equals("/"))  { // Check for consecutive slashes
      throw new IllegalArgumentException("Consecutive slashes in path");
    }
    
    DbPreferences child = childPrefs.get(token);
    if (child == null) {
      if (token.length() > MAX_NAME_LENGTH) {
        throw new IllegalArgumentException("Node name " + token + " too long");
      }
      child = factory.createPreferences(this, token);
      if (child.node.isNew()) {
        enqueueNodeAddedEvent(child);
      }
      else  {
        childIds.add(child.node.getId());
      }
      childPrefs.put(token, child);
    }
    if (!path.hasMoreTokens()) {
      return child;
    }
    path.nextToken();  // Consume slash
    if (!path.hasMoreTokens()) {
      throw new IllegalArgumentException("Path ends with slash");
    }
    return child.node(path);
  }

  
  /**
   * Recurive implementation for removeNode.
   * Invoked with locks on all nodes on path from parent of "removal root"
   * to this (including the former but excluding the latter).
   */
  private void removeNodeImpl() throws BackingStoreException {
    if (node != null) {
      loadNodes();    // get all nodes

      // recursively remove all children
      for (DbPreferences child: childPrefs.values()) {
        child.removeNodeImpl();
      }

      if (node.delete() == false) {
          throw new BackingStoreException("node '" + node + "' does not exist in db");
      }

      prefIdMap.remove(node.getId());     // remove from nodemap
      prefNameMap.remove(new NodeIndex(node));

      // delete all keys for this node
      factory.createKey(db).deleteByNodeId(node.getId());
      unloadKeys();

      keys          = null;
      childPrefs    = null;
      childIds      = null;
      nodeListeners = null;
      prefListeners = null;

      node = null;                        // mark node deleted

      parent.enqueueNodeRemovedEvent(this);
    }
  }
  
  
  /**
   * @return true if any node listeners are registered on this node
   */
  private boolean areNodeListenersRegistered()  {
    return nodeListeners != null && nodeListeners.length > 0;
  }

  
  /**
   * @return true if any preferences listeners are registered on this node
   */
  private boolean arePreferenceListenersRegistered()  {
    return prefListeners != null && prefListeners.length > 0;
  }
  
  
    
    
  
  
  /**
   * Returns a string representation of this preferences node.<br>
   * <ul>
   * <li>if node is deleted: "&lt;absolutePath&gt; [deleted]"</li>
   * <li>user node: "&lt;user&gt:&lt;absolutePath&gt;"</li>
   * <li>system node: "&lt;system&gt:&lt;absolutePath&gt;"</li>
   * </ul>
   */
  @Override
  public String toString() {
    if (node == null) {
      return absolutePath + " [deleted]";
    }
    if (userMode) {
      return node.getUser() + ":" + absolutePath;
    }
    else  {
      return "<system>:" + absolutePath;
    }
  }

  
  
  
  // --------------- implements Preferences --------------------------
  
  public String name() {
    return name;
  }

  
  public String absolutePath() {
    return absolutePath;
  }
  
  
  public boolean isUserNode() {
    return userMode;
  }

  
  public void put(String key, String value) {
    
    if (key == null || value == null) {
      throw new NullPointerException();
    }
    if (key.length() > MAX_KEY_LENGTH)  {
      throw new IllegalArgumentException("key too long: <" + key + ">");
    }
    if (value.length() > MAX_VALUE_LENGTH)  {
      throw new IllegalArgumentException("value too long: <" + value + ">");
    }

    synchronized(lock) {
      assertNotRemoved();
      if (!isReadOnly()) {
        DbPreferencesKey k = getKey(key);
        if (k == null) {
          // add a new key/value-pair to the node
          k = factory.createKey(db);
          if (!node.isIdValid()) {
            /**
             * node is not saved to disk: delay setting the nodeID
             * by setLazyNode(). See prepareSetFields in DbPreferencesKey.
             */
            k.setLazyNode(node); 
          }
          else  {
            k.setNodeId(node.getId());
          }
          k.setKey(key);
          keys.put(key, k);
        }
        else if (k.isDeleted())  {
          // was marked deleted in remove(): enable it again
          k.setId(-k.getId());
        }
        String oldValue = k.getValue();
        k.setValue(value);
        if (Compare.compare(oldValue, value) != 0)  {
          enqueuePreferenceChangeEvent(key, value);
        }
      }
    }
  }
  

  public String get(String key, String def) {
    if (key == null) {
      throw new NullPointerException("null key");
    }
    synchronized(lock) {
      assertNotRemoved();
      DbPreferencesKey k = getKey(key);
      return k == null || k.isDeleted() || k.getValue() == null ? def : k.getValue();
    }
  }

  
  public void remove(String key) {
    synchronized(lock) {
      assertNotRemoved();
      if (!isReadOnly()) {
        DbPreferencesKey k = getKey(key);
        if (k != null)  {
          if (k.isIdValid())  {
            /**
             * We don't delete the key right now, but mark it as "deleted" by
             * negating the ID. Notice that getId() always returns a positive ID,
             * so the following code will always set a negative ID.
             */
            k.setId(-k.getId());
          }
          else {
            // never saved to disk: remove from keys right now
            keys.remove(key);
          }
          // else: already marked deleted
          enqueuePreferenceChangeEvent(key, null);
        }
      }
    }
  }

  
  public void clear() throws BackingStoreException {
    if (!isReadOnly())  {
      String[] keyArray = keys();
      for (int i=0; i < keyArray.length; i++) {
        remove(keyArray[i]);
      }
    }
  }

  
  public void sync() throws BackingStoreException {       
    flush();  // flush first
    // explicit expiration
    expireKeys(db, factory.createKey(db).selectModification());
    expireNodes(db, factory.createNode(db).selectModification());
  }


  public void flush() throws BackingStoreException {
    if (!isReadOnly())  {
      synchronized(lock)  {
        // single transaction
        boolean oldCommit = db.begin(TX_FLUSH);
        try {
          /**
           * go up to the first parent with a new node
           */
          DbPreferences prefs = this;
          while (prefs.parent != null && prefs.parent.node != null && prefs.parent.node.isNew()) {
            prefs = prefs.parent;
          }
          // flush starting at first non-persistent node
          prefs.flushImpl();
          db.commit(oldCommit);
        }
        catch (BackingStoreException ex)  {
          db.rollback(oldCommit);   // some severe db-error or unique violation: rollback
          /**
           * recover the node, i.e. load from storage and invalidate all modifications
           * made so far. This is the only way to get a working preferences tree again.
           */
          recover();
          throw ex;
        }
      }
    }
  }


  public void putInt(String key, int value) {
    put(key, Integer.toString(value));
  }

  
  public int getInt(String key, int def) {
    int result = def;
    try {
      String value = get(key, null);
      if (value != null) {
        result = Integer.parseInt(value);
      }
    } 
    catch (NumberFormatException e) {
      // Ignoring exception causes specified default to be returned
    }
    return result;
  }

  
  public void putLong(String key, long value) {
    put(key, Long.toString(value));
  }

  
  public long getLong(String key, long def) {
    long result = def;
    try {
      String value = get(key, null);
      if (value != null) {
        result = Long.parseLong(value);
      }
    } 
    catch (NumberFormatException e) {
      // Ignoring exception causes specified default to be returned
    }
    return result;
  }

  
  public void putBoolean(String key, boolean value) {
    put(key, String.valueOf(value));
  }

  
  public boolean getBoolean(String key, boolean def) {
    boolean result = def;
    String value = get(key, null);
    if (value != null) {
      if (value.equalsIgnoreCase("true")) {
        result = true;
      }
      else if (value.equalsIgnoreCase("false")) {
        result = false;
      }
    }
    return result;
  }

  
  public void putFloat(String key, float value) {
    put(key, Float.toString(value));
  }

  
  public float getFloat(String key, float def) {
    float result = def;
    try {
      String value = get(key, null);
      if (value != null) {
        result = Float.parseFloat(value);
      }
    } 
    catch (NumberFormatException e) {
      // Ignoring exception causes specified default to be returned
    }
    return result;
  }

  
  public void putDouble(String key, double value) {
    put(key, Double.toString(value));
  }

  
  public double getDouble(String key, double def) {
    double result = def;
    try {
      String value = get(key, null);
      if (value != null) {
        result = Double.parseDouble(value);
      }
    } 
    catch (NumberFormatException e) {
      // Ignoring exception causes specified default to be returned
    }
    return result;
  }
  

  public void putByteArray(String key, byte[] value) {
    put(key, Base64.byteArrayToBase64(value));
  }

  
  public byte[] getByteArray(String key, byte[] def) {
    byte[] result = def;
    String value = get(key, null);
    try {
      if (value != null) {
        result = Base64.base64ToByteArray(value);
      }
    }
    catch (RuntimeException e) {
      // Ignoring exception causes specified default to be returned
    }
    return result;
  }
    
  
  public String[] keys() throws BackingStoreException {
    synchronized(lock) {
      assertNotRemoved();
      if (keys == null) {
        loadKeys();
      }
      String keyNames[] = new String[keys.size()];
      keys.keySet().toArray(keyNames);
      return keyNames;
    }
  }

    
  public String[] childrenNames() throws BackingStoreException {
    synchronized(lock) {
      assertNotRemoved();
      loadNodes();    // update childCache
      // else: nodes and childCache up to date
      String[] childNames = new String[childPrefs.size()];
      childPrefs.keySet().toArray(childNames);
      return childNames;        
    }
  }


  public Preferences parent() {
    assertNotRemoved();
    return parent;
  }
  
  
  public Preferences node(String path) {
    synchronized(lock) {
      assertNotRemoved();
      if (path.equals("")) {
        return this;
      }
      if (path.equals("/")) {
        return root;
      }
      if (path.charAt(0) != '/') {
        // relative path
        return node(new StringTokenizer(path, "/", true));
      }
    }

    // absolute path: start at root node
    return root.node(new StringTokenizer(path.substring(1), "/", true));
  }


    
  /** 
   * {@inheritDoc}
   * <p>
   * This implementation differs from AbstractPreferences because
   * it checks the persistent storage rather than the cache, i.e.
   * a node only created in memory but not yet written to persistent storage
   * is considered non-existant. As a consequence it is possible to decide
   * whether a system-preference has been overridden by a user-preference.
   * (see CompositePreferences).
   * Notice that "" will return true even if not written to disk to 
   * denote "not deleted" (which does not mean "saved" -- a little confusing)
   */
  public boolean nodeExists(String pathName) throws BackingStoreException {
    synchronized(lock) {
      if (pathName.equals("")) {  // special for ""
        return node != null;
      }
      assertNotRemoved();
      if (pathName.equals("/")) {
        // root
        return true;
      }
      if (pathName.charAt(0) != '/') {
        // relative path: make absolute
        pathName = absolutePath + "/" + pathName;
      }
      
      // go by absolute path
      DbPreferences pref = prefNameMap.get(new NodeIndex(node.getUser(), pathName));  // check cache first
      return (pref != null && pref.node.isNew() == false) ||  // persistent node exists in cache
             // or node exists in storage
             factory.createNode(db).selectByUserAndName(node.getUser(), pathName) != null;
    }
  }
  
  
  public void removeNode() throws BackingStoreException {
    if (this == root) {
      throw new UnsupportedOperationException("Can't remove the root!");
    }
    if (!isReadOnly()) {
      synchronized(lock) {
        assertNotRemoved();
        boolean oldCommit = db.begin(TX_REMOVE_NODE);
        try {
          updateParentNode(); // update parent to trigger other jvms
          removeNodeImpl();   // remove this node and all subnodes
          parent.childPrefs.remove(name);
          db.commit(oldCommit);
        }
        catch (Exception e) {
          db.rollback(oldCommit);
          if (e instanceof BackingStoreException) {
            throw (BackingStoreException)e;
          }
          throw new BackingStoreException(e);
        }
      }
    }
  }


  /**
   * Registers the specified listener to receive <i>preference change
   * events</i> for this preference node.  A preference change event is
   * generated when a preference is added to this node, removed from this
   * node, or when the value associated with a preference is changed.
   * (Preference change events are <i>not</i> generated by the {@link
   * #removeNode()} method, which generates a <i>node change event</i>.
   * Preference change events <i>are</i> generated by the <tt>clear</tt>
   * method.)
   *
   * <p>Events are generated even for changes made outside this JVM. For the local
   * JVM events are generated before the changes have been made persistent. For all
   * other JVMs events are generated *after* flush()/syncObject().
   * 
   * @param pcl The preference change listener to add.
   * @throws NullPointerException if <tt>pcl</tt> is null.
   * @throws IllegalStateException if this node (or an ancestor) has been
   *         removed with the {@link #removeNode()} method.
   * @see #removePreferenceChangeListener(PreferenceChangeListener)
   * @see #addNodeChangeListener(NodeChangeListener)
   */
  public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
    if (pcl == null) {
      throw new NullPointerException("Change listener is null.");
    }
    synchronized(lock) {
      assertNotRemoved();
      if (prefListeners == null) {
        prefListeners = new PreferenceChangeListener[1];
        prefListeners[0] = pcl;
      } 
      else {
        PreferenceChangeListener[] old = prefListeners;
        prefListeners = new PreferenceChangeListener[old.length + 1];
        System.arraycopy(old, 0, prefListeners, 0, old.length);
        prefListeners[old.length] = pcl;
      }
      // load keys if not yet done
      if (keys == null) {
        loadKeys();
      }
    }
    startEventDispatchThreadIfNecessary();
  }
  
  
  public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
    synchronized(lock) {
      assertNotRemoved();
      if ((prefListeners == null) || (prefListeners.length == 0)) {
        throw new IllegalArgumentException("Listener not registered.");
      }
      // Copy-on-write
      PreferenceChangeListener[] newPl = new PreferenceChangeListener[prefListeners.length - 1];
      int i = 0;
      while (i < newPl.length && prefListeners[i] != pcl) {
        newPl[i] = prefListeners[i++];
      }

      if (i == newPl.length && prefListeners[i] != pcl) {
        throw new IllegalArgumentException("Listener not registered.");
      }
      while (i < newPl.length) {
        newPl[i] = prefListeners[++i];
      }
      prefListeners = newPl;
    }
  }

  
  /**
   * Registers the specified listener to receive <i>node change events</i>
   * for this node.  A node change event is generated when a child node is
   * added to or removed from this node.  (A single {@link #removeNode()}
   * invocation results in multiple <i>node change events</i>, one for every
   * node in the subtree rooted at the removed node.)
   *
   * <p>Events are generated even for changes made outside this JVM. For the local
   * JVM events are generated before the changes have been made persistent. For all
   * other JVMs events are generated *after* flush()/syncObject().
   *
   * <p>Node creation will always generate an even for the local JVM. Other
   * JVMs get that event only in case the node is created on disk (and not updated
   * in case another JVM already created that node).
   * 
   * @param ncl The <tt>NodeChangeListener</tt> to add.
   * @throws NullPointerException if <tt>ncl</tt> is null.
   * @throws IllegalStateException if this node (or an ancestor) has been
   *         removed with the {@link #removeNode()} method.
   * @see #removeNodeChangeListener(NodeChangeListener)
   * @see #addPreferenceChangeListener(PreferenceChangeListener)
   */
  public void addNodeChangeListener(NodeChangeListener ncl) {
    if (ncl == null) {
      throw new NullPointerException("Change listener is null.");
    }
    synchronized(lock) {
      assertNotRemoved();
      if (nodeListeners == null) {
        nodeListeners = new NodeChangeListener[1];
        nodeListeners[0] = ncl;
      } 
      else {
        NodeChangeListener[] old = nodeListeners;
        nodeListeners = new NodeChangeListener[old.length + 1];
        System.arraycopy(old, 0, nodeListeners, 0, old.length);
        nodeListeners[old.length] = ncl;
      }
    }
    startEventDispatchThreadIfNecessary();
  }

  
  public void removeNodeChangeListener(NodeChangeListener ncl) {
    synchronized(lock) {
      assertNotRemoved();
      if ((nodeListeners == null) || (nodeListeners.length == 0)) {
        throw new IllegalArgumentException("Listener not registered.");
      }
      NodeChangeListener[] newNl = new NodeChangeListener[nodeListeners.length - 1];
      int i = 0;
      while (i < nodeListeners.length && nodeListeners[i] != ncl) {
        newNl[i] = nodeListeners[i++];
      }
      if (i == nodeListeners.length) {
        throw new IllegalArgumentException("Listener not registered.");
      }
      while (i < newNl.length) {
        newNl[i] = nodeListeners[++i];
      }
      nodeListeners = newNl;
    }
  }





  /**
   * Queue of pending notification events.  When a preference or node
   * change event for which there are one or more listeners occurs,
   * it is placed on this queue and the queue is notified.  A background
   * thread waits on this queue and delivers the events.  This decouples
   * event delivery from preference activity, greatly simplifying
   * locking and reducing opportunity for deadlock.
   */
  private static final List<EventObject> eventQueue = new LinkedList<EventObject>();

  /**
   * These two classes are used to distinguish NodeChangeEvents on
   * eventQueue so the event dispatch thread knows whether to call
   * childAdded or childRemoved.
   */
  private class NodeAddedEvent extends NodeChangeEvent {
    private static final long serialVersionUID = -6743557530157328528L;
    NodeAddedEvent(Preferences parent, Preferences child) {
      super(parent, child);
    }
  }
  private class NodeRemovedEvent extends NodeChangeEvent {
    private static final long serialVersionUID = 8735497392918824837L;
    NodeRemovedEvent(Preferences parent, Preferences child) {
      super(parent, child);
    }
  }

  /**
   * A single background thread ("the event notification thread") monitors
   * the event queue and delivers events that are placed on the queue.
   */
  private static class EventDispatchThread extends Thread {
    
    @Override
    public void run() {
      while(true) {
        // Wait on eventQueue till an event is present
        EventObject event = null;
        synchronized(eventQueue) {
          try {
            while (eventQueue.isEmpty())  {
              eventQueue.wait();    // wait for notify
            }
            event = eventQueue.remove(0);
          } 
          catch (InterruptedException e) {
            return;   // terminate thread
          }
        }

        // Now we have event & hold no locks; deliver evt to listeners
        DbPreferences src=(DbPreferences)event.getSource();
        if (event instanceof PreferenceChangeEvent) {
          PreferenceChangeEvent pce = (PreferenceChangeEvent)event;
          PreferenceChangeListener[] listeners = src.prefListeners();
          for (int i=0; i<listeners.length; i++) {
            listeners[i].preferenceChange(pce);
          }
        } 
        else {
          NodeChangeEvent nce = (NodeChangeEvent)event;
          NodeChangeListener[] listeners = src.nodeListeners();
          if (nce instanceof NodeAddedEvent) {
            for (int i=0; i<listeners.length; i++) {
              listeners[i].childAdded(nce);
            }
          } 
          else {
            // assert nce instanceof NodeRemovedEvent;
            for (int i=0; i<listeners.length; i++) {
              listeners[i].childRemoved(nce);
            }
          }
        }
      }
    }
  }

  private static Thread eventDispatchThread = null;

  /**
   * This method starts the event dispatch thread the first time it
   * is called.  The event dispatch thread will be started only
   * if someone registers a listener.
   */
  private static synchronized void startEventDispatchThreadIfNecessary() {
    if (eventDispatchThread == null) {
      eventDispatchThread = new EventDispatchThread();
      eventDispatchThread.setDaemon(true);
      eventDispatchThread.start();
    }
  }

  /**
   * Return this node's preference/node change listeners.  Even though
   * we're using a copy-on-write lists, we use synchronized accessors to
   * ensure information transmission from the writing thread to the
   * reading thread.
   * 
   * @return the array of listeners
   */
  PreferenceChangeListener[] prefListeners() {
    return prefListeners;
  }
  
  /**
   * @return the array of node listeners
   */
  NodeChangeListener[] nodeListeners() {
    return nodeListeners;
  }

  /**
   * Enqueue a preference change event for delivery to registered
   * preference change listeners unless there are no registered
   * listeners.  Invoked with this.lock held.
   */
  private void enqueuePreferenceChangeEvent(String key, String newValue) {
    if (arePreferenceListenersRegistered()) {
      synchronized(eventQueue) {
        eventQueue.add(new PreferenceChangeEvent(this, key, newValue));
        eventQueue.notify();
      }
    }
  }

  /**
   * Enqueue a "node added" event for delivery to registered node change
   * listeners unless there are no registered listeners.  Invoked with
   * this.lock held.
   */
  private void enqueueNodeAddedEvent(Preferences child) {
    if (areNodeListenersRegistered()) {
      synchronized(eventQueue) {
        eventQueue.add(new NodeAddedEvent(this, child));
        eventQueue.notify();
      }
    }
  }

  /**
   * Enqueue a "node removed" event for delivery to registered node change
   * listeners unless there are no registered listeners.  Invoked with
   * this.lock held.
   */
  private void enqueueNodeRemovedEvent(Preferences child) {
    if (areNodeListenersRegistered()) {
      synchronized(eventQueue) {
        eventQueue.add(new NodeRemovedEvent(this, child));
        eventQueue.notify();
      }
    }
  }

  
  public void exportNode(OutputStream os) throws IOException, BackingStoreException {   
    PreferencesSupport.export(os, this, false);
  }

  
  public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {       
    PreferencesSupport.export(os, this, true);
  }



  /**
   * Imports all of the preferences represented by the XML document as in Preferences.<p>
   * 
   * CAUTION: As in Preferences this method is static, i.e. this one does *NOT*
   * override Preferences.importPreferences()!
   * YOU MUST INVOKE DbPreferences.importPreferences() directly!!!
   * This is due to the design by Sun.
   * Sun's implementation of Preferences.importPreferences() should better invoke
   * a method in PreferencesFactory according to the property 
   * "java.util.prefs.PreferencesFactory".
   * Instead it invokes (package scoped) XmlSupport :-(
   * 
   * @param is the input stream from which to read the XML document.
   * @see RuntimePermission
   * @throws IOException if reading from the specified output stream
   *         results in an <tt>IOException</tt>.
   * @throws InvalidPreferencesFormatException Data on input stream does not
   *         constitute a valid XML document with the mandated document type.
   * @throws SecurityException If a security manager is present and
   *         it denies <tt>RuntimePermission("preferences")</tt>.
   */
  public static void importPreferences(InputStream is) throws IOException, InvalidPreferencesFormatException {
    PreferencesSupport.importPreferences(is);
  }
  
  
}
