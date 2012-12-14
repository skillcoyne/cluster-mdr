package org.epistasis.combinatoric.mdr.newengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public abstract class RandomCombinationGenerator implements
                                                 Iterator<AttributeCombination>, Iterable<AttributeCombination>
    {
    private final Random rand;
    private final List<Integer> attributes;
    private final List<String> labels;
    private final int attrCount;

    public RandomCombinationGenerator(final List<String> labels,
                                      final int attrCount, final long seed)
        {
        this.labels = labels;
        rand = new Random(seed);
        this.attrCount = attrCount;
        attributes = new ArrayList<Integer>(labels.size() - 1);
        for (int i = 0; i < labels.size() - 1; ++i)
            {
            attributes.add(i);
            }
        }

    public Iterator<AttributeCombination> iterator()
        {
        return this;
        }

    public AttributeCombination next()
        {
        Collections.shuffle(attributes, rand);
        final List<Integer> attr = new ArrayList<Integer>(attrCount);
        for (int i = 0; i < attrCount; ++i)
            {
            attr.add(attributes.get(i));
            }
        return new AttributeCombination(attr, labels);
        }

    public void remove()
        {
        throw new UnsupportedOperationException();
        }
    }
