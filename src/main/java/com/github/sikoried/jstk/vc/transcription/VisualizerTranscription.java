/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Stefan Steidl

	This file is part of the Java Speech Toolkit (JSTK).

	The JSTK is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	The JSTK is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/
package com.github.sikoried.jstk.vc.transcription;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Vector;

import com.github.sikoried.jstk.io.BufferedAudioSource;
import com.github.sikoried.jstk.vc.FileVisualizer;
import com.github.sikoried.jstk.vc.interfaces.WordClickListener;
import com.github.sikoried.jstk.vc.interfaces.WordDblClickListener;
import com.github.sikoried.jstk.vc.interfaces.WordHighlightedListener;


public class VisualizerTranscription extends FileVisualizer {

	private static final long serialVersionUID = 5323804942999875591L;

	public int d0 = 10; // distance between transcription and x-axis
	public int h0 = 20; // height of transcription bars
	public Color colorText = Color.BLACK;
	public Color colorTextSelection = Color.WHITE;

	private Transcription transcription;
	private int currentEntry = 0;
	
	private Vector<WordClickListener> wordClickListeners;
	private Vector<WordDblClickListener> wordDblClickListeners;
	private Vector<WordHighlightedListener> wordHighlightedListeners;

	public VisualizerTranscription(String name, BufferedAudioSource source) {
		super(name, source);
		yMin = 0;
		yMax = 1.0;
		ytics = 2.0;
		colorSignal = colorBackgroundHighlightedSection;
		colorBackgroundSelectedArea = new Color(245, 245, 245);
		border_top = 0;
		wordClickListeners = new Vector<WordClickListener>();
		wordDblClickListeners = new Vector<WordDblClickListener>();
		wordHighlightedListeners = new Vector<WordHighlightedListener>();
	}

	@Override
	protected void drawHighlightedArea(Graphics g) {
		// highlighted areas are part of the transliteration blocks
	}

	@Override
	protected void drawSignal(Graphics g) {
		if (transcription == null) {
			return;
		}
		Iterator<TranscriptionEntry> iterator = transcription.getIterator();
		int y = getHeight() - border_bottom - d0 - h0;
		Font font = g.getFont();
		FontMetrics metrics = g.getFontMetrics(font);
		int height = metrics.getAscent() + 1;
		int range_start = convertXtoPX(highlightedSectionStartSample);
		int range_end = convertXtoPX(highlightedSectionEndSample);
		while (iterator.hasNext()) {
			TranscriptionEntry entry = iterator.next();
			int x0 = convertXtoPX(entry.startSample);
			int x1 = convertXtoPX(entry.endSample);
			if (x0 < border_left) {
				x0 = border_left;
			}
			int width = metrics.stringWidth(entry.word);

			if ((x0 > range_end) || (x1 < range_start)
					|| (!showHighlightedSection)) {
				// outside the highlighted region
				g.setColor(colorSignal);
				g.fillRect(x0, y, x1 - x0 + 1, h0);
				g.setColor(colorText);
				g.drawLine(x0, y, x0, y + h0 - 1);
				g.drawLine(x1, y, x1, y + h0 - 1);
			} else if ((x0 >= range_start) && (x1 <= range_end)) {
				// completely inside the highlighted region
				g.setColor(colorHighlightedSignal);
				g.fillRect(x0, y, x1 - x0 + 1, h0);
				g.setColor(colorTextSelection);
				g.drawLine(x0, y, x0, y + h0 - 1);
				g.drawLine(x1, y, x1, y + h0 - 1);
			} else if (x0 < range_start) {
				// overlap to the right
				g.setColor(colorSignal);
				g.fillRect(x0, y, range_start - x0 + 1, h0);
				g.setColor(colorHighlightedSignal);
				g.drawLine(x0, y, x0, y + h0 - 1);
				g.fillRect(range_start, y, x1 - range_start + 1, h0);
				if (range_start - x0 > x1 - range_start) {
					g.setColor(colorText);
				} else {
					g.setColor(colorTextSelection);
				}
				g.drawLine(x1, y, x1, y + h0 - 1);
			} else {
				// overlap to the left
				g.setColor(colorHighlightedSignal);
				g.fillRect(x0, y, range_end - x0, h0);
				g.setColor(colorSignal);
				g.drawLine(x0, y, x0, y + h0 - 1);
				g.fillRect(range_end, y, x1 - range_end + 1, h0);
				if (range_end - x0 > x1 - range_end + 1) {
					g.setColor(colorTextSelection);
				} else {
					g.setColor(colorText);
				}
				g.drawLine(x1, y, x1, y + h0 - 1);
			}

			if (width <= (x1 - x0)) {
				g.drawString(entry.word, (x0 + x1 - width) / 2, y + height);
			}
		}

		// draw vertical lines to show selected area
		if (isSelected) {
			g.setColor(colorSelectedSignal);
			int x1 = convertXtoPX(selectedSectionStartSample);
			int x2 = convertXtoPX(selectedSectionEndSample);
			g.drawLine(x1, border_top, x1, getHeight() - border_bottom);
			g.drawLine(x2, border_top, x2, getHeight() - border_bottom);
		}
	}

