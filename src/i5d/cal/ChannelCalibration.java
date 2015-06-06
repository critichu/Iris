//
// ChannelCalibration.java
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

import ij.measure.Calibration;

/*
 * Created on 15.10.2005
 */

/**
 * "Struct" for storing the calibration function and parameters of a color
 * channel
 * 
 * @author Joachim Walter
 */
public class ChannelCalibration {

	private String label;

	// Fields for calibration function. Documented in ij.measure.Calibration.
	private String valueUnit = "Gray Value";
	private int function = Calibration.NONE;
	private double[] coefficients;
	private boolean zeroClip;

	public ChannelCalibration() {
		label = "";
		disableDensityCalibration();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

//  Functions to handle density calibration follow.
	public void setFunction(final int function, final double[] coefficients,
		final String unit, final boolean zeroClip)
	{
		if (function == Calibration.NONE) {
			disableDensityCalibration();
			return;
		}
		if (coefficients == null && function >= Calibration.STRAIGHT_LINE &&
			function <= Calibration.LOG2) return;
		this.function = function;
		this.coefficients = coefficients;
		this.zeroClip = zeroClip;
		if (unit != null) valueUnit = unit;
	}

	public void disableDensityCalibration() {
		function = Calibration.NONE;
		coefficients = null;
		valueUnit = "Gray Value";
		zeroClip = false;
	}

	/** Returns the calibration function ID. */
	public int getFunction() {
		return function;
	}

	/** Returns the calibration function coefficients. */
	public double[] getCoefficients() {
		return coefficients;
	}

	/** Returns the value unit. */
	public String getValueUnit() {
		return valueUnit;
	}

	/** Returns true if zero clipping is enabled. */
	public boolean isZeroClip() {
		return zeroClip;
	}

	/**
	 * Returns a copy of the ChannelCalibration object. The copy is a deep copy.
	 */
	public ChannelCalibration copy() {
		final ChannelCalibration cc = new ChannelCalibration();
		cc.setLabel(getLabel());
		cc.setFunction(getFunction(), getCoefficients(), getValueUnit(),
			isZeroClip());
		return cc;
	}
}
