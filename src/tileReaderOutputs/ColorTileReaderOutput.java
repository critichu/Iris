/**
 * 
 */
package tileReaderOutputs;

import ij.gui.Roi;

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
	 * This is the color intensity sum for the whole colony divided by
	 * the size of the colony itself
	 */
	public double relativeColorIntensity = 0;
	
	
	/**
	 * This is the color intensity sum for the whole colony divided by
	 * the area of the biggest disk that can fit into the colony 
	 */
	public double relativeColorIntensityForRoundSize = 0;


	
	/**
	 * This is the mean of the color in X randomly sampled pixels, where X doesn't depend on the size of the colony 
	 */
	public double meanSampleColor = 0;
	
	

	/**
	 * This value holds the color within a circular area of defined-radius starting from the center of mass of the colony
	 */
	public double centerAreaColor = 0;
	
	
	/**
	 * This value holds the opacity within a circular area of defined-radius starting from the center of mass of the colony
	 */
	public double centerAreaOpacity = 0;
	
	
	/**
	 * This is the ROI that defines the center of the colony
	 */
	public Roi centerROI = null;
	
	
	
	public ColorTileReaderOutput(){
	}
	

	public ColorTileReaderOutput(int biofilmArea_, int colourIntensitySum_){
		biofilmArea = biofilmArea_;
		colorIntensitySum = colourIntensitySum_;
	}
}
