package org.epistasis;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.epistasis.combinatoric.mdr.newengine.Dataset;

public class ChiSquaredScorer extends IsolatedAttributeScorer {
	private byte[] classes;
	private int classNLevels;
	private boolean pvalue;
	private Map<String, String> config = new TreeMap<String, String>();

	public ChiSquaredScorer(final Dataset data, final boolean pvalue,
			final boolean parallel) {
		super(data, parallel);
		init(pvalue);
	}

	public ChiSquaredScorer(final Dataset data, final boolean pvalue,
			final boolean parallel, final Runnable onIncrementProgress) {
		super(data, parallel, onIncrementProgress);
		init(pvalue);
	}

	@Override
	protected double computeScore(final int index) {
		final byte[] attribute = getData().getColumn(index);
		final int nLevels = getData().getLevels().get(index).size();
		final double[][] table = new double[classNLevels][nLevels];
		for (int i = 0; i < getData().getRows(); ++i) {
			table[classes[i]][attribute[i]]++;
		}
		double chisq = Utility.computeChiSquared(table);
		chisq = pvalue ? (double) Utility.pchisq(chisq, (classNLevels - 1)
				* (nLevels - 1)) : chisq;
		return chisq;
	}

	@Override
	protected double[] computeScores() {
		final int classIdx = getData().getCols() - 1;
		classNLevels = getData().getLevels().get(classIdx).size();
		classes = getData().getColumn(classIdx);
		return super.computeScores();
	}

	@Override
	public Map<String, String> getConfig() {
		return config;
	}

	private void init(final boolean pPvalue) {
		pvalue = pPvalue;
		config.put("FILTER", "CHISQUARED");
		config.put("PVALUE", Boolean.toString(pvalue));
		config = Collections.unmodifiableMap(config);
	}
}
