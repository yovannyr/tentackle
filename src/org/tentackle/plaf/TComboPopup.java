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

// $Id: TComboPopup.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.plaf;

import org.tentackle.ui.FormComboBox;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboPopup;

/**
 * Popup for comboboxes displaying {@link org.tentackle.util.ShortLongText} values.
 * 
 * @author harald
 */
public class TComboPopup extends BasicComboPopup {
  
  /** listener for changing the popupRenderer **/
  protected PropertyChangeListener tPropertyChangeListener; 
  
  
  
  /**
   * Creates a combo popup.
   * 
   * @param combo the combobox
   */
  public TComboPopup(JComboBox combo) {
    super(combo);
  }
  
  
  @Override
  public void uninstallingUI() {
    // remove extra listener for popupRenderer
    if (tPropertyChangeListener != null) {
      comboBox.removePropertyChangeListener(tPropertyChangeListener);
    }
    super.uninstallingUI();
  }
  
  
  /**
   * Gets the listener for popup renderer property changes
   * and creates it if it does not exist.
   * 
   * @return the listener
   */
  protected PropertyChangeListener createTPropertyChangeListener() {
    if (tPropertyChangeListener == null)  {
      tPropertyChangeListener = new THandler();
    }
    return tPropertyChangeListener;
  }
  
  
  @Override
  protected void installComboBoxListeners() {
    // create extra listener for popupRenderer
    if ((tPropertyChangeListener = createTPropertyChangeListener()) != null) {
      comboBox.addPropertyChangeListener(tPropertyChangeListener);
    }
    super.installComboBoxListeners();
  }
  
  
  @Override
  protected void configureList() {
    super.configureList();
    if (comboBox instanceof FormComboBox) {
      ListCellRenderer popupRenderer = ((FormComboBox)comboBox).getPopupRenderer();
      if (popupRenderer != null)  {
        /**
         * Sets the popup renderer if different from standard renderer
         */
        list.setCellRenderer(popupRenderer);
      }
    }
  }
  
  
  
 
  /** 
   * Computes the minimum width of the popupmenu.
   * The method is only invoked if a popupRenderer is available.
   */
  private int getMinimumDisplayWidth() {
      
    int width = 0;

    // Calculate the dimension by iterating over all the elements in the combo
    // box list.
    ComboBoxModel model = comboBox.getModel();
    int modelSize = model.getSize();
    ListCellRenderer renderer = list.getCellRenderer();
    if (comboBox instanceof FormComboBox) {
      /**
       * If the renderer has changed (PlasticComboBoxUI overwrites it on every property change)
       * set the list cell renderer to the popup renderer (if not the default)
       */
      ListCellRenderer popupRenderer = ((FormComboBox)comboBox).getPopupRenderer();
      if (popupRenderer != null) {
        renderer = popupRenderer;
        list.setCellRenderer(popupRenderer);
      }
    }
    if (renderer instanceof TPopupRenderer) {
      // the left label is always in combobox-width
      ((TPopupRenderer)renderer).setMinLeftWidth(comboBox.getPreferredSize().width);
      int leftWidth  = 0;
      int rightWidth = 0;
      for (int i = 0; i < modelSize ; i++ ) {
        // this will layout
        renderer.getListCellRendererComponent(
                            list, model.getElementAt(i), -1, false, false).getPreferredSize();
        leftWidth  = Math.max(leftWidth, ((TPopupRenderer)renderer).getLeftWidth());
        rightWidth = Math.max(rightWidth, ((TPopupRenderer)renderer).getRightWidth());
      }
      ((TPopupRenderer)renderer).setMinLeftWidth(leftWidth);
      ((TPopupRenderer)renderer).setMinRightWidth(rightWidth);
      width = leftWidth + rightWidth + 3;   // the 3 comes from FlowLayout in TPopupRenderer
    }
    else  {
      for (int i = 0; i < modelSize ; i++ ) {
        // Calculates the maximum height and width based on the largest
        // element
        width = Math.max(width, renderer.getListCellRendererComponent(
                                    list, model.getElementAt(i), -1, false, false).getPreferredSize().width);
      }
    }

    return width;
  }

  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to enlarge the width of the popup if an extra popupRenderer is used
   * and shows more information than the default renderer.
   */
  @Override
  protected Rectangle computePopupBounds(int px,int py,int pw,int ph) {
    Rectangle rect = super.computePopupBounds(px, py, pw, ph);
    if (comboBox instanceof FormComboBox && ((FormComboBox)comboBox).getPopupRenderer() != null)  {
      rect.width = Math.max(rect.width, getMinimumDisplayWidth() + 
                    scroller.getVerticalScrollBar().getPreferredSize().width);  
                    // scrollbar added dynamically, but ComboBox does not adjust size of popup.
    }
    return rect;
  }
  
  
  
  
  private class THandler implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent e) {
      JComboBox comboBox = (JComboBox)e.getSource();
      String propertyName = e.getPropertyName();
      if (propertyName.equals("popupRenderer")) {
        // set the popupRenderer if changed, null = use default renderer
        ListCellRenderer renderer = ((FormComboBox)comboBox).getPopupRenderer();
        list.setCellRenderer(renderer == null ? comboBox.getRenderer() : renderer);
        if (isVisible()) {
          hide();
        }
      }
      else if (propertyName.equals("shortLongPopup")) {
        // set the popupRenderer if changed, null = use default renderer
        boolean flag = ((FormComboBox)comboBox).isShortLongPopupEnabled();
        if (flag) {
          ((FormComboBox)comboBox).setPopupRenderer(new TPopupRenderer());
        }
        else  {
          ((FormComboBox)comboBox).setPopupRenderer(null);
        }
        // this will trigger the propertyChangeEvent "popupRenderer"
      }
    }
  }
  
}
