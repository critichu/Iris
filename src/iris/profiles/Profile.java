/**
 * 
 */
package iris.profiles;

import ij.ImagePlus;
import iris.imageSegmenterOutput.ImageSegmenterOutput;
import iris.settings.Settings;
import iris.tileReaderOutputs.TileReaderOutput;

import java.util.ArrayList;

/**
 * 
 * 
 * @author george
 *
 */
abstract class Profile {
	
	/**
	 * this holds the image given to this profile for processing
	 */
	public ImagePlus originalImage;
	
	
	/**
	 * This holds the Settings object
	 */
	public Settings settings;
	
	
	/**
	 * This is where the results of the calls to the imageSegmenter are being stored
	 */
	public ArrayList<ArrayList<ImageSegmenterOutput>> segmenterOutput; 
	
	
	
	/**
	 * This is where the results of the calls to the tileReadout(s) are being stored
	 */
	public ArrayList<ArrayList<TileReaderOutput>> tileReaderOutput;
	
	
	
		
}
	
	
