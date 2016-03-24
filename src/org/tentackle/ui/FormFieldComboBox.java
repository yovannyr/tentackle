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

// $Id: FormFieldComboBox.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serializable;
import javax.swing.JList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.tentackle.util.StringConverter;


/**
 * A {@code FormFieldComboBox} is a {@code FormComboBox} with a 
 * {@code FormFieldComponent} as its editor.
 * 
 * @author harald
 */
public class FormFieldComboBox extends FormComboBox implements FormFieldComponent {

  
  /**
   * the editor
   */
  private class FormFieldComboBoxEditor extends BasicComboBoxEditor implements Serializable {

    @Override
    public Component getEditorComponent ()  {
      return editorField;
    }

    @Override
    public void setItem (Object obj)  {
      editorField.setFormValue(obj);
    }

    @Override
    public Object getItem ()  {
      return editorField.getFormValue();
    }
  }


  /**
   * the renderer
   */
  private class FormFieldComboBoxRenderer extends BasicComboBoxRenderer {
    
    @Override
    public Component getListCellRendererComponent (JList list, Object value,
                                                   int index, boolean isSelected,
                                                   boolean cellHasFocus)  {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      // set the formatted value
      setText (editorField.doFormat(value));
      return this;
    }
  }
  
  
  
  
  private FormField editorField;    // the editor field
  
  
  /**
   * special key listener to make use of control keys
   */
  private KeyListener keyListener = new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
      if (isEditable()) {
        // editable combobox
        if (isPopupVisible())  {
          // process Arrow-Keys if popped up
          if (e.getModifiers() == 0)  {
            int index = getSelectedIndex();
            if (e.getKeyCode() == KeyEvent.VK_DOWN &&
                index < getItemCount() - 1)  {
              setFormValueIndex(index + 1);
            }
            else if (e.getKeyCode() == KeyEvent.VK_UP &&
                index > 0)  {
              setFormValueIndex(index - 1);
            }
            else if (e.getKeyCode() == KeyEvent.VK_ENTER ||
                     e.getKeyCode() == KeyEvent.VK_ESCAPE ||
                     e.getKeyCode() == KeyEvent.VK_F2) {
              hidePopup();
            }
            if (e.getKeyCode() != KeyEvent.VK_ENTER)  {
              // ENTER = make selection, else consume event
              e.consume();
            }
          }
        }
        else  {
          // popup not visible
          if (e.getKeyCode() == KeyEvent.VK_F2 ||
              (e.isControlDown() &&
               (e.getKeyCode() == KeyEvent.VK_UP ||
                e.getKeyCode() == KeyEvent.VK_DOWN ||
                e.getKeyCode() == KeyEvent.VK_ENTER)) ||
              // handy only in tables:
              (e.getKeyCode() == KeyEvent.VK_DOWN && isCaretRight() && isCellEditorUsage()) ||
              (e.getKeyCode() == KeyEvent.VK_UP && isCaretLeft() && isCellEditorUsage())) {
            // display the popup if Ctrl-Down/Enter/Space
            showPopup();
            e.consume();
          }
          else if (e.getKeyCode() == KeyEvent.VK_TAB && isCellEditorUsage())  {
            /**
             * TAB in tables will discard the input. This is not desired.
             * Map TAB to ENTER in such cases.
             */
            e.setKeyCode(KeyEvent.VK_ENTER);
          }
        }
      }
    }
  };  // key listener for special keys

    

  /**
   * special popup listsner to select first matchable item
   */
  private PopupMenuListener popupListener = new PopupMenuListener() {
    
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    /**
     * check for autoselecting first matchable item in editable
     * comboboxes when popup is shown.
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      if (isEditable()) {
        String text = getFormValueText();
        int matchIndex = getItemIndexWithLeadString(text, getModel());
        if (matchIndex >= 0)  {
          setFormValueIndex(matchIndex);
        }
      }
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }
  };
  
  

  
  
  
  /**
   * Creates a {@code FormFielComboBox} for a given FormField.
   *
   * @param editorField the FormField used as the editor field and formatting
   *        null, if standard {@link StringFormField}
   *
   */
  public FormFieldComboBox (FormField editorField) {
    super();
    setEditorField(editorField);
    setup();
  }

  /**
   * Creates a {@code FormFielComboBox} for a default {@link StringFormField}.
   */
  public FormFieldComboBox ()  {
    this((FormField)null);
  }

  /**
   * Creates a {@code FormFielComboBox} for a given FormField.
   *
   * @param editorField the FormField used for editing  and formatting,
   *        null if standard {@link StringFormField}
   * @param items the array of items
   */
  public FormFieldComboBox (FormField editorField, Object[] items) {
    super (items);
    setEditorField(editorField);
    setup();
  }

  /**
   * Creates a {@code FormFielComboBox} for a default {@link StringFormField}.
   * 
   * @param items the array of items
   */
  public FormFieldComboBox (Object[] items) {
    this ((FormField)null, items);
  }




  /**
   * setup editor, renderer and popuplistener
   */
  private void setup ()  {
    setEditor (new FormFieldComboBoxEditor());
    setRenderer(new FormFieldComboBoxRenderer());
    addPopupMenuListener(popupListener);
  }


  
  /**
   * Sets the editor field.
   *
   * @param editorField the FormField used for editing and formatting,
   *        null if standard {@link StringFormField}
   */
  public void setEditorField (FormField editorField)  {
    
    if (isEditable() && this.editorField != null)  {
      this.editorField.removeFocusListener(this);
    }
    
    if (this.editorField != null) {
      this.editorField.removeKeyListener(keyListener);
    }
    
    if (editorField == null)  {
      this.editorField = new StringFormField();
    }
    else  {
      this.editorField = editorField;
    }
    
    if (isEditable()) {
      this.editorField.addFocusListener(this);
    }
    // special key-handling for editable comboboxes
    this.editorField.addKeyListener(keyListener);
  }
  

  /**
   * Gets the editor field.
   * 
   * @return the editor field
   */
  public FormField getEditorField ()  {
    return editorField;
  }

  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden so that the editor field gets the focus.
   */
  @Override
  public boolean requestFocusInWindow() {
    if (isEditable()) {
      return editorField.requestFocusInWindow();
    }
    else  {
      return super.requestFocusInWindow();
    }
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden so that the editor field gets the focus.
   */
  @Override
  public void requestFocus() {
    if (isEditable()) {
      editorField.requestFocus();
    }
    else  {
      super.requestFocus();
    }
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to store tooltip in editorfield.
   * This allows editable comboboxes handle tooltips the same way
   * as non-editable.
   */
  @Override
  public void setToolTipText(String text) {
    editorField.setToolTipText(text);
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to get the tooltip from the editorfield.
   * This allows editable comboboxes handle tooltips the same way
   * as non-editable.
   */
  @Override
  public String getToolTipText()  {
    return editorField.getToolTipText();
  }

  
  
  // ------------------------- implements FormFieldComponent ------------------
  
  @Override
  public boolean wasTransferFocus() {
    return isEditable() ? editorField.wasTransferFocus() : super.wasTransferFocus();
  }
  
  @Override
  public boolean wasTransferFocusBackward() {
    return isEditable() ? editorField.wasTransferFocusBackward() : super.wasTransferFocusBackward();
  }
  
  @Override
  public boolean wasFocusGainedFromTransfer()  {
    return isEditable() ? editorField.wasFocusGainedFromTransfer() : super.wasFocusGainedFromTransfer();
  }
  
  @Override
  public boolean wasFocusGainedFromTransferBackward()  {
    return isEditable() ? editorField.wasFocusGainedFromTransferBackward() : super.wasFocusGainedFromTransferBackward();
  }  
  
  @Override
  public boolean wasTransferFocusByEnter() {
    return isEditable() ? editorField.wasTransferFocusByEnter() : super.wasTransferFocusByEnter();
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to setFormValueText
   */
  @Override
  public void fireValueEntered () {
    if (isEditable()) {
      // try to find the value in the list and select it if found.
      // if no match found: do nothing, i.e. field not from list
      setFormValueText(editorField.getText());
    }
    super.fireValueEntered();
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to show the value if editable.
   * @param e the focus lost event
   */
  @Override
  public void focusLost (FocusEvent e)  {
    super.focusLost(e);
    if (!e.isTemporary() && isEditable() && !isCellEditorUsage()) {
      // show what has been read
      fireValueChanged();
    }
  }
  
  @Override
  public boolean setFormValueText(String text)  {
    boolean rv = super.setFormValueText(text);
    editorField.setText(text);  // in case setFormValueText() did not match
    return rv;
  }
  
  public void setFormat (String format) {
    editorField.setFormat(format);
  }

  public String getFormat ()  {
    return editorField.getFormat();
  }

  public void setColumns (int columns)  {
    editorField.setColumns(columns);
  }

  public int getColumns ()  {
    return editorField.getColumns();
  }

  public  void  setAutoSelect (boolean autoselect)  {
    editorField.setAutoSelect(autoselect);
  }

  public boolean isAutoSelect()  {
    return editorField.isAutoSelect();
  }

  public  void  setAutoNext (boolean autonext)  {
    editorField.setAutoNext(autonext);
  }

  public boolean isAutoNext()  {
    return editorField.isAutoNext();
  }

  public  void  setAutoUpdate (boolean autoupdate)  {
    editorField.setAutoUpdate(autoupdate);
  }

  public boolean isAutoUpdate()  {
    return editorField.isAutoUpdate();
  }

  public  void  setConvert (char convert)  {
    editorField.setConvert(convert);
  }

  public  char  getConvert ()  {
    return editorField.getConvert();
  }

  public  void  setAdjust (char adjust)  {
    editorField.setAdjust(adjust);
  }

  public void setConverter(StringConverter converter) {
    editorField.setConverter(converter);
  }
  
  public StringConverter getConverter() {
    return editorField.getConverter();
  }
  
  public  char  getAdjust ()  {
    return editorField.getAdjust();
  }

  public  void  setMaxColumns (int columns) {
    editorField.setMaxColumns(columns);
  }

  public int  getMaxColumns ()  {
    return editorField.getMaxColumns();
  }

  public  void  setFiller (char filler)  {
    editorField.setFiller(filler);
  }

  public char getFiller ()  {
    return editorField.getFiller();
  }

  public  void  setOverwrite (boolean override)  {
    editorField.setOverwrite(override);
  }

  public boolean isOverwrite()  {
    return editorField.isOverwrite();
  }

  public  void  setValidChars (String str)  {
    editorField.setValidChars (str);
  }

  public String getValidChars ()  {
    return editorField.getValidChars();
  }

  public  void  setInvalidChars (String str)  {
    editorField.setInvalidChars(str);
  }

  public String getInvalidChars ()  {
    return editorField.getInvalidChars();
  }

  public void setEraseFirst(boolean erasefirst) {
    editorField.setEraseFirst(erasefirst);
  }
 
  public boolean isEraseFirst() {
    return editorField.isEraseFirst();
  }  
  
  public void setCaretPosition(int pos)  {
    editorField.setCaretPosition(pos);
  }
  
  public int getCaretPosition() {
    return editorField.getCaretPosition();
  }

  public boolean isCaretLeft()  {
    return editorField.isCaretLeft();
  }
  
  public boolean isCaretRight() {
    return editorField.isCaretRight();
  }
  
  public void setHorizontalAlignment(int alignment)  {
    editorField.setHorizontalAlignment(alignment);
  }

  public int getHorizontalAlignment() {
    return editorField.getHorizontalAlignment();
  }
  
  public void setVerticalAlignment(int alignment) {
    editorField.setVerticalAlignment(alignment);
  }
    
  public int getVerticalAlignment() {
    return editorField.getVerticalAlignment();
  }

  @Override
  public void requestFocusLater() {
    FormHelper.requestFocusLater(this);   // this will invoke requestFocusInWindow()
  }
  
  public boolean isInhibitAutoSelect() {
    return editorField.isInhibitAutoSelect();
  }  

  public void setInhibitAutoSelect(boolean inhibitAutoSelect) {
    editorField.setInhibitAutoSelect(inhibitAutoSelect);
  }  

  public void clearText() {
    editorField.clearText();
  }
  
  public void setCaretLeft()  {
    editorField.setCaretLeft();
  }

  public void setCaretRight() {
    editorField.setCaretRight();
  }

  /**
   * {@inheritDoc}
   * <p>
   * If the popup is visible the UP-key will move to the item above
   * the current item.
   */
  public void upLeft() {
    if (isPopupVisible()) {
      int max = getItemCount();
      int ndx = getSelectedIndex();
      if (ndx != -1 && ndx > 0) {
        setSelectedIndex(ndx - 1);
      }
      else {
        setSelectedIndex(max - 1);
      }      
    }
    else  {
      editorField.upLeft();
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * If the popup is visible the DOWN-key will move to the item below
   * the current item.
   */
  public void downRight() {
    if (isPopupVisible()) {
      int max = getItemCount();
      int ndx = getSelectedIndex();
      if (ndx == -1 || ndx < max - 1) {
        setSelectedIndex(ndx + 1);
      }
      else {
        setSelectedIndex(0);
      }
    }
    else  {
      editorField.downRight();
    }
  }

  public int getErrorOffset () {
    return 0;
  }
  
  public void setText (String str) {
    editorField.setText(str);
  }

  public String getText() {
    return editorField.getText();
  }
  
  public boolean isEmpty()  {
    return editorField.isEmpty();
  }
  
  public String doFormat(Object object)  {
    return editorField.doFormat(object);
  }
  
  public void setStartEditLeftmost (boolean startEditLeftmost)  {
    editorField.setStartEditLeftmost(startEditLeftmost);
  }

  public boolean isStartEditLeftmost()  {
    return editorField.isStartEditLeftmost();
  }
  
  public void doActionPerformed() {
    editorField.doActionPerformed();
  }

  @Override
  public void saveValue() {
    editorField.saveValue();
    super.saveValue();
  }

}
