/**
 * 
 */
package tileReaders;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imagescience.feature.Laplacian;
import imagescience.image.Image;
import imagescience.segment.ZeroCrosser;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class LaplacianFilterTileReader {


	public static BasicTileReaderOutput processTile(BasicTileReaderInput input){

		ImagePlus tileImageCopy = input.tileImage.duplicate();
		tileImageCopy.setRoi(input.tileImage.getRoi());
		
		//0. create the output object
		BasicTileReaderOutput output = new BasicTileReaderOutput();

		


		if(!IrisFrontend.settings.userDefinedRoi){
			
			//median filter radius
			double radius = 2.0;
			//laplacian filter scale 
			double scale = 4.0;
			//gaussian radius (sigma)
			double sigma = 1.0;
			
			//normally here we would check if the tile is empty, but we're gonna give Laplacian Filter a chance

			//this is the input image
			//		input.tileImage.show();
			//		input.tileImage.hide();

			//first, median-normalize image
			RankFilters medianFilter = new RankFilters();
			ImageProcessor tileImageProcessor = tileImageCopy.getProcessor();

			medianFilter.rank(tileImageProcessor, radius, RankFilters.MEDIAN);
			//		input.tileImage.show();
			//		input.tileImage.hide();

			//then, apply laplacian filter

			Image tileImageAsImage = Image.wrap(tileImageCopy);
			Laplacian laplacianFilter = new Laplacian();
			ZeroCrosser zc = new ZeroCrosser();

			Image tileImageToZeroCross = laplacianFilter.run(tileImageAsImage, scale);

			///ImagePlus tileImageLaplacian = tileImageToZeroCross.imageplus();
			//		tileImageLaplacian.show();
			//		tileImageLaplacian.hide();

			zc.run(tileImageToZeroCross);
			ImagePlus tileImageLaplacianZeroCrossed = tileImageToZeroCross.imageplus();

			/*
		//		tileImageLaplacianZeroCrossed.show();
		//		tileImageLaplacianZeroCrossed.hide();


		//re-scale the laplacian output
		//		tileImageLaplacian.setProcessor(tileImageLaplacian.getProcessor().convertToByte(true));


		//calculate the negative of the laplacian
		ImagePlus tileImageLaplacianNegative = tileImageLaplacian.duplicate();
		tileImageLaplacianNegative.getProcessor().invert();


		//		tileImageLaplacianNegative.show();
		//		tileImageLaplacianNegative.hide();


		//subtract the 2 laplacians
		ImageCalculator calculator = new ImageCalculator();
		ImagePlus laplacianDifference = calculator.run("diff create", tileImageLaplacian, tileImageLaplacianNegative);
		laplacianDifference.setProcessor(laplacianDifference.getProcessor().convertToByte(true));

		//remove the unused images
		tileImageLaplacian.flush();
		tileImageLaplacianNegative.flush();


		//		laplacianDifference.updateImage();
		//		laplacianDifference.show();
		//		laplacianDifference.hide();



		//Sobel edge detection, try to find the "halo" around the colonies, nicely given by the laplacian diff
		laplacianDifference.getProcessor().findEdges();

		//		laplacianDifference.updateImage();
		//		laplacianDifference.show();
		//		laplacianDifference.hide();

		//blur the edge detection a bit
		GaussianBlur blur = new GaussianBlur();
		ByteProcessor laplacianDifferenceProcessor = (ByteProcessor) laplacianDifference.getProcessor();
		blur.blurGaussian(laplacianDifferenceProcessor, sigma, sigma, 0.02);

		//		laplacianDifference.updateImage();
		//		laplacianDifference.show();
		//		laplacianDifference.hide();
			 */


			//next step: threshold the tile image, use Huang (we might change this once we move to large scale tests)   
			//Toolbox.turnImageBW_Huang_auto(laplacianDifference);
			Toolbox.turnImageBW_Huang_auto(tileImageLaplacianZeroCrossed);



			//		laplacianDifference.updateImage();
			//		laplacianDifference.show();
			//		laplacianDifference.hide();

			//analyze the particles in the image, this includes filling in holes (which we expect using the above pipeline)
			ResultsTable resultsTable = new ResultsTable();
			RoiManager roiManager = Toolbox.particleAnalysis_fillHoles(tileImageLaplacianZeroCrossed, resultsTable);

			if(roiManager==null){ //no particles found
				output.emptyResulsTable = true; // this is highly abnormal
				output.colonySize = 0;//return a colony size of zero

				input.cleanup(); //clear the tile image here, since we don't need it anymore
				//laplacianDifference.flush();

				return(output);
			}


			int indexOfBiggestParticle = Toolbox.getIndexOfBiggestParticle(resultsTable);
			output.colonySize = Toolbox.getBiggestParticleAreaPlusPerimeter(resultsTable, indexOfBiggestParticle);
			output.circularity = Toolbox.getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
			output.colonyCenter = Toolbox.getBiggestParticleCenterOfMass(resultsTable, indexOfBiggestParticle);
			output.colonyROI = roiManager.getRoisAsArray()[indexOfBiggestParticle];

			//input.cleanup(); //clear the tile image here, since we don't need it anymore
			//laplacianDifference.flush();
			int colonyOpacity = totalColonyBrightnessMinusBackground(tileImageCopy, output.colonyROI);
			

			//check if the tile is empty
			boolean debug = false;
			if(debug){ //HACK: don't check MH results!
				if(output.circularity<0.30 || output.colonySize<100){

					output.emptyTile = true;
					output.colonySize = 0;//return a colony size of zero
					output.circularity = 0;
					output.colonyROI = null;

					input.cleanup(); //clear the tile image here, since we don't need it anymore
					//laplacianDifference.flush();

					return(output);
				}
			}
			
			//I will double-check here if this tile reader returns the whole tile
			if(output.colonySize == input.tileImage.getWidth()*input.tileImage.getHeight()){
				return(new BasicTileReaderOutput());
			}
			

			return(output);//returns the biggest result
		}

		else{ // this is a user-defined ROI, we have to use this one 

			OvalRoi colonyRoi = (OvalRoi) tileImageCopy.getRoi();
			output.colonySize = (int) Toolbox.getRoiArea(tileImageCopy);
			output.circularity = 1; ///HACK: 1 means user-set ROI for now, need to change it to a proper circularity measurement
			output.colonyCenter = new Point(colonyRoi.getBounds().width/2, colonyRoi.getBounds().height/2);
			output.colonyROI = colonyRoi;
			return(output);

		}
	}

	/**
	 * @param tileImage
	 * @param colonyROI
	 * @return
	 */
	private static int totalColonyBrightnessMinusBackground(
			ImagePlus tileImage, Roi colonyROI) {
		
		FloatProcessor backgroundPixels = (FloatProcessor) tileImage.getProcessor().convertToFloat().duplicate();
		backgroundPixels.setRoi(colonyROI);
		backgroundPixels.setValue(0);
		backgroundPixels.setBackgroundValue(0);
		
		backgroundPixels.fill(backgroundPixels.getMask());
//		(new ImagePlus("keep-background mask",backgroundPixels)).show();
		
		FloatProcessor foregroundPixels = (FloatProcessor) tileImage.getProcessor().convertToFloat().duplicate();
		foregroundPixels.setRoi(colonyROI);
		foregroundPixels.setValue(0);
		foregroundPixels.setBackgroundValue(0);
		
		foregroundPixels.fillOutside(colonyROI);
//		(new ImagePlus("keep-foreground mask",foregroundPixels)).show();
		
		
		
		
		int backgroundMedian = getBackgroundMedian(backgroundPixels);
		
		int sumColonyBrightness = sumPixelOverBackgroundBrightness(foregroundPixels, backgroundMedian);
		
		
		return(sumColonyBrightness);
	}

	/**
	 * @param ip
	 * @return
	 */
	private static int sumPixelOverBackgroundBrightness(FloatProcessor ip, int backgroundMedian) {

		float[] pixels = (float[]) ip.getPixels();

		ArrayList<Float> nonZeroPixels = new ArrayList<Float>();
		ArrayList<Float> zeroPixels = new ArrayList<Float>();
		ArrayList<Float> onePixels = new ArrayList<Float>();

		for(float thisPixel : pixels){
			if(thisPixel==255)
				onePixels.add(thisPixel);
			else if(thisPixel==0)
				zeroPixels.add(thisPixel);//don't deal with pixels inside the colony ROI
			else
				nonZeroPixels.add(thisPixel);
		}
		
		int sum = 0;
		for(Float thisPixel : nonZeroPixels){
			sum += Math.round(thisPixel)-backgroundMedian;
		}
	

		return(sum);
	}
	
	
	
	/**
	 * @param ip
	 * @return
	 */
	private static int getBackgroundMedian(FloatProcessor ip) {

		float[] pixels = (float[]) ip.getPixels();

		ArrayList<Float> nonZeroPixels = new ArrayList<Float>();
		ArrayList<Float> zeroPixels = new ArrayList<Float>();
		ArrayList<Float> onePixels = new ArrayList<Float>();

		for(float thisPixel : pixels){
			if(thisPixel==255)
				onePixels.add(thisPixel);
			else if(thisPixel==0)
				zeroPixels.add(thisPixel);//don't deal with pixels inside the colony ROI
			else
				nonZeroPixels.add(thisPixel);
		}


		return(Math.round(getMedian(nonZeroPixels.toArray(new Float[nonZeroPixels.size()]))));
	}
	
	/**
	 * just gets the median, nothing to see here
	 * @param inputArray
	 * @return
	 */
	public static float getMedian(Float[] inputArray){
		Arrays.sort(inputArray);
		double median;
		if (inputArray.length % 2 == 0)
			median = ((double)inputArray[inputArray.length/2] + (double)inputArray[inputArray.length/2 - 1])/2;
		else
			median = (double) inputArray[inputArray.length/2];

		return((float)median);
	}



}
