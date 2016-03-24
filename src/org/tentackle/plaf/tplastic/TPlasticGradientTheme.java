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

// $Id: TPlasticGradientTheme.java 337 2008-05-09 18:35:27Z harald $

package org.tentackle.plaf.tplastic;

import com.incors.plaf.ColorUIResource2;
import com.incors.plaf.kunststoff.GradientTheme;
import javax.swing.plaf.ColorUIResource;



/**
 * Replaces KunststoffGradientTheme.
 * 
 * @author harald
 */
public class TPlasticGradientTheme implements GradientTheme {

  // gradient colors
  private final ColorUIResource componentGradientColorReflection = new ColorUIResource2(170, 197, 255, 88);
  private final ColorUIResource componentGradientColorShadow = new ColorUIResource2(0, 0, 0, 48);
  private final ColorUIResource textComponentGradientColorReflection = new ColorUIResource2(89, 122, 193, 32);
  private final ColorUIResource textComponentGradientColorShadow = null;

  private final int backgroundGradientShadow = 32;


  
  public String getName() {
    return "Default TPlastic Gradient Theme";
  }


  public ColorUIResource getComponentGradientColorReflection() {
    return componentGradientColorReflection;
  }

  public ColorUIResource getComponentGradientColorShadow() {
    return componentGradientColorShadow;
  }

  public ColorUIResource getTextComponentGradientColorReflection() {
    return textComponentGradientColorReflection;
  }

  public ColorUIResource getTextComponentGradientColorShadow() {
    return textComponentGradientColorShadow;
  }

  public int getBackgroundGradientShadow() {
    return backgroundGradientShadow;
  }

}