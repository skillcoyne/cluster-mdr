package org.epistasis.combinatoric;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.epistasis.ColumnFormat;
import org.epistasis.Entropy;
import org.epistasis.combinatoric.mdr.Console;
import org.epistasis.combinatoric.mdr.Console.AmbiguousCellStatus;
import org.epistasis.combinatoric.mdr.Main;
import org.epistasis.combinatoric.mdr.newengine.AttributeCombination;
import org.epistasis.combinatoric.mdr.newengine.Dataset;
import org.epistasis.combinatoric.mdr.newengine.Model;

public class EntropyAnalysis {
	private static final List<Integer> firsttwo = Arrays.asList(0, 1);
	private List<String> attrNames;
	private double[][] entropy;
	private double[][] cond_entropy;
	private double class_entropy;
	private double[] class_cond_entropy;
	private double maxDist = 1000;
	private List<AttributeCombination> combos;
	private Dataset data;
	private HierarchicalCluster<AttributeCombination> cluster;
	private DistanceMatrix<AttributeCombination> distmat;
	private DistanceMatrix<AttributeCombination> infomat;
	private Console.AmbiguousCellStatus tieStatus = Main.defaultAmbiguousCellStatus;

	public EntropyAnalysis() {
	}

	public void clear() {
		combos = null;
		data = null;
		cluster = null;
		distmat = null;
		infomat = null;
	}

	private void constructMatrices(final List<AttributeCombination> pCombos) {
		final List<byte[]> attributes = generateAttributes(pCombos);
		final byte[] classAttribute = data.getColumn(data.getCols() - 1); // get class level indices
		class_entropy = Entropy.getEntropy(classAttribute);
		class_cond_entropy = new double[attributes.size()];
		entropy = new double[attributes.size()][attributes.size()];
		cond_entropy = new double[attributes.size()][attributes.size()];
		attrNames = new ArrayList<String>(attributes.size());
		distmat = new DistanceMatrix<AttributeCombination>();
		infomat = new DistanceMatrix<AttributeCombination>();
		for (int i = 0; i < attributes.size(); i++) {
			final byte[] attribute = attributes.get(i);
			entropy[i][i] = Entropy.getEntropy(attribute);
			cond_entropy[i][i] = Entropy.getConditionalEntropy(attribute,
					classAttribute);
			class_cond_entropy[i] = Entropy.getConditionalEntropy(classAttribute,
					attribute);
			attrNames.add(combos.get(i).toString());
		}
		for (int i = 0; i < attributes.size() - 1; ++i) {
			final byte[] a = attributes.get(i);
			for (int j = i + 1; j < attributes.size(); ++j) {
				final byte[] b = attributes.get(j);
				final byte[] combinedAttribute = constructMDRAttribute(a, b);
				entropy[i][j] = entropy[j][i] = Entropy.getEntropy(combinedAttribute);
				cond_entropy[i][j] = cond_entropy[j][i] = Entropy
						.getConditionalEntropy(combinedAttribute, classAttribute);
				final double info = (cond_entropy[i][i] + cond_entropy[j][j] - cond_entropy[i][j])
						- (entropy[i][i] + entropy[j][j] - entropy[i][j]);
				double distance = Math.abs(1 / info);
				distance = Math.min(maxDist, distance);
				distmat.put(combos.get(i), combos.get(j), distance);
				infomat.put(combos.get(i), combos.get(j), info);
			}
		}
	}

