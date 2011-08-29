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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.FloatMath;

/**
 * Class representing an audio scene with all its entities and the current
 * transport state and volume.
 * It can also draw itself to a Canvas.
 * 
 * @author Peter Bartz
 */
public class AudioScene {
	private static final String TAG = "AudioScene";
	
	private ArrayList<SoundSource> soundSources;
	private ArrayList<Loudspeaker> loudspeakers;
	private ArrayList<SoundSource> selectedSoundSources;
	private Reference reference = null;
	private Matrix referenceTransformation;
	private Matrix inverseReferenceTransformation;
	private boolean drawSourcesFixedEnabled = false;	// draw sources/speakers or listener fixed?
	private float volume;	// in dB
	private boolean volumeFlag;
	private TransportState transportState;
	private boolean transportStateFlag;
	
	// static fields for drawing
	private static Paint paint = null;
	
	public enum TransportState {
		PLAYING, PAUSED
	}

	public AudioScene() {
		soundSources = new ArrayList<SoundSource>();
		loudspeakers = new ArrayList<Loudspeaker>();
		selectedSoundSources = new ArrayList<SoundSource>();
		referenceTransformation = new Matrix();
		inverseReferenceTransformation = new Matrix();
		setVolume(0.0f);
		setTransportState(TransportState.PAUSED);
	}
	
	public static Paint getPaint() {
		return paint;
	}

	public static void setPaint(Paint paint) {
		AudioScene.paint = paint;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
		volumeFlag = true;
	}

	public TransportState getTransportState() {
		return transportState;
	}
	
	public void setTransportState(TransportState state) {
		this.transportState = state;
		transportStateFlag = true;
	}
	
	public boolean getAndClearVolumeFlag() {
		boolean retVal = volumeFlag;
		volumeFlag = false;
		return retVal;
	}
	
	public boolean getAndClearTransportStateFlag() {
		boolean retVal = transportStateFlag;
		transportStateFlag = false;
		return retVal;
	}
	
	public void addSoundSource(SoundSource s) {
		soundSources.add(s);
	}
	
	public boolean removeSoundSource(SoundSource s) {
		return soundSources.remove(s);
	}
	
	public boolean removeSoundSource(String id) {
		return soundSources.remove(getSoundSource(id));
	}

