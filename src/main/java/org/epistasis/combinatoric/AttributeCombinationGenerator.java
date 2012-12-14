package org.epistasis.combinatoric;

import java.util.Iterator;

import org.epistasis.AttributeLabels;

public class AttributeCombinationGenerator implements
		Iterator<AttributeCombination> {
	private final CombinationGenerator cg;
	private AttributeLabels labels = null;

	public AttributeCombinationGenerator() {
		cg = new CombinationGenerator();
	}

	public AttributeCombinationGenerator(final AttributeLabels labels) {
		this.labels = labels;
		cg = new CombinationGenerator();
	}

	public AttributeCombinationGenerator(final AttributeLabels labels,
			final int nCombo) {
		this.labels = labels;
		cg = new CombinationGenerator(labels.size(), nCombo);
	}

	public AttributeCombinationGenerator(final int nVars, final int nCombo) {
		cg = new CombinationGenerator(nVars, nCombo);
	}

	public boolean hasNext() {
		return cg.hasNext();
	}

	public AttributeCombination next() {
		final int[] combo = cg.next();
		if (combo == null) {
			return null;
		}
		return new AttributeCombination(combo, labels);
	}

	public void remove() {
		cg.remove();
	}

	public void setLabels(final AttributeLabels labels) {
		this.labels = labels;
	}
}
