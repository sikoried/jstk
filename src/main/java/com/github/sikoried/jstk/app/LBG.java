package com.github.sikoried.jstk.app;

import java.io.File;
import java.util.List;

import com.github.sikoried.jstk.io.ChunkedDataSet;
import com.github.sikoried.jstk.stat.Initialization;
import com.github.sikoried.jstk.stat.Mixture;
import com.github.sikoried.jstk.stat.Sample;

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
