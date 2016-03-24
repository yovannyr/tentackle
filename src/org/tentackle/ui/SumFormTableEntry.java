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

// $Id: SumFormTableEntry.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import org.tentackle.util.BMoney;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A formtable entry to sumup the numeric columns of a table.
 * 
 * @author harald
 */
public class SumFormTableEntry extends FormTableEntry {
  
  private static final int UNKNOWN = 0;   // unknown column
  private static final int NAN     = 1;   // not a number
  private static final int DOUBLE  = 2;   // double or float
  private static final int LONG    = 3;   // byte,short,integer or long
  private static final int BIGDEC  = 4;   // BigDecimal
  private static final int BIGINT  = 5;   // BigInteger
  private static final int BMONEY  = 6;   // BMoney
  
  private int[] types;            // column types
  private Class[] classes;        // real classes
  private Object[] sums;          // final sums
  
  private double[] doubles;       // for fast summing up
  private long[] longs;
  private BigDecimal[] bigdecs;
  private BigInteger[] bigints;
  private BMoney[] bmoneys;
  
  
  
  /**
   * Creates a SumFormTableEntry for a given table model.
   * 
   * @param model the form table model
   */
  public SumFormTableEntry(AbstractFormTableModel model) {
    if (model != null)  {
      sumUp(model);
    }
  }
  
  /**
   * Creates an empty SumFormTableEntry.
   * @see #sumUp(org.tentackle.ui.AbstractFormTableModel) 
   */
  public SumFormTableEntry()  {
  }

  
  /**
   * Sums up all numeric column in the model.
   * @param model the form table model
   */
  public void sumUp(AbstractFormTableModel model) {
    FormTableEntry template = model.getTemplate();
    int cols = template.getColumnCount();
    types   = new int[cols];
    classes = new Class[cols];
    sums    = new Object[cols];
    doubles = new double[cols];
    longs   = new long[cols];
    bigdecs = new BigDecimal[cols];
    bigints = new BigInteger[cols];
    bmoneys = new BMoney[cols];
    
    FormTable table = model.getTable();
    // initialize to "unknown", to "ignore" if column is not visible
    for (int col=0; col < cols; col++) {
      types[col] = (table != null && table.isColumnVisible(col) == false) || template.isColumnNotSummable(col) ? NAN : UNKNOWN;   
    }
    
    int rows = model.getRowCount();
    for (int row=0; row < rows; row++)  {
      FormTableEntry entry = model.getEntryAt(row);
      if (entry != null)  { // if not a sum entry
        for (int col=0; col < cols; col++) {
          int type = types[col];
          if (type != NAN)  {
            Object value = entry.getValueAt(col);
            if (value != null)  {
              try {
                switch (type) {
                  case UNKNOWN: // type is unknown yet
                    Class clazz = value.getClass();   // we use real classes, no extensions!
                    if (clazz == BMoney.class)  {
                      types[col]   = BMONEY;
                      classes[col] = clazz;
                      bmoneys[col] = (BMoney)value;
                    }
                    else if (clazz == BigDecimal.class) {
                      types[col]   = BIGDEC;
                      classes[col] = clazz;
                      bigdecs[col] = (BigDecimal)value;
                    }
                    else if (clazz == BigInteger.class) {
                      types[col]   = BIGINT;
                      classes[col] = clazz;
                      bigints[col] = (BigInteger)value;
                    }
                    else if (clazz == Double.class || clazz == Float.class) {
                      types[col]   = DOUBLE;
                      classes[col] = clazz;
                      doubles[col] = ((Number)value).doubleValue();
                    }
                    else if (value instanceof Number) {
                      types[col]   = LONG;
                      classes[col] = clazz;
                      longs[col]   = ((Number)value).longValue();                    
                    }
                    else  {
                      types[col] = NAN;   // not a number
                    }
                    break;

                  case BMONEY:
                    bmoneys[col] = bmoneys[col].add((BMoney)value);
                    break;

                  case BIGDEC:
                    bigdecs[col] = bigdecs[col].add((BigDecimal)value);
                    break;

                  case BIGINT:
                    bigints[col] = bigints[col].add((BigInteger)value);
                    break;

                  case DOUBLE:
                    doubles[col] += ((Number)value).doubleValue();
                    break;

                  case LONG:
                    longs[col] += ((Number)value).longValue();
                    break;
                }
              }
              catch (Exception e) {
                // any exception (usually classcast) -> ignore
              }
            }
          }
        }
      }
    }
    
    // everything summed up: build result
    for (int col=0; col < cols; col++)  {
      switch(types[col])  {
        case BMONEY:
          sums[col] = bmoneys[col];
          break;

        case BIGDEC:
          sums[col] = bigdecs[col];
          break;

        case BIGINT:
          sums[col] = bigints[col];
          break;

        case DOUBLE:
          if (classes[col] == Double.class) {
            sums[col] = new Double(doubles[col]);
          }
          else  {
            sums[col] = new Float((float)doubles[col]);
          }
          break;

        case LONG:
          if (classes[col] == Byte.class) {
            sums[col] = new Byte((byte)longs[col]);
          }
          else if (classes[col] == Short.class) {
            sums[col] = new Short((short)longs[col]);
          }
          else if (classes[col] == Integer.class) {
            sums[col] = new Integer((int)longs[col]);
          } 
          else {
            sums[col] = new Long(longs[col]);
          } 
          break;
          
        default:
          sums[col] = null;
      }
    }
  }
  
  
  
  public FormTableEntry newInstanceOf(Object object) {
    return null;      // never invoked
  }

  public Object getValueAt(int mColumn) {
    return sums[mColumn];
  }

  public String getColumnName(int mColumn) {
    return null;    // never invoked
  }

  public Object getObject() {
    return null;    // never invoked
  }

  public int getColumnCount() {
    return types.length;   // but: never invoked
  }
  
}
