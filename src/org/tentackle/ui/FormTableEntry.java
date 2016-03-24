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

// $Id: FormTableEntry.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import org.tentackle.util.Compare;
import java.awt.Rectangle;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;




/**
 * Entry per object that describes the data and configuration in order to
 * keep all table-config-stuff in one place.
 */
abstract public class FormTableEntry {

  private AbstractFormTableModel model;       // the model this entry lives in
  private int row;                            // the row of this entry with respect to the model
  
  
  // --------------- methods referring to all objects in a table ------------
  
  /**
   * Creates a new instance of an entry for a given data-object.
   *
   * @param object the data-object
   * @return the FormTableEntry for this object
   */
  abstract public FormTableEntry newInstanceOf (Object object);
  
  /**
   * Determines the number of data-model columns.
   * Notice that not all columns may actually be visible.
   *
   * @return the number of columns
   */
  abstract public int getColumnCount();  
  
  /**
   * Describes the column name.<br>
   * This may be a symbolic name if getDisplayedColumnName()
   * is overridden.
   *
   * @param mColumn the datamodel-column 
   * @return the name of the column
   */
  abstract public String getColumnName(int mColumn);

  /**
   * Gets the displayed column name.<br>
   * By default the column-name and the displayed column name
   * are the same.
   * @param mColumn the datamodel column
   * @return the display name of the column
   */
  public String getDisplayedColumnName(int mColumn) {
    return getColumnName(mColumn);
  }
  
  
  /**
   * Determines the class for a given column.<br>
   * If not overridden (or returning null) the class will be
   * determined by inspecting the data.
   *
   * @param mColumn the datamodel-column
   * @return the column class or null
   */
  public Class getColumnClass (int mColumn) {
    return null;
  }
  
  
  /**
   * Defines the format (for numeric or date/time-types)
   * for each column. If null is returned a default
   * format will be used according to the column class.
   *
   * @param mColumn the datamodel-column
   * @return the format or null if default
   */
  public String getFormat(int mColumn)  {
    return null;
  }

  /**
   * Defines the horizontal alignment
   * for each column. If -1 is returned a default
   * will be used according to the column class.
   *
   * @param mColumn the datamodel-column
   * @return the alignment or -1 if default
   */
  public int getHorizontalAlignment(int mColumn)  {
    return -1;
  }
  
  /**
   * Defines the vertical alignment
   * for each column. If -1 is returned a default
   * will be used according to the column class.
   *
   * @param mColumn the datamodel-column
   * @return the alignment or -1 if default
   */
  public int getVerticalAlignment(int mColumn)  {
    return -1;
  }

  /**
   * Defines the "blankzero" attribute
   * for each column.
   *
   * @param mColumn the datamodel-column
   * @return true if blank zeros, false if not (default)
   */
  public boolean isBlankZero(int mColumn)  {
    return false;
  }
  
  /**
   * Gets the autoselect flag.
   *
   * @param mColumn the datamodel-column
   * @return true if autoselect, false if not (default)
   */
  public boolean isAutoSelect(int mColumn) {
    return false;
  }

  /**
   * Defines the character conversion attribute
   * for each column. Default is CONVERT_NONE.
   *
   * @param mColumn the datamodel-column
   * @return the conversion
   */
  public char getConvert (int mColumn)  {
    return FormField.CONVERT_NONE;
  }
  
  

  
  /**
   * Determines whether the given column is summable.<br>
   * The SumFormTableEntry will sumup all numeric columns by default.
   * However, for some numeric columns it doesn't make sense to build sums.
   * In this case, better override this method.
   * Furthermore, some apps (like AppDbObjectNaviPanel) check the first
   * row to determine whether a sumup-Button should be visible at all.
   * If the first row contains null-values, it cannot eliminate the possibilty
   * that this column is a numeric unless the getColumnClass() method explicitly
   * tells so.
   *
   * @param mColumn the datamodel column
   * @return true if column is definitely NOT numeric, else we dont know
   */
  public boolean isColumnNotSummable(int mColumn)  {
    Class clazz = getColumnClass(mColumn);
    if (clazz != null && !Number.class.isAssignableFrom(clazz)) {
      return true;    // is not a number
    }
    Object value = getValueAt(mColumn);
    if (value != null && !(value instanceof Number))  {
      return true;    // is not a number
    }
    return false;   // is probably a number, but we can't tell for sure    
  }
  
  

  
  /**
   * Determines whether the cell renderers are fixed.<br>
   * FormTable invokes getCellRenderer in FormTableEntry only once to
   * improve performance. If the cellrenderer changes according to
   * content this method must be overridden.
   *
   * @return false if FormTable should invoke getCellRenderer for every cell.
   */
  public boolean isCellRendererFixed()  { return true; }
  
