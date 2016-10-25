/**
 * 
 */
package iris.test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

/**
 * @author george
 *
 */
public class TestCropImageCopy {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		
		//get current working directory, by default, it's the bin directory
//		URL location = TestCropImageCopy.class.getProtectionDomain().getCodeSource().getLocation();
//        System.out.println(location.getFile());

        
        //FileReader fr = new FileReader("images/easy.jpeg");

		
		//open the original file
		ImagePlus originalPicture = IJ.openImage("images/easy.jpeg");
				
		//crop the plate so that we keep only the colonies
		Roi keepOnlyColoniesROI = new Roi(580, 380, 4080, 2730);
		originalPicture.setRoi(keepOnlyColoniesROI);
		originalPicture.copy(false);//copy to the internal clipboard
		
		
		
		//copy to a new picture, trash the original (larger) one
		ImagePlus croppedImage = ImagePlus.getClipboard();
		originalPicture.flush();
		 
		
		//make the cropped picture grayscale
		ImageConverter imageConverter = new ImageConverter(croppedImage);
		imageConverter.convertToGray8();
		
        croppedImage.deleteRoi();
		croppedImage.show();
        
        
        //set selection 1 and copy
		croppedImage.setRoi(233, 0, 400, 400);
		croppedImage.copy(false);//copy to the internal clipboard
        
        //show selection 1
        ImagePlus imp2 = ImagePlus.getClipboard();
        imp2.show();
        croppedImage.deleteRoi();
        
        
        //set selection 2 and copy
        croppedImage.setRoi(300, 0, 400, 400);
        croppedImage.copy(false);//copy to the internal clipboard
        
        //show selection 1
        ImagePlus imp3 = ImagePlus.getClipboard();
        imp3.show();
        croppedImage.deleteRoi();
                

	}
	
}
