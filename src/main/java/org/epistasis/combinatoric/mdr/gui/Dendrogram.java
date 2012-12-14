package org.epistasis.combinatoric.mdr.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.List;

import javax.swing.JComponent;

import org.epistasis.combinatoric.EntropyAnalysis;
import org.epistasis.combinatoric.HierarchicalCluster;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.jibble.epsgraphics.EpsGraphics2D;

public class Dendrogram extends JComponent implements EntropyDisplay
    {
    private static final long serialVersionUID = 1L;
    public final static Color[] colors = {Color.decode("#0000FE"),
            Color.decode("#99CC00"), Color.decode("#CC9900"),
            Color.decode("#FE6600"), Color.decode("#FE0000")};
    public final static int NUM_DATA_BINS = Dendrogram.colors.length;
    // the bins are 0,1 for negative interaction, 2 (midpoint) for close to zero,
    // 3,4 for positive interaction
    public final static int NEUTRAL_BIN = Dendrogram.NUM_DATA_BINS / 2;
    private int lineThickness = 5;
    private double textPad = 5;
    private int margin = 10;
    private final EntropyAnalysis entropyAnalysis;

    private static double center(
            final HierarchicalCluster.Cluster<AttributeCombination> c)
        {
        if (c.isLeaf())
            {
            return 0;
            }
        return Dendrogram.center(c.getChild(0)) + Dendrogram.length(c) / 2.0;
        }

    private static double height(
            final HierarchicalCluster.Cluster<AttributeCombination> c)
        {
        if (c.isLeaf())
            {
            return 0;
            }
        return Dendrogram.height(c.getChild(0)) + 1
               + Dendrogram.height(c.getChild(1));
        }

    private static double length(
            final HierarchicalCluster.Cluster<AttributeCombination> c)
        {
        if (c.isLeaf())
            {
            return 0;
            }
        return Dendrogram.height(c) - Dendrogram.center(c.getChild(0))
               - (Dendrogram.height(c.getChild(1)) - Dendrogram.center(c.getChild(1)));
        }

    public Dendrogram(final EntropyAnalysis pEntropyAnalysis)
        {
        super(); // create empty Display
        setBackground(Color.WHITE);
        entropyAnalysis = pEntropyAnalysis;
        setFontSize(Main.defaultEntropyGraphTextSize);
        setLineThickness(Main.defaultEntropyGraphLineThickness);
        }

    private Color getBranchColor(final double info)
        {
        final int bin = entropyAnalysis.getIndexIntoRange(Dendrogram.NUM_DATA_BINS,
                                                          info);
        return Dendrogram.colors[bin];
        }

    public double getDefaultMinimumAbsoluteEntropyPercent()
        {
        throw new RuntimeException("not implemented");
        }

    public String getEPSText()
        {
        final EpsGraphics2D g = new EpsGraphics2D();
        paint(g);
        // g.setFont(getFont());
        // paintDendrogram(g, entropyAnalysis.getCluster(), new Rectangle(new
        // Point(), d));
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
        g.setFont(getFont());
        paint(g);
        g.dispose();
        return img;
        }

    public int getLineThickness()
        {
        return lineThickness;
        }

    public double getMinimumAbsoluteEntropyPercent()
        {
        throw new RuntimeException("not implemented");
        }

    public List<Double> getMinimumAbsoluteEntropyPercentValuesSet()
        {
        throw new RuntimeException("not implemented");
        }

    @Override
    public Dimension getPreferredSize() {
    Dimension returnDimension;
    final HierarchicalCluster<AttributeCombination> cluster = entropyAnalysis
            .getCluster();
    if (cluster == null)
        {
        returnDimension = new Dimension();
        }
    else
        {
        double maxLabelWidth = 0;
        for (final AttributeCombination attributeCombination : cluster.getRoot())
            {
            final String label = attributeCombination.toString();
            final double width = getGraphics().getFontMetrics().stringWidth(label);
            if (width > maxLabelWidth)
                {
                maxLabelWidth = width;
                }
            }
        returnDimension = new Dimension(
                (int) (maxLabelWidth + 200 + margin + margin), (getGraphics()
                                                                        .getFontMetrics().getHeight())
                                                               * cluster.getRoot().size()
                                                               + margin + margin);
        }
    return returnDimension;
    }

    public double getTextPad()
        {
        return textPad;
        }

    @Override
    public void paint(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;
    g2.setBackground(getBackground());
    g2.setColor(getForeground());
    g2.clearRect(0, 0, getWidth(), getHeight());
    final HierarchicalCluster<AttributeCombination> cluster = entropyAnalysis
            .getCluster();
    if (cluster != null)
        {
        g2.translate(margin, margin);
        paintDendrogram(g2, cluster, new Rectangle(0, 0, getWidth(), getHeight()));
        }
    }

    private void paintCluster(final Graphics2D g,
                              final HierarchicalCluster.Cluster<AttributeCombination> c,
                              final double x, final double width, final double maxWidth,
                              final double yscale, final double labelWidth)
        {
        if (c.isLeaf())
            {
            final String label = c.get(0).toString();
            final Color color = g.getColor();
            g.setColor(getForeground());
            g.drawString(label, (float) (x + lineThickness / 2.0 + textPad),
                         (float) ((g.getFontMetrics().getAscent()
                                   - g.getFontMetrics().getLeading() - g.getFontMetrics()
                                 .getDescent()) / 2.0));
            g.setColor(color);
            return;
            }
        final double cl = Dendrogram.length(c);
        final double cc = Dendrogram.center(c);
        final double y1 = (cc - cl / 2.0) * yscale;
        final double y2 = (cc + cl / 2.0) * yscale;
        final double bottomstart = yscale * (Dendrogram.height(c.getChild(0)) + 1);
        final double x1 = lineThickness / 2 + (maxWidth - c.getChild(0).getDist())
                                              * (width - labelWidth - lineThickness) / maxWidth;
        final double x2 = lineThickness / 2 + (maxWidth - c.getChild(1).getDist())
                                              * (width - labelWidth - lineThickness) / maxWidth;
        final Color color = g.getColor();
        g.setColor(getBranchColor(entropyAnalysis.getInfo(c.getChild(0),
                                                          c.getChild(1))));
        g.drawLine((int) x, (int) y1, (int) x, (int) y2);
        g.drawLine((int) x, (int) y1, (int) x1, (int) y1);
        g.drawLine((int) x, (int) y2, (int) x2, (int) y2);
        g.setColor(color);
        paintCluster(g, c.getChild(0), x1, width, maxWidth, yscale, labelWidth);
        g.translate(0, bottomstart);
        paintCluster(g, c.getChild(1), x2, width, maxWidth, yscale, labelWidth);
        g.translate(0, -bottomstart);
        }

    private void paintDendrogram(final Graphics2D g,
                                 final HierarchicalCluster<AttributeCombination> hc,
                                 final Rectangle2D bounds)
        {
        g.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE,
                                    BasicStroke.JOIN_MITER));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        double maxLabelWidth = 0;
        for (final AttributeCombination attributeCombination : hc.getRoot())
            {
            final String label = attributeCombination.toString();
            final double width = g.getFontMetrics().stringWidth(label);
            if (width > maxLabelWidth)
                {
                maxLabelWidth = width;
                }
            }
        final double ySpace = Math.max(g.getFontMetrics().getHeight(),
                                       lineThickness);
        final Rectangle2D newBounds = new Rectangle2D.Double(bounds.getX()
                                                             + lineThickness / 2, bounds.getY() + ySpace / 2.0, bounds.getWidth()
                                                                                                                - lineThickness / 2 - margin - margin - textPad, bounds.getHeight()
                                                                                                                                                                 - ySpace - margin - margin);
        g.translate(newBounds.getX(), newBounds.getY());
        newBounds.setRect(0, 0, newBounds.getWidth(), newBounds.getHeight());
        paintCluster(g, hc.getRoot(), 0, newBounds.getWidth(), hc.getRoot()
                .getDist(), newBounds.getHeight() / (hc.getRoot().size() - 1),
                     maxLabelWidth);
        }

    public void setFontSize(final int pFontSize)
        {
        final int newFontSize = (pFontSize < 1) ? Main.defaultEntropyGraphTextSize
                                                : pFontSize;
        setFont(new Font("SansSerif", Font.BOLD, newFontSize));
        repaint();
        }

    public void setLineThickness(final int pLineThickness)
        {
        lineThickness = (pLineThickness < 1) ? Main.defaultEntropyGraphLineThickness
                                             : pLineThickness;
        repaint();
        }

    public void setMargin(final int pMargin)
        {
        margin = pMargin;
        }

    public void setMinimumAbsoluteEntropyPercent(
            final double minimumAbsoluteEntropy)
        {
        throw new RuntimeException("not implemented");
        }

    public void setTextPad(final double textPad)
        {
        this.textPad = textPad;
        }

    public boolean supportEntropyThreshold()
        {
        return false;
        }

    public void updateGraph()
        {
        // nothing special has to be done because graph is created from scratch on
        // every redraw.
        invalidate();
        repaint();
        } // end updateGraph()
    }
