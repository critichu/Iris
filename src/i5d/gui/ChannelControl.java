//
// ChannelControl.java
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

import i5d.Image5D;
import i5d.cal.ChannelDisplayProperties;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.gui.ColorChooser;
import ij.gui.GUI;
import ij.plugin.LUT_Editor;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;

/*
 * Created on 15.05.2005
 */

/**
 * ChannelControl contains one (sub-)Panel ("selectorPanel"), which contains a
 * Button, which opens the color selection menu (grayed out in ONE_CHANNEL_GRAY
 * mode) and a Choice "displayCoice", that controls the display mode (1 channel,
 * overlay). The button and the Coice are in a sub-Panel, which is in
 * BorderLayout.NORTH. Additionally the selectorPanel contains either a
 * scrollbar (1 ch) or a checkbox group (overlay) in BorderLayout.CENTER. The
 * selectorPanel is in BorderLayout.CENTER of the main panel. The color
 * selection menu is in BorderLayout.EAST of the main panel.
 */
public class ChannelControl extends Panel implements ItemListener,
	AdjustmentListener, ActionListener
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3537465383241321652L;

	private Image5DWindow win;
	private Image5D i5d;
	private ImageJ ij;

	private Choice displayChoice;
	private int displayMode;
	private boolean displayGrayInTiles;

	private Panel selectorPanel;
	private ScrollbarWithLabel scrollbarWL; // Selects channel in gray and color mode.
	private ChannelSelectorOverlay channelSelectorOverlay; // Selects channels and in
																									// overlay and tiled mode.

	private Panel tiledSelectorPanel; // contains allGrayBox and ChannelSelectorOverlay in
														// tiled mode
	private Checkbox allGrayInTilesBox; // Used to select if all single channels in tiled
															// mode are displayed in gray.

	private Button colorButton;
	private boolean colorChooserDisplayed;
	private ChannelColorChooser cColorChooser;

	private int nChannels;
	private int currentChannel;

	public static final int ONE_CHANNEL_GRAY = 0;
	public static final int ONE_CHANNEL_COLOR = 1;
	public static final int OVERLAY = 2;
	public static final int TILED = 3;
	public static final String[] displayModes = { "gray", "color", "overlay",
		"tiled" };

	/** Display mode to use by default. */
	public static final int DEFAULT_DISPLAY_MODE = ONE_CHANNEL_COLOR;

	/** ImageJ preferences key to use for storing last used display mode. */
	public static final String DISPLAY_MODE_PREF = "image5d.displayMode";

	private static final String BUTTON_ACTIVATE_COLOR_CHOOSER = "Color";
	private static final String BUTTON_DEACTIVATE_COLOR_CHOOSER = "Color";

	public ChannelControl(final Image5DWindow win) {
		super(new BorderLayout(5, 5));
		this.win = win;
		this.i5d = (Image5D) win.getImagePlus();
		this.ij = IJ.getInstance();

		currentChannel = i5d.getCurrentChannel();
		nChannels = i5d.getNChannels();

		/* --------------------------------------------------------------------------------
		 * Handling of Key events:
		 * Add the Image5DWindow as KeyListener, then ImageJ, then the Image5DWindow again!
		 * 
		 * The reason for this rather complex structure is, that the Image5DWindow needs to 
		 * catch the keystrokes for changing c/z/t before ImageJ gets them,
		 * but needs to receive keys for text ROIs and the arrow keys after
		 * ImageJ got and processed them.
		 * 
		 * The same order of KeyListeners is applied to all sub-components.
		 * 
		 */
		addKeyListener(win);
		addKeyListener(ij);
		addKeyListener(win);

		// selectorPanel: contains the colorButton, displayChoice and
		// channel selector: scrollbar or channelSelectorOverlay.
		selectorPanel = new Panel(new BorderLayout(0, 0));
		add(selectorPanel, BorderLayout.CENTER);

		final Panel subPanel = new Panel(new GridLayout(2, 1, 1, 1));

		// colorButton: makes colorChooser visible/invisible
		colorButton = new Button(BUTTON_ACTIVATE_COLOR_CHOOSER);
		colorButton.setEnabled(true);
		colorChooserDisplayed = false;
		subPanel.add(colorButton);
		colorButton.addActionListener(this);

		colorButton.addKeyListener(win);
		colorButton.addKeyListener(ij);
		colorButton.addKeyListener(win);

		// displayChoice: selects desired display mode
		displayChoice = new Choice();
		for (final String mode : displayModes) {
			displayChoice.add(mode);
		}
		displayChoice.select(displayMode);
		subPanel.add(displayChoice);
		displayChoice.addItemListener(this);
		displayChoice.addKeyListener(win);
		displayChoice.addKeyListener(ij);
		displayChoice.addKeyListener(win);

		selectorPanel.add(subPanel, BorderLayout.NORTH);

		// channelSelectorOverlay: to select current and currently displayed
		// channels in overlay mode.
		addChannelSelectorOverlay();

		// channelSelectorTiled: to select current and currently displayed channels
		// in tiled mode.
		displayGrayInTiles = false;
		addTiledSelectorPanel();
		// add ij as KeyListener itself
		// scrollbar: select current channel in one channel modes
		addScrollbar();

		// Don't show selector, if there is only one channel;
		if (nChannels <= 1) {
			if (scrollbarWL != null) selectorPanel.remove(scrollbarWL);
		}

		// initially hidden Panel to select the ColorMap of the currently active
		// channel.
		cColorChooser = new ChannelColorChooser(this);

		// add ij as KeyListener itself
	}

	private void addScrollbar() {
		nChannels = i5d.getNChannels();
		if (scrollbarWL == null) {
			scrollbarWL =
				new ScrollbarWithLabel(Scrollbar.VERTICAL, 1, 1, 1, nChannels + 1, i5d
					.getDimensionLabel(2));
			scrollbarWL.addAdjustmentListener(this);
			scrollbarWL.setFocusable(false); // prevents scroll bar from accepting key
																				// shortcuts and from blinking on
																				// Windows
		}
		else {
			scrollbarWL.setMaximum(nChannels + 1);
		}

		int blockIncrement = nChannels / 10;
		if (blockIncrement < 1) blockIncrement = 1;
		scrollbarWL.setUnitIncrement(1);
		scrollbarWL.setBlockIncrement(blockIncrement);

		if (channelSelectorOverlay != null) selectorPanel
			.remove(channelSelectorOverlay);
		if (tiledSelectorPanel != null) selectorPanel.remove(tiledSelectorPanel);
		selectorPanel.add(scrollbarWL, BorderLayout.CENTER);
		win.pack();
		selectorPanel.repaint();
	}

	private void addChannelSelectorOverlay() {
		if (channelSelectorOverlay == null) {
			channelSelectorOverlay = new ChannelSelectorOverlay(this);
		}

		if (scrollbarWL != null) selectorPanel.remove(tiledSelectorPanel);
		if (tiledSelectorPanel != null) selectorPanel.remove(scrollbarWL);
		selectorPanel.add(channelSelectorOverlay, BorderLayout.CENTER);
		win.pack();
		selectorPanel.repaint();
	}

	private void addTiledSelectorPanel() {
		if (channelSelectorOverlay == null) {
			channelSelectorOverlay = new ChannelSelectorOverlay(this);
		}
		if (tiledSelectorPanel == null) {
			tiledSelectorPanel = new Panel(new BorderLayout());
		}
		if (allGrayInTilesBox == null) {
			allGrayInTilesBox = new Checkbox("all gray", displayGrayInTiles);
			allGrayInTilesBox.addItemListener(this);
			allGrayInTilesBox.addKeyListener(win);
			allGrayInTilesBox.addKeyListener(ij);
			allGrayInTilesBox.addKeyListener(win);
		}
		if (channelSelectorOverlay != null) selectorPanel
			.remove(channelSelectorOverlay);
		if (scrollbarWL != null) selectorPanel.remove(scrollbarWL);

		tiledSelectorPanel.add(allGrayInTilesBox, BorderLayout.NORTH);
		tiledSelectorPanel.add(channelSelectorOverlay, BorderLayout.CENTER);

		selectorPanel.add(tiledSelectorPanel, BorderLayout.CENTER);
		win.pack();
		selectorPanel.repaint();
	}

	public int getNChannels() {
		return nChannels;
	}

	public void setNChannels(final int nChannels) {
		if (nChannels < 1) return;
		if (scrollbarWL != null) {
			final int max = scrollbarWL.getMaximum();
			if (max != (nChannels + 1)) scrollbarWL.setMaximum(nChannels + 1);
		}
	}

