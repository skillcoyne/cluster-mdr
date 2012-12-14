package org.epistasis.combinatoric.mdr.newengine;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.epistasis.Pair;
import org.epistasis.ProducerConsumerThread;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.ExpertKnowledge.RWRuntime;

public class EDAAnalysisThread extends AnalysisThread
    {
    private final int minAttrs;
    private final int maxAttrs;
    private final int numEntities;
    private final int numUpdates;
    private final RWRuntime expertKnowledgeRWRuntime;
    private final Random rng;

    public EDAAnalysisThread(final Dataset data,
                             final Pair<List<Dataset>, List<Dataset>> partition,
                             final AmbiguousCellStatus tiePriorityList,
                             final Console.ScoringMethod scoringMethod, final long seed,
                             final Runnable onEndModel, final Runnable onEndAttribute,
                             final Runnable onEnd, final boolean parallel,
                             final int topModelsLandscapeSize,
                             final boolean computeAllModelsLandscape, final int minAttrs,
                             final int maxAttrs, final int numEntities, final int numUpdates,
                             final RWRuntime expertKnowledgeRWRuntime)
        {
        super(data, partition, tiePriorityList, scoringMethod, seed, onEndModel,
              onEndAttribute, onEnd, parallel, topModelsLandscapeSize,
              computeAllModelsLandscape);
        this.minAttrs = minAttrs;
        this.maxAttrs = maxAttrs;
        this.numEntities = numEntities;
        this.numUpdates = numUpdates;
        this.expertKnowledgeRWRuntime = expertKnowledgeRWRuntime;
        // create a random number generator that will be used to generate a seed for a random number generator
        // specific to each thread/level.
        rng = new Random(seed);
        }

    @Override
    public int getMinAttr() {
    return minAttrs;
    }

    @Override
    public void run() {
    for (int numAttrs = minAttrs; (numAttrs <= maxAttrs) && !isInterrupted(); numAttrs++)
        {
        expertKnowledgeRWRuntime.initializeWheel();
        final int processorsToUse = parallel ? Runtime.getRuntime()
                .availableProcessors() : 1;
        if (processorsToUse > 1)
            {
            // each level starts off with the same initial version of the wheel
            collectors.add(collector = new SynchronizedCollector(intervals,
                                                                 topModelsLandscapeSize, allModelsLandscape,
                                                                 (partition == null) ? null : partition.getSecond()));
            // run for multiple generations
            for (int updateCtr = 0; (updateCtr < numUpdates) && !isInterrupted(); ++updateCtr)
                {
                final EDACombinationGenerator combinationGenerator = new EDACombinationGenerator(
                        data.getLabels(), numAttrs, new Random(rng.nextLong()),
                        numEntities, expertKnowledgeRWRuntime);
                final ProducerConsumerThread.Producer<QueueEntry> producer = new EDAProducer(
                        combinationGenerator, getIntervals());
                final ProducerConsumerThread<QueueEntry> pct = new ProducerConsumerThread<QueueEntry>();
                pct.setProducer(producer);
                for (int j = 0; j < (processorsToUse); ++j)
                    {
                    pct.addConsumer(new EDAModelAnalyzer());
                    }
                pct.run();
                // update rouletteWheel after every generation
                if (updateCtr < (numUpdates - 1))
                    {
                    expertKnowledgeRWRuntime.adjustRouletteWheel();
                    }
                }
            }
        else
            { // not parallel
            // each level starts off with the same initial version of the wheel
            collectors.add(collector = new Collector(intervals,
                                                     topModelsLandscapeSize, allModelsLandscape,
                                                     (partition == null) ? null : partition.getSecond()));
            // run for multiple generations
            for (int updateCtr = 0; (updateCtr < numUpdates) && !isInterrupted(); ++updateCtr)
                {
                final EDACombinationGenerator combinationGenerator = new EDACombinationGenerator(
                        data.getLabels(), numAttrs, new Random(rng.nextLong()),
                        numEntities, expertKnowledgeRWRuntime);
                final ProducerConsumerThread.Producer<QueueEntry> producer = new EDAProducer(
                        combinationGenerator, getIntervals());
                QueueEntry queueEntry;
                final EDAModelAnalyzer modelAnalyzer = new EDAModelAnalyzer();
                while (((queueEntry = producer.produce()) != null)
                       && !isInterrupted())
                    {
                    modelAnalyzer.consume(queueEntry);
                    }
                // update rouletteWheel after every generation
                if (updateCtr < (numUpdates - 1))
                    {
                    expertKnowledgeRWRuntime.adjustRouletteWheel();
                    }
                } // end updateCtr
            } // end if !parallel
        if (intervals > 1)
            {
            collector.test();
            }
        collector.computeBest(data, tieStatus);
        if (!isInterrupted() && (onEndAttribute != null))
            {
            onEndAttribute.run();
            }
        }
    collector = null;
    complete = !isInterrupted();
    if (onEnd != null)
        {
        onEnd.run();
        }
    }

    protected class EDAModelAnalyzer extends ModelAnalyzer
        {
        @Override
        public void consume(final QueueEntry entry) {
        final Model model = new Model(entry.getAttributes(), tieStatus);
        final Dataset train = partition == null ? data : partition.getFirst()
                .get(entry.getInterval());
        model.buildCounts(train);
        model.buildStatuses(train, data.getStatusCounts());
        final ConfusionMatrix result = model.test(train);
        if (result != null)
            {
            expertKnowledgeRWRuntime.trackFitness(entry.getAttributes(),
                                                  result.getFitness());
            if (collector != null)
                {
                collector.consider(entry.getInterval(), model, result);
                }
            }
        if (onEndModel != null)
            {
            onEndModel.run();
            }
        }
        }

    protected static class EDAProducer extends Producer
        {
        private AttributeCombination attributes;
        private final Iterator<AttributeCombination> ecg;
        private int interval = 0;

        public EDAProducer(final EDACombinationGenerator ecg, final int intervals)
            {
            super(intervals);
            this.ecg = ecg;
            }

        @Override
        public QueueEntry produce() {
        if ((interval == 0) && !ecg.hasNext())
            {
            return null;
            }
        if (interval == 0)
            {
            attributes = ecg.next();
            }
        final QueueEntry entry = new QueueEntry(attributes, interval);
        interval = (interval + 1) % getIntervals();
        return entry;
        }

        public void resetCombinationGenerator()
            {
            }
        }
    }
