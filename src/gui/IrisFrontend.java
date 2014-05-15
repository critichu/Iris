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

/**
 * This class acts as the decision point between console and GUI versions
 * @author George Kritikos
 *
 */
public class IrisFrontend {
	
	
	public static String selectedProfile;


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
		"Ecoli growth",
		"Ecoli opacity",
		"Ecoli opacity 384",
		"Ecoli growth 384 - hazy colonies",
		"B.subtilis Opacity (HSB)",
		"B.subtilis Sporulation (HSB)",
		//"CPRG 384",
		"Biofilm formation",
		"Biofilm formation (HSB)",
		"Morphology Profile [Candida 96-plates]"
		//"Biofilm formation - Simple Grid",
		//"Opacity",
		//"Opacity (fixed grid)"
	};


	/**
	 * This is the name of the log file to be written. 
	 * Iris opens it for appending on every invocation of the software, writing a header with the time.
	 * It closes it after a run is done.
	 */
	public static BufferedWriter logFile = null;

	/**
	 * This string holds the software version that is defined here once to be used whenever it needs to be displayed.
	 */
	public static String IrisVersion = "0.9.4.23";

	/**
	 * This string holds the hash id of Iris versioning in Git
	 */
	public static String IrisBuild = "06f4453";


	

	public static void main(String[] args) {
		if(args.length==0)
			IrisGUI.main(args);
		else
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
