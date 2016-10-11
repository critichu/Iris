/**
 * 
 */
package utils;

import fiji.threshold.Auto_Local_Threshold;
import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imageSegmenterOutput.BasicImageSegmenterOutput;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import settings.ColorSettings;
import tileReaderInputs.BasicTileReaderInput;
import tileReaderInputs.ColorTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaders.BasicTileReader_Bsu;

import com.opencsv.CSVReader;

/**
 * This class holds methods that are not profile-specific
 * This should reduce code duplication, and make bug fixing easier.
 * @author George Kritikos
 *
 */
public class Toolbox {


	/**
	 * Returns the center of mass of the biggest particle in the results table
	 */
	public static Point getBiggestParticleCenterOfMass(ResultsTable resultsTable, int indexOfBiggestParticle) {

		//get the coordinates of all the particles the particle analyzer has found		
		float X_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("XM"));//get the X of the center of mass of all the particles
		float Y_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("YM"));//get the Y of the center of mass of all the particles


		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = indexOfBiggestParticle;//getIndexOfMaximumElement(areas);

		return( new Point(	Math.round(X_center_of_mass[indexOfMax]),
				Math.round(Y_center_of_mass[indexOfMax])));
	}

	/**
	 * This method will return the threshold found by the Otsu method and do nothing else
	 * @param grayscale_image
	 * @return
	 */
	public static int getThresholdOtsu(ImagePlus grayscale_image){

		//get all the objects required: calibration, imageProcessor and histogram
		Calibration calibration = new Calibration(grayscale_image);		
		ImageProcessor imageProcessor = grayscale_image.getProcessor();
		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		//use that histogram to find a threshold
		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Otsu, histogram);

		return(threshold);
	}



	/**
	 * This function will give you the pixel values (rgb, HSB, or l for luma) for the specific ROI
	 * @param imp
	 * @param roi
	 * @param channelToGet
	 * @return
	 */
	public static Float[] getRoiPixels(ImagePlus imp, Roi roi, char channelToGet){

		if (roi!=null && !roi.isArea()) roi = null;
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor mask = roi!=null?roi.getMask():null;
		Rectangle r = roi!=null?roi.getBounds():new Rectangle(0,0,ip.getWidth(),ip.getHeight());


		ArrayList<Float> pixelList = new ArrayList<Float>();

		for (int y=0; y<r.height; y++) {
			for (int x=0; x<r.width; x++) {
				if (mask==null||mask.getPixel(x,y)!=0) {


					//get the rgb values
					int[] rgb = new int[3];
					ip.getPixel(x+r.x, y+r.y,rgb);

					//get the corresponding HSB values
					float[] HSB = java.awt.Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null); 


					switch(channelToGet){
					case('l'): //luminence/brightness
						pixelList.add(ip.getPixelValue(x+r.x, y+r.y));
					break;
					case('r'): //red
						pixelList.add((float)rgb[0]); ///RGB values are in the range of 0...255
					break;
					case('g'): //green
						pixelList.add((float)rgb[1]);
					break;
					case('b'): //blue
						pixelList.add((float)rgb[2]);
					break;
					case('H'): //Hue
						pixelList.add((float)HSB[0]*(float)255); //HSB values are from 0...1, convert that to 0...255
					break;
					case('S'): //Saturation
						pixelList.add((float)HSB[1]*(float)255);
					break;
					case('B'): //Brightness
						pixelList.add((float)HSB[2]*(float)255);
					break;
					}
				}
			}
		}
		//convert to array
		Float[] pixelarray = new Float[pixelList.size()];
		pixelarray = pixelList.toArray(pixelarray);

		return(pixelarray);

	}


	/**
	 * This function will convert the given picture into black and white
	 * using a fancy local thresholding algorithm, as described here:
	 * @see http://www.dentistry.bham.ac.uk/landinig/software/autothreshold/autothreshold.html
	 * @param 
	 */
	public static ImagePlus turnImageBW_Local_auto_mean(ImagePlus originalImage, int radius){
		ImagePlus imageToThreshold = originalImage.duplicate();

		//convert the image to grayscale
		ImageConverter converter = new ImageConverter(imageToThreshold);
		converter.convertToGray8();


		Auto_Local_Threshold.Mean(imageToThreshold, radius, 0, 0, true);
		imageToThreshold.setTitle(originalImage.getTitle());

		return(imageToThreshold);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using a fancy local thresholding algorithm, as described here:
	 * @see http://www.dentistry.bham.ac.uk/landinig/software/autothreshold/autothreshold.html
	 * @param 
	 */
	public static ImagePlus turnImageBW_Local_auto(ImagePlus originalImage, int radius, String method){
		ImagePlus imageToThreshold = originalImage.duplicate();

		//convert the image to grayscale
		ImageConverter converter = new ImageConverter(imageToThreshold);
		converter.convertToGray8();


		if(method=="Mean"){
			Auto_Local_Threshold.Mean(imageToThreshold, radius, 0, 0, false);
		}
		if(method=="Bernsen"){
			Auto_Local_Threshold.Bernsen(imageToThreshold, radius, 0, 0, false);
		}
		if(method=="Median"){
			Auto_Local_Threshold.Median(imageToThreshold, radius, 0, 0, false);
		}
		if(method=="Niblack"){
			Auto_Local_Threshold.Niblack(imageToThreshold, radius, 0, 0, false);
		}
		if(method=="MidGrey"){
			Auto_Local_Threshold.MidGrey(imageToThreshold, radius, 0, 0, false);
		}
		if(method=="Otsu"){
			Auto_Local_Threshold.Otsu(imageToThreshold, radius, 0, 0, false);
		}
		imageToThreshold.updateImage();
		imageToThreshold.setTitle(originalImage.getTitle());

		return(imageToThreshold);
	}

	/**
	 * This function will return a grayscale version of the given image, using the
	 * brightness channel of the HSB conversion of the image.
	 * The original input image is unchanged.
	 * @param originalImage
	 * @return
	 */
	public static ImagePlus getHSBgrayscaleImageBrightness(ImagePlus originalImage){

		ImagePlus originalImageCopy = originalImage.duplicate();
		ImageProcessor ip =  originalImageCopy.getProcessor();

		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the picture
		//ip.snapshot(); // override ColorProcessor bug in 1.32c
		int width = originalImageCopy.getWidth();
		int height = originalImageCopy.getHeight();
		int numPixels = width*height;

		//we need those arrays to save the different channels into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the HSB channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);

		//create a new image with the original title and the brightness HSB channel of the input image
		ByteProcessor bpBri = new ByteProcessor(width,height,bSource);
		ImagePlus grayscaleImage = new ImagePlus(originalImage.getTitle(), bpBri);
		originalImageCopy.flush();

		return(grayscaleImage);
	}


	/**
	 * This function will create a copy of the original image, and invert the colours on that copy.
	 * The original image should be flushed by the caller if not reused
	 * @param originalImage
	 * @param angle
	 * @return
	 */
	public static ImagePlus invertImage(ImagePlus originalImage){

		originalImage.deleteRoi();
		ImagePlus aDuplicate = originalImage.duplicate();//because we don't want to tamper with the original image

		aDuplicate.getProcessor().invert();

		aDuplicate.updateImage();

		aDuplicate.setTitle(originalImage.getTitle());

		return(aDuplicate);
	}

	/**
	 * This function will create a copy of the original image, and rotate that copy.
	 * The original image should be flushed by the caller if not reused
	 * @param originalImage
	 * @param angle
	 * @return
	 */
	public static ImagePlus rotateImage(ImagePlus originalImage, double angle) {
	
		if(IrisFrontend.singleColonyRun==true){
			ImagePlus rotatedImage = originalImage.duplicate();
			rotatedImage.setRoi(originalImage.getRoi());
			return(rotatedImage); // otherwise I'd have to delete the ROI
		}

		originalImage.deleteRoi();
		ImagePlus aDuplicate = originalImage.duplicate();//because we don't want to tamper with the original image

		aDuplicate.getProcessor().setBackgroundValue(0);

		aDuplicate.getProcessor().rotate(angle);

		aDuplicate.updateImage();

		aDuplicate.setTitle(originalImage.getTitle());

		return(aDuplicate);
	}


	/**
	 * This function will return the list of the points in the periphery of the given ROI
	 * @param tileImage
	 * @param roiOfInterest
	 * @return
	 */
	public static Point[] getRoiEdgePoints(ImagePlus tileImage, Roi roiOfInterest){
		//apply the ROI, get the mask
		ImageProcessor tileProcessor = tileImage.getProcessor();
		tileProcessor.setRoi(roiOfInterest);

		tileProcessor.setColor(Color.white);
		tileProcessor.setBackgroundValue(0);
		tileProcessor.fill(tileProcessor.getMask());


		//get the bounds of the mask, that's it, save it
		tileProcessor.findEdges();

		//		int minX = roiOfInterest.getBounds().x;
		//		int minY = roiOfInterest.getBounds().y;
		//		int maxX = roiOfInterest.getBounds().width  + minX;
		//		int maxY = roiOfInterest.getBounds().height + minY;

		int minX = 0;
		int minY = 0;
		int maxX = tileImage.getWidth();
		int maxY = tileImage.getHeight();


		//in this object, white pixels are the perimeter of the ROI, everything else is black
		ByteProcessor bwRoiPerimeter = (ByteProcessor) tileProcessor.convertToByte(true);


		ArrayList<Point> perimeterPoints = new ArrayList<Point>();

		for(int i=minX; i<=maxX; i++){
			for (int j=minY; j<=maxY; j++) {
				if(bwRoiPerimeter.getPixel(i, j)==255){
					perimeterPoints.add(new Point(i, j));
				}
			}
		}


		Point[] dummy = new Point[perimeterPoints.size()];
		return(perimeterPoints.toArray(dummy));
	}


	/**
	 * This function will get original picture, segment it into tiles.
	 * For each one, it will apply the colony ROI on it (except it it was empty -- add an empty ROI).
	 * Then, it will get the mask from the ROI and find it's bounds.
	 * At the end, for each original tile, we'll have 0/1 tiles, with 1s where the colony bounds are.
	 * @param croppedImage
	 * @param segmenterOutput
	 * @param colonyRoi
	 * @return
	 */
	public static ByteProcessor[][] getColonyBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmentationOutput, BasicTileReaderOutput [][] tileReaderOutputs){

		ByteProcessor[][] colonyBounds = new ByteProcessor[tileReaderOutputs.length][tileReaderOutputs[0].length];

		//for all rows
		for(int i=0;i<tileReaderOutputs.length; i++){
			//for all columns
			for (int j = 0; j<tileReaderOutputs[0].length; j++) {

				//get the tile
				croppedImage.setRoi(segmentationOutput.ROImatrix[i][j]);
				croppedImage.copy(false);
				ImagePlus tile = ImagePlus.getClipboard();


				//apply the ROI, get the mask
				ImageProcessor tileProcessor = tile.getProcessor();
				tileProcessor.setRoi(tileReaderOutputs[i][j].colonyROI);

				tileProcessor.setColor(Color.white);
				tileProcessor.setBackgroundValue(0);
				tileProcessor.fill(tileProcessor.getMask());


				//get the bounds of the mask, that's it, save it
				tileProcessor.findEdges();
				colonyBounds[i][j] = (ByteProcessor) tileProcessor.convertToByte(true);		
			}
		}

		croppedImage.deleteRoi();

		return(colonyBounds);
	}


	/**
	 * This function will use the ROI information in each TileReader to get the colony bounds on the picture, with
	 * offsets found in the segmenterOutput.  
	 * @param segmentedImage
	 * @param segmenterOutput
	 */
	public static void drawColonyBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmenterOutput, BasicTileReaderOutput [][] tileReaderOutputs){


		//first, get all the colony bounds into byte processors (one for each tile, having the exact tile size)
		ByteProcessor[][] colonyBounds = getColonyBounds(croppedImage, segmenterOutput, tileReaderOutputs);


		//paint those bounds on the original cropped image
		ImageProcessor bigPictureProcessor = croppedImage.getProcessor();
		//bigPictureProcessor.setColor(Color.black);
		bigPictureProcessor.setColor(Color.cyan);
		bigPictureProcessor.setLineWidth(1);


		//for all rows
		for(int i=0; i<tileReaderOutputs.length; i++){
			//for all columns
			for(int j=0; j<tileReaderOutputs[0].length; j++) {

				if(tileReaderOutputs[i][j].colonySize==0)
					continue; //don't go through the trouble for empty tiles

				//get tile offsets
				int tile_y_offset = segmenterOutput.ROImatrix[i][j].getBounds().y;
				int tile_x_offset = segmenterOutput.ROImatrix[i][j].getBounds().x;
				int tileWidth = segmenterOutput.ROImatrix[i][j].getBounds().width;
				int tileHeight = segmenterOutput.ROImatrix[i][j].getBounds().height;


				//for each pixel, if it is colony bounds, paint it on the big picture
				for(int x=0; x<tileWidth; x++){
					for(int y=0; y<tileHeight; y++){
						if(colonyBounds[i][j].getPixel(x, y)==255){ //it is a colony bounds pixel
							bigPictureProcessor.drawDot(x+tile_x_offset, y+tile_y_offset); //paint it on the big picture
						}
					}
				}

			}

		}






		//		croppedImage.updateImage();
		//		croppedImage.show();
		//		croppedImage.hide();




		/**
		 * TODO: if cropping the segmented (input) image doesn't give the tile we want (to set the ROI),
		 * then we can switch back to using the original (cropped color) picture and draw both the grid and the colony bounds on it.
		 * This will also give us the advantage of outputting a color grid
		 */
	}



	/**
	 * @param colonyCenter
	 * @param colonyRoiPerimeter
	 * @return
	 */
	public static double getMinimumPointDistance(Point colonyCenter,
			Point[] colonyRoiPerimeter) {

		if(colonyRoiPerimeter==null || colonyRoiPerimeter.length==0)
			return(0);

		ArrayList<Double> distances = new ArrayList<Double>();

		for(int i=0; i<colonyRoiPerimeter.length; i++){
			distances.add(Point2D.distance(
					colonyCenter.getX(), colonyCenter.getY(), 
					colonyRoiPerimeter[i].getX(), colonyRoiPerimeter[i].getY()));
		}

		Collections.sort(distances);


		return(distances.get(0));		
	}

	/**
	 * @param colonyCenter
	 * @param colonyRoiPerimeter
	 * @return
	 */
	public static double getMedianPointDistance(Point colonyCenter,
			Point[] colonyRoiPerimeter) {

		ArrayList<Double> distances = new ArrayList<Double>();

		for(int i=0; i<colonyRoiPerimeter.length; i++){
			distances.add(Point2D.distance(
					colonyCenter.getX(), colonyCenter.getY(), 
					colonyRoiPerimeter[i].getX(), colonyRoiPerimeter[i].getY()));
		}

		Collections.sort(distances);

		int medianLocation = (int) Math.floor((double)colonyRoiPerimeter.length/2.0);		
		return(distances.get(medianLocation));

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

		//0. if user has cropped the colony, no need to rotate
		if(IrisFrontend.singleColonyRun==true){
			return(0.0);
		}

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
	 * This function will convert the given picture into black and white
	 * using the Otsu method. This version will also return the threshold found.
	 * @param 
	 */
	public static ImagePlus turnImageBW(ImagePlus grayscaleImage, String method) {

		ImagePlus grayscaleImage_copy = grayscaleImage.duplicate();

		Calibration calibration = new Calibration(grayscaleImage_copy);

		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = grayscaleImage_copy.getProcessor();

		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		AutoThresholder at = new AutoThresholder();
		int threshold = 0; 

		if(method=="Huang"){
			threshold = at.getThreshold(Method.Huang, histogram);
		}
		if(method=="IJ_IsoData"){
			threshold = at.getThreshold(Method.IJ_IsoData, histogram);
		}
		if(method=="Intermodes"){
			threshold = at.getThreshold(Method.Intermodes, histogram);
		}
		if(method=="Li"){
			threshold = at.getThreshold(Method.Li, histogram);
		}
		if(method=="MaxEntropy"){
			threshold = at.getThreshold(Method.MaxEntropy, histogram);
		}
		if(method=="Mean"){
			threshold = at.getThreshold(Method.Mean, histogram);
		}
		if(method=="MinError"){
			threshold = at.getThreshold(Method.MinError, histogram);
		}
		if(method=="Minimum"){
			threshold = at.getThreshold(Method.Minimum, histogram);
		}
		if(method=="Moments"){
			threshold = at.getThreshold(Method.Moments, histogram);
		}
		if(method=="Otsu"){
			threshold = at.getThreshold(Method.Otsu, histogram);
		}
		if(method=="Percentile"){
			threshold = at.getThreshold(Method.Percentile, histogram);
		}
		if(method=="RenyiEntropy"){
			threshold = at.getThreshold(Method.RenyiEntropy, histogram);
		}
		if(method=="Shanbhag"){
			threshold = at.getThreshold(Method.Shanbhag, histogram);
		}
		if(method=="Yen"){
			threshold = at.getThreshold(Method.Yen, histogram);
		}



		imageProcessor.threshold(threshold);
		grayscaleImage_copy.updateImage();
		//BW_croppedImage.updateAndDraw();

		return(grayscaleImage_copy);
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
	 * using ImageProcessor's auto thresholding function, employing the Percentile algorithm. 
	 * This function does not return the threshold
	 * @param 
	 */
	public static void turnImageBW_Percentile_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Percentile, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Shanbhag algorithm. 
	 * This function does not return the threshold
	 * @param 
	 */
	public static void turnImageBW_Shanbhag_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Shanbhag, true, ImageProcessor.BLACK_AND_WHITE_LUT);
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

		Toolbox.show(BW_croppedImage, "before thresholding RenyiEntropy_auto");

		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.RenyiEntropy, true, ImageProcessor.BLACK_AND_WHITE_LUT);


	}


	/**
	 * This method will return the threshold found by the given method and do nothing else
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


	/**
	 * @param tileImage
	 * @param string
	 */
	public static void show(ImagePlus image, String title) {

		if(IrisFrontend.debug)
		{
			image.setTitle(title);
			image.show();
			image.hide();
		}

	}


	/**
	 * This is very very very badly done
	 * @param colonyRoi
	 * @return
	 * @deprecated
	 */
	public static int getSizeOfRoi(Roi colonyRoi) {

		ImagePlus image = colonyRoi.getImage().duplicate();
		image.setRoi(colonyRoi);
		ColorProcessor ip = (ColorProcessor) image.getChannelProcessor();
		ip.setRoi(colonyRoi);
		ip.fillOutside(colonyRoi);

		//ip.convertToByte(false);

		//ByteProcessor bp = (ByteProcessor) image.getpro

		byte[] maskPixels = (byte[]) ip.getChannel(1);



		int roiSize = 0;

		for(int i=1; i<maskPixels.length; i++){
			if(maskPixels[i]>0)
				roiSize++;
		}

		return(roiSize);


	}


	/**
	 * Designed to be a pretty generic function, it will save the tile at the i,j location to
	 * the desired filename
	 * @param i	the colony row
	 * @param j	the colony column
	 * @param colourCroppedImage	the cropped image, still in color
	 * @param segmentationOutput	the colony borders
	 * @param basicTileReaderOutputs	the colony's basic measurements
	 */
	public static void saveColonyPicture(int i, int j,
			ImagePlus colourCroppedImage,
			BasicImageSegmenterOutput segmentationOutput,
			BasicTileReaderOutput[][] basicTileReaderOutputs,
			String tileImageFilename) {

		//get just the tile part of the image
		ImagePlus tileImage = cropImage(colourCroppedImage, segmentationOutput.ROImatrix[i][j]);


		//construct the output filename for the tile.
		//this will hold the 

		savePicture(tileImage, tileImageFilename);

	}





	public static int getIndexOfBiggestParticle(ResultsTable resultsTable){
		//get the areas of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = getIndexOfMaximumElement(areas);

		return(indexOfMax);
	}


	/**
	 * Returns the area of the biggest particle in the results table
	 */
	public static int getBiggestParticleArea(ResultsTable resultsTable, int indexOfBiggestParticle) {

		//get the areas of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = indexOfBiggestParticle;//getIndexOfMaximumElement(areas);


		int largestParticleArea = Math.round(areas[indexOfBiggestParticle]);

		return(largestParticleArea);
	}


	/**
	 * Returns the area of the biggest particle in the results table.
	 * This function compensates for a mildly stringent thresholding algorithm (such as Otsu),
	 * in which it is known that the outer pixels of the colony are missing.
	 * By adding back pixels that equal the periphery in number, we compensate for those missing pixels.
	 * This in round colonies equals to the increase in diameter by 1. Warning: in colonies of highly abnormal
	 * shape (such as colonies that form a biofilm), this could add much more than just an outer layer of pixels,
	 * thus overcorrecting the stringency of the thresholding algorithm. 
	 */
	public static int getBiggestParticleAreaPlusPerimeter(ResultsTable resultsTable, int indexOfBiggestParticle) {

		//get the areas and perimeters of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));
		float perimeters[] = resultsTable.getColumn(resultsTable.getColumnIndex("Perim."));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = indexOfBiggestParticle;//getIndexOfMaximumElement(areas);

		//get the area and perimeter of the biggest particle
		int largestParticleArea = Math.round(areas[indexOfMax]);
		int largestParticlePerimeter = Math.round(perimeters[indexOfMax]);

		return(largestParticleArea+largestParticlePerimeter);
	}



	/**
	 * Returns the circularity of the biggest particle in the results table.
	 */
	public static float getBiggestParticleCircularity(ResultsTable resultsTable, int indexOfBiggestParticle) {

		//get the areas and perimeters of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = indexOfBiggestParticle;//getIndexOfMaximumElement(areas);

		return(circularities[indexOfMax]);
	}



	/**
	 * This method simply iterates through this array and finds the index
	 * of the largest element
	 */
	public static int getIndexOfMaximumElement(float[] areas) {
		int index = -1;
		float max = -Float.MAX_VALUE;

		for (int i = 0; i < areas.length; i++) {
			if(areas[i]>max){
				max = areas[i];
				index = i;
			}
		}

		return(index);
	}


	/**
	 * This function will read a CSV file where the first column is the filename (incl. path)
	 * the second column is the row, and the third row is the column
	 * @param csvFilename
	 * @return
	 */
	public static List<String[]> readCSV(String csvFilename) {

		try{
			CSVReader reader = new CSVReader(new FileReader(csvFilename));
			List<String[]> myEntries = reader.readAll();
			reader.close();
			return(myEntries);
		} catch (Exception e){
			return null;
		}
	}




	/**
	 * This function just checks for circularity. If it's under 0.20, then the tile gets rejected.
	 * @param resultsTable
	 * @param tileImage
	 * @return
	 */
	public static boolean isTileEmpty_simple2(ResultsTable resultsTable,
			ImagePlus tileImage) {

		if(IrisFrontend.singleColonyRun==true){
			return(false);
		}

		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));//get the areas of all the particles the particle analyzer has found
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));//get the circularities of all the particles


		//for the following, we only check the largest particle
		//which is the one who would be reported either way if we decide that this spot is not empty
		int indexOfMax = getIndexOfMaximumElement(areas);

		//check for the circularity of the largest particle
		//usually, colonies have roundnesses that start from 0.50 (faint colonies)
		//and reach 0.92 for normal colonies
		//for empty spots, this value is usually around 0.07, but there have been cases
		//where it reached 0.17.
		//Since this threshold would characterize a spot as empty, we will be more relaxed and set it at 0.20
		//everything below that, gets characterized as an empty spot
		if(circularities[indexOfMax]<0.20){
			return(true); //it's empty
		}

		return(false);
	}


	/**
	 * This function checks whether the given tile is empty,
	 * by summing up it's brightness and calculating the variance of these sums.
	 * Empty tiles have a very low variance, whereas tiles with colonies have high variances.
	 * @param tile
	 * @return
	 */
	public static boolean isTileEmpty_simple(ImagePlus tile, double varianceThreshold){
		if(IrisFrontend.singleColonyRun==true){
			return(false);
		}

		//sum up the pixel values (brightness) on the x axis
		double[] sumOfBrightnessXaxis = Toolbox.sumOfRows_double(tile);
		double variance = StdStats.varp(sumOfBrightnessXaxis);

		//System.out.println(variance);

		if(variance<varianceThreshold){
			return(true);
		}
		return(false);
	}


	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	public static double[] sumOfRows_double(ImagePlus croppedImage){
		int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows

		double[] sumOfRows = new double[dimensions[1]];

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
			sumOfRows[y] = sum;
		}

		return(sumOfRows);
	}




	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	public static ArrayList<Integer> sumOfRows(ImagePlus croppedImage){
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
	 * a very commonly used procedure across tile readers: particle analysis
	 * @param inputImage
	 * @param resultsTable
	 * @return
	 */
	public static RoiManager particleAnalysis_fillHoles(ImagePlus inputImage, ResultsTable resultsTable){
		//create the results table, where the results of the particle analysis will be shown
		//ResultsTable resultsTable = new ResultsTable();
		RoiManager roiManager = new RoiManager(true);

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
				Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
				resultsTable, 5, Integer.MAX_VALUE);

		ParticleAnalyzer.setRoiManager(roiManager);

		particleAnalyzer.analyze(inputImage);

		//3.1 check if the returned results table is empty
		if(resultsTable.getCounter()==0){
			return(null);
		}

		return(roiManager);
	}

	/**
	 * 
	 * @param BW_tile: a thresholded tile
	 * @return it's center, calculated via the Ultimate Erosion Points (via Eucledian Distance Maps)
	 */
	public static Point getParticleUltimateErosionPoint(ImagePlus BW_tile) {


		ImageConverter imageConverter = new ImageConverter(BW_tile);
		imageConverter.convertToGray8();

		//		turnImageBW_Local_auto(BW_tile, 8);
		turnImageBW_Otsu_auto(BW_tile); //almost works
		//turnImageBW_Shanbhag_auto(BW_tile);


		turnImageBW_Huang_auto(BW_tile);

		//		BW_tile.show();
		//		BW_tile.hide();

		//re-threshold the image for good luck


		//		BW_tile.show();
		//		BW_tile.hide();

		//fill holes -- could be why UEP fails
		ij.plugin.filter.Binary my_Binary = new ij.plugin.filter.Binary();
		my_Binary.setup("Fill Holes", BW_tile);
		my_Binary.run(BW_tile.getProcessor());

		//		BW_tile.show();
		//		BW_tile.hide();
		//		
		ij.plugin.filter.EDM my_EDM = new ij.plugin.filter.EDM();
		FloatProcessor EDM_processor = my_EDM.makeFloatEDM(BW_tile.getProcessor(), 0, true);
		ij.plugin.filter.MaximumFinder MaximumFinder = new ij.plugin.filter.MaximumFinder();
		double processorMax = EDM_processor.getMax();
		Polygon centerPoints = MaximumFinder.getMaxima(EDM_processor, processorMax-1, true);

		Point pointToReturn = new Point(centerPoints.xpoints[0], centerPoints.ypoints[0]);

		//if UEP failed miserably, I will erode myself and get the center
		if(pointToReturn.x==0){
			BW_tile.getProcessor().setAutoThreshold(Method.Otsu, true, ImageProcessor.BLACK_AND_WHITE_LUT);

			BW_tile.getProcessor().erode();
			BW_tile.getProcessor().erode();
			BW_tile.getProcessor().erode();
			BW_tile.getProcessor().erode();

			ResultsTable my_ResultsTable = new ResultsTable();
			RoiManager my_RoiManager = Toolbox.particleAnalysis_fillHoles(BW_tile, my_ResultsTable);

			int indexOfBiggestParticle = getIndexOfBiggestParticle(my_ResultsTable);
			pointToReturn = getBiggestParticleCenterOfMass(my_ResultsTable, indexOfBiggestParticle);
		}
		return(pointToReturn);
	}



	/**
	 * Calculates the median of the given values. For a sample with an odd
	 * number of elements the median is the mid-point value of the 
	 * sorted sample. For an even number of elements it is the mean of
	 * the two values on either side of the mid-point. 
	 * 
	 * Modified from org.jaitools.SampleStats.
	 * 
	 * 
	 * @param values sample values (need not be pre-sorted)
	 * @param valueToIgnore this value will be removed from the set before calculating the median
	 * @param ignoreValue whether or not to ignore the given value
	 * @return median value or Double.NaN if the sample is empty
	 * 
	 * 
	 */
	@SuppressWarnings("empty-statement")
	public static double median(Double[] values, Double valueToIgnore, boolean ignoreValue) {
		if (values == null) {
			return Double.NaN;
		}

		List<Double> nonNaNValues = org.jaitools.CollectionFactory.list();
		nonNaNValues.addAll(Arrays.asList(values));
		if (ignoreValue) {
			while (nonNaNValues.remove(valueToIgnore)) /* deliberately empty */ ;
		}

		if (nonNaNValues.isEmpty()) {
			return Double.NaN;
		} else if (nonNaNValues.size() == 1) {
			return nonNaNValues.get(0);
		} else if (nonNaNValues.size() == 2) {
			return (nonNaNValues.get(0) + nonNaNValues.get(1)) / 2;
		}

		Collections.sort(nonNaNValues);

		int midHi = nonNaNValues.size() / 2;
		int midLo = midHi - 1;
		boolean even = nonNaNValues.size() % 2 == 0;

		Double result = 0.0d;
		int k = 0;
		for (Double val : nonNaNValues) {
			if (k == midHi) {
				if (!even) {
					return val;
				} else {
					result += val;
					return result / 2;
				}
			} else if (even && k == midLo) {
				result += val;
			}
			k++ ;
		}

		return 0;  // to suppress compiler warning
	}



	/**
	 * Does what it says in the box.
	 * Will make a copy of your input image, so no worries there
	 * @param inputImage
	 * @return
	 */
	public static ImagePlus makeImageGrayscaleHSB(ImagePlus inputImage){
		ImagePlus grayscaleImage = inputImage.duplicate();

		ImageProcessor ip =  grayscaleImage.getProcessor();
		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the tile
		int width = grayscaleImage.getWidth();
		int height = grayscaleImage.getHeight();
		int numPixels = width*height;

		//we need those to save into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);

		ByteProcessor bpBri = new ByteProcessor(width,height,bSource);
		grayscaleImage = new ImagePlus("", bpBri);

		return(grayscaleImage);
	}

	/**
	 * This function will calculate the colony centers using a very basic technique,
	 * then will get the median of 
	 * @return
	 */
	public static ColorTileReaderInput [][] precalculateColonyCenters(ImagePlus inputCroppedImage, BasicImageSegmenterOutput segmentationOutput, ColorSettings settings){

		//if there's only one colony, then there's no way to pre-calculate centers.
		//basically return nulls in the place of the colony center. This will get it to calculate the center individually per-colony
		if(IrisFrontend.singleColonyRun==true){
			ColorTileReaderInput [][]  dummy_centeredColorTileReaderInput = new ColorTileReaderInput[1][1];
			dummy_centeredColorTileReaderInput[0][0] = new ColorTileReaderInput(inputCroppedImage, segmentationOutput.ROImatrix[0][0], settings); //notice last argument (center point) is missing
			return(dummy_centeredColorTileReaderInput);

		}	
		//initialize output
		BasicTileReaderOutput [][] basicTileReaderOutputsCenters = new BasicTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];


		//make input image grayscale (in case it wasn't)
		ImagePlus grayscaleCroppedImage = makeImageGrayscaleHSB(inputCroppedImage);


		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				basicTileReaderOutputsCenters[i][j] = BasicTileReader_Bsu.getColonyCenter(
						new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));

			}
		}

		//get the medians of all the rows and columns, ignore zeroes
		//for all rows
		ArrayList<Integer> rowYsMedians = new ArrayList<Integer>();
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			ArrayList<Double> rowYs = new ArrayList<Double>();
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				if(basicTileReaderOutputsCenters[i][j].colonyCenter!=null)
					rowYs.add((double) basicTileReaderOutputsCenters[i][j].colonyCenter.y);
			}
			Double[] simpleArray = new Double[ rowYs.size() ];
			int rowMedian = (int) Toolbox.median(rowYs.toArray(simpleArray), 0.0, true);
			rowYsMedians.add(rowMedian);
		}

		ArrayList<Integer> columnXsMedians = new ArrayList<Integer>();
		for(int j=0; j<settings.numberOfColumnsOfColonies; j++){
			ArrayList<Double> columnXs = new ArrayList<Double>();
			for (int i = 0; i < settings.numberOfRowsOfColonies; i++) {
				if(basicTileReaderOutputsCenters[i][j].colonyCenter!=null)
					columnXs.add((double) basicTileReaderOutputsCenters[i][j].colonyCenter.x);
			}
			Double[] simpleArray = new Double[ columnXs.size() ];
			int columnMedian = (int) Toolbox.median(columnXs.toArray(simpleArray), 0.0, true);
			columnXsMedians.add(columnMedian);
		}


		//save the pre-calculated colony centers in a matrix of input to basic tile reader
		//all the tile readers will get it from there
		//BasicTileReaderInput [][] centeredTileReaderInput = new BasicTileReaderInput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		ColorTileReaderInput [][] centeredColorTileReaderInput = new ColorTileReaderInput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				centeredColorTileReaderInput[i][j] = new ColorTileReaderInput(inputCroppedImage, segmentationOutput.ROImatrix[i][j], settings,
						new Point(columnXsMedians.get(j), rowYsMedians.get(i)));
			}
		}

		return(centeredColorTileReaderInput);

	}

	/**
	 * This function will use the ROI information in each TileReader to get the colony bounds on the picture, with
	 * offsets found in the segmenterOutput.  
	 * @param segmentedImage
	 * @param segmenterOutput
	 */
	public static void drawColonyRoundBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmenterOutput, 
			BasicTileReaderOutput [][] tileReaderOutputs){


		//first, get all the colony bounds into byte processors (one for each tile, having the exact tile size)
		ByteProcessor[][] colonyBounds = getColonyRoundBounds(croppedImage, segmenterOutput, tileReaderOutputs);


		//paint those bounds on the original cropped image
		ImageProcessor bigPictureProcessor = croppedImage.getProcessor();
		//bigPictureProcessor.setColor(Color.black);
		bigPictureProcessor.setColor(Color.green);
		bigPictureProcessor.setLineWidth(1);


		//for all rows
		for(int i=0; i<tileReaderOutputs.length; i++){
			//for all columns
			for(int j=0; j<tileReaderOutputs[0].length; j++) {

				//get tile offsets
				int tile_y_offset = segmenterOutput.ROImatrix[i][j].getBounds().y;
				int tile_x_offset = segmenterOutput.ROImatrix[i][j].getBounds().x;
				int tileWidth = segmenterOutput.ROImatrix[i][j].getBounds().width;
				int tileHeight = segmenterOutput.ROImatrix[i][j].getBounds().height;


				//for each pixel, if it is colony bounds, paint it on the big picture
				for(int x=0; x<tileWidth; x++){
					for(int y=0; y<tileHeight; y++){
						if(colonyBounds[i][j].getPixel(x, y)==255){ //it is a colony bounds pixel
							bigPictureProcessor.drawDot(x+tile_x_offset, y+tile_y_offset); //paint it on the big picture
						}
					}
				}

			}

		}
	}



	/**
	 * 
	 * @param croppedImage
	 * @param segmentationOutput
	 * @param tileReaderOutputs
	 * @return
	 */
	public static ByteProcessor[][] getColonyRoundBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmentationOutput, BasicTileReaderOutput [][] tileReaderOutputs){

		ByteProcessor[][] colonyBounds = new ByteProcessor[tileReaderOutputs.length][tileReaderOutputs[0].length];

		//for all rows
		for(int i=0;i<tileReaderOutputs.length; i++){
			//for all columns
			for (int j = 0; j<tileReaderOutputs[0].length; j++) {

				//get the tile
				croppedImage.setRoi(segmentationOutput.ROImatrix[i][j]);
				croppedImage.copy(false);
				ImagePlus tile = ImagePlus.getClipboard();


				//apply the ROI, get the mask
				ImageProcessor tileProcessor = tile.getProcessor();
				tileProcessor.setRoi(tileReaderOutputs[i][j].colonyROIround);

				tileProcessor.setColor(Color.white);
				tileProcessor.setBackgroundValue(0);
				tileProcessor.fill(tileProcessor.getMask());


				//get the bounds of the mask, that's it, save it
				tileProcessor.findEdges();
				colonyBounds[i][j] = (ByteProcessor) tileProcessor.convertToByte(true);		


			}
		}

		croppedImage.deleteRoi();

		return(colonyBounds);
	}


	/**
	 * This function will get the color of the 4 corner pixels and get their average color
	 * (after excluding 1 possible outlier)
	 * @param tileImage
	 * @return
	 */
	public static Color getBackgroundColor(ImagePlus tileImage){

		ColorProcessor colorProcessor = (ColorProcessor) tileImage.getProcessor();

		//get all corner colors
		Color[] cornerColors = new Color[4];
		cornerColors[0] = colorProcessor.getColor(0, 0); //topLeftCornerColor
		cornerColors[1] = colorProcessor.getColor(0, tileImage.getWidth()-1); //topRightCornerColor
		cornerColors[2] = colorProcessor.getColor(tileImage.getHeight()-1, 0); //bottomLeftCornerColor
		cornerColors[3] = colorProcessor.getColor(tileImage.getHeight()-1, tileImage.getWidth()); //bottomRightCornerColor 


		//check if a color is an outlier by calculating the distance to all other colors
		boolean[] outlierFlag = new boolean[4];
		for (int i = 0; i < outlierFlag.length; i++) {
			outlierFlag[i] = false;
		}

		double outlierThreshold = 0.5;

		for (int i = 0; i < cornerColors.length; i++) {
			double distanceSum = 0.0;
			for (int j = 0; j < cornerColors.length; j++) {
				if(i==j) continue;
				distanceSum += ColorUtil.colorDistance(cornerColors[i], cornerColors[j]);
			}

			if(distanceSum>outlierThreshold)
				outlierFlag[i] = true;
		}



		//HACK: just return the centroid for now (center of mass of 4 points in the RGB space)
		Color originalCentroidColor = ColorUtil.blend(
				ColorUtil.blend(cornerColors[0],cornerColors[1]), 
				ColorUtil.blend(cornerColors[2],cornerColors[3]));

		return(originalCentroidColor);


	}




	/***
	 * This function works great on colonies that have even brightness.
	 * While it works well in colonies that are just above the background,
	 * it doesn't work so well in colonies that have morphology etc.
	 * @param tileImage
	 * @return
	 */
	public static Roi getColonyRoiTranslucent(ImagePlus tileImage){
		//reset ROI
		//make a copy of the tileImage
		//make sure it's 8-bit grayscale
		//background subtraction (rolling ball)
		/**
		 * found here: http://stackoverflow.com/questions/33827715/imagej-subtract-background
		 * BackgroundSubtracter bs = new BackgroundSubtracter();
		 * bs.rollingBallBackground(img.getProcessor(), (double)rollballsize, false, false, false, true, false);
		 * img.getProcessor().resetMinAndMax();
		 * 
		 * reference:
		 * https://github.com/imagej/ImageJA/blob/master/src/main/java/ij/plugin/filter/BackgroundSubtracter.java
		 * public void rollingBallBackground(ImageProcessor ip, double radius, 
		 * boolean createBackground, boolean lightBackground, boolean useParaboloid, boolean doPresmooth, boolean correctCorners) 
		 * 
		 */
		//threshold using an entropy algorithm (e.g. Renyi Entropy)
		//particle analysis
		//return largest ROI -- caller will have to deal with size etc thresholds
		return(null);
	}




	///TODO: make function to replace certain color range if pixel falls within that

	/**
	 * This function will use the ROI information in each TileReader to get the colony bounds on the picture, with
	 * offsets found in the segmenterOutput.  
	 * @param segmentedImage
	 * @param segmenterOutput
	 */
	public static ImagePlus replaceColonyBackground(ImagePlus tileImage){

		ImagePlus returnImage = tileImage.duplicate();

		//get the background color given its corners
		Color backgroundColor = getBackgroundColor(returnImage);

		//paint those pixels black
		ImageProcessor tileImageProcessor = tileImage.getProcessor();
		tileImageProcessor.setColor(Color.black);
		tileImageProcessor.setLineWidth(1);

		//for each pixel, if it is colony bounds, paint it on the big picture
		for(int x=0; x<returnImage.getWidth(); x++){
			for(int y=0; y<returnImage.getHeight(); y++){

				//calculate distance of pixel color to known background color
				//
				if(true){//EDIT: THIS IS just to get the compilation right, should not be true here
					tileImageProcessor.drawDot(x, y); //paint it on the big picture
				}
			}
		}
		return(null);
	}


	/**
	 * This just returns the ROI area
	 * @param selectedRoi
	 * @return
	 */
	public static double getRoiArea(ImagePlus imp){
		
		if(imp.getRoi()==null){
			return(0);
		}
		
		ImageProcessor ip = imp.getProcessor(); 
		ip.setRoi(imp.getRoi()); 
		ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.MEAN, imp.getCalibration()); 
		double area2 = stats.area;
		return(area2);
	}
	
	
	





}
