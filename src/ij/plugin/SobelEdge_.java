/*
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

public class SobelEdge_ implements PlugInFilter {
	private int dirx = 0;
	private int diry = 0;

	public void run(ImageProcessor ip) {
		// X
		// int mtx[] = new int[] { 1, 2, 1, 0, 0, 0, -1, -2, -1 };
		// Y
		// int mtx[] = new int[] { 1, 0, -1, 2, 0, -2, 1, 0, -1 };
		//
		// int mtx[] = new int[] { -1, 0, 1, -2, 0, 2, -1, 0, 1 };
		// Scharr

		int mtx[] = null;

		if (diry == 0) {
			mtx = new int[] { 3, 0, -3, 10, 0, -10, 3, 0, -3 };
		} else if (dirx == -diry && dirx != 0) {
			mtx = new int[] { 0, -3, -10, 3, 0, -3, 10, 3, 0 };
		} else if (dirx == diry && dirx != 0) {
			mtx = new int[] { 10, 3, 0, 3, 0, -3, 0, -3, -10 };
		} else {
			// hleda hrany v y
			mtx = new int[] { 3, 10, 3, 0, 0, 0, -3, -10, -3 };

		}
		ip.convolve3x3(mtx);
	}

	public void setup(int p_dirx, int p_diry) {
		dirx = (int) Math.signum(p_dirx);
		diry = (int) Math.signum(p_diry);
	}

	@Override
	public int setup(String conf, ImagePlus im) {
		return DOES_ALL;
	}
}