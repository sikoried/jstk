package com.github.sikoried.jstk.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import com.github.sikoried.jstk.io.FrameInputStream;
import com.github.sikoried.jstk.io.FrameOutputStream;

public class MkInitFtFile {

	public static final String SYNOPSIS = 
		"Cat the first (num-frames) of each file in the list to stdout\n" +
		"usage: app.MkInitFtFile num-frames dir list";
	public static void main(String [] args) throws Exception {
		if (args.length != 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		int n = Integer.parseInt(args[0]);
		double [] x = null;
		FrameOutputStream fos = null;
		String ind = args[1];
		LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(args[2])));
		String l;
		while ((l = lnr.readLine()) != null) {
			FrameInputStream fis = new FrameInputStream(new File(ind + System.getProperty("file.separator") + l.trim()));
			if (x == null) {
				x = new double [fis.getFrameSize()];
				fos = new FrameOutputStream(fis.getFrameSize());
			} else if (x.length != fis.getFrameSize())
				throw new IOException(args[1] + ":" + lnr.getLineNumber() + " file has different framesize");
			
			int nn = 0; 
			while (fis.read(x) && nn++ < n)
				fos.write(x);
			fis.close();
		}
		fos.close();
	}

}
