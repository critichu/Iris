/**
 * 
 */
package iris.imageSegmenterInput;

import ij.ImagePlus;
import iris.settings.BasicSettings;
import iris.settings.Settings;

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
