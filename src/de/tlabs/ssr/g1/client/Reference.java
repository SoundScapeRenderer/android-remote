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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;

/**
 * Class representing the reference point in an audio scene (i.e. the listener).
 * 
 * @author Peter Bartz
 */
public class Reference extends Entity {
	private static final String TAG = "Reference";
	
	// static fields for drawing
	protected static Paint paint = null;
	protected static Picture arrowPicture = null;
	
	public static Paint getPaint() {
		return paint;
	}

	public static void setPaint(Paint paint) {
		Reference.paint = paint;
	}

	@Override
	public void draw(Canvas canvas, float inverseScaling, float counterRotation) {
		canvas.save();
		canvas.scale(inverseScaling, -inverseScaling);
		arrowPicture.draw(canvas);
		canvas.restore();
	}
}
