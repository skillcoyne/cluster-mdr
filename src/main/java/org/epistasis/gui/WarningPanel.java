package org.epistasis.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.epistasis.StringListener;

public class WarningPanel extends JPanel implements StringListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String defaultTitle = "Warnings";
	private JTabbedPane tabPane = null;
	private Component preWarn = null;
	private String title = WarningPanel.defaultTitle;
	private final BorderLayout bolWanning = new BorderLayout();
	private final JPanel pnlButtons = new JPanel();
	private final GridBagLayout gblButtons = new GridBagLayout();
	private final JButton cmdClear = new JButton();
	private final JScrollPane scpWarning = new JScrollPane();
	private final JList lstWarning = new JList();
	private final DefaultListModel dlmWarning = new DefaultListModel();
	private final Runnable clearWarning = new ClearWarning();

	public WarningPanel() {
		this(null, WarningPanel.defaultTitle);
	}

	public WarningPanel(final JTabbedPane parent) {
		this(parent, WarningPanel.defaultTitle);
	}

	public WarningPanel(final JTabbedPane tabPane, final String title) {
		this.tabPane = tabPane;
		this.title = title;
		jbInit();
	}

	public WarningPanel(final String title) {
		this(null, title);
	}

	public void clear() {
		SwingUtilities.invokeLater(clearWarning);
	}

	public JTabbedPane getTabPane() {
		return tabPane;
	}

	public String getTitle() {
		return title;
	}

	private void jbInit() {
		setLayout(bolWanning);
		pnlButtons.setLayout(gblButtons);
		cmdClear.setText("Clear");
		cmdClear.addActionListener(new ClearAction());
		scpWarning.setBorder(BorderFactory.createLoweredBevelBorder());
		lstWarning.setForeground(Color.red);
		lstWarning.setModel(dlmWarning);
		lstWarning.setSelectionForeground(Color.red);
		setTitle("Warning");
		this.add(pnlButtons, java.awt.BorderLayout.SOUTH);
		pnlButtons.add(cmdClear, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0));
		this.add(scpWarning, java.awt.BorderLayout.CENTER);
		scpWarning.getViewport().add(lstWarning);
	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		if (lstWarning != null) {
			lstWarning.setFont(font);
		}
		if (cmdClear != null) {
			cmdClear.setFont(font);
		}
	}

	public void setTabPane(final JTabbedPane tabPane) {
		if (tabPane == this.tabPane) {
			return;
		}
		final boolean showing = (this.tabPane != null)
				&& (this.tabPane == getParent());
		if (showing) {
			this.tabPane.remove(this);
		}
		this.tabPane = tabPane;
		if ((tabPane != null) && showing) {
			tabPane.add(this);
		}
	}

	public void setTitle(final String title) {
		if (this.title == title) {
			return;
		}
		if ((this.title != null) && (title != null) && this.title.equals(title)) {
			return;
		}
		this.title = title;
		if ((tabPane != null) && (tabPane == getParent())) {
			tabPane.remove(this);
			tabPane.add(this, title);
		}
	}

	public void stringReceived(final String s) {
		warn(s);
	}

	public void warn(final String s) {
		SwingUtilities.invokeLater(new WarnString(s));
	}

	private class ClearAction implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			clear();
		}
	}

	private class ClearWarning implements Runnable {
		public void run() {
			tabPane.remove(WarningPanel.this);
			if (preWarn != null) {
				tabPane.setSelectedComponent(preWarn);
			}
			dlmWarning.clear();
		}
	}

	private class WarnString implements Runnable {
		private final String warning;

		public WarnString(final String warning) {
			this.warning = warning;
		}

		public void run() {
			if (getParent() != tabPane) {
				preWarn = tabPane.getSelectedComponent();
				tabPane.add(WarningPanel.this, title);
				tabPane.setSelectedComponent(WarningPanel.this);
			}
			dlmWarning.addElement(warning);
		}
	}
}
