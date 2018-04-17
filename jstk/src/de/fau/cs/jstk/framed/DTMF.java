package de.fau.cs.jstk.framed;

import de.fau.cs.jstk.io.FrameOutputStream;
import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.RawAudioFormat;
import de.fau.cs.jstk.util.Arithmetics;
import de.fau.cs.jstk.util.Various;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * DTMF tone feature extraction, see
 * https://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling
 * https://www.audiocheck.net/audiocheck_dtmf.php (generator)
 */
public class DTMF implements FrameSource {
	private static Logger logger = Logger.getLogger(DTMF.class);

	private static final String DIAL_PAD = "123A456B789C*0#D";
	private static final int[] FREQS = {697, 770, 852, 941, 1209, 1336, 1477, 1633};

	private FFT fft;
	private Selection sel;
	private double[] buf;

	public DTMF(FFT fft) {
		this.fft = fft;
		// select energy
		int[] indices = new int[FREQS.length];
		for (int i = 0; i < FREQS.length; i++)
			indices[i] = (int) Math.round((double) FREQS[i] / fft.getResolution());

		logger.info("FFT resolution (Hz/bin) = " + fft.getResolution());
		logger.info(Arrays.toString(FREQS));
		logger.info(Arrays.toString(indices));

		this.sel = new Selection(fft, indices);
		this.buf = new double [this.sel.getFrameSize()];
	}

	public static List<Triple<Character, Integer, Integer>> synthesize(String str) {
		List<Triple<Character, Integer, Integer>> li = new LinkedList<>();
		for (char c : str.toCharArray()) {
			int i = DIAL_PAD.indexOf(c);

			li.add(Triple.of(c, FREQS[i/4], FREQS[4 + i%4]));
		}
		return li;
	}

	public char decoded;

	@Override
	public boolean read(double[] buf) throws IOException {
		// read FFT, select bins.
		boolean res = sel.read(this.buf);

		// make sum to one
		buf[0] = fft.getRawSpectralEnergy();
		if (buf[0] > 1e-6) {
			Arithmetics.makesumto1(this.buf);

			// find max, decode
			int[] mp = new int[2];
			double[] mv = new double [2];
			Various.max(this.buf, mp, mv);

			// lower and upper bin (freq)
			int lf = Math.min(mp[0], mp[1]);
			int uf = Math.max(mp[0], mp[1]);

			// re-map to dialpad
			decoded = DIAL_PAD.charAt(lf * 4 + (uf % 4));
		} else {
			decoded = '-';
		}

		System.arraycopy(this.buf, 0, buf, 1, this.buf.length);
		return res;
	}

	@Override
	public int getFrameSize() {
		return sel.getFrameSize() + 1;
	}

	@Override
	public FrameSource getSource() {
		return fft;
	}

	public static final String SYNOPSIS =
			"sikoried, 4/20/2010\n" +
					"Compute the FFT given a format, file and window description; select DTMF bins Output is ASCII\n" +
					"if no output-file is given.\n\n" +
					"DTMF symbol to frequency pair map: " + synthesize(DIAL_PAD) + "\n\n" +
					"usage: framed.DTMF <in-file> [out-file]";

	public static void main(String [] args) throws Exception {
		BasicConfigurator.configure();

		if (args.length < 1 || args.length > 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		String sWindow = "hamm,25,10";
		String inFile = args[0];
		String outFile = (args.length > 1 ? args[1] : null);

		AudioSource as = new de.fau.cs.jstk.sampled.AudioFileReader(inFile, RawAudioFormat.create("f:"+inFile), true);
		Window w = Window.create(as, sWindow);
		FFT fft = new FFT(w);

		DTMF fs = new DTMF(fft);

		FrameOutputStream fw = (outFile == null ? null : new FrameOutputStream(fs.getFrameSize(), new File(outFile)));

		double [] buf = new double [fs.getFrameSize()];

		while (fs.read(buf)) {
			if (fw != null)
				fw.write(buf);
			else {
				int i = 0;
				for (; i < buf.length-1; ++i)
					System.out.printf("%.2f ", buf[i]);
				System.out.printf("%.2f %c\n", buf[i], fs.decoded);
			}
		}

		if (fw != null)
			fw.close();
	}
}
