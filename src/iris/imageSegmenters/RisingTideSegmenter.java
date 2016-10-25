/**
 * 
 */
package iris.imageSegmenters;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import iris.imageSegmenterInput.BasicImageSegmenterInput;
import iris.imageSegmenterOutput.BasicImageSegmenterOutput;
import iris.settings.BasicSettings;
import iris.ui.IrisFrontend;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class holds methods that implement the rising tide algorithm
 *
 */
public class RisingTideSegmenter {

	/**
	 * This function will segment the picture according to the rising tide algorithm.
	 * The input should be a BasicImageSegmenterInput, initialized with a grayscaled, cropped
	 * picture of the plate, as well as a corresponding settings object.
	 * @param input
	 * @return
	 */
	public static BasicImageSegmenterOutput segmentPicture(BasicImageSegmenterInput input){

		//if user has made the cropping, return one tile equal to the entire (single-colony) picture
		if(IrisFrontend.singleColonyRun==true){

			//set up an output object
			BasicImageSegmenterOutput output = new BasicImageSegmenterOutput();
			output.ROImatrix = new Roi[1][1];

			//return only one ROI: the entire picture
			output.ROImatrix[0][0] = new Roi( 
					/*x*/ 0,
					/*y*/ 0,
					/*width*/ input.imageToSegment.getWidth(),
					/*height*/ input.imageToSegment.getHeight());

			return(output);
		}


		//get input values
		ImagePlus croppedImage = input.imageToSegment;
		BasicSettings settings = input.settings;

		//set up an output object
		BasicImageSegmenterOutput output = new BasicImageSegmenterOutput();

		//1. copy original picture
		//since this pipeline needs a black and white version of the picture
		//we copy the original picture here, so that we don't tamper with the original cropped picture
		ImagePlus BW_croppedImage = croppedImage.duplicate();


		//2. turn the image Black/White using the Otsu method
		//save the threshold in settings for future use --this is never actually used
		////input.settings.threshold = turnImageBW_Otsu(BW_croppedImage);


		//3. the next step includes calculating the sum of the row/column brightness
		ArrayList<Integer> sumOfColumns = sumOfColumns(BW_croppedImage);
		ArrayList<Integer> sumOfRows = sumOfRows(BW_croppedImage);

		BW_croppedImage.flush();//flush the BW picture, we took the measurements we needed from it


		//4. in this step, we apply the rising tide algorithm 
		//first to the sum of rows, then to the sum of columns
		ArrayList<Integer> minimaBagRows = risingTide(sumOfRows, settings, true);
		ArrayList<Integer> minimaBagColumns = risingTide(sumOfColumns, settings, false);


		//5. check how many minima did rising tide return
		if(minimaBagRows.size()!=settings.numberOfRowsOfColonies+1){
			output.errorOccurred = true;
			output.notEnoughRowsFound = true;
			//continue with executing the algorithm, maybe the result can still be rescued
		}
		if(minimaBagColumns.size()!=settings.numberOfColumnsOfColonies+1){
			output.errorOccurred = true;
			output.notEnoughColumnsFound = true;
			//continue with executing the algorithm, maybe the result can still be rescued
		}

		//		if(output.errorOccurred){
		//			return(output);
		//		}


		//6. sort the rows and columns found
		try {
			Collections.sort(minimaBagRows);
			Collections.sort(minimaBagColumns);			
		} catch (Exception e) {
			return(output); //return here just in case the rows and columns returned are actually null
		}




		//7. check whether the rows and columns are too closely spaced, continue in case of incorrectly spaced columns
		//there will be no iris file written in the end, just an entry in the log message. 
		//We just need to output any Rois (see step 8), so that a grid file can be written for debugging purposes.
		if(!checkTileSpacing(minimaBagRows, settings, input)){
			output.errorOccurred = true;
			output.incorrectRowSpacing = true;
			//return(output);
		}
		if(!checkTileSpacing(minimaBagColumns, settings, input)){
			output.errorOccurred = true;
			output.incorrectColumnSpacing = true;
			//return(output);
		}



		//8. return the ROIs found
		output.ROImatrix = new Roi[minimaBagRows.size()-1][minimaBagColumns.size()-1];

		//for all rows
		for(int i=0;i<minimaBagRows.size()-1;i++){			
			int heightOfThisRow = minimaBagRows.get(i+1) - minimaBagRows.get(i); //Ydiff -- rows go with i

			//for all columns
			for (int j = 0; j < minimaBagColumns.size()-1; j++) {
				int widthOfThisColumn = minimaBagColumns.get(j+1) - minimaBagColumns.get(j); //Xdiff -- columns go with j

				output.ROImatrix[i][j] = new Roi( 
						/*x*/ minimaBagColumns.get(j),
						/*y*/ minimaBagRows.get(i),
						/*width*/ widthOfThisColumn,
						/*height*/ heightOfThisRow);
			}
		}

		return(output);
	}





