package com.example.testwifi;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	WifiP2pManager mManager;
	Channel mChannel;
	BroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	TextView mTextView;
	WifiP2pDnsSdServiceInfo mServiceInfo;
	WifiP2pDnsSdServiceRequest mServiceRequest;
	DiscoveryAsyncTask mDiscoveryTask;
	boolean mWeAreGroupOwner = false;
	boolean mConnected = false;
	
	private final String LOGTAG = "WIFI_P2P_VS2";
	private final String SERVICE_NAME = "_walkietalkie._tcp";
	
	private final int SERVER_PORT = 42634;
	
	public TextView getTextView() {
		return mTextView;
	}
	
	private void setupService() {
		WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
       	WifiInfo wifiInf = wifiMan.getConnectionInfo();
       	String macAddr = wifiInf.getMacAddress();
       	
		//  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("device_name", Helpers.getDeviceName());

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        mServiceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(macAddr, SERVICE_NAME, record);
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
	    
	    setupService();
	    setupServiceListeners();
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    
	    mWeAreGroupOwner = false;
		mConnected = false;
	    
	    registerReceiver(mReceiver, mIntentFilter);
	    mManager.addLocalService(mChannel, mServiceInfo, null);
	    
	    mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        
        mDiscoveryTask = new DiscoveryAsyncTask();
	    mDiscoveryTask.execute();
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	    mDiscoveryTask.stop();
	    mManager.removeLocalService(mChannel, mServiceInfo, null);
	    mManager.removeServiceRequest(mChannel, mServiceRequest, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			mManager.discoverServices(mChannel, null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class Buddy {
		WifiP2pDevice device;
		String deviceName;
		int serverPort = -1;
		boolean rightService = false;
		
		public String toString() {
			String statusText = "<null>";
			switch(device.status) {
			case WifiP2pDevice.AVAILABLE:
				statusText = "available";
				break;
			case WifiP2pDevice.CONNECTED:
				statusText = "connected";
				break;
			case WifiP2pDevice.FAILED:
				statusText = "failed";
				break;
			case WifiP2pDevice.INVITED:
				statusText = "invited";
				break;
			case WifiP2pDevice.UNAVAILABLE:
				statusText = "unavailable";
				break;
			}
			
			return "{<Buddy> device=<<<"+device.toString()+
					">>>, serverPort="+serverPort+
					", rightService="+rightService+
					", status="+statusText+"("+device.status+")"+
					", grpOwner="+device.isGroupOwner()+
					", groupOwner="+device.isGroupOwner()+
					", deviceName="+deviceName+"}";
		}
	}
	
	final HashMap<String, Buddy> buddies = new HashMap<String, Buddy>();

	private void setupServiceListeners() {
	    DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
			@Override
			public void onDnsSdTxtRecordAvailable(String domain, Map<String, String> record, WifiP2pDevice device) {
				Buddy b;
				if(buddies.containsKey(device.deviceAddress)) {
					b = buddies.get(device.deviceAddress);
	            } else {
	            	b = new Buddy();
	            }
	            
				b.device = device;
				b.deviceName = record.get("device_name");
				
				try {
					b.serverPort = Integer.parseInt(record.get("listenport"));
				} catch(Exception e) {}
				
				if(!buddies.containsKey(device.deviceAddress)) {
					buddies.put(device.deviceAddress, b);
	            }
			}
	    };
	    
	    DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	        @Override
	        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
	            Buddy b;
				if(buddies.containsKey(device.deviceAddress)) {
					b = buddies.get(device.deviceAddress);
	            } else {
	            	b = new Buddy();
	            }
			
				b.device = device;
				
    			if(registrationType.startsWith(SERVICE_NAME)) {
    				b.rightService = true;
    			}
    			
    			if(!buddies.containsKey(device.deviceAddress)) {
					buddies.put(device.deviceAddress, b);
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
				
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {}
				
				publishProgress();
			}
			return null;
		}
		
		protected void onProgressUpdate(Void... progress) {
			StringBuilder s = new StringBuilder();
			for(Buddy b: buddies.values()) {
				s.append(b.toString());
				s.append('\n');
			}
			mTextView.setText(s.toString());
	    }
	}
}
