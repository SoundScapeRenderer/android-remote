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

package de.tlabs.ssr.g1.client;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

/**
 * Custom minimalistic look of the standard SeekBar.
 * 
 * @author Peter Bartz
 */
public class CustomSeekBar extends SeekBar {
	private static Paint paint;
	
	private int visualProgress; 

	public CustomSeekBar(Context context) {
		super(context);
		
		if (paint == null) {
			paint = new Paint();
			paint.setColor(Color.WHITE);
		}
		
		visualProgress = getProgress();
	}

	public CustomSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		if (paint == null) {
			paint = new Paint();
			paint.setColor(Color.WHITE);
		}
		
		visualProgress = getProgress();
	}

	public CustomSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		if (paint == null) {
			paint = new Paint();
			paint.setColor(Color.WHITE);
		}
		
		visualProgress = getProgress();
	}

	public void setVisualProgress(int progress) {
		visualProgress = progress;
	}
	
	public int getVisualProgress() {
		return visualProgress;
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		int width, height;
		width = this.getWidth();
		height = this.getHeight();

		canvas.drawColor(getResources().getColor(R.drawable.col_btn_default));
		
		canvas.drawLine(0, 0, width, 0, paint);
		canvas.drawRect(0, 0, Math.round((float) this.visualProgress / (float) this.getMax() * (float) width), height, paint);
	}
}
