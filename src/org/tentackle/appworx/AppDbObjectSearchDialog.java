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

// $Id: AppDbObjectSearchDialog.java 389 2008-08-15 19:17:22Z harald $
// Created on July 23, 2002, 3:33 PM

package org.tentackle.appworx;


import org.tentackle.db.DbCursor;
import org.tentackle.db.DbGlobal;
import org.tentackle.db.ModificationThread;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.ui.FormDialog;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormInfo;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.StringHelper;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.tree.TreeSelectionModel;
import org.tentackle.db.Db;




/**
 * A generic search dialog.
 * 
 * @author harald
 */
public class AppDbObjectSearchDialog extends FormDialog {
  

  /**
   * Creates a search dialog.
   * 
   * @param comp the component to determine the owner window, null if none
   * @param qbfPlugin the QBF plugin
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   * @return the search dialog
   */
  public static AppDbObjectSearchDialog createAppDbObjectSearchDialog (
                                  Component comp,
                                  QbfPlugin qbfPlugin,
                                  Class[] selectClasses, 
                                  boolean allowCreate, 
                                  boolean modal)  {
    
    return new AppDbObjectSearchDialog (FormHelper.getParentWindow(comp),
                                        qbfPlugin, selectClasses, allowCreate, modal);
  }
  
  /**
   * Creates a search dialog.
   * 
   * @param comp the component to determine the owner window, null if none
   * @param contextDb the database context
   * @param searchClass the object class'es table to search in, null = all tables
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   * @return the search dialog
   */
  public static AppDbObjectSearchDialog createAppDbObjectSearchDialog (
                                  Component comp,
                                  ContextDb contextDb, 
                                  Class<? extends AppDbObject> searchClass,
                                  Class[] selectClasses, 
                                  boolean allowCreate, 
                                  boolean modal) {
                                    
    return new AppDbObjectSearchDialog (FormHelper.getParentWindow(comp),
                                        contextDb, searchClass, selectClasses, allowCreate, modal);
  }
  
  
  
  
  private QbfPlugin               qbfPlugin;            // the QBF-Plugin according to searchClass
  private Class[]                 selectClasses;        // classes allowed for selection
  private boolean                 allowCreate;          // true if creating new is allowed
  private boolean                 multiSelection;       // true if modal and multiselections allowed
  private boolean                 autoSelectFirstItem;  // true if autoselect first item, default = true
  private boolean                 noShowIfSingle;       // true = don't show dialog if exactly single item matches (default = false)
  private boolean                 qbfSelection;         // true = use this dialog to create (and save) a query
  
  private QbfPanel                qbfPanel;             // panel from plugin
  private QbfParameter            qbfParameter;         // parameters from plugin
  private String                  morePattern;          // searchPattern im Suchergebnis
  private AppDbObject             selectedObject;       // returned selected object
  private List<AppDbObject>       selectedObjects;      // returned selected objects, if multiSelection
  private AppDbObjectNaviPanel    naviPanel;            // navigation panel
  private List<AppDbObject>       naviList;             // array of objects shown in naviPanel
  private boolean                 packed;               // true if dialog is already packed
  
  
  
  /**
   * Creates a search dialog.
   * 
   * @param owner the owner window, null if none
   * @param contextDb the database context
   * @param searchClass the object class'es table to search in, null = all tables
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   */
  public AppDbObjectSearchDialog(Window owner, ContextDb contextDb, Class<? extends AppDbObject> searchClass, Class[] selectClasses, 
                                 boolean allowCreate, boolean modal) {
    super (owner, modal);
    setup (contextDb, searchClass, selectClasses, allowCreate);
  }

  /**
   * Creates a search dialog.
   * 
   * @param contextDb the database context
   * @param searchClass the object class'es table to search in, null = all tables
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   */
  public AppDbObjectSearchDialog(ContextDb contextDb, Class<? extends AppDbObject> searchClass, Class[] selectClasses, 
                                  boolean allowCreate, boolean modal) {
    this (null, contextDb, searchClass, selectClasses, allowCreate, modal);
  }
  
