package org.epistasis.combinatoric.mdr;

import java.text.NumberFormat;

import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Interval;

public class CVResultsTextGenerator extends ModelTextGenerator
    {
    private final Collector coll;

    public CVResultsTextGenerator(final Collector coll,
                                  final Console.AmbiguousCellStatus tieStatus,
                                  final Console.ScoringMethod scoringMethod, final byte affectedStatus,
                                  final NumberFormat nf, final double pValueTol, final boolean isVerbose)
        {
        super(tieStatus, scoringMethod, nf, pValueTol, isVerbose);
        this.coll = coll;
        }

    private void addInterval(final StringBuffer b, final int idx)
        {
        final Interval interval = coll.getInterval(idx);
        b.append("Cross Validation ");
        b.append(idx + 1);
        b.append(" of ");
        b.append(coll.size());
        b.append(": ");
        b.append(interval.getModel().getCombo());
        b.append("\n");
        b.append(getResultText(interval.getTrain(), " Training", 28));
        if (interval.getTest() != null)
            {
            b.append("\n");
            b.append(getResultText(interval.getTest(), " Testing", 28));
            }
        }

    @Override
    public String toString() {
    final StringBuffer b = new StringBuffer();
    for (int i = 0; i < coll.size(); ++i)
        {
        if (i != 0)
            {
            b.append('\n');
            }
        addInterval(b, i);
        }
    return b.toString();
    }
    }
