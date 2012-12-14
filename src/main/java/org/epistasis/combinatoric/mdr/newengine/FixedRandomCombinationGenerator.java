package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;

public class FixedRandomCombinationGenerator extends RandomCombinationGenerator
    {
    private final long maxEval;
    private long nEval;

    public FixedRandomCombinationGenerator(final List<String> labels,
                                           final int attrCount, final long seed, final long maxEval)
        {
        super(labels, attrCount, seed);
        this.maxEval = maxEval;
        nEval = 0;
        }

    public boolean hasNext()
        {
        return nEval <= maxEval;
        }

    @Override
    public AttributeCombination next() {
    final AttributeCombination next = super.next();
    ++nEval;
    return next;
    }
    }
