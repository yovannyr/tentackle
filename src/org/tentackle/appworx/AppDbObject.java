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

// $Id: AppDbObject.java 478 2009-08-09 19:16:24Z harald $

package org.tentackle.appworx;

import org.tentackle.appworx.rmi.AppDbObjectRemoteDelegate;
import org.tentackle.db.Db;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbObject;
import org.tentackle.db.DbObjectClassVariables;
import org.tentackle.db.DbRuntimeException;
import org.tentackle.db.PreparedStatementWrapper;
import org.tentackle.db.ResultSetWrapper;
import org.tentackle.db.SqlHelper;
import org.tentackle.ui.FormContainer;
import org.tentackle.ui.FormTableEntry;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.Compare;
import org.tentackle.util.ObjectHelper;
import org.tentackle.util.StringHelper;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.tentackle.appworx.rmi.QbfCursorResult;
import org.tentackle.util.TrackedArrayList;





/**
 * Application database object.<br>
 * Extends {@link DbObject} with features necessary in nearly all
 * desktop enterprise applications.
 * <p>
 * In contrast to DbObject, which is associated to a {@link Db},
 * an AppDbObject is associated to a {@link ContextDb}.
 *
 * @author harald
 */
public abstract class AppDbObject extends DbObject implements ContextDependable {
  
  private ContextDb contextDb;                    // application database context
  private transient AppDbObject copiedObject;     // != null if this object is a "copy" of copiedObject
  
  // Optional Feature to detect that an object is being edited by another user.
  /** the object ID of the user currently editing this object **/
  public static final String FIELD_EDITEDBY      = "editedby";    // NOI18N
  /** time when editing started **/
  public static final String FIELD_EDITEDSINCE   = "editedsince";    // NOI18N
  /** time when "lock token" will expire **/
  public static final String FIELD_EDITEDEXPIRY  = "editedexpiry";    // NOI18N
  
  private long      editedBy;        // if != 0 : holds the userId of token holder
  private Timestamp editedSince;     // if editedBy!=0: time since token given to user, if 0: time since token released
  private Timestamp editedExpiry;    // time when token expires
  
  // for AppDbObjectCache
  private transient long cacheAccessCount;        // access counter (if caching strategy is MOU)
  private transient long cacheAccessTime;         // last access time (if caching strategy is LRU)
  private transient boolean expired;              // true = object is expired in all caches it belongs to
  
  // lazy method optimization
  private transient boolean showableLazy;
  private transient boolean showableLazyValid;
  
  /**
   * all AppDbObjects have a "NormText", i.e. a phonetically normalized text which can be searched
   * made protected here, so all subclasses can directly access it.
   */
  private String normText;
  
  /** column name for the normtext **/
  public static final String FIELD_NORMTEXT = "normtext";    // NOI18N
 
  // transaction names
  /** save copy in context **/
  public static final String TX_SAVE_COPY_IN_CONTEXT    = "save copy in context";
  /** delete all in context **/
  public static final String TX_DELETE_ALL_IN_CONTEXT   = "delete all in context";
  /** transfer edited by **/
  public static final String TX_TRANSFER_EDITEDBY       = "transfer edited by";
  
  
  private static String[] classPath = { StringHelper.emptyString };       // classpath for loadClass()
  
  // the default name for a FormTable of this class of objects
  private static final String formTableName = "appDbObjectDefaultTable";    // NOI18N

  
  
  // predefined errorcode for the verify() methods
  
  /** saving this object will result in a unique violation **/
  public static final int VERIFIED_DUPLICATE = -1;
  
  
  
  
  /**
   * Creates an application database object.
   *
   * @param contextDb the database context
   */
  public AppDbObject(ContextDb contextDb) {
    super(contextDb == null ? null : contextDb.getDb());
    setContextDb(contextDb);
  }
  
  /**
   * Creates an application database object without a database context.
   * Extendes classes must also implement the empty constructor!
   */
  public AppDbObject() {
    super();
  }

  
  
  
  /**
   * Sets the normtext.
   * @param normText the normtext
   */
  public void setNormText (String normText) {
    if (!Compare.equals(this.normText, normText)) {
      setModified(true);
    }
    this.normText = normText;
  }
  
  /**
   * Gets the normtext.
   * 
   * @return the normtext
   */
  public String getNormText() {
    return normText;
  }
  
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the Db in ContextDb as well.
   */
  @Override
  public void setDb(Db db)  {
    if (contextDb != null)  {
      contextDb.setDb(db);
    }
    super.setDb(db);
  }


  
  
  // ---------------- implements ContextDependable -------------------------
  
