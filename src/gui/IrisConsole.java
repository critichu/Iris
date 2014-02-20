/**
 * 
 */
package gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author George Kritikos
 *
 */
public class IrisConsole {

	

	/**
	 * This is the name of the log file to be written. 
	 * Iris opens it for appending on every invocation of the software, writing a header with the time.
	 * It closes it after a run is done.
	 */
	public static BufferedWriter logFile = null;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {


		if(args.length<2){
			printUsage();
			return;
		}

		IrisFrontend.selectedProfile = args[0];
		String folderLocation = args[1];

		


		//distinguish between whole-folder input and single-file input
		File fileOrFolder = new File(folderLocation);
		if(fileOrFolder.isDirectory()){
			ProcessFolderWorker processFolderWorker = new ProcessFolderWorker();
			processFolderWorker.directory = new File(folderLocation);

			try {
				processFolderWorker.doInBackground();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else{
			//it's a file
			//we have to check whether it's an image file
			PicturesFilenameFilter filter = new PicturesFilenameFilter();
			if(filter.accept(null, folderLocation)){
				try {
					ProcessFolderWorker.processSingleFile(fileOrFolder);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}

	}


	public static void printUsage(){
		System.out.println("Usage: Iris ProfileName FolderLocation\n");
		System.out.println("Tip: call without any arguments to invoke GUI\n");
	}


	/**
	 * In case we're running in console mode, the combobox will be null
	 * @return
	 */
	public static String getCurrentlySelectedProfile(){

		return(IrisFrontend.selectedProfile);
	}




	/**
	 * This function will create a unique log filename and open it for writing
	 */
	public static void openLog(String path){		
		String uniqueLogFilename = path + File.separator + getUniqueLogFilename();
		try {
			IrisConsole.logFile = new BufferedWriter(new FileWriter(uniqueLogFilename));
		} catch (IOException e) {
			System.err.println("Could not open log file");
			IrisConsole.logFile = null;
		}
	}

	/**
	 * Does what it says in the box
	 */
	public static void writeToLog(String text){
		try {
			if(logFile!=null) 
				IrisConsole.logFile.write(text);
		} catch (IOException e) {
			//System.err.println("Error writing log file");
			//fail silently, because the standard error is redirected to this function
		}
	}	

	/**
	 * Does what it says in the box
	 */
	public static void closeLog(){

		try {
			if(logFile!=null) 
				IrisConsole.logFile.close();
		} catch (IOException e) {
			System.err.println("Error writing log file");
		}
	}



	/**
	 * This function will create a unique filename, using the Iris version and the current time
	 * @return
	 */
	public static String getUniqueLogFilename() {
		return("iris_v"+IrisFrontend.IrisVersion+"_"+getDateTime()+".log");
	}

	/**
	 * This function will return the date and time in a format that can be used to create a unique filename.
	 * @return
	 */
	private final static String getDateTime(){
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss");
		return df.format(new Date());
	}
}
