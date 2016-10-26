//
// I5DVirtualStack.java
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

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.image.ColorModel;

/**
 * This class represents an array of disk-resident images.
 */
public class I5DVirtualStack extends ImageStack {

	private static final int INITIAL_SIZE = 100;
	private String path;
	int nSlices;
	private String[] names;

	/** Creates a new, empty virtual stack. */
	public I5DVirtualStack(final int width, final int height,
		final ColorModel cm, final String path)
	{
		super(width, height, cm);
		this.path = path;
		names = new String[INITIAL_SIZE];
		// IJ.log("VirtualStack: "+path);
	}

	/** Adds an image to the end of the stack. */
	public void addSlice(final String name) {
		if (name == null) throw new IllegalArgumentException("'name' is null!");
		nSlices++;
		// IJ.log("addSlice: "+nSlices+"  "+name);
		if (nSlices == names.length) {
			final String[] tmp = new String[nSlices * 2];
			System.arraycopy(names, 0, tmp, 0, nSlices);
			names = tmp;
		}
		names[nSlices - 1] = name;
	}

	/** Does nothing. */
	@Override
	public void addSlice(final String sliceLabel, final Object pixels) {}

	/** Does nothing.. */
	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip) {}

	/** Does noting. */
	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip,
		final int n)
	{}

	/** Deletes the specified slice, were 1<=n<=nslices. */
	@Override
	public void deleteSlice(final int n) {
		if (n < 1 || n > nSlices) throw new IllegalArgumentException(
			"Argument out of range: " + n);
		if (nSlices < 1) return;
		for (int i = n; i < nSlices; i++)
			names[i - 1] = names[i];
		names[nSlices - 1] = null;
		nSlices--;
	}

	/** Deletes the last slice in the stack. */
	@Override
	public void deleteLastSlice() {
		if (nSlices > 0) deleteSlice(nSlices);
	}

	/** Returns the pixel array for the specified slice, were 1<=n<=nslices. */
	@Override
	public Object getPixels(final int n) {
		final ImageProcessor ip = getProcessor(n);
		if (ip != null) return ip.getPixels();
		return null;
	}

	/**
	 * Assigns a pixel array to the specified slice, were 1<=n<=nslices.
	 */
	@Override
	public void setPixels(final Object pixels, final int n) {}

	/**
	 * Returns an ImageProcessor for the specified slice, were 1<=n<=nslices.
	 * Returns null if the stack is empty.
	 */
	@Override
	public ImageProcessor getProcessor(final int n) {
		// IJ.log("getProcessor: "+n+"  "+names[n-1]);
		final ImagePlus imp = new Opener().openImage(path, names[n - 1]);
		if (imp != null && this.getColorModel() != null) {
			imp.getProcessor().setColorModel(this.getColorModel());
		}
		else return null;
		return imp.getProcessor();
	}

	/** Returns the directory of the stack. */
	public String getPath() {
		return path;
	}

	/** Returns the number of slices in this stack. */
	@Override
	public int getSize() {
		return nSlices;
	}

	/** Returns the file name of the Nth image. */
	@Override
	public String getSliceLabel(final int n) {
		return names[n - 1];
	}

	/** Returns null. */
	@Override
	public Object[] getImageArray() {
		return null;
	}

	/** Does nothing. */
	@Override
	public void setSliceLabel(final String label, final int n) {}

	/** Always return true. */
	@Override
	public boolean isVirtual() {
		return true;
	}

	/** Does nothing. */
	@Override
	public void trim() {}

}
