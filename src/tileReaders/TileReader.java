/**
 * 
 */
package tileReaders;

import javax.swing.JFrame;

import settings.Settings;
import tileReaderInputs.TileReaderInput;
import tileReaderOutputs.TileReaderOutput;

/**
 * Subclasses of this class have to be first initialized using a constructor.
 * Then the method processTile() can be called. 
 * Both of these calls could also be done by a separate thread, which will write the output to the
 * mother class before ending
 * @author george
 *
 */
public abstract class TileReader {
	
	
	TileReaderInput input;
	
	
	/**
	 * This function has to be implemented by TileReader classes
	 * @param input
	 * @return
	 */
	public TileReader(TileReaderInput input_) {
		input = input_;
	}
	
	
	public abstract TileReaderOutput processTile();
	
	
}
