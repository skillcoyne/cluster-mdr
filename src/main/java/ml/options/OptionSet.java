package ml.options;

import java.math.BigInteger;

/**
 * This class holds the information for a <i>set</i> of ml.options. A set can hold any number of
 * <code>OptionData</code> instances which are
 * checked together to determine success or failure.
 * <p/>
 * The approach to use this class looks like this:
 * <p/>
 * <ol>
 * <li>The user uses any of the <code>Options.addSet()</code> (e. g. {@link Options#addSet(String)}) to create any
 * number of sets required
 * (or just relies on the default set, if only one set is required)
 * <li>The user adds all required option definitions to each set
 * <li>Using any of the <code>Options.check()</code> methods, each set can be checked whether the ml.options that
 * were specified on the command
 * line satisfy its requirements
 * <li>If the check was successful for a given set, several data items are available from this class:
 * <ul>
 * <li>All ml.options defined for the set (through with e. g. values, details, and multiplicity are available)
 * <li>All data items found (these are the items on the command line which do not start with the prefix,
 * i. e. non-option arguments)
 * <li>All unmatched arguments on the command line (these are the items on the command line which start with the
 * prefix, but do not match to
 * one of the ml.options). Programs can elect to ignore these, or react with an error
 * </ul>
 * </ol>
 * From JavaWorld article by Dr. Matthias Laux (http://www.javaworld.com/javaworld/jw-08-2004/jw-0816-command.html)
 * with changes made to
 * accomodate Java 1.4 January 24, 2007 -- Changed to accomodate Java 1.5 by Nate Barney
 */
