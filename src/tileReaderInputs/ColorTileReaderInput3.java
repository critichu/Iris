/**
 * 
 */
package tileReaderInputs;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Point;

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
	public ColorTileReaderInput3(ImagePlus croppedImage, Roi tileRoi, Roi colonyRoi_, int colonySize_, Point colonyCenter_, ColorSettings settings_){

		if(IrisFrontend.singleColonyRun==true){
			//single colony: tile is the croppedImage itself
			this.tileImage = croppedImage.duplicate();
			this.colonyRoi = colonyRoi_;
			
			if(IrisFrontend.settings.userDefinedRoi==true){
				//preserve the user-defined ROI
				this.tileImage.setRoi(croppedImage.getRoi());
				this.colonyRoi = croppedImage.getRoi();
			}
		}	
		else{
			//normally, copy the tileRoi section of the cropped image into the tile image
			croppedImage.setRoi(tileRoi);
			croppedImage.copy(false);
			this.tileImage = ImagePlus.getClipboard();
			this.colonyRoi = colonyRoi_;
		}
		
		this.settings = settings_;
		this.colonySize = colonySize_;
		this.colonyCenter = colonyCenter_;
	}

	//	/**	
	//	 * Creates a ColorTileReaderInput object, given the image tile to be processed
	//	 * @param tileImage_
	//	 * @param settings_
	//	 */
	//	public ColorTileReaderInput3(ImagePlus tileImage_, Roi colonyRoi_, int colonySize, Point colonyCenter_, ColorSettings settings_){
	//		tileImage = tileImage_;
	//		settings = settings_;
	//		this.colonyRoi = colonyRoi_;
	//		this.colonyCenter = colonyCenter_;
	//
	//		if(IrisFrontend.settings.userDefinedRoi){
	//			//get the user-defined ROI from the tile image 
	//			//(this is where its normally saved when user selects the roi
	//			this.colonyRoi = this.tileImage.getRoi(); 
	//		}
	//	}


	/**
	 * this function should be called after the input object has been used, 
	 * in order to free the memory used by the tile image object
	 */
	public void cleanup(){
		tileImage.flush();
	}
}
