package org.epistasis.combinatoric.mdr.newengine;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.epistasis.Pair;
import org.epistasis.PriorityList;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.MetricRankOrder;
import org.epistasis.combinatoric.mdr.newengine.Interval.IntervalCollection;

public class TopModelsLandscape extends Landscape
    {
    private final PriorityList<Pair<Pair<String, Float>, IntervalCollection>> landscape;
    private final static Comparator<Pair<Pair<String, Float>, IntervalCollection>> cmp = new
            Comparator<Pair<Pair<String, Float>, IntervalCollection>>()
    {
    public int compare(final Pair<Pair<String, Float>, IntervalCollection> o1,
                       final Pair<Pair<String, Float>, IntervalCollection> o2)
        {
        final Float obj1AverageFitness = o1.getFirst().getSecond();
        final Float obj2AverageFitness = o2.getFirst().getSecond();
        // reverse the result to sort the list Descending
        int compareResult = obj2AverageFitness.compareTo(obj1AverageFitness);
        if (compareResult == 0)
            {
            compareResult = o1.getFirst().getFirst()
                    .compareTo(o2.getFirst().getFirst());
            }
        return compareResult;
        }
    };

    public TopModelsLandscape(final int intervals, final int maximumEntries)
        {
        super(intervals);
        landscape = new PriorityList<Pair<Pair<String, Float>, IntervalCollection>>(
                maximumEntries, TopModelsLandscape.cmp);
        }

    public TopModelsLandscape(final int intervals, final LineNumberReader r,
                              final Pattern endPattern, final List<String> labels) throws IOException
        {
        super(intervals);
        landscape = new PriorityList<Pair<Pair<String, Float>, IntervalCollection>>(
                Integer.MAX_VALUE, TopModelsLandscape.cmp);
        read(r, endPattern, labels);
        landscape.setCapacity(landscape.size());
        }

    @Override
    protected void addLandscapeItem(final AttributeCombination attributeCombo,
                                    final float averageTrainingFitness,
                                    final IntervalCollection trainingMatrices)
        {
        if (Console.outputAllModels)
            {
            Console.writeOutAllModel(attributeCombo, intervalCount,
                                     averageTrainingFitness, trainingMatrices.getAverageFitness(true));
            }
        landscape.add(new Pair<Pair<String, Float>, IntervalCollection>(
                new Pair<String, Float>(attributeCombo.getComboString(),
                                        averageTrainingFitness), trainingMatrices));
        }

    @Override
    public List<Pair<String, Float>> getLandscape() {
    final List<Pair<String, Float>> landscapeList = new ArrayList<Pair<String, Float>>(
            landscape.size());
    for (final Pair<Pair<String, Float>, IntervalCollection> pairPair : landscape)
        {
        landscapeList.add(pairPair.getFirst());
        }
    return landscapeList;
    }

    public Collection<Pair<Pair<String, Float>, IntervalCollection>> getLandscapeWithAllDetails()
        {
        return landscape;
        }

    public IntervalCollection getModelIntervals(
            final AttributeCombination attributeCombination)
        {
        IntervalCollection requestedModelsIntervals = null;
        final String comboName = attributeCombination.getComboString();
        for (final Pair<Pair<String, Float>, IntervalCollection> topModelPairPair : landscape)
            {
            final String currentIntervalModelName = topModelPairPair.getFirst()
                    .getFirst();
            if (currentIntervalModelName.equals(comboName))
                {
                requestedModelsIntervals = topModelPairPair.getSecond();
                break;
                }
            }
        return requestedModelsIntervals;
        } // getModelIntervals

    public List<IntervalCollection> getTopSignificanceModels(final Dataset data)
        {
        final Console.ModelSignificanceMetric modelSignificanceMetric = Console.modelSignificanceMetric;
        final MetricRankOrder rankOrder = modelSignificanceMetric
                .getModelSignificanceMetricRankOrder();
        SortedMap<Double, List<IntervalCollection>> sortedMap;
        switch (rankOrder)
            {
            case ASCENDING:
                sortedMap = new TreeMap<Double, List<IntervalCollection>>();
                break;
            case DESCENDING:
                sortedMap = new TreeMap<Double, List<IntervalCollection>>(
                        Collections.reverseOrder());
                break;
            default:
                throw new IllegalArgumentException("Unknown MetricRankOrder: "
                                                   + rankOrder);
            }
        for (final Pair<Pair<String, Float>, IntervalCollection> topModelPairPair : landscape)
            {
            final IntervalCollection intervalCollection = topModelPairPair
                    .getSecond();
            final Model model = intervalCollection.get(0).getModel();
            final Double currentIntervalSignificance = model.getModelSignificance(
                    data, modelSignificanceMetric);
            List<IntervalCollection> listOfLists = sortedMap
                    .get(currentIntervalSignificance);
            if (listOfLists == null)
                {
                listOfLists = new ArrayList<IntervalCollection>();
                sortedMap.put(currentIntervalSignificance, listOfLists);
                }
            listOfLists.add(intervalCollection);
            }
        final List<IntervalCollection> bestModels = sortedMap.get(sortedMap
                                                                          .firstKey());
        return bestModels;
        }

    public List<IntervalCollection> getTopTestingModels(final boolean useTesting)
        {
        final SortedMap<Float, List<IntervalCollection>> sortedMap = new TreeMap<Float, List<IntervalCollection>>(
                Collections.reverseOrder());
        for (final Pair<Pair<String, Float>, IntervalCollection> topModelPairPair : landscape)
            {
            final IntervalCollection intervalsWithSameModel = topModelPairPair
                    .getSecond();
            final Float averageFitness = intervalsWithSameModel
                    .getAverageFitness(useTesting);
            List<IntervalCollection> listOfLists = sortedMap.get(averageFitness);
            if (listOfLists == null)
                {
                listOfLists = new ArrayList<IntervalCollection>();
                sortedMap.put(averageFitness, listOfLists);
                }
            listOfLists.add(intervalsWithSameModel);
            }
        final List<IntervalCollection> bestModels = sortedMap.get(sortedMap
                                                                          .firstKey());
        return bestModels;
        }

    public IntervalCollection getTopTopModelIntervals()
        {
        final Pair<Pair<String, Float>, IntervalCollection> topModelPairPair = landscape
                .first();
        return topModelPairPair.getSecond();
        }

    public List<IntervalCollection> getTopTrainingModels()
        {
        final List<IntervalCollection> listOfLists = new ArrayList<IntervalCollection>();
        Float bestTrainingAverage = null;
        for (final Pair<Pair<String, Float>, IntervalCollection> topModelPairPair : landscape)
            {
            final Float currentIntervalTrainingAverage = topModelPairPair.getFirst()
                    .getSecond();
            if ((bestTrainingAverage == null)
                || (bestTrainingAverage.compareTo(currentIntervalTrainingAverage) <= 0))
                {
                bestTrainingAverage = currentIntervalTrainingAverage;
                listOfLists.add(topModelPairPair.getSecond());
                }
            else
                {
                break;
                }
            }
        return listOfLists;
        }

    @Override
    public boolean isEmpty() {
    return (landscape == null) || landscape.isEmpty();
    }
    }
