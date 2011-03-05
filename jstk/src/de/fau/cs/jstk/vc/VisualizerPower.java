package de.fau.cs.jstk.vc;

import java.awt.Graphics;

import de.fau.cs.jstk.framed.Window;
import de.fau.cs.jstk.io.BufferedAudioSource;

public class VisualizerPower extends FileVisualizer {

	private static final long serialVersionUID = 1733600843781446588L;

	public VisualizerPower(String name, BufferedAudioSource audiosource) {
		super(name, audiosource);
		yMin = 0;
		yMax = 100;
		ytics = 25;
	}

	@Override
	protected void recalculate() {
	}

	@Override
	protected void drawSignal(Graphics g) {
		// the constant C corrects the fact that the signal is in the range from -1 to +1
		// instead of -32768 to +32768 as in wavesurfer
		final double C1 = 300 * Math.log10(2);
		
		int width = getWidth() - border_left - border_right;
		int[] px = new int[width];
		int[] py = new int[width];
		final int windowLength = 20;
		int frameSize = windowLength * samplerate / 1000;
		final double C2 = 10 * Math.log10(frameSize);
		double[] frame = new double[frameSize];
		Window window = Window.create(audiosource, Window.RECTANGULAR_WINDOW, windowLength, 10);
		int i = 0;
		
		for (; i < width; i++) {
			int index = (int) (xMin + i * xPerPixel);
			for (int s = 0; s < frameSize; s++) {
				double v = audiosource.get(index);
				frame[s] = v;
				index++;
			}
			
			window.applyWindowToFrame(frame);

			double power = 0;
			for (int s = 0; s < frameSize; s++) {
				power += (frame[s] * frame[s]);
			}
			power = 10 * Math.log10(power) - C2 + C1;

			px[i] = border_left + i;
			py[i] = convertYtoPY(power);

			if (index > xMax) {
				break;
			}
		}
		g.setColor(colorSignal);
		g.drawPolyline(px, py, i);
	}
	
	@Override
	public String toString() {
		return "VisualizerPower '" + name + "'";
	}
}
