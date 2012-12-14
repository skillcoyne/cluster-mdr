package org.epistasis.gui;
import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import org.epistasis.combinatoric.mdr.ExpertKnowledge;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
public class DatafilePanel extends JScrollPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Table to hold data values */
	JTable tblDataFile = new JTable();
	// /** Data representation of the table */
	// private final DefaultTableModel dtmDataFile = new ReadOnlyTableModel();
	/** Cell renderer used to right-justify columns */
	// private final DefaultTableCellRenderer dcrDataFile = new DefaultTableCellRenderer();
	public DatafilePanel() {
		jbInit();
	}
	private void jbInit() {
		tblDataFile.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		final JTableHeader jTableHeader = tblDataFile.getTableHeader();
		jTableHeader.setReorderingAllowed(false);
		((DefaultTableCellRenderer) jTableHeader.getDefaultRenderer())
				.setHorizontalAlignment(SwingConstants.CENTER);
		getViewport().add(tblDataFile);
	}
	/**
	 * Display a data set.
	 * @param data Dataset to display
	 */
	public void readDatafile(final Dataset data) {
		tblDataFile.setModel(new AbstractTableModel() {
			private static final long serialVersionUID = 1L;
			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				return Integer.class;
			}
			public int getColumnCount() {
				return data.getCols();
			}
			@Override
			public String getColumnName(final int column) {
				return data.getLabels().get(column);
			}
			public int getRowCount() {
				return data.getRows();
			}
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				return data.getDatum(rowIndex, columnIndex);
			}
			@Override
			public boolean isCellEditable(final int row, final int col) {
				return false;
			}
		});
	}
	public void readDatafile(final ExpertKnowledge expertKnowledge) {
		tblDataFile.setModel(expertKnowledge.getTableModel());
	}
	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		if (tblDataFile == null) {
			return;
		}
		tblDataFile.setFont(font);
		tblDataFile.getTableHeader().setFont(font);
	}
}
