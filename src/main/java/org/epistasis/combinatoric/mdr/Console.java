package org.epistasis.combinatoric.mdr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

import org.epistasis.AbstractAttributeScorer;
import org.epistasis.ChiSquaredScorer;
import org.epistasis.ColumnFormat;
import org.epistasis.OddsRatioScorer;
import org.epistasis.Pair;
import org.epistasis.ReliefFAttributeScorer;
import org.epistasis.SURFAttributeScorer;
import org.epistasis.SURFStarAttributeScorer;
import org.epistasis.SURFStarnTuRFAttributeScorer;
import org.epistasis.SURFnTuRFAttributeScorer;
import org.epistasis.StopWatch;
import org.epistasis.TuRFAttributeScorer;
import org.epistasis.combinatoric.AttributeRanker;
import org.epistasis.combinatoric.EntropyAnalysis;
import org.epistasis.combinatoric.mdr.ExpertKnowledge.RWRuntime;
import org.epistasis.combinatoric.mdr.gui.Frame;
import org.epistasis.combinatoric.mdr.newengine.AnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Collector.BestModel;
import org.epistasis.combinatoric.mdr.newengine.Collector.MdrByGenotypeResults;
import org.epistasis.combinatoric.mdr.newengine.ConfusionMatrix;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.EDAAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.ExhaustiveAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.FixedRandomAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.ForcedAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.Interval.IntervalCollection;
import org.epistasis.combinatoric.mdr.newengine.Model;
import org.epistasis.combinatoric.mdr.newengine.TimedRandomAnalysisThread;

public class Console {
	final static StopWatch stopWatch = new StopWatch();
	public static boolean outputTopModelsAverageTesting = false;
	public static boolean outputModelsSignificanceMetric = false;
	private static AnalysisThread analysis;
	private static boolean minimalOutput = false;
	private static boolean outputEntropy = false;
	public static boolean preserveTuRFRemovalOrder = Main.defaultPreserveTuRFRemovalOrder;
	private static int topModelsLandscapeSize = Main.defaultTopModelsLandscapeSize;
	private static int permutations = Main.defaultPermutations;
	private static boolean computeAllModelsLandscape = true; // Console has historically had landscape on by default
	public static float fishersThreshold = Main.defaultFishersThreshold;
	public static float balancedAccuracyWeighting = 0.68f; // 1 == all balancedAccuracy, 0 == all coverage
	public static AmbiguousCellStatus tieStatus = Main.defaultAmbiguousCellStatus;
	public static boolean convertGenotypeCountsToAlleleCounts = false;
	public static ScoringMethod scoringMethod = Main.defaultScoringMethod;
	public static ModelSignificanceMetric modelSignificanceMetric = ModelSignificanceMetric.MAXIMUM_LIKELIHOOD;
	public static MetricNormalizationMethod metricNormalizationMethod = MetricNormalizationMethod.NONE;
	public static MetricCellComparisonScope metricCellComparisonScope = MetricCellComparisonScope.GLOBAL_INDEPENDENT;
	public static MetricDiffFromExpectedWeightingMethod metricDiffFromExpectedWeightingMethod = MetricDiffFromExpectedWeightingMethod.IDENTITY;
	public static FitnessCriteriaOrder fitnessCriteriaOrder = FitnessCriteriaOrder.CVC_INTERVAL_TESTING;
	public static int reliefFWeightedDistanceIterations = 0;
	public static WeightScheme reliefFWeightedDistanceMethod = WeightScheme.PROPORTIONAL;
	public static ScalingMethod reliefFWeightedScalingMethod = ScalingMethod.EXPONENTIAL;
	public static MultiWayBestModelPickingMethod multiWayBestModelPickingMethod = MultiWayBestModelPickingMethod.Testing_CVC_Parsimony;
	public static double reliefFWeightedScalingParameter = 0.99;
	public static ReliefFRebalancingMethod reliefFRebalancingMethod = ReliefFRebalancingMethod.OVERSAMPLE_MINORITY;
	public static boolean outputAllModels = false;
	private static PrintWriter allModelsWriter = null;
	public static boolean computeMdrByGenotype = false && Main.isExperimental;
	// JAS added following attributes
	private static String missingCode = Main.defaultMissing;
	private static int mdrNodeCount = Main.defaultMDRNodeCount;
	private static int mdrNodeNumber = Main.defaultMDRNodeNumber;
	private static String topOutFname;
	private static Logger log = Logger.getLogger(Console.class.getName());
	static {
		Console.log.setLevel(Level.OFF);
	}
	// ENDJAS
	public static boolean useBestModelActualIntervals = Main.defaultUseBestModelActualIntervals;
	private static boolean useExplicitTestOfInteraction = Main.defaultUseExplicitTestOfInteraction;
	private static boolean printDetailedConfusionMatrix = false;

	private static AnalysisThread createAnalysisThread(final OptionSet set,
			final Dataset data, final int min, final int max,
			final AttributeCombination forced, final int evaluations,
			final double runtime, final int unitmult, final int numAgents,
			final int numUpdates, final double retention, final int alpha,
			final int beta, final ScalingMethod scalingMethod,
			final double scalingParameter, final WeightScheme weightScheme,
			final ExpertKnowledge expertKnowledge, final SearchMethod searchMethod,
			final boolean parallel, final long seed,
			final AmbiguousCellStatus tiePriorityList,
			final Pair<List<Dataset>, List<Dataset>> partition,
			final Runnable onEndAttribute, final Runnable onEnd) {
		AnalysisThread newAnalysis = null;
		switch (searchMethod) {
			case FORCED: {
				newAnalysis = new ForcedAnalysisThread(data, partition,
						tiePriorityList, Console.scoringMethod, seed, null, onEndAttribute,
						onEnd, parallel, Console.topModelsLandscapeSize,
						Console.computeAllModelsLandscape, forced);
			}
				break;
			case RANDOM: {
				if (set.isSet("random_search_eval")) {
					newAnalysis = new FixedRandomAnalysisThread(data, partition,
							tiePriorityList, Console.scoringMethod, seed, null,
							onEndAttribute, onEnd, parallel, Console.topModelsLandscapeSize,
							Console.computeAllModelsLandscape, min, max, evaluations);
				} else if (set.isSet("random_search_runtime")) {
					final long count = (long) Math.ceil(runtime * unitmult);
					newAnalysis = new TimedRandomAnalysisThread(data, partition,
							tiePriorityList, Console.scoringMethod, seed, null,
							onEndAttribute, onEnd, parallel, Console.topModelsLandscapeSize,
							Console.computeAllModelsLandscape, min, max, count);
				}
			}
				break;
			case EDA: {
				final RWRuntime currentRWRuntime = expertKnowledge.new RWRuntime(
						weightScheme, scalingMethod, scalingParameter, alpha, beta,
						retention);
				newAnalysis = new EDAAnalysisThread(data, partition, tiePriorityList,
						Console.scoringMethod, seed, null, onEndAttribute, onEnd, parallel,
						Console.topModelsLandscapeSize, Console.computeAllModelsLandscape,
						min, max, numAgents, numUpdates, currentRWRuntime);
			}
				break;
			case EXHAUSTIVE: {
				newAnalysis = new ExhaustiveAnalysisThread(data, partition,
						tiePriorityList, Console.scoringMethod, seed, null, onEndAttribute,
						onEnd, parallel, Console.topModelsLandscapeSize,
						Console.computeAllModelsLandscape, min, max, Console.mdrNodeCount,
						Console.mdrNodeNumber);
			}
				break;
			case INVALID:
			default:
				throw new RuntimeException(
						"Search parameter arguments not understood: " + searchMethod);
		} // end switch
		return newAnalysis;
	} // end createAnalysisThread()

	private static boolean edaSelected(final OptionSet set) {
		if (set.isSet("eda_search_numAgents") || set.isSet("eda_search_numUpdates")
				|| set.isSet("eda_search_retention") || set.isSet("eda_search_alpha")
				|| set.isSet("eda_search_beta")
				|| set.isSet("eda_search_expertKnowledge")
				|| set.isSet("eda_search_fitness")
				|| set.isSet("eda_search_percentMaxAttributeRange")
				|| set.isSet("eda_search_theta")) {
			return true;
		}
		return false;
	}

	private static boolean forcedSelected(final OptionSet set) {
		if (set.isSet("forced_search")) {
			return true;
		}
		return false;
	}

