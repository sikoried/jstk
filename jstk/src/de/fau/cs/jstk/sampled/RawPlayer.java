package de.fau.cs.jstk.sampled;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class RawPlayer implements Runnable, LineListener{
	
	public interface PlayEventListener {
		// actual playback starts
		public void playbackStarted(RawPlayer instance);
		// actual playback stops (was actively stopped or ais is at its and, and has been playback stops now)
		public void playbackStopped(RawPlayer instance);		
	}	

	private Set<PlayEventListener> dependents = new HashSet<PlayEventListener>();
	
	Thread thread;
	
	boolean firsttime = true;
	
	boolean stopped = false;
	
	AudioInputStream ais;
	
	SourceDataLine line;
	
	double desiredBufSize;
	
	Mixer.Info mixer = null;
	
	boolean stressTestEnabled = false;	
	double activeSleepRatio;
	
	private int factor_buffer_smaller = 16;

	private Thread shutdownHook = null;
	
	RawPlayer(AudioInputStream ais){
		this(ais, null, 0.0);		
	}

	RawPlayer(AudioInputStream ais, String mixerName){
		this(ais, mixerName, 0.0);
	}
	
	public void dispose(){
		stopPlaying();
		
		if (line != null)
			line.removeLineListener(this);
		line = null;
		
		if (dependents != null)
			dependents.clear();		
		dependents = null;
		
		thread = null;
		ais = null;
		
		mixer = null;
		if (shutdownHook != null)
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		shutdownHook = null;
	}
	
	/**
	 * set up player. no lines are occupied until start() is called.
	 * @param ais
	 * @param mixerName
	 * @param desiredBufSize in seconds, determines latency when stop()ing: currently,
	 * stopping cannot begin before desiredBufSize, and afterwards only with a granularity of
	 *  desiredBufSize / factor_buffer_smaller.
	 */
	public RawPlayer(AudioInputStream ais, String mixerName, double desiredBufSize){
		this.ais = ais;
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
	
	public void addStateListener(PlayEventListener client) {
		dependents.add(client);
	}
	
	public void removeStateListener(PlayEventListener client) {
		dependents.remove(client);
	}
	
	private void notifyStart() {
		System.err.println("notifyStart for " + dependents.size());
		for (PlayEventListener s : dependents){
			System.err.println("notify " + s);
			s.playbackStarted(this);
		}
	}
	
	private void notifyStop() {
		System.err.println("notifyStop for " + dependents.size());
		for (PlayEventListener s : dependents){
			System.err.println("notify " + s);
			s.playbackStopped(this);
		}
	}
	
	/**
	 * stress-test this component: sleep (actively) *after* reading data from ais.
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
		Runtime.getRuntime().addShutdownHook(shutdownHook  = new Thread(){
			public void run() {
				System.err.println("Inside Shutdown Hook: stopping player...");
				stopPlaying();
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
	
	public void stopPlaying(){
		if (stopped)
			return;
		
		stopped = true;
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// TODO: remove Listeners?
	}

	@Override
	public void run() {
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
		if (!AudioSystem.isLineSupported(info)) {
			System.err.println("Not supported line: " + info);
			stopPlaying();
			return;
        }
		 
		try {
			line = (SourceDataLine) AudioSystem.getMixer(mixer).getLine(info);

			if (desiredBufSize != 0.0)			
				line.open(ais.getFormat(), 
						(int)Math.round(desiredBufSize * ais.getFormat().getFrameRate() * ais.getFormat().getFrameSize()));
			else
				line.open(ais.getFormat());				
			
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			stopPlaying();
			return;
		}		
		System.err.println("Bufsize = " + line.getBufferSize());

		byte [] buffer = new byte[line.getBufferSize()];
		int partialBufferSize = buffer.length / factor_buffer_smaller;
				
		line.addLineListener(this);
		line.flush();
		line.start();		
		
		// main playback loop
		while(!stopped){
			int numBytesRead = 0;
			try {
				if (firsttime){ // read+write a whole buffer
					numBytesRead = ais.read(buffer);
					firsttime = false;
				}
				else /* 
					 * read+write partial buffer (why? see http://download.oracle.com/javase/tutorial/sound/capturing.html)
					 * also, stress testing has confirmed that scheme (see enableStressTest)
					 * */				 
					numBytesRead = ais.read(buffer, 0, partialBufferSize);
					
			} catch (IOException e) {				
				e.printStackTrace();
				stopPlaying();				
			}			
		
			if (stressTestEnabled){
				long nanoSleep = (long) (activeSleepRatio * numBytesRead / ais.getFormat().getFrameRate() / ais.getFormat().getFrameSize() * 1000000000.0);
				
				// simulate busy system by active waiting
				long startTime = System.nanoTime();
				System.err.println("numBytesRead = " + numBytesRead);
				System.err.println("available = " + line.available());
				System.err.println("nanoSleep = " + nanoSleep);

				while ((System.nanoTime() - startTime) < nanoSleep);
			}						
			
			if (numBytesRead < 0)
				break;
			//System.err.println("available = " + line.available());

			line.write(buffer, 0, numBytesRead);	
			
		}
		
		if (!stopped)
			line.drain();
		line.stop();
		if (stopped){ 
			line.flush();			
		}
		line.close();
		
		stopped = true;		
				
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
	
	public boolean hasStopped(){
		return stopped;
	}
	
	public static void main(String [] args){
		Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
	
		for (Mixer.Info m : availableMixers)
			System.err.println(m.getName());
		
		String mixer = null;
		
		System.err.println("usage: < IN.wav [mixer]");
		
		if (args.length > 0)
			mixer = args[0];		
		
		AudioInputStream ais = null;
		
		InputStream is = System.in;
		
		try {
			ais = AudioSystem.getAudioInputStream(is);
		} catch (UnsupportedAudioFileException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}				
		
		System.err.println("format = " + ais.getFormat());
		
		final RawPlayer player = new RawPlayer(ais, mixer, 0.1);
		
		// "Intel [plughw:0,0]" works much better than "Java Sound Audio Engine". 
		// And the latter from time to time refuses to put out anything
		//player.enableStressTest(0.98);
		
		/* outdated 
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				System.err.println("Inside Add Shutdown Hook");
				player.stopPlaying();
				System.err.println("player stopped");
			}			
		});		
		*/
		
		player.start();		
		
		player.join();
		System.err.println("joined");

	}
}
