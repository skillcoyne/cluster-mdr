package org.epistasis.combinatoric.mdr.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.epistasis.AbstractAttributeScorer;
import org.epistasis.BareBonesBrowserLaunch;
import org.epistasis.ChiSquaredScorer;
import org.epistasis.DisplayPair;
import org.epistasis.FileSaver;
import org.epistasis.IterableEnumeration;
import org.epistasis.OddsRatioScorer;
import org.epistasis.Pair;
import org.epistasis.ReliefFAttributeScorer;
import org.epistasis.SURFAttributeScorer;
import org.epistasis.SURFStarAttributeScorer;
import org.epistasis.SURFStarnTuRFAttributeScorer;
import org.epistasis.SURFnTuRFAttributeScorer;
import org.epistasis.TimerRunnableTask;
import org.epistasis.TuRFAttributeScorer;
import org.epistasis.Utility;
import org.epistasis.combinatoric.AttributeRankerThread;
import org.epistasis.combinatoric.EntropyAnalysis;
import org.epistasis.combinatoric.gui.LandscapePanel;
import org.epistasis.combinatoric.mdr.AnalysisFileManager;
import org.epistasis.combinatoric.mdr.BestModelTextGenerator;
import org.epistasis.combinatoric.mdr.CVResultsTextGenerator;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.ExpertKnowledge;
import org.epistasis.combinatoric.mdr.ExpertKnowledge.RWRuntime;
import org.epistasis.combinatoric.mdr.IfThenRulesTextGenerator;
import org.epistasis.combinatoric.mdr.MDRProperties;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.ScalingMethod;
import org.epistasis.combinatoric.mdr.WeightScheme;
import org.epistasis.combinatoric.mdr.newengine.AnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Collector.MdrByGenotypeResults;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.EDAAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.ExhaustiveAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.FixedRandomAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.ForcedAnalysisThread;
import org.epistasis.combinatoric.mdr.newengine.TimedRandomAnalysisThread;
import org.epistasis.gui.CenterFrame;
import org.epistasis.gui.ComponentEnabler;
import org.epistasis.gui.DatafileFrame;
import org.epistasis.gui.DatafilePanel;
import org.epistasis.gui.OrderedTabbedPane;
import org.epistasis.gui.ProgressPanel;
import org.epistasis.gui.ProgressPanelUpdater;
import org.epistasis.gui.ReadOnlyTableModel;
import org.epistasis.gui.SwingInvoker;
import org.epistasis.gui.SwingPropUtils;
import org.epistasis.gui.TextComponentUpdaterThread;
import org.epistasis.gui.TitledPanel;
import org.epistasis.gui.WarningPanel;

