package org.epistasis.combinatoric.mdr.newengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math.stat.correlation.Covariance;
import org.apache.commons.math.stat.inference.TestUtils;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Console.MetricCellComparisonScope;
import org.epistasis.combinatoric.mdr.Console.ModelSignificanceMetric;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.Dataset.Genotype;

import edu.northwestern.at.utils.math.distributions.Sig;
import edu.northwestern.at.utils.math.matrix.MatrixFactory;
import edu.northwestern.at.utils.math.statistics.ContingencyTable;

public class Model
    {
    public static final byte UNKNOWN_STATUS = (byte) 255;
    public static final byte MISSING_DATA = Model.UNKNOWN_STATUS;
    public static final byte INVALID_STATUS = (byte) 254;
    private final AmbiguousCellStatus tieStatus;
    private SortedMap<byte[], Cell> cells;
    private final AttributeCombination combo;
    private List<List<String>> levels;
    private final static Comparator<byte[]> byteArrayComparator = new Comparator<byte[]>()
    {
    public int compare(final byte[] o1, final byte[] o2)
        {
        if (o1.length != o2.length)
            {
            throw new RuntimeException(
                    "byte arrays are not the same length! first: "
                    + Arrays.toString(o1) + " second: " + Arrays.toString(o2));
            }
        int comparisonResult = 0;
        for (int index = 0; index < o1.length; ++index)
            {
            comparisonResult = o1[index] - o2[index];
            if (comparisonResult != 0)
                {
                break;
                }
            }
        return comparisonResult;
        }
    };
    private static final DecimalFormat plainNumberFormat = new DecimalFormat(
            "0.####");
    public static final Model EMPTY = new Model(AttributeCombination.EMPTY,
                                                AmbiguousCellStatus.UNASSIGNED);

    static
        {
        Model.EMPTY.cells = new TreeMap<byte[], Cell>(Model.byteArrayComparator);
        }

    static
        {
        Model.plainNumberFormat.setDecimalSeparatorAlwaysShown(false);
        }

    private static double getMetricCellWeight(final double diffFromExpected)
        {
        double cellWeight;
        if (diffFromExpected < 0)
            {
            throw new IllegalArgumentException(
                    "getMetricCellWeight expects diffFromExpected to be a positive value: "
                    + diffFromExpected);
            }
        switch (Console.metricDiffFromExpectedWeightingMethod)
            {
            case IDENTITY:
                cellWeight = diffFromExpected;
                break;
            case SQUARED:
                cellWeight = diffFromExpected * diffFromExpected;
                break;
            default:
                throw new RuntimeException(
                        "Unhandled switch case for Console.metricDiffFromExpectedWeightingMethod: "
                        + Console.metricDiffFromExpectedWeightingMethod);
            }
        return cellWeight;
        }

    private static double getMetricDiffFromExpected(final Cell cell,
                                                    final double expectedGenotypeAffectedCount)
        {
        final double diffFromExpected = Math.abs(cell.getAffected()
                                                 - expectedGenotypeAffectedCount);
        // if (diffFromExpected >= 0.5f) {
        // // reduce by 1/2 for a continuity correction
        // diffFromExpected -= 0.5f;
        // }
        return diffFromExpected;
        }

    public Model(final AttributeCombination combo,
                 final AmbiguousCellStatus tieStatus)
        {
        this.tieStatus = tieStatus;
        this.combo = combo;
        }

    private void addAlleleCount(final byte[] rowSlice, final byte numStatuses,
                                final byte affectedStatus, final byte currentRowStatus,
                                final float currentRowWeight)
        {
        addAlleleCount(rowSlice, numStatuses, affectedStatus, currentRowStatus,
                       currentRowWeight, 0, 1);
        }

    private void addAlleleCount(final byte[] rowSlice, final byte numStatuses,
                                final byte affectedStatus, final byte currentRowStatus,
                                final float currentRowWeight, final int currentComboIndex,
                                final int increment)
        {
        final List<String> comboValues = new ArrayList<String>(rowSlice.length);
        for (int comboIndex = 0; comboIndex < combo.size(); ++comboIndex)
            {
            final int attributeIndex = combo.get(comboIndex);
            final List<String> attributeValues = levels.get(attributeIndex);
            final byte levelIndex = rowSlice[comboIndex];
            comboValues.add(attributeValues.get(levelIndex));
            }
        // final String genotype = comboValues.toString();
        if (currentComboIndex < rowSlice.length)
            {
            // System.out.println("Genotype: " + genotype + " called with status: "
            // + currentRowStatus + " currentComboIndex: " + currentComboIndex
            // + "(\"" + comboValues.get(currentComboIndex) + "\")" + " increment:"
            // + increment);
            final int attributeIndex = combo.get(currentComboIndex);
            List<String> attributeValues = levels.get(attributeIndex);
            final byte levelIndex = rowSlice[currentComboIndex];
            final String attributeValue = attributeValues.get(levelIndex);
            if (attributeValue.equals("1"))
                {
                for (final String allele : new String[]{"0", "2"})
                    {
                    int levelIndexOfAttributeValue = attributeValues.indexOf(allele);
                    if (levelIndexOfAttributeValue == -1)
                        {
                        // the current allele, 0 or 2 must not have existed in the original column
                        // need to make a copy in case this levels list is shared with status column
                        attributeValues = new ArrayList<String>(attributeValues);
                        levels.set(attributeIndex, attributeValues);
                        attributeValues.add(allele);
                        levelIndexOfAttributeValue = attributeValues.size() - 1;
                        }
                    final byte[] rowSliceCopy = new byte[rowSlice.length];
                    for (int rowSliceIndex = 0; rowSliceIndex < rowSlice.length; ++rowSliceIndex)
                        {
                        rowSliceCopy[rowSliceIndex] = rowSlice[rowSliceIndex];
                        }
                    rowSliceCopy[currentComboIndex] = (byte) levelIndexOfAttributeValue;
                    addAlleleCount(rowSliceCopy, numStatuses, affectedStatus,
                                   currentRowStatus, currentRowWeight, currentComboIndex + 1, 1);
                    }
                } // end if heterozygote
            else
                {
                addAlleleCount(rowSlice, numStatuses, affectedStatus, currentRowStatus,
                               currentRowWeight, currentComboIndex + 1, 2);
                }
            }
        else
            {
            Cell c = cells.get(rowSlice);
            if (c == null)
                {
                c = new Cell(numStatuses, affectedStatus);
                cells.put(rowSlice, c);
                }
            // System.out.println("Genotype: " + genotype + " count for status "
            // + currentRowStatus + " changing from " + c.counts[currentRowStatus]
            // + " to " + (c.counts[currentRowStatus] + increment));
            c.counts[currentRowStatus] += increment;
            c.weightedCounts[currentRowStatus] += currentRowWeight * increment;
            }
        } // end addAlleleCount

    public void buildCounts(final Dataset data)
        {
        if (Console.convertGenotypeCountsToAlleleCounts)
            {
            buildCountsAllelic(data);
            return;
            }
        levels = data.getLevels();
        if (cells == null)
            {
            cells = new TreeMap<byte[], Cell>(Model.byteArrayComparator);
            }
        else
            {
            for (final Model.Cell c : cells.values())
                {
                if (c != null)
                    {
                    Arrays.fill(c.counts, 0);
                    Arrays.fill(c.weightedCounts, 0);
                    }
                }
            }
        final int statusColIndex = data.getStatusColIndex();
        final byte nStatus = data.getNumStatuses();
        final int[] attributeIndices = combo.getAttributeIndices();
        for (int rowIndex = 0; rowIndex < data.getRows(); ++rowIndex)
            {
            final byte status = data.getRawDatum(rowIndex, statusColIndex);
            final byte[] rowSlice = data.getRowSlice(rowIndex, attributeIndices);
            Cell c = cells.get(rowSlice);
            if (c == null)
                {
                c = new Cell(nStatus, data.getAffectedStatus());
                cells.put(rowSlice, c);
                }
            ++c.counts[status];
            c.weightedCounts[status] += data.getRowWeight(rowIndex);
            }
        }

    /**
     * Assumes data is SNP and that genotypes are 0, 1, or 2 with 0 and 2 representing homozygous and 1 heterozygous.
     * Instead of counting
     * genotypes, count alleles. Do this by counting each 0 and 2 twice and for a 1,
     * add one each to 0 and 2 and leave the count for 1 as
     * zero.
     *
     * @param data
     */
    public void buildCountsAllelic(final Dataset data)
        {
        levels = data.getLevels();
        if (cells == null)
            {
            cells = new TreeMap<byte[], Cell>(Model.byteArrayComparator);
            }
        else
            {
            for (final Model.Cell c : cells.values())
                {
                if (c != null)
                    {
                    Arrays.fill(c.counts, 0);
                    }
                }
            }
        final int statusColIndex = data.getStatusColIndex();
        final byte nStatus = data.getNumStatuses();
        final int[] attributeIndices = combo.getAttributeIndices();
        for (int rowIndex = 0; rowIndex < data.getRows(); ++rowIndex)
            {
            final byte status = data.getRawDatum(rowIndex, statusColIndex);
            final byte[] rowSlice = data.getRowSlice(rowIndex, attributeIndices);
            addAlleleCount(rowSlice, nStatus, data.getAffectedStatus(), status,
                           data.getRowWeight(rowIndex));
            }
        }

    public void buildStatuses(final Dataset data, final int[] statusCounts)
        {
        buildStatusesNew(data, statusCounts);
        // final byte[] newStatuses = new byte[cells.size()];
        // for (int index = 0; index < newStatuses.length; ++index) {
        // final Cell c = cells.get(index);
        // if (c == null) {
        // continue;
        // }
        // newStatuses[index] = c.status;
        // }
        // buildStatusesOld(data, statusCounts, skipped);
        // for (int index = 0; index < newStatuses.length; ++index) {
        // final Cell c = cells.get(index);
        // if (c == null) {
        // continue;
        // }
        // if (c.status != newStatuses[index]) {
        // System.out.println("Status differ for cell index " + index
        // + " with counts: " + Arrays.toString(c.getCounts())
        // + " affectedStatus: " + c.getAffectedStatus() + " old: " + c.status
        // + " new: " + newStatuses[index]);
        // buildStatusesNew(data, statusCounts, skipped);
        // buildStatusesOld(data, statusCounts, skipped);
        // }
        // }
        }

    public void buildStatusesNew(final Dataset data, final int[] statusCounts)
        {
        for (final Map.Entry<byte[], Cell> entry : cells.entrySet())
            {
            final Cell cell = entry.getValue();
            // if any of the attributes are missing then cell status must be set to unknown
            // Unknown status cells will be later used in confusion matrix to calculate coverage
            // and lower the fitness of the model
            boolean statusAlreadySet = false;
            final byte[] attributeLevelIndices = entry.getKey();
            // support for missing data is still experimental because of the
            // issue about how fitness should be adjusted for unclassifiable data
            if (Main.isExperimental)
                {
                final String missing = data.getMissing();
                // this is probably slow, especially since it has string comparisons
                for (int index = 0; index < combo.size(); ++index)
                    {
                    final int attributeIndex = combo.get(index);
                    final List<String> attributeLevels = getLevels().get(attributeIndex);
                    final byte attributeLevelIndex = attributeLevelIndices[index];
                    // can use object comparison == instead of String.equals here because
                    // Dataset read code makes sure all missing values use missing string object
                    if (attributeLevels.get(attributeLevelIndex) == missing)
                        {
                        cell.status = Model.UNKNOWN_STATUS;
                        statusAlreadySet = true;
                        continue;
                        }
                    }
                if (statusAlreadySet)
                    {
                    continue;
                    }
                } // end if experimental
            // if (c == null) {
            // continue;
            // }
            // code below in new as of 2008-10-20 and was not in mdr alpha 2.0
            final byte affectedStatus = data.getAffectedStatus();
            final byte unaffectedStatus = data.getUnaffectedStatus();
            final float affected = cell.counts[affectedStatus];
            final float unaffected = cell.counts[unaffectedStatus];
            final float affectedUnaffectedRatio;
            affectedUnaffectedRatio = statusCounts[affectedStatus]
                                      / (float) statusCounts[unaffectedStatus];
            // Need to adjust the counts for imbalances so that we can determine cell status
            // Also wish to keep the total of the count unchanged -- this is needed if we do a fisher's test for
            // significance since we don't wish
            // to change the quantities which would change the significance
            final float totalInCell = affected + unaffected;
            final float proportionalUnaffected = unaffected * affectedUnaffectedRatio;
            final float reductionRatio = totalInCell
                                         / (proportionalUnaffected + affected);
            final float normalizedUnaffected = proportionalUnaffected
                                               * reductionRatio;
            final float normalizedAffected = totalInCell - normalizedUnaffected;
            // System.out.println(" original affected: " + affected + "+           unaffected: " + unaffected + "=" +
            // (affected + unaffected)
            // + "\nnormalizedAffected: " + normalizedAffected + "+ normalizedUnaffected: " + normalizedUnaffected + "="
            // + (normalizedAffected + normalizedUnaffected + "\n"));
            byte status;
            boolean isSignificantDifference = Float.compare(normalizedAffected,
                                                            normalizedUnaffected) != 0;
            double twoTailedFisherExact = Double.NaN;
            // look for tie
            // EXPERIMENTAL
            if (isSignificantDifference && !Double.isNaN(Console.fishersThreshold)
                && (Console.fishersThreshold != 1.0f))
                {
                final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                        .createMatrix(2, 2);
                matrix.set(1, 1, normalizedAffected);
                matrix.set(1, 2, normalizedUnaffected);
                matrix.set(2, 1, normalizedUnaffected);
                matrix.set(2, 2, normalizedAffected);
                // double vector with three entries. [0] = two-sided Fisher's exact test. [1] = left-tail Fisher's
                // exact test. [2] =
                // right-tail
                // Fisher's exact test.
                final double[] fisherExactTestResults = ContingencyTable
                        .fishersExactTest(matrix);
                twoTailedFisherExact = fisherExactTestResults[0];
                isSignificantDifference = twoTailedFisherExact <= Console.fishersThreshold;
                } // end if doing fisher test
            if (isSignificantDifference)
                {
                if (normalizedAffected > normalizedUnaffected)
                    {
                    status = affectedStatus;
                    }
                else
                    {
                    status = unaffectedStatus;
                    }
                }
            else
                { // not significant so consider a tie
                switch (tieStatus)
                    {
                    case AFFECTED:
                        status = affectedStatus;
                        break;
                    case UNAFFECTED:
                        status = unaffectedStatus;
                        break;
                    case UNASSIGNED:
                        status = Model.UNKNOWN_STATUS;
                        break;
                    default:
                        throw new RuntimeException("Unhandled AmbiguousCellAssgnmentType: "
                                                   + tieStatus.toString());
                    }
                }
            cell.status = status;
            if (data.trackMostPredictiveGenotypes)
                {
                synchronized (data.mostSignificantGenotypes)
                    {
                    data.mostSignificantGenotypes.add(new Genotype(data, combo, status,
                                                                   entry.getKey(), affected, unaffected));
                    }
                }
            } // end cell loop
        } // end buildStatuses()

    public void buildStatusesOld(final Dataset data, final int[] statusCounts,
                                 final int[] skipped)
        {
        final byte nStatus = data.getNumStatuses();
        for (final Cell c : cells.values())
            {
            if (c == null)
                {
                continue;
                }
            final boolean useExactOld = false;
            float maxWeight = Float.NEGATIVE_INFINITY;
            byte maxStatus = Model.UNKNOWN_STATUS;
            for (byte status = 0; status < nStatus; ++status)
                {
                float weight;
                if (statusCounts[status] == 0)
                    {
                    continue;
                    }
                if (skipped == null)
                    {
                    weight = c.counts[status] / statusCounts[status];
                    }
                else
                    {
                    weight = c.counts[status] / (statusCounts[status] - skipped[status]);
                    }
                if (weight > maxWeight)
                    {
                    maxWeight = weight;
                    maxStatus = status;
                    }
                else if (!useExactOld && (Float.compare(weight, maxWeight) == 0))
                    {
                    switch (tieStatus)
                        {
                        case AFFECTED:
                            maxStatus = data.getAffectedStatus();
                            break;
                        case UNAFFECTED:
                            maxStatus = data.getUnaffectedStatus();
                            break;
                        case UNASSIGNED:
                            maxStatus = Model.UNKNOWN_STATUS;
                            break;
                        default:
                            throw new RuntimeException(
                                    "Unhandled AmbiguousCellAssgnmentType: "
                                    + tieStatus.toString());
                        }
                    }
                } // end status loop
            c.status = maxStatus;
            } // end cell loop
        } // end buildStatuses()

    public List<String> constructAttribute(final Dataset data)
        {
        levels = data.getLevels();
        if (cells == null)
            {
            return Collections.emptyList();
            }
        final byte[] raw = constructRawAttribute(data);
        final List<String> attribute = new ArrayList<String>(raw.length);
        final int statusCol = data.getCols() - 1;
        for (final byte status : raw)
            {
            if (status == Model.MISSING_DATA)
                {
                attribute.add(data.getMissing());
                }
            else
                {
                attribute.add(data.getLevels().get(statusCol).get(status));
                }
            }
        return attribute;
        }

    public byte[] constructRawAttribute(final Dataset data)
        {
        if (cells == null)
            {
            return new byte[0];
            }
        final byte[] attribute = new byte[data.getRows()];
        for (int i = 0; i < data.getRows(); ++i)
            {
            final byte[] rowSlice = data.getRowSlice(i, combo.getAttributeIndices());
            final Model.Cell c = cells.get(rowSlice);
            if (c != null)
                {
                attribute[i] = c.getStatus();
                }
            else
                {
                attribute[i] = Model.UNKNOWN_STATUS;
                }
            } // end row loop
        return attribute;
        }

    public SortedMap<byte[], Cell> getCells()
        {
        return cells;
        }

    public AttributeCombination getCombo()
        {
        return combo;
        }

    public List<List<String>> getLevels()
        {
        return levels;
        }

    private double getMetricNormalized(final double sumWeightedCells,
                                       final double sumWeightedSignificanceMetric, final Dataset data,
                                       final int numCells)
        {
        double normalizedSignificanceMetric;
        switch (Console.metricNormalizationMethod)
            {
            case NONE:
                normalizedSignificanceMetric = sumWeightedSignificanceMetric;
                break;
            case DIVIDE_BY_SUM_WEIGHTED_CELLS:
                normalizedSignificanceMetric = (sumWeightedCells == 0.0f) ? 0.0f
                                                                          : sumWeightedSignificanceMetric /
                                                                            sumWeightedCells;
                break;
            case DIVIDE_BY_CELL_COUNT:
                normalizedSignificanceMetric = sumWeightedSignificanceMetric / numCells;
                break;
            default:
                throw new RuntimeException(
                        "Unhandled switch case for metricNormalizationMethod: "
                        + Console.metricNormalizationMethod);
            }
        return normalizedSignificanceMetric;
        }

    public double getModelSignificance(final Dataset data,
                                       final ModelSignificanceMetric modelSignificanceMetric)
        {
        double significance;
        final SortedMap<byte[], Cell> oldCells = cells;
        cells = null;
        buildCounts(data);
        final SortedMap<byte[], Cell> datasetCells = cells;
        cells = oldCells;
        switch (modelSignificanceMetric)
            {
            case BINOMIAL:
                significance = getSignificanceMetricBinomial(data, datasetCells);
                break;
            case COVARIANCE:
                significance = getSignificanceMetricCovariance(data, datasetCells);
                break;
            case COVARIANCE_AFFECTED_VS_UNAFFECTED:
            {
            significance = getSignificanceMetricCovarianceAffectedVersusUnaffected(
                    data, datasetCells);
            break;
            }
            case CHI_SQUARED:
            {
            final double[] chiValueAndPValue = getSignificanceMetricChiSquared(
                    data, datasetCells);
            significance = chiValueAndPValue[0];
            break;
            }
            case CHI_SQUARED_OBSERVED_AFFECTED_VS_EXPECTED_AFFECTED:
            {
            final double[] chiValueAndPValue = getSignificanceMetricChiSquaredObservedAffectedVersusExpectedAffected(
                    data, datasetCells);
            significance = chiValueAndPValue[0];
            break;
            }
            case CHI_SQUARED_AFFECTED_VS_UNAFFECTED:
            {
            final double[] chiValueAndPValue = getSignificanceMetricChiSquaredAffectedVersusUnaffected(
                    data, datasetCells);
            significance = chiValueAndPValue[0];
            break;
            }
            case TTEST:
            {
            final double[] tTestAndPValue = getSignificanceMetricTTest(data,
                                                                       datasetCells);
            significance = -tTestAndPValue[0];
            }
            break;
            case HOMOSCEDASTIC_TTEST:
            {
            final double[] tTestAndPValue = getSignificanceMetricHomoscedasticTTest(
                    data, datasetCells);
            significance = 1.0 - tTestAndPValue[1];
            }
            break;
            case MAXIMUM_LIKELIHOOD:
            {
            significance = getSignificanceMetricMaximumLikelihood(data,
                                                                  datasetCells);
            break;
            }
            case MAXIMUM_LIKELIHOOD_AFFECTED_VS_UNAFFECTED:
            {
            significance = getSignificanceMetricMaximumLikelihoodAffectedVersusUnaffected(
                    data, datasetCells);
            break;
            }
            case G_TEST:
                significance = getSignificanceMetricGTest(data, datasetCells);
                break;
            case FISHERS_EXACT:
                significance = getSignificanceMetricFishersExpected(data, datasetCells);
                break;
            case FISHERS_EXACT_MULTIPLY:
                significance = getSignificanceMetricFishersExactMultiply(data,
                                                                         datasetCells);
                break;
            case FISHERS_EXACT_MULTIPLY_WEIGHTED:
                significance = getSignificanceMetricFishersExactMultiplyWeighted(data,
                                                                                 datasetCells);
                break;
            case HERITABILITY_FROM_ODDS_RATIO:
                significance = getSignificanceMetricHeritabilityFromOddsRatio(data,
                                                                              datasetCells);
                break;
            case HERITABILITY_FROM_COUNTS:
                significance = getSignificanceMetricHeritabilityFromCounts(data,
                                                                           datasetCells);
                break;
            default:
                throw new RuntimeException("Unhandled ModelSignificanceMetric: "
                                           + Console.modelSignificanceMetric);
            }
        return significance;
        }

    // private double getSignificanceMetricBinomial(final Dataset data,
    // final SortedMap<byte[], Cell> datasetCells) {
    // final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
    // double sumWeightedCells = 0;
    // double sumWeightedSignificanceMetric = 0;
    // final double datasetRatio = data.getAffectedStatusCount()
    // / (double) (data.getAffectedStatusCount() + data
    // .getUnaffectedStatusCount());
    // for (final Entry<byte[], Cell> pair : datasetCells.entrySet()) {
    // final Cell cell = pair.getValue();
    // final double expectedGenotypeAffectedCount = (datasetRatio * (cell
    // .getAffected() + cell.getUnaffected()));
    // final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
    // expectedGenotypeAffectedCount);
    // final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
    // final BinomialDistribution binomialDistribution = new BinomialDistributionImpl(
    // (int) (cell.getAffected() + cell.getUnaffected()), datasetRatio);
    // double twoTailedBinomialProbability = Float.NaN;
    // try {
    // twoTailedBinomialProbability = (1 - binomialDistribution
    // .cumulativeProbability(expectedGenotypeAffectedCount
    // - diffFromExpected, expectedGenotypeAffectedCount
    // + diffFromExpected));
    // } catch (final MathException ex) {
    // ex.printStackTrace();
    // }
    // final double weightedBinomialProbabilityForGenotype = (cellsToWeight * (1.0f - twoTailedBinomialProbability));
    // if (printDebug) {
    // System.out
    // .println("\nfor Model-cell: "
    // + combo
    // + ":"
    // + Arrays.toString(pair.getKey())
    // + " affected: "
    // + cell.getAffected()
    // + " unaffected: "
    // + cell.getUnaffected()
    // + " twoTailedBinomialProbability: "
    // + Main.defaultFormat.format(twoTailedBinomialProbability)
    // + " * diffFromExpected: "
    // + diffFromExpected
    // + " = "
    // + Main.defaultFormat
    // .format(weightedBinomialProbabilityForGenotype));
    // }
    // sumWeightedCells += cellsToWeight;
    // if (!Double.isNaN(weightedBinomialProbabilityForGenotype)) {
    // sumWeightedSignificanceMetric += weightedBinomialProbabilityForGenotype;
    // }
    // }
    // final double normalizedSignificanceMetric = getMetricNormalized(
    // sumWeightedCells, sumWeightedSignificanceMetric, data,
    // datasetCells.size());
    // if (printDebug) {
    // System.out.println(" sumWeightedSignificanceMetric("
    // + sumWeightedSignificanceMetric + ") / sumWeightedCells("
    // + sumWeightedCells + ") = normalizedSignificanceMetric("
    // + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
    // }
    // return normalizedSignificanceMetric;
    // }
    private double getSignificanceMetricBinomial(final Dataset data,
                                                 final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumLogBinomial = 0;
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        // double expectedAffected;
        // double expectedUnaffected;
        // int globalAffected = data.getAffectedStatusCount();
        // int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            // switch (Console.metricCellComparisonScope) {
            // case GLOBAL_DEPENDENT:
            // expectedAffected = globalAffected;
            // expectedUnaffected = globalUnaffected;
            // globalAffected -= cell.getAffected();
            // globalUnaffected -= cell.getUnaffected();
            // break;
            // case GLOBAL_INDEPENDENT:
            // expectedAffected = data.getAffectedStatusCount() - cell.getAffected();
            // expectedUnaffected = data.getUnaffectedStatusCount()
            // - cell.getUnaffected();
            // break;
            // case LOCAL:
            // expectedAffected = expectedGenotypeAffectedCount;
            // expectedUnaffected = (cell.getAffected() + cell.getUnaffected())
            // - expectedGenotypeAffectedCount;
            // break;
            // default:
            // throw new RuntimeException(
            // "Unhandled Console.metricCellComparisonScope: "
            // + Console.metricCellComparisonScope);
            // }
            final BinomialDistribution binomialDistribution = new BinomialDistributionImpl(
                    (int) (cell.getAffected() + cell.getUnaffected()), datasetRatio);
            double twoTailedBinomialProbability = Float.NaN;
            try
                {
                twoTailedBinomialProbability = (binomialDistribution
                                                        .cumulativeProbability(cell.getAffected()));
                }
            catch (final MathException ex)
                {
                ex.printStackTrace();
                }
            sumLogBinomial += Math.log(twoTailedBinomialProbability);
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            } // end cells
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, -sumLogBinomial, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" normalizedSignificanceMetric("
                               + normalizedSignificanceMetric + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double[] getSignificanceMetricChiSquared(final Dataset data,
                                                     final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        double expectedAffected;
        double expectedUnaffected;
        double chiSquaredValue = 0;
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expectedAffected = globalAffected;
                    expectedUnaffected = globalUnaffected;
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expectedAffected = data.getAffectedStatusCount() - cell.getAffected();
                    expectedUnaffected = data.getUnaffectedStatusCount()
                                         - cell.getUnaffected();
                    break;
                case LOCAL:
                    expectedAffected = expectedGenotypeAffectedCount;
                    expectedUnaffected = (cell.getAffected() + cell.getUnaffected())
                                         - expectedGenotypeAffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            double difference = cell.getAffected() - expectedAffected;
            double squaredDifferencesOverExpected = (difference * difference)
                                                    / expectedAffected;
            chiSquaredValue += squaredDifferencesOverExpected;
            difference = cell.getUnaffected() - expectedUnaffected;
            squaredDifferencesOverExpected = (difference * difference)
                                             / expectedUnaffected;
            chiSquaredValue += squaredDifferencesOverExpected;
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            } // end cells
        double chiSquaredPValue = Double.NaN;
        try
            {
            // degrees of freedom is minus 2 because the affected and unaffected of the last cell are determined by
            // all the preceding cells.
            chiSquaredPValue = Sig.chisquare(chiSquaredValue,
                                             (datasetCells.size() * 2) - 2);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        // if (chiSquaredPValue == 0.0) {
        // System.out.println(" chiSquaredValue: " + chiSquaredValue
        // + " chiSquaredPValue: " + chiSquaredPValue);
        // try {
        // chiSquaredPValue = Sig.chisquare(chiSquaredValue,
        // datasetCells.size() - 1);
        // } catch (final IllegalArgumentException ex) {
        // ex.printStackTrace();
        // }
        // }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, chiSquaredPValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" chiSquaredResults(" + chiSquaredPValue
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        // return normalizedSignificanceMetric;
        // return new double[] { chiSquaredValue, chiSquaredPValue };
        return new double[]{chiSquaredValue, chiSquaredPValue};
        }

    private double[] getSignificanceMetricChiSquaredAffectedVersusUnaffected(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final long[][] matrix = new long[Math.max(2, datasetCells.size())][2];
        int cellIndex = 0;
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            matrix[cellIndex][0] = (long) cell.getAffected();
            matrix[cellIndex][1] = (long) cell.getUnaffected();
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            cellIndex++;
            } // end cells
        double chiSquaredPValue = Double.NaN;
        double chiSquaredValue = Double.NaN;
        try
            {
            chiSquaredPValue = TestUtils.chiSquareTest(matrix);
            chiSquaredValue = TestUtils.chiSquare(matrix);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        catch (final MathException ex)
            {
            ex.printStackTrace();
            }
        // if (chiSquaredPValue == 0.0) {
        // System.out.println(" chiSquaredValue: " + chiSquaredValue
        // + " chiSquaredPValue: " + chiSquaredPValue);
        // try {
        // chiSquaredPValue = TestUtils.chiSquareTest(matrix);
        // } catch (final IllegalArgumentException ex) {
        // ex.printStackTrace();
        // } catch (final MathException ex) {
        // ex.printStackTrace();
        // }
        // }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, chiSquaredPValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" chiSquaredResults(" + chiSquaredPValue
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        // return normalizedSignificanceMetric;
        return new double[]{chiSquaredValue, chiSquaredPValue};
        }

    private double[] getSignificanceMetricChiSquaredObservedAffectedVersusExpectedAffected(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        double expected;
        double observed;
        double chiSquaredValue = 0;
        int globalAffected = data.getAffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expected = globalAffected;
                    globalAffected -= cell.getAffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expected = data.getAffectedStatusCount() - cell.getAffected();
                    break;
                case LOCAL:
                    expected = expectedGenotypeAffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            observed = cell.getAffected();
            final double difference = observed - expected;
            final double squaredDifferencesOverExpected = (difference * difference)
                                                          / expected;
            chiSquaredValue += squaredDifferencesOverExpected;
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            } // end cells
        double chiSquaredPValue = Double.NaN;
        try
            {
            chiSquaredPValue = Sig
                    .chisquare(chiSquaredValue, datasetCells.size() - 1);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        // if (chiSquaredPValue == 0.0) {
        // System.out.println(" chiSquaredValue: " + chiSquaredValue
        // + " chiSquaredPValue: " + chiSquaredPValue);
        // try {
        // chiSquaredPValue = Sig.chisquare(chiSquaredValue,
        // datasetCells.size() - 1);
        // } catch (final IllegalArgumentException ex) {
        // ex.printStackTrace();
        // }
        // }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, chiSquaredPValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" chiSquaredResults(" + chiSquaredPValue
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        // return normalizedSignificanceMetric;
        // return new double[] { chiSquaredValue, chiSquaredPValue };
        return new double[]{chiSquaredValue, chiSquaredPValue};
        }

    private double getSignificanceMetricCovariance(final Dataset data,
                                                   final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final double[] expected = new double[Math.max(2, datasetCells.size())];
        final double[] observed = new double[Math.max(2, datasetCells.size())];
        int cellIndex = 0;
        int globalAffected = data.getAffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expected[cellIndex] = globalAffected;
                    globalAffected -= cell.getAffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expected[cellIndex] = data.getAffectedStatusCount()
                                          - cell.getAffected();
                    break;
                case LOCAL:
                    expected[cellIndex] = expectedGenotypeAffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            observed[cellIndex] = cell.getAffected();
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            cellIndex++;
            } // end cells
        double covariance = Double.NaN;
        try
            {
            covariance = new Covariance().covariance(expected, observed);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, -covariance, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" covariance(" + covariance + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricCovarianceAffectedVersusUnaffected(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final double[] unaffected = new double[Math.max(2, datasetCells.size())];
        final double[] affected = new double[Math.max(2, datasetCells.size())];
        int cellIndex = 0;
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            affected[cellIndex] = cell.getAffected();
            unaffected[cellIndex] = cell.getUnaffected();
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            cellIndex++;
            } // end cells
        double covariance = Double.NaN;
        try
            {
            covariance = new Covariance().covariance(unaffected, affected);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, covariance, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" covariance(" + covariance + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricFishersExactMultiply(final Dataset data,
                                                             final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double multipliedFishers = 0.0f;
        double sumWeightedCells = 0.0f;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(2, 2);
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    matrix.set(1, 1, globalAffected);
                    matrix.set(1, 2, globalUnaffected);
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    matrix.set(1, 1, data.getAffectedStatusCount() - cell.getAffected());
                    matrix.set(1, 2,
                               data.getUnaffectedStatusCount() - cell.getUnaffected());
                    break;
                case LOCAL:
                    matrix.set(1, 1, expectedGenotypeAffectedCount);
                    matrix.set(1, 2, expectedGenotypeUnaffectedCount);
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            matrix.set(2, 1, cell.getAffected());
            matrix.set(2, 2, cell.getUnaffected());
            final double[] fisherExactTestResults = ContingencyTable
                    .fishersExactTest(matrix);
            final double twoTailedFisherExact = fisherExactTestResults[0];
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            // adding a log is equivalent to multiply
            multipliedFishers += Math.log(twoTailedFisherExact);
            }
        // final double pValue = Math.pow(Math.E, multipliedFishers);
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, -multipliedFishers, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" multipliedFishers(" + multipliedFishers
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricFishersExactMultiplyWeighted(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        double multipliedWeightedSignificanceMetric = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(2, 2);
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    matrix.set(1, 1, globalAffected);
                    matrix.set(1, 2, globalUnaffected);
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    matrix.set(1, 1, data.getAffectedStatusCount() - cell.getAffected());
                    matrix.set(1, 2,
                               data.getUnaffectedStatusCount() - cell.getUnaffected());
                    break;
                case LOCAL:
                    matrix.set(1, 1, expectedGenotypeAffectedCount);
                    matrix.set(1, 2, expectedGenotypeUnaffectedCount);
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            matrix.set(2, 1, cell.getAffected());
            matrix.set(2, 2, cell.getUnaffected());
            final double[] fisherExactTestResults = ContingencyTable
                    .fishersExactTest(matrix);
            final double twoTailedFisherExact = fisherExactTestResults[0];
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            double cellScore = twoTailedFisherExact;
            if (cellsToWeight != 0)
                {
                cellScore = cellsToWeight / twoTailedFisherExact;
                // System.out.println("twoTailedFisherExact: " + twoTailedFisherExact
                // + " cellsToWeight: " + cellsToWeight + " cellScore: " + cellScore);
                }
            multipliedWeightedSignificanceMetric += Math.log(cellScore);
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, multipliedWeightedSignificanceMetric, data,
                datasetCells.size());
        if (printDebug)
            {
            System.out.println(" sumWeightedSignificanceMetric("
                               + multipliedWeightedSignificanceMetric + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricFishersExpected(final Dataset data,
                                                        final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        double sumWeightedSignificanceMetric = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(2, 2);
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            if (Console.metricCellComparisonScope == MetricCellComparisonScope.GLOBAL_INDEPENDENT)
                {
                matrix.set(1, 1, data.getAffectedStatusCount() - cell.getAffected());
                matrix
                        .set(1, 2, data.getUnaffectedStatusCount() - cell.getUnaffected());
                }
            else
                {
                matrix.set(1, 1, expectedGenotypeAffectedCount);
                matrix.set(1, 2, expectedGenotypeUnaffectedCount);
                }
            matrix.set(2, 1, cell.getAffected());
            matrix.set(2, 2, cell.getUnaffected());
            final double[] fisherExactTestResults = ContingencyTable
                    .fishersExactTest(matrix);
            final double twoTailedFisherExact = fisherExactTestResults[0];
            final double weightedFisherForGenotype = cellsToWeight
                                                     * (1.0f - twoTailedFisherExact);
            if (printDebug)
                {
                System.out.println("\nfor Model-cell: " + combo + ":"
                                   + Arrays.toString(pair.getKey()) + " affected: "
                                   + cell.getAffected() + " unaffected: " + cell.getUnaffected()
                                   + " fisher's: " + Main.defaultFormat.format(twoTailedFisherExact)
                                   + " * diffFromExpected: " + diffFromExpected + " = "
                                   + Main.defaultFormat.format(weightedFisherForGenotype));
                }
            sumWeightedCells += cellsToWeight;
            if (!Double.isNaN(weightedFisherForGenotype))
                {
                sumWeightedSignificanceMetric += weightedFisherForGenotype;
                }
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, sumWeightedSignificanceMetric, data,
                datasetCells.size());
        if (printDebug)
            {
            System.out.println(" sumWeightedSignificanceMetric("
                               + sumWeightedSignificanceMetric + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricGTest(final Dataset data,
                                              final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        double expected;
        double observed;
        // gTest 2* sum over all cells observed*ln(observed/expected)
        double gTestValue = 0;
        int globalAffected = data.getAffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expected = globalAffected;
                    globalAffected -= cell.getAffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expected = data.getAffectedStatusCount() - cell.getAffected();
                    break;
                case LOCAL:
                    expected = expectedGenotypeAffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            observed = cell.getAffected();
            if (observed != 0.0)
                {
                final double gTestCellValue = 2 * observed
                                              * Math.log(observed / expected);
                gTestValue += gTestCellValue;
                final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                                expectedGenotypeAffectedCount);
                final double cellsToWeight = Model
                        .getMetricCellWeight(diffFromExpected);
                sumWeightedCells += cellsToWeight;
                }
            } // end cells
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, gTestValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" gTestValue(" + gTestValue + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricHeritabilityFromCounts(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X0 X1");
        double sumWeightedCells = 0;
        double sumWeightedSignificanceMetric = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            final double diffSquared = diffFromExpected * diffFromExpected;
            if (printDebug)
                {
                System.out.println("\nfor Model-cell: " + combo + ":"
                                   + Arrays.toString(pair.getKey()) + " affected: "
                                   + cell.getAffected() + " unaffected: " + cell.getUnaffected()
                                   + " diffFromExpected: " + diffFromExpected + " diffSquared: "
                                   + Main.defaultFormat.format(diffSquared));
                }
            sumWeightedCells += cellsToWeight;
            if (!Double.isNaN(diffSquared))
                {
                sumWeightedSignificanceMetric += diffSquared;
                }
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, sumWeightedSignificanceMetric, data,
                datasetCells.size());
        if (printDebug)
            {
            System.out.println(" sumWeightedSignificanceMetric("
                               + sumWeightedSignificanceMetric + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricHeritabilityFromOddsRatio(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X0 X1");
        double sumWeightedCells = 0;
        double sumWeightedSignificanceMetric = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double cellRatio = cell.getAffected()
                                     / (cell.getAffected() + cell.getUnaffected());
            final double diffRatioFromExpected = Math.abs(cellRatio - datasetRatio);
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            // final double cellsToWeight = cell.getAffected() + cell.getUnaffected();
            final double weightedHeritabilityForGenotype = cellsToWeight
                                                           * diffRatioFromExpected;
            if (printDebug)
                {
                System.out.println("\nfor Model-cell: " + combo + ":"
                                   + Arrays.toString(pair.getKey()) + " affected: "
                                   + cell.getAffected() + " unaffected: " + cell.getUnaffected()
                                   + " cellRatio: " + cellRatio + " diffRatioFromExpected: "
                                   + diffRatioFromExpected + " * weight: " + cellsToWeight + " = "
                                   + Main.defaultFormat.format(weightedHeritabilityForGenotype));
                }
            sumWeightedCells += cellsToWeight;
            if (!Double.isNaN(weightedHeritabilityForGenotype))
                {
                sumWeightedSignificanceMetric += weightedHeritabilityForGenotype;
                }
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, sumWeightedSignificanceMetric, data,
                datasetCells.size());
        if (printDebug)
            {
            System.out.println(" sumWeightedSignificanceMetric("
                               + sumWeightedSignificanceMetric + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double[] getSignificanceMetricHomoscedasticTTest(final Dataset data,
                                                             final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final double[] expected = new double[datasetCells.size() * 2];
        final double[] observed = new double[datasetCells.size() * 2];
        int cellIndex = 0;
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            final int cellOffset = cellIndex++ * 2;
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expected[cellOffset] = globalAffected;
                    expected[cellOffset + 1] = globalUnaffected;
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expected[cellOffset] = data.getAffectedStatusCount()
                                           - cell.getAffected();
                    expected[cellOffset + 1] = data.getUnaffectedStatusCount()
                                               - cell.getUnaffected();
                    break;
                case LOCAL:
                    expected[cellOffset] = expectedGenotypeAffectedCount;
                    expected[cellOffset + 1] = expectedGenotypeUnaffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            observed[cellOffset] = cell.getAffected();
            observed[cellOffset + 1] = cell.getUnaffected();
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            } // end cells
        double tTestPValue = Double.NaN;
        double tTestValue = Double.NaN;
        try
            {
            tTestPValue = TestUtils.homoscedasticT(expected, observed);
            tTestValue = TestUtils.t(expected, observed);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, tTestPValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" tTestResults(" + tTestPValue
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        // return normalizedSignificanceMetric;
        return new double[]{tTestValue, tTestPValue};
        }

    private double getSignificanceMetricMaximumLikelihood(final Dataset data,
                                                          final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(datasetCells.size() * 2, 2);
        int cellIndex = 0;
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            final int cellOffset = cellIndex++ * 2;
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    matrix.set(cellOffset + 1, 1, globalAffected);
                    matrix.set(cellOffset + 1, 2, globalUnaffected);
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    matrix.set(cellOffset + 1, 1,
                               data.getAffectedStatusCount() - cell.getAffected());
                    matrix.set(cellOffset + 1, 2,
                               data.getUnaffectedStatusCount() - cell.getUnaffected());
                    break;
                case LOCAL:
                    matrix.set(cellOffset + 1, 1, expectedGenotypeAffectedCount);
                    matrix.set(cellOffset + 1, 2, expectedGenotypeUnaffectedCount);
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            matrix.set(cellOffset + 2, 1, cell.getAffected());
            matrix.set(cellOffset + 2, 2, cell.getUnaffected());
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            }
        final double maximumLikelihoodResults = ContingencyTable
                .likelihoodRatio(matrix);
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, maximumLikelihoodResults, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" maximumLikelihoodResults("
                               + maximumLikelihoodResults + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double getSignificanceMetricMaximumLikelihoodAffectedVersusUnaffected(
            final Dataset data, final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(datasetCells.size(), 2);
        int cellIndex = 0;
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            matrix.set(cellIndex + 1, 1, cell.getAffected());
            matrix.set(cellIndex + 1, 2, cell.getUnaffected());
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            cellIndex++;
            }
        final double maximumLikelihoodResults = ContingencyTable
                .likelihoodRatio(matrix);
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, maximumLikelihoodResults, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" maximumLikelihoodResults("
                               + maximumLikelihoodResults + ") / sumWeightedCells("
                               + sumWeightedCells + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        return normalizedSignificanceMetric;
        }

    private double[] getSignificanceMetricTTest(final Dataset data,
                                                final SortedMap<byte[], Cell> datasetCells)
        {
        final boolean printDebug = false;// getCombo().toString().equals("X1 X6 X8");
        double sumWeightedCells = 0;
        final double datasetRatio = data.getAffectedStatusCount()
                                    / (double) (data.getAffectedStatusCount() + data
                .getUnaffectedStatusCount());
        final double[] expected = new double[datasetCells.size() * 2];
        final double[] observed = new double[datasetCells.size() * 2];
        int cellIndex = 0;
        int globalAffected = data.getAffectedStatusCount();
        int globalUnaffected = data.getUnaffectedStatusCount();
        for (final Entry<byte[], Cell> pair : datasetCells.entrySet())
            {
            final Cell cell = pair.getValue();
            final double expectedGenotypeAffectedCount = datasetRatio
                                                         * (cell.getAffected() + cell.getUnaffected());
            final double expectedGenotypeUnaffectedCount = (cell.getAffected() + cell
                    .getUnaffected()) - expectedGenotypeAffectedCount;
            final int cellOffset = cellIndex++ * 2;
            switch (Console.metricCellComparisonScope)
                {
                case GLOBAL_DEPENDENT:
                    expected[cellOffset] = globalAffected;
                    expected[cellOffset + 1] = globalUnaffected;
                    globalAffected -= cell.getAffected();
                    globalUnaffected -= cell.getUnaffected();
                    break;
                case GLOBAL_INDEPENDENT:
                    expected[cellOffset] = data.getAffectedStatusCount()
                                           - cell.getAffected();
                    expected[cellOffset + 1] = data.getUnaffectedStatusCount()
                                               - cell.getUnaffected();
                    break;
                case LOCAL:
                    expected[cellOffset] = expectedGenotypeAffectedCount;
                    expected[cellOffset + 1] = expectedGenotypeUnaffectedCount;
                    break;
                default:
                    throw new RuntimeException(
                            "Unhandled Console.metricCellComparisonScope: "
                            + Console.metricCellComparisonScope);
                }
            observed[cellOffset] = cell.getAffected();
            observed[cellOffset + 1] = cell.getUnaffected();
            final double diffFromExpected = Model.getMetricDiffFromExpected(cell,
                                                                            expectedGenotypeAffectedCount);
            final double cellsToWeight = Model.getMetricCellWeight(diffFromExpected);
            sumWeightedCells += cellsToWeight;
            } // end cells
        double tTestPValue = Double.NaN;
        double tTestValue = Double.NaN;
        try
            {
            tTestPValue = TestUtils.tTest(expected, observed);
            tTestValue = TestUtils.t(expected, observed);
            }
        catch (final IllegalArgumentException ex)
            {
            ex.printStackTrace();
            }
        catch (final MathException ex)
            {
            ex.printStackTrace();
            }
        final double normalizedSignificanceMetric = getMetricNormalized(
                sumWeightedCells, tTestPValue, data, datasetCells.size());
        if (printDebug)
            {
            System.out.println(" tTestResults(" + tTestPValue
                               + ") / sumWeightedCells(" + sumWeightedCells
                               + ") = normalizedSignificanceMetric("
                               + Main.defaultFormat.format(normalizedSignificanceMetric) + ")");
            }
        // return normalizedSignificanceMetric;
        return new double[]{tTestValue, tTestPValue};
        }

    public AmbiguousCellStatus getTieStatus()
        {
        return tieStatus;
        }

    public void read(final Dataset data, final BufferedReader r)
            throws IOException
        {
        String line = r.readLine();
        if (!line.equals("BeginModelDetail"))
            {
            throw new IOException();
            }
        levels = data.getLevels();
        final List<Byte> dims = new ArrayList<Byte>(combo.size());
        boolean useDenseMatrix = true;
        long ncells = 1;
        for (final int attr : combo.getAttributeIndices())
            {
            final byte dim = (byte) data.getLevels().get(attr).size();
            if (useDenseMatrix)
                {
                ncells *= dim;
                // need to stop checking if there are a lot of dimensions be
                useDenseMatrix = (ncells > 0) && (ncells < data.getRows());
                }
            dims.add(dim);
            }
        // if (useDenseMatrix) {
        // cells = new DenseMatrix<Cell>(dims);
        // } else {
        // cells = new SparseMatrix<Cell>(dims);
        // }
        cells = new TreeMap<byte[], Cell>(Model.byteArrayComparator);
        final byte nStatus = data.getNumStatuses();
        while ((line = r.readLine()) != null)
            {
            if (line.equals("EndModelDetail"))
                {
                break;
                }
            final String[] fields = line.split("\\s+");
            final String[] values = fields[0].split(",");
            final byte[] rowSlice = new byte[values.length];
            for (int i = 0; i < values.length; ++i)
                {
                final int attributeIndex = combo.get(i);
                final List<String> attributeLevels = data.getLevels().get(
                        attributeIndex);
                final String attributeValue = values[i];
                final int attributeValueIndex = attributeLevels.indexOf(attributeValue);
                if (attributeValueIndex == -1)
                    {
                    final String errorMessage = "For model " + combo.toString()
                                                + " Attribute '" + combo.getLabel(i)
                                                + "' which is column index #" + attributeIndex + "'s value '"
                                                + attributeValue + "' was not found in the attribute levels: "
                                                + attributeLevels.toString();
                    System.out.println(errorMessage);
                    throw new RuntimeException(errorMessage);
                    }
                rowSlice[i] = (byte) attributeValueIndex;
                }
            final Model.Cell c = new Cell(nStatus, data.getAffectedStatus());
            for (int i = 0; i < nStatus; ++i)
                {
                c.getCounts()[i] = Float.parseFloat(fields[i + 1]);
                }
            cells.put(rowSlice, c);
            }
        }

    /**
     * despite the name, this is used this is used for to evaluate both training and test data
     *
     * @param data
     * @return
     */
    public ConfusionMatrix test(final Dataset data)
        {
        final ConfusionMatrix ret = new ConfusionMatrix(data.getLevels().get(
                data.getCols() - 1), data.getAffectedStatus());
        for (final Cell c : cells.values())
            {
            // if (c == null) {
            // continue;
            // }
            // Note: cells with UNKNOWN status should be counted and added to confusion matrix
            for (byte status = 0; status < c.counts.length; ++status)
                {
                if (c.status == Model.INVALID_STATUS)
                    {
                    // if a genotype was not encountered in the training data but exists in the
                    // testing data we give it the tie status. That is because 'not encountered' is
                    // another way of saying the count was 0 and 0 -- a tie.
                    switch (tieStatus)
                        {
                        case AFFECTED:
                            c.status = data.getAffectedStatus();
                            break;
                        case UNAFFECTED:
                            c.status = data.getUnaffectedStatus();
                            break;
                        case UNASSIGNED:
                            c.status = Model.UNKNOWN_STATUS;
                            break;
                        default:
                            throw new RuntimeException(
                                    "Unhandled AmbiguousCellAssgnmentType: "
                                    + tieStatus.toString());
                        } // end switch
                    } // end if INVALID_STATUS
                ret.add(status, c.status, c.weightedCounts[status]);
                } // end for status
            } // end for cells
        return ret;
        }

    @Override
    public String toString() {
    return combo.toString();
    }

    public void write(final PrintWriter p)
        {
        p.println("BeginModelDetail");
        final SortedMap<byte[], Cell> matrix = getCells();
        for (final Map.Entry<byte[], Model.Cell> matrixCell : matrix.entrySet())
            {
            final byte[] dims = matrixCell.getKey();
            final Model.Cell cell = matrixCell.getValue();
            for (int i = 0; i < dims.length; ++i)
                {
                if (i != 0)
                    {
                    p.print(',');
                    }
                final int attributeIndex = combo.get(i);
                final List<String> attributeLevels = getLevels().get(attributeIndex);
                final int attributeValueIndex = dims[i];
                final String attributeValue = attributeLevels.get(attributeValueIndex);
                p.print(attributeValue);
                }
            for (final float count : cell.getCounts())
                {
                p.print(' ');
                p.print(Model.plainNumberFormat.format(count));
                }
            p.println();
            }
        p.println("EndModelDetail");
        }

    public static class Cell
        {
        private final float[] counts;
        private final float[] weightedCounts;
        private byte status;
        // TODO : when polytomy happens, this will go away
        private final byte affectedStatus;

        public Cell(final byte nStatus, final byte affectedStatus)
            {
            counts = new float[nStatus];
            weightedCounts = new float[nStatus];
            this.affectedStatus = affectedStatus;
            status = Model.INVALID_STATUS;
            }

        // TODO : when polytomy happens, this will go away
        public float getAffected()
            {
            return counts[affectedStatus == 0 ? 0 : 1];
            }

        // TODO : when polytomy happens, this will go away
        public byte getAffectedStatus()
            {
            return affectedStatus;
            }

        public float[] getCounts()
            {
            return counts;
            }

        public byte getStatus()
            {
            return status;
            }

        // TODO : when polytomy happens, this will go away
        public float getUnaffected()
            {
            return counts[affectedStatus == 0 ? 1 : 0];
            }

        @Override
        public String toString() {
        return "Cell: affected=" + getAffected() + " unaffected="
               + getUnaffected() + " status=" + status;
        }
        } // end private class Cell
    }