//	/**
//	 * For changing the currently selected channel as displayed by the controls.
//	 * This method has no effect on the current channel in the Image5D.
//	 * currentChannel ranges from 1 to nChannels.
//	 */
//	public void setChannel(int currentChannel) {
//		// update nChannels and according control elements
//		if (currentChannel > nChannels || currentChannel < 1
//				|| this.channel == currentChannel)
//			return;
//		this.channel = currentChannel;
//		if (scrollbar != null) {
//			scrollbar.setValue(currentChannel);
//		}
//	}

	public int getCurrentChannel() {
		return currentChannel;
	}

	@Override
	public Dimension getMinimumSize() {
		int width = selectorPanel.getMinimumSize().width;
		if (colorChooserDisplayed) {
			width += cColorChooser.getMinimumSize().width;
		}

		int height = selectorPanel.getMinimumSize().height;
		if (colorChooserDisplayed) {
			height = Math.max(height, cColorChooser.getMinimumSize().height);
		}
		if (height > 500) height = 350;

		return new Dimension(width, height);
	}

	@Override
	public Dimension getPreferredSize() {
		int width = selectorPanel.getPreferredSize().width;
		if (colorChooserDisplayed) {
			width += cColorChooser.getPreferredSize().width;
		}

		int height = selectorPanel.getPreferredSize().height;
		if (colorChooserDisplayed) {
			height = Math.max(height, cColorChooser.getPreferredSize().height);
		}

		return new Dimension(width, height);
	}

	@Override
	public void itemStateChanged(final ItemEvent e) {
		if (e.getSource() == displayChoice) {
			if (displayChoice.getSelectedIndex() == displayMode) {
				return;
			}
			i5d.setDisplayMode(displayChoice.getSelectedIndex());
			updateChannelSelector();

		}
		else if (e.getSource() == allGrayInTilesBox) {
			if (allGrayInTilesBox.getState() == displayGrayInTiles) {
				return;
			}
			displayGrayInTiles = allGrayInTilesBox.getState();
			i5d.setDisplayGrayInTiles(displayGrayInTiles);
		}
	}

	public synchronized void updateSelectorDisplay() {
		nChannels = i5d.getNChannels();

		if (nChannels <= 1) {
			selectorPanel.remove(scrollbarWL);
			selectorPanel.remove(channelSelectorOverlay);
			selectorPanel.remove(tiledSelectorPanel);
		}
		else if (displayMode == OVERLAY) {
			addChannelSelectorOverlay();
		}
		else if (displayMode == TILED) {
			addTiledSelectorPanel();
		}
		else {
			addScrollbar();
		}
	}

	/**
	 * Sets the displayMode as seen in the channelControl. Doesn't do anything to
	 * the Image5D.
	 */
	public synchronized void setDisplayMode(final int mode) {
		if (mode == displayMode) {
			return;
		}

		if (mode == ONE_CHANNEL_GRAY) {
			if (colorChooserDisplayed) {
				remove(cColorChooser);
				colorButton.setLabel(BUTTON_ACTIVATE_COLOR_CHOOSER);
				colorChooserDisplayed = false;
			}
			colorButton.setEnabled(false);
			if (nChannels > 1) {
				addScrollbar();
			}
		}
		else if (mode == ONE_CHANNEL_COLOR) {
			colorButton.setEnabled(true);
			if (nChannels > 1) {
				addScrollbar();
			}
		}
		else if (mode == OVERLAY) {
			colorButton.setEnabled(true);
			if (nChannels > 1) {
				addChannelSelectorOverlay();
			}
		}
		else if (mode == TILED) {
			colorButton.setEnabled(true);
			if (nChannels > 1) {
				addTiledSelectorPanel();
			}
		}
		else {
			throw new IllegalArgumentException("Unknown display mode: " + mode);
		}
		displayMode = mode;
		displayChoice.select(mode);

		// save selected display mode in ImageJ preferences
		Prefs.set(DISPLAY_MODE_PREF, displayModes[displayMode]);
	}

	public int getDisplayMode() {
		return displayMode;
	}

	public synchronized void setDisplayGrayInTiles(
		final boolean displayGrayInTiles)
	{
		if (this.displayGrayInTiles == displayGrayInTiles &&
			allGrayInTilesBox != null &&
			allGrayInTilesBox.getState() == displayGrayInTiles)
		{
			return;
		}

		this.displayGrayInTiles = displayGrayInTiles;
		if (allGrayInTilesBox != null) {
			allGrayInTilesBox.setState(displayGrayInTiles);
		}
	}

	public boolean isDisplayGrayInTiles() {
		return displayGrayInTiles;
	}

	/**
	 * Executed if scrollbar has been moved.
	 */
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent e) {
		if (scrollbarWL != null && e.getSource() == scrollbarWL) {
			currentChannel = scrollbarWL.getValue();
			channelChanged(currentChannel);
		}
	}

	/**
	 * Called by ChannelSelectorOverlay if current channel has been changed.
	 */
	private void channelChanged(final int newChannel) {
		currentChannel = newChannel;
		win.channelChanged();
		cColorChooser.channelChanged();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == colorButton) {
			if (colorChooserDisplayed) {
				remove(cColorChooser);
				colorButton.setLabel(BUTTON_ACTIVATE_COLOR_CHOOSER);
				colorChooserDisplayed = false;
				win.pack();
			}
			else {
				add(cColorChooser, BorderLayout.EAST);
				colorButton.setLabel(BUTTON_DEACTIVATE_COLOR_CHOOSER);
				colorChooserDisplayed = true;
				win.pack();
			}
		}
	}

	/**
	 * Updates nChannels, currentChannel and colors of channels from values in
	 * Image5D.
	 */
	public void updateChannelSelector() {
		currentChannel = i5d.getCurrentChannel();
		// scrollbar
		nChannels = i5d.getNChannels();
		final int max = scrollbarWL.getMaximum();
		if (max != (nChannels + 1)) scrollbarWL.setMaximum(nChannels + 1);
		scrollbarWL.setValue(i5d.getCurrentChannel());

		// ChannelSelectorOverlay
		channelSelectorOverlay.updateFromI5D();

		// ChannelColorChooser
		cColorChooser.channelChanged();
//		win.pack();
	}

	/**
	 * Contains a radio button and a check button for each channel to select the
	 * current channel and toggle display of this channel. The background color of
	 * the buttons corresponds to the color of the channel (highest color in
	 * colormap). Also has a button to select all channels for display and
	 * deselect all.
	 * 
	 * @author Joachim Walter
	 */
	protected class ChannelSelectorOverlay extends Panel implements
		ActionListener, ItemListener
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = -8138803830180530129L;

		private ChannelControl cControl;

		private Panel allNonePanel;
		private Button allButton;
		private Button noneButton;

		private ScrollPane channelPane;
		private Panel channelPanel;

		private Checkbox[] channelDisplayed;
		private Checkbox[] channelActive;
		private CheckboxGroup channelActiveGroup;

		int nChannels;

		public ChannelSelectorOverlay(final ChannelControl cControl) {
			super(new BorderLayout());
			this.cControl = cControl;

			nChannels = cControl.nChannels;

			// Panel with "All" and "None" Buttons in NORTH of this control.
			allNonePanel = new Panel(new GridLayout(1, 2));
			allButton = new Button("All");
			allButton.addActionListener(this);
			allNonePanel.add(allButton);
			noneButton = new Button("None");
			noneButton.addActionListener(this);
			allNonePanel.add(noneButton);
			add(allNonePanel, BorderLayout.NORTH);

			addKeyListener(win);
			allButton.addKeyListener(win);
			noneButton.addKeyListener(win);

			// Pass key hits on to ImageJ
			addKeyListener(ij);
			allButton.addKeyListener(ij);
			noneButton.addKeyListener(ij);

			addKeyListener(win);
			allButton.addKeyListener(win);
			noneButton.addKeyListener(win);

			buildChannelSelector();

		}

		private void buildChannelSelector() {
			// ScrollPane, which contains selectors for all channels in CENTER of
			// control;
			// ScrollPane doesn't take LayoutManager, so add a panel for this.
			channelPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			channelPanel = new Panel();
			channelPane.add(channelPanel);
			add(channelPane, BorderLayout.CENTER);

			// Pass Mousewheel events to Image5DWindow. This didn't happen
			// automatically as for
			// the other components.
			channelPane.addMouseWheelListener(win);

			// KeyListener
			channelPane.addKeyListener(win);
			channelPanel.addKeyListener(win);

			// Pass key hits on to ImageJ
			channelPane.addKeyListener(ij);
			channelPanel.addKeyListener(ij);

			channelPane.addKeyListener(win);
			channelPanel.addKeyListener(win);

			nChannels = 0; // Make sure channelSelector is really updated.
			updateFromI5D();
		}

		/**
		 * Updates number of channels, selected and displayed channels from Image5D.
		 */
		/*        
		 * 	Is called from constructor (via buildChannelSelector) and from updateChannelSelector().
		 */
		private void updateFromI5D() {
			// If nChannels changed, build new channelPanel.
			if (nChannels != i5d.getNChannels()) {
				nChannels = i5d.getNChannels();

				channelPane.getVAdjustable().setUnitIncrement(20);

				channelPanel.removeAll();
				channelPanel.setLayout(new GridLayout(nChannels, 1));

				channelActiveGroup = new CheckboxGroup();
				channelDisplayed = new Checkbox[nChannels];
				channelActive = new Checkbox[nChannels];

				for (int i = 0; i < nChannels; ++i) {
					final Panel p = new Panel(new BorderLayout());
					channelActive[i] = new Checkbox();
					channelActive[i].setCheckboxGroup(channelActiveGroup);
					channelActive[i].addItemListener(this);
					p.add(channelActive[i], BorderLayout.WEST);

					channelDisplayed[i] = new Checkbox();
					channelDisplayed[i].addItemListener(this);
					p.add(channelDisplayed[i], BorderLayout.CENTER);
					channelPanel.add(p);

					channelActive[i].addKeyListener(win);
					channelDisplayed[i].addKeyListener(win);

					// Pass key hits on to ImageJ
					channelActive[i].addKeyListener(ij);
					channelDisplayed[i].addKeyListener(ij);

					channelActive[i].addKeyListener(win);
					channelDisplayed[i].addKeyListener(win);
				}
				win.pack();
			}

			// Update ChannelPanel from Image5D.
			for (int i = 0; i < nChannels; ++i) {
				channelActive[i].setBackground(new Color(i5d
					.getChannelDisplayProperties(i + 1).getColorModel().getRGB(255)));
				channelActive[i].setForeground(Color.black);
				channelActive[i].repaint();

				channelDisplayed[i].setLabel(i5d.getChannelCalibration(i + 1)
					.getLabel());

				channelDisplayed[i].setState(i5d.getChannelDisplayProperties(i + 1)
					.isDisplayedInOverlay());
				channelDisplayed[i].setBackground(new Color(i5d
					.getChannelDisplayProperties(i + 1).getColorModel().getRGB(255)));
				channelDisplayed[i].setForeground(Color.black);
				channelDisplayed[i].repaint();
			}

			channelActive[(i5d.getCurrentChannel() - 1)].setState(true);
		}

		@Override
		public Dimension getMinimumSize() {
			final int widthButtons = allNonePanel.getMinimumSize().width;
			final int widthChecks = channelPane.getMinimumSize().width;
			final int width = Math.max(widthButtons, widthChecks);

			int height = allNonePanel.getMinimumSize().height;
			height += nChannels * channelDisplayed[0].getMinimumSize().height;

			return new Dimension(width, height);
		}

		@Override
		public Dimension getPreferredSize() {
			final int widthButtons = allNonePanel.getPreferredSize().width;
			final int widthChecks = channelPane.getPreferredSize().width;
			final int width = Math.max(widthButtons, widthChecks);

			int height = allNonePanel.getPreferredSize().height;
			height += nChannels * channelDisplayed[0].getPreferredSize().height;

			return new Dimension(width, height);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == allButton) {
				for (int i = 0; i < nChannels; ++i) {
					channelDisplayed[i].setState(true);
					i5d.setDisplayedInOverlay(i + 1, true);
				}
				updateFromI5D();
				i5d.updateImageAndDraw();
			}
			else if (e.getSource() == noneButton) {
				for (int i = 0; i < nChannels; ++i) {
					channelDisplayed[i].setState(false);
					i5d.setDisplayedInOverlay(i + 1, false);
				}
				updateFromI5D();
				i5d.updateImageAndDraw();
			}
		}

		@Override
		public void itemStateChanged(final ItemEvent e) {
			for (int i = 0; i < nChannels; ++i) {
				if (e.getItemSelectable() == channelDisplayed[i]) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						i5d.setDisplayedInOverlay(i + 1, true);
					}
					else if (e.getStateChange() == ItemEvent.DESELECTED) {
						i5d.setDisplayedInOverlay(i + 1, false);
					}
					updateFromI5D();
					i5d.updateImageAndDraw();
					return;
				}
				if (e.getItemSelectable() == channelActive[i]) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						currentChannel = i + 1;
						cControl.channelChanged(currentChannel);
						return;
					}
				}
			}
		}

	}

	/**
	 * Panel to select the display color or colormap of the current channel.
	 * Contains the ChannelColorCanvas, where the user can select a color. There
	 * is a checkbox "displayGrayBox" to temporarily switch the channel to
	 * grayscale display without losing the display color. Pressing the Button
	 * "editLUTButton" opens the builtin LUT_Editor plugin for defining more
	 * complex LUTs.
	 * 
	 * @author Joachim Walter
	 */
	protected class ChannelColorChooser extends Panel implements ActionListener,
		ItemListener
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = -2987819237979841674L;
		private ChannelControl cControl;
		private Checkbox displayGrayBox;
		Button editColorButton;
		Button editLUTButton;
		private ChannelColorCanvas cColorCanvas;

		public ChannelColorChooser(final ChannelControl cControl) {
			super(new BorderLayout(5, 5));
			this.cControl = cControl;

			displayGrayBox = new Checkbox("Grayscale");
			displayGrayBox.setState(i5d.getChannelDisplayProperties(
				cControl.currentChannel).isDisplayedGray());
			displayGrayBox.addItemListener(this);
			add(displayGrayBox, BorderLayout.NORTH);

			cColorCanvas = new ChannelColorCanvas(this);
			add(cColorCanvas, BorderLayout.CENTER);

			final Panel tempPanel = new Panel(new GridLayout(2, 1, 5, 2));

			editColorButton = new Button("Edit Color");
			editColorButton.addActionListener(this);
			tempPanel.add(editColorButton);

			editLUTButton = new Button("Edit LUT");
			editLUTButton.addActionListener(this);
			tempPanel.add(editLUTButton);

			add(tempPanel, BorderLayout.SOUTH);

			addKeyListener(win);
			displayGrayBox.addKeyListener(win);
			cColorCanvas.addKeyListener(win);
			editColorButton.addKeyListener(win);
			editLUTButton.addKeyListener(win);

			addKeyListener(ij);
			displayGrayBox.addKeyListener(ij);
			cColorCanvas.addKeyListener(ij);
			editColorButton.addKeyListener(ij);
			editLUTButton.addKeyListener(ij);

			addKeyListener(win);
			displayGrayBox.addKeyListener(win);
			cColorCanvas.addKeyListener(win);
			editColorButton.addKeyListener(win);
			editLUTButton.addKeyListener(win);
		}

		/**
		 * Sets the Color of the current channel to c in the Image5D.
		 * 
		 * @param c Color
		 */
		public void setColor(final Color c) {
			i5d.storeChannelProperties(cControl.currentChannel);
			i5d.getChannelDisplayProperties(cControl.currentChannel).setColorModel(
				ChannelDisplayProperties.createModelFromColor(c));
			i5d.restoreChannelProperties(cControl.currentChannel);

			i5d.updateAndDraw();
			cControl.updateChannelSelector();
			cColorCanvas.repaint();
		}

