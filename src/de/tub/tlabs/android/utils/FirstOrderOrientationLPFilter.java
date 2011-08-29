/******************************************************************************
 * Copyright (c) 2006-2011 Quality & Usability Lab                            *
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
 * Specialized for filtering orientations given in degrees: prevents jumps at 0¡/360¡
 * 
 * @author Peter Bartz
 */
public class FirstOrderOrientationLPFilter extends FirstOrderLPFilter {
	private int orientationAngleCorrectionFactor = 0;
	private float lastRawOrientationAngle;

	public FirstOrderOrientationLPFilter(float k) {
		super(k);
	}

	/**
	 * Init filter output with given value
	 * @param value
	 */
	@Override
	public void init(float value) {
		lastRawOrientationAngle = value;
		super.init(value);
	}

	@Override
	public float filter(float measurement) {
		// compensate orientation angle so we have no jumps at 359¡/0¡
		float newRawOrientationAngle = measurement;
		newRawOrientationAngle += ((float) orientationAngleCorrectionFactor * 360.0f);
		if (lastRawOrientationAngle - newRawOrientationAngle < -180.0f) {
			orientationAngleCorrectionFactor--;
			newRawOrientationAngle -= 360.0f;
		} else if (lastRawOrientationAngle - newRawOrientationAngle > 180.0f) {
			orientationAngleCorrectionFactor++;
			newRawOrientationAngle += 360.0f;
		}
		lastRawOrientationAngle = newRawOrientationAngle;
		
		return super.filter(newRawOrientationAngle);
	}
}
