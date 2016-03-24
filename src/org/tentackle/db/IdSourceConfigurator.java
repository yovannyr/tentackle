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

// $Id: IdSourceConfigurator.java 374 2008-07-25 12:58:09Z harald $


package org.tentackle.db;

import org.tentackle.util.ApplicationException;
import java.util.StringTokenizer;
import org.tentackle.util.Logger.Level;


/**
 * Configurator for an {@link IdSource}.
 * <p>
 * The configurator binds an IdSource to the application by means of an 
 * {@link IdSourceConnector}. An IdSource is uniquely identified by a namespace,
 * a name and an optional realm.
 * <p>
 * There are three types of connectors:
 * <ol>
 *  <li>
 *    Local connectors: for local ID sources, usually stored in a database.
 *    The format of the descriptor is:
 *    <pre>
 *    pool:{IdSourceConnector}:{space}:{name}[:realm]
 * 
 *    Example:
 *    pool:de.krake.poolkeeper.dbms.IdPool:plsbl:einlagNr
 *    </pre> 
 *  </li>
 *  <li>
 *    Remote connectors: for connecting to remote ID sources, usually provided by an
 *    application server.
 *    The format of the descriptor is:
 *    <pre>
 *    rmi:{IdSourceConnector}:{url}:{space}:{name}[:realm]
 * 
 *    Example:
 *    rmi://gonzo.krake.local:28000/PoolKeeper:scout:auftragNr:HAMBURG
 *    </pre> 
 *  </li>
 *  <li>
 *    The default ID source {@link ObjectId} is described as:
 *    <pre>
 *    default|objectid
 *    </pre>
 *  </li>
 * </ol> 
 * 
 * For an extensive implementation of this concept
 * please see the PoolKeeper project.
 *
 * @author harald
 */
public class IdSourceConfigurator {

  private String  idSourceConnector;  // classname of the connector (usually from PoolKeeper)
  private String  idSourceUrl;        // remote URL of IdSource if via IdPoolAgent, null = local pool or objectid
  private String  idSourceSpace;      // pool's namespace, null = via objectid
  private String  idSourceName;       // pool's name
  private String  idSourceRealm;      // pool's realm
  private int     idSourceType;       // connector type, one of SOURCE_... below
  
  /** unknown, i.e. not configured **/
  public static final int SOURCE_UNKNOWN  = 0;
  /** default mode, use ObjectId (if in db) or same as Db (if in DbObject) **/
  public static final int SOURCE_DEFAULT  = 1;
  /** rmi mode, i.e. remote source **/
  public static final int SOURCE_RMI      = 2;
  /** local mode, i.e. direct access to pool **/
  public static final int SOURCE_POOL     = 3;
  
  
  /**
   * Constructs a configurator from a descriptor.
   * 
   * @param descriptor the connection descriptor
   * @throws ApplicationException if parsing the descriptor failed
   */
  public IdSourceConfigurator(String descriptor) throws ApplicationException {
    parse(descriptor);
  }
  
  
  /**
   * Creates a default configurator for {@link ObjectId}.
   */
  public IdSourceConfigurator() {
    // default, nothing
  }
  
  
  /**
   * Parse a connection descriptor.
   *
   * @param descriptor the connection descriptor
   * 
   * @throws ApplicationException if descriptor malformed
   */
  public void parse(String descriptor)  throws ApplicationException {
    
    if (descriptor != null)  {

      idSourceType = SOURCE_UNKNOWN;

      StringTokenizer stok = new StringTokenizer(descriptor, ":");
      int i = 0;
      while (stok.hasMoreTokens())  {
        String token = stok.nextToken();
        if (i == 0) {
          if (token.equals("rmi")) {
            idSourceType = SOURCE_RMI;
          }
          else if (token.equals("pool"))  {
            idSourceType = SOURCE_POOL;
          }
          else if (token.equals("default") || token.equals("objectid"))  {
            idSourceType = SOURCE_DEFAULT;
          }
          else  {
            throw new ApplicationException("illegal idsource type: " + token);
          }
        }
        else if (i == 1)  {
          if (idSourceType == SOURCE_RMI || idSourceType == SOURCE_POOL)  {
            idSourceConnector = token;
          }
        }
        else if (i == 2)  {
          if (idSourceType == SOURCE_RMI)  {
            idSourceUrl = "rmi:" + token;
          }
          else  {
            idSourceSpace = token;
          }
        }
        else if (i == 3)  {
          if (idSourceType == SOURCE_RMI)  {
            idSourceSpace = token;
          }
          else  {
            idSourceName = token;
          }
        }
        else if (i == 4)  {
          if (idSourceType == SOURCE_RMI)  {
            idSourceName = token;
          }
          else  {
            idSourceRealm = token;
          }
        }
        else if (i == 5)  {
          idSourceRealm = token;
        }
        i++;
      }

      if (idSourceType == SOURCE_UNKNOWN) {
        throw new ApplicationException("idsource type missing");
      }

      if (idSourceType != SOURCE_DEFAULT && 
          (idSourceSpace == null || idSourceName == null ||
           (idSourceType == SOURCE_RMI && idSourceUrl == null))) {
        throw new ApplicationException("malformed idsource");
      }
    }
    else  {
      throw new ApplicationException("missing values");
    }    
  }
  
  
  
  /**
   * Connect to the ID-Source.
   *
   * @param db the db connection
   * @return the connected IdSource
   * @throws ApplicationException if connection failed
   */
  public IdSource connect(Db db) throws ApplicationException {
    
    if (DbGlobal.logger.isLoggable(Level.FINE)) {
      DbGlobal.logger.fine("connecting IdSource url=" + idSourceUrl + ", space=" + idSourceSpace +
                         ", name=" + idSourceName + ", realm=" + idSourceRealm);
    }
    
    if (idSourceType == SOURCE_DEFAULT)  {
      return new ObjectId(db);
    }
    
    // else: load the IdSourceConnector
    try {
      Class clazz = Class.forName(idSourceConnector);
      return ((IdSourceConnector)clazz.newInstance()).connect(
          db, idSourceUrl, idSourceSpace, idSourceName, idSourceRealm);
    }
    catch (Exception ex)  {
      throw new ApplicationException("connection to IdSource failed", ex);
    }
  }
  
  
  /**
   * Gets the loaded {@link IdSourceConnector}.
   * 
   * @return the connector.
   */
  public java.lang.String getIdSourceConnector() {
    return idSourceConnector;
  }
  
  
  /**
   * Gets the url if connector is remote.
   * 
   * @return the url, null if local
   */
  public String getIdSourceUrl() {
    return idSourceUrl;
  }
  
  
  /**
   * Gets the namespace of the id source.
   * 
   * @return the space
   */
  public String getIdSourceSpace() {
    return idSourceSpace;
  }  
                      
  
  /**
   * Gets the name of the id source.
   * 
   * @return Value of property idSourceName.
   */
  public String getIdSourceName() {
    return idSourceName;
  }
  

  /**
   * Gets the realm of the id source.
   * 
   * @return the realm
   */
  public String getIdSourceRealm() {
    return idSourceRealm;
  }
  
  
  /**
   * Gets the type of the id source.
   * One of SOURCE_...
   * 
   * @return the type
   */
  public int getIdSourceType() {
    return idSourceType;
  }
  
}
