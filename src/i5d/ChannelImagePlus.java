//
// ChannelImagePlus.java
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

package i5d;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

public class ChannelImagePlus extends ImagePlus {

	public ChannelImagePlus(final String title, final ImageProcessor ip) {
		super(title, ip);
	}

	/* Empty method. Prevents messing up the Image5DWindow's title. 
	 * A ChannelImagePlus needs no title. */
	@Override
	public void setTitle(final String title) {
		return;
	}

	public String getValueAsStringI5d(final int x, final int y) {
		if (win != null && win instanceof PlotWindow) return "";
		final Calibration cal = getCalibration();
		final int v = getProcessor().getPixel(x, y);
		final int type = getType();
		switch (type) {
			case GRAY8:
			case GRAY16:
				final double cValue = cal.getCValue(v);
				if (cValue == v) return ("" + v);
				return ("" + IJ.d2s(cValue) + " (" + v + ")");
			case GRAY32:
				return ("" + Float.intBitsToFloat(v));
			default:
				return ("");
		}
	}

	/**
	 * Assigns the specified ROI to this image and displays it without saving it
	 * as previousRoi as it is done by <code>setRoi()</code>.
	 */
	public void putRoi(Roi newRoi) {

		Rectangle bounds = new Rectangle();

		if (newRoi != null) {
			// Roi with width and height = 0 is same as null Roi.
			bounds = newRoi.getBounds();
			if (bounds.width == 0 && bounds.height == 0 &&
				newRoi.getType() != Roi.POINT)
			{
				newRoi = null;
			}
		}

		roi = newRoi;

		if (roi != null) {
			if (ip != null) {
				ip.setMask(null);
				if (roi.isArea()) ip.setRoi(bounds);
				else ip.resetRoi();
			}
			roi.setImage(this);
		}

		draw();
	}

}
