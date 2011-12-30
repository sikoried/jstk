package de.fau.cs.jstk.agmt;

public class PeakProminence {
	public static double pp(double [] a) {
		int [] mp = de.fau.cs.jstk.util.Various.maxp(a, 1);
		return pp(a, mp[0]);
	}
	
	public static double pp(double [] a, int mpos) {
		double [] x = new double [a.length];
		for (int i = 0; i < a.length; ++i)
			x[i] = (double) i;
		
		double [] lp = fit(x, a, mpos);
		
		// compute the prominence, i.e., the distance between the regression
		// line and the actual value
		return Math.abs(a[mpos] - lp[0] + mpos*lp[1]);
	}
	
	/**
	 * Estimate the parameters of a straight line, ignoring the indicated value
	 * @param x
	 * @param y
	 * @param skip
	 * @return offset, slope
	 */
	public static double [] fit(double[] x, double[] y, int skip) {
		double s = 0.0, sx = 0.0, sy = 0.0, sxx = 0.0, sxy = 0.0, del;

		s = x.length;
		for (int i = 0; i < x.length; i++) {
			if (i == skip)
				continue;
			sx += x[i];
			sy += y[i];
			sxx += x[i] * x[i];
			sxy += x[i] * y[i];
		}

		del = s * sxx - sx * sx;

		double[] pout = new double[2];
		pout[0] = (sxx * sy - sx * sxy) / del;
		pout[1] = (s * sxy - sx * sy) / del;

		return pout;
	}
}
