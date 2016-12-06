package iris.ui;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

class IrisFrontendBigMem {

	public static void main(String[] args) throws Exception {
		
		//String pathToThisJar = String ClassLoader.getSystemClassLoader().getResource(".").getPath();
		//String pathToThisJar = IrisFrontendBigMem.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
	
		//check if we have enough memory -- if so just run the IrisFrontend
		//memory threshold here is 1GB
		long maxHeapSize = Runtime.getRuntime().maxMemory();
		if(maxHeapSize>(long)1e9){
			
			//System.out.println("Invoking Iris with more than 1.5GM of memory");
			//System.out.println("Invoking Iris with " + Math.round(maxHeapSize/1e9) + "GB memory");
			IrisFrontend.main(args);
			return;
		}
		
		//System.out.println("not enough memory, reinvoke this jar..");
		
		
		//if there's not enough space, invoke another JVM, this time with enough heap space
		String pathToThisJar = getPathToJar();

//		System.out.println("path:");
//		System.out.println(pathToThisJar);
//		System.out.println("");
		
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
		   strBuilder.append(" "+args[i]);
		}
		String allArguments = strBuilder.toString();
		
		/*
		 * run on 1GB heap memory, unless it's a 64-bit system
		 */
		String gigabytes = "1";
		String systemArchitecture = System.getProperty("sun.arch.data.model");
		if(systemArchitecture.equals("64")){
			gigabytes = "2";
		}
		
		ProcessBuilder pb = new ProcessBuilder(
				"java",
				"-jar",
				"-Xmx"+gigabytes+"G",
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