/**
 * 
 */
package imageCroppers;

import ij.ImagePlus;
import ij.gui.Roi;

/**
 *This class provides methods to crop the original picture so as to keep only the colonies.
 *The Naive image cropper crops the picture in predefined (hard-coded) places
 *
 */
public abstract class NaiveImageCropper {
	
	/**
	 * This method will naively crop the plate in a hard-coded manner.
	 * It copies the area of interest (580, 380, 4080, 2730) to the internal clipboard.
	 * Then, it copies the internal clipboard results to a new ImagePlus object.
	 * @param originalPicture
	 * @return
	 */
	public static ImagePlus cropPlate(ImagePlus originalImage){
		//crop the plate so that we keep only the colonies
		//Roi keepOnlyColoniesROI = new Roi(580, 380, 4080, 2730);
		Roi keepOnlyColoniesROI = new Roi(590, 380, 4130, 2730);
		originalImage.setRoi(keepOnlyColoniesROI);
		originalImage.copy(false);//copy to the internal clipboard
		
		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		return(croppedImage);

	}
}
