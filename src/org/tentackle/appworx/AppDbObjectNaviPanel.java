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

// $Id: AppDbObjectNaviPanel.java 465 2009-07-19 15:18:09Z harald $

package org.tentackle.appworx;


import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormButton;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormTable;
import org.tentackle.ui.FormTableEntry;
import org.tentackle.ui.FormTableModel;
import org.tentackle.ui.FormTableSorter;
import org.tentackle.ui.FormTableUtilityPopup;
import org.tentackle.ui.FormTree;
import org.tentackle.ui.SumFormTableEntry;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;



/**
 * Panel for object navigation.
 * <p>
 * The {@code AppDbObjectNaviPanel} provides object navigation in a tree
 * and a table in parallel. The user switches between the tree- and the
 * table view. The tree view is primarily for navigation through object
 * relations and operations on objects by means of a context-driven popup menu.
 * The table view is for sorting, building sums, cutting the list
 * and a table popup to export to spreadsheet, xml or print the table.
 * Furthermore, other navigation panels may be created on-the-fly,
 * for example to sum up the details (childs) of a given type for
 * some object within a tree.
 * 
 * @author harald
 */
public class AppDbObjectNaviPanel extends org.tentackle.ui.FormPanel implements KeyListener {
  
  // Buttonmodes
  /** show cancel button **/
  public static final int SHOW_CANCEL   = 0x01;
  /** show select button **/
  public static final int SHOW_SELECT   = 0x02;
  /** show close button **/
  public static final int SHOW_CLOSE    = 0x04;
  /** show default buttons: cancel and select **/
  public static final int SHOW_BUTTONS  = SHOW_CANCEL | SHOW_SELECT;
  /** don't show any button **/
  public static final int SHOW_NOBUTTON = 0x00;
  
  // action commands
  /** "select" action **/
  public static final String ACTION_SELECT = "select";
  /** "cancel" action **/
  public static final String ACTION_CANCEL = "cancel";
  /** "close" action **/
  public static final String ACTION_CLOSE  = "close";

  // view mode
  /** current view (don't change) **/
  public static final int VIEW_CURRENT = 0;
  /** switch to tree view **/
  public static final int VIEW_TREE    = 1;
  /** switch to table/list view **/
  public static final int VIEW_LIST    = 2;
  
  
  private int viewMode;                        // one of VIEW_....
  private List<AppDbObject> list;              // object list
  private Class[] selectClasses;               // one of AppDbObject...
  private int buttonMode;                      // one of SHOW_....
  private AppDbObject selectedObject;          // the selected object, null=none
  private List<AppDbObject> selectedObjects;   // List of selected objects if multi-selections
  private boolean disposeKeyEnabled;           // true if ESCAPE in tree or table disposes the dialog (default is false).
  private int treeSelectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
  private int listSelectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
  
  // the tree
  private AppDbObjectTree naviTree;            // navigation tree
  private boolean naviTreeUpdated;             // true if naviTree is up to date

  // the table
  private Class<? extends AppDbObject> tableClass;  // != null if table mode allowed, i.e. this is a AppDbObject.class
  private FormTable naviTable;                 // the table object
  private boolean naviTableUpdated;            // true if naviTable is up to date
  private FormTableSorter naviSorter;          // the table sorter
  private FormTableModel naviModel;            // the table model
  private boolean packed;                      // true if pack() already done
  private boolean autoPack = true;             // true = automatically pack container window (default)
  private String tableName;                    // non default tablename
  private String tableIntro;                   // intro text (usually selection criteria) if table is printed, null = none
  private String tableTitle;                   // title text, if table is printed, null = default from context
  
  

  /**
   * Creates a navigation panel.
   * 
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of SHOW_...
   * @param showTable true if initially show the table view, false = tree view
   * @param tableName the preferences tablename, null if preferences by getFormTableName() from 1st object in list
   */
  public AppDbObjectNaviPanel(List<? extends AppDbObject> list, Class[] selectClasses, int buttonMode, boolean showTable, String tableName)  {
    setup(list, selectClasses, buttonMode, showTable, tableName);
  }
  
  /**
   * Creates a navigation panel.<br>
   * The preferences table name is determined by the first object.
   * 
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of SHOW_...
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviPanel(List<? extends AppDbObject> list, Class[] selectClasses, int buttonMode, boolean showTable)  {
    this(list, selectClasses, buttonMode, showTable, null);
  }
  
  /**
   * Creates a navigation panel.<br>
   * The preferences table name is determined by the first object.
   * The default buttons are shown (select and cancel).
   * The initial view mode is tree-view.
   * 
   * @param list the list of objects
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviPanel(List<? extends AppDbObject> list, Class[] selectClasses) {
    this(list, selectClasses, SHOW_BUTTONS, false);
  }
  
  /**
   * Creates a navigation panel.<br>
   * The preferences table name is determined by the first object.
   * The default buttons are shown (select and cancel).
   * The initial view mode is tree-view.
   * Nothing to select.
   * 
   * @param list the list of objects
   */
  public AppDbObjectNaviPanel(List<? extends AppDbObject> list) {
    this(list, null, SHOW_BUTTONS, false);
  }  

  /**
   * Creates a navigation panel for a single object.<br>
   * 
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of SHOW_...
   * @param showTable true if initially show the table view, false = tree view
   * @param tableName the preferences tablename, null if preferences object
   */
  public AppDbObjectNaviPanel(AppDbObject obj, Class[] selectClasses, int buttonMode, boolean showTable, String tableName)  {
    List<AppDbObject> objList = new ArrayList<AppDbObject>();
    objList.add(obj);
    setup(objList, selectClasses, buttonMode, showTable, tableName);
  }
  
  /**
   * Creates a navigation panel for a single object.<br>
   * The preferences table name is determined by the object.
   * 
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   * @param buttonMode the visibility of buttons, one of SHOW_...
   * @param showTable true if initially show the table view, false = tree view
   */
  public AppDbObjectNaviPanel(AppDbObject obj, Class[] selectClasses, int buttonMode, boolean showTable)  {
    this(obj, selectClasses, buttonMode, showTable, null);
  }
  
