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

// $Id: AppDbCursor.java 481 2009-09-18 15:18:36Z svn $


package org.tentackle.appworx;

import java.awt.EventQueue;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.tentackle.db.ModificationThread;
import org.tentackle.db.ResultSetWrapper;
import org.tentackle.db.SimpleDbCursor;
import org.tentackle.db.rmi.RemoteDbCursor;
import org.tentackle.ui.FormInfo;
import org.tentackle.ui.FormQuestion;
import org.tentackle.ui.UIGlobal;



/**
 * Extends {@link SimpleDbCursor} for {@link AppDbObject}s.
 * Furthermore, adds warning thresholds, fetchsize, 
 * sleep intervals (be nice to other users)
 * and a progress dialog if many rows are retrieved.
 *
 * @param <T> the data object class
 * @author harald
 */

public class AppDbCursor<T extends AppDbObject> extends SimpleDbCursor<T> {
  
  /** database context **/
  protected ContextDb contextDb;
  /** != 0 if ask user if more objects retrieved **/
  protected int warnRowCount;
  /** != 0 if ask user can't receive more objects **/
  protected int maxRowCount;
  /** if != 0: show user progress in this intervals **/
  protected int updateRowCount;
  /** for fetch() only: number of rows fetched since progress shown **/
  protected int updateFetchCount;
  /** time [ms] to sleep between warnRowCount packets, 0 = no sleep (default) **/
  protected long warnSleep;
  /** user requested to abort the loading of data **/
  protected boolean abortRequested;
  /** expected number of rows retrieved, 0 = unknown (default) **/
  protected int estimatedRowCount;
  
  
  private AppDbCursorProgressDialog pd;   // != null if progress dialog shown
  private Thread keepAliveThread;         // != null if thread started to keep remote connections alive
  
  
  /**
   * Creates a cursor for a local connection.
   *
   * @param contextDb the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   * @param withLinkedObjects true if load linked objects, false otherwise
   */
  public AppDbCursor(ContextDb contextDb, Class<T> dbClass, ResultSetWrapper rs, boolean withLinkedObjects) {
    super(contextDb.getDb(), dbClass, rs, withLinkedObjects);
    this.contextDb = contextDb;
  }
  
  /**
   * Creates a cursor for a local connection.
   *
   * @param contextDb the db connection
   * @param dbClass the DbObject class
   * @param rs the resultset
   */
  public AppDbCursor(ContextDb contextDb, Class<T> dbClass, ResultSetWrapper rs) {
    this(contextDb, dbClass, rs, true);
  }
  
  /**
   * Creates a cursor for a remote connection.
   *
   * @param contextDb the db connection
   * @param rc the remote cursor
   */
  public AppDbCursor(ContextDb contextDb, RemoteDbCursor rc) {
    super(contextDb.getDb(), rc);
    this.contextDb = contextDb;
  }
  
