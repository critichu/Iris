/**
 * 
 */
package iris.tileReaders;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder.Method;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import iris.tileReaderInputs.ColorTileReaderInput;
import iris.tileReaderOutputs.ColorTileReaderOutput;

/**
 * This class provides with methods that output the color of a colony.
 * The color information consists of 2 parts:
 * -the overall color intensity of a colony (the sum of it's relative color intensity)
 * -the area (in pixels) where the CoCo dye binds strongest to the cells (biofilm area)
 * @author George Kritikos
 *
 */
public class ColorTileReader {

	public static ColorTileReaderOutput processTile(ColorTileReaderInput input){

		//0. create the output object
		ColorTileReaderOutput output = new ColorTileReaderOutput();
		//
		//--------------------------------------------------
		//
		//
		
		//1. get a grayscale image as a copy
		ImagePlus grayTile = input.tileImage.duplicate();
		ImageConverter imageConverter = new ImageConverter(grayTile);
		imageConverter.convertToGray8();
		//
		//--------------------------------------------------
		//
		//
		
		//2. threshold the image, 
		//get the bitmask of the largest particle
		//get the coordinates of its perimeter pixels
		turnImageBW_Otsu_auto(grayTile);
		

		//2.1 perform particle analysis on the thresholded tile

		//create the results table, where the results of the particle analysis will be shown
		ResultsTable resultsTable = new ResultsTable();

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER, 
				Measurements.AREA+Measurements.PERIMETER, resultsTable, 5, Integer.MAX_VALUE);
		
		RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
		ParticleAnalyzer.setRoiManager(manager);

		particleAnalyzer.analyze(grayTile); //it gets the image processor internally

		//2.2 pick the largest particle, the check if there is something in the tile has already been performed
		int biggestParticleIndex = getBiggestParticleAreaIndex(resultsTable);
		
		Roi colonyRoi = manager.getRoisAsArray()[biggestParticleIndex];//RoiManager.getInstance().getRoisAsArray()[biggestParticleIndex];
		
		//
		//--------------------------------------------------
		//
		//
		
		//3. remove the background to measure color only from the colony
		
		//first check that there is actually a selection there..
		if(colonyRoi.getBounds().width<=0||colonyRoi.getBounds().height<=0){
			output.biofilmArea=0;
			output.colorIntensitySum=0;
			output.errorOccurred=true;
			
			input.cleanup();
			return(output);
		}
			
		//set that ROI (of the largest particle = colony) on the original picture and fill everything around it with black
		input.tileImage.setRoi(colonyRoi);
		
		try {
			input.tileImage.getProcessor().fillOutside(colonyRoi);
		} catch (Exception e) {
			output.biofilmArea=0;
			output.colorIntensitySum=0;
			output.errorOccurred=true;
			
			input.cleanup();
			return(output);
		}
			
		
		//dilate 3 times to remove the colony periphery
		input.tileImage.getProcessor().dilate();
		input.tileImage.getProcessor().dilate();
		input.tileImage.getProcessor().dilate();
		//
		//--------------------------------------------------
		//
		//

		//4. separate the color channels, calculate relative color intensity of red
		byte[] relativeColorIntensity = calculateRelativeColorIntensity(input.tileImage, 2, 1);
		//
		//--------------------------------------------------
		//
		//
		
		
		//5. get the sum of remaining color intensity
		//6. apply a threshold (found in settings), count area in pixels above the threshold
		// also count the color intensity sum 
		int colonyColorSum = 0;
		int biofilmPixelCount = 0;
		int biofilmColorSum = 0;
		
		for(int i=0;i<relativeColorIntensity.length;i++){
			
			int relativeColorIntensityByteValue = relativeColorIntensity[i]&0xFF;
			
			colonyColorSum += relativeColorIntensityByteValue;
			
			if(relativeColorIntensityByteValue>input.settings.colorThreshold){
				biofilmPixelCount++;
				biofilmColorSum += relativeColorIntensityByteValue;
			}
		}
		
