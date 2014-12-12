package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import ch.ethz.inf.vs.projectmuejonat.walkietalkie.ConnectionState.Buddy;

import ch.ethz.inf.vs.projectmuejonat.walkietalkie.R;

public abstract class WifiActivity extends Activity {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private TextView mTextView;
	private TextView mStatTextView;
	private WifiP2pDnsSdServiceInfo mServiceInfo;
	private WifiP2pDnsSdServiceRequest mServiceRequest;
	private MessageDeduplicator mDeduplicator = new MessageDeduplicator();
	
	private DiscoveryBGThread mDiscoveryTask;
	private StatusDisplayBGThread mStatusUpdateTask;
	private ConnectionBGThread mConnectionTask;
	private ServerBGThread mServerTask;
	
	private final static String LOGTAG = "WIFI_P2P_VS";
	private final String SERVICE_NAME = "_walkietalkie._tcp";
	
	public final static int SERVER_PORT = 42634;
	private TextView mConnStatTextView;
	private RegisterClientBGThread mRegisterTask;
	private ReceivedMessageShower mMessageShowTask;
	
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
	    setupDebugButtons();
	}
	
	private void setupDebugButtons() {
		Button debugButton = (Button) findViewById(R.id.debugButton);
		debugButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {((ViewSwitcher) findViewById(R.id.viewSwitcher)).showNext();}
		});
		
		debugButton = (Button) findViewById(R.id.debugButton2);
		debugButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {((ViewSwitcher) findViewById(R.id.viewSwitcher)).showNext();}
		});
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    
	    state = new ConnectionState(getMacAddress());
	    
	    registerReceiver(mReceiver, mIntentFilter);
	    mManager.addLocalService(mChannel, mServiceInfo, null);
	    
	    mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        
        mDiscoveryTask = new DiscoveryBGThread();
        mStatusUpdateTask = new StatusDisplayBGThread();
        mConnectionTask = new ConnectionBGThread();
        mServerTask = new ServerBGThread();
        mRegisterTask = new RegisterClientBGThread();
        mMessageShowTask = new ReceivedMessageShower();
        // execute tasks in parallel
	    mDiscoveryTask.start();
	    mStatusUpdateTask.start();
	    mConnectionTask.start();
	    mServerTask.start();
	    mRegisterTask.start();
	    mMessageShowTask.start();
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	    mDiscoveryTask.setStop();
	    mStatusUpdateTask.setStop();
	    mConnectionTask.setStop();
	    mServerTask.setStop();
	    mRegisterTask.setStop();
	    mMessageShowTask.setStop();
	    
	    mManager.removeLocalService(mChannel, mServiceInfo, null);
	    mManager.removeServiceRequest(mChannel, mServiceRequest, null);
	    
	    mManager.cancelConnect(mChannel, null);
	    mManager.removeGroup(mChannel, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void publishData(byte[] data) {
		if(state.mWeAreGroupOwner) {
			broadcastData(data);
		} else {
			sendData(data);
		}
	}
	
	protected abstract void showData(byte[] data);
	
	/**
	 * Only works if we are server!!
	 */
	private void broadcastData(byte[] data) {
		if(!state.mWeAreGroupOwner) {
			return;
		}
		Log.d(LOGTAG, "BROADCAST");
		byte[] msg = NetworkProtocol.composeMessage(NetworkProtocol.CMD_SEND_DATA, data);
		for(InetSocketAddress a: state.getClientAddresses()) {
			Log.d(LOGTAG, "CLIENT BROADCAST TO IP="+a);
			sendMessageToAddr(msg, a);
		}
	}
	
	/**
	 * Only works if we are client!!
	 */
	private void sendData(byte[] data) {
		if(state.mWeAreGroupOwner) {
			return;
		}
		Log.d(LOGTAG, "SEND");
		byte[] msg = NetworkProtocol.composeMessage(NetworkProtocol.CMD_SEND_DATA, data);
		mDeduplicator.addMessage(data);
		sendMessageToAddr(msg, state.getGroupOwnerConnectionInfos());
	}
	
	/**
	 * Must be called on UI thread!
	 * @param msg
	 */
	private void dataReceived(byte[] data) {
		if(!mDeduplicator.messageIsNew(data)) {
			return;
		}
		
		messagesToProcess.offer(data);
		
		// rebroadcast immediately
		if(state.mWeAreGroupOwner) {
			broadcastData(data);
		}
	}
	
	private BlockingQueue<byte[]> messagesToProcess = new ArrayBlockingQueue<byte []>(100);
	
	private class ReceivedMessageShower extends Thread {
		private boolean stop = false;

		public void setStop() {
			stop = true;
		}

		@Override
		public void run() {
			while(!stop) {
				try {
					final byte[] data = messagesToProcess.poll(1000, TimeUnit.MILLISECONDS);
					if(data != null) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showData(data);
							}
						});
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {}
				}
			}
		}
	}
	
	private class ServerConnectionThread extends Thread {

		private final Socket client;
		
		public ServerConnectionThread(Socket client) {
			this.client = client;
		}
		
		public void run() {
			try {
				InputStream in = client.getInputStream();
				
				String cmd = NetworkProtocol.getMessageType(in);
				if(cmd != null && cmd.equals(NetworkProtocol.CMD_REGISTER_CLIENT)) {
					String mac = NetworkProtocol.getMacAddress(in);
					state.updateClientIp(mac, client.getInetAddress());
				}
				if(cmd != null && cmd.equals(NetworkProtocol.CMD_SEND_DATA)) {
					final byte[] data = NetworkProtocol.getData(in);
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							WifiActivity.this.dataReceived(data);
						}
					});
				}
				
	            in.close();
	            client.close();
			} catch (IOException e) {}
		}
	}
	
	private void sendMessageToAddr(final byte[] msg, final InetSocketAddress addr) {
		if(addr == null)
			return;
		
		new Thread() {
			public void run() {
				Socket socket = new Socket();
				try {
					socket.bind(null);
					socket.connect(addr, 5000);
					
					socket.getOutputStream().write(msg);
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private class RegisterClientBGThread extends Thread {
		private boolean stop = false;

		public void setStop() {
			stop = true;
		}

		@Override
		public void run() {
			while(!stop) {
				tryToRegister();
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {}
			}
		}
		
		private void tryToRegister() {
			if(!state.mConnected || state.mWeAreGroupOwner) {
				return;
			}
				
			byte[] msg = NetworkProtocol.composeMessage(NetworkProtocol.CMD_REGISTER_CLIENT, getMacAddress());
			sendMessageToAddr(msg, state.getGroupOwnerConnectionInfos());
	    }
	}
	
	private class ServerBGThread extends Thread {
		private ServerSocket serverSocket;
		
		public void setStop() {
			try {
				serverSocket.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	    @Override
	    public void run() {
	        try {
	            serverSocket = new ServerSocket(SERVER_PORT);
	            
	            try {
	            	while(true) {
	            		Socket client = serverSocket.accept();
	            		ServerConnectionThread conn = WifiActivity.this.new ServerConnectionThread(client);
	            		conn.start();
	            	}
	            } catch (SocketException e) {
	            	// our serverSocket got killed, exit
	            	e.printStackTrace();
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
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
	        	if(ConnectionState.macAddressAlmostEqual(device.deviceAddress, getMacAddress())) {
	        		return;
	        	}
	        	
	        	Buddy b = state.getBuddy(device.deviceAddress);
			
				b.device = device;
				
    			if(registrationType.startsWith(SERVICE_NAME)) {
    				b.rightService = true;
    			}
	        }
	    };

	    mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
	}
	
	private class DiscoveryBGThread extends Thread {
		private boolean stop = false;
		
		public void setStop() {
			stop = true;
		}

		@Override
		public void run(){
			while(!stop) {
				mManager.discoverServices(mChannel, null);
				mManager.requestConnectionInfo(mChannel, null);
				
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	private class StatusDisplayBGThread extends Thread {
		private boolean stop = false;
		
		public void setStop() {
			stop = true;
		}

		@Override
		public void run() {
			while(!stop) {
				updateStatus();
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {}
			}
		}
		
		private void updateStatus() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTextView.setText(state.buddiesToString());
					mStatTextView.setText(state.toString());
				}
			});
			
	    }
	}
	
	private class ConnectionBGThread extends Thread {
		private boolean stop = false;

		public void setStop() {
			stop = true;
		}

		@Override
		public void run() {
			while(!stop) {
				manageConnection();
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	private void manageConnection() {
		final StringBuilder connState = new StringBuilder();

		if(state.connected()) {
			connState.append("CONNECTED\n");
			// search for buddies which have to be invited
			for(Buddy b: state.findSingleBuddies()) {
				connState.append("INVITE: " + b + "\n");
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = b.device.deviceAddress;

				mManager.connect(mChannel, config, null);
			}
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
					WifiP2pConfig config = new WifiP2pConfig();
					config.deviceAddress = b.device.deviceAddress;
					
					mManager.connect(mChannel, config, null);
				} else {
					connState.append("NO BUDDY TO CONNECT TO\n");
				}
			}
		}
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnStatTextView.setText(connState.toString());
			}
		});
	}
}
