/**
 * 
 */
package tileReaderOutputs;

/**
 * @author George Kritikos
 *
 */
public class OpacityTileReaderOutput extends BasicTileReaderOutput {

	/**
	 * This return value denotes the sum of brightness in a colony 
	 * after removal of the background's brightness.
	 * Brightness here stands for the 8-bit numeric value (0 to 255) of pixel intensity
	 * in grayscale.
	 */
	public int opacity = 0;
	
	
	/**
	 * This is set to true if something went wrong getting the colony opacity levels
	 */
	public boolean errorGettingOpacity = false;
	
}
