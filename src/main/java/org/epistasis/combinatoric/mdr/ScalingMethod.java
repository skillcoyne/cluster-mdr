/**
 *
 */
package org.epistasis.combinatoric.mdr;

import java.util.HashMap;
import java.util.Map;

public enum ScalingMethod
    {
        LINEAR("Linear"), EXPONENTIAL("Exponential");
    private static Map<String, ScalingMethod> displayNameMap = new HashMap<String, ScalingMethod>();

    static
        {
        for (final ScalingMethod scalingMethod : ScalingMethod.values())
            {
            ScalingMethod.displayNameMap.put(scalingMethod.getDisplayName(),
                                             scalingMethod);
            }
        } // end static initializer

    private final String displayName;

    public static ScalingMethod getScalingMethod(final String displayName)
        {
        return ScalingMethod.displayNameMap.get(displayName);
        }

    private ScalingMethod(final String displayName)
        {
        this.displayName = displayName;
        }

    public String getDisplayName()
        {
        return displayName;
        }
    }
