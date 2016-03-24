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
 */// $Id: TTinyTextFieldUI.java 440 2008-09-23 16:28:35Z harald $
package org.tentackle.plaf.tinylaf;

import de.muntjak.tinylookandfeel.TinyTextFieldUI;
import org.tentackle.plaf.TFieldView;
import org.tentackle.ui.FormFieldComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.GlyphView;
import javax.swing.text.Position;
import javax.swing.text.ViewFactory;
import javax.swing.text.ParagraphView;
import javax.swing.text.Element;
import javax.swing.text.View;

/**
 * UI for JTextField/FormField.
 * Adds vertical alignment (i.e. in FormTables with autoRowHeight).
 * 
 * @author  harald
 */
public class TTinyTextFieldUI extends TinyTextFieldUI {
  // ********************************
  //        Create PlAF
  // ********************************
  public static ComponentUI createUI(JComponent c) {
    return new TTinyTextFieldUI(c);
  }

  public TTinyTextFieldUI(JComponent c) {
    super();
  }

  /**
   * Creates a new BasicTextFieldUI.
   */
  public TTinyTextFieldUI() {
    super();
  }
  
  
  
  // copied from BasicTextFieldUI...
  @Override
  public View create(Element elem) {
    Document doc = elem.getDocument();
    Object i18nFlag = doc.getProperty("i18n"/*AbstractDocument.I18NProperty*/);
    if ((i18nFlag != null) && i18nFlag.equals(Boolean.TRUE)) {
      // To support bidirectional text, we build a more heavyweight
      // representation of the field.
      String kind = elem.getName();
      if (kind != null) {
        if (kind.equals(AbstractDocument.ContentElementName)) {
          return new GlyphView(elem);
        }
        else if (kind.equals(AbstractDocument.ParagraphElementName)) {
          return new I18nFieldView(elem);   // <<<<<<<<<<< this is what we have to change!
        }
      }
      // this shouldn't happen, should probably throw in this case.
    }
    return new TFieldView(elem); // // <<<<<<<<<<< this is what we have to change!
  }

  /**
   * A field view that support bidirectional text via the
   * support provided by ParagraphView.
   */
  static class I18nFieldView extends ParagraphView {

    I18nFieldView(Element elem) {
      super(elem);
    }

    @Override
    public int getFlowSpan(int index) {
      return Integer.MAX_VALUE;
    }

    static boolean isLeftToRight(java.awt.Component c) {
      return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * Adjusts the allocation given to the view
     * to be a suitable allocation for a text field.
     * If the view has been allocated more than the
     * preferred span vertically, the allocation is
     * changed to be centered vertically.  Horizontally
     * the view is adjusted according to the horizontal
     * alignment property set on the associated JTextField
     * (if that is the type of the hosting component).
     *
     * @param a the allocation given to the view, which may need
     *  to be adjusted.
     * @return the allocation that the superclass should use.
     */
    Shape adjustAllocation(Shape a) {
      if (a != null) {
        Rectangle bounds = a.getBounds();
        int vspan = (int) getPreferredSpan(Y_AXIS);
        int hspan = (int) getPreferredSpan(X_AXIS);
        if (bounds.height != vspan) {
          boolean center = true;
          Component c = getContainer();
          // Harr
          if (c instanceof FormFieldComponent) {
            int align = ((FormFieldComponent) c).getVerticalAlignment();
            if (align == JLabel.TOP) {
              bounds.height = vspan;
              center = false;
            }
            else if (align == JLabel.BOTTOM) {
              bounds.y += bounds.height - vspan;
              center = false;
            }
          }
          if (center) {
            int slop = bounds.height - vspan;
            bounds.y += slop / 2;
            bounds.height -= slop;
          }
        }

        // horizontal adjustments
        Component c = getContainer();
        if (c instanceof JTextField) {
          JTextField field = (JTextField) c;
          BoundedRangeModel vis = field.getHorizontalVisibility();
          int max = Math.max(hspan, bounds.width);
          int value = vis.getValue();
          int extent = Math.min(max, bounds.width - 1);
          if ((value + extent) > max) {
            value = max - extent;
          }
          vis.setRangeProperties(value, extent, vis.getMinimum(),
                  max, false);
          if (hspan < bounds.width) {
            // horizontally align the interior
            int slop = bounds.width - 1 - hspan;

            int align = ((JTextField) c).getHorizontalAlignment();
            if (isLeftToRight(c)) {
              if (align == LEADING) {
                align = LEFT;
              }
              else if (align == TRAILING) {
                align = RIGHT;
              }
            }
            else {
              if (align == LEADING) {
                align = RIGHT;
              }
              else if (align == TRAILING) {
                align = LEFT;
              }
            }

            switch (align) {
              case SwingConstants.CENTER:
                bounds.x += slop / 2;
                bounds.width -= slop;
                break;
              case SwingConstants.RIGHT:
                bounds.x += slop;
                bounds.width -= slop;
                break;
            }
          }
          else {
            // adjust the allocation to match the bounded range.
            bounds.width = hspan;
            bounds.x -= vis.getValue();
          }
        }
        return bounds;
      }
      return null;
    }

    void updateVisibilityModel() {
      Component c = getContainer();
      if (c instanceof JTextField) {
        JTextField field = (JTextField) c;
        BoundedRangeModel vis = field.getHorizontalVisibility();
        int hspan = (int) getPreferredSpan(X_AXIS);
        int extent = vis.getExtent();
        int maximum = Math.max(hspan, extent);
        extent = (extent == 0) ? maximum : extent;
        int value = maximum - extent;
        int oldValue = vis.getValue();
        if ((oldValue + extent) > maximum) {
          oldValue = maximum - extent;
        }
        value = Math.max(0, Math.min(value, oldValue));
        vis.setRangeProperties(value, extent, 0, maximum, false);
      }
    }

    // --- View methods -------------------------------------------
    
    @Override
    public void paint(Graphics g, Shape a) {
      Rectangle r = (Rectangle) a;
      g.clipRect(r.x, r.y, r.width, r.height);
      super.paint(g, adjustAllocation(a));
    }

    @Override
    public int getResizeWeight(int axis) {
      if (axis == View.X_AXIS) {
        return 1;
      }
      return 0;
    }

    @Override
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
      return super.modelToView(pos, adjustAllocation(a), b);
    }

    @Override
    public Shape modelToView(int p0, Position.Bias b0,
            int p1, Position.Bias b1, Shape a)
            throws BadLocationException {
      return super.modelToView(p0, b0, p1, b1, adjustAllocation(a));
    }

    @Override
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
      return super.viewToModel(fx, fy, adjustAllocation(a), bias);
    }

    @Override
    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
      super.insertUpdate(changes, adjustAllocation(a), f);
      updateVisibilityModel();
    }

    @Override
    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
      super.removeUpdate(changes, adjustAllocation(a), f);
      updateVisibilityModel();
    }
  }
}
