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

// $Id: ObjectId.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.db;

import org.tentackle.db.rmi.ObjectIdRemoteDelegate;
import org.tentackle.util.ApplicationException;




/**
 * A {@link DbObject} implementing {@link IdSource} used to create unique object IDs.
 * <p>
 * As the object-id is stored in a separate table, an instance
 * of an object is used to retrieve a new (unique) ID for a DbObject.
 * <p>
 * 
 * @author harald
 */
public class ObjectId extends DbObject implements IdSource, CommitTxRunnable {
  
  private static final long serialVersionUID = -2463189262257268747L;
  
  /** the table name **/
  public static final String TABLENAME = "objectid";
  
  private static DbObjectClassVariables classVariables = 
    new DbObjectClassVariables(ObjectId.class, TABLENAME, "Object ID", "Object IDs");

  
  private static int newObjectSelectStatementId;
  private static int newObjectIncrementStatementId;
  private static int newObjectUpdateStatementId;
  
  
  private long lastTxCount;           // for optimization within larger transactions
  private boolean registerTxPending;  // true if we need to register final update in Db 
  
  

  /**
   * Creates a new unique object ID.
   * <p>
   * use <CODE>new ObjectId(db).getId()</CODE> to get a new id for a DbObject.
   * <p>
   * @param db the database connection
   */

  public ObjectId (Db db) {
    super (db);
  }

  public ObjectId() {
    super();
  }
  
  
  /**
   * Retrieves the next ID and updates the table.
   * 
   * @throws ApplicationException if ID could not be created
   */
  public long nextId() throws ApplicationException {
    
    if (getDb().isRemote())  {
      try {
        setId(((ObjectIdRemoteDelegate)getRemoteDelegate()).nextId());
      }
      catch (Exception e) {
        throw new ApplicationException("getting next remote ID failed", e);
      }
    }
    
    else  {
      
      if (newObjectIncrementStatementId == 0) {
        newObjectIncrementStatementId = getDb().prepareStatement(
            "UPDATE " + getTableName() + " SET " + FIELD_ID + "=" + FIELD_ID + "+1");
        newObjectSelectStatementId = getDb().prepareStatement(
            "SELECT " + FIELD_ID + " FROM " + getTableName());
        newObjectUpdateStatementId = getDb().prepareStatement(
            "UPDATE " + getTableName() + " SET " + FIELD_ID + "=?");
      }

      /**
       * always within a transaction because nextId() is never invoked
       * without further db-operations
       */
      boolean oldcommit = getDb().begin();
      
      if (getDb().getTxCount() > lastTxCount) {
        
        // new transaction
        lastTxCount = getDb().getTxCount();   // remember current tx
        registerTxPending = true;             // next invokation will register final update  
        
        // increment by 1 (this starts isolation withing the transaction!)
        PreparedStatementWrapper newObjectIncrementStatement =
                getDb().getPreparedStatement(newObjectIncrementStatementId);
        PreparedStatementWrapper newObjectSelectStatement =
                getDb().getPreparedStatement(newObjectSelectStatementId);
        
        /**
         * Note: this will get the write-lock on objectid.
         * Further nextId() will only increment the id without any db-operation
         * until the transaction gets closed and the final update is done.
         */
        if (newObjectIncrementStatement.executeUpdate() == 1)  {
          // read back new value
          ResultSetWrapper rs = newObjectSelectStatement.executeQuery();
          if (rs.next())  {
            setId(rs.getLong(1));
            rs.close();
            // alles ok. Ende der Transaktion
            getDb().commit(oldcommit);
          }
        }
        else  {
          getDb().rollback(oldcommit);
          throw new ApplicationException("getting next ID failed");
        }
      }
      else  {
        // same tx: optimize and count only
        setId(getId() + 1);
        if (registerTxPending)  {
          // register final update for end of transaction
          getDb().registerCommitTxRunnable(this);
          registerTxPending = false;
        }
      }
    }
    
    return getId();
  }
  

  @Override
  public DbObject newObject() {
    return new ObjectId(getDb());
  }

  
  public void returnId(long id) throws ApplicationException {
    throw new DbRuntimeException("returnId not allowed for ObjectId");
  }

  
  public void commit() {
    PreparedStatementWrapper newObjectUpdateStatement =
            getDb().getPreparedStatement(newObjectUpdateStatementId);
    newObjectUpdateStatement.setLong(1, getId());   // set current value of ID
    if (newObjectUpdateStatement.executeUpdate() != 1)  {
      throw new DbRuntimeException("updating ID of " + TABLENAME + " failed in " + getDb()); // this will terminate the app if not caught
    }    
  }

  

  // ------- implement abstract methods that are not used ---------

  @Override
  public boolean insert() {
    return false;
  }

  @Override
  public boolean update() {
    return false;
  }

  @Override
  public DbObject select(long id) {
    return null;
  }

  @Override
  public int setFields(PreparedStatementWrapper st) { 
    return 0; 
  }

  @Override
  public boolean getFields(ResultSetWrapper rs) { 
    return false; 
  }

  @Override
  public int prepareUpdateStatement() {
    throw new DbRuntimeException("update not allowed for ObjectId");
  }
  
  @Override
  public int prepareInsertStatement() { 
    throw new DbRuntimeException("insert not allowed for ObjectId");
  }
  
  @Override
  public int prepareDeleteStatement() {
    throw new DbRuntimeException("delete not allowed for ObjectId");
  }

  @Override
  public DbObjectClassVariables getDbObjectClassVariables() {
    return classVariables;
  }  

}
