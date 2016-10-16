/**
 * 
 */
package tileReaders;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.util.ArrayList;

import tileReaderInputs.ColorTileReaderInput;
import tileReaderOutputs.MorphologyTileReaderOutput;
import utils.StdStats;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class MorphologyTileReaderStm {

	/**
	 * Below this variance threshold, the tile will be flagged as empty by the brightness sum algorithm
	 */
	public static double varianceThreshold = 1e6;

	/**
	 * This is the radius of the innermost circle scanning for morphology changes
	 */
	public static int initialRadius = 30; //this is an empirically defined good value for the Candida 96-plate readout

	/**
	 * This is the stepwise increase in circle radius
	 */
	public static int radiusStep = 5; //this is an empirically defined good value for the Candida 96-plate readout


	/**
	 * This defines the minimum brightness elevation required, 
	 * in order for it to be considered as a structural element of the colony
	 */
	public static int minimumBrightnessStep = 5;  //this is an empirically defined good value for the Candida 96-plate readout


	/**
	 * The tile measurement will stop after this amount of circles
	 */
	public static int maximumNumberOfCircles = 50;

	/**
	 * tile measurement will only measure this number of circles for the "fixed circles" output
	 */
	private static int circlesToMeasure = 8;

	/**
	 * tile measurement will ignore these number of circles from the outmost circle in the colony ROI
	 */
	private static int circlesToIgnore = 1;









	/**
	 * This tile reader is specialized in capturing the colony morphology. It returns a measure of how
	 * "wrinkly" a colony is. Flat colonies would get a low morphology score, whereas colonies featuring a complicated structure
	 * will be given a high score 
	 * 
	 * Since colonies featuring complex morphologies are typically not round, I will reuse here the code used for the
	 * hazy colony detection (vs empty tile).
	 * 
	 * @param input
	 * @return
	 */
	public static MorphologyTileReaderOutput processTile(ColorTileReaderInput input){

		//0. create the output object
		MorphologyTileReaderOutput output = new MorphologyTileReaderOutput();

		//get a copy of this tile, before it gets thresholded
		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();	
		//grayscaleTileCopy = Toolbox.makeImageGrayscaleHSB(grayscaleTileCopy);
		ImageConverter ic = new ImageConverter(grayscaleTileCopy);
		ic.convertToGray8();
		grayscaleTileCopy.updateImage();


		//
		//--------------------------------------------------
		//
		//
		//		grayscaleTileCopy.show();
		//		grayscaleTileCopy.hide();

		//1. apply a threshold at the tile, using the Otsu algorithm
		//turnImageBW_Otsu_auto(grayscaleTileCopy);
		//grayscaleTileCopy = Toolbox.turnImageBW_Local_auto(grayscaleTileCopy, 5, "Otsu");
		Toolbox.turnImageBW_Percentile_auto(grayscaleTileCopy);
		//grayscaleTileCopy = Toolbox.turnImageBW(grayscaleTileCopy, "Otsu");
		grayscaleTileCopy.updateImage();
		//		grayscaleTileCopy.show();
		//		grayscaleTileCopy.hide();

		//
		//--------------------------------------------------
		//
		//

		//2. perform particle analysis on the thresholded tile

		//create the results table, where the results of the particle analysis will be shown
		ResultsTable resultsTable = new ResultsTable();

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		//this version includes flood filling for holes; this is necessary given the weird morphology of candida colonies
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
				Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
				resultsTable, 5, Integer.MAX_VALUE);


		RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
		ParticleAnalyzer.setRoiManager(manager);

		particleAnalyzer.analyze(grayscaleTileCopy); //it gets the image processor internally
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

		//sum up the pixel values (brightness) on the x axis
		double[] sumOfBrightnessXaxis = sumOfRows(grayscaleTileCopy);
		double variance = StdStats.varp(sumOfBrightnessXaxis);


		//if variance is more than 1, then the brightness sum said there's a colony there
		//so there's has to be both variance less than 1 and other filters saying that there's no colony there
		if(variance < varianceThreshold) {// && isTileEmpty(resultsTable, input.tileImage)){
			output.emptyTile = true;
			output.colonySize = 0;//return a colony size of zero
			output.circularity = 0;
			output.morphologyScoreFixedNumberOfCircles = 0;
			output.normalizedMorphologyScore = 0;

			input.cleanup(); //clear the tile image here, since we don't need it anymore
			grayscaleTileCopy.flush();

			return(output);
		}


		//3.3 if there was a colony there, return the area of the biggest particle
		//this should also clear away contaminations, because normally the contamination
		//area will be smaller than the colony area, so the contamination will never be reported
		int indexOfBiggestParticle = getIndexOfBiggestParticle(resultsTable);
		output.colonySize = getBiggestParticleArea(resultsTable, indexOfBiggestParticle);
		output.circularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);

		//this will give us that the circles will not start in an awkward location, even in cases where
		//we might have oddly shaped colonies (e.g. budding shaped)
		if(input.colonyCenter==null){
			output.colonyCenter = Toolbox.getParticleUltimateErosionPoint(grayscaleTileCopy.duplicate());//getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);
		}
		else{
			output.colonyCenter = new Point(input.colonyCenter);
		}


		//3.4 get the morphology score of the colony
		//for this, we need the Roi (region of interest) that corresponds to the colony
		//so as to exclude the brightness of any contaminations
		Roi colonyRoi = manager.getRoisAsArray()[indexOfBiggestParticle];
		grayscaleTileCopy.setRoi(colonyRoi);

		ArrayList<Integer> elevationCounts = getBiggestParticleElevationCounts(grayscaleTileCopy, colonyRoi, output.colonyCenter);

		if(elevationCounts.size()==0){
			//check if we've hit empty space with the first circle already
			output.morphologyScoreFixedNumberOfCircles=0;
		}else {
			//get the sum of the elevation counts for all circles except the previous circle
			//that one is likely to get high elevation counts 
			//just because colony edges tend to be really bright compared to the background
			output.morphologyScoreFixedNumberOfCircles = sumElevationCounts_limited(elevationCounts, circlesToMeasure);
			output.morphologyScoreWholeColony = sumElevationCounts(elevationCounts, circlesToIgnore );
		}


		//if(elevationCounts.size()-1<=0)
		if(output.colonySize==0)
			output.normalizedMorphologyScore = 0; 
		else
			output.normalizedMorphologyScore = 1000* (double)output.morphologyScoreWholeColony / (double)output.colonySize;//(elevationCounts.size()-1);



		output.colonyROI = colonyRoi;
		output.colonyOpacity = getBiggestParticleOpacity(grayscaleTileCopy.duplicate(), output.colonyROI, Toolbox.getThreshold(grayscaleTileCopy, Method.Shanbhag));
		//output.wholeTileOpacity = output.colonyOpacity; --> this is only for colonies with agar invasion 

		//get minimum radius

		double minimumDistance = 0;

		//sometimes this fails
		try{
			Point[] colonyRoiPerimeter = Toolbox.getRoiEdgePoints(grayscaleTileCopy.duplicate(), output.colonyROI);
			minimumDistance = Toolbox.getMinimumPointDistance(output.colonyCenter, colonyRoiPerimeter);
		} catch(Exception e){
			minimumDistance = 0;
		}

		output.colonyROIround = new OvalRoi(
				output.colonyCenter.x-minimumDistance, 
				output.colonyCenter.y -minimumDistance, 
				minimumDistance*2, minimumDistance*2);


		output.colonyRoundSize = (int)Math.round(Math.PI*Math.pow(minimumDistance, 2));






		input.cleanup(); //clear the tile image here, since we don't need it anymore
		grayscaleTileCopy.flush();

		return(output);

	}


	/**
	 * This tile reader is specialized in capturing the colony morphology. It returns a measure of how
	 * "wrinkly" a colony is. Flat colonies would get a low morphology score, whereas colonies featuring a complicated structure
	 * will be given a high score 
	 * 
	 * Since colonies featuring complex morphologies are typically not round, I will reuse here the code used for the
	 * hazy colony detection (vs empty tile).
	 * 
	 * @param input
	 * @return
	 */
	public static MorphologyTileReaderOutput processTileWrinkly(ColorTileReaderInput input){


		//0. get a copy of this tile, before it gets processed
		ImagePlus inputTileImage = input.tileImage.duplicate();

		//0. first do a simple run using the old tile reader
		MorphologyTileReaderOutput outputSimple = MorphologyTileReaderStm.processTile(input);

		//return(outputSimple);

		input.tileImage = inputTileImage.duplicate();


		//1. check the output.
		//if the tile is empty, return immediately
		//if the tile holds a non-agar-grown colony, return the simple output (no hair)
		//see this note on how to distinguish the 2 types of colonies: https://www.evernote.com/l/ACigfyvs1zBAo5u0J1_9FA6--2TAwWiZg38
		//if the tile does indeed have a colony that's grown in agar, 
		//then we need to process it by running 2 different thresholding algorithms:
		//Percentile (for the in-agar growth) and Shangbhag for the colony itself
		//see also here: https://www.evernote.com/l/ACg4M6IXe29K2KbzhXiv1DyhyCRdctOIPFo

		if(outputSimple.emptyTile){
			return(outputSimple);
		}

		if(outputSimple.circularity>0.6){
			return(outputSimple);
		}

		//if we're still here it means that we need to re-process this colony
		try{
			//2. create the output object
			MorphologyTileReaderOutput output = new MorphologyTileReaderOutput();
			output.colonyHasInAgarGrowth = true;

			//
			//--------------------------------------------------
			//
			//

			//get a copy of this tile, before it gets thresholded	


			//A: get the entire colony first (including in-agar and over agar growth)

			//3A. apply a threshold at the tile, using the Percentile algorithm (that will get us the colony+hair) -- get a copy first
			ImagePlus grayscaleTileCopy = inputTileImage.duplicate();
			ImageConverter ic = new ImageConverter(grayscaleTileCopy);
			ic.convertToGray8();
			grayscaleTileCopy.updateImage();

			Toolbox.turnImageBW_Percentile_auto(grayscaleTileCopy);
			int inAgarBrightnessThreshold = Toolbox.getThreshold(grayscaleTileCopy, Method.Percentile);

			//4A. perform particle analysis on the thresholded tile
			ResultsTable resultsTable = new ResultsTable();
			ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
					Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
					resultsTable, 5, Integer.MAX_VALUE);
			RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
			ParticleAnalyzer.setRoiManager(manager);
			particleAnalyzer.analyze(grayscaleTileCopy);


			//5A. return the area of the biggest particle
			//this should also clear away contaminations, because normally the contamination
			//area will be smaller than the colony area, so the contamination will never be reported
			int indexOfBiggestParticle = getIndexOfBiggestParticle(resultsTable);
			output.inAgarSize = getBiggestParticleArea(resultsTable, indexOfBiggestParticle);
			output.inAgarCircularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
			output.inAgarROI = manager.getRoisAsArray()[indexOfBiggestParticle];
			output.inAgarOpacity = getBiggestParticleOpacity(grayscaleTileCopy, output.inAgarROI, inAgarBrightnessThreshold);



			//one last thing for the in-agar growth would be to get the total brightness in the tile
			//40 is the background of our pictures in the August 2015 experiment setup
			//but here I want to get the tile opacity without any subtraction, so I set the "background to subtract" to 0
			output.wholeTileOpacity = getWholeTileOpacity(grayscaleTileCopy, 0);


			//
			//--------------------------------------------------
			//
			//

			//B: then get the actual over-agar colony


			//3B. set the whole-colony ROI to the tileImage
			//	then apply a threshold at the ROI-- that should get us just the colony
			grayscaleTileCopy = inputTileImage.duplicate();
			ImageConverter ic2 = new ImageConverter(grayscaleTileCopy);
			ic2.convertToGray8();
			grayscaleTileCopy.updateImage();
			grayscaleTileCopy.setRoi(output.inAgarROI);
			Toolbox.turnImageBW_Minimum_auto(grayscaleTileCopy);
			int colonyBrightnessThreshold = Toolbox.getThreshold(grayscaleTileCopy, Method.Minimum);


			//4B. perform particle analysis on the thresholded tile
			resultsTable = new ResultsTable();
			particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
					Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
					resultsTable, 5, Integer.MAX_VALUE);
			manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
			ParticleAnalyzer.setRoiManager(manager);
			particleAnalyzer.analyze(grayscaleTileCopy);


			//5B. return the area of the biggest particle
			//this should also clear away contaminations, because normally the contamination
			//area will be smaller than the colony area, so the contamination will never be reported
			indexOfBiggestParticle = getIndexOfBiggestParticle(resultsTable);
			output.colonySize = getBiggestParticleArea(resultsTable, indexOfBiggestParticle);
			output.circularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
			output.colonyROI = manager.getRoisAsArray()[indexOfBiggestParticle];
			output.colonyOpacity = getBiggestParticleOpacity(grayscaleTileCopy, output.colonyROI, colonyBrightnessThreshold);


			//6B. get the morphology of the over-agar colony

			//this will give us that the circles will not start in an awkward location, even in cases where
			//we might have oddly shaped colonies (e.g. budding shaped)
			Point colonyCenter = getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);

			//for this, we need the Roi (region of interest) that corresponds to the colony
			//so as to exclude the brightness of any contaminations

			grayscaleTileCopy.setRoi(output.colonyROI);
			ArrayList<Integer> elevationCounts = getBiggestParticleElevationCounts(grayscaleTileCopy, output.colonyROI, colonyCenter);

			if(elevationCounts.size()==0){
				//check if we've hit empty space with the first circle already
				output.morphologyScoreFixedNumberOfCircles=0;
			}else {
				//get the sum of the elevation counts for all circles except the previous circle
				//that one is likely to get high elevation counts 
				//just because colony edges tend to be really bright compared to the background
				output.morphologyScoreFixedNumberOfCircles = sumElevationCounts_limited(elevationCounts, circlesToMeasure);
				output.morphologyScoreWholeColony = sumElevationCounts(elevationCounts, circlesToIgnore );
			}


			//if(elevationCounts.size()-1<=0)
			if(output.colonySize==0)
				output.normalizedMorphologyScore = 0; 
			else
				output.normalizedMorphologyScore = 1000* (double)output.morphologyScoreWholeColony / (double)output.colonySize;//(elevationCounts.size()-1);


			//before we go, I would like to calculate the difference in the opacity of the whole in-agar growth to the
			//one due to just the colony. For this, we need to get the colony opacity but using the in-agar growth's background.
			//see also note: 
			output.invasionRingOpacity = output.inAgarOpacity - getBiggestParticleOpacity(grayscaleTileCopy, output.colonyROI, inAgarBrightnessThreshold);
			output.invasionRingSize = output.inAgarSize - output.colonySize;


			input.cleanup(); //clear the tile image here, since we don't need it anymore
			grayscaleTileCopy.flush();

			return(output);

		}catch(Exception e){
			//if failed to threshold twice for whatever reason, fall back to the simple readout

			return(outputSimple);
		}
	}




	/**
	 * This tile reader is specialized in capturing the colony morphology. It returns a measure of how
	 * "wrinkly" a colony is. Flat colonies would get a low morphology score, whereas colonies featuring a complicated structure
	 * will be given a high score 
	 * 
	 * Since colonies featuring complex morphologies are typically not round, I will reuse here the code used for the
	 * hazy colony detection (vs empty tile).
	 * 
	 * @param input
	 * @return
	 */
	public static MorphologyTileReaderOutput processTileOverAgarOnly(ColorTileReaderInput input){

		MorphologyTileReaderOutput output = new MorphologyTileReaderOutput();

		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();
		ImageConverter ic2 = new ImageConverter(grayscaleTileCopy);
		ic2.convertToGray8();
		grayscaleTileCopy.updateImage();
		grayscaleTileCopy.deleteRoi();
		Toolbox.turnImageBW_Minimum_auto(grayscaleTileCopy);
		int colonyBrightnessThreshold = Toolbox.getThreshold(grayscaleTileCopy, Method.Minimum);


		if(!IrisFrontend.settings.userDefinedRoi){

			//4B. perform particle analysis on the thresholded tile
			ResultsTable resultsTable = new ResultsTable();
			ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
					Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
					resultsTable, 5, Integer.MAX_VALUE);
			RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
			ParticleAnalyzer.setRoiManager(manager);
			particleAnalyzer.analyze(grayscaleTileCopy);


			//5B. return the area of the biggest particle
			//this should also clear away contaminations, because normally the contamination
			//area will be smaller than the colony area, so the contamination will never be reported
			int indexOfBiggestParticle = getIndexOfBiggestParticle(resultsTable);
			if(indexOfBiggestParticle<0){
				return(new MorphologyTileReaderOutput());
			}
			output.colonySize = getBiggestParticleArea(resultsTable, indexOfBiggestParticle);
			output.circularity = getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
			output.colonyROI = manager.getRoisAsArray()[indexOfBiggestParticle];
			output.colonyOpacity = getBiggestParticleOpacity(grayscaleTileCopy, output.colonyROI, colonyBrightnessThreshold);

			
			//this is interesting, but I've seen this algorithm converge to returning the entire tile as the colony
			//I will check for this here and return accordingly
			if(output.colonySize == input.tileImage.getWidth()*input.tileImage.getHeight()){
				return(new MorphologyTileReaderOutput());
			}
			
			//this is a bug of the particle detection algorithm. 
			//It returns a particle of 0.791 circularity and area equal to the tile area
			//when there's nothing on the tile
			if(output.circularity>0.790 & output.circularity<0.792){

				input.cleanup(); //clear the tile image here, since we don't need it anymore
				grayscaleTileCopy.flush();

				output.colonySize = 0;
				output.circularity = 0;
				return(output);
			}

			//6B. get the morphology of the over-agar colony

			//this will give us that the circles will not start in an awkward location, even in cases where
			//we might have oddly shaped colonies (e.g. budding shaped)
			Point colonyCenter = getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);
			output.colonyCenter = colonyCenter;

		}
		else{ // user-defined colony ROI

			OvalRoi colonyRoi = (OvalRoi) input.tileImage.getRoi();
			output.colonySize = (int) Toolbox.getRoiArea(input.tileImage);
			output.circularity = 1; ///HACK: 1 means user-set ROI for now, need to change it to a proper circularity measurement
			output.colonyOpacity = getBiggestParticleOpacity(grayscaleTileCopy, colonyRoi, colonyBrightnessThreshold);
			output.colonyCenter = new Point(colonyRoi.getBounds().width/2, colonyRoi.getBounds().height/2);
			output.colonyROI = colonyRoi;
		}

		//for this, we need the Roi (region of interest) that corresponds to the colony
		//so as to exclude the brightness of any contaminations

		grayscaleTileCopy.setRoi(output.colonyROI);
		ArrayList<Integer> elevationCounts = getBiggestParticleElevationCounts(grayscaleTileCopy, output.colonyROI, output.colonyCenter);

		if(elevationCounts.size()==0){
			//check if we've hit empty space with the first circle already
			output.morphologyScoreFixedNumberOfCircles=0;
		}else {
			//get the sum of the elevation counts for all circles except the previous circle
			//that one is likely to get high elevation counts 
			//just because colony edges tend to be really bright compared to the background
			output.morphologyScoreFixedNumberOfCircles = sumElevationCounts_limited(elevationCounts, circlesToMeasure);
			output.morphologyScoreWholeColony = sumElevationCounts(elevationCounts, circlesToIgnore );
		}


		//if(elevationCounts.size()-1<=0)
		if(output.colonySize==0)
			output.normalizedMorphologyScore = 0; 
		else
			output.normalizedMorphologyScore = 1000* (double)output.morphologyScoreWholeColony / (double)output.colonySize;//(elevationCounts.size()-1);

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		grayscaleTileCopy.flush();

		return(output);


	}




	/**
	 * This function will calculate the morphology score for the colony by traversing concentric circles,
	 * starting in the center of the colony and moving upwards until the outmost circle is out of colony bounds.
	 *  
	 * 
	 * @param grayscale_image
	 * @param colonyRoi
	 * @return
	 */
	private static ArrayList<Integer> getBiggestParticleElevationCounts(ImagePlus grayscale_image, Roi colonyRoi, Point colonyCenter){

		//This variable will become true, once the current outmost circle has gone out of
		//colony bounds
		//boolean lastCircleOutOfBounds = false;

		int number_of_circles = 0;

		ArrayList<Integer> elevationCounts = new ArrayList<Integer>();


		//for every circle
		while(number_of_circles<maximumNumberOfCircles){

			int radius = initialRadius + number_of_circles * radiusStep;

			//first of all, we need to get the coordinates of the circle
			ArrayList<Point> circleCoordinates = getCircleCoordinates(colonyCenter, radius);
			ArrayList<Integer> meanPixelValues = new ArrayList<Integer>(); 

			//now, we need to traverse these circle coordinates to get the brightness elevations
			for (Point point : circleCoordinates) {
				//first, check if the point is out of bounds
				if(!colonyRoi.contains(point.x, point.y)){
					//grayscale_image.show();
					//grayscale_image.hide();

					//if it was found to be out of bounds,
					//return the sum of the elevation counts for all circles except the previous
					//the current one is never saved, but also the one before is likely to get high elevation counts 
					//just because colony edges tend to be really bright compared to the background
					//return(sumElevationCounts(elevationCounts, 1));
					return(elevationCounts);
				}
				meanPixelValues.add(getBrightnessAverage9pixels(grayscale_image, point));
			}

			elevationCounts.add(new Integer(countBrightnessChanges(meanPixelValues, minimumBrightnessStep, 2)));

			number_of_circles++;
		}

		//return the sum of the elevation counts for all circles except the last one
		//return(sumElevationCounts(elevationCounts, 1));
		return(elevationCounts);

	}


	/**
	 * This function just sums the elements of the given ArrayList, ignoring the last circlesToIgnore elements
	 * @param elevationCounts
	 * @param circlesToIgnore
	 * @return
	 */
	private static int sumElevationCounts(ArrayList<Integer> elevationCounts, int circlesToIgnore){
		int sum = 0;
		for (int i = 0; i < elevationCounts.size() - circlesToIgnore; i++) {
			sum += elevationCounts.get(i);
		}

		return(sum);
	}


	/**
	 * This function just sums the elements of the given ArrayList, ignoring the last circlesToIgnore elements
	 * @param elevationCounts
	 * @param circlesToIgnore
	 * @return
	 */
	private static int sumElevationCounts_limited(ArrayList<Integer> elevationCounts, int maxCirclesToCount){
		int sum = 0;
		int circlesToActuallyCount = Math.min(elevationCounts.size(), maxCirclesToCount);
		for (int i = 0; i < circlesToActuallyCount; i++) {
			sum += elevationCounts.get(i);
		}

		return(sum);
	}


	/**
	 * This function will get a sequence of measurements and count the times there's a difference
	 * greater or equal to threshold, when subtracting a measurement from it's previous
	 * @param series:	the pixel brightness values
	 * @param threshold:	above which brightness difference is it going to be picked up as a brightness change
	 * @param offset:	skip these number of pixels. Default value is 1	
	 * @return
	 */
	private static int countBrightnessChanges(ArrayList<Integer> series, int threshold, int offset){

		int changesOverThreshold = 0;

		for (int i = 0; i < series.size()-offset; i++) {
			int difference = Math.abs(series.get(i+offset) - series.get(i));
			if(difference>threshold){
				changesOverThreshold++;
			}
		}

		return(changesOverThreshold);
	}


	/**
	 * This function will return the average brightness of +/- 1 pixels around the requested point
	 * @param grayscale_image
	 * @param colonyRoi
	 * @param pixelToGet
	 * @return
	 */
	private static int getBrightnessAverage9pixels(ImagePlus grayscale_image, Point pixelToGet){

		//count the number of pixels actually retrieved and added to the sum		
		int pixelsAdded = 0;
		int sumOfPixelIntensity = 0;

		ByteProcessor grayscale_image_ip = (ByteProcessor) grayscale_image.getProcessor();  

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				int pixelIntensity = 0;
				try{
					pixelIntensity = grayscale_image_ip.getPixel(pixelToGet.x + x, pixelToGet.y + y);					
				}
				catch(Exception e){
					continue;
				}

				if(pixelIntensity==0){
					continue;//because getPixel() gives a zero if the requested pixel coordinates are out of bounds
				}

				//all went all right, we add up the pixel intensity and increase the pixelsAdded counter
				pixelsAdded++;
				sumOfPixelIntensity += pixelIntensity;				
			}
		}


		return (int) (Math.round( (double)sumOfPixelIntensity / (double)pixelsAdded ));
	}


	/**
	 * Since we have a rasterized picture with relatively small resolution,
	 * we will use Bresenham's circle algorithm, an adaptation of Bresenham's line algorithm
	 * to draw a rasterized circle.
	 * Note that this implementation follows Jean-Yves Tinevez's <jeanyves.tinevez@gmail.com> 
	 * Matlab implementation. Hence it provides us with a list of points that are sorted, as in
	 * a circle that starts at 6 o'clock and goes counterclockwise.
	 * This happens because the coordinate system this function was made for is
	 * 	+
	 * - +
	 * 	-
	 * whereas what happens for image coordinate systems is
	 * 0	+
	 * +
	 * a.k.a
	 * 	-
	 * - +
	 * 	+
	 * @param center
	 * @param radius
	 * @return the circle point coordinates  
	 * @see http://en.wikipedia.org/wiki/Midpoint_circle_algorithm
	 */
	public static ArrayList<Point> getCircleCoordinates(Point center, int radius){
		// Compute first the number of points
		int octant_size = (int) Math.floor((Math.sqrt(2)*(radius-1)+4)/2);
		int n_points = 8 * octant_size;


		int x0 = center.x;
		int y0 = center.y;

		// Iterate a second time, and this time retrieve coordinates.
		// We "zig-zag" through indices, so that we reconstruct a continuous
		// set of of x,y coordinates, starting from the top of the circle.

		//Matlab uses the 1..n array notation instead of the 0...n-1 notation that Java uses
		//an easy way to go from one to the other is make a bigger array and then "shift" all the values
		//before moving them back into Java context
		int[] xc = new int[n_points+1];
		int[] yc = new int[n_points+1];

		int x = 0;
		int y = radius;
		int f = 1 - radius;
		int dx = 1;
		int dy = - 2 * radius;

		// Store

		// 1 octant
		xc[1] = x0 + x;
		yc[1] = y0 + y;

		// 2nd octant 
		xc[8 * octant_size] = x0 - x;
		yc[8 * octant_size] = y0 + y;

		// 3rd octant 
		xc[4 * octant_size] = x0 + x;
		yc[4 * octant_size] = y0 - y;

		// 4th octant 
		xc[4 * octant_size + 1] = x0 - x;
		yc[4 * octant_size + 1] = y0 - y;

		// 5th octant 
		xc[2 * octant_size] = x0 + y;
		yc[2 * octant_size] = y0 + x;

		// 6th octant 
		xc[6 * octant_size + 1] = x0 - y;
		yc[6 * octant_size + 1] = y0 + x;

		// 7th octant 
		xc[2 * octant_size + 1] = x0 + y;
		yc[2 * octant_size + 1] = y0 - x;

		// 8th octant 
		xc[6 * octant_size] = x0 - y;
		yc[6 * octant_size] = y0 - x;


		for(int i=2; i<=n_points/8; i++){

			// We update x & y
			if (f > 0){
				y = y - 1;
				dy = dy + 2;
				f = f + dy;
			}
			x = x + 1;
			dx = dx + 2;
			f = f + dx;

			// 1 octant
			xc[i] = x0 + x;
			yc[i] = y0 + y;

			// 2nd octant
			xc[8 * octant_size - i + 1] = x0 - x;
			yc[8 * octant_size - i + 1] = y0 + y;

			// 3rd octant
			xc[4 * octant_size - i + 1] = x0 + x;
			yc[4 * octant_size - i + 1] = y0 - y;

			// 4th octant
			xc[4 * octant_size + i] = x0 - x;
			yc[4 * octant_size + i] = y0 - y;

			// 5th octant
			xc[2 * octant_size - i + 1] = x0 + y;
			yc[2 * octant_size - i + 1] = y0 + x;

			// 6th octant
			xc[6 * octant_size + i] = x0 - y;
			yc[6 * octant_size + i] = y0 + x;

			// 7th octant
			xc[2 * octant_size + i] = x0 + y;
			yc[2 * octant_size + i] = y0 - x;

			// 8th octant
			xc[6 * octant_size - i + 1] = x0 - y;
			yc[6 * octant_size - i + 1] = y0 - x;

		}

		//ready, now prepare the output
		ArrayList<Point> pointsToReturn = new ArrayList<Point>();

		for(int i=1; i<n_points+1; i++){
			pointsToReturn.add(new Point(xc[i], yc[i]));
		}


		return(pointsToReturn);


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

		float[] areas;
		try{
			areas = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));
		} catch(IllegalArgumentException e){
			return (-1);
		}

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




	/**
	 * This function calculates the sum of pixel brighness per colony, subtracting the given background value
	 * (typically reported by the thresholding algorithm used to detect the colony) 
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 */
	private static int getBiggestParticleOpacity(ImagePlus grayscaleTileCopy, Roi colonyRoi, int background_level) {

		//1. find the background level, which is the threshold set by Otsu
		//int background_level = getThresholdOtsu(grayscaleTileCopy);

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


		return (sumOfBrightness);
	}




	/**
	 * This function calculates the sum of pixel brighness per tile, subtracting the given background value
	 * (typically reported by the thresholding algorithm used to detect the colony) 
	 * Then, it sums the brightness (0 to 255) value of each pixel in the image, as long as it's inside the colony.
	 * The background level is determined using the Otsu algorithm and subtracted from each pixel before the sum is calculated.
	 * @param grayscaleTileCopy
	 * @return
	 */
	private static int getWholeTileOpacity(ImagePlus grayscaleTileCopy, int background_level) {


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


		return (sumOfBrightness);
	}




}
