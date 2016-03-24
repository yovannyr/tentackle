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

// $Id: SecurityTableEntry.java 425 2008-09-14 16:20:54Z harald $
// Created on August 21, 2002, 10:14 AM

package org.tentackle.appworx;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.tentackle.ui.FormButton;
import org.tentackle.ui.FormComponentCellEditor;
import org.tentackle.ui.FormQuestion;
import org.tentackle.ui.FormTableCellRenderer;
import org.tentackle.ui.FormTableEntry;
import org.tentackle.ui.ValueEvent;
import org.tentackle.ui.ValueListener;



/**
 * The default formtable entry for a {@link Security} rule.
 * 
 * @author harald
 */
public class SecurityTableEntry extends FormTableEntry {


  /**
   * By default there is only one grantee-class for the grantee cell editor.
   * Your application *MUST* set this or extend the Security...-classes to
   * add groups, etc...
   */
  public static Class<? extends AppDbObject> granteeClazz;
  
  
  /**
   * For multi-tenancy the base context class and all its child context classes
   * may be defined here.
   * If contextClazz is null (default), the context column will be non-editable.
   * Otherwise a selection dialog for the first of the given class is shown
   * in the context of the table entry's object. All classes in the array
   * are selectable.
   */
  public static Class[] contextClazz;
  
  
  protected static final int GRANTEE    = 0;
  protected static final int CONTEXT    = 1;
  protected static final int PRIVS      = 2;
  protected static final int TEXT       = 3;
  protected static final int ALLOW      = 4;

  protected static final String[] columnNames = {
    "User",         // NOI18N
    "Context",      // NOI18N
    "Permission",   // NOI18N
    "Remark",       // NOI18N
    "allowed"       // NOI18N
  };
  
  
  protected Security security;   // current security record
  

  
  /**
   * Creates a table entry.
   * 
   * @param security the security object
   */
  public SecurityTableEntry(Security security)  {
    this.security = security;
  }


  @Override
  public FormTableEntry newInstanceOf(Object object) {
    return new SecurityTableEntry((Security)object);
  }

  @Override
  public Object getObject() {
    return security;
  }

  @Override
  public String getColumnName(int col) {
    return columnNames[col];
  }
  
  @Override
  public String getDisplayedColumnName(int col) {
    switch (col)  {
      case GRANTEE: return Locales.bundle.getString("User");
      case CONTEXT: return Locales.bundle.getString("Context");
      case PRIVS:   return Locales.bundle.getString("Permission");
      case TEXT:    return Locales.bundle.getString("Remark");
      case ALLOW:   return Locales.bundle.getString("allowed");
    }
    return "?";
  }

  /**
   * {@inheritDoc}
   * <p>
   * All cells are editable, except {@link #CONTEXT} which is only
   * editable if {@link #contextClazz} is not null.
   */
  @Override
  public boolean isCellEditable(int mColumn) {
    return mColumn != CONTEXT || contextClazz != null;
  }

  @Override
  public int getColumnCount() {
    return (columnNames.length);
  }

  @Override
  public Object getValueAt (int col)  {
    try {
      switch (col)  {
        case GRANTEE: return security.getGrantee();
        case CONTEXT: return security.getContextObject();
        case PRIVS:   return new Integer(security.getPermission());
        case TEXT:    return security.getMessage();
        case ALLOW:   return new Boolean(security.getAllowed());
      }
    } catch (Exception e) {}
    return null;
  }

  @Override
  public void setValueAt (int col, Object obj)  {
    switch (col)  {
    case GRANTEE: 
            security.setGrantee((AppDbObject)obj);              
            break;
    case CONTEXT:
            security.setContextObject((AppDbObject)obj);
            break;
    case PRIVS: 
            security.setPermission(((Integer)obj).intValue());
            break;
    case TEXT: 
            security.setMessage((String)obj);
            break;
    case ALLOW: 
            security.setAllowed(((Boolean)obj).booleanValue()); 
            break;
    }
  }
  
  


  @Override
  public TableCellRenderer getCellRenderer (int col)  {
    if (col == GRANTEE || col == CONTEXT) {   // grantee and context
      return new SecurityObjectCellRenderer();
    }
    if (col == PRIVS) {
      return new SecurityPermissionCellRenderer();
    }
    return null;
  }

  
  @Override
  public TableCellEditor getCellEditor(int col)  {
    if (col == GRANTEE) {
      return new SecurityGranteeCellEditor();
    }
    if (col == CONTEXT) {
      return new SecurityContextCellEditor();
    }
    if (col == PRIVS)  {
      // return RechteComboBox 
      return new SecurityPermissionCellEditor();
    }
    return null;    // default editor
  }



  
  // ---------------------- renderers ---------------------------------

