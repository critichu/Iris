/**
 * 
 */
package gui;

import java.awt.Toolkit;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import profiles.BasicProfile;
import profiles.BsubtilisHazyProfileHSB;
import profiles.BsubtilisSporulationProfile;
import profiles.CPRGProfile384;
import profiles.ColorProfile;
import profiles.ColorProfileEcoli;
import profiles.ColorProfilePA;
import profiles.ColorProfile_SimpleSegmentation;
import profiles.EcoliGrowthProfile;
import profiles.EcoliOpacityProfile;
import profiles.EcoliOpacityProfile384;
import profiles.EcoliOpacityProfile384_HazyColonies;
import profiles.MorphologyProfileCandida96;
import profiles.OpacityProfile;
import profiles.OpacityProfile2;
import profiles.XgalProfile;

/**
 * @author George Kritikos
 *
 */
//public class ProcessFolderWorker extends SimpleSwingWorker {
public class ProcessFolderWorker extends SwingWorker<String, String> {

	//public IrisGUI parentJFrame;
	public File directory;




	//	public ProcessFolderWorker(IrisGUI parent){
	//		parentJFrame = parent;
	//	}


	/**
	 * This function is executed when the open folder button is clicked
	 * @return 
	 */
	@Override
	protected String doInBackground() throws Exception {

		//open the log file for writing
		IrisFrontend.openLog(directory.getAbsolutePath());
		IrisFrontend.writeToLog("--- Iris version " + IrisFrontend.IrisVersion + " log file\tbuild "+IrisFrontend.IrisBuild+" ---\n");
		IrisFrontend.writeToLog("-- Started processing files at "+ new Date() + " --\n");
		IrisFrontend.writeToLog("-----------------------------------------\n\n\n");


		//get a list of the files in the directory, keeping only image files
		File[] filesInDirectory = directory.listFiles(new PicturesFilenameFilter());

		int i=0;
		int max = filesInDirectory.length;

		for (File file : filesInDirectory) {
			processSingleFile(file);

			i++;
			int progress = Math.min(i*100/max, 100);
			setProgress(progress);
			System.out.println(i + " / " + max + "\t(" + progress +"% done)" +  "\n\n");

			publish("...done! " + "\n\n\n");
		}

		//IrisFrontend.closeLog();
		//close the log file
		IrisFrontend.writeToLog("\n\n-----------------------------------------\n");
		IrisFrontend.writeToLog("-- Done processing all files at "+ new Date() + " --\n");
		IrisFrontend.closeLog();

		return(null);
	}


