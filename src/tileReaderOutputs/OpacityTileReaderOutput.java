/**
 * 
 */
package tileReaderOutputs;

import java.awt.Point;

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


	/**
	 * This holds the mean of the brightest 10% pixels in the colony
	 */
	public double max10percentOpacity = 0;

	
	/**
	 * This holds the opacity of the pixels within R pixels from the center of the colony
	 * R is the radius of the circle, chosen by the user, or set to 8, in order to compare directly with
	 * http://www.biomedcentral.com/1471-2180/14/171
	 */
	public int centerAreaOpacity = 0;


	/**
	 * This holds the coordinates of the colony center. 
	 * It's useful so as not to re-discover it if we need to access it again on calling a
	 * "process defined tile" function 
	 */
	public Point colonyCenter = null;
	
}
