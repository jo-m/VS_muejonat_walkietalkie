package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends WifiActivity implements OnTouchListener  {
	
	private ImageButton mSpeakButton;
	private TextView mDisplay;
	private DisplayAsyncTask mDisplayTask;
	
	private static String mSendTempFile;
	private static String mPlayTempFile;
	
	private MediaRecorder mRecorder = null;
	private MediaPlayer   mPlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSpeakButton = (ImageButton) findViewById(R.id.speakButton);
		mSpeakButton.setOnTouchListener(this);
		
		mDisplay = (TextView) findViewById(R.id.screenText);
		
		mSendTempFile = getFilesDir() + "/send_temp.3gp";
		mPlayTempFile = getFilesDir() + "/play_temp.3gp";
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    
	    mDisplayTask = new DisplayAsyncTask();
	    mDisplayTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    
	    mDisplayTask.stop();
	    
	    if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
	}

	@Override
	protected void showData(byte[] data) {
		Toast.makeText(this, "showData = " + data.length, Toast.LENGTH_SHORT).show();
	}
	
	private void startRecording() {
		mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mSendTempFile);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
        	e.printStackTrace();
        }

        mRecorder.start();
	}
	
	private byte[] fileToBuf(String fname) {
		InputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(fname));
		} catch (FileNotFoundException e) {
			return null;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[100 * 1024];
		
		try {
			int len;
			while ((len = in.read(buffer)) != -1) {
			    bos.write(buffer, 0, len);
			}
		} catch (IOException e) {
			return null;
		}
		try {
			bos.close();
			in.close();
		} catch (IOException e) {}
		return bos.toByteArray();
	}
	
	private void stopRecording() {
		try {
			mRecorder.stop();
	        mRecorder.release();
	        mRecorder = null;
		} catch (java.lang.RuntimeException e) {
			e.printStackTrace();
			return;
		}
		
        byte[] data = fileToBuf(mSendTempFile);
        Toast.makeText(this, "RECORDED len="+new File(mSendTempFile).length(), Toast.LENGTH_SHORT).show();
        publishData(data);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(event.getAction()) {
	    case MotionEvent.ACTION_DOWN:
	    	startRecording();
	    	break;
	    case MotionEvent.ACTION_UP:
	    	stopRecording();
	    	break;
	    }
		return false;
	}
	
	private class DisplayAsyncTask extends AsyncTask<Void, Void, Void> {
		private boolean stop = false;
		
		public void stop() {
			stop = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			while(!stop) {
				publishProgress();
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {}
			}
			return null;
		}
		
		@Override
		public void onProgressUpdate(Void... values) {
			StringBuilder sb = new StringBuilder();
			ConnectionState state = MainActivity.this.getState();
			
			if(state.mConnected) {
				sb.append("CONNECTED\n");
			} else {
				sb.append("NOT CONNECTED\n");
			}
			
			if(state.mWeAreGroupOwner) {
				sb.append("GO\n");
			} else {
				sb.append("CL\n");
			}
			
			mDisplay.setText(sb.toString());
		}
	}
}
