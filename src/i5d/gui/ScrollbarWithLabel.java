//
// ScrollbarWithLabel.java
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

import java.awt.AWTEventMulticaster;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyListener;

/*
 * Created on 10.04.2005
 */

/**
 * Quick hack to add labels to the dimension sliders of Image5DWindow
 * 
 * @author Joachim Walter
 */
public class ScrollbarWithLabel extends Panel implements Adjustable,
	AdjustmentListener
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7934396430763922931L;

	private final Scrollbar bar;
	private Label label;

	private final int orientation;

	transient AdjustmentListener adjustmentListener;

	public ScrollbarWithLabel(final int orientation, final int value,
		final int visible, final int minimum, final int maximum, final String label)
	{
		super(new BorderLayout(2, 0));
		this.orientation = orientation;
		bar = new Scrollbar(orientation, value, visible, minimum, maximum);
		if (label != null) {
			this.label = new Label(label);
		}
		else {
			this.label = new Label("");
		}
		if (orientation == Scrollbar.HORIZONTAL) add(this.label, BorderLayout.WEST);
		else if (orientation == Scrollbar.VERTICAL) add(this.label,
			BorderLayout.NORTH);
		else throw new IllegalArgumentException("invalid orientation");

		add(bar, BorderLayout.CENTER);
		bar.addAdjustmentListener(this);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension dim = new Dimension(0, 0);

		if (orientation == Scrollbar.HORIZONTAL) {
			int width = bar.getPreferredSize().width + label.getPreferredSize().width;
			final Dimension minSize = getMinimumSize();
			if (width < minSize.width) width = minSize.width;
			final int height = bar.getPreferredSize().height;
			dim = new Dimension(width, height);
		}
		else {
			int height =
				bar.getPreferredSize().height + label.getPreferredSize().height;
			final Dimension minSize = getMinimumSize();
			if (height < minSize.height) height = minSize.height;
//			int width = Math.max(bar.getPreferredSize().width, label.getPreferredSize().width);
			final int width = bar.getPreferredSize().width;
			dim = new Dimension(width, height);
		}
		return dim;
	}

	@Override
	public Dimension getMinimumSize() {
		if (orientation == Scrollbar.HORIZONTAL) {
			return new Dimension(80, 15);
		}
		return new Dimension(15, 80);
	}

	/**
	 * @deprecated Returns a reference to the Scrollbar. Ideally the scrollbar
	 *             should be fully handled by this class and hidden to other
	 *             classes, but as it was once exposed this method is kept for
	 *             backward compatibility. SyncWindows up to version 1.6 needs
	 *             this method. The Image5DWindow also needs it to mimick the
	 *             SliceSelector of StackWindow.
	 */
	@Deprecated
	public Scrollbar getScrollbar() {
		return bar;
	}

	/* Adds KeyListener also to all sub-components.
	 */
	@Override
	public synchronized void addKeyListener(final KeyListener l) {
		super.addKeyListener(l);
		bar.addKeyListener(l);
		label.addKeyListener(l);
	}

	/* Removes KeyListener also from all sub-components.
	 */
	@Override
	public synchronized void removeKeyListener(final KeyListener l) {
		super.removeKeyListener(l);
		bar.removeKeyListener(l);
		label.removeKeyListener(l);
	}

	/* 
	 * Methods of the Adjustable interface
	 */
	@Override
	public synchronized void addAdjustmentListener(final AdjustmentListener l) {
		if (l == null) {
			return;
		}
		adjustmentListener = AWTEventMulticaster.add(adjustmentListener, l);
	}

	@Override
	public int getBlockIncrement() {
		return bar.getBlockIncrement();
	}

	@Override
	public int getMaximum() {
		return bar.getMaximum();
	}

	@Override
	public int getMinimum() {
		return bar.getMinimum();
	}

	@Override
	public int getOrientation() {
		return bar.getOrientation();
	}

	@Override
	public int getUnitIncrement() {
		return bar.getUnitIncrement();
	}

	@Override
	public int getValue() {
		return bar.getValue();
	}

	@Override
	public int getVisibleAmount() {
		return bar.getVisibleAmount();
	}

	@Override
	public synchronized void removeAdjustmentListener(final AdjustmentListener l)
	{
		if (l == null) {
			return;
		}
		adjustmentListener = AWTEventMulticaster.remove(adjustmentListener, l);
	}

	@Override
	public void setBlockIncrement(final int b) {
		bar.setBlockIncrement(b);
	}

	@Override
	public void setMaximum(final int max) {
		bar.setMaximum(max);
	}

	@Override
	public void setMinimum(final int min) {
		bar.setMinimum(min);
	}

	@Override
	public void setUnitIncrement(final int u) {
		bar.setUnitIncrement(u);
	}

	@Override
	public void setValue(final int v) {
		bar.setValue(v);
	}

	@Override
	public void setVisibleAmount(final int v) {
		bar.setVisibleAmount(v);
	}

	@Override
	public void setFocusable(final boolean focusable) {
		super.setFocusable(focusable);
		bar.setFocusable(focusable);
		label.setFocusable(focusable);
	}

	/*
	 * Method of the AdjustmenListener interface.
	 */
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent e) {
		if (bar != null && e.getSource() == bar) {
			final AdjustmentEvent myE =
				new AdjustmentEvent(this, e.getID(), e.getAdjustmentType(), e
					.getValue(), e.getValueIsAdjusting());
			final AdjustmentListener listener = adjustmentListener;
			if (listener != null) {
				listener.adjustmentValueChanged(myE);
			}
		}
	}

}
