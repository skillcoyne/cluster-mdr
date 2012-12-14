package org.epistasis.combinatoric.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.epistasis.gui.AbstractChart;
import org.epistasis.gui.SelectionEvent;
import org.epistasis.gui.SelectionListener;

public class LandscapeHistogram extends AbstractChart implements
		SelectionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long[] bins;
	private double xmin;
	private double xmax;
	private long binmax = 1;
	private long adjustedmax;
	private Color barColor = Color.LIGHT_GRAY;
	private int xticks;
	private int yticks;
	private boolean zoom1d = true;

	public LandscapeHistogram() {
		setXTicks(10, false);
		setYTicks(10, false);
		addSelectionListener(this);
	}

	private void calcAdjustedMax() {
		if (yticks > 0) {
			adjustedmax = (long) Math.ceil((double) binmax / yticks) * yticks;
			clearYLabels(false);
			final NumberFormat nf = new DecimalFormat("#");
			((DecimalFormat) nf).setGroupingSize(3);
			nf.setGroupingUsed(true);
			for (int i = 0; i <= yticks; ++i) {
				final double y = (double) i / yticks * getViewport().getHeight()
						+ getViewport().getMinY();
				addYLabel(y, nf.format(y * adjustedmax), false);
			}
		} else {
			adjustedmax = binmax;
		}
	}

	@Override
	public void drawContents(final Graphics2D g, final int width, final int height) {
		if ((bins == null) || (bins.length == 0)) {
			return;
		}
		final int min = (int) Math.floor(getViewport().getMinX()
				* (bins.length - 1));
		final int max = (int) Math
				.ceil(getViewport().getMaxX() * (bins.length - 1));
		for (int i = min; i <= max; ++i) {
			final int tempXmin = (int) Math.round(Math.max(
					(((double) i / bins.length) - getViewport().getMinX())
							/ getViewport().getWidth() * width, 0));
			final int tempXmax = (int) Math.round(Math.min(
					(((double) (i + 1) / bins.length) - getViewport().getMinX())
							/ getViewport().getWidth() * width, width));
			final int binheight = (int) Math
					.round(((bins[i] / (double) adjustedmax) - getViewport().getMinY())
							/ getViewport().getHeight() * height);
			g.setColor(barColor);
			g.fillRect(tempXmin, 0, tempXmax - tempXmin, binheight);
			g.setColor(getForeground());
			g.drawRect(tempXmin, 0, tempXmax - tempXmin, binheight);
		}
	}

	public Color getBarColor() {
		return barColor;
	}

	public int getXTicks() {
		return xticks;
	}

	public int getYTicks() {
		return yticks;
	}

	public boolean is1DZoom() {
		return zoom1d;
	}

	@Override
	public void paint(final Graphics g) {
		if ((bins == null) || (bins.length == 0)) {
			g.clearRect(0, 0, getWidth(), getHeight());
		} else {
			super.paint(g);
		}
	}

	@Override
	protected void selectionChanged(final Rectangle2D before,
			final Rectangle2D after, final boolean done) {
		if (zoom1d) {
			final int ymin = getBorderInsets().top + getChartInsets().top;
			final int ymax = getHeight() - getBorderInsets().bottom
					- getChartInsets().bottom;
			super.selectionChanged(before == null ? null : new Rectangle2D.Double(
					before.getMinX(), ymin, before.getWidth(), ymax - ymin),
					new Rectangle2D.Double(after.getMinX(), ymin, after.getWidth(), ymax
							- ymin), done);
		} else {
			super.selectionChanged(before, after, done);
		}
	}

	public void selectionChanged(final SelectionEvent e) {
		final Rectangle2D chart = getSelectable();
		final AffineTransform xform = new AffineTransform();
		xform.translate(getViewport().getX(), getViewport().getY());
		xform.scale(getViewport().getWidth(), getViewport().getHeight());
		xform.translate(0, 1);
		xform.scale(1, -1);
		xform.scale(1.0 / chart.getWidth(), 1.0 / chart.getHeight());
		xform.translate(-chart.getX(), -chart.getY());
		final Rectangle2D v = xform.createTransformedShape(e.getSelection())
				.getBounds2D();
		setViewport(v);
	}

	public void set1DZoom(final boolean zoom1d) {
		cancelSelection();
		this.zoom1d = zoom1d;
	}

	public void setBarColor(final Color barColor) {
		setBarColor(barColor, true);
	}

	public void setBarColor(final Color barColor, final boolean repaint) {
		this.barColor = barColor;
		if (repaint && isVisible()) {
			repaint();
		}
	}

	public void setBins(final long[] bins) {
		setBins(bins, true);
	}

	public void setBins(final long[] bins, final boolean repaint) {
		setBins(bins, 0, 1, repaint);
	}

	public void setBins(final long[] bins, final double pXmin, final double pXmax) {
		setBins(bins, pXmin, pXmax, true);
	}

	public void setBins(final long[] pBins, final double pXmin,
			final double pXmax, final boolean repaint) {
		binmax = 1;
		bins = pBins;
		xmin = pXmin;
		xmax = pXmax;
		for (int i = 0; (bins != null) && (i < bins.length); ++i) {
			if (bins[i] > binmax) {
				binmax = bins[i];
			}
		}
		setSelectionEnabled((bins != null) && (bins.length > 0));
		calcAdjustedMax();
		setXTicks(getXTicks(), false);
		if (repaint && isVisible()) {
			repaint();
		}
	}

	@Override
	public void setViewport(final Rectangle2D viewport, final boolean repaint) {
		super.setViewport(viewport, false);
		setXTicks(getXTicks(), false);
		setYTicks(getYTicks(), repaint);
	}

	public void setXTicks(final int xticks) {
		setXTicks(xticks, true);
	}

	public void setXTicks(final int xticks, final boolean repaint) {
		this.xticks = xticks;
		clearXLabels(false);
		final NumberFormat nf = new DecimalFormat("0.0###");
		for (int i = 0; i <= xticks; ++i) {
			final double x = (double) i / xticks * getViewport().getWidth()
					+ getViewport().getMinX();
			addXLabel(x, nf.format(x * (xmax - xmin) + xmin), false);
		}
		if (repaint && isVisible()) {
			repaint();
		}
	}

	public void setYTicks(final int yticks) {
		setYTicks(yticks, true);
	}

	public void setYTicks(final int yticks, final boolean repaint) {
		this.yticks = yticks;
		calcAdjustedMax();
		if (repaint && isVisible()) {
			repaint();
		}
	}
}
