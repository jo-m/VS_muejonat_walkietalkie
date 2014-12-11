package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import java.net.InetAddress;
import java.net.InetSocketAddress;
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
	
	public ConnectionState(String ourAddress) {
		this.ourAddress = ourAddress;
	}
	
	public static boolean macAddressAlmostEqual(String a, String b) {
		int distance = 0;
		if(a == null || b == null)
			return false;
		if(a.length() != b.length())
			return false;
		for(int i = 0; i < a.length(); i++) {
			distance += (a.codePointAt(i) == b.codePointAt(i)) ? 0 : 1;
		}
		return distance <= 2 ;
	}
	
	private boolean almostOurAddress(String addr) {
		return macAddressAlmostEqual(ourAddress, addr);
	}
	
	public synchronized InetSocketAddress getGroupOwnerConnectionInfos() {
		if(mGroupOwner == null) {
			// Hacky hack make an exception... we just assume default port but do
			// not know the name etc (no bonjour discovery made)
			Log.d(LOGTAG, "getGroupOwnerConnectionInfos HACK " + mConnected + " " + !mWeAreGroupOwner + " " + mGroupOwnerAddress);
			if(mConnected && !mWeAreGroupOwner && mGroupOwnerAddress != null) {
				return new InetSocketAddress(mGroupOwnerAddress, MainActivity.SERVER_PORT);
			}
			return null;
		}
		Buddy b = getBuddy(mGroupOwner.deviceAddress);
		if(b.serverPort > 0) {
			return new InetSocketAddress(mGroupOwnerAddress, b.serverPort);
		}
		return null;
	}
	
	public Collection<InetSocketAddress> getClientAddresses() {
		ArrayList<InetSocketAddress> ret = new ArrayList<InetSocketAddress>();
		for(Buddy b: buddies.values()) {
			if(b.addr != null) {
				int port = b.serverPort;
				if(port < 0) {
					port = MainActivity.SERVER_PORT;
				}
				ret.add(new InetSocketAddress(b.addr, port));
			}
		}
		return ret;
	}
	
	public synchronized Buddy getBuddy(String deviceAddress) {
		Buddy b = buddies.get(deviceAddress);
		if(b == null) {
        	b = new Buddy();
        	buddies.put(deviceAddress, b);
        }
		
		return b;
	}
	
	private String findSimilarBuddyAddress(String addr) {
		for(String a: buddies.keySet()) {
			if(macAddressAlmostEqual(addr, a)) {
				return a;
			}
		}
		return null;
	}
	
	public synchronized void updateClientIp(String deviceAddress, InetAddress addr) {
		if(almostOurAddress(deviceAddress)) {
			return;
		}
		String key = findSimilarBuddyAddress(deviceAddress);
		if(key == null) {
			return;
		}
		Buddy b = getBuddy(key);
		if(b.device == null)
			Log.i(LOGTAG, "updateClientIp but DEVICE IS NULL key="+key);
		b.addr = addr;
	}
	
	public synchronized void updateStatus(WifiP2pDevice device) {
		if(almostOurAddress(device.deviceAddress)) {
			return;
		}
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
		updateStatus(peers.getDeviceList());
	}
	
	public synchronized void updateStatus(Collection<WifiP2pDevice> peers) {
		if(peers == null) {
			return;
		}
		
		// add new
		ArrayList<String> addresses = new ArrayList<String>();
		for(final WifiP2pDevice device: peers) {
			addresses.add(device.deviceAddress);
			updateStatus(device);
		}
		// delete old
		for(String addr: buddies.keySet().toArray(new String[0])) {
			if(!addresses.contains(addr)) {
				buddies.remove(addr);
				if(mGroupOwner != null && mGroupOwner.deviceAddress.equals(addr)) {
					mGroupOwner = null;
					mGroupOwnerAddress = null;
				}
			}
		}
	}
	
	public synchronized void updateStatus(WifiP2pInfo info) {
		if(info == null) {
			return;
		}
		
		// Update connection state
		if(info.groupFormed) {
			mGroupOwnerAddress = info.groupOwnerAddress;
			mWeAreGroupOwner = info.isGroupOwner;
			if(mWeAreGroupOwner) {
				mConnected = true;
			}
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
    	}
	}
	
	public synchronized void updateStatus(WifiP2pGroup group) {
		if(group == null) {
			return;
		}
		
		mGroupOwner = group.getOwner();
		mWeAreGroupOwner = group.isGroupOwner();
		updateStatus(group.getClientList());
	}
	
	public synchronized void updateStatus(WifiP2pDevice device, String deviceName) {
		if(almostOurAddress(device.deviceAddress)) {
			return;
		}
		Buddy b = getBuddy(device.deviceAddress);
		b.device = device;
		b.deviceName = deviceName;
	}
	
	public synchronized void updateStatus(WifiP2pDevice device, int deviceServerPort) {
		if(almostOurAddress(device.deviceAddress)) {
			return;
		}
		Buddy b = getBuddy(device.deviceAddress);
		b.device = device;
		b.serverPort = deviceServerPort;
	}
	
	public synchronized boolean connected() {
		return mConnected;
	}
	
	public synchronized boolean haveGroupOwner() {
		return mGroupOwner != null;
	}
	
	public synchronized ArrayList<Buddy> findSingleBuddies() {
		ArrayList<Buddy> ret = new ArrayList<Buddy>();
		for(Buddy b: buddies.values()) {
			// thats ourselves...
			if(almostOurAddress(b.device.deviceAddress)) {
				continue;
			}
			// thats not what we are interested in
			if(!b.rightService) {
				continue;
			}
			if(b.device.status != WifiP2pDevice.AVAILABLE
					&& b.device.status != WifiP2pDevice.INVITED) {
				continue;
			}
			ret.add(b);
		}
		return ret;
	}
	
	public synchronized Buddy findBuddyToConnect() {
		ArrayList<Buddy> ret = findSingleBuddies();
		if(ret.size() == 0) {
			return null;
		}
		return ret.get(0);
	}
	
	public class Buddy {
		WifiP2pDevice device;
		String deviceName;
		int serverPort = -1;
		boolean rightService = false;
		WifiP2pInfo connection = null;
		InetAddress addr;
		
		public String toString() {
			if(device == null) {
				return "device = <null>";
			}
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
					", addr="+(addr == null ? "null" : addr.toString())+
					", deviceName="+deviceName+"}";
		}
	}
	
	public synchronized String toString() {
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
		
		return s.toString();
	}
	
	public synchronized String buddiesToString() {
		StringBuilder s = new StringBuilder();
		for(Buddy b: buddies.values()) {
			s.append(b.toString());
			s.append("\n\n");
		}
		return s.toString();
	}
}
