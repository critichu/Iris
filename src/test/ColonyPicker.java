/**
 * 
 */
package test;

import gui.IrisFrontend;
import gui.ProcessFolderWorker;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class ColonyPicker implements PlugIn, KeyListener//, MouseListener
{
	ImagePlus imp;
	String imageFilename;
	String colonyImageFilename;
	double roiMinSizeThreshold = 50;

	/*
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	 */
	
	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		if(keyCode==32 || keyChar==' '){
			try {
				ImagePlus colonyImage = saveColonyRoi_plugin();
				
				IrisFrontend.selectedProfile = "Ecoli opacity 96";
				IrisFrontend.singleColonyRun=true;
				ProcessFolderWorker.processSingleFile(new File(colonyImageFilename));
				
			} catch (Exception e1) {
				//fail silently
				//e1.printStackTrace();
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}


	static { 
		System.setProperty("plugins.dir", "./plugins/");
		System.setProperty("sun.java2d.opengl", "true");
	}


	/* obsolete, run through ColonyPickerFrontendBigMem
	public static void main(String args[]) { 

		new ImageJ();

		ColonyPicker colonyPicker = new ColonyPicker();
		colonyPicker.run("");

		//        System.setProperty("plugins.dir", "./");
		//        IJ.run("colonyPicker");
		//fiji.Debug.run("colonyPicker");
	}
	 */


	@Override
	public void run(String arg0) 
	{
		imp = IJ.openImage(arg0);
		imp.show();

		new WaitForUserDialog("Instructions", "Define colony areas, hit space to verify selection. \nClose image window when done").show();


		//get filename and directory information
		FileInfo originalFileInfo = imp.getOriginalFileInfo();
		imageFilename = new String( originalFileInfo.directory + "/" + originalFileInfo.fileName);

//		imp.getCanvas().addMouseListener(this);
		imp.getCanvas().removeKeyListener(IJ.getInstance()); // to stop imageJ from getting any keyboard input
		imp.getCanvas().addKeyListener(this);
		IJ.setTool("oval");
	}

	//    public void run(String arg0) 
	//    {
	//    	//ij.io.DirectoryChooser dc = new DirectoryChooser("Please choose an image directory");
	//        IJ.open();
	//        imp = WindowManager.getCurrentImage();
	//        imp.getCanvas().addMouseListener(this);
	//        IJ.setTool("oval");
	//    }


	/**
	 * This function will be called upon mouse release
	 * @return
	 */
	public ImagePlus saveColonyRoi_plugin() {


		//check if this is an OvalRoi, Shape, or Rectangle			
		//OvalRoi selectedRoi = (OvalRoi) imp.getRoi();
		Roi selectedRoi = (Roi) imp.getRoi();
		imp.getProcessor().setBackgroundValue(255); //set background to black

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(Toolbox.getRoiArea(imp)<roiMinSizeThreshold){
			return(null);
		}

		//calculate the colony image filename
		String colonyImageDirectory = new String(imageFilename + ".colonies");
		new File(colonyImageDirectory).mkdir();

		//width and height are equal to the width and height of the tile picture 
		String colonyImageFilenameDecoration = new String(
				Integer.toString(selectedRoi.getBounds().x) + "_" +
						Integer.toString(selectedRoi.getBounds().y));// + "_" +
		//						Integer.toString(selectedRoi.getBounds().width) + "_" +
		//						Integer.toString(selectedRoi.getBounds().height));

		if(imp.getRoi().getClass().equals(OvalRoi.class)){
			colonyImageFilename = new String(
					colonyImageDirectory + File.separator + 
					"colony_" + colonyImageFilenameDecoration + ".jpg" );
		}
		else if(imp.getRoi().getClass().equals(Roi.class)){
			colonyImageFilename = new String(
					colonyImageDirectory + File.separator + 
					"tile_" + colonyImageFilenameDecoration + ".jpg" );
		} else {
			System.out.println("only rectangle and oval selections allowed");
			return(null);
		}

		//convert ROI to the bounding rect
		imp.setRoi(new Rectangle(selectedRoi.getBounds()));


		/*
		//this is how it works in a script mode
		IJ.run(imp, "Copy", "");
		IJ.run("Internal Clipboard", "");
		ImagePlus colonyImage = WindowManager.getCurrentImage();
		colonyImage.getProcessor().setBackgroundValue(0);
		 */

		//and this is how it works faster/better/easier in Java
		imp.copy();
		ImagePlus colonyImage = ImagePlus.getClipboard();

		//reset ROI back to the one the user selected 
		imp.setRoi(selectedRoi);

		//apply the same ROI to the colony image
		if(imp.getRoi().getClass().equals(OvalRoi.class)){
			colonyImage.setRoi(new OvalRoi(
					0, 0,
					selectedRoi.getBounds().width,
					selectedRoi.getBounds().height));
		}
		else if(imp.getRoi().getClass().equals(Roi.class)){
			colonyImage.setRoi(new Roi(
					0, 0,
					selectedRoi.getBounds().width,
					selectedRoi.getBounds().height));
		}


		//set overlay of the new ROI
		//colonyImage.setOverlay(colonyImage.getRoi(), Color.cyan, 1, new Color(0, 0, 0, 0));

		IJ.saveAs(colonyImage, "Jpeg", colonyImageFilename);
		//colonyImage.close();

		return(colonyImage);

	}


	/**
	 * This function will be called upon mouse release
	 * @return
	 */
	@Deprecated
	public Roi saveColonyRoi_standalone() {

		Roi selectedRoi = (OvalRoi) imp.getRoi();

		if(selectedRoi==null){
			return(null);
		}

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(Toolbox.getRoiArea(imp)<roiMinSizeThreshold){
			return(null);
		}

		//calculate the colony image filename
		String colonyImageDirectory = new String(imageFilename + ".colonies");
		new File(colonyImageDirectory).mkdir();

		String colonyImageFilenameDecoration = new String(
				Integer.toString(selectedRoi.getBounds().x) + "_" +
						Integer.toString(selectedRoi.getBounds().y));

		String colonyImageFilename = new String(
				colonyImageDirectory + File.pathSeparator + 
				"colony_" + colonyImageFilenameDecoration + ".jpg" );




		imp.setRoi(selectedRoi);
		imp.copy();




		ImageProcessor ip = imp.getProcessor().duplicate();
		ip.setRoi(selectedRoi);
		ip.setColor(0);
		ip.setBackgroundValue(0);
		ip.fillOutside(selectedRoi);
		ip = ip.crop();

		//    	Roi s1 = (Roi) selectedRoi.clone();
		//    	ShapeRoi s2 = new ShapeRoi(new Roi(0,0, ip.getWidth(), ip.getHeight()));
		//    	ShapeRoi s3 = s2.xor_generic(s1);
		//    	ip.reset(s3.getMask());

		ImagePlus colonyImage = new ImagePlus("img2", ip);
		colonyImage.show();
		Toolbox.savePicture(colonyImage, colonyImageFilename);

		colonyImage.close();

		return(selectedRoi);
	}

}
