/*
 *  Brno University of Technology
 *
 *  Copyright (C) 2009-2010 by Brno University of Technology and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://spl.utko.feec.vutbr.cz
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package cz.vutbr.feec.imageprocessing.imagej.pluginsAndFilters;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

// TODO: Auto-generated Javadoc
/**
 * The Class BinaryFillHoles.
 */
public class BinaryFillHoles implements PlugInFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		// TODO Metoda Fill neni doposud implementovana
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg0, ImagePlus arg1) {
		return 0;
	}

}
