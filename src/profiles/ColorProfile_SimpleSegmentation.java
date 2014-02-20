/**
 * 
 */
package profiles;

import gui.IrisFrontend;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import imageCroppers.NaiveImageCropper;
import imageSegmenterInput.BasicImageSegmenterInput;
import imageSegmenterOutput.BasicImageSegmenterOutput;
import imageSegmenters.RisingTideSegmenter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import settings.ColorSettings;
import tileReaderInputs.BasicTileReaderInput;
import tileReaderInputs.ColorTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaderOutputs.ColorTileReaderOutput;
import tileReaders.BasicTileReader;
import tileReaders.ColorTileReader;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class ColorProfile_SimpleSegmentation extends Profile{
	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "Colour Profile - Simple Grid";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile measures biofilm formation in Coomasie blue - Congo red 1536 plates using just a simple grid";


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

		File file = new File(filename);
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

		//initialize results file output
		StringBuffer output = new StringBuffer();
		output.append("Iris output\n");
		output.append("Profile: " + profileName + "\n");
		output.append("Iris version: " + IrisFrontend.IrisVersion + ", revision id: " + IrisFrontend.IrisBuild + "\n");
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

		//2. crop the plate to keep only the colonies
		ImagePlus croppedImage = NaiveImageCropper.cropPlate(originalImage);

		//flush the original picture, we won't be needing it anymore
		originalImage.flush();




		//
		//--------------------------------------------------
		//
		//

		//3. pre-process the picture (i.e. make it grayscale), but keep a copy so that we have the colour information
		ImagePlus colourCroppedImage = croppedImage.duplicate();
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();


		//
		//--------------------------------------------------
		//
		//

		//4. segment the cropped picture
		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(croppedImage, settings);
		BasicImageSegmenterOutput segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);

		//check if something went wrong
		if(segmentationOutput.errorOccurred){

			System.err.println("\nColor profile: unable to process picture " + justFilename);

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
			Toolbox.savePicture(croppedImage, filename + ".grid.jpg");

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

		//5. analyze each tile

		//create an array of measurement outputs
		BasicTileReaderOutput [][] basicTileReaderOutputs = new BasicTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		ColorTileReaderOutput [][] colourTileReaderOutputs = new ColorTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				//first get the colony size (so that the user doesn't have to run 2 profiles for this)
				basicTileReaderOutputs[i][j] = BasicTileReader.processTile(
						new BasicTileReaderInput(croppedImage, segmentationOutput.ROImatrix[i][j], settings));

				//only run the color analysis if there is a colony in the tile
				if(basicTileReaderOutputs[i][j].colonySize>0){
					colourTileReaderOutputs[i][j] = ColorTileReader.processTile(
							new ColorTileReaderInput(colourCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
				}
				else{
					colourTileReaderOutputs[i][j] = new ColorTileReaderOutput();
					colourTileReaderOutputs[i][j].biofilmArea=0;
					colourTileReaderOutputs[i][j].colorIntensitySum=0;
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
			Toolbox.savePicture(croppedImage, filename + ".grid.jpg");

			
			//HACK for Lucia, normally the next line is not commented
			//return;
			//HACK for Lucia end
		}




		//6. output the results

		//6.1 output the colony measurements as a text file
		output.append("row\t" +
				"column\t" +
				"colony size\t" +
				"circularity\t" +
				"colony color intensity\t" +
				"biofilm area size\t" +
				"biofilm color intensity\t" +
				"biofilm area ratio\n");
		
		
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				//calculate the ratio of biofilm size (in pixels) to colony size
				float biofilmAreaRatio = 0;
				if(basicTileReaderOutputs[i][j].colonySize!=0){
					biofilmAreaRatio = (float)colourTileReaderOutputs[i][j].biofilmArea / (float)basicTileReaderOutputs[i][j].colonySize;
				}


				output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
						+ Integer.toString(basicTileReaderOutputs[i][j].colonySize) + "\t"
						+ String.format("%.3f", basicTileReaderOutputs[i][j].circularity) + "\t"
						+ Integer.toString(colourTileReaderOutputs[i][j].colorIntensitySum) + "\t"
						+ Integer.toString(colourTileReaderOutputs[i][j].biofilmArea) + "\t"
						+ Integer.toString(colourTileReaderOutputs[i][j].colorIntensitySumInBiofilmArea) + "\t"
						+ String.format("%.3f", biofilmAreaRatio) + "\n");
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



		//6.2 save any intermediate picture files, if requested
		settings.saveGridImage = true;
		if(settings.saveGridImage){
			//calculate grid image
			RisingTideSegmenter.paintSegmentedImage(croppedImage, segmentationOutput);
			Toolbox.savePicture(croppedImage, filename + ".grid.jpg");
		}

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




