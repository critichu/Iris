/**
 * 
 */
package test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import profiles.BasicProfile;
import settings.BasicSettings;
import settings.Settings;

/**
 * @author george
 *
 */
public class RisingTideDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		File directory = new File(args[0]);
		if(!directory.isDirectory()){
			System.err.println("\n"+args[0]+" is not a valid directory, exiting");
			return;
		}
		
		FilenameFilter filter = new FilenameFilter() {
			
			@Override
			/**
			 * This function is designed to accept every JPEG file in the given directory
			 * Except dat filenames and the ones that that their filename contains the word "grid"
			 */
			public boolean accept(File dir, String filename) {
				if(filename.contains(".iris")){
					return(false);
				}				
				if(filename.contains(".fig")){
					return(false);
				}
				if(filename.endsWith("dat")){
					return(false);
				}
				if(filename.contains(".grid.")){
					return(false);
				}
				if(filename.contains("jpg") || filename.contains("JPG")){
					return(true);
				}
				return(false);
			}
		};
		
		File[] filesInDirectory = directory.listFiles(filter);
		
		
		
		
		
		for (File file : filesInDirectory) {
			String filename = file.getAbsolutePath();
			
			BasicProfile profile = new BasicProfile();
			profile.analyzePicture(filename);
			//System.err.println("\n");
		}
	}
	
	public static void myRisingTideDemo(String filename){
		
		BasicSettings settings = new BasicSettings();
		
		File myFile = new File(filename);
		String onlyName = myFile.getName();
		
		

		
		String pictureFileName = filename;//"images/easy.jpeg";
		
		
		ImagePlus originalImage = IJ.openImage(pictureFileName);
		
		//check that file was opened successfully
		if(originalImage==null){
			System.err.println("File: "+onlyName+". Warning! Could not open image file: " + pictureFileName);
			return;
		}
		
		//crop so as to keep only the colonies
		ImagePlus croppedImage = naiveCropPlate(originalImage);
		
		//trash the originalPicture
		//originalImage.flush();
		 
		
		//make the cropped picture grayscale
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();

		//create a copy of the original image, just in case
		ImagePlus BW_croppedImage = croppedImage.duplicate();

		
	//	BW_croppedImage.show();
		//make picture black and white here
		///IJ.setAutoThreshold(BW_croppedImage, "Otsu");		
		///IJ.run("Convert to Mask");
		///IJ.run("Invert");
		///BW_croppedImage.updateImage();
		///BW_croppedImage.hide();
		//croppedImage.updateAndDraw();
		
		int threshold = turnImageBW_Otsu(BW_croppedImage);		
	//	BW_croppedImage.show();
		
		
		//save the threshold into settings
		settings.threshold = threshold;
		
		
		//the next step includes calculating the sum of the row/column brightness
		ArrayList<Integer> sumOfColumns = sumOfColumns(BW_croppedImage);
		ArrayList<Integer> sumOfRows = sumOfRows(BW_croppedImage);
		
		BW_croppedImage.flush();
		
		
		
		
		
		//in this step, we apply the rising tide algorithm, first to the sum of rows, then to the sum of columns
		ArrayList<Integer> minimaBagRows = risingTide(sumOfRows, settings, true);
		ArrayList<Integer> minimaBagColumns = risingTide(sumOfColumns, settings, false);
		
		
		//check how many minima did rising tide return
		if(minimaBagRows.size()!=settings.numberOfRowsOfColonies+1){
			System.err.println("File: "+onlyName+". Warning! Image segmentation: Rising Tide: not enough horizontal lines in mesh (found only  " 
								+ Integer.toString(minimaBagRows.size()) + ")" );
			//TODO: also warn the user via the GUI
		}
		if(minimaBagColumns.size()!=settings.numberOfColumnsOfColonies+1){
			System.err.println("File: "+onlyName+". Warning! Image segmentation: Rising Tide: not enough vertical lines in mesh (found only  " 
					+ Integer.toString(minimaBagColumns.size()) + ")" );
			//TODO: also warn the user via the GUI
		}

		//we need them sorted for the next step
		Collections.sort(minimaBagRows);
		Collections.sort(minimaBagColumns);
		
		
		
		//debug_printWeirdDistances(onlyName, minimaBagRows, minimaBagColumns);
		
		
		
		System.out.println("\nnow processing tiles of picture " + filename + "\n");
		croppedImage.show();
		
		//in this step we find out the ROIs that will give us the tiles
		Roi [][] ROImatrix = new Roi[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		
		
		//do some processing on the tiles
		
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){			
			int heightOfThisRow = minimaBagRows.get(i+1) - minimaBagRows.get(i); //Ydiff -- rows go with i
			
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				int widthOfThisColumn = minimaBagColumns.get(j+1) - minimaBagColumns.get(j); //Xdiff -- columns go with j
				
				ROImatrix[i][j] = new Roi( 
						/*x*/ minimaBagColumns.get(j),
						/*y*/ minimaBagRows.get(i),
						/*width*/ widthOfThisColumn,
						/*height*/ heightOfThisRow);
				
				
				int area = getTileArea_autoBackground(croppedImage, ROImatrix[i][j], settings);
				
				
				
				
				
				//output the size of the biggest particle in it's area's pixels, as well as the i and j where it's found
				System.out.println(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" + area);
				
				
				if(area==0){
					System.out.print("");
				}

				
			}
		}
		
		//hide the picture and clear the memory before exiting this function
		croppedImage.hide();
		croppedImage.flush();
		
		

		
		
		
		//now all that's left is to draw the picture
		//paintSegmentedImage(croppedImage, minimaBagRows, minimaBagColumns);
		
		//croppedImage.show();
		
		
		
		//save the file
		//FileSaver fileSaver = new FileSaver(croppedImage);
		//fileSaver.saveAsJpeg(filename+".grid.jpeg");


	}
	
	
	
	
	/**
	 * Displays a tile in the image croppedImage, given it's ROI
	 * @param croppedImage
	 * @param roi
	 */
	private static void debug_showTile(ImagePlus croppedImage, Roi roi) {
		croppedImage.setRoi(roi);
		croppedImage.copy(false);
		
		ImagePlus newImage = ImagePlus.getClipboard();
		turnImageBW_Otsu_auto(newImage);
		
		
		//particle analysis
		ResultsTable resultsTable = new ResultsTable();//where the results will be stored
		
		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS, Measurements.AREA, resultsTable, 5, Integer.MAX_VALUE);
		//particleAnalyzer.analyze(newImage); //it gets the processor internally

		
		//newImage.show();
		
		particleAnalyzer.analyze(newImage); //it gets the processor internaly
		//resultsTable.show("particle analyzer results");
		
		newImage.hide();
		newImage.flush();
		
		//get the area of the first result 
		float result[] = resultsTable.getColumn(0);//get the areas of all the particles the particle analyzer has found
		
