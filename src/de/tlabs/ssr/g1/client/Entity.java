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

/**
 * Abstact class representing an entity in a audioscene.
 * 
 * @author Peter Bartz
 */public abstract class Entity {
	private static final String TAG = "Entity";
	
	protected float[] position = {0.0f, 0.0f};
	protected float[] transformedPosition = {0.0f, 0.0f};
	protected float[] savedPosition = {0.0f, 0.0f};
	protected boolean selected;
	protected float azimuth;
	protected boolean positionFixed = false;
	protected boolean positionFlag = true;
	protected boolean azimuthFlag = true;
	protected boolean selectedFlag = true;
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public float getX() {
		return position[0];
	}

	public void setX(float x) {
		this.position[0] = x;
		positionFlag = true;
	}

	public float getY() {
		return position[1];
	}
	
	public void setY(float y) {
		this.position[1] = y;
		positionFlag = true;
	}
	
	public float[] getXY() {
		return position.clone();
	}
	
	public void getXY(float[] pos) {
		pos[0] = getX();
		pos[1] = getY();
	}
	
	public void setXY(float[] position) {
		setX(position[0]);		
		setY(position[1]);		
	}

	public void setXY(float x, float y) {
		setX(x);		
		setY(y);		
	}

	public void savePosition(float x, float y) {
		savedPosition[0] = x;
		savedPosition[1] = y;
	}
	
	public void savePosition(float[] pos) {
		savedPosition[0] = pos[0];
		savedPosition[1] = pos[1];
	}
	
	public float getSavedX() {
		return savedPosition[0];
	}
	
	public float getSavedY() {
		return savedPosition[1];
	}
	
	public float getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(float azimuth) {
		this.azimuth = azimuth;
		azimuthFlag = true;
	}
	
	public boolean isPositionFixed() {
		return positionFixed;
	}
	
	public void setPositionFixed(boolean positionFixed) {
		this.positionFixed = positionFixed;
	}

	public boolean getAndClearPositionFlag() {
		boolean returnValue = positionFlag;
		positionFlag = false;
		return returnValue;
	}

	public void setPositionFlag(boolean positionFlag) {
		this.positionFlag = positionFlag;
	}

	public boolean getAndClearAzimuthFlag() {
		boolean returnValue = azimuthFlag;
		azimuthFlag = false;
		return returnValue;
	}

	public void setAzimuthFlag(boolean azimuthFlag) {
		this.azimuthFlag = azimuthFlag;
	}

	public boolean getAndClearSelectedFlag() {
		boolean returnValue = selectedFlag;
		selectedFlag = false;
		return returnValue;
	}

	public void setSelectedFlag(boolean selectedFlag) {
		this.selectedFlag = selectedFlag;
	}

	public Entity(float posX, float posY) {
		initEntity();		
		setX(posX);
		setY(posY);
	}

	public Entity() {
		initEntity();
	}
	
	private void initEntity() {
		setSelected(false);
		setX(0.0f);
		setY(0.0f);
		azimuth = 0.0f;
	}
	
	public abstract void draw(Canvas canvas, float inverseScaling, float counterRotation);
}
