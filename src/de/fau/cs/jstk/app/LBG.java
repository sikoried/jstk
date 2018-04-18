package de.fau.cs.jstk.app;

import java.io.File;
import java.util.List;

import de.fau.cs.jstk.io.ChunkedDataSet;
import de.fau.cs.jstk.stat.Initialization;
import de.fau.cs.jstk.stat.Mixture;
import de.fau.cs.jstk.stat.Sample;

public class LBG {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.out.println("usage: app.LBG numc eps conv listfile mixture-out");
			return;
		}
		int numc = Integer.parseInt(args[0]);
		double eps = Double.parseDouble(args[1]);
		double conv = Double.parseDouble(args[2]);
		
		ChunkedDataSet cds = new ChunkedDataSet(new File(args[3]));
		List<Sample> li = cds.cachedData();		
		
		Mixture m = Initialization.lbg(li, numc, eps, conv, true);
		
		m.writeToFile(new File(args[4]));
	}

}
