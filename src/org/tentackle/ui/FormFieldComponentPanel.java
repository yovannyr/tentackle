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

// $Id: FormFieldComponentPanel.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import org.tentackle.util.StringConverter;

/**
 * A FormComponentPanel with an embedded FormFieldComponent.
 *
 * @author harald
 */
public class FormFieldComponentPanel extends FormComponentPanel implements FormFieldComponent {


  /**
   * Creates a {@code FormFieldComponentPanel} for a given {@code FormFieldComponent}.
   * 
   * @param comp the component
   */
  public FormFieldComponentPanel(FormFieldComponent comp) {
    setFormFieldComponent(comp);
  }

  /**
   * Creates a {@code FormFieldComponentPanel} for a {@link StringFormField}.
   */
  public FormFieldComponentPanel() {
    this(new StringFormField());
  }


  /**
   * Gets the embedded form field component.
   * 
   * @return the component
   */
  public FormFieldComponent getFormFieldComponent() {
    return (FormFieldComponent)getFormComponent();
  }

  /**
   * Sets the embedded form field component.
   * 
   * @param comp the component.
   */
  public void setFormFieldComponent(FormFieldComponent comp)  {
    setFormComponent(comp);
  }

  
  // ---------------------- implements FormFieldComponent --------------------
  
  public void setColumns(int columns) {
    getFormFieldComponent().setColumns(columns);
  }

  public int getColumns() {
    return getFormFieldComponent().getColumns();
  }

  public void setFormat (String format) {
    getFormFieldComponent().setFormat(format);
  }

  public String getFormat() {
    return getFormFieldComponent().getFormat();
  }

  public String doFormat(Object object) {
    return getFormFieldComponent().doFormat(object);
  }

  public void clearText() {
    getFormFieldComponent().clearText();
  }
  
  public boolean isEmpty()  {
    return getFormFieldComponent().isEmpty();
  }

  public void setText (String str)  {
    getFormFieldComponent().setText(str);
  }

  public String getText() {
    return getFormFieldComponent().getText();
  }

  public void setEraseFirst(boolean erasefirst) {
    getFormFieldComponent().setEraseFirst(erasefirst);
  }
  
  public boolean isEraseFirst() {
    return getFormFieldComponent().isEraseFirst();
  }

  public void setMaxColumns (int columns) {
    getFormFieldComponent().setMaxColumns(columns);
  }

  public int  getMaxColumns ()  {
    return getFormFieldComponent().getMaxColumns();
  }

  public  void  setConvert (char convert)  {
    getFormFieldComponent().setConvert(convert);
  }

  public  char  getConvert ()  {
    return getFormFieldComponent().getConvert();
  }

  public void setConverter(StringConverter converter) {
    getFormFieldComponent().setConverter(converter);
  }
  
  public StringConverter getConverter() {
    return getFormFieldComponent().getConverter();
  }

  public  void  setAdjust (char adjust)  {
    getFormFieldComponent().setAdjust(adjust);
  }

  public char getAdjust() {
    return getFormFieldComponent().getAdjust();
  }

  public  void  setAutoSelect (boolean autoselect)  {
    getFormFieldComponent().setAutoSelect(autoselect);
  }

  public boolean isAutoSelect()  {
    return getFormFieldComponent().isAutoSelect();
  }

  public  void  setAutoNext (boolean autonext)  {
    getFormFieldComponent().setAutoNext(autonext);
  }

  public boolean isAutoNext()  {
    return getFormFieldComponent().isAutoNext();
  }

  public  void  setAutoUpdate (boolean autoupdate)  {
    getFormFieldComponent().setAutoUpdate(autoupdate);
  }

  public boolean isAutoUpdate()  {
    return getFormFieldComponent().isAutoUpdate();
  }

  public  void  setValidChars (String str)  {
    getFormFieldComponent().setValidChars(str);
  }

  public String getValidChars ()  {
    return getFormFieldComponent().getValidChars();
  }

  public  void  setInvalidChars (String str)  {
    getFormFieldComponent().setInvalidChars(str);
  }

  public String getInvalidChars ()  {
    return getFormFieldComponent().getInvalidChars();
  }

  public  void  setFiller (char filler)  {
    getFormFieldComponent().setFiller(filler);
  }

  public char getFiller ()  {
    return getFormFieldComponent().getFiller();
  }

  public  void  setOverwrite (boolean override)  {
    getFormFieldComponent().setOverwrite(override);
  }

  public boolean isOverwrite()  {
    return getFormFieldComponent().isOverwrite();
  }
  
  public  void  setStartEditLeftmost (boolean startEditLeftmost)  {
    getFormFieldComponent().setStartEditLeftmost(startEditLeftmost);
  }

  public boolean isStartEditLeftmost()  {
    return getFormFieldComponent().isStartEditLeftmost();
  }

  public int  getErrorOffset () {
    return getFormFieldComponent().getErrorOffset();
  }
  
  public boolean isInhibitAutoSelect()  {
    return getFormFieldComponent().isInhibitAutoSelect();
  }
  
  public void setInhibitAutoSelect(boolean inhibitAutoSelect) {
    getFormFieldComponent().setInhibitAutoSelect(inhibitAutoSelect);
  }

  public boolean isCaretLeft()  {
    return getFormFieldComponent().isCaretLeft();
  }
  
  public boolean isCaretRight() {
    return getFormFieldComponent().isCaretRight();
  }
  
  public void setCaretPosition(int pos) {
    getFormFieldComponent().setCaretPosition(pos);
  }

  public int getCaretPosition() {
    return getFormFieldComponent().getCaretPosition();
  }

  public void setHorizontalAlignment(int align) {
    getFormFieldComponent().setHorizontalAlignment(align);
  }

  public int getHorizontalAlignment() {
    return getFormFieldComponent().getHorizontalAlignment();
  }

  public void setVerticalAlignment(int alignment) {
    getFormFieldComponent().setVerticalAlignment(alignment);
  }

  public int  getVerticalAlignment() {
    return getFormFieldComponent().getVerticalAlignment();
  }
  
  public void setCaretLeft() {
    getFormFieldComponent().setCaretLeft();
  }

  public void setCaretRight() {
    getFormFieldComponent().setCaretRight();
  }

  public void upLeft() {
    getFormFieldComponent().upLeft();
  }

  public void downRight() {
    getFormFieldComponent().downRight();
  }

  public void doActionPerformed() {
    getFormFieldComponent().doActionPerformed();
  }

  @Override
  public void setToolTipText(String text) {
    getFormFieldComponent().setToolTipText(text);
  }
  
  @Override
  public String getToolTipText() {
    return getFormFieldComponent().getToolTipText();
  }
  
  @Override
  public int getBaseline(int width, int height) {
    return getFormFieldComponent().getBaseline(width, height);
  }

}