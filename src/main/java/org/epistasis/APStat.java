package org.epistasis;
/**
 * This is a translation of Fortran code taken from http://lib.stat.cmu.edu/apstat/, and the comments on the individual functions in this
 * class are taken directly from the original.
 */
public class APStat {
	/**
	 * ALGORITHM AS245 APPL STATIST (1989) VOL 38, NO 2. Calculation of the logarithm of the gamma function.
	 * @param x Value for which to compute the log of the gamma function.
	 * @return log(gamma(x))
	 */
	public static double alngam(double x) {
		// Fixed constants
		final double alr2pi = 9.18938533204673e-1;
		// Machine-dependant constants.
		// A table of values is given at the top of page 399 of the paper.
		// These values are for the IEEE double-precision format for which
		// B = 2, t = 53 and U = 1023 in the notation of the paper.
		final double xlge = 5.1e6;
		final double xlgst = 1e305;
		double alngam = 0;
		double y;
		// Test for valid function argument
		if ((x < 0) || (x >= xlgst)) {
			throw new IllegalArgumentException("x must satisfy 0 < x < " + xlgst);
		}
		// Calculation for 0 < X < 0.5 and 0.5 <= X < 1.5 combined
		if (x < 1.5) {
			if (x < 0.5) {
				alngam = -Math.log(x);
				y = x + 1;
				// Test whether X < machine epsilon
				if (y == 1) {
					return alngam;
				}
			} else {
				alngam = 0;
				y = x;
				x = (x - 0.5) - 0.5;
			}
			alngam += x
					* ((((3.13060547623 * y + 1.11667541262e1) * y - 2.19698958928e1) * y - 2.44387534237e1)
							* y - 2.66685511495)
					/ ((((y + 1.52346874070e1) * y + 3.14690115749e1) * y + 1.19400905721e1)
							* y + 6.07771387771e-1);
			return alngam;
		}
		// Calculation for 1.5 <= X < 4.0
		if (x < 4) {
			return (x - 2)
					* ((((4.16438922228 * x + 7.86994924154e1) * x + 1.37519416416e2) * x - 1.42046296688e2)
							* x - 7.83359299449e1)
					/ ((((x + 4.33400022514e1) * x + 2.63505074721e2) * x + 3.13399215894e2)
							* x + 4.70668766060e1);
		}
		// Calculation for 4.0 <= X < 12.0
		if (x < 12) {
			return ((((-2.29660729780e3 * x - 4.02621119975e4) * x + 2.74647644705e4)
					* x + 2.30661510616e5)
					* x - 2.12159572323e5)
					/ ((((x - 5.70691009324e2) * x - 2.42357409629e4) * x - 1.46025937511e5)
							* x - 1.16328495004e5);
		}
		// Calculation for X >= 12.0
		y = Math.log(x);
		alngam = x * (y - 1) - 0.5 * y + alr2pi;
		if (x > xlge) {
			return alngam;
		}
		final double x1 = 1 / x;
		final double x2 = x1 * x1;
		alngam += x1
				* ((6.92910599291889e-2 * x2 + 4.917317610505968e-1) * x2 + 2.79195317918525e-1)
				/ ((x2 + 6.012459259764103) * x2 + 3.350343815022304);
		return alngam;
	}

	/**
	 * Algorithm AS66 Applied Statistics (1973) vol22 no.3. Evaluates the tail area of the standardised normal curve from x to infinity if
	 * upper is .true. or from minus infinity to x if upper is .false.
	 * @param x Location for which to compute tail area
	 * @param upper True to find upper tail area, false to find lower tail area
	 * @return Tail area for given x
	 */
	public static double alnorm(double x, boolean upper) {
		if (x < 0) {
			x = -x;
			upper = !upper;
		}
		final double y = 0.5 * x * x;
		double alnorm;
		if ((x > 7.0) && !(upper && (x <= 18.66))) {
			alnorm = 0;
		} else if (x <= 1.28) {
			alnorm = 0.5
					- x
					* (0.398942280444 - 0.39990348504
							* y
							/ (y + 5.75885480458 - 29.8213557807 / (y + 2.62433121679 + 48.6959930692 / (y + 5.92885724438))));
		} else {
			alnorm = 0.398942280385
					* Math.exp(-y)
					/ (x - 3.8052e-8 + 1.00000615302 / (x + 3.98064794e-4 + 1.98615381364 / (x - 0.151679116635 + 5.29330324926 / (x + 4.8385912808 - 15.1508972451 / (x + 0.742380924027 + 30.789933034 / (x + 3.99019417011))))));
		}
		return upper ? alnorm : 1 - alnorm;
	}

