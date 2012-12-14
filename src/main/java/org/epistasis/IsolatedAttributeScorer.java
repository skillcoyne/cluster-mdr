package org.epistasis;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
public abstract class IsolatedAttributeScorer extends AbstractAttributeScorer {
	private final boolean parallel;
	public IsolatedAttributeScorer(final Dataset data, final boolean parallel) {
		super(data);
		this.parallel = parallel;
	}
	public IsolatedAttributeScorer(final Dataset data, final boolean parallel,
			final Runnable onIncrementProgress) {
		super(data, onIncrementProgress);
		this.parallel = parallel;
	}
	protected abstract double computeScore(int index);
	@Override
	protected double[] computeScores() {
		final double[] scores = new double[getData().getCols() - 1];
		final int processorsToUse = parallel ? Runtime.getRuntime()
				.availableProcessors() : 1;
		if (processorsToUse > 1) {
			final ProducerConsumerThread<Integer> thread = new ProducerConsumerThread<Integer>();
			thread.setProducer(new Producer());
			for (int i = 0; i < (processorsToUse); ++i) {
				thread.addConsumer(new Consumer(scores));
			}
			thread.run();
		} else {
			for (int i = 0; i < scores.length; ++i) {
				scores[i] = computeScore(i);
				incrementProgress();
			}
		}
		return scores;
	}
	@Override
	public int getTotalProgress() {
		return getData().getCols() - 1;
	}
	private class Consumer extends ProducerConsumerThread.Consumer<Integer> {
		private final double[] scores;
		public Consumer(final double[] scores) {
			this.scores = scores;
		}
		@Override
		public void consume(final Integer i) {
			scores[i] = computeScore(i);
			incrementProgress();
		}
	}
	private class Producer extends ProducerConsumerThread.Producer<Integer> {
		private int i = 0;
		@Override
		public Integer produce() {
			Integer result;
			if (i < getData().getCols() - 1) {
				result = i++;
			} else {
				result = null;
			}
			return result;
		}
	}
}
