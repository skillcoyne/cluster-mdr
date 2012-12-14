package org.epistasis;

import java.util.Arrays;
import java.util.Map;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

public abstract class AbstractAttributeScorer {
	private final Runnable onIncrementProgress;
	private final Dataset data;
	private double[] scores;
	protected final int numAttributes;

	public AbstractAttributeScorer(final Dataset data) {
		this(data, null);
	}

	public AbstractAttributeScorer(final Dataset data,
			final Runnable onIncrementProgress) {
		this.data = data;
		numAttributes = data.getCols() - 1;
		this.onIncrementProgress = onIncrementProgress;
	}

	protected abstract double[] computeScores();

	public double get(final int index) {
		return scores[index];
	}

	public abstract Map<String, String> getConfig();

	public Dataset getData() {
		return data;
	}

	public double[] getScores() {
		if (scores == null) {
			scores = computeScores();
		}
		return scores;
	}

	public abstract int getTotalProgress();

	protected void incrementProgress() {
		if (onIncrementProgress != null) {
			onIncrementProgress.run();
		}
	}

	/**
	 * vector of <double> output values of the transformed input double values to z-scores with mean=0, stddev=1
	 * @param originalScores
	 * @return
	 */
	public double[] makeZScores(final double[] originalScores) {
		// results array starts off all zeros via standard Java initialization
		final double[] zTransformedScores = new double[originalScores.length];
		double sum = 0;
		for (final double score : originalScores) {
			sum += score;
		}
		final double mean = sum / originalScores.length;
		double sumOfSquaredDistanceFromMean = 0.0;
		for (final double score : originalScores) {
			final double diff = score - mean;
			sumOfSquaredDistanceFromMean += diff * diff;
		}
		// if the variance is zero, then z-scores are equal to zero
		// which array has already been initialized to
		if (Double.compare(sumOfSquaredDistanceFromMean, 0.0) != 0) {
			final double variance = sumOfSquaredDistanceFromMean
					/ (originalScores.length - 1);
			final double stddev = Math.sqrt(variance);
			for (int index = 0; index < originalScores.length; ++index) {
				zTransformedScores[index] = (originalScores[index] - mean) / stddev;
				if (Double.isNaN(zTransformedScores[index])) {
					System.out.println("generated Nan for attribute at index " + index
							+ " with original score " + originalScores[index] + " mean: "
							+ mean + " stddev: " + stddev + " original scores: "
							+ Arrays.toString(originalScores));
				}
			}
		}
		return zTransformedScores;
	} // end zTransform

	public int size() {
		return scores == null ? 0 : scores.length;
	}
}
