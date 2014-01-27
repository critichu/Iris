/**
 * 
 */
package tileReaderOutputs;

/**
 * @author George Kritikos
 *
 */
public class ColorTileReaderOutput extends BasicTileReaderOutput {

	/**
	 * the pixel count of the area which exhibits high relative color intensity
	 */
	public int biofilmArea = 0;
	
	/**
	 * the sum of relative color intensities for the entire colony
	 */
	public int colorIntensitySum = 0;

	/**
	 * this is the sum of relative color intensity only for the biofilm area 
	 * (area over relative color intensity above a given threshold)
	 */
	public int colorIntensitySumInBiofilmArea = 0;
	
	
	/**
	 * This is the logarithm of the color intensity sum for the whole colony divided by
	 * the size of the colony itself
	 */
	public double relativeColorIntensity = 0;
	
	
	
	public ColorTileReaderOutput(){
	}
	

	public ColorTileReaderOutput(int biofilmArea_, int colourIntensitySum_){
		biofilmArea = biofilmArea_;
		colorIntensitySum = colourIntensitySum_;
	}
}
