package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSpeakButton = (ImageButton) findViewById(R.id.speakButton);
		mSpeakButton.setOnTouchListener(this);
		
		mDisplay = (TextView) findViewById(R.id.screenText);
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
	}

	@Override
	protected void showData(byte[] data) {
		Toast.makeText(this, "showData = " + data.length, Toast.LENGTH_SHORT).show();
	}
	
	private void startRecording() {
		
	}
	
	private void stopRecording() {
		publishData("blah".getBytes());
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
			
			mDisplay.setText(sb.toString());
		}
	}
}
