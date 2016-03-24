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

// $Id: FormTableUtilityPopup.java 481 2009-09-18 15:18:36Z svn $

// Created on January 31, 2003, 11:30 AM

package org.tentackle.ui;

import org.tentackle.print.FormTablePrintable;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.tentackle.util.BMoney;
import org.tentackle.util.StringHelper;
import org.tentackle.util.URLHelper;
import java.awt.Window;
import javax.swing.JRadioButtonMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;



/**
 * A utility popup menu to allow a FormTable to be printed, converted to excel, find an item
 * in the table, set/get the preferences, size, columns, etc...
 * The popup-Menu will be triggered by pressing the right-mouse button (using isPopupTrigger())
 * somewhere in the header of the table (by default). 
 *
 * @author harald
 */
public class FormTableUtilityPopup implements MouseListener, KeyListener {
  
  private FormTable table;                      // table
  private String title;                         // title of table (null = title from window)
  private String intro;                         // intro on first printed page (null = none)
  private boolean columnMenuEnabled = true;     // true if user is allowed to hide/show columns (default)
  
  // last search criteria
  private String searchText;
  private boolean caseSensitive;
  private int searchKey;            // search-Key (default is F3)
  
  private static final String EXCEL_EXTENSION = ".xls";
  private static final String XML_EXTENSION   = ".xml";
  private static final String LASTPREFIX = "/_lastExcelNames_";
  private static final String EXCELKEY   = "path";
  private static final String LASTXMLPREFIX = "/_lastXmlNames_";
  private static final String XMLKEY   = "path";
  
  
  /**
   * Creates a popup menu for a given table.
   * 
   * @param table the formtable
   * @param registerListeners true if register the mouse- and keyboard listeners
   * @param title the table title, null if default
   * @param intro the table intro, null if default
   */
  public FormTableUtilityPopup(FormTable table, boolean registerListeners, String title, String intro) {
    
    this.table = table;
    
    setTitle(title);
    setIntro(intro);
    
    if (registerListeners)  {
      addMouseListenerForTable();
      addKeyListener(); 
    }
  }
  
  /**
   * Creates a popup menu for a given table.<br>
   * Listeners will be registered.
   * 
   * @param table the formtable
   * @param title the table title, null if default
   * @param intro the table intro, null if default
   */
  public FormTableUtilityPopup(FormTable table, String title, String intro) {
    this(table, true, title, intro); 
  }
  
  /**
   * Creates a popup menu for a given table.<br>
   * Listeners will be registered, default title and table.
   * This is the standard constructor applications should use.
   * 
   * @param table the formtable
   */
  public FormTableUtilityPopup(FormTable table) {
    this(table, true, null, null);
  }
  
  
  /**
   * Uninstalls the popup from a table
   */
  public void uninstall() {
    if (table != null) {
      removeMouseListenerForTable();
      removeKeyListener();
    }
  }
 
  
  /**
   * Sets the title.<br>
   * The title will be printed pn each page and shown in excel sheets.
   * 
   * @param title the title, null if default from window 
   */
  public void setTitle(String title)  {
    this.title = title; 
  }
  
  /**
   * Gets the title.
   * @return the title, null if default from window 
   */
  public String getTitle()  {
    return title; 
  }
  
  /**
   * Sets the intro-text.<br>
   * The intro will be printed on the first page and shown in excel sheets.
   * 
   * @param intro the intro, null = none
   */
  public void setIntro(String intro)  {
    this.intro = intro; 
  }
  
  /**
   * Gets the intro text.
   * 
   * @return the intro, null = none
   */
  public String getIntro()  {
    return intro; 
  }