public class Frame extends CenterFrame
    {
    private static final String TRAINING_BAL_ACC = "Training Bal. Acc.";
    private static final String TESTING_BAL_ACC = "Testing Bal. Acc.";
    private static final String CV_CONSISTENCY = "CV Consistency";
    public static JFileChooser fileChooser;
    /**
     * Runnable used to update the memory usage fields on the panel.
     */
    private final static int BytesPerMegabyte = 1024 * 1024;
    // private final static int RecommendedMinimumMemoryMegabytes = 100;
    private final static int MemorySentinelBytes = 35 * Frame.BytesPerMegabyte;
    private static final long serialVersionUID = 1L;
    public static final Font font = new Font("Dialog", Font.PLAIN, 11);
    public static final Font fontBold = Frame.font.deriveFont(Font.BOLD);
    public static final Font fontFixed = new Font("Monospaced", Font.PLAIN, 12);
    private final static boolean parallel = true;
    public static final String MemorySentinelWarningMessage = "MDR does not have enough memory and results are not " +
                                                              "reliable. Close and re-start from the command line " +
                                                              "using instructions in the RELEASE_NOTES.txt";
    private static Frame frame;
    private final Runnable onEnd = new SwingInvoker(new OnEnd(), false);
    private final Runnable onEndAttribute = new SwingInvoker(
            new OnEndAttribute(), false);
    private final Frame_changeAdapter FrameChangeAdapter = new Frame_changeAdapter(
            this);
    private Dataset unfiltered;
    public Dataset data;
    private AttributeRankerThread rankerThread;
    private AttributeCombination forced;
    private AnalysisThread analysis;
    private AnalysisFileManager afm;
    private List<Collector> loadedCollectors;
    private final DatafileFrame frmDatafile = new DatafileFrame(this);
    private final DatafileFrame frmExpertKnowledge = new DatafileFrame(this);
    private String datafileName = "";
    private ExpertKnowledge expertKnowledge = null;
    private final SpinnerNumberModel snmTopN = new SpinnerNumberModel(
            new Integer(Main.defaultCriterionFilterTopN), new Integer(1), null,
            new Integer(1));
    private final SpinnerNumberModel snmTopPct = new SpinnerNumberModel(
            new Double(Main.defaultCriterionFilterTopPct), new Double(
            Double.MIN_VALUE), new Double(100), new Double(5));
    private final SpinnerNumberModel snmThreshold = new SpinnerNumberModel(
            new Double(Main.defaultCriterionFilterThreshold), null, null, new Double(
            1));
    private Timer tmrProgress;
    JTabbedPane tpnAnalysis = new JTabbedPane();
    ProgressPanel prgProgress = new ProgressPanel();
    TitledPanel pnlSummaryTable = new TitledPanel(new BorderLayout());
    OrderedTabbedPane tpnResults = new OrderedTabbedPane();
    JPanel pnlBestModel = new JPanel(new BorderLayout());
    JPanel pnlCVResults = new JPanel(new BorderLayout());
    JPanel pnlAnalysis = new JPanel(new GridBagLayout());
    JScrollPane scpMain = new JScrollPane();
    JPanel pnlMain = new JPanel(new GridBagLayout());
    JPanel pnlBestModelButtons = new JPanel(new GridBagLayout());
    JScrollPane scpBestModel = new JScrollPane();
    JTextArea txaBestModel = new JTextArea();
    private JScrollPane scpAbout;
    private JEditorPane editorPaneAbout;
    private JPanel pnlAbout;
    JButton cmdBestModelSave = new JButton();
    final JCheckBox chkBestModelVerbose = new JCheckBox();
    final JCheckBox chkCVResultsVerbose = new JCheckBox();
    JPanel pnlCVResultsButtons = new JPanel(new GridBagLayout());
    JButton cmdCVResultsSave = new JButton();
    JScrollPane scpCVResults = new JScrollPane();
    JTextArea txaCVResults = new JTextArea();
    JScrollPane scpSummaryTable = new JScrollPane();
    JTable tblSummaryTable = new JTable();
    DefaultTableModel dtmSummaryTable = new ReadOnlyTableModel();
    DefaultTableCellRenderer dcrRightJustified = new DefaultTableCellRenderer();
    TableColumn tbcModel;
    TableColumn tbcTrainingValue;
    TableColumn tbcTestingValue;
    TableColumn tbcCVConsistency;
    TitledPanel pnlDatafileInformation = new TitledPanel(new GridBagLayout());
    JButton cmdRunAnalysis = new JButton();
    JButton cmdLoadAnalysis = new JButton();
    JButton cmdSaveAnalysis = new JButton();
    JLabel lblMaxMem = new JLabel();
    JLabel lblMaxMemLabel = new JLabel("Max Mem:");
    JLabel lblTotalMem = new JLabel();
    JLabel lblTotalMemLabel = new JLabel("Total Mem:");
    JLabel lblUsedMem = new JLabel();
    JLabel lblUsedMemLabel = new JLabel("Used Mem:");
    private final JPanel pnlTotalMem = new JPanel(new GridBagLayout());
    private final JPanel pnlMaxMem = new JPanel(new GridBagLayout());
    private final JPanel pnlUsedMem = new JPanel(new GridBagLayout());
    /**
     * Thread used to continually update the memory usage fields.
     */
    private final Thread memMon = new Thread(new MemoryMonitor(), "MemoryMonitor");
    JButton cmdLoadDatafile = new JButton();
    JButton cmdViewDatafile = new JButton();
    JButton cmdViewExpertKnowledge = new JButton();
    JPanel pnlDatafile = new JPanel(new GridBagLayout());
    JPanel pnlAttributes = new JPanel(new GridBagLayout());
    JPanel pnlInstances = new JPanel(new GridBagLayout());
    JLabel lblDatafileValue = new JLabel();
    JLabel lblDatafile = new JLabel();
    JLabel lblAttributesValue = new JLabel();
    JLabel lblAttributes = new JLabel();
    JLabel lblInstancesValue = new JLabel();
    JLabel lblInstances = new JLabel();
    // Declaration of "Configuration" tab objects
    JPanel pnlConfiguration, pnlSearchAlgorithm, pnlSearchOptions,
            pnlExhaustiveSearchOptions, pnlForcedSearchOptions,
            pnlRandomSearchOptions, pnlEDASearchOptions, pnlEDAUpdate, pnlEDAEKPanel,
            pnlEDAEKInfo, pnlEDAEKConv;
    TitledPanel pnlAnalysisConfiguration, pnlSearchMethodConfiguration,
            pnlEDAExpertKnowledge, pnlAmbiguousCellAnalysis;
    JLabel lblRandomSeed, lblAttributeCountRange, lblAttributeCountRangeColon,
            lblCrossValidationCount, lblPairedAnalysis, lblAmbiguousCellAssignment,
            lblTieCells, lblComputeAllModelsLandscape, lblSearchType,
            lblForcedAttributeCombination, lblUnits, lblNumUpdates, lblNumAgents,
            lblExpertKnowledgeValue, lblExpertKnowledgeFileLoaded, lblLinearMax,
            lblLinearMin, lblExponentialTheta, lblRetention, lblAlpha, lblBeta;
    JSpinner spnRandomSeed, spnAttributeCountMin, spnAttributeCountMax,
            spnCrossValidationCount, spnEvaluations, spnRuntime, spnLinearMaxPercent,
            spnExponentialTheta, spnNumUpdates, spnNumAgents, spnRetention, spnAlpha,
            spnBeta, spnFisherThreshold;
    final JCheckBox chkPairedAnalysis = new JCheckBox();
    JSpinner spnTopModels;
    final JCheckBox chkComputeAllModelsLandscape = new JCheckBox();
    JComboBox cboAmbiguousCellStatuses, cboSearchType, cboUnits;
    JTextField txtForcedAttributeCombination;
    ButtonGroup bgrExpertKnowledgeScalingMethod,
            bgrExpertKnowledgeWeightingScheme, bgrAmbiguousCellCriteria;
    JRadioButton rdoEvaluations, rdoRuntime, rdoFitnessProportional,
            rdoRankedSelection, rdoExpertKnowledgeWeightingSchemeGhost,
            rdoLinearKnowledge, rdoExponentialKnowledge,
            rdoExpertKnowledgeScalingMethodGhost, rdoAmbiguousCellCriteriaTieCells,
            rdoAmbiguousCellCriteriaFishersExact;
    JButton cmdDefaults, cmdLoadExpertKnowledge;
    /*
	 * Map<String, Double> antCacheMap; public Vector<Pair< String, Float >> bestAnts;
	 */
    // End "Configuration" tab declarations
    TitledPanel pnlAnalysisControls = new TitledPanel(new GridBagLayout());
    WarningPanel pnlWarning = new WarningPanel();
    JPanel pnlRatio = new JPanel(new GridBagLayout());
    JLabel lblRatio = new JLabel();
    JLabel lblRatioValue = new JLabel();
    JPanel pnlFilter = new JPanel(new GridBagLayout());
    JLabel lblFilter = new JLabel();
    JComboBox cboFilter = new JComboBox();
    JLabel lblCriterion = new JLabel();
    JComboBox cboCriterion = new JComboBox();
    JLabel lblCriterionValue = new JLabel();
    JSpinner spnCriterionFilter = new JSpinner();
    TitledPanel pnlFilterOptions = new TitledPanel(new GridBagLayout());
    TitledPanel pnlFilterControls = new TitledPanel(new GridBagLayout());
    JButton cmdDefaultFilter = new JButton();
    JButton cmdExportFiltered = new JButton();
    JButton cmdRevertFilter = new JButton();
    JButton cmdFilter = new JButton();
    CardLayout crdFilterOptions = new CardLayout();
    JPanel pnlReliefFOptions = new JPanel(new GridBagLayout());
    JLabel lblReliefFNeighbors = new JLabel();
    JSpinner spnReliefFNeighbors = new JSpinner();
    JLabel lblReliefFSampleSize = new JLabel();
    JSpinner spnReliefFSampleSize = new JSpinner();
    final JCheckBox chkReliefFWholeDataset = new JCheckBox();
    final JPanel pnlOddsRatioOptions = new JPanel(new GridBagLayout());
    JPanel pnlChiSquaredOptions = new JPanel(new GridBagLayout());
    final JCheckBox chkChiSquaredPValue = new JCheckBox();
    JPanel pnlTuRFOptions = new JPanel(new GridBagLayout());
    JLabel lblTuRFPercent = new JLabel();
    JSpinner spnTuRFPercent = new JSpinner();
    JLabel lblTuRFNeighbors = new JLabel();
    JSpinner spnTuRFNeighbors = new JSpinner();
    JLabel lblTuRFSampleSize = new JLabel();
    JSpinner spnTuRFSampleSize = new JSpinner();
    final JCheckBox chkTuRFWholeDataset = new JCheckBox();
    JPanel pnlSURFOptions = new JPanel(new GridBagLayout());
    JPanel pnlSURFStarOptions = new JPanel(new GridBagLayout());
    JPanel pnlSURFnTuRFOptions = new JPanel(new GridBagLayout());
    JLabel lblSURFnTuRFPercent = new JLabel();
    JSpinner spnSURFnTuRFPercent = new JSpinner();
    JPanel pnlSURFStarnTuRFOptions = new JPanel(new GridBagLayout());
    JLabel lblSURFStarnTuRFPercent = new JLabel();
    JSpinner spnSURFStarnTuRFPercent = new JSpinner();
    GraphicalModelControls pnlGraphicalModel = new GraphicalModelControls();
    JPanel pnlIfThenRules = new JPanel(new BorderLayout());
    JPanel pnlIfThenRulesButtons = new JPanel(new GridBagLayout());
    JButton cmdIfThenRulesSave = new JButton();
    JScrollPane scpIfThenRules = new JScrollPane();
    JTextArea txaIfThenRules = new JTextArea();
    LandscapePanel pnlAllModelsLandscape = new LandscapePanel();
    LandscapePanel pnlTopModelsLandscape = new LandscapePanel();
    JTabbedPane tpnFilter = new JTabbedPane();
    JPanel pnlDatasetView = new JPanel(new BorderLayout());
    LandscapePanel pnlFilterLandscape = new LandscapePanel();
    DatafilePanel pnlDatafileTable = new DatafilePanel();
    JPanel pnlDatafileViewButtons = new JPanel(new GridBagLayout());
    JButton cmdRevert = new JButton();
    ProgressPanel prgFilterProgress = new ProgressPanel();
    TitledPanel pnlFilterSelection = new TitledPanel(new GridBagLayout());
    JLabel lblDatafileLoaded = new JLabel();
    JLabel lblDatafileFiltered = new JLabel();
    ButtonGroup bgrRandomOptions = new ButtonGroup();
    CardLayout crdSearchOptions = new CardLayout();
    JPanel pnlEntropy = new JPanel(new BorderLayout());
    JPanel pnlEntropyDisplayCommonControls = new JPanel(new GridBagLayout());
    EntropyAnalysis entropyAnalysis = new EntropyAnalysis();
    Dendrogram pnlDendrogram = new Dendrogram(entropyAnalysis);
    InteractionGraph pnlForceDirectedGraph = new InteractionGraph(
            Main.modelTextNumberFormat, 5, entropyAnalysis,
            InteractionGraph.NetworkType.FORCE_DIRECTED_GRAPH);
    InteractionGraph pnlRadialTree = new InteractionGraph(
            Main.modelTextNumberFormat, 5, entropyAnalysis,
            InteractionGraph.NetworkType.RADIAL_TREE);
    InteractionGraph pnlCircleGraph = new InteractionGraph(
            Main.modelTextNumberFormat, 5, entropyAnalysis,
            InteractionGraph.NetworkType.CIRCLE_LAYOUT);
    JComboBox cboInteractionGraphType = new JComboBox(new DefaultComboBoxModel(
            InteractionGraph.NetworkType.values()));
    JButton cmdSaveEntropy = new JButton();
    JLabel lblEntropyGraphLineThickness = new JLabel();
    JSpinner spnEntropyGraphLineThickness = new JSpinner();
    JLabel lblMinimumAbsoluteEntropy = new JLabel();
    JSpinner spnMinimumAbsoluteEntropy = new JSpinner();
    JLabel lblEntropyGraphTextSize = new JLabel();
    JSpinner spnEntropyGraphTextSize = new JSpinner();
    AttributeConstructionPanel pnlConstruct = new AttributeConstructionPanel();
    CovariateAdjustmentPanel pnlAdjust = new CovariateAdjustmentPanel();
    JButton cmdResetEntropyGraph = new JButton();
    JButton cmdMaximizeEntropy = new JButton();
    // Panel contains a card layout so that only one
    // component is visible at a time. Override
    // getPreferredSize to getPreferredSize of
    // the currently visible item. This will determine whether
    // scroll bars are shown
    @SuppressWarnings("serial")
    JPanel pnlEntropyDisplay = new JPanel(new BorderLayout())
    {
    @Override
    public Dimension getPreferredSize() {
    Dimension returnDimension = null;
    for (final Component component : getComponents())
        {
        final boolean isVisible = component.isVisible();
        if (isVisible)
            {
            returnDimension = component.getPreferredSize();
            break;
            }
        }
    return returnDimension;
    }
    };
    JScrollPane scpEntropyDisplay = new JScrollPane(pnlEntropyDisplay);
    JTextArea txaRawEntropyValues = new JTextArea();
    JPanel pnlEntropyGraphControls = new JPanel(new GridBagLayout());
    JPanel pnlEntropyDisplaySpecificControls = new JPanel(new GridBagLayout());
    JPanel pnlRawEntropyValuesControls = new JPanel(new GridBagLayout());
    @SuppressWarnings("serial")
    Map<String, Component> entropyPanelSpecificControls = new HashMap<String, Component>()
    {
    {
    put(DisplayType.Dendogram.toString(), pnlEntropyGraphControls);
    put(DisplayType.ForceDirectedGraph.toString(), pnlEntropyGraphControls);
    put(DisplayType.RadialTree.toString(), pnlEntropyGraphControls);
    put(DisplayType.CircleGraph.toString(), pnlEntropyGraphControls);
    put(DisplayType.Raw_Entropy_Values.toString(),
        pnlRawEntropyValuesControls);
    }
    };
    @SuppressWarnings("serial")
    Map<String, Component> entropyPanelEntropyDisplay = new HashMap<String, Component>()
    {
    {
    put(DisplayType.Dendogram.toString(), pnlDendrogram);
    put(DisplayType.ForceDirectedGraph.toString(), pnlForceDirectedGraph);
    put(DisplayType.RadialTree.toString(), pnlRadialTree);
    put(DisplayType.CircleGraph.toString(), pnlCircleGraph);
    put(DisplayType.Raw_Entropy_Values.toString(), txaRawEntropyValues);
    }
    };
    JComboBox cboEntropy = new JComboBox(new DefaultComboBoxModel(
            DisplayType.values()));
    JLabel lblLogo = new JLabel();
    private JLabel lblBestModelCriteria;
    private JComboBox cboBestModelCriteria;
    private JLabel lblUseBestModelActualIntervals;
    private JCheckBox chkUseBestModelActualIntervals;
    private final ChangeListener filterChangeListener = new ChangeListener()
    {
    public void stateChanged(final ChangeEvent e)
        {
        cmdDefaultFilter.setEnabled(!isDefaultFilter());
        }
    };
    private final PropertyChangeListener filterPropertyChangeListener = new PropertyChangeListener()
    {
    public void propertyChange(final PropertyChangeEvent evt)
        {
        cmdDefaultFilter.setEnabled(!isDefaultFilter());
        }
    };

    public static Frame getFrame()
        {
        return Frame.frame;
        }

    public static void run()
        {
        SwingPropUtils.setProperty("swing.aatext", "true", false);
        SwingPropUtils.useSystemLookAndFeel(false);
        final Frame runFrame = new Frame(GraphicsEnvironment
                                                 .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                                                 .getDefaultConfiguration());
        runFrame.setVisible(true);
        }

    public Frame(final GraphicsConfiguration gc) throws HeadlessException
        {
        super(gc);
        Frame.frame = this;
        init();
        center(gc.getBounds());
        Frame.fileChooser = new JFileChooser(); // initialized here rather tand statically so that look and feel has
        // been set
        // if (Runtime.getRuntime().maxMemory() < (Frame.RecommendedMinimumMemoryMegabytes * Frame.BytesPerMegabyte)) {
        // SwingUtilities.invokeLater(new Runnable() {
        // public void run() {
        // JOptionPane
        // .showMessageDialog(
        // Frame.this,
        // "Java was given less than "
        // + Frame.RecommendedMinimumMemoryMegabytes
        // + " meagabytes of memory which may make MDR fail.\n"
        // + "We recommend starting java from the command line like this 'java -Xmx1000mb -jar MDR.jar'",
        // "Low memory", JOptionPane.WARNING_MESSAGE);
        // }
        // });
        // }
        }

    private void addTableRow(final Collector.BestModel summary,
                             final int intervals)
        {
        final Vector<String> v = new Vector<String>(5);
        v.add(summary.getModel().getCombo().toString());
        v.add(Main.defaultFormat.format(summary.getAvgTrain().getFitness()));
        if (intervals > 1)
            {
            v.add(Main.defaultFormat.format(summary.getAvgTest().getFitness()));
            v.add(Integer.toString(summary.getCVC()) + '/' + intervals);
            }
        dtmSummaryTable.addRow(v);
        if (tblSummaryTable.getSelectedRow() < 0)
            {
            tblSummaryTable.getSelectionModel().setSelectionInterval(0, 0);
            }
        }

    public void cboCriterion_itemStateChanged(final ItemEvent e)
        {
        switch (cboCriterion.getSelectedIndex())
            {
            case 0:
                spnCriterionFilter.setModel(snmTopN);
                break;
            case 1:
                spnCriterionFilter.setModel(snmTopPct);
                break;
            case 2:
                spnCriterionFilter.setModel(snmThreshold);
                break;
            }
        cmdDefaultFilter.setEnabled(!isDefaultFilter());
        }

    public void cboFilter_itemStateChanged(final ItemEvent e)
        {
        final String s = cboFilter.getSelectedItem().toString();
        crdFilterOptions.show(pnlFilterOptions, s);
        cmdDefaultFilter.setEnabled(!isDefaultFilter());
        }

    public void cboSearchType_itemStateChanged(final ItemEvent e)
        {
        final String selected = cboSearchType.getSelectedItem().toString();
        crdSearchOptions.show(pnlSearchOptions, selected);
        final boolean tempForced = selected.equalsIgnoreCase("Forced");
        lblAttributeCountRange.setEnabled(!tempForced);
        spnAttributeCountMin.setEnabled(!tempForced);
        lblAttributeCountRangeColon.setEnabled(!tempForced);
        spnAttributeCountMax.setEnabled(!tempForced);
        if (!tempForced || (txtForcedAttributeCombination.getText().length() == 0))
            {
            forced = null;
            }
        else
            {
            forced = new AttributeCombination(
                    txtForcedAttributeCombination.getText(), data.getLabels());
            }
        }

    public void checkConfigDefaultsOnEvent(final Object e)
        {
        cmdDefaults.setEnabled(!isDefaultConfig());
        }

    public void chkPairedAnalysis_actionPerformed(final ActionEvent e)
        {
        if (data != null)
            {
            data.setPaired(chkPairedAnalysis.isSelected());
            }
        }

    private void clearFilterTabs()
        {
        pnlFilterLandscape.setEnabled(false);
        pnlFilterLandscape.setLandscape(null);
        }

    private void clearTabs(final boolean modelBasedOnly)
        {
        cmdSaveAnalysis.setEnabled(false);
        txaBestModel.setText("");
        cmdBestModelSave.setEnabled(false);
        txaCVResults.setText("");
        cmdCVResultsSave.setEnabled(false);
        txaIfThenRules.setText("");
        cmdIfThenRulesSave.setEnabled(false);
        pnlGraphicalModel.setModel(null);
        setModelDependentItemsEnabled(false);
        if (!modelBasedOnly)
            {
            pnlWarning.clear();
            pnlAllModelsLandscape.setEnabled(false);
            pnlAllModelsLandscape.setLandscape(null);
            pnlTopModelsLandscape.setEnabled(false);
            pnlTopModelsLandscape.setLandscape(null);
            entropyAnalysis.clear();
            final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                               .getSelectedItem().toString());
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                entropyDisplay.updateGraph();
                }
            txaRawEntropyValues.setText("");
            cmdSaveEntropy.setEnabled(false);
            lblEntropyGraphLineThickness.setEnabled(false);
            spnEntropyGraphLineThickness.setEnabled(false);
            lblEntropyGraphTextSize.setEnabled(false);
            spnEntropyGraphTextSize.setEnabled(false);
            }
        }

    public void cmdBestModelSave_actionPerformed(final ActionEvent e)
        {
        final Pair<File, FileFilter> ff = FileSaver.getSaveFile(this,
                                                                "Save Best Model Output", FileSaver.fltText);
        if (ff == null)
            {
            return;
            }
        try
            {
            FileSaver.saveText(txaBestModel.getText(), ff.getFirst());
            }
        catch (final IOException ex)
            {
            Utility.logException(ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            }
        }

    public void cmdCVResultsSave_actionPerformed(final ActionEvent e)
        {
        final Pair<File, FileFilter> ff = FileSaver.getSaveFile(this,
                                                                "Save CV Results Output", FileSaver.fltText);
        if (ff == null)
            {
            return;
            }
        try
            {
            FileSaver.saveText(txaCVResults.getText(), ff.getFirst());
            }
        catch (final IOException ex)
            {
            Utility.logException(ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            }
        }

    public void cmdExportFiltered_actionPerformed(final ActionEvent e)
        {
        final PrintWriter out = FileSaver.openFileWriter(this, "Export Datafile",
                                                         FileSaver.fltText);
        if (out != null)
            {
            data.write(out);
            out.flush();
            out.close();
            }
        }

    public void cmdFilter_actionPerformed(final ActionEvent e)
        {
        if ((rankerThread != null) && rankerThread.isAlive())
            {
            rankerThread.interrupt();
            return;
            }
        AbstractAttributeScorer scorer = null;
        boolean ascending = false;
        if (unfiltered == null)
            {
            unfiltered = data;
            }
        long seed = ((Long) spnRandomSeed.getValue()).longValue();
        if (seed == -1)
            {
            seed = System.currentTimeMillis();
            System.out.println("Randomly generated random number generator seed: "
                               + seed);
            }
        switch (cboFilter.getSelectedIndex())
            {
            // relief-f
            case 0:
            {
            final int numSamples = chkReliefFWholeDataset.isSelected() ? data
                    .getRows() : ((Number) spnReliefFSampleSize.getValue()).intValue();
            scorer = new ReliefFAttributeScorer(data, numSamples,
                                                ((Number) spnReliefFNeighbors.getValue()).intValue(), new Random(
                    seed), Frame.parallel, new SwingInvoker(
                    new ProgressPanelUpdater(prgFilterProgress, numSamples)));
            }
            break;
            // turf
            case 1:
            {
            final int numSamples = chkTuRFWholeDataset.isSelected() ? data
                    .getRows() : ((Number) spnTuRFSampleSize.getValue()).intValue();
            final float TuRFPercent = ((Number) spnTuRFPercent.getValue())
                                              .intValue() / 100f;
            final int sampleMultiplier = (int) Math.ceil(1 / TuRFPercent);
            final int totalSamples = sampleMultiplier * numSamples;
            scorer = new TuRFAttributeScorer(data, numSamples,
                                             ((Number) spnTuRFNeighbors.getValue()).intValue(),
                                             ((Number) spnTuRFPercent.getValue()).intValue() / 100f, new Random(
                    ((Number) spnRandomSeed.getValue()).longValue()), true,
                                             new SwingInvoker(new ProgressPanelUpdater(prgFilterProgress,
                                                                                       totalSamples)));
            }
            break;
            // chi-squared
            case 2:
                scorer = new ChiSquaredScorer(data, chkChiSquaredPValue.isSelected(),
                                              true, new SwingInvoker(new ProgressPanelUpdater(prgFilterProgress,
                                                                                              data.getCols() - 1)));
                ascending = chkChiSquaredPValue.isSelected();
                break;
            // odds ratio
            case 3:
                scorer = new OddsRatioScorer(data, true, new SwingInvoker(
                        new ProgressPanelUpdater(prgFilterProgress, data.getCols() - 1)));
                break;
            // SuRF
            case 4:
            {
            scorer = new SURFAttributeScorer(data, new Random(seed),
                                             Frame.parallel, new SwingInvoker(new ProgressPanelUpdater(
                    prgFilterProgress, data.getRows())));
            break;
            }
            // SuRFnTuRF
            case 5:
            {
            final int numSamples = data.getRows();
            final float SURFnTuRFPercent = ((Number) spnSURFnTuRFPercent.getValue())
                                                   .intValue() / 100f;
            final int sampleMultiplier = (int) Math.ceil(1 / SURFnTuRFPercent);
            final int totalSamples = sampleMultiplier * numSamples;
            scorer = new SURFnTuRFAttributeScorer(data,
                                                  ((Number) spnSURFnTuRFPercent.getValue()).intValue() / 100f,
                                                  new Random(((Number) spnRandomSeed.getValue()).longValue()), true,
                                                  new SwingInvoker(new ProgressPanelUpdater(prgFilterProgress,
                                                                                            totalSamples)));
            break;
            }
            // SURFStar
            case 6:
            {
            scorer = new SURFStarAttributeScorer(data, new Random(seed),
                                                 Frame.parallel, new SwingInvoker(new ProgressPanelUpdater(
                    prgFilterProgress, data.getRows())));
            break;
            }
            // SuRF*nTuRF
            case 7:
            {
            final int numSamples = data.getRows();
            final float SURFStarnTuRFPercent = ((Number) spnSURFStarnTuRFPercent
                    .getValue()).intValue() / 100f;
            final int sampleMultiplier = (int) Math.ceil(1 / SURFStarnTuRFPercent);
            final int totalSamples = sampleMultiplier * numSamples;
            scorer = new SURFStarnTuRFAttributeScorer(data,
                                                      ((Number) spnSURFStarnTuRFPercent.getValue()).intValue() / 100f,
                                                      new Random(((Number) spnRandomSeed.getValue()).longValue()), true,
                                                      new SwingInvoker(new ProgressPanelUpdater(prgFilterProgress,
                                                                                                totalSamples)));
            break;
            }
            }
        final Runnable tempOnEnd = new SwingInvoker(new OnFilterEnd(ascending),
                                                    false);
        switch (cboCriterion.getSelectedIndex())
            {
            // Top N
            case 0:
                rankerThread = new AttributeRankerThread(scorer, snmTopN.getNumber()
                        .intValue(), ascending, tempOnEnd);
                break;
            // Top %
            case 1:
                rankerThread = new AttributeRankerThread(scorer, snmTopPct.getNumber()
                                                                         .doubleValue() / 100.0, ascending, tempOnEnd);
                break;
            // Threshold
            case 2:
                rankerThread = new AttributeRankerThread(scorer, snmThreshold
                        .getNumber().doubleValue(), !ascending, true, tempOnEnd);
                break;
            }
        lockdown(true);
        cmdFilter.setText("Stop");
        cmdFilter.setForeground(Color.RED);
        cmdFilter.setEnabled(true);
        rankerThread.start();
        }

    public void cmdIfThenRulesSave_actionPerformed(final ActionEvent e)
        {
        final Pair<File, FileFilter> ff = FileSaver.getSaveFile(this,
                                                                "Save If-Then Rules Output", FileSaver.fltText);
        if (ff != null)
            {
            try
                {
                FileSaver.saveText(txaIfThenRules.getText(), ff.getFirst());
                }
            catch (final IOException ex)
                {
                Utility.logException(ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                              JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    public void cmdLoadAnalysis_actionPerformed(final ActionEvent e)
        {
        Frame.fileChooser.setMultiSelectionEnabled(false);
        Frame.fileChooser.setDialogTitle("Load Analysis");
        Frame.fileChooser.addChoosableFileFilter(FileSaver.txtFilter);
        if (Frame.fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
            final Thread loadThread = new Thread(new Runnable()
            {
            public void run()
                {
                try
                    {
                    prgProgress.setIndeterminate(true);
                    prgProgress.setString("Loading saved analysis: "
                                          + Frame.fileChooser.getSelectedFile().getName());
                    final SwingInvoker progressUpdater = new SwingInvoker(
                            new ProgressPanelUpdater(prgProgress, Long.MAX_VALUE), false);
                    lockdown(true);
                    // create a blank placeholder dataset since code doesn't deal well with no dataset
                    data = new Dataset(Main.defaultMissing, Main.defaultPairedAnalysis);
                    datafileName = "";
                    unfiltered = null;
                    loadedCollectors = null;
                    dataFileChange();
                    try
                        {
                        afm = new AnalysisFileManager();
                        afm.read(
                                new LineNumberReader(new FileReader(Frame.fileChooser
                                                                            .getSelectedFile())), progressUpdater);
                        if (afm.getFiltered() == null)
                            {
                            unfiltered = null;
                            data = afm.getDataset();
                            }
                        else
                            {
                            unfiltered = afm.getDataset();
                            data = afm.getFiltered();
                            }
                        spnAttributeCountMin.setValue(new Integer(afm.getMin()));
                        spnAttributeCountMax.setValue(new Integer(afm.getMax()));
                        spnCrossValidationCount.setValue(new Integer(afm.getIntervals()));
                        txtForcedAttributeCombination
                                .setText(afm.getForced() == null ? "" : afm.getForced()
                                        .getComboString());
                        chkPairedAnalysis.setSelected(afm.isPaired());
                        chkComputeAllModelsLandscape.setSelected(afm
                                                                         .getAllModelsLandscape() != null);
                        final Console.AmbiguousCellStatus tiePriority = afm
                                .getTiePriority();
                        setComboAmbiguousStatus(tiePriority);
                        datafileName = Frame.fileChooser.getSelectedFile()
                                .getAbsolutePath();
                        dataFileChange();
                        if ((afm.getAllModelsLandscape() != null)
                            && !afm.getAllModelsLandscape().isEmpty())
                            {
                            pnlAllModelsLandscape.setLandscape(afm.getAllModelsLandscape()
                                                                       .getLandscape(), false);
                            pnlAllModelsLandscape.setEnabled(true);
                            tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, true);
                            }
                        else
                            {
                            tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, false);
                            }
                        loadedCollectors = afm.getCollectors();
                        final Collector lastCollector = loadedCollectors
                                .get(loadedCollectors.size() - 1);
                        pnlTopModelsLandscape.setLandscape(null, false);
                        if (lastCollector.getTopModelsLandscape() != null)
                            {
                            spnTopModels.setValue(lastCollector.getTopModelsLandscape()
                                                          .getLandscape().size());
                            tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, true);
                            pnlTopModelsLandscape.setEnabled(true);
                            }
                        else
                            {
                            tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, false);
                            spnTopModels.setValue(Main.defaultTopModelsLandscapeSize);
                            }
                        for (final Collector coll : loadedCollectors)
                            {
                            addTableRow(coll.getBest(), afm.getIntervals());
                            }
                        final String wrapper = afm.getCfg(AnalysisFileManager.cfgWrapper);
                        if ((wrapper == null)
                            || wrapper
                                .equalsIgnoreCase(AnalysisFileManager.cfgValExhaustive))
                            {
                            cboSearchType.setSelectedIndex(0);
                            }
                        else if (wrapper
                                .equalsIgnoreCase(AnalysisFileManager.cfgValForced))
                            {
                            cboSearchType.setSelectedIndex(1);
                            }
                        else if (wrapper
                                .equalsIgnoreCase(AnalysisFileManager.cfgValRandom))
                            {
                            cboSearchType.setSelectedIndex(2);
                            String s = afm.getCfg(AnalysisFileManager.cfgEvaluations);
                            if (s != null)
                                {
                                rdoEvaluations.setSelected(true);
                                spnEvaluations.setValue(Integer.valueOf(s));
                                }
                            else
                                {
                                s = afm.getCfg(AnalysisFileManager.cfgRuntime);
                                rdoRuntime.setSelected(true);
                                spnRuntime.setValue(Double.valueOf(s));
                                s = afm.getCfg(AnalysisFileManager.cfgRuntimeUnits);
                                if ((s == null) || s.equalsIgnoreCase("MINUTES"))
                                    {
                                    cboUnits.setSelectedIndex(1);
                                    }
                                else if (s.equalsIgnoreCase("SECONDS"))
                                    {
                                    cboUnits.setSelectedIndex(0);
                                    }
                                else if (s.equalsIgnoreCase("HOURS"))
                                    {
                                    cboUnits.setSelectedIndex(2);
                                    }
                                else if (s.equalsIgnoreCase("DAYS"))
                                    {
                                    cboUnits.setSelectedIndex(3);
                                    }
                                else
                                    {
                                    cboUnits.setSelectedIndex(1);
                                    }
                                }
                            }
                        if (unfiltered != null)
                            {
                            final String filtername = afm.getFilterConfig().get("FILTER");
                            if (filtername.equalsIgnoreCase("RELIEFF"))
                                {
                                cboFilter.setSelectedIndex(0);
                                pnlFilterLandscape.setYAxisLabel("ReliefF");
                                final String samples = afm.getFilterConfig().get("SAMPLES");
                                if (samples.equalsIgnoreCase("ALL"))
                                    {
                                    chkReliefFWholeDataset.setSelected(true);
                                    }
                                else
                                    {
                                    chkReliefFWholeDataset.setSelected(false);
                                    spnReliefFSampleSize.setValue(Integer.valueOf(samples));
                                    }
                                if (afm.getFilterConfig().containsKey("NEIGHBORS"))
                                    {
                                    spnReliefFNeighbors.setValue(Integer.valueOf(afm
                                                                                         .getFilterConfig().get
                                                    ("NEIGHBORS")));
                                    }
                                }
                            else if (filtername.equalsIgnoreCase("TURF"))
                                {
                                cboFilter.setSelectedIndex(1);
                                pnlFilterLandscape.setYAxisLabel("TuRF");
                                final String samples = afm.getFilterConfig().get("SAMPLES");
                                if (samples.equalsIgnoreCase("ALL"))
                                    {
                                    chkTuRFWholeDataset.setSelected(true);
                                    }
                                else
                                    {
                                    chkTuRFWholeDataset.setSelected(false);
                                    spnTuRFSampleSize.setValue(Integer.valueOf(samples));
                                    }
                                if (afm.getFilterConfig().containsKey("NEIGHBORS"))
                                    {
                                    spnTuRFNeighbors.setValue(Integer.valueOf(afm
                                                                                      .getFilterConfig().get
                                                    ("NEIGHBORS")));
                                    }
                                if (afm.getFilterConfig().containsKey("PCT"))
                                    {
                                    spnTuRFPercent.setValue(Float.valueOf(afm.getFilterConfig()
                                                                                  .get("PCT")) * 100);
                                    }
                                }
                            else if (filtername.equalsIgnoreCase("CHISQUARED"))
                                {
                                cboFilter.setSelectedIndex(2);
                                pnlFilterLandscape.setYAxisLabel("\u03a7\u00b2");
                                if (afm.getFilterConfig().containsKey("PVALUE"))
                                    {
                                    chkChiSquaredPValue.setSelected(Boolean.valueOf(
                                            afm.getFilterConfig().get("PVALUE")).booleanValue());
                                    }
                                }
                            else if (filtername.equalsIgnoreCase("ODDSRATIO"))
                                {
                                cboFilter.setSelectedIndex(3);
                                pnlFilterLandscape.setYAxisLabel("OddsRatio");
                                }
                            else if (filtername.equalsIgnoreCase("SURF"))
                                {
                                cboFilter.setSelectedIndex(4);
                                pnlFilterLandscape.setYAxisLabel("SURF");
                                }
                            else if (filtername.equalsIgnoreCase("SURFNTURF"))
                                {
                                cboFilter.setSelectedIndex(5);
                                pnlFilterLandscape.setYAxisLabel("SURFnTuRF");
                                if (afm.getFilterConfig().containsKey("PCT"))
                                    {
                                    spnSURFnTuRFPercent.setValue(Float.valueOf(afm
                                                                                       .getFilterConfig().get("PCT"))
                                                                 * 100);
                                    }
                                }
                            else if (filtername.equalsIgnoreCase("SURF*"))
                                {
                                cboFilter.setSelectedIndex(6);
                                pnlFilterLandscape.setYAxisLabel("SURF*");
                                }
                            else if (filtername.equalsIgnoreCase("SURF*NTURF"))
                                {
                                cboFilter.setSelectedIndex(7);
                                pnlFilterLandscape.setYAxisLabel("SURF*nTuRF");
                                if (afm.getFilterConfig().containsKey("PCT"))
                                    {
                                    spnSURFStarnTuRFPercent.setValue(Float.valueOf(afm
                                                                                           .getFilterConfig().get
                                                    ("PCT")) * 100);
                                    }
                                }
                            final String selection = afm.getFilterConfig().get("SELECTION");
                            final String selectionvalue = afm.getFilterConfig().get(
                                    "SELECTIONVALUE");
                            if (selection != null)
                                {
                                if (selection.equalsIgnoreCase("TOPN"))
                                    {
                                    cboCriterion.setSelectedIndex(0);
                                    if (selectionvalue != null)
                                        {
                                        spnCriterionFilter.setValue(Integer
                                                                            .valueOf(selectionvalue));
                                        }
                                    }
                                else if (selection.equalsIgnoreCase("TOP%"))
                                    {
                                    cboCriterion.setSelectedIndex(1);
                                    if (selectionvalue != null)
                                        {
                                        spnCriterionFilter.setValue(Double
                                                                            .valueOf(selectionvalue));
                                        }
                                    }
                                else if (selection.equalsIgnoreCase("THRESHOLD"))
                                    {
                                    cboCriterion.setSelectedIndex(2);
                                    if (selectionvalue != null)
                                        {
                                        spnCriterionFilter.setValue(Double
                                                                            .valueOf(selectionvalue));
                                        }
                                    }
                                }
                            final List<String> labels = unfiltered.getLabels();
                            final List<Pair<Integer, Float>> scores = afm.getFilterScores();
                            final List<Pair<String, Float>> landscape = new ArrayList<Pair<String, Float>>(
                                    scores.size());
                            for (final Pair<Integer, Float> p : scores)
                                {
                                landscape.add(new Pair<String, Float>(
                                        labels.get(p.getFirst()), p.getSecond()));
                                }
                            pnlFilterLandscape.setLandscape(landscape);
                            pnlFilterLandscape.setEnabled(true);
                            }
                        updateEntropyDisplay(afm.getDataset(), afm.getCollectors(),
                                             afm.getMin(), afm.getMax(), afm.getTiePriority());
                        final boolean displayTestingColumn = (afm.getIntervals() > 1);
                        final boolean displayCvcColumn = displayTestingColumn
                                                         && !afm.getCfg(AnalysisFileManager.cfgWrapper)
                                .equalsIgnoreCase(AnalysisFileManager.cfgValForced);
                        setTestingAndCvcColumnsDisplay(displayTestingColumn,
                                                       displayCvcColumn, afm.getTiePriority());
                        analysis = null;
                        }
                    catch (final Exception ex)
                        {
                        Utility.logException(ex);
                        JOptionPane
                                .showMessageDialog(
                                        Frame.this,
                                        "Unable to open analysis '"
                                        + Frame.fileChooser.getSelectedFile().toString()
                                        + "' or unrecognized file format.\n\nIs it possible you meant to use 'Load " +
                                        "Datafile'?\n\nMessage:\n"
                                        + ex, "Datafile Error", JOptionPane.ERROR_MESSAGE);
                        // create a blank placeholder dataset since code doesn't deal well with no dataset
                        data = new Dataset(Main.defaultMissing,
                                           Main.defaultPairedAnalysis);
                        datafileName = "";
                        unfiltered = null;
                        loadedCollectors = null;
                        analysis = null;
                        }
                    }
                finally
                    {
                    prgProgress.setIndeterminate(false);
                    prgProgress.setString(null);
                    lockdown(false);
                    }
                } // end run
            }, "LoadSavedAnalysisThread");
            loadThread.start();
            } // if user chose file
        } // end cmdLoadAnalysis_actionPerformed

    public void cmdLoadDatafile_actionPerformed(final ActionEvent e)
        {
        Frame.fileChooser.setMultiSelectionEnabled(false);
        Frame.fileChooser.setDialogTitle("Load Datafile");
        Frame.fileChooser.addChoosableFileFilter(FileSaver.txtFilter);
        if (Frame.fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
            final Thread loadThread = new Thread(new Runnable()
            {
            public void run()
                {
                try
                    {
                    prgProgress.setIndeterminate(true);
                    prgProgress.setString("Loading datafileName: "
                                          + Frame.fileChooser.getSelectedFile().getName());
                    final SwingInvoker progressUpdater = new SwingInvoker(
                            new ProgressPanelUpdater(prgProgress, Long.MAX_VALUE), false);
                    lockdown(true);
                    // create a blank placeholder dataset since code doesn't deal well with no dataset
                    data = new Dataset(Main.defaultMissing, Main.defaultPairedAnalysis);
                    datafileName = "";
                    unfiltered = null;
                    loadedCollectors = null;
                    dataFileChange();
                    data.setPaired(chkPairedAnalysis.isSelected());
                    try
                        {
                        data.read(
                                new LineNumberReader(new FileReader(Frame.fileChooser
                                                                            .getSelectedFile())), progressUpdater);
                        }
                    catch (final IOException ex)
                        {
                        Utility.logException(ex);
                        JOptionPane.showMessageDialog(
                                Frame.this,
                                "Unable to open datafile '"
                                + Frame.fileChooser.getSelectedFile().toString()
                                + "' or unrecognized file format.\n\nMessage:\n"
                                + ex.getMessage(), "Datafile Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                        }
                    datafileName = Frame.fileChooser.getSelectedFile()
                            .getAbsolutePath();
                    unfiltered = null;
                    loadedCollectors = null;
                    dataFileChange();
                    }
                finally
                    {
                    prgProgress.setIndeterminate(false);
                    prgProgress.setString(null);
                    lockdown(false);
                    }
                } // end run
            }, "LoadDatasetThread");
            loadThread.start();
            } // if user chose file
        }

    public void cmdMaximizeEntropyDisplay_actionPerformed(final ActionEvent e)
        {
        final Component c = getContentPane().getComponent(0);
        if (c == scpMain)
            {
            getContentPane().remove(scpMain);
            tpnResults.setOrderedTabVisible(pnlEntropy, false);
            getContentPane().add(pnlEntropy, BorderLayout.CENTER);
            cmdMaximizeEntropy.setText("Restore");
            }
        else
            {
            tpnResults.setOrderedTabVisible(pnlEntropy, true);
            tpnResults.setSelectedComponent(pnlEntropy);
            getContentPane().add(scpMain, BorderLayout.CENTER);
            cmdMaximizeEntropy.setText("Maximize");
            }
        invalidate();
        validate();
        repaint();
        }

    public void cmdResetEntropyGraph_actionPerformed(final ActionEvent e)
        {
        spnEntropyGraphLineThickness.setValue(new Integer(
                Main.defaultEntropyGraphLineThickness));
        spnEntropyGraphTextSize.setValue(new Integer(
                Main.defaultEntropyGraphTextSize));
        final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                           .getSelectedItem().toString());
        if (component instanceof EntropyDisplay)
            {
            final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
            if (entropyDisplay.supportEntropyThreshold())
                {
                spnMinimumAbsoluteEntropy.setValue(entropyDisplay
                                                           .getDefaultMinimumAbsoluteEntropyPercent());
                }
            }
        }

    public void cmdRevertFilter_actionPerformed(final ActionEvent e)
        {
        data = unfiltered;
        unfiltered = null;
        rankerThread = null;
        dataFileChange();
        cmdRevertFilter.setEnabled(false);
        cmdRevert.setEnabled(false);
        prgFilterProgress.setValue(0);
        clearFilterTabs();
        }

    public void cmdRunAnalysis_actionPerformed(final ActionEvent e)
        {
        if ((analysis == null) || !analysis.isAlive())
            {
            loadedCollectors = null;
            long seed = ((Long) spnRandomSeed.getValue()).longValue();
            if (seed == -1)
                {
                seed = System.currentTimeMillis();
                System.out.println("Randomly generated random number generator seed: "
                                   + seed);
                }
            final int intervals = ((Integer) spnCrossValidationCount.getValue())
                    .intValue();
            int min = ((Integer) spnAttributeCountMin.getValue()).intValue();
            int max = ((Integer) spnAttributeCountMax.getValue()).intValue();
            final int topModelsLandscapeSize = ((Integer) spnTopModels.getValue())
                    .intValue();
            final boolean computeAllModelsLandscape = chkComputeAllModelsLandscape
                    .isSelected();
            TimerTask task = null;
            Runnable onModelEnd = null;
            final String search = cboSearchType.getSelectedItem().toString();
            final Pair<List<Dataset>, List<Dataset>> partition = data.partition(
                    intervals, new Random(seed));
            final AmbiguousCellStatus tiePriority = getTiePriority();
            if (search.equalsIgnoreCase("Exhaustive"))
                {
                long count = 0;
                for (int i = min; i <= max; ++i)
                    {
                    count += Utility.combinations(data.getCols() - 1, i);
                    }
                count *= intervals;
                onModelEnd = new SwingInvoker(new ProgressPanelUpdater(prgProgress,
                                                                       count), false);
                }
            else if (search.equalsIgnoreCase("Forced"))
                {
                if ((forced == null) || (forced.size() == 0))
                    {
                    JOptionPane.showMessageDialog(this, "Search type is forced but '"
                                                        + txtForcedAttributeCombination.getText()
                                                        + "' is not a list of attribute names or indices",
                                                  "Forced Attributes Not Understood", JOptionPane.WARNING_MESSAGE);
                    return;
                    }
                onModelEnd = new SwingInvoker(new ProgressPanelUpdater(prgProgress,
                                                                       intervals), false);
                min = max = forced.size();
                }
            else if (search.equalsIgnoreCase("Random"))
                {
                if (rdoEvaluations.isSelected())
                    {
                    onModelEnd = new SwingInvoker(new ProgressPanelUpdater(prgProgress,
                                                                           ((Number) spnEvaluations.getValue())
                                                                                   .intValue() * (max - min + 1)
                                                                           * intervals), false);
                    }
                else
                    {
                    @SuppressWarnings("unchecked")
                    final int units = ((Pair<String, Integer>) cboUnits.getSelectedItem())
                            .getSecond();
                    task = new TimerRunnableTask(new SwingInvoker(
                            new ProgressPanelUpdater(prgProgress,
                                                     (long) Math.ceil(((Number) spnRuntime.getValue())
                                                                              .doubleValue() * units)), false));
                    }
                }
            else if (search.equalsIgnoreCase("EDA"))
                {
                final int numEntities = ((SpinnerNumberModel) spnNumAgents.getModel())
                        .getNumber().intValue();
                final int numUpdates = ((SpinnerNumberModel) spnNumUpdates.getModel())
                        .getNumber().intValue();
                onModelEnd = new SwingInvoker(new ProgressPanelUpdater(prgProgress,
                                                                       numEntities * numUpdates * (max - min + 1) *
                                                                       intervals), false);
                }
            resetForm();
            if (search.equalsIgnoreCase("Exhaustive"))
                {
                analysis = new ExhaustiveAnalysisThread(data, partition, tiePriority,
                                                        Console.scoringMethod, seed, onModelEnd, onEndAttribute, onEnd,
                                                        Frame.parallel, topModelsLandscapeSize,
                                                        computeAllModelsLandscape,
                                                        min, max);
                }
            else if (search.equalsIgnoreCase("Forced"))
                {
                analysis = new ForcedAnalysisThread(data, partition, tiePriority,
                                                    Console.scoringMethod, seed, onModelEnd, onEndAttribute, onEnd,
                                                    Frame.parallel, topModelsLandscapeSize, computeAllModelsLandscape,
                                                    forced);
                }
            else if (search.equalsIgnoreCase("Random"))
                {
                if (rdoEvaluations.isSelected())
                    {
                    analysis = new FixedRandomAnalysisThread(data, partition,
                                                             tiePriority, Console.scoringMethod, seed, onModelEnd,
                                                             onEndAttribute, onEnd, Frame.parallel,
                                                             topModelsLandscapeSize,
                                                             computeAllModelsLandscape, min, max,
                                                             ((Number) spnEvaluations.getValue()).intValue());
                    }
                else if (rdoRuntime.isSelected())
                    {
                    @SuppressWarnings("unchecked")
                    final int units = ((Pair<String, Integer>) cboUnits.getSelectedItem())
                            .getSecond();
                    final long count = (long) Math.ceil(((Number) spnRuntime.getValue())
                                                                .doubleValue() * units * 1000.0 / (max - min + 1));
                    analysis = new TimedRandomAnalysisThread(data, partition,
                                                             tiePriority, Console.scoringMethod, seed, onModelEnd,
                                                             onEndAttribute, onEnd, Frame.parallel,
                                                             topModelsLandscapeSize,
                                                             computeAllModelsLandscape, min, max, count);
                    }
                }
            else if (search.equalsIgnoreCase("EDA"))
                {
                final RWRuntime currentRWRuntime = createExpertKnowledgeRuntime();
                final int numEntities = ((SpinnerNumberModel) spnNumAgents.getModel())
                        .getNumber().intValue();
                final int numUpdates = ((SpinnerNumberModel) spnNumUpdates.getModel())
                        .getNumber().intValue();
                analysis = new EDAAnalysisThread(data, partition, tiePriority,
                                                 Console.scoringMethod, seed, onModelEnd, onEndAttribute, onEnd,
                                                 Frame.parallel, topModelsLandscapeSize, computeAllModelsLandscape,
                                                 min, max, numEntities, numUpdates, currentRWRuntime);
                }
            lockdown(true);
            cmdRunAnalysis.setText("Stop Analysis");
            cmdRunAnalysis.setForeground(Color.RED);
            cmdRunAnalysis.setEnabled(true);
            final boolean displayTestingColumn = (intervals > 1);
            final boolean displayCvcColumn = displayTestingColumn
                                             && !search.equalsIgnoreCase(AnalysisFileManager.cfgValForced);
            setTestingAndCvcColumnsDisplay(displayTestingColumn, displayCvcColumn,
                                           analysis.getTiePriority());
            tpnResults.setOrderedTabVisible(pnlTopModelsLandscape,
                                            topModelsLandscapeSize > 0);
            tpnResults.setOrderedTabVisible(pnlAllModelsLandscape,
                                            computeAllModelsLandscape);
            afm = null;
            analysis.start();
            if (task != null)
                {
                tmrProgress = new Timer(true);
                tmrProgress.schedule(task, 1000, 1000);
                }
            }
        else
            {
            analysis.interrupt();
            }
        }

    public void cmdSaveAnalysis_actionPerformed(final ActionEvent e)
        {
        final PrintWriter out = FileSaver.openFileWriter(this, "Save Analysis",
                                                         FileSaver.fltText);
        if (out != null)
            {
            final AnalysisFileManager tempAfm = new AnalysisFileManager();
            if (unfiltered == null)
                {
                tempAfm.setAnalysis(data, datafileName, analysis.getMinAttr(),
                                    analysis.getMinAttr() + analysis.getCollectors().size() - 1,
                                    analysis.getCollectors(), analysis.getAllModelsLandscape(), forced,
                                    analysis.getSeed(), analysis.getTiePriority());
                }
            else
                {
                tempAfm.setAnalysis(unfiltered, data, rankerThread, datafileName,
                                    analysis.getMinAttr(), analysis.getMinAttr()
                                                           + analysis.getCollectors().size() - 1,
                                    analysis.getCollectors(), analysis.getAllModelsLandscape(), forced,
                                    analysis.getSeed(), analysis.getTiePriority());
                }
            switch (cboSearchType.getSelectedIndex())
                {
                case 0:
                    tempAfm.putCfg(AnalysisFileManager.cfgWrapper,
                                   AnalysisFileManager.cfgValExhaustive);
                    break;
                case 1:
                    tempAfm.putCfg(AnalysisFileManager.cfgWrapper,
                                   AnalysisFileManager.cfgValForced);
                    break;
                case 2:
                    tempAfm.putCfg(AnalysisFileManager.cfgWrapper,
                                   AnalysisFileManager.cfgValRandom);
                    if (rdoEvaluations.isSelected())
                        {
                        tempAfm.putCfg(AnalysisFileManager.cfgEvaluations, spnEvaluations
                                .getValue().toString());
                        }
                    else if (rdoRuntime.isSelected())
                        {
                        tempAfm.putCfg(AnalysisFileManager.cfgRuntime, spnRuntime
                                .getValue().toString());
                        tempAfm.putCfg(AnalysisFileManager.cfgRuntimeUnits, cboUnits
                                .getSelectedItem().toString());
                        }
                    break;
                }
            tempAfm.write(out);
            out.flush();
            }
        }

    public void cmdSaveEntropyDisplay_actionPerformed(final ActionEvent e)
        {
        final String name = cboEntropy.getSelectedItem().toString();
        final Component component = entropyPanelEntropyDisplay.get(name);
        if (component instanceof EntropyDisplay)
            {
            final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
            final Pair<File, FileFilter> ff = FileSaver.getSaveFile(this,
                                                                    "Save Entropy Graph", FileSaver.fltGraphics);
            if (ff == null)
                {
                return;
                }
            if (ff.getSecond() == FileSaver.epsFilter)
                {
                try
                    {
                    final Writer w = new FileWriter(ff.getFirst());
                    w.write(entropyDisplay.getEPSText());
                    w.flush();
                    w.close();
                    }
                catch (final IOException ex)
                    {
                    Utility.logException(ex);
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    }
                }
            else if (ff.getSecond() == FileSaver.jpgFilter)
                {
                try
                    {
                    ImageIO.write(entropyDisplay.getImage(), "jpeg", ff.getFirst());
                    }
                catch (final IOException ex)
                    {
                    Utility.logException(ex);
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    }
                }
            else if (ff.getSecond() == FileSaver.pngFilter)
                {
                try
                    {
                    ImageIO.write(entropyDisplay.getImage(), "png", ff.getFirst());
                    }
                catch (final IOException ex)
                    {
                    Utility.logException(ex);
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        else if (name.equals(DisplayType.Raw_Entropy_Values.toString()))
            {
            final Pair<File, FileFilter> ff = FileSaver.getSaveFile(this,
                                                                    "Save Raw Entropy Values", FileSaver.fltText);
            if (ff == null)
                {
                return;
                }
            try
                {
                FileSaver.saveText(txaRawEntropyValues.getText(), ff.getFirst());
                }
            catch (final IOException ex)
                {
                Utility.logException(ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                              JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    public void cmdViewDatafile_actionPerformed(final ActionEvent e)
        {
        frmDatafile.setVisible(true);
        frmDatafile.readDatafile(data);
        frmDatafile.toFront();
        }

    private RWRuntime createExpertKnowledgeRuntime()
        {
        ExpertKnowledge tempExpertKnowledege;
        ScalingMethod scalingMethod;
        WeightScheme weightScheme;
        double scalingParameter;
        int betaConstant;
        if (expertKnowledge != null)
            {
            tempExpertKnowledege = expertKnowledge;
            scalingMethod = getScalingMethod();
            weightScheme = getWeightScheme();
            betaConstant = ((SpinnerNumberModel) spnBeta.getModel()).getNumber()
                    .intValue();
            scalingParameter = getScalingParameter(scalingMethod);
            }
        else
            {
            tempExpertKnowledege = new ExpertKnowledge(data.getLabels());
            scalingMethod = null;
            scalingParameter = Double.NaN;
            weightScheme = null;
            betaConstant = 0;
            }
        final RWRuntime currentRWRuntime = tempExpertKnowledege.new RWRuntime(
                weightScheme, scalingMethod, scalingParameter,
                ((SpinnerNumberModel) spnAlpha.getModel()).getNumber().intValue(),
                betaConstant, ((SpinnerNumberModel) spnRetention.getModel())
                .getNumber().doubleValue());
        return currentRWRuntime;
        }

    private void dataFileChange()
        {
        if (frmDatafile.isShowing())
            {
            frmDatafile.readDatafile(data);
            }
        if (frmExpertKnowledge.isShowing())
            {
            frmExpertKnowledge.dispose();
            }
        final Integer numAttributes = new Integer(data.getCols() - 1);
        final Integer numInstances = data.getRows();
        lblDatafileValue.setText(datafileName);
        expertKnowledgeSetEnabled("", null);
        lblDatafileFiltered.setText(unfiltered == null ? "" : " (Filtered)");
        lblDatafileLoaded.setText(loadedCollectors == null ? ""
                                                           : " (Saved Analysis)");
        lblAttributesValue.setText(numAttributes.toString());
        lblInstancesValue.setText(numInstances.toString());
        // TODO: when polytomy happens, this will need to change
        resetForm();
        clearFilterTabs();
        if (numInstances > 0)
            {
            if (((Integer) spnAttributeCountMin.getValue()).compareTo(numAttributes) > 0)
                {
                spnAttributeCountMin.setValue(numAttributes);
                pnlWarning.warn("Attribute Count Minimum set to " + numAttributes
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnAttributeCountMin.getModel())
                    .setMaximum(numAttributes);
            if (((Integer) spnAttributeCountMax.getValue()).compareTo(numAttributes) > 0)
                {
                spnAttributeCountMax.setValue(numAttributes);
                pnlWarning.warn("Attribute Count Maximum set to " + numAttributes
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnAttributeCountMax.getModel())
                    .setMaximum(numAttributes);
            if (forced != null)
                {
                try
                    {
                    forced = new AttributeCombination(
                            txtForcedAttributeCombination.getText(), data.getLabels());
                    txtForcedAttributeCombination.setText(forced.getComboString());
                    }
                catch (final IllegalArgumentException ex)
                    {
                    forced = null;
                    }
                }
            if (Main.defaultForcedAttributeCombination != null)
                {
                Main.defaultForcedAttributeCombination.setLabels(data.getLabels());
                }
            ((SpinnerNumberModel) spnCrossValidationCount.getModel())
                    .setMaximum(numInstances);
            if (((Integer) spnCrossValidationCount.getValue())
                        .compareTo(numInstances) > 0)
                {
                spnCrossValidationCount.setValue(numInstances);
                pnlWarning.warn("Cross-Validation Count set to " + numInstances
                                + " to accomodate data set.");
                }
            if ((spnCriterionFilter.getModel() == snmTopN)
                && (((Number) snmTopN.getValue()).intValue() > numAttributes
                    .intValue()))
                {
                pnlWarning.warn("Filter Top N set to " + numAttributes
                                + " to accomodate data set.");
                }
            if (((Number) snmTopN.getValue()).intValue() > numAttributes.intValue())
                {
                snmTopN.setValue(numAttributes);
                }
            snmTopN.setMaximum(numAttributes);
            if (((Integer) spnReliefFSampleSize.getValue()).compareTo(numInstances) > 0)
                {
                spnReliefFSampleSize.setValue(numInstances);
                pnlWarning.warn("ReliefF Sample Size set to " + numInstances
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnReliefFSampleSize.getModel())
                    .setMaximum(numInstances);
            int maximumNeighbors;
            if (Console.reliefFRebalancingMethod == Console.ReliefFRebalancingMethod.OVERSAMPLE_MINORITY)
                {
                maximumNeighbors = data.getMajorityStatusCount() - 1;
                }
            else
                {
                maximumNeighbors = data.getMinorityStatusCount() - 1;
                }
            if (((Integer) spnReliefFNeighbors.getValue())
                        .compareTo(maximumNeighbors) > 0)
                {
                spnReliefFNeighbors.setValue(maximumNeighbors);
                pnlWarning.warn("ReliefF Neighbors set to " + maximumNeighbors
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnReliefFNeighbors.getModel())
                    .setMaximum(maximumNeighbors);
            if (((Integer) spnTuRFSampleSize.getValue()).compareTo(numInstances) > 0)
                {
                spnTuRFSampleSize.setValue(numInstances);
                pnlWarning.warn("TuRF Sample Size set to " + numInstances
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnTuRFSampleSize.getModel())
                    .setMaximum(numInstances);
            if (((Integer) spnTuRFNeighbors.getValue()).compareTo(maximumNeighbors) > 0)
                {
                spnTuRFNeighbors.setValue(maximumNeighbors);
                pnlWarning.warn("TuRF Neighbors set to " + maximumNeighbors
                                + " to accomodate data set.");
                }
            ((SpinnerNumberModel) spnTuRFNeighbors.getModel())
                    .setMaximum(maximumNeighbors);
            chkPairedAnalysis.setEnabled(data.canBePaired());
            if (!data.canBePaired() && chkPairedAnalysis.isSelected())
                {
                chkPairedAnalysis.setSelected(false);
                pnlWarning.warn("Paired Analysis set to false"
                                + " to accomodate data set.");
                }
            pnlGraphicalModel.setData(data);
            }
        lblRatioValue.setText((numInstances > 0) ? Main.modelTextNumberFormat
                .format(data.getRatio()) : "");
        pnlGraphicalModel.setModel(null);
        setModelDependentItemsEnabled(false);
        pnlGraphicalModel.setEnabled(false);
        pnlDatafileTable.readDatafile(data);
        pnlConstruct.setData(data);
        pnlAdjust.setData(data);
        setDataDependentItemsEnabled(datafileName.length() > 0);
        }

    /**
     * Overridden so that when the program is closed, all the threads and windows close with it.
     */
    @Override
    public void dispose() {
    System.exit(0);
    // frmDatafile.dispose();
    // frmExpertKnowledge.dispose();
    // if ((analysis != null) && analysis.isAlive()) {
    // analysis.interrupt();
    // }
    // if ((rankerThread != null) && rankerThread.isAlive()) {
    // rankerThread.interrupt();
    // }
    // super.dispose();
    }

    private void expertKnowledgeSetEnabled(final String expertKnowledgeFileName,
                                           final ExpertKnowledge pExpertKnowledge)
        {
        lblExpertKnowledgeValue.setText(expertKnowledgeFileName);
        expertKnowledge = pExpertKnowledge;
        spnExponentialTheta.setValue(Main.defaultEDAExponentialTheta);
        if (pExpertKnowledge != null)
            {
            rdoLinearKnowledge.setSelected(true);
            rdoFitnessProportional.setSelected(true);
            cmdViewExpertKnowledge.setEnabled(true);
            }
        else
            {
            spnBeta.setEnabled(false);
            rdoExpertKnowledgeWeightingSchemeGhost.setSelected(true);
            rdoExpertKnowledgeScalingMethodGhost.setSelected(true);
            spnLinearMaxPercent.setValue(Main.defaultEDAPercentMaxAttributeRange);
            cmdViewExpertKnowledge.setEnabled(false);
            }
        } // end expertKnowledgeSetEnabled()

    @SuppressWarnings("unchecked")
    private Console.AmbiguousCellStatus getComboAmbiguousStatus(final int index) {
    final Console.AmbiguousCellStatus cboItemTiePriority = ((Pair<String, Console.AmbiguousCellStatus>)
                                                                    cboAmbiguousCellStatuses
            .getItemAt(index)).getSecond();
    return cboItemTiePriority;
    }

    public long getRandomSeed()
        {
        final long seed = ((Long) spnRandomSeed.getValue()).longValue();
        return seed;
        }

    private ScalingMethod getScalingMethod()
        {
        ScalingMethod scalingMethod = null;
        for (final AbstractButton abstractButton : new IterableEnumeration<AbstractButton>(
                bgrExpertKnowledgeScalingMethod.getElements()))
            {
            if (abstractButton.isSelected())
                {
                scalingMethod = ScalingMethod
                        .getScalingMethod(abstractButton.getText());
                break;
                }
            }
        return scalingMethod;
        }

    private double getScalingParameter(final ScalingMethod scalingMethod)
        {
        double scalingParameter;
        switch (scalingMethod)
            {
            case EXPONENTIAL:
                scalingParameter = ((SpinnerNumberModel) spnExponentialTheta.getModel())
                        .getNumber().doubleValue();
                break;
            case LINEAR:
                scalingParameter = ((SpinnerNumberModel) spnLinearMaxPercent.getModel())
                                           .getNumber().intValue() / 100.0;
                break;
            default:
                throw new RuntimeException("unhandled scalingMethod: " + scalingMethod);
                // break;
            }
        return scalingParameter;
        }

    public Console.AmbiguousCellStatus getTiePriority()
        {
        @SuppressWarnings("unchecked")
        final Console.AmbiguousCellStatus tieValue = ((Pair<String, Console.AmbiguousCellStatus>)
                                                                      cboAmbiguousCellStatuses
                .getSelectedItem()).getSecond();
        return tieValue;
        }

    private WeightScheme getWeightScheme()
        {
        WeightScheme weightScheme = null;
        for (final AbstractButton abstractButton : new IterableEnumeration<AbstractButton>(
                bgrExpertKnowledgeWeightingScheme.getElements()))
            {
            if (abstractButton.isSelected())
                {
                weightScheme = WeightScheme.getWeightScheme(abstractButton.getText());
                break;
                }
            }
        return weightScheme;
        }

    private void init()
        {
        jbInit();
        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        memMon.setDaemon(true);
        memMon.start();
        }

    private boolean isDefaultConfig()
        {
        return isDefaultConfig(false);
        }

    private boolean isDefaultConfig(final boolean reset)
        {
        if (!spnRandomSeed.getValue().equals(new Long(Main.defaultRandomSeed)))
            {
            if (reset)
                {
                spnRandomSeed.setValue(Main.defaultRandomSeed);
                }
            else
                {
                return false;
                }
            }
        if (!spnAttributeCountMin.getValue().equals(
                new Integer(Main.defaultAttributeCountMin)))
            {
            if (reset)
                {
                spnAttributeCountMin.setValue(Main.defaultAttributeCountMin);
                }
            else
                {
                return false;
                }
            }
        if (!spnAttributeCountMax.getValue().equals(
                new Integer(Main.defaultAttributeCountMax)))
            {
            if (reset)
                {
                spnAttributeCountMax.setValue(Main.defaultAttributeCountMax);
                }
            else
                {
                return false;
                }
            }
        if ((Main.defaultForcedAttributeCombination == null)
            && (txtForcedAttributeCombination.getText().length() > 0))
            {
            if (reset)
                {
                txtForcedAttributeCombination.setText("");
                }
            else
                {
                return false;
                }
            }
        if (chkPairedAnalysis.isSelected() != Main.defaultPairedAnalysis)
            {
            if (reset)
                {
                chkPairedAnalysis.setSelected(Main.defaultPairedAnalysis);
                }
            else
                {
                return false;
                }
            }
        if ((Integer) spnTopModels.getValue() != Main.defaultTopModelsLandscapeSize)
            {
            if (reset)
                {
                spnTopModels.setValue(Main.defaultTopModelsLandscapeSize);
                }
            else
                {
                return false;
                }
            }
        if (chkComputeAllModelsLandscape.isSelected() != Main.defaultComputeAllModelsLandscape)
            {
            if (reset)
                {
                chkComputeAllModelsLandscape
                        .setSelected(Main.defaultComputeAllModelsLandscape);
                }
            else
                {
                return false;
                }
            }
        if (!rdoAmbiguousCellCriteriaTieCells.isSelected())
            {
            if (reset)
                {
                rdoAmbiguousCellCriteriaTieCells.setSelected(true);
                }
            else
                {
                return false;
                }
            }
        if (getComboAmbiguousStatus(cboAmbiguousCellStatuses.getSelectedIndex()) != Main.defaultAmbiguousCellStatus)
            {
            if (reset)
                {
                setComboAmbiguousStatus(Main.defaultAmbiguousCellStatus);
                }
            else
                {
                return false;
                }
            }
        if (cboSearchType.getSelectedIndex() != Main.defaultSearchTypeIndex)
            {
            if (reset)
                {
                cboSearchType.setSelectedIndex(Main.defaultSearchTypeIndex);
                }
            else
                {
                return false;
                }
            }
        if (rdoEvaluations.isSelected() != Main.defaultIsRandomSearchEvaluations)
            {
            if (reset)
                {
                rdoEvaluations.setSelected(Main.defaultIsRandomSearchEvaluations);
                }
            else
                {
                return false;
                }
            }
        if (rdoRuntime.isSelected() != Main.defaultIsRandomSearchRuntime)
            {
            if (reset)
                {
                rdoRuntime.setSelected(Main.defaultIsRandomSearchRuntime);
                }
            else
                {
                return false;
                }
            }
        if (!spnEvaluations.getValue().equals(
                new Integer(Main.defaultRandomSearchEvaluations)))
            {
            if (reset)
                {
                spnEvaluations.setValue(Main.defaultRandomSearchEvaluations);
                }
            else
                {
                return false;
                }
            }
        if (!spnRuntime.getValue().equals(
                new Double(Main.defaultRandomSearchRuntime)))
            {
            if (reset)
                {
                spnRuntime.setValue(Main.defaultRandomSearchRuntime);
                }
            else
                {
                return false;
                }
            }
        if (cboUnits.getSelectedIndex() != Main.defaultRandomSearchRuntimeUnitsIndex)
            {
            if (reset)
                {
                cboUnits.setSelectedIndex(Main.defaultRandomSearchRuntimeUnitsIndex);
                }
            else
                {
                return false;
                }
            }
        if (Main.defaultForcedAttributeCombination != null)
            {
            try
                {
                final AttributeCombination attributes = new AttributeCombination(
                        txtForcedAttributeCombination.getText(), data.getLabels());
                if (!attributes.equals(Main.defaultForcedAttributeCombination))
                    {
                    if (reset)
                        {
                        txtForcedAttributeCombination
                                .setText(Main.defaultForcedAttributeCombination.toString());
                        }
                    else
                        {
                        return false;
                        }
                    }
                }
            catch (final IllegalArgumentException ex)
                {
                return false;
                }
            }
        else if (txtForcedAttributeCombination.getText().length() != 0)
            {
            if (reset)
                {
                txtForcedAttributeCombination.setText("");
                }
            else
                {
                return false;
                }
            }
        if (!spnCrossValidationCount.getValue().equals(
                new Integer(Main.defaultCrossValidationCount)))
            {
            if (reset)
                {
                spnCrossValidationCount.setValue(Main.defaultCrossValidationCount);
                }
            else
                {
                return false;
                }
            }
        if (reset)
            {
            final boolean isDefault = isDefaultConfig();
            if (!isDefault)
                {
                System.out.println("isDefaultConfig with reset=true failed!");
                }
            return isDefault;
            }
        return true;
        }

    private boolean isDefaultEntropyGraph()
        {
        boolean returnValue = (((Number) spnEntropyGraphLineThickness.getValue())
                                       .intValue() == Main.defaultEntropyGraphLineThickness)
                              && (((Number) spnEntropyGraphTextSize.getValue()).intValue() == Main
                .defaultEntropyGraphTextSize);
        if (returnValue)
            {
            final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                               .getSelectedItem().toString());
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                if (entropyDisplay.supportEntropyThreshold())
                    {
                    returnValue = entropyDisplay.getMinimumAbsoluteEntropyPercent() == entropyDisplay
                            .getDefaultMinimumAbsoluteEntropyPercent();
                    }
                }
            }
        return returnValue;
        }

    private boolean isDefaultFilter()
        {
        return isDefaultFilter(false);
        }

    private boolean isDefaultFilter(final boolean reset)
        {
        if (cboFilter.getSelectedIndex() != Main.defaultFilterIndex)
            {
            if (reset)
                {
                cboFilter.setSelectedIndex(Main.defaultFilterIndex);
                }
            else
                {
                return false;
                }
            }
        if (cboCriterion.getSelectedIndex() != Main.defaultCriterionIndex)
            {
            if (reset)
                {
                cboCriterion.setSelectedIndex(Main.defaultCriterionIndex);
                }
            else
                {
                return false;
                }
            }
        if (spnCriterionFilter.getModel() != snmTopN)
            {
            if (reset)
                {
                spnCriterionFilter.setModel(snmTopN);
                }
            else
                {
                return false;
                }
            }
        if (chkChiSquaredPValue.isSelected() != Main.defaultChiSquaredPValue)
            {
            if (reset)
                {
                chkChiSquaredPValue.setSelected(Main.defaultChiSquaredPValue);
                }
            else
                {
                return false;
                }
            }
        if (((Number) snmTopN.getValue()).intValue() != Main.defaultCriterionFilterTopN)
            {
            if (reset)
                {
                snmTopN.setValue(Main.defaultCriterionFilterTopN);
                }
            else
                {
                return false;
                }
            }
        if (((Number) snmTopPct.getValue()).doubleValue() != Main.defaultCriterionFilterTopPct)
            {
            if (reset)
                {
                snmTopPct.setValue(Main.defaultCriterionFilterTopPct);
                }
            else
                {
                return false;
                }
            }
        if (((Number) snmThreshold.getValue()).doubleValue() != Main.defaultCriterionFilterThreshold)
            {
            if (reset)
                {
                snmThreshold.setValue(Main.defaultCriterionFilterThreshold);
                }
            else
                {
                return false;
                }
            }
        if (chkReliefFWholeDataset.isSelected() != Main.defaultReliefFWholeDataset)
            {
            if (reset)
                {
                chkReliefFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnReliefFNeighbors.getValue()).intValue() != Main.defaultReliefFNeighbors)
            {
            if (reset)
                {
                spnReliefFNeighbors.setValue(Main.defaultReliefFNeighbors);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnReliefFSampleSize.getValue()).intValue() != Main.defaultReliefFSampleSize)
            {
            if (reset)
                {
                spnReliefFSampleSize.setValue(Main.defaultReliefFSampleSize);
                }
            else
                {
                return false;
                }
            }
        if (chkTuRFWholeDataset.isSelected() != Main.defaultReliefFWholeDataset)
            {
            if (reset)
                {
                chkTuRFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnTuRFNeighbors.getValue()).intValue() != Main.defaultReliefFNeighbors)
            {
            if (reset)
                {
                spnTuRFNeighbors.setValue(Main.defaultReliefFNeighbors);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnTuRFSampleSize.getValue()).intValue() != Main.defaultReliefFSampleSize)
            {
            if (reset)
                {
                spnTuRFSampleSize.setValue(Main.defaultReliefFSampleSize);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnTuRFPercent.getValue()).intValue() != Main.defaultTuRFPct)
            {
            if (reset)
                {
                spnTuRFPercent.setValue(Main.defaultTuRFPct);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnSURFnTuRFPercent.getValue()).intValue() != Main.defaultTuRFPct)
            {
            if (reset)
                {
                spnSURFnTuRFPercent.setValue(Main.defaultTuRFPct);
                }
            else
                {
                return false;
                }
            }
        if (((Number) spnSURFStarnTuRFPercent.getValue()).intValue() != Main.defaultTuRFPct)
            {
            if (reset)
                {
                spnSURFStarnTuRFPercent.setValue(Main.defaultTuRFPct);
                }
            else
                {
                return false;
                }
            }
        if (reset)
            {
            final boolean isDefault = isDefaultFilter();
            if (!isDefault)
                {
                System.out.println("isDefaultFilter with reset=true failed!");
                }
            return isDefault;
            }
        return true;
        }

    private void jbInit()
        {
        setTitle("Multifactor Dimensionality Reduction "
                 + MDRProperties.get("version") + " " + MDRProperties.get("releaseType"));
        getContentPane().setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(new Dimension(700, 600));
        lblLogo.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                ClassLoader.getSystemResource("images/mdrlogo.png"))));
        lblLogo.addMouseListener(new Frame_lblLogo_mouseAdapter(this));
        lblLogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLogo
                .setToolTipText("http://www.multifactordimensionalityreduction.org/");
        setIconImage(Toolkit.getDefaultToolkit().getImage(
                ClassLoader.getSystemResource("images/mdriconsm.png")));
        pnlMain.setPreferredSize(new Dimension(getSize().width - 2,
                                               getSize().height - 2));
        scpMain.setBorder(null);
        pnlConstruct.setFont(Frame.font);
        {
        class PanelConstructChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                dataFileChange();
                }
            }
        pnlConstruct.addChangeListener(new PanelConstructChangeListener());
        }
        {
        class PanelAdjustChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                data = pnlAdjust.getData();
                dataFileChange();
                }
            }
        pnlAdjust.addChangeListener(new PanelAdjustChangeListener());
        }
        scpBestModel.setBorder(BorderFactory.createLoweredBevelBorder());
        txaBestModel.setEditable(false);
        txaBestModel.setFont(Frame.fontFixed);
        chkBestModelVerbose.setText("Verbose");
        chkBestModelVerbose.setFont(Frame.fontFixed);
        {
        class VerboseCheckboxActionListener implements ActionListener
            {
            public void actionPerformed(final ActionEvent e)
                {
                tblSummaryTable_selectionChanged(new ListSelectionEvent(this, 0, 0,
                                                                        false));
                }
            }
        chkBestModelVerbose
                .addActionListener(new VerboseCheckboxActionListener());
        chkCVResultsVerbose
                .addActionListener(new VerboseCheckboxActionListener());
        }
        chkCVResultsVerbose.setText("Verbose");
        chkCVResultsVerbose.setFont(Frame.fontFixed);
        cmdBestModelSave.setEnabled(false);
        cmdBestModelSave.setText("Save");
        cmdBestModelSave
                .addActionListener(new Frame_cmdBestModelSave_actionAdapter(this));
        cmdBestModelSave.setFont(Frame.font);
        cmdCVResultsSave.setEnabled(false);
        cmdCVResultsSave.setText("Save");
        cmdCVResultsSave
                .addActionListener(new Frame_cmdCVResultsSave_actionAdapter(this));
        cmdCVResultsSave.setFont(Frame.font);
        scpCVResults.setBorder(BorderFactory.createLoweredBevelBorder());
        txaCVResults.setEditable(false);
        txaCVResults.setFont(Frame.fontFixed);
        prgProgress.setTitleFont(Frame.fontBold);
        pnlSummaryTable.setMinimumSize(new Dimension(16, 125));
        pnlSummaryTable.setPreferredSize(new Dimension(0, 125));
        pnlSummaryTable.setTitle("Summary Table");
        pnlSummaryTable.setTitleFont(Frame.fontBold);
        scpSummaryTable.setBorder(BorderFactory.createLoweredBevelBorder());
        pnlDatafileInformation.setTitle("Datafile Information");
        pnlDatafileInformation.setTitleFont(Frame.fontBold);
        cmdRunAnalysis.setText("Run Analysis");
        cmdRunAnalysis.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdRunAnalysis_actionPerformed(e);
            }
        });
        cmdRunAnalysis.setEnabled(false);
        cmdRunAnalysis.setFont(Frame.font);
        cmdLoadAnalysis.setText("Load Analysis");
        cmdLoadAnalysis.addActionListener(new Frame_cmdLoadAnalysis_actionAdapter(
                this));
        cmdLoadAnalysis.setEnabled(true);
        cmdLoadAnalysis.setFont(Frame.font);
        cmdSaveAnalysis.setText("Save Analysis");
        cmdSaveAnalysis.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdSaveAnalysis_actionPerformed(e);
            }
        });
        cmdSaveAnalysis.setEnabled(false);
        cmdSaveAnalysis.setFont(Frame.font);
        cmdLoadDatafile.setText("Load Datafile");
        cmdLoadDatafile.addActionListener(new Frame_cmdLoadDatafile_actionAdapter(
                this));
        cmdLoadDatafile.setFont(Frame.font);
        cmdViewDatafile.setText("View Datafile");
        cmdViewDatafile.addActionListener(new Frame_cmdViewDatafile_actionAdapter(
                this));
        cmdViewDatafile.setEnabled(false);
        cmdViewDatafile.setFont(Frame.font);
        lblDatafile.setText("Datafile:");
        lblDatafile.setFont(Frame.font);
        lblDatafileValue.setFont(Frame.font);
        lblAttributes.setText("Attributes:");
        lblAttributes.setFont(Frame.font);
        lblAttributesValue.setFont(Frame.font);
        lblInstances.setText("Instances:");
        lblInstances.setFont(Frame.font);
        lblInstancesValue.setFont(Frame.font);
        lblUsedMemLabel.setFont(Frame.font);
        lblUsedMem.setFont(Frame.font);
        lblMaxMemLabel.setFont(Frame.font);
        lblMaxMem.setFont(Frame.font);
        lblTotalMemLabel.setFont(Frame.font);
        lblTotalMem.setFont(Frame.font);
        /* Create Configuration Tab frame */
        pnlConfiguration = new JPanel(new GridBagLayout());
        /* Build AnalysisConfiguration subpanel */
        pnlAnalysisConfiguration = new TitledPanel(new GridBagLayout());
        pnlAnalysisConfiguration.setTitle("Analysis Configuration");
        pnlAnalysisConfiguration.setTitleFont(Frame.fontBold);
        /* Random Seed Label and Spinner */
        lblRandomSeed = new JLabel("Random Seed:");
        lblRandomSeed.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblRandomSeed, new GridBagConstraints(0, 0, 1,
                                                                           1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                           GridBagConstraints.NONE,
                                                                           new Insets(0, 5, 5, 0), 0, 0));
        spnRandomSeed = new JSpinner(new SpinnerNumberModel(new Long(
                Main.defaultRandomSeed), new Long(Main.minimumRandomSeed), null,
                                                            new Long(1)));
        spnRandomSeed.setMinimumSize(new Dimension(60, 20));
        spnRandomSeed.setPreferredSize(new Dimension(60, 20));
        spnRandomSeed.addChangeListener(FrameChangeAdapter);
        spnRandomSeed.setFont(Frame.font);
        pnlAnalysisConfiguration.add(spnRandomSeed, new GridBagConstraints(1, 0, 3,
                                                                           1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                           GridBagConstraints.NONE,
                                                                           new Insets(0, 5, 5, 0), 0, 0));
        /* Attribute Count Range Labels and Spinners */
        lblAttributeCountRange = new JLabel("Attribute Count Range:");
        lblAttributeCountRange.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblAttributeCountRange,
                                     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        spnAttributeCountMin = new JSpinner(new SpinnerNumberModel(new Integer(
                Main.defaultAttributeCountMin), new Integer(1), null, new Integer(1)));
        spnAttributeCountMin.setMinimumSize(new Dimension(60, 20));
        spnAttributeCountMin.setPreferredSize(new Dimension(60, 20));
        spnAttributeCountMin
                .addChangeListener(new Frame_spnAttributeCount_changeAdapter(this));
        spnAttributeCountMin.addChangeListener(FrameChangeAdapter);
        spnAttributeCountMin.setFont(Frame.font);
        pnlAnalysisConfiguration.add(spnAttributeCountMin, new GridBagConstraints(
                1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 5, 5, 0), 0, 0));
        lblAttributeCountRangeColon = new JLabel(":");
        lblAttributeCountRangeColon.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblAttributeCountRangeColon,
                                     new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        spnAttributeCountMax = new JSpinner(new SpinnerNumberModel(new Integer(
                Main.defaultAttributeCountMax), new Integer(1), null, new Integer(1)));
        spnAttributeCountMax.setMinimumSize(new Dimension(60, 20));
        spnAttributeCountMax.setPreferredSize(new Dimension(60, 20));
        spnAttributeCountMax
                .addChangeListener(new Frame_spnAttributeCount_changeAdapter(this));
        spnAttributeCountMax.addChangeListener(FrameChangeAdapter);
        spnAttributeCountMax.setFont(Frame.font);
        pnlAnalysisConfiguration.add(spnAttributeCountMax, new GridBagConstraints(
                3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 5, 5, 5), 0, 0));
        /* Cross-Validation Count Label and Spinner */
        lblCrossValidationCount = new JLabel("Cross-Validation Count:");
        lblCrossValidationCount.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblCrossValidationCount,
                                     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        spnCrossValidationCount = new JSpinner(
                new SpinnerNumberModel(new Integer(Main.defaultCrossValidationCount),
                                       new Integer(1), null, new Integer(1)));
        spnCrossValidationCount.setMinimumSize(new Dimension(60, 20));
        spnCrossValidationCount.setPreferredSize(new Dimension(60, 20));
        spnCrossValidationCount.addChangeListener(FrameChangeAdapter);
        spnCrossValidationCount.setFont(Frame.font);
        pnlAnalysisConfiguration.add(spnCrossValidationCount,
                                     new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
        /* Compute Landscape Label and CheckBox */
        lblComputeAllModelsLandscape = new JLabel("Compute Fitness Landscape:");
        lblComputeAllModelsLandscape.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblComputeAllModelsLandscape,
                                     new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        chkComputeAllModelsLandscape.setFont(Frame.font);
        chkComputeAllModelsLandscape.addChangeListener(FrameChangeAdapter);
        pnlAnalysisConfiguration.add(chkComputeAllModelsLandscape,
                                     new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        final JLabel lblTopModelsLandscapeSize = new JLabel("Track Top Models:");
        lblTopModelsLandscapeSize.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblTopModelsLandscapeSize,
                                     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        spnTopModels = new JSpinner(new LandscapeSpinnerModel());
        spnTopModels.setMinimumSize(new Dimension(60, 20));
        spnTopModels.setPreferredSize(new Dimension(60, 20));
        spnTopModels.setFont(Frame.font);
        spnTopModels.addChangeListener(FrameChangeAdapter);
        pnlAnalysisConfiguration.add(spnTopModels, new GridBagConstraints(1, 4, 1,
                                                                          1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                          GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 5, 0), 0, 0));
        if (Main.isExperimental)
            {
            lblBestModelCriteria = new JLabel("Best Model Criteria:");
            lblBestModelCriteria.setFont(Frame.font);
            pnlAnalysisConfiguration.add(lblBestModelCriteria,
                                         new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
            cboBestModelCriteria = new JComboBox(new DefaultComboBoxModel(
                    Console.FitnessCriteriaOrder.getMenuItems()));
            cboBestModelCriteria.setMinimumSize(new Dimension(140, 20));
            cboBestModelCriteria.setPreferredSize(new Dimension(140, 20));
            cboBestModelCriteria.setFont(Frame.font);
            cboBestModelCriteria.addItemListener(new ItemListener()
            {
            public void itemStateChanged(final ItemEvent e)
                {
                Console.fitnessCriteriaOrder = Console.FitnessCriteriaOrder
                        .lookup(cboBestModelCriteria.getSelectedItem().toString());
                }
            });
            cboBestModelCriteria.addItemListener(new Frame_itemAdapter(this));
            pnlAnalysisConfiguration.add(cboBestModelCriteria,
                                         new GridBagConstraints(1, 5, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
            lblUseBestModelActualIntervals = new JLabel(
                    "Use Best Model Actual Intervals:");
            lblUseBestModelActualIntervals.setFont(Frame.font);
            pnlAnalysisConfiguration.add(lblUseBestModelActualIntervals,
                                         new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
            chkUseBestModelActualIntervals = new JCheckBox();
            chkUseBestModelActualIntervals.setFont(Frame.font);
            pnlAnalysisConfiguration.add(chkUseBestModelActualIntervals,
                                         new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
            chkUseBestModelActualIntervals
                    .setSelected(Console.useBestModelActualIntervals);
            chkUseBestModelActualIntervals.addItemListener(new ItemListener()
            {
            public void itemStateChanged(final ItemEvent e)
                {
                Console.useBestModelActualIntervals = chkUseBestModelActualIntervals
                        .isSelected();
                }
            });
            chkUseBestModelActualIntervals
                    .addItemListener(new Frame_itemAdapter(this));
            } // end if experimental
        /* Paired Analysis Label and CheckBox */
        lblPairedAnalysis = new JLabel("Paired Analysis:");
        lblPairedAnalysis.setFont(Frame.font);
        pnlAnalysisConfiguration.add(lblPairedAnalysis, new GridBagConstraints(4,
                                                                               0, 1, 1, 0.0, 0.0,
                                                                               GridBagConstraints.WEST,
                                                                               GridBagConstraints.NONE,
                                                                               new Insets(0, 5, 5, 0), 0, 0));
        chkPairedAnalysis
                .addActionListener(new Frame_chkPairedAnalysis_actionAdapter(this));
        chkPairedAnalysis.setFont(Frame.font);
        chkPairedAnalysis.addChangeListener(FrameChangeAdapter);
        pnlAnalysisConfiguration.add(chkPairedAnalysis, new GridBagConstraints(5,
                                                                               0, 1, 1, 0.0, 0.0,
                                                                               GridBagConstraints.WEST,
                                                                               GridBagConstraints.NONE,
                                                                               new Insets(0, 5, 5, 5), 0, 0));
        /* Ambiguous cell analysis */
        pnlAmbiguousCellAnalysis = new TitledPanel(new GridBagLayout());
        pnlAmbiguousCellAnalysis.setTitle("Ambiguous Cell Analysis");
        rdoAmbiguousCellCriteriaTieCells = new JRadioButton("Tie Cells", true);
        rdoAmbiguousCellCriteriaTieCells.setFont(Frame.font);
        pnlAmbiguousCellAnalysis.add(rdoAmbiguousCellCriteriaTieCells,
                                     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        rdoAmbiguousCellCriteriaFishersExact = new JRadioButton(
                "Fishers Exact Test", true);
        rdoAmbiguousCellCriteriaFishersExact.setFont(Frame.font);
        pnlAmbiguousCellAnalysis.add(rdoAmbiguousCellCriteriaFishersExact,
                                     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        bgrAmbiguousCellCriteria = new ButtonGroup();
        bgrAmbiguousCellCriteria.add(rdoAmbiguousCellCriteriaTieCells);
        bgrAmbiguousCellCriteria.add(rdoAmbiguousCellCriteriaFishersExact);
        bgrAmbiguousCellCriteria.setSelected(
                rdoAmbiguousCellCriteriaTieCells.getModel(), true);
        rdoAmbiguousCellCriteriaTieCells.addChangeListener(FrameChangeAdapter);
        final AmbiguousCellCriteriaActionListener ambiguousCellCriteriaActionListener = new
                AmbiguousCellCriteriaActionListener();
        rdoAmbiguousCellCriteriaTieCells
                .addActionListener(ambiguousCellCriteriaActionListener);
        rdoAmbiguousCellCriteriaFishersExact.addChangeListener(FrameChangeAdapter);
        rdoAmbiguousCellCriteriaFishersExact
                .addActionListener(ambiguousCellCriteriaActionListener);
        // lblTieCells = new JLabel("Tie Cells:");
        // lblTieCells.setFont(Frame.font);
        // pnlAmbiguousCellAnalysis.add(lblTieCells, new GridBagConstraints(0, 0, 1,
        // 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        // new Insets(0, 5, 5, 0), 0, 0));
        // final JLabel lblFisherExact = new JLabel("Fisher's Exact Test:");
        // lblFisherExact.setFont(Frame.font);
        // pnlAmbiguousCellAnalysis.add(lblFisherExact, new GridBagConstraints(0, 1,
        // 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        // new Insets(0, 5, 5, 0), 0, 0));
        spnFisherThreshold = new JSpinner(new SpinnerNumberModel(0.05, 0.01, 1.0,
                                                                 .01));
        spnFisherThreshold.setEnabled(false);
        spnFisherThreshold.setMinimumSize(new Dimension(60, 20));
        spnFisherThreshold.setPreferredSize(new Dimension(60, 20));
        spnFisherThreshold.setFont(Frame.font);
        spnFisherThreshold.addChangeListener(FrameChangeAdapter);
        spnFisherThreshold.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            Console.fishersThreshold = ((Double) spnFisherThreshold.getValue())
                    .floatValue();
            }
        });
        pnlAmbiguousCellAnalysis.add(spnFisherThreshold, new GridBagConstraints(1,
                                                                                1, 1, 1, 0.0, 0.0,
                                                                                GridBagConstraints.WEST,
                                                                                GridBagConstraints.NONE,
                                                                                new Insets(0, 5, 5, 0), 0, 0));
        lblAmbiguousCellAssignment = new JLabel("Ambiguous cell assignment:");
        lblAmbiguousCellAssignment.setFont(Frame.font);
        pnlAmbiguousCellAnalysis.add(lblAmbiguousCellAssignment,
                                     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        cboAmbiguousCellStatuses = new JComboBox();
        cboAmbiguousCellStatuses.setFont(Frame.font);
        cboAmbiguousCellStatuses
                .addItem(new DisplayPair<String, Console.AmbiguousCellStatus>(
                        Console.AmbiguousCellStatus.AFFECTED.getDisplayName(),
                        Console.AmbiguousCellStatus.AFFECTED));
        cboAmbiguousCellStatuses
                .addItem(new DisplayPair<String, Console.AmbiguousCellStatus>(
                        Console.AmbiguousCellStatus.UNAFFECTED.getDisplayName(),
                        Console.AmbiguousCellStatus.UNAFFECTED));
        cboAmbiguousCellStatuses
                .addItem(new DisplayPair<String, Console.AmbiguousCellStatus>(
                        Console.AmbiguousCellStatus.UNASSIGNED.getDisplayName(),
                        Console.AmbiguousCellStatus.UNASSIGNED));
        cboAmbiguousCellStatuses.addItemListener(new Frame_itemAdapter(this));
        pnlAmbiguousCellAnalysis.add(cboAmbiguousCellStatuses,
                                     new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
        /* Add AnalysisConfiguration Panel to Configuration Tab */
        pnlAnalysisConfiguration.add(pnlAmbiguousCellAnalysis,
                                     new GridBagConstraints(4, 1, 2, 4, 1.0, 0.0, GridBagConstraints.WEST,
                                                            GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        pnlConfiguration.add(pnlAnalysisConfiguration, new GridBagConstraints(0, 0,
                                                                              1, 1, 1.0, 0.0,
                                                                              GridBagConstraints.NORTHWEST,
                                                                              GridBagConstraints.HORIZONTAL,
                                                                              new Insets(5, 5, 0, 5), 0, 0));
        /* Create SearchMethodConfiguration subpanel frame */
        pnlSearchMethodConfiguration = new TitledPanel(new BorderLayout());
        pnlSearchMethodConfiguration.setTitle("Search Method Configuration");
        pnlSearchMethodConfiguration.setTitleFont(Frame.fontBold);
        /* Build SearchAlgorithm subpanel */
        pnlSearchAlgorithm = new JPanel(new GridBagLayout());
        /* Search Type Label and ComboBox */
        lblSearchType = new JLabel("Search Type:");
        lblSearchType.setFont(Frame.font);
        pnlSearchAlgorithm.add(lblSearchType, new GridBagConstraints(0, 0, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        cboSearchType = new JComboBox();
        cboSearchType.setFont(Frame.font);
        cboSearchType.addItem("Exhaustive");
        cboSearchType.addItem("Forced");
        cboSearchType.addItem("Random");
        cboSearchType.addItem("EDA");
        cboSearchType.addItemListener(new Frame_cboSearchType_itemAdapter(this));
        cboSearchType.addItemListener(new Frame_itemAdapter(this));
        pnlSearchAlgorithm.add(cboSearchType, new GridBagConstraints(1, 0, 1, 1,
                                                                     1.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 5, 5), 0, 0));
        /* Add SearchAlgorithm Panel to SearchMethodConfiguration Panel */
        pnlSearchMethodConfiguration.add(pnlSearchAlgorithm,
                                         java.awt.BorderLayout.NORTH);
        /* Create SearchOptions Frame */
        pnlSearchOptions = new JPanel(crdSearchOptions);
        /*
		 * Define Search Options 1) Exhaustive 2) Forced 3) Random 4) Estimation of Distribution Algorithm (EDA)
		 */
        /* ExhaustiveSearchOptions Panel */
        pnlExhaustiveSearchOptions = new JPanel(new GridBagLayout());
        pnlSearchOptions.add(pnlExhaustiveSearchOptions, "Exhaustive");
        /* ForcedSearchOptions Panel */
        pnlForcedSearchOptions = new JPanel(new GridBagLayout());
        /* ForcedAttributeCombination Label and TextBox */
        lblForcedAttributeCombination = new JLabel("Forced Attribute Combination:");
        lblForcedAttributeCombination.setFont(Frame.font);
        pnlForcedSearchOptions.add(lblForcedAttributeCombination,
                                   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                                                          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        txtForcedAttributeCombination = new JTextField();
        txtForcedAttributeCombination
                .setText(Main.defaultForcedAttributeCombination == null ? ""
                                                                        : Main.defaultForcedAttributeCombination
                        .getComboString());
        txtForcedAttributeCombination
                .addFocusListener(new Frame_txtForcedAttributeCombination_focusAdapter(
                        this));
        pnlForcedSearchOptions.add(txtForcedAttributeCombination,
                                   new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                                                          GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
        pnlSearchOptions.add(pnlForcedSearchOptions, "Forced");
        /* RandomSearchOptions Panel */
        pnlRandomSearchOptions = new JPanel(new GridBagLayout());
        /* Evaluations RadioButton and Spinner */
        rdoEvaluations = new JRadioButton("Evaluations:", true);
        rdoEvaluations.setFont(Frame.font);
        rdoEvaluations.addChangeListener(new Frame_rdoEvaluations_changeAdapter(
                this));
        rdoEvaluations.addChangeListener(FrameChangeAdapter);
        pnlRandomSearchOptions.add(rdoEvaluations, new GridBagConstraints(0, 0, 1,
                                                                          1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                          GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 5, 0), 0, 0));
        spnEvaluations = new JSpinner(new SpinnerNumberModel(new Integer(
                Main.defaultRandomSearchEvaluations), new Integer(1), new Integer(
                10000000), new Integer(100)));
        spnEvaluations.setMinimumSize(new Dimension(60, 20));
        spnEvaluations.setPreferredSize(new Dimension(60, 20));
        spnEvaluations.addChangeListener(FrameChangeAdapter);
        spnEvaluations.setFont(Frame.font);
        pnlRandomSearchOptions.add(spnEvaluations, new GridBagConstraints(1, 0, 1,
                                                                          1, 0.0, 0.0, GridBagConstraints.CENTER,
                                                                          GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 5, 0), 0, 0));
        /* Runtime RadioButton and Spinner */
        rdoRuntime = new JRadioButton("Runtime:");
        rdoRuntime.setFont(Frame.font);
        rdoRuntime.addChangeListener(FrameChangeAdapter);
        rdoRuntime.setEnabled(true);
        pnlRandomSearchOptions.add(rdoRuntime, new GridBagConstraints(0, 1, 1, 1,
                                                                      0.0, 0.0, GridBagConstraints.WEST,
                                                                      GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        spnRuntime = new JSpinner(new SpinnerNumberModel(new Double(
                Main.defaultRandomSearchRuntime), new Double(Double.MIN_VALUE),
                                                         new Double(Double.MAX_VALUE), new Double(1)));
        spnRuntime.setMinimumSize(new Dimension(60, 20));
        spnRuntime.setPreferredSize(new Dimension(60, 20));
        spnRuntime.addChangeListener(FrameChangeAdapter);
        spnRuntime.setFont(Frame.font);
        spnRuntime.setEnabled(false);
        pnlRandomSearchOptions.add(spnRuntime, new GridBagConstraints(1, 1, 1, 1,
                                                                      0.0, 0.0, GridBagConstraints.CENTER,
                                                                      GridBagConstraints.NONE,
                                                                      new Insets(0, 5, 5, 0), 0, 0));
        /* Units Label and Combobox */
        lblUnits = new JLabel("Units:");
        lblUnits.setFont(Frame.font);
        lblUnits.setEnabled(false);
        pnlRandomSearchOptions.add(lblUnits, new GridBagConstraints(2, 1, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.CENTER,
                                                                    GridBagConstraints.NONE,
                                                                    new Insets(0, 5, 5, 0), 0, 0));
        cboUnits = new JComboBox();
        cboUnits.setFont(Frame.font);
        cboUnits.addItem(new DisplayPair<String, Integer>("Seconds", 1));
        cboUnits.addItem(new DisplayPair<String, Integer>("Minutes", 1 * 60));
        cboUnits.addItem(new DisplayPair<String, Integer>("Hours", 1 * 60 * 60));
        cboUnits
                .addItem(new DisplayPair<String, Integer>("Days", 1 * 60 * 60 * 24));
        cboUnits.setSelectedIndex(1);
        cboUnits.addItemListener(new Frame_itemAdapter(this));
        cboUnits.setEnabled(false);
        pnlRandomSearchOptions.add(cboUnits, new GridBagConstraints(3, 1, 1, 1,
                                                                    1.0, 0.0, GridBagConstraints.WEST,
                                                                    GridBagConstraints.NONE, new Insets(
                0, 5, 5, 5), 0, 0));
        pnlSearchOptions.add(pnlRandomSearchOptions, "Random");
        /* EDASearchOptions Panel */
        final GridBagLayout pnlEDASearchOptionsLayout = new GridBagLayout();
        pnlEDASearchOptionsLayout.rowWeights = new double[]{0.1, 0.1, 0.1};
        pnlEDASearchOptionsLayout.rowHeights = new int[]{1, 1, 1};
        pnlEDASearchOptionsLayout.columnWeights = new double[]{0, 0, 0.1, 0, 1};
        pnlEDASearchOptionsLayout.columnWidths = new int[]{1, 1, 1, 1, 1};
        pnlEDASearchOptions = new JPanel(pnlEDASearchOptionsLayout);
        /* Number of Agents */
        lblNumAgents = new JLabel("Number of Agents:");
        lblNumAgents.setFont(Frame.font);
        pnlEDASearchOptions.add(lblNumAgents, new GridBagConstraints(0, 0, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.EAST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        spnNumAgents = new JSpinner(new SpinnerNumberModel(
                Main.defaultEDANumAgents, 1, null, 1));
        spnNumAgents.setMinimumSize(new Dimension(60, 20));
        spnNumAgents.setPreferredSize(new Dimension(60, 20));
        spnNumAgents.addChangeListener(FrameChangeAdapter);
        spnNumAgents.setFont(Frame.font);
        pnlEDASearchOptions.add(spnNumAgents, new GridBagConstraints(1, 0, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        /* Number of Updates */
        lblNumUpdates = new JLabel("Number of Updates:");
        lblNumUpdates.setFont(Frame.font);
        pnlEDASearchOptions.add(lblNumUpdates, new GridBagConstraints(2, 0, 1, 1,
                                                                      0.0, 0.0, GridBagConstraints.EAST,
                                                                      GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        spnNumUpdates = new JSpinner(new SpinnerNumberModel(new Integer(
                Main.defaultEDANumUpdates), new Integer(1), null, new Integer(50)));
        spnNumUpdates.setMinimumSize(new Dimension(60, 20));
        spnNumUpdates.setPreferredSize(new Dimension(60, 20));
        spnNumUpdates.addChangeListener(FrameChangeAdapter);
        spnNumUpdates.setFont(Frame.font);
        pnlEDASearchOptions.add(spnNumUpdates, new GridBagConstraints(3, 0, 1, 1,
                                                                      0.0, 0.0, GridBagConstraints.WEST,
                                                                      GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        // EDA Update Configuration Panel
        pnlEDAUpdate = new JPanel(new GridBagLayout());
        // Retention Constant
        lblRetention = new JLabel("Retention:");
        lblRetention.setFont(Frame.font);
        pnlEDAUpdate.add(lblRetention, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                              GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                              new Insets(0, 5, 5, 0), 0, 0));
        spnRetention = new JSpinner(new SpinnerNumberModel(
                Main.defaultEDARetention, 0.1, 1.0, 0.01));
        spnRetention.setMinimumSize(new Dimension(60, 20));
        spnRetention.setPreferredSize(new Dimension(60, 20));
        spnRetention.addChangeListener(FrameChangeAdapter);
        spnRetention.setFont(Frame.font);
        pnlEDAUpdate.add(spnRetention, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                              GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                              new Insets(0, 5, 5, 0), 0, 0));
        // Alpha Constant
        lblAlpha = new JLabel("Alpha:");
        lblAlpha.setFont(Frame.font);
        pnlEDAUpdate.add(lblAlpha, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                                          GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                          new Insets(0, 5, 5, 0), 0, 0));
        spnAlpha = new JSpinner(new SpinnerNumberModel(Main.defaultEDAAlpha, 0,
                                                       100, 1));
        spnAlpha.setMinimumSize(new Dimension(60, 20));
        spnAlpha.setPreferredSize(new Dimension(60, 20));
        spnAlpha.addChangeListener(FrameChangeAdapter);
        spnAlpha.setFont(Frame.font);
        pnlEDAUpdate.add(spnAlpha, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                          GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                          new Insets(0, 5, 5, 0), 0, 0));
        // Beta Constant
        lblBeta = new JLabel("Beta:");
        lblBeta.setFont(Frame.font);
        pnlEDAUpdate.add(lblBeta, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                                         GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                         new Insets(0, 5, 5, 0), 0, 0));
        spnBeta = new JSpinner(new SpinnerNumberModel(Main.defaultEDABeta, 0, 100,
                                                      1));
        spnBeta.setMinimumSize(new Dimension(60, 20));
        spnBeta.setPreferredSize(new Dimension(60, 20));
        spnBeta.addChangeListener(FrameChangeAdapter);
        spnBeta.setFont(Frame.font);
        pnlEDAUpdate.add(spnBeta, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0,
                                                         GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                         new Insets(0, 5, 5, 0), 0, 0));
        pnlEDASearchOptions.add(pnlEDAUpdate, new GridBagConstraints(0, 1,
                                                                     GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                                                     GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(0, 5, 5, 0),
                                                                     0, 0));
        /* Create Expert Knowledge Interpretation Titled Sub-panel */
        pnlEDAExpertKnowledge = new TitledPanel(new BorderLayout());
        pnlEDAExpertKnowledge.setTitle("Expert Knowledge");
        pnlEDAExpertKnowledge.setTitleFont(Frame.fontBold);
        // Create Panel to hold EK Configuration Information
        pnlEDAEKPanel = new JPanel(new GridBagLayout());
        // Expert Knowledge file loading/viewing
        pnlEDAEKInfo = new JPanel(new GridBagLayout());
        /* Expert Knowledge Upload */
        cmdLoadExpertKnowledge = new JButton("Load Scores");
        cmdLoadExpertKnowledge
                .addActionListener(new Frame_cmdLoadExpertKnowledge());
        cmdLoadExpertKnowledge.setFont(Frame.font);
        pnlEDAEKInfo.add(cmdLoadExpertKnowledge, new GridBagConstraints(0, 0, 1, 1,
                                                                        0.0, 0.0, GridBagConstraints.EAST,
                                                                        GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        lblExpertKnowledgeValue = new JLabel();
        lblExpertKnowledgeValue.setFont(Frame.font);
        pnlEDAEKInfo.add(lblExpertKnowledgeValue, new GridBagConstraints(1, 0,
                                                                         GridBagConstraints.REMAINDER, 1, 1, 0.0,
                                                                         GridBagConstraints.WEST,
                                                                         GridBagConstraints.NONE, new Insets(0, 5, 0,
                                                                                                             0), 0, 0));
        cmdViewExpertKnowledge = new JButton("View Scores");
        cmdViewExpertKnowledge.addActionListener(new cmdViewExpertKnowledge());
        cmdViewExpertKnowledge.setFont(Frame.font);
        pnlEDAEKInfo.add(cmdViewExpertKnowledge, new GridBagConstraints(0, 1, 1, 1,
                                                                        0.0, 0.0, GridBagConstraints.EAST,
                                                                        GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        // Add pnlEDAEKUpdate to pnlEDAExpertKnowledge panel
        pnlEDAEKPanel.add(pnlEDAEKInfo, new GridBagConstraints(0, 0,
                                                               GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                                               GridBagConstraints.WEST,
                                                               GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 0),
                                                               0, 0));
        // EDA Expert Knowledge Conversion Panel
        pnlEDAEKConv = new JPanel(new GridBagLayout());
        rdoFitnessProportional = new JRadioButton(
                WeightScheme.PROPORTIONAL.getDisplayName());
        rdoFitnessProportional.setFont(Frame.font);
        rdoFitnessProportional.addChangeListener(FrameChangeAdapter);
        pnlEDAEKConv.add(rdoFitnessProportional, new GridBagConstraints(0, 0, 1, 1,
                                                                        0.0, 0.0, GridBagConstraints.WEST,
                                                                        GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        rdoRankedSelection = new JRadioButton(WeightScheme.RANK.getDisplayName());
        rdoRankedSelection.setFont(Frame.font);
        {
        class WeightSchemeChangeListener implements ActionListener
            {
            public void actionPerformed(final ActionEvent e)
                {
                if (frmExpertKnowledge.isVisible())
                    {
                    createExpertKnowledgeRuntime();
                    frmExpertKnowledge.readDatafile(expertKnowledge);
                    }
                }
            }
        final WeightSchemeChangeListener weightSchemeChangeListener = new WeightSchemeChangeListener();
        rdoRankedSelection.addActionListener(weightSchemeChangeListener);
        rdoFitnessProportional.addActionListener(weightSchemeChangeListener);
        }
        rdoRankedSelection.addChangeListener(FrameChangeAdapter);
        pnlEDAEKConv.add(rdoRankedSelection, new GridBagConstraints(0, 1, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.WEST,
                                                                    GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        bgrExpertKnowledgeWeightingScheme = new ButtonGroup();
        bgrExpertKnowledgeWeightingScheme.add(rdoRankedSelection);
        bgrExpertKnowledgeWeightingScheme.add(rdoFitnessProportional);
        rdoExpertKnowledgeWeightingSchemeGhost = new JRadioButton("Ghost");
        rdoExpertKnowledgeWeightingSchemeGhost
                .addChangeListener(new ChangeListener()
                {
                public void stateChanged(final ChangeEvent e)
                    {
                    rdoRankedSelection
                            .setEnabled(!rdoExpertKnowledgeWeightingSchemeGhost
                                    .isSelected());
                    rdoFitnessProportional
                            .setEnabled(!rdoExpertKnowledgeWeightingSchemeGhost
                                    .isSelected());
                    }
                });
        rdoExpertKnowledgeWeightingSchemeGhost.setVisible(false);
        bgrExpertKnowledgeWeightingScheme
                .add(rdoExpertKnowledgeWeightingSchemeGhost);
        rdoLinearKnowledge = new JRadioButton("Linear");
        rdoLinearKnowledge.setFont(Frame.font);
        rdoLinearKnowledge.addChangeListener(FrameChangeAdapter);
        {
        class LinearKnowledgeChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                spnLinearMaxPercent.setEnabled(rdoLinearKnowledge.isSelected());
                lblLinearMax.setEnabled(rdoLinearKnowledge.isSelected());
                }
            }
        rdoLinearKnowledge.addChangeListener(new LinearKnowledgeChangeListener());
        }
        pnlEDAEKConv.add(rdoLinearKnowledge, new GridBagConstraints(1, 0, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.WEST,
                                                                    GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        lblLinearMax = new JLabel(" % Maximum Probability:");
        lblLinearMax.setFont(Frame.font);
        pnlEDAEKConv.add(lblLinearMax, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                                              GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                              new Insets(0, 5, 5, 0), 0, 0));
        spnLinearMaxPercent = new JSpinner(new SpinnerNumberModel(
                Main.defaultEDAPercentMaxAttributeRange, 0, 100, 1));
        spnLinearMaxPercent.setMinimumSize(new Dimension(60, 20));
        spnLinearMaxPercent.setPreferredSize(new Dimension(60, 20));
        spnLinearMaxPercent.addChangeListener(FrameChangeAdapter);
        {
        class LinearMaxChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                if (frmExpertKnowledge.isVisible())
                    {
                    createExpertKnowledgeRuntime();
                    frmExpertKnowledge.readDatafile(expertKnowledge);
                    }
                }
            }
        spnLinearMaxPercent.addChangeListener(new LinearMaxChangeListener());
        }
        spnLinearMaxPercent.setFont(Frame.font);
        pnlEDAEKConv.add(spnLinearMaxPercent, new GridBagConstraints(1, 2, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        rdoExponentialKnowledge = new JRadioButton("Exponential");
        rdoExponentialKnowledge.setFont(Frame.font);
        rdoExponentialKnowledge.addChangeListener(FrameChangeAdapter);
        {
        class ExponentialKnowledgeChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                spnExponentialTheta.setEnabled(rdoExponentialKnowledge.isSelected());
                lblExponentialTheta.setEnabled(rdoExponentialKnowledge.isSelected());
                }
            }
        rdoExponentialKnowledge
                .addChangeListener(new ExponentialKnowledgeChangeListener());
        }
        pnlEDAEKConv.add(rdoExponentialKnowledge, new GridBagConstraints(1, 1, 1,
                                                                         1, 0.0, 0.0, GridBagConstraints.WEST,
                                                                         GridBagConstraints.NONE,
                                                                         new Insets(0, 5, 0, 0), 0, 0));
        {
        class ScalingMethodChangeListener implements ActionListener
            {
            public void actionPerformed(final ActionEvent e)
                {
                if (frmExpertKnowledge.isVisible())
                    {
                    createExpertKnowledgeRuntime();
                    frmExpertKnowledge.readDatafile(expertKnowledge);
                    }
                }
            }
        final ScalingMethodChangeListener scalingMethodChangeListener = new ScalingMethodChangeListener();
        rdoLinearKnowledge.addActionListener(scalingMethodChangeListener);
        rdoExponentialKnowledge.addActionListener(scalingMethodChangeListener);
        }
        bgrExpertKnowledgeScalingMethod = new ButtonGroup();
        bgrExpertKnowledgeScalingMethod.add(rdoLinearKnowledge);
        bgrExpertKnowledgeScalingMethod.add(rdoExponentialKnowledge);
        rdoExpertKnowledgeScalingMethodGhost = new JRadioButton("Ghost");
        rdoExpertKnowledgeScalingMethodGhost.setVisible(false);
        bgrExpertKnowledgeScalingMethod.add(rdoExpertKnowledgeScalingMethodGhost);
        rdoExpertKnowledgeScalingMethodGhost
                .addChangeListener(new ChangeListener()
                {
                public void stateChanged(final ChangeEvent e)
                    {
                    rdoExponentialKnowledge
                            .setEnabled(!rdoExpertKnowledgeScalingMethodGhost.isSelected());
                    rdoLinearKnowledge.setEnabled(!rdoExpertKnowledgeScalingMethodGhost
                            .isSelected());
                    }
                });
        lblExponentialTheta = new JLabel("Theta:");
        lblExponentialTheta.setFont(Frame.font);
        pnlEDAEKConv.add(lblExponentialTheta, new GridBagConstraints(0, 3, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        spnExponentialTheta = new JSpinner(new SpinnerNumberModel(
                Main.defaultEDAExponentialTheta, 0.0, 1.0, 0.01));
        spnExponentialTheta.setMinimumSize(new Dimension(60, 20));
        spnExponentialTheta.setPreferredSize(new Dimension(60, 20));
        spnExponentialTheta.addChangeListener(FrameChangeAdapter);
        {
        class ExponentialThetaChangeListener implements ChangeListener
            {
            public void stateChanged(final ChangeEvent e)
                {
                if (frmExpertKnowledge.isVisible())
                    {
                    createExpertKnowledgeRuntime();
                    frmExpertKnowledge.readDatafile(expertKnowledge);
                    }
                }
            }
        spnExponentialTheta
                .addChangeListener(new ExponentialThetaChangeListener());
        }
        spnExponentialTheta.setEnabled(false);
        spnExponentialTheta.setFont(Frame.font);
        pnlEDAEKConv.add(spnExponentialTheta, new GridBagConstraints(1, 3, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.WEST,
                                                                     GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        // Add pnlEDAEKConv to pnlEDAExpertKnowledge panel
        pnlEDAEKPanel.add(pnlEDAEKConv, new GridBagConstraints(0, 1,
                                                               GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                                               GridBagConstraints.WEST,
                                                               GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
        pnlEDAExpertKnowledge.add(pnlEDAEKPanel, java.awt.BorderLayout.WEST);
        // Add pnlEDAExpertKnowledge to pnlEDASearchOptions panel
        pnlEDASearchOptions.add(pnlEDAExpertKnowledge, new GridBagConstraints(0, 2,
                                                                              GridBagConstraints.REMAINDER,
                                                                              GridBagConstraints.REMAINDER, 0.0, 0.0,
                                                                              GridBagConstraints.WEST,
                                                                              GridBagConstraints.HORIZONTAL,
                                                                              new Insets(0,


































































































































































































































































                                                                                                                                                 5, 0, 0), 0, 0));
        // Add EDASearchOptions to SearchOptions panel
        pnlSearchOptions.add(pnlEDASearchOptions, "EDA");
        /* Add SearchOptions to SearchMethodConfiguration Panel */
        pnlSearchMethodConfiguration.add(pnlSearchOptions,
                                         java.awt.BorderLayout.CENTER);
        /* Add SearchMethodConfiguration to Configuration Panel */
        pnlConfiguration.add(pnlSearchMethodConfiguration, new GridBagConstraints(
                0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        /* Add Defaults Button to Configuration Panel */
        cmdDefaults = new JButton("Defaults");
        cmdDefaults.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            setConfigToDefaults();
            }
        });
        cmdDefaults.setFont(Frame.font);
        cmdDefaults.setEnabled(false);
        pnlConfiguration.add(cmdDefaults, new GridBagConstraints(0, 2, 1, 1, 0.0,
                                                                 1.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(
                5, 0, 0, 5), 0, 0));
        pnlAllModelsLandscape.setXAxisLabel("Models");
        pnlAllModelsLandscape.setYAxisLabel("Training Accuracy");
        pnlTopModelsLandscape.setXAxisLabel("Models");
        pnlTopModelsLandscape.setYAxisLabel("Training Accuracy");
        pnlFilterLandscape.setXAxisLabel("Attributes");
        pnlFilterLandscape.setYAxisLabel("Filter");
        cboFilter.setFont(Frame.font);
        cboFilter.addItem(new DisplayPair<String, Component>("ReliefF",
                                                             pnlReliefFOptions));
        cboFilter
                .addItem(new DisplayPair<String, Component>("TuRF", pnlTuRFOptions));
        cboFilter.addItem(new DisplayPair<String, Component>("\u03a7\u00b2",
                                                             pnlChiSquaredOptions));
        cboFilter.addItem(new DisplayPair<String, Component>("OddsRatio",
                                                             pnlOddsRatioOptions));
        cboFilter
                .addItem(new DisplayPair<String, Component>("SURF", pnlSURFOptions));
        cboFilter.addItem(new DisplayPair<String, Component>("SURFnTuRF",
                                                             pnlSURFnTuRFOptions));
        cboFilter.addItem(new DisplayPair<String, Component>("SURF*",
                                                             pnlSURFStarOptions));
        cboFilter.addItem(new DisplayPair<String, Component>("SURF*nTuRF",
                                                             pnlSURFStarnTuRFOptions));
        cboFilter.addItemListener(new Frame_cboFilter_itemAdapter(this));
        cboCriterion.setFont(Frame.font);
        cboCriterion.addItemListener(new Frame_cboCriterion_itemAdapter(this));
        cboCriterion.addItem("Top N");
        cboCriterion.addItem("Top %");
        cboCriterion.addItem("Threshold");
        snmTopN.addChangeListener(filterChangeListener);
        snmTopPct.addChangeListener(filterChangeListener);
        snmThreshold.addChangeListener(filterChangeListener);
        pnlEntropyDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
        cmdSaveEntropy.setText("Save");
        cmdSaveEntropy
                .addActionListener(new Frame_cmdSaveEntropyDisplay_actionAdapter(this));
        cmdSaveEntropy.setFont(Frame.font);
        cmdSaveEntropy.setEnabled(false);
        pnlDendrogram.setMargin(5);
        lblEntropyGraphLineThickness
                .setText("<html><align='right'>Line<br>size:</align></html>");
        lblEntropyGraphLineThickness.setEnabled(false);
        lblEntropyGraphLineThickness.setFont(Frame.font);
        lblEntropyGraphTextSize
                .setText("<html><align='right'>Text<br>size:</align></html>");
        lblEntropyGraphTextSize.setEnabled(false);
        lblEntropyGraphTextSize.setFont(Frame.font);
        lblMinimumAbsoluteEntropy
                .setText("<html><align='right'>Min. Abs.<br>% entropy:</align></html>");
        lblMinimumAbsoluteEntropy.setEnabled(false);
        lblMinimumAbsoluteEntropy.setFont(Frame.font);
        spnEntropyGraphLineThickness.setMinimumSize(new Dimension(40, 20));
        spnEntropyGraphLineThickness.setPreferredSize(new Dimension(40, 20));
        spnEntropyGraphLineThickness.setModel(new SpinnerNumberModel(new Integer(
                Main.defaultEntropyGraphLineThickness), new Integer(1), new Integer(
                1000), new Integer(1)));
        spnEntropyGraphLineThickness.setEnabled(false);
        spnEntropyGraphLineThickness.setFont(Frame.font);
        spnEntropyGraphLineThickness.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            cmdResetEntropyGraph.setEnabled(!isDefaultEntropyGraph());
            final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                               .getSelectedItem().toString());
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                final int lineThickness = ((Number) spnEntropyGraphLineThickness
                        .getValue()).intValue();
                entropyDisplay.setLineThickness(lineThickness);
                }
            }
        });
        // force initial setting of line thickness
        spnEntropyGraphLineThickness.getChangeListeners()[0]
                .stateChanged(new ChangeEvent(this));
        spnEntropyGraphTextSize.setMinimumSize(new Dimension(40, 20));
        spnEntropyGraphTextSize.setPreferredSize(new Dimension(40, 20));
        spnEntropyGraphTextSize.setModel(new SpinnerNumberModel(new Integer(
                Main.defaultEntropyGraphTextSize), new Integer(1), new Integer(1000),
                                                                new Integer(1)));
        spnEntropyGraphTextSize.setEnabled(false);
        spnEntropyGraphTextSize.setFont(Frame.font);
        spnEntropyGraphTextSize.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            cmdResetEntropyGraph.setEnabled(!isDefaultEntropyGraph());
            final int fontSize = ((Number) spnEntropyGraphTextSize.getValue())
                    .intValue();
            final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                               .getSelectedItem().toString());
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                entropyDisplay.setFontSize(fontSize);
                }
            }
        });
        // force initial setting of font size
        spnEntropyGraphTextSize.getChangeListeners()[0]
                .stateChanged(new ChangeEvent(this));
        spnMinimumAbsoluteEntropy.setMinimumSize(new Dimension(50, 20));
        spnMinimumAbsoluteEntropy.setPreferredSize(new Dimension(50, 20));
        spnMinimumAbsoluteEntropy.setModel(new SpinnerListModel());
        final JFormattedTextField tf = ((JSpinner.ListEditor) spnMinimumAbsoluteEntropy
                .getEditor()).getTextField();
        tf.setHorizontalAlignment(SwingConstants.RIGHT);
        spnMinimumAbsoluteEntropy.setEnabled(false);
        spnMinimumAbsoluteEntropy.setFont(Frame.font);
        spnMinimumAbsoluteEntropy.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                               .getSelectedItem().toString());
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                if (entropyDisplay.supportEntropyThreshold())
                    {
                    final double minimumAbsoluteEntropyPercent = ((Number) spnMinimumAbsoluteEntropy
                            .getValue()).doubleValue();
                    entropyDisplay
                            .setMinimumAbsoluteEntropyPercent(minimumAbsoluteEntropyPercent);
                    cmdResetEntropyGraph.setEnabled(!isDefaultEntropyGraph());
                    }
                }
            }
        });
        // force initial setting of minimum absolute entropy
        spnMinimumAbsoluteEntropy.getChangeListeners()[0]
                .stateChanged(new ChangeEvent(this));
        cmdResetEntropyGraph.setEnabled(false);
        cmdResetEntropyGraph.setText("Reset");
        cmdResetEntropyGraph.setFont(Frame.font);
        cmdResetEntropyGraph.setEnabled(false);
        cmdResetEntropyGraph.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdResetEntropyGraph_actionPerformed(e);
            }
        });
        cmdMaximizeEntropy.setText("Maximize");
        cmdMaximizeEntropy.setFont(Frame.font);
        cmdMaximizeEntropy.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdMaximizeEntropyDisplay_actionPerformed(e);
            }
        });
        txaRawEntropyValues.setEditable(false);
        txaRawEntropyValues.setFont(Frame.fontFixed);
        cboEntropy.setEnabled(false);
        cboEntropy.addItemListener(new ItemListener()
        {
        public void itemStateChanged(final ItemEvent e)
            {
            final String name = cboEntropy.getSelectedItem().toString();
            pnlEntropyDisplay.removeAll();
            pnlEntropyDisplaySpecificControls.removeAll();
            pnlEntropyDisplaySpecificControls.add(entropyPanelSpecificControls
                                                          .get(name));
            final Component component = entropyPanelEntropyDisplay.get(name);
            component.setBounds(pnlEntropyDisplay.getBounds());
            pnlEntropyDisplay.add(component);
            if (component instanceof EntropyDisplay)
                {
                final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
                entropyDisplay.updateGraph();
                spnEntropyGraphTextSize.setValue(entropyDisplay.getFontSize());
                spnEntropyGraphLineThickness.setValue(entropyDisplay
                                                              .getLineThickness());
                if (entropyDisplay.supportEntropyThreshold())
                    {
                    lblMinimumAbsoluteEntropy.setVisible(true);
                    spnMinimumAbsoluteEntropy.setVisible(true);
                    ((SpinnerListModel) (spnMinimumAbsoluteEntropy.getModel()))
                            .setList(entropyDisplay
                                             .getMinimumAbsoluteEntropyPercentValuesSet());
                    spnMinimumAbsoluteEntropy.setValue(entropyDisplay
                                                               .getMinimumAbsoluteEntropyPercent());
                    }
                else
                    {
                    lblMinimumAbsoluteEntropy.setVisible(false);
                    spnMinimumAbsoluteEntropy.setVisible(false);
                    }
                cmdResetEntropyGraph.setEnabled(!isDefaultEntropyGraph());
                }
            pnlEntropy.invalidate();
            pnlEntropy.repaint();
            }
        });
        // force initial setting of entropy display
        cboEntropy.getItemListeners()[0].itemStateChanged(new ItemEvent(cboEntropy,
                                                                        0, null, ItemEvent.SELECTED));
        lblReliefFNeighbors.setText("Nearest Neighbors:");
        lblReliefFNeighbors.setFont(Frame.font);
        lblReliefFSampleSize.setText("Sample Size:");
        lblReliefFSampleSize.setEnabled(false);
        lblReliefFSampleSize.setFont(Frame.font);
        spnReliefFSampleSize.setEnabled(false);
        spnReliefFSampleSize.setMinimumSize(new Dimension(60, 20));
        spnReliefFSampleSize.setPreferredSize(new Dimension(60, 20));
        spnReliefFSampleSize
                .addPropertyChangeListener(filterPropertyChangeListener);
        spnReliefFSampleSize.setModel(new SpinnerNumberModel(new Integer(
                Main.defaultReliefFSampleSize), new Integer(1), new Integer(1000000),
                                                             new Integer(50)));
        spnReliefFSampleSize.setFont(Frame.font);
        spnReliefFNeighbors.setMinimumSize(new Dimension(60, 20));
        spnReliefFNeighbors.setPreferredSize(new Dimension(60, 20));
        spnReliefFNeighbors.setModel(new SpinnerNumberModel(new Integer(
                Main.defaultReliefFNeighbors), new Integer(1), new Integer(1000000),
                                                            new Integer(1)));
        spnReliefFNeighbors.getModel().addChangeListener(filterChangeListener);
        spnReliefFNeighbors.setFont(Frame.font);
        chkReliefFWholeDataset.setText("Whole Dataset");
        chkReliefFWholeDataset.setFont(Frame.font);
        chkReliefFWholeDataset.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            final boolean enabled = !chkReliefFWholeDataset.isSelected();
            lblReliefFSampleSize.setEnabled(enabled);
            spnReliefFSampleSize.setEnabled(enabled);
            }
        });
        chkReliefFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        chkReliefFWholeDataset
                .addPropertyChangeListener(filterPropertyChangeListener);
        lblTuRFSampleSize.setText("Sample Size:");
        lblTuRFSampleSize.setFont(Frame.font);
        lblTuRFNeighbors.setText("Nearest Neighbors:");
        lblTuRFNeighbors.setFont(Frame.font);
        spnTuRFNeighbors.setMinimumSize(new Dimension(60, 20));
        spnTuRFNeighbors.setPreferredSize(new Dimension(60, 20));
        spnTuRFNeighbors.setFont(Frame.font);
        spnTuRFNeighbors.addPropertyChangeListener(filterPropertyChangeListener);
        spnTuRFSampleSize.setMinimumSize(new Dimension(60, 20));
        spnTuRFSampleSize.setPreferredSize(new Dimension(60, 20));
        spnTuRFSampleSize.setFont(Frame.font);
        spnTuRFSampleSize.addPropertyChangeListener(filterPropertyChangeListener);
        spnTuRFSampleSize.setModel(new SpinnerNumberModel(new Integer(
                Main.defaultReliefFSampleSize), new Integer(1), new Integer(1000000),
                                                          new Integer(50)));
        spnTuRFSampleSize.getModel().addChangeListener(filterChangeListener);
        chkTuRFWholeDataset.setText("Whole Dataset");
        chkTuRFWholeDataset.setFont(Frame.font);
        chkTuRFWholeDataset.addChangeListener(new ChangeListener()
        {
        public void stateChanged(final ChangeEvent e)
            {
            final boolean enabled = !chkTuRFWholeDataset.isSelected();
            lblTuRFSampleSize.setEnabled(enabled);
            spnTuRFSampleSize.setEnabled(enabled);
            }
        });
        chkTuRFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        chkTuRFWholeDataset.addPropertyChangeListener(filterPropertyChangeListener);
        lblTuRFPercent.setText("Percent:");
        lblTuRFPercent.setFont(Frame.font);
        spnTuRFPercent.setMinimumSize(new Dimension(60, 20));
        spnTuRFPercent.setPreferredSize(new Dimension(60, 20));
        spnTuRFPercent.setFont(Frame.font);
        spnTuRFPercent.addPropertyChangeListener(filterPropertyChangeListener);
        spnTuRFPercent
                .setModel(new SpinnerNumberModel(new Integer(Main.defaultTuRFPct),
                                                 new Integer(1), new Integer(100), new Integer(1)));
        spnTuRFPercent.getModel().addChangeListener(filterChangeListener);
        lblSURFnTuRFPercent.setText("Percent:");
        lblSURFnTuRFPercent.setFont(Frame.font);
        spnSURFnTuRFPercent.setMinimumSize(new Dimension(60, 20));
        spnSURFnTuRFPercent.setPreferredSize(new Dimension(60, 20));
        spnSURFnTuRFPercent.setFont(Frame.font);
        spnSURFnTuRFPercent.addPropertyChangeListener(filterPropertyChangeListener);
        spnSURFnTuRFPercent
                .setModel(new SpinnerNumberModel(new Integer(Main.defaultTuRFPct),
                                                 new Integer(1), new Integer(100), new Integer(1)));
        spnSURFnTuRFPercent.getModel().addChangeListener(filterChangeListener);
        lblSURFStarnTuRFPercent.setText("Percent:");
        lblSURFStarnTuRFPercent.setFont(Frame.font);
        spnSURFStarnTuRFPercent.setMinimumSize(new Dimension(60, 20));
        spnSURFStarnTuRFPercent.setPreferredSize(new Dimension(60, 20));
        spnSURFStarnTuRFPercent.setFont(Frame.font);
        spnSURFStarnTuRFPercent
                .addPropertyChangeListener(filterPropertyChangeListener);
        spnSURFStarnTuRFPercent
                .setModel(new SpinnerNumberModel(new Integer(Main.defaultTuRFPct),
                                                 new Integer(1), new Integer(100), new Integer(1)));
        spnSURFStarnTuRFPercent.getModel().addChangeListener(filterChangeListener);
        tblSummaryTable.setModel(dtmSummaryTable);
        // tblSummaryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblSummaryTable.getTableHeader().setReorderingAllowed(false);
        tblSummaryTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        tblSummaryTable.getSelectionModel().addListSelectionListener(
                new Frame_tblSummaryTable_selectionAdapter(this));
        dcrRightJustified.setHorizontalAlignment(SwingConstants.RIGHT);
        dtmSummaryTable.addColumn("Model");
        dtmSummaryTable.addColumn(Frame.TRAINING_BAL_ACC);
        dtmSummaryTable.addColumn(Frame.TESTING_BAL_ACC);
        dtmSummaryTable.addColumn(Frame.CV_CONSISTENCY);
        tbcModel = tblSummaryTable.getColumnModel().getColumn(0);
        tbcTrainingValue = tblSummaryTable.getColumnModel().getColumn(1);
        tbcTestingValue = tblSummaryTable.getColumnModel().getColumn(2);
        tbcCVConsistency = tblSummaryTable.getColumnModel().getColumn(3);
        tbcModel.setPreferredWidth(125);
        tbcTrainingValue.setPreferredWidth(125);
        tbcTrainingValue.setCellRenderer(dcrRightJustified);
        tbcTestingValue.setPreferredWidth(125);
        tbcTestingValue.setCellRenderer(dcrRightJustified);
        tbcCVConsistency.setPreferredWidth(125);
        tbcCVConsistency.setCellRenderer(dcrRightJustified);
        pnlWarning.setTabPane(tpnResults);
        pnlWarning.setTitle("Warnings");
        pnlWarning.setFont(Frame.font);
        lblRatio.setText("Ratio:");
        lblRatio.setFont(Frame.font);
        lblRatioValue.setFont(Frame.font);
        lblFilter.setText("Filter:");
        lblFilter.setFont(Frame.font);
        lblCriterion.setText("Attribute Subset Criterion:");
        lblCriterion.setFont(Frame.font);
        lblCriterionValue.setText("Value:");
        lblCriterionValue.setFont(Frame.font);
        spnCriterionFilter.setMinimumSize(new Dimension(60, 20));
        spnCriterionFilter.setPreferredSize(new Dimension(60, 20));
        spnCriterionFilter.addPropertyChangeListener(filterPropertyChangeListener);
        spnCriterionFilter.setFont(Frame.font);
        pnlFilterControls.setTitleFont(Frame.fontBold);
        pnlFilterControls.setTitle("Filter Controls");
        cmdDefaultFilter.setText("Defaults");
        cmdDefaultFilter.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            setFilterToDefaults();
            }
        });
        cmdDefaultFilter.setEnabled(false);
        cmdDefaultFilter.setFont(Frame.font);
        cmdExportFiltered.setText("Export Datafile");
        cmdExportFiltered
                .addActionListener(new Frame_cmdExportFiltered_actionAdapter(this));
        cmdExportFiltered.setEnabled(false);
        cmdExportFiltered.setFont(Frame.font);
        cmdRevertFilter.setText("Revert");
        cmdRevertFilter.addActionListener(new Frame_cmdRevertFilter_actionAdapter(
                this));
        cmdRevertFilter.setEnabled(false);
        cmdRevertFilter.setFont(Frame.font);
        cmdFilter.setText("Apply");
        cmdFilter.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdFilter_actionPerformed(e);
            }
        });
        cmdFilter.setEnabled(false);
        cmdFilter.setFont(Frame.font);
        pnlFilterOptions.setLayout(crdFilterOptions);
        pnlFilterOptions.setTitle("Filter Options");
        pnlFilterOptions.setTitleFont(Frame.fontBold);
        pnlGraphicalModel.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            pnlGraphicalModel_actionPerformed(e);
            }
        });
        pnlGraphicalModel.setEnabled(false);
        cmdIfThenRulesSave.setText("Save");
        cmdIfThenRulesSave
                .addActionListener(new Frame_cmdIfThenRulesSave_actionAdapter(this));
        scpIfThenRules.setBorder(BorderFactory.createLoweredBevelBorder());
        chkChiSquaredPValue.setText("P-Values");
        cmdRevert.setEnabled(false);
        cmdRevert.setFont(Frame.font);
        cmdRevert.setText("Revert Filter");
        cmdRevert.addActionListener(new Frame_cmdRevertFilter_actionAdapter(this));
        prgFilterProgress.setTitleFont(Frame.fontBold);
        pnlFilterSelection.setTitleFont(Frame.fontBold);
        pnlFilterSelection.setTitle("Filter Selection");
        pnlDatafileTable.setBorder(BorderFactory.createLoweredBevelBorder());
        lblDatafileLoaded.setFont(Frame.font);
        lblDatafileFiltered.setFont(Frame.font);
        pnlAnalysisControls.setTitle("Analysis Controls");
        pnlAnalysisControls.setTitleFont(Frame.fontBold);
        tpnAnalysis.add(pnlAnalysis, "Analysis");
        pnlAnalysis.setPreferredSize(new java.awt.Dimension(3924, 759));
        tpnAnalysis.setFont(Frame.fontBold);
        scpMain.getViewport().add(pnlMain);
        tpnResults.setFont(Frame.fontBold);
        pnlGraphicalModel.setFont(Frame.font);
        pnlBestModel.add(pnlBestModelButtons, java.awt.BorderLayout.SOUTH);
        pnlBestModelButtons.add(cmdBestModelSave, new GridBagConstraints(0, 0, 1,
                                                                         1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                                         new Insets(5, 5, 5, 5), 0, 0));
        pnlBestModelButtons.add(chkBestModelVerbose, new GridBagConstraints(0, 0,
                                                                            1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                            new Insets(5, 5, 5, 5), 0, 0));
        pnlBestModel.add(scpBestModel, java.awt.BorderLayout.CENTER);
        scpBestModel.getViewport().add(txaBestModel);
        tpnResults.addOrderedTab(pnlGraphicalModel, "Graphical Model");
        tpnResults.addOrderedTab(pnlBestModel, "Best Model");
        tpnResults.addOrderedTab(pnlIfThenRules, "If-Then Rules");
        tpnResults.addOrderedTab(pnlCVResults, "CV Results");
        tpnResults.addOrderedTab(pnlEntropy, "Entropy");
        tpnResults.addOrderedTab(pnlTopModelsLandscape, "Top Models");
        tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, false);
        tpnResults.addOrderedTab(pnlAllModelsLandscape, "Landscape");
        tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, false);
        tpnFilter.setFont(Frame.fontBold);
        getContentPane().add(scpMain, java.awt.BorderLayout.CENTER);
        pnlCVResults.add(pnlCVResultsButtons, java.awt.BorderLayout.SOUTH);
        pnlCVResultsButtons.add(cmdCVResultsSave, new GridBagConstraints(0, 0, 1,
                                                                         1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                                         new Insets(5, 5, 5, 5), 0, 0));
        pnlCVResultsButtons.add(chkCVResultsVerbose, new GridBagConstraints(0, 0,
                                                                            1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                            new Insets(5, 5, 5, 5), 0, 0));
        pnlCVResults.add(scpCVResults, java.awt.BorderLayout.CENTER);
        scpCVResults.getViewport().add(txaCVResults);
        pnlAllModelsLandscape.setFont(Frame.font);
        pnlAllModelsLandscape.setTextFont(Frame.fontFixed);
        pnlAllModelsLandscape.setEnabled(false);
        pnlAllModelsLandscape.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            pnlAllModelsLandscape_actionPerformed(e);
            }
        });
        pnlTopModelsLandscape.setFont(Frame.font);
        pnlTopModelsLandscape.setTextFont(Frame.fontFixed);
        pnlTopModelsLandscape.setEnabled(false);
        pnlTopModelsLandscape.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            pnlTopModelsLandscape_actionPerformed(e);
            }
        });
        pnlDatafileInformation.add(pnlDatafile, new GridBagConstraints(0, 0, 3, 1,
                                                                       1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                       new Insets(0, 5, 0, 0), 0, 0));
        pnlDatafileInformation.add(cmdLoadDatafile, new GridBagConstraints(3, 0, 1,
                                                                           1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                           new Insets(0, 5, 0, 5), 0, 0));
        pnlDatafileInformation.add(pnlInstances, new GridBagConstraints(0, 1, 1, 1,
                                                                        1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                        new Insets(0, 5, 0, 0), 0, 0));
        pnlInstances.add(lblInstances, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                              GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
                                                                                                                             0), 0, 0));
        pnlInstances.add(lblInstancesValue, new GridBagConstraints(1, 0, 1, 1, 1.0,
                                                                   0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                     0, 0), 0, 0));
        pnlDatafileInformation.add(pnlAttributes, new GridBagConstraints(1, 1, 1,
                                                                         1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                         new Insets(0, 5, 0, 0), 0, 0));
        pnlAttributes.add(lblAttributes, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
                                                                                                                                    0, 0, 0), 0, 0));
        pnlAttributes.add(lblAttributesValue, new GridBagConstraints(1, 0, 1, 1,
                                                                     1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        pnlDatafileInformation.add(pnlRatio, new GridBagConstraints(2, 1, 1, 1,
                                                                    1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                    new Insets(0, 5, 0, 0), 0, 0));
        pnlRatio.add(lblRatio, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                      GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
                                                                                                                     0), 0, 0));
        pnlRatio.add(lblRatioValue, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                           GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,
                                                                                                                              5, 0, 0), 0, 0));
        pnlDatafileInformation.add(cmdViewDatafile, new GridBagConstraints(3, 1, 1,
                                                                           1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                           new Insets(5, 5, 5, 5), 0, 0));
        pnlDatafileInformation.add(pnlUsedMem, new GridBagConstraints(0, 2, 1, 1,
                                                                      1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                      new Insets(0, 5, 0, 0), 0, 0));
        pnlUsedMem.add(lblUsedMemLabel, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                               0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
                                                                                                                                   0, 0, 0), 0, 0));
        pnlUsedMem.add(lblUsedMem, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                          GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                          new Insets(0, 5, 0, 0), 0, 0));
        pnlDatafileInformation.add(pnlTotalMem, new GridBagConstraints(1, 2, 1, 1,
                                                                       1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                       new Insets(0, 5, 0, 0), 0, 0));
        pnlTotalMem.add(lblTotalMemLabel, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                                 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
                                                                                                                                     0, 0, 0), 0, 0));
        pnlTotalMem.add(lblTotalMem, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                            GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                            new Insets(0, 5, 0, 0), 0, 0));
        pnlDatafileInformation.add(pnlMaxMem, new GridBagConstraints(2, 2, 1, 1,
                                                                     1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                     new Insets(0, 5, 0, 0), 0, 0));
        pnlMaxMem.add(lblMaxMemLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                             GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
                                                                                                                            0), 0, 0));
        pnlMaxMem.add(lblMaxMem, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                        new Insets(0, 5, 0, 0), 0, 0));
        tpnAnalysis.add(pnlConfiguration, "Configuration");
        pnlIfThenRules.add(pnlIfThenRulesButtons, java.awt.BorderLayout.SOUTH);
        pnlIfThenRulesButtons.add(cmdIfThenRulesSave, new GridBagConstraints(0, 0,
                                                                             1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                                             new Insets(5, 5, 5, 5), 0, 0));
        pnlIfThenRules.add(scpIfThenRules, java.awt.BorderLayout.CENTER);
        scpIfThenRules.getViewport().add(txaIfThenRules);
        tpnFilter.add(pnlFilterLandscape, "Landscape");
        tpnFilter.add(pnlDatasetView, "Filtered Datafile");
        pnlDatasetView.add(pnlDatafileTable, java.awt.BorderLayout.CENTER);
        pnlDatasetView.add(pnlDatafileViewButtons, java.awt.BorderLayout.SOUTH);
        pnlDatafileViewButtons.add(cmdExportFiltered, new GridBagConstraints(0, 0,
                                                                             1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                                             new Insets(5, 5, 5, 5), 0, 0));
        pnlFilter.add(pnlFilterSelection, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                                 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5,
                                                                                                                                     5, 0, 5), 0, 0));
        pnlFilterControls.add(cmdFilter, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                new Insets(5, 5, 5, 0), 0, 0));
        pnlFilterControls.add(cmdDefaultFilter, new GridBagConstraints(2, 0, 1, 1,
                                                                       1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                5, 5, 5, 5), 0, 0));
        pnlFilterControls.add(cmdRevertFilter, new GridBagConstraints(1, 0, 1, 1,
                                                                      0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                      new Insets(5, 5, 5, 0), 0, 0));
        pnlFilter.add(prgFilterProgress, new GridBagConstraints(0, 3, 1, 1, 0.0,
                                                                0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5,
                                                                                                                                    5, 0, 5), 0, 0));
        pnlFilter.add(tpnFilter, new GridBagConstraints(0, 4, 1, 1, 0.0, 1.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5,
                                                                                                                       5), 0, 0));
        pnlFilter.add(pnlFilterControls, new GridBagConstraints(0, 2, 1, 1, 0.0,
                                                                0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5,
                                                                                                                                    5, 0, 5), 0, 0));
        pnlFilter.add(pnlFilterOptions, new GridBagConstraints(0, 1, 1, 1, 1.0,
                                                               0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5,
                                                                                                                                   5, 0, 5), 0, 0));
        pnlFilterOptions.add(pnlReliefFOptions, "ReliefF");
        pnlReliefFOptions.add(lblReliefFSampleSize, new GridBagConstraints(0, 1, 1,
                                                                           1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                           new Insets(5, 5, 0, 0), 0, 0));
        pnlReliefFOptions.add(lblReliefFNeighbors, new GridBagConstraints(0, 0, 1,
                                                                          1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 0, 0), 0, 0));
        pnlReliefFOptions.add(spnReliefFSampleSize, new GridBagConstraints(1, 1, 1,
                                                                           1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                           new Insets(5, 5, 0, 0), 0, 0));
        pnlReliefFOptions.add(spnReliefFNeighbors, new GridBagConstraints(1, 0, 1,
                                                                          1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 0, 0), 0, 0));
        pnlReliefFOptions.add(chkReliefFWholeDataset, new GridBagConstraints(2, 1,
                                                                             1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                             new Insets(5, 5, 0, 0), 0, 0));
        pnlFilterOptions.add(pnlChiSquaredOptions, "\u03a7\262");
        pnlChiSquaredOptions.add(chkChiSquaredPValue, new GridBagConstraints(0, 0,
                                                                             1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                             new Insets(0, 5, 0, 0), 0, 0));
        pnlAnalysisControls.add(cmdRunAnalysis, new GridBagConstraints(0, 0, 1, 1,
                                                                       0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                5, 5, 5, 0), 0, 0));
        pnlAnalysisControls.add(cmdLoadAnalysis, new GridBagConstraints(1, 0, 1, 1,
                                                                        0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                        new Insets(5, 5, 5, 0), 0, 0));
        pnlAnalysisControls.add(cmdSaveAnalysis, new GridBagConstraints(2, 0, 1, 1,
                                                                        0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                        new Insets(5, 5, 5, 0), 0, 0));
        pnlAnalysisControls.add(cmdRevert, new GridBagConstraints(3, 0, 1, 1, 1.0,
                                                                  0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5,
                                                                                                                                    5, 0), 0, 0));
        pnlSummaryTable.add(scpSummaryTable, java.awt.BorderLayout.CENTER);
        tblSummaryTable.setVisible(true);
        scpSummaryTable.getViewport().add(tblSummaryTable);
        tpnAnalysis.add(pnlFilter, "Filter");
        pnlDatafile.add(lblDatafile, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                            GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
                                                                                                                           0), 0, 0));
        pnlDatafile.add(lblDatafileFiltered, new GridBagConstraints(3, 0, 1, 1,
                                                                    1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 0, 0, 0), 0, 0));
        pnlDatafile.add(lblDatafileValue, new GridBagConstraints(1, 0, 1, 1, 0.0,
                                                                 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                   0, 0), 0, 0));
        pnlDatafile.add(lblDatafileLoaded, new GridBagConstraints(2, 0, 1, 1, 0.0,
                                                                  0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
                                                                                                                                    0, 0), 0, 0));
        pnlFilterLandscape.setFont(Frame.font);
        pnlFilterLandscape.setTextFont(Frame.fontFixed);
        pnlFilterLandscape.setEnabled(false);
        pnlFilterLandscape.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            pnlFilterLandscape_actionPerformed(e);
            }
        });
        cmdIfThenRulesSave.setEnabled(false);
        cmdIfThenRulesSave.setText("Save");
        cmdIfThenRulesSave.setFont(Frame.font);
        chkChiSquaredPValue.setFont(Frame.font);
        chkChiSquaredPValue.addPropertyChangeListener(filterPropertyChangeListener);
        frmDatafile.setFont(Frame.font);
        frmExpertKnowledge.setFont(Frame.font);
        txaIfThenRules.setEditable(false);
        txaIfThenRules.setFont(Frame.fontFixed);
        bgrRandomOptions.add(rdoEvaluations);
        bgrRandomOptions.add(rdoRuntime);
        pnlMain.add(tpnAnalysis, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
                                                                                                                       0), 0, 0));
        pnlFilterSelection.add(lblFilter, new GridBagConstraints(0, 0, 1, 1, 0.0,
                                                                 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                   5, 0), 0, 0));
        pnlFilterSelection.add(cboFilter, new GridBagConstraints(1, 0, 1, 1, 0.0,
                                                                 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                   5, 0), 0, 0));
        pnlFilterSelection.add(lblCriterion, new GridBagConstraints(0, 1, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                    new Insets(0, 5, 5, 0), 0, 0));
        pnlFilterSelection.add(cboCriterion, new GridBagConstraints(1, 1, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 5, 5, 0), 0, 0));
        pnlFilterSelection.add(lblCriterionValue, new GridBagConstraints(2, 1, 1,
                                                                         1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                         new Insets(0, 5, 5, 0), 0, 0));
        pnlFilterSelection.add(spnCriterionFilter, new GridBagConstraints(3, 1, 1,
                                                                          1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                          new Insets(0, 5, 5, 5), 0, 0));
        pnlFilterOptions.add(pnlOddsRatioOptions, "OddsRatio");
        pnlFilterOptions.add(pnlTuRFOptions, "TuRF");
        pnlTuRFOptions.add(lblTuRFSampleSize, new GridBagConstraints(0, 1, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                     new Insets(5, 5, 0, 0), 0, 0));
        pnlTuRFOptions.add(spnTuRFSampleSize, new GridBagConstraints(1, 1, 1, 1,
                                                                     0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                     new Insets(5, 5, 0, 0), 0, 0));
        pnlTuRFOptions.add(lblTuRFNeighbors, new GridBagConstraints(0, 0, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        pnlTuRFOptions.add(spnTuRFNeighbors, new GridBagConstraints(1, 0, 1, 1,
                                                                    0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 5, 0, 0), 0, 0));
        pnlTuRFOptions.add(lblTuRFPercent, new GridBagConstraints(2, 0, 1, 1, 0.0,
                                                                  0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                    0, 0), 0, 0));
        pnlTuRFOptions.add(spnTuRFPercent, new GridBagConstraints(3, 0, 1, 1, 0.0,
                                                                  0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5,
                                                                                                                                    0, 0), 0, 0));
        pnlTuRFOptions.add(chkTuRFWholeDataset, new GridBagConstraints(2, 1, 2, 1,
                                                                       1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                                       new Insets(5, 5, 0, 0), 0, 0));
        pnlFilterOptions.add(pnlSURFOptions, "SURF");
        pnlFilterOptions.add(pnlSURFStarOptions, "SURF*");
        pnlFilterOptions.add(pnlSURFnTuRFOptions, "SURFnTuRF");
        pnlSURFnTuRFOptions.add(lblSURFnTuRFPercent, new GridBagConstraints(0, 0,
                                                                            1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                            new Insets(0, 5, 0, 0), 0, 0));
        pnlSURFnTuRFOptions.add(spnSURFnTuRFPercent, new GridBagConstraints(1, 0,
                                                                            1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                            new Insets(0, 5, 0, 0), 0, 0));
        pnlFilterOptions.add(pnlSURFStarnTuRFOptions, "SURF*nTuRF");
        pnlSURFStarnTuRFOptions.add(lblSURFStarnTuRFPercent,
                                    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                           GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(
                                            0, 5, 0, 0), 0, 0));
        pnlSURFStarnTuRFOptions.add(spnSURFStarnTuRFPercent,
                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                                                           GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropy
                .add(pnlEntropyDisplayCommonControls, java.awt.BorderLayout.SOUTH);
        tpnAnalysis.add(pnlConstruct, "Attribute Construction");
        tpnAnalysis.add(pnlAdjust, "Covariate Adjustment");
        pnlAbout = new JPanel(new BorderLayout());
        scpAbout = new JScrollPane();
        pnlAbout.add(scpAbout, BorderLayout.CENTER);
        editorPaneAbout = new JEditorPane();
        scpAbout.setViewportView(editorPaneAbout);
        editorPaneAbout.setEditable(false);
        editorPaneAbout.addHyperlinkListener(new HyperlinkListener()
        {
        public void hyperlinkUpdate(final HyperlinkEvent evt)
            {
            if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                // try {
                // jEditorPaneAbout.setPage(evt.getURL());
                BareBonesBrowserLaunch.openURL(evt.getURL().toExternalForm());
                // } catch (final Exception e) {
                // }
                }
            }
        });
        // load the page asynchronously because otherwise if there is a connection
        // problem the entire gui stops entire the url times out (30+ seconds)
        final Thread loadAboutPanThread = new Thread(new Runnable()
        {
        public void run()
            {
            try
                {
                // editorPaneAbout.setPage("http://discovery.dartmouth.edu");
                editorPaneAbout.setPage("http://www.org.epistasis.org/about.html");
                tpnAnalysis.addTab("About MDR", null, pnlAbout, null);
                }
            catch (final IOException ex)
                {
                // MDR url could not be read so no tab shown
                }
            }
        }, "About Pane loader");
        loadAboutPanThread.setDaemon(true); // kill this thread when program ends if still running
        loadAboutPanThread.start();
        cboEntropy.setFont(Frame.font);
        cboEntropy.setEditable(false);
        pnlEntropy.add(scpEntropyDisplay, java.awt.BorderLayout.CENTER);
        pnlEntropyDisplaySpecificControls.add(entropyPanelSpecificControls
                                                      .get(DisplayType.Dendogram.toString()));
        pnlEntropyDisplayCommonControls.add(cmdMaximizeEntropy,
                                            new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
                                                                   GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        pnlEntropyDisplayCommonControls.add(cmdSaveEntropy, new GridBagConstraints(
                3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5), 0, 0));
        pnlEntropyDisplayCommonControls.add(cboEntropy, new GridBagConstraints(0,
                                                                               0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                               new Insets(5, 5, 5, 0), 0, 0));
        pnlEntropyDisplayCommonControls.add(pnlEntropyDisplaySpecificControls,
                                            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                                                   GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        pnlEntropyGraphControls.add(lblEntropyGraphLineThickness,
                                    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                           GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(spnEntropyGraphLineThickness,
                                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                                           GridBagConstraints.BOTH, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(lblEntropyGraphTextSize,
                                    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                           GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(spnEntropyGraphTextSize,
                                    new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                                           GridBagConstraints.BOTH, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(lblMinimumAbsoluteEntropy,
                                    new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                                           GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(spnMinimumAbsoluteEntropy,
                                    new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                                           GridBagConstraints.BOTH, new Insets(0, 5, 0, 0), 0, 0));
        pnlEntropyGraphControls.add(cmdResetEntropyGraph, new GridBagConstraints(6,
                                                                                 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                                                 new Insets(0, 5, 0, 0), 0, 0));
        pnlAnalysis.add(prgProgress, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                                                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                                5, 0, 5), 0, 0));
        pnlAnalysis.add(pnlAnalysisControls, new GridBagConstraints(0, 1, 2, 1,
                                                                    0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                                                    new Insets(5, 5, 0, 5), 0, 0));
        pnlAnalysis.add(pnlSummaryTable, new GridBagConstraints(0, 3, 2, 1, 0.0,
                                                                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                new Insets(5, 5, 0, 5), 0, 0));
        pnlAnalysis.add(tpnResults, new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0,
                                                           GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5,
                                                                                                                          5), 0, 0));
        pnlAnalysis.add(pnlDatafileInformation, new GridBagConstraints(1, 0, 1, 1,
                                                                       1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                                                       new Insets(5, 5, 0, 5), 0, 0));
        pnlAnalysis.add(lblLogo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0,
                                                                                                                       0), 0, 0));
        setConfigToDefaults();
        setFilterToDefaults();
        setDataDependentItemsEnabled(false);
        setModelDependentItemsEnabled(false);
        pack(); // layout components
        }

    public void lblLogo_mouseClicked(final MouseEvent e)
        {
        if (e.getButton() == MouseEvent.BUTTON1)
            {
            BareBonesBrowserLaunch.openURL(lblLogo.getToolTipText());
            }
        }

    private void lockdown(final boolean lock)
        {
        System.gc();
        if (lock)
            {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
        pnlConstruct.setWarnOnChange(!lock);
        pnlAdjust.setWarnOnChange(!lock);
        cmdFilter.setEnabled(!lock);
        cmdLoadDatafile.setEnabled(!lock);
        cmdLoadAnalysis.setEnabled(!lock);
        lblRandomSeed.setEnabled(!lock);
        spnRandomSeed.setEnabled(!lock);
        lblForcedAttributeCombination.setEnabled(!lock);
        txtForcedAttributeCombination.setEnabled(!lock);
        rdoAmbiguousCellCriteriaTieCells.setEnabled(!lock);
        rdoAmbiguousCellCriteriaFishersExact.setEnabled(!lock);
        lblAmbiguousCellAssignment.setEnabled(!lock);
        cboAmbiguousCellStatuses.setEnabled(!lock);
        lblPairedAnalysis.setEnabled(!lock);
        lblComputeAllModelsLandscape.setEnabled(!lock);
        spnTopModels.setEnabled(!lock);
        lblFilter.setEnabled(!lock);
        cboFilter.setEnabled(!lock);
        lblCriterion.setEnabled(!lock);
        cboCriterion.setEnabled(!lock);
        lblCriterionValue.setEnabled(!lock);
        spnCriterionFilter.setEnabled(!lock);
        chkChiSquaredPValue.setEnabled(!lock);
        lblReliefFNeighbors.setEnabled(!lock);
        spnReliefFNeighbors.setEnabled(!lock);
        chkReliefFWholeDataset.setEnabled(!lock);
        lblTuRFNeighbors.setEnabled(!lock);
        spnTuRFNeighbors.setEnabled(!lock);
        chkTuRFWholeDataset.setEnabled(!lock);
        chkTuRFWholeDataset.setEnabled(!lock);
        lblSearchType.setEnabled(!lock);
        cboSearchType.setEnabled(!lock);
        rdoEvaluations.setEnabled(!lock);
        rdoRuntime.setEnabled(!lock);
        lblEntropyGraphLineThickness.setEnabled(!lock);
        spnEntropyGraphLineThickness.setEnabled(!lock);
        lblEntropyGraphTextSize.setEnabled(!lock);
        spnEntropyGraphTextSize.setEnabled(!lock);
        spnMinimumAbsoluteEntropy.setEnabled(!lock);
        lblCrossValidationCount.setEnabled(!lock);
        spnCrossValidationCount.setEnabled(!lock);
        pnlConstruct.setEnabled(!lock);
        pnlAdjust.setEnabled(!lock);
        if (lock)
            {
            cmdRunAnalysis.setEnabled(false);
            cmdViewDatafile.setEnabled(false);
            cmdSaveAnalysis.setEnabled(false);
            cmdDefaultFilter.setEnabled(false);
            cmdDefaults.setEnabled(false);
            lblAttributeCountRange.setEnabled(false);
            spnAttributeCountMin.setEnabled(false);
            lblAttributeCountRangeColon.setEnabled(false);
            spnAttributeCountMax.setEnabled(false);
            cmdRevert.setEnabled(false);
            cmdRevertFilter.setEnabled(false);
            cmdExportFiltered.setEnabled(false);
            lblReliefFSampleSize.setEnabled(false);
            spnReliefFSampleSize.setEnabled(false);
            spnEvaluations.setEnabled(false);
            spnRuntime.setEnabled(false);
            chkPairedAnalysis.setEnabled(false);
            lblUnits.setEnabled(false);
            cboUnits.setEnabled(false);
            cmdResetEntropyGraph.setEnabled(false);
            }
        else
            {
            cmdRunAnalysis.setEnabled(datafileName.length() > 0);
            cmdViewDatafile.setEnabled(datafileName.length() > 0);
            cmdSaveAnalysis.setEnabled((datafileName.length() > 0)
                                       && (analysis != null) && analysis.isComplete());
            cmdDefaultFilter.setEnabled(!isDefaultFilter());
            cmdDefaults.setEnabled(!isDefaultConfig());
            lblAttributeCountRange.setEnabled(forced == null);
            spnAttributeCountMin.setEnabled(forced == null);
            lblAttributeCountRangeColon.setEnabled(forced == null);
            spnAttributeCountMax.setEnabled(forced == null);
            cmdRevert.setEnabled(unfiltered != null);
            cmdRevertFilter.setEnabled(unfiltered != null);
            cmdExportFiltered.setEnabled(unfiltered != null);
            lblReliefFSampleSize.setEnabled(!chkReliefFWholeDataset.isSelected());
            spnReliefFSampleSize.setEnabled(!chkReliefFWholeDataset.isSelected());
            spnEvaluations.setEnabled(rdoEvaluations.isSelected());
            spnRuntime.setEnabled(rdoRuntime.isSelected());
            chkPairedAnalysis.setEnabled(data.canBePaired());
            lblUnits.setEnabled(rdoRuntime.isSelected());
            cboUnits.setEnabled(rdoRuntime.isSelected());
            cmdResetEntropyGraph.setEnabled(!isDefaultEntropyGraph());
            setCursor(Cursor.getDefaultCursor());
            }
        }

    public void pnlAllModelsLandscape_actionPerformed(final ActionEvent e)
        {
        final Component c = getContentPane().getComponent(0);
        if (c == scpMain)
            {
            getContentPane().remove(scpMain);
            tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, false);
            getContentPane().add(pnlAllModelsLandscape, BorderLayout.CENTER);
            }
        else
            {
            tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, true);
            tpnResults.setSelectedComponent(pnlAllModelsLandscape);
            getContentPane().add(scpMain, BorderLayout.CENTER);
            }
        invalidate();
        validate();
        repaint();
        }

    public void pnlConstruct_stateChanged(final ChangeEvent e)
        {
        dataFileChange();
        }

    public void pnlFilterLandscape_actionPerformed(final ActionEvent e)
        {
        final Component c = getContentPane().getComponent(0);
        if (c == scpMain)
            {
            getContentPane().remove(scpMain);
            getContentPane().add(pnlFilterLandscape, BorderLayout.CENTER);
            }
        else
            {
            tpnFilter.add(pnlFilterLandscape, "Landscape", 1);
            tpnFilter.setSelectedComponent(pnlFilterLandscape);
            getContentPane().add(scpMain, BorderLayout.CENTER);
            }
        invalidate();
        validate();
        repaint();
        }

    public void pnlGraphicalModel_actionPerformed(final ActionEvent e)
        {
        final Component c = getContentPane().getComponent(0);
        if (c == scpMain)
            {
            getContentPane().remove(scpMain);
            tpnResults.setOrderedTabVisible(pnlGraphicalModel, false);
            getContentPane().add(pnlGraphicalModel, BorderLayout.CENTER);
            }
        else
            {
            tpnResults.setOrderedTabVisible(pnlGraphicalModel, true);
            tpnResults.setSelectedComponent(pnlGraphicalModel);
            getContentPane().add(scpMain, BorderLayout.CENTER);
            }
        invalidate();
        validate();
        repaint();
        }

    public void pnlTopModelsLandscape_actionPerformed(final ActionEvent e)
        {
        final Component c = getContentPane().getComponent(0);
        if (c == scpMain)
            {
            getContentPane().remove(scpMain);
            tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, false);
            getContentPane().add(pnlTopModelsLandscape, BorderLayout.CENTER);
            }
        else
            {
            tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, true);
            tpnResults.setSelectedComponent(pnlTopModelsLandscape);
            getContentPane().add(scpMain, BorderLayout.CENTER);
            }
        invalidate();
        validate();
        repaint();
        }

    public void rdoEvaluations_stateChanged(final ChangeEvent e)
        {
        final boolean enabled = rdoEvaluations.isSelected();
        spnEvaluations.setEnabled(enabled);
        spnRuntime.setEnabled(!enabled);
        lblUnits.setEnabled(!enabled);
        cboUnits.setEnabled(!enabled);
        }

    private void resetForm()
        {
        dtmSummaryTable.setRowCount(0);
        prgProgress.setValue(0.0);
        prgFilterProgress.setValue(unfiltered == null ? 0 : 1);
        clearTabs(false);
        tpnResults.setSelectedComponent(pnlGraphicalModel);
        pnlConstruct.setWarnOnChange(false);
        }

    private void setComboAmbiguousStatus(
            final Console.AmbiguousCellStatus tiePriority)
        {
        for (int index = 0; index < cboAmbiguousCellStatuses.getItemCount(); ++index)
            {
            final Console.AmbiguousCellStatus cboItemTiePriority = getComboAmbiguousStatus(index);
            if (tiePriority == cboItemTiePriority)
                {
                cboAmbiguousCellStatuses.setSelectedIndex(index);
                break;
                }
            }
        }

    public void setConfigToDefaults()
        {
        cmdDefaults.setEnabled(!isDefaultConfig(true));
        // spnRandomSeed.setValue(new Long(Main.defaultRandomSeed));
        // spnAttributeCountMin.setValue(new Integer(Main.defaultAttributeCountMin));
        // spnAttributeCountMax.setValue(new Integer(Main.defaultAttributeCountMax));
        // chkPairedAnalysis.setSelected(Main.defaultPairedAnalysis);
        // spnTopModels.setValue(new Integer(Main.defaultTopModelsLandscapeSize));
        // spnCrossValidationCount.setValue(new Integer(
        // Main.defaultCrossValidationCount));
        // cboAmbiguousCellStatuses.setSelectedIndex(Main.defaultTieCellsIndex);
        // if (Main.defaultForcedAttributeCombination == null) {
        // forced = null;
        // } else {
        // forced = new AttributeCombination(Main.defaultForcedAttributeCombination
        // .getComboString(), data.getLabels());
        // }
        // cboSearchType.setSelectedIndex(Main.defaultSearchTypeIndex);
        // rdoEvaluations.setSelected(Main.defaultIsRandomSearchEvaluations);
        // rdoRuntime.setSelected(Main.defaultIsRandomSearchRuntime);
        // spnEvaluations.setValue(new Integer(Main.defaultRandomSearchEvaluations));
        // spnRuntime.setValue(new Double(Main.defaultRandomSearchRuntime));
        // cboUnits.setSelectedIndex(Main.defaultRandomSearchRuntimeUnitsIndex);
        // txtForcedAttributeCombination.setText(forced == null ? "" : forced
        // .getComboString());
        // spnNumAgents.setValue(new Integer(Main.defaultEDANumAgents));
        // spnNumUpdates.setValue(new Integer(Main.defaultEDANumUpdates));
        // spnRetention.setValue(Main.defaultEDARetention);
        // spnAlpha.setValue(Main.defaultEDAAlpha);
        // spnBeta.setValue(Main.defaultEDABeta);
        // expertKnowledgeSetEnabled("", null);
        }

    private void setDataDependentItemsEnabled(final boolean enabled)
        {
        for (int index = 1; index < tpnAnalysis.getComponentCount(); ++index)
            {
            if (tpnAnalysis.getComponent(index) != pnlAbout)
                {
                tpnAnalysis.setEnabledAt(index, enabled);
                }
            }
        }

    public void setFilterToDefaults()
        {
        cmdFilter.setEnabled(!isDefaultFilter(true));
        // cboFilter.setSelectedIndex(Main.defaultFilterIndex);
        // cboCriterion.setSelectedIndex(Main.defaultCriterionIndex);
        // snmTopN.setValue(new Integer(Main.defaultCriterionFilterTopN));
        // snmTopPct.setValue(new Double(Main.defaultCriterionFilterTopPct));
        // snmThreshold.setValue(new Double(Main.defaultCriterionFilterThreshold));
        // chkChiSquaredPValue.setSelected(Main.defaultChiSquaredPValue);
        // spnCriterionFilter.setModel(snmTopN);
        // chkReliefFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        // spnReliefFNeighbors.setValue(new Integer(Main.defaultReliefFNeighbors));
        // spnReliefFSampleSize.setValue(new Integer(Main.defaultReliefFSampleSize));
        // chkTuRFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        // spnTuRFNeighbors.setValue(new Integer(Main.defaultReliefFNeighbors));
        // spnTuRFSampleSize.setValue(new Integer(Main.defaultReliefFSampleSize));
        // spnTuRFPercent.setValue(new Integer(Main.defaultTuRFPct));
        // chkSURFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        // spnSURFSampleSize.setValue(new Integer(Main.defaultReliefFSampleSize));
        // chkSURFStarWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        // spnSURFStarSampleSize.setValue(new Integer(Main.defaultReliefFSampleSize));
        // chkSURFnTuRFWholeDataset.setSelected(Main.defaultReliefFWholeDataset);
        // spnSURFnTuRFSampleSize.setValue(new Integer(Main.defaultReliefFSampleSize));
        // spnSURFnTuRFPercent.setValue(new Integer(Main.defaultTuRFPct));
        }

    private void setModelDependentItemsEnabled(final boolean enabled)
        {
        tpnResults.setEnabled(enabled);
        }

    private void setTestingAndCvcColumnsDisplay(
            final boolean displayTestingColumn, final boolean displayCvcColumn,
            final AmbiguousCellStatus ambiguousCellStatus)
        {
        if (ambiguousCellStatus == AmbiguousCellStatus.UNASSIGNED)
            {
            tbcTrainingValue.setHeaderValue("Training Adj. Bal. Acc.");
            tbcTestingValue.setHeaderValue("Testing Adj. Bal. Acc.");
            }
        else
            {
            tbcTrainingValue.setHeaderValue("Training Bal. Acc.");
            tbcTestingValue.setHeaderValue("Testing Bal. Acc.");
            }
        // redraw column headers
        tblSummaryTable.getTableHeader().resizeAndRepaint();
        if (displayTestingColumn)
            {
            if ((tblSummaryTable.getColumnCount() <= 2)
                || (tblSummaryTable.getColumnName(2).indexOf("Testing") == -1))
                {
                tblSummaryTable.getColumnModel().addColumn(tbcTestingValue);
                tpnResults.setOrderedTabVisible(pnlCVResults, true);
                }
            }
        else
            {
            tblSummaryTable.getColumnModel().removeColumn(tbcTestingValue);
            tpnResults.setOrderedTabVisible(pnlCVResults, false);
            }
        if (displayCvcColumn)
            {
            if (tblSummaryTable.getColumnName(tblSummaryTable.getColumnCount() - 1) != Frame.CV_CONSISTENCY)
                {
                tblSummaryTable.getColumnModel().addColumn(tbcCVConsistency);
                }
            }
        else
            {
            tblSummaryTable.getColumnModel().removeColumn(tbcCVConsistency);
            }
        }

    public void spnAttributeCount_stateChanged(final ChangeEvent e)
        {
        final Integer min = (Integer) spnAttributeCountMin.getValue();
        final Integer max = (Integer) spnAttributeCountMax.getValue();
        if (min.compareTo(max) > 0)
            {
            if (e.getSource() == spnAttributeCountMin)
                {
                spnAttributeCountMax.setValue(min);
                }
            else
                {
                spnAttributeCountMin.setValue(max);
                }
            }
        }

    public void tblSummaryTable_selectionChanged(final ListSelectionEvent e)
        {
        final List<Collector> collectors = loadedCollectors == null ? (analysis == null ? null
                                                                                        : analysis.getCollectors())
                                                                    : loadedCollectors;
        if (collectors == null)
            {
            return;
            }
        Console.AmbiguousCellStatus tiePriority;
        Console.ScoringMethod scoringMethod;
        if (analysis == null)
            {
            tiePriority = Main.defaultAmbiguousCellStatus;
            scoringMethod = Main.defaultScoringMethod;
            }
        else
            {
            tiePriority = analysis.getTiePriority();
            scoringMethod = analysis.getScoringMethod();
            }
        final int row = tblSummaryTable.getSelectedRow();
        if (row < 0)
            {
            clearTabs(true);
            }
        else
            {
            final int intervals = collectors.get(0).size();
            new TextComponentUpdaterThread(txaBestModel, new BestModelTextGenerator(
                    data, tiePriority, scoringMethod, collectors.get(row).getBest(),
                    intervals, Main.modelTextNumberFormat, Main.pValueTol,
                    chkBestModelVerbose.isSelected()), new ComponentEnabler(
                    cmdBestModelSave, true)).start();
            if (intervals > 1)
                {
                new TextComponentUpdaterThread(txaCVResults,
                                               new CVResultsTextGenerator(collectors.get(row), tiePriority,
                                                                          scoringMethod, data.getAffectedStatus(),
                                                                          Main.modelTextNumberFormat, Main.pValueTol,
                                                                          chkCVResultsVerbose.isSelected()), new ComponentEnabler(
                        cmdCVResultsSave, true)).start();
                }
            new TextComponentUpdaterThread(txaIfThenRules,
                                           new IfThenRulesTextGenerator(data, collectors.get(row).getBest()),
                                           new ComponentEnabler(cmdIfThenRulesSave, true)).start();
            pnlGraphicalModel.setModel(collectors.get(row).getBest().getModel());
            setModelDependentItemsEnabled(true);
            pnlGraphicalModel.setEnabled(true);
            if (collectors.get(row).getTopModelsLandscape() != null)
                {
                tpnResults.setOrderedTabVisible(pnlTopModelsLandscape, true);
                pnlTopModelsLandscape.setLandscape(collectors.get(row)
                                                           .getTopModelsLandscape().getLandscape(), false);
                pnlTopModelsLandscape.setEnabled(true);
                }
            }
        }

    public void txtForcedAttributeCombination_focusLost(final FocusEvent e)
        {
        if ((e.getOppositeComponent() == null)
            || (e.getOppositeComponent() == cmdDefaults)
            || (txtForcedAttributeCombination.getText().length() == 0))
            {
            forced = null;
            return;
            }
        try
            {
            forced = new AttributeCombination(
                    txtForcedAttributeCombination.getText(), data.getLabels());
            txtForcedAttributeCombination.setText(forced.getComboString());
            }
        catch (final IllegalArgumentException ex)
            {
            ex.fillInStackTrace();
            System.err.println("Caught exception: " + ex.toString());
            JOptionPane
                    .showMessageDialog(
                            this,
                            "Forced Attribute Combination accepts  "
                            + "lists of attribute labels or attribute column numbers."
                            + ((unfiltered != null) ? " Perhaps attributes are currently filtered out?"
                                                    : ""), "Invalid Attribute Combination",
                            JOptionPane.ERROR_MESSAGE);
            txtForcedAttributeCombination.selectAll();
            txtForcedAttributeCombination.requestFocus();
            tpnAnalysis.setSelectedComponent(pnlConfiguration);
            return;
            }
        }

    private void updateEntropyDisplay(final Dataset pData,
                                      final List<Collector> modelFilter, final int min, final int max,
                                      final AmbiguousCellStatus tiePriority)
        {
        final Set<AttributeCombination> comboSet = new TreeSet<AttributeCombination>();
        for (int i = min; i <= max; ++i)
            {
            final AttributeCombination combo = modelFilter.get(i - min).getBest()
                    .getModel().getCombo();
            if (combo.size() > 1)
                {
                for (int j = 0; j < combo.size(); ++j)
                    {
                    comboSet.add(new AttributeCombination(Collections.singletonList(combo
                                                                                            .get(j)), pData.getLabels()));
                    }
                }
            else
                {
                comboSet.add(combo);
                }
            }
        final List<AttributeCombination> combos = new ArrayList<AttributeCombination>(
                comboSet);
        if (combos.size() > 1)
            {
            entropyAnalysis.setTiePriority(tiePriority);
            entropyAnalysis.set(combos, pData);
            txaRawEntropyValues.setText(entropyAnalysis
                                                .getEntropyText(Main.modelTextNumberFormat));
            txaRawEntropyValues.select(0, 0);
            cmdSaveEntropy.setEnabled(true);
            cboEntropy.setEnabled(true);
            }
        else
            {
            entropyAnalysis.clear();
            txaRawEntropyValues.setText("");
            cmdSaveEntropy.setEnabled(false);
            lblEntropyGraphLineThickness.setEnabled(false);
            spnEntropyGraphLineThickness.setEnabled(false);
            lblEntropyGraphTextSize.setEnabled(false);
            spnEntropyGraphTextSize.setEnabled(false);
            spnMinimumAbsoluteEntropy.setEnabled(false);
            cboEntropy.setEnabled(false);
            }
        final Component component = entropyPanelEntropyDisplay.get(cboEntropy
                                                                           .getSelectedItem().toString());
        if (component instanceof EntropyDisplay)
            {
            final EntropyDisplay entropyDisplay = (EntropyDisplay) component;
            if (entropyDisplay.supportEntropyThreshold())
                {
                ((SpinnerListModel) (spnMinimumAbsoluteEntropy.getModel()))
                        .setList(entropyDisplay.getMinimumAbsoluteEntropyPercentValuesSet());
                }
            entropyDisplay.updateGraph();
            }
        }

    public class AmbiguousCellCriteriaActionListener implements ActionListener
        {
        public void actionPerformed(final ActionEvent e)
            {
            spnFisherThreshold.setEnabled(!rdoAmbiguousCellCriteriaTieCells
                    .isSelected());
            if (spnFisherThreshold.isEnabled())
                {
                Console.fishersThreshold = ((Double) spnFisherThreshold.getValue())
                        .floatValue();
                }
            else
                {
                Console.fishersThreshold = Main.defaultFishersThreshold;
                }
            }
        }

    private class cmdViewExpertKnowledge implements ActionListener
        {
        public void actionPerformed(final ActionEvent e)
            {
            frmExpertKnowledge.setVisible(true);
            createExpertKnowledgeRuntime();
            frmExpertKnowledge.readDatafile(expertKnowledge);
            frmExpertKnowledge.toFront();
            }
        }

    enum DisplayType
        {
            Dendogram("Dendogram"), ForceDirectedGraph(
                InteractionGraph.NetworkType.FORCE_DIRECTED_GRAPH.getDisplayName()), RadialTree(
                InteractionGraph.NetworkType.RADIAL_TREE.getDisplayName()), CircleGraph(
                InteractionGraph.NetworkType.CIRCLE_LAYOUT.getDisplayName()), Raw_Entropy_Values(
                "Raw Values");
        private final String displayName;
        private final static Map<String, DisplayType> displayTypeLookupMap = new HashMap<String, DisplayType>();

        static
            {
            for (final DisplayType displayType : DisplayType.values())
                {
                DisplayType.displayTypeLookupMap.put(displayType.toString(),
                                                     displayType);
                }
            }

        public static DisplayType lookup(final String pDisplayName)
            {
            return DisplayType.displayTypeLookupMap.get(pDisplayName);
            }

        DisplayType(final String pDisplayName)
            {
            displayName = pDisplayName;
            }

        @Override
        public String toString() {
        return displayName;
        }
        } // end enum DisplayType

    private class Frame_cboCriterion_itemAdapter implements ItemListener
        {
        private final Frame adaptee;

        Frame_cboCriterion_itemAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void itemStateChanged(final ItemEvent e)
            {
            adaptee.cboCriterion_itemStateChanged(e);
            }
        }

    private class Frame_cboFilter_itemAdapter implements ItemListener
        {
        private final Frame adaptee;

        Frame_cboFilter_itemAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void itemStateChanged(final ItemEvent e)
            {
            adaptee.cboFilter_itemStateChanged(e);
            }
        }

    private class Frame_cboSearchType_itemAdapter implements ItemListener
        {
        private final Frame adaptee;

        Frame_cboSearchType_itemAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void itemStateChanged(final ItemEvent e)
            {
            adaptee.cboSearchType_itemStateChanged(e);
            }
        }

    private class Frame_changeAdapter implements ChangeListener
        {
        private final Frame adaptee;

        Frame_changeAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void stateChanged(final ChangeEvent e)
            {
            adaptee.checkConfigDefaultsOnEvent(e);
            }
        }

    private class Frame_chkPairedAnalysis_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_chkPairedAnalysis_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.chkPairedAnalysis_actionPerformed(e);
            }
        }

    private class Frame_cmdBestModelSave_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdBestModelSave_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdBestModelSave_actionPerformed(e);
            }
        }

    private class Frame_cmdCVResultsSave_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdCVResultsSave_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdCVResultsSave_actionPerformed(e);
            }
        }

    private class Frame_cmdExportFiltered_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdExportFiltered_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdExportFiltered_actionPerformed(e);
            }
        }

    private class Frame_cmdIfThenRulesSave_actionAdapter implements
                                                         ActionListener
        {
        private final Frame adaptee;

        Frame_cmdIfThenRulesSave_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdIfThenRulesSave_actionPerformed(e);
            }
        }

    private class Frame_cmdLoadAnalysis_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdLoadAnalysis_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdLoadAnalysis_actionPerformed(e);
            }
        }

    private class Frame_cmdLoadDatafile_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdLoadDatafile_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdLoadDatafile_actionPerformed(e);
            }
        }

    private class Frame_cmdLoadExpertKnowledge implements ActionListener
        {
        public void actionPerformed(final ActionEvent e)
            {
            Frame.fileChooser.setMultiSelectionEnabled(false);
            Frame.fileChooser.setDialogTitle("Load Scores");
            Frame.fileChooser.addChoosableFileFilter(FileSaver.txtFilter);
            if (Frame.fileChooser.showOpenDialog(Frame.this) == JFileChooser.APPROVE_OPTION)
                {
                try
                    {
                    final ExpertKnowledge newExpertKnowledge = new ExpertKnowledge(
                            new FileReader(Frame.fileChooser.getSelectedFile()),
                            data.getLabels());
                    expertKnowledgeSetEnabled(Frame.fileChooser.getSelectedFile()
                                                      .toString(), newExpertKnowledge);
                    spnBeta.setEnabled(true);
                    cmdViewExpertKnowledge.setEnabled(true);
                    final List<String> nonMatchingExpertKnowledgeAttributes = expertKnowledge
                            .getNonMatchingExpertKnowledgeAttributes();
                    if (nonMatchingExpertKnowledgeAttributes.size() > 0)
                        {
                        JOptionPane
                                .showMessageDialog(
                                        Frame.this,
                                        nonMatchingExpertKnowledgeAttributes.size()
                                        + " attribute names from the expert knowledge file could not be matched to data file attributes. Unmatched expert knowledge attributes: "
                                        + nonMatchingExpertKnowledgeAttributes.toString(),
                                        "Unmatched Expert Knowledge scores",
                                        JOptionPane.WARNING_MESSAGE);
                        }
                    if (newExpertKnowledge.size() != data.getCols() - 1)
                        {
                        JOptionPane
                                .showMessageDialog(
                                        Frame.this,
                                        ((data.getCols() - 1) - newExpertKnowledge.size())
                                        + " data set attributes do not have an expert knowledge score provided",
                                        "Missing Expert Knowledge scores for data file attributes",
                                        JOptionPane.WARNING_MESSAGE);
                        }
                    }
                catch (final IOException ex)
                    {
                    Utility.logException(ex);
                    JOptionPane.showMessageDialog(
                            Frame.this,
                            "Unable to open file '"
                            + Frame.fileChooser.getSelectedFile().toString()
                            + "' or unrecognized file format.\n\nMessage:\n"
                            + ex.getMessage(), "Datafile Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    private class Frame_cmdRevertFilter_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdRevertFilter_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdRevertFilter_actionPerformed(e);
            }
        }

    private class Frame_cmdSaveEntropyDisplay_actionAdapter implements
                                                            ActionListener
        {
        private final Frame adaptee;

        Frame_cmdSaveEntropyDisplay_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdSaveEntropyDisplay_actionPerformed(e);
            }
        }

    private class Frame_cmdViewDatafile_actionAdapter implements ActionListener
        {
        private final Frame adaptee;

        Frame_cmdViewDatafile_actionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void actionPerformed(final ActionEvent e)
            {
            adaptee.cmdViewDatafile_actionPerformed(e);
            }
        }

    private class Frame_itemAdapter implements ItemListener
        {
        private final Frame adaptee;

        Frame_itemAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void itemStateChanged(final ItemEvent e)
            {
            adaptee.checkConfigDefaultsOnEvent(e);
            }
        }

    private class Frame_lblLogo_mouseAdapter extends MouseAdapter
        {
        private final Frame adaptee;

        Frame_lblLogo_mouseAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        @Override
        public void mouseClicked(final MouseEvent e) {
        adaptee.lblLogo_mouseClicked(e);
        }
        }

    private class Frame_rdoEvaluations_changeAdapter implements ChangeListener
        {
        private final Frame adaptee;

        Frame_rdoEvaluations_changeAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void stateChanged(final ChangeEvent e)
            {
            adaptee.rdoEvaluations_stateChanged(e);
            }
        }

    private class Frame_spnAttributeCount_changeAdapter implements ChangeListener
        {
        private final Frame adaptee;

        Frame_spnAttributeCount_changeAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void stateChanged(final ChangeEvent e)
            {
            adaptee.spnAttributeCount_stateChanged(e);
            }
        }

    private class Frame_tblSummaryTable_selectionAdapter implements
                                                         ListSelectionListener
        {
        private final Frame adaptee;

        Frame_tblSummaryTable_selectionAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        public void valueChanged(final ListSelectionEvent e)
            {
            adaptee.tblSummaryTable_selectionChanged(e);
            }
        }

    private class Frame_txtForcedAttributeCombination_focusAdapter extends
                                                                   FocusAdapter
        {
        private final Frame adaptee;

        Frame_txtForcedAttributeCombination_focusAdapter(final Frame adaptee)
            {
            this.adaptee = adaptee;
            }

        @Override
        public void focusLost(final FocusEvent e) {
        adaptee.txtForcedAttributeCombination_focusLost(e);
        }
        }

    private class LandscapeSpinnerModel extends SpinnerNumberModel
        {
        private static final long serialVersionUID = 1L;

        LandscapeSpinnerModel()
            {
            super();
            setValue(Main.defaultTopModelsLandscapeSize);
            }

        @Override
        public Object getNextValue() {
        final int currentValue = Integer.parseInt(getValue().toString());
        final int stepSize = getStepSize(currentValue);
        final int nextValue = currentValue + stepSize;
        return nextValue;
        }

        @Override
        public Object getPreviousValue() {
        final int currentValue = Integer.parseInt(getValue().toString());
        final int stepSize = getStepSize(currentValue - 1);
        final int previousValue = currentValue - stepSize;
        return previousValue;
        }

        private int getStepSize(final int rangeDeterminingValue)
            {
            int stepSize;
            if (rangeDeterminingValue >= 1000)
                {
                stepSize = 100;
                }
            else if (rangeDeterminingValue >= 100)
                {
                stepSize = 50;
                }
            else if (rangeDeterminingValue >= 50)
                {
                stepSize = 10;
                }
            else if (rangeDeterminingValue >= 10)
                {
                stepSize = 5;
                }
            else if (rangeDeterminingValue >= 0)
                {
                stepSize = 1;
                }
            else
                {
                stepSize = 0;
                }
            return stepSize;
            }
        }

    private class MemoryDisplay implements Runnable
        {
        /**
         * NumberFormat used to add commas in the appropriate places.
         */
        private final NumberFormat nf = new DecimalFormat(",###.#");
        /**
         * Maximum heap size in bytes.
         */
        private long max;
        /**
         * Current heap size in bytes.
         */
        private long total;
        /**
         * Number of bytes used.
         */
        private long used;

        /**
         * Update the memory usage fields.
         */
        public void run()
            {
            lblUsedMem.setText(nf.format(used / Frame.BytesPerMegabyte) + " MB");
            lblTotalMem.setText(nf.format(total / Frame.BytesPerMegabyte) + " MB");
            lblMaxMem.setText(nf.format(max / Frame.BytesPerMegabyte) + " MB");
            }

        /**
         * Set the values which will be displayed.
         *
         * @param used  number of bytes used
         * @param total current heap size in bytes
         * @param max   maximum heap size in bytes
         */
        public void setValues(final long used, final long total, final long max)
            {
            this.used = used;
            this.total = total;
            this.max = max;
            }
        }

    /**
     * Runnable which loops until interrupted and updates the memory usage fields on the panel.
     */
    private class MemoryMonitor implements Runnable
        {
        private SoftReference<byte[]> sentinelMemory;

        public MemoryMonitor()
            {
            grabSentinelMemory();
            }

        private void grabSentinelMemory()
            {
            sentinelMemory = new SoftReference<byte[]>(
                    new byte[Frame.MemorySentinelBytes]);
            }

        /**
         * Run the loop.
         */
        public void run()
            {
            final MemoryDisplay memDisp = new MemoryDisplay();
            final Runtime rt = Runtime.getRuntime();
            while (!Thread.interrupted())
                {
                try
                    {
                    final long maxMem = rt.maxMemory();
                    final long totalMem = rt.totalMemory();
                    final long freeMem = rt.freeMemory();
                    final long usedMem = totalMem - freeMem;
                    memDisp.setValues(usedMem, totalMem, maxMem);
                    SwingUtilities.invokeAndWait(memDisp);
                    if (sentinelMemory != null)
                        { // don't warn user more than once
                        if (sentinelMemory.get() == null)
                            { // if the SoftReference was taken then check further
                            final long spareCapacity = maxMem - usedMem;
                            if (spareCapacity < Frame.MemorySentinelBytes)
                                {
                                sentinelMemory = null; // only show warning once
                                System.err.println(Frame.MemorySentinelWarningMessage);
                                JOptionPane.showMessageDialog(Frame.this,
                                                              Frame.MemorySentinelWarningMessage,
                                                              "Running out of memory!", JOptionPane.WARNING_MESSAGE);
                                }
                            else
                                {
                                // the sentinel memory was probably taken before virtual machine had expanded to use all memory so re-establish sentinel
                                grabSentinelMemory();
                                }
                            } // end if sentinel memory was taken
                        }
                    else
                        {
                        if (pnlUsedMem.getBackground() == getBackground())
                            {
                            pnlUsedMem.setBackground(Color.RED);
                            }
                        else
                            {
                            pnlUsedMem.setBackground(getBackground());
                            }
                        } // end if sentinel memory warning has been made
                    Thread.sleep(1000);
                    }
                catch (final InterruptedException ex)
                    {
                    break;
                    }
                catch (final InvocationTargetException ex)
                    {
                    ex.printStackTrace();
                    }
                }
            }
        }

    private class OnEnd implements Runnable
        {
        public void run()
            {
            if (tmrProgress != null)
                {
                tmrProgress.cancel();
                tmrProgress = null;
                }
            if ((analysis != null) && analysis.isComplete())
                {
                prgProgress.setValue(1);
                }
            updateEntropyDisplay(data, analysis.getCollectors(),
                                 analysis.getMinAttr(),
                                 analysis.getMinAttr() + tblSummaryTable.getRowCount() - 1,
                                 analysis.getTiePriority());
            if (analysis.getAllModelsLandscape() != null)
                {
                tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, true);
                pnlAllModelsLandscape.setEnabled(true);
                pnlAllModelsLandscape.setLandscape(analysis.getAllModelsLandscape()
                                                           .getLandscape(), false);
                }
            else
                {
                tpnResults.setOrderedTabVisible(pnlAllModelsLandscape, false);
                }
            cmdRunAnalysis.setText("Run Analysis");
            cmdRunAnalysis.setForeground(cmdLoadDatafile.getForeground());
            lockdown(false);
            }
        }

    private class OnEndAttribute implements Runnable
        {
        public void run()
            {
            int attrCount = tblSummaryTable.getRowCount();
            final int minLevel = ((Number) spnAttributeCountMin.getValue())
                    .intValue();
            if (forced == null)
                {
                attrCount += minLevel;
                }
            else
                {
                attrCount += forced.size();
                }
            final Collector currentCollector = analysis.getCollectors().get(
                    tblSummaryTable.getRowCount());
            addTableRow(currentCollector.getBest(),
                        ((Number) spnCrossValidationCount.getValue()).intValue());
            if (Console.computeMdrByGenotype)
                {
                final List<MdrByGenotypeResults> mdrByGenotypeResults = currentCollector
                        .getMdrByGenotypeResults();
                if (tblSummaryTable.getRowCount() == 1)
                    {
                    System.out.println("# Attributes\t"
                                       + Collector.MdrByGenotypeResults.getTabDelimitedHeaderString());
                    }
                final int maxLevel = ((Number) spnAttributeCountMax.getValue())
                        .intValue();
                for (final MdrByGenotypeResults mdrByGenotypeResult : mdrByGenotypeResults)
                    {
                    System.out.println(minLevel + "-" + maxLevel + "\t"
                                       + mdrByGenotypeResult.getTabDelimitedRowString());
                    }
                }
            System.gc();
            }
        }

    private class OnFilterEnd implements Runnable
        {
        private final boolean ascending;

        public OnFilterEnd(final boolean ascending)
            {
            this.ascending = ascending;
            }

        public void run()
            {
            if (!isVisible())
                {
                return;
                }
            final AttributeCombination combo = rankerThread.getCombo();
            lockdown(false);
            cmdFilter.setText("Apply");
            cmdFilter.setForeground(getForeground());
            if ((combo == null) || (combo.size() == 0))
                {
                JOptionPane.showMessageDialog(Frame.this,
                                              "Current filter criteria selected zero attributes "
                                              + "-- not applying.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
                }
            final List<String> labels = data.getLabels();
            data = data.filter(combo.getAttributeIndices());
            dataFileChange();
            prgFilterProgress.setValue(1);
            cmdRevertFilter.setEnabled(true);
            cmdRevert.setEnabled(true);
            if (cboFilter.getSelectedItem().toString().equals("\u03a7\u00b2")
                && chkChiSquaredPValue.isSelected())
                {
                pnlFilterLandscape.setYAxisLabel("P-Value");
                }
            else
                {
                pnlFilterLandscape
                        .setYAxisLabel(cboFilter.getSelectedItem().toString());
                }
            pnlFilterLandscape.setEnabled(true);
            Comparator<Pair<String, Float>> cmp = new Pair.SecondComparator<String, Float>();
            if (!ascending)
                {
                cmp = Collections.reverseOrder(cmp);
                }
            final List<Pair<Integer, Double>> scores = rankerThread.getRanker()
                    .getSortedScores();
            final List<Pair<String, Float>> landscape = new ArrayList<Pair<String, Float>>(
                    scores.size());
            for (final Pair<Integer, Double> p : scores)
                {
                landscape.add(new Pair<String, Float>(labels.get(p.getFirst()),
                                                      new Float(p.getSecond())));
                }
            pnlFilterLandscape.setLandscape(landscape, cmp);
            }
        }
    } // end Frame
