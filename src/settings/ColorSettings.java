/**
 * 
 */
package settings;

import java.io.Serializable;

/**
 * @author George Kritikos
 *
 */
public class ColorSettings extends BasicSettings implements Serializable{
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1046472000512210000L;

	
	/**
	 * This is the threshold above which, the red-color enhanced colony pixels will
	 * be counted as a biofilm area pixels
	 * THIS SHOULD NOT BE EXPOSED TO THE USER VIA THE SETTINGS WINDOW
	 */
	public int colorThreshold = 30;
	

	/**
	 * Create the frame.
	 */
	public ColorSettings() {
		super();
//		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}
}
