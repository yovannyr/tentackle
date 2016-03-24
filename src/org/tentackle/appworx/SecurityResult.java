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

// $Id: SecurityResult.java 381 2008-08-01 12:52:40Z harald $
// Created on November 27, 2002, 11:40 AM

package org.tentackle.appworx;

/**
 * Represents a result from the {@link SecurityManager} for a privilege request.
 *
 * @author harald
 */
public interface SecurityResult {
  
  /**
   * Denotes whether a privilege is accepted according to the
   * {@link Security} rules.
   * 
   * @return true if accepted
   */
  public boolean isAccepted();
  
  /**
   * Denotes whether a privilege is denied according to the
   * {@link Security} rules.
   * 
   * @return true if denied
   */
  public boolean isDenied();
  
  /**
   * Denotes whether a privilege is neither accepted nor according to the
   * {@link Security} rules, i.e. there are no rules that fired.
   * 
   * @return true if default
   */
  public boolean isDefault();
  
  
  /**
   * Formats a string for an explanation of rule that fired.
   * The default explanation will be returned unchanged if there is no message in the rule.
   * 
   * @param defaultMessage 
   * @return the formatted message or defaultMessage unchanged
   */
  public String explain(String defaultMessage);
  
  
  /**
   * Gets the {@link Security} rule that fired for this result.
   * 
   * @return the security rule, null if no rule fired
   */
  public Security getSecurity();
  
}
