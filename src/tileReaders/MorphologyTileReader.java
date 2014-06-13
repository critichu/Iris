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

import java.awt.Point;
import java.util.ArrayList;

import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.MorphologyTileReaderOutput;
import utils.StdStats;

/**
 * @author George Kritikos
 *
 */
public class MorphologyTileReader {

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
	public static MorphologyTileReaderOutput processTile(OpacityTileReaderInput input){

		//0. create the output object
		MorphologyTileReaderOutput output = new MorphologyTileReaderOutput();

		//get a copy of this tile, before it gets thresholded
		ImagePlus grayscaleTileCopy = input.tileImage.duplicate();
		//
		//--------------------------------------------------
		//
		//


		//1. apply a threshold at the tile, using the Otsu algorithm
		turnImageBW_Otsu_auto(input.tileImage);		

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
		//this version includes flood filling for holes; this is necessary given the weird morphology of candida colonies
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
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

		//sum up the pixel values (brightness) on the x axis
		double[] sumOfBrightnessXaxis = sumOfRows(input.tileImage);
		double variance = StdStats.varp(sumOfBrightnessXaxis);

		//		System.out.println(variance);

		//if variance is more than 1, then the brightness sum said there's a colony there
		//so there's has to be both variance less than 1 and other filters saying that there's no colony there
		if(variance < varianceThreshold) {// && isTileEmpty(resultsTable, input.tileImage)){
			output.emptyTile = true;
			output.colonySize = 0;//return a colony size of zero
			output.circularity = 0;
			output.morphologyScore = 0;
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
		Point colonyCenter = getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);

		//3.4 get the morphology score of the colony
		//for this, we need the Roi (region of interest) that corresponds to the colony
		//so as to exclude the brightness of any contaminations
		Roi colonyRoi = manager.getRoisAsArray()[indexOfBiggestParticle];
		grayscaleTileCopy.setRoi(colonyRoi);
		//ImagePlus blah = Toolbox.cropImage(copyOfTileImage, colonyRoi);
		
//		copyOfTileImage.show();
//		copyOfTileImage.hide();

		ArrayList<Integer> elevationCounts = getBiggestParticleElevationCounts(grayscaleTileCopy, colonyRoi, colonyCenter);

		//get the sum of the elevation counts for all circles except the previous circle
		//that one is likely to get high elevation counts 
		//just because colony edges tend to be really bright compared to the background
		output.morphologyScore = sumElevationCounts(elevationCounts, 1);
		if(elevationCounts.size()-1<=0) 
			output.normalizedMorphologyScore = 0; 
		else
			output.normalizedMorphologyScore = output.morphologyScore / (elevationCounts.size()-1);


		output.colonyROI = colonyRoi;

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
			
			elevationCounts.add(countBrightnessChanges(meanPixelValues, minimumBrightnessStep));
			
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
	 * This function will get a sequence of measurements and count the times there's a difference
	 * greater or equal to threshold, when subtracting a measurement from it's previous
	 * @param series
	 * @param threshold
	 * @return
	 */
	private static int countBrightnessChanges(ArrayList<Integer> series, int threshold){
		
		int changesOverThreshold = 0;
		
		for (int i = 0; i < series.size()-1; i++) {
			int difference = Math.abs(series.get(i+1) - series.get(i));
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


}
