/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Florian Hoenig
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

package com.github.sikoried.jstk.segmented;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;

import com.github.sikoried.jstk.exceptions.MalformedParameterStringException;
import com.github.sikoried.jstk.framed.FFT;
import com.github.sikoried.jstk.framed.FilterBank;
import com.github.sikoried.jstk.framed.SpectralTransformation;
import com.github.sikoried.jstk.framed.Window;
import com.github.sikoried.jstk.framed.filters.MedianFilter;
import com.github.sikoried.jstk.io.FrameOutputStream;
import com.github.sikoried.jstk.io.FrameSource;
import com.github.sikoried.jstk.sampled.AudioCapture;
import com.github.sikoried.jstk.sampled.AudioFileReader;
import com.github.sikoried.jstk.sampled.AudioSource;
import com.github.sikoried.jstk.sampled.RawAudioFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Syllable Nuclei Detector. Detects the Syllable Nuclei
 * 
 * 
 */

public class SyllableNucleiDetector implements FrameSource {
	private static final double FEX_VERSION = 0.1;

	private FrameSource median;
	private static Logger logger = LogManager.getLogger(SyllableNucleiDetector.class);

	private int index_offset = 0;
	private double[] buf = null;
	private SyllableNucleus[] nuclei = null;
	private int active_nuclei_index = 0;
	private LinkedList<double[]> cache = null;
	private LinkedList<SyllableNucleus> nuclei_cache = null;
	private SyllableNucleus[] current_nuclei = null;
	private SyllableNucleus last = null;
	private double significance_threshold = 0.0;
	private double local_threshold_factor = 0.0;
	private double soft_local_threshold_factor = 0.0;
	private int min_width = 0;
	private int min_distance = 0;
	private int minima_maxima_context = 0;

	protected RawAudioFormat format = new RawAudioFormat();
	protected AudioSource asource = null;
	protected FrameSource output = null;
	protected FrameSource filter = null;
	protected Window window = null;
	protected SpectralTransformation fft = null;

	protected int framesize = 0;

	private double frames_to_milliseconds = 0.;
	/* what's this? real_*time*_mode? is it used? */
	private boolean real_mode = true;
	private boolean with_absolute_syllable_data = false;
	private boolean without_duration = false;
	private boolean without_loudness = false;
	private boolean with_positions = false;
	private boolean endOfInput = false;

	public static boolean outputLAB = false;
	private PrintStream labout = null;

	/** 16kHz, 16bit, signed, little endian, linear */
	public static String DEFAULT_AUDIO_FORMAT = "t:ssg/16";

	/** Hamming window of 16ms, 10ms shift */
	public static String DEFAULT_WINDOW = "hamm,12,12";

	/** Filter bank 188Hz-6071Hz, 226.79982mel band width, 50% filter overlap */
	public static String DEFAULT_FILTER = "lin_rect:100,300,300,2300,2300,5000";

	/** Median-Filter with length 5 */
	public static int DEFAULT_MEDIAN_LENGTH = 5;

	/** Minimum width of a syllable nucleus */
	public static int DEFAULT_MIN_WIDTH = 5;

	/** Minimum distance between two syllable nuclei */
	public static int DEFAULT_MIN_DISTANCE = 3;

	/** Context when verifying a minimum/maximum */
	public static int DEFAULT_MIN_MAX_CONTEXT = 1;

	/**
	 * initializes the audio source
	 * 
	 * @param inFile
	 *            input file to read data from. "-": use default mic,
	 *            "mixer:<mixer-name>": use specific mixer
	 * @param parameterString
	 *            String representation of the options to use:
	 *            f:<path-to-file-with-header> -> load audio format from file;
	 *            t:<template-name> -> use existing template
	 *            [ssg,alaw,ulaw]/[8,16];
	 *            r:<bit-rate>,<sample-rate>,<signed(0,1)>,<little-endian(0,1)>
	 *            -> specify raw format directly;
	 */
	private void initializeAudio(String inFile, String parameterString)
			throws Exception {
		if (parameterString != null)
			format = RawAudioFormat.create(parameterString);

		if (inFile == null || inFile.equals("-"))
			asource = new AudioCapture(format.getBitRate(), format
					.getSampleRate());
		else if (inFile.startsWith("mixer:"))
			asource = new AudioCapture(inFile.substring(6),
					(inFile.length() == 6), format.getBitRate(), format
							.getSampleRate(), 0.2);
		else
			asource = new AudioFileReader(inFile, format, true);
	}

	/**
	 * initializes the window function on the audio source
	 * 
	 * @param parameterString
	 *            <hamm|hann|rect>,<length-ms>,<shift-ms>: window function
	 *            (Hamming, Hanning, Rectangular), length of window and shift
	 *            time in milliseconds
	 */
	protected void initializeWindow(String parameterString) throws Exception {
		window = Window.create(asource, parameterString);
		output = window;
	}

	/**
	 * initializes the FFT
	 * 
	 * @param pad
	 *            pad to the next power of 2
	 * @param normalize
	 *            normalize spectral energy to 1
	 */
	protected void initializeFFT(boolean pad, boolean normalize) {
		fft = new FFT(output, pad, normalize);
		output = fft;
	}

