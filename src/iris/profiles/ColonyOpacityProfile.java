/**
 * 
 */
package iris.profiles;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ImageConverter;
import iris.imageCroppers.GenericImageCropper;
import iris.imageCroppers.NaiveImageCropper3;
import iris.imageSegmenterInput.BasicImageSegmenterInput;
import iris.imageSegmenterOutput.BasicImageSegmenterOutput;
import iris.imageSegmenters.ColonyBreathing;
import iris.imageSegmenters.RisingTideSegmenter;
import iris.settings.BasicSettings;
import iris.settings.UserSettings.ProfileSettings;
import iris.tileReaderInputs.OpacityTileReaderInput;
import iris.tileReaderOutputs.OpacityTileReaderOutput;
import iris.tileReaders.OpacityTileReader;
import iris.ui.IrisFrontend;
import iris.utils.Toolbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This profile is calibrated for use in measuring the colony sizes of E. coli or Salmonella 1536 plates
 * 
 * @author George Kritikos
 *
 */
public class ColonyOpacityProfile extends Profile {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	static String profileName = "Colony growth";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile is calibrated for use in measuring colony size, density, and opacity";


	/**
	 * This holds access to the settings object
	 */
	private BasicSettings settings = new BasicSettings(IrisFrontend.settings);

	/**
	 * This function will analyze the picture using the basic profile
	 * The end result will be a file with the same name as the input filename,
	 * after the addition of a .iris ending
	 * @param filename
	 */
	public void analyzePicture(String filename){

		File file = new File(filename);
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

		//initialize results file output
		StringBuffer output = new StringBuffer();
		output.append("#Iris output\n");
		output.append("#Profile: " + profileName + "\n");
		output.append("#Iris version: " + IrisFrontend.IrisVersion + ", revision id: " + IrisFrontend.IrisBuild + "\n");
		output.append("#"+filename+"\n");


		//1. open the image file, and check if it was opened correctly
		ImagePlus originalImage = IJ.openImage(filename);

		//check that file was opened successfully
		if(originalImage==null){
			//TODO: warn the user that the file was not opened successfully
			System.err.println("Could not open image file: " + filename);
			return;
		}


		//find any user settings pertaining to this profile
		ProfileSettings userProfileSettings = null;
		if(IrisFrontend.userSettings!=null){
			userProfileSettings = IrisFrontend.userSettings.getProfileSettings(profileName);
		}

		//set flag to honour a possible user-set ROI
		if(filename.contains("colony_")){
			IrisFrontend.singleColonyRun=true;
			settings.numberOfColumnsOfColonies=1;
			settings.numberOfRowsOfColonies=1;
			IrisFrontend.settings.userDefinedRoi=true; //doesn't hurt to re-set it
			originalImage.setRoi(new OvalRoi(0,0,originalImage.getWidth(),originalImage.getHeight()));
		}
		else if(filename.contains("tile_")){
			IrisFrontend.singleColonyRun=true;
			settings.numberOfColumnsOfColonies=1;
			settings.numberOfRowsOfColonies=1;
			IrisFrontend.settings.userDefinedRoi=false; //doesn't hurt to re-set it
			originalImage.setRoi(new Roi(0,0,originalImage.getWidth(),originalImage.getHeight()));
		}



		//
		//--------------------------------------------------
		//
		//

		//2. rotate the whole image
		double imageAngle = 0;
		if(userProfileSettings==null || IrisFrontend.singleColonyRun){ 
			//if no settings loaded
			//or if this is a single colony image
			imageAngle = Toolbox.calculateImageRotation(originalImage);
		}
		else if(userProfileSettings.rotationSettings.autoRotateImage){
			imageAngle = Toolbox.calculateImageRotation(originalImage);
		}
		else if(!userProfileSettings.rotationSettings.autoRotateImage){
			imageAngle = userProfileSettings.rotationSettings.manualImageRotationDegrees;
		}

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

		//3. crop the plate to keep only the colonies
		ImagePlus croppedImage = null;

		if(userProfileSettings==null){ //default behavior
			croppedImage = GenericImageCropper.cropPlate(rotatedImage);
		}
		else if(userProfileSettings.croppingSettings.UserCroppedImage || IrisFrontend.singleColonyRun){
			//perform no cropping if the user already cropped the picture
			//or if this is a single-colony picture
			croppedImage = rotatedImage.duplicate();
			croppedImage.setRoi(rotatedImage.getRoi());
		}
		else if(userProfileSettings.croppingSettings.UseFixedCropping){
			int x_start = userProfileSettings.croppingSettings.FixedCropping_X_Start;
			int x_end = userProfileSettings.croppingSettings.FixedCropping_X_End;
			int y_start = userProfileSettings.croppingSettings.FixedCropping_Y_Start;
			int y_end = userProfileSettings.croppingSettings.FixedCropping_Y_End;

			NaiveImageCropper3.keepOnlyColoniesROI = new Roi(x_start, y_start, x_end, y_end);
			croppedImage = NaiveImageCropper3.cropPlate(rotatedImage);
		}
		else if(!userProfileSettings.croppingSettings.UseFixedCropping){
			croppedImage = GenericImageCropper.cropPlate(rotatedImage);
		}

		//flush the original picture, we won't be needing it anymore
		rotatedImage.flush();




		//
		//--------------------------------------------------
		//
		//

		//4. pre-process the picture (i.e. make it grayscale)
		ImagePlus colourCroppedImage = croppedImage.duplicate();
		colourCroppedImage.setRoi(croppedImage.getRoi());

		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();


		//
		//--------------------------------------------------
		//
		//


		calculateGridSpacing(settings, croppedImage);

		//5. segment the cropped picture
		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(croppedImage, settings);
		BasicImageSegmenterOutput segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);


