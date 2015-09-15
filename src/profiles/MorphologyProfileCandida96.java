/**
 * 
 */
package profiles;

import gui.IrisFrontend;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageConverter;
import imageCroppers.NaiveImageCropper3;
import imageSegmenterInput.BasicImageSegmenterInput;
import imageSegmenterOutput.BasicImageSegmenterOutput;
import imageSegmenters.ColonyBreathing;
import imageSegmenters.SimpleImageSegmenter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import settings.BasicSettings;
import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.MorphologyTileReaderOutput;
import tileReaders.MorphologyTileReader;
import utils.Toolbox;

/**
 * This profile is calibrated for use in measuring the colony sizes of E. coli or Salmonella 1536 plates
 * 
 * @author George Kritikos
 *
 */
public class MorphologyProfileCandida96 extends Profile {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "Morphology Profile [Candida 96-plates]";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile quantifies the amount of colony structure (how 'wrinkly' a colony is)";


	/**
	 * This holds access to the settings object
	 */
	public BasicSettings settings = new BasicSettings(IrisFrontend.settings);


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
		//
		//--------------------------------------------------
		//
		//


		//2. rotate the whole image
//		double imageAngle = Toolbox.calculateImageRotation(originalImage);
//
//		//create a copy of the original image and rotate it, then clear the original picture
//		ImagePlus rotatedImage = Toolbox.rotateImage(originalImage, imageAngle);
//		originalImage.flush();
//
//		//output how much the image needed to be rotated
//		if(imageAngle!=0){
//			System.out.println("Image had to be rotated by  " + imageAngle + " degrees");
//		}
		
		
		ImagePlus rotatedImage = originalImage.duplicate();
		originalImage.flush();


		//
		//--------------------------------------------------
		//
		//


		//3. crop the plate to keep only the colonies
		//NaiveImageCropper.keepOnlyColoniesROI = new Roi(480, 520/*580*/, 3330, 2220);
		//ImagePlus croppedImage = NaiveImageCropper.cropPlate(rotatedImage);
//		GenericImageCropper.plateBorderSearchAreaRows = 50;
//		GenericImageCropper.plateBorderSearchAreaColumns = 100;
//		GenericImageCropper.searchStart = 0.035;
//		GenericImageCropper.searchEnd = 0.065;
//		GenericImageCropper.skip = 20;
		NaiveImageCropper3.keepOnlyColoniesROI = new Roi(435, 290, 4200, 2800);
		ImagePlus croppedImage = NaiveImageCropper3.cropPlate(rotatedImage);

		
		ImagePlus colorCroppedImage = croppedImage.duplicate(); //it's already rotated
		//flush the rotated picture, we won't be needing it anymore
		rotatedImage.flush();



		//
		//--------------------------------------------------
		//
		//

		//4. pre-process the picture (i.e. make it grayscale)
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();


		//
		//--------------------------------------------------
		//
		//


		//5. segment the cropped picture
		//first change the settings, to get a 96 plate segmentation
		settings.numberOfRowsOfColonies = 8;
		settings.numberOfColumnsOfColonies = 12;
		SimpleImageSegmenter.offset = 30;
		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(croppedImage, settings);
		BasicImageSegmenterOutput segmentationOutput = SimpleImageSegmenter.segmentPicture(segmentationInput);

		//let the tile boundaries "breathe"
		ColonyBreathing.breathingSpace = 40;//20;
		segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);
		
//		ColonyBreathing.paintSegmentedImage(croppedImage, segmentationOutput); //calculate grid image
//		croppedImage.show();
//		croppedImage.hide();copyOfTileImage		//check if something went wrong
		if(segmentationOutput.errorOccurred){

			System.err.println("\nOpacity profile: unable to process picture " + justFilename);

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
			croppedImage.flush();
			return;
		}

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

		//6. analyze each tile

		//create an array of measurement outputs
		MorphologyTileReaderOutput [][] readerOutputs = new MorphologyTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				readerOutputs[i][j] = MorphologyTileReader.processTile(
						new OpacityTileReaderInput(croppedImage, segmentationOutput.ROImatrix[i][j], settings));

				//each generated tile image is cleaned up inside the tile reader
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

			/*
			 * HACK: just carry on outputting an iris file, since we know that there's pictures with many
			 * empty spots 
			 * 
			//calculate and save grid image
			ColonyBreathing.paintSegmentedImage(croppedImage, segmentationOutput);
			Toolbox.savePicture(croppedImage, filename + ".grid.jpg");

			return;
			*/
			
			System.err.println("\tWarning: writing iris file anyway");
		}


		//7. output the results

		//7.1 output the colony measurements as a text file
		output.append("row\tcolumn\tsize\tcircularity\tmorphology score\tnormalized morphology score\n");
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
						+ Integer.toString(readerOutputs[i][j].colonySize) + "\t"
						+ String.format("%.3f", readerOutputs[i][j].circularity) + "\t"
						+ Integer.toString(readerOutputs[i][j].morphologyScore) + "\t"
						+ Integer.toString(readerOutputs[i][j].normalizedMorphologyScore) + "\n");
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
			
			//draw the colony bounds
			Toolbox.drawColonyBounds(colorCroppedImage, segmentationOutput, readerOutputs);
			
			//calculate grid image
			ImagePlus paintedImage = ColonyBreathing.paintSegmentedImage(colorCroppedImage, segmentationOutput);
			
			Toolbox.savePicture(paintedImage, filename + ".grid.jpg");
		}

	}


	/**
	 * This function will check if there is any row or any column with more than half of it's tiles being empty.
	 * If so, it will return true. If everything is ok, it will return false.
	 * @param readerOutputs
	 * @return
	 */
	private boolean checkRowsColumnsIncorrectGridding(
			MorphologyTileReaderOutput[][] readerOutputs) {

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
