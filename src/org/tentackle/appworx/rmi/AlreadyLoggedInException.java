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

// $Id: AlreadyLoggedInException.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.appworx.rmi;

import java.text.MessageFormat;
import java.util.Date;
import org.tentackle.appworx.AppUserInfo;
import org.tentackle.appworx.Locales;
import org.tentackle.db.rmi.LoginFailedException;
import org.tentackle.util.StringHelper;


/**
 * Exception thrown by an application server if the user is already logged in.<br>
 * Only for appservers that don't allow a user logged in more than once.
 * 
 * @author harald
 */
public class AlreadyLoggedInException extends LoginFailedException {
  
  private AppUserInfo loginInfo;    // the application user info
  
  /**
   * Creates an {@code AlreadyLoggedInException}.
   * 
   * @param loginInfo the application user info
   */
  public AlreadyLoggedInException(AppUserInfo loginInfo) {
    super(StringHelper.emptyString);
    this.loginInfo = loginInfo;
  }
  
  /**
   * Gets the application user info.
   * 
   * @return the user info
   */
  public AppUserInfo getLoginInfo() {
    return loginInfo;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * Overridden due to localized message.
   */
  @Override
  public String getMessage() {
    return MessageFormat.format(Locales.bundle.getString("You're_already_logged_into_{0}_since_{1}"),
                                loginInfo.getApplication(), StringHelper.shortTimestampFormat.format(new Date(loginInfo.getSince())));
  }
}
