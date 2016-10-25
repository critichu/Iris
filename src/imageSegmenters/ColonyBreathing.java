/**
 * 
 */
package imageSegmenters;

import gui.IrisFrontend;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import imageSegmenterInput.BasicImageSegmenterInput;
import imageSegmenterOutput.BasicImageSegmenterOutput;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * This class implements methods that redefine the output of an existing image segmenter,
 * by giving each tile the chance to redefine it's borders, using the initial assessment of
 * another image segmenter (like RisingTideSegmenter).
 * @author George Kritikos
 *
 */
public class ColonyBreathing {

	/**
	 * This variable holds the maximum distance a boundary can travel from it's original location
	 * (calculated by means of another image segmenter). This value is +/-
	 */
	public static int breathingSpace = 15;


	/**
	 * This function will redefine the borders of every tile
	 * @param originalSegmentation - the output of an existing segmenter
	 * @param input - holds the original picture
	 * @return
	 */
	public static BasicImageSegmenterOutput segmentPicture(BasicImageSegmenterOutput originalSegmentation, BasicImageSegmenterInput input){

		//don't re-adjust colony borders if user has made the cropping
		if(IrisFrontend.singleColonyRun==true){
			return(originalSegmentation);
		}

		try{

			//create a copy of the output
			BasicImageSegmenterOutput output = new BasicImageSegmenterOutput();
			output.ROImatrix = originalSegmentation.copyRoiMatrix();

			//for all rows except the last one
			for(int i=0;i<input.settings.numberOfRowsOfColonies-1; i++){
				//for all columns except the last one
				for (int j = 0; j < input.settings.numberOfColumnsOfColonies-1; j++) {



					Roi newRoi = colonyBreathe(output.ROImatrix[i][j], input.imageToSegment);



					//update bottom one's top (also change it's height)
					//bottom one is on the next row, so i+1
					//the new top of the tile just under the current tile is where the old one was 
					//plus the difference from the new height to the previous height 
					//e.g. making this tile higher should increase the bottom one's y
					//conversely, making this tile shorter should bring the bottom one higher
					int heightDifference = newRoi.getBounds().height - output.ROImatrix[i][j].getBounds().height;				

					int bottomsNewTop = output.ROImatrix[i+1][j].getBounds().y + heightDifference;
					int bottomsNewHeight = output.ROImatrix[i+1][j].getBounds().height - heightDifference; //just because we want to keep the bottom's bottom where it was

					int x = output.ROImatrix[i+1][j].getBounds().x;
					int y = bottomsNewTop;
					int width = output.ROImatrix[i+1][j].getBounds().width;
					int height = bottomsNewHeight;

					output.ROImatrix[i+1][j] = new Roi(new Rectangle(x, y, width, height));

					//in a similar fashion, update right one's left (also change it's width)
					//right one is on the next column, so j+1
					int widthDifference = newRoi.getBounds().width - output.ROImatrix[i][j].getBounds().width;				

					int rightsNewLeft = output.ROImatrix[i][j+1].getBounds().x + widthDifference;
					int rightsNewWidth = output.ROImatrix[i][j+1].getBounds().width - widthDifference; //just because we want to keep the bottom's bottom where it was

					x = rightsNewLeft;
					y = output.ROImatrix[i][j+1].getBounds().y;
					width = rightsNewWidth;
					height = output.ROImatrix[i][j+1].getBounds().height;

					output.ROImatrix[i][j+1] = new Roi(new Rectangle(x, y, width, height));


					//now update also our current tile
					output.ROImatrix[i][j] = newRoi;

				}
			}

			return output;
		}
		catch(Exception e){
			//in case something goes wrong, fall back to the non-breathing segmentation
			return(originalSegmentation);
		}
	}



	/**
	 * This function takes a picture and draws lines in the coordinates of the rows and columns given as arguments.
	 * This function is customized to draw the boundaries of tiles that have gone through the breathing process.
	 * It will work also with the output of other image segmenters, but it's not tested, better use the output of the
	 * specific image segmenter.
	 * @param croppedImage
	 * @param segmenterOutput
	 */
	public static ImagePlus paintSegmentedImage(ImagePlus input_croppedImage, BasicImageSegmenterOutput segmenterOutput) {

		ImagePlus paintedImage = input_croppedImage.duplicate();

		ImageProcessor croppedImageProcessor = paintedImage.getProcessor();		
		croppedImageProcessor.setColor(java.awt.Color.WHITE);

		int numberOfRows = segmenterOutput.ROImatrix.length;
		int numberOfColumns = segmenterOutput.ROImatrix[0].length;


		//for all rows
		for(int i=0;i<numberOfRows; i++){
			//for all columns
			for (int j = 0; j < numberOfColumns; j++) {


				int y1 = segmenterOutput.ROImatrix[i][j].getBounds().y;
				int y2 = y1 + segmenterOutput.ROImatrix[i][j].getBounds().height;
				int x1 = segmenterOutput.ROImatrix[i][j].getBounds().x;
				int x2 = x1 + segmenterOutput.ROImatrix[i][j].getBounds().width;


				//draw the tile's borders:
				//1. draw the top boundary (1px below)
				croppedImageProcessor.drawLine(x1, y1+1, x2, y1+1);
				//2. draw the bottom boundary (1px above)
				croppedImageProcessor.drawLine(x1, y2-1, x2, y2-1);

				//3. draw the left boundary (1px to the right)
				croppedImageProcessor.drawLine(x1+1, y1, x1+1, y2);
				//4. draw the right boundary (1px to the left)
				croppedImageProcessor.drawLine(x2-1, y1, x2-1, y2);

			}

		}


		return(paintedImage);
	}



