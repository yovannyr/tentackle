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

// $Id: FormTextArea.java 344 2008-06-02 14:55:48Z harald $

package org.tentackle.ui;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.StringConverter;
import org.tentackle.util.StringHelper;

import static org.tentackle.ui.FormField.*;



/**
 * Extended JTextArea as a {@link FormFieldComponent}.
 *
 * @author harald
 */
public class FormTextArea extends JTextArea implements FormFieldComponent {
  
  private static final String TABLECELLEDITOR_PROPERTY = "JTextArea.isTableCellEditor";
  
  private boolean autoUpdate = true;                // true if data binding enabled (default)
  private boolean startEditLeftmost;                // true if start edit leftmost
  private int horizontalAlignment = JLabel.LEADING; // horizontal alignment
  private int verticalAlignment = JLabel.CENTER;    // is a dummy so far...
  private boolean skipNextFocusLost;                // keep focus on next focus lost
  private FormWindow formWrapWindow;                // window to receive wrap event
  private String savedValue;                        // last savepoint
  private String helpURL;                           // != null for online help
  private boolean smartEnter = true;                // true = smart ENTER
  private boolean enterWillInsert;                  // true = ENTER will insert a new line
  private boolean transferFocusDone;                // transfer focus forward done
  private boolean transferFocusBackwardDone;        // transfer focus backward done
  private boolean transferFocusByEnter;             // focus lost due to ENTER
  private boolean focusGainedFromTransfer;          // focus gained by transfer focus forward
  private boolean focusGainedFromTransferBackward;  // focus gained by transfer focus backward
  private boolean formTraversable = true;           // true if textarea gets keyboard focus
  private boolean valueChangedTriggered;            // value changed and propagated to parent(s)
  private boolean honourChangeable = true;          // true if honour setChangeable
  private boolean fireRunning;                      // indicates fireValue... running
  private boolean eraseFirst;                       // erase text before first setText()
  private char convert = CONVERT_NONE;              // character conversion, default is NONE
  private char adjust = ADJUST_TRIM;                // text adjustment, default is TRIM
  private boolean autoSelect;                       // true if autoselect on focus gained
  private boolean inhibitAutoSelect;                // inhibit autoselect once
  private boolean autoNext;                         // transfer focus to next field if all chars entered
  private int maxColumns;                           // max. number of columns. 0 if no limit
  private String validChars;                        // valid characters, null = all
  private String invalidChars;                      // invalid characters, null = none
  private char filler = ' ';                        // the fill character (defaults to space)
  private boolean override;                        // true if override, else insert mode
  private Window parentWindow;                      // parent window (cached)
  private TooltipDisplay tooltipDisplay;            // tooltip display
  
  // default actions
  private Action upAction;                          // action for up-key
  private Action downAction;                        // action for down-key
  
  

