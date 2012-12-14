package org.epistasis.combinatoric;

import java.util.Random;

import org.epistasis.AttributeLabels;

public class FixedRandomACG extends RandomACG {
	private final long maxEval;
	private long nEval;

	public FixedRandomACG(final AttributeLabels labels, final int attrCount,
			final Random rand, final long maxEval) {
		super(labels, attrCount, rand);
		this.maxEval = maxEval;
		nEval = 0;
	}

	public boolean hasNext() {
		return nEval <= maxEval;
	}

	@Override
	public synchronized AttributeCombination next() {
		final AttributeCombination next = super.next();
		++nEval;
		return next;
	}
}
