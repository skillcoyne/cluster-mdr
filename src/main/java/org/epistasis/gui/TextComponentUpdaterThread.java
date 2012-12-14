package org.epistasis.gui;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
public class TextComponentUpdaterThread extends Thread {
	private final JTextComponent component;
	private final Object textgen;
	private final Runnable target;
	public TextComponentUpdaterThread(final JTextComponent component,
			final Object textgen) {
		this(component, textgen, null);
	}
	public TextComponentUpdaterThread(final JTextComponent component,
			final Object textgen, final Runnable target) {
		this.component = component;
		this.textgen = textgen;
		this.target = target;
		setName("TextComponentUpdaterThread");
	}
	@Override
	public void run() {
		SwingUtilities.invokeLater(new TextSetter(textgen.toString()));
	}
	private class TextSetter implements Runnable {
		private final String text;
		public TextSetter(final String text) {
			this.text = text;
		}
		public void run() {
			component.setText(text);
			component.select(0, 0);
			if (target != null) {
				target.run();
			}
		}
	}
}
