package org.epistasis.combinatoric;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DistanceMatrix<E> {
	private final List<E> keys = new ArrayList<E>();
	private final List<List<Double>> values = new ArrayList<List<Double>>();

	private int addKey(final E key) {
		final int ret = keys.size();
		keys.add(key);
		for (final List<Double> list : values) {
			list.add(null);
		}
		final List<Double> row = new ArrayList<Double>(keys.size());
		for (int i = 0; i < keys.size(); ++i) {
			row.add(null);
		}
		values.add(row);
		return ret;
	}

	public boolean contains(final E a, final E b) {
		final int i = keys.indexOf(a);
		if (i == -1) {
			return false;
		}
		final int j = keys.indexOf(b);
		if (j == -1) {
			return false;
		}
		if (getCell(i, j) == null) {
			return false;
		}
		return true;
	}

	public double get(final E a, final E b) {
		final int i = keys.indexOf(a);
		if (i == -1) {
			throw new NoSuchElementException(a.toString());
		}
		final int j = keys.indexOf(b);
		if (j == -1) {
			throw new NoSuchElementException(b.toString());
		}
		final Double value = getCell(i, j);
		if (value == null) {
			throw new NoSuchElementException(a.toString() + " x " + b.toString());
		}
		return value;
	}

	private Double getCell(final int i, final int j) {
		return values.get(i).get(j);
	}

	public List<E> getKeys() {
		return keys;
	}

	public double getMaxValue() {
		final List<Double> max = new ArrayList<Double>();
		for (final List<Double> sublist : values) {
			for (final Double x : sublist) {
				if (x != null) {
					max.add(x);
				}
			}
		}
		final Double d = Collections.max(max);
		return d == null ? Double.NaN : d.doubleValue();
	}

	public double getMinValue() {
		final List<Double> min = new ArrayList<Double>();
		for (final List<Double> sublist : values) {
			for (final Double x : sublist) {
				if (x != null) {
					min.add(x);
				}
			}
		}
		final Double d = Collections.min(min);
		return d == null ? Double.NaN : d.doubleValue();
	}

	public void put(final E a, final E b, final double value) {
		int i = keys.indexOf(a);
		int j = keys.indexOf(b);
		if (i == -1) {
			i = addKey(a);
		}
		if (j == -1) {
			j = addKey(b);
		}
		setCell(i, j, value);
		setCell(j, i, value);
	}

	public void remove(final E a) {
		final int i = keys.indexOf(a);
		if (i == -1) {
			throw new NoSuchElementException(a.toString());
		}
		removeKey(i);
	}

	private void removeKey(final int idx) {
		keys.remove(idx);
		values.remove(idx);
		for (final List<Double> list : values) {
			list.remove(idx);
		}
	}

	public void replace(final E oldKey, final E newKey) {
		final int i = keys.indexOf(oldKey);
		if (i == -1) {
			throw new NoSuchElementException(oldKey.toString());
		}
		keys.set(i, newKey);
	}

	private void setCell(final int i, final int j, final Double value) {
		values.get(i).set(j, value);
	}

	public void write(final PrintWriter w) {
		w.println("\"A\",\"B\",\"Dist\"");
		for (int i = 0; i < keys.size() - 1; ++i) {
			final E key1 = keys.get(i);
			for (int j = i + 1; j < keys.size(); ++j) {
				final E key2 = keys.get(j);
				w.print('"');
				w.print(key1);
				w.print("\",\"");
				w.print(key2);
				w.print("\",\"");
				w.print(get(key1, key2));
				w.println('"');
			}
		}
		w.flush();
	}

	public void write_matrix(final PrintWriter w) {
		w.print("\"\"");
		for (final E key : keys) {
			w.print(",\"");
			w.print(key);
			w.print('"');
		}
		w.println();
		Iterator<E> i;
		Iterator<List<Double>> j;
		for (i = keys.iterator(), j = values.iterator(); i.hasNext();) {
			w.print('"');
			w.print(i.next());
			w.print('"');
			for (final Double d : j.next()) {
				w.print(",\"");
				w.print(d == null ? "0.0" : d.toString());
				w.print('"');
			}
			w.println();
		}
		w.flush();
	}
}