	/**
	 * initializes the filterbank
	 * 
	 * @param parameterString
	 *            string representation of the filterbank to use:
	 *            [<mel|lin_tri|lin_rect>:]<filter_spec>; first part defaults to
	 *            "mel" if omitted. Specify the type of filterbank to use
	 *            (Mel-filter, linear with triangular filters, linear with
	 *            rectangular filters), followed by a type-specific spec: mel:
	 *            <start-freq>,<end-freq>,<bandwidth(mel)>,<overlap if < 1.0,
	 *            min num of filters otherweise>; lin_{tri,rect}:
	 *            <start-freq>,<end
	 *            -freq>[[,<start-freq>,<end-freq>]*|_<bandwidth
	 *            >|:<numFilters>]: either specify each filter directly or a
	 *            frequency range for non-overlapping filters of either
	 *            <bandwidth> width or a bandwidth that results in exactly
	 *            <numFilters> non-overlapping filters in the range.
	 * @param doLog
	 *            apply logarithm on filters. Ignored if parameterString
	 *            specifies a mel filter (always logarithmized).
	 */
	protected void initializeFilterBank(String parameterString, boolean doLog)
			throws Exception {
		String type = "mel";
		if (parameterString.indexOf(':') > 0) {
			type = parameterString.substring(0, parameterString.indexOf(':'));
			parameterString = parameterString.substring(parameterString
					.indexOf(':') + 1);
		}
		if ("mel".equals(type)) {
			filter = FilterBank.generateMelFilterBank(fft, parameterString);
			output = filter;
			return;
		} else if (type.startsWith("lin_")) {
			String shape = type.substring("lin_".length());
			FilterBank.FilterType filter_type = FilterBank.FilterType.RECTANGULAR;
			if (shape.equals("rect")) {
				filter_type = FilterBank.FilterType.RECTANGULAR;
			} else if (shape.equals("tri")) {
				filter_type = FilterBank.FilterType.TRIANGULAR;
			} else {
				throw new MalformedParameterStringException(
						"initializeFilterBank: FilterBank-Description contains unrecognized shape for linear filter.");
			}
			if (parameterString.indexOf('_') > 0) {
				String bandWidthString = parameterString
						.substring(parameterString.indexOf('_') + 1);
				double bandWidth = Double.parseDouble(bandWidthString);
				parameterString = parameterString.substring(0, parameterString
						.indexOf('_'));
				String[] freqs = parameterString.split(",");
				if (freqs.length != 2) {
					throw new MalformedParameterStringException(
							"initializeFilterBank: FilterBank-Description contains bandwith-specification and != 2 frequencies");
				}
				double startFreq = Double.parseDouble(freqs[0]);
				double endFreq = Double.parseDouble(freqs[1]);
				if (endFreq - startFreq <= bandWidth) {
					throw new MalformedParameterStringException(
							"initializeFilterBank: FilterBank-Description specifies bandwidth bigger than end-freq - start-freq!");
				}
				FilterBank.Filter[] filters = new FilterBank.Filter[(int) Math
						.ceil(((endFreq - startFreq - bandWidth) / bandWidth) + 1)];
				double current_startfreq = startFreq;
				int filter_cnt = 0;
				while (current_startfreq != endFreq) {
					double current_end = (current_startfreq + bandWidth > endFreq ? endFreq
							: current_startfreq + bandWidth);
					filters[filter_cnt] = FilterBank.generateFreqFilter(fft,
							current_startfreq, current_end, filter_type, doLog);
					filter_cnt++;
					current_startfreq = current_end;
				}
				filter = new FilterBank(fft, filters);
				output = filter;
			} else if (parameterString.indexOf(':') > 0) {
				String numFiltersString = parameterString
						.substring(parameterString.indexOf(':') + 1);
				int numFilters = Integer.parseInt(numFiltersString);
				parameterString = parameterString.substring(0, parameterString
						.indexOf(':'));
				String[] freqs = parameterString.split(",");
				if (freqs.length != 2) {
					throw new MalformedParameterStringException(
							"initializeFilterBank: FilterBank-Description contains number of filters and != 2 frequencies");
				}
				double startFreq = Double.parseDouble(freqs[0]);
				double endFreq = Double.parseDouble(freqs[1]);
				double bandWidth = (endFreq - startFreq) / numFilters;
				FilterBank.Filter[] filters = new FilterBank.Filter[numFilters];
				double current_start = startFreq;
				for (int i = 0; i < numFilters; i++) {
					double current_end = ((current_start + bandWidth > endFreq) ? endFreq
							: current_start + bandWidth);
					filters[i] = FilterBank.generateFreqFilter(fft,
							current_start, current_end, filter_type, doLog);
					current_start = current_end;
				}
				filter = new FilterBank(fft, filters);
				output = filter;
			} else {
				String[] freqs = parameterString.split(",");
				if (freqs.length % 2 != 0) {
					throw new MalformedParameterStringException(
							"initializeFilterBank: FilterBank-description specifies odd number of frequencies as startFreq-endFreq-pairs.");
				}
				FilterBank.Filter[] filters = new FilterBank.Filter[freqs.length / 2];
				for (int i = 0; i < freqs.length; i += 2) {
					double current_start = Double.parseDouble(freqs[i]);
					double current_end = Double.parseDouble(freqs[i + 1]);
					filters[i / 2] = FilterBank.generateFreqFilter(fft,
							current_start, current_end, filter_type, doLog);
				}
				filter = new FilterBank(fft, filters);
				output = filter;
			}
		} else {
			throw new MalformedParameterStringException(
					"initializeFilterBank: FilterBank-Description unparseable!");
		}
	}