  /**
   * {@inheritDoc}
   * <p>
   * Setting the context will also {@link DbObject#setDb} and {@link #setContextId}
   */
  public void setContextDb(ContextDb contextDb)  {
    this.contextDb = contextDb;
    if (contextDb != null)  {
      if (getDb() != contextDb.getDb()) {  // avoid loops in object references
        super.setDb(contextDb.getDb());
      }
      // add context. Must be given according to type!
      // if the context is too weak according to type, an exception will be thrown.
      setContextId();
    }
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation just returns the context.
   * Subclasses may override this with a covariant method.
   */
  public ContextDb getContextDb() {
    return contextDb;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing (object living in a context
   * not depending on another object).
   */
  public void setContextId() {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns 0.
   */
  public long getContextId() {
    return 0;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns null.
   */
  public String getSqlContextCondition() {
    return null;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns a new ContextDb.
   */
  public ContextDb getBaseContext()  {
    return new ContextDb(getDb());
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation just returns a new {@link ContextDb}.<br>
   * Notice that the {@link Db} must be valid!
   */
  public ContextDb makeValidContext() {
    return new ContextDb(getDb());
  }
  

  // ---------------- end ContextDependable ---------------------------------
  
  
  

  
  /**
   * Gets the application oriented class variables for this object.<br>
   * Class variables for classes derived from AppDbObject are kept in an
   * instance of {@link AppDbObjectClassVariables}.<p>
   * Notice: we cannot use a covariant method getDbObjectClassVariables() returning
   * AppDbObjectClassVariables because super.getDbObjectClassVariables() is abstract
   * and in that case covariance does not work. You will get the compiler 
   * error: 
   * "abstract method getDbObjectClassVariables() in org.tentackle.db.DbObject cannot be accessed directly"
   * @return the class variables
   * @see DbObject#getDbObjectClassVariables
   */
  public abstract AppDbObjectClassVariables getAppDbObjectClassVariables();
  
  
  /**
   * {@inheritDoc}
   * <p>
   * In fact, {@link AppDbObjectClassVariables} is an extension of {@link DbObjectClassVariables}.
   * So this fullfills the requirements for DbObject
   */
  public DbObjectClassVariables getDbObjectClassVariables() {
    return getAppDbObjectClassVariables();
  }
  
  
  
  /**
   * Tells whether this object is composite (i.e. has composite relations).<br>
   * The method is overridden by the AppDbRelations-wurblet if at least one
   * relation is flagged as "composite".
   * The default implementation returns false.
   *
   * @return true if object is composite, false if not.
   */
  public boolean isComposite() {
    return false;
  }
  
  
  /**
   * Loads all composites.<br>
   * The method is used to transfer a copy of an object between tiers including all
   * composite object relations recursively.
   * By default the method throws a DbRuntimeException telling that it is not implemented
   * if isComposite() returns true.
   * The --loadcomposites option of the AppDbRelations wurblet will generate the method.
   * Applications should not override this method if AppDbRelations is used.
   *
   * @return true if composites loaded or object is not composite, false if loading composites failed
   */
  public boolean loadComposites() {
    if (isComposite()) {
      throw new DbRuntimeException("method not implemented");
    }
    return true;    // ok.
  }
  
  
  /**
   * Checks whether this object (if saved) would violate any
   * unique constraints.
   * <p>
   * The method is usually used by the presentation layer
   * to check for duplicates.
   * The default implementation does nothing and returns null.
   * 
   * @return the duplicate object, null if no duplicated
   */
  public AppDbObject findDuplicate() {
    return null;
  }
  
  
  /**
   * Verifies the consistency and constraints of this object.
   * <p>
   * The method is usually invoked by the presentation layer
   * before save().
   * The default implementation checks for duplicates via {@link #findDuplicate()}.
   * 
   * @return a list of {@link InteractiveError}s, never null.
   * @see #VERIFIED_DUPLICATE
   */
  public List<InteractiveError> verify() {
    List<InteractiveError> errorList = new ArrayList<InteractiveError>();
    if (findDuplicate() != null) {
      errorList.add(new InteractiveError(VERIFIED_DUPLICATE,
                    MessageFormat.format(Locales.bundle.getString("{0}_{1}_already_exists"), 
                                         getSingleName(), toString())));
    }
    return errorList;
  }
  
  
  /**
   * Determines whether object is cacheable or not.
   * The default implementation always returns true, but apps
   * can use this as a filter.
   * @return true if object is cacheable
   */
  public boolean isCacheable()  {
    return true;
  }
  
  
  /**
   * Checks whether object has been marked expired.
   * Expired objects will be reloaded from the database by
   * the cache when the object is retrieved again.
   * @return true if object is expired (in cache)
   */
  public boolean isExpired() {
    return expired;
  }

  /**
   * Sets this object's expiration flag.
   * @param expired true if object is expired
   */
  public void setExpired(boolean expired) {
    this.expired = expired;
  }
  
  
  /**
   * Set the last cache access time.
   * @param millis is the system time in ms.
   */
  public void setCacheAccessTime(long millis) {
    cacheAccessTime = millis;
  }
  
  /**
   * Gets the last cache access time.
   * @return the last cache access time
   */
  public long getCacheAccessTime()  {
    return cacheAccessTime;
  }
  
  /**
   * Count cache access
   */
  public void countCacheAccess()  {
    cacheAccessCount++;
  }
  
  /**
   * Sets the cache access count.
   * @param count new access count
   */
  public void setCacheAccessCount(long count)  {
    cacheAccessCount = count;
  }
  
  /**
   * Gets the cache access count.
   * 
   * @return the access count
   */
  public long getCacheAccessCount() {
    return cacheAccessCount;
  }
  

  /**
   * mark cache access (count and set current system-time)
   */
  public void markCacheAccess() {
    countCacheAccess();
    setCacheAccessTime(System.currentTimeMillis());
  }
  
  /**
   * @return true if object is cached
   */
  public boolean isCached() {
    return cacheAccessCount > 0;
  }
  
  

  /**
   * Gets the {@link SecurityManager} for this object.
   * 
   * @return the manager. Never null.
   */
  public SecurityManager getSecurityManager() {
    try {
      SecurityManager manager = getContextDb().getAppUserInfo().getSecurityManager();
      if (manager == null)  {
        throw new ApplicationException(Locales.bundle.getString("Security_Manager_not_installed"));
      }
      return manager;
    }
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(getDb(), ex, Locales.bundle.getString("security_manager_configuration_error"));
      return null; // notreached
    }
  }
  
  
  /**
   * Creates the {@link SecurityResult} for a given permission.
   * @param permission the permission type
   * @return the security result (never null)
   */
  public SecurityResult getSecurityResult(int permission) {
    return getSecurityManager().privilege(this, permission);
  }
  
  
  /**
   * Checks if the privilege for a given permission is denied.
   * @param permission the permission type
   * @return true if privilege is denied
   */
  public boolean isPermissionDenied(int permission) {
    return getSecurityResult(permission).isDenied();
  }
  
  
  /**
   * Checks if the privilege for a given permission is accepted.
   * @param permission the permission type
   * @return true if privilege is accepted
   */
  public boolean isPermissionAccepted(int permission) {
    return getSecurityResult(permission).isAccepted();
  }
  
  
  
  /**
   * Checks if the privilege for a given permission is the default privilege.
   * The default privilege is returned if there were no rules found.
   * 
   * @param permission the permission type
   * @return true if privilege is default (no rule found)
   */
  public boolean isPermissionDefault(int permission) {
    return getSecurityResult(permission).isDefault();
  }
    
  
  
  /**
   * Creates a new object as a copy of the current object in another 
   * (or the same) database context.<br>
   * The new context must be of the same class as the old one.
   * Subclasses must override this method if they contain links to
   * other objects (via IDs) that may have changed too (depending on
   * the context).<br>
   * The default implementation just clones the object, sets the context
   * and sets the original object (see {@link #getCopiedObject}).
   * @param otherDb the database context
   * @return the created object
   */
  public AppDbObject createCopyInContextDb(ContextDb otherDb) {
    // first we have to clone() ourself. This gives a "new" object in the same context
    AppDbObject obj = (AppDbObject)clone();
    // set the new context
    obj.setContextDb(otherDb);
    // set the origin of the copy (so apps may check this)
    obj.copiedObject = this;
    // .. to be followed by ID-replacement code in subclasses (they MUST super.createCopyInContextDb())!!
    return obj;
  }
  
  
  /**
   * Gets the original object if this object was created by {@link #createCopyInContextDb}.
   * @return the object or null if this object is not a copy
   */
  public AppDbObject getCopiedObject()  {
    return copiedObject;
  }
  
  
  /**
   * Same as {@link #createCopyInContextDb} but saves the new object within a transaction.<br>
   * The default implementation also creates the {@link Security}-rules for the new object
   * @param otherDb the database context
   * @return the created object
   * @throws ApplicationException if save failed
   */
  public AppDbObject saveCopyInContextDb(ContextDb otherDb) throws ApplicationException {
    boolean oldCommit = beginTx(TX_SAVE_COPY_IN_CONTEXT);
    // create the new object
    AppDbObject object = createCopyInContextDb(otherDb);
    if (object.save() == false) {
      getDb().rollback(oldCommit);
      throw new ApplicationException(MessageFormat.format(
              Locales.bundle.getString("created_{0}_in_context_{1}_could_not_be_saved"), 
              object.getClassBaseName(), otherDb)); 
    }
    if (getSecurityManager().isEnabled()) {
      // create Security rules in the new context
      List<Security> list = Security.createSecurityForObjectInOtherDb(this, object);
      if (saveList(list) != list.size()) {
        getDb().rollback(oldCommit);
        throw new ApplicationException(MessageFormat.format(
                "creating security rules for {0} in context {1} failed", 
                object.getClassBaseName(), otherDb));
      }
    }
    getDb().commit(oldCommit);
    return object;
  }
  
  
  /**
   * Selects an object according to a template object.
   * Useful to find corresponding objects in another context.
   * The default implementation loads the same object by its ID.
   * Should be overwridden to select by some other unique key.
   *
   * @param template the template object
   * @return the object if found, else null.
   */
  public AppDbObject selectByTemplate(AppDbObject template) {
    return select(template.getId());
  }
  
  
  /** 
   * Creates an instance of the same class as this object,
   * initialized in the same contextDb.
   *
   * @return the new object
   */
  @Override
  public AppDbObject newObject() {
    AppDbObject obj = (AppDbObject)super.newObject();
    obj.setContextDb(contextDb);
    return obj;
  }
  
  
  /**
   * Creates a "fast copy" of this object.
   * <p>
   * The method is provided as a fast alternative to deep cloning
   * via reflection or serialization.
   * <p>
   * All database-attributes are copied in such a way that
   * each attribute of the new object can be changed without modifying 
   * the state of the original object. This also applies to all related
   * AppDbObjects along the object graph. Non-AppDbObjects or any other
   * non-database attributes are initialized according to the object's
   * constructor (i.e. will usually be initialized to false, 0 or null, respectively).
   * <p>
   * The method will create a new object (no clone!) and set all 
   * attributes the same way as if the object has been loaded from storage.
   * <p>
   * Notice that it differs semantically from createCopyInContextDb() as
   * copy() will copy the identity (ID) from the source object as well and
   * will retain the contextDb.
   * Thus, upon return we're talking about another instance of the "same" object
   * with respect to what the database considers as "same".
   * <p>
   * A copied object is always not modified and not deleted, 
   * as it is when loaded from the database.
   * <p>
   * The default implementation throws a DbRuntimeException, i.e.
   * must be overridden. See the AppDbCopy wurblet for an implementation.
   *
   * @return the copied object
   * @see #copyCollection
   */
  public AppDbObject copy() {
    throw new DbRuntimeException("copy() not implemented for " + getClassName());
  }
  


  
  /**
   * Selects an object by its unique ID in a newly created context.<br>
   * Notice that {@link #loadLinkedObjects} is invoked _after_ {@link #makeValidContext}!
   * Furthermore, this method does not work well with multi-table objects (see {@link PartialAppDbObject})
   * and eager loading because it invokes {@link #getFields} directly instead of 
   * {@link #readFromResultSetWrapper}.
   */
  @Override
  public AppDbObject selectInValidContext(long id, boolean withLinkedObjects) {
    
    if (getDb().isRemote()) {
      return (AppDbObject)super.selectInValidContext(id, withLinkedObjects);
    }
    
    PreparedStatementWrapper st = getDb().getPreparedStatement(prepareSelectStatement());
    st.setLong(1, id);
    ResultSetWrapper rs = st.executeQuery();
    if (rs.next() && getFields(rs)) {
      setContextDb(makeValidContext());
      if (!withLinkedObjects || loadLinkedObjects() &&
          getAppDbObjectClassVariables().isReadAllowed(this)) {
        rs.close();
        return this;
      }
    }
    rs.close();
    return null;
  }
  
  /**
   * Selects an object by its unique ID in a newly created context
   * (with linked objects).
   * 
   * @param id the object id
   * @return the object, null if no such object
   * @see #selectInValidContext(long, boolean) 
   */
  public AppDbObject selectInValidContext(long id) {
    return selectInValidContext(id, true);
  }
  
  /**
   * Selects an object for a given db connection by its unique ID
   * in a newly created context.
   * 
   * @param db the db connection
   * @param id the object id
   * @param withLinkedObjects true if load linked objects too
   * @return the object, null if no such object
   * @see #selectInValidContext(long, boolean) 
   */
  public AppDbObject selectInValidContext(Db db, long id, boolean withLinkedObjects) {
    setDb(db);
    return selectInValidContext(id, withLinkedObjects);
  }
  
  /**
   * Selects an object for a given db connection by its unique ID
   * in a newly created context
   * (with linked objects).
   * 
   * @param db the db connection
   * @param id the object id
   * @return the object, null if no such object
   * @see #selectInValidContext(Db, long, boolean) 
   */
  public AppDbObject selectInValidContext(Db db, long id) {
    setDb(db);
    return selectInValidContext(id);
  }
  

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to security check whether reading this
   * object is allowed. If not, false is returned as if reading
   * some values failed.
   */
  @Override
  public boolean readFromResultSetWrapper(ResultSetWrapper rs, boolean withLinkedObjects)  {
    return super.readFromResultSetWrapper(rs, withLinkedObjects) && 
           getAppDbObjectClassVariables().isReadAllowed(this);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because we need to set the contextDb in remote connections.
   */
  @Override
  public AppDbObject select(long id, boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        /**
         * Because the method is overridden from DbObject we must check
         * which of the remote methods to use.
         * If contextDb is null, select() usually has been invoked from DbObject.select...
         */
        AppDbObject obj = contextDb == null ?
                            (AppDbObject)getRemoteDelegate().select(id, withLinkedObjects) :
                            getRemoteDelegate().select(contextDb, id, withLinkedObjects);
        if (obj != null) {
          if (contextDb != null)  {
            // set the context
            obj.setContextDb(contextDb);
          }
          else  {
            // no context: set at least the db
            obj.setDb(getDb());
          }
        }
        return obj;     // obj != this !!!
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_select_failed"));
        return null;
      }
    }
    else  {
      return (AppDbObject)super.select(id, withLinkedObjects);    // obj == this
    }    
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because we need to set the contextDb in remote connections.
   */
  @Override
  public AppDbObject select(long id)  {
    return select(id, true);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because we need to set the contextDb in remote connections.
   */
  @Override
  public AppDbObject selectLocked(long id, boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        AppDbObject obj = contextDb == null ?
                            (AppDbObject)getRemoteDelegate().selectLocked(id, withLinkedObjects) :
                            getRemoteDelegate().selectLocked(contextDb, id, withLinkedObjects);
        if (obj != null) {
          if (contextDb != null)  {
            // set the context
            obj.setContextDb(contextDb);
          }
          else  {
            // no context: set at least the db
            obj.setDb(getDb());
          }
        }
        return obj;     // obj != this !!!
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_selectLocked_failed"));
        return null;
      }
    }
    else  {
      return (AppDbObject)super.selectLocked(id, withLinkedObjects);    // obj == this
    }    
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because we need to set the contextDb in remote connections.
   */  
  @Override
  public AppDbObject selectLocked(long id)  {
    return selectLocked(id, true);
  }
  
  
  /**
   * Selects all objects in their corresponding context.<br>
   * Because objects of the same table may live in different ContextDb
   * makeValidContext is invoked for each object.
   * Notice that {@link #loadLinkedObjects} is invoked _after_ {@link #makeValidContext}.
   * The method overrides {@link DbObject#selectAll}.
   * 
   * @see #selectAllInValidContext(boolean)
   */
  @Override
  public List<? extends AppDbObject> selectAll(boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        List<? extends AppDbObject> list = getRemoteDelegate().selectAll(withLinkedObjects);
        // sets the db to the possible different contextDb/and objects
        Db.applyToCollection(getDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectAll failed");
        return null;
      }
    }
    else  {
      List<AppDbObject> list = new ArrayList<AppDbObject>();
      ResultSetWrapper rs = selectAllResultSet();
      while (rs.next()) {
        AppDbObject obj = (AppDbObject)super.newObject();   // initialize db only
        if (obj.getFields(rs)) {
          obj.setContextDb(obj.makeValidContext());
          if ((!withLinkedObjects || loadLinkedObjects()) &&
              getAppDbObjectClassVariables().isReadAllowed(obj)) {
            list.add(obj);
          }
        }
      }
      rs.close();
      return list;      
    }
  }
  
  
  /**
   * The method {@link #selectAll} should get the suffix "InValidContext"
   * but it must override {@link DbObject#selectAll}. This method just
   * fixes the naming convention and invokes selectAll.
   * 
   * @param withLinkedObjects true if load linked objects too
   * @return the list of objects
   */
  public List<? extends AppDbObject> selectAllInValidContext(boolean withLinkedObjects) {
    return selectAll(withLinkedObjects);
  }
  
  
  /**
   * Selects all objects in their corresponding context (with linked objects).
   * 
   * @return the list of objects
   * @see #selectAll(boolean)
   */
  public List<? extends AppDbObject> selectAllInValidContext() {
    return selectAll(true);
  }


  /**
   * Gets the result set for the normtext query (where normtext like ?)
   * for use in lists.<br>
   * If the given normtext is null or just "%" the method falls back
   * to {@link #resultAllInContext}.
   * 
   * @param normText the normtext to search for
   * @return the result set
   * @see #resultByNormTextCursor
   */
  public ResultSetWrapper resultByNormText(String normText) {
    
    if (normText == null || "%".equals(normText)) {    // NOI18N
      return resultAllInContext();
    }
    
    getDb().assertNotRemote();

    int stmtId = getSelectByNormTextStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      // Notice: no getSqlPrefixWithDot() before FIELD_NORMTEXT because normtext 
      // must be defined only once in an inheritance path and all dbms will pick
      // the right one, regardless where declared.
      String sql = getSqlSelectAllFields() + " AND " +  // NOI18N
                   FIELD_NORMTEXT + " LIKE ?";          // NOI18N
      String condition = getSqlContextCondition();
      if (condition != null) {
        sql += condition;
      }
      String orderSuffix = orderBy();
      if (orderSuffix != null)  {
        sql += " ORDER BY " + orderSuffix;    // NOI18N
      }
      // prepare statement according to context
      stmtId = getDb().prepareStatement(sql);
      setSelectByNormTextStatementId(stmtId);
    }

    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);

    st.setString(1, normText);
    long contextId = getContextId();
    if (contextId != 0) {
      st.setLong(2, contextId);
    }

    return st.executeQuery();    
  }
  
  
  /**
   * Gets the result set for the normtext query (where normtext like ?)
   * for use in cursors.<br>
   * Cursors differ from lists because they need {@link ResultSet#TYPE_SCROLL_INSENSITIVE}
   * set.
   * If the given normtext is null or just "%" the method falls back
   * to {@link #resultAllInContext}.
   * 
   * @param normText the normtext to search for
   * @return the result set
   * @see #resultByNormText
   */
  public ResultSetWrapper resultByNormTextCursor(String normText) {
    
    if (normText == null || "%".equals(normText)) {    // NOI18N
      return resultAllInContextCursor();
    }
    
    getDb().assertNotRemote();

    int stmtId = getSelectByNormTextCursorStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      // Notice: no getSqlPrefixWithDot() before FIELD_NORMTEXT because normtext 
      // must be defined only once in an inheritance path and all dbms will pick
      // the right one, regardless where declared.
      String sql = getSqlSelectAllFields() + " AND " +  // NOI18N
                   FIELD_NORMTEXT + " LIKE ?";          // NOI18N
      String condition = getSqlContextCondition();
      if (condition != null) {
        sql += condition;
      }
      String orderSuffix = orderBy();
      if (orderSuffix != null)  {
        sql += " ORDER BY " + orderSuffix;    // NOI18N
      }
      // prepare statement according to context
      stmtId = getDb().prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE);
      setSelectByNormTextCursorStatementId(stmtId);
    }

    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);

