package org.epistasis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ColumnFormat {
	private final List<Integer> widths;

	public static String fitStringWidth(String string, final int width) {
		if (string.length() > width) {
			string = string.substring(0, width);
		} else if (string.length() < width) {
			final char[] fill = new char[width - string.length()];
			Arrays.fill(fill, ' ');
			string += new String(fill);
		}
		return string;
	}

	public ColumnFormat(final List<Integer> widths) {
		this.widths = new ArrayList<Integer>(widths.size());
		this.widths.addAll(widths);
	}

	public String format(final List<String> values) {
		final StringBuffer b = new StringBuffer();
		final Iterator<String> i = values.iterator();
		final Iterator<Integer> j = widths.iterator();
		while (i.hasNext() && j.hasNext()) {
			b.append(ColumnFormat.fitStringWidth(i.next().toString(), j.next()));
		}
		return b.toString();
	}
}
