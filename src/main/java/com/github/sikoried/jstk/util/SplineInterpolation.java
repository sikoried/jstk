/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Stefan Steidl

	This file is part of the Java Speech Toolkit (JSTK).

	The JSTK is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	The JSTK is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/
package com.github.sikoried.jstk.util;

import com.github.sikoried.jstk.vc.F0Point;


public class SplineInterpolation {

	private F0Point[] points;
	private double[] h;
	private double[] k;

	public SplineInterpolation(F0Point[] points) {
		int n = points.length - 1;
		double[] e = new double[n];
		double[] u = new double[n];
		double[] r = new double[n];
		h = new double[n];
		k = new double[n + 1];
		this.points = points;

		for (int i = 0; i < n; i++) {
			h[i] = points[i + 1].frame - points[i].frame;
			e[i] = 6.0 / h[i] * (points[i + 1].f0 - points[i].f0);
		}

		if (n > 1) {
			u[1] = 2 * (h[0] + h[1]);
			r[1] = e[1] - e[0];
			for (int i = 2; i < n; i++) {
				u[i] = 2 * (h[i] + h[i - 1]) - Math.pow(h[i - 1], 2) / u[i - 1];
				r[i] = (e[i] - e[i - 1]) - r[i - 1] * h[i - 1] / u[i - 1];
			}
		}

		k[n] = 0;
		k[0] = 0;
		for (int i = n - 1; i > 0; i--) {
			k[i] = (r[i] - h[i] * k[i + 1]) / u[i];
		}
	}

	public double getValueAt(double x) {
		double a, b, c, d, h1, h2, h3;
		int n = points.length - 1;
		int i;

		for (i = 0; i < n; i++) {
			if ((x >= points[i].frame) && (x < points[i + 1].frame)) {
				break;
			}
		}

		a = (k[i + 1] - k[i]) / (6 * h[i]);
		b = k[i] / 2;
		c = (points[i + 1].f0 - points[i].f0) / h[i] - h[i] / 6
				* (2 * k[i] + k[i + 1]);
		d = points[i].f0;

		h1 = x - points[i].frame;
		h2 = h1 * h1;
		h3 = h1 * h2;
		return a * h3 + b * h2 + c * h1 + d;
	}
}
