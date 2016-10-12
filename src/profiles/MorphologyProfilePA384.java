/**
 * 
 */
package profiles;

import gui.IrisFrontend;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import imageCroppers.NaiveImageCropper3;
import imageSegmenterInput.BasicImageSegmenterInput;
import imageSegmenterOutput.BasicImageSegmenterOutput;
import imageSegmenters.ColonyBreathing;
import imageSegmenters.SimpleImageSegmenter;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import settings.ColorSettings;
import tileReaderInputs.BasicTileReaderInput;
import tileReaderInputs.ColorTileReaderInput;
import tileReaderInputs.ColorTileReaderInput3;
import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaderOutputs.ColorTileReaderOutput;
import tileReaderOutputs.MorphologyTileReaderOutput;
import tileReaderOutputs.OpacityTileReaderOutput;
import tileReaders.BasicTileReaderHSB;
import tileReaders.ColorTileReaderHSB;
import tileReaders.LaplacianFilterTileReader;
import tileReaders.MorphologyTileReader;
import tileReaders.OpacityTileReader;
import utils.Toolbox;

/**
 * This profile is calibrated for use in measuring the colony sizes of E. coli or Salmonella 1536 plates
 * 
 * @author George Kritikos
 *
 */
public class MorphologyProfilePA384 extends Profile {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "Morphology Profile [Pseudomonas 384-plates]";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile quantifies the amount of colony structure (how 'wrinkly' a colony is)";


	/**
	 * This holds access to the settings object
	 */
	public ColorSettings settings = new ColorSettings(IrisFrontend.settings);





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
		ImagePlus originalImage;
		try {
			originalImage  = IJ.openImage(filename);
		} catch (Exception e) {
			originalImage = null;
		}		

		//check that file was opened successfully
		if(originalImage==null){
			//TODO: warn the user that the file was not opened successfully
			System.err.println("Could not open image file: " + filename);
			return;
		}


