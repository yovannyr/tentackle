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

// $Id: TableSerialExpirationBacklog.java 409 2008-09-02 13:52:50Z harald $

package org.tentackle.db;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.tentackle.util.Compare;


/**
 * Keeps a backlog of expiration sets.<br>
 * Used in RMI-servers to reduce database roundtrips for clients
 * requesting the expiration info for their local caches.
 *
 * @author harald
 */
public class TableSerialExpirationBacklog {

  // holds a set of expiration info
  private class ExpirationSet implements Cloneable {
    
    private long minSerial;       // > minSerial
    private long maxSerial;       // <= maxSerial
    private long[] idSer;         // pairs of id/tableserial
    
    private ExpirationSet(long minSerial, long maxSerial, long[] idSer) {
      this.minSerial = minSerial;
      this.maxSerial = maxSerial;
      this.idSer = idSer;
    }

    @Override
    public ExpirationSet clone() {
      try {
        return (ExpirationSet)super.clone();
      } 
      catch (CloneNotSupportedException ex) {
        return null;
      }
    }
  }
  
  
  
  // the backlog
  private ExpirationSet[] expirations;      // array of expirations
  private int nextNdx;                      // round robin index
  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  
  
  
  /**
   * Creates a backlog.
   *
   * @param size the size of the backlog
   */
  public TableSerialExpirationBacklog(int size) {
    expirations = new ExpirationSet[size];      // should be enough for the 99% case
    for (int i=0; i < size; i++) {
      expirations[i] = null;    // null = unused yet
    }
  }
  
  /**
   * Creates a backlog with a default size.
   */
  public TableSerialExpirationBacklog() {
    this(32);   // should be enough for the 99% case
  }
  
  
  /**
   * Adds an expiration set to the backlog.
   *
   * @param minSerial the lower serial bound of the query (minSerial < tableSerial)
   * @param maxSerial the upper serial bound of the query (tableSerial <= maxSerial)
   * @param idSer the expiration info as pairs of id/tableserial.
   */
  public void addExpiration(long minSerial, long maxSerial, long[] idSer) {
    if (idSer.length > 0) {
      lock.writeLock().lock();
      try {
        expirations[nextNdx++] = new ExpirationSet(minSerial, maxSerial, idSer);
      }
      finally {
        lock.writeLock().unlock();
      }
    }
  }
  
  
  /**
   * Gets the expiration backlog for a given range of tableserials.
   * 
   * @param minSerial the lower serial bound of the query (minSerial < tableSerial)
   * @param maxSerial the upper serial bound of the query (tableSerial <= maxSerial)
   * @return the expiration info as pairs of id/tableserial, null if given range was not found in the backlog
   */
  public long[] getExpiration(long minSerial, long maxSerial) {
    
    // extract matching sets
    ExpirationSet[] matchedExps = new ExpirationSet[expirations.length];
    int matchCount = 0;
    lock.readLock().lock();
    try {
      // we start at current nextNdx: this is the oldest entry
      int ndx = nextNdx;
      for (int i=0; i < expirations.length; i++) {
        if (ndx >= expirations.length) {
          ndx = 0;
        }
        ExpirationSet exp = expirations[ndx++];
        if (exp != null) {
          if (exp.maxSerial >= minSerial && exp.minSerial <= maxSerial) {
            // add to list of matched sets
            matchedExps[matchCount++] = exp;
          }
        }
      }
    }
    finally {
      lock.readLock().unlock();
    }
    
    while (matchCount > 0) {
      // merge the sets. note that a merge creates a clone, so there's no need for synchronization
      matchedExps = merge(matchedExps, matchCount);
      if (matchedExps.length == matchCount) {
        // cannot be merged any further
        for (int m=0; m < matchCount; m++) {
          ExpirationSet exp = matchedExps[m];
          if (exp.minSerial <= minSerial && exp.maxSerial >= maxSerial) {
          
            // create the result of unique id/tableserial pairs.
            Set<IdSerTuple> setById = new TreeSet<IdSerTuple>(new Comparator<IdSerTuple>() {
              public int compare(IdSerTuple o1, IdSerTuple o2) {
                // order by id + tableserial descending
                int rv = Compare.compareLong(o2.id, o1.id);
                if (rv == 0) {
                  rv = Compare.compareLong(o2.tableSerial, o1.tableSerial);
                }
                return rv;
              }
            });

            int ndx = 0;
            while (ndx < exp.idSer.length) {
              long id = exp.idSer[ndx++];
              long ts = exp.idSer[ndx++];
              if (ts > minSerial && ts <= maxSerial) {
                setById.add(new IdSerTuple(id, ts));
              }
            }

            // only take the id/tableserial with the highest tableserial for duplicate ids
            Set<IdSerTuple> setByTs = new TreeSet<IdSerTuple>(new Comparator<IdSerTuple>() {
              public int compare(IdSerTuple o1, IdSerTuple o2) {
                // order by tableserial + id ascending
                int rv = Compare.compareLong(o1.tableSerial, o2.tableSerial);
                if (rv == 0) {
                  rv = Compare.compareLong(o1.id, o2.id);
                }
                return rv;
              }
            });

            long lastId = -1;
            for (IdSerTuple ids: setById) {
              if (ids.id != lastId) {
                // take only the first for an ID -> highest tableSerial
                setByTs.add(ids);
                lastId = ids.id;
              }
            }

            // fine! create the array to return
            long[] idSer = new long[setByTs.size() << 1];
            ndx = 0;
            for (IdSerTuple ids: setByTs) {
              idSer[ndx++] = ids.id;
              idSer[ndx++] = ids.tableSerial;
            }

            return idSer;
          }
        }
        // no usable range found
        break;
      }
      
      matchCount = matchedExps.length;
      // continue merge...
    }

    return null;    // no such range in backlog: ask the database!
  }
  
  
  
