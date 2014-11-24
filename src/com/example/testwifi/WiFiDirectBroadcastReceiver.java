package com.example.testwifi;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.widget.Toast;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private MainActivity mActivity;
    private Context mContext;
    private final String LOGTAG = "WIFI_P2P_VS";

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
    		MainActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.mContext = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            	Toast.makeText(context, "You do not have WiFi P2P available!", Toast.LENGTH_LONG).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        	Log.d(LOGTAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
        	if (mManager != null) {
                mManager.requestPeers(mChannel, this);
                Log.d(LOGTAG, "requestPeers");
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	// we got a connection
        	if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                mManager.requestConnectionInfo(mChannel, this);
            } else {  	
                // It's a disconnect
            }
        	
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {		
		for(final WifiP2pDevice d: peers.getDeviceList()) {
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = d.deviceAddress;
			
			mManager.connect(mChannel, config, new ActionListener() {
			    @Override
			    public void onSuccess() {
			    	Log.d(LOGTAG, "success connect");
			    }

			    @Override
			    public void onFailure(int reason) {
			    	Log.d(LOGTAG, "fail connect");
			    }
			});
		}
	}
	
	// map of MAC address -> connection info
	private ArrayList<WifiP2pInfo> mConnections = new ArrayList<WifiP2pInfo>();
	
	private synchronized void addConnection(WifiP2pInfo info) {
		ArrayList<WifiP2pInfo> filtered = new ArrayList<WifiP2pInfo>();
		mConnections.add(info);
		for(int i = 0; i < mConnections.size(); i++) {
			boolean unique = true;
			for(int j = 0; j < mConnections.size(); j++) {
				if(j == i) {
					continue;
				}
				if(mConnections.get(i).equals(mConnections.get(j))) {
					unique = false;
					break;
				}
			}
			if(unique) {
				filtered.add(mConnections.get(i));
			}
		}
		mConnections = filtered;
		Log.d(LOGTAG, "FILTERED: NEW SIZE IS " + filtered.size());
	}
	
	public String getStatus() {
		StringBuilder s = new StringBuilder();
		for(WifiP2pInfo i: mConnections) {
			s.append(i.toString() + "\n");
		}
		return s.toString();
	}
	
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		Log.d(LOGTAG, "onConnectionInfoAvailable " + info.toString());
		if(info != null) {
			addConnection(info);
		}
	}
}
