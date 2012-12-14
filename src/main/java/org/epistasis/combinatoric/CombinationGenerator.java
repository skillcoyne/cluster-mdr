package org.epistasis.combinatoric;

import java.util.Iterator;

public class CombinationGenerator implements Iterator<int[]> {
	private int nVars;
	private boolean hasNext;
	private int[] combo;

	public CombinationGenerator() {
		nVars = 0;
		hasNext = true;
		combo = new int[0];
	}

	public CombinationGenerator(final int nVars, final int nCombo) {
		set(nVars, nCombo);
	}

	public boolean hasNext() {
		return hasNext;
	}

	private boolean increment(final int index) {
		boolean didIncrement = false;
		if (combo.length > 0) {
			if (++combo[combo.length - 1 - index] < nVars - index) {
				for (int i = combo.length - index; i < combo.length; ++i) {
					combo[i] = combo[i - 1] + 1;
				}
				didIncrement = true;
			} else if (index + 1 < combo.length) {
				didIncrement = increment(index + 1);
			}
		} // end if combo length > 0
		return didIncrement;
	}

	public int[] next() {
		if (!hasNext()) {
			return null;
		}
		final int[] ret = combo.clone();
		hasNext = increment(0);
		return ret;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void reset() {
		hasNext = true;
		for (int i = 0; i < combo.length; ++i) {
			combo[i] = i;
		}
	}

	public void set(final int nVars, final int nCombo) {
		if (nCombo < 0) {
			throw new IllegalArgumentException("nCombo must be non-negative");
		}
		if (nVars < nCombo) {
			throw new IllegalArgumentException("nCombo cannot be larger than nVars");
		}
		this.nVars = nVars;
		combo = new int[nCombo];
		reset();
	}
}
