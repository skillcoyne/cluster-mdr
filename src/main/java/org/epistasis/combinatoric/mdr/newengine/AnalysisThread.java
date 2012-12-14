package org.epistasis.combinatoric.mdr.newengine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.ProducerConsumerThread;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;

public abstract class AnalysisThread extends Thread
    {
    protected final Dataset data;
    protected final Pair<List<Dataset>, List<Dataset>> partition;
    protected final int intervals;
    protected final Console.AmbiguousCellStatus tieStatus;
    protected final Console.ScoringMethod scoringMethod;
    private final long seed;
    protected final boolean parallel;
    protected final Runnable onEndModel;
    protected final Runnable onEndAttribute;
    protected final Runnable onEnd;
    protected boolean complete = false;
    protected final List<Producer> producers = new LinkedList<Producer>();
    protected final List<Collector> collectors = new ArrayList<Collector>();
    protected Collector collector;
    protected final int topModelsLandscapeSize;
    protected final AllModelsLandscape allModelsLandscape;

    public AnalysisThread(final Dataset data,
                          final Pair<List<Dataset>, List<Dataset>> partition,
                          final AmbiguousCellStatus tiePriority,
                          final Console.ScoringMethod scoringMethod, final long seed,
                          final Runnable onEndModel, final Runnable onEndAttribute,
                          final Runnable onEnd, final boolean parallel,
                          final int topModelsLandscapeSize, final boolean computeAllModelsLandscape)
        {
        this.data = data;
        this.partition = partition;
        this.scoringMethod = scoringMethod;
        tieStatus = tiePriority;
        this.seed = seed;
        intervals = partition == null ? 1 : partition.getFirst().size();
        this.onEndModel = onEndModel;
        this.onEndAttribute = onEndAttribute;
        this.onEnd = onEnd;
        this.parallel = parallel;
        this.topModelsLandscapeSize = topModelsLandscapeSize;
        allModelsLandscape = computeAllModelsLandscape ? new AllModelsLandscape(
                intervals) : null;
        }

    protected void addProducer(final Producer producer)
        {
        producers.add(producer);
        }

    public AllModelsLandscape getAllModelsLandscape()
        {
        return allModelsLandscape;
        }

    public List<Collector> getCollectors()
        {
        return collectors;
        }

    public int getIntervals()
        {
        return intervals;
        }

    public abstract int getMinAttr();

    public Console.ScoringMethod getScoringMethod()
        {
        return scoringMethod;
        }

    public long getSeed()
        {
        return seed;
        }

    public Console.AmbiguousCellStatus getTiePriority()
        {
        return tieStatus;
        }

    public boolean isComplete()
        {
        return complete;
        }

    @Override
    public void run() {
    for (final ProducerConsumerThread.Producer<QueueEntry> producer : producers)
        {
        final int processorsToUse = parallel ? Runtime.getRuntime()
                .availableProcessors() : 1;
        if (processorsToUse > 1)
            {
            final ProducerConsumerThread<QueueEntry> pct = new ProducerConsumerThread<QueueEntry>();
            pct.setProducer(producer);
            collectors.add(collector = new SynchronizedCollector(intervals,
                                                                 topModelsLandscapeSize, allModelsLandscape,
                                                                 partition == null ? null : partition.getSecond()));
            for (int j = 0; j < (processorsToUse); ++j)
                {
                pct.addConsumer(new ModelAnalyzer());
                }
            pct.run();
            }
        else
            {
            QueueEntry queueEntry;
            final ModelAnalyzer modelAnalyzer = new ModelAnalyzer();
            collectors.add(collector = new Collector(intervals,
                                                     topModelsLandscapeSize, allModelsLandscape,
                                                     partition == null ? null : partition.getSecond()));
            while (((queueEntry = producer.produce()) != null) && !isInterrupted())
                {
                modelAnalyzer.consume(queueEntry);
                }
            }
        if (intervals > 1)
            {
            // each interval represents the most fit attribute combination for each separate run of mdr
            collector.test();
            }
        collector.computeBest(data, tieStatus);
        if (Console.computeMdrByGenotype)
            {
            // only calculate on last analysis
            if (collectors.size() == producers.size())
                {
                collector.computeMdrByGenotype(partition);
                }
            }
        if (!isInterrupted() && (onEndAttribute != null))
            {
            onEndAttribute.run();
            }
        } // end loop over producers
    collector = null;
    complete = !isInterrupted();
    if (onEnd != null)
        {
        onEnd.run();
        }
    }

    protected class ModelAnalyzer extends
                                  ProducerConsumerThread.Consumer<QueueEntry>
        {
        @Override
        public void consume(final QueueEntry entry) {
        final Model model = new Model(entry.getAttributes(), tieStatus);
        final Dataset train = partition == null ? data : partition.getFirst()
                .get(entry.getInterval());
        model.buildCounts(train);
        model.buildStatuses(train, data.getStatusCounts());
        if (collector != null)
            {
            final ConfusionMatrix result = model.test(train);
            if (result != null)
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

    protected abstract static class Producer extends
                                             ProducerConsumerThread.Producer<QueueEntry>
        {
        private final int intervals;

        public Producer(final int intervals)
            {
            this.intervals = intervals;
            }

        public int getIntervals()
            {
            return intervals;
            }
        }

    protected static class QueueEntry
        {
        private final AttributeCombination attributes;
        private final int interval;

        public QueueEntry(final AttributeCombination attributes, final int interval)
            {
            this.attributes = attributes;
            this.interval = interval;
            }

        public AttributeCombination getAttributes()
            {
            return attributes;
            }

        public int getInterval()
            {
            return interval;
            }
        }
    }
