/**
 * 
 */
package tileReaders;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.OpacityTileReaderOutput;
import utils.StdStats;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class OpacityTileReaderForBsuColonies {

	/**
	 * Below this variance threshold, the tile will be flagged as empty by the brightness sum algorithm
	 */
	public static double varianceThreshold = 5000;

	/**
	 * This tile reader gets the size of the colony in pixels, as well as the sum of it's brightness.
	 * This version is specialized in hazy colonies, so it gets a histogram on the X axis of the picture before it's thresholded.
	 * This histogram is then queried for it's variance large variance (over 1) means that there's a colony there, so the rest of the
	 * measures we have for picking up colonies there are overridden 
	 * 
	 * @param input
	 * @return
	 */
	public static OpacityTileReaderOutput processTile(OpacityTileReaderInput input){

		//0. create the output object
		OpacityTileReaderOutput output = new OpacityTileReaderOutput();

		//get a copy of this tile, before it gets thresholded
		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();
		//
		//--------------------------------------------------
		//
		//


		//1. apply a threshold at the tile, using the Otsu algorithm
		Toolbox.turnImageBW_Otsu_auto(input.tileImage);
		//		input.tileImage.show();
		//		input.tileImage.hide();

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

		//3.2 check to see if the tile was empty. If so, return a colony size of zero.
		//Change for fuzzy colonies: first check out the variance of it's sum of brightnesses



		//if variance is more than 1, then the brightness sum said there's a colony there
		//so there's has to be both variance less than 1 and other filters saying that there's no colony there
		//if(isTileEmpty(resultsTable, input.tileImage)){
		if(isTileEmpty_simple(input.tileImage)){
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
		output.colonySize = getBiggestParticleAreaPlusPerimeter(resultsTable, indexOfBiggestParticle);
		output.circularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);


		//3.4 get the opacity of the colony
		//for this, we need the Roi (region of interest) that corresponds to the colony
		//so as to exclude the brightness of any contaminations
		Roi colonyRoi = manager.getRoisAsArray()[indexOfBiggestParticle];

		output.opacity = getBiggestParticleOpacicity(grayscaleTileCopy, colonyRoi);
		output.colonyROI = colonyRoi;

		if(output.opacity==0){
			//this cannot be zero, unless we have an empty tile, 
			//in which case this code shouldn't be reached
			output.errorGettingOpacity=true;
		}

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		grayscaleTileCopy.flush();

		return(output);


		//TODO: still there is no way to filter out contaminations in case the tile is empty
		//this should be straight forward to do, since the center of mass (see ResultsTable) of the contamination
		//should be very far from the center of the tile

	}



	/**
	 * This method finds the Otsu threshold of the picture.
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 */
	private static int getBiggestParticleOpacicity(ImagePlus grayscaleTileCopy, Roi colonyRoi) {

		//1. find the background level, which is the threshold set by Otsu
		int background_level = getThresholdOtsu(grayscaleTileCopy);

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

			//subtract the threshold and put the pixels in the sum
			//every pixel inside the colony should normally be above the threshold
			//but just in case, we'll just take 0 if a colony pixel turns out to be below the threshold
			//Also all the pixels outside the Roi would have a negative value after subtraction 
			//(they are already zero) because of the mask process

			sumOfBrightness += Math.max(0, pixelValue-background_level);
		}


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
	 * This function checks whether the given tile is empty,
	 * by summing up it's brightness and calculating the variance of these sums.
	 * Empty tiles have a very low variance, whereas tiles with colonies have high variances.
	 * @param tile
	 * @return
	 */
	private static boolean isTileEmpty_simple(ImagePlus tile){
		//sum up the pixel values (brightness) on the x axis
		double[] sumOfBrightnessXaxis = sumOfRows(tile);
		double variance = StdStats.varp(sumOfBrightnessXaxis);


		if(variance<varianceThreshold){
			return(true);
		}
		return(false);
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
		//int largestParticlePerimeter = 0;//HACK TO SEE IF IT'S THE PERIMETER THAT MAKES THE DIFFERENCE= Math.round(perimeters[indexOfMax]);
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


	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	private static double[] sumOfRows(ImagePlus croppedImage){
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


}
