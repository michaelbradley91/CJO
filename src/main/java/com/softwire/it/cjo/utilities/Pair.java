package com.softwire.it.cjo.utilities;

/**
 * This class is designed to hold pair of objects in Java.
 * @author michael
 *
 * @param <Left> - the type of the left element in the pair
 * @param <Right> - the type of the right element in the pair
 */
public class Pair<Left, Right> {
	
	//The left and right items held in the pair
	private Left left;
	private Right right;

	/**
	 * Construct a new pair object, initialised with the given items
	 * @param left - the left object in the pair
	 * @param right - the right object in the pair
	 */
	public Pair(Left left, Right right) {
		this.left = left;
		this.right = right;
	}
	
	/**
	 * @return - the left element of this pair
	 */
	public Left getLeft() {
		return left;
	}
	
	/**
	 * @return - the right element of this pair
	 */
	public Right getRight() {
		return right;
	}
	
	/**
	 * @param left - the left element to place in this pair
	 */
	public void setLeft(Left left) {
		this.left = left;
	}
	
	/**
	 * @param right - the right element to place in this pair
	 */
	public void setRight(Right right) {
		this.right = right;
	}
	
	public boolean equals(Object obj) {
		if (obj==this) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		if (obj instanceof Pair<?,?>) {
			Pair<?,?> pair = (Pair<?,?>)obj;
			if (pair.left==null) {
				if (this.left!=null) {
					return false;
				}
			} else if(!pair.left.equals(this.left)) {
				return false;
			}
			if (pair.right==null) {
				if (this.right!=null) {
					return false;
				}
			} else if(!pair.right.equals(this.right)) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return (left==null ? 0 : left.hashCode())+(right==null ? 0 : right.hashCode());
	}
	
	public String toString() {
		return "(" + (left==null ? null : left.toString()) + "," + (right==null ? null : right.toString()) + ")";
	}
}
