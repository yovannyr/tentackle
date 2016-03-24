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

// $Id: FormTableModel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.util.List;
import org.tentackle.db.DbCursor;
import org.tentackle.db.DbObject;


/**
 * Table-model for {@link FormTable}s.
 *
 * @author harald
 */
public class FormTableModel extends AbstractFormTableModel {

  private FormTableEntry template;                // template zum Erzeugen von Objekten
  private FormTableEntry[] entries;               // alle Objekte im Cache
  
  private DbCursor<? extends DbObject> cursor;    // != null falls über Cursor
  private List<?> list;                           // != null falls über List
  private Object[] array;                         // != null falls über Array


  /**
   * Creates a table model for a database cursor.
   * 
   * @param template  the table entry as a template to create other entries
   * @param cursor    the database cursor
   */
  public FormTableModel (FormTableEntry template, DbCursor<? extends DbObject> cursor) {
    this.template = template;
    cursorChanged(cursor);
  }

  /**
   * Creates a table model for a list of objects.
   * 
   * @param template  the table entry as a template to create other entries
   * @param list      the list of objects
   */
  public FormTableModel (FormTableEntry template, List<?> list) {
    this.template = template;
    listChanged(list);
  }
  
  /**
   * Creates a table model for an array of objects.
   * 
   * @param template  the table entry as a template to create other entries
   * @param array     the array of objects
   */
  public FormTableModel (FormTableEntry template, Object[] array) {
    this.template = template;
    listChanged(array);
  }


  /**
   * Creates an empty table model for a given template.
   * 
   * @param template  the table entry as a template to create other entries
   */
  public FormTableModel (FormTableEntry template) {
    this.template = template;
  }

  
  /**
   * Sets the template (useful if context changed)
   * @param template the new template
   */
  public void setTemplate(FormTableEntry template)  {
    this.template = template;
  }
  
  
  public FormTableEntry getTemplate() {
    return template;
  }


  public int getRowCount() {
    return entries == null ? 0 : entries.length;
  }


  public int getColumnCount() {
    return template.getColumnCount();
  }

  
  @Override
  public String getColumnName (int columnIndex) {
    return template.getColumnName(columnIndex);
  }
  
  public String getDisplayedColumnName (int columnIndex) {
    return template.getDisplayedColumnName(columnIndex);
  }
  
  
  public Object getValueAt(int rowIndex, int columnIndex) {
    FormTableEntry entry = getEntryAt(rowIndex);
    return entry == null ? null : entry.getValueAt(columnIndex);
  }


