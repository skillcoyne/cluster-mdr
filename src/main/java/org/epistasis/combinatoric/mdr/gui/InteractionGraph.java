package org.epistasis.combinatoric.mdr.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.epistasis.combinatoric.EntropyAnalysis;
import org.epistasis.combinatoric.HierarchicalCluster;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.jibble.epsgraphics.EpsGraphics2D;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.layout.CircleLayout;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.PrefuseLib;
import prefuse.util.UpdateListener;
import prefuse.util.force.SpringForce;
import prefuse.visual.DecoratorItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;

public class InteractionGraph extends Display implements EntropyDisplay
    {
    private static final long serialVersionUID = 1L;
    private static final String[] TREE_GROUPS = {"tree", "tree.nodes",
            "tree.edges"};
    private static final String[] GRAPH_GROUPS = {"graph", "graph.nodes",
            "graph.edges"};
    public static final String EDGE_DECORATORS = "edgeDeco";
    private final static Schema NODE_SCHEMA = PrefuseLib.getVisualItemSchema();
    private final static int NODE_COLUMN_INDEX = InteractionGraph.NODE_SCHEMA
            .getColumnCount();
    private final static int NODE_NAME_COLUMN_INDEX = InteractionGraph.NODE_COLUMN_INDEX;
    private final static int NODE_VALUE_COLUMN_INDEX = InteractionGraph.NODE_COLUMN_INDEX + 3;
    private final static Schema EDGE_SCHEMA = PrefuseLib.getVisualItemSchema();
    private final static int EDGE_COLUMN_INDEX = InteractionGraph.EDGE_SCHEMA
            .getColumnCount();
    private final static int EDGE_VALUE_COLUMN_INDEX = InteractionGraph.EDGE_COLUMN_INDEX + 5;
    private final static int EDGE_INTERACTION_BIN_COLUMN_INDEX = InteractionGraph.EDGE_COLUMN_INDEX + 7;
    private static final Schema DECORATOR_SCHEMA = PrefuseLib
            .getMinimalVisualSchema();

    static
        {
        InteractionGraph.NODE_SCHEMA.addColumn("Attribute", String.class);
        InteractionGraph.NODE_SCHEMA.addColumn("H(A)", double.class);
        InteractionGraph.NODE_SCHEMA.addColumn("H(A|C)", double.class);
        InteractionGraph.NODE_SCHEMA.addColumn("I(A;C)", double.class);
        InteractionGraph.NODE_SCHEMA.setDefault(VisualItem.FILLCOLOR,
                                                ColorLib.rgba(255, 255, 255, 128)); // half
        // transparent
        // NODE_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("SansSerif",
        // Font.BOLD, 16));
        }

    static
        {
        InteractionGraph.EDGE_SCHEMA.addColumn("Attribute A", int.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("Attribute B", int.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("H(AB)", double.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("H(AB|C)", double.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("I(A;B)", double.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("I(A;B;C)", double.class); // main edge
        InteractionGraph.EDGE_SCHEMA.addColumn("I(AB;C)", double.class);
        InteractionGraph.EDGE_SCHEMA.addColumn("I(A;B;C) color bin", int.class);
        }

    private final EntropyAnalysis entropyAnalysis;
    private int margin = 5;
    private int mostInformativeNodeRowNumber = -1;
    private List<Double> minimumAbsoluteEntropyPercentValuesList = null;
    private final NetworkType networkType;
    // static {
    // DECORATOR_SCHEMA.setDefault(VisualItem.INTERACTIVE, false);
    // DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(64));
    // DECORATOR_SCHEMA.setDefault(VisualItem.FILLCOLOR, ColorLib.rgba(255, 255,
    // 255, 128));
    // DECORATOR_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("SansSerif",
    // Font.PLAIN, 32));
    // }
    private int lineThickness;
    private double minimumAbsoluteEntropyPercent = Double.NaN;
    private double defaultMinimumAbsoluteEntropyPercent;
    private final UpdateListener resizeListener = new UpdateListener()
    {
    @Override
    public void componentResized(final java.awt.event.ComponentEvent e) {
    getVisualization().run("layout");
    }

    @Override
    public void update(final Object src) {
    }
    };
    private final NumberFormat numberFormat;

    public InteractionGraph(final NumberFormat pNumberFormat, final int pMargin,
                            final EntropyAnalysis pEntropyAnalysis, final NetworkType pNetworkType)
        {
        super(new Visualization()); // create empty Display
        numberFormat = pNumberFormat;
        setMargin(pMargin);
        setHighQuality(true); // turn on anti-aliasing (slower)
        entropyAnalysis = pEntropyAnalysis;
        networkType = pNetworkType;
        setFontSize(Main.defaultEntropyGraphTextSize);
        setLineThickness(Main.defaultEntropyGraphLineThickness);
        addControlListener(new PanControl());
        addControlListener(new CustomComboControl("draw", "drag", "click"));
        setUpDisplay(getEntropyGraph());
        }

    private boolean doClick()
        {
        return networkType.doClick;
        }

    private boolean doDrag()
        {
        return networkType.doDrag;
        }

    public double getDefaultMinimumAbsoluteEntropyPercent()
        {
        return defaultMinimumAbsoluteEntropyPercent;
        }

    private Graph getEntropyGraph()
        {
        final List<String> attrNames = entropyAnalysis.getAttrNames();
        final double[][] entropy = entropyAnalysis.getEntropy();
        final double[][] cond_entropy = entropyAnalysis.getCond_entropy();
        final double class_entropy = entropyAnalysis.getClass_entropy();
        final double[] class_cond_entropy = entropyAnalysis.getClass_cond_entropy();
        final HierarchicalCluster<AttributeCombination> cluster = entropyAnalysis
                .getCluster();
        final Table nodes = InteractionGraph.NODE_SCHEMA.instantiate();
        final Table edges = InteractionGraph.EDGE_SCHEMA.instantiate();
        Graph entropyGraph = null;
        if (cluster != null)
            {
            double mostInformationInANode = Double.MIN_VALUE;
            final Map<Integer, Integer> attributeIndexToNodeRow = new HashMap<Integer, Integer>();
            for (int i = 0; i < entropy.length; ++i)
                {
                final int nodeRowNumber = nodes.addRow();
                attributeIndexToNodeRow.put(i, nodeRowNumber);
                nodes.setString(nodeRowNumber, InteractionGraph.NODE_COLUMN_INDEX + 0,
                                attrNames.get(i).toString());
                nodes.setDouble(nodeRowNumber, InteractionGraph.NODE_COLUMN_INDEX + 1,
                                entropy[i][i]);
                nodes.setDouble(nodeRowNumber, InteractionGraph.NODE_COLUMN_INDEX + 2,
                                cond_entropy[i][i]);
                final double information = class_entropy - class_cond_entropy[i];
                nodes.setDouble(nodeRowNumber, InteractionGraph.NODE_COLUMN_INDEX + 3,
                                information);
                if (information > mostInformationInANode)
                    {
                    mostInformationInANode = information;
                    mostInformativeNodeRowNumber = nodeRowNumber;
                    }
                }
            for (int i = 0; i < entropy.length - 1; ++i)
                {
                final int sourceNodeRowNumber = attributeIndexToNodeRow.get(i);
                for (int j = i + 1; j < entropy[i].length; ++j)
                    {
                    final int targetNodeRowNumber = attributeIndexToNodeRow.get(j);
                    final double i_a_b = entropy[i][i] + entropy[j][j] - entropy[i][j];
                    final double i_a_c = class_entropy - class_cond_entropy[i];
                    final double i_a_b_c = cond_entropy[i][i] + cond_entropy[j][j]
                                           - cond_entropy[i][j] - i_a_b;
                    final double i_b_c = class_entropy - class_cond_entropy[j];
                    final double i_ab_c = i_a_b_c + i_a_c + i_b_c;
                    final int edgeRowNumber = edges.addRow();
                    edges.setInt(edgeRowNumber, InteractionGraph.EDGE_COLUMN_INDEX + 0,
                                 sourceNodeRowNumber);
                    edges.setInt(edgeRowNumber, InteractionGraph.EDGE_COLUMN_INDEX + 1,
                                 targetNodeRowNumber);
                    edges.setDouble(edgeRowNumber,
                                    InteractionGraph.EDGE_COLUMN_INDEX + 2, entropy[i][j]);
                    edges.setDouble(edgeRowNumber,
                                    InteractionGraph.EDGE_COLUMN_INDEX + 3, cond_entropy[i][j]);
                    edges.setDouble(edgeRowNumber,
                                    InteractionGraph.EDGE_COLUMN_INDEX + 4, i_a_b);
                    edges.setDouble(edgeRowNumber,
                                    InteractionGraph.EDGE_COLUMN_INDEX + 5, i_a_b_c);
                    edges.setDouble(edgeRowNumber,
                                    InteractionGraph.EDGE_COLUMN_INDEX + 6, i_ab_c);
                    edges.setInt(edgeRowNumber, InteractionGraph.EDGE_COLUMN_INDEX + 7,
                                 entropyAnalysis.getIndexIntoRange(Dendrogram.NUM_DATA_BINS,
                                                                   i_a_b_c));
                    } // end j
                } // end i
            entropyGraph = new Graph(nodes, edges, false,
                                     InteractionGraph.EDGE_SCHEMA
                                             .getColumnName(InteractionGraph.EDGE_COLUMN_INDEX + 0),
                                     InteractionGraph.EDGE_SCHEMA
                                             .getColumnName(InteractionGraph.EDGE_COLUMN_INDEX + 1));
            }
        return entropyGraph;
        } // end getEntropyGraph()

    public String getEPSText()
        {
        final EpsGraphics2D g = new EpsGraphics2D();
        g.setFont(getFont());
        invalidate();
        printComponent(g);
        // paintDisplay(g, d);
        return g.toString();
        }

    public int getFontSize()
        {
        final Font font = getFont();
        return font.getSize();
        }

    public RenderedImage getImage()
        {
        final BufferedImage img = new BufferedImage(getWidth(), getHeight(),
                                                    BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE,
                                    BasicStroke.JOIN_MITER));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(getBackground());
        g.clearRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(getFont());
        invalidate();
        printComponent(g);
        g.dispose();
        return img;
        }

    public int getLineThickness()
        {
        return lineThickness;
        }

    public double getMinimumAbsoluteEntropyPercent()
        {
        return minimumAbsoluteEntropyPercent;
        }

    public List<Double> getMinimumAbsoluteEntropyPercentValuesSet()
        {
        return minimumAbsoluteEntropyPercentValuesList;
        }

    private void setEdgeVisibility()
        {
        @SuppressWarnings("unchecked")
        final Iterator<DecoratorItem> items = m_vis
                .items(InteractionGraph.EDGE_DECORATORS);
        final double minimumAbsoluteEntropy = getMinimumAbsoluteEntropyPercent() / 100;
        while (items.hasNext())
            {
            final DecoratorItem decoratorItem = items.next();
            final EdgeItem edgeItem = (EdgeItem) decoratorItem.getDecoratedItem();
            final double edgeValue = edgeItem.getDouble(InteractionGraph.EDGE_SCHEMA
                                                                .getColumnName(InteractionGraph
                                                                                       .EDGE_VALUE_COLUMN_INDEX));
            // round to the closest 1/100th of a percent
            final boolean isVisible = (Math.abs(edgeValue) + 0.00005) >= minimumAbsoluteEntropy;
            PrefuseLib.updateVisible(decoratorItem, isVisible);
            PrefuseLib.updateVisible(edgeItem, isVisible);
            }
        }

    public void setFontSize(final int pFontSize)
        {
        final int newFontSize = (pFontSize < 1) ? Main.defaultEntropyGraphTextSize
                                                : pFontSize;
        setFont(new Font("SansSerif", Font.BOLD, newFontSize));
        m_vis.run("draw");
        }

    public void setLineThickness(final int pLineThickness)
        {
        lineThickness = (pLineThickness < 1) ? Main.defaultEntropyGraphLineThickness
                                             : pLineThickness;
        invalidate();
        repaint();
        }

    public void setMargin(final int pMargin)
        {
        margin = pMargin;
        }

    public void setMinimumAbsoluteEntropyPercent(
            final double minimumAbsoluteEntropy)
        {
        minimumAbsoluteEntropyPercent = minimumAbsoluteEntropy;
        setEdgeVisibility();
        m_vis.run("drag");
        }

    private void setUpDisplay(final Graph entropyGraph)
        {
        if (entropyGraph != null)
            {
            final Action layoutAction = m_vis.getAction("layout");
            if (layoutAction != null)
                {
                layoutAction.setDuration(0);
                layoutAction.cancel();
                }
            m_vis.cancel("animate");
            m_vis.cancel("drag");
            m_vis.cancel("click");
            m_vis.cancel("layout");
            m_vis.cancel("draw");
            m_vis.reset();
            removeComponentListener(resizeListener);
            // this.setVisualization(new Visualization());
            m_vis.add(networkType.getNetworkType(), entropyGraph);
            m_vis.setInteractive(networkType.getEdgeGroup(), null, false);
            // -- set up renderers --
            final DefaultRendererFactory rf = new DefaultRendererFactory(
                    new LabelRenderer(InteractionGraph.NODE_SCHEMA.getColumnName(0))
                    {
                    {
                    setRenderType(AbstractShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);
                    setHorizontalAlignment(Constants.CENTER);
                    setRoundedCorner(8, 8);
                    }

                    @Override
                    protected java.lang.String getText(final VisualItem item) {
                    final StringBuilder sb = new StringBuilder(item
                                                                       .getString(InteractionGraph.NODE_SCHEMA
                                                                                          .getColumnName
                                                                                                  (InteractionGraph
























































































































































                                                                                                           .NODE_NAME_COLUMN_INDEX)));
                    sb.append("\n");
                    sb.append(String.format(
                            "%.2f%%",
                            item.getDouble(InteractionGraph.NODE_SCHEMA
                                                   .getColumnName(InteractionGraph.NODE_VALUE_COLUMN_INDEX)) * 100));
                    return sb.toString();
                    }
                    });
            rf.add(new InGroupPredicate(networkType.getEdgeGroup()),
                   new EdgeRenderer(Constants.EDGE_TYPE_LINE)
                   {
                   @Override
                   protected double getLineWidth(final VisualItem item) {
                   final int bin = item
                           .getInt(InteractionGraph.EDGE_INTERACTION_BIN_COLUMN_INDEX);
                   final int differenceFromNeutral = Math.abs(Dendrogram.NEUTRAL_BIN
                                                              - bin);
                   final double lineWidth = (1 + differenceFromNeutral)
                                            * lineThickness;
                   return lineWidth;
                   }
                   });
            rf.add(
                    new InGroupPredicate(InteractionGraph.EDGE_DECORATORS),
                    new LabelRenderer(InteractionGraph.EDGE_SCHEMA
                                              .getColumnName(InteractionGraph.EDGE_VALUE_COLUMN_INDEX))
                    {
                    @Override
                    protected java.lang.String getText(final VisualItem item) {
                    VisualItem itemToLabel = null;
                    if (item instanceof DecoratorItem)
                        {
                        itemToLabel = ((DecoratorItem) item).getDecoratedItem();
                        }
                    else
                        {
                        itemToLabel = item;
                        }
                    final String columnName = getTextField();
                    final Object columnLabelObject = itemToLabel.get(columnName);
                    final StringBuilder sb = new StringBuilder();
                    // if (false) {
                    // final int bin = itemToLabel
                    // .getInt(InteractionGraph.EDGE_INTERACTION_BIN_COLUMN_INDEX);
                    // final int differenceFromNeutral = Math
                    // .abs(Dendrogram.NEUTRAL_BIN - bin);
                    // sb.append(bin + "/" + differenceFromNeutral + ": ");
                    // }
                    if (columnLabelObject instanceof Double)
                        {
                        sb.append(String.format("%.2f%%",
                                                ((Double) columnLabelObject) * 100));
                        }
                    else
                        {
                        sb.append(columnLabelObject.toString());
                        }
                    return sb.toString();
                    }
                    });
            m_vis.setRendererFactory(rf);
            m_vis.addDecorators(InteractionGraph.EDGE_DECORATORS,
                                networkType.getEdgeGroup(), InteractionGraph.DECORATOR_SCHEMA);
            m_vis.setInteractive(InteractionGraph.EDGE_DECORATORS, null, true);
            // -- set up processing actions --
            // colors
            final ActionList draw = new ActionList();
            draw.add(new FontAction(networkType.getNodeGroup(), getFont())
            {
            @Override
            public java.awt.Font getFont(final VisualItem item) {
            return InteractionGraph.this.getFont();
            }
            });
            draw.add(new ColorAction(networkType.getNodeGroup(),
                                     VisualItem.STROKECOLOR, Color.LIGHT_GRAY.getRGB())
            {
            { // this is an instance initializer, also called a 'free floating code
            // block'
            add(VisualItem.FIXED, Color.DARK_GRAY.getRGB()); // FIXED seems to be
            // used during hover
            // -- don't know why
            }
            });
            draw.add(new ColorAction(networkType.getNodeGroup(),
                                     VisualItem.FILLCOLOR, Color.WHITE.getRGB()));
            draw.add(new ColorAction(networkType.getNodeGroup(),
                                     VisualItem.TEXTCOLOR, Color.BLACK.getRGB()));
            draw.add(new FontAction(networkType.getEdgeGroup(), getFont())
            {
            @Override
            public java.awt.Font getFont(final VisualItem item) {
            return InteractionGraph.this.getFont();
            }
            });
            draw.add(new ColorAction(networkType.getEdgeGroup(),
                                     VisualItem.STROKECOLOR, Color.BLACK.getRGB())
            {
            { // this is an instance initializer, also called a 'free floating code
            // block'
            add(VisualItem.FIXED, Color.DARK_GRAY.getRGB()); // FIXED seems to be
            add(VisualItem.HOVER, Color.BLUE.getRGB()); // FIXED seems to be
            add(VisualItem.HIGHLIGHT, Color.YELLOW.getRGB()); // FIXED seems to be
            // used during hover
            // -- don't know why
            }

            @Override
            public int getColor(final VisualItem item) {
            final int bin = item
                    .getInt(InteractionGraph.EDGE_INTERACTION_BIN_COLUMN_INDEX);
            final int color = Dendrogram.colors[bin].getRGB();
            return color;
            }
            });
            draw.add(new ColorAction(networkType.getEdgeGroup(),
                                     VisualItem.FILLCOLOR, Color.WHITE.getRGB()));
            draw.add(new ColorAction(networkType.getEdgeGroup(),
                                     VisualItem.TEXTCOLOR, Color.BLACK.getRGB()));
            draw.add(new RepaintAction());
            m_vis.putAction("draw", draw);
            final Layout nodeAndEdgeLayout = networkType.getLayout(this, margin);
            final Layout edgeLabelLayout = new EdgeLabelLayout(
                    InteractionGraph.EDGE_DECORATORS);
            final ActionList layout = new ActionList(
                    networkType.getActionListDuration());
            layout.add(nodeAndEdgeLayout);
            layout.add(edgeLabelLayout);
            m_vis.putAction("layout", layout);
            if (networkType.getActionListDuration() > 0)
                {
                layout.add(draw);
                m_vis.putAction("drag", layout);
                }
            else
                {
                m_vis.alwaysRunAfter("layout", "draw");
                final ActionList drag = new ActionList();
                drag.add(edgeLabelLayout);
                drag.add(draw);
                m_vis.putAction("drag", drag);
                }
            if (networkType.doClick())
                {
                // create the filtering and layout
                final ActionList click = new ActionList();
                click.add(new TreeRootAction(networkType.getNetworkType()));
                click.add(layout);
                m_vis.putAction("click", click);
                // animated transition
                final ActionList animate = new ActionList(1250);
                animate.setPacingFunction(new SlowInSlowOutPacer());
                // animate.add(new QualityControlAnimator());
                animate.add(new VisibilityAnimator(networkType.getNetworkType()));
                animate.add(new PolarLocationAnimator(networkType.getNodeGroup()));
                animate
                        .add(new PolarLocationAnimator(InteractionGraph.EDGE_DECORATORS));
                animate.add(new ColorAnimator(networkType.getNodeGroup()));
                animate.add(new RepaintAction());
                m_vis.putAction("animate", animate);
                m_vis.alwaysRunAfter("click", "animate");
                }
            // select the most informative node and put it into the middle
            // set the focus to node and run "click" and TreeRootAction will
            // cause the item to be placed into middle of display.
            final Node mostInformativeNode = entropyGraph
                    .getNode(mostInformativeNodeRowNumber);
            final VisualItem mostInformativeNodeItem = m_vis.getVisualItem(
                    networkType.getNodeGroup(), mostInformativeNode);
            final TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
            focus.setTuple(mostInformativeNodeItem);
            // if (nodeAndEdgeLayout instanceof ForceDirectedLayout) {
            // Point2D layoutAnchor = nodeAndEdgeLayout.getLayoutAnchor();
            // Rectangle2D layoutBounds = nodeAndEdgeLayout.getLayoutBounds();
            // PrefuseLib.setX(mostInformativeNodeItem, null, layoutAnchor.getX());
            // PrefuseLib.setY(mostInformativeNodeItem, null, layoutAnchor.getY());
            // PrefuseLib.setX(mostInformativeNodeItem, null,
            // layoutBounds.getCenterX());
            // PrefuseLib.setY(mostInformativeNodeItem, null,
            // layoutBounds.getCenterY());
            // ((ForceDirectedLayout)nodeAndEdgeLayout).setReferrer(mostInformativeNodeItem);
            // }
            addComponentListener(resizeListener);
            // Don't know why it is necessary to call m_vis.run("filter") twice
            // m_vis.run("click");
            m_vis.run("layout");
            // // maintain a set of items that should be interpolated linearly
            // // this isn't absolutely necessary, but makes the animations nicer
            // // the PolarLocationAnimator should read this set and act accordingly
            // m_vis.addFocusGroup(linear, new DefaultTupleSet());
            // m_vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(new
            // TupleSetListener() {
            // public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
            // TupleSet linearInterp = m_vis.getGroup(linear);
            // if (add.length < 1) return;
            // linearInterp.clear();
            // for (Node n = (Node) add[0]; n != null; n = n.getParent())
            // linearInterp.addTuple(n);
            // }
            // });
            } // end if entropyGraph != null
        @SuppressWarnings("unchecked")
        final Iterator<EdgeItem> items = m_vis.items(networkType.getEdgeGroup());
        final SortedSet<Double> edgeValues = new TreeSet<Double>();
        while (items.hasNext())
            {
            final EdgeItem edgeItem = items.next();
            final double edgeValue = edgeItem.getDouble(InteractionGraph.EDGE_SCHEMA
                                                                .getColumnName(InteractionGraph.EDGE_VALUE_COLUMN_INDEX));
            // want to convert from floating point to percentages rounder to the
            // closest hundredth of a percent
            final Double roundedValueForDisplay = ((int) ((Math.abs(edgeValue) + 0.00005) * 10000)) / 100.0;
            edgeValues.add(roundedValueForDisplay);
            }
        if (edgeValues.size() > 0)
            {
            // add one more so we can hide last edge
            edgeValues.add(edgeValues.last() + .01);
            }
        minimumAbsoluteEntropyPercentValuesList = new ArrayList<Double>(edgeValues);
        if (edgeValues.size() > 0)
            {
            defaultMinimumAbsoluteEntropyPercent = minimumAbsoluteEntropyPercentValuesList
                    .get(0);
            if (!minimumAbsoluteEntropyPercentValuesList
                    .contains(minimumAbsoluteEntropyPercent))
                {
                minimumAbsoluteEntropyPercent = defaultMinimumAbsoluteEntropyPercent;
                }
            setEdgeVisibility();
            }
        } // end setUpDisplay()

    public boolean supportEntropyThreshold()
        {
        return true;
        }

    public void updateGraph()
        {
        invalidate();
        setUpDisplay(getEntropyGraph());
        }

    public class CustomComboControl extends DragControl
        {
        private final String hoverAction;
        private final String dragAction;
        private final String clickAction;

        public CustomComboControl(final String pHoverAction,
                                  final String pDragAction, final String pClickAction)
            {
            super();
            hoverAction = pHoverAction;
            dragAction = pDragAction;
            clickAction = pClickAction;
            }

        @Override
        public void itemClicked(final VisualItem item, final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && (item instanceof NodeItem))
            {
            final TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
            focus.setTuple(item);
            if ((clickAction != null) && doClick())
                {
                m_vis.run(clickAction);
                }
            }
        }

        @Override
        public void itemDragged(final VisualItem item, final MouseEvent e) {
        // don't allow edge labels to be dragged
        if ((dragAction != null) && doDrag() && !(item instanceof DecoratorItem))
            {
            super.itemDragged(item, e);
            m_vis.run(dragAction);
            }
        }

        @Override
        public void itemEntered(final VisualItem item, final MouseEvent e) {
        super.itemEntered(item, e);
        if (item instanceof NodeItem)
            {
            if (hoverAction != null)
                {
                m_vis.run(hoverAction);
                }
            final StringBuilder sb = new StringBuilder("<html>");
            sb.append("<CENTER><BOLD>");
            final String nodeColumnName = InteractionGraph.NODE_SCHEMA
                    .getColumnName(InteractionGraph.NODE_NAME_COLUMN_INDEX);
            sb.append(item.getString(nodeColumnName));
            sb.append("</BOLD></CENTER>");
            final int startColumnIndex = InteractionGraph.NODE_COLUMN_INDEX + 1;
            for (int columnIndex = startColumnIndex; columnIndex <= (InteractionGraph.NODE_COLUMN_INDEX + 3); ++columnIndex)
                {
                if (columnIndex > startColumnIndex)
                    {
                    sb.append("<br>");
                    }
                final String columnName = InteractionGraph.NODE_SCHEMA
                        .getColumnName(columnIndex);
                sb.append(columnName);
                sb.append("=");
                sb.append(numberFormat.format(item.getDouble(columnName)));
                }
            sb.append("</html>");
            setToolTipText(sb.toString());
            }
        else if (item instanceof DecoratorItem)
            {
            if (hoverAction != null)
                {
                m_vis.run(hoverAction);
                }
            final EdgeItem edgeItem = (EdgeItem) ((DecoratorItem) item)
                    .getDecoratedItem();
            final NodeItem sourceItem = edgeItem.getSourceItem();
            final NodeItem targetItem = edgeItem.getTargetItem();
            final String nodeColumnName = InteractionGraph.NODE_SCHEMA
                    .getColumnName(InteractionGraph.NODE_NAME_COLUMN_INDEX);
            final StringBuilder sb = new StringBuilder("<html>");
            sb.append("<CENTER><BOLD>");
            sb.append(sourceItem.getString(nodeColumnName));
            sb.append(" &lt;-&gt; ");
            sb.append(targetItem.getString(nodeColumnName));
            sb.append("</BOLD></CENTER>");
            final int startColumnIndex = InteractionGraph.EDGE_COLUMN_INDEX + 2;
            for (int columnIndex = startColumnIndex; columnIndex <= (InteractionGraph.EDGE_COLUMN_INDEX + 6); ++columnIndex)
                {
                if (columnIndex > startColumnIndex)
                    {
                    sb.append("<br>");
                    }
                final String columnName = InteractionGraph.EDGE_SCHEMA
                        .getColumnName(columnIndex);
                sb.append(columnName);
                sb.append("=");
                sb.append(numberFormat.format(edgeItem.getDouble(columnName)));
                }
            sb.append("</html>");
            setToolTipText(sb.toString());
            }
        } // end itemEntered()

        @Override
        public void itemExited(final VisualItem item, final MouseEvent e) {
        super.itemExited(item, e);
        if ((item instanceof NodeItem) || (item instanceof DecoratorItem))
            {
            setToolTipText(null);
            if (hoverAction != null)
                {
                m_vis.run(hoverAction);
                }
            }
        } // end itemExited()
        } // end inner class CustomComboControl

    /**
     * Set label positions. Labels are assumed to be DecoratorItem instances, decorating their respective nodes. The layout simply gets the
     * bounds of the decorated node and assigns the label coordinates to the center of those bounds.
     */
    class EdgeLabelLayout extends Layout
        {
        public EdgeLabelLayout(final String group)
            {
            super(group);
            }

        @Override
        public void run(final double frac) {
        @SuppressWarnings("unchecked")
        final Iterator<DecoratorItem> iter = m_vis.items(m_group);
        while (iter.hasNext())
            {
            final DecoratorItem decorator = iter.next();
            final VisualItem decoratedItem = decorator.getDecoratedItem();
            final Rectangle2D bounds = decoratedItem.getBounds();
            final double x = bounds.getCenterX();
            final double y = bounds.getCenterY();
            /*
                    * modification to move edge labels more to the arrow head double x2 = 0, y2 = 0; if (decoratedItem instanceof EdgeItem){ VisualItem
                    * dest = ((EdgeItem)decoratedItem).getTargetItem(); x2 = dest.getX(); y2 = dest.getY(); x = (x + x2) / 2; y = (y + y2) / 2; }
                    */
            setX(decorator, null, x);
            setY(decorator, null, y);
            }
        }
        } // end of inner class EdgeLabelLayout

    public static class EdgeValueForceLayout extends ForceDirectedLayout
        {
        private final float maxCalculatedLength;
        private final Display display;

        public EdgeValueForceLayout(final Display pDisplay, final String graph)
            {
            super(graph, true, false /* runOnce */);
            // this.setDuration(5000);
            // this.setIterations(500);
            display = pDisplay;
            maxCalculatedLength = SpringForce.DEFAULT_MAX_SPRING_LENGTH
                                  + Dendrogram.NEUTRAL_BIN * SpringForce.DEFAULT_MAX_SPRING_LENGTH;
            }

        @Override
        protected float getSpringCoefficient(final EdgeItem e) {
        return SpringForce.DEFAULT_SPRING_COEFF;
        }

        @Override
        protected float getSpringLength(final EdgeItem e) {
        final int entropyValueColumnIndex = e
                .getColumnIndex(InteractionGraph.EDGE_SCHEMA
                                        .getColumnName(InteractionGraph.EDGE_INTERACTION_BIN_COLUMN_INDEX));
        final int bin = e.getInt(entropyValueColumnIndex);
        final int differenceFromNeutral = Math.abs(Dendrogram.NEUTRAL_BIN - bin);
        final int inverseDifferenceFromNeutral = Dendrogram.NEUTRAL_BIN
                                                 - differenceFromNeutral;
        final int minDimension = Math
                .min(display.getHeight(), display.getWidth());
        final float calculatedSpringLength = SpringForce.DEFAULT_MAX_SPRING_LENGTH
                                             + (SpringForce.DEFAULT_MAX_SPRING_LENGTH * inverseDifferenceFromNeutral);
        final float ratio = calculatedSpringLength / maxCalculatedLength;
        final float springLength = minDimension * ratio;
        return springLength;
        }
        } // end inner class EdgeValueForceLayout

    public enum NetworkType
        {
            FORCE_DIRECTED_GRAPH("Force Graph", 10000, InteractionGraph.GRAPH_GROUPS,
                                 false, true), RADIAL_TREE("Radial Graph", 0,
                                                           InteractionGraph.TREE_GROUPS, true, true), CIRCLE_LAYOUT(
                "Circle Graph", 0, InteractionGraph.TREE_GROUPS, false, true);
        private String displayName;
        private String[] groupNames;
        private long actionListDuration;
        private boolean doClick;
        private boolean doDrag;

        private NetworkType(final String pDisplayName,
                            final long pActionListDuration, final String[] pGroupNames,
                            final boolean pDoClick, final boolean pDoDrag)
            {
            displayName = pDisplayName;
            actionListDuration = pActionListDuration;
            groupNames = pGroupNames;
            doClick = pDoClick;
            doDrag = pDoDrag;
            }

        public boolean doClick()
            {
            return doClick;
            }

        public boolean doDrag()
            {
            return doDrag;
            }

        public long getActionListDuration()
            {
            return actionListDuration;
            }

        public String getDisplayName()
            {
            return displayName;
            }

        public String getEdgeGroup()
            {
            return groupNames[2];
            }

        public Layout getLayout(final Display display, final int margin)
            {
            Layout layout = null;
            switch (this)
                {
                case RADIAL_TREE:
                    final RadialTreeLayout radialTreeLayout = new RadialTreeLayout(
                            getNetworkType());
                    radialTreeLayout.setAutoScale(true);
                    layout = radialTreeLayout;
                    break;
                // case NODE_LINK_TREE_LAYOUT:
                // final NodeLinkTreeLayout nodeLinkTreeLayout = new
                // NodeLinkTreeLayout(getNetworkType());
                // nodeLinkTreeLayout.setBreadthSpacing(100);
                // nodeLinkTreeLayout.setSubtreeSpacing(200);
                // nodeLinkTreeLayout.setOrientation(Constants.ORIENT_TOP_BOTTOM);
                // layout = nodeLinkTreeLayout;
                // break;
                case CIRCLE_LAYOUT:
                    final CircleLayout circleLayout = new CircleLayout(getNodeGroup());
                    layout = circleLayout;
                    break;
                case FORCE_DIRECTED_GRAPH:
                    layout = new EdgeValueForceLayout(display, getNetworkType());
                    // // fix selected focus nodes
                    // final Visualization vis = display.getVisualization();
                    // TupleSet focusGroup = vis.getGroup(Visualization.FOCUS_ITEMS);
                    // focusGroup.addTupleSetListener(new TupleSetListener() {
                    // public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem)
                    // {
                    // for ( int i=0; i<rem.length; ++i ) {
                    // //((VisualItem)rem[i]).setFixed(false);
                    // }
                    // for ( int i=0; i<add.length; ++i ) {
                    // VisualItem visualItem = (VisualItem)add[i];
                    // visualItem.setFixed(!visualItem.isFixed());
                    // visualItem.setFixed(!visualItem.isFixed());
                    // visualItem.setFixed(!visualItem.isFixed());
                    // }
                    // if ( ts.getTupleCount() == 0 ) {
                    // ts.addTuple(rem[0]);
                    // ((VisualItem)rem[0]).setFixed(false);
                    // }
                    // vis.run("draw");
                    // }
                    // });
                    break;
                default:
                    throw new RuntimeException("Unhandled network type: " + toString());
                    // break;
                }
            layout.setMargin(margin, margin, margin, margin);
            return layout;
            } // end getLayout

        public String getNetworkType()
            {
            return groupNames[0];
            }

        public String getNodeGroup()
            {
            return groupNames[1];
            }

        @Override
        public String toString() {
        return getDisplayName();
        }
        } // end enum NetworkTypes

    /**
     * Switch the root of the tree by requesting a new spanning tree at the desired root
     */
    public static class TreeRootAction extends GroupAction
        {
        public TreeRootAction(final String graphGroup)
            {
            super(graphGroup);
            }

        @Override
        public void run(final double frac) {
        final TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
        if ((focus == null) || (focus.getTupleCount() == 0))
            {
            return;
            }
        final Graph g = (Graph) m_vis.getGroup(m_group);
        Tuple f = null;
        @SuppressWarnings("unchecked")
        final Iterator<Tuple> tuples = focus.tuples();
        while (tuples.hasNext() && !g.containsTuple(f = tuples.next()))
            {
            f = null;
            }
        if ((f != null) && (f instanceof Node))
            {
            g.getSpanningTree((Node) f);
            }
        } // end run()
        } // end class TreeRootAction
    } // end class InteractionGraph
