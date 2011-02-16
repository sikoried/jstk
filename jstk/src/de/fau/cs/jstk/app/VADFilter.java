package de.fau.cs.jstk.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import de.fau.cs.jstk.framed.FrameSource;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;

public class VADFilter {

	public static final String SYNOPSIS = 
		"sikoried, 5/19/2010\n\n" +
		"Filter the given feature files using frame-wise (external) VAD decisions. The\n" +
		"VAD files are strict ASCII, consisting of bytes \"0\" for silence and \"1\" for\n" +
		"voice activity.\n\n" +
		"usage: app.VADFilter [options]\n" +
		"-f <ft-file> <vad-file> <out-file>\n" +
		"  Apply the filtering to an individual file set. Can be used multiple times\n" +
		"  but paths must be absolute.\n" +
		"-l <list-file1> [list-file2 ...]\n" +
		"  Use the given list(s) to perform the filtering. Lists contain a single\n" +
		"  file name per line, thus the directory options below MUST be set.\n" +
		"--iolist <list-file1> [list-file2 ...]\n" +
		"  Use the given list(s) to perform the filtering. Lists must contain lines\n" +
		"  consisting of the target features, VAD decision and (optional) output file.\n" +
		"  You may find the following directory options useful.\n" +
		"--dir-ft  <dir>\n" +
		"--dir-vad <dir>\n" +
		"--dir-out <dir>\n" +
		"  Specify the feature/VAD/output directory for the feature files referenced in\n" +
		"  the lists.\n";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println(SYNOPSIS);
			System.exit(0);
		}
		
		String dirFt  = null;
		String dirVad = null;
		String dirOut = null;
		
		LinkedList<String> lists = new LinkedList<String>();
		
		LinkedList<String> lfFt  = new LinkedList<String>();
		LinkedList<String> lfVad = new LinkedList<String>();
		LinkedList<String> lfOut = new LinkedList<String>();
		
		// parse arguments
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-f")) {
				String f1 = args[++i];
				String f2 = args[++i];
				String f3 = args[++i];
				
				if (!(new File(f1)).canRead())
					throw new IOException("VADFilter.main(): Could not read from " + f1);
				if (!(new File(f2)).canRead())
					throw new IOException("VADFilter.main(): Could not read from " + f2);
								
				lfFt.add(f1);
				lfVad.add(f2);
				lfOut.add(f3);
			} else if (args[i].equals("-l")) {
				while (i < args.length - 1 && !args[i+1].startsWith("-"))
					lists.add(args[++i]);
			} else if (args[i].equals("--dir-ft"))
				dirFt = args[++i];
			else if (args[i].equals("--dir-vad"))
				dirVad = args[++i];
			else if (args[i].equals("--dir-out")) {
				if (!(new File(args[i+1])).canWrite())
					throw new IOException("VADFilter.main(): Cannot write to directory " + args[i+1]);
				dirOut = args[++i];
			}
		}
		
		// read in the lists
		for (String lf : lists) {
			BufferedReader br = new BufferedReader(new FileReader(lf));
			String lbuf;
			while ((lbuf = br.readLine()) != null) {
				String [] split = lbuf.split("\\s+");
				String f1 = null;
				String f2 = null;
				String f3 = null;
				if (split.length == 3) {
					f1 = split[0];
					f2 = split[1];
					f3 = split[2];
				} else if (split.length == 1){
					if (dirFt == null || dirOut == null || dirVad == null)
						throw new IOException("VADFilter.main(): the -l option requires the --dir-X options to be set!");
					
					f1 = dirFt + System.getProperty("file.separator") + lbuf;
					f2 = dirVad + System.getProperty("file.separator") + lbuf;
					f3 = dirOut + System.getProperty("file.separator") + lbuf;
				} else
					throw new IOException("VADFilter.main(): invalid list format in file " + lf);
				
				if (!(new File(f1)).canRead())
					throw new IOException("VADFilter.main(): Could not read from " + f1);
				if (!(new File(f2)).canRead())
					throw new IOException("VADFilter.main(): Could not read from " + f2);
				
				lfFt.add(f1);
				lfVad.add(f2);
				lfOut.add(f3);
			}
		}
		
		if (lfFt.size() == 0) {
			System.err.println("VADFilter.main(): No input files. Bye.");
			System.exit(0);
		}
		
		while (lfFt.size() > 0) {
			// dequeue...
			String f1 = lfFt.remove();
			String f2 = lfVad.remove();
			String f3 = lfOut.remove();
			
			// init I/O
			FrameSource fs1 = new FrameInputStream(new File(f1));
			FrameOutputStream fw = new FrameOutputStream(fs1.getFrameSize(), new File(f3));
			
			// read in VAD
			BufferedReader br = new BufferedReader(new FileReader(f2));
			
			double [] buf1 = new double [fs1.getFrameSize()];
			char [] buf2 = new char [1];
			char last = 0;
			int ind = 0;
			while (fs1.read(buf1)) {
				int num = br.read(buf2);
				if (num != 1) {
					// file ended too early, so pad the last decision
					buf2[0] = last;
				}
				
				if (buf2[0] == '1') {
					fw.write(buf1);
					ind++;
				} else if (buf2[0] == '0') {
					ind++;
				} else
					throw new IOException("VADFilter.main(): Unexpected token '" + buf2[0] + "' position " + ind);
				
				last = buf2[0];
			}
			
			if (br.read(buf2) > 0)
				throw new IOException("VADFilter.main(): VAD file (" + f2 + ") was longer as " + f1);
			
			fw.close();
			
			((FrameInputStream) fs1).close();
			br.close();
		}
	}
}
