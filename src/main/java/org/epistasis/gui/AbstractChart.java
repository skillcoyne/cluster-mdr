package org.epistasis.gui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

import org.jibble.epsgraphics.EpsGraphics2D;

public abstract class AbstractChart extends SelectableComponent {
	private static final long serialVersionUID = 3690070840489510184L;
	private Insets borderInsets = new Insets(10, 10, 10, 10);
	private final Insets chartInsets = new Insets(0, 0, 0, 0);
	private boolean dirtyInsets = true;
	private String xAxisLabel = "x-axis";
	private String yAxisLabel = "y-axis";
	private int xTickSize = 5;
	private int yTickSize = 5;
	private Font axisFont = new Font("Dialog", Font.BOLD, 20);
	private Font labelFont = new Font("Dialog", Font.PLAIN, 12);
	private final Map<Double, Object> xLabels = new TreeMap<Double, Object>();
	private final Map<Double, Object> yLabels = new TreeMap<Double, Object>();
	private Rectangle2D viewport = new Rectangle.Double(0, 0, 1, 1);

	protected void addXLabel(final double x, final Object label) {
		addXLabel(x, label, true);
	}

	protected void addXLabel(final double x, final Object label,
			final boolean repaint) {
		xLabels.put(x, label);
		update(repaint);
	}

	protected void addYLabel(final double y, final Object label) {
		addYLabel(y, label, true);
	}

	protected void addYLabel(final double y, final Object label,
			final boolean repaint) {
		yLabels.put(y, label);
		update(repaint);
	}

	protected void clearXLabels() {
		clearXLabels(true);
	}

	protected void clearXLabels(final boolean repaint) {
		xLabels.clear();
		update(repaint);
	}

	protected void clearYLabels() {
		clearYLabels(true);
	}

	protected void clearYLabels(final boolean repaint) {
		yLabels.clear();
		update(repaint);
	}

	private void draw(final Graphics2D g, final int width, final int height) {
		final FontMetrics fmAxis = g.getFontMetrics(axisFont);
		final Font oldFont = g.getFont();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawLine(getChartInsets().left, getChartInsets().top,
				getChartInsets().left, height - getChartInsets().bottom);
		g.drawLine(getChartInsets().left, height - getChartInsets().bottom, width
				- getChartInsets().right, height - getChartInsets().bottom);
		g.setFont(labelFont);
		double last = Double.NEGATIVE_INFINITY;
		for (final Map.Entry<Double, Object> entry : xLabels.entrySet()) {
			double x = entry.getKey();
			if ((x < viewport.getMinX()) || (x > viewport.getMaxX())) {
				continue;
			}
			final String label = entry.getValue().toString();
			final Rectangle2D r = getXLabelBounds(g, width, height, x, label);
			x = ((x - viewport.getMinX()) / viewport.getWidth())
					* (width - getChartInsets().left - getChartInsets().right)
					+ getChartInsets().left;
			g.drawLine((int) Math.round(x), height - getChartInsets().bottom,
					(int) Math.round(x), height - getChartInsets().bottom + xTickSize);
			if (r.getMinX() - last > 5) {
				g.drawString(label, (float) r.getMinX(), (float) r.getMaxY());
				last = r.getMaxX();
			}
		}
		last = Double.POSITIVE_INFINITY;
		for (final Map.Entry<Double, Object> entry : yLabels.entrySet()) {
			double y = entry.getKey();
			if ((y < viewport.getMinY()) || (y > viewport.getMaxY())) {
				continue;
			}
			final String label = entry.getValue().toString();
			final Rectangle2D r = getYLabelBounds(g, width, height, y, label);
			y = ((viewport.getMaxY() - y) / viewport.getHeight())
					* (height - getChartInsets().top - getChartInsets().bottom)
					+ getChartInsets().top;
			g.drawLine(getChartInsets().left, (int) Math.round(y),
					getChartInsets().left - yTickSize, (int) Math.round(y));
			if (last - r.getMaxY() > 5) {
				g.drawString(label, (float) r.getMinX(), (float) r.getMaxY());
				last = r.getMinY();
			}
		}
		g.setFont(axisFont);
		if ((xAxisLabel != null) && (xAxisLabel.length() > 0)) {
			g.drawString(xAxisLabel, (float) (getChartInsets().left + (width
					- getChartInsets().left - getChartInsets().right - fmAxis
					.getStringBounds(xAxisLabel, g).getWidth()) / 2.0),
					height - fmAxis.getDescent());
		}
		if ((yAxisLabel != null) && (yAxisLabel.length() > 0)) {
			final AffineTransform oldXForm = g.getTransform();
			g.translate(
					fmAxis.getHeight() - fmAxis.getLeading() - fmAxis.getDescent(),
					getChartInsets().top
							+ (height - getChartInsets().top - getChartInsets().bottom + fmAxis
									.getStringBounds(yAxisLabel, g).getWidth()) / 2.0);
			g.rotate(-Math.PI / 2.0);
			g.drawString(yAxisLabel, 0, 0);
			g.setTransform(oldXForm);
		}
		final int width2 = width - getChartInsets().left - getChartInsets().right;
		final int height2 = height - getChartInsets().top - getChartInsets().bottom;
		g.setFont(oldFont);
		final AffineTransform oldXform = g.getTransform();
		g.translate(getChartInsets().left, height - getChartInsets().bottom);
		g.scale(1, -1);
		if (g.getClip() == null) {
			g.setClip(new Rectangle(1, 0, width2, height2));
		} else {
			g.setClip(g.getClip().getBounds()
					.intersection(new Rectangle(1, 0, width2, height2)));
		}
		drawContents(g, width2, height2);
		g.setTransform(oldXform);
	}

