/**
 * 
 */
package iris.tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;
import iris.settings.BasicSettings;
import iris.ui.IrisFrontend;

import java.awt.Point;

/**
 * @author George Kritikos
 *
 */
public class OpacityTileReaderInput extends BasicTileReaderInput {

	public Roi colonyRoi;
	public int colonySize;

	/**
	 * @param tileImage_
	 * @param settings_
	 */
	public OpacityTileReaderInput(ImagePlus tileImage_, BasicSettings settings_) {
		super(tileImage_, settings_);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param tileImage_
	 * @param roi_
	 * @param settings_
	 */
	public OpacityTileReaderInput(ImagePlus croppedImage_, Roi tile_roi_, BasicSettings settings_) {
		super(croppedImage_, tile_roi_, settings_);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param tileImage_
	 * @param roi_
	 * @param settings_
	 */
	public OpacityTileReaderInput(ImagePlus croppedImage_, Roi tile_roi_, Roi colonyRoi_, int colonySize_, BasicSettings settings_) {
		super(croppedImage_, tile_roi_, settings_);
		this.colonyRoi = colonyRoi_;
		this.colonySize = colonySize_;
		
		if(IrisFrontend.settings.userDefinedRoi){
			//get the user-defined ROI from the tile image 
			//(this is where its normally saved when user selects the roi
			this.colonyRoi = this.tileImage.getRoi(); 
		}
	}

	public OpacityTileReaderInput(BasicTileReaderInput that){
		super(that.tileImage, that.settings);
		if(that.colonyCenter==null)
			this.colonyCenter=null;
		else
			this.colonyCenter = new Point(that.colonyCenter);
	}


}