	/**
	 * Constructor
	 * 
	 * @param inFile
	 *            The file to read audio data from
	 * @param pAudio
	 *            Audio parameter string
	 * @param pWindow
	 *            Window parameter string
	 * @param pFilter
	 *            Filter parameter string
	 * @param medianLength
	 *            Length of the median to apply (== 2 * context + 1)
	 * @param significance_threshold
	 *            Smallest value of a possible maximum to be treated as one
	 * @param local_threshold_factor
	 *            Factor determining the threshold for minima per maximum
	 *            (minimum < (1-local_threshold_factor) * maximum must be true)
	 * @param soft_local_threshold_factor
	 *            Softer local_threshold_factor for detecting too long nuclei
	 * @param min_width
	 *            Minimum width of a syllable nucleus in frames
	 * @param min_distance
	 *            Minimum distance of two neighboring syllable nuclei in frames
	 * @param minima_maxima_context
	 *            Context to use when testing a frame's value against being a
	 *            maximum/minimum
	 * 
	 *            buf[0] = syllable nuclei frequency (always at index 0) * @param
	 *            with_positions: add position (in milliseconds) of left
	 *            syllable * border, left syllable nucleus border, syllable
	 *            nucleus maximum, right syllable nucleus border, right syllable
	 *            border right after the frequency buf++ = pos of left syl
	 *            border buf++ = pos of left syl nuc border buf++ = syll nuc
	 *            maximum buf++ = right sym nuc border buf++ = right syll border
	 * 
	 * @param with_absolute_syllable_data
	 *            Add absolute data of each syllable nucleus to the feature
	 *            frame. if true, each feature frame begins with buf++ =
	 *            absolute length of syllable nucleus in ms buf++ = maximum
	 *            energy of syllable nucleus buf++ = energy integral syl nucleus
	 * @param without_duration
	 *            If true, omit duration feature (next in line after absolute
	 *            syllable data per feature frame buf++ = duration
	 * @param without_loudness
	 *            If true, omit loudness features (maximum energy and sum of all
	 *            energies per syllable nucleus, related to neighboring nuclei,
	 *            next in line after duration feature per feature frame) buf++ =
	 *            maximum energy buf++ = sum of all energies per syllable
	 *            nucleus
	 */
	public SyllableNucleiDetector(String inFile, String pAudio, String pWindow,
			String pFilter, int medianLength, double significance_threshold,
			double local_threshold_factor, double soft_local_threshold_factor,
			int min_width, int min_distance, int minima_maxima_context,
			boolean with_absolute_syllable_data, boolean without_duration,
			boolean without_loudness, boolean with_positions) throws Exception {
		this.significance_threshold = significance_threshold;
		this.local_threshold_factor = local_threshold_factor;
		this.soft_local_threshold_factor = soft_local_threshold_factor;
		this.min_width = min_width;
		this.min_distance = min_distance;
		this.minima_maxima_context = minima_maxima_context;
		this.with_absolute_syllable_data = with_absolute_syllable_data;
		this.without_duration = without_duration;
		this.without_loudness = without_loudness;
		this.with_positions = with_positions;

		String inFileBaseName = new File(inFile).getName();

		if (outputLAB) {
			labout = new PrintStream(new File(inFileBaseName + ".lab"));
		}

		// get audio data
		logger.info("Initializing audio...");
		this.initializeAudio(inFile, pAudio);
		logger.info("done");
		logger.info("initializing windowing...");
		// window the data to get frames
		initializeWindow(pWindow);
		frames_to_milliseconds = 1000.0 / window.getNumberOfFramesPerSecond();
		logger.info("done.");
		// get the power spectrum
		logger.info("Initializing power spectrum...");
		initializeFFT(true, false);
		logger.info("done");
		// apply bandpass-filtering
		logger.info("Initializing band-pass filter...");
		initializeFilterBank(pFilter, false);
		logger.info("done");

		// apply median filtering
		logger.info("Initializing Median Filter...");
		median = new MedianFilter(output, 5, MedianFilter.InputType.FRAMED);
		output = median;
		logger.info("done");

		buf = new double[output.getFrameSize()];
		nuclei = new SyllableNucleus[3];
		cache = new LinkedList<double[]>();
		current_nuclei = new SyllableNucleus[output.getFrameSize()];

		logger.info("Calculating frame size...");
		framesize = 1;
		if (with_positions)
			framesize += 5;
		if (with_absolute_syllable_data)
			framesize += 3;
		if (!without_duration)
			framesize += 1;
		if (!without_loudness)
			framesize += 2;
		logger.info("done");
	}

	/**
	 * Constructor for finding the maxima_threshold Stops after the application
	 * of the median filter
	 * 
	 * @param inFile
	 *            the input file to read data from
	 * @param pAudio
	 *            the audio parameter string
	 * @param pWindow
	 *            the window parameter string
	 * @param pFilter
	 *            the filter parameter string
	 * @param medianLength
	 *            the length of the median filter to apply
	 */
	public SyllableNucleiDetector(String inFile, String pAudio, String pWindow,
			String pFilter, int medianLength) throws Exception {
		// get audio data
		logger.info("Initializing audio...");
		initializeAudio(inFile, pAudio);
		logger.info("done");
		logger.info("initializing windowing...");
		// window the data to get frames
		initializeWindow(pWindow);
		logger.info("done.");
		// get the power spectrum
		logger.info("Initializing power spectrum...");
		initializeFFT(true, false);
		logger.info("done");
		// apply bandpass-filtering
		logger.info("Initializing band-pass filter...");
		initializeFilterBank(pFilter, false);
		logger.info("done");

		// apply median filtering
		logger.info("Initializing Median Filter...");
		median = new MedianFilter(output, 5, MedianFilter.InputType.FRAMED);
		output = median;
		logger.info("done");
		framesize = output.getFrameSize();
		real_mode = false;
	}