	/**
	 * ALGORITHM AS 275 APPL.STATIST (1992), VOL.41, NO.2. Computes the noncentral chi-square distribution function with positive real degrees
	 * of freedom f and nonnegative noncentrality parameter theta
	 * @param x double Input value
	 * @param f double Degrees of freedom
	 * @param theta double Noncentrality parameter
	 * @return Noncentral chi-square distribution
	 */
	public static float chi2nc(final float x, final float f, final float theta) {
		final float errmax = 1e-6f;
		final int itrmax = 50;
		float chi2nc = x;
		if (f <= 0) {
			throw new IllegalArgumentException("f must be > 0");
		}
		if (theta < 0) {
			throw new IllegalArgumentException("theta must be >= 0");
		}
		if (x < 0) {
			throw new IllegalArgumentException("x must be >= 0");
		}
		if (x == 0) {
			return 0;
		}
		final float lam = theta / 2;
		// Evaluate the first term
		int n = 1;
		float u = (float) Math.exp(-lam);
		float v = u;
		final float x2 = x / 2;
		final float f2 = f / 2;
		float t = (float) (Math.pow(x2, f2) * Math.exp(-x2) / Math.exp(APStat
				.alngam(f2 + 1)));
		chi2nc = v * t;
		boolean flag = false;
		float bound = 0;
		while (true) {
			// check if (f+2n) is greater than x
			if (flag || ((f + 2 * n - x) > 0)) {
				// Find the error bound and check for convergence
				flag = true;
				bound = t * x / (f + 2 * n - x);
				if ((bound <= errmax) || (n > itrmax)) {
					if (n > itrmax) {
						throw new IllegalStateException("Maximum iterations hit");
					}
					return chi2nc;
				}
			}
			// Evaluate the next term of the expansion and then the
			// partial sum
			u *= lam / n;
			v += u;
			t *= x / (f + 2 * n);
			chi2nc += v * t;
			n++;
		}
	}

	/**
	 * ALGORITHM AS239 APPL STATIST (1988) VOL 37, NO 3 Computation of the Incomplete Gamma Integral Auxiliary functions required: ALOGAM =
	 * logarithm of the gamma function, and ALNORM = algorithm AS66
	 * @param x
	 * @param p
	 * @return Incomplete gamma integral of x and p
	 */
	public static double gammad(final double x, final double p) {
		final double oflo = 1e37;
		final double tol = 1e-14;
		final double xbig = 1e8;
		final double plimit = 1000;
		final double elimit = -88;
		double gammad = 0;
		// Check that we have valid values for X and P
		if (p <= 0) {
			throw new IllegalArgumentException("p must be > 0");
		}
		if (x < 0) {
			throw new IllegalArgumentException("x must be >= 0");
		}
		if (x == 0) {
			return 0;
		}
		// Use a normal approximation if P > PLIMIT
		if (p > plimit) {
			return APStat.alnorm(3 * Math.sqrt(p)
					* (Math.pow(x / p, 1 / 3) + 1 / (9 * p) - 1), false);
		}
		// If X is extremely large compared to P then set GAMMAD = 1
		if (x > xbig) {
			return 1;
		}
		if ((x <= 1) || (x < p)) {
			// Use Pearson's series expansion.
			// (Note that P is not large enough to force overflow in ALOGAM).
			double arg = p * Math.log(x) - x - APStat.alngam(p + 1);
			double c = 1;
			double a = p;
			gammad = 1;
			do {
				a++;
				c *= x / a;
				gammad += c;
			} while (c > tol);
			arg += Math.log(gammad);
			gammad = 0;
			if (arg >= elimit) {
				gammad = Math.exp(arg);
			}
		} else {
			// Use a continued fraction expansion
			double arg = p * Math.log(x) - x - APStat.alngam(p);
			double a = 1 - p;
			double b = a + x + 1;
			double c = 0;
			double pn1 = 1;
			double pn2 = x;
			double pn3 = x + 1;
			double pn4 = x * b;
			double pn5;
			double pn6;
			while (true) {
				a++;
				b += 2;
				c++;
				final double an = a * c;
				pn5 = b * pn3 - an * pn1;
				pn6 = b * pn4 - an * pn2;
				if (Math.abs(pn6) > 0) {
					final double rn = pn5 / pn6;
					if (Math.abs(gammad - rn) <= Math.min(tol, tol * rn)) {
						break;
					}
					gammad = rn;
				}
				pn1 = pn3;
				pn2 = pn4;
				pn3 = pn5;
				pn4 = pn6;
				if (Math.abs(pn5) >= oflo) {
					// Re-scale terms in continued fraction if terms are large
					pn1 = pn1 / oflo;
					pn2 = pn2 / oflo;
					pn3 = pn3 / oflo;
					pn4 = pn4 / oflo;
				}
			}
			arg += Math.log(gammad);
			gammad = 1;
			if (arg >= elimit) {
				gammad = 1 - Math.exp(arg);
			}
		}
		return gammad;
	}

