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

// $Id: Version.java 481 2009-09-18 15:18:36Z svn $

package org.tentackle.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Holds Tentackle versioning information.
 * 
 * @author harald
 */
public class Version {
  
  // the current SVN-revision is taken from an Ant property generated by the svnversion command
  public static final String SVNREVISION = /**/"481"/**/;    // @wurblet < Inject --string $svn_revision
  
  /** the build number / SVN-revision **/
  public static final String BUILD;
  /** the Open Source official release **/
  public static final String OSSRELEASE = "1.0.5";
  /** the public version **/
  public static final String VERSION;
  /** the year of the last checkin **/
  public static final String LAST_COPYRIGHT_YEAR;
  
  
  static {
    // We moved to a new svn-trunk after build 1388.
    // As a result, the real svn build is 1388 + SVNREVISION.
    int colonNdx = SVNREVISION.indexOf(':');    // take only latest revision
    String latestSvnRevision = colonNdx >= 0 ? SVNREVISION.substring(colonNdx + 1) : SVNREVISION;
    Pattern pattern = Pattern.compile("[0-9]*");
    Matcher matcher = pattern.matcher(latestSvnRevision);
    int revNo = matcher.find() ? Integer.valueOf(matcher.group()) : 0;
    BUILD = Integer.toString(1388 + revNo);
    LAST_COPYRIGHT_YEAR = "$Date: 2009-09-18 17:18:36 +0200 (Fri, 18 Sep 2009) $".substring(7,11);
    VERSION = "OSS " + OSSRELEASE + " (Build " + BUILD + ")";
  }
  
  
  /**
   * Prints the build version and license.
   * 
   * @param args command line args (ignored)
   */
  public static void main(String[] args) {
    System.out.println(
"\nTentackle - The Productivity Framework For Enterprise Desktop Java" +
"\nCopyright (C) 2001-" + LAST_COPYRIGHT_YEAR + " Harald Krake, harald@krake.de, +49 7722 9508-0" +
"\nhttp://www.tentackle.org" +
"\n\nVersion " + VERSION + "\n\n" +
"This library is free software; you can redistribute it and/or\n" +
"modify it under the terms of the GNU Lesser General Public\n" +
"License as published by the Free Software Foundation; either\n" +
"version 2.1 of the License, or (at your option) any later version.\n\n" +
"This library is distributed in the hope that it will be useful,\n" +
"but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU\n" +
"Lesser General Public License for more details.\n\n" +
"You should have received a copy of the GNU Lesser General Public\n" +
"License along with this library; if not, write to the Free Software\n" +
"Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA\n");
  }
  
}
