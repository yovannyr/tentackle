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

// $Id: FormHelper.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.ui;

import java.awt.ActiveEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import org.tentackle.util.StringHelper;
import org.tentackle.util.URLHelper;





/**
 * Utilities for the UI layer.
 *
 * @author harald
 */
public class FormHelper {
  
  
  /**
   * true if retrieving and storing preferences refers to the
   * systemRoot only. This feature is nice to configure a system for all users.
   */
  public static boolean useSystemPreferencesOnly = false;
  
  /**
   * true if user should not be allowed to write preferences
   */
  public static boolean preferencesAreReadOnly = false;

  
  
  

  // one instance of the focus traversal policy for SwingForms-Containers
  private static FormFocusTraversalPolicy defaultFocusTraversalPolicy = new FormFocusTraversalPolicy();

  // Set containing all windows (dialogs and frames, visible or iconified)
  private static Set<Window> windows;
  private static EventListenerList windowListeners;
  private static List<Dialog> modalDialogs;   // modal Dialogs in order of creation (List of Dialogs)

  private static int uiVersion;           // to track LookAndFeel changes
  
  private static String helpURL;          // prefix for online help, e.g.: http://localhost/manual/index.html
  
  private static FormEventQueue eventQueue;   // Tentacle uses an extended EventQueue
  
  private static boolean requestFocusLaterPending;    // true if a request is pending
  
  private static List<Runnable> localeRunnables = new ArrayList<Runnable>();    // registered runnables to invoke if the locale changes
  
  
  
  
  
  // to be executed once at loadtime
  static {
    
    GUIExceptionHandler.install(false);    // install GUI Exception handler if not yet done by app
    
    // create the window-Set
    windows = new HashSet<Window>();
    windowListeners = new EventListenerList();
    modalDialogs = new ArrayList<Dialog>();
    
    // register the multiline tooltip
    try {
      String multiLineToolTipUIClassName =
                  "org.tentackle.ui.MultiLineToolTipUI";    // NOI18N
      UIManager.put("ToolTipUI", multiLineToolTipUIClassName);  // NOI18N
      UIManager.put(multiLineToolTipUIClassName,
                Class.forName(multiLineToolTipUIClassName));
    } 
    catch (ClassNotFoundException cnfe) {
      UIGlobal.logger.severe("MultiLine ToolTip UI class not found: " + cnfe.toString());
    }
  }

  
  

  
  
  
  
  /**
   * Registers a runnable which will be invoked for each Locale change.
   * There is no unregister because locales are always static.
   * 
   * @param runnable the runnable
   */
  public static void registerLocaleRunnable(Runnable runnable) {
    localeRunnables.add(runnable);
  }
  
  /**
   * Invokes all locale runnables.
   * @see #registerLocaleRunnable(java.lang.Runnable) 
   */
  public static void triggerLocaleChanged() {
    // these both must always come first because other localeRunnables may refer to it
    org.tentackle.util.Locales.reload();
    StringHelper.loadFormats();   
    // now the others... (don't use iterator, cause of concurrent modifications)
    for (int i=0; i < localeRunnables.size(); i++) {
      localeRunnables.get(i).run();
    }
  }
  
  
  
  /**
   * Gets the eventqueue.<br>
   * The method installs the FormEventQueue if not yet done.
   * This is the preferred method to get the event queue in an application.
   * 
   * @return the form event queue
   */
  public synchronized static FormEventQueue getEventQueue()  {
    // install special EventQueue:
    if (eventQueue == null) {
      eventQueue = new FormEventQueue();
      Toolkit.getDefaultToolkit().getSystemEventQueue().push(eventQueue);
    }
    return eventQueue;
  }
  
  
  
  
  
  /**
   * A simple event to detect when the event queue becomes empty.
   * We're using a PaintEvent which has the lowest priority, so we're
   * sure that InvocationEvents (i.e. from invokeLater()) will have been
   * processed as well.
   */
  private static final Rectangle dummyRectangle = new Rectangle();    // we need a dummy rectangle cause of EventQueue.mergePaintEvents
  private static final Component dummyComponent = new JLabel();
  
  private static class EmptyEvent extends PaintEvent implements ActiveEvent {
    
    private boolean dispatched = false;
    private boolean queueEmpty = false;
    
    EmptyEvent() {
      // UPDATE does the trick, see EventQueue.getPriority()
      super(dummyComponent, PaintEvent.UPDATE, dummyRectangle);
    }
    
    boolean isDispatched() {
      synchronized(this) {
        return dispatched;
      }
    }
    
    boolean isEventQueueEmpty() {
      synchronized(this) {
        return queueEmpty;
      }
    }
    
    public void dispatch() {
      synchronized(this) {
        queueEmpty = getEventQueue().peekEvent() == null;
        dispatched = true;
        notifyAll();
      }
    }
  }
  
  
  /**
   * Waits for the event queue to become empty.<br>
   * This method must not be invoked from the GUI thread!
   * The method waits until the q is empty.
   */
  public static void waitForEmptyEventQueue() {
    EventQueue q = getEventQueue();
    if (EventQueue.isDispatchThread()) {
      throw new Error("waitForEmptyEventQueue() invoked from within dispatch thread!"); // NOI18N
    }
    boolean queueEmpty = false;
    while (!queueEmpty) {
      // post a new event with lowest priority
      EmptyEvent e = new EmptyEvent();
      q.postEvent(e);
      synchronized(e) {
        while (!e.isDispatched()) {
          try {
            e.wait();   // wait for being dispatched
          } 
          catch (InterruptedException ie) {
            // ignore
          }
        }
        // event has been dispatched
        queueEmpty = e.isEventQueueEmpty();
      }
    }
  }
  
  
  
  
  
  
  /**
   * for windows with autoClose feature enabled
   */
  private static long autoClose;                      // global autoclose for all FormWindows, default is off.
  private static AutoCloseThread autoCloseThread;     // only started if at least one window enables autoclose
  
