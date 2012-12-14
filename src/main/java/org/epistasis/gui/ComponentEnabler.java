package org.epistasis.gui;
import javax.swing.JComponent;
public class ComponentEnabler implements Runnable {
	private final JComponent component;
	private final boolean enabled;
	public ComponentEnabler(final JComponent component, final boolean enabled) {
		this.component = component;
		this.enabled = enabled;
	}
	public void run() {
		component.setEnabled(enabled);
	}
}
