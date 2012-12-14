package org.epistasis.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JTabbedPane;

import org.epistasis.DisplayPair;
import org.epistasis.Pair;

public class OrderedTabbedPane extends JTabbedPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<Pair<String, Component>> order = new ArrayList<Pair<String, Component>>();

	public void addOrderedTab(final Component c, final String name) {
		removeOrderedTab(c);
		order.add(new DisplayPair<String, Component>(name, c));
		add(c, name);
	}

	public boolean isOrderedTabVisible(final Component c) {
		final Component[] comps = getComponents();
		for (int i = 0; i < comps.length; ++i) {
			if (comps[i] == c) {
				return true;
			}
		}
		return false;
	}

	public void removeOrderedTab(final Component c) {
		for (final ListIterator<Pair<String, Component>> i = order.listIterator(); i
				.hasNext();) {
			final Pair<String, Component> p = i.next();
			if (c == p.getSecond()) {
				i.remove();
				remove(c);
				break;
			}
		}
	}

	public void setOrderedTabVisible(final Component c, final boolean visible) {
		if (visible) {
			int index = 0;
			String name = "";
			for (final Pair<String, Component> p : order) {
				if (p.getSecond() == c) {
					name = p.toString();
					if (!isOrderedTabVisible(p.getSecond())) {
						add(c, name, index);
					}
					break;
				}
				if (isOrderedTabVisible(p.getSecond())) {
					++index;
				}
			}
		} else {
			remove(c);
		}
	}
}
