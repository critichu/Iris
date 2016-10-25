/**
 * 
 */

package iris.ui;

import ij.IJ;
import ij.ImageJ;
import ij.gui.ImageWindow;
import iris.test.ColonyPicker;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

import javax.swing.JOptionPane;

class ColonyPickerFrontendBigMem {


	static { 
		System.setProperty("plugins.dir", "./plugins/");
		System.setProperty("sun.java2d.opengl", "true");
	}


	public static void main(String[] args) throws Exception {

		//check if we have enough memory -- if so just run the IrisFrontend
		//memory threshold here is 1.5GB
		long maxHeapSize = Runtime.getRuntime().maxMemory();
		if(maxHeapSize>(long)1.5e9){

			new ImageJ();
			ColonyPicker colonyPicker = new ColonyPicker();


			if(args==null){
				colonyPicker.run("");
				return;
			}

			if(args.length==1){
				colonyPicker.run(args[0]); //args[0] is the folder/filename to open
				return;
			}

			if(args.length>=2){
				//args[0] is the profile name (like in Iris)
				IrisFrontend.selectedProfile = args[0];
				IrisFrontend.singleColonyRun=true;
				colonyPicker.invokeIris = true;

				colonyPicker.run(args[1]); //args[1] is the folder/filename to open

				JOptionPane.showMessageDialog(IJ.getInstance(),
						"Define colony areas, hit space to verify selection.\n\n"+
								"Rectangular selection: let Iris detect the colony\n"+
								"Round selection: manually define the colony\n\n"+

								"Zoom in/out using the magnifying lens icon\n\n"+

								"Hit escape when done",
								"Quick instructions",
								JOptionPane.PLAIN_MESSAGE);


				//just make sure the current window has the focus
				//otherwise setting a ROI and hitting space will fail the first 1-2 times
				try{
					ImageWindow imageWindow = ij.WindowManager.getCurrentWindow();
					imageWindow.requestFocus();
				} catch (Exception e){}

				return;
			}
			return;
		}

		//System.out.println("not enough memory, reinvoke this jar..");


		//if there's not enough space, invoke another JVM, this time with enough heap space
		String pathToThisJar = getPathToJar();


		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			strBuilder.append(" "+args[i]);
		}
		String allArguments = strBuilder.toString();

		ProcessBuilder pb = new ProcessBuilder(
				"java",
				"-jar",
				"-Xmx2G",
				pathToThisJar,
				allArguments
				);
		pb.directory(new File("."));
		Process process = pb.start();
		process.waitFor();
	}



	private static String getPathToJar() throws URISyntaxException{
		CodeSource codeSource = IrisFrontendBigMem.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		String jarDir = jarFile.getParentFile().getPath();
		return(jarFile.getPath());
	}
}