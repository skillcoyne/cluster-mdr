package org.epistasis;

import java.util.TimerTask;

public class TimerRunnableTask extends TimerTask {
	private final Runnable target;

	public TimerRunnableTask(final Runnable target) {
		this.target = target;
	}

	@Override
	public void run() {
		target.run();
	}
}
