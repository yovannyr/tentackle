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

// $Id: ContextDb.java 477 2009-08-09 18:54:26Z harald $
// Created on September 2, 2002, 3:55 PM


package org.tentackle.appworx;

import org.tentackle.db.Db;
import org.tentackle.util.ApplicationException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.tentackle.db.DbRuntimeException;
import org.tentackle.util.Compare;
import org.tentackle.util.StringHelper;
import org.tentackle.util.TrackedArrayList;


/**
 * The application's database context.
 * <p>
 * Basically, this is a decorator for a logical {@link Db}-connection.
 * All {@link AppDbObject}s refer to a ContextDb instead of a Db.
 * The ContextDb carries the application's higher level database context
 * which can be used to implement the so-called multitenancy.
 * If your application is not multitenant, you don't have to pay
 * further attention to it.
 * <p>
 * For multitenant applications, however, the ContextDb can be extended
 * to hold the ID(s) of the object(s) describing the context. Because
 * an AppDbObject carries a reference to that context, it can serve as
 * a root for a logical data space. For example, a {@link AppDbObject#selectAllInContext()}
 * of accounts will deliver only the accounts belonging to that particular tenant.<br>
 * All classes depending on those extended contexts must also implement
 * an appropriate extension of {@link ContextDependable}. By further subclassing,
 * a multitenancy hierarchy can be achieved. For example, in a financial
 * accounting system, the hierarchy could be: TenantDb -> FiscalYearDb -> BookDb
 * with corresponding TenantDependable -> FiscalYearDependable -> BookDependable.
 * It is recommended to subclass AppDbObject accordingly, i.e. TenantDbObject -> 
 * FiscalYearDbObject -> BookDbObject and/or to override the methods in {@link ContextDependable}
 * by applying multiple inheritance emulation via the Wurbelizer.
 * <p>
 * Note: the wurblets support multitenancy by means of the options
 * CONTEXT, CONTEXTID and UNIQUE.
 * 
 * @author harald
 */
public class ContextDb implements Comparable<ContextDb>, Serializable, Cloneable {
  
  private static final long serialVersionUID = 1326690682129400506L;
  
  private transient Db db;      // the decorated db connection.
  
  
  /**
   * Creates a default context.
   * 
   * @param db the db connection
   */
  public ContextDb(Db db) {
    this.db = db;     // don't use setDb() as this will also assertPermissions
  }
  
  
  /**
   * Gets the db connection of that context
   * 
   * @return the db
   */
  public Db getDb() {
    return db;
  }
  
  
  /**
   * Sets the db connection.<br>
   * Used for objects traveling between the client and
   * the server. It is also checked that the user is
   * allowed to use this context.
   * 
   * @param db the db connection
   * @see #assertPermissions() 
   */
  public void setDb(Db db) {
    this.db = db;
    assertPermissions();
  }
  
  
  /**
   * AppDbObjects should be connected to a Db via {@link AppUserInfo},
   * not {@link org.tentackle.db.UserInfo}.
   * The AppUserInfo extends the UserInfo by a SecurityManager.
   * 
   * @return the AppUserInfo associated to the Db.
   * @throws ApplicationException if Db does not provide AppUserInfo 
   */
  public AppUserInfo getAppUserInfo() throws ApplicationException {
    try {
      return (AppUserInfo)db.getUserInfo(); 
    }
    catch (Exception ex)  {
      throw new ApplicationException("db not connected via AppUserInfo", ex);
    }
  }
  
  
  /**
   * Asserts that this context is allowed to be used by the current user.<br>
   * 
   * If it is not allowed, a {@link DbRuntimeException} is thrown.
   * The default implementation does nothing.
   * <p>
   * Override the method in middle tier servers, for example in a hypothetical
   * {@code TenantContextDb}:
   * <pre>
   *  public void assertPermissions()  {
   *    if (DbGlobal.serverDb != null &&    // if in RMI server
   *        !getTenant().isUsageAllowed()) {
   *       throw new DbRuntimeException("malicious client tried to use context " + this);
   *    }
   *  }
   * </pre>
   * 
   * @throws DbRuntimeException if assertion failed
   */
  public void assertPermissions() {
    
  }
  
  
  /**
   * Gets the object that spans this context.
   * The default implementation returns null.
   * Must be overridden in subclasses.
   * 
   * @return the root object, null if in default context
   */
  public AppDbObject getContextObject()  {
    return null;
  }
  
  
  /**
   * Gets the ID of the context object.
   * The default implementation returns 0.
   * Must be overridden in subclasses.
   * 
   * @return the object ID, 0 if in default context 
   */
  public long getContextId()  {
    return 0;
  }
  
  
  /**
   * Determines whether this context belongs to an inheritance hierarchy that
   * is created from a given context object.
   * <p>
   * The method is invoked from {@link Security#evaluate} to check whether a
   * security rule applies to a given context. Note that contextClass is the
   * basename of the context object's class, for example "Tenant" or "FiscalYear",
   * and not the classname of the ContextDb.
   * <p>
   * The default implementation returns {@code contextId == 0 || contextId == getContextId()}.
   * This is sufficient for zero or one level of context inheritance. For more than one
   * level this method must be overwridden in each level. Example for a hypothetical "TenantContextDb":
   * <pre>
   *  public boolean isWithinContext(long contextId, String contextClass) {
   *    if (contextClass != null && contextClass.equals("Tenant") && contextId == getContextId()) {
   *      return true;
   *    }
   *    return super.isWithinContext(contextId, contextClass);
   *  }
   * </pre>
   * If the object IDs of the context objects are unique among all context entities the
   * contextClass can be ignored and the method reduces to:
   * <pre>
   *  public boolean isWithinContext(long contextId, String contextClass) {
   *    return contextId == getContextId() ||
   *           super.isWithinContext(contextId, contextClass);
   *  }
   * </pre> 
   * 
   * @param contextId the object ID of a context object, 0 = default context
   * @param contextClass the class basename of the context object, null = default context
   * @return true if within that context, false if context does not apply
   */
  public boolean isWithinContext(long contextId, String contextClass) {
    return contextId == 0 || contextId == getContextId();    
  }
  

