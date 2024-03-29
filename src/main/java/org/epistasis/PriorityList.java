package org.epistasis;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * List that keeps only the best n elements, and keeps them in sorted order.
 * @param <T> type of element
 */
public class PriorityList<T> extends TreeSet<T> {
	private static final long serialVersionUID = 1L;
	/**
	 */
	private int capacity;
	private long numAttemptedAdds = 0;
	private long numSuccessfulAdds = 0;

	/**
	 * Construct a PriorityList of given capacity. T must implement Comparable&lt;T&gt;.
	 * @param capacity maximum number of elements to keep
	 */
	public PriorityList(final int capacity) {
		this(capacity, null);
	}

	/**
	 * Construct a PriorityList of given capacity, using a user-specified comparator.
	 * @param capacity maximum number of elements to keep
	 * @param cmp user-specified comparator
	 */
	public PriorityList(final int capacity, final Comparator<T> cmp) {
		super(cmp);
		this.capacity = capacity;
	}

	/**
	 * Add an element to the list.
	 * @param obj element to add
	 * @return true if element was added
	 */
	@Override
	public boolean add(final T obj) {
		boolean added = false;
		if (size() <= capacity) {
			added = super.add(obj);
			if (size() > capacity) {
				final T objectToBeRemoved = last();
				if (objectToBeRemoved == obj) {
					added = false;
				}
				remove(objectToBeRemoved);
			}
		}
		++numAttemptedAdds;
		if (added) {
			++numSuccessfulAdds;
		}
		return added;
	}

	/**
	 * Add all elements from a given PriorityList to this PriorityList.
	 * @param l PriorityList from which to add elements
	 * @return true if the current list was changed
	 */
	public boolean addAll(final PriorityList<T> l) {
		boolean changed = false;
		for (final T obj : l) {
			changed |= add(obj);
		}
		return changed;
	}

	@Override
	public void clear() {
		numSuccessfulAdds = numAttemptedAdds = 0;
		super.clear();
	}

	/**
	 * Get the maximum number of elements for the list.
	 * @return the maximum number of elements for the list
	 */
	public int getCapacity() {
		return capacity;
	}

	public long getNumberOfAttemptedAdditions() {
		return numAttemptedAdds;
	}

	public long getNumberOfSuccessfulAdditions() {
		return numSuccessfulAdds;
	}

	public void setCapacity(final int capacity) {
		this.capacity = capacity;
		while (size() > capacity) {
			remove(last());
		}
	}
}
