package org.epistasis.combinatoric.mdr;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.logging.Logger;

import org.epistasis.combinatoric.mdr.Console.ScoringMethod;
import org.epistasis.combinatoric.mdr.gui.Frame;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;

public class Main
    {
    public static boolean isExperimental = false;
    public static Logger devLog = Logger.getLogger(Main.class.getCanonicalName());
    public static int minimumRandomSeed;
    public static final boolean defaultComputeAllModelsLandscape = false;
    public static final boolean defaultPreserveTuRFRemovalOrder = false;
    public static final int defaultRandomSeed = 0;
    public static final int defaultAttributeCountMin = 1;
    public static final int defaultAttributeCountMax = 4;
    public static final AttributeCombination defaultForcedAttributeCombination = null;
    public static final double defaultRatioThreshold = 1.0;
    public static final boolean defaultAutoRatioThreshold = false;
    public static final int defaultCrossValidationCount = 10;
    public static final int defaultTopModelsLandscapeSize = 20;
    public static final boolean defaultPairedAnalysis = false;
    public static final Console.AmbiguousCellStatus defaultAmbiguousCellStatus = Console.AmbiguousCellStatus.AFFECTED;
    public static final int defaultSearchTypeIndex = 0;
    public static final boolean defaultIsRandomSearchEvaluations = true;
    public static final boolean defaultIsRandomSearchRuntime = false;
    public static final int defaultRandomSearchEvaluations = 1000;
    public static final double defaultRandomSearchRuntime = 10.0;
    public static final int defaultRandomSearchRuntimeUnitsIndex = 1;
    public static final boolean defaultEDAIsFitnessProportional = true;
    public static final int defaultEDANumUpdates = 100;
    public static final int defaultEDANumAgents = 100;
    public static final double defaultEDARetention = 0.5;
    public static final int defaultEDAAlpha = 1;
    public static final int defaultEDABeta = 1;
    public static final double defaultEDAPercentMaxAttributeRange = 100.0;
    public static final double defaultEDAExponentialTheta = 0.85;
    public static final int defaultFilterIndex = 0;
    public static final int defaultCriterionIndex = 0;
    public static final int defaultCriterionFilterTopN = 5;
    public static final double defaultCriterionFilterTopPct = 5.0;
    public static final double defaultCriterionFilterThreshold = 0.0;
    public static final boolean defaultChiSquaredPValue = false;
    public static final boolean defaultReliefFWholeDataset = true;
    public static final int defaultReliefFNeighbors = 10;
    public static final int defaultReliefFSampleSize = 100;
    public static final int defaultTuRFPct = 10;
    public static final int defaultEntropyGraphLineThickness = 5;
    public static final int defaultEntropyGraphTextSize = 16;
    public static final double pValueTol = 0.0001;
    public static final NumberFormat defaultFormat;
    public static final NumberFormat modelSignificanceFormat;
    public static final NumberFormat modelTextNumberFormat;
    public static final String defaultMissing = "";
    public static final int defaultMDRNodeCount = 1;
    public static final int defaultMDRNodeNumber = 1;
    public static final boolean defaultUseBestModelActualIntervals = false;
    public static final boolean defaultUseExplicitTestOfInteraction = false;
    public static final int defaultPermutations = 0;
    public static final String missingRepresentation = "{MISSING}";
    public static final String unknownRepresentation = "{UNASSIGNED}";
    public static final float defaultFishersThreshold = 1.0f;
    public static final ScoringMethod defaultScoringMethod = ScoringMethod.ADJUSTED_BALANCED_ACCURACY;

    static
        {
        final DecimalFormat df = new DecimalFormat("0.0000");
        final DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
        sym.setNaN("NaN");
        df.setDecimalFormatSymbols(sym);
        defaultFormat = df;
        }

    static
        {
        final DecimalFormat df = new DecimalFormat("0.0########################");
        final DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
        sym.setNaN("NaN");
        df.setDecimalFormatSymbols(sym);
        modelSignificanceFormat = df;
        }

    static
        {
        final DecimalFormat df = new DecimalFormat("0.####");
        final DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
        sym.setNaN("NaN");
        df.setDecimalFormatSymbols(sym);
        modelTextNumberFormat = df;
        }

    public static void main(final String[] args)
        {
        final String experimentalTest = MDRProperties.get("experimentalTest");
        if (experimentalTest != null)
            {
            final String[] propertyAndPossibleValues = experimentalTest.split(" ");
            if (propertyAndPossibleValues.length >= 2)
                {
                final String[] propertyNames = propertyAndPossibleValues[0]
                        .split("[|]");
                for (final String propertyName : propertyNames)
                    {
                    final String propertyValue = System.getProperty(propertyName);
                    if ((propertyValue != null) && (propertyValue.length() > 0))
                        {
                        for (int index = 1; index < propertyAndPossibleValues.length; ++index)
                            {
                            if (propertyValue
                                    .equalsIgnoreCase(propertyAndPossibleValues[index]))
                                {
                                Main.isExperimental = true;
                                break;
                                }
                            }
                        if (Main.isExperimental)
                            {
                            break;
                            }
                        }
                    } // end propertyNames
                }
            }
        if (Main.isExperimental)
            {
            Main.minimumRandomSeed = -1;
            }
        else
            {
            Main.minimumRandomSeed = 0;
            }
        try
            {
            //if (args.length > 0)
                {
                final int status = Console.parseCommandLineAndRun(args);
                System.exit(status);
                }
            //else
                {
                //Frame.run();
                }
            }
        catch (final Exception ex)
            {
            System.err.println("Caught an exception in Main() method: " + ex);
            try
                {
                throw (ex);
                }
            catch (final Exception ex1)
                {
                ex1.printStackTrace();
                }
            }
        }
    }
