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

// $Id: FormEventQueue.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;

/**
 * A replacement for the standard event queue providing
 * support for dropping key events a given amount of time.
 * 
 * @author harald
 */
public class FormEventQueue extends EventQueue {
  
  /** default milliseconds to drop keyevents for FormError, etc... **/
  public static long dropKeyEventDefaultTime = 250;
  
  
  private long dropKeyEventTime;    // drop keys in ms
  
  
  /**
   * Creates an event queue.
   */
  public FormEventQueue() {
    super();
  }

  
  /**
   * Drops all key events so far up to a given time from now.<br>
   * Useful for dialogs to prevent accidently ack (usually ENTER) by user.
   * 
   * @param millis the time in milliseconds to add the current system time
   */
  public void dropKeyEvents(long millis) {
    dropKeyEventTime = System.currentTimeMillis() + millis;
  }
  
  /**
   * Drops all key events so far up to a given time from now.<br>
   * @see #dropKeyEventDefaultTime
   */
  public void dropKeyEvents() {
    dropKeyEvents(dropKeyEventDefaultTime);
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to drop keyboard events.
   */
  @Override
  public AWTEvent getNextEvent() throws InterruptedException {
    AWTEvent event;
    do  {
      event = super.getNextEvent();
    }
    while (event instanceof KeyEvent && ((KeyEvent)event).getWhen() <= dropKeyEventTime);
    return event;
  }
  
}
