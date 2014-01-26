/**
 * 
 */
package tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;
import settings.ColorSettings;

/**
 * This class does exactly the same job as ColorTileReaderInput, 
 * but this time, it also passes to the Tile Reader an already thresholded tile
 * image (using local thresholding on the complete picture).
 * The reason for doing this, is because local thresholding algorithms have been shown to work
 * better on CoCo pictures, but only if applied to the complete picture.
 * If done per tile, there is a lot of noise introduced.
 * @author George Kritikos
 *
 */
public class ColorTileReaderInput2 extends TileReaderInput {
	public ColorSettings settings;
	public ImagePlus tileImage;
	public ImagePlus thresholdedTileImage;
	
	/**
	 * Creates a BasicTileReaderInput object, given the cropped, grayscaled image and the ROI
	 * corresponding to the image tile to be processed.
	 * The Roi is applied to the image, and then the ROI is copied to a new ImagePlus
	 * object, which is saved in this BasicTileReaderInput object.
	 * @param croppedImage
	 * @param roi
	 * @param settings_
	 */
	public ColorTileReaderInput2(ImagePlus croppedImage, ImagePlus thresholdedImage, Roi roi, ColorSettings settings_){
		croppedImage.setRoi(roi);
		croppedImage.copy(false);
		this.tileImage = ImagePlus.getClipboard();
		ImagePlus.resetClipboard();
		
		thresholdedImage.setRoi(roi);
		thresholdedImage.copy(false);
		this.thresholdedTileImage = ImagePlus.getClipboard();
		ImagePlus.resetClipboard();
		
		this.settings = settings_;
	}
	
	/**	
	 * Creates a ColorTileReaderInput object, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	public ColorTileReaderInput2(ImagePlus tileImage_, ImagePlus thresholdedTileImage_, ColorSettings settings_){
		tileImage = tileImage_;
		thresholdedTileImage = thresholdedTileImage_;
		settings = settings_;
	}
	
	
	/**
	 * this function should be called after the input object has been used, 
	 * in order to free the memory used by the tile image object
	 */
	public void cleanup(){
		tileImage.flush();
		thresholdedTileImage.flush();
	}
}
