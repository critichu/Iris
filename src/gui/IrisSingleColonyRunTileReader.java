/**
 * 
 */
package gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaders.BasicTileReader;
import tileReaders.BasicTileReaderHSB_darkColonies;
import utils.StdStats;
import utils.Toolbox;

/**
 * This class is meant to run just one colony picture at a time.
 * This means there's no cropping, or segmentation, so we skip directly to the
 * tile reader.
 * We also need to output a colony contour together with the output of the tile readers.. 
 * @author George Kritikos
 *
 */
public class IrisSingleColonyRunTileReader {


	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "single tile profile";


	private static double varianceThreshold = 2e4;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//argument 1 is the colony picture filename
		//argument 2 is the tile reader to use


		//		if(args.length<2){
		//			System.out.println("Usage: IrisSingleColonyRunTileReader [colonyPictureFilename] [tileReaderFilename]");
		//			return;
		//		}


		String folderLocation = args[0];

		//get a list of the files in the directory, keeping only image files
		File directory = new File(folderLocation);
		File[] filesInDirectory = directory.listFiles(new PicturesFilenameFilter());


		for (File file : filesInDirectory) {
			String[] args1 = new String[2];
			args1[0] = file.getAbsolutePath();
//			try {
				IrisSingleColonyRunTileReader.processSingleTile(args1);
//			} catch (Exception e) {
//				System.out.println("Couldn't process file..\n");
//			}
			
		}
	}

	public static void processSingleTile(String[] args) {

		String filename = args[0];
		if(filename.contains("ROI"))
			return;

		File file = new File(filename);
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);
		//IrisFrontend.writeToLog("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

		//initialize results file output
		StringBuffer output = new StringBuffer();
		output.append("#Iris output\n");
		output.append("#Profile: " + profileName + "\n");
		output.append("#Iris version: " + IrisFrontend.IrisVersion + ", revision id: " + IrisFrontend.IrisBuild + "\n");
		output.append("#"+filename+"\n");
		output.append("no grid, single colony image file\n");
		output.append("no grid, single colony image file\n");


		//1. open the image file, and check if it was opened correctly
		ImagePlus originalImage = IJ.openImage(filename);
		//check that file was opened successfully
		if(originalImage==null){
			//TODO: warn the user that the file was not opened successfully
			System.err.println("Could not open image file: " + filename);
			return;
		}

		//output the colony measurements as a text file
		output.append("row\t" +
				"column\t" +
				"empty\t" +
				"penalty\t" +
				"variance\t" +
				"numberOfParticles\t" +
				"aspect_ratio\t" +
				"circularity\t" +
				"X_bounding_rectangle\t" +
				"Y_bounding_rectangle\t" +
				"size_threshold\t" +
				"size_hough\n");

//		int i=0;
//		int j=0;
//		output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t");
		//4. pre-process the picture (i.e. make it grayscale)
		ImagePlus tile = originalImage.duplicate();
		ImageConverter imageConverter = new ImageConverter(tile);
		imageConverter.convertToGray8();
		
		BasicTileReaderInput input = new BasicTileReaderInput(tile, null);

		Roi defaultRoi = getTileFeatures(originalImage.duplicate(), output);
		
		BasicTileReaderOutput basicTileReaderOutput = BasicTileReaderHSB_darkColonies.processTile(input);
		

		
		BasicTileReaderOutput houghTileReaderOutput = null;

		//if it's not empty but the default ROI method failed
		//if(!isTileEmpty_simple(originalImage) && basicTileReaderOutput.colonySize==0){
			//call Hough tile reader
			
			tile = originalImage.duplicate();
			imageConverter = new ImageConverter(tile);
			imageConverter.convertToGray8();
			
			input = new BasicTileReaderInput(tile, null);
			
//			houghTileReaderOutput = MyHoughCircleFinder.processTile(input);
//			output.append(Integer.toString(houghTileReaderOutput.colonySize) + "\n");
		//}

