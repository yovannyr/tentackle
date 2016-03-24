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

// $Id: FormFocusTraversalPolicy.java 336 2008-05-09 14:40:20Z harald $
// Created on August 21, 2002, 3:57 PM

package org.tentackle.ui;

import java.awt.Component;
import java.awt.Container;
import javax.swing.LayoutFocusTraversalPolicy;



/**
 * Keyboard focus traversal policy for Tentackle forms.
 * 
 * @author harald
 */
public class FormFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
  
  private Component lastComp;   // last triggered component
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to accept only FormComponents, FormButtons, FormTables and JPasswordFields
   */
  @Override
  protected boolean accept(Component aComponent) {
    if (aComponent instanceof FormFieldComponent &&
        ((FormFieldComponent)aComponent).isChangeable() == false)  {
      // don't focus non-editable formfields (the default accept() would do)
      return false;
    }
    if ((aComponent instanceof FormComponent && ((FormComponent)aComponent).isFormTraversable()) ||
        (aComponent instanceof FormButton && aComponent.isEnabled() && ((FormButton)aComponent).isFormTraversable()) ||
        (aComponent instanceof FormTable && aComponent.isEnabled() && ((FormTable)aComponent).isFormTraversable()) ||
        aComponent instanceof javax.swing.JPasswordField) {
      return super.accept(aComponent);
    }
    return false;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to trigger {@link FormWrapEvent}.
   */
  @Override
  public Component getComponentAfter(Container aContainer,
                                     Component aComponent) {
    Component comp = super.getComponentAfter(aContainer, aComponent);
    if (aContainer instanceof FormWindow &&                     // must be a FormWindow
        aComponent instanceof FormComponent &&                  // must be for FormComponent
        comp != aComponent && lastComp != aComponent &&         // don't trigger more than once if stayed in same field
        accept(aComponent) &&                                   // don't trigger from a non-accepted field
        comp == super.getFirstComponent(aContainer))  {
      // next focusLost will trigger the event
      ((FormComponent)aComponent).setFormWrapWindow((FormWindow)aContainer);
      lastComp = aComponent;
    }
    else  {
      lastComp = null;
    }
    return comp;
  }
  
}
