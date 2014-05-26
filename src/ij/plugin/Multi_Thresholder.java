/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package ij.plugin;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.TrimmedButton;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.Recorder;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import ij.process.StackStatistics;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;


public class Multi_Thresholder extends PlugInFrame implements PlugIn,
		Measurements, Runnable, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;
	static final double defaultMinThreshold = 85;
	static final double defaultMaxThreshold = 170;
	static boolean fill1 = true;
	static boolean fill2 = true;
	static boolean useBW = true;
	static boolean backgroundToNaN = true;
	static String[] methodNames = AutoThresholder.getMethods();
	static String method = methodNames[0];

	static Frame instance;
	int bitDepth = 8;
	int hMin = 0;
	int hMax = 256;
	MultiThresholderPlot plot = new MultiThresholderPlot();
	Thread thread;
	boolean doRethreshold, doRefresh, doReset, doApplyLut, doSet, applyOptions,
			alreadyRecorded;

	Panel panel;
	Button refreshB, resetB, applyB, setB;
	int previousImageID;
	int previousImageType;
	ImageJ ij;
	double minThreshold, maxThreshold, threshold; // 0-255
	Label label1, label2, threshLabel;
	boolean done;
	boolean invertedLut;
	// int lutColor;
	static Choice choice;
	boolean firstActivation;
	boolean macro;
	String macroOptions;
	Checkbox darkBackground, stackHistogram;
	ImageStatistics stats;

	static final int RESET = 0, HIST = 1, APPLY = 2, THRESHOLD = 3,
			MIN_THRESHOLD = 4, MAX_THRESHOLD = 5, SET = 6, REFRESH = 7;

	/**
	 * Enables the user to use a variety of threshold algorithms to threshold
	 * the image.
	 **/
	public Multi_Thresholder() {
		super("MultiThresholder");
		macroOptions = Macro.getOptions();
		boolean macro = macroOptions != null;
		if (instance != null && !macro) {
			instance.toFront();
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		ij = IJ.getInstance();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			setup(imp);
			bitDepth = imp.getBitDepth();
		}
		if (macro) {
			instance = null;
			method = macroOptions;
			int index = method.indexOf(" ");
			if (index > 0)
				method = method.substring(0, index);
			if ("IsoData".equalsIgnoreCase(method))
				method = "IsoData";
			else if ("Entropy".equalsIgnoreCase(method)
					|| "Maximum Entropy".equalsIgnoreCase(method))
				method = "MaxEntropy";
			boolean found = false;
			for (int i = 0; i < methodNames.length; i++) {
				if (methodNames[i].equals(method)) {
					found = true;
					break;
				}
			}
			if (!found) {
				IJ.error("MultiThresholder", "\"" + method
						+ "\" method not found");
				close();
				return;
			}
			setThreshold(method);
			if (macroOptions.indexOf("apply") != -1)
				applyThreshold(imp);
			close();
			return;
		}
		Font font = new Font("SansSerif", Font.PLAIN, 10);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		// plot
		int y = 0;
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(10, 10, 0, 10);
		add(plot, c);

		// Threshold label
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy = y++;
		c.weightx = IJ.isMacintosh() ? 10 : 0;
		c.insets = new Insets(5, 0, 0, 10);
		threshLabel = new Label("Threshold:", Label.CENTER);
		threshLabel.setFont(font);
		add(threshLabel, c);

		// choice
		choice = new Choice();
		for (int i = 0; i < methodNames.length; i++)
			choice.addItem(methodNames[i]);
		choice.select(method);
		choice.addItemListener(this);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 0, 5);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		add(choice, c);

		// checkboxes
		darkBackground = new Checkbox("Dark Background");
		darkBackground.setState(false);
		darkBackground.addItemListener(this);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(5, 50, 0, 5);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		add(darkBackground, c);

		stackHistogram = new Checkbox("Stack Histogram");
		stackHistogram.setState(false);
		stackHistogram.addItemListener(this);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(0, 50, 0, 5);
		c.fill = GridBagConstraints.NONE;
		add(stackHistogram, c);

		// buttons
		int trim = IJ.isMacOSX() ? 11 : 0;
		panel = new Panel();
		applyB = new TrimmedButton("Apply", trim);
		applyB.addActionListener(this);
		applyB.addKeyListener(ij);
		panel.add(applyB);
		refreshB = new TrimmedButton("Refresh", trim);
		refreshB.addActionListener(this);
		refreshB.addKeyListener(ij);
		panel.add(refreshB);
		resetB = new TrimmedButton("Reset", trim);
		resetB.addActionListener(this);
		resetB.addKeyListener(ij);
		panel.add(resetB);
		setB = new TrimmedButton("Set", trim);
		setB.addActionListener(this);
		setB.addKeyListener(ij);
		panel.add(setB);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(0, 5, 10, 5);
		add(panel, c);

		addKeyListener(ij); // ImageJ handles keyboard shortcuts
		pack();
		GUI.center(this);
		setThreshold(method);
		firstActivation = true;
