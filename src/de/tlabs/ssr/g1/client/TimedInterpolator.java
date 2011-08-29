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

package de.tlabs.ssr.g1.client;

import android.os.SystemClock;
import android.view.animation.DecelerateInterpolator;

/**
 * Class to simplify interpolation of values over a given time.
 * 
 * @author Peter Bartz
 */
public class TimedInterpolator {
	private static DecelerateInterpolator adInterpolator = new DecelerateInterpolator();
	private float startValue;
	private float endValue;
	private float diffValue;
	private float startTime;
	private float endTime;
	private float duration; // in ms
	private boolean active = false;
	
	public void setStartEndValues(float start, float end) {
		this.startValue = start;
		this.endValue = end;
		this.diffValue = this.endValue - this.startValue;
	}
	
	public void setDuration(float dur) {
		this.duration = dur;
	}
	
	public float getStartValue() {
		return startValue;
	}

	public void setStartValue(float startValue) {
		this.startValue = startValue;
	}

	public float getEndValue() {
		return endValue;
	}

	public void setEndValue(float endValue) {
		this.endValue = endValue;
	}

	public float getDuration() {
		return duration;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void startInterpolating() {
		startTime = SystemClock.uptimeMillis();
		endTime = startTime + duration;
		active = true;
	}
	
	public float getCurrentValue() {
		float result;
		float currentTime;
		
		// get current time
		currentTime = (float) SystemClock.uptimeMillis();
		
		// check if we stay active after this method returns
		if (currentTime > endTime) {
			active = false;
		}
		
		// calculate result
		result = startValue + adInterpolator.getInterpolation((currentTime - startTime) / duration) * diffValue;
		
		// check bounds of result
		if (startValue < endValue) {
			if (result < startValue) result = startValue;
			else if (result > endValue) result = endValue;
		} else {
			if (result > startValue) result = startValue;
			else if (result < endValue) result = endValue;
		}
		
		return result;
	}
}
