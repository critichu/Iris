/**
 * 
 */
package gui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 
 * @author George Kritikos
 *
 */
public class IrisConsole {


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
		
		//check to see if the user specified debug mode
		try {
			if(args[2].contains("DEBUG"))
				IrisFrontend.debug = true;
		} catch (Exception e) {}


		redirectSystemStreams();


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
	 * Calling this function will redirect the standard error to also write the error to the log file
	 */
	private static void redirectSystemStreams() {

		OutputStream err = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				updateLog_err(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateLog_err(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setErr(new PrintStream(err, true));
	}


	private static void updateLog_err(final String text) {
		//first, append this entry to the log file
		IrisFrontend.writeToLog(text);
		//also write it to the standard output
		System.out.println(text);

	}
}
