package org.epistasis;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

/**
 * SURF is very similar to reliefF except that instead of getting a fixed number of neighbors it gets all neighbors that are within a
 * threshold
 * @author pandrews
 */
public class SURFStarAttributeScorer extends ReliefFAttributeScorer {
	public SURFStarAttributeScorer(final Dataset data, final Random random,
			final boolean parallel) {
		this(data, random, parallel, null);
	}

	public SURFStarAttributeScorer(final Dataset data, final Random random,
			final boolean parallel, final Runnable onIncrementProgress) {
		super(data, data.getRows(), 0, random, parallel, onIncrementProgress);
	}

	@Override
	protected Map<Byte, List<Integer>> computeNeighborhood(
			final Integer instanceIndex) {
		throw new RuntimeException(
				"ComputeNeighborhood should never be called for Surf*");
	} // end computeNeighborhood()

	@Override
	String getFilterName() {
		return "SURF*";
	} // end synchronized

	@Override
	int getNormalizationFactor() {
		return 1;
	}

	@Override
	protected void processInstance(final int idx) {
		final byte instanceStatus = getData().getRawDatum(idx, numAttributes);
		final long[] neighborhoodAttributeSameClassDifferenceSums = new long[numAttributes];
		final long[] neighborhoodAttributeSameClassAgreementSums = new long[numAttributes];
		final long[] neighborhoodAttributeDifferentClassDifferenceSums = new long[numAttributes];
		final long[] neighborhoodAttributeDifferentClassAgreementSums = new long[numAttributes];
		for (final Map.Entry<Byte, List<Integer>> statusAndNeighborhood : instByClass
				.entrySet()) {
			final byte neighborStatus = statusAndNeighborhood.getKey();
			final boolean isSameClass = (neighborStatus == instanceStatus);
			final List<Integer> neighborhoodIndices = statusAndNeighborhood
					.getValue();
			for (final int neighborIndex : neighborhoodIndices) {
				// if idx > neighborIndex can skip since we will check the relation since all
				// we sample all rows
				if (idx < neighborIndex) {
					final double distance = distance(idx, neighborIndex);
					final int comparisonResult = Double
							.compare(distance, averageDistance);
					if (comparisonResult != 0) {
						final boolean closerThanAverage = comparisonResult < 0;
						for (int attributeIndex = 0; attributeIndex < numAttributes; ++attributeIndex) {
							final boolean attributeHasSameValue = getData().getRawDatum(idx,
									attributeIndex) == getData().getRawDatum(neighborIndex,
									attributeIndex);
							// basic idea is that when distance of neighbor greater than average distance
							// reverse the weighting
							final int delta = closerThanAverage ? 1 : -1;
							if (!attributeHasSameValue) {
								if (isSameClass) {
									neighborhoodAttributeSameClassDifferenceSums[attributeIndex] += delta;
								} else {
									neighborhoodAttributeDifferentClassDifferenceSums[attributeIndex] += delta;
								}
							} else {
								if (isSameClass) {
									neighborhoodAttributeSameClassAgreementSums[attributeIndex] += delta;
								} else {
									neighborhoodAttributeDifferentClassAgreementSums[attributeIndex] += delta;
								}
							}
						} // end attribute loop
					} // end if distance not exactly equal to average distance
				} // end if low enough index that it wasn't skipped
			} // end neighbor loop
		} // end status loop
		// totalAttributeSameClassDifferenceSums and totalAttributeDifferentClassDifferenceSums are member variables written to simultaneously
		// by many threads so need to synchronize
		synchronized (this) {
			for (int attributeIndex = 0; attributeIndex < totalAttributeSameClassDifferenceSums.length; ++attributeIndex) {
				totalAttributeSameClassDifferenceSums[attributeIndex] += neighborhoodAttributeSameClassDifferenceSums[attributeIndex];
				totalAttributeSameClassAgreementSums[attributeIndex] += neighborhoodAttributeSameClassAgreementSums[attributeIndex];
				totalAttributeDifferentClassDifferenceSums[attributeIndex] += neighborhoodAttributeDifferentClassDifferenceSums[attributeIndex];
				totalAttributeDifferentClassAgreementSums[attributeIndex] += neighborhoodAttributeDifferentClassAgreementSums[attributeIndex];
				// diffWeights[attributeIndex] = (totalAttributeSameClassDifferenceSums[attributeIndex] -
				// totalAttributeDifferentClassDifferenceSums[attributeIndex]);
			} // end for attributes
		} // end synchronized
	} // end processInstance
} // end SURFAttributeScorer
