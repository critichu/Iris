/**
 * 
 */
package iris.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import iris.ui.ProcessFolderWorker;
import iris.utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class ColonyPicker implements PlugIn, KeyListener//, MouseListener
{
	private ImagePlus loadedImage;
	Roi userSelectedRoi;
	private String imageFilename;
	private String colonyImageFilename;
	double roiMinSizeThreshold = 50;
	boolean userIsDone = true;
	public boolean invokeIris = false;

	static { 
		System.setProperty("plugins.dir", "./plugins/");
		System.setProperty("sun.java2d.opengl", "true");
	}

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
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		//user hit space
		if(keyCode==32 || keyChar==' '){
			try {				
				//ImagePlus colonyImage = saveColonyRoi_plugin();
				saveColonyRoi_plugin();

				if(invokeIris){
					
					RoiManager roiManager = new RoiManager(true);
					ProcessFolderWorker.processSingleFile(new File(colonyImageFilename));
					roiManager.moveRoisToOverlay(loadedImage);
					
					Overlay overlay = loadedImage.getOverlay();
					if(overlay!=null)
						overlay.clear();
					
					paintUserSelectedRoi();
				}

			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		//user hit escape
		if(keyCode==KeyEvent.VK_ESCAPE){
			loadedImage.close();
			System.exit(0);
		}

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

		File fileOrFolder = new File(arg0);

		//single file -- interactive mode
		if(arg0.equals("") || !fileOrFolder.exists()){
			loadedImage = IJ.openImage();
			WaitForUserDialog instructionsDialog = new WaitForUserDialog("Instructions", "Define colony areas, hit space to verify selection.\nRectangular selection: let iris detect the colony\nRound selection: manually-defined colony\n\nHit escape when done");
			instructionsDialog.show();
		}

		//single file -- batch mode
		else if(fileOrFolder.isFile()){
			loadedImage = IJ.openImage(fileOrFolder.getPath());
		}

		/*
		//folder mode -- call again this function until we've got all files in directory
		else if(fileOrFolder.isDirectory()){

			File[] filesInDirectory = fileOrFolder.listFiles(new PicturesFilenameFilter());

			for (File file : filesInDirectory) {

				try{
					if(!file.exists()) continue; //if its not a file or it doesn't exist, skip to the next one
					if(!file.isFile()) continue;


					userIsDone = false;

					this.run(file.getPath());

					while(!userIsDone )
						this.wait();


				} catch(Exception e){
					System.out.println("Error processing file!\n");
				}
			}
		}
		 */



		if(loadedImage==null){
			System.err.println("cannot open image");
			return;
		}

		loadedImage.show();

		//get filename and directory information
		FileInfo originalFileInfo = loadedImage.getOriginalFileInfo();
		imageFilename = new String( originalFileInfo.directory + "/" + originalFileInfo.fileName);

		//		imp.getCanvas().addMouseListener(this);
		loadedImage.getCanvas().removeKeyListener(IJ.getInstance()); // to stop imageJ from getting any keyboard input
		loadedImage.getCanvas().addKeyListener(this);
		IJ.setTool("oval");
		userIsDone = false;
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
	private ImagePlus saveColonyRoi_plugin() {


		//check if this is an OvalRoi, Shape, or Rectangle
		userSelectedRoi = (Roi) loadedImage.getRoi();
		loadedImage.getProcessor().setBackgroundValue(255); //set background to black 255 is black b/c this has an inverted look-up table

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(Toolbox.getRoiArea(loadedImage)<roiMinSizeThreshold){
			return(null);
		}

		//calculate the colony image filename
		String colonyImageDirectory = new String(imageFilename + ".colonies");
		new File(colonyImageDirectory).mkdir();

		//width and height are equal to the width and height of the tile picture 
		String colonyImageFilenameDecoration = new String(
				Integer.toString(userSelectedRoi.getBounds().x) + "_" +
						Integer.toString(userSelectedRoi.getBounds().y));

		if(loadedImage.getRoi().getClass().equals(OvalRoi.class)){
			colonyImageFilename = new String(
					colonyImageDirectory + File.separator + 
					"colony_" + colonyImageFilenameDecoration + ".jpg" );
		}
		else if(loadedImage.getRoi().getClass().equals(Roi.class)){
			colonyImageFilename = new String(
					colonyImageDirectory + File.separator + 
					"tile_" + colonyImageFilenameDecoration + ".jpg" );
		} else {
			System.out.println("only rectangle and oval selections allowed");
			return(null);
		}


		//convert ROI to the bounding rect
		loadedImage.setRoi(new Rectangle(userSelectedRoi.getBounds()));


		/*
		//this is how it works in a script mode
		IJ.run(imp, "Copy", "");
		IJ.run("Internal Clipboard", "");
		ImagePlus colonyImage = WindowManager.getCurrentImage();
		colonyImage.getProcessor().setBackgroundValue(0);
		 */

		//and this is how it works faster/better/easier in Java
		loadedImage.copy();
		ImagePlus colonyImage = ImagePlus.getClipboard();

		//reset ROI back to the one the user selected
		loadedImage.deleteRoi();
		loadedImage.setRoi(userSelectedRoi);

		//apply the same ROI to the colony image
		if(userSelectedRoi.getClass().equals(OvalRoi.class)){//oval selection
			colonyImage.setRoi(new OvalRoi(
					0, 0,
					userSelectedRoi.getBounds().width,
					userSelectedRoi.getBounds().height));
			loadedImage.setOverlay(loadedImage.getRoi(), Color.cyan, 1, new Color(0, 0, 0, 0));

		}
		else if(userSelectedRoi.getClass().equals(Roi.class)){//rectangular selection
			colonyImage.setRoi(new Roi(
					0, 0,
					userSelectedRoi.getBounds().width,
					userSelectedRoi.getBounds().height));
			loadedImage.setOverlay(userSelectedRoi.getBounds(), Color.cyan, new BasicStroke(1));
		}


		IJ.saveAs(colonyImage, "Jpeg", colonyImageFilename);

		return(colonyImage);
	}


	/**
	 * use an overlay to paint the current user-verified selection with cyan on the image
	 */
	public void paintUserSelectedRoi(){
		
		//removes any existing overlay
		if(loadedImage.getOverlay()!=null)
			loadedImage.getOverlay().clear();
		
		if(userSelectedRoi.getClass().equals(OvalRoi.class)){//oval selection
			loadedImage.setOverlay(userSelectedRoi, Color.cyan, 1, new Color(0, 0, 0, 0));
		}
		else if(userSelectedRoi.getClass().equals(Roi.class)){//rectangular selection
			loadedImage.setOverlay(userSelectedRoi.getBounds(), Color.cyan, new BasicStroke(1));
		}
	}


	/**
	 * This function will be called upon mouse release
	 * @return
	 */
	@Deprecated
	public Roi saveColonyRoi_standalone() {

		Roi selectedRoi = (OvalRoi) loadedImage.getRoi();

		if(selectedRoi==null){
			return(null);
		}

		//if the selectedRoi is not big enough then quit
		//or too large? -- too large filter is not implemented, large might mean different things for different applications
		if(Toolbox.getRoiArea(loadedImage)<roiMinSizeThreshold){
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




		loadedImage.setRoi(selectedRoi);
		loadedImage.copy();




		ImageProcessor ip = loadedImage.getProcessor().duplicate();
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
