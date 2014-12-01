package com.example.testwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.widget.Toast;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements ConnectionInfoListener, PeerListListener {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private MainActivity mActivity;
    private final String LOGTAG = "WIFI_P2P_VS2";

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
    		MainActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
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
        	if (mManager == null) {
        		return;
        	}
        	
        	mActivity.getState().updateStatus(
        			(WifiP2pDeviceList)intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST));
        	
        	mManager.requestPeers(mChannel, this);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	if (mManager == null) {
        		return;
        	}
        	
        	NetworkInfo networkInfo = (NetworkInfo) intent
        			.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        	mManager.requestConnectionInfo(mChannel, this);
        	mActivity.getState().updateStatus(networkInfo);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	WifiP2pDevice device = (WifiP2pDevice) intent
        			.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        	mActivity.getState().updateStatus(device);
        }
    }

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		mActivity.getState().updateStatus(info);
	}
	
	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		mActivity.getState().updateStatus(peers);
	}
}
