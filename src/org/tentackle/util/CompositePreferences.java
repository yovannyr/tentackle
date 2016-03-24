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

// $Id: CompositePreferences.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.tentackle.ui.FormError;
import org.tentackle.ui.WorkerThread;



/**
 * User- and System-Preferences combined.<br>
 * 
 * Implements Preferences in such a way that
 * user-space overwrites system-space which is the default
 * for most applications.
 * Furthermore, some helper methods allow exporting and importing
 * with file-dialogs, etc...
 * <p>
 * This class is abstract because it should be extended by
 * the application to get its own namespace (by classname).
 *
 * @author harald
 */
public abstract class CompositePreferences {
  
  
  private static final String FILE_EXTENSION    = ".xml";
  private static final String FILE_DESCRIPTION  = "Preferences";
  
  
  /**
   * Exports the preferences (user or system) to XML.
   *
   * @param pathname the filname
   * @param system true if system-prefs, else userprefs
   * @throws BackingStoreException
   * @throws FileNotFoundException
   * @throws IOException 
   */
  public static void exportPreferences(String pathname, boolean system) 
         throws BackingStoreException, FileNotFoundException, IOException {
    
    Preferences prefs = system ? Preferences.systemRoot() : Preferences.userRoot();
    prefs.sync();
    prefs.exportSubtree(new FileOutputStream(pathname));
  }
  
  
  /**
   * Exports the preferences (user or system) to XML
   * and prompts the user in a dialog for the file.
   * 
   * @param system true if system-prefs, else userprefs
   */
  public static void exportPreferences(final boolean system)  {
    JFileChooser jfc = new JFileChooser();
    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    jfc.setFileFilter(new FileFilter()  {
      public boolean accept(File f) {
        return f.getName().toLowerCase().endsWith(FILE_EXTENSION) || f.isDirectory();
      }
      public String getDescription()  {
        return FILE_DESCRIPTION;
      }
    });
    if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
      File outFile = jfc.getSelectedFile();
      if (outFile.getName().toLowerCase().endsWith(FILE_EXTENSION) == false)  {
        outFile = new File(outFile.getPath() + FILE_EXTENSION);
      }
      final String pathname = outFile.getPath();
      new WorkerThread(new Runnable() {
        public void run() {
          try {
            exportPreferences(pathname, system);
          }
          catch (Exception ex)  {
            FormError.printException(Locales.bundle.getString("exporting_preferences_failed"), ex);
          }
        }
      }, Locales.bundle.getString("exporting_preferences...")).start();
    }      
  }
  
  
  
  /**
   * Imports the preferences (user or system) from XML.
   *
   * @param pathname is the filname
   * @param system true if system-prefs, else userprefs
   * @throws FileNotFoundException
   * @throws IOException
   * @throws InvalidPreferencesFormatException
   * @throws BackingStoreException 
   */
  public static void importPreferences(String pathname, boolean system) 
         throws FileNotFoundException, IOException, InvalidPreferencesFormatException, BackingStoreException {
    /**
     * Hack: because importPreferences() is static, Preferences-implementations that do not extend
     * AbstractPreferences (as DbPreferences do!) will fail. Therefore, we explicitly check for the
     * a static method by reflection. Sun better should move this method to the PreferencesFactory!
     */
    try {
      Class<?> clazz = (system ? Preferences.systemRoot() : Preferences.userRoot()).getClass();
      Method method = clazz.getDeclaredMethod("importPreferences", InputStream.class);
      method.invoke(null, new BufferedInputStream(new FileInputStream(pathname)));
    }
    catch (Exception ex)  {
      FormError.printException(Locales.bundle.getString("importing_preferences_failed"), ex);
    }
    
    // write back to persistant storage
    if (system) {
      Preferences.systemRoot().flush();
    }
    else  {
      Preferences.userRoot().flush();
    }
  }
    
  
  /**
   * Imports the preferences (user or system) from XML and
   * prompts the user for a file.
   *
   * @param system true if system-prefs, else userprefs
   */
  public static void importPreferences(final boolean system)  {
    JFileChooser jfc = new JFileChooser();
    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    jfc.setFileFilter(new FileFilter()  {
      public boolean accept(File f) {
        return f.getName().toLowerCase().endsWith(FILE_EXTENSION) || f.isDirectory();
      }
      public String getDescription()  {
        return FILE_DESCRIPTION;
      }
    });
    if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      final File inFile = jfc.getSelectedFile();
      new WorkerThread(new Runnable() {
        public void run() {
          try {
            importPreferences(inFile.getPath(), system);
          }
          catch (Exception ex)  {
            FormError.printException(Locales.bundle.getString("importing_preferences_failed"), ex);
          }
        }
      }, Locales.bundle.getString("importing_preferences...")).start();
    }      
  }

  
  
  
  
  
  
  
  
  private Preferences userPrefs;        // preferences for current user
  private Preferences systemPrefs;      // system default prefs
  
  
  
  /**
   * Creates composite preferences.
   * 
   * @param systemOnly true if refer to system-preferences only, false = user space overwrites system space
   */
  
  public CompositePreferences(boolean systemOnly) {
    systemPrefs = Preferences.systemNodeForPackage(this.getClass());
    if (!systemOnly)  {
      userPrefs = Preferences.userNodeForPackage(this.getClass());
    }
  }
  
  
  
  /**
   * Gets the user peferences.
   * 
   * @return the user-scope preferences, null if systemOnly
   */
  public Preferences getUserPrefs()  {
    return userPrefs;
  }
  
  /**
   * Gets the system preferences.
   * 
   * @return the system-scope preferences
   */
  public Preferences getSystemPrefs()  {
    return systemPrefs;
  }
  
  
  
  /**
   * Flushes the preferences (system and user).
   * 
   * @throws BackingStoreException 
   */
  public void flush() throws BackingStoreException {
    if (userPrefs != null) {
      userPrefs.flush();
    }
    systemPrefs.flush();
  }
  
  
  /**
   * Syncs the preferences (system and user).
   * 
   * @throws BackingStoreException 
   */
  public void sync() throws BackingStoreException {
    if (userPrefs != null) {
      userPrefs.sync();
    }
    systemPrefs.sync();
  }
  


  /**
   * Gets the value for a key.
   * Userspace overwrites systemspace.
   * 
   * @param key the preferences key
   * @return the value string or null if no such value
   */
  public String getString(String key) {
    String val = null;
    if (userPrefs != null) {
      val = userPrefs.get(key, null);
    }
    if (val == null) {
      val = systemPrefs.get(key, null);
    }
    return val;
  }
  
  
  /**
   * Gets the value for a key.
   * Userspace overwrites systemspace.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the value string
   */
  public String getString(String key, String def) {
    String val = getString(key);
    return val == null ? def : val;
  }
  
  

  /**
   * Sets the value for a key.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setString(String key, String val) {
    if (userPrefs != null)  {
      if (val == null || Compare.equals(val, systemPrefs.get(key, null))) {
        userPrefs.remove(key);
      }
      else  {
        userPrefs.put(key, val);
      }
    }
    else  {
      if (val == null)  {
        systemPrefs.remove(key);
      }
      else  {
        systemPrefs.put(key, val);
      }
    }
  }

  
  
  /**
   * Gets an Integer.
   * 
   * @param key the preferences key
   * @return the integer or null if no such value
   */
  public Integer getInteger(String key) {
    String val = getString(key);
    return val == null ? null : new Integer(val);
  }
  
  /**
   * Gets an int value.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the integer
   */
  public int getInt(String key, int def) {
    Integer val = getInteger(key);
    return val == null ? def : val;
  }

  /**
   * Sets an integer.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setInteger(String key, Integer val) {
    setString(key, val == null ? null : val.toString());
  }

  
  /**
   * Gets a Long.
   * 
   * @param key the preferences key
   * @return the integer or null if no such value
   */
  public Long getALong(String key) {
    String val = getString(key);
    return val == null ? null : new Long(val);
  }
  
  /**
   * Gets a long value.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the integer
   */
  public long getLong(String key, long def) {
    Long val = getALong(key);
    return val == null ? def : val;
  }

  /**
   * Sets a Long.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setLong(String key, Long val) {
    setString(key, val == null ? null : val.toString());
  }


  
  /**
   * Gets a Float.
   * 
   * @param key the preferences key
   * @return the integer or null if no such value
   */
  public Float getAFloat(String key) {
    String val = getString(key);
    return val == null ? null : new Float(val);
  }
  
  /**
   * Gets a float value.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the integer
   */
  public float getFloat(String key, float def) {
    Float val = getAFloat(key);
    return val == null ? def : val;
  }

  /**
   * Sets a Float.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setFloat(String key, Float val) {
    setString(key, val == null ? null : val.toString());
  }
  
  
  
  /**
   * Gets a Double.
   * 
   * @param key the preferences key
   * @return the integer or null if no such value
   */
  public Double getADouble(String key) {
    String val = getString(key);
    return val == null ? null : new Double(val);
  }
  
  /**
   * Gets a double value.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the integer
   */
  public double getDouble(String key, double def) {
    Double val = getADouble(key);
    return val == null ? def : val;
  }

  /**
   * Sets a Double.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setDouble(String key, Double val) {
    setString(key, val == null ? null : val.toString());
  }


  
  /**
   * Gets a Boolean.
   * 
   * @param key the preferences key
   * @return the integer or null if no such value
   */
  public Boolean getABoolean(String key) {
    String val = getString(key);
    return val == null ? null : new Boolean(val);
  }
  
  /**
   * Gets a boolean value.
   * 
   * @param key the preferences key
   * @param def the default value
   * @return the integer
   */
  public boolean getBoolean(String key, boolean def) {
    Boolean val = getABoolean(key);
    return val == null ? def : val;
  }

  /**
   * Sets a Boolean.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the value
   */
  public void setBoolean(String key, Boolean val) {
    setString(key, val == null ? null : val.toString());
  }

  
  
  
  
  /**
   * Gets a color value from preferences.
   * 
   * @param key the preferences key
   * @return the color or null if no such value
   */
  public Color getColor(String key) {
    String rgbText = getString(key);
    if (rgbText != null)  {
      StringTokenizer tk = new StringTokenizer(rgbText);
      int red   = 0;
      int green = 0;
      int blue  = 0;
      int i=0;
      while (tk.hasMoreTokens())  {
        int val = Integer.parseInt(tk.nextToken());
        switch (i)  {
          case 0: red = val;    break;
          case 1: green = val;  break;
          case 2: blue = val;   break;
        }
        ++i;
      }
      return new Color(red, green, blue);
    }
    return Color.GRAY;
  }
  
  /**
   * Gets a color value from preferences.
   * 
   * @param key the preferences key
   * @param def the default color
   * @return the color
   */
  public Color getColor(String key, Color def) {
    Color val = getColor(key);
    return val == null ? def : val;
  }
  
  
  /**
   * Sets a preferences color.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * Colors are stored as three integers {@code "red green blue"}.
   * 
   * @param key the preferences key
   * @param color the color
   */
  public void setColor(String key, Color color) {
    setString(key, color == null ? 
          null :
          (color.getRed() + " " + color.getGreen() + " " + color.getBlue()));
  }
  
  
  
  /**
   * Gets a bytearray from the preferences.
   * 
   * @param key the preferences key
   * @return the byte array or null if no such value
   */
  public byte[] getByteArray(String key) {
    byte[] val = null;
    if (userPrefs != null) {
      val = userPrefs.getByteArray(key, null);
    }
    if (val == null) {
      val = systemPrefs.getByteArray(key, null);
    }
    return val;
  }
  
  /**
   * Gets a bytearray from the preferences.
   * 
   * @param key the preferences key
   * @param def the default values
   * @return the byte array
   */
  public byte[] getByteArray(String key, byte[] def) {
    byte[] val = getByteArray(key);
    return val == null ? def : val;
  }

  
  /**
   * Sets a bytearray.<br>
   * If the value is the same as in systemspace, the user value is removed.
   * Null-values also perform a remove.
   * 
   * @param key the preferences key
   * @param val the values
   */
  public void setByteArray(String key, byte[] val) {
    if (userPrefs != null)  {
      if (val == null || areByteArraysEqual(val, systemPrefs.getByteArray(key, null))) {
        userPrefs.remove(key);
      }
      else  {
        userPrefs.putByteArray(key, val);
      }
    }
    else  {
      if (val == null)  {
        systemPrefs.remove(key);
      }
      else  {
        systemPrefs.putByteArray(key, val);
      }
    }
  }
  
  
  /**
   * Checks whether two byte arrays are equal.
   */
  private boolean areByteArraysEqual(byte[] b1, byte[] b2)  {
    if (b1 == null) {
      return b2 == null;
    }
    else if (b2 == null) {
      return b1 == null;
    }
    else  {
      if (b1.length == b2.length) {
        for (int i=0; i < b1.length; i++) {
          if (b1[i] != b2[i]) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }
  
}
