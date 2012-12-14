package org.epistasis.combinatoric.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.filechooser.FileFilter;

import org.epistasis.DisplayPair;
import org.epistasis.FileSaver;
import org.epistasis.Pair;
import org.epistasis.Utility;
import org.epistasis.gui.AbstractChart;
import org.epistasis.gui.SelectionEvent;
import org.epistasis.gui.SelectionListener;

public class LandscapePanel extends JComponent implements ItemListener,
		SelectionListener, ActionListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean useMinMax;
	private List<Pair<String, Float>> landscape = null;
	private Float landscapeMin = null;
	private Float landscapeMax = null;
	private Comparator<Pair<String, Float>> rawTextComparator;
	private final BorderLayout bolThis = new BorderLayout();
	private final JPanel pnlControls = new JPanel();
	private final JPanel pnlDisplay = new JPanel();
	private final CardLayout crdDisplay = new CardLayout();
	private final JScrollPane scpLineChart = new JScrollPane();
	private final JScrollPane scpHistogram = new JScrollPane();
	private final JScrollPane scpRaw = new JScrollPane();
	private final JComboBox cboDisplayType = new JComboBox();
	private final JPanel pnlDisplayControls = new JPanel();
	private final JButton cmdSave = new JButton();
	private final JButton cmdUnzoom = new JButton();
	private final JButton cmdResetView = new JButton();
	private final JPanel pnlRawControls = new JPanel();
	private final JPanel pnlHistogramControls = new JPanel();
	private final JPanel pnlLineChartControls = new JPanel();
	private final CardLayout crdDisplayControls = new CardLayout();
	private final GridBagLayout gblControls = new GridBagLayout();
	private final GridBagLayout gblHistogramControls = new GridBagLayout();
	private final JLabel lblBins = new JLabel();
	private final JSpinner spnBins = new JSpinner();
	private final JPanel pnlZoomControls = new JPanel();
	private final JPanel pnlZoomable = new JPanel();
	private final JPanel pnlUnzoomable = new JPanel();
	private final CardLayout crdZoomControls = new CardLayout();
	private final GridBagLayout gblZoomable = new GridBagLayout();
	private final JTextArea txaRaw = new JTextArea();
	private final LandscapeHistogram pnlHistogram = new LandscapeHistogram();
	private final LandscapeLineChart pnlLineChart = new LandscapeLineChart();
	private final Stack<Rectangle2D> stkLineChartZoom = new Stack<Rectangle2D>();
	private final Stack<Rectangle2D> stkHistogramZoom = new Stack<Rectangle2D>();
	private final JButton cmdMaximize = new JButton();
	private final EventListenerList listenerList = new EventListenerList();

	public LandscapePanel() {
		jbInit();
	}

	public void actionPerformed(final ActionEvent e) {
		@SuppressWarnings("unchecked")
		final Pair<String, Component> p = (Pair<String, Component>) cboDisplayType
				.getSelectedItem();
		AbstractChart chart = null;
		Dimension chartSize = null;
		Stack<Rectangle2D> stack = null;
		String saveTitle = null;
		List<? extends FileFilter> filters = null;
		if (p.getSecond() == scpLineChart) {
			stack = stkLineChartZoom;
			chart = pnlLineChart;
			chartSize = new Dimension(1200, 400);
			saveTitle = "Save Landscape Line Chart";
			filters = FileSaver.fltGraphics;
		} else if (p.getSecond() == scpHistogram) {
			stack = stkHistogramZoom;
			chart = pnlHistogram;
			chartSize = new Dimension(1000, 1000);
			saveTitle = "Save Landscape Histogram";
			filters = FileSaver.fltGraphics;
		} else {
			saveTitle = "Save Landscape Raw Text";
			filters = FileSaver.fltText;
		}
		if ((e.getSource() == cmdUnzoom) && (stack != null) && (chart != null)) {
			final Rectangle2D viewport = stack.pop();
			chart.setViewport(viewport);
			checkEnabled(isEnabled());
		} else if ((e.getSource() == cmdResetView) && (stack != null)
				&& (chart != null)) {
			chart.setViewport(stack.firstElement());
			stack.clear();
			checkEnabled(isEnabled());
		} else if (e.getSource() == cmdSave) {
			try {
				final Pair<File, FileFilter> ff = FileSaver.getSaveFile(
						getTopLevelAncestor(), saveTitle, filters);
				if (ff == null) {
					return;
				}
				if ((chart == null) || (chartSize == null)) {
					FileSaver.saveText(txaRaw.getText(), ff.getFirst());
				} else if (ff.getSecond() == FileSaver.epsFilter) {
					final Writer w = new FileWriter(ff.getFirst());
					w.write(chart.getEpsText(chartSize.width, chartSize.height));
					w.flush();
					w.close();
				} else if (ff.getSecond() == FileSaver.pngFilter) {
					ImageIO.write(chart.getImage(chartSize.width, chartSize.height),
							"png", ff.getFirst());
				} else if (ff.getSecond() == FileSaver.jpgFilter) {
					ImageIO.write(chart.getImage(chartSize.width, chartSize.height),
							"jpeg", ff.getFirst());
				}
			} catch (final IOException ex) {
				Utility.logException(ex);
				JOptionPane.showMessageDialog(getTopLevelAncestor(), ex.getMessage(),
						"I/O Error", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			throw new IllegalArgumentException(e.getActionCommand());
		}
	}

	public void addActionListener(final ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}

	private long[] binLandscape(final int nBins) {
		if (landscape == null) {
			return null;
		}
		final long[] bins = new long[nBins];
		for (final Pair<String, Float> p : landscape) {
			float fitness = p.getSecond();
			if (Float.isInfinite(fitness)) {
				continue;
			}
			if (useMinMax && (landscapeMin != null) && (landscapeMax != null)) {
				fitness = (fitness - landscapeMin) / (landscapeMax - landscapeMin);
			}
			if (fitness >= 1.0) {
				bins[bins.length - 1]++;
			} else {
				bins[(int) (fitness * nBins)]++;
			}
		}
		return bins;
	}

	@SuppressWarnings("unchecked")
	private void checkEnabled(final boolean enabled) {
		cboDisplayType.setEnabled(enabled);
		cmdSave.setEnabled(enabled);
		lblBins.setEnabled(enabled);
		spnBins.setEnabled(enabled);
		pnlLineChart.setSelectionEnabled(enabled);
		pnlHistogram.setSelectionEnabled(enabled);
		final Pair<String, Component> p = (Pair<String, Component>) cboDisplayType
				.getSelectedItem();
		if (enabled) {
			boolean zoomed = false;
			if (p.getSecond() == scpLineChart) {
				zoomed = !stkLineChartZoom.empty();
			} else if (p.getSecond() == scpHistogram) {
				zoomed = !stkHistogramZoom.empty();
			}
			cmdUnzoom.setEnabled(zoomed);
			cmdResetView.setEnabled(zoomed);
		} else {
			cmdUnzoom.setEnabled(false);
			cmdResetView.setEnabled(false);
		}
	}

	public void cmdMaximize_actionPerformed(final ActionEvent e) {
		if (cmdMaximize.getText().equals("Maximize")) {
			cmdMaximize.setText("Restore");
		} else {
			cmdMaximize.setText("Maximize");
		}
		fireActionEvent(e);
	}

	protected void fireActionEvent(final ActionEvent e) {
		final ActionListener[] listeners = listenerList
				.getListeners(ActionListener.class);
		for (int i = 0; i < listeners.length; ++i) {
			listeners[i].actionPerformed(e);
		}
	}

	public Font getTextFont() {
		return txaRaw.getFont();
	}

	public String getXAxisLabel() {
		return pnlLineChart.getXAxisLabel();
	}

	public String getYAxisLabel() {
		return pnlLineChart.getYAxisLabel();
	}

	@SuppressWarnings("unchecked")
	public void itemStateChanged(final ItemEvent e) {
		final Pair<String, Component> p = (Pair<String, Component>) e.getItem();
		crdDisplay.show(pnlDisplay, p.toString());
		crdDisplayControls.show(pnlDisplayControls, p.toString());
		if (p.getSecond() == scpLineChart) {
			crdZoomControls.show(pnlZoomControls, "Zoomable");
		} else if (p.getSecond() == scpHistogram) {
			crdZoomControls.show(pnlZoomControls, "Zoomable");
		} else if (p.getSecond() == scpRaw) {
			crdZoomControls.show(pnlZoomControls, "Unzoomable");
		}
		checkEnabled(isEnabled());
	}

	private void jbInit() {
		setLayout(bolThis);
		pnlDisplay.setLayout(crdDisplay);
		cmdSave.setText("Save");
		cmdSave.addActionListener(this);
		cmdUnzoom.addActionListener(this);
		cmdResetView.addActionListener(this);
		cmdUnzoom.setText("Unzoom");
		cmdResetView.setText("Reset View");
		pnlDisplayControls.setLayout(crdDisplayControls);
		pnlControls.setLayout(gblControls);
		pnlHistogramControls.setLayout(gblHistogramControls);
		lblBins.setText("Bins:");
		spnBins.setMinimumSize(new Dimension(60, 20));
		spnBins.setPreferredSize(new Dimension(60, 20));
		spnBins.addChangeListener(this);
		spnBins.setModel(new SpinnerNumberModel(new Integer(10), new Integer(1),
				null, new Integer(1)));
		pnlZoomControls.setLayout(crdZoomControls);
		pnlZoomable.setLayout(gblZoomable);
		scpLineChart.setBorder(BorderFactory.createLoweredBevelBorder());
		scpHistogram.setBorder(BorderFactory.createLoweredBevelBorder());
		scpRaw.setBorder(BorderFactory.createLoweredBevelBorder());
		cboDisplayType.addItemListener(this);
		txaRaw.setEditable(false);
		pnlHistogram.setYAxisLabel("Count");
		cmdMaximize.setText("Maximize");
		cmdMaximize.addActionListener(new LandscapePanel_cmdMaximize_actionAdapter(
				this));
		this.add(pnlControls, java.awt.BorderLayout.SOUTH);
		this.add(pnlDisplay, java.awt.BorderLayout.CENTER);
		pnlDisplay.add(scpLineChart, "Line Chart");
		pnlDisplay.add(scpHistogram, "Histogram");
		pnlDisplay.add(scpRaw, "Raw Text");
		scpLineChart.getViewport().add(pnlLineChart);
		scpHistogram.getViewport().add(pnlHistogram);
		scpRaw.getViewport().add(txaRaw);
		pnlLineChart.addSelectionListener(this);
		pnlHistogram.addSelectionListener(this);
		cboDisplayType.addItem(new DisplayPair<String, Component>("Line Chart",
				scpLineChart));
		cboDisplayType.addItem(new DisplayPair<String, Component>("Histogram",
				scpHistogram));
		cboDisplayType.addItem(new DisplayPair<String, Component>("Raw Text",
				scpRaw));
		pnlZoomControls.add(pnlZoomable, "Zoomable");
		pnlZoomControls.add(pnlUnzoomable, "Unzoomable");
		pnlZoomable.add(cmdUnzoom, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
						0), 0, 0));
		pnlZoomable.add(cmdResetView, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
						0), 0, 0));
		pnlHistogramControls.add(lblBins, new GridBagConstraints(1, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5,
						5, 5, 0), 0, 0));
		pnlHistogramControls.add(spnBins, new GridBagConstraints(2, 0, 1, 1, 1.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5,
						5, 5), 0, 0));
		pnlDisplayControls.add(pnlLineChartControls, "Line Chart");
		pnlDisplayControls.add(pnlHistogramControls, "Histogram");
		pnlDisplayControls.add(pnlRawControls, "Raw Text");
		pnlControls.add(pnlZoomControls, new GridBagConstraints(1, 0, 1, 2, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
						0, 0, 0), 0, 0));
		pnlControls.add(cboDisplayType, new GridBagConstraints(0, 0, 1, 2, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 5, 0), 0, 0));
		pnlControls.add(pnlDisplayControls, new GridBagConstraints(3, 0, 1, 2, 1.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,
						0, 0, 0), 0, 0));
		pnlControls.add(cmdMaximize, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
						0), 0, 0));
		pnlControls.add(cmdSave, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
						5), 0, 0));
	}

	public void removeActionListener(final ActionListener l) {
		listenerList.remove(ActionListener.class, l);
	}

	public void selectionChanged(final SelectionEvent e) {
		final AbstractChart chart = (AbstractChart) e.getSource();
		final Rectangle2D current = e.getViewport();
		if (chart == pnlLineChart) {
			stkLineChartZoom.push(current);
		} else if (chart == pnlHistogram) {
			stkHistogramZoom.push(current);
		}
		checkEnabled(isEnabled());
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		if (cboDisplayType == null) {
			return;
		}
		checkEnabled(enabled);
	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		if (cboDisplayType == null) {
			return;
		}
		cboDisplayType.setFont(font);
		cmdSave.setFont(font);
		cmdUnzoom.setFont(font);
		cmdResetView.setFont(font);
		cmdMaximize.setFont(font);
		spnBins.setFont(font);
		lblBins.setFont(font);
	}

	public void setLandscape(final List<Pair<String, Float>> landscape) {
		setLandscape(landscape, (Comparator<Pair<String, Float>>) null);
	}

	public void setLandscape(final List<Pair<String, Float>> landscape,
			final boolean scaleToFit) {
		setLandscape(landscape, scaleToFit, null);
	}

	private void setLandscape(final List<Pair<String, Float>> landscape,
			final boolean scaleToFit,
			final Comparator<Pair<String, Float>> rawTextComparator) {
		this.rawTextComparator = rawTextComparator;
		useMinMax = scaleToFit;
		setLandscapeCommon(landscape);
		if (scaleToFit && (landscapeMin != null) && (landscapeMax != null)) {
			pnlLineChart.setViewport(new Rectangle2D.Float(0, landscapeMin, 1,
					landscapeMax - landscapeMin));
		}
		pnlLineChart.setLandscape(landscape, true);
	}

	public void setLandscape(final List<Pair<String, Float>> landscape,
			final Comparator<Pair<String, Float>> rawTextComparator) {
		setLandscape(landscape, true, rawTextComparator);
	}

	private void setLandscapeCommon(final List<Pair<String, Float>> landscape) {
		this.landscape = landscape;
		if (!stkLineChartZoom.empty()) {
			pnlLineChart.setViewport(stkLineChartZoom.get(0));
			stkLineChartZoom.clear();
		}
		if (landscape == null) {
			landscapeMin = landscapeMax = null;
		} else {
			float min = Float.POSITIVE_INFINITY;
			float max = Float.NEGATIVE_INFINITY;
			for (final Pair<String, Float> p : landscape) {
				final float fitness = p.getSecond();
				if (Float.isInfinite(fitness)) {
					continue;
				}
				if (fitness < min) {
					min = fitness;
				}
				if (fitness > max) {
					max = fitness;
				}
			}
			landscapeMin = new Float(min - max * 0.05);
			landscapeMax = new Float(max * 1.05);
		}
		if (!stkHistogramZoom.empty()) {
			pnlHistogram.setViewport(stkHistogramZoom.get(0));
			stkHistogramZoom.clear();
		}
		if (useMinMax && (landscapeMin != null) && (landscapeMax != null)) {
			pnlHistogram.setBins(
					binLandscape(((Number) spnBins.getValue()).intValue()),
					landscapeMin.floatValue(), landscapeMax.floatValue());
		} else {
			pnlHistogram.setBins(binLandscape(((Number) spnBins.getValue())
					.intValue()));
		}
		// cboDisplayType.setSelectedIndex(0);
		if (landscape == null) {
			txaRaw.setText("");
		} else {
			final StringBuffer b = new StringBuffer();
			if (rawTextComparator != null) {
				Collections.sort(landscape, rawTextComparator);
			}
			b.append(getXAxisLabel());
			b.append('\t');
			b.append(getYAxisLabel());
			b.append('\n');
			for (final Pair<String, Float> p : landscape) {
				b.append(p.getFirst());
				b.append('\t');
				b.append(Float.toString(p.getSecond()));
				b.append('\n');
			}
			txaRaw.setText(b.toString());
			txaRaw.select(0, 0);
		}
	}

	public void setTextFont(final Font font) {
		txaRaw.setFont(font);
	}

	public void setXAxisLabel(final String label) {
		pnlLineChart.setXAxisLabel(label);
	}

	public void setYAxisLabel(final String label) {
		pnlLineChart.setYAxisLabel(label);
		pnlHistogram.setXAxisLabel(label);
	}

	public void stateChanged(final ChangeEvent e) {
		if ((landscapeMin != null) && (landscapeMax != null)) {
			pnlHistogram.setBins(
					binLandscape(((Number) spnBins.getValue()).intValue()),
					landscapeMin.floatValue(), landscapeMax.floatValue());
		} else {
			pnlHistogram.setBins(binLandscape(((Number) spnBins.getValue())
					.intValue()));
		}
	}
}

class LandscapePanel_cmdMaximize_actionAdapter implements ActionListener {
	private final LandscapePanel adaptee;

	LandscapePanel_cmdMaximize_actionAdapter(final LandscapePanel adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(final ActionEvent e) {
		adaptee.cmdMaximize_actionPerformed(e);
	}
}
