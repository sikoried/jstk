package de.fau.cs.jstk.framed;

import java.io.IOException;

import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.SineGenerator;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 *	This class serves for computing the autocorrelation function (ACF).
 *	The computing is done using the FFT.
 * 	If a window was applied to the signal the first half of the ACF
 *  is divided by the ACF values of the window
 *  
 * 	@author siniwitt 
 */
public class ACF implements FrameSource {

	/** FrameSource to read from */
	private FrameSource source;

	/** internal read buffer */
	private double[] bufRead;
	
	/**internal fft buffer*/
	private double[] bufFFT;

	/** outgoing frame size */
	private int fsOut;
	
	/** Incoming frame size*/
	private int fsIn;
	
	/** internal fft frame size*/
	private int fsFFT;
	
	/**FFT buffer size*/
	private int fsFFTbuf;
	
	/** FFT object */
	private DoubleFFT_1D fft = null;
	
	/** Autrocorrelation values of Window*/
	double[] acfWindow;
	
	/**was a window applied*/
	boolean wasWindowed;
	
	/**were the acf values manually set*/
	boolean acfWindowManuallySet = false;
	
	/**
	 * Construct an AutoCorrelation object using the given source to read from.
	 * 
	 * @param source
	 */
	public ACF(FrameSource source) {
		this.source = source;
		this.initialize();
	}
	
	/**
	 * Construct an AutoCorrelation object using the given source to read from.
	 * 
	 * @param source
	 * @param acfWindow the autocorrelation of the windowing function
	 */
	public ACF(FrameSource source, double[] acfWindow) {
		this.source = source;
		this.acfWindow = new double[acfWindow.length];
		System.arraycopy(acfWindow, 0, this.acfWindow, 0, acfWindow.length);
		acfWindowManuallySet = true;
		wasWindowed = true;
		this.initialize();
	}
	
	public int getFrameSize() {
		return fsOut;
	}

	public FrameSource getSource() {
	    return source;
	}

	private void initialize() {
		// init internal buffer
		fsIn = source.getFrameSize();
		bufRead = new double [fsIn];
		
		// was a window function applied
		if(acfWindowManuallySet){
			wasWindowed = true;
		}
	
		//searching for power of 2 for FFT length
		double log2L = Math.log(fsIn)/Math.log(2);
		int exp = (int)Math.ceil(log2L);
		fsFFT = 2*(int)Math.pow(2., exp);
		fsFFTbuf = 2*fsFFT;
		
		fft = new DoubleFFT_1D(fsFFT);
		fsOut = fsIn;
	}
	
	/**
	 * Reads from the FrameSource and computes the autocorrelation using the FFT
	 * ACF = IFFT ( |FFT|^2 )
	 * */
	public boolean read(double[] buf) throws IOException {
		if (!source.read(bufRead))
			return false;
		
		bufFFT = new double [fsFFTbuf];	
		System.arraycopy(this.bufRead, 0, bufFFT, 0, fsIn);
		
		// compute FFT
		fft.realForwardFull(bufFFT);

		// setting absolute values as real part and zero for imaginary part
		// <-> compute power spectrum 
		for (int i = 0; i < fsFFTbuf/2; i++){
			double tmp;
			tmp = bufFFT[2*i]*bufFFT[2*i] + bufFFT[2*i+1]*bufFFT[2*i+1];
			bufFFT[2*i] = tmp;
			bufFFT[2*i+1]	= 0.;
		}

		// compute inverse FFT
		fft.complexInverse(bufFFT, true);
		
		// get the real part
		// divide first half! by ACF of window if FrameSource source was a Window
		double firstValue = bufFFT[0];
		if(wasWindowed){
			firstValue /= acfWindow[0];
			for (int i = 0; i < (int)(fsOut*0.5); i++)
				buf[i] = bufFFT[2*i]/firstValue/acfWindow[i];
			for (int i = (int)(fsOut*0.5); i < fsOut; i++)	
				buf[i] = bufFFT[2*i]/firstValue;		
		}else{
			buf[0] = bufFFT[0];
			for (int i = 0; i < fsOut; i++){	
				buf[i] = bufFFT[2*i]/firstValue;
			}
		}
		return true;
	}
	
	public double[] getReadBuf(){
		return bufRead;
	}
	
	public int getReadBufFrameSize(){
		return bufRead.length;
	}
	
	public static void main(String [] args) throws Exception {
		long duration = 100000L;
		SineGenerator as = new SineGenerator(duration);
		System.err.println(as.toString());
		
		Window w = new RectangularWindow(as, 50, 10);
		System.err.println(w);
		FrameSource acf = new ACF(w);
		System.err.println(acf);
		double [] bufACF = new double [acf.getFrameSize()];
		
		long diffACF, diffAC, start, end;
		
		start 	= System.currentTimeMillis();
		while(true){
			if(!acf.read(bufACF))
				break;
			//System.out.println(Arrays.toString(bufACF));
		}
		end	= System.currentTimeMillis();
		diffACF = end-start;
		System.out.println("ACF.java exec. time: "+diffACF+" ms");
		
		as = new SineGenerator(duration);
		System.err.println(as.toString());
		w = new RectangularWindow(as, 50, 10);
		System.err.println(w);
	
		FrameSource ac = new AutoCorrelation(w);
		double [] bufAC = new double [ac.getFrameSize()];
		start 	= System.currentTimeMillis();
		while(true){
			if(!ac.read(bufAC))
				break;
		}
		end	= System.currentTimeMillis();
		diffAC = end-start;
		System.out.println("Autocorrelation.java  exec. time: "+diffAC+" ms");
		
		System.exit(0);
	}	

}
