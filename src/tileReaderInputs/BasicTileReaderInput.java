/**
 * 
 */
package tileReaderInputs;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Point;

import settings.BasicSettings;

/**
 * This class holds information required by the TileReader
 * @author george
 *
 */
public class BasicTileReaderInput extends TileReaderInput {
	public BasicSettings settings;
	public ImagePlus tileImage;
	public Point colonyCenter = null;

	/**
	 * Creates a BasicTileReaderInput obect, given the cropped, grayscaled whole image and the ROI
	 * corresponding to the image tile to be processed.
	 * The Roi is applied to the image, and then the ROI is copied to a new ImagePlus
	 * object, which is saved in this BasicTileReaderInput object.
	 * @param croppedImage
	 * @param roi
	 * @param settings_
	 */
	public BasicTileReaderInput(ImagePlus croppedImage, Roi roi, BasicSettings settings_){

		if(IrisFrontend.singleColonyRun==true){
			//single colony: tile is the croppedImage itself
			this.tileImage = croppedImage.duplicate(); 

			if(IrisFrontend.settings.userDefinedRoi==true){
				//preserve the user-defined ROI
				this.tileImage.setRoi(croppedImage.getRoi());
			}
		}
		else{		
			synchronized(settings_){
				croppedImage.setRoi(roi);
				croppedImage.copy(false);

				this.tileImage = ImagePlus.getClipboard();
			}
		}
		this.settings = settings_;
	}

	/**	
	 * Creates a BasicTileReaderInput obect, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	BasicTileReaderInput(ImagePlus tileImage_, BasicSettings settings_){
		tileImage = tileImage_;
		settings = settings_;
	}

	/**	
	 * Creates a BasicTileReaderInput obect, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	private BasicTileReaderInput(ImagePlus tileImage_, BasicSettings settings_, Point colonyCenter_){
		tileImage = tileImage_;
		settings = settings_;
		colonyCenter = new Point(colonyCenter_);
	}


	/**
	 * @param grayscaleCroppedImage
	 * @param roi
	 * @param settings2
	 * @param point
	 */
	public BasicTileReaderInput(ImagePlus croppedImage, Roi roi,
			BasicSettings settings_, Point colonyCenter_) {
		synchronized(settings_){
			croppedImage.setRoi(roi);
			croppedImage.copy(false);

			this.tileImage = ImagePlus.getClipboard();
		}
		this.settings = settings_;

		colonyCenter = new Point(colonyCenter_);
	}

	/**
	 * this function should be called after the input object has been used, 
	 * in order to free the memory used by the tile image object
	 */
	public void cleanup(){
		tileImage.flush();
	}

	/**
	 * Create a proper copy of this object
	 */
	public BasicTileReaderInput clone(){
		return(new BasicTileReaderInput(this.tileImage.duplicate(), this.settings, new Point(this.colonyCenter)));
	}
}