		//let the tile boundaries "breathe"
		if(userProfileSettings==null){//default behavior
			ColonyBreathing.breathingSpace = 8;
			segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);

		}
		else if(userProfileSettings.segmentationSettings.ColonyBreathing){
			ColonyBreathing.breathingSpace = userProfileSettings.segmentationSettings.ColonyBreathingSpace;
			segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);
		}

		

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
			ImagePlus paintedImage = ColonyBreathing.paintSegmentedImage(croppedImage, segmentationOutput); //calculate grid image
			Toolbox.savePicture(paintedImage, filename + ".grid.jpg");

			return;
		}

		//6. colony breathing
		segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);


		int x = segmentationOutput.getTopLeftRoi().getBounds().x;
		int y = segmentationOutput.getTopLeftRoi().getBounds().y;
		output.append("#top left of the grid found at (" +x+ " , " +y+ ")\n");

		x = segmentationOutput.getBottomRightRoi().getBounds().x;
		y = segmentationOutput.getBottomRightRoi().getBounds().y;
		output.append("#bottom right of the grid found at (" +x+ " , " +y+ ")\n");




		//
		//--------------------------------------------------
		//
		//
		
		//retrieve the user-defined detection thresholds
		float minimumValidColonyCircularity;
		try{minimumValidColonyCircularity = userProfileSettings.detectionSettings.MinimumValidColonyCircularity;} 
		catch(Exception e) {minimumValidColonyCircularity = (float)0.3;}

		int minimumValidColonySize;
		try{minimumValidColonySize = userProfileSettings.detectionSettings.MinimumValidColonySize;} 
		catch(Exception e) {minimumValidColonySize = 50;}

		

		//7. analyze each tile

		//create an array of measurement outputs
		OpacityTileReaderOutput [][] readerOutputs = new OpacityTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				readerOutputs[i][j] = OpacityTileReader.processTile(
						new OpacityTileReaderInput(croppedImage, segmentationOutput.ROImatrix[i][j], settings));

				//each generated tile image is cleaned up inside the tile reader
				
				//colony QC
				if(readerOutputs[i][j].colonySize<minimumValidColonySize ||
						readerOutputs[i][j].circularity<minimumValidColonyCircularity){
					readerOutputs[i][j] = new OpacityTileReaderOutput();
				}

				
			}
		}



		//check if a row or a column has most of it's tiles empty (then there was a problem with gridding)
		//check rows first
		if(checkRowsColumnsIncorrectGridding(readerOutputs)){
			//something was wrong with the gridding.
			//just print an error message, save grid for debugging reasons and exit
			System.err.println("\n"+profileName+": unable to process picture " + justFilename);
			System.err.print("Image segmentation algorithm failed:\n");
			System.err.println("\ttoo many empty rows/columns");

			//calculate and save grid image
			Toolbox.drawColonyBounds(colourCroppedImage, segmentationOutput, readerOutputs);
			Toolbox.savePicture(colourCroppedImage, filename + ".grid.jpg");

			return;
		}




		//8. output the results

		//8.1 output the colony measurements as a text file
		output.append("row\tcolumn\tsize\tcircularity\topacity\n");
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
						+ Integer.toString(readerOutputs[i][j].colonySize) + "\t"
						+ String.format("%.3f", readerOutputs[i][j].circularity) + "\t"
						+ Integer.toString(readerOutputs[i][j].opacity) + "\n");
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



		//8.2 save any intermediate picture files, if requested
		settings.saveGridImage = true;
		if(settings.saveGridImage){
			//calculate grid image
			colourCroppedImage = ColonyBreathing.paintSegmentedImage(colourCroppedImage, segmentationOutput);
			Toolbox.drawColonyBounds(colourCroppedImage, segmentationOutput, readerOutputs);
			Toolbox.savePicture(colourCroppedImage, filename + ".grid.jpg");
		}

	}


	/**
	 * This function calculates the minimum and maximum grid distances according to the
	 * cropped image size and
	 * the number of rows and columns that need to be found.
	 * Since the cropped image needs to be segmented roughly in equal distances, the
	 * nominal distance in which the columns will be spaced apart will be
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
	private void calculateGridSpacing(BasicSettings settings_,
			ImagePlus croppedImage) {

		int image_width = croppedImage.getWidth();
		float nominal_width = image_width / settings_.numberOfColumnsOfColonies;

		//save the results directly to the settings object
		settings_.minimumDistanceBetweenRows = Math.round(nominal_width*2/3);
		settings_.maximumDistanceBetweenRows = Math.round(nominal_width*3/2);

	}


	/**
	 * This function will check if there is any row or any column with more than half of it's tiles being empty.
	 * If so, it will return true. If everything is ok, it will return false.
	 * @param readerOutputs
	 * @return
	 */
	private boolean checkRowsColumnsIncorrectGridding(
			OpacityTileReaderOutput[][] readerOutputs) {

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
