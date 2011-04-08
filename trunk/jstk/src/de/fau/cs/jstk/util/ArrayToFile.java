package de.fau.cs.jstk.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * class for writing array values to an ascii output file
 * 
 * @author siniwitt
 * 
 * TODO: is this class used anywhere?
 *
 */
public class ArrayToFile {
	public static void write(String outputFile, double array[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * appends values to an output file
	 */
	public static void append(String outputFile, double array[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** 
	 * writes double array values (y-values) in first column and 
	 *  xtics (with xtics = xScaling*(0:array.length-1)) in second column to file
	 */
	public static void write(String outputFile, double array[], double xScaling){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write((double)i*xScaling+"\t"+new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * writes x and y axis double values 
	 */
	public static void writeXY(String outputFile, double array[], double xaxis[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Double(xaxis[i]).toString()+"\t"+new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * writes double y-axis values and integer x-axis values 
	 */
	public static void writeXY(String outputFile, double array[], int xaxis[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Integer(xaxis[i]).toString()+"\t"+new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writeLabeled(String outputFile, double array[], double label[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Integer((int)label[i]).toString()+"\t"+new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void appendLabeled(String outputFile, double array[], double label[]){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(outputFile, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i = 0; i < array.length; i++){
			try {
				out.write(System.getProperty("line.separator") );
				out.write(new Integer((int)label[i]).toString()+"\t"+new Double(array[i]).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
