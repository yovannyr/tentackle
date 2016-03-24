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

// $Id: FormSpinField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import org.tentackle.plaf.PlafGlobal;



/**
 * Generic spinnable field.
 *
 * @author harald
 */
public class FormSpinField extends FormComponentPanel implements ValueListener {

  private GridBagLayout gridBagLayout = new GridBagLayout();
  private FormButton upButton         = new FormButton();
  private FormButton downButton       = new FormButton();

  
  /**
   * Creates a spin field.
   * 
   * @param editingComponent the form component
   */
  public FormSpinField(FormComponent editingComponent) {
    super(editingComponent);
    initComponents();
  }

  
  /**
   * Creates a spin field with a default editor {@link IntegerFormField}.
   */
  public FormSpinField () {
    this (new IntegerFormField());
  }

  
  
  /**
   * Sets the form component to spin up or down.
   * 
   * @param comp the editor component
   */
  public void setFormField(FormComponent comp)  {
    if (getFormComponent() != null)  {
      removeValueListener(this);
      remove((Component)getFormComponent());
    }
    super.setFormComponent(comp);
    addValueListener(this);
    add((Component)comp, new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
  }


  private void initComponents() {
    this.setLayout(gridBagLayout);
    upButton.setIcon(PlafGlobal.getIcon("spinup_mini"));
    upButton.setMargin(new Insets(0, 2, 0, 2));
    upButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        upButton_actionPerformed(e);
      }
    });
    downButton.setIcon(PlafGlobal.getIcon("spindown_mini"));
    downButton.setMargin(new Insets(0, 2, 0, 2));
    downButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        downButton_actionPerformed(e);
      }
    });
    this.add(upButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    this.add(downButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
  }


  @Override
  public void setChangeable (boolean flag)  {
    setAllChangeable(flag);   // auch die Buttons
  }

  @Override
  public boolean isChangeable() {
    return isAllChangeable();
  }



  /**
   * Adds a SpinListener.
   *
   * @param listener the spin listener to add
   */
  public synchronized void addSpinListener (SpinListener listener) {
     listenerList.add (SpinListener.class, listener);
  }

  /**
   * Removes a SpinListener.
   *
   * @param listener the spin listener to remove
   */
  public synchronized void removeSpinListener (SpinListener listener) {
     listenerList.remove (SpinListener.class, listener);
  }

  
  /**
   * Notifies all SpinListeners that the field has
   * to be incremented.
   */
  public void fireIncrement () {
    Object listeners[] = listenerList.getListenerList();
    if (listeners != null)  {
      SpinEvent evt = null;
      for (int i = listeners.length-2; i >= 0; i -= 2)  {
        if (listeners[i] == SpinListener.class)  {
          if (evt == null) {
            evt = new SpinEvent(this, SpinEvent.INCREMENT);
          }
          ((SpinListener)listeners[i+1]).increment(evt, this);
        }
      }
    }
  }

  
  /**
   * Notifies all SpinListeners that the field has
   * to be decremented.
   */
  public void fireDecrement () {
    Object listeners[] = listenerList.getListenerList();
    if (listeners != null)  {
      SpinEvent evt = null;
      for (int i = listeners.length-2; i >= 0; i -= 2)  {
        if (listeners[i] == SpinListener.class)  {
          if (evt == null) {
            evt = new SpinEvent(this, SpinEvent.DECREMENT);
          }
          ((SpinListener)listeners[i+1]).decrement(evt, this);
        }
      }
    }
  }



  public void valueChanged (ValueEvent e)  {
    fireValueChanged();
  }


  public void valueEntered (ValueEvent e)  {
    fireValueEntered();
  }


  /**
   * Handles the UP button action.
   * 
   * @param e the action event
   */
  protected void upButton_actionPerformed(ActionEvent e) {
    fireIncrement();
    fireValueEntered();
  }

  /**
   * Handles the DOWN button action.
   * 
   * @param e the action event
   */
  protected void downButton_actionPerformed(ActionEvent e) {
    fireDecrement();
    fireValueEntered();
  }

}