package mjb.dev.cjo.utilities;

/**
 * This implements a box designed simply to hold another item.
 * @author michael
 *
 * @param <Item>
 */
public class Box<Item> {
	private Item item;
	
	/**
	 * Construct a new box, initialised with the given item
	 * @param item - the item to put in the box
	 */
	public Box(Item item) {
		this.item = item;
	}
	
	/**
	 * @return - the item inside the box
	 */
	public Item getItem() {
		return item;
	}
	
	/**
	 * @param item - the item to be placed inside the box
	 */
	public void setItem(Item item) {
		this.item = item;
	}
	
	public boolean equals(Object obj) {
		if (obj==this) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		if (obj instanceof Box<?>) {
			if (((Box<?>) obj).item==null) {
				return this.item==null;
			} else {
				return ((Box<?>) obj).item.equals(item);
			}
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		if (item==null) {
			return 0;
		}
		return item.hashCode();
	}
	
	public String toString() {
		if (item==null) {
			return "[" + null + "]";
		}
		return "[" + item.toString() + "]";
	}
}
