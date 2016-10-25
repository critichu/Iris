/**
 * 
 */
package iris.test;

import iris.tileReaders.MorphologyTileReader;

import java.awt.Point;
import java.util.ArrayList;

/**
 * @author George Kritikos
 *
 */
public class TestCircleOutput {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int radius = 9;
		Point center = new Point(0, 0);
		
		
		ArrayList<Point> circlePoints = MorphologyTileReader.getCircleCoordinates(center, radius);
		
		for (Point point : circlePoints) {
			System.out.println(point.x + "\t" + point.y);
		}

	}

}
