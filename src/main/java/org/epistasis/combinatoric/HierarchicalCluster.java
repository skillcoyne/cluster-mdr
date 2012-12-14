package org.epistasis.combinatoric;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class HierarchicalCluster<E extends Comparable<E>> {
	private final List<Cluster<E>> clusters = new ArrayList<Cluster<E>>();
	private final DistanceMatrix<Cluster<E>> dist;
	public HierarchicalCluster(final DistanceMatrix<E> pDist) {
		for (final E item : pDist.getKeys()) {
			final Cluster<E> c = new Cluster<E>(item);
			clusters.add(c);
		}
		this.dist = new DistanceMatrix<Cluster<E>>();
		final int nkeys = pDist.getKeys().size();
		for (int i = 0; i < nkeys - 1; ++i) {
			for (int j = i + 1; j < nkeys; ++j) {
				this.dist.put(clusters.get(i), clusters.get(j),
						pDist.get(pDist.getKeys().get(i), pDist.getKeys().get(j)));
			}
		}
		while (clusters.size() > 1) {
			double mindist = Double.POSITIVE_INFINITY;
			Cluster<E> mina = null;
			Cluster<E> minb = null;
			for (int i = 0; i < clusters.size() - 1; ++i) {
				for (int j = i + 1; j < clusters.size(); ++j) {
					final Cluster<E> a = clusters.get(i);
					final Cluster<E> b = clusters.get(j);
					final double d = this.dist.get(a, b);
					if (d < mindist) {
						mina = a;
						minb = b;
						mindist = d;
					}
				}
			}
			final Cluster<E> merged = new Cluster<E>(mina, minb, mindist);
			clusters.remove(mina);
			clusters.remove(minb);
			for (final Cluster<E> c : clusters) {
				this.dist.put(merged, c, dist(merged, c));
			}
			clusters.add(merged);
		}
	}
	protected double avg_dist(final List<Double> dists) {
		double sum = 0;
		for (final double x : dists) {
			sum += x;
		}
		return sum / dists.size();
	}
	protected double dist(final Cluster<E> a, final Cluster<E> b) {
		double tempDist;
		if (dist.contains(a, b)) {
			tempDist = dist.get(a, b);
		} else {
			final List<Cluster<E>> list1 = new ArrayList<Cluster<E>>();
			final List<Cluster<E>> list2 = new ArrayList<Cluster<E>>();
			if (a.isLeaf()) {
				list1.add(a);
			} else {
				list1.add(a.getChild(0));
				list1.add(a.getChild(1));
			}
			if (b.isLeaf()) {
				list2.add(b);
			} else {
				list2.add(b.getChild(0));
				list2.add(b.getChild(1));
			}
			final List<Double> dists = new ArrayList<Double>(list1.size()
					* list2.size());
			for (final Cluster<E> c1 : list1) {
				for (final Cluster<E> c2 : list2) {
					if (dist.contains(c1, c2)) {
						dists.add(new Double(dist.get(c1, c2)));
					} else {
						final double d = dist(c1, c2);
						dist.put(c1, c2, d);
						dists.add(d);
					}
				}
			}
			tempDist = avg_dist(dists);
		}
		return tempDist;
	}
	public Cluster<E> getRoot() {
		return clusters.get(0);
	}
	protected double max_dist(final List<Double> dists) {
		double d = Double.NEGATIVE_INFINITY;
		for (final double x : dists) {
			if (x > d) {
				d = x;
			}
		}
		return d;
	}
	protected double min_dist(final List<Double> dists) {
		double d = Double.POSITIVE_INFINITY;
		for (final double x : dists) {
			if (x < d) {
				d = x;
			}
		}
		return d;
	}
	@Override
	public String toString() {
		final StringBuffer b = new StringBuffer();
		for (final Cluster<E> c : clusters) {
			b.append(c);
		}
		return b.toString();
	}
	public static class Cluster<E extends Comparable<E>> extends AbstractList<E> {
		private List<E> values = new ArrayList<E>();
		private final List<Cluster<E>> children;
		private final double dist;
		private double info;
		public Cluster(final Cluster<E> a, final Cluster<E> b, final double dist) {
			values = new ArrayList<E>(a.size() + b.size());
			children = new ArrayList<Cluster<E>>(2);
			children.add(a);
			children.add(b);
			this.dist = dist;
			values.addAll(a);
			values.addAll(b);
			Collections.sort(values);
		}
		public Cluster(final E value) {
			values = Collections.singletonList(value);
			children = null;
			dist = 0;
		}
		@Override
		public E get(final int index) {
			return values.get(index);
		}
		public Cluster<E> getChild(final int index) {
			return children.get(index);
		}
		public double getDist() {
			return dist;
		}
		public double getInfo() {
			return info;
		}
		@Override
		public int hashCode() {
			return values.hashCode();
		}
		public boolean isLeaf() {
			return children == null;
		}
		@Override
		public int size() {
			return values.size();
		}
		@Override
		public String toString() {
			return toString(0);
		}
		public String toString(final int indent) {
			final StringBuffer b = new StringBuffer();
			for (int i = 0; i < indent; ++i) {
				b.append(' ');
			}
			for (int i = 0; i < values.size(); ++i) {
				if (i != 0) {
					b.append(' ');
				}
				b.append(values.get(i));
			}
			if (dist > 0) {
				b.append("\t(");
				b.append(dist);
				b.append(')');
			}
			b.append('\n');
			if (children != null) {
				b.append(children.get(0).toString(indent + 2));
				b.append(children.get(1).toString(indent + 2));
			}
			return b.toString();
		}
	}
}