		//set flag to honour a possible user-set ROI
		if(IrisFrontend.singleColonyRun==true){
			if(filename.contains("colony_")){
				IrisFrontend.settings.userDefinedRoi=true; //doesn't hurt to re-set it
				originalImage.setRoi(new OvalRoi(0,0,originalImage.getWidth(),originalImage.getHeight()));
			}
			else if(filename.contains("tile_")){
				IrisFrontend.settings.userDefinedRoi=false; //doesn't hurt to re-set it
				originalImage.setRoi(new Roi(0,0,originalImage.getWidth(),originalImage.getHeight()));
			}
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
		rotatedImage.setRoi(originalImage.getRoi());
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



		//HACK for PA Ornithine screen
		NaiveImageCropper3.keepOnlyColoniesROI = new Roi(470, 330, 4140, 2750);
		ImagePlus croppedImage = NaiveImageCropper3.cropPlate(rotatedImage);


		ImagePlus colorCroppedImage = croppedImage.duplicate(); //it's already rotated
		colorCroppedImage.setRoi(croppedImage.getRoi());
		//flush the rotated picture, we won't be needing it anymore
		rotatedImage.flush();



		//
		//--------------------------------------------------
		//
		//

		//4. pre-process the picture (i.e. make it grayscale)
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();

		//4b. also make BW the input to the image segmenter
		ImagePlus BWimageToSegment = croppedImage.duplicate();
		BWimageToSegment.setRoi(croppedImage.getRoi());
		Toolbox.turnImageBW_Otsu_auto(BWimageToSegment);


		ImagePlus grayscaleCroppedImage = Toolbox.getHSBgrayscaleImageBrightness(colorCroppedImage);

		//get a copy of the picture thresholded using a local algorithm
		ImagePlus BW_local_thresholded_picture = Toolbox.turnImageBW_Local_auto_mean(grayscaleCroppedImage, 65);


		//
		//--------------------------------------------------
		//
		//


		//5. segment the cropped picture
		//first change the settings, to get a 96 plate segmentation
		if(IrisFrontend.singleColonyRun==false){
			settings.numberOfRowsOfColonies = 16;
			settings.numberOfColumnsOfColonies = 24;
		}

		SimpleImageSegmenter.offset = 10;

		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(BWimageToSegment, settings);
		//

		//		segmentationInput.settings.maximumDistanceBetweenRows = 500;
		//		segmentationInput.settings.minimumDistanceBetweenRows = 200;
		//		BasicImageSegmenterOutput segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);
		BasicImageSegmenterOutput segmentationOutput = SimpleImageSegmenter.segmentPicture_width(segmentationInput);

		//let the tile boundaries "breathe"
		//Edit: no, don't do 100, doesn't play well with Stm readout
		//		ColonyBreathing.breathingSpace = 100;//20;
		//		segmentationInput = new BasicImageSegmenterInput(croppedImage.duplicate(), settings);
		//		segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);

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
			BWimageToSegment.flush();
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

		//precalculate the colony centers
		ColorTileReaderInput [][] colonyCenteredInput = Toolbox.precalculateColonyCenters(colorCroppedImage, segmentationOutput, settings);

		//6. analyze each tile

		//create an array of measurement outputs
		BasicTileReaderOutput[][] basicTileReaderOutputs = new BasicTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		OpacityTileReaderOutput[][] opacityTileReaderOutputs = new OpacityTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		MorphologyTileReaderOutput [][] morphologyReaderOutputs = new MorphologyTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		ColorTileReaderOutput [][] colorReaderOutputs = new ColorTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				try{
					
					basicTileReaderOutputs[i][j] = BasicTileReaderHSB.processTile(
							new BasicTileReaderInput(BW_local_thresholded_picture, segmentationOutput.ROImatrix[i][j], settings)); 

					//try once more using the other
					if(basicTileReaderOutputs[i][j].colonySize==0){
						basicTileReaderOutputs[i][j] = LaplacianFilterTileReader.processTile(
								new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
					}


					//set the known colony centers
					if(colonyCenteredInput[i][j].colonyCenter!=null)
						basicTileReaderOutputs[i][j].colonyCenter = new Point(colonyCenteredInput[i][j].colonyCenter);



					//only get morphology and color if there was a colony there
					if(basicTileReaderOutputs[i][j].colonySize>0){

						//get morphology for that colony
						morphologyReaderOutputs[i][j] = MorphologyTileReader.processDefinedColonyTile(
								new OpacityTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], 
										basicTileReaderOutputs[i][j].colonyROI, basicTileReaderOutputs[i][j].colonySize, settings));

						//get color for that colony
						colorReaderOutputs[i][j] = ColorTileReaderHSB.processDefinedColonyTile(
								new ColorTileReaderInput3(colorCroppedImage, segmentationOutput.ROImatrix[i][j], morphologyReaderOutputs[i][j].colonyROI, 
										morphologyReaderOutputs[i][j].colonySize, morphologyReaderOutputs[i][j].colonyCenter, settings));

						//opacity -- to check if colony darkness correlates with colour information -- true means opacities can get negative
						//this is a fix for very dark colonies
						opacityTileReaderOutputs[i][j] = OpacityTileReader.processDefinedColonyTile(
								new OpacityTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], 
										basicTileReaderOutputs[i][j].colonyROI, basicTileReaderOutputs[i][j].colonySize, settings), true);


					} else {
						basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
						opacityTileReaderOutputs[i][j] = new OpacityTileReaderOutput();
						morphologyReaderOutputs[i][j] = new MorphologyTileReaderOutput();
						colorReaderOutputs[i][j] = new ColorTileReaderOutput();
					}
				}

				catch(Exception e){
					System.err.print("\tError getting morphology at tile "+ Integer.toString(i+1) +" "+ Integer.toString(j+1) + "\n");
					basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
					morphologyReaderOutputs[i][j] = new MorphologyTileReaderOutput();
					colorReaderOutputs[i][j] = new ColorTileReaderOutput();
				}

				//each generated tile image is cleaned up inside the tile reader
			}
		}



		//check if a row or a column has most of it's tiles empty (then there was a problem with gridding)
		//check rows first
		if(checkRowsColumnsIncorrectGridding(morphologyReaderOutputs)){
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
		//output.append("row\tcolumn\tcolony size\tcolony circularity\tcolony morphology score\tcolony normalized morphology score\t\n");
		output.append("row\t" +
				"column\t" +
				"colony size\t" +
				"colony circularity\t" +
				"colony opacity\t" +
				"morphology score fixed circles\t" +
				"morphology score whole colony\t" +
				"normalized morphology score\t" +
				"in agar size\t" +
				"in agar circularity\t" +
				"in agar opacity\t" + 
				"whole tile opacity\t" +
				"colony color intensity\t" +
				"biofilm area size\t" +
				"biofilm color intensity\t" +
				"size normalized color intensity\t" +
				"brightness corrected size normalized color intensity\n");

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				
				double brightnessCorrectedSizeNormalizedColorIntensity = 0;
				if(basicTileReaderOutputs[i][j].colonySize>0)
					brightnessCorrectedSizeNormalizedColorIntensity = colorReaderOutputs[i][j].relativeColorIntensity * 0.5 + opacityTileReaderOutputs[i][j].opacity /basicTileReaderOutputs[i][j].colonySize; 
				
				output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
						+ Integer.toString(basicTileReaderOutputs[i][j].colonySize) + "\t"
						+ String.format("%.3f", basicTileReaderOutputs[i][j].circularity) + "\t"
						+ Integer.toString(opacityTileReaderOutputs[i][j].opacity) + "\t"
						+ Integer.toString(morphologyReaderOutputs[i][j].morphologyScoreFixedNumberOfCircles) + "\t"
						+ Integer.toString(morphologyReaderOutputs[i][j].morphologyScoreWholeColony) + "\t"
						+ String.format("%.3f", morphologyReaderOutputs[i][j].normalizedMorphologyScore) + "\t"
						+ Integer.toString(morphologyReaderOutputs[i][j].inAgarSize) + "\t"
						+ String.format("%.3f", morphologyReaderOutputs[i][j].inAgarCircularity) + "\t"
						+ Integer.toString(morphologyReaderOutputs[i][j].inAgarOpacity) + "\t"
						+ Integer.toString(morphologyReaderOutputs[i][j].wholeTileOpacity) + "\t"
						+ Integer.toString(colorReaderOutputs[i][j].colorIntensitySum) + "\t"
						+ Integer.toString(colorReaderOutputs[i][j].biofilmArea) + "\t"
						+ Integer.toString(colorReaderOutputs[i][j].colorIntensitySumInBiofilmArea) + "\t"
						+ String.format("%.3f", colorReaderOutputs[i][j].relativeColorIntensity) + "\t"
						+ String.format("%.3f", brightnessCorrectedSizeNormalizedColorIntensity) + "\n");
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
		//settings.saveGridImage = true;
		//		if(settings.saveGridImage){
		//			
		//			//draw the colony bounds, and the in-agar growth bounds
		//			//Toolbox.drawColonyBounds(colorCroppedImage, segmentationOutput, readerOutputs);
		//			drawInAgarGrowthBounds(colorCroppedImage, segmentationOutput, readerOutputs);
		//			Toolbox.drawColonyRoundBounds(colorCroppedImage, segmentationOutput, readerOutputs);
		//			//calculate grid image
		//			ImagePlus paintedImage = ColonyBreathing.paintSegmentedImage(colorCroppedImage, segmentationOutput);
		//			
		//			Toolbox.savePicture(paintedImage, filename + ".grid.jpg");
		//		}
		//		
		//7.2 save any intermediate picture files, if requested
		settings.saveGridImage = true;
		if(settings.saveGridImage){

			//draw the colony bounds, and the in-agar growth bounds
			Toolbox.drawColonyBounds(colorCroppedImage, segmentationOutput, morphologyReaderOutputs);
			drawInAgarGrowthBounds(colorCroppedImage, segmentationOutput, morphologyReaderOutputs);

			//calculate grid image
			ImagePlus paintedImage = ColonyBreathing.paintSegmentedImage(colorCroppedImage, segmentationOutput);

			Toolbox.savePicture(paintedImage, filename + ".grid.jpg");
		}

	}



	/**
	 * This function will use the ROI information in each TileReader to get the colony bounds on the picture, with
	 * offsets found in the segmenterOutput.  
	 * @param segmentedImage
	 * @param segmenterOutput
	 */
	public static void drawInAgarGrowthBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmenterOutput, MorphologyTileReaderOutput [][] tileReaderOutputs){


		//first, get all the colony bounds into byte processors (one for each tile, having the exact tile size)
		ByteProcessor[][] colonyBounds = getInAgarGrowthBounds(croppedImage, segmenterOutput, tileReaderOutputs);


		//paint those bounds on the original cropped image
		ImageProcessor bigPictureProcessor = croppedImage.getProcessor();
		//bigPictureProcessor.setColor(Color.black);
		bigPictureProcessor.setColor(Color.red);
		bigPictureProcessor.setLineWidth(1);


		//for all rows
		for(int i=0; i<tileReaderOutputs.length; i++){
			//for all columns
			for(int j=0; j<tileReaderOutputs[0].length; j++) {

				if(tileReaderOutputs[i][j].colonySize==0)
					continue; //don't go through the trouble for emtpy tiles

				//get tile offsets
				int tile_y_offset = segmenterOutput.ROImatrix[i][j].getBounds().y;
				int tile_x_offset = segmenterOutput.ROImatrix[i][j].getBounds().x;
				int tileWidth = segmenterOutput.ROImatrix[i][j].getBounds().width;
				int tileHeight = segmenterOutput.ROImatrix[i][j].getBounds().height;


				//for each pixel, if it is colony bounds, paint it on the big picture
				for(int x=0; x<tileWidth; x++){
					for(int y=0; y<tileHeight; y++){
						if(colonyBounds[i][j].getPixel(x, y)==255){ //it is a colony bounds pixel
							bigPictureProcessor.drawDot(x+tile_x_offset, y+tile_y_offset); //paint it on the big picture
						}
					}
				}

			}

		}
	}


	/**
	 * This function will get original picture, segment it into tiles.
	 * For each one, it will apply the colony ROI on it (except it it was empty -- add an empty ROI).
	 * Then, it will get the mask from the ROI and find it's bounds.
	 * At the end, for each original tile, we'll have 0/1 tiles, with 1s where the colony bounds are.
	 * @param croppedImage
	 * @param segmenterOutput
	 * @param colonyRoi
	 * @return
	 */
	public static ByteProcessor[][] getInAgarGrowthBounds(ImagePlus croppedImage, BasicImageSegmenterOutput segmentationOutput, MorphologyTileReaderOutput [][] tileReaderOutputs){

		ByteProcessor[][] colonyBounds = new ByteProcessor[tileReaderOutputs.length][tileReaderOutputs[0].length];

		//for all rows
		for(int i=0;i<tileReaderOutputs.length; i++){
			//for all columns
			for (int j = 0; j<tileReaderOutputs[0].length; j++) {

				//get the tile
				croppedImage.setRoi(segmentationOutput.ROImatrix[i][j]);
				croppedImage.copy(false);
				ImagePlus tile = ImagePlus.getClipboard();


				//apply the ROI, get the mask
				ImageProcessor tileProcessor = tile.getProcessor();
				tileProcessor.setRoi(tileReaderOutputs[i][j].inAgarROI);

				tileProcessor.setColor(Color.white);
				tileProcessor.setBackgroundValue(0);
				tileProcessor.fill(tileProcessor.getMask());
				//tileProcessor.fill(tileReaderOutputs[i][j].colonyROI.getMask());

				//get the bounds of the mask, that's it, save it
				tileProcessor.findEdges();
				colonyBounds[i][j] = (ByteProcessor) tileProcessor.convertToByte(true);		


			}
		}

		croppedImage.deleteRoi();

		return(colonyBounds);
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
