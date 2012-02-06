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
package de.fau.cs.jstk.app;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.Configuration;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.arch.TreeNode;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch.Hypothesis;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.lm.Bigram;

public class Decoder {
	private static Logger logger = Logger.getLogger(Decoder.class);
	
	public static final String SYNOPSIS =
		"sikoried, 11/16/2010\n" +
		"Time-synchronuous beam search for network decoding.\n\n" +
		"usage: app.Decoder config codebook lmfile [options]\n" +
		"-l list [indir]\n" +
		"  Read files from list and optional indir.\n" +
		"-f file\n" +
		"  Work on given file.\n" +
		"-bs <beamsize>\n" +
		"  Set Viterbi beam size (default: 500)\n" +
		"-bw <beamwidth>\n" +
		"  Set the Viterbi beam width in terms of logprobs\n" + 
		"-i <insertion-penalty>\n" +
		"  Set the insertion penalty (default: 0.01)\n" +
		"-w <lm-weight>\n" +
		"  Set the language model weight (default: 10.)\n" +
		"-n <num>\n" +
		"  Set number of hypotheses to generate (default: 1).\n" +
		"-s prob\n" +
		"  Set the silence probability (default: 0.01).\n" +
		"-m [mode]\n" +
		"  Set the output mode; currently supported:\n" +
		"  word    : generate word sequence (default)\n" +
		"  token   : generate token sequence\n" +
		"  compact : generate compact trace\n" +
		"  detail  : generate detailed trace\n" +
		"  ma      : generate MetaAlignment (useful for wavesurfer)\n" +
		"-q\n" +
		"  Silence DebugOutput.\n" +
		"-o <file>\n" +
		"  Write out recognition output to given file\n";
	
	public static enum Mode {
		WORD,
		COMPACT,
		DETAIL,
		MA,
		TOKEN
	}
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		
		if (args.length < 5) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// scan for silencer
		boolean silencer = false;
		for (int i = 0; i < args.length; ++i)
			if (args[i].equals("-q")) {
				silencer = true;
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
			}
		
		double wip = 0.01;
		double lmwt = 10.;
		@SuppressWarnings("unused")
		float silprob = 0.05f;
		Mode mode = Mode.WORD;
		int n = 1;
		int bs = 500;
		double bw = Double.MAX_VALUE;
		String outf = null;
		List<String> files = new LinkedList<String>();
		
		// parse args
		int z = 0;
		
		// load the config
		Configuration conf = new Configuration(new File(args[z++]));
		conf.loadCodebook(new File(args[z++]));
		
		// load language model
		HashMap<Tokenization, Float> sil = new HashMap<Tokenization, Float>();
		// sil.put(conf.tok.getWordTokenization("pau"), silprob);
		// sil.put(conf.tok.getWordTokenization("h#"), silprob);
		Bigram lm = new Bigram(conf.tok, conf.th, sil);
		lm.loadSrilm(new File(args[z++]));
		
		for (; z < args.length; ++z) {
			if (args[z].equals("-f"))
				files.add(args[++z]);
			else if (args[z].equals("-l")) {
				BufferedReader br = new BufferedReader(new FileReader(args[++z]));
				String dir = null;
				if (args.length > z+1 && !args[z+1].startsWith("-"))
					dir = args[++z];
				String line;
				while ((line = br.readLine()) != null)
					files.add(dir == null ? line : dir + System.getProperty("file.separator") + line);
			} else if (args[z].equals("-i"))
				wip = Double.parseDouble(args[++z]);
			else if (args[z].equals("-w"))
				lmwt = Double.parseDouble(args[++z]);
			else if (args[z].equals("-m")) {
				String m = args[++z];
				if (m.equals("word"))
					mode = Mode.WORD;
				else if (m.equals("compact"))
					mode = Mode.COMPACT;
				else if (m.equals("detail"))
					mode = Mode.DETAIL;
				else if (m.equals("ma"))
					mode = Mode.MA;
				else if (m.equals("token"))
					mode = Mode.TOKEN;
				else
					throw new IOException("unsupported output mode");
			} else if (args[z].equals("-bs"))
				bs = Integer.parseInt(args[++z]);
			else if (args[z].equals("-bw"))
				bw = Double.parseDouble(args[++z]);
			else if (args[z].equals("-n"))
				n = Integer.parseInt(args[++z]);
			else if (args[z].equals("-q")) {
				silencer = true;
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
			}
			else if (args[z].equals("-o"))
				outf = args[++z];
			else if (args[z].equals("-s"))
				silprob = Float.parseFloat(args[++z]);
			else
				throw new Exception("unknown argument " + args[z]);
		}
		
		
		
		TreeNode root = lm.generateNetwork();
		
		// logger.info(TokenTree.traverseNetwork(root, " "));
		
		if (files.size() < 1) {
			System.err.println("Nothing to do. Bye.");
			System.exit(1);
		}
		
		ViterbiBeamSearch dec = new ViterbiBeamSearch(root, lmwt, wip);
		
		// set up the output stream
		BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(outf == null ? System.out : new FileOutputStream(outf)));
		
		for (String f : files) {
			logger.info("reading " + f);
			FrameInputStream fr = new FrameInputStream(new File(f));
			double [] buf = new double [fr.getFrameSize()];
			LinkedList<double []> obs = new LinkedList<double []>();
			
			while (fr.read(buf))
				obs.add(buf.clone());
			
			// init the decoder
			Iterator<double []> it = obs.iterator();
			dec.initialize(bs, bw, it.next());
	
			int i = 1;
			while (it.hasNext()) {
				double cbw = dec.step(it.next());
				i++;
				if (!silencer)
					System.err.print("\rprogress=" + (int)((i / (double) obs.size()) * 100) + "% bs=" + dec.getCurrentBeamSize() + " exp=" + dec.getCurrentExpandedSize() + " bw=" + cbw);
			}
			
			// conclude the decoding (and reduce to active final states)
			dec.conclude();
			
			logger.info("\n processed " + i + " frames");
						
			for (Hypothesis h : dec.getBestHypotheses(n)) {
				switch (mode) {
				case TOKEN:
					for (Hypothesis t : h.extractTokens())
						bwr.append(t.node.toString() + " ");
					bwr.append("\n");
					break;
				case WORD:
					for (Hypothesis w : h.extractWords())
						bwr.append(w.node.word.word + " ");
					bwr.append("\n");
					break;
				case COMPACT:
					bwr.append(h.toCompactString());
					bwr.append("\n");
					break;
				case DETAIL:
					bwr.append(h.toDetailedString());
					bwr.append("\n");
					break;
				case MA:
					h.toMetaAlignment(conf.th).write(bwr);
					break;
				}
			}

			bwr.flush();
		}
		
		bwr.close();
	}
}
