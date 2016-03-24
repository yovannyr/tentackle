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

// $Id: SecurityManager.java 475 2009-08-07 18:29:14Z harald $
// Created on September 12, 2002, 2:38 PM

package org.tentackle.appworx;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.tentackle.util.ApplicationException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.tentackle.util.Compare;
import org.tentackle.util.StringHelper;



/**
 * The SecurityManager applies {@link Security}-rules to {@link AppDbObject}s
 * and grants or denies privileges.<br>
 * The rules themselves are AppDbObjects
 * and thus can be easily stored in the database. The SecurityManager keeps
 * a list of rules that belong to a user. Again, this can easily be
 * extended to roles. The list of rules is interpreted as an ACL (access
 * control list). Whenever a privilege is checked, the list is processes
 * until a rule fires. If no rule fires the default result is returned,
 * which is ether granted or denied.<br>
 *
 * @author harald
 */
public class SecurityManager {
  
  // Grant types
  private static final int GRANT_DEFAULT = 0;     // no rule found, default applies (i.e. usually accepted)
  private static final int GRANT_ACCEPT  = 1;     // explicit accept for requested permission found
  private static final int GRANT_DENY    = 2;     // explicit deny for requested permission found

  private static AtomicLong validCount = new AtomicLong(1);  // incremented each time invalidated
  
  /**
   * Marks the rules cache invalid in all security managers.
   * This method is invoked whenever some {@link Security} rules change in the
   * database.
   */
  public static void invalidateAll() {
    validCount.incrementAndGet();
  }
  
  
  /**
   * ContextDb to enable "server mode".
   * In servers (especially web servers) it makes sense to preload _all_ security rules once for all
   * grantees and then to create manager instances sharing the rules among
   * their grantees.
   * By default server mode is turned off (null)
   */
  public static ContextDb serverContextDb = null;
  
  private static long serverValidCount = 0;
  private static TreeSet<Security> rulesSet;     // all rules organized by grantId

  // sort rules by grantId + Id
  private static class SecurityGranteeComparator implements Comparator<Security> {
    public int compare(Security o1, Security o2) {
      // compare the grantee first
      int rv = Compare.compareLong(o1.getGrantId(), o2.getGrantId());
      if (rv == 0)  {
        // compare object id
        rv = Compare.compareLong(o1.getId(), o2.getId());
      }
      return rv;
    }
  }
  
  
  
  private ContextDb contextDb;                    // base-context
  private long userId;                            // user ID
  private TreeMap<ClassKey,Security> classMap;    // class rules
  private TreeMap<ObjectKey,Security> objectMap;  // object rules
  private TreeMap<Object2Key,Security> clsObjMap; // all classes with object rules
  private long lastValid;                         // != validCount if cache needs initialization
  private boolean enabled;                        // true if security manager is enabled
  private boolean acceptByDefault = true;         // true if isAccepted() on GRANT_DEFAULT
  private boolean denyByDefault;                  // true if isDenied() on GRANT_DEFAULT
  
  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  

  /**
   * Creates a security manager for a given database context and user.
   * 
   * @param contextDb the database context
   * @param userId the object ID of the user
   */
  public SecurityManager(ContextDb contextDb, long userId) {
    
    this.contextDb = contextDb;
    this.userId = userId;
    
    enabled = contextDb != null;
    
    // manager is invalidated initially because lastValid = 0
  }  
  
  
  /**
   * Creates a security manager for given user.
   * 
   * @param user the user
   */
  public SecurityManager(AppDbObject user) {
    this(user == null ? null : user.getContextDb(), 
         user == null ? 0 : user.getId());
  }
  
  
  /**
   * Creates a disabled security manager.
   * This is the default manager.
   */
  public SecurityManager() {
    this(null, 0);
  }
  
  
  
  /**
   * Creates a new {@link Security} instance.<br>
   * Security instances should not be created by their constructor.
   * Instead, the security manager should be used to create it.
   * Must be overridden in apps if Security is extended.
   *
   * @return an empty Security object in contextDb-context
   */
  public Security newSecurityInstance() {
    return new Security(contextDb);
  }
  
  
  /**
   * Gets the class for {@link Security} rules.<br>
   * Must be overridden in apps if Security is extended.
   * 
   * @return the class for security rules
   */
  public Class getSecurityClass() {
    return Security.class; 
  }
  
  
  
