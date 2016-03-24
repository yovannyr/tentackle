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

// $Id: HistoryTableEntry.java 336 2008-05-09 14:40:20Z harald $
// Created on June 8, 2005, 12:03 PM

package org.tentackle.appworx;

import org.tentackle.ui.FormField;
import org.tentackle.ui.FormTableEntry;
import javax.swing.table.TableCellRenderer;

/**
 * FormTableEntry for {@link History} objects.
 * <p>
 * The implementation takes the {@link FormTableEntry} of the
 * database object and prepends the history columns.
 * 
 * @author harald
 */
public class HistoryTableEntry extends FormTableEntry {
  
  /** column names **/
  protected static final String[] columnNames = {
    Locales.bundle.getString("Time"),
    Locales.bundle.getString("User"),
    Locales.bundle.getString("Type"),
  };

  /** index for timestamp column **/
  protected static final int TIME = 0;
  /** index for username column **/
  protected static final int USER = 1;
  /** index for modification type column **/
  protected static final int TYPE = 2;
  
  
  
  private History history;                    // history object
  private FormTableEntry objectTableEntry;    // formtable enrty of the database object
  
  
  
  /**
   * Creates a history table entry.
   * 
   * @param history the history object
   */
  public HistoryTableEntry(History history)  {
    this.history = history;
    objectTableEntry = history.getAppDbObject().getFormTableEntry();
  }


  public FormTableEntry newInstanceOf(Object object) {
    return new HistoryTableEntry((History)object);
  }

  
  public Object getObject() {
    return history;
  }


  public String getColumnName(int col) {
    return col < columnNames.length ? columnNames[col] : objectTableEntry.getColumnName(col - columnNames.length);
  }


  public int getColumnCount() {
    return columnNames.length + objectTableEntry.getColumnCount();
  }
  
  
  public Object getValueAt (int col)  {
    try {
      switch (col)  {
        case TIME:       return history.getTime();
        case USER:       return history.getUser();
        case TYPE:       return History.typeToString(history.getType());
      }
    }
    catch (Exception e) {
      return null;
    }
    return objectTableEntry.getValueAt(col - columnNames.length);
  }
  
  
  @Override
  public String getFormat(int col) {
    if (col == TIME)  {
      return Locales.bundle.getString("d/M/yy_HH:mm:ss");
    }
    return col < columnNames.length ? super.getFormat(col) : objectTableEntry.getFormat(col - columnNames.length);
  }
  
  
  @Override
  public boolean isCellEditable(int col)  {
    return false;   // never!
  }

  
  @Override
  public TableCellRenderer getCellRenderer(int col) {
    return col < columnNames.length ? super.getCellRenderer(col) : objectTableEntry.getCellRenderer(col - columnNames.length);
  }  

  
  @Override
  public Class getColumnClass(int col) {
    return col < columnNames.length ? super.getColumnClass(col) : objectTableEntry.getColumnClass(col - columnNames.length);
  }  

  
  @Override
  public char getConvert(int col) {
    return col < columnNames.length ? super.getConvert(col) : objectTableEntry.getConvert(col - columnNames.length);
  }
  
  
  @Override
  public int getHorizontalAlignment(int col) {
    return col < columnNames.length ? super.getHorizontalAlignment(col) : objectTableEntry.getHorizontalAlignment(col - columnNames.length);
  }  
  
  
  @Override
  public int getVerticalAlignment(int col) {
    return col < columnNames.length ? FormField.CENTER : objectTableEntry.getVerticalAlignment(col - columnNames.length);
  }  
  
  
  @Override
  public boolean isBlankZero(int col) {
    return col < columnNames.length ? super.isBlankZero(col) : objectTableEntry.isBlankZero(col - columnNames.length);
  }  
  
  
  @Override
  public boolean isVisiblyEqual(FormTableEntry entry)  {
    try {
      return objectTableEntry.isVisiblyEqual(((HistoryTableEntry)entry).objectTableEntry);
    }
    catch (Exception e) {
      return false;
    }
  }

}
