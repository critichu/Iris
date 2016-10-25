/**
 * 
 */
package tileReaders;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.OpacityTileReaderOutput;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class OpacityTileReader {

	public static int diameter = 16;

	/**
	 * This tile reader gets the size of the colony in pixels, as well as the sum of it's brightness.
	 * 
	 * @param input
	 * @return
	 */
	public static OpacityTileReaderOutput processTile(OpacityTileReaderInput input){

		//0. create the output object
		OpacityTileReaderOutput output = new OpacityTileReaderOutput();

		//get a copy of this tile, before it gets thresholded
		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();
		grayscaleTileCopy.setRoi(input.tileImage.getRoi());
		//
		//--------------------------------------------------
		//
		//


		Roi colonyRoi;
		if(!IrisFrontend.settings.userDefinedRoi){


			//1. apply a threshold at the tile, using the Otsu algorithm
			Toolbox.turnImageBW_Otsu_auto(input.tileImage);
			//
			//--------------------------------------------------
			//
			//

			//2. perform particle analysis on the thresholded tile

			//create the results table, where the results of the particle analysis will be shown
			ResultsTable resultsTable = new ResultsTable();

			//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
			ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER, 
					Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
					resultsTable, 5, Integer.MAX_VALUE);


			RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
			ParticleAnalyzer.setRoiManager(manager);

			particleAnalyzer.analyze(input.tileImage); //it gets the image processor internally
			//
			//--------------------------------------------------
			//
			//

			//3.1 check if the returned results table is empty
			if(resultsTable.getCounter()==0){
				output.emptyResulsTable = true; // this is highly abnormal
				output.colonySize = 0;//return a colony size of zero

				input.cleanup(); //clear the tile image here, since we don't need it anymore
				grayscaleTileCopy.flush();

				return(output);
			}

			//3.2 check to see if the tile was empty. If so, return a colony size of zero
			//if(isTileEmpty(resultsTable, input.tileImage)){
			if(Toolbox.isTileEmpty_simple2(resultsTable, input.tileImage)){
				//if(OpacityTileReaderForHazyColonies_old.isTileEmpty_simple(input.tileImage)){
				output.emptyTile = true;
				output.colonySize = 0;//return a colony size of zero
				output.circularity = 0;
				output.opacity = 0;

				input.cleanup(); //clear the tile image here, since we don't need it anymore
				grayscaleTileCopy.flush();

				return(output);
			}


			//3.3 if there was a colony there, return the area of the biggest particle
			//this should also clear away contaminations, because normally the contamination
			//area will be smaller than the colony area, so the contamination will never be reported
			int indexOfBiggestParticle = getIndexOfBiggestParticle(resultsTable);
			//3.4 get the opacity of the colony
			//for this, we need the Roi (region of interest) that corresponds to the colony
			//so as to exclude the brightness of any contaminations
			colonyRoi = manager.getRoisAsArray()[indexOfBiggestParticle];

			output.colonySize = getBiggestParticleAreaPlusPerimeter(resultsTable, indexOfBiggestParticle);
			output.circularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);

			if(input.colonyCenter==null){ //if the center's preset for us, don't recalculate it
				output.colonyCenter = getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);	
			} else {
				output.colonyCenter = new Point(input.colonyCenter);
			}

			output.opacity = getBiggestParticleOpacity(grayscaleTileCopy, colonyRoi);
			output.max10percentOpacity = getLargestTenPercentOpacityMedian(grayscaleTileCopy, colonyRoi);
			output.centerAreaOpacity = getCenterAreaOpacity(grayscaleTileCopy, output.colonyCenter, diameter);
			output.colonyROI = colonyRoi;
		} 

		else { //user defined colony
			colonyRoi = (OvalRoi) input.tileImage.getRoi();
			output.colonySize = (int) Toolbox.getRoiArea(input.tileImage);
			output.circularity = 1; ///HACK: 1 means user-set ROI for now, need to change it to a proper circularity measurement
			output.opacity = totalColonyBrightnessMinusBackground(grayscaleTileCopy, colonyRoi);
			output.max10percentOpacity = getLargestTenPercentOpacityMedian(grayscaleTileCopy, colonyRoi);
			output.colonyCenter = new Point(colonyRoi.getBounds().width/2, colonyRoi.getBounds().height/2);
			output.centerAreaOpacity = getCenterAreaOpacity(grayscaleTileCopy, output.colonyCenter, diameter);
			output.colonyROI = colonyRoi;
		}

		if(output.opacity==0){
			//this cannot be zero, unless we have an empty tile, 
			//in which case this code shouldn't be reached
			output.errorGettingOpacity=true;
		}

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		grayscaleTileCopy.flush();

		output.centerROI = new OvalRoi(
				output.colonyCenter.x-diameter/2, 
				output.colonyCenter.y -diameter/2, 
				diameter, diameter);

		return(output);


		//TODO: still there is no way to filter out contaminations in case the tile is empty
		//this should be straight forward to do, since the center of mass (see ResultsTable) of the contamination
		//should be very far from the center of the tile

	}


	/**
	 * This function will take as input a tile plus it's thresholded version from a previous run of a similar method.
	 * It will perform particle analysis and measure the color in the thresholded area.
	 * @param input
	 * @return
	 */
	public static OpacityTileReaderOutput processDefinedColonyTile(OpacityTileReaderInput input){
		return(processDefinedColonyTile(input, false));
	}


	/**
	 * This function will take as input a tile plus it's thresholded version from a previous run of a similar method.
	 * It will perform particle analysis and measure the color in the thresholded area.
	 * @param input
	 * @return
	 */
	public static OpacityTileReaderOutput processDefinedColonyTile(OpacityTileReaderInput input, boolean useDarkColonies){

		//in case no-one's done this for us, get the ROI the traditional way
		if(input.colonyRoi==null && !IrisFrontend.settings.userDefinedRoi){
			return(processTile(input));
		} else if(IrisFrontend.settings.userDefinedRoi){
			input.colonyRoi=input.tileImage.getRoi();
		}


		//0. create the output object
		OpacityTileReaderOutput output = new OpacityTileReaderOutput();

		//get a copy of this tile, before it gets thresholded
		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();
		grayscaleTileCopy.setRoi(input.colonyRoi);
		//
		//--------------------------------------------------
		//
		//




		/// DONT check if its empty


		//3.3 if there was a colony there, return the area of the biggest particle
		//this should also clear away contaminations, because normally the contamination
		//area will be smaller than the colony area, so the contamination will never be reported

		output.colonySize = input.colonySize;
		output.circularity = 0;


		//3.4 get the opacity of the colony
		//for this, we need the Roi (region of interest) that corresponds to the colony
		//so as to exclude the brightness of any contaminations

		//		if(useDarkColonies==true){
		//			output.opacity = getBiggestParticleOpacity_darkColonies(grayscaleTileCopy, input.colonyRoi);
		//		}
		//		else{
		//			output.opacity = getBiggestParticleOpacity(grayscaleTileCopy, input.colonyRoi);
		//		}

		output.opacity = totalColonyBrightnessMinusBackground(grayscaleTileCopy, input.colonyRoi);


		output.max10percentOpacity = getLargestTenPercentOpacityMedian(grayscaleTileCopy, input.colonyRoi);


		output.colonyROI = input.colonyRoi;

		if(output.opacity==0){
			//this cannot be zero, unless we have an empty tile, 
			//in which case this code shouldn't be reached
			output.errorGettingOpacity=true;
		}

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		grayscaleTileCopy.flush();

		return(output);

	}



	/**
	 * @param grayscaleTileCopy
	 * @param colonyRoi
	 * @return
	 */
	private static int totalColonyBrightnessMinusBackground(
			ImagePlus tileImage, Roi colonyROI) {

		FloatProcessor backgroundPixels = (FloatProcessor) tileImage.getProcessor().convertToFloat().duplicate();
		backgroundPixels.setRoi(colonyROI);
		backgroundPixels.setValue(0);
		backgroundPixels.setBackgroundValue(0);

		backgroundPixels.fill(backgroundPixels.getMask());
//		(new ImagePlus("keep-background mask",backgroundPixels)).show();

		FloatProcessor foregroundPixels = (FloatProcessor) tileImage.getProcessor().convertToFloat().duplicate();
		foregroundPixels.setRoi(colonyROI);
		foregroundPixels.setValue(0);
		foregroundPixels.setBackgroundValue(0);

		foregroundPixels.fillOutside(colonyROI);
//		(new ImagePlus("keep-foreground mask",foregroundPixels)).show();




		int backgroundMedian = getBackgroundMedian(backgroundPixels);

		int sumColonyBrightness = sumPixelOverBackgroundBrightness(foregroundPixels, backgroundMedian);


		return(sumColonyBrightness);
	}


	/**
	 * @param ip
	 * @return
	 */
	private static int sumPixelOverBackgroundBrightness(FloatProcessor ip, int backgroundMedian) {

		float[] pixels = (float[]) ip.getPixels();

		ArrayList<Float> nonZeroPixels = new ArrayList<Float>();
		ArrayList<Float> zeroPixels = new ArrayList<Float>();
		ArrayList<Float> onePixels = new ArrayList<Float>();

		for(float thisPixel : pixels){
			if(thisPixel==255)
				onePixels.add(thisPixel);
			else if(thisPixel==0)
				zeroPixels.add(thisPixel);//don't deal with pixels inside the colony ROI
			else
				nonZeroPixels.add(thisPixel);
		}

		int sum = 0;
		for(Float thisPixel : nonZeroPixels){
			sum += Math.round(thisPixel)-backgroundMedian;
		}


		return(sum);
	}



	/**
	 * @param ip
	 * @return
	 */
	private static int getBackgroundMedian(FloatProcessor ip) {

		float[] pixels = (float[]) ip.getPixels();

		ArrayList<Float> nonZeroPixels = new ArrayList<Float>();
		ArrayList<Float> zeroPixels = new ArrayList<Float>();
		ArrayList<Float> onePixels = new ArrayList<Float>();

		for(float thisPixel : pixels){
			if(thisPixel==255)
				onePixels.add(thisPixel);
			else if(thisPixel==0)
				zeroPixels.add(thisPixel);//don't deal with pixels inside the colony ROI
			else
				nonZeroPixels.add(thisPixel);
		}


		return(Math.round(getMedian(nonZeroPixels.toArray(new Float[nonZeroPixels.size()]))));
	}

	/**
	 * just gets the median, nothing to see here
	 * @param inputArray
	 * @return
	 */
	private static float getMedian(Float[] inputArray){
		Arrays.sort(inputArray);
		double median;
		if (inputArray.length % 2 == 0)
			median = ((double)inputArray[inputArray.length/2] + (double)inputArray[inputArray.length/2 - 1])/2;
		else
			median = (double) inputArray[inputArray.length/2];

		return((float)median);
	}



	/**
	 * Returns the center of mass of the biggest particle in the results table
	 */
	private static Point getBiggestParticleCenterOfMass(ResultsTable resultsTable, int indexOfBiggestParticle) {

		//get the coordinates of all the particles the particle analyzer has found		
		float X_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("XM"));//get the X of the center of mass of all the particles
		float Y_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("YM"));//get the Y of the center of mass of all the particles


		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = indexOfBiggestParticle;//getIndexOfMaximumElement(areas);

		return( new Point(	Math.round(X_center_of_mass[indexOfMax]),
				Math.round(Y_center_of_mass[indexOfMax])));
	}




	/**
	 * @param grayscaleTileCopy
	 * @param colonyRoi
	 * @param i
	 * @return
	 */
	private static int getCenterAreaOpacity(ImagePlus grayscaleTile, Point colonyCenter, int diameter) {

		ImagePlus grayscaleTileCopy = grayscaleTile.duplicate();

		//1. find the background level, which is the threshold set by Otsu
		int background_level = getThresholdOtsu(grayscaleTileCopy);


		//3. get the colony center of mass, this will be the center of the circle
		//OvalRoi(xc-r/2,yc-r/2,r,r)
		Roi centerRoi = new OvalRoi(
				colonyCenter.x-diameter/2, 
				colonyCenter.y -diameter/2, 
				diameter, diameter);

		//4. set the center Roi and paint eveything outside it as black
		grayscaleTileCopy.setRoi(centerRoi);
		try {
			grayscaleTileCopy.getProcessor().fillOutside(centerRoi);
		} catch (Exception e) {
			return(0);
		}

		//4. get the pixel values of the image
		ByteProcessor processor = (ByteProcessor) grayscaleTileCopy.getProcessor();
		byte[] imageBytes = (byte[]) processor.getPixels();

		int size = imageBytes.length;

		int sumOfBrightness = 0;

		for(int i=0;i<size;i++){
			//since our pixelValue is unsigned, this is what we need to do to get it's actual (unsigned) value
			int pixelValue = imageBytes[i]&0xFF;

			//subtract the threshold and put the pixels in the sum
			//every pixel inside the colony should normally be above the threshold
			//but just in case, we'll just take 0 if a colony pixel turns out to be below the threshold
			//Also all the pixels outside the Roi would have a negative value after subtraction 
			//(they are already zero) because of the mask process

			sumOfBrightness += Math.max(0, pixelValue-background_level);
		}

		grayscaleTileCopy.flush();

		return (sumOfBrightness);
	}


	/**
	 * This method finds the Otsu threshold of the picture.
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 */
	private static double getLargestTenPercentOpacityMedian(ImagePlus grayscaleTile, Roi colonyRoi) {

		ImagePlus grayscaleTileCopy = grayscaleTile.duplicate();

		//1. find the background level
		FloatProcessor backgroundPixels = (FloatProcessor) grayscaleTile.getProcessor().convertToFloat().duplicate();
		backgroundPixels.setRoi(colonyRoi);
		backgroundPixels.setValue(0);
		backgroundPixels.setBackgroundValue(0);
		int background_level = getBackgroundMedian(backgroundPixels);

		//2. check sanity of the given Roi
		if(colonyRoi.getBounds().width<=0||colonyRoi.getBounds().height<=0){
			return(0);
		}

		//3. set the Roi and paint eveything outside it as black
		grayscaleTileCopy.setRoi(colonyRoi);
		try {
			grayscaleTileCopy.getProcessor().fillOutside(colonyRoi);
		} catch (Exception e) {
			return(0);
		}

		//4. get the pixel values of the image
		ByteProcessor processor = (ByteProcessor) grayscaleTileCopy.getProcessor();
		byte[] imageBytes = (byte[]) processor.getPixels();

		int size = imageBytes.length;

		//sort the bytes according to opacity
		Arrays.sort(imageBytes);
		ArrayList<Integer> pixelIntValues = new ArrayList<Integer>();

		for(int i=0;i<size;i++){
			int pixelValue = imageBytes[i]&0xFF;
			pixelIntValues.add(pixelValue);
		}

		Collections.sort(pixelIntValues, Collections.reverseOrder());
		//get the mean of the top 10%
		int sumOfBrightness = 0;
		int size_subset = (int)Math.ceil((double)size/(double)10);
		for(int i=0;i<size_subset;i++){
			sumOfBrightness += Math.max(0, pixelIntValues.get(i)-background_level);
		}
		//int top10percentMean = (int) Math.round((double)sumOfBrightness/(double)size_subset);
		double top10percentMean = (double)sumOfBrightness/(double)size_subset;

		grayscaleTileCopy.flush();
		return(top10percentMean);
	}



	/**
	 * This method finds the Otsu threshold of the picture.
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 * @deprecated, see sumPixelOverBackgroundBrightness
	 */
	private static int getBiggestParticleOpacity(ImagePlus grayscaleTile, Roi colonyRoi) {

		ImagePlus grayscaleTileCopy = grayscaleTile.duplicate();

		//1. find the background level, which is the median of the pixels not in the ROI
		FloatProcessor backgroundPixels = (FloatProcessor) grayscaleTile.getProcessor().convertToFloat().duplicate();
		backgroundPixels.setRoi(colonyRoi);
		backgroundPixels.setValue(0);
		backgroundPixels.setBackgroundValue(0);

		backgroundPixels.fill(backgroundPixels.getMask());
		int background_level = getBackgroundMedian(backgroundPixels);

		//2. check sanity of the given Roi
		if(colonyRoi.getBounds().width<=0||colonyRoi.getBounds().height<=0){
			return(0);
		}

		//3. set the Roi and paint eveything outside it as black
		grayscaleTileCopy.setRoi(colonyRoi);
		try {
			grayscaleTileCopy.getProcessor().fillOutside(colonyRoi);
		} catch (Exception e) {
			return(0);
		}

		//4. get the pixel values of the image
		ByteProcessor processor = (ByteProcessor) grayscaleTileCopy.getProcessor();
		byte[] imageBytes = (byte[]) processor.getPixels();

		int size = imageBytes.length;

		int sumOfBrightness = 0;

		for(int i=0;i<size;i++){
			//since our pixelValue is unsigned, this is what we need to do to get it's actual (unsigned) value
			int pixelValue = imageBytes[i]&0xFF;

			//subtract the threshold and put the pixels in the sum
			//every pixel inside the colony should normally be above the threshold
			//but just in case, we'll just take 0 if a colony pixel turns out to be below the threshold
			//Also all the pixels outside the Roi would have a negative value after subtraction 
			//(they are already zero) because of the mask process

			sumOfBrightness += Math.max(0, pixelValue-background_level);
		}

		grayscaleTileCopy.flush();

		return (sumOfBrightness);
	}




	/**
	 * @deprecated, see sumPixelOverBackgroundBrightness
	 * 
	 * This method finds the Otsu threshold of the picture.
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 */
	private static int getBiggestParticleOpacity_darkColonies(ImagePlus grayscaleTile, Roi colonyRoi) {

		ImagePlus grayscaleTileCopy = grayscaleTile.duplicate();

		//1. find the background level
		FloatProcessor backgroundPixels = (FloatProcessor) grayscaleTile.getProcessor().convertToFloat().duplicate();
		backgroundPixels.setRoi(colonyRoi);
		backgroundPixels.setValue(0);
		backgroundPixels.setBackgroundValue(0);
		int background_level = getBackgroundMedian(backgroundPixels);

		//2. check sanity of the given Roi
		if(colonyRoi.getBounds().width<=0||colonyRoi.getBounds().height<=0){
			return(0);
		}

		//3. set the Roi and paint eveything outside it as black
		grayscaleTileCopy.setRoi(colonyRoi);
		try {
			grayscaleTileCopy.getProcessor().fillOutside(colonyRoi);
		} catch (Exception e) {
			return(0);
		}

		//4. get the pixel values of the image
		ByteProcessor processor = (ByteProcessor) grayscaleTileCopy.getProcessor();
		byte[] imageBytes = (byte[]) processor.getPixels();

		int size = imageBytes.length;

		int sumOfBrightness = 0;

		for(int i=0;i<size;i++){
			int pixelValue = imageBytes[i]&0xFF;		

			//if a pixel value is zero, then it's either a really dark spot in the colony --improbable--
			//or its outside the colony (see fillOutside above) --much more probable--

			if(pixelValue==0){
				continue; //we don't want to count the background pixels
			}

			//sum up the pixel values, after subtracting the (Otsu defined background level).
			//if a colony is darker than the background, it will get a negative value here

			sumOfBrightness += pixelValue-background_level;
		}

		grayscaleTileCopy.flush();


		return (sumOfBrightness);
	}




	/**
	 * This method will return the threshold found by the Otsu method and do nothing else
	 * @param grayscale_image
	 * @return
	 */
	private static int getThresholdOtsu(ImagePlus grayscale_image){

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
	 * This function will convert the given picture into black and white
	 * using the image's histogram. This version will also return the threshold found.
	 * @param 
	 */
	private static int turnImageBW_Otsu(ImagePlus BW_croppedImage) {

		//get all the objects required: calibration, imageProcessor and histogram
		Calibration calibration = new Calibration(BW_croppedImage);		
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();
		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		//use that histogram to find a threshold
		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Otsu, histogram);

		imageProcessor.threshold(threshold);

		return(threshold);
	}



	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Otsu algorithm. 
	 * This function does not return the threshold
	 * @param 
	 */
	private static void turnImageBW_Otsu_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Otsu, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Huang algorithm
	 * This function does not return the threshold
	 * @param 
	 */
	private static void turnImageBW_Huang_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Huang, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Minimum algorithm
	 * This function does not return the threshold
	 * @param 
	 */
	private static void turnImageBW_Minimum_auto(ImagePlus BW_croppedImage) {
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
	private static void turnImageBW_MinError_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.MinError, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}


	/**
	 * This function uses the results table produced by the particle analyzer to
	 * find out whether this tile has a colony in it or it's empty.
	 * This function uses 3 sorts of filters, trying to pick up empty spots:
	 * 1. how many particles were found
	 * 2. the circularity of the biggest particle
	 * 3. the coordinates of the bounding rectangle of the biggest particle
	 * Returns true if the tile was empty, false if there is a colony in it.
	 */
	private static boolean isTileEmpty(ResultsTable resultsTable, ImagePlus tile) {

		//get the columns that we're interested in out of the results table
		int numberOfParticles = resultsTable.getCounter();
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));//get the areas of all the particles the particle analyzer has found
		float X_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BX"));//get the X of the bounding rectangles of all the particles
		float Y_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BY"));//get the Y of the bounding rectangles of all the particles
		float X_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("XM"));//get the X of the center of mass of all the particles
		float Y_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("YM"));//get the Y of the center of mass of all the particles
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));//get the circularities of all the particles
		float aspect_ratios[] = resultsTable.getColumn(resultsTable.getColumnIndex("AR"));//get the aspect ratios of all the particles

		/**
		 * Penalty is a number given to this tile if some of it's attributes (e.g. circularity of biggest particle)
		 * are borderline to being considered that of an empty tile.
		 * Initially, penalty starts at zero. Every time there is a borderline situation, the penalty gets increased.
		 * When the penalty exceeds a threshold, then this function returns that the tile is empty.
		 */
		int penalty = 0;


		//get the width and height of the tile
		int tileDimensions[] = tile.getDimensions();
		int tileWidth = tileDimensions[0];
		int tileHeight = tileDimensions[1];


		//check for the number of detected particles. 
		//Normally, if there is a colony there, the number of results should not be more than 20.
		//We set a limit of 40, since empty spots usually have more than 70 particles.
		if(numberOfParticles>40){
			return(true);//it's empty
		}

		//borderline to empty tile
		if(numberOfParticles>15){
			penalty++;
		}

		//for the following, we only check the largest particle
		//which is the one who would be reported either way if we decide that this spot is not empty
		int indexOfMax = getIndexOfMaximumElement(areas);


		//check for unusually high aspect ratio
		//Normal colonies would have an aspect ratio around 1, but contaminations have much higher aspect ratios (around 4)
		if(aspect_ratios[indexOfMax]>2){
			return(true); 
			//the tile is empty, the particle was just a contamination
			//TODO: notify the user that there has been a contamination in the plate in this spot
		}

		//borderline situation
		if(aspect_ratios[indexOfMax]>1.2){
			penalty++;
		}


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


		//If there is only one particle, then it is sure that this is not an empty spot
		//Unless it's aspect ratio is ridiculously high, which we already made sure it is not
		if(numberOfParticles==1){
			return(false);//it's not empty
		}

		//assess here the penalty function
		if(penalty>1){
			return(true); //it's empty
		}


		//UPDATE, last version that used the bounding box threshold was aaa9161, this was found to be erroneously detecting
		//colonies as empty, even though they were just close to the boundary
		//instead, it's combined with a circularity threshold 

		//check for the bounding rectangle of the largest colony
		//this consists of 4 values, X and Y of top-left corner of the bounding rectangle,
		//the width and the height of the rectangle.
		//In empty spots, I always got (0, 0) as the top-left corner of the bounding rectangle
		//in normal colony tiles I never got that. Only if the colony is growing out of bounds, you would get
		//one of the 2 (X or Y) to be zero, depending on whether it's growing out of left or top bound.
		//it would be extremely difficult for the colony to be overgrowing on both the left and top borders, because of the way the
		//image segmentation works
		//So, I'll only characterize a spot to be empty if both X and Y are zero.
		if(X_bounding_rectangles[indexOfMax]==0 && Y_bounding_rectangles[indexOfMax]==0){

			//it's growing near the border, but how round is it?
			//if it's circularity is above 0.5, then we conclude that this is a colony
			if(circularities[indexOfMax]>0.5){
				return(false); //it's a normal colony
			}

			return(true); //it's empty
		}


		return(false);//it's not empty
	}


	private static int getIndexOfBiggestParticle(ResultsTable resultsTable){
		//get the areas of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = getIndexOfMaximumElement(areas);

		return(indexOfMax);
	}


	/**
	 * Returns the area of the biggest particle in the results table
	 */
	private static int getBiggestParticleArea(ResultsTable resultsTable, int indexOfBiggestParticle) {

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
	private static int getBiggestParticleAreaPlusPerimeter(ResultsTable resultsTable, int indexOfBiggestParticle) {

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
	private static float getBiggestParticleCircularity(ResultsTable resultsTable, int indexOfBiggestParticle) {

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
	private static int getIndexOfMaximumElement(float[] areas) {
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

}
