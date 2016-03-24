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

// $Id: FormField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.Component;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.Document;
import org.tentackle.util.StringConverter;
import org.tentackle.util.StringHelper;
import org.tentackle.util.Toolkit;



/**
 * Abstract base class for all text fields.
 * <p>
 * All Tentackle beans providing a single-line text field bean subclass
 * the FormField class which implements {@code FormFieldComponent}.
 * FormFields provide a lot of features essential for typical business
 * applications, such as data binding.
 *
 * @author harald
 */
public abstract class FormField extends JTextField
                                implements FormFieldComponent {
  
  // text adjustments
  /** no adjustment at all **/
  static public final char ADJUST_NONE   = '=';
  /** remove leading fillers **/
  static public final char ADJUST_LEFT   = '<';
  /** remove trailing fillers **/
  static public final char ADJUST_RIGHT  = '>';
  /** trim to both sides (default) **/
  static public final char ADJUST_TRIM   = '|';

  // character conversion
  /** no conversion (default) **/
  static public final char CONVERT_NONE   = '=';
  /** to uppercase **/
  static public final char CONVERT_UC     = '^';
  /** to lowercase **/
  static public final char CONVERT_LC     = 'v';
  
  

  private boolean honourChangeable = true;// true if honour setChangeable
  private boolean autoUpdate = true;      // true if data binding enabled (default)
  private char convert = CONVERT_NONE;    // character conversion, default is NONE
  private char adjust = ADJUST_TRIM;      // text adjustment, default is TRIM
  private boolean autoSelect;             // true if autoselect on focus gained
  private boolean inhibitAutoSelect;      // inhibit autoselect once
  private boolean autoNext;               // transfer focus to next field if all chars entered
  private int maxColumns;                 // max. number of columns. 0 if no limit
  private String validChars;              // valid characters, null = all
  private String invalidChars;            // invalid characters, null = none
  private char filler = ' ';              // the fill character (defaults to space)
  private boolean override;              // true if override, else insert mode
  private boolean startEditLeftmost;      // true if start edit leftmost in field
  private boolean eraseFirst;             // erase text before first setText()
  private boolean wasError;               // flag in autoupdate to inhibit toForm
  private boolean fireRunning;            // indicates fireValue... running
  private String savedValue;              // last savepoint
  private int verticalAlignment = JLabel.CENTER;  // vertical alignment (only with Tentackle LAF)
  private String helpURL;                 // URL for online help,  null = none
  private Window parentWindow;            // parent window (cached)
  private TooltipDisplay tooltipDisplay;  // tooltip display
  private boolean formTraversable = true; // true if component is allowed for autofocus
  private boolean tableCellEditorUsage;   // true if component is used as a table cell editor
  private boolean transferFocusByEnter;   // focus lost due to ENTER
  private boolean transferFocusDone;      // transfer focus forward done
  private boolean transferFocusBackwardDone;  // transfer focus backward done
  private boolean focusGainedFromTransfer;  // focus gained by transfer focus forward
  private boolean focusGainedFromTransferBackward;  // focus gained by transfer focus backward
  private boolean valueChangedTriggered;  // value changed and propagated to parent(s)
  private boolean skipNextFocusLost;      // keep focus on next focus lost
  private FormWindow formWrapWindow;      // the window to form wrap event on next focus lost
  
  /** caret position for (first) error, -1 if no error **/
  protected int errorOffset = -1;
  

  /**
   * Creates a FormField.<br>
   * Notice: setting doc != null requires a doc derived from FormFieldDocument.
   * 
   * @param doc the document model, null = default
   * @param str the initial text, null = empty
   * @param columns the number of columns, 0 = minimum width
   */
  public FormField (Document doc, String str, int columns) {

    // setup parent class first
    super (doc, str, columns);

    setAlignmentX(0);
    
    // add Key mappings
    FormHelper.setupDefaultBindings(this);

    // set text again because of new document-model
    if (str != null)  {
      setText(str);
    }
    
    // enable drag support by default
    setDragEnabled(true);
    

    /**
     * we need a special TransferHandler because the Focus-Events will remove
     * the dropped text again. Furthermore we replace the whole field rather
     * than just inserting.
     */
    setTransferHandler(new TransferHandler("text") {
      private static final long serialVersionUID = 2662933148941085832L;
      @Override
      public boolean canImport(JComponent comp, DataFlavor[] flavors) {
        if (flavors != null)  {
          for (int i=0; i < flavors.length; i++)  {
            if (flavors[i] == DataFlavor.stringFlavor) {
              return true;
            }
          }
        }
        return false;
      }
      @Override
      public boolean importData(JComponent comp, Transferable t)  {
        try {
          String text = (String)(t.getTransferData(DataFlavor.stringFlavor));
          setText(text);        // replace!
          fireValueEntered();   // store in field cause of focus events
          return true;
        }
        catch (Exception e) {
          FormError.printException(e);
          return false;
        }
      }
      @Override
      protected Transferable createTransferable(JComponent c) { 
        return new StringSelection(getSelectedText());
      }
      @Override
      public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;    // don't remove the text from the source!
      }
    });
  }
  
  /**
   * Creates a FormField with the default document model.<br>
   * 
   * @param str the initial text, null = empty
   * @param columns the number of columns, 0 = minimum width
   */
  public FormField (String str, int columns)  {
    this (null, str, columns);
  }

  /**
   * Creates a FormField with the default document model,
   * minimum width and given initial text.<br>
   * 
   * @param str the initial text, null = empty
   */
  public FormField (String str) {
    this (null, str, 0);
  }

  /**
   * Creates a FormField with the default document model and
   * given column width.<br>
   * 
   * @param columns the number of columns, 0 = minimum width
   */
  public FormField (int columns)  {
    this (null, null, columns);
  }

  /**
   * Creates an empty FormField with the default document model,
   * and minimum column width.<br>
   */
  public FormField () {
    this (null, null, 0);
  }

  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden:
   * Changes the nerving behaviour that pressing backspace at the end
   * of a selection clears the whole selection. Especially in autoselected
   * numeric fields its often necessary to overtype the last digits.
   * With this hack backspace simply clears the selection.
   */
  @Override
  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
    if (pressed && isAutoSelect() &&
        ks.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      int selStart = getSelectionStart();
      int selEnd   = getSelectionEnd();
      if (selEnd > selStart && getCaretPosition() == selEnd)  {
        // only if something selected and caret is at rightmost position of selection 
        setSelectionStart(getSelectionEnd());   // clear selection, leave caret rightmost
      }
    }
    return super.processKeyBinding(ks, e, condition, pressed);
  }
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to inhibit excessive geometry-management (for example in Gridbaglayout).
   */
  @Override
  public void setColumns(int columns) {
    super.setColumns(columns);
    // @todo: is this still necessary for the latest JDK?
    setMinimumSize(getPreferredSize());
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to set the default {@link FormFieldDocument}.
   */
  @Override
  protected Document createDefaultModel ()  {
    return new FormFieldDocument (this);
  }
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to perform focus handling and data binding
   */
  @Override
  protected void processFocusEvent(FocusEvent e) {
    super.processFocusEvent(e);
    if (e.isTemporary() == false) {
      if (e.getID() == FocusEvent.FOCUS_GAINED) {
        performFocusGained(e.getOppositeComponent());
      }
      else if (e.getID() == FocusEvent.FOCUS_LOST)  {
        if (skipNextFocusLost)  {
          skipNextFocusLost = false;
          performWrapEvent();
        }
        else  {
          performFocusLost();
        }
      }
    }
  }
  

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to turn off autoNext during inserts
   * and to implement eraseFirst.
   */
  @Override
  public void setText (String str)  {
    
    if (autoNext) {
      autoNext = false;
      super.setText(str);
      autoNext = true;
    }
    else {
      super.setText(str);
    }

    if (eraseFirst) {
      // trigger eraseFirst in Document after this setText
      Document doc = getDocument();
      try {
        ((FormFieldDocument)doc).setEraseFirst(true);
      } 
      catch (Exception e) {}
      eraseFirst = false;
    }
    // always start at left side (useful for long text not fitting into field)
    setCaretLeft();
  }


  /**
   * {@inheritDoc}
   * <p>
   * Overridden because of text adjustment.
   */
  @Override
  public String getText() {
    String str = super.getText();
    if (str != null)  {
      int len = str.length();
      if (adjust == ADJUST_LEFT || adjust == ADJUST_TRIM) {
        int i = 0;
        while (i < len && str.charAt(i) == filler) {
          i++;
        }
        if (i > 0)  {
          str = str.substring(i);
          len -= i;
        }
      }
      if (adjust == ADJUST_RIGHT || adjust == ADJUST_TRIM)  {
        int i = len - 1;
        while (i > 0 && str.charAt(i) == filler) {
          i--;
        }
        i++;
        if (i < len)  {
          str = str.substring(0, i);
        }
      }
    }
    return str;
  }



  /**
   * {@inheritDoc}
   * <p>
   * Ovwerwritten to set transferFocusDone.
   */
  @Override
  public void transferFocus() {
    transferFocusDone = true;
    super.transferFocus();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Ovwerwritten to set transferFocusBackwardDone.
   */
  @Override
  public void transferFocusBackward() {
    transferFocusBackwardDone = true;
    super.transferFocusBackward();
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
  
  
  /**
   * Gets the tooltip display.
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
   * process the focus gained event
   */
  private void performFocusGained (Component opposite)  {
    boolean wasEnter = false;
    if (opposite instanceof FormComponent)  {
      focusGainedFromTransfer         = ((FormComponent)opposite).wasTransferFocus();
      focusGainedFromTransferBackward = ((FormComponent)opposite).wasTransferFocusBackward();
      wasEnter                        = ((FormComponent)opposite).wasTransferFocusByEnter();
    }
    transferFocusDone = false;
    transferFocusBackwardDone = false;
    if (wasError)  {
      // in JTables the text will be empty, so we must check length before 
      if (errorOffset >= 0 && getDocument().getLength() > errorOffset)  {
        setCaretPosition (errorOffset);
      }
      wasError = false;
    }
    else  {
      if (autoUpdate) {
        // update the fields display
        fireValueChanged();
        // after setText(), the caret is at the left side of the field.
        // Move to the right if we came down with VK_DOWN.
        if (focusGainedFromTransfer && !wasEnter && !startEditLeftmost)  {
          setCaretRight();
        }
      }
      // select all if autoselect enabled
      if (autoSelect && !inhibitAutoSelect)   {
        selectAll();
      }
    }
    transferFocusByEnter = false;
    inhibitAutoSelect = false;
    formWrapWindow = null;

    showTooltip(super.getToolTipText());
  }
  

  /**
   * process the focus lost event
   */
  private void performFocusLost ()  {
    wasError = false;
    setEraseFirst(false);
    if (autoUpdate) {
      // update data field and show what has been read
      errorOffset = -1;
      fireValueEntered();
      if (errorOffset >= 0) { /* some conversion error */
        // show where the error was
        setCaretPosition (errorOffset);
        Toolkit.beep();
        wasError = true;
        requestFocusLater();
      }
      else  {
        // show what has been read
        fireValueChanged();
      }
    }
    performWrapEvent();
    showTooltip(null);
  }
  
  
  /**
   * process the form wrap event
   */
  private void performWrapEvent() {
    if (formWrapWindow != null)  {
      formWrapWindow.fireFormWrappedFocus(new FormWrapEvent(this));
      formWrapWindow = null;
    }    
  }
  
  
  
  
  
  // -------------------- implements FormFieldComponent ---------------------

  
  public synchronized void addValueListener (ValueListener listener) {
     listenerList.add (ValueListener.class, listener);
  }

  public synchronized void removeValueListener (ValueListener listener) {
     listenerList.remove (ValueListener.class, listener);
  }

  public void requestFocusLater() {
    FormHelper.requestFocusLater(this);
  }
  
  public void fireValueChanged () {
    FormHelper.doFireValueChanged (this, listenerList.getListenerList());
    valueChangedTriggered = false;
  }

  public void fireValueEntered () {
    valueChangedTriggered = false;  // check always after field exit
    FormHelper.doFireValueEntered (this, listenerList.getListenerList());
  }
  
  public void doActionPerformed()  {
    fireActionPerformed();
    transferFocusByEnter = true;
  }
  
  public void saveValue() {
    if (honourChangeable) {
      savedValue = super.getText();
      valueChangedTriggered = false;
    }
  }
  
  public boolean isValueChanged() {
    if (honourChangeable) {
      String value = super.getText();
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
    return false;
  }
  
  public void triggerValueChanged()   {
    if (!valueChangedTriggered && !fireRunning) {
      // if not already promoted
      FormHelper.triggerValueChanged(this);
      // don't trigger again when field is now changed (this is the usual case).
      // But trigger on next keystroke, if field became unchanged due to this keystoke!
      valueChangedTriggered = isValueChanged();   
    }
  }
  
  public void setCellEditorUsage(boolean flag) {
    tableCellEditorUsage = flag;
  }
  
  public boolean isCellEditorUsage() {
    return tableCellEditorUsage;
  }

  public void prepareFocusLost()  {
    if (tableCellEditorUsage == false) {
      performFocusLost();
      skipNextFocusLost = true;
    }
    // else: triggered by FOCUS_LOST
  }

  public void showHelp()  {
    FormHelper.openHelpURL(this);
  }

  public void clearText() {
    setText(StringHelper.emptyString);
  }
  
  public boolean isEmpty()  {
    String text = getText();
    return text == null || text.length() == 0;
  }
  
  public boolean isCaretLeft()  {
    return getCaretPosition() == 0;
  }
  
  public boolean isCaretRight() {
    return getCaretPosition() == getDocument().getLength();
  }
  
  public void upLeft()  {
    if (isCaretLeft())  {
      // go to previous field if we already are at start
      transferFocusBackward();
    }
    else  {
      setCaretLeft();
    }
  }

  public void downRight()  {
    if (isCaretRight())  {
     // go to next field if we already stay at right bound
      transferFocus();
    }
    else  {
      setCaretRight();
    }
  }

  public void setCaretLeft()  {
    setCaretPosition(0);
  }

  public void setCaretRight() {
    setCaretPosition (getDocument().getLength());
  }
  
  public void setEraseFirst(boolean erasefirst) {
    this.eraseFirst = erasefirst;
    if (erasefirst == false)  {
      Document doc = getDocument();
      try {
        ((FormFieldDocument)doc).setEraseFirst(false);
      } catch (Exception e) {}
    }
  }

  public boolean isEraseFirst()  {
    return eraseFirst;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Changeable is translated to editable.
   */
  public void setChangeable (boolean changeable)  {
    if (honourChangeable) {
      setEditable(changeable);
    }
  }

  public boolean isChangeable() {
    return isEditable();
  }

  public void setHonourChangeable(boolean flag) {
    this.honourChangeable = flag;
  }
  
  public boolean isHonourChangeable() {
    return honourChangeable;
  }

  public  void  setMaxColumns (int columns) {
    if (columns > 0)  {
      this.maxColumns = columns;
      // this will set the filler
      setText (getText());
      // setColumns too if 0 (overriding getColumns() does not work due to Sun's implementation)
      if (getColumns() == 0)  {
        setColumns(columns);
      }
    }
    else  {
      this.maxColumns = 0;
    }
  }
  
  public int getMaxColumns ()  {
    return maxColumns;
  }

  public  void  setConvert (char convert)  {
    this.convert = convert;
    String text = getText();
    if (text != null) {
      switch (convert) {
      case CONVERT_UC:  setText (text.toUpperCase());
                        break;
      case CONVERT_LC:  setText (text.toLowerCase());
                        break;
      }
    }
  }

  public  char  getConvert ()  {
    return convert;
  }

  public  void  setAdjust (char adjust)  {
    this.adjust = adjust;
  }

  public char getAdjust() {
    return adjust;
  }

  public  void  setAutoSelect (boolean autoselect)  {
    this.autoSelect = autoselect;
  }

  public boolean isAutoSelect()  {
    return autoSelect;
  }
  
  public  void  setAutoNext (boolean autonext)  {
    this.autoNext = autonext;
  }

  public boolean isAutoNext()  {
    return autoNext;
  }
  
  public  void  setAutoUpdate (boolean autoupdate)  {
    this.autoUpdate = autoupdate;
  }

  public boolean isAutoUpdate()  {
    return autoUpdate;
  }
  
  public  void  setValidChars (String validChars)  {
    this.validChars = validChars;
  }

  public String getValidChars ()  {
    return validChars;
  }
  
  public  void  setInvalidChars (String invalidChars)  {
    this.invalidChars = invalidChars;
  }

  public String getInvalidChars ()  {
    return invalidChars;
  }
  
  public  void  setFiller (char filler)  {
    char oldfiller = this.filler;
    this.filler = filler;
    // this will set the filler
    if (!isEmpty()) {
      setText (getText().replace(oldfiller, filler));
    }
  }

  public char getFiller ()  {
    return filler;
  }
  
  public void setOverwrite (boolean override)  {
    this.override = override;
  }

  public boolean isOverwrite()  {
    return override;
  }
  
  public void setStartEditLeftmost (boolean startEditLeftmost)  {
    this.startEditLeftmost = startEditLeftmost;
  }

  public boolean isStartEditLeftmost()  {
    return startEditLeftmost;
  }
  
  public int  getErrorOffset () {
    return errorOffset;
  }

  public void setFireRunning(boolean running) {
    this.fireRunning = running;
  }

  public boolean isFireRunning()  {
    return fireRunning;
  }

  public boolean isInhibitAutoSelect() {
    return inhibitAutoSelect;
  }
  
  /** 
   * {@inheritDoc}
   * <p>
   * When set inhibits autoSelect once after having performed eraseFirst.
   * This is necessary for components in a JTable because after pressing a key
   * eraseFirst will be executed, then the key inserted and THEN the field
   * gets the focusGained which will selectAll if autoSelect enabled.
   * The eraseFirst is triggered in FormFieldComponentCellEditor.
   * This flag is really package internal!
   */
  public void setInhibitAutoSelect(boolean inhibitAutoSelect) {
    this.inhibitAutoSelect = inhibitAutoSelect;
  }  
  
  public void setFormWrapWindow(FormWindow parent)  {
    formWrapWindow = parent;
  }
  
  public void setVerticalAlignment(int alignment) {
    this.verticalAlignment = alignment;
  }

  public int  getVerticalAlignment()  {
    return verticalAlignment;
  }
  
  public String getHelpURL() {
    return helpURL;
  }
  
  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }

  public StringConverter getConverter() {
    Document doc = getDocument();
    try {
      return ((FormFieldDocument)doc).getConverter();
    }
    catch (Exception e) {
      return null;
    }
  }
  
  public void setConverter(StringConverter converter) {
    Document doc = getDocument();
    try {
      ((FormFieldDocument)doc).setConverter(converter);
    } catch (Exception e) {}
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
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to show tooltip in tooltipdisplay OR via mouse
   * but not both.
   */
  @Override
  public String getToolTipText() {
    return getTooltipDisplay() == null ? super.getToolTipText() : null;
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

}


