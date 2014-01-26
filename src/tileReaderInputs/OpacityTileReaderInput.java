/**
 * 
 */
package tileReaderInputs;

import ij.ImagePlus;
import ij.gui.Roi;
import settings.BasicSettings;

/**
 * @author George Kritikos
 *
 */
public class OpacityTileReaderInput extends BasicTileReaderInput {

	/**
	 * @param tileImage_
	 * @param settings_
	 */
	public OpacityTileReaderInput(ImagePlus tileImage_, BasicSettings settings_) {
		super(tileImage_, settings_);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param tileImage_
	 * @param roi_
	 * @param settings_
	 */
	public OpacityTileReaderInput(ImagePlus tileImage_, Roi roi_, BasicSettings settings_) {
		super(tileImage_, roi_, settings_);
		// TODO Auto-generated constructor stub
	}

}
