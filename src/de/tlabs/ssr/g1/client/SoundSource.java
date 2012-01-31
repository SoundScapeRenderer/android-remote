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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.util.Log;

/**
 * Class representing a sound source in an audio scene.
 * 
 * @author Peter Bartz
 */
public class SoundSource extends Entity {
	protected static final String TAG = "SoundSource";
	protected static final float SOURCE_RADIUS = 15f;
	protected static final float SOURCE_HALO_RADIUS = SourcesView.SOURCE_SELECT_RADIUS;
	
	// static fields for drawing
	protected static Paint paint = null;
	protected static Picture planeWavePicture = null;
	
	protected String id;
	protected String name;
	protected SourceModel sourceModel;
	protected String audioFile;
	protected float volume;
	protected float level;
	protected float normalizedLevel; // [-60dB, 12dB] -> [0.0, 1.0] 
	protected boolean muted;
	
	private float[] sourceCircleRadiusCache = {0.0f, 0.0f};	// one key/value pair
	private static final double LOG2 = Math.log(2.0);
		
	
	public static enum SourceModel {
		POINT,
		PLANE
	}
	
	public SoundSource(String id) {
		super();
		setId(id);
		setName("<unnamed>");
		setSourceModel(SourceModel.POINT);
		setAudioFile(null);
		setVolume(0.0f);
		setMuted(false);
		setPositionFixed(false);
	}

	public SoundSource(float posX, float posY, String id, String name, SourceModel sourceModel, String audioFile, float volume, boolean muted) {
		super(posX, posY);
		setId(id);
		setName(name);
		setSourceModel(sourceModel);
		setAudioFile(audioFile);
		setVolume(volume);
		setMuted(muted);
		setPositionFixed(positionFixed);
	}
	
	public static Paint getPaint() {
		return paint;
	}

	public static void setPaint(Paint paint) {
		SoundSource.paint = paint;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SourceModel getSourceModel() {
		return sourceModel;
	}

	public void setSourceModel(SourceModel sourceModel) {
		this.sourceModel = sourceModel;
	}

	public String getAudioFile() {
		return audioFile;
	}

	public void setAudioFile(String audioFile) {
		this.audioFile = audioFile;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
		this.normalizedLevel = (level + 60.0f) / 72.0f; // 12dB headroom
		if (this.normalizedLevel < 0.0f) this.normalizedLevel = 0.0f;
		else if (this.normalizedLevel > 1.0f) this.normalizedLevel = 1.0f;
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}
	
	@Override
	public void draw(Canvas canvas, float inverseScaling, float counterRotation) {
		canvas.save();
		canvas.translate(position[0], position[1]);

		canvas.scale(inverseScaling, -inverseScaling); // re-invert y axis

		// draw halo
		if (positionFixed) {
			SoundSource.paint.setARGB(102, 150, 150, 150);
		} else {
			SoundSource.paint.setARGB(102, 203, 133, 249);
		}
		if (selected) {
			SoundSource.paint.setStyle(Paint.Style.FILL);
		} else {
			SoundSource.paint.setStyle(Paint.Style.STROKE);
			SoundSource.paint.setStrokeWidth(4);
		}
		canvas.drawCircle(0.0f, 0.0f, SOURCE_HALO_RADIUS, SoundSource.paint);
		
		// draw plane if necessary
		if (sourceModel == SourceModel.PLANE){
			canvas.save();
			canvas.rotate(-azimuth);  // rotate in opposite direction because of inverted y axis
			planeWavePicture.draw(canvas);
			canvas.restore();
		}
		
		// calculate source circle radius
		if (sourceCircleRadiusCache[0] != inverseScaling) {
			// log scaling, base 2
			float scaling = (float) (Math.log(1.0f / inverseScaling) / LOG2 - 0.5) / 5.0f;
			// limit value to range
			if (scaling > 2.0) scaling = 2.0f;
			if (scaling < 0.2) scaling = 0.2f;
			// save to cache
			sourceCircleRadiusCache[0] = inverseScaling;
			sourceCircleRadiusCache[1] = SOURCE_RADIUS * scaling;
		}
		
		// draw source circle
		canvas.rotate(-counterRotation + 90.0f); // rotate in opposite direction because of inverted y axis
		SoundSource.paint.setARGB(255, 255, 255, 255);
		if (muted) {
			SoundSource.paint.setStyle(Paint.Style.STROKE);
		} else {
			SoundSource.paint.setStyle(Paint.Style.FILL);
		}
		SoundSource.paint.setStrokeWidth(0);
		canvas.drawCircle(0.0f, 0.0f, sourceCircleRadiusCache[1], SoundSource.paint);
		
		// draw text
		if (selected) {
			SoundSource.paint.setARGB(255, 255, 255, 255);
		} else {
			SoundSource.paint.setARGB(255, 127, 127, 127);
		}
		SoundSource.paint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText(name, 0.0f, -sourceCircleRadiusCache[1] - 5.0f, SoundSource.paint);

		// draw level meter content
		if (!muted) {
			if (level > 0.0f) {
				SoundSource.paint.setARGB(200, 200, 0, 0); // over 0dB -> red
			} else {
				SoundSource.paint.setARGB(200, 0, 200, 0); // under 0dB -> green
			}
			SoundSource.paint.setStyle(Paint.Style.FILL);
			canvas.drawRect(-10.0f, sourceCircleRadiusCache[1] + 6.0f, -10.0f + 20.0f * normalizedLevel, sourceCircleRadiusCache[1] + 10.0f, SoundSource.paint);
		}
		
		// draw level meter border
		SoundSource.paint.setARGB(255, 127, 127, 127);
		SoundSource.paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(-10.0f, sourceCircleRadiusCache[1] + 6.0f, 10.0f, sourceCircleRadiusCache[1] + 10.0f, SoundSource.paint);
		
		canvas.restore();
	}
}