  /**
   * Creates a search dialog.
   * 
   * @param owner the owner window, null if none
   * @param qbfPlugin the QBF plugin
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   */
  public AppDbObjectSearchDialog(Window owner, QbfPlugin qbfPlugin, Class[] selectClasses, boolean allowCreate, boolean modal) {
    super (owner, modal);
    setup (qbfPlugin, selectClasses, allowCreate);
  }

  /**
   * Creates a search dialog.
   * 
   * @param qbfPlugin the QBF plugin
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   * @param modal true if modal dialog
   */
  public AppDbObjectSearchDialog(QbfPlugin qbfPlugin, Class[] selectClasses, boolean allowCreate, boolean modal) {
    this(null, qbfPlugin, selectClasses, allowCreate, modal);
  }




  
  
  /** 
   * get the Qbf-Plugin
   * @return Value of property qbfPlugin.
   *
   */
  public QbfPlugin getQbfPlugin() {
    return qbfPlugin;
  }  
  
  /**
   * Set the qbf-Plugin (if custom)
   *
   * @param qbfPlugin New value of property qbfPlugin.
   *
   */
  public void setQbfPlugin(QbfPlugin qbfPlugin) {
    this.qbfPlugin = qbfPlugin;
  }
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to allow autoclosing whether the values in QbfPanels are
   * changed or not.
   * The time of last change is set in insertTree() and whenever a QBF-Field is changed.
   */
  @Override
  public boolean checkAutoClose() {
    return isAutoCloseable() && isVisible() &&
           getTimeOfLastValuesChanged() + getAutoClose() < System.currentTimeMillis();
  }

  
  /**
   * Enables/disables all buttons.
   * <p>
   * Notice: the buttons of the plugin panel are not modified.
   * 
   * @param flag true if all buttons enabled, false if all disabled
   */
  public void setButtonsEnabled(boolean flag) {
    searchButton.setEnabled(flag);
    cancelButton.setEnabled(flag);
    okQbfButton.setEnabled(flag);
    moreButton.setEnabled(flag);
    newButton.setEnabled(flag);
  }
  
  
  /**
   * Gives access to the current navigation panel.
   * 
   * @return the navigation panel
   */
  public AppDbObjectNaviPanel getNaviPanel()  {
    return naviPanel;
  }
  
  
  /**
   * Sets whether multiple selections are allowed or not.
   * 
   * @param multiSelection true if allowed, false if only single object (default)
   */
  public void setMultiSelection(boolean multiSelection) {
    this.multiSelection = multiSelection;
  }
  
  /**
   * Returns whether multiple selections are allowed or not.
   * 
   * @return true if allowed, false if only single object (default)
   */
  public boolean isMultiSelection() {
    return multiSelection;
  }
  
  
  /**
   * Sets whether the first object is automatically selected or not.
   * 
   * @param autoSelectFistItem true if selected, false if not (default)
   */
  public void setAutoSelectFirstItem(boolean autoSelectFistItem) {
    this.autoSelectFirstItem = autoSelectFistItem;
  }
  
