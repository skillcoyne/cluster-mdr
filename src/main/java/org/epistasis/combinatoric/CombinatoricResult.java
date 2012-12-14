package org.epistasis.combinatoric;

import java.text.NumberFormat;
import java.util.List;

public interface CombinatoricResult<V> extends
		Comparable<CombinatoricResult<V>>, Cloneable {
	public static final int VALID = 0;

	public void assignAverage(List<CombinatoricResult<V>> results);

	public Object clone();

	public double getFitness();

	public int getStatus();

	public String getStatusString();

	public String getSummaryString();

	public void setFromSummaryString(String s);

	public void setNumberFormat(NumberFormat nf);
}
