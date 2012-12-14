package org.epistasis.combinatoric.mdr.newengine;

import java.util.Iterator;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;

public abstract class RandomAnalysisThread extends AnalysisThread
    {
    private final int minAttr;

    protected RandomAnalysisThread(final Dataset data,
                                   final Pair<List<Dataset>, List<Dataset>> partition,
                                   final AmbiguousCellStatus tiePriorityList,
                                   final ScoringMethod scoringMethod, final long seed,
                                   final Runnable onEndModel, final Runnable onEndAttribute,
                                   final Runnable onEnd, final boolean parallel,
                                   final int topModelsLandscapeSize,
                                   final boolean computeAllModelsLandscape, final int minAttr)
        {
        super(data, partition, tiePriorityList, scoringMethod, seed, onEndModel,
              onEndAttribute, onEnd, parallel, topModelsLandscapeSize,
              computeAllModelsLandscape);
        this.minAttr = minAttr;
        }

    @Override
    public int getMinAttr() {
    return minAttr;
    }

    protected static class RandomProducer extends Producer
        {
        private AttributeCombination attributes;
        private final Iterator<AttributeCombination> rcg;
        private int interval = 0;

        public RandomProducer(final RandomCombinationGenerator rcg,
                              final int intervals)
            {
            super(intervals);
            this.rcg = rcg;
            }

        @Override
        public QueueEntry produce() {
        if ((interval == 0) && !rcg.hasNext())
            {
            return null;
            }
        if (interval == 0)
            {
            attributes = rcg.next();
            }
        final QueueEntry entry = new QueueEntry(attributes, interval);
        interval = (interval + 1) % getIntervals();
        return entry;
        }
        }
    }