	/**
	 * determines if a specified value of a specified band is a maximum
	 * 
	 * @param cache_index
	 *            the index of the value in the current cache
	 * @param filter_index
	 *            the band to search
	 * @param context
	 *            the context to take into account (2 * context + 1 values are
	 *            compared)
	 * @return true if there is a maximum at cache_index, false otherwise
	 */
	private boolean isMaximum(int cache_index, int filter_index, int context) {
		double curr_value = cache.get(cache_index)[filter_index];
		logger.info(toString() + ":\t\tisMaximum(" + cache_index + ", "
				+ filter_index + ", " + context + "): curr_value = "
				+ curr_value);
		for (int i = -context; i <= context; i++) {
			logger
					.info(toString()
							+ "\t\t\tLooking at context pos "
							+ i
							+ ": "
							+ (cache_index + i >= 0
									&& cache_index + i < cache.size() ? cache
									.get(cache_index + i)[filter_index]
									: "out of range"));
			if (i == 0)
				continue;
			if (i < 0) {
				if (cache_index + i >= 0) {
					if (cache.get(cache_index + i)[filter_index] > curr_value) {
						return false;
					}
				}
			}
			if (i > 0) {
				if (cache_index + i < cache.size()) {
					if (cache.get(cache_index + i)[filter_index] > curr_value) {
						return false;
					}
				} else {
					if (!endOfInput) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * determines if a specified value of a specified band is a minimum
	 * 
	 * @param cache_index
	 *            the index of the value in the current cache
	 * @param filter_index
	 *            the band to search
	 * @param context
	 *            the context to take into account (2 * context + 1 values are
	 *            compared)
	 * @return true if there is a minimum at cache_index, false otherwise
	 */
	private boolean isMinimum(int cache_index, int filter_index, int context) {
		double curr_value = cache.get(cache_index)[filter_index];
		logger.info(toString() + ":\t\tisMinimum(" + cache_index + ", "
				+ filter_index + ", " + context + "): curr_value = "
				+ curr_value);
		for (int i = -context; i <= context; i++) {
			logger
					.info(toString()
							+ "\t\t\tLooking at context pos "
							+ i
							+ ": "
							+ (cache_index + i >= 0
									&& cache_index + i < cache.size() ? cache
									.get(cache_index + i)[filter_index]
									: "out of range"));
			if (i == 0)
				continue;
			if (i < 0) {
				if (cache_index + i >= 0) {
					if (cache.get(cache_index + i)[filter_index] < curr_value) {
						return false;
					}
				}
			}
			if (i > 0) {
				if (cache_index + i < cache.size()) {
					if (cache.get(cache_index + i)[filter_index] < curr_value) {
						return false;
					}
				} else {
					if (!endOfInput) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Searches for a nucleus in a specified band in the current cache
	 * 
	 * @param band
	 *            the band to search
	 * @param local_factor
	 *            factor to use for calculating the local threshold used to
	 *            search for minima
	 * @param cache_offset
	 *            number of frames in the cache to skip for this search
	 * @return the found SyllableNucleus, or null if there is no nucleus to be
	 *         found
	 */
	private SyllableNucleus detectNucleus(int band, double local_factor,
			int cache_offset) {
		SyllableNucleus found = null;
		logger.info("SyllableNuclei.detectNucleus(" + band + ", "
				+ local_factor + ", " + cache_offset
				+ "): start with cache.size() = " + cache.size());
		for (int i = cache_offset; i < cache.size(); i++) {
			double possible_max = cache.get(i)[band];
			logger
					.info("SyllableNuclei.detectNucleus():  looking at cache index "
							+ i
							+ " (complete signal index "
							+ (i + index_offset)
							+ "): "
							+ possible_max
							+ "[ significance threshold: "
							+ significance_threshold + "]");
			if (possible_max > significance_threshold
					&& isMaximum(i, band, minima_maxima_context)) {
				double local_threshold = (1 - local_factor) * possible_max;
				logger.info("\tmight be a maximum! local threshold set to "
						+ local_threshold);
				// found a potential maximum
				// search for a minimum to the left
				maximum_checkpoint: for (int l = i; l > 0; l--) {
					double possible_min_l = cache.get(l)[band];
					if (possible_min_l < local_threshold
							&& isMinimum(l, band, minima_maxima_context)) {
						// found a left minimum! yay!
						// get the syllable nucleus limit
						double snl_l_threshold = (possible_max + possible_min_l) / 2;
						logger.info("\tfound a possible left minimum at index "
								+ l + ": " + possible_min_l
								+ "; snl_l_threshold: " + snl_l_threshold);
						int possible_snl_l = -1;
						for (int snl_l = i; snl_l >= l; snl_l--) {
							if (cache.get(snl_l)[band] < snl_l_threshold) {
								possible_snl_l = snl_l;
								break;
							}
						}
						if (possible_snl_l == -1) {
							// this is really strange and should not happen...
							break maximum_checkpoint;
						}
						logger.info("\t\tLocated snl_l at " + possible_snl_l
								+ " with value "
								+ cache.get(possible_snl_l)[band]);
						// now search a right minimum. yes, in the loop, we need
						// both.
						for (int r = i; r < cache.size(); r++) {
							double possible_min_r = cache.get(r)[band];
							if (possible_min_r < local_threshold
									&& isMinimum(r, band, minima_maxima_context)) {
								// found a right minimum!
								// get the syllable nucleus limit
								double snl_r_threshold = (possible_max + possible_min_r) / 2;
								logger
										.info("\tfound a possible right minimum at index "
												+ r
												+ ": "
												+ possible_min_r
												+ "; snl_r_threshold: "
												+ snl_r_threshold);
								int possible_snl_r = -1;
								for (int snl_r = i; snl_r <= r; snl_r++) {
									if (cache.get(snl_r)[band] < snl_r_threshold) {
										possible_snl_r = snl_r;
										break;
									}
								}
								if (possible_snl_r == -1) {
									// really strange, should not happen
									break maximum_checkpoint;
								}
								logger.info("\t\tlocated snl_r at "
										+ possible_snl_r + " with value "
										+ cache.get(possible_snl_r)[band]);
								// check minimum width
								logger.info("\tChecking "
										+ (possible_snl_r - possible_snl_l)
										+ " against min_width (" + min_width
										+ ")");
								if (possible_snl_r - possible_snl_l < min_width) {
									// this syllable nucleus is too small,
									// search for a better one in the rest of
									// the cache
									logger
											.info("\t\tNucleus too small, back to maximum search...");
									break maximum_checkpoint;
								}
								logger.info("\tChecking r (" + r
										+ ") against cache.size() - 5 ("
										+ (cache.size() - 5) + ") while "
										+ (endOfInput ? "not" : "")
										+ " having any more input");
								if (r > cache.size() - 5 && !endOfInput) {
									logger
											.info("\t\tnucleus too near to end of cache while there still is input to be read, back to maximum search");
									break maximum_checkpoint;
								}
								// now we got everything we need for this
								// syllable nucleus
								if (found == null) {
									found = new SyllableNucleus();
								}
								found.i_max = i + index_offset;
								found.i_min_l = l + index_offset;
								found.i_min_r = r + index_offset;
								found.i_snl_l = possible_snl_l + index_offset;
								found.i_snl_r = possible_snl_r + index_offset;

								found.max = possible_max;
								found.min_l = possible_min_l;
								found.min_r = possible_min_r;
								found.snl_l = cache.get(possible_snl_l)[band];
								found.snl_r = cache.get(possible_snl_r)[band];

								found.max_e = possible_max;
								found.sum_e = 0.0;

								for (int e = possible_snl_l; e <= possible_snl_r; e++) {
									double energy = cache.get(e)[band];
									if (energy > found.max_e) {
										found.max_e = energy;
									}
									found.sum_e += energy;
								}

								found.band = band;

								logger.info("\tFound a nucleus! Rejoice!");
								// now i have that nucleus, get out of here!
								return found;
							} else {
								if (possible_min_r > possible_max) {
									break maximum_checkpoint;
								}
							}
						}
					} else {
						if (possible_min_l > possible_max) {
							break maximum_checkpoint;
						}
					}
				}
			}
		}
		return found;
	}

	/**
	 * reads data into the cache until at least one nucleus is found obeys
	 * restrictions concerning touching nuclei in the other bands
	 * 
	 * @return the found syllable nucleus obeying all restrictions, if any. null
	 *         otherwise
	 */
	private SyllableNucleus checkCurrentNuclei() throws IOException {
		boolean found_one = false;

		SyllableNucleus to_return = null;
		logger.info("SyllableNuclei.checkCurrentNuclei()");
		while (to_return == null && output.read(this.buf)) {
			cache.addLast(this.buf.clone());
			logger
					.info("\tFound nothing yet but reading works, cache size is now "
							+ cache.size());
			logger.info("\tdetecting nuclei in all bands...");
			for (int i = 0; i < current_nuclei.length; i++) {
				current_nuclei[i] = detectNucleus(i, local_threshold_factor, 0);
				if (current_nuclei[i] != null) {
					found_one = true;
				}
			}
			logger.info("done."
					+ (found_one ? " i got at least one!"
							: " i got nothing, so continue..."));
			if (found_one) {
				logger.info("current_nuclei state:");
				for (int i = 0; i < current_nuclei.length; i++) {
					logger.info("\tcurrent_nuclei[" + i + "]:");
					if (current_nuclei[i] != null) {
						logger.info(current_nuclei[i].toString());
					} else {
						logger.info("NULL");
					}
				}
				if (current_nuclei[1] != null) {
					logger.info("Nucleus in sonorant band");
					if (current_nuclei[2] != null) {
						if (current_nuclei[1].i_snl_l > current_nuclei[2].i_snl_r
								|| current_nuclei[1].i_snl_r < current_nuclei[2].i_snl_l) {
							// no contact
							to_return = current_nuclei[1];

							logger
									.info("\tnucleus in fricative band does not touch the sonorant one");
						} else {
							// touches
							logger
									.info("\tnucleus in fricative band touches the sonorant one");
							if (current_nuclei[2].sum_e < current_nuclei[1].sum_e
									|| current_nuclei[2] == null) {
								logger
										.info("\tfricative band has nucleus with smaller energy, success!");
								to_return = current_nuclei[1];

							} else {
								logger
										.info("\tfricative band nucleus has greater energy :-(");
							}
						}
					} else {
						logger.info("\tfricative band has no nucleus! yeah!");
						to_return = current_nuclei[1];

					}
				} else {
					logger.info("No nucleus in sonorant band");
				}
				if (current_nuclei[0] != null) {
					logger.info("Nucleus in low band");
					if (current_nuclei[1] == null) {
						logger
								.info("Nucleus is in really low band and there is no nucleus in sonorant band -> success!");
						to_return = current_nuclei[0];

					} else {
						if (current_nuclei[0].i_snl_l > current_nuclei[1].i_snl_r
								|| current_nuclei[0].i_snl_r < current_nuclei[1].i_snl_l) {
							logger
									.info("Nucleus is in really low band and doesn't touch nucleus in sonorant band -> success!");
							to_return = current_nuclei[0];

						} else {
							logger
									.info("Nucleus in low band and touches nucleus in sonorant band :-(");
						}
					}
				} else {
					logger.info("No nucleus in low band");
				}
				// clear current_nuclei for the next run (reference still in
				// to_return, so no garbage collection
				for (int i = 0; i < current_nuclei.length; i++) {
					current_nuclei[i] = null;
				}
				if (to_return == null) {
					logger.info("No valid nucleus found :-(");
				} else {
					logger.info("Found a valid nucleus...");
				}
			}
		}
		if (to_return == null) {
			endOfInput = true;
		}
		return to_return;
	}

	/**
	 * uses checkCurrentNuclei() to find a "real" nucleus, checks its distance
	 * to any previously found ones and checks if the nucleus is overly long,
	 * splitting it if it is
	 * 
	 * @return the valid syllable nucleus that may be used for further
	 *         computations, if any. null otherwise
	 */
	private SyllableNucleus getNextNucleus() throws IOException {
		logger.info("SyllableNuclei.getNextNucleus(): start");
		SyllableNucleus next = null;
		if (nuclei_cache == null) {
			nuclei_cache = new LinkedList<SyllableNucleus>();
		}
		if (nuclei_cache.size() == 0) {
			logger
					.info("SyllableNuclei.getNextNucleus(): nuclei_cache empty --> detecting more");
			next = checkCurrentNuclei();
			if (next != null) {
				logger
						.info("SyllableNuclei.getNextNucleus(): Found one, checking distance...");
				if (last != null) {
					logger
							.info("SyllableNuclei.getNextNucleus(): really need to check distance (i have a last nucleus)");
					while (next != null
							&& next.getDistance(last) < min_distance) {
						logger
								.info("SyllableNuclei.getNextNucleus(): distance to small, searching next...");
						logger.info("clear signal cache...");
						while (next != null && index_offset < next.i_min_r
								&& cache.size() > 0) {
							cache.removeFirst();
							index_offset++;
						}
						logger.info("done. signal cache size is now "
								+ cache.size());
						next = checkCurrentNuclei();
					}
					logger
							.info("SyllableNuclei.getNextNucleus(): either got one with sufficient distance or no more nuclei there...");
				} else {
					logger
							.info("SyllableNuclei.getNextNucleus(): got no last nucleus, no need to check distance.");
				}
			}
			if (next != null) {
				logger
						.info("SyllableNuclei.getNextNucleus(): got one with sufficient distance, checking with soft_threshold if it would be 2 nuclei...");
				SyllableNucleus test1 = detectNucleus(next.band,
						soft_local_threshold_factor, 0);
				if (test1 != null && test1.i_min_r < next.i_min_r) {
					logger
							.info("SyllableNuclei.getNextNucleus(): right edge with soft threshold is farther left, checking if there is another one to the right now");
					SyllableNucleus test2 = detectNucleus(next.band,
							soft_local_threshold_factor, test1.i_min_r
									- index_offset);
					if (test2 != null) {
						logger
								.info("SyllableNuclei.getNextNucleus(): There is another one to the right! adding both to cache!");
						nuclei_cache.addLast(test1);
						nuclei_cache.addLast(test2);
					} else {
						logger
								.info("SyllableNuclei.getNextNucleus(): No other one to the right now, doing nothing...");
					}
				}
				if (nuclei_cache.size() == 0) {
					logger
							.info("SyllableNuclei.getNextNucleus(): test with soft threshold failed, adding original nucleus to cache...");
					nuclei_cache.addLast(next);
				}
				logger.info("clear signal cache...");
				while (next != null && index_offset < next.i_min_r
						&& cache.size() > 0) {
					cache.removeFirst();
					index_offset++;
				}
				logger.info("done. signal cache size is now " + cache.size());
			}
		}
		if (nuclei_cache.size() > 0) {
			logger
					.info("SyllableNuclei.getNextNucleus(): got at least one nucleus in my cache, returning that one.");
			next = nuclei_cache.removeFirst();
			last = next;
		}
		logger.info("SyllableNuclei.getNextNucleus(): end");
		return next;
	}

	@Override
	public String toString() {
		return "bin.SyllableNuclei frame_size=" + framesize;
	}

	@Override
	public boolean read(double[] buf) throws IOException {
		if (!real_mode) {
			return output.read(buf);
		}
		logger.info("SyllableNuclei.read() starting");
		int num_nuclei_in_cache = 0;
		for (int i = 0; i < nuclei.length; i++) {
			if (nuclei[i] != null) {
				num_nuclei_in_cache++;
			}
		}
		int nuclei_needed = nuclei.length - num_nuclei_in_cache;
		logger.info("SyllableNuclei.read(): i have " + num_nuclei_in_cache
				+ " nuclei cached, so i need " + nuclei_needed
				+ " to work again");
		for (int i = num_nuclei_in_cache; i < nuclei.length; i++) {
			logger.info("Trying to get nucleus for cache pos " + i);
			nuclei[i] = getNextNucleus();
			if (nuclei[i] == null) {
				break;
			}
		}
		logger.info("got all the nuclei i need");
		if (nuclei[active_nuclei_index] == null) {
			logger.info("even now there is no valid nucleus at cache["
					+ active_nuclei_index + "]: returning false!");
			if (outputLAB) {
				labout.close();
			}
			return false;
		}
		logger.info("filling read buffer...");
		int buf_index = 0;
		double freq1 = -1;
		if (active_nuclei_index > 0) {
			freq1 = 2
					* (double) window.getNumberOfFramesPerSecond()
					/ (double) (nuclei[active_nuclei_index].i_max - nuclei[active_nuclei_index - 1].i_max);
			logger.info("Calculated 'left' frequency as " + freq1 + " (2 * "
					+ window.getNumberOfFramesPerSecond() + " / ("
					+ nuclei[active_nuclei_index].i_max + " - "
					+ nuclei[active_nuclei_index - 1].i_max + "))");
		}
		double freq2 = -1;
		if (nuclei[active_nuclei_index + 1] != null) {
			freq2 = 2
					* (double) window.getNumberOfFramesPerSecond()
					/ (double) (nuclei[active_nuclei_index + 1].i_max - nuclei[active_nuclei_index].i_max);
			logger.info("Calculated 'right' frequency as " + freq2 + " (2 * "
					+ window.getNumberOfFramesPerSecond() + " / ("
					+ nuclei[active_nuclei_index + 1].i_max + " - "
					+ nuclei[active_nuclei_index].i_max + "))");
		}
		double freq = (freq1 < 0 && freq2 > 0 ? freq2 : (freq1 < 0 ? 0
				: (freq2 > 0 ? (freq1 + freq2) / 2 : freq1)));
		logger.info("Mean frequency is " + freq);

		buf[buf_index++] = freq;
		if (with_positions) {
			buf[buf_index++] = nuclei[active_nuclei_index].i_min_l
					* window.getShift();
			buf[buf_index++] = nuclei[active_nuclei_index].i_snl_l
					* window.getShift();
			buf[buf_index++] = nuclei[active_nuclei_index].i_max
					* window.getShift();
			buf[buf_index++] = nuclei[active_nuclei_index].i_snl_r
					* window.getShift();
			buf[buf_index++] = nuclei[active_nuclei_index].i_min_r
					* window.getShift();
		}
		if (with_absolute_syllable_data) {
			buf[buf_index++] = (nuclei[active_nuclei_index].i_snl_r - nuclei[active_nuclei_index].i_snl_l)
					* frames_to_milliseconds / 1000.0;
			buf[buf_index++] = nuclei[active_nuclei_index].max_e;
			buf[buf_index++] = nuclei[active_nuclei_index].sum_e;
		}
		if (!without_duration) {
			buf[buf_index++] = (2
					* nuclei[active_nuclei_index].getFramesDuration()
					- (active_nuclei_index > 0 ? nuclei[active_nuclei_index - 1]
							.getFramesDuration()
							: 0) - (nuclei[active_nuclei_index + 1] != null ? nuclei[active_nuclei_index + 1]
					.getFramesDuration()
					: 0))
					/ frames_to_milliseconds;
		}
		if (!without_loudness) {
			buf[buf_index++] = (active_nuclei_index > 0 ? nuclei[active_nuclei_index].max_e
					/ nuclei[active_nuclei_index - 1].max_e
					: 0)
					+ (nuclei[active_nuclei_index + 1] != null ? nuclei[active_nuclei_index].max_e
							/ nuclei[active_nuclei_index + 1].max_e
							: 0) - 2;
			buf[buf_index++] = (active_nuclei_index > 0 ? nuclei[active_nuclei_index].sum_e
					/ nuclei[active_nuclei_index - 1].sum_e
					: 0)
					+ (nuclei[active_nuclei_index + 1] != null ? nuclei[active_nuclei_index].sum_e
							/ nuclei[active_nuclei_index + 1].sum_e
							: 0) - 2;
		}
		if (outputLAB) {
			labout.println(""
					+ (nuclei[active_nuclei_index].i_snl_l
							* frames_to_milliseconds / 1000.0)
					+ "\t"
					+ (nuclei[active_nuclei_index].i_snl_r
							* frames_to_milliseconds / 1000.0) + "\t"
					+ "Syllable Nucleus");
		}
		logger.info("done.");
		if (active_nuclei_index == 0) {
			logger.info("active_nuclei_index == 0 --> set it to 1");
			active_nuclei_index = 1;
		} else {
			logger
					.info("active_nuclei_index is already 1, shift references in the array...");
			for (int i = 0; i < nuclei.length - 1; i++) {
				nuclei[i] = nuclei[i + 1];
			}
			nuclei[nuclei.length - 1] = null;
			logger.info("done.");
		}
		logger.info("SyllableNuclei.read() finished.");
		return true;
	}

	public int getFrameSize() {
		return framesize;
	}

	public FrameSource getSource() {
		return output;
	}

	private static final String nl = System.getProperty("line.separator");

	private static final String SYNOPSIS = "Syllable Nuclei feature extraction v"
			+ FEX_VERSION
			+ nl
			+ "usage: bin.SyllableNuclei [options]"
			+ nl
			+ nl
			+ "file options:"
			+ nl
			+ "  -i in-file"
			+ nl
			+ "    use the given file for input. use \"-\" for default microphone"
			+ nl
			+ "    input or \"mixer:<mixer-name>\" for a specific mixer"
			+ nl
			+ "  -o out-file"
			+ nl
			+ "    write output to out-file, default: std-out"
			+ nl
			+ "  --in-out-list <list>"
			+ nl
			+ "    the <list> contains lines \"<infile> <outfile>\" for batch processing"
			+ nl
			+ "  --in-list <list> <dir>"
			+ nl
			+ "    read infiles from <list>, outfiles are <dir>/basename(infile)"
			+ nl
			+ "  --dir <directory>"
			+ nl
			+ "    use this to specify a directory where the audio files are located."
			+ nl
			+ "  --ufv"
			+ nl
			+ "    write UFVs instead of header + double frames"
			+ nl
			+ nl
			+ "audio format options:"
			+ nl
			+ "  -f <format-string>"
			+ nl
			+ "    \"f:path-to-file-with-header\": load audio format from file"
			+ nl
			+ "    \"t:template-name\": use an existing template (ssg/[8,16], alaw/[8,16], ulaw/[8,16]"
			+ nl
			+ "    \"r:bit-rate,sample-rate,signed(0,1),little-endian(0,1)\": specify raw format (no-header)"
			+ nl
			+ "    default: \""
			+ DEFAULT_AUDIO_FORMAT
			+ "\""
			+ nl
			+ nl
			+ "feature extraction options:"
			+ nl
			+ "  -w \"<hamm|hann|rect>,<length-ms>,<shift-ms>\""
			+ nl
			+ "    window function (Hamming, Hann, Rectangular), length of window and"
			+ nl
			+ "    shift time (in ms)"
			+ nl
			+ "    It is STRONGLY recommended to use non-overlapping windows (length == shift)!"
			+ nl
			+ "    default: \""
			+ DEFAULT_WINDOW
			+ "\""
			+ nl
			+ "  --median-length <length>"
			+ nl
			+ "    Length of the median filter to use to smoothe the band-passed energy contour."
			+ nl
			+ "    Must be odd."
			+ nl
			+ "    default: "
			+ DEFAULT_MEDIAN_LENGTH
			+ nl
			+ "  --significance <threshold>"
			+ nl
			+ "    The minimum energy a potential maximum needs to have to be treated as one."
			+ nl
			+ "  --find-significance"
			+ nl
			+ "    calculate the significance factor of the input as the mean of the maximum and the minimum energy."
			+ nl
			+ "  --local_threshold_factors <hard factor> <soft factor>"
			+ nl
			+ "    A factor describing the minimum relative amplitude of a maximum. 0 < <hard factor> < 1"
			+ nl
			+ "    After finding a nucleus, it is re-checked with <soft factor> to see if it actually spans 2 nuclei. 0 < <soft factor> < <hard factor>"
			+ nl
			+ "  --min-width <width>"
			+ nl
			+ "    The minimum width (in frames) a syllable nucleus needs to have."
			+ nl
			+ "    default: "
			+ DEFAULT_MIN_WIDTH
			+ nl
			+ "  --min-distance <dist>"
			+ nl
			+ "    The minimum distance (in frames) between two nuclei."
			+ nl
			+ "    default: "
			+ DEFAULT_MIN_DISTANCE
			+ nl
			+ "  --min-max-context <context>"
			+ nl
			+ "    The context to take into account when verifying a maximum/minimum."
			+ nl
			+ "    In other words, search an area of 2 * <context> + 1 for bigger/smaller values."
			+ nl
			+ "    default: "
			+ DEFAULT_MIN_MAX_CONTEXT
			+ nl
			+ "  --with-absolute-data"
			+ nl
			+ "    if present, include absolute data about the syllable nucleus in the feature frame."
			+ nl
			+ "    Intended for plotting purposes."
			+ nl
			+ "  --without-duration"
			+ nl
			+ "    if present, omit duration features."
			+ nl
			+ "  --without-loudness"
			+ nl
			+ "    if present, omit loudness features."
			+ nl
			+ "  --with-positions"
			+ nl
			+ "    if present, include position of milliseconds in each feature frame"
			+ nl
			+ "  --gnuplot"
			+ nl
			+ "    no real processing, but per step: output gnuplot-compatible .dat file and cache data for next step"
			+ nl
			+ "  --labfile"
			+ nl
			+ "    output a .lab-file for use with wavesurfer containing nuclei data"
			+ nl
			+ "general options:"
			+ nl
			+ "  -h | --help"
			+ nl
			+ "    display this help text" + nl;

	public static void main(String[] args) throws Exception {
		// defaults
		boolean displayHelp = false;
		boolean float_feat = true;
		int median_length = DEFAULT_MEDIAN_LENGTH;
		double significance = -1.;
		double hard_amplitude_factor = -1.;
		double soft_amplitude_factor = -1.;
		int min_width = DEFAULT_MIN_WIDTH;
		int min_distance = DEFAULT_MIN_DISTANCE;
		int min_max_context = DEFAULT_MIN_MAX_CONTEXT;
		boolean with_absolute = false;
		boolean without_duration = false;
		boolean without_loudness = false;
		boolean with_positions = false;

		boolean find_significance_mode = false;

		String inFile = null;
		String outFile = null;
		String outDir = null;
		String listFile = null;
		String audioDir = null;

		String audioFormatString = DEFAULT_AUDIO_FORMAT;
		String windowFormatString = DEFAULT_WINDOW;
		String filterFormatString = DEFAULT_FILTER;

		if (args.length > 1) {
			// process arguments
			for (int i = 0; i < args.length; ++i) {

				// file options
				if (args[i].equals("--in-out-list"))
					listFile = args[++i];
				else if (args[i].equals("-i"))
					inFile = args[++i];
				else if (args[i].equals("-o"))
					outFile = args[++i];
				else if (args[i].equals("--double"))
					float_feat = false;
				else if (args[i].equals("--in-list")) {
					listFile = args[++i];
					outDir = args[++i];
				}

				// prefix for audio input directory
				else if (args[i].equals("--audioinputdir")
						|| args[i].equals("--dir"))
					audioDir = args[++i];

				// audio format options
				else if (args[i].equals("-f"))
					audioFormatString = args[++i];

				// window options
				else if (args[i].equals("-w"))
					windowFormatString = args[++i];

				else if (args[i].equals("--median-length"))
					median_length = Integer.parseInt(args[++i]);
				else if (args[i].equals("--significance"))
					significance = Double.parseDouble(args[++i]);
				else if (args[i].equals("--find-significance"))
					find_significance_mode = true;
				else if (args[i].equals("--local-threshold-factors")) {
					hard_amplitude_factor = Double.parseDouble(args[++i]);
					soft_amplitude_factor = Double.parseDouble(args[++i]);
				} else if (args[i].equals("--min-width"))
					min_width = Integer.parseInt(args[++i]);
				else if (args[i].equals("--min-distance"))
					min_distance = Integer.parseInt(args[++i]);
				else if (args[i].equals("--min-max-context"))
					min_max_context = Integer.parseInt(args[++i]);
				else if (args[i].equals("--with-absolute-data"))
					with_absolute = true;
				else if (args[i].equals("--with-positions"))
					with_positions = true;
				else if (args[i].equals("--without-duration"))
					without_duration = true;
				else if (args[i].equals("--without-loudness"))
					without_loudness = true;
				else if (args[i].equals("--labfile"))
					SyllableNucleiDetector.outputLAB = true;

				// help?
				else if (args[i].equals("-h") || args[i].equals("--help"))
					displayHelp = true;
				// whoops...
				else
					System.err.println("ignoring argument " + i + ": "
							+ args[i]);
			}
		} else {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		// help?
		if (displayHelp) {
			System.err.println(SYNOPSIS);
			System.exit(0);
		}

		// consistency checks
		if (listFile != null && (inFile != null || outFile != null))
			throw new Exception("-l and (-i,-o) are exclusive!");

		if (!(find_significance_mode)) {
			if (hard_amplitude_factor <= 0. || soft_amplitude_factor <= 0.) {
				throw new Exception("local threshold factors too small!!");
			}
			if (hard_amplitude_factor >= 1.0) {
				throw new Exception(
						"hard local threshold factor too big! Needs to be < 1");
			}
			if (soft_amplitude_factor >= hard_amplitude_factor) {
				throw new Exception(
						"soft local threshold factor needs to be smaller than hard threshold factor!");
			}
			if (with_absolute && without_duration && without_loudness) {
				throw new Exception(
						"At least one of --with-absolute-data, --without-duration and --without-loudness needs to not be given!");
			}
			if (min_max_context < 0) {
				throw new Exception("min_max_context must be positive!");
			}
		}

		LinkedList<String> inlist = new LinkedList<String>();
		LinkedList<String> outlist = new LinkedList<String>();

		// read list
		if (listFile == null) {
			inlist.add(inFile);
			outlist.add(outFile);
		} else {
			BufferedReader lr = new BufferedReader(new FileReader(listFile));
			String line = null;
			int i = 1;
			while ((line = lr.readLine()) != null) {
				if (outDir == null) {
					String[] help = line.split("\\s+");
					if (help.length != 2)
						throw new Exception("file list is broken at line " + i);
					inlist.add(help[0]);
					outlist.add(help[1]);
				} else {
					// input file. add the string to each audiofile in list
					if (audioDir != null) {
						line = audioDir + "/" + line;
					}
					String[] help = line.split("/");
					inlist.add(line);
					outlist.add(outDir + "/" + help[help.length - 1]);
				}
				i++;
			}
		}

		SyllableNucleiDetector sn = null;

		if (find_significance_mode) {
			significance = 0.0;
		}
		int numFrames = 0;
		while (inlist.size() > 0) {
			inFile = inlist.remove(0);
			outFile = outlist.remove(0);

			if (find_significance_mode) {
				sn = new SyllableNucleiDetector(inFile, audioFormatString,
						windowFormatString, filterFormatString, median_length);

				double[] buf = new double[sn.getFrameSize()];
				while (sn.read(buf)) {
					significance += buf[1];
					numFrames++;
				}
			} else {
				sn = new SyllableNucleiDetector(inFile, audioFormatString,
						windowFormatString, filterFormatString, median_length,
						significance, hard_amplitude_factor,
						soft_amplitude_factor, min_width, min_distance,
						min_max_context, with_absolute, without_duration,
						without_loudness, with_positions);
				double[] buf = new double[sn.getFrameSize()];
				FrameOutputStream fos = new FrameOutputStream(buf.length,
						new File(outFile), float_feat);
				while (sn.read(buf)) {
					fos.write(buf);
				}
				fos.close();
			}
		}
		if (find_significance_mode) {
			System.out.println("Significance threshold: "
					+ (significance / (double) numFrames));
		}
	}

}
