/**
 * 
 */
package iris.ui;

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

import iris.settings.BasicSettings;
import iris.settings.UserSettings;

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
	 * UserSettings object will be read by the JSON reader
	 */
	public static UserSettings userSettings = new UserSettings();

	/**
	 * This field declares whether its about a single colony per picture or not
	 */
	public static boolean singleColonyRun = false;

	/**
	 * these are added specially for the multithreading case
	 */
	public static boolean multiThreaded = false;
	static ExecutorService executorService;
	public static List<Callable<Object>> todoThread;
	public static int numberOfThreads = 4;


	/**
	 * This string array holds the names of all the profiles
	 */
	public static String[] profileCollection = {
		"Colony growth",
		"Colony growth inverted",
		"B.subtilis sporulation",
		"CPRG profile",
		"Biofilm formation",
		"Morphology profile",
		"Morphology&Color profile",
		"Xgal assay"
	};

	static String selectedProfile = IrisFrontend.profileCollection[0];


	/**
	 * This is the name of the log file to be written. 
	 * Iris opens it for appending on every invocation of the software, writing a header with the time.
	 * It closes it after a run is done.
	 */
	private static BufferedWriter logFile = null;

	/**
	 * This string holds the software version that is defined here once to be used whenever it needs to be displayed.
	 */
	public static String IrisVersion = "0.9.7.1";

	/**
	 * This string holds the hash id of Iris versioning in Git
	 */
	public static String IrisBuild = "aec27d8";


	/**
	 * This value calls whether Iris is running in debug mode or not
	 */
	public static boolean debug = false;

	/**
	 * This value calls whether Iris will not overwrite existing iris files (nice) or not
	 */
	static boolean nice = false;



	public static void main(String[] args) {

		userSettings = UserSettings.loadUserSettings();
		
		//apply user settings
		UserSettings.applyUserSettings(userSettings);

		int argumentOffset = 0;
		//first check if we need to turn on debug mode
		if(args.length>0 && args[args.length-1].equalsIgnoreCase("DEBUG")){
			IrisFrontend.debug = true;
			argumentOffset = 1;
		}

		for(int i=1; i<args.length; i++){
			if(args[i].equalsIgnoreCase("singleColony")){
				IrisFrontend.singleColonyRun=true;
			}
		}
		
		//if there's no more command line arguments, then it's GUI mode
		if(args.length<2){
			IrisGUI.main(args);
			return;
		}


		if(args.length>2+argumentOffset && !IrisFrontend.singleColonyRun){
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
	static void openLog(String path){		
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
	static void closeLog(){

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
	private static String getUniqueLogFilename() {
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
