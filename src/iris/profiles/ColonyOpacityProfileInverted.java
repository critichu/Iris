/**
 * 
 */
package iris.profiles;

import ij.IJ;
import ij.ImagePlus;

/**
 * This profile is calibrated for use in measuring the colony sizes of E. coli or Salmonella 1536 plates
 * 
 * @author George Kritikos
 *
 */
public class ColonyOpacityProfileInverted extends Profile {

	/**
	 * the user-friendly name of this profile (will appear in the drop-down list of the GUI) 
	 */
	private static String profileName = "Colony Opacity Profile inverted";


	/**
	 * this is a description of the profile that will be shown to the user on hovering the profile name 
	 */
	public static String profileNotes = 
			"This profile is calibrated for use in measuring colony size, density, and opacity\n"+
					"for colonies that are darker than their background e.g. using backlight";


	/**
	 * This function will analyze the picture using the basic profile
	 * The end result will be a file with the same name as the input filename,
	 * after the addition of a .iris ending
	 * @param filename
	 */
	public void analyzePicture(String filename){

		//just invert picture, save it as temp, and then feed it to the normal opacity profile

		//1. open the image file, and check if it was opened correctly
		ImagePlus originalImage = IJ.openImage(filename);

		//check that file was opened successfully
		if(originalImage==null){
			//TODO: warn the user that the file was not opened successfully
			System.err.println("Could not open image file: " + filename);
			return;
		}
		
		
		//2. invert the picture, and save it under a new filename
		String invertedFilename = filename+".inverted.jpg";
		originalImage.getProcessor().invert();
		IJ.saveAs(originalImage, "jpg", invertedFilename);
		

		//3. pass it to the normal opacity profile to analyze
		ColonyOpacityProfile normalOpacityProfile = new ColonyOpacityProfile();
		ColonyOpacityProfile.profileName = ColonyOpacityProfileInverted.profileName;

		normalOpacityProfile.analyzePicture(invertedFilename);



	}


}
