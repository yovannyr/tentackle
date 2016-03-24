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

// $Id: DefaultObfuscator.java 447 2008-10-10 18:25:48Z harald $

package org.tentackle.util;

/**
 * A very simple string obfuscator.
 *
 * @author harald
 */
public class DefaultObfuscator {
  
  /**
   * Obfuscates a character array.<br>
   *
   * We return using this instead of md5sum whenever the length
   * of the obfuscated string must be the same as the input string.
   * Obfuscation is *NOT* the preferred method for passwords!
   * Use md5sum instead.
   *
   * @param input is the input array of chars to obfuscate
   * @param salt the salt to start with
   * @return the obfuscated array
   */
  public static char[] obfuscate(char[] input, char salt) {
    char[] output = null;
    if (input != null)  {
      output = new char[input.length];    // same length
      char c = salt;
      for (int i=0; i < input.length; i++)  {
        output[i] = (char)((input[i] ^ c + 1) & 0x7f);
        if      (output[i] <= ' ') {
          output[i] += ' ' + 1;
        }
        else if (output[i] > 'z') {
          output[i] -= ' ';
        }
        c = output[i];
      }
    }
    return output;
  }
  
  
  /**
   * Creates an obfuscating CharConverter.
   *
   * @param salt the salt to start with
   * @return the converter
   */
  public static CharConverter createObfuscator(final char salt) {
    return new CharConverter() {
      public char[] convert(char[] in) {
        return obfuscate(in, salt);
      }
    };
  }
  
  
  
  /**
   * Command line tool to obfuscate a string.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("usage: DefaultObfuscator <salt> <string>");
      System.exit(1);
    }
    System.out.println(createObfuscator(args[0].charAt(0)).convert(args[1].toCharArray()));
  }
    
}