  private static class AutoCloseThread extends Thread {
    @Override
    public void run() {
      try {
        boolean keepRunning;
        do  {
          sleep(1000);
          keepRunning = false;
          synchronized (windows)  {
            for (Window w: windows) {
              if (w.isVisible() && 
                  w instanceof FormWindow && ((FormWindow)w).isAutoCloseable())  {
                keepRunning = true;
                final FormWindow fw = (FormWindow)w;
                if (fw.checkAutoClose())  {
                  EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      ((Window)fw).dispose();
                      if (UIGlobal.logger.isFineLoggable()) {
                        UIGlobal.logger.fine("autoclosing " + fw); // NOI18N
                      }
                    }
                  });
                }
              }
            }
          }
        } while (keepRunning);
      }
      catch (InterruptedException e) {}
      // terminate
      synchronized(windows) {   // sync with addWindow()
        if (UIGlobal.logger.isFineLoggable()) {
          UIGlobal.logger.fine("autoclose-thread stopped"); // NOI18N
        }
        autoCloseThread = null;
      }
    }
  }
  

  
  /**
   * Sets the global autoclose feature for all newly created Windows.
   *
   * @param ms timeout in milliseconds. Default is 0.
   */
  public static void setAutoClose(long ms) {
    autoClose = ms;    // for all newly created dialogs
  }
  
  
  /**
   * Gets the global autoclose feature for all newly created Windows.
   * @return the timeout in milliseconds. Default is 0.
   */
  public static long getAutoClose() {
    return autoClose;
  }
  
  
  
  
  
  
  
  
  /**
   * Brings the current modal dialogs toFront.
   */
  public static void modalToFront() {
    int size = modalDialogs.size();
    if (size > 0) {
      final Dialog d = modalDialogs.get(size-1);
      // works with KDE only if KDE-focus is set to "focus under mouse".
      // works with windows perfectly.
      // works with gnome and sawfish but not with metacity.
      
      /**
       * get it painted last to avoid loops
       */
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          d.toFront();
        }
      });
    }
  }
  

  
  
  
  
  /**
   * notified all window-listeners
   */
  private static void fireWindowActionPerformed(ActionEvent e)  {
      Object[] lList = windowListeners.getListenerList();
      for (int i = lList.length-2; i>=0; i-=2) {
          if (lList[i] == ActionListener.class) {
              ((ActionListener)lList[i+1]).actionPerformed(e);
          }
      }    
  }
  
  
  /**
   * Adds window to the set of windows (must be visible or iconified).
   * 
   * @param w the window to add
   */
  public static void addWindow(Window w) {
    synchronized (windows)  {
      if (windows.add(w)) {
        if (w instanceof Dialog && ((Dialog)w).isModal()) {
          modalDialogs.add((Dialog)w);
        }
        FormWindow fw = w instanceof FormWindow ? (FormWindow)w : null;
        if (fw != null && fw.isAutoCloseable()) {
          // start timer for autoclose for this window
          fw.setTimeOfLastValuesChanged(System.currentTimeMillis());
          // start the autoclose-thread if not running
          if (autoCloseThread == null) {
            autoCloseThread = new AutoCloseThread();
            autoCloseThread.start();
            if (UIGlobal.logger.isFineLoggable()) {
              UIGlobal.logger.fine("autoclose-thread started"); // NOI18N
            }
          }
        }
        fireWindowActionPerformed(new ActionEvent(w, Event.ACTION_EVENT, "add")); // NOI18N
      }
    }
  }
  
  /**
   * Removes a window from the set (i.e. window is hidden or closed now)
   * @param w the window to remove
   */
  public static void removeWindow(Window w) {
    synchronized (windows)  {
      if (windows.remove(w))  {
        if (w instanceof Dialog && ((Dialog)w).isModal()) {
          modalDialogs.remove(w);
        }
        fireWindowActionPerformed(new ActionEvent(w, Event.ACTION_EVENT, "remove")); // NOI18N
      }
    }
  }
  
  
  /**
   * Gets the current windows which are visible or iconified.
   * 
   * @return the array of windows
   */
  public static Object[] getWindows() {
    synchronized (windows)  {
      /**
       * for some reason, on some linux-desktops, a disposing/hiding/closing window does
       * not always deliver the event, which leaves the window in the list.
       * So we check again for "isShowing"
       */
      for (Iterator<Window> iter = windows.iterator(); iter.hasNext(); )  {
        Window w = iter.next();
        if (!w.isShowing()) {
          iter.remove(); 
        }
      }
      return windows.toArray();
    }
  }
  
  
  
  

  /**
   * Updates the UI of all registered windows
   */
  public static void updateUIofAllWindows () {
    uiVersion++;
    for (Window w: windows) {
      updateUIofWindow(w);
    }
  }
  
  
  /**
   * Updates the UI of a given window.
   * 
   * @param w the window
   */
  public static void updateUIofWindow(Window w) {
    if (w instanceof JDialog)  {
      SwingUtilities.updateComponentTreeUI(((JDialog)w).getRootPane());
    }
    if (w instanceof JFrame)  {
      SwingUtilities.updateComponentTreeUI(((JFrame)w).getRootPane());
    }
    if (w instanceof FormWindow)  {
      ((FormWindow)w).setUIVersion(uiVersion);
    }    
  }
  
  
  
  
  
  
  /**
   * Process a window event (from a FormDialog or FormFrame).
   * Update the list of registered windows.
   * 
   * @param e the event to process
   */
  @SuppressWarnings("fallthrough")
  public static void processWindowEvent(WindowEvent e)  {
    
    Window w = e.getWindow();
    
    switch (e.getID())  {
      
      case WindowEvent.WINDOW_OPENED:
        // we get this if window is newly opened
      case WindowEvent.WINDOW_ACTIVATED:
        // we get this if window was setVisible(false)
        addWindow(w);
        // fallthrough
      
      case WindowEvent.WINDOW_DEICONIFIED:
        if (w instanceof FormWindow && ((FormWindow)w).getUIVersion() != uiVersion)  {
          updateUIofWindow(w);
        }
        break;
              
      case WindowEvent.WINDOW_CLOSING:
        // we get this if user-app did not process this event
      case WindowEvent.WINDOW_CLOSED:
        // we get this if user app dispose()'d the window
        removeWindow(w);
        break;
    }
    
  }
  
  
  
  /**
   * Adds a listener being notified whenever the state of the 
   * current windows-list has changed.
   * 
   * @param listener the action listener
   */
  public static void addWindowActionListener(ActionListener listener)  {
    windowListeners.add(ActionListener.class, listener);
  }
  
  /**
   * Removes a listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowActionListener(ActionListener listener) {
    windowListeners.remove(ActionListener.class, listener);
  }
  

  

  
  
  
  /**
   * Sets the focus-policy for a container.
   * The method does nothing if the given container is not
   * a FormContainer.
   * 
   * @param container the container.
   */
  public static void setDefaultFocusTraversalPolicy(Container container) {
    if (container instanceof FormContainer) {
      container.setFocusTraversalPolicy(defaultFocusTraversalPolicy);
    }
  }

  
  
  /**
   * Notifies all ValueListeners (usually only one!) that the field is
   * going to be displayed and thus needs the data what to display.
   * 
   * @param c the component
   * @param listeners the listener array
   */
  public static void doFireValueChanged (FormComponent c, Object[] listeners) {
    if (c.isFireRunning() == false) {
      c.setFireRunning (true);
      if (listeners != null)  {
        ValueEvent evt = null;
        for (int i = listeners.length-2; i >= 0; i -= 2)  {
          if (listeners[i] == ValueListener.class)  {
            if (evt == null) {
              evt = new ValueEvent(c, ValueEvent.SET);
            }
            ((ValueListener)listeners[i+1]).valueChanged(evt);
          }
        }
      }
      c.setFireRunning (false);
    }
  }

  
  /**
   * Notifies all ValueListeners (usually only one!) that the field contents
   * should be moved to the actual data object.
   * 
   * @param c the component
   * @param listeners the listener array
   */
  public static void doFireValueEntered (FormComponent c, Object[] listeners) {
    if (c.isFireRunning() == false) {
      c.setFireRunning (true);
      if (listeners != null)  {
        ValueEvent evt = null;
        for (int i = listeners.length-2; i >= 0; i -= 2)  {
          if (listeners[i] == ValueListener.class)  {
            if (evt == null) {
              evt = new ValueEvent(c, ValueEvent.GET);
            }
            ((ValueListener)listeners[i+1]).valueEntered(evt);
          }
        }
      }
      c.setFireRunning (false);
      // trigger possible value changed to container
      c.triggerValueChanged();
    }
  }


  /**
   * Recursively walk down and fireValueChanged().
   * 
   * @param c the component
   */
  public static void setFormValue (Component c)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          // initialize form field with values
          ((FormComponent)next).fireValueChanged();
        }
        else if (next instanceof FormContainer) {
          ((FormContainer)next).setFormValues();
        }
        else {
          // go down the component tree recursively
          setFormValue (next);
        }
      }
    }
  }
  
  
  /**
   * Recursively walk down and fireValueChanged(),
   * but only fields that have *NOT* been changed by the user.<br>
   * Nice to mask out unchanged fields.
   * @param c the component
   */
  public static void setFormValueKeepChanged (Component c)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          if (((FormComponent)next).isValueChanged() == false)  {
            // initialize form field with values
            ((FormComponent)next).fireValueChanged();
          }
        }
        else if (next instanceof FormContainer) {
          ((FormContainer)next).setFormValuesKeepChanged();
        }
        else {
          // go down the component tree recursively
          setFormValueKeepChanged(next);
        }
      }
    }
  }
  

  /**
   * Recursively walk down and fireValueEntered().
   * @param c the component
   */
  public static void getFormValue (Component c)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          // initialize form field with values
          ((FormComponent)next).fireValueEntered();
        }
        else if (next instanceof FormContainer) {
          ((FormContainer)next).getFormValues();
        }
        else {
          // go down the component tree recursively
          getFormValue (next);
        }
      }
    }
  }
  
  

  /**
   * Recursively walk down and saveValue().
   * 
   * @param c the component
   */
  public static void saveValue (Component c)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          // initialize form field with values
          ((FormComponent)next).saveValue();
        }
        else if (next instanceof FormContainer) {
          ((FormContainer)next).saveValues();
        }
        else {
          // go down the component tree recursively
          saveValue (next);
        }
      }
    }
  }
  
  

  /**
   * Recursively walk down and check for value changed.
   * 
   * @param c the component
   * @return true if data has changed in some component
   */
  public static boolean isValueChanged (Component c)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          // initialize form field with values
          if (((FormComponent)next).isValueChanged())  {
            return true;
          }
        }
        else if (next instanceof FormContainer) {
          if (((FormContainer)next).areValuesChanged())  {
            return true;
          }
        }
        else if (next instanceof FormTable) {
          if (((FormTable)next).isDataChanged()) {
            return true; 
          }
        }
        else {
          // go down the component tree recursively
          if (isValueChanged(next)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  

  /**
   * Recursively walk up and trigger value changed in FormContainers.
   * 
   * @param c the component
   */
  public static void triggerValueChanged (Component c)  {
    while (c != null) {
      if (c instanceof FormContainer) {
        ((FormContainer)c).triggerValuesChanged();
      }
      c = c.getParent();
    }
  }



  /**
   * Recursively walk down and setChangeable().
   * 
   * @param c the component
   * @param changeable true if changeable
   */
  public static void setChangeable (Component c, boolean changeable)  {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          ((FormComponent)next).setChangeable(changeable);
        }
        else if (next instanceof FormTable)  {
          ((FormTable)next).setChangeable(changeable);
        }
        else if (next instanceof JTable)  {
          ((JTable)next).setEnabled(changeable);
        }
        else if (next instanceof AbstractButton)  {
          // use the enabled flag
          ((AbstractButton)next).setEnabled(changeable);
        }
        else if (next instanceof FormContainer) {
          // if special FormContainer: allow method to be overridden
          ((FormContainer)next).setAllChangeable(changeable);
        }
        else {
          // go down the component tree recursively for standard elements
          setChangeable (next, changeable);
        }
      }
    }
  }


  /**
   * Recursively walk down and setBackground().
   * 
   * @param c the component
   * @param background the background color
   */
  public static void setBackground (Component c, Color background)  {
    c.setBackground(background);
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        // go down the component tree recursively for standard elements
        setBackground (components[i], background);
      }
    }
  }


  /**
   * Recursively walk down and setForeground().
   * 
   * @param c the component
   * @param foreground the foreground color
   */
  public static void setForeground (Component c, Color foreground)  {
    c.setForeground(foreground);
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        // go down the component tree recursively for standard elements
        setForeground (components[i], foreground);
      }
    }
  }



  /**
   * Determines the parent-window of a component.<br>
   * Much the same as getTopLevelAncestor in JComponent, this does method
   * not return Applets.
   * @param comp the component
   * @return the parent window, null if none
   */
  public static Window getParentWindow(Component comp) {
    while (comp != null && !(comp instanceof Window)) {
      comp = comp.getParent();
    }
    // either comp is null or comp is a Window
    return (Window)comp;
  }
  
  
  /**
   * Gets the parent window of the given component.
   * If the parent is not visible, gets the related window.
   * @param comp the component
   * @return the parent or related window, null if neither nor
   */
  public static Window getVisibleParentOrRelatedWindow(Component comp) {
    Window w = getParentWindow(comp);
    if (w != null && w.isVisible() == false && w instanceof FormWindow) {
      // try to use the related window instead
      FormWindow fw = ((FormWindow)w).getRelatedWindow();
      if (fw instanceof Window) {
        w = (Window)fw;
      }
    }
    return w;
  }
  
  
  
  /**
   * Determines whether parent window a modal dialog.
   * 
   * @param comp the component
   * @return true if parent window is a modal dialog, false if no parent, not
   * a dialog or not modal
   */
  public static boolean isParentWindowModal(Component comp) {
    Window w = getParentWindow(comp);
    return w instanceof Dialog && ((Dialog)w).isModal();
  }

  
  /**
   * Packs the window containing the given component.
   *
   * @param comp the component
   * @return true if packed, false if no window
   */
  public static boolean packParentWindow(Component comp)  {
    Window w = null;
    if (comp instanceof FormComponent)  {
      w = ((FormComponent)comp).getParentWindow();
    }
    else if (comp instanceof FormContainer) {
      w = ((FormContainer)comp).getParentWindow();
    }
    else  {
      w = getParentWindow(comp);
    }
    if (w != null)  {
      w.pack();
      return true;
    }
    return false;
  }
  
  
  /**
   * Recursively walk down and invalidate the parentInfo.
   * 
   * @param c the component
   */
  public static void invalidateParentInfo(Component c) {
    if (c instanceof Container) {
      Component[] components = ((Container)c).getComponents();
      for (int i = 0; i < components.length; i++) {
        Component next = components[i];
        if (next instanceof FormComponent)  {
          // initialize form field with values
          ((FormComponent)next).invalidateParentInfo();
        }
        else if (next instanceof FormContainer) {
          ((FormContainer)next).invalidateParentInfo();
        }
        else {
          // go down the component tree recursively
          invalidateParentInfo(next);
        }
      }
    }
  }
  
  
  
  /**
   * Requests focus by EventQueue.invokeLater().
   * Also sets a flag that such a request is pending.
   * @param c the component
   */
  public static void requestFocusLater(final Component c) {
    requestFocusLaterPending = true;
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        c.requestFocusInWindow();
      }
    });
  }
  
  
  
  /**
   * Sets the wait-cursor.<br>
   * Determines the parent or related window and applies the cursor.
   * 
   * @param comp the related component
   */
  public static void setWaitCursor(Component comp) {
    Window w = getVisibleParentOrRelatedWindow(comp);
    Cursor c = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    if (w instanceof RootPaneContainer) {
      Component glassPane = ((RootPaneContainer)w).getGlassPane();
      if (glassPane != null)  {
        glassPane.setCursor(c);
        glassPane.setVisible(true);   // this trick really shows the cursor always ;-)
      }
    }
    if (w != null)  {
      // sometimes glassPane-trick doesn't work
      w.setCursor(c);
    }
  }
  
  
  /**
   * Sets the default-cursor.<br>
   * Determines the parent or related window and applies the cursor.
   * 
   * @param comp the related component
   */
  public static void setDefaultCursor(Component comp) {
    Window w = getVisibleParentOrRelatedWindow(comp);
    Cursor c = Cursor.getDefaultCursor();
    if (w instanceof RootPaneContainer) {
      Component glassPane = ((RootPaneContainer)w).getGlassPane();
      if (glassPane != null)  {
        glassPane.setCursor(c);
        glassPane.setVisible(false);
      }
    }
    if (w != null)  {
      // sometimes glassPane-trick doesn't work
      w.setCursor(c);
    }
  }
    
  
  
  
  
  

  /**
   * Calculates the location of a window so that it will be centered on the screen.
   * @param window the window
   * @return the location (top left corner)
   */
  public static Point getCenteredLocation(Window window)  {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension windowSize = window.getSize();
    // align sizes (for computation below)
    if (windowSize.width > screenSize.width)  {
      windowSize.width = screenSize.width;
    }
    if (windowSize.height > screenSize.height)  {
      windowSize.height = screenSize.height;
    }
    // center
    return new Point ((screenSize.width  - windowSize.width)  / 2,
                      (screenSize.height - windowSize.height) / 2);
  }

  
  /**
   * Calculates the position of a window on the screen so that
   * it is being display in an optimal manner.
   *
   * @param window the window to be positioned on the screen
   * @param owner the window to which the window will be related to
   * @return the location
   */
  public static Point getPreferredLocation(Window window, Window owner)  {

    Point location;   // the returned top left corner

    // place in the middle of the owner if possibe
    if (owner != null && owner.isShowing()) {   // isShowing cause of SwingUtilities.SharedOwnerFrame
      Dimension windowSize = window.getSize();
      Dimension ownerSize  = owner.getSize();
      location    = owner.getLocation();
      location.x += (ownerSize.width - windowSize.width) / 2;
      location.y += (ownerSize.height - windowSize.height) / 2;
    }
    else  {
      // not much we can do: center it
      location = getCenteredLocation(window);
    }
    
    return getAlignedLocation(window, location);
  }
  
  
  /**
   * Calculates the location of a window so that it
   * is completely visible on the screen, using a "free" spot.
   * 
   * @param window the current window
   * @param location the desired (not necessarily current!) location
   * @return the location
   */
  public static Point getAlignedLocation(Window window, Point location) {

    Dimension windowSize = window.getSize();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    int maxWidth  = screenSize.width * 19 / 20;   // leave 5% off from right/bottom
    int maxHeight = screenSize.height * 19 / 20;
    int minX      = screenSize.width / 20;
    int minY      = screenSize.height / 20;

    if (location.x + windowSize.width > maxWidth) {
      location.x = maxWidth - windowSize.width;
    }
    if (location.x < 0) {
      location.x = 0;
    }
    else if (location.x < minX && location.x + windowSize.width < maxWidth) {
      location.x = minX;
    }

    if (location.y + windowSize.height > maxHeight) {
      location.y = maxHeight - windowSize.height;
    }
    if (location.y < 0) {
      location.y = 0;
    }
    else if (location.y < minY && location.y + windowSize.height < maxHeight) {
      location.y = minY;
    }

    return getFreeLocation(window, location);
  }
  
  
  
  /**
   * steps the window ne/so/sw/se if it would overlay with another.
   */
  private static final int STEP_X = 32;
  private static final int STEP_Y = 24;
  
  private static Point getFreeLocation(Window window, Point startLocation)  {
    
    // get screensize
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    // compute the center of the window
    int x = startLocation.x + window.getWidth()/2;
    int y = startLocation.y + window.getHeight()/2;
    
    // initial diff-stepping
    int dx;
    int dy;
    
    if (x > screenSize.width/2) {
      dx = -STEP_X;
    }
    else {
      dx = STEP_X;
    }
    
    if (y > screenSize.height/2) {
      dy = -STEP_Y;
    }
    else {
      dy = STEP_Y;
    }
    
    for (int loop=0; loop < 4; loop++)  {
      
      boolean abort = false;
      Point location = new Point(startLocation);
      
      while (!abort && isWindowOverlaying(window, location, dx, dy))  {
        location.x += dx;
        location.y += dy;
        if (location.x < 0) {
          location.x = 0;
          abort = true;
        }
        if (location.x + window.getWidth() > screenSize.width)  {
          location.x = screenSize.width - window.getWidth();
          abort = true;
        }
        if (location.y < 0) {
          location.y = 0;
          abort = true;
        }
        if (location.y + window.getHeight() > screenSize.height)  {
          location.y = screenSize.height - window.getHeight();
          abort = true;
        }      
      }
      
      if (abort == false) {
        // !isWindowOverlaying
        return location;
      }
      
      // try other direction clockwise
      if (dx > 0 && dy > 0) {
        dx = -STEP_X;
      }
      else if (dx < 0 && dy > 0) {
        dy = -STEP_Y;
      }
      else if (dx < 0 && dy < 0) {
        dx = STEP_X;
      }
      else if (dx > 0 && dy < 0) {
        dy = STEP_Y;
      }
    }
    
    return startLocation;
  }
  
  
  
  
  /**
   * checks if window would overlay another that belongs to the same owner
   */
  private static boolean isWindowOverlaying(Window window, Point location, int dx, int dy)  {
    Window owner = window.getOwner();
    if (dx < 0) {
      dx = -dx;
    }
    if (dy < 0) {
      dy = -dy;
    }
    for (Window w: windows)  {
      if (w.isShowing()) {
        Window o = w.getOwner();
        if (w != window && o == owner &&
            ((location.x <= w.getX() + dx && 
              location.x + window.getWidth() + dx >= w.getX() + w.getWidth()) ||
             (location.y <= w.getY() + dy &&
              location.y + window.getHeight() + dy >= w.getY() + w.getHeight())))  {
          return true;
        }
      }
    }    
    return false;
  }
  
  
  

  /**
   * Registers some default Keyboard Actions for Components.
   * Will also replace some standard actions!
   * @param comp the component
   */
  public static void setupDefaultBindings (final JComponent comp)  {

    // ENTER = to next component (override old action!)
    Action enterAction = new AbstractAction ("focusNextFormComponent") { // NOI18N
      public void actionPerformed (ActionEvent e)  {
        if (comp instanceof FormFieldComponent)  {
          ((FormFieldComponent)comp).doActionPerformed();
        }
        if (comp instanceof FormTextArea) {
          ((FormTextArea)comp).doSmartEnter();
        }
        else  {
          if (comp instanceof FormComponent)  {
            requestFocusLaterPending = false;   // to detect requests during prepareFocusLost()
            ((FormComponent)comp).prepareFocusLost();
            if (!requestFocusLaterPending)  {
              /**
               * in case the component issued a requestFocusLater(), we should
               * not transfer focus to the next component!
               */
              comp.transferFocus();
            }
          }
          else  {
            comp.transferFocus();
          }
        }
      }
    };
    comp.getActionMap().put(enterAction.getValue(Action.NAME), enterAction);
    comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                           enterAction.getValue(Action.NAME));

    // Shift-ENTER = to previous component
    Action enterShiftAction = new AbstractAction ("focusPreviousFormComponent") { // NOI18N
      public void actionPerformed (ActionEvent e)  {
        if (comp instanceof FormFieldComponent)  {
          ((FormFieldComponent)comp).doActionPerformed();
        }    
        if (comp instanceof FormComponent)  {
          requestFocusLaterPending = false;   // to detect requests during prepareFocusLost()
          ((FormComponent)comp).prepareFocusLost();
          if (!requestFocusLaterPending)  {
            comp.transferFocusBackward();
          }
        }
        else  {
          comp.transferFocusBackward();
        }
      }
    };
    comp.getActionMap().put(enterShiftAction.getValue(Action.NAME), enterShiftAction);
    comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK),
                           enterShiftAction.getValue(Action.NAME));
    
    if (comp instanceof FormComponent)  {
      // help
      final FormComponent fc = (FormComponent)comp;
      Action helpAction = new AbstractAction ("showHelp") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.showHelp();
        }
      };
      comp.getActionMap().put(helpAction.getValue(Action.NAME), helpAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0),
                             helpAction.getValue(Action.NAME));
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                             helpAction.getValue(Action.NAME));
    }
    
    if (comp instanceof FormFieldComponent) {
      
      final FormFieldComponent fc = (FormFieldComponent)comp;
      // clear field
      Action clearAllAction = new AbstractAction ("clearText") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.clearText();
        }
      };
      comp.getActionMap().put(clearAllAction.getValue(Action.NAME), clearAllAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Event.SHIFT_MASK),
                             clearAllAction.getValue(Action.NAME));

      // toggle insert/override
      Action toggleInsertAction = new AbstractAction ("toggleInsert") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.setOverwrite(!fc.isOverwrite());
        }
      };
      comp.getActionMap().put(toggleInsertAction.getValue(Action.NAME), toggleInsertAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
                             toggleInsertAction.getValue(Action.NAME));


      // home field (leftmost position)
      Action caretLeftAction = new AbstractAction ("caretLeft") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.setCaretLeft();
        }
      };
      comp.getActionMap().put(caretLeftAction.getValue(Action.NAME), caretLeftAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
                             caretLeftAction.getValue(Action.NAME));
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.SHIFT_MASK),
                             caretLeftAction.getValue(Action.NAME));

      // rightmost position
      Action caretRightAction = new AbstractAction ("caretRight") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.setCaretRight();
        }
      };
      comp.getActionMap().put(caretRightAction.getValue(Action.NAME), caretRightAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
                             caretRightAction.getValue(Action.NAME));
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.SHIFT_MASK),
                             caretRightAction.getValue(Action.NAME));

      // override existing action
      Action upLeftAction = new AbstractAction ("upLeft") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.upLeft();
        }
      };
      comp.getActionMap().put(upLeftAction.getValue(Action.NAME), upLeftAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                             upLeftAction.getValue(Action.NAME));

      // override existing action
      Action downRightAction = new AbstractAction ("downRight") { // NOI18N
        public void actionPerformed (ActionEvent e)  {
          fc.downRight();
        }
      };
      comp.getActionMap().put(downRightAction.getValue(Action.NAME), downRightAction);
      comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                             downRightAction.getValue(Action.NAME));
    }
  }


  
  /**
   * Changes the format according to the given scale.
   * (leaves the part before the comma unchanged!)
   * @param format the decimal format
   * @param scale the scale
   */
  static public void setScale(DecimalFormat format, int scale) {
    String fmt = format.toPattern();
    boolean groupingUsed = format.isGroupingUsed();
    int groupingSize = format.getGroupingSize();
    
    // cut after last dot, if any
    int dotNdx = fmt.lastIndexOf('.');
    if (dotNdx < 0) {
      dotNdx = fmt.length();
    }
    
    if (dotNdx > 0) {
      fmt = fmt.substring(0, dotNdx);
    }
    else  {
      // set default
      fmt = "#0";
    }
    
    if (scale > 0)  {
      fmt += '.';
      for (int i=0; i < scale; i++) {
        fmt += "0";
      }
    }
    
    // apply new format
    format.applyPattern(fmt);
    
    if (dotNdx <= 0 && groupingUsed)  {
      // default has been set: restore grouping pars
      format.setGroupingSize(groupingSize);
      format.setGroupingUsed(groupingUsed);
    }
  }


  
  
  /**
   * Saves the preferences of a component.<br>
   * Use it whenever there is no table in it (FormTable provide setting too)
   * or some other scrolling regions need to be preset.
   * The method scans for all scrolling regions contained in component
   * and stores their sizes.
   *
   * @param comp is the component (usually a panel, srolling area or window)
   * @param prefName is the preferences name
   * @param system is true if store to system-preferences, else store in userprefs
   */
  public static void savePreferredSizes(Component comp, String prefName, boolean system) {
    int[] counter = new int[] { 0 };
    try {
      Preferences prefs = system ? Preferences.systemRoot().node(prefName) : Preferences.userRoot().node(prefName);
      savePreferredSizes(comp, prefs, counter);
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Einstellungen_konnten_nicht_gespeichert_werden"), ex); 
    }
  }
  
  private static void savePreferredSizes(Component comp, Preferences prefs, int[] counter) throws BackingStoreException {
    if (comp instanceof JScrollPane)  {
      counter[0]++;
      prefs.putInt("height_" + counter[0] , comp.getHeight()); // NOI18N
      prefs.putInt("width_" + counter[0], comp.getWidth()); // NOI18N
      prefs.flush();
    }
    else if (comp instanceof Container) {
      Component[] components = ((Container)comp).getComponents();
      for (int i = 0; i < components.length; i++) {
        // go down the component tree recursively for standard elements
        savePreferredSizes (components[i], prefs, counter);
      }
    }      
  }
  
  
  
  /**
   * Loads the preferences of a component.<br>
   * Use it whenever there is no table in it (FormTable provide setting too)
   * or some other scrolling regions need to be preset.
   * The method scans for all scrolling regions contained in component
   * and sets their preferred-sizes.
   *
   * @param comp is the component (usually a panel, srolling area or window)
   * @param prefName is the preferences name
   * @param system is true if load from system-preferences, else try userprefs first
   */
  public static void loadPreferredSizes(Component comp, String prefName, boolean system) {
    int[] counter = new int[] { 0 };
    try {
      Preferences sysPrefs  = Preferences.systemRoot().node(prefName);
      Preferences userPrefs = system ? null : Preferences.userRoot().node(prefName);
      loadPreferredSizes(comp, sysPrefs, userPrefs, counter);
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Einstellungen_konnten_nicht_geladen_werden"), ex); 
    }
  }
  
  private static void loadPreferredSizes(Component comp, Preferences sysPrefs, Preferences userPrefs, int[] counter) throws BackingStoreException {
    if (comp instanceof JScrollPane)  {
      counter[0]++;
      String key = "width_" + counter[0]; // NOI18N
      int width = -1;
      if (userPrefs != null)  {
        width = userPrefs.getInt(key,  width); 
      }
      if (width == -1)  {
        width = sysPrefs.getInt(key,  width); 
      }
      key = "height_" + counter[0]; // NOI18N
      int height = -1;
      if (userPrefs != null)  {
        height = userPrefs.getInt(key,  height); 
      }
      if (height == -1)  {
        height = sysPrefs.getInt(key,  height); 
      }
      if (width > 0 && height > 0)  {
        ((JScrollPane)comp).setPreferredSize(new Dimension(width, height));
      }
    }
    else if (comp instanceof Container) {
      Component[] components = ((Container)comp).getComponents();
      for (int i = 0; i < components.length; i++) {
        // go down the component tree recursively for standard elements
        loadPreferredSizes (components[i], sysPrefs, userPrefs, counter);
      }
    }
  }
  
  
  


  /**
   * Installs a menu for setting/retrieving the preferred sizes.
   *
   * @param comp is the component
   * @param prefName is the preferences name
   */
  public static void installPreferredSizeMenu(Component comp, String prefName) {
    FormHelper.PreferredSizeMouseListener ml = new FormHelper.PreferredSizeMouseListener(prefName);
    if (comp instanceof JScrollPane)  {
      comp.addMouseListener(ml);
    }
    else if (comp instanceof Container) {
      Component[] components = ((Container)comp).getComponents();
      for (int i = 0; i < components.length; i++) {
        // go down the component tree recursively for standard elements
        installPreferredSizeMenu(components[i], prefName);
      }
    }          
  }
  
  
  /**
   * A mouse listener to open a popup menu for setting/getting the preferred sizes.
   */
  public static class PreferredSizeMouseListener implements MouseListener {
    
    private String prefName;
    
    public PreferredSizeMouseListener(String prefName) {
      this.prefName = prefName;
    }

    public void mouseClicked(MouseEvent e) {
    }  
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
      processTableMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
      processTableMouseEvent(e);
    }


    private void processTableMouseEvent(MouseEvent e)  {
      if (e.isPopupTrigger())  {
        final Component comp = e.getComponent();
        if (comp != null) {
          // determine topmost window
          final Window w = getParentWindow(comp);
          final Component parent = w == null ? comp : (Component)w;
          // build menu
          JPopupMenu menu = new JPopupMenu();
          if (useSystemPreferencesOnly) {
            if (!preferencesAreReadOnly)  {
              JMenuItem saveItem = new JMenuItem(Locales.bundle.getString("Systemeinstellungen_sichern"));
              saveItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)  {
                  if (FormQuestion.yesNo(Locales.bundle.getString("Systemeinstellungen_für_dieses_Fenster_speichern?"))) {
                    savePreferredSizes(parent, prefName, true);
                  }
                }
              });
              menu.add(saveItem);
            }

            JMenuItem restoreItem = new JMenuItem(Locales.bundle.getString("Systemeinstellungen_laden"));
            restoreItem.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e)  {
                loadPreferredSizes(parent, prefName, true);
                w.pack();
              }
            });
            menu.add(restoreItem);        
          }
          else  {
            if (!preferencesAreReadOnly)  {
              JMenuItem saveItem = new JMenuItem(Locales.bundle.getString("Einstellungen_sichern"));
              saveItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)  {
                  if (FormQuestion.yesNo(Locales.bundle.getString("Benutzereinstellungen_für_dieses_Fenster_speichern?"))) {
                    savePreferredSizes(parent, prefName, false);
                  }
                }
              });
              menu.add(saveItem);
            }

            JMenuItem restoreItem = new JMenuItem(Locales.bundle.getString("Einstellungen_laden"));
            restoreItem.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e)  {
                loadPreferredSizes(comp, prefName, false);
                w.pack();
              }
            });
            menu.add(restoreItem);

            JMenuItem restoreSysItem = new JMenuItem("Systemeinstellungen laden");
            restoreSysItem.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e)  {
                loadPreferredSizes(parent, prefName, true);
                w.pack();
              }
            });
            menu.add(restoreSysItem);
          }
          menu.show(comp, e.getX(), e.getY());
        }    
      }
    }
  }
  

  
  
  /**
   * Determines the Preferences-name for a class.<br>
   * The name is built from the classname and a componentname.
   * plus the name of the table. E.g.: "/de/krake/bixworx/common/OpAusziffTableEntry/opAusziffOpTable"
   *
   * @param clazz the class
   * @param compName the name of the component
   * @return the preferences name
   */
  public static String getPreferencesName(Class clazz, String compName) {
    return "/" + clazz.getName().replace('.', '/') + "/" + compName;
  }
  

 
  /**
   * Sets the global help url prefix.
   * The prefix will be prepended to all help requests.
   * 
   * @param aHelpURL the prefix
   */
  public static void setHelpURL(String aHelpURL) {
    helpURL = aHelpURL;
  }
  
  /**
   * Gets the global help url prefix.
   * 
   * @return the prefix
   */
  public static String getHelpURL() {
    return helpURL;
  }

  
  /**
   * Opens the online help for a given component.
   * 
   * @param comp the component
   */
  public static void openHelpURL(Component comp)  {
    String url = null;
    
    while (comp != null) {
      if (comp instanceof FormComponent)  {
        url = ((FormComponent)comp).getHelpURL();
      }
      else if (comp instanceof FormContainer) {
        url = ((FormContainer)comp).getHelpURL();
      }
      else if (comp instanceof FormButton)  {
        url = ((FormButton)comp).getHelpURL();
      }
      else if (comp instanceof FormTable) {
        url = ((FormTable)comp).getHelpURL();
      }
      if (url != null) {
        break;
      }
      comp = comp.getParent();
    }
    
    if (helpURL != null) {
      if (url != null)  {
        url = helpURL + url;
      }
      else  {
        url = helpURL;
      }
    }
    
    if (url != null)  {
      try {
        FormInfo.print(MessageFormat.format(Locales.bundle.getString("opening_help_for_<{0}>_..."), url), true, 5000); // show for 5 seconds
        URLHelper.openURL(url);
      }
      catch (Exception ex) {
        FormError.printException(MessageFormat.format(Locales.bundle.getString("can't_open_help_for_<{0}>"), url), ex);
      }
    }
  }
  


}