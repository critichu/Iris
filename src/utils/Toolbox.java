/**
 * 
 */
package utils;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * This class holds methods that are not profile-specific
 * This should reduce code duplication, and make bug fixing easier.
 * @author George Kritikos
 *
 */
public class Toolbox {

	/**
	 * This function will create a copy of the original image, and rotate that copy.
	 * The original image should be flushed by the caller if not reused
	 * @param originalImage
	 * @param angle
	 * @return
	 */
	public static ImagePlus rotateImage(ImagePlus originalImage, double angle) {

		originalImage.deleteRoi();
		ImagePlus aDuplicate = originalImage.duplicate();//because the caller is going to flush the original image

		aDuplicate.getProcessor().setBackgroundValue(0);

		//IJ.run(aDuplicate, "Arbitrarily...", "angle=" + angle + " grid=0 interpolate enlarge");
		aDuplicate.getProcessor().rotate(angle);

		aDuplicate.updateImage();

		return(aDuplicate);
	}

	/**
	 * Saves a picture, sparing us the dramatic ImageJ's pass through the GUI elements
	 * @param croppedImage
	 * @param string
	 */
	public static void savePicture(ImagePlus inputImage, String path) {
		FileSaver saver = new FileSaver(inputImage);
		saver.saveAsJpeg(path);
		
	}

	/**
	 * This function will return the area covered by a ROI
	 * @param imp
	 * @param roi
	 * @return
	 */
	public static float getRoiArea(ImagePlus img, Roi roi) {
		ImageProcessor ip = img.getProcessor(); 
		ip.setRoi(roi); 
		ImageStatistics stats = ImageStatistics.getStatistics(ip, 
				Measurements.MEAN, img.getCalibration()); 
		return((float) stats.area); 
	}
	
	
	/**
	 * This function will return the circumference of a ROI
	 * @param imp
	 * @param roi
	 * @return
	 * @deprecated: doesn't work..
	 */
	public static float getRoiPerimeter(ImagePlus img, Roi roi) {
		ImageProcessor ip = img.getProcessor(); 
		ip.setRoi(roi); 
		ImageStatistics stats = ImageStatistics.getStatistics(ip, 
				Measurements.MEAN, img.getCalibration()); 
		return((float) stats.binSize); 
	}

}