//		show();

		thread = new Thread(this, "MultiThresholder");
		thread.start();
		if (Recorder.record)
			Recorder.recordOption(method);
	}

	public synchronized void actionPerformed(ActionEvent e) {
		Button b = (Button) e.getSource();
		if (b == null)
			return;
		if (b == resetB)
			doReset = true;
		else if (b == refreshB)
			doRefresh = true;
		else if (b == applyB)
			doApplyLut = true;
		else if (b == setB)
			doSet = true;
		notify();
	}

	void apply(ImagePlus imp) {
		try {
			if (imp.getBitDepth() == 32) {
				GenericDialog gd = new GenericDialog("NaN Backround");
				gd.addCheckbox("Set Background Pixels to NaN", backgroundToNaN);
				gd.showDialog();
				if (gd.wasCanceled()) {
					applyThreshold(imp);
					return;
				}
				backgroundToNaN = gd.getNextBoolean();
				if (backgroundToNaN)
					IJ.run("NaN Background");
				else
					applyThreshold(imp);
			} else
				applyThreshold(imp);
		} catch (Exception e) {/* do nothing */
		}
		close();
	}

	public void applyThreshold(ImagePlus imp) {
		if (Recorder.record && !alreadyRecorded) {
			Recorder.record("run", "MultiThresholder", method + " apply");
			alreadyRecorded = true;
		}
		IJ.run(imp, "Convert to Mask", "");
	}

	void autoSetLevels(ImageProcessor ip, ImageStatistics stats) {
		if (stats == null || stats.histogram == null) {
			minThreshold = defaultMinThreshold;
			maxThreshold = defaultMaxThreshold;
			return;
		}
		int threshold = ip.getAutoThreshold(stats.histogram);
		// IJ.log(threshold+" "+stats.min+" "+stats.max+" "+stats.dmode);
		if ((stats.max - stats.dmode) > (stats.dmode - stats.min)) {
			minThreshold = threshold;
			maxThreshold = stats.max;
		} else {
			minThreshold = stats.min;
			maxThreshold = threshold;
		}
	}

	/** Overrides close() in PlugInFrame. */
	@Override
	public void close() {
		super.close();
		instance = null;
		done = true;
		synchronized (this) {
			notify();
		}
	}

	/** Restore image outside non-rectangular roi. */
	void doMasking(ImagePlus imp, ImageProcessor ip) {
		ImageProcessor mask = imp.getMask();
		if (mask != null)
			ip.reset(mask);
	}

	void doSet(ImagePlus imp, ImageProcessor ip) {
		double level1 = ip.getMinThreshold();
		double level2 = ip.getMaxThreshold();
		if (level1 == ImageProcessor.NO_THRESHOLD) {
			level1 = scaleUp(ip, defaultMinThreshold);
			level2 = scaleUp(ip, defaultMaxThreshold);
		}
		Calibration cal = imp.getCalibration();
		int digits = (ip instanceof FloatProcessor) || cal.calibrated() ? 2 : 0;
		level1 = cal.getCValue(level1);
		level2 = cal.getCValue(level2);
		GenericDialog gd = new GenericDialog("Set Threshold Levels");
		gd.addNumericField("Lower Threshold Level: ", level1, digits);
		gd.addNumericField("Upper Threshold Level: ", level2, digits);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		level1 = gd.getNextNumber();
		level2 = gd.getNextNumber();
		level1 = cal.getRawValue(level1);
		level2 = cal.getRawValue(level2);
		if (level2 < level1)
			level2 = level1;
		double minDisplay = ip.getMin();
		double maxDisplay = ip.getMax();
		ip.resetMinAndMax();
		double minValue = ip.getMin();
		double maxValue = ip.getMax();
		if (level1 < minValue)
			level1 = minValue;
		if (level2 > maxValue)
			level2 = maxValue;
		boolean outOfRange = level1 < minDisplay || level2 > maxDisplay;
		if (outOfRange)
			plot.setHistogram(imp, entireStack(imp));
		else
			ip.setMinAndMax(minDisplay, maxDisplay);

		minThreshold = scaleDown(ip, level1);
		maxThreshold = scaleDown(ip, level2);
		scaleUpAndSet(ip, minThreshold, maxThreshold);
		if (Recorder.record) {
			if (imp.getBitDepth() == 32)
				Recorder.record("setThreshold", ip.getMinThreshold(),
						ip.getMaxThreshold());
			else
				Recorder.record("setThreshold", (int) ip.getMinThreshold(),
						(int) ip.getMaxThreshold());
		}
		threshold = maxThreshold;
		updateLabels(imp, ip);
	}

	void doUpdate() {
		ImagePlus imp;
		ImageProcessor ip;
		int action;
		if (doReset)
			action = RESET;
		else if (doRefresh)
			action = REFRESH;
		else if (doApplyLut)
			action = APPLY;
		else if (doRethreshold)
			action = THRESHOLD;
		else if (doSet)
			action = SET;
		else
			return;
		doReset = false;
		doRefresh = false;
		doApplyLut = false;
		doRethreshold = false;
		doSet = false;
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.beep();
			IJ.showStatus("No image");
			return;
		}
		ip = setup(imp);
		if (ip == null) {
			imp.unlock();
			IJ.beep();
			IJ.showStatus("RGB images cannot be thresholded");
			return;
		}
		switch (action) {
		case REFRESH:
			refresh();
			break;
		case RESET:
			reset(imp, ip);
			break;
		case APPLY:
			apply(imp);
			break;
		case SET:
			doSet(imp, ip);
			break;
		}
		updatePlot(imp);
		ip.setLutAnimation(true);
		imp.updateAndDraw();
	}

	boolean entireStack(ImagePlus imp) {
		return stackHistogram != null && stackHistogram.getState()
				&& imp.getStackSize() > 1;
	}

	public synchronized void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == choice) {
			method = choice.getSelectedItem();
			setThreshold(method);
			doRethreshold = true;
		} else
			doRefresh = true;
		notify();
	}

	void refresh() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return;
		previousImageID = 0;
		ImageProcessor ip = setup(imp);
		bitDepth = imp.getBitDepth();
		method = choice.getSelectedItem();
		setThreshold(method);
		updateLabels(imp, ip);
	}

	void reset(ImagePlus imp, ImageProcessor ip) {
		plot.setHistogram(imp, entireStack(imp));
		ip.resetThreshold();
		if (Recorder.record)
			Recorder.record("resetThreshold");
		updateLabels(imp, ip);
	}

	// Separate thread that does the potentially time-consuming processing
	public void run() {
		while (!done) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			doUpdate();
		}
	}

	/** Scales a threshold level to the range 0-255. */
	double scaleDown(ImageProcessor ip, double threshold) {
		if (ip instanceof ByteProcessor)
			return threshold;
		double min = ip.getMin();
		double max = ip.getMax();
		if (max > min)
			return ((threshold - min) / (max - min)) * 255.0;
		else
			return ImageProcessor.NO_THRESHOLD;
	}

	/** Scales a threshold level in the range 0-255 to the actual level. */
	double scaleUp(ImageProcessor ip, double threshold) {
		double min = ip.getMin();
		double max = ip.getMax();
		// IJ.log("scaleUp: "+ threshold+"  "+min+"  "+max+"  "+(min +
		// (threshold/255.0)*(max-min)));
		if (max > min)
			return min + (threshold / 255.0) * (max - min);
		else
			return ImageProcessor.NO_THRESHOLD;
	}

	/** Scales threshold levels in the range 0-255 to the actual levels. */
	void scaleUpAndSet(ImageProcessor ip, double minThreshold,
			double maxThreshold) {
		if (!(ip instanceof ByteProcessor)
				&& minThreshold != ImageProcessor.NO_THRESHOLD) {
			double min = ip.getMin();
			double max = ip.getMax();
			if (max > min) {
				minThreshold = min + (minThreshold / 255.0) * (max - min);
				maxThreshold = min + (maxThreshold / 255.0) * (max - min);
			} else
				minThreshold = ImageProcessor.NO_THRESHOLD;
		}
		ip.setThreshold(minThreshold, maxThreshold, ImageProcessor.RED_LUT);
	}

	void setThreshold(double threshold, ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		boolean darkb = darkBackground != null && darkBackground.getState();
		double threshold2 = scaleUp(ip, threshold);
		double minThreshold2, maxThreshold2;
		if (darkb) {
			if (invertedLut) {
				minThreshold2 = 0;
				maxThreshold2 = threshold2;
			} else {
				minThreshold2 = threshold2;
				maxThreshold2 = ip.getMax();
			}
		} else {
			if (invertedLut) {
				minThreshold2 = threshold2;
				maxThreshold2 = ip.getMax();
			} else {
				minThreshold2 = 0;
				maxThreshold2 = threshold2;
			}
		}
		ip.setThreshold(minThreshold2, maxThreshold2, ImageProcessor.RED_LUT);
		imp.updateAndDraw();
		updateLabels(imp, ip);
		if (Recorder.record)
			Recorder.record("run", "MultiThresholder", method);
	}

	void setThreshold(String method) {
		ImagePlus imp;
		ImageProcessor ip;
		imp = WindowManager.getCurrentImage();
		if (imp == null || bitDepth != 8 && bitDepth != 16 && bitDepth != 32) {
			IJ.beep();
			IJ.showStatus("No 8, 16 or 32-bit image");
			return;
		}
		ip = setup(imp);
		if (ip == null) {
			imp.unlock();
			IJ.beep();
			IJ.showStatus("RGB images cannot be thresholded");
			return;
		}
		AutoThresholder_IMMI at = new AutoThresholder_IMMI();
		if (plot.histogram != null) {
			threshold = at.getThreshold(method, plot.histogram);
			updatePlot(imp);
			setThreshold(threshold, imp);
		}
	}

	ImageProcessor setup(ImagePlus imp) {
		ImageProcessor ip;
		int type = imp.getType();
		if (type == ImagePlus.COLOR_RGB)
			return null;
		ip = imp.getProcessor();
		boolean not8Bits = type == ImagePlus.GRAY16 || type == ImagePlus.GRAY32;
		int id = imp.getID();
		if (id != previousImageID || type != previousImageType) {
			// IJ.log(minMaxChange +"  "+
			// (id!=previousImageID)+"  "+(type!=previousImageType));
			if (not8Bits) {
				ip.resetMinAndMax();
				imp.updateAndDraw();
			}
			invertedLut = imp.isInvertedLut();
			minThreshold = ip.getMinThreshold();
			maxThreshold = ip.getMaxThreshold();
			stats = plot.setHistogram(imp, entireStack(imp));
			if (minThreshold == ImageProcessor.NO_THRESHOLD)
				autoSetLevels(ip, stats);
			else {
				minThreshold = scaleDown(ip, minThreshold);
				maxThreshold = scaleDown(ip, maxThreshold);
			}
			scaleUpAndSet(ip, minThreshold, maxThreshold);
			updatePlot(imp);
			imp.updateAndDraw();
		}
		previousImageID = id;
		previousImageType = type;
		return ip;
	}

	void updateLabels(ImagePlus imp, ImageProcessor ip) {
		if (threshLabel != null) {
			double threshold2 = scaleUp(ip, threshold);
			if (threshold == ImageProcessor.NO_THRESHOLD) {
				threshLabel.setText("Threshold: None");
			} else {
				Calibration cal = imp.getCalibration();
				if (cal.calibrated()) {
					threshold2 = cal.getCValue((int) threshold2);
				}
				if (((int) threshold2 == threshold2 || (ip instanceof ShortProcessor))) {
					threshLabel.setText("Threshold: " + (int) threshold2);
				} else {
					threshLabel.setText("Threshold: " + IJ.d2s(threshold2, 2));
				}
			}
		}
	}

	void updatePlot(ImagePlus imp) {
		plot.minThreshold = minThreshold;
		plot.maxThreshold = maxThreshold;
		plot.repaint();
	}

	@Override
	public void windowActivated(WindowEvent e) {
		super.windowActivated(e);
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (!firstActivation) {
				previousImageID = 0;
				setup(imp);
			}
			firstActivation = false;
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		close();
	}

}

