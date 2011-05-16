package de.fau.cs.jstk.sampled;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class RawCapturer implements Runnable, LineListener{
	
	public interface CaptureEventListener {
		// actual recording starts
		public void captureStarted(RawCapturer instance);
		// actual recording stops (was actively stopped)
		public void captureStopped(RawCapturer instance);		
	}	

	private Set<CaptureEventListener> dependents = new HashSet<CaptureEventListener>();
	
	Thread thread;
	
	boolean firsttime = true;
	
	boolean stopped = false;
	
	OutputStream os;
	AudioFormat format;
	
	TargetDataLine line;
	
	double desiredBufSize;
	
	Mixer.Info mixer = null;
	
	boolean stressTestEnabled = false;	
	double activeSleepRatio;

	private int factor_buffer_smaller = 16;

	private Thread shutdownHook = null;
	
	RawCapturer(BufferedOutputStream os, AudioFormat format){
		this(os, format, null, 0.0);		
	}

	RawCapturer(BufferedOutputStream os, AudioFormat format, String mixerName){
		this(os, format, mixerName, 0.0);
	}
	
	/**
	 * set up recorder. no lines are occupied until start() is called.
	 * @param os
	 * @param mixerName
	 * @param desiredBufSize in seconds, determines latency when stop()ing: currently, 
	 * stopping can take about as long as desiredBufSize / factor_buffer_smaller.
	 */
	public RawCapturer(BufferedOutputStream os, AudioFormat format, String mixerName, double desiredBufSize){
		this.os = os;
		this.format = format;
		thread = new Thread(this);
		thread.setName("RawPlayer");
		this.desiredBufSize = desiredBufSize;
		
		if (mixerName != null){
			Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
			
			for (Mixer.Info m : availableMixers)
				if (m.getName().trim().equals(mixerName))
					mixer = m;
			if (mixer == null)
				System.err.println("could not find mixer " + mixerName);			
		}
	}
	
	public void dispose(){
		stopCapturing();
		
		line.removeLineListener(this);
		line = null;

		dependents.clear();
		dependents = null;
		
		thread = null;
		os = null;
		format = null;
		
		mixer = null;
		if (shutdownHook != null)
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		shutdownHook = null;
	}
	
	public void addStateListener(CaptureEventListener client) {
		dependents.add(client);
	}
	public void removeStateListener(CaptureEventListener client) {
		dependents.remove(client);
	}
	private void notifyStart() {
		System.err.println("notifyStart...");
		for (CaptureEventListener s : dependents)
			s.captureStarted(this);
	}
	private void notifyStop() {
		System.err.println("notifyStop...");
		for (CaptureEventListener s : dependents)
			s.captureStopped(this);
	}

	
	/**
	 * stress-test this component: sleep (actively) *after* writing data to os.
	 * sleeping time is relative to (the duration of) amount of data  activeSleepRatio: the ratio of time spent (actively) sleeping
	 * relative to the audio samples played back. 
	 * Use e.g. 0.95 for a hard stress test.
	 * 
	 * @param activeSleepRatio
	 */
	public void enableStressTest(double activeSleepRatio){
		stressTestEnabled = true;
		this.activeSleepRatio = activeSleepRatio;
	}
	
	public void start(){
		Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(){
			public void run() {
				System.err.println("Inside Shutdown Hook: stopping player...");
				stopCapturing();
				System.err.println("player stopped");
			}			
		});		
		thread.start();		
	}
	
	public void join(){
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stopCapturing(){
		if (stopped)
			return;
		
		stopped = true;
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public void run() {
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			System.err.println("Not supported line: " + info);
			stopCapturing();
			return;
        }
		 
		try {
			line = (TargetDataLine) AudioSystem.getMixer(mixer).getLine(info);

			if (desiredBufSize != 0.0)			
				line.open(format, 
						(int)Math.round(desiredBufSize * format.getFrameRate() * format.getFrameSize()));
			else
				line.open(format);				
			
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			stopCapturing();
			return;
		}		
		System.err.println("Bufsize = " + line.getBufferSize());
		
		System.err.println(String.format("desired bufsize = %f, actual = %f",
				desiredBufSize, line.getBufferSize() / format.getFrameRate() / format.getFrameSize()));				

		/* read+write partial buffer (why? see http://download.oracle.com/javase/tutorial/sound/capturing.html)
			 * also, stress testing has confirmed that scheme (see enableStressTest)
			 * */				 
		byte [] buffer = new byte[line.getBufferSize() / factor_buffer_smaller ];		
				
		line.addLineListener(this);
		line.flush();
		line.start();		
		
		// main playback loop
		while(!stopped){
			int numBytesRead = 0;
			
			numBytesRead = line.read(buffer, 0, buffer.length);												
			
			if (numBytesRead < 0)
				break;
			//System.err.println("available = " + line.available());
			try {
				os.write(buffer, 0, numBytesRead);
			} catch (IOException e) {				
				e.printStackTrace();				
				stopCapturing();	
			}
			if (stressTestEnabled){
				long nanoSleep = (long) (activeSleepRatio * numBytesRead / format.getFrameRate() / format.getFrameSize() * 1000000000.0);
				
				// simulate busy system by active waiting
				long startTime = System.nanoTime();
				System.err.println("numBytesRead = " + numBytesRead);
				System.err.println("available = " + line.available());
				System.err.println("nanoSleep = " + nanoSleep);

				while ((System.nanoTime() - startTime) < nanoSleep);
			}
		}
		
		if (!stopped)
			line.drain();
		line.stop();
		if (stopped) 
			line.flush();
		line.close();		

				
	}
	
	@Override
	public void update(LineEvent le) {		
		if (!le.getLine().equals(line))
			return;
		
		System.err.println(le);
		
		if (le.getType() == LineEvent.Type.START)
			notifyStart();
		else if (le.getType() == LineEvent.Type.STOP)
			notifyStop();						
	}	
	
	public static void main(String [] args){
		Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
	
		for (Mixer.Info m : availableMixers)
			System.err.println(m.getName());
		
		String mixer = null;
		
		System.err.println("usage: > IN.ssg [mixer]");
		
		if (args.length > 0)
			mixer = args[0];		
		
		
		BufferedOutputStream os = new BufferedOutputStream(System.out);
	
		AudioFormat format = new AudioFormat(32000, 16, 1, true, false);
			
		final RawCapturer capturer = new RawCapturer(os, format, mixer, 0.1);
		
		// "Intel [plughw:0,0]" works much better than "Java Sound Audio Engine". 
		// And the latter from time to time refuses to put out anything
		capturer.enableStressTest(0.99);
		
		/* outdated
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				System.err.println("Inside Add Shutdown Hook");
				capturer.stopCapturing();
				System.err.println("player stopped");
			}			
		});
		*/		
		
		capturer.start();		
		
		// i.e. forever, unless interrupted (see addShutdownHook above)
		capturer.join();
		System.err.println("joined");
	}
}

