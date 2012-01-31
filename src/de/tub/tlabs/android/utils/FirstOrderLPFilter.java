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

/**
 * Simple first order IIR low-pass filter.
 * 
 * @author Peter Bartz
 */
public class FirstOrderLPFilter {
	private float k, oneMinusK;
	private float lastFilterOutput = 0.0f;

	/**
	 * Constructor
	 * @param k Filter strength [0.0 .. 1.0], higher value filters more
	 */
	public FirstOrderLPFilter(float k) {
		this.k = k;
		oneMinusK = 1.0f - k;
	}
	
	/**
	 * Init filter output with given value
	 * @param value
	 */
	public void init(float value) {
		lastFilterOutput = value;
	}
	
	public float filter(float measurement) {
		lastFilterOutput = lastFilterOutput * k + measurement * oneMinusK;
		return lastFilterOutput;
	}
}
