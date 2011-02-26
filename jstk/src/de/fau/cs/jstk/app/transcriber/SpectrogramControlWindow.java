package de.fau.cs.jstk.app.transcriber;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import de.fau.cs.jstk.vc.*;

public class SpectrogramControlWindow extends JFrame implements
		ContrastBrightnessListener {

	private static final long serialVersionUID = 7203314678810998437L;

	private JComboBox windowTypeComboBox;
	private JComboBox windowLengthComboBox;
	private JComboBox colorSpectrogramComboBox;
	private ContrastBrightnessControl contrastBrightnessControl;
	private WindowClosedListener mainWindow;
	private VisualizerSpectrogram spectrogramVisualizer;

	public SpectrogramControlWindow(Preferences preferences,
			WindowClosedListener listener,
			VisualizerSpectrogram spectrogramVisualizer) {
		super("Spectrogram control window", "spectrogramControlWindow",
				preferences);
		mainWindow = listener;
		this.spectrogramVisualizer = spectrogramVisualizer;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				hideWindow();
			}
		});

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		Box box = Box.createVerticalBox();
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Window type"));
		String[] windowTypes = { "Hamming", "Hann", "Rectangle" };
		windowTypeComboBox = new JComboBox(windowTypes);
		windowTypeComboBox.setSelectedIndex(0);
		// windowTypeComboBox.setEnabled(false);
		windowTypeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setWindowType();
			}
		});
		panel.add(windowTypeComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Window length"));
		String[] windowLengths = { "2", "4", "8", "16", "32", "64" };
		windowLengthComboBox = new JComboBox(windowLengths);
		windowLengthComboBox.setSelectedIndex(3);
		windowLengthComboBox.setEditable(true);
		// windowLengthComboBox.setEnabled(false);
		windowLengthComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setWindowLength();
			}
		});
		panel.add(windowLengthComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Color spectrogram"));
		String[] colors = { "color", "gray", };
		colorSpectrogramComboBox = new JComboBox(colors);
		colorSpectrogramComboBox.setSelectedIndex(1);
		colorSpectrogramComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setColor();
			}
		});
		panel.add(colorSpectrogramComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Brightness/contrast"));
		box.add(panel);

		panel = new JPanel();
		contrastBrightnessControl = new ContrastBrightnessControl();
		contrastBrightnessControl.addContrastBrightnessListener(this);
		panel.add(contrastBrightnessControl);
		box.add(panel);

		c.add(box, BorderLayout.NORTH);

		setAlwaysOnTop(true);
	}

	private void hideWindow() {
		setVisible(false);
		mainWindow.windowClosed(this);
	}

	private void setWindowType() {
		spectrogramVisualizer.setWindowFunction(windowTypeComboBox.getSelectedIndex() + 1);
	}

	private void setWindowLength() {
		spectrogramVisualizer.setWindowLength(Double.parseDouble(windowLengthComboBox.getSelectedItem().toString()));
	}

	private void setColor() {
		if (colorSpectrogramComboBox.getSelectedIndex() == 0) {
			spectrogramVisualizer.setColorSpectrogram(true);
		} else {
			spectrogramVisualizer.setColorSpectrogram(false);
		}
	}

	@Override
	public void contrastBrightnessChanged(double contrast, double brightness) {
		spectrogramVisualizer.setProperties(contrast, brightness);
	}

}
