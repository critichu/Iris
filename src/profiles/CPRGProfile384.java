/**
 * 
 */
package profiles;

import gui.IrisGUI;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imageCroppers.CPRGImageCropper;
import imageSegmenterInput.BasicImageSegmenterInput;
import imageSegmenterOutput.BasicImageSegmenterOutput;
import imageSegmenters.RisingTideSegmenter;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import settings.BasicSettings;
import settings.ColorSettings;
import tileReaderInputs.BasicTileReaderInput;
import tileReaderInputs.ColorTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaderOutputs.CPRGTileReaderOutput;
import tileReaders.BasicTileReader;
import tileReaders.CPRGColorTileReader;
import tileReaders.CPRGColorTileReaderHSV;
import utils.Toolbox;

/**
 * This profile is calibrated for use in measuring the colony sizes of E. coli or Salmonella 1536 plates
 * 
 * @author George Kritikos
 *
 */
public class CPRGProfile384 extends Profile {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "CPRG readout Profile for 384 plates";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile is calibrated for reading out the CPRG assay";


	/**
	 * This holds access to the settings object
	 */
	public ColorSettings settings = new ColorSettings();


	/**
	 * This function will analyze the picture using the basic profile
	 * The end result will be a file with the same name as the input filename,
	 * after the addition of a .iris ending
	 * @param filename
	 */
	public void analyzePicture(String filename){


		//0. initialize settings and open files for input and output
		//since this is a 384 plate, make sure the settings are redefined to match our setup
		settings.numberOfColumnsOfColonies = 24;
		settings.numberOfRowsOfColonies = 16;

		//
		//--------------------------------------------------
		//
		//

		File file = new File(filename);
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

		//initialize results file output
		StringBuffer output = new StringBuffer();
		output.append("Iris output\n");
		output.append("Profile: " + profileName + "\n");
		output.append("Iris version: " + IrisGUI.IrisVersion + ", revision id: " + IrisGUI.IrisBuild + "\n");
		output.append(filename+"\n");


		//1. open the image file, and check if it was opened correctly
		ImagePlus originalImage = IJ.openImage(filename);

		//check that file was opened successfully
		if(originalImage==null){
			//TODO: warn the user that the file was not opened successfully
			System.err.println("Could not open image file: " + filename);
			return;
		}




		//
		//--------------------------------------------------
		//
		//

		//2. rotate the whole image
		ImagePlus duplicate = originalImage.duplicate();
		double imageAngle = calculateImageRotation(duplicate);
		duplicate.flush();

		//create a copy of the original image and rotate it, then clear the original picture
		ImagePlus rotatedImage = Toolbox.rotateImage(originalImage, imageAngle);
		originalImage.flush();

		//output how much the image needed to be rotated
		if(imageAngle!=0){
			System.out.println("Image had to be rotated by  " + imageAngle + " degrees");
		}


		//
		//--------------------------------------------------
		//
		//

		//3. crop the plate to keep only the colonies, here, a coloured picture is given to the imageCropper
		ImagePlus croppedImage = CPRGImageCropper.cropPlate(rotatedImage);

		//flush the original picture, we won't be needing it anymore
		rotatedImage.flush();




		//
		//--------------------------------------------------
		//
		//

		//4. pre-process the picture (i.e. make it grayscale)
		ImagePlus colorCroppedImage = croppedImage.duplicate();



		//turn the image BW to aid in segmentation
		//turnImageBW_auto(croppedImage);

		//remove the blue color to aid in segmentation, then turn it black/white
		croppedImage = removeBlueColor(colorCroppedImage.duplicate());
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();
		turnImageBW_auto(croppedImage);


		//
		//--------------------------------------------------
		//
		//

		//calculate the minimum and maximum grid spacings according to the cropped image size 
		//and the number of rows and columns, save the results in the settings object
		calculateGridSpacing_modified(settings, croppedImage);

		//		//change the settings so that the distance between the colonies can now be smaller
		//		settings.minimumDistanceBetweenRows = 40;
		//		//..or larger
		//		settings.maximumDistanceBetweenRows = 100;






		//5. segment the cropped picture
		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(croppedImage, settings);
		BasicImageSegmenterOutput segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);

