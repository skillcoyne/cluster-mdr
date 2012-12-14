package org.epistasis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.ExpertKnowledge;
import org.epistasis.combinatoric.mdr.newengine.Dataset;

public abstract class ReliefFamilyAttributeScorer extends
		AbstractAttributeScorer {
	private Map<String, String> config = new TreeMap<String, String>();
	private Random rnd;
	// m is number of samples. Recommended to be the entire dataset
	private int m;
	// k is the number of neighbors
	private int k;
	private boolean parallel;
	private List<Integer> samples = null;
	protected SortedMap<Byte, List<Integer>> instByClass;
	// private double[] weights;
	private double[] diffWeights = null;
	private final static boolean preComputeDistances = true;
	private double[][] distances;
	protected double averageDistance;
	// where I use 'SameClass' other relief people use 'hits'
	protected float[] totalAttributeSameClassDifferenceSums;
	protected float[] totalAttributeSameClassAgreementSums;
	// where I use 'DifferentClass' other relief people use 'misses'
	protected float[] totalAttributeDifferentClassDifferenceSums;
	protected float[] totalAttributeDifferentClassAgreementSums;
	protected float[] statusProportionalWeights = null;

	// private final static Comparator<Pair<Float, String>> doubleStringComparator = new Comparator<Pair<Float, String>>() {
	// public int compare(final Pair<Float, String> o1,
	// final Pair<Float, String> o2) {
	// int compareVal = Float.compare(o2.getFirst(), o1.getFirst());
	// if ((compareVal == 0) && (o1.getSecond() != null)) {
	// compareVal = o1.getSecond().compareTo(o2.getSecond());
	// }
	// return compareVal;
	// }
	// };
	public ReliefFamilyAttributeScorer(final Dataset data, final int m,
			final int k, final Random rnd, final boolean parallel) {
		super(data);
		init(data, m, k, rnd, parallel);
	}

	public ReliefFamilyAttributeScorer(final Dataset data, final int m,
			final int k, final Random rnd, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, onIncrementProgress);
		init(data, m, k, rnd, parallel);
	}

	private void calculateInstanceDifferences() {
		final Dataset data = getData();
		final int numRows = data.getRows();
		final int numColumns = data.getCols() - 1;
		double aggregateDistances = 0;
		long numComparisons = 0;
		distances = new double[numRows][];
		final int processorsToUse = parallel ? Runtime.getRuntime()
				.availableProcessors() : 1;
		if (processorsToUse > 1) {
			final ProducerConsumerThread<Integer> distanceThread = new ProducerConsumerThread<Integer>();
			distanceThread.setProducer(new DistanceProducer());
			for (int i = 0; i < (processorsToUse); ++i) {
				distanceThread.addConsumer(new DistanceConsumer());
			}
			distanceThread.run();
			if (!Thread.interrupted()) {
				for (int instance1Index = 0; instance1Index < numRows; ++instance1Index) {
					final double[] instance1Distances = distances[instance1Index];
					for (int instance2Index = 0; instance2Index < instance1Distances.length; ++instance2Index) {
						aggregateDistances += instance1Distances[instance2Index];
						++numComparisons;
					}// end instance2 loop
				} // end instance1 loop
			}
		} else {
			for (int instance1Index = 0; instance1Index < numRows; ++instance1Index) {
				final double[] instance1Distances = new double[instance1Index];
				distances[instance1Index] = instance1Distances;
				final byte[] instance1Data = data.getRawRowData(instance1Index);
				for (int instance2Index = 0; instance2Index < instance1Index; ++instance2Index) {
					final byte[] instance2Data = data.getRawRowData(instance2Index);
					double sum = 0;
					for (int columnIndex = 0; columnIndex < numColumns; ++columnIndex) {
						sum += (instance1Data[columnIndex] == instance2Data[columnIndex]) ? 0
								: ((diffWeights == null) ? 1 : diffWeights[columnIndex]);
					}
					instance1Distances[instance2Index] = sum;
					aggregateDistances += sum;
					++numComparisons;
				}// end instance2 loop
					// System.out.println("Finished row " + (instance1Index + 1) + " distances.");
			} // end instance1 loop
		}
		averageDistance = aggregateDistances / numComparisons;
		// System.out.println(getClass().getSimpleName() + ": Finished calculateInstanceDifferences with " + data.getRows() + " instances with "
		// + (data.getCols() - 1)
		// + " attributes. Average distance: " + averageDistance);
	} // end calculateInstanceDifferences()

	private void computeInstByClass() {
		instByClass = new TreeMap<Byte, List<Integer>>();
		for (int i = 0; i < getData().getRows(); ++i) {
			final byte status = getData().getRawDatum(i, getData().getCols() - 1);
			List<Integer> list = instByClass.get(status);
			if (list == null) {
				list = new ArrayList<Integer>();
				instByClass.put(status, list);
			}
			list.add(i);
		}
	}

	abstract Map<Byte, List<Integer>> computeNeighborhood(final Integer idx);

	@Override
	public double[] computeScores() {
		// long startTime = System.currentTimeMillis();
		// if (Main.isExperimental) {
		// final PriorityList<Pair<Float, String>> topInterestingGenotypeCombinations = new PriorityList<Pair<Float, String>>(
		// 20, ReliefFamilyAttributeScorer.doubleStringComparator);
		// final float[][] attributeStatsResults = getData()
		// .calculateAttributeStats(topInterestingGenotypeCombinations);
		// final float[] totalAttributeDifferences = attributeStatsResults[0];
		// System.out
		// .println((System.currentTimeMillis() - startTime)
		// + " Finished getData().calculateAttributeStats(). Ratio accepted: "
		// + (topInterestingGenotypeCombinations
		// .getNumberOfSuccessfulAdditions() / (double) topInterestingGenotypeCombinations
		// .getNumberOfAttemptedAdditions())
		// + ". Number of attemptedAdds = "
		// + topInterestingGenotypeCombinations
		// .getNumberOfAttemptedAdditions()
		// + ", successful adds: "
		// + topInterestingGenotypeCombinations
		// .getNumberOfSuccessfulAdditions());
		// System.out.println("**** Top "
		// + topInterestingGenotypeCombinations.getCapacity()
		// + " improbable 2-way genotype co-occurrences");
		// int lineCtr = 0;
		// final List<Byte> tieStatus = Arrays.asList(getData()
		// .getAffectedStatus(), getData().getUnaffectedStatus());
		// final int[] skipped = new int[getData().getStatusCounts().length];
		// for (final Pair<Float, String> probabilityPair : topInterestingGenotypeCombinations) {
		// String comboString = probabilityPair.getSecond().substring(
		// "status: 1 ".length());
		// comboString = comboString.substring(0, comboString.indexOf(' '))
		// .replace('-', ' ');
		// final Model model = new Model(new AttributeCombination(comboString,
		// getData().getLabels()), tieStatus);
		// model.buildCounts(getData(), skipped);
		// model.buildStatuses(getData(), getData().getStatusCounts(), skipped);
		// final ConfusionMatrix training = model.test(getData());
		// System.out.println(++lineCtr + "\t" + probabilityPair.getFirst() + "\t"
		// + probabilityPair.getSecond() + " training balanced accuracy: "
		// + training.getBalancedAccuracy());
		// }
		// int x0_rank = 1;
		// int x1_rank = 1;
		// for (int scoreIndex = 1; scoreIndex < totalAttributeDifferences.length; ++scoreIndex) {
		// if (totalAttributeDifferences[scoreIndex] > totalAttributeDifferences[0]) {
		// ++x0_rank;
		// }
		// if (totalAttributeDifferences[scoreIndex] > totalAttributeDifferences[1]) {
		// ++x1_rank;
		// }
		// }
		// System.out.println("totalAttributeDifferences: x0 value: "
		// + totalAttributeDifferences[0] + ", rank: " + x0_rank + " x1 value: "
		// + totalAttributeDifferences[1] + ", rank: " + x1_rank);
		// } // end if experimental
		// if (Main.isExperimental) {
		// Console.reliefFWeightedDistanceIterations = 1;
		// Console.reliefFWeightedDistanceMethod = WeightScheme.PROPORTIONAL;
		// Console.reliefFWeightedScalingMethod = ScalingMethod.LINEAR;
		// Console.reliefFWeightedScalingParameter = 0.95;
		// System.out
		// .println("DEBUGGING: FORCED settings\nConsole.reliefFWeightedDistanceIterations: "
		// + Console.reliefFWeightedDistanceIterations
		// + "\nConsole.reliefFWeightedDistanceMethod: "
		// + Console.reliefFWeightedDistanceMethod
		// + "\nConsole.reliefFWeightedScalingMethod: "
		// + Console.reliefFWeightedScalingMethod
		// + "\nConsole.reliefFWeightedScalingParameter: "
		// + Console.reliefFWeightedScalingParameter);
		// }
		double[] scores = null;
		// final boolean testAllRebalancingMethods = false;
		// if (testAllRebalancingMethods) {
		// System.out.println("\n\n===============\n# samples (m): " + m
		// + " neighbors (k): " + k);
		// for (final Console.ReliefFRebalancingMethod reliefFRebalancingMethod : Console.ReliefFRebalancingMethod
		// .values()) {
		// Console.reliefFRebalancingMethod = reliefFRebalancingMethod;
		// final long startTime = System.currentTimeMillis();
		// scores = computeScores(Console.reliefFWeightedDistanceIterations);
		// String identifier = (System.currentTimeMillis() - startTime) + ": "
		// + reliefFRebalancingMethod;
		// printOutX0_X1Ranks(identifier, scores);
		// } // end for loop over all rebalancingMethods
		// } else {
		// Console.reliefFRebalancingMethod = ReliefFRebalancingMethod.DO_NOTHING;
		scores = computeScores(Console.reliefFWeightedDistanceIterations);
		// }
		// Console.reliefFRebalancingMethod = ReliefFRebalancingMethod.OVERSAMPLE_MINORITY_MULTIPLE_DATASETS;
		// final double[] partitionScores = computeScores(Console.reliefFWeightedDistanceIterations);
		// Console.reliefFRebalancingMethod = ReliefFRebalancingMethod.UNDERSAMPLE_MAJORITY;
		// final double[] undersamplingScores = computeScores(Console.reliefFWeightedDistanceIterations);
		// for (int index = 0; index < scores.length; ++index) {
		// System.out.println(index + " DO_NOTHING: " + scores[index]
		// + " versus partitionScores: " + partitionScores[index]
		// + " versus undersamplingScores: " + undersamplingScores[index]);
		// }
		// scores = partitionScores;
		// }
		return scores;
	}

	private double[] computeScores(final int numTimesToIterateWithDiffWeights) {
		double[] weights;
		computeInstByClass();
		if (ReliefFamilyAttributeScorer.preComputeDistances) {
			calculateInstanceDifferences();
			if (Thread.interrupted()) {
				return null;
			}
		}
		final int minorityStatusCount = getData().getMinorityStatusCount();
		final int majorityStatusCount = getData().getMajorityStatusCount();
		Console.ReliefFRebalancingMethod reliefFRebalancingMethod = Console.reliefFRebalancingMethod;
		// if dataset is balanced no special processing is needed
		if (minorityStatusCount == majorityStatusCount) {
			reliefFRebalancingMethod = Console.ReliefFRebalancingMethod.DO_NOTHING;
		}
		switch (reliefFRebalancingMethod) {
			case DO_NOTHING: {
				/*
				 * Prior to release MDR 2.0 beta 5 this was the only method -- no special handling of imbalanced datasets
				 */
				weights = computeScores2(numTimesToIterateWithDiffWeights);
				break;
			}
			case PROPORTIONAL_WEIGHTING: {
				final float majorityRatio = majorityStatusCount
						/ (float) (minorityStatusCount + majorityStatusCount);
				final float minorityRatio = 1.0f - majorityRatio;
				statusProportionalWeights = new float[3];
				statusProportionalWeights[getData().getMinorityStatus()] = 0.25f / (minorityRatio * minorityRatio);
				statusProportionalWeights[getData().getMajorityStatus()] = 0.25f / (majorityRatio * majorityRatio);
				statusProportionalWeights[2] = 0.50f / (2 * (minorityRatio * majorityRatio));
				weights = computeScores2(numTimesToIterateWithDiffWeights);
				break;
			}
			case OVERSAMPLE_MINORITY: {
				/*
				 * This effectively creates a larger dataset where samples of the minority class are used multiple times so that the dataset becomes
				 * balanced. For example if a dataset of 1000 had 700/300 cases/controls we would create a dataset of 700/700 by using instances of
				 * the minority class twice and then taking another random 100 instances to make a total of 700
				 */
				final byte minorityStatus = getData().getMinorityStatus();
				final List<Integer> minorityInstancesList = instByClass
						.get(minorityStatus);
				Collections.shuffle(minorityInstancesList, rnd);
				int instancesYetToBeAdded = majorityStatusCount - minorityStatusCount;
				do {
					if (instancesYetToBeAdded > minorityInstancesList.size()) {
						instancesYetToBeAdded -= minorityInstancesList.size();
						minorityInstancesList.addAll(minorityInstancesList);
					} else {
						minorityInstancesList.addAll(minorityInstancesList.subList(0,
								instancesYetToBeAdded));
						instancesYetToBeAdded -= instancesYetToBeAdded; // looks odd but logically correct and causes loop termination without need for
						// explicit break
					}
				} while (instancesYetToBeAdded > 0);
				weights = computeScores2(numTimesToIterateWithDiffWeights);
			}
				break;
			case OVERSAMPLE_MINORITY_MULTIPLE_DATASETS: {
				/*
				 * Concept here is to make split large unbalanced dataset into at least two smaller datasets. The datasets are equal in size to the
				 * smaller of the two classes. Assuming that the smaller status count does not fit evenly into the larger status, a final small
				 * dataset must be made which includes the final samples of the majority class and reuses some of the minority class samples So for
				 * example if a dataset of 1000 had 700/300 cases/controls we would make 2 datasets of 300/300 and one of 100/100
				 */
				final Map<Byte, List<Integer>> instByClassOriginal = instByClass;
				final byte majorityStatus = getData().getMajorityStatus();
				final byte minorityStatus = getData().getMinorityStatus();
				final List<Integer> majorityRows = instByClassOriginal
						.get(majorityStatus);
				Collections.shuffle(majorityRows, rnd);
				final List<Integer> minorityRows = instByClassOriginal
						.get(minorityStatus);
				Collections.shuffle(minorityRows, rnd);
				int majorityUsedSoFar = 0;
				final double[] weightedAverageWeights = new double[getData().getCols() - 1];
				do {
					final int numberToUse = Math.min(majorityStatusCount
							- majorityUsedSoFar, minorityStatusCount);
					final List<Integer> partialMajorityRows = majorityRows.subList(
							majorityUsedSoFar, majorityUsedSoFar + numberToUse);
					List<Integer> partialMinorityRows;
					if (numberToUse == minorityStatusCount) {
						partialMinorityRows = minorityRows;
					} else {
						partialMinorityRows = minorityRows.subList(0, numberToUse);
					}
					instByClass.put(majorityStatus, partialMajorityRows);
					instByClass.put(minorityStatus, partialMinorityRows);
					samples = null;
					weights = computeScores2(numTimesToIterateWithDiffWeights);
					// each dataset's scores must be weighted by its relative size
					final double percentageFraction = numberToUse
							/ (double) majorityStatusCount;
					for (int attributeIndex = 0; attributeIndex < weights.length; ++attributeIndex) {
						weightedAverageWeights[attributeIndex] += weights[attributeIndex]
								* percentageFraction;
					}
					majorityUsedSoFar += numberToUse;
				} while (majorityUsedSoFar < majorityStatusCount);
				weights = weightedAverageWeights;
				break;
			}
			default:
				throw new RuntimeException("Unhandled case: "
						+ Console.reliefFRebalancingMethod);
		}
		return weights;
	}

	private double[] computeScores2(final int numTimesToIterateWithDiffWeights) {
		double[] weights;
		if (samples == null) {
			samples = new ArrayList<Integer>(m);
			for (final List<Integer> list : instByClass.values()) {
				samples.addAll(list);
			}
			// in case of rebalancing, the size of the dataset can have changed. To try to
			// honor the intent of the user, we convert the original request to a percentage
			// and then get that percentage of samples
			// NOTE: I do not adjust the number of neighbors. Perhaps not consistent but it seemed odd to do so and was not tested
			final float percentageSamplesRequested = m / (float) getData().getRows();
			if (percentageSamplesRequested < 1.0) {
				Collections.shuffle(samples, rnd);
				final int numToSample = (int) (samples.size() * percentageSamplesRequested);
				samples = samples.subList(0, numToSample);
			}
		}
		preProcess();
		final int processorsToUse = parallel ? Runtime.getRuntime()
				.availableProcessors() : 1;
		if (processorsToUse > 1) {
			final ProducerConsumerThread<Integer> reliefThread = new ProducerConsumerThread<Integer>();
			reliefThread.setProducer(new Producer());
			for (int i = 0; i < (processorsToUse); ++i) {
				reliefThread.addConsumer(new Consumer());
			}
			reliefThread.run();
		} else {
			for (final int sampleRow : samples) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				processInstance(sampleRow);
				incrementProgress();
			}
		}
		weights = postProcess();
		if (numTimesToIterateWithDiffWeights > 0) {
			final boolean useExpertKnowledgeScalingAndWeighting = true;
			if (useExpertKnowledgeScalingAndWeighting) {
				double weightsMin = weights[0];
				double weightsMax = weights[0];
				double weightsSum = weights[0];
				for (int attributeIndex = 1; attributeIndex < weights.length; ++attributeIndex) {
					final double currentAttributeWeight = weights[attributeIndex];
					weightsMax = Math.max(weightsMax, currentAttributeWeight);
					weightsMin = Math.min(weightsMin, currentAttributeWeight);
					weightsSum += currentAttributeWeight;
				}
				final ExpertKnowledge.ScoresMetaInformation scoresMetaInformation = new ExpertKnowledge.ScoresMetaInformation(
						weightsMin, weightsMax, weightsSum, weights.length);
				diffWeights = ExpertKnowledge.scaleAndWeight(weights,
						scoresMetaInformation, Console.reliefFWeightedDistanceMethod,
						Console.reliefFWeightedScalingMethod,
						Console.reliefFWeightedScalingParameter);
			} else {
				diffWeights = new double[getData().getCols() - 1];
				switch (Console.reliefFWeightedDistanceMethod) {
					case RANK:
						final SortedMap<Double, Integer> sortedWeights = new TreeMap<Double, Integer>();
						for (int attributeIndex = 0; attributeIndex < weights.length; ++attributeIndex) {
							final double attributeWeight = weights[attributeIndex];
							sortedWeights.put(attributeWeight, attributeIndex);
						}
						int attributeRank = 1;
						for (final Map.Entry<Double, Integer> entry : sortedWeights
								.entrySet()) {
							diffWeights[entry.getValue()] = attributeRank++;
						}
						break;
					case PROPORTIONAL:
						double minWeight = weights[0];
						double maxWeight = weights[0];
						for (int attributeIndex = 1; attributeIndex < weights.length; ++attributeIndex) {
							final double attributeWeight = weights[attributeIndex];
							int compareResult = Double.compare(attributeWeight, minWeight);
							if (compareResult < 0) {
								minWeight = attributeWeight;
							} else {
								compareResult = Double.compare(attributeWeight, maxWeight);
								if (compareResult > 0) {
									maxWeight = attributeWeight;
								}
							}
						} // end attribute to get min and maxWeight
						final double range = maxWeight - minWeight;
						if (range != 0.0f) {
							for (int attributeIndex = 0; attributeIndex < weights.length; ++attributeIndex) {
								final double attributeWeight = weights[attributeIndex];
								final double percentageFraction = (attributeWeight - minWeight)
										/ range;
								// higher weighted attributes get a lower weighted diff weight
								diffWeights[attributeIndex] = (int) (percentageFraction * 1000);
							}
						} // end if non-zero range
						break;
					default:
						throw new RuntimeException(
								"Unhandled RELIEFF_WEIGHTED_DISTANCE_METHOD: "
										+ Console.reliefFWeightedDistanceMethod);
				} // end switch
			}
			final double[] newReliefWeights = computeScores(numTimesToIterateWithDiffWeights - 1);
			diffWeights = null;
			weights = newReliefWeights;
		} // end if going to re-run relief with revised weights
		return weights;
	}

	protected double diff(final int index, final int a, final int b) {
		return getData().getRawDatum(a, index) == getData().getRawDatum(b, index) ? 0
				: ((diffWeights == null) ? 1.0 : diffWeights[index]);
	}

	protected double distance(final int a, final int b) {
		double distance;
		if (ReliefFamilyAttributeScorer.preComputeDistances) {
			int instance1Index;
			int instance2Index;
			if (a > b) {
				instance1Index = a;
				instance2Index = b;
			} else {
				instance1Index = b;
				instance2Index = a;
			}
			distance = distances[instance1Index][instance2Index];
		} else {
			double sum = 0;
			for (int i = 0; i < getData().getCols() - 1; ++i) {
				sum += diff(i, a, b);
			}
			distance = sum;
		}
		return distance;
	}

	@Override
	public Map<String, String> getConfig() {
		return config;
	}

	abstract String getFilterName();

	public int getK() {
		return k;
	}

	public int getM() {
		return m;
	}

	abstract int getNormalizationFactor();

	// public List<Integer> getNeighborsWithinAverageDistance(final int instanceIndex) {
	// final List<Integer> closeNeighbors = new ArrayList<Integer>();
	// for (int comparisonInstanceIndex = 0; comparisonInstanceIndex < getData().getRows(); ++comparisonInstanceIndex) {
	// if (instanceIndex == comparisonInstanceIndex) {
	// continue;
	// } else {
	// final int diff = diff(instanceIndex, comparisonInstanceIndex);
	// if (diff <= averageDistance) {
	// closeNeighbors.add(comparisonInstanceIndex);
	// }
	// }
	// } // end comparison loop
	// return closeNeighbors;
	// } // end getNeighborsWithinAverageDistance()
	@Override
	public int getTotalProgress() {
		return m;
	}

	private void init(final Dataset data, final int pM, final int pK,
			final Random pRnd, final boolean pParallel) {
		m = ((pM > 0) && (pM < data.getRows())) ? pM : data.getRows();
		k = pK;
		rnd = pRnd;
		parallel = pParallel;
		config.put("FILTER", getFilterName());
		config.put("NEIGHBORS", Integer.toString(k));
		config.put("SAMPLES", m == data.getRows() ? "ALL" : Integer.toString(m));
		config = Collections.unmodifiableMap(config);
	}

	abstract double[] postProcess();

	abstract void preProcess();

	protected void printOutFirstTwoAttributeRanks(final String identifier,
			final double[] scores) {
		int firstAttributeRank = scores.length;
		int secondAttributeRank = scores.length;
		System.out.print(getClass().getSimpleName() + "-"
				+ Console.reliefFRebalancingMethod + ":");
		for (int scoreIndex = 0; scoreIndex < scores.length; ++scoreIndex) {
			if (scores[scoreIndex] < scores[0]) {
				--firstAttributeRank;
			}
			if (scores[scoreIndex] < scores[1]) {
				--secondAttributeRank;
			}
		}
		System.out.println(identifier + " " + getData().getLabels().get(0) + ": "
				+ firstAttributeRank + " " + getData().getLabels().get(1) + ": "
				+ secondAttributeRank + " Average: "
				+ ((firstAttributeRank + secondAttributeRank) / 2.0f));
	}

	protected abstract void processInstance(int idx);

	private class Consumer extends ProducerConsumerThread.Consumer<Integer> {
		@Override
		public void consume(final Integer i) {
			processInstance(i);
			incrementProgress();
		}
	}

	private class DistanceConsumer extends
			ProducerConsumerThread.Consumer<Integer> {
		@Override
		public void consume(final Integer instance1Index) {
			final double[] instance1Distances = distances[instance1Index];
			final byte[] instance1Data = getData().getRawRowData(instance1Index);
			for (int instance2Index = 0; instance2Index < instance1Index; ++instance2Index) {
				final byte[] instance2Data = getData().getRawRowData(instance2Index);
				double sum = 0;
				for (int columnIndex = (instance2Data.length - 2); columnIndex >= 0; --columnIndex) {
					sum += (instance1Data[columnIndex] == instance2Data[columnIndex]) ? 0
							: ((diffWeights == null) ? 1 : diffWeights[columnIndex]);
				}
				instance1Distances[instance2Index] = sum;
			} // end instance2 loop
			// System.out.println("Finished row " + (instance1Index + 1) + " distances.");
		} // end consume()
	}

	private class DistanceProducer extends
			ProducerConsumerThread.Producer<Integer> {
		private int instance1Index;

		public DistanceProducer() {
			instance1Index = getData().getRows() - 1;
		}

		@Override
		public Integer produce() {
			// if (((instance1Index + 1) % 100) == 0) {
			// System.out.println("Working on instance " + (instance1Index + 1));
			// }
			if (instance1Index < 0) {
				return null; // ALL DONE!
			}
			distances[instance1Index] = new double[instance1Index];
			return instance1Index--;
		}
	}

	protected class InstanceDistanceComparator implements Comparator<Integer> {
		private final int idx;

		public InstanceDistanceComparator(final int idx) {
			this.idx = idx;
		}

		public int compare(final Integer i1, final Integer i2) {
			int compareResult = Double.compare(distance(idx, i1), distance(idx, i2));
			if (compareResult == 0) {
				// in case of tie decide in a deterministic way
				compareResult = i1.compareTo(i2);
			}
			return compareResult;
		}
	}

	private class Producer extends ProducerConsumerThread.Producer<Integer> {
		private final Iterator<Integer> i = samples.iterator();

		@Override
		public Integer produce() {
			return i.hasNext() ? i.next() : null;
		}
	}
}
