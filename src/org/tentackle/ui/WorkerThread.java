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

// $Id: WorkerThread.java 451 2009-01-24 18:34:03Z harald $

package org.tentackle.ui;

import javax.swing.JDialog;
import java.awt.EventQueue;


/**
 * A worker thread.
 * <p>
 * The thread will dispose a dialog if terminated.
 * <p>
 * The specified {@link Runnable} must either monitor whether the thread
 * got interrupt()ed by checking {@link #isInterrupted()}
 * or override {@link #interrupt()} and terminate the thread gracefully.
 *
 * @see WorkerDialog
 * @author harald
 */
public class WorkerThread extends Thread {
  
  private Runnable      todo;           // what's to do?
  private JDialog       workerDialog;   // worker dialog
  private boolean       modal;          // modality of the dialog
  private String        title;          // the title to set

  /**
   * Creates a worker thread.
   * 
   * @param todo  the runnable for this thread
   * @param modal true if dialog should be set to modal
   * @param title the dialog's title, null if leave unchanged
   * @param workerDialog the dialog that will be disposed when thread terminates, 
   *        null if {@link WorkerDialog}
   */
  public WorkerThread (Runnable todo, boolean modal, String title, JDialog workerDialog) {
    super();
    
    this.todo  = todo;
    this.modal = modal;
    this.title = title;
    this.workerDialog = workerDialog == null ? new WorkerDialog(this) : workerDialog;

    if (title != null) {
      this.workerDialog.setTitle(title);
    }
    this.workerDialog.setModal(modal);
    
    setPriority(NORM_PRIORITY);
  }
  
  /**
   * Creates a worker thread using an {@link WorkerDialog}.
   * 
   * @param todo  what's to do in the run-method?
   * @param modal true if dialog should be set to modal
   * @param title the dialog's title, null if leave unchanged
   */
  public WorkerThread (Runnable todo, boolean modal, String title) {
    this(todo, modal, title, null);
  }
  
  /**
   * Creates a worker thread using a modal {@link WorkerDialog}
   * and a given title.<br>
   * This is the default case.
   * 
   * @param todo  what's to do in the run-method?
   * @param title the dialog's title, null if leave unchanged
   */
  public WorkerThread (Runnable todo, String title) {
    this(todo, true, title, null);
  }

  
  
  /**
   * Gets the worker dialog.
   * 
   * @return the worker dialog (usually a {@link WorkerDialog})
   */
  public JDialog getWorkerDialog() {
    return workerDialog;
  }
  


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to dispose the dialog.
   */
  @Override
  public void run() {
    // do what has to be done
    if (todo != null) {
      todo.run();
    }
    // dispose from event queue
    EventQueue.invokeLater (new Runnable() {
      public void run() {
        workerDialog.dispose();
      }
    });
  }
  

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to show the dialog.
   * The dialog will be shown by invokeLater. This
   * allows the method to return immediately even if the dialog is modal.
   */
  @Override
  public void start() {
    // show the dialog
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        workerDialog.pack();
        workerDialog.setVisible(true);
        // notice: although this method blocks if modal dialog
        // it will not block the event queue! Nice trick... ;-)
      }
    });
    super.start();
  }
  
  
  /**
   * Starts the workerthread and waits until the modal
   * worker dialog is closed.
   */
  public void startAndWait() {
    super.start();
    workerDialog.pack();
    workerDialog.setVisible(true);  // this blocks if dialog is modal
  }

}
