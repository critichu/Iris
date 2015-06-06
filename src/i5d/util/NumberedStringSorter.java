//
// NumberedStringSorter.java
//

/*
Image5D plugins for 5-dimensional image stacks in ImageJ.

Copyright (c) 2010, Joachim Walter and ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package i5d.util;

/**
 * Sorts an array of Strings treating any sequences of digits as numbers, not
 * single digits. (Windows XP Style) Essentially the ij.util.StringSorter with a
 * new compare method. Works nicely, when image names have two or more numbers
 * (like name-<ch>-<z>-<t>).
 */
public class NumberedStringSorter {

	/** Sorts the array. */
	public static void sort(final String[] a) {
		if (!alreadySorted(a)) sort(a, 0, a.length - 1);
	}

	static void sort(final String[] a, final int from, final int to) {
		int i = from, j = to;
		final String center = a[(from + to) / 2];
		do {
			while (i < to && compareNumberedStrings(center, a[i]) > 0)
				i++;
			while (j > from && compareNumberedStrings(center, a[j]) < 0)
				j--;
			if (i < j) {
				final String temp = a[i];
				a[i] = a[j];
				a[j] = temp;
			}
			if (i <= j) {
				i++;
				j--;
			}
		}
		while (i <= j);
		if (from < j) sort(a, from, j);
		if (i < to) sort(a, i, to);
	}

	static boolean alreadySorted(final String[] a) {
		for (int i = 1; i < a.length; i++) {
			if (compareNumberedStrings(a[i], a[i - 1]) < 0) return false;
		}
		return true;
	}

	/**
	 * Compares Strings treating any sequences of digits in them as numbers, not
	 * single digits. Similar to the treatment of numbers in the Windows XP
	 * explorer
	 * 
	 * @param first
	 * @param second
	 * @return: -1 if first<second, 0 if equal, +1 if first>second
	 */
	static int compareNumberedStrings(final String first, final String second) {
		char c1, c2;
		int N1, N2;
		int i1 = 0, i2 = 0;
		String num1, num2;
		int int1, int2;

		N1 = first.length();
		N2 = second.length();

		while (i1 < N1 && i2 < N2) {
			c1 = first.charAt(i1);
			c2 = second.charAt(i2);

			// char1 is digit (between ASCII 48 and 57)
			if (c1 >= '0' && c1 <= '9') {
				if ('9' < c2) {
					return -1;
				}
				else if ('0' > c2) {
					return +1;
					// char1 and char2 are both digits. Get the full numbers and compare.
				}
				else {
					num1 = "";
					num2 = "";
					do {
						num1 += c1;
						i1++;
						if (i1 < N1) {
							c1 = first.charAt(i1);
						}
					}
					while (i1 < N1 && c1 >= '0' && c1 <= '9');
					int1 = Integer.parseInt(num1);
					do {
						num2 += c2;
						i2++;
						if (i2 < N2) {
							c2 = second.charAt(i2);
						}
					}
					while (i2 < N2 && c2 >= '0' && c2 <= '9');
					int2 = Integer.parseInt(num2);

					if (int1 < int2) {
						return -1;
					}
					else if (int1 > int2) {
						return +1;
					}
				} // if (c1>='0' && c1<='9')

				// char1 is not digit, compare as usual
			}
			else if (c1 < c2) {
				return -1;
			}
			else if (c1 > c2) {
				return +1;
			}
			else {
				++i1;
				++i2;
			}

		} // while (i1<N1 && i2<N2)

		// Got through loop without differences. Compare by stringlength.
		if (N1 < N2) {
			return -1;
		}
		else if (N1 > N2) {
			return +1;
		}
		return 0;
	} // compareNumberedStrings

}
