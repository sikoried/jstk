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
package de.fau.cs.jstk.agmt;

import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.io.FrameReader;
import de.fau.cs.jstk.stat.Density;
import de.fau.cs.jstk.stat.Sample;
import de.fau.cs.jstk.stat.Trainer;

/**
 * Compute mue and sigma of a given random variable (double array).
 * 
 * @author sikoried
 */
public class MueSigma {
	public static String SYNOPSIS = 
		"usage: MueSigma data-file-1 <data-file-2 ...>";
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
		for (int i = 0; i < args.length; ++i) {
			try {
				List<Sample> samples = new LinkedList<Sample>();
				FrameReader fr = new FrameReader(new FileReader(args[i]));
				double [] buf = new double [fr.getFrameSize()];
				while (fr.read(buf))
					samples.add(new Sample((short) 0, buf));
				Density d = Trainer.ml(samples, true);
				System.out.print(args[i] + ": mue = [");
				for (double dd : d.mue)
					System.out.print(" " + dd);
				System.out.print(" ] sig = [");
				for (double dd : d.cov)
					System.out.print(" " + Math.sqrt(dd));
				System.out.println(" ]");
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		}
	}
}
