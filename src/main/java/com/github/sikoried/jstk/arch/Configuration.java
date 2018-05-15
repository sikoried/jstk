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
package com.github.sikoried.jstk.arch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.github.sikoried.jstk.arch.mf.CModelFactory;
import com.github.sikoried.jstk.arch.mf.SCModelFactory;
import com.github.sikoried.jstk.exceptions.CodebookException;
import com.github.sikoried.jstk.stat.Mixture;
import com.github.sikoried.jstk.stat.hmm.Hmm;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The Configuration loads, keeps and saves a decoding configuration. While the
 * TokenTree is always generated on the fly, Alphabet, Tokenizer and 
 * TokenHierarchy are loaded as available.
 * 
 * @author sikoried
 */
public final class Configuration {
	private static Logger logger = LogManager.getLogger(Configuration.class);
	
	/** tokens => states */
	public Alphabet a = null;
	
	/** word => token sequence */
	public Tokenizer tok = null;
	
	/** available tokens and their hierarchy */
	public TokenHierarchy th = null;
	
	/** stores available HMM models and respective emission models */
	public Codebook cb = null;
	
	/** compression of available words */
	public TokenTree tt = null;
	
	/**
	 * Allocate a new (empty) configuration
	 */
	public Configuration() {
		
	}
	
	/**
	 * Allocate a new Configuration and load the settings from the given XML
	 * file.
	 * @param config
	 * @throws IOException
	 */
	public Configuration(File config) throws IOException {
		loadConfiguration(config);
	}
	
	/** name of the XML root element */
	private static final String ROOT = "jstkconfig";
	
	/**
	 * Load a configuration from the given XML file.
	 * @param config
	 * @throws IOException
	 */
	public void loadConfiguration(File config) throws IOException {
		logger.info("loading configuration from " + config.getAbsolutePath());
		
		try {
			// prepare XML file
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			dbf.setIgnoringElementContentWhitespace(true);

			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new FileReader(config)));
			
			// read and validate root element
			Element root = doc.getDocumentElement();
			if (!root.getNodeName().equals(ROOT))
				throw new IOException("invalid root node in xml document");

