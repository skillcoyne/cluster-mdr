package org.epistasis.combinatoric.mdr.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.epistasis.FileSaver;
import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class CovariateAdjustmentPanel extends JComponent
    {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final GridBagLayout gblThis = new GridBagLayout();
    private final JButton cmdAdjustCovariate = new JButton();
    private final JButton cmdRevert = new JButton();
    private final JScrollPane scpAttributes = new JScrollPane();
    private final JButton cmdExport = new JButton();
    private final JLabel lblName = new JLabel();
    private final JTextField txtName = new JTextField();
    private final JList lstAttributes = new JList();
    private final JButton cmdSort = new JButton();
    private final JLabel lblAttributes = new JLabel();
    private Dataset data;
    private Dataset previousDataset = null;
    private final EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent;
    private boolean warnOnChange = false;

    public CovariateAdjustmentPanel()
        {
        jbInit();
        }

    public void addChangeListener(final ChangeListener l)
        {
        listenerList.add(ChangeListener.class, l);
        }

    private boolean canDatasetBeReverted()
        {
        return previousDataset != null;
        }

    public void cmdAdjustCovariate_actionPerformed(final ActionEvent e)
        {
        final String covariateAttributeName = txtName.getText();
        if (warnOnChange)
            {
            if (!warn("Adjusting for covariate attribute will clear the current "
                      + "analysis pane.  Continue?"))
                {
                return;
                }
            warnOnChange = false;
            }
        final Object[] labels = lstAttributes.getSelectedValues();
        final List<Integer> indices = new ArrayList<Integer>(labels.length);
        for (final Object label : labels)
            {
            indices.add(data.getLabels().indexOf(label.toString()));
            }
        Dataset adjustedDataset = null;
        try
            {
            adjustedDataset = data.adjustForCovariate(Frame.getFrame()
                                                              .getRandomSeed(), covariateAttributeName);
            previousDataset = data;
            cmdRevert.setEnabled(canDatasetBeReverted());
            // setData(adjustedDataset);
            data = adjustedDataset;
            fireChangeEvent();
            }
        catch (final Exception ex)
            {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                                          "Covariate adjustment failed", JOptionPane.ERROR_MESSAGE);
            }
        }

    public void cmdExport_actionPerformed(final ActionEvent e)
        {
        final PrintWriter p = FileSaver.openFileWriter(this, "Export Dataset",
                                                       FileSaver.fltText);
        if (p != null)
            {
            data.write(p);
            p.flush();
            }
        }

    public void cmdRevert_actionPerformed(final ActionEvent e)
        {
        if (JOptionPane.showConfirmDialog(this,
                                          "Really revert data set and undo covariate adjustment?",
                                          "Revert dataset", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
            if (warnOnChange)
                {
                if (!warn("Reverting the dataset will clear the current analysis pane.  Continue?"))
                    {
                    return;
                    }
                warnOnChange = false;
                }
            data = previousDataset;
            previousDataset = null;
            cmdRevert.setEnabled(canDatasetBeReverted());
            fireChangeEvent();
            }
        }

    public void cmdSort_actionPerformed(final ActionEvent e)
        {
        final DefaultListModel model = (DefaultListModel) lstAttributes.getModel();
        final String[] values = new String[model.getSize()];
        for (int i = 0; i < values.length; ++i)
            {
            values[i] = model.get(i).toString();
            }
        Arrays.sort(values, String.CASE_INSENSITIVE_ORDER);
        model.clear();
        model.ensureCapacity(values.length);
        for (int i = 0; i < values.length; ++i)
            {
            model.addElement(values[i]);
            }
        }

    protected void fireChangeEvent()
        {
        final EventListener[] listeners = listenerList
                .getListeners(ChangeListener.class);
        cmdSort.setEnabled((lstAttributes.getModel().getSize() > 1) && isEnabled());
        for (final EventListener element : listeners)
            {
            if (changeEvent == null)
                {
                changeEvent = new ChangeEvent(this);
                }
            ((ChangeListener) element).stateChanged(changeEvent);
            }
        }

    public Dataset getData()
        {
        return data;
        }

    public boolean isWarnOnChange()
        {
        return warnOnChange;
        }

    private void jbInit()
        {
        setBackground(UIManager.getColor("control"));
        setLayout(gblThis);
        cmdAdjustCovariate.setText("Adjust for covariate");
        cmdAdjustCovariate.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdAdjustCovariate_actionPerformed(e);
            }
        });
        cmdAdjustCovariate.setEnabled(false);
        cmdRevert.setText("Discard adjusted dataset and revert to previous");
        cmdRevert.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdRevert_actionPerformed(e);
            }
        });
        cmdRevert.setEnabled(canDatasetBeReverted());
        cmdExport.setEnabled(false);
        cmdExport.setText("Export Data");
        cmdExport.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdExport_actionPerformed(e);
            }
        });
        cmdRevert.setVisible(true);
        scpAttributes.setBorder(BorderFactory.createLoweredBevelBorder());
        scpAttributes.setMinimumSize(new Dimension(200, 0));
        scpAttributes.setPreferredSize(new Dimension(200, 0));
        lblName.setToolTipText("");
        lblName.setText("Covariate attribute to adjust for:");
        txtName.getDocument().addDocumentListener(new DocumentListener()
        {
        public void changedUpdate(final DocumentEvent e)
            {
            txtName_textChanged(e);
            }

        public void insertUpdate(final DocumentEvent e)
            {
            txtName_textChanged(e);
            }

        public void removeUpdate(final DocumentEvent e)
            {
            txtName_textChanged(e);
            }
        });
        cmdSort.setEnabled(false);
        cmdSort.setText("Sort List");
        cmdSort.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdSort_actionPerformed(e);
            }
        });
        lstAttributes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lblAttributes.setText("Attributes:");
        lstAttributes.getSelectionModel().addListSelectionListener(
                new ListSelectionListener()
                {
                public void valueChanged(final ListSelectionEvent e)
                    {
                    lstAttributes_selectionChanged(e);
                    }
                });
        this.add(lblAttributes, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                       GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                       new Insets(5, 5, 0, 0), 0, 0));
        this.add(scpAttributes, new GridBagConstraints(0, 1, 1, 6, 0.0, 1.0,
                                                       GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                       new Insets(5, 5, 5,
















                                                                                                                      0), 0, 0));
        this.add(lblName, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0,
                                                 GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                 new Insets(5, 5, 0, 0), 0, 0));
        this.add(txtName, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0,
                                                 GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                      5, 0, 5), 0, 0));
        this.add(cmdAdjustCovariate, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                                 5, 0, 0), 0, 0));
        this.add(cmdRevert, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                                   GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                        5, 0, 0), 0, 0));
        this.add(cmdSort, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                                 GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                      5, 0, 0), 0, 0));
        this.add(cmdExport, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                                   GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                        5, 0, 0), 0, 0));
        scpAttributes.getViewport().add(lstAttributes);
        }

    public void lstAttributes_selectionChanged(final ListSelectionEvent e)
        {
        final Object selectedName = lstAttributes.getSelectedValue();
        txtName.setText((selectedName != null) ? selectedName.toString() : "");
        txtName.selectAll();
        txtName.requestFocus();
        }

    @Override
    public void paint(final Graphics g) {
    final Color c = g.getColor();
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    g.setColor(c);
    super.paint(g);
    }

    public void removeChangeListener(final ChangeListener l)
        {
        listenerList.remove(ChangeListener.class, l);
        }

    public void setData(final Dataset data)
        {
        if ((this.data == data)
            && (lstAttributes.getModel().getSize() == data.getRows()))
            {
            return;
            }
        this.data = data;
        cmdExport.setEnabled((data != null) && isEnabled());
        final DefaultListModel model = new DefaultListModel();
        if (data != null)
            {
            model.ensureCapacity(data.getCols() - 1);
            for (int i = 0; i < data.getLabels().size() - 1; ++i)
                {
                model.addElement(data.getLabels().get(i));
                }
            }
        lstAttributes.setModel(model);
        cmdSort.setEnabled((lstAttributes.getModel().getSize() > 1) && isEnabled());
        }

    @Override
    public void setEnabled(final boolean enabled) {
    if (enabled)
        {
        final int selectedCount = lstAttributes.getSelectedValues().length;
        cmdAdjustCovariate.setEnabled((selectedCount > 1)
                                      && (txtName.getText().length() > 0));
        cmdRevert.setEnabled(canDatasetBeReverted());
        cmdSort.setEnabled(lstAttributes.getModel().getSize() > 1);
        cmdExport.setEnabled(data != null);
        }
    else
        {
        lstAttributes.clearSelection();
        cmdAdjustCovariate.setEnabled(enabled);
        cmdRevert.setEnabled(enabled);
        cmdSort.setEnabled(enabled);
        cmdExport.setEnabled(enabled);
        }
    lstAttributes.setEnabled(enabled);
    lblName.setEnabled(enabled);
    txtName.setEnabled(enabled);
    super.setEnabled(enabled);
    }

    /**
     * PCA don't believe that font setting is necessary since all components inherit from parent if font not set
     */
    @Override
    public void setFont(final Font font) {
    super.setFont(font);
    if (cmdAdjustCovariate != null)
        {
        cmdAdjustCovariate.setFont(font);
        cmdRevert.setFont(font);
        cmdExport.setFont(font);
        cmdSort.setFont(font);
        lblName.setFont(font);
        lblAttributes.setFont(font);
        lstAttributes.setFont(font);
        }
    }

    public void setWarnOnChange(final boolean warnOnChange)
        {
        this.warnOnChange = warnOnChange;
        }

    public void txtName_textChanged(final DocumentEvent e)
        {
        final String attributeName = txtName.getText();
        final DefaultListModel model = (DefaultListModel) lstAttributes.getModel();
        // if current contents is an attribute then enable button
        cmdAdjustCovariate.setEnabled(model.contains(attributeName));
        }

    private boolean warn(final String warning)
        {
        return JOptionPane.showConfirmDialog(this, warning, "Warning",
                                             JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
    }
