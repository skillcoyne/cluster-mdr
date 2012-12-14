package org.epistasis.combinatoric.mdr.newengine;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.epistasis.Pair;
import org.epistasis.PriorityList;
import org.epistasis.Utility;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.Dataset.Genotype.GenotypeQualityMetric;
import org.epistasis.gui.SwingInvoker;

import edu.northwestern.at.utils.math.matrix.MatrixFactory;
import edu.northwestern.at.utils.math.statistics.ContingencyTable;

public class Dataset
    {
    private static final String sDelim = "\t";
    private static final Pattern patDelim = Pattern.compile("[\\s,]+");
    private List<String> labels = Collections.emptyList();
    private List<List<String>> levels;
    private int[] statusCounts;
    /*
      * weighted status counts takes the weights into account. At first I thought that this should be used when using
       * weights but it seemed
      * strange to Jason and me that the ratios changed due to weighting. For now, the weights alter the contingency
      * table and balanced
      * accuracy but not the ratio
      */
    private final String missing;
    private byte[][] data;
    private float[] weights;
    private boolean paired = false;
    private boolean pairedEnabled = false;
    private int rows = 0;
    private int cols = 0;
    private byte affectedStatus = Model.UNKNOWN_STATUS;
    private byte unaffectedStatus = Model.UNKNOWN_STATUS;
    public boolean trackMostPredictiveGenotypes = Main.isExperimental;
    PriorityList<Genotype> mostSignificantGenotypes = trackMostPredictiveGenotypes ? new PriorityList<Genotype>(
            10000) : null;
    private static Logger log = Logger.getLogger(Dataset.class.getName());

    static
        {
        Dataset.log.setLevel(Level.OFF);
        }

    private static void mdrByGenotypeClassifyDataset(final Dataset dataset,
                                                     final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod,
                                                     final boolean updateGenotypesConfusionMatrices,
                                                     final ConfusionMatrix confusionMatrix,
                                                     final Map<String, MutableFloat> attributeCountMap,
                                                     final List<Genotype> sortedGenotypes)
        {
        final int[] genotypeMatchCounts = new int[sortedGenotypes.size()];
        int totalGenotypeMatches = 0;
        for (int rowIndex = 0; rowIndex < dataset.getRows(); ++rowIndex)
            {
            final float[] statusVotes = new float[dataset.getNumStatuses()];
            final byte status = dataset.getStatusLevelIndex(rowIndex);
            byte predictedStatus = Model.UNKNOWN_STATUS;
            for (int genotypeIndex = 0; genotypeIndex < sortedGenotypes.size(); ++genotypeIndex)
                {
                final Genotype genotype = sortedGenotypes.get(genotypeIndex);
                final byte[] rowSlice = dataset.getRowSlice(rowIndex,
                                                            genotype.attributeCombination.getAttributeIndices());
                if (Arrays.equals(rowSlice, genotype.rowSlice))
                    {
                    predictedStatus = genotype.predictedStatus;
                    if (updateGenotypesConfusionMatrices)
                        {
                        genotype.predict(status, dataset.getRowWeight(rowIndex));
                        }
                    ++genotypeMatchCounts[genotypeIndex];
                    ++totalGenotypeMatches;
                    float voteWeight = Float.NaN;
                    switch (mdrByGenotypeVotingMethod)
                        {
                        case FIRST_MATCH_PREDICTS:
                            voteWeight = 1;
                            break;
                        case FIRST_MATCH_PREDICTS_WEIGHT_INVERSE_FISHERS:
                            voteWeight = (float) (1.0 - genotype.getFisherExact());
                            break;
                        case FIRST_MATCH_PREDICTS_WEIGHT_ACCURACY:
                            if (genotype.confusionMatrix != null)
                                {
                                voteWeight = genotype.confusionMatrix.getAccuracy();
                                }
                            else
                                {
                                // this must be testing and this genotype was never previously seen
                                // any positive value will cause us to go with this genotype's prediction. It would
                                // be cheating to look at the status during
                                // testing. So 50% chance of being correct/incorrect. Split difference and assign
                                // voteWeight of 0.5
                                voteWeight = 0.5f;
                                }
                            break;
                        case FIRST_MATCH_PREDICTS_WEIGHT_MAJORITY_RATIO:
                            voteWeight = genotype.getMajorityRatio();
                            break;
                        case SUM_OF_ALL_MATCHES:
                            voteWeight = 1;
                            break;
                        case WEIGHTED_SUM_FISHER_FOR_ALL_MATCHES:
                            voteWeight = (float) (1.0 - genotype.getFisherExact());
                            break;
                        case WEIGHTED_SUM_OF_ACCURACY_FOR_ALL_MATCHES:
                            voteWeight = genotype.confusionMatrix.getAccuracy();
                            break;
                        case WEIGHTED_SUM_OF_MAJORITY_RATIO_FOR_ALL_MATCHES:
                            voteWeight = genotype.getMajorityRatio();
                            break;
                        default:
                            throw new RuntimeException(
                                    "Unhandled MdrByGenotypeVotingMethod: "
                                    + mdrByGenotypeVotingMethod);
                        }
                    statusVotes[predictedStatus] += voteWeight;
                    for (final int attributeIndex : genotype.attributeCombination
                            .getAttributeIndices())
                        {
                        final String attributeName = dataset.labels.get(attributeIndex);
                        // final String attributeName = genotype.attributeCombination
                        // .getComboString();
                        MutableFloat mutableFloat = attributeCountMap.get(attributeName);
                        if (mutableFloat == null)
                            {
                            mutableFloat = new MutableFloat(0.0f);
                            attributeCountMap.put(attributeName, mutableFloat);
                            }
                        if (true)
                            {
                            mutableFloat.add((predictedStatus == status) ? voteWeight
                                                                         : -voteWeight);
                            }
                        else
                            {
                            mutableFloat.add(voteWeight);
                            }
                        // mutableFloat.add(1);
                        } // end loop over attributes in current genotype
                    if (mdrByGenotypeVotingMethod.toString().startsWith(
                            "FIRST_MATCH_PREDICTS"))
                        {
                        break; // exit out of best genotypes loop
                        }
                    } // end if matches current best genotype
                } // end loop over best genotypes
            // debugging -- want to see failures
            // if (predictedStatus == Model.UNKNOWN_STATUS) {
            // for (final Genotype genotype : trainingDataset.mostSignificantGenotypes) {
            // final byte[] rowSlice = getRowSlice(rowIndex,
            // genotype.attributeCombination.getAttributeIndices());
            // if (Arrays.equals(rowSlice, genotype.rowSlice)) {
            // predictedStatus = genotype.predictedStatus;
            // genotype.predict(status, getRowWeight(rowIndex));
            // break;
            // }
            // System.out.println(genotype.attributeCombination + " row  rowSlice: "
            // + Arrays.toString(rowSlice) + " != genotype rowSlice: "
            // + Arrays.toString(genotype.rowSlice));
            // } // end loop over best genotypes
            // }
            if (predictedStatus != Model.UNKNOWN_STATUS)
                {
                // now compare votes and predict whichever has the most votes
                predictedStatus = 0;
                for (byte statusIndex = 1; statusIndex < statusVotes.length; ++statusIndex)
                    {
                    if (statusVotes[statusIndex] > statusVotes[predictedStatus])
                        {
                        predictedStatus = statusIndex;
                        }
                    }
                }
            confusionMatrix.add(status, predictedStatus,
                                dataset.getRowWeight(rowIndex));
            } // end loop over rows
        // int matchedGenotypeCount = 0;
        // for (final int genotypeMatchCount : genotypeMatchCounts) {
        // if (genotypeMatchCount > 0) {
        // ++matchedGenotypeCount;
        // }
        // }
        // System.out.println(mdrByGenotypeVotingMethod + " matchedGenotypeCount: "
        // + matchedGenotypeCount + " totalGenotypeMatches: "
        // + totalGenotypeMatches + " " + Arrays.toString(genotypeMatchCounts));
        }

    public static Pair<ConfusionMatrix, ConfusionMatrix> scoreWithMostSignificantGenotypes(
            final MdrByGenotypeVotingMethod mdrByGenotypeVotingMethod,
            final GenotypeQualityMetric genotypeQualityMetric,
            final GenotypeFilterType genotypeFilterType,
            final Dataset trainingDataset, final Dataset testingDataset,
            final Map<String, MutableFloat> trainingAttributeCountMap,
            final Map<String, MutableFloat> testingAttributeCountMap)
        {
        final int numAttributes = trainingDataset.getCols() - 1;
        final int statusColIndex = numAttributes;
        final List<Genotype> sortedGenotypes = new ArrayList<Genotype>(
                trainingDataset.mostSignificantGenotypes.size());
        final ConfusionMatrix trainingConfusionMatrix = new ConfusionMatrix(
                trainingDataset.getLevels().get(statusColIndex),
                trainingDataset.getAffectedStatus());
        final ConfusionMatrix testingConfusionMatrix = new ConfusionMatrix(
                testingDataset.getLevels().get(statusColIndex),
                testingDataset.getAffectedStatus());
        final Pair<ConfusionMatrix, ConfusionMatrix> result = new Pair<ConfusionMatrix, ConfusionMatrix>(
                trainingConfusionMatrix, testingConfusionMatrix);
        for (final Genotype genotype : trainingDataset.mostSignificantGenotypes)
            {
            if (genotypeFilterType.passFilter(genotype))
                {
                sortedGenotypes.add(genotype);
                genotype.resetConfusionMatrix();
                }
            } // end adding loop over cached genotypes
        // for speed priority list did not calculate Fisher's test but now
        // it is worthwhile to determine best genotypes to use for classifying
        Collections.sort(sortedGenotypes, new Comparator<Genotype>()
        {
        public int compare(final Genotype o1, final Genotype o2)
            {
            int compareResult = Double.compare(
                    o1.getRequestedSortValue(genotypeQualityMetric),
                    o2.getRequestedSortValue(genotypeQualityMetric));
            if (compareResult == 0)
                {
                compareResult = o1.compareTo(o2);
                }
            return compareResult;
            }
        });
        // do training data first. This modifies the genotype confusion matrices which
        // affect classification with method WEIGHTED_SUM_OF_ACCURACY_FOR_ALL_MATCHES
        Dataset.mdrByGenotypeClassifyDataset(trainingDataset,
                                             mdrByGenotypeVotingMethod, true /* updateGenotypesConfusionMatrices */,
                                             trainingConfusionMatrix, trainingAttributeCountMap, sortedGenotypes);
        Dataset.mdrByGenotypeClassifyDataset(testingDataset,
                                             mdrByGenotypeVotingMethod,
                                             false /* updateGenotypesConfusionMatrices */, testingConfusionMatrix,
                                             testingAttributeCountMap, sortedGenotypes);
        // re-sort this time by how many rows each genotype classified
        // for (int sortedGenotypesIndex = sortedGenotypes.size() - 1; sortedGenotypesIndex >= 0;
        // --sortedGenotypesIndex) {
        // if (sortedGenotypes.get(sortedGenotypesIndex).confusionMatrix == null) {
        // sortedGenotypes.remove(sortedGenotypesIndex);
        // }
        // } // end removing genotypes that were not used to classify.
        // if (sortedGenotypes.size() < 0) {
        // Collections.sort(sortedGenotypes, new Comparator<Genotype>() {
        // public int compare(final Genotype o1, final Genotype o2) {
        // int compareResult = -Float.compare(
        // o1.confusionMatrix.getTotalCount(),
        // o2.confusionMatrix.getTotalCount());
        // if (compareResult == 0) {
        // compareResult = o1.compareTo(o2);
        // }
        // return compareResult;
        // }
        // });
        // System.out.println(sortedGenotypes.toString());
        // } // end if printing out sortedGenotypes
        // final ScoringMethod preferredScoringMethod = ScoringMethod.BALANCED_ACCURACY_RYAN;
        // final String message = mdrByGenotypeVotingMethod
        // + " MDRByGenotype overall picked using " + sortedGenotypes.size()
        // + " genotypes out of "
        // + trainingDataset.mostSignificantGenotypes.size()
        // + " remembered. Overall confusion matrix: "
        // + ret.toString(preferredScoringMethod);
        // // System.out.println(message);
        // System.out.println("MDRByGenotype\t" + mdrByGenotypeVotingMethod + "\t"
        // + ret.getTotalCount() + "\t" + ret.getUnknownCount() + "\t"
        // + ret.getBalancedAccuracy() + "\t"
        // + ret.getScore(preferredScoringMethod) + "\t" + mdrModelString + "\t"
        // + mdrByGenotypeModelString + "\t"
        // + (mdrModelString.equals(mdrByGenotypeModelString) ? 1 : 0));
        return result;
        } // end scoreWithMostSignificantGenotypes

    public Dataset(final String pMissing, final boolean pPaired)
        {
        levels = Collections.emptyList();
        labels = new ArrayList<String>();
        labels.add("Class");
        rows = 0;
        cols = labels.size();
        missing = pMissing;
        paired = pPaired;
        }

    public Dataset(final String pMissing, final boolean pPaired,
                   final LineNumberReader lineNumberReader) throws IOException
        {
        this(pMissing, pPaired);
        read(lineNumberReader);
        }

    public Dataset(final String pMissing, final boolean pPaired,
                   final List<String> labels, final List<List<String>> levels,
                   final byte[][] data, final int rows, final byte affectedStatus,
                   final byte unaffectedStatus)
        {
        this(pMissing, pPaired);
        this.labels = labels;
        this.levels = levels;
        this.data = data;
        this.rows = rows; // rows is passed in when partitioning
        cols = labels.size();
        this.affectedStatus = affectedStatus;
        this.unaffectedStatus = unaffectedStatus;
        calculateStatusCounts();
        }

    public Dataset(final String pMissing, final boolean pPaired,
                   final List<String> labels, final List<List<String>> levels,
                   final List<Pair<byte[], Float>> weightedData, final int rows,
                   final byte affectedStatus, final byte unaffectedStatus)
        {
        this(pMissing, pPaired);
        this.labels = labels;
        this.levels = levels;
        data = new byte[rows][];
        final boolean useWeights = (rows > 0)
                                   && (weightedData.get(0).getSecond() != null);
        if (useWeights)
            {
            weights = new float[rows];
            }
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex)
            {
            final Pair<byte[], Float> pair = weightedData.get(rowIndex);
            data[rowIndex] = pair.getFirst();
            if (useWeights)
                {
                weights[rowIndex] = pair.getSecond();
                }
            } // end storing data into rows
        this.rows = rows; // rows is passed in when partitioning
        cols = labels.size();
        this.affectedStatus = affectedStatus;
        this.unaffectedStatus = unaffectedStatus;
        calculateStatusCounts();
        }

    /**
     * Adjust for a single attribute. Method: For each allele of an attribute, use oversampling to force the ratio of
     * case/control to match
     * the global ratio of case/control
     *
     * @param covariateAttributeName
     * @return
     * @throws Exception
     */
    public Dataset adjustForCovariate(final long randomSeed,
                                      final String covariateAttributeName) throws Exception
        {
        final Random rnd = new Random(randomSeed);
        int covariateAttributeIndex = getLabels().indexOf(covariateAttributeName);
        if (covariateAttributeIndex == -1)
            {
            covariateAttributeIndex = getLabels().indexOf(
                    covariateAttributeName.toUpperCase());
            if (covariateAttributeIndex == -1)
                {
                throw new Exception("Attribute name '" + covariateAttributeName
                                    + "' does not exist in the dataset.");
                }
            }
        final float datasetOverallRatio = getRatio();
        final int statusColumnIndex = labels.size() - 1;
        final byte numAttributeLevels = (byte) levels.get(covariateAttributeIndex)
                .size();
        final ArrayList<byte[]> rowsToAdd = new ArrayList<byte[]>();
        // because logic is complicated deal with only one genotype at a time -- efficiency not critical here
        for (int attributeLevelIndex = 0; attributeLevelIndex < numAttributeLevels; ++attributeLevelIndex)
            {
            final ArrayList<byte[]> affectedRows = new ArrayList<byte[]>();
            final ArrayList<byte[]> unaffectedRows = new ArrayList<byte[]>();
            for (final byte[] row : data)
                {
                final byte attributeLevelIndexForRow = row[covariateAttributeIndex];
                if (attributeLevelIndexForRow == attributeLevelIndex)
                    {
                    final byte statusLevelIndex = row[statusColumnIndex];
                    if (statusLevelIndex == affectedStatus)
                        {
                        affectedRows.add(row);
                        }
                    else
                        {
                        unaffectedRows.add(row);
                        }
                    } // if the row has the current allele we are considering
                } // end row
            if ((affectedRows.size() == 0) || (unaffectedRows.size() == 0))
                {
                throw new Exception(
                        "Covariate adjustment cannot be made because all rows with attribute value '"
                        + levels.get(covariateAttributeIndex).get(attributeLevelIndex)
                        + "' are of the same class.");
                }
            final int numAffected = affectedRows.size();
            final int numUnaffected = unaffectedRows.size();
            final float affectedUnaffectedRatioForGenotype = (float) numAffected
                                                             / numUnaffected;
            float numToOversample;
            ArrayList<byte[]> arrayToOversampleFrom;
            if (affectedUnaffectedRatioForGenotype > datasetOverallRatio)
                {
                // going to oversample unaffected rows
                // affected / (unaffected + oversampleAmount) = datasetOverallRatio
                // solving for oversampleAmount:
                // oversampleAmount = affected/datasetOverallRatio - unaffected
                arrayToOversampleFrom = unaffectedRows;
                numToOversample = (numAffected / datasetOverallRatio) - numUnaffected;
                }
            else
                {
                // need to oversample affected rows
                // (affected + oversampleAmount)/unaffected = datasetOverallRatio
                // solving for oversampleAmount:
                // oversampleAmount = unaffected*datasetOverallRatio - affected
                arrayToOversampleFrom = affectedRows;
                numToOversample = (numUnaffected * datasetOverallRatio) - numAffected;
                }
            for (int overSampleIndex = 0; overSampleIndex < numToOversample; ++overSampleIndex)
                {
                final int randomIndex = rnd.nextInt(arrayToOversampleFrom.size());
                rowsToAdd.add(arrayToOversampleFrom.get(randomIndex));
                }
            } // end attributeLevelIndex (genotype)
        // create a new dataset
        final byte[][] newData = new byte[rows + rowsToAdd.size()][];
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex)
            {
            newData[rowIndex] = data[rowIndex];
            }
        for (int newRowIndex = 0; newRowIndex < rowsToAdd.size(); ++newRowIndex)
            {
            newData[rows + newRowIndex] = rowsToAdd.get(newRowIndex);
            }
        final Dataset adjustedDataset = new Dataset(getMissing(), isPaired(),
                                                    getLabels(), getLevels(), newData, newData.length,
                                                    getAffectedStatus(),
                                                    getUnaffectedStatus());
        return adjustedDataset;
        } // end adjustForCovariate

    // public float[][] calculateAttributeStats(
    // final PriorityList<Pair<Float, String>> topInterestingGenotypeCombinations) {
    // final int numAttributes = labels.size() - 1;
    // final int statusColumnIndex = labels.size() - 1;
    // final int numStatuses = levels.get(statusColumnIndex).size();
    // // order is attribute1, attribute2,attribute1LevelIndex, attribute2levelIndex, statusIndex
    // final int[][][][][] attributesCoOccurrences = new int[numAttributes - 1][][][][];
    // final float[] maxAttributeDifferences = new float[numAttributes];
    // final float[] totalAlleleFrequencyDifferences = new float[numAttributes];
    // final float[] maxAlleleFrequencyDifferences = new float[numAttributes];
    // // final float[] totalAttributeDifferences = new float[numAttributes];
    // final float[][] attributeStatsResults = new float[][] {
    // maxAttributeDifferences, totalAlleleFrequencyDifferences,
    // maxAlleleFrequencyDifferences };
    // // Create list status counts for each attributes different levels
    // // This could be done down below but requires a lot of conditional logic so I thought it cleaner to split it out
    // // order is attribute index, level/value index, status index
    // final int[][][] attributesAlleleCountsByStatus = new int[numAttributes][][];
    // for (int attributeIndex = 0; attributeIndex < numAttributes; ++attributeIndex) {
    // final byte numAttributeLevels = (byte) levels.get(attributeIndex).size();
    // final int[][] attributeAlleleCountsByStatus = new int[numAttributeLevels][numStatuses];
    // attributesAlleleCountsByStatus[attributeIndex] = attributeAlleleCountsByStatus;
    // for (final byte[] row : data) {
    // final byte statusLevelIndex = row[statusColumnIndex];
    // ++attributeAlleleCountsByStatus[row[attributeIndex]][statusLevelIndex];
    // } // end row
    // } // end attribute
    // for (int attribute1Index = 0; attribute1Index < attributesCoOccurrences.length; ++attribute1Index) {
    // final byte numAttribute1Levels = (byte) levels.get(attribute1Index)
    // .size();
    // final String attribute1Name = labels.get(attribute1Index);
    // final List<String> attribute1Values = levels.get(attribute1Index);
    // final int[][][][] attribute1CoOccurrences = new int[numAttributes
    // - (attribute1Index + 1)][numAttribute1Levels][][];
    // attributesCoOccurrences[attribute1Index] = attribute1CoOccurrences;
    // for (int coOccurrenceIndex = 0; coOccurrenceIndex < attribute1CoOccurrences.length; ++coOccurrenceIndex) {
    // final int attribute2Index = attribute1Index + 1 + coOccurrenceIndex;
    // final String attribute2Name = labels.get(attribute2Index);
    // final List<String> attribute2Values = levels.get(attribute2Index);
    // final byte numAttribute2Levels = (byte) levels.get(attribute2Index)
    // .size();
    // final int[][][] attribute1Attribute2CoOccurrences = new
    // int[numAttribute1Levels][numAttribute2Levels][numStatuses];
    // attribute1CoOccurrences[coOccurrenceIndex] = attribute1Attribute2CoOccurrences;
    // for (final byte[] row : data) {
    // final byte attribute1LevelIndex = row[attribute1Index];
    // final byte attribute2LevelIndex = row[attribute2Index];
    // final byte statusLevelIndex = row[statusColumnIndex];
    // ++attribute1Attribute2CoOccurrences[attribute1LevelIndex][attribute2LevelIndex][statusLevelIndex];
    // } // end row
    // // finished calculating all interactions between these two attributes so now examine in detail
    // for (byte attribute1LevelIndex = 0; attribute1LevelIndex < numAttribute1Levels; ++attribute1LevelIndex) {
    // final String attribute1ValueName = attribute1Values
    // .get(attribute1LevelIndex);
    // for (byte attribute2LevelIndex = 0; attribute2LevelIndex < numAttribute2Levels; ++attribute2LevelIndex) {
    // final String attribute2ValueName = attribute2Values
    // .get(attribute2LevelIndex);
    // for (int statusLevelIndex = 0; statusLevelIndex < numStatuses; ++statusLevelIndex) {
    // final int attribute1StatusCount = attributesAlleleCountsByStatus[attribute1Index][attribute1LevelIndex
    // ][statusLevelIndex];
    // final int attribute2StatusCount = attributesAlleleCountsByStatus[attribute2Index][attribute2LevelIndex
    // ][statusLevelIndex];
    // final float predictedCount = (attribute1StatusCount * attribute2StatusCount)
    // / (float) statusCounts[statusLevelIndex];
    // final int actualCount = attribute1Attribute2CoOccurrences[attribute1LevelIndex][attribute2LevelIndex
    // ][statusLevelIndex];
    // final float difference = Math.abs(predictedCount - actualCount);
    // maxAttributeDifferences[attribute1Index] = Math.max(
    // maxAttributeDifferences[attribute1Index], difference);
    // maxAttributeDifferences[attribute2Index] = Math.max(
    // maxAttributeDifferences[attribute2Index], difference);
    // final String identifier = "status: "
    // + levels.get(statusColumnIndex).get(statusLevelIndex) + " "
    // + attribute1Name + "-" + attribute2Name + " ["
    // + attribute1ValueName + "," + attribute2ValueName
    // + "] : actual: " + actualCount + "  predicted: "
    // + predictedCount + " (" + attribute1StatusCount + ","
    // + attribute2StatusCount + ")";
    // if (false && attribute1Name.equals("X0")
    // && attribute2Name.equals("X1")) {
    // System.out.println(difference + ": " + identifier);
    // }
    // topInterestingGenotypeCombinations.add(new Pair<Float, String>(
    // difference, identifier));
    // }
    // } // for attribute2LevelIndex
    // } // for attribute1LevelIndex
    // } // end coOccurrence
    // } // end attribute1
    // return attributeStatsResults;
    // }// calculateAttributeStats()
    private void calculateStatusCounts()
        {
        final int statusColumnIndex = labels.size() - 1;
        final List<String> statuses = levels.get(statusColumnIndex);
        statusCounts = new int[statuses.size()];
        pairedEnabled = true;
        int lastStatusLevelIndex = -1;
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex)
            {
            final byte[] row = data[rowIndex];
            final byte statusLevelIndex = row[statusColumnIndex];
            ++statusCounts[statusLevelIndex];
            if (pairedEnabled)
                {
                pairedEnabled &= lastStatusLevelIndex != statusLevelIndex;
                }
            lastStatusLevelIndex = statusLevelIndex;
            } // end for all rows
        if (paired && !pairedEnabled)
            {
            System.err
                    .println("WARNING: A paired analysis was requested but the dataset does not alternate " +
                             "status/class in the the final column so a normal analysis will be performed instead.");
            paired = false;
            }
        } // end calculateStatusCounts

    public boolean canBePaired()
        {
        return pairedEnabled;
        }

    public PermutationSupport createPermutationSupport(
            final boolean useExplicitTestOfInteraction)
        {
        return new PermutationSupport(useExplicitTestOfInteraction);
        }

    public Dataset filter(final int[] attributes)
        {
        final int numColumns = attributes.length + 1; // add a status column
        final byte[][] newData = new byte[rows][attributes.length + 1];
        final List<String> newLabels = new ArrayList<String>(numColumns);
        final List<List<String>> newLevels = new ArrayList<List<String>>(numColumns);
        for (final int i : attributes)
            {
            newLabels.add(labels.get(i));
            newLevels.add(levels.get(i));
            }
        final int statusColIndex = cols - 1;
        newLabels.add(labels.get(statusColIndex));
        newLevels.add(levels.get(statusColIndex));
        for (int r = 0; r < rows; ++r)
            {
            for (int c = 0; c < attributes.length; ++c)
                {
                newData[r][c] = data[r][attributes[c]];
                }
            newData[r][attributes.length] = data[r][statusColIndex];
            }
        return new Dataset(missing, paired, newLabels, newLevels, newData, rows,
                           affectedStatus, unaffectedStatus);
        }

    public byte getAffectedStatus()
        {
        return affectedStatus;
        }

    public int getAffectedStatusCount()
        {
        return statusCounts[affectedStatus];
        }

    public int getCols()
        {
        return cols;
        }

    public byte[] getColumn(final int col)
        {
        final byte[] ret = new byte[getRows()];
        for (int i = 0; i < getRows(); ++i)
            {
            ret[i] = data[i][col];
            }
        return ret;
        }

    public String getDatum(final int row, final int col)
        {
        String returnValue;
        final byte levelIndex = getRawDatum(row, col);
        returnValue = getLevels().get(col).get(levelIndex);
        return returnValue;
        }

    public List<String> getLabels()
        {
        return labels;
        }

    public List<List<String>> getLevels()
        {
        return levels;
        }

    /**
     * Return the index to the class which has the most instances or the affected class if they are equal
     *
     * @return
     */
    public byte getMajorityStatus()
        {
        byte majorityStatus = getAffectedStatus();
        if (statusCounts[getAffectedStatus()] < statusCounts[getUnaffectedStatus()])
            {
            majorityStatus = getUnaffectedStatus();
            }
        return majorityStatus;
        }

    public int getMajorityStatusCount()
        {
        final byte majorityStatus = getMajorityStatus();
        return statusCounts[majorityStatus];
        }

    /**
     * Return the index to the class which has the least instances or the unaffected class if they are equal
     *
     * @return
     */
    public byte getMinorityStatus()
        {
        byte minorityStatus = getUnaffectedStatus();
        if (statusCounts[affectedStatus] < statusCounts[unaffectedStatus])
            {
            minorityStatus = getAffectedStatus();
            }
        return minorityStatus;
        }

    public int getMinorityStatusCount()
        {
        final byte minorityStatus = getMinorityStatus();
        return statusCounts[minorityStatus];
        }

    public String getMissing()
        {
        return missing;
        }

    public byte getNumStatuses()
        {
        final int statusCol = getCols() - 1;
        final byte numStatuses = (byte) getLevels().get(statusCol).size();
        return numStatuses;
        }

    // TODO: when polytomy happens, this will go away

    /**
     * ratio of affected to unaffected
     */
    public float getRatio()
        {
        return (float) statusCounts[getAffectedStatus()]
               / (float) statusCounts[getUnaffectedStatus()];
        }

    public byte getRawDatum(final int rowIndex, final int columnIndex)
        {
        if ((rowIndex > rows) || (columnIndex > cols))
            {
            throw new IndexOutOfBoundsException();
            }
        return data[rowIndex][columnIndex];
        }

    public byte[] getRawRowData(final int rowIndex)
        {
        return data[rowIndex];
        }

    public int getRows()
        {
        return rows;
        }

    public byte[] getRowSlice(final int row, final int[] attributeIndices)
        {
        final byte[] ret = new byte[attributeIndices.length];
        for (int comboIndex = 0; comboIndex < attributeIndices.length; ++comboIndex)
            {
            final int attributeIndex = attributeIndices[comboIndex];
            final byte levelIndex = data[row][attributeIndex];
            ret[comboIndex] = levelIndex;
            }
        return ret;
        }

    public float getRowWeight(final int rowIndex)
        {
        float weight;
        if (weights == null)
            {
            weight = 1.0f;
            }
        else
            {
            weight = weights[rowIndex];
            }
        return weight;
        }

    public int getStatusColIndex()
        {
        return cols - 1;
        }

    public int[] getStatusCounts()
        {
        return statusCounts;
        }

    public byte getStatusLevelIndex(final int rowIndex)
        {
        final byte[] rawRowData = getRawRowData(rowIndex);
        return rawRowData[rawRowData.length - 1];
        }

    public byte getUnaffectedStatus()
        {
        return unaffectedStatus;
        }

    public int getUnaffectedStatusCount()
        {
        return statusCounts[unaffectedStatus];
        }

    public void insertColumn(final int col, final String name,
                             final List<String> values)
        {
        final SortedSet<String> uniqueLevels = new TreeSet<String>(values);
        final List<String> level = new ArrayList<String>(uniqueLevels);
        final List<Byte> byteValues = new ArrayList<Byte>(values.size());
        for (final String value : values)
            {
            byteValues.add((byte) level.indexOf(value));
            }
        insertColumn(col, name, level, byteValues);
        }

    public void insertColumn(final int col, final String name,
                             final List<String> level, final List<Byte> values)
        {
        assert ((col >= 0) && (col <= cols));
        final List<String> newLabels = new ArrayList<String>(labels.size() + 1);
        final List<List<String>> newLevels = new ArrayList<List<String>>(
                levels.size() + 1);
        for (int i = 0, j = 0; i <= cols; ++i)
            {
            if (i == col)
                {
                newLabels.add(name);
                newLevels.add(level);
                continue;
                }
            newLabels.add(labels.get(j));
            newLevels.add(levels.get(j));
            ++j;
            }
        final byte[][] newData = new byte[rows][cols + 1];
        for (int r = 0; r < rows; ++r)
            {
            final byte value = values.get(r).byteValue();
            for (int c = 0; c < cols + 1; ++c)
                {
                if (c < col)
                    {
                    newData[r][c] = data[r][c];
                    }
                else if (c > col)
                    {
                    newData[r][c] = data[r][c - 1];
                    }
                else
                    {
                    newData[r][c] = value;
                    }
                }
            }
        labels = newLabels;
        levels = newLevels;
        data = newData;
        cols = cols + 1;
        }

    public boolean isBalanced()
        {
        return Float.compare(getRatio(), 1.0f) == 0;
        }

    public boolean isEmpty()
        {
        return (rows == 0) || (cols == 0);
        }

    public boolean isPaired()
        {
        return paired;
        }

    private List<List<Pair<byte[], Float>>> pairedPartition(final int intervals,
                                                            final Random rnd)
        {
        final List<Pair<Pair<byte[], Float>, Pair<byte[], Float>>> pairs = new ArrayList<Pair<Pair<byte[], Float>,
                Pair<byte[], Float>>>();
        for (int i = 0; i < rows - 1; i += 2)
            {
            pairs
                    .add(new Pair<Pair<byte[], Float>, Pair<byte[], Float>>(
                            new Pair<byte[], Float>(data[i], weights == null ? null
                                                                             : weights[i]), new Pair<byte[],
                            Float>(data[i + 1],

                                   weights == null ? null : weights[i + 1])));
            }
        final List<List<Pair<byte[], Float>>> partitions = new ArrayList<List<Pair<byte[], Float>>>(
                intervals);
        if (rnd != null)
            {
            Collections.shuffle(pairs, rnd);
            }
        for (int i = 0; i < intervals; ++i)
            {
            partitions.add(new ArrayList<Pair<byte[], Float>>());
            }
        for (int i = 0; i < pairs.size(); ++i)
            {
            final List<Pair<byte[], Float>> partition = partitions.get(i % intervals);
            final Pair<Pair<byte[], Float>, Pair<byte[], Float>> pair = pairs.get(i);
            partition.add(pair.getFirst());
            partition.add(pair.getSecond());
            }
        return partitions;
        }

    public Pair<List<Dataset>, List<Dataset>> partition(final int intervals,
                                                        final Random rnd)
        {
        if (intervals < 2)
            {
            return null;
            }
        final List<List<Pair<byte[], Float>>> list = paired ? pairedPartition(
                intervals, rnd) : unpairedPartition(intervals, rnd);
        final Pair<List<Dataset>, List<Dataset>> partitions = new Pair<List<Dataset>, List<Dataset>>(
                new ArrayList<Dataset>(intervals), new ArrayList<Dataset>(intervals));
        for (int i = 0; i < intervals; ++i)
            {
            final List<Pair<byte[], Float>> testingPartition = list.get(i);
            final Dataset testing = new Dataset(missing, paired, labels, levels,
                                                testingPartition, testingPartition.size(), affectedStatus,
                                                unaffectedStatus);
            // for each training set, add all of the rows from the testing sets
            // from all other intervals
            final List<Pair<byte[], Float>> traingingData = new ArrayList<Pair<byte[], Float>>(
                    rows - testingPartition.size());
            for (int j = 0; j < intervals; ++j)
                {
                if (i == j)
                    {
                    continue;
                    }
                final List<Pair<byte[], Float>> partition = list.get(j);
                traingingData.addAll(partition);
                }
            final Dataset training = new Dataset(missing, paired, labels, levels,
                                                 traingingData, traingingData.size(), affectedStatus, unaffectedStatus);
            partitions.getFirst().add(training);
            partitions.getSecond().add(testing);
            }
        return partitions;
        }

    public void read(final LineNumberReader lnr) throws IOException
        {
        read(lnr, (Pattern) null, (SwingInvoker) null);
        }

    public String read(final LineNumberReader lnr, final Pattern endPattern,
                       final SwingInvoker progressUpdater) throws IOException
        {
        String line;
        final ArrayList<byte[]> rawData = new ArrayList<byte[]>();
        final ArrayList<Float> rawWeights = new ArrayList<Float>();
        line = lnr.readLine();
        if (line == null)
            {
            throw new IOException("Empty file!");
            }
        String[] fields = Dataset.patDelim.split(line, 4);
        if (fields.length < 3)
            {
            throw new IOException(
                    "Too few rows: The first row is expected to contain at least two attibutes and the class column " +
                    "so there must be at least three columns. '"
                    + line
                    + "' only contains "
                    + fields.length
                    + " recognizable columns: " + Arrays.toString(fields));
            }
        // find the delimiters used between first two columns
        final int endIndexOfFirstField = fields[0].length();
        final int startIndexOfSecondField = line.indexOf(fields[1], endIndexOfFirstField);
        final String delimiter = line.substring(endIndexOfFirstField, startIndexOfSecondField);
        // split the line from scratch using the delimiter since we expect all fields to be delimited in the same way
        fields = line.split(delimiter);
        Set<String> labelsSet = new HashSet<String>();
        final int statusColumnIndex = fields.length - 1;
        labels = new ArrayList<String>(fields.length);
        levels = new ArrayList<List<String>>(fields.length - 1);
        final List<String> templateLevelList = new ArrayList<String>(3);
        for (int i = 0; i < fields.length; ++i)
            {
            final String label = fields[i];
            if ((i != statusColumnIndex) && label.contains(" "))
                {
                throw new IOException("Attribute name '" + label + "' in column "
                                      + (i + 1) + " contains a space which is not allowed.");
                }
            if (label.length() == 0)
                {
                throw new IOException(
                        "There is an extra delimiter just after attribute name '"
                        + fields[i - 1] + "' in column " + (i + 1) + ".");
                }
            if (labelsSet.contains(label))
                {
                throw new IOException("Duplicate label '" + fields[i] + "' at columns "
                                      + (i + 1) + " and " + (labels.indexOf(fields[i]) + 1) + ".");
                }
            labels.add(label);
            labelsSet.add(label);
            levels.add(templateLevelList);
            }
        labelsSet.clear();
        labelsSet = null;
        if (labels.isEmpty())
            {
            throw new IOException("No columns in data set!");
            }
        while (((line = lnr.readLine()) != null) && ((endPattern == null) || !endPattern.matcher(line).matches()))
            {
            fields = line.split(delimiter);
            if (fields.length != labels.size())
                {
                throw new IOException("Line " + lnr.getLineNumber() + ": Expected "
                                      + labels.size() + " columns, found " + fields.length + ".");
                }
            final byte[] levelIndices = new byte[labels.size()];
            rawData.add(levelIndices);
            for (int i = 0; i < fields.length; ++i)
                {
                String cellValue = fields[i].trim();
                if ((i == statusColumnIndex) && (cellValue.length() == 0))
                    {
                    throw new IOException("Line " + lnr.getLineNumber()
                                          + ": The last column, which represents class, cannot be empty.");
                    }
                if (i == statusColumnIndex)
                    {
                    if (Main.isExperimental)
                        {
                        final String[] statusAndWeight = cellValue.split(":");
                        if (statusAndWeight.length == 2)
                            {
                            rawWeights.add(Float.valueOf(statusAndWeight[1]));
                            if (rawWeights.size() != rawData.size())
                                {
                                throw new RuntimeException(
                                        "Line "
                                        + lnr.getLineNumber()
                                        + ": The status value for row contained a weight but some previous rows did " +
                                        "not. Status column value: "
                                        + cellValue);
                                }
                            cellValue = statusAndWeight[0];
                            }
                        } // end if experimental
                    }
                else
                    {
                    // support for missing data is still experimental because of the
                    // issue about how fitness should be adjusted for unclassifiable data
                    if (Main.isExperimental && cellValue.equals(missing))
                        {
                        // kludgy optimization -- use the actual string object of missing so
                        // that we can use object comparison == instead of call to String.equal method
                        cellValue = missing;
                        }
                    }
                final List<String> discreteValues = levels.get(i);
                byte levelIndex = (byte) discreteValues.indexOf(cellValue);
                if (levelIndex < 0)
                    {
                    List<String> newDiscreteValues = new ArrayList<String>(discreteValues);
                    newDiscreteValues.add(cellValue);
                    levelIndex = (byte) (newDiscreteValues.size() - 1);
                    if (levelIndex == (Model.UNKNOWN_STATUS))
                        {
                        throw new IOException("Line " + lnr.getLineNumber() + ": Column "
                                              + (i + 1) + " '" + labels.get(i) + "' has more than "
                                              + (Model.UNKNOWN_STATUS - 1)
                                              + " different values! The list of different values is: "
                                              + newDiscreteValues.toString());
                        }
                    if (i == statusColumnIndex)
                        {
                        if (newDiscreteValues.size() > 2)
                            {
                            throw new IOException(
                                    "Line "
                                    + lnr.getLineNumber()
                                    + ":Only two classes are currently supported in the last column. As of this line " +
                                    "number the following were found: "
                                    + newDiscreteValues.toString());
                            }
                        }
                    final int sameLevelsListIndex = levels.indexOf(newDiscreteValues);
                    if (sameLevelsListIndex != -1)
                        {
                        // if a list with these values already exists, use it instead of newly created one
                        newDiscreteValues = levels.get(sameLevelsListIndex);
                        }
                    levels.set(i, newDiscreteValues);
                    }
                levelIndices[i] = levelIndex;
                } // for fields[] loop
            if (progressUpdater != null)
                {
                progressUpdater.run();
                }
            } // end while lines
        // now actually store data as indices into levels for each attribute column
        data = new byte[rawData.size()][];
        cols = labels.size();
        rows = data.length;
        for (int rowIndex = 0; rowIndex < data.length; ++rowIndex)
            {
            data[rowIndex] = rawData.get(rowIndex);
            if (progressUpdater != null)
                {
                progressUpdater.run();
                }
            } // end for rowIndex
        if (rawWeights.size() == rows)
            {
            weights = new float[rows];
            for (int rowIndex = 0; rowIndex < data.length; ++rowIndex)
                {
                weights[rowIndex] = rawWeights.get(rowIndex);
                } // end for rowIndex
            }
        final List<String> statuses = levels.get(statusColumnIndex);
        if (statuses.size() != 2)
            {
            throw new IOException(
                    "After having completely read file, there should be exactly two different values for the last " +
                    "(class/status) column '"
                    + labels.get(statusColumnIndex)
                    + "'. The values found were: "
                    + statuses.toString());
            }
        affectedStatus = (byte) statuses.indexOf("1");
        unaffectedStatus = (byte) statuses.indexOf("0");
        if ((affectedStatus < 0) || (unaffectedStatus < 0))
            {
            // if the class variables are something other than 0 or 1 then the first one alphabetically is affected
            // and the second unaffected.
            // this is arbitrary but works with the common pairs 'case/control' and 'affected/unaffected'
            final int comparisonResult = statuses.get(0).compareTo(statuses.get(1));
            if (comparisonResult < 1)
                {
                affectedStatus = 0;
                unaffectedStatus = 1;
                }
            else
                {
                affectedStatus = 1;
                unaffectedStatus = 0;
                }
            }
        calculateStatusCounts();
        return line; // return the last line
        }

    public void read(final LineNumberReader r, final SwingInvoker progressUpdater)
            throws IOException
        {
        read(r, (Pattern) null, progressUpdater);
        }

    public void removeColumn(final int col)
        {
        assert ((col >= 0) && (col < cols - 1));
        final List<String> newLabels = new ArrayList<String>(labels.size() - 1);
        final List<List<String>> newLevels = new ArrayList<List<String>>(
                levels.size() - 1);
        for (int i = 0; i < cols; ++i)
            {
            if (i != col)
                {
                newLabels.add(labels.get(i));
                newLevels.add(levels.get(i));
                }
            }
        final byte[][] newData = new byte[rows][cols - 1];
        for (int r = 0; r < rows; ++r)
            {
            for (int c = 0; c < cols - 1; ++c)
                {
                newData[r][c] = data[r][c < col ? c : c + 1];
                }
            }
        labels = newLabels;
        levels = newLevels;
        data = newData;
        cols = cols - 1;
        }

    public void setPaired(final boolean paired)
        {
        this.paired = paired;
        }

    private List<List<Pair<byte[], Float>>> unpairedPartition(
            final int intervals, final Random rnd)
        {
        final int nstatus = getNumStatuses();
        final List<List<Pair<byte[], Float>>> byClass = new ArrayList<List<Pair<byte[], Float>>>(
                nstatus);
        for (int i = 0; i < nstatus; ++i)
            {
            byClass.add(new ArrayList<Pair<byte[], Float>>());
            }
        for (int rowIndex = 0; rowIndex < data.length; ++rowIndex)
            {
            final byte[] inst = data[rowIndex];
            Float weight = null;
            if (weights != null)
                {
                weight = weights[rowIndex];
                }
            final byte status = inst[inst.length - 1];
            final List<Pair<byte[], Float>> list = byClass.get(status);
            list.add(new Pair<byte[], Float>(inst, weight));
            }
        for (final List<Pair<byte[], Float>> list : byClass)
            {
            Collections.shuffle(list, rnd);
            }
        final List<List<Pair<byte[], Float>>> partitions = new ArrayList<List<Pair<byte[], Float>>>(
                intervals);
        for (int i = 0; i < intervals; ++i)
            {
            partitions.add(new ArrayList<Pair<byte[], Float>>());
            }
        int total = 0;
        for (final List<Pair<byte[], Float>> list : byClass)
            {
            for (int i = 0; i < list.size(); ++i)
                {
                partitions.get((i + total) % intervals).add(list.get(i));
                }
            total += list.size();
            }
        return partitions;
        }

    public void write(final Writer w)
        {
        final PrintWriter bw = new PrintWriter(w);
        bw.println(Utility.join(labels, Dataset.sDelim));
        for (int r = 0; r < rows; ++r)
            {
            for (int c = 0; c < cols; ++c)
                {
                if (c != 0)
                    {
                    bw.print(Dataset.sDelim);
                    }
                bw.print(levels.get(c).get(
                        data[r][c] < 0 ? data[r][c] + 256 : data[r][c]));
                }
            bw.println();
            }
        bw.flush();
        }

    static class Genotype implements Comparable<Genotype>
        {
        final AttributeCombination attributeCombination;
        final byte[] rowSlice;
        final float numAffected;
        final float numUnaffected;
        private double fishersQuickAndDirty = Double.NaN;
        private double fishersExact = Double.NaN;
        private final Dataset data;
        private final byte predictedStatus;
        private ConfusionMatrix confusionMatrix = null;
        final static edu.northwestern.at.utils.math.matrix.Matrix matrix = MatrixFactory
                .createMatrix(2, 2);

        public Genotype(final Dataset pData,
                        final AttributeCombination pAttributeCombination,
                        final byte pPredictedStatus, final byte[] pRowSlice,
                        final float pNumAffected, final float pNumUnaffected)
            {
            data = pData;
            attributeCombination = pAttributeCombination;
            predictedStatus = pPredictedStatus;
            rowSlice = Arrays.copyOf(pRowSlice, pRowSlice.length);
            numAffected = pNumAffected;
            numUnaffected = pNumUnaffected;
            }

        public int compareTo(final Genotype o)
            {
            int compareResult = 0;
            if (!super.equals(o))
                {
                compareResult = Double
                        .compare(
                                getRequestedSortValue(GenotypeQualityMetric.MAJORITY_RATIO_TIMES_TOTAL),
                                o.getRequestedSortValue(GenotypeQualityMetric.MAJORITY_RATIO_TIMES_TOTAL));
                if (compareResult == 0)
                    {
                    // reverse sort since more samples is better
                    compareResult = -(data.getRows() - o.data.getRows());
                    if (compareResult == 0)
                        {
                        // reverse sort since more samples is better
                        // compareResult = -Float.compare(getTotal(), o.getTotal());
                        compareResult = -Float.compare(getDelta(), o.getDelta());
                        if (compareResult == 0)
                            {
                            compareResult = attributeCombination
                                    .compareTo(o.attributeCombination);
                            if (compareResult == 0)
                                {
                                compareResult = rowSlice.length - o.rowSlice.length;
                                if (compareResult == 0)
                                    {
                                    for (int index = 0; (index < rowSlice.length)
                                                        && (compareResult == 0); ++index)
                                        {
                                        compareResult = rowSlice[index] - o.rowSlice[index];
                                        }
                                    if (compareResult == 0)
                                        {
                                        // the genotypes are not the same object but describe the same thing so are
                                        // functionally equal
                                        // System.out
                                        // .println(Thread.currentThread().getName()
                                        // + ": Genotype compareTo hit two apparently equal genotypes:\n"
                                        // + toString() + o.toString());
                                        // compareResult = hashCode() - o.hashCode();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            return compareResult;
            } // end compareTo

        // do not override equals because not sure how to override hashcode in this case
        // I think it is allowable for results of compareTo == 0 to be the same as equals() but
        // it will make a sorted set act different than a regualr set since sortedSet uses compareTo and Set uses equals
        // @Override
        // public boolean equals(final Object aThat) {
        // boolean result = super.equals(aThat);
        // if (result) {
        // result = compareTo((Genotype) aThat) == 0;
        // }
        // return result;
        // }
        private float getDelta()
            {
            return Math.abs(numAffected - numUnaffected);
            }

        private double getFisherExact()
            {
            if (Double.isNaN(fishersExact))
                {
                Genotype.matrix.set(1, 1, data.getAffectedStatusCount() - numAffected);
                Genotype.matrix.set(1, 2, data.getUnaffectedStatusCount()
                                          - numUnaffected);
                Genotype.matrix.set(2, 1, numUnaffected);
                Genotype.matrix.set(2, 2, numAffected);
                // double vector with three entries. [0] = two-sided Fisher's exact test. [1] = left-tail Fisher's exact test. [2] =
                // right-tail
                // Fisher's exact test.
                final double[] fisherExactTestResults = ContingencyTable
                        .fishersExactTest(Genotype.matrix);
                fishersExact = fisherExactTestResults[0];
                }
            return fishersExact;
            }

        private double getFisherExactQuickAndDirty()
            {
            if (Double.isNaN(fishersQuickAndDirty))
                {
                Genotype.matrix.set(1, 1, numAffected);
                Genotype.matrix.set(1, 2, numUnaffected);
                Genotype.matrix.set(2, 1, numUnaffected);
                Genotype.matrix.set(2, 2, numAffected);
                // double vector with three entries. [0] = two-sided Fisher's exact test. [1] = left-tail Fisher's exact test. [2] =
                // right-tail
                // Fisher's exact test.
                final double[] fisherExactTestResults = ContingencyTable
                        .fishersExactTest(Genotype.matrix);
                fishersQuickAndDirty = fisherExactTestResults[0];
                }
            return fishersQuickAndDirty;
            }

        private double getHeritability()
            {
            final double total = (numAffected + numUnaffected);
            final double average = total / 2.0;
            final double differenceSquared = (float) Math.pow(numAffected - average,
                                                              2);
            return differenceSquared / total;
            }

        private float getMajorityRatio()
            {
            return Math.max(numAffected, numUnaffected) / getTotal();
            }

        private double getRequestedSortValue(
                final GenotypeQualityMetric genotypeQualityMetric)
            {
            double qualityMetric = Double.NaN;
            switch (genotypeQualityMetric)
                {
                case FISHERS_EXACT:
                    qualityMetric = getFisherExact();
                    break;
                case FISHERS_QUICK_AND_DIRTY:
                    qualityMetric = getFisherExactQuickAndDirty();
                    break;
                case HERITABILITY:
                    qualityMetric = getHeritability();
                    break;
                case MAJORITY_RATIO:
                    qualityMetric = -getMajorityRatio();
                    break;
                case MAJORITY_RATIO_TIMES_TOTAL:
                    qualityMetric = -getMajorityRatio() * getTotal();
                    break;
                case MAJORITY_RATIO_TIMES_DELTA:
                    qualityMetric = -getMajorityRatio() * getDelta();
                    break;
                case DELTA:
                    qualityMetric = -getDelta();
                    break;
                default:
                    throw new RuntimeException("Unhandled GenotypeQualityMetric: "
                                               + genotypeQualityMetric);
                }
            return qualityMetric;
            }

        private float getTotal()
            {
            return numAffected + numUnaffected;
            }

        public void predict(final byte status, final float rowWeight)
            {
            if (confusionMatrix == null)
                {
                confusionMatrix = new ConfusionMatrix(data.getLevels().get(
                        data.getCols() - 1), data.getAffectedStatus());
                }
            confusionMatrix.add(status, predictedStatus, rowWeight);
            }

        public void resetConfusionMatrix()
            {
            confusionMatrix = null;
            }

        @Override
        public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Fisher=" + Double.toString(fishersQuickAndDirty) + ' ');
        for (int comboIndex = 0; comboIndex < rowSlice.length; ++comboIndex)
            {
            final int attributeIndex = attributeCombination.getAttributeIndices()[comboIndex];
            if (comboIndex > 0)
                {
                sb.append(',');
                }
            final int levelIndex = rowSlice[comboIndex];
            sb.append(data.getLabels().get(attributeIndex) + "=");
            sb.append(data.getLevels().get(attributeIndex).get(levelIndex));
            }
        sb.append(" majorityRatio="
                  + Main.modelTextNumberFormat.format(getMajorityRatio()));
        sb.append(" delta=" + Main.modelTextNumberFormat.format(getDelta()));
        sb.append(" numAffected="
                  + Main.modelTextNumberFormat.format(numAffected));
        sb.append(" numUnaffected="
                  + Main.modelTextNumberFormat.format(numUnaffected));
        sb.append(" predicted status: " + predictedStatus);
        if (confusionMatrix != null)
            {
            sb.append(" Conf.: " + confusionMatrix.toString());
            }
        return "\n" + sb.toString();
        }

        /**
         * The value used to sort Genotype. Smaller is better since sort used for ranking is Ascending.
         *
         * @return
         */
        enum GenotypeQualityMetric
            {
                FISHERS_EXACT, FISHERS_QUICK_AND_DIRTY, MAJORITY_RATIO, MAJORITY_RATIO_TIMES_TOTAL, MAJORITY_RATIO_TIMES_DELTA, DELTA, HERITABILITY
            }
        } // end private class

    public static class GenotypeFilterType
        {
        public static GenotypeFilterType[] genotypeFilterTypes = new GenotypeFilterType[]{
                new GenotypeFilterType(null, Double.NaN),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_EXACT, 0.10),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_EXACT, 0.05),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_EXACT, 0.03),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_EXACT, 0.01),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_QUICK_AND_DIRTY,
                                       0.10),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_QUICK_AND_DIRTY,
                                       0.05),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_QUICK_AND_DIRTY,
                                       0.03),
                new GenotypeFilterType(GenotypeQualityMetric.FISHERS_QUICK_AND_DIRTY,
                                       0.01),};
        GenotypeQualityMetric genotypeQualityMetric;
        Double filterLevel;

        public GenotypeFilterType(
                final GenotypeQualityMetric pGenotypeQualityMetric,
                final Double pFilterLevel)
            {
            genotypeQualityMetric = pGenotypeQualityMetric;
            filterLevel = pFilterLevel;
            }

        public boolean passFilter(final Genotype genotype)
            {
            boolean result = false;
            if (genotypeQualityMetric == null)
                {
                result = true;
                }
            else
                {
                switch (genotypeQualityMetric)
                    {
                    case FISHERS_EXACT:
                        result = genotype.getFisherExact() <= filterLevel;
                        break;
                    case FISHERS_QUICK_AND_DIRTY:
                        result = genotype.getFisherExactQuickAndDirty() <= filterLevel;
                        break;
                    case DELTA:
                    case MAJORITY_RATIO:
                    case MAJORITY_RATIO_TIMES_DELTA:
                    case MAJORITY_RATIO_TIMES_TOTAL:
                    default:
                        throw new RuntimeException(
                                "GenotypeQualityMetric type not handled for passFilter: "
                                + genotypeQualityMetric);
                    }
                }
            return result;
            }

        @Override
        public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (genotypeQualityMetric == null)
            {
            sb.append("NONE");
            }
        else
            {
            sb.append(genotypeQualityMetric);
            sb.append("<=");
            sb.append(filterLevel);
            }
        return sb.toString();
        }
        } // end static class GenotypeFilterType

    enum MdrByGenotypeVotingMethod
        {
            FIRST_MATCH_PREDICTS, SUM_OF_ALL_MATCHES, WEIGHTED_SUM_FISHER_FOR_ALL_MATCHES, WEIGHTED_SUM_OF_ACCURACY_FOR_ALL_MATCHES, WEIGHTED_SUM_OF_MAJORITY_RATIO_FOR_ALL_MATCHES, FIRST_MATCH_PREDICTS_WEIGHT_INVERSE_FISHERS, FIRST_MATCH_PREDICTS_WEIGHT_ACCURACY, FIRST_MATCH_PREDICTS_WEIGHT_MAJORITY_RATIO
        }

    public static class MutableFloat
        {
        float myFloat;

        public MutableFloat(final float initialValue)
            {
            myFloat = initialValue;
            }

        public void add(final float deltaValue)
            {
            myFloat += deltaValue;
            }

        @Override
        public String toString() {
        return Float.toString(myFloat);
        }
        }

    public class PermutationSupport
        {
        private byte[][] affectedRows = null;
        private byte[][] unaffectedRows = null;
        private final boolean useExplicitTestOfInteraction;

        public PermutationSupport(final boolean useExplicitTestOfInteraction)
            {
            this.useExplicitTestOfInteraction = useExplicitTestOfInteraction;
            if (useExplicitTestOfInteraction)
                {
                affectedRows = new byte[statusCounts[affectedStatus]][];
                unaffectedRows = new byte[statusCounts[unaffectedStatus]][];
                final int statusColIndex = cols - 1;
                int affectedRowsAdded = 0;
                int unaffectedRowsAdded = 0;
                for (int rowIndex = 0; rowIndex < data.length; ++rowIndex)
                    {
                    final byte status = data[rowIndex][statusColIndex];
                    if (status == affectedStatus)
                        {
                        affectedRows[affectedRowsAdded++] = data[rowIndex];
                        }
                    else
                        {
                        unaffectedRows[unaffectedRowsAdded++] = data[rowIndex];
                        }
                    } // end row loop
                if (affectedRowsAdded != statusCounts[affectedStatus])
                    {
                    throw new RuntimeException(
                            "number of affectedRows did not match affected row count! "
                            + affectedRowsAdded + "!=" + statusCounts[affectedStatus]);
                    }
                if (unaffectedRowsAdded != statusCounts[unaffectedStatus])
                    {
                    throw new RuntimeException(
                            "number of unaffectedRows did not match affected row count! "
                            + unaffectedRowsAdded + "!=" + statusCounts[unaffectedStatus]);
                    }
                }
            } // end constructor

        public void permuteData(final Random rnd)
            {
            if (paired)
                {
                permutePairedData(rnd);
                }
            else
                {
                permuteUnpairedData(rnd);
                }
            } // end permuteStatuses

        /*
           * Code derived from Sun Collections.shuffle implementation This implementation traverses the list backwards, from the last element up
           * to the second, repeatedly swapping a randomly selected element into the "current position". Elements are randomly selected from the
           * portion of the list that runs from the first element to the current position, inclusive.<p>
           */
        private void permuteDataColumn(final Random rnd,
                                       final byte[][] dataToBePermuted, final int columnIndexToBePermuted)
            {
            for (int i = dataToBePermuted.length; i > 1; i--)
                {
                final int indexToSwap = rnd.nextInt(i);
                final byte tempLevelIndex = dataToBePermuted[indexToSwap][columnIndexToBePermuted];
                dataToBePermuted[indexToSwap][columnIndexToBePermuted] = dataToBePermuted[i - 1][columnIndexToBePermuted];
                dataToBePermuted[i - 1][columnIndexToBePermuted] = tempLevelIndex;
                } // end for
            } // end permuteStatuses

        /**
         * Shuffle the class values of all rows based on the original value.
         *
         * @param rnd Random number generator to use for the shuffle
         */
        private void permutePairedData(final Random rnd)
            {
            final int statusColIndex = cols - 1;
            for (int i = 0; i < rows; i += 2)
                {
                final byte[] row1 = data[i];
                final byte[] row2 = data[i + 1];
                if (useExplicitTestOfInteraction)
                    {
                    throw new IllegalArgumentException(
                            "Both paired and useExplicitTestOfInteraction are true which should be impossible");
                    }
                // else //got Warning Statement unnecessarily nested within else clause. The corresponding then clause does not complete normally
                { // not explicitTestOfInteraction
                // with 1/2 probability, swap the statuses for the paired rows
                if (rnd.nextInt(2) == 0)
                    {
                    final byte swapStatus = row1[statusColIndex];
                    row1[statusColIndex] = row2[statusColIndex];
                    row2[statusColIndex] = swapStatus;
                    } // end if shuffle statuses
                } // end if not explicitTestOfInteraction
                } // end rows
            } // end permutePairedData()

        private void permuteUnpairedData(final Random rnd)
            {
            final int statusColIndex = cols - 1;
            if (useExplicitTestOfInteraction)
                {
                // rather than permuting statuses across all data
                // permute the attributes within each class
                // This will preserve main affects while breaking epistatic affects.
                for (int colIndex = 0; colIndex < statusColIndex; ++colIndex)
                    {
                    permuteDataColumn(rnd, affectedRows, colIndex);
                    permuteDataColumn(rnd, unaffectedRows, colIndex);
                    } // end columns
                }
            else
                {
                permuteDataColumn(rnd, data, statusColIndex);
                }
            } // end permuteStatuses
        } // end local class
    } // end Dataset class
