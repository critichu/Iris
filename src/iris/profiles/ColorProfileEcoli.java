/**
 * 
 */
package iris.profiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import fiji.threshold.Auto_Local_Threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import iris.imageCroppers.GenericImageCropper;
import iris.imageCroppers.GenericImageCropper2;
import iris.imageCroppers.NaiveImageCropper;
import iris.imageCroppers.NaiveImageCropper3;
import iris.imageSegmenterInput.BasicImageSegmenterInput;
import iris.imageSegmenterOutput.BasicImageSegmenterOutput;
import iris.imageSegmenters.ColonyBreathing;
import iris.imageSegmenters.RisingTideSegmenter;
import iris.settings.ColorSettings;
import iris.settings.UserSettings.ProfileSettings;
import iris.tileReaderInputs.BasicTileReaderInput;
import iris.tileReaderInputs.ColorTileReaderInput3;
import iris.tileReaderInputs.OpacityTileReaderInput;
import iris.tileReaderOutputs.BasicTileReaderOutput;
import iris.tileReaderOutputs.ColorTileReaderOutput;
import iris.tileReaderOutputs.OpacityTileReaderOutput;
import iris.tileReaders.BasicTileReaderHSB;
import iris.tileReaders.BasicTileReaderHSB_darkColonies;
import iris.tileReaders.ColorTileReaderHSB;
import iris.tileReaders.LaplacianFilterTileReader;
import iris.tileReaders.OpacityTileReader;
import iris.ui.IrisFrontend;
import iris.utils.Toolbox;
/**
 * @author George Kritikos
 *
 */
public class ColorProfileEcoli extends Profile{
	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	private static String profileName = "Biofilm formation";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile measures biofilm formation in Coomasie blue - Congo red 1536 plates after converting the picture to HSB space";


	/**
	 * This holds access to the settings object
	 */
	private ColorSettings settings = new ColorSettings(IrisFrontend.settings);



	/**
	 * This function will analyze the picture using the basic profile
	 * The end result will be a file with the same name as the input filename,
	 * after the addition of a .iris ending
	 * @param filename
	 */
	public void analyzePicture(String filename){


		File file = new File(filename);
		String path = file.getParent();
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);
		//IrisFrontend.writeToLog("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

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


		//3. crop the plate to keep only the colonies

		ImagePlus colourCroppedImage = null;
		if(userProfileSettings==null){ //default behavior
			colourCroppedImage = GenericImageCropper2.cropPlate(rotatedImage);
		}
		else if(userProfileSettings.croppingSettings.UserCroppedImage || IrisFrontend.singleColonyRun){
			//perform no cropping if the user already cropped the picture
			//or if this is a single-colony picture
			colourCroppedImage = rotatedImage.duplicate();
			colourCroppedImage.setRoi(rotatedImage.getRoi());
		}
		else if(userProfileSettings.croppingSettings.UseFixedCropping){
			int x_start = userProfileSettings.croppingSettings.FixedCropping_X_Start;
			int x_end = userProfileSettings.croppingSettings.FixedCropping_X_End;
			int y_start = userProfileSettings.croppingSettings.FixedCropping_Y_Start;
			int y_end = userProfileSettings.croppingSettings.FixedCropping_Y_End;

			NaiveImageCropper3.keepOnlyColoniesROI = new Roi(x_start, y_start, x_end, y_end);
			colourCroppedImage = NaiveImageCropper3.cropPlate(rotatedImage);
		}
		else if(!userProfileSettings.croppingSettings.UseFixedCropping){
			colourCroppedImage = GenericImageCropper2.cropPlate(rotatedImage);
		}
		//flush the original pictures, we won't be needing them anymore
		rotatedImage.flush();
		originalImage.flush();




		//
		//--------------------------------------------------
		//
		//

		//4. pre-process the picture (i.e. make it grayscale), but keep a copy so that we have the colour information
		//This is how you do it the HSB way		
		ImagePlus grayscaleCroppedImage = Toolbox.getHSBgrayscaleImageBrightness(colourCroppedImage);

		//
		//--------------------------------------------------
		//
		//

