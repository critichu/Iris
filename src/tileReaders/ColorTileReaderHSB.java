/**
 * 
 */
package tileReaders;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.util.Random;

import tileReaderInputs.ColorTileReaderInput;
import tileReaderInputs.ColorTileReaderInput2;
import tileReaderInputs.ColorTileReaderInput3;
import tileReaderOutputs.ColorTileReaderOutput;
import utils.Toolbox;

/**
 * This class provides with methods that output the color of a colony.
 * The color information consists of 2 parts:
 * -the overall color intensity of a colony (the sum of it's relative color intensity)
 * -the area (in pixels) where the CoCo dye binds strongest to the cells (biofilm area)
 * 
 * The HSB version converts an RGB input image into it's HSB counterpart and then thresholds using
 * the B (Brightness) channel. The original version gets the brightness to threshold just by
 * converting the picture to grayscale. As I saw in several ImageJ tests, these are not
 * equivalent, see also note:
 * evernote:///view/4419507/s40/c8e42bc2-98c9-4da0-89d7-a372d4a45a6c/c8e42bc2-98c9-4da0-89d7-a372d4a45a6c/ 
 * @author George Kritikos
 *
 */
public class ColorTileReaderHSB {

	public static ColorTileReaderOutput processTile(ColorTileReaderInput input){

		//1. get a grayscale image as a copy
		ImagePlus grayTile = input.tileImage.duplicate();
		ImageProcessor ip =  grayTile.getProcessor();

		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the tile
		//		ip.snapshot(); // override ColorProcessor bug in 1.32c
		int width = grayTile.getWidth();
		int height = grayTile.getHeight();
		int numPixels = width*height;

		//we need those to save into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);

		//creates a new image using the bSource (brightness)
		ByteProcessor bpBri = new ByteProcessor(width,height,bSource);
		grayTile = new ImagePlus("", bpBri);
		


		//
		//--------------------------------------------------
		//
		//

		//2. threshold the image, 
		//get the bitmask of the largest particle
		//get the coordinates of its perimeter pixels
		ImagePlus thresholded_tile = grayTile.duplicate();
		turnImageBW_Local_auto(thresholded_tile);
		grayTile.flush();
		
		ColorTileReaderInput2 input2 = new ColorTileReaderInput2(input.tileImage, thresholded_tile, input.settings);
		
		
		return (processThresholdedTile(input2));
	}

	
	/**
	 * This function will take as input a colored tile, plus a tile which has already been thresholded.
	 * It will perform particle analysis and measure the color in the thresholded area.
	 * @param input
	 * @return
	 */
	public static ColorTileReaderOutput processThresholdedTile(ColorTileReaderInput2 input){

		//0. create the output object
		ColorTileReaderOutput output = new ColorTileReaderOutput();

		//1. get the thresholded tile ready from the input
		ImagePlus BW_tile = input.thresholdedTileImage;
		turnImageBW_Huang_auto(BW_tile);
//		int width = grayTile.getWidth();
//		int height = grayTile.getHeight();

//		BW_tile.show();
//		BW_tile.hide();
		

		//2.1 perform particle analysis on the thresholded tile

		//create the results table, where the results of the particle analysis will be shown
		ResultsTable resultsTable = new ResultsTable();

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
				Measurements.AREA+Measurements.PERIMETER, resultsTable, 5, Integer.MAX_VALUE);

		RoiManager manager = new RoiManager(true);//we do this so that the RoiManager window will not pop up
		ParticleAnalyzer.setRoiManager(manager);

		particleAnalyzer.analyze(BW_tile); //it gets the image processor internally

		//2.2 pick the largest particle, the check if there is something in the tile has already been performed
		int biggestParticleIndex = getBiggestParticleAreaIndex(resultsTable);

		Roi colonyRoi = manager.getRoisAsArray()[biggestParticleIndex];//RoiManager.getInstance().getRoisAsArray()[biggestParticleIndex];

		
		int colonySize = getBiggestParticleAreaPlusPerimeter(resultsTable, biggestParticleIndex);
		//
		//--------------------------------------------------
		//
		//

		//3. remove the background to measure color only from the colony

