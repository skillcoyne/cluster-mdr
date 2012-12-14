package org.epistasis.combinatoric.mdr.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.epistasis.FileSaver;
import org.epistasis.Utility;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class GraphicalModelControls extends JPanel
    {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final EventListenerList listenerList = new EventListenerList();
    private final BorderLayout bolThis = new BorderLayout();
    private final JScrollPane scpGraphicalModel = new JScrollPane();
    private final JPanel pnlControls = new JPanel();
    private final GridBagLayout gblControls = new GridBagLayout();
    private final JLabel lblPage = new JLabel();
    private final JCheckBox chkLimitDimension = new JCheckBox();
    private final JSpinner spnLimitDimension = new JSpinner();
    private final JButton cmdPrevious = new JButton();
    private final JButton cmdNext = new JButton();
    private final JButton cmdSave = new JButton();
    private final GraphicalModelPanel gmpGraphicalModel = new GraphicalModelPanel();
    private final JButton cmdMaximize = new JButton();

    public GraphicalModelControls()
        {
        jbInit();
        }

    public void addActionListener(final ActionListener l)
        {
        listenerList.add(ActionListener.class, l);
        }

    private void checkEnabled()
        {
        lblPage.setEnabled(isEnabled());
        chkLimitDimension.setEnabled(isEnabled());
        gmpGraphicalModel.setEnabled(isEnabled());
        if (isEnabled())
            {
            spnLimitDimension.setEnabled(chkLimitDimension.isSelected());
            cmdPrevious.setEnabled(gmpGraphicalModel.getPage() > 0);
            cmdNext.setEnabled((gmpGraphicalModel.getPage() + 1) < gmpGraphicalModel
                    .getNumPages());
            cmdSave.setEnabled(gmpGraphicalModel.getNumPages() > 0);
            }
        else
            {
            spnLimitDimension.setEnabled(false);
            cmdPrevious.setEnabled(false);
            cmdNext.setEnabled(false);
            cmdSave.setEnabled(false);
            }
        cmdMaximize.setEnabled(isEnabled());
        }

    public void chkLimitDimension_actionPerformed(final ActionEvent e)
        {
        if (chkLimitDimension.isSelected())
            {
            gmpGraphicalModel.setMaxDim(((Number) spnLimitDimension.getValue())
                                                .intValue());
            }
        else
            {
            gmpGraphicalModel.setMaxDim(0);
            }
        updateLabel();
        checkEnabled();
        }

    public void cmdMaximize_actionPerformed(final ActionEvent e)
        {
        if (cmdMaximize.getText().equals("Maximize"))
            {
            cmdMaximize.setText("Restore");
            }
        else
            {
            cmdMaximize.setText("Maximize");
            }
        fireActionEvent(e);
        }

    public void cmdNext_actionPerformed(final ActionEvent e)
        {
        gmpGraphicalModel.setPage(gmpGraphicalModel.getPage() + 1);
        updateLabel();
        checkEnabled();
        }

    public void cmdPrevious_actionPerformed(final ActionEvent e)
        {
        gmpGraphicalModel.setPage(gmpGraphicalModel.getPage() - 1);
        updateLabel();
        checkEnabled();
        }

    public void cmdSave_actionPerformed(final ActionEvent e)
        {
        final NumberFormat nf = new DecimalFormat("0000");
        final JFileChooser fc = new JFileChooser();
        final int pages = gmpGraphicalModel.getNumPages();
        if (pages > 1)
            {
            fc.setDialogTitle("Save " + pages + " Pages");
            }
        else
            {
            fc.setDialogTitle("Save 1 Page");
            }
        fc.setMultiSelectionEnabled(false);
        fc.setCurrentDirectory(FileSaver.getSaveFolder());
        fc.addChoosableFileFilter(FileSaver.jpgFilter);
        fc.addChoosableFileFilter(FileSaver.pngFilter);
        fc.addChoosableFileFilter(FileSaver.epsFilter);
        fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
        try
            {
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
                {
                final File f = fc.getSelectedFile();
                String name = f.getName();
                int pos = name.lastIndexOf('.');
                if (fc.getFileFilter() instanceof FileSaver.ExtensionFilter)
                    {
                    final FileSaver.ExtensionFilter extFilter = (FileSaver.ExtensionFilter) fc
                            .getFileFilter();
                    if (pos < 0)
                        {
                        name += '.' + extFilter.getExtension();
                        pos = name.lastIndexOf('.');
                        }
                    }
                for (int i = 0; i < pages; ++i)
                    {
                    String filename;
                    if (pages > 1)
                        {
                        filename = f.getParent() + File.separator + name.substring(0, pos)
                                   + nf.format(i + 1) + name.substring(pos);
                        }
                    else
                        {
                        filename = f.getParent() + File.separator + name;
                        }
                    if (fc.getFileFilter() == FileSaver.epsFilter)
                        {
                        final BufferedWriter w = new BufferedWriter(
                                new FileWriter(filename));
                        w.write(gmpGraphicalModel.getPageEPS(i));
                        w.flush();
                        w.close();
                        }
                    else if (fc.getFileFilter() == FileSaver.jpgFilter)
                        {
                        ImageIO.write(gmpGraphicalModel.getPageImage(i), "jpeg", new File(
                                filename));
                        }
                    else if (fc.getFileFilter() == FileSaver.pngFilter)
                        {
                        ImageIO.write(gmpGraphicalModel.getPageImage(i), "png", new File(
                                filename));
                        }
                    }
                FileSaver.setSaveFolder(f.getParentFile());
                }
            }
        catch (final IOException ex)
            {
            Utility.logException(ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            }
        }

    protected void fireActionEvent(final ActionEvent e)
        {
        final ActionListener[] listeners = listenerList
                .getListeners(ActionListener.class);
        for (int i = 0; i < listeners.length; ++i)
            {
            listeners[i].actionPerformed(e);
            }
        }

    private void jbInit()
        {
        setLayout(bolThis);
        scpGraphicalModel.setBorder(BorderFactory.createLoweredBevelBorder());
        gmpGraphicalModel.setCellFont(new java.awt.Font("Dialog", Font.BOLD, 14));
        gmpGraphicalModel.setValueFont(new java.awt.Font("Dialog", Font.PLAIN, 14));
        gmpGraphicalModel.setAxisFont(new java.awt.Font("Dialog", Font.BOLD, 16));
        pnlControls.setLayout(gblControls);
        lblPage.setText("");
        chkLimitDimension.setSelected(true);
        chkLimitDimension.setText("Limit Dimension");
        chkLimitDimension
                .addActionListener(new GraphicalModelControls_chkLimitDimension_actionAdapter(
                        this));
        cmdPrevious.setText("< Previous");
        cmdPrevious
                .addActionListener(new GraphicalModelControls_cmdPrevious_actionAdapter(
                        this));
        cmdNext.setText("Next >");
        cmdNext.addActionListener(new GraphicalModelControls_cmdNext_actionAdapter(
                this));
        cmdSave.setText("Save");
        cmdSave.addActionListener(new GraphicalModelControls_cmdSave_actionAdapter(
                this));
        spnLimitDimension.setMinimumSize(new Dimension(60, 20));
        spnLimitDimension.setPreferredSize(new Dimension(60, 20));
        spnLimitDimension
                .addChangeListener(new GraphicalModelControls_spnLimitDimension_changeAdapter(
                        this));
        spnLimitDimension.setModel(new SpinnerNumberModel(new Integer(3),
                                                          new Integer(1), null, new Integer(1)));
        cmdMaximize.setText("Maximize");
        cmdMaximize
                .addActionListener(new GraphicalModelControls_cmdMaximize_actionAdapter(
                        this));
        this.add(scpGraphicalModel, java.awt.BorderLayout.CENTER);
        scpGraphicalModel.getViewport().add(gmpGraphicalModel);
        this.add(pnlControls, java.awt.BorderLayout.SOUTH);
        pnlControls.add(lblPage, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                        new Insets(5, 5, 5,


















                                                                                                                       0), 0, 0));
        pnlControls.add(chkLimitDimension, new GridBagConstraints(2, 0, 1, 1, 1.0,
                                                                  0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5,
                                                                                                                                    5, 0), 0, 0));
        pnlControls.add(spnLimitDimension, new GridBagConstraints(3, 0, 1, 1, 0.0,
                                                                  0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5,
                                                                                                                                      5, 5, 0), 0, 0));
        pnlControls.add(cmdPrevious, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                                                            GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
                                                                                                                           0), 0, 0));
        pnlControls.add(cmdNext, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
                                                                                                                       0), 0, 0));
        pnlControls.add(cmdMaximize, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
                                                            GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
                                                                                                                           0), 0, 0));
        pnlControls.add(cmdSave, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
                                                                                                                       5), 0, 0));
        }

    public void removeActionListener(final ActionListener l)
        {
        listenerList.remove(ActionListener.class, l);
        }

    public void setData(final Dataset data)
        {
        gmpGraphicalModel.setLabels(data.getLabels());
        gmpGraphicalModel.setLevels(data.getLevels());
        }

    @Override
    public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    if (lblPage != null)
        {
        checkEnabled();
        }
    }

    @Override
    public void setFont(final Font font) {
    super.setFont(font);
    if (lblPage == null)
        {
        return;
        }
    lblPage.setFont(font);
    chkLimitDimension.setFont(font);
    spnLimitDimension.setFont(font);
    cmdPrevious.setFont(font);
    cmdNext.setFont(font);
    cmdSave.setFont(font);
    cmdMaximize.setFont(font);
    }

    public void setModel(final Model model)
        {
        gmpGraphicalModel.setModel(model);
        updateLabel();
        checkEnabled();
        }

    public void spnLimitDimension_stateChanged(final ChangeEvent e)
        {
        gmpGraphicalModel.setMaxDim(((Number) spnLimitDimension.getValue())
                                            .intValue());
        updateLabel();
        checkEnabled();
        }

    private void updateLabel()
        {
        if (gmpGraphicalModel.getModel() == null)
            {
            lblPage.setText("");
            }
        else
            {
            lblPage.setText("Page " + (gmpGraphicalModel.getPage() + 1) + " of "
                            + gmpGraphicalModel.getNumPages());
            }
        }
    }

