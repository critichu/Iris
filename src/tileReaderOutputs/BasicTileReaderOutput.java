/**
 * 
 */
package tileReaderOutputs;

import ij.gui.Roi;

/**
 * 
 * @author George Kritikos
 *
 */
public class BasicTileReaderOutput extends TileReaderOutput {

	/**
	 * the pixel count of the area of the biggest particle in the tile
	 */
	public int colonySize = 0;
	
	/**
	 * the circularity index of the biggest particle in the tile (corresponds to the colony)
	 */
	public float circularity = 0;
	
	/**
	 * the ROI that describes the colony
	 */
	public Roi colonyROI = null;

	
	public BasicTileReaderOutput(){
	}
	

	public BasicTileReaderOutput(int colonySize_){
		colonySize = colonySize_;
	}
	
	
	
	
	//the rest of the class has to do with error handling
	
	/**
	 * This value is true if something went wrong during the execution of the tile reader
	 */
	public boolean errorOccurred = false;
	
	
	/**
	 * This value is true if the ResultsTable returned by the particle analyzer was empty.
	 * This is highly unusual, perhaps the user should be notified.
	 */
	public boolean emptyResulsTable = false;

	/**
	 * This value is true if the specific tile has no colonies in it.
	 */
	public boolean emptyTile = false;
}
