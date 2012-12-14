package org.epistasis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

/**
 * SURF is very similar to reliefF except that instead of getting a fixed number of neighbors it gets all neighbors that are within a
 * threshold
 * @author pandrews
 */
public class SURFAttributeScorer extends ReliefFAttributeScorer {
	public SURFAttributeScorer(final Dataset data, final int numSamples,
			final Random random, final boolean parallel) {
		this(data, random, parallel, null);
	}

	public SURFAttributeScorer(final Dataset data, final Random random,
			final boolean parallel, final Runnable onIncrementProgress) {
		super(data, data.getRows(), 0, random, parallel, onIncrementProgress);
	}

	@Override
	protected Map<Byte, List<Integer>> computeNeighborhood(
			final Integer instanceIndex) {
		// System.out.println("ComputeNeighborhood for instance index: "
		// + instanceIndex + " using average " + averageDistance);
		final Map<Byte, List<Integer>> neighborhood = new HashMap<Byte, List<Integer>>();
		for (final Map.Entry<Byte, List<Integer>> entry : instByClass.entrySet()) {
			final List<Integer> closeNeighbors = new ArrayList<Integer>();
			neighborhood.put(entry.getKey(), closeNeighbors);
			for (final Integer comparisonInstanceIndex : entry.getValue()) {
				// if idx > neighborIndex can skip since we will check the relation since all
				// we sample all rows
				if (instanceIndex < comparisonInstanceIndex) {
					final double diff = distance(instanceIndex, comparisonInstanceIndex);
					if (diff < averageDistance) {
						closeNeighbors.add(comparisonInstanceIndex);
					}
				}
			} // end status instances loop
		} // end status look
		// System.out.print(averageDistance + ": For instanceIndex: " + instanceIndex
		// + " the number of each:");
		// for (final Map.Entry<Byte, List<Integer>> entry : neighborhood.entrySet()) {
		// System.out.print(" class : " + entry.getKey() + " #: "
		// + entry.getValue().size());
		// }
		// System.out.println();
		return neighborhood;
	} // end computeNeighborhood()

	@Override
	String getFilterName() {
		return "SURF";
	}

	@Override
	int getNormalizationFactor() {
		return 1;
	}
} // end SURFAttributeScorer
