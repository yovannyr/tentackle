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

// $Id: Security.java 477 2009-08-09 18:54:26Z harald $

package org.tentackle.appworx;

import java.util.ArrayList;
import java.util.List;
import org.tentackle.appworx.rmi.SecurityRemoteDelegate;
import org.tentackle.db.Binary;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.DbRuntimeException;
import org.tentackle.db.PreparedStatementWrapper;
import org.tentackle.db.ResultSetWrapper;
import org.tentackle.ui.FormTableEntry;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.Compare;
import org.tentackle.util.StringHelper;
import org.tentackle.util.TrackedArrayList;




// @> $mapfile
// # Security ACLs
// #
// # CREATE INDEX security_context on security (contextid, contextclass);
// # CREATE INDEX security_grant on security (grantid, grantclass);
// # CREATE UNIQUE INDEX security_constraints on security (objectid,objectclass,grantid,grantclass,contextid,contextclass,priority);
// 
// String   192	objectClass	objectclass	classbasename if database class, else full classname
// long     0	objectId	objectid	object id, 0=all or not a database class
// long     0	contextId	contextid	ID of context object, 0 = all of contextClass
// String   64	contextClass	contextclass	classbasename of ContextDb's Rootobject
// long     0	grantId		grantid		ID of grantobject, 0 = all of grantClass
// String   64	grantClass	grantclass	classbasename of grantobject
// int      0	priority	priority	ordering, 0=first
// int      0	permissionType	permtype	set of possible Permissions
// int      0	permission	permission	permissionbits according to permtype
// boolean  0	allowed		allowed         false=denied, true=allowed
// String   0	message		message		user message
// Binary   0	extendedRule	extrule		extension object
// @<


/**
 * A security rule.
 * <p>
 * Any object, whether it is a database object ({@link AppDbObject}) or any
 * other class (for example a dialog) can be secured by rules.
 * Security rules grant or deny a permission to a grantee object or a grantee class. 
 * Optionally, rules can be restricted to some context object or context class.
 * Grantees and contexts must be of type {@link AppDbObject}.
 * The permission is an integer associated to a permission type, which is
 * also an integer identifying the permission scheme. Permission types
 * can be application specific, for example "invoiceable".
 * Security rules also have a priority, can explain the denial to the user
 * by means of a message text and can easily be extended by the application.
 * Furthermore, rules may optionally be restricted to an application database context.
 *
 * @author harald
 */
public class Security extends AppDbObject {

  private static final long serialVersionUID = -1178387730540795339L;
  
  
  // predefined permission types to be extended by subclasses:
  
  /** permissiontype "data", i.e. AppDbObject **/
  public static final int TYPE_DATA    = 0;
  /** java executable class, i.e. program logic **/
  public static final int TYPE_PROGRAM = 1;
  
  
  // predefined permissions
  // to be extended by derived classes.
  // Apps should start at 0x0100 cause bitmask might be extended...
  
