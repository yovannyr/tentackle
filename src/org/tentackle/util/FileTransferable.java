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

// $Id: FileTransferable.java 336 2008-05-09 14:40:20Z harald $
// Created on August 22, 2002, 7:41 PM

package org.tentackle.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Vector;
import java.io.File;

/**
 * A transferable for files.<br>
 * Returned to the drop target (if we are the drag source)
 * and if the dragged object is a document (either File or Directory).
 */
public class FileTransferable extends Vector<File> implements Transferable  {
  
  private static final long serialVersionUID = 316070171066467593L;

  private final static int FILE   = 0;
  private final static int STRING = 1;

  private DataFlavor flavors[] = {DataFlavor.javaFileListFlavor,
                                  DataFlavor.stringFlavor};

                                  
  /**
   * Creates a FileTransferable.
   * 
   * @param file the file
   */
  public FileTransferable(File file) {
    addElement(file);
  }

  public synchronized DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    boolean b  = false;
    b |= flavor.equals(flavors[FILE]);
    b |= flavor.equals(flavors[STRING]);
    return (b);
  }

  public synchronized Object getTransferData(DataFlavor flavor) 
         throws UnsupportedFlavorException, java.io.IOException {
    if (flavor.equals(flavors[FILE])) {
      return this;
    }
    else if (flavor.equals(flavors[STRING])) {
      return elementAt(0).getAbsolutePath();
    }
    else {
      throw new UnsupportedFlavorException(flavor);
    }
  }
}
