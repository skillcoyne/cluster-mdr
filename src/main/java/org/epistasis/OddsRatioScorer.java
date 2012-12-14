package org.epistasis;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class OddsRatioScorer extends IsolatedAttributeScorer {
	private Map<String, String> config = new TreeMap<String, String>();
	private byte focus;

	private static double computeOddsRatio(final double a, double b, double c,
			final double d) {
		if (b == 0) {
			b = 1;
		}
		if (c == 0) {
			c = 1;
		}
		return a * d / (b * c);
	}

	public OddsRatioScorer(final Dataset data, final boolean parallel) {
		super(data, parallel);
		// TODO: when polytomy happens, this will need to be a parameter
		focus = data.getAffectedStatus();
		init();
	}

	public OddsRatioScorer(final Dataset data, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, parallel, onIncrementProgress);
		init();
	}

	@Override
	protected double computeScore(final int index) {
		final int classIdx = getData().getCols() - 1;
		final int[] total = new int[2];
		double best = 0;
		final Map<Byte, int[]> m = new TreeMap<Byte, int[]>();
		for (int i = 0; i < getData().getRows(); ++i) {
			final byte value = getData().getRawDatum(i, index);
			int[] counts = m.get(value);
			if (counts == null) {
				counts = new int[2];
				m.put(value, counts);
			}
			final byte status = getData().getRawDatum(i, classIdx) == focus ? (byte) 0
					: (byte) 1;
			counts[status]++;
			total[status]++;
		}
		for (final int[] counts : m.values()) {
			double ratio = OddsRatioScorer.computeOddsRatio(counts[1], counts[0],
					total[1] - counts[1], total[0] - counts[0]);
			if (ratio > best) {
				best = ratio;
			}
			for (final int[] jcounts : m.values()) {
				if (jcounts == counts) {
					continue;
				}
				ratio = OddsRatioScorer.computeOddsRatio(counts[1], counts[0],
						jcounts[1], jcounts[0]);
				if (ratio > best) {
					best = ratio;
				}
			}
		}
		return best;
	}

	@Override
	public Map<String, String> getConfig() {
		return config;
	}

	private void init() {
		config.put("FILTER", "ODDSRATIO");
		config = Collections.unmodifiableMap(config);
	}
}
