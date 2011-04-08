package de.fau.cs.jstk.util;


import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Serves for convolving two signals A and B using the FFT
 * The resulting signal will have the length L = length(A)+length(B)-1
 * 
 * @author siniwitt
 */
public class Convolution {

	/**length of signal*/
	private int K;
	
	/**length of impulse response*/
	private int N;
	
	/**length of convolution result*/
	private int L;
	
	/**2^exp padded length*/
	private int newL;
	
	/**zero padded array 
	 * containing signal values*/
	private double[] signal;
	
	/**zero padded array 
	 * containing impulse response values*/
	private double[] h;
	
	/**for temporal storage of fft values*/
	private double[] tmp;
	
	/**fft*/
	DoubleFFT_1D fft;
	
	/**
	 * initialize the convolution given to signals
	 * @param signal the signal to convolve with e.g. an impulse response
	 * @param h the second signal for convolution e.g. an impulse response  
	 */
	public Convolution(double[] signal, double[] h){
		K = signal.length;
		N = h.length;
		L = K + N - 1;
		
		//getting length L a power of 2
		double log2L = Math.log(L)/Math.log(2);
		int exp = (int)Math.ceil(log2L);
		newL = (int)Math.pow(2., exp);
		
		//double the new length for complex values in DoubleFFT_1D
		this.signal = new double[2*newL];
		System.arraycopy(signal, 0, this.signal, 0, K);
		this.h = new double[2*newL];
		System.arraycopy(h, 0, this.h, 0, N);
		tmp = new double[2*newL];
	}
	
	/**
	 * initialize the convolution given a filter kernel (impulse response)
	 * which will not change if you do multiple convolutions
	 * @param filterKernel an impulse response
	 * @param signalLength length of the signal which will be convolved with the filterKernel    
	 */
	public Convolution(double[] filterKernel, int signalLength){
		K = signalLength;
		N = filterKernel.length;
		L = K + N - 1;
		//getting length L a power of 2
		double log2L = Math.log(L)/Math.log(2);
		int exp = (int)Math.ceil(log2L);
		newL = (int)Math.pow(2., exp);
		//double the new length for complex values in DoubleFFT_1D
		this.signal = new double[2*newL];
		this.h = new double[2*newL];
		System.arraycopy(h, 0, this.h, 0, N);
		tmp = new double[2*newL];
	}
	
	/**
	 * compute the fast convolution.
	 * Use this method along with the constructor Convolution(double[] filterKernel, int signalLength) 
	 * @param origSignal signal to convolve with the filter kernel
	 * @param result will contain the resulting samples, ensure it is big enough by using the getFrameSize() method. 
	 */
	public void computeConvResult(double[] origSignal, double[] resultSignal){
		System.arraycopy(origSignal, 0, this.signal, 0, K);
		fft = new DoubleFFT_1D(newL);
		
		//fft
		fft.realForwardFull(signal);
		fft.realForwardFull(h);
		
		
		//complex mult.
		for(int i = 0; i < newL; i++){
			tmp[2*i] 	= signal[2*i]*h[2*i]-signal[2*i+1]*h[2*i+1];
			tmp[2*i+1]	= signal[2*i]*h[2*i+1]+signal[2*i+1]*h[2*i];
		}

		//inverse fft
		fft.complexInverse(tmp, true);

		//fill result with real part
		//the starting index in tmp is choosen 1, because this corresponds to the 
		//Matlab result of convolution
		for(int i = 1; i < (L+1); i++){
			resultSignal[i-1] = tmp[2*i]; 
		}
		
	}
	
	/**
	 * computes the fast convolution (symbol #): signal # h = IFFT(FFT(signal)*FFT(h))
	 * Use this method along with the constructor Convolution(double[] signal, double[] h)
	 * @param result will contain the resulting samples, ensure it is big enough by using
	 * the getFrameSize() method 
	 * */
	public void computeResult(double[] result){
		fft = new DoubleFFT_1D(newL);
		
		//fft
		fft.realForwardFull(signal);
		fft.realForwardFull(h);
		
		//complex mult., if hConj the signal will be mult. by the complex conj. of FFT(h)
		for(int i = 0; i < newL; i++){
			tmp[2*i] 	= signal[2*i]*h[2*i]-signal[2*i+1]*h[2*i+1];
			tmp[2*i+1]	= signal[2*i]*h[2*i+1]+signal[2*i+1]*h[2*i];
		}

		//inverse fft
		fft.complexInverse(tmp, true);
		//fill result with real part
		for(int i = 1; i < (L+1); i++){
			result[i-1] = tmp[2*i]; 
		}
	}
	
	/**
	 * returns the length of the resulting convolution
	 */
	public int getFrameSize(){
		return L;
	}
	
}