  /**
   * Creates a navigation panel for a single object.<br>
   * The preferences table name is determined by the object.
   * The default buttons are shown (select and cancel).
   * The initial view mode is tree-view.
   * 
   * @param obj the database object
   * @param selectClasses the class allowed to select, null = nothing selectable (view only)
   */
  public AppDbObjectNaviPanel(AppDbObject obj, Class[] selectClasses) {
    this(obj, selectClasses, SHOW_BUTTONS, false);
  }
  
  /**
   * Creates a navigation panel for a single object.<br>
   * The preferences table name is determined by the object.
   * The default buttons are shown (select and cancel).
   * The initial view mode is tree-view.
   * Nothing to select.
   * 
   * @param obj the database object
   */
  public AppDbObjectNaviPanel(AppDbObject obj) {
    this(obj, null, SHOW_BUTTONS, false);
  }
  
  
  /**
   * Creates an empty navigation panel.
   * This constructor makes it a bean.
   */
  public AppDbObjectNaviPanel() {
    this(new ArrayList<AppDbObject>(), null, SHOW_BUTTONS, false, null);
  }

  
  
  
  /**
   * Sets the table's intro-text that appears on the first page of the printout,
   * usually the selection criteria from a qbfparameter.
   * Must be invoked before setObjects().
   * 
   * @param tableIntro the table intro text, null if none
   */
  public void setTableIntro(String tableIntro) {
    this.tableIntro = tableIntro; 
  }
  
  /**
   * Gets the table intro text.
   * 
   * @return the intro text, null if none
   */
  public String getTableIntro() {
    return tableIntro; 
  }
  
  
  /**
   * Sets the table's title-text that appears on the printout.
   * Must be invoked before setObjects().
   * 
   * @param tableTitle the table title, null if none
   */
  public void setTableTitle(String tableTitle) {
    this.tableTitle = tableTitle; 
  }
  
  /**
   * Gets the table's title.
   * 
   * @return the title, null if none
   */
  public String getTableTitle() {
    return tableTitle; 
  }
  
  
  /**
   * Gets the class of objects which is shown in the table.
   * 
   * @return the database object class, null if not determined yet
   */
  public Class<? extends AppDbObject> getTableClass()  {
    return tableClass; 
  }
  
  
  /**
   * Sets the classes of objects that are allowed to select.
   * <p>
   * If there are already objects selected their selections
   * will be cleared.
   * 
   * @param selectClasses the classes, null if none
   */
  public void setSelectClasses(Class[] selectClasses) {
    if (this.selectClasses != null) {
      clearSelection();
    }
    this.selectClasses = selectClasses;
  }
  
  /**
   * Gets the classes of objects that are allowed to select.
   * 
   * @return the classes, null if none
   */
  public Class[] getSelectClasses() {
    return selectClasses;
  }
  
  

  /**
   * Allow/disallow popup-menu in the tree view.
   * 
   * @param enabled true if allow popup menu (default)
   */
  public void setPopupEnabled(boolean enabled)  {
    naviTree.setPopupEnabled(enabled);
  }

  /**
   * Returns whether popup menu is allowed in the tree view.
   * 
   * @return true if allowed (default)
   */
  public boolean isPopupEnabled() {
    return naviTree.isPopupEnabled();
  }
  
  
  /**
   * Requests the focus for the first object,
   * whether list- or tree-view.
   */
  public void requestFocusForFirstItem()  {
    if (list != null && !list.isEmpty()) {
      if (viewMode == VIEW_LIST) {
        naviTable.setSelectedRow(0);
        naviTable.scrollToCell(0, 0);
        FormHelper.requestFocusLater(naviTable);
      }
      else if (viewMode == VIEW_TREE) {
        naviTree.requestFocusForFirstItem();
      }
    }
  }
  
  
  /**
   * Scrolls to the first object, whether
   * list- or tree-view.
   */
  public void scrollToFirstItem()  {
    if (list != null && !list.isEmpty()) {
      if (naviTable != null) {
        naviTable.scrollToCell(0, 0);
      }
       if (naviTree != null) {
        naviTree.scrollRowToVisible(0);
      }
    }
  }
  
  
  /**
   * Gets the current list of objects shown.
   * @return the list of objects
   */
  public List<? extends AppDbObject> getObjects()  {
    return list;
  }
  
  
  /**
   * Replaces the current list of objects and updates the view.
   *
   * @param list the list of objects
   * @param viewMode is {@link #VIEW_CURRENT} to keep current view, else {@link #VIEW_TREE} or {@link #VIEW_LIST}
   *        to rebuild the view as a tree or table
   */
  @SuppressWarnings("unchecked")
  public void setObjects(List<? extends AppDbObject> list, int viewMode) {
    
    this.list = (List<AppDbObject>) list;
    cutSelectedButton.setEnabled(false);
    
    if (list == null || list.isEmpty()) {
      treeViewButton.setEnabled(false);
      tableViewButton.setEnabled(false);
    }
    else  {
      if (tableClass == null) {
        // not yet determined
        determineTableClass(viewMode == VIEW_LIST);
      }
      treeViewButton.setEnabled(true);
      tableViewButton.setEnabled(true);
    }
    
    clearSelection();
    
    // if nothing is already displayed and viewMode is CURRENT: default to TREE
    if (this.viewMode == VIEW_CURRENT && viewMode == VIEW_CURRENT) {
      viewMode = VIEW_TREE;
    }
    
    if (viewMode != VIEW_CURRENT) {
      showView(viewMode, true);
      if (naviSorter != null && sumButton.isVisible() && sumButton.isSelected()) {
        naviSorter.setSumEntry(new SumFormTableEntry(naviModel));
      }
    }
    else  {
      listUpdated();
      scrollToFirstItem();
    }
  }


  /**
   * Replaces the current list of objects and updates the view.
   *
   * @param list the list of objects
   * @param rebuildView is true to rebuild the view, false if keep it
   */ 
  public void setObjects(List<? extends AppDbObject> list, boolean rebuildView) {
    setObjects(list, rebuildView ? viewMode : VIEW_CURRENT);
  }
  