	private static String getConfusionMatrixInfoString(
			final ConfusionMatrix confusionMatrix, final int affectedStatus) {
		final StringBuilder sb = new StringBuilder();
		// sb.append(Main.defaultFormat.format(confusionMatrix
		// .getScore(Console.scoringMethod)));
		// sb.append("\t");
		if (confusionMatrix == null) {
			sb.append(Main.defaultFormat.format(Float.NaN));
			sb.append("\t");
		} else {
			sb.append(Main.defaultFormat.format(confusionMatrix.getBalancedAccuracy()));
			sb.append("\t");
			if (Console.printDetailedConfusionMatrix) {
				System.out
						.print(Main.defaultFormat.format(confusionMatrix.getFitness()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix
						.getClassifiedCount() / confusionMatrix.getTotalCount()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix
						.getClassifiedCount()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.getTotalCount()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format((confusionMatrix.numTruePositives() + confusionMatrix
						.numFalseNegatives()) / confusionMatrix.getClassifiedCount()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format((confusionMatrix.numTrueNegatives() + confusionMatrix
						.numFalsePositives()) / confusionMatrix.getClassifiedCount()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.numTruePositives()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.numFalseNegatives()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.numTrueNegatives()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.numFalsePositives()));
				sb.append("\t");
				sb.append(Main.defaultFormat.format(confusionMatrix.getUnknownCount()));
				sb.append("\t");
			}
		}
		return sb.toString();
	}

	public static String getDataTableRowString(
			final List<Pair<String, String>> tableData) {
		final StringBuilder sb = new StringBuilder();
		for (final Pair<String, String> pair : tableData) {
			sb.append(pair.getSecond() + "\t");
		}
		return sb.toString();
	}

	private static String getPermutationStatisticsHeaderString(final String prefix) {
		final String outputString = prefix + "\tperm. best level avg. " + prefix
				+ "\tp-value " + prefix + "\t0.001 p-value boundary " + prefix
				+ "\t0.010 p-value boundary " + prefix + "\t0.050 p-value boundary "
				+ prefix + "\t0.100 p-value boundary " + prefix + "\t";
		return outputString;
	}

	private static String getPermutationStatisticsString(
			final double bestModelValue, final double[] passedInPermutationValues) {
		return Console.getPermutationStatisticsString(bestModelValue,
				passedInPermutationValues, MetricRankOrder.DESCENDING);
	}

	private static String getPermutationStatisticsString(
			final double bestModelValue, final double[] passedInPermutationValues,
			final MetricRankOrder metricRankOrder) {
		final int numPermutations = passedInPermutationValues.length;
		final List<Double> permutationValues = new ArrayList<Double>(
				numPermutations);
		for (final double permutationValue : passedInPermutationValues) {
			permutationValues.add(permutationValue);
		}
		switch (metricRankOrder) {
			case ASCENDING:
				Collections.sort(permutationValues);
				break;
			case DESCENDING:
				Collections.sort(permutationValues, Collections.reverseOrder());
				break;
			default:
				throw new IllegalArgumentException("Unknown MetricRankOrder: "
						+ metricRankOrder);
		}
		final double oneTenthOnePercentSignificanceLowerBoundary = Console
				.getPermutationValueForSignificanceLevel(permutationValues,
						metricRankOrder, 0.001f);
		final double onePercentSignificanceLowerBoundary = Console
				.getPermutationValueForSignificanceLevel(permutationValues,
						metricRankOrder, 0.010f);
		final double fivePercentSignificanceLowerBoundary = Console
				.getPermutationValueForSignificanceLevel(permutationValues,
						metricRankOrder, 0.050f);
		final double tenPercentSignificanceLowerBoundary = Console
				.getPermutationValueForSignificanceLevel(permutationValues,
						metricRankOrder, 0.100f);
		int betterThanOrEqualToCount = 0;
		boolean foundExactValue = false;
		double sumFitness = 0.0;
		for (final Double permutationValue : permutationValues) {
			if (permutationValue == bestModelValue) {
				foundExactValue = true;
			}
			if (metricRankOrder.isBetterThanOrEqual(permutationValue, bestModelValue)) {
				++betterThanOrEqualToCount;
			} else {
				// since list is sorted no point in keeping going once lower than best value
				// break;
			}
			sumFitness += permutationValue;
		} // end permutation loop
		final double significance = betterThanOrEqualToCount
				/ (double) numPermutations;
		String significanceRange = Main.defaultFormat.format(significance);
		if (!foundExactValue) {
			final double lowerBoundSignificance = (betterThanOrEqualToCount + 1)
					/ (double) numPermutations;
			significanceRange += "-"
					+ Main.defaultFormat.format(lowerBoundSignificance);
		}
		final double permutationFitnessAverage = sumFitness / numPermutations;
		String outputString;
		outputString = Main.defaultFormat.format(bestModelValue)
				+ "\t"
				+ Main.defaultFormat.format(permutationFitnessAverage)
				+ "\t"
				+ significanceRange
				+ "\t"
				+ Main.defaultFormat
						.format(oneTenthOnePercentSignificanceLowerBoundary) + "\t"
				+ Main.defaultFormat.format(onePercentSignificanceLowerBoundary) + "\t"
				+ Main.defaultFormat.format(fivePercentSignificanceLowerBoundary)
				+ "\t" + Main.defaultFormat.format(tenPercentSignificanceLowerBoundary)
				+ "\t";
		return outputString;
	}

	private static double getPermutationValueForSignificanceLevel(
			final List<Double> permutationValues,
			final MetricRankOrder metricRankOrder,
			final double significanceLevelToLookup) {
		double valueFoundAtProportionalLocation;
		switch (metricRankOrder) {
			case ASCENDING: {
				// if values are in ascending order the largest, most significant, values are at the end of the array
				// in a 1000 item list, for 0.010 significance we want to look at the 10th highest value which
				// will be at index 990
				final double invertedSignificance = 1.0f - significanceLevelToLookup;
				final int indexAtProportion = (int) Math.round(permutationValues.size()
						* invertedSignificance);
				if (indexAtProportion == permutationValues.size()) {
					valueFoundAtProportionalLocation = Double.NaN;
				} else {
					valueFoundAtProportionalLocation = permutationValues
							.get(indexAtProportion);
				}
				break;
			}
			case DESCENDING: {
				final int indexAtProportion = (int) Math.round(permutationValues.size()
						* significanceLevelToLookup);
				if (indexAtProportion == permutationValues.size()) {
					valueFoundAtProportionalLocation = Double.NaN;
				} else {
					valueFoundAtProportionalLocation = permutationValues
							.get(indexAtProportion);
				}
				break;
			}
			default:
				throw new RuntimeException(
						"Console.getPermutationValueForSignificanceLevel: unhandled MetricRankOrder: "
								+ metricRankOrder);
		} // end switch
		return valueFoundAtProportionalLocation;
	}

	private static SearchMethod getRequestedSearchMethod(final OptionSet set) {
		final List<SearchMethod> searchMethods = new ArrayList<SearchMethod>();
		if (Console.forcedSelected(set)) {
			searchMethods.add(SearchMethod.FORCED);
		}
		if (Console.randomSelected(set)) {
			searchMethods.add(SearchMethod.RANDOM);
		}
		if (Console.edaSelected(set)) {
			searchMethods.add(SearchMethod.EDA);
		}
		if (searchMethods.size() > 1) {
			throw new IllegalArgumentException(
					"Error: Multiple search algorithms specified! "
							+ searchMethods.toString());
		}
		// else //got Warning Statement unnecessarily nested within else clause. The corresponding then clause does not complete normally
		{
			final SearchMethod searchMethod = (searchMethods.size() == 0) ? SearchMethod.EXHAUSTIVE
					: searchMethods.get(0);
			return searchMethod;
		}
	}

	// JAS modified to support missingCode
	private static Dataset openDataSet(final String datasetIdentifier,
			final boolean paired) throws Exception, IOException {
		Dataset data = null;
		String dataFileName;
		if (datasetIdentifier.contains("//")) {
			final File tempMDRDataset = File.createTempFile("MDRDataset", ".tmp");
			tempMDRDataset.deleteOnExit();
			String line;
			final String lineSeparator = System.getProperty("line.separator");
			final BufferedReader r = new BufferedReader(new InputStreamReader(
					new URL(datasetIdentifier).openStream()));
			final BufferedWriter w = new BufferedWriter(
					new FileWriter(tempMDRDataset));
			while ((line = r.readLine()) != null) {
				w.write(line);
				w.write(lineSeparator);
			} // end while lines
			w.close();
			dataFileName = tempMDRDataset.getCanonicalPath();
		} // if a URL
		else {
			dataFileName = datasetIdentifier;
		}
		data = new Dataset(Console.missingCode, paired, new LineNumberReader(
				new FileReader(dataFileName)));
		return data;
	}

	public static int parseCommandLineAndRun(final String[] args) {
		int status = 0; // zero is good
		try {
			final Options opt = new Options(args, 1);
			final OptionSet optHelp = opt.addSet("help", 0);
			final OptionSet optVersion = opt.addSet("version", 0);
			final OptionSet optAnalysis = opt.addSet("analysis",
					Main.isExperimental ? 0 : 1, 1000);
			final OptionSet optFilter = opt.addSet("filter", 1, 2);
			optHelp.addOption("help", Multiplicity.ONCE);
			optVersion.addOption("version", Multiplicity.ONCE);
			optAnalysis.addOption("minimal_output", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("onEndAttributeMinimalOutput", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE); // backward compatibility -- mistakenly used
			// camelCase
			optAnalysis.addOption("table_data", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("cv", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("max", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("min", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("nolandscape", Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("top_models_landscape_size", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("paired", Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("parallel", Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("saveanalysis", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("adjust_for_covariate", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("seed", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("permutations", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("permute_with_explicit_test_of_interaction",
					Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			// JAS additions follow
			optAnalysis.addOption("mdr_node_count", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("mdr_node_number", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			// ENDJAS
			optAnalysis.addOption("tie", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("forced_search", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("random_search_eval", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("random_search_runtime", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_numAgents", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_numUpdates", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_retention", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_alpha", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_beta", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_expertKnowledge", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_fitness", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_percentMaxAttributeRange",
					Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("eda_search_theta", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("fishers_threshold", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optAnalysis.addOption("all_models_outfile", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			if (Main.isExperimental) {
				// JAS additions follow
				optAnalysis.addOption("top_models_landscape_outfile", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("missing_code", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				// ENDJAS
				optAnalysis.addOption("balanced_accuracy_weighting", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("scoring_method", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("multi_way_best_model_picking_method",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("fitness_criteria_order", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("convert_genotypes_to_alleles", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("output_top_models_average_testing",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("output_models_significance_metric",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("metric_normalization_method", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("metric_cell_comparison_scope", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optAnalysis.addOption("metric_diff_from_expected_weighting_method",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			} // end isExperimental
			optFilter.addOption("filter", Separator.EQUALS, Multiplicity.ONCE);
			optFilter.addOption("minimal_output", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("parallel", Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("relieff_neighbors", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("relieff_samplesize", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("chisq_pvalue", Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("turf_pct", Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("seed", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
			optFilter.addOption("select_", true, Separator.EQUALS,
					Multiplicity.ZERO_OR_ONE);
			if (Main.isExperimental) {
				optFilter.addOption("preserve_turf_removal_order", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
				optFilter.addOption("relieff_weighted_distance_iterations",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optFilter.addOption("relieff_weighted_distance_method",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optFilter.addOption("relieff_weighted_scaling_method",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optFilter.addOption("relieff_weighted_scaling_exponential_theta",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optFilter.addOption(
						"relieff_weighted_scaling_linear_percentMaxAttributeRange",
						Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
				optFilter.addOption("relieff_rebalancing_method", Separator.EQUALS,
						Multiplicity.ZERO_OR_ONE);
			}
			//Console.printVersionHeader();
			final OptionSet set = opt.getMatchingSet(false /* ignoreUnmatched */,
					false /* requireDataLast */);
			if ((set != null) && (set.getUnmatched().size() > 0)) {
				System.err.println("The following ml.options were not understood:");
				for (final String unmatched : set.getUnmatched()) {
					System.err.println(unmatched);
				}
				System.err.println("Run with -h to see all legal ml.options.");
			} else if (set == optHelp) {
				Console.printUsage(new PrintWriter(System.out, true));
			} else if (set == optVersion) {
				// Console.printVersionHeader(w);
			} else if (set == optFilter) {
				Console.runFilter(args, set);
			} else if (set == optAnalysis) {
				if ((set.getData() == null) || (set.getData().size() == 0)) {
					Console.runAnalysis(args, set, null);
				} else {
					for (final String dataFileName : set.getData()) {
						Console.runAnalysis(args, set, dataFileName);
					}
				}
			} else {
				status = 1; // non-zero is bad
				System.err.println("Command line passed in: " + Arrays.toString(args));
				System.err
						.println("Did you remember to pass the input dataset after all ml.options?");
				System.err
						.println("If you are trying to filter a dataset, did you remember to pass the output file as the final argument?");
				Console.printUsage(new PrintWriter(System.out, true));
			}
		} catch (final Exception ex) {
			status = 1; // non-zero is bad
			System.err.println("Command line passed in: " + Arrays.toString(args));
			System.err.println("Console parseCommandLine caught an exception: " + ex);
			ex.printStackTrace();
		} finally {
			if (Console.allModelsWriter != null) {
				Console.allModelsWriter.close();
			}
			System.out.println("\nMDR finished. Elapsed running time: "
					+ Console.stopWatch.getElapsedTime());
		}
		return status;
	}

	private static void printConfusionMatrixInfoHeader(
			final String columnNameSuffix) {
		System.out.print("bal. acc." + columnNameSuffix + "\t");
		if (Console.printDetailedConfusionMatrix) {
			System.out.print("score" + columnNameSuffix + "\t");
			System.out.print("coverage" + columnNameSuffix + "\t");
			System.out.print("covered" + columnNameSuffix + "\t");
			System.out.print("total" + columnNameSuffix + "\t");
			System.out.print("ratio_cases" + columnNameSuffix + "\t");
			System.out.print("ratio_controls" + columnNameSuffix + "\t");
			System.out.print("true_positives" + columnNameSuffix + "\t");
			System.out.print("false_negatives" + columnNameSuffix + "\t");
			System.out.print("true_negatives" + columnNameSuffix + "\t");
			System.out.print("false_positives" + columnNameSuffix + "\t");
			System.out.print("unknown" + columnNameSuffix + "\t");
		}
	}

	public static void printTableDataHeader(
			final List<Pair<String, String>> tableData) {
		for (final Pair<String, String> pair : tableData) {
			System.out.print(pair.getFirst().replace(' ', '_') + "\t");
		}
	}

	// JAS
	public static void printTopOutFile() {
		Console.printTopOutFile(null);
	}

	public static void printTopOutFile(final String annotation) {
		if (Console.topOutFname == null) {
			return;
		}
		try {
			PrintWriter topout;
			final ArrayList<String> lines = new ArrayList<String>();
			try {
				for (final Collector collector : Console.analysis.getCollectors()) {
					for (final Pair<String, Float> p : collector.getTopModelsLandscape()
							.getLandscape()) {
						lines.add("" + p.getFirst() + '\t' + p.getSecond());
					}
				}
				Console.log.info("got " + lines.size() + " top lines");
			} catch (final ConcurrentModificationException e) {
				Console.log.info("concurrent modification exception, trying again...");
				Thread.sleep(200);
				Console.printTopOutFile(annotation);
				return;
			}
			try {
				topout = new PrintWriter(Console.topOutFname + ".new");
				if (annotation != null) {
					topout.println("#" + annotation);
				}
				topout.println("Combo\tScore");
			} catch (final FileNotFoundException e) {
				System.err.println("Error: couldn't open file " + Console.topOutFname);
				return;
			}
			Console.log.info("pre printout got " + lines.size() + " top lines");
			for (final String l : lines) {
				topout.println(l);
			}
			topout.flush();
			topout.close();
			final File f = new File(Console.topOutFname);
			f.delete();
			if (!new File(Console.topOutFname + ".new").renameTo(new File(
					Console.topOutFname))) {
				System.err.println("Error, couldn't rename .new to "
						+ Console.topOutFname);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	// ENDJAS
	private static void printUsage(final PrintWriter out) {
		out.println("Usage:");
		out.println();
		out.println("java -jar mdr.jar");
		out.println("  (GUI mode)");
		out.println();
		out.println("java -jar mdr.jar -help");
		out.println("  (Display this message)");
		out.println();
		out.println("java -jar mdr.jar -version");
		out.println("  (Display version information)");
		out.println();
		out.println("java -jar mdr.jar [analysis ml.options] <datafile>");
		out.println("  (Batch mode analysis)");
		out.println();
		out.println("java -jar mdr.jar [filter ml.options] <datafile> "
				+ "<outputfile>");
		out.println("  (Batch mode filter)");
		out.println();
		out.println("Analysis Options:");
		out.println();
		out.println("-cv=<int>\n\tdefault: "
				+ Main.defaultCrossValidationCount
				+ "\n\tCross-validation count. Determines partitioning of the data into multiple overlapping datasets which can be used to check against over-fitting. Setting to 1 disables cross-validation.");
		out.println("-min=<int>\n\tdefault: "
				+ Main.defaultAttributeCountMin
				+ "\n\tThe minimum number of attributes considered together. Referred to in UI under 'Attribute Count Range'.");
		out.println("-max=<int>\n\tdefault: "
				+ Main.defaultAttributeCountMax
				+ "\n\tThe maximum number of attributes considered together. Referred to in UI under 'Attribute Count Range'.");
		out.println("-nolandscape\n\tdefault: this is not a boolean and takes no argument -- if present then true\n\tThis will stop the output of the fitness of every attribute combination looked at. This will signifigantly reduce the size of your output.");
		out.println("-top_models_landscape_size\n\tdefault: "
				+ Console.topModelsLandscapeSize
				+ "\n\tThis will keep track of this number of the best models for each level.");
		out.println("-parallel\n\tdefault: this is not a boolean and takes no argument -- if present then true\n\tIf present, program will make best use of multiple processors/cores.");
		out.println("-forced_search=<comma-separated attribute list>\n\tdefault: exhaustive search\n\tChanges search from exhaustive testing of all attribute combinations to only test the combination specified with this option.");
		out.println("-random_search_eval=<int>\n\tdefault: exhaustive search\n\tChanges search from exhaustive testing of all attribute combinations to instead test the passed in number of random combinations of attributes. There is nothing to prevent the same combination from being repeatedly tested.");
		out.println("-random_search_runtime=<double><s|m|h|d>\n\tdefault: exhaustive search\n\tchanges search from exhaustive testing of all attribute combinations to instead test the passed random combinations of attributes for a specified time such as 30m (30 minutes) or 1h (1 hour). There is nothing to prevent the same combination from being repeatedly tested.");
		out.println("-adjust_for_covariate=<attributeName>\n\tdefault: not used\n\tIf specified, the dataset will be modified to remove all main effects attributable to the passed in attribute.");
		out.println("-seed=<long>\n\tdefault: "
				+ Main.defaultRandomSeed
				+ "\n\tThis number will be used to seed the random number generator. This is used to partition the datasets when using cross validation. Varying this number will change the cross validation results slightly. It is also used more extensively for types of searches that involve probabilities or random choices such as EDA and Random.");
		out.println("-tie=<"
				+ Arrays.toString(AmbiguousCellStatus.values())
				+ ">\n\tdefault: "
				+ Main.defaultAmbiguousCellStatus
				+ "\n\tIf the case and control counts are equal for an attribute combination cell, the status of that cell will be as specified here.");
		out.println("-saveanalysis=<filename>\n\tdefault: analysis not saved by default\n\tname of file to save MDR Gui compatible analysis file.");
		out.println("-minimal_output=<boolean>\n\tdefault: "
				+ Console.minimalOutput
				+ "\n\tThis will output only the best attribute combination for each level, skipping all other normal information such as cross-validation, etcetera.");
		out.println("-paired\n\tdefault: this is not a boolean and takes no argument -- if present then true\n\tThis alters the way data sets are partitioned to make sure adjacent case/control pairs always end up in the same cross-validation interval.");
		out.println("-permutations=<int>\n\tdefault: "
				+ Console.permutations
				+ "\tIf non-zero the mdr status column will be shuffled and mdr run a number of times. Minimum = 100. The probability of the observed mdr model testing accuracy and cv will be output.");
		out.println("-permute_with_explicit_test_of_interaction=<boolean>\n\tdefault: "
				+ Console.useExplicitTestOfInteraction
				+ "\n\tVariant of permutation testign that keeps main affects while permuting attribute interactions");
		// JAS
		// JAS code to support cluster-based parallelization
		out.println("-mdr_node_count=<Integer>\n\tdefault: "
				+ Console.mdrNodeCount
				+ "\tIf greater than 1, attribute combinations will be split in the specified number of subset. Which subset mdr looks at is determined by mdr_node_number. This can be used as a primitive form of parallelization where multiple mdr instances (perhaps on a cluster) process a subset of the data. It will be up to the user to collate the results.");
		out.println("-mdr_node_number=<Integer>\n\tdefault: "
				+ Console.mdrNodeNumber
				+ "\tOnly relevant if mdr_node_count is greater than 1. This will determine which subset of the attribute combinations the current instance of mdr will examine.");
		// ENDJAS
		out.println("-eda_search_numAgents=<int>\n\tdefault: "
				+ Main.defaultEDANumAgents
				+ "\n\tnumber of MDR models (attribute combinations) evaluated in each generation");
		out.println("-eda_search_numUpdates=<int>\n\tdefault: "
				+ Main.defaultEDANumUpdates + "\n\tnumber of generations");
		out.println("-eda_search_retention=<double>\n\tdefault: "
				+ Main.defaultEDARetention
				+ "\n\tdetermines how much weight information from previous iterations is given relative to information gained in the most recent iteration");
		out.println("-eda_search_alpha=<int>\n\tdefault: "
				+ Main.defaultEDAAlpha
				+ "\n\tthe relative weight given to the current generations MDR score (balanced accuracy).");
		out.println("-eda_search_beta=<int>\n\tdefault: "
				+ Main.defaultEDABeta
				+ "\n\tthe relative weight given to the expert knowledge score of an attribute.");
		out.println("-eda_search_expertKnowledge=<filename>\n\tdefault: no file read in by default\n\tformat is one row per attribute with two tab-delimited columns: attribute name and floating point number as weight. Higher numbers are considered better.");
		out.println("-eda_search_fitness=<boolean>\n\tdefault: "
				+ !Main.defaultEDAIsFitnessProportional + "\n\t");
		out.println("-eda_search_theta=<double>\n\tdefault: "
				+ Main.defaultEDAExponentialTheta + "\n\t");
		out.println("-eda_search_percentMaxAttributeRange=<0-100>\n\tdefault: "
				+ Main.defaultEDAPercentMaxAttributeRange + "\n\t");
		out.println("-table_data=<either 'true' or else comma delimited name=value pairs>\n\tIf this is present the format of the output data is changed into a tab-delimited table format with each attribute level result generating only one row/line. It is useful when doing many runs with different parameters. You add the parameters that can vary and their value for the current run and these will shown in their own columns.");
		out.println("-fishers_threshold=<float>\n\tdefault: "
				+ Console.fishersThreshold
				+ "\n\tIf not Double.NaN this alters how MDR determines how ties are determined when determining the classification for an attribute combination. Instead of looking only to see if case and control are equal, this uses the Fisher's Exact Test to determine if the two numbers differ significantly. If the Fisher's two tailed result is greater than or equal to the passed in number the differences are considered significant, else considered a tie and the cell is classified according to tie priority.");
		out.println("-all_models_outfile=<filename>\n\tdefault: <none>\tIf present, all model results will be output as they are calculated. This is better than landscape which does not output until the end and may fail due to running out of memory.");
		if (Main.isExperimental) {
			out.println("-scoring_method=<"
					+ Arrays.toString(ScoringMethod.values())
					+ ">\n\tdefault: "
					+ Console.scoringMethod
					+ "\tDetermines the how model predictions are weighted. Methods other than 'BALANCED_ACCURACY' affect results only if -tie=UNKNOWN and are variants of reducing fitness proportionate to the number of tied rows which are not classified.");
			// JAS
			out.println("-missing_code=<String>\n\tdefault: "
					+ Console.missingCode
					+ "\tWhat string should be interpreted as missing data. Note: the default is an empty cell.");
			// ENDJAS
			out.println("-balanced_accuracy_weighting=<float from 0.0 to 1.0>\n\tdefault: 1.0 (score = 100% balanced accuracy and no coverage)\n\t0.0 would mean the opposite -- 100% coverage and no balanced accuracy. A value of around 0.70 (favoring balanced accuracy but still counting coverage) gives good results.");
			out.println("-fitness_criteria_order=<"
					+ Arrays.toString(FitnessCriteriaOrder.values())
					+ ">\n\tdefault: "
					+ Console.fitnessCriteriaOrder
					+ "\tDetermines the metrics and the order they are applied to compare models within a level.");
			out.println("-multi_way_best_model_picking_method=<"
					+ Arrays.toString(MultiWayBestModelPickingMethod.values())
					+ ">\n\tdefault: "
					+ Console.multiWayBestModelPickingMethod
					+ "\tDetermines the metrics and the order they are applied to compare models across levels.");
			out.println("-convert_genotypes_to_alleles=<boolean>\n\tdefault: "
					+ Console.convertGenotypeCountsToAlleleCounts
					+ "\tConcept is that alleles may determine status/class more than genotype. If true then genotypes equal to '1' will be treated as if heterozgous Aa and counts will be split between '0' and '2'.");
			out.println("-output_top_models_average_testing=<boolean>\n\tdefault: "
					+ Console.outputTopModelsAverageTesting
					+ "\tIf true then the top models will include an extra column showing average testing accurracy.");
			out.println("-output_models_significance_metric=<"
					+ Arrays.toString(Console.ModelSignificanceMetric.values())
					+ ">\n\tdefault: "
					+ Console.modelSignificanceMetric
					+ "\tIf present then models will include an extra column showing the significance of the case control ratios for all genotypes of the model.");
		} // end isExperimental
		out.println();
		out.println("Filter Options:");
		out.println();
		out.println("-chisq_pvalue\n\tdefault: this is not a boolean and takes no argument -- if present then true\n\tif present then uses chi squared p-value, else it uses the chi squared value.");
		out.println("-filter=<CHISQUARED | ODDSRATIO | RELIEFF | TURF | SURF | SURFNTURF | SURF* (or SURFSTAR) | SURF*NTURF (or SURFSTARNTURF)>\n\tdefault: no default - if not specified then must not be doing a filter\n\tspecifies which measure used to filter data.\nTo save the filtered dataset, include the name of the output file as your final argument.");
		out.println("-parallel\n\tdefault: this is not a boolean and takes no argument -- if present then true\n\tIf present, program will make best use of multiple processors/cores.");
		out.println("-seed=<long>\n\tdefault: "
				+ Main.defaultRandomSeed
				+ "\n\tThis number will be used to seed the random number generator. This is used to pick random samples if relieff_samplesize less than entire dataset.");
		out.println("-relieff_neighbors=<int>\n\tdefault: "
				+ Main.defaultReliefFNeighbors
				+ " or number of datarows if smaller\n\tnumber of instances with most similar attributes used in calculating score");
		out.println("-relieff_samplesize=<int>\n\tdefault: "
				+ "<entire dataset>\n\tnumber of instances used in calculating scores. Recommended to use all.");
		if (Main.isExperimental) {
			out.println("-preserve_turf_removal_order=<boolean>\n\tdefault: "
					+ Console.preserveTuRFRemovalOrder
					+ "\tIf false, TurF scores are last reliefF (or Surf or SurfStar) scores for each attributes. If true, then the order attributes are removed is kept track of and the last reliefF score is normalized so that the scores sorting order is in reverse order of removal.");
			out.println("-relieff_weighted_distance_iterations=<int>\n\tdefault: "
					+ Console.reliefFWeightedDistanceIterations
					+ "\tIf non-zero reliefF will iterate n times using the previous generation relieff scores based on relieff_weighted_distance_method");
			out.println("-relieff_weighted_distance_method=<"
					+ Arrays.toString(WeightScheme.values())
					+ ">\n\tdefault: "
					+ Console.reliefFWeightedDistanceMethod
					+ "\tWhen relieff_weighted_distance_iterations is > 0, this specifies the weighting method of the previous iteration of relieff scores.");
			out.println("-relieff_weighted_scaling_method=<"
					+ Arrays.toString(ScalingMethod.values())
					+ ">\n\tdefault: "
					+ Console.reliefFWeightedScalingMethod
					+ "\tWhen relieff_weighted_distance_iterations is > 0, this specifies the scaling method of the previous iteration of relieff scores.");
			out.println("-relieff_weighted_scaling_exponential_theta=<double>\n\tdefault: "
					+ Main.defaultEDAExponentialTheta + "\n\t");
			out.println("-relieff_weighted_scaling_linear_percentMaxAttributeRange=<0-100>\n\tdefault: "
					+ Main.defaultEDAPercentMaxAttributeRange + "\n\t");
			out.println("-relieff_rebalancing_method=<"
					+ Arrays.toString(ReliefFRebalancingMethod.values())
					+ ">\n\tdefault: "
					+ Console.reliefFRebalancingMethod
					+ "\tHow relieff deals with data. Even when balanced slightly changes behavior because samples drawn equally from classes.");
		} // end if Main.isExperimental
		out.println("-select_<N | PCT | THRESH>=<value>\n\tN selects a set number of top attributes based on score, percent selects a percentage of top attributes, threshold selects ones with a score above a specified value.");
		out.println("-turf_pct=<float>\n\tdefault: "
				+ (Main.defaultTuRFPct / 100.0f)
				+ "\n\tNumber between 0 and 1 which represents fraction of attributes removed in each TuRF iteration.");
		out.println();
	}

//	private static void printVersionHeader() {
//		System.out.println("MDR Version " + MDRProperties.get("version") + " "
//				+ MDRProperties.get("releaseType") + " build date: "
//				+ MDRProperties.get("buildDate"));
//		if (Main.isExperimental) {
//			System.out.println("Experimental features are enabled.");
//		}
//		System.out.println();
//	}

	private static boolean randomSelected(final OptionSet set) {
		if (set.isSet("random_search_eval") || set.isSet("random_search_runtime")) {
			return true;
		}
		return false;
	}

	// ENDJAS
	// JAS support function
	public static String replaceSuffix(final String fname, final String newsuff) {
		final int i = fname.lastIndexOf('.');
		String base;
		if (i > -1) {
			base = fname.substring(0, i);
		} else {
			base = fname;
		}
		return base + "." + newsuff;
	} // end runAnalysis

	private static void runAnalysis(final String[] args, final OptionSet set,
			final String dataFileName) throws Exception {
		final StringBuffer config = new StringBuffer();
		final ColumnFormat columns = new ColumnFormat(Arrays.asList(new Integer[] {
				new Integer(50), new Integer(100) }));
		boolean paired = false;
		if (set.isSet("paired")) {
			paired = true;
		}
		List<Pair<String, String>> tableData = null;
		if (set.isSet("table_data")) {
			final String rawTableData = set.getOption("table_data").getResultValue(0)
					.trim();
			if (Boolean.parseBoolean(rawTableData)) {
				tableData = new ArrayList<Pair<String, String>>(0);
			} else {
				if (!rawTableData.equalsIgnoreCase("False")) {
					final String[] pairs = rawTableData.split("[,]"); // name value pairs split on commas
					tableData = new ArrayList<Pair<String, String>>(pairs.length);
					for (final String nameEqualValue : pairs) {
						final String[] pairInfo = nameEqualValue.split("=");
						if (pairInfo.length != 2) {
							throw new RuntimeException(
									"-table_data parameter cound not be parsed into key value pairs. Value passed in was: "
											+ rawTableData);
						}
						tableData.add(new Pair<String, String>(pairInfo[0], pairInfo[1]));
					}
				}
			}
		}
		// JAS
		// JAS code to support cluster-based parallelization
		if (set.isSet("mdr_node_count")) {
			Console.mdrNodeCount = set.getOptionInteger("mdr_node_count");
			config.append(columns.format(Arrays.asList(new String[] {
					"mdr_node_count:", String.valueOf(Console.mdrNodeCount) })) + '\n');
		}
		if (set.isSet("mdr_node_number")) {
			Console.mdrNodeNumber = set.getOptionInteger("mdr_node_number");
			config.append(columns.format(Arrays.asList(new String[] {
					"mdr_node_number:", String.valueOf(Console.mdrNodeNumber) })) + '\n');
		}
		if (set.isSet("tie")) {
			final String tieStatusString = set.getOption("tie").getResultValue(0);
			try {
				Console.tieStatus = AmbiguousCellStatus
						.getTiePriorityFromString(tieStatusString);
				config.append(columns.format(Arrays.asList(new String[] { "Tie Cells:",
						Console.tieStatus.toString() })) + '\n');
			} catch (final IllegalArgumentException ex) {
				System.out.println("Error: tie value. Legal values: "
						+ Arrays.toString(AmbiguousCellStatus.values())
						+ ". Passed in value: " + tieStatusString);
				return;
			}
		}
		if (set.isSet("fishers_threshold")) {
			try {
				final String fishersThresholdString = set
						.getOption("fishers_threshold").getResultValue(0);
				Console.fishersThreshold = Float.parseFloat(fishersThresholdString);
				if ((Console.fishersThreshold > 1.0f)
						|| (Console.fishersThreshold <= 0.0f)) {
					System.out
							.println("Error: fishers_threshold out of range. This must a number greater than zero and less than or equal to one. Passed in value: "
									+ fishersThresholdString);
					return;
				}
				if (Console.tieStatus != AmbiguousCellStatus.UNASSIGNED) {
					System.out
							.println("Warning: fishers_threshold was passed in but 'tie' is set to "
									+ Console.tieStatus
									+ ".\n\tAmbiguous cells will automatically be set to that status rather than being considered UNASSIGNED\n\twhich is the more commonn choice when fishers_threshold is specified.");
				}
				config
						.append(columns.format(Arrays.asList(new String[] {
								"fishers_threshold:", String.valueOf(Console.fishersThreshold) })) + '\n');
			} catch (final NumberFormatException ex) {
				System.out
						.println("Error: fishers_threshold accepts float values only.");
				return;
			}
		}
		if (Main.isExperimental) {
			if (set.isSet("scoring_method")) {
				final String scoringMethodString = set.getOption("scoring_method")
						.getResultValue(0);
				try {
					Console.scoringMethod = Enum.valueOf(ScoringMethod.class,
							scoringMethodString);
					config
							.append(columns.format(Arrays.asList(new String[] {
									"scoring_method:", String.valueOf(Console.scoringMethod) })) + '\n');
				} catch (final IllegalArgumentException ex) {
					System.out.println("Error: unknown scoring_method. Legal values: "
							+ Arrays.toString(ScoringMethod.values()) + ". Passed in value: "
							+ scoringMethodString);
					return;
				}
			}
			if (set.isSet("missing_code")) {
				Console.missingCode = set.getOption("missing_code").getResultValue(0);
				config.append(columns.format(Arrays.asList(new String[] {
						"missing_code:", Console.missingCode })) + '\n');
			}
			if (set.isSet("balanced_accuracy_weighting")) {
				try {
					Console.balancedAccuracyWeighting = Float.parseFloat(set.getOption(
							"balanced_accuracy_weighting").getResultValue(0));
					config.append(columns.format(Arrays.asList(new String[] {
							"balanced_accuracy_weighting:",
							String.valueOf(Console.balancedAccuracyWeighting) })) + '\n');
				} catch (final NumberFormatException ex) {
					System.err
							.println("Error: balancedAccuracyWeighting accepts float values only.");
					return;
				}
			}
			if (set.isSet("multi_way_best_model_picking_method")) {
				final String multiWayBestModelPickingMethodString = set.getOption(
						"multi_way_best_model_picking_method").getResultValue(0);
				try {
					Console.multiWayBestModelPickingMethod = Enum.valueOf(
							MultiWayBestModelPickingMethod.class,
							multiWayBestModelPickingMethodString);
					config
							.append(columns.format(Arrays.asList(new String[] {
									"multi_way_best_model_picking_method:",
									String.valueOf(Console.multiWayBestModelPickingMethod) })) + '\n');
				} catch (final NumberFormatException ex) {
					System.out
							.println("Error: unknown multiWayBestModelPickingMethod. Legal values: "
									+ Arrays.toString(MultiWayBestModelPickingMethod.values())
									+ ". Passed in value: "
									+ multiWayBestModelPickingMethodString);
					return;
				}
			}
			if (set.isSet("fitness_criteria_order")) {
				final String fitnessCriteriaOrderString = set.getOption(
						"fitness_criteria_order").getResultValue(0);
				try {
					Console.fitnessCriteriaOrder = Enum.valueOf(
							FitnessCriteriaOrder.class, fitnessCriteriaOrderString);
					config.append(columns.format(Arrays.asList(new String[] {
							"fitness_criteria_order:",
							String.valueOf(Console.fitnessCriteriaOrder) })) + '\n');
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown fitness criteria order. Legal values: "
									+ Arrays.toString(FitnessCriteriaOrder.values())
									+ ". Passed in value: " + fitnessCriteriaOrderString);
					return;
				}
			}
			if (set.isSet("convert_genotypes_to_alleles")) {
				Console.convertGenotypeCountsToAlleleCounts = Boolean.parseBoolean(set
						.getOption("convert_genotypes_to_alleles").getResultValue(0));
				config
						.append(columns.format(Arrays.asList(new String[] {
								"convert_genotypes_to_alleles:",
								String.valueOf(Console.convertGenotypeCountsToAlleleCounts) })) + '\n');
			}
			if (set.isSet("output_top_models_average_testing")) {
				Console.outputTopModelsAverageTesting = Boolean.parseBoolean(set
						.getOption("output_top_models_average_testing").getResultValue(0));
				config.append(columns.format(Arrays.asList(new String[] {
						"output_top_models_average_testing:",
						String.valueOf(Console.outputTopModelsAverageTesting) })) + '\n');
			}
			if (set.isSet("output_models_significance_metric")) {
				final String modelSignificanceMetricString = set.getOption(
						"output_models_significance_metric").getResultValue(0);
				try {
					Console.modelSignificanceMetric = Enum.valueOf(
							ModelSignificanceMetric.class, modelSignificanceMetricString);
					config.append(columns.format(Arrays.asList(new String[] {
							"output_models_significance_metric:",
							String.valueOf(Console.modelSignificanceMetric) })) + '\n');
					Console.outputModelsSignificanceMetric = true;
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown models significance metric. Legal values: "
									+ Arrays.toString(ModelSignificanceMetric.values())
									+ ". Passed in value: " + modelSignificanceMetricString);
					return;
				}
			}
			if (set.isSet("metric_normalization_method")) {
				final String modelSignificanceNormalizationMethodString = set
						.getOption("metric_normalization_method").getResultValue(0);
				try {
					Console.metricNormalizationMethod = Enum.valueOf(
							MetricNormalizationMethod.class,
							modelSignificanceNormalizationMethodString);
					config.append(columns.format(Arrays.asList(new String[] {
							"metric_normalization_method:",
							String.valueOf(Console.metricNormalizationMethod) })) + '\n');
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown metric_normalization_method. Legal values: "
									+ Arrays.toString(MetricNormalizationMethod.values())
									+ ". Passed in value: "
									+ modelSignificanceNormalizationMethodString);
					return;
				}
			}
			if (set.isSet("metric_cell_comparison_scope")) {
				final String metricCellComparisonScopeString = set.getOption(
						"metric_cell_comparison_scope").getResultValue(0);
				try {
					Console.metricCellComparisonScope = Enum.valueOf(
							MetricCellComparisonScope.class, metricCellComparisonScopeString);
					config.append(columns.format(Arrays.asList(new String[] {
							"metric_cell_comparison_scope:",
							String.valueOf(Console.metricCellComparisonScope) })) + '\n');
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown metric_cell_comparison_scope. Legal values: "
									+ Arrays.toString(MetricCellComparisonScope.values())
									+ ". Passed in value: " + metricCellComparisonScopeString);
					return;
				}
			}
			if (set.isSet("metric_diff_from_expected_weighting_method")) {
				final String metricDiffFromExpectedWeightingMethodString = set
						.getOption("metric_diff_from_expected_weighting_method")
						.getResultValue(0);
				try {
					Console.metricDiffFromExpectedWeightingMethod = Enum.valueOf(
							MetricDiffFromExpectedWeightingMethod.class,
							metricDiffFromExpectedWeightingMethodString);
					config
							.append(columns.format(Arrays.asList(new String[] {
									"metric_diff_from_expected_weighting_method:",
									String.valueOf(Console.metricDiffFromExpectedWeightingMethod) })) + '\n');
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown metric_diff_from_expected_weighting_method. Legal values: "
									+ Arrays.toString(MetricDiffFromExpectedWeightingMethod
											.values())
									+ ". Passed in value: "
									+ metricDiffFromExpectedWeightingMethodString);
					return;
				}
			}
			if (dataFileName == null) {
				// if no dataset then just in experimental mode launch the gui version
				// this allows setting of non-standard ml.options such as fishers test from
				// the command line
				System.out
						.println("NO DATASET PASSED IN. IN EXPERIMENTAL MODE THIS TRIGGERS GUI WITH PARSED COMMAND LINE ARGUMENTS");
				System.out.println("=== Configuration ===");
				System.out.println("Command line arguments: " + Arrays.toString(args));
				System.out.println(config.toString());
				Frame.run();
				return;
			}
		} // end is experimental
		Dataset data = Console.openDataSet(dataFileName, paired);
		config.append(columns.format(Arrays.asList(new String[] { "Datafile:",
				dataFileName })) + '\n');
		int cv = Math.min(Main.defaultCrossValidationCount, data.getRows());
		if (set.isSet("cv")) {
			try {
				cv = Integer.parseInt(set.getOption("cv").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.out.println("Error: cv accepts integer values only.");
				return;
			}
			if ((cv < 1) || (cv > data.getRows())) {
				System.err
						.println("Error: cv must be > 0 and <= number of instances in the data set.");
				return;
			}
		}
		config.append(columns.format(Arrays.asList(new String[] { "CV Intervals:",
				Integer.toString(cv) })) + '\n');
		if (set.isSet("permutations")) {
			try {
				Console.permutations = Integer.parseInt(set.getOption("permutations")
						.getResultValue(0));
				// if ((Console.permutations != Main.defaultPermutations)
				// && (Console.permutations < 100)) {
				// System.out
				// .println("Permutations,of specified, amust be at least one hundred. Passed in value: "
				// + Console.permutations);
				// return;
				// }
				if (set.isSet("permute_with_explicit_test_of_interaction")) {
					Console.useExplicitTestOfInteraction = Boolean.parseBoolean(set
							.getOption("permute_with_explicit_test_of_interaction")
							.getResultValue(0));
				}
				config.append(columns.format(Arrays.asList(new String[] {
						"Permutations:", String.valueOf(Console.permutations) })) + '\n');
				config.append(columns.format(Arrays.asList(new String[] {
						"Permute explicit test:",
						String.valueOf(Console.useExplicitTestOfInteraction) })) + '\n');
			} catch (final NumberFormatException ex) {
				System.out.println("Error: permutations accepts integer values only.");
				return;
			}
		}
		int minLevel = Math.min(Main.defaultAttributeCountMin, data.getCols() - 1);
		int maxLevel = Math.min(Main.defaultAttributeCountMax, data.getCols() - 1);
		// Forced Search Options
		AttributeCombination forced = null;
		// Random Search Options
		int evaluations = 0;
		double runtime = 0;
		String units = "";
		int unitmult = 1;
		// EDA Search Options
		int numAgents = Main.defaultEDANumAgents;
		int numUpdates = Main.defaultEDANumUpdates;
		double retention = Main.defaultEDARetention;
		int alpha = Main.defaultEDAAlpha;
		int beta = Main.defaultEDABeta;
		ScalingMethod scalingMethod = null;
		double scalingParameter = 0.0;
		WeightScheme weightScheme = null;
		ExpertKnowledge expertKnowledge = null;
		boolean expertKnowledgeLoaded = false;
		boolean fitness = true;
		double percentMaxAttributeRange = Main.defaultEDAPercentMaxAttributeRange;
		double theta = Main.defaultEDAExponentialTheta;
		if (set.isSet("minimal_output")) {
			Console.minimalOutput = Boolean.parseBoolean(set.getOption(
					"minimal_output").getResultValue(0));
		} else if (set.isSet("onEndAttributeMinimalOutput")) {
			Console.minimalOutput = Boolean.parseBoolean(set.getOption(
					"onEndAttributeMinimalOutput").getResultValue(0));
		}
		// Specify Search
		final SearchMethod searchMethod = Console.getRequestedSearchMethod(set);
		switch (searchMethod) {
			case FORCED: {
				final String value = set.getOption("forced_search").getResultValue(0);
				if (set.isSet("min") || set.isSet("max")) {
					System.out.println("Warning: -min and -max ignored for forced "
							+ "analysis.");
				}
				try {
					forced = new AttributeCombination(value, data.getLabels());
				} catch (final IllegalArgumentException ex) {
					System.out.println("Error: " + ex.getMessage());
					return;
				}
				config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
						"Forced" })) + '\n');
				config.append(columns.format(Arrays.asList(new String[] { "Forced:",
						forced.getComboString() })) + '\n');
				minLevel = maxLevel = forced.size();
			}
				break;
			case RANDOM: {
				if (set.isSet("random_search_eval")) {
					if (set.isSet("random_search_runtime")) {
						System.out.println("Error: Multiple search algorithms specified!");
						return;
					}
					evaluations = Integer.parseInt(set.getOption("random_search_eval")
							.getResultValue(0));
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"Random (Evaluations)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"Evaluations:", Integer.toString(evaluations) })) + '\n');
				} else if (set.isSet("random_search_runtime")) {
					final String s = set.getOption("random_search_runtime")
							.getResultValue(0);
					final Pattern p = Pattern.compile("^(.+?)([smhd]?)$",
							Pattern.CASE_INSENSITIVE);
					final Matcher m = p.matcher(s);
					if (m.matches()) {
						runtime = Double.parseDouble(m.group(1));
						if (m.group(2).equalsIgnoreCase("S")) {
							units = "Seconds";
						} else if ((m.group(2).length() == 0)
								|| m.group(2).equalsIgnoreCase("M")) {
							units = "Minutes";
							unitmult = 60;
						} else if (m.group(2).equalsIgnoreCase("H")) {
							units = "Hours";
							unitmult = 60 * 60;
						} else if (m.group(2).equalsIgnoreCase("D")) {
							units = "Days";
							unitmult = 60 * 60 * 24;
						}
					}
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"Random (Runtime)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] { "Runtime:",
							Double.toString(runtime) }))
							+ ' ' + units + '\n');
				}
			}
				break;
			case EDA: {
				/** Might need to catch improper arguments here, i.e. given a double when expecting an int */
				// NumAgents
				if (set.isSet("eda_search_numAgents")) {
					numAgents = Integer.parseInt(set.getOption("eda_search_numAgents")
							.getResultValue(0));
				}
				// NumUpdates
				if (set.isSet("eda_search_numUpdates")) {
					numUpdates = Integer.parseInt(set.getOption("eda_search_numUpdates")
							.getResultValue(0));
				}
				// Retention
				if (set.isSet("eda_search_retention")) {
					retention = Double.parseDouble(set.getOption("eda_search_retention")
							.getResultValue(0));
				}
				// Alpha
				if (set.isSet("eda_search_alpha")) {
					alpha = Integer.parseInt(set.getOption("eda_search_alpha")
							.getResultValue(0));
				}
				// Beta
				if (set.isSet("eda_search_beta")) {
					beta = Integer.parseInt(set.getOption("eda_search_beta")
							.getResultValue(0));
				}
				// ExpertKnowledge
				if (set.isSet("eda_search_expertKnowledge")) {
					final String expertFileName = set
							.getOption("eda_search_expertKnowledge").getResultValue(0)
							.toString();
					try {
						expertKnowledge = new ExpertKnowledge(
								new FileReader(expertFileName), data.getLabels());
					} catch (final IOException ex) {
						System.err
								.println("Error: Unable to read specified ExpertKnowledge file: "
										+ expertFileName);
						return;
					}
					if (set.isSet("eda_search_fitness")) {
						fitness = Boolean.parseBoolean(set.getOption("eda_search_fitness")
								.getResultValue(0));
					}
					if (fitness) {
						weightScheme = WeightScheme.PROPORTIONAL;
					} else {
						weightScheme = WeightScheme.RANK;
					}
					if (set.isSet("eda_search_theta")) {
						if (set.isSet("eda_search_percentMaxAttributeRange")) {
							System.out
									.println("Error: Multiple search algorithms specified!");
							return;
						}
						theta = Double.parseDouble(set.getOption("eda_search_theta")
								.getResultValue(0));
						scalingMethod = ScalingMethod.EXPONENTIAL;
						scalingParameter = theta;
					} else {
						if (set.isSet("eda_search_percentMaxAttributeRange")) {
							percentMaxAttributeRange = Double.parseDouble(set.getOption(
									"eda_search_percentMaxAttributeRange").getResultValue(0));
						}
						if ((percentMaxAttributeRange < 0.0)
								|| (percentMaxAttributeRange > 100.0)) {
							System.err
									.println("Error: eda_search_percentMaxAttributeRange must be from 0-100. You passed in: "
											+ percentMaxAttributeRange);
							return;
						}
						scalingParameter = percentMaxAttributeRange / 100.0;
						scalingMethod = ScalingMethod.LINEAR;
					}
					expertKnowledgeLoaded = true;
				} else {
					expertKnowledge = new ExpertKnowledge(data.getLabels());
					beta = 0;
					// scalingMethod = null; got Redundant assignment warning that it is already null
					scalingParameter = Double.NaN;
					// weightScheme = null; got Redundant assignment warning that it is already null
					if (set.isSet("eda_search_theta")) {
						System.out
								.println("Warning: Scaling parameter 'eda_search_theta' was passed in when no expertKnowledge file loaded and therefore will not be used.");
					}
					if (set.isSet("eda_search_percentMaxAttributeRange")) {
						System.out
								.println("Warning: Scaling parameters 'eda_search_percentMaxAttributeRange' was passed in when no expertKnowledge file loaded and therefore will not be used.");
					}
					if (set.isSet("eda_search_beta")) {
						System.out
								.println("Warning: 'eda_search_beta' was passed in when no expertKnowledge file loaded and therefore will be ignored.");
					}
				} // end if EDA but no expert knowledge file passed in
				if (!Console.minimalOutput) {
					// Report parameter upload to user
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"EDA (numAgents)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"numAgents:", Integer.toString(numAgents) })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"EDA (numUpdates)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"numUpdates:", Integer.toString(numUpdates) })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"EDA (retention)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"retention:", Double.toString(retention) })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] { "Wrapper:",
							"EDA (alpha)" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] { "alpha:",
							Integer.toString(alpha) })) + '\n');
					if (expertKnowledgeLoaded) {
						config.append(columns.format(Arrays.asList(new String[] {
								"Wrapper:", "EDA (beta)" })) + '\n');
						config.append(columns.format(Arrays.asList(new String[] { "beta:",
								Integer.toString(beta) })) + '\n');
						config.append(columns.format(Arrays.asList(new String[] {
								"Wrapper:", "EDA (expertKnowledge)" })) + '\n');
						config.append(columns.format(Arrays
								.asList(new String[] {
										"expertKnowledge:",
										set.getOption("eda_search_expertKnowledge").getResultValue(
												0) })) + '\n');
						if ((scalingMethod != null)
								&& scalingMethod.equals(ScalingMethod.LINEAR)) {
							config.append(columns.format(Arrays.asList(new String[] {
									"Wrapper:", "EDA (maxProb)" })) + '\n');
							config
									.append(columns.format(Arrays.asList(new String[] {
											"maxProb:", Double.toString(percentMaxAttributeRange) })) + '\n');
						} else {
							config.append(columns.format(Arrays.asList(new String[] {
									"Wrapper:", "EDA (theta)" })) + '\n');
							config.append(columns.format(Arrays.asList(new String[] {
									"theta:", Double.toString(theta) })) + '\n');
						}
					}
				} // end if !onEndAttributeMinimalOutput
			} // end if EDA search
				break;
			case EXHAUSTIVE:
				// nothing special needed here
				break;
			case INVALID:
			default:
				throw new RuntimeException(
						"Search parameter arguments not understood: " + searchMethod);
		} // end switch
		if (set.isSet("permutations") && (searchMethod != SearchMethod.EXHAUSTIVE)) {
			throw new RuntimeException(
					"permutations only supported for Exhaustive search, not "
							+ searchMethod);
		}
		if ((forced == null) && set.isSet("min")) {
			try {
				minLevel = Integer.parseInt(set.getOption("min").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.out.println("Error: min accepts integer values only.");
				return;
			}
			if ((minLevel < 1) || (minLevel > data.getCols() - 1)) {
				System.out.println("Error: min must be > 0 and <= number "
						+ "of attributes in the data set.");
				return;
			}
		}
		if ((forced == null) && set.isSet("max")) {
			try {
				maxLevel = Integer.parseInt(set.getOption("max").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.out.println("Error: max accepts integer values only.");
				return;
			}
			if ((maxLevel < 1) || (maxLevel > data.getCols() - 1)) {
				System.out.println("Error: max must be > 0 and <= number "
						+ "of attributes in the data set.");
				return;
			}
		}
		if (maxLevel < minLevel) {
			System.out.println("Error: max must be >= min.");
			return;
		}
		config.append(columns.format(Arrays.asList(new String[] { "Min Attr:",
				Integer.toString(minLevel) })) + '\n');
		config.append(columns.format(Arrays.asList(new String[] { "Max Attr:",
				Integer.toString(maxLevel) })) + '\n');
		config.append(columns.format(Arrays.asList(new String[] { "Paired:",
				Boolean.toString(paired) })) + '\n');
		if (set.isSet("nolandscape")) {
			Console.computeAllModelsLandscape = false;
		} else {
			Console.computeAllModelsLandscape = true;
		}
		if (set.isSet("top_models_landscape_size")) {
			try {
				Console.topModelsLandscapeSize = Integer.parseInt(set.getOption(
						"top_models_landscape_size").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.err
						.println("Error: top_models_landscape_size accepts integer values only.");
				return;
			}
		}
		// JAS support separate top_models_landscape file
		if (Main.isExperimental && set.isSet("top_models_landscape_outfile")) {
			Console.topOutFname = set.getOption("top_models_landscape_outfile")
					.getResultValue(0);
		}
		// ENDJAS
		if (set.isSet("all_models_outfile")) {
			final String landscapeOutFname = set.getOption("all_models_outfile")
					.getResultValue(0);
			Console.outputAllModels = true;
			Console.allModelsWriter = new PrintWriter(landscapeOutFname);
			Console.writeOutAllModelsHeader();
			config.append(columns.format(Arrays.asList(new String[] {
					"all_models_outfile:", landscapeOutFname })) + '\n');
		}
		boolean parallel = false;
		if (set.isSet("parallel")) {
			parallel = true;
		}
		config.append(columns.format(Arrays.asList(new String[] { "Parallel:",
				Boolean.toString(parallel) })) + '\n');
		long seed = Main.defaultRandomSeed;
		if (set.isSet("seed")) {
			try {
				seed = Long.parseLong(set.getOption("seed").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.out.println("Error: seed accepts integer values only.");
				return;
			}
		}
		config.append(columns.format(Arrays.asList(new String[] { "Random Seed:",
				Long.toString(seed) })) + '\n');
		if (set.isSet("adjust_for_covariate")) {
			final String covariateAttributeName = set.getOption(
					"adjust_for_covariate").getResultValue(0);
			try {
				data = data.adjustForCovariate(seed, covariateAttributeName);
				config.append(columns.format(Arrays.asList(new String[] {
						"Covariate adjustment:", covariateAttributeName })) + '\n');
			} catch (final Exception ex) {
				System.out.println("Covariate adjustment for attribute '"
						+ covariateAttributeName + "' in dataset '" + dataFileName
						+ "' caught an exception: " + ex.toString());
			}
		}
		if (paired && Console.useExplicitTestOfInteraction) {
			System.out
					.println("Error: permute_with_explicit_test_of_interaction is set to true when paired analysis specified. An explicitTestOfInteraction cannot be done in conjunction with a paired analysis.");
			return;
		}
		if (!Console.minimalOutput) {
			System.out.println("=== Configuration ===");
			System.out.println("Command line arguments: " + Arrays.toString(args));
			System.out.println(config.toString());
		}
		if (tableData != null) {
			System.out.print("Datafile\t");
			Console.printTableDataHeader(tableData);
			System.out.print("# CV partitions\t# Attributes\tAttributes\tCVC\t");
			Console.printConfusionMatrixInfoHeader(" overall");
			Console.printConfusionMatrixInfoHeader(" CV training");
			Console.printConfusionMatrixInfoHeader(" CV testing");
			Console.printConfusionMatrixInfoHeader(" model training");
			Console.printConfusionMatrixInfoHeader(" model testing");
			if (Console.outputModelsSignificanceMetric) {
				System.out.print("model significance\t");
			}
			if (Console.computeMdrByGenotype) {
				System.out.print(Collector.MdrByGenotypeResults
						.getTabDelimitedHeaderString());
			}
			System.out.println();
		}
		final Random rnd = new Random(seed);
		final Pair<List<Dataset>, List<Dataset>> partition = data
				.partition(cv, rnd);
		final Runnable onEndAttribute = new OnEndAttribute(dataFileName, tableData,
				data, minLevel, cv, Console.minimalOutput);
		final Runnable onEnd = new OnEnd(Console.minimalOutput, data);
		Console.analysis = Console.createAnalysisThread(set, data, minLevel,
				maxLevel, forced, evaluations, runtime, unitmult, numAgents,
				numUpdates, retention, alpha, beta, scalingMethod, scalingParameter,
				weightScheme, expertKnowledge, searchMethod, parallel, seed,
				Console.tieStatus, partition, onEndAttribute, onEnd);
		// use run() instead of start. This is so MDR is reasonably behaved when running in batch mode on supercomputer cluster
		Console.analysis.run();
		if (set.isSet("saveanalysis")) {
			try {
				final AnalysisFileManager afm = new AnalysisFileManager();
				afm.setAnalysis(data, set.getData().get(0), minLevel, maxLevel,
						Console.analysis.getCollectors(),
						Console.analysis.getAllModelsLandscape(), forced, seed,
						Console.tieStatus);
				switch (searchMethod) {
					case EDA:
						// TODO Save EDA?
						break;
					case EXHAUSTIVE:
						// nothing special needed here
						break;
					case FORCED:
						afm.putCfg(AnalysisFileManager.cfgWrapper,
								AnalysisFileManager.cfgValForced);
						break;
					case RANDOM:
						if (set.isSet("random_search_eval")) {
							afm.putCfg(AnalysisFileManager.cfgWrapper,
									AnalysisFileManager.cfgValRandom);
							afm.putCfg(AnalysisFileManager.cfgEvaluations,
									set.getOption("random_search_eval").getResultValue(0));
						} else if (set.isSet("random_search_runtime")) {
							afm.putCfg(AnalysisFileManager.cfgWrapper,
									AnalysisFileManager.cfgValRandom);
							afm.putCfg(AnalysisFileManager.cfgRuntime,
									Double.toString(runtime));
							afm.putCfg(AnalysisFileManager.cfgRuntimeUnits, units);
						}
						break;
					case INVALID:
					default:
						throw new RuntimeException(
								"Search parameter arguments not understood: " + searchMethod);
				} // end switch
				final PrintWriter p = new PrintWriter(new FileWriter(set.getOption(
						"saveanalysis").getResultValue(0)));
				afm.write(p);
				p.flush();
				p.close();
			} catch (final IOException ex) {
				System.out.println("Error: Unable to write to analysis file.");
				return;
			}
		} // end save analysis
		if (Console.outputEntropy) {
			final int numAttributes = data.getCols() - 1;
			final List<Integer> comboList = new ArrayList<Integer>(1);
			comboList.add(0);
			final List<AttributeCombination> combos = new ArrayList<AttributeCombination>(
					numAttributes);
			for (int colIndex = 0; colIndex < numAttributes; ++colIndex) {
				comboList.set(0, colIndex);
				combos.add(new AttributeCombination(comboList, data.getLabels()));
			}
			final EntropyAnalysis entropyAnalysis = new EntropyAnalysis();
			entropyAnalysis.set(combos, data);
			System.out.println(entropyAnalysis.getEntropyText(Main.defaultFormat));
		} // end if output entropy
		if (Console.permutations != 0) {
			System.out.println("\nStandard MDR analysis finished. Starting "
					+ Console.permutations + " permutations. Elapsed running time: "
					+ Console.stopWatch.getElapsedTime());
			final AnalysisThread originalAnalysis = Console.analysis;
			final List<Collector> collectors = originalAnalysis.getCollectors();
			final double[] overallAverages = new double[collectors.size()];
			final double[] modelIntervalsTrainAverages = new double[collectors.size()];
			final double[] modelIntervalsTestAverages = new double[collectors.size()];
			final double[] cvIntervalsTrainAverages = new double[collectors.size()];
			final double[] cvIntervalsTestAverages = new double[collectors.size()];
			final int[] cvcs = new int[collectors.size()];
			final double[] modelSignificance = new double[collectors.size()];
			// need to make sure all data is collected before data is permuted
			// this is because permuting can cause calculation of testing to change
			// because datasets share byte arrays
			for (int collectorIndex = 0; collectorIndex < collectors.size(); ++collectorIndex) {
				final Collector collector = collectors.get(collectorIndex);
				final BestModel bestModel = collector.getBest();
				overallAverages[collectorIndex] = bestModel.getTotal().getFitness();
				cvIntervalsTrainAverages[collectorIndex] = bestModel.getAvgTrain()
						.getFitness();
				if (cv < 2) {
					cvIntervalsTestAverages[collectorIndex] = Float.NaN;
				} else {
					cvIntervalsTestAverages[collectorIndex] = bestModel.getAvgTest()
							.getFitness();
				}
				cvcs[collectorIndex] = bestModel.getCVC();
				if (collector.getTopModelsLandscape() != null) {
					final IntervalCollection intervalsWithSameModel = collector
							.getTopModelsLandscape().getModelIntervals(
									bestModel.getModel().getCombo());
					if (intervalsWithSameModel != null) {
						modelIntervalsTrainAverages[collectorIndex] = intervalsWithSameModel
								.getAverageFitness(false /* useTesting */);
						modelIntervalsTestAverages[collectorIndex] = intervalsWithSameModel
								.getAverageFitness(true /* useTesting */);
					}
				}
				modelSignificance[collectorIndex] = bestModel.getModel()
						.getModelSignificance(data, Console.modelSignificanceMetric);
			} // end collectors loop
			final OnEndLevelForPermutation onEndLevelForPermutation = new OnEndLevelForPermutation(
					minLevel, maxLevel, data);
			final OnEndRunForPermutation onEndRunForPermutation = new OnEndRunForPermutation(
					onEndLevelForPermutation, Console.permutations);
			final Dataset.PermutationSupport permutationSupport = data
					.createPermutationSupport(Console.useExplicitTestOfInteraction);
			final Random rndPermutation = new Random(seed);
			// final long startTime = System.currentTimeMillis();
			for (int permutationCtr = 1; permutationCtr <= Console.permutations; ++permutationCtr) {
				permutationSupport.permuteData(rndPermutation);
				final Pair<List<Dataset>, List<Dataset>> permutationPartition = data
						.partition(cv, rndPermutation);
				final long partitionRandomSeed = rndPermutation.nextLong();
				// not sure why, but runs much faster with parallel turned off
				Console.analysis = Console.createAnalysisThread(set, data, minLevel,
						maxLevel, forced, evaluations, runtime, unitmult, numAgents,
						numUpdates, retention, alpha, beta, scalingMethod,
						scalingParameter, weightScheme, expertKnowledge, searchMethod,
						false /* parallel */, partitionRandomSeed, Console.tieStatus,
						permutationPartition, onEndLevelForPermutation,
						onEndRunForPermutation /* onEnd */);
				// use run() instead of start. This is so MDR is reasonably behaved when running in batch mode on supercomputer cluster
				Console.analysis.run();
			} // end for permutations
			// final long endTime = System.currentTimeMillis();
			// System.out.println(Console.permutations + " permutations completed in "
			// + Main.defaultFormat.format((endTime - startTime) / (float) 1000) + " seconds.");
			System.out.print("Datafile\t");
			if (tableData != null) {
				Console.printTableDataHeader(tableData);
			}
			System.out
					.print("useExplicitTestOfInteraction\t# permutations\t# CV partitions\t# Attributes\tAttributes\tCVC\tperm. best level avg. CVC\t");
			System.out.print(Console
					.getPermutationStatisticsHeaderString("overall bal. acc."));
			System.out.print(Console
					.getPermutationStatisticsHeaderString("testing bal. acc."));
			System.out.print(Console
					.getPermutationStatisticsHeaderString("model testing bal. acc."));
			if (Console.outputModelsSignificanceMetric) {
				System.out.print(Console
						.getPermutationStatisticsHeaderString("model significance"));
			}
			System.out.println();
			for (int collectorIndex = 0; collectorIndex < collectors.size(); ++collectorIndex) {
				final int level = minLevel + collectorIndex;
				final Collector collector = collectors.get(collectorIndex);
				final BestModel bestModel = collector.getBest();
				System.out.print(dataFileName + "\t");
				if (tableData != null) {
					System.out.print(Console.getDataTableRowString(tableData));
				}
				System.out.print(Console.useExplicitTestOfInteraction + "\t");
				System.out.print(Console.permutations + "\t");
				System.out.print(cv + "\t");
				System.out.print(level + "\t");
				System.out.print(bestModel.getModel().getCombo() + "\t");
				// System.out.print(Console.printPermutationStatistics(
				// cvIntervalsTrainAverages[collectorIndex],
				// onEndRunForPermutation.getCVIntervalsTrainAverages(level)));
				System.out.print(cvcs[collectorIndex] + "\t");
				System.out.print(Main.defaultFormat.format(onEndRunForPermutation
						.getCVCAverage()) + "\t");
				System.out.print(Console.getPermutationStatisticsString(
						overallAverages[collectorIndex],
						onEndRunForPermutation.getOverallAverages()));
				System.out.print(Console.getPermutationStatisticsString(
						cvIntervalsTestAverages[collectorIndex],
						onEndRunForPermutation.getCVIntervalsTestAverages()));
				if (collector.getTopModelsLandscape() != null) {
					// System.out.print(Console.printPermutationStatistics(
					// modelIntervalsTrainAverages[collectorIndex],
					// onEndRunForPermutation.getBestModelTrainAverages(level)));
					System.out.print(Console.getPermutationStatisticsString(
							modelIntervalsTestAverages[collectorIndex],
							onEndRunForPermutation.getBestModelTestAverages()));
				} // end if top models landscape exists
				if (Console.outputModelsSignificanceMetric) {
					System.out.print(Console.getPermutationStatisticsString(
							modelSignificance[collectorIndex], onEndRunForPermutation
									.getModelSignificances(), Console.modelSignificanceMetric
									.getModelSignificanceMetricRankOrder()));
				}
				System.out.println();
			} // end for levels
			if (!Console.minimalOutput) {
				// print out raw data for all permutations
				// the reason we include the seemingly useless 'Permutation identifier' is so that permutation results can be easily grepped out of
				// the results.
				// Multiple mdr permutation jobs could be run, each with a different seed, and a larger set of permutation results created by
				// concatenating the raw results.
				System.out
						.print("\nPermutation identifier\tMultiWayBestModelPickingMethod\tRank\tModel\tBestLevel\tCVC\tbal. acc. overall\tbal. acc. CV training\tbal. acc. Model training\tbal. acc. CV testing\tbal. acc. Model testing\t");
				if (Console.outputModelsSignificanceMetric) {
					System.out.print("model significance\t");
				}
				System.out.println();
				final ArrayList<Integer> permutationIndices = onEndRunForPermutation
						.getPermutationIndicesByRank();
				for (int rankIndex = 0; rankIndex < permutationIndices.size(); ++rankIndex) {
					final int permutationIndex = permutationIndices.get(rankIndex);
					System.out.print("Permutation seed " + seed + "\t");
					System.out.print(Console.multiWayBestModelPickingMethod + "\t");
					System.out.print(String.valueOf(rankIndex + 1) + "\t");
					System.out
							.print(onEndRunForPermutation.bestModelNames[permutationIndex]
									+ "\t");
					System.out
							.print(onEndRunForPermutation.bestModelAttributeLevels[permutationIndex]
									+ "\t");
					System.out
							.print(onEndRunForPermutation.attributeLevelsCVCs[permutationIndex]
									+ "\t");
					System.out
							.print(Main.defaultFormat
									.format(onEndRunForPermutation.attributeLevelsOverallAverages[permutationIndex])
									+ "\t");
					System.out
							.print(Main.defaultFormat
									.format(onEndRunForPermutation.attributeLevelsCVIntervalsTrainAverages[permutationIndex])
									+ "\t");
					System.out
							.print(Main.defaultFormat
									.format(onEndRunForPermutation.attributeLevelsBestModelTrainAverages[permutationIndex])
									+ "\t");
					System.out
							.print(Main.defaultFormat
									.format(onEndRunForPermutation.attributeLevelsCVIntervalsTestAverages[permutationIndex])
									+ "\t");
					System.out
							.print(Main.defaultFormat
									.format(onEndRunForPermutation.attributeLevelsBestModelTestAverages[permutationIndex])
									+ "\t");
					if (Console.outputModelsSignificanceMetric) {
						System.out
								.print(Main.modelSignificanceFormat
										.format(onEndRunForPermutation.attributeLevelsModelSignificance[permutationIndex])
										+ "\t");
					}
					System.out.println();
				}
			} // end if not minimal output
			Console.analysis = originalAnalysis; // not that it matters but...
		} // end if running permutations
	}

	private static void runFilter(final String[] args, final OptionSet set)
			throws Exception {
		final StringBuffer config = new StringBuffer();
		final String filter = set.getOption("filter").getResultValue(0);
		final ColumnFormat columns = new ColumnFormat(Arrays.asList(new Integer[] {
				new Integer(20), new Integer(59) }));
		final String dataFileName = set.getData().get(0);
		final Dataset data = Console.openDataSet(dataFileName, false);
		AbstractAttributeScorer scorer = null;
		boolean parallel = false;
		boolean ascending = false;
		int criterion = 0;
		double percentMaxAttributeRange = Main.defaultEDAPercentMaxAttributeRange;
		double theta = Main.defaultEDAExponentialTheta;
		long seed = Main.defaultRandomSeed;
		Number critValue = new Integer(Main.defaultCriterionFilterTopN);
		config.append(columns.format(Arrays.asList(new String[] { "Input File:",
				set.getData().get(0) })) + '\n');
		if (set.isSet("seed")) {
			try {
				seed = Long.parseLong(set.getOption("seed").getResultValue(0));
			} catch (final NumberFormatException ex) {
				System.out.println("Error: seed accepts integer values only.");
				return;
			}
		}
		config.append(columns.format(Arrays.asList(new String[] { "Random Seed:",
				Long.toString(seed) })) + '\n');
		if (set.isSet("select_")) {
			final String sCriterion = set.getOption("select_").getResultDetail(0);
			final String sCritValue = set.getOption("select_").getResultValue(0);
			if (sCriterion.equalsIgnoreCase("N")) {
				try {
					critValue = Integer.valueOf(sCritValue);
				} catch (final NumberFormatException ex) {
					System.out.println("Error: select_N accepts integer values "
							+ "only.");
					return;
				}
				if ((critValue.intValue() <= 0)
						|| (critValue.intValue() > data.getCols() - 1)) {
					System.err.println("Error: N must be > 0 and <= number of "
							+ "attributes in the data set.");
					return;
				}
				criterion = 0;
				config.append(columns.format(Arrays.asList(new String[] { "Selection:",
						"Top N (" + critValue.toString() + ')' })) + '\n');
			} else if (sCriterion.equalsIgnoreCase("PCT")) {
				try {
					critValue = Double.valueOf(sCritValue);
				} catch (final NumberFormatException ex) {
					System.err.println("Error: select_PCT accepts numeric "
							+ "values only.");
					return;
				}
				if ((critValue.doubleValue() <= 0) || (critValue.doubleValue() > 100)) {
					System.err.println("Error: PCT must be > 0 and <= 100");
					return;
				}
				criterion = 1;
				config.append(columns.format(Arrays.asList(new String[] { "Selection:",
						"Top Percent (" + critValue.toString() + "%)" })) + '\n');
			} else if (sCriterion.equalsIgnoreCase("THRESH")) {
				try {
					critValue = Double.valueOf(sCritValue);
				} catch (final NumberFormatException ex) {
					System.err.println("Error: select_THRESH accepts numeric "
							+ "values only.");
					return;
				}
				criterion = 2;
				config.append(columns.format(Arrays.asList(new String[] { "Selection:",
						"Threshold (" + critValue.toString() + ")" })) + '\n');
			} else {
				System.err.println("Error: Invalid selection criterion: " + sCriterion
						+ "\n");
				return;
			}
		} else {
			config.append(columns.format(Arrays.asList(new String[] { "Selection:",
					"Top N (" + critValue.toString() + ')' })) + '\n');
		}
		if (set.isSet("parallel")) {
			parallel = true;
		}
		config.append("Parallel:           " + Boolean.toString(parallel) + '\n');
		if (filter.equalsIgnoreCase("ChiSquared")) {
			boolean pvalue = false;
			if (set.isSet("chisq_pvalue")) {
				pvalue = true;
				ascending = true;
			}
			if (set.isSet("relieff_neighbors") || set.isSet("relieff_samplesize")) {
				System.out
						.println("Warning: ReliefF parameters ignored for ChiSquared filter.");
				System.out.println();
			}
			if (set.isSet("turf_pct")) {
				System.out
						.println("Warning: TuRF parameters ignored for ChiSquared filter.");
				System.out.println();
			}
			scorer = new ChiSquaredScorer(data, pvalue, parallel);
			config.append(columns.format(Arrays.asList(new String[] { "Filter:",
					"\u03a7\u00b2" })) + '\n');
			config.append(columns.format(Arrays.asList(new String[] { "Use P-Value:",
					Boolean.toString(pvalue) })) + '\n');
		} else if (filter.equalsIgnoreCase("OddsRatio")) {
			ascending = false;
			if (set.isSet("relieff_neighbors") || set.isSet("relieff_samplesize")) {
				System.out
						.println("Warning: ReliefF parameters ignored for OddsRatio filter.");
				System.out.println();
			}
			if (set.isSet("chisq_pvalue")) {
				System.out
						.println("Warning: ChiSquared parameters ignored for OddsRatio filter.");
				System.out.println();
			}
			if (set.isSet("turf_pct")) {
				System.out
						.println("Warning: TuRF parameters ignored for OddsRatio filter.");
				System.out.println();
			}
			scorer = new OddsRatioScorer(data, parallel);
			config.append(columns.format(Arrays.asList(new String[] { "Filter:",
					"OddsRatio" })) + '\n');
		} else {
			int neighbors = Math.min(Main.defaultReliefFNeighbors, data.getRows());
			if (set.isSet("relieff_neighbors")) {
				try {
					neighbors = Integer.parseInt(set.getOption("relieff_neighbors")
							.getResultValue(0));
				} catch (final NumberFormatException ex) {
					System.err
							.println("Error: relieff_neighbors accepts integer values only.");
					return;
				}
				if ((neighbors < 1) || (neighbors > data.getRows())) {
					System.err
							.println("Error: relieff_neighbors must be > 0 and <= number of instances in the data set ");
					return;
				}
			}
			int sampleSize = data.getRows();
			if (set.isSet("relieff_samplesize")) {
				try {
					sampleSize = Integer.parseInt(set.getOption("relieff_samplesize")
							.getResultValue(0));
				} catch (final NumberFormatException ex) {
					System.err
							.println("Error: relieff_samplesize accepts integer values only.");
					return;
				}
				if ((sampleSize < 1) || (sampleSize > data.getRows())) {
					System.out
							.println("Warning: relieff_samplesize was not set in range of 1 to number of data rows and will be changed to be all rows ");
					sampleSize = data.getRows();
				}
			}
			if (set.isSet("chisq_pvalue")) {
				System.out.println("Warning: ChiSquared parameters ignored for "
						+ filter + " filter.");
				System.out.println();
			}
			if (filter.toUpperCase().contains("TURF")) {
				float pct = Main.defaultTuRFPct / 100.0f;
				if (set.isSet("turf_pct")) {
					try {
						pct = Float.parseFloat(set.getOption("turf_pct").getResultValue(0));
					} catch (final NumberFormatException ex) {
						System.err.println("Error: turf_pct accepts "
								+ "float values only.");
						return;
					}
					if ((pct <= 0) || (pct > 1)) {
						System.err.println("Error: turf_pct must be > 0 and <= 1.");
						return;
					}
				}
				config.append(columns.format(Arrays.asList(new String[] { "Filter:",
						filter })) + '\n');
				config.append(columns.format(Arrays.asList(new String[] { "Pct:",
						Float.toString(pct) })) + '\n');
				if (filter.equalsIgnoreCase("TuRF")) {
					config.append(columns.format(Arrays.asList(new String[] {
							"Sample Size:", Integer.toString(sampleSize) })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"Nearest Neighbors:", Integer.toString(neighbors) })) + '\n');
					scorer = new TuRFAttributeScorer(data, sampleSize, neighbors, pct,
							new Random(seed), parallel);
				} else if (filter.equalsIgnoreCase("SURFnTuRF")) {
					scorer = new SURFnTuRFAttributeScorer(data, pct, new Random(seed),
							parallel);
				} else if (filter.equalsIgnoreCase("SURF*NTURF")
						|| filter.equalsIgnoreCase("SURFStarNTURF")) {
					scorer = new SURFStarnTuRFAttributeScorer(data, pct,
							new Random(seed), parallel);
				}
			} else {
				if (set.isSet("turf_pct")) {
					System.out.println("Warning: TuRF parameters ignored for" + filter
							+ " filter.");
					System.out.println();
				}
				if (filter.equalsIgnoreCase("ReliefF")) {
					scorer = new ReliefFAttributeScorer(data, sampleSize, neighbors,
							new Random(seed), parallel);
					config.append(columns.format(Arrays.asList(new String[] { "Filter:",
							"ReliefF" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"Nearest Neighbors:", Integer.toString(neighbors) })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"Sample Size:", Integer.toString(sampleSize) })) + '\n');
				} else if (filter.equalsIgnoreCase("SURF")) {
					scorer = new SURFAttributeScorer(data, sampleSize, new Random(seed),
							parallel);
					config.append(columns.format(Arrays.asList(new String[] { "Filter:",
							"SURF" })) + '\n');
					config.append(columns.format(Arrays.asList(new String[] {
							"Sample Size:", Integer.toString(sampleSize) })) + '\n');
				} else if (filter.equalsIgnoreCase("SURF*")
						|| filter.equalsIgnoreCase("SURFStar")) {
					scorer = new SURFStarAttributeScorer(data, new Random(seed), parallel);
					config.append(columns.format(Arrays.asList(new String[] { "Filter:",
							"SURF*" })) + '\n');
				} else {
					System.out.println("Unknown filter type: " + filter + "\n");
					return;
				}
			}
		} // end if a reliefF family filter
		if (Main.isExperimental) {
			if (set.isSet("preserve_turf_removal_order")) {
				Console.preserveTuRFRemovalOrder = Boolean.parseBoolean(set.getOption(
						"preserve_turf_removal_order").getResultValue(0));
				config.append(columns.format(Arrays.asList(new String[] {
						"Order by survival:",
						String.valueOf(Console.preserveTuRFRemovalOrder) })) + '\n');
			}
			if (set.isSet("relieff_weighted_distance_iterations")) {
				try {
					Console.reliefFWeightedDistanceIterations = Integer.parseInt(set
							.getOption("relieff_weighted_distance_iterations")
							.getResultValue(0));
					config
							.append(columns.format(Arrays.asList(new String[] {
									"Iterations:",
									String.valueOf(Console.reliefFWeightedDistanceIterations) })) + '\n');
				} catch (final NumberFormatException ex) {
					System.err
							.println("Error: relieff_weighted_distance_iterations accepts integer values only.");
					return;
				}
			}
			if (Console.reliefFWeightedDistanceIterations > 0) {
				if (set.isSet("relieff_weighted_distance_method")) {
					final String reliefFWeightedDistanceString = set.getOption(
							"relieff_weighted_distance_method").getResultValue(0);
					try {
						Console.reliefFWeightedDistanceMethod = Enum.valueOf(
								WeightScheme.class, reliefFWeightedDistanceString);
						config.append(columns.format(Arrays.asList(new String[] {
								"Weighting method:",
								Console.reliefFWeightedDistanceMethod.toString() })) + '\n');
					} catch (final IllegalArgumentException ex) {
						System.err
								.println("Error: unknown relieff_weighted_distance_method. Legal values: "
										+ Arrays.toString(WeightScheme.values())
										+ ". Passed in value: " + reliefFWeightedDistanceString);
						return;
					}
				} // end weighted distance method
				if (set.isSet("relieff_weighted_scaling_method")) {
					final String reliefFWeightedScalingString = set.getOption(
							"relieff_weighted_scaling_method").getResultValue(0);
					try {
						Console.reliefFWeightedScalingMethod = Enum.valueOf(
								ScalingMethod.class, reliefFWeightedScalingString);
						config.append(columns.format(Arrays.asList(new String[] {
								"Scaling method:",
								Console.reliefFWeightedScalingMethod.toString() })) + '\n');
					} catch (final IllegalArgumentException ex) {
						System.err
								.println("Error: unknown relieff_weighted_scaling_method. Legal values: "
										+ Arrays.toString(ScalingMethod.values())
										+ ". Passed in value: " + reliefFWeightedScalingString);
						return;
					}
				} // end weighted scaling method
				switch (Console.reliefFWeightedScalingMethod) {
					case LINEAR:
						if (set
								.isSet("relieff_weighted_scaling_linear_percentMaxAttributeRange")) {
							try {
								percentMaxAttributeRange = Double.parseDouble(set.getOption(
										"relieff_weighted_scaling_linear_percentMaxAttributeRange")
										.getResultValue(0));
							} catch (final NumberFormatException ex) {
								System.err
										.println("Error: relieff_weighted_scaling_linear_percentMaxAttributeRange accepts values from 0-100 only.");
								return;
							}
							if ((percentMaxAttributeRange < 0.0)
									|| (percentMaxAttributeRange > 100.0)) {
								System.err
										.println("Error: relieff_weighted_scaling_linear_percentMaxAttributeRange must be from 0-100. You passed in: "
												+ percentMaxAttributeRange);
								return;
							}
						}
						Console.reliefFWeightedScalingParameter = percentMaxAttributeRange / 100.0;
						break;
					case EXPONENTIAL:
						if (set.isSet("relieff_weighted_scaling_exponential_theta")) {
							final String optionString = set.getOption(
									"relieff_weighted_scaling_exponential_theta").getResultValue(
									0);
							try {
								theta = Double.parseDouble(optionString);
								if ((theta < 0) || (theta > 1)) {
									System.err
											.println("Error: relieff_weighted_scaling_exponential_theta must be >= 0 and <= 1. You passed in: "
													+ optionString);
									return;
								}
							} catch (final NumberFormatException ex) {
								System.err
										.println("Error: relieff_weighted_scaling_exponential_theta accepts double values only. You passed in: "
												+ optionString);
								return;
							}
						}
						Console.reliefFWeightedScalingParameter = theta;
						break;
					default:
						throw new IllegalArgumentException(
								"Unhandled value for Console.reliefFWeightedScalingMethod: "
										+ Console.reliefFWeightedScalingMethod);
						// break;
				} // end switch (Console.reliefFWeightedScalingMethod)
			} // end if # iterations > 0
			if (set.isSet("relieff_rebalancing_method")) {
				final String reliefFRebalancingMethodString = set.getOption(
						"relieff_rebalancing_method").getResultValue(0);
				try {
					Console.reliefFRebalancingMethod = Enum.valueOf(
							ReliefFRebalancingMethod.class, reliefFRebalancingMethodString);
				} catch (final IllegalArgumentException ex) {
					System.out
							.println("Error: unknown relieff_rebalancing_method. Legal values: "
									+ Arrays.toString(ReliefFRebalancingMethod.values())
									+ ". Passed in value: '"
									+ reliefFRebalancingMethodString
									+ "'");
					return;
				}
			}
		} // end is experimental
		if (set.isSet("minimal_output")) {
			Console.minimalOutput = Boolean.parseBoolean(set.getOption(
					"minimal_output").getResultValue(0));
		}
		File output = null;
		if (set.getData().size() < 2) {
			if (!Console.minimalOutput) {
				System.out
						.println("Warning: No output file was passed in so a filtered dataset will not be created.");
			}
		} else {
			final String outputFileName = set.getData().get(1);
			output = new File(outputFileName);
			if (!output.getAbsoluteFile().getParentFile().canWrite()
					&& !output.canWrite()) {
				System.err.println("Error: Unable to open '" + outputFileName
						+ "' for writing.");
				return;
			}
			config.append("Output file:        " + outputFileName + '\n');
		}
		if (!Console.minimalOutput) {
			System.out.println("=== Configuration ===");
			System.out.println("Command line arguments: " + Arrays.toString(args));
			System.out.println(config.toString());
		}
		final AttributeRanker ranker = new AttributeRanker();
		AttributeCombination combo = null;
		ranker.setScorer(scorer);
		switch (criterion) {
			// top N
			case 0:
				combo = ranker.selectN(critValue.intValue(), ascending);
				break;
			// top %
			case 1:
				combo = ranker.selectPct(critValue.doubleValue() / 100.0, ascending);
				break;
			// THRESH
			case 2:
				combo = ranker.selectThreshold(critValue.doubleValue(), !ascending,
						true);
				break;
			default:
				throw new RuntimeException(
						"criterion is expected to be 0,1, or 2 but is: " + criterion);
				// break;
		}
		System.out.println("=== Scores ===");
		int outputNumber = 0;
		final StringBuffer experimentalSummary = new StringBuffer();
		for (final Pair<Integer, Double> p : ranker.getSortedScores()) {
			final String attributeName = data.getLabels().get(p.getFirst());
			System.out.print(attributeName);
			System.out.print('\t');
			System.out.print(p.getSecond());
			System.out.print('\t');
			System.out.println(++outputNumber);
			if (Main.isExperimental
					&& (attributeName.equals("X0") || attributeName.equals("X1"))) {
				experimentalSummary.append(attributeName + ": rank: " + outputNumber
						+ "\n");
			}
		}
		if (Main.isExperimental && (experimentalSummary.length() > 0)) {
			System.out.println("Experimental output for standard test files:\n"
					+ experimentalSummary.toString());
		}
		if (Console.minimalOutput) {
			if ((output != null) && (combo.size() > 0)) {
				try {
					data.filter(combo.getAttributeIndices())
							.write(new FileWriter(output));
				} catch (final IOException ex) {
					System.err.println("Error: cannot write to output file.\n");
					return;
				}
			}
		} else {
			System.out.println("\n=== Selected Attributes ===\n");
			if (combo.size() == 0) {
				System.out
						.println("No attributes selected. Output file not written.\n");
			} else {
				System.out.println(combo);
				System.out.println();
				if (output != null) {
					try {
						data.filter(combo.getAttributeIndices()).write(
								new FileWriter(output));
					} catch (final IOException ex) {
						System.err.println("Error: cannot write to output file.\n");
						return;
					}
					System.out.print("'");
					System.out.print(output);
					System.out.println("' successfully written.\n");
				}
			}
		} // end if !minimal_output
		// Write out scores in format suitable for use by CES as expert knowledge scores
		if (Main.isExperimental && (output != null)) {
			try {
				final File expertKnowledgeScoresFile = new File(filter + "_scores_for_"
						+ new File(dataFileName).getName());
				final FileWriter expertKnowledgeScoresFileWriter = new FileWriter(
						expertKnowledgeScoresFile);
				System.out
						.println("Writing out CES expert knowledge file (scores in attribute order): "
								+ expertKnowledgeScoresFile.getCanonicalPath());
				for (final double score : ranker.getUnsortedScores()) {
					expertKnowledgeScoresFileWriter.append(score + "\n");
				}
				expertKnowledgeScoresFileWriter.close();
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		} // end if isExperimental
	} // end runFilter

	public static synchronized void writeOutAllModel(
			final AttributeCombination attributeCombo, final int numCVIntervals,
			final float trainingBalancedAccuracy, final float testingBalancedAccuracy) {
		Console.allModelsWriter.print(attributeCombo.size() + "\t");
		Console.allModelsWriter.print(numCVIntervals + "\t");
		Console.allModelsWriter.print(attributeCombo.getComboString() + "\t");
		Console.allModelsWriter.print(Main.defaultFormat
				.format(trainingBalancedAccuracy) + "\t");
		Console.allModelsWriter.println(Main.defaultFormat
				.format(testingBalancedAccuracy) + "\t");
	}

	private static void writeOutAllModelsHeader() {
		Console.allModelsWriter
				.println("numAttributes\tnumCVIntervals\tmodelAttributes\ttrainingBalAcc\ttestingBalAcc");
	}

	public enum AmbiguousCellStatus {
		AFFECTED("Affected"), UNAFFECTED("Unaffected"), UNASSIGNED("Unassigned");
		private final String displayName;

		public static Console.AmbiguousCellStatus getTiePriorityFromString(
				String tieStatusString) throws IllegalArgumentException {
			tieStatusString = tieStatusString.toUpperCase();
			Console.AmbiguousCellStatus resultTieStatus = null;
			if (tieStatusString.equals("UNKNOWN")) {
				resultTieStatus = Console.AmbiguousCellStatus.UNASSIGNED;
			} else {
				resultTieStatus = Enum.valueOf(AmbiguousCellStatus.class,
						tieStatusString);
			}
			return resultTieStatus;
		}

		private AmbiguousCellStatus(final String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	public enum FitnessCriteriaOrder {
		CVC_INTERVAL_TRAINING("CVC-Training", FitnessCriterion.CVC,
				FitnessCriterion.TRAINING_INTERVALS), CVC_INTERVAL_TESTING(
				"CVC-Testing", FitnessCriterion.CVC, FitnessCriterion.TESTING_INTERVALS), CVC_TOP_TRAINING(
				"CVC-Top Training", FitnessCriterion.CVC, FitnessCriterion.TRAINING_ALL), CVC_TOP_TESTING(
				"CVC-Top Testing", FitnessCriterion.CVC, FitnessCriterion.TESTING_ALL), TOP_TRAINING_CVC(
				"Top Training-CVC", FitnessCriterion.TRAINING_ALL, FitnessCriterion.CVC), TOP_TESTING_CVC(
				"Top Testing-CVC", FitnessCriterion.TESTING_ALL, FitnessCriterion.CVC), MODEL_SIGNIFICANCE_CVC(
				"Model Significance-CVC", FitnessCriterion.MODEL_SIGNIFICANCE,
				FitnessCriterion.CVC)
		/*
		 * , INTERVAL_TRAINING_CVC( "Training-CVC", FitnessCriterion.TRAINING_INTERVALS, FitnessCriterion.CVC),
		 * INTERVAL_TESTING_CVC("Testing-CVC", FitnessCriterion.TESTING_INTERVALS, FitnessCriterion.CVC), INTERVAL_TRAINING_INTERVAL_TESTING(
		 * "Training-Testing", FitnessCriterion.TRAINING_INTERVALS, FitnessCriterion.TESTING_INTERVALS), INTERVAL_TESTING_INTERVAL_TRAINING(
		 * "Testing-Training", FitnessCriterion.TESTING_INTERVALS, FitnessCriterion.TRAINING_INTERVALS), TOP_TRAINING_CVC( "Top Training-CVC",
		 * FitnessCriterion.TRAINING_ALL, FitnessCriterion.CVC), TOP_TESTING_CVC( "Top Testing-CVC", FitnessCriterion.TESTING_ALL,
		 * FitnessCriterion.CVC), TOP_TRAINING_TESTING( "Top Training-Top Testing", FitnessCriterion.TRAINING_ALL,
		 * FitnessCriterion.TESTING_ALL), TOP_TESTING_TRAINING( "Top Testing-Top Training", FitnessCriterion.TESTING_ALL,
		 * FitnessCriterion.TRAINING_ALL)
		 */;
		private final String displayName;
		private final FitnessCriterion primaryTest;
		private final FitnessCriterion tieBreakerTest;
		private final static Map<String, FitnessCriteriaOrder> displayTypeLookupMap = new HashMap<String, FitnessCriteriaOrder>();
		static {
			for (final FitnessCriteriaOrder displayType : FitnessCriteriaOrder
					.values()) {
				FitnessCriteriaOrder.displayTypeLookupMap.put(displayType.toString(),
						displayType);
			}
		}

		public static FitnessCriteriaOrder[] getMenuItems() {
			return new FitnessCriteriaOrder[] { CVC_INTERVAL_TRAINING,
					CVC_INTERVAL_TESTING, TOP_TRAINING_CVC, TOP_TESTING_CVC,
					CVC_TOP_TRAINING, CVC_TOP_TESTING, MODEL_SIGNIFICANCE_CVC /*
																																		 * , INTERVAL_TRAINING_CVC, INTERVAL_TESTING_CVC,
																																		 * INTERVAL_TRAINING_INTERVAL_TESTING,
																																		 * INTERVAL_TESTING_INTERVAL_TRAINING, TOP_TRAINING_TESTING,
																																		 * TOP_TESTING_TRAINING
																																		 */};
		}

		public static FitnessCriteriaOrder lookup(final String pDisplayName) {
			return FitnessCriteriaOrder.displayTypeLookupMap.get(pDisplayName);
		}

		FitnessCriteriaOrder(final String pDisplayName,
				final FitnessCriterion pPrimaryTest,
				final FitnessCriterion pTieBreakerTest) {
			displayName = pDisplayName;
			primaryTest = pPrimaryTest;
			tieBreakerTest = pTieBreakerTest;
		}

		public FitnessCriterion getPrimaryTest() {
			return primaryTest;
		}

		public FitnessCriterion getTieBreakerTest() {
			return tieBreakerTest;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	public enum FitnessCriterion {
		CVC, TRAINING_INTERVALS, TESTING_INTERVALS, TRAINING_ALL, TESTING_ALL, MODEL_SIGNIFICANCE;
	}

	public enum MetricCellComparisonScope {
		LOCAL, GLOBAL_INDEPENDENT, GLOBAL_DEPENDENT;
	}

	public enum MetricDiffFromExpectedWeightingMethod {
		IDENTITY, SQUARED;
	}

	public enum MetricNormalizationMethod {
		DIVIDE_BY_SUM_WEIGHTED_CELLS, NONE, DIVIDE_BY_CELL_COUNT;
	}

	/**
	 * used for telling whether a significance metric is better when low (ASCENDING) like a probability or better when high (DESCENDING) such
	 * as a count or balanced accuracy
	 * @author pandrews
	 */
	public enum MetricRankOrder {
		ASCENDING, DESCENDING;
		/**
		 * true if firstValue betterThan or equal to secondValue
		 * @param permutationValue
		 * @param bestModelValue
		 * @return
		 */
		public boolean isBetterThanOrEqual(final Double permutationValue,
				final double bestModelValue) {
			boolean betterThanOrEqualTo;
			final int compareResult = permutationValue.compareTo(bestModelValue);
			switch (this) {
				case ASCENDING:
					betterThanOrEqualTo = compareResult <= 0;
					break;
				case DESCENDING:
					betterThanOrEqualTo = compareResult >= 0;
					break;
				default:
					throw new IllegalArgumentException("Unhandled MetricRankOrder: "
							+ this);
			}
			return betterThanOrEqualTo;
		}
	}

	public enum ModelSignificanceMetric {
		MAXIMUM_LIKELIHOOD, MAXIMUM_LIKELIHOOD_AFFECTED_VS_UNAFFECTED, G_TEST, FISHERS_EXACT, FISHERS_EXACT_MULTIPLY, FISHERS_EXACT_MULTIPLY_WEIGHTED, BINOMIAL, HERITABILITY_FROM_ODDS_RATIO, HERITABILITY_FROM_COUNTS, CHI_SQUARED, CHI_SQUARED_OBSERVED_AFFECTED_VS_EXPECTED_AFFECTED, CHI_SQUARED_AFFECTED_VS_UNAFFECTED, TTEST, HOMOSCEDASTIC_TTEST, COVARIANCE, COVARIANCE_AFFECTED_VS_UNAFFECTED;
		private final MetricRankOrder metricRankOrder;

		private ModelSignificanceMetric() {
			this(MetricRankOrder.DESCENDING);
		}

		private ModelSignificanceMetric(final MetricRankOrder metricRankOrder) {
			this.metricRankOrder = metricRankOrder;
		}

		public MetricRankOrder getModelSignificanceMetricRankOrder() {
			return metricRankOrder;
		}
	}

	private enum MultiWayBestModelPickingMethod {
		Testing_CVC_Parsimony, CVC_Testing_Parsimony;
	}

	private static class OnEnd implements Runnable {
		private final boolean onEndMinimalOutput;
		private final Dataset data;

		public OnEnd(final boolean pMinimalOutput, final Dataset data) {
			onEndMinimalOutput = pMinimalOutput;
			this.data = data;
		}

		public void run() {
			if (!onEndMinimalOutput) {
				if (Console.analysis.getAllModelsLandscape() != null) {
					System.out.println("### Fitness Landscape ###\n");
					System.out.println("Attributes\tbal. acc. Model training\t");
					for (final Pair<String, Float> p : Console.analysis
							.getAllModelsLandscape().getLandscape()) {
						System.out.print(p.getFirst());
						System.out.print('\t');
						System.out.println(Main.defaultFormat.format(p.getSecond()));
					}
				}
				if (Console.analysis.getCollectors().get(0).getTopModelsLandscape() != null) {
					if (Console.topOutFname != null) {
						Console.printTopOutFile(); // JAS
					}
					System.out.println("\n\n### Top Models ###");
					System.out.print("Attributes\tbal. acc. Model training\t");
					if (Console.outputTopModelsAverageTesting) {
						System.out.print("bal. acc. Model testing\t");
					}
					if (Console.outputModelsSignificanceMetric) {
						System.out.print("model significance\t");
					}
					System.out.println();
					for (final Collector collector : Console.analysis.getCollectors()) {
						for (final Pair<Pair<String, Float>, IntervalCollection> topModelData : collector
								.getTopModelsLandscape().getLandscapeWithAllDetails()) {
							final Pair<String, Float> nameAndAverageTraining = topModelData
									.getFirst();
							System.out.print(nameAndAverageTraining.getFirst());
							System.out.print('\t');
							System.out.print(Main.defaultFormat.format(nameAndAverageTraining
									.getSecond()));
							System.out.print('\t');
							if (Console.outputTopModelsAverageTesting) {
								final IntervalCollection intervalsWithSameModel = topModelData
										.getSecond();
								System.out.print(Main.defaultFormat
										.format(intervalsWithSameModel
												.getAverageFitness(true /* useTesting */)));
								System.out.print('\t');
							}
							if (Console.outputModelsSignificanceMetric) {
								final IntervalCollection intervalsWithSameModel = topModelData
										.getSecond();
								final Model model = intervalsWithSameModel.get(0).getModel();
								System.out
										.print(Main.modelSignificanceFormat.format(model
												.getModelSignificance(data,
														Console.modelSignificanceMetric)));
								System.out.print('\t');
							}
							System.out.println();
						} // end for loop
						System.out.println();
					}
				}
			}
			System.out.flush();
		}
	}

	private static class OnEndAttribute implements Runnable {
		private final Dataset data;
		private int attrCount;
		private final int intervals;
		private final boolean onEndAttributeMinimalOutput;
		private final String dataFileName;
		private final List<Pair<String, String>> tableData;

		public OnEndAttribute(final String dataFileName,
				final List<Pair<String, String>> tableData, final Dataset data,
				final int initAttr, final int intervals, final boolean pMinimalOutput) {
			this.dataFileName = dataFileName;
			this.tableData = tableData;
			this.data = data;
			this.intervals = intervals;
			attrCount = initAttr;
			onEndAttributeMinimalOutput = pMinimalOutput;
		}

		public void run() {
			final Collector coll = Console.analysis.getCollectors().get(
					Console.analysis.getCollectors().size() - 1);
			final BestModel bestModel = coll.getBest();
			if (bestModel == null) {
				System.out.println("Error: for dataset '" + dataFileName
						+ "' no attribute combination had good data when looking for "
						+ attrCount + "-way models.");
			} else {
				if (tableData != null) {
					final StringBuilder sb = new StringBuilder();
					sb.append(dataFileName);
					sb.append("\t");
					sb.append(Console.getDataTableRowString(tableData));
					final AttributeCombination bestModelAttributeCombination = bestModel
							.getModel().getCombo();
					sb.append(intervals);
					sb.append("\t");
					sb.append(bestModelAttributeCombination.size());
					sb.append("\t");
					sb.append(bestModelAttributeCombination);
					sb.append("\t");
					sb.append(bestModel.getCVC());
					sb.append("\t");
					sb.append(Console.getConfusionMatrixInfoString(bestModel.getTotal(),
							data.getAffectedStatus()));
					sb.append(Console.getConfusionMatrixInfoString(
							bestModel.getAvgTrain(), data.getAffectedStatus()));
					sb.append(Console.getConfusionMatrixInfoString(
							bestModel.getAvgTest(), data.getAffectedStatus()));
					final IntervalCollection intervalsWithSameModel = coll
							.getTopModelsLandscape().getModelIntervals(
									bestModelAttributeCombination);
					if (intervalsWithSameModel != null) {
						sb.append(Console.getConfusionMatrixInfoString(
								intervalsWithSameModel
										.getAverageFitnessConfusionMatrix(false /* useTesting */),
								data.getAffectedStatus()));
						if (intervals > 1) {
							sb.append(Console.getConfusionMatrixInfoString(
									intervalsWithSameModel
											.getAverageFitnessConfusionMatrix(true /* useTesting */),
									data.getAffectedStatus()));
						} else {
							sb.append(Console.getConfusionMatrixInfoString(null,
									data.getAffectedStatus()));
						}
					} else {
						sb.append(Console.getConfusionMatrixInfoString(null,
								data.getAffectedStatus()));
						sb.append(Console.getConfusionMatrixInfoString(null,
								data.getAffectedStatus()));
					}
					if (Console.outputModelsSignificanceMetric) {
						final Model model = bestModel.getModel();
						sb.append(Main.modelSignificanceFormat.format(model
								.getModelSignificance(data, Console.modelSignificanceMetric)));
						sb.append("\t");
					}
					if (Console.computeMdrByGenotype) {
						final List<MdrByGenotypeResults> mdrByGenotypeResults = coll
								.getMdrByGenotypeResults();
						if ((mdrByGenotypeResults == null)
								|| (mdrByGenotypeResults.size() == 0)) {
							System.out.println(sb.toString());
						} else {
							for (final MdrByGenotypeResults mdrByGenotypeResult : mdrByGenotypeResults) {
								System.out.println(sb.toString()
										+ mdrByGenotypeResult.getTabDelimitedRowString());
							}
						}
					} else {
						System.out.println(sb.toString());
					}
				} else if (onEndAttributeMinimalOutput) {
					System.out.println(coll.getBest().getModel().getCombo());
				} else {
//					System.out.println("### " + attrCount + "-Attribute Results ###");
//					System.out.println("\n=== Best Model ===\n");
//					System.out
//							.println(new BestModelTextGenerator(data, Console.tieStatus,
//									Console.scoringMethod, coll.getBest(), intervals,
//									Main.defaultFormat, Main.pValueTol, true /* verbose */));
//					System.out.println("=== If-Then Rules ===\n");
//					System.out
//							.println(new IfThenRulesTextGenerator(data, coll.getBest()));
//					if (intervals > 1) {
//						System.out.println("\n=== Cross-Validation Results ===\n");
//						System.out.println(new CVResultsTextGenerator(coll,
//								Console.tieStatus, Console.scoringMethod, data
//										.getAffectedStatus(), Main.defaultFormat, Main.pValueTol,
//								true /* verbose */));
//					}
				}
				System.out.flush();
			}
			attrCount++;
		} // end run()
	} // end OnAttribute private class

	private static class OnEndLevelForPermutation implements Runnable {
		private final float[] attributeLevelsOverallAverages;
		private final float[] attributeLevelsCVIntervalsTrainAverages;
		private final float[] attributeLevelsCVIntervalsTestAverages;
		private final float[] attributeLevelsBestModelTrainAverages;
		private final float[] attributeLevelsBestModelTestAverages;
		private final String[] bestModelNames;
		private final int[] attributeLevelsCVCs;
		private final int minAttr;
		private final double[] attributeLevelsModelSignificance;
		private final Dataset data;

		public OnEndLevelForPermutation(final int minAttr, final int maxAttr,
				final Dataset data) {
			this.data = data;
			this.minAttr = minAttr;
			final int numCollectors = (maxAttr - minAttr) + 1;
			attributeLevelsOverallAverages = new float[numCollectors];
			attributeLevelsCVIntervalsTrainAverages = new float[numCollectors];
			attributeLevelsCVIntervalsTestAverages = new float[numCollectors];
			attributeLevelsCVCs = new int[numCollectors];
			attributeLevelsBestModelTrainAverages = new float[numCollectors];
			attributeLevelsBestModelTestAverages = new float[numCollectors];
			bestModelNames = new String[numCollectors];
			attributeLevelsModelSignificance = new double[numCollectors];
		}

		/**
		 * For each permutation, the way to pick best model among the different sized attribute combinations is to decide based on these items
		 * in order: 1. highest testing fitness 2. highest cvc 3. lowest number of attributes (level)
		 * @return
		 */
		public int getBestModelAttributeLevel() {
			final ArrayList<Integer> levels = new ArrayList<Integer>();
			for (int collectorIndex = 0; collectorIndex < bestModelNames.length; ++collectorIndex) {
				levels.add(collectorIndex);
			}
			// put the levels in best to worst order
			// note o2 and o1 order in first two comparisons -- this is because bigger fitness and
			// bigger cvc are better
			Collections.sort(levels, new Comparator<Integer>() {
				// public int compare(final Integer o1, final Integer o2) {
				// int comparisonResult = Float.compare(
				// attributeLevelsCVIntervalsTestAverages[o2],
				// attributeLevelsCVIntervalsTestAverages[o1]);
				// if (comparisonResult == 0) {
				// comparisonResult = attributeLevelsCVCs[o2]
				// - attributeLevelsCVCs[o1];
				// if (comparisonResult == 0) {
				// comparisonResult = o1 - o2;
				// }
				// }
				// return comparisonResult;
				// }
				public int compare(final Integer o1, final Integer o2) {
					// higher average is better
					int comparisonResult;
					switch (Console.multiWayBestModelPickingMethod) {
						case Testing_CVC_Parsimony:
							// higher testing average is better
							comparisonResult = Float.compare(
									attributeLevelsCVIntervalsTestAverages[o2],
									attributeLevelsCVIntervalsTestAverages[o1]);
							if (comparisonResult == 0) {
								// higher CVC is better
								comparisonResult = attributeLevelsCVCs[o2]
										- attributeLevelsCVCs[o1];
								// smaller number of attributes is better
								if (comparisonResult == 0) {
									comparisonResult = o1 - o2;
								}
							}
							break;
						case CVC_Testing_Parsimony:
							// higher CVC is better
							comparisonResult = attributeLevelsCVCs[o2]
									- attributeLevelsCVCs[o1];
							if (comparisonResult == 0) {
								// higher testing average is better
								comparisonResult = Float.compare(
										attributeLevelsCVIntervalsTestAverages[o2],
										attributeLevelsCVIntervalsTestAverages[o1]);
								// smaller number of attributes is better
								if (comparisonResult == 0) {
									comparisonResult = o1 - o2;
								}
							}
							break;
						default:
							throw new RuntimeException(
									"Unhandled value in switch for Console.multiWayBestModelPickingMethod: "
											+ Console.multiWayBestModelPickingMethod);
					}
					return comparisonResult;
				} // end compare method
			});
			return minAttr + levels.get(0);
		}

		public double getBestModelModelSignificance(final int attributeLevel) {
			final double bestModelSignificance = attributeLevelsModelSignificance[attributeLevel
					- minAttr];
			return bestModelSignificance;
		}

		public String getBestModelName(final int attributeLevel) {
			final String bestModelName = bestModelNames[attributeLevel - minAttr];
			return bestModelName;
		}

		public float getBestModelTestAverage(final int attributeLevel) {
			final float testAverage = attributeLevelsBestModelTestAverages[attributeLevel
					- minAttr];
			return testAverage;
		}

		public float getBestModelTrainAverage(final int attributeLevel) {
			final float trainAverage = attributeLevelsBestModelTrainAverages[attributeLevel
					- minAttr];
			return trainAverage;
		}

		public int getCVC(final int attributeLevel) {
			final int CVC = attributeLevelsCVCs[attributeLevel - minAttr];
			return CVC;
		}

		public float getCVIntervalsTestAverage(final int attributeLevel) {
			final float testAverage = attributeLevelsCVIntervalsTestAverages[attributeLevel
					- minAttr];
			return testAverage;
		}

		public float getCVIntervalsTrainAverage(final int attributeLevel) {
			final float trainAverage = attributeLevelsCVIntervalsTrainAverages[attributeLevel
					- minAttr];
			return trainAverage;
		}

		public float getOverallAverage(final int attributeLevel) {
			final float overallAverage = attributeLevelsOverallAverages[attributeLevel
					- minAttr];
			return overallAverage;
		}

		public void run() {
			synchronized (this) {
				final int collectorIndex = Console.analysis.getCollectors().size() - 1;
				final Collector coll = Console.analysis.getCollectors().get(
						collectorIndex);
				final BestModel bestModel = coll.getBest();
				bestModelNames[collectorIndex] = bestModel.getModel().getCombo()
						.getComboString();
				attributeLevelsOverallAverages[collectorIndex] = bestModel.getTotal()
						.getFitness();
				attributeLevelsCVIntervalsTrainAverages[collectorIndex] = bestModel
						.getAvgTrain().getFitness();
				final ConfusionMatrix bestModelAverageConfusionMatrix = bestModel
						.getAvgTest();
				if (bestModelAverageConfusionMatrix == null) {
					attributeLevelsCVIntervalsTestAverages[collectorIndex] = Float.NaN;
				} else {
					attributeLevelsCVIntervalsTestAverages[collectorIndex] = bestModelAverageConfusionMatrix
							.getFitness();
				}
				attributeLevelsCVCs[collectorIndex] = bestModel.getCVC();
				if (coll.getTopModelsLandscape() != null) {
					final IntervalCollection intervalsWithSameModel = coll
							.getTopModelsLandscape().getModelIntervals(
									bestModel.getModel().getCombo());
					if (intervalsWithSameModel != null) {
						attributeLevelsBestModelTrainAverages[collectorIndex] = intervalsWithSameModel
								.getAverageFitness(false /* useTesting */);
						attributeLevelsBestModelTestAverages[collectorIndex] = intervalsWithSameModel
								.getAverageFitness(true /* useTesting */);
					}
				}
				attributeLevelsModelSignificance[collectorIndex] = bestModel.getModel()
						.getModelSignificance(data, Console.modelSignificanceMetric);
			} // end synchronized()
		} // end run()
	} // end OnEndLevelForPermutation private class

	private static class OnEndRunForPermutation implements Runnable {
		private final double[] attributeLevelsOverallAverages;
		private final double[] attributeLevelsCVIntervalsTrainAverages;
		private final double[] attributeLevelsCVIntervalsTestAverages;
		private final double[] attributeLevelsBestModelTrainAverages;
		private final double[] attributeLevelsBestModelTestAverages;
		private final String[] bestModelNames;
		private final int[] attributeLevelsCVCs;
		private final int[] bestModelAttributeLevels;
		public final double[] attributeLevelsModelSignificance;
		private final OnEndLevelForPermutation onEndLevelForPermutation;
		private int permutationIndex = 0;

		public OnEndRunForPermutation(
				final OnEndLevelForPermutation onEndLevelForPermutation,
				final int numPermutations) {
			this.onEndLevelForPermutation = onEndLevelForPermutation;
			attributeLevelsOverallAverages = new double[numPermutations];
			attributeLevelsCVIntervalsTrainAverages = new double[numPermutations];
			attributeLevelsCVIntervalsTestAverages = new double[numPermutations];
			attributeLevelsCVCs = new int[numPermutations];
			attributeLevelsBestModelTrainAverages = new double[numPermutations];
			attributeLevelsBestModelTestAverages = new double[numPermutations];
			bestModelAttributeLevels = new int[numPermutations];
			bestModelNames = new String[numPermutations];
			attributeLevelsModelSignificance = new double[numPermutations];
		}

		public double[] getBestModelTestAverages() {
			return attributeLevelsBestModelTestAverages;
		}

		public double getCVCAverage() {
			int sumCVC = 0;
			for (int permIndex = 0; permIndex < attributeLevelsCVCs.length; ++permIndex) {
				sumCVC += attributeLevelsCVCs[permIndex];
			}
			final double CVCAverage = sumCVC / (double) attributeLevelsCVCs.length;
			return CVCAverage;
		}

		// public double[] getBestModelTrainAverages() {
		// return attributeLevelsBestModelTrainAverages;
		// }
		// public int[] getCVCs() {
		// return attributeLevelsCVCs;
		// }
		public double[] getCVIntervalsTestAverages() {
			return attributeLevelsCVIntervalsTestAverages;
		}

		public double[] getModelSignificances() {
			return attributeLevelsModelSignificance;
		}

		public double[] getOverallAverages() {
			return attributeLevelsOverallAverages;
		}

		public ArrayList<Integer> getPermutationIndicesByRank() {
			final ArrayList<Integer> ranks = new ArrayList<Integer>();
			for (int rankIndex = 0; rankIndex < bestModelNames.length; ++rankIndex) {
				ranks.add(rankIndex);
			}
			// put the permutations in best to worst order
			Collections.sort(ranks, new Comparator<Integer>() {
				public int compare(final Integer o1, final Integer o2) {
					// higher average is better
					int comparisonResult;
					// switch (Console.multiWayBestModelPickingMethod) {
					// hard-code the order to sort by testing for now
					switch (Console.MultiWayBestModelPickingMethod.Testing_CVC_Parsimony) {
						case Testing_CVC_Parsimony:
							// higher testing average is better
							comparisonResult = Double.compare(
									attributeLevelsCVIntervalsTestAverages[o2],
									attributeLevelsCVIntervalsTestAverages[o1]);
							if (comparisonResult == 0) {
								// higher CVC is better
								comparisonResult = attributeLevelsCVCs[o2]
										- attributeLevelsCVCs[o1];
								// smaller number of attributes is better
								if (comparisonResult == 0) {
									comparisonResult = bestModelAttributeLevels[o1]
											- bestModelAttributeLevels[o2];
								}
							}
							break;
						case CVC_Testing_Parsimony:
							// higher CVC is better
							comparisonResult = attributeLevelsCVCs[o2]
									- attributeLevelsCVCs[o1];
							if (comparisonResult == 0) {
								// higher testing average is better
								comparisonResult = Double.compare(
										attributeLevelsCVIntervalsTestAverages[o2],
										attributeLevelsCVIntervalsTestAverages[o1]);
								// smaller number of attributes is better
								if (comparisonResult == 0) {
									comparisonResult = bestModelAttributeLevels[o1]
											- bestModelAttributeLevels[o2];
								}
							}
							break;
						default:
							throw new RuntimeException(
									"Unhandled value in switch for Console.multiWayBestModelPickingMethod: "
											+ Console.multiWayBestModelPickingMethod);
					}
					if (comparisonResult == 0) {
						// if can't decide, use permutation order to make result determinate
						comparisonResult = o1 - o2;
					}
					return comparisonResult;
				} // end compare method
			});
			return ranks;
		} // end getPermutationResultRankIndex

		// public double[] getCVIntervalsTrainAverages(final int attributeLevel) {
		// return attributeLevelsCVIntervalsTrainAverages;
		// }
		public void run() {
			synchronized (this) {
				final int bestModelAttributeLevel = bestModelAttributeLevels[permutationIndex] = onEndLevelForPermutation
						.getBestModelAttributeLevel();
				bestModelNames[permutationIndex] = onEndLevelForPermutation
						.getBestModelName(bestModelAttributeLevel);
				attributeLevelsOverallAverages[permutationIndex] = onEndLevelForPermutation
						.getOverallAverage(bestModelAttributeLevel);
				attributeLevelsCVIntervalsTrainAverages[permutationIndex] = onEndLevelForPermutation
						.getCVIntervalsTrainAverage(bestModelAttributeLevel);
				attributeLevelsCVIntervalsTestAverages[permutationIndex] = onEndLevelForPermutation
						.getCVIntervalsTestAverage(bestModelAttributeLevel);
				attributeLevelsCVCs[permutationIndex] = onEndLevelForPermutation
						.getCVC(bestModelAttributeLevel);
				attributeLevelsBestModelTrainAverages[permutationIndex] = onEndLevelForPermutation
						.getBestModelTrainAverage(bestModelAttributeLevel);
				attributeLevelsBestModelTestAverages[permutationIndex] = onEndLevelForPermutation
						.getBestModelTestAverage(bestModelAttributeLevel);
				attributeLevelsModelSignificance[permutationIndex] = onEndLevelForPermutation
						.getBestModelModelSignificance(bestModelAttributeLevel);
				++permutationIndex;
			} // end synchronized()
		} // end run()
	} // end OnAttribute private class //end calculateAndPrintPermutationStatistics()

	public enum ReliefFRebalancingMethod {
		DO_NOTHING, PROPORTIONAL_WEIGHTING, OVERSAMPLE_MINORITY, OVERSAMPLE_MINORITY_MULTIPLE_DATASETS
	}

	public enum ScoringMethod {
		BALANCED_ACCURACY, ADJUSTED_BALANCED_ACCURACY, BALANCED_ACCURACY_TIMES_COVERAGE, BALANCED_ACCURACY_RYAN;
	}

	private enum SearchMethod {
		INVALID, EXHAUSTIVE, FORCED, RANDOM, EDA;
	}
}
