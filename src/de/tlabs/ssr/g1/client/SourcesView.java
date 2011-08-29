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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.http.util.EncodingUtils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;

/**
 * Class containing GUI and logic for display and manipulation of sound sources.
 * 
 * @author Peter Bartz
 */
public class SourcesView extends View implements OnGestureListener, OnDoubleTapListener {
	private static final String TAG = "SourcesView";

	// source won't be select if touch point outside this radius (in pixels)
	public static final float SOURCE_SELECT_RADIUS = 70f;
	// wait at least 10ms between two position update requests
	private static final long MAX_POSITION_UPDATE_FREQ = 30;
	// border when calculation "fit scene into screen"
	private static final float FIT_SCENE_PIXEL_BORDER = 60;
	private static final float FIT_SCENE_PIXEL_BORDER_2 = FIT_SCENE_PIXEL_BORDER * 2.0f;

	public enum TransformationMode {
		TRANSLATE, ROTATE
	}

	private static Paint paint;
	private Picture sizeScalePicture = new Picture();
	private Matrix viewportTransformation;
	private Matrix inverseViewportTransformation;
	private Matrix newViewportTransformation;
	private float[] selectionOffset;
	private float[] touchPoint;
	private float[] firstScrollPoint = { 0.0f, 0.0f };
	private float[] point = { 0.0f, 0.0f }; // for common use, to avoid object creation
	private SoundSource lastTouchSoundSource;
	private ByteBuffer buffer;
	private float currentScaling = 0.5f;
	private float currentInverseScaling = 1.0f / currentScaling;
	private float[] currentTranslation = { 0.0f, 80.0f };
	private float[] currentSavedTranslation = { 0.0f, 0.0f };
	private boolean scrolling = false;
	private float currentCenterRotation = 0.0f;
	private long lastSendTime = 0;
	private int currentOrientation;
	private TransformationMode transformationMode = TransformationMode.TRANSLATE;
	protected TimedInterpolator scalingInterpolator;
	protected TimedInterpolator translationXInterpolator;
	protected TimedInterpolator translationYInterpolator;
	protected TimedInterpolator rotationInterpolator;
	protected TimedInterpolator centerRotationInterpolator;
	private OrientationEventListener orientationEventListener;
	private GestureDetector gestureDetector;
	private boolean viewSizeInitialized = false;

	// flags
	private boolean viewportFlag = true;
	private boolean orientationFlag = true;
	private boolean transformToFitSceneFlag = false;

	public SourcesView(Context context) {
		super(context);
		init();
	}

	public SourcesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SourcesView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		// make this view focusable
		setFocusable(true);

		// init fields
		viewportTransformation = new Matrix();
		newViewportTransformation = new Matrix();
		inverseViewportTransformation = new Matrix();
		
		selectionOffset = new float[2];
		touchPoint = new float[2];
		
		buffer = ByteBuffer.allocate(1024);
		
		scalingInterpolator = new TimedInterpolator();
		scalingInterpolator.setDuration(800);
		translationXInterpolator = new TimedInterpolator();
		translationXInterpolator.setDuration(800);
		translationYInterpolator = new TimedInterpolator();
		translationYInterpolator.setDuration(800);
		rotationInterpolator = new TimedInterpolator();
		rotationInterpolator.setDuration(800);
		centerRotationInterpolator = new TimedInterpolator();
		centerRotationInterpolator.setDuration(800);
		
		currentOrientation = getContext().getResources().getConfiguration().orientation;
		setOrientationFlag(true);
		
		if (SourcesView.paint == null) {
			SourcesView.paint = new Paint();
			SourcesView.paint.setAntiAlias(false);
			SourcesView.paint.setStrokeWidth(0);
			SourcesView.paint.setTextAlign(Paint.Align.CENTER);
			SourcesView.paint.setTextSize(9.0f);
		}

		// set up orientation event listener
		orientationEventListener = new OrientationEventListener(getContext(),
				SensorManager.SENSOR_DELAY_NORMAL) {
			@Override
			public void onOrientationChanged(int orientation) {
				if ((orientation >= 80 && orientation <= 100)
						|| (orientation >= 260 && orientation <= 280)) { // landscape
					setOrientation(Configuration.ORIENTATION_LANDSCAPE);
				} else if ((orientation >= 350 || orientation <= 10)
							|| (orientation >= 170 && orientation <= 190)) { // portrait
					setOrientation(Configuration.ORIENTATION_PORTRAIT);
				}
			}
		};
		if (!GlobalData.orientationTrackingEnabled)	// orientation tracking and screen rotation tracking don't go together
			orientationEventListener.enable();

