package org.epistasis.combinatoric.mdr;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.epistasis.Pair;
import org.epistasis.RouletteWheel;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;

/**
 * A set of scores, one for each attribute of a data set, which can be used to influence the evolution of solutions.
 */
public class ExpertKnowledge extends ArrayList<ExpertKnowledge.Attribute>
    {
    private final static boolean DEBUG = false;
    private static final long serialVersionUID = 1L;
    /**
     * Delimiter used for parsing input data.
     */
    private static final Pattern delim = Pattern.compile("\\s+");
    private ScoresMetaInformation scoresMetaInformation;
    private final List<String> nonMatchingExpertKnowledgeAttributes = new ArrayList<String>();
    private final Map<Integer, ExpertKnowledge.Attribute> datasetIndexToExpertKnowledgeAttributeMap = new
            HashMap<Integer, Attribute>();

    public static double[] scaleAndWeight(final double[] inputWeights,
                                          final ScoresMetaInformation scoresMetaInformation,
                                          final WeightScheme weightScheme, final ScalingMethod scalingMethod,
                                          double scalingParameter)
        {
        final int size = inputWeights.length;
        final double originalScalingParameter = scalingParameter;
        final double[] outputWeights = new double[size];
        if (scalingMethod.equals(ScalingMethod.LINEAR))
            {
            // scaling parameter is not the actual value but a percentage of possible range of values
            scalingParameter = scoresMetaInformation.getLinearMaxMin()
                               + ((scoresMetaInformation.getMaxDefault(weightScheme) - scoresMetaInformation
                    .getLinearMaxMin()) * originalScalingParameter);
            }
        if (weightScheme != null)
            {
            // linear Maximum (M) = user defined
            final double lMax = scalingParameter;
            // linear Minimum (m)
            double lMin;
            // linear constant (k)
            double k;
            // minScore score
            double s1;
            // maxScore score
            double sn;
            switch (weightScheme)
                {
                case PROPORTIONAL:
                    s1 = scoresMetaInformation.getMinScore();
                    sn = scoresMetaInformation.getMaxScore();
                    k = scoresMetaInformation.getLinearMaxConstant();
                    lMin = (1.0 - (lMax * k)) / (size - k);
                    // no initial output is just the scores
                    for (int index = 0; index < size; ++index)
                        {
                        outputWeights[index] = inputWeights[index];
                        }
                    break;
                case RANK:
                    final List<Pair<Double, Integer>> tempList = new ArrayList<Pair<Double, Integer>>(
                            size);
                    for (int index = 0; index < size; ++index)
                        {
                        tempList.add(new Pair<Double, Integer>(inputWeights[index], index));
                        }
                    Collections.sort(tempList,
                                     new Pair.FirstComparator<Double, Integer>());
                    s1 = 1.0;
                    sn = size;
                    double rank = 0.0;
                    for (int index = 0; index < size; ++index)
                        {
                        final Pair<Double, Integer> pair = tempList.get(index);
                        final int attributeIndex = pair.getSecond();
                        outputWeights[attributeIndex] = ++rank;
                        }
                    k = size / 2.0;
                    lMin = (2.0 / size) - lMax;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "WeightScheme was unhandled value: " + weightScheme);
                    // break;
                }
            // now scale the weights in the pair list according to user choices
            switch (scalingMethod)
                {
                case LINEAR:
                    for (int index = 0; index < size; ++index)
                        {
                        // ith score
                        final double si = outputWeights[index];
                        // (((M-m)*(Si - S1))/(Sn - S1)) + m
                        outputWeights[index] = ((((lMax - lMin) * (si - s1)) / (sn - s1)) + lMin);
                        }
                    break;
                case EXPONENTIAL:
                    // Summation of f(Si)
                    double sumFsi = 0.0;
                    double newSumFsi = 0.0;
                    final double c = scalingParameter;
                    final double realTheta = Math.pow(c, ((size - 1) / (sn - s1)));
                    for (int index = 0; index < size; ++index)
                        {
                        // ith score
                        final double si = outputWeights[index];
                        // if proportional then Sn = MAX, s1 = MIN
                        // f(Si) = c^((n - 1)*((Sn - Si)/(Sn - S1)))
                        // this results in the scaled weight of the maxScore item == 1 and the scaled weight of the
                        // minScore is very close to 0
                        // for turf scores which range between 1 and -1 sn-si is roughly equivalent to adding one to
                        // all values (correct?)
                        final double scaledWeight = Math.pow(c,
                                                             ((size - 1) * ((sn - si) / (sn - s1))));
                        outputWeights[index] = scaledWeight;
                        sumFsi += scaledWeight;
                        if (ExpertKnowledge.DEBUG)
                            {
                            final double newSi = si + 1;
                            final double comparisonScaledWeight = Math.pow(realTheta, -newSi);
                            newSumFsi += comparisonScaledWeight;
                            }
                        } // end for index
                    // scale a second time to put values between o and 1
                    for (int index = 0; index < size; ++index)
                        {
                        final double scaledWeight = outputWeights[index] / sumFsi;
                        if (ExpertKnowledge.DEBUG)
                            {
                            System.out.println("index " + index + " c=" + scalingParameter
                                               + " score= " + inputWeights[index] + " first scaledWeight="
                                               + outputWeights[index] + "  normalized scaledWeight="
                                               + scaledWeight);
                            }
                        outputWeights[index] = scaledWeight;
                        }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "ScalingMethod was unhandled value: " + scalingMethod);
                    // break;
                }
            }
        return outputWeights;
        }

    public ExpertKnowledge(final FileReader fileReader, final List<String> labels)
            throws IOException
        {
        read(fileReader, labels);
        }

    public ExpertKnowledge(final List<String> labels)
        {
        // subtract 1 to skip last column which is status
        final int numAttributes = labels.size() - 1;
        final double weightValue = 1.0 / numAttributes;
        for (int index = 0; index < numAttributes; ++index)
            {
            this.add(new Attribute(index, labels.get(index), weightValue));
            }
        }

    public ScoresMetaInformation getLinearMinMaxDefault()
        {
        return scoresMetaInformation;
        }

    public double getMaxEKScore()
        {
        return scoresMetaInformation.getMaxScore();
        }

    public double getMinEKScore()
        {
        return scoresMetaInformation.getMinScore();
        }

    public List<String> getNonMatchingExpertKnowledgeAttributes()
        {
        return nonMatchingExpertKnowledgeAttributes;
        }

    public TableModel getTableModel()
        {
        return new ExpertKnowledgeTableModel();
        }

    /**
     * Read the expert knowledge file from a stream. It is read from two whitespace-separated columns. The first
     * column is the attribute name,
     * and the second is the expert knowledge score. A list of labels from the data set is provided,
     * and any attributes not in that list of
     * labels are ignored.
     *
     * @param r      reader from which to read
     * @param labels list of attribute labels for which to read scores
     */
    private void read(final Reader r, final List<String> labels)
            throws IOException
        {
        final LineNumberReader lnr = new LineNumberReader(r);
        String line;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        while ((line = lnr.readLine()) != null)
            {
            final String[] fields = ExpertKnowledge.delim.split(line);
            if (fields.length != 2)
                {
                throw new IOException("Expected 2 columns, found " + fields.length
                                      + " on line " + lnr.getLineNumber() + ".");
                }
            final String name = fields[0];
            final int index = labels.indexOf(name);
            double score = 0.0;
            if (index < 0)
                {
                nonMatchingExpertKnowledgeAttributes.add(name);
                continue;
                }
            try
                {
                score = Double.parseDouble(fields[1]);
                }
            catch (final NumberFormatException e)
                {
                throw new IOException("Column 2 value '" + fields[1]
                                      + "'not numeric on line " + lnr.getLineNumber() + ".");
                }
            sum += score;
            min = Math.min(min, score);
            max = Math.max(max, score);
            final Attribute newAttribute = new Attribute(index, name, score);
            datasetIndexToExpertKnowledgeAttributeMap.put(index, newAttribute);
            add(newAttribute);
            }
        if (nonMatchingExpertKnowledgeAttributes.size() > 0)
            {
            System.out
                    .println("WARNING: "
                             + nonMatchingExpertKnowledgeAttributes.size()
                             + " attribute names from the expert knowledge file could not be matched to data file " +
                             "attributes. Unmatched expert knowledge attributes: "
                             + nonMatchingExpertKnowledgeAttributes.toString());
            }
        if (size() != (labels.size() - 1))
            {
            System.out
                    .println("WARNING: "
                             + ((labels.size() - 1) - size())
                             + " data set attributes do not have an expert knowledge score provided");
            }
        scoresMetaInformation = new ScoresMetaInformation(min, max, sum, size());
        }

    /**
     * Write the expert knowledge file to a stream. It is written into two tab-separated columns. The first column is
     * the attribute name, and
     * the second is the expert knowledge score.
     *
     * @param w writer to which to write
     */
    public void write(final Writer w)
        {
        final PrintWriter p = new PrintWriter(w);
        for (final Attribute a : this)
            {
            p.print(a.getLabel());
            p.print('\t');
            p.println(a.getScore());
            }
        }

    /**
     * ExpertKnowledge's internal representation of a data attribute.
     */
    public static class Attribute implements Comparable<Attribute>
        {
        /**
         * Column index for this Attribute in the data set.
         */
        private final int index;
        /**
         * Name of this Attribute in the data set.
         */
        private final String label;
        /**
         * Expert knowledge for this Attribute.
         */
        private final double score;
        private double scaledAndWeightedScore;

        /**
         * Construct an Attribute. Class is immutable
         *
         * @param index numerical index of position in data set
         * @param name  attribute name
         * @param score expert knowledge score for this attribute
         */
        public Attribute(final int index, final String name, final double score)
            {
            this.index = index;
            label = name;
            this.score = score;
            }

        /**
         * Comparison operator to sort Attributes in ascending order by index.
         *
         * @param a other Attribute to compare
         * @return &lt; 0 if this &lt; a, &gt; 0 if this &gt; a, or 0 otherwise
         */
        public int compareTo(final Attribute a)
            {
            if (a == this)
                {
                return 0;
                }
            final int[] idx = {getIndex(), a.getIndex()};
            if (idx[0] > idx[1])
                {
                return -1;
                }
            if (idx[0] < idx[1])
                {
                return 1;
                }
            return 0;
            }

        /**
         * Get this Attribute's index.
         *
         * @return this Attribute's index
         */
        public int getIndex()
            {
            return index;
            }

        /**
         * Get this Attribute's name.
         *
         * @return this Attribute's name
         */
        public String getLabel()
            {
            return label;
            }

        public double getScaledAndWeightedScore()
            {
            return scaledAndWeightedScore;
            }

        /**
         * Get this Attribute's score.
         *
         * @return this Attribute's score
         */
        public double getScore()
            {
            return score;
            }

        public void setScaledAndWeightedScore(final double scaledAndWeightedScore)
            {
            this.scaledAndWeightedScore = scaledAndWeightedScore;
            }

        @Override
        public String toString() {
        return "Attribute: " + label + " index: " + index + " score: "
               + String.format("%.4f", score) + " scaledAndWeightedScore: "
               + String.format("%.4f", scaledAndWeightedScore);
        }

        /**
         * Comparison operator to order attributes in ascending order by score.
         */
        public static class ScoreComparator implements Comparator<Attribute>
            {
            /**
             * Compare two attributes.
             *
             * @param a first attribute
             * @param b second attribute
             * @return &lt; 0 if a &lt; b, &gt; 0 if a &gt; b, or 0 otherwise
             */
            public int compare(final Attribute a, final Attribute b)
                {
                return Double.compare(b.getScore(), a.getScore());
                }
            }
        }

    private static class AttributeFitnessData
        {
        public double originalExpertKnowledgeWeight;
        public double lastRouletteWeight;
        public double decayedWeight;
        public double fitnessesRaisedToAlphaSummed;
        public double betaAttributeWeight;
        public double deltaWeight;
        public double adjustedWeight;
        public double normalizedAdjustedWeight;

        public AttributeFitnessData(final double originalExpertKnowledgeWeight)
            {
            this.originalExpertKnowledgeWeight = originalExpertKnowledgeWeight;
            }

        @Override
        public String toString() {
        return "originalExpertKnowledgeWeight: " + originalExpertKnowledgeWeight
               + "\n" + "lastRouletteWeight: " + lastRouletteWeight + "\n"
               + "decayedWeight: " + decayedWeight + "\n"
               + "fitnessesRaisedToAlphaSummed: " + fitnessesRaisedToAlphaSummed
               + "\n" + "betaAttributeWeight: " + betaAttributeWeight + "\n"
               + "deltaWeight: " + deltaWeight + "\n" + "adjustedWeight: "
               + adjustedWeight + "\n" + "normalizedAdjustedWeight: "
               + normalizedAdjustedWeight + "\n\n";
        }
        }

    private class ExpertKnowledgeTableModel extends AbstractTableModel
        {
        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
        Class<?> classVar;
        if (columnIndex == 0)
            {
            classVar = String.class;
            }
        else
            {
            classVar = Double.class;
            }
        return classVar;
        }

        public int getColumnCount()
            {
            return 3;
            }

        @Override
        public String getColumnName(final int columnIndex) {
        String returnValue = null;
        switch (columnIndex)
            {
            case 0:
                returnValue = "Attribute";
                break;
            case 1:
                returnValue = "Score";
                break;
            case 2:
                returnValue = "Probability";
                break;
            default:
                throw new RuntimeException("Unexpected column: " + columnIndex);
                // break;
            } // end switch
        return returnValue;
        }

        public int getRowCount()
            {
            return size();
            }

        public Object getValueAt(final int rowIndex, final int columnIndex)
            {
            Object returnValue = null;
            switch (columnIndex)
                {
                case 0:
                    returnValue = get(rowIndex).getLabel();
                    break;
                case 1:
                    returnValue = get(rowIndex).getScore();
                    break;
                case 2:
                    returnValue = get(rowIndex).getScaledAndWeightedScore();
                    break;
                default:
                    throw new RuntimeException("Unexpected column: " + columnIndex);
                    // break;
                } // end switch
            return returnValue;
            }

        @Override
        public boolean isCellEditable(final int row, final int col) {
        return false;
        }
        } // end ExpertKnowledgeTableModel class

    public class RWRuntime
        {
        private RouletteWheel<Attribute> wheel;
        private final int beta;
        private final int alpha;
        private final double decay;
        private final Map<Integer, AttributeFitnessData> attributeAdjustments = new HashMap<Integer,
                AttributeFitnessData>();

        public RWRuntime(final WeightScheme weightScheme,
                         final ScalingMethod scalingMethod, final double scalingParameter,
                         final int alpha, final int beta, final double decay)
            {
            this.alpha = alpha;
            this.beta = beta;
            this.decay = decay;
            final double[] inputWeights = new double[size()];
            // init ScaledAndWeightedScore to be same as the score
            for (int attributeIndex = 0; attributeIndex < inputWeights.length; ++attributeIndex)
                {
                final Attribute attribute = get(attributeIndex);
                inputWeights[attributeIndex] = attribute.getScore();
                }
            final double[] outputWeights = ExpertKnowledge.scaleAndWeight(
                    inputWeights, scoresMetaInformation, weightScheme, scalingMethod,
                    scalingParameter);
            for (int attributeIndex = 0; attributeIndex < inputWeights.length; ++attributeIndex)
                {
                final Attribute attribute = get(attributeIndex);
                attribute.setScaledAndWeightedScore(outputWeights[attributeIndex]);
                }
            initializeFitnessTrackingdata();
            initializeWheel();
            if (ExpertKnowledge.DEBUG)
                {
                System.out.println("RWRuntime constructor(WeightScheme " + weightScheme
                                   + ", ScalingMethod " + scalingMethod
                                   + ", double reliefFWeightedScalingParameter " + scalingParameter
                                   + " created scaled attributes:\n" + ExpertKnowledge.this);
                wheel.updateTotalIfNeeded();
                System.out.println("RWRuntime constructor initial wheel: "
                                   + wheel.getRouletteWedges());
                }
            } // constructor

        public void adjustRouletteWheel()
            {
            for (int attributeIndex = 0; attributeIndex < size(); ++attributeIndex)
                {
                final Attribute expertKnowledgeAttribute = get(attributeIndex);
                final AttributeFitnessData attributeFitnessData = attributeAdjustments
                        .get(expertKnowledgeAttribute.index);
                attributeFitnessData.betaAttributeWeight = Math.pow(
                        expertKnowledgeAttribute.getScaledAndWeightedScore(), beta);
                attributeFitnessData.deltaWeight = attributeFitnessData.fitnessesRaisedToAlphaSummed
                                                   * attributeFitnessData.betaAttributeWeight;
                }
            final List<RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge> wedges = wheel
                    .getRouletteWedges();
            double fitnessSum = 0.0;
            for (final RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge wedge : wedges)
                {
                final Attribute expertKnowledgeAttribute = wedge.getPrize();
                final AttributeFitnessData attributeFitnessData = attributeAdjustments
                        .get(expertKnowledgeAttribute.index);
                attributeFitnessData.lastRouletteWeight = wedge.getWeight();
                attributeFitnessData.decayedWeight = wedge.getWeight() * decay;
                attributeFitnessData.betaAttributeWeight = Math.pow(
                        expertKnowledgeAttribute.getScaledAndWeightedScore(), beta);
                attributeFitnessData.deltaWeight = attributeFitnessData.fitnessesRaisedToAlphaSummed
                                                   * attributeFitnessData.betaAttributeWeight;
                attributeFitnessData.adjustedWeight = attributeFitnessData.decayedWeight
                                                      + attributeFitnessData.deltaWeight;
                fitnessSum += attributeFitnessData.adjustedWeight;
                }
            // adjust weights one more time to put between 0 and 1
            for (final RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge wedge : wedges)
                {
                final Attribute expertKnowledgeAttribute = wedge.getPrize();
                final AttributeFitnessData attributeFitnessData = attributeAdjustments
                        .get(expertKnowledgeAttribute.index);
                attributeFitnessData.normalizedAdjustedWeight = attributeFitnessData.adjustedWeight
                                                                / fitnessSum;
                wedge.setWeight(attributeFitnessData.normalizedAdjustedWeight);
                }
            if (ExpertKnowledge.DEBUG)
                {
                for (final Map.Entry<Integer, AttributeFitnessData> mapEntry : attributeAdjustments
                        .entrySet())
                    {
                    System.out.println("X" + (mapEntry.getKey() + 1) + ":\n"
                                       + mapEntry.getValue());
                    }
                wheel.updateTotalIfNeeded();
                System.out.println("adjustRouletteWheel modified wedges: " + wedges);
                }
            initializeFitnessTrackingdata();
            }

        public int[] getNAttributes(final Random rnd, final int numberOfAttributes)
            {
            final Set<RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge> pickedAttributes = wheel
                    .spinNTimes(rnd, numberOfAttributes);
            final int[] pickedIndices = new int[pickedAttributes.size()];
            int index = 0;
            for (final RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge wedge : pickedAttributes)
                {
                pickedIndices[index++] = wedge.getPrize().getIndex();
                }
            return pickedIndices;
            }

        private void initializeFitnessTrackingdata()
            {
            attributeAdjustments.clear();
            for (final Attribute attribute : ExpertKnowledge.this)
                {
                attributeAdjustments.put(attribute.index, new AttributeFitnessData(
                        attribute.getScaledAndWeightedScore()));
                }
            }

        public void initializeWheel()
            {
            if (ExpertKnowledge.DEBUG)
                {
                System.out.println("initializeWheel called.");
                }
            wheel = new RouletteWheel<Attribute>();
            // create rouletteWheel
            for (final Attribute attribute : ExpertKnowledge.this)
                {
                wheel.add(attribute, attribute.getScaledAndWeightedScore());
                }
            }

        @Override
        public String toString() {
        String result = "RWheel:";
        final List<RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge> wedges = wheel
                .getRouletteWedges();
        for (final RouletteWheel<ExpertKnowledge.Attribute>.RouletteWedge wedge : wedges)
            {
            result = result + " " + wedge.getPrize().getLabel() + ": "
                     + wedge.getWeight() + "|||";
            }
        return result;
        }

        public synchronized void trackFitness(
                final AttributeCombination attributes, final double fitness)
            {
            for (final int datasetAttributeIndex : attributes.getAttributeIndices())
                {
                final AttributeFitnessData attributeFitnessData = attributeAdjustments
                        .get(datasetAttributeIndex);
                if (attributeFitnessData != null)
                    {
                    attributeFitnessData.fitnessesRaisedToAlphaSummed += Math.pow(
                            fitness, alpha);
                    }
                }
            }
        } // end RWRuntime class

    public static class ScoresMetaInformation
        {
        private final double minScore;
        private final double maxScore;
        private final double linearMaxMin;
        private final double linearMaxMax;
        private final double linearMaxConstant;

        public ScoresMetaInformation(final double min, final double max,
                                     final double sum, final int size)
            {
            linearMaxConstant = (sum - (size * min)) / (max - min);
            linearMaxMin = 1.0 / size;
            linearMaxMax = 1.0 / linearMaxConstant;
            minScore = min;
            maxScore = max;
            }

        public double getLinearMaxConstant()
            {
            return linearMaxConstant;
            }

        public double getLinearMaxMin()
            {
            return linearMaxMin;
            }

        public double getMaxDefault(final WeightScheme weightScheme)
            {
            double returnValue;
            switch (weightScheme)
                {
                case PROPORTIONAL:
                    returnValue = linearMaxMax;
                    break;
                case RANK:
                    /* 2/(n + 1), since linearMaxMin = 1/n */
                    returnValue = 2.0 / ((1.0 / linearMaxMin) + 1.0);
                    break;
                default:
                    throw new RuntimeException(
                            "Unknown WeightScheme passed into getLinearMaxMax: "
                            + weightScheme);
                    // break;
                }
            return returnValue;
            }

        public double getMaxScore()
            {
            return maxScore;
            }

        public double getMinScore()
            {
            return minScore;
            }
        }
    } // end class