	//----------------------------------------
	//
	//private, helper functions from here on
	//
	//----------------------------------------








	/**
	 * This function implements the rising tide algorithm to find local minima in the
	 * sum of light intensities, that are distant by at least a minimum distance, defined in the settings
	 * @param sumOfBrightness : an array of integers that is calculated by summing the brightness of all the pixels in an image row-wise or column-wise 
	 * @param settings : pointer to a settings object, which can be used to fine-tune the algorithm
	 * @param isRows : a boolean that shows whether this call is meant to find minima of rows or columns
	 * @return a list of first X minima that were found while the threshold was rising
	 */
	private static ArrayList<Integer> risingTide(ArrayList<Integer> sumOfBrightness, BasicSettings settings, boolean isRows){

		//calculate the number of rows and columns we should reach
		int targetMinimaNumber;
		if(isRows)
			targetMinimaNumber = settings.numberOfRowsOfColonies+1;
		else//is colonies
			targetMinimaNumber = settings.numberOfColumnsOfColonies+1;



		//start with an empty bag
		ArrayList<Integer> minimaBag = new ArrayList<Integer>();


		//limit this loop to a trillion iterations
		int iterations=0;
		while(iterations<Math.pow(10, 9)){

			iterations++;

			//Step 1: find the current global minimum and it's index
			Integer indexOfCurrentMinimum = new Integer(getMinimumAndIndexBW(sumOfBrightness));
			if(indexOfCurrentMinimum<0){
				//an error occurred
				return(minimaBag);
			}

			Integer currentMinimum = new Integer(sumOfBrightness.get(indexOfCurrentMinimum));

			//Step 2: check: if everything is now under the tide (was assigned a max value), the algorithm has to end
			if(currentMinimum==Integer.MAX_VALUE){		
				//this check is now delegated to the caller of rising tide, since the caller also knows
				//the number of minima that need to be returned, and probably has better access to notify the user
				//even by GUI
				return(minimaBag); 
			}


			//Step 3: set everything that is within reach (left and right) of the current minimum to MAX_INT
			//INCLUDING this minimum: it's fate is decided on the next step
			//we have to go as far as the distance in the settings mandates

			int leftMostIndex = indexOfCurrentMinimum - settings.minimumDistanceBetweenRows;
			int rightMostIndex = indexOfCurrentMinimum + settings.minimumDistanceBetweenRows;

			//we cannot go lower than 0, so set leftMost to 0 if that would have been the case
			if(leftMostIndex<0)
				leftMostIndex = 0;		

			//we cannot go over the maximum index, so set rightMost to maxIndex if that would have been the case
			if(rightMostIndex>sumOfBrightness.size()-1)
				rightMostIndex = sumOfBrightness.size()-1;

			for(int i=leftMostIndex; i<=rightMostIndex; i++){
				sumOfBrightness.set(i, Integer.MAX_VALUE);
			}



			//Step 4: check whether our bag of minimas already has a nearby minima there
			//this check is redundant, I'll skip it
			//------------------------------



			//Step 5: add this minima into the minima bag and check the bag's size
			//if the size is equal to the one we want (from settings), then the function is done
			minimaBag.add(indexOfCurrentMinimum);

			if(minimaBag.size()>=targetMinimaNumber){
				//we've reached our goal

				Collections.sort(minimaBag);

				return(minimaBag);
			}


		}
		return(minimaBag);
	}





