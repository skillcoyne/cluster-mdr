/**
 *
 */
package org.epistasis.combinatoric.mdr;

import java.util.HashMap;
import java.util.Map;

public enum WeightScheme
    {
        PROPORTIONAL("Fitness Proportional"), RANK("Ranked Selection");
    private static Map<String, WeightScheme> displayNameMap = new HashMap<String, WeightScheme>();

    static
        {
        for (final WeightScheme weightScheme : WeightScheme.values())
            {
            WeightScheme.displayNameMap.put(weightScheme.getDisplayName(),
                                            weightScheme);
            }
        } // end static initializer

    private final String displayName;

    public static WeightScheme getWeightScheme(final String displayName)
        {
        return WeightScheme.displayNameMap.get(displayName);
        }

    private WeightScheme(final String displayName)
        {
        this.displayName = displayName;
        }

    public String getDisplayName()
        {
        return displayName;
        }
    }
