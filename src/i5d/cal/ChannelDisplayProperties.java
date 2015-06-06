//
// ChannelDisplayProperties.java
//

/*
Image5D plugins for 5-dimensional image stacks in ImageJ.

Copyright (c) 2010, Joachim Walter and ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package i5d.cal;

import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

/*
 * Created on 08.04.2005
 */

/**
 * "Struct" for storing the display properties of a color channel (e.g. min, max
 * )
 * 
 * @author Joachim Walter
 */
public class ChannelDisplayProperties {

	private ColorModel colorModel;
	private double minValue;
	private double maxValue;
	private double minThreshold;
	private double maxThreshold;
	private int lutUpdateMode;
	private boolean displayedGray;
	private boolean displayedInOverlay;

//	private String label;
//    
//    // Fields for calibration function. Documented in ij.measure.Calibration.
//    private String valueUnit = "Gray Value";
//    private int function = Calibration.NONE;
//    private double[] coefficients;
//    private boolean zeroClip;

	public ChannelDisplayProperties() {

		final byte[] lut = new byte[256];
		for (int i = 0; i < 256; i++) {
			lut[i] = (byte) i;
		}
		colorModel = new IndexColorModel(8, 256, lut, lut, lut);
		minValue = 0d;
		maxValue = 255d;
		minThreshold = ImageProcessor.NO_THRESHOLD;
		maxThreshold = ImageProcessor.NO_THRESHOLD;
		displayedGray = false;
		displayedInOverlay = true;
		lutUpdateMode = ImageProcessor.RED_LUT;
//		label = "";
//        
//        disableDensityCalibration();
	}

	public ColorModel getColorModel() {
		return colorModel;
	}

	public void setColorModel(final ColorModel colorModel) {
		this.colorModel = colorModel;
	}

	public double getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(final double maxThreshold) {
		this.maxThreshold = maxThreshold;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(final double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(final double minThreshold) {
		this.minThreshold = minThreshold;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(final double minValue) {
		this.minValue = minValue;
	}

//	public String getLabel() {
//		return label;
//	}
//	public void setLabel(String label) {
//		this.label = label;
//	}
	public boolean isDisplayedGray() {
		return displayedGray;
	}

	public void setDisplayedGray(final boolean displayGray) {
		this.displayedGray = displayGray;
	}

	public boolean isDisplayedInOverlay() {
		return displayedInOverlay;
	}

	public void setDisplayedInOverlay(final boolean displayedInOverlay) {
		this.displayedInOverlay = displayedInOverlay;
	}

	public int getLutUpdateMode() {
		return lutUpdateMode;
	}

	public void setLutUpdateMode(final int lutUpdateMode) {
		this.lutUpdateMode = lutUpdateMode;
	}

	public static IndexColorModel createModelFromColor(final Color color) {
		final byte[] rLut = new byte[256];
		final byte[] gLut = new byte[256];
		final byte[] bLut = new byte[256];

		final int red = color.getRed();
		final int green = color.getGreen();
		final int blue = color.getBlue();

		final double rIncr = (red) / 255d;
		final double gIncr = (green) / 255d;
		final double bIncr = (blue) / 255d;

		for (int i = 0; i < 256; ++i) {
			rLut[i] = (byte) (i * rIncr);
			gLut[i] = (byte) (i * gIncr);
			bLut[i] = (byte) (i * bIncr);
		}

		return new IndexColorModel(8, 256, rLut, gLut, bLut);
	}

//// Functions to handle density calibration follow.
//    public void setFunction(int function, double[] coefficients, String unit, boolean zeroClip) {
//        if (function==Calibration.NONE)
//            {disableDensityCalibration(); return;}
//        if (coefficients==null && function>=Calibration.STRAIGHT_LINE && function<=Calibration.LOG2)
//            return;
//        this.function = function;
//        this.coefficients = coefficients;
//        this.zeroClip = zeroClip;
//        if (unit!=null)
//            valueUnit = unit;
//    }
//    
//    public void disableDensityCalibration() {
//        function = Calibration.NONE;
//        coefficients = null;
//        valueUnit = "Gray Value";
//        zeroClip = false;
//    }   
//    
//    /** Returns the calibration function ID. */
//    public int getFunction() {
//        return function;
//    }
//    /** Returns the calibration function coefficients. */
//    public double[] getCoefficients() {
//        return coefficients;
//    }
//    /** Returns the value unit. */
//    public String getValueUnit() {
//        return valueUnit;
//    }
//    /** Returns true if zero clipping is enabled. */
//    public boolean isZeroClip() {
//        return zeroClip;
//    }   

	/**
	 * Returns a copy of the colorChannelProperties object. The copy is a deep
	 * copy except for the colorModel, where only the reference is copied.
	 */
	public ChannelDisplayProperties copy() {
		final ChannelDisplayProperties ccp = new ChannelDisplayProperties();

		ccp.setColorModel(getColorModel());
		ccp.setMinValue(getMinValue());
		ccp.setMaxValue(getMaxValue());
		ccp.setMinThreshold(getMinThreshold());
		ccp.setMaxThreshold(getMaxThreshold());
		ccp.setLutUpdateMode(getLutUpdateMode());
		ccp.setDisplayedGray(isDisplayedGray());
		ccp.setDisplayedInOverlay(isDisplayedInOverlay());

		return ccp;
	}

}
