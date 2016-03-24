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

// $Id: DefaultQbfParameter.java 377 2008-07-28 08:40:47Z harald $


package org.tentackle.appworx;

import java.text.MessageFormat;
import org.tentackle.ui.FormError;
import org.tentackle.ui.FormInfo;
import org.tentackle.util.ApplicationException;

/**
 * The default qbf parameter with default settings suitable for most applications.
 *
 * @author harald
 */
public class DefaultQbfParameter extends QbfParameter {
  
  private static final long serialVersionUID = 8270811260647625463L;
  
  /** minimum normtext pattern length, 0 = default **/
  public int minPatternLength;
  
  
  /**
   * Creates a default qbf parameter.
   *
   * @param clazz the data object class
   * @param contextDb the database context
   */
  public DefaultQbfParameter(Class<? extends AppDbObject> clazz, ContextDb contextDb) {
    super(clazz, contextDb);
    // preset some defaults suitable for most applications
    warnRowCount        = 1000;
    warnSleep           = 500;
    maxRowCount         = 20000;
    fetchSize           = 500;
    searchInExtraThread = true;
  }
  
  
  @Override
  public boolean isEmpty()  {
    return getClass() == DefaultQbfParameter.class && (pattern == null || pattern.length() == 0);
  }
  
  
  @Override
  public boolean isValid() {
    try {
      // check permissions
      SecurityResult sr = contextDb.getAppUserInfo().getSecurityManager().privilege(clazz, contextDb, Security.READ);
      if (sr.isDenied())  {
        FormInfo.print(sr.explain(Locales.bundle.getString("You_don't_have_permission_to_view_this_kind_of_data!")));
        return false;
      }
    }
    catch (ApplicationException e)  {
      FormError.printException(e);
      return false;
    }
    
    if (minPatternLength > 0 && 
        (pattern == null || pattern.length() < minPatternLength))  {
      FormInfo.print(MessageFormat.format(Locales.bundle.getString("Please_enter_at_least_{0}_characters"), minPatternLength));
      return false;
    }
    return true;
  }
  
}
