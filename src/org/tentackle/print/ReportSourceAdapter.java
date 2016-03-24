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

// $Id: ReportSourceAdapter.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.print;

/**
 * Provides some default-implementations for a ReportSource Interface
 *
 * @author harald
 */
public class ReportSourceAdapter implements ReportSource {

  public void open() {
  }

  public void close() {
  }

  public void rewind() {
  }

  public void save()  {
  }

  public void restore() {
  }
  
  public boolean hasNext()  {
    return true;
  }

  public int advance(Report report) {
    return ReportSource.EOF;
  }

  public int prepareIntro(Report report) {
    return 0;
  }

  public int prepareTrailer(Report report) {
    return 0;
  }

  public int prepareHeader(Report report) {
    return 0;
  }

  public int prepareFooter(Report report) {
    return 0;
  }

  public int prepareLine(Report report) {
    return 0;
  }

  public int prepareSubHeader(Report report, int level) {
    return 0;
  }

  public int prepareSubFooter(Report report, int level) {
    return 0;
  }

}