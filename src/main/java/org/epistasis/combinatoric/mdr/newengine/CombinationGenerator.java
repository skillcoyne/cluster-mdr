package org.epistasis.combinatoric.mdr.newengine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombinationGenerator implements Iterator<AttributeCombination>,
                                             Iterable<AttributeCombination>
    {
    private final int nVars;
    private boolean hasNext;
    private final List<Integer> combo;
    private final List<String> labels;

    public CombinationGenerator(final int nVars, final int nCombo,
                                final List<String> labels)
        {
        this.labels = labels;
        if (nCombo < 0)
            {
            throw new IllegalArgumentException("nCombo must be non-negative");
            }
        if (nVars < nCombo)
            {
            throw new IllegalArgumentException("nCombo cannot be larger than nVars");
            }
        this.nVars = nVars;
        combo = new ArrayList<Integer>(nCombo);
        hasNext = true;
        combo.clear();
        for (int i = 0; i < nCombo; ++i)
            {
            combo.add(i);
            }
        }

    public boolean hasNext()
        {
        return hasNext;
        }

    private boolean increment(final int index)
        {
        boolean didIncrement = false;
        if (!combo.isEmpty())
            {
            combo.set(combo.size() - 1 - index,
                      combo.get(combo.size() - 1 - index) + 1);
            if (combo.get(combo.size() - 1 - index) < nVars - index)
                {
                for (int i = combo.size() - index; i < combo.size(); ++i)
                    {
                    combo.set(i, combo.get(i - 1) + 1);
                    }
                didIncrement = true;
                }
            else if (index + 1 < combo.size())
                {
                return increment(index + 1);
                }
            }
        return didIncrement;
        }

    public Iterator<AttributeCombination> iterator()
        {
        return this;
        }

    public AttributeCombination next()
        {
        if (!hasNext())
            {
            return null;
            }
        final AttributeCombination ret = new AttributeCombination(combo, labels);
        hasNext = increment(0);
        return ret;
        }

    public void remove()
        {
        throw new UnsupportedOperationException();
        }
    }
