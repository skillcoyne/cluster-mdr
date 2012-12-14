package org.epistasis.combinatoric.mdr.newengine;

import java.util.Arrays;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.Utility;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.ScoringMethod;
import org.epistasis.combinatoric.mdr.Main;

public class ConfusionMatrix
    {
    private final List<String> statuses;
    private final float[][] matrix;
    private float totalCount;
    private float unknownCount;
    // private Float balancedAccuracy = null;
    private final byte affectedStatusIndex;
    public final static ConfusionMatrix EMPTY = new ConfusionMatrix(
            Arrays.asList("empty status1", "empty status2"), (byte) 0);

    public static ConfusionMatrix getAverage(final List<ConfusionMatrix> matrices)
        {
        if (matrices.isEmpty())
            {
            return null;
            }
        final ConfusionMatrix ret = new ConfusionMatrix(matrices.get(0).statuses,
                                                        matrices.get(0).affectedStatusIndex);
        for (int i = 0; i < ret.matrix.length; ++i)
            {
            for (int j = 0; j < ret.matrix.length; ++j)
                {
                for (final ConfusionMatrix x : matrices)
                    {
                    ret.matrix[i][j] += x.matrix[i][j];
                    }
                ret.matrix[i][j] /= matrices.size();
                }
            }
        float sumUnknown = 0;
        float sumTotal = 0;
        for (final ConfusionMatrix x : matrices)
            {
            sumUnknown += x.unknownCount;
            sumTotal += x.totalCount;
            }
        ret.unknownCount = sumUnknown / matrices.size();
        ret.totalCount = sumTotal / matrices.size();
        return ret;
        }

    public ConfusionMatrix(final List<String> statuses,
                           final byte affectedStatusIndex)
        {
        this.statuses = statuses;
        matrix = new float[statuses.size()][statuses.size()];
        this.affectedStatusIndex = affectedStatusIndex;
        }

    // public void add(final int actual, final int predicted) {
    // add(actual, predicted, 1);
    // }
    public void add(final int actual, final int predicted, final float count)
        {
        totalCount += count;
        if (predicted == Model.UNKNOWN_STATUS)
            {
            unknownCount += count;
            }
        else
            {
            matrix[actual][predicted] += count;
            }
        }

    // public void add(final String actual, final String predicted) {
    // final int iActual = statuses.indexOf(actual);
    // if (iActual < 0) {
    // throw new IndexOutOfBoundsException("Unknown status '" + actual + "'.");
    // }
    // final int iPredicted = statuses.indexOf(predicted);
    // if (iPredicted < 0) {
    // throw new IndexOutOfBoundsException("Unknown status '" + predicted + "'.");
    // }
    // add(iActual, iPredicted);
    // }
    public float get(final int actual, final int predicted)
        {
        return matrix[actual][predicted];
        }

    // public float get(final String actual, final String predicted) {
    // final int iActual = statuses.indexOf(actual);
    // if (iActual < 0) {
    // throw new IndexOutOfBoundsException("Unknown class '" + actual + "'.");
    // }
    // final int iPredicted = statuses.indexOf(predicted);
    // if (iPredicted < 0) {
    // throw new IndexOutOfBoundsException("Unknown class '" + predicted + "'.");
    // }
    // return matrix[iActual][iPredicted];
    // }
    public float getAccuracy()
        {
        float correct = 0;
        float total = 0;
        for (int i = 0; i < matrix.length; ++i)
            {
            for (int j = 0; j < matrix[i].length; ++j)
                {
                if (i == j)
                    {
                    correct += matrix[i][j];
                    }
                total += matrix[i][j];
                }
            }
        return correct / total;
        }

    public float getBalancedAccuracy()
        {
        float balancedAccuracy;
        float sum = 0;
        for (int i = 0; i < matrix.length; ++i)
            {
            final float[] row = matrix[i];
            float total = 0;
            float correct = 0;
            for (int j = 0; j < row.length; ++j)
                {
                if (i == j)
                    {
                    correct += row[j];
                    }
                total += row[j];
                }
            if (total != 0)
                {
                sum += correct / total;
                }
            }
        balancedAccuracy = sum / matrix.length;
        return balancedAccuracy;
        }

    public float getChiSquared()
        {
        return Utility.computeChiSquared(matrix);
        }

    public int getChiSquaredDF()
        {
        return (matrix.length - 1) * (matrix.length - 1);
        }

    public float getClassifiedCount()
        {
        return totalCount - unknownCount;
        }

    float getCoverageRatio()
        {
        final float predicted = totalCount - unknownCount;
        final float coverage = predicted / totalCount;
        return coverage;
        }

    public float getFitness()
        {
        final float fitness = getScore(Console.scoringMethod);
        return fitness;
        }

    public float getFMeasure()
        {
        final float tp = numTruePositives();
        final float fn = numFalseNegatives();
        final float fp = numFalsePositives();
        return 2.0f * tp / (2.0f * tp + fp + fn);
        }

    public double getKappa()
        {
        return Utility.computeKappaStatistic(matrix);
        }

    public float getLogORStdErr()
        {
        final float tp = numTruePositives();
        final float fn = numFalseNegatives();
        final float fp = numFalsePositives();
        final float tn = numTrueNegatives();
        return (float) Math.sqrt(1.0f / tp + 1.0f / fp + 1.0f / tn + 1.0f / fn);
        }

    public float getOddsRatio()
        {
        final float tp = numTruePositives();
        final float fn = numFalseNegatives();
        final float fp = numFalsePositives();
        final float tn = numTrueNegatives();
        return (tp * tn) / (fp * fn);
        }

    public Pair<Float, Float> getORConfInt()
        {
        final float lor = (float) Math.log(getOddsRatio());
        final float lorse = getLogORStdErr();
        return new Pair<Float, Float>((float) Math.exp(lor - 1.96f * lorse),
                                      (float) Math.exp(lor + 1.96 * lorse));
        }

    public double getPrecision()
        {
        final float tp = numTruePositives();
        final float fp = numFalsePositives();
        return tp / (tp + fp);
        }

    public Float getScore(final Console.ScoringMethod scoringMethod)
        {
        final float score;
        final float balAccuracy = getBalancedAccuracy();
        switch (scoringMethod)
            {
            case BALANCED_ACCURACY:
                score = balAccuracy;
                break;
            case ADJUSTED_BALANCED_ACCURACY:
            {
            if (unknownCount == 0.0f)
                {
                score = balAccuracy;
                }
            else
                {
                score = (float) ((balAccuracy - 0.5f) * Math.sqrt(getCoverageRatio())) + 0.5f;
                }
            }
            break;
            case BALANCED_ACCURACY_TIMES_COVERAGE:
            {
            if (unknownCount == 0.0f)
                {
                score = balAccuracy;
                }
            else
                {
                score = balAccuracy * getCoverageRatio();
                }
            }
            break;
            case BALANCED_ACCURACY_RYAN:
            {
            if (unknownCount == 0.0f)
                {
                score = balAccuracy;
                }
            else
                {
                final float coverage = getCoverageRatio();
                // =(ba*coverage)+ (1-coverage)*0.5
                score = (balAccuracy * coverage) + ((1 - coverage) * 0.5f);
                }
            }
            break;
            default:
                throw new IllegalArgumentException("Unhandled ScoringMethod: "
                                                   + scoringMethod);
            }
        return score;
        }

    public float getSensitivity()
        {
        final float tp = numTruePositives();
        final float fn = numFalseNegatives();
        float sensitivity;
        if ((tp + fn) > 0)
            {
            sensitivity = tp / (tp + fn);
            }
        else
            {
            sensitivity = 0;
            }
        return sensitivity;
        }

    public float getSpecificity()
        {
        final float fp = numFalsePositives();
        final float tn = numTrueNegatives();
        float specificity;
        if ((tn + fp) > 0)
            {
            specificity = tn / (tn + fp);
            }
        else
            {
            specificity = 0;
            }
        return specificity;
        }

    public float getTotalCount()
        {
        return totalCount;
        }

    public float getUnknownCount()
        {
        return unknownCount;
        }

    public float numFalseNegatives()
        {
        float incorrect = 0;
        for (int i = 0; i < matrix.length; i++)
            {
            if (i == affectedStatusIndex)
                {
                for (int j = 0; j < matrix.length; j++)
                    {
                    if (j != affectedStatusIndex)
                        {
                        incorrect += matrix[i][j];
                        }
                    }
                }
            }
        return incorrect;
        }

    public float numFalsePositives()
        {
        float incorrect = 0;
        for (int i = 0; i < matrix.length; i++)
            {
            if (i != affectedStatusIndex)
                {
                for (int j = 0; j < matrix.length; j++)
                    {
                    if (j == affectedStatusIndex)
                        {
                        incorrect += matrix[i][j];
                        }
                    }
                }
            }
        return incorrect;
        }

    public float numTrueNegatives()
        {
        float correct = 0;
        for (int i = 0; i < matrix.length; i++)
            {
            if (i != affectedStatusIndex)
                {
                for (int j = 0; j < matrix.length; j++)
                    {
                    if (j != affectedStatusIndex)
                        {
                        correct += matrix[i][j];
                        }
                    }
                }
            }
        return correct;
        }

    public float numTruePositives()
        {
        float correct = 0;
        for (int j = 0; j < matrix.length; j++)
            {
            if (j == affectedStatusIndex)
                {
                correct += matrix[affectedStatusIndex][j];
                }
            }
        return correct;
        }

    public int size()
        {
        return matrix.length;
        }

    public String toMatrixString()
        {
        final StringBuffer b = new StringBuffer();
        final int width[] = new int[matrix.length];
        int labelwidth = 0;
        for (int i = 0; i < matrix.length; ++i)
            {
            final float[] row = matrix[i];
            for (int j = 0; j < row.length; ++j)
                {
                final int len = (int) Math.ceil(Math.log10(row[j]));
                if (len > width[j])
                    {
                    width[j] = len;
                    }
                }
            }
        for (int i = 0; i < matrix.length; ++i)
            {
            final int len = statuses.get(i).length();
            if (len > width[i])
                {
                width[i] = len;
                }
            if (len > labelwidth)
                {
                labelwidth = len;
                }
            }
        b.append(Utility.chrdup(' ', labelwidth + 2));
        for (int i = 0; i < matrix.length; ++i)
            {
            b.append(' ');
            b.append(Utility.padRight(statuses.get(i), width[i]));
            }
        b.append(Utility.NEWLINE);
        b.append(Utility.chrdup(' ', labelwidth + 2));
        for (int i = 0; i < matrix.length; ++i)
            {
            b.append(' ');
            b.append(Utility.chrdup('-', width[i]));
            }
        b.append(Utility.NEWLINE);
        for (int i = 0; i < matrix.length; ++i)
            {
            b.append(Utility.padRight(statuses.get(i), labelwidth));
            b.append(" | ");
            final float[] row = matrix[i];
            for (int j = 0; j < matrix.length; ++j)
                {
                if (j != 0)
                    {
                    b.append(' ');
                    }
                b.append(Utility.padLeft(Float.toString(row[j]), width[j]));
                }
            b.append(Utility.NEWLINE);
            }
        return b.toString();
        }

    @Override
    public String toString() {
    return toString(Console.scoringMethod);
    }

    public String toString(final ScoringMethod scoringMethod)
        {
        final StringBuffer sb = new StringBuffer();
        sb.append("total: " + Main.modelTextNumberFormat.format(getTotalCount()));
        sb.append(" unknown: "
                  + Main.modelTextNumberFormat.format(getUnknownCount()));
        if (getUnknownCount() > 0)
            {
            sb.append(" coverage: " + (int) (getCoverageRatio() * 100) + "%");
            }
        sb.append(" TP: " + Main.modelTextNumberFormat.format(numTruePositives()));
        sb.append(" FP: " + Main.modelTextNumberFormat.format(numFalsePositives()));
        sb.append(" TN: " + Main.modelTextNumberFormat.format(numTrueNegatives()));
        sb.append(" FN: " + Main.modelTextNumberFormat.format(numFalseNegatives()));
        sb.append(" accuracy: " + Main.modelTextNumberFormat.format(getAccuracy()));
        sb.append(" score: "
                  + Main.modelTextNumberFormat.format(getScore(scoringMethod)));
        return sb.toString();
        }
    }
