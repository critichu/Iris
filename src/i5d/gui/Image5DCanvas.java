//
// Image5DCanvas.java
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

package i5d.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/*
 * Created on 29.05.2005

/** Canvas compatible with Image5DLayout.
 * @author Joachim Walter
 */
public class Image5DCanvas extends ImageCanvas {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8563611296852137396L;

	/**
	 * @param imp
	 */
	public Image5DCanvas(final ImagePlus imp) {
		super(imp);
	}

	@Override
	public ImagePlus getImage() {
		return imp;
	}

	/**
	 * Adjust the canvas size symmetrically about the middle of the srcRect, if
	 * the user resizes the window. Called from Image5DLayout.layoutContainer().
	 */
	protected Dimension resizeCanvasI5D(int width, int height) {
		final double magnification = getMagnification();

		if (width > imageWidth * magnification) width =
			(int) (imageWidth * magnification);
		if (height > imageHeight * magnification) height =
			(int) (imageHeight * magnification);
		setDrawingSize(width, height);
		final Dimension dim = new Dimension(width, height);

		int newSrcRectWidth = (int) (width / magnification);
		// Prevent display of zoomIndicator due to rounding error.
		if (Math.round(magnification) != magnification &&
			(int) ((width + 1) / magnification) >= imageWidth)
		{
			newSrcRectWidth = imageWidth;
		}
		int newSrcRectHeight = (int) (height / magnification);
		// Prevent display of zoomIndicator due to rounding error.
		if (Math.round(magnification) != magnification &&
			(int) ((height + 1) / magnification) >= imageHeight)
		{
			newSrcRectHeight = imageHeight;
		}

		srcRect.x = srcRect.x + (srcRect.width - newSrcRectWidth) / 2;
		if (srcRect.x < 0) srcRect.x = 0;
		srcRect.y = srcRect.y + (srcRect.height - newSrcRectHeight) / 2;
		if (srcRect.y < 0) srcRect.y = 0;
		srcRect.width = newSrcRectWidth;
		srcRect.height = newSrcRectHeight;
		if ((srcRect.x + srcRect.width) > imageWidth) srcRect.x =
			imageWidth - srcRect.width;
		if ((srcRect.y + srcRect.height) > imageHeight) srcRect.y =
			imageHeight - srcRect.height;
		repaint();

		adaptChannelCanvasses();

		return dim;
	}

	/* Unfortunately, the setSrcRect method of ImageCanvas has default access rights. 
	 * TODO: ask Wayne to change access rights. */
	public void setSrcRectI5d(final Rectangle rect) {
		srcRect = rect;
	}

	public void setCursorLoc(final int xMouse, final int yMouse) {
		this.xMouse = xMouse;
		this.yMouse = yMouse;
	}

	public void setModifiers(final int flags) {
		this.flags = flags;
	}

	public Dimension getDrawingSize() {
		return new Dimension(dstWidth, dstHeight);
	}

	/* copied and modified from ImageCanvas */
	@Override
	public void zoomOut(int x, int y) {
		if (magnification <= 0.03125) return;
		final double newMag = getLowerZoomLevel(magnification);
//        if (newMag==imp.getWindow().getInitialMagnification()) {
//          unzoom();
//          return;
//        }
		if (imageWidth * newMag > dstWidth) {
			int w = (int) Math.round(dstWidth / newMag);
			if (w * newMag < dstWidth) w++;
			int h = (int) Math.round(dstHeight / newMag);
			if (h * newMag < dstHeight) h++;
			x = offScreenX(x);
			y = offScreenY(y);
			final Rectangle r = new Rectangle(x - w / 2, y - h / 2, w, h);
			if (r.x < 0) r.x = 0;
			if (r.y < 0) r.y = 0;
			if (r.x + w > imageWidth) r.x = imageWidth - w;
			if (r.y + h > imageHeight) r.y = imageHeight - h;
			srcRect = r;
			setMagnification(newMag);
			adaptChannelCanvasses();
		}
		else {
			srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
			setDrawingSize((int) (imageWidth * newMag), (int) (imageHeight * newMag));
			setMagnification(newMag);
			adaptChannelCanvasses();
			// Pack after adapting all canvasses for case that zoom comes from a
			// channel image.
			imp.getWindow().pack();
		}
		// IJ.write(newMag + " " +
		// srcRect.x+" "+srcRect.y+" "+srcRect.width+" "+srcRect.height+" "+dstWidth
		// + " " + dstHeight);
		// IJ.write(srcRect.x + " " + srcRect.width + " " + dstWidth);
		repaint();

	}

//    public void zoomIn(int x, int y) {
//        super.zoomIn(x, y);
//        adaptChannelCanvasses();
//    }
	/**
	 * Copied over from ImageCanvas just to move the "pack" call behind adapting
	 * channel canvasses.
	 */
	@Override
	public void zoomIn(final int x, final int y) {
		if (magnification >= 32) return;
		final double newMag = getHigherZoomLevel(magnification);
		final int newWidth = (int) (imageWidth * newMag);
		final int newHeight = (int) (imageHeight * newMag);
		final Dimension newSize = canEnlarge(newWidth, newHeight);
		if (newSize != null) {
			setDrawingSize(newSize.width, newSize.height);
			if (newSize.width != newWidth || newSize.height != newHeight) adjustSourceRectI5d(
				newMag, x, y);
			else setMagnification(newMag);
			adaptChannelCanvasses();
			imp.getWindow().pack();
		}
		else {
			adjustSourceRectI5d(newMag, x, y);
			adaptChannelCanvasses();
		}
		repaint();
	}

