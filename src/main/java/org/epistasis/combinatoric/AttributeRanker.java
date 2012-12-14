package org.epistasis.combinatoric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.epistasis.AbstractAttributeScorer;
import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class AttributeRanker {
	private AbstractAttributeScorer scorer;
	private List<Integer> attributes;
	private List<Pair<Integer, Double>> scores;

	public Dataset getData() {
		return scorer == null ? null : scorer.getData();
	}

	public AbstractAttributeScorer getScorer() {
		return scorer;
	}

	public List<Pair<Integer, Double>> getSortedScores() {
		return scores;
	}

	public double[] getUnsortedScores() {
		return scorer.getScores();
	}

	public List<Pair<Integer, Double>> rank(final boolean ascending) {
		Collections.sort(attributes, new RankOrder(scorer.getScores(), ascending));
		scores = new ArrayList<Pair<Integer, Double>>(attributes.size());
		for (final int attr : attributes) {
			scores.add(new Pair<Integer, Double>(attr, scorer.get(attr)));
		}
		return scores;
	}

	public AttributeCombination selectN(final int n, final boolean ascending) {
		if (scorer == null) {
			throw new IllegalStateException("No Scorer");
		}
		if (getData() == null) {
			throw new IllegalStateException("No Dataset");
		}
		rank(ascending);
		final List<Integer> attr = new ArrayList<Integer>(n);
		for (int i = 0; i < n; ++i) {
			attr.add(attributes.get(i));
		}
		return new AttributeCombination(attr, getData().getLabels());
	}

	public AttributeCombination selectPct(final double pct,
			final boolean ascending) {
		return selectN(
				(int) Math.floor(pct * (attributes == null ? 0 : attributes.size())),
				ascending);
	}

	public AttributeCombination selectThreshold(final double threshold,
			final boolean above, final boolean closed) {
		if (scorer == null) {
			throw new IllegalStateException("No Scorer");
		}
		if (getData() == null) {
			throw new IllegalStateException("No Dataset");
		}
		rank(!above);
		final List<Integer> selected = new ArrayList<Integer>();
		for (final int idx : attributes) {
			final double value = scorer.get(idx);
			if (!((above && (value > threshold)) || ((!above) && (value < threshold)) || (closed && (value == threshold)))) {
				break;
			}
			selected.add(idx);
		}
		return new AttributeCombination(selected, getData().getLabels());
	}

	public void setScorer(final AbstractAttributeScorer scorer) {
		this.scorer = scorer;
		if (scorer == null) {
			attributes = null;
		} else {
			attributes = new ArrayList<Integer>(scorer.getData().getCols() - 1);
			for (int i = 0; i < scorer.getData().getCols() - 1; ++i) {
				attributes.add(i);
			}
		}
	}

	private static class RankOrder implements Comparator<Integer> {
		private final double[] scores;
		private final boolean ascending;

		public RankOrder(final double[] scores, final boolean ascending) {
			this.scores = scores;
			this.ascending = ascending;
		}

		public int compare(final Integer o1, final Integer o2) {
			final double a = scores[o1];
			final double b = scores[o2];
			// if the scores are tied then used the attribute index to order the results
			final int ret = a < b ? -1 : a > b ? 1 : o1.compareTo(o2);
			return ascending ? ret : -ret;
		}
	}
}
