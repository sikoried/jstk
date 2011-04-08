package de.fau.cs.jstk.framed;

import java.io.IOException;

import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.SineGenerator;
import de.fau.cs.jstk.util.MaxDetector;



/**
 * this class estimates the fundamental frequency of one frame
 * 
 * @author siniwitt
 * */
public class F0DetectorRT implements FrameSource {

	public static final double DEFAULT_UB = 400.;
	
	public static final double DEFAULT_LB = 60.;
	
	private static final int DEFAULT_FEATURE = 1;
	
	/** number of candidates to extract */
	private int nc;
	
	/** sampling rate of the input signal */
	private double sr;
	
	/** source to read from */
	private FrameSource window;
	
	/** source to read from */
	private FIRFilter windowFiltered;
	
	/** Copies of window for reuse */
	private SimulatedFrameSource windowACF;
	
	/** source function to compute F0*/
	private ACF acf;
	
	/** buffer used to read from window */
	private double [] bufRead;
	
	/** buffer used to read from f0 detection function */
	private double [] bufDetect;
	
	/** lower F0 boundary (Hz) */
	private double lb = DEFAULT_LB;
	
	/** upper F0 boundary (Hz) */
	private double ub = DEFAULT_UB;
	
	private int ind_lb;
	
	private int ind_ub;
	
	private double[] f0Candidates;
	
	private double[] vudFeature = new double[1];
	
	private int featureNr;
	private double featureThreshold;
	
	/** 
	 * Create a new F0VUDetectiorRT extractor, providing the best F0 estimate.
	 * @param source
	 * @param sampleRate
	 */
	public F0DetectorRT(FrameSource window, int sampleRate) {
		this(window, sampleRate, DEFAULT_LB, DEFAULT_UB, DEFAULT_FEATURE);
	}
	
	/**
	 * Create a new F0VUDetectiorRT extractor, providing a list of alternate F0 estimates
	 * @param source
	 * @param sampleRate
	 * @param candidates number of candidates to extract
	 */
	public F0DetectorRT(FrameSource window, int sampleRate, int featureNr) {
		this(window, sampleRate, DEFAULT_LB, DEFAULT_UB, featureNr);
	}
	
	/** 
	 * Create a new F0VUDetectiorRT extractor, providing a list of alternate F0 estimates
	 * @param source
	 * @param sampleRate
	 * @param candidates number of candidates to extract
	 * @param lower lower boundary for plausible F0
	 * @param upper upper boundary for plausible F0
	 */
	public F0DetectorRT(FrameSource window, int sampleRate, double lower, double upper, int featureNr) {
		this.window = window;
		this.sr = (double) sampleRate;
		this.lb = lower;
		this.ub = upper;
		this.featureNr = featureNr;
		this.f0Candidates = new double[1];
		
		windowACF = new SimulatedFrameSource(new double [1][window.getFrameSize()]);
		windowACF.dequeue();
		
		//default source for F0 computation is ACF
		//windowFiltered = new FIRFilter(windowACF, "F:\\DA_Data\\FIRFilters\\Lowpass(400-1000_80dB).txt", true);
		windowFiltered = new FIRFilter(windowACF, "/home/sithjanu/work/da/code/F0_Extraction/FIRFilters/Lowpass(400-1000_80dB).txt", true);
		acf = new ACF(windowFiltered);
		
		// allocate buffers
		this.bufRead = new double [window.getFrameSize()];
		this.bufDetect = new double [acf.getFrameSize()];
		
		// indices: they go from high to low -- the shorter the lag, the higher the freq!
		ind_lb = (int) (sr / ub);
		ind_ub = (int) (sr / lb);
	}
	
	public String toString() {
		return "F0DetectorRT: Allowing frequencies within " + lb + "Hz (ind=" + ind_ub + ") and " + ub + "Hz (ind=" + ind_lb + ")";
	}
	
	public int getFrameSize() {
		return 1;
	}

	public FrameSource getSource() {
	    return window;
	}
	
	/** 
	 * Read the next frame and extract the F0 candidates.
	 */
	public boolean read(double [] buf) throws IOException {
		if (!window.read(bufRead))
			return false;
		
		// copy window
		windowACF.appendFrame(bufRead);
		acf.read(bufDetect);
		MaxDetector maxDetector = new MaxDetector(bufDetect, 1, ind_lb, ind_ub, true);
		double[] maxIndices 	= maxDetector.getMaxIndices();
		if(new Double(maxIndices[0]).isNaN()){
			buf[0] = 0.0;
		}else{
			f0Candidates[0] = sr / maxIndices[0];
			buf[0] = f0Candidates[0];
		}
		
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		double F0 = 70;
		double[] freq = new double[20];
		freq[0] = F0;
		for(int i = 1; i<freq.length; i++)
			freq[i] = freq[i-1]+F0;
			
		AudioSource as = new SineGenerator(10000L, freq);
//		as  = new sampled.AdditiveNoiseSource(as, Math.sqrt(2), -0.0);
		System.out.println(as.toString());
		
		int shift_ms = 10;
		Window w = new HammingWindow(as, 25, shift_ms);
		System.out.println(w);

		F0DetectorRT f0detector =  new F0DetectorRT(w, as.getSampleRate(), 1);
		
		int readNFrames = 900;
		double [] buf = new double [f0detector.getFrameSize()];
		double [] f0s = new double [readNFrames];
		
		double [] error = new double [readNFrames];
	
		int numFrames = 0;
		long start, end, diff;
		start = System.currentTimeMillis();
		while (f0detector.read(buf)) {
			if(numFrames >= readNFrames)
				break;
			f0s[numFrames]   = buf[0];
			error[numFrames] = Math.abs((buf[0]-F0));
			numFrames++;
		}
		end = System.currentTimeMillis();
		diff = end-start;
		
		double meanError  = 0;
		double varF0 = 0; 
		for(int i = 0; i < readNFrames; i++){
			meanError 	+= error[i]/(numFrames);
			varF0 	+= (f0s[i]-F0)*(f0s[i]-F0)/(numFrames-1);
		}
	
//		ArrayToFile.write("F0RT_f0est.txt", f0s);
		System.out.println("");
		System.out.println("Mean Abs. error = "+meanError+" Hz");
		System.out.println("Std. dev. = "+Math.sqrt(varF0)+" Hz");
		System.out.println("");
		System.out.println("real  time: "+shift_ms*readNFrames+" ms");
		System.out.println("exec. time: "+diff+" ms");
		System.exit(0);
		}
}
