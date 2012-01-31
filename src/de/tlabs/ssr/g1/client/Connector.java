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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity class that contains GUI to init a connection to an SSR server.
 * Receives status messages from {@link ConnectThread} class.
 * 
 * @author Peter Bartz
 */
public class Connector extends Activity {
	protected static final String TAG = "Connector";
	public static final int CONNECT_OK_MSG = 1;
	public static final int CONNECT_ERR_MSG = 2;
	private static final int REQUEST_MOVE = 0;
	
	private static final String PREFS_LAST_USED_SERVER_NAME = "lastUsedServerName";
	private static final int MAX_NUM_LAST_USED_SERVER_NAMES = 6;
	
	private EditText hostPortEdit;
	private RadioButton fixedListenerTrackingModeRadioBtn;
	private RadioButton fixedSourcesModeRadioBtn;
	private Button okButton;
	private Button cancelButton;
	private Button helpButton;
	private ListView predefinedServersListView;
	private ScrollView connectorScrollView;
	
	private SharedPreferences prefs;
	LinkedList<String> serverList = new LinkedList<String>();

	// message handler to receive messages from other threads
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECT_OK_MSG: // connecting was successful
				Log.d(TAG, "connect ok handler");
				
				// save this server to preferences
				String serverName = hostPortEdit.getText().toString();
				if (serverList.contains(serverName))
					serverList.remove(serverName);
				serverList.add(0, serverName);
				SharedPreferences.Editor prefsEditor = prefs.edit();
				for (int i = 0; i < Math.min(MAX_NUM_LAST_USED_SERVER_NAMES, serverList.size()); i++) {
					prefsEditor.putString(PREFS_LAST_USED_SERVER_NAME + i, serverList.get(i));
					Log.d(TAG, "wrote server name: " + serverList.get(i));
				}
				prefsEditor.commit();


				enableOkButton();
				synchronized (GlobalData.audioScene) {
					GlobalData.audioScene.setDrawSourcesFixedEnabled(fixedSourcesModeRadioBtn.isChecked());
				}
				GlobalData.orientationTrackingEnabled = fixedListenerTrackingModeRadioBtn.isChecked();
				
				// start mover activity, which will start an xml input thread
				startActivityForResult(new Intent(Connector.this, SourcesMover.class), REQUEST_MOVE);
				
				break;
			case CONNECT_ERR_MSG: // connecting failed
				Log.d(TAG, "connect err handler");
				
				enableOkButton();
				GlobalData.socketChannel = null;
				
				// show error message
				Toast.makeText(Connector.this, "Sorry, could not connect (" + (String) msg.obj + ").", Toast.LENGTH_LONG).show(); 
				
				break;
				
			default:
				super.handleMessage(msg);
        	}
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		// set content view
		setContentView(R.layout.connector);
		
		// publish message handler
		GlobalData.connectorMsgHandler = msgHandler;

		// get component references
		hostPortEdit = (EditText) findViewById(R.id.host_port_edit);
		fixedListenerTrackingModeRadioBtn = (RadioButton) findViewById(R.id.listenerfixedtracking_radiobtn);
		fixedSourcesModeRadioBtn = (RadioButton) findViewById(R.id.sourcesfixed_radiobtn);
		okButton = (Button) findViewById(R.id.ok_button);
		cancelButton = (Button) findViewById(R.id.cancel_button);
		helpButton = (Button) findViewById(R.id.connector_help_button);
		predefinedServersListView = (ListView) findViewById(R.id.predefinedservers_listview);
		connectorScrollView = (ScrollView) findViewById(R.id.connector_scrollview);
		
		// get preferences
		prefs = getPreferences(MODE_PRIVATE);
		
		// ok button click handler
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				URL url;
				
				// parse url
				try {
					// construct url with dummy protocol
					url = new URL("http://" + hostPortEdit.getText().toString());
				} catch (MalformedURLException mue) {
					// show message box
					Toast.makeText(Connector.this, "Please enter a valid address (" + mue.getMessage() + ").", Toast.LENGTH_LONG).show();
					return;
				}

				disableOkButton();
				
				// start thread to connect to server
				new ConnectThread(url.getHost(), url.getPort()).start();
			}
		});
		
		// cancel button click handler
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				try {
					synchronized (GlobalData.socketChannel) {
						GlobalData.socketChannel.close();
					}
				} catch (IOException e) {
					Log.d(TAG, "exception while closing: " + e.getMessage());
				}
				enableOkButton();
			}
		});
		
		// help button click handler
		helpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
//				DialogHelper.showOneButtonDialog(Connector.this, "Title", "Message", "Done");
				
