/**
 * 
 */
package iris.ui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import iris.settings.UserSettings;

/**
 * @author George Kritikos
 *
 */
public class DropdownItemChangeListener implements ItemListener{
	public void itemStateChanged(ItemEvent e) {
		//this only fires when there's a new selected item
		if (e.getStateChange() == ItemEvent.SELECTED) {
	
			//update the user settings
			IrisFrontend.userSettings.ArrayFormat = ((Integer) IrisGUI.comboBox_format.getSelectedItem()).intValue();

			//apply the new settings -- namely update the rows and columns
			UserSettings.applyUserSettings(IrisFrontend.userSettings);
			
			//write the new array format on disk
			if(IrisFrontend.userSettings.writeUserSettings()){
				System.out.println("\n\nFormat settings updated:");
				System.out.println("\tnumber of rows:\t"+IrisFrontend.settings.numberOfRowsOfColonies);
				System.out.println("\tnumber of columns:\t"+IrisFrontend.settings.numberOfColumnsOfColonies + "\n");
			}
		}
	}

}
