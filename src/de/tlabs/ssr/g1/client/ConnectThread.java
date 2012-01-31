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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;

import android.util.Log;

/**
 * Thread that tries to establish a socket connection to an SSR server.
 * 
 * @author Peter Bartz
 */
public class ConnectThread extends Thread {
	private static final String TAG = "ConnectThread";
	private String host;
	private int port;
	
	public ConnectThread(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void run() {
        try {
        	// first create unconnected socket, so it can be closed while connecting
       		GlobalData.socketChannel = SocketChannel.open();
       		GlobalData.socketChannel.connect(new InetSocketAddress(host, port));
        } catch (AsynchronousCloseException ace) {
        	// socket was closed (i.e. cancel button was hit)
        	Log.d(TAG, "async close exception");
        	return;
		} catch (IOException ioe) {
			// connecting failed
			GlobalData.connectorMsgHandler.sendMessage(GlobalData.connectorMsgHandler.obtainMessage(Connector.CONNECT_ERR_MSG, ioe.getMessage()));
			return;
		} catch (Exception e) {
			// connection failed
			GlobalData.connectorMsgHandler.sendMessage(GlobalData.connectorMsgHandler.obtainMessage(Connector.CONNECT_ERR_MSG, e.getMessage()));
			return;
		}
		
		// alrighty
		GlobalData.connectorMsgHandler.sendEmptyMessage(Connector.CONNECT_OK_MSG);
	}
}
