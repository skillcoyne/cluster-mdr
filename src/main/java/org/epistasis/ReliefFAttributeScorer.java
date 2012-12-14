package org.epistasis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.ConfusionMatrix;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class ReliefFAttributeScorer extends ReliefFamilyAttributeScorer {
	private final static boolean computeAttributeCoOccurrences = false;
	private int[][][] attributesCoOccurrences;
	private final static Comparator<Pair<Float, String>> comboComparator = new Comparator<Pair<Float, String>>() {
		public int compare(final Pair<Float, String> o1,
				final Pair<Float, String> o2) {
			int compareVal = Float.compare(o2.getFirst(), o1.getFirst());
			if ((compareVal == 0) && (o1.getSecond() != null)) {
				compareVal = o1.getSecond().compareTo(o2.getSecond());
			}
			return compareVal;
		}
	};

	public ReliefFAttributeScorer(final Dataset data, final int m, final int k,
			final Random rnd, final boolean parallel) {
		this(data, m, k, rnd, parallel, null);
	}

	public ReliefFAttributeScorer(final Dataset data, final int m, final int k,
			final Random rnd, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, m, k, rnd, parallel, onIncrementProgress);
	}

	private double[] calculateWeights(final int numStatusClasses,
			final int normalizationFactor, final boolean needsReweighting,
			final float[] totalAttributeSameClassSums,
			final float[] totalAttributeDifferentClassSums) {
		final double[] weights = new double[totalAttributeSameClassSums.length];
		for (int attribute1Index = 0; attribute1Index < totalAttributeSameClassSums.length; ++attribute1Index) {
			if (needsReweighting) {
				// for polytomy, cannot assume that number of classes is two so there could be more than one status that was
				// considered a miss
				final float adjustedMissSums = totalAttributeDifferentClassSums[attribute1Index]
						/ (numStatusClasses - 1);
				weights[attribute1Index] = (adjustedMissSums - totalAttributeSameClassSums[attribute1Index])
						/ normalizationFactor;
			} else {
				weights[attribute1Index] = totalAttributeDifferentClassSums[attribute1Index]
						- totalAttributeSameClassSums[attribute1Index];
			}
		}
		return weights;
	} // end calculateWeights()

	@Override
	protected Map<Byte, List<Integer>> computeNeighborhood(final Integer idx) {
		final Map<Byte, List<Integer>> neighborhood = new HashMap<Byte, List<Integer>>();
		final Comparator<Integer> cmp = new InstanceDistanceComparator(idx);
		for (final Map.Entry<Byte, List<Integer>> entry : instByClass.entrySet()) {
			final byte status = entry.getKey();
			final List<Integer> list = new ArrayList<Integer>(entry.getValue());
			// make sure we don't pass ourself to nearest neighbors (will do nothing if not in list)
			while (list.contains(idx)) {
				list.remove(idx);
			}
			neighborhood.put(status, Utility.lowestN(list, getK(), cmp));
		}
		return neighborhood;
	}

	@Override
	String getFilterName() {
		return "RELIEFF";
	}

	@Override
	int getNormalizationFactor() {
		return getM() * getK();
	}

	@Override
	protected double[] postProcess() {
		double[] weights = null;
		double[] differenceWeights = null;
		// double[] agreementWeights = null;
		// set the reliefFScores
		// find the number of classes
		final int numStatusClasses = getData().getStatusCounts().length;
		final int normalizationFactor = getNormalizationFactor();
		final boolean needsReweighting = (numStatusClasses != 2)
				|| (normalizationFactor != 1);
		// final float averageNumberOfNeighbors = denom / getM();
		differenceWeights = calculateWeights(numStatusClasses, normalizationFactor,
				needsReweighting, totalAttributeSameClassDifferenceSums,
				totalAttributeDifferentClassDifferenceSums);
		// printOutFirstTwoAttributeRanks("differenceWeights: ", differenceWeights);
		// agreementWeights = calculateWeights(numStatusClasses, normalizationFactor,
		// needsReweighting, totalAttributeDifferentClassAgreementSums,
		// totalAttributeSameClassAgreementSums);
		// printOutFirstTwoAttributeRanks(" agreementWeights: ", agreementWeights);
		weights = differenceWeights;
		// weights = agreementWeights;
		/*
		 * 
		 * 
		 * 
		 * 
		 */
		if (ReliefFAttributeScorer.computeAttributeCoOccurrences) {
			// print header row of attribute names
			for (int attributeIndex = 0; attributeIndex < attributesCoOccurrences.length; ++attributeIndex) {
				System.out.print("\t" + getData().getLabels().get(attributeIndex));
			}
			System.out.println();
			for (int attribute1Index = 0; attribute1Index < attributesCoOccurrences.length; ++attribute1Index) {
				System.out.print(getData().getLabels().get(attribute1Index) + "\t");
				final int[][] attributeCoOccurrences = attributesCoOccurrences[attribute1Index];
				for (int attribute2Index = 0; attribute2Index < attribute1Index; ++attribute2Index) {
					final int[] hitsAndMissSumsForCoOccurrence = attributeCoOccurrences[attribute2Index];
					System.out.print("[" + getData().getLabels().get(attribute1Index)
							+ "+" + getData().getLabels().get(attribute2Index) + "="
							+ hitsAndMissSumsForCoOccurrence[0] + ","
							+ hitsAndMissSumsForCoOccurrence[1] + "}\t");
				}
				System.out.println();
			}
		}
		/*
		 * 
		 * 
		 * 
		 * 
		 */
		final boolean doExperimentalAttributeTesting = false;
		if (doExperimentalAttributeTesting) {
			final boolean doThreeWay = false && (getData().getCols() < 100);
			final AmbiguousCellStatus tiePriority = Main.defaultAmbiguousCellStatus;
			final PriorityList<Pair<Float, String>> twoWayCombos = new PriorityList<Pair<Float, String>>(
					10, ReliefFAttributeScorer.comboComparator);
			final PriorityList<Pair<Float, String>> threeWayCombos = new PriorityList<Pair<Float, String>>(
					2000, ReliefFAttributeScorer.comboComparator);
			for (int attribute1Index = 0; attribute1Index < totalAttributeSameClassDifferenceSums.length; ++attribute1Index) {
				for (int attribute2Index = 0; attribute2Index < attribute1Index; ++attribute2Index) {
					final float sumTwoWay = ((totalAttributeSameClassDifferenceSums[attribute1Index] + totalAttributeSameClassDifferenceSums[attribute2Index]) - (totalAttributeDifferentClassDifferenceSums[attribute1Index] + totalAttributeDifferentClassDifferenceSums[attribute2Index]))
							/ normalizationFactor;
					final Pair<Float, String> twoWayPair = new Pair<Float, String>(
							sumTwoWay / 2f, attribute1Index + "|" + attribute2Index);
					twoWayCombos.add(twoWayPair);
					if (doThreeWay) {
						for (int attribute3Index = attribute2Index + 1; attribute3Index < totalAttributeSameClassDifferenceSums.length; ++attribute3Index) {
							final float sumThreeWay = ((totalAttributeSameClassDifferenceSums[attribute1Index]
									+ totalAttributeSameClassDifferenceSums[attribute2Index] + totalAttributeSameClassDifferenceSums[attribute3Index]) - (totalAttributeDifferentClassDifferenceSums[attribute1Index]
									+ totalAttributeDifferentClassDifferenceSums[attribute2Index] + totalAttributeDifferentClassDifferenceSums[attribute3Index]))
									/ normalizationFactor;
							final Pair<Float, String> threeWayPair = new Pair<Float, String>(
									sumThreeWay / 3f, attribute1Index + "|" + attribute2Index
											+ "|" + attribute3Index);
							threeWayCombos.add(threeWayPair);
						} // for attribute3 loop
					}
				} // for attribute2 loop
				// System.out.println();
			} // for attribute1 loop
			int lineCtr = 0;
			System.out.println("****TWO WAY COMBOS\n#\treliefF avg.\tcombination");
			for (final Pair<Float, String> sumAndIdentifier : twoWayCombos) {
				final String[] attributes = sumAndIdentifier.getSecond().split("[|]");
				final int attribute1Index = Integer.parseInt(attributes[0]);
				final String attribute1Identifier = getData().getLabels().get(
						attribute1Index)
						+ "("
						+ totalAttributeSameClassDifferenceSums[attribute1Index]
						+ ","
						+ totalAttributeDifferentClassDifferenceSums[attribute1Index]
						+ "|" + differenceWeights[attribute1Index] + ")";
				final int attribute2Index = Integer.parseInt(attributes[1]);
				final String attribute2Identifier = getData().getLabels().get(
						attribute2Index)
						+ "("
						+ totalAttributeSameClassDifferenceSums[attribute2Index]
						+ ","
						+ totalAttributeDifferentClassDifferenceSums[attribute2Index]
						+ "|" + differenceWeights[attribute2Index] + ")";
				final Model model = new Model(new AttributeCombination(
						Arrays.asList(new Integer[] { attribute1Index, attribute2Index }),
						getData().getLabels()), tiePriority);
				model.buildCounts(getData());
				model.buildStatuses(getData(), getData().getStatusCounts());
				final ConfusionMatrix training = model.test(getData());
				final String identifierTwoWay = "["
						+ attribute1Identifier
						+ "+"
						+ attribute2Identifier
						+ "="
						+ (totalAttributeSameClassDifferenceSums[attribute1Index] + totalAttributeSameClassDifferenceSums[attribute2Index])
						+ ","
						+ (totalAttributeDifferentClassDifferenceSums[attribute1Index] + totalAttributeDifferentClassDifferenceSums[attribute2Index])
						+ "] balancedAccuracy: " + training.getBalancedAccuracy();
				System.out.println(++lineCtr + "\t" + sumAndIdentifier.getFirst()
						+ "\t" + identifierTwoWay);
			}
			if (doThreeWay) {
				lineCtr = 0;
				System.out
						.println("****THREE WAY COMBOS\n#\treliefF avg.\tcombination");
				for (final Pair<Float, String> sumAndIdentifier : threeWayCombos) {
					final String[] attributes = sumAndIdentifier.getSecond().split("[|]");
					final int attribute1Index = Integer.parseInt(attributes[0]);
					final String attribute1Identifier = getData().getLabels().get(
							attribute1Index)
							+ "("
							+ totalAttributeSameClassDifferenceSums[attribute1Index]
							+ ","
							+ totalAttributeDifferentClassDifferenceSums[attribute1Index]
							+ "|" + differenceWeights[attribute1Index] + ")";
					final int attribute2Index = Integer.parseInt(attributes[1]);
					final String attribute2Identifier = getData().getLabels().get(
							attribute2Index)
							+ "("
							+ totalAttributeSameClassDifferenceSums[attribute2Index]
							+ ","
							+ totalAttributeDifferentClassDifferenceSums[attribute2Index]
							+ "|" + differenceWeights[attribute2Index] + ")";
					final int attribute3Index = Integer.parseInt(attributes[2]);
					final String attribute3Identifier = getData().getLabels().get(
							attribute3Index)
							+ "("
							+ totalAttributeSameClassDifferenceSums[attribute3Index]
							+ ","
							+ totalAttributeDifferentClassDifferenceSums[attribute3Index]
							+ "|" + differenceWeights[attribute3Index] + ")";
					final Model model = new Model(new AttributeCombination(
							Arrays.asList(new Integer[] { attribute1Index, attribute2Index,
									attribute3Index }), getData().getLabels()), tiePriority);
					model.buildCounts(getData());
					model.buildStatuses(getData(), getData().getStatusCounts());
					final ConfusionMatrix training = model.test(getData());
					final String identifierThreeWay = "["
							+ attribute1Identifier
							+ "+"
							+ attribute2Identifier
							+ attribute3Identifier
							+ "="
							+ (totalAttributeSameClassDifferenceSums[attribute1Index]
									+ totalAttributeSameClassDifferenceSums[attribute2Index] + totalAttributeSameClassDifferenceSums[attribute3Index])
							+ ","
							+ (totalAttributeDifferentClassDifferenceSums[attribute1Index]
									+ totalAttributeDifferentClassDifferenceSums[attribute2Index] + totalAttributeDifferentClassDifferenceSums[attribute3Index])
							+ "] balancedAccuracy: " + training.getBalancedAccuracy();
					System.out.println(++lineCtr + "\t" + sumAndIdentifier.getFirst()
							+ "\t" + identifierThreeWay);
				}
			}
		}
		return weights;
	} // end postProcess()

	@Override
	protected void preProcess() {
		if (ReliefFAttributeScorer.computeAttributeCoOccurrences) {
			attributesCoOccurrences = new int[numAttributes][][];
			for (int attribute1Index = 0; attribute1Index < attributesCoOccurrences.length; ++attribute1Index) {
				final int[][] attribute1CoOccurrences = new int[attribute1Index][];
				attributesCoOccurrences[attribute1Index] = attribute1CoOccurrences;
				for (int attribute2Index = 0; attribute2Index < attribute1Index; ++attribute2Index) {
					attributesCoOccurrences[attribute1Index][attribute2Index] = new int[2]; // hitsum, misssum
				}
			}
		}
		totalAttributeSameClassDifferenceSums = new float[numAttributes];
		totalAttributeSameClassAgreementSums = new float[numAttributes];
		totalAttributeDifferentClassDifferenceSums = new float[numAttributes];
		totalAttributeDifferentClassAgreementSums = new float[numAttributes];
		// diffWeights = new int[numAttributes];
	}

	@Override
	/*
	 * This implementation can handle polytomy
	 */
	protected void processInstance(final int idx) {
		final Map<Byte, List<Integer>> neighborhoodForEachStatus = computeNeighborhood(idx);
		// is the current instance a case or a control
		final byte instanceStatus = getData().getRawDatum(idx, numAttributes);
		final long[] neighborhoodAttributeSameClassDifferenceSums = new long[numAttributes];
		final long[] neighborhoodAttributeSameClassAgreementSums = new long[numAttributes];
		final long[] neighborhoodAttributeDifferentClassDifferenceSums = new long[numAttributes];
		final long[] neighborhoodAttributeDifferentClassAgreementSums = new long[numAttributes];
		for (int attributeIndex = 0; attributeIndex < numAttributes; ++attributeIndex) {
			for (final Map.Entry<Byte, List<Integer>> statusAndNeighborhood : neighborhoodForEachStatus
					.entrySet()) {
				final byte neighborStatus = statusAndNeighborhood.getKey();
				final List<Integer> neighborhoodIndices = statusAndNeighborhood
						.getValue();
				float instanceIncrement = 1.0f;
				if (statusProportionalWeights != null) {
					if (instanceStatus == neighborStatus) {
						instanceIncrement = statusProportionalWeights[neighborStatus];
					} else {
						// get multiplier for heterozygous case
						instanceIncrement = statusProportionalWeights[2];
					}
				}
				int numNeighborsWithDifferentAttributeValue = 0;
				int numNeighborsWithSameAttributeValue = 0;
				for (final int neighborIndex : neighborhoodIndices) {
					if (getData().getRawDatum(idx, attributeIndex) == getData()
							.getRawDatum(neighborIndex, attributeIndex)) {
						numNeighborsWithSameAttributeValue += instanceIncrement;
					} else {
						numNeighborsWithDifferentAttributeValue += instanceIncrement;
					}
				} // end neighbor loop
				// if the current neighbor group status == same as this instance's status
				if (neighborStatus == instanceStatus) {
					neighborhoodAttributeSameClassDifferenceSums[attributeIndex] += numNeighborsWithDifferentAttributeValue;
					neighborhoodAttributeSameClassAgreementSums[attributeIndex] += numNeighborsWithSameAttributeValue;
				} else {
					neighborhoodAttributeDifferentClassDifferenceSums[attributeIndex] += numNeighborsWithDifferentAttributeValue;
					neighborhoodAttributeDifferentClassAgreementSums[attributeIndex] += numNeighborsWithSameAttributeValue;
				}
			} // end status loop
		} // end attribute loop
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
		if (ReliefFAttributeScorer.computeAttributeCoOccurrences) {
			for (int attribute1Index = 0; attribute1Index < attributesCoOccurrences.length; ++attribute1Index) {
				final int[][] attributeCoOccurrences = attributesCoOccurrences[attribute1Index];
				for (int attribute2Index = 0; attribute2Index < attribute1Index; ++attribute2Index) {
					final int[] hitsAndMissSumsForCoOccurrence = attributeCoOccurrences[attribute2Index];
					if (attribute2Index == 9) {
						System.out
								.println(neighborhoodAttributeSameClassDifferenceSums[attribute1Index]
										+ ","
										+ neighborhoodAttributeDifferentClassDifferenceSums[attribute1Index]
										+ ": Setting co-occurence of "
										+ getData().getLabels().get(attribute1Index)
										+ "+"
										+ getData().getLabels().get(attribute2Index)
										+ "="
										+ neighborhoodAttributeSameClassDifferenceSums[attribute2Index]
										+ "(+"
										+ hitsAndMissSumsForCoOccurrence[0]
										+ "),"
										+ neighborhoodAttributeDifferentClassDifferenceSums[attribute2Index]
										+ "(+" + hitsAndMissSumsForCoOccurrence[1] + ")");
					}
					hitsAndMissSumsForCoOccurrence[0] += neighborhoodAttributeSameClassDifferenceSums[attribute2Index];
					hitsAndMissSumsForCoOccurrence[1] += neighborhoodAttributeDifferentClassDifferenceSums[attribute2Index];
				} // loop attribute2
			} // loop attribute1
		} // end if computeAttributeCoOccurrences
	} // end processInstance()
} // end class