  /**
   * special renderer for the object the rule applies to.
   * If the object is null, the text for "all" is displayed.
   */
  private class SecurityObjectCellRenderer extends FormTableCellRenderer {
    
    static final long serialVersionUID = 4163869751820657440L;

    public SecurityObjectCellRenderer()  {
      this.setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent (JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column)  {

      return super.getTableCellRendererComponent (table,
                  (value != null ? 
                    (((AppDbObject)value).getSingleName() + " " + value) : 
                    Locales.bundle.getString("all")),
                  isSelected, hasFocus, row, column);
    }
  }
  
  
  /**
   * Renderer for the permission.
   */
  private class SecurityPermissionCellRenderer extends FormTableCellRenderer {
    
    static final long serialVersionUID = 833322769619954772L;
    
    public SecurityPermissionCellRenderer()  {
      this.setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent (JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column)  {

      return super.getTableCellRendererComponent (table,
          (value instanceof Integer ? 
              Security.permissionToString(security.getPermissionType(), ((Integer)value).intValue()) : 
              "?"),
          isSelected, hasFocus, row, column);
    }
  }

  
  
  // ---------------------- editors ---------------------------------
  
  
  /**
   * Selects the grantee.
   * The default implementation invokes the AppDbSearchDialog
   * for the grantee class.
   * 
   * @return the grantee, null if cancelled or no granteeclazz set
   */
  public AppDbObject selectGrantee() {
    return granteeClazz == null ? null :
           new AppDbObjectSearchDialog (
                    security.getContextDb(),
                    granteeClazz, new Class[] { granteeClazz },
                    true, true).showDialog();
  }
  
  
  /**
   * Editor for the grantee.
   * Notice that the cell is only editable, if the granteeClazz is valid!
   */
  private class SecurityGranteeCellEditor extends FormComponentCellEditor {
    
    private FormButton button;
    private AppDbObject  rootObject;

    public SecurityGranteeCellEditor()  {
      button = new FormButton();
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            AppDbObject obj = selectGrantee();
            if (obj != null || 
                FormQuestion.yesNo(Locales.bundle.getString("free_for_all?"))) {
              rootObject = obj;
            }
          }
          catch (Exception ex)  {
            AppworxGlobal.logger.severe(ex.toString()); 
          }
          stopCellEditing();
        }
      });
    }

    @Override
    public Object getCellEditorValue() {
      return rootObject;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                boolean isSelected,
                                                int row, int column) {
      rootObject = (AppDbObject)value;
      button.setText (rootObject == null ? 
                        Locales.bundle.getString("all") : 
                        rootObject.toString());
      return button;
    }
  }
  
  
  
  private class SecurityContextCellEditor extends FormComponentCellEditor  {

    private FormButton      button;
    private AppDbObject     contextObject;

    @SuppressWarnings("unchecked")
    public SecurityContextCellEditor()  {
      button = new FormButton();
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // mit einem rootObject verbinden
          try {
            AppDbObject obj = new AppDbObjectSearchDialog (SecurityTableEntry.this.security.getContextDb(),
                                      contextClazz[0], contextClazz, true, true).showDialog();
            if (obj == null) {
              if (FormQuestion.yesNo(Locales.bundle.getString("free_for_all?"))) {
                contextObject = null;
              }
            }
            else  {
              contextObject = obj;
            }
          }
          catch (Exception ex)  {
             AppworxGlobal.logger.severe(ex.toString()); 
          }
          stopCellEditing();
        }
      });
    }

    public Object getCellEditorValue() {
      return contextObject;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                boolean isSelected,
                                                int row, int column) {
      contextObject = (AppDbObject)value;
      button.setText (contextObject == null ? 
                        Locales.bundle.getString("all") : 
                        contextObject.toString());
      return button;
    }
  }
  


  /**
   * Editor for permissions.
   */
  private class SecurityPermissionCellEditor extends FormComponentCellEditor {
    
    static final long serialVersionUID = 5240663889581202960L;

    private PermissionComboBox  box;
    private int                 permission;

    public SecurityPermissionCellEditor()  {
      super(new PermissionComboBox());
      box = (PermissionComboBox)getEditorComponent();
      box.addValueListener(new ValueListener()  {
        public void valueChanged(ValueEvent evt) {
          box.setPermissionType(security.getPermissionType());
          box.setPermission(permission);
        }
        public void valueEntered(ValueEvent evt) {
          permission = box.getPermission();
          stopCellEditing();
        }
      });
    }

    @Override
    public Object getCellEditorValue() {
      return new Integer(permission);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                boolean isSelected,
                                                int row, int column) {
      if (value instanceof Integer) {
        permission = ((Integer)value).intValue();
      }
      else  {
        permission = 0;
      }
      box.fireValueChanged();
      return box;
    }
  }
  


}

