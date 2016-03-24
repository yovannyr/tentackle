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

// $Id: DefaultQbfPlugin.java 336 2008-05-09 14:40:20Z harald $
// Created on September 18, 2002, 3:56 PM

package org.tentackle.appworx;

import java.text.MessageFormat;
import org.tentackle.db.DbCursor;
import org.tentackle.db.DbGlobal;
import java.util.Collection;



/**
 * The default implementation of a {@code QbfPlugin}.
 *
 * @author harald
 */
public class DefaultQbfPlugin implements QbfPlugin {
  
  protected QbfParameter                      parameter;  // Abfrageparameter
  protected DbCursor<? extends AppDbObject>   cursor;     // the result cursor, null = arraylist
  protected Collection<? extends AppDbObject> list;       // the result list
  protected int                               rowCount;   // number of items
  
  
  /**
   * Creates a plugin from a parameter.
   *
   * @param parameter the qbf parameter
   */
  public DefaultQbfPlugin(QbfParameter parameter) {
    setParameter(parameter);
  }
  
  /**
   * Creates a plugin for a class and database context
   *
   * @param clazz the data object class
   * @param contextDb the database context
   */
  public DefaultQbfPlugin(Class<? extends AppDbObject> clazz, ContextDb contextDb) {
    this(new DefaultQbfParameter(clazz, contextDb));
  }
  
  

  public void cleanup() {
    if (cursor != null) {
      cursor.close();
    }
    // pass to GC
    list      = null;
    cursor    = null;
    rowCount  = -1;       // unknown
  }
  
  
  public boolean executeQuery() {
    
    cleanup();    // free cursor, if open
    
    try {
      
      if (parameter.isEmpty()) {
        // special case: QbfParameter is not extended and pattern is empty
        // --> use selectAllInContext(). This will also get the objects from cache if
        // this class is cached.
        setObjects(AppDbObject.newByClass(parameter.contextDb, parameter.clazz).selectAllInContextCached());
        if (list.isEmpty() == false)  {
          return true;
        }
      }
      else  {
        // really go by Qbf
        setCursor(AppDbObject.newByClass(parameter.contextDb, parameter.clazz).searchQbfCursor(parameter));
        
        if (cursor.first()) {
          // if at least one record exists
          return true;
        }
      }
      
      // nothing found
      cleanup();
    }
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(parameter.contextDb.getDb(), ex, null);
    }
    
    return false;
  }
  
  
  public DbCursor<? extends AppDbObject> getCursor() {
    return cursor;
  }
  
  
  /**
   * Sets the cursor.
   *
   * @param cursor the cursor
   */
  public void setCursor(DbCursor<? extends AppDbObject> cursor)  {
    this.cursor = cursor;
    this.list = null;
    rowCount = -1;
  }
  
  
  public Collection<? extends AppDbObject> getObjects() {
    if (cursor != null) {
      return cursor.toList();    // Liste liefern, falls gewünscht
    }
    return list;
  }
  
  
  /**
   * Sets the objects.
   *
   * @param objects the collection
   */
  public void setObjects(Collection<? extends AppDbObject> objects) {
    this.list = objects;
    this.cursor = null;
    rowCount = -1;
  }
  
  
  public QbfPanel getPanel() {
    return new DefaultQbfPanel(parameter);
  }
  
  
  public void setParameter(QbfParameter parameter) {
    this.parameter = parameter;
  }
  
  
  public QbfParameter getParameter() {
    return parameter;
  }
  
  
  public boolean isParameterValid() {
    return parameter.isValid();
  }
  

  public boolean isResultDisplayable() {
    return true;
  }
  
  
  public AppDbObject newAppDbObject() {
    try {
      return AppDbObject.newByClass(parameter.contextDb, parameter.clazz);
    }
    catch (Exception ex)  {
      DbGlobal.errorHandler.severe(parameter.contextDb.getDb(), ex, null);
      return null;    // notreached
    }
  }  
  
  
  public String notFoundMessage() {
    return MessageFormat.format(Locales.bundle.getString("no_such_{0}_found"), newAppDbObject().getMultiName());
  }
  

  public int getRowCount() {
    if (rowCount == -1) {
      // still unknown
      if (cursor != null) {
        rowCount = cursor.getRowCount();
      }
      else  {
        rowCount = list.size();
      }
    }
    return rowCount;
  }  

}
