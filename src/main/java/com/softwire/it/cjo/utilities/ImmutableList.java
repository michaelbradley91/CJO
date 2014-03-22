package com.softwire.it.cjo.utilities;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ****************<br>
 * Date: 20/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class represents a very simple immutable list.<br>
 * Elements can be added to this list, which will result in a new list being returned. This is similar to "cons"
 * in Haskell.
 *
 * @param <T> - the type of elements to be stored in the list
 */
public class ImmutableList<T> implements Iterable<T> {
	//The next element in the immutable list (null indicates the end)
	private final ImmutableList<T> next;
	//The element held at this position
	private final T item;
	//The size of the list
	private final int size;
	
	/**
	 * Create an empty immutable list
	 */
	public ImmutableList() {
		next = null;
		item = null;
		size=0;
	}
	
	/**
	 * Construct a new list with the given parameters
	 * @param next - the tail of the list
	 * @param item - the item held at the head of the list
	 * @param size - the size of the list
	 */
	private ImmutableList(ImmutableList<T> next, T item, int size) {
		this.next = next;
		this.item = item;
		this.size = size;
	}
	
	/**
	 * @param item - an item to add to the front of the immutable list
	 * @return - a new immutable list, with the given item at the head, and all other items beyond it
	 */
	public ImmutableList<T> add(T item) {
		return new ImmutableList<T>(this,item,size+1);
	}
	
	/**
	 * @return - an immutable list with the first element removed
	 * @throws IllegalStateException - if this list is empty
	 */
	public ImmutableList<T> tail() {
		if (next==null) {
			throw new IllegalStateException("Cannot call remove() on an empty immutable list");
		} else {
			return next;
		}
	}
	
	/**
	 * @return - the element at the head of this list
	 * @throws IllegalStateException - if this list is empty
	 */
	public T head() {
		if (next==null) {
			throw new IllegalStateException("Cannot call head() on an empty immutable list");
		} else {
			return item;
		}
	}
	
	/**
	 * @return - the size of this list
	 */
	public int size() {
		return size;
	}
	
	/**
	 * @return - true iff this list is empty
	 */
	public boolean isEmpty() {
		return size==0;
	}
	
	/**
	 * @param item - an item to check for containment
	 * @return - true iff this list contains an item equal to this item
	 */
	public boolean contains(T item) {
		if (item==null) {
			return (item==null && this.item==null) || (isEmpty() ? false : tail().contains(item));
		} else {
			return (item.equals(this.item)) || (isEmpty() ? false : tail().contains(item));
		}
	}
	
	/**
	 * The iterator returned here should only be used by one thread - it itself is not thread safe.
	 * However, you can call other addition or removal operations as desired on this object.
	 */
	@Override
	public Iterator<T> iterator() {
		return new ImmutableListIterator<T>(this);
	}
	
	private static class ImmutableListIterator<T> implements Iterator<T> {
		private ImmutableList<T> current;
		/**
		 * @param list - the list to be iterated over
		 */
		public ImmutableListIterator(ImmutableList<T> list) {
			current = list;
		}
		
		@Override
		public boolean hasNext() {
			return !current.isEmpty();
		}

		@Override
		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			T item = current.head();
			current = current.tail();
			return item;
		}

		@Override
		public void remove() {
			//Doesn't make sense here.
			throw new UnsupportedOperationException();
		}
	}
	
	public boolean equals(Object obj) {
		if (obj==this) {
			return true;
		} else if(obj==null) {
			return false;
		} else if (obj instanceof ImmutableList<?>) {
			ImmutableList<?> other = (ImmutableList<?>)obj;
			if (other.size==this.size) {
				//Check recursively on objects...
				return equalsRecursive(other);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Only call this if the two lists are known to have the same size
	 * @param obj - another list to check for equality
	 * @return - true iff this is equal
	 */
	private boolean equalsRecursive(ImmutableList<?> obj) {
		if (item==null) {
			return obj.item==null && (isEmpty() ? true : next.equalsRecursive(obj.next));
		} else {
			return item.equals(obj.item) && (isEmpty() ? true : next.equalsRecursive(obj.next));
		}
	}
	
	public int hashCode() {
		if (next==null) {
			return 0; //the end of the list
		}
		//Recursively construct the hash code...
		if (item==null) {
			return 5 + 3*(next.hashCode());
		} else {
			return item.hashCode()+3*(next.hashCode());
		}
	}
}
