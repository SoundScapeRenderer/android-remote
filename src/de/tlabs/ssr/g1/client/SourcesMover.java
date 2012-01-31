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

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import de.tlabs.ssr.g1.client.AudioScene.TransportState;
import de.tub.tlabs.android.utils.AppVersionInfo;

/*
 * TODO Better lifecycle management?
 */
/**
 * Main activity of the app. Contains sources display/manipulation view and 
 * settings screens/dialogs.
 * Manages periodical redraw and collects status messages from the network/xml thread.
 * 
 * @author Peter Bartz
 */
public class SourcesMover extends Activity implements SensorEventListener {
	private static final String TAG = "SourcesMover";
	
	// message ids
	public static final int XMLINPUT_ERR_MSG = 1;
	public static final int SCENEPARSED_OK_MSG = 2;
	public static final int OVERALLVOLUME_CHANGED_MSG = 3;
	private static final int TIMED_INVALIDATE_MSG = 4;
	
	private ImageButton zoomOutButton;
	private ImageButton zoomInButton;
	private ImageButton helpButton;
	private ImageButton exitButton;
	private ImageButton playButton;
	private ImageButton pauseButton;
	private ImageButton rewindButton;
	private TextView volumeTextView;
	private CustomSeekBar volumeSeekBar;
	private RelativeLayout transportLayout;
	private TextView helpTextView;
	private RelativeLayout helpLayout;
	private LinearLayout buttonsLayout;
	
	private SensorManager sensorManager;
	private boolean draw;
	private float initialAzimuth;
	private boolean initialAzimuthFlag;
	private float lastAzimuthValues[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	private int azimuthValuesIndex = 0;
	
	
	// message handler to receive messages from other threads
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
			switch (msg.what) {
			case TIMED_INVALIDATE_MSG: // draw next frame
				if (draw)
					this.sendEmptyMessageDelayed(TIMED_INVALIDATE_MSG, 40); // 25 fps
				GlobalData.sourcesMoverView.invalidate();
				synchronized (GlobalData.audioScene) {
					// update volume seekbar?
					if (GlobalData.audioScene.getAndClearVolumeFlag()) {
						float newVolume = GlobalData.audioScene.getVolume();
						// set seek bar
						volumeSeekBar.setVisualProgress(dbToSeekBar(newVolume));
						// show current value in text view
						volumeTextView.setText(Math.round(newVolume) + " dB");
					}
					
					// update transport state?
					if (GlobalData.audioScene.getAndClearTransportStateFlag()) {
						if (GlobalData.audioScene.getTransportState() == TransportState.PLAYING) {
							pauseButton.setVisibility(View.VISIBLE);
							playButton.setVisibility(View.GONE);
						} else if (GlobalData.audioScene.getTransportState() == TransportState.PAUSED) {
							playButton.setVisibility(View.VISIBLE);
							pauseButton.setVisibility(View.GONE);
						}
					}
				}
				
				break;
			case XMLINPUT_ERR_MSG: // an xml input error occurred
				Log.d(TAG, "xml input err handler");
				
				// show error message
				Toast.makeText(SourcesMover.this, "An error occured during XML parsing (" + (String) msg.obj + "). Please try to connect again.", Toast.LENGTH_LONG).show(); 
				finish(); // go back to connect screen
				
				break;
			case SCENEPARSED_OK_MSG: // the scene was successfully and completely parsed
				Log.d(TAG, "scene parsed ok handler");
				
				// scale view to fit entire scene
				synchronized (GlobalData.audioScene) {
					GlobalData.audioScene.recalculateReferenceTransformation();
				}
				GlobalData.sourcesMoverView.transformToFitScene();
				
				break;
			default:
				super.handleMessage(msg);
        	}
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // are we coming from a saved instance state? we would have to reconnect.
        if (savedInstanceState != null && savedInstanceState.getBoolean("fromSavedInstance", false)) {
        	// go back to connect screen
        	finish();
        	return;
        }

		// publish message handler
		GlobalData.sourcesMoverMsgHandler = msgHandler;
        
        // hide window title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // set content
        setContentView(R.layout.sources_mover);

        // get sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // get references to views
       	GlobalData.sourcesMoverView = (SourcesView) findViewById(R.id.sources_mover_view);
        zoomOutButton = (ImageButton) findViewById(R.id.zoomout_button);
        zoomInButton = (ImageButton) findViewById(R.id.zoomin_button);
        helpButton = (ImageButton) findViewById(R.id.help_button);
        exitButton = (ImageButton) findViewById(R.id.exit_button);
        playButton = (ImageButton) findViewById(R.id.play_button);
        pauseButton = (ImageButton) findViewById(R.id.pause_button);
        rewindButton = (ImageButton) findViewById(R.id.rewind_button);
        transportLayout = (RelativeLayout) findViewById(R.id.transport_layout);
        helpLayout = (RelativeLayout) findViewById(R.id.help_layout);
        helpTextView = (TextView) findViewById(R.id.help_textview);
        buttonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);
        volumeSeekBar = (CustomSeekBar) findViewById(R.id.volume_seekbar);
        volumeTextView = (TextView) findViewById(R.id.volume_textview);
        