  /**
   * Creates a new {@link SecurityDialog}.<br>
   * The dialog edits the rules for a given object or class.
   * Override in apps if the SecurityDialog is not tentackle's one.
   *
   * @param contextDb the contextDb 
   * @param clazz   the class to set security rules for.
   *                (use org.tentackle.appworx.SecurityDialog.class if working on security itself)
   * @param permissionType the kind of permission set. If 0, will be determined from clazz (if AppDbObject)
   *                or set to Security.TYPE_PROGRAM otherwise.
   * @param id      he object id in clazz (if clazz is a AppDbObject) or null
   *                if all objects or clazz is not a AppDbObject.
   *
   * @return the security dialog
   * @throws ApplicationException is setup failed
   */
  public SecurityDialog newSecurityDialogInstance (ContextDb contextDb, int permissionType, Class clazz, long id) 
         throws ApplicationException {
    return new SecurityDialog(contextDb, permissionType, clazz, id);     
  }
  
  
  /**
   * Creates a new SecurityDialog instance for a given data object.
   *
   * @param object the AppDbObject
   * @return the security dialog
   * @throws ApplicationException is setup failed
   */
  public final SecurityDialog newSecurityDialogInstance(AppDbObject object) throws ApplicationException {
    return newSecurityDialogInstance(object.getContextDb(), object.permissionType(), object.getClass(), object.getId());
  }
  
  
  /**
   * Creates a new SecurityDialog instance for a class and permission type.
   *
   * @param contextDb the contextDb-context
   * @param permissionType the permission type
   * @param clazz the class
   * @return the security dialog
   * @throws ApplicationException is setup failed
   */
  public final SecurityDialog newSecurityDialogInstance(ContextDb contextDb, int permissionType, Class clazz) throws ApplicationException {
    return newSecurityDialogInstance(contextDb, permissionType, clazz, 0);
  }
  
  
  /**
   * Creates a new SecurityDialog instance for a class figuring out its permission type.
   *
   * @param contextDb the contextDb-context
   * @param clazz the class
   * @return the security dialog
   * @throws ApplicationException is setup failed
   */
  public final SecurityDialog newSecurityDialogInstance(ContextDb contextDb, Class clazz) throws ApplicationException {
    return newSecurityDialogInstance(contextDb, 0, clazz, 0);
  }
  
  
  /**
   * Creates a new SecurityDialog for the SecurityDialog itself.
   * The default implementation works on the permissions of the Security-class.
   *
   * @param contextDb the contextDb-context
   * @return the security dialog
   * @throws ApplicationException is setup failed
   */
  public final SecurityDialog newSecurityDialogInstance(ContextDb contextDb) throws ApplicationException {
    return newSecurityDialogInstance(contextDb, 0, getSecurityClass(), 0);
  }
  
  
  
