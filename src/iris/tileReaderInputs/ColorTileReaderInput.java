/**
 * 
 */
package iris.tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;
import iris.settings.ColorSettings;
import iris.ui.IrisFrontend;

import java.awt.Point;

/**
 * @author George Kritikos
 *
 */
public class ColorTileReaderInput extends TileReaderInput {
	public ColorSettings settings;
	public ImagePlus tileImage;
	public Point colonyCenter;

	/**
	 * Creates a BasicTileReaderInput obect, given the cropped, grayscaled image and the ROI
	 * corresponding to the image tile to be processed.
	 * The Roi is applied to the image, and then the ROI is copied to a new ImagePlus
	 * object, which is saved in this BasicTileReaderInput object.
	 * @param croppedImage
	 * @param roi
	 * @param settings_
	 */
	public ColorTileReaderInput(ImagePlus croppedImage, Roi roi, ColorSettings settings_){

		if(IrisFrontend.settings.userDefinedRoi==true){
			this.tileImage = croppedImage.duplicate(); // otherwise I'd have to delete the user-defined ROI
			this.tileImage.setRoi(croppedImage.getRoi());
		}
		else{
			croppedImage.setRoi(roi);
			croppedImage.copy(false);

			this.tileImage = ImagePlus.getClipboard();
		}
		this.settings = settings_;
	}

	/**	
	 * Creates a ColorTileReaderInput object, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	public ColorTileReaderInput(ImagePlus tileImage_, ColorSettings settings_){
		tileImage = tileImage_;
		settings = settings_;
	}


	/**
	 * @param colourCroppedImage
	 * @param roi
	 * @param settings2
	 * @param point
	 */
	public ColorTileReaderInput(ImagePlus croppedImage, Roi roi,
			ColorSettings settings_, Point colonyCenter_) {

		if(IrisFrontend.settings.userDefinedRoi==true){
			this.tileImage = croppedImage.duplicate(); // otherwise I'd have to delete the user-defined ROI
			this.tileImage.setRoi(croppedImage.getRoi());
		}
		else{
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
}
