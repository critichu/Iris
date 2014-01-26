/**
 * 
 */
package imageSegmenterOutput;

import ij.gui.Roi;

import java.awt.Rectangle;

/**
 * @author george
 *
 */
public class BasicImageSegmenterOutput {

	/**
	 * This matrix holds the ROIs that are calculated using the segmentation algorithm.
	 */
	public Roi [][] ROImatrix;
	
	
	
	
	
	//the rest of the class has to do with error handling
	
	/**
	 * This value is true if something went wrong during the execution of the sementation algorithm
	 */
	public boolean errorOccurred = false;
	
	
	/**
	 * If not enough rows were found during the segmentation, this flag is raised
	 */
	public boolean notEnoughRowsFound = false;
	
	
	/**
	 * If not enough columns were found during the segmentation, this flag is raised
	 */
	public boolean notEnoughColumnsFound = false;
	
	/**
	 * If rows are too close or too far spaced appart, this will be true
	 */
	public boolean incorrectRowSpacing = false;
	
	
	/**
	 * If columns are too close or too far spaced appart, this will be true
	 */
	public boolean incorrectColumnSpacing = false;
	
	
	/**
	 * This function will return the very first ROI
	 * @return
	 */
	public Roi getTopLeftRoi(){
		return(ROImatrix[0][0]);
	}
	
	
	/**
	 * This function will return the very last ROI
	 * @return
	 */
	public Roi getBottomRightRoi(){
		int rows = ROImatrix.length;
		int columns = ROImatrix[0].length;
		
		return(ROImatrix[rows-1][columns-1]);
	}
	
	
	
	/**
	 * This function will return a copy of the Roi matrix
	 * @return
	 */
	public Roi[][] copyRoiMatrix(){
		
		Roi[][] copyOfRoi = new Roi[ROImatrix.length][ROImatrix[0].length];
		
		for(int i = 0; i < ROImatrix.length; i++){
			
//			copyOfRoi[i] = ROImatrix[i].clone();
			
			for(int j = 0; j < ROImatrix[i].length; j++){
				copyOfRoi[i][j] = new Roi(new Rectangle(ROImatrix[i][j].getBounds()));;
			}
		}
		
		return(copyOfRoi);
	}
	
	
}
