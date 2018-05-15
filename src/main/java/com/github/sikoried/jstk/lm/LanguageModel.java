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
package com.github.sikoried.jstk.lm;

import com.github.sikoried.jstk.arch.TokenHierarchy;
import com.github.sikoried.jstk.arch.Tokenization;
import com.github.sikoried.jstk.arch.Tokenizer;
import com.github.sikoried.jstk.arch.TreeNode;
import com.github.sikoried.jstk.exceptions.OutOfVocabularyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Up to now, the LanguageModel interface enforces only the generateNetwork 
 * function. This is intended for small sized vocabularies that can explicitly 
 * be modeled as a graph. For larger vocabularies or more complex grammars, 
 * refer to the WFST packages.
 *  
 * @author sikoried
 */
public interface LanguageModel {
	Logger logger = LogManager.getLogger(LanguageModel.class);

	/**
	 * Generate the LST network from the given TokenTrees and build silence
	 * models as requested
	 * @return Root node of the LST network
	 */
	public TreeNode generateNetwork();

	static LanguageModel loadNgramModel(File file, Tokenizer tok, TokenHierarchy th, HashMap<Tokenization, Float> sil) throws IOException, OutOfVocabularyException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String lin;

		logger.info("scanning for \\data\\ section...");

		// skip everything till \data\
		while ((lin = br.readLine()) != null) {
			if (lin.equals("\\data\\"))
				break;
		}

		int n = 0;
		while ((lin = br.readLine()).length() > 0)
			n++;

		logger.info("loading " + n + "-gram model");

		if (n == 1) {
			Unigram lm = new Unigram(tok, th, sil);
			lm.loadSrilm(br);
			logger.info(lm);
			return lm;
		} else if (n == 2) {
			Bigram lm = new Bigram(tok, th, sil);
			lm.loadSrilm(br);
			logger.info(lm);
			return lm;
		} else {
			logger.info("can't handle n>2");
			throw new IOException("unsupported format");
		}
	}
}
