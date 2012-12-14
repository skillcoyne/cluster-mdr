package org.epistasis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class TuRFAttributeScorer extends AbstractAttributeScorer {
	private static final Comparator<Pair<Integer, Double>> cmp = Collections
			.reverseOrder(new Pair.SecondComparator<Integer, Double>());
	protected final Runnable progress = new IncrementProgress();
	private Map<String, String> config;
	protected int m;
	private int k;
	private float pct;
	protected Random rnd;
	protected boolean parallel;
	protected boolean zTransformScores = false;

	public TuRFAttributeScorer(final Dataset data, final int m, final int k,
			final float pct, final Random rnd, final boolean parallel) {
		super(data);
		init(m, k, pct, rnd, parallel);
	}

	public TuRFAttributeScorer(final Dataset data, final int m, final int k,
			final float pct, final Random rnd, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, onIncrementProgress);
		init(m, k, pct, rnd, parallel);
	}

	@Override
	protected double[] computeScores() {
		final Map<String, Double> scoreMap = new HashMap<String, Double>();
		final List<String> ranks = new ArrayList<String>();
		final List<Double> deltas = new ArrayList<Double>();
		Dataset data = getData();
		final int nremove = (int) Math.ceil(pct * numAttributes);
		int[] newAttr = null;
		for (;;) {
			final ReliefFAttributeScorer rf = getReliefFAttributeScorer(data);
			double[] scoresToCompareAndStore = rf.computeScores();
			if (zTransformScores) {
				scoresToCompareAndStore = makeZScores(scoresToCompareAndStore);
			}
			for (int i = 0; i < scoresToCompareAndStore.length; ++i) {
				scoreMap.put(data.getLabels().get(i), scoresToCompareAndStore[i]);
			}
			final List<Pair<Integer, Double>> sorted = new ArrayList<Pair<Integer, Double>>(
					scoresToCompareAndStore.length);
			for (int i = 0; i < scoresToCompareAndStore.length; ++i) {
				sorted.add(new Pair<Integer, Double>(i, scoresToCompareAndStore[i]));
			}
			Collections.sort(sorted, TuRFAttributeScorer.cmp);
			if (Console.preserveTuRFRemovalOrder) {
				final int firstRemovalIndex = Math.max(0,
						(scoresToCompareAndStore.length - nremove));
				for (int i = scoresToCompareAndStore.length - 1; i >= firstRemovalIndex; --i) {
					final Pair<Integer, Double> attributeBeingRemoved = sorted.get(i);
					final int indexOfAttributeBeingRemoved = attributeBeingRemoved
							.getFirst();
					final double reliefFScoreOfAttributeBeingRemoved = attributeBeingRemoved
							.getSecond();
					ranks.add(0, data.getLabels().get(indexOfAttributeBeingRemoved));
					double delta = Double.NaN;
					if (i == 0) {
						delta = 0.0;
					} else {
						final Pair<Integer, Double> attributeBeforeOneBeingRemoved = sorted
								.get(i - 1);
						final double reliefFScoreOfAttributeBeforeOneBeingRemoved = attributeBeforeOneBeingRemoved
								.getSecond();
						delta = reliefFScoreOfAttributeBeforeOneBeingRemoved
								- reliefFScoreOfAttributeBeingRemoved;
					}
					deltas.add(0, delta);
				} // end loop over ones about to be removed
			} // end if (Console.preserveTuRFRemovalOrder)
			if (scoresToCompareAndStore.length <= (nremove + 1)) {
				break; // exit out of loop
			}
			newAttr = new int[scoresToCompareAndStore.length - nremove];
			for (int i = 0; i < scoresToCompareAndStore.length - nremove; ++i) {
				newAttr[i] = sorted.get(i).getFirst();
			}
			data = data.filter(newAttr);
		} // end infinite loop until
		if (Console.preserveTuRFRemovalOrder) {
			// now need to make all scores have a common context. We use the final set of attributes as our list of best attributes. We then test
			// each other attribute in the context of the best attributes
			double currentScore = scoreMap.get(ranks.get(0));
			for (int i = 1; i < ranks.size(); ++i) {
				final String rankedAttribute = ranks.get(i);
				final double delta = deltas.get(i);
				currentScore -= delta;
				scoreMap.put(rankedAttribute, currentScore);
			}
		}
		final double[] scores = new double[numAttributes];
		for (int i = 0; i < scores.length; ++i) {
			scores[i] = scoreMap.get(getData().getLabels().get(i));
		}
		// for convenience during gui testing show the ranks
		if (Main.isExperimental
				&& (org.epistasis.combinatoric.mdr.gui.Frame.getFrame() != null)
				&& (getData().getLabels().get(0).equals("X0") || getData().getLabels()
						.get(1).equals("X1"))) {
			System.out.println("X0 rank: " + (ranks.indexOf("X0") + 1) + " X1 rank: "
					+ (ranks.indexOf("X1") + 1));
		}
		return scores;
	}

	@Override
	public Map<String, String> getConfig() {
		return config;
	}

	protected String getFilterName() {
		return "TURF";
	}

	protected ReliefFAttributeScorer getReliefFAttributeScorer(final Dataset data) {
		final ReliefFAttributeScorer rf = new ReliefFAttributeScorer(data, m, k,
				rnd, parallel, progress);
		return rf;
	}

	@Override
	public int getTotalProgress() {
		return m * (int) Math.ceil(1 / pct);
	}

	private void init(final int pM, final int pK, final float pPct,
			final Random pRnd, final boolean pParallel) {
		m = pM;
		k = pK;
		pct = pPct;
		rnd = pRnd;
		parallel = pParallel;
		config = new HashMap<String, String>();
		config.put("FILTER", getFilterName());
		config.put("NEIGHBORS", Integer.toString(k));
		config.put("PERCENT", Float.toString(pct));
		config.put("SAMPLES",
				m == getData().getRows() ? "ALL" : Integer.toString(m));
		config = Collections.unmodifiableMap(config);
	}

	private class IncrementProgress implements Runnable {
		public void run() {
			incrementProgress();
		}
	}
}