    st.setString(1, normText);
    long contextId = getContextId();
    if (contextId != 0) {
      st.setLong(2, contextId);
    }

    return st.executeQuery();    
  }
  
  
  
  /**
   * Selects all objects with a given normtext as a list.
   *
   * @param normText the normtext
   * @return the list of objects, never null
   * @see #resultByNormText
   */
  public List<? extends AppDbObject> selectByNormText(String normText) {
    if (getDb().isRemote())  {
      try {
        List<? extends AppDbObject> list = getRemoteDelegate().selectByNormText(contextDb, normText);
        ContextDb.applyToCollection(contextDb, list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_selectByNormText_failed"));
        return null;
      }
    }
    else  {
      List<AppDbObject> list = new ArrayList<AppDbObject>();
      ResultSetWrapper rs = resultByNormText(normText);
      while (rs.next()) {
        AppDbObject obj = newObject();
        if (obj.readFromResultSetWrapper(rs)) {
          list.add(obj);
        }
      }
      rs.close();
      return list;
    }
  }
  
  
  /**
   * Selects all objects with a given normtext as a cursor.
   *
   * @param normText the normtext
   * @return the cursor
   * @see #resultByNormTextCursor
   */
  @SuppressWarnings("unchecked")
  public AppDbCursor<? extends AppDbObject> selectByNormTextCursor(String normText) {
    if (getDb().isRemote())  {
      try {
        return new AppDbCursor<AppDbObject>(contextDb, 
                               getRemoteDelegate().selectByNormTextCursor(contextDb, normText));
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_selectByNormTextCursor_failed"));
        return null;    // not reached
      }
    }
    else  {
      return new AppDbCursor<AppDbObject>(getContextDb(), (Class<AppDbObject>)getClass(), 
                                          resultByNormTextCursor(normText));
    }
  }
  
  
  
  
  /**
   * Gets the result set for all objects in the current context
   * for use in lists.<br>
   * 
   * @return the result set
   * @see #resultAllInContextCursor
   */
  public ResultSetWrapper resultAllInContext()  {
    
    getDb().assertNotRemote();

    int stmtId = getSelectAllInContextStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it */
      String sql = getSqlSelectAllFields();
      String condition = getSqlContextCondition();
      if (condition != null) {
        sql += condition;
      }
      String orderSuffix = orderBy();
      if (orderSuffix != null)  {
        sql += " ORDER BY " + orderSuffix;    // NOI18N
      }
      // prepare statement according to context
      stmtId = getDb().prepareStatement(sql);
      setSelectAllInContextStatementId(stmtId);
    }

    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    
    long contextId = getContextId();
    if (contextId != 0) {
      st.setLong(1, contextId);
    }

    return st.executeQuery();    
  }
  
  
  /**
   * Gets the result set for all objects in the current context
   * for use in cursors.<br>
   * Cursors differ from lists because they need {@link ResultSet#TYPE_SCROLL_INSENSITIVE}
   * set.
   * 
   * @return the result set
   * @see #resultAllInContext
   */
  public ResultSetWrapper resultAllInContextCursor()  {
    
    getDb().assertNotRemote();

    int stmtId = getSelectAllInContextCursorStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it (all lowercase to fool duplicate prepared statement check)
      String sql = getSqlSelectAllFields();
      String condition = getSqlContextCondition();
      if (condition != null) {
        sql += condition;
      }
      String orderSuffix = orderBy();
      if (orderSuffix != null)  {
        sql += " ORDER BY " + orderSuffix;    // NOI18N
      }
      // prepare statement according to context
      stmtId = getDb().prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE);
      setSelectAllInContextCursorStatementId(stmtId);
    }

    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    
    long contextId = getContextId();
    if (contextId != 0) {
      st.setLong(1, contextId);
    }
    
    return st.executeQuery();    
  }
  
  
  /**
   * Selects all records in current context as a list.
   * 
   * @return the list of objects, never null
   * @see #resultAllInContext
   */
  public List<? extends AppDbObject> selectAllInContext() {
    
    if (getDb().isRemote())  {
      try {
        List<? extends AppDbObject> list = getRemoteDelegate().selectAllInContext(contextDb);
        ContextDb.applyToCollection(contextDb, list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_selectAllInContext_failed"));
        return null;
      }
    }
    else  {
      List<AppDbObject> list = new ArrayList<AppDbObject>();   /* returned list of objects */
      ResultSetWrapper rs = resultAllInContext();
      while (rs.next()) {
        AppDbObject obj = newObject();
        if (obj.readFromResultSetWrapper(rs)) {
          list.add(obj);
        }
      }
      rs.close();
      return list;
    }
  }
  
  
  /**
   * Selects all records in current context as a cursor
   * 
   * @return the cursor
   * @see #resultAllInContextCursor
   */
  @SuppressWarnings("unchecked")
  public AppDbCursor<? extends AppDbObject> selectAllInContextCursor() {
    if (getDb().isRemote())  {
      try {
        return new AppDbCursor<AppDbObject>(getContextDb(),
                                            getRemoteDelegate().selectAllInContextCursor(contextDb));
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_selectAllInContextCursor_failed"));
        return null;    // not reached
      }
    }
    else  {
      return new AppDbCursor<AppDbObject>(getContextDb(), (Class<AppDbObject>)getClass(), 
                                          resultAllInContextCursor());
    }
  }
  
  
  
  /**
   * Runs a QBF-Search returning a cursor.
   * Will prompt the user if too much data received.
   * The method is provided to be overridden in the concrete classes.
   * The default implementation just queries the normtext for the
   * default qbf parameter.
   *
   * @param par the qbf parameter
   * @return the cursor
   */
  @SuppressWarnings("unchecked")
  public AppDbCursor<? extends AppDbObject> searchQbfCursor(QbfParameter par)  {
    
    AppDbCursor<? extends AppDbObject> cursor = null;
    
    if (getDb().isRemote())  {
      try {
        QbfCursorResult result = getRemoteDelegate().searchQbfCursor(par);
        cursor = new AppDbCursor<AppDbObject>(contextDb, result.cursor);
        par.estimatedRowCount = result.estimatedRowCount;
        par.withEstimatedRowCount = false;
        cursor.applyQbfParameterLocalOnly(par);   // apply QBF-Settings (saving a roundtrip for setFetchSize())
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_searchQbfCursor_failed"));
        // not reached
      }
    }
    else  {
      if (par.limit > 0 || par.offset > 0 || par.withEstimatedRowCount) {
        // we need a one-shot query
        return new AppDbQuery(par, this).cursor();
      }
      else  {
        cursor = new AppDbCursor<AppDbObject>(getContextDb(), (Class<AppDbObject>)getClass(),
                                              resultByNormTextCursor(par.patternAsLikeSql()));
        cursor.applyQbfParameter(par);    // apply QBF-Settings
      }
    }
     
    return cursor;
  }
  
  
  /**
   * Runs a QBF-Search returning a list.
   * The implementation simply invokes searchQbfCursor().toListAndClose().
   *
   * @param par the QbfParameter
   * @return the list, never null
   */
  public List<? extends AppDbObject> searchQbf(QbfParameter par)  {
    return searchQbfCursor(par).toListAndClose();
  }
  
  

  
  
  
  /**
   * speichert ein Object in einem anderen Db-Context.
   * Wird z.B. verwendet um Stammdaten in einen anderen Mandant/Jahr zu kopieren.
   * Das Objekt wird NICHT(!) clone()'d, sondern bekommt lediglich
   * eine neue Object-ID und einen neuen Context. Es werden auch
   * keine linked Objects gespeichert, sondern nur das nackte Objekt.
   *
   * @param newDb is the new ContextDb
   * @return true if done, false if error
   */
  public boolean saveInContextDb(ContextDb newDb)  {
    setContextDb(newDb);  // set the new context
    setId(0);             // make it "new"
    newId();              // obtain a new ID
    setSerial(1);         // start with serial reset
    return getAppDbObjectClassVariables().isWriteAllowed(this) && insertPlain(); // insert it in new context
  }
  
  
  
  
  
  /**
   * Gets the cache.
   * The default implementation returns null.
   * Must be overridden to enable optimization features with RMI servers.
   *
   * @return the cache, null if uncached
   */
  public AppDbObjectCache getCache() {
    return null;
  }
  
  
  /**
   * Expires the cache according to the serial numbers.<br>
   * 
   * If objects of this class are cached, the
   * cache must be expired on updates, etc...
   * Furthermore, if there is a cache, isCountingModification() MUST return true,
   * in order for countModification() to invalidate the cache for the local JVM.
   * Classes with a cache must override this method!
   * The implementation with the AppDbObjectCache should look like this:
   * <pre>
   *    cache.expire(maxSerial);
   * </pre>
   * while "cache" has been declared by the wurblet AppDbCache.
   * 
   * @param maxSerial is the new tableSerial this object will get
   * @return true if cache invalidated, false if there is no cache
   * @see AppDbObjectCache
   */
  public boolean expireCache(long maxSerial)  {
    return false;
  }
  
  
  /**
   * Gets the object via cache.<br>
   * If there is no cache (i.e. the method is not overridden), 
   * the default implementation just loads from the db.
   * 
   * @param id the uniue object ID
   * @return the object, null if no such object
   * @see #select(long)
   */
  public AppDbObject selectCached(long id)  {
    return select(id);
  }
  
  
  /**
   * Gets all objects in context via cache.
   * If there is no cache (i.e. the method is not overridden),
   * the default implementation gets it from the db.
   * 
   * @return the list, never null
   * @see #selectAllInContext()
   */
  public List<? extends AppDbObject> selectAllInContextCached()  {
    return selectAllInContext();
  }
    
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to expire the cache if object is using the tableserial.
   */
  @Override
  public long countModification (boolean optimize)  {
    long tableSerial = super.countModification(optimize);
    if (tableSerial >= 0)  {
      expireCache(tableSerial);
    }
    return tableSerial;
  }
  
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the normtext from {@link #toString}.
   */
  @Override
  public boolean prepareSetFields()	{
    setNormText(StringHelper.normalize(toString()));
    return true;
  }
  

  
  /**
   * Determines whether a panel exists to edit or view this object.
   * The default is false.
   * 
   * @return true if a panel exists
   * @see #newPanel()
   */
  public boolean panelExists() {
    return false;
  }
  
  /** 
   * Creates a panel to edit or view this object.
   * The default implementation returns null.
   * 
   * @return the panel, null if none
   * @see #panelExists()
   */
  public FormContainer newPanel() {
    return null;
  }
  
  /**
   * Gets the object that should be edited or viewed in a panel.
   * The default implementation returns this object.
   * 
   * @return the object to be edited or viewed in a panel
   */
  public AppDbObject getPanelObject()  {
    return this;
  }
  
  
  /**
   * Checks whether this object can be viewed by the user.
   * <p>
   * The default implementation returns true.
   * Does not refer to the SecurityManager!
   * <p>
   * The application can assume a lazy context (invoked from is...Lazy) 
   * if invoked outside a transaction. This is just an optimization hint.
   * 
   * @return true if showable
   * @see #isShowableLazy()
   */
  public boolean isShowable() {
    return true;
  }
  
  
  /**
   * Gets the showable state which has been valid for at least the
   * last lazyMethodInterval milliseconds.
   *
   * @return true if showable
   * @see #isShowable()
   */
  public boolean isShowableLazy() {
    assertLazyNotWithinTX();
    if (isLazyElapsed() || showableLazyValid == false) {
      showableLazy = isShowable();
      showableLazyValid = true;
    }
    return showableLazy;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Ovewritten due to {@link #isShowableLazy}.
   */
  @Override
  public void copyLazyValues(DbObject obj) {
    super.copyLazyValues(obj);
    showableLazy = ((AppDbObject)obj).showableLazy;
    showableLazyValid = ((AppDbObject)obj).showableLazyValid;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Ovewritten due to {@link #isShowableLazy}.
   */
  @Override
  public void invalidateLazyValues() {
    super.invalidateLazyValues();
    showableLazyValid = false;  
  }
  
  
  /**
   * Determines whether the user allowed to create a new object.<br>
   * The default is true.
   * 'false' usually means that objects of this class are created (and removed)
   * by the application logic and the user has no control over that.
   * @return true if user is allowed to create (and delete)
   */
  public boolean isInstantiatable() {
    return true;
  }
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden because remote method requires a contextDb instead of just the db-connection.
   */
  @Override
  public boolean isReferenced() {
    boolean rv = false;
    if (getDb().isRemote()) {
      if (!isNew())  {
        // new objects are never referenced because they simply don't exist in the db!
        try {
          rv = getRemoteDelegate().isReferenced(contextDb, getId());
        }
        catch (Exception e) {
          DbGlobal.errorHandler.severe(getDb(), e, "remote isReferenced failed");
        }            
      }
      // else local db: default is false
    }
    return rv;
  };

  

  /**
   * Determines whether this object may have child objects that should
   * be visible in a navigatable tree.<br>
   * The default implementation returns true.
   * 
   * @return true if object may have childs
   * @see AppDbObjectTree
   */
  public boolean allowsTreeChildObjects() {
    return true;
  }
  

  /**
   * Gets all childs of this objects that should be visible to the user
   * in a navigatable object tree.<br>
   * The default implementation returns an empty {@link ArrayList}.
   * Notice that these objects need not necessarily be AppDbObjects.
   *
   * @return the childs
   * @see AppDbObjectTree
   */
  public List<Object> getTreeChildObjects()  {
    if (getDb().isRemote())  {
      try {
        List<Object> list = getRemoteDelegate().getTreeChildObjects(this);
        if (list != null) {
          for (Object obj: list)  {
            if (obj instanceof AppDbObject) {
              ((AppDbObject)obj).setContextDb(contextDb);
            }
            // other objects remain untouched
          }
        }
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_getTreeChildObjects_failed"));
        return null;  // not reached
      }
    }
    return new ArrayList<Object>();
  }
  
  
  /**
   * Gets the childs with respect to the parent object this
   * object is displayed in the current tree.<br>
   * The default implementation just invokes {@link #getTreeChildObjects}.
   * Override if the childs vary according to this object's parent.
   *
   * @param parentObject the parent object of this object in the tree, null = no parent
   * @return the list of childs
   * @see AppDbObjectTree
   */
  public List<Object> getTreeChildObjects(Object parentObject) {
    if (getDb().isRemote())  {
      try {
        List<Object> list = getRemoteDelegate().getTreeChildObjects(this, parentObject);
        if (list != null) {
          for (Object obj: list)  {
            if (obj instanceof AppDbObject) {
              ((AppDbObject)obj).setContextDb(contextDb);
            }
            // other objects remain untouched
          }
        }
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_getTreeChildObjects_failed"));
        return null;
      }
    }
    // default implementation
    return getTreeChildObjects();
  }

  
  /**
   * Gets the maximum level of expansion allowed in a navigatable object tree.
   * The default is 0, i.e. no limit.
   * Override this method to limit the load if node is expanded.
   * 
   * @return the maximum depth of a subtree of this object, 0 = no limit (default)
   * @see AppDbObjectTree
   */
  public int getTreeExpandMaxDepth() {
    return 0;
  }


  /**
   * Determines whether the navigatable object tree should show the
   * parents of this object.<br>
   * Objects may be childs of different parents in terms of "being referenced from"
   * or any other relation, for example: the warehouses this customer
   * visited.
   * Trees may provide a button to make these parents visible.
   * The default implementation returns false.
   * 
   * @return true if showing parents is enabled
   * @see AppDbObjectTree
   */
  public boolean allowsTreeParentObjects() {
    return false;
  }


  /**
   * Gets the parents of this object.
   * The default implementation returns an empty {@link ArrayList}.
   * 
   * @return the parent objects
   * @see AppDbObjectTree
   * @see #allowsTreeParentObjects
   */
  public List<Object> getTreeParentObjects()  {
    if (getDb().isRemote())  {
      try {
        List<Object> list = getRemoteDelegate().getTreeParentObjects(this);
        if (list != null) {
          for (Object obj: list)  {
            if (obj instanceof AppDbObject) {
              ((AppDbObject)obj).setContextDb(contextDb);
            }
          }
        }
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_getTreeParentObjects_failed"));
        return null;  // not reached
      }
    }
    return new ArrayList<Object>();
  }
  
  
  /**
   * Gets the parents with respect to the parent object this
   * object is displayed in the current tree.<br>
   * Override if the parents vary according to this object's parent.
   *
   * @param parentObject the parent object, null if no parent
   * @return the parents
   * @see AppDbObjectTree
   */
  public List<Object> getTreeParentObjects(Object parentObject) {
    if (getDb().isRemote())  {
      try {
        List<Object> list = getRemoteDelegate().getTreeParentObjects(this, parentObject);
        if (list != null) {
          for (Object obj: list)  {
            if (obj instanceof AppDbObject) {
              ((AppDbObject)obj).setContextDb(contextDb);
            }
          }
        }
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_getTreeParentObjects_failed"));
        return null;
      }
    }
    // default implementation
    return getTreeParentObjects();
  }
  
  
  
  /**
   * Gets the object to be displayed in trees in place of
   * this object. The default implementation returns "this".
   * Override if necessary.
   *
   * @return the object to be displayed in a tree instead of this.
   * @see AppDbObjectTree
   */
  public AppDbObject getTreeRoot()  {
    return this;
  }
  
  
  /**
   * Gets the text to be displayed in trees for this object.
   * The default returns {@link #toString}.
   * 
   * @return the tree text
   * @see AppDbObjectTree
   */
  public String getTreeText() {
    return toString();
  }
  
  
  /**
   * Gets the tree text with respect to the parent object this
   * object is displayed in a tree.<br>
   * The default implementation just invokes {@link #getTreeText}.
   * Override this method if the object's treetext varies according
   * to its parent.
   *
   * @param parent is the parent object, null if no parent
   * @return the tree text
   * @see AppDbObjectTree
   */
  public String getTreeText(Object parent) {
    return getTreeText();
  }
  

  /**
   * Gets the tooltip to be displayed for an object in a tree.
   * The default implementation returns {@link #toString}.
   * 
   * @return the tooltip text
   * @see AppDbObjectTree
   */
  public String getToolTipText()  {
    return toString();
  }
  
  /**
   * Gets the tooltip text with respect to the parent object this
   * object is displayed in a tree.<br>
   * The default implementation just invokes {@link #getToolTipText}.
   * Override this method if the objects tooltip varies according
   * to its parent.
   *
   * @param parent is the parent object, null if no parent
   * @return the tooltip text
   * @see AppDbObjectTree
   */
  public String getToolTipText(Object parent) {
    return getToolTipText();
  }

  
  /**
   * Determines whether tree explansion should stop at this object.<br>
   * Sometimes treechilds should not be expanded if in a recursive "expand all".
   * However they should expand if the user explicitly wants to.
   * If this method returns true, the tree will not further expand this node
   * automatically (only on demand).<br>
   * The default implementation returns false.
   * 
   * @return true if expansion should stop.
   * @see AppDbObjectTree
   */
  public boolean stopTreeExpansion() {
    return false;
  }
  
  

  /**
   * Extracts tree childs with a given class.<br>
   * The method provides a hint and an optimization method for {@link #extractTreePath}.
   * The default implementation simply invokes getTreeChildObjects().
   *
   * @param clazz is the class of the childs to get
   * @return a list of childs which may be of *ANY* class, not clazz only.
   */
  public List<AppDbObject> extractTreePathObjects(Class clazz)  {
    return filterAppDbObjects(getTreeChildObjects());
  }
  

  
  
  /**
   * Gets the {@link Transferable} for this object.<br>
   * Used for drag and drop.
   * If the object is new, null is returned because the object
   * has no ID yet.
   *
   * @return the transferable, null if none
   * @see AppDbObjectTransferable
   */
  public Transferable getTransferable() {
    return isNew() ? null : new AppDbObjectTransferable(this); 
  }
  
  
  /**
   * Drops a transferable on this object.<br>
   * The default implementation does nothing and returns false (drop failed).
   *
   * @param transferable the Transferable
   * @return true if drop succeeded, else false
   */
  public boolean dropTransferable(Transferable transferable) {
    return false;
  }
  
  
  /**
   * Searches for a "pattern" in this object.<br>
   * The default implementation looks for the pattern in the normtext.
   * 
   * @param pattern the pattern to search for
   * @return true if this object "contains" the pattern
   * @see AppDbObjectSearchDialog
   */
  public boolean containsPattern (String pattern) {
    if (getNormText() != null && pattern != null)  {
      return getNormText().indexOf(pattern) >= 0;
    }
    return false;
  }
  
  
  /**
   * Determines whether this object (or class) provides a field level history.<br>
   * This controls the history-menuitem in trees.
   * It does *not* tell whether the history is implemented by
   * db-triggers or by tentackle!
   * 
   * @return true if history is available
   * @see AppDbObjectTree
   */
  public boolean allowsHistory() {
    return false;
  }

  
  /**
   * Gets the history object for this object.<br>
   * The default implementation returns null.
   * 
   * @return the history object, null if no history
   */
  public History getHistory() {
    return null;
  }
  
  
  /**
   * Determines whether the history is implemented by Tentackle.<br>
   * History tables can be easily achieved by triggers directly within 
   * the database backend.
   * However, sometimes it's better to let the application generate the history
   * records. Furthermore, tentackle history logging *ONLY* logs ordinary
   * inserts, updates, saves and deletes! No editedBy, updateSerial, dummyUpdate, etc...<br>
   * The default implementation returns false.
   * 
   * @return true if the application generates the history for this object (or class) 
   */
  public boolean isLoggingHistory() {
    return false;
  }
  
  
  /**
   * Performs the history logging.
   *
   * @param historyType is one of History.INSERT|UPDATE|DELETE
   * @return true if history log could be created (and saved to the database)
   * @see #isLoggingHistory()
   */
  public boolean logHistory(char historyType) {
    History history = getHistory();
    if (history != null)  {
      return history.createHistoryLog(historyType);
    }
    return false;
  }
  
  
  
  /**
   * Gets the natural ordering to be added in WHERE-clauses following "ORDER BY ".
   * The wurblets will use it if --sort option set.
   * Example:
   * <pre>
   *  return FIELD_ID + "," + FIELD_PRINTED + " DESC";
   * </pre>
   * For a single field with sort ascending returning the fieldname is sufficient.
   * The default is null, i.e. no order-by-clause will be added.
   * 
   * @return the order by appendix string, null if no order by
   */
  public String orderBy() {
    return null;
  }
  
  
  
  /**
   * Creates the qbf plugin for the search dialog.<br>
   * The default implementation returns {@link DefaultQbfPlugin}.
   * 
   * @return the plugin
   */
  public QbfPlugin makeQbfPlugin()  {
    return new DefaultQbfPlugin(getClass(), contextDb);
  }
  

  
  /**
   * Presets the qbf parameter with default values from this object.<br>
   * The default implementation does nothing.
   * 
   * @param qbfPar the qbf parameter
   */
  public void presetQbfParameter(QbfParameter qbfPar) {
    // needs to be overridden!
  }
  
  
  /**
   * Determines the permission-type for this object.<br>
   * Can also be some application specific type.
   * The default implementation returns {@link Security#TYPE_DATA}
   * 
   * @return the permission type
   */
  public int permissionType()  {
    return Security.TYPE_DATA;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to security check.
   */
  @Override
  public boolean initModification(char modType) {
    return super.initModification(modType) &&
           getAppDbObjectClassVariables().isWriteAllowed(this);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to log history, remove security rules and clear
   * the editedby-token.
   */
  @Override
  public boolean finishModification(char modType) {

    boolean rv = super.finishModification(modType);

    if (rv) {

      if (isLoggingHistory()) {
        rv = logHistory(modType);
      }

      if (rv) {
        if (modType == DELETE) {
          if (isEntity() && getSecurityManager().isEnabled()) {
            // remove security rules as well
            new Security(getContextDb()).deleteByObject(getId(), getClassName());
            new Security(getContextDb()).deleteByGrantee(getId());
          }
        }
        else if (modType == UPDATE) {
          /**
           * If the object has getUpdateEditedByTimeout() > 0, the token
           * will be released in the object within the transaction.
           * Ensures also that the update will be refused if the lock
           * timed out and another user got the lock. In such a case,
           * the current tokens data is stored in the dbObject.
           *
           * Furthermore, if in the meantime (while lock timed out) another
           * user did a sucessful update, the update will fail cause of
           * the serial.
           *
           * However, if our lock timed out and another user held a lock
           * which has been released without any modification to the record,
           * the update will succeed, which is ok.
           */
          rv = getUpdateEditedByTimeout() <= 0 || updateEditedBy(null);
        }
      }
    }

    return rv;
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Ovewritten to get the editedBy token removed, if any.
   */
  @Override
  public boolean finishNotUpdated() {
    return getUpdateEditedByTimeout() <= 0 || updateEditedBy(null);
  }
  
  
  
  /**
   * Clears the editedBy token.<br>
   * The operation is only done in memory, not in persistent storage.
   */
  public void clearEditedBy() {
    editedBy = 0;
    editedExpiry = null;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to clear editedBy if remote db.
   */
  @Override
  public boolean updateObject(boolean withLinkedObjects) {
    boolean rv = super.updateObject(withLinkedObjects);
    if (rv && getDb().isRemote()) {
      clearEditedBy();
    }
    return rv;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to clear editedBy if remote db.
   */
  @Override
  public boolean save() {
    boolean rv = super.save();
    if (rv && getDb().isRemote()) {
      clearEditedBy();
    }
    return rv;
  }
  
  
  
  /**
   * Gets the expiration in milliseconds of the "being-edited-token" if
   * this object should request such a token when being edited.<br>
   * The default implementation returns 0.
   *
   * @return the timespan in ms, 0 = no token required.
   * @see AppDbObjectDialog
   */
  public long getUpdateEditedByTimeout()  {
    return 0; 
  }
  
  /**
   * Gets the statement id for {@link #updateEditedBy}.
   * @return the statement id
   */
  protected int getUpdateEditedByStatementId()  {
    return getAppDbObjectClassVariables().updateEditedByStatementId;
  }
  
  /**
   * Sets the statement id for {@link #updateEditedBy}.
   * @param id the statement id
   */
  protected void setUpdateEditedByStatementId (int id) {
    getAppDbObjectClassVariables().updateEditedByStatementId = id;
  }

  /**
   * Gets the statement id for {@link #updateEditedByOnly}.
   * @return the statement id
   */
  protected int getUpdateEditedByOnlyStatementId()  {
    return getAppDbObjectClassVariables().updateEditedByOnlyStatementId;
  }

  /**
   * Sets the statement id for {@link #updateEditedByOnly}.
   * @param id the statement id
   */
  protected void setUpdateEditedByOnlyStatementId (int id) {
    getAppDbObjectClassVariables().updateEditedByOnlyStatementId = id;
  }
  
  /**
   * Gets the statement id for select in {@link #updateEditedBy}.
   * @return the statement id
   */
  protected int getSelectEditedByStatementId()  {
    return getAppDbObjectClassVariables().selectEditedByStatementId;
  }

  /**
   * Sets the statement id for select in {@link #updateEditedBy}.
   * @param id the statement id
   */
  protected void setSelectEditedByStatementId (int id) {
    getAppDbObjectClassVariables().selectEditedByStatementId = id;
  }  
  
  /**
   * Gets the statement id for {@link #transferEditedBy}.
   * @return the statement id
   */
  protected int getTransferEditedByStatementId()  {
    return getAppDbObjectClassVariables().transferEditedByStatementId;
  }
  
  /**
   * Sets the statement id for {@link #transferEditedBy}.
   * @param id the statement id
   */
  protected void setTransferEditedByStatementId (int id) {
    getAppDbObjectClassVariables().transferEditedByStatementId = id;
  }  
  
  

  /**
   * Prepares the {@link #updateEditedBy} statement.
   * 
   * @return the statement id
   */
  protected int prepareUpdateEditedByStatement() {
    int stmtId = getUpdateEditedByStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      getDb().assertNotRemote();
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " +     // NOI18N
              FIELD_EDITEDBY + "=?, " +    // NOI18N
              FIELD_EDITEDSINCE + "=?, " +     // NOI18N
              FIELD_EDITEDEXPIRY + "=? WHERE " +    // NOI18N
              FIELD_ID + "=? AND (" +     // NOI18N
              FIELD_EDITEDBY + "=? OR " +         // the current user holds the token    // NOI18N
              FIELD_EDITEDBY + "=0 OR " +         // no one holding the token    // NOI18N
              FIELD_EDITEDEXPIRY + "<? OR " +     // token expired    // NOI18N
              FIELD_EDITEDEXPIRY + " IS NULL)");  // or not set at all (pathologic case)            // NOI18N                   
      setUpdateEditedByStatementId(stmtId);
    }
    return stmtId;
  }


  /**
   * Prepares the {@link #updateEditedByOnly} statement.
   *
   * @return the statement id
   */
  protected int prepareUpdateEditedByOnlyStatement() {
    int stmtId = getUpdateEditedByOnlyStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      getDb().assertNotRemote();
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " +     // NOI18N
              FIELD_EDITEDBY + "=?, " +    // NOI18N
              FIELD_EDITEDSINCE + "=?, " +     // NOI18N
              FIELD_EDITEDEXPIRY + "=? WHERE " +    // NOI18N
              FIELD_ID + "=?");  // NOI18N
      setUpdateEditedByOnlyStatementId(stmtId);
    }
    return stmtId;
  }


  
  /**
   * Prepares the select in {@link #updateEditedBy} statement.
   * 
   * @return the statement id
   */
  protected int prepareSelectEditedByStatement() {
    int stmtId = getSelectEditedByStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      getDb().assertNotRemote();
      stmtId = getDb().prepareStatement(
              "SELECT " + FIELD_EDITEDBY + "," + FIELD_EDITEDSINCE + "," + FIELD_EDITEDEXPIRY +    // NOI18N
              " FROM " + getTableName() + " WHERE " + FIELD_ID + "=?");    // NOI18N
      setSelectEditedByStatementId(stmtId);
    }
    return stmtId;
  }
    
  
  /**
   * Prepares the {@link #transferEditedBy} statement.
   * 
   * @return the statement id
   */
  protected int prepareTransferEditedByStatement() {
    int stmtId = getTransferEditedByStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      getDb().assertNotRemote();
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " +     // NOI18N
              FIELD_SERIAL + "=" + FIELD_SERIAL + "+1, " +    // NOI18N
              FIELD_EDITEDBY + "=? WHERE " +    // NOI18N
              FIELD_ID + "=?");    // NOI18N
      setTransferEditedByStatementId(stmtId);
    }
    return stmtId;
  }
  
  
  /**
   * Gets the id of the user currently editing this object.
   *
   * @return the id or 0 if not being edited currently.
   */
  public long getEditedBy() {
    return editedBy;
  }
  
  /** 
   * Sets the user editing this object.
   * Does *NOT* alter isModified().
   *
   * @param editedBy the id of the user, 0 to clear.
   *
   */
  public void setEditedBy(long editedBy) {
    this.editedBy = editedBy;
  }
  
  /**
   * Checks whether this object is token locked (editedBy != 0) and
   * the lock is not expired.
   * 
   * @return true if locked by another user 
   */
  public boolean isEditedLocked() {
    return editedBy != 0 && editedExpiry != null && editedExpiry.getTime() > System.currentTimeMillis();
  }
  
  /** 
   * Gets the time since when this object is being edited.
   *
   * @return the time, null if not being edited.
   */
  public Timestamp getEditedSince() {
    return editedSince;
  }
  
  /** 
   * Sets the time since when this object is being edited.
   * Does *NOT* alter isModified().
   *
   * @param editedSince the time, null to clear.
   */
  public void setEditedSince(Timestamp editedSince) {
    this.editedSince = editedSince;
  }
  
  
  /**
   * Gets the time since when this object is being edited.
   *
   * @return the time, null if not being edited.
   */
  public Timestamp getEditedExpiry() {
    return editedExpiry;
  }
  
  /**
   * Sets the time when the token should expire.
   * Does *NOT* alter isModified().
   *
   * @param editedExpiry the expiration time, null to clear.
   */
  public void setEditedExpiry(Timestamp editedExpiry) {
    this.editedExpiry = editedExpiry;
  }
  
  
  /**
   * Gets the object associated to the id of the editedBy-attribute.<br>
   * This is usually the id of an AppDbObject implementing the concept
   * of a user, group, role or whatever. The default implementation
   * invokes {@link AbstractApplication#getUser(long)}.
   *
   * @return the user object, null if no user or id was 0
   */
  public AppDbObject getEditedByObject()  {
    AbstractApplication application = AbstractApplication.getRunningApplication();
    return application == null ? null : application.getUser(editedBy);
  }
  
  /**
   * Sets the user/group/role-object editing this object.
   *
   * @param obj the object, null to clear.
   */
  public void setEditedByObject(AppDbObject obj)  {
    editedBy = obj == null ? 0 : obj.getId(); 
  }
  
  
  
  /**
   * Updates editing info in db-record (if feature enabled).<br>
   * Will *NOT* log modification and *NOT* update the serial-counter!
   * Must be called from within application where appropriate. See AppDbObjectDialog.
   * <p>
   * 2 cases:
   * <p>
   * If expiry == 0, the "token" is released.
   * True is returned if operation was successful and editedBy will hold 0 while editedSince
   * holds the current (release) time. False is returned if token could not
   * be released and nothing is changed. This usually is the case when another
   * user holds the token (and indicates some logic errors in the application, btw.)
   * EditedBy and editedSince are updated in the object to reflect the current values
   * in the database.
   * Notice: releasing an already released token is not considered to be an error.
   * This will simply update the release timestamp.
   * <p>
   * If expiry > 0, a new token for the current user is requested.
   * True is returned if the token could be exclusively acquired. EditedBy and
   * editedSince are updated accordingly.
   * If false: the object is in use by another user and editedSince and editedId
   * holds this user and his timestamp.
   * Notice: requesting an already requested token will simply renew the token.
   * <p>
   * The method does not check getUpdateEditedByTimeout()! This is due to the application.
   * Applications should use updateEditedBy(tokenExpiry). This is an internal implementation only.
   *
   * @param tokenExpiry holds the time the token will expire. Null to release token.
   * @param userId is the current user (unused if tokenExpiry is null)
   * @param curTime is the current system time
   *
   * @return true if operation done, false if couldn't set/unset token
   */
  public boolean updateEditedBy(Timestamp tokenExpiry, long userId, Timestamp curTime) {
    
    if (getDb().isRemote())  {
      try {
        AppDbObjectRemoteDelegate.BeingEditedToken token = getRemoteDelegate().updateEditedBy(getId(), tokenExpiry, userId, curTime);
        setEditedBy(token.editedBy);
        setEditedSince(token.editedSince);
        setEditedExpiry(token.editedExpiry);
        return token.success;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_updateBeingEditedToken_failed"));
      }
    }
    
    else  {

      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateEditedByStatement());
      
      long newUser = tokenExpiry != null ? userId : 0;
      
      st.setLong(1, newUser);
      st.setTimestamp(2, curTime);
      st.setTimestamp(3, tokenExpiry);
      st.setLong(4, getId());
      st.setLong(5, userId);
      st.setTimestamp(6, curTime);
      
      if (st.executeUpdate() == 1)  {
        // update was successful
        setEditedBy(newUser);
        setEditedSince(curTime);
        setEditedExpiry(tokenExpiry);
        return true;
      }
      else {
        // no success: another user is currently holding the token
        st = getDb().getPreparedStatement(prepareSelectEditedByStatement());
        st.setLong(1, getId());
        ResultSetWrapper rs = st.executeQuery();
        if (rs.next())  {
          setEditedBy(rs.getLong(FIELD_EDITEDBY));
          setEditedSince(rs.getTimestamp(FIELD_EDITEDSINCE));
          setEditedExpiry(rs.getTimestamp(FIELD_EDITEDEXPIRY));
        }
        rs.close();
      }
    }
    return false;
  }
  
  
  
  
  /**
   * Updates editing info in db-record (if feature enabled).<br>
   * Will *NOT* log modification and *NOT* update the serial-counter!
   * Must be called from within application where appropriate. See AppDbObjectDialog.
   * <p>
   * 2 cases:
   * <p>
   * If expiry == 0, the "token" is released.
   * True is returned if operation was successful and editedBy will hold 0 while editedSince
   * holds the current (release) time. False is returned if token could not
   * be released and nothing is changed. This usually is the case when another
   * user holds the token (and indicates some logic errors in the application, btw.)
   * EditedBy and editedSince are updated in the object to reflect the current values
   * in the database.
   * Notice: releasing an already released token is not considered to be an error.
   * This will simply update the release timestamp.
   * <p>
   * If expiry > 0, a new token for the current user is requested.
   * True is returned if the token could be exclusively acquired. EditedBy and
   * editedSince are updated accordingly.
   * If false: the object is in use by another user and editedSince and editedId
   * holds this user and his timestamp.
   * Notice: requesting an already requested token will simply renew the token.
   * <p>
   * The method does not check getUpdateEditedByTimeout()! This is due to the application.
   *
   * @param tokenExpiry holds the time the token will expire. Null to release token.
   *
   * @return true if operation done, false if couldn't set/unset token
   */
  public boolean updateEditedBy(Timestamp tokenExpiry) {
    
    Timestamp curTime = SqlHelper.now();
    long userId = 0;
    
    try {
      userId = getContextDb().getAppUserInfo().getUserId();
      if (userId == 0) {
        throw new ApplicationException(Locales.bundle.getString("userId_is_0"));
      }
    }
    catch (ApplicationException e)  {
      DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("couldn't_determine_user_id"));
    }
    
    return updateEditedBy(tokenExpiry, userId, curTime);
  }
  


  /**
   * Update the editedBy-attributes to persistent storage.<br>
   * No check is done whether locked or not and there is no serial
   * update and no modlog. Used by daemons to cleanup.
   *
   * @return true if updated
   */
  public boolean updateEditedByOnly() {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().updateEditedByOnly(getId(), editedBy, editedSince, editedExpiry);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_updateBeingEditedToken_failed"));
      }
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateEditedByOnlyStatement());
      st.setLong(1, editedBy);
      st.setTimestamp(2, editedSince);
      st.setTimestamp(3, editedExpiry);
      st.setLong(4, getId());
      return st.executeUpdate() == 1;
    }
    return false;
  }



  /**
   * Transfers the editing info in db-record (if feature enabled)
   * to another user.<br>
   * Necessary to hand-over a token without releasing it.
   * Eliminates the time-gap when object is token-free.
   * Furthermore allows taking over the token WITHOUT possessing it!
   * This method should be used with great care as it does not check
   * who owns the token and if there is a token at all.
   * The serial will be increased, so the old user gets an error
   * if trying to update.
   * Furthermore, the current object does *NOT* get the serial increased
   * thus preventing any update without reloading the object again!
   * As opposed to updateEditedBy the modtable is updated (countModification)
   * because the serial is increased and thus all caches must be invalidated.
   *
   * @param userId is the new user Id. (special 0 = free token, even if we are not the owner)
   *
   * @return true if operation done, false if couldn't transfer token
   */
  public boolean transferEditedBy(long userId) {

    if (getDb().isRemote())  {
      try {
        AppDbObjectRemoteDelegate.BeingEditedToken token = getRemoteDelegate().transferEditedBy(userId);
        setEditedBy(token.editedBy);
        return token.success;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, Locales.bundle.getString("remote_updateBeingEditedToken_failed"));
      }
    }
    
    else  {
      boolean oldCommit = getDb().begin(TX_TRANSFER_EDITEDBY);  // within a TX cause of countModification to invalidateCache
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareTransferEditedByStatement());
      st.setLong(1, userId);
      st.setLong(2, getId());
      if (st.executeUpdate() == 1)  {
        if (isCountingModification(UPDATE))  {
          setTableSerial(countModification(true));    // this will rollback and terminate on error
        }
        // update was successful
        getDb().commit(oldCommit);
        return true;
      }
      getDb().rollback(oldCommit);
    }
    
    return false;
  }



  /**
   * Gets the table entry for viewing lists of this object
   * in a {@link org.tentackle.ui.FormTable}.<br>
   * The default implementation returns an {@link AppDbObjectDefaultTableEntry}.
   * Should be overridden in subclasses.
   *
   * @return the formtable entry
   * @see #getFormTableName()
   */
  public FormTableEntry getFormTableEntry() {
    return new AppDbObjectDefaultTableEntry(this);
  }
  
  
  /**
   * Gets the tablename used in to initialize the table's gui (columns, sizes, etc...).
   * The default implementation returns {@link #formTableName}.
   * Should be overridden.
   * 
   * @return the table name
   * @see #getFormTableEntry()
   */
  public String getFormTableName()  {
    return formTableName;
  }
  
  
  /**
   * Gets the optional transient data object.<br>
   * The default implementation does nothing.
   *
   * @return the transient data, null if none.
   */
  public Object getTransientData()  {
    return null;
  }
  
  /**
   * Sets the optional transient data object.<br>
   * Sometimes, e.g. when objects need to be reloaded from storage, all non-persistent
   * data attached to the object would be lost. In such cases the framework invokes
   * getTransientData() and setTransientData() to keep the non-persistent attributes.<br>
   * The default implementation does nothing.
   * 
   * @param data the transient data
   */
  public void setTransientData(Object data) {
  }
  
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to covariance.
   */
  @Override
  public AppDbObjectRemoteDelegate getRemoteDelegate()  {
    return (AppDbObjectRemoteDelegate)super.getRemoteDelegate();
  }
  
  
  
  /**
   * Gets the statememt id for {@link #selectByNormText}.
   * @return the statement id
   */
  protected int getSelectByNormTextStatementId() {
    return getAppDbObjectClassVariables().normTextStatementId;
  }
  
  /**
   * Sets the statememt id for {@link #selectByNormText}.
   * @param id the statement id
   */
  protected void setSelectByNormTextStatementId (int id) {
    getAppDbObjectClassVariables().normTextStatementId = id;
  }
  
  
  /**
   * Gets the statememt id for {@link #selectAllInContext}.
   * @return the statement id
   */
  protected int getSelectAllInContextStatementId() {
    return getAppDbObjectClassVariables().allInContextStatementId;
  }
  
  /**
   * Sets the statememt id for {@link #selectAllInContext}.
   * @param id the statement id
   */
  protected void setSelectAllInContextStatementId (int id) {
    getAppDbObjectClassVariables().allInContextStatementId = id;
  }
  
  
  /**
   * Gets the statememt id for {@link #selectByNormTextCursor}.
   * @return the statement id
   */
  protected int getSelectByNormTextCursorStatementId() {
    return getAppDbObjectClassVariables().normTextCursorStatementId;
  }
  
  /**
   * Sets the statememt id for {@link #selectByNormTextCursor}.
   * @param id the statement id
   */
  protected void setSelectByNormTextCursorStatementId (int id) {
    getAppDbObjectClassVariables().normTextCursorStatementId = id;
  }
  
  
  /**
   * Gets the statememt id for {@link #selectAllInContextCursor}.
   * @return the statement id
   */
  protected int getSelectAllInContextCursorStatementId() {
    return getAppDbObjectClassVariables().allInContextCursorStatementId;
  }
  
  /**
   * Sets the statememt id for {@link #selectAllInContextCursor}.
   * @param id the statement id
   */
  protected void setSelectAllInContextCursorStatementId (int id) {
    getAppDbObjectClassVariables().allInContextCursorStatementId = id;
  }

  
  
  /**
   * Creates a cloned copy of a collection in such a way that
   * all operations on the copied collection or its objects will not modify
   * the orginal collection or any of its objects.
   * 
   * @param col the original collection
   * @return the copied collection
   * @see #copy
   */
  @SuppressWarnings("unchecked")
  public static Collection<? extends AppDbObject> copyCollection(Collection<? extends AppDbObject> col) {
    Collection<AppDbObject> ncol = null;
    if (col != null) {
      if (col instanceof ArrayList) {
        // for the mostly used ArrayList and TrackedArrayList, we speed up a little
        ncol = (Collection<AppDbObject>) ((ArrayList)col).clone();
      }
      else  {
        // generic clone by reflection
        try {
          ncol = (Collection<AppDbObject>) ObjectHelper.clone(col);
        } 
        catch (CloneNotSupportedException ex) {
          throw new DbRuntimeException(ex.getMessage());
        }
      }
      ncol.clear();   // clear + add is usually faster than set/get (which is not available in all collections anyway)
      for (AppDbObject obj: col) {
        ncol.add(obj == null ? null : obj.copy());
      }
      if (ncol instanceof TrackedArrayList) {
        // copied lists are not modified by definition of copy()
        ((TrackedArrayList)ncol).setModified(false);
      }
    }
    return ncol;
  }
  
  
  
  /**
   * Sets the classpath for loading classes by basename.<br>
   * Database object classes may be loaded by their basename (see {@link #getClassBaseName}).
   * For this to work a classpath must be defined.
   * This is simply an array of package names.
   * The default classpath contains only the empty string.
   * 
   * @param path the classpath
   */
  public static void setClassPath(String[] path)  {
    classPath = path;
  }
  
  /**
   * Gets the current classpath for loading classes by basename.
   * 
   * @return the classpath
   */
  public static String[] getClassPath()  {
    return classPath;
  }
  
  
  /**
   * Loads an AppDbObject-class by its classname.<br>
   * If the classname is fully qualifiedthe class will simply be loaded
   * via {@link Class#forName}.
   * If the classname does not contain a dot '.' the class is loaded by prepending the
   * classpath set by {@link #setClassPath}, one by one.<br>
   * This method allows storing shorter classnames in db-tables without
   * the need to install a new classloader for the whole application.
   * 
   * @param className
   * @return the class
   * @throws ClassNotFoundException if the class is not an extension of AppDbObject
   */
  @SuppressWarnings("unchecked")
  static public Class<? extends AppDbObject> loadClass (String className) 
         throws ClassNotFoundException {
    // search in classpath only if classPath set and className is not fully qualified
    int max = (classPath == null || className.indexOf('.') >= 0) ? 1 : classPath.length + 1;
    for (int i=0; i < max; i++) {
      String name = (i == max-1) ? className : (classPath[i] + "." + className);    // NOI18N
      try {
        // load the class
        Class<?> clazz = Class.forName(name);
        if (AppDbObject.class.isAssignableFrom(clazz))  {
          return (Class<AppDbObject>)clazz;    // unchecked
        }
      }
      catch (ClassNotFoundException ex) {
        if (i == max - 1) {
          throw ex; // last chance gone
        }
        // try next
      }
    }
    // notreached
    throw new ClassNotFoundException(className + " is not an extension of AppDbObject");
  }
  
  
  /**
   * Instantiates a new object for a given class and context.
   * 
   * @param <T> the class
   * @param contextDb the database context
   * @param clazz the class to create a new object for
   * @return the object
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  static public <T extends AppDbObject> T newByClass (ContextDb contextDb, Class<T> clazz) 
         throws InstantiationException, IllegalAccessException {
    // load the class
    T obj = newByClass(clazz);
    obj.setContextDb(contextDb);
    return obj;
  }
  
  
  /**
   * Instantiates a new object for a given classname and context.
   * The classname may be full qualified or a basename.
   *
   * @param contextDb the database context 
   * @param classBaseName is the class basename
   * @return the new object
   * @throws ClassNotFoundException 
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @see #loadClass
   */
  static public AppDbObject newByClassBaseName (ContextDb contextDb, String classBaseName)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return newByClass(contextDb, loadClass(classBaseName));
  }
  
  
  
  /**
   * Gets the human readable name for a single object of given class.
   * 
   * @param clazz the class
   * @return the name
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @see DbObject#getSingleName
   */
  static public String getSingleName(Class<? extends AppDbObject> clazz) 
         throws InstantiationException, IllegalAccessException {
    if (Modifier.isAbstract(clazz.getModifiers()))  {
      // pathological case
      return StringHelper.getClassBaseName(clazz);
    }
    return newByClass(clazz).getSingleName();
  }
  
  
  /**
   * Gets the human readable name for multiple objects of given class.
   * 
   * @param clazz the class
   * @return the name
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @see DbObject#getMultiName
   */
  static public String getMultiName(Class<? extends AppDbObject> clazz) 
         throws InstantiationException, IllegalAccessException {
    if (Modifier.isAbstract(clazz.getModifiers()))  {
      // pathological case
      return StringHelper.getClassBaseName(clazz);
    }
    return newByClass(clazz).getMultiName();
  }

  
  
  /**
   * Selects an object for a given db connection and class by its unique ID
   * in a newly created context
   * (with linked objects).
   * 
   * @param <T> the class
   * @param db the db connection
   * @param clazz the class
   * @param id the object id
   * @return the object, null if no such object
   * @throws ClassNotFoundException 
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @see #selectInValidContext(long)
   */
  @SuppressWarnings("unchecked")
  public static <T extends AppDbObject> T selectInValidContext(Db db, Class<T> clazz, long id) 
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return (T)newByClass(db, clazz).selectInValidContext(id);
  }  
  
  
  /**
   * Selects an object for a given db connection and class by its unique ID
   * in a newly created context
   * (with linked objects).
   * 
   * @param db the db connection
   * @param classBaseName the class basename
   * @param id the object id
   * @return the object, null if no such object
   * @throws ClassNotFoundException 
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @see #selectInValidContext(long)
   */
  @SuppressWarnings("unchecked")
  public static AppDbObject selectInValidContext(Db db, String classBaseName, long id) 
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return selectInValidContext(db, loadClass(classBaseName), id);
  }  
  
  
  /**
   * Selects all objects for a given context and class.
   * 
   * @param <T> the class
   * @param contextDb the database context
   * @param clazz the class
   * @return the list of objects
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @see #selectAllInContext
   */
  @SuppressWarnings("unchecked")
  public static <T extends AppDbObject> List<T> selectAllInContext(ContextDb contextDb, Class<T> clazz)
         throws InstantiationException, IllegalAccessException {
    return (List<T>)newByClass(contextDb, clazz).selectAllInContext();
  }  
  
  
  /**
   * Selects an object for a given context, class and object ID.<br>
   * 
   * If the class is abstract reflection is used to invoke a static method
   * with the signature:
   * <pre>
   * static T select(ContextDb, Class, long)
   * 
   * while T extends AppDbObject
   * </pre>
   * If the class does not provide such a method an InstantationException is thrown.
   * 
   * @param <T> the class
   * @param contextDb the database context
   * @param clazz the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  static public <T extends AppDbObject> T select (ContextDb contextDb, Class<T> clazz, long id)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    if (Modifier.isAbstract(clazz.getModifiers()))  {
      // find the static method in abstract class to load the object.
      // if there is none, fall through and generate an InstantationException
      try {
        Method method = clazz.getDeclaredMethod("select", new Class[] { ContextDb.class, Long.TYPE });    // NOI18N
        return (T)method.invoke(null, new Object[] { contextDb, new Long(id) });
      }
      catch (Exception ex)  {}  // fall through...
    }
    return (T)newByClass(contextDb, clazz).select(id);
  }
  
  
  /**
   * Selects an object via cache for a given context, class and object ID.<br>
   * If there is no cache, the object is selected uncached.
   * If the class is abstract reflection is used to invoke a static method
   * with the signature:
   * <pre>
   * static T selectCached(ContextDb, Class, long)
   * 
   * while T extends AppDbObject
   * </pre>
   * If the class does not provide such a method an InstantationException is thrown.
   * 
   * @param <T> the class
   * @param contextDb the database context
   * @param clazz the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  static public <T extends AppDbObject> T selectCached (ContextDb contextDb, Class<T> clazz, long id)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    if (Modifier.isAbstract(clazz.getModifiers()))  {
      // find the static method in abstract class to load the object.
      // if there is none, fall through and generate an InstantationException
      try {
        Method method = clazz.getDeclaredMethod("selectCached", new Class[] { ContextDb.class, Long.TYPE });    // NOI18N
        return (T)method.invoke(null, new Object[] { contextDb, new Long(id) });
      }
      catch (Exception ex)  {}  // fall through...
    }
    return (T)newByClass(contextDb, clazz).selectCached(id);
  }
  
  
  /**
   * Selects and write-locks an object for a given context, class and object ID.<br>
   * 
   * If the class is abstract reflection is used to invoke a static method
   * with the signature:
   * <pre>
   * static T selectLocked(ContextDb, Class, long)
   * 
   * while T extends AppDbObject
   * </pre>
   * If the class does not provide such a method an InstantationException is thrown.
   * 
   * @param <T> the class
   * @param contextDb the database context
   * @param clazz the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  static public <T extends AppDbObject> T selectLocked (ContextDb contextDb, Class<T> clazz, long id)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    if (Modifier.isAbstract(clazz.getModifiers()))  {
      // find the static method in abstract class to load the object.
      // if there is none, fall through and generate an InstantationException
      try {
        Method method = clazz.getDeclaredMethod("selectLocked", new Class[] { ContextDb.class, Long.TYPE });    // NOI18N
        return (T)method.invoke(null, new Object[] { contextDb, new Long(id) });
      }
      catch (Exception ex)  {}  // fall through...
    }
    return (T)newByClass(contextDb, clazz).selectLocked(id);
  }
  
  
  /**
   * Selects an object for a given context, class and object ID.<br>
   * 
   * @param contextDb the database context
   * @param classBaseName the basename or full name of the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @see #newByClassBaseName
   */
  static public AppDbObject select (ContextDb contextDb, String classBaseName, long id) 
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return newByClassBaseName(contextDb, classBaseName).select(id);
  }
  
  
  /**
   * Selects an object via cache for a given context, class and object ID.<br>
   * If there is no cache the object will be loaded uncached.
   * 
   * @param contextDb the database context
   * @param classBaseName the basename or full name of the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @see #newByClassBaseName
   */
  static public AppDbObject selectCached (ContextDb contextDb, String classBaseName, long id) 
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return newByClassBaseName(contextDb, classBaseName).selectCached(id);
  }
  
  
  /**
   * Selects and write-locks an object for a given context, class and object ID.<br>
   * 
   * @param contextDb the database context
   * @param classBaseName the basename or full name of the class
   * @param id the object ID
   * @return the object, null if no such object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @see #newByClassBaseName
   */
  static public AppDbObject selectLocked (ContextDb contextDb, String classBaseName, long id)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return newByClassBaseName(contextDb, classBaseName).selectLocked(id);
  }
  
  
  
  /**
   * Loads all composites of a collection.<br>
   *
   * @param objects the collection of AppDbObjects
   * @return the number of objects processed, &lt; 0 if failed (number*(-1)-1)
   * @throws DbRuntimeException if some object did not implement loadComposites()
   */
  public static int loadComposites(Collection<? extends AppDbObject> objects) {
    int count = 0;
    if (objects != null) {
      for (AppDbObject obj: objects) {
        if (obj.loadComposites() == false) {
          count = -count - 1;
          break;
        }
        count++;
      }
    }
    return count;
  }
  
  
  
  /**
   * Filters a collection of Objects, returning only
   * those that are AppDbObjects.
   *
   * @param col the collection of objects
   * @return a list of AppDbObjects, the rest is ignored. Never null.
   */
  public static List<AppDbObject> filterAppDbObjects(Collection col) {
    List<AppDbObject> list = new ArrayList<AppDbObject>();
    if (col != null)  {
      for (Object obj: col)  {
        if (obj instanceof AppDbObject) {
          list.add((AppDbObject)obj);
        }
      }      
    }
    return list;
  }
  
  
  
  /**
   * Extracts from a list of objects the tree-hierarchy at a
   * given depth.<br>
   * The last class in the array clazz[] is the desired one.
   * The objects will usually displayed as a list in a table.
   * 
   * @param objects the root-objects to walk along the clazz[]-path. Non-AppDbObjects will be skipped.
   * @param clazz the classes to pass along
   * @return the list of objects (of the last Class in clazz[])
   * @see AppDbObjectTree
   */
  public static List<AppDbObject> extractTreePath(Collection objects, Class[] clazz) {
    Set<AppDbObject> parsedObjects = new TreeSet<AppDbObject>();   // parsed objects (unique)
    Set<AppDbObject> treeObjects   = new TreeSet<AppDbObject>();   // found objects (unique)
    int level = 0;
    int depth = clazz.length;
    for (Object obj: objects)  {
      if (obj instanceof AppDbObject) {
        extractIntoTreeColumnList(treeObjects, parsedObjects, clazz, (AppDbObject)obj, level);
      }
    }
    return new ArrayList<AppDbObject>(treeObjects);
  }
  
  
  private static void extractIntoTreeColumnList(Set<AppDbObject> treeObjects, Set<AppDbObject> parsedObjects, 
                                                Class clazz[], AppDbObject object, int level)  {
    if (object != null && object.getClass() == clazz[level]) {
      // if path matches
      if (level == clazz.length - 1) {
        // we are at end of path: add object uniquely
        treeObjects.add(object);
      }
      else if (parsedObjects.add(object)) {
        // if not yet parsed
        level++;
        // process childlist
        List<AppDbObject> childs = object.extractTreePathObjects(clazz[level]);
        if (childs != null) {
          for (AppDbObject child: childs) {
            extractIntoTreeColumnList(treeObjects, parsedObjects, clazz, child, level);
          }
        }
      }
    }
  }
  
  
  
  /**
   * Invokes makeQbfPlugin in abstract classes.<br>
   * Searches for a static method "makeQbfPlugin(ContextDb)" and invokes it.
   *  
   * @param clazz is the abstract class
   * @param db is the db-context
   * @return the plugin
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws InvocationTargetException 
   */
  public static QbfPlugin makeQbfPlugin(Class<? extends AppDbObject> clazz, ContextDb db) 
         throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method method = clazz.getDeclaredMethod("makeQbfPlugin", new Class[] { ContextDb.class });    // NOI18N
    return (QbfPlugin)method.invoke(null, new Object[] { db });
  }
  
}
