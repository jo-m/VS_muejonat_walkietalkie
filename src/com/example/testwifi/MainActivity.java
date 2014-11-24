package com.example.testwifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	WifiP2pManager mManager;
	Channel mChannel;
	WiFiDirectBroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	TextView mTextView;
	DiscoveryAsyncTask mDiscoveryTask;
	
	public TextView getTextView() {
		return mTextView;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(this, getMainLooper(), null);
	    mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
	    mTextView = (TextView) findViewById(R.id.textView);
	    
	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	}
	
	/* register the broadcast receiver with the intent values to be matched */
	@Override
	protected void onResume() {
	    super.onResume();
	    registerReceiver(mReceiver, mIntentFilter);
	    mDiscoveryTask = new DiscoveryAsyncTask();
	    mDiscoveryTask.execute();
	}
	/* unregister the broadcast receiver */
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	    mDiscoveryTask.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private class DiscoveryAsyncTask extends AsyncTask<Void, Void, Void> {
		private boolean stop = false;
		
		public void stop() {
			stop = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			while(!stop) {
				mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			        @Override
			        public void onSuccess() {}

			        @Override
			        public void onFailure(int reasonCode) {}
			    });
				
				Log.d("WIFI_P2P_VS", "SEARCH FROM ASYNC");
				
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_getstatus) {
			mTextView.setText(mReceiver.getStatus());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
