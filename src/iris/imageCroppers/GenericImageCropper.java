/**
 * 
 */
package iris.imageCroppers;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageConverter;
import iris.ui.IrisFrontend;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 *This class provides methods to crop the original picture so as to keep only the colonies.
 *This class can be used as a generic method with all plates
 *
 */
public class GenericImageCropper {

	/**
	 * the margin in which to look for the start of the first/last columns
	 */
	private static int plateBorderSearchAreaColumns = 100;

	/**
	 * the margin in which to look for the start of the first/last rows
	 */
	private static int plateBorderSearchAreaRows = 50;


	/**
	 * how many rows/columns that were found to be above the average to skip before reporting that this is the plate border
	 */
	private static int skip = 20;


	/**
	 * These values correspond to the fraction of the in-plate image that will be used as
	 * boundaries within which a search for a minimum will be performed.
	 */
	private static double searchStart = 0.035;
	private static double searchEnd = 0.065;


	/**
	 * This method will detect the plate (plastic) borders, by detecting the first foreground sum of row/column.
	 * By foreground here, I denote sums of rows/columns that are above the mean sum of row/column, which has been calculated using
	 * only the middle of the plate.
	 * This makes it more robust than just searching for the global maxima.
	 * After finding the plate's borders, a search for each beginning of colonies starts within the range of
	 * 4-6% of the in-plate boundaries.
	 * The picture is then cropped at the local minima found within this range.
	 * @param originalImage
	 * @return
	 */
	public static ImagePlus cropPlate(ImagePlus originalImage){
		
		//if user has cropped the picture, no need to re-crop
		if(IrisFrontend.singleColonyRun==true){
			ImagePlus croppedImage = originalImage.duplicate();
			croppedImage.setRoi(originalImage.getRoi());
			return(croppedImage);
		}


		//find plate borders (where the colonies start) and return the Roi that these correspond to
		//perform this in a duplicate picture, so any operations performed to find the Roi won't
		//interfere with the original picture
		ImagePlus duplicate = originalImage.duplicate();
		Roi rectangle = findCropBorders(duplicate);
		duplicate.flush();

		originalImage.setRoi(rectangle);
		originalImage.copy(false);//copy to the internal clipboard

		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		return(croppedImage);
	}