  /**
   * Creates the popup-menu.
   * @return the popup menu
   */
  public JPopupMenu createPopupMenu() {

    JPopupMenu menu = new JPopupMenu();

    TableColumnModel cm = table.getColumnModel();   // visible columns
    int numcol = cm.getColumnCount();

    boolean someRowSelected    = table.getSelectedRow() >= 0;
    boolean someColumnSelected = cm.getSelectionModel().getAnchorSelectionIndex() >= 0;

    if (columnMenuEnabled && cm instanceof FormTableColumnModel)  {
      // build columns-menu
      JMenu columnMenu = new JMenu();
      columnMenu.setText(Locales.bundle.getString("Spalten..."));
      TableModel dm = table.getModel();         // original columns
      boolean isAbstractFormTableModel = dm instanceof AbstractFormTableModel;
      int maxcol = dm.getColumnCount();
      JCheckBoxMenuItem[] items = new JCheckBoxMenuItem[maxcol];
      for (int i=0; i < maxcol; i++)  {
        items[i] = new JCheckBoxMenuItem(
                new ColumnAction(isAbstractFormTableModel ?
                                 ((AbstractFormTableModel)dm).getDisplayedColumnName(i) :
                                 dm.getColumnName(i), i));
        if (table.isColumnVisible(i)) {
          items[i].setSelected(true);
          items[i].setEnabled(numcol > 1);    // don't allow user to deselect last column
        }
        else  {
          items[i].setSelected(false);
        }
        columnMenu.add(items[i]);
      }
      // add select all button
      JRadioButtonMenuItem allItem = new JRadioButtonMenuItem(new AllColumnsAction(0, maxcol-1, true));
      allItem.setEnabled(numcol < maxcol);
      columnMenu.add(allItem);
      // add deselect all button
      allItem = new JRadioButtonMenuItem(new AllColumnsAction(0, maxcol-1, false));
      allItem.setEnabled(numcol > 1); // don't allow user to deselect last column
      columnMenu.add(allItem);

      menu.add(columnMenu);
    }


    JMenuItem searchItem = new JMenuItem();
    searchItem.setText(Locales.bundle.getString("suchen"));
    searchItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showSearchDialog();
      }
    });
    searchItem.setEnabled(someRowSelected && someColumnSelected);
    menu.add(searchItem);

    JMenuItem printItem = new JMenuItem();
    printItem.setText(Locales.bundle.getString("drucken"));
    printItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showPrintDialog();
      }
    });
    menu.add(printItem);

    JMenuItem excelItem = new JMenuItem();
    excelItem.setText(Locales.bundle.getString("export_Excel"));
    excelItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showExcelDialog(false);
      }
    });
    menu.add(excelItem);

    JMenuItem selectedExcelItem = new JMenuItem();
    selectedExcelItem.setText(Locales.bundle.getString("export_Excel_(nur_selektierte)"));
    selectedExcelItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showExcelDialog(true);
      }
    });
    selectedExcelItem.setEnabled(someRowSelected);
    menu.add(selectedExcelItem);

    JMenuItem xmlItem = new JMenuItem();
    xmlItem.setText(Locales.bundle.getString("export_XML"));
    xmlItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showXmlDialog(false);
      }
    });
    menu.add(xmlItem);

    JMenuItem selectedXmlItem = new JMenuItem();
    selectedXmlItem.setText(Locales.bundle.getString("export_XML_(nur_selektierte)"));
    selectedXmlItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)  {
        showXmlDialog(true);
      }
    });
    selectedXmlItem.setEnabled(someRowSelected);
    menu.add(selectedXmlItem);

    if (table.isCreateDefaultColumnsFromPreferences()) {

      if (FormHelper.useSystemPreferencesOnly) {
        if (!FormHelper.preferencesAreReadOnly)  {
          JMenuItem saveItem = new JMenuItem();
          saveItem.setText(Locales.bundle.getString("Systemeinstellungen_sichern"));
          saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)  {
              if (FormQuestion.yesNo(Locales.bundle.getString("Systemeinstellungen_für_diese_Tabelle_speichern?"))) {
                saveSettings(true);
              }
            }
          });
          menu.add(saveItem);
        }

        JMenuItem restoreItem = new JMenuItem();
        restoreItem.setText(Locales.bundle.getString("Systemeinstellungen_laden"));
        restoreItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)  {
            restoreSettings(true);
          }
        });
        menu.add(restoreItem);
      }
      else  {
        if (!FormHelper.preferencesAreReadOnly)  {
          JMenuItem saveItem = new JMenuItem();
          saveItem.setText(Locales.bundle.getString("Einstellungen_sichern"));
          saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)  {
              if (FormQuestion.yesNo(Locales.bundle.getString("Benutzereinstellungen_für_diese_Tabelle_speichern?"))) {
                saveSettings(false);
              }
            }
          });
          menu.add(saveItem);
        }

        JMenuItem restoreItem = new JMenuItem();
        restoreItem.setText(Locales.bundle.getString("Einstellungen_laden"));
        restoreItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)  {
            restoreSettings(false);
          }
        });
        menu.add(restoreItem);

        JMenuItem restoreSysItem = new JMenuItem();
        restoreSysItem.setText(Locales.bundle.getString("Systemeinstellungen_laden"));
        restoreSysItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)  {
            restoreSettings(true);
          }
        });
        menu.add(restoreSysItem);
      }

      JMenuItem defaultItem = new JMenuItem();
      defaultItem.setText(Locales.bundle.getString("Grundeinstellungen_laden"));
      defaultItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)  {
          FormTableUtilityPopup.this.table.createDefaultColumnsFromDefaultModel();
        }
      });
      menu.add(defaultItem);
    }

    return menu;
  }

  
  
  /**
   * Returns whether the column menu is enabled.
   * 
   * @return true if column menu is enabled
   */
  public boolean isColumnMenuEnabled() {
    return columnMenuEnabled;
  }

  /**
   * Enables or disables the column menu.<br>
   * The column menu allows to set the visibility of columns.
   * 
   * @param columnMenuEnabled  true to enable column menu (default)
   */
  public void setColumnMenuEnabled(boolean columnMenuEnabled) {
    this.columnMenuEnabled = columnMenuEnabled;
  }
  
  
  /**
   * Saves the table settings to the Preferences.
   * 
   * @param system is true if save to systemRoot, else userRoot
   */
  public void saveSettings(boolean system)  {
    try {
      table.savePreferences(
            table.getPreferencesName(((AbstractFormTableModel)table.getModel()).getTemplate()), system);
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Einstellungen_konnten_nicht_gesichert_werden"), ex); 
    }
  }
  
  
  /**
   * Restores the tables default settings from the Preferences.
   * 
   * @param system is true if load ONLY from systemRoot, else try userRoot first (should be default)
   */
  public void restoreSettings(boolean system) {
    try {
      if (table.createDefaultColumnsFromPreferences(
            table.getPreferencesName(((AbstractFormTableModel)table.getModel()).getTemplate()), system)
          == false) {
        if (FormQuestion.yesNo(Locales.bundle.getString("Keine_Einstellungen_gefunden._Voreinstellungen_laden?"))) {
          table.createDefaultColumnsFromDefaultModel(); 
        }
      }
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Einstellungen_konnten_nicht_gelesen_werden"), ex); 
    }
  }
  
  
  /**
   * Searches for a text starting at the current cell.
   *
   * @param searchText the search text
   * @param caseSensitive true if case sensitive
   * @return true if found
   */
  public boolean search(String searchText, boolean caseSensitive) {
    if (searchText != null && searchText.length() > 0)  {
      String text = caseSensitive ? searchText : searchText.toUpperCase();
      int currentRow = table.getSelectionModel().getAnchorSelectionIndex();
      // start after current field (+1)
      int currentCol = table.getColumnModel().getSelectionModel().getAnchorSelectionIndex() + 1;
      if (currentCol >= table.getColumnCount())  {
        // wrap to next line
        currentRow++;
        currentCol = 0;
      }
      TableModel model = table.getModel();
      int rows = model.getRowCount();
      int cols = model.getColumnCount();
      
      int startRow = currentRow;
      int endRow   = rows;
      int startCol = currentCol;
      
      for (int part=0; part < 2; part++)  {
        if (part == 1)  {
          startRow = 0;
          endRow   = currentRow;
        }
        for (int row=startRow; row < endRow; row++) {
          for (int col=startCol; col < cols; col++) {
            Object value = model.getValueAt(row, col);
            if (value != null)  {
              String cellText = value.toString();
              if (cellText != null) {
                if (!caseSensitive)  {
                  cellText = cellText.toUpperCase();
                }
                if (cellText.indexOf(text) >= 0) {
                  // found: set selection
                  table.setSelectedRow(row);
                  table.getColumnModel().getSelectionModel().setAnchorSelectionIndex(col);
                  table.scrollToCell(row, table.convertColumnIndexToView(col));
                  return true;
                }
              }
            }
          }
          startCol = 0;
        }
      }
    }
    return false;
  }
  
  
  /**
   * Opens a dialog to search in a table
   * starting at the current cell.
   */
  public void showSearchDialog() {
    SearchTextDialog sd = new SearchTextDialog();
    if (sd.showDialog(searchText, caseSensitive))  {
      // remember last parameters
      searchText = sd.getSearchText();
      caseSensitive = sd.isCaseSensitive();
      // run the search
      search(searchText, caseSensitive);
    }
  }
  
  
  
  /**
   * Prints the table.
   */
  public void showPrintDialog() {
    table.clearSelection();
    new FormTablePrintable(table, title, intro).doPrint();
  }
  
  
  /**
   * Converts the table to an excel spreadsheet.
   * @param file the output file
   * @param onlySelected true if export only selected rows
   * @throws IOException if export failed
   */
  public void excel(File file, boolean onlySelected) throws IOException {
           
    HSSFWorkbook wb = new HSSFWorkbook();
    HSSFSheet sheet = wb.createSheet();

    TableModel       model       = table.getModel();
    TableColumnModel columnModel = table.getColumnModel();
    
    int[] selectedRows = onlySelected ? table.getSelectedRows() : new int[] { };
    
    int rows = onlySelected ? selectedRows.length : model.getRowCount();  // number of data rows
    int cols = columnModel.getColumnCount();    // number of data columns
    
    short srow = 0;                             // current spreadsheet row
    
    // local copies cause might be changed
    String xTitle = this.title;
    String xIntro = this.intro;
    
    if (xTitle == null)  {
      // get default from window title
      Window parent = FormHelper.getParentWindow(table);
      try {
        // paint page-title
        xTitle = ((FormWindow)parent).getTitle();
      }
      catch (Exception e) {
        xTitle = null;
      }
    }
    if (xTitle != null)  {
      HSSFRow row = sheet.createRow(srow);
      HSSFFont font = wb.createFont();
      font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
      HSSFCellStyle cs = wb.createCellStyle();
      cs.setAlignment(HSSFCellStyle.ALIGN_CENTER);
      cs.setFont(font);
      HSSFCell cell = row.createCell(0);
      cell.setCellStyle(cs);
      cell.setCellValue(new HSSFRichTextString(xTitle));
      // region rowFrom, colFrom, rowTo, colTo
      sheet.addMergedRegion(new CellRangeAddress(0, srow, 0, cols - 1));
      srow++;
    }
    
    if (xIntro != null || onlySelected)  {
      HSSFRow row = sheet.createRow(srow);
      HSSFCell cell = row.createCell(0);
      HSSFCellStyle cs = wb.createCellStyle();
      cs.setAlignment(HSSFCellStyle.ALIGN_LEFT);
      cs.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
      cs.setWrapText(true);
      cell.setCellStyle(cs);
      if (onlySelected) {
        if (xIntro == null) {
          xIntro = "";
        }
        else {
          xIntro += ", ";
        }
        xIntro += Locales.bundle.getString("<nur_selektierte_Zeilen>");
      }
      cell.setCellValue(new HSSFRichTextString(xIntro));
      sheet.addMergedRegion(new CellRangeAddress(srow, srow + 2, 0, cols - 1));
      srow += 3;      
    }
    
    // column headers
    boolean isAbstractFormTableModel = model instanceof AbstractFormTableModel;
    srow++;   // always skip one line
    HSSFRow row = sheet.createRow(srow);
    HSSFFont font = wb.createFont();
    font.setItalic(true);
    font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
    HSSFCellStyle cs = wb.createCellStyle();
    cs.setAlignment(HSSFCellStyle.ALIGN_CENTER);
    cs.setFont(font);
    for (int c=0; c < cols; c++)  {
      HSSFCell cell = row.createCell(c);
      cell.setCellValue(new HSSFRichTextString(isAbstractFormTableModel ?
                         ((AbstractFormTableModel)model).getDisplayedColumnName(columnModel.getColumn(c).getModelIndex()) :
                         model.getColumnName(columnModel.getColumn(c).getModelIndex())));
      cell.setCellStyle(cs);
    }
    srow++;
    
    // default cell-style for date
    HSSFCellStyle dateStyle = wb.createCellStyle();
    dateStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy"));
    
    // cellstyles for numbers
    List<HSSFCellStyle> numberStyles = new ArrayList<HSSFCellStyle>();
    HSSFDataFormat format = wb.createDataFormat();
    
    
    for (int r=0; r < rows; r++)  {
      
      int modelRow = onlySelected ? selectedRows[r] : r;
      
      row = sheet.createRow(srow + (short)r);
      
      for (int i=0; i < cols; i++)  {
        
        int c = columnModel.getColumn(i).getModelIndex();
        
        Object value = model.getValueAt(modelRow, c);
        
        HSSFCell cell = row.createCell(i);
        
        if      (value instanceof Boolean)  {
          cell.setCellValue(((Boolean)value).booleanValue());
        }
        else if (value instanceof BMoney)   {
          BMoney money = (BMoney)value;
          cell.setCellValue(money.doubleValue());

          String fmt = "#,##0";
          if (money.scale() > 0) {
            fmt += ".";
            for (int j=0; j < money.scale(); j++)  {
              fmt += "0";
            }
          }
          // create format
          short fmtIndex = format.getFormat(fmt);
          
          // check if there is already a cellstyle with this scale
          Iterator<HSSFCellStyle> iter = numberStyles.iterator();
          boolean found = false;
          while (iter.hasNext())  {
            cs = iter.next();
            if (cs.getDataFormat() == fmtIndex) {
              // reuse that
              found = true;
              break;
            }
          }
          if (!found) {
            // create a new style
            cs = wb.createCellStyle();
            cs.setDataFormat(fmtIndex);
            numberStyles.add(cs);
          }
          cell.setCellStyle(cs);
        }
        else if (value instanceof Number)   {
          cell.setCellValue(((Number)value).doubleValue());
        }
        else if (value instanceof Date) {
          cell.setCellValue((Date)value);
          cell.setCellStyle(dateStyle);
        }
        else if (value instanceof GregorianCalendar) {
          cell.setCellValue((GregorianCalendar)value);
          cell.setCellStyle(dateStyle);
        }        
        else if (value != null) {
          cell.setCellValue(new HSSFRichTextString(value.toString()));
        }
      }
    }
    
    // set the width for each column
    for (int c=0; c < cols; c++)  {
      short width = (short)(columnModel.getColumn(c).getWidth() * 45); // is a reasonable value
      sheet.setColumnWidth(c, width);
    }

    // Write the output to a file
    FileOutputStream fileOut = new FileOutputStream(file);
    wb.write(fileOut);
    fileOut.close();
    
    // open Excel
    URLHelper.openURL(file.getPath());
  }
  
  
  
  
  /**
   * Opens a dialog to export a table to an excel sheet.
   * 
   * @param onlySelected true if export only selected rows
   */
  public void showExcelDialog(boolean onlySelected) {
    try {
      // Pfadname fuer diese Tabelle merken (immer in den userPrefs und immer abseits der normalen prefs)
      String prefName = LASTPREFIX + table.getPreferencesName(((AbstractFormTableModel)table.getModel()).getTemplate());
      Preferences prefs = Preferences.userRoot().node(prefName);
      String lastName = prefs.get(EXCELKEY, null);

      // Filename erfragen (default vom letzten Mal)
      JFileChooser jfc = new JFileChooser(lastName);
      if (lastName != null) {
        jfc.setSelectedFile(new File(lastName));
      }
      jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      jfc.setFileFilter(new FileFilter()  {
        public boolean accept(File f) {
          return f.getName().toLowerCase().endsWith(EXCEL_EXTENSION) || f.isDirectory();
        }
        public String getDescription()  {
          return Locales.bundle.getString("Excel-Datei");
        }
      });
      if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        File outFile = jfc.getSelectedFile();
        if (outFile.getName().toLowerCase().endsWith(EXCEL_EXTENSION) == false)  {
          outFile = new File(outFile.getPath() + EXCEL_EXTENSION);
        }
        excel(outFile, onlySelected);
        if (!FormHelper.preferencesAreReadOnly) {
          prefs.put(EXCELKEY, outFile.getAbsolutePath());
          prefs.flush();
        }
      }
    }
    catch (BackingStoreException ex)  {
      // no harm 
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("Excel-Datei_konnte_nicht_erzeugt_werden"), ex);
    }
  }  

  
  
  
  
  
  
  
  
  /**
   * Exports a table to an XML file.
   * 
   * @param file the output file
   * @param onlySelected true if export only selected rows
   * @throws IOException
   * @throws TransformerConfigurationException
   * @throws SAXException 
   */
  public void xml(File file, boolean onlySelected) throws IOException, TransformerConfigurationException, SAXException {
           
    TableModel       model       = table.getModel();
    TableColumnModel columnModel = table.getColumnModel();
    
    PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
    
    int[] selectedRows = onlySelected ? table.getSelectedRows() : new int[] { };
    
    int rows = onlySelected ? selectedRows.length : model.getRowCount();  // number of data rows
    int cols = columnModel.getColumnCount();                              // number of data columns
    
    StreamResult streamResult = new StreamResult(out);
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    // SAX2.0 ContentHandler.
    TransformerHandler hd = tf.newTransformerHandler();
    Transformer serializer = hd.getTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    hd.setResult(streamResult);
    hd.startDocument();
    AttributesImpl atts = new AttributesImpl();
    
    hd.startElement(StringHelper.emptyString, StringHelper.emptyString, "FormTable", atts);
    
    // local copies cause might be changed
    String xTitle = this.title;
    String xIntro = this.intro;
    
    if (xTitle == null)  {
      // get default from window title
      Window parent = FormHelper.getParentWindow(table);
      try {
        // paint page-title
        xTitle = ((FormWindow)parent).getTitle();
      }
      catch (Exception e) {
        xTitle = null;
      }
    }
    
    if (xTitle != null)  {
      hd.startElement(StringHelper.emptyString, StringHelper.emptyString, "title", atts);
      hd.characters(xTitle.toCharArray(), 0, xTitle.length());
      hd.endElement(StringHelper.emptyString, StringHelper.emptyString, "title");
    }
    
    if (xIntro != null || onlySelected)  {
      if (onlySelected) {
        if (xIntro == null) {
          xIntro = StringHelper.emptyString;
        }
        else {
          xIntro += ", ";
        }
        xIntro += Locales.bundle.getString("<nur_selektierte_Zeilen>");
      }
      hd.startElement(StringHelper.emptyString, StringHelper.emptyString, "intro", atts);
      hd.characters(xIntro.toCharArray(), 0, xIntro.length());
      hd.endElement(StringHelper.emptyString, StringHelper.emptyString, "intro");
    }
    
    boolean isAbstractFormTableModel = model instanceof AbstractFormTableModel;
    
    // create xml-tags
    String[] tags = new String[cols];
    if (rows > 0) {
      for (int i=0; i < cols; i++)  {
        int c = columnModel.getColumn(i).getModelIndex();
        tags[i] = StringHelper.toVarName(StringHelper.unDiacrit(
                      isAbstractFormTableModel ?
                        ((AbstractFormTableModel)model).getDisplayedColumnName(c) :
                        model.getColumnName(c)));
      }
    }
    
    for (int r=0; r < rows; r++)  {
      
      int modelRow = onlySelected ? selectedRows[r] : r;
      
      hd.startElement(StringHelper.emptyString, StringHelper.emptyString, table.getName(), atts);
      
      for (int i=0; i < cols; i++)  {
        int c = columnModel.getColumn(i).getModelIndex();
        String tag = tags[i];
        Object object = model.getValueAt(modelRow, c);
        String value = object == null ? StringHelper.emptyString : object.toString();
        hd.startElement(StringHelper.emptyString, StringHelper.emptyString, tag, atts);
        hd.characters(value.toCharArray(), 0, value.length());
        hd.endElement(StringHelper.emptyString, StringHelper.emptyString, tag);
      }
      
      hd.endElement(StringHelper.emptyString, StringHelper.emptyString, table.getName());
    }
    
    hd.endElement(StringHelper.emptyString, StringHelper.emptyString, "FormTable");
    hd.endDocument();

    out.close();
  }
  

  /**
   * Opens a dialog to export a table to an XML file.
   * 
   * @param onlySelected true if export only selected rows
   */
  public void showXmlDialog(boolean onlySelected) {
    try {
      // Pfadname fuer diese Tabelle merken (immer in den userPrefs und immer abseits der normalen prefs)
      String prefName = LASTXMLPREFIX + table.getPreferencesName(((AbstractFormTableModel)table.getModel()).getTemplate());
      Preferences prefs = Preferences.userRoot().node(prefName);
      String lastName = prefs.get(XMLKEY, null);

      // Filename erfragen (default vom letzten Mal)
      JFileChooser jfc = new JFileChooser(lastName);
      if (lastName != null) {
        jfc.setSelectedFile(new File(lastName));
      }
      jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      jfc.setFileFilter(new FileFilter()  {
        public boolean accept(File f) {
          return f.getName().toLowerCase().endsWith(XML_EXTENSION) || f.isDirectory();
        }
        public String getDescription()  {
          return Locales.bundle.getString("XML-Datei");
        }
      });
      if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        File outFile = jfc.getSelectedFile();
        if (outFile.getName().toLowerCase().endsWith(XML_EXTENSION) == false)  {
          outFile = new File(outFile.getPath() + XML_EXTENSION);
        }
        xml(outFile, onlySelected);
        if (!FormHelper.preferencesAreReadOnly) {
          prefs.put(XMLKEY, outFile.getAbsolutePath());
          prefs.flush();
        }
      }
    }
    catch (BackingStoreException ex)  {
      // no harm 
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("XML-Datei_konnte_nicht_erzeugt_werden"), ex);
    }
  }  
  
  
  
  
  
  
  
  
  
  
  /**
   * Adds a mouse listener only to the table header for this popup.
   * The listener will open the popup menu on a right click.
   * Used if there the table body needs another listener.
   */
  public void addMouseListenerForTableHeader() {
    removeMouseListenerForTableHeader();
    table.getTableHeader().addMouseListener(this);
  }

  
  /**
   * Removes the table header mouse listener for this popup.
   */
  public void removeMouseListenerForTableHeader()  {
    table.getTableHeader().removeMouseListener(this);
  }
  
  

  /**
   * Adds a mouse listener to the whole table (including the header) for this popup.
   * The listener will open the popup menu on a right click.
   */
  public void addMouseListenerForTable() {
    removeMouseListenerForTable();
    table.addMouseListener(this);
    JTableHeader header = table.getTableHeader();
    if (header != null) {
      header.addMouseListener(this);
    }
  }

  
  /**
   * Removes the table mouse listener for this popup.
   */
  public void removeMouseListenerForTable()  {
    table.removeMouseListener(this);
    JTableHeader header = table.getTableHeader();
    if (header != null) {
      header.removeMouseListener(this);
    }
  }


  
  public void mouseClicked(MouseEvent e) {
  }  

  public void mouseEntered(MouseEvent e) {
  }
  
  public void mouseExited(MouseEvent e) {
  }
  
  public void mousePressed(MouseEvent e) {
    processTableMouseEvent(e);
  }
  
  public void mouseReleased(MouseEvent e) {
    processTableMouseEvent(e);
  }
  
  // process the mouse event and show popup
  private void processTableMouseEvent(MouseEvent e)  {
    if (table != null && e.isPopupTrigger())  {
      createPopupMenu().show(table, e.getX(), e.getY());
    }    
  }
  
  
  /**
   * Adds a key listener for a given keycode.
   * 
   * @param keyCode the keycode
   */
  public void addKeyListenerForKey(int keyCode) {
    searchKey = keyCode;
    table.addKeyListener(this);
  }
  
  /**
   * Adds the default key listener for {@link KeyEvent#VK_F3}.
   * Pressing this key will start the search dialog or continue the search.
   */
  public void addKeyListener()  {
    addKeyListenerForKey(KeyEvent.VK_F3);
  }
  
  /**
   * Removes the key listener.
   */
  public void removeKeyListener()  {
    table.removeKeyListener(this);
  }
  
  
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == searchKey)  {
      if (searchText == null) {
        showSearchDialog();
      }
      else  {
        search(searchText, caseSensitive);
      }
    }
  }
  
  public void keyReleased(KeyEvent e) {
  }
  
  public void keyTyped(KeyEvent e) {
  }
  
  
  
  /**
   * to turn on/off columns in columnMenu
   */
  private class ColumnAction extends AbstractAction {
    
    /**
     * @param text is the column name
     * @param ndx is the column index (according to datamodel)
     */
    public ColumnAction(String text, int ndx)  {
      super(text);
      this.ndx = ndx;
    }
    
    public void actionPerformed(ActionEvent e) {
      table.setColumnVisible(ndx, !table.isColumnVisible(ndx)); // toggle
    }
    
    private int ndx;
  }
  
  
  /**
   * select all columns action
   */
  private class AllColumnsAction extends AbstractAction {
    
    private int first;    // first column index
    private int last;     // last column index
    private boolean show; // true = show, false = hide
    
    public AllColumnsAction(int first, int last, boolean show) {
      super(show ? Locales.bundle.getString("show_all") : Locales.bundle.getString("hide_all"));
      this.first = first;
      this.last  = last;
      this.show  = show;
    }
    
    public void actionPerformed(ActionEvent e) {
      if (show) {
        for (int ndx = first; ndx <= last; ndx++) {
          table.setColumnVisible(ndx, true);
        }
      }
      else  {
        for (int ndx = last; ndx >= first; ndx--) {
          // make it invisible, but only if it's not the last visible column at all.
          // otherwise we wont be able to invoke the menu anymore :-(
          if (table.getColumnModel().getColumnCount() > 1)  {
            table.setColumnVisible(ndx, false);
          }
        }
      }
    }
  }


  
}
