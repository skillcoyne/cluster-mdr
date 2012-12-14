package org.epistasis.combinatoric.mdr;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class IfThenRulesTextGenerator
    {
    private final Dataset data;
    private final Collector.BestModel summary;

    public IfThenRulesTextGenerator(final Dataset data,
                                    final Collector.BestModel summary)
        {
        this.summary = summary;
        this.data = data;
        }

    @Override
    public String toString() {
    final StringWriter s = new StringWriter();
    final PrintWriter w = new PrintWriter(s);
    final int[] attr = summary.getModel().getCombo().getAttributeIndices();
    for (final Map.Entry<byte[], Model.Cell> cell : summary.getModel()
            .getCells().entrySet())
        {
        w.print("IF ");
        final byte[] bytes = cell.getKey();
        for (int i = 0; i < bytes.length; ++i)
            {
            if (i != 0)
                {
                w.print(" AND ");
                }
            w.print(data.getLabels().get(attr[i]));
            w.print(" = ");
            final byte attributeLevelIndex = bytes[i];
            final String attributeValue = data.getLevels().get(attr[i])
                    .get(attributeLevelIndex);
            if (attributeValue == data.getMissing())
                {
                w.print(Main.missingRepresentation);
                }
            w.print(attributeValue);
            }
        w.print(" THEN CLASSIFY AS ");
        final int predictedStatus = cell.getValue().getStatus();
        if (predictedStatus == Model.UNKNOWN_STATUS)
            {
            w.print(Main.unknownRepresentation);
            }
        else
            {
            w.print(data.getLevels().get(data.getCols() - 1).get(predictedStatus));
            }
        w.print('.');
        w.println();
        }
    return s.toString();
    }
    }
