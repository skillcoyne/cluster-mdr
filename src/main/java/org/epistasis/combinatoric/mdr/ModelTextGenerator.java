package org.epistasis.combinatoric.mdr;

import java.text.NumberFormat;

import org.epistasis.ColumnFormat;
import org.epistasis.Pair;
import org.epistasis.Utility;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;
import org.epistasis.combinatoric.mdr.newengine.ConfusionMatrix;

public abstract class ModelTextGenerator
    {
    private final NumberFormat nf;
    private final double pValueTol;
    private final boolean isVerbose;
    @SuppressWarnings("unused")
    private final ScoringMethod scoringMethod;
    private final AmbiguousCellStatus tieStatus;

    public ModelTextGenerator(final Console.AmbiguousCellStatus tieStatus,
                              final Console.ScoringMethod scoringMethod, final NumberFormat nf,
                              final double pValueTol, final boolean isVerbose)
        {
        this.tieStatus = tieStatus;
        this.scoringMethod = scoringMethod;
        this.nf = nf;
        this.pValueTol = pValueTol;
        this.isVerbose = isVerbose;
        }

    protected String getResultText(final ConfusionMatrix result,
                                   final String name, final int labelWidth)
        {
        final StringBuffer b = new StringBuffer();
        final float total = result.getTotalCount();
        final float unknown = result.getUnknownCount();
        if (tieStatus == Console.AmbiguousCellStatus.UNASSIGNED)
            {
            b.append(ColumnFormat.fitStringWidth(name + " Adj. Bal. Accuracy:",
                                                 labelWidth));
            b.append(nf.format(result.getFitness()));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " Coverage:", labelWidth));
            final float coverageRatio = (total - unknown) / total;
            b.append(nf.format(coverageRatio));
            b.append(" (");
            b.append(nf.format(total - unknown));
            b.append(',');
            b.append(nf.format(total));
            b.append(')');
            b.append('\n');
            }
        b.append(ColumnFormat.fitStringWidth(name + " Balanced Accuracy:",
                                             labelWidth));
        b.append(nf.format(result.getBalancedAccuracy()));
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " Accuracy:", labelWidth));
        b.append(nf.format(result.getAccuracy()));
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " Sensitivity:", labelWidth));
        b.append(nf.format(result.getSensitivity()));
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " Specificity:", labelWidth));
        b.append(nf.format(result.getSpecificity()));
        b.append('\n');
        if (isVerbose)
            {
            b.append(ColumnFormat.fitStringWidth(name + " Precision:", labelWidth));
            b.append(nf.format(result.getPrecision()));
            b.append('\n');
            b.append(ColumnFormat
                             .fitStringWidth(name + " Rows examined:", labelWidth));
            b.append(nf.format(total));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " True Positive(TP):",
                                                 labelWidth));
            b.append(nf.format(result.numTruePositives()));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " False Positive(FP):",
                                                 labelWidth));
            b.append(nf.format(result.numFalsePositives()));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " True Negative(TN):",
                                                 labelWidth));
            b.append(nf.format(result.numTrueNegatives()));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " False Negative(FN):",
                                                 labelWidth));
            b.append(nf.format(result.numFalseNegatives()));
            b.append('\n');
            b.append(ColumnFormat.fitStringWidth(name + " Unknown:", labelWidth));
            b.append(nf.format(unknown));
            b.append('\n');
            }
        b.append(ColumnFormat.fitStringWidth(name + " Odds Ratio:", labelWidth));
        final double oddsRatio = result.getOddsRatio();
        b.append(nf.format(oddsRatio));
        if (!Double.isNaN(oddsRatio) && !Double.isInfinite(oddsRatio))
            {
            final Pair<Float, Float> confInt = result.getORConfInt();
            b.append(" (");
            b.append(nf.format(confInt.getFirst()));
            b.append(',');
            b.append(nf.format(confInt.getSecond()));
            b.append(')');
            }
        b.append('\n');
        final double chisq = result.getChiSquared();
        b.append(ColumnFormat.fitStringWidth(name + " \u03A7\u00B2:", labelWidth));
        b.append(nf.format(chisq));
        if (!Double.isNaN(chisq))
            {
            final double p = Utility.pchisq(chisq, result.getChiSquaredDF());
            b.append(" (p ");
            if (p < pValueTol)
                {
                b.append("< ");
                b.append(nf.format(pValueTol));
                }
            else
                {
                b.append("= ");
                b.append(nf.format(p));
                }
            b.append(')');
            }
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " Precision:", labelWidth));
        b.append(nf.format(result.getPrecision()));
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " Kappa:", labelWidth));
        b.append(nf.format(result.getKappa()));
        b.append('\n');
        b.append(ColumnFormat.fitStringWidth(name + " F-Measure:", labelWidth));
        b.append(nf.format(result.getFMeasure()));
        b.append('\n');
        return b.toString();
        }

    @Override
    public abstract String toString();
    }
