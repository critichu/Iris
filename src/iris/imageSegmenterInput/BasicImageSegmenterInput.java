/**
 * 
 */
package iris.imageSegmenterInput;

import ij.ImagePlus;
import iris.settings.BasicSettings;

/**
 * @author george
 *
 */
public class BasicImageSegmenterInput extends ImageSegmenterInput {
	
	public ImagePlus imageToSegment;
	public BasicSettings settings;
	
	
	/**
	 * This will also adapt the input for various array formats
	 * @param imageToSegment
	 * @param settings
	 */
	public BasicImageSegmenterInput(ImagePlus imageToSegment_, BasicSettings settings_) {
		imageToSegment = imageToSegment_;
		settings = settings_;
		
		
		//update minimum and maximum allowed row/column distance
		double nominalDistanceBetweenRows =  (double) imageToSegment.getWidth() / (double) settings.numberOfColumnsOfColonies; 
		settings.minimumDistanceBetweenRows = (int) Math.round(nominalDistanceBetweenRows*0.75);
		settings.maximumDistanceBetweenRows = (int) Math.round(nominalDistanceBetweenRows*1.5);
	}
}
