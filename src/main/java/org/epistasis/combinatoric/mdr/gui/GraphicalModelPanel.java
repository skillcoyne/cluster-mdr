package org.epistasis.combinatoric.mdr.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Model;
import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Swing component that draws a graphical chart depicting a given <code>MDRModel</code>. Provides functionality for
 * breaking up a model
 * chart into smaller-dimensional chunks, which may be paged through, and export of graphics to EPS format,
 * using the Jibble
 * <code>EPSGraphics2D</code> class.
 */
// TODO: when polytomy happens, this whole thing will need a revamp
public class GraphicalModelPanel extends JComponent
    {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final String MODEL_TOO_BIG = "Model too big to display.";
    private static final BufferedImage biImage = new BufferedImage(1, 1,
                                                                   BufferedImage.TYPE_INT_RGB);
    /**
     * Model to draw.
     */
    private Model model = null;
    private boolean toobig = false;
    /**
     * Color to use to fill in "affected" cells.
     */
    private Color affectedCellColor = new Color(0.75f, 0.75f, 0.75f);
    /**
     * Color to use to fill in "unaffected cells.
     */
    private Color unaffectedCellColor = new Color(0.85f, 0.85f, 0.85f);
    /**
     * Color to use to fill in "unknown" cells.
     */
    private Color unknownCellColor = Color.WHITE;
    /**
     * Color to use to fill in empty cells.
     */
    private Color emptyCellColor = Color.WHITE;
    /**
     * Font to use when drawing counts inside cells.
     */
    private Font cellFont = new Font("Dialog", Font.PLAIN, 12);
    /**
     * Font to use when drawing value labels.
     */
    private Font valueFont = new Font("Dialog", Font.PLAIN, 12);
    /**
     * Font to use when drawing attribute labels.
     */
    private Font axisFont = new Font("Dialog", Font.PLAIN, 12);
    /**
     * Size of cells, in pixels
     */
    private int cellSize = 100;
    /**
     * Value of largest count inside a cell in the current model. This is used to scale the bars inside the cells.
     */
    private float modelMax;
    private List<String> labels;
    /**
     * A <code>Vector of <code>Set</code>s of <code>Comparable</code>s that represent the values possible at each
     * attribute.
     */
    private ArrayList<List<String>> levels;
    private GraphicalModel gm;
    private final List<String[]> pages = new ArrayList<String[]>();
    /**
     * Maximum dimensionality to be shown on a single page.
     */
    private int maxDim = 3;
    /**
     * Index of page currently showing.
     */
    private int page = 0;
    private int[] drawnVar = null;

    private void addPage(final int[] variables, final String[] constants)
        {
        if ((maxDim > 0) && (variables.length > maxDim))
            {
            final int[] newVar = new int[variables.length - 1];
            System.arraycopy(variables, 0, newVar, 0, newVar.length);
            final String[] newConst;
            if (constants == null)
                {
                newConst = new String[1];
                }
            else
                {
                newConst = new String[constants.length + 1];
                for (int i = 1; i < newConst.length; ++i)
                    {
                    newConst[i] = constants[i - 1];
                    }
                }
            final List<String> values = new ArrayList<String>(
                    levels.get(variables[variables.length - 1]));
            Collections.sort(values);
            for (final String value : values)
                {
                newConst[0] = value;
                addPage(newVar, newConst.clone());
                }
            }
        else
            {
            if (drawnVar == null)
                {
                drawnVar = variables;
                }
            pages.add(constants);
            }
        }

    private void analyzeModel()
        {
        modelMax = 0;
        if ((model == null) || toobig)
            {
            gm = null;
            return;
            }
        for (final Model.Cell c : model.getCells().values())
            {
            if (c != null)
                {
                for (final float count : c.getCounts())
                    {
                    if (count > modelMax)
                        {
                        modelMax = count;
                        }
                    }
                }
            }
        gm = new GraphicalModel(
                (Graphics2D) GraphicalModelPanel.biImage.getGraphics(), model
                .getCombo().getAttributeIndices());
        }

    public Color getAffectedCellColor()
        {
        return affectedCellColor;
        }

    public Font getAxisFont()
        {
        return axisFont;
        }

    public Font getCellFont()
        {
        return cellFont;
        }

    /**
     * Get the size in pixels of an individual cell.
     *
     * @return Cell size in pixels
     */
    public int getCellSize()
        {
        return cellSize;
        }

    public Color getEmptyCellColor()
        {
        return emptyCellColor;
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
     * Get the maximum number of dimensions per page that this panel will display.
     *
     * @return Maximum dimension of pages
     */
    public int getMaxDim()
        {
        return maxDim;
        }

    public Model getModel()
        {
        return model;
        }

    /**
     * Get the number of pages that the current model fills, given the maximum dimension per page constraint.
     *
     * @return Number of pages for current model
     */
    public int getNumPages()
        {
        return pages.size();
        }

    /**
     * Get the index of the page that is currently showing.
     *
     * @return Index of current page
     */
    public int getPage()
        {
        return page;
        }

    public String getPageEPS(final int pPage)
        {
        if (pPage >= getNumPages())
            {
            return null;
            }
        final Graphics2D g = new EpsGraphics2D();
        final GraphicalModel tempGm = new GraphicalModel(g, drawnVar,
                                                         pages.get(pPage));
        final String pageLabel = getPageLabel(pPage);
        if (pageLabel != null)
            {
            g.setFont(axisFont);
            g.translate(0, g.getFontMetrics().getHeight());
            g.drawString(pageLabel, 5, 0);
            }
        tempGm.draw(g);
        return g.toString();
        }

    public RenderedImage getPageImage(final int pPage)
        {
        final BufferedImage img = new BufferedImage(getPreferredSize().width,
                                                    getPreferredSize().height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(getBackground());
        g.setColor(getForeground());
        g.clearRect(0, 0, getPreferredSize().width, getPreferredSize().height);
        if (pPage >= getNumPages())
            {
            return null;
            }
        final GraphicalModel tempGm = new GraphicalModel(g, drawnVar,
                                                         pages.get(pPage));
        final String pageLabel = getPageLabel(pPage);
        if (pageLabel != null)
            {
            g.setFont(axisFont);
            g.translate(0, g.getFontMetrics().getHeight());
            g.drawString(pageLabel, 5, 0);
            }
        tempGm.draw(g);
        g.dispose();
        return img;
        }

    /**
     * Generate a header label for a given page, indicating the values for each attribute that has been split.
     *
     * @param page Page for which to generate the label
     * @return Header label for given page
     */
    private String getPageLabel(final int pPage)
        {
        String ret = null;
        if (getNumPages() > 1)
            {
            ret = "";
            final String[] values = pages.get(pPage);
            final int[] variables = model.getCombo().getAttributeIndices();
            for (int i = values.length - 1; i >= 0; --i)
                {
                if (i != values.length - 1)
                    {
                    ret += ", ";
                    }
                ret += labels.get(variables[i + maxDim]);
                ret += " = " + values[i];
                }
            }
        return ret;
        }

    public Color getUnaffectedCellColor()
        {
        return unaffectedCellColor;
        }

    public Color getUnknownCellColor()
        {
        return unknownCellColor;
        }

    public Font getValueFont()
        {
        return valueFont;
        }

    @Override
    public void paint(final Graphics g) {
    super.paint(g);
    g.clearRect(0, 0, getWidth(), getHeight());
    if (model == null)
        {
        return;
        }
    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                      RenderingHints.VALUE_ANTIALIAS_ON);
    if (toobig)
        {
        g.setFont(axisFont);
        g.translate(0, g.getFontMetrics().getHeight());
        g.setColor(Color.red);
        g.drawString(GraphicalModelPanel.MODEL_TOO_BIG, 5, 0);
        }
    else
        {
        final String pageLabel = getPageLabel(page);
        if (pageLabel != null)
            {
            g.setFont(axisFont);
            g.translate(0, g.getFontMetrics().getHeight());
            g.drawString(pageLabel, 5, 0);
            }
        g.translate(5, 5);
        gm.draw((Graphics2D) g);
        }
    }

    /**
     *
     */
    private void recalculatePages()
        {
        pages.clear();
        drawnVar = null;
        if ((model == null) || toobig)
            {
            page = 0;
            return;
            }
        addPage(model.getCombo().getAttributeIndices(), null);
        setPage(0);
        }

    private void recalculateSize()
        {
        if (model == null)
            {
            setPreferredSize(new Dimension(0, 0));
            }
        else if (toobig)
            {
            final FontMetrics fm = GraphicalModelPanel.biImage.getGraphics()
                    .getFontMetrics(axisFont);
            final Rectangle2D bounds = fm.getStringBounds(
                    GraphicalModelPanel.MODEL_TOO_BIG,
                    GraphicalModelPanel.biImage.getGraphics());
            setPreferredSize(new Dimension((int) Math.ceil(bounds.getWidth()),
                                           (int) Math.ceil(bounds.getHeight())));
            }
        else
            {
            final Dimension d = new Dimension(gm.getSize());
            final String pageLabel = getPageLabel(page);
            if (pageLabel != null)
                {
                final Rectangle r = getFontMetrics(axisFont).getStringBounds(pageLabel,
                                                                             GraphicalModelPanel.biImage.getGraphics
                                                                                     ()).getBounds();
                d.height += r.height;
                if (d.width < r.width)
                    {
                    d.width = r.width;
                    }
                }
            d.width += 10;
            d.height += 10;
            setPreferredSize(d);
            }
        revalidate();
        }

    public void setAffectedCellColor(final Color affectedCellColor)
        {
        this.affectedCellColor = affectedCellColor;
        }

    public void setAxisFont(final Font axisFont)
        {
        this.axisFont = axisFont;
        }

    public void setCellFont(final Font cellFont)
        {
        this.cellFont = cellFont;
        }

    /**
     * Set the size in pixels of individual cells.
     *
     * @param cellSize Cell size in pixels
     */
    public void setCellSize(final int cellSize)
        {
        this.cellSize = cellSize;
        recalculateSize();
        }

    public void setEmptyCellColor(final Color emptyCellColor)
        {
        this.emptyCellColor = emptyCellColor;
        }

    public void setLabels(final List<String> labels)
        {
        this.labels = labels;
        }

    public void setLevels(final List<List<String>> levels)
        {
        this.levels = new ArrayList<List<String>>(levels.size());
        this.levels.addAll(levels);
        }

    /**
     * set the maximum number of dimensions per page that this panel will display.
     *
     * @param maxDim Maximum dimension of pages
     */
    public void setMaxDim(final int maxDim)
        {
        this.maxDim = maxDim;
        if (model == null)
            {
            return;
            }
        recalculatePages();
        }

    /**
     * Set the model to be displayed as a graphical chart.
     *
     * @param model The model to be drawn
     */
    public void setModel(final Model model)
        {
        this.model = model;
        toobig = (model != null) && (model.getCombo().size() > 10);
        analyzeModel();
        recalculatePages();
        recalculateSize();
        repaint();
        }

    /**
     * Show a given page of the current model.
     *
     * @param page Index of page to show
     */
    public void setPage(final int page)
        {
        if ((page < 0) || (page >= getNumPages()))
            {
            return;
            }
        this.page = page;
        gm = new GraphicalModel(
                (Graphics2D) GraphicalModelPanel.biImage.getGraphics(), drawnVar,
                pages.get(page));
        getPageLabel(page);
        recalculateSize();
        repaint();
        }

    public void setUnaffectedCellColor(final Color unaffectedCellColor)
        {
        this.unaffectedCellColor = unaffectedCellColor;
        }

    public void setUnknownCellColor(final Color unknownCellColor)
        {
        this.unknownCellColor = unknownCellColor;
        }

    public void setValueFont(final Font valueFont)
        {
        this.valueFont = valueFont;
        }

    public class GraphicalModel
        {
        private final Dimension size = new Dimension();
        private final Dimension trimSize = new Dimension();
        private Point loc = null;
        private int labelSpace = 0;
        private int[] variables = null;
        private String[] constants = null;
        private List<GraphicalModel> children = null;
        private final boolean firstx;
        private final boolean firsty;

        public GraphicalModel(final Graphics2D g, final int[] variables)
            {
            this(g, new Point(), true, true, variables, null);
            }

        public GraphicalModel(final Graphics2D g, final int[] variables,
                              final String[] constants)
            {
            this(g, new Point(), true, true, variables, constants);
            }

        private GraphicalModel(final Graphics2D g, final Point loc,
                               final boolean firstx, final boolean firsty, final int[] variables,
                               final String[] constants)
            {
            this.variables = variables;
            this.constants = constants;
            this.loc = new Point(loc);
            this.firstx = firstx;
            this.firsty = firsty;
            if (getDimension() == 0)
                {
                trimSize.width = trimSize.height = size.width = size.height = cellSize;
                return;
                }
            final int[] newVar = new int[variables.length - 1];
            System.arraycopy(variables, 0, newVar, 0, newVar.length);
            final String[] newConst;
            if (constants == null)
                {
                newConst = new String[1];
                }
            else
                {
                newConst = new String[constants.length + 1];
                for (int i = 1; i < newConst.length; ++i)
                    {
                    newConst[i] = constants[i - 1];
                    }
                }
            final List<String> values = new ArrayList<String>(
                    levels.get(variables[variables.length - 1]));
            Collections.sort(values);
            calcLabelSpace(g);
            children = new ArrayList<GraphicalModel>(values.size());
            final Point p = new Point(loc);
            if (isHorizontal())
                {
                p.y += labelSpace;
                }
            else
                {
                p.x += labelSpace;
                }
            boolean fx = firstx;
            boolean fy = firsty;
            for (final String value : values)
                {
                newConst[0] = value;
                final GraphicalModel m = new GraphicalModel(g, p, fx, fy, newVar,
                                                            newConst.clone());
                children.add(m);
                if (isHorizontal())
                    {
                    p.x += m.getSize().width + getGap();
                    fx = false;
                    }
                else
                    {
                    p.y += m.getSize().height + getGap();
                    fy = false;
                    }
                }
            int maxLabelSpace = 0;
            for (final GraphicalModel m : children)
                {
                if (m.getLabelSpace() > maxLabelSpace)
                    {
                    maxLabelSpace = m.getLabelSpace();
                    }
                }
            for (final GraphicalModel m : children)
                {
                m.setLabelSpace(maxLabelSpace);
                }
            if (isHorizontal())
                {
                trimSize.width = size.width = (children.size() - 1) * getGap();
                for (final GraphicalModel m : children)
                    {
                    size.width += m.getSize().width;
                    trimSize.width += m.getTrimSize().width;
                    }
                final GraphicalModel m = children.get(0);
                trimSize.height = m.getTrimSize().height;
                size.height = m.getSize().height + labelSpace;
                }
            else
                {
                trimSize.height = size.height = (children.size() - 1) * getGap();
                for (final GraphicalModel m : children)
                    {
                    size.height += m.getSize().height;
                    trimSize.height += m.getTrimSize().height;
                    }
                final GraphicalModel m = children.get(0);
                trimSize.width = m.getTrimSize().width;
                size.width = m.getSize().width + labelSpace;
                }
            }

        private void calcLabelSpace(final Graphics2D g)
            {
            if (!isLabeled())
                {
                labelSpace = 0;
                return;
                }
            if (isHorizontal())
                {
                labelSpace = g.getFontMetrics(axisFont).getHeight()
                             + g.getFontMetrics(valueFont).getHeight() + 10;
                return;
                }
            final List<String> values = levels.get(variables[variables.length - 1]);
            int maxWidth = 0;
            for (final String value : values)
                {
                final int width = g.getFontMetrics(valueFont).getStringBounds(value, g)
                        .getBounds().width;
                if (width > maxWidth)
                    {
                    maxWidth = width;
                    }
                }
            labelSpace = g.getFontMetrics(axisFont).getHeight() + maxWidth + 10;
            }

        public void draw(final Graphics2D g)
            {
            if (getDimension() == 0)
                {
                final AttributeCombination combo = getModel().getCombo();
                final byte[] cellIndex = new byte[combo.size()];
                for (int i = 0; i < combo.size(); ++i)
                    {
                    cellIndex[i] = (byte) getLevels().get(combo.get(i)).indexOf(
                            constants[i]);
                    }
                // final byte tieStatus = getModel().getTieStatus();
                final Model.Cell c = getModel().getCells().get(cellIndex);
                if (c == null)
                    {
                    drawEmptyCell(g, loc);
                    }
                else
                    {
                    final byte status = c.getStatus();
                    // if ((status == Model.UNKNOWN_STATUS)
                    // && (tieStatus != Model.UNKNOWN_STATUS)) {
                    // status = tieStatus;
                    // }
                    drawCell(g, loc, c.getAffected(), c.getUnaffected(), status,
                             c.getAffectedStatus());
                    }
                }
            else
                {
                for (final GraphicalModel child : children)
                    {
                    child.draw(g);
                    }
                if (isLabeled())
                    {
                    drawLabels(g);
                    }
                }
            }

        private void drawCell(final Graphics2D g, final Point p,
                              final float affected, final float unaffected, final byte status,
                              final int affectedStatus)
            {
            if (status == Model.UNKNOWN_STATUS)
                {
                g.setColor(unknownCellColor);
                }
            else if (status == affectedStatus)
                {
                g.setColor(affectedCellColor);
                }
            else
                {
                g.setColor(unaffectedCellColor);
                }
            g.fillRect(p.x, p.y, cellSize, cellSize);
            g.setColor(getForeground());
            String s = Main.modelTextNumberFormat.format(affected);
            g.setFont(cellFont);
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(s, g);
            int height = (int) ((double) Math.min(affected, modelMax)
                                / (double) modelMax * (cellSize - 5 - bounds.getHeight()));
            g.fillRect(p.x + (int) (cellSize / 6.0), p.y + cellSize - height,
                       (int) (cellSize / 4.0), height);
            g.drawString(
                    s,
                    p.x
                    + (int) (cellSize / 6.0 + (((cellSize / 4.0) - bounds.getWidth()) / 2.0)),
                    p.y + cellSize - 5 - height);
            s = Main.modelTextNumberFormat.format(unaffected);
            bounds = g.getFontMetrics().getStringBounds(s, g);
            height = (int) ((double) Math.min(unaffected, modelMax)
                            / (double) modelMax * (cellSize - 5 - bounds.getHeight()));
            g.fillRect(p.x + (int) (cellSize / 3.0 + cellSize / 4.0), p.y + cellSize
                                                                      - height, (int) (cellSize / 4.0), height);
            g.drawString(
                    s,
                    p.x
                    + (int) (cellSize / 3.0 + cellSize / 4.0 + (((cellSize / 4.0) - bounds
                            .getWidth()) / 2.0)), p.y + cellSize - 5 - height);
            g.drawRect(p.x, p.y, cellSize, cellSize);
            }

        private void drawEmptyCell(final Graphics2D g, final Point p)
            {
            g.setColor(emptyCellColor);
            g.fillRect(p.x, p.y, cellSize, cellSize);
            g.setColor(getForeground());
            g.drawRect(p.x, p.y, cellSize, cellSize);
            }

        private void drawLabels(final Graphics2D g)
            {
            final List<String> values = new ArrayList<String>(
                    levels.get(variables[variables.length - 1]));
            Collections.sort(values);
            String label = labels.get(variables[variables.length - 1]);
            g.setFont(axisFont);
            final Rectangle r = g.getFontMetrics().getStringBounds(label, g)
                    .getBounds();
            Point center = getLabelCenter();
            if (isHorizontal())
                {
                g.drawString(label, loc.x + center.x - r.width / 2, loc.y + r.height);
                g.setFont(valueFont);
                int i = 0;
                final Iterator<String> value = values.iterator();
                while (value.hasNext())
                    {
                    final GraphicalModel m = children.get(i++);
                    center = m.getLabelCenter();
                    label = value.next();
                    final Rectangle r2 = g.getFontMetrics().getStringBounds(label, g)
                            .getBounds();
                    g.drawString(label, m.loc.x + center.x - r2.width / 2, loc.y + 5
                                                                           + r.height + r2.height);
                    }
                }
            else
                {
                drawVerticalString(g, label, loc.x + r.height, loc.y + center.y
                                                               + r.width / 2);
                g.setFont(valueFont);
                int i = 0;
                final Iterator<String> value = values.iterator();
                while (value.hasNext())
                    {
                    final GraphicalModel m = children.get(i++);
                    center = m.getLabelCenter();
                    label = value.next();
                    final Rectangle r2 = g.getFontMetrics().getStringBounds(label, g)
                            .getBounds();
                    g.drawString(label, loc.x + getLabelSpace() - 5 - r2.width, m.loc.y
                                                                                + center.y + r2.height / 2);
                    }
                }
            }

        private void drawVerticalString(final Graphics2D g, final String s,
                                        final int x, final int y)
            {
            g.translate(x, y);
            g.rotate(-Math.PI / 2);
            g.drawString(s, 0, 0);
            g.rotate(Math.PI / 2);
            g.translate(-x, -y);
            }

        public int getDimension()
            {
            return variables.length;
            }

        public int getGap()
            {
            return ((getDimension() + 1) / 2 - 1) * cellSize / 2;
            }

        public Point getLabelCenter()
            {
            final Point ret = new Point();
            ret.x = size.width - trimSize.width / 2;
            ret.y = size.height - trimSize.height / 2;
            return ret;
            }

        public int getLabelSpace()
            {
            return labelSpace;
            }

        public Dimension getSize()
            {
            return size;
            }

        public Dimension getTrimSize()
            {
            return trimSize;
            }

        public boolean isHorizontal()
            {
            return (getDimension() % 2) == 1;
            }

        public boolean isLabeled()
            {
            if (getDimension() == 0)
                {
                return false;
                }
            if (isHorizontal())
                {
                return firsty;
                }
            return firstx;
            }

        public void setLabelSpace(final int labelSpace)
            {
            if (isHorizontal())
                {
                size.width -= this.labelSpace;
                this.labelSpace = labelSpace;
                size.width += labelSpace;
                }
            else
                {
                size.height -= this.labelSpace;
                this.labelSpace = labelSpace;
                size.height += labelSpace;
                }
            }
        }
    }