		//check if something went wrong
		if(segmentationOutput.errorOccurred){

			System.err.println("\n"+profileName+": unable to process picture " + justFilename);

			System.err.print("Image segmentation algorithm failed:\n");

			if(segmentationOutput.notEnoughColumnsFound){
				System.err.print("\tnot enough columns found\n");
			}
			if(segmentationOutput.notEnoughRowsFound){
				System.err.print("\tnot enough rows found\n");
			}
			if(segmentationOutput.incorrectColumnSpacing){
				System.err.print("\tincorrect column spacing\n");
			}
			if(segmentationOutput.notEnoughRowsFound){
				System.err.print("\tincorrect row spacing\n");
			}			


			//save the grid before exiting
			RisingTideSegmenter.paintSegmentedImage(croppedImage, segmentationOutput); //calculate grid image
			IJ.save(croppedImage, filename + ".grid.jpg");

			return;
		}

		int x = segmentationOutput.getTopLeftRoi().getBounds().x;
		int y = segmentationOutput.getTopLeftRoi().getBounds().y;
		output.append("top left of the grid found at (" +x+ " , " +y+ ")\n");

		x = segmentationOutput.getBottomRightRoi().getBounds().x;
		y = segmentationOutput.getBottomRightRoi().getBounds().y;
		output.append("bottom right of the grid found at (" +x+ " , " +y+ ")\n");




		//
		//--------------------------------------------------
		//
		//

		//6. analyze each tile

		//create an array of measurement outputs
		BasicTileReaderOutput [][] basicTileReaderOutputs = new BasicTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		CPRGTileReaderOutput [][] cprgTileReaderOutputs = new CPRGTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		CPRGTileReaderOutput [][] cprgTileReaderOutputsHSV = new CPRGTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				//first get the colony size (so that the user doesn't have to run 2 profiles for this)
				basicTileReaderOutputs[i][j] = BasicTileReader.processTile(
						new BasicTileReaderInput(croppedImage, segmentationOutput.ROImatrix[i][j], settings));

				//only run the CPRG color analysis if there is a colony in the tile
				if(basicTileReaderOutputs[i][j].colonySize>0){
					cprgTileReaderOutputs[i][j] = CPRGColorTileReader.processTile(
							new ColorTileReaderInput(colorCroppedImage, segmentationOutput.ROImatrix[i][j], settings));

					cprgTileReaderOutputsHSV[i][j] = CPRGColorTileReaderHSV.processTile(
							new ColorTileReaderInput(colorCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
				}
				else{
					cprgTileReaderOutputs[i][j] = new CPRGTileReaderOutput();
					cprgTileReaderOutputs[i][j].colorSumInTile = 0;
					cprgTileReaderOutputs[i][j].colorSumInColony = 0;

					cprgTileReaderOutputsHSV[i][j] = new CPRGTileReaderOutput();
					cprgTileReaderOutputsHSV[i][j].colorSumInTile = 0;
					cprgTileReaderOutputsHSV[i][j].colorSumInColony = 0;
				}


				//each generated tile image is cleaned up inside the tile reader
			}
		}



		//check if a row or a column has most of it's tiles empty (then there was a problem with gridding)
		//check rows first
		if(checkRowsColumnsIncorrectGridding(basicTileReaderOutputs)){
			//something was wrong with the gridding.
			//just print an error message, save grid for debugging reasons and exit
			System.err.println("\n"+profileName+": unable to process picture " + justFilename);
			System.err.print("Image segmentation algorithm failed:\n");
			System.err.println("\ttoo many empty rows/columns");

			//calculate and save grid image
			RisingTideSegmenter.paintSegmentedImage(croppedImage, segmentationOutput);
			IJ.save(croppedImage, filename + ".grid.jpg");

			return;
		}


		//correct for overspilling effects in tiles
		int[][] correctedCPRGoutputs_Tiles = slidingWindowDyeOverspillCorrection( CPRGTileReaderOutput.getAllTileColorSums(cprgTileReaderOutputs));

		//correct for overspilling effects in colonies
		int[][] correctedCPRGoutputs_Colonies = slidingWindowDyeOverspillCorrection( CPRGTileReaderOutput.getAllColonyColorSums(cprgTileReaderOutputs));


		//correct for overspilling effects in tiles
		int[][] correctedCPRGoutputs_TilesHSV = slidingWindowDyeOverspillCorrection( CPRGTileReaderOutput.getAllTileColorSums(cprgTileReaderOutputsHSV));

