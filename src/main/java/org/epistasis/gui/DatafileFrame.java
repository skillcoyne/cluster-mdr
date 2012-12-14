package org.epistasis.gui;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import org.epistasis.combinatoric.mdr.ExpertKnowledge;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
/**
 * Frame that displays a dataset in a tabular format.
 */
public class DatafileFrame extends CenterFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Frame that created this frame */
	private final Frame owner = null;
	private final DatafilePanel pnlDatafile = new DatafilePanel();
	private final BorderLayout bolThis = new BorderLayout();
	/**
	 * Construct a DatafileFrame centered over its owner.
	 * @param owner Frame over which to center
	 */
	public DatafileFrame(final Frame owner) {
		jbInit();
	}
	private void jbInit() {
		this.setSize(512, 384);
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(bolThis);
		getContentPane().add(pnlDatafile, java.awt.BorderLayout.CENTER);
	}
	public void readDatafile(final Dataset data) {
		setTitle("Current Datafile");
		pnlDatafile.readDatafile(data);
	}
	public void readDatafile(final ExpertKnowledge expertKnowledge) {
		setTitle("Current Scores");
		pnlDatafile.readDatafile(expertKnowledge);
	}
	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		if (pnlDatafile == null) {
			return;
		}
		pnlDatafile.setFont(font);
	}
	@Override
	public void setVisible(final boolean visible) {
		if (isVisible() == visible) {
			return;
		}
		if (visible && (owner != null)) {
			center(owner.getBounds());
		}
		super.setVisible(visible);
	}
}