  /** no permission at all **/
  public static final int NONE    = 0x0000;
  /** allow for read **/
  public static final int READ    = 0x0001;
  /** allow for write, create, delete **/
  public static final int WRITE   = 0x0002;
  /** allow to execute **/
  public static final int EXEC    = 0x0004;
  
  
  
  
  /**
   * Converts a permission and its type to a string.
   * 
   * @param type the permission type
   * @param permission the permission
   * @return the text representation
   */
  public static String permissionToString(int type, int permission) {
    if (type == TYPE_DATA)  {
      if (permission == READ) {
        return Locales.bundle.getString("read");
      }
      else if (permission == WRITE) {
        return Locales.bundle.getString("write");
      }
      else if (permission == (READ | WRITE))  {
        return Locales.bundle.getString("all");
      }      
    }
    else if (type == TYPE_PROGRAM)  {
      if ((permission & EXEC) != 0) {
        return Locales.bundle.getString("execute");
      }
    }
    return Locales.bundle.getString("nothing");    // "no permission" (does not mean "denied"!)
  }
  
  
  /**
   * Creates security rules for objects created by {@link AppDbObject#createCopyInContextDb}.<br>
   * The method copies the rules from one object to another (logically the same)
   * object in another context.
   * 
   * @param fromObject 
   * @param toObject
   * @return the created list of rules
   * @throws ApplicationException if security instances could not be created
   */
  public static List<Security> createSecurityForObjectInOtherDb(AppDbObject fromObject, AppDbObject toObject) 
                throws ApplicationException {
    // create Security rules in the new context
    try {
      Security s = toObject.getSecurityManager().newSecurityInstance();
      List<Security> list = s.selectByObject(fromObject);
      for (Security sec: list)  {
        sec.setContextDb(toObject.getContextDb());  // switch contextDb in case toObject shares secManager with fromObject.
        sec.setObjectId(toObject.getId());          // switch ID
        sec.setId(0);
        sec.setSerial(0);
      }
      return list;
    }
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(fromObject.getDb(), ex, null);
      return null;  // not reached
    }
  }


  
  

  /** database tablename **/
  public static final String TABLENAME = "security";

  private static AppDbObjectClassVariables classVariables = 
    new AppDbObjectClassVariables(Security.class, TABLENAME, "Security Entry", "Security Entries");

  
  
  
  
  /**
   * Creates a security rule.
   * 
   * @param db the database context
   */
  public Security (ContextDb db)    {
    super(db);
  }
  
  /**
   * Creates a security rule (without db context).
   */
  public Security() {
    super();
  }
  
  
  

  
  /**
   * Evaluates a rule.
   * <p>
   * The method is only invoked for rules that apply to the object and/or class in question.
   * Notice further, that due to servermode the given {@link ContextDb} may point to a different
   * {@link org.tentackle.db.Db} than this security rule. For example, if some extendedRule must load data
   * from the db, it must handle that appropriately.
   * 
   * @param contextDb  the context-Db the object or class is used in, null = all
   * @param permission the requested permission(s)
   * 
   * @return true if rule fires, else this rules does not apply
   */
  public boolean evaluate(ContextDb contextDb, int permission) {
    return // check that the context applies
           (contextDb == null || contextDb.isWithinContext(getContextId(), getContextClass())) &&
           // check that permission applies
           (allowed ? 
              // allow if _all_ requested permissions are granted
              (permission & getPermission()) == permission :
              // deny if _any_ of the requested permissions are denied
              (permission & getPermission()) != 0);
  }
  
  
  
  /**
   * Gets the object this rule applies to.<br>
   * The object is retrieved in its valid context.
   * @return the object, null if none (class rule)
   */
  public AppDbObject getObject() {
    if (objectId != 0 && objectClass != null)  {
      try {
        return AppDbObject.selectInValidContext(getDb(), objectClass, objectId);
      }
      catch (Exception e) {
        DbGlobal.logger.warning("can't get secured object for " + objectClass + ", " + objectId);
      }
    }
    return null;
  }
  
  
  /**
   * Sets the object this rule applies to.<br>
   * Setting the null object also clears the objectClass
   * making the rule invalid.
   * 
   * @param object the object, null to clear
   */
  public void setObject(AppDbObject object)  {
    if (object == null) {
      setObjectId(0);
      setObjectClass((String)null);
    }
    else  {
      setObjectId(object.getId());
      setObjectClass(object.getClassBaseName());
    }
  }
  
  
  /**
   * Sets the class to be secured.<br>
   * If the class is an {@link AppDbObject} only the basename
   * will be stored. Otherwise the full qualified classname
   * is used.
   * 
   * @param clazz the objectclass
   */
  public void setObjectClass(Class clazz) {
    setObjectClass(AppDbObject.class.isAssignableFrom(clazz) ?
                      StringHelper.getClassBaseName(clazz) : clazz.getName());
  }
  
  

  /**
   * Gets the grantee.
   * 
   * @return the grantee, null if grant class
   */
  public AppDbObject getGrantee()  {
    try {
      return grantId == 0 ? null : 
             AppDbObject.selectCached(getContextDb(), grantClass, grantId);
    }
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(getDb(), ex, null);
      return null;
    }
  }
  
  
  /**
   * Sets the grantee.
   * 
   * @param grantee the grantee, null to clear (all)
   */
  public void setGrantee(AppDbObject grantee)  {
    if (grantee == null)  {
      setGrantId(0);
      setGrantClass(null);
    }
    else  {
      setGrantId(grantee.getId());
      setGrantClass(grantee.getClassBaseName());
    }
  }
  
  

  /**
   * Gets the application context's object.<br>
   * The object will be loaded in its valid context.
   * 
   * @return the context object, null if no context (all)
   */
  public AppDbObject getContextObject()  {
    if (contextId != 0) {
      try {
        return AppDbObject.selectInValidContext(getDb(), contextClass, contextId);
      }
      catch (Exception ex)  {
        DbGlobal.errorHandler.severe(getDb(), ex, null);
      }
    }
    return null;
  }
  
  
  /**
   * Sets the application context's object.
   * 
   * @param contextObject the context object, null to clear (all) 
   */
  public void setContextObject(AppDbObject contextObject)  {
    if (contextObject == null)  {
      setContextId(0);
      setContextClass(null);
    }
    else {
      setContextId(contextObject.getId());
      setContextClass(contextObject.getClassBaseName());
    }
  }



  
  @Override
  public FormTableEntry getFormTableEntry() {
    return new SecurityTableEntry(this);
  }

  @Override
  public String getFormTableName()  {
    return "securityTable";
  }



  

  /**
   * Selects a rule by ID.
   */
  @Override
  public Security select (long id)  {
    return (Security)super.select(id);
  }

  
  /**
   * for debugging only
   */
  @Override
  public String toString()  {
    return "Security ID=" + getId() + ", objectClass=" + objectClass + ", objectId=" + objectId + ", contextId=" + contextId +
           ", contextClass=" + contextClass + ", grantId=" + grantId + ", grantClass=" + grantClass +
           ", priority=" + priority + ", permissionType=" + permissionType + ", permission=" + permission + 
           ", allowed=" + allowed + ", message=" + message + ", extendedRule=" + extendedRule;
  }

  
  /**
   * All selects are sorted by priority!
   */
  @Override
  public String orderBy() {
    return FIELD_PRIORITY;
  }    
  
  
  @Override
  public AppDbObjectClassVariables getAppDbObjectClassVariables() {
    return classVariables;
  }  
  

  /** 
   * Changes in Security usually force caches, etc... to be reloaded.
   * 
   * @return always true
   */
  @Override
  public boolean isCountingModification(int modType) {
    return true;
  }
  

  
  
  /**
   * Makes sure that the remote security manager is up to date.
   * The method is necessary to avoid situations where the SecurityManager.initialize
   * at the client side will need some data that in turn is security-checked and
   * that would trigger the remote SecurityManager to be initialized as well.
   * This could lead to a statement being marked ready twice in the server
   * (client executes that statement remote, readFromResultSetWrapper in the server
   * checks the security and starts the initialize in the server, which in
   * turn uses the same statement -> catch22...)
   * This method is package scope!
   */
  void assertRemoteSecurityManagerInitialized() {
    try {
      ((SecurityRemoteDelegate)getRemoteDelegate()).assertRemoteSecurityManagerInitialized(getContextDb());
    }
    catch (Exception e) {
      DbGlobal.errorHandler.severe(getDb(), e, "remote assertRemoteSecurityManagerInitialized failed");
    }
  }
  

  /**
   * Selects all rules for a given grantId.<br>
   * Notice that among all objects of grantee-classes 
   * (usually one for the user and one for the roles) the IDs must be unique!
   *
   * @param grantId the grantee's object ID
   * @return the rules
   * 
   * @wurblet selectByGrantId AppDbSelectList $mapfile $remote --sort grantId
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectByGrantId

  public List<Security> selectByGrantId(long grantId) {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        List<Security> list = ((SecurityRemoteDelegate)getRemoteDelegate()).selectByGrantId(getContextDb(), grantId);
        ContextDb.applyToCollection(getContextDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByGrantId failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectByGrantIdStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      String sql = "SELECT ";
      sql += getSqlAllFields() 
                    + " AND " + FIELD_GRANTID + "=?";
      String orderSuffix = orderBy();
      sql += " ORDER BY " + (orderSuffix == null ? (getSqlPrefixWithDot() + FIELD_ID) : orderSuffix);
      stmtId = getDb().prepareStatement(sql);
      selectByGrantIdStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setLong(ndx++, grantId);
    ResultSetWrapper rs = st.executeQuery();
    List<Security> list = new ArrayList<Security>();
    boolean derived = getClass() != Security.class;
    while (rs.next()) {
      Security obj = derived ? (Security)newObject() : new Security(getContextDb());
      if (obj.readFromResultSetWrapper(rs))  {
        list.add(obj);
      }
    }
    rs.close();
    return list;
  }

  private static int selectByGrantIdStatementId;


  // End of wurblet generated code.//GEN-END:selectByGrantId




  /**
   * Select all rules for given objectId and objectClass.
   *
   * @param objectId the object's ID
   * @param objectClass the object's class
   * @return the rules
   * 
   * @wurblet selectByObject AppDbSelectList $mapfile $remote --sort --tracked objectId objectClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:selectByObject

  public TrackedArrayList<Security> selectByObject(long objectId, String objectClass) {
    if (getDb().isRemote())  {
      // invoke remote method
      try {
        TrackedArrayList<Security> list = ((SecurityRemoteDelegate)getRemoteDelegate()).selectByObject(getContextDb(), objectId, objectClass);
        ContextDb.applyToCollection(getContextDb(), list);
        return list;
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote selectByObject failed");
        return null;
      }
    }
    // else: local mode
    int stmtId = selectByObjectStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      String sql = "SELECT ";
      sql += getSqlAllFields() 
                    + " AND " + FIELD_OBJECTID + "=?"
                    + " AND " + FIELD_OBJECTCLASS + "=?";
      String orderSuffix = orderBy();
      sql += " ORDER BY " + (orderSuffix == null ? (getSqlPrefixWithDot() + FIELD_ID) : orderSuffix);
      stmtId = getDb().prepareStatement(sql);
      selectByObjectStatementId = stmtId;
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    int ndx = 1;
    st.setLong(ndx++, objectId);
    st.setString(ndx++, objectClass);
    ResultSetWrapper rs = st.executeQuery();
    TrackedArrayList<Security> list = new TrackedArrayList<Security>();
    boolean derived = getClass() != Security.class;
    while (rs.next()) {
      Security obj = derived ? (Security)newObject() : new Security(getContextDb());
      if (obj.readFromResultSetWrapper(rs))  {
        list.add(obj);
      }
    }
    rs.close();
    list.setModified(false);
    return list;
  }

  private static int selectByObjectStatementId;


  // End of wurblet generated code.//GEN-END:selectByObject

  

  /**
   * Select all rules for given object.
   *
   * @param object the object
   * @return the rules
   */
  public TrackedArrayList<Security> selectByObject(AppDbObject object) {
    return selectByObject(object.getId(), object.getClassBaseName());
  }
  
  
  
  /**
   * Delete all rules for given grantee.<br>
   * Notice that among all objects of grantee-classes 
   * (usually one for the user and one for the roles) the IDs must be unique!
   *
   * @param grantId the grantee's object ID
   * @return the number of deleted rules
   * 
   * @wurblet deleteByGrantee AppDbDeleteBy $mapfile $remote grantId
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteByGrantee

  public int deleteByGrantee(long grantId) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteByGrantee(grantId);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteByGrantee failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteByGranteeStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_GRANTID + "=?");
      deleteByGranteeStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteByGrantee denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, grantId);
    return st.executeUpdate();
  }

  private static int deleteByGranteeStatementId;


  // End of wurblet generated code.//GEN-END:deleteByGrantee

  
  
  /**
   * Deletes all rules for given objectId and objectClass.
   *
   * @param objectId the object's ID
   * @param objectClass the object's class
   * @return the number of deleted rules
   * 
   * @wurblet deleteByObject AppDbDeleteBy $mapfile $remote objectId objectClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteByObject

  public int deleteByObject(long objectId, String objectClass) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteByObject(objectId, objectClass);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteByObject failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteByObjectStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_OBJECTID + "=?" + " AND " + FIELD_OBJECTCLASS + "=?");
      deleteByObjectStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteByObject denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, objectId);
    st.setString(2, objectClass);
    return st.executeUpdate();
  }

  private static int deleteByObjectStatementId;


  // End of wurblet generated code.//GEN-END:deleteByObject


  /**
   * Deletes all rules for given AppDbObject.
   *
   * @param object the database object
   * @return the number of deleted rules
   */
  public int deleteByObject(AppDbObject object) {
    return deleteByObject(object.getId(), object.getClassBaseName());
  }
 
  
  /**
   * Delete all object-related rules for given objectClass.<br>
   * This will NOT delete any class-based rules (with objectId = 0) !
   *
   * @param objectClass the object class
   * @return the number of deleted rules
   * 
   * @wurblet deleteObjectRulesByObjectClass AppDbDeleteBy $mapfile $remote objectId:!=:0 objectClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteObjectRulesByObjectClass

  public int deleteObjectRulesByObjectClass(String objectClass) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteObjectRulesByObjectClass(objectClass);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteObjectRulesByObjectClass failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteObjectRulesByObjectClassStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_OBJECTID + "!=?" + " AND " + FIELD_OBJECTCLASS + "=?");
      deleteObjectRulesByObjectClassStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteObjectRulesByObjectClass denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, 0);
    st.setString(2, objectClass);
    return st.executeUpdate();
  }

  private static int deleteObjectRulesByObjectClassStatementId;


  // End of wurblet generated code.//GEN-END:deleteObjectRulesByObjectClass



  
  /**
   * Delete all rules for given objectClass and Context.<br>
   * This will NOT delete any class-based rules (with objectId = 0) !
   *
   * @param objectClass the object class
   * @param contextId the ID of the context object
   * @param contextClass the class of the context object
   * @return the number of deleted rules
   * 
   * @wurblet deleteObjectRulesByObjectClassAndContext AppDbDeleteBy $mapfile $remote objectId:!=:0 objectClass contextId contextClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteObjectRulesByObjectClassAndContext

  public int deleteObjectRulesByObjectClassAndContext(String objectClass, long contextId, String contextClass) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteObjectRulesByObjectClassAndContext(objectClass, contextId, contextClass);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteObjectRulesByObjectClassAndContext failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteObjectRulesByObjectClassAndContextStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_OBJECTID + "!=?" + " AND " + FIELD_OBJECTCLASS + "=?" + " AND " + FIELD_CONTEXTID + "=?" + " AND " + FIELD_CONTEXTCLASS + "=?");
      deleteObjectRulesByObjectClassAndContextStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteObjectRulesByObjectClassAndContext denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, 0);
    st.setString(2, objectClass);
    st.setLong(3, contextId);
    st.setString(4, contextClass);
    return st.executeUpdate();
  }

  private static int deleteObjectRulesByObjectClassAndContextStatementId;


  // End of wurblet generated code.//GEN-END:deleteObjectRulesByObjectClassAndContext



  /**
   * Deletes all rules for given grantClass.<br>
   * This will NOT delete any class-based rules (with grantId = 0) !
   *
   * @param grantClass the grantee class
   * @return the number of deleted rules
   * 
   * @wurblet deleteObjectRulesByGrantClass AppDbDeleteBy $mapfile $remote grantId:!=:0 grantClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteObjectRulesByGrantClass

  public int deleteObjectRulesByGrantClass(String grantClass) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteObjectRulesByGrantClass(grantClass);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteObjectRulesByGrantClass failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteObjectRulesByGrantClassStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_GRANTID + "!=?" + " AND " + FIELD_GRANTCLASS + "=?");
      deleteObjectRulesByGrantClassStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteObjectRulesByGrantClass denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, 0);
    st.setString(2, grantClass);
    return st.executeUpdate();
  }

  private static int deleteObjectRulesByGrantClassStatementId;


  // End of wurblet generated code.//GEN-END:deleteObjectRulesByGrantClass




  
  /**
   * Deletes all rules for given grantClass and Context.
   * This will NOT delete any class-based rules (with grantId = 0) !
   *
   * @param grantClass the grantee class
   * @param contextId the ID of the context object
   * @param contextClass the class of the context object
   * @return the number of deleted rules
   * 
   * @wurblet deleteObjectRulesByGrantClassAndContext AppDbDeleteBy $mapfile $remote grantId:!=:0 grantClass contextId contextClass
   */
  // Code generated by wurblet. Do not edit!//GEN-BEGIN:deleteObjectRulesByGrantClassAndContext

  public int deleteObjectRulesByGrantClassAndContext(String grantClass, long contextId, String contextClass) {
    if (getDb().isRemote())  {
      try {
        return ((SecurityRemoteDelegate)getRemoteDelegate()).deleteObjectRulesByGrantClassAndContext(grantClass, contextId, contextClass);
      }
      catch (Exception e) {
        DbGlobal.errorHandler.severe(getDb(), e, "remote deleteObjectRulesByGrantClassAndContext failed");
        return 0;
      }
    }
    // else: local mode
    int stmtId = deleteObjectRulesByGrantClassAndContextStatementId;
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "DELETE FROM " + getTableName() + " WHERE " +
            FIELD_GRANTID + "!=?" + " AND " + FIELD_GRANTCLASS + "=?" + " AND " + FIELD_CONTEXTID + "=?" + " AND " + FIELD_CONTEXTCLASS + "=?");
      deleteObjectRulesByGrantClassAndContextStatementId = stmtId;
    }
    // check security rules at class level
    if (!getAppDbObjectClassVariables().isWriteAllowed(getContextDb())) {
      throw new DbRuntimeException("deleteObjectRulesByGrantClassAndContext denied by security rules");
    }
    PreparedStatementWrapper st = getDb().getPreparedStatement(stmtId);
    st.setLong(1, 0);
    st.setString(2, grantClass);
    st.setLong(3, contextId);
    st.setString(4, contextClass);
    return st.executeUpdate();
  }

  private static int deleteObjectRulesByGrantClassAndContextStatementId;


  // End of wurblet generated code.//GEN-END:deleteObjectRulesByGrantClassAndContext

  


  
  // @wurblet methods DbMethods --tracked $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:methods

  
  /**
   * Overridden cause of "--tracked".
   * @return true = setters check for modification
   */
  @Override
  public boolean isTracked() {
    return true;    // invoking isModified() is ok
  }

  /**
   * Gets the db attribute objectClass
   *
   * @return classbasename if database class, else full classname
   */
  public String getObjectClass()    {
    return objectClass;
  }

  /**
   * Sets the db attribute objectClass
   *
   * @param objectClass classbasename if database class, else full classname
   */
  public void setObjectClass(String objectClass) {
    if (!attributesModified()) {
      setModified(Compare.equals(this.objectClass, objectClass) == false);
    }
    this.objectClass = objectClass;
  }

  /**
   * Gets the db attribute objectId
   *
   * @return object id, 0=all or not a database class
   */
  public long getObjectId()    {
    return objectId;
  }

  /**
   * Sets the db attribute objectId
   *
   * @param objectId object id, 0=all or not a database class
   */
  public void setObjectId(long objectId) {
    if (!attributesModified()) {
      setModified(this.objectId != objectId);
    }
    this.objectId = objectId;
  }

  /**
   * Gets the db attribute contextId
   *
   * @return ID of context object, 0 = all of contextClass
   */
  public long getContextId()    {
    return contextId;
  }

  /**
   * Sets the db attribute contextId
   *
   * @param contextId ID of context object, 0 = all of contextClass
   */
  public void setContextId(long contextId) {
    if (!attributesModified()) {
      setModified(this.contextId != contextId);
    }
    this.contextId = contextId;
  }

  /**
   * Gets the db attribute contextClass
   *
   * @return classbasename of ContextDb's Rootobject
   */
  public String getContextClass()    {
    return contextClass;
  }

  /**
   * Sets the db attribute contextClass
   *
   * @param contextClass classbasename of ContextDb's Rootobject
   */
  public void setContextClass(String contextClass) {
    if (!attributesModified()) {
      setModified(Compare.equals(this.contextClass, contextClass) == false);
    }
    this.contextClass = contextClass;
  }

  /**
   * Gets the db attribute grantId
   *
   * @return ID of grantobject, 0 = all of grantClass
   */
  public long getGrantId()    {
    return grantId;
  }

  /**
   * Sets the db attribute grantId
   *
   * @param grantId ID of grantobject, 0 = all of grantClass
   */
  public void setGrantId(long grantId) {
    if (!attributesModified()) {
      setModified(this.grantId != grantId);
    }
    this.grantId = grantId;
  }

  /**
   * Gets the db attribute grantClass
   *
   * @return classbasename of grantobject
   */
  public String getGrantClass()    {
    return grantClass;
  }

  /**
   * Sets the db attribute grantClass
   *
   * @param grantClass classbasename of grantobject
   */
  public void setGrantClass(String grantClass) {
    if (!attributesModified()) {
      setModified(Compare.equals(this.grantClass, grantClass) == false);
    }
    this.grantClass = grantClass;
  }

  /**
   * Gets the db attribute priority
   *
   * @return ordering, 0=first
   */
  public int getPriority()    {
    return priority;
  }

  /**
   * Sets the db attribute priority
   *
   * @param priority ordering, 0=first
   */
  public void setPriority(int priority) {
    if (!attributesModified()) {
      setModified(this.priority != priority);
    }
    this.priority = priority;
  }

  /**
   * Gets the db attribute permissionType
   *
   * @return set of possible Permissions
   */
  public int getPermissionType()    {
    return permissionType;
  }

  /**
   * Sets the db attribute permissionType
   *
   * @param permissionType set of possible Permissions
   */
  public void setPermissionType(int permissionType) {
    if (!attributesModified()) {
      setModified(this.permissionType != permissionType);
    }
    this.permissionType = permissionType;
  }

  /**
   * Gets the db attribute permission
   *
   * @return permissionbits according to permtype
   */
  public int getPermission()    {
    return permission;
  }

  /**
   * Sets the db attribute permission
   *
   * @param permission permissionbits according to permtype
   */
  public void setPermission(int permission) {
    if (!attributesModified()) {
      setModified(this.permission != permission);
    }
    this.permission = permission;
  }

  /**
   * Gets the db attribute allowed
   *
   * @return false=denied, true=allowed
   */
  public boolean getAllowed()    {
    return allowed;
  }

  /**
   * Sets the db attribute allowed
   *
   * @param allowed false=denied, true=allowed
   */
  public void setAllowed(boolean allowed) {
    if (!attributesModified()) {
      setModified(this.allowed != allowed);
    }
    this.allowed = allowed;
  }

  /**
   * Gets the db attribute message
   *
   * @return user message
   */
  public String getMessage()    {
    return message;
  }

  /**
   * Sets the db attribute message
   *
   * @param message user message
   */
  public void setMessage(String message) {
    if (!attributesModified()) {
      setModified(Compare.equals(this.message, message) == false);
    }
    this.message = message;
  }

  /**
   * Gets the db attribute extendedRule
   *
   * @return extension object
   */
  public Binary getExtendedRule()    {
    return extendedRule;
  }

  /**
   * Sets the db attribute extendedRule
   *
   * @param extendedRule extension object
   */
  public void setExtendedRule(Binary extendedRule) {
    if (!attributesModified()) {
      setModified(Compare.equals(this.extendedRule, extendedRule) == false);
    }
    this.extendedRule = extendedRule;
  }

  // End of wurblet generated code.//GEN-END:methods


  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to assert objectClass != null. 
   */
  @Override
  public boolean prepareSetFields() {
    return objectClass != null && super.prepareSetFields();
  }

  
  public boolean getFields(ResultSetWrapper rs)  {
    // @wurblet getFields DbGetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:getFields

    if (columnsValid == false)  {
      if (!isGettingFieldCount() && (rs.getColumnOffset() > 0 || isPartial())) {
        // invoked within joined select the first time
        getFieldCount(); // get column indexes with offset 0
        if (columnsValid == false) {
          DbGlobal.errorHandler.severe(rs.getDb(), null, 
                "initial getFieldCount() failed in " + getTableName()); 
        }
      }
      else {
        COLUMN_OBJECTCLASS = rs.findColumn(FIELD_OBJECTCLASS);
        updateFieldCount(COLUMN_OBJECTCLASS);
        COLUMN_OBJECTID = rs.findColumn(FIELD_OBJECTID);
        updateFieldCount(COLUMN_OBJECTID);
        COLUMN_CONTEXTID = rs.findColumn(FIELD_CONTEXTID);
        updateFieldCount(COLUMN_CONTEXTID);
        COLUMN_CONTEXTCLASS = rs.findColumn(FIELD_CONTEXTCLASS);
        updateFieldCount(COLUMN_CONTEXTCLASS);
        COLUMN_GRANTID = rs.findColumn(FIELD_GRANTID);
        updateFieldCount(COLUMN_GRANTID);
        COLUMN_GRANTCLASS = rs.findColumn(FIELD_GRANTCLASS);
        updateFieldCount(COLUMN_GRANTCLASS);
        COLUMN_PRIORITY = rs.findColumn(FIELD_PRIORITY);
        updateFieldCount(COLUMN_PRIORITY);
        COLUMN_PERMISSIONTYPE = rs.findColumn(FIELD_PERMISSIONTYPE);
        updateFieldCount(COLUMN_PERMISSIONTYPE);
        COLUMN_PERMISSION = rs.findColumn(FIELD_PERMISSION);
        updateFieldCount(COLUMN_PERMISSION);
        COLUMN_ALLOWED = rs.findColumn(FIELD_ALLOWED);
        updateFieldCount(COLUMN_ALLOWED);
        COLUMN_MESSAGE = rs.findColumn(FIELD_MESSAGE);
        updateFieldCount(COLUMN_MESSAGE);
        COLUMN_EXTENDEDRULE = rs.findColumn(FIELD_EXTENDEDRULE);
        updateFieldCount(COLUMN_EXTENDEDRULE);
        COLUMN_SERIAL = rs.findColumn(FIELD_SERIAL);
        updateFieldCount(COLUMN_SERIAL);
        COLUMN_ID = rs.findColumn(FIELD_ID);
        updateFieldCount(COLUMN_ID);
        columnsValid = true;
      }
    }

    if (rs.getRow() <= 0) {
      return false;   // no valid row
    }

    objectClass = rs.getString(COLUMN_OBJECTCLASS);
    objectId = rs.getLong(COLUMN_OBJECTID);
    contextId = rs.getLong(COLUMN_CONTEXTID);
    contextClass = rs.getString(COLUMN_CONTEXTCLASS);
    grantId = rs.getLong(COLUMN_GRANTID);
    grantClass = rs.getString(COLUMN_GRANTCLASS);
    priority = rs.getInt(COLUMN_PRIORITY);
    permissionType = rs.getInt(COLUMN_PERMISSIONTYPE);
    permission = rs.getInt(COLUMN_PERMISSION);
    allowed = rs.getBoolean(COLUMN_ALLOWED);
    message = rs.getString(COLUMN_MESSAGE);
    extendedRule = rs.getBinary(COLUMN_EXTENDEDRULE, 0);
    setId(rs.getLong(COLUMN_ID));
    setSerial(rs.getLong(COLUMN_SERIAL));

    // End of wurblet generated code.//GEN-END:getFields
    return true;
  }


  public int setFields(PreparedStatementWrapper st)	{
    // @wurblet setFields DbSetFields $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:setFields

    int ndx = 0;
    st.setString(++ndx, objectClass); 
    st.setLong(++ndx, objectId); 
    st.setLong(++ndx, contextId); 
    st.setString(++ndx, contextClass); 
    st.setLong(++ndx, grantId); 
    st.setString(++ndx, grantClass); 
    st.setInt(++ndx, priority); 
    st.setInt(++ndx, permissionType); 
    st.setInt(++ndx, permission); 
    st.setBoolean(++ndx, allowed); 
    st.setString(++ndx, message); 
    st.setBinary(++ndx, extendedRule); 
    st.setLong(++ndx, getId());
    st.setLong(++ndx, getSerial());

    // End of wurblet generated code.//GEN-END:setFields
    return ndx;
  }


  public int prepareInsertStatement ()  {
    // @wurblet insert DbInsert $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:insert

    int stmtId = getInsertStatementId();
    if (stmtId == 0 || alwaysPrepare()) {
      // prepare it
      stmtId = getDb().prepareStatement(
            "INSERT INTO " + getTableName()
            + " (" + FIELD_OBJECTCLASS
            + ","  + FIELD_OBJECTID
            + ","  + FIELD_CONTEXTID
            + ","  + FIELD_CONTEXTCLASS
            + ","  + FIELD_GRANTID
            + ","  + FIELD_GRANTCLASS
            + ","  + FIELD_PRIORITY
            + ","  + FIELD_PERMISSIONTYPE
            + ","  + FIELD_PERMISSION
            + ","  + FIELD_ALLOWED
            + ","  + FIELD_MESSAGE
            + ","  + FIELD_EXTENDEDRULE
            + ","  + FIELD_ID
            + ","  + FIELD_SERIAL + ") VALUES (" +
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +     
            "?," +
            "?)");
      setInsertStatementId(stmtId);
    }

    // End of wurblet generated code.//GEN-END:insert
    
    return stmtId;
  }


  public int prepareUpdateStatement () {
    // @wurblet update DbUpdate $mapfile

    // Code generated by wurblet. Do not edit!//GEN-BEGIN:update

    int stmtId = getUpdateStatementId();
    if (stmtId == 0 || alwaysPrepare())  {
      // prepare it
      stmtId = getDb().prepareStatement(
            "UPDATE " + getTableName() + " SET "
            +       FIELD_OBJECTCLASS + "=?"
            + "," + FIELD_OBJECTID + "=?"
            + "," + FIELD_CONTEXTID + "=?"
            + "," + FIELD_CONTEXTCLASS + "=?"
            + "," + FIELD_GRANTID + "=?"
            + "," + FIELD_GRANTCLASS + "=?"
            + "," + FIELD_PRIORITY + "=?"
            + "," + FIELD_PERMISSIONTYPE + "=?"
            + "," + FIELD_PERMISSION + "=?"
            + "," + FIELD_ALLOWED + "=?"
            + "," + FIELD_MESSAGE + "=?"
            + "," + FIELD_EXTENDEDRULE + "=?"
            + "," + FIELD_SERIAL + "=" + FIELD_SERIAL + "+1"
            + " WHERE " + FIELD_ID + "=?"
            + " AND " + FIELD_SERIAL + "=?"
            );
      setUpdateStatementId(stmtId);
    }


    // End of wurblet generated code.//GEN-END:update
    
    return stmtId;
  }
  


  // @wurblet declare DbDeclare $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:declare


  /** classbasename if database class, else full classname **/
  private String objectClass;

  /** object id, 0=all or not a database class **/
  private long objectId;

  /** ID of context object, 0 = all of contextClass **/
  private long contextId;

  /** classbasename of ContextDb's Rootobject **/
  private String contextClass;

  /** ID of grantobject, 0 = all of grantClass **/
  private long grantId;

  /** classbasename of grantobject **/
  private String grantClass;

  /** ordering, 0=first **/
  private int priority;

  /** set of possible Permissions **/
  private int permissionType;

  /** permissionbits according to permtype **/
  private int permission;

  /** false=denied, true=allowed **/
  private boolean allowed;

  /** user message **/
  private String message;

  /** extension object **/
  private Binary extendedRule;

  // End of wurblet generated code.//GEN-END:declare



  
  

  // @wurblet fieldNames DbFieldNames $mapfile

  // Code generated by wurblet. Do not edit!//GEN-BEGIN:fieldNames

  private static boolean columnsValid;    // true if COLUMN_.... are valid for getFields()
  /** database column name for objectClass **/
  public  static final String FIELD_OBJECTCLASS = "objectclass";
  private static       int    COLUMN_OBJECTCLASS;
  /** database column name for objectId **/
  public  static final String FIELD_OBJECTID = "objectid";
  private static       int    COLUMN_OBJECTID;
  /** database column name for contextId **/
  public  static final String FIELD_CONTEXTID = "contextid";
  private static       int    COLUMN_CONTEXTID;
  /** database column name for contextClass **/
  public  static final String FIELD_CONTEXTCLASS = "contextclass";
  private static       int    COLUMN_CONTEXTCLASS;
  /** database column name for grantId **/
  public  static final String FIELD_GRANTID = "grantid";
  private static       int    COLUMN_GRANTID;
  /** database column name for grantClass **/
  public  static final String FIELD_GRANTCLASS = "grantclass";
  private static       int    COLUMN_GRANTCLASS;
  /** database column name for priority **/
  public  static final String FIELD_PRIORITY = "priority";
  private static       int    COLUMN_PRIORITY;
  /** database column name for permissionType **/
  public  static final String FIELD_PERMISSIONTYPE = "permtype";
  private static       int    COLUMN_PERMISSIONTYPE;
  /** database column name for permission **/
  public  static final String FIELD_PERMISSION = "permission";
  private static       int    COLUMN_PERMISSION;
  /** database column name for allowed **/
  public  static final String FIELD_ALLOWED = "allowed";
  private static       int    COLUMN_ALLOWED;
  /** database column name for message **/
  public  static final String FIELD_MESSAGE = "message";
  private static       int    COLUMN_MESSAGE;
  /** database column name for extendedRule **/
  public  static final String FIELD_EXTENDEDRULE = "extrule";
  private static       int    COLUMN_EXTENDEDRULE;
  private static       int    COLUMN_ID;
  private static       int    COLUMN_SERIAL;

  // End of wurblet generated code.//GEN-END:fieldNames

}

