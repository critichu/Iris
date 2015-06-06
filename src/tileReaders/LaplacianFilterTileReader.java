/**
 * 
 */
package tileReaders;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagescience.feature.Laplacian;
import imagescience.image.Image;
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
		double scale = 1.5;
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

		ImagePlus tileImageLaplacian = laplacianFilter.run(tileImageAsImage, scale).imageplus();


		//		tileImageLaplacian.show();
		//		tileImageLaplacian.hide();


		//re-scale the laplacian output
		tileImageLaplacian.setProcessor(tileImageLaplacian.getProcessor().convertToByte(true));


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



		//next step: threshold the tile image, use Huang (we might change this once we move to large scale tests)   
		Toolbox.turnImageBW_Huang_auto(laplacianDifference);



//		laplacianDifference.updateImage();
//		laplacianDifference.show();
//		laplacianDifference.hide();

		//analyze the particles in the image, this includes filling in holes (which we expect using the above pipeline)
		ResultsTable resultsTable = new ResultsTable();
		RoiManager roiManager = particleAnalysis_fillHoles(laplacianDifference, resultsTable);

		if(roiManager==null){ //no particles found
			output.emptyResulsTable = true; // this is highly abnormal
			output.colonySize = 0;//return a colony size of zero

			input.cleanup(); //clear the tile image here, since we don't need it anymore
			laplacianDifference.flush();

			return(output);
		}


		int indexOfBiggestParticle = Toolbox.getIndexOfBiggestParticle(resultsTable);
		output.colonySize = Toolbox.getBiggestParticleAreaPlusPerimeter(resultsTable, indexOfBiggestParticle);
		output.circularity = Toolbox.getBiggestParticleCircularity(resultsTable, indexOfBiggestParticle);
		output.colonyROI = roiManager.getRoisAsArray()[indexOfBiggestParticle];

		input.cleanup(); //clear the tile image here, since we don't need it anymore
		laplacianDifference.flush();

		//check if the tile is empty
		if(output.circularity<0.30 || output.colonySize<100){
			output.emptyTile = true;
			output.colonySize = 0;//return a colony size of zero
			output.circularity = 0;
			output.colonyROI = null;

			input.cleanup(); //clear the tile image here, since we don't need it anymore
			laplacianDifference.flush();

			return(output);
		}

		return(output);//returns the biggest result
	}


	static RoiManager particleAnalysis_fillHoles(ImagePlus inputImage, ResultsTable resultsTable){
		//create the results table, where the results of the particle analysis will be shown
		//ResultsTable resultsTable = new ResultsTable();
		RoiManager roiManager = new RoiManager(true);

		//arguments: some weird ParticleAnalyzer.* options , what to measure (area), where to store the results, what is the minimum particle size, maximum particle size
		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES, 
				Measurements.CENTER_OF_MASS + Measurements.AREA+Measurements.CIRCULARITY+Measurements.RECT+Measurements.PERIMETER, 
				resultsTable, 5, Integer.MAX_VALUE);

		ParticleAnalyzer.setRoiManager(roiManager);

		particleAnalyzer.analyze(inputImage);

		//3.1 check if the returned results table is empty
		if(resultsTable.getCounter()==0){
			return(null);
		}

		return(roiManager);
	}

}