  /**
   * Creates a cursor for a remote connection.
   *
   * @param par the qbf parameter
   * @param rc the remote cursor
   */
  public AppDbCursor(QbfParameter par, RemoteDbCursor rc) {
    this(par.contextDb, rc);
    applyQbfParameter(par);
  }
  
  
  /**
   * Applies local settings of the qbf parameter to a remote cursor.
   * This sets warnRowCount, warnSleep and maxRowCount, i.e.
   * only attributes of the parameter that affect the local
   * side of a remote cursor.
   *
   * @param par the qbf parameter
   */
  public void applyQbfParameterLocalOnly(QbfParameter par) {  
    setWarnRowCount(par.warnRowCount);
    setWarnSleep(par.warnSleep);
    setMaxRowCount(par.maxRowCount);
    setEstimatedRowCount(par.estimatedRowCount);
  }
  
  
  /**
   * Applies the qbf parameter to a local cursor.
   * 
   * @param par the qbf parameter
   */
  public void applyQbfParameter(QbfParameter par) {
    setFetchSize(par.fetchSize);
    applyQbfParameterLocalOnly(par);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the contextdb instead of db only.
   *
   * @param object the AppDbObject to set the contextDb for, never null
   */
  @Override
  public void setDbContext(T object) {
    object.setContextDb(contextDb);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to check for warnRowCount and maxRowCount.
   */
  @Override
  public boolean next() {
    if (UIGlobal.isHeadless) {
      return super.next();
    }
    if (!abortRequested && super.next()) {
      if (maxRowCount > 0)  {
        if (row >= maxRowCount)  {
          startKeepAliveIfNecessary();
          FormInfo.print(MessageFormat.format(
                  Locales.bundle.getString("more_than_{0}_objects_are_not_allowed_to_retrieve"), 
                  maxRowCount));
          stopKeepAliveIfNecessary();
          abortRequested = true;
          return true;    // return this last record
        }
      }
      if (warnRowCount > 0 && row > warnRowCount) {
        startKeepAliveIfNecessary();
        boolean rv = FormQuestion.yesNo(estimatedRowCount > 0 ?
            MessageFormat.format(Locales.bundle.getString("{0}_objects_found._Retrieve_more_than_{1}?"), estimatedRowCount, warnRowCount) :
            MessageFormat.format(Locales.bundle.getString("more_than_{0}_objects_found._Retrieve_all?"), warnRowCount));
        stopKeepAliveIfNecessary();
        if (rv && !EventQueue.isDispatchThread()) {
          // continue and show progress in modal window if we're not the GUI-thread
          pd = new AppDbCursorProgressDialog(AppDbCursor.this);
          pd.updateRowCount(warnRowCount);
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              pd.showDialog();
              // notice: although this method blocks because AppDbCursorProgressDialog is modal
              // it will not block the event queue! Nice trick... ;-)
            }
          });
        }
        updateRowCount = warnRowCount;  // show progress every updateRowCount objects
        warnRowCount   = 0;             // ask only once
        return rv;
      }
      else if (pd != null && (row % updateRowCount) == 0) {
        // EventQueue.isDispatchThread() == false !
        try {
          EventQueue.invokeAndWait(new Runnable() {
            public void run() {
              pd.updateRowCount(row);
            }
          });
          if (warnSleep > 0) {
            Thread.sleep(warnSleep);    // be nice to other users and show user that this *really* sucks ;-)
          }
        }
        catch (Exception e) {}
      }
      return true;
    }
    
