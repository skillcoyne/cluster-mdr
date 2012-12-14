package org.epistasis;

import java.util.Random;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class SURFStarnTuRFAttributeScorer extends TuRFAttributeScorer {
	public SURFStarnTuRFAttributeScorer(final Dataset data, final float pct,
			final Random random, final boolean parallel) {
		this(data, pct, random, parallel, null);
	}

	public SURFStarnTuRFAttributeScorer(final Dataset data, final float pct,
			final Random random, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, data.getRows(), 0, pct, random, parallel, onIncrementProgress);
		zTransformScores = true;
	}

	@Override
	protected String getFilterName() {
		return "SURF*nTURF";
	}

	@Override
	protected ReliefFAttributeScorer getReliefFAttributeScorer(final Dataset data) {
		final ReliefFAttributeScorer rf = new SURFStarAttributeScorer(data, rnd,
				parallel, progress);
		return rf;
	}
}
