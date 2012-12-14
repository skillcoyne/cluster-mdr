package org.epistasis;

import java.util.Map;
import java.util.TreeMap;

public class Entropy {
	public static double getConditionalEntropy(final byte[] attribute,
			final byte[] given) {
		final Map<Byte, Map<Byte, Integer>> counts = new TreeMap<Byte, Map<Byte, Integer>>();
		double e = 0;
		for (int index = 0; index < attribute.length; ++index) {
			final byte attrKey = attribute[index];
			final byte givenKey = given[index];
			Map<Byte, Integer> subcounts = counts.get(givenKey);
			if (subcounts == null) {
				subcounts = new TreeMap<Byte, Integer>();
			}
			Integer count = subcounts.get(attrKey);
			if (count == null) {
				count = 0;
			}
			subcounts.put(attrKey, count + 1);
			counts.put(givenKey, subcounts);
		}
		for (final Map<Byte, Integer> subcounts : counts.values()) {
			int subtotal = 0;
			double sube = 0;
			for (final int count : subcounts.values()) {
				subtotal += count;
			}
			for (final int count : subcounts.values()) {
				final double genotypeProbability = count / (double) subtotal;
				sube -= genotypeProbability * Math.log(genotypeProbability)
						/ Math.log(2);
			}
			e += subtotal * sube / given.length;
		}
		return e;
	}

	public static double getEntropy(final byte[] columnData) {
		final Map<Byte, Integer> counts = new TreeMap<Byte, Integer>();
		double entropy = 0;
		for (final byte levelIndex : columnData) {
			Integer count = counts.get(levelIndex);
			if (count == null) {
				count = 0;
			}
			counts.put(levelIndex, count + 1);
		}
		for (final int count : counts.values()) {
			final double genotypeProbability = count / (double) columnData.length;
			entropy -= genotypeProbability * Math.log(genotypeProbability)
					/ Math.log(2);
		}
		return entropy;
	}
}
