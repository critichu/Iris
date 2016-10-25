/**
 * 
 */
package settings;

import gui.IrisFrontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * @author George Kritikos
 * This class will be read by the JSON reader
 */
public class UserSettings {

	private boolean SingleColony = false;
	private boolean DebugMode = false;
	private ProfileSettings profileSettings[] = new ProfileSettings[0];
	private int ArrayFormat = 1536;

	public class ProfileSettings {
		private String ProfileName = "";
		public RoatationSettings rotationSettings = new RoatationSettings();
		public CroppingSettings croppingSettings = new CroppingSettings();
		public SegmentationSettings segmentationSettings = new SegmentationSettings();
		public DetectionSettings detectionSettings = new DetectionSettings();
	}
	public class RoatationSettings {
		public boolean autoRotateImage = true;
		public float manualImageRotationDegrees = 0;
	}
	public class CroppingSettings {
		public boolean UserCroppedImage = false;
		public boolean UseFixedCropping = false;
		public int FixedCropping_X_Start = 0;
		public int FixedCropping_Y_Start = 0;
		public int FixedCropping_X_End = 1000;
		public int FixedCropping_Y_End = 1000;
	}
	public class SegmentationSettings {
		public boolean ColonyBreathing = false;
		public int ColonyBreathingSpace = 30;
	}
	public class DetectionSettings {
		public int MinimumValidColonySize = 100;
		public float MinimumValidColonyCircularity = (float) 0.3;
	}

	//dummy settings	
	//	public UserSettings(){
	//		profileSettings[0] = new ProfileSettings();
	//		profileSettings[1] = new ProfileSettings();
	//		profileSettings[2] = new ProfileSettings();
	//	}

	//	public UserSettings(){
	//		UserSettings loadedSettings = loadUserSettings();
	//		
	//		SingleColony = loadedSettings.SingleColony;
	//		DebugMode = loadedSettings.DebugMode;
	//		this.profileSettings = loadedSettings.profileSettings;
	//	}

	/**
	 * this will parse the JSON file inside the jar into a stream, and load that stream into a userSettings object
	 * @return
	 */
	public static UserSettings loadUserSettings(){
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File("iris.user.settings.json"));
		} catch (FileNotFoundException e) {
//			System.err.println("no user settings file found, using default settings");
//			return(new UserSettings());
			return(null); //return null if file not found
		}
		return(loadUserSettings(inputStream));
	}
	

	/**
	 * loads the user setings from the given input stream corresponding to a JSON file
	 * @param inputStream
	 * @return
	 */
	private static UserSettings loadUserSettings(InputStream inputStream){ 

		String jsonString = readStream(inputStream);

		Gson gson = new Gson();
		UserSettings loadedSettings = null;
		try {
			loadedSettings = gson.fromJson(jsonString, UserSettings.class);
		} catch (JsonSyntaxException e) {
//			System.err.println("no user settings file found, using default settings");
//			return(new UserSettings());
			return(null); //return null settings if it didn't work out
		}
		return(loadedSettings);
	}


	/**
	 * Copies over user-defined settings to the Iris Settings class
	 * @param loadedSettings
	 */
	public static void applyUserSettings(UserSettings loadedSettings){

		if(loadedSettings==null) //no user settings loaded
			return;

		if(loadedSettings.DebugMode)
			IrisFrontend.debug = true;


		//set number of rows and columns
		if(loadedSettings.SingleColony){
			IrisFrontend.singleColonyRun = true;
			IrisFrontend.settings.numberOfRowsOfColonies = 1;
			IrisFrontend.settings.numberOfColumnsOfColonies = 1;
		}
		else{
			switch (loadedSettings.ArrayFormat) {
			case 6144:
				IrisFrontend.settings.numberOfRowsOfColonies = 64;
				IrisFrontend.settings.numberOfColumnsOfColonies = 96;
				break;
			case 1536:
				IrisFrontend.settings.numberOfRowsOfColonies = 32;
				IrisFrontend.settings.numberOfColumnsOfColonies = 48;
				break;
			case 384:
				IrisFrontend.settings.numberOfRowsOfColonies = 16;
				IrisFrontend.settings.numberOfColumnsOfColonies = 24;
				break;
			case 96:
				IrisFrontend.settings.numberOfRowsOfColonies = 8;
				IrisFrontend.settings.numberOfColumnsOfColonies = 12;
				break;
			case 24:
				IrisFrontend.settings.numberOfRowsOfColonies = 4;
				IrisFrontend.settings.numberOfColumnsOfColonies = 6;
				break;
			default:
				System.err.println("User settings: ignoring unkown format: " + loadedSettings.ArrayFormat);
				break;
			}
		}
	}
	
	
	
	/**
	 * Returns the user settings associated with this profile
	 * @param profileName
	 * @return
	 */
	public ProfileSettings getProfileSettings(String profileName){
		
		if(this.profileSettings.length==0)
			return(null);
		
		for (ProfileSettings profileSettings : this.profileSettings) {
			if(profileSettings.ProfileName.equals(profileName)){
				return(profileSettings);
			}
		}
		return(null);//no settings supplied for this profile
	}
	
	
	
	/**
	 * gets the subset of loaded settings that matched a known profile name
	 * @return
	 */
	public HashSet<String> getLoadedUserSettings(){
		
		HashSet<String> allProfileNames = new HashSet<String>();
		Collections.addAll(allProfileNames, IrisFrontend.profileCollection);
		
		HashSet<String> allLoadedUserSettings = new HashSet<String>();
		
		for (ProfileSettings profileSettings : this.profileSettings) {
			allLoadedUserSettings.add(profileSettings.ProfileName);
		}
		
		//keep only the profile names with loaded settings
		allProfileNames.retainAll(allLoadedUserSettings);
		
		return(allProfileNames);
	}






	/**
	 * writes user settings to disk
	 * @param thisSettings
	 * @return
	 */
	public static String writeUserSettings(UserSettings thisSettings){ 

		Gson gson = new Gson();  
		String settingsJsonString = gson.toJson(thisSettings);
		return(settingsJsonString);
	}


	/**
	 * this function will read the entire given stream into a String
	 * @param is
	 * @return
	 */
	private static String readStream(InputStream is) {
		StringBuilder sb = new StringBuilder(512);
		try {
			Reader r = new InputStreamReader(is, "UTF-8");
			int c = 0;
			while ((c = r.read()) != -1) {
				sb.append((char) c);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}

}