//		//check if writing to disk was successful
//		String outputFilename = filename + ".iris";
//		if(!writeOutputFile(outputFilename, output)){
//			System.err.println("Could not write output file " + outputFilename);
//		}
//		else{
//			//System.out.println("Done processing file " + filename + "\n\n");
//			System.out.println("...done processing!");
//		}




		//optionals:

		//paint contour picture (from output Roi) on input picture
		//originalImage.setRoi(output.colonyROI, true);
		originalImage.getProcessor().setColor(java.awt.Color.cyan);
		if(houghTileReaderOutput!=null)
			houghTileReaderOutput.colonyROI.drawPixels(originalImage.getProcessor());

		originalImage.getProcessor().setColor(java.awt.Color.white);
		if(basicTileReaderOutput.colonySize!=0)
			basicTileReaderOutput.colonyROI.drawPixels(originalImage.getProcessor());


		//save it as filename.ROI.png
		Toolbox.savePicture(originalImage, filename.concat(".ROI.png"));

		//write file with one entry
	}

	/**
	 * This function writes the contents of the string buffer to the file with the given filename.
	 * This function was written solely to hide the ugliness of the Exception catching from the Profile code.
	 * @param outputFilename
	 * @param output
	 * @return
	 */
	private static boolean writeOutputFile(String outputFilename, StringBuffer output) {

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
	private static Roi getTileFeatures(ImagePlus tile, StringBuffer output) {

		/**
		 * Penalty is a number given to this tile if some of it's attributes (e.g. circularity of biggest particle)
		 * are borderline to being considered that of an empty tile.
		 * Initially, penalty starts at zero. Every time there is a borderline situation, the penalty gets increased.
		 * When the penalty exceeds a threshold, then this function returns that the tile is empty.
		 */
		int penalty = 0;
		boolean empty = false;

		//4. pre-process the picture (i.e. make it grayscale)
		ImageConverter imageConverter = new ImageConverter(tile);
		imageConverter.convertToGray8();

		//threshold the picture
		Toolbox.turnImageBW_Otsu_auto(tile);



		//2. perform particle analysis on the thresholded tile

		//create the results table, where the results of the particle analysis will be shown
		ResultsTable resultsTable = new ResultsTable();
		RoiManager roiManager = new RoiManager(true);

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER, 
				Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
				resultsTable, 5, Integer.MAX_VALUE);

		ParticleAnalyzer.setRoiManager(roiManager);

		particleAnalyzer.analyze(tile); //it gets the image processor internally

		Roi[] rois = roiManager.getRoisAsArray();

		double[] sumOfBrightnessXaxis = sumOfRows(tile);
		double variance = StdStats.varp(sumOfBrightnessXaxis);


		//3.1 check if the returned results table is empty
		if(resultsTable.getCounter()==0){
			empty=true;
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(0) + "\t");//numberOfParticles
			output.append(Double.toString(0) + "\t");//aspect_ratio
			output.append(Double.toString(0) + "\t");//circularity
			output.append(Double.toString(0) + "\t");//X_bounding_rectangle
			output.append(Double.toString(0) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(0) + "\t");//size
			return(null);
		}










		//get the columns that we're interested in out of the results table
		int numberOfParticles = resultsTable.getCounter();
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));//get the areas of all the particles the particle analyzer has found
		float X_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BX"));//get the X of the bounding rectangles of all the particles
		float Y_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BY"));//get the Y of the bounding rectangles of all the particles
		float X_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("XM"));//get the X of the center of mass of all the particles
		float Y_center_of_mass[] = resultsTable.getColumn(resultsTable.getColumnIndex("YM"));//get the Y of the center of mass of all the particles
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));//get the circularities of all the particles
		float aspect_ratios[] = resultsTable.getColumn(resultsTable.getColumnIndex("AR"));//get the aspect ratios of all the particles

		//for the following, we only check the largest particle
		//which is the one who would be reported either way if we decide that this spot is not empty
		int indexOfMax = getIndexOfMaximumElement(areas);



		//get the width and height of the tile
		int tileDimensions[] = tile.getDimensions();
		int tileWidth = tileDimensions[0];
		int tileHeight = tileDimensions[1];


		//check for the number of detected particles. 
		//Normally, if there is a colony there, the number of results should not be more than 20.
		//We set a limit of 40, since empty spots usually have more than 70 particles.
		if(numberOfParticles>40){
			empty=true;
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
			output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
			output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
			output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
			output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(areas[indexOfMax]) + "\t");//size
			return(null);
		}

		//borderline to empty tile
		if(numberOfParticles>15){
			penalty++;
		}


		//check for unusually high aspect ratio
		//Normal colonies would have an aspect ratio around 1, but contaminations have much higher aspect ratios (around 4)
		if(aspect_ratios[indexOfMax]>2){
			empty=true;
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
			output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
			output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
			output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
			output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(areas[indexOfMax]) + "\t");//size
			return(null);
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
			empty=true; //it's empty
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
			output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
			output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
			output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
			output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(areas[indexOfMax]) + "\t");//size
			return(null);
		}


		//If there is only one particle, then it is sure that this is not an empty spot
		//Unless it's aspect ratio is ridiculously high, which we already made sure it is not
		if(numberOfParticles==1){
			empty=false;//it's not empty
		}

		//assess here the penalty function
		if(penalty>1){
			empty=true; //it's empty
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
			output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
			output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
			output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
			output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(areas[indexOfMax]) + "\t");//size
			return(null);
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
				empty=false; //it's a normal colony
			}

			empty=true; //it's empty
			output.append(Integer.toString(1) + "\t");//empty
			output.append(Integer.toString(penalty) + "\t");//penalty
			output.append(Double.toString(variance) + "\t");//variance
			output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
			output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
			output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
			output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
			output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
			output.append(Double.toString(areas[indexOfMax]) + "\t");//size
			return(null);
		}


		output.append(Integer.toString(0) + "\t");//empty
		output.append(Integer.toString(penalty) + "\t");//penalty
		output.append(Double.toString(variance) + "\t");//variance
		output.append(Double.toString(numberOfParticles) + "\t");//numberOfParticles
		output.append(Double.toString(aspect_ratios[indexOfMax]) + "\t");//aspect_ratio
		output.append(Double.toString(circularities[indexOfMax]) + "\t");//circularity
		output.append(Double.toString(X_bounding_rectangles[indexOfMax]) + "\t");//X_bounding_rectangle
		output.append(Double.toString(Y_bounding_rectangles[indexOfMax]) + "\t");//Y_bounding_rectangle
		output.append(Double.toString(areas[indexOfMax]) + "\t");//size


		return(rois[indexOfMax]);
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

}
