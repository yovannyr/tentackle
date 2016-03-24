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

// $Id: URLHelper.java 466 2009-07-24 09:16:17Z svn $

package org.tentackle.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;


/**
 * Helper methods for desktop file and directory operations, web and mail.
 *
 * @author harald
 */
public class URLHelper {

  // initialization section
  private static final String URLHELPER_PROPERTIES = "URLHelper.properties";
  private static String     openUrlCommand;     // shell command open a url
  private static String     openFileCommand;    // shell command to edit/open a file
  private static String     browseCommand;      // shell command to browse a directory
  private static boolean    quoteFileNames;     // enclose file- and dirnames in double quotes


  /**
   * load config file (once)
   *
   * url: command to open a url (e.g. http://, mailto:....)
   * file: command to edit a file with respect to its filetype
   * browse: command to start the file-browser
   *
   */
  static {
    // load configuration settings
    InputStream in = URLHelper.class.getResourceAsStream (URLHELPER_PROPERTIES);
    if (in != null) {
      try {
        PropertyResourceBundle rs = new PropertyResourceBundle (in);
        Enumeration keys = rs.getKeys();
        while (keys.hasMoreElements())  {
          String key  = (String)keys.nextElement();
          String prop = rs.getString(key);
          if (key.compareTo("url") == 0) {
            openUrlCommand = prop;
          }
          else if (key.compareTo("file") == 0) {
            openFileCommand = prop;
          }
          else if (key.compareTo("browse") == 0)  {
            browseCommand = prop;
          }
          else if (key.compareTo("quote") == 0)  {
            quoteFileNames = true;
          }
        }
        in.close();
      } catch (IOException e) {
        // not found, leave them 'null'
        UtilGlobal.logger.warning("loading " + URLHELPER_PROPERTIES + " failed");
      }
    }
    else  {
      UtilGlobal.logger.warning("no config-file: " + URLHELPER_PROPERTIES);
    }
  }
  
  
  
  
  
  
  
  /**
   * Opens a URL according to its filetype.
   * 
   * @param urlDesc the url
   * @throws IOException 
   */
  public static void openURL (String urlDesc) throws IOException {
    String cmd = null;
    try {
      URL url = new URL(urlDesc);
      cmd = openUrlCommand + " " + url.toString();
    }
    catch (MalformedURLException ex)  {
      // it's a file
      File file = new File(urlDesc);
      if (file.isDirectory()) {
        browseDirectory(urlDesc); 
      }
      else  {
        if (quoteFileNames) {
          cmd = openFileCommand + " \"" + file.getPath() + "\"";
        }
        else  {
          cmd = openFileCommand + " " + file.getPath();
        }
      }
    }
    if (cmd != null) {
      if (UtilGlobal.logger.isFineLoggable()) {
        UtilGlobal.logger.fine(cmd);
      }
      Runtime.getRuntime().exec(cmd);
    }
  }
  
  
  


  /**
   * Browses a directory.
   * @param path the directory pathname
   * @param createDir true if create directory if missing
   * @throws IOException 
   */
  public static void browseDirectory (String path, boolean createDir) throws IOException {
    // normalize File
    File filePath = new File(path);
    if (filePath.exists() == false && createDir) {
      filePath.mkdirs();
    }
    String cmd;
    if (quoteFileNames) {
      cmd = browseCommand + " \"" + filePath.getPath() + "\"";
    }
    else  {
      cmd = browseCommand + " " + filePath.getPath();
    }
    if (UtilGlobal.logger.isFineLoggable()) {
      UtilGlobal.logger.fine(cmd);
    }
    Runtime.getRuntime().exec(cmd);
  }

  /**
   * Browses a directory.
   * @param path the directory pathname
   * @throws IOException 
   */
  public static void browseDirectory (String path) throws IOException {
    browseDirectory(path, false);
  }

  
  
  
  
  /**
   * Sends a mail (and start eMail-Frontend).
   * 
   * @param recipient the recipoent
   * @param subject the subject, null if none
   * @param body the body, null if none
   * @throws IOException
   * @throws URISyntaxException 
   */
  public static void mailTo (String recipient, String subject, String body) throws IOException, URISyntaxException {
    if (recipient != null)  {
      int atNdx = recipient.indexOf('@');
      if (atNdx > 0)  {
        String user = recipient.substring(0, atNdx);
        String host = recipient.substring(atNdx + 1);
        String query = null;
        if (subject != null)  {
          query = "subject=" + subject; 
        }
        if (body != null) {
          if (query == null) {
            query = "";
          }
          else {
            query += "&";
          }
          query += "body=" + body;
        }
        URI uri = new URI("mailto", user, host, -1, null, query, null);
        // cut off "//" after mailto
        String urlDesc = uri.toASCIIString();
        urlDesc = urlDesc.substring(0, 7) + urlDesc.substring(9);
        openURL(urlDesc);
      }
    }
  }




}