	@Override
	protected void recalculate() {
		// nothing to do
	}

	public void setTranscription(Transcription transcription, BufferedAudioSource source) {
		this.transcription = transcription;
		this.audiosource = source;
		if (audiosource != null) {
			transcription.maxSamples = audiosource.getBufferSize();
		}
		if (transcription != null) {
			currentEntry = 0;
			TranscriptionEntry entry = transcription.get(currentEntry);
			if (entry != null) {
				setHighlightedRegion(entry.startSample, entry.endSample, ALIGN_NONE, 0);
			} else {
				isHighlighted = false;
			}
			draw();
			repaint();
			informVisualizationListeners();
			informWordHighlightedListeners();
		}
	}

	public String getWord() {
		if (transcription == null) {
			return null;
		}
		if (currentEntry < transcription.countWords()) {
			return transcription.get(currentEntry).word;
		} else {
			return "";
		}
	}
	
	public void nextWord() {
		if (transcription == null) {
			return;
		}
		if (currentEntry + 1 < transcription.countWords()) {
			currentEntry++;
			TranscriptionEntry entry = transcription.get(currentEntry);
			TranscriptionEntry next = transcription.get(currentEntry + 1);
			int sample = (int) xMax;
			if (next != null) {
				sample = next.endSample;
			}
			setHighlightedRegion(entry.startSample, entry.endSample,
					ALIGN_RIGHT, sample);
			informWordHighlightedListeners();
		}
	}

	public void previousWord() {
		if (transcription == null) {
			return;
		}
		if (currentEntry > 0) {
			currentEntry--;
			TranscriptionEntry entry = transcription.get(currentEntry);
			TranscriptionEntry previous = transcription.get(currentEntry - 1);
			int sample = 0;
			if (previous != null) {
				sample = previous.startSample;
			}
			setHighlightedRegion(entry.startSample, entry.endSample,
					ALIGN_LEFT, sample);
			informWordHighlightedListeners();
		}
	}

	public void setWord(String newWord) {
		if (transcription == null) {
			return;
		}
		if (transcription.countWords() > 0) {
			transcription.get(currentEntry).word = newWord;
			draw();
			repaint();
		}
	}

	public void insertWord(String word, int from, int to) {
		if (transcription == null) {
			return;
		}
		if (from < 0) {
			from = 0;
		}
		if (to >= (int) xMax) {
			to = (int) xMax;
		}
		from = (from / 160) * 160;
		to = (to / 160) * 160 - 1;
		try {
			currentEntry = transcription.insert(word, from, to);
			setHighlightedRegion(from, to, ALIGN_NONE, 0);
			draw();
			repaint();
			System.err.println("word inserted at position " + currentEntry);
			informWordHighlightedListeners();
		} catch (TranscriptionOverlappingEntriesException e) {
			System.err.println(e);
		}
	}

	public void deleteWord() {
		if ((transcription == null) || (transcription.countWords() == 0)) {
			return;
		}
		transcription.delete(currentEntry);
		if (currentEntry > 0) {
			previousWord();
		} else if (transcription.countWords() > 0) {
			TranscriptionEntry entry = transcription.get(currentEntry);
			TranscriptionEntry next = transcription.get(currentEntry + 1);
			int sample = (int) xMax;
			if (next != null) {
				sample = next.endSample;
			}
			setHighlightedRegion(entry.startSample, entry.endSample,
					ALIGN_RIGHT, sample);
		} else {
			setHighlightedRegion(0, 0, ALIGN_LEFT, 0);
			isHighlighted = false;
		}
		draw();
		repaint();
		informWordHighlightedListeners();
	}

