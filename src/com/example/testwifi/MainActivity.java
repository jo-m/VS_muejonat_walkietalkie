package com.example.testwifi;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.example.testwifi.ConnectionState.Buddy;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private TextView mTextView;
	private TextView mStatTextView;
	private WifiP2pDnsSdServiceInfo mServiceInfo;
	private WifiP2pDnsSdServiceRequest mServiceRequest;
	private DiscoveryAsyncTask mDiscoveryTask;
	private StatusDisplayAsyncTask mStatusUpdateTask;
	private ConnectionAsyncTask mConnectionTask;
	
	private final String LOGTAG = "WIFI_P2P_VS";
	private final String SERVICE_NAME = "_walkietalkie._tcp";
	
	private final int SERVER_PORT = 42634;
	private TextView mConnStatTextView;
	
	private String getMacAddress() {
		WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
       	WifiInfo wifiInf = wifiMan.getConnectionInfo();
       	return wifiInf.getMacAddress();
	}
	
	private void setupService() {
		//  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("device_name", Helpers.getDeviceName());

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        mServiceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(getMacAddress(), SERVICE_NAME, record);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(this, getMainLooper(), null);
	    mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
	    mTextView = (TextView) findViewById(R.id.textView);
	    mStatTextView = (TextView) findViewById(R.id.statTextView);
	    mConnStatTextView = (TextView) findViewById(R.id.connStatTextView);
	    
	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    
	    setupService();
	    setupServiceListeners();
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    
	    state = new ConnectionState(getMacAddress());
	    
	    registerReceiver(mReceiver, mIntentFilter);
	    mManager.addLocalService(mChannel, mServiceInfo, null);
	    
	    mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        
        mDiscoveryTask = new DiscoveryAsyncTask();
        mStatusUpdateTask = new StatusDisplayAsyncTask();
        mConnectionTask = new ConnectionAsyncTask();
        // execute tasks in parallel
	    mDiscoveryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	    mStatusUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	    mConnectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	    mDiscoveryTask.stop();
	    mStatusUpdateTask.stop();
	    mConnectionTask.stop();
	    mManager.removeLocalService(mChannel, mServiceInfo, null);
	    mManager.removeServiceRequest(mChannel, mServiceRequest, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_conn) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private ConnectionState state;
	
	public ConnectionState getState() {
		return state;
	}

	private void setupServiceListeners() {
	    DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
			@Override
			public void onDnsSdTxtRecordAvailable(String domain, Map<String, String> record, WifiP2pDevice device) {
				state.updateStatus(device, record.get("device_name"));

				try {
					state.updateStatus(device,
							Integer.parseInt(record.get("listenport")));
				} catch(Exception e) {}
			}
	    };
	    
	    DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	        @Override
	        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
	        	Buddy b = state.getBuddy(device.deviceAddress);
			
				b.device = device;
				
    			if(registrationType.startsWith(SERVICE_NAME)) {
    				b.rightService = true;
    			}
	        }
	    };

	    mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
	}
	
	private class DiscoveryAsyncTask extends AsyncTask<Void, Void, Void> {
		private boolean stop = false;
		
		public void stop() {
			stop = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			while(!stop) {
				mManager.discoverServices(mChannel, null);
				mManager.requestConnectionInfo(mChannel, null);
				
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {}
			}
			return null;
		}
	}
	
	private class StatusDisplayAsyncTask extends AsyncTask<Void, Void, Void> {
		private boolean stop = false;
		
		public void stop() {
			stop = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			while(!stop) {
				publishProgress();	
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {}
			}
			return null;
		}
		
		protected void onProgressUpdate(Void... progress) {
			mTextView.setText(state.buddiesToString());
			mStatTextView.setText(state.toString());
	    }
	}
	
	private class ConnectionAsyncTask extends AsyncTask<Void, Void, Void> {
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
		
		protected void onProgressUpdate(Void... progress) {
			manageConnection();
	    }
	}
	
	private void manageConnection() {
		StringBuilder connState = new StringBuilder();

		if(state.connected()) {
			connState.append("CONNECTED\n");
			// search for buddies which have to be invited
		} else {
			connState.append("NOT CONNECTED\n");
			if(state.haveGroupOwner()) {
				connState.append("E GROUP OWNER, WAIT 10 sec\n");
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {}
			} else {
				connState.append("NO GROUP OWNER\n");
				// try to connect to the first buddy
				Buddy b = state.findBuddyToConnect();
				
				if(b != null) {
					connState.append("CONNECT TO: " + b + "\n");
				} else {
					connState.append("NO BUDDY TO CONNECT TO\n");
				}
			}
		}
		
		mConnStatTextView.setText(connState.toString());
		
	}
}