  /**
   * Returns whether the first object is automatically selected or not.
   * 
   * @return true if selected, false if not (default)
   */
  public boolean isAutoSelectFirstItem() {
    return autoSelectFirstItem;
  }
    
  
  /**
   * Sets the qbf parameter.
   * 
   * @param qbfParameter the qbf parameter
   */
  public void setQbfParameter(QbfParameter qbfParameter)  {
    
    qbfPlugin.setParameter(qbfParameter);
    qbfPanel.setParameter(qbfParameter);
    this.qbfParameter = qbfParameter;
    
    String fmtClass   = null;
    String fmtContext = null;

    if (qbfParameter != null && qbfParameter.clazz != null) {
      try {
        fmtClass = AppDbObject.getMultiName(qbfParameter.clazz);
        if (Modifier.isAbstract(qbfParameter.clazz.getModifiers()) == false)  {
          fmtContext = AppDbObject.newByClass(qbfParameter.contextDb, qbfParameter.clazz).getBaseContext().toString();
        }
      } 
      catch (Exception ex) {
        // nothing we can do
      }
    }
    
    String title = null;
    if (fmtClass == null) {
      title = Locales.bundle.getString("Search");
    }
    else if (fmtContext == null || fmtContext.length() == 0)  {
      title = MessageFormat.format(Locales.bundle.getString("Search_{0}"), fmtClass);
    }
    else  {
      title = MessageFormat.format(Locales.bundle.getString("Search_{0}_in_{1}"), fmtClass, fmtContext);
    }
    
    if (isModal())  {
      title += " " + Locales.bundle.getString("(modal)"); 
    }
    
    setTitle(title);
  }
  
  
  /**
   * Gets the qbf parameter.
   * 
   * @return the qbf parameter
   */
  public QbfParameter getQbfParameter() {
    return qbfParameter;
  }
  

  /**
   * Shows the dialog (modal or non-modal).
   * <p>
   * Notice: if multiSelection=true the first selected object is returned. U
   * se getSelectedObjects() to get all objects
   *
   * @return the selected object, null if nothing selected or non-modal
   */
  public AppDbObject showDialog ()  {
    
    if (qbfParameter.clazz != null) {
      try {
        // check permissions
        SecurityResult sr = qbfParameter.contextDb.getAppUserInfo().getSecurityManager().privilege(
              qbfParameter.clazz, qbfParameter.contextDb, Security.READ);
        if (sr.isDenied())  {
          FormInfo.print(sr.explain(Locales.bundle.getString("You_don't_have_permission_to_view_this_kind_of_data!")));
          return null;
        }
      }
      catch (ApplicationException e)  {
        FormError.printException(e);
        return null;
      }
    }
    
    if (qbfParameter.searchImmediate && qbfParameter.qbfPanelInvisible) {
      searchPanel.setVisible(false);
    }
    
    setFormValues();
    saveValues();   // to allow areValuesChanged, if some app needs that
    pack();
    
    qbfPanel.setInitialFocus();    // select the first object

    if (qbfParameter.searchImmediate)  {
      doSearch(null);
      if (noShowIfSingle && naviList != null && naviList.size() == 1)  {
        selectedObject = naviList.get(0);
        return selectedObject;
      }
      if (qbfParameter.showDialogIfSearchImmediateFailes == false && (naviList == null || naviList.isEmpty())) {
        return null;
      }
    }

    setVisible(true);
    return selectedObject;    // returns null if non-modal
  }

  
  /**
   * Gets the (first) selected object.
   * 
   * @return the selected object, null if none
   */
  public AppDbObject getSelectedObject() {
    return selectedObject;
  }

  /**
   * Gets the list of all selected objects.
   * 
   * @return the selected objects, null if none
   */
  public List<AppDbObject> getSelectedObjects()  {
    return selectedObjects;
  }
  
  
  
  /** 
   * Returns whether this dialog is configured to "save a query".
   * 
   * @return true if qbf selection mode enabled, default is false
   */
  public boolean isQbfSelection() {
    return qbfSelection;
  }  

  /**
   * Enables a button for "save query" and configures
   * the dialog to dispose if that button is pressed.
   * 
   * @param qbfSelection true to enable qbf selection mode, default is false
   */
  public void setQbfSelection(boolean qbfSelection) {
    this.qbfSelection = qbfSelection;
    okQbfButton.setVisible(qbfSelection);
  }  

  
  
  /**
   * Returnss whether the dialog should not show up if there is
   * only one object matching.
   * 
   * @return true if return single object immediately, false if show (default)
   */
  public boolean isNoShowIfSingle() {
    return noShowIfSingle;
  }
  
