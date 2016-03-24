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

// $Id: FormFrame.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.event.EventListenerList;
import org.tentackle.util.StringHelper;



/**
 * Extended {@code JFrame} implementing {@code FormWindow} (and {@code FormContainer}).
 *
 * @author harald
 */
public class FormFrame extends JFrame implements FormWindow, ContainerListener {
  
  private String helpURL = null;                // default help for this container  
  private TooltipDisplay tooltipDisplay = null; // to automatically display tooltips  
  private boolean honourAllChangeable = true;   // honour the allChangeable-request
  private boolean allChangeable = true;         // last operation from setAllChangeable()
  private boolean autoPosition  = false;        // automatically compute position on screen
  private EventListenerList listeners = null;   // special listeners (not managed by AWT)
  private int uiVersion = 0;                    // to track LookAndFeel changes
  private boolean keepChanged = false;          // keep changed values?
  private long autoClose = 0;                   // timeout to close in seconds
  private long lastValuesChanged;               // time of last change
  private FormWindow relatedWindow;             // related (owner) window
  private Window parentWindow;                  // the (cached) parent window
  private boolean parentWindowLoaded;           // true if parent window cached  
  

  /**
   * Creates a new, initially invisible <code>FormFrame</code> with the 
   * specified title.
   *
   * @param title the title for the frame, null if no title
   * @see JFrame#JFrame(java.lang.String)
   */
  public FormFrame (String title) {
    // JFrame does not explicitly allow a null title (?)
    super(title == null ? StringHelper.emptyString : title);
    // setup
    FormHelper.setDefaultFocusTraversalPolicy(this);    // set traversal policy
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    getContentPane().addContainerListener(this);
    setAutoClose(FormHelper.getAutoClose());
  }
  