		//first check that there is actually a selection there..
		if(colonyRoi.getBounds().width<=0||colonyRoi.getBounds().height<=0){
			output.biofilmArea=0;
			output.colorIntensitySum=0;
			output.colorIntensitySumInBiofilmArea=0;
			output.relativeColorIntensity=0;			
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
			output.colorIntensitySumInBiofilmArea=0;
			output.relativeColorIntensity=0;
			output.errorOccurred=true;

			input.cleanup();
			return(output);
		}


		//dilate 3 times to remove the colony periphery
//		input.tileImage.getProcessor().dilate();
//		input.tileImage.getProcessor().dilate();
//		input.tileImage.getProcessor().dilate();
		//
		//--------------------------------------------------
		//
		//

		//4. separate the color channels, calculate relative color intensity of red
		byte[] pixelBiofilmScores = calculateRelativeColorIntensityUsingSaturationAndBrightness(input.tileImage, 2, 1, (float)1, (float)2); ///
		//byte[] relativeColorIntensity_includingBrightness = calculateRelativeColorIntensity(input.tileImage, 2, 1);

		//but because colonies get darker with accumulation of congo red..		


		//
		//--------------------------------------------------
		//
		//
		
		//show picture
//		ByteProcessor biofilmScoreP = new ByteProcessor(width,height,biofilmScorePerPixel);
//		ImagePlus biofilmScore = new ImagePlus("biofilmScore", biofilmScoreP);
//		biofilmScore.show();
//		biofilmScore.hide();


		//5. get the sum of remaining color intensity
		//6. apply a threshold (found in settings), count area in pixels above the threshold
		// also count the color intensity sum 
		int colonyColorSum = 0;
		int biofilmPixelCount = 0;
		int biofilmColorSum = 0;

		for(int i=0;i<pixelBiofilmScores.length;i++){

			int pixelBiofilmScoreByteValue = pixelBiofilmScores[i]&0xFF;

			colonyColorSum += pixelBiofilmScoreByteValue;

			if(pixelBiofilmScoreByteValue>input.settings.colorThreshold){
				biofilmPixelCount++;
				biofilmColorSum += pixelBiofilmScoreByteValue;
			}
		}

		output.colorIntensitySum = colonyColorSum;
		output.biofilmArea = biofilmPixelCount;
		output.colorIntensitySumInBiofilmArea = biofilmColorSum;
		output.colonyROI = colonyRoi;
		if(colonySize!=0)
			output.relativeColorIntensity = (double) colonyColorSum / (double) colonySize;
			//output.relativeColorIntensity = 10000 * (int) Math.round(Math.log10(colonyColorSum+1)  / colonySize);