		//5. segment the cropped picture
		BasicImageSegmenterInput segmentationInput = new BasicImageSegmenterInput(grayscaleCroppedImage, settings);
		BasicImageSegmenterOutput segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);


		//check if something went wrong with the segmenting process, then try out different croppers
		if(segmentationOutput.errorOccurred){
			//before giving up, try again with a different cropper (this is usually why the segmentation fails)

			colourCroppedImage = GenericImageCropper.cropPlate(rotatedImage);
			grayscaleCroppedImage = Toolbox.getHSBgrayscaleImageBrightness(colourCroppedImage);			
			segmentationInput = new BasicImageSegmenterInput(grayscaleCroppedImage, settings);
			segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);

		}

		if(segmentationOutput.errorOccurred){
			//before giving up, try again with a different cropper (this is usually why the segmentation fails)

			colourCroppedImage = NaiveImageCropper.cropPlate(rotatedImage);
			grayscaleCroppedImage = Toolbox.getHSBgrayscaleImageBrightness(colourCroppedImage);
			segmentationInput = new BasicImageSegmenterInput(grayscaleCroppedImage, settings);
			segmentationOutput = RisingTideSegmenter.segmentPicture(segmentationInput);

		}

		//check if something went wrong
		if(segmentationOutput.errorOccurred){

			System.err.println("\n"+ profileName +" profile: unable to process picture " + justFilename);

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
			ImagePlus croppedImageSegmented = grayscaleCroppedImage.duplicate();
			croppedImageSegmented.setRoi(grayscaleCroppedImage.getRoi());

			RisingTideSegmenter.paintSegmentedImage(colourCroppedImage, segmentationOutput); //calculate grid image
			Toolbox.savePicture(croppedImageSegmented, filename + ".grid.jpg");

			croppedImageSegmented.flush();
			grayscaleCroppedImage.flush();

			return;
		}


		//
		//--------------------------------------------------
		//
		//


		//6. colony breathing

		//let the tile boundaries "breathe"
		if(userProfileSettings==null){//default behavior
			segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);
		}
		else if(userProfileSettings.segmentationSettings.ColonyBreathing){
			ColonyBreathing.breathingSpace = userProfileSettings.segmentationSettings.ColonyBreathingSpace;
			segmentationOutput = ColonyBreathing.segmentPicture(segmentationOutput, segmentationInput);
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

		//retrieve the user-defined detection thresholds
		float minimumValidColonyCircularity;
		try{minimumValidColonyCircularity = userProfileSettings.detectionSettings.MinimumValidColonyCircularity;} 
		catch(Exception e) {minimumValidColonyCircularity = (float)0.4;}

		int minimumValidColonySize;
		try{minimumValidColonySize = userProfileSettings.detectionSettings.MinimumValidColonySize;} 
		catch(Exception e) {minimumValidColonySize = 50;}


		//6. analyze each tile

		//create an array of measurement outputs
		BasicTileReaderOutput [][] basicTileReaderOutputs = new BasicTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		ColorTileReaderOutput [][] colourTileReaderOutputs = new ColorTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];
		OpacityTileReaderOutput [][] opacityTileReaderOutputs = new OpacityTileReaderOutput[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				//first get the colony size using one method


				//v60 (or v66, it's the same)
				try{
					basicTileReaderOutputs[i][j] = BasicTileReaderHSB_darkColonies.processTile(
							new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
				} catch (Exception e){
					IrisFrontend.writeToLog(e.getStackTrace().toString());
					basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
				}
				//v60 end


				//v69 using Lucia's thresholds
				if(basicTileReaderOutputs[i][j].colonySize==0 || basicTileReaderOutputs[i][j].circularity<0.4){

					//System.out.println(filename + " " + Integer.toString(i+1) + " " + Integer.toString(j+1));

					try{
						basicTileReaderOutputs[i][j] = BasicTileReaderHSB.processTile(
								new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
					} catch (Exception e){
						IrisFrontend.writeToLog(e.getStackTrace().toString());
						basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
					}

					//if that didn't work, try the Laplacian Zero-crossings
					if(basicTileReaderOutputs[i][j].colonySize==0){
						try{
							basicTileReaderOutputs[i][j] = LaplacianFilterTileReader.processTile(
									new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));
						} catch(Exception e){
							IrisFrontend.writeToLog(e.getStackTrace().toString());
							basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
						}
					}

				}
				//v69 end


				/*
				 * for the record, this is how v0.9.6.1 worked
				basicTileReaderOutputs[i][j] = BasicTileReaderHSB.processTile(
						new BasicTileReaderInput(BW_local_thresholded_picture, segmentationOutput.ROImatrix[i][j], settings));

				//try once more using the other
				if(basicTileReaderOutputs[i][j].colonySize==0){
					//					basicTileReaderOutputs[i][j] = BasicTileReaderHSB_darkColonies.processTile(
					//							new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));

					basicTileReaderOutputs[i][j] = LaplacianFilterTileReader.processTile(
							new BasicTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], settings));

				}
				 */

				//colony QC -- user thresholds
				if(basicTileReaderOutputs[i][j].colonySize<minimumValidColonySize ||
						basicTileReaderOutputs[i][j].circularity<minimumValidColonyCircularity){
					basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
				}


				//only run the color analysis if there is a colony in the tile
				if(basicTileReaderOutputs[i][j].colonySize>0){
					//colour
					try{
						colourTileReaderOutputs[i][j] = ColorTileReaderHSB.processDefinedColonyTile(
								new ColorTileReaderInput3(colourCroppedImage, segmentationOutput.ROImatrix[i][j], 
										basicTileReaderOutputs[i][j].colonyROI, basicTileReaderOutputs[i][j].colonySize, 
										basicTileReaderOutputs[i][j].colonyCenter, settings));
					} catch(Exception e){
						IrisFrontend.writeToLog(e.getStackTrace().toString());
						colourTileReaderOutputs[i][j] = new ColorTileReaderOutput();
					}
					
					//opacity -- to check if colony darkness correlates with colour information -- true means opacities can get negative
					//this is a fix for very dark colonies
					try{
						opacityTileReaderOutputs[i][j] = OpacityTileReader.processDefinedColonyTile(
								new OpacityTileReaderInput(grayscaleCroppedImage, segmentationOutput.ROImatrix[i][j], 
										basicTileReaderOutputs[i][j].colonyROI, basicTileReaderOutputs[i][j].colonySize, settings), true);
					} catch(Exception e){
						IrisFrontend.writeToLog(e.getStackTrace().toString());
						opacityTileReaderOutputs[i][j] = new OpacityTileReaderOutput();
					}
				}
				else{
					colourTileReaderOutputs[i][j] = new ColorTileReaderOutput();
					colourTileReaderOutputs[i][j].biofilmArea=0;
					colourTileReaderOutputs[i][j].colorIntensitySum=0;
					colourTileReaderOutputs[i][j].meanSampleColor=0;

					opacityTileReaderOutputs[i][j] = new OpacityTileReaderOutput();
					opacityTileReaderOutputs[i][j].colonySize=0;
					opacityTileReaderOutputs[i][j].circularity=0;
					opacityTileReaderOutputs[i][j].opacity=0;
				}

				//colony QC -- remove colonies that are very close to the background
				if(opacityTileReaderOutputs[i][j].opacity<=0 ){//|| opacityTileReaderOutputs[i][j].max10percentOpacity<=0){
					basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
					colourTileReaderOutputs[i][j] = new ColorTileReaderOutput();
					opacityTileReaderOutputs[i][j] = new OpacityTileReaderOutput();
				}


				//each generated tile image is cleaned up inside the tile reader

			}
		}



		//6.2 -- double-check the results

		double minimumSizeNormalizedOpacityThreshold=4;
		double maximumDarkColonySizeNormalizedOpacityThreshold=-1.5;
		double minimumCircularityThreshold=0.25;
		boolean colony_flagged=false;
		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){
			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {
				colony_flagged=false;


				if(basicTileReaderOutputs[i][j].colonySize==0){
					continue; //colony is already empty
				}




				double SNO = (double)opacityTileReaderOutputs[i][j].opacity / (double)opacityTileReaderOutputs[i][j].colonySize;

				if( SNO < minimumSizeNormalizedOpacityThreshold 
						&& 
						basicTileReaderOutputs[i][j].circularity<minimumCircularityThreshold)
				{
					colony_flagged=true; 
					//discard the colony only if both criteria are met
					//see https://www.dropbox.com/s/mwkkkeppeqix5lt/Screenshot%202015-07-21%2015.29.34.png?dl=0
				}

				//second round of filtering
				if( SNO < 2 
						&& 
						basicTileReaderOutputs[i][j].circularity< 0.2)
				{
					colony_flagged=true; 
					//discard the colony only if both criteria are met
					//see https://www.dropbox.com/s/mwkkkeppeqix5lt/Screenshot%202015-07-21%2015.29.34.png?dl=0
				}

				//third round of filtering: remove colonies that are too close to the background
				//EDIT: ignore this if the user defined the colony
				if( SNO < 0.5 && SNO > -0.5 && !IrisFrontend.settings.userDefinedRoi)
				{
					colony_flagged=true;
				}


				//if we flagged the colony in any of the previous steps, remove it from the results
				if(colony_flagged){
					//System.err.println("Warning: removing colony "+ (i+1) + " " + (j+1) + " in file "+ justFilename);
					//System.err.println("Warning: removing colony "+ (i+1) + " " + (j+1) + " SNO= "+ SNO);
					basicTileReaderOutputs[i][j] = new BasicTileReaderOutput();
					basicTileReaderOutputs[i][j].colonySize=0;
					basicTileReaderOutputs[i][j].circularity=0;
					basicTileReaderOutputs[i][j].colonyROI = null;
					basicTileReaderOutputs[i][j].emptyTile=true;

					colourTileReaderOutputs[i][j] = new ColorTileReaderOutput();
					colourTileReaderOutputs[i][j].biofilmArea=0;
					colourTileReaderOutputs[i][j].colorIntensitySum=0;
					colourTileReaderOutputs[i][j].meanSampleColor=0;

					opacityTileReaderOutputs[i][j] = new OpacityTileReaderOutput();
					opacityTileReaderOutputs[i][j].colonySize=0;
					opacityTileReaderOutputs[i][j].circularity=0;
					opacityTileReaderOutputs[i][j].opacity=0;
					opacityTileReaderOutputs[i][j].max10percentOpacity=0;
				}

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
			ImagePlus croppedImageSegmented = colourCroppedImage.duplicate();
			croppedImageSegmented.setRoi(colourCroppedImage.getRoi());

			Toolbox.drawColonyBounds(colourCroppedImage, segmentationOutput, basicTileReaderOutputs);
			Toolbox.savePicture(colourCroppedImage, filename + ".grid.jpg");

			croppedImageSegmented.flush();
			grayscaleCroppedImage.flush();

			return;
		}




		//7. output the results

		//7.1 output the colony measurements as a text file
		output.append("row\t" +
				"column\t" +
				"colony size\t" +
				"circularity\t" +
				"colony color intensity\t" +
				"biofilm area size\t" +
				"biofilm color intensity\t" +
				"biofilm area ratio\t" +
				"size normalized color intensity\t" +
				"mean sampled color intensity\t" +
				"average pixel saturation\t" +
				"opacity\t" + 
				"max 10% opacity\n");


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
						+ String.format("%.3f", biofilmAreaRatio) + "\t"
						+ String.format("%.3f", colourTileReaderOutputs[i][j].relativeColorIntensity) + "\t" 
						+ String.format("%.3f", colourTileReaderOutputs[i][j].meanSampleColor) + "\t"
						+ String.format("%.3f", colourTileReaderOutputs[i][j].averagePixelSaturation) + "\t"
						+ Integer.toString(opacityTileReaderOutputs[i][j].opacity) + "\t"
						+ String.format("%.3f", opacityTileReaderOutputs[i][j].max10percentOpacity) + "\n");
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

			//TODO: need to make this function return a copy of the picture with the grid drawn on it

			Toolbox.drawColonyBounds(colourCroppedImage, segmentationOutput, basicTileReaderOutputs);
			//now paint also the tile bounds 
			//the original picture will be untouched
			colourCroppedImage = ColonyBreathing.paintSegmentedImage(colourCroppedImage, segmentationOutput);
			Toolbox.savePicture(colourCroppedImage, filename + ".grid.jpg");

			grayscaleCroppedImage.flush();
		}
		else
		{
			grayscaleCroppedImage.flush();
		}

		//7.3 save any colony picture files, if in debug mode
		if(IrisFrontend.debug){
			double circularityThreshold_min = 0.5;
			double circularityThreshold_max = 0.6;
			int sizeThreshold = 10;

			//for all rows
			for(int i=0;i<settings.numberOfRowsOfColonies;i++){
				//for all columns
				for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

					//output all the tiles that fulfill the above criteria
					if(basicTileReaderOutputs[i][j].circularity<circularityThreshold_max &&
							basicTileReaderOutputs[i][j].circularity>circularityThreshold_min &&
							basicTileReaderOutputs[i][j].colonySize>sizeThreshold){

						//get the output filename, keep in mind: i and j are zero-based, user wants to see them 1-based
						String tileFilename = path + File.separator + String.format("tile_%.3f_%04d_%02d_%02d_", 
								basicTileReaderOutputs[i][j].circularity, basicTileReaderOutputs[i][j].colonySize, i+1, j+1) + justFilename;

						Toolbox.saveColonyPicture(i,j,colourCroppedImage, segmentationOutput, basicTileReaderOutputs, tileFilename);
					}

					//also output all the empty tiles
					if(basicTileReaderOutputs[i][j].colonySize==0){
						//get the output filename, keep in mind: i and j are zero-based, user wants to see them 1-based
						String tileFilename = path + File.separator + String.format("tile_%.3f_%04d_%02d_%02d_", 
								basicTileReaderOutputs[i][j].circularity, basicTileReaderOutputs[i][j].colonySize, i+1, j+1) + justFilename;

						Toolbox.saveColonyPicture(i,j,colourCroppedImage, segmentationOutput, basicTileReaderOutputs, tileFilename);
					}
				}
			}



		}

		colourCroppedImage.flush();

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
	 * I cannot believe I have to write this
	 * @param list
	 * @return
	 */
	private static double getMean(ArrayList<Integer> list){

		int sum = 0;

		for(int i=0;i<list.size();i++){
			sum += list.get(i);
		}

		return(sum/list.size());
	}

	/**
	 * There has to be a better way guys..
	 * @param list
	 * @return
	 */
	private static double getVariance(ArrayList<Integer> list){
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
	private static ImagePlus cropImage(ImagePlus originalImage, Roi roi){
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