	/**
	 * ALGORITHM AS 111, APPL.STATIST., VOL.26, 118-121, 1977. PRODUCES NORMAL DEVIATE CORRESPONDING TO LOWER TAIL AREA = P. See also AS 241
	 * which contains alternative routines accurate to about 7 and 16 decimal digits.
	 * @param p P-value for which to compute normal deviate
	 * @return Normal deviate corresponding to lower tail area = p
	 */
	public static double ppnd(final double p) {
		double result;
		final double q = p - 0.5;
		// p < 0.08 or p > 0.92, set r = min(p,1-p)
		if (Math.abs(q) > 0.42) {
			double r = Math.min(p, 1 - p);
			if (r <= 0) {
				throw new IllegalArgumentException("Invalid p value: " + p);
			}
			r = Math.sqrt(-Math.log(r));
			result = (q < 0 ? -1 : 1)
					* (((2.32121276858 * r + 4.85014127135) * r - 2.29796479134) * r - 2.78718931138)
					/ ((1.63706781897 * r + 3.54388924762) * r + 1);
		}
		// 0.08 < p < 0.92
		else {
			final double r = q * q;
			result = q
					* (((-25.44106049637 * r + 41.39119773534) * r - 18.61500062529) * r + 2.50662823884)
					/ ((((3.13082909833 * r - 21.06224101826) * r + 23.08336743743) * r - 8.47351093090)
							* r + 1);
		}
		return result;
	}

	/**
	 * Algorithm AS 190 Appl Statist (1983) Vol.32, No.2. Incorporates corrections from Appl. Statist. (1985) Vol.34 (1) Evaluates the
	 * probability from 0 to q for a studentized range having v degrees of freedom and r samples. Uses subroutine ALNORM = algorithm AS66.
	 * Arrays vw and qw store transient values used in the quadrature summation. Node spacing is controlled by step. pcutj and pcutk control
	 * truncation. Minimum and maximum number of steps are controlled by jmin, jmax, kmin and kmax. Accuracy can be increased by use of a
	 * finer grid - Increase sizes of arrays vw and qw, and jmin, jmax, kmin, kmax and 1/step proportionally.
	 * @param q Quantile for which to find p-value
	 * @param v Degrees of freedom for distribution
	 * @param r Number of samples for distribution
	 * @return P-value for q for given distribution
	 */
	public static double prtrng(final double q, final double v, final double r) {
		final double pcutj = 0.00003;
		final double pcutk = 0.0001;
		final double step = 0.45;
		final double vmax = 120.0;
		final int jmin = 3;
		final int jmax = 15;
		final int kmin = 7;
		final int kmax = 15;
		// Check initial values
		double prtrng = 0;
		if (v < 1) {
			throw new IllegalArgumentException("Degrees of freedom must be >= 1.");
		}
		if (r < 2) {
			throw new IllegalArgumentException("Number of samples must be >= 2.");
		}
		if (q <= 0) {
			return prtrng;
		}
		// Computing constants, local midpoint, adjusting steps.
		final double g = step * Math.pow(r, -0.2);
		final double gmid = 0.5 * Math.log(r);
		final double r1 = r - 1;
		double c = Math.log(r * g * 0.39894228);
		double h = 0;
		double v2 = 0;
		if (v <= vmax) {
			h = step * Math.pow(v, -0.5);
			v2 = v * 0.5;
			if (v == 1) {
				c = 0.193064705;
			} else if (v == 2) {
				c = 0.293525326;
			} else {
				c = Math.sqrt(v2)
						* 0.318309886
						/ (1 + ((-0.268132716e-2 / v2 + 0.347222222e-2) / v2 + 0.833333333e-1)
								/ v2);
			}
			c = Math.log(c * r * g * h);
		}
		// Computing integral
		// Given a row k, the procedure starts at the midpoint and works
		// outward (index j) in calculating the probability at nodes
		// symmetric about the midpoint. The rows (index k) are also
		// processed outwards symmetrically about the midpoint. The
		// centre row is unpaired.
		double gstep = g;
		final double[] qw = new double[30];
		final double[] vw = new double[30];
		qw[0] = -1;
		qw[jmax] = -1;
		double pk1 = 1;
		double pk2 = 1;
		for (int k = 1; k <= kmax; ++k) {
			gstep -= g;
			do {
				gstep = -gstep;
				final double gk = gmid + gstep;
				double pk = 0;
				if ((pk2 > pcutk) || (k <= kmin)) {
					final double w0 = c - gk * gk * 0.5;
					final double pz = APStat.alnorm(gk, true);
					double x = APStat.alnorm(gk - q, true) - pz;
					if (x > 0) {
						pk = Math.exp(w0 + r1 * Math.log(x));
					}
					if (v <= vmax) {
						int jump = -jmax;
						do {
							jump = jump + jmax;
							for (int j = 1; j <= jmax; ++j) {
								final int jj = j + jump;
								if (qw[jj - 1] <= 0) {
									final double hj = h * j;
									if (j < jmax) {
										qw[jj] = -1;
									}
									final double ehj = Math.exp(hj);
									qw[jj - 1] = q * ehj;
									vw[jj - 1] = v * (hj + 0.5 - ehj * ehj * 0.5);
								}
								double pj = 0;
								x = APStat.alnorm(gk - qw[jj - 1], true) - pz;
								if (x > 0) {
									pj = Math.exp(w0 + vw[jj - 1] + r1 * Math.log(x));
								}
								pk += pj;
								if (pj > pcutj) {
									continue;
								}
								if ((jj > jmin) || (k > kmin)) {
									break;
								}
							}
							h = -h;
						} while (h < 0);
					}
				}
				prtrng = prtrng + pk;
				if ((k > kmin) && (pk <= pcutk) && (pk1 <= pcutk)) {
					return prtrng;
				}
				pk2 = pk1;
				pk1 = pk;
			} while (gstep > 0);
		}
		return prtrng;
	}