  /**
   * Determines whether the cell editors are fixed.<br>
   * FormTable invokes getCellEditor in FormTableEntry only once to
   * improve performance. If the celleditor changes according to
   * content this method must be overridden.
   *
   * @return false if FormTable should invoke getCellEditor for every cell.
   */
  public boolean isCellEditorFixed()  { return true; }
  
  /**
   * Determines whether the cell rectangles are fixed.<br>
   * Usually the cell dimension is fixed and does not depend on the data.
   * However, in special cases (e.g. multi-column cells), it is desirable
   * to get the cellRect computed for every cell.
   *
   * @return true if cell-dimensions are fixed (default).
   */
  public boolean isCellRectFixed() { return true; }
  
  
  
  
  
  // --------------- methods referring to a single row ----------------------
  
  /**
   * Sets the model this entry lives in.
   * Will be invoked in FormTableModel.getEntryAt.
   *
   * @param model the data-model
   */
  public void setModel(AbstractFormTableModel model)  {
    this.model = model;
  }
  
  /**
   * Gets the model.
   * Useful to determine the chain of models up to the table
   * @return the data-model the entry lives in
   */
  public AbstractFormTableModel getModel()  {
    return model;
  }
  
  /**
   * Sets the row of this entry with respect to the model.
   * Will be invoked in FormTableModel.getEntryAt.
   *
   * @param row the data-model row
   */
  public void setRow(int row)  {
    this.row = row;
  }
  
  /**
   * Gets the row of this entry with respect to the model.
   * Useful to determine the model row and via the
   * model the view-row.
   * @return the data-model row of this entry
   */
  public int getRow()  {
    return row;
  }  
  
  /**
   * Gets the object wrapped by this entry.
   *
   * @return the data object
   */
  abstract public Object getObject();


  /**
   * Gets the column-object for this entry in a given column.
   *
   * @param mColumn the datamodel-column
   * @return the column data object
   */
  abstract public Object getValueAt (int mColumn);

  
  /**
   * Sets the data object for a column.
   * The default implementation does nothing.
   *
   * @param mColumn the datamodel-column
   * @param value the cell value
   */
  public void setValueAt (int mColumn, Object value) {}
  

  /**
   * Performs an update of the current entry to the underlying
   * database cursor, if the data-model is based on a cursor.
   * The default implemenation does nothing and returns true.
   *
   * @param mRow the datamodel-row
   * @return true if update sucessful, false if not.
   */
  public boolean updateCursor(int mRow) { return true; }

  /**
   * Performs an update of the current entry to the underlying
   * list, if the data-model is based on a List.
   * The default implemenation does nothing and returns true.
   *
   * @param mRow the datamodel-row
   * @return true if update sucessful, false if not.
   */
  public boolean updateList(int mRow) { return true; }
  
  /**
   * Performs an update of the current entry to the underlying
   * array, if the data-model is based on an object array.
   * The default implemenation does nothing and returns true.
   *
   * @param mRow the datamodel-row
   * @return true if update sucessful, false if not.
   */
  public boolean updateArray(int mRow) { return true; }

  
  /**
   * Compares this entry with another one (for sorting).
   * If the data-objects implement the Comparable-interface the compareTo
   * method of the data-objects will be used.
   * Otherwise the string-represenation (from toString()) will be compared.
   * This default behaviour is ok for most applications.
   *
   * @param entry to be compared against this entry
   * @param compareBy is an array of mColumns where 0 is the first and
   *        negative minus 1 means descending. I.e.: [-1, 2] means:
   *        first column descending, third ascending
   *
   * @return a positive integer if this entry is logically "larger" than the
   *         given entry. A negative if "smaller" and zero if "equal".
   */

  @SuppressWarnings("unchecked")
  public int compareTo (FormTableEntry entry, int[] compareBy)  {
    int rv = 0;
    for (int i=0; rv == 0 && i < compareBy.length; i++) {
      int col = compareBy[i];            // index for column
      boolean descending = false;        // false = ascending
      if (col < 0)  {
        col = -col - 1;
        descending = true;
      }
      try {
        // try Comparable first (works in most cases)
        rv = Compare.compare((Comparable<Object>)getValueAt(col), (Comparable<Object>)entry.getValueAt(col));        
      }
      catch (Exception e) {
        Object o1 = getValueAt(col);
        Object o2 = entry.getValueAt(col);
        String s1 = o1 != null ? o1.toString() : null;
        String s2 = o2 != null ? o2.toString() : null;
        rv = Compare.compare(s1, s2);
      }
      if (descending) {
        rv = -rv;
      }
    }
    return rv;
  }
  
  
  
