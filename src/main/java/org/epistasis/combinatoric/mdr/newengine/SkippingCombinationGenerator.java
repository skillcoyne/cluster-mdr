/**
 *
 */
package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;

/**
 * @author jsegal
 */
public class SkippingCombinationGenerator extends CombinationGenerator
    {
    private final int mdrNodeCount;

    /**
     * @param nVars
     * @param nCombo
     * @param labels
     */
    public SkippingCombinationGenerator(final int nVars, final int nCombo,
                                        final List<String> labels, final int mdrNodeCount, final int mdrNodeNumber)
        {
        super(nVars, nCombo, labels);
        if ((mdrNodeNumber > mdrNodeCount) || (mdrNodeCount < 2)
            || (mdrNodeNumber < 1))
            {
            throw new IllegalArgumentException("mdrNodeNumber " + mdrNodeNumber
                                               + " > mdrNodeCount " + mdrNodeCount);
            }
        this.mdrNodeCount = mdrNodeCount;
        // now skip mdrNodeNumber-1 entries so that the first time next is called, we
        // get the right combination
        skipN(mdrNodeNumber - 1);
        }

    @Override
    public AttributeCombination next() {
    // When this is called, the iterator will be pointing to the next
    // combination to be returned. So we fetch it, and then skip
    // combinations to prepare for the next time it gets called
    final AttributeCombination rval = super.next();
    skipN(mdrNodeCount - 1);
    return rval;
    }

    private void skipN(int numberToSkip)
        {
        while (numberToSkip > 0)
            {
            super.next();
            --numberToSkip;
            }
        }
    }