	protected byte[] constructMDRAttribute(final byte[] a, final byte[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException(
					"byte arrays a and b must be same length. a.length: " + a.length
							+ " b.length: " + b.length);
		}
		byte[][] attrs = new byte[3][];
		attrs[0] = a;
		attrs[1] = b;
		attrs[2] = data.getColumn(data.getCols() - 1);
		final List<List<String>> levels = new ArrayList<List<String>>(attrs.length);
		for (final byte[] attributeLevelIndices : attrs) {
			final SortedSet<String> currentLevels = new TreeSet<String>();
			for (final byte attributeLevelIndex : attributeLevelIndices) {
				currentLevels.add(Byte.toString(attributeLevelIndex));
			}
			levels.add(new ArrayList<String>(currentLevels));
		}
		attrs = null;
		final byte[][] rows = new byte[data.getRows()][3];
		final byte[] statusLevelIndices = data.getColumn(data.getCols() - 1);
		for (int rowIndex = 0; rowIndex < rows.length; ++rowIndex) {
			rows[rowIndex][0] = a[rowIndex];
			rows[rowIndex][1] = b[rowIndex];
			rows[rowIndex][2] = statusLevelIndices[rowIndex];
		}
		final Dataset newdata = new Dataset(data.getMissing(), false /* paired */,
				Arrays.asList("A", "B", "Class"), levels, rows, rows.length,
				data.getAffectedStatus(), data.getUnaffectedStatus());
		final Model model = new Model(new AttributeCombination(
				EntropyAnalysis.firsttwo, newdata.getLabels()), tieStatus);
		model.buildCounts(newdata);
		model.buildStatuses(newdata, data.getStatusCounts());
		return model.constructRawAttribute(newdata);
	}

	protected List<byte[]> generateAttributes(
			final List<AttributeCombination> pCombos) {
		final List<byte[]> attributes = new ArrayList<byte[]>(pCombos.size());
		for (final AttributeCombination combo : pCombos) {
			byte[] attribute;
			if (combo.size() == 1) {
				attribute = data.getColumn(combo.get(0));
			} else {
				final Model model = new Model(combo, tieStatus);
				model.buildCounts(data);
				model.buildStatuses(data, data.getStatusCounts());
				attribute = model.constructRawAttribute(data);
			}
			attributes.add(attribute);
		}
		return attributes;
	}

	public List<String> getAttrNames() {
		return attrNames;
	}

	public double[] getClass_cond_entropy() {
		return class_cond_entropy;
	}

	public double getClass_entropy() {
		return class_entropy;
	}

	public HierarchicalCluster<AttributeCombination> getCluster() {
		return cluster;
	}

	public List<AttributeCombination> getCombos() {
		return combos;
	}

	public double[][] getCond_entropy() {
		return cond_entropy;
	}

	public Dataset getData() {
		return data;
	}

	public DistanceMatrix<AttributeCombination> getDistmat() {
		return distmat;
	}

	public DistanceMatrix<AttributeCombination> getDistMatrix() {
		return distmat;
	}

	public double[][] getEntropy() {
		return entropy;
	}

	public String getEntropyText(final NumberFormat nf) {
		final List<Integer> widths = new ArrayList<Integer>();
		final List<String> values = new ArrayList<String>();
		final StringBuffer b = new StringBuffer();
		if (entropy == null) {
			return b.toString();
		}
		widths.add(12);
		widths.add(10);
		widths.add(10);
		widths.add(10);
		ColumnFormat fmt = new ColumnFormat(widths);
		b.append("Single Attribute Values:\n\n");
		values.add("Attribute");
		values.add("H(A)");
		values.add("H(A|C)");
		values.add("I(A;C)");
		b.append(fmt.format(values));
		b.append('\n');
		values.clear();
		values.add("---------");
		values.add("----");
		values.add("------");
		values.add("------");
		b.append(fmt.format(values));
		b.append('\n');
		values.clear();
		for (int i = 0; i < entropy.length; ++i) {
			values.add(attrNames.get(i).toString());
			values.add(nf.format(entropy[i][i]));
			values.add(nf.format(cond_entropy[i][i]));
			values.add(nf.format(class_entropy - class_cond_entropy[i]));
			b.append(fmt.format(values));
			b.append('\n');
			values.clear();
		}
		b.append('\n');
		widths.clear();
		widths.add(new Integer(12));
		widths.add(new Integer(12));
		widths.add(new Integer(10));
		widths.add(new Integer(10));
		widths.add(new Integer(10));
		widths.add(new Integer(10));
		widths.add(new Integer(10));
		fmt = new ColumnFormat(widths);
		b.append("Pairwise Values:\n\n");
		values.add("Attribute A");
		values.add("Attribute B");
		values.add("H(AB)");
		values.add("H(AB|C)");
		values.add("I(A;B)");
		values.add("I(A;B;C)");
		values.add("I(AB;C)");
		b.append(fmt.format(values));
		b.append('\n');
		values.clear();
		values.add("-----------");
		values.add("-----------");
		values.add("-----");
		values.add("-------");
		values.add("------");
		values.add("--------");
		values.add("-------");
		b.append(fmt.format(values));
		b.append('\n');
		values.clear();
		for (int i = 0; i < entropy.length - 1; ++i) {
			for (int j = i + 1; j < entropy[i].length; ++j) {
				final double i_a_b = entropy[i][i] + entropy[j][j] - entropy[i][j];
				final double i_a_b_c = cond_entropy[i][i] + cond_entropy[j][j]
						- cond_entropy[i][j] - i_a_b;
				final double i_a_c = class_entropy - class_cond_entropy[i];
				final double i_b_c = class_entropy - class_cond_entropy[j];
				final double i_ab_c = i_a_b_c + i_a_c + i_b_c;
				values.add(attrNames.get(i).toString());
				values.add(attrNames.get(j).toString());
				values.add(nf.format(entropy[i][j]));
				values.add(nf.format(cond_entropy[i][j]));
				values.add(nf.format(i_a_b));
				values.add(nf.format(i_a_b_c));
				values.add(nf.format(i_ab_c));
				b.append(fmt.format(values));
				b.append('\n');
				values.clear();
			}
		}
		return b.toString();
	}

