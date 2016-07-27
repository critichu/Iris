/**
 * 
 */
package imageCroppers;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;

/**
 *This class provides methods to crop the original picture so as to keep only the colonies.
 *The Naive image cropper crops the picture in predefined (hard-coded) places.
 *This differs from the first Naive image cropper in the sense that it's better for zoomed-in plates
 *
 */
public abstract class NaiveImageCropper3 {
	
	
	/**
	 * this ROI is designed to keep just the colonies, given a picture from
	 * the Sympatico picture taking robot (with the Canon T3i Rebel camera).
	 * ROI is defined as: left, top, width, height in pixels
	 * left and top are measured from the top left pixel of the picture
	 * width and height are measured from the ROI starting point (left, top)
	 */
	public static Roi keepOnlyColoniesROI = new Roi(470, 325, 4150, 2750);
	
	/**
	 * This method will naively crop the plate in a hard-coded manner.
	 * It copies the area of interest (580, 380, 4080, 2730) to the internal clipboard.
	 * Then, it copies the internal clipboard results to a new ImagePlus object.
	 * @param originalPicture
	 * @return
	 */
	public static ImagePlus cropPlate(ImagePlus originalImage){
		
		//if user has cropped the picture, no need to re-crop
		if(IrisFrontend.singleColonyRun==true){
			return(originalImage.duplicate());
		}

		
		//crop the plate so that we keep only the colonies
		//Roi keepOnlyColoniesROI = new Roi(580, 380, 4080, 2730);
		
		originalImage.setRoi(keepOnlyColoniesROI);
		originalImage.copy(false);//copy to the internal clipboard
		
		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		croppedImage.setTitle(originalImage.getTitle());
		return(croppedImage);

	}
}
