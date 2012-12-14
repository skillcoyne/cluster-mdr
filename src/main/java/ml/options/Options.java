package ml.options;

import java.util.Iterator;

/**
 * The central class for option processing. Sets are identified by their name, but there is also an anonymous default
 * set, which is very
 * convenient if an application requieres only one set. January 24, 2007 -- Changed to accomodate Java 1.5 by Nate
 * Barney
 */
public class Options
    {
    private final static String CLASS = "Options";
    /**
     * The name used internally for the default set
     */
    public final static String DEFAULT_SET = "DEFAULT_OPTION_SET";
    private final java.util.HashMap<String, OptionSet> optionSets = new java.util.HashMap<String, OptionSet>();
    private Prefix prefix = null;
    private Multiplicity defaultMultiplicity = null;
    private String[] arguments = null;
    private int defaultMinData = 0;
    private int defaultMaxData = 0;
    private StringBuffer checkErrors = null;

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}, the default number of data items is set to 0,
     * and the multiplicity is set to
     * {@link Multiplicity#ONCE}.
     * <p/>
     *
     * @param args The command line arguments to check
     *             <p/>
     * @throws IllegalArgumentException If <code>args</code> is <code>null</code>
     */
    public Options(final String args[])
        {
        this(args, Prefix.DASH, Multiplicity.ONCE);
        }

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}, and the multiplicity is set to {@link Multiplicity#ONCE}.
     * <p/>
     *
     * @param args The command line arguments to check
     * @param data The default minimum and maximum number of data items for all sets (can be overridden when adding a
     *             set)
     *             <p/>
     * @throws IllegalArgumentException If <code>args</code> is <code>null</code> - or if the data range value
     * doesn't make sense
     */
    public Options(final String args[], final int data)
        {
        this(args, Prefix.DASH, Multiplicity.ONCE, data, data);
        }

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}, and the multiplicity is set to {@link Multiplicity#ONCE}.
     * <p/>
     *
     * @param args       The command line arguments to check
     * @param defMinData The default minimum number of data items for all sets (can be overridden when adding a set)
     * @param defMaxData The default maximum number of data items for all sets (can be overridden when adding a set)
     *                   <p/>
     * @throws IllegalArgumentException If <code>args</code> is <code>null</code> - or if the data range values don't
     * make sense
     */
    public Options(final String args[], final int defMinData, final int defMaxData)
        {
        this(args, Prefix.DASH, Multiplicity.ONCE, defMinData, defMaxData);
        }

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}, and the default number of data items is set to 0.
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>defaultMultiplicity</code> is <code>null
     * </code>
     */
    public Options(final String args[], final Multiplicity defaultMultiplicity)
        {
        this(args, Prefix.DASH, defaultMultiplicity, 0, 0);
        }

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}.
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     * @param data                The default minimum and maximum number of data items for all sets (can be
     *                            overridden when adding a set)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>defaultMultiplicity</code> is <code>null
     * </code> - or if the data
     *                                  range value doesn't make sense
     */
    public Options(final String args[], final Multiplicity defaultMultiplicity,
                   final int data)
        {
        this(args, Prefix.DASH, defaultMultiplicity, data, data);
        }

    /**
     * Constructor. The prefix is set to {@link Prefix#DASH}.
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     * @param defMinData          The default minimum number of data items for all sets (can be overridden when
     *                            adding a set)
     * @param defMaxData          The default maximum number of data items for all sets (can be overridden when
     *                            adding a set)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>defaultMultiplicity</code> is
     * <code>null</code> - or if the data
     *                                  range values don't make sense
     */
    public Options(final String args[], final Multiplicity defaultMultiplicity,
                   final int defMinData, final int defMaxData)
        {
        this(args, Prefix.DASH, defaultMultiplicity, defMinData, defMaxData);
        }

    /**
     * Constructor. The default number of data items is set to 0, and the multiplicity is set to {@link
     * Multiplicity#ONCE}.
     * <p/>
     *
     * @param args   The command line arguments to check
     * @param prefix The prefix to use for all command line ml.options. It can only be set here for all ml.options at
     *               the same time
     *               <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>prefix</code> is <code>null</code>
     */
    public Options(final String args[], final Prefix prefix)
        {
        this(args, prefix, Multiplicity.ONCE, 0, 0);
        }

    /**
     * Constructor. The multiplicity is set to {@link Multiplicity#ONCE}.
     * <p/>
     *
     * @param args   The command line arguments to check
     * @param prefix The prefix to use for all command line ml.options. It can only be set here for all ml.options at
     * @param data   The default minimum and maximum number of data items for all sets (can be overridden when adding
     *               a set)
     *               <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>prefix</code> is <code>null</code> - or
     * if the data range value
     *                                  doesn't make sense
     */
    public Options(final String args[], final Prefix prefix, final int data)
        {
        this(args, prefix, Multiplicity.ONCE, data, data);
        }

    /**
     * Constructor. The multiplicity is set to {@link Multiplicity#ONCE}.
     * <p/>
     *
     * @param args       The command line arguments to check
     * @param prefix     The prefix to use for all command line ml.options. It can only be set here for all ml
     *                   .options at the same time
     * @param defMinData The default minimum number of data items for all sets (can be overridden when adding a set)
     * @param defMaxData The default maximum number of data items for all sets (can be overridden when adding a set)
     *                   <p/>
     * @throws IllegalArgumentException If either <code>args</code> or <code>prefix</code> is <code>null</code> - or
     * if the data range values
     *                                  don't make sense
     */
    public Options(final String args[], final Prefix prefix,
                   final int defMinData, final int defMaxData)
        {
        this(args, prefix, Multiplicity.ONCE, defMinData, defMaxData);
        }

    /**
     * Constructor. The default number of data items is set to 0.
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param prefix              The prefix to use for all command line ml.options. It can only be set here for all
     *                            ml.options at the same time
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code>, <code>prefix</code>,
     * or <code>defaultMultiplicity</code> is
     *                                  <code>null</code>
     */
    public Options(final String args[], final Prefix prefix,
                   final Multiplicity defaultMultiplicity)
        {
        this(args, prefix, defaultMultiplicity, 0, 0);
        }

    /**
     * Constructor
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param prefix              The prefix to use for all command line ml.options. It can only be set here for all
     *                            ml.options at the same time
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     * @param data                The default minimum and maximum number of data items for all sets (can be
     *                            overridden when adding a set)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code>, <code>prefix</code>,
     * or <code>defaultMultiplicity</code> is
     *                                  <code>null</code> - or if the data range value doesn't make sense
     */
    public Options(final String args[], final Prefix prefix,
                   final Multiplicity defaultMultiplicity, final int data)
        {
        this(args, prefix, defaultMultiplicity, data, data);
        }

    /**
     * Constructor
     * <p/>
     *
     * @param args                The command line arguments to check
     * @param prefix              The prefix to use for all command line ml.options. It can only be set here for all
     *                            ml.options at the same time
     * @param defaultMultiplicity The default multiplicity to use for all ml.options (can be overridden when adding
     *                            an option)
     * @param defMinData          The default minimum number of data items for all sets (can be overridden when
     *                            adding a set)
     * @param defMaxData          The default maximum number of data items for all sets (can be overridden when
     *                            adding a set)
     *                            <p/>
     * @throws IllegalArgumentException If either <code>args</code>, <code>prefix</code>,
     * or <code>defaultMultiplicity</code> is
     *                                  <code>null</code> - or if the data range values don't make sense From
     *                                  JavaWorld article by Dr. Matthias Laux *
     *                                  (http://www.javaworld.com/javaworld/jw-08-2004/jw-0816-command.html) with
     *                                  changes made to accomodate Java 1.4
     */
    public Options(final String args[], final Prefix prefix,
                   final Multiplicity defaultMultiplicity, final int defMinData,
                   final int defMaxData)
        {
        if (args == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": args may not be null");
            }
        if (prefix == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": prefix may not be null");
            }
        if (defaultMultiplicity == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": defaultMultiplicity may not be null");
            }
        if (defMinData < 0)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": defMinData must be >= 0");
            }
        if (defMaxData < defMinData)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": defMaxData must be >= defMinData");
            }
        arguments = new String[args.length];
        for (int i = 0; i < args.length; ++i)
            {
            arguments[i] = args[i];
            }
        this.prefix = prefix;
        this.defaultMultiplicity = defaultMultiplicity;
        defaultMinData = defMinData;
        defaultMaxData = defMaxData;
        }

    /**
     * Add the given non-value option to <i>all</i> known sets. See {@link OptionSet#addOption(String)} for details.
     */
    public void addOptionAllSets(final String key)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, defaultMultiplicity);
            }
        }

    /**
     * Add the given value option to <i>all</i> known sets. See {@link OptionSet#addOption(String, boolean,
     * Options.Separator)} for details.
     */
    public void addOptionAllSets(final String key, final boolean details,
                                 final Separator separator)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, details, separator, true,
                                             defaultMultiplicity);
            }
        }

    /**
     * Add the given value option to <i>all</i> known sets. See
     * {@link OptionSet#addOption(String, boolean, Options.Separator, Options.Multiplicity)} for details.
     */
    public void addOptionAllSets(final String key, final boolean details,
                                 final Separator separator, final Multiplicity multiplicity)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, details, separator, true,
                                             multiplicity);
            }
        }

    /**
     * Add the given non-value option to <i>all</i> known sets. See {@link OptionSet#addOption(String,
     * Options.Multiplicity)} for details.
     */
    public void addOptionAllSets(final String key, final Multiplicity multiplicity)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, false, Separator.NONE, false,
                                             multiplicity);
            }
        }

    /**
     * Add the given value option to <i>all</i> known sets. See {@link OptionSet#addOption(String,
     * Options.Separator)} for details.
     */
    public void addOptionAllSets(final String key, final Separator separator)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, false, separator, true,
                                             defaultMultiplicity);
            }
        }

    /**
     * Add the given value option to <i>all</i> known sets. See {@link OptionSet#addOption(String, Options.Separator,
     * Options.Multiplicity)}
     * for details.
     */
    public void addOptionAllSets(final String key, final Separator separator,
                                 final Multiplicity multiplicity)
        {
        for (final String string : optionSets.keySet())
            {
            optionSets.get(string).addOption(key, false, separator, true,
                                             multiplicity);
            }
        }

    /**
     * Add an option set. The defaults for the number of data items are used.
     * <p/>
     *
     * @param setName The name for the set. This must be a unique identifier
     *                <p/>
     * @return The new <code>Optionset</code> instance created. This is useful to allow chaining of <code>addOption()
     * </code> calls right after
     *         this method
     */
    public OptionSet addSet(final String setName)
        {
        return addSet(setName, defaultMinData, defaultMaxData);
        }

    /**
     * Add an option set.
     * <p/>
     *
     * @param setName The name for the set. This must be a unique identifier
     * @param data    The minimum and maximum number of data items for this set
     *                <p/>
     * @return The new <code>Optionset</code> instance created. This is useful to allow chaining of <code>addOption()
     * </code> calls right after
     *         this method
     */
    public OptionSet addSet(final String setName, final int data)
        {
        return addSet(setName, data, data);
        }

    /**
     * Add an option set.
     * <p/>
     *
     * @param setName The name for the set. This must be a unique identifier
     * @param minData The minimum number of data items for this set
     * @param maxData The maximum number of data items for this set
     *                <p/>
     * @return The new <code>Optionset</code> instance created. This is useful to allow chaining of <code>addOption()
     * </code> calls right after
     *         this method
     */
    public OptionSet addSet(final String setName, final int minData,
                            final int maxData)
        {
        if (setName == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": setName may not be null");
            }
        if (optionSets.containsKey(setName))
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": a set with the name " + setName + " has already been defined");
            }
        final OptionSet os = new OptionSet(prefix, defaultMultiplicity, setName,
                                           minData, maxData);
        optionSets.put(setName, os);
        return os;
        }

    /**
     * Run the checks for the default set. <code>ignoreUnmatched</code> is set to <code>false</code>,
     * and <code>requireDataLast</code> is set
     * to <code>true</code>.
     * <p/>
     *
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check()
        {
        return check(Options.DEFAULT_SET, false, true);
        }

    /**
     * Run the checks for the default set.
     * <p/>
     *
     * @param ignoreUnmatched A boolean to select whether unmatched ml.options can be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have to be the last ones on the command
     *                        line or not
     *                        <p/>
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check(final boolean ignoreUnmatched,
                         final boolean requireDataLast)
        {
        return check(Options.DEFAULT_SET, ignoreUnmatched, requireDataLast);
        }

    /**
     * Run the checks for the given set. <code>ignoreUnmatched</code> is set to <code>false</code>,
     * and <code>requireDataLast</code> is set to
     * <code>true</code>.
     * <p/>
     *
     * @param setName The name for the set to check
     *                <p/>
     * @return A boolean indicating whether all checks were successful or not
     *         <p/>
     * @throws IllegalArgumentException If either <code>setName</code> is <code>null</code>, or the set is unknown.
     */
    public boolean check(final String setName)
        {
        return check(setName, false, true);
        }

    /**
     * Run the checks for the given set.
     * <p/>
     *
     * @param setName         The name for the set to check
     * @param ignoreUnmatched A boolean to select whether unmatched ml.options can be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have to be the last ones on the command
     *                        line or not
     *                        <p/>
     * @return A boolean indicating whether all checks were successful or not
     *         <p/>
     * @throws IllegalArgumentException If either <code>setName</code> is <code>null</code>, or the set is unknown.
     */
    public boolean check(final String setName, final boolean ignoreUnmatched,
                         final boolean requireDataLast)
        {
        boolean isOkay = true;
        if (setName == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": setName may not be null");
            }
        if (optionSets.get(setName) == null)
            {
            throw new IllegalArgumentException(Options.CLASS
                                               + ": Unknown OptionSet: " + setName);
            }
        checkErrors = new StringBuffer();
        checkErrors.append("Checking set ");
        checkErrors.append(setName);
        checkErrors.append('\n');
        // .... Access the data for the set to use
        final OptionSet set = optionSets.get(setName);
        final java.util.ArrayList<OptionData> options = set.getOptionData();
        final java.util.ArrayList<String> data = set.getData();
        final java.util.ArrayList<String> unmatched = set.getUnmatched();
        // .... Catch some trivial cases
        if (options.size() == 0)
            { // No ml.options have been defined at all
            if (arguments.length > 0)
                { // No arguments have been given: in this case,
                // this is a success
                checkErrors.append("No ml.options have been defined, nothing to check\n");
                isOkay = false;
                }
            }
        else if (arguments.length == 0)
            { // Options have been defined, but no
            // arguments given
            checkErrors
                    .append("Options have been defined, but no arguments have been given; nothing to check\n");
            isOkay = false;
            }
        // .... Parse all the arguments given
        if (isOkay)
            {
            int ipos = 0;
            int offset = 0;
            java.util.regex.Matcher m = null;
            String value = null;
            String detail = null;
            String next = null;
            String key = null;
            final String pre = Character.toString(prefix.getName());
            boolean add = true;
            final boolean[] matched = new boolean[arguments.length];
            for (int i = 0; i < matched.length; i++)
                {
                // Initially, we assume there was no match at all
                matched[i] = false;
                }
            while (true)
                {
                value = null;
                detail = null;
                offset = 0;
                add = true;
                key = arguments[ipos];
                for (final OptionData optionData : options)
                    { // For
                    m = optionData.getPattern().matcher(key);
                    if (m.lookingAt())
                        {
                        if (optionData.useValue())
                            { // The code section for value ml.options
                            if (optionData.useDetail())
                                {
                                detail = m.group(1);
                                offset = 2; // required for correct Matcher.group access below
                                }
                            if (optionData.getSeparator() == Separator.BLANK)
                                { // In this
                                // case, the
                                // next
                                // argument
                                // must be the
                                // value
                                if (ipos + 1 == arguments.length)
                                    { // The last argument, thus
                                    // no value follows it:
                                    // Error
                                    checkErrors
                                            .append("At end of arguments - no value found following argument ");
                                    checkErrors.append(key);
                                    checkErrors.append('\n');
                                    add = false;
                                    }
                                else
                                    {
                                    next = arguments[ipos + 1];
                                    if (next.startsWith(pre))
                                        { // The next one is an argument,
                                        // not a value: Error
                                        checkErrors.append("No value found following argument ");
                                        checkErrors.append(key);
                                        checkErrors.append('\n');
                                        add = false;
                                        }
                                    else
                                        {
                                        value = next;
                                        matched[ipos++] = true; // Mark the key and the value
                                        matched[ipos] = true;
                                        }
                                    }
                                }
                            else
                                { // The value follows the separator in this case
                                value = m.group(1 + offset);
                                matched[ipos] = true;
                                }
                            }
                        else
                            { // Simple, non-value ml.options
                            matched[ipos] = true;
                            }
                        if (add)
                            {
                            optionData.addResult(value, detail); // Store the result
                            }
                        break; // No need to check more ml.options, we have a match
                        }
                    }
                ipos++; // Advance to the next argument to check
                if (ipos >= arguments.length)
                    {
                    break; // Terminating condition for the
                    // check loop
                    }
                }
            // .... Identify unmatched arguments and actual (non-option) data
            int first = -1; // Required later for requireDataLast
            for (int i = 0; i < matched.length; i++)
                { // Assemble the list of
                // unmatched ml.options
                if (!matched[i])
                    {
                    if (arguments[i].startsWith(pre))
                        { // This is an unmatched option
                        unmatched.add(arguments[i]);
                        checkErrors.append("No matching option found for argument ");
                        checkErrors.append(arguments[i]);
                        checkErrors.append('\n');
                        }
                    else
                        { // This is actual data
                        if (first < 0)
                            {
                            first = i;
                            }
                        data.add(arguments[i]);
                        }
                    }
                }
            // .... Checks to determine overall success; start with multiplicity of
            // ml.options
            for (final Iterator<OptionData> i = options.iterator(); i.hasNext()
                                                                    && isOkay; )
                {
                final OptionData optionData = i.next();
                key = optionData.getKey();
                if (((optionData.getMultiplicity() == Multiplicity.ONCE) && (optionData
                                                                                     .getResultCount() != 1))
                    || ((optionData.getMultiplicity() == Multiplicity.ONCE_OR_MORE) && (optionData
                                                                                                .getResultCount() == 0))
                    || ((optionData.getMultiplicity() == Multiplicity.ZERO_OR_ONE) && (optionData
                                                                                               .getResultCount() > 1)))
                    {
                    checkErrors.append("Wrong number of occurences found for argument ");
                    checkErrors.append(prefix.getName());
                    checkErrors.append(key);
                    checkErrors.append('\n');
                    isOkay = false;
                    }
                }
            // .... Check range for data
            if ((data.size() < set.getMinData()) || (data.size() > set.getMaxData()))
                {
                checkErrors.append("Invalid number of data arguments: ");
                checkErrors.append(data.size());
                checkErrors.append(" (allowed range: ");
                checkErrors.append(set.getMinData());
                checkErrors.append(" ... ");
                checkErrors.append(set.getMaxData());
                checkErrors.append(")\n");
                isOkay = false;
                }
            // .... Check for location of the data in the list of command line
            // arguments
            if (requireDataLast && (first >= 0))
                {
                if (first + data.size() != arguments.length)
                    {
                    checkErrors
                            .append("Invalid data specification: data arguments are not the last ones on the command line\n");
                    isOkay = false;
                    }
                }
            // .... Check for unmatched arguments
            if (!ignoreUnmatched && (unmatched.size() > 0))
                {
                isOkay = false; // Don't accept unmatched arguments
                }
            } // end if still okay
        // .... If we made it to here, all checks were successful
        return isOkay;
        }

    /**
     * The error messages collected during the last option check (invocation of any of the <code>check()</code>
     * methods). This is useful to
     * determine what was wrong with the command line arguments provided
     * <p/>
     *
     * @return A string with all collected error messages
     */
    public String getCheckErrors()
        {
        return checkErrors.toString();
        }

    /**
     * Return the (first) matching set. This invocation does not ignore unmatched ml.options and requires that data items are the last ones on
     * the command line.
     * <p/>
     *
     * @return The first set which matches (i. e. the <code>check()</code> method returns <code>true</code>) - or <code>null</code>, if no set
     *         matches.
     */
    public OptionSet getMatchingSet()
        {
        return getMatchingSet(false, true);
        }

    /**
     * Return the (first) matching set.
     * <p/>
     *
     * @param ignoreUnmatched A boolean to select whether unmatched ml.options can be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have to be the last ones on the command line or not
     *                        <p/>
     * @return The first set which matches (i. e. the <code>check()</code> method returns <code>true</code>) - or
     * <code>null</code>, if no set
     *         matches.
     */
    public OptionSet getMatchingSet(final boolean ignoreUnmatched,
                                    final boolean requireDataLast)
        {
        for (final String string : optionSets.keySet())
            {
            final String setName = string.toString();
            if (check(setName, ignoreUnmatched, requireDataLast))
                {
                return optionSets.get(setName);
                }
            }
        return null;
        }

    /**
     * This returns the (anonymous) default set
     * <p/>
     *
     * @return The default set
     */
    public OptionSet getSet()
        {
        if (getSet(Options.DEFAULT_SET) == null)
            {
            addSet(Options.DEFAULT_SET, defaultMinData, defaultMaxData);
            }
        return getSet(Options.DEFAULT_SET);
        }

    /**
     * Return an option set - or <code>null</code>, if no set with the given name exists
     * <p/>
     *
     * @param setName The name for the set to retrieve
     *                <p/>
     * @return The set to retrieve (or <code>null</code>, if no set with the given name exists)
     */
    public OptionSet getSet(final String setName)
        {
        return optionSets.get(setName);
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
    for (final OptionSet set : optionSets.values())
        {
        sb.append("Set: ");
        sb.append(set.getSetName());
        sb.append('\n');
        for (final OptionData data : set.getOptionData())
            {
            sb.append(data.toString());
            sb.append('\n');
            }
        }
    return sb.toString();
    }

    /**
     * An enum encapsulating the possible multiplicities for ml.options
     */
    public static final class Multiplicity
        {
        /**
         * Option needs to occur exactly once
         */
        public static final Multiplicity ONCE = new Multiplicity(0);
        /**
         * Option needs to occur at least once
         */
        public static final Multiplicity ONCE_OR_MORE = new Multiplicity(1);
        /**
         * Option needs to occur either once or not at all
         */
        public static final Multiplicity ZERO_OR_ONE = new Multiplicity(2);
        /**
         * Option can occur any number of times
         */
        public static final Multiplicity ZERO_OR_MORE = new Multiplicity(3);
        private final int i;

        private Multiplicity(final int i)
            {
            this.i = i;
            }

        public int getValue()
            {
            return i;
            }
        }

    /**
     * An enum encapsulating the possible prefixes identifying ml.options (and separating them from command line data items)
     */
    public static final class Prefix
        {
        /**
         * Options start with a "-" (typically on Unix platforms)
         */
        public static final Prefix DASH = new Prefix('-');
        /**
         * Options start with a "/" (typically on Windows platforms)
         */
        public static final Prefix SLASH = new Prefix('/');
        private final char c;

        private Prefix(final char c)
            {
            this.c = c;
            }

        /**
         * Return the actual prefix character
         * <p/>
         *
         * @return The actual prefix character
         */
        char getName()
            {
            return c;
            }
        }

    /**
     * An enum encapsulating the possible separators between value ml.options and their actual values.
     */
    public static final class Separator
        {
        /**
         * Separate option and value by ":"
         */
        public static final Separator COLON = new Separator(':');
        /**
         * Separate option and value by "="
         */
        public static final Separator EQUALS = new Separator('=');
        /**
         * Separate option and value by blank space
         */
        public static final Separator BLANK = new Separator(' '); // Or, more
        // precisely,
        // whitespace (as
        // allowed by the
        // CLI)
        /**
         * This is just a placeholder in case no separator is required (i. e. for non-value ml.options)
         */
        public static final Separator NONE = new Separator('D'); // NONE is a
        // placeholder in
        // case no
        // separator is
        // required, 'D'
        // is just an
        // arbitrary dummy
        // value
        private final char c;

        private Separator(final char c)
            {
            this.c = c;
            }

        /**
         * Return the actual separator character
         * <p/>
         *
         * @return The actual separator character
         */
        char getName()
            {
            return c;
            }
        }
    }