  /**
   * Sets the cell value.
   */
  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    FormTableEntry entry = getEntryAt(rowIndex);
    if (entry != null)  {
      entry.setValueAt(columnIndex, aValue);
      setDataChanged(true);
      fireTableCellUpdated(rowIndex, columnIndex);
    }
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to propagate triggerValueChanged()
   */
  @Override
  public void setDataChanged(boolean dataChanged) {
    boolean wasChanged = isDataChanged();
    super.setDataChanged(dataChanged);
    if (!wasChanged && dataChanged) {
      FormTable table = getTable();
      if (table != null)  {
        FormHelper.triggerValueChanged(table);
      }
    }
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to get the table if models are chained
   */
  @Override
  public FormTable getTable()  {
    AbstractFormTableModel model = this;
    FormTable table = super.getTable();
    while (table == null)  {
      model = model.getMap();
      if (model == null) {
        break;
      }
      table = model.getTable();
    }
    return table;
  }
  

  public FormTableEntry getEntryAt (int row)  {
    
    synchronized (this) {
      if (row < 0 || row >= getRowCount())  {
        return null;
      }
      else  {
        if (entries[row] == null) {
          // build new entry
          FormTableEntry entry = null;
          if (list != null)  {
            entry = template.newInstanceOf(list.get(row));
          }
          else if (cursor != null)  {
            entry = template.newInstanceOf(cursor.getObjectAt(row + 1));
          }
          else if (array != null) {
            entry = template.newInstanceOf(array[row]); 
          }
          if (entry != null)  {
            entry.setModel(this);
            entry.setRow(row);
            entries[row] = entry;
          }
        }
        return entries[row];
      }
    }
  }


  @Override
  public boolean setEntryAt (FormTableEntry entry, int row) {
    boolean rv = false;
    synchronized (this) {
      if (row >= 0 && row < getRowCount())  {
        entries[row] = entry;
        // update im darunterliegenden Layer
        if      (list   != null) {
          rv = entry.updateList(row);         // falls List
        }
        else if (cursor != null) {
          rv = entry.updateCursor(row);       // falls Cursor
        }
        else if (array != null) {
          rv = entry.updateArray(row);        // falls Array
        }
        if (rv) {
          setDataChanged(true);
          fireTableRowsUpdated(row, row);
        }
        return rv;
      }
    }
    return false;
  }

  
  @Override
  public Class getColumnClass (int columnIndex) {
    // check if formtable entry overrides default
    Class colClass = template.getColumnClass(columnIndex);
    if (colClass != null) {
      return colClass;
    }
    // analyze the data
    int rows = getRowCount();
    for (int i=0; i < rows; i++)  {
      FormTableEntry entry = getEntryAt(i);
      if (entry != null)  {
        Object value = entry.getValueAt(columnIndex);
        if (value != null) {
          return value.getClass();
        }
      }
    }
    return Object.class;
  }

  @Override
  public boolean isCellEditable (int rowIndex, int columnIndex)  {
    FormTableEntry entry = getEntryAt(rowIndex);
    return entry != null ? entry.isCellEditable(columnIndex) : false;
  }

  
  
  /**
   * Sets a new cursor and fires tableDataChanged.
   * 
   * @param cursor the new cursor
   */
  public void cursorChanged(DbCursor<? extends DbObject> cursor) {
    synchronized (this) {
      this.cursor = cursor;
      this.list   = null;
      this.array  = null;
      setDataChanged(false);
      entries = (cursor == null ? null : new FormTableEntry[cursor.getRowCount()]);
      fireTableDataChanged();   // generate the right fireTableChanged-Event
    }
  }

  
  /**
   * Sets a new list of objects and fires tableDataChanged.
   * 
   * @param list the new list of objects
   */
  public void listChanged(List<?> list) {
    synchronized (this) {
      this.list   = list;
      this.cursor = null;
      this.array  = null;
      setDataChanged(false);
      entries = (list == null ? null : new FormTableEntry[list.size()]);
      fireTableDataChanged();   // generate the right fireTableChanged-Event
    }
  }
  
  
  
  /**
   * Sets a new array of objects and fires tableDataChanged.
   * 
   * @param array the new array of objects
   */
  public void listChanged(Object[] array) {
    synchronized (this) {
      this.array  = array;
      this.list   = null;
      this.cursor = null;
      setDataChanged(false);
      entries = (array == null ? null : new FormTableEntry[array.length]);
      fireTableDataChanged();   // generate the right fireTableChanged-Event
    }
  }

  

  
  /**
   * Denotes that a range of rows have been changed and fires tableRowsUpdated.
   * The row numbers are silently aligned if out of range.
   * 
   * @param firstRow the first row
   * @param lastRow the last changed row (>= firstRow)
   */
  public void listUpdated(int firstRow, int lastRow) {
    synchronized (this) {
      
      if (firstRow < 0) {
        firstRow = 0;
      }
      if (lastRow < firstRow) {
        lastRow = firstRow;
      }
      if (lastRow >= entries.length) {
        lastRow = entries.length - 1;
      }
      
      if (lastRow >= firstRow) {
        for (int i=firstRow; i <= lastRow; i++) {
          entries[i] = null;    // row will be reloaded if used
        }
        setDataChanged(true);
        fireTableRowsUpdated(firstRow, lastRow);
      }
    }
  }

  
  
  /**
   * Denotes that a table cell has changed and fires tableCellUpdated.
   * If the row is out of range, nothing will be done.
   * The handling of the column depends on the listener for the
   * TableModelEvent.
   * 
   * @param rowIndex the row number
   * @param columnIndex the column index
   */
  public void listCellUpdated(int rowIndex, int columnIndex) {
    synchronized (this) {
      if (rowIndex >= 0 && rowIndex < entries.length) {
        entries[rowIndex] = null;
        setDataChanged(true);
        fireTableCellUpdated(rowIndex, columnIndex);
      }
    }
  }

  
  /**
   * Denotes that a range of rows have been insert and fires tableRowsInserted.
   * The row numbers are silently aligned if out of range.
   * Will nullpex if not in list-mode.
   * 
   * @param firstRow the first row
   * @param lastRow the last changed row (>= firstRow)
   */
  public void listInserted(int firstRow, int lastRow)  {
    synchronized (this) {
      int newSize = list.size();
      if (firstRow < 0) {
        firstRow = 0;
      }
      if (lastRow < firstRow) {
        lastRow = firstRow;
      }
      if (lastRow >= newSize) {
        lastRow = newSize - 1;
      }
      if (lastRow >= firstRow) {
        FormTableEntry[] oldEntries = entries;
        entries = new FormTableEntry[newSize];
        int i;
        for (i=0; i < firstRow; i++) {
          entries[i] = oldEntries[i];
        }
        for (; i <= lastRow; i++) {
          entries[i] = null;
        }
        int inserted = lastRow - firstRow + 1;
        for (; i < entries.length; i++) {
          entries[i] = oldEntries[i - inserted];
        }
        setDataChanged(true);
        fireTableRowsInserted(firstRow, lastRow);
      }
    }
  }

  
  /**
   * Denotes that a range of rows have been deleted and fires tableRowsDeleted.
   * The row numbers are silently aligned if out of range.
   * Will nullpex if not in list-mode.
   * 
   * @param firstRow the first row
   * @param lastRow the last changed row (>= firstRow)
   */
  public void listDeleted(int firstRow, int lastRow) {
    synchronized (this) {
      if (firstRow < 0) {
        firstRow = 0;
      }
      if (lastRow < firstRow) {
        lastRow = firstRow;
      }
      if (lastRow >= entries.length) {
        lastRow = entries.length - 1;
      }
      if (lastRow >= firstRow) {
        FormTableEntry[] oldEntries = entries;
        entries = new FormTableEntry[list.size()];
        int i;
        for (i=0; i < firstRow; i++)  {
          entries[i] = oldEntries[i];
        }
        int deleted = lastRow - firstRow + 1;
        for (; i < entries.length; i++) {
          entries[i] = oldEntries[i + deleted];
        }
        setDataChanged(true);
        fireTableRowsDeleted(firstRow, lastRow);
      }
    }
  }

}