	public abstract void drawContents(Graphics2D g, int width, int height);

	public Font getAxisFont() {
		return axisFont;
	}

	public Insets getBorderInsets() {
		return borderInsets;
	}

	protected Insets getChartInsets() {
		if (dirtyInsets) {
			final FontMetrics fmAxis = getGraphics().getFontMetrics(axisFont);
			final FontMetrics fmLabel = getGraphics().getFontMetrics(labelFont);
			final int width = getWidth() - borderInsets.left - borderInsets.right;
			final int height = getHeight() - borderInsets.top - borderInsets.bottom;
			final Graphics2D g = (Graphics2D) getGraphics();
			double xmin = Double.POSITIVE_INFINITY;
			double xmax = Double.NEGATIVE_INFINITY;
			double ymin = Double.NEGATIVE_INFINITY;
			double ymax = Double.POSITIVE_INFINITY;
			chartInsets.top = chartInsets.bottom = chartInsets.left = chartInsets.right = 0;
			if ((xAxisLabel != null) && (xAxisLabel.length() > 0)) {
				chartInsets.bottom += fmAxis.getHeight();
			}
			if ((yAxisLabel != null) && (yAxisLabel.length() > 0)) {
				chartInsets.left += 5 + fmAxis.getHeight();
			}
			if (!xLabels.isEmpty()) {
				chartInsets.bottom += 5 + xTickSize + fmLabel.getHeight();
			}
			if (!yLabels.isEmpty()) {
				double maxwidth = 0;
				for (final Map.Entry<Double, Object> entry : yLabels.entrySet()) {
					final double y = entry.getKey();
					if (y < viewport.getMinY()) {
						continue;
					}
					if (y > viewport.getMaxY()) {
						break;
					}
					final String label = entry.getValue().toString();
					final Rectangle2D r = fmLabel.getStringBounds(label, getGraphics());
					if (r.getWidth() > maxwidth) {
						maxwidth = r.getWidth();
					}
				}
				chartInsets.left += 5 + yTickSize + Math.ceil(maxwidth);
			}
			for (final Map.Entry<Double, Object> entry : xLabels.entrySet()) {
				final double x = entry.getKey();
				if ((x < viewport.getMinX()) || (x > viewport.getMaxX())) {
					continue;
				}
				final String label = entry.getValue().toString();
				final Rectangle2D r = getXLabelBounds(g, width, height, x, label);
				if (r.getMinX() < xmin) {
					xmin = r.getMinX();
				}
				if (r.getMaxX() > xmax) {
					xmax = r.getMaxX();
				}
			}
			for (final Map.Entry<Double, Object> entry : yLabels.entrySet()) {
				final double y = entry.getKey();
				if ((y < viewport.getMinY()) || (y > viewport.getMaxY())) {
					continue;
				}
				final String label = entry.getValue().toString();
				final Rectangle2D r = getYLabelBounds(g, width, height, y, label);
				if (r.getMinY() > ymin) {
					ymin = r.getMinY();
				}
				if (r.getMaxY() < ymax) {
					ymax = r.getMaxY();
				}
			}
			if ((xmin != Double.POSITIVE_INFINITY) && (xmin < 0)) {
				chartInsets.left -= (int) Math.floor(xmin);
			}
			if ((xmax != Double.NEGATIVE_INFINITY) && (xmax > width)) {
				chartInsets.right += (int) Math.floor(xmax - width + chartInsets.right);
			}
			if ((ymin != Double.NEGATIVE_INFINITY) && (ymin < chartInsets.top)) {
				chartInsets.top -= (int) Math.floor(ymin);
			}
			if ((ymax != Double.POSITIVE_INFINITY)
					&& (ymax > height - chartInsets.bottom)) {
				chartInsets.bottom += (int) Math.floor(ymax - height
						+ chartInsets.bottom);
			}
			dirtyInsets = false;
		}
		return chartInsets;
	}

