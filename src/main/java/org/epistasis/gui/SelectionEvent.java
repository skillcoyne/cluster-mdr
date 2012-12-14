package org.epistasis.gui;
import java.awt.geom.Rectangle2D;
import java.util.EventObject;
public class SelectionEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Rectangle2D viewport;
	private final Rectangle2D selection;
	public SelectionEvent(final SelectableComponent source,
			final Rectangle2D viewport, final Rectangle2D selection) {
		super(source);
		this.viewport = viewport;
		this.selection = selection;
	}
	public Rectangle2D getSelection() {
		return selection;
	}
	public Rectangle2D getViewport() {
		return viewport;
	}
}