		output.colorIntensitySum = colonyColorSum;
		output.biofilmArea = biofilmPixelCount;
		output.colorIntensitySumInBiofilmArea = biofilmColorSum;
		output.colonyROI = colonyRoi;
	
		input.cleanup();
		return output;
	}

	/**
	 * This function gets the 3 separate channels, and calculates a per-pixel relative intensity on the color.
	 * Default value of the red gain is 2, default value of the blue/green gain is 1
	 * @param channels
	 * @return
	 */
	private static byte[] calculateRelativeColorIntensity(ImagePlus tile, float red_gain, float blue_green_gain) {
		ColorProcessor processor = (ColorProcessor) tile.getProcessor();
		
		byte[] red = processor.getChannel(1);
		byte[] green = processor.getChannel(2);
		byte[] blue = processor.getChannel(3);
		
		
		byte[] redWithGain = multiply(red_gain,red);
		byte[] green_and_blue = multiply(blue_green_gain, add(green, blue));
		byte[] relative_colour_intensity = subtract(redWithGain, green_and_blue);


		return relative_colour_intensity;
	}
	
	/**
	 * This helper function multiplies a byte array by a constant factor, taking into account that
	 * overflowed values are given the maximum 8-bit value (255)
	 * @param factor
	 * @param array
	 * @return
	 */
	private static byte[] multiply(float factor, byte[] array){
		byte[] result = new byte[array.length];
		
		for(int i=0;i<array.length;i++){
			 
			 result[i] =  (byte)(Math.min((array[i] & 0xff)*factor, 255)); 
		}
		return(result);
	}
	
	/**
	 * This helper function adds 2 byte arrays, taking into account that
	 * overflowed values are given the maximum 8-bit value (255)
	 * @param factor
	 * @param array
	 * @return
	 */
	private static byte[] add(byte[] array1, byte[] array2){
		
		//if the 2 arrays are not of equal length, this will crash..
				
		byte[] result = new byte[array1.length];
		for(int i=0;i<array1.length;i++){
			
			//add the values, but avoid overflow
			result[i] = (byte)(Math.min( (array1[i]&0xFF)+(array2[i]&0xFF) , 255));
			
			//result[i] = (byte) (array1[i]+array2[i]);
		}
		
		return(result);
	}
	
	/**
	 * This helper function subtracts 2 byte arrays, taking into account that
	 * underflowed (negative) values are given the minimum value (0)
	 * @param factor
	 * @param array
	 * @return
	 */
	private static byte[] subtract(byte[] array1, byte[] array2){
		
		//if the 2 arrays are not of equal length, this will crash..
				
		byte[] result = new byte[array1.length];
		for(int i=0;i<array1.length;i++){
			
			//subtract the values, but avoid underflow
			result[i] = (byte)(Math.max( (array1[i]&0xFF)-(array2[i]&0xFF) , 0));
		}
		
		return(result);
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
	 * 
	 * This is a copy of the function BasicTileReader.isTileEmpty, in order to maintain the separation
	 * between the 2 tile readout modules
	 * @see BasicTileReader.isTileEmpty
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



	/**
	 * Returns the area of the biggest particle in the results table.
	 * This function compensates for a mildly stringent thresholding algorithm (such as Otsu),
	 * in which it is known that the outer pixels of the colony are missing.
	 * By adding back pixels that equal the periphery in number, we compensate for those missing pixels.
	 * This in round colonies equals to the increase in diameter by 1. Warning: in colonies of highly abnormal
	 * shape (such as colonies that form a biofilm), this could add much more than just an outer layer of pixels,
	 * thus overcorrecting the stringency of the thresholding algorithm. 
	 */
	private static int getBiggestParticleAreaIndex(ResultsTable resultsTable) {

		//get the areas and perimeters of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));
		float perimeters[] = resultsTable.getColumn(resultsTable.getColumnIndex("Perim."));

		//get the index of the biggest particle (in area in pixels)
		int indexOfMax = getIndexOfMaximumElement(areas);


		return(indexOfMax);
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
