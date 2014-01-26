/**
 * 
 */
package imageSegmenterInput;

import settings.BasicSettings;
import settings.Settings;
import ij.ImagePlus;

/**
 * @author george
 *
 */
public class BasicImageSegmenterInput extends ImageSegmenterInput {
	
	public ImagePlus imageToSegment;
	public BasicSettings settings;
	
	
	/**
	 * @param imageToSegment
	 * @param settings
	 */
	public BasicImageSegmenterInput(ImagePlus imageToSegment_, BasicSettings settings_) {
		imageToSegment = imageToSegment_;
		settings = settings_;
	}
}
