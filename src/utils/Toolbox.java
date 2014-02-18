/**
 * 
 */
package utils;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
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
	
	
	
	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Otsu algorithm. 
	 * This function does not return the threshold
	 * @param 
	 */
	public static void turnImageBW_Otsu_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Otsu, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Huang algorithm
	 * This function does not return the threshold
	 * @param 
	 */
	public static void turnImageBW_Huang_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Huang, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Minimum algorithm
	 * This function does not return the threshold
	 * @param 
	 */
	public static void turnImageBW_Minimum_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Minimum, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * @deprecated: see Evernote note on how this algorithm performs on overgrown colonies
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the MinError algorithm
	 * This function does not return the threshold
	 * @param
	 */
	public static void turnImageBW_MinError_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.MinError, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}
	
	
	/**
	 * @deprecated: see Evernote note on how this algorithm performs on overgrown colonies
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the RenyiEntropy algorithm
	 * This function does not return the threshold
	 * @param
	 */
	public static void turnImageBW_RenyiEntropy_auto(ImagePlus BW_croppedImage) {
		
		System.out.println(Integer.toString(getThreshold(BW_croppedImage, Method.RenyiEntropy)));
		
		BW_croppedImage.show();
		BW_croppedImage.hide();
		
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.RenyiEntropy, true, ImageProcessor.BLACK_AND_WHITE_LUT);
		
		
	}
	
	
	/**
	 * This method will return the threshold found by the Otsu method and do nothing else
	 * @param grayscale_image
	 * @return
	 */
	public static int getThreshold(ImagePlus grayscale_image, Method method){

		//get all the objects required: calibration, imageProcessor and histogram
		Calibration calibration = new Calibration(grayscale_image);		
		ImageProcessor imageProcessor = grayscale_image.getProcessor();
		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		//use that histogram to find a threshold
		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(method, histogram);

		return(threshold);
	}

}
