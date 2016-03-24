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

// $Id: FormFileChooser.java 336 2008-05-09 14:40:20Z harald $
// Created on September 16, 2002, 10:34 AM

package org.tentackle.ui;


import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.tentackle.plaf.PlafGlobal;


/**
 * A FormField for files decorated with buttons to link and unlink to a file.
 * 
 * @author harald
 */
public class FormFileChooser extends FormFieldComponentPanel {
  
  
  private String startDir;            // Directory where to open (null = user's home)
  private String description;         // kind of selection
  private int    fileSelectionMode;   // one of FileChooser.XXXX
  private String[] extensions;        // allowed File-Extensions (null = all)
  private boolean changeable = true;  // the changeable state
  
  /**
   * Creates a file chooser field.
   * 
   * @param startDir the directory where to start browsing
   * @param description some description of the file type
   * @param fileSelectionMode the selection mode
   * @param extensions the file extensions
   * @see JFileChooser
   */
  public FormFileChooser(String startDir, String description, int fileSelectionMode, String[] extensions) {
    this.startDir = startDir;
    this.description = description; 
    this.fileSelectionMode = fileSelectionMode;
    this.extensions = extensions;
    initComponents();
    // pathField retrieved by getFormFieldComponent() below
  }
  
  
  
  /**
   * Creates a file chooser field for files and directories.
   * @see JFileChooser
   */
  public FormFileChooser()  {
    this(null, null, JFileChooser.FILES_AND_DIRECTORIES, null);
  }
  
  
  
  /**
   * Sets the selection mode.
   * 
   * @param mode the selection mode
   * @see JFileChooser
   */
  public void setFileSelectionMode(int mode)  {
    this.fileSelectionMode = mode;
  }
  
  /**
   * Gets the selection mode.
   * 
   * @return the selection mode
   */
  public int getFileSelectionMode() {
    return this.fileSelectionMode;
  }
  
  /**
   * Sets the file name extensions.
   * 
   * @param ext the array of extensions.
   */
  public void setExtensions(String[] ext) {
    this.extensions = ext;
  }
  
  /**
   * Gets the file name extensions.
   * 
   * @return the array of extensions.
   */
  public String[] getExtensions() {
    return this.extensions;
  }
  
  /**
   * Sets the file type description.
   * 
   * @param desc the short description
   */
  public void setDescription(String desc) {
    this.description = desc;
  }
  
  /**
   * Gets the file type description.
   * 
   * @return the short description
   */
  public String getDescription()  {
    return this.description;
  }
  
  /**
   * Sets the starting directory.
   * @param dir the directory name
   * @see JFileChooser
   */
  public void setStartDir(String dir) {
    this.startDir = dir;
  }
  
  /**
   * Gets the starting directory.
   * @return the directory name
   */
  public String getStartDir()  {
    return this.startDir;
  }  
  
  
  
  @Override
  public boolean requestFocusInWindow() {
    return browseButton.requestFocusInWindow();
  }
    
  @Override
  public void setChangeable(boolean changeable) {
    this.changeable = changeable;
    browseButton.setEnabled(changeable);
    unlinkButton.setEnabled(changeable);
  }  
  
  @Override
  public boolean isChangeable() {
    return changeable;
  }  
  
  
  
  /**
   * FileChoose Filter
   */
  private class ExtensionFileFilter extends FileFilter  {
    
    public String getDescription()  {
      return description;
    }
    
    public boolean accept(File f) {
      if (f.isDirectory()) {
        return true; // always accept to browse
      }    // always accept to browse
      if (f.isFile()) {
        if (extensions != null) {
          // Filter extensions
          String name = f.getName();
          for (int i=0; i < extensions.length; i++) {
            if (name.endsWith(extensions[i])) {
              return true;
            }
          }
        }
      }
      return false;
    }
    
  }
  
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    pathField = (StringFormField)getFormFieldComponent();
    browseButton = new org.tentackle.ui.FormButton();
    unlinkButton = new org.tentackle.ui.FormButton();

    setLayout(new java.awt.GridBagLayout());

    pathField.setEditable(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    add(pathField, gridBagConstraints);

    browseButton.setFormTraversable(true);
    browseButton.setIcon(PlafGlobal.getIcon("open"));
    browseButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    browseButton.setToolTipText("\u00f6ffnen");
    browseButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        browseButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    add(browseButton, gridBagConstraints);

    unlinkButton.setFormTraversable(true);
    unlinkButton.setIcon(PlafGlobal.getIcon("clear"));
    unlinkButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    unlinkButton.setToolTipText("l\u00f6schen");
    unlinkButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        unlinkButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    add(unlinkButton, gridBagConstraints);

  }// </editor-fold>//GEN-END:initComponents

  private void unlinkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlinkButtonActionPerformed
    pathField.clearText();
    fireValueEntered();
  }//GEN-LAST:event_unlinkButtonActionPerformed

  private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
    JFileChooser fc = new JFileChooser(startDir);
    fc.setFileFilter(new ExtensionFileFilter());
    fc.setFileSelectionMode(fileSelectionMode);
    if  (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)  {
      pathField.setText(fc.getSelectedFile().getPath());
      fireValueEntered();
    }
  }//GEN-LAST:event_browseButtonActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private org.tentackle.ui.FormButton browseButton;
  private org.tentackle.ui.StringFormField pathField;
  private org.tentackle.ui.FormButton unlinkButton;
  // End of variables declaration//GEN-END:variables
  
}

