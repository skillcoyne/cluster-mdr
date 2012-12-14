package org.epistasis.combinatoric.mdr.newengine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.epistasis.combinatoric.mdr.ExpertKnowledge.RWRuntime;

public class EDACombinationGenerator implements Iterator<AttributeCombination>,
                                                Iterable<AttributeCombination>
    {
    private final Random rnd;
    private final List<String> labels;
    private final int attrCount;
    private final RWRuntime rwRuntime;
    private final long maxEval;
    private long nEval;

    public EDACombinationGenerator(final List<String> labels,
                                   final int attrCount, final Random rnd, final long maxEval,
                                   final RWRuntime rwRuntime)
        {
        this.labels = labels;
        this.rnd = rnd;
        this.attrCount = attrCount;
        this.rwRuntime = rwRuntime;
        this.maxEval = maxEval;
        nEval = 0;
        }

    public boolean hasNext()
        {
        return nEval <= maxEval;
        }

    public Iterator<AttributeCombination> iterator()
        {
        return this;
        }

    public AttributeCombination next()
        {
        ++nEval;
        final List<Integer> attr = new ArrayList<Integer>(attrCount);
        final int[] attributes = rwRuntime.getNAttributes(rnd, attrCount);
        for (final int attribute : attributes)
            {
            attr.add(attribute);
            }
        return new AttributeCombination(attr, labels);
        }

    public void remove()
        {
        throw new UnsupportedOperationException();
        }
    /*
      * public CombinationGenerator(final int nVars, final int nCombo, final List<String> labels) { this.labels =
      * labels; if (nCombo < 0) {
      * throw new IllegalArgumentException("nCombo must be non-negative"); } if (nVars < nCombo) { throw new
      * IllegalArgumentException("nCombo cannot be larger than nVars"); } this.nVars = nVars; combo = new
      * ArrayList<Integer>(nCombo); hasNext =
      * true; combo.clear(); for (int i = 0; i < nCombo; ++i) { combo.add(i); } } private boolean increment(final int
       * index) { boolean
      * didIncrement = false; if (!combo.isEmpty()) { combo.set(combo.size() - 1 - index,
      * combo.get(combo.size() - 1 - index) + 1); if
      * (combo.get(combo.size() - 1 - index) < nVars - index) { for (int i = combo.size() - index; i < combo.size();
      * ++i) { combo.set(i,
      * combo.get(i - 1) + 1); } didIncrement = true; } else if (index + 1 < combo.size()) { return increment(index + 1); } } return
      * didIncrement; } public Iterator<AttributeCombination> iterator() { return this; } public AttributeCombination next() { if (!hasNext())
      * { return null; } final AttributeCombination ret = new AttributeCombination(combo, labels); hasNext = increment(0); return ret; } public
      * void remove() { throw new UnsupportedOperationException(); }
      */
    }