			// try to load Alphabet
			{
				NodeList nl = root.getElementsByTagName("alphabet");
				if (nl.getLength() != 1)
					throw new IOException("no/multiple Alphabet in xml document");
				
				logger.info("found Alphabet section");
				a = new Alphabet();
				
				Node aroot = nl.item(0);
				Node al = aroot.getFirstChild();
				
				do {
					// we're only interested in the element nodes
					if (al.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					NamedNodeMap nnm = al.getAttributes();
					String name = nnm.getNamedItem("n").getNodeValue();
					String states = nnm.getNamedItem("s").getNodeValue();
					
					a.lookup.put(name, Short.parseShort(states));
				} while ((al = al.getNextSibling()) != null);
				
				if (a.lookup.size() == 0)
					throw new IOException("empty Alphabet section");
				
				logger.info(a.toString());
			}
			
			// try to load Tokenizer
			{
				NodeList nl = root.getElementsByTagName("tokenizer");
				if (nl.getLength() == 1) {
					logger.info("found Tokenizer section");
					
					// initialize Tokenizer
					tok = new Tokenizer(a);
					
					// to iterate, use the next sibling method (speed...)
					Node troot = nl.item(0);
					Node t = troot.getFirstChild();
					
					do {
						// we're only interested in the element nodes
						if (t.getNodeType() != Node.ELEMENT_NODE)
							continue;
						
						String word = t.getAttributes().getNamedItem("w").getNodeValue().trim();
						String trans = t.getAttributes().getNamedItem("t").getNodeValue().trim();
						
						tok.addTokenization(word, trans);
					} while ((t = t.getNextSibling()) != null);
					
					if (tok.tokenizations.size() == 0)
						throw new IOException("empty Tokenizer section");
					
					logger.info(tok.toString());
				} else
					logger.info("no or multiple Tokenizer sections in xml file");
			}
			
			// try to load TokenHierarchy
			{
				NodeList nl = root.getElementsByTagName("hierarchy");
				if (nl.getLength() == 1) {
					logger.info("found TokenHierarchy section");
					th = new TokenHierarchy();
					
					Node hroot = nl.item(0);
					
					List<Pair<Node, Token>> agenda = new LinkedList<Pair<Node, Token>>();
					
					// add all root Tokens
					Node rt = hroot.getFirstChild();
					if (rt == null)
						throw new IOException("empty TokenHierarchy");
					
					while (rt != null) {
						if (rt.getNodeType() == Node.ELEMENT_NODE)
							agenda.add(Pair.of(rt, null));
						rt = rt.getNextSibling();
					}
										
					// depth first search
					while (agenda.size() > 0) {
						Pair<Node, Token> p = agenda.remove(agenda.size() - 1);
						
						// extract values and create Token
						NamedNodeMap nnm = p.getLeft().getAttributes();
						String name = nnm.getNamedItem("n").getNodeValue();
						
						String [] left = new String [0];
						Node leftc = nnm.getNamedItem("l");
						if (leftc != null) {
							String help = leftc.getNodeValue().trim();
							if (help.length() > 0)
								left = help.split("\\s+");
						}
						
						String [] right = new String [0];
						Node rightc = nnm.getNamedItem("r");
						if (rightc != null) {
							String help = rightc.getNodeValue().trim();
							if (help.length() > 0)
								right = help.split("\\s+");
						}
						
						Token tk = new Token(left, name, right);
											
						// add to global list
						th.tokens.put(tk.uniqueIdentifier(), tk);
						if (p.getRight() == null)
							th.rtokens.put(tk.token, tk);
						else
							p.getRight().insertChild(-1, tk);
						
						// check for attached HMM
						Node hmmidc = nnm.getNamedItem("h");
						if (hmmidc != null)
							tk.hmmId = Integer.parseInt(hmmidc.getNodeValue());
												
						// add children, if any
						Node c = p.getLeft().getFirstChild();
						while (c != null) {
							if (c.getNodeType() == Node.ELEMENT_NODE)
								agenda.add(Pair.of(c, tk));
							c = c.getNextSibling();
						}
					}
					
					if (th.tokens.size() == 0)
						throw new IOException("no Tokens in hierarchy");
					
					logger.info(th.toString());
				} else
					logger.info("no or multiple TokenHierarchy sections in xml file");
			}
		} catch (SAXException e) {
			throw new IOException(e.toString());
		} catch (ParserConfigurationException e) {
			throw new IOException(e.toString());
		} 
	}
	
	/**
	 * Save the current configuration to the given file. Note that the Codebook
	 * is NOT written.
	 * @param config
	 * @throws IOException
	 */
	public void saveConfiguration(File config) throws IOException {
		logger.info("saving current configuration to " + config.getAbsolutePath());
		
		// we need at least an alphabet to write!
		if (a == null) {
			logger.warn("no alphabet present -- no write!");
			return;
		}
		
		try {
			// initialize XML output
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			// create the root element and add it to the document
			Element root = doc.createElement(ROOT);
			doc.appendChild(root);

			// add a comment on when we wrote this configuration (solely for
			// debugging)
			Comment comment = doc.createComment("configuration generated at "
					+ java.util.Calendar.getInstance().getTime());
			root.appendChild(comment);

			// write alphabet
			if (a != null) {
				Element aroot = doc.createElement("alphabet");
				
				for (Entry<String, Short> e : a.lookup.entrySet()) {
					Element entry = doc.createElement("token");
					entry.setAttribute("n", e.getKey());
					entry.setAttribute("s", e.getValue().toString());
					aroot.appendChild(entry);
				}
				
				root.appendChild(aroot);
			}

			// write tokenizer
			if (tok != null) {
				Element troot = doc.createElement("tokenizer");
				
				for (Tokenization e : tok.tokenizations) {
					Element el = doc.createElement("tokenization");
					el.setAttribute("w", e.word);
					el.setAttribute("t", join(e.sequence, " "));
					troot.appendChild(el);
				}
				
				root.appendChild(troot);
			}
			
			// write token hierarchy
			if (th != null) {
				Element hroot = doc.createElement("hierarchy");
				
				// for all root tokens...
				for (Token r : th.rtokens.values()) {
					// depth first search
					List<Pair<Token, Element>> agenda = new LinkedList<Pair<Token, Element>>();
					agenda.add(Pair.of(r, hroot));

					// depth first search
					while (agenda.size() > 0) {
						Pair<Token, Element> p = agenda.remove(agenda.size() - 1);

						// prepare new element
						Element el = doc.createElement("token");
						el.setAttribute("n", p.getLeft().token);
						el.setAttribute("l", join(p.getLeft().left, " "));
						el.setAttribute("r", join(p.getLeft().right, " "));
						if (p.getLeft().hmm != null)
							el.setAttribute("h", Integer.toString(p.getLeft().hmmId));
						p.getRight().appendChild(el);
						
						// add the new element
						p.getRight().appendChild(el);
						
						// add the children to the agenda
						for (Token c : p.getLeft().moreContext)
							agenda.add(Pair.of(c, el));
					}
				}
				
				root.appendChild(hroot);
			}
					
			// write out the XML
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			
			// write it to the given file
			BufferedWriter bw = new BufferedWriter(new FileWriter(config));
			StreamResult result = new StreamResult(bw);
			DOMSource source = new DOMSource(doc);
			
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.transform(source, result);
			
			bw.flush();
			bw.close();
		} catch (ParserConfigurationException e) {
			throw new IOException(e.toString());
		} catch (TransformerConfigurationException e) {
			throw new IOException(e.toString());
		} catch (TransformerException e) {
			throw new IOException(e.toString());
		}
	}
	
	/**
	 * Similar to Array.toString, but without the [] and a specified glue.
	 * @param array
	 * @param glue
	 * @return
	 */
	private static String join(String [] array, String glue) {
		if (array == null || array.length == 0)
			return "";
		else if (array.length == 1)
			return array[0];
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length - 1; ++i)
			sb.append(array[i] + glue);
		
		sb.append(array[array.length - 1]);
		
		return sb.toString();
	}
	
	/**
	 * Load the alphabet from the given file in ASCII format.
	 * @param file
	 * @throws IOException
	 */
	public void loadAlphabet(File file) throws IOException {
		logger.info("loading Alphabet from " + file.getAbsolutePath());
		a = new Alphabet(file);
		logger.info(a.describe());
	}
	
	/**
	 * Load a tokenizer from the given file in ASCII format. Requires an already
	 * loaded Alphabet!
	 * @param file
	 * @throws IOException
	 */
	public void loadTokenizer(File file) throws IOException {
		if (a == null)
			throw new NullPointerException("No alphabet loaded.");
		
		logger.info("loading Tokenizer from " + file.getAbsolutePath());
		tok = new Tokenizer(a, file);
		logger.info(tok.describe());

	}
	
	/**
	 * Load a codebook from the given binary file and attach to the TokenHierarchy,
	 * if present.
	 * @param file
	 * @throws CodebookException
	 * @throws IOException
	 */
	public void loadCodebook(File file) throws CodebookException, IOException {
		logger.info("loading Codebook from " + file.getAbsolutePath());
		cb = new Codebook(file);
		
		if (th != null) {
			logger.info("attaching models to TokenHierarchy");
			cb.attachModels(th);
		}
	}
	
	/**
	 * Valid Alphabet present?
	 * @return
	 */
	public boolean hasAlphabet() {
		return a != null;
	}
	
	/**
	 * Valid Tokenizer present?
	 * @return
	 */
	public boolean hasTokenizer() {
		return tok != null;
	}
	
	/**
	 * Valid TokenHierarchy present?
	 * @return
	 */
	public boolean hasTokenHierarchy() {
		return th != null;
	}
	
	/**
	 * Valid Codebook loaded?
	 * @return
	 */
	public boolean hasCodebook() {
		return cb != null;
	}
	
	public boolean hasTokenTree() {
		return tt != null;
	}
	
	public static final String SYNOPSIS =
		"sikoried, 2/3/2011\n" +
		"Compile, load and manipulate JSTK configurations.\n" +
		"usage: arch.Configuration [options]\n" +
		"  --compile alphabet tokenization [max-context=0]\n" +
		"    Compile a new configuration based on the given Alphabet and Tokenization.\n" +
		"    If desired, a max-context for extracted Token may be specified.\n" +
		"  --read file\n" +
		"    Load configuration from given file in XML format.\n" +
		"  --write file [codebookfile]\n" +
		"    Save configuration at program termination.\n" +
		"\n" +
		"  --reduce size\n" +
		"    Reduce the Token context size to the given (max) size.\n" +
		"  --prune min-occ file\n" +
		"    Prune Tokens that do not appear at least min-occ times in the given (text)\n" +
		"    training file\n" +
		"\n" +
		"  --discrete \"alphabet\"\n" +
		"    Initialize the Token model states with discrete emission probabilities on\n" +
		"    the given Alphabet (floating point numbers) and write it to out-file.\n" +
		"\n" +
		"  --semi mixture\n" +
		"    Initialize the Token model states with semi-continuous emission models using\n" +
		"    the given Mixture density and write it to out-file.\n" +
		"  --list-shared\n" +
		"    List the available shared Mixtures (compact).\n" +
		"  --extract-shared id file\n" +
		"  --replace-shared id file\n" +
		"    Extract or replace a shared Mixture by ID\n" +
		"\n" +
		"  --make-phone-conf codebook\n" +
		"    Produce a phone recognizer type configuration.";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		Configuration conf = null;
		
		// process arguments in order
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--compile")) {
				// compile new configuration
				conf = new Configuration();
				conf.loadAlphabet(new File(args[++i]));
				conf.loadTokenizer(new File(args[++i]));
				
				// allocate new TokenHierarchy
				conf.th = new TokenHierarchy();
				if ((i+1) < args.length && !args[i+1].startsWith("--"))
					conf.th.addTokensFromTokenizer(conf.tok, Integer.parseInt(args[++i]));
				else
					conf.th.addTokensFromTokenizer(conf.tok, 0);
				
			} else if (args[i].equals("--read")) {
				conf = new Configuration(new File(args[++i]));
				
				if ((i+1) <  args.length && !args[i+1].startsWith("--"))
					conf.loadCodebook(new File(args[++i]));
				
			} else if (args[i].equals("--write")) {
				if (conf == null)
					throw new Exception("No Configuration loaded!");
				
				conf.saveConfiguration(new File(args[++i]));
				
				if ((i+1) <  args.length && !args[i+1].startsWith("--"))
					conf.cb.write(new File(args[++i]));
			} else if (args[i].equals("--reduce")) {
				if (conf == null)
					throw new Exception("No Configuration loaded!");
				if (!conf.hasTokenHierarchy())
					throw new Exception("Configuration does not have TokenHierarchy");
				
				conf.th.reduceContext(Integer.parseInt(args[++i]));
			} else if (args[i].equals("--prune")) {
				if (conf == null)
					throw new Exception("No Configuration loaded!");
				if (!conf.hasTokenHierarchy())
					throw new Exception("Configuration does not have TokenHierarchy");
				
				int mc = Integer.parseInt(args[++i]);
				String pf = args[++i];
				
				conf.th.pruneHierarchyByOccurrence(mc, conf.tok, new File(pf));
			} else if (args[i].equals("--discrete")) {
				// TODO discrete initialization
			} else if (args[i].equals("--semi")) {
				// semi-continuous initialization
				if (conf == null)
					throw new Exception("No Configuration loaded!");
				if (!conf.hasTokenHierarchy())
					throw new Exception("Configuration does not have TokenHierarchy");
				
				logger.info("loading Mixture from " + args[i+1]);
				Mixture mixture = new Mixture(new FileInputStream(args[++i]));
				
				conf.cb = new Codebook();
				conf.cb.initializeModels(conf.th, new SCModelFactory(conf.a, Hmm.Topology.LINEAR, mixture));
			} else if (args[i].equals("--cont")) {
				logger.info("loading Mixture from " + args[i+1]);
				Mixture mixture = new Mixture(new FileInputStream(args[++i]));
				
				conf.cb = new Codebook();
				conf.cb.initializeModels(conf.th, new CModelFactory(conf.a, Hmm.Topology.LINEAR, mixture));
			} else if (args[i].equals("--cont0")) {
				logger.info("initializing empty density fd=" + args[i+1] + " nd=" + args[i+2]);
				Mixture mixture = new Mixture(Integer.parseInt(args[i+1]), Integer.parseInt(args[i+2]), true);

				conf.cb = new Codebook();
				conf.cb.initializeModels(conf.th, new CModelFactory(conf.a, Hmm.Topology.LINEAR, mixture));
				i += 2;
			} else if (args[i].equals("--dump")) {
				if (conf.hasAlphabet())
					conf.a.dump(System.out);
				if (conf.hasTokenizer())
					conf.tok.dump(System.out);
				if (conf.hasTokenHierarchy())
					conf.th.dump(System.out);
				if (conf.hasCodebook())
					conf.cb.dump(new BufferedWriter(new OutputStreamWriter(System.out)));
			} else if (args[i].equals("--list-shared")) {
				if (!conf.hasCodebook())
					throw new Exception("No Configuration with valid Codebook loaded");
				
				for (Mixture m : conf.cb.getSharedMixtures())
					System.out.println(m.id + " : " + m.info());
			} else if (args[i].equals("--extract-shared")) {
				if (!conf.hasCodebook())
					throw new Exception("No Configuration with valid Codebook loaded");
				
				int id = Integer.parseInt(args[++i]);
				for (Mixture m : conf.cb.getSharedMixtures()) {
					if (m.id == id) {
						m.writeToFile(new File(args[++i]));
						break;
					}
				}
			} else if (args[i].equals("--replace-shared")) {
				if (!conf.hasCodebook())
					throw new Exception("No Configuration with valid Codebook loaded");
				
				int id = Integer.parseInt(args[++i]);
				Mixture m = new Mixture(new FileInputStream(args[++i]));
				m.id = id;
				conf.cb.replaceSharedMixture(m);
			} else if (args[i].equals("--make-phone-conf")) {
				
				String fcbin = args[++i];
						
				conf.loadCodebook(new File(fcbin));
				
				conf.th.reduceContext(0);
				conf.tok.tokenizations.clear();
				for (Token t : conf.th.rtokens.values())
					conf.tok.addTokenization(t.token, t.token);
				conf.tok.sortTokenizations();
			} else
				throw new Exception("unknown parameter " + args[i]);
		}
	}
}