  /**
   * Compares what the user sees on the GUI between two HistoryTableEntries.
   * This is in order to suppress invisible changes (i.e. editedBy, editedSince)
   *
   * @param entry to be compared against this entry
   * @return true if equal.
   */
  public boolean isVisiblyEqual(FormTableEntry entry)  {
    for (int col = getColumnCount() - 1; col >= 0; --col) {
      Object o1 = getValueAt(col);
      Object o2 = entry.getValueAt(col);
      String s1 = o1 != null ? o1.toString() : null;
      String s2 = o2 != null ? o2.toString() : null;
      if (Compare.compare(s1, s2) != 0) {
        return false;
      }      
    }
    return true;
  }
    

  /**
   * Gets the cell renderer for a given column.<br>
   * Depending on isCellRendererFixed() this method is invoked
   * only once per column or for each cell.
   * The default implementation returns null, i.e. a
   * default renderer depending on the class is used.
   *
   * @param mColumn the datamodel-column
   * @return the renderer or null if default
   */
  public TableCellRenderer getCellRenderer(int mColumn) { return null; }

  
  /**
   * Determines whether the cell is editable or not.
   * The default is not editable.
   *
   * @param mColumn the datamodel-column
   * @return true if the cell is editable.
   */
  public boolean isCellEditable(int mColumn) { return false; }
  
  /**
   * Gets the cell editor for a given column.
   * Depending on isCellEditorFixed() this method is invoked
   * only once per column or for each cell.
   * Furthermore the cell must be editable.
   * The default implementation returns null, i.e. a
   * default editor depending on the class is used.
   *
   * @param mColumn the datamodel-column
   * @return the editor or null if default
   */
  public TableCellEditor getCellEditor(int mColumn) { return null; }
  
  
  
  // ------------- methods that apply to the table and optionally to the entry ----------
  
  
  /**
   * Gets the cellrect for a column.<br>
   * The method is only invoked if isCellRectFixed() returns false.
   * <p>
   * Note:
   * Usually tables with dynamic cell sizes (i.e. multispan columns)
   * don't allow the user to change column ordering and/or sort rows.
   * For optimization reasons the given row and column are according
   * to the view which is usually is identical to the model.
   * If this is not the case (which is hard to handle btw.) getCellRect
   * must convert the row and column to the datamodel.
   * 
   * @param vRow the row in table view
   * @param vColumn the column in table view
   * @param includeSpacing is true to include margins
   * @return the cellrect for a cell, null if use default
   */
  public Rectangle getCellRect(int vRow, int vColumn, boolean includeSpacing) { return null; }
  
  /**
   * Determines whether the cell is visible or not.<br>
   * The method is only invoked if isCellRectFixed() returns false.
   * Invisible cells are not rendered.
   * Overwriting this method usually makes sense
   * if cells are merged depending on the data, i.e. the
   * merged cells should not be rendered.
   *
   * The default is visible.
   *
   * @param vRow the row in table view
   * @param vColumn the column in table view
   * @return true if the cell is visible.
   */
  public boolean isCellVisible(int vRow, int vColumn) { return true; }
  
  /**
   * Gets the referenced row if cells are merged.<br>
   * If cells are merged they must reference to valid cell.
   * The method is only invoked if isCellRectFixed() returns false.
   * 
   * @param vRow the row in table view
   * @param vColumn the column in table view
   * @return the referenced row for this cell
   */
  public int getReferencedRow(int vRow, int vColumn) { return vRow; }
  
  /**
   * Gets the referenced column if cells are merged.<br>
   * If cells are merged they must reference to valid cell.
   * The method is only invoked if isCellRectFixed() returns false.
   * 
   * @param vRow the row in table view
   * @param vColumn the column in table view
   * @return the referenced column for this cell
   */
  public int getReferencedColumn(int vRow, int vColumn) { return vColumn; }
  
  /**
   * Determines whether the horizontal grid line following the
   * given row should be drawn or not.
   * The method is only invoked if isCellRectFixed() returns false.
   *
   * @param vRow the row in table view
   * @return true if draw horizontal grid
   */
  public boolean getShowHorizontalLine(int vRow) { return true; }
  
  /**
   * Determines whether the vertical grid line following the
   * given cell should be drawn or not.
   * The method is only invoked if isCellRectFixed() returns false.
   *
   * @param vRow the row in table view
   * @param vColumn the column in table view
   * @return true if draw horizontal grid
   */
  public boolean getShowVerticalLine(int vRow, int vColumn) { return true; } 
 
  
  
  // ------------------------------- utility methods --------------------------------
  
  /**
   * Fires an update of all cells in the current row.
   */
  public void fireRowUpdated() {
    getModel().fireTableRowsUpdated(row, row);
  }
  
  /**
   * Fires an update of the given cells in current row.
   *
   * @param mColumns columns with respect to the model
   */
  public void fireCellsUpdated(int... mColumns) {
    for (int col: mColumns) {
      getModel().fireTableCellUpdated(row, col);
    }
  }
  
}