	/**
	 * This function uses the plate border detection and then starts a search from those plate borders
	 * inwards within a range that depends on the size (in pixels) of the picture within the plate borders.
	 * @param originalImage
	 * @return
	 */
	private static Roi findCropBorders(ImagePlus originalImage) {



		//1. get the plate's plastic borders
		Roi plasticPlateBorders = findPlatePlasticBorders(originalImage);

		//2. get the sums of brightness per row/column
		ImageConverter imageConverter = new ImageConverter(originalImage);
		imageConverter.convertToGray8();

		ArrayList<Integer> sumOfColumns = sumOfColumns(originalImage);
		ArrayList<Integer> sumOfRows = sumOfRows(originalImage);


		//3. define the search space for the minima
		int searchSmallWidth = (int) Math.round(plasticPlateBorders.getBounds().width * searchStart);
		int searchBigWidth = (int) Math.round(plasticPlateBorders.getBounds().width * searchEnd);

		int searchSmallHeight = (int) Math.round(plasticPlateBorders.getBounds().height * searchStart);
		int searchBigHeight = (int) Math.round(plasticPlateBorders.getBounds().height * searchEnd);



		//4. get the sublist of columns. The indices found during the call to getIndexOfMinimumElement are relative to the index of the element 
		//at the start of the search. We add it back before storing the indexOfMinimum*, so these variables store indices relative to the original
		//picture and NOT the in-plate picture.

		//left
		int searchStartLeft = plasticPlateBorders.getBounds().x + searchSmallWidth;
		int searchEndLeft = plasticPlateBorders.getBounds().x + searchBigWidth;

		ArrayList<Integer> sublistColumnsLeft = new ArrayList<Integer>(sumOfColumns.subList(searchStartLeft, searchEndLeft));
		int indexOfMinimumLeft = searchStartLeft + getIndexOfMinimumElement(sublistColumnsLeft);

		//top
		int searchStartTop = plasticPlateBorders.getBounds().y + searchSmallHeight;
		int searchEndTop = plasticPlateBorders.getBounds().y + searchBigHeight;

		ArrayList<Integer> sublistRowsTop = new ArrayList<Integer>(sumOfRows.subList(searchStartTop, searchEndTop));
		int indexOfMinimumTop = searchStartTop + getIndexOfMinimumElement(sublistRowsTop);

		//right
		int searchStartRight = plasticPlateBorders.getBounds().x + plasticPlateBorders.getBounds().width - searchBigWidth;
		int searchEndRight = plasticPlateBorders.getBounds().x + plasticPlateBorders.getBounds().width - searchSmallWidth;

		ArrayList<Integer> sublistColumnsRight = new ArrayList<Integer>(sumOfColumns.subList(searchStartRight, searchEndRight));
		int indexOfMinimumRight = searchStartRight + getIndexOfMinimumElement(sublistColumnsRight);

		//bottom
		int searchStartBottom = plasticPlateBorders.getBounds().y + plasticPlateBorders.getBounds().height - searchBigHeight;
		int searchEndBottom = plasticPlateBorders.getBounds().y + plasticPlateBorders.getBounds().height - searchSmallHeight;

		ArrayList<Integer> sublistColumnsBottom = new ArrayList<Integer>(sumOfRows.subList(searchStartBottom, searchEndBottom));
		int indexOfMinimumBottom = searchStartBottom + getIndexOfMinimumElement(sublistColumnsBottom);


		//create the Roi and return
		int roiX = indexOfMinimumLeft;
		int roiY = indexOfMinimumTop;
		int roiWidth = indexOfMinimumRight - indexOfMinimumLeft;
		int roiHeight = indexOfMinimumBottom - indexOfMinimumTop;


		Roi roiToReturn = new Roi(new Rectangle(roiX, roiY, roiWidth, roiHeight));
		return(roiToReturn);

	}
	
	
	/**
	 * This method will return the Roi of the image, where the plate's plastic borders were found.
	 * The image is assumed to be already rotated.
	 * @param originalImage
	 * @return
	 */
	private static Roi findPlatePlasticBorders(ImagePlus originalImage) {
		
		//1. make image grayscale
		ImageConverter imageConverter = new ImageConverter(originalImage);
		imageConverter.convertToGray8();
		
		//2. get sum of rows/columns
		ArrayList<Integer> sumOfColumns = sumOfColumns(originalImage);
		ArrayList<Integer> sumOfRows = sumOfRows(originalImage);
		
		//3. get only the sums that correspond to the middle of the plate, get their means
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		
		//define from where to where to get the sums of brightness (in the sawtooth pattern)
		int columnsStartArea = width/2 - plateBorderSearchAreaColumns;
		int columnsEndArea = width/2 + plateBorderSearchAreaColumns;
		//get the sublist of those sums
		ArrayList<Integer> sublistColumns = new ArrayList<Integer>(sumOfColumns.subList(columnsStartArea, columnsEndArea));
		//get their mean
		int meanOfCenterColumns = (int)Math.round(getMean(sublistColumns));
		
		//define from where to where to get the sums of brightness (in the sawtooth pattern)
		int rowsStartArea = height/2 - plateBorderSearchAreaRows;
		int rowsEndArea = height/2 + plateBorderSearchAreaRows;
		//get the sublist of those sums
		ArrayList<Integer> sublistRows = new ArrayList<Integer>(sumOfRows.subList(rowsStartArea, rowsEndArea));
		//get their mean
		int meanOfCenterRows = (int)Math.round(getMean(sublistRows));
		
		//4. get the index of the 20th element above the mean, this is the plate's plastic bounds
		ArrayList<Integer> indicesOfColumnsSumsAboveMean = getIndicesAboveMean(sumOfColumns, meanOfCenterColumns);
		ArrayList<Integer> indicesOfRowsSumsAboveMean = getIndicesAboveMean(sumOfRows, meanOfCenterRows);
		
		
		
		//get the 20th from left and the 20th from the right
		int indexOfLeftBorder = indicesOfColumnsSumsAboveMean.get(skip);
		int indexOfRightBorder = indicesOfColumnsSumsAboveMean.get(indicesOfColumnsSumsAboveMean.size()-skip);
		
		//get the 20th from the top and the 20th from the bottom
		int indexOfTopBorder = indicesOfRowsSumsAboveMean.get(skip);
		int indexOfBottomBorder = indicesOfRowsSumsAboveMean.get(indicesOfRowsSumsAboveMean.size()-skip);
		
		
		
		int roiX = indexOfLeftBorder;
		int roiY = indexOfTopBorder;
		int roiWidth = indexOfRightBorder - indexOfLeftBorder;
		int roiHeight = indexOfBottomBorder - indexOfTopBorder;
		
		
		Roi roiToReturn = new Roi(new Rectangle(roiX, roiY, roiWidth, roiHeight));
		return(roiToReturn);		
	}



	


	/**
	 * This method simply iterates through this array and finds the index
	 * of the smallest element
	 */
	private static int getIndexOfMinimumElement(ArrayList<Integer> columns) {
		int index = -1;
		float min = Float.MAX_VALUE;

		for (int i = 0; i < columns.size(); i++) {
			if(columns.get(i)<min){
				min = columns.get(i);
				index = i;
			}
		}

		return(index);
	}
	
	/**
	 * This method simply iterates through this array and finds the index
	 * of the smallest element
	 */
	private static int getIndexOfMaximumElement(ArrayList<Integer> columns) {
		int index = -1;
		float max = -Float.MAX_VALUE;

		for (int i = 0; i < columns.size(); i++) {
			if(columns.get(i)>max){
				max = columns.get(i);
				index = i;
			}
		}

		return(index);
	}


	/**
	 * This method iterates over the given list.
	 * It returns the indices of that list where a value was found that was higher than the
	 * given threshold.
	 * 
	 * @param sumOfColumns
	 * @param meanOfCenterColumns
	 * @return
	 */
	private static ArrayList<Integer> getIndicesAboveMean(
			ArrayList<Integer> list, int threshold) {

		ArrayList<Integer> elementsAboveThreshold = new ArrayList<Integer>();

		for(int i=0; i<list.size(); i++){
			if(list.get(i)>threshold){
				elementsAboveThreshold.add(i);
			}
		}
		return(elementsAboveThreshold);
	}



	/**
	 * I cannot believe I have to write this
	 * @param list
	 * @return
	 */
	private static double getMean(ArrayList<Integer> list){

		int sum = 0;

		for(int i=0;i<list.size();i++){
			sum += list.get(i);
		}

		return(sum/list.size());
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
}
