/**
 * 
 */
package iris.test;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.process.ImageProcessor;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author George Kritikos
 *
 */


public class ListenMouseRelease implements MouseListener
{

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		TestUserColonyRoiSelection.startX = e.getX();
		TestUserColonyRoiSelection.startY = e.getY();
	}

	
	@Override
	/**
	 * Upon mouse release, a new colony picture is created from the user-defined ROI
	 * 
	 */
	public void mouseReleased(MouseEvent e) {
		
		
		TestUserColonyRoiSelection.endX = e.getX();
		TestUserColonyRoiSelection.endY = e.getY();
		
		
		ImagePlus imp = WindowManager.getCurrentImage();
		//imp.saveRoi();
		ImageProcessor ip =  imp.getRoi().getMask().crop();
		ImagePlus colonyPicture = new ImagePlus("colony image", ip);
		colonyPicture.show();
		
		
		TestUserColonyRoiSelection.userRoi = (OvalRoi) imp.getRoi();
		ImagePlus.resetClipboard();
		imp.copy();//copy to clipboard
		ImagePlus colonyPicture2 = ImagePlus.getClipboard(); //make a new image
		colonyPicture2.show();
		
		IJ.save(colonyPicture, "test.colony.jpg");
		
//		ImagePlus colonyPicture = new ImagePlus();
//		IJ.run(colonyPicture, "Copy", ""); //copy to clipboard
//		IJ.run("Internal Clipboard", ""); //make a new image
//		IJ.saveAs(colonyPicture, "Jpeg", new String(TestUserColonyRoiSelection.currentFilename+".colony.jpg")); //save it under a new filename

		
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

}
