/**
 * 
 */
package iris.ui;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author George Kritikos
 *
 */
class PicturesFilenameFilter implements FilenameFilter {

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
		if(filename.startsWith("tile_")){
			return(false);
		}
		if((new File(dir, filename)).isDirectory()){
			return(false);
		}
		if(filename.contains(".jpg") || filename.contains(".JPG") || filename.contains(".png") || filename.contains(".tif")){ //png is a hack to also process screenshots
			return(true);
		}
		return(false);
	}
	
}

