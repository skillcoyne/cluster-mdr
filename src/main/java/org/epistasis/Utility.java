package org.epistasis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Several utility functions for org.epistasis.org programs.
 */
public final class Utility {
	public static final String NEWLINE = System.getProperty("line.separator");

	public static String chrdup(final char c, final int count) {
		if (count < 0) {
			return "";
		}
		final char[] a = new char[count];
		Arrays.fill(a, c);
		return String.valueOf(a);
	}

	/**
	 * Compute the number of combinations possible when choosing r from n.
	 * @param n The total number of items
	 * @param r The number of items to choose
	 * @return Number of possible combinations
	 */
	public static BigInteger combinations(final BigInteger n, BigInteger r) {
		if (r.compareTo(n.shiftRight(1)) < 0) {
			r = n.subtract(r);
		}
		return Utility.product(n.subtract(r).add(BigInteger.ONE), n).divide(
				Utility.factorial(r));
	}

	/**
	 * Compute the number of combinations possible when choosing r from n.
	 * @param n The total number of items
	 * @param r The number of items to choose
	 * @return Number of possible combinations
	 */
	public static long combinations(final long n, long r) {
		if (r > n / 2) {
			r = n - r;
		}
		return Utility
				.product(BigInteger.valueOf(n - r + 1), BigInteger.valueOf(n))
				.divide(Utility.factorial(BigInteger.valueOf(r))).longValue();
	}

	public static <E extends Comparable<E>> int compareRanges(
			final Iterator<E> i, final Iterator<E> j) {
		while (i.hasNext() && j.hasNext()) {
			final E a = i.next();
			final E b = j.next();
			final int ret = a.compareTo(b);
			if (ret != 0) {
				return ret;
			}
		}
		if (i.hasNext()) {
			return 1;
		}
		if (j.hasNext()) {
			return -1;
		}
		return 0;
	}

