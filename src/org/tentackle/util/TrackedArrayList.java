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

// $Id: TrackedArrayList.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;



/**
 * An extended ArrayList with the ability
 * to tell whether it has been structurally modified.
 * <p>
 * Furthermore, a TrackedArrayList distinguish between additions
 * and removals. Replacements are treated as a removal followed
 * by an addition.
 * Furthermore, all removed objects are kept in a separate list.
 * <p>
 * The main usage is in OR-mapping to check whether a list should
 * be written back to persistant storage or not and which elements
 * have to be deleted and which to be inserted or updated. However,
 * the TrackedArrayList is not restricted to DbObjects.
 * However, if it is used in OR-mapping (see DbRelations.wrbl), all
 * objects must be DbObjects and must be tracked as well (honour
 * isModified()).
 * <p>
 * Notice that the list of added objects is just the sum of this list
 * and the list of removed objects.
 * 
 * @param <E> the element type
 * @author harald
 */
public class TrackedArrayList<E> extends ArrayList<E> {
  
  private static final long serialVersionUID = 5820724718467532237L;
  
  private boolean modified;         // initial modification parameter or from setModified(true)
  private boolean removed;          // some object has been removed
  private boolean added;            // some object has been added
  
  private ArrayList<E> removedObjects;   // list of removed objects
  
  
  /**
   * Creates a tracked ArrayList.
   *
   * @param initialCapacity is the initial size of the list
   * @param modified is true if list is marked modified initially
   */
  public TrackedArrayList(int initialCapacity, boolean modified) {
    super(initialCapacity);
    setModified(modified);
  }
  
  
  /**
   * Creates a tracked ArrayList.
   *
   * @param modified is true if list is marked modified initially
   */  
  public TrackedArrayList(boolean modified) {
    super();
    setModified(modified);
  }
  
  
  /**
   * Creates a tracked ArrayList.
   * Notice that the list is initially marked 'modified'!
   * Thus replacing a list with an empty one will still treated as
   * a modification.
   */
  public TrackedArrayList() {
    this(true);
  }
  
  
  /**
   * Creates a list from a collection.
   * The list is NOT marked modified.
   * @param collection the collection
   */
  public TrackedArrayList(Collection<E> collection) {
    super(collection);
  }
  
  
  
  /**
   * Clones the list.
   * A cloned list is always not modified and does not contain any removed objects.
   */
  @Override
  @SuppressWarnings("unchecked")
  public TrackedArrayList<E> clone() {
    TrackedArrayList<E> nlist = (TrackedArrayList<E>)super.clone();
    nlist.removedObjects = null;
    modified = false;
    removed = false;
    added = false;
    return nlist;
  }  
  
  
  /**
   * Sets the modified state.
   *
   * @param modified is true to set modified to true, 
   *        false to clear all 4 flags including the list of removed Objects.
   */
  public void setModified(boolean modified) {
    if (modified) {
      modified = true;
    }
    else  {
      // applies to all
      modified = false;
      added    = false;
      removed  = false;
      removedObjects = null;
    }
  }
  
  
  /**
   * Gets the modification state, i.e.
   * whether objects have been added, removed or replaced or
   * setModified(true) was invoked.
   *
   * @return true if modified.
   */
  public boolean isModified() {
    return modified || added || removed;
  }
  
  /**
   * Gets the removal state.
   *
   * @return true if objects have been removed.
   */
  public boolean isObjectRemoved() {
    return removed;
  }
  
  /**
   * Gets the added state.
   *
   * @return true if objects have been added.
   */
  public boolean isObjectAdded() {
    return added;
  }
  
  
  /**
   * Gets the removed objects in the order of their removal.
   *
   * @return the list of removed objects, null if no object has been removed so far.
   */
  public List<E> getRemovedObjects() {
    return removedObjects;
  }
  
  
  
  /**
   * Adds an object to the list of removed objects.
   * Only objects of type <E> are added.
   * The method is invoked whenever an object is removed
   * from the list.
   * 
   * @param object the object to be added.
   */
  protected void addRemovedObject(E object) {
    if (removedObjects == null) {
      removedObjects = new ArrayList<E>();
    }
    removedObjects.add(object);
    removed = true;
  }
  
  
  /**
   * Adds a list of objects to the list of removed objects.
   * Only objects of type <E> are added.
   * The method is invoked whenever an object is removed
   * from the list.
   * 
   * @param objects the list of objects
   */
  protected void addRemovedObjects(Collection<E> objects) {
    for (E o: objects) {
      addRemovedObject(o);
    }
  }
  
  
  
  
  /**
   * Appends the specified element to the end of this list.
   *
   * @param o element to be appended to this list
   * @return <tt>true</tt> (as specified by {@link Collection#add})
   */
  @Override
  public boolean add(E o) {
    if (super.add(o)) {
      added = true;
      return true;
    }
    return false;
  }

  
  /**
   * Inserts the specified element at the specified position in this
   * list. Shifts the element currently at that position (if any) and
   * any subsequent elements to the right (adds one to their indices).
   *
   * @param index index at which the specified element is to be inserted
   * @param element element to be inserted
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */  
  @Override
  public void add(int index, E element) {
    super.add(index, element);
    added = true;
  }

  
  /**
   * Appends all of the elements in the specified collection to the end of
   * this list, in the order that they are returned by the
   * specified collection's Iterator.  The behavior of this operation is
   * undefined if the specified collection is modified while the operation
   * is in progress.  (This implies that the behavior of this call is
   * undefined if the specified collection is this list, and this
   * list is nonempty.)
   *
   * @param c collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @throws NullPointerException if the specified collection is null
   */  
  @Override
  public boolean addAll(Collection<? extends E> c) {
    if (super.addAll(c))  {
      added = true;
      return true;
    }
    return false;
  }
  

