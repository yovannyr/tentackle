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

// $Id: DbObject.java 472 2009-08-07 08:30:36Z svn $

package org.tentackle.db;

import org.tentackle.db.rmi.DbObjectRemoteDelegate;
import org.tentackle.db.rmi.DbObjectResult;
import org.tentackle.util.Compare;
import org.tentackle.util.LongArray;
import org.tentackle.util.TrackedArrayList;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import org.tentackle.plaf.PlafGlobal;

        

/**
 * Database object.<p>
 * All database objects must extend DbObject, which provides the generic
 * functionality of persistent objects. Every DbObject is associated
 * to a logical {@link Db} connection, which can be either local or remote.
 * The application-specific configuration is achieved by implementing and/or
 * overriding methods (pure OO-approach). These methods are generated
 * by wurblets (see http://www.wurbelizer.org) which are already provided by Tentackle.
 * <p>
 * DbObjects have the following predefined attributes:
 * <ul>
 *  <li>
 *  <strong>id</strong>: a unique number (long) that identifies the object. The number is
 *  guaranteed to be unique for all objects of the same class.
 *  </li>
 *  <li>
 *  <strong>serial</strong>: the object's serial number. When an object is first
 *  persisted it gets the serial 1. Every update increments the serial. The serial
 *  is primarily used for optimistic locking and cache synchronization.
 *  </li>
 *  <li>
 *  <strong>tableSerial</strong>: a serial number for all objects of this class
 *  (per table or relation in database lingo). This attribute is optional and
 *  allows to determine which objects have been modified/inserted/deleted since
 *  the last change to any object within the same class.
 *  </li>
 * </ul>
 * 
 * By default, all dbobject classes use {@link ObjectId} as their common {@link IdSource}.
 * However, that can be changed on a per-class basis by providing a property file.
 * 
 */
public abstract class DbObject implements Serializable, Comparable<DbObject>, Cloneable {

  // data object attributes are always private
  private long            id;                   // unique Object ID
  private long            serial;               // serial-nummer (version to detect simultaneous updates)
  
  private long            tableSerial;          // last table serial from countModification (only if isTableSerialValid() == true)
  
  // not persistable (but not transient due to RMI)
  private boolean         modified;             // true if object is modified and not written to db yet
  
  // transient (not transferred to/from the application server)
  private transient       Db db;                // Database (transient because it shouldn't be serialized)
  private transient long  lastLazyMethodInvocationMs; // time when the last is....Lazy() method was invoked in [ms]
  
  private transient boolean referencedLazy;     // last results
  private transient boolean referencedLazyValid;// true = last result is valid
  private transient boolean removableLazy;
  private transient boolean removableLazyValid;
  private transient boolean editableLazy;
  private transient boolean editableLazyValid;
  
  
  /**
   * Interval in [ms] to avoid invocation bursts of some methods due
   * to swing's event model. See {@link #isEditableLazy}, for example.
   */
  public static long lazyMethodInterval = 5000;
  
  
  // avoids excessive creation of comparators
  /** ID comparator **/
  public static IdComparator<DbObject>     idComparator     = new IdComparator<DbObject>();
  /** name comparator **/
  public static NameComparator<DbObject>   nameComparator   = new NameComparator<DbObject>();
  /** name + ID comparator **/
  public static NameIdComparator<DbObject> nameIdComparator = new NameIdComparator<DbObject>();
  
  
  // column names common for all objects.
  // names work with all databases mentioned in Db.
  // (not final in case needs to be changed globally)
  /** name of ID column **/
  public static String FIELD_ID          = "id";
  /** name of serial column **/
  public static String FIELD_SERIAL      = "serial";
  /** name of tableserial column **/
  public static String FIELD_TABLESERIAL = "tableserial";

  
  /** modification type: begin transaction **/
  public static final char BEGIN      = 'B';    // 
  /** modification type: commit transaction **/
  public static final char COMMIT     = 'C';    // commit transaction
  /** modification type: object inserted **/
  public static final char INSERT     = 'I';    // object inserted
  /** modification type: object updated **/
  public static final char UPDATE     = 'U';    // 
  /** modification type: object deleted **/
  public static final char DELETE     = 'D';    // 
  /** modification type: all objects of given type deleted **/
  public static final char DELETEALL  = 'E';    // 

  
  /** transaction name: insert plain **/
  public static final String TX_INSERT_PLAIN                  = "insert plain";
  /** transaction name: insert object **/
  public static final String TX_INSERT_OBJECT                 = "insert object";
  /** transaction name: update plain **/
  public static final String TX_UPDATE_PLAIN                  = "update plain";
  /** transaction name: dummy update **/
  public static final String TX_DUMMY_UPDATE                  = "dummy update";
  /** transaction name: update serial **/
  public static final String TX_UPDATE_SERIAL                 = "update serial";
  /** transaction name: update serial and tableserial **/
  public static final String TX_UPDATE_SERIAL_AND_TABLESERIAL = "update serial and tableserial";
  /** transaction name: update tableserial **/
  public static final String TX_UPDATE_TABLESERIAL            = "update tableserial";
  /** transaction name: update object **/
  public static final String TX_UPDATE_OBJECT                 = "update object";
  /** transaction name: save **/
  public static final String TX_SAVE                          = "save";
  /** transaction name: delete object **/
  public static final String TX_DELETE_OBJECT                 = "delete object";
  /** transaction name: save list **/
  public static final String TX_SAVE_LIST                     = "save list";
  /** transaction name: delete list **/
  public static final String TX_DELETE_LIST                   = "delete list";
  /** transaction name: delete missing in list **/
  public static final String TX_DELETE_MISSING_IN_LIST        = "delete missing in list";

  
  

  /**
   * Creates a database object.
   *
   * @param db the logical {@link Db}-connection
   */
  public DbObject(Db db) {
    this.db = db;
  }

  
  /**
   * Creates a database object not associated to a logical {@link Db}-connection.
   * The connection must be set via {@link #setDb} in order to use it.
   */
  public DbObject() {
    // nothing to do
  }
  
  
  
  /**
   * Gets the default string value.<br>
   * The default implementation invokes {@link #toGenericString}.
   * 
   * @return the string value of this DbObject
   */
  @Override
  public String toString() {
    return toGenericString();
  }



