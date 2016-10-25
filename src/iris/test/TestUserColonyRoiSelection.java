/**
 * 
 */
package iris.test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Toolbar;

/**
 * @author George Kritikos
 *
 */
public class TestUserColonyRoiSelection {

	public static int startX = 0;
	public static int startY = 0;
	public static int endX = 0;
	public static int endY = 0;
	public static OvalRoi userRoi = null;
	public static String currentFilename = "";
	
	/**
	 * @param args
	 */
	 public static void main(String[] args) {
		 currentFilename = new String("/Users/george/Desktop/kinetics with Iris/example output/Pseudomonas biofilm/TB-1p_014.JPG.grid.jpg");

		 ImagePlus imp = IJ.openImage(currentFilename);
		 int blah = Toolbar.getToolId();
		 
		 Toolbar newToolbar = new Toolbar();
		 newToolbar.setTool(Toolbar.OVAL);
		 //IJ.setTool(Toolbar.OVAL);
		 imp.show();
		 
		 //imp = WindowManager.getCurrentImage();
		 imp.getCanvas().addMouseListener(new ListenMouseRelease()); 
		 //this will call a separate routine upon every mouse release

	 }

}