	public void moveRightBorder(int samples) {
		if ((transcription == null) || (transcription.countWords() == 0)) {
			return;
		}

		int oldEndSample = transcription.get(currentEntry).endSample;
		int newEndSample = transcription.moveRightBorder(currentEntry, samples);

		if (oldEndSample != newEndSample) {
			highlightedSectionEndSample = newEndSample;
			draw();
			repaint();
			informVisualizationListeners();
		}
	}

	public void moveLeftBorder(int samples) {
		if ((transcription == null) || (transcription.countWords() == 0)) {
			return;
		}

		int oldStartSample = transcription.get(currentEntry).startSample;
		int newStartSample = transcription
				.moveLeftBorder(currentEntry, samples);

		if (oldStartSample != newStartSample) {
			highlightedSectionStartSample = newStartSample;
			draw();
			repaint();
			informVisualizationListeners();
		}
	}

	public void connect(int neighbour) {
		if ((transcription == null) || (transcription.countWords() == 0)) {
			return;
		}
		transcription.connect(currentEntry, currentEntry + neighbour);
		TranscriptionEntry entry = transcription.get(currentEntry);
		setHighlightedRegion(entry.startSample, entry.endSample, ALIGN_NONE, 0);
		draw();
		repaint();
	}

	public void disconnect(int neighbour) {
		if ((transcription == null) || (transcription.countWords() == 0)) {
			return;
		}
		transcription.disconnect(currentEntry, currentEntry + neighbour);
	}

	public int wordAt(int x, int y) {
		int y0 = getHeight() - border_bottom - d0;
		if ((x < border_left) || (x > getWidth() - border_right)
				|| (y < y0 - h0) || (y > y0)) {
			return -1;
		}

		int sample = (int) convertPXtoX(x);
		int idx = 0;
		Iterator<TranscriptionEntry> iterator = transcription.getIterator();
		while (iterator.hasNext()) {
			TranscriptionEntry entry = iterator.next();
			if ((sample >= entry.startSample) && (sample <= entry.endSample)) {
				return idx;
			}
			idx++;
		}

		return -1;
	}
	
	public void addWordClickListener(WordClickListener listener) {
		wordClickListeners.add(listener);
	}
	
	public void addWordDblClickListener(WordDblClickListener listener) {
		wordDblClickListeners.add(listener);
	}
	
	public void addWordHighlightedWordListener(WordHighlightedListener listener) {
		wordHighlightedListeners.add(listener);
	}
	
	public void informWordClickListeners() {
		Iterator<WordClickListener> iterator = wordClickListeners.iterator();
		while (iterator.hasNext()) {
			WordClickListener listener = iterator.next();
			listener.wordClicked();
		}
	}

	public void informWordDblClickListeners() {
		Iterator<WordDblClickListener> iterator = wordDblClickListeners.iterator();
		while (iterator.hasNext()) {
			WordDblClickListener listener = iterator.next();
			listener.wordDblClicked();
		}
	}
	
	public void informWordHighlightedListeners() {
		Iterator<WordHighlightedListener> iterator = wordHighlightedListeners.iterator();
		while (iterator.hasNext()) {
			WordHighlightedListener listener = iterator.next();
			listener.wordHighlighted();
		}
	}
	
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			int idx = wordAt(arg0.getX(), arg0.getY());
			if (idx >= 0) {
				boolean highlightedWordChanged = false;
				if (currentEntry != idx) {
					highlightedWordChanged = true;
				}
				if (arg0.getClickCount() == 1) {
					currentEntry = idx;
					TranscriptionEntry entry = transcription.get(currentEntry);
					setHighlightedRegion(entry.startSample, entry.endSample,
							ALIGN_CENTER, 0);	
					informWordClickListeners();
				} else {
					informWordDblClickListeners();
				}
				if (highlightedWordChanged) {
					informWordHighlightedListeners();	
				}
			}
		}
		super.mouseClicked(arg0);
	}

	@Override
	public String toString() {
		return "VisualizerTranscription '" + name + "'";
	}
}