  /**
   * Gets the string value: "{@link #getSingleName}[id/serial]".<br>
   * Example: "Product[344/2]"
   *
   * @return the string value of this DbObject
   */
  public String toGenericString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getSingleName());
    buf.append('[');
    buf.append(Long.toString(id));
    buf.append('/');
    buf.append(Long.toString(serial));
    buf.append(']');
    return buf.toString();
  }
  
  
  /**
   * Gets the some attributes and variables common to all objects of the same class.
   * Class variables for classes derived from DbObject are kept in an
   * instance of {@link DbObjectClassVariables}.
   * 
   * @return the class variables
   */
  public abstract DbObjectClassVariables getDbObjectClassVariables();
  
  

  /**
   * Gets the basename of the class of this object.<br>
   * The basename is the class name without the package name.
   * 
   * @return the basename of the Objects class
   */
  public String getClassBaseName ()  {
    return getDbObjectClassVariables().classBaseName;
  }
  
  
  /**
   * Gets the class name of this object.<br>
   * This is equivalent to getClass().getName() except for PartialDbObjects!
   *
   * @return the classname
   */
  public String getClassName()  {
    return getDbObjectClassVariables().className;
  }
  
  
  
  /**
   * Clones an object.<p>
   * Cloning an object yields a copy with id=0 and serial=0.
   * Needs to be overridden if object references, etc... have to be cloned too.
   * Subclasses should throw a {@link DbRuntimeException} if they should not be cloned
   * depending on the application logic (not CloneNotSupportedException).
   *
   * @return a clone of this object
   */
  @Override
  public DbObject clone() {
    DbObject obj;
    try {
      obj = (DbObject)super.clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new InternalError();    // should never happen
    }
    obj.setId(0);
    obj.setSerial(0);
    return obj;
  }
  
  
  /**
   * Returns true if object is only a partial implementation.
   * This is used in conjunction with PartialAppDbObject and the wurblet AppDbSuper
   * for abstract super classes in a table-per-class mapping.
   * 
   * @return true if partial implementation
   */
  public boolean isPartial() {
    return false;
  }
  
  
  /**
   * Returns whether instances of this class exist as database entities.
   * The default is true. An example of a non-entity object is PartialDbObject.
   * 
   * @return true if entity 
   */
  public boolean isEntity() {
    return true;
  }
  

  /**
   * Creates a new object with the same class and same logical
   * db connection as this object.
   * Must be overridden if further setup is necessary.
   * 
   * @return the new object 
   */
  public DbObject newObject() {
    try {
      DbObject obj = (DbObject)(getClass().newInstance());
      obj.setDb(getDb());
      return obj;
    }
    catch (Exception e) {
      DbGlobal.errorHandler.severe(getDb(), e, "creating new object failed");
      return null;
    }
  }
  

  /**
   * Sets the logical db connection for this object.
   * 
   * @param db the db connection
   */
  public void setDb (Db db)  {
    this.db = db;
  }

  
  /**
   * Get the logical db connection for this object.
   * 
   * @return the db connection
   */
  public Db getDb ()  {
    return db;
  }


  /**
   * Sets the unique ID of this object.
   * Does <em>not</em> set this object to be modified, see {@link #isModified}.
   * 
   * @param id the object id
   */
  public void setId (long id) {
    this.id = id;
  }

  /**
   * Gets the object ID.
   * If the object is deleted (negated ID) the returned
   * ID is still positive!
   * 
   * @return the object id
   */
  public long getId ()  {
    return id < 0 ? -id : id;
  }
  
  
  /**
   * Sets the serial number (modification count).
   * Does <em>not</em> set this object to be modified, see {@link #isModified}.
   * 
   * @param serial the serial number
   */
  public void setSerial (long serial) {
    this.serial = serial;
  }

  /**
   * Gets the serial number.
   * 
   * @return the serial number.
   */
  public long getSerial ()  {
    return serial;
  }


  /**
   * Sets the table serial number (table modification count).
   * Does <em>not</em> set this object to be modified, see {@link #isModified}.
   * 
   * @param tableSerial the new table serial
   */
  public final void setTableSerial (long tableSerial) {
    this.tableSerial = tableSerial;
  }

  /**
   * Gets the table serial.
   * 
   * @return the table serial
   */
  public long getTableSerial ()  {
    return tableSerial;
  }



  /**
   * Obtains a new ID for this object.<p>
   * If the object already has an ID or is deleted (negative ID)
   * the ID will _not_ change.
   */
  public void newId() {
    if (id == 0)  {
      try {
        id = getIdSource().nextId();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "unable to obtain a new ID"); 
      }
    }
  }

  
  /**
   * Reserves an ID.<p>
   * Reserved IDs are negative.
   * A new object with a reserved ID can be distinguished from
   * a deleted object by its serial. See also {@link #isVirgin}.
   * If the object already has an ID or is deleted (negative ID)
   * the ID will _not_ change.
   */
  public void reserveId() {
    if (id == 0)  {
      newId();
      id = -id;
    }
  }

  
  /**
   * Checks whether this object is already persistant in the db
   * or only residing in memory.
   * If an object isNew(), it means that it can be inserted.
   * This does not mean, that the object never has been stored in the db,
   * i.e. it is possible that the object just has been deleted.
   *
   * @return true if object is not in database, i.e. new (or deleted)
   */
  public boolean isNew() {
    // deleted objects get the id negated but keep their serial, i.e.
    // it's not sufficient to check only the serial.
    return id <= 0;
  }
  
  
  /**
   * Checks whether the object has a valid ID, i.e. not 0.
   * 
   * @return true if object got a valid id, whether deleted or not
   */
  public boolean isIdValid()  {
    return id != 0;
  }
  
  
  /**
   * Checks whether object is deleted (or got a reserved ID).
    * A deleted object with a reserved ID can be distinguished from
   * an object with a reserved ID by its serial. See also {@link #isVirgin}.
   * 
   * @return true if object has been deleted
   */
  public boolean isDeleted()  {
    return id < 0;
  }

  
  /**
   * Checks whether this object ever was stored in the database.
   * Virgin objects have a serial of zero.
   * Notice that an object is still "virgin", if it got a valid id via reserveId()
   * but has not been saved so far.
   *
   * @return true if object is virgin, false if it is or was stored in the database.
   */
  public boolean isVirgin() {
    return serial == 0;
  }

  
  /**
   * Sets the modified flag.<p>
   * For optimizations it is possible to skip objects that have not
   * been modified. The modified-attribute is cleared whenever the
   * object is saved (inserted or updated -- NOT insertPlain and updatePlain!). 
   * The application is responsible to set the modified flag!
   * This is usually done in the setter-methods of the db-attributes.
   * The wurblet 'DbMethods' does this for you with option '--tracked'
   *
   * @param modified is true if object is flagged modified, false if not.
   */
  public void setModified(boolean modified) {
    this.modified = modified;
  }
  
  
  /**
   * Checks if modification of this object are tracked, i.e. setModified()
   * is called properly. By default, DbObjects are *NOT* tracked!
   * This is quality measure to ensure that isModified() returns
   * false if and only if it hasn't been modified, i.e. the setters
   * check for modification. See the wurblet DbMethods.
   *
   * @return true if tracked, false otherwise (default)
   */
  public boolean isTracked()  {
    return false;
  }
  
  
  /**
   * Determines whether the object should be written to persistant
   * storage because it has been modified.<p>
   * By definition, an object is 'modified' if the object OR ANY
   * of its composites are modified. See DbRelations.wrbl on how
   * and when to override isModified().<p>
   * New objects are modified by definition!
   * Furthermore, isModified() will invoke the errorhandler if
   * isTracked() != true. DbMethods automatically override this
   * method if option --tracked is given.
   *
   * @return true if object is modified and should be saved().
   */
  public boolean isModified() {
    if (!isTracked()) {
      DbGlobal.errorHandler.severe(getDb(), null, 
              "isModified() invoked on untracked " + getSingleName() + " " + id);
    }
    return modified || isNew();     // new objects are modified by definition
  }
  
  
  /**
   * Determines whether this object got some of its attributes modified.<br>
   * It does not check whether some of its composites are modified!
   * This method can also be used for non-tracked objects.
   *
   * @return true if this object 
   */
  public boolean attributesModified() {
    return modified;
  }
  
  
  /**
   * Determines whether this object is allowed to be stored in DB.<br>
   * The default is true.
   * Objects not allowed to be saved will force save(), insert() and update()
   * to return 'false' and silently skipped in saveList().
   * 
   * @return true if saveable
   */
  public boolean isSaveable() {
    return true;
  }
  
  
  
  /**
   * Loads all linked objects.
   * <p>
   * Note: Linked objects are not to be mixed up with composites.
   * See the wurblet AppDbRelations for how this method is used.
   * @return true if all object loaded
   */
  public boolean loadLinkedObjects() { 
    return true;
  }

  /**
   * Saves all linked objects.<br>
   * <p>
   * Note: Linked objects are not to be mixed up with composites.
   * See the wurblet AppDbRelations for how this method is used.
   * @return true of all objects saved
   */
  public boolean saveLinkedObjects() { 
    return true;
  }

  /**
   * Deletes all linked objects   
   * <p>
   * Note: Linked objects are not to be mixed up with composites.
   * See the wurblet AppDbRelations for how this method is used.
   * @return true of all objects deleted
   */
  public boolean deleteLinkedObjects() { 
    return true;
  }

  
  /**
   * Gets the icon of this object.
   * The icon is displayed in trees, for example.
   * The default implementation returns {@link PlafGlobal#getIcon}("unknown").
   * 
   * @return the icon
   */
  public ImageIcon getIcon()  {
    return PlafGlobal.getIcon("unknown");
  }

  
  /**
   * Checks whether this object is referenced by other objects.
   * <p>
   * It is invoked before operations that may have an impact on the
   * referential integrity.
   * The default implementation returns false.
   * <p>
   * The application can assume a lazy context (invoked from is...Lazy) 
   * if invoked outside a transaction. This is just an optimization hint.
   * <p>
   * @return true if referenced
   */
  public boolean isReferenced() {
    boolean rv = false;
    if (!isNew())  {
      // new objects are never referenced because they simply don't exist in the db!
      if (getDb().isRemote()) {
        try {
          rv = getRemoteDelegate().isReferenced(id);
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
   * Checks whether this object can be removed.
   * <p>
   * It is invoked before operations that may have an impact on the
   * referential integrity.
   * The default implementation returns true if !isNew and !isReferenced.
   * Does not refer to the SecurityManager!
   * <p>
   * The application can assume a lazy context (invoked from is...Lazy) 
   * if invoked outside a transaction. This is just an optimization hint.
   * <p>
   * Notice that isRemovable() for performance reasons is not covered
   * by its own delegate method in remote connections.
   * Hence, if classes in your application require a different implementation
   * (!isNew && !isReferenced) you must provide such a remote method.
   * 
   * @return true if removable
   */
  public boolean isRemovable() {
    if (isNew()) {
      return false;
    }
    else  {
      if (getDb().isAutoCommit()) {
        // outside tx (invoked from GUI): lazy is sufficient.
        return !isReferencedLazy();
      }
      else  {
        // within tx: do a real check!
        return !isReferenced();
      }
    }
  }
  

  /**
   * Checks whether this object can be edited by the user.
   * <p>
   * The default implementation returns true.
   * Does not refer to the SecurityManager!
   * <p>
   * The application can assume a lazy context (invoked from is...Lazy) 
   * if invoked outside a transaction. This is just an optimization hint.
   * <p>
   * Notice that isEditable() for performance reasons is not covered
   * by its own delegate method in remote connections.
   * Hence, if classes in your application require a different implementation
   * you must provide such a remote method.
   * 
   * @return true if editable
   */
  public boolean isEditable()  {
    return true; 
  }
  
  
  
  /**
   * For the GUI a lazy method is provided to reduce the invocations.
   * Use this method instead of the non-lazy one if it's just a hint
   * for enabling certain gui-components (which is checked within a
   * succeeding tx anyway).
   *
   * @return true if the lazy method should invoke the non-lazy. false if return lazy value.
   */
  public boolean isLazyElapsed() {
    long curtime = System.currentTimeMillis();
    if (curtime - lastLazyMethodInvocationMs > lazyMethodInterval) {
      invalidateLazyValues();
      lastLazyMethodInvocationMs = curtime;
      return true;
    }
    return false;
  }

  /**
   * Copies the lazy values from one object to another.
   *
   * @param obj the object to copy the values from
   */
  public void copyLazyValues(DbObject obj) {
    lastLazyMethodInvocationMs  = obj.lastLazyMethodInvocationMs;
    referencedLazy              = obj.referencedLazy;
    referencedLazyValid         = obj.referencedLazyValid;
    removableLazy               = obj.removableLazy;
    removableLazyValid          = obj.removableLazyValid;
    editableLazy                = obj.editableLazy;
    editableLazyValid           = obj.editableLazyValid;
  }
  
  
  /**
   * Clears the lazy values.<br>
   * Makes it safe to invoke the lazy method and being sure
   * to invoke the non-lazy too.
   */
  public void invalidateLazyValues() {
    referencedLazyValid = false;
    removableLazyValid  = false;
    editableLazyValid   = false;  
  }
  
  
  /**
   * Asserts that lazy method is not used within a transaction.
   */
  protected void assertLazyNotWithinTX() {
    if (!getDb().isAutoCommit()) {
      throw new DbRuntimeException("lazy checks not allowed within transaction");
    }
  }
  
  
  /**
   * Gets the referenced state which has been valid for at least the
   * last lazyMethodInterval milliseconds.
   * 
   * @return true if referenced
   */
  public boolean isReferencedLazy() {
    assertLazyNotWithinTX();
    // first isLazyElapsed(), then valid == false! Don't change!!!
    if (isLazyElapsed() || referencedLazyValid == false) {
      referencedLazy = isReferenced();
      referencedLazyValid = true;
      if (referencedLazy) {
        // if it is referenced it is not removable as well!
        removableLazy = false;
        removableLazyValid = true;
      }
    }
    return referencedLazy;
  }
  
  
  /**
   * Gets the removeable state which has been valid for at least the
   * last lazyMethodInterval milliseconds.
   * 
   * @return true if removable
   */
  public boolean isRemovableLazy() {
    assertLazyNotWithinTX();
    if (isLazyElapsed() || removableLazyValid == false) {
      removableLazy = isRemovable();
      removableLazyValid = true;
      // not being removable does not necessarily mean it's referenced!
    }
    return removableLazy;
  }
  
  
  /**
   * Gets the editable state which has been valid for at least the
   * last lazyMethodInterval milliseconds.
   * 
   * @return true if editable
   */
  public boolean isEditableLazy() {
    assertLazyNotWithinTX();
    if (isLazyElapsed() || editableLazyValid == false) {
      editableLazy = isEditable();
      editableLazyValid = true;
    }
    return editableLazy;
  }
  

  
  /**
   * Determines whether modifications of this object are counted in the modification table.
   * The default implementation returns false. Must be overridden
   * if counting the modifications is desired.
   *
   * @param modType is one of the modtypes INSERT, UPDATE, DELETE, ...
   * @return true if count modification, false if not.
   * @see ModificationCounter
   */
  public boolean isCountingModification(int modType)  {
    return false;
  }
  
  
  /**
   * By default objects don't need to include the tableSerial in the
   * database table.
   * Override this method if object contains a TABLESERIAL-column.
   *
   * @return true if object is using the tableSerial column, false if not.
   */
  public boolean isTableSerialValid()  {
    return false;
  }
  
  
  /**
   * Determines whether each modification of this object (with respect to the database)
   * should be logged.
   * 
   * @param modType is one of the modtypes INSERT, UPDATE, DELETE, ...
   * @return true if log modification, false if not.
   * @see ModificationLog
   */
  public boolean isLoggingModification(int modType)  {
    return false;
  }
  
  
  /**
   * Objects are identical if their IDs and classes are identical.
   * IDs are *NOT* necessarily unique among all db-tables! (see poolkeeper).
   * Does not throw any exception, so its safe to add null-objects
   * to set of DbObjects.
   * 
   * @param object the object to test for equality
   */
  @Override
  public boolean equals (Object object) {
    try {
      return ((DbObject)object).getId() == getId() && getClass() == object.getClass();
    }
    catch (Exception ex) {
      return false;
    }
  }


  
  /** 
   * Compare two objects.<br>
   * We are using the ID to compare objects. Makes only sense within
   * the same class.
   * Must be overridden if other sortings are desired.
   * Does not throw any exception, so its safe to add null-objects
   * to a sorted set of DbObjects.
   * 
   * @param obj the object to compare this object with
   */
  public int compareTo(DbObject obj) {
    try {
      int rv = Compare.compareLong(getId(), obj.getId());
      if (rv == 0) {
        // should not happen, but one never knows: check the class
        if (getClass() != obj.getClass()) {   // != is okay here
          rv = getClassName().compareTo(obj.getClassName());
        }
      }
      return rv;
    }
    catch (Exception ex)  {
      return 1;
    }
  }  
  


  /**
   * The hashcode for a Db-object is simply the ID.
   * It is ok -- according to the contract of hashCode() -- that objects
   * in different tables with the same id may return the same hashcode.
   *
   * @return  a hash code value for this object.
   * @see     java.lang.Object#equals(java.lang.Object)
   * @see     java.util.Hashtable
   */
  @Override
  public int hashCode() {
    return (int)getId();
  }  
  
  
  
  
  /**
   * Get the IdSource.
   * If the IdSource is null, the method will look for
   * a property file which is the classname + ".properties".
   * The property file must contain an idsource-line very
   * similar to that in Db.properties. However, the "objectid" means 
   * "use the source from the db" (which is the default anyway).
   *
   * If there is no such file, the source from the db will be used.
   *
   * @return Value of property idSource.
   */
  public IdSource getIdSource() {
    
    IdSource idSource = db.getIdSource(getDbObjectClassVariables().idSourceId);
    
    if (idSource == null) {
      /**
       * not initialized so far.
       * try to get from property file
       */
      try {
        // look for file first
        String ids = getDbObjectClassVariables().getProperty("idsource");
        idSource = ids == null ? db.getDefaultIdSource() : new IdSourceConfigurator(ids).connect(db);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "IdSource configuration failed for " + this.getClassBaseName());
      }
      
      // set ID Source for this class
      getDbObjectClassVariables().idSourceId = db.addIdSource(idSource);
    }
    
    return idSource;
  }  
  

  
  
  
  
  /**
   * Column names may be preceeded by a prefix. This is especially useful
   * for joined tables auch as "table-per-class". The prefix must contain
   * a trailing dot, just the name.
   * 
   * The default is null.
   * 
   * @return the SQL prefix
   */
  public String getSqlPrefix()  {
    return null;
  }
  
  
  /**
   * Gets the prefix with a trailing dot or "" if no prefix.
   * 
   * @return the SQL prefix
   */
  public String getSqlPrefixWithDot() {
    String prefix = getSqlPrefix();
    return prefix == null ? "" : (prefix + ".");
  }
  

  /**
   * Reads the values from a result-set into this object.
   *
   * @param rs is the result set (wrapper)
   * @param withLinkedObjects is true if load linked objects too
   *
   * @return true if values read, false if some error
   */
  public boolean readFromResultSetWrapper(ResultSetWrapper rs, boolean withLinkedObjects)  {
    getDb().setAlive(true);   // keep the (local) db alive for long running retrievals
    boolean rv = getFields(rs) && (!withLinkedObjects || loadLinkedObjects());
    setModified(false);
    return rv;
  }
  
  
  /**
   * Reads the values from a result-set into this object.
   *
   * @param rs is the result set (wrapper)
   *
   * @return true if values read, false if some error
   */
  public final boolean readFromResultSetWrapper(ResultSetWrapper rs)  {
    return readFromResultSetWrapper(rs, true);
  }
  


  /**
   * Loads an object from the database by its unique ID.<p>
   * For local db connections the current object's attributes will be
   * replaced by the database values (i.e. <em>this</em> object is returned).
   * For remote connections, a copy of the object in the server is returned.
   * Hence, applications should always create a new object and invoke
   * select and don't make any further assumptions. This applies to
   * all select methods returning an object! Example:
   * <pre>
   *    Customer customer = new Customer(db).select(customerId);
   * </pre>
   * Since Java 1.5 covariance is supported and you should consider
   * overriding the method and cast the return value to the appropriate class.
   * 
   * @param id is the object id
   * @param withLinkedObjects is true if load linked objects too
   *
   * @return object if loaded, null if no such object
   */
  public DbObject select (long id, boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        DbObject obj = getRemoteDelegate().select(id, withLinkedObjects);
        if (obj != null)  {
          obj.setDb(getDb());
        }
        return obj;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote select failed"); 
      }
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareSelectStatement());
      st.setLong(1, id);
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next() && readFromResultSetWrapper(rs, withLinkedObjects)) {
        rs.close();
        return this;
      }
      rs.close();
    }
    return null;
  }
  
  
  /**
   * Loads an object from the database by its unique ID.<p>
   * For local db connections the current object's attributes will be
   * replaced by the database values (i.e. <em>this</em> object is returned).
   * For remote connections, a copy of the object in the server is returned.
   * Hence, applications should always create a new object and invoke
   * select and don't make any further assumptions. This applies to
   * all select methods returning an object! Example:
   * <pre>
   *    Customer customer = new Customer(db).select(customerId);
   * </pre>
   * Since Java 1.5 covariance is supported and you should consider
   * overriding the method and cast the return value to the appropriate class.
   * 
   * @param id is the object id
   *
   * @return object if loaded, null if no such object
   */
  public DbObject select (long id)  {
    return select(id, true);
  }
  
  
  
  /**
   * Load the object from the database with exclusive lock (aka write lock).
   * This is implemented via "SELECT FOR UPDATE". 
   *
   * @param id is the object id
   * @param withLinkedObjects is true if load linked objects too
   *
   * @return object if loaded, null if no such object
   */
  public DbObject selectLocked (long id, boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        DbObject obj = getRemoteDelegate().selectLocked(id, withLinkedObjects);
        if (obj != null) {
          obj.setDb(getDb());
        }
        return obj;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectLocked failed"); 
      }
    }
    else  {
      /**
       * Notice that some dbms don't provide a select for update. In this case, 
       * the dummyUpdate()-method should be automatically used by tentackle
       * (allthough it's not really the same, but better than nothing).
       * We did not implement it so far, because all tentackle-supported dbms
       * provide a SELECT FOR UPDATE.
       *
       * However, in Oracle (version 8 at the time of writing) the SELECT FOR UPDATE
       * is broken under the following circumstances:
       * If an object is selected without "FOR UPDATE" and IMMEDIATELY after
       * a transaction is started AND the same object is read with
       * SELECT FOR UPDATE within that transaction, a "fetch out of sequence" 
       * ORA-01002 exception is raised.
       * Furthermore, if a transaction that contains a SELECT FOR UPDATE for a given
       * object and the same object is selected IMMEDIATELY after the commit
       * of the transaction, the db-connection hangs!
       */
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareSelectLockedStatement());
      st.setLong(1, id);
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next() && readFromResultSetWrapper(rs, withLinkedObjects)) {
        rs.close();
        return this;
      }
      rs.close();
    }
    return null;
  }
  
  
  /**
   * Load the object from the database with exclusive lock (aka write lock).
   * This is implemented via "SELECT FOR UPDATE".
   * The linked objects are loaded as well.
   *
   * @param id is the object id
   *
   * @return object if loaded, null if no such object
   */
  public DbObject selectLocked (long id)  {
    return selectLocked(id, true);
  }

  
  
  /**
   * Same as {@link #select} but makes sure that the object is selected
   * in a valid context, i.e. performs optional class specific
   * initialization that are beyond a normal DbObject.<p>
   * This method primarily is provided for being overridden by
   * {@link org.tentackle.appworx.AppDbObject#selectInValidContext}.
   * The default implementation simply invokes select().
   *
   * @param id is the object id
   * @param withLinkedObjects is true if load linked objects too
   *
   * @return object if loaded, null if no such object
   */
  public DbObject selectInValidContext(long id, boolean withLinkedObjects) {
    
    if (getDb().isRemote()) {
      try {
        DbObject obj = getRemoteDelegate().selectInValidContext(id, withLinkedObjects);
        if (obj != null) {
          // set the db only
          obj.setDb(getDb());
        }
        return obj;     // obj != this !!!
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectInValidContext failed");
        return null;
      }      
    }
    // defaults to standard select
    return select(id, withLinkedObjects);
  }
  
  
  /**
   * Reloads the object.<p>
   * Note: to make sure that any lazy inits are cleared,
   * the returned object is always a new object.
   * 
   * @return the object if reloaded, else null (never <em>this</em>)
   */
  public DbObject reload() {
    return newObject().select(id);
  }
  
  
  /**
   * Reloads the object with a write lock.
   * Note: to make sure that any lazy inits are cleared,
   * the returned object is always a new object.
   * 
   * @return the object if reloaded, else null (never <em>this</em>)
   */
  public DbObject reloadLocked() {
    return newObject().selectLocked(id);
  }


  /**
   * Selects all objects of this class and returns the ResultSetWrapper.
   * 
   * @return the result set
   */
  public ResultSetWrapper selectAllResultSet() {
    getDb().assertNotRemote();
    int stmtId = getSelectAllStatementId();
    if (stmtId == 0 || alwaysPrepare()) {
      stmtId = getDb().prepareStatement(getSqlSelectAllFields());
      setSelectAllStatementId(stmtId);
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    return st.executeQuery();    
  }

  
  /**
   * Selects the next object from a resultset.
   * Applications should close the resultset if null is returned.
   * 
   * @param rs the result set
   * @param withLinkedObjects is true if load linked objects too
   * @return the next object, null if end of set
   */
  public DbObject selectNext(ResultSetWrapper rs, boolean withLinkedObjects)  {
    getDb().assertNotRemote();
    if (rs.next() && readFromResultSetWrapper(rs, withLinkedObjects))  {
      return this;
    }
    return null;
  }

  
  
  /**
   * Selects all objects of this class as a {@link java.util.List}.
   * 
   * @param withLinkedObjects is true if load linked objects too
   * @return the list of objects
   */
  public List<? extends DbObject> selectAll(boolean withLinkedObjects) {
    if (getDb().isRemote())  {
      try {
        List<? extends DbObject> list = getRemoteDelegate().selectAll(withLinkedObjects);
        for (DbObject obj : list) {
          obj.setDb(getDb());
        }
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectAll failed");
        return null;
      }
    }
    else  {
      List<DbObject> list = new ArrayList<DbObject>();
      ResultSetWrapper rs = selectAllResultSet();
      DbObject obj;
      while ((obj = newObject().selectNext(rs, withLinkedObjects)) != null)  {
        list.add(obj);
      }
      rs.close();
      return list;
    }
  }
  
  
  /**
   * Selects all objects of this class as a {@link java.util.List}.
   * The linked objects are loaded as well.
   * 
   * @return the list of objects
   */
  public List<? extends DbObject> selectAll()  {
    return selectAll(true);
  }

  
  /**
   * Selects all objects of this class as a {@link DbCursor}.
   * Will prompt the user if too much data received.
   *
   * @param withLinkedObjects is true if load linked objects too
   * @return the cursor, never null
   */
  @SuppressWarnings("unchecked")
  public DbCursor<? extends DbObject> selectAllCursor(boolean withLinkedObjects) {
    
    if (getDb().isRemote())  {
      try {
        return new SimpleDbCursor<DbObject>(getDb(), getRemoteDelegate().selectAllCursor(withLinkedObjects));
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectAllCursor failed");
        return null;  // not reached
      }
    }
    
    return new SimpleDbCursor<DbObject>(getDb(), (Class<DbObject>)getClass(), selectAllResultSet(), withLinkedObjects);
  }
  
  
  /**
   * Selects all objects of this class as a {@link DbCursor}.
   * Will prompt the user if too much data received.
   * The linked objects are loaded as well.
   *
   * @return the cursor, never null
   */
  public DbCursor<? extends DbObject> selectAllCursor() {
    return selectAllCursor(true);
  }
  
  
  /**
   * Selects the serial-number for a given object id.
   * 
   * @param id the object id
   * @return the serial for that id, -1 if no such object
   */
  public long selectSerial(long id) {
    if (getDb().isRemote()) {
      try {
        return getRemoteDelegate().selectSerial(id);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectSerial failed");
        return 0;  // not reached
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareSelectSerialStatement());
      st.setLong(1, id);
      long ser = -1;
      ResultSetWrapper rs = st.executeQuery();
      if (rs.next()) {
        ser = rs.getLong(1);
      }
      rs.close();
      return ser;
    }
  }
  
  
  /**
   * Selects the serial-number for this object.
   * 
   * @return the serial for this object, -1 object not stored in database.
   */
  public long selectSerial() {
    return selectSerial(getId());
  }
  
  
  /**
   * Copies an object from one db connection to another.
   *
   * @param destDb the destination db
   * @param plain is true to use insertPlain (recommended), else insert
   * @return true if inserted in destDb, false if failed
   */
  public boolean copyToDb(Db destDb, boolean plain) {
    Db oldDb = getDb();
    setDb(destDb);
    boolean rv = plain ? insertPlain() : insert();
    setDb(oldDb);
    return rv;
  }
  
  

  /**
   * The logger for {@link DbObject#copyAllToDb}.
   */
  public static interface CopyAllToDbLogger {
    /**
     * Log object copied.
     *
     * @param dbObject the object to log
     */
    public void log(DbObject dbObject);
    
    /**
     * Log object copy failed.
     *
     * @param dbObject the object to log
     */
    public void logError(DbObject dbObject);
  }
  
  
  /**
   * Copies all objects of this class to another db.
   *
   * @param destDb is the destination DB
   * @param plain is true to use insertPlain (recommended), 
   *        else use insert (if superclass is abstract for example)
   * @param logger is an optional logger (null = none)
   *
   * @return the number copied or a negative count - 1 if error
   */ 
  public int copyAllToDb(Db destDb, boolean plain, CopyAllToDbLogger logger) {
    int count = 0;
    DbCursor cursor = selectAllCursor(false);
    if (cursor != null) {
      while (cursor.next())  {
        DbObject obj = cursor.getObject();
        if (obj != null) {
          if (obj.copyToDb(destDb, plain) == false)  {
            count = -1 - count;
            logger.logError(obj);
            break;
          }
          if (logger != null) {
            logger.log(obj);
          }
          count++;
        }
      }
      cursor.close();
    }
    return count;
  }
  

  /**
   * Insert this object into the database without any further processing
   * (i.e. prepareSetFields, linked objcets, mod counting, logging, etc...).
   * 
   * @return true if object inserted, false if unique violation
   */
  public boolean insertPlain()  {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().insertPlain(this);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote insertPlain failed");
        return false;
      }      
    }
    else  {
      int stmtId = prepareInsertStatement();
      PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
      setFields (st);         // set fields
      return st.executeUpdate() == 1;
    }
  }
  
  
  /**
   * Deletes this object from the database without any further processing
   * (i.e. linked objcets, mod counting, logging, etc...).
   * 
   * @return true if deleted, false if no such object
   */
  public boolean deletePlain()  {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().deletePlain(id, serial);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deletePlain failed");
        return false;
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareDeleteStatement());
      st.setLong(1, id);
      st.setLong(2, serial);
      return st.executeUpdate() == 1;
    }
  }
  

  /**
   * Updates this object to the database without any further processing 
   * (i.e. prepareSetFields, linked objcets, mod counting, logging, etc...).
   * 
   * @return true if object updated, false if unique violation or no such object
   */
  public boolean updatePlain ()  {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().updatePlain(this);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote updatePlain failed");
        return false;
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateStatement());
      setFields (st);         // set fields
      boolean rv = st.executeUpdate() == 1;
      if (rv) {
        serial++;
      }
      return rv;
    }
  }

  
  /**
   * Performs a dummy update.<br>
   * The method is provided as an alternative to {@link #reloadLocked} or {@link #selectLocked}
   * to lock the object during a transaction by updating the ID without changing it.
   * 
   * @return true if updated, false if no such object ID
   */
  public boolean dummyUpdate ()  {
    
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().dummyUpdate(this);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote dummyUpdate failed");
        return false;
      }      
    }
    else  {
      // for Oracle notes see selectLocked().
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareDummyUpdateStatement());
      boolean oldcommit = getDb().begin(TX_DUMMY_UPDATE);
      st.setLong(1, id);
      st.setLong(2, id);
      if (st.executeUpdate() == 0)  {
        getDb().rollback(oldcommit);
        return false;
      }
      getDb().commit(oldcommit);
      return true;
    }
  }
  
  
  /**
   * Updates and increments the serial number of this object.<br>
   * The method is provided to update an object with isModified() == true
   * and 'modified' == false, i.e. an object that itself is not modified
   * but some of its composites. In such a case it is not necessary
   * to update the whole object. However, it is sometimes necessary to
   * update the serial to indicate 'some modification' and to make
   * sure that this object is part of the transaction.
   * Whether it is necessary or not depends on the application.
   * 
   * @return true if serial 
   * @see #isUpdatingSerialEvenIfNotModified
   */
  public boolean updateSerial ()  {
    
    if (getDb().isRemote())  {
      try {
        if (getRemoteDelegate().updateSerial(id, serial)) {
          serial++;
          return true;
        }
        return false;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote updateSerial failed");
        return false;
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateSerialStatement());
      boolean oldcommit = getDb().begin(TX_UPDATE_SERIAL);
      st.setLong(1, id);
      st.setLong(2, serial);
      if (st.executeUpdate() == 0)  {
        getDb().rollback(oldcommit);
        return false;
      }
      getDb().commit(oldcommit);
      serial++;   // SQL statement incremented serial successfully
      return true;
    }
  }
  
  
  /**
   * Same as {@link #updateSerial} but updates tableSerial as well.
   * Notice: the tableSerial is NOT modified in the current object,
   * but only in the database!
   * 
   * @return true if serial and tableSerial updated
   */
  public boolean updateSerialAndTableSerial ()  {
    
    if (getDb().isRemote())  {
      try {
        if (getRemoteDelegate().updateSerialAndTableSerial(id, serial, tableSerial)) {
          serial++;
          return true;
        }
        return false;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote updateSerialAndTableSerial failed");
        return false;
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateSerialAndTableSerialStatement());
      boolean oldcommit = getDb().begin(TX_UPDATE_SERIAL_AND_TABLESERIAL);
      st.setLong(1, tableSerial);
      st.setLong(2, id);
      st.setLong(3, serial);
      if (st.executeUpdate() == 0)  {
        getDb().rollback(oldcommit);
        return false;
      }
      getDb().commit(oldcommit);
      serial++;   // SQL statement incremented serial successfully
      return true;
    }
  }
  
  
  /**
   * Updates the tableSerial only.<br>
   * The method is provided to explicitly force this object to expire
   * in caches. Useful, for example, if only editedBy() is changed
   * and this change should be reflected in other apps.
   * Notice: the tableSerial is NOT modified in the current object,
   * but only in the database!
   *
   * @return true if updated, else false
   */
  public boolean updateTableSerial ()  {
    
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().updateTableSerial(id);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote updateTableSerial failed");
        return false;
      }      
    }
    else  {
      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateTableSerialStatement());
      boolean oldcommit = getDb().begin(TX_UPDATE_TABLESERIAL);
      st.setString(1, getTableName());
      st.setLong(2, id);
      if (st.executeUpdate() == 0)  {
        getDb().rollback(oldcommit);
        return false;
      }
      getDb().commit(oldcommit);
      return true;
    }
  }
  
  
  /**
   * Determines whether in updates of composite objects unmodified objects in the
   * update path get at least the serial updated or are not touched at all.
   * The default is to leave unmodified objects untouched. However, in some applications
   * it is necessary to update the master object if some of its childs are updated (usually
   * to trigger something, e.g. a cache-update).<p>
   * The default implementation returns false.
   * Override this method to change to 'true'.
   *
   * @return true if update serial even if object is unchanged
   * @see #updateObject
   */
  public boolean isUpdatingSerialEvenIfNotModified()  {
    return false;
  }
  


  /**
   * Determines whether this object becomes the {@code txObject} (see {@link Db#setTxObject}) within a transaction.
   * By default, only objects initiating a transaction are txObjects.
   * However, for composites the top-level object usually is also the txObject, even if the
   * transaction involves different txobjects. As a consequence, the current txObject can
   * change during a transaction.
   *
   * @return true if this is a txObject.
   */
  public boolean isTxObject() {
    return false;
  }
  

  
  /**
   * Does any preprocessing before delete, insert or update.
   *
   * @param modType is the modification type: DELETE, INSERT or UPDATE
   * @return true if preprocessing ok, false if not
   */
  public boolean initModification(char modType) {
    if (isCountingModification(modType))  {
      setTableSerial(countModification(modType != DELETE)); // delete always counts to enable tableSerial change detection by cache
      // will never return on failure -> errorHandler
    }
    return true;
  }
  
  
  /**
   * Does any postprocessing after delete, insert or update.
   *
   * @param modType is the modification type: DELETE, INSERT or UPDATE
   * @return true if postprocessing ok, false if not
   */
  public boolean finishModification(char modType) {
    if (isLoggingModification(modType))  {
      return logModification(modType);
    }
    return true;
  }
  
  
  /**
   * Does any update postprocessing for objects not being updated (because not modified
   * for some reason, e.g. only composites modified).
   * The default implementation does nothing. See {@link org.tentackle.appworx.AppDbObject}.
   * 
   * @return true if done, false if failed
   */
  public boolean finishNotUpdated() {
    return true;
  }




  /**
   * Begins a transaction.<br>
   * Also sets the txObject if
   * a transaction is started or {@link #isTxObject()} returns true.
   *
   * @param txName the transaction name
   * @return true if tx begun
   */
  protected boolean beginTx(String txName) {
    boolean oldcommit = getDb().begin(txName);
    if (oldcommit || isTxObject()) {
      getDb().setTxObject(this);
    }
    return oldcommit;
  }
  
  
  

  /**
   * Inserts this (new) object into the database.<p>
   * Note: this method does *NOT* set the ID and should be used
   * by the application with great care! Use {@link #save} instead!
   *
   * @param   withLinkedObjects true if insert also linked objects
   * @return  true if done, false if duplicate
   */
  public boolean insertObject (boolean withLinkedObjects)  {
    
    if (prepareSetFields() == false) {
      return false;
    }
    
    if (getDb().isRemote())  {
      try {
        DbObjectResult result = getRemoteDelegate().insertObject(this, withLinkedObjects);
        id = result.id;
        serial = result.serial;
        tableSerial = result.tableSerial;
        getDb().setUniqueViolation(result.uniqueViolation);
        if (result.result)  {
          setModified(false);
        }
        return result.result;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote insertObject failed");
        return false;
      }      
    }
    else  {

      int stmtId = prepareInsertStatement();

      boolean oldcommit = beginTx(TX_INSERT_OBJECT);

      if (initModification(INSERT) == false) {
        getDb().rollback(oldcommit);
        return false;
      }

      long oldId = id;        // save it in case of errors
      if (id < 0) {
        id = -id;             // could have been "deleted" before
      }
      serial++;               // count serial before!

      boolean rv = !withLinkedObjects || saveLinkedObjects();

      if (rv) {
        PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
        rv = setFields(st) >= 0 &&
             st.executeUpdate() == 1 &&
             finishModification(INSERT);
      }

      if (rv) {
        getDb().commit(oldcommit);
        setModified(false);
        return true;
      }
      else  {
        getDb().rollback(oldcommit);
        serial--;
        id = oldId;
        return false;
      }
    }
  }

  
  /**
   * Inserts this (new) object into the database with linked objects.<p>
   * The modified attribute gets cleared if insert was successful.
   * It is also verified that the object {@link #isSaveable}.<p>
   * Note: this method does *NOT* set the ID and should be used
   * by the application with great care! Use {@link #save} instead!
   *
   * @return true if inserted, false if duplicate or not saveable.
   */
  public boolean insert() {
    return isSaveable() && insertObject(true);
  }



  /**
   * Updates this object to the database.<br>
   * The modified attribute gets cleared if insert was successful.
   * Note: this method should be used by the application with great care! 
   * Use {@link #save} instead!
   *
   * @param  withLinkedObjects true if update also linked objects
   * @return true if done, false if duplicate
   */
  public boolean updateObject (boolean withLinkedObjects)  {

    if (prepareSetFields() == false) {
      return false;
    }
    
    if (getDb().isRemote())  {
      try {
        DbObjectResult result = getRemoteDelegate().updateObject(this, withLinkedObjects);
        id = result.id;
        serial = result.serial;
        tableSerial = result.tableSerial;
        getDb().setUniqueViolation(result.uniqueViolation);
        if (result.result)  {
          setModified(false);
        }
        return result.result;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote updateObject failed");
        return false;
      }      
    }
    
    else  {
      
      boolean oldcommit = beginTx(TX_UPDATE_OBJECT);  // start transaction

      boolean updated = true; // cleared to false if this object was NOT updated, only composites
      
      if (isTracked() && modified == false && !isNew()) {   // DON'T USE isModified() here!
        // the object itself is not modified, but some of its composites
        if (isUpdatingSerialEvenIfNotModified()) {
          if (initModification(UPDATE) == false)  {  // necessary cause serial changes!
            getDb().rollback(oldcommit);
            return false;
          }
          // DON'T USE updateSerial() here! We need the old serial unchanged
          PreparedStatementWrapper st = getDb().getPreparedStatement(prepareUpdateSerialStatement());
          // id can't be negative cause if isNew() above
          st.setLong(1, id);
          st.setLong(2, serial);
          if ((withLinkedObjects && saveLinkedObjects() == false) ||
              st.executeUpdate() != 1)  {
            getDb().rollback(oldcommit);
            return false;
          }
        }
        else  {
          // save linked objects without updating the serial of this object
          if ((withLinkedObjects && saveLinkedObjects() == false) ||
              finishNotUpdated() == false) {
            getDb().rollback(oldcommit);
            return false;
          }
          updated = false;    // this object was not updated! -> no modlog as well, no serial++!
        }
        if (updated && finishModification(UPDATE) == false)  {
          getDb().rollback(oldcommit);
          return false;        
        }
      }
      
      else  {
        // normal update
        if (initModification(UPDATE) == false)  {
          getDb().rollback(oldcommit);
          return false;
        }

        int stmtId = prepareUpdateStatement();
        
        long oldId = id;        // save in case of error
        if (id < 0) {
          id = -id; // was deleted: reuse ID
        }

        boolean ok = !withLinkedObjects || saveLinkedObjects();

        if (ok) {
          PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
          ok = setFields(st) >= 0 &&
               st.executeUpdate() == 1 &&
               finishModification(UPDATE);
        }

        if (!ok) {
          getDb().rollback(oldcommit);
          id = oldId;
          return false;
        }
      }

      getDb().commit(oldcommit);
      
      if (updated) {
        serial++; // serial is already incremented in the SQL-statement!
      }
      
      setModified(false);   // clear modified flag
      
      return true;
    }
  }


  /**
   * Updates this object to the database.<br>
   * The modified attribute gets cleared if insert was successful.
   * It is also verified that the object {@link #isSaveable}.<p>
   * Note: this method should be used by the application with great care! 
   * Use {@link #save} instead!
   *
   * @return true if done, false if duplicate or object is not saveable.
   */
  public boolean update () {
    return isSaveable() && updateObject(true);
  }

  


  /**
   * Saves this object.<br>
   * This is the standard method applications should use to insert or update
   * objects.<p>
   * If the ID is 0, a new ID is obtained and this object inserted.
   * Otherwise this object is updated.
   * The modified attribute gets cleared if save() was successful.
   *
   * @return true of done, false if duplicate
   */
  public boolean save ()  {
    
    if (getDb().isRemote())  {
      
      if (prepareSetFields() == false)  {
        return false;   // we do that on the client side BEFORE update/insert
      }
      
      try {
        DbObjectResult result = getRemoteDelegate().save(this);
        id = result.id;
        serial = result.serial;
        tableSerial = result.tableSerial;
        getDb().setUniqueViolation(result.uniqueViolation);
        if (result.result)  {
          setModified(false);
        }
        return result.result;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote save failed");
        return false;
      }      
    }
    
    else  {
      
      long oldId = id;    // save in case of error
      
      if (id <= 0)  {     // new or reserved
        if (id == 0)  {
          boolean oldcommit = beginTx(TX_SAVE);   // obtaining ID should be used in transaction with insert()
          // obtain new id
          newId();
          if (insert()) { // may be overridden!
            getDb().commit(oldcommit);
            return true;
          }
          getDb().rollback(oldcommit);
        }
        else  {
          // if id has been reserved it will be made positive in insert or update
          // dont't use insertObject() bec. insert() OR insertObject()
          return insert(); // may be overridden!
        }
      }
      else  {
        // dto. as for insert()
        return update();
      }
      
      // set back old Id in case of change
      id = oldId;
      return false;   // some error
    }
  }

  
  /**
   * Wrapper for {@link #save()}.
   * <p>
   * Unique violations and optimistic locking failures don't throw
   * exceptions, because if these operations fail, the application can and
   * must deal with it. However, in some situations (bulk operations) it is sufficient
   * to just throw an exception instead.
   * @throws DbRuntimeException thrown if save returned false
   */
  public void _save() throws DbRuntimeException {
    if (!save()) {
      if (getDb().isUniqueViolation()) {
        throw new DbRuntimeException("unique violation detected for " + this);
      }
      else  {
        throw new DbRuntimeException("cannot save " + this);
      }
    }
  }
  
  
  /**
   * Wrapper for {@link #delete()}.
   * <p>
   * Optimistic locking or delete failures don't throw
   * exceptions, because if these operations fail, the application can and
   * must deal with it. However, in some situations (bulk operations) it is sufficient
   * to just throw an exception instead.
   * @throws DbRuntimeException thrown if delete returned false
   */
  public void _delete() throws DbRuntimeException {
    if (!delete()) {
      throw new DbRuntimeException("cannot delete " + this);
    }
  }

  
  /**
   * Try to update an object first and if that failes, try to insert.<br>
   * This method is used for transferring an Object from one DB to another
   * as it is the case in RMI-connections, where the ID is transmitted,
   * but the object may or may not exist in the other database.
   * Notice that the modified-attribute remains unchanged!
   * 
   * @param  withLinkedObjects true if insert/update also linked objects
   * @return true if done, false if failed
   */
  public boolean syncObject(boolean withLinkedObjects) {
    DbObject old = newObject().select(id);
    if (old != null)  {
      // object exists
      serial = old.serial;
      return updateObject(withLinkedObjects);
    }
    return insertObject(withLinkedObjects);
  }

  
  /**
   * Try to update an object first and if that failes, try to insert.<br>
   * This method is used for transferring an Object from one DB to another
   * as it is the case in RMI-connections, where the ID is transmitted,
   * but the object may or may not exist in the other database.
   * It is also verified that the object {@link #isSaveable}.<p>
   * Notice that the modified-attribute remains unchanged!
   * 
   * @return true if done, false if failed
   */
  public boolean sync() {
    return isSaveable() && syncObject(true);
  }



  /**
   * Removes this object from the database.<br>
   * Note: this method should be used by the application with great care! 
   * Use {@link #delete} instead!
   *
   * @param withLinkedObjects true if remove linked objects too.
   * @return true if removed, false if no such object
   */
  public boolean deleteObject (boolean withLinkedObjects)  {

    if (isNew()) {
      return false;    // don't delete new objects!
    }
    
    if (getDb().isRemote())  {
      try {
        DbObjectResult result = getRemoteDelegate().deleteObject(this, withLinkedObjects);
        id = result.id;
        serial = result.serial;
        tableSerial = result.tableSerial;
        return result.result;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteObject failed");
        return false;
      }      
    }
    else  {

      boolean oldcommit = beginTx(TX_DELETE_OBJECT);

      if (initModification(DELETE) == false) {
        getDb().rollback(oldcommit);
        return false;        
      }

      PreparedStatementWrapper st = getDb().getPreparedStatement(prepareDeleteStatement());
      st.setLong(1, id);
      st.setLong(2, serial);

      if (st.executeUpdate() != 1 ||
          (withLinkedObjects && deleteLinkedObjects() == false) ||
          finishModification(DELETE) == false) {
        getDb().rollback(oldcommit);
        return false;
      }

      getDb().commit(oldcommit);
      
      id = -id;     // make ID reserved again, i.e. mark object as being deleted
      
      return true;
    }
  }

  
  /**
   * Removes this object from the database (with linked objects, i.e. standard-case).
   * A removed object will also get the modified attribute set by definition,
   * because it is {@link #isNew} again.
   * It is also verified that the object {@link #isSaveable}.
   *
   * @return true if removed, false if failed
   */
  public boolean delete()  {
    return isSaveable() && deleteObject(true);
  }
  
  

  /**
   * Marks an object to be deleted.<br>
   * This is done by negating its id.
   * If the object is already marked deleted the method does nothing.
   * Must be overridden if the object is composite, i.e.
   * all its composites must be markDeleted as well.
   * Note: an object with a negative ID is always {@link #isModified}.
   */
  public void markDeleted() {
    // getId() always returns >= 0!
    setId(-getId());
  }
  

  /**
   * Counts a modification for the class of this object.
   *
   * @param optimize is true if optimize modcounting within transaction
   *
   * @return the table serial if isTableSerialValid()==true, 
   *          -1 if isCountModificationAllowed() == false
   * @see ModificationCounter
   */
  public long countModification (boolean optimize)  {
    getDb().assertNotRemote();
    return getDb().isCountModificationAllowed() ?
      getDb().getModificationCounter(getTableName()).countModification(isTableSerialValid(), optimize) :
      -1;
  }
  

  /**
   * Counts a modification for the class of this object with optimization.
   * 
   * @return the table serial if isTableSerialValid()==true, 
   *          -1 if isCountModificationAllowed() == false
   * @see ModificationCounter
   */
  public long countModification() {
    return countModification(true);
  }
  

  /**
   * Selects the current modification counter for the class of this object.
   * 
   * @return the modification counter
   * @see ModificationCounter
   */
  public long selectModification () {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().selectModification();
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectModification failed");
        return 0;
      }      
    }
    else  {
      return getDb().getModificationCounter(getTableName()).selectModification();
    }
  }
  
  
  
  /**
   * Determines the objects with a tableSerial starting at a given serial.
   * Useful to cleanup caches for example.
   *
   * @param oldSerial non-inclusive lower bound for tableSerial (>oldSerial)
   * @return pairs of longs, the first being the ID, the second the tableserial, never null
   */
  public long[] selectExpiredTableSerials(long oldSerial) {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().selectExpiredTableSerials(oldSerial);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByExpiredTableSerial failed");
        return null;
      }      
    }
    else  {
      if (getSelectExpiredTableSerials1StatementId() == 0)  {
        setSelectExpiredTableSerials1StatementId(db.prepareStatement(
                "SELECT " + FIELD_ID + "," + FIELD_TABLESERIAL + " FROM " + getTableName() + " WHERE " +
                FIELD_TABLESERIAL + ">? ORDER BY " + FIELD_TABLESERIAL));
      }
      PreparedStatementWrapper st = db.getPreparedStatement(getSelectExpiredTableSerials1StatementId());
      st.setLong(1, oldSerial);
      ResultSetWrapper rs = st.executeQuery();
      LongArray expireList = new LongArray();
      while (rs.next()) {
        expireList.add(rs.getLong(1));
        expireList.add(rs.getLong(2));
      }
      rs.close();
      return expireList.values();
    }
  }
          
  
  /**
   * Determines the objects with their tableSerial within a given range.
   * Useful to cleanup caches.
   *
   * @param oldSerial non-inclusive lower bound for tableSerial (&gt; oldSerial)
   * @param maxSerial inclusive upper bound for tableSerial (&le; maxSerial)
   * @return pairs of longs, the first being the ID, the second the tableserial, never null
   */
  public long[] selectExpiredTableSerials(long oldSerial, long maxSerial) {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().selectExpiredTableSerials(oldSerial, maxSerial);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByExpiredTableSerial failed");
        return null;
      }      
    }
    else  {
      if (getSelectExpiredTableSerials2StatementId() == 0)  {
        setSelectExpiredTableSerials2StatementId(db.prepareStatement(
                "SELECT " + FIELD_ID + "," + FIELD_TABLESERIAL + " FROM " + getTableName() + " WHERE " +
                FIELD_TABLESERIAL + ">? AND " + FIELD_TABLESERIAL + "<=? ORDER BY " + FIELD_TABLESERIAL));
      }
      PreparedStatementWrapper st = db.getPreparedStatement(getSelectExpiredTableSerials2StatementId());
      st.setLong(1, oldSerial);
      st.setLong(2, maxSerial);
      ResultSetWrapper rs = st.executeQuery();
      LongArray expireList = new LongArray();
      while (rs.next()) {
        expireList.add(rs.getLong(1));
        expireList.add(rs.getLong(2));
      }
      rs.close();
      long[] exp = expireList.values();
      if (DbGlobal.isServer()) {
        getDbObjectClassVariables().expirationBacklog.addExpiration(oldSerial, maxSerial, exp);
        if (DbGlobal.logger.isFineLoggable()) {
          DbGlobal.logger.fine("added expiration set " + oldSerial + "-" + maxSerial + "[" + exp.length + "] for " + getTableName());
        }
      }
      return exp;
    }
  }
          
  
  /**
   * Gets the expiration backlog for a given range of tableserials.
   * Note that the backlog is maintained only if DbGlobal.serverDb != null.
   * 
   * @param minSerial the lower serial bound of the query (minSerial &lt; tableSerial)
   * @param maxSerial the upper serial bound of the query (tableSerial &le; maxSerial)
   * @return the expiration info as pairs of id/tableserial, null if given range was not found in the backlog
   */
  public long[] getExpirationBacklog(long minSerial, long maxSerial) {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().getExpirationBacklog(minSerial, maxSerial);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote getExpirationBacklog failed");
        return null;
      }      
    }
    else  {
      return getDbObjectClassVariables().expirationBacklog.getExpiration(minSerial, maxSerial);
    }
  }
  
  
  /**
   * Combines {@link #selectExpiredTableSerials} and {@link #getExpirationBacklog}.<br>
   * A physical database query is only done if the requested range is not in the backlog.
   * Used in RMIservers to reduces db-roundtrips.
   * 
   * @param oldSerial non-inclusive lower bound for tableSerial (&gt; oldSerial)
   * @param maxSerial inclusive upper bound for tableSerial (&le; maxSerial)
   * @return pairs of longs, the first being the ID, the second the tableserial, never null
   */
  public long[] getExpiredTableSerials(long oldSerial, long maxSerial) {
    if (getDb().isRemote())  {
      try {
        return getRemoteDelegate().getExpiredTableSerials(oldSerial, maxSerial);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote getExpiredTableSerials failed");
        return null;
      }      
    }
    else  {
      long[] exp = null;
      if (DbGlobal.isServer()) {
        // try to get from backlog
        exp = getDbObjectClassVariables().expirationBacklog.getExpiration(oldSerial, maxSerial);
      }
      if (exp == null) {
        // no info in backlog: physically select
        exp = selectExpiredTableSerials(oldSerial, maxSerial);
      }
      return exp;
    }  
  }
  
  
  /**
   * Creates a {@link ModificationLog}.
   *
   * @param modType is the modification type
   * @return the created ModificationLog ready for save()
   */
  public ModificationLog createModificationLog(char modType)  {
    return new ModificationLog(this, modType);
  }
  
  
  /**
   * Logs a modification for this object to the modlog 
   * (not to be mixed up with the modification-counter!)
   *
   * @param modType is the modification type (INSERT, DELETE or UPDATE)
   * @return true if logging ok, false if writing the log failed
   */
  public boolean logModification(char modType)  {
    getDb().assertNotRemote();   // don't log locally if db is remote
    if (getDb().isLogModificationAllowed()) {
      getDb().logBeginTx();      // optionally create the begin log
      return createModificationLog(modType).save();
    }
    return true;
  }
  
  
  /**
   * Gets the delegate for remote connections.<br>
   * Each class has its own delegate.
   * 
   * @return the delegate for this object
   */
  public DbObjectRemoteDelegate getRemoteDelegate()  {
    return getDbObjectClassVariables().getRemoteDelegate(getDb());
  }
  
  
  
  /**
   * Gets the number of columns (fields) in the table corresponding to the
   * class of this object.<br>
   * The method does a dummy select if not known so far.
   *
   * @return the number of columns
   */
  public int getFieldCount()  {
    getDb().assertNotRemote();
    int count = getDbObjectClassVariables().fieldCount;
    if (count <= 0) {
      gettingFieldCount = true;
      // perform a dummy select (this happens only once, hence no prepared statement)
      ResultSetWrapper rs = getDb().createStatement().executeQuery("SELECT * FROM " + getTableName() + " WHERE 1=0");
      getFields(rs);   // this will set the fieldCount
      rs.close();
      if ((count = getDbObjectClassVariables().fieldCount) <= 0) {
        // still not set: invoke errorhandler
        DbGlobal.errorHandler.severe(getDb(), null, "can not determine fieldCount for " + getTableName());
      }
      gettingFieldCount = false;
    }
    return count;
  }

  

  private boolean gettingFieldCount;
  
  /**
   * As {@link #getFieldCount} is invoked from {@link #getFields} this prevents 
   * a heap overflow.
   * @return true if in getFieldCount
   */
  public boolean isGettingFieldCount() {
    return gettingFieldCount;
  }
  
  
  /**
   * Updates the fieldCount in the class variables
   * if the given columnIndex is greater than current fieldcount.
   *
   * @param columnIndex is the result-set column index (>=1)
   */
  protected void updateFieldCount(int columnIndex)  {
    DbObjectClassVariables dbvar = getDbObjectClassVariables();
    if (dbvar.fieldCount < columnIndex) {
      dbvar.fieldCount = columnIndex;
    }
  }
  
  
  /**
   * Determines whether prepared statements of this class should always
   * be prepared each time when the statement used.
   * @return true if always prepare
   */
  public boolean alwaysPrepare() {
    return getDbObjectClassVariables().alwaysPrepare;
  }
  
  /**
   * Sets the always prepare flag.
   * 
   * @param alwaysPrepare true if always prepare
   */
  public void setAlwaysPrepare(boolean alwaysPrepare) {
    getDbObjectClassVariables().alwaysPrepare = alwaysPrepare;
  }
  
  
  /**
   * Gets the prepared statement id for {@link #select}.
   * @return the statement id
   */
  protected int getSelectStatementId() {
    return getDbObjectClassVariables().selectStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #select}.
   * @param id the statement id
   */
  protected void setSelectStatementId (int id)  {
    getDbObjectClassVariables().selectStatementId = id;
  }
  
  
  /**
   * Gets the prepared statement id for {@link #selectAll}.
   * @return the statement id
   */
  protected int getSelectAllStatementId() {
    return getDbObjectClassVariables().selectAllStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectAll}.
   * @param id the statement id
   */
  protected void setSelectAllStatementId (int id) {
    getDbObjectClassVariables().selectAllStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #selectLocked}.
   * @return the statement id
   */
  protected int getSelectLockedStatementId() {
    return getDbObjectClassVariables().selectLockedStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectLocked}.
   * @param id the statement id
   */
  protected void setSelectLockedStatementId (int id) {
    getDbObjectClassVariables().selectLockedStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #selectSerial}.
   * @return the statement id
   */
  protected int getSelectSerialStatementId() {
    return getDbObjectClassVariables().selectSerialStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectSerial}.
   * @param id the statement id
   */
  protected void setSelectSerialStatementId (int id) {
    getDbObjectClassVariables().selectSerialStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #insert}.
   * @return the statement id
   */
  protected int getInsertStatementId() {
    return getDbObjectClassVariables().insertStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #insert}.
   * @param id the statement id
   */
  protected void setInsertStatementId (int id) {
    getDbObjectClassVariables().insertStatementId = id;
  }
    
  /**
   * Gets the prepared statement id for {@link #update}.
   * @return the statement id
   */
  protected int getUpdateStatementId() {
    return getDbObjectClassVariables().updateStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #update}.
   * @param id the statement id
   */
  protected void setUpdateStatementId (int id) {
    getDbObjectClassVariables().updateStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #delete}.
   * @return the statement id
   */
  protected int getDeleteStatementId() {
    return getDbObjectClassVariables().deleteStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #delete}.
   * @param id the statement id
   */
  protected void setDeleteStatementId (int id) {
    getDbObjectClassVariables().deleteStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #dummyUpdate}.
   * @return the statement id
   */
  protected int getDummyUpdateStatementId() {
    return getDbObjectClassVariables().dummyUpdateStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #dummyUpdate}.
   * @param id the statement id
   */
  protected void setDummyUpdateStatementId (int id) {
    getDbObjectClassVariables().dummyUpdateStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #updateSerial}.
   * @return the statement id
   */
  protected int getUpdateSerialStatementId() {
    return getDbObjectClassVariables().updateSerialStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #updateSerial}.
   * @param id the statement id
   */
  protected void setUpdateSerialStatementId (int id) {
    getDbObjectClassVariables().updateSerialStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #updateTableSerial}.
   * @return the statement id
   */
  protected int getUpdateTableSerialStatementId() {
    return getDbObjectClassVariables().updateTableSerialStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #updateTableSerial}.
   * @param id the statement id
   */
  protected void setUpdateTableSerialStatementId (int id) {
    getDbObjectClassVariables().updateTableSerialStatementId = id;
  }
  
  /**
   * Gets the prepared statement id for {@link #updateSerialAndTableSerial}.
   * @return the statement id
   */
  protected int getUpdateSerialAndTableSerialStatementId() {
    return getDbObjectClassVariables().updateSerialAndTableSerialStatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #updateSerialAndTableSerial}.
   * @param id the statement id
   */
  protected void setUpdateSerialAndTableSerialStatementId (int id) {
    getDbObjectClassVariables().updateSerialAndTableSerialStatementId = id;
  }

  /**
   * Gets the prepared statement id for {@link #selectExpiredTableSerials}.
   * @return the statement id
   */
  protected int getSelectExpiredTableSerials1StatementId() {
    return getDbObjectClassVariables().selectExpiredTableSerials1StatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectExpiredTableSerials}.
   * @param id the statement id
   */
  protected void setSelectExpiredTableSerials1StatementId (int id) {
    getDbObjectClassVariables().selectExpiredTableSerials1StatementId = id;
  }    

  /**
   * Gets the prepared statement id for {@link #selectExpiredTableSerials}.
   * @return the statement id
   */
  protected int getSelectExpiredTableSerials2StatementId() {
    return getDbObjectClassVariables().selectExpiredTableSerials2StatementId;
  }
  
  /**
   * Sets the prepared statement id for {@link #selectExpiredTableSerials}.
   * @param id the statement id
   */
  protected void setSelectExpiredTableSerials2StatementId (int id) {
    getDbObjectClassVariables().selectExpiredTableSerials2StatementId = id;
  }    

  
  /**
   * Gets the database table name for the class of this object.
   * @return the table name
   */
  public String getTableName () {
    return getDbObjectClassVariables().tableName;
  }

  /**
   * Gets the human readable name for one (1) object of this class.
   * @return the name
   */
  public String getSingleName () {
    return getDbObjectClassVariables().singleName;
  }

  /**
   * Gets the human readable name for multiple (&gt; 1) objects of this class.
   * @return the name
   */
  public String getMultiName () {
    return getDbObjectClassVariables().multiName;
  }

  
  
  /**
   * Retrieves the values of all fields (all columns of the database table)
   * in the given {@link ResultSetWrapper} and stores them in the object's attributes.
   * 
   * @param rs the result set
   * @return true if all fields retrieved
   */
  abstract public boolean getFields(ResultSetWrapper rs);

  
  /**
   * Prepares the object's attributes before the object is saved to the database.
   * Should be overridden. The default implementation does nothing.
   * Used to setup, check and align values.
   * 
   * @return true if proceed with save/insert/update,
   *         false if some error.
   */
  public boolean prepareSetFields()  {
    return true;
  }
  
  
  /**
   * Sets the values of all fields (all columns of the database table)
   * in the given {@link PreparedStatementWrapper} from the object's attributes.
   *
   * @param st the statement
   * @return the number of fields set, -1 if error.
   */
  abstract public int setFields (PreparedStatementWrapper st);

  
  /**
   * Updates the fields in the result-set.<br>
   * Because it is only used in updateable cursors and
   * by far most apps don't use that feature, the
   * method is not abstract and by default throws the exception for "not implemented".
   *
   * @param rs the result set
   * @return the number of fields set, -1 if error.
   */
  public int updateFields (ResultSetWrapper rs) {
    throw new DbRuntimeException("not implemented for " + getClassName());
  }

  

  /**
   * Gets the sql text to select all fields (and all objects).
   * 
   * @return the sql text
   */
  public String getSqlAllFields() {
    return getSqlPrefixWithDot() + "* FROM " + getTableName() + Db.WHEREALL_CLAUSE;
  }
  
  /**
   * Get the SQL-string to select all columns of this class (may be across multiple tables)
   *
   * @return the sql-string for "SELECT * FROM ..."
   */
  public String getSqlSelectAllFields() {
    return "SELECT " + getSqlAllFields();
  }

  
  
  /**
   * Prepares the insert statement.
   * @return the statememt id
   */
  abstract public int prepareInsertStatement();

  /**
   * Prepare the update statement.
   * @return the statememt id
   */
  abstract public int prepareUpdateStatement();


  /**
   * Prepares the select statement (usually the same for all objects)
   * @return the statememt id
   */
  public int prepareSelectStatement() {
    int stmtId = getSelectStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(getSqlSelectAllFields() + " AND " + getSqlPrefixWithDot() + FIELD_ID + "=?");
      setSelectStatementId(stmtId);
    }
    return stmtId;
  }
  
  /**
   * Prepares the select statement with lock (usually the same for all objects)
   * @return the statememt id
   */
  public int prepareSelectLockedStatement() {
    int stmtId = getSelectLockedStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(getSqlSelectAllFields() + " AND " + getSqlPrefixWithDot() + FIELD_ID + "=? FOR UPDATE");
      setSelectLockedStatementId(stmtId);
    }
    return stmtId;
  }
  
  /**
   * Prepares the selectSerial statement (usually the same for all objects)
   * @return the statememt id
   */
  public int prepareSelectSerialStatement() {
    int stmtId = getSelectSerialStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement("SELECT " + FIELD_SERIAL + " FROM " + getTableName() + " WHERE " + FIELD_ID + "=?");
      setSelectSerialStatementId(stmtId);
    }
    return stmtId;
  }

  /**
   * Prepares the delete statement (usually the same for all objects)
   * @return the statememt id
   */
  public int prepareDeleteStatement() {
    int stmtId = getDeleteStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(
              "DELETE FROM " + getTableName() + " WHERE " + FIELD_ID + "=? AND " + FIELD_SERIAL + "=?");
      setDeleteStatementId(stmtId);
    }
    return stmtId;
  }
  
  
  /**
   * Prepares the dummy update.
   * Useful get an exclusive lock within a transaction
   * @return the statememt id
   */
  public int prepareDummyUpdateStatement() {
    int stmtId = getDummyUpdateStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " + FIELD_ID + "=? WHERE " + FIELD_ID + "=?");
      setDummyUpdateStatementId(stmtId);
    }
    return stmtId;
  }
  
  
  /**
   * Prepares the serial update.
   * @return the statememt id
   */
  public int prepareUpdateSerialStatement() {
    int stmtId = getUpdateSerialStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " + FIELD_SERIAL + "=" + FIELD_SERIAL + "+1 WHERE " + 
              FIELD_ID + "=? AND " + FIELD_SERIAL + "=?");
      setUpdateSerialStatementId(stmtId);
    }
    return stmtId;
  }
  
  
  /**
   * Prepares the tableSerial update.
   * @return the statememt id
   */
  public int prepareUpdateTableSerialStatement() {
    int stmtId = getUpdateTableSerialStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " + FIELD_TABLESERIAL + "=(SELECT " + 
              FIELD_SERIAL + " FROM " + ModificationCounter.TABLENAME + " WHERE " +
              ModificationCounter.FIELD_TABLENAME + "=?) WHERE " + FIELD_ID + "=?"); 
      setUpdateTableSerialStatementId(stmtId);
    }
    return stmtId;
  }
  
  
  /**
   * Prepares the serial + tableSerial update.
   * @return the statememt id
   */
  public int prepareUpdateSerialAndTableSerialStatement() {
    int stmtId = getUpdateSerialAndTableSerialStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      stmtId = getDb().prepareStatement(
              "UPDATE " + getTableName() + " SET " + FIELD_SERIAL + "=" + FIELD_SERIAL + "+1, " +
              FIELD_TABLESERIAL + "=? WHERE " + 
              FIELD_ID + "=? AND " + FIELD_SERIAL + "=?");
      setUpdateSerialAndTableSerialStatementId(stmtId);
    }
    return stmtId;
  }
  
  

  
  
  /**
   * Instantiates a new object for a given class (without db-context).
   * @param <T> the class type
   * @param clazz the class
   * @return the object
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  static public <T extends DbObject> T newByClass (Class<T> clazz) 
         throws InstantiationException, IllegalAccessException {
    // load the class
    return clazz.newInstance();
  }
  
  
  /**
   * Instantiates a new object for a given class transforming exceptions to
   * DbRuntimeException. Commonly used in apps to simplify code.
   * 
   * @param <T> the class type
   * @param clazz the class
   * @return the object
   */
  static public <T extends DbObject> T newByClassWrapped (Class<T> clazz) {
    try {
      return newByClass(clazz);
    }
    catch (Exception ex) {
      throw new DbRuntimeException("creating object for " + clazz + " failed", ex);
    }
  }  
  
  
  
  /**
   * Instantiates a new object for a given class and db.
   * 
   * @param <T> the class type
   * @param db the db connection
   * @param clazz the class
   * @return the object
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  static public <T extends DbObject> T newByClass (Db db, Class<T> clazz) 
         throws InstantiationException, IllegalAccessException {
    // load the class
    T obj = newByClass(clazz);
    obj.setDb(db);
    return obj;
  }
  
  
  
  
  
  /**
   * Selects an object for a given class and db by its unique id.
   *
   * @param <T> the class type
   * @param db the db connection
   * @param clazz the class
   * @param id the object ID
   * @return the object or null if no such object.
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  public static <T extends DbObject> T select(Db db, Class<T> clazz, long id) 
         throws InstantiationException, IllegalAccessException {
    return (T)newByClass(db, clazz).selectInValidContext(id, true);   // unchecked
  }
  
  
  /**
   * Selects an object for a given class name and db by its unique id.
   *
   * @param <T> the class type
   * @param db the db connection
   * @param className the class name
   * @param id the object ID
   * @return the object or null if no such object.
   * @throws ClassNotFoundException 
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  public static <T extends DbObject> T select(Db db, String className,long id) 
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    return select(db, (Class<T>)Class.forName(className), id);
  }  
  
  
  
  /**
   * Selects all objects of this class.
   * 
   * @param <T> the database object class
   * @param db the logical db connection
   * @param clazz the database object class
   * @return the list of objects
   * @throws InstantiationException
   * @throws IllegalAccessException 
   */
  @SuppressWarnings("unchecked")
  public static <T extends DbObject> List<T> selectAll(Db db, Class<T> clazz)
         throws InstantiationException, IllegalAccessException {
    return (List<T>)newByClass(db, clazz).selectAll();
  }  
  
  

  /**
   * Deletes a List of objects.
   * Virgin objects are not deleted.
   *
   * @param list the list of object to delete
   * @return the number of objects deleted, -1 if error  (some object wasn't deleted)
   */
  public static int deleteList (List<? extends DbObject> list) {
    int deleted = 0;
    if (list != null && !list.isEmpty()) {
      Db db = list.get(0).getDb();
      boolean oldCommit = db.begin(TX_DELETE_LIST);
      for (DbObject obj: list)  {
        if (obj != null && !obj.isVirgin()) {
          if (obj.delete()) {
            deleted++;
          }
          else {
            db.rollback(oldCommit);
            return -1;    // error!
          }
        }
      }
      db.commit(oldCommit);
      if (list instanceof TrackedArrayList) {
        ((TrackedArrayList)list).setModified(false);
      }
    }
    return deleted;
  }
  
  
  /**
   * Marks all objects in a list to be deleted.<br>
   * Use this method if a list-SQL-delete removed a set of objects and
   * you have to reflect that in the list of objects.
   * Bulk-deletes with succeeding list-iteration for marking the objects
   * deleted usually is faster that invoking deleteList().
   *
   * @param list the objects to delete
   * @return the number of marked objects (with id > 0).
   */
  public static int markListDeleted(List<? extends DbObject> list)  {
    int marked = 0;
    if (list != null) {
      for (DbObject obj: list)  {
        if (obj != null && !obj.isNew()) {
          obj.markDeleted();
          marked++;
        }
      }
    }
    return marked;    
  }
  
  
  /**
   * Deletes all objects in oldList that are not in newList.
   * The method is handy for deleting cascaded composite lists.
   *
   * @param oldList the list of objects stored in db
   * @param newList the new list of objects
   *
   * @return the number of objects deleted, -1 if some error
   */
  public static int deleteMissingInList(List<? extends DbObject> oldList, List<? extends DbObject> newList) {
    int deleted = 0;
    if (oldList != null && !oldList.isEmpty()) {
      Db db = oldList.get(0).getDb();
      boolean oldCommit = db.begin(TX_DELETE_MISSING_IN_LIST);
      for (DbObject obj: oldList) {
        if (obj != null && !obj.isVirgin() && (newList == null || !newList.contains(obj))) {
          if (obj.delete()) { // delete() will cascade too, if necessary!
            deleted++;
          }
          else  {
            db.rollback(oldCommit);
            return -1;
          }
        }
      }
      db.commit(oldCommit);
    }
    return deleted;
  }
  
  
  
  /**
   * Checks whether some of the objects in the list are modified.
   * Useful for recursive optimizations.
   * 
   * @param list the objects
   * @return true if modified
   */
  public static boolean isListModified(List<? extends DbObject> list) {
    // TrackedArrayLists are modified if elements added, replaced or removed
    if (list instanceof TrackedArrayList &&
        ((TrackedArrayList)list).isModified())  {
      return true;
    }
    // check attributes
    if (list != null) {
      for (DbObject obj: list)  {
        if (obj != null && obj.isModified()) {
          return true;
        }
      }
    }    
    return false;
  }
  
  
  
  /**
   * Saves a list of DbObjects.<br>
   * All objects with isSaveable() == true will be saved.
   * If modifiedOnly is true only isModified() objects will be saved.
   * All objects with isSaveable() == false and isNew() == false
   * are removed!
   * By definition, a {@link TrackedArrayList} must *NOT* contain untracked objects.
   * The errorhandler will be invoked if such an object
   * is detected. This is a quality measure to ensure code consistency.
   * The wurblet DbRelations automatically takes care of that.
   *
   * @param list the list to save
   * @param modifiedOnly is true if only modified objects are saved
   *
   * @return the number of objects saved, -1 if some error
   */
  public static int saveList (List<? extends DbObject> list, boolean modifiedOnly) {
    int saved = 0;
    if (list != null && list.size() > 0)  {
      Db db = list.get(0).getDb();
      boolean oldcommit = db.begin(TX_SAVE_LIST);
      for (DbObject obj: list)  {
        if (obj != null) {
          if (obj.isSaveable()) {
            if (!modifiedOnly || obj.isModified())  {
              if (obj.save()) {
                saved++;
              }
              else {
                db.rollback(oldcommit);
                return -1;
              }
            }
          }
          else {
            if (!obj.isNew()) {
              // already stored on disk: remove it!
              if (obj.delete() == false) {
                db.rollback(oldcommit);
                return -1;
              }
            }
          }
        }
      }
      db.commit(oldcommit);
      
      if (list instanceof TrackedArrayList) {
        ((TrackedArrayList)list).setModified(false);
      }
    }
    return saved;
  }
  
  
  /**
   * Saves a list of objects.<br>
   * The linked objects are saved as well.
   * All objects with isSaveable() == true will be saved.
   * If modifiedOnly is true only isModified() objects will be saved.
   * All objects with isSaveable() == false and isNew() == false
   * are removed!
   * By definition, a {@link TrackedArrayList} must *NOT* contain untracked objects.
   * The errorhandler will be invoked if such an object
   * is detected. This is a quality measure to ensure code consistency.
   * The wurblet DbRelations automatically takes care of that.
   *
   * @param list the list to save
   *
   * @return the number of objects saved, -1 if some error
   */
  public static int saveList (List<? extends DbObject> list) {  
    return saveList(list, false);
  }
  


  
  
  /**
   * Compares the IDs.
   * 
   * @param <T> the database object class
   */
  public static class IdComparator<T extends DbObject> implements Comparator<T> {
    public int compare (T o1, T o2) {
      // getId() cause of "reserved Ids"
      return Compare.compareLong(o1.getId(), o2.getId());
    }
  }
  
  
  /**
   * Compares the string representation (toString()).
   * 
   * @param <T> the database object class
   */
  public static class NameComparator<T extends DbObject> implements Comparator<T> {
    public int compare (T o1, T o2) {
      return o1.toString().compareToIgnoreCase(o2.toString());
    }
  }
  
  
  /**
   * Compares the names + IDs.
   * 
   * @param <T> the database object class
   */
  public static class NameIdComparator<T extends DbObject> implements Comparator<T> {
    public int compare (T o1, T o2) {
      int rv = o1.toString().compareToIgnoreCase(o2.toString());
      if (rv == 0)  {
        // getId() cause of "reserved Ids"
        rv = Compare.compareLong(o1.getId(), o2.getId());
      }
      return rv;
    }
  }
  
}
