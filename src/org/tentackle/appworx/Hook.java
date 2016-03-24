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

// $Id: Hook.java 465 2009-07-19 15:18:09Z harald $

package org.tentackle.appworx;

import java.awt.Component;
import java.io.PrintStream;
import java.text.MessageFormat;
import org.tentackle.ui.FormInfo;
import org.tentackle.ui.FormQuestion;
import org.tentackle.util.ApplicationException;
import org.tentackle.util.Logger.Level;
import org.tentackle.util.LoggerOutputStream;

/**
 * Delegates certain (mostly interactive) functionality to the concrete implementation.
 * <p>
 * For example, applications should not instantiate AppDbObjectDialog directly, but use
 * Hook.edit() instead. By using this technique it is fairly easy to change the
 * implementation at runtime. Poolkeepers replication framework relies on that, for example.
 *
 * @author harald
 */
public class Hook {
  
  // --------------------- static section ----------------------------------
  
  /**
   * The classname of the hook.
   * Applications *must* change this via install() if the hook's implementation needs to be changed.
   */
  protected static String CLASSNAME = "org.tentackle.appworx.Hook";
  
  /** global hook (singleton) **/
  protected static Hook hook;
  
  private static Object mutex = new Object();   // to synchronize cause of singleton
  
  
  /**
   * Gets the global hook.
   * This is a factory method and refers to CLASSNAME.
   * 
   * @return the hook, never null
   */
  public static Hook hook() {
    synchronized(mutex) {
      if (hook == null) {
        try {
          hook = (Hook)Class.forName(CLASSNAME).newInstance();
        } 
        catch (Exception ex) {
          PrintStream ps = new PrintStream(new LoggerOutputStream(AppworxGlobal.logger, Level.SEVERE));
          ex.printStackTrace(ps);
          ps.close();        
          System.exit(1);
        }
      }
      return hook;
    }
  }
  
  /**
   * Installs the hook (overriding any existing hook) for the given public CLASSNAME.
   * @param className the classname of the hook
   */
  public static void install(String className) {
    synchronized(mutex) {
      if (hook != null) {
        hook.shutdown();
        hook = null;    // to GC
      }
      CLASSNAME = className;
      // the next invocation of hook() will load it!
    }
  }
  
  // --------------------- end static section ----------------------------------
  
  
  
  
  protected AppDbObjectDialogPool dialogPool;   // the hook's dialog pool
  
  
  /**
   * Creates a hook.
   */
  protected Hook() {
    startup();
  }
  
  
  /**
   * Initializes a new hook.
   * The default implementation creates an empty {@link AppDbObjectDialogPool}.
   */
  protected void startup() {
    dialogPool = new AppDbObjectDialogPool();
  }

  
  /**
   * Shuts the hook down (for being replaced)
   */
  protected void shutdown() {
    if (dialogPool != null) {
      dialogPool.disposeAllDialogs();
      dialogPool = null;
    }
  }
  
  
  /**
   * Gets the dialog pool.
   * 
   * @return the dialog pool, never null
   */
  public AppDbObjectDialogPool getDialogPool() {
    return dialogPool;
  }
  
  
  /**
   * Shows an {@code AppDbObject} in a non-modal dialog.
   *
   * @param object the AppDbObject to view-only
   * @param comp optional component to determine the window owner, null = no owner
   */
  public void view(AppDbObject object, Component comp) {
    getDialogPool().useNonModalDialog(comp, object, false);
  }
  
  /**
   * Shows an {@code AppDbObject} in a non-modal dialog.
   *
   * @param object the AppDbObject to view-only
   */
  public void view(AppDbObject object) {
    getDialogPool().useNonModalDialog(null, object, false);
  }
  
  /**
   * Shows an {@code AppDbObject} in a modal dialog.
   *
   * @param object the AppDbObject to view-only
   * @param comp optional component to determine the window owner, null = no owner
   */
  public void viewModal(AppDbObject object, Component comp) {
    getDialogPool().useModalDialog(comp, object, false);
  }
  
  /**
   * Shows an {@code AppDbObject} in a modal dialog.
   *
   * @param object the AppDbObject to view-only
   */
  public void viewModal(AppDbObject object) {
    getDialogPool().useModalDialog(null, object, false);
  }
  
  
  /**
   * Edits an {@code AppDbObject} in a non-modal dialog.
   *
   * @param object the AppDbObject to edit
   * @param comp optional component to determine the window owner, null = no owner
   * @param disposeOnDeleteOrSave true if dispose dialog after delete or save
   */
  public void edit(AppDbObject object, Component comp, boolean disposeOnDeleteOrSave) {
    getDialogPool().useNonModalDialog(comp, object, true, false, disposeOnDeleteOrSave);
  }
  
  /**
   * Edits an {@code AppDbObject} in a non-modal dialog.
   *
   * @param object the AppDbObject to edit
   * @param disposeOnDeleteOrSave true if dispose dialog after delete or save
   */
  public void edit(AppDbObject object, boolean disposeOnDeleteOrSave) {
    edit(object, null, disposeOnDeleteOrSave);
  }

  /**
   * Edits an {@code AppDbObject} in a non-modal dialog.
   *
   * @param object the AppDbObject to edit
   */
  public void edit(AppDbObject object) {
    edit(object, false);
  }
  
  
  /**
   * Edits an {@code AppDbObject} in a modal dialog.
   *
   * @param object the AppDbObject to edit
   * @param comp optional component to determine the window owner, null = no owner
   *
   * @return the (possibly reloaded) object, null if cancel.
   */
  public AppDbObject editModal(AppDbObject object, Component comp) {
    return getDialogPool().useModalDialog(comp, object, true);
  }
  
  /**
   * Edits an {@code AppDbObject} in a modal dialog.
   *
   * @param object the AppDbObject to edit
   *
   * @return the (possibly reloaded) object, null if cancel.
   */
  public AppDbObject editModal(AppDbObject object) {
    return getDialogPool().useModalDialog(null, object, true);
  }
  
  
  /**
   * Deletes an {@code AppDbObject}.<br>
   * The user will be prompted for confirmation.
   * If the delete fails, an error message will be displayed.
   *
   * @param object the AppDbObject to delete
   * @return true if deleted, false if user aborted or delete error.
   */
  public boolean delete(AppDbObject object) {
    
    if (FormQuestion.yesNo(MessageFormat.format(
            Locales.bundle.getString("Are_you_sure_to_delete_{0}_{1}_?"), 
            object.getSingleName(), object))) {
      
      boolean oldCommit = object.getDb().begin("delete interactive");
      try {
        if (object.isRemovable() == false) {
          throw new ApplicationException(Locales.bundle.getString("object_is_not_allowed_to_be_removed"));
        }
        SecurityResult sr = object.getContextDb().getAppUserInfo().getSecurityManager().privilege(object, Security.WRITE);
        if (sr.isDenied()) {
          throw new ApplicationException(sr.explain(Locales.bundle.getString("you_are_not_allowed_to_remove_this_object")));
        }
        if (object.delete() == false) {
          throw new ApplicationException(Locales.bundle.getString("couldn't_delete"));
        }
        object.getDb().commit(oldCommit);
        return true;
      }
      catch (Exception e) {
        object.getDb().rollback(oldCommit);
        FormInfo.print(e.getMessage());
      }
    }    
    return false;
  }
  
}