  /**
   * Sets whether the dialog should not show up if there is
   * only one object matching.
   * 
   * @param noShowIfSingle true if return single object immediately, false if show (default)
   */
  public void setNoShowIfSingle(boolean noShowIfSingle) {
    this.noShowIfSingle = noShowIfSingle;
  }
  
  
  


  /**
   * Runs the search.
   * <p>
   * Depending on the qbf parameter the search is run in a separate thread.
   * 
   * @see QbfParameter#searchInExtraThread
   */
  protected void runSearch()  {
    
    // setup buttons and change cursor
    FormHelper.setWaitCursor(this);
    setButtonsEnabled(false);
    
    // run this after doSearch
    final Runnable cleanup = new Runnable() {
      public void run() {
        setButtonsEnabled(true);
        FormHelper.setDefaultCursor(AppDbObjectSearchDialog.this);
        if (naviPanel != null && autoSelectFirstItem)  {
          naviPanel.requestFocusForFirstItem();
        }        
      }
    };
            
    final ModificationThread modThread = ModificationThread.getThread();
    
    if (qbfParameter.searchInExtraThread && modThread.isAlive()) {
      // run once in modthread.
      // This prevents modthread from doing nasty things while
      // the search is running.
      modThread.runOnce(new Runnable() {
        public void run() {
          // use the db from the modthread.
          // this allows the user to do other things in parallel
          // on the primary db-connection.
          // (although this would be a rare case)
          doSearch(modThread.getDb());
          try {
            EventQueue.invokeAndWait(cleanup);
          }
          catch (Exception e) {}
        }
      });
    }
    else  {
      // run in GUI-Thread
      doSearch(null);
      cleanup.run();
    }
  }
  
  
  
