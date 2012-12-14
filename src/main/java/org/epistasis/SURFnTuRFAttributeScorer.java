package org.epistasis;

import java.util.Random;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class SURFnTuRFAttributeScorer extends TuRFAttributeScorer {
	public SURFnTuRFAttributeScorer(final Dataset data, final float pct,
			final Random random, final boolean parallel) {
		this(data, pct, random, parallel, null);
	}

	public SURFnTuRFAttributeScorer(final Dataset data, final float pct,
			final Random random, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, data.getRows(), 0, pct, random, parallel, onIncrementProgress);
		zTransformScores = true;
	}

	@Override
	protected String getFilterName() {
		return "SURFnTURF";
	}

	@Override
	protected ReliefFAttributeScorer getReliefFAttributeScorer(final Dataset data) {
		final ReliefFAttributeScorer rf = new SURFAttributeScorer(data, rnd,
				parallel, progress);
		return rf;
	}
}
