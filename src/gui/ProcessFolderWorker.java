/**
 * 
 */
package gui;

import java.awt.Toolkit;
import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import profiles.BasicProfile;
import profiles.BsubtilisHazyProfileHSB;
import profiles.BsubtilisSporulationProfile;
import profiles.CPRGProfile384;
import profiles.ColorProfile;
import profiles.ColorProfileHSB;
import profiles.ColorProfile_SimpleSegmentation;
import profiles.EcoliGrowthProfile;
import profiles.EcoliGrowthProfile384_HazyColonies;
import profiles.EcoliOpacityProfile;
import profiles.EcoliOpacityProfile384;
import profiles.OpacityProfile;
import profiles.OpacityProfile2;

/**
 * @author George Kritikos
 *
 */
//public class ProcessFolderWorker extends SimpleSwingWorker {
public class ProcessFolderWorker extends SwingWorker<String, String> {


	public IrisGUI parentJFrame;
	public File directory;




	public ProcessFolderWorker(IrisGUI parent){
		parentJFrame = parent;
	}


	/**
	 * This function is executed when the open folder button is clicked
	 * @return 
	 */
	@Override
	protected String doInBackground() throws Exception {

		//open the log file for writing
		parentJFrame.openLog(directory.getAbsolutePath());
		parentJFrame.writeToLog("--- Iris version " + IrisGUI.IrisVersion + " log file\tbuild "+IrisGUI.IrisBuild+" ---\n");
		parentJFrame.writeToLog("-- Started processing files at "+ new Date() + " --\n");
		parentJFrame.writeToLog("-----------------------------------------\n\n\n");


		//get a list of the files in the directory, keeping only image files
		File[] filesInDirectory = directory.listFiles(new PicturesFilenameFilter());

		int i=0;
		int max = filesInDirectory.length;
		for (File file : filesInDirectory) {

			publish("Now processing file " + "\n");

			String filename = file.getAbsolutePath();

			/**
			 * Decide which profile to use, according to the profile name
			 */
			String profileName = (String)parentJFrame.comboBox.getSelectedItem();

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
			
			else if(profileName.equals("Ecoli growth 384 - hazy colonies")){
				EcoliGrowthProfile384_HazyColonies ecoliGrowth384_hazy = new EcoliGrowthProfile384_HazyColonies();
				ecoliGrowth384_hazy.analyzePicture(filename);			
			}			
			
			else if(profileName.equals("CPRG 384")){
				CPRGProfile384 cprgProfile384 = new CPRGProfile384();
				cprgProfile384.analyzePicture(filename);			
			}

			else if(profileName.equals("Biofilm formation")){
				ColorProfile colorProfile = new ColorProfile();
				colorProfile.analyzePicture(filename);
			}
			
			else if(profileName.equals("Biofilm formation (HSB)")){
				ColorProfileHSB colorProfileHSB = new ColorProfileHSB();
				colorProfileHSB.analyzePicture(filename);
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

			else{
				System.err.println("Unknown profile name: \"" + profileName +"\"");
			}

			publish("...done! " + "\n\n\n");

			i++;
			setProgress(Math.min(i*100/max, 100));
		}



		return(null);
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
		parentJFrame.btnOpenFolder.setEnabled(true);

		System.out.println("\n\n\n\nDone processing all files\n\n");

		//close the log file
		parentJFrame.writeToLog("\n\n-----------------------------------------\n");
		parentJFrame.writeToLog("-- Done processing all files at "+ new Date() + " --\n");
		parentJFrame.closeLog();

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
