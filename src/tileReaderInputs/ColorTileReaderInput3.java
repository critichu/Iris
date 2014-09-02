/**
 * 
 */
package tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;
import settings.ColorSettings;

/**
 * @author George Kritikos
 *
 */
public class ColorTileReaderInput3 extends TileReaderInput {
	public ColorSettings settings;
	public ImagePlus tileImage;
	public Roi colonyRoi;
	public int colonySize;
	
	/**
	 * Creates a BasicTileReaderInput obect, given the cropped, grayscaled image and the ROI
	 * corresponding to the image tile to be processed.
	 * The Roi is applied to the image, and then the ROI is copied to a new ImagePlus
	 * object, which is saved in this BasicTileReaderInput object.
	 * @param croppedImage
	 * @param roi
	 * @param settings_
	 */
	public ColorTileReaderInput3(ImagePlus croppedImage, Roi tileRoi, Roi colonyRoi_, int colonySize_, ColorSettings settings_){
		croppedImage.setRoi(tileRoi);
		croppedImage.copy(false);
		
		this.tileImage = ImagePlus.getClipboard();
		this.settings = settings_;
		this.colonyRoi = colonyRoi_;
		this.colonySize = colonySize_;
	}
	
	/**	
	 * Creates a ColorTileReaderInput object, given the image tile to be processed
	 * @param tileImage_
	 * @param settings_
	 */
	public ColorTileReaderInput3(ImagePlus tileImage_, Roi colonyRoi_, int colonySize, ColorSettings settings_){
		tileImage = tileImage_;
		settings = settings_;
		this.colonyRoi = colonyRoi_;
	}
	
	
	/**
	 * this function should be called after the input object has been used, 
	 * in order to free the memory used by the tile image object
	 */
	public void cleanup(){
		tileImage.flush();
	}
}
