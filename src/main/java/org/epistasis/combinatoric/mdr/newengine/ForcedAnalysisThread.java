package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;

public class ForcedAnalysisThread extends AnalysisThread
    {
    private final int nAttr;

    public ForcedAnalysisThread(final Dataset data,
                                final Pair<List<Dataset>, List<Dataset>> partition,
                                final AmbiguousCellStatus tiePriorityList,
                                final ScoringMethod scoringMethod, final long seed,
                                final Runnable onEndModel, final Runnable onEndAttribute,
                                final Runnable onEnd, final boolean parallel,
                                final int allModelsLandscapeSize,
                                final boolean computeAllModelsLandscape, final AttributeCombination combo)
        {
        super(data, partition, tiePriorityList, scoringMethod, seed, onEndModel,
              onEndAttribute, onEnd, parallel, allModelsLandscapeSize,
              computeAllModelsLandscape);
        nAttr = combo.size();
        addProducer(new ForcedCombinationProducer(combo, getIntervals()));
        }

    @Override
    public int getMinAttr() {
    return nAttr;
    }

    private static class ForcedCombinationProducer extends Producer
        {
        private final AttributeCombination attributes;
        private int interval = 0;

        public ForcedCombinationProducer(final AttributeCombination attributes,
                                         final int intervals)
            {
            super(intervals);
            this.attributes = attributes;
            }

        @Override
        public QueueEntry produce() {
        if (interval >= getIntervals())
            {
            return null;
            }
        return new QueueEntry(attributes, interval++);
        }
        }
    }