class MultiThresholderPlot extends Canvas implements Measurements,
		MouseListener {

	private static final long serialVersionUID = 1L;
	static final int WIDTH = 256, HEIGHT = 48;
	double minThreshold = 85;
	double maxThreshold = 170;
	int[] histogram;
	Color[] hColors;
	int hmax;
	Image os;
	Graphics osg;

	public MultiThresholderPlot() {
		addMouseListener(this);
		setSize(WIDTH + 1, HEIGHT + 1);
	}

	/**
	 * Overrides Component getPreferredSize(). Added to work around a bug in
	 * Java 1.4.1 on Mac OS X.
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH + 1, HEIGHT + 1);
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void paint(Graphics g) {
		if (histogram != null) {
			if (os == null && hmax > 0) {
				os = createImage(WIDTH, HEIGHT);
				osg = os.getGraphics();
				osg.setColor(Color.white);
				osg.fillRect(0, 0, WIDTH, HEIGHT);
				osg.setColor(Color.gray);
				for (int i = 0; i < WIDTH; i++) {
					if (hColors != null)
						osg.setColor(hColors[i]);
					osg.drawLine(i, HEIGHT, i, HEIGHT
							- ((HEIGHT * histogram[i]) / hmax));
				}
				osg.dispose();
			}
			g.drawImage(os, 0, 0, this);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		g.setColor(Color.black);
		g.drawRect(0, 0, WIDTH, HEIGHT);
		g.setColor(Color.red);
		g.drawRect((int) minThreshold, 1, (int) (maxThreshold - minThreshold),
				HEIGHT);
		g.drawLine((int) minThreshold, 0, (int) maxThreshold, 0);
	}

	ImageStatistics setHistogram(ImagePlus imp, boolean stack) {
		imp.killRoi();
		ImageProcessor ip = imp.getProcessor();
		ImageStatistics stats = null;
		if (stack)
			stats = new StackStatistics(imp);
		else {
			if (!(ip instanceof ByteProcessor)) {
				ip.resetMinAndMax();
				imp.updateAndDraw();
			}
			stats = ImageStatistics.getStatistics(ip, AREA + MIN_MAX + MODE,
					null);
		}
		int maxCount2 = 0;
		histogram = new int[stats.nBins];
		for (int i = 0; i < stats.nBins; i++)
			histogram[i] = stats.histogram[i];
		for (int i = 0; i < stats.nBins; i++)
			if ((histogram[i] > maxCount2) && (i != stats.mode))
				maxCount2 = histogram[i];
		hmax = stats.maxCount;
		if ((hmax > (maxCount2 * 2)) && (maxCount2 != 0)) {
			hmax = (int) (maxCount2 * 1.5);
			histogram[stats.mode] = hmax;
		}
		os = null;

		ColorModel cm = ip.getColorModel();
		if (!(cm instanceof IndexColorModel))
			return null;
		IndexColorModel icm = (IndexColorModel) cm;
		int mapSize = icm.getMapSize();
		if (mapSize != 256)
			return null;
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		icm.getReds(r);
		icm.getGreens(g);
		icm.getBlues(b);
		hColors = new Color[256];
		for (int i = 0; i < 256; i++)
			hColors[i] = new Color(r[i] & 255, g[i] & 255, b[i] & 255);
		return stats;
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

}