	/**
	 * Algorithm AS 190.1 Appl Statist (1983) Vol.32, No.2. Approximates the quantile p for a studentized range distribution having v degrees
	 * of freedom and r samples for probability p, p.ge.0.90 .and. p.le.0.99. Uses functions alnorm, ppnd, prtrng and qtrng0 - Algorithms AS
	 * 66, AS 111, AS 190 and AS 190.2
	 * @param p P-value for which to find quantile
	 * @param v Degrees of freedom for distribution
	 * @param r Number of samples for distribution
	 * @return Quantile at p for given distribution
	 */
	public static double qtrng(final double p, final double v, final double r) {
		final int jmax = 8;
		final double pcut = 0.001;
		final double eps = 1e-4;
		// Check input parameters
		if (v < 1) {
			throw new IllegalArgumentException("Degrees of freedom must be >= 1.");
		}
		if (r < 2) {
			throw new IllegalArgumentException("Number of samples must be >= 2.");
		}
		if ((p < 0.9) || (p > 0.99)) {
			throw new IllegalArgumentException("P-value must be in range [0.9,0.99].");
		}
		// Obtain initial values
		double q1 = APStat.qtrng0(p, v, r);
		double p1 = APStat.prtrng(q1, v, r);
		double q2 = 0;
		double p2 = 0;
		double qtrng = q1;
		if (Math.abs(p1 - p) < pcut) {
			return qtrng;
		}
		if (p1 > p) {
			p1 = 1.75 * p - 0.75 * p1;
		}
		if (p1 < p) {
			p2 = p + (p - p1) * (1 - p) / (1 - p1) * 0.75;
		}
		if (p2 < 0.8) {
			p2 = 0.8;
		}
		if (p2 > 0.995) {
			p2 = 0.995;
		}
		q2 = APStat.qtrng0(p2, v, r);
		// Refine approximation
		double e1 = 0;
		double e2 = 0;
		double d = 0;
		for (int j = 2; j <= jmax; ++j) {
			p2 = APStat.prtrng(q2, v, r);
			e1 = p1 - p;
			e2 = p2 - p;
			qtrng = (q1 + q2) / 2;
			d = e2 - e1;
			if (Math.abs(d) > eps) {
				qtrng = (e2 * q1 - e1 * q2) / d;
			}
			if (Math.abs(e1) >= Math.abs(e2)) {
				q1 = q2;
				p1 = p2;
			}
			if (Math.abs(p1 - p) < pcut * 5) {
				return qtrng;
			}
			q2 = qtrng;
		}
		return qtrng;
	}

	/**
	 * Algorithm AS 190.2 Appl Statist (1983) Vol.32, No.2. Calculates an initial quantile p for a studentized range distribution having v
	 * degrees of freedom and r samples for probability p, p.gt.0.80 .and. p.lt.0.995. Uses function ppnd - Algorithm AS 111
	 * @param p P-value for which to find initial quantile
	 * @param v Degrees of freedom for distribution
	 * @param r Number of samples for distribution
	 * @return Initial quantile at p for given distribution
	 */
	public static double qtrng0(final double p, final double v, final double r) {
		final double vmax = 120;
		double t = APStat.ppnd(0.5 + 0.5 * p);
		if (v < vmax) {
			t += (t * t * t + t) / v / 4;
		}
		double q = 0.8843 - 0.2368 * t;
		if (v < vmax) {
			q += -1.214 / v + 1.208 * t / v;
		}
		return t * (q * Math.log(r - 1) + 1.4142);
	}
}
