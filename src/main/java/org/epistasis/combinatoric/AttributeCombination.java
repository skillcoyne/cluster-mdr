package org.epistasis.combinatoric;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epistasis.AttributeLabels;

public class AttributeCombination extends AbstractList<Integer> implements
		Comparable<AttributeCombination> {
	private static final Pattern pOldAttributeFormat = Pattern
			.compile("^\\s*\\[\\s+(.+?)\\s+\\]\\s*$");
	private static final Pattern pAttributeFormat = Pattern
			.compile("^\\s*(.+?)\\s*$");
	private List<Integer> attributes;
	private AttributeLabels labels;

	public static int countAttributesInString(final String attributes) {
		Matcher m = AttributeCombination.pOldAttributeFormat.matcher(attributes);
		if (!m.matches()) {
			m = AttributeCombination.pAttributeFormat.matcher(attributes);
			if (!m.matches()) {
				return -1;
			}
		}
		return m.group(1).split("\\s+").length;
	}

	public AttributeCombination() {
	}

	public AttributeCombination(final int[] attributes) {
		setAttributes(attributes);
	}

	public AttributeCombination(final int[] attributes,
			final AttributeLabels labels) {
		this.labels = labels;
		setAttributes(attributes);
	}

	public AttributeCombination(final String comboString) {
		setComboString(comboString);
	}

	public AttributeCombination(final String comboString,
			final AttributeLabels labels) {
		this.labels = labels;
		setComboString(comboString);
	}

	/**
	 * Comparison function so that this object can serve as a map key.
	 * @param o Object to which compare this AttributeCombination
	 * @return < 0 if this object is less than the parameter, 0 if they are equal, > 0 if this object is greater than the parameter
	 */
	public int compareTo(final AttributeCombination k) {
		// an object is always equal to itself
		if (this == k) {
			return 0;
		}
		// sort based on length
		if (size() < k.size()) {
			return -1;
		} else if (size() > k.size()) {
			return 1;
		} else {
			for (int i = 0; i < size(); ++i) {
				final int attribute = (get(i)).intValue();
				final int kattribute = (k.get(i)).intValue();
				if (attribute < kattribute) {
					return -1;
				} else if (attribute > kattribute) {
					return 1;
				}
			}
		}
		// no differences, so they're equal
		return 0;
	}

	/**
	 * Convert a Set of Integers to an array of ints and assign that array to the attribute combination.
	 * @param s Set
	 */
	private void convertSetToArray(final Set<Integer> s) {
		// if the set is empty, clear the forced attribute combination
		if (s.isEmpty()) {
			attributes = null;
			return;
		}
		// allocate an array for the the AttributeCombination
		attributes = new ArrayList<Integer>(s.size());
		// copy each value from the set to the array
		attributes.addAll(s);
	}

	@Override
	public boolean equals(final Object o) {
		boolean isEqual;
		if (o == this) {
			isEqual = true;
		} else if (o instanceof AttributeCombination) {
			isEqual = super.equals(o);
		} else {
			isEqual = false;
		}
		return isEqual;
	}

	@Override
	public Integer get(final int index) {
		return attributes == null ? null : attributes.get(index);
	}

	public String getComboString() {
		final StringBuffer b = new StringBuffer();
		for (int i = 0; i < size(); ++i) {
			if (i != 0) {
				b.append(',');
			}
			final int attribute = (get(i)).intValue();
			if ((labels == null) || (attribute >= labels.size())) {
				b.append(attribute + 1);
			} else {
				b.append(labels.get(attribute));
			}
		}
		return b.toString();
	}

	public void setAttributes(final int[] attributes) {
		if (attributes == null) {
			this.attributes = null;
			return;
		}
		final Set<Integer> s = new TreeSet<Integer>();
		for (final int a : attributes) {
			s.add(a);
		}
		convertSetToArray(s);
	}

	public void setComboString(final String comboString) {
		if (comboString.trim().length() == 0) {
			attributes = null;
			return;
		}
		final String[] fields = comboString.split(",");
		final Set<Integer> s = new TreeSet<Integer>();
		for (int i = 0; i < fields.length; ++i) {
			fields[i] = fields[i].trim();
			Integer index = labels == null ? null : labels.get(fields[i]);
			if (index == null) {
				try {
					index = Integer.valueOf(fields[i]);
					if (index.intValue() < 1) {
						throw new IllegalArgumentException(index.toString()
								+ " is less than 1.");
					}
					index = new Integer(index.intValue() - 1);
				} catch (final NumberFormatException ex) {
					throw new IllegalArgumentException("'" + fields[i]
							+ "' is not a recognized attribute label.");
				}
			}
			s.add(index);
		}
		convertSetToArray(s);
	}

	public void setLabels(final AttributeLabels labels) {
		this.labels = labels;
	}

	@Override
	public int size() {
		return attributes == null ? 0 : attributes.size();
	}

	/**
	 * Convert an AttributeCombination to a String representation.
	 * @return String representation of the AttributeCombination
	 */
	@Override
	public String toString() {
		// use StringBuffer to build the string, to avoid needless
		// String reallocations
		final StringBuffer b = new StringBuffer();
		// add each element, delimited by whitespace
		for (int i = 0; i < size(); ++i) {
			// whitespace
			if (i > 0) {
				b.append(' ');
			}
			final int attribute = (get(i)).intValue();
			// next element
			if ((labels != null) && (labels.size() > attribute)) {
				b.append(labels.get(attribute));
			} else {
				b.append(attribute + 1);
			}
		}
		// return constructed String
		return b.toString();
	}

	public boolean trimToMax(final int max) {
		boolean trimmed = false;
		for (final ListIterator<Integer> i = attributes.listIterator(); i.hasNext();) {
			final Integer attribute = i.next();
			if (attribute.intValue() >= max) {
				i.remove();
				trimmed = true;
			}
		}
		return trimmed;
	}
}