		input.cleanup();
		return output;
	}
	
	
	
	/**
	 * This function will take as input a colored tile, plus a tile which has already been thresholded.
	 * It will perform particle analysis and measure the color in the thresholded area.
	 * @param input
	 * @return
	 */
	public static ColorTileReaderOutput processDefinedColonyTile(ColorTileReaderInput3 input){

		//0. create the output object
		ColorTileReaderOutput output = new ColorTileReaderOutput();

		//set the pre-calculated ROI (of the largest particle = colony) on the original picture and fill everything around it with black
		input.tileImage.setRoi(input.colonyRoi);
		
				

		try {
			input.tileImage.getProcessor().fillOutside(input.colonyRoi);
		} catch (Exception e) {
			output.biofilmArea=0;
			output.colorIntensitySum=0;
			output.colorIntensitySumInBiofilmArea=0;
			output.relativeColorIntensity=0;
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
		byte[] pixelBiofilmScores = calculateRelativeColorIntensityUsingSaturationAndBrightness(input.tileImage, 2, 1, (float)1, (float)2); ///
		//byte[] relativeColorIntensity_includingBrightness = calculateRelativeColorIntensity(input.tileImage, 2, 1);

		//but because colonies get darker with accumulation of congo red..		

		

		//
		//--------------------------------------------------
		//
		//
		
		//show picture
//		ByteProcessor biofilmScoreP = new ByteProcessor(width,height,biofilmScorePerPixel);
//		ImagePlus biofilmScore = new ImagePlus("biofilmScore", biofilmScoreP);
//		biofilmScore.show();
//		biofilmScore.hide();


		//5. get the sum of remaining color intensity
		//6. apply a threshold (found in settings), count area in pixels above the threshold
		// also count the color intensity sum 
		int colonyColorSum = 0;
		int biofilmPixelCount = 0;
		int biofilmColorSum = 0;

		for(int i=0;i<pixelBiofilmScores.length;i++){

			int pixelBiofilmScoreByteValue = pixelBiofilmScores[i]&0xFF;

			if(pixelBiofilmScoreByteValue>0){
				colonyColorSum += pixelBiofilmScoreByteValue;
			}
			else{
				continue;
			}

			if(pixelBiofilmScoreByteValue>input.settings.colorThreshold){
				biofilmPixelCount++;
				biofilmColorSum += pixelBiofilmScoreByteValue;
			}
		}
		
		//get pixel color values again, this time by means of integer values
		Float[] pixelBiofilmScores_float = calculateRelativeColorIntensityUsingSaturationAndBrightness_float(input.tileImage, input.colonyRoi, 2, 1, (float)1, (float)2);
		
		
		
		//7. also get an estimate of the colony color, through random pixel sampling, this should be 1000 pixels but only for colonies that are 
		int maxSamples = Math.min(1000, input.colonySize);
		//int maxSamples = 1000;
		double sampleColorSum = 0;
		int numerOfSamplesInBounds = 0;
		
		Random myrandom = new Random((long) 762827825);// this is a seed I picked at random, but it has to be the same always to get the same results with every Iris run 
		for(int i=0; i<maxSamples; i++){
			try {		
				
				//pixelID is an integer from 0 to pixelBiofilmScores.length-1 (pixelBiofilmScores[pixelBiofilmScores.length] is out of bounds)
				int pixelID = (int)Math.round(myrandom.nextDouble()*(double)(pixelBiofilmScores_float.length-1));
				numerOfSamplesInBounds++;
				
				if(pixelBiofilmScores_float[pixelID]==0){
					//pixel was outside the (eroded) colony bounds
					continue;
				}
				
				sampleColorSum += pixelBiofilmScores_float[pixelID];				
			} catch (Exception e) {				
				System.out.println(e.getMessage());
				// otherwise do nothing, this is just in case the pixelID is out of bounds
			}
		}
		//divide by the number of samples
		double meanSampleColor = sampleColorSum/(double)numerOfSamplesInBounds;
		
		
		

		output.colorIntensitySum = colonyColorSum;
		output.biofilmArea = biofilmPixelCount;
		output.colorIntensitySumInBiofilmArea = biofilmColorSum;
		output.colonyROI = input.colonyRoi;
		output.meanSampleColor = meanSampleColor;
		//Toolbox.show(input.tileImage, "after processing");
		
		
		if(input.colonySize!=0)
			output.relativeColorIntensity = (double) colonyColorSum / (double) input.colonySize;
			//output.relativeColorIntensity = 10000 * (int) Math.round(Math.log10(colonyColorSum+1)  / colonySize);

		input.cleanup();
		return output;
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
	 * This function gets the 3 separate channels, and calculates a per-pixel relative intensity on the color.
	 * Default value of the red gain is 2, default value of the blue/green gain is 1.
	 * This version also takes into account the colony's brightness, since we know now that the more colonies
	 * accumulate congo red, the darker they become.
	 * @param channels
	 * @return
	 */
	private static byte[] calculateRelativeColorIntensityUsingSaturationAndBrightness(ImagePlus tile, float red_gain, float blue_green_gain, float color_gain, float brightness_gain) {
		ColorProcessor processor = (ColorProcessor) tile.getProcessor();

		byte[] red = processor.getChannel(1);
		byte[] green = processor.getChannel(2);
		byte[] blue = processor.getChannel(3);


		byte[] redWithGain = multiply(red_gain,red);
		byte[] green_and_blue = multiply(blue_green_gain, add(green, blue));
		byte[] relative_colour_intensity = subtract(redWithGain, green_and_blue);

		
		//end of color calculations
		//------
		//start calculating brightness contribution
		
		ImageProcessor ip =  tile.getProcessor();
		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the tile
		int width = tile.getWidth();
		int height = tile.getHeight();
		int numPixels = width*height;

		//we need those to save into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);
		
		byte[] saturationMinusBrightness = subtract(sSource, bSource);
				
		byte[] relative_colour_intensity_with_gain = multiply(color_gain, relative_colour_intensity);
		byte[] colonySaturationBrightness_with_gain = multiply(brightness_gain, saturationMinusBrightness);
		
		byte[] total_biofilm_score = add(relative_colour_intensity_with_gain, colonySaturationBrightness_with_gain);



		return total_biofilm_score;
	}
	
	
	
	/**
	 * This function gets the 3 separate channels, and calculates a per-pixel relative intensity on the color.
	 * Default value of the red gain is 2, default value of the blue/green gain is 1.
	 * This version also takes into account the colony's brightness, since we know now that the more colonies
	 * accumulate congo red, the darker they become.
	 * This version uses integer-converted byte values, that are easier to work with, and also don't have the issue that we need to limit our values to 255
	 * @param channels
	 * @return
	 */
	private static Float[] calculateRelativeColorIntensityUsingSaturationAndBrightness_float(ImagePlus tile, Roi colonyRoi, float red_gain, float blue_green_gain, float color_gain, float brightness_gain) {
		
		//Float[] roiPixels_brightness = Toolbox.getRoiPixels(tile, colonyRoi, 'l');
		Float[] roiPixels_red = Toolbox.getRoiPixels(tile, colonyRoi, 'r');
		Float[] roiPixels_green = Toolbox.getRoiPixels(tile, colonyRoi, 'g');
		Float[] roiPixels_blue = Toolbox.getRoiPixels(tile, colonyRoi, 'b');
		
		Float[] redWithGain = multiply(red_gain, roiPixels_red);
		Float[] green_and_blue = multiply(blue_green_gain, add(roiPixels_green, roiPixels_blue));
		Float[] relative_colour_intensity = subtract(redWithGain, green_and_blue);

		
		//end of color calculations
		//------
		//start calculating brightness contribution

		
		Float[] roiPixels_saturation = Toolbox.getRoiPixels(tile, colonyRoi, 'S');
		Float[] roiPixels_brightness = Toolbox.getRoiPixels(tile, colonyRoi, 'B');
		
		Float[] roiPixels_darkness = negate_skippingZeros(roiPixels_brightness);
		
		Float[] saturationAndDarkness = add(roiPixels_saturation, roiPixels_darkness);
				
//		Float[] saturationMinusBrightness = subtract(roiPixels_saturation, roiPixels_brightness);
//				
		Float[] relative_colour_intensity_with_gain = multiply(color_gain, relative_colour_intensity);
		Float[] colonySaturationBrightness_with_gain = multiply(brightness_gain, saturationAndDarkness);//saturationMinusBrightness);
		
		Float[] total_biofilm_score = add(relative_colour_intensity_with_gain, colonySaturationBrightness_with_gain);



		return total_biofilm_score;
	}

	
	//for every byte in the given byte array, will convert it to it's corresponding unsigned integer
	public static int[] convertByteArrayToIntegerArray(byte[] byteArray){
		
		int[] toReturn = new int[byteArray.length];
		
		for(int i=0; i<byteArray.length; i++){
			toReturn[i] = byteArray[i]&0xFF;
			
			//make sure we're not in under or overflow, normally, if you 0xFF, then it should be from 0 to 255...
			toReturn[i] = (byte)Math.max(toReturn[i], 0);
			toReturn[i] = (byte)Math.min(toReturn[i], 255);
			
		}
		
		return(toReturn);
	}
	
	

	
	/**
	 * This function gets the 3 separate channels, and calculates a per-pixel relative intensity on the color.
	 * Default value of the red gain is 2, default value of the blue/green gain is 1.
	 * This version also takes into account the colony's brightness, since we know now that the more colonies
	 * accumulate congo red, the darker they become.
	 * @param channels
	 * @return
	 */
	private static byte[] calculateRelativeColorIntensityUsingSaturation(ImagePlus tile, float red_gain, float blue_green_gain, float color_gain, float brightness_gain) {
		ColorProcessor processor = (ColorProcessor) tile.getProcessor();

		byte[] red = processor.getChannel(1);
		byte[] green = processor.getChannel(2);
		byte[] blue = processor.getChannel(3);


		byte[] redWithGain = multiply(red_gain,red);
		byte[] green_and_blue = multiply(blue_green_gain, add(green, blue));
		byte[] relative_colour_intensity = subtract(redWithGain, green_and_blue);

		
		//end of color calculations
		//------
		//start calculating brightness contribution
		
		ImageProcessor ip =  tile.getProcessor();
		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the tile
		int width = tile.getWidth();
		int height = tile.getHeight();
		int numPixels = width*height;

		//we need those to save into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);
				
		byte[] relative_colour_intensity_with_gain = multiply(color_gain,relative_colour_intensity);
		byte[] colonySaturation_with_gain = multiply(brightness_gain,sSource);
		
		byte[] total_biofilm_score = add(relative_colour_intensity_with_gain, colonySaturation_with_gain);



		return total_biofilm_score;
	}
	

	/**
	 * This function gets the 3 separate channels, and calculates a per-pixel relative intensity on the color.
	 * Default value of the red gain is 2, default value of the blue/green gain is 1.
	 * This version also takes into account the colony's brightness, since we know now that the more colonies
	 * accumulate congo red, the darker they become.
	 * @param channels
	 * @return
	 */
	private static byte[] calculateRelativeColorIntensityUsingBrightness(ImagePlus tile, float red_gain, float blue_green_gain, float color_gain, float brightness_gain) {
		ColorProcessor processor = (ColorProcessor) tile.getProcessor();

		byte[] red = processor.getChannel(1);
		byte[] green = processor.getChannel(2);
		byte[] blue = processor.getChannel(3);


		byte[] redWithGain = multiply(red_gain,red);
		byte[] green_and_blue = multiply(blue_green_gain, add(green, blue));
		byte[] relative_colour_intensity = subtract(redWithGain, green_and_blue);

		
		//end of color calculations
		//------
		//start calculating brightness contribution
		
		ImageProcessor ip =  tile.getProcessor();
		ColorProcessor cp = (ColorProcessor)ip;

		//get the number of pixels in the tile
		int width = tile.getWidth();
		int height = tile.getHeight();
		int numPixels = width*height;

		//we need those to save into
		byte[] hSource = new byte[numPixels];
		byte[] sSource = new byte[numPixels];
		byte[] bSource = new byte[numPixels];

		//saves the channels of the cp into the h, s, bSource
		cp.getHSB(hSource,sSource,bSource);

		byte[] colonyDarkness = negate_skippingZeros(bSource); //get directly the Brightness value calculated earlier for the HSB

		
		byte[] relative_colour_intensity_with_gain = multiply(color_gain,relative_colour_intensity);
		byte[] colonyDarkness_with_gain = multiply(brightness_gain,colonyDarkness);
		
		byte[] total_biofilm_score = add(relative_colour_intensity_with_gain, colonyDarkness_with_gain);



		return total_biofilm_score;
	}

	
	
	/**
	 * This helper function will return the negative of the given array: 255-array[i].
	 * At the same time, it will skip zeros, meaning that the pixels outside of the colony will
	 * remain 0. This is because afterwards we will add those values together with the color intensity values.
	 * @param array
	 * @return
	 */
	private static byte[] negate_skippingZeros(byte[] array){
		byte[] result = new byte[array.length];

		for(int i=0;i<array.length;i++){
		
			if(array[i]==(byte)0)
				continue; //keep it zero
			
			result[i] =  (byte)(Math.min(255-(array[i] & 0xff), 255)); 
		}
		return(result);
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

			//avoid overflow
			result[i] =  (byte)(Math.min((array[i] & 0xff)*factor, 255));
			
			//avoid underflow			
			//result[i] = (byte)(Math.max(result[i], 0));
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
			
			//also avoid underflow
			//result[i] = (byte)(Math.max(result[i], 0));
			
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
			
			//also avoid overflow
			//result[i] = (byte)(Math.min(result[i], 255));
		}

		return(result);
	}

	
	
	
	/**
	 * This helper function will return the negative of the given array: 255-array[i].
	 * At the same time, it will skip zeros, meaning that the pixels outside of the colony will
	 * remain 0. This is because afterwards we will add those values together with the color intensity values.
	 * @param array
	 * @return
	 */
	private static int[] negate_skippingZeros(int[] array){
		int[] result = new int[array.length];

		for(int i=0;i<array.length;i++){
		
			if(array[i]==(int)0)
				continue; //keep it zero
			
			result[i] =  (int)(Math.min(255-array[i], 255)); 
		}
		return(result);
	}
	
	
	/**
	 * This helper function will return the negative of the given array: 255-array[i].
	 * At the same time, it will skip zeros, meaning that the pixels outside of the colony will
	 * remain 0. This is because afterwards we will add those values together with the color intensity values.
	 * @param array
	 * @return
	 */
	private static Float[] negate_skippingZeros(Float[] array){
		Float[] result = new Float[array.length];

		for(int i=0;i<array.length;i++){
		
			if(array[i]==(int)0)
				result[i]=(float)0; //keep it zero
			
			result[i] =  Math.min(255-array[i], 255);
			result[i] = Math.max(result[i], 0); //make sure the result is positive
		}
		return(result);
	}
	
	

	/**
	 * This helper function multiplies a int array by a constant factor
	 * @param factor
	 * @param array
	 * @return
	 */
	private static int[] multiply(float factor, int[] array){
		int[] result = new int[array.length];

		for(int i=0;i<array.length;i++){

			result[i] =  (int)Math.round(array[i]*factor);
			
		}
		return(result);
	}
	
	/**
	 * This helper function multiplies a int array by a constant factor
	 * @param factor
	 * @param array
	 * @return
	 */
	private static Float[] multiply(float factor, Float[] array){
		Float[] result = new Float[array.length];

		for(int i=0;i<array.length;i++){

			result[i] =  (Float)(array[i]*factor);
			
		}
		return(result);
	}
	

	/**
	 * This helper function adds 2 int arrays
	 * @param factor
	 * @param array
	 * @return
	 */
	private static int[] add(int[] array1, int[] array2){

		//if the 2 arrays are not of equal length, this will crash..

		int[] result = new int[array1.length];
		for(int i=0;i<array1.length;i++){

			//just add the values
			result[i] = array1[i] + array2[i];
		}

		return(result);
	}
	
	
	/**
	 * This helper function adds 2 int arrays
	 * @param factor
	 * @param array
	 * @return
	 */
	private static Float[] add(Float[] array1, Float[] array2){

		//if the 2 arrays are not of equal length, this will crash..

		Float[] result = new Float[array1.length];
		for(int i=0;i<array1.length;i++){

			//just add the values
			result[i] = array1[i] + array2[i];
		}

		return(result);
	}
	

	/**
	 * This helper function subtracts 2 int arrays, taking into account that
	 * negative values are given the minimum value (0)
	 * @param factor
	 * @param array
	 * @return
	 */
	private static int[] subtract(int[] array1, int[] array2){

		//if the 2 arrays are not of equal length, this will crash..

		int[] result = new int[array1.length];
		for(int i=0;i<array1.length;i++){

			//subtract the values, but avoid underflow
			result[i] = (int)(Math.max( array1[i] - array2[i] , 0));
			
		}

		return(result);
	}
	
	
	/**
	 * This helper function subtracts 2 int arrays, taking into account that
	 * negative values are given the minimum value (0)
	 * @param factor
	 * @param array
	 * @return
	 */
	private static Float[] subtract(Float[] array1, Float[] array2){

		//if the 2 arrays are not of equal length, this will crash..

		Float[] result = new Float[array1.length];
		for(int i=0;i<array1.length;i++){

			//subtract the values, but avoid underflow
			result[i] = (Math.max( array1[i] - array2[i] , 0));
			
		}

		return(result);
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
		Auto_Local_Threshold.Mean(BW_croppedImage, 65, 0, 0, true);
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
