/**
 * 
 */
package profiles;

import ij.ImagePlus;
import imageSegmenterOutput.ImageSegmenterOutput;

import java.util.ArrayList;

import settings.Settings;
import tileReaderOutputs.TileReaderOutput;

/**
 * 
 * 
 * @author george
 *
 */
public abstract class Profile {
	
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
	
	
