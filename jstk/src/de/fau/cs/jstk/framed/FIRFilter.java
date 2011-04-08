package de.fau.cs.jstk.framed;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.SineGenerator;
import de.fau.cs.jstk.util.Convolution;


public class FIRFilter implements FrameSource {
		
	/**Convolution object*/
	private Convolution conv;
	
	/**number of filter coefficients*/
	private int filterLength;
	
	/**filter coefficients*/
	private double[] filter;
	
	/**buffer holding the signal samples*/
	private double[] bufSig;
	
	/**buffer holding the convolution result*/
	private double[] bufConvRes;
	
	/**buffer holding the overlapping convolution result*/
	private double[] bufOverlap;
	
	/**Convolution object*/
	private Convolution convolution;
	
	/**output frame size*/
	private int fsOut;
	
	/**window to read from*/
	private FrameSource window;
	
	/**contains the acf values of the applied window*/
	private double[] acfOfWindow;
	
	/**
	 * framewise filtering if framewise == true for overlapping windows
	 * -> read() returns only the valid convolution result
	 * framewise == false for use with non-overlapping! windows (e.g for filtering whole files)
	 * -> read() returns frames computed with overlapp-add method 
	 */
	private boolean framewise;
	
	public FIRFilter(FrameSource window, String CoeffFile, boolean framewise) {
//		super();
		this.window = window;
		int fsIn = window.getFrameSize();
		this.framewise = framewise;
		bufSig = new double[fsIn];
		CoeffsToBuffer(CoeffFile);
		convolution = new Convolution(filter, fsIn);
//		ArrayToFile.write("Filt.txt", filter);
		bufConvRes 	= new double[bufSig.length + filterLength - 1];
		bufOverlap = new double[filterLength - 1];
		if(framewise == false){
			fsOut = fsIn;
		}else{
			fsOut = bufConvRes.length - filterLength - 1;
		}
	}
	
	public FIRFilter(FrameSource window, double[] filter, boolean framewise){
//		super();
		this.window = window;
		int fsIn = window.getFrameSize();
		this.framewise = framewise;
		bufSig = new double[fsIn];
		this.filter = filter;
		convolution = new Convolution(filter, fsIn);
//		ArrayToFile.write("Filt.txt", filter);
		bufConvRes 	= new double[bufSig.length + filterLength - 1];
		bufOverlap = new double[filterLength - 1];
		if(framewise == false){
			fsOut = fsIn;
		}else{
			fsOut = bufConvRes.length - filterLength - 1;
		}
	}

	@Override
	public int getFrameSize() {
		return fsOut;
	}

	public FrameSource getSource() {
	    return window;
	}

	@Override
	public boolean read(double[] buf) throws IOException {
		if (!window.read(bufSig))
			return false;
		Arrays.fill(buf, 0.0);
//		ArrayToFile.write("filterTestsig.txt", bufSig);
		conv = new Convolution(bufSig, filter);
		conv.computeResult(bufConvRes);
		
		if(framewise == false){
			//adding the overlapping result
			for(int i = 0; i < filterLength - 1; i++){
				buf[i] = bufConvRes[i] + bufOverlap[i];
			}
	
			System.arraycopy(bufConvRes, fsOut, bufOverlap, 0, bufOverlap.length);
			//filling the non-overlapping result.
			System.arraycopy(bufConvRes, filterLength-1, buf, filterLength-1, fsOut-filterLength+1);
		}else{
			System.arraycopy(bufConvRes, filterLength-1, buf, 0, fsOut);
		}

		return true;
	}
	
	/**
	 * reading the filter coefficients from a text file
	 */
	private void CoeffsToBuffer(String fileName) throws NumberFormatException{
		Vector<Double> doubleVec = new Vector<Double>(); 
		try{
			FileReader fileReader = new FileReader(fileName);
			BufferedReader reader = new BufferedReader(fileReader);
			String value;
			filterLength = 0;
			while((value = reader.readLine()) != null){
				doubleVec.add(new Double(value));
				filterLength++;
			}
			filter = new double[doubleVec.size()];
			for(int i = 0; i < filter.length; i++){
				filter[i] = doubleVec.get(i).doubleValue();
			}
		}catch(Exception e){
			if(e instanceof IOException){
				System.err.println("Could not read coeff file: "+fileName);
				System.exit(1);
			}else if(e instanceof FileNotFoundException){
				System.err.println("Could not find coeff file: "+fileName);
				System.exit(1);
			}else if(e instanceof NumberFormatException){
				System.err.println("The number format in: "+fileName+" does not match the expected number format");
				System.exit(1);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		
		double F0 = 100;
		double[] freq = new double[20];
		freq[0] = F0;
		for(int i = 1; i<freq.length; i++)
			freq[i] = freq[i-1]+F0;
			
		AudioSource as = new SineGenerator(10000L, freq);
		Window w = new RectangularWindow(as, 256,  256);
		FIRFilter wFiltered = new FIRFilter(w, "F:\\DA_Data\\h411000RIR.txt", false);
		System.out.println(w);
		
		long start, end, diff;
		
		double[] buf = new double[wFiltered.getFrameSize()];
		
		start = System.currentTimeMillis();
		int nFrames = 0;
		while(wFiltered.read(buf) == true){
//			ArrayToFile.write("filterTest.txt", buf);
			nFrames++;
		}	
		end = System.currentTimeMillis();
		diff = end-start;
		
		System.out.println("Frames read: "+nFrames);
		System.out.println("exec. time: "+diff+" ms");
		System.out.println("Time per Frame: "+Math.round((double)diff/(double)nFrames)+" ms");
		System.exit(0);
	}

}


