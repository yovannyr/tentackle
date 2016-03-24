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

// $Id: DefaultErrorHandler.java 389 2008-08-15 19:17:22Z harald $
// Created on May 21, 2003, 5:18 PM

package org.tentackle.db;

import org.tentackle.util.LoggerOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import org.tentackle.util.Logger.Level;
import org.tentackle.util.StringHelper;



/**
 * The default database error handler.<br>
 * 
 * @author harald
 */
public class DefaultErrorHandler implements ErrorHandler {
  
  public DefaultErrorHandler() {
    // something to do here??
  }
  
  public void severe(Db db, Exception e, String msg) {
    logMessage(db, e, msg, Level.SEVERE);
    throw new DbRuntimeException(msg, e);    // this will terminate the application if not explicitly caught!
  }
  
  public void severe(Exception e, String msg) {
    severe(null, e, msg); 
  }
  
  
  public void warning(Db db, Exception e, String msg) {
    logMessage(db, e, msg, Level.WARNING);
  }
  
  public void warning(Exception e, String msg) {
    warning(null, e, msg); 
  }
  

  public void info(Db db, Exception e, String msg) {
    logMessage(db, e, msg, Level.INFO);
  }
  
  public void info(Exception e, String msg) {
    info(null, e, msg);
  }  
  
  
  
  private void logMessage(Db db, Exception e, String msg, Level level)  {
    if (e == null)  {
      // avoid "Nullpointer Exception" cause it isn't one!
      e = new Exception("unknown (exception was null)");
    }
    PrintStream ps = new PrintStream(new LoggerOutputStream(DbGlobal.logger, level));
    ps.println(buildMessage(db, e, msg));
    e.printStackTrace(ps);
    ps.close();        
  }
  
  
  private String buildMessage(Db db, Exception e, String msg) {
    if (msg == null) {
      msg = StringHelper.emptyString;
    }
    if (db != null) {
      msg += "\n>>>DB>>>> " + db;
    }
    while (e != null)  {
      if (e instanceof SQLException)  {
        String state = ((SQLException)e).getSQLState();
        if (state != null && state.startsWith("08")) {
          // some severe comlink error, probably closed by server
          ManagedConnection mc = db.getConnection();
          if (mc != null) {
            // if connection still attached: mark it dead!
            mc.setDead(true);
            msg += " => DEAD";
          }
        }
        msg += "\n>>>SQL>>> " + e.getMessage() +
               "\n>>>Code>> " + ((SQLException)e).getErrorCode() +
               "\n>>>State> " + state;
        e = ((SQLException)e).getNextException();
      }
      else  {
        msg += "\n" + e.getMessage();
        Throwable t = e.getCause();
        if (t instanceof Exception) {
          e = (Exception)t;
        }
        else  {
          e = null;
        }
      }
    }
    return msg;
  }
  
}
