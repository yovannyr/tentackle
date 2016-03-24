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

// $Id: DbPreferencesFactory.java 427 2008-09-15 09:14:46Z harald $



package org.tentackle.db;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;




/**
 * Factory for DbPreferences.  This class allows DbPreferences
 * to be installed as the Preferences implementations via the
 * java.util.prefs.PreferencesFactory system property.
 */
  

public class DbPreferencesFactory implements PreferencesFactory {
  
  
  /**
   * Installs the factory class for the Preferences-API.
   *
   * @param db is the db-connection the prefs are stored
   * @param autoSync is true if automatically sync with other JVMs (recommended)
   */
  public static void installPreferencesFactory(Db db, boolean autoSync)  {
    System.setProperty("java.util.prefs.PreferencesFactory", "org.tentackle.db.DbPreferencesFactory");
    DbPreferences.initialize(db, autoSync);
  }
  
  
  public DbPreferencesFactory() {
  }
  
  
  public Preferences userRoot() {
    return DbPreferences.getUserRoot();
  }

  public Preferences systemRoot() {
    return DbPreferences.getSystemRoot();
  }
  
  
  /**
   * Creates a DbPreferences instance.
   * 
   * @param userMode true if user mode
   * @return the preferences instance
   */
  public DbPreferences createPreferences(boolean userMode) {
    return new DbPreferences(userMode);
  }
  
  /**
   * Creates a DbPreferences instance from a parent and a name.
   * 
   * @param parent the parent preferences node
   * @param name the name of the child node
   * @return the preferences instance
   */
  public DbPreferences createPreferences(DbPreferences parent, String name) {
    return new DbPreferences(parent, name);
  }
  
  /**
   * Creates a DbPreferences instance from a parent and a node.
   * Protected scope!
   * 
   * @param parent the parent preferences
   * @param node the node
   * @return the preferences instance
   * @throws BackingStoreException 
   */
  protected DbPreferences createPreferences(DbPreferences parent, DbPreferencesNode node) throws BackingStoreException {
    return new DbPreferences(parent, node);
  }
  
  
  /**
   * Creates a new DbPreferencesNode.<br>
   * All nodes are created through this method.
   * This allows replacing the concrete implementation by
   * overriding the method.
   * 
   * @param db the db connection
   * @return the new node within the current db
   */
  public DbPreferencesNode createNode(Db db) {
    return new DbPreferencesNode(db);
  }
  
  
  /**
   * Creates a new DbPreferencesKey.
   * All keys are created through this method.
   * This allows replacing the concrete implementation by
   * overriding the method.
   * 
   * @param db the db connection
   * @return the new key within the current db
   */
  public DbPreferencesKey createKey(Db db) {
    return new DbPreferencesKey(db);
  }
  
  
}
