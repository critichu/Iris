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

/***
 * Author: Martin Hasmanda, Brno University of Technology, FEEC, Department of telecommunications
 * 
 * Implementace Diskretni waveletove paketove dekompozice
 *  * 
 * pouziti:
 *  
 * Type of wave:	typ waveletu: 	haar
 * Path: 			cesta:			LH,HH, ...									
 * 
 * Level 1 (LH)						Level 2	(HH)
 * ___________________				___________
 *|			|	 	  |				 | LL | LH |
 *|	  LL  	|  (LH)	  |	-->			 |____|____|
 *|			|	 	  |				 | HL |(HH)|	-->		....
 *|_________|_________|				_|____|____|
 *|			|		  |						   |
 *|	  HL	|	HH 	  | 					
 *|	  		|		  |
 *|_________|_________|		
 *
 * Vysledny obraz je zvetsen na rozmery puvodniho obrazu
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;

/**
 * The Class Wavelet_Decompositon.
 */
public class Wavelet_Decompositon implements ExtendedPlugInFilter,
		DialogListener {

	/**
	 * The Class Convolution2D.
	 */
	public static class Convolution2D {

		/**
		 * The Enum e_conv_border.
		 */
		public enum e_conv_border {

			/** The EC b_ zeros. */
			ECB_ZEROS,

			/** The EC b_ border. */
			ECB_BORDER
		}

		// specify type of border
		/**
		 * The Enum e_conv_shape.
		 */
		public enum e_conv_shape {

			/** The EC s_ full. */
			ECS_FULL,

			/** The EC s_ shape. */
			ECS_SHAPE,

			/** The EC s_ valid. */
			ECS_VALID
		};

		/**
		 * The Enum e_filter_type.
		 */
		public enum e_filter_type {

			/** The EF t_ rows. */
			EFT_ROWS,

			/** The EF t_ cols. */
			EFT_COLS
		};

		/**
		 * The Enum e_sample_dir.
		 */
		public enum e_sample_dir {

			/** The E s_ horizontal. */
			ES_HORIZONTAL,

			/** The E s_ vertical. */
			ES_VERTICAL
		}

		// constructor
		/**
		 * Instantiates a new convolution2 d.
		 */
		public Convolution2D() {
		}

		// simple method for applying 2D convolution of array input by kernel,
		// output is on base by shape
		/**
		 * Convolution_2 d.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param shape
		 *            the shape
		 * 
		 * @return the float[][]
		 */
		public float[][] Convolution_2D(float[][] input, float[] kernel,
				e_conv_shape shape) {
			// test of empty
			if (input == null || kernel == null)
				return null;

			// get kernel sub size for expanding input data
			int extended = kernel.length - 1;
			int ext = 0, begin = 0;

			if (shape == e_conv_shape.ECS_FULL) {
				ext = extended;
			} else if (shape == e_conv_shape.ECS_SHAPE) {
				begin = kernel.length / 2;
			}

			// create result data
			float[][] result = new float[input.length][input[0].length + ext];

			// convolution input by vector
			int i, j, k;
			float sum;

			for (k = 0; k < input.length; k++) // rows
			{
				for (i = begin; i < input[0].length + ext + begin; i++) // columns
				{
					sum = 0;
					for (j = 0; j < kernel.length; j++) { // kernel size
						if (kernel.length - extended - j + i > 0
								&& i - j < input[0].length)
							sum += input[k][i - j] * kernel[j];
					}
					result[k][i - begin] = sum;
				}
			}

			return result;
		}

		/**
		 * Convolution_2 d.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param shape
		 *            the shape
		 * @param border
		 *            the border
		 * 
		 * @return the float[][]
		 */
		public float[][] Convolution_2D(float[][] input, float[] kernel,
				e_conv_shape shape, e_conv_border border) {
			if (border == e_conv_border.ECB_ZEROS) {
				return Convolution_2D(input, kernel, shape);
			} else if (border == e_conv_border.ECB_BORDER) {
				// extends array
				float ext_input[][] = symetric_extend_matrix(input, kernel);
				ext_input = Convolution_2D(ext_input, kernel,
						e_conv_shape.ECS_FULL);

				// update size of matrix to original
				if (shape == e_conv_shape.ECS_SHAPE) {
					float result[][] = new float[input.length][input[0].length];
					int padd_y = (ext_input.length - result.length) / 2;
					int padd_x = (ext_input[0].length - result[0].length) / 2;
					for (int i = 0; i < result.length; i++) {
						for (int j = 0; j < result[0].length; j++) {
							result[i][j] = ext_input[i + padd_y][j + padd_x];
						}
					}
					return result;
				} else
					return ext_input;
			}

			return null;
		}

		// simple method for applying 2D convolution of array input by kernel,
		// output is on base by shape
		/**
		 * Convolution_2 d.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param shape
		 *            the shape
		 * 
		 * @return the float[][]
		 */
		public float[][] Convolution_2D(float[][] input, float[][] kernel,
				e_conv_shape shape) {
			// test of empty
			if (input == null || kernel == null)
				return null;

			int ext_w = kernel[0].length;
			int ext_h = kernel.length;

			int center_x = 1 - ext_w;
			int center_y = 1 - ext_h;

			int result_w = input[0].length + ext_w - 1;
			int result_h = input.length + ext_h - 1;

			if (shape == e_conv_shape.ECS_SHAPE) {
				// center_x = 1-(ext_w+1)/2;
				// center_y = 1-(ext_h+1)/2;
				center_x /= 2;
				center_y /= 2;

				result_w = input[0].length;
				result_h = input.length;
			}

			// create result data
			float[][] result = new float[result_h][result_w];

			// convolution input by vector
			int ii, jj, mm, nn;

			for (int i = 0; i < result.length; i++) // rows
			{
				for (int j = 0; j < result[0].length; j++) // columns
				{
					for (int m = 0; m < kernel.length; m++) { // kernel size

						mm = ext_h - m - 1;

						for (int n = 0; n < kernel[0].length; n++) {
							nn = ext_w - n - 1;

							ii = i + (m + center_y);
							jj = j + (n + center_x);

							if (ii >= 0 && ii < input.length && jj >= 0
									&& jj < input[0].length)
								result[i][j] += input[ii][jj] * kernel[mm][nn];
						}
					}
				}
			}

			return result;
		};

		/**
		 * Downsample_image.
		 * 
		 * @param original
		 *            the original
		 * @param factor
		 *            the factor
		 * @param sample_dir
		 *            the sample_dir
		 * 
		 * @return the float[][]
		 */
		public float[][] downsample_image(float[][] original, int factor,
				e_sample_dir sample_dir) {
			int i, j, m = 0, n = 0;
			int factor_cols = (sample_dir == e_sample_dir.ES_HORIZONTAL) ? factor
					: 1;
			int factor_rows = (sample_dir == e_sample_dir.ES_VERTICAL) ? factor
					: 1;

			int result_rows = original.length / factor_rows;
			int result_cols = (original[0].length / factor_cols);

			float[][] result = null;

			// update dimension
			if (original[0].length - result_cols * factor_cols > 0)
				++result_cols;
			if (original.length - result_rows * factor_rows > 0)
				++result_rows;

			result = new float[result_rows][result_cols];

			for (i = 0, m = 0; i < original.length && m < result.length; i += factor_rows, m++) {
				for (j = 0, n = 0; j < original[0].length
						&& n < result[0].length; j += factor_cols, n++) {
					result[m][n] = original[i][j];
				}
			}
			return result;
		}

		// upsample and convolution with inversion kernel
		/**
		 * Gets the sub.
		 * 
		 * @param x
		 *            the x
		 * @param g1
		 *            the g1
		 * @param g2
		 *            the g2
		 * 
		 * @return the sub
		 */
		public float[][] getSub(float[][] x, float[] g1, float[] g2) {

			float[][] result = upsample_image(x, 2, e_sample_dir.ES_VERTICAL);
			result = transpose_image(result);
			result = Convolution_2D(result, g1, e_conv_shape.ECS_SHAPE,
					e_conv_border.ECB_ZEROS);
			result = transpose_image(result);
			result = upsample_image(result, 2, e_sample_dir.ES_HORIZONTAL);
			result = Convolution_2D(result, g2, e_conv_shape.ECS_SHAPE,
					e_conv_border.ECB_ZEROS);

			// float [][] result_new = new
			// float[result.length-g1.length/2][result[0].length-g2.length/2];
			float[][] result_new = new float[result.length][result[0].length];

			for (int i = 0; i < result_new.length; i++) {
				for (int j = 0; j < result_new[0].length; j++) {
					result_new[i][j] = result[i][j];
				}
			}
			return result_new;
		}

		// remove border after extend matrix
		/**
		 * Removes the border.
		 * 
		 * @param result_size
		 *            the result_size
		 * @param input
		 *            the input
		 * 
		 * @return the float[][]
		 */
		public float[][] RemoveBorder(Dimension result_size, float[][] input) {
			// test input parameters
			if (input == null)
				return null;

			// remove at all size
			int padd_x = (input.length - result_size.height) / 2;
			int padd_y = (input[0].length - result_size.width) / 2;

			float[][] result = new float[result_size.height][result_size.width];

			int ii, jj, i, j;
			for (ii = 0, i = padd_y; ii < result.length; i++, ii++) // rows
			{
				for (jj = 0, j = padd_x; jj < result[0].length; j++, jj++) // columns
				{
					result[ii][jj] = input[i][j];
				}
			}

			return result;
		};

		// only sum two array
		/**
		 * Sum_arrays.
		 * 
		 * @param input_1
		 *            the input_1
		 * @param input_2
		 *            the input_2
		 * 
		 * @return the float[][]
		 */
		public float[][] sum_arrays(float[][] input_1, float[][] input_2) {
			if (input_1.length != input_2.length
					|| input_1[0].length != input_2[0].length)
				return null;

			float[][] result = new float[input_1.length][input_1[0].length];

			for (int i = 0; i < result.length; i++) {
				for (int j = 0; j < result[0].length; j++) {
					result[i][j] = input_1[i][j] + input_2[i][j];
				}
			}

			return result;
		}

		// convolution with kernel and downsample image
		/**
		 * Symetric_convolve.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param type
		 *            the type
		 * 
		 * @return the float[][]
		 */
		public float[][] symetric_convolve(float[][] input, float[] kernel,
				e_filter_type type) {
			float[][] result = null;

			if (type == e_filter_type.EFT_ROWS) {
				result = Convolution_2D(input, kernel, e_conv_shape.ECS_SHAPE,
						e_conv_border.ECB_BORDER);
				result = downsample_image(result, 2, e_sample_dir.ES_HORIZONTAL);
			} else {
				result = transpose_image(input);
				result = Convolution_2D(result, kernel, e_conv_shape.ECS_SHAPE,
						e_conv_border.ECB_BORDER);
				result = transpose_image(result);
				result = downsample_image(result, 2, e_sample_dir.ES_VERTICAL);
			}

			return result;
		}

		// upsample
		/**
		 * Symetric_convolve2.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param type
		 *            the type
		 * 
		 * @return the float[][]
		 */
		public float[][] symetric_convolve2(float[][] input, float[] kernel,
				e_filter_type type) {
			float[][] result = null;

			if (type == e_filter_type.EFT_ROWS) {
				result = upsample_image(input, 2, e_sample_dir.ES_HORIZONTAL);
				result = Convolution_2D(result, kernel, e_conv_shape.ECS_SHAPE);
			} else {
				result = upsample_image(input, 2, e_sample_dir.ES_VERTICAL);
				result = transpose_image(result);
				result = Convolution_2D(result, kernel, e_conv_shape.ECS_SHAPE);
				result = transpose_image(result);
			}

			return result;
		}

		// extends matrix to result matrix
		/**
		 * Symetric_extend_matrix.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * 
		 * @return the float[][]
		 */
		public float[][] symetric_extend_matrix(float[][] input, float[] kernel) {
			// test of empty
			if (input == null || kernel == null)
				return null;

			float[][] result = null;

			int padd_x = kernel.length - 1;
			int padd_y = kernel.length - 1;

			result = new float[input.length + padd_x * 2][input[0].length
					+ padd_y * 2];

			int i, j;

			// copy old data to new data
			for (i = 0; i < input.length; i++) {
				for (j = 0; j < input[0].length; j++) {
					result[i + padd_y][j + padd_x] = input[i][j];
				}
			}

			// copy rows
			for (i = 0; i < padd_y; i++) {
				for (j = padd_x; j < result[0].length - padd_x; j++) {
					result[i][j] = result[padd_y][j];
				}
			}

			for (i = result.length - 1; i > result.length - padd_y - 1; i--) {
				for (j = padd_x; j < result[0].length - padd_x; j++) {
					result[i][j] = result[result.length - padd_y - 1][j];
				}
			}

			// copy columns
			for (i = 0; i < padd_x; i++) {
				for (j = 0; j < result.length; j++) {
					result[j][i] = result[j][padd_x];
				}
			}

			for (i = result[0].length - 1; i > result[0].length - padd_x - 1; i--) {
				for (j = 0; j < result.length; j++) {
					result[j][i] = result[j][result[0].length - padd_x - 1];
				}
			}

			return result;
		}

		// transpose matrix
		/**
		 * Transpose_image.
		 * 
		 * @param input
		 *            the input
		 * 
		 * @return the float[][]
		 */
		public float[][] transpose_image(float[][] input) {
			int i, j;

			float[][] result = new float[input[0].length][input.length];

			for (i = 0; i < input.length; i++) {
				for (j = 0; j < input[0].length; j++) {
					result[j][i] = input[i][j];
				}
			}

			return result;
		}

		/**
		 * Upsample_image.
		 * 
		 * @param original
		 *            the original
		 * @param factor
		 *            the factor
		 * @param sample_dir
		 *            the sample_dir
		 * 
		 * @return the float[][]
		 */
		public float[][] upsample_image(float[][] original, int factor,
				e_sample_dir sample_dir) {
			int i, j, m = 0, n = 0;
			int factor_cols = (sample_dir == e_sample_dir.ES_HORIZONTAL) ? factor
					: 1;
			int factor_rows = (sample_dir == e_sample_dir.ES_VERTICAL) ? factor
					: 1;

			int result_rows = original.length * factor_rows;
			int result_cols = (original[0].length * factor_cols);

			float[][] result = null;

			// update dimension
			if (original[0].length - result_cols * factor_cols > 0)
				++result_cols;
			if (original.length - result_rows * factor_rows > 0)
				++result_rows;

			result = new float[result_rows][result_cols];

			for (i = 0, m = 0; i < original.length * factor_rows
					&& m < result.length; i++, m += factor_rows) {
				for (j = 0, n = 0; j < original[0].length * factor_cols
						&& n < result[0].length; j++, n += factor_cols) {
					result[m][n] = original[i][j];
				}
			}
			return result;
		}

	};

	/**
	 * The Class UnsignedChar.
	 */
	static class UnsignedChar {

		/**
		 * Byte to float.
		 * 
		 * @param val
		 *            the val
		 * 
		 * @return the float
		 */
		static float byteTofloat(byte val) {
			return (short) ((int) (0x000000FF & ((int) val)));
		}
	};

	/**
	 * The Class WaveletDecomposition.
	 */
	public static class WaveletDecomposition {
		/**
		 * The Enum e_wpt_type.
		 */
		public enum e_wpt_type {

			/** The EWP t_ ll. */
			EWPT_LL,

			/** The EWP t_ lh. */
			EWPT_LH,

			/** The EWP t_ hl. */
			EWPT_HL,

			/** The EWP t_ hh. */
			EWPT_HH
		}

		// recognition for rapidminer
		/**
		 * The Class WindowType.
		 */
		public class WindowType {

			/** The window name. */
			public String windowName;

			/** The window type. */
			public e_wpt_type windowType;

			/**
			 * Instantiates a new window type.
			 * 
			 * @param name
			 *            the name
			 * @param type
			 *            the type
			 */
			public WindowType(String name, e_wpt_type type) {
				this.windowName = name;
				this.windowType = type;
			}
		}

		/** The m_windows. */
		private WindowType[] m_windows = {
				new WindowType("LL", e_wpt_type.EWPT_LL),
				new WindowType("LH", e_wpt_type.EWPT_LH),
				new WindowType("HL", e_wpt_type.EWPT_HL),
				new WindowType("HH", e_wpt_type.EWPT_HH) };

		/**
		 * Decomposition.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param level
		 *            the level
		 * @param type
		 *            the type
		 * 
		 * @return the float[][]
		 */
		public float[][] Decomposition(float[][] input, float[][] kernel,
				int level, e_wpt_type type) {

			if (input == null || kernel == null)
				return null;

			// (1) computing level
			float[][] result = new float[input.length / (2 * (level + 1))][input[0].length
					/ (2 * (level + 1))];

			Convolution2D conv = new Convolution2D();

			// prepare image
			float[][] tmp = conv.symetric_extend_matrix(input, kernel[0]);

			for (int i = 0; i < level; i++) {
				// LL
				tmp = conv.symetric_convolve(conv.symetric_convolve(tmp,
						kernel[0], Convolution2D.e_filter_type.EFT_ROWS),
						kernel[0], Convolution2D.e_filter_type.EFT_COLS);
			}

			// get type
			if (type == e_wpt_type.EWPT_LL || type == e_wpt_type.EWPT_LH) {
				float[][] rows = conv.symetric_convolve(tmp, kernel[0],
						Convolution2D.e_filter_type.EFT_ROWS);
				if (type == e_wpt_type.EWPT_LL)
					result = conv.symetric_convolve(rows, kernel[0],
							Convolution2D.e_filter_type.EFT_COLS);
				else
					result = conv.symetric_convolve(rows, kernel[1],
							Convolution2D.e_filter_type.EFT_COLS);
			} else if (type == e_wpt_type.EWPT_HL || type == e_wpt_type.EWPT_HH) {
				float[][] rows = conv.symetric_convolve(tmp, kernel[1],
						Convolution2D.e_filter_type.EFT_ROWS);
				if (type == e_wpt_type.EWPT_HH)
					result = conv.symetric_convolve(rows, kernel[1],
							Convolution2D.e_filter_type.EFT_COLS);
				else
					result = conv.symetric_convolve(rows, kernel[0],
							Convolution2D.e_filter_type.EFT_COLS);
			}

			return result;
		}

		/**
		 * Decomposition.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param path
		 *            the path
		 * 
		 * @return the float[][]
		 */
		public float[][] Decomposition(float[][] input, float[][] kernel,
				Vector<e_wpt_type> path) {
			// test input params
			if (path.size() == 0)
				return null;
			if (input == null || kernel == null)
				return null;

			float[][] result = input;

			Convolution2D conv = new Convolution2D();

			for (int i = 0; i < path.size(); i++) {
				Dimension result_size = new Dimension(result[0].length / 2,
						result.length / 2);
				e_wpt_type type = path.get(i);
				result = conv.symetric_extend_matrix(result, kernel[0]);
				result = GetFirstLevel(result, kernel, type); // decomposition
																// kernel
				result = conv.RemoveBorder(result_size, result);
			}

			return result;
		}

		// [sub image] [rows] [cols], sub image[] = {downsample, horizontal,
		// vertical, diagonal}
		/**
		 * Forward_trf.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * 
		 * @return the float[][][]
		 */
		public float[][][] forward_trf(float[][] input, float[][] kernel) {

			// test input data
			if (input == null || kernel == null)
				return null;

			// create result image
			float[][][] result = new float[input.length / 2][input[0].length / 2][4];

			Convolution2D conv = new Convolution2D();

			// horizontal, sample
			float[][] rows = conv.symetric_convolve(input, kernel[0],
					Convolution2D.e_filter_type.EFT_ROWS);
			// downsample
			result[0] = conv.symetric_convolve(rows, kernel[0],
					Convolution2D.e_filter_type.EFT_COLS);
			// horizontal
			result[1] = conv.symetric_convolve(rows, kernel[1],
					Convolution2D.e_filter_type.EFT_COLS);

			// diagonal, vertical
			rows = conv.symetric_convolve(input, kernel[1],
					Convolution2D.e_filter_type.EFT_ROWS);

			// vertical
			result[2] = conv.symetric_convolve(rows, kernel[0],
					Convolution2D.e_filter_type.EFT_COLS);
			// diagonal
			result[3] = conv.symetric_convolve(rows, kernel[1],
					Convolution2D.e_filter_type.EFT_COLS);

			return result;
		}

		/**
		 * Gets the first level.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * @param type
		 *            the type
		 * 
		 * @return the float[][]
		 */
		protected float[][] GetFirstLevel(float[][] input, float[][] kernel,
				e_wpt_type type) {
			if (input == null || kernel == null)
				return null;

			Convolution2D conv = new Convolution2D();

			float[][] result = null;

			// get type
			if (type == e_wpt_type.EWPT_LL || type == e_wpt_type.EWPT_LH) {
				float[][] rows = conv.symetric_convolve(input, kernel[0],
						Convolution2D.e_filter_type.EFT_ROWS);
				if (type == e_wpt_type.EWPT_LL)
					result = conv.symetric_convolve(rows, kernel[0],
							Convolution2D.e_filter_type.EFT_COLS);
				else
					result = conv.symetric_convolve(rows, kernel[1],
							Convolution2D.e_filter_type.EFT_COLS);
			} else if (type == e_wpt_type.EWPT_HL || type == e_wpt_type.EWPT_HH) {
				float[][] rows = conv.symetric_convolve(input, kernel[1],
						Convolution2D.e_filter_type.EFT_ROWS);
				if (type == e_wpt_type.EWPT_HH)
					result = conv.symetric_convolve(rows, kernel[1],
							Convolution2D.e_filter_type.EFT_COLS);
				else
					result = conv.symetric_convolve(rows, kernel[0],
							Convolution2D.e_filter_type.EFT_COLS);
			}

			return result;
		}

		/**
		 * Gets the windows.
		 * 
		 * @return the window type[]
		 */
		public WindowType[] GetWindows() {
			return m_windows;
		}
	};

	// normalize

	/**
	 * The Class Wavelets.
	 */
	public static class Wavelets {

		/**
		 * The Enum e_filter_type.
		 */
		public enum e_filter_type {

			/** The EF t_ rec. */
			EFT_REC,

			/** The EF t_ dec. */
			EFT_DEC
		}

		/**
		 * The Enum e_wave_type.
		 */
		public enum e_wave_type {

			/** The EW t_ first. */
			EWT_FIRST,
			// orthogonal wavelets

			// Daubechies
			/** The EW t_ harr. */
			EWT_HARR,

			/** The EW t_ d b_1. */
			EWT_DB_1,

			/** The EW t_ d b_2. */
			EWT_DB_2,

			/** The EW t_ d b_3. */
			EWT_DB_3,

			/** The EW t_ d b_4. */
			EWT_DB_4,

			/** The EW t_ d b_5. */
			EWT_DB_5,

			/** The EW t_ d b_6. */
			EWT_DB_6,

			/** The EW t_ d b_7. */
			EWT_DB_7,

			/** The EW t_ d b_8. */
			EWT_DB_8,

			/** The EW t_ d b_9. */
			EWT_DB_9,

			/** The EW t_ d b_10. */
			EWT_DB_10,

			// Coiflets
			/** The EW t_ coi f_1. */
			EWT_COIF_1,

			/** The EW t_ coi f_2. */
			EWT_COIF_2,

			/** The EW t_ coi f_3. */
			EWT_COIF_3,

			/** The EW t_ coi f_4. */
			EWT_COIF_4,

			/** The EW t_ coi f_5. */
			EWT_COIF_5,

			// Symlet
			/** The EW t_ sy m_1. */
			EWT_SYM_1,

			/** The EW t_ sy m_2. */
			EWT_SYM_2,

			/** The EW t_ sy m_3. */
			EWT_SYM_3,

			/** The EW t_ sy m_4. */
			EWT_SYM_4,

			/** The EW t_ sy m_5. */
			EWT_SYM_5,

			/** The EW t_ sy m_6. */
			EWT_SYM_6,

			/** The EW t_ sy m_7. */
			EWT_SYM_7,

			/** The EW t_ sy m_8. */
			EWT_SYM_8,

			// biorNr.Nd
			/** The EW t_ bio r_1_1. */
			EWT_BIOR_1_1,

			/** The EW t_ bio r_1_3. */
			EWT_BIOR_1_3,

			/** The EW t_ bio r_1_5. */
			EWT_BIOR_1_5,

			/** The EW t_ bio r_2_2. */
			EWT_BIOR_2_2,

			/** The EW t_ bio r_2_4. */
			EWT_BIOR_2_4,

			/** The EW t_ bio r_2_6. */
			EWT_BIOR_2_6,

			/** The EW t_ bio r_2_8. */
			EWT_BIOR_2_8,

			/** The EW t_ bio r_3_1. */
			EWT_BIOR_3_1,

			/** The EW t_ bio r_3_3. */
			EWT_BIOR_3_3,

			/** The EW t_ bio r_3_5. */
			EWT_BIOR_3_5,

			/** The EW t_ bio r_3_7. */
			EWT_BIOR_3_7,

			/** The EW t_ bio r_3_9. */
			EWT_BIOR_3_9,

			/** The EW t_ bio r_4_4. */
			EWT_BIOR_4_4,

			/** The EW t_ bio r_5_5. */
			EWT_BIOR_5_5,

			/** The EW t_ bio r_6_8. */
			EWT_BIOR_6_8,

			// reverse biorthogonal spline wavelet
			/** The EW t_ rbio r_1_1. */
			EWT_RBIOR_1_1,

			/** The EW t_ rbio r_1_3. */
			EWT_RBIOR_1_3,

			/** The EW t_ rbio r_1_5. */
			EWT_RBIOR_1_5,

			/** The EW t_ rbio r_2_2. */
			EWT_RBIOR_2_2,

			/** The EW t_ rbio r_2_4. */
			EWT_RBIOR_2_4,

			/** The EW t_ rbio r_2_6. */
			EWT_RBIOR_2_6,

			/** The EW t_ rbio r_2_8. */
			EWT_RBIOR_2_8,

			/** The EW t_ rbio r_3_1. */
			EWT_RBIOR_3_1,

			/** The EW t_ rbio r_3_3. */
			EWT_RBIOR_3_3,

			/** The EW t_ rbio r_3_5. */
			EWT_RBIOR_3_5,

			/** The EW t_ rbio r_3_7. */
			EWT_RBIOR_3_7,

			/** The EW t_ rbio r_3_9. */
			EWT_RBIOR_3_9,

			/** The EW t_ rbio r_4_4. */
			EWT_RBIOR_4_4,

			/** The EW t_ rbio r_5_5. */
			EWT_RBIOR_5_5,

			/** The EW t_ rbio r_6_8. */
			EWT_RBIOR_6_8,

			/** The EW t_ dmey. */
			EWT_DMEY,

			// jpeg2000
			/** The EW t_ jpe g_9_7. */
			EWT_JPEG_9_7,

			/** The EW t_ jpe g_5_3. */
			EWT_JPEG_5_3,

			//
			/** The EW t_ last. */
			EWT_LAST
		}

		// recognition for rapidminer
		/**
		 * The Class WaveletType.
		 */
		public class WaveletType {

			/** The wave name. */
			public String waveName;

			/** The wave type. */
			public e_wave_type waveType;

			/**
			 * Instantiates a new wavelet type.
			 * 
			 * @param name
			 *            the name
			 * @param type
			 *            the type
			 */
			public WaveletType(String name, e_wave_type type) {
				this.waveName = name;
				this.waveType = type;
			}
		}

		/** The SQR t_2. */
		private final float SQRT_2 = (float) 1.41421356237309504880168;

		/** The m_wavelets. */
		private WaveletType[] m_wavelets = {
				new WaveletType("haar", e_wave_type.EWT_HARR),
				new WaveletType("db1", e_wave_type.EWT_DB_1),
				new WaveletType("db2", e_wave_type.EWT_DB_2),
				new WaveletType("db3", e_wave_type.EWT_DB_3),
				new WaveletType("db4", e_wave_type.EWT_DB_4),
				new WaveletType("db5", e_wave_type.EWT_DB_5),
				new WaveletType("db6", e_wave_type.EWT_DB_6),
				new WaveletType("db7", e_wave_type.EWT_DB_7),
				new WaveletType("db8", e_wave_type.EWT_DB_8),
				new WaveletType("db9", e_wave_type.EWT_DB_9),
				new WaveletType("db10", e_wave_type.EWT_DB_10),

				// Coiflets
				new WaveletType("coif1", e_wave_type.EWT_COIF_1),
				new WaveletType("coif2", e_wave_type.EWT_COIF_2),
				new WaveletType("coif3", e_wave_type.EWT_COIF_3),
				new WaveletType("coif4", e_wave_type.EWT_COIF_4),
				new WaveletType("coif5", e_wave_type.EWT_COIF_5),

				// Symlet
				new WaveletType("sym1", e_wave_type.EWT_SYM_1),
				new WaveletType("sym2", e_wave_type.EWT_SYM_2),
				new WaveletType("sym3", e_wave_type.EWT_SYM_3),
				new WaveletType("sym4", e_wave_type.EWT_SYM_4),
				new WaveletType("sym5", e_wave_type.EWT_SYM_5),
				new WaveletType("sym6", e_wave_type.EWT_SYM_6),
				new WaveletType("sym7", e_wave_type.EWT_SYM_7),
				new WaveletType("sym8", e_wave_type.EWT_SYM_8),

				// biorNr.Nd
				new WaveletType("bior1.1", e_wave_type.EWT_BIOR_1_1),
				new WaveletType("bior1.3", e_wave_type.EWT_BIOR_1_3),
				new WaveletType("bior1.5", e_wave_type.EWT_BIOR_1_5),

				new WaveletType("bior2.8", e_wave_type.EWT_BIOR_2_2),
				new WaveletType("bior2.8", e_wave_type.EWT_BIOR_2_4),
				new WaveletType("bior2.8", e_wave_type.EWT_BIOR_2_6),
				new WaveletType("bior2.8", e_wave_type.EWT_BIOR_2_8),

				new WaveletType("bior3.1", e_wave_type.EWT_BIOR_3_1),
				new WaveletType("bior3.3", e_wave_type.EWT_BIOR_3_3),
				new WaveletType("bior3.5", e_wave_type.EWT_BIOR_3_5),
				new WaveletType("bior3.7", e_wave_type.EWT_BIOR_3_7),
				new WaveletType("bior3.9", e_wave_type.EWT_BIOR_3_9),

				new WaveletType("bior4.4", e_wave_type.EWT_BIOR_4_4),
				new WaveletType("bior5.5", e_wave_type.EWT_BIOR_5_5),
				new WaveletType("bior6.8", e_wave_type.EWT_BIOR_6_8),

				// reverse biorthogonal spline wavelet
				new WaveletType("rbior1.1", e_wave_type.EWT_RBIOR_1_1),
				new WaveletType("rbior1.3", e_wave_type.EWT_RBIOR_1_3),
				new WaveletType("rbior1.5", e_wave_type.EWT_RBIOR_1_5),

				new WaveletType("rbior2.2", e_wave_type.EWT_RBIOR_2_2),
				new WaveletType("rbior2.4", e_wave_type.EWT_RBIOR_2_4),
				new WaveletType("rbior2.6", e_wave_type.EWT_RBIOR_2_6),
				new WaveletType("rbior2.8", e_wave_type.EWT_RBIOR_2_8),

				new WaveletType("rbior3.1", e_wave_type.EWT_RBIOR_3_1),
				new WaveletType("rbior3.3", e_wave_type.EWT_RBIOR_3_3),
				new WaveletType("rbior3.5", e_wave_type.EWT_RBIOR_3_5),
				new WaveletType("rbior3.7", e_wave_type.EWT_RBIOR_3_7),
				new WaveletType("rbior3.9", e_wave_type.EWT_RBIOR_3_9),

				new WaveletType("rbior4.4", e_wave_type.EWT_RBIOR_4_4),
				new WaveletType("rbior5.5", e_wave_type.EWT_RBIOR_5_5),
				new WaveletType("rbior6.8", e_wave_type.EWT_RBIOR_6_8),

				new WaveletType("dmey", e_wave_type.EWT_DMEY),

				// jpeg2000
				new WaveletType("jpeg9.7", e_wave_type.EWT_JPEG_9_7),
				new WaveletType("jpeg5.3", e_wave_type.EWT_JPEG_5_3)

		};

		/**
		 * Add_to_reverse_array.
		 * 
		 * @param input
		 *            the input
		 * 
		 * @return the float[]
		 */
		private float[] add_to_reverse_array(float[] input) {

			if (input == null)
				return null;
			if (input.length == 0)
				return null;

			float result[] = new float[input.length * 2 + 1];

			// fist data copy
			int i, j;
			for (i = 0; i < input.length; i++) {
				result[i] = input[i];
			}

			// inverse data
			for (j = input.length - 1; j >= 0; j--, i++) {
				result[i] = input[j];
			}

			return result;
		}

		/**
		 * Add_to_reverse_array.
		 * 
		 * @param input
		 *            the input
		 * @param center
		 *            the center
		 * 
		 * @return the float[]
		 */
		private float[] add_to_reverse_array(float[] input, float center) {

			if (input == null)
				return null;
			if (input.length == 0)
				return null;

			float result[] = new float[input.length * 2 + 1];

			// fist data copy
			int i, j;
			for (i = 0; i < input.length; i++) {
				result[i] = input[i];
			}

			// center save
			result[i++] = center;

			// inverse data
			for (j = input.length - 1; j >= 0; j--, i++) {
				result[i] = input[j];
			}

			return result;
		}

		// associated filters
		/*
		 * 0,0 - Lo_R filter 0,1 - Hi_R filter 1,0 - Lo_D filter 1,1 - Hi_D
		 * filter
		 */
		/**
		 * Associate filters.
		 * 
		 * @param norm_values
		 *            the norm_values
		 * 
		 * @return the float[][][]
		 */
		protected float[][][] AssociateFilters(float[] norm_values) {

			// tests input params
			if (norm_values == null)
				return null;

			if (norm_values.length == 0)
				return null;

			// result
			float result[][][] = new float[2][2][norm_values.length];

			// Lo_R
			for (int i = 0; i < result[0][0].length; i++) {
				result[0][0][i] = norm_values[i] * SQRT_2;
			}

			// HI_R
			result[0][1] = ComputeQmf(result[0][0], 0);

			// Lo_D
			for (int i = 0; i < result[0][0].length; i++) {
				result[1][0][i] = result[0][0][result[0][0].length - i - 1];
			}

			// HI_D
			for (int i = 0; i < result[0][0].length; i++) {
				result[1][1][i] = result[0][1][result[0][0].length - i - 1];
			}

			return result;
		}

		// compute biortogonal filters
		/**
		 * Compute biorthogonal values.
		 * 
		 * @param df
		 *            the df
		 * @param rf
		 *            the rf
		 * 
		 * @return the float[][][]
		 */
		protected float[][][] ComputeBiorthogonalValues(float[] df, float[] rf) {

			if (df == null || rf == null)
				return null;

			// get length of vectors
			int length = Math.max(df.length, rf.length);
			if (length % 2 != 0)
				++length;

			float Df[] = new float[(int) Math.floor((length - df.length) / 2)
					+ df.length + (int) Math.floor((length - df.length) / 2)
					+ 1];
			float Rf[] = new float[(int) Math.floor((length - rf.length) / 2)
					+ rf.length + (int) Math.floor((length - rf.length) / 2)
					+ 1];

			// coppy array to Df and Rf
			int k = 0;
			for (int i = (int) Math.floor((length - df.length)) / 2; k < df.length; i++, k++) {
				Df[i] = df[k];
			}

			k = 0;
			for (int i = (int) Math.floor((length - rf.length)) / 2; k < rf.length; i++, k++) {
				Rf[i] = rf[k];
			}

			// compute orthogonal filters
			float filters_1[][][] = ComputeOrthogonalValues(Df);
			float filters_2[][][] = ComputeOrthogonalValues(Rf);

			float result[][][] = new float[2][2][];

			result[0][0] = filters_2[0][0]; // lo r2
			result[0][1] = filters_1[0][1]; // hi r1
			result[1][0] = filters_1[1][0]; // lo d1
			result[1][1] = filters_2[1][1]; // hi d2

			return result;
		}

		// compute orhogonal filters
		/**
		 * Compute orthogonal values.
		 * 
		 * @param filter_value
		 *            the filter_value
		 * 
		 * @return the float[][][]
		 */
		protected float[][][] ComputeOrthogonalValues(float[] filter_value) {
			if (filter_value == null)
				return null;

			// normalize filter and return filter values for reconstruction and
			// decomposition
			return AssociateFilters(NormalizeValues(filter_value));
		};

		// compute quadrature mirror filters
		/**
		 * Compute qmf.
		 * 
		 * @param values
		 *            the values
		 * @param p
		 *            the p
		 * 
		 * @return the float[]
		 */
		protected float[] ComputeQmf(float[] values, int p) {

			if (values == null)
				return null;

			float result[] = new float[values.length];

			float tmp = 2 - (p % 2);
			// compute reverse filter
			for (int i = 0; i < result.length; i++) {
				result[i] = values[result.length - i - 1];
				if ((i + 1) % tmp == 0)
					result[i] = -result[i];
			}

			return result;
		};

		/**
		 * Gets the discrete meyer values.
		 * 
		 * @return the float[]
		 */
		public float[] GetDiscreteMeyerValues() {
			float tmp[] = { -1.06754713000000e-06f, 9.04223910000000e-07f,
					3.17904740000000e-07f, -1.48249686000000e-06f,
					1.21850207000000e-06f, 4.93618310000000e-07f,
					-2.03604729000000e-06f, 1.68513902000000e-06f,
					6.94742880000000e-07f, -2.98242491000000e-06f,
					2.37128175000000e-06f, 1.18420622000000e-06f,
					-4.26703335000000e-06f, 3.42066573000000e-06f,
					1.69867277000000e-06f, -6.75732600000000e-06f,
					5.10285152000000e-06f, 3.42881336000000e-06f,
					-1.00458073700000e-05f, 7.42738297000000e-06f,
					4.37527643000000e-06f, -1.72802656000000e-05f,
					1.42173515200000e-05f, 1.06020135900000e-05f,
					-3.28300673700000e-05f, 2.28687423700000e-05f,
					2.64526068300000e-05f, -7.26756723600000e-05f,
					1.72972015000000e-05f, 0.000105863355880000f,
					-5.34521877000000e-05f, -9.89334554300000e-05f,
					-6.61235476200000e-05f, 0.000113978321900000f,
					0.000607757935360000f, -0.000408838764160000f,
					-0.00191072028190000f, 0.00155193926157000f,
					0.00427481806225000f, -0.00451609544333000f,
					-0.00780973483285000f, 0.0107840153442700f,
					0.0123063973649900f, -0.0226939113793400f,
					-0.0171980843830200f, 0.0450195435858100f,
					0.0216524716331900f, -0.0938306002586500f,
					-0.0247828615296900f, 0.314022352386430f,
					0.525910951416260f, 0.314022352386430f,
					-0.0247828615296900f, -0.0938306002586500f,
					0.0216524716331900f, 0.0450195435858100f,
					-0.0171980843830200f, -0.0226939113793400f,
					0.0123063973649900f, 0.0107840153442700f,
					-0.00780973483285000f, -0.00451609544333000f,
					0.00427481806225000f, 0.00155193926157000f,
					-0.00191072028190000f, -0.000408838764160000f,
					0.000607757935360000f, 0.000113978321900000f,
					-6.61235476200000e-05f, -9.89334554300000e-05f,
					-5.34521877000000e-05f, 0.000105863355880000f,
					1.72972015000000e-05f, -7.26756723600000e-05f,
					2.64526068300000e-05f, 2.28687423700000e-05f,
					-3.28300673700000e-05f, 1.06020135900000e-05f,
					1.42173515200000e-05f, -1.72802656000000e-05f,
					4.37527643000000e-06f, 7.42738297000000e-06f,
					-1.00458073700000e-05f, 3.42881336000000e-06f,
					5.10285152000000e-06f, -6.75732600000000e-06f,
					1.69867277000000e-06f, 3.42066573000000e-06f,
					-4.26703335000000e-06f, 1.18420622000000e-06f,
					2.37128175000000e-06f, -2.98242491000000e-06f,
					6.94742880000000e-07f, 1.68513902000000e-06f,
					-2.03604729000000e-06f, 4.93618310000000e-07f,
					1.21850207000000e-06f, -1.48249686000000e-06f,
					3.17904740000000e-07f, 9.04223910000000e-07f,
					-1.06754713000000e-06f, 0 };

			return tmp;
		}

		/**
		 * Gets the kernel filter.
		 * 
		 * @param wave_type
		 *            the wave_type
		 * @param filter_type
		 *            the filter_type
		 * 
		 * @return the float[][]
		 */
		public float[][] GetKernelFilter(e_wave_type wave_type,
				e_filter_type filter_type) {
			float[][][] filters = null;

			int actual_type = wave_type.ordinal();

			if (actual_type <= e_wave_type.EWT_SYM_8.ordinal()
					&& actual_type > e_wave_type.EWT_FIRST.ordinal()) {
				filters = ComputeOrthogonalValues(GetWaveValues(wave_type));
			} else if (actual_type <= e_wave_type.EWT_BIOR_6_8.ordinal()
					&& actual_type > e_wave_type.EWT_SYM_8.ordinal()) {
				float filt[][] = GetOrBiorthogonalFilterCoer(wave_type, false);
				if (filt == null)
					return null;

				filters = ComputeBiorthogonalValues(filt[0], filt[1]);
			} else if (actual_type <= e_wave_type.EWT_RBIOR_6_8.ordinal()
					&& actual_type > e_wave_type.EWT_BIOR_6_8.ordinal()) {
				float filt[][] = GetReverseBiorthogonalFilter(wave_type);
				if (filt == null)
					return null;

				filters = ComputeBiorthogonalValues(filt[0], filt[1]);
			} else if (actual_type == e_wave_type.EWT_DMEY.ordinal()) {
				filters = ComputeOrthogonalValues(GetDiscreteMeyerValues());
			} else {
				filters = GetOtherFilter(wave_type);
			}

			if (filter_type == e_filter_type.EFT_REC)
				return filters[0];
			else if (filter_type == e_filter_type.EFT_DEC)
				return filters[1];

			return null;
		}

		// return max level for decomposition
		/**
		 * Gets the max level.
		 * 
		 * @param input
		 *            the input
		 * @param kernel
		 *            the kernel
		 * 
		 * @return the int
		 */
		public int GetMaxLevel(float[][] input, float[] kernel) {
			int size = Math.min(input[0].length, input.length);
			return (int) Math.round(Math.log(size) / Math.log(2)) - 2; // -2 ..
																		// min
																		// div
																		// is 2
		}

		/**
		 * Gets the or biorthogonal filter coer.
		 * 
		 * @param type
		 *            the type
		 * @param bReverse
		 *            the b reverse
		 * 
		 * @return the float[][]
		 */
		protected float[][] GetOrBiorthogonalFilterCoer(e_wave_type type,
				boolean bReverse) {

			float Rf[] = null;
			float Df[] = null;

			if (type == e_wave_type.EWT_BIOR_6_8) {
				float rf[] = { -0.01020092218704f, -0.01023007081937f,
						0.05566486077996f, 0.02854447171515f,
						-0.29546393859292f, -0.53662880179157f,
						-0.29546393859292f, 0.02854447171515f,
						0.05566486077996f, -0.01023007081937f,
						-0.01020092218704f };

				float df[] = { 0.00134974786501f, -0.00135360470301f,
						-0.01201419666708f, 0.00843901203981f,
						0.03516647330654f, -0.05463331368252f,
						-0.06650990062484f, 0.29754790634571f,
						0.58401575224075f, 0.29754790634571f,
						-0.06650990062484f, -0.05463331368252f,
						0.03516647330654f, 0.00843901203981f,
						-0.01201419666708f, -0.00135360470301f,
						0.00134974786501f };

				Rf = rf;
				Df = df;
			} else if (type == e_wave_type.EWT_BIOR_5_5) {
				float rf[] = { 0.009515330511f, -0.001905629356f,
						-0.096666153049f, -0.066117805605f, 0.337150822538f,
						.636046869922f, 0.337150822538f, -0.066117805605f,
						-0.096666153049f, -0.001905629356f, 0.009515330511f };

				float df[] = { 0.028063009296f, 0.005620161515f,
						-0.038511714155f, 0.244379838485f, 0.520897409718f,
						0.244379838485f, -0.038511714155f, 0.005620161515f,
						0.028063009296f };

				Rf = rf;
				Df = df;
			} else if (type == e_wave_type.EWT_BIOR_4_4) {
				float rf[] = { -0.045635881557f, -0.028771763114f,
						0.295635881557f, 0.557543526229f, 0.295635881557f,
						-0.028771763114f, -0.045635881557f };

				float df[] = { 0.026748757411f, -0.016864118443f,
						-0.078223266529f, 0.266864118443f, 0.602949018236f,
						0.266864118443f, -0.078223266529f, -0.016864118443f,
						0.026748757411f };
				Rf = rf;
				Df = df;
			} else if (type.ordinal() >= e_wave_type.EWT_BIOR_3_1.ordinal()
					&& type.ordinal() <= e_wave_type.EWT_BIOR_3_9.ordinal()) {
				float rf[] = { 1.0f / 3.0f, 3.0f / 8.0f };

				float df[] = null;

				if (type == e_wave_type.EWT_BIOR_3_1) {
					float tmp[] = { -1.0f / 4.0f, 3.0f / 4.0f };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_3_3) {
					float tmp[] = { 3.0f / 64.0f, -9.0f / 64.0f, -7.0f / 64.0f,
							45.0f / 64.0f };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_3_5) {
					float tmp[] = { -5.0f / 512.0f, 15.0f / 512.0f,
							19.0f / 512.0f, -97.0f / 512.0f, -26.0f / 512.0f,
							350.0f / 512.0f };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_3_7) {
					float val = (float) Math.pow(2.0f, 14.0f);

					float tmp[] = { 35.0f / val, -105.0f / val, -195.0f / val,
							865.0f / val, 363.0f / val, -3489.0f / val,
							-307.0f / val, 11025.0f / val };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_3_9) {
					float val = (float) Math.pow(2.0, 17.0);
					float tmp[] = { -63.0f / val, 189.0f / val, 469.0f / val,
							-1911.0f / val, -1308.0f / val, 9188.0f / val,
							1140.0f / val, -29676.0f / val, 190.0f / val,
							87318.0f / val };
					df = tmp;
				}
				if (df == null)
					return null;

				Rf = add_to_reverse_array(rf);
				Df = add_to_reverse_array(df);

			} else if (type.ordinal() >= e_wave_type.EWT_BIOR_2_2.ordinal()
					&& type.ordinal() <= e_wave_type.EWT_BIOR_2_8.ordinal()) {
				float rf[] = { 1.0f / 4.0f, 1.0f / 2.0f, 1.0f / 4.0f };

				float df[] = null;

				if (type == e_wave_type.EWT_BIOR_2_2) {
					float tmp[] = { -1.0f / 8.0f, 1.0f / 4.0f };
					df = add_to_reverse_array(tmp, 3.0f / 4.0f);
				} else if (type == e_wave_type.EWT_BIOR_2_4) {
					float tmp[] = { 3.0f / 128.0f, -3.0f / 64.0f, -1.0f / 8.0f,
							19.0f / 64.0f };
					df = add_to_reverse_array(tmp, 45.0f / 64.0f);
				} else if (type == e_wave_type.EWT_BIOR_2_6) {
					float tmp[] = { -5.0f / 1024.0f, 5.0f / 512.0f,
							17.0f / 512.0f, -39.0f / 512.0f, -123.0f / 1024.0f,
							81.0f / 256.0f };
					df = add_to_reverse_array(tmp, 175.0f / 256.0f);
				} else if (type == e_wave_type.EWT_BIOR_2_8) {
					float val = (float) Math.pow(2.0, 15.0);
					float tmp[] = { 35.0f / val, -70.0f / val, -300.0f / val,
							670.0f / val, 1228.0f / val, -3126.0f / val,
							-3796.0f / val, 10718.0f / val };
					df = add_to_reverse_array(tmp, 22050.0f / val);
				}

				if (df == null)
					return null;

				Df = df;
				Rf = rf;

			} else if (type.ordinal() >= e_wave_type.EWT_BIOR_1_1.ordinal()
					&& type.ordinal() <= e_wave_type.EWT_BIOR_1_5.ordinal()) {
				float rf[] = { 0.5f };

				float df[] = null;

				if (type == e_wave_type.EWT_BIOR_1_1) {
					float tmp[] = { 0, 5 };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_1_3) {
					float tmp[] = { -1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 2.0f };
					df = tmp;
				} else if (type == e_wave_type.EWT_BIOR_1_5) {
					float tmp[] = { 3.0f / 256.0f, -3.0f / 256.0f,
							-11.0f / 128.0f, 11.0f / 128.0f, 1.0f / 2.0f };
					df = tmp;
				}

				if (df == null)
					return null;

				Df = add_to_reverse_array(df);
				Rf = add_to_reverse_array(rf);
			}

			if (Df == null || Rf == null)
				return null;

			float result[][] = new float[2][];

			if (!bReverse) {
				result[0] = Df;
				result[1] = Rf;
			} else {
				result[0] = Rf;
				result[1] = Df;
			}

			return result;
		}

		/**
		 * Gets the orthogonal filter coef.
		 * 
		 * @param type
		 *            the type
		 * 
		 * @return the float[]
		 */
		protected float[] GetOrthogonalFilterCoef(e_wave_type type) {

			switch (type) {
			case EWT_HARR:
			case EWT_DB_1: {
				float F[] = { 0.500f, 0.500f };
				return F;
			}

			case EWT_DB_2: {
				float F[] = { 0.34150635094622f, 0.59150635094587f,
						0.15849364905378f, -0.09150635094587f };
				return F;
			}

			case EWT_DB_3: {
				float F[] = { 0.23523360389270f, 0.57055845791731f,
						0.32518250026371f, -0.09546720778426f,
						-0.06041610415535f, 0.02490874986589f };
				return F;
			}

			case EWT_DB_4: {
				float F[] = { 0.16290171402562f, 0.50547285754565f,
						0.44610006912319f, -0.01978751311791f,
						-0.13225358368437f, 0.02180815023739f,
						0.02325180053556f, -0.00749349466513f };
				return F;
			}

			case EWT_DB_5: {
				float F[] = { 0.11320949129173f, 0.42697177135271f,
						0.51216347213016f, 0.09788348067375f,
						-0.17132835769133f, -0.02280056594205f,
						0.05485132932108f, -0.00441340005433f,
						-0.00889593505093f, 0.00235871396920f };
				return F;
			}

			case EWT_DB_6: {
				float F[] = { 0.07887121600143f, 0.34975190703757f,
						0.53113187994121f, 0.22291566146505f,
						-0.15999329944587f, -0.09175903203003f,
						0.06894404648720f, 0.01946160485396f,
						-0.02233187416548f, 0.00039162557603f,
						0.00337803118151f, -0.00076176690258f };
				return F;
			}

			case EWT_DB_7: {
				float F[] = { 0.05504971537285f, 0.28039564181304f,
						0.51557424581833f, 0.33218624110566f,
						-0.10175691123173f, -0.15841750564054f,
						0.05042323250485f, 0.05700172257986f,
						-0.02689122629486f, -0.01171997078235f,
						0.00887489618962f, 0.00030375749776f,
						-0.00127395235906f, 0.00025011342658f };
				return F;
			}

			case EWT_DB_8: {
				float F[] = { 0.03847781105406f, 0.22123362357624f,
						0.47774307521438f, 0.41390826621166f,
						-0.01119286766665f, -0.20082931639111f,
						0.00033409704628f, 0.09103817842345f,
						-0.01228195052300f, -0.03117510332533f,
						0.00988607964808f, 0.00618442240954f,
						-0.00344385962813f, -0.00027700227421f,
						0.00047761485533f, -0.00008306863060f };
				return F;
			}

			case EWT_DB_9: {
				float F[] = { 0.02692517479416f, 0.17241715192471f,
						0.42767453217028f, 0.46477285717278f,
						0.09418477475112f, -0.20737588089628f,
						-0.06847677451090f, 0.10503417113714f,
						0.02172633772990f, -0.04782363205882f,
						0.00017744640673f, 0.01581208292614f,
						-0.00333981011324f, -0.00302748028715f,
						0.00130648364018f, 0.00016290733601f,
						-0.00017816487955f, 0.00002782275679f };
				return F;
			}

			case EWT_DB_10: {
				float F[] = { 0.01885857879640f, 0.13306109139687f,
						0.37278753574266f, 0.48681405536610f,
						0.19881887088440f, -0.17666810089647f,
						-0.13855493935993f, 0.09006372426666f,
						0.06580149355070f, -0.05048328559801f,
						-0.02082962404385f, 0.02348490704841f,
						0.00255021848393f, -0.00758950116768f,
						0.00098666268244f, 0.00140884329496f,
						-0.00048497391996f, -0.00008235450295f,
						0.00006617718320f, -0.00000937920789f };
				return F;
			}

				// coiflets filters
			case EWT_COIF_1: {
				float F[] = { -0.05142972847100f, 0.23892972847100f,
						0.60285945694200f, 0.27214054305800f,
						-0.05142972847100f, -0.01107027152900f };
				return F;
			}

			case EWT_COIF_2: {
				float F[] = { 0.01158759673900f, -0.02932013798000f,
						-0.04763959031000f, 0.27302104653500f,
						0.57468239385700f, 0.29486719369600f,
						-0.05408560709200f, -0.04202648046100f,
						0.01674441016300f, 0.00396788361300f,
						-0.00128920335600f, -0.00050950539900f, };
				return F;
			}

			case EWT_COIF_3: {
				float F[] = { -0.00268241867100f, 0.00550312670900f,
						0.01658356047900f, -0.04650776447900f,
						-0.04322076356000f, 0.28650333527400f,
						0.56128525687000f, 0.30298357177300f,
						-0.05077014075500f, -0.05819625076200f,
						0.02443409432100f, 0.01122924096200f,
						-0.00636960101100f, -0.00182045891600f,
						0.00079020510100f, 0.00032966517400f,
						-0.00005019277500f, -0.00002446573400f };
				return F;
			}

			case EWT_COIF_4: {
				float F[] = { 0.00063096104600f, -0.00115222485200f,
						-0.00519452402600f, 0.01136245924400f,
						0.01886723537800f, -0.05746423442900f,
						-0.03965264851700f, 0.29366739089500f,
						0.55312645256200f, 0.30715732619800f,
						-0.04711273886500f, -0.06803812705100f,
						0.02781364015300f, 0.01773583743800f,
						-0.01075631851700f, -0.00400101288600f,
						0.00265266594600f, 0.00089559452900f,
						-0.00041650057100f, -0.00018382976900f,
						0.00004408035400f, 0.00002208285700f,
						-0.00000230494200f, -0.00000126217500f, };
				return F;
			}

			case EWT_COIF_5: {
				float F[] = { -0.00014996380000f, 0.00025356120000f,
						0.00154024570000f, -0.00294111080000f,
						-0.00716378190000f, 0.01655206640000f,
						0.01991780430000f, -0.06499726280000f,
						-0.03680007360000f, 0.29809232350000f,
						0.54750542940000f, 0.30970684900000f,
						-0.04386605080000f, -0.07465223890000f,
						0.02919587950000f, 0.02311077700000f,
						-0.01397368790000f, -0.00648009000000f,
						0.00478300140000f, 0.00172065470000f,
						-0.00117582220000f, -0.00045122700000f,
						0.00021372980000f, 0.00009937760000f,
						-0.00002923210000f, -0.00001507200000f,
						0.00000264080000f, 0.00000145930000f,
						-0.00000011840000f, -0.00000006730000f };
				return F;
			}

				// Symlet wavelet filters
			case EWT_SYM_1: {
				float F[] = { 0.500f, 0.500f };
				return F;
			}
			case EWT_SYM_2: {
				float F[] = { 0.34150635094622f, 0.59150635094587f,
						0.15849364905378f, -0.09150635094587f };
				return F;
			}
			case EWT_SYM_3: {
				float F[] = { 0.23523360389270f, 0.57055845791731f,
						0.32518250026371f, -0.09546720778426f,
						-0.06041610415535f, 0.02490874986589f };
				return F;
			}
			case EWT_SYM_4: {
				float F[] = { 0.02278517294800f, -0.00891235072085f,
						-0.07015881208950f, 0.21061726710200f,
						0.56832912170500f, 0.35186953432800f,
						-0.02095548256255f, -0.05357445070900f };
				return F;
			}
			case EWT_SYM_5: {
				float F[] = { 0.01381607647893f, -0.01492124993438f,
						-0.12397568130675f, 0.01173946156807f,
						0.44829082419092f, 0.51152648344605f,
						0.14099534842729f, -0.02767209305836f,
						0.02087343221079f, 0.01932739797744f };
				return F;
			}
			case EWT_SYM_6: {
				float F[] = { -0.00551593375469f, 0.00124996104639f,
						0.03162528132994f, -0.01489187564922f,
						-0.05136248493090f, 0.23895218566605f,
						0.55694639196396f, 0.34722898647835f,
						-0.03416156079324f, -0.08343160770584f,
						0.00246830618592f, 0.01089235016328f };
				return F;
			}
			case EWT_SYM_7: {
				float F[] = { 0.00726069738101f, 0.00283567134288f,
						-0.07623193594814f, -0.09902835340368f,
						0.20409196986287f, 0.54289135490599f,
						0.37908130098269f, 0.01233282974432f,
						-0.03503914561106f, 0.04800738396784f,
						0.02157772629104f, -0.00893521582557f,
						-0.00074061295730f, 0.00189632926710f };
				return F;
			}
			case EWT_SYM_8: {
				float F[] = { 0.00133639669640f, -0.00021419715012f,
						-0.01057284326418f, 0.00269319437688f,
						0.03474523295559f, -0.01924676063167f,
						-0.03673125438038f, 0.25769933518654f,
						0.54955331526901f, 0.34037267359439f,
						-0.04332680770282f, -0.10132432764282f,
						0.00537930587524f, 0.02241181152181f,
						-0.00038334544811f, -0.00239172925575f };
				return F;
			}

			}

			return null;
		}

		/**
		 * Gets the other filter.
		 * 
		 * @param type
		 *            the type
		 * 
		 * @return the float[][][]
		 */
		protected float[][][] GetOtherFilter(e_wave_type type) {
			if (type == e_wave_type.EWT_JPEG_9_7) {
				float Lo_d[] = { 0.0f, 0.02674875741080976f,
						-0.01686411844287495f, -0.07822326652898785f,
						0.2668641184428723f, 0.6029490182363579f,
						0.2668641184428723f, -0.07822326652898785f,
						-0.01686411844287495f, 0.02674875741080976f };

				float Hi_d[] = { 0, -0.09127176311424948f,
						0.05754352622849957f, 0.5912717631142470f,
						-1.115087052456994f, 0.5912717631142470f,
						0.05754352622849957f, -0.09127176311424948f, 0f, 0f };

				float Lo_r[] = { 0, -0.09127176311424948f,
						-0.05754352622849957f, 0.5912717631142470f,
						1.115087052456994f, 0.5912717631142470f,
						-0.05754352622849957f, -0.09127176311424948f, 0f, 0f };

				float Hi_r[] = { 0f, -0.02674875741080976f,
						-0.01686411844287495f, 0.07822326652898785f,
						0.2668641184428723f, -0.6029490182363579f,
						0.2668641184428723f, 0.07822326652898785f,
						-0.01686411844287495f, -0.02674875741080976f };

				float result[][][] = new float[2][2][];

				result[0][0] = Lo_r; // lo r
				result[0][1] = Hi_r; // hi r
				result[1][0] = Lo_d; // lo d
				result[1][1] = Hi_d; // hi d

				return result;
			}
			if (type == e_wave_type.EWT_JPEG_5_3) {
				float Lo_d[] = { 0, -1.0f / 8.0f, 2.0f / 8.0f, 6.0f / 8.0f,
						2.0f / 8.0f, -1.0f / 8.0f };

				float Hi_d[] = { 0, -0.5f, 1.0f, -0.5f, 0, 0 };

				float Lo_r[] = { 0, 0.5f, 1.0f, 0.5f, 0, 0 };

				float Hi_r[] = { 0, -1.0f / 8.0f, -2.0f / 8.0f, 6.0f / 8.0f,
						-2.0f / 8.0f, -1.0f / 8.0f };

				float result[][][] = new float[2][2][];

				result[0][0] = Lo_r; // lo r
				result[0][1] = Hi_r; // hi r
				result[1][0] = Lo_d; // lo d
				result[1][1] = Hi_d; // hi d

				return result;
			}

			return null;
		}

		/**
		 * Gets the reverse biorthogonal filter.
		 * 
		 * @param type
		 *            the type
		 * 
		 * @return the float[][]
		 */
		protected float[][] GetReverseBiorthogonalFilter(e_wave_type type) {

			switch (type) {
			case EWT_RBIOR_1_1:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_1_1,
						true);
			case EWT_RBIOR_1_3:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_1_3,
						true);
			case EWT_RBIOR_1_5:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_1_5,
						true);

			case EWT_RBIOR_2_2:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_2_2,
						true);
			case EWT_RBIOR_2_4:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_2_4,
						true);
			case EWT_RBIOR_2_6:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_2_6,
						true);
			case EWT_RBIOR_2_8:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_2_8,
						true);

			case EWT_RBIOR_3_1:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_3_1,
						true);
			case EWT_RBIOR_3_3:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_3_3,
						true);
			case EWT_RBIOR_3_5:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_3_5,
						true);
			case EWT_RBIOR_3_7:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_3_7,
						true);
			case EWT_RBIOR_3_9:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_3_9,
						true);

			case EWT_RBIOR_4_4:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_4_4,
						true);
			case EWT_RBIOR_5_5:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_5_5,
						true);
			case EWT_RBIOR_6_8:
				return GetOrBiorthogonalFilterCoer(e_wave_type.EWT_BIOR_6_8,
						true);

			}
			;

			return null;
		}

		/**
		 * Gets the wavelets.
		 * 
		 * @return the wavelet type[]
		 */
		public WaveletType[] GetWavelets() {
			return m_wavelets;
		}

		/**
		 * Gets the wave values.
		 * 
		 * @param type
		 *            the type
		 * 
		 * @return the float[]
		 */
		public float[] GetWaveValues(e_wave_type type) {
			return GetOrthogonalFilterCoef(type);
		}

		// normalize of array
		/**
		 * Normalize values.
		 * 
		 * @param values
		 *            the values
		 * 
		 * @return the float[]
		 */
		protected float[] NormalizeValues(float[] values) {

			if (values == null)
				return null;

			float result[] = new float[values.length];

			float sum = SumValues(values);

			for (int i = 0; i < values.length; i++) {
				result[i] = values[i] / sum;
			}

			return result;
		}

		// sum of array
		/**
		 * Sum values.
		 * 
		 * @param values
		 *            the values
		 * 
		 * @return the float
		 */
		protected float SumValues(float[] values) {
			if (values == null)
				return 0;

			float sum = 0;
			for (int i = 0; i < values.length; i++) {
				sum += values[i];
			}
			return sum;
		}
	}

	/** The m_path. */
	private String m_path;

	/** The path. */
	private Vector<String> path = new Vector<String>();

	/** The m_wave_index. */
	private int m_wave_index;

	/** The m_imgp. */
	private ImagePlus m_imgp = null;

	/** The flags. */
	private int flags = DOES_ALL | SUPPORTS_MASKING | PARALLELIZE_STACKS;

	/** The m_prev. */
	boolean m_prev = false;

	boolean m_resize = true;

	double m_res_width = 1.0;
	double m_res_height = 1.0;

	/**
	 * vytvoøí obraz z pole RGB.
	 * 
	 * @param rgb
	 *            the rgb
	 * 
	 * @return the image
	 */
	public Image createImag(float[][] rgb) {
		Image newImage = null;
		Color color;
		int pom = 0;
		int height = (int) rgb.length;
		int width = (int) rgb[0].length;
		int[] pixels = new int[width * height];

		// get max value
		float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
		for (int i = 0; i < rgb.length; i++) {
			for (int j = 0; j < rgb[0].length; j++) {
				float mx = Math.max(rgb[i][j], max);
				float mn = Math.min(rgb[i][j], min);

				if (mx > max)
					max = mx;
				if (mn < min)
					min = mn;
			}
		}

		float koef = 256 / (max - min + 1);

		// posun do pocatku
		for (int i = 0; i < rgb.length; i++) {
			for (int j = 0; j < rgb[0].length; j++) {
				rgb[i][j] = ((float) ((rgb[i][j] - min) * koef));
			}
		}

		for (int j = 0; j < rgb.length; j++) {
			for (int i = 0; i < rgb[0].length; i++) {

				if (rgb[j][i] >= 0 && rgb[j][i] < 256)
					color = new Color((int) rgb[j][i], (int) rgb[j][i],
							(int) rgb[j][i]);
				else {
					color = new Color(255, 0, 0);

				}

				pixels[pom++] = color.getRGB();
			}
		}

		Toolkit tk = Toolkit.getDefaultToolkit();
		newImage = tk.createImage(new MemoryImageSource(width, height, pixels,
				0, width));
		return (newImage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.gui.DialogListener#dialogItemChanged(ij.gui.GenericDialog,
	 * java.awt.AWTEvent)
	 */
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent ev) {

		m_path = (String) gd.getNextString();

		m_wave_index = (int) gd.getNextChoiceIndex();

		m_prev = (boolean) gd.getNextBoolean();

		return true;
	}

	/**
	 * Gets the data.
	 * 
	 * @param img
	 *            the img
	 * 
	 * @return the float[][]
	 */
	public float[][] GetData(BufferedImage img) {
		if (img == null)
			return null;

		float[][] result = new float[img.getHeight()][img.getWidth()];
		// for all data
		for (int i = 0; i < img.getHeight(); i++) { // rows
			for (int j = 0; j < img.getWidth(); j++) { // cols
				Color color = new Color(img.getRGB(j, i));
				// 0.3*R + 0.59*G + 0.11*B
				result[i][j] = (0.3f * (float) color.getRed() + 0.59f
						* (float) color.getGreen() + 0.11f * (float) color
						.getBlue());
			}
		}

		return result;
	}

	/**
	 * Gets the data.
	 * 
	 * @param image
	 *            the image
	 * 
	 * @return the float[][]
	 */
	public float[][] GetData(ImageIcon image) {
		BufferedImage res_img = new BufferedImage(image.getIconWidth(),
				image.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = res_img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(image.getImage(), 0, 0, image.getIconWidth(),
				image.getIconHeight(), null);
		g2.dispose();

		return GetData(res_img);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	/**
	 * This method is invoked for each slice during execution
	 */
	public void run(ImageProcessor arg0) {

		int width = arg0.getWidth();
		int height = arg0.getHeight();

		float[][] in = arg0.getFloatArray();

		Wavelets wave = new Wavelets();
		WaveletDecomposition dwt = new WaveletDecomposition();

		Wavelets.e_wave_type wave_type = wave.GetWavelets()[m_wave_index].waveType;

		// get wavelet
		float[][] kernel = wave.GetKernelFilter(wave_type,
				Wavelets.e_filter_type.EFT_DEC);

		// compute decompositon
		Vector<WaveletDecomposition.e_wpt_type> _path = new Vector<WaveletDecomposition.e_wpt_type>();

		_path.clear();
		WaveletDecomposition.WindowType[] type = dwt.GetWindows();

		m_path = m_path.replace(',', ' ');
		StringTokenizer st = new StringTokenizer(m_path);
		path.clear();

		do {
			path.add(st.nextToken());
		} while (st.hasMoreElements());

		for (int i = 0; i < path.size(); i++) {

			if (path.get(i).compareTo(type[0].windowName) == 0)
				_path.add(type[0].windowType);
			else if (path.get(i).compareTo(type[1].windowName) == 0)
				_path.add(type[1].windowType);
			else if (path.get(i).compareTo(type[2].windowName) == 0)
				_path.add(type[2].windowType);
			else if (path.get(i).compareTo(type[3].windowName) == 0)
				_path.add(type[3].windowType);
			else
				;
		}

		float[][] result = dwt.Decomposition(in, kernel, _path);

		// convert to result
		int w = result[0].length;
		int h = result.length;

		Image img = createImag(result);
		ImageIcon icon = new ImageIcon(img);
		float[][] res = GetData(icon);

		ImageProcessor ip2 = arg0.createProcessor(h, w);

		ip2.setFloatArray(res);
		ip2.convertToByte(true);

		arg0.setInterpolate(true);

		if (!m_resize) {
			// System.out.print("\ninput size:" + width + ", " + height);
			width = (int) (m_res_width * width);
			height = (int) (m_res_height * height);
			// System.out.print("\nresult size:" + width + ", " + height);
		} else {
			width = h;
			height = w;
			// System.out.print("ok");
		}

		m_imgp.setProcessor(m_imgp.getTitle(), ip2.resize(width, height));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
	 */
	@Override
	public void setNPasses(int arg0) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg0, ImagePlus arg1) {

		this.m_imgp = arg1;

		if (arg0.isEmpty()) {
			path.clear();
			m_wave_index = 0;
			m_path = "LL LL";
			m_path.replace(';', ' ');
			StringTokenizer st = new StringTokenizer(m_path);
			while (st.hasMoreElements())
				path.add(st.nextToken());

			m_resize = true;
			m_res_width = 1.0;
			m_res_height = 1.0;
		} else {
			// parameters
			path.clear();

			StringTokenizer st1 = new StringTokenizer(arg0);

			// text
			m_wave_index = (int) Integer.parseInt(st1.nextToken(";"));
			m_path = st1.nextToken();
			m_resize = (boolean) Boolean.parseBoolean(st1.nextToken());
			m_res_width = (double) Double.parseDouble(st1.nextToken());
			m_res_height = (double) Double.parseDouble(st1.nextToken());
			m_path = m_path.replaceAll("\\s+", "");

			// System.out.print(m_res_width + ", " + m_res_height);

			StringTokenizer st = new StringTokenizer(m_path);

			while (st.hasMoreElements())
				path.add(st.nextToken());

		}

		return flags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus,
	 * java.lang.String, ij.plugin.filter.PlugInFilterRunner)
	 */
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog(command + "");

		// Packet tree path
		gd.addStringField("Path", "LL,LL");

		// Wave
		Wavelet_Decompositon.Wavelets wave = new Wavelet_Decompositon.Wavelets();
		Wavelet_Decompositon.Wavelets.WaveletType[] wa = wave.GetWavelets();

		String[] tmp = new String[wa.length];

		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = wa[i].waveName;
		}

		gd.addChoice("Packet tree path", tmp, tmp[0]);

		// Preview
		gd.addPreviewCheckbox(pfr);

		gd.addDialogListener(this);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;

		IJ.register(this.getClass());

		return IJ.setupDialog(imp, flags);
	}

}