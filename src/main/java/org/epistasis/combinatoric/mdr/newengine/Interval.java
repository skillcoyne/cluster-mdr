package org.epistasis.combinatoric.mdr.newengine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Interval
    {
    private final Model model;
    private final ConfusionMatrix train;
    private ConfusionMatrix test;
    public static final Interval EMPTY = new Interval(Model.EMPTY,
                                                      ConfusionMatrix.EMPTY, ConfusionMatrix.EMPTY);

    Interval(final Model model, final ConfusionMatrix train)
        {
        this(model, train, null);
        }

    Interval(final Model model, final ConfusionMatrix train,
             final ConfusionMatrix test)
        {
        this.model = model;
        this.train = train;
        this.test = test;
        }

    public Model getModel()
        {
        return model;
        }

    public ConfusionMatrix getTest()
        {
        return test;
        }

    public ConfusionMatrix getTrain()
        {
        return train;
        }

    ConfusionMatrix test(final Dataset data)
        {
        model.buildCounts(data);
        return test = model.test(data);
        }

    public static class IntervalCollection implements Iterable<Interval>,
                                                      Comparable<IntervalCollection>
        {
        final Interval[] intervals;
        private int setIntervalsCount = 0;
        private final List<Dataset> testSets;

        public static IntervalCollection getBestAverage(final boolean useTesting,
                                                        final List<IntervalCollection> bestModels)
            {
            Float bestAverage = Float.NEGATIVE_INFINITY;
            IntervalCollection bestModel = null;
            for (final IntervalCollection intervalCollection : bestModels)
                {
                final Float averageTestingFitness = intervalCollection
                        .getAverageFitness(useTesting);
                if (bestAverage.compareTo(averageTestingFitness) < 0)
                    {
                    bestAverage = averageTestingFitness;
                    bestModel = intervalCollection;
                    }
                }
            return bestModel;
            }

        public IntervalCollection(final int intervalsCount,
                                  final List<Dataset> testSets)
            {
            intervals = new Interval[intervalsCount];
            this.testSets = testSets;
            }

        public IntervalCollection(final List<Interval> pIntervals,
                                  final List<Dataset> testSets)
            {
            this(pIntervals.size(), testSets);
            for (int index = 0; index < intervals.length; ++index)
                {
                setInterval(index, pIntervals.get(index));
                }
            }

        public int compareTo(final IntervalCollection o)
            {
            return getAttributeCombination().compareTo(o.getAttributeCombination());
            }

        public Interval get(final int index)
            {
            return intervals[index];
            }

        public AttributeCombination getAttributeCombination()
            {
            return intervals[0].model.getCombo();
            }

        public float getAverageFitness(final boolean useTesting)
            {
            float returnValue;
            if (useTesting && (testSets == null))
                {
                returnValue = Float.NaN;
                }
            else
                {
                final ConfusionMatrix averageConfusionMatrix = getAverageFitnessConfusionMatrix(useTesting);
                // final float fitnessOfAverageConfusionMatrix = averageConfusionMatrix
                // .getFitness();
                // final float averageFitness = getAverageFitnessOld(useTesting);
                // if (Float.compare(fitnessOfAverageConfusionMatrix, averageFitness) != 0) {
                // System.out.println("fitnessOfAverageConfusionMatrix: "
                // + fitnessOfAverageConfusionMatrix + " averageFitness:"
                // + averageFitness);
                // }
                returnValue = averageConfusionMatrix.getFitness();
                }
            return returnValue;
            }

        public ConfusionMatrix getAverageFitnessConfusionMatrix(
                final boolean useTesting)
            {
            final List<ConfusionMatrix> confusionMatrices = new ArrayList<ConfusionMatrix>(
                    intervals.length);
            for (int intervalIndex = 0; intervalIndex < intervals.length; ++intervalIndex)
                {
                final Interval interval = intervals[intervalIndex];
                if (useTesting)
                    {
                    ConfusionMatrix test = interval.getTest();
                    if (test == null)
                        {
                        // only calculate if did not do previously
                        if (testSets == null)
                            {
                            test = ConfusionMatrix.EMPTY;
                            }
                        else
                            {
                            final Dataset testingPartition = testSets.get(intervalIndex);
                            test = interval.test(testingPartition);
                            }
                        }
                    confusionMatrices.add(test);
                    }
                else
                    {
                    confusionMatrices.add(interval.getTrain());
                    }
                }
            final ConfusionMatrix averageConfusionMatrix = ConfusionMatrix
                    .getAverage(confusionMatrices);
            return averageConfusionMatrix;
            }

        public Interval[] getIntervalArray()
            {
            return intervals;
            }

        public Iterator<Interval> iterator()
            {
            return new Iterator<Interval>()
            {
            private int index = 0;

            public boolean hasNext()
                {
                return index < intervals.length;
                }

            public Interval next()
                {
                return intervals[index++];
                }

            public void remove()
                {
                throw new UnsupportedOperationException();
                }
            };
            } // end iterator()

        public int setInterval(final int index, final Interval newInterval)
            {
            if (intervals[index] != null)
                {
                throw new RuntimeException(
                        "IntervalCollection: something is wrong. setInterval called when interval index "
                        + index + " is already set.");
                }
            else if (setIntervalsCount >= intervals.length)
                {
                throw new RuntimeException(
                        "IntervalCollection: something is wrong. setInterval called when setIntervalsCount was already full");
                }
            else
                {
                intervals[index] = newInterval;
                ++setIntervalsCount;
                return setIntervalsCount;
                }
            }// end setInterval

        public int size()
            {
            return intervals.length;
            }

        @Override
        public String toString() {
        return getClass().getSimpleName() + " size: " + size() + " model: "
               + getAttributeCombination() + " training avg: "
               + getAverageFitness(false);
        }
        } // end class IntervalCollection
    } // end class Interval