  /**
   * Constructs a new <code>FormFrame</code> that is initially invisible
   * without at title.
   * @see JFrame#JFrame() 
   */
  public FormFrame () {
    this(null);
  }


  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to allow FormHelper keeping track of windows
   */
  @Override
  protected void processWindowEvent(WindowEvent e)  {
    FormHelper.processWindowEvent(e);
    super.processWindowEvent(e);
  }
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to autolocate window if possible
   */
  @Override
  public void pack ()  {
    super.pack();
    if (autoPosition) {
      alignLocation();
    }
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden in order to bring modal windows
   * to front in case accidently covered by another window.
   * This is the case whenever a modal dialog is not owned
   * by the window covering it.
   * It solves the problem of "freezing" an application because
   * the user clicked on another window.
   */
  @Override
  public void paint(Graphics g) {
    super.paint(g);
    FormHelper.modalToFront();
  }  
  
  
  
  
  // --------------------- implements ContainerListener -----------------------
  

  public void componentAdded(ContainerEvent e)  {
    Component comp = e.getChild();
    if (comp instanceof FormComponent)  {
      // tell child that next invocation of getParentWindow() should walk up the tree again
      ((FormComponent)comp).invalidateParentInfo();
    }
    else {
      FormHelper.invalidateParentInfo(comp);
    }
  }

  public void componentRemoved(ContainerEvent e)  {
    componentAdded(e);    // same code
  }


  
  
  // --------------------- implements FormContainer -----------------------


  public Window getParentWindow() {
    if (!parentWindowLoaded) {
      parentWindow = FormHelper.getParentWindow(this);
      parentWindowLoaded = true;
    }
    return parentWindow;
  }
  
  public void invalidateParentInfo()  {
    parentWindowLoaded = false;
    FormHelper.invalidateParentInfo(getContentPane());
  }
  
  
  public void setFormValues ()  {
    if (keepChanged) {
      setFormValuesKeepChanged();
    }
    else {
      FormHelper.setFormValue(getContentPane());
    }
  }
  
  public void setFormValuesKeepChanged ()  {
    FormHelper.setFormValueKeepChanged (getContentPane());
  }

  
  public void getFormValues ()  {
    FormHelper.getFormValue (getContentPane());
  }

  
  public void saveValues()  {
    FormHelper.saveValue(getContentPane());
  }
  
  
  public boolean areValuesChanged() {
    return FormHelper.isValueChanged(getContentPane()); 
  }

  
  public void triggerValuesChanged()  {
    lastValuesChanged = System.currentTimeMillis();
  }
  
  
  public void setAllChangeable (boolean flag) {
    if (honourAllChangeable)  {
      FormHelper.setChangeable (this, flag);
      allChangeable = flag;
    }
  }

  public boolean isAllChangeable()  {
    return allChangeable;
  }
  
  
  public void setHonourAllChangeable(boolean flag) {
    this.honourAllChangeable = flag;
  }
  
  public boolean isHonourAllChangeable() {
    return this.honourAllChangeable;
  }


  public void setHelpURL(String helpURL)  {
    this.helpURL = helpURL;
  }
  
  public String getHelpURL() {
    return helpURL;
  }
  
  public void showHelp() {
    FormHelper.openHelpURL(this);
  }
  

  
  
  // --------------------- implements FormWindow ------------------------------
  
  
  public int getUIVersion() {
    return uiVersion;
  }  
  
  public void setUIVersion(int version) {
    this.uiVersion = version;
  }
  
  public void setTooltipDisplay(TooltipDisplay display) {
    tooltipDisplay = display;
  }
  
  public TooltipDisplay getTooltipDisplay() {
    return tooltipDisplay;
  }

  
  public void fireFormWrappedFocus(FormWrapEvent evt) {
    if (listeners != null)  {
      Object[] lList = listeners.getListenerList();
      for (int i = lList.length-2; i>=0; i-=2) {
          if (lList[i] == FormWrapListener.class) {
              ((FormWrapListener)lList[i+1]).formWrapped(evt);
          }
      }
    }
  }
  
  
  public void addFormWrapListener(FormWrapListener l) {
    if (listeners == null)  {
      listeners = new EventListenerList();
    }
    listeners.add(FormWrapListener.class, l);
  }
  
  public void removeFormWrapListener(FormWrapListener l) {
    if (listeners != null)  {
      listeners.remove(FormWrapListener.class, l);
    }
  }
  

  public void setAutoPosition(boolean flag) {
    this.autoPosition = flag;
  }

  public boolean isAutoPosition()  {
    return autoPosition;
  }

  /**
   * {@inheritDoc}
   * <p>
   * FormFrames never have an owner (except the default shared swing object,
   * which is no FormWindow).
   */
  public void setRelatedWindow(FormWindow relatedWindow) {
    this.relatedWindow = relatedWindow;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * FormFrames never have an owner (except the default shared swing object,
   * which is no FormWindow).
   */
  public FormWindow getRelatedWindow() {
    return relatedWindow;
  }
  
  
  public void alignLocation() {
    Point location;           // new location
    if (isShowing()) {
      // check if position is still ok
      location = FormHelper.getAlignedLocation(this, this.getLocation());   // align location if necessary
    }
    else  {
      // initial align
      location = FormHelper.getPreferredLocation(this, getOwner());         // position to preferred location
    }
    if (getLocation().equals(location) == false)  {
      setLocation (location);   // set new location
    }
  }
  
  
  public void setKeepChangedValues(boolean keepChanged)  {
    this.keepChanged = keepChanged;
  }

  public boolean getKeepChangedValues() {
    return keepChanged;
  }
  
  
  public long getTimeOfLastValuesChanged() {
    return lastValuesChanged;
  }
  
  public void setTimeOfLastValuesChanged(long millis) {
    lastValuesChanged = millis;
  }
  
  
  public void setAutoClose(long autoClose)  {
    this.autoClose = autoClose;
  }
  
  public long getAutoClose()  {
    return autoClose;
  }
  
  public boolean isAutoCloseable() {
    return autoClose > 0;
  }
  
  public boolean checkAutoClose() {
    return isAutoCloseable() && isVisible() &&
           lastValuesChanged + autoClose < System.currentTimeMillis() &&
           !areValuesChanged();
  }
  
}
