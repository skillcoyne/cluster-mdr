package org.epistasis;

import java.util.Enumeration;
import java.util.Iterator;

public class IterableEnumeration<T> implements Iterable<T> {
	private final Enumeration<T> en;

	public static <T> Iterable<T> make(final Enumeration<T> en) {
		return new IterableEnumeration<T>(en);
	}

	public IterableEnumeration(final Enumeration<T> en) {
		this.en = en;
	}

	// return an adaptor for the Enumeration
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return en.hasMoreElements();
			}

			public T next() {
				return en.nextElement();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
