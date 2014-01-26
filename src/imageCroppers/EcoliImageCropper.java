/**
 * 
 */
package imageCroppers;

import java.awt.Rectangle;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageConverter;

/**
 *This class provides methods to crop the original picture so as to keep only the colonies.
 *This specific image cropper has been customized to work with the E.coli screen pictures.
 *
 */
public class EcoliImageCropper {

	/**
	 * This method will detect the plate (plastic) borders, by detecting the first foreground sum of row/column.
	 * By foreground here, I denote sums of rows/columns that are above the mean sum of row/column, which has been calculated using
	 * only the middle of the plate.
	 * This makes it more robust than just searching for the global maxima.
	 * @param originalImage
	 * @return
	 */
	public static ImagePlus cleverCropPlate(ImagePlus originalImage){
		
		//find plate borders (where the colonies start) and return the Roi that these correspond to
		Roi rectangle = findPlateBorders(originalImage);
		originalImage.setRoi(rectangle);
		originalImage.copy(false);//copy to the internal clipboard
		
		//copy to a new picture
		ImagePlus croppedImage = ImagePlus.getClipboard();
		return(croppedImage);
	}



	/**
	 * This method will return the Roi of the image, where the colonies start 
	 * @param originalImage
	 * @return
	 */
	private static Roi findPlateBorders(ImagePlus originalImage) {
		
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
		int columnsStartArea = width/2 - 100;
		int columnsEndArea = width/2 + 100;
		//get the sublist of those sums
		ArrayList<Integer> sublistColumns = new ArrayList<Integer>(sumOfColumns.subList(columnsStartArea, columnsEndArea));
		//get their mean
		int meanOfCenterColumns = (int)Math.round(getMean(sublistColumns));
		
		//define from where to where to get the sums of brightness (in the sawtooth pattern)
		int rowsStartArea = height/2 - 50;
		int rowsEndArea = height/2 + 50;
		//get the sublist of those sums
		ArrayList<Integer> sublistRows = new ArrayList<Integer>(sumOfRows.subList(rowsStartArea, rowsEndArea));
		//get their mean
		int meanOfCenterRows = (int)Math.round(getMean(sublistRows));
		
		//4. get the index of the 20th element above the mean, this is the plate's plastic bounds
		ArrayList<Integer> indicesOfColumnsSumsAboveMean = getIndicesAboveMean(sumOfColumns, meanOfCenterColumns);
		ArrayList<Integer> indicesOfRowsSumsAboveMean = getIndicesAboveMean(sumOfRows, meanOfCenterRows);
		
		/**
		 * how many rows/columns that were found to be above the average to skip before reporting that this is the plate border
		 */
		int skip = 20;
		
		//get the 20th from left and the 20th from the right
		int indexOfLeftBorder = indicesOfColumnsSumsAboveMean.get(skip);
		int indexOfRightBorder = indicesOfColumnsSumsAboveMean.get(indicesOfColumnsSumsAboveMean.size()-skip);
		
		//get the 20th from the top and the 20th from the bottom
		int indexOfTopBorder = indicesOfRowsSumsAboveMean.get(skip);
		int indexOfBottomBorder = indicesOfRowsSumsAboveMean.get(indicesOfRowsSumsAboveMean.size()-skip);
		
		
		//5. add a fixed number of pixels (40) that correspond to the distance between the plate's plastic and the colonies start 
		int indexOfLeftColoniesStart = indexOfLeftBorder + 70;
		int indexOfRightColoniesStart = indexOfRightBorder - 70;
		int indexOfTopColoniesStart = indexOfTopBorder + 40;
		int indexOfBottomColoniesStart = indexOfBottomBorder - 40;
		
		
		int roiX = indexOfLeftColoniesStart;
		int roiY = indexOfTopColoniesStart;
		int roiWidth = indexOfRightColoniesStart - indexOfLeftColoniesStart;
		int roiHeight = indexOfBottomColoniesStart - indexOfTopColoniesStart;
		
		
		Roi roiToReturn = new Roi(new Rectangle(roiX, roiY, roiWidth, roiHeight));
		return(roiToReturn);		
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
