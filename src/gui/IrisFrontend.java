/**
 * 
 */
package gui;

import java.io.BufferedWriter;
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
	public static String IrisVersion = "0.9.4.18";

	/**
	 * This string holds the commit id of Iris versioning in Git
	 */
	public static String IrisBuild = "55e1fd9";


	

	public static void main(String[] args) {
		if(args.length==0)
			IrisGUI.main(args);
		else
			IrisConsole.main(args);
	}
	
	
}
