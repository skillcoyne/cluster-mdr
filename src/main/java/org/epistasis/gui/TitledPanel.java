package org.epistasis.gui;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
public class TitledPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final TitledBorder tboTitle = new TitledBorder("");
	public TitledPanel(final LayoutManager layout) {
		super(layout);
		jbInit();
	}
	public String getTitle() {
		return tboTitle.getTitle();
	}
	public Border getTitleBorder() {
		return tboTitle.getBorder();
	}
	public Color getTitleColor() {
		return tboTitle.getTitleColor();
	}
	public Font getTitleFont() {
		return tboTitle.getTitleFont();
	}
	public int getTitleJustification() {
		return tboTitle.getTitleJustification();
	}
	public int getTitlePosition() {
		return tboTitle.getTitlePosition();
	}
	private void jbInit() {
		setBorder(tboTitle);
		tboTitle.setBorder(BorderFactory.createEtchedBorder());
	}
	public void setTitle(final String title) {
		tboTitle.setTitle(title);
	}
	public void setTitleBorder(final Border border) {
		tboTitle.setBorder(border);
	}
	public void setTitleColor(final Color color) {
		tboTitle.setTitleColor(color);
	}
	public void setTitleFont(final Font font) {
		tboTitle.setTitleFont(font);
	}
	public void setTitleJustification(final int titleJustification) {
		tboTitle.setTitleJustification(titleJustification);
	}
	public void setTitlePosition(final int titlePosition) {
		tboTitle.setTitlePosition(titlePosition);
	}
}
