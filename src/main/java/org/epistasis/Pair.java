package org.epistasis;

import java.util.Comparator;

public class Pair<F, S> {
	private F first;
	private S second;

	public Pair(final F first, final S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public void setFirst(final F first) {
		this.first = first;
	}

	public void setSecond(final S second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return first + "," + second;
	}

	public static class FirstComparator<F extends Comparable<F>, S extends Comparable<S>>
			implements Comparator<Pair<F, S>> {
		private Comparator<F> cmp;

		public FirstComparator() {
		}

		public FirstComparator(final Comparator<F> cmp) {
			this.cmp = cmp;
		}

		public int compare(final Pair<F, S> p1, final Pair<F, S> p2) {
			int compareResult;
			if (cmp == null) {
				compareResult = p1.first.compareTo(p2.first);
			} else {
				compareResult = cmp.compare(p1.first, p2.first);
			}
			return compareResult;
		}
	}

	public static class SecondComparator<F extends Comparable<F>, S extends Comparable<S>>
			implements Comparator<Pair<F, S>> {
		private Comparator<S> cmp;

		public SecondComparator() {
		}

		public SecondComparator(final Comparator<S> cmp) {
			this.cmp = cmp;
		}

		public int compare(final Pair<F, S> p1, final Pair<F, S> p2) {
			int compareResult;
			if (cmp == null) {
				compareResult = p1.second.compareTo(p2.second);
			} else {
				compareResult = cmp.compare(p1.second, p2.second);
			}
			return compareResult;
		}
	}
}
