/**
 * 
 */
package test;

import java.io.File;
import java.io.FilenameFilter;

import profiles.BasicProfile;
import profiles.ColorProfile;
import tileReaderOutputs.ColorTileReaderOutput;

/**
 * @author George Kritikos
 *
 */
public class ColourReadoutDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File directory = new File(args[0]);
		if(!directory.isDirectory()){
			System.err.println("\n"+args[0]+" is not a valid directory, exiting");
			return;
		}
		
		FilenameFilter filter = new FilenameFilter() {
			
			@Override
			/**
			 * This function is designed to accept every JPEG file in the given directory
			 * Except dat filenames and the ones that that their filename contains the word "grid"
			 */
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
				if(filename.contains("jpg") || filename.contains("JPG")){
					return(true);
				}
				return(false);
			}
		};
		
		File[] filesInDirectory = directory.listFiles(filter);
		
		
		
		
		
		for (File file : filesInDirectory) {
			String filename = file.getAbsolutePath();
			
			ColorProfile profile = new ColorProfile();
			profile.analyzePicture(filename);
			//System.err.println("\n");
		}
	}

	}


