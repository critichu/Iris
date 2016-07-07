/**
 * 
 */
package profileBundledTileReaders;

import gui.IrisFrontend;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import settings.ColorSettings;
import tileReaderInputs.OpacityTileReaderInput;
import tileReaderOutputs.MorphologyTileReaderOutput;
import tileReaders.MorphologyTileReader;

/**
 * @author George Kritikos
 *
 */
public class MorphologyProfileStm96TileReaders {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	public static String profileName = "Morphology Profile [Salmonella 96-plates]";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = "This profile quantifies the amount of colony structure (how 'wrinkly' a colony is)";


	/**
	 * This holds access to the settings object
	 */
	public ColorSettings settings = new ColorSettings(IrisFrontend.settings);








	//7.1 output the colony measurements as a text file
	//output.append("row\tcolumn\tcolony size\tcolony circularity\tcolony morphology score\tcolony normalized morphology score\t\n");
	String header = new String("row\t" +
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
			"size normalized color intensity\n");



	/**
	 * This function will analyze the picture using the basic profile
	 * The end result will be a file with the same name as the input filename,
	 * after the addition of a .iris ending
	 * @param filename
	 */
	public void analyzePicture(String filename){


		//load picture

		File file = new File(filename);
		String justFilename = file.getName();

		System.out.println("\n\n[" + profileName + "] analyzing picture:\n  "+justFilename);

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


		//create an array of measurement outputs
		MorphologyTileReaderOutput readerOutput = new MorphologyTileReaderOutput();


		//imageSegmentationBounds = ();

		try{
			readerOutput = MorphologyTileReader.processTileWrinkly(
					new OpacityTileReaderInput(originalImage, imageSegmentationBounds, settings));
		}catch(Exception e){
			System.err.print("\tError getting morphology at tile "+ Integer.toString(i+1) +" "+ Integer.toString(j+1) + "\n");
			readerOutput = new MorphologyTileReaderOutput();
		}
		//each generated tile image is cleaned up inside the tile reader





		output.append(Integer.toString(i+1) + "\t" + Integer.toString(j+1) + "\t" 
				+ Integer.toString(readerOutputs[i][j].colonySize) + "\t"
				+ String.format("%.3f", readerOutputs[i][j].circularity) + "\t"
				+ Integer.toString(readerOutputs[i][j].colonyOpacity) + "\t"
				+ Integer.toString(readerOutputs[i][j].morphologyScoreFixedNumberOfCircles) + "\t"
				+ Integer.toString(readerOutputs[i][j].morphologyScoreWholeColony) + "\t"
				+ String.format("%.3f", readerOutputs[i][j].normalizedMorphologyScore) + "\t"
				+ Integer.toString(readerOutputs[i][j].inAgarSize) + "\t"
				+ String.format("%.3f", readerOutputs[i][j].inAgarCircularity) + "\t"
				+ Integer.toString(readerOutputs[i][j].inAgarOpacity) + "\t"
				+ Integer.toString(readerOutputs[i][j].wholeTileOpacity) + "\t"
				+ Integer.toString(colorReaderOutputs[i][j].colorIntensitySum) + "\t"
				+ Integer.toString(colorReaderOutputs[i][j].biofilmArea) + "\t"
				+ Integer.toString(colorReaderOutputs[i][j].colorIntensitySumInBiofilmArea) + "\t"
				+ String.format("%.3f", colorReaderOutputs[i][j].relativeColorIntensity) + "\n");



	}




}
