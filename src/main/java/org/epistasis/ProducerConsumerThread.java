package org.epistasis;

import java.util.concurrent.BlockingQueue;

public class ProducerConsumerThread<E> extends Thread {
	private final BlockingQueue<E> queue = new ArrayBlockingQueue<E>(100);
	private final ThreadPool threads = new ThreadPool();
	private Producer<E> producer = null;

	public void addConsumer(final Consumer<E> consumer) {
		consumer.setQueue(queue);
		threads.add(consumer);
	}

	public void clearConsumers() {
		threads.clear();
		if (producer != null) {
			threads.add(producer);
		}
	}

	@Override
	public void interrupt() {
		threads.interrupt();
		super.interrupt();
	}

	@Override
	public void run() {
		if (producer == null) {
			throw new IllegalStateException("No Producer");
		}
		if (threads.size() < 2) {
			throw new IllegalStateException("No Consumers");
		}
		threads.run();
	}

	public void setProducer(final Producer<E> producer) {
		if (this.producer != null) {
			threads.remove(this.producer);
			this.producer.setQueue(null);
		}
		this.producer = producer;
		if (producer != null) {
			producer.setQueue(queue);
			threads.add(producer);
		}
	}

	public abstract static class Consumer<E> implements Runnable {
		private BlockingQueue<E> queue;

		public abstract void consume(E obj);

		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					final E obj = queue.take();
					if (obj == null) {
						queue.put(null);
						return;
					}
					consume(obj);
				}
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		public void setQueue(final BlockingQueue<E> queue) {
			this.queue = queue;
		}
	}

	public abstract static class Producer<E> implements Runnable {
		private BlockingQueue<E> queue;

		public abstract E produce();

		public void run() {
			E obj = null;
			try {
				while (((obj = produce()) != null)
						&& !Thread.currentThread().isInterrupted()) {
					queue.put(obj);
				}
				queue.put(null);
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		public void setQueue(final BlockingQueue<E> queue) {
			this.queue = queue;
		}
	}
}
