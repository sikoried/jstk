package de.fau.cs.jstk.util;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * simple class for creating a log-file
 * 
 *  @author siniwitt
 *  
 *  TODO: remove/substitute by log4j
 */
public class LogFile {
	
	
	private BufferedWriter writer;
	
	/**
	 * Constructs a log file object
	 */
	public LogFile(String fileName){
		FileWriter fstream = null;;
		try {
			fstream = new FileWriter(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer = new BufferedWriter(fstream);
	}
	
	/**
	 * writing to the log-file like System.out.println(logLine)
	 */
	public void println(String logLine){
		try {
			writer.write(logLine);
			writer.write(System.getProperty("line.separator"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * do not forget to close the log-file 
	 */
	public void close(){
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