	/**
	 * This function is the same as above for black/white pictures.
	 * The difference is that it finds the first zero it can get it's hands on
	 * then it counts how many consecutive zeros it finds and it returns the mean point between 
	 * the first and last encountered zero
	 * @param list_input
	 * @param min_out
	 * @param index_out
	 */
	private static int getMinimumAndIndexBW(ArrayList<Integer> list_input){

		//initialize the output to something exotic
		int min_out = Integer.MAX_VALUE;
		int index_out = -1;


		for(int i=0; i<list_input.size(); i++){

			//in case a list that 2 total minima (of the same low number, but in other places)
			//we arbitrarily pick the first one.
			//this algorithm will run again, after the currently selected minima (one of the 2)
			//has been set to MAX_INT, so the other minima (of the 2) will have it's chance
			if(list_input.get(i)<min_out){

				min_out = list_input.get(i);
				index_out = i;

				if(min_out==0){//nothing can go lower than zero

					int originalZero = i;
					int lastZero = i;

					//we found our first zero, now count consequtive zeros
					for(;i<list_input.size(); i++){
						if(list_input.get(i)==0){
							lastZero++;
						}
						else
							break;
					}
					index_out = Math.round((lastZero-originalZero)/2)+originalZero;			
					break;	
				}
			}	

		}
		return(index_out);

	}





	/**
	 * This function will convert the given picture into black and white
	 * using the Otsu method. This version will also return the threshold found.
	 * @param 
	 */
	private static int turnImageBW_Otsu(ImagePlus BW_croppedImage) {
		Calibration calibration = new Calibration(BW_croppedImage);

		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();

		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Otsu, histogram);

		imageProcessor.threshold(threshold);

		//BW_croppedImage.updateAndDraw();