//        /** Called by ChannelLUTEditor after LUT change
//         */
//        public void lutWasEdited() {            
//            ColorModel cm = i5d.channelIPs[cControl.activeChannel-1].getColorModel();
//            i5d.cChannelProps[cControl.activeChannel-1].setColorModel(cm);
//            i5d.restoreChannelProperties(cControl.activeChannel);
//            
//            i5d.updateAndDraw();
//            cControl.updateChannelSelector();
//            cColorCanvas.repaint();            
//        }	    

		/**
		 * Called from ChannelControl when active channel changes.
		 */
		private void channelChanged() {
			cColorCanvas.repaint();
			displayGrayBox.setState(i5d.getChannelDisplayProperties(
				cControl.currentChannel).isDisplayedGray());

			cColorCanvas.setEnabled(!displayGrayBox.getState());
			editColorButton.setEnabled(!displayGrayBox.getState());
			editLUTButton.setEnabled(!displayGrayBox.getState());

		}

		@Override
		public Dimension getMinimumSize() {
			final int width = cColorCanvas.getMinimumSize().width;
			final int height =
				displayGrayBox.getMinimumSize().height + 5 +
					cColorCanvas.getMinimumSize().height + 5 +
					editColorButton.getMinimumSize().height + 5 +
					editLUTButton.getMinimumSize().height;
			return new Dimension(width, height);
		}

		@Override
		public Dimension getPreferredSize() {
			final int width = cColorCanvas.getPreferredSize().width;
			final int height =
				displayGrayBox.getPreferredSize().height + 5 +
					cColorCanvas.getPreferredSize().height + 5 +
					editColorButton.getPreferredSize().height + 5 +
					editLUTButton.getPreferredSize().height;
			return new Dimension(width, height);
		}

		// Edit Color or edit LUT Button
		@Override
		public void actionPerformed(final ActionEvent e) {
			ColorModel cm =
				i5d.getChannelDisplayProperties(cControl.currentChannel)
					.getColorModel();
			i5d.storeChannelProperties(cControl.currentChannel);
			if (e.getSource() == editLUTButton) {
				new LUT_Editor().run("");
				cm = i5d.getProcessor(cControl.currentChannel).getColorModel();
			}
			else if (e.getSource() == editColorButton) {
				Color c =
					new Color(i5d.getProcessor(cControl.currentChannel).getColorModel()
						.getRGB(255));
				final ColorChooser cc = new ColorChooser("Channel Color", c, false);
				c = cc.getColor();
				if (c != null) {
					cm = ChannelDisplayProperties.createModelFromColor(c);
				}
			}
			i5d.getChannelDisplayProperties(cControl.currentChannel)
				.setColorModel(cm);
			i5d.restoreChannelProperties(cControl.currentChannel);

			i5d.updateAndDraw();
			cControl.updateChannelSelector();
			cColorCanvas.repaint();
		}

		// displayGray Checkbox
		@Override
		public void itemStateChanged(final ItemEvent e) {
			if (e.getItemSelectable() == displayGrayBox) {
				i5d.storeChannelProperties(cControl.currentChannel);
				i5d.getChannelDisplayProperties(cControl.currentChannel)
					.setDisplayedGray(displayGrayBox.getState());
				i5d.restoreChannelProperties(cControl.currentChannel);
				i5d.updateImageAndDraw();

				cColorCanvas.setEnabled(!displayGrayBox.getState());
				editColorButton.setEnabled(!displayGrayBox.getState());
				editLUTButton.setEnabled(!displayGrayBox.getState());
			}
		}

	}

	/**
	 * Canvas to select the display color of a channel. The Canvas contains a
	 * rectangle of colored squares. The hue of the squares changes in vertical
	 * direction. The saturation in horizontal direction. "Value" can be changed
	 * by changing contrast and brightness of the channel. The Canvas also
	 * contains an image of the current LUT.
	 * 
	 * @author Joachim Walter
	 */
	protected class ChannelColorCanvas extends Canvas implements MouseListener {

		/**
	 * 
	 */
		private static final long serialVersionUID = 3918313955836224104L;

		private ChannelColorChooser cColorChooser;

		Image lutImage;
		Image colorPickerImage;
		Image commonsImage;

		int lutWidth = 15;
		int lutHeight = 129;

		int cpNHues = 24;
		int cpNSats = 8;
		int cpRectWidth = 10;
		int cpRectHeight = 10;
		private int cpWidth;
		int cpHeight;

//        int commonsWidth = 105;
//        int commonsHeight = 15;
		int commonsWidth = 0;
		int commonsHeight = 0;

		int hSpacer = 5;
		int vSpacer = 0;
//        int vSpacer = 5;

		private int canvasWidth;
		int canvasHeight;

		public ChannelColorCanvas(final ChannelColorChooser cColorChooser) {
			super();

			this.cColorChooser = cColorChooser;

			setForeground(Color.black);
			setBackground(Color.white);

			cpWidth = cpRectWidth * cpNSats;
			cpHeight = cpRectHeight * cpNHues;

			canvasWidth = lutWidth + 2 + hSpacer + cpWidth + 2;
			canvasHeight = cpHeight + 2 + vSpacer + commonsHeight;

			setSize(canvasWidth, canvasHeight);

//            lutImage = new BufferedImage(lutWidth+2, lutHeight+2, BufferedImage.TYPE_INT_RGB);
//            colorPickerImage = new BufferedImage(cpWidth+2, cpHeight+2, BufferedImage.TYPE_INT_RGB);
			lutImage = GUI.createBlankImage(lutWidth + 2, lutHeight + 2);
			colorPickerImage = GUI.createBlankImage(cpWidth + 2, cpHeight + 2);
//	        commonsImage = new BufferedImage(commonsWidth, commonsHeight, BufferedImage.TYPE_INT_RGB);

			drawLUT();
			drawColorPicker();
//	        drawCommons();

			addMouseListener(this);

		}

		/**
		 * Draws the LUT of the currently displayed channel into a rectangle.
		 */
		private void drawLUT() {
			final Graphics g = lutImage.getGraphics();
			final int[] rgb = new int[256];

			final ColorModel currentLUT =
				i5d.getChannelDisplayProperties(cColorChooser.cControl.currentChannel)
					.getColorModel();
			if (!(currentLUT instanceof IndexColorModel)) throw new IllegalArgumentException(
				"Color Model has to be IndexColorModel.");

			for (int i = 0; i < 256; i++) {
				rgb[i] = ((IndexColorModel) currentLUT).getRGB(i);
			}

			g.setColor(Color.black);
			g.drawRect(0, 0, lutWidth + 2, lutHeight + 2);

			// check consistency with lutHeight!
			g.setColor(new Color(rgb[255]));
			g.drawLine(1, 1, lutWidth, 1);
			for (int i = 0; i < 128; i++) {
				g.setColor(new Color(rgb[255 - i * 2]));
				g.drawLine(1, i + 2, lutWidth, i + 2);
			}
		}

		/**
		 * Draws the color picker image: a rectangle of small squares all with
		 * different hue or saturation.
		 */
		private void drawColorPicker() {
			final Graphics g = colorPickerImage.getGraphics();

			float hue = 0;
			float sat = 0;
			g.setColor(Color.black);
			g.drawRect(0, 0, cpWidth + 2, cpHeight + 2);
			for (int h = 0; h < cpNHues; h++) {
				for (int s = 0; s < cpNSats; s++) {
					hue = h / (float) cpNHues;
					sat = s / (float) (cpNSats - 1);
					g.setColor(Color.getHSBColor(hue, sat, 1f));
					g.fillRect(cpWidth - (s + 1) * cpRectWidth + 1, h * cpRectHeight + 1,
						cpRectWidth, cpRectHeight);
				}
			}

		}

//		Commons: larger squares with commonly used colors.	    
//	    void drawCommons() {       
//	        Graphics g = commonsImage.getGraphics();
//	        g.setColor(Color.red);
//	        g.fillRect(0, 0, commonsHeight, commonsHeight);
//	        
//	        g.setColor(Color.green);
//	        g.fillRect(commonsHeight, 0, commonsHeight, commonsHeight);
//	        
//	        g.setColor(Color.blue);
//	        g.fillRect(2*commonsHeight, 0, commonsHeight, commonsHeight);
//
//	        g.setColor(Color.cyan);
//	        g.fillRect(3*commonsHeight, 0, commonsHeight, commonsHeight);
//	        
//	        g.setColor(Color.magenta);
//	        g.fillRect(4*commonsHeight, 0, commonsHeight, commonsHeight);
//	        
//	        g.setColor(Color.yellow);
//	        g.fillRect(5*commonsHeight, 0, commonsHeight, commonsHeight);
//	        
//	        g.setColor(Color.white);
//	        g.fillRect(6*commonsHeight, 0, commonsHeight, commonsHeight);
//	    }

		@Override
		public void paint(final Graphics g) {
			g.drawImage(colorPickerImage, lutWidth + 2 + hSpacer, 0, cpWidth + 2,
				cpHeight + 2, null);
			drawLUT();
			g.drawImage(lutImage, 0, 0, lutWidth + 2, lutHeight + 2, null);
//	        g.drawImage(commonsImage, 0, cpHeight+vSpacer, commonsWidth, commonsHeight, null);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(canvasWidth, canvasHeight);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(canvasWidth, canvasHeight);
		}

		/**
		 * Change color, if user presses in one of the colored squares.
		 */
		@Override
		public void mousePressed(final MouseEvent e) {

			final int x = e.getX();
			final int y = e.getY();

			final Rectangle colorPickerRect =
				new Rectangle(lutWidth + 2 + hSpacer + 1, 1, cpWidth, cpHeight);

			if (colorPickerRect.contains(x, y)) {
				final int xCP = x - (lutWidth + 2 + hSpacer);
				final int yCP = y;

				final int[] pixels32 = new int[1];
				final PixelGrabber pg =
					new PixelGrabber(colorPickerImage, xCP, yCP, 1, 1, pixels32, 0,
						cpWidth);
				pg.setColorModel(ColorModel.getRGBdefault());
				try {
					pg.grabPixels();
				}
				catch (final InterruptedException ie) {
					return;
				}
//                IJ.log("x "+xCP+"  y "+yCP+"  "+((pixels32[0]>>16)&0xff)+" "+((pixels32[0]>>8)&0xff)+" "+((pixels32[0])&0xff));
				Color c = new Color(pixels32[0]);
//                IJ.log("r "+ c.getRed()+"  g "+ c.getGreen()+"  b "+c.getBlue());

//	            Color c = new Color(colorPickerImage.getRGB(xCP, yCP));
//	            
				cColorChooser.setColor(c);

				if (e.getClickCount() == 2) {
					final ColorChooser cc = new ColorChooser("Channel Color", c, false);
					c = cc.getColor();
					if (c != null) {
						cColorChooser.setColor(c);
					}
				}

			}
		}

		@Override
		public void mouseClicked(final MouseEvent e) {}

		@Override
		public void mouseReleased(final MouseEvent e) {}

		@Override
		public void mouseEntered(final MouseEvent e) {}

		@Override
		public void mouseExited(final MouseEvent e) {}

	}

}