	public static void processSingleFile(File file){


		//publish("Now processing file " + "\n");
		//System.out.println("Now processing file " + "\n");

		String filename = file.getAbsolutePath();

		/**
		 * Decide which profile to use, according to the profile name
		 */
		String profileName = IrisFrontend.selectedProfile;

		if(profileName.equals("Stm growth")){
			BasicProfile basicProfile = new BasicProfile();
			basicProfile.analyzePicture(filename);
		}

		else if(profileName.equals("Ecoli opacity")){
			EcoliOpacityProfile ecoliOpacityProfile = new EcoliOpacityProfile();
			ecoliOpacityProfile.analyzePicture(filename);			
		}			

		else if(profileName.equals("B.subtilis Opacity (HSB)")){
			BsubtilisHazyProfileHSB bsubtilisHazyProfileHSB = new BsubtilisHazyProfileHSB();
			bsubtilisHazyProfileHSB.analyzePicture(filename);			
		}

		else if(profileName.equals("B.subtilis Sporulation (HSB)")){
			BsubtilisSporulationProfile bsubtilisSporulationProfile = new BsubtilisSporulationProfile();
			bsubtilisSporulationProfile.analyzePicture(filename);			
		}

		else if(profileName.equals("Ecoli growth")){
			EcoliGrowthProfile ecoliGrowthProfile = new EcoliGrowthProfile();
			ecoliGrowthProfile.analyzePicture(filename);			
		}

		else if(profileName.equals("Ecoli opacity 384")){
			EcoliOpacityProfile384 ecoliOpacityProfile384 = new EcoliOpacityProfile384();
			ecoliOpacityProfile384.analyzePicture(filename);			
		}

		else if(profileName.equals("Ecoli opacity 384 - hazy colonies")){
			EcoliOpacityProfile384_HazyColonies ecoliOpacity384_hazy = new EcoliOpacityProfile384_HazyColonies();
			ecoliOpacity384_hazy.analyzePicture(filename);			
		}
		
		else if(profileName.equals("Xgal assay 384")){
			XgalProfile xgalProfile = new XgalProfile();
			xgalProfile.settings.numberOfColumnsOfColonies = 24;
			xgalProfile.settings.numberOfRowsOfColonies = 16;
			xgalProfile.analyzePicture(filename);			
		}
		
		else if(profileName.equals("Xgal assay 1536")){
			XgalProfile xgalProfile = new XgalProfile();
			xgalProfile.settings.numberOfColumnsOfColonies = 48;
			xgalProfile.settings.numberOfRowsOfColonies = 32;
			xgalProfile.analyzePicture(filename);			
		}

		else if(profileName.equals("CPRG 384")){
			CPRGProfile384 cprgProfile384 = new CPRGProfile384();
			cprgProfile384.analyzePicture(filename);			
		}

		else if(profileName.equals("Biofilm formation")){
			ColorProfile colorProfile = new ColorProfile();
			colorProfile.analyzePicture(filename);
		}

		else if(profileName.equals("Biofilm formation PA")){
			ColorProfilePA colorProfilePA = new ColorProfilePA();
			colorProfilePA.analyzePicture(filename);
		}
		
		else if(profileName.equals("Biofilm formation Ecoli")){
			ColorProfileEcoli colorProfileEcoli = new ColorProfileEcoli();
			colorProfileEcoli.analyzePicture(filename);
		}

		else if(profileName.equals("Biofilm formation - Simple Grid")){
			ColorProfile_SimpleSegmentation colorProfile_simpleSegmentation = new ColorProfile_SimpleSegmentation();
			colorProfile_simpleSegmentation.analyzePicture(filename);
		}

		else if(profileName.equals("Opacity")){
			OpacityProfile opacityProfile = new OpacityProfile();
			opacityProfile.analyzePicture(filename);
		}

		else if(profileName.equals("Opacity (fixed grid)")){
			OpacityProfile2 opacityProfile2 = new OpacityProfile2();
			opacityProfile2.analyzePicture(filename);
		}

		else if(profileName.equals("Morphology Profile [Candida 96-plates]")){
			MorphologyProfileCandida96 morphologyProfile = new MorphologyProfileCandida96();
			morphologyProfile.analyzePicture(filename);
		}


		else{
			System.err.println("Unknown profile name: \"" + profileName +"\"");
		}

	}


	/**
	 * This method will wait for all threads to finish execution before carrying on
	 */
	private static void waitForThreads(List<Callable<Object>> todoIndex_) {
		List<Future<Object>> listOfFutures = null;
		try {
			listOfFutures = IrisFrontend.executorService.invokeAll(todoIndex_);

			for (Future<Object> future : listOfFutures) {
				future.get();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//wait for all futures to finish executing
		IrisFrontend.executorService.shutdown();

	}


	protected void process(String item) {
		System.out.println(item);
	}

	/*
	 * Executed in event dispatching thread
	 */
	@Override
	public void done() {

		// call get to make sure any exceptions 
		// thrown during doInBackground() are 
		// thrown again
		try {
			get();
		} catch (final InterruptedException ex) {
			throw new RuntimeException(ex);
		} catch (final ExecutionException ex) {
			throw new RuntimeException(ex.getCause());
		}

		Toolkit.getDefaultToolkit().beep();
		IrisGUI.btnOpenFolder.setEnabled(true);

		System.out.println("\n\n\n\nDone processing all files\n\n");

		//close the log file
		IrisFrontend.writeToLog("\n\n-----------------------------------------\n");
		IrisFrontend.writeToLog("-- Done processing all files at "+ new Date() + " --\n");
		IrisFrontend.closeLog();

	}


	/**
	 * This function gets a value i and it's maximum possible value
	 * and calculates what percentage of the max value is the given value i
	 * @param i
	 * @param max
	 * @return
	 */
	private int getPercentageDone(int i, int max){
		float fraction = i/max;
		float percentage = fraction*100;

		return(Math.round(percentage));
	}

}