		// set up gesture detector
		gestureDetector = new GestureDetector(getContext(), this);
		gestureDetector.setIsLongpressEnabled(false);
		gestureDetector.setOnDoubleTapListener(this);

		// init viewport transformation matrix
		recalculateViewportTransformation();
	}

	public float getCurrentScaling() {
		return currentScaling;
	}

	public void setCurrentScaling(float currentScaling) {
		this.currentScaling = currentScaling;
		this.currentInverseScaling = 1.0f / currentScaling;
		setViewportFlag(true);
	}

	public float getCurrentCenterRotation() {
		return currentCenterRotation;
	}

	public void setCurrentCenterRotation(float currentCenterRotation) {
		this.currentCenterRotation = currentCenterRotation;
		setViewportFlag(true);
	}

	public float getCurrentInverseScaling() {
		return currentInverseScaling;
	}

	public void setCurrentInverseScaling(float currentInverseScaling) {
		this.currentInverseScaling = currentInverseScaling;
		this.currentScaling = 1.0f / currentInverseScaling;
		setViewportFlag(true);
	}

	public float[] getCurrentTranslation() {
		return currentTranslation;
	}

	public void setCurrentTranslation(float[] currentTranslation) {
		setCurrentTranslation(currentTranslation[0], currentTranslation[1]);
	}

	public void setCurrentTranslation(float currentTranslationX,
			float currentTranslationY) {
		this.currentTranslation[0] = currentTranslationX;
		this.currentTranslation[1] = currentTranslationY;
		setViewportFlag(true);
	}

	public boolean getAndClearViewportFlag() {
		boolean returnValue = viewportFlag;
		viewportFlag = false;
		return returnValue;
	}

	public void setViewportFlag(boolean viewportFlag) {
		this.viewportFlag = viewportFlag;
	}

	public boolean getAndClearOrientationFlag() {
		boolean returnValue = orientationFlag;
		orientationFlag = false;
		return returnValue;
	}

	public void setOrientationFlag(boolean orientationFlag) {
		this.orientationFlag = orientationFlag;
	}

	public void setTransformationMode(TransformationMode mode) {
		this.transformationMode = mode;
	}

	public void zoomView(float factor) {
		float newScaling;

		if (scalingInterpolator.isActive()) {
			newScaling = scalingInterpolator.getEndValue() * factor;
		} else {
			newScaling = currentScaling * factor;
		}

		scalingInterpolator.setStartEndValues(currentScaling, newScaling);
		scalingInterpolator.startInterpolating();
	}

	public void translateView(float transX, float transY) {
		float newTransX;
		float newTransY;

		if (transX != 0.0f) {
			if (translationXInterpolator.isActive()) {
				newTransX = translationXInterpolator.getEndValue() + transX;
			} else {
				newTransX = currentTranslation[0] + transX;
			}
			translationXInterpolator.setStartEndValues(currentTranslation[0],
					newTransX);
			translationXInterpolator.startInterpolating();
		}

		if (transY != 0.0f) {
			if (translationYInterpolator.isActive()) {
				newTransY = translationYInterpolator.getEndValue() + transY;
			} else {
				newTransY = currentTranslation[1] + transY;
			}
			translationYInterpolator.setStartEndValues(currentTranslation[1],
					newTransY);
			translationYInterpolator.startInterpolating();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		setViewportFlag(true);
		viewSizeInitialized = true;
		if (transformToFitSceneFlag) {
			transformToFitSceneFlag = false;
			transformToFitScene();
		}
	}

	private void recalculateViewportTransformation() {
		recalculateViewportTransformation(this.viewportTransformation,
				currentCenterRotation, currentScaling, currentTranslation);
		viewportTransformation.invert(inverseViewportTransformation);
	}

	private void recalculateViewportTransformation(Matrix dstMatrix,
			float centerRotation, float scaling, float[] translation) {
		dstMatrix.reset();
		// translate to center
		dstMatrix.preTranslate((float) getWidth() / 2.0f, (float) getHeight() / 2.0f);
		// rotate around center
		dstMatrix.preRotate(-centerRotation); // negative rotation, because y axis not yet inverted
		// translate
		dstMatrix.preTranslate(translation[0], translation[1]);
		// scale and invert y axis
		dstMatrix.preScale(scaling, -scaling);
		// rotate to look north always (instead of east)
		dstMatrix.preRotate(90.0f);
	}

	private void recalculateSizeScale() {
		float sizeInMeters;
		double upperBoundingPowOfTen;
		double scaleLengthInMeters;
		float scaleLengthInPixels;
		float scaleStartInPixels = 20.0f;

		// get size of short display edge in meters
		sizeInMeters = inverseViewportTransformation.mapRadius(Math.min(getWidth(), 
				getHeight())- scaleStartInPixels * 2.0f);

		// find upper bounding power of ten (0.01, 0.1, 1, 10, 100, etc.)
		if (sizeInMeters > 1.0) {
			upperBoundingPowOfTen = 10.0;
			while (true) {
				if (sizeInMeters / upperBoundingPowOfTen > 1.0f) {
					upperBoundingPowOfTen *= 10.0;
				} else {
					break;
				}
			}
		} else {
			upperBoundingPowOfTen = 1.0;
			while (true) {
				if (sizeInMeters / upperBoundingPowOfTen >= 0.1f) {
					break;
				} else {
					upperBoundingPowOfTen *= 0.1;
				}
			}
		}

		// map to subdivisions
		scaleLengthInMeters = sizeInMeters / upperBoundingPowOfTen;
		if (scaleLengthInMeters > 0.75)
			scaleLengthInMeters = 0.75;
		else if (scaleLengthInMeters > 0.5)
			scaleLengthInMeters = 0.5;
		else if (scaleLengthInMeters > 0.3)
			scaleLengthInMeters = 0.3;
		else if (scaleLengthInMeters > 0.2)
			scaleLengthInMeters = 0.2;
		else if (scaleLengthInMeters > 0.15)
			scaleLengthInMeters = 0.15;
		else
			scaleLengthInMeters = 0.1;
		scaleLengthInMeters *= upperBoundingPowOfTen;

		// get corresponding scale length in pixels
		scaleLengthInPixels = viewportTransformation.mapRadius((float) scaleLengthInMeters);

		// generate picture
		Canvas canvas = sizeScalePicture.beginRecording(0, 0);
		float y = getHeight() - 5.0f;
		float xHalf = scaleStartInPixels + scaleLengthInPixels / 2.0f;
		float scaleEndInPixels = scaleStartInPixels + scaleLengthInPixels;

		SourcesView.paint.setARGB(255, 255, 255, 255);
		SourcesView.paint.setAntiAlias(true);
		canvas.drawLine(scaleStartInPixels - 1.0f, y, scaleEndInPixels + 1.0f, y, SourcesView.paint);
		canvas.drawLine(scaleStartInPixels, y, scaleStartInPixels, y - 5.0f, SourcesView.paint);
		canvas.drawLine(xHalf, y, xHalf, y - 5.0f, SourcesView.paint);
		canvas.drawLine(scaleEndInPixels, y, scaleEndInPixels, y - 5.0f, SourcesView.paint);

		canvas.drawText("0m", scaleStartInPixels, y - 8.0f, SourcesView.paint);
		canvas.drawText(String.valueOf((float) scaleLengthInMeters / 2.0f) + "m", xHalf, y - 8.0f, SourcesView.paint);
		canvas.drawText(String.valueOf((float) scaleLengthInMeters) + "m", scaleEndInPixels, y - 8.0f, SourcesView.paint);
	}

	@Override
	public void onDraw(Canvas canvas) {
		// zooming animation
		if (scalingInterpolator.isActive()) {
			setCurrentScaling(scalingInterpolator.getCurrentValue());
		}

		// translation animation
		if (translationXInterpolator.isActive() || translationYInterpolator.isActive()) {
			float transX = currentTranslation[0];
			float transY = currentTranslation[1];
			if (translationXInterpolator.isActive()) {
				transX = translationXInterpolator.getCurrentValue();
			}
			if (translationYInterpolator.isActive()) {
				transY = translationYInterpolator.getCurrentValue();
			}
			setCurrentTranslation(transX, transY);
		}

		// center rotation animation
		if (centerRotationInterpolator.isActive()) {
			setCurrentCenterRotation(centerRotationInterpolator.getCurrentValue());
		}

		// calculate current viewport matrix if necessary
		if (getAndClearViewportFlag()) {
			recalculateViewportTransformation();
			recalculateSizeScale();
		}

		// clear background
		canvas.drawColor(0xFF000000);

		// draw audio scene
		canvas.setMatrix(viewportTransformation);
		synchronized (GlobalData.audioScene) {
			GlobalData.audioScene.draw(canvas, currentInverseScaling);
		}

		// reset matrix
		canvas.setMatrix(null);

		// draw size scale
		sizeScalePicture.draw(canvas);
	}

	public float[][] getSceneBounds(Matrix viewportTransformation) {
		float[] point = { 0.0f, 0.0f };

		// map reference point to screen coordinate system
		viewportTransformation.mapPoints(point);

		// set min/max to reference position
		float[][] minMaxXY = { { point[0], point[1] }, { point[0], point[1] } }; // two points: minXY and maxXY

		synchronized (GlobalData.audioScene) {
			int numSources = GlobalData.audioScene.getNumSoundSources();
			SoundSource s = null;
			for (int i = 0; i < numSources; i++) {
				s = GlobalData.audioScene.getSoundSource(i);
				point[0] = s.getX();
				point[1] = s.getY();
	
				// map to reference coordinate system
				GlobalData.audioScene.mapPoint(point);
				viewportTransformation.mapPoints(point);
	
				// check if point lies outwards current bounds
				if (point[0] < minMaxXY[0][0])
					minMaxXY[0][0] = point[0];
				if (point[0] > minMaxXY[1][0])
					minMaxXY[1][0] = point[0];
				if (point[1] < minMaxXY[0][1])
					minMaxXY[0][1] = point[1];
				if (point[1] > minMaxXY[1][1])
					minMaxXY[1][1] = point[1];
			}
		}

		return minMaxXY;
	}

	public void transformToFitScene() {
		float xScalingFactor = 0.0f;
		float yScalingFactor = 0.0f;
		float xDiff;
		float yDiff;
		float[][] sceneBounds;
		float minBounds[];
		float maxBounds[];

		// if size of this view not yet initialized, wait until it is initialized
		if (!viewSizeInitialized) {
			transformToFitSceneFlag = true;
			return;
		}
		
		// get scene bounds in screen coordinates
		recalculateViewportTransformation();	// just to be sure we are using the latest values
		sceneBounds = getSceneBounds(viewportTransformation);
		minBounds = sceneBounds[0];
		maxBounds = sceneBounds[1];

		// get max x and y distances in pixels
		xDiff = maxBounds[0] - minBounds[0];
		yDiff = maxBounds[1] - minBounds[1];

		// recalculate scaling
		if (xDiff > 0 || yDiff > 0) {
			// calculate best scaling in x and y direction
			if (xDiff > 0) {
				xScalingFactor = ((float) getWidth() - FIT_SCENE_PIXEL_BORDER_2) / xDiff;
			} else {
				xScalingFactor = Float.POSITIVE_INFINITY;
			}

			if (yDiff > 0) {
				yScalingFactor = ((float) getHeight() - FIT_SCENE_PIXEL_BORDER_2) / yDiff;
			} else {
				yScalingFactor = Float.POSITIVE_INFINITY;
			}

			// set best scaling
			float scalingFactor = Math.min(xScalingFactor, yScalingFactor);
			zoomView(scalingFactor);
			recalculateViewportTransformation(newViewportTransformation, currentCenterRotation, 
					currentScaling * scalingFactor, currentTranslation);
		} else {
			newViewportTransformation.set(viewportTransformation);
		}

		// get scene bounds using new scaling
		sceneBounds = getSceneBounds(newViewportTransformation);
		minBounds = sceneBounds[0];
		maxBounds = sceneBounds[1];

		// get max x and y distances in pixels
		xDiff = maxBounds[0] - minBounds[0];
		yDiff = maxBounds[1] - minBounds[1];

		// calculate translation to center the scene
		float transX = -minBounds[0] + (getWidth() - xDiff) / 2;
		float transY = -minBounds[1] + (getHeight() - yDiff) / 2;
		if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			translateView(transX, transY);
		} else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			translateView(transY, -transX);
		}
	}

	public void setOrientation(int newOrientation) {
		if (currentOrientation == newOrientation)
			return;

		if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) { // portrait -> landscape
			centerRotationInterpolator.setStartEndValues(0.0f, -90.0f);
		} else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) { // landscape -> portrait
			centerRotationInterpolator.setStartEndValues(-90.0f, 0.0f);
		} else { // unsupported orientation, this should never happen
			return;
		}

		// save current orientation
		currentOrientation = newOrientation;
		setOrientationFlag(true);

		// start interpolating
		centerRotationInterpolator.startInterpolating();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// pass event to gesture detector
		gestureDetector.onTouchEvent(event);
		return true;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		scrolling = false;

		synchronized (GlobalData.audioScene) {
			// determine transformed coordinate of touch point
			touchPoint[0] = event.getX();
			touchPoint[1] = event.getY();
			inverseViewportTransformation.mapPoints(touchPoint);
			GlobalData.audioScene.inverseMapPoint(touchPoint);

			// try to find nearest sound source
			lastTouchSoundSource = GlobalData.audioScene.getNearestSoundSource(touchPoint);
			if (lastTouchSoundSource != null) {
				// get distance (touch point to source) in pixels
				selectionOffset[0] = lastTouchSoundSource.getX();
				selectionOffset[1] = lastTouchSoundSource.getY();
				GlobalData.audioScene.mapPoint(selectionOffset);
				viewportTransformation.mapPoints(selectionOffset);
				selectionOffset[0] -= event.getX();
				selectionOffset[1] -= event.getY();
				float distance = FloatMath.sqrt(selectionOffset[0] * selectionOffset[0] + 
						selectionOffset[1] * selectionOffset[1]);

				// select source?
				if (distance > SOURCE_SELECT_RADIUS) {
					lastTouchSoundSource = null;
				}
			}
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (lastTouchSoundSource == null) {
			if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
				translateView(velocityX * 0.3f, velocityY * 0.3f);
			} else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				translateView(velocityY * 0.3f, -velocityX * 0.3f);
			}
		} else {
			// no flinging for sound sources
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent firstEvent, MotionEvent thisEvent, float distanceX, float distanceY) {
		
		// transform sound source or surface?
		if (lastTouchSoundSource != null) { // transform sound source
			synchronized (GlobalData.audioScene) {
				// is lastTouchSoundSource selected?
				if (!lastTouchSoundSource.isSelected()) {
					// select only this source
					GlobalData.audioScene.deselectAllSoundSources();
					GlobalData.audioScene.selectSoundSource(lastTouchSoundSource);
				}

				// save positions of sources if this is first scroll event
				if (!scrolling) {
					scrolling = true;
					firstScrollPoint[0] = firstEvent.getX();
					firstScrollPoint[1] = firstEvent.getY();
					ArrayList<SoundSource> selectedSources = GlobalData.audioScene.getSelectedSoundSources();
					int numSources = selectedSources.size();
					for (int i = 0; i < numSources; i++) { // loop through all currently selected sources
						selectedSources.get(i).getXY(point);
						GlobalData.audioScene.mapPoint(point);
						viewportTransformation.mapPoints(point);
						selectedSources.get(i).savePosition(point);
					}
				}

				// enough time elapsed to do next position update?
				long currentTime = SystemClock.uptimeMillis();
				if (currentTime - lastSendTime < MAX_POSITION_UPDATE_FREQ)
					return true;
				lastSendTime = currentTime;

				// translate or rotate?
				if (transformationMode == TransformationMode.TRANSLATE) { // translate
					// generate server request string
					String strMsg = "<request>";
					ArrayList<SoundSource> selectedSources = GlobalData.audioScene.getSelectedSoundSources();
					int numSources = selectedSources.size();
					SoundSource soundSource;
					for (int i = 0; i < numSources; i++) { // loop through all currently selected sources
						soundSource = selectedSources.get(i);
						
						// if source is fixed, skip it
						if (soundSource.isPositionFixed())
							continue;	

						strMsg += "<source id='" + soundSource.getId() + "'>";
						// transform screen coords into object coords, consider offset
						point[0] = soundSource.getSavedX() + thisEvent.getX() - firstScrollPoint[0];
						point[1] = soundSource.getSavedY() + thisEvent.getY() - firstScrollPoint[1];
						inverseViewportTransformation.mapPoints(point);

						if (soundSource.getSourceModel() == SoundSource.SourceModel.PLANE) { // recalculate orientation for plane waves
							float norm = FloatMath.sqrt(point[0] * point[0] + point[1] * point[1]); // for plane waves, if source is movable
							if (norm != 0.0f) {
								float newAzimuth;
								if (point[1] >= 0.0f)
									newAzimuth = (float) (Math.acos(point[0] / norm) / Math.PI * 180.0f)
									- 180.0f + GlobalData.audioScene.getReference().getAzimuth();
								else
									newAzimuth = (float) -(Math.acos(point[0] / norm) / Math.PI * 180.0f) 
									- 180.0f + GlobalData.audioScene.getReference().getAzimuth();
								strMsg += "<orientation azimuth='"
									+ String.valueOf(newAzimuth) + "'/>";
							}
						}

						GlobalData.audioScene.inverseMapPoint(point);
						strMsg += "<position x='" + String.valueOf(point[0])
						+ "' y='" + String.valueOf(point[1]) + "'/>";
						strMsg += "</source>";
					}
					strMsg += "</request>\0";

					// send changes to server
					sendToServer(strMsg);
				} else { // rotate
					// not implemented
				}
			}
		} else { // transform surface
			if (!scrolling) {
				scrolling = true;
				firstScrollPoint[0] = thisEvent.getX();
				firstScrollPoint[1] = thisEvent.getY();
				currentSavedTranslation[0] = currentTranslation[0];
				currentSavedTranslation[1] = currentTranslation[1];
			}

			// translate or rotate?
			if (transformationMode == TransformationMode.TRANSLATE) { // translate
				if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
					point[0] = thisEvent.getX() - firstScrollPoint[0];
					point[1] = thisEvent.getY() - firstScrollPoint[1];
				} else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
					point[0] = thisEvent.getY() - firstScrollPoint[1];
					point[1] = -(thisEvent.getX() - firstScrollPoint[0]);
				}
				setCurrentTranslation(currentSavedTranslation[0] + point[0], currentSavedTranslation[1] + point[1]);
			} else { // rotate
				// not implemented
			}
		}

		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		synchronized (GlobalData.audioScene) {
			if (lastTouchSoundSource == null) { // fit scene
				transformToFitScene();
			} else { // (un)mute sound sources
				// generate server request string
				String strMsg = "<request>";
				if (!lastTouchSoundSource.isSelected()) {
					// (un)mute one source
					strMsg += "<source id='" + lastTouchSoundSource.getId() + "' mute='" + (lastTouchSoundSource.isMuted() ? "false" : "true") + "'/>";
				} else {
					// (un)mute current selected group of sources
					ArrayList<SoundSource> selectedSources = GlobalData.audioScene.getSelectedSoundSources();
					int numSources = selectedSources.size();
					SoundSource soundSource;
					for (int i = 0; i < numSources; i++) { // loop through all currently selected sources
						soundSource = selectedSources.get(i);
						strMsg += "<source id='" + soundSource.getId() + "' mute='" + (soundSource.isMuted() ? "false" : "true") + "'/>";
					}
				}
				strMsg += "</request>\0";
				sendToServer(strMsg);
			}
		}

		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		// nothing
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		synchronized (GlobalData.audioScene) {
			// (de)select one or all sound sources
			if (lastTouchSoundSource != null) {
				if (lastTouchSoundSource.isSelected()) {
					GlobalData.audioScene.deselectSoundSource(lastTouchSoundSource);
				} else {
					GlobalData.audioScene.selectSoundSource(lastTouchSoundSource);
				}
			} else {
				if (GlobalData.audioScene.getSelectedSoundSources().isEmpty()) {
					GlobalData.audioScene.selectAllSoundSources();
				} else {
					GlobalData.audioScene.deselectAllSoundSources();
				}
			}
		}

		return true;
	}

	void sendToServer(String strMsg) {
		byte[] msg = EncodingUtils.getAsciiBytes(strMsg);
		if (msg.length > buffer.capacity()) // check if buffer long enough
			buffer = ByteBuffer.wrap(msg);
		buffer.clear();
		buffer.put(msg);
		buffer.flip();
		try {
			GlobalData.socketChannel.write(buffer);
		} catch (IOException e) {
			Log.d(TAG, "error on write: " + e.getMessage());
		}
	}
}