class GraphicalModelControls_chkLimitDimension_actionAdapter implements
                                                             ActionListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_chkLimitDimension_actionAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.chkLimitDimension_actionPerformed(e);
        }
    }

class GraphicalModelControls_cmdMaximize_actionAdapter implements
                                                       ActionListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_cmdMaximize_actionAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdMaximize_actionPerformed(e);
        }
    }

class GraphicalModelControls_cmdNext_actionAdapter implements ActionListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_cmdNext_actionAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdNext_actionPerformed(e);
        }
    }

class GraphicalModelControls_cmdPrevious_actionAdapter implements
                                                       ActionListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_cmdPrevious_actionAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdPrevious_actionPerformed(e);
        }
    }

class GraphicalModelControls_cmdSave_actionAdapter implements ActionListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_cmdSave_actionAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void actionPerformed(final ActionEvent e)
        {
        adaptee.cmdSave_actionPerformed(e);
        }
    }

class GraphicalModelControls_spnLimitDimension_changeAdapter implements
                                                             ChangeListener
    {
    private final GraphicalModelControls adaptee;

    GraphicalModelControls_spnLimitDimension_changeAdapter(
            final GraphicalModelControls adaptee)
        {
        this.adaptee = adaptee;
        }

    public void stateChanged(final ChangeEvent e)
        {
        adaptee.spnLimitDimension_stateChanged(e);
        }
    }
