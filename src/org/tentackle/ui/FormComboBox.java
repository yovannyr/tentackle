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

// $Id: FormComboBox.java 423 2008-09-13 09:13:49Z harald $

package org.tentackle.ui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;



/**
 * A forms-aware combobox.<br>
 * 
 * Notes:
 * 
 * <ol>
 * <li>If {@code valueEnteredOnSelect} is set to {@code false} the {@code valueEntered}
 * event will only be fired on focus lost and not whenever the selection is changed.</li>
 * <li>the action event behave as expected, i.e. fired on each ENTER</li>
 * <li>due to the data binding there is a decent separation of the "selected item"
 * and the "contents" of the editor field. To get the contents, i.e. the "real value",
 * applications should use getFormValue. To get only the current selected object use
 * getSelectedItem.</li>
 * <li>As opposed to JComboBox getSelectedItem is guaranteed to be null if
 * getSelectedIndex is -1.</li>
 * <li>For editable comboboxes applications should use {@link FormFieldComboBox}.</li>
 * </ol>
 * 
 * @author harald
 */
public class FormComboBox extends JComboBox
       implements FormComponent, FocusListener {

  
  private String helpURL;                         // != null for online help
  private FormWindow formWrapWindow;              // window to receive wrap event
  private boolean valueEnteredOnSelect = true;    // true if trigger ValueEntered if selection changed (default)
  private boolean allowDeselect = true;           // false = don't allow BACKSPACE/DELETE to deselect
  private long leadKeyTimeout = 1000;             // timeout in milliseconds for multi-key-selection
  private Window parentWindow;                    // parent window (cached)
  private TooltipDisplay tooltipDisplay;          // tooltip display
  private Object lastActionItem;                  // last item returned by fireActionEvent
  private boolean actionFired;                    // true if actionEvent has been fired
  private boolean fireRunning;                    // true if fireValueEntered is running
  private String  savedValue;                     // saved value
  private int     leadMatchLen;                   // last matchlen from getItemIndexWithLeadString() (swing is not mt anyway)
  private ListCellRenderer popupRenderer;         // extra Popup renderer if not null
  private boolean shortLongPopup;                 // true use extra popup renderer for ShortLongText-Objects
  private KeyEvent lastKeyEvent;                  // last key event
  private boolean transferFocusDone;              // transfer focus forward done
  private boolean transferFocusBackwardDone;      // transfer focus backward done
  private boolean valueChangedTriggered;          // true if value changed triggered
  private boolean valueAdjusting;                 // to inhibit actionEvents during changes of the list
  private boolean transferFocusByEnter;           // focus lost due to ENTER
  private boolean focusGainedFromTransfer;        // focus gained by transfer focus forward
  private boolean focusGainedFromTransferBackward; // focus gained by transfer focus backward  
  private boolean formTraversable = true;         // true if checkbox gets keyboard focus
  private boolean honourChangeable = true;        // honour the changeable-request

  
  /**
   * Creates a <code>FormComboBox</code> that takes its items from an
   * existing <code>ComboBoxModel</code>.  Since the
   * <code>ComboBoxModel</code> is provided, a combo box created using
   * this constructor does not create a default combo box model and
   * may impact how the insert, remove and add methods behave.
   *
   * @param model the <code>ComboBoxModel</code> that provides the 
   * 		displayed list of items
   * @see JComboBox#JComboBox(javax.swing.ComboBoxModel) 
   */
  public FormComboBox (ComboBoxModel model) {
    super (model);
    setup();
  }

  /** 
   * Creates a <code>FormComboBox</code> that contains the elements
   * in the specified array.  By default the first item in the array
   * (and therefore the data model) becomes selected.
   *
   * @param items  an array of objects to insert into the combo box
   * @see JComboBox#JComboBox(java.lang.Object[]) 
   */
  public FormComboBox (Object[] items) {
    super (items);
    setup();
  }

  /**
   * Creates a <code>FormComboBox</code> with a default data model.
   * The default data model is an empty list of objects.
   * Use <code>addItem</code> to add items.
   *
   * @see JComboBox#JComboBox() 
   */
  public FormComboBox ()  {
    super();
    setup();
  }


  private void setup()  {
    // setup default key-bindings
    FormHelper.setupDefaultBindings(this);
    // setup focus handling if not editable
    if (!isEditable()) {
      addFocusListener(this);
    }
  }
  
  

  /** 
   * {@inheritDoc}
   * <p>
   * Overridden for keyboard shortcuts.
   */
  @Override
  public void processKeyEvent(KeyEvent e) {
    
    if (e.getID() == KeyEvent.KEY_PRESSED)  {
      
      lastKeyEvent = e;
      boolean isTableCellEditor = isCellEditorUsage();
      
      if (isEditable() == false)  {
        
        // not editable ComboBox
        if (isTableCellEditor) {
          if (e.getKeyCode() == KeyEvent.VK_LEFT ||
              e.getKeyCode() == KeyEvent.VK_RIGHT ||
              e.getKeyCode() == KeyEvent.VK_TAB) {
            /* this prevents to move to the next/previous cell */
            e.consume();
            return;
          }
        }
        else  {
          // not in table
          if (isPopupVisible() == false && e.getModifiers() == 0) {
            // Popup is not shown
            if (e.getKeyCode() == KeyEvent.VK_UP) {
              e.consume();
              transferFocusBackward();
              return;   // don't process event further
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
              e.consume();
              transferFocus();
              return;   // don't process event further
            }
          }
        }
        
        // clear selection
        if (allowDeselect && 
            (e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
             e.getKeyCode() == KeyEvent.VK_DELETE))  {
          setSelectedIndex(-1);
          clearMultiKeyLeadString();
          e.consume();
        }
      }
      
      
      // show popup if Ctrl-Enter/Down/Space or F2
      if (isPopupVisible() == false)  {
        if (e.getKeyCode() == KeyEvent.VK_F2 ||
            ((e.getKeyCode() == KeyEvent.VK_DOWN ||
              e.getKeyCode() == KeyEvent.VK_UP ||
              (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) ||
              e.getKeyCode() == KeyEvent.VK_SPACE) &&
             (isTableCellEditor || e.isControlDown())))  {
          showPopup();
          e.consume();
        }
      }
      else  {
        if (isTableCellEditor && e.getModifiers() == 0)  {
          /**
           * if ComboBox is inside a JTable the cursor Keys (UP/DOWN) won't work
           * in the default implementation as expected.
           */
          if (e.getKeyCode() == KeyEvent.VK_UP) {
            int index = getSelectedIndex();
            if (index > 0)  {
              setFormValueIndex(index - 1);
            }
            e.consume();
            return;
          }
          else if (e.getKeyCode() == KeyEvent.VK_DOWN)  {
            int index = getSelectedIndex();
            if (index < getItemCount() - 1) {
              setFormValueIndex(index + 1);
            }
            e.consume();
            return;
          }
        }
      }
    }
    
    // do the default processing
    super.processKeyEvent(e);
  }
  
  
  /**
   * Overridden to do nothing!
   * <p>
   * Reason: pressing ENTER in editable comboboxes would store the "text" of
   * the editorfield into the selectedItem. We don't need this nonsense
   * because FormComboBoxes are able to distinguish between the text-value and
   * the selected object due the data binding via valueListeners.
   * @param e the action event
   * @see #fireActionEvent()
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    // do nothing!
  }
  
  
  /**
   * Overridden for the reason of actionEvents not being triggered by setFormValue().
   * ActionEvents will *ONLY* be triggered due to a selection in the pulldown-menu!
   * @see #actionPerformed(java.awt.event.ActionEvent) 
   */
  @Override
  protected void fireActionEvent() {
    if (!valueAdjusting)  {
      if (valueEnteredOnSelect) {
        if (isCellEditorUsage())  {
          // fake user pressed ENTER.
          // we need this if table is in celltraversal mode.
          // otherwise this does not harm
          dispatchEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
        }
        else  {
          fireValueEntered();
          lastActionItem = getSelectedItem();
          actionFired    = true;
        }
      }
      else  {
        super.fireActionEvent();
      }
    }
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to focus handling
   * @param editable true if combobox is editable
   */
  @Override
  public void setEditable(boolean editable) {
    boolean changed = editable != isEditable();
    super.setEditable(editable);
    if (changed)  {
      if (editable) {
        removeFocusListener(this);
        if (editor != null && editor.getEditorComponent() instanceof FormField)  {
          // register focus event on formfield instead of combobox
          ((FormField)editor.getEditorComponent()).addFocusListener(this);
        }
      }
      else  {
        addFocusListener(this);
        if (editor != null && editor.getEditorComponent() instanceof FormField)  {
          // register focus event on formfield instead of combobox
          ((FormField)editor.getEditorComponent()).removeFocusListener(this);
        }
      }
    }
  }
  
  

  
  /**
   * Sets the form value by matching a given text with the objects
   * in the selection list<br>
   * 
   * Tries to find the value in the object list according to its String-representation
   * and select it if found.
   * if no match found: just clear selectedItem but 
   * don't change contents of editor!
   * @param text the string to match
   * @return true if some object selected, false if no such object
   */
  public boolean setFormValueText(String text)  {
    if (text != null && text.length() > 0)  {
      int count = getItemCount();
      for (int i=0; i < count; i++) {
        Object item = getItemAt(i);
        if (item != null) {
          String itemString = item.toString();
          if (itemString != null && text.compareTo(itemString) == 0)  {
            // set selected index
            setFormValueIndex(i);
            return true;
          }
        }
      }
    }
    boolean oldValueAdjusting = valueAdjusting;
    valueAdjusting = true;
    setSelectedIndex(-1);
    valueAdjusting = oldValueAdjusting;
    return false;
  }
  
  /**
   * Gets the text representation of the current selection.<br>
   * 
   * @return the String value of the selection, null = nothing selected
   */
  public String getFormValueText()  {
    Object obj = getFormValue();
    return obj == null ? null : obj.toString();
  }
  
  
  
  
  /**
   * Sets the formvalue by its index.<br>
   * Avoids triggering fireValueEntered if used from application
   * (use instead of setSelectedIndex())
   * @param index the object index, -1 to deselect
   */
  public void setFormValueIndex(int index) {
    boolean oldValueAdjusting = valueAdjusting;
    valueAdjusting = true;
    actionFired = false;
    setSelectedIndex(index);
    clearMultiKeyLeadString();
    valueAdjusting = oldValueAdjusting;
  }

  

  /**
   * Adds an array of items to the list of objects.
   * 
   * @param items the array of items to add
   */
  public void addAllItems (Object[] items) {
    if (items != null) {
      for (int i=0; i < items.length; i++)  {
        addItem(items[i]);
      }
    }
  }

  
  /**
   * Adds a collection of items to the list of objects.
   * 
   * @param items the collection of items to add
   */
  public void addAllItems (Collection items) {
    if (items != null) {
      Iterator iter = items.iterator();
      while (iter.hasNext())  {
        addItem(iter.next());
      }
    }
  }
  
  
  /**
   * Replaces all items by an array of objects.
   * 
   * @param items the array of items
   */
  public void setAllItems (Object[] items) {
    removeAllItems();
    addAllItems(items);
  }

  
  /**
   * Replaces all items by a collection of objects.
   * 
   * @param items the collection of items
   */
  public void setAllItems (Collection items) {
    removeAllItems();
    addAllItems(items);
  }
  
  
  /**
   * Gets the current array of items.
   * 
   * @return the array of items (never null)
   */
  public Object[] getAllItems() {
    int count = getItemCount();
    Object[] items = new Object[count];
    for (int i=0; i < count; i++) {
      items[i] = getItemAt(i);
    }
    return items;
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to turn off annoying actionEvents during updates
   */
  @Override
  public void removeAllItems() {
    boolean oldValueAdjusting = valueAdjusting;
    valueAdjusting = true;
    super.removeAllItems();
    valueAdjusting = oldValueAdjusting;
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to turn off annoying actionEvents during updates
   */
  @Override
  public void addItem (Object anObject) {
    boolean oldValueAdjusting = valueAdjusting;
    valueAdjusting = true;
    super.addItem(anObject);
    valueAdjusting = oldValueAdjusting;
  }

  

  
  
  /** 
   * Enables firing a valueEntered-Event *instead* of
   * an actionEvent (avoids registering value- and actionEvent)
   * The default is to fire actionEvents for selection changes and
   * valueEvents for leaving the field (i.e. as in FormFields)
   * @param flag true to fire on select, false only on focus lost
   */
  public void setValueEnteredOnSelect(boolean flag) {
    this.valueEnteredOnSelect = flag;
  }
  
  /**
   * Returns whether valueEntered is fired on select or focus lost (default).
   * 
   * @return true if on select, else focus lost
   */
  public boolean isValueEnteredOnSelect() {
    return valueEnteredOnSelect;
  }

  
  
  /** 
   * sets a flag that we are currently adjusting a value and
   * no fireValueEntered-Events should be triggered
   * @param flag true if is adjusting
   */
  public void setValueAdjusting(boolean flag) {
    this.valueAdjusting = flag;
  }
  
  /**
   * Returns whether currently adjusting the value.
   * 
   * @return true if adjusting
   */
  public boolean isValueAdjusting() {
    return valueAdjusting;
  }

  
  
  
  /**
   * Selects the list item that best corresponds to the specified String
   * and returns true, if there is an item corresponding
   * to that lead-string.  Otherwise, returns false.
   *
   * @param lead the first chars types by user
   * @return true if selected, false if no object matches
   */
  public boolean selectWithLeadString(String lead) {
    int index = getItemIndexWithLeadString(lead, getModel());
    setSelectedIndex(index);
    return index >= 0;
  }
  
  
  /**
   * Returns the index of the first/best matching selection according to a string
   * 
   * @param lead the first chars types by user
   * @param model the data model
   * @return the index, -1 if no match
   */
  public int getItemIndexWithLeadString(String lead, ComboBoxModel model)  {
    // find first partial match
    int matchIndex = -1;
    leadMatchLen   = 0;
    
    if (lead != null) {
      lead = lead.toLowerCase();
      int leadLen = lead.length();
      if (leadLen > 0)  {
        int items = getItemCount();
        for (int itemIndex=0; itemIndex < items; itemIndex++) {
          Object item = model.getElementAt(itemIndex);
          String itemText = item.toString().toLowerCase();
          if (itemText != null) {
            int itemLen = itemText.length();
            int charPos;
            for (charPos=0; charPos < itemLen && charPos < leadLen; charPos++) {
              if (itemText.charAt(charPos) != lead.charAt(charPos)) {
                break;
              }
            }
            if (charPos > leadMatchLen) {
              matchIndex    = itemIndex;
              leadMatchLen  = charPos;
            }
          }
        }
      }
    }
    return matchIndex;
  }
  
  
  
  /**
   * Enables multi-key selection.<br>
   * The default implementation installs the {@link MultiKeySelectionManager}.
   * 
   * @param flag true to enable multi key selection.
   */
  public void setMultiKeySelectionManager(boolean flag)  {
    if (flag) {
      setKeySelectionManager(new FormComboBox.MultiKeySelectionManager());
    }
    else  {
      setKeySelectionManager(createDefaultKeySelectionManager());
    }
  }
  
  
  /**
   * Returns whether multi-key selection is enabled.
   * 
   * @return true if enabled
   */
  public boolean isMultiKeySelectionManager() {
    return getKeySelectionManager() instanceof MultiKeySelectionManager;
  }


  /** 
   * Gets the timeout for multi key selections.
   * 
   * @return the timeout in ms (default is 1000).
   */
  public long getLeadKeyTimeout() {
    return leadKeyTimeout;
  }  

  
  /** 
   * Sets the timeout for multi key selections.
   * 
   * @param leadKeyTimeout  the timeout in ms
   */
  public void setLeadKeyTimeout(long leadKeyTimeout) {
    this.leadKeyTimeout = leadKeyTimeout;
  }  
  
  
  /** 
   * Returns the eselect feature for non-editable comboboxes
   * @return true if backspace/delete deselects (default), false = ignore keys
   */
  public boolean isAllowDeselect() {
    return allowDeselect;
  }
  
  /** 
   * Deselect feature for non-editable comboboxes
   * @param allowDeselect true if backspace/delete deselects (default), false = ignore keys
   */
  public void setAllowDeselect(boolean allowDeselect) {
    this.allowDeselect = allowDeselect;
  }
  
  
  
  /**
   * Set the popup renderer.<br>
   * By default, a JComboBox uses the same renderer for rendering
   * the combobox itself (the selected item in the arrowButton).
   * However, if popupRenderer is set explicitly (null by default)
   * the popup's JList uses the popupRenderer which may show
   * some extra info to the user.
   * <p>
   * Notice that this attribute will only be honoured if 
   * one of the tentackle plafs are used.
   * 
   * @param aRenderer the popup renderer, null to disable
   */
  public void setPopupRenderer(ListCellRenderer aRenderer)  {
    ListCellRenderer oldRenderer = popupRenderer;
    popupRenderer = aRenderer;
    firePropertyChange("popupRenderer", oldRenderer, popupRenderer );
    invalidate();
  }
  
  
  /**
   * Gets the popup renderer.
   * @return the popup renderer, null if disabled
   */
  public ListCellRenderer getPopupRenderer()  {
    return popupRenderer;
  }
  
  
  /**
   * Enable the special {@link org.tentackle.util.ShortLongText} popup renderer.<br>
   * A special popupRenderer is implemented in the tentackle-plaf
   * that supports ShortLongText-objects for a two-column popup-List.
   * This is a very common case, where the combobox-value corresponds
   * to a short text and the popup contains the short text and
   * a longer description in a second column.
   * @param flag true if enable short/long text popup renderer
   */
  public void setShortLongPopupEnabled(boolean flag)  {
    boolean oldFlag = shortLongPopup;
    shortLongPopup = flag;
    firePropertyChange("shortLongPopup", oldFlag, shortLongPopup);
    invalidate();    
  }
  
  /**
   * Returns whether short/long text popup renderer enabled.
   * @return true if enabled
   */
  public boolean isShortLongPopupEnabled()  {
    return shortLongPopup;
  }
  
  
  
  
  
    

  /**
   * clears the multi-key selection, if any
   */
  private void clearMultiKeyLeadString()  {
    JComboBox.KeySelectionManager km = getKeySelectionManager();
    if (km instanceof MultiKeySelectionManager) {
      ((MultiKeySelectionManager)km).clearLeadChars();
    }     
  }

  
  /**
   * sets the value in the editorField if not from drop-down list
   */
  private void setValueInEditorField(Object item)  {
    Component c = getEditor().getEditorComponent();
    if (c instanceof FormField) {
      ((FormField)c).setFormValue(item);
    }
    else if (c instanceof JTextField) {
      ((JTextField)c).setText(item.toString());
    }
  }
  
  
  /**
   * to refer to the visible representation, we can't do getFormValue()
   */
  private String getVisibleText()  {
    if (isEditable()) {
      Component c = getEditor().getEditorComponent();
      if (c instanceof JTextField) {
        return ((JTextField)c).getText();
      }
    }
    Object obj = super.getSelectedItem();
    return obj != null ? obj.toString() : null;
  }
  
  
  /**
   * gets the tooltip display (cached)
   */
  private TooltipDisplay getTooltipDisplay() {
    if (tooltipDisplay == null) {
      try {
        // getParentWindow is fast, because its cached!
        tooltipDisplay = ((FormWindow)getParentWindow()).getTooltipDisplay();
      }
      catch (Exception e)  {
        // if no parentwindow or not a FormWindow.
      }
    }
    return tooltipDisplay;
  }
  
  
  /**
   * shows the tooltip
   */
  private void showTooltip(String text)  {
    try {
      getTooltipDisplay().setTooltip(text);
    }
    catch (NullPointerException e)  {  
      // ok if not such display 
    }
  }
  
  

  
  // -------------------- implements FocusListener ---------------------------
  
  public void focusGained (FocusEvent e)  {
    if (!e.isTemporary()) {
      Component opposite = e.getOppositeComponent();
      boolean wasEnter = false;
      if (opposite instanceof FormComponent)  {
        focusGainedFromTransfer         = ((FormComponent)opposite).wasTransferFocus();
        focusGainedFromTransferBackward = ((FormComponent)opposite).wasTransferFocusBackward();
        wasEnter                        = ((FormComponent)opposite).wasTransferFocusByEnter();
      }
      transferFocusDone = false;
      transferFocusBackwardDone = false;
      transferFocusByEnter = false;
      fireValueChanged();
      formWrapWindow = null;
      showTooltip(super.getToolTipText());
      if (isEditable() && this instanceof FormFieldComboBox) {
        FormField field = ((FormFieldComboBox)this).getEditorField();
        if (field.isStartEditLeftmost() || wasEnter || focusGainedFromTransferBackward)  {
          field.setCaretLeft();
        }
        else if (focusGainedFromTransfer) {
          field.setCaretRight();
        }
      }
    }
  }
  

  public void focusLost (FocusEvent e)  {
    if (!e.isTemporary()) {
      transferFocusByEnter = lastKeyEvent != null && 
                             lastKeyEvent.getKeyCode() == KeyEvent.VK_ENTER && lastKeyEvent.getModifiers() == 0;
      fireValueEntered();
      if (formWrapWindow != null)  {
        formWrapWindow.fireFormWrappedFocus(new FormWrapEvent(this));
        formWrapWindow = null;
      }
      showTooltip(null);
    }
  }


  

  // ---------------------- implements FormComponent ------------------------
  
  public void requestFocusLater() {
    FormHelper.requestFocusLater(this);
  }


  public synchronized void addValueListener (ValueListener listener) {
     listenerList.add (ValueListener.class, listener);
  }

  public synchronized void removeValueListener (ValueListener listener) {
     listenerList.remove (ValueListener.class, listener);
  }

  public void fireValueChanged () {
    FormHelper.doFireValueChanged (this, listenerList.getListenerList());
    valueChangedTriggered = false;
  }

  /**
   * {@inheritDoc}
   * <p>
   * note: valueEntered will *ONLY* be fired if the
   * last actionEvent did not fire for the same selectedItem (see fireActionEvent)
   */
  public void fireValueEntered () {
    if (!actionFired || lastActionItem != getSelectedItem())  {
      valueChangedTriggered = false;  // check always after field exit
      FormHelper.doFireValueEntered (this, listenerList.getListenerList());
    }
  }
  
  

  /**
   * {@inheritDoc}
   * <p>
   * Notice: this will compare the objects by "equals" as opposed to setSelectedItem()!
   *
   * @param item is the object to set, null means deselect
   */
  public void setFormValue (Object item)  {

    boolean oldValueAdjusting = valueAdjusting;
    
    actionFired = false;
    
    try {
      valueAdjusting = true;

      if (item != null) {

        // select by "equals"
        for (int i=0; i < getItemCount(); i++)  {
          if (getItemAt(i).equals(item))   {
            setSelectedIndex(i);
            return;
          }
        }
        
        if (item instanceof String) {
          // try to select by text
          if (setFormValueText((String)item)) {
            return; // found
          }
        }
        
        if (isEditable())  {
          // treat as if user has typed in the String-representation of the object
          if (setFormValueText(item.toString())) {
            return; // found
          }
          // value not from drop-down list
          // set the object in the editorfield, if possible
          setValueInEditorField(item);
          return;
        }
      }
      
      // deselect
      setSelectedIndex(-1);
      if (isEditable()) {
        setValueInEditorField(null);
      }
    }
    finally {
      valueAdjusting = oldValueAdjusting;
      clearMultiKeyLeadString();
    }
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Notice: if the combobox is editable the value will be read from
   * the editor field. Otherwise it's read from the selected item.
   */
  public Object getFormValue ()  {
    
    if (isEditable()) {
      // Daten aus dem editorField holen
      // set the object in the editorfield, if possible
      Component c = getEditor().getEditorComponent();
      if (c instanceof FormField) {
        return ((FormField)c).getFormValue();
      }
      else if (c instanceof JTextField) {
        return ((JTextField)c).getText();
      }
    }

    // sonst ist es die aktuelle Selektion
    return getSelectedItem();
  }
  


  public void saveValue() {
    savedValue = getVisibleText();
    valueChangedTriggered = false;
  }
  

  public boolean isValueChanged() {
    String value = getVisibleText();
    if (savedValue == null) {
      return value != null; 
    }
    else  {
      if (value != null) {
        return !value.equals(savedValue);
      }
      else {
        return true;
      }
    }
  }
  
  
  public void triggerValueChanged()   {
    if (!valueChangedTriggered && !fireRunning) {
      // if not already promoted
      FormHelper.triggerValueChanged(this);
      valueChangedTriggered = true;
    }
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Translated to enabled.
   */
  public void setChangeable (boolean changeable)  {
    if (honourChangeable) {
      setEnabled(changeable);
    }
  }

  public boolean isChangeable() {
    return isEnabled();
  }
  
  public void setHonourChangeable(boolean flag) {
    this.honourChangeable = flag;
  }
  
  public boolean isHonourChangeable() {
    return this.honourChangeable;
  }
  
  
  public void setFireRunning(boolean running) {
    this.fireRunning = running;
  }

  public boolean isFireRunning()  {
    return fireRunning;
  }
  
  
  
  /**
   * This cures a lot of problems and side-effects.
   * FormComponenCellEditor does this automatically. 
   * See also FormTable and JTable (i.e. plaf)
   */
  private static final String TABLECELLEDITOR_PROPERTY = "JComboBox.isTableCellEditor";
  
  public void setCellEditorUsage(boolean flag) {
    putClientProperty(TABLECELLEDITOR_PROPERTY, Boolean.valueOf(flag));
  }
  
  public boolean isCellEditorUsage() {
    try {
      Object obj = getClientProperty(TABLECELLEDITOR_PROPERTY);
      if (obj != null)  {
        return ((Boolean)obj).booleanValue();
      }
    }
    catch (Exception ex) {
      UIGlobal.logger.severe("FormComboBox: isTableCellEditor not Boolean! " + ex);
    }
    return false;
  }
  
  
  /**
   * Implemented to do nothing!
   */
  public void prepareFocusLost() {
    // not used here
  }
  

  public void setFormWrapWindow(FormWindow parent)  {
    formWrapWindow = parent;
  }
  

  public String getHelpURL() {
    return helpURL;
  }  
  
  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }
  
  public void showHelp()  {
    FormHelper.openHelpURL(this);
  }

  
  public Window getParentWindow() {
    if (parentWindow == null) {
      parentWindow = FormHelper.getParentWindow(this);
    }
    return parentWindow;
  }
  
  public void invalidateParentInfo()  {
    parentWindow = null;
    tooltipDisplay = null;
  }


  @Override
  public String getToolTipText() {
    return getTooltipDisplay() == null ? super.getToolTipText() : null;
  }
  
  
  @Override
  public void transferFocus() {
    transferFocusDone = true;
    super.transferFocus();
  }

  @Override
  public void transferFocusBackward() {
    transferFocusBackwardDone = true;
    super.transferFocusBackward();
  }

  public boolean wasTransferFocus() {
    return transferFocusDone;
  }
  
  public boolean wasTransferFocusBackward() {
    return transferFocusBackwardDone;
  }
  
  public boolean wasFocusGainedFromTransfer()  {
    return focusGainedFromTransfer;
  }
  
  public boolean wasFocusGainedFromTransferBackward()  {
    return focusGainedFromTransferBackward;
  }
  
  public boolean wasTransferFocusByEnter() {
    return transferFocusByEnter;
  }
  
  public void setFormTraversable(boolean formTraversable) {
    this.formTraversable = formTraversable;
  }
  
  public boolean isFormTraversable() {
    return formTraversable;
  }
  
  
  
  // ------------------------------------------------------------------------
  
  
  /**
   * The standard key selection manager selects the first item that starts with 
   * a given character.<br>
   * This MultiKeySelectionManager keeps an internal "lead-string" of possibly more than one char.
   * Thus typing "AE" selects the first item starting with that string.
   * Pressing the erase or delete button will clear the "lead-string".
   * 
   * To make use of it: setKeySelectionManager(new FormComboBox.MultiKeySelectionManager());
   * or setMultiKeySelectionManager(true);
   */
  
  public class MultiKeySelectionManager implements JComboBox.KeySelectionManager, Serializable {
    
    public int selectionForKey(char aKey,ComboBoxModel aModel) {
      if (Character.isLetterOrDigit(aKey)) {
        Date now = new Date();
        if (lastTime != null && now.getTime() - lastTime.getTime() > leadKeyTimeout)  {
          clearLeadChars();  
        }
        if (leadChars == null) {
          leadChars = new StringBuilder();
        }
        leadChars.append(aKey);
        lastTime = now;
        int index = getItemIndexWithLeadString(leadChars.toString(), aModel);
        // cut non matching trailing chars
        if (index >= 0) {
          if (leadMatchLen > 0) {
            leadChars.setLength(leadMatchLen);
          }
          else  {
            clearLeadChars(); 
          }
        }
        return index;
      }
      return -1;
    }
    
    public void clearLeadChars()  {
      leadChars = null;
      lastTime  = null;
    }
    
    private StringBuilder leadChars;                // leading chars for selection
    private Date          lastTime;                 // time last key pressed, for timeout
  }
  

}
