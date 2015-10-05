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
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imageSegmenterOutput.BasicImageSegmenterOutput;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import tileReaderOutputs.BasicTileReaderOutput;

import com.opencsv.CSVReader;

/**
 * This class holds methods that are not profile-specific
 * This should reduce code duplication, and make bug fixing easier.
 * @author George Kritikos
 *
 */
public class Toolbox {




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
	public static ImagePlus turnImageBW_Local_auto(ImagePlus originalImage, int radius){
		ImagePlus imageToThreshold = originalImage.duplicate();
		
		//convert the image to grayscale
		ImageConverter converter = new ImageConverter(imageToThreshold);
		converter.convertToGray8();
		
		
		Auto_Local_Threshold.Mean(imageToThreshold, radius, 0, 0, true);
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

		originalImage.deleteRoi();
		ImagePlus aDuplicate = originalImage.duplicate();//because we don't want to tamper with the original image

		aDuplicate.getProcessor().setBackgroundValue(0);

		aDuplicate.getProcessor().rotate(angle);

		aDuplicate.updateImage();

		aDuplicate.setTitle(originalImage.getTitle());

		return(aDuplicate);
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

				//				ImageConverter imageConverter = new ImageConverter(tile);
				//				imageConverter.convertToGray8();

				//				tile.updateImage();
				//								tile.show();
				//								tile.hide();

				//apply the ROI, get the mask
				ImageProcessor tileProcessor = tile.getProcessor();
				tileProcessor.setRoi(tileReaderOutputs[i][j].colonyROI);

				tileProcessor.setColor(Color.white);
				tileProcessor.setBackgroundValue(0);
				tileProcessor.fill(tileProcessor.getMask());
				//tileProcessor.fill(tileReaderOutputs[i][j].colonyROI.getMask());


				//				tile.updateImage();
				//								tile.show();
				//								tile.hide();


				//get the bounds of the mask, that's it, save it
				tileProcessor.findEdges();
				colonyBounds[i][j] = (ByteProcessor) tileProcessor.convertToByte(true);		


				//				tile.setProcessor(colonyBounds[i][j]);
				//								tile.updateImage();
				//								tile.show();
				//								tile.hide();


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
					continue; //don't go through the trouble for emtpy tiles

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




}
