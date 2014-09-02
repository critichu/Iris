/**
 * 
 */
package tileReaders;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.plugin.Hough_Circles;
import ij.plugin.filter.Binary;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.geom.Ellipse2D;

import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;

/**
 * This tile reader is designed to kick in for nasty colonies that are close to the background
 * color.
 * It finds the circle that fits best* in the colony, by applying the Hough transformation
 * to the tile picture.
 * Basically, it's sole output is then the colonyROI
 * 
 * @author George Kritikos
 *
 */
public class MyHoughCircleFinder {

	/**
	 * This simple tile readout gets the area (in pixels) of the colony in the tile.
	 * 
	 * @param input
	 * @return
	 */
	public static BasicTileReaderOutput processTile(BasicTileReaderInput input){


		ImagePlus tileImage =  input.tileImage.duplicate();
		//tileImage.setRoi((Roi)null);

		//0. create the output object
		BasicTileReaderOutput output = new BasicTileReaderOutput();

		//		tileImage.updateImage();
		//		tileImage.show();
		//		tileImage.hide();

		//
		//--------------------------------------------------
		//
		//

		//1. pre-process the picture (i.e. make it grayscale)
		ImageConverter imageConverter = new ImageConverter(tileImage);
		imageConverter.convertToGray8();

		tileImage.updateImage();
		//		tileImage.show();
		//		tileImage.hide();

		//
		//--------------------------------------------------
		//
		//

		//optional: smooth grayscale picture for best results

		//		ImageProcessor ip = tileImage.getProcessor();
		//		ip.setSnapshotCopyMode(true);
		//		//ip.smooth();
		//		ip.sharpen();
		//		ip.setSnapshotCopyMode(false);
		//
		//		tileImage.updateImage();
		//		tileImage.show();
		//		tileImage.hide();


		//
		//--------------------------------------------------
		//
		//




		//
		//--------------------------------------------------
		//
		//

		//4. apply a threshold at the tile, using the Otsu algorithm		
		//utils.Toolbox.turnImageBW_Otsu_auto(tileImage);
		//ip.setAutoThreshold(Method.Otsu, true);


		//tileImage.updateImage();
		//				tileImage.show();
		//				tileImage.hide();



		//		Thresholder thresholder = new Thresholder();
		//		thresholder.skipDialog = true;
		//		thresholder.applyThreshold(tileImage);
		//
		//		adaptiveThr_ adaptiveThresholder = new adaptiveThr_();
		//		adaptiveThresholder.setup("", tileImage);
		//		adaptiveThresholder.my_run(ip, 20, 0);

		Auto_Local_Threshold.Mean(tileImage, 20, 0, 0, false);
		tileImage.show();		
		tileImage.hide();



		Binary binaryTools = new Binary();
		binaryTools.setup("close", tileImage);
		binaryTools.run(tileImage.getProcessor());

		tileImage.show();		
		tileImage.hide();


		//	Prefs.blackBackground = true;
		binaryTools.setup("fill", tileImage);
		binaryTools.run(tileImage.getProcessor());

		tileImage.show();		
		tileImage.hide();


		Prefs.blackBackground = true;
		tileImage.getProcessor().invert();

		//3. apply edge detection (Sobel)

		ImageProcessor ip = tileImage.getProcessor();
		ip.setSnapshotCopyMode(true);
		ip.findEdges();
		ip.setSnapshotCopyMode(false);
		
		tileImage.updateImage();
		tileImage.show();
		tileImage.hide();


		//
		//
//		tileImage.show();		
//		tileImage.hide();

		//
		//--------------------------------------------------
		//
		//

		//5. apply the Hough algorithm for circle detection
		Hough_Circles my_Hough = new Hough_Circles();
		//tileImage.setRoi(input.tileImage.getRoi(), false);
		//tileImage.getProcessor().setRoi(input.tileImage.getRoi());
		Ellipse2D biggestCircle = my_Hough.my_run(tileImage.getProcessor());
		//my_Hough.run(tileImage.getProcessor());


		//		Hough_Circles_IMMI immi_Hough = new Hough_Circles_IMMI();
		//		immi_Hough.run(tileImage.getProcessor());
		//		
		//		int[] blah = immi_Hough.getRadius();
		//		Point[] blah2 = immi_Hough.getCirclesCentre();



		double topleftX = biggestCircle.getX() - biggestCircle.getWidth() / 2.0;
		double topleftY = biggestCircle.getY() - biggestCircle.getWidth() / 2.0;

		double radius = biggestCircle.getWidth()/2;


		output.colonySize = (int) Math.round(Math.PI*radius*radius);
		output.circularity=1;
		output.colonyROI = new OvalRoi(topleftX, topleftY, biggestCircle.getWidth(), biggestCircle.getHeight());

		return(output);
	}



}