		return(threshold);
	}



	/**
	 * This function will convert the given picture into black and white
	 * using the Huang method. This version will also return the threshold found.
	 * @param 
	 */
	private static int turnImageBW_Huang(ImagePlus BW_croppedImage) {
		Calibration calibration = new Calibration(BW_croppedImage);

		//2 things can go wrong here, the image processor and the 2nd argument (mOptions)
		ImageProcessor imageProcessor = BW_croppedImage.getProcessor();

		ImageStatistics statistics = ImageStatistics.getStatistics(imageProcessor, ij.measure.Measurements.MEAN, calibration);
		int[] histogram = statistics.histogram;

		AutoThresholder at = new AutoThresholder();
		int threshold = at.getThreshold(Method.Huang, histogram);

		imageProcessor.threshold(threshold);

		//BW_croppedImage.updateAndDraw();

		return(threshold);
	}


	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	private static ArrayList<Integer> sumOfRows(ImagePlus croppedImage){
		int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows

		ArrayList<Integer> sumOfRows = new ArrayList<Integer>(dimensions[1]);

		int sum = 0;

		//for all rows
		for(int y=0; y<dimensions[1]; y++ ){
			sum = 0;

			//for all columns
			for(int x=0; x<dimensions[0]; x++ ){

				sum += croppedImage.getPixel(x, y)[0];
			}

			//sum complete, add it to the list
			//sumOfRows.set(y, sum);
			sumOfRows.add(sum);
		}

		return(sumOfRows);
	}





	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's rows (for every y)
	 * @param croppedImage
	 * @return
	 */
	private static ArrayList<Integer> sumOfColumns(ImagePlus croppedImage){
		int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows and columns
		ArrayList<Integer> sumOfColumns = new ArrayList<Integer>(dimensions[0]);

		int sum = 0;

		//for all columns
		for(int x=0; x<dimensions[0]; x++ ){
			sum = 0;

			//for all rows
			for(int y=0; y<dimensions[1]; y++ ){

				sum += croppedImage.getPixel(x, y)[0];
			}

			//sum complete, add it to the list
			//sumOfColumns.set(x, sum);
			sumOfColumns.add(sum);
		}

		return(sumOfColumns);
	}



	/**
	 * This function takes a picture and draws lines in the coordinates of the rows and columns given as arguments
	 * the input picture will not change, we will retr
	 * @param croppedImage
	 * @param segmenterOutput
	 */
	public static ImagePlus paintSegmentedImage(ImagePlus input_croppedImage, BasicImageSegmenterOutput segmenterOutput) {

		ImagePlus paintedImage = input_croppedImage.duplicate();

		//now, all that remains is to paint the picture using imageJ
		ImageProcessor croppedImageProcessor = paintedImage.getProcessor();
		int dimensions[] = paintedImage.getDimensions();
		croppedImageProcessor.setColor(java.awt.Color.white);




		//draw horizontal lines
		for(int i=0; i<segmenterOutput.ROImatrix.length; i++){

			int y = segmenterOutput.ROImatrix[i][0].getBounds().y;

			//draw 3 pixels, one before, one after and one in the middle (where the actual boundary is
			croppedImageProcessor.drawLine(0, y-1, dimensions[0], y-1);

			croppedImageProcessor.drawLine(0, y, dimensions[0], y);

			croppedImageProcessor.drawLine(0, y+1, dimensions[0], y+1);
		}


		//draw vertical lines
		for(int j=0; j<segmenterOutput.ROImatrix[0].length; j++){

			int x = segmenterOutput.ROImatrix[0][j].getBounds().x;

			//draw 3 pixels, one before, one after and one in the middle (where the actual boundary is
			croppedImageProcessor.drawLine(x-1, 0,  x-1, dimensions[1]);

			croppedImageProcessor.drawLine(x, 0, x, dimensions[1]);

			croppedImageProcessor.drawLine(x+1, 0, x+1, dimensions[1]);
		}

		paintedImage.updateImage();

		//HACK: this line needs to be commented out
		input_croppedImage = paintedImage;

		return(paintedImage);


	}



	/**
	 * This function gets a list of Xs (or Ys) and decides whether they are spaced too far or too close apart,
	 * given the minimum distance between them, as given in the settings.
	 * Change: GK, 24.04.2013: there are many false positives for wrong grid placement due to the first rows and
	 * columns being usually bigger. Reasons are 
	 * -bigger outer row/column colonies and
	 * -no other colonies to prohibit placing the grid closer to the boundary of the image 
	 * 
	 * @param minimaBagRows
	 * @param input 
	 * @return false if the distances are not OK, true if they are OK
	 */
	private static boolean checkTileSpacing(ArrayList<Integer> minimaBagRows, BasicSettings settings, BasicImageSegmenterInput input) {

		//false means something went wrong

		if(minimaBagRows==null){
			return(false);
		}

		for(int i=0;i<minimaBagRows.size()-1;i++){
			int distance = minimaBagRows.get(i+1)-minimaBagRows.get(i);

			//check if the distance is too small
			if(distance<settings.minimumDistanceBetweenRows){
				System.err.println(input.imageToSegment.getTitle() + ":\ttoo small distance encountered: " + distance);
				return(false);
			}

			//check if the distance is too big
			if(distance>settings.maximumDistanceBetweenRows){

				//but this is typical for outer rows/columns, so check if this is the case
				if(i==0 || i==minimaBagRows.size()-2)
					continue;
				//System.err.println();
				System.err.println(input.imageToSegment.getTitle() + ":\ttoo big distance encountered: " + distance);
				//return(false);
				return(true);
			}
		}


		return(true);
	}




}