        // show version info on help screen
		try {
			helpTextView.setText("\nVersion " + AppVersionInfo.getVersionName(this)
					+ ", built " + AppVersionInfo.getBuildDate(this) + " " + AppVersionInfo.getBuildTime(this)
					+ "\n" + helpTextView.getText());
		} catch (Exception e) {}
        
        // set ui actions
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GlobalData.sourcesMoverView.zoomView(1.0f/1.3f);
			}
		});
        
        zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GlobalData.sourcesMoverView.zoomView(1.3f);
			}
		});
        
        helpButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		helpLayout.setVisibility(View.VISIBLE);
        	}
        });
        
        helpTextView.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		// TODO way to distinguish link-clicks from non-link-clicks?
        		// helpLayout.setVisibility(View.INVISIBLE);
        	}
        });
        
        exitButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		// hide controls controls
        		transportLayout.setVisibility(View.INVISIBLE);
        		buttonsLayout.setVisibility(View.VISIBLE);
        	}
        });
        
        playButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		GlobalData.sourcesMoverView.sendToServer("<request><state transport='start'/></request>\0");
//        		playButton.setVisibility(View.GONE);
//        		pauseButton.setVisibility(View.VISIBLE);
        	}
        });
        
        pauseButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		GlobalData.sourcesMoverView.sendToServer("<request><state transport='stop'/></request>\0");