  /**
   * Runs the database search.
   * 
   * @param db the optional db, null if use default from qbf parameter
   */
  @SuppressWarnings("unchecked")
  protected void doSearch(Db db) {

    if (qbfPlugin.isParameterValid()) {
      
      FormHelper.setWaitCursor(this);
      
      ContextDb savedContextDb = null;
      
      try {
        
        if (db != null) {
          // switch to different db temporarily
          savedContextDb = qbfPlugin.getParameter().contextDb;
          ContextDb clonedContextDb = savedContextDb.clone();
          clonedContextDb.setDb(db);
          qbfPlugin.getParameter().contextDb = clonedContextDb;
        }

        // execute query
        if (qbfPlugin.executeQuery())  {

          if (qbfPlugin.isResultDisplayable())  {
            /**
             * Insert all objects.
             * Don't insert an object more than once!
             * Keep the ordering of original source.
             */
            Set<AppDbObject>  uset    = new HashSet<AppDbObject>();
            List<AppDbObject> objects = null;

            DbCursor<? extends AppDbObject> cursor = qbfPlugin.getCursor();
            if (cursor != null) {
              // select as cursor
              objects = new ArrayList<AppDbObject>();
              for (;;) {
                List<? extends AppDbObject> fetched = cursor.fetch();   // get next fetchsize block
                if (fetched == null) {
                  break;
                }
                // add the context uniquely
                for (AppDbObject obj: fetched) {
                  if (obj != null) {
                    if (savedContextDb != null) {
                      // set context first due to SecurityManager's db connection,
                      // if reloading rules is necessary
                      obj.setContextDb(savedContextDb);
                    }
                    if (isObjectAllowed(obj) && uset.add(obj)) {
                      objects.add(obj);
                    }
                  }
                }
              }
              cursor.close();
            }
            else  {   
              // select as Collection
              Collection<? extends AppDbObject> objColl = qbfPlugin.getObjects();
              if (objColl instanceof List)  {
                // this is the default
                objects = (List<AppDbObject>)objColl;    // unchecked
                for (Iterator<AppDbObject> iter = objects.iterator(); iter.hasNext(); )  {
                  // add the context uniquely
                  AppDbObject obj = iter.next();
                  if (obj != null) {
                    if (savedContextDb != null) {
                      // set context first due to SecurityManager's db connection,
                      // if reloading rules is necessary
                      obj.setContextDb(savedContextDb);
                    }
                    if (!isObjectAllowed(obj) || !uset.add(obj)) {
                      iter.remove();
                    }
                  }
                  else  {
                    iter.remove();
                  }
                }
              }
              else if (objColl != null) {
                // not a list: make a List (rare)
                objects = new ArrayList<AppDbObject>();
                for (AppDbObject obj: objColl)  {
                  if (obj != null) {
                    if (savedContextDb != null) {
                      // set context first due to SecurityManager's db connection,
                      // if reloading rules is necessary
                      obj.setContextDb(savedContextDb);
                    }
                    if (isObjectAllowed(obj) && uset.add(obj)) {
                      objects.add(obj);
                    }
                  }
                }              
              }
            }

            final List<AppDbObject> list = objects;

            // insert objects
            // as a runnable becasue might run in modthread
            Runnable r = new Runnable() {
              public void run() {
                insertTree(list);
                // if previous search was searchMore:
                if (morePattern != null)  {
                  morePattern = null;
                  morePatternField.fireValueChanged();
                }
                // set the focus
                if (autoSelectFirstItem)  {
                  naviPanel.requestFocusForFirstItem();
                }
                FormHelper.setDefaultCursor(AppDbObjectSearchDialog.this);
              }
            };

            if (EventQueue.isDispatchThread())  {
              r.run();    // we are in GUI thread: run in GUI thread
            }
            else  {
              // we are in modthread
              try {
                EventQueue.invokeAndWait(r);
              }
              catch (Exception e) {
                DbGlobal.errorHandler.warning(e, "search failed");
              }
            }
            return;
          }
        }
        else  {
          FormHelper.setDefaultCursor(this);
          FormInfo.print(qbfPlugin.notFoundMessage());
        }
      }
      finally {
        if (savedContextDb != null) {
          // switch back to orgininal contextdb
          qbfPlugin.getParameter().contextDb = savedContextDb;
        }
      }
    }
    // start over
    qbfPanel.setInitialFocus();
  }


  /**
   * Search in results.
   */
  protected void doMoreSearch ()  {
    if (naviList != null && morePattern != null && morePattern.length() > 0)  {
      String thMorePattern = StringHelper.normalize(morePattern);
      List<AppDbObject> nlist = new ArrayList<AppDbObject>();
      for (AppDbObject mo: naviList)  {
        if (mo != null && mo.containsPattern(thMorePattern))  {
          nlist.add(mo);
        }
      }
      insertTree(nlist);
    }
  }

  
  /**
   * Inserts a list of objects into the navigation panel.
   * 
   * @param list the list of objects
   */
  protected void insertTree (List<AppDbObject> list)  {

    clearResult();

    if (list != null)  {
      
      // remember for "search in results"
      naviList = list;    

      // create navigation panel
      if (naviPanel == null)  {
        naviPanel = new AppDbObjectNaviPanel (
                          naviList, selectClasses, 
                          (isModal() && !isQbfSelection() ? 
                            (qbfParameter.qbfPanelInvisible ? 
                              AppDbObjectNaviPanel.SHOW_BUTTONS : 
                              AppDbObjectNaviPanel.SHOW_SELECT) :
                            (qbfParameter.qbfPanelInvisible ? 
                              AppDbObjectNaviPanel.SHOW_CLOSE :
                              AppDbObjectNaviPanel.SHOW_NOBUTTON)),
                          qbfParameter.showTable, qbfParameter.tableName);
        
        // show view
        getContentPane().add(naviPanel, BorderLayout.CENTER);

        naviPanel.addActionListener(new java.awt.event.ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // some button pressed in naviPanel
            selectedObject = naviPanel.getSelectedObject();
            selectedObjects = naviPanel.getSelectedObjects();
            if (isModal() || selectedObject == null) {
              dispose();    // finish dialog
            }
            else  {
              // show selected object
              Hook.hook().view(selectedObject);
            }
          }
        });
      }
      else  {
        naviPanel.setObjects(list, qbfParameter.rebuildView);
      }
      
      naviPanel.setTableIntro(qbfPlugin.getParameter().toString());
      if (isModal()) {
        naviPanel.setDisposeKeyEnabled(true);
      }
      
      if (multiSelection)  {
        naviPanel.setTreeSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      }
      else  {
        naviPanel.setTreeSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      }
                                                         
      if (naviList.size() > 1 && qbfPlugin.getParameter() instanceof DefaultQbfParameter &&
          ((DefaultQbfParameter)qbfPlugin.getParameter()).minPatternLength >= 0)  {
        // enable "search in results"
        moreButton.setVisible(true);
        morePatternField.setVisible(true);
      }
      
      qbfPanel.resultsShown();    // tell the QbfPanel that results are shown
    }

    // start timeout interval again
    setTimeOfLastValuesChanged(System.currentTimeMillis());
    
    if (!packed) {
      pack();
      packed = true;
    }
  }


  /**
   * Cancels the dialog.<br>
   * (clears the selection and the qbf parameter).
   */
  protected void doCancel() {
    selectedObject = null;
    selectedObjects = null;
    qbfParameter = null;
  }


  
  /**
   * Determines the plugin from the searchClass.
   * 
   * @param contextDb the database context
   * @param searchClass the allowed selections
   * @return the plugin
   * @throws ApplicationException if plugin could not be determined
   */
  protected QbfPlugin makePlugin(ContextDb contextDb, Class<? extends AppDbObject> searchClass) throws ApplicationException {
    QbfPlugin plugin;
    if (searchClass == null)  {
      // search in all classes
      plugin = new DefaultQbfPlugin(new DefaultQbfParameter(null, contextDb));
      allowCreate = false;
    }
    else if (Modifier.isAbstract(searchClass.getModifiers())) {
      // abtract class
      try {
        plugin = AppDbObject.makeQbfPlugin(searchClass, contextDb);
      }
      catch (Exception ex)  {
        throw new ApplicationException(Locales.bundle.getString("static_makeQbfPlugin()_missing_in_") + searchClass, ex); 
      }
    }
    else  {
      // get the plugin from the object
      try {
        plugin = AppDbObject.newByClass(contextDb, searchClass).makeQbfPlugin();
      }
      catch (Exception ex)  {
        throw new ApplicationException(Locales.bundle.getString("makeQbfPlugin()_failed_in_") + searchClass, ex); 
      }
    }
    return plugin;
  }
  
  
  
  /**
   * Sets up the dialog by context and searchclass.
   * 
   * @param contextDb the database context
   * @param searchClass the object class'es table to search in, null = all tables
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   */
  protected void setup (ContextDb contextDb, Class<? extends AppDbObject> searchClass,
                        Class[] selectClasses, boolean allowCreate) {
    try {
      setup(makePlugin(contextDb, searchClass), selectClasses, allowCreate);
    }
    catch (ApplicationException ex) {
      DbGlobal.errorHandler.severe(ex, "setup search dialog failed");
    }
  }
  
  
  /**
   * Sets up the dialog by plugin.
   * 
   * @param qbfPlugin the QBF plugin
   * @param selectClasses the allowed selections
   * @param allowCreate true if "new"-button for creation of a new object of searchClass
   */
  protected void setup (QbfPlugin qbfPlugin, Class[] selectClasses, boolean allowCreate)  {

    this.qbfPlugin      = qbfPlugin;
    this.selectClasses  = selectClasses;
    this.allowCreate    = allowCreate;
    
    initComponents();
    
    setMultiSelection(true);    // this is default
    setAutoSelectFirstItem(true);
    setQbfSelection(false);
    
    qbfPanel = qbfPlugin.getPanel();
    searchPanel.add(qbfPanel, BorderLayout.CENTER);
    
    qbfPanel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        searchButton.doClick();
      }
    });
    
    // set the qbfParameter (and title, etc...)
    setQbfParameter(qbfPlugin.getParameter());
    
    newButton.setVisible(allowCreate);
    
    setFormValues();      // set all Values (also in qbfPanel!)
    
    clearResult();
  }

  
  /**
   * Clears the search result.
   */
  protected void clearResult()  {
    selectedObject = null;
    selectedObjects = null;
    naviList = null;
    if (naviPanel != null)  {
      naviPanel.setObjects(new ArrayList<AppDbObject>());
    }
    morePatternField.setVisible(false);
    moreButton.setVisible(false);
  }

  
  
  
  
  
  /**
   * Checks read-permission
   */
  private boolean isObjectAllowed(AppDbObject object) {
    return qbfParameter.checkReadPermission == false || object.isPermissionDenied(Security.READ) == false;
  }
  
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    searchPanel = new org.tentackle.ui.FormPanel();
    patternPanel = new org.tentackle.ui.FormPanel();
    buttonPanel = new org.tentackle.ui.FormPanel();
    searchButton = new org.tentackle.ui.FormButton();
    newButton = new org.tentackle.ui.FormButton();
    okQbfButton = new org.tentackle.ui.FormButton();
    cancelButton = new org.tentackle.ui.FormButton();
    morePatternField = new org.tentackle.ui.StringFormField();
    moreButton = new org.tentackle.ui.FormButton();

    setAutoPosition(true);
    addFormWrapListener(new org.tentackle.ui.FormWrapListener() {
      public void formWrapped(org.tentackle.ui.FormWrapEvent evt) {
        formFormWrapped(evt);
      }
    });
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosed(java.awt.event.WindowEvent evt) {
        formWindowClosed(evt);
      }
    });

    searchPanel.setLayout(new java.awt.BorderLayout());

    patternPanel.setLayout(new java.awt.GridBagLayout());

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    searchButton.setIcon(PlafGlobal.getIcon("search"));
    searchButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    searchButton.setMnemonic(Locales.bundle.getString("searchMnemonic").charAt(0));
    searchButton.setText(Locales.bundle.getString("find")); // NOI18N
    searchButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        searchButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(searchButton, gridBagConstraints);

    newButton.setIcon(PlafGlobal.getIcon("new"));
    newButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    newButton.setMnemonic(Locales.bundle.getString("newMnemonic").charAt(0));
    newButton.setText(Locales.bundle.getString("new")); // NOI18N
    newButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        newButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(newButton, gridBagConstraints);

    okQbfButton.setIcon(PlafGlobal.getIcon("save"));
    okQbfButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    okQbfButton.setMnemonic(Locales.bundle.getString("saveMnemonic").charAt(0));
    okQbfButton.setText(Locales.bundle.getString("save_Query")); // NOI18N
    okQbfButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        okQbfButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(okQbfButton, gridBagConstraints);

    cancelButton.setIcon(PlafGlobal.getIcon("close"));
    cancelButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    cancelButton.setMnemonic(Locales.bundle.getString("cancelMnemonic").charAt(0));
    cancelButton.setText(Locales.bundle.getString("cancel")); // NOI18N
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    buttonPanel.add(cancelButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    patternPanel.add(buttonPanel, gridBagConstraints);

    morePatternField.setAutoSelect(true);
    morePatternField.setColumns(15);
    morePatternField.setConvert('^');
    morePatternField.setInvalidChars("=%");
    morePatternField.addValueListener(new org.tentackle.ui.ValueListener() {
      public void valueChanged(org.tentackle.ui.ValueEvent evt) {
        morePatternFieldValueChanged(evt);
      }
      public void valueEntered(org.tentackle.ui.ValueEvent evt) {
        morePatternFieldValueEntered(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    patternPanel.add(morePatternField, gridBagConstraints);

    moreButton.setIcon(PlafGlobal.getIcon("search"));
    moreButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
    moreButton.setMnemonic(Locales.bundle.getString("findInResultsMnemonic").charAt(0));
    moreButton.setText(Locales.bundle.getString("find_in_results")); // NOI18N
    moreButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        moreButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    patternPanel.add(moreButton, gridBagConstraints);

    searchPanel.add(patternPanel, java.awt.BorderLayout.SOUTH);

    getContentPane().add(searchPanel, java.awt.BorderLayout.NORTH);

  }// </editor-fold>//GEN-END:initComponents

  private void okQbfButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okQbfButtonActionPerformed
    dispose();
  }//GEN-LAST:event_okQbfButtonActionPerformed

  private void formFormWrapped(org.tentackle.ui.FormWrapEvent evt) {//GEN-FIRST:event_formFormWrapped
    if (moreButton.isVisible() && moreButton.isSelected() == false) {
      moreButton.doClick();
    }
    else if (searchButton.isVisible())  {
      searchButton.doClick();
    }
  }//GEN-LAST:event_formFormWrapped

  private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
    if (qbfPlugin != null)  {
      qbfPlugin.cleanup();
    }
  }//GEN-LAST:event_formWindowClosed

  private void morePatternFieldValueEntered(org.tentackle.ui.ValueEvent evt) {//GEN-FIRST:event_morePatternFieldValueEntered
    morePattern = morePatternField.getText();
  }//GEN-LAST:event_morePatternFieldValueEntered

  private void morePatternFieldValueChanged(org.tentackle.ui.ValueEvent evt) {//GEN-FIRST:event_morePatternFieldValueChanged
    morePatternField.setFormValue(morePattern);
  }//GEN-LAST:event_morePatternFieldValueChanged

  private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    doCancel();
    dispose();
  }//GEN-LAST:event_cancelButtonActionPerformed

  private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
    // create and edit new object
    selectedObject = Hook.hook().editModal(qbfPlugin.newAppDbObject());

    if (selectedObject != null) {
      if (selectClasses != null)  {
        // special case: searchClass is not contained in selectClasses (e.g. obj is Person and sel is Adresse)
        int i;
        for (i=0; i < selectClasses.length; i++)  {
          if (selectClasses[i] == qbfParameter.clazz) {
            break;
          }
        }
        if (i >= selectClasses.length)  {
          List<AppDbObject> list = new ArrayList<AppDbObject>();
          list.add(selectedObject);
          insertTree(list);     // show selected object only
          selectedObjects = new ArrayList<AppDbObject>();
          selectedObjects.add(selectedObject);
          return;
        }
      }
      if (isModal()) {
        dispose();
      }
    }
  }//GEN-LAST:event_newButtonActionPerformed

  private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
    runSearch();
  }//GEN-LAST:event_searchButtonActionPerformed

  private void moreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreButtonActionPerformed
    morePattern = morePatternField.getText();
    doMoreSearch();
    if (naviPanel != null)  {
      naviPanel.requestFocusForFirstItem();
    }
  }//GEN-LAST:event_moreButtonActionPerformed


  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormPanel buttonPanel;
  private org.tentackle.ui.FormButton cancelButton;
  private org.tentackle.ui.FormButton moreButton;
  private org.tentackle.ui.StringFormField morePatternField;
  private org.tentackle.ui.FormButton newButton;
  private org.tentackle.ui.FormButton okQbfButton;
  private org.tentackle.ui.FormPanel patternPanel;
  private org.tentackle.ui.FormButton searchButton;
  private org.tentackle.ui.FormPanel searchPanel;
  // End of variables declaration//GEN-END:variables
  
}
