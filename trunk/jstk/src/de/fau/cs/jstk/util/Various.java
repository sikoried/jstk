/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet

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
package de.fau.cs.jstk.util;

import java.io.IOException;
import java.io.PrintStream;

public class Various {
	/**
	 * Analyze a confusion matrix and print the results on the referenced PrintStream
	 * @param result number of events [reference][classified]
	 * @param os Output PrintStream to write results to
	 */
	public static void analyzeConfusionMatrix(int [][] result, PrintStream os) throws IOException {
		os.println("confusion matrix:");

		int max = 0;
		for (int [] dim1 : result)
			for (int dim2 : dim1)
				if (dim2 > max) max = dim2;
		int fieldSize = (int)(Math.floor(Math.log10(max)) + 2.);
		
		for (int i = 0; i < fieldSize; ++i)
			os.print(" ");
		
		for (int i = 0; i < result.length; ++i)
			os.printf("%" + fieldSize + "s ", i);
		os.println();
		
		for (int r1 = 0; r1 < result.length; ++r1) {
			os.printf("%" + fieldSize + "s ", r1);
			for (int r2 = 0; r2 < result.length; ++r2)
				os.printf("%" + fieldSize + "s ", result[r1][r2]);
			os.println();
		}
		
		os.println("class recall precision f-measure");
		double rg = 0, pg = 0;
		for (int i = 0; i < result.length; ++i) {
			int recall = 0;
			int precision = 0;
			for (int j = 0; j < result.length; ++j) {
				recall += result[i][j];
				precision += result[j][i];
			}
			
			double r = (double) result[i][i] / recall;
			double p = (double) result[i][i] / precision;
			double f = 2. * r * p / (p + r);
			
			rg += r;
			pg += p;
			
			os.printf("%" + (int)(Math.floor(Math.log10(result.length)) + 2.) + "s %.2f %.2f %.2f\n", i, r, p, f);
		}
		
		rg /= result.length;
		pg /= result.length;
		
		os.printf("averg %.2f %.2f %.2f\n", rg, pg, 2. * rg * pg / (pg + rg));
		os.flush();
	}
}