  /**
   * Gets the string representation of this context.
   * The default implementation returns the string of the context object,
   * or the empty string (not the null-string!), if no such object,
   * which is the case for the plain ContextDb.
   * 
   * @return the string
   */
  @Override
  public String toString()  {
    // contextDb does not include Db.toString() because contextDb.toString() is
    // heavily used in GUIs to show the users context.
    return getContextId() == 0 ? 
      StringHelper.emptyString : getContextObject().toString();
  }


  /**
   * Returns the generic string representation of this context.
   * <p>
   * Use this for logging as it will not invoke methods on other objects.
   *
   * @return the String as <tt>Classname[contextId]<tt>
   */
  public String toGenericString() {
    return getClass().getName() + "[" + getContextId() + "]";
  }
  
  
  /**
   * Get the long description of this context.
   * Used for logging, for example.
   * 
   * @return the long info
   */
  public String getInfo() {
    return getDb() + ":{" + this + "}'";
  }
  
  
  /**
   * Contexts must be comparable. For example, the {@link AppDbObjectCache}
   * may keep several instances of the same object in different contexts.
   * The default implementation just compares the classes, then the ContextIds
   * and then the db connections.<br>
   *
   * @param otherContextDb the context to compare this context to
   * @return  a negative integer, zero, or a positive integer as this object
   *          is less than, equal to, or greater than the specified object.
   */
  public int compareTo(ContextDb otherContextDb) {
    int rv = getClass().hashCode() - otherContextDb.getClass().hashCode();
    if (rv == 0) {
      rv = Compare.compareLong(getContextId(), otherContextDb.getContextId());
      if (rv == 0) {
        rv = db.compareTo(otherContextDb.db);
      }
    }
    return rv;
  }

  
  /**
   * Overridden to allow for checks whether contexts are equal.
   * The default implementation just checks the classes, the contextIds and db connections for equality.
   * Checking against the null object is allowed and will return false.
   * 
   * @param obj the object to check for equality
   */
  @Override
  public boolean equals(Object obj) {
    // db must be exactly the same instance!!
    try {
      return db.equals(((ContextDb)obj).db) &&
             getContextId() == ((ContextDb)obj).getContextId() &&
             getClass() == obj.getClass();
    }
    catch (Exception e) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 7 + (int)getContextId() + (getClass().hashCode() & 0xffff);
    hash = 53 * hash + (db != null ? db.hashCode() : 0);
    return hash;
  }

  
  
  /**
   * ContextDbs may be cloned (e.g. in QbfParameter to allow temporary
   * change in the db-connection).
   * The default implementation just invokes clone().
   * 
   * @return the cloned context
   */
  @Override
  public ContextDb clone() {
    try {
      return (ContextDb)super.clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new InternalError();    // should never happen
    }    
  }
  
  
  
  
  
  /**
   * Sets the context in a {@link ContextDependable}.<br>
   * The method invokes <tt>obj.setContextDb()</tt> only if the contextDb really differs.
   * This prevents infinite loops in object circular references.
   * 
   * @param contextDb the database context
   * @param obj the data object, null if ignore
   */
  public static void applyToContextDependable(ContextDb contextDb, ContextDependable obj) {
    if (obj != null && obj.getContextDb() != contextDb)  {
      obj.setContextDb(contextDb);
    } 
  }
  
  
  /**
   * Sets the context in a list of {@link ContextDependable}s.
   * 
   * @param contextDb the database context
   * @param list the collection of data objects
   */
  public static void applyToCollection(ContextDb contextDb, Collection<? extends ContextDependable> list)  {
    if (list != null) {
      for (ContextDependable obj: list) {
        applyToContextDependable(contextDb, obj);
      }
      if (list instanceof TrackedArrayList) {
        @SuppressWarnings("unchecked")
        List<ContextDependable> removedObjects = ((TrackedArrayList)list).getRemovedObjects();
        if (removedObjects != null) {
          for (ContextDependable obj: removedObjects) {
            applyToContextDependable(contextDb, obj);
          }
        }
      }
    }
  }

}
