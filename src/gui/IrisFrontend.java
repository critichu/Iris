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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import settings.BasicSettings;

/**
 * This class acts as the decision point between console and GUI versions
 * @author George Kritikos
 *
 */
public class IrisFrontend {


	/**
	 * This holds access to the settings object
	 */
	public static BasicSettings settings = new BasicSettings();


	/**
	 * these are added specially for the multithreading case
	 */
	public static boolean multiThreaded = false;
	public static ExecutorService executorService;
	public static List<Callable<Object>> todoThread;
	public static int numberOfThreads = 4;


	/**
	 * This string array holds the names of all the profiles
	 */
	public static String[] profileCollection = {
		"Stm growth",
		"Ecoli growth -- no empty check",
		"Ecoli growth",
		"Ecoli opacity 96",
		"Ecoli opacity 384",
		"Ecoli opacity 1536",
		//"Ecoli opacity 384 - hazy colonies",
		"B.subtilis Opacity (HSB)",
		"B.subtilis Sporulation (HSB)",
		//"CPRG 384",
		//"Biofilm formation",
		"Biofilm formation PA",
		"Biofilm formation Ecoli",
		"Biofilm formation Ecoli Natural Isolates",
		"Morphology Profile [Candida 96-plates]",
		"Xgal assay 96",
		"Xgal assay 384",
		"Xgal assay 1536",
		"Growth profile inverted"
		//"Biofilm formation - Simple Grid",
		//"Opacity",
		//"Opacity (fixed grid)"
	};

	public static String selectedProfile = IrisFrontend.profileCollection[0];


	/**
	 * This is the name of the log file to be written. 
	 * Iris opens it for appending on every invocation of the software, writing a header with the time.
	 * It closes it after a run is done.
	 */
	public static BufferedWriter logFile = null;

	/**
	 * This string holds the software version that is defined here once to be used whenever it needs to be displayed.
	 */
	public static String IrisVersion = "0.9.4.83";

	/**
	 * This string holds the hash id of Iris versioning in Git
	 */
	public static String IrisBuild = "dfb4322";


	/**
	 * This value calls whether Iris is running in debug mode or not
	 */
	public static boolean debug = false;
	
	/**
	 * This value calls whether Iris will not overwrite existing iris files (nice) or not
	 */
	public static boolean nice = false;
	
	




	/**
	 * 
	 * @param args	IrisJarFilename Profile Path [384/96] [DEBUG] 
	 */
	public static void main(String[] args) {


		int argumentOffset = 0;
		//first check if we need to turn on debug mode
		if(args.length>0 && args[args.length-1].equalsIgnoreCase("DEBUG")){
			IrisFrontend.debug = true;
			argumentOffset = 1;
		}

		//if there's no more command line arguments, then it's GUI mode
		if(args.length<2){
			IrisGUI.main(args);
			return;
		}


		if(args.length>2+argumentOffset){
			if(args[args.length-1-argumentOffset].equals("384")){
				IrisFrontend.settings.numberOfColumnsOfColonies = 24;
				IrisFrontend.settings.numberOfRowsOfColonies = 16;
			}
			else if(args[args.length-1-argumentOffset].equals("96")){
				IrisFrontend.settings.numberOfColumnsOfColonies = 12;
				IrisFrontend.settings.numberOfRowsOfColonies = 8;
			}
		}

		//call the console version
		IrisConsole.main(args);
	}


	/**
	 * This function will create a unique log filename and open it for writing
	 */
	public static void openLog(String path){		
		String uniqueLogFilename = path + File.separator + getUniqueLogFilename();
		try {
			logFile = new BufferedWriter(new FileWriter(uniqueLogFilename));
		} catch (IOException e) {
			System.err.println("Could not open log file");
			logFile = null;
		}
	}

	/**
	 * Does what it says in the box
	 */
	public static void writeToLog(String text){
		try {
			if(logFile!=null){
				logFile.write(text);
				logFile.flush();//write immediately
			}
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
				logFile.close();
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
