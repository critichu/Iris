/**
 * 
 */
package gui;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import tileReaders.MyHoughCircleFinder;
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
			IrisSingleColonyRunTileReader.main2(args1);
		}
	}

	public static void main2(String[] args) {

			String filename = args[0];

			//String filename = "/Users/george/Desktop/Iris/biofilm readout/Manuel odd colonies issue/colony pictures/dark/Screen Shot 2014-05-14 at 7.39.43 PM.png";

			//1. open the image file, and check if it was opened correctly
			ImagePlus originalImage = IJ.openImage(filename);
			//check that file was opened successfully
			if(originalImage==null){
				//TODO: warn the user that the file was not opened successfully
				System.err.println("Could not open image file: " + filename);
				return;
			}


			//create input
			BasicTileReaderInput input = new BasicTileReaderInput(originalImage.duplicate(), null);

			//call tile reader
			BasicTileReaderOutput output = MyHoughCircleFinder.processTile(input);



			//optionals:

			//paint contour picture (from output Roi) on input picture
			//originalImage.setRoi(output.colonyROI, true);
			originalImage.getProcessor().setColor(java.awt.Color.cyan);
			output.colonyROI.drawPixels(originalImage.getProcessor());
//			originalImage.show();
//			originalImage.hide();

			//save it as filename.ROI.png
			Toolbox.savePicture(originalImage, filename.concat(".ROI.png"));

			//write file with one entry
		}

	}