	public SoundSource getSoundSource(int index) {
		try {
			return soundSources.get(index);
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
	
	public SoundSource getSoundSource(String id) {
		if (id == null) return null;
		
		int numSources = soundSources.size();
		SoundSource s = null;
		for (int i = 0; i < numSources; i++) {
			s = soundSources.get(i);
			if(s.getId().equals(id))
				return s;
		}
		
		// source not found
		return null;
	}
	
	public void addLoudspeaker(Loudspeaker l) {
		loudspeakers.add(l);
	}
	
	public boolean removeLoudspeaker(Loudspeaker l) {
		return loudspeakers.remove(l);
	}
	
	public Loudspeaker getLoudspeaker(int index) {
		try {
			return loudspeakers.get(index);
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
	
	public void recalculateReferenceTransformation() {
		// draw reference fixed?
		if (!drawSourcesFixedEnabled) {	// draw reference fixed
			referenceTransformation.reset();
			// rotate
			referenceTransformation.preRotate(-reference.getAzimuth());
			// translate to center
			referenceTransformation.preTranslate(-reference.getX(), -reference.getY());
			// save inverted matrix
			referenceTransformation.invert(inverseReferenceTransformation);
		} else { // draw sources fixed
			// rotate/translate reference and speakers 
			if (reference != null) {
				referenceTransformation.reset();
				// rotate
				referenceTransformation.preRotate(-90);
				// save inverted matrix
				referenceTransformation.invert(inverseReferenceTransformation);
			}
		}
	}

	public void draw(Canvas canvas, float inverseScaling) {
		float referenceRotation = -90.0f;
		
		// save current transformation matrix
		canvas.save();

		// calculate current reference transformation matrix if necessary
		if (reference != null) {
			if (reference.getAndClearAzimuthFlag() | reference.getAndClearPositionFlag()) {
				recalculateReferenceTransformation();
			}
		}
		
		// draw reference fixed?
		if (!drawSourcesFixedEnabled) {	// draw reference fixed
			// draw speakers
			int numSpeakers = loudspeakers.size();
			for (int i = 0; i < numSpeakers; i++) {
				loudspeakers.get(i).draw(canvas, inverseScaling, 0.0f);
			}
	
			if (reference != null) {
				// draw reference
				reference.draw(canvas, inverseScaling, 0.0f);
				
				referenceRotation = -reference.getAzimuth();
				
				// transform into reference coordinate system
				canvas.concat(referenceTransformation);
			}
			
			// draw sound sources
			int numSources = soundSources.size();
			for (int i = 0; i < numSources; i++) {
				soundSources.get(i).draw(canvas, inverseScaling, -referenceRotation);
			}
		} else { // draw sources fixed
			// rotate/translate reference and speakers 
			if (reference != null) {
				referenceRotation = -90.0f;

				// transform into reference coordinate system
				canvas.concat(referenceTransformation);

				// save matrix
				canvas.save();
				
				// translate to center
				canvas.translate(reference.getX(), reference.getY());
				// rotate
				canvas.rotate(reference.getAzimuth());

				// draw speakers
				int numSpeakers = loudspeakers.size();
				for (int i = 0; i < numSpeakers; i++) {
					loudspeakers.get(i).draw(canvas, inverseScaling, 0.0f);
				}
	
				// draw reference
				reference.draw(canvas, inverseScaling, 0.0f);
				
				// restore matrix
				canvas.restore();
			}
			
			// draw sound sources
			int numSources = soundSources.size();
			for (int i = 0; i < numSources; i++) {
				soundSources.get(i).draw(canvas, inverseScaling, -referenceRotation);
			}
		}
		
		// restore previous transformation matrix
		canvas.restore();
	}

	// position must be given in sound source coordinates 
	public SoundSource getNearestSoundSource(float[] pos) {
		float dX, dY, distance, bestDistance;
		SoundSource nearestSource = null;
		
		// find nearest sound source
		bestDistance = Float.POSITIVE_INFINITY;
		int numSources = soundSources.size();
		SoundSource s = null;
		for (int i = 0; i < numSources; i++) {
			s = soundSources.get(i);
			dX = pos[0] - s.getX();
			dY = pos[1] - s.getY();
			distance = FloatMath.sqrt(dX*dX + dY*dY);
			if(distance < bestDistance) {
				bestDistance = distance;
				nearestSource = s;
			}
		}
		
		return nearestSource;
	}
	
	// transform point into reference coordinate system
	public void mapPoint(float[] p) {
		referenceTransformation.mapPoints(p);
	}
	
	// transform point out of reference coordinate system
	public void inverseMapPoint(float[] p) {
		inverseReferenceTransformation.mapPoints(p);
	}
	
	public void selectSoundSource(SoundSource source) {
		// check if sound source already contained
		if (selectedSoundSources.contains(source))
			return;
		
		// add new source to list of selected source
		selectedSoundSources.add(source);
		
		// set selected-flag in source itself
		source.setSelected(true);
	}
	
	public void deselectSoundSource(SoundSource source) {
		// deselect source
		selectedSoundSources.remove(source);
		source.setSelected(false);
	}
	
	public void selectAllSoundSources() {
		int numSources = soundSources.size();
		for (int i = 0; i < numSources; i++) {
			soundSources.get(i).setSelected(true);
		}
		selectedSoundSources.addAll(soundSources);
	}
	
	public void deselectAllSoundSources() {
		int numSources = soundSources.size();
		for (int i = 0; i < numSources; i++) {
			soundSources.get(i).setSelected(false);
		}
		selectedSoundSources.clear();
	}
	
	public ArrayList<SoundSource> getSelectedSoundSources() {
		return selectedSoundSources;
	}

	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}

	public void reset() {
		soundSources.clear();
		loudspeakers.clear();
		selectedSoundSources.clear();
	}
	
	/*public float[][] getSceneBounds() {
		float[] point = {0.0f, 0.0f};
		float[][] minMaxXY = {{0.0f, 0.0f}, {0.0f, 0.0f}}; // two points: minXY and maxXY
										// 0.0f is reference, so it's automatically included
		
		int numSources = soundSources.size();
		SoundSource s = null;
		for (int i = 0; i < numSources; i++) {
			s = soundSources.get(i);
			point[0] = s.getX();
			point[1] = s.getY();
			
			// map to reference coordinate system
			mapPoint(point);
			
			// check if point lies outwards current bounds
			if(point[0] < minMaxXY[0][0])
				minMaxXY[0][0] = point[0];
			if(point[0] > minMaxXY[1][0])
				minMaxXY[1][0] = point[0];
			if(point[1] < minMaxXY[0][1])
				minMaxXY[0][1] = point[1];
			if(point[1] > minMaxXY[1][1])
				minMaxXY[1][1] = point[1];
		}
		
		return minMaxXY;
	}*/

	public int getNumSoundSources() {
		return soundSources.size();
	}

	public boolean isDrawSourcesFixedEnabled() {
		return drawSourcesFixedEnabled;
	}

	public void setDrawSourcesFixedEnabled(boolean drawSourcesFixedEnabled) {
		this.drawSourcesFixedEnabled = drawSourcesFixedEnabled;
	}
}
