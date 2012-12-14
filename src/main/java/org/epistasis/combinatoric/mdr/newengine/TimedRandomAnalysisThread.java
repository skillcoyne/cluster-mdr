package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;

public class TimedRandomAnalysisThread extends RandomAnalysisThread
    {
    public TimedRandomAnalysisThread(final Dataset data,
                                     final Pair<List<Dataset>, List<Dataset>> partition,
                                     final AmbiguousCellStatus tiePriorityList,
                                     final ScoringMethod scoringMethod, final long seed,
                                     final Runnable onEndModel, final Runnable onEndAttribute,
                                     final Runnable onEnd, final boolean parallel,
                                     final int topModelsLandscapeSize,
                                     final boolean computeAllModelsLandscape, final int minAttr,
                                     final int maxAttr, final long millis)
        {
        super(data, partition, tiePriorityList, scoringMethod, seed, onEndModel,
              onEndAttribute, onEnd, parallel, topModelsLandscapeSize,
              computeAllModelsLandscape, minAttr);
        for (int nAttr = minAttr; nAttr <= maxAttr; ++nAttr)
            {
            addProducer(new RandomProducer(new TimedRandomCombinationGenerator(
                    data.getLabels(), nAttr, seed, millis), getIntervals()));
            }
        }
    }