	/* This method has default access rights in ImageCanvas. 
	 * TODO: ask Wayne to make it protected.
	 */
	private void adjustSourceRectI5d(final double newMag, int x, int y) {
		// IJ.log("adjustSourceRect1: "+newMag+" "+dstWidth+"  "+dstHeight);
		int w = (int) Math.round(dstWidth / newMag);
		if (w * newMag < dstWidth) w++;
		int h = (int) Math.round(dstHeight / newMag);
		if (h * newMag < dstHeight) h++;
		x = offScreenX(x);
		y = offScreenY(y);
		final Rectangle r = new Rectangle(x - w / 2, y - h / 2, w, h);
		if (r.x < 0) r.x = 0;
		if (r.y < 0) r.y = 0;
		if (r.x + w > imageWidth) r.x = imageWidth - w;
		if (r.y + h > imageHeight) r.y = imageHeight - h;
		srcRect = r;
		setMagnification(newMag);
		// IJ.log("adjustSourceRect2: "+srcRect+" "+dstWidth+"  "+dstHeight);
	}

	@Override
	protected void scroll(final int sx, final int sy) {
		super.scroll(sx, sy);
		adaptChannelCanvasses();
		repaint();
	}

//    protected void setupScroll(int ox, int oy) {
//        super.setupScroll(ox, oy);
//        adaptChannelCanvasses();
//    }

	/** Predicts, whether Canvas can enlarge on this desktop, even in tiled mode */
	// TODO: ChannelCanvas or z/t sliders are not handled fully correctly.
	@Override
	protected Dimension canEnlarge(final int newWidth, final int newHeight) {
		if ((flags & Event.SHIFT_MASK) != 0 || IJ.shiftKeyDown()) return null;
		final ImageWindow win = imp.getWindow();
		if (win == null) return null;
		final Rectangle r1 = win.getBounds();

		final Dimension prefSize =
			((Image5DLayout) win.getLayout()).preferredLayoutSize(win, newWidth,
				newHeight);
		r1.width = prefSize.width;
		r1.height = prefSize.height;

		final Rectangle max = ((Image5DWindow) win).getMaxWindowI5d();
		final boolean fitsHorizontally = r1.x + r1.width < max.x + max.width;
		final boolean fitsVertically = r1.y + r1.height < max.y + max.height;
		if (fitsHorizontally && fitsVertically) return new Dimension(newWidth,
			newHeight);
		else if (fitsVertically && newHeight < dstWidth) return new Dimension(
			dstWidth, newHeight);
		else if (fitsHorizontally && newWidth < dstHeight) return new Dimension(
			newWidth, dstHeight);
		else return null;
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		final boolean selectionBrush =
			(Toolbar.getToolId() == Toolbar.OVAL && Toolbar.getBrushSize() > 0);
		// Keep reference to Roi in main canvas
		Roi oldMainRoi = null;
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			oldMainRoi = win.getImagePlus().getRoi();
		}

		super.mouseDragged(e);

		adaptChannelMouse();

		// Get new reference to Roi in main canvas
		Roi newMainRoi = null;
		if (win != null) {
			newMainRoi = win.getImagePlus().getRoi();
		}
		// Work around special behaviour of some tools, that operate on the main
		// canvas and
		// not on the channel canvas, that receives the mousePressed event.
		if (oldMainRoi != newMainRoi || selectionBrush) {
			adaptChannelRois(false);
		}
		else {
			adaptChannelRois(true);
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		super.mouseMoved(e);

		adaptChannelMouse();

		// To avoid ROI flickering, only adapt ROI when it really has changed.
		// Code copied from ImageCanvas.mouseMoved().
		final Roi roi = imp.getRoi();
		if (roi != null &&
			(roi.getType() == Roi.POLYGON || roi.getType() == Roi.POLYLINE || roi
				.getType() == Roi.ANGLE) && roi.getState() == Roi.CONSTRUCTING)
		{
			adaptChannelRois(true);
		}
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		final boolean selectionBrush =
			(Toolbar.getToolId() == Toolbar.OVAL && Toolbar.getBrushSize() > 0);

		setThisChannelAsCurrent();

		// Keep reference to Roi in main canvas
		Roi oldMainRoi = null;
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			oldMainRoi = win.getImagePlus().getRoi();
		}

		super.mousePressed(e);

		adaptChannelMouse();

		// Get new reference to Roi in main canvas
		Roi newMainRoi = null;
		if (win != null) {
			newMainRoi = win.getImagePlus().getRoi();
		}
		// Work around special behaviour of some tools, that operate on the main
		// canvas and
		// not on the channel canvas, that receives the mousePressed event.
		if (oldMainRoi != newMainRoi || selectionBrush) {
			adaptChannelRois(false);
		}
		else {
			adaptChannelRois(true);
		}

	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		super.mouseReleased(e);
		adaptChannelMouse();
		adaptChannelRois(true);
	}

	private void adaptChannelCanvasses() {
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			win.adaptCanvasses(this);
		}
	}

	/* If <code>thisChannel</code> is true, hands on the current ROIto all other channels. 
	 * If <code>thisChannel</code> is false, hands on the ROI of main canvas to all channels
	 * including this one. */
	private void adaptChannelRois(final boolean thisChannel) {
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			if (thisChannel) {
				win.adaptRois(this);
			}
			else {
				win.adaptRois((Image5DCanvas) win.getCanvas());
			}
		}
	}

	/* Hands on the current cursor location and modifiers to all other channels. */
	private void adaptChannelMouse() {
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			win.adaptMouse(this);
		}
	}

	private void setThisChannelAsCurrent() {
		final Image5DWindow win = (Image5DWindow) imp.getWindow();
		if (win != null) {
			win.setChannelAsCurrent(this);
		}
	}

}