		//correct for overspilling effects in colonies
		int[][] correctedCPRGoutputs_ColoniesHSV = slidingWindowDyeOverspillCorrection( CPRGTileReaderOutput.getAllColonyColorSums(cprgTileReaderOutputsHSV));

		
		//my way --> HSV with square root tiles to discover plate effects, corrected for neighborhood (local plate effects)
		int[][] newWay_tiles = neighborhoodCorrection(CPRGTileReaderOutput.getAllTileColorSums(cprgTileReaderOutputsHSV), 3);


		//7. output the results

		//7.1 output the colony measurements as a text file
		output.append("row\tcolumn\tsize\tavg. dye intensity in colony\tavg. dye intensity in tile\tcorrected avg. dye intensity in colony\tcorrected avg. dye intensity in tile\n");
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
						+ Integer.toString(basicTileReaderOutputs[i][j].colonySize) + "\t"

						+ Integer.toString(cprgTileReaderOutputs[i][j].colorSumInColony) + "\t"
						+ Integer.toString(cprgTileReaderOutputs[i][j].colorSumInTile)  + "\t"
						+ Integer.toString(correctedCPRGoutputs_Colonies[i][j]) + "\t"
						+ Integer.toString(correctedCPRGoutputs_Tiles[i][j]) + "\t"

						+ Integer.toString(cprgTileReaderOutputsHSV[i][j].colorSumInColony) + "\t"
						+ Integer.toString(cprgTileReaderOutputsHSV[i][j].colorSumInTile)  + "\t"
						+ Integer.toString(correctedCPRGoutputs_ColoniesHSV[i][j]) + "\t"
						+ Integer.toString(correctedCPRGoutputs_TilesHSV[i][j]) + "\t"
						
