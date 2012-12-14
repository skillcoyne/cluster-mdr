package org.epistasis.combinatoric.mdr;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.epistasis.ColumnFormat;
import org.epistasis.Utility;
import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class BestModelTextGenerator extends ModelTextGenerator
    {
    private final Dataset data;
    private final Collector.BestModel summary;
    private final int intervals;

    public BestModelTextGenerator(final Dataset data,
                                  final Console.AmbiguousCellStatus tieStatus,
                                  final Console.ScoringMethod scoringMethod,
                                  final Collector.BestModel summary, final int intervals,
                                  final NumberFormat nf, final double pValueTol, final boolean isVerbose)
        {
        super(tieStatus, scoringMethod, nf, pValueTol, isVerbose);
        this.data = data;
        this.summary = summary;
        this.intervals = intervals;
        }

    @Override
    public String toString() {
    final Model model = summary.getModel();
    final StringBuffer b = new StringBuffer();
    b.append(model.getCombo());
    b.append("\n");
    if (intervals > 1)
        {
        b.append("\nCross-validation Statistics:\n\n");
        b.append(getResultText(summary.getAvgTrain(), " Training", 30));
        b.append('\n');
        b.append(getResultText(summary.getAvgTest(), " Testing", 30));
        b.append(ColumnFormat.fitStringWidth("Cross-validation Consistency:", 30));
        b.append(summary.getCVC());
        b.append('/');
        b.append(intervals);
        b.append('\n');
        }
    b.append("\nWhole Dataset Statistics:\n\n");
    b.append(getResultText(summary.getTotal(), "", 30));
    b.append("\nModel Detail:\n\n");
    final List<Integer> widths = new ArrayList<Integer>();
    final List<String> list = new ArrayList<String>();
    list.add("Combination");
    widths.add(25);
    final String statusColumnName = data.getLabels().get(data.getCols() - 1);
    final List<String> statusColumnValues = data.getLevels().get(
            data.getCols() - 1);
    // for (final String name : ) {
    // list.add("Class '" + name + "'");
    // widths.add(15);
    // }
    final String affectedValueString = statusColumnValues.get(data
                                                                      .getAffectedStatus());
    final String unaffectedValueString = statusColumnValues.get(data
                                                                        .getUnaffectedStatus());
    list.add(statusColumnName + " '" + affectedValueString + "'");
    widths.add(list.get(list.size() - 1).length() + 8);
    list.add(statusColumnName + " '" + unaffectedValueString + "'");
    widths.add(list.get(list.size() - 1).length() + 8);
    list.add(affectedValueString + "/" + unaffectedValueString + " Ratio");
    widths.add(list.get(list.size() - 1).length() + 8);
    list.add("Predicted '" + statusColumnName + "'");
    widths.add(list.get(list.size() - 1).length() + 8);
    final ColumnFormat cf = new ColumnFormat(widths);
    b.append(cf.format(list));
    b.append('\n');
    final List<String> underScoreList = new ArrayList<String>(widths.size());
    for (int columnIndex = 0; columnIndex < widths.size(); ++columnIndex)
        {
        underScoreList.add(Utility.chrdup('-', list.get(columnIndex).length()));
        }
    // list.add(Utility.chrdup('-', 11));
    // for (final String name : data.getLevels().get(data.getCols() - 1)) {
    // list.add(Utility.chrdup('-', name.length() + 8));
    // }
    // list.add(Utility.chrdup('-', 15));
    b.append(cf.format(underScoreList));
    b.append('\n');
    final int[] attr = summary.getModel().getCombo().getAttributeIndices();
    for (final Map.Entry<byte[], Model.Cell> cellPair : summary.getModel()
            .getCells().entrySet())
        {
        final StringBuffer b2 = new StringBuffer();
        list.clear();
        final byte[] bytes = cellPair.getKey();
        for (int i = 0; i < bytes.length; ++i)
            {
            if (i != 0)
                {
                b2.append(',');
                }
            final byte attributeLevelIndex = bytes[i];
            String attributeValue = data.getLevels().get(attr[i])
                    .get(attributeLevelIndex);
            if (attributeValue == data.getMissing())
                {
                attributeValue = Main.missingRepresentation;
                }
            b2.append(attributeValue);
            }
        list.add(b2.toString());
        final float[] counts = cellPair.getValue().getCounts();
        final float affectedCount = counts[data.getAffectedStatus()];
        list.add(Main.modelTextNumberFormat.format(affectedCount));
        final float unaffectedCount = counts[data.getUnaffectedStatus()];
        list.add(Main.modelTextNumberFormat.format(unaffectedCount));
        list.add(Main.modelTextNumberFormat.format(affectedCount
                                                   / unaffectedCount));
        final int predictedStatus = cellPair.getValue().getStatus();
        if (predictedStatus == Model.UNKNOWN_STATUS)
            {
            list.add(Main.unknownRepresentation);
            }
        else
            {
            list.add(data.getLevels().get(data.getCols() - 1).get(predictedStatus));
            }
        b.append(cf.format(list));
        b.append('\n');
        }
    return b.toString();
    }
    }
