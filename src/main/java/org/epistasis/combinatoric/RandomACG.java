package org.epistasis.combinatoric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.epistasis.AttributeLabels;

public abstract class RandomACG implements Iterator<AttributeCombination> {
	private final Random rand;
	private final List<Integer> attributes;
	private final AttributeLabels labels;
	private final int attrCount;

	public RandomACG(final AttributeLabels labels, final int attrCount,
			final Random rand) {
		this.labels = labels;
		this.rand = rand;
		this.attrCount = attrCount;
		attributes = new ArrayList<Integer>(labels.size());
		for (int i = 0; i < labels.size(); ++i) {
			attributes.add(i);
		}
	}

	public synchronized AttributeCombination next() {
		final int[] attr = new int[attrCount];
		Collections.shuffle(attributes, rand);
		for (int i = 0; i < attr.length; ++i) {
			attr[i] = attributes.get(i);
		}
		return new AttributeCombination(attr, labels);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
