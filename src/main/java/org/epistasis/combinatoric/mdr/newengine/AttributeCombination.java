package org.epistasis.combinatoric.mdr.newengine;

import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;
import java.util.SortedSet;
import java.util.TreeSet;

public class AttributeCombination implements RandomAccess,
                                             Comparable<AttributeCombination>
    {
    public static final AttributeCombination EMPTY = new AttributeCombination(
            Arrays.asList(0), Arrays.asList("{EMPTY}"));
    private int[] combo;
    private final List<String> labels;

    public AttributeCombination(final List<Integer> comboList,
                                final List<String> labels)
        {
        copySortedSetIntoArray(new TreeSet<Integer>(comboList));
        this.labels = labels;
        }

    public AttributeCombination(final String comboString,
                                final List<String> labels)
        {
        this.labels = labels;
        if (comboString.trim().length() == 0)
            {
            throw new IllegalArgumentException("From line '" + comboString
                                               + "' forcedAttributes must contain at least one attribute.");
            }
        final String[] fields = comboString.split("[, \t;:]+");
        final SortedSet<Integer> sortedSet = new TreeSet<Integer>();
        for (int i = 0; i < fields.length; ++i)
            {
            fields[i] = fields[i].trim();
            if (fields[i].length() == 0)
                {
                continue;
                }
            int index = labels.indexOf(fields[i]);
            if (index < 0)
                {
                index = labels.indexOf(fields[i].toUpperCase());
                }
            if (index < 0)
                {
                index = labels.indexOf(fields[i].toLowerCase());
                }
            if (index < 0)
                {
                int columnNumber;
                try
                    {
                    columnNumber = Integer.parseInt(fields[i]);
                    }
                catch (final Exception ex)
                    {
                    throw new IllegalArgumentException("From line '" + comboString
                                                       + "', item #" + (index + 1) + " '" + fields[i]
                                                       + "' is not a recognized attribute label.");
                    }
                if (columnNumber < 1)
                    {
                    throw new IllegalArgumentException("From line '" + comboString
                                                       + "', item #" + (index + 1) + " '" + fields[i]
                                                       + "' is less than 1.");
                    }
                else if (columnNumber > labels.size())
                    {
                    throw new IllegalArgumentException("From line '" + comboString
                                                       + "', item #" + (index + 1) + " '" + fields[i]
                                                       + "' is greater than the number of attributes.");
                    }
                index = columnNumber - 1;
                } // end if seeing if they typed in a number rather than an attribute name
            sortedSet.add(index);
            } // end for
        copySortedSetIntoArray(sortedSet);
        } // end constructor

    public int compareTo(final AttributeCombination k)
        {
        // an object is always equal to itself
        if (this == k)
            {
            return 0;
            }
        // sort based on length
        if (size() < k.size())
            {
            return -1;
            }
        else if (size() > k.size())
            {
            return 1;
            }
        else
            {
            for (int i = 0; i < size(); ++i)
                {
                final int attribute = get(i);
                final int kattribute = k.get(i);
                if (attribute < kattribute)
                    {
                    return -1;
                    }
                else if (attribute > kattribute)
                    {
                    return 1;
                    }
                }
            }
        // no differences, so they're equal
        return 0;
        }

    private void copySortedSetIntoArray(final SortedSet<Integer> sortedSet)
        {
        combo = new int[sortedSet.size()];
        int ctr = 0;
        for (final int attributeIndex : sortedSet)
            {
            combo[ctr++] = attributeIndex;
            }
        }

    public int get(final int index)
        {
        return combo[index];
        }

    public int[] getAttributeIndices()
        {
        return combo;
        }

    public String getComboString()
        {
        return getComboString(',');
        }

    public String getComboString(final char delimiter)
        {
        final StringBuffer b = new StringBuffer();
        for (int comboAttributeIndex = 0; comboAttributeIndex < size(); ++comboAttributeIndex)
            {
            if (comboAttributeIndex != 0)
                {
                b.append(delimiter);
                }
            final int attributeIndex = combo[comboAttributeIndex];
            if ((labels == null) || (attributeIndex >= labels.size()))
                {
                b.append(attributeIndex + 1);
                }
            else
                {
                b.append(labels.get(attributeIndex));
                }
            }
        return b.toString();
        }

    public String getLabel(final int index)
        {
        return labels.get(combo[index]);
        }

    public List<String> getLabels()
        {
        return labels;
        }

    @Override
    public int hashCode() {
    return combo.hashCode();
    }

    public void setLabels(final List<String> labels)
        {
        this.labels.clear();
        this.labels.addAll(labels);
        }

    public int size()
        {
        return combo.length;
        }

    @Override
    public String toString() {
    return getComboString(' ');
    }
    }
