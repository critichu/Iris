/**
 * 
 */
package tileReaders;

import fiji.threshold.Auto_Local_Threshold;
import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Hough_Circles;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Point;

import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import utils.StdStats;
import utils.Toolbox;

/**
 * This class provides methods to size a colony in the CoCo background.
 * Hence, it uses a different (variance-
 * @author George Kritikos
 *
 */
public class BasicTileReaderHSB {

	/**
	 * Below this variance threshold, the tile will be flagged as empty by the brightness sum algorithm
	 */
	public static double varianceThreshold = 2e4;

	/**
	 * This simple tile readout gets the area (in pixels) of the colony in the tile.
	 * The HSB version uses the HSB transformation to get each pixel's brightness.
	 * Moreover, this version implements a simpler threshold to classify empty tiles from
	 * tiles with colonies.
	 * 
	 * @param input
	 * @return
	 */
	public static BasicTileReaderOutput processTile(BasicTileReaderInput input){

		//-1. make a copy of the input tile, in case we need to pass it to HoughCircleFinder
		ImagePlus tileCopy = input.tileImage.duplicate();
		tileCopy.setRoi(input.tileImage.getRoi());

		//0. create the output object
		BasicTileReaderOutput output = new BasicTileReaderOutput();


		if(!IrisFrontend.settings.userDefinedRoi){


			//1. apply a threshold at the tile, using a local thresholding algorithm
			turnImageBW_Huang_auto(input.tileImage);
			//turnImageBW_Local_auto(input.tileImage); //no need, image is already local thresholded



			//2. perform particle analysis on the thresholded tile

			//create the results table, where the results of the particle analysis will be shown
			ResultsTable resultsTable = new ResultsTable();
			RoiManager roiManager = new RoiManager(true);

			//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
			ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.INCLUDE_HOLES+ParticleAnalyzer.ADD_TO_MANAGER, 
					Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
					resultsTable, 5, Integer.MAX_VALUE);

			ParticleAnalyzer.setRoiManager(roiManager);
			particleAnalyzer.analyze(input.tileImage); //it gets the image processor internally

			Roi[] rois = roiManager.getRoisAsArray();



			//3.1 check if the returned results table is empty
			if(resultsTable.getCounter()==0){
				output.emptyResulsTable = true; // this is highly abnormal
				output.colonySize = 0;//return a colony size of zero

				input.cleanup(); //clear the tile image here, since we don't need it anymore

				return(output);
			}

			//3.2 check to see if the tile was empty. If so, return a colony size of zero
			//if variance is more than 1, then the brightness sum said there's a colony there
			//so there's has to be both variance less than 1 and other filters saying that there's no colony there

			boolean emptyTile = isTileEmpty(resultsTable, input.tileImage);
			boolean emptyTile_simple = isTileEmpty_simple(input.tileImage);


			if(emptyTile_simple) { //even with the variance criteria, there was no colony found here, quitting 

				output.emptyTile = true;
				output.colonySize = 0;//return a colony size of zero

				input.cleanup(); //clear the tile image here, since we don't need it anymore

				return(output);
			}

			//		if(isTileEmpty_simple2(resultsTable, input.tileImage)){
			//			output.emptyTile = true;
			//			output.colonySize = 0;//return a colony size of zero
			//
			//			input.cleanup(); //clear the tile image here, since we don't need it anymore
			//
			//			return(output);
			//		}


			if(!emptyTile){ //there's a colony in the tile which was detected in the traditional way

				//3.2 if there was a colony there, return the area of the biggest particle
				//this should also clear away contaminations, because normally the contamination
				//area will be smaller than the colony area, so the contamination will never be reported
				int indexOfBiggestParticle = Toolbox.getIndexOfBiggestParticle(resultsTable);
				output.colonySize = Toolbox.getBiggestParticleAreaPlusPerimeter(resultsTable, indexOfBiggestParticle);
				output.circularity = Toolbox.getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
				output.colonyCenter = Toolbox.getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);
				output.colonyROI = rois[indexOfBiggestParticle];

				input.cleanup(); //clear the tile image here, since we don't need it anymore

				
				//I will double-check here if this tile reader returns the whole tile
				if(output.colonySize == input.tileImage.getWidth()*input.tileImage.getHeight()){
					return(new BasicTileReaderOutput());
				}
				
				return(output);//returns the biggest result
			}
			else{ //there's a colony here, but we need to employ Hough to get it right

				//but only if the user wishes to do so
				input.settings.useHoughCircles=false; //it's better to fall back to KC's method as a backup
				if(input.settings.useHoughCircles){

					input.tileImage = tileCopy;
					//input.tileImage.setRoi(rois[getIndexOfBiggestParticle(resultsTable)], false);

					Hough_Circles.centerX = rois[Toolbox.getIndexOfBiggestParticle(resultsTable)].getBounds().getCenterX();
					Hough_Circles.centerY = rois[Toolbox.getIndexOfBiggestParticle(resultsTable)].getBounds().getCenterY();
					return(MyHoughCircleFinder.processTile(input));
				}
				else{
					//give up and return that the tile is empty
					output.emptyTile = true;
					output.colonySize = 0;//return a colony size of zero

					input.cleanup(); //clear the tile image here, since we don't need it anymore

					return(output);				
				}
			}
		} 
		else
		{ // this is a user-defined ROI, we have to use this one 
			
			OvalRoi colonyRoi = (OvalRoi) input.tileImage.getRoi();
			output.colonySize = (int) Toolbox.getRoiArea(input.tileImage);
			output.circularity = 1; ///HACK: 1 means user-set ROI for now, need to change it to a proper circularity measurement
			output.colonyCenter = new Point(colonyRoi.getBounds().width/2, colonyRoi.getBounds().height/2);
			output.colonyROI = colonyRoi;
			return(output);
		}

		//TODO: still there is no way to filter out contaminations in case the tile is empty
		//this should be straight forward to do, since the center of mass (see ResultsTable) of the contamination
		//should be very far from the center of the tile

	}


	/**
	 * This function just checks for circularity. If it's under 0.20, then the tile gets rejected.
	 * @param resultsTable
	 * @param tileImage
	 * @return
	 */
	private static boolean isTileEmpty_simple2(ResultsTable resultsTable,
			ImagePlus tileImage) {

		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));//get the areas of all the particles the particle analyzer has found
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));//get the circularities of all the particles


		//for the following, we only check the largest particle
		//which is the one who would be reported either way if we decide that this spot is not empty
		int indexOfMax = Toolbox.getIndexOfMaximumElement(areas);

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
	private static boolean isTileEmpty_simple(ImagePlus tile){
		//sum up the pixel values (brightness) on the x axis
		double[] sumOfBrightnessXaxis = sumOfRows(tile);
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
	 * using a fancy local thresholding algorithm, as described here:
	 * @see http://www.dentistry.bham.ac.uk/landinig/software/autothreshold/autothreshold.html
	 * @param 
	 */
	private static void turnImageBW_Local_auto(ImagePlus BW_croppedImage){
		//use the mean algorithm with default values
		//just use smaller radius (8 instead of default 15)
		Auto_Local_Threshold.Mean(BW_croppedImage, 8, 0, 0, true);
		//		BW_croppedImage.updateAndDraw();
		//		BW_croppedImage.show();
		//		BW_croppedImage.hide();
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
	private static boolean isTileEmpty_HSB(ResultsTable resultsTable, ImagePlus tile) {

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
		int indexOfMax = Toolbox.getIndexOfMaximumElement(areas);


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

		int threshold = Toolbox.getThreshold(tile, Method.Otsu);

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

		//check low the thresholding algorithm had to get in order to detect something
		//most tiles are around 128-131, under 100 it's certainly empty.
		//under 120 it starts getting kind of fishy..
		if(threshold<100){
			return(true);//it's empty
		}

		if(threshold<120){
			penalty++;
		}



		//for the following, we only check the largest particle
		//which is the one who would be reported either way if we decide that this spot is not empty
		int indexOfMax = Toolbox.getIndexOfMaximumElement(areas);


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
		if(circularities[indexOfMax]<0.30){
			return(true); //it's empty
		}

		//this is a bug of the particle detection algorithm. 
		//It returns a particle of 0.791 circularity and area equal to the tile area
		//when there's nothing on the tile 
		if(circularities[indexOfMax]<0.792 & circularities[indexOfMax]>0.790){
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

}