	public String getEpsText(final int width, final int height) {
		final EpsGraphics2D g = new EpsGraphics2D();
		dirtyInsets = true;
		draw(g, width, height);
		dirtyInsets = true;
		return g.toString();
	}

	public BufferedImage getImage(final int width, final int height) {
		final BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = img.createGraphics();
		g.setBackground(getBackground());
		g.setColor(getForeground());
		g.clearRect(0, 0, width, height);
		g.translate(borderInsets.left, borderInsets.right);
		dirtyInsets = true;
		draw(g, width - borderInsets.left - borderInsets.right, height
				- borderInsets.top - borderInsets.bottom);
		dirtyInsets = true;
		g.dispose();
		return img;
	}

	public Font getLabelFont() {
		return labelFont;
	}

	@Override
	public Rectangle2D getSelectable() {
		return new Rectangle(getBorderInsets().left + getChartInsets().left,
				getBorderInsets().top + getChartInsets().top, getWidth()
						- getBorderInsets().left - getBorderInsets().right
						- getChartInsets().left - getChartInsets().right, getHeight()
						- getBorderInsets().top - getBorderInsets().bottom
						- getChartInsets().top - getChartInsets().bottom);
	}

	@Override
	public Rectangle2D getViewport() {
		return viewport;
	}

	public String getXAxisLabel() {
		return xAxisLabel;
	}

	private Rectangle2D getXLabelBounds(final Graphics2D g, final int width,
			final int height, double x, final String label) {
		final FontMetrics fmLabel = g.getFontMetrics(labelFont);
		x = (x - viewport.getMinX()) / viewport.getWidth();
		x *= width - chartInsets.left - chartInsets.right;
		x += chartInsets.left;
		final Rectangle2D r = fmLabel.getStringBounds(label, g);
		r.setFrameFromCenter(x,
				height - chartInsets.bottom + xTickSize + r.getHeight() / 2.0,
				x - r.getWidth() / 2.0, height - chartInsets.bottom + xTickSize);
		return r;
	}

