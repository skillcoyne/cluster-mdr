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
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.epistasis.FileSaver;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class AttributeConstructionPanel extends JComponent
    {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final GridBagLayout gblThis = new GridBagLayout();
    private final JButton cmdConstruct = new JButton();
    private final JButton cmdDelete = new JButton();
    private final JScrollPane scpAttributes = new JScrollPane();
    private final JButton cmdExport = new JButton();
    private final JLabel lblName = new JLabel();
    private final JTextField txtName = new JTextField();
    private final JList lstAttributes = new JList();
    private final JButton cmdSort = new JButton();
    private final JLabel lblAttributes = new JLabel();
    private Dataset data;
    private final EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent;
    private boolean warnOnChange = false;

    public AttributeConstructionPanel()
        {
        jbInit();
        }

    public void addChangeListener(final ChangeListener l)
        {
        listenerList.add(ChangeListener.class, l);
        }

    public void cmdConstruct_actionPerformed(final ActionEvent e)
        {
        final DefaultListModel model = (DefaultListModel) lstAttributes.getModel();
        final String name = txtName.getText();
        if (model.contains(name))
            {
            JOptionPane.showMessageDialog(this, "Attribute '" + name
                                                + "' already exists.", "Duplicate Attribute",
                                          JOptionPane.ERROR_MESSAGE);
            return;
            }
        if (warnOnChange)
            {
            if (!warn("Constructing an attribute will clear the current "
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
        final AttributeCombination combo = new AttributeCombination(indices,
                                                                    data.getLabels());
        final Model mdr = new Model(combo, Frame.getFrame().getTiePriority());
        mdr.buildCounts(data);
        mdr.buildStatuses(data, data.getStatusCounts());
        final List<String> attribute = mdr.constructAttribute(data);
        data.insertColumn(data.getCols() - 1, name, attribute);
        model.addElement(name);
        fireChangeEvent();
        }

    public void cmdDelete_actionPerformed(final ActionEvent e)
        {
        if (JOptionPane.showConfirmDialog(this,
                                          "Really delete selected attribute(s)?", "Delete Attribute(s)",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
            if (warnOnChange)
                {
                if (!warn("Deleting an attribute will clear the current analysis pane.  Continue?"))
                    {
                    return;
                    }
                warnOnChange = false;
                }
            final Object[] attrs = lstAttributes.getSelectedValues();
            for (int i = 0; i < attrs.length; ++i)
                {
                data.removeColumn(data.getLabels().indexOf(attrs[i].toString()));
                ((DefaultListModel) lstAttributes.getModel()).removeElement(attrs[i]);
                }
            fireChangeEvent();
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
        cmdConstruct.setText("Construct");
        cmdConstruct.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdConstruct_actionPerformed(e);
            }
        });
        cmdConstruct.setEnabled(false);
        cmdDelete.setText("Delete");
        cmdDelete.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdDelete_actionPerformed(e);
            }
        });
        cmdDelete.setEnabled(false);
        cmdExport.setEnabled(false);
        cmdExport.setText("Export Data");
        cmdExport.addActionListener(new ActionListener()
        {
        public void actionPerformed(final ActionEvent e)
            {
            cmdExport_actionPerformed(e);
            }
        });
        scpAttributes.setBorder(BorderFactory.createLoweredBevelBorder());
        scpAttributes.setMinimumSize(new Dimension(200, 0));
        scpAttributes.setPreferredSize(new Dimension(200, 0));
        lblName.setToolTipText("");
        lblName.setText("Constructed Attribute Name:");
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
        this.add(cmdConstruct, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                      GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5,
                                                                                                                           5, 0, 0), 0, 0));
        this.add(cmdDelete, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
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
        final StringBuffer name = new StringBuffer();
        final Object[] values = lstAttributes.getSelectedValues();
        for (int i = 0; i < values.length; ++i)
            {
            if (i != 0)
                {
                name.append('_');
                }
            name.append(values[i]);
            }
        txtName.setText(name.toString());
        txtName.selectAll();
        txtName.requestFocus();
        cmdConstruct.setEnabled(values.length > 1);
        cmdDelete.setEnabled((values.length > 0)
                             && (values.length < lstAttributes.getModel().getSize()));
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
        cmdConstruct.setEnabled((selectedCount > 1)
                                && (txtName.getText().length() > 0));
        cmdDelete.setEnabled(selectedCount > 0);
        cmdSort.setEnabled(lstAttributes.getModel().getSize() > 1);
        cmdExport.setEnabled(data != null);
        }
    else
        {
        lstAttributes.clearSelection();
        cmdConstruct.setEnabled(enabled);
        cmdDelete.setEnabled(enabled);
        cmdSort.setEnabled(enabled);
        cmdExport.setEnabled(enabled);
        }
    lstAttributes.setEnabled(enabled);
    lblName.setEnabled(enabled);
    txtName.setEnabled(enabled);
    super.setEnabled(enabled);
    }

    @Override
    public void setFont(final Font font) {
    super.setFont(font);
    if (cmdConstruct != null)
        {
        cmdConstruct.setFont(font);
        cmdDelete.setFont(font);
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
        cmdConstruct.setEnabled((txtName.getText().length() > 0)
                                && (lstAttributes.getSelectedValues().length > 1));
        }

    private boolean warn(final String warning)
        {
        return JOptionPane.showConfirmDialog(this, warning, "Warning",
                                             JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
    }

class AttributeConstructionPanel_cmdConstruct_actionAdapter implements
                                                            ActionListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_cmdConstruct_actionAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdConstruct_actionPerformed(e);
        }
    }

class AttributeConstructionPanel_cmdDelete_actionAdapter implements
                                                         ActionListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_cmdDelete_actionAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdDelete_actionPerformed(e);
        }
    }

class AttributeConstructionPanel_cmdExport_actionAdapter implements
                                                         ActionListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_cmdExport_actionAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdExport_actionPerformed(e);
        }
    }

class AttributeConstructionPanel_cmdSort_actionAdapter implements
                                                       ActionListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_cmdSort_actionAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdSort_actionPerformed(e);
        }
    }

class AttributeConstructionPanel_lstAttributes_listSelectionAdapter implements
                                                                    ListSelectionListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_lstAttributes_listSelectionAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void valueChanged(final ListSelectionEvent e)
        {
        adaptee.lstAttributes_selectionChanged(e);
        }
    }

class AttributeConstructionPanel_txtName_documentAdapter implements
                                                         DocumentListener
    {
    private final AttributeConstructionPanel adaptee;

    AttributeConstructionPanel_txtName_documentAdapter(
            final AttributeConstructionPanel adaptee)
        {
        this.adaptee = adaptee;
        }

    public void changedUpdate(final DocumentEvent e)
        {
        adaptee.txtName_textChanged(e);
        }

    public void insertUpdate(final DocumentEvent e)
        {
        adaptee.txtName_textChanged(e);
        }

    public void removeUpdate(final DocumentEvent e)
        {
        adaptee.txtName_textChanged(e);
        }
    }