  /**
   * Creates a {@code FormTextArea}.
   * 
   * @param doc the document model, null if default {@link FormFieldDocument}.
   * @param text the text to be displayed, null if none
   * @param rows the number of rows >= 0
   * @param columns the number of columns >= 0
   */
  public FormTextArea(Document doc, String text, int rows, int columns) {

    // setup parent class first
    super (doc, text, rows, columns);

    // add Key mappings
    setupActions();

    // enable drag support by default
    setDragEnabled(true);

    /**
     * we need a special TransferHandler because of the Focus-Events the dropped
     * text would be removed again. 
     */
    setTransferHandler(new TransferHandler("text") {
      private static final long serialVersionUID = 7928262548574524310L;
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
          insert(text, getCaretPosition());
          fireValueEntered();   // store in Field bec. of focus events
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
    
    // get default actions
    upAction = getActionMap().get(DefaultEditorKit.upAction);
    downAction = getActionMap().get(DefaultEditorKit.downAction);
  }
  
  
  /**
   * Creates a {@code FormTextArea} with the default document model.
   * 
   * @param text the text to be displayed, null if none
   * @param rows the number of rows >= 0
   * @param columns the number of columns >= 0
   */
  public FormTextArea (String text, int rows, int columns)  {
    this (null, text, rows, columns);
  }

  /**
   * Creates a {@code FormTextArea} with the default document model.
   * 
   * @param text the text to be displayed, null if none
   */
  public FormTextArea (String text) {
    this (null, text, 0, 0);
  }

  /**
   * Creates an empty {@code FormTextArea} with the default document model.
   * 
   * @param rows the number of rows >= 0
   * @param columns the number of columns >= 0
   */
  public FormTextArea (int rows, int columns)  {
    this (null, null, rows, columns);
  }

  /**
   * Creates an empty {@code FormTextArea} with the default document model.
   */
  public FormTextArea () {
    this (null, null, 0, 0);
  }

  
  
  
 
  /**
   * Returns whether smart enter is enabled.
   * 
   * @return true if smart enter is enabled (default)
   */
  public boolean isSmartEnter() {
    return smartEnter;
  }
  
  /**
   * Enable smart enter.
   * When enabled pressing the Enter-key at the start of the
   * text area (as long as there are no other keys typed or mouse clicks)
   * will move to the next field and will _not_ insert a newline.
   * Inserting a newline can be achieved with Ctrl+Enter.
   * 
   * @param smartEnter true to enable smart enter (default)
   */
  public void setSmartEnter(boolean smartEnter) {
    this.smartEnter = smartEnter;
  }
  
  /**
   * Transfers the focus to the next field if it is
   * the first key pressed in this JTextArea (and the cursor hasn't
   * been moved by the mouse). This allows the user to ENTER
   * through all fields in a mask without worrying about textareas.
   * In all other cases ENTER inserts a newline and moves the cursor
   * after the newline (as expected).
   */
  public void doSmartEnter() {
    if (enterWillInsert && smartEnter)  {
      insert("\n", getCaretPosition());
    }
    else  {
      prepareFocusLost();
      transferFocus();
    }
  }
  

  
  /**
   * Adds the specified action listener to receive 
   * action events from this textfield.
   *
   * @param l the action listener to be added
   */ 
  public synchronized void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  /**
   * Removes the specified action listener so that it no longer
   * receives action events from this textfield.
   *
   * @param l the action listener to be removed
   */ 
  public synchronized void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }


  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.  The event instance 
   * is lazily created.
   * The listener list is processed in last to
   * first order.
   */
  protected void fireActionPerformed() {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    ActionEvent e = new ActionEvent(
                  this, ActionEvent.ACTION_PERFORMED,
                  getText(),
                  EventQueue.getMostRecentEventTime(), 0);

    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==ActionListener.class) {
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }          
    }
  }

  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden for consistent colors.
   * A non-editable text area should have the same color
   * as a non-editable text field (in panals).
   */
  @Override
  public void setEditable(boolean b) {
    super.setEditable(b);
    if (!isCellEditorUsage())  {
      setBackground(b ? 
                    PlafGlobal.textFieldBackgroundColor : 
                    PlafGlobal.textFieldInactiveBackgroundColor);
    }
  }
  
  
 
  /**
   * {@inheritDoc}
   * <p>
   * Overridden for enhanced focus handling
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
   * Overridden due to smart enter.
   */
  @Override
  protected void processKeyEvent(KeyEvent e)  {
    super.processKeyEvent(e);
    if (e.getID() == KeyEvent.KEY_PRESSED)  {
      enterWillInsert = true;
    }
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to smart enter.
   */
  @Override
  protected void processMouseEvent(MouseEvent e)  {
    super.processMouseEvent(e);
    if (e.getID() == MouseEvent.MOUSE_PRESSED)  {
      enterWillInsert = true;
    }
  }
  


  /**
   * {@inheritDoc}
   * <p>
   * Overridden to create a {@link FormFieldDocument}.
   */
  @Override
  protected Document createDefaultModel ()  {
    return new FormFieldDocument (this);
  }

  
  /**
   * Calculates the dimensions for displaying the text according 
   * to the number of lines and max. chars in lines.
   * If the text area is empty the preferred size is returned.
   * 
   * @return the optimal size
   */
  public Dimension getOptimalSize() {
    String text = getText();
    if (text != null) {
      FontMetrics fm = getFontMetrics(getFont());
      StringTokenizer tokenizer = new StringTokenizer(text, "\n");
      int numberOfLines = tokenizer.countTokens();
      int width = 0;
      for(int i=0; i < numberOfLines; i++) {
        int lineWidth = fm.stringWidth(tokenizer.nextToken());
        if (lineWidth > width) {
          width = lineWidth;
        } 
      }
      Insets insets = getInsets();
      return new Dimension(width + insets.left + insets.right, 
                           fm.getHeight() * numberOfLines + insets.bottom + insets.top);
    }
    return getPreferredSize();
  }
  
  

  @Override
  public String getToolTipText() {
    return getTooltipDisplay() == null ? super.getToolTipText() : null;
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to implement wasTransferFocus
   */
  @Override
  public void transferFocus() {
    transferFocusDone = true;
    super.transferFocus();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to implement wasTransferFocusBackward
   */
  @Override
  public void transferFocusBackward() {
    transferFocusBackwardDone = true;
    super.transferFocusBackward();
  }


  

  // --------------------------- implements FormComponent ---------------------
  

  
  public void setFormTraversable(boolean formTraversable) {
    this.formTraversable = formTraversable;
  }
  
  public boolean isFormTraversable() {
    return formTraversable;
  }
  
  
  public void setFormValue (Object object)  {
    if (object != null)  {
      setText (object.toString());   // this allows all objects to setFormValue()
    }
    else  {
      clearText();
    }
  }
  
  public String getFormValue ()  {
    return getText();
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
  
  public void requestFocusLater() {
    FormHelper.requestFocusLater(this);
  }
  
 
  public synchronized void addValueListener (ValueListener l) {
     listenerList.add (ValueListener.class, l);
  }

  public synchronized void removeValueListener (ValueListener l) {
     listenerList.remove (ValueListener.class, l);
  }

  public void fireValueChanged () {
    FormHelper.doFireValueChanged (this, listenerList.getListenerList());
    valueChangedTriggered = false;
    enterWillInsert = false;
  }

  public void fireValueEntered () {
    valueChangedTriggered = false;  // check always after field exit
    FormHelper.doFireValueEntered (this, listenerList.getListenerList());
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
          return value.compareTo(savedValue) != 0;
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
      valueChangedTriggered = true;
    }
  }
  
  
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
      UIGlobal.logger.severe("FormTextArea: isTableCellEditor not Boolean! " + ex);
    }
    return false;
  }
  

  public void setChangeable (boolean changeable)  {
    if (honourChangeable) {
      setEditable(changeable);
    }
  }

  public boolean isChangeable() {
    return isEditable();
  }
  
  public void setHonourChangeable(boolean honourChangeable) {
    this.honourChangeable = honourChangeable;
  }
  
  public boolean isHonourChangeable() {
    return this.honourChangeable;
  }
  

  public void prepareFocusLost()  {
    if (isCellEditorUsage() == false) {
      performFocusLost();
      skipNextFocusLost = true;
    }
    // else: triggered by FOCUS_LOST
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
  
  
  

  
  
  // --------------------------- implements FormFieldComponent -----------------

  public void doActionPerformed()  {
    fireActionPerformed();
    transferFocusByEnter = true;
  }


  @Override
  public void setText (String str)  {
    if (str == null) {
      str = StringHelper.emptyString;
    }
    
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
      if (doc instanceof FormFieldDocument) {
        ((FormFieldDocument)doc).setEraseFirst(true);
      }
      eraseFirst = false;
    }
    
    setCaretLeft(); // always start at left side (useful for long text not fitting into field)
  }
  

  public void clearText() {
    setText(StringHelper.emptyString);
  }

  
  
  public boolean isCaretLeft()  {
    return getCaretPosition() == 0;
  }
  
  public boolean isCaretRight() {
    return getCaretPosition() == getDocument().getLength();
  }
  
  
  public void setCaretLeft()  {
    setCaretPosition(0);
  }

  public void setCaretRight() {
    setCaretPosition (getDocument().getLength());
  }
  
  
  public void upLeft()  {
    if (isCaretLeft())  {
      // go to previous field if we already are at start
      transferFocusBackward();
    }
    // else move up one line
    if (upAction != null) {
      int pos = getCaretPosition();
      upAction.actionPerformed(new ActionEvent(this, 0, null));
      if (pos == getCaretPosition())  {
        // we are already in first line
        setCaretLeft();
      }
    }
  }

  public void downRight()  {
    if (isCaretRight())  {
     // go to next field if we already stay at right bound
      transferFocus();
    }
    // else move down one line
    if (downAction != null) {
      int pos = getCaretPosition();
      downAction.actionPerformed(new ActionEvent(this, 0, null));
      if (pos == getCaretPosition())  {
        // we are already in last line
        setCaretRight();
      }
    }
  }


  public void setConvert (char convert)  {
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

  public char getConvert ()  {
    return convert;
  }


  public  void  setAutoSelect (boolean autoselect)  {
    this.autoSelect = autoselect;
  }

  public boolean isAutoSelect()  {
    return autoSelect;
  }


  public  void  setAutoUpdate (boolean autoupdate)  {
    this.autoUpdate = autoupdate;
  }

  public boolean isAutoUpdate()  {
    return autoUpdate;
  }

  public  void  setValidChars (String str)  {
    this.validChars = str.length() == 0 ? null : str;
  }

  public String getValidChars ()  {
    return validChars;
  }

  public  void  setInvalidChars (String str)  {
    this.invalidChars = str.length() == 0 ? null : str;
  }

  public String getInvalidChars ()  {
    return invalidChars;
  }

  public  void  setFiller (char filler)  {
    char oldfiller = this.filler;
    this.filler = filler;
    // this will set the filler
    setText (getText().replace(oldfiller, filler));
  }

  public char getFiller ()  {
    return filler;
  }

  public  void  setOverwrite (boolean override)  {
    this.override = override;
  }

  public boolean isOverwrite()  {
    return override;
  }
  
  
  public void setEraseFirst(boolean erasefirst) {
    this.eraseFirst = erasefirst;
    if (erasefirst == false)  {
      Document doc = getDocument();
      if (doc instanceof FormFieldDocument) {
        ((FormFieldDocument)doc).setEraseFirst(false);
      }
    }
  }

  public boolean isEraseFirst()  {
    return eraseFirst;
  }
  

  /**
   * The format will be ignored in FormTextArea.
   */
  public void setFormat (String pattern) {
    // do nothing
  }

  /**
   * The format will be ignored in FormTextArea.
   * @return the empty string
   */
  public String getFormat ()  {
    return StringHelper.emptyString;
  }

  /**
   * The format will be ignored in FormTextArea.
   * @return just returns the given string
   */
  public String doFormat (Object object) {
    return object == null ? null : object.toString();
  }


  public void  setAutoNext (boolean autonext) {
    this.autoNext = autonext;
  }

  public boolean isAutoNext()  {
    return autoNext;
  }
  
  
  public  void  setAdjust (char adjust) {
    this.adjust = adjust;
  }
    
  public  char  getAdjust ()  {
    return adjust;
  }


  
  public  void  setMaxColumns (int maxColumns) {
    this.maxColumns = maxColumns;
  }

  public int  getMaxColumns ()  {
    return maxColumns;
  }
  
  
  /**
   * Not implemented in FormTextArea.
   * <p>
   * {@inheritDoc}
   */
  public void setHorizontalAlignment(int alignment) {
    this.horizontalAlignment = alignment;
  }

  /**
   * Not implemented in FormTextArea.
   * <p>
   * {@inheritDoc}
   */
  public int  getHorizontalAlignment() {
    return horizontalAlignment;
  }

  
  
  public void setVerticalAlignment(int alignment) {
    this.verticalAlignment = alignment;
  }

  public int  getVerticalAlignment()  {
    return verticalAlignment;
  }

  
  public boolean isInhibitAutoSelect() {
    return inhibitAutoSelect;
  }
  
  public void setInhibitAutoSelect(boolean inhibitAutoSelect) {
    this.inhibitAutoSelect = inhibitAutoSelect;
  }
  

  public void setFireRunning(boolean fireRunning) {
    this.fireRunning = fireRunning;
  }

  public boolean isFireRunning()  {
    return fireRunning;
  }

  public void setFormWrapWindow(FormWindow parent)  {
    formWrapWindow = parent;
  }
  

  
  public StringConverter getConverter() {
    Document doc = getDocument();
    if (doc instanceof FormFieldDocument) {
      return ((FormFieldDocument)doc).getConverter();
    }
    return null;
  }
  
  public void setConverter(StringConverter converter) {
    Document doc = getDocument();
    if (doc instanceof FormFieldDocument) {
      ((FormFieldDocument)doc).setConverter(converter);
    }
  }

  
  /**
   * TextArea's don't provide formatting and thus no
   * possibilities for errors.
   * @return always -1 (no error)
   */
  public int getErrorOffset () {
    return -1;
  }
  
  
  public boolean isEmpty()  {
    String text = getText();
    return text == null || text.length() == 0;
  }
  
  
  public void setStartEditLeftmost (boolean startEditLeftmost)  {
    this.startEditLeftmost = startEditLeftmost;
  }

  public boolean isStartEditLeftmost()  {
    return startEditLeftmost;
  }




  
  

  /**
   * register actions.
   * We make some important changes to default behaviour here:
   *
   * 1. TAB will *not* insert a tab as in default swing. 
   *    Instead it will transfer focus. This allows the user to tab
   *    through all fields in a mask without accidently inserting tabs
   *    when tabbing into a JTextArea.
   *
   * 2. Instead, Ctrl-TAB will insert a tab. (is there any application
   *    using tabs in JTextAreas, seriously??)
   *
   * 3. Shift-TAB will transfer the focus to the previous field as expected.
   *    The default swing does nothing.
   *
   * 4. ENTER will transfer the focus to the next field if it is
   *    the first key pressed in this JTextArea (and the cursor hasn't
   *    been moved by the mouse). Again, this allows the user to ENTER
   *    through all fields in a mask without worrying about textareas.
   *    In all other cases ENTER inserts a newline and moves the cursor
   *    after the newline (as expected).
   *
   * 5. Shift-ENTER moves to the previous field (as expected)
   *
   * 6. Ctrl-ENTER inserts a newline at the current caret position but
   *    leaves the cursor where it was.
   *
   * Notice: if the FormTextArea is used as a TableCellEditor, ENTER will transfer focus
   * and Ctrl-ENTER will insert a newline and move the cursor *after* the newline.
   */
  private void setupActions () {

    // default keys
    FormHelper.setupDefaultBindings(this);
    
    // replace focus traversal keys and install expected TAB/Shift-TAB behavious
    HashSet<AWTKeyStroke> set = new HashSet<AWTKeyStroke>();
    set.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
    setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);
    set = new HashSet<AWTKeyStroke>();
    set.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
    setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
    
    // insert tab
    Action tabAction = new AbstractAction ("insertTab") {
      public void actionPerformed (ActionEvent e)  {
        insert("\t", getCaretPosition());
      }
    };
    getActionMap().put(tabAction.getValue(Action.NAME), tabAction);
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.CTRL_MASK),
                           tabAction.getValue(Action.NAME));    
    
    
    // insert newline and keep caret position
    Action newlineAction = new AbstractAction ("insertNewline") {
      public void actionPerformed (ActionEvent e)  {
        int pos = getCaretPosition();
        insert("\n", pos);
        if (smartEnter)  {
          setCaretPosition(pos);
        }
      }
    };
    getActionMap().put(newlineAction.getValue(Action.NAME), newlineAction);
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.CTRL_MASK),
                           newlineAction.getValue(Action.NAME));    
  }
  
  


  /**
   * handle focus gained
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
    if (autoUpdate) {
      // update the fields display
      fireValueChanged ();
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
    inhibitAutoSelect = false;
    formWrapWindow = null;
    transferFocusByEnter = false;
    showTooltip(super.getToolTipText());
  }

  
  /**
   * handle focus lost
   */
  private void performFocusLost ()  {
    if (autoUpdate) {
      // update data field and show what has been read
      fireValueEntered();
    }
    setEraseFirst(false);
    performWrapEvent();
    showTooltip(null);
  }
  
  
  /**
   * handle form wrap
   */
  private void performWrapEvent() {
    if (formWrapWindow != null)  {
      formWrapWindow.fireFormWrappedFocus(new FormWrapEvent(this));
      formWrapWindow = null;
    }    
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
   * gets the tooltip diaplay
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
  
}
