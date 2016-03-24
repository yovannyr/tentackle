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

// $Id: PartialDbObject.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.db;

/**
 * Partial implementation of a {@link DbObject} that is an abstract
 * super class in a table-per-class mapping.
 * 
 * @author harald
 */
public abstract class PartialDbObject extends DbObject {

  
  private DbObject master;    // master object
  
  
  /**
   * Creates a partial object for a given master object.
   * 
   * @param master the master object
   */
  public PartialDbObject(DbObject master) {
    super(master.getDb());
    this.master = master;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to return true.
   * Notice that for hierarchies of depth > 2 the DbGetFields wurblet
   * must be invoked with the "--nameonly" option.
   */
  @Override
  public boolean isPartial() {
    return true;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to return false.
   * Partial object are not entities by definition.
   */
  @Override
  public boolean isEntity() {
    return false;
  }
  
  @Override
  public Db getDb() {
    return master.getDb();
  }

  @Override
  public void setDb(Db db) {
    master.setDb(db);
  }

  @Override
  public String getClassBaseName() {
    return master.getClassBaseName();
  }

  @Override
  public long getId() {
    return master.getId();
  }

  @Override
  public void setId(long id) {
    master.setId(id);
    super.setId(id);
  }
  
  @Override
  public void setSerial(long serial) {
    master.setSerial(serial);
    super.setSerial(serial);
  }

  // notice: serial is kept per partial record!

  @Override
  public boolean isNew() {
    return master.isNew();
  }

  @Override
  public String getClassName() {
    return master.getClassName();
  }
  
}