	/**
	 * This method takes one rectangle and recalculates the best position for it's bottom and it's
	 * right side
	 * @param originalTileRectangle
	 * @param bigPicture - the complete grayscaled picture
	 * @return
	 */
	private static Roi colonyBreathe(Roi originalTileRectangle, ImagePlus bigPicture){

		//first calculate the boundaries within which we will sum up brightnesses
		int left = originalTileRectangle.getBounds().x;
		int oldRight = left + originalTileRectangle.getBounds().width;
		int top = originalTileRectangle.getBounds().y;
		int oldBottom = top + originalTileRectangle.getBounds().height;


		//first calculate the bottom boundary, then the right boundary
		int newBottom = colonyBreatheBotom(left, oldRight, oldBottom, bigPicture) + oldBottom;
		int newRight = colonyBreatheRight(top, newBottom, oldRight, bigPicture) + oldRight; //use the new bottom

		int newHeight = newBottom - top;
		int newWidth = newRight - left;



		//calculate the new colony Roi
		Roi newRoi = new Roi(new Rectangle(left, top, newWidth, newHeight));
		return(newRoi);
	}



	/**
	 * This function sums up the brightnesses to find the correct bottom boundary
	 * @param left
	 * @param right
	 * @param initialBottom
	 * @param bigPicture
	 * @return the y of the new bottom of the tile
	 */
	private static int colonyBreatheBotom(int left, int right, int initialBottom, ImagePlus bigPicture){
		try{
			//first get the sums of brightnesses
			ArrayList<Integer> sumOfRows_ = sumOfRows(bigPicture, left, right, initialBottom);
			//then, find the minimum
			int minimumBrightnessSumLocation = getIndexOfMinimumElement(sumOfRows_);
			//that's your new bottom
			return(minimumBrightnessSumLocation - breathingSpace);
		}
		catch(Exception e){
			//do nothing
		}
		return(initialBottom);
	}


	/**
	 * This function sums up the brightnesses to find the correct right boundary
	 * @param left
	 * @param right
	 * @param initialBottom
	 * @param bigPicture
	 * @return the y of the new bottom of the tile
	 */
	private static int colonyBreatheRight(int top, int bottom, int initialRight, ImagePlus bigPicture){
		try{
			//first get the sums of brightnesses
			ArrayList<Integer> sumOfColumns_ = sumOfColumns(bigPicture, top, bottom, initialRight);
			//then, find the minimum
			int minimumBrightnessSumLocation = getIndexOfMinimumElement(sumOfColumns_);
			//that's your new bottom
			return(minimumBrightnessSumLocation - breathingSpace);
		}
		catch(Exception e){
			//do nothing
		}
		return(initialRight);
	}



	/**
	 * Takes the grayscale cropped image and calculates the sum of the
	 * light intensity of it's columns (for every x)
	 * @param croppedImage
	 * @return
	 */
	private static ArrayList<Integer> sumOfRows(ImagePlus croppedImage, int left, int right, int initialBottom ){
		//int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows

		ArrayList<Integer> sumOfRows = new ArrayList<Integer>(2*breathingSpace+1);

		int sum = 0;

		//for all rows
		for(int y=initialBottom-breathingSpace; y<=initialBottom+breathingSpace; y++ ){
			sum = 0;

			//for all columns
			for(int x=left; x<=right; x++ ){

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
	private static ArrayList<Integer> sumOfColumns(ImagePlus croppedImage, int top, int bottom, int initialRight){
		//int dimensions[] = croppedImage.getDimensions();

		//make the sum of rows and columns
		ArrayList<Integer> sumOfColumns = new ArrayList<Integer>(2*breathingSpace+1);

		int sum = 0;

		//for all columns
		for(int x=initialRight-breathingSpace; x<=initialRight+breathingSpace; x++ ){
			sum = 0;

			//for all rows
			for(int y=top; y<=bottom; y++ ){

				sum += croppedImage.getPixel(x, y)[0];
			}

			//sum complete, add it to the list
			//sumOfColumns.set(x, sum);
			sumOfColumns.add(sum);
		}

		return(sumOfColumns);
	}


	/**
	 * This method simply iterates through this array and finds the index
	 * of the smallest element
	 */
	private static int getIndexOfMinimumElement(ArrayList<Integer> list) {
		int index = -1;
		int min = Integer.MAX_VALUE;

		for (int i = 0; i < list.size(); i++) {
			if(list.get(i)<min){
				min = list.get(i);
				index = i;
			}
		}

		return(index);
	}

}
