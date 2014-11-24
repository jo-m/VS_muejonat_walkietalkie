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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	WifiP2pManager mManager;
	Channel mChannel;
	BroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	TextView mTextView;
	WifiP2pDnsSdServiceInfo mServiceInfo;
	WifiP2pDnsSdServiceRequest mServiceRequest;
	
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
        record.put("mac", macAddr);
        record.put("name", "name=" + macAddr);

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
	    registerReceiver(mReceiver, mIntentFilter);
	    mManager.addLocalService(mChannel, mServiceInfo, null);
	    
	    mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
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
		String name;
		String mac;
		String ip;
		int serverPort;
		boolean rightService = false;
	}
	
	final HashMap<String, Buddy> buddies = new HashMap<String, Buddy>();

	private void setupServiceListeners() {
	    DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
			@Override
			public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
	            Buddy b;
				if(buddies.containsKey(device.deviceAddress)) {
					b = buddies.get(device.deviceAddress);
	            } else {
	            	b = new Buddy();
	            }
	            
	            b.name = record.get("name");
	            b.mac = record.get("mac");
	            b.serverPort = Integer.parseInt(record.get("listenport"));
	            b.ip = device.toString();
				
				if(!buddies.containsKey(device.deviceAddress)) {
					buddies.put(device.deviceAddress, b);
	            }
			}
	    };
	    
	    DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	        @Override
	        public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                WifiP2pDevice device) {
                Buddy b;
				if(buddies.containsKey(device.deviceAddress)) {
					b = buddies.get(device.deviceAddress);
	            } else {
	            	b = new Buddy();
	            }
			
    			if(registrationType.startsWith(SERVICE_NAME)) {
	                Toast.makeText(MainActivity.this, "discovered "+b.mac, Toast.LENGTH_LONG).show();
    			}
    		
    			if(!buddies.containsKey(device.deviceAddress)) {
					buddies.put(device.deviceAddress, b);
	            }	
	        }
	    };

	    mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
	}
}