public class OptionSet
    {
    private final static String CLASS = "OptionSet";
    private final java.util.ArrayList<OptionData> options = new java.util.ArrayList<OptionData>();
    private final java.util.HashMap<String, OptionData> keys = new java.util.HashMap<String, OptionData>();
    private final java.util.ArrayList<String> unmatched = new java.util.ArrayList<String>();
    private final java.util.ArrayList<String> data = new java.util.ArrayList<String>();
    private String setName = null;
    private int minData = 0;
    private int maxData = 0;
    private Options.Prefix prefix = null;
    private Options.Multiplicity defaultMultiplicity = null;

    /**
     * Constructor
     */
    OptionSet(final Options.Prefix prefix,
              final Options.Multiplicity defaultMultiplicity, final String setName,
              final int minData, final int maxData)
        {
        if (setName == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": setName may not be null");
            }
        if (minData < 0)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": minData must be >= 0");
            }
        if (maxData < minData)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": maxData must be >= minData");
            }
        this.prefix = prefix;
        this.defaultMultiplicity = defaultMultiplicity;
        this.setName = setName;
        this.minData = minData;
        this.maxData = maxData;
        }

    /**
     * Add a non-value option with the given key, and the default prefix and multiplicity
     * <p/>
     *
     * @param key The key for the option
     *            <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined
     */
    public OptionSet addOption(final String key)
        {
        return addOption(key, defaultMultiplicity);
        }

    /**
     * Add a value option with the given key and separator, possibly details, and the default prefix and multiplicity
     * <p/>
     *
     * @param key       The key for the option
     * @param details   A boolean indicating whether details are expected for the option
     * @param separator The separator for the option
     *                  <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined or if
     *                                  <code>separator</code> is <code>null</code>
     */
    public OptionSet addOption(final String key, final boolean details,
                               final Options.Separator separator)
        {
        return addOption(key, details, separator, true, defaultMultiplicity);
        }

    /**
     * The master method to add an option. Since there are combinations which are not acceptable (like a NONE
     * separator and a true value),
     * this method is not public. Internally, we only supply acceptable combinations.
     */
    OptionSet addOption(final String key, final boolean details,
                        final Options.Separator separator, final boolean value,
                        final Options.Multiplicity multiplicity)
        {
        if (key == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": key may not be null");
            }
        if (multiplicity == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": multiplicity may not be null");
            }
        if (separator == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": separator may not be null");
            }
        if (keys.containsKey(key))
            {
            throw new IllegalArgumentException(OptionSet.CLASS + ": the key " + key
                                               + " has already been defined for this OptionSet");
            }
        final OptionData od = new OptionData(prefix, key, details, separator,
                                             value, multiplicity);
        options.add(od);
        keys.put(key, od);
        return this;
        }

    /**
     * Add a value option with the given key, separator, and multiplicity, possibly details, and the default prefix
     * <p/>
     *
     * @param key          The key for the option
     * @param details      A boolean indicating whether details are expected for the option
     * @param separator    The separator for the option
     * @param multiplicity The multiplicity for the option
     *                     <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined or if
     *                                  <code>separator</code> or <code>multiplicity</code> are <code>null</code>
     */
    public OptionSet addOption(final String key, final boolean details,
                               final Options.Separator separator, final Options.Multiplicity multiplicity)
        {
        return addOption(key, details, separator, true, multiplicity);
        }

    /**
     * Add a non-value option with the given key and multiplicity, and the default prefix
     * <p/>
     *
     * @param key          The key for the option
     * @param multiplicity The multiplicity for the option
     *                     <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined or if
     *                                  <code>multiplicity</code> is <code>null</code>
     */
    public OptionSet addOption(final String key,
                               final Options.Multiplicity multiplicity)
        {
        return addOption(key, false, Options.Separator.NONE, false, multiplicity);
        }

    /**
     * Add a value option with the given key and separator, no details, and the default prefix and multiplicity
     * <p/>
     *
     * @param key       The key for the option
     * @param separator The separator for the option
     *                  <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined or if
     *                                  <code>separator</code> is <code>null</code>
     */
    public OptionSet addOption(final String key, final Options.Separator separator)
        {
        return addOption(key, false, separator, true, defaultMultiplicity);
        }

    /**
     * Add a value option with the given key, separator, and multiplicity, no details, and the default prefix
     * <p/>
     *
     * @param key          The key for the option
     * @param separator    The separator for the option
     * @param multiplicity The multiplicity for the option
     *                     <p/>
     * @return The set instance itself (to support invocation chaining for <code>addOption()</code> methods)
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or a key with this name has
     * already been defined or if
     *                                  <code>separator</code> or <code>multiplicity</code> are <code>null</code>
     */
    public OptionSet addOption(final String key,
                               final Options.Separator separator, final Options.Multiplicity multiplicity)
        {
        return addOption(key, false, separator, true, multiplicity);
        }

    /**
     * Return the data items found (these are the items on the command line which do not start with the prefix,
     * i. e. non-option arguments)
     * <p/>
     *
     * @return A list of strings with all data items found
     */
    public java.util.ArrayList<String> getData()
        {
        return data;
        }

    /**
     * Getter method for <code>maxData</code> property
     * <p/>
     *
     * @return The value for the <code>maxData</code> property
     */
    public int getMaxData()
        {
        return maxData;
        }

    /**
     * Getter method for <code>minData</code> property
     * <p/>
     *
     * @return The value for the <code>minData</code> property
     */
    public int getMinData()
        {
        return minData;
        }

    /**
     * Get the data for a specific option, identified by its key name (which is unique)
     * <p/>
     *
     * @param key The key for the option
     *            <p/>
     * @return The {@link OptionData} instance
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or unknown in this set
     */
    public OptionData getOption(final String key)
        {
        if (key == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": key may not be null");
            }
        if (!keys.containsKey(key))
            {
            throw new IllegalArgumentException(OptionSet.CLASS + ": unknown key: "
                                               + key);
            }
        return keys.get(key);
        }

    /**
     * return a single int value for an option
     *
     * @param key the key for the option
     * @return the integer value of the option
     * @throws NumberFormatException if malformed
     */
    public int getOptionInteger(final String key)
        {
        int rval;
        try
            {
            rval = Integer.parseInt(getOption(key).getResultValue(0));
            }
        catch (final NumberFormatException ex)
            {
            System.out.println("Error: " + key + " accepts integer values only.");
            throw (ex);
            }
        return (rval);
        }

    public BigInteger getOptionBigInt(final String key)
        {
        BigInteger rval;
        try
            {
            rval = new BigInteger(getOption(key).getResultValue(0));
            }
        catch (final NumberFormatException ex)
            {
            System.out
                    .println("Error: " + key + " accepts (big)integer values only.");
            throw (ex);
            }
        // TODO Auto-generated method stub
        return rval;
        }

    /**
     * Get a list of all the ml.options defined for this set
     * <p/>
     *
     * @return A list of {@link OptionData} instances defined for this set
     */
    public java.util.ArrayList<OptionData> getOptionData()
        {
        return options;
        }

    /**
     * Getter method for <code>setName</code> property
     * <p/>
     *
     * @return The value for the <code>setName</code> property
     */
    public String getSetName()
        {
        return setName;
        }

    /**
     * Return all unmatched items found (these are the items on the command line which start with the prefix,
     * but do not match to one of the
     * ml.options)
     * <p/>
     *
     * @return A list of strings with all unmatched items found
     */
    public java.util.ArrayList<String> getUnmatched()
        {
        return unmatched;
        }

    /**
     * Check whether a specific option is set, i. e. whether it was specified at least once on the command line.
     * <p/>
     *
     * @param key The key for the option
     *            <p/>
     * @return <code>true</code> or <code>false</code>, depending on the outcome of the check
     *         <p/>
     * @throws IllegalArgumentException If the <code>key</code> is <code>null</code> or unknown in this set
     */
    public boolean isSet(final String key)
        {
        if (key == null)
            {
            throw new IllegalArgumentException(OptionSet.CLASS
                                               + ": key may not be null");
            }
        if (!keys.containsKey(key))
            {
            throw new IllegalArgumentException(OptionSet.CLASS + ": unknown key: "
                                               + key);
            }
        return (keys.get(key)).getResultCount() > 0 ? true : false;
        }
    }
