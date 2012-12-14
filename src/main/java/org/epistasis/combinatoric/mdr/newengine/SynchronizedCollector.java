package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;

public class SynchronizedCollector extends Collector
    {
    private final Object[] locks;

    public SynchronizedCollector(final int intervals,
                                 final int topModelsLandscapeSize,
                                 final AllModelsLandscape allModelsLandscape, final List<Dataset> testSets)
        {
        super(intervals, topModelsLandscapeSize, allModelsLandscape, testSets);
        locks = new Object[intervals];
        for (int i = 0; i < intervals; ++i)
            {
            locks[i] = new Object();
            }
        }

    @Override
    public void consider(final int interval, final Model model,
                         final ConfusionMatrix train)
        {
        synchronized (locks[interval])
            {
            super.consider(interval, model, train);
            }
        }

    @Override
    public Interval getInterval(final int interval) {
    synchronized (locks[interval])
        {
        return super.getInterval(interval);
        }
    }
    }