	/**
	 * This creates bins that are even divided between negative and positive values If 5 bins requested, first two are degrees of being
	 * negative, middle bin means 'near zero' and last 2 bins are degrees of being positive
	 * @param numberOfBins
	 * @param info
	 * @return
	 */
	public int getIndexIntoRange(final int numberOfBins, final double info) {
		final double maxDistanceFromZero = getMaxDistanceFromZero();
		final double totalRangeSize = maxDistanceFromZero * 2; // range covers both
		// negative and positive
		// numbers
		final double minOfVirtualRange = -maxDistanceFromZero;
		final double amountPerBin = totalRangeSize / numberOfBins;
		int bin = numberOfBins - 1; // initialize to last bin and then check to see
		// if in any earlier bin
		for (int index = 1; index < numberOfBins; ++index) {
			final double binStart = minOfVirtualRange + (index * amountPerBin);
			if (info < binStart) {
				// if less than bin's starting value then must be in previous bin
				bin = index - 1;
				break;
			}
		}
		return bin;
	} // end getIndexIntoRange

	public double getInfo(
			final HierarchicalCluster.Cluster<AttributeCombination> a,
			final HierarchicalCluster.Cluster<AttributeCombination> b) {
		double sum = 0;
		for (final AttributeCombination o1 : a) {
			for (final AttributeCombination o2 : b) {
				sum += infomat.get(o1, o2);
			}
		}
		return sum / (a.size() * b.size());
	}

	public DistanceMatrix<AttributeCombination> getInfomat() {
		return infomat;
	}

	public DistanceMatrix<AttributeCombination> getInfoMatrix() {
		return infomat;
	}

	public double getMaxDist() {
		return maxDist;
	}

	public double getMaxDistanceFromZero() {
		double maxDistanceFromZero = 0.0;
		if (infomat != null) {
			maxDistanceFromZero = Math.max(Math.abs(infomat.getMinValue()),
					Math.abs(infomat.getMaxValue()));
		}
		return maxDistanceFromZero;
	}

	public AmbiguousCellStatus getTiePriority() {
		return tieStatus;
	}

	public void recalc() {
		if (combos == null) {
			return;
		}
		constructMatrices(combos);
		cluster = new HierarchicalCluster<AttributeCombination>(distmat);
	}

	public void set(final List<AttributeCombination> combos, final Dataset data) {
		this.combos = combos;
		this.data = data;
		recalc();
	}

	public void setMaxDist(final double maxDist) {
		this.maxDist = maxDist;
	}

	public void setTiePriority(final AmbiguousCellStatus tiePriority) {
		tieStatus = tiePriority;
	}
}
