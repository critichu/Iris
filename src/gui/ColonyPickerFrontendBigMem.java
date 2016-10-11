/**
 * 
 */

package gui;

import ij.ImageJ;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

import test.ColonyPicker;

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
	        
	        if(args!=null && args.length!=0)
	        	colonyPicker.run(args[0]); //args[0] is the filename to open
	        else
	        	colonyPicker.run("");
	        
			
			//System.out.println("Invoking Iris with more than 1.5GM of memory");
			//System.out.println("Invoking Iris with " + Math.round(maxHeapSize/1e9) + "GB memory");
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