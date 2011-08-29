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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.util.ByteArrayBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.tlabs.ssr.g1.client.AudioScene.TransportState;

import android.util.Log;

/**
 * A thread constantly reading and parsing a socket xml input stream.
 * 
 * @author Peter Bartz
 */
public class XmlInputThread extends Thread {
	private static final String TAG = "XmlInputThread";
	
	public Boolean abortFlag;

	public XmlInputThread() {
		abortFlag = false;
	}

	@Override
	public void run() {
		Log.d(TAG, "(" + this.getId() + ") HELLO");

		try {
			// create sax parser
			SAXParserFactory spf = SAXParserFactory.newInstance(); 
			SAXParser sp = spf.newSAXParser(); 

			// get an xml reader 
			XMLReader xr = sp.getXMLReader();

			// set up xml input source
			XMLChunkInputStream xmlChunkInputStream;
			synchronized (GlobalData.socketChannel) {
				xmlChunkInputStream = new XMLChunkInputStream(new ByteArrayBuffer(32 * 1024), 
						GlobalData.socketChannel.socket().getInputStream());
			}
			InputSource inputSource = new InputSource(xmlChunkInputStream);
			
			// create handler for scene description and assign it
			SceneDescrXMLHandler sceneDescrXMLHandler = new SceneDescrXMLHandler(GlobalData.audioScene); 
			xr.setContentHandler(sceneDescrXMLHandler);
			
			// parse scene description
			//Log.d(TAG, "(" + this.getId() + ") parsing description...");
			//xmlChunkInputStream.printToLog = true;
			while (xmlChunkInputStream.bufferNextChunk() && !sceneDescrXMLHandler.receivedSceneDescr()) {
				// parse and process xml input
				xr.parse(inputSource);
			}
			//xmlChunkInputStream.printToLog = false;
			
			// signal that scene description was parsed
			//Log.d(TAG, "(" + this.getId() + ") sending SCENEPARSED_OK_MSG");
			GlobalData.sourcesMoverMsgHandler.sendMessage(GlobalData.sourcesMoverMsgHandler.obtainMessage(SourcesMover.SCENEPARSED_OK_MSG));
			
			// create handler for scene updates and assign it
			SceneUpdateXMLHandler sceneUpdateXMLHandler = new SceneUpdateXMLHandler(GlobalData.audioScene); 
			xr.setContentHandler(sceneUpdateXMLHandler);
			
			// parse scene updates
			Log.d(TAG, "(" + this.getId() + ") starting xml input loop...");
			while (xmlChunkInputStream.bufferNextChunk() ) {
				// parse and process xml input
				xr.parse(inputSource);
				
				// check if we should abort
				synchronized (abortFlag) {
					if (abortFlag == true) {
						break;
					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "(" + this.getId() + ") Exception " + e.toString() + ": " + e.getMessage());
			
			// check if this thread was aborted and/or stopped by a call to interrupt()
			if (Thread.interrupted() || abortFlag) {
				Log.d(TAG, "(" + this.getId() + ") interrupted/aborted");
			} else {
				GlobalData.sourcesMoverMsgHandler.sendMessage(GlobalData.sourcesMoverMsgHandler.obtainMessage(SourcesMover.XMLINPUT_ERR_MSG, e.getMessage()));
				Log.d(TAG, "(" + this.getId() + ") sending XMLINPUT_ERR_MSG");
			}
		}

		Log.d(TAG, "(" + this.getId() + ") GOOD BYE");
	}
	
	/**
	 * Base class for {@link SceneDescrXMLHandler} and {@link SceneUpdateXMLHandler}.
	 * 
	 * @author Peter Bartz
	 */
	private class SceneXMLHandler extends DefaultHandler {
		protected static final String ID = "id";
		protected static final String NAME = "name";
		protected static final String MODEL = "model";
		protected static final String X = "x";
		protected static final String Y = "y";
		protected static final String POSITION = "position";
		protected static final String SOURCE = "source";
		protected static final String REFERENCE = "reference";
		protected static final String LOUDSPEAKER = "loudspeaker";
		protected static final String UPDATE = "update";
		protected static final String MUTE = "mute";
		protected static final String VOLUME = "volume";
		protected static final String LEVEL = "level";
		protected static final String STATE = "state";
		protected static final String SCENE = "scene";
		protected static final String TRANSPORT = "transport";
		protected static final String START = "start";
		protected static final String STOP = "stop";
		protected static final String AZIMUTH = "azimuth";
		protected static final String ORIENTATION = "orientation";
		protected static final String TRUE = "true";
		protected static final String FALSE = "false";
		protected static final String FIXED = "fixed";
		
		protected AudioScene audioScene;
		
		protected SoundSource soundSource;
		protected Loudspeaker loudspeaker;
		protected boolean inUpdateTag;
		protected boolean inSourceTag;
		protected boolean inLoudspeakerTag;
		protected boolean inReferenceTag;
		protected boolean inVolumeTag;
		protected boolean inTransportTag;
		
		public SceneXMLHandler(AudioScene audioScene) {
			this.audioScene = audioScene;
		}
		
		protected void setSoundSourceAttributes(SoundSource soundSource, Attributes attributes) {
			// get attributes
			String sName = attributes.getValue(NAME);
			String sModel = attributes.getValue(MODEL);
			String sMuted = attributes.getValue(MUTE);
			String sVolume = attributes.getValue(VOLUME);
			String sLevel = attributes.getValue(LEVEL);
			
			// set attributes in soundSource
			if (sName != null) soundSource.setName(sName);
			try {
				if (sModel != null) 
					soundSource.setSourceModel(SoundSource.SourceModel.valueOf(sModel.toUpperCase()));
			} catch (Exception e) {} // string -> enum conversion exception
			if (sMuted != null) {
				if (sMuted.equals(TRUE)) {
					soundSource.setMuted(true);
				} else {
					soundSource.setMuted(false);
				}
			}
			try {
				soundSource.setVolume(Float.parseFloat(sVolume));
			} catch (Exception e) {} // string -> float conversion exception
			if (sLevel != null) {
				try {
					soundSource.setLevel(Float.parseFloat(sLevel));
				} catch (Exception e) {} // string -> float conversion exception
			}
		}
		
		protected void setLoudspeakerAttributes(Loudspeaker loudspeaker, Attributes attributes) {
			// get attributes
			String lModel = attributes.getValue(MODEL);
			
			// set attributes in loudspeaker
			try {
				if (lModel != null) 
					loudspeaker.setSpeakerModel(Loudspeaker.SpeakerModel.valueOf(lModel.toUpperCase()));
			} catch (Exception e) {} // string -> enum conversion exception
		}
		
		protected void setStateAttributes(Attributes attributes) {
			// get attributes
			String transport = attributes.getValue(TRANSPORT);
			
			// set state attributes
			if (transport != null) {
				setTransportState(transport);
			}
		}
		
		protected void setTransportState(String state) {
			Log.d(TAG, "Setting Transport State = " + state);
			if (state.equals(START)) {
				audioScene.setTransportState(TransportState.PLAYING);				
			} else if (state.equals(STOP)) {
				audioScene.setTransportState(TransportState.PAUSED);
			} else {
				Log.d(TAG, "Received unknown transport state: " + state);
			}
		}
		
		protected void setSceneAttributes(Attributes attributes) {
			// get attributes
			String volume = attributes.getValue(VOLUME);
			
			// set attributes audio scene
			if (volume != null) {
				setSceneVolume(volume);
			}
		}
		
		protected void setSceneVolume(String volume) {
			try {
				float newVolume;
				newVolume = Float.parseFloat(volume);
				audioScene.setVolume(newVolume);
			} catch (Exception e) {} // string -> float conversion exception
		}
		
		protected void setEntityPosition(Entity entity, Attributes attributes) {
			// get attributes
			String pX = attributes.getValue(X);
			String pY = attributes.getValue(Y);
			String pFixed = attributes.getValue(FIXED);
			
			// set position of entity
			if (pX != null && pY != null) {
				try {
					float x, y;
					x = Float.parseFloat(pX);
					y = Float.parseFloat(pY);
					entity.setXY(x, y);
				} catch (Exception e) {} // string -> float conversion exception
			}
			
			// fixed position?
			if (pFixed != null) {
				if (pFixed.equals(TRUE)) {
					entity.setPositionFixed(true);
				} else {
					entity.setPositionFixed(false);
				}
			}
		}
		
		protected void setEntityOrientation(Entity entity, Attributes attributes) {
			// get attributes
			String azimuth = attributes.getValue(AZIMUTH);
			
			// set orientation of entity
			try {
				entity.setAzimuth(Float.parseFloat(azimuth));
			} catch (Exception e) {} // string -> float conversion exception
		}
	}
	
	/**
	 * XML handler to parse initial scene description and construct an {@link AudioScene}
	 * 
	 * @author Peter Bartz
	 */
	private class SceneDescrXMLHandler extends SceneXMLHandler {
		private boolean receivedSceneDescr;
		private boolean parsingRootTag;
		private boolean parsingScene;
		private int idCounter = 100;

		public SceneDescrXMLHandler(AudioScene audioScene) {
			super(audioScene);
			this.receivedSceneDescr = false;
		}
		
		public boolean receivedSceneDescr() {
			return receivedSceneDescr;
		}

		@Override
		public void startDocument() throws SAXException {
			parsingRootTag = true;
			parsingScene = false;
			inUpdateTag = false;
			inSourceTag = false;
			inReferenceTag = false;
			soundSource = null;
		}
		
		@Override
		public void endDocument() throws SAXException {
			if (parsingScene) 
				receivedSceneDescr = true;
		}
		
		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			if (parsingRootTag) { 
				parsingRootTag = false;
				if (localName.equals(UPDATE)) { 
					parsingScene = true;
					Log.d(TAG, "creating new audio scene");
				} else {
					parsingScene = false;
					Log.d(TAG, "outer tag is not an update tag (" + localName + ")");
				}
				return;
			} else if (parsingScene) {
				if (inSourceTag) {
					if (localName.equals(POSITION)) {
						// no synchronization needed, because soundSource not added to audioScene yet
						setEntityPosition(soundSource, attributes);
						return;
					} else if (localName.equals(ORIENTATION)) {
						// no synchronization needed, because soundSource not added to audioScene yet
						setEntityOrientation(soundSource, attributes);
						return;
					}
				} else if (inLoudspeakerTag) { 
					if (localName.equals(POSITION)) {
						// no synchronization needed, because loudspeaker not added to audioScene yet
						setEntityPosition(loudspeaker, attributes);
						return;
					} else if (localName.equals(ORIENTATION)) {
						// no synchronization needed, because loudspeaker not added to audioScene yet
						setEntityOrientation(loudspeaker, attributes); 
						return;
					}
				} else if (inReferenceTag) { 
					if (localName.equals(POSITION)) {
						synchronized (audioScene) {
							setEntityPosition(audioScene.getReference(), attributes);
						}
						return;
					} else if (localName.equals(ORIENTATION)) {
						synchronized (audioScene) {
							setEntityOrientation(audioScene.getReference(), attributes);
						}
						return;
					}
				} else if (localName.equals(SOURCE)) { 
					inSourceTag = true;
					String sId = attributes.getValue(ID);
					if (sId == null) 
						sId = String.valueOf(idCounter++);
					soundSource = new SoundSource(sId);
					// no synchronization needed, because soundSource not added to audioScene yet
					setSoundSourceAttributes(soundSource, attributes);
					return;
				} else if (localName.equals(LOUDSPEAKER)) { 
					inLoudspeakerTag = true;
					loudspeaker = new Loudspeaker();
					// no synchronization needed, because loudspeaker not added to audioScene yet
					setLoudspeakerAttributes(loudspeaker, attributes);
					return;
				} else if (localName.equals(REFERENCE)) {
					inReferenceTag = true;
					// any direct reference attribs?
					synchronized (audioScene) {
						audioScene.setReference(new Reference());
					}
					return;
				} else if (localName.equals(VOLUME)) {
					inVolumeTag = true;
					return;
				} else if (localName.equals(TRANSPORT)) {
					inTransportTag = true;
					return;
				}
			}
			
			Log.d(TAG, "start unhandled element: '" + localName + "'");
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (parsingScene) {
				if (inVolumeTag) {
					synchronized (audioScene) {
						setSceneVolume(String.valueOf(ch, start, length));
					}
				} else if (inTransportTag) {
					synchronized (audioScene) {
						setTransportState(String.valueOf(ch, start, length));
					}
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
		throws SAXException {
			if (!parsingScene) return;
			
			if (localName.equals(SOURCE)) {
				synchronized (audioScene) {
					audioScene.addSoundSource(soundSource);
				}
				soundSource = null;
				inSourceTag = false;
				return;
			} else if (localName.equals(LOUDSPEAKER)) {
				synchronized (audioScene) {
					audioScene.addLoudspeaker(loudspeaker);
				}
				inLoudspeakerTag = false;
				loudspeaker = null;
				return;
			} else if (localName.equals(REFERENCE)) {
				inReferenceTag = false;
				return;
			} else if (localName.equals(VOLUME)) {
				inVolumeTag = false;
				return;
			} else if (localName.equals(TRANSPORT)) {
				inTransportTag = false;
				return;
			}
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			Log.d(TAG, "error");
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			Log.d(TAG, "fatal error");
		}
		
		@Override
		public void warning(SAXParseException e) throws SAXException {
			Log.d(TAG, "warning");
		}
	}
	
	/**
	 * XML handler to parse scene updates received from SSR server and save updates to {@link AudioScene}.
	 * 
	 * @author Peter Bartz
	 */
	private class SceneUpdateXMLHandler extends SceneXMLHandler {
		
		public SceneUpdateXMLHandler(AudioScene audioScene) {
			super(audioScene);
		}
		
		@Override
		public void startDocument() throws SAXException {
			inUpdateTag = false;
			inSourceTag = false;
			inReferenceTag = false;
			inVolumeTag = false;
			soundSource = null;
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			if (inUpdateTag) {
				if (inSourceTag) {
					if (localName.equals(POSITION)) {
						synchronized (audioScene) {
							setEntityPosition(soundSource, attributes);
						}
						return;
					} else if (localName.equals(ORIENTATION)) {
						synchronized (audioScene) {
							setEntityOrientation(soundSource, attributes);
						}
						return;
					}
				} else if (inReferenceTag) {
					if (localName.equals(POSITION)) {
						synchronized (audioScene) {
							setEntityPosition(audioScene.getReference(), attributes);
						}
						return;
					} else if (localName.equals(ORIENTATION)) {
						synchronized (audioScene) {
							setEntityOrientation(audioScene.getReference(), attributes);
						}
						return;
					}
				} else if (localName.equals(SOURCE)) {
					inSourceTag = true;
					synchronized (audioScene) {
						soundSource = audioScene.getSoundSource(attributes.getValue(ID));
						if (soundSource != null)
							setSoundSourceAttributes(soundSource, attributes);
					}
					return;
				} else if (localName.equals(REFERENCE)) {
					inReferenceTag = true;
					// any direct reference attribs? 
					return;
				} else if (localName.equals(SCENE)) {
					synchronized (audioScene) {
						setSceneAttributes(attributes);
					}
					return;
				} else if (localName.equals(STATE)) {
					Log.d(TAG, "received STATE");
					synchronized (audioScene) {
						setStateAttributes(attributes);
					}
					return;
				}
			} else if (localName.equals(UPDATE)) {
				inUpdateTag = true;
				return;
			}
			
			Log.d(TAG, "start unhandled element: '" + localName + "'");
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			// nothing
		}

		@Override
		public void endElement(String uri, String localName, String name)
		throws SAXException {
			if (localName.equals(SOURCE)) {
				inSourceTag = false;
				soundSource = null;
				return;
			} else if (localName.equals(UPDATE)) {
				inUpdateTag = false;
				return;
			} else if (localName.equals(REFERENCE)) {
				inReferenceTag = false;
				return;
			}
		}
		
		@Override
		public void error(SAXParseException e) throws SAXException {
			Log.d(TAG, "error");
		}
		
		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			Log.d(TAG, "fatal error");
		}
		
		@Override
		public void warning(SAXParseException e) throws SAXException {
			Log.d(TAG, "warning");
		}
	}
}