//		for (int i = 0; i < result.length; i++) {
//			System.err.println("found area in image " + result[i]);
//		}
		
		if(result.length!=1){
			System.err.println("found " + result.length + " results in the picture!");
		}
		
		
		
		
	}
	
	
	
	/**
	 * Gets a tile from the given roi of the given picture.
	 * Then gets the particles present in it and outputs the size of the biggest one.
	 * This version of the tile readout calculates a per-tile threshold using Otsu algorithm.
	 * Returns the aeria of the biggest particle
	 * @param croppedImage
	 * @param roi
	 */
	private static int getTileArea_autoBackground(ImagePlus croppedImage, Roi roi, Settings settings) {
		
		//1. first, get the tile image by copying it off the input picture (the grayscaled and cropped original input image)
		croppedImage.setRoi(roi);
		croppedImage.copy(false);
		ImagePlus tileImage = ImagePlus.getClipboard();
		
		//2. apply a threshold at the tile, using the Otsu algorithm
		turnImageBW_Otsu_auto(tileImage);
		
		
		//3. perform particle analysis on the thresholded tile
		
		//create the results table, where the results of the particle analysis will be shown
		ResultsTable resultsTable = new ResultsTable();
		
		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT, resultsTable, 5, Integer.MAX_VALUE);
		particleAnalyzer.analyze(tileImage); //it gets the image processor internally

		
		
		tileImage.flush();//clear the tile image here, since we don't need it anymore
		 
		
		//4.1 check to see if the tile was empty. If so, return a colony size of zero
		if(isTileEmpty(resultsTable)){
			return(0);//return a colony size of zero
		}
		
		
		//4.2 if there was a colony there, return the area of the biggest particle
		//this should also clear away contaminations, because normally the contamination
		//area will be smaller than the colony area, so the contamination will never be reported
		int area = getBiggestParticleArea(resultsTable);
		return(area);//returns the biggest result	
			
	}
	
	
	
	
	/**
	 * Returns the area of the biggest particle in the results table
	 */
	private static int getBiggestParticleArea(ResultsTable resultsTable) {

		//get the areas of all the particles the particle analyzer has found
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));
		
		int indexOfMax = getIndexOfMaximumElement(areas);
		
		return(Math.round(areas[indexOfMax]));
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
	private static boolean isTileEmpty(ResultsTable resultsTable) {
		
		//get the columns that we're interested in out of the results table
		int numberOfParticles = resultsTable.getCounter();
		float areas[] = resultsTable.getColumn(resultsTable.getColumnIndex("Area"));//get the areas of all the particles the particle analyzer has found
		float X_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BX"));//get the X of the center of mass of all the particles
		float Y_bounding_rectangles[] = resultsTable.getColumn(resultsTable.getColumnIndex("BY"));//get the Y of the center of mass of all the particles
		float circularities[] = resultsTable.getColumn(resultsTable.getColumnIndex("Circ."));//get the circularities of all the particles
		
		
		//check for the number of detected particles. 
		//Normally, if there is a colony there, the number of results should not be more than 20.
		//We set a limit of 40, since empty spots usually have more than 70 particles.
		if(numberOfParticles>40){
			return(true);//it's empty
		}
		
		//If there is only one particle, then it is sure that this is not an empty spot
		if(numberOfParticles==1){
			return(false);//it's empty
		}
		
		
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
			return(true); //it's empty
		}
		
		
		return(false);//it's not empty
		
		
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
	 * Gets a tile from the given roi of the given picture.
	 * Then gets the particles present in it and outputs the size of the biggest one.
	 * This version of the tile readout uses a preset background, that was set from a previous step
	 * (i.e. the image segmentation)
	 * @param croppedImage
	 * @param roi
	 */
	private static int getTileArea_presetBackground(ImagePlus croppedImage, Roi roi, BasicSettings settings) {
		croppedImage.setRoi(roi);
		croppedImage.copy(false);
		
		//get the tile by copying it off the main (cropped) image
		ImagePlus tileImage = ImagePlus.getClipboard();
		ImageProcessor tileImageProcessor = tileImage.getProcessor();
		
		//apply the preset threshold to the tileImage
		tileImageProcessor.threshold(settings.threshold);
		tileImageProcessor.invert();
		
		//show the tile
		//tileImage.updateImage();
		//tileImage.show();
		
		
		//particle analysis
		ResultsTable resultsTable = new ResultsTable();//where the results will be stored
		
		
		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.AREA, resultsTable, 5, Integer.MAX_VALUE);
		particleAnalyzer.analyze(tileImage); //it gets the image processor internally

		
		//get the areas of all the particles the particle analyzer has found
		float particleSizes[] = resultsTable.getColumn(0);
		
		//if no results are returned, then no particles were found. This means that there was no colony at that spot, hence we return the area size of zero.
		if(particleSizes==null){
			return(0);
		}

		//sort the results
		Arrays.sort(particleSizes);
				
		//checks to see if we only have one particle
		if(particleSizes.length!=1){
			ParticleAnalyzer newparticleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OUTLINES, Measurements.AREA, resultsTable, 5, Integer.MAX_VALUE);
			newparticleAnalyzer.analyze(tileImage); //re-analyze so as to show what the found particles are
			
			//i also want to see the original picture
			tileImage.show();
			
			System.err.println("found " + particleSizes.length + " results in the picture!");
			tileImage.hide();
		}
		
		tileImage.hide();
		tileImage.flush();//clear the newly constructed picture before exiting
		return(Math.round(particleSizes[particleSizes.length-1]));//returns the biggest result
		
		
		
	}

	/**
	 * prints out all the row/column distances that are not expected normally
	 * @param onlyName
	 * @param minimaBagRows
	 * @param minimaBagColumns
	 */
	private static void debug_printWeirdDistances(String onlyName,
			ArrayList<Integer> minimaBagRows,
			ArrayList<Integer> minimaBagColumns) {
		//in this step, we print out all the differences of minima coordinates that are significantly different than 84
		for(int i=1; i<minimaBagRows.size(); i++){
			int diff = minimaBagRows.get(i) - minimaBagRows.get(i-1);
			if(diff < 64 || diff > 100){
				System.err.println("File: "+onlyName+". Warning! Unexpected distance ("+diff+") on row " +i+"!");
			}
		}
		
		for(int i=1; i<minimaBagColumns.size(); i++){
			int diff = minimaBagColumns.get(i) - minimaBagColumns.get(i-1);
			if(diff < 64 || diff > 100){
				System.err.println("File: "+onlyName+". Warning! Unexpected distance ("+diff+") on column " +i+"!");
			}
		}
		
	}

	/**
	 * This function will convert the given picture into black and white using an automatically defined threshold value
	 * and will output that value
	 * @param 
	 */
	private static int turnImageBW_auto(ImagePlus BW_croppedImage) {
//		Calibration calibration = new Calibration(BW_croppedImage);
		
		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();
		
//		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
//		int[] histogram = statistics.histogram;
//		
//		AutoThresholder at = new AutoThresholder();
//		int threshold = at.getThreshold(Method.Otsu, histogram);
		int threshold = imageProcessor.getAutoThreshold();
		
		imageProcessor.threshold(threshold);
		
		return(threshold);
		
		
		//BW_croppedImage.updateAndDraw();
	}
	
	
	/**
	 * This function will convert the given picture into black and white
	 * using ImageProcessor's auto thresholding function. This function does not return the threshold
	 * @param 
	 */
	private static void turnImageBW_Otsu_auto(ImagePlus BW_croppedImage) {
		
		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();
		
//		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
//		int[] histogram = statistics.histogram;
//		
//		AutoThresholder at = new AutoThresholder();
//		int threshold = at.getThreshold(Method.Otsu, histogram);
		
		imageProcessor.setAutoThreshold(Method.Otsu, true, ImageProcessor.BLACK_AND_WHITE_LUT);
		
		//BW_croppedImage.updateAndDraw();
	}
	
	
	
	/**
	 * This function will convert the given picture into black and white
	 * using the histogram method. This version will also return the threshold found.
	 * @param 
	 */
	private static int turnImageBW_Otsu(ImagePlus BW_croppedImage) {
		Calibration calibration = new Calibration(BW_croppedImage);
		
		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();
		
		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;
		
		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Otsu, histogram);
		
		imageProcessor.threshold(threshold);
				
		//BW_croppedImage.updateAndDraw();
		
		return(threshold);
	}
	
	
	
	

	/**
	 * This function takes a picture and draws lines in the coordinates of the rows and columns given as arguments
	 * @param croppedImage
	 * @param minimaBagRows
	 * @param minimaBagColumns
	 * @deprecated
	 * @see <code>paintSegmentedImage</code> 
	 */
	private static void drawSegmentedImage(ImagePlus croppedImage,
			ArrayList<Integer> minimaBagRows,
			ArrayList<Integer> minimaBagColumns) {
		//now, all that remains is to paint the picture using imageJ
		ImageProcessor croppedImageProcessor = croppedImage.getProcessor();
		int dimensions[] = croppedImage.getDimensions();
		
		
		//draw horizontal lines
		for(int i=0; i<minimaBagRows.size(); i++){
			croppedImageProcessor.drawLine(0, minimaBagRows.get(i), dimensions[0], minimaBagRows.get(i));
		}
		
		
		//draw vertical lines
		for(int i=0; i<minimaBagColumns.size(); i++){
			croppedImageProcessor.drawLine(minimaBagColumns.get(i), 0, minimaBagColumns.get(i), dimensions[1]);
		}
		
		croppedImage.updateImage();
		croppedImage.show();
		

	}
	
	
	
	/**
	 * This function takes a picture and draws lines in the coordinates of the rows and columns given as arguments
	 * @param croppedImage
	 * @param minimaBagRows
	 * @param minimaBagColumns
	 */
	private static void paintSegmentedImage(ImagePlus croppedImage,
			ArrayList<Integer> minimaBagRows,
			ArrayList<Integer> minimaBagColumns) {
		//now, all that remains is to paint the picture using imageJ
		ImageProcessor croppedImageProcessor = croppedImage.getProcessor();
		int dimensions[] = croppedImage.getDimensions();
		
		
		//draw horizontal lines
		for(int i=0; i<minimaBagRows.size(); i++){
			
			//draw 3 pixels, one before, one after and one in the middle (where the actual boundary is
			croppedImageProcessor.drawLine(0, minimaBagRows.get(i)-1, dimensions[0], minimaBagRows.get(i)-1);
			
			croppedImageProcessor.drawLine(0, minimaBagRows.get(i), dimensions[0], minimaBagRows.get(i));
			
			croppedImageProcessor.drawLine(0, minimaBagRows.get(i)+1, dimensions[0], minimaBagRows.get(i)+1);
		}
		
		
		//draw vertical lines
		for(int i=0; i<minimaBagColumns.size(); i++){
			
			//draw 3 pixels, one before, one after and one in the middle (where the actual boundary is
			croppedImageProcessor.drawLine(minimaBagColumns.get(i)-1, 0, minimaBagColumns.get(i)-1, dimensions[1]);
			
			croppedImageProcessor.drawLine(minimaBagColumns.get(i), 0, minimaBagColumns.get(i), dimensions[1]);
			
			croppedImageProcessor.drawLine(minimaBagColumns.get(i)+1, 0, minimaBagColumns.get(i)+1, dimensions[1]);
		}
		
		croppedImage.updateImage();
		//croppedImage.show();
		
		

		

	}




	/**
	 * This method will naively crop the plate in a hard-coded manner.
	 * It copies the area of interest (580, 380, 4080, 2730) to the internal clipboard.
	 * Then, it copies the internal clipboard results to a new ImagePlus object.
	 * @param originalPicture
	 * @return
	 */
	public static ImagePlus naiveCropPlate(ImagePlus originalImage){
		//crop the plate so that we keep only the colonies
		Roi keepOnlyColoniesROI = new Roi(580, 380, 4080, 2730);
		originalImage.setRoi(keepOnlyColoniesROI);
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
	 * This function implements the rising tide algorithm to find local minima in the
	 * sum of light intensities, that are distant by at least a minimum distance, defined in the settings
	 * @param sumOfBrightness : an array of integers that is calculated by summing the brightness of all the pixels in an image row-wise or column-wise 
	 * @param settings : pointer to a settings object, which can be used to fine-tune the algorithm
	 * @param isRows : a boolean that shows whether this call is meant to find minima of rows or columns
	 * @return a list of first X minima that were found while the threshold was rising
	 */
	public static ArrayList<Integer> risingTide(ArrayList<Integer> sumOfBrightness, BasicSettings settings, boolean isRows){
		
		boolean errorOccurred = false;
		
		//calculate the number of rows and columms we should reach
		int targetMinimaNumber;
		if(isRows)
			targetMinimaNumber = settings.numberOfRowsOfColonies+1;
		else//is colonies
			targetMinimaNumber = settings.numberOfColumnsOfColonies+1;
		
		
					
		//start with an empty bag
		ArrayList<Integer> minimaBag = new ArrayList<Integer>();
		
		
		
		//Integer indexOfCurrentMinimum = new Integer(-1);
		
		while(true){
			
			//Step 1: find the current global minimum and it's index
			Integer indexOfCurrentMinimum = new Integer(getMinimumAndIndexBW(sumOfBrightness));
			Integer currentMinimum = new Integer(sumOfBrightness.get(indexOfCurrentMinimum));
			
			//Step 2: check: if everything is now under the tide (was assigned a max value), the algorithm has to end
			if(currentMinimum==Integer.MAX_VALUE){
				
				
//			this check is now delegated to the caller of rising tide, since the caller also knows
//			the number of minima that need to be returned, and probably has better access to notify the user
//			even by GUI			
//				
//				if(isRows){
//					if(	minimaBag.size()!=settings.numberOfRowsOfColonies+1){
//						errorOccurred = true;
//					}
//				}
//				else{//is Columns
//					if(minimaBag.size()!=settings.numberOfColumnsOfColonies+1){
//						errorOccurred = true;
//					}
//				}
//				
//				//if we didn't find enough rows or columns, we have to warn the user				
//				if(errorOccurred){
//					System.err.println("Warning! Image segmentation: Rising Tide: not enough lines in mesh (found only  " 
//							+ Integer.toString(minimaBag.size()) + ")\n" );
//					
//					
//					//TODO: also warn the user via the GUI
//				}
				
				return(minimaBag); 
			}
			
						
			//Step 3: set everything that is within reach (left and right) of the current minimum to MAX_INT
			//INCLUDING this minimum: it's fate is decided on the next step
			//we have to go as far as the distance in the settings mandates
			
			int leftMostIndex = indexOfCurrentMinimum - settings.minimumDistanceBetweenRows;
			int rightMostIndex = indexOfCurrentMinimum + settings.minimumDistanceBetweenRows;
			
			//we cannot go lower than 0, so set leftMost to 0 if that would have been the case
			if(leftMostIndex<0)
				leftMostIndex = 0;		

			//we cannot go over the maximum index, so set rightMost to maxIndex if that would have been the case
			if(rightMostIndex>sumOfBrightness.size()-1)
				rightMostIndex = sumOfBrightness.size()-1;
						
			for(int i=leftMostIndex; i<=rightMostIndex; i++){
				sumOfBrightness.set(i, Integer.MAX_VALUE);
			}
			
			
			
			//Step 4: check whether our bag of minimas already has a nearby minima there
			//this check is redundant, I'll skip it
			//------------------------------
			
			
			
			//Step 5: add this minima into the minima bag and check the bag's size
			//if the size is equal to the one we want (from settings), then the function is done
			minimaBag.add(indexOfCurrentMinimum);
			
			if(minimaBag.size()>=targetMinimaNumber){
				//we've reached our goal
				
				Collections.sort(minimaBag);
				
				return(minimaBag);
			}
			
			
		}
		
		
		
		
	}
	
	
	
	
	
	/**
	 * This function finds the minimum in a set of elements and returns
	 * the value of that minimum, as well as it's location
	 * @param list_input
	 * @param min_out
	 * @param index_out
	 */
	public static int getMinimumAndIndex(ArrayList<Integer> list_input){
		
		//initialize the output to something exotic
		int min_out = Integer.MAX_VALUE;
		int index_out = -1;
		
		int zero_count = 0; //counts how many times we've seen zero
		
		for(int i=0; i<list_input.size(); i++){
			
			//in case a list that 2 total minima (of the same low number, but in other places)
			//we arbitrarily pick the first one.
			//this algorithm will run again, after the currently selected minima (one of the 2)
			//has been set to MAX_INT, so the other minima (of the 2) will have it's chance
			if(list_input.get(i)<min_out){
				
				//we found our new minimum
				min_out = list_input.get(i);
				index_out = i;				
			}	
			
		}
		
		return(index_out);
	}
	
	
	
	
	/**
	 * This function is the same as above for black/white pictures.
	 * The difference is that it finds the first zero it can get it's hands on
	 * then it counts how many consecutive zeros it finds and it returns the mean point between 
	 * the first and last encountered zero
	 * @param list_input
	 * @param min_out
	 * @param index_out
	 */
	public static int getMinimumAndIndexBW(ArrayList<Integer> list_input){
		
		//initialize the output to something exotic
		int min_out = Integer.MAX_VALUE;
		int index_out = -1;
		
		
		for(int i=0; i<list_input.size(); i++){
			
			//in case a list that 2 total minima (of the same low number, but in other places)
			//we arbitrarily pick the first one.
			//this algorithm will run again, after the currently selected minima (one of the 2)
			//has been set to MAX_INT, so the other minima (of the 2) will have it's chance
			if(list_input.get(i)<min_out){
				
				min_out = list_input.get(i);
				index_out = i;
				
				if(min_out==0){//nothing can go lower than zero
						
					int originalZero = i;
					int lastZero = i;
					
					//we found our first zero, now count consequtive zeros
					for(;i<list_input.size(); i++){
						if(list_input.get(i)==0){
							lastZero++;
						}
						else
							break;
					}
					index_out = Math.round((lastZero-originalZero)/2)+originalZero;			
					break;	
				}
			}	
			
		}
		return(index_out);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
