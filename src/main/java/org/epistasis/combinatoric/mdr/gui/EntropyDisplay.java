package org.epistasis.combinatoric.mdr.gui;

import java.awt.image.RenderedImage;
import java.util.List;

public interface EntropyDisplay
    {
    public double getDefaultMinimumAbsoluteEntropyPercent();

    public String getEPSText();

    public int getFontSize();

    public RenderedImage getImage();

    public int getLineThickness();

    public double getMinimumAbsoluteEntropyPercent();

    public List<Double> getMinimumAbsoluteEntropyPercentValuesSet();

    public void setFontSize(int fontSize);

    public void setLineThickness(int lineThickness);

    public void setMargin(int pMargin);

    public void setMinimumAbsoluteEntropyPercent(
            double minimumAbsoluteEntropyPercent);

    public boolean supportEntropyThreshold();

    public void updateGraph();
    }
