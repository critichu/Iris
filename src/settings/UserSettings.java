/**
 * 
 */
package settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * @author George Kritikos
 * This class will be read by the JSON reader
 */
public class UserSettings {
		
	boolean SingleColony = false;
	boolean DebugMode = false;
	ProfileSettings profileSettings[]; //= new ProfileSettings[3];
	
	public class ProfileSettings {
		public String ProfileName = "";
		int NumberOfRows = 0;
		int NumberOfColumns = 0;
		CroppingSettings croppingSettings = new CroppingSettings();
		SegmentationSettings segmentationSettings = new SegmentationSettings();
	}
	public class CroppingSettings {
		boolean UserCroppedImage = false;
		boolean UseFixedCropping = false;
		int FixedCropping_X_Start = 0;
		int FixedCropping_Y_Start = 0;
		int FixedCropping_X_End = 1000;
		int FixedCropping_Y_End = 1000;
	}
	public class SegmentationSettings {
		boolean ColonyBreathing = false;
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
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(new File("iris.user.settings.json"));
		} catch (FileNotFoundException e) {
			System.err.println("no user settings file found, using default settings");
			return(new UserSettings());
		}
		return(loadUserSettings(inputStream));
	}
	
	
	public static UserSettings loadUserSettings(InputStream inputStream){ 
		 
		String jsonString = readStream(inputStream);
		
		Gson gson = new Gson();
		UserSettings loadedSettings;
		try {
			loadedSettings = gson.fromJson(jsonString, UserSettings.class);
		} catch (JsonSyntaxException e) {
			System.err.println("no user settings file found, using default settings");
			return(new UserSettings());
		}
		return(loadedSettings);
	}
	

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
	public static String readStream(InputStream is) {
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