  /**
   * Gets the Class of a SecurityDialog.<br>
   * Override in apps if the SecurityDialog is not tentackle's one.
   * @return the class
   */
  public Class getSecurityDialogClass() {
    return SecurityDialog.class; 
  }  
  
  
  /**
   * Gets the database context for this manager.
   * 
   * @return the database context
   */
  public ContextDb getContextDb() {
    return contextDb; 
  }
  
  
  /**
   * Gets the object ID of the user's identity.
   * 
   * @return the object ID
   */
  public long getUserId() {
    return userId; 
  }
  
  

  
  /**
   * Checks whether the user is allowed to invoke the SecurityDialog.
   * The default implementation checks the write-permission of the
   * Security-class.
   * 
   * @return the security result
   */
  public SecurityResult getSecurityDialogPrivilege() {
    return privilege(getSecurityClass(), contextDb, Security.WRITE);
  }
  
  
  /**
   * Checks a privilege.
   *
   * @param clazz      the class, for example a FormDialog or an AppDbObject (never null!)
   * @param contextDb  the context-Db the object or class is used in, null = all
   * @param objectId   the objectId or 0 if applies to class, -1 for "any object of given class"
   * @param permission the requested permission(s)
   * @return a SecurityResult
   */
  public SecurityResult privilege(Class clazz, ContextDb contextDb, long objectId, int permission)  {
    
    if (AppworxGlobal.logger.isFineLoggable()) {
      AppworxGlobal.logger.fine("Checking clazz=" + clazz + ", context=" + contextDb.toGenericString() + ", objectId=" + objectId + ", permission=" + permission);
    }
    
    
    if (enabled == false)   {
      if (AppworxGlobal.logger.isFineLoggable()) {
        AppworxGlobal.logger.fine("SecurityManager is disabled -> GRANT_DEFAULT");
      }
      return new SecResultImpl();
    }
    
    if (userId == 0) {
      if (AppworxGlobal.logger.isFineLoggable()) {
        AppworxGlobal.logger.fine("user not set -> GRANT_DENY");
      }      
      return new SecResultImpl(GRANT_DENY, Locales.bundle.getString("user_not_set"), null);
    }
    
    
    Lock readLock = lock.readLock();
    
    try {
      readLock.lock();
      
      if (validCount.get() != lastValid)  {
        readLock.unlock();    // we must release to acquire the write lock
        Lock writeLock = lock.writeLock();
        long currentValidCount = validCount.get();
        boolean writeLocked = false;
        try {
          if (currentValidCount != lastValid)  {  // check again because it was unlocked for a short moment
            writeLock.lock();
            writeLocked = true;
            // valid now (so that initialize does not loop recursivly to here...)
            lastValid = currentValidCount;
            // disable the manager temporarily: don't check any object while reloading
            // this might happen only for *this* thread (reentrant lock) and "enabled"
            // need not to be volatile
            enabled = false;
            // if database connection is remote: make sure that the remote
            // manager is initialized first!
            if (getContextDb().getDb().isRemote()) {
              newSecurityInstance().assertRemoteSecurityManagerInitialized();
            }
            // reload cache
            initialize();
            // enable again
            enabled = true;
          }
        }
        finally {
          readLock.lock();    // get readlock again
          if (writeLocked) {
            writeLock.unlock();
          }
        }
      }
      
      // the map of applicable security rules
      SortedMap<?,Security> map;
      String className = AppDbObject.class.isAssignableFrom(clazz) ? 
                            StringHelper.getClassBaseName(clazz) : clazz.getName();
      
      if (objectId == 0)  {
        // for all objects of the class or class is not a AppDbObject
        map = classMap.subMap(new ClassKey(className, 0), 
                              new ClassKey(className, Integer.MAX_VALUE));
      }
      else if (objectId == -1) {
        // any object rule, i.e. all object rules for given class
        map = clsObjMap.subMap(new Object2Key(className, 0, 0), 
                               new Object2Key(className, Long.MAX_VALUE, Integer.MAX_VALUE));
      }
      else  {
        // for a given object
        map = objectMap.subMap(new ObjectKey(objectId, className, 0), 
                               new ObjectKey(objectId, className, Integer.MAX_VALUE));
      }
      
      // walk through the security settings of the map
      // they are sorted by priority!
      for (Security sec: map.values())  {
        if (AppworxGlobal.logger.isFineLoggable()) {
          AppworxGlobal.logger.fine("evaluate " + sec);
        }
        // check if rule fires
        if (sec.evaluate(contextDb, permission)) {
          // rule applies!
          if (AppworxGlobal.logger.isFineLoggable()) {
            AppworxGlobal.logger.fine(sec.getAllowed() ? "-> GRANT_ACCEPT" : "-> GRANT_DENY");
          }
          return new SecResultImpl(sec.getAllowed() ? GRANT_ACCEPT : GRANT_DENY, sec.getMessage(), sec);
        }
      }
    }
    finally {
      readLock.unlock();
    }
      
    
    if (AppworxGlobal.logger.isFineLoggable()) {
      AppworxGlobal.logger.fine("no rule matched -> GRANT_DEFAULT");
    }
    
    return new SecResultImpl();   // no rule applies, GRANT_DEFAULT
  }
  

  /**
   * Checks a privilege for class only.
   *
   * @param clazz      the class, for example a FormDialog or an AppDbObject (never null!)
   * @param contextDb  the context-Db the object or class is used in, null = all
   * @param permission the requested permission(s)
   * @return a SecurityResult
   * @see #privilege(java.lang.Class, org.tentackle.appworx.ContextDb, long, int) 
   */
  public SecurityResult privilege(Class clazz, ContextDb contextDb, int permission)  {
    return privilege(clazz, contextDb, 0, permission);
  }
    

