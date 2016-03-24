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

// $Id: CopyDb.java 336 2008-05-09 14:40:20Z harald $
// Created on November 13, 2002, 7:33 PM

package org.tentackle.db.tools;

import org.tentackle.db.Db;
import org.tentackle.db.DbObject;
import org.tentackle.db.UserInfo;
import org.tentackle.util.ApplicationException;


/**
 * Console application to copy tables from the current db to another.
 * <pre>
 * usage: CopyDb &lt;table&gt; [&lt;user&gt; &lt;passwd&gt;]
 * </pre>
 * 
 * The app expects two property-files in the current classpath:
 * <ol>
 * <li>SourceDb.properties for the source-db</li>
 * <li>DestDb.properties for the destination-db</li>
 * </ol>
 * user and passwd args are optional if not given in the property files 
 * (as dbuser and dbpasswd).
 *
 * Example:
 * <pre>
 * org.tentackle.db.tools.CopyDb de.krake.jplsbl.dbms.Barren 
 * </pre>
 * 
 * @author harald
 */
public class CopyDb {
  
  /** transaction name for "copy db" **/
  public static final String TX_COPY_DB = "copy db";
  

  private Class clazz;                // DbObject class to copy
  private boolean plain;              // insertPlain/insert
  private Db sourceDb;                // db to copy from
  private Db destDb;                  // db to copy to
  private int txCount;                // current tx-count
  private boolean oldCommit;          // tx-committer
  
  
  
  public CopyDb(String args[]) throws Exception {
    
    plain = true;     // insertPlain is the default!
    String user = "";
    String passwd = "";
    
    for (String arg: args) {
      if (arg.startsWith("--")) {
        // option
        if (arg.equals("--noplain")) {
          plain = false;
        }
        else  {
          throw new Exception("unknown option: " + arg);
        }
      }
      else  {
        if (clazz == null) {
          clazz = Class.forName(arg);
        }
        else if (user == null) {
          user = arg;
        }
        else if (passwd == null) {
          passwd = arg;
        }
      }
    }
    
    UserInfo sourceUI = new UserInfo(user, passwd.toCharArray(), "SourceDb");
    sourceDb = new Db(sourceUI);
    if (sourceDb.open() == false) {
      throw new Exception("source connection failed");
    }
    sourceDb.setFetchSize(100);

    UserInfo destUI = new UserInfo(user, passwd.toCharArray(), "DestDb");
    destDb = new Db(destUI);
    if (destDb.open() == false) {
      throw new Exception("destination connection failed");
    }
  }
  
  
  
  /**
   * copy a singe table
   */
  private void run() throws ApplicationException {
    oldCommit = destDb.begin(TX_COPY_DB);
    txCount   = 0;
    if (sourceDb.isPostgres())  {
      sourceDb.begin(TX_COPY_DB);   // needed for fetchsize to work
    }
    try {
      DbObject obj = (DbObject)clazz.newInstance();
      obj.setDb(sourceDb);
      System.out.print("Copying " + obj.getClassName() + ": ");

      int count = obj.copyAllToDb(destDb, plain, new DbObject.CopyAllToDbLogger() {
        public void log(DbObject obj) {
          txCount++;
          if (txCount > 100)  {
            System.out.print(".");
            destDb.commit(oldCommit);  // don't make transactions too long
            oldCommit = destDb.begin(TX_COPY_DB);
            txCount = 0;
          }
        }
        public void logError(DbObject obj) {
          System.out.println("\n*** could not copy " + obj.getClassName() + ", ID=" + obj.getId() + " ***");
        }
      });
      
      destDb.commit(oldCommit);
      System.out.println("\n" + count + " copied\n");
    }
    catch (Exception e)  {
      destDb.rollback(oldCommit);
      throw new ApplicationException("Copying " + clazz.getName() + " failed", e);
    }
  }
  
  
  
  public static void main(String args[])  {
    
    if (args.length < 1)  {
      System.out.println("CopyDb [--noplain] <table> [<user> <passwd>]");
      System.exit(1);
    }

    try {
      new CopyDb(args).run();
      System.exit(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
}
