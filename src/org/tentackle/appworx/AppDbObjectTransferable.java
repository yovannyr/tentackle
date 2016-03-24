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

// $Id: AppDbObjectTransferable.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.appworx;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;



/**
 * Transferable for Drag and Drop of {@link AppDbObject}s or lists of.
 *
 * @author harald
 */
public class AppDbObjectTransferable implements Transferable, ClipboardOwner {

  // the first or single object
  private AppDbObject object;
  private DataFlavor  objectFlavor;
  
  // the list of objects
  private List<AppDbObject> objectList;
  private List<DataFlavor>  objectFlavorList;
  
  // supported flavours
  private DataFlavor[] flavors;

  /** flavor for a single {@link AppDbObject} **/
  public static final DataFlavor appDbObjectFlavor     = new DataFlavor(AppDbObject.class, "AppDbObject");
  /** flavor for a list of {@link AppDbObject}s **/
  public static final DataFlavor appDbObjectListFlavor = new DataFlavor(List.class, "AppDbObjectList");
  
  
  /**
   * Creates a transfertable for an object.
   * 
   * @param object the AppDbObject
   */
  public AppDbObjectTransferable(AppDbObject object) {
    
    this.object = object;
    
    objectList = new ArrayList<AppDbObject>();
    objectList.add(object);
    
    // the MIME-type of the transferable is derived from the class of the object
    // (which must be serializable which is checked by DataFlavor to allow cross-JVM
    // DnD. However, in fact we transfer only the Id, i.e. the class need not
    // necessarily be serializable.
    objectFlavor = new DataFlavor (object.getClass(), object.getClassBaseName());
    objectFlavorList = new ArrayList<DataFlavor>();
    objectFlavorList.add(objectFlavor);
    
    flavors = new DataFlavor[] { DataFlavor.stringFlavor, appDbObjectFlavor, appDbObjectListFlavor, objectFlavor };
  }
  

  /**
   * Creates a transferable for a collection of objects.
   * Non-AppDbObjects are skipped.
   * 
   * @param objects 
   */
  public AppDbObjectTransferable(Collection<?> objects) {
    
    objectList = new ArrayList<AppDbObject>();
    objectFlavorList = new ArrayList<DataFlavor>();
    
    if (objects != null && objects.size() > 0)  {
      Iterator iter = objects.iterator();
      boolean first = true;
      while (iter.hasNext())  {
        Object obj = iter.next();
        if (obj instanceof AppDbObject)  {
          objectList.add((AppDbObject)obj);
          DataFlavor flavor = new DataFlavor (obj.getClass(), ((AppDbObject)obj).getClassBaseName());
          objectFlavorList.add(flavor);
          if (first)  {
            this.object = (AppDbObject)obj;
            this.objectFlavor = flavor;
            first = false;
          }
        }
      }
      flavors = new DataFlavor[] { DataFlavor.stringFlavor, appDbObjectFlavor, appDbObjectListFlavor, objectFlavor };
    }
  }
  
  
  

  public DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  
  public boolean isDataFlavorSupported(DataFlavor flv) {
    for (int i=0; i < flavors.length; i++)  {
      if (flv.equals(flavors[i])) {
        return true;
      }
    }
    return false;
  }

  
  public Object getTransferData(DataFlavor flv)
         throws UnsupportedFlavorException {
    if (flv.equals(appDbObjectFlavor) ||
        flv.equals(objectFlavor)) {
      // construct the transfer data
      return new AppDbObjectTransferData(object);
    }
    else if (flv.equals(appDbObjectListFlavor))  {
      List<AppDbObjectTransferData> transferList = new ArrayList<AppDbObjectTransferData>();
      for (AppDbObject obj: objectList)  {
        transferList.add(new AppDbObjectTransferData(obj));
      }
      return transferList;
    }
    else if (flv.equals(DataFlavor.stringFlavor)) {
      return object.toString();
    }
    throw new UnsupportedFlavorException(flv);
  }
  
  
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    // nothing to do
  }
  
}
