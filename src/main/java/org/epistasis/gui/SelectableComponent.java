package org.epistasis.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

public class SelectableComponent extends JComponent implements MouseListener,
		MouseMotionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Point2D dragStart = null;
	private Point2D dragEnd = null;
	private Rectangle2D selected = null;
	private final EventListenerList listenerList = new EventListenerList();
	private final Color selectionColor = UIManager.getLookAndFeelDefaults()
			.getColor("TextField.selectionBackground");
	private boolean selectionEnabled = true;

	public SelectableComponent() {
		setFocusable(true);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setBackground(Color.white);
	}

	public void addSelectionListener(final SelectionListener l) {
		listenerList.add(SelectionListener.class, l);
	}

	protected void cancelSelection() {
		if (selected == null) {
			return;
		}
		final Graphics2D g = (Graphics2D) getGraphics();
		g.setXORMode(Color.WHITE);
		g.setColor(selectionColor);
		g.fill(selected);
		g.setColor(getForeground());
		g.setPaintMode();
		dragStart = dragEnd = null;
		selected = null;
	}

	protected void fireSelectionEvent(final SelectionEvent e) {
		final Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SelectionListener.class) {
				((SelectionListener) listeners[i + 1]).selectionChanged(e);
			}
		}
	}

	public Rectangle2D getSelectable() {
		return getBounds();
	}

	public Rectangle2D getViewport() {
		return getSelectable();
	}

	public boolean isSelectionEnabled() {
		return selectionEnabled;
	}

	public void keyPressed(final KeyEvent e) {
	}

	public void keyReleased(final KeyEvent e) {
	}

	public void keyTyped(final KeyEvent e) {
		if (!selectionEnabled) {
			return;
		}
		if ((dragStart != null) && (e.getKeyChar() == KeyEvent.VK_ESCAPE)) {
			cancelSelection();
		}
	}

	public void mouseClicked(final MouseEvent e) {
		if (!selectionEnabled) {
			return;
		}
		if ((dragStart != null) && (e.getButton() == MouseEvent.BUTTON3)) {
			dragEnd = e.getPoint();
			cancelSelection();
		}
	}

	public void mouseDragged(final MouseEvent e) {
		if (!selectionEnabled) {
			return;
		}
		if (dragStart != null) {
			Rectangle2D before = null;
			final Rectangle2D after = new Rectangle();
			final Rectangle2D selectable = getSelectable();
			if (dragEnd != null) {
				before = new Rectangle();
				before.setFrameFromDiagonal(dragStart, dragEnd);
				Rectangle2D.intersect(before, selectable, before);
			}
			dragEnd = e.getPoint();
			after.setFrameFromDiagonal(dragStart, dragEnd);
			Rectangle2D.intersect(after, selectable, after);
			selectionChanged(before, after, false);
		}
	}

	public void mouseEntered(final MouseEvent e) {
	}

	public void mouseExited(final MouseEvent e) {
	}

	public void mouseMoved(final MouseEvent e) {
	}

	public void mousePressed(final MouseEvent e) {
		if (!selectionEnabled) {
			return;
		}
		if ((dragStart == null) && (e.getButton() == MouseEvent.BUTTON1)) {
			dragStart = e.getPoint();
			requestFocusInWindow(true);
		}
	}

	public void mouseReleased(final MouseEvent e) {
		if (!selectionEnabled) {
			return;
		}
		if ((dragStart != null) && (e.getButton() == MouseEvent.BUTTON1)) {
			Rectangle2D before = null;
			final Rectangle2D after = new Rectangle();
			Rectangle2D selectable = getSelectable();
			if (dragEnd != null) {
				before = new Rectangle();
				before.setFrameFromDiagonal(dragStart, dragEnd);
				Rectangle2D.intersect(before, selectable, before);
			}
			dragEnd = e.getPoint();
			after.setFrameFromDiagonal(dragStart, dragEnd);
			Rectangle2D.intersect(after, selectable, after);
			selectionChanged(before, after, true);
			dragStart = dragEnd = null;
			selectable = null;
		}
	}

	@Override
	public void paint(final Graphics g) {
		g.clearRect(getInsets().left, getInsets().top, getWidth()
				- getInsets().left - getInsets().right, getHeight() - getInsets().top
				- getInsets().bottom);
	}

	public void removeSelectionListener(final SelectionListener l) {
		listenerList.remove(SelectionListener.class, l);
	}

	protected void selectionChanged(final Rectangle2D before,
			final Rectangle2D after, final boolean done) {
		final Graphics2D g = (Graphics2D) getGraphics();
		g.setXORMode(Color.WHITE);
		g.setColor(selectionColor);
		if ((before != null) && (after != null) && (after.getWidth() > 0)
				&& (after.getHeight() > 0) && done) {
			g.fill(before);
			fireSelectionEvent(new SelectionEvent(this, getViewport(), selected));
		} else if (before != null) {
			final Area areaFill = new Area(before);
			areaFill.exclusiveOr(new Area(after));
			g.fill(areaFill);
		} else {
			g.fill(after);
		}
		selected = after;
		g.setColor(getForeground());
		g.setPaintMode();
	}

	public void setSelectionEnabled(final boolean enabled) {
		selectionEnabled = enabled;
	}
}