  /**
   * Checks a privilege for an {@link AppDbObject}.<br>
   * Usually the object-ID rules come first and then the objectclass-rules.
   * This combines the two.
   * Note: if contextDb is null, the context from the obj will be used.
   * However, usually only the basecontext makes sense.
   *
   * @param obj the database object (never null!)
   * @param contextDb the optional database context the object is used in, null = object's contextDb
   * @param permission the requested permission(s)
   * @return a SecurityResult
   * @see #privilege(java.lang.Class, org.tentackle.appworx.ContextDb, long, int) 
   */
  public SecurityResult privilege(AppDbObject obj, ContextDb contextDb, int permission)  {
    SecurityResult grant = null;
    if (obj != null)  {
      if (contextDb == null) {
        contextDb = obj.getContextDb();
      }
      grant = privilege(obj.getClass(), contextDb, obj.getId(), permission);
      if (grant.getSecurity() == null) {
        // no explicit rule fired: check class
        grant = privilege(obj.getClass(), contextDb, permission);
      }
    }
    return grant == null ? new SecResultImpl() : grant;
  }
  
  
  /**
   * Checks a privilege for an {@link AppDbObject}.<br>
   * Usually the object-ID rules come first and then the objectclass-rules.
   * This combines the two.
   *
   * @param obj the database object (never null!)
   * @param permission the requested permission(s)
   * @return a SecurityResult
   * @see #privilege(org.tentackle.appworx.AppDbObject, org.tentackle.appworx.ContextDb, int) 
   */
  public SecurityResult privilege(AppDbObject obj, int permission)  {
    return privilege(obj, null, permission);
  }
  
  
    
  
  /**
   * Initializes the ACL.<br>
   * Must be overridden to implement roles.
   * Example:
   * <pre>
  protected void initialize() {
    super.initialize();
    // get all role IDs the user belongs to
    for (Role r: new Role(getContextDb()).selectByUserId(getUserId()))  {
      addForGrantId(r.getId());
    }
  } 
   * </pre>
   */
  protected void initialize() {
    
    classMap  = new TreeMap<ClassKey,Security>();
    objectMap = new TreeMap<ObjectKey,Security>();
    clsObjMap = new TreeMap<Object2Key,Security>();
    
    // load settings for all users
    addForGrantId(0);
    
    // load settings for this user
    addForGrantId(userId);
    
    if (AppworxGlobal.logger.isFineLoggable()) {
      AppworxGlobal.logger.fine("maps initialized");
    }
  }

  
  
  /**
   * Adds the {@link Security} rules for a given grantee ID
   * to the ACL cache.
   * 
   * @param grantId the grantee ID
   */
  @SuppressWarnings("unchecked")
  protected void addForGrantId(long grantId)  {
    // add all rules for given grant ID
    Collection<Security> rules;
    if (serverContextDb != null) {
      synchronized(SecurityManager.class) {
        long currentValidCount = validCount.get();
        if (serverValidCount != currentValidCount) {
          // need reload of the rules set
          rulesSet = new TreeSet<Security>(new SecurityGranteeComparator());
          Security sec = newSecurityInstance();
          sec.setContextDb(serverContextDb);
          rulesSet.addAll((List<Security>)sec.selectAllInContext());
          serverValidCount = currentValidCount;
        }
        // rules set is up to date: determine the rules for given grantId
        Security fromSec = newSecurityInstance();
        fromSec.setGrantId(grantId);
        Security toSec = newSecurityInstance();
        toSec.setGrantId(grantId + 1);
        rules = rulesSet.subSet(fromSec, toSec);
      }
    }
    else  {
      // select from database
      rules = newSecurityInstance().selectByGrantId(grantId);
    }
    
    // append all rules to this manager instance
    for (Security s: rules) {
      if (s.getObjectId() == 0) {
        classMap.put(new ClassKey(s), s);
      }
      else  {
        objectMap.put(new ObjectKey(s), s);
        clsObjMap.put(new Object2Key(s), s);
      }
    }
  }
  
  
  /** 
   * Determines whether this manager is enabled.
   * 
   * @return true if enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }  
  
  /**
   * Enables/disables this security manager.<br>
   * If it is disabled, all requests return GRANT_DEFAULT, which usually 
   * will be treated by the application as GRANT_ACCEPT.
   * If enabled, the real GRANT will be returned.
   * Notice that an enabled manager will return GRANT_DENY if the userId is 0!
   *
   * @param enabled New value of property enabled.
   * @see #setAcceptByDefault(boolean) 
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }  
  
  
  
  /**
   * Determines whether this manager accepts by default,
   * i.e. if no matching rule is found for a privilege.
   * 
   * @return true if accept by default
   */
  public boolean isAcceptByDefault() {
    return acceptByDefault;
  }  
  