//        		playButton.setVisibility(View.VISIBLE);
//        		pauseButton.setVisibility(View.GONE);
        	}
        });
        
        rewindButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		GlobalData.sourcesMoverView.sendToServer("<request><state transport='rewind'/></request>\0");
        	}
        });
        
        transportLayout.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		exitButton.performClick();
        	}
        });
        
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
								
				// send volume change to server if change was user-initiated
				if (fromUser) {
					float newVolume = seekBarToDb(progress);
					// generate server request string
					String strMsg = "<request><scene volume='" + newVolume + "'/></request>\0";
					GlobalData.sourcesMoverView.sendToServer(strMsg);
					//volumeSeekBar.setProgress(volumeSeekBar.lastProgress);
				} else {
					//volumeSeekBar.lastProgress = progress;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// nothing
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// nothing
			}
        });
        
        // init volume
        volumeSeekBar.setProgress(dbToSeekBar(0));
        
        // set up stuff for drawing entities
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        SoundSource.setPaint(paint);

        paint = new Paint(paint);
        paint.setAntiAlias(true);
        Reference.setPaint(paint);

        paint = new Paint(paint);
        paint.setAntiAlias(true);
        Loudspeaker.setPaint(paint);

        paint = new Paint(paint);
        paint.setAntiAlias(true);
        paint.setARGB(255, 0, 0, 0);
        paint.setStrokeWidth(3);
        AudioScene.setPaint(paint);

        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(0, 0);
        SoundSource.paint.setARGB(255, 150, 150, 150);
        canvas.drawLine(0.0f, -30.0f, 0.0f,  30.0f, SoundSource.paint);
        canvas.drawLine(0.0f, -35.0f, 0.0f, -45.0f, SoundSource.paint);
        canvas.drawLine(0.0f,  35.0f, 0.0f,  45.0f, SoundSource.paint);
        canvas.drawLine(0.0f, -49.0f, 0.0f, -54.0f, SoundSource.paint);
        canvas.drawLine(0.0f,  49.0f, 0.0f,  54.0f, SoundSource.paint);
        canvas.drawPoint(0.0f,  58.0f, SoundSource.paint);
        canvas.drawPoint(0.0f,  62.0f, SoundSource.paint);
        canvas.drawPoint(0.0f, -58.0f, SoundSource.paint);
        canvas.drawPoint(0.0f, -62.0f, SoundSource.paint);
        canvas.drawLine(0.0f, -30.0f, 8.0f, -30.0f, SoundSource.paint);
        canvas.drawLine(8.0f, -30.0f, 4.0f, -27.5f, SoundSource.paint);
        canvas.drawLine(8.0f, -30.0f, 4.0f, -32.5f, SoundSource.paint);
        canvas.drawLine(0.0f, 30.0f, 8.0f, 30.0f, SoundSource.paint);
        canvas.drawLine(8.0f, 30.0f, 4.0f, 27.5f, SoundSource.paint);
        canvas.drawLine(8.0f, 30.0f, 4.0f, 32.5f, SoundSource.paint);
        picture.endRecording();
        SoundSource.planeWavePicture = picture;

        picture = new Picture();
        canvas = picture.beginRecording(0, 0);
        Reference.paint.setColor(getResources().getColor(R.drawable.col_listener));
        Reference.paint.setStrokeWidth(3.0f);
        canvas.drawLine(-12.0f, 0.0f, 13.0f,  0.0f, Reference.paint);
        Reference.paint.setStrokeWidth(2.0f);
        canvas.drawLine( 15.0f, 0.0f, -1.0f, -10.0f, Reference.paint);
        canvas.drawLine( 15.0f, 0.0f, -1.0f,  10.0f, Reference.paint);
        picture.endRecording();
        Reference.arrowPicture = picture;

        picture = new Picture();
        canvas = picture.beginRecording(0, 0);
        Loudspeaker.paint.setColor(getResources().getColor(R.drawable.col_loudspeaker));
        canvas.drawRect(-4.0f, -5.0f, 4.0f, 5.0f, Loudspeaker.paint);
        canvas.drawLine( 4.0f, 0.0f, 10.0f, 0.0f, Loudspeaker.paint);
        canvas.drawLine(10.0f, 0.0f,  6.0f, -2.5f, Loudspeaker.paint);
        canvas.drawLine(10.0f, 0.0f,  6.0f,  2.5f, Loudspeaker.paint);
        Loudspeaker.picture = picture;

        // start xml input thread
        GlobalData.xmlInputThread = new XmlInputThread();
        GlobalData.xmlInputThread.start();
	}
    
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (transportLayout.getVisibility() == View.INVISIBLE) {
			transportLayout.setVisibility(View.VISIBLE);
    		buttonsLayout.setVisibility(View.INVISIBLE);
		} else {
			transportLayout.setVisibility(View.INVISIBLE);
    		buttonsLayout.setVisibility(View.VISIBLE);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Log.d(TAG, "onActivityResult");
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "onDestroy");
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	// stop periodical drawing
    	draw = false;
    	
    	// stop sensing orientation
   		sensorManager.unregisterListener(this);
    	
    	Log.d(TAG, "onPause");
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	Log.d(TAG, "onRestart");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
		// start periodical invalidation every 40 ms (-> repainting)
    	draw = true;
    	msgHandler.sendEmptyMessageDelayed(TIMED_INVALIDATE_MSG, 40); // 25 fps

    	// start sensing orientation
    	if (GlobalData.orientationTrackingEnabled) {
    		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
    		sensorManager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
    		initialAzimuthFlag = true;	// "calibrate" to initial orientation
    	}
    	Log.d(TAG, "onResume");
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
		outState.putBoolean("fromSavedInstance", true); // signal that threads and audio scene are already set up
		Log.d(TAG, "onSaveInstanceState");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// catch "back"-button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (transportLayout.getVisibility() == View.VISIBLE) { // go back from transport controls?
				transportLayout.setVisibility(View.INVISIBLE);
				buttonsLayout.setVisibility(View.VISIBLE);
				return true;
			} else if (helpLayout.getVisibility() == View.VISIBLE) { // go back from help screen?
				helpLayout.setVisibility(View.INVISIBLE);
				return true;
			}
		}
		
		return super.onKeyDown(keyCode, event);
	}

	public float seekBarToDb(int progress) {
		return (float) progress / (float) volumeSeekBar.getMax() * 72.0f - 60.0f;
	}
	
	public int dbToSeekBar(float db) {
		return Math.round((db + 60.0f) / 72.0f * (float) volumeSeekBar.getMax()); 
	}
    
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if (initialAzimuthFlag) { // calibrate
				initialAzimuth = -(event.values[0] + 90.0f);
				for (int i = 1; i < lastAzimuthValues.length; i++) {
					lastAzimuthValues[i] = initialAzimuth;
				}
				initialAzimuthFlag = false;
			} else {
				lastAzimuthValues[azimuthValuesIndex++]= -event.values[0];	
				if (azimuthValuesIndex >= lastAzimuthValues.length)
					azimuthValuesIndex = 0;
				float azimuth = 0;
				for (int i = 1; i < lastAzimuthValues.length; i++) {
					float diff;
					diff = lastAzimuthValues[i] - lastAzimuthValues[0];
					
					if (diff > 180) 
						azimuth += diff - 360.0f;
					else if (diff < -180)
						azimuth += diff + 360.0f;
					else
						azimuth += diff;
				}
				azimuth = lastAzimuthValues[0] + azimuth / lastAzimuthValues.length - initialAzimuth;
				
				// generate server request string
				// TODO temporary
//				GlobalData.sourcesMoverView.setCurrentCenterRotation(-azimuth);
				String strMsg = "<request><reference><orientation azimuth='" + azimuth + "'/></reference></request>\0";
				GlobalData.sourcesMoverView.sendToServer(strMsg);
			}
		}
	}
}