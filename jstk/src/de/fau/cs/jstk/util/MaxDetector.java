package de.fau.cs.jstk.util;

import java.util.Arrays;

public class MaxDetector {
	
	private int numCandidates;
	
	private int fs;
	
	private double[] result;
	
	/**
	 *  Create new MaxDetector object that finds local maxima in an array
	 *  @param array the array to search for maxima
	 *  @param numCandidates the number of maxima to search for
	 *  @param startIndex the index to start searching
	 *  @param endIndex the index to stop searching
	 *  @param doInterpolation do a parabolic interpolation of the indices and values of the maxima
	 *  
	 *  @authors siniwitt, sikoried
	 */
	public MaxDetector(double[] array, int numCandidates, int startIndex, int endIndex, boolean doInterpolation){
		this.numCandidates = numCandidates;
		fs = 2*numCandidates;
		result = new double[fs];
		if(startIndex < 0)
			startIndex = 0;
		if(endIndex > array.length - 1)
			endIndex = array.length - 1;
		findMaxima(array, startIndex, endIndex, doInterpolation);
	}
	
	/**
	 * @return returns an array containing the indices of the found maxima
	 */
	public double[] getMaxIndices(){
		double[] result = new double[numCandidates];
		for(int i = 0; i < numCandidates; i++)
			result[i] = this.result[2*i];
		return result;
	}
	
	/**
	 * @return returns an array containing the values of the found maxima
	 */
	public double[] getMaxValues(){
		double[] result = new double[numCandidates];
		for(int i = 0; i < numCandidates; i++)
			result[i] = this.result[2*i+1];
		return result;
	}
	
	/**
	 * finds multiple local maxima in an array.
	 * The array can be searched in a limited area.
	 * @param array the array to search for maxima
	 * @param startIndex index to start searching
	 * @param endIndex index to end searching 
	 * @param do a parabolic interpolation of the maxima indices and values 
	 */
	private void findMaxima(double[] array, int startIndex, int endIndex, boolean doInterpolation){
		// indices of the maxima
		double[] maxIndices = new double[numCandidates];
		// reset maxima
		double[] maxv 	= new double [numCandidates];
		//array values "before" boa[0], "on" boa[1] and "after" boa[2] the array's maximum
		double[] boa	= new double [3];
		//index distance of for boa[0] and boa[2] to index of the maximum boa[1] 
		int deltaBoa	= 1;
		
//		Arrays.fill(maxv, array[startIndex]);
		Arrays.fill(maxv, 0);
//		Arrays.fill(maxv, Double.MIN_VALUE);
		Arrays.fill(maxIndices, Double.NaN);
		
		// locate maxima
		for (int i = startIndex; i <= endIndex; ++i) {
			
			boa[1] = array[i];
			
			//for outer limits of the array
			if(i == startIndex){
				boa[0] = boa[1]+1;
			}else{
				boa[0] = array[i-deltaBoa];
			}
			if(i == endIndex){
				boa[2] = boa[1]+1;
			}else{
				boa[2] = array[i+deltaBoa];
			}
				
			// if we have a local max AND it's bigger than the smallest max
			// then add it to the candidate list
			if (boa[0] < boa[1] && boa[1] > boa[2] && boa[1] > maxv[0]){
				if (numCandidates == 1) {
					//Parabolic Interpolation
					if(doInterpolation == true){
						maxIndices[0] 	= parabolInterpIndex(i, boa);
						maxv[0]			= parabolInterpValue(boa); 
					}else{
						maxIndices[0] = i;
						maxv[0] = boa[1];
					}
				} else {
					// find the right position to insert
					int p = 0;
					while (p < numCandidates - 1 && boa[1] > maxv[p+1])
						p++;
					
					// shift the old maxima
					for (int j = 1; j <= p; ++j) { 
						maxv[j-1] = maxv[j]; 
						maxIndices[j-1] = maxIndices[j];
					}
					
					// insert the new max
					if(doInterpolation == true){
						maxIndices[p] 	= parabolInterpIndex(i, boa);
						maxv[p]			= parabolInterpValue(boa); 
					}else{
						maxIndices[p] = i;
						maxv[p] = boa[1];
					}
				}
			}
		}
		// sorting thus result[2*i]	contains the indices
		// 			and result[2*i+1] contains the values of the maxima	
		for(int i = 0; i < numCandidates; i++){
			this.result[2*i] 	= maxIndices[i];
			this.result[2*i+1] 	= maxv[i];
		}
	}
	
	/**
	 * does a one time parabolic interpolation of a maximum's index
	 * @param maxPos index of the maximum
	 * @param boa array values "before" boa[0], "on" boa[1] and "after" boa[2] the array's maximum
	 */
	private double parabolInterpIndex(int maxPos, double[] boa){
		double interpPos = 0;
		interpPos = maxPos + (0.5*(boa[2] - boa[0]))/
						(2*boa[1] - boa[0] - boa[2]);
		return interpPos;
	}
	
	/**
	 * does a one time parabolic interpolation of a maximum's value
	 * @param boa array values "before" boa[0], "on" boa[1] and "after" boa[2] the array's maximum
	 */
	private double parabolInterpValue(double[] boa){
		double interpPos = 0;
		interpPos = boa[1] + (boa[2] - boa[0])*(boa[2] - boa[0])/
						(8*(2*boa[1] - boa[0] - boa[2]));
		return interpPos;
	}
	
	/**
	 * @return the number of maxima 
	 */
	public int getFrameSize(){
		return numCandidates;
	}
	
	public static void main(String[] args){
//		double[] array = {800, 0.5, 42., 0.8, 420., 0.5, 0.3, 0.4, 4.2, 0.2, 0.42, 0.1, 1000};
		double[] array = {0.9, 0.61, 0.61, -0.8, 0.8, 0.3};
		MaxDetector detector = new MaxDetector(array, 2, 0, array.length-1, true);
		double[] indices = new double[detector.getFrameSize()];
		double[] values	 = new double[detector.getFrameSize()];
		indices = detector.getMaxIndices();
		values  = detector.getMaxValues();
		System.out.println(detector.getFrameSize()+" maxima found at array position:");
		System.out.println(Arrays.toString(indices));
		System.out.println("the values of the maxima are:");
		System.out.println(Arrays.toString(values));
	}
}
