package org.epistasis.combinatoric;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.epistasis.AbstractAttributeScorer;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;

public class AttributeRankerThread extends Thread {
	private final AttributeRanker ranker = new AttributeRanker();
	private AttributeCombination combo;
	private final Double value;
	private final boolean ascending;
	private boolean closed;
	private final SelectBy mode;
	private final Runnable onEnd;
	private Map<String, String> config;

	public AttributeRankerThread(final AbstractAttributeScorer scorer,
			final double threshold, final boolean above, final boolean closed,
			final Runnable onEnd) {
		ranker.setScorer(scorer);
		value = new Double(threshold);
		mode = SelectBy.Threshold;
		ascending = !above;
		this.closed = closed;
		this.onEnd = onEnd;
		config = new TreeMap<String, String>(scorer.getConfig());
		config.put("SELECTION", "THRESHOLD");
		config.put("SELECTIONVALUE", value.toString());
		config = Collections.unmodifiableMap(config);
	}

	public AttributeRankerThread(final AbstractAttributeScorer scorer,
			final double pct, final boolean ascending, final Runnable onEnd) {
		ranker.setScorer(scorer);
		value = new Double(pct);
		mode = SelectBy.Percent;
		this.ascending = ascending;
		this.onEnd = onEnd;
		config = new TreeMap<String, String>(scorer.getConfig());
		config.put("SELECTION", "TOP%");
		config.put("SELECTIONVALUE", value.toString());
		config = Collections.unmodifiableMap(config);
	}

	public AttributeRankerThread(final AbstractAttributeScorer scorer,
			final int n, final boolean ascending, final Runnable onEnd) {
		ranker.setScorer(scorer);
		value = new Double(n);
		mode = SelectBy.N;
		this.ascending = ascending;
		this.onEnd = onEnd;
		config = new TreeMap<String, String>(scorer.getConfig());
		config.put("SELECTION", "TOPN");
		config.put("SELECTIONVALUE", Integer.toString(n));
		config = Collections.unmodifiableMap(config);
	}

	public AttributeCombination getCombo() {
		return combo;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public AttributeRanker getRanker() {
		return ranker;
	}

	@Override
	public void run() {
		switch (mode) {
			case N:
				combo = ranker.selectN(value.intValue(), ascending);
				break;
			case Percent:
				combo = ranker.selectPct(value.doubleValue(), ascending);
				break;
			case Threshold:
				combo = ranker.selectThreshold(value.doubleValue(), !ascending, closed);
				break;
		}
		if (onEnd != null) {
			onEnd.run();
		}
	}

	private static enum SelectBy {
		N, Percent, Threshold,
	}
}