  /**
   * Sets the default behaviour for "no rules found" for the
   * {@link SecurityResult} to "accepted".
   * This is the default.
   * <p>
   * Notice: setting both acceptByDefault and denyByDefault to false will
   * return "neither accepted nor denied" security results if no rules fire.
   * 
   * @param acceptByDefault true if {@link SecurityResult#isAccepted} returns true if no rules found.
   */
  public void setAcceptByDefault(boolean acceptByDefault) {
    this.acceptByDefault = acceptByDefault;
    if (acceptByDefault)  {
      setDenyByDefault(false);
    }
  }  
  
  
  /**
   * Determines whether this manager denies by default,
   * i.e. if no matching rule is found for a privilege.
   * 
   * @return true if deny by default
   */
  public boolean isDenyByDefault() {
    return denyByDefault;
  }
  
  /**
   * Sets the default behaviour for "no rules found" for the
   * {@link SecurityResult} to "denied". The default is false.
   * <p>
   * Notice: setting both acceptByDefault and denyByDefault to false will
   * return "neither accepted nor denied" security results if no rules fire.
   * 
   * @param denyByDefault true if {@link SecurityResult#isDenied} returns true if no rules found.
   */
  public void setDenyByDefault(boolean denyByDefault) {
    this.denyByDefault = denyByDefault;
    if (denyByDefault)  {
      setAcceptByDefault(false);
    }
  }
  
  
  
  
  
  
  // key for a class-oriented rule
  private class ClassKey implements Comparable<ClassKey> {
    String className;           // the class (never null!)
    int    priority;            // ordering
    
    // construct the entry
    public ClassKey(Security sec)  {
      className = sec.getObjectClass();
      priority  = sec.getPriority();
    }
    
    // to get the first/last key
    public ClassKey(String className, int priority) {
      this.className = className;
      this.priority  = priority;
    }
    
    
    public int compareTo(ClassKey k) {
      int rv = className.compareTo(k.className);
      if (rv == 0)  {
        rv = priority - k.priority;
      }
      return rv;
    }
  }
  
  
  // key for object-oriented rules (fast at access by objectIds)
  private class ObjectKey implements Comparable<ObjectKey> {
    long   objectId;            // object the rules applies to
    String objectClass;         // the objects class
    int    priority;            // ordering
    
    // construct the map key
    public ObjectKey(Security sec)  {
      objectId    = sec.getObjectId();
      objectClass = sec.getObjectClass();
      priority    = sec.getPriority();
    }
    
    // to get the first/last key
    public ObjectKey(long objectId, String objectClass, int priority) {
      this.objectId     = objectId;
      this.objectClass  = objectClass;
      this.priority     = priority;
    }

    
    public int compareTo(ObjectKey k) {
      int rv = Compare.compareLong(objectId, k.objectId);
      if (rv == 0)  {
        rv = objectClass.compareTo(k.objectClass);
      }
      if (rv == 0) {
        rv = priority - k.priority;
      }
      return rv;
    }
  }
  
  // same as ObjectKey but faster at acces by classnames (for "any" object rule check)
  private class Object2Key extends ObjectKey {
    
    public Object2Key(Security sec)  {
      super(sec);
    }
    
    public Object2Key(String objectClass, long objectId, int priority) {
      super(objectId, objectClass, priority);
    }

    @Override
    public int compareTo(ObjectKey k) {
      // class first!
      int rv = objectClass.compareTo(k.objectClass);
      if (rv == 0)  {
        rv = Compare.compareLong(objectId, k.objectId);
      }
      if (rv == 0) {
        rv = priority - k.priority;
      }
      return rv;
    }
  }
  
  
  
  
  
  
  // implements the result returned for a request
  
  private class SecResultImpl implements SecurityResult {
    
    private int grantType;      // one of GRANT_...
    private String message;     // further explanation
    private Security security;  // fired security object
    
    
    public SecResultImpl(int grantType, String message, Security security)  {
      this.grantType = grantType;
      this.message   = message;
      this.security  = security;
    }
    
    public SecResultImpl() {
      grantType = GRANT_DEFAULT;
    }
    
    public Security getSecurity() {
      return security;
    }
    
    public String explain(String defaultMessage) {
      return message == null ? defaultMessage : 
                               (defaultMessage + "\n" + message);
    }    
    
    public boolean isAccepted() {
      return grantType == GRANT_ACCEPT || (acceptByDefault && grantType == GRANT_DEFAULT);
    }
    
    public boolean isDefault() {
      return grantType == GRANT_DEFAULT;
    }
    
    public boolean isDenied() {
      return grantType == GRANT_DENY || (denyByDefault && grantType == GRANT_DEFAULT);
    }
    
  }
  
}
