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

// $Id: AppDbObjectPanel.java 408 2008-09-01 13:54:55Z harald $

package org.tentackle.appworx;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.List;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormPanel;


/**
 * Panel to view and/or edit an {@link AppDbObject}.
 * <p>
 * The panel is designed to used as a plugin by {@link AppDbObjectDialog}.
 * Notice: the class should be abstract but most GUI-designers cannot
 * handle abstract classes and need a bean to instantiate.
 * Hence, some methods that should be abstract as well are non-abstract
 * default implementations that *must* be overridden.
 * 
 * @author harald
 */
public class AppDbObjectPanel extends FormPanel implements Printable, Pageable {
  
  /** the enclosing {@link AppDbObjectDialog}, null if none **/
  protected AppDbObjectDialog dialog;
  
  private PageFormat defaultPageFormat;  // default page format
  

  /**
   * Sets the database object and updates the view.
   * <p>
   * The default implementation returns false.
   * Must be overridden!
   * 
   * @param object the database object
   * @return true if object accepted
   */
  public boolean setObject (AppDbObject object)  {
    return false;
  }

  /**
   * Gets the currently displayed database object.
   * <p>
   * The default implementation returns null.
   * Must be overridden!
   * 
   * @return the database object
   */
  public AppDbObject getObject ()  {
    return null;
  }
  

  /**
   * Verifies the data object.
   * <p>
   * The default implementation invokes {@link AppDbObject#verify()}.
   * 
   * @return the list of errors, never null
   */
  public List<InteractiveError> verifyObject() {
    return getObject().verify();
  }
  
  
  /**
   * Checks the object for consistency before it is saved.<br>
   * 
   * This method must *NOT* do any modifications to the database.
   * It is invoked from {@link AppDbObjectDialog} within the transaction
   * of saving the object.
   * <p>
   * The default implementation invokes {@link #verifyObject()}.
   *
   * @return true if continue tx, false if rollback tx
   */
  public boolean prepareSave() { 
    List<InteractiveError> errorList = verifyObject();
    // show/clear errors
    if (dialog != null) {
      TooltipAndErrorPanel ttep = dialog.getTooltipAndErrorPanel();
      if (ttep != null) {
        ttep.setErrors(errorList);
      }
    }
    return errorList.isEmpty();
  }

  
  /**
   * Prepares the cancel operation.<br>
   * 
   * Invoked when the dialog is closed or the object
   * is removed from the panel in general.
   * <p>
   * The default implementation returns true.
   *
   * @return true if cancel is ok, false if not
   */
  public boolean prepareCancel() {
    return true;
  }

  
  /**
   * Prepares to create a new object.<br>
   * 
   * Invoked when the user instantiates a new object
   * (presses the "new" button, for example).
   * <p>
   * The default implementation returns true.
   *
   * @return true if creating new object is ok, false if not
   */
  public boolean prepareNew() {
    return true;
  }

  
  /**
   * Prepares to search for objects.<br>
   * 
   * Invoked when the user wants to search for objects
   * (presses the "search" button, for example).
   * <p>
   * The default implementation returns true.
   *
   * @return true if search is ok, false if not
   */
  public boolean prepareSearch() {
    return true;
  }

  
  /**
   * Prepares to delete the current object.<br>
   * 
   * Invoked when the user wants to delete the current object
   * (presses the "delete" button, for example).
   * <p>
   * The default implementation returns true.
   *
   * @return true if delete is ok, false if not
   */
  public boolean prepareDelete() {
    return true;
  }


  /**
   * Sets the parent {@code AppDbObjectDialog}.
   * <p>
   * Invoked from {@link AppDbObjectDialog} when this panel
   * is "plugged in".
   * 
   * @param dialog the parent dialog
   */
  public void setAppDbObjectDialog (AppDbObjectDialog dialog) {
    this.dialog = dialog;
  }

  /**
   * Gets the parent AppDbObjectDialog.
   * 
   * @return the parent dialog, null if this panel is not a child of an {@code AppDbObjectDialog}.
   */
  public AppDbObjectDialog getAppDbObjectDialog() {
    return dialog;
  }

  
  /**
   * Sets the initial focus.<br>
   * 
   * Usually requests the focus for the top left component.
   * The default implementation does nothing.
   * Must be overridden!
   */
  public void setInitialFocus() {
    // override this!!!
  }
  
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden to generate the title from the current object
   * if title is null.
   */
  @Override
  public String getTitle()  {
    String title = super.getTitle();
    if (title == null) {
      AppDbObject obj = getObject();
      if (obj != null)  {
        String contextName = obj.getBaseContext().toString();
        if (obj.isNew()) {
          title = contextName == null || contextName.length() == 0 ? 
                    obj.getMultiName() : 
                    obj.getMultiName() + " in " + contextName;
        }
        else  {
          title = obj.getSingleName() + " " +
                    (contextName == null || contextName.length() == 0 ? 
                      obj.toString() : 
                      obj.toString() + " in " + contextName);
        }
      }
    }
    return title;
  }

  
  /**
   * Packs the parent window.
   */
  public void pack() {
    if (dialog != null) {
      dialog.pack();
    }
    else {
      FormHelper.packParentWindow(this);
    }
  }
  
  
  /**
   * Gets the {@link Pageable} of this panel.<br>
   * 
   * AppDbObjectDialog invokes this method if the "print" button is pressed.
   * If this method returns null, getPrintable() will be invoked.
   * The default implementation returns null.
   * @param printJob the printer job
   * @return the pageable, null if try getPrintable
   */
  public Pageable getPageable(PrinterJob printJob) {
    defaultPageFormat = printJob.defaultPage();
    return null;
  }

  
  /**
   * Gets the {@link Printable} of this panel.<br>
   * 
   * AppDbObjectDialog invokes this method if the "print" button is pressed
   * and getPageable() returned null.
   * The default implementation returns {@code this}, which is simply
   * a screendump of this panel.
   * 
   * @param printJob the printer job
   * @return the printable, null if printing is disabled
   */
  public Printable getPrintable(PrinterJob printJob) {
    defaultPageFormat = printJob.defaultPage();
    return this;
  }


  /**
   * Indicates a successful printout.<br>
   * 
   * The default implementation does nothing.
   * Can be used to set a printing date in an object or alike.
   */
  public void markPrinted() {
  }

  // ------------------------------ implements Printable ---------------------
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation prints a screendump of this panel.
   */
  public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
    if (pageIndex > 0) {
      return(Printable.NO_SUCH_PAGE);
    }
    else {
      Graphics2D g2d = (Graphics2D)graphics;
      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
      double scaleX = pageFormat.getImageableWidth() / getWidth();
      double scaleY = pageFormat.getImageableHeight() / getHeight();
      double scale  = scaleX < scaleY ? scaleX : scaleY;
      if (scale < 1.0)  {
        g2d.scale(scale, scale);
      }
      paint(g2d);
      return(Printable.PAGE_EXISTS);
    }
  }


  // ----------------- implements Pageable -------------------------------
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns {@code this}.<br>
   */
  public Printable getPrintable(int pageIndex) {
    return this;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns the default pageformat of the printjob.
   */
  public PageFormat getPageFormat(int pageIndex) {
    return defaultPageFormat;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation returns {@code UNKNOWN_NUMBER_OF_PAGES}.
   */
  public int getNumberOfPages() {
    return Pageable.UNKNOWN_NUMBER_OF_PAGES;
  }

}
