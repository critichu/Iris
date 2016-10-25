/**
 * 
 */
package imageCroppers;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageConverter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 *This class provides methods to crop the original picture so as to keep only the colonies.
 *This class can be used as a generic method with all plates
 *
 */
public class GenericImageCropper2 {

	/**
	 * the margin in which to look for the start of the first/last columns
	 */
	public static int plateBorderSearchAreaColumns = 100;

	/**
	 * the margin in which to look for the start of the first/last rows
	 */
	public static int plateBorderSearchAreaRows = 50;


	/**
	 * how many rows/columns that were found to be above the average to skip before reporting that this is the plate border
	 */
	public static int skip = 20;


	/**
	 * These values correspond to the fraction of the in-plate image that will be used as
	 * boundaries within which a search for a minimum will be performed.
	 */
	private static double searchStart = 0.055;
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
		Roi rectangle = findCropBorders2(duplicate);
		duplicate.flush();

		originalImage.setRoi(rectangle);
		originalImage.copy(false);//copy to the internal clipboard

		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		croppedImage.setTitle(originalImage.getTitle());
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
		Roi plasticPlateBorders = findPlatePlasticBorders2(originalImage);

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


	private static Roi findCropBorders2(ImagePlus originalImage){
		//1. make image grayscale
		ImageConverter imageConverter = new ImageConverter(originalImage);
		imageConverter.convertToGray8();

		int originalImageHeight = originalImage.getHeight();
		int originalImageWidth = originalImage.getWidth();



		//get a horizontal section of the plate
		originalImage.setRoi(new Rectangle(0, 1000, originalImageWidth, 1000));
		originalImage.copy(false);
		ImagePlus horizontalSection = ImagePlus.getClipboard();


		//get a vertical section of the plate
		originalImage.setRoi(new Rectangle(1000, 0, 1000, originalImageHeight));
		originalImage.copy(false);
		ImagePlus verticalSection = ImagePlus.getClipboard();


		//2. get sum of rows/columns
		ArrayList<Integer> sumOfColumns = sumOfColumns(horizontalSection);
		ArrayList<Integer> sumOfRows = sumOfRows(verticalSection);

		//3. get only the sums that correspond to the middle of the plate, get their means
		int indexOfLeftPlasticBorder = getIndexOfMaximumElement(sumOfColumns.subList(0, originalImageWidth/2));
		int indexOfRightPlasticBorder = getIndexOfMaximumElement(sumOfColumns.subList(originalImageWidth/2, originalImageWidth)) + originalImageWidth/2;

		int indexOfTopPlasticBorder = getIndexOfMaximumElement(sumOfRows.subList(0, originalImageHeight/2));
		int indexOfBottomPlasticBorder = getIndexOfMaximumElement(sumOfRows.subList(originalImageHeight/2, originalImageHeight)) + originalImageHeight/2;


		//set the search space 
		int smallMarginWidth = (int)Math.round(searchStart * originalImageWidth);
		int largeMarginWidth = (int)Math.round(searchEnd * originalImageWidth);
		int smallMarginHeight = (int)Math.round(searchStart * originalImageHeight);
		int largeMarginHeight = (int)Math.round(searchEnd * originalImageHeight);


		int indexOfLeftColonyBorder = getIndexOfMinimumElement(sumOfColumns.subList(indexOfLeftPlasticBorder+smallMarginWidth, indexOfLeftPlasticBorder+largeMarginWidth)) + indexOfLeftPlasticBorder+smallMarginWidth;
		int indexOfRightColonyBorder = getIndexOfMinimumElement(sumOfColumns.subList(indexOfRightPlasticBorder-largeMarginWidth, indexOfRightPlasticBorder-smallMarginWidth)) + indexOfRightPlasticBorder-largeMarginWidth;

		int indexOfTopColonyBorder = getIndexOfMinimumElement(sumOfRows.subList(indexOfTopPlasticBorder+smallMarginHeight, indexOfTopPlasticBorder+largeMarginHeight)) + indexOfTopPlasticBorder+smallMarginHeight;
		int indexOfBottomColonyBorder = getIndexOfMinimumElement(sumOfRows.subList(indexOfBottomPlasticBorder-largeMarginHeight, indexOfBottomPlasticBorder-smallMarginHeight)) + indexOfBottomPlasticBorder-largeMarginHeight;


		int roiX = indexOfLeftColonyBorder;
		int roiY = indexOfTopColonyBorder;
		int roiWidth = indexOfRightColonyBorder - indexOfLeftColonyBorder;
		int roiHeight = indexOfBottomColonyBorder - indexOfTopColonyBorder;


		Roi roiToReturn = new Roi(new Rectangle(roiX, roiY, roiWidth, roiHeight));
		return(roiToReturn);
	}



	/**
	 * This method will return the Roi of the image, where the plate's plastic borders were found.
	 * The image is assumed to be already rotated.
	 * @param originalImage
	 * @return
	 */
	private static Roi findPlatePlasticBorders2(ImagePlus originalImage) {

		//1. make image grayscale
		ImageConverter imageConverter = new ImageConverter(originalImage);
		imageConverter.convertToGray8();

		int height = originalImage.getHeight();
		int width = originalImage.getWidth();



		//get a horizontal section of the plate
		originalImage.setRoi(new Rectangle(0, 1000, width, 1000));
		originalImage.copy(false);
		ImagePlus horizontalSection = ImagePlus.getClipboard();


		//get a vertical section of the plate
		originalImage.setRoi(new Rectangle(1000, 0, 1000, height));
		originalImage.copy(false);
		ImagePlus verticalSection = ImagePlus.getClipboard();


		//2. get sum of rows/columns
		ArrayList<Integer> sumOfColumns = sumOfColumns(horizontalSection);
		ArrayList<Integer> sumOfRows = sumOfRows(verticalSection);

		//3. get only the sums that correspond to the middle of the plate, get their means
		int indexOfLeftBorder = getIndexOfMaximumElement(sumOfColumns.subList(0, width/2));
		int indexOfRightBorder = getIndexOfMaximumElement(sumOfColumns.subList(width/2, width));

		int indexOfTopBorder = getIndexOfMaximumElement(sumOfRows.subList(0, height/2));
		int indexOfBottomBorder = getIndexOfMaximumElement(sumOfColumns.subList(height/2, height));



		int roiX = indexOfLeftBorder;
		int roiY = indexOfTopBorder;
		int roiWidth = indexOfRightBorder+width/2 - indexOfLeftBorder;
		int roiHeight = indexOfBottomBorder+height/2 - indexOfTopBorder;


		Roi roiToReturn = new Roi(new Rectangle(roiX, roiY, roiWidth, roiHeight));
		return(roiToReturn);		
	}


	/**
	 * This method simply iterates through this array and finds the index
	 * of the largest element
	 */
	private static int getIndexOfMinimumElement(List<Integer> list) {
		int index = -1;
		float min = Float.MAX_VALUE;

		for (int i = 0; i < list.size(); i++) {
			if(list.get(i)<min){
				min = list.get(i);
				index = i;
			}
		}

		return(index);
	}


	/**
	 * This method simply iterates through this array and finds the index
	 * of the smallest element
	 */
	private static int getIndexOfMaximumElement(List<Integer> list) {
		int index = -1;
		float max = -Float.MAX_VALUE;

		for (int i = 0; i < list.size(); i++) {
			if(list.get(i)>max){
				max = list.get(i);
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
	static double getMean(ArrayList<Integer> list){

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
