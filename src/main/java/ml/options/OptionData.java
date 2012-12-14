package ml.options;

/**
 * This class holds all the data for an option. This includes the prefix, the key,
 * the separator (for value ml.options), the multiplicity, and
 * all the other settings describing the option. The class is designed to be only a data container from a user
 * perspective, i. e. the user
 * has read-access to any data determined by the {@link Options#check()}, but not access to any of the other methods
 * which are used
 * internally for the operation of the actual check. From JavaWorld article by Dr. Matthias Laux
 * (http://www.javaworld.com/javaworld/jw-08-2004/jw-0816-command.html) with changes made to accomodate Java 1.4
 * January 24, 2007 -- Changed
 * to accomodate Java 1.5 by Nate Barney
 */
public class OptionData
    {
    private final static String CLASS = "OptionData";
    private Options.Prefix prefix = null;
    private String key = null;
    private boolean detail = false;
    private Options.Separator separator = null;
    private boolean value = false;
    private Options.Multiplicity multiplicity = null;
    private java.util.regex.Pattern pattern = null;
    private int counter = 0;
    private java.util.ArrayList<String> values = null;
    private java.util.ArrayList<String> details = null;

    /**
     * The constructor
     */
    OptionData(final Options.Prefix prefix, final String key,
               final boolean detail, final Options.Separator separator,
               final boolean value, final Options.Multiplicity multiplicity)
        {
        if (prefix == null)
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": prefix may not be null");
            }
        if (key == null)
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": key may not be null");
            }
        if (separator == null)
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": separator may not be null");
            }
        if (multiplicity == null)
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": multiplicity may not be null");
            }
        // .... The data describing the option
        this.prefix = prefix;
        this.key = key;
        this.detail = detail;
        this.separator = separator;
        this.value = value;
        this.multiplicity = multiplicity;
        // .... Create the pattern to match this option
        if (value)
            {
            if (separator == Options.Separator.BLANK)
                {
                if (detail)
                    {
                    pattern = java.util.regex.Pattern.compile(prefix.getName() + key
                                                              + "((\\w|\\.)+)$");
                    }
                else
                    {
                    pattern = java.util.regex.Pattern.compile(prefix.getName() + key
                                                              + "$");
                    }
                }
            else
                {
                if (detail)
                    {
                    pattern = java.util.regex.Pattern.compile(prefix.getName() + key
                                                              + "((\\w|\\.)+)" + separator.getName() + "(.+)$");
                    }
                else
                    {
                    pattern = java.util.regex.Pattern.compile(prefix.getName() + key
                                                              + separator.getName() + "(.+)$");
                    }
                }
            }
        else
            {
            pattern = java.util.regex.Pattern.compile(prefix.getName() + key + "$");
            }
        // .... Structures to hold result data
        if (value)
            {
            values = new java.util.ArrayList<String>();
            if (detail)
                {
                details = new java.util.ArrayList<String>();
                }
            }
        }

    /**
     * Store the data for a match found
     */
    void addResult(final String valueData, final String detailData)
        {
        if (value)
            {
            if (valueData == null)
                {
                throw new IllegalArgumentException(OptionData.CLASS
                                                   + ": valueData may not be null");
                }
            values.add(valueData);
            if (detail)
                {
                if (detailData == null)
                    {
                    throw new IllegalArgumentException(OptionData.CLASS
                                                       + ": detailData may not be null");
                    }
                details.add(detailData);
                }
            }
        counter++;
        }

    /**
     * Getter method for <code>key</code> property
     * <p/>
     *
     * @return The value for the <code>key</code> property
     */
    String getKey()
        {
        return key;
        }

    /**
     * Getter method for <code>multiplicity</code> property
     * <p/>
     *
     * @return The value for the <code>multiplicity</code> property
     */
    Options.Multiplicity getMultiplicity()
        {
        return multiplicity;
        }

    /**
     * Getter method for <code>pattern</code> property
     * <p/>
     *
     * @return The value for the <code>pattern</code> property
     */
    java.util.regex.Pattern getPattern()
        {
        return pattern;
        }

    /**
     * Getter method for <code>prefix</code> property
     * <p/>
     *
     * @return The value for the <code>prefix</code> property
     */
    Options.Prefix getPrefix()
        {
        return prefix;
        }

    /**
     * Get the number of results found for this option, which is number of times the key matched
     * <p/>
     *
     * @return The number of results
     */
    public int getResultCount()
        {
        int result;
        if (value)
            {
            result = values.size();
            }
        else
            {
            result = counter;
            }
        return result;
        }

    /**
     * Get the detail with the given index. The index can range between 0 and {@link #getResultCount()}<code> -
     * 1</code>. However, only for
     * value ml.options which take details, a non-<code>null</code> detail will be returned. Non-value ml.options and
     * value ml.options which do not
     * take details always return <code>null</code>.
     * <p/>
     *
     * @param index The index for the desired value
     *              <p/>
     * @return The option detail with the given index
     *         <p/>
     * @throws IllegalArgumentException If the value for <code>index</code> is out of bounds
     */
    public String getResultDetail(final int index)
        {
        if (!detail)
            {
            return null;
            }
        if ((index < 0) || (index >= getResultCount()))
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": illegal value for index");
            }
        return details.get(index);
        }

    /**
     * Get the value with the given index. The index can range between 0 and {@link #getResultCount()}<code> -
     * 1</code>. However, only for
     * value ml.options, a non-<code>null</code> value will be returned. Non-value ml.options always return
     * <code>null</code>.
     * <p/>
     *
     * @param index The index for the desired value
     *              <p/>
     * @return The option value with the given index
     *         <p/>
     * @throws IllegalArgumentException If the value for <code>index</code> is out of bounds
     */
    public String getResultValue(final int index)
        {
        if (!value)
            {
            return null;
            }
        if ((index < 0) || (index >= getResultCount()))
            {
            throw new IllegalArgumentException(OptionData.CLASS
                                               + ": illegal value for index");
            }
        return values.get(index);
        }

    /**
     * Getter method for <code>separator</code> property
     * <p/>
     *
     * @return The value for the <code>separator</code> property
     */
    Options.Separator getSeparator()
        {
        return separator;
        }

    /**
     * This is the overloaded {@link Object#toString()} method, and it is provided mainly for debugging purposes.
     * <p/>
     *
     * @return A string representing the instance
     */
    @Override
    public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Prefix      : ");
    sb.append(prefix);
    sb.append('\n');
    sb.append("Key         : ");
    sb.append(key);
    sb.append('\n');
    sb.append("Detail      : ");
    sb.append(detail);
    sb.append('\n');
    sb.append("Separator   : ");
    sb.append(separator);
    sb.append('\n');
    sb.append("Value       : ");
    sb.append(value);
    sb.append('\n');
    sb.append("Multiplicity: ");
    sb.append(multiplicity);
    sb.append('\n');
    sb.append("Pattern     : ");
    sb.append(pattern);
    sb.append('\n');
    sb.append("Results     : ");
    sb.append(counter);
    sb.append('\n');
    if (value)
        {
        if (detail)
            {
            for (int i = 0; i < values.size(); i++)
                {
                sb.append(details.get(i));
                sb.append(" / ");
                sb.append(values.get(i));
                sb.append('\n');
                }
            }
        else
            {
            for (int i = 0; i < values.size(); i++)
                {
                sb.append(values.get(i));
                sb.append('\n');
                }
            }
        }
    return sb.toString();
    }

    /**
     * Getter method for <code>detail</code> property
     * <p/>
     *
     * @return The value for the <code>detail</code> property
     */
    boolean useDetail()
        {
        return detail;
        }

    /**
     * Getter method for <code>value</code> property
     * <p/>
     *
     * @return The value for the <code>value</code> property
     */
    boolean useValue()
        {
        return value;
        }
    }