  /**
   * Inserts all of the elements in the specified collection into this
   * list, starting at the specified position.  Shifts the element
   * currently at that position (if any) and any subsequent elements to
   * the right (increases their indices).  The new elements will appear
   * in the list in the order that they are returned by the
   * specified collection's iterator.
   *
   * @param index index at which to insert the first element from the
   *              specified collection
   * @param c collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @throws IndexOutOfBoundsException {@inheritDoc}
   * @throws NullPointerException if the specified collection is null
   */  
  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    if (super.addAll(index, c))  {
      added = true;
      return true;
    }
    return false;
  }

  
  /**
   * Removes all of the elements from this list.  The list will
   * be empty after this call returns. All objects in the list
   * will be added to the list of removed objects.
   */  
  @Override
  public void clear() {
    addRemovedObjects(this);
    super.clear();
  }

  /**
   * Removes the element at the specified position in this list.
   * Shifts any subsequent elements to the left (subtracts one from their
   * indices). The removed object will be added to the list of removed
   * objects.
   *
   * @param index the index of the element to be removed
   * @return the element that was removed from the list
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */  
  @Override
  public E remove(int index) {
    E obj = super.remove(index);
    addRemovedObject(obj);
    return obj;
  }

  /**
   * Removes the first occurrence of the specified element from this list,
   * if it is present.  If the list does not contain the element, it is
   * unchanged.  More formally, removes the element with the lowest index
   * <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
   * (if such an element exists).  Returns <tt>true</tt> if this list
   * contained the specified element (or equivalently, if this list
   * changed as a result of the call). 
   * The removed object will be added to the list of removed objects.
   *
   * @param obj element to be removed from this list, if present
   * @return <tt>true</tt> if this list contained the specified element
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean remove(Object obj) {
    if (super.remove(obj)) {
      addRemovedObject((E)obj);
      return true;
    }
    return false;
  }

  /**
   * Removes all objects that are contained in the given collection.
   * All removed object will be added to the list of removed objects.
   *
   * @param c the collection containing the objects to be removed from this list.
   * @return true if at least one object has been removed.
   */  
  @Override
  public boolean removeAll(Collection<?> c) {
    // re-implemented here to catch removed objects
    boolean someRemoved = false;
    Iterator<E> iter = iterator();
    while (iter.hasNext()) {
      E o = iter.next();
      if (c.contains(o)) {
        iter.remove();
        addRemovedObject(o);
        someRemoved = true;
      }
    }
    return someRemoved;
  }

  
  /**
   * Removes all objects that are not contained in the given collection.
   * All removed object will be added to the list of removed objects.
   *
   * @param c the collection containing the objects not to be removed from this list.
   * @return true if at least one object has been removed.
   */  
  @Override
  public boolean retainAll(Collection<?> c) {
    // re-implemented here to catch removed objects
    boolean someRemoved = false;
    Iterator<E> iter = iterator();
    while (iter.hasNext()) {
      E o = iter.next();
      if (c.contains(o) == false) {
        iter.remove();
        addRemovedObject(o);
        someRemoved = true;
      }
    }
    return someRemoved;
  }

  
  /**
   * Removes from this list all of the elements whose index is between
   * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
   * Shifts any succeeding elements to the left (reduces their index).
   * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
   * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
   * All removed object will be added to the list of removed objects.
   *
   * @param fromIndex index of first element to be removed
   * @param toIndex index after last element to be removed
   * @throws IndexOutOfBoundsException if fromIndex or toIndex out of
   *              range (fromIndex &lt; 0 || fromIndex &gt;= size() || toIndex
   *              &gt; size() || toIndex &lt; fromIndex)
   */
  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    int max = size();
    // we need to align before because we have to catch the removed objects
    if (fromIndex < max) {
      if (toIndex > max)  {
        toIndex = max;
      }
      if (toIndex - fromIndex > 0) {
        addRemovedObjects(subList(fromIndex, toIndex));
        super.removeRange(fromIndex, toIndex);
      }
    }
  }
  
  
  /**
   * Replaces the element at the specified position in this list with
   * the specified element. The old element will be added to the list
   * of removed objects. Thus, replacing an element will result in
   * isAdded() and isRemoved() to return true.
   *
   * @param index index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */  
  @Override
  public E set(int index, E element) {
    E obj = super.set(index, element);
    if (Compare.equals(obj, element) == false)  {
      addRemovedObject(obj);
      added = true;
    }
    return obj;
  }

  
}
