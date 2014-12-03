package com.example.testwifi;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

public class ConnectionState {
	private final String LOGTAG = "WIFI_P2P_VS";
	private final String ourAddress;
	
	private WifiP2pDevice mGroupOwner;
	private InetAddress mGroupOwnerAddress;
	public boolean mWeAreGroupOwner = false;
	public boolean mConnected = false;
	
	private final HashMap<String, Buddy> buddies = new HashMap<String, Buddy>();
	private Collection<WifiP2pDevice> mGroupClients = null;
	
	public ConnectionState(String ourAddress) {
		this.ourAddress = ourAddress;
	}
	
	public boolean connected() {
		return mConnected;
	}
	
	public boolean haveGroupOwner() {
		return mGroupOwner != null;
	}
	
	public Buddy getBuddy(String deviceAddress) {
		Buddy b = buddies.get(deviceAddress);
		if(b == null) {
        	b = new Buddy();
        	buddies.put(deviceAddress, b);
        }
		return b;
	}
	
	public synchronized void updateStatus(WifiP2pDevice device) {
		Buddy b = getBuddy(device.deviceAddress);
		b.device = device;
		
		// Update connection state
		if(device.isGroupOwner()) {
			// does he belong to our service scope?
			if(buddies.get(device.deviceAddress).rightService) {
				mGroupOwner = device;
				mWeAreGroupOwner = device.deviceAddress.equals(ourAddress);
			}
		}
	}
	
	public synchronized void updateStatus(WifiP2pDeviceList peers) {
		if(peers == null) {
			return;
		}
		
		// add new
		ArrayList<String> addresses = new ArrayList<String>();
		for(final WifiP2pDevice device: peers.getDeviceList()) {
			addresses.add(device.deviceAddress);
			updateStatus(device);
		}
		// delete old
		for(String addr: buddies.keySet().toArray(new String[0])) {
			if(!addresses.contains(addr)) {
				buddies.remove(addr);
			}
		}
	}
	
	public synchronized void updateStatus(WifiP2pInfo info) {
		if(info == null) {
			return;
		}
		
		// Update connection state
		mGroupOwnerAddress = info.groupOwnerAddress;
		mWeAreGroupOwner = info.isGroupOwner;
		if(mWeAreGroupOwner) {
			mConnected = true;
		}
	}
	
	public synchronized void updateStatus(NetworkInfo info) {
		if(info == null) {
			return;
		}
		
		// Update connection state
		mConnected = info.isConnected();
		if (!mConnected) {
			mWeAreGroupOwner = false;
			mConnected = false;
			mGroupOwnerAddress = null;
			mGroupClients = null;
    	}
	}
	
	public synchronized void updateStatus(WifiP2pGroup group) {
		if(group == null) {
			return;
		}
		
		mGroupOwner = group.getOwner();
		mWeAreGroupOwner = group.isGroupOwner();
		mGroupClients = group.getClientList();
	}
	
	public synchronized void updateStatus(WifiP2pDevice device, String deviceName) {
		Buddy b = getBuddy(device.deviceAddress);
		b.device = device;
		b.deviceName = deviceName;
	}
	
	public synchronized void updateStatus(WifiP2pDevice device, int deviceServerPort) {
		Buddy b = getBuddy(device.deviceAddress);
		b.device = device;
		b.serverPort = deviceServerPort;
	}
	
	public Buddy findBuddyToConnect() {
		for(Buddy b: buddies.values()) {
			// thats ourselves...
			if(b.device.deviceAddress.equals(ourAddress)) {
				continue;
			}
			// thats not what we are interested in
			if(!b.rightService) {
				continue;
			}
			if(b.device.status != WifiP2pDevice.AVAILABLE) {
				continue;
			}
			return b;
		}
		return null;
	}
	
	public class Buddy {
		WifiP2pDevice device;
		String deviceName;
		int serverPort = -1;
		boolean rightService = false;
		WifiP2pInfo connection = null;
		
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
			
			return "{<Buddy> device=<<<"+device.deviceAddress+
					", " + device.deviceName+ ">>>, serverPort="+serverPort+
					", rightService="+rightService+
					", status="+statusText+"("+device.status+")"+
					", grpOwner="+device.isGroupOwner()+
					", deviceName="+deviceName+"}";
		}
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("mWeAreGroupOwner=");
		s.append(mWeAreGroupOwner + "\n");
		s.append("mConnected=");
		s.append(mConnected + "\n");
		s.append("mGroupOwner=");
		s.append(mGroupOwner + "\n");
		s.append("mGroupOwnerAddress=");
		s.append(mGroupOwnerAddress + "\n");
		s.append("buddies.size()=");
		s.append(buddies.size() + "\n");
		if(mGroupClients != null) {
			s.append("mGroupClients.size()=");
			s.append(mGroupClients.size() + "\n");
		}
		
		return s.toString();
	}
	
	public String buddiesToString() {
		StringBuilder s = new StringBuilder();
		for(Buddy b: buddies.values()) {
			s.append(b.toString());
			s.append("\n\n");
		}
		return s.toString();
	}
}
