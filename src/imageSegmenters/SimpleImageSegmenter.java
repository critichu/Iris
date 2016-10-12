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
import settings.BasicSettings;


/**
 * This class holds methods that implement a simple image segmenter that will just cut the picture into equal pieces
 * given the number of desired rows and columns, as well as the desired image padding (which is known for 384 plates)
 *
 */
public class SimpleImageSegmenter {

	/**this is used for image padding, so that we leave some space before the colonies actually start*/
	public static int offset = 20; //20 is actually used for UCSF E.coli experiments (e.g. CPRG)




	/**
	 * This function will segment the picture according to it's size and the number of rows and columns
	 * that it should have at the end.
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

		//Toolbox.savePicture(croppedImage, "dummy1234.jpg");

		//1. copy original picture
		//since this pipeline needs a black and white version of the picture
		//we copy the original picture here, so that we don't tamper with the original cropped picture
		ImagePlus BW_croppedImage = croppedImage.duplicate();
		int imageHeight = BW_croppedImage.getHeight();


		//2. calculate the nominal distance between rows and columns
		int nominalDistance = (int) Math.round( Math.floor( (imageHeight-2*offset)/settings.numberOfRowsOfColonies ) );




		//5. check how many minima did rising tide return
		//no need here

		//6. sort the rows and columns found
		//no need here

		//7. check whether the rows and columns are too closely spaced, continue in case of incorrectly spaced columns
		//no need here


		//8. return the ROIs found
		output.ROImatrix = new Roi[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){			

			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				output.ROImatrix[i][j] = new Roi( 
						/*x*/ offset + j*nominalDistance,
						/*y*/ offset + i*nominalDistance,
						/*width*/ nominalDistance,
						/*height*/ nominalDistance);
			}
		}

		return(output);
	}

	/**
	 * This function will segment the picture according to it's size and the number of rows and columns
	 * that it should have at the end.
	 * The input should be a BasicImageSegmenterInput, initialized with a grayscaled, cropped
	 * picture of the plate, as well as a corresponding settings object.
	 * @param input
	 * @return
	 */
	public static BasicImageSegmenterOutput segmentPicture_width(BasicImageSegmenterInput input){



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

		//Toolbox.savePicture(croppedImage, "dummy1234.jpg");

		//1. copy original picture
		//since this pipeline needs a black and white version of the picture
		//we copy the original picture here, so that we don't tamper with the original cropped picture
		ImagePlus BW_croppedImage = croppedImage.duplicate();
		int imageWidth = BW_croppedImage.getWidth();


		//2. calculate the nominal distance between rows and columns
		int nominalDistance = (int) Math.round( Math.floor( (imageWidth-2*offset)/settings.numberOfColumnsOfColonies ) );




		//5. check how many minima did rising tide return
		//no need here

		//6. sort the rows and columns found
		//no need here

		//7. check whether the rows and columns are too closely spaced, continue in case of incorrectly spaced columns
		//no need here


		//8. return the ROIs found
		output.ROImatrix = new Roi[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){			

			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				output.ROImatrix[i][j] = new Roi( 
						/*x*/ offset + j*nominalDistance,
						/*y*/ offset + i*nominalDistance,
						/*width*/ nominalDistance,
						/*height*/ nominalDistance);
			}
		}

		return(output);
	}


	/**
	 * This function will segment the picture according to it's size and the number of rows and columns
	 * that it should have at the end.
	 * The input should be a BasicImageSegmenterInput, initialized with a grayscaled, cropped
	 * picture of the plate, as well as a corresponding settings object.
	 * @param input
	 * @return
	 */
	public static BasicImageSegmenterOutput segmentPicture_colonyDistance(BasicImageSegmenterInput input, int nominalColonyDistance){

		//get input values
		ImagePlus croppedImage = input.imageToSegment;
		BasicSettings settings = input.settings;

		//set up an output object
		BasicImageSegmenterOutput output = new BasicImageSegmenterOutput();

		//Toolbox.savePicture(croppedImage, "dummy1234.jpg");

		//1. copy original picture
		//since this pipeline needs a black and white version of the picture
		//we copy the original picture here, so that we don't tamper with the original cropped picture
		ImagePlus BW_croppedImage = croppedImage.duplicate();
		int imageWidth = BW_croppedImage.getWidth();


		//2. calculate the nominal distance between rows and columns
		int nominalDistance = nominalColonyDistance;




		//5. check how many minima did rising tide return
		//no need here

		//6. sort the rows and columns found
		//no need here

		//7. check whether the rows and columns are too closely spaced, continue in case of incorrectly spaced columns
		//no need here


		//8. return the ROIs found
		output.ROImatrix = new Roi[settings.numberOfRowsOfColonies][settings.numberOfColumnsOfColonies];

		//for all rows
		for(int i=0;i<settings.numberOfRowsOfColonies;i++){			

			//for all columns
			for (int j = 0; j < settings.numberOfColumnsOfColonies; j++) {

				output.ROImatrix[i][j] = new Roi( 
						/*x*/ offset + j*nominalDistance,
						/*y*/ offset + i*nominalDistance,
						/*width*/ nominalDistance,
						/*height*/ nominalDistance);
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
	 * This function takes a picture and draws lines in the coordinates of the rows and columns given as arguments
	 * @param croppedImage
	 * @param minimaBagRows
	 * @param minimaBagColumns
	 */
	public static void paintSegmentedImage(ImagePlus croppedImage, BasicImageSegmenterOutput segmenterOutput) {
		//now, all that remains is to paint the picture using imageJ
		ImageProcessor croppedImageProcessor = croppedImage.getProcessor();
		int dimensions[] = croppedImage.getDimensions();
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

		croppedImage.updateImage();
		//croppedImage.show();

	}






}
