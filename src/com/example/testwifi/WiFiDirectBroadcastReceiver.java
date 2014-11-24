package com.example.testwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.widget.Toast;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements PeerListListener {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private MainActivity mActivity;
    private Context mContext;
    private final String LOGTAG = "WIFI P2P";

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
        	Log.d(LOGTAG, "SEARCH FOR PEERS");
            if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            	Toast.makeText(context, "You do not have WiFi P2P available!", Toast.LENGTH_LONG).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        	Toast.makeText(context, "discover peers", Toast.LENGTH_LONG).show();
        	
        	if (mManager != null) {
                mManager.requestPeers(mChannel, this);
                
                Log.d(LOGTAG, "SEARCH FOR PEERS");
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		Toast.makeText(mContext, "got peers", Toast.LENGTH_LONG).show();
		
		Log.d(LOGTAG, "GOT PEERS " + peers.getDeviceList().size());
		StringBuilder s = new StringBuilder();
		
		for(WifiP2pDevice d: peers.getDeviceList()) {
			Log.d(LOGTAG, "PEER" + d.toString());
			
			s.append("Peer" + d.deviceAddress + " " + d.deviceName + " " + d.primaryDeviceType + "\n\n");
		}
		mActivity.getTextView().setText(s.toString());
	}
}