	public static float computeChiSquared(final double[][] table) {
		final int rows = table.length;
		final int cols = rows > 0 ? table[0].length : 0;
		final double[] rtotal = new double[rows];
		final double[] ctotal = new double[cols];
		double total = 0;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				rtotal[i] += table[i][j];
				ctotal[j] += table[i][j];
			}
			total += rtotal[i];
		}
		float chisq = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final double expected = ctotal[j] * rtotal[i] / total;
				final double diff = table[i][j] - expected;
				chisq += diff * diff / expected;
			}
		}
		return chisq;
	}

	public static float computeChiSquared(final float[][] table) {
		final int rows = table.length;
		final int cols = rows > 0 ? table[0].length : 0;
		final double[] rtotal = new double[rows];
		final double[] ctotal = new double[cols];
		double total = 0;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				rtotal[i] += table[i][j];
				ctotal[j] += table[i][j];
			}
			total += rtotal[i];
		}
		float chisq = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final double expected = ctotal[j] * rtotal[i] / total;
				final double diff = table[i][j] - expected;
				chisq += diff * diff / expected;
			}
		}
		return chisq;
	}

	/**
	 * Returns value of kappa statistic if class is nominal. This was adapted from Weka 3.4.4.
	 * @param confusion A square matrix of floats with actual classes as the row indices and predicted classes as the column indices.
	 * @return the value of the kappa statistic
	 */
	public static float computeKappaStatistic(final float[][] confusion) {
		final float[] sumRows = new float[confusion.length];
		final float[] sumColumns = new float[confusion.length];
		float sumOfWeights = 0;
		float kappaStatistic;
		for (int i = 0; i < confusion.length; i++) {
			for (int j = 0; j < confusion.length; j++) {
				sumRows[i] += confusion[i][j];
				sumColumns[j] += confusion[i][j];
				sumOfWeights += confusion[i][j];
			}
		}
		float correct = 0, chanceAgreement = 0;
		for (int i = 0; i < confusion.length; i++) {
			chanceAgreement += (sumRows[i] * sumColumns[i]);
			correct += confusion[i][i];
		}
		chanceAgreement /= (sumOfWeights * sumOfWeights);
		correct /= sumOfWeights;
		if (chanceAgreement < 1) {
			kappaStatistic = (correct - chanceAgreement) / (1 - chanceAgreement);
		} else {
			kappaStatistic = 1;
		}
		return kappaStatistic;
	}

	/**
	 * Compute the factorial of a long integer.
	 * @param n Number of which to compute the factorial
	 * @return Factorial of the parameter
	 */
	public static BigInteger factorial(final BigInteger n) {
		return Utility.product(BigInteger.valueOf(2), n);
	}

	/**
	 * Compute the factorial of a long integer.
	 * @param n Number of which to compute the factorial
	 * @return Factorial of the parameter
	 */
	public static long factorial(final long n) {
		return Utility.product(2, n);
	}

	public static <E> String join(final List<? extends E> list,
			final CharSequence delim) {
		final StringBuffer b = new StringBuffer();
		boolean skip = true;
		for (final E e : list) {
			if (skip) {
				skip = false;
			} else {
				b.append(delim);
			}
			b.append(e);
		}
		return b.toString();
	}

	public static void logException(final Exception ex) {
		System.err.println("Caught exception: " + ex.getMessage() + "\n"
				+ Utility.stackTraceToString(ex.getStackTrace()));
	}

	public static <E extends Comparable<E>> List<E> lowestN(final List<E> list,
			final int n) {
		return Utility.lowestN(list, n, null);
	}

	public static <E extends Comparable<E>> List<E> lowestN(final List<E> list,
			final int n, final Comparator<E> c) {
		if (list.size() <= n) {
			return new ArrayList<E>(list);
		}
		final List<E> lowest = new ArrayList<E>(n);
		int maxindex = 0;
		for (final E o : list) {
			if (lowest.size() < n) {
				lowest.add(o);
				if (lowest.size() == n) {
					maxindex = Utility.maxIndex(lowest, c);
				} else {
					maxindex++;
				}
				continue;
			}
			final E max = lowest.get(maxindex);
			if (c == null) {
				if (o.compareTo(max) < 0) {
					lowest.set(maxindex, o);
					maxindex = Utility.maxIndex(lowest, c);
				}
			} else {
				if (c.compare(o, max) < 0) {
					// indices[maxindex] = i;
					lowest.set(maxindex, o);
					maxindex = Utility.maxIndex(lowest, c);
				}
			}
		}
		return lowest;
	}

	public static <E extends Comparable<E>> int maxIndex(final List<E> list) {
		return Utility.maxIndex(list, null);
	}

	public static <E extends Comparable<E>> int maxIndex(final List<E> list,
			final Comparator<E> c) {
		int maxIndex = -1;
		E maxObj = null;
		int i = 0;
		for (final E o : list) {
			if (maxObj == null) {
				maxIndex = i;
				maxObj = o;
				i++;
				continue;
			}
			if (c == null) {
				if (o.compareTo(maxObj) > 0) {
					maxIndex = i;
					maxObj = o;
				}
			} else {
				if (c.compare(o, maxObj) > 0) {
					maxIndex = i;
					maxObj = o;
				}
			}
			++i;
		}
		return maxIndex;
	}

	public static String padLeft(final String s, final int len) {
		final StringBuffer b = new StringBuffer(len);
		b.append(Utility.chrdup(' ', len - s.length()));
		b.append(s);
		return b.toString();
	}

	public static String padRight(final String s, final int len) {
		final StringBuffer b = new StringBuffer(len);
		b.append(s);
		b.append(Utility.chrdup(' ', len - s.length()));
		return b.toString();
	}

	public static double pchisq(final double chisq, final int df) {
		if (chisq == 0) {
			return 1;
		}
		return 1 - APStat.gammad(chisq / 2.0, df / 2.0);
	}

	/**
	 * Compute the power mean of an array of doubles for a given power (p). Many means are special cases of the power mean: Harmonic Mean => p
	 * = -1, Geometric Mean => p = 0, Arithmetic Mean => p = 1, Root-Mean-Square => p = 2.
	 * @param p Power of the mean
	 * @param x Array of doubles for which to compute the power mean
	 * @return Power mean of input values
	 */
	public static double powerMean(final double p, final double[] x) {
		double sum = 0;
		for (int i = 0; i < x.length; ++i) {
			sum += Math.pow(x[i], p);
		}
		return Math.pow(sum / x.length, 1.0 / p);
	}

	/**
	 * Compute the product of all integers in the range [min,max].
	 * @param min First number to include in product
	 * @param max Last number to include in product
	 * @return Product of all integers in range.
	 */
	public static BigInteger product(final BigInteger min, final BigInteger max) {
		BigInteger ret = BigInteger.ONE;
		for (BigInteger i = min; i.compareTo(max) <= 0; i = i.add(BigInteger.ONE)) {
			ret = ret.multiply(i);
		}
		return ret;
	}

	/**
	 * Compute the product of all integers in the range [min,max].
	 * @param min First number to include in product
	 * @param max Last number to include in product
	 * @return Product of all integers in range.
	 */
	public static long product(final long min, final long max) {
		long ret = 1;
		for (long i = min; i <= max; ++i) {
			ret *= i;
		}
		return ret;
	}

	public static String stackTraceToString(final StackTraceElement[] stackTrace) {
		final StringBuilder sb = new StringBuilder();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(stackTraceElement);
		}
		return sb.toString();
	}

	/**
	 * Prevent the Utility class from being instantiated.
	 */
	private Utility() {
	}
}
