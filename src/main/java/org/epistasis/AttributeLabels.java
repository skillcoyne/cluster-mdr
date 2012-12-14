package org.epistasis;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AttributeLabels extends AbstractList<String> implements Cloneable {
	private List<String> labels;
	private String classLabel;
	private Map<String, Integer> labelMap = new TreeMap<String, Integer>();

	public AttributeLabels() {
	}

	public AttributeLabels(final String[] labels) {
		this(labels, false);
	}

	public AttributeLabels(final String[] labels, final boolean removeClass) {
		labelMap.clear();
		if (labels == null) {
			this.labels = null;
		} else {
			this.labels = new ArrayList<String>();
			this.labels.addAll(Arrays.asList(labels));
			if (removeClass) {
				final List<String> newLabels = new ArrayList<String>();
				classLabel = labels[labels.length - 1];
				newLabels.addAll(this.labels.subList(0, labels.length - 1));
				this.labels = newLabels;
			}
		}
		remap();
	}

	public AttributeLabels(final String[] labels, final String classLabel) {
		this(labels, false);
		this.classLabel = classLabel;
	}

	@Override
	public void add(final int idx, final String label) {
		if (labels == null) {
			labels = new ArrayList<String>();
		}
		labels.add(idx, label);
		remap();
	}

	@Override
	public Object clone() {
		try {
			final AttributeLabels a = (AttributeLabels) super.clone();
			a.classLabel = classLabel;
			a.labelMap = null;
			a.remap();
			a.labels = new ArrayList<String>();
			a.labels.addAll(labels);
			return a;
		} catch (final CloneNotSupportedException ex) {
			return null;
		}
	}

	public boolean contains(final String o) {
		return labelMap.containsKey(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for (final Object name : c) {
			if (!contains(name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String get(final int index) {
		return labels == null ? null : labels.get(index);
	}

	public Integer get(final String label) {
		return labelMap == null ? null : labelMap.get(label);
	}

	public String getClassLabel() {
		return classLabel;
	}

	private void remap() {
		if (labelMap == null) {
			labelMap = new TreeMap<String, Integer>();
		} else {
			labelMap.clear();
		}
		for (int i = 0; (labels != null) && (i < labels.size()); ++i) {
			final String label = labels.get(i);
			if (labelMap.containsKey(label)) {
				throw new IllegalArgumentException("Duplicate label '" + label
						+ "' found.");
			}
			labelMap.put(label, i);
		}
	}

	@Override
	public String remove(final int idx) {
		final String label = labels.remove(idx);
		remap();
		return label;
	}

	@Override
	public int size() {
		return labels == null ? 0 : labels.size();
	}

	@Override
	public String toString() {
		final StringBuffer b = new StringBuffer();
		for (final Iterator<String> i = iterator(); i.hasNext();) {
			b.append(i.next());
			if (i.hasNext()) {
				b.append('\t');
			}
		}
		if (classLabel != null) {
			b.append('\t');
			b.append(classLabel);
		}
		return b.toString();
	}
}
