package org.epistasis.combinatoric.mdr.newengine;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.newengine.Interval.IntervalCollection;

public abstract class Landscape
    {
    private final Map<AttributeCombination, IntervalCollection> map = new HashMap<AttributeCombination,
            IntervalCollection>();
    protected final int intervalCount;

    public Landscape(final int intervalCount)
        {
        this.intervalCount = intervalCount;
        }

    public final synchronized void add(final int intervalNumber,
                                       final Model model, final ConfusionMatrix train,
                                       final List<Dataset> testSets)
        {
        IntervalCollection trainingModels = map.get(model.getCombo());
        if (trainingModels == null)
            {
            trainingModels = new IntervalCollection(intervalCount, testSets);
            map.put(model.getCombo(), trainingModels);
            }
        final int numberOfSetIntervals = trainingModels.setInterval(intervalNumber,
                                                                    new Interval(model, train));
        if (numberOfSetIntervals >= intervalCount)
            {
            final float averageTrainingFitness = trainingModels
                    .getAverageFitness(false);
            // float sumFitnesses = 0;
            // for (final Interval interval : trainingModels) {
            // sumFitnesses += interval.getTrain().getFitness();
            // }
            // final float averageTrainingFitness = sumFitnesses / intervalCount;
            addLandscapeItem(model.getCombo(), averageTrainingFitness, trainingModels);
            map.remove(model.getCombo());
            }
        }

    protected abstract void addLandscapeItem(
            final AttributeCombination attributeCombo,
            final float averageTrainingFitness,
            final IntervalCollection trainingModels);

    public abstract List<Pair<String, Float>> getLandscape();

    public abstract boolean isEmpty();

    public final String read(final LineNumberReader lnr,
                             final Pattern endPattern, final List<String> labels) throws IOException
        {
        String line;
        try
            {
            while (((line = lnr.readLine()) != null)
                   && ((endPattern == null) || !endPattern.matcher(line).matches()))
                {
                final String[] fields = line.split("\t");
                if (fields.length != 2)
                    {
                    throw new IOException(lnr.getLineNumber()
                                          + " Expected 2 fields, delimited by a tab, got " + fields.length
                                          + " fields.");
                    }
                addLandscapeItem(new AttributeCombination(fields[0], labels),
                                 Float.parseFloat(fields[1]), null);
                }
            }
        catch (final NumberFormatException e)
            {
            throw new IOException(Integer.toString(lnr.getLineNumber()) + ':'
                                  + e.getMessage());
            }
        return line;
        }

    public final void write(final Writer w)
        {
        final PrintWriter p = new PrintWriter(w);
        for (final Pair<String, Float> pair : getLandscape())
            {
            p.print(pair.getFirst());
            p.print('\t');
            p.println(pair.getSecond());
            }
        p.flush();
        }
    }
