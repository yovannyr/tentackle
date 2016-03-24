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

// $Id: DefaultStringNormalizer.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

import java.util.Locale;

/**
 * The default normalizer (works sufficiently for most western languages).
 *
 * @author harald
 */
public class DefaultStringNormalizer implements StringConverter {
  

  /**
   * Normalizes a string (phonetically) for use as AppDbObject.normText.
   * 
   * @param str the string to be normalized
   * @return the normalized string
   */
   public String convert(String str)  {

    str = StringHelper.unDiacrit(str);     // convert special characters and umlauts
    
    if (str != null) {

      // convert to uppercase
      str = str.toUpperCase(Locale.ENGLISH);    // should work in all locales because of unDiacrit

      int len = str.length();
      StringBuilder sb = new StringBuilder (len);

      for (int i=0; i < len; i++)  {

        char c = str.charAt(i);

        // remove certain characters that are commonly used within names, addresses, numbers.
        if (c == '-' || c == ' ' || c == '.' || c == '\'' || c == '\"' ||
            c == '+' || c == '&') {
          continue;
        }

        // convert all whitespaces to the comma separator
        if (Character.isWhitespace(c))  {
          c = ',';
        }

        /**
         * convert certain character sequences.
         * This should work with all western languages sufficiently.
         */
        if (i + 2 < len)  {
          if (c == 'S' && str.charAt(i+1) == 'C' && str.charAt(i+2) == 'H') {
            // sch -> s
            i += 2;
          }
        }
        if (i + 1 < len)  {
          if (c == 'C' && str.charAt(i+1) == 'H') {
            // ch -> k
            c = 'K';
            i++;
          }
          else if (c == 'C' && str.charAt(i+1) == 'K')  {
            // ck -> k
            c = 'K';
            i++;
          }
          else if (c == 'P' && str.charAt(i+1) == 'H')  {
            // ph -> f
            c = 'F';
            i++;
          }
          else if (c == 'T' && str.charAt(i+1) == 'H')  {
            // th -> t
            c = 'T';
            i++;
          }
        }

        /**
         * Map certain characters phonetically.
         * This should work with all western languages sufficiently.
         */
        if      (c == 'D') {
          c = 'T';
        }
        else if (c == 'G') {
          c = 'K';
        }
        else if (c == 'C') {
          c = 'K';
        }
        else if (c == 'Y') {
          c = 'I';
        }
        else if (c == 'B') {
          c = 'P';
        }
        else if (c == 'Q') {
          c = 'K';
        }
        else if (c == 'W') {
          c = 'V';
        }
        else if (c == 'Z') {
          c = 'S';
        }
        
        sb.append(c);
      }

      
      // next round...
      
      len = sb.length();
      
      for (int i = 0; i < len; i++) {

        char c = sb.charAt(i);

        if (i + 1 < len)  {
          
          // solve the Mayer, Maier, Meier, Meyer problem
          if (c == 'A' && sb.charAt(i+1) == 'I')  {
            sb.setCharAt(i, 'E');    /* AI,AY -> EI */
          }
          
          // remove repeating characters (except for numbers!)
          if (Character.isLetter(c) && c == sb.charAt(i+1)) {
            sb.deleteCharAt(i+1);
            len--;
            continue;
          }
          
          // map IE to I
          if (c == 'I' && sb.charAt(i+1) == 'E')  {
            sb.deleteCharAt(i+1);
            len--;
            continue;
          }
        }

        // vowel + H + consonant --> remove the H
        if (i + 1 < len)  {
          char c2 = sb.charAt(i+1);
          char c3 = i + 2 < len ? sb.charAt(i+2) : ' ';
          if ((c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') &&
              c2 == 'H' &&
              (c3 != 'A' || c3 != 'E' || c3 != 'I' || c3 != 'O' || c3 != 'U')) {
            sb.deleteCharAt(i+1);
            len--;
          }
        }
      }

      return (sb.toString());
    }
    
    return null;
  }
  
  
}