    if (pd != null) {
      pd.dispose();
      pd = null;
      abortRequested = false;
      warnRowCount   = updateRowCount;
      updateRowCount = 0;
    }
    return false;
  }
  
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to check for warnRowCount and maxRowCount.
   */
  @Override
  public List<T> fetch() {
    if (UIGlobal.isHeadless) {
      return super.fetch();
    }
    if (!abortRequested) {
      List<T> list = super.fetch();
      if (list != null) {
        updateFetchCount += list.size();
        if (maxRowCount > 0)  {
          if (row >= maxRowCount)  {
            startKeepAliveIfNecessary();
            FormInfo.print(MessageFormat.format(
                  Locales.bundle.getString("more_than_{0}_objects_are_not_allowed_to_retrieve"), 
                  maxRowCount));
            stopKeepAliveIfNecessary();
            abortRequested = true;    // next fetch() will return null
            return list;
          }
        }
        if (warnRowCount > 0 && row > warnRowCount) {
          startKeepAliveIfNecessary();
          boolean rv = FormQuestion.yesNo(estimatedRowCount > 0 ?
            MessageFormat.format(Locales.bundle.getString("{0}_objects_found._Retrieve_more_than_{1}?"), estimatedRowCount, warnRowCount) :
            MessageFormat.format(Locales.bundle.getString("more_than_{0}_objects_found._Retrieve_all?"), warnRowCount));
          stopKeepAliveIfNecessary();
          if (rv && !EventQueue.isDispatchThread()) {
            // continue and show progress in modal window if we're not the GUI-thread
            pd = new AppDbCursorProgressDialog(AppDbCursor.this);
            pd.updateRowCount(warnRowCount);
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                pd.showDialog();
                // notice: although this method blocks because AppDbCursorProgressDialog is modal
                // it will not block the event queue! Nice trick... ;-)
              }
            });
          }
          updateRowCount = warnRowCount;  // show progress every updateRowCount objects
          warnRowCount   = 0;             // ask only once
          return rv ? list : null;
        }
        else if (pd != null && updateFetchCount >= updateRowCount) {
          // EventQueue.isDispatchThread() == false !
          updateFetchCount = 0;
          try {
            EventQueue.invokeAndWait(new Runnable() {
              public void run() {
                pd.updateRowCount(row);
              }
            });
            if (warnSleep > 0) {
              Thread.sleep(warnSleep);    // be nice to other users and show user that this *really* sucks ;-)
            }
          }
          catch (Exception e) {}
        }
        return list;
      }
    }
    
    if (pd != null) {
      pd.dispose();
      pd = null;
      abortRequested   = false;
      warnRowCount     = updateRowCount;
      updateRowCount   = 0;
      updateFetchCount = 0;
    }
    
    return null;    // end of list reached or abort requested
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to check for warnRowCount and maxRowCount.
   */
  @Override
  public List<T> toList() {
    if (isRemote()) {
      List<T> list = null;
      List<T> chunk;
      while ((chunk = fetch()) != null) {
        if (list == null) {
          list = chunk;
        }
        else  {
          list.addAll(chunk);
        }
      }
      if (list == null) {
        list = new ArrayList<T>();
      }
      return list;
    }
    else  {
      return super.toList();
    }
  }

  
  /**
   * Gets the current warning row count.
   * 
   * @return the warnRowCount.
   */
  public int getWarnRowCount() {
    return warnRowCount;
  }  
  
  
  /**
   * Sets the threshold for the number of rows to
   * warn the user in an infodialog and ask him whether more
   * rows should really be retrieved. 
   * 
   * @param warnRowCount the warnRowCount, 0 to disable the feature
   */
  public void setWarnRowCount(int warnRowCount) {
    this.warnRowCount = warnRowCount;
  }
  
  
  /**
   * Gets the maximum number of rows to retrieve.
   * 
   * @return the maxRowCount
   */
  public int getMaxRowCount() {
    return maxRowCount;
  }  
  
  
  /**
   * Sets the maximum number of rows to retrieve.
   * This will prevent "dumb" queries from using too much memory.
   * 
   * @param maxRowCount the maxRowCount, 0 = unlimited
   */
  public void setMaxRowCount(int maxRowCount) {
    this.maxRowCount = maxRowCount;
  }
  
  
  /**
   * Gets the abort flag.
   * 
   * @return true if abort has been requested
   */
  public boolean isAbortRequested() {
    return abortRequested;
  }  
  
  
  /**
   * Sets the abort requestion flag.
   * If set, the cursor will be closed before all data has been retrieved.
   * Used by progress dialogs, for example {@link AppDbCursorProgressDialog}.
   * 
   * @param abortRequested true if the query should be aborted asap.
   */
  public void setAbortRequested(boolean abortRequested) {
    this.abortRequested = abortRequested;
  }
  
  
  /**
   * Gets the sleep interval.
   * 
   * @return the interval in [ms]
   */
  public long getWarnSleep() {
    return warnSleep;
  }
  
  
  /**
   * Sets the sleep interval between two warnRowCount packets retrieved.
   * This gives the user a chance to abort the retrieval in the progress dialog
   * and reduces the db-server's load temporarily, being nice to other users.
   * 
   * @param warnSleep the interval, 0 to disable
   */
  public void setWarnSleep(long warnSleep) {
    this.warnSleep = warnSleep;
  }
  
  
  
  /**
   * Sets the expected query count.
   * Usually determined by a prior select count(*).
   * Affects only the message displayed to the user if
   * more than warnRowCount objects are retrieved.
   * 
   * @param estimatedRowCount the expected count, 0 = unknown (default)
   */
  public void setEstimatedRowCount(int estimatedRowCount) {
    this.estimatedRowCount = estimatedRowCount;
  }
  
  
  /**
   * Gets the expected query count.
   * @return the expected count, 0 = unknown (default)
   */
  public int getEstimatedRowCount() {
    return estimatedRowCount;
  }
  
  
  /**
   * Checks whether starting a keep alive thread is necessary and starts it if so.
   */
  private void startKeepAliveIfNecessary() {
    if (db.isRemote()) {
      long interval = ModificationThread.getThread().getInterval();
      if (interval < 1000)  {
        interval = 5000;    // default value, not too short/long
      }
      keepAliveThread = contextDb.getDb().createKeepAliveThread(interval);
      keepAliveThread.start();
    }
  }
  
  /**
   * Checks whether stopping a keep alive thread is necessary and stops it if so.
   */
  private void stopKeepAliveIfNecessary() {
    if (keepAliveThread != null) {
      keepAliveThread.interrupt();
      try {
        keepAliveThread.join();
      } 
      catch (InterruptedException ex) {
        // ignore
      }
      keepAliveThread = null;
    }
  }
}