  /**
   * Replaces the current list of objects and updates the view.
   *
   * @param list the list of objects
   */ 
  public void setObjects(List<? extends AppDbObject> list) {
    setObjects(list, VIEW_CURRENT);
  }

  
  /**
   * Gets the current viewmode
   * 
   * @return one of {@code VIEW_....} (CURRENT means: nothing at all)
   */
  public int getViewMode() {
    return viewMode;
  }
  
  
  
  /**
   * Gets the pack mode.
   * 
   * @return true if parent window is packed whenever the view is updated, default is false
   */
  public boolean isAutoPack() {
    return autoPack;
  }

  /**
   * Sets the pack mode.
   * 
   * @param autoPack true to pack the parent window if view is updated, default is false
   */
  public void setAutoPack(boolean autoPack) {
    this.autoPack = autoPack;
  }
  
  
  /**
   * Sets the button mode.
   * 
   * @param buttonMode sets the buttons (see {@code SHOW_...} above)
   */
  public void setButtonMode(int buttonMode) {
    this.buttonMode = buttonMode;
    selectButton.setVisible((buttonMode & SHOW_SELECT) != 0);
    cancelButton.setVisible((buttonMode & SHOW_CANCEL) != 0);
    closeButton.setVisible((buttonMode & SHOW_CLOSE) != 0);
  }
  
  /**
   * Gets the button mode.
   * 
   * @return the buttonMode (see {@code SHOW_...} above)
   */
  public int getButtonMode()  {
    return buttonMode;
  }
  
  
  
  /**
   * Sets the visibility of the cancel button.
   *
   * @param visible is true if visible, false if not
   */
  public void setCancelButtonVisible(boolean visible) {
    buttonMode = visible ? (buttonMode | SHOW_CANCEL) : (buttonMode & ~SHOW_CANCEL);
    cancelButton.setVisible(visible);
  }
  
  /**
   * Gets the visibility of the cancel button.
   *
   * @return true if visible, false if not
   */
  public boolean isCancelButtonVisible() {
    return (buttonMode & SHOW_CANCEL) != 0;
  }
  
  
  /**
   * Sets the visibility of the select button.
   *
   * @param visible is true if visible, false if not
   */
  public void setSelectButtonVisible(boolean visible) {
    buttonMode = visible ? (buttonMode | SHOW_SELECT) : (buttonMode & ~SHOW_SELECT);
    selectButton.setVisible(visible);
  }
  
  /**
   * Gets the visibility of the select button.
   *
   * @return true if visible, false if not
   */
  public boolean isSelectButtonVisible() {
    return (buttonMode & SHOW_SELECT) != 0;
  }
  
  
  /**
   * Sets the visibility of the select button.
   *
   * @param visible is true if visible, false if not
   */
  public void setCloseButtonVisible(boolean visible) {
    buttonMode = visible ? (buttonMode | SHOW_CLOSE) : (buttonMode & ~SHOW_CLOSE);
    closeButton.setVisible(visible);
  }
  
  /**
   * Gets the visibility of the select button.
   *
   * @return true if visible, false if not
   */
  public boolean isCloseButtonVisible() {
    return (buttonMode & SHOW_CLOSE) != 0;
  }
  
  
  

