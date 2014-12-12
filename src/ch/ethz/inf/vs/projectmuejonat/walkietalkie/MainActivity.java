package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends WifiActivity implements OnTouchListener  {
	
	private ImageButton mSpeakButton;

	private void setupDebugButtons() {
		Button debugButton = (Button) findViewById(R.id.debugButton);
		debugButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.viewSwitcher);
				vs.showNext();
			}
		});
		
		debugButton = (Button) findViewById(R.id.debugButton2);
		debugButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.viewSwitcher);
				vs.showNext();
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSpeakButton = (ImageButton) findViewById(R.id.speakButton);
		mSpeakButton.setOnTouchListener(this);
		
		setupDebugButtons();
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
}
