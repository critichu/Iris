/**
 * 
 */
package test;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author George Kritikos
 *
 */
public class MouseClick implements PlugIn, MouseListener
{
	ImagePlus imp;

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
    	
    	IJ.run(imp, "Copy", "");
		IJ.run("Internal Clipboard", "");
		ImagePlus colonyImage = WindowManager.getCurrentImage();
		
		IJ.saveAs(colonyImage, "Jpeg", "/Users/george/Desktop/Clipboard2.jpg");
		colonyImage.close();
		
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void run(String arg0) 
    {
        IJ.open();
        imp = WindowManager.getCurrentImage();
        imp.getCanvas().addMouseListener(this);
        IJ.setTool("oval");
    }
    
}