  /**
   * Merge expiration sets.
   *
   * @param expSets the expiration sets to merge
   * @return the merged sets
   */
  private ExpirationSet[] merge(ExpirationSet[] expSets, int count) {
    ExpirationSet[] mergedSets = new ExpirationSet[count];
    mergedSets[0] = expSets[0].clone();
    int mergeCount = 1;
    for (int i=1; i < count; i++) {
      ExpirationSet exp = expSets[i];
      boolean merged = false;
      for (int j=0; j < mergeCount; j++) {
        ExpirationSet mexp = mergedSets[j];
        if (exp.maxSerial >= mexp.minSerial && exp.minSerial <= mexp.minSerial) {
          mexp.minSerial = exp.minSerial;
          if (exp.maxSerial > mexp.maxSerial) {
            mexp.maxSerial = exp.maxSerial;
          }
          merged = true;
        }
        if (exp.minSerial <= mexp.maxSerial && exp.maxSerial >= mexp.maxSerial) {
          mexp.maxSerial = exp.maxSerial;
          if (exp.minSerial < mexp.minSerial) {
            mexp.minSerial = exp.minSerial;
          }
          merged = true;
        }
        if (merged) {
          long[] idSer = new long[mexp.idSer.length + exp.idSer.length];
          System.arraycopy(mexp.idSer, 0, idSer, 0, mexp.idSer.length);
          System.arraycopy(exp.idSer, 0, idSer, mexp.idSer.length, exp.idSer.length);
          mexp.idSer = idSer;
          break;
        }
      }
      if (!merged) {
        // not merged: add a clone to merged sets
        mergedSets[mergeCount++] = exp.clone();
      }
    }
    // cut result array in size
    ExpirationSet[] nMergedSets = new ExpirationSet[mergeCount];
    System.arraycopy(mergedSets, 0, nMergedSets, 0, mergeCount);
    return nMergedSets;
  }
  
  
  // for sorting via Comparator
  private class IdSerTuple {
    private long id;
    private long tableSerial;
    private IdSerTuple(long id, long tableSerial) {
      this.id = id;
      this.tableSerial = tableSerial;
    }
  }
  
}
