package de.fau.cs.jstk.util;


/**
 *	This class serves for computing the cross correlation function (CCF).
 *	The computing is done using the FFT.
 *
 * 	@author siniwitt 
 */
public class CrossCorrelation {

	/** Convolution object*/
	private Convolution conv;
	
	/** array containing the lags (x-axis of the CCF)*/
	private int[] lags;
	
	/** signal 2 in reverse order*/
	private double[] sig2Reverse;
	
	/** length of the correlation*/
	private int corrLength;
	
	/** length of the signal, if the signals are of equal lengths*/
	private int sigLength = 0;
	
	/** are the signals of equal lengths*/
	private boolean isEqualLength;
	
	/**
	 * Construct a CrossCorrelation object, which
	 * does the cross-correlation between two input signals
	 * 
	 * @param sig1 first signal
	 * @param sig2 second signal
	 */
	public CrossCorrelation(double[] sig1, double[] sig2) {
		sig2Reverse = new double[sig2.length];
		for(int i = 0; i < sig2.length; i++){
			sig2Reverse[i] = sig2[sig2.length - 1 - i];
		}
		
		// the computing is done using fast convolution
		conv = new Convolution(sig1, sig2Reverse);
		lags = new int[conv.getFrameSize()];
		int sig1Length = sig1.length;
		int sig2Length = sig2.length;
		corrLength = sig1Length+sig2Length-1;
		
		if(sig1Length == sig2Length){
			isEqualLength = true;
			sigLength = sig1Length;
		}else{
			isEqualLength = false;
		}
		
		if(!isEqualLength){
			int diffLength = sig1Length - sig2Length;
			for(int i = 0; i < (sig1Length+sig2Length-1); i++)
				lags[i] = i - sig1Length + 2 + diffLength;
		}else{
			for(int i = 0; i < corrLength; i++)
				lags[i] = i - sig1Length + 1;
		}
	}
	
	/**
	 * @return returns the framesize of the resulting cross-correlation
	 */
	public int getFrameSize() {
		return conv.getFrameSize();
	}
	
	/**
	 * @return returns the lags corresponding to the resulting
	 * cross-correlation 
	 */
	public int[] getLags(){
		return lags;
	}

	/**
	 * Computes the Crosscorrelation using the FFT
	 */
	public void getCCF(double[] ccf) {
		conv.computeResult(ccf);
		
		if(isEqualLength){
			//normalizing like MATLABs xcorr 'unbiased' option
			for(int i = 0; i < ccf.length; i++)
				ccf[i] /= (double)(sigLength - Math.abs(lags[i]));
		} 
	}
	
}
