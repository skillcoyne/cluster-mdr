package org.epistasis.combinatoric.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.epistasis.Pair;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.gui.AbstractChart;
import org.epistasis.gui.SelectionEvent;
import org.epistasis.gui.SelectionListener;

public class LandscapeLineChart extends AbstractChart implements
		SelectionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Pair<String, Float>> landscape;
	private boolean xticks = true;
	private int yticks = 10;
	private int dotradius = 3;
	int[] xPoints = new int[0];
	int[] yPoints = new int[0];
	long first;
	long last;

	public LandscapeLineChart() {
		setYTicks(yticks, false);
		addSelectionListener(this);
		setToolTipText("");
	}

	@Override
	public void drawContents(final Graphics2D g, final int width, final int height) {
		if ((landscape == null) || landscape.isEmpty()) {
			return;
		}
		// use member variables so that getTooltip can determine if mouse is over
		first = (long) Math.floor(getViewport().getMinX() * (landscape.size() - 1));
		last = (long) Math.min(
				Math.ceil(getViewport().getMaxX() * (landscape.size() - 1)),
				landscape.size() - 1);
		// System.out.println("First: " + first + " Last; " + last);
		xPoints = new int[(int) (last - first) + 1];
		yPoints = new int[xPoints.length];
		final boolean drawDots = (dotradius > 0)
				&& (((1.0 / (landscape.size() - 1)) / getViewport().getWidth() * width) >= dotradius * 2);
		for (int i = 0; i < xPoints.length; ++i) {
			double x;
			if (xPoints.length > 1) {
				x = (double) (i + first) / (landscape.size() - 1);
				x -= getViewport().getMinX();
				x /= getViewport().getWidth();
				x *= width;
			} else {
				x = width / 2.0;
			}
			xPoints[i] = (int) Math.round(x);
			double y = landscape.get((int) (i + first)).getSecond().doubleValue();
			y -= getViewport().getMinY();
			y /= getViewport().getHeight();
			y *= height;
			if (y == Double.POSITIVE_INFINITY) {
				yPoints[i] = Short.MAX_VALUE;
			} else if (y == Double.NEGATIVE_INFINITY) {
				yPoints[i] = Short.MIN_VALUE;
			} else {
				yPoints[i] = (int) Math.round(y);
			}
			if (drawDots) {
				g.fillOval(xPoints[i] - dotradius, yPoints[i] - dotradius - 1,
						dotradius * 2 + 1, dotradius * 2 + 1);
			}
			// final Point2D src = new Point(xPoints[i], yPoints[i]);
			// final Point2D dest = new Point();
			// final AffineTransform transform = g.getTransform();
			// transform.transform(src, dest);
			// final Point2D inverseDest = new Point();
			// try {
			// transform.inverseTransform(src, dest);
			// } catch (final NoninvertibleTransformException ex) {
			// // TODO Auto-generated catch block
			// ex.printStackTrace();
			// }
			// System.out.println(i + " - src: " + src.toString() + " transformed to: "
			// + dest.toString() + " inverseTransformed to: "
			// + inverseDest.toString());
		}
		g.setColor(getForeground());
		if (xPoints.length > 1) {
			// System.out.println("xPoints: " + Arrays.toString(xPoints));
			// System.out.println("YPoints: " + Arrays.toString(yPoints));
			g.drawPolyline(xPoints, yPoints, xPoints.length);
		}
	}

	public int getDotRadius() {
		return dotradius;
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		String result = null;
		final int height = getHeight() - getBorderInsets().top
				- getBorderInsets().bottom - getChartInsets().top
				- getChartInsets().bottom;
		final int mouseX = e.getX() - getChartInsets().left
				- getBorderInsets().left;
		final int mouseY = height - e.getY() + getBorderInsets().top;
		double bestDistance = Double.MAX_VALUE;
		int bestDrawnPointIndex = -1;
		for (int drawnPointIndex = 0; drawnPointIndex < xPoints.length; ++drawnPointIndex) {
			final int yPoint = yPoints[drawnPointIndex];
			if ((yPoint == Short.MAX_VALUE) || (yPoint == Short.MIN_VALUE)) {
				continue;
			}
			final int xPoint = xPoints[drawnPointIndex];
			final int xDiff = Math.abs(mouseX - xPoint);
			final int threshold = 5;
			if (xDiff <= threshold) {
				final int yDiff = Math.abs(mouseY - yPoint);
				if (yDiff <= (threshold * 10)) {
					final double distance = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
					// System.out.println("drawnPointIndex: " + drawnPointIndex
					// + " pointXY: " + xPoint + "," + yPoint + " event pos: "
					// + e.getX() + "," + e.getY() + " transformed XY: " + mouseX
					// + "," + mouseY + " distance pointXY->transformed XY: "
					// + Main.defaultFormat.format(distance) + " xDiff: " + xDiff + " yDiff: "
					// + yDiff);
					if (distance < bestDistance) {
						bestDistance = distance;
						bestDrawnPointIndex = drawnPointIndex;
					}
				} // //if yDiff close enough
			} // if xDiff close enough
		} // end for loop over drawn points
		if (bestDrawnPointIndex >= 0) {
			final Pair<String, Float> modelPair = landscape.get((int) first
					+ bestDrawnPointIndex);
			result = modelPair.getFirst() + " "
					+ Main.defaultFormat.format(modelPair.getSecond());
		} else {
			// System.out.println("bestDistance = " + Main.defaultFormat.format(bestDistance)
			// + " for bestDrawnPointIndex: " + bestDrawnPointIndex);
		}
		return result;
	}

	public int getYTicks() {
		return yticks;
	}

	public boolean isXTicks() {
		return xticks;
	}

	@Override
	public void paint(final Graphics g) {
		if ((landscape == null) || landscape.isEmpty()) {
			g.clearRect(0, 0, getWidth(), getHeight());
		} else {
			super.paint(g);
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

	public void setDotRadius(final int dotradius) {
		setDotRadius(dotradius, true);
	}

	public void setDotRadius(final int dotradius, final boolean repaint) {
		this.dotradius = dotradius;
		if (repaint && isVisible()) {
			repaint();
		}
	}

	public void setLandscape(final List<Pair<String, Float>> landscape,
			final boolean repaint) {
		this.landscape = landscape;
		setSelectionEnabled((landscape != null) && !landscape.isEmpty());
		setXTicks(xticks, false);
		if (repaint && isVisible()) {
			repaint();
		}
	}

	@Override
	public void setViewport(final Rectangle2D viewport, final boolean repaint) {
		super.setViewport(viewport, false);
		setXTicks(isXTicks(), repaint);
		setYTicks(getYTicks(), repaint);
	}

	public void setXTicks(final boolean xticks) {
		setXTicks(xticks, true);
	}

	public void setXTicks(final boolean xticks, final boolean repaint) {
		this.xticks = xticks;
		clearXLabels();
		if (xticks && (landscape != null) && !landscape.isEmpty()) {
			// two styles of landscape -- one for showing attribute combinations and another for showing attribute values
			// if the last item in the list has only one attribute then assume we are showing attribute values.
			final int firstAttributeIndex = (int) Math.floor(getViewport().getMinX()
					* (landscape.size() - 1));
			final int lastAttributeIndex = (int) Math.min(
					Math.ceil(getViewport().getMaxX() * (landscape.size() - 1)),
					landscape.size() - 1);
			final int numAttributesFirst = landscape.get(firstAttributeIndex)
					.getFirst().split(",").length;
			final int numAttributesLast = landscape.get(lastAttributeIndex)
					.getFirst().split(",").length;
			// System.out.println("firstAttributeIndex: " + firstAttributeIndex
			// + " lastAttributeIndex: " + lastAttributeIndex);
			if (numAttributesFirst == numAttributesLast) {
				for (int index = 0; index < landscape.size(); ++index) {
					final String attributeName = landscape.get(index).getFirst();
					addXLabel(index / (double) (landscape.size() - 1), attributeName);
				}
			} else {
				int previousNumAttributes = 0;
				for (int index = 0; index < landscape.size() - 1; ++index) {
					final String attributeCombinationName = landscape.get(index)
							.getFirst();
					final int numAttributes = attributeCombinationName.split(",").length;
					if (numAttributes != previousNumAttributes) {
						addXLabel(index / (double) (landscape.size() - 1), ""
								+ numAttributes);
						previousNumAttributes = numAttributes;
					}
				}
			}
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
		clearYLabels();
		if (yticks > 0) {
			final NumberFormat nf = new DecimalFormat("0.0###");
			for (int i = 0; i <= yticks; ++i) {
				final double y = (double) i / yticks * getViewport().getHeight()
						+ getViewport().getMinY();
				addYLabel(y, nf.format(y), false);
			}
		}
		if (repaint && isVisible()) {
			repaint();
		}
	}
}
