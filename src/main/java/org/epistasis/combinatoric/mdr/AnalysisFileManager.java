package org.epistasis.combinatoric.mdr;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epistasis.Pair;
import org.epistasis.combinatoric.AttributeRankerThread;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.newengine.AllModelsLandscape;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Collector;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Landscape;
import org.epistasis.gui.SwingInvoker;

public class AnalysisFileManager
    {
    public static final String cfgMin = "ATTRIBUTECOUNTMIN";
    public static final String cfgMax = "ATTRIBUTECOUNTMAX";
    public static final String cfgIntervals = "CVINTERVALS";
    public static final String cfgPaired = "PAIRED";
    public static final String cfgSeed = "RANDOMSEED";
    public static final String cfgThreshold = "THRESHOLD";
    public static final String cfgTie = "TIEVALUE";
    public static final String cfgWrapper = "WRAPPER";
    public static final String cfgForced = "FORCED";
    public static final String cfgEvaluations = "EVALUATIONS";
    public static final String cfgRuntime = "RUNTIME";
    public static final String cfgRuntimeUnits = "RUNTIMEUNITS";
    public static final String cfgValAffected = "AFFECTED";
    public static final String cfgValUnaffected = "UNAFFECTED";
    public static final String cfgValUnknown = "UNKNOWN";
    public static final String cfgValExhaustive = "EXHAUSTIVE";
    public static final String cfgValForced = "FORCED";
    public static final String cfgValEDA = "EDA";
    public static final String cfgValRandom = "RANDOM";
    private Dataset data;
    private Dataset filtered;
    private List<Collector> collectors;
    private Landscape allModelsLandscape;
    private String datafileName;
    private final Map<String, String> cfg = new TreeMap<String, String>();
    private final Map<String, String> filtercfg = new TreeMap<String, String>();
    private final List<Pair<Integer, Float>> filterScores = new ArrayList<Pair<Integer, Float>>();
    private AttributeRankerThread ranker;

    public Landscape getAllModelsLandscape()
        {
        return allModelsLandscape;
        }

    public String getCfg(final String key)
        {
        final Object value = cfg.get(key);
        return value == null ? null : value.toString();
        }

    public List<Collector> getCollectors()
        {
        return collectors;
        }

    public String getDatafileName()
        {
        return datafileName;
        }

    public Dataset getDataset()
        {
        return data;
        }

    public Map<String, String> getFilterConfig()
        {
        return filtercfg;
        }

    public Dataset getFiltered()
        {
        return filtered;
        }

    public List<Pair<Integer, Float>> getFilterScores()
        {
        return filterScores;
        }

    public AttributeCombination getForced()
        {
        AttributeCombination forced = null;
        if (cfg.containsKey(AnalysisFileManager.cfgForced)
            && (!cfg.get(AnalysisFileManager.cfgForced).equalsIgnoreCase("NONE")))
            {
            forced = new AttributeCombination(cfg.get(AnalysisFileManager.cfgForced),
                                              data.getLabels());
            }
        return forced;
        }

    public int getIntervals()
        {
        int intervals = Main.defaultCrossValidationCount;
        if (cfg.containsKey(AnalysisFileManager.cfgIntervals))
            {
            intervals = Integer.parseInt(cfg.get(AnalysisFileManager.cfgIntervals));
            }
        return intervals;
        }

    public int getMax()
        {
        int max = Main.defaultAttributeCountMax;
        if (cfg.containsKey(AnalysisFileManager.cfgMax))
            {
            max = Integer.parseInt(cfg.get(AnalysisFileManager.cfgMax));
            }
        return max;
        }

    public int getMin()
        {
        int min = Main.defaultAttributeCountMin;
        if (cfg.containsKey(AnalysisFileManager.cfgMin))
            {
            min = Integer.parseInt(cfg.get(AnalysisFileManager.cfgMin));
            }
        return min;
        }

    public long getRandomSeed()
        {
        long seed = Main.defaultCrossValidationCount;
        if (cfg.containsKey(AnalysisFileManager.cfgIntervals))
            {
            seed = Long.parseLong(cfg.get(AnalysisFileManager.cfgIntervals));
            }
        return seed;
        }

    public double getThreshold()
        {
        double threshold = Main.defaultRatioThreshold;
        if (cfg.containsKey(AnalysisFileManager.cfgThreshold) && !isAutoThreshold())
            {
            threshold = Double.parseDouble(cfg.get(AnalysisFileManager.cfgThreshold));
            }
        return threshold;
        }

    public Console.AmbiguousCellStatus getTiePriority()
        {
        Console.AmbiguousCellStatus tieStatus = Main.defaultAmbiguousCellStatus;
        if (cfg.containsKey(AnalysisFileManager.cfgTie))
            {
            final String tieStatusString = cfg.get(AnalysisFileManager.cfgTie);
            tieStatus = Console.AmbiguousCellStatus
                    .getTiePriorityFromString(tieStatusString);
            }
        return tieStatus;
        }

    public boolean isAutoThreshold()
        {
        boolean autoThresh = Main.defaultAutoRatioThreshold;
        if (cfg.containsKey(AnalysisFileManager.cfgThreshold))
            {
            autoThresh = (cfg.get(AnalysisFileManager.cfgThreshold))
                    .equalsIgnoreCase("AUTO");
            }
        return autoThresh;
        }

    public boolean isPaired()
        {
        boolean paired = Main.defaultPairedAnalysis;
        if (cfg.containsKey(AnalysisFileManager.cfgPaired))
            {
            paired = Boolean.valueOf(cfg.get(AnalysisFileManager.cfgPaired))
                    .booleanValue();
            }
        return paired;
        }

    public void putCfg(final String key, final String value)
        {
        cfg.put(key, value);
        }

    public void read(final LineNumberReader r, final SwingInvoker progressUpdater)
            throws IllegalArgumentException
        {
        try
            {
            final Pattern p = Pattern.compile("^@(.+)$");
            final Pattern kv = Pattern.compile("^(\\w+)\\s*=\\s*(.+)$");
            String s = null;
            String section = "";
            data = null;
            collectors = null;
            allModelsLandscape = null;
            datafileName = null;
            while ((s = r.readLine()) != null)
                {
                final Matcher m = p.matcher(s);
                if (m.matches())
                    {
                    section = m.group(1);
                    if (section.equalsIgnoreCase("DATAFILE"))
                        {
                        data = new Dataset(Main.defaultMissing, isPaired());
                        section = data.read(r, p, progressUpdater).substring(1);
                        }
                    if (section.equalsIgnoreCase("LANDSCAPE"))
                        {
                        allModelsLandscape = new AllModelsLandscape(getIntervals());
                        section = allModelsLandscape.read(r, p, data.getLabels())
                                .substring(1);
                        }
                    else if (section.equalsIgnoreCase("RESULTS"))
                        {
                        if (collectors == null)
                            {
                            collectors = new ArrayList<Collector>(getMax() - getMin() + 1);
                            }
                        final Dataset datasetToUseForCollectors = (filtered != null) ? filtered
                                                                                     : data;
                        for (int i = getMin(); i <= getMax(); ++i)
                            {
                            final Collector coll = new Collector(getIntervals(), 0, null,
                                                                 null);
                            coll.read(datasetToUseForCollectors, getTiePriority(), r);
                            collectors.add(coll);
                            }
                        }
                    }
                else if (section.equalsIgnoreCase("END"))
                    {
                    break;
                    }
                else if (section.equalsIgnoreCase("DATAFILEINFORMATION"))
                    {
                    final Matcher m2 = kv.matcher(s);
                    if (!m2.matches())
                        {
                        throw new IllegalStateException();
                        }
                    final String key = m2.group(1).toUpperCase();
                    final String value = m2.group(2);
                    if (key.equals("DATAFILE"))
                        {
                        datafileName = value;
                        }
                    }
                else if (section.equalsIgnoreCase("CONFIGURATION"))
                    {
                    final Matcher m2 = kv.matcher(s);
                    if (!m2.matches())
                        {
                        throw new IllegalStateException();
                        }
                    final String key = m2.group(1).toUpperCase();
                    final String value = m2.group(2);
                    cfg.put(key, value);
                    }
                else if (section.equalsIgnoreCase("FILTERCONFIG"))
                    {
                    final Matcher m2 = kv.matcher(s);
                    if (!m2.matches())
                        {
                        throw new IllegalStateException();
                        }
                    final String key = m2.group(1).toUpperCase();
                    final String value = m2.group(2);
                    filtercfg.put(key, value);
                    }
                else if (section.equalsIgnoreCase("FILTERSCORES"))
                    {
                    final String[] values = s.split("\\s+");
                    filterScores.add(new Pair<Integer, Float>(data.getLabels().indexOf(
                            values[0]), Float.parseFloat(values[1])));
                    }
                else if (section.equalsIgnoreCase("FILTERSELECTION"))
                    {
                    filtered = data.filter(new AttributeCombination(s, data.getLabels())
                                                   .getAttributeIndices());
                    }
                else
                    {
                    throw new IllegalStateException();
                    }
                } // end while lines
            if ((data == null) || (collectors == null))
                {
                throw new IllegalArgumentException();
                }
            }
        catch (final IOException ex)
            {
            throw new IllegalArgumentException(
                    "Invalid or corrupt analysis file. Error detected at line: "
                    + r.getLineNumber());
            }
        finally
            {
            try
                {
                r.close();
                }
            catch (final IOException ex)
                {
                // nothing to do if closign file fails
                }
            }
        }

    public void setAnalysis(final Dataset data, final Dataset filtered,
                            final AttributeRankerThread ranker, final String datafileName,
                            final int min, final int max, final List<Collector> collectors,
                            final AllModelsLandscape allModelsLandscape,
                            final AttributeCombination forced, final long seed,
                            final AmbiguousCellStatus tiePriorityList)
        {
        this.collectors = collectors;
        this.data = data;
        this.datafileName = datafileName;
        this.filtered = filtered;
        this.ranker = ranker;
        this.allModelsLandscape = allModelsLandscape;
        cfg.clear();
        cfg.put(AnalysisFileManager.cfgMin, Integer.toString(min));
        cfg.put(AnalysisFileManager.cfgMax, Integer.toString(max));
        cfg.put(AnalysisFileManager.cfgIntervals,
                Integer.toString(collectors.get(0).size()));
        if (forced != null)
            {
            cfg.put(AnalysisFileManager.cfgWrapper, AnalysisFileManager.cfgValForced);
            cfg.put(AnalysisFileManager.cfgForced, forced.getComboString());
            }
        cfg.put(AnalysisFileManager.cfgPaired, Boolean.toString(data.isPaired()));
        cfg.put(AnalysisFileManager.cfgSeed, Long.toString(seed));
        // TODO: when polytomy happens, this will need to change
        switch (tiePriorityList)
            {
            case AFFECTED:
                cfg.put(AnalysisFileManager.cfgTie, AnalysisFileManager.cfgValAffected);
                break;
            case UNAFFECTED:
                cfg.put(AnalysisFileManager.cfgTie,
                        AnalysisFileManager.cfgValUnaffected);
                break;
            case UNASSIGNED:
                cfg.put(AnalysisFileManager.cfgTie, AnalysisFileManager.cfgValUnknown);
                break;
            default:
                throw new RuntimeException("Unhandled AmbiguousCellAssgnmentType: "
                                           + tiePriorityList.toString());
            }
        }

    public void setAnalysis(final Dataset data, final String datafileName,
                            final int min, final int max, final List<Collector> collectors,
                            final AllModelsLandscape allModelsLandscape,
                            final AttributeCombination forced, final long seed,
                            final AmbiguousCellStatus tiePriorityList)
        {
        setAnalysis(data, null, null, datafileName, min, max, collectors,
                    allModelsLandscape, forced, seed, tiePriorityList);
        }

    public void write(final PrintWriter out)
        {
        out.println("@DatafileInformation");
        out.println("Datafile = " + datafileName);
        out.println("Instances = " + data.getRows());
        out.println("Attributes = " + (data.getCols() - 1));
        // TODO: when polytomy happens, this will need to change
        out.println("Ratio = " + Main.defaultFormat.format(data.getRatio()));
        out.println("@Configuration");
        write_map(out, cfg);
        out.println("@Datafile");
        data.write(out);
        if (ranker != null)
            {
            out.println("@FilterConfig");
            write_map(out, ranker.getConfig());
            out.println("@FilterScores");
            for (final Pair<Integer, Double> p : ranker.getRanker().getSortedScores())
                {
                out.print(data.getLabels().get(p.getFirst()));
                out.print(' ');
                out.println(p.getSecond());
                }
            out.println("@FilterSelection");
            out.println(ranker.getCombo().getComboString());
            }
        out.println("@Results");
        for (final Collector coll : collectors)
            {
            coll.write(out);
            }
        if (allModelsLandscape != null)
            {
            out.println("@Landscape");
            allModelsLandscape.write(out);
            }
        out.println("@End");
        out.flush();
        }

    private void write_map(final PrintWriter out, final Map<String, String> map)
        {
        for (final Entry<String, String> entry : map.entrySet())
            {
            out.print(entry.getKey());
            out.print(" = ");
            out.println(entry.getValue());
            }
        }
    }