						+ Integer.toString(newWay_tiles[i][j]) + "\n");
			}
		}

		//check if writing to disk was successful
		String outputFilename = filename + ".iris";
		if(!writeOutputFile(outputFilename, output)){
			System.err.println("Could not write output file " + outputFilename);
		}
		else{
			//System.out.println("Done processing file " + filename + "\n\n");
			System.out.println("...done processing!");
		}



		//7.2 save any intermediate picture files, if requested
		settings.saveGridImage = true;
		if(settings.saveGridImage){
			//calculate grid image
			RisingTideSegmenter.paintSegmentedImage(croppedImage, segmentationOutput);
			IJ.save(croppedImage, filename + ".grid.jpg");
		}

		croppedImage.flush();
		colorCroppedImage.flush();

	}


	/**
	 * This function removes the blue color from the picture.
	 * Presumably, this is what is prohibiting it's correct segmentation
	 * @param croppedImage
	 * @return
	 */
	private ImagePlus removeBlueColor(ImagePlus croppedImage) {
		ColorProcessor processor = (ColorProcessor) croppedImage.getProcessor();

		//get the 8-bit (1 byte) channels, each value in that array corresponds to one pixel

		processor.setChannel(3, new ByteProcessor(processor.getWidth(), processor.getHeight()));

		croppedImage.updateImage();

		return(croppedImage);

	}


	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function, employing the Otsu algorithm. 
	 * This function does not return the threshold
	 * @param 
	 */
	private static void turnImageBW_auto(ImagePlus BW_croppedImage) {
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();		
		imageProcessor.setAutoThreshold(Method.Otsu, true, ImageProcessor.BLACK_AND_WHITE_LUT);
	}

	/**
	 * This function will apply a sliding window technique, to compensate for the effect of dye overspilling
	 * to neighboring colonies. Using this technique, the colony that is overproducing dye is attributed with
	 * the measurement of it's neighbors (in addition to it's own measurement).
	 * On the other hand, it's neighbors (which were originally identified as positive), are stripped of their
	 * artifactual measurement and are set to zero.
	 * @param originalTileReaderOutputs
	 */
	private static int [][] slidingWindowDyeOverspillCorrection(int [][] originalTileReaderOutputs){


		//0. make a copy of the input, so that we manipulate the values in a separate object
		//get the sizes of the complete input, define the sliding window sizes

		int [][] cprgTileReaderCorrected = new int[originalTileReaderOutputs.length][originalTileReaderOutputs[0].length];

		for (int i = 0; i < cprgTileReaderCorrected.length; i++) {
			for (int j = 0; j < cprgTileReaderCorrected[0].length; j++) {
				cprgTileReaderCorrected[i][j] =  originalTileReaderOutputs[i][j];
			}
		}



		//inflate the original matrix twice and apply the 25-window correction
		int[][] inflated = inflate2DMatrix(cprgTileReaderCorrected);
		inflated = inflate2DMatrix(inflated);


		int[][] afterSlidingWindow25 = slidingWindow(inflated, 5, 8);

		//deflate once before performing the sliding window 9
		int[][]afterSlidingWindow25_deflated = deflate2DMatrix(afterSlidingWindow25);


		int[][] afterSlidingWindow9 = slidingWindow(afterSlidingWindow25_deflated, 3, 4);

		//deflate once more ...and we're done
		int[][]afterSlidingWindow9_deflated = deflate2DMatrix(afterSlidingWindow9);

		return(afterSlidingWindow9_deflated);



	}



	/**
	 * /**
	 * This function will apply a selective window technique, to compensate for the effect of dye overspilling
	 * to neighboring colonies. Using this technique, the colony that is overproducing dye is attributed with
	 * the measurement of it's neighbors (in addition to it's own measurement).
	 * On the other hand, it's neighbors (which were originally identified as positive), are stripped of their
	 * artifactual measurement and are set to zero.
	 * This function in particular will apply this technique using a window centered around the 5 "brightest" colonies
	 * @param originalTileReaderOutputs
	 * @return
	 */
	private static int [][] selectiveWindowDyeOverspillCorrection(int [][] originalTileReaderOutputs){
		//0. make a copy of the input, so that we manipulate the values in a separate object
		//get the sizes of the complete input, define the sliding window sizes

		int [][] cprgTileReaderCorrected = new int[originalTileReaderOutputs.length][originalTileReaderOutputs[0].length];

		for (int i = 0; i < cprgTileReaderCorrected.length; i++) {
			for (int j = 0; j < cprgTileReaderCorrected[0].length; j++) {
				cprgTileReaderCorrected[i][j] =  originalTileReaderOutputs[i][j];
			}
		}

		//inflate the original matrix twice and apply the 25-window correction
		int[][] inflated = inflate2DMatrix(cprgTileReaderCorrected);
		inflated = inflate2DMatrix(inflated);


		int meanOfInputMatrix = Math.round(getMean(cprgTileReaderCorrected));


		//for the 5 most positive colonies
		for (int i = 0; i < 5; i++) {
			//get which one is the most positive now
			Point mostPositiveTile = getMostPositiveTile(cprgTileReaderCorrected);

			//apply a 25-sized window and then a 9-sized window
			correctOverspill(cprgTileReaderCorrected, mostPositiveTile.x, mostPositiveTile.y, 5, 8, meanOfInputMatrix);
			correctOverspill(cprgTileReaderCorrected, mostPositiveTile.x, mostPositiveTile.y, 3, 4, meanOfInputMatrix);
		}


		return(cprgTileReaderCorrected);


	}





	/**
	 * @param cprgTileReaderCorrected
	 * @param xOfMiddle
	 * @param yOfMiddle
	 * @param windowSize
	 * @param minNumberOfPositives
	 * @param meanOfInputMatrix
	 */
	private static void correctOverspill(int[][] cprgTileReaderCorrected,
			int xOfMiddle, int yOfMiddle, int windowSize, int minNumberOfPositives,
			int meanOfInputMatrix) {

		int windowSizeX = windowSize;
		int windowSizeY = windowSize;
		int startX = xOfMiddle - windowSizeX/2;
		int startY = yOfMiddle - windowSizeY/2;


		//first check if we have at least the minimum number of positives (i.e. if there is an overspill)
		int numberOfPositivesInWindow = countPositivesInWindow(cprgTileReaderCorrected, startX, startY, windowSizeX, windowSizeY, meanOfInputMatrix);
		if(numberOfPositivesInWindow < minNumberOfPositives)
			return;

		//then check if the middle one is the guilty one for the overspill
		boolean isTheMiddleOneTheBrightest = isMiddleColonyTheBrightest(cprgTileReaderCorrected, startX, startY, windowSizeX, windowSizeY);				
		if(!isTheMiddleOneTheBrightest)
			return;


		//since we reached this point, it was indeed the given colony that was responsible for the overspill 
		//we need to subtract all the outer colonies' measurements and add them to the middle one

		//for all colonies in window
		for(int i=startX; i<startX+windowSizeX; i++){
			for(int j=startY; j<startY+windowSizeY; j++){

				if(i==xOfMiddle && j==yOfMiddle)
					continue; //skip the middle colony

				//then set this peripheral colony to zero
				cprgTileReaderCorrected[i][j] = 0;
			}
		}

	}


	/**
	 * @param cprgTileReaderCorrected
	 * @return
	 */
	private static Point getMostPositiveTile(int[][] cprgTileReaderCorrected) {

		int sizeX = cprgTileReaderCorrected.length;
		int sizeY = cprgTileReaderCorrected[0].length;

		int indexX_ofMax = -1;
		int indexY_ofMax = -1;
		int max = Integer.MIN_VALUE;

		for(int i=0; i<sizeX; i++){
			for(int j=0; j<sizeY; j++){
				if(cprgTileReaderCorrected[i][j]>max){
					max = cprgTileReaderCorrected[i][j];
					indexX_ofMax = i;
					indexY_ofMax = j;
				}
			}
		}

		return(new Point(indexX_ofMax, indexY_ofMax));

	}


	/**
	 * This function will perform a sliding window technique to correct for the overspill of dye from a
	 * very positive colony to neighboring colonies. The window is square 
	 * @param inputMatrix
	 * @param i
	 * @param j
	 * @return
	 */
	private static int[][] slidingWindow(int[][] inputMatrix, int windowSideSize, int minNumberOfPositives) {


		int input_sizeX = inputMatrix.length;
		int input_sizeY = inputMatrix[0].length;


		//1. for inner rows and columns
		int windowSizeX = windowSideSize;
		int windowSizeY = windowSideSize;

		int meanOfInputMatrix = Math.round(getMean(inputMatrix));


		//for all the possible starting positions of the sliding window
		for (int startX = 0; startX < input_sizeX - windowSizeX; startX++) {
			for (int startY = 0; startY < input_sizeY - windowSizeY; startY++) {

				//first check if we have at least the minimum number of positives (i.e. if there is an overspill)
				int numberOfPositivesInWindow = countPositivesInWindow(inputMatrix, startX, startY, windowSizeX, windowSizeY, meanOfInputMatrix);
				if(numberOfPositivesInWindow < minNumberOfPositives)
					continue;

				//then check if the middle one is the guilty one for the overspill
				boolean isTheMiddleOneTheBrightest = isMiddleColonyTheBrightest(inputMatrix, startX, startY, windowSizeX, windowSizeY);				
				if(!isTheMiddleOneTheBrightest)
					continue;


				//since we reached this point, we have found the colony responsible for the overspill 
				//we need to subtract all the outer colonies' measurements and add them to the middle one

				int indexX_ofMiddle = startX + windowSizeX/2;
				int indexY_ofMiddle = startY + windowSizeY/2;

				//for all colonies in window
				for(int i=startX; i<startX+windowSizeX; i++){
					for(int j=startY; j<startY+windowSizeY; j++){

						if(i==indexX_ofMiddle && j==indexY_ofMiddle)
							continue; //skip the middle colony

						//first add the peripheral colony's color to the middle one
						//no, don't add it inputMatrix[indexX_ofMiddle][indexY_ofMiddle] += inputMatrix[i][j];

						//then set this peripheral colony to zero
						inputMatrix[i][j] = 0;
					}
				}

			}
		}


		return(inputMatrix);
	}


	/**
	 * This function will create a separate matrix, which holds the difference between the observed measurement in a specific
	 * tile and the expected value. This is because often in the CPRG readout, we see that there are effects systematic to an area
	 * of the plate, which really influence which single mutants a human observer sees as positive and which not.
	 * As was observed, a slightly positive colony surrounded by negative ones, will be seen as positive, whereas Iris would report it as
	 * slightly positive, assuming that there is independence between tiles.
	 * In a different example, the same slightly positive colony, placed among similar colonies wouldn't be humanly observable,
	 * whereas Iris would still report it as a slightly positive colony.
	 * Warning, the output matrix will also have negative values.
	 * @param inputMatrix
	 * @param windowSideSize
	 * @param minNumberOfPositives
	 * @return
	 */
	private static int[][] neighborhoodCorrection(int[][] inputMatrix, int windowSideSize) {

		//create a new matrix of zeros, the size of the original matrix
		int [][] outputMatrix = new int[inputMatrix.length][inputMatrix[0].length];

		//create a copy of the original, to which all operations will occur, so as not to temper with the original matrix
		int [][] inputMatrixCopy = new int[inputMatrix.length][inputMatrix[0].length];


		//inflate the original matrix, so as to have a padding for the outer rows and columns
		//beware that because of this matrix inflation, the new outer rows/columns will be filled with 0
		//hence, for each outer row/column colony, these zeros will be taken into account when calculating the mean (expected) value
		//to fix this, we have the following trick:
		//we add the value of offset = 100 to every element of the original matrix.
		//then we inflate
		//now the resulting matrix doesn't have any zeros, except for the padding rows/columns
		//every time a zero is encountered, we also remove one element from the mean calculation (starting from 8 elements)

		//add an offset to each element of the copied matrix
		int offset = 100;
		for (int i = 0; i < inputMatrixCopy.length; i++) {
			for (int j = 0; j < inputMatrixCopy[0].length; j++) {
				inputMatrixCopy[i][j] =  inputMatrix[i][j]+offset;
			}
		}

		int[][] inflated = inflate2DMatrix(inputMatrixCopy);


		int windowSizeX = 3;
		int windowSizeY = 3;
		int inflated_sizeX = inflated.length;
		int inflated_sizeY = inflated[0].length;
		
		//for each sliding window positioning (size 9 window)
		for (int startX = 0; startX < inflated_sizeX - windowSizeX; startX++) {
			for (int startY = 0; startY < inflated_sizeY - windowSizeY; startY++) {

				//calculate mean of all elements except the middle element and except zero elements
				//get the location of the middle element in this window
				int indexX_ofMiddle = startX + windowSizeX/2;
				int indexY_ofMiddle = startY + windowSizeY/2;

				int sum = 0;
				int numberOfElementsInMean = windowSizeX*windowSizeY-1; //how many elements will be used as the mean denominator

				//for all colonies in window, sum up their values
				for(int i=startX; i<startX+windowSizeX; i++){
					for(int j=startY; j<startY+windowSizeY; j++){

						if(i==indexX_ofMiddle && j==indexY_ofMiddle)
							continue; //skip the middle colony
						
						if(inflated[i][j]==0) {//this is padding (outer row/column from matrix inflation)
							numberOfElementsInMean--; //don't count it in the mean
							continue;
						}
						
						//remove the offset here
						sum += (inflated[i][j]-offset); //the mean will not have any offset
					}
				}

				//calculate diff = (measured-mean)/mean
				float mean = (float)sum/(float)numberOfElementsInMean;
				float measured_minus_offset = inflated[indexX_ofMiddle][indexY_ofMiddle]-offset; //the measurement still has some offset, so we remove it now
				float diff = ( measured_minus_offset - mean);// / mean;
				
				//we don't care if a colony/tile is less positive than expected, do we?
				//if it's less positive, turn this zero
				if(diff < 0){
					diff = 0;
				}
				

				//save it to the output matrix as a percentage increase/decrease
				//the -1 compensate for origin matrix inflation
				outputMatrix[indexX_ofMiddle-1][indexY_ofMiddle-1] = Math.round(diff);
				//outputMatrix[indexX_ofMiddle-1][indexY_ofMiddle-1] = Math.round(100*diff); --if you divide by the mean
			}
		}
		return(outputMatrix);
	}


	/**
	 * This function will return a 2-D matrix that is 2 elements larger in all dimensions.
	 * It is essentially a copy of the original matrix, plus a padding of 2 rows and 2 columns of zeros 
	 * (top, bottom, left, right)
	 * @param input
	 * @return
	 */
	private static int[][] inflate2DMatrix(int[][] input){
		int sizeX = input.length;
		int sizeY = input[0].length;

		//create a new, bigger 2-D array filled with zeros
		int[][] output = new int[sizeX+2][sizeY+2];

		//copy the old one in the new one
		for (int i = 0; i < output.length-2; i++) {
			for (int j = 0; j < output[1].length-2; j++) {
				output[i+1][j+1] = input[i][j];
			}
		}

		return(output);
	}

	/**
	 * This function will return a 2-D matrix that is 2 elements smaller in all dimensions.
	 * It is essentially a copy of the original matrix, plus a padding of 2 rows and 2 columns of zeros 
	 * (top, bottom, left, right)
	 * @param input
	 * @return
	 */
	private static int[][] deflate2DMatrix(int[][] input){
		int sizeX = input.length;
		int sizeY = input[0].length;

		//create a new, bigger 2-D array filled with zeros
		int[][] output = new int[sizeX-2][sizeY-2];

		//copy the old one in the new one
		for (int i = 1; i < input.length-1; i++) {
			for (int j = 1; j < input[1].length-1; j++) {
				output[i-1][j-1] = input[i][j];
			}
		}

		return(output);
	}


	/**
	 * This function counts the number of positives in the sliding window, given it's start coordinates and it's size
	 * @param cprgTileReaderOutputs
	 * @param startX
	 * @param startY
	 * @param windowSizeX
	 * @param windowSizeY
	 * @return
	 */
	private static int countPositivesInWindow(int [][] cprgTileReaderOutputs, int startX, int startY, int windowSizeX, int windowSizeY, int threshold){

		int count = 0;

		for(int i=startX; i<startX+windowSizeX; i++){
			for(int j=startY; j<startY+windowSizeY; j++){
				if(cprgTileReaderOutputs[i][j]>threshold){
					count++;
				}
			}
		}
		return(count);
	}


	/**
	 * This function simply replies whether the middle colony in this window 
	 * has the greatest measurement of all the surrounding colonies.
	 * This is meant to be used to identify the colony that is actually overspilling dye to it's
	 * surroundings
	 * @param cprgTileReaderOutputs
	 * @param startX
	 * @param startY
	 * @param windowSizeX
	 * @param windowSizeY
	 * @return
	 */
	private static boolean isMiddleColonyTheBrightest(int [][] cprgTileReaderOutputs, int startX, int startY, int windowSizeX, int windowSizeY){

		int indexX_ofMax = -1;
		int indexY_ofMax = -1;
		int max = Integer.MIN_VALUE;

		for(int i=startX; i<startX+windowSizeX; i++){
			for(int j=startY; j<startY+windowSizeY; j++){
				if(cprgTileReaderOutputs[i][j]>max){
					max = cprgTileReaderOutputs[i][j];
					indexX_ofMax = i;
					indexY_ofMax = j;
				}
			}
		}

		//find which is the middle colony's indexes in x and y
		int indexX_ofMiddle = startX + windowSizeX/2;
		int indexY_ofMiddle = startY + windowSizeY/2;

		//return true if the biggest colony is actually the middle one
		if(indexX_ofMax == indexX_ofMiddle && indexY_ofMax == indexY_ofMiddle )
			return(true);
		else
			return(false);
	}



	/**
	 * This function calculates the minimum and maximum grid distances according to the
	 * cropped image size and
	 * the number of rows and columns that need to be found.
	 * Since the cropped image needs to be segmented roughly in equal distances, the
	 * nominal distance in which the coluns will be spaced apart will be
	 * nominal distance = image width / number of columns
	 * this should be equal to the (image height / number of rows), which is not calculated separately.
	 * Using this nominal distance, we can calculate the minimum and maximum distances, which are then used
	 * by the image segmentation algorithm. Distances that do in practice lead the segmentation algorithm
	 * to a legitimate segmentation of the picture are:
	 * minimum = 2/3 * nominal distance
	 * maximum = 4/3 * nominal distance
	 * 
	 * @param settings_
	 * @param croppedImage
	 */
	private void calculateGridSpacing_modified(BasicSettings settings_,
			ImagePlus croppedImage) {

		int image_width = croppedImage.getWidth();
		float nominal_width = image_width / settings_.numberOfColumnsOfColonies;

		//save the results directly to the settings object
		settings_.minimumDistanceBetweenRows = Math.round(nominal_width*5/7);
		settings_.maximumDistanceBetweenRows = Math.round(nominal_width*5/3);
		//		settings_.minimumDistanceBetweenRows = Math.round(nominal_width*4/5);
		//		settings_.maximumDistanceBetweenRows = Math.round(nominal_width*5/3);

	}


	/**
	 * This function will check if there is any row or any column with more than half of it's tiles being empty.
	 * If so, it will return true. If everything is ok, it will return false.
	 * @param readerOutputs
	 * @return
	 */
	private boolean checkRowsColumnsIncorrectGridding(
			BasicTileReaderOutput[][] readerOutputs) {

		int numberOfRows = readerOutputs.length;		
		if(numberOfRows==0)
			return(false);//something is definitely wrong, but probably not too many empty tiles

		int numberOfColumns = readerOutputs[0].length;



		//for all rows
		for(int i=0; i<numberOfRows; i++){
			int numberOfEmptyTiles = 0;
			//for all the columns this row spans
			for (int j=0; j<numberOfColumns; j++) {
				if(readerOutputs[i][j].colonySize==0)
					numberOfEmptyTiles++;
			}

			//check the number of empty tiles for this row 
			if(numberOfEmptyTiles>numberOfColumns/2)
				return(true); //we found one row that more than half of it's colonies are of zero size			
		}

		//do the same for all columns
		for (int j=0; j<numberOfColumns; j++) {
			int numberOfEmptyTiles = 0;
			//for all the rows this column spans
			for(int i=0; i<numberOfRows; i++){
				if(readerOutputs[i][j].colonySize==0)
					numberOfEmptyTiles++;
			}

			//check the number of empty tiles for this column 
			if(numberOfEmptyTiles>numberOfRows/2)
				return(true); //we found one row that more than half of it's colonies are of zero size			
		}

		return(false);
	}


	/**
	 * This function will create a copy of the original image, and rotate that copy.
	 * The original image should be flushed by the caller if not reused
	 * @param originalImage
	 * @param angle
	 * @return
	 */
	private ImagePlus rotateImage(ImagePlus originalImage, double angle) {

		originalImage.deleteRoi();
		ImagePlus aDuplicate = originalImage.duplicate();//because the caller is going to flush the original image

		aDuplicate.getProcessor().setBackgroundValue(0);

		IJ.run(aDuplicate, "Arbitrarily...", "angle=" + angle + " grid=0 interpolate enlarge");  

		aDuplicate.updateImage();

		return(aDuplicate);



		//		ImagePlus rotatedOriginalImage = originalImage.duplicate();
		//		rotatedOriginalImage.getProcessor().rotate(angle);
		//		rotatedOriginalImage.updateImage();
		//		
		//		return(rotatedOriginalImage);
	}


	/**
	 * This method gets a subset of that picture (for faster execution), and calculates the rotation of that part
	 * using an OCR-derived method. The method applied here rotates the image, attempting to maximize
	 * the variance of the sums of row and column brightnesses. This is in direct analogy to detecting skewed text
	 * in a scanned document, as part of the OCR procedure.
	 * @param originalImage
	 * @return the angle of this picture's rotation 
	 */
	private double calculateImageRotation(ImagePlus originalImage) {
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
		turnImageBW_Otsu(imageSubset);


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
	 * I cannot believe I have to write this
	 * @param list
	 * @return
	 */
	static double getMean(ArrayList<Integer> list){

		int sum = 0;

		for(int i=0;i<list.size();i++){
			sum += list.get(i);
		}

		return(sum/list.size());
	}


	/**
	 * I cannot believe I have to write this
	 * @param list
	 * @return
	 */
	static float getMean(int[][] list){

		long sum = 0;

		for(int i=0;i<list.length;i++){
			for (int j = 0; j < list[0].length; j++) {
				sum += list[i][j];
			}		
		}

		return(sum/(list.length*list[0].length));
	}

	/**
	 * There has to be a better way guys..
	 * @param list
	 * @return
	 */
	static double getVariance(ArrayList<Integer> list){
		double mean = getMean(list);

		double sum = 0;

		for(int i=0;i<list.size();i++){			
			sum += Math.pow(list.get(i)-mean, 2);
		}

		return(sum/(list.size()-1));

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
	private static ArrayList<Integer> sumOfColumns(ImagePlus croppedImage){
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
	private static int turnImageBW_Otsu(ImagePlus grayscaleImage) {
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
	 * This function writes the contents of the string buffer to the file with the given filename.
	 * This function was written solely to hide the ugliness of the Exception catching from the Profile code.
	 * @param outputFilename
	 * @param output
	 * @return
	 */
	private boolean writeOutputFile(String outputFilename, StringBuffer output) {

		FileWriter writer;

		try {
			writer = new FileWriter(outputFilename);
			writer.write(output.toString());
			writer.close();

		} catch (IOException e) {
			return(false); //operation failed
		}

		return(true); //operation succeeded
	}



}
