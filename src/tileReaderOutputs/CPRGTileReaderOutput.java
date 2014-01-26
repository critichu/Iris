/**
 * 
 */
package tileReaderOutputs;

/**
 * @author George Kritikos
 *
 */
public class CPRGTileReaderOutput extends BasicTileReaderOutput {

	/**
	 * the sum of relative color intensities for the colony in the tile
	 * note that the number here is already normalized for the size of the tile
	 * (i.e. it is a mean of the weighted color intensity per pixel) 
	 */
	public int colorSumInColony = 0;
	
	/**
	 * the sum of relative color intensities for the entire given tile
	 * note that the number here is already normalized for the size of the tile
	 * (i.e. it is a mean of the weighted color intensity per pixel) 
	 */
	public int colorSumInTile = 0;

	
	public CPRGTileReaderOutput(){
	}
	

	public CPRGTileReaderOutput(int colorSumIncolony_, int colorSumInTile_){
		colorSumInColony = colorSumIncolony_;
		colorSumInTile = colorSumInTile_;
	}
	
	/**
	 * Creates a copy of this object
	 * @param that
	 */
	public CPRGTileReaderOutput(CPRGTileReaderOutput that){
		colorSumInColony = that.colorSumInColony;
		colorSumInTile = that.colorSumInTile;
	}
	
	/**
	 * This helper function will extract all the colony color sums from the given 2D array and
	 * extract them as a 2D array of integers
	 * @param cprgOutputArray
	 * @return
	 */
	public static int[][] getAllColonyColorSums(CPRGTileReaderOutput[][] cprgOutputArray){
		
		int[][] output = new int[cprgOutputArray.length][cprgOutputArray[0].length];
		
		for (int i = 0; i < cprgOutputArray.length; i++) {
			for (int j = 0; j < cprgOutputArray[0].length; j++) {
				output[i][j] = cprgOutputArray[i][j].colorSumInColony;
			}
		}
		
		return(output);
	}
	
	
	/**
	 * This helper function will extract all the tile color sums from the given 2D array and
	 * extract them as a 2D array of integers
	 * @param cprgOutputArray
	 * @return
	 */
	public static int[][] getAllTileColorSums(CPRGTileReaderOutput[][] cprgOutputArray){
		
		int[][] output = new int[cprgOutputArray.length][cprgOutputArray[0].length];
		
		for (int i = 0; i < cprgOutputArray.length; i++) {
			for (int j = 0; j < cprgOutputArray[0].length; j++) {
				output[i][j] = cprgOutputArray[i][j].colorSumInTile;
			}
		}
		
		return(output);
	}
	
	
}
