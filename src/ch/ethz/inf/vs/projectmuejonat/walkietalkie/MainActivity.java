package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends WifiActivity implements OnTouchListener  {
	
	private ImageButton mSpeakButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Toast.makeText(this, "yow", Toast.LENGTH_SHORT).show();
		mSpeakButton = (ImageButton) findViewById(R.id.speakButton);
		mSpeakButton.setOnTouchListener(this);
	}

	@Override
	protected void showData(byte[] data) {
		Toast.makeText(this, "showData = " + data.length, Toast.LENGTH_SHORT).show();
	}
	
	private void startRecording() {
		Toast.makeText(this, "startRecording", Toast.LENGTH_SHORT).show();
	}
	
	private void stopRecording() {
		Toast.makeText(this, "stopRecording", Toast.LENGTH_SHORT).show();
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
}
