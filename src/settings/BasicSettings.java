/**
 * 
 */
package settings;

import java.awt.EventQueue;
import java.io.Serializable;

import javax.swing.JFrame;

/**
 * @author george
 *
 */
public class BasicSettings extends Settings implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8114385137202950668L;

	
	/**
	 * holds the number of columns of colonies
	 */
	public int numberOfColumnsOfColonies = 48; 
	
	/**
	 * holds the number of rows of colonies
	 */
	public int numberOfRowsOfColonies = 32;
	
	
	/**
	 * the minimum distance in pixels you would expect between the centers of 2 adjacent colonies
	 * it should be the same in both axes, so it's set here only once 
	 */
	public int minimumDistanceBetweenRows = 60;
	

	/**
	 * the maximum distance in pixels you would expect between the centers of 2 adjacent colonies
	 * it should be the same in both axes, so it's set here only once 
	 */
	public int maximumDistanceBetweenRows = 108;
	
	
	/**
	 * This is the plate-wide threshold (after cropping) that is found, for instance, during the image
	 * segmentation step. This is intended to be set before the TileReader is called, so that each call
	 * to the TileReader also contains this global threshold setting.
	 * Here it is originally set as -1 to designate that it is not set at any step of the
	 * image processing.
	 * THIS SHOULD NOT BE EXPOSED TO THE USER VIA THE SETTINGS WINDOW
	 */
	public int threshold = -1;
	
	
	
	
	/**
	 * This option, if set to true, will save the image file of the grid in the same folder, 
	 * next to the original input image file. 
	 */
	public boolean saveGridImage = false;
	
	
	

	/**
	 * Create the frame.
	 */
	public BasicSettings() {
		setBounds(100, 100, 450, 300);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}

}
