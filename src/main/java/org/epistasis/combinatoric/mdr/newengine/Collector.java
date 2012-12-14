package org.epistasis.combinatoric.mdr.newengine;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;
import org.epistasis.combinatoric.mdr.newengine.Dataset.Genotype.GenotypeQualityMetric;
import org.epistasis.combinatoric.mdr.newengine.Dataset.GenotypeFilterType;
import org.epistasis.combinatoric.mdr.newengine.Dataset.MdrByGenotypeVotingMethod;
import org.epistasis.combinatoric.mdr.newengine.Dataset.MutableFloat;
import org.epistasis.combinatoric.mdr.newengine.Interval.IntervalCollection;

public class Collector
    {
    private final Interval[] intervals;
    private BestModel best;
    private TopModelsLandscape topModelsLandscape;
    private final AllModelsLandscape allModelsLandscape;
    private final List<Dataset> testSets;
    List<MdrByGenotypeResults> mdrByGenotypeResults = new ArrayList<Collector.MdrByGenotypeResults>();

    private static ConfusionMatrix constructConfMatrix(final Dataset d,
                                                       final String[] fields, int index)
        {
        final ConfusionMatrix m = new ConfusionMatrix(d.getLevels().get(
                d.getCols() - 1), d.getAffectedStatus());
        for (int i = 0; i < m.size(); ++i)
            {
            for (int j = 0; j < m.size(); ++j)
                {
                m.add(i, j, Float.parseFloat(fields[index++]));
                }
            }
        return m;
        }

    private static String mdrByGenotypeGetBestAttributes(
            final List<String> labels,
            final Map<String, Dataset.MutableFloat> attributeCountMap)
        {
        String mdrByGenotypeModelString = "NA";
        final SortedSet<Map.Entry<String, Dataset.MutableFloat>> sortedAttributeUseCounts = new TreeSet<Map
                .Entry<String, MutableFloat>>(
                new Comparator<Map.Entry<String, Dataset.MutableFloat>>()
                {
                public int compare(
                        final java.util.Map.Entry<String, MutableFloat> o1,
                        final java.util.Map.Entry<String, MutableFloat> o2)
                    {
                    // larger is better so comparison result negated
                    int compareResult = -Float.compare(o1.getValue().myFloat,
                                                       o2.getValue().myFloat);
                    if (compareResult == 0)
                        {
                        final int attribute1Index = labels.indexOf(o1.getKey());
                        final int attribute2Index = labels.indexOf(o2.getKey());
                        // higher index is considered better in breaking ties -- this is so that X0 X1 not best in
                        // case of all equal
                        compareResult = attribute2Index - attribute1Index;
                        }
                    return compareResult;
                    }
                });
        sortedAttributeUseCounts.addAll(attributeCountMap.entrySet());
        if (false)
            {
            return sortedAttributeUseCounts.first().getKey().replace(',', ' ');
            }
        // System.out.println("Attribute map: "
        // + sortedAttributeUseCounts.toString());
        if (sortedAttributeUseCounts.size() >= 2)
            {
            if (false)
                {
                while (sortedAttributeUseCounts.size() > 100)
                    {
                    sortedAttributeUseCounts.remove(sortedAttributeUseCounts.last());
                    }
                mdrByGenotypeModelString = sortedAttributeUseCounts.toString();
                }
            else
                {
                final Map.Entry<String, MutableFloat> attributeOne = sortedAttributeUseCounts
                        .first();
                final int attributeOneIndex = labels.indexOf(attributeOne.getKey());
                sortedAttributeUseCounts.remove(attributeOne);
                final Map.Entry<String, MutableFloat> attributeTwo = sortedAttributeUseCounts
                        .first();
                final int attributeTwoIndex = labels.indexOf(attributeTwo.getKey());
                final List<String> bestAttributesInAttributeOrder = new ArrayList<String>(
                        2);
                if (attributeOneIndex < attributeTwoIndex)
                    {
                    bestAttributesInAttributeOrder.add(labels.get(attributeOneIndex));
                    bestAttributesInAttributeOrder.add(labels.get(attributeTwoIndex));
                    }
                else
                    {
                    bestAttributesInAttributeOrder.add(labels.get(attributeTwoIndex));
                    bestAttributesInAttributeOrder.add(labels.get(attributeOneIndex));
                    }
                mdrByGenotypeModelString = bestAttributesInAttributeOrder.get(0) + " "
                                           + bestAttributesInAttributeOrder.get(1);
                }
            } // end if have at least 2 good attributes
        return mdrByGenotypeModelString;
        }

    private static void printAttributeCombination(final PrintWriter p,
                                                  final AttributeCombination a)
        {
        for (int i = 0; i < a.size(); ++i)
            {
            if (i != 0)
                {
                p.print(',');
                }
            p.print(a.getLabels().get(a.get(i)));
            }
        }

    private static void printConfusionMatrix(final PrintWriter p,
                                             final ConfusionMatrix m)
        {
        for (int i = 0; i < m.size(); ++i)
            {
            for (int j = 0; j < m.size(); ++j)
                {
                p.print(' ');
                p.print(m.get(i, j));
                }
            }
        }

    public Collector(final int intervalCount, final int topModelsLandscapeSize,
                     final AllModelsLandscape allModelsLandscape, final List<Dataset> testSets)
        {
        this.testSets = testSets;
        // System.out.println("\n\n\nStarting...");
        intervals = new Interval[intervalCount];
        topModelsLandscape = (topModelsLandscapeSize > 0) ? new TopModelsLandscape(
                intervalCount, topModelsLandscapeSize) : null;
        this.allModelsLandscape = allModelsLandscape;
        }

    void computeBest(final Dataset data, final AmbiguousCellStatus tieStatus)
        {
        if (best == null)
            {
            final Map<AttributeCombination, IntervalCollection> attrMap = getAttributeIntervalMap();
            BestModel bestModel = BestModel.EMPTY;
            if (attrMap.size() != 0)
                {
                final Console.FitnessCriterion primaryCriterion = Console.fitnessCriteriaOrder
                        .getPrimaryTest();
                List<IntervalCollection> bestModels = null;
                switch (primaryCriterion)
                    {
                    case CVC:
                        bestModels = getBestCVC(attrMap);
                        break;
                    case TESTING_ALL:
                        bestModels = topModelsLandscape.getTopTestingModels(true);
                        break;
                    case TRAINING_ALL:
                        bestModels = topModelsLandscape.getTopTrainingModels();
                        break;
                    case TRAINING_INTERVALS:
                        bestModels = getBestIntervals(attrMap, false);
                        break;
                    case TESTING_INTERVALS:
                        bestModels = getBestIntervals(attrMap, true);
                        break;
                    case MODEL_SIGNIFICANCE:
                        bestModels = topModelsLandscape.getTopSignificanceModels(data);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "unhandled Console.FitnessCriterion in switch: "
                                + primaryCriterion);
                    }
                IntervalCollection bestIntervalCollection = null;
                // System.out.println("primaryCriterion: " + primaryCriterion + " returned " + bestModels.toString());
                if (bestModels.size() == 1)
                    {
                    bestIntervalCollection = bestModels.get(0);
                    }
                else
                    {
                    Collections.sort(bestModels); // sort so order is deterministic
                    final Console.FitnessCriterion tieBreakerCriterion = Console.fitnessCriteriaOrder
                            .getTieBreakerTest();
                    switch (tieBreakerCriterion)
                        {
                        case CVC:
                            bestIntervalCollection = getBestCVC(attrMap, bestModels);
                            break;
                        case TESTING_ALL:
                            bestIntervalCollection = IntervalCollection.getBestAverage(true,
                                                                                       bestModels);
                            break;
                        case TRAINING_ALL:
                            bestIntervalCollection = IntervalCollection.getBestAverage(false,
                                                                                       bestModels);
                            break;
                        case TRAINING_INTERVALS:
                            bestIntervalCollection = getBestIntervals(attrMap, false,
                                                                      bestModels);
                            break;
                        case TESTING_INTERVALS:
                            bestIntervalCollection = getBestIntervals(attrMap, true,
                                                                      bestModels);
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "unhandled Console.FitnessCriterion in switch: "
                                    + primaryCriterion);
                        }
                    // System.out.println("tieBreakerCriterion: " + tieBreakerCriterion + " returned " +
                    // bestIntervalCollection.toString());
                    }
                Interval[] intervalArray = null;
                final AttributeCombination bestModelAttributeCombination = bestIntervalCollection
                        .getAttributeCombination();
                if (Console.useBestModelActualIntervals && (topModelsLandscape != null))
                    {
                    bestIntervalCollection = topModelsLandscape
                            .getModelIntervals(bestModelAttributeCombination);
                    if (bestIntervalCollection != null)
                        {
                        intervalArray = bestIntervalCollection.getIntervalArray();
                        }
                    }
                if (intervalArray == null)
                    {
                    intervalArray = intervals;
                    }
                final List<ConfusionMatrix> trainingMatrices = new ArrayList<ConfusionMatrix>(
                        intervalArray.length);
                final List<ConfusionMatrix> testingMatrices = new ArrayList<ConfusionMatrix>(
                        intervalArray.length);
                for (int intervalIndex = 0; intervalIndex < intervalArray.length; ++intervalIndex)
                    {
                    Interval interval = intervalArray[intervalIndex];
                    if (interval == null)
                        {
                        interval = intervalArray[intervalIndex] = Interval.EMPTY;
                        }
                    trainingMatrices.add(interval.getTrain());
                    if (testSets != null)
                        {
                        ConfusionMatrix test = interval.getTest();
                        if (test == null)
                            {
                            final Dataset testingPartition = testSets.get(intervalIndex);
                            test = interval.test(testingPartition);
                            }
                        testingMatrices.add(test);
                        }
                    }
                if (trainingMatrices.size() != intervals.length)
                    {
                    throw new RuntimeException(
                            "Collector.computeBest has the wrong number of trainingMatrices!. Expected size: "
                            + intervals.length
                            + ". Actual size: "
                            + trainingMatrices.size());
                    }
                // get cvc count
                final int cvcCount = getCvcCount(attrMap, bestModelAttributeCombination);
                bestModel = new BestModel(bestModelAttributeCombination, data,
                                          tieStatus, cvcCount, ConfusionMatrix.getAverage(trainingMatrices),
                                          ConfusionMatrix.getAverage(testingMatrices));
                }
            best = bestModel;
            }
        }

    public void computeMdrByGenotype(
            final Pair<List<Dataset>, List<Dataset>> partition)
        {
        final List<String> labels = partition.getFirst().get(0).getLabels();
        for (final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod : new
                MdrByGenotypeVotingMethod[]{MdrByGenotypeVotingMethod.FIRST_MATCH_PREDICTS,
                /*
             * MdrByGenotypeVotingMethod.FIRST_MATCH_PREDICTS_WEIGHT_ACCURACY,
             * MdrByGenotypeVotingMethod.FIRST_MATCH_PREDICTS_WEIGHT_INVERSE_FISHERS,
             * MdrByGenotypeVotingMethod.FIRST_MATCH_PREDICTS_WEIGHT_MAJORITY_RATIO,
             * MdrByGenotypeVotingMethod.SUM_OF_ALL_MATCHES,
             * MdrByGenotypeVotingMethod.WEIGHTED_SUM_OF_ACCURACY_FOR_ALL_MATCHES,
             * MdrByGenotypeVotingMethod.WEIGHTED_SUM_OF_MAJORITY_RATIO_FOR_ALL_MATCHES
             */
        })
            {
            // for (final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod : MdrByGenotypeVotingMethod
            // .values()) {
            // for (final GenotypeQualityMetric genotypeQualityMetric : GenotypeQualityMetric
            // .values()) {
            for (final GenotypeQualityMetric genotypeQualityMetric : new GenotypeQualityMetric[]{
                    GenotypeQualityMetric.FISHERS_EXACT,
                    GenotypeQualityMetric.FISHERS_QUICK_AND_DIRTY,})
                {
                // for (final Dataset.GenotypeFilterType genotypeFilterType : Dataset.GenotypeFilterType
                // .genotypeFilterTypes) {
                for (final Dataset.GenotypeFilterType genotypeFilterType : new Dataset.GenotypeFilterType[]{new
                        GenotypeFilterType(
                        null, Double.NaN)})
                    {
                    final Map<String, Dataset.MutableFloat> trainingAttributeCountMap = new HashMap<String,
                            Dataset.MutableFloat>();
                    final Map<String, Dataset.MutableFloat> testingAttributeCountMap = new HashMap<String,
                            Dataset.MutableFloat>();
                    final List<Dataset> trainingDatasets = partition.getFirst();
                    final List<Dataset> testingDatasets = partition.getSecond();
                    final List<ConfusionMatrix> trainingMatrices = new ArrayList<ConfusionMatrix>(
                            trainingDatasets.size());
                    final List<ConfusionMatrix> testingMatrices = new ArrayList<ConfusionMatrix>(
                            testingDatasets.size());
                    for (int intervalIndex = 0; intervalIndex < trainingDatasets.size(); ++intervalIndex)
                        {
                        final Pair<ConfusionMatrix, ConfusionMatrix> resultConfusionMatrices = Dataset
                                .scoreWithMostSignificantGenotypes(mdrByGenotypeVotingMethod,
                                                                   genotypeQualityMetric, genotypeFilterType,
                                                                   trainingDatasets.get(intervalIndex),
                                                                   testingDatasets.get(intervalIndex),
                                                                   trainingAttributeCountMap, testingAttributeCountMap);
                        trainingMatrices.add(resultConfusionMatrices.getFirst());
                        testingMatrices.add(resultConfusionMatrices.getSecond());
                        }
                    final ConfusionMatrix avgTraining = ConfusionMatrix
                            .getAverage(trainingMatrices);
                    final String trainingBestAttributes = Collector
                            .mdrByGenotypeGetBestAttributes(labels, trainingAttributeCountMap);
                    final ConfusionMatrix avgTesting = ConfusionMatrix
                            .getAverage(testingMatrices);
                    final String testingBestAttributes = Collector
                            .mdrByGenotypeGetBestAttributes(labels, testingAttributeCountMap);
                    mdrByGenotypeResults.add(new MdrByGenotypeResults(
                            mdrByGenotypeVotingMethod, genotypeQualityMetric,
                            genotypeFilterType, avgTraining, trainingBestAttributes,
                            avgTesting, testingBestAttributes));
                    } // end filterByFishers
                } // end GenotypeQualityMetric
            } // end for MdrByGenotypeVotingMethod
        }// end computeMdrByGenotype()

    public void consider(final int intervalIndex, final Model model,
                         final ConfusionMatrix train)
        {
        final Interval old = intervals[intervalIndex];
        final float currentTrainFitness = train.getFitness();
        if (!Float.isNaN(currentTrainFitness))
            {
            boolean doReplace;
            float oldTrainFitness = Float.NaN;
            if (old == null)
                {
                doReplace = true;
                }
            else
                {
                oldTrainFitness = old.getTrain().getFitness();
                final int compareResult = Float.compare(train.getFitness(),
                                                        oldTrainFitness);
                doReplace = (compareResult > 0);
                }
            if (doReplace)
                {
                // System.out
                // .println("consider["
                // + intervalIndex
                // + "] best "
                // + ((old != null) ? (old.getModel().getCombo() + " " + oldTrainFitness)
                // : "null") + " replaced with: " + model.getCombo()
                // + " with " + currentTrainFitness);
                intervals[intervalIndex] = new Interval(model, train);
                }
            if (topModelsLandscape != null)
                {
                topModelsLandscape.add(intervalIndex, model, train, testSets);
                }
            if (allModelsLandscape != null)
                {
                allModelsLandscape.add(intervalIndex, model, train, testSets);
                }
            best = null;
            } // if currentTrain not null
        }

    private Map<AttributeCombination, IntervalCollection> getAttributeIntervalMap()
        {
        final Map<AttributeCombination, List<Interval>> attrMap = new HashMap<AttributeCombination, List<Interval>>();
        for (final Interval interval : intervals)
            {
            if ((interval == null) || (interval.getModel() == null))
                {
                continue;
                }
            final AttributeCombination combo = interval.getModel().getCombo();
            List<Interval> list = attrMap.get(combo);
            if (list == null)
                {
                list = new ArrayList<Interval>();
                attrMap.put(combo, list);
                }
            list.add(interval);
            }
        final Map<AttributeCombination, IntervalCollection> convertedAttrMap = new HashMap<AttributeCombination,
                IntervalCollection>();
        for (final Map.Entry<AttributeCombination, List<Interval>> entry : attrMap
                .entrySet())
            {
            convertedAttrMap.put(entry.getKey(),
                                 new IntervalCollection(entry.getValue(), testSets));
            }
        return convertedAttrMap;
        }

    public BestModel getBest()
        {
        return best;
        }

    /**
     * This will return the IntervalCollection for the model that is most common among the CV intervals. It is
     * possible that there could be a
     * tie or tie in which case multiple IntervalCollections might be returned.
     *
     * @param attrMap
     * @return
     */
    private List<IntervalCollection> getBestCVC(
            final Map<AttributeCombination, IntervalCollection> attrMap)
        {
        // FIRST -- identify combo(s) with best CVC (i.e. most number of times model was found)
        final SortedMap<Integer, List<IntervalCollection>> sortedMap = new TreeMap<Integer, List<IntervalCollection>>(
                Collections.reverseOrder());
        for (final Map.Entry<AttributeCombination, IntervalCollection> entry : attrMap
                .entrySet())
            {
            final int cvc = entry.getValue().size();
            List<IntervalCollection> listOfLists = sortedMap.get(cvc);
            if (listOfLists == null)
                {
                listOfLists = new ArrayList<IntervalCollection>();
                sortedMap.put(cvc, listOfLists);
                }
            listOfLists.add(entry.getValue());
            }
        final List<IntervalCollection> bestModels = sortedMap.get(sortedMap
                                                                          .firstKey());
        return bestModels;
        }

    private IntervalCollection getBestCVC(
            final Map<AttributeCombination, IntervalCollection> attrMap,
            final List<IntervalCollection> bestModels)
        {
        IntervalCollection bestModel = null;
        int bestModelCvcCount = -1;
        for (final IntervalCollection oneOfBestModels : bestModels)
            {
            final int cvcCount = getCvcCount(attrMap,
                                             oneOfBestModels.getAttributeCombination());
            if (cvcCount > bestModelCvcCount)
                {
                bestModel = oneOfBestModels;
                bestModelCvcCount = cvcCount;
                }
            }
        return bestModel;
        }

    private List<IntervalCollection> getBestIntervals(
            final Map<AttributeCombination, IntervalCollection> attrMap,
            final boolean useTesting)
        {
        // now from all models with the best CVC, pick the one with the best average training fitness
        final SortedMap<Float, List<IntervalCollection>> sortedMap = new TreeMap<Float, List<IntervalCollection>>(
                Collections.reverseOrder());
        for (final Map.Entry<AttributeCombination, IntervalCollection> entry : attrMap
                .entrySet())
            {
            final IntervalCollection intervalsWithSameModel = entry.getValue();
            final Float fitness = intervalsWithSameModel
                    .getAverageFitness(useTesting);
            List<IntervalCollection> listOfLists = sortedMap.get(fitness);
            if (listOfLists == null)
                {
                listOfLists = new ArrayList<IntervalCollection>();
                sortedMap.put(fitness, listOfLists);
                }
            listOfLists.add(entry.getValue());
            }
        final List<IntervalCollection> bestModels = sortedMap.get(sortedMap
                                                                          .firstKey());
        return bestModels;
        }

    private IntervalCollection getBestIntervals(
            final Map<AttributeCombination, IntervalCollection> attrMap,
            final boolean useTesting, final List<IntervalCollection> bestModels)
        {
        IntervalCollection bestModel = null;
        Float bestCvcIntervalsAverageFitness = Float.NEGATIVE_INFINITY;
        for (final IntervalCollection oneOfBestModels : bestModels)
            {
            final AttributeCombination currentModelIdentifier = oneOfBestModels
                    .getAttributeCombination();
            final IntervalCollection cvcForCurrentModel = attrMap
                    .get(currentModelIdentifier);
            Float cvcIntervalsAverageFitness = 0.0f;
            if (cvcForCurrentModel != null)
                {
                cvcIntervalsAverageFitness = cvcForCurrentModel
                        .getAverageFitness(useTesting);
                }
            if (bestCvcIntervalsAverageFitness.compareTo(cvcIntervalsAverageFitness) < 0)
                {
                bestModel = oneOfBestModels;
                bestCvcIntervalsAverageFitness = cvcIntervalsAverageFitness;
                }
            }
        return bestModel;
        }

    private int getCvcCount(
            final Map<AttributeCombination, IntervalCollection> attrMap,
            final AttributeCombination currentModelIdentifier)
        {
        int cvcCount = 0;
        final IntervalCollection cvcForCurrentModel = attrMap
                .get(currentModelIdentifier);
        if (cvcForCurrentModel != null)
            {
            cvcCount = cvcForCurrentModel.size();
            }
        return cvcCount;
        }

    public Interval getInterval(final int intervalIndex)
        {
        return intervals[intervalIndex] != null ? intervals[intervalIndex]
                                                : Interval.EMPTY;
        }

    public List<MdrByGenotypeResults> getMdrByGenotypeResults()
        {
        return mdrByGenotypeResults;
        }

    public TopModelsLandscape getTopModelsLandscape()
        {
        return topModelsLandscape;
        }

    // TODO: when polytomy happens, this will need to change
    public void read(final Dataset d,
                     final AmbiguousCellStatus ambiguousCellAssgnmentType,
                     final LineNumberReader r) throws IOException
        {
        String line = r.readLine();
        if (line.equals("BeginTopModels"))
            {
            topModelsLandscape = new TopModelsLandscape(intervals.length, r,
                                                        Pattern.compile("^EndTopModels$"), d.getLabels());
            line = r.readLine();
            }
        for (int i = 0; i < size(); ++i)
            {
            final String[] fields = line.split("\\s+");
            int index = 0;
            if (!fields[index++].equals("Attr"))
                {
                throw new IOException();
                }
            final Model model = new Model(new AttributeCombination(fields[index++],
                                                                   d.getLabels()), ambiguousCellAssgnmentType);
            if (!fields[index++].equals("Train"))
                {
                throw new IOException();
                }
            final ConfusionMatrix train = Collector.constructConfMatrix(d, fields,
                                                                        index);
            index += 5;
            if (size() > 1)
                {
                if (!fields[index++].equals("Test"))
                    {
                    throw new IOException();
                    }
                final ConfusionMatrix test = Collector.constructConfMatrix(d, fields,
                                                                           index++);
                intervals[i] = new Interval(model, train, test);
                }
            else
                {
                intervals[i] = new Interval(model, train, null);
                }
            line = r.readLine();
            }
        final String[] fields = line.split("\\s+");
        int index = 0;
        if (!fields[index++].equals("Attr"))
            {
            throw new IOException();
            }
        final AttributeCombination combo = new AttributeCombination(
                fields[index++], d.getLabels());
        if (!fields[index++].equals("AvgTrain"))
            {
            throw new IOException();
            }
        final ConfusionMatrix avgtrain = Collector.constructConfMatrix(d, fields,
                                                                       index);
        index += 5;
        ConfusionMatrix avgtest = null;
        if (size() > 1)
            {
            if (!fields[index++].equals("AvgTest"))
                {
                throw new IOException();
                }
            avgtest = Collector.constructConfMatrix(d, fields, index);
            index += 5;
            }
        if (!fields[index++].equals("Train"))
            {
            throw new IOException();
            }
        final ConfusionMatrix total = Collector.constructConfMatrix(d, fields,
                                                                    index);
        index += 5;
        if (!fields[index++].equals("Summary"))
            {
            throw new IOException();
            }
        final int cvc = Integer.parseInt(fields[index++]);
        best = new BestModel(combo, cvc, avgtrain, avgtest, total,
                             ambiguousCellAssgnmentType);
        best.getModel().read(d, r);
        best.getModel().buildStatuses(d, d.getStatusCounts());
        }

    public int size()
        {
        return intervals.length;
        }

    public void test()
        {
        for (int i = 0; (i < intervals.length) && (intervals[i] != null); ++i)
            {
            intervals[i].test(testSets.get(i));
            }
        best = null;
        }

    public void write(final Writer w)
        {
        final PrintWriter p = new PrintWriter(w);
        if ((topModelsLandscape != null) && !topModelsLandscape.isEmpty())
            {
            p.println("BeginTopModels");
            topModelsLandscape.write(p);
            p.println("EndTopModels");
            }
        for (final Interval interval : intervals)
            {
            p.print("Attr ");
            Collector.printAttributeCombination(p, interval.getModel().getCombo());
            p.print(" Train");
            Collector.printConfusionMatrix(p, interval.getTrain());
            p.print(' ');
            p.print(0.0f);
            if (interval.getTest() != null)
                {
                p.print(" Test");
                Collector.printConfusionMatrix(p, interval.getTest());
                p.print(' ');
                p.print(0.0f);
                }
            p.println();
            }
        p.print("Attr ");
        Collector.printAttributeCombination(p, getBest().getModel().getCombo());
        p.print(" AvgTrain");
        Collector.printConfusionMatrix(p, getBest().getAvgTrain());
        p.print(' ');
        p.print(0.0f);
        if (getBest().getAvgTest() != null)
            {
            p.print(" AvgTest");
            Collector.printConfusionMatrix(p, getBest().getAvgTest());
            p.print(' ');
            p.print(0.0f);
            }
        p.print(" Train");
        Collector.printConfusionMatrix(p, getBest().getTotal());
        p.print(' ');
        p.print(0.0f);
        p.print(" Summary ");
        p.print(getBest().getCVC());
        p.println();
        getBest().getModel().write(p);
        p.flush();
        }

    public static class BestModel
        {
        private final Model model;
        private final ConfusionMatrix total;
        private final int cvc;
        private final ConfusionMatrix avgTrain;
        private final ConfusionMatrix avgTest;
        public final static BestModel EMPTY = new BestModel(Model.EMPTY, 0,
                                                            ConfusionMatrix.EMPTY, ConfusionMatrix.EMPTY, ConfusionMatrix.EMPTY);

        private BestModel(final AttributeCombination combo, final Dataset data,
                          final AmbiguousCellStatus tieStatus, final int cvc,
                          final ConfusionMatrix avgTrain, final ConfusionMatrix avgTest)
            {
            model = new Model(combo, tieStatus);
            model.buildCounts(data);
            model.buildStatuses(data, data.getStatusCounts());
            total = model.test(data);
            this.cvc = cvc;
            this.avgTrain = avgTrain;
            this.avgTest = avgTest;
            }

        private BestModel(final AttributeCombination combo, final int cvc,
                          final ConfusionMatrix avgTrain, final ConfusionMatrix avgTest,
                          final ConfusionMatrix total,
                          final Console.AmbiguousCellStatus tiePriority)
            {
            this(new Model(combo, tiePriority), cvc, avgTrain, avgTest, total);
            }

        private BestModel(final Model model, final int cvc,
                          final ConfusionMatrix avgTrain, final ConfusionMatrix avgTest,
                          final ConfusionMatrix total)
            {
            this.model = model;
            this.total = total;
            this.cvc = cvc;
            this.avgTrain = avgTrain;
            this.avgTest = avgTest;
            }

        public ConfusionMatrix getAvgTest()
            {
            return avgTest;
            }

        public ConfusionMatrix getAvgTrain()
            {
            return avgTrain;
            }

        public int getCVC()
            {
            return cvc;
            }

        public Model getModel()
            {
            return model;
            }

        public ConfusionMatrix getTotal()
            {
            return total;
            }
        } // end class BestModel

    public static class MdrByGenotypeResults
        {
        private final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod;
        private final ConfusionMatrix avgTraining;
        private final String trainingBestAttributes;
        private final ConfusionMatrix avgTesting;
        private final String testingBestAttributes;
        private final GenotypeQualityMetric genotypeQualityMetric;
        private final GenotypeFilterType genotypeFilterType;
        private final static ScoringMethod preferredScoringMethod = ScoringMethod.BALANCED_ACCURACY_RYAN;

        public static String getTabDelimitedHeaderString()
            {
            final StringBuilder sb = new StringBuilder();
            sb.append("MdrByGenotypeVotingMethod\t");
            sb.append("GenotypeQualityMetric\t");
            sb.append("GenotypeFilterType\t");
            sb.append("mdrByGenotypeTrainingBestAttributes\t");
            sb.append("mdrByGenotypeTrainingConfusionMatrix_totalCount\t");
            sb.append("mdrByGenotypeTrainingConfusionMatrix_unknownCount\t");
            sb.append("mdrByGenotypeTrainingConfusionMatrix_balancedAccuracy\t");
            sb.append("mdrByGenotypeTrainingConfusionMatrix_adjustedBalancedAccuracy\t");
            sb.append("mdrByGenotypeTestingBestAttributes\t");
            sb.append("mdrByGenotypeTestingConfusionMatrix_totalCount\t");
            sb.append("mdrByGenotypeTestingConfusionMatrix_unknownCount\t");
            sb.append("mdrByGenotypeTestingConfusionMatrix_balancedAccuracy\t");
            sb.append("mdrByGenotypeTestingConfusionMatrix_adjustedBalancedAccuracy\t");
            return sb.toString();
            }

        MdrByGenotypeResults(
                final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod,
                final GenotypeQualityMetric genotypeQualityMetric,
                final GenotypeFilterType genotypeFilterType,
                final ConfusionMatrix avgTraining, final String trainingBestAttributes,
                final ConfusionMatrix avgTesting, final String testingBestAttributes)
            {
            this.mdrByGenotypeVotingMethod = mdrByGenotypeVotingMethod;
            this.genotypeQualityMetric = genotypeQualityMetric;
            this.genotypeFilterType = genotypeFilterType;
            this.avgTraining = avgTraining;
            this.trainingBestAttributes = trainingBestAttributes;
            this.avgTesting = avgTesting;
            this.testingBestAttributes = testingBestAttributes;
            }

        public String getTabDelimitedRowString()
            {
            final StringBuilder sb = new StringBuilder();
            sb.append(mdrByGenotypeVotingMethod + "\t");
            sb.append(genotypeQualityMetric + "\t");
            sb.append(genotypeFilterType + "\t");
            sb.append(trainingBestAttributes + "\t");
            sb.append(avgTraining.getTotalCount() + "\t");
            float unknownCount = avgTraining.getUnknownCount();
            sb.append(unknownCount);
            if (unknownCount > 0)
                {
                sb.append(" coverage: " + (int) (avgTraining.getCoverageRatio() * 100)
                          + "%");
                }
            sb.append("\t");
            sb.append(avgTraining.getBalancedAccuracy() + "\t");
            sb.append(avgTraining
                              .getScore(MdrByGenotypeResults.preferredScoringMethod) + "\t");
            sb.append(testingBestAttributes + "\t");
            sb.append(avgTesting.getTotalCount() + "\t");
            unknownCount = avgTesting.getUnknownCount();
            sb.append(unknownCount);
            if (unknownCount > 0)
                {
                sb.append(" coverage: " + (int) (avgTesting.getCoverageRatio() * 100)
                          + "%");
                }
            sb.append("\t");
            sb.append(avgTesting.getBalancedAccuracy() + "\t");
            sb.append(avgTesting
                              .getScore(MdrByGenotypeResults.preferredScoringMethod) + "\t");
            return sb.toString();
            }

        @Override
        public String toString() {
        return MdrByGenotypeResults.getTabDelimitedHeaderString() + "\n"
               + getTabDelimitedRowString();
        }
        } // end class MdrByGenotypeResults
    } // end class Collector
