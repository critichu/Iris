/**
 * 
 */
package gui;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author George Kritikos
 *
 */
public class PicturesFilenameFilter implements FilenameFilter {

	/**
	 * This function is designed to accept every JPEG file in the given directory
	 * Except dat filenames and the ones that that their filename contains the word "grid"
	 */
	@Override
	public boolean accept(File dir, String filename) {
		if(filename.contains(".iris")){
			return(false);
		}				
		if(filename.contains(".fig")){
			return(false);
		}
		if(filename.endsWith("dat")){
			return(false);
		}
		if(filename.contains(".grid.")){
			return(false);
		}
		if(filename.contains("dat_grid_ovr")){
			return(false);
		}
		if(filename.contains("jpg") || filename.contains("JPG")){
			return(true);
		}
		return(false);
	}
	
}

