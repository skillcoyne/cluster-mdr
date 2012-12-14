package org.epistasis.combinatoric;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

public class AttributeValueList<E extends Comparable<E>> extends
		AbstractList<E> implements Comparable<AttributeValueList<E>>, Cloneable {
	private Stack<E> values = new Stack<E>();

	@Override
	public void clear() {
		values.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			final AttributeValueList<E> a = (AttributeValueList<E>) super.clone();
			a.values = (Stack<E>) values.clone();
			return a;
		} catch (final CloneNotSupportedException ex) {
			return null;
		}
	}

	public int compareTo(final AttributeValueList<E> g) {
		if (size() < g.size()) {
			return -1;
		}
		if (size() > g.size()) {
			return 1;
		}
		for (Iterator<E> i = iterator(), j = g.iterator(); i.hasNext();) {
			final E c1 = i.next();
			final E c2 = j.next();
			final int result = c1.compareTo(c2);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (o == null) {
			return false;
		}
		final AttributeValueList<E> g = (AttributeValueList<E>) o;
		return values.equals(g.values);
	}

	@Override
	public E get(final int index) {
		return values.get(index);
	}

	public E pop() {
		return values.pop();
	}

	public void push(final E c) {
		values.push(c);
	}

	public void set(final Collection<E> c) {
		values.clear();
		values.addAll(c);
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public String toString() {
		final StringBuffer b = new StringBuffer();
		for (final E e : values) {
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(e);
		}
		return b.toString();
	}
}
