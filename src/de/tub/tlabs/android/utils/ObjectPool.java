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
 * A generic high performance object pooling implementation without size 
 * limit. Very useful to avoid pauses caused by the Garbage Collector.
 * Will only allocate memory if get() is called on empty pool.
 *
 * @param <T> Type of pooled objects
 * @author Peter Bartz
 */
public class ObjectPool<T> {
	// Simple linked list
	private class ListElement {
		public T item;
		public ListElement next;
		public ListElement prev;
	}
	private ListElement listHead = new ListElement();
	private ListElement listPosition = listHead;
	
	private ObjectFactory<T> factory;

	/**
	 * Creates an empty pool.
	 * 
	 * @param factory Factory to be used for creating new objects
	 */
	public ObjectPool(ObjectFactory<T> factory) {
		this.factory = factory;
	}
	
	/**
	 * Creates a pool of specified initial size. Choose sufficient
	 * size to avoid later allocations at all.
	 * 
	 * @param factory Factory to be used for creating new objects
	 * @param initialSize Initial number of objects in the pool
	 */
	public ObjectPool(ObjectFactory<T> factory, int initialSize) {
		this.factory = factory;
		for (int i = 0; i < initialSize; i++) {
			put(factory.newObject());
		}
	}
	
	/**
	 * Puts an object into the pool. Pool size is not limited.
	 * 
	 * @param item
	 */
	public synchronized void put(T item) {
		if (listPosition.next == null) {
			listPosition.next = new ListElement();
			listPosition.next.prev = listPosition;
		}
		listPosition = listPosition.next;
		listPosition.item = item;
	}
	
	/**
	 * Removes and returns an object from the pool. If there are no objects
	 * left, a new object will be created using the factory.
	 * 
	 * @return
	 */
	public synchronized T get() {
		T item;
		if (listPosition != listHead) {
			item = listPosition.item;
			listPosition = listPosition.prev;
		} else {
			item = factory.newObject();
		}
		
		return item;
	}
	
	/**
	 * Used to create objects for an object pool.
	 *
	 * @param <T> Type of pooled objects
	 */
	public static interface ObjectFactory<T> {
		public T newObject();
	}
	
	/**
	 * Object wrapper to enable pooling of primitive float values
	 */
	public static class Float {
		public float value;
	}
	/**
	 * Object wrapper to enable pooling of primitive double values
	 */
	public static class Double {
		public double value;
	}
	/**
	 * Object wrapper to enable pooling of primitive byte values
	 */
	public static class Byte {
		public byte value;
	}
	/**
	 * Object wrapper to enable pooling of primitive short values
	 */
	public static class Short {
		public short value;
	}
	/**
	 * Object wrapper to enable pooling of primitive int values
	 */
	public static class Int {
		public int value;
	}
	/**
	 * Object wrapper to enable pooling of primitive long values
	 */
	public static class Long {
		public long value;
	}
	/**
	 * Object wrapper to enable pooling of primitive boolean values
	 */
	public static class Boolean {
		public boolean value;
	}
	/**
	 * Object wrapper to enable pooling of primitive char values
	 */
	public static class Char {
		public char value;
	}
}