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

// $Id: FormFieldHorizontalAlignmentEditor.java 336 2008-05-09 14:40:20Z harald $
// Created on August 6, 2002, 7:28 PM

package org.tentackle.ui;

import java.beans.PropertyEditorSupport;
import javax.swing.JLabel;



/**
 * Bean property editor for the horizontal alignment.
 * 
 * @author harald
 */
public class FormFieldHorizontalAlignmentEditor extends PropertyEditorSupport {
  
  private static final String[] options = {
     "LEFT",
     "CENTER",
     "RIGHT",
     "LEADING",
     "TRAILING"
  };
  
  /** 
   * Creates a FormFieldHorizontalAlignmentEditor 
   */
  public FormFieldHorizontalAlignmentEditor() {
  }
  
  @Override
  public String[] getTags() {
    return options;
  }
  
  @Override
  public String getAsText() {
    int alignment = ((Integer)getValue()).intValue();
    if      (alignment == JLabel.CENTER) {
      return options[1];
    }
    else if (alignment == JLabel.RIGHT) {
      return options[2];
    }
    else if (alignment == JLabel.LEADING) {
      return options[3];
    }
    else if (alignment == JLabel.TRAILING) {
      return options[4];
    }
    else {
      return options[0];
    }
  }
  
  @Override
  public void setAsText(String s) {
    if      (options[1].equals(s)) {
      setValue(new Integer(JLabel.CENTER));
    }
    else if (options[2].equals(s)) {
      setValue(new Integer(JLabel.RIGHT));
    }
    else if (options[3].equals(s)) {
      setValue(new Integer(JLabel.LEADING));
    }
    else if (options[4].equals(s)) {
      setValue(new Integer(JLabel.TRAILING));
    }
    else {
      setValue(new Integer(JLabel.LEFT));
    }
  }
  
  
  @Override
  public String getJavaInitializationString() {
    return "javax.swing.JLabel." + getAsText();
  }
  
}
