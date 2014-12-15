package ch.ethz.inf.vs.project.muejonat.walkietalkie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MessageDeduplicator {
	/**
	 * Very primitive method to ensure that we
	 * do not re-play messages we receive which
	 * are actually an echo of ours (i.e. we sent
	 * it to group owner, and he re-broadcasted id).
	 * 
	 * We just assume two messages will never have the
	 * same content (which is highly likely for audio)
	 */
	
	private ArrayList<String> msgHashes = new ArrayList<String>();
	
	private static String hashMessage(byte[] msg) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			return getHex(md.digest(msg));
		} catch (NoSuchAlgorithmException e) {}
		return null;
	}
	
	private static final String HEXES = "0123456789ABCDEF";
	  
	private static String getHex( byte [] raw ) {
		StringBuilder hex = new StringBuilder( 2 * raw.length );
		for(byte b: raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
	        	.append(HEXES.charAt((b & 0x0F)));
	    }
	    return hex.toString();
	}

	
	public void addMessage(byte[] msg) {
		String hash = hashMessage(msg);
		if(!msgHashes.contains(hash)) {
			msgHashes.add(hash);
		}
	}
	
	public boolean messageIsNew(byte[] msg) {
		String hash = hashMessage(msg);
		boolean new_ = !msgHashes.contains(hash);
		if(new_) {
			msgHashes.add(hash);
		}
		return new_;
	}
}
