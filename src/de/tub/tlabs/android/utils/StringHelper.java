/******************************************************************************
 * Copyright (c) 2006-2012 Quality & Usability Lab                            *
 *                         Deutsche Telekom Laboratories, TU Berlin           *
 *                         Ernst-Reuter-Platz 7, 10587 Berlin, Germany        *
 *                                                                            *
 * This file is part of the SoundScape Renderer (SSR).                        *
 *                                                                            *
 * The SSR is free software:  you can redistribute it and/or modify it  under *
 * the terms of the  GNU  General  Public  License  as published by the  Free *
 * Software Foundation, either version 3 of the License,  or (at your option) *
 * any later version.                                                         *
 *                                                                            *
 * The SSR is distributed in the hope that it will be useful, but WITHOUT ANY *
 * WARRANTY;  without even the implied warranty of MERCHANTABILITY or FITNESS *
 * FOR A PARTICULAR PURPOSE.                                                  *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You should  have received a copy  of the GNU General Public License  along *
 * with this program.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                            *
 * The SSR is a tool  for  real-time  spatial audio reproduction  providing a *
 * variety of rendering algorithms.                                           *
 *                                                                            *
 * http://tu-berlin.de/?id=ssr                  SoundScapeRenderer@telekom.de *
 ******************************************************************************/

/* 
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 */

package de.tub.tlabs.android.utils;

import android.util.Log;

/**
 * Static methods to construct strings containing numbers avoiding
 * all the object creations java normally does when converting and appending using '+'.
 * 
 * @author Peter Bartz
 */
public class StringHelper {
	private static final char[] DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static final int DEFAULT_NUM_DEC_PLACES = 5;
	private static final char DEFAULT_PADDING_CHAR = '0';

	// Convert integer value to string and concatenate to given StringBuilder.
	public static void append(StringBuilder sb, int val, int padding, char paddingChar, int base) {
		// Minus sign
		if (val < 0) {
			sb.append('-');
			val = -val;
		}
		
		// Calculate length of string
		int length = 0;
		int lengthVal = val;
		do {
			lengthVal /= base;
			length++;
		} while (lengthVal > 0);

		// Output padding and make room
		int maxLength = Math.max(padding, length);
		for (int i = 0; i < maxLength; i++) {
			sb.append(paddingChar);
		}

		// We're writing backwards, one character at a time
		for (int i = sb.length()-1; length > 0; i--, length--) {
			sb.setCharAt(i, DIGITS[val % base]);
			val /= base;
		}
	}

	// Convert integer value to string and concatenate to given StringBuilder. Base 10 and given padding.
	public static void append(StringBuilder sb, int val, int padding, char paddingChar) {
		append(sb, val, padding, paddingChar, 10);
	}

	// Convert integer value to string and concatenate to given StringBuilder. Base 10 and given padding using zeros.
	public static void append(StringBuilder sb, int val, int padding) {
		append(sb, val, padding, DEFAULT_PADDING_CHAR, 10);
	}
	
	// Convert integer value to string and concatenate to given StringBuilder. Base 10 and no padding.
	public static void append(StringBuilder sb, int val) {
		append(sb, val, 0, DEFAULT_PADDING_CHAR, 10);
	}
	
	// Convert float value to string and concatenate to given StringBuilder.
	public static void append(StringBuilder sb, float val, int decimalPlaces, int padding, char paddingChar) {
		if (decimalPlaces == 0) {
			// Round and treat as int
			append(sb, Math.round(val), padding, paddingChar, 10);
		} else {
			int intPart = (int) val;

			// Cast to int and append
			append(sb, intPart, padding, paddingChar, 10);

			// Decimal point
			sb.append('.');

			// Calculate remainder
			float remainder = Math.abs(val - intPart);

			// Multiply, so we get an int
			remainder *= Math.pow(10.0, decimalPlaces);

			// Round last digit
			remainder += 0.5f;

			// Append as int
			append(sb, (int) remainder, decimalPlaces, '0', 10);
		}
	}
	
	// Convert float value to string and concatenate to given StringBuilder. 5 decimal places, no padding.
	public static void append(StringBuilder sb, float val) {
		append(sb, val, DEFAULT_NUM_DEC_PLACES, 0, DEFAULT_PADDING_CHAR);
	}

	// Convert float value to string and concatenate to given StringBuilder. No padding.
	public static void append(StringBuilder sb, float val, int decimalPlaces) {
		append(sb, val, decimalPlaces, 0, DEFAULT_PADDING_CHAR);
	}

	// Convert float value to string and concatenate to given StringBuilder. Padding with zeros
	public static void append(StringBuilder sb, float val, int decimalPlaces, int padding) {
		append(sb, val, decimalPlaces, padding, DEFAULT_PADDING_CHAR);
	}
}
