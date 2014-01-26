/**
 * 
 */
package tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;

import javax.swing.JFrame;

import settings.BasicSettings;
import settings.Settings;

/**
 * This class holds information required by the TileReader
 * @author george
 *
 */
public class BasicTileReaderInput extends TileReaderInput {
	public BasicSettings settings;
	public ImagePlus tileImage;
	
	/**
	 * Creates a BasicTileReaderInput obect, given the cropped, grayscaled image and the ROI
	 * corresponding to the image tile to be processed.
	 * The Roi is applied to the image, and then the ROI is copied to a new ImagePlus
	 * object, which is saved in this BasicTileReaderInput object.
	 * @param croppedImage
	 * @param roi
	 * @param settings_
	 */
	public BasicTileReaderInput(ImagePlus croppedImage, Roi roi, BasicSettings settings_){
		croppedImage.setRoi(roi);
		croppedImage.copy(false);
		
		this.tileImage = ImagePlus.getClipboard();
		this.settings = settings_;
	}
	
	/**	
	 * Creates a BasicTileReaderInput obect, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	public BasicTileReaderInput(ImagePlus tileImage_, BasicSettings settings_){
		tileImage = tileImage_;
		settings = settings_;
	}
	
	
	/**
	 * this function should be called after the input object has been used, 
	 * in order to free the memory used by the tile image object
	 */
	public void cleanup(){
		tileImage.flush();
	}
}


