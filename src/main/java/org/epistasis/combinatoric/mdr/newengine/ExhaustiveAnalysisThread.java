package org.epistasis.combinatoric.mdr.newengine;

import java.util.Iterator;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;
import org.epistasis.combinatoric.mdr.Main;

public class ExhaustiveAnalysisThread extends AnalysisThread
    {
    private final int minAttr;

    public ExhaustiveAnalysisThread(final Dataset data,
                                    final Pair<List<Dataset>, List<Dataset>> partition,
                                    final AmbiguousCellStatus tiePriority, final ScoringMethod scoringMethod,
                                    final long seed, final Runnable onEndModel,
                                    final Runnable onEndAttribute, final Runnable onEnd,
                                    final boolean parallel, final int topModelsLandscapeSize,
                                    final boolean computeAllModelsLandscape, final int minAttr,
                                    final int maxAttr)
        {
        this(data, partition, tiePriority, scoringMethod, seed, onEndModel,
             onEndAttribute, onEnd, parallel, topModelsLandscapeSize,
             computeAllModelsLandscape, minAttr, maxAttr, Main.defaultMDRNodeCount,
             Main.defaultMDRNodeNumber);
        }

    public ExhaustiveAnalysisThread(final Dataset data,
                                    final Pair<List<Dataset>, List<Dataset>> partition,
                                    final AmbiguousCellStatus tiePriorityList,
                                    final ScoringMethod scoringMethod, final long seed,
                                    final Runnable onEndModel, final Runnable onEndAttribute,
                                    final Runnable onEnd, final boolean parallel,
                                    final int topModelsLandscapeSize,
                                    final boolean computeAllModelsLandscape, final int minAttr,
                                    final int maxAttr, final int mdrNodeCount, final int mdrNodeNumber)
        {
        super(data, partition, tiePriorityList, scoringMethod, seed, onEndModel,
              onEndAttribute, onEnd, parallel, topModelsLandscapeSize,
              computeAllModelsLandscape);
        this.minAttr = minAttr;
        final int nVars = data.getCols() - 1;
        for (int nAttr = minAttr; nAttr <= maxAttr; ++nAttr)
            {
            addProducer(new ExhaustiveProducer(nVars, nAttr, data.getLabels(),
                                               getIntervals(), mdrNodeCount, mdrNodeNumber));
            }
        }

    @Override
    public int getMinAttr() {
    return minAttr;
    }

    private static class ExhaustiveProducer extends Producer
        {
        private AttributeCombination attributes;
        private final Iterator<AttributeCombination> acg;
        private int interval = 0;

        public ExhaustiveProducer(final int nVars, final int nCombo,
                                  final List<String> labels, final int intervals, final int mdrNodeCount,
                                  final int mdrNodeNumber)
            {
            super(intervals);
            if (mdrNodeCount == Main.defaultMDRNodeCount)
                {
                acg = new CombinationGenerator(nVars, nCombo, labels);
                }
            else
                {
                acg = new SkippingCombinationGenerator(nVars, nCombo, labels,
                                                       mdrNodeCount, mdrNodeNumber);
                }
            }

        @Override
        public QueueEntry produce() {
        if ((interval == 0) && !acg.hasNext())
            {
            return null;
            }
        if (interval == 0)
            {
            attributes = acg.next();
            }
        final QueueEntry entry = new QueueEntry(attributes, interval);
        interval = (interval + 1) % getIntervals();
        return entry;
        }
        }
    }
