/**
 * 
 */
package tileReaders;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import imagescience.feature.Laplacian;
import imagescience.image.Image;
import imagescience.segment.ZeroCrosser;
import tileReaderInputs.BasicTileReaderInput;
import tileReaderOutputs.BasicTileReaderOutput;
import utils.Toolbox;

/**
 * @author George Kritikos
 *
 */
public class LaplacianFilterTileReader {


	public static BasicTileReaderOutput processTile(BasicTileReaderInput input){

		//median filter radius
		double radius = 2.0;
		//laplacian filter scale 
		double scale = 4.0;
		//gaussian radius (sigma)
		double sigma = 1.0;


		//0. create the output object
		BasicTileReaderOutput output = new BasicTileReaderOutput();

		//normally here we would check if the tile is empty, but we're gonna give Laplacian Filter a chance

		//this is the input image
		//		input.tileImage.show();
		//		input.tileImage.hide();

		//first, median-normalize image
		RankFilters medianFilter = new RankFilters();
		ImageProcessor tileImageProcessor = input.tileImage.getProcessor();

		medianFilter.rank(tileImageProcessor, radius, RankFilters.MEDIAN);
		//		input.tileImage.show();
		//		input.tileImage.hide();

		//then, apply laplacian filter

		Image tileImageAsImage = Image.wrap(input.tileImage);
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

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		//laplacianDifference.flush();

		//check if the tile is empty
		if(false){ //HACK: don't check MH results!
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

		return(output);//returns the biggest result
	}


}
