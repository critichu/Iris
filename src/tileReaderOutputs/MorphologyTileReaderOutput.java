/**
 * 
 */
package tileReaderOutputs;

/**
 * @author George Kritikos
 *
 */
public class MorphologyTileReaderOutput extends BasicTileReaderOutput {

	/**
	 * This return value denotes the "wrinkliness" of the colony.
	 * The higher this number, the more complicated the colony structure
	 */
	public int morphologyScore = 0;
	
	/**
	 * This return value is the same as the number above, but this time it's normalized
	 * against the size of the colony (since typically bigger colonies have better chances 
	 * of getting a higher morphology score)
	 */
	public int normalizedMorphologyScore = 0;
	
	
	/**
	 * This is set to true if something went wrong getting the colony morphology levels
	 */
	public boolean errorGettingMorphology = false;
	
}
