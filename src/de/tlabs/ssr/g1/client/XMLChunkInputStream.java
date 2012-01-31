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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

import android.util.Log;

/**
 * Class to split continuous input stream into chunks. A split is done every
 * time a '\0' character is encountered. The split character is dropped before
 * passing the chunk.
 * 
 * @author Peter Bartz
 */
public class XMLChunkInputStream extends ByteArrayInputStream {
	private static final int TEMPBUFFERSIZE = 2048; //2 kbytes
	private static final String TAG = "XMLChunkInputStream";
	private InputStream inputStream;
	private ByteArrayBuffer arrayBuffer;
	private byte[] tempBuffer;		// for faster detecting of delimiters
	private int tempBufferStart;	// start index of buffer content
	private int tempBufferEnd;		// first unused index
	
	public boolean printToLog = false;
	
	public XMLChunkInputStream(ByteArrayBuffer arrayBuffer, InputStream inputStream) {
		super(arrayBuffer.buffer());
		this.arrayBuffer = arrayBuffer; 
		this.inputStream = inputStream;
		this.tempBuffer = new byte[TEMPBUFFERSIZE];
		this.tempBufferStart = 0;
		this.tempBufferEnd = 0;
	}
	
	public boolean bufferNextChunk() throws IOException {
		boolean readDelimiter = false;
		
		arrayBuffer.clear();

		// read input data until delimiter is encountered
		while (!readDelimiter) {
			// process data which is still in temp buffer
			if (tempBufferEnd - tempBufferStart != 0) {
				// find next '\0'
				for (int i = tempBufferStart; i < tempBufferEnd; i++) {
					if (tempBuffer[i] == '\0') {
						// copy chunk to arrayBuffer
						arrayBuffer.append(tempBuffer, tempBufferStart, i - tempBufferStart);
						
						// update tempBufferStart (and skip '\0')
						tempBufferStart = i + 1;
						
						// break loop
						readDelimiter = true;
						break;
					}
				}
				
				// was delimiter read?
				if (!readDelimiter) {
					// copy whole temp buffer to array buffer
					arrayBuffer.append(tempBuffer, tempBufferStart, tempBufferEnd - tempBufferStart);
					tempBufferStart = 0;
					tempBufferEnd = 0;
				}
			}
			
			// was delimiter read out of temp buffer?
			if (!readDelimiter) {
				// temp buffer is definitely empty now and delimiter was not read yet
				// so fill buffer again
				if (fillTempBuffer() == -1)
					return false;
			}
		}
		
		// set properties of this ByteArrayInputStream
		this.mark = 0;
		this.pos = 0;
		this.buf = arrayBuffer.buffer();
		this.count = arrayBuffer.length();
		
		// print to log
		if (printToLog) {
			for (int i = 0; i < count; i += 100) {
				Log.d(TAG, EncodingUtils.getAsciiString(buf, i, Math.min(100, count - i)));
			}
		}
		
		return true;
	}

	private int fillTempBuffer() throws IOException {
		int numBytes = inputStream.read(tempBuffer);
		if (numBytes != -1) {
			tempBufferStart = 0;
			tempBufferEnd = numBytes;
		}
		
		return numBytes;
	}
}