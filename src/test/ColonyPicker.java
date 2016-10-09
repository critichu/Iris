/**
 * 
 */
package test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Measurements;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class ColonyPicker implements PlugIn, MouseListener
{
	ImagePlus imp;
	String imageFilename;
	double roiMinSizeThreshold = 50;

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		try {
			saveColonyRoi_plugin();
			//saveColonyRoi_standalone();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}


	static { 
		System.setProperty("plugins.dir", "./plugins/");
		System.setProperty("sun.java2d.opengl", "true");
	}


	public static void main(String args[]) { 

		new ImageJ();

		ColonyPicker colonyPicker = new ColonyPicker();
		colonyPicker.run("");

		//        System.setProperty("plugins.dir", "./");
		//        IJ.run("colonyPicker");
		//fiji.Debug.run("colonyPicker");
	}


	@Override
	public void run(String arg0) 
	{
		imp = IJ.openImage();
		imp.show();
		//get filename and directory information
		FileInfo originalFileInfo = imp.getOriginalFileInfo();
		imageFilename = new String( originalFileInfo.directory + "/" + originalFileInfo.fileName);

		imp.getCanvas().addMouseListener(this);
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
	public OvalRoi saveColonyRoi_plugin() {

		OvalRoi selectedRoi = (OvalRoi) imp.getRoi();
		imp.getProcessor().setBackgroundValue(255); //set background to black

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(getRoiArea()<roiMinSizeThreshold){
			return(null);
		}

		//calculate the colony image filename
		String colonyImageDirectory = new String(imageFilename + ".colonies");
		new File(colonyImageDirectory).mkdir();

		String colonyImageFilenameDecoration = new String(
						Integer.toString(selectedRoi.getBounds().x) + "_" +
						Integer.toString(selectedRoi.getBounds().y) + "_" +
						Integer.toString(selectedRoi.getBounds().width) + "_" +
						Integer.toString(selectedRoi.getBounds().height));

		String colonyImageFilename = new String(
				colonyImageDirectory + File.separator + 
				"colony_" + colonyImageFilenameDecoration + ".jpg" );

		
		//convert ROI to the bounding rect
		imp.setRoi(new Rectangle(selectedRoi.getBounds()));

		IJ.run(imp, "Copy", "");
		IJ.run("Internal Clipboard", "");
		ImagePlus colonyImage = WindowManager.getCurrentImage();
		colonyImage.getProcessor().setBackgroundValue(0);
		
		colonyImage.setRoi(new OvalRoi(
				0, 0,
				selectedRoi.getBounds().width,
				selectedRoi.getBounds().height));
		
		//set overlay of the new ROI
		colonyImage.setOverlay(colonyImage.getRoi(), Color.cyan, 1, new Color(0, 0, 0, 0));
		
		IJ.saveAs(colonyImage, "Jpeg", colonyImageFilename);
		//colonyImage.close();

		return(selectedRoi);
	}


	/**
	 * This function will be called upon mouse release
	 * @return
	 */
	public Roi saveColonyRoi_standalone() {

		Roi selectedRoi = (OvalRoi) imp.getRoi();

		if(selectedRoi==null){
			return(null);
		}

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(getRoiArea()<roiMinSizeThreshold){
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




	/**
	 * This just returns the ROI area
	 * @param selectedRoi
	 * @return
	 */
	public double getRoiArea(){
		ImageProcessor ip = imp.getProcessor(); 
		ip.setRoi(imp.getRoi()); 
		ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.MEAN, imp.getCalibration()); 
		double area2 = stats.area;
		return(area2);
	}


}