  /**
   * setup panel
   *
   * @param list is the initial list of objects to be shown
   * @param selectClasses are the classes allowed to be selected
   * @param buttonMode tells what buttons are visible/invisible (one of SHOW_...)
   * @param showTable is true if initially show the table view, false = tree view
   * @param tableName is != null if preferences not by getFormTableName() from 1st object in list
   */
  @SuppressWarnings("unchecked")
  public void setup (List<? extends AppDbObject> list, Class[] selectClasses, int buttonMode, boolean showTable, String tableName)  {

    this.list           = (List<AppDbObject>) list;
    this.selectClasses  = selectClasses;
    this.tableName      = tableName;
    
    // create empty tree
    naviTree = new AppDbObjectTree();
    naviTree.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        naviTree_actionPerformed(e);
      }
    });
    
    // add listeners
    naviTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        naviTree_valueChanged(e);
      }
    });
    naviTree.getSelectionModel().setSelectionMode(treeSelectionMode);

    // create empty table
    naviTable = new FormTable();
    naviTable.setDragEnabled(true);
    naviTable.setAutoResizeMode(FormTable.AUTO_RESIZE_OFF);
    naviTable.setCreateDefaultColumnsFromPreferences(true);
    naviTable.setEnterActionEnabled(true);
    
    // add Listeners
    naviTable.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        naviTable_actionPerformed(e);
      }
    });
    
    naviTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
          naviTable_valueChanged(e);
        }
      }
    });

    naviTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      public void columnAdded(TableColumnModelEvent e) {
        updateSumButton();
      }
      public void columnMarginChanged(ChangeEvent e) {
      }
      public void columnMoved(TableColumnModelEvent e) {
      }
      public void columnRemoved(TableColumnModelEvent e) {
        updateSumButton();
      }
      public void columnSelectionChanged(ListSelectionEvent e) {
      }
    });

    // same control-keys as in tree
    naviTable.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e)  {
        int keyCode = e.getKeyCode();
        AppDbObject obj = getFirstSelectedObjectInTable();
        if (obj != null && e.isControlDown()) {
          switch (keyCode) {
            case KeyEvent.VK_C:   // copy to clipboard
              try {
                Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                AppDbObjectTransferable trans = new AppDbObjectTransferable(obj);
                clip.setContents(trans, trans);
              }
              catch (Exception ex) {
                // nothing to do
              }
              break;
            case KeyEvent.VK_E:   // edit
              editObjectInTable(true);
              break;
            case KeyEvent.VK_N:   // show
              editObjectInTable(false);
              break;
          }
          e.consume();
        }
      }
    });

    if (disposeKeyEnabled)  {
      naviTable.addKeyListener(this);
    }
    
    tableClass = null;
    viewMode   = VIEW_CURRENT;
    
    initComponents();

    if (selectClasses == null)  {   // only search, no select
      buttonMode = SHOW_NOBUTTON;
    }
    setButtonMode(buttonMode);
    
    selectedObject = null;
    selectedObjects = null;
    selectButton.setEnabled(false);       // noch nichts ausgewaehlt
    cutSelectedButton.setEnabled(false);
    sumButton.setVisible(false);
    
    // check whether table view is allowed
    determineTableClass(showTable);
    
    // setup view but don't pack initially (this must be done in Container)
    packed = true;
    showView(showTable ? VIEW_LIST : VIEW_TREE, false);
    packed = false;
  }

  
  

  /**
   * Gives access to cancel button (to modify the text, i.e.)
   * @return the cancel button
   */
  public FormButton getCancelButton()  {
    return cancelButton;
  }
  
  /**
   * Gives access to select button (to modify the text, i.e.)
   * @return the select button
   */
  public FormButton getSelectButton()  {
    return selectButton;
  } 
  
  
 
  
  /**
   * Sets the list selection mode.
   * The tree selection mode is updated accordingly.
   * 
   * @param mode the list selection mode from {@link ListSelectionModel}
   * @see ListSelectionModel
   * @see TreeSelectionModel
   */
  public void setListSelectionMode(int mode)  {

    listSelectionMode = mode;
    
    switch(mode)  {
      case ListSelectionModel.SINGLE_SELECTION: 
        treeSelectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION;
        break;
      case ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:
        treeSelectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
        break;
      case ListSelectionModel.SINGLE_INTERVAL_SELECTION:
        treeSelectionMode = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
        break;
    }
    
    applySelectionMode();
  }
  
  
  
  /**
   * Sets the tree selection mode.
   * The list selection mode is updated accordingly.
   * 
   * @param mode the list selection mode from {@link TreeSelectionModel}
   * @see ListSelectionModel
   * @see TreeSelectionModel
   */
  public void setTreeSelectionMode(int mode)  {

    treeSelectionMode = mode;
    
    switch(mode)  {
      case TreeSelectionModel.SINGLE_TREE_SELECTION:
        listSelectionMode = ListSelectionModel.SINGLE_SELECTION;
        break;
      case TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION:
        listSelectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
        break;
      case TreeSelectionModel.CONTIGUOUS_TREE_SELECTION:
        listSelectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION;
        break;
    }
 
    applySelectionMode();
  }
  
  
  
  /**
   * Gives access to the navigation tree.
   * 
   * @return the tree, null if not yet created
   */
  public AppDbObjectTree getNaviTree()  {
    return naviTree;
  }

  
  /**
   * Gives access to the table list.
   * 
   * @return the table, null if not yet created
   */
  public FormTable getNaviTable() {
    return naviTable;
  }
  
  
  
  /**
   * Clears the selections.
   */
  public void clearSelection() {
    if (naviTable != null)  {
      naviTable.clearSelection();
    }
    if (naviTree != null) {
      naviTree.clearSelection();
    }
  }
  
  
  /**
   * Notifies the navigation panel that the list of objects or objects
   * within the list have changed and the view needs to be updated.
   */
  public void listUpdated() {
    if (viewMode == VIEW_LIST)  {
      naviModel.listChanged(list);
      naviTableUpdated = true;
      naviTreeUpdated  = false;
      SumFormTableEntry sumEntry = (SumFormTableEntry)naviSorter.getSumEntry();
      if (sumEntry != null) {
        sumEntry.sumUp(naviModel);          // sumup again
        naviSorter.setSumEntry(sumEntry);   // causes update
      }
    }
    else if (viewMode == VIEW_TREE) {
      buildTree();
      naviTableUpdated = false;
      naviTreeUpdated  = true;
    }
    updateRowCountLabel();
    updateSumButton();
  }
  
  
  
  /**
   * Adds a selection for a given range of objects.
   * 
   * @param firstIndex the index of the first object in the current list of objects
   * @param lastIndex the index of the last object in the current list of objects
   */
  public void addSelectionInterval(int firstIndex, int lastIndex) {
    if (naviSorter != null)  {
      for (int i=firstIndex; i <= lastIndex; i++) {
        int row = naviSorter.getMappedIndex(i);
        naviTable.getSelectionModel().addSelectionInterval(row, row);  
      }      
    }
    for (int i=firstIndex; i <= lastIndex; i++) {
      naviTree.addSelectionRow(i);
    }
  }
  
  
  /**
   * Removes a selection for a given range of objects.
   * 
   * @param firstIndex the index of the first object in the current list of objects
   * @param lastIndex the index of the last object in the current list of objects
   */
  public void removeSelectionInterval(int firstIndex, int lastIndex) {
    if (naviSorter != null)  {
      for (int i=firstIndex; i <= lastIndex; i++) {
        int row = naviSorter.getMappedIndex(i);
        naviTable.getSelectionModel().removeSelectionInterval(row, row);  
      }      
    }
    for (int i=firstIndex; i <= lastIndex; i++) {
      naviTree.removeSelectionRow(i);
    }
  }  
  
  
  
  /**
   * Scrolls to object at given index.
   * 
   * @param index the index of the object in the current list of objects
   */
  public void scrollToIndex(int index)  {
    if (viewMode == VIEW_LIST) {
      if (naviSorter != null) {
        naviTable.scrollToCell(naviSorter.getMappedIndex(index), 0);
        naviTable.requestFocusInWindow();
      }
    }
    else if (viewMode == VIEW_TREE) {
      naviTree.scrollRowToVisible(index);
      naviTree.requestFocusInWindow();
    }
  }
  
  
  
  /**
   * Sets whether the cancel/dispose-key is enabled.<br>
   * If this key is pressed when tree or table has the focus
   * the enclosing window is disposed.
   *
   * @param enabled true to enable, false to disable
   */
  public void setDisposeKeyEnabled(boolean enabled)  {
    if (enabled != disposeKeyEnabled)  {
      if (!enabled && disposeKeyEnabled)  {
        // unregister 
        naviTree.removeKeyListener(this);
        naviTable.removeKeyListener(this);
      }
      else if (enabled) {
        // register new listener
        naviTree.addKeyListener(this);
        naviTable.addKeyListener(this);
      }
      disposeKeyEnabled = enabled;
    }
  }
  
  /**
   * Returns whether the dispose key is enabled.
   * 
   * @return true if enabled
   */
  public boolean isDisposeKeyEnabled()  {
    return disposeKeyEnabled;
  }
  
  
  
  
  
  /**
   * Adds a selection changed listener.
   * 
   * @param listener the listener to add
   */
  public synchronized void addListSelectionListener (ListSelectionListener listener)  {
    listenerList.add (ListSelectionListener.class, listener);
  }

  /**
   * Removes a selection changed listener.
   * 
   * @param listener the listener to remove
   */
  public synchronized void removeListSelectionListener (ListSelectionListener listener) {
     listenerList.remove (ListSelectionListener.class, listener);
  }

  

  /**
   * Gets the selected object.
   * If there are more than one object in the current list of selections,
   * the object "clicked last" is returned.
   *
   * @return the object, null if nothing selected
   */
  public AppDbObject getSelectedObject () {
    return selectedObject;
  }

  /**
   * Gets all selected objects.
   *
   * @return the list of selected objects, null if nothing selected
   */
  public List<AppDbObject> getSelectedObjects () {
    return selectedObjects;
  }

  
  
  /**
   * determine the table class
   */
  private void determineTableClass(boolean showTable) {
    
    // check whether table view is allowed
    if (list != null) {
      // setup tableclass, if all object are of the same class
      try {
        for (AppDbObject object: list) {
          if (tableClass == null) {
            tableClass = object.getClass(); 
          }
          else  {
            if (tableClass.equals(object.getClass()) == false)  {
              tableClass = null;
              break; 
            }
          }
        }
      }
      catch (Exception e) {
        tableClass = null;
      }
    }
    
    if (tableClass != null) {
      viewButtonPanel.setVisible(true);
      if (showTable) {
        tableViewButton.setSelected(true);
        treeViewButton.setSelected(false);
      }
      else {
        treeViewButton.setSelected(true);
        tableViewButton.setSelected(false);
      }
    }
    else  {
      viewButtonPanel.setVisible(false);
    }
  }
  
  
  
  /**
   * Updates the row count label
   */
  private void updateRowCountLabel()  {
    if (list != null) {
      rowCountLabel.setText("" + list.size());
    }
    else  {
      rowCountLabel.setText(null);
    }
  }

  
  /**
   * applis the selection mode for both table and tree
   */
  private void applySelectionMode() {
    naviTable.setSelectionMode(listSelectionMode);
    naviTree.getSelectionModel().setSelectionMode(treeSelectionMode);
    cutSelectedButton.setVisible(listSelectionMode != ListSelectionModel.SINGLE_SELECTION);    
  }
  

  /**
   * notify all ActionListeners (usually only one!) that the field is
   * going to be displayed and thus needs the data to display
   */
  private void fireActionPerformed (String action) {
    fireActionPerformed(new ActionEvent (this, ActionEvent.ACTION_PERFORMED, action));
  }

  
  /**
   * notifies all Listeners that the selection has changed
   */
  private void fireValueChanged()  {
    if (viewMode == VIEW_TREE || viewMode == VIEW_LIST) {
      Object[] listeners = listenerList.getListenerList();
      ListSelectionEvent e = null;

      for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ListSelectionListener.class) {
          if (e == null) {
            e = new ListSelectionEvent(viewMode == VIEW_TREE ? naviTree : naviTable, 0, selectedObjects == null ? -1 : selectedObjects.size()-1, false);
          }
          ((ListSelectionListener)listeners[i+1]).valueChanged(e);
        }
      }
    }
  }
  

  /**
   * adds object to selection.
   */
  private boolean addToSelectedObjects(AppDbObject obj) {
    if (isObjectAllowed(obj)) {
      // ok zum Auswaehlen, jeweils das letzte ist das aktuelle
      selectedObject = obj;
      if (selectedObjects == null) {
        selectedObjects = new ArrayList<AppDbObject>();
      }
      selectedObjects.add(selectedObject);
      return true;
    }
    return false;
  }

  
  
  /**
   * checks whether the selected object is allowed for selections
   */
  private boolean isObjectAllowed(Object obj)  {
    if (selectClasses != null)  {
      for (int i=0; i < selectClasses.length; i++)  {
        if (selectClasses[i].isInstance(obj)) {
          return true;
        }
      }
    }
    return false;
  }

  
  
  private void naviTree_actionPerformed(ActionEvent e)  {
    // double click
    if (selectedObject != null && selectButton.isVisible() && selectButton.isEnabled()) {
      selectButton.doClick();
    }
    else if (e.getActionCommand().equals(FormTree.ENTER_ACTION)) {
      naviTree.showPopup();   // show at least the popup menu (for those users that don't know how to get it)
    }
  }

  private void naviTree_valueChanged(TreeSelectionEvent e) {

    // ausgewaehltes Element ermitteln
    TreePath[] paths = naviTree.getSelectionPaths();
    
    selectedObject  = null;
    selectedObjects = null;
    int enableCut   = 0;
    
    if (paths != null)  {
      for (int i=0; i < paths.length; i++)  {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)(paths[i].getLastPathComponent());
        Object obj = ((AppDbTreeObject)node.getUserObject()).getObject();
        if (obj instanceof AppDbObject) {
          addToSelectedObjects((AppDbObject)obj);
          if (paths[i].getPathCount() == 2) { // root is always skipped
            enableCut++;
          }
        }
      }
    }
    cutSelectedButton.setEnabled(enableCut > 1);
    fireValueChanged();
    selectButton.setEnabled(selectedObject != null);      
  }



  private int findRowInModel(AppDbObject object) {
    return object == null ? -1 : list.indexOf(object);
  }

  private AppDbObject getFirstSelectedObjectInTable() {
    int row = naviTable.getSelectedRow();
    return row >= 0 ? list.get(naviSorter.getModelIndex(row)) : null;
  }


  
  private void editObjectInTable(boolean allChangeable) {
    AppDbObject obj = getFirstSelectedObjectInTable();
    if (obj != null) {
      if (FormHelper.isParentWindowModal(this)) {
        Hook.hook().editModal(obj);
      }
      else  {
        // non-modal
        final AppDbObjectDialog d = Hook.hook().getDialogPool().useNonModalDialog(obj, allChangeable, true);
        d.setObjectList(list);
        d.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == AppDbObjectDialog.ACTION_SAVE) {
              AppDbObject obj = d.getLastObject();
              // reload to get the real data stored in DB (in case of CANCEL)
              if (!obj.isNew()) {
                obj = (AppDbObject)obj.reload();
              }
              if (obj != null) {
                int modelRow = findRowInModel(obj);
                list.set(modelRow, obj);
                naviModel.listUpdated(modelRow, modelRow);
              }
            }
            else if (e.getActionCommand() == AppDbObjectDialog.ACTION_DELETE) {
              AppDbObject obj = d.getLastObject();
              if (obj != null) {
                int modelRow = findRowInModel(obj);
                naviModel.listDeleted(modelRow, modelRow);
                list.remove(modelRow);
              }
            }
            else if (e.getActionCommand() == AppDbObjectDialog.ACTION_PREVIOUS) {
              int modelRow = findRowInModel(d.getLastObject());
              if (modelRow > 0) {
                naviTable.setSelectedRow(modelRow - 1);
              }
            }
            else if (e.getActionCommand() == AppDbObjectDialog.ACTION_NEXT) {
              int modelRow = findRowInModel(d.getLastObject());
              if (modelRow >= 0 && modelRow < list.size() - 1) {
                naviTable.setSelectedRow(modelRow + 1);
              }
            }
          }
        });
      }
    }
  }
  
  
  private void naviTable_actionPerformed(ActionEvent e)  {
    // double click or ENTER
    if (selectClasses != null) {
      if (selectedObject != null) {
        selectButton.doClick();
      }
    }
    else  {
      // no select button: we are usually in a non-modal search dialog.
      // try to edit the object in a AppDbObjectDialog.
      editObjectInTable(true);
    }
  }
  

  private void naviTable_valueChanged(ListSelectionEvent e) {
    // ausgewaehlte Elemente ermitteln
    int[] rows = naviTable.getSelectedRows();
    
    selectedObject  = null;
    selectedObjects = null;
    int enableCut   = 0;
    
    if (rows != null)  {
      for (int i=0; i < rows.length; i++)  {
        int row = naviSorter.getModelIndex(rows[i]);
        if (row >= 0 && row < list.size()) { 
          addToSelectedObjects(list.get(row));
          enableCut++;
        }
      }
    }
    cutSelectedButton.setEnabled(enableCut > 1);
    fireValueChanged();
    selectButton.setEnabled(selectedObject != null);
  }

  
  
  private void updateSumButton()  {
    // check if at least one column can be numeric
    boolean sumUpEnabled = false;
    if (list != null && list.isEmpty() == false) {
      if (naviModel != null)  {
        FormTableEntry template = naviModel.getTemplate();
        int cols = template.getColumnCount();
        for (int col=0; col < cols; col++)  {
          if (naviTable.isColumnVisible(col) && !template.isColumnNotSummable(col))  {
            sumUpEnabled = true;
            break;
          }
        }
        if (!sumUpEnabled && naviSorter.getSumEntry() != null)  {
          naviSorter.setSumEntry(null);
          sumButton.setSelected(false);
        }
      }
    }
    sumButton.setVisible(sumUpEnabled); 
  }
  
  
  
  
  /**
   * Builds up the new tree
   */
  private void buildTree() {
    if (naviSorter != null && naviSorter.isSorted())  {
      // build sorted list
      List<AppDbObject> sortedList = new ArrayList<AppDbObject>();
      int size = list == null ? 0 : list.size();
      for (int i=0; i < size; i++)  {
        FormTableEntry entry = naviSorter.getEntryAt(i);
        if (entry != null)  {   // if not the sumEntry
          sortedList.add((AppDbObject)entry.getObject());
        }
      }
      naviTree.buildTree(sortedList);   // unsorted
    }
    else  {
      naviTree.buildTree(list);   // unsorted
    }
  }
  
  
  
  
  
  
  /**
   * updates the view
   * 
   * @param mode one of VIEW_...
   * @param rebuildView true if rebuild the view always
   */
  private void showView(int mode, boolean rebuildView)  {
    
    if (list != null && list.size() > 0 && (rebuildView || viewMode != mode)) { // view changed
      
      boolean scrollToFirstSelection = false;    // true if a new view (table or tree) has been created
      
      if (mode == VIEW_LIST) {    // switch to table view
        
        if (naviModel == null || rebuildView)  {    // initialize view
          
          AppDbObject obj = list.get(0);
          naviModel  = new FormTableModel(obj.getFormTableEntry());
          naviSorter = new FormTableSorter(naviModel);
          naviTable.setName(tableName == null ? obj.getFormTableName() : tableName);
          
          String title = tableTitle;
          if (title == null) {
            title = obj.getMultiName();
            ContextDb contextDb = obj.getBaseContext();
            if (contextDb != null) {
              String contextName = contextDb.toString();
              if (contextName != null && contextName.length() > 0)  {
                title = MessageFormat.format(Locales.bundle.getString("{0}_in_{1}"), title, contextName);
              }
            }
          }
          
          naviTable.createDefaultColumnsFromModel();    // create column width from preferences. table height see below
          
          new FormTableUtilityPopup(naviTable, title, tableIntro);
          
          // switch to table view
          naviScroll.setViewportView(naviTable);
          naviModel.listChanged(list);
          naviTable.setSelectionMode(listSelectionMode);
          naviTable.setModel(naviSorter);
          naviTableUpdated = true;
          scrollToFirstSelection = true;
          
          // check size if (use from preferences if available)
          int width = (int)(naviTable.getPreferredSize().getWidth() + naviScroll.getVerticalScrollBar().getPreferredSize().getWidth()) + 4;
          Dimension size = naviScroll.getPreferredSize();
          Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
          if (width > screenSize.width - 50) {
            width = screenSize.width - 50; // align
          }
          if (size.height < 50) {
            size.height = screenSize.height >> 1;
          }
          size.width = width;
          naviScroll.setPreferredSize(size);
          
          naviSorter.addPropertyChangeListener(new PropertyChangeListener()  {
            public void propertyChange(PropertyChangeEvent e) {
              if (e.getSource() instanceof FormTableSorter) {
                // set sorting string
                setSorting(((FormTableSorter)(e.getSource())).getSortNames());
                naviTreeUpdated = false;
              }
            }
          });
        }
        
        else  {
           // switch to table view
          if (naviTableUpdated == false)  {
            naviModel.listChanged(list);
            naviTableUpdated = true;
            naviTreeUpdated = false;
            scrollToFirstSelection = true;
          }
          naviScroll.setViewportView(naviTable);
        }
        
        
        // select objects in naviTable that are already selected in naviTree
        TreePath paths[] = naviTree.getSelectionPaths();
        
        naviTable.clearSelection();
        selectedObjects = null;
        selectedObject  = null;
        if (paths != null)  {
          for (int p=0; p < paths.length; p++)  {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)(paths[p].getLastPathComponent());
            // find index in object array
            Object so = ((AppDbTreeObject)node.getUserObject()).getObject();
            int i = naviSorter.getMappedIndex(list.indexOf(so));
            if (i >= 0) {
              naviTable.getSelectionModel().addSelectionInterval(i, i);
              if (scrollToFirstSelection) {
                naviTable.scrollToCell(i, 0);
                scrollToFirstSelection = false;
              }
            }
          }
        }
        
        if (scrollToFirstSelection) {
          // no previous selection: scroll to begin
          naviTable.scrollToCell(0, 0);
          scrollToFirstSelection = false;          
        }
        
        sortBox.setVisible(true);
        setSorting(naviSorter.getSortNames());
        
        updateSumButton();
      }
      
      else if (mode == VIEW_TREE) {
        
        // select objects in naviTree that are already selected in naviTable
        int[] rows = naviTable.getSelectedRows();
        
        naviTree.clearSelection();
        selectedObject  = null;
        selectedObjects = null;
        
        if (naviTreeUpdated == false || rebuildView) {
          buildTree();
          naviTreeUpdated = true;
          naviTableUpdated = false;
          scrollToFirstSelection = !list.isEmpty();
        }
        
        // switch back to treeView
        naviScroll.setViewportView(naviTree);
        
        // mark selected
        for (int i=0; i < rows.length; i++)  {
          Object obj = naviTable.getObjectAt(rows[i]);
          TreePath path = naviTree.findPathInCollection(obj);
          if (path != null) {
            naviTree.getSelectionModel().addSelectionPath(path);
          }
          if (scrollToFirstSelection) {
            naviTree.scrollPathToVisible(path);
            scrollToFirstSelection = false;
          }
        }
          
        if (scrollToFirstSelection) {
          // no previous selection: scroll to begin
          naviTree.scrollRowToVisible(0);
          scrollToFirstSelection = false;          
        }
        
        if (naviSorter != null && naviSorter.isSorted())  {
          sortBox.setVisible(true);
          sortBox.setEnabled(false);
        }
        else  {
          sortBox.setVisible(false);
        }
        sumButton.setVisible(false);
      }
      
      viewMode = mode;
      
      if (autoPack && isShowing() && !packed) {
        Window w = getParentWindow();
        if (w != null)  {
          w.pack();
          packed = true;
        }
      }
    }
    
    // set number of rows
    updateRowCountLabel();
  }
  

  
  /**
   * sets the current sorting string
   */
  private void setSorting(String text)  {
    if (text == null) {
      sortBox.setText(Locales.bundle.getString("unsorted"));
      sortBox.setSelected(false);
      sortBox.setEnabled(false);
    }
    else  {
      sortBox.setText(text);
      sortBox.setSelected(true);
      sortBox.setEnabled(true);
    }
  }


  
  
  // --------------- implemente KeyListener ---------------------------------
  
  public void keyTyped(KeyEvent e)  {}

  public void keyPressed(KeyEvent e)  {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      getParentWindow().dispose();
    }
  }

  public void keyReleased(KeyEvent e) {}
    
    
  

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroup1 = new javax.swing.ButtonGroup();
    viewButtonPanel = new org.tentackle.ui.FormPanel();
    tableViewButton = new javax.swing.JToggleButton();
    treeViewButton = new javax.swing.JToggleButton();
    sortBox = new org.tentackle.ui.FormRadioButton();
    jLabel1 = new javax.swing.JLabel();
    rowCountLabel = new javax.swing.JLabel();
    cutSelectedButton = new javax.swing.JButton();
    sumButton = new javax.swing.JToggleButton();
    naviScroll = new javax.swing.JScrollPane();
    buttonPanel = new javax.swing.JPanel();
    selectButton = new org.tentackle.ui.FormButton();
    cancelButton = new org.tentackle.ui.FormButton();
    closeButton = new org.tentackle.ui.FormButton();

    setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
    setLayout(new java.awt.BorderLayout());

    viewButtonPanel.setLayout(new java.awt.GridBagLayout());

    buttonGroup1.add(tableViewButton);
    tableViewButton.setIcon(PlafGlobal.getIcon("table"));
    tableViewButton.setToolTipText(Locales.bundle.getString("table_view")); // NOI18N
    tableViewButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    tableViewButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        tableViewButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
    viewButtonPanel.add(tableViewButton, gridBagConstraints);

    buttonGroup1.add(treeViewButton);
    treeViewButton.setIcon(PlafGlobal.getIcon("tree"));
    treeViewButton.setToolTipText(Locales.bundle.getString("tree_view")); // NOI18N
    treeViewButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    treeViewButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        treeViewButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
    viewButtonPanel.add(treeViewButton, gridBagConstraints);

    sortBox.setFont(new java.awt.Font("Dialog", 0, 12));
    sortBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        sortBoxActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    viewButtonPanel.add(sortBox, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.ipadx = 80;
    gridBagConstraints.weightx = 1.0;
    viewButtonPanel.add(jLabel1, gridBagConstraints);

    rowCountLabel.setText("0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    viewButtonPanel.add(rowCountLabel, gridBagConstraints);

    cutSelectedButton.setIcon(PlafGlobal.getIcon("cut"));
    cutSelectedButton.setToolTipText(Locales.bundle.getString("keep_selected_only")); // NOI18N
    buttonGroup1.add(cutSelectedButton);
    cutSelectedButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    cutSelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cutSelectedButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
    viewButtonPanel.add(cutSelectedButton, gridBagConstraints);

    sumButton.setIcon(PlafGlobal.getIcon("sum"));
    sumButton.setToolTipText(Locales.bundle.getString("compute_sums")); // NOI18N
    sumButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    sumButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        sumButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
    viewButtonPanel.add(sumButton, gridBagConstraints);

    add(viewButtonPanel, java.awt.BorderLayout.NORTH);
    add(naviScroll, java.awt.BorderLayout.CENTER);

    selectButton.setIcon(PlafGlobal.getIcon("ok"));
    selectButton.setMnemonic(Locales.bundle.getString("selectMnemonic").charAt(0));
    selectButton.setText(Locales.bundle.getString("select")); // NOI18N
    selectButton.setActionCommand(ACTION_SELECT);
    selectButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        selectButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(selectButton);

    cancelButton.setIcon(PlafGlobal.getIcon("cancel"));
    cancelButton.setMnemonic(Locales.bundle.getString("cancelMnemonic").charAt(0));
    cancelButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelButton.setActionCommand(ACTION_CANCEL);
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(cancelButton);

    closeButton.setIcon(PlafGlobal.getIcon("close"));
    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/tentackle/appworx/Locales"); // NOI18N
    closeButton.setText(bundle.getString("close")); // NOI18N
    closeButton.setActionCommand(ACTION_CLOSE);
    closeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        closeButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(closeButton);

    add(buttonPanel, java.awt.BorderLayout.SOUTH);
  }// </editor-fold>//GEN-END:initComponents

  private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    selectedObject  = null;
    selectedObjects = null;
    fireActionPerformed (evt.getActionCommand());
  }//GEN-LAST:event_closeButtonActionPerformed

  private void sumButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumButtonActionPerformed
    FormHelper.setWaitCursor(this);
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        if (sumButton.isSelected()) {

          naviSorter.setSumEntry(new SumFormTableEntry(naviModel));
          naviTable.scrollToCell(naviModel.getRowCount(), 0);
        }
        else  {
          naviSorter.setSumEntry(null);
        }
        FormHelper.setDefaultCursor(AppDbObjectNaviPanel.this);        
      }
    });
  }//GEN-LAST:event_sumButtonActionPerformed

  private void cutSelectedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutSelectedButtonActionPerformed
    // select objects in naviTree or naviTable (NOT from selectedObjects!)
    List<AppDbObject> objList = new ArrayList<AppDbObject>();
    if (viewMode == VIEW_LIST) {
      int[] rows = naviTable.getSelectedRows();
      if (rows != null)  {
        for (int i=0; i < rows.length; i++)  {
          int row = naviSorter.getModelIndex(rows[i]);
          if (row >= 0 && row < this.list.size()) { 
            objList.add(this.list.get(row));
          }
        }
      }      
    }
    else if (viewMode == VIEW_TREE) {
      TreePath[] paths = naviTree.getSelectionPaths();
      if (paths != null)  {
        for (int i=0; i < paths.length; i++)  {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)(paths[i].getLastPathComponent());
          Object obj = ((AppDbTreeObject)node.getUserObject()).getObject();
          if (paths[i].getPathCount() == 2) {
            objList.add((AppDbObject)obj);
          }
        }
      }
    }
    
    if (objList.size() > 0)  {
      setObjects(objList);
      naviTable.clearSelection();
      naviTree.clearSelection();
    }
  }//GEN-LAST:event_cutSelectedButtonActionPerformed

  private void sortBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortBoxActionPerformed
    if (sortBox.isSelected() == false) {
      naviSorter.clearSorting();
      naviSorter.sort();
      setSorting(null);
      naviTreeUpdated = false;
    }
  }//GEN-LAST:event_sortBoxActionPerformed

  private void treeViewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_treeViewButtonActionPerformed
    if (treeViewButton.isSelected())  {
      FormHelper.setWaitCursor(this);
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          showView(VIEW_TREE, false);  // switch to tree
          if (naviTree != null) {
            FormHelper.setDefaultCursor(AppDbObjectNaviPanel.this);
            naviTree.requestFocusInWindow();
          }
        }
      });
    }
  }//GEN-LAST:event_treeViewButtonActionPerformed

  private void tableViewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableViewButtonActionPerformed
    if (tableViewButton.isSelected()) {
      FormHelper.setWaitCursor(this);
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          showView(VIEW_LIST, false);    // switch to table view
          FormHelper.setDefaultCursor(AppDbObjectNaviPanel.this);
          naviTable.requestFocusInWindow();
        }
      });
    }
  }//GEN-LAST:event_tableViewButtonActionPerformed

  private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    selectedObject  = null;
    selectedObjects = null;
    fireActionPerformed (evt.getActionCommand());
  }//GEN-LAST:event_cancelButtonActionPerformed

  private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed
    fireActionPerformed (evt.getActionCommand());
  }//GEN-LAST:event_selectButtonActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup buttonGroup1;
  private javax.swing.JPanel buttonPanel;
  private org.tentackle.ui.FormButton cancelButton;
  private org.tentackle.ui.FormButton closeButton;
  private javax.swing.JButton cutSelectedButton;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JScrollPane naviScroll;
  private javax.swing.JLabel rowCountLabel;
  private org.tentackle.ui.FormButton selectButton;
  private org.tentackle.ui.FormRadioButton sortBox;
  private javax.swing.JToggleButton sumButton;
  private javax.swing.JToggleButton tableViewButton;
  private javax.swing.JToggleButton treeViewButton;
  private org.tentackle.ui.FormPanel viewButtonPanel;
  // End of variables declaration//GEN-END:variables
  
}

