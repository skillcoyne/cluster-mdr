package org.epistasis.combinatoric.mdr.newengine;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.newengine.Interval.IntervalCollection;

public class AllModelsLandscape extends Landscape
    {
    private final SoftReference<List<Pair<String, Float>>> softReferenceToLandscape;

    public AllModelsLandscape(final int intervals)
        {
        super(intervals);
        softReferenceToLandscape = new SoftReference<List<Pair<String, Float>>>(
                new LinkedList<Pair<String, Float>>());
        }

    @Override
    protected void addLandscapeItem(final AttributeCombination attributeCombo,
                                    final float averageTrainingFitness,
                                    final IntervalCollection trainingMatrices)
        {
        List<Pair<String, Float>> landscape = softReferenceToLandscape.get();
        if (landscape != null)
            {
            try
                {
                landscape.add(new Pair<String, Float>(attributeCombo.getComboString(),
                                                      averageTrainingFitness));
                }
            catch (final OutOfMemoryError outOfMemory)
                {
                landscape = null;
                softReferenceToLandscape.clear();
                System.gc();
                }
            }
        }

    @Override
    public List<Pair<String, Float>> getLandscape() {
    List<Pair<String, Float>> landscape = softReferenceToLandscape.get();
    if (landscape == null)
        {
        landscape = Collections.emptyList();
        }
    return landscape;
    }

    @Override
    public boolean isEmpty() {
    final List<Pair<String, Float>> landscape = getLandscape();
    return (landscape == null) || landscape.isEmpty();
    }
    }
