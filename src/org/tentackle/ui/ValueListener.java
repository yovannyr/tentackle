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

// $Id: ValueListener.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.util.EventListener;



/**
 * A listener that sets/gets the value of a form-field.
 * Implements the data binding in forms.
 *
 * @author harald
 */
public interface ValueListener extends EventListener {

    /**
     * Updates the view of the component with
     * the representation of the data.
     *
     * @param e the value event
     */
    public void valueChanged (ValueEvent e);

    /**
     * Updates the data according to the view of the component.
     *
     * @param e the value event
     */
    public void valueEntered (ValueEvent e);
    
}
