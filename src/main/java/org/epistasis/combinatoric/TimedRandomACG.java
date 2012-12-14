package org.epistasis.combinatoric;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.epistasis.AttributeLabels;

public class TimedRandomACG extends RandomACG {
	private Timer t;
	private final long millis;
	private boolean hasNext = true;

	public TimedRandomACG(final AttributeLabels labels, final int attrCount,
			final Random rand, final long millis) {
		super(labels, attrCount, rand);
		this.millis = millis;
	}

	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public synchronized AttributeCombination next() {
		if (t == null) {
			t = new Timer(true);
			t.schedule(new SetDoneTask(), millis);
		}
		return super.next();
	}

	private synchronized void setDone() {
		hasNext = false;
	}

	private class SetDoneTask extends TimerTask {
		@Override
		public void run() {
			setDone();
		}
	}
}
