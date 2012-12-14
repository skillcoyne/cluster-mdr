package org.epistasis.gui;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JProgressBar;
public class ProgressPanel extends TitledPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JProgressBar prgProgress = new JProgressBar();
	private double value = 0;
	public ProgressPanel() {
		super(new GridBagLayout());
		jbInit();
	}
	public double getValue() {
		return value;
	}
	private void jbInit() {
		setTitle("Progress Completed");
		this.add(prgProgress, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
						0), 240, 253));
		this.add(prgProgress, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5,
						5), 0, 0));
		prgProgress.setStringPainted(true);
	}
	public void setIndeterminate(final boolean newValue) {
		prgProgress.setIndeterminate(newValue);
	}
	public void setString(final String stringToDisplay) {
		prgProgress.setString(stringToDisplay);
	}
	public void setValue(final double value) {
		this.value = value;
		prgProgress.setValue(Math.round((float) value * 100.0f));
	}
}
