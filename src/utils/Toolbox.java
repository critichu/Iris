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
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

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
		
		aDuplicate.setTitle(originalImage.getTitle());

		return(aDuplicate);
	}
	
	
	
	/**
	 * This method gets a subset of that picture (for faster execution), and calculates the rotation of that part
	 * using an OCR-derived method. The method applied here rotates the image, attempting to maximize
	 * the variance of the sums of row and column brightnesses. This is in direct analogy to detecting skewed text
	 * in a scanned document, as part of the OCR procedure.
	 * @param originalImage
	 * @return the angle of this picture's rotation 
	 */
	public static double calculateImageRotation(ImagePlus originalImage) {
		//1. get a subset of that picture
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		int roiX = (int)Math.round(3.0*width/8.0);
		int roiY = (int)Math.round(3.0*height/8.0);
		int roiWidth = (int)Math.round(1.0*width/4.0);
		int roiHeight = (int)Math.round(1.0*height/4.0);

		Roi centerRectangle = new Roi(roiX, roiY, roiWidth, roiHeight);
		ImagePlus imageSubset = cropImage(originalImage, centerRectangle);


		//2. make grayscale, then auto-threshold to get black/white picture
		ImageConverter imageConverter = new ImageConverter(imageSubset);
		imageConverter.convertToGray8();

		//convert to b/w
		turnImageBW_Otsu_auto(imageSubset);


		//3. iterate over different angles
		double initialAngle = -2;
		double finalAngle = 2;
		double angleIncrements = 0.25;


		double bestAngle = 0;
		double bestVariance = -Double.MAX_VALUE;

		for(double angle = initialAngle; angle<=finalAngle; angle+=angleIncrements){
			//3.1 rotate the b/w picture
			ImagePlus rotatedImage = Toolbox.rotateImage(imageSubset, angle);			

			//3.2 calculate sums of rows and columns
			ArrayList<Integer> sumOfColumns = sumOfColumns(rotatedImage);
			ArrayList<Integer> sumOfRows = sumOfRows(rotatedImage);

			//3.3 calculate their variances
			double varianceColumns = getVariance(sumOfColumns);
			double varianceRows = getVariance(sumOfRows);
			double varianceSum = varianceColumns + varianceRows;

			//3.4 pick the best (biggest) variance, store it's angle
			if(varianceSum > bestVariance){
				bestAngle = angle;
				bestVariance = varianceSum;
			}

			rotatedImage.flush(); //we don't need this anymore, it was a copy after all
		}

		return(bestAngle);			
	}
	
	
	
	/**
	 * This method will naively crop the plate in a hard-coded manner.
	 * It copies the given area of interest to the internal clipboard.
	 * Then, it copies the internal clipboard results to a new ImagePlus object.
	 * @param originalPicture
	 * @return
	 */
	public static ImagePlus cropImage(ImagePlus originalImage, Roi roi){
		originalImage.setRoi(roi);
		originalImage.copy(false);//copy to the internal clipboard
		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		return(croppedImage);

	}
	
	
	
	/**
	 * I cannot believe I have to write this
	 * @param list
	 * @return
	 */
	public static double getMean(ArrayList<Integer> list){

		int sum = 0;

		for(int i=0;i<list.size();i++){
			sum += list.get(i);
		}

		return(sum/list.size());
	}

	/**
	 * There has to be a better way guys..
	 * @param list
	 * @return
	 */
	public static double getVariance(ArrayList<Integer> list){
		double mean = getMean(list);

		double sum = 0;

		for(int i=0;i<list.size();i++){			
			sum += Math.pow(list.get(i)-mean, 2);
		}

		return(sum/(list.size()-1));

	}
	
	
	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	private static ArrayList<Integer> sumOfRows(ImagePlus croppedImage){
		int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows

		ArrayList<Integer> sumOfRows = new ArrayList<Integer>(dimensions[1]);

		int sum = 0;

		//for all rows
		for(int y=0; y<dimensions[1]; y++ ){
			sum = 0;

			//for all columns
			for(int x=0; x<dimensions[0]; x++ ){

				sum += croppedImage.getPixel(x, y)[0];
			}

			//sum complete, add it to the list
			//sumOfRows.set(y, sum);
			sumOfRows.add(sum);
		}

		return(sumOfRows);
	}





	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's rows (for every y)
	 * @param croppedImage
	 * @return
	 */
	public static ArrayList<Integer> sumOfColumns(ImagePlus croppedImage){
		int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows and columns
		ArrayList<Integer> sumOfColumns = new ArrayList<Integer>(dimensions[0]);

		int sum = 0;

		//for all columns
		for(int x=0; x<dimensions[0]; x++ ){
			sum = 0;

			//for all rows
			for(int y=0; y<dimensions[1]; y++ ){

				sum += croppedImage.getPixel(x, y)[0];
			}

			//sum complete, add it to the list
			//sumOfColumns.set(x, sum);
			sumOfColumns.add(sum);
		}

		return(sumOfColumns);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using the Otsu method. This version will also return the threshold found.
	 * @param 
	 */
	public static int turnImageBW_Otsu(ImagePlus grayscaleImage) {
		Calibration calibration = new Calibration(grayscaleImage);

		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = grayscaleImage.getProcessor();

		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Otsu, histogram);

		imageProcessor.threshold(threshold);

		//BW_croppedImage.updateAndDraw();

		return(threshold);
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
	
	
	
	/**
	 * Normally, you can only create an Ellipse2D defining the top left coordinates.
	 * This function allows you to create an Ellipse2D by defining the center coordinates (x, y).
	 * Internally, this function will transform the center coordinates to the correct top-left coordinates
	 * and return the requested ellipse.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public static Ellipse2D getEllipseFromCenter(double x, double y, double width, double height)
	{
	    double newX = x - width / 2.0;
	    double newY = y - height / 2.0;

	    Ellipse2D ellipse = new Ellipse2D.Double(newX, newY, width, height);

	    return ellipse;
	}
	
	
	
	/**
	 * This method simply iterates through this array and finds the index
	 * of the largest element
	 */
	public static int getIndexOfMaximumElement(int[] array) {
		int index = -1;
		float max = -Integer.MAX_VALUE;
		
		for (int i = 0; i < array.length; i++) {
			if(array[i]>max){
				max = array[i];
				index = i;
			}
		}
		
		return(index);
	}
	
	

}