	public int getXTickSize() {
		return xTickSize;
	}

	public String getYAxisLabel() {
		return yAxisLabel;
	}

	private Rectangle2D getYLabelBounds(final Graphics2D g, final int width,
			final int height, double y, final String label) {
		final FontMetrics fmLabel = g.getFontMetrics(labelFont);
		y = getViewport().getMaxY() - y;
		y /= viewport.getHeight();
		y *= height - chartInsets.top - chartInsets.bottom;
		y += chartInsets.top;
		final Rectangle2D r = fmLabel.getStringBounds(label, g);
		r.setFrameFromCenter(chartInsets.left - 5 - yTickSize - r.getWidth() / 2.0,
				y, chartInsets.left - 5 - yTickSize, y - r.getHeight() / 2.0);
		return r;
	}

	public int getYTickSize() {
		return yTickSize;
	}

	@Override
	public void paint(final Graphics g) {
		g.clearRect(0, 0, getWidth(), getHeight());
		g.translate(borderInsets.left, borderInsets.right);
		draw((Graphics2D) g, getWidth() - borderInsets.left - borderInsets.right,
				getHeight() - borderInsets.top - borderInsets.bottom);
		/**
		 * Jason's computer showed extremely slow zooming and using the buffered image seemed to fix it which was done in 2.0 beta 3.1. However
		 * in testing in prep for 2.0 beta 7, I noticed that the zoom feature was not quite correct -- when you slected stuff you got the area
		 * to the left of the selection point so this change is gone for now. Let's hope that it still works on Jason
		 */
		// final BufferedImage image = getImage(getWidth() - borderInsets.left
		// - borderInsets.right, getHeight() - borderInsets.top
		// - borderInsets.bottom);
		// g.drawImage(image, 0, 0, this);
	}

	public void setAxisFont(final Font axisFont) {
		setAxisFont(axisFont, true);
	}

	public void setAxisFont(final Font axisFont, final boolean repaint) {
		this.axisFont = axisFont;
		update(repaint);
	}

	public void setBorderInsets(final Insets borderInsets) {
		setBorderInsets(borderInsets, true);
	}

	public void setBorderInsets(final Insets borderInsets, final boolean repaint) {
		this.borderInsets = borderInsets;
		update(repaint);
	}

	public void setLabelFont(final Font labelFont) {
		setAxisFont(labelFont, true);
	}

	public void setLabelFont(final Font labelFont, final boolean repaint) {
		this.labelFont = labelFont;
		update(repaint);
	}

	public void setViewport(final Rectangle2D viewport) {
		setViewport(viewport, true);
	}

	public void setViewport(final Rectangle2D viewport, final boolean repaint) {
		this.viewport = viewport;
		update(repaint);
	}

	public void setXAxisLabel(final String xAxisLabel) {
		setXAxisLabel(xAxisLabel, true);
	}

	public void setXAxisLabel(final String xAxisLabel, final boolean repaint) {
		this.xAxisLabel = xAxisLabel;
		update(repaint);
	}

	public void setXTickSize(final int xTickSize) {
		setXTickSize(xTickSize, true);
	}

	public void setXTickSize(final int xTickSize, final boolean repaint) {
		this.xTickSize = xTickSize;
		update(repaint);
	}

	public void setYAxisLabel(final String yAxisLabel) {
		setYAxisLabel(yAxisLabel, true);
	}

	public void setYAxisLabel(final String yAxisLabel, final boolean repaint) {
		this.yAxisLabel = yAxisLabel;
		update(repaint);
	}

	public void setYTickSize(final int yTickSize) {
		setYTickSize(yTickSize, true);
	}

	public void setYTickSize(final int yTickSize, final boolean repaint) {
		this.yTickSize = yTickSize;
		update(repaint);
	}

	private void update(final boolean repaint) {
		dirtyInsets = true;
		if (repaint && isVisible()) {
			repaint();
		}
	}
}
