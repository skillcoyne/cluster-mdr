package org.epistasis;
public class DisplayPair<F, S> extends Pair<F, S> {
	public DisplayPair(final F f, final S s) {
		super(f, s);
	}

	@Override
	public String toString() {
		return getFirst().toString();
	}
}