//				Dialog dialog = new Dialog(Connector.this);
//				dialog.setContentView(R.layout.connector_help_dialog);
//				dialog.setTitle("Help");
//				dialog.show();
				
				// inflate help dialog view
				LayoutInflater inflater = (LayoutInflater) Connector.this.getSystemService(LAYOUT_INFLATER_SERVICE);
				View dialogView = inflater.inflate(R.layout.connector_help_dialog, (ViewGroup) findViewById(R.id.connector_help_dialog_layout));
				
				// create alert dialog and set view
				AlertDialog.Builder builder = new AlertDialog.Builder(Connector.this);
				builder.setTitle("Help").setPositiveButton("Ok", null);
				builder.setView(dialogView);
				
				// show dialog
				builder.show();
			}
		});
		
		// predefined servers list view click handler
		predefinedServersListView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				hostPortEdit.setText(((TextView)view).getText());
				connectorScrollView.scrollTo(0, 0);
			}
		});
		
		// little hack to get a scrollview inside a listview
		connectorScrollView.setOnTouchListener(new ScrollView.OnTouchListener() {
			private boolean resizedListView = false;
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!resizedListView) {
					// resize list to vertically wrap content (to overcome android listview-inside-scrollview bug).
					// this can not be done in onResume(), because the measuring does not work there yet.
					// this is a bit hacky. an alternative would be: use a linear layout instead of list view and
					// add custom items to layout ourselves.
					ListAdapter adapter = predefinedServersListView.getAdapter();
			        if (adapter == null) return false;
			        int desiredWidth = MeasureSpec.makeMeasureSpec(predefinedServersListView.getWidth(), MeasureSpec.AT_MOST);
			        int totalHeight = 0;
			        for (int i = 0; i < adapter.getCount(); i++) {
			            View listItem = adapter.getView(i, null, predefinedServersListView);
			            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
			            totalHeight += listItem.getMeasuredHeight();
			        }

			        ViewGroup.LayoutParams params = predefinedServersListView.getLayoutParams();
			        params.height = totalHeight + (predefinedServersListView.getDividerHeight() * (predefinedServersListView.getCount() - 1));
			        predefinedServersListView.setLayoutParams(params);
			        predefinedServersListView.requestLayout();
			        
					resizedListView = true;
				}
				return false;
			}
		});
		
		// host/port edit key down listener
		hostPortEdit.setOnKeyListener(new EditText.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// pressing <ENTER> is like clicking connect button
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					okButton.performClick();
					return true;
				}
				return false;
			}
		});
	}
	
	private void enableOkButton() {
		// enable ok button
		okButton.setEnabled(true);
		
		// disable cancel button
		cancelButton.setEnabled(false);
	}

	private void disableOkButton() {
		// disable ok button and set text
		okButton.setEnabled(false);
		
		// enable cancel button
		cancelButton.setEnabled(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult, result code: " + resultCode);
    	
		// user hit back-button from SourcesMover activity
    	if (requestCode == REQUEST_MOVE && resultCode == RESULT_CANCELED) {
    		// stop xml input thread
    		synchronized (GlobalData.xmlInputThread.abortFlag) {
    			GlobalData.xmlInputThread.abortFlag = true;
    		}
    		GlobalData.xmlInputThread.interrupt();
    		GlobalData.xmlInputThread = null;
    		
    		// close socket channel
    		try {
    			synchronized (GlobalData.socketChannel) {
    				GlobalData.socketChannel.close();
    				GlobalData.socketChannel = null;
    			}
				
				// workaround: dummy create channel, else GlobalData.socketChannel is not closed
				SocketChannel.open();	
			} catch (IOException e) {
				Log.d(TAG, "io exception on socket channel close()");
			} catch (Exception e) {
				Log.d(TAG, "exception on socket channel close()");
			}
			
			// reset audio scene
			synchronized (GlobalData.audioScene) {
				GlobalData.audioScene.reset();
			}
    	} else 
    		Log.d(TAG, "onActivityResult was called with unknown request/result code");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		
		// populate predefined servers list
		String serverName;
		serverList.clear();
		for (int i = 0; i < MAX_NUM_LAST_USED_SERVER_NAMES; i++) {
			serverName = prefs.getString(PREFS_LAST_USED_SERVER_NAME + i, null);
			if (serverName != null) {
				Log.d(TAG, "found server name: " + serverName);
				if (!serverList.contains(serverName))	
					serverList.add(serverName);
			}
		}
		String[] hardServerList = getResources().getStringArray(R.array.predefinedservers_list);
		for (String s : hardServerList) {
			if (!serverList.contains(s))
				serverList.add(s);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		for (int i = 0; i < Math.min(MAX_NUM_LAST_USED_SERVER_NAMES, serverList.size()); i++) {
			adapter.add(serverList.get(i));
		}
		predefinedServersListView.setAdapter(adapter);
		
		// set default server
		if (serverList.size() > 0)
			hostPortEdit.setText(serverList.getFirst());

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG, "onRestoreInstanceState");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState");
	}
}
