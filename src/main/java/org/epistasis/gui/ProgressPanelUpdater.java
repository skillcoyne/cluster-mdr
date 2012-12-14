package org.epistasis.gui;
public class ProgressPanelUpdater implements Runnable {
	private final long max;
	private long value = 0;
	private final ProgressPanel prgProgress;
	public ProgressPanelUpdater(final ProgressPanel prgProgress, final long max) {
		this.prgProgress = prgProgress;
		this.max = max;
	}
	private synchronized double getNextValue() {
		return (double) (++value) / max;
	}
	public void run() {
		prgProgress.setValue(getNextValue());
	}
}
