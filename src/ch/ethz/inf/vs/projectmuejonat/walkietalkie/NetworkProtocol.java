package ch.ethz.inf.vs.projectmuejonat.walkietalkie;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class NetworkProtocol {
	/*
	 * Stateless protocoll. Each connection means a new message.
	 */
	
	public static final int CMD_LEN_BS = 4;
	public static final String CMD_REGISTER_CLIENT = "recl";
	public static final String CMD_SEND_DATA = "sdta";
	
	/**
	 * @param cmd
	 * @param data
	 * @param len how many bytes to take from data
	 * @return
	 */
	public static byte[] composeMessage(String cmd, byte[] data, int len) {
		byte[] buf = Arrays.copyOf(data, len);
		return composeMessage(cmd,  buf);
	}
	
	public static byte[] composeMessage(String cmd, byte[] data) {
		byte msg[] = new byte[data.length + CMD_LEN_BS];
		System.arraycopy(cmd.getBytes(), 0, msg, 0, CMD_LEN_BS);
		System.arraycopy(data, 0, msg, CMD_LEN_BS, data.length);
		return msg;
	}
	
	public static byte[] composeMessage(String cmd, String data) {
		return composeMessage(cmd, data.getBytes());
	}
	
	public static String getMessageType(InputStream is) {
		byte[] buf = new byte[CMD_LEN_BS];
		try {
			is.read(buf, 0, CMD_LEN_BS);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		String cmd = new String(buf);
		if(cmd.equals(CMD_REGISTER_CLIENT))
			return cmd;
		if(cmd.equals(CMD_SEND_DATA))
			return cmd;
		return null;
	}
	
	/**
	 * Call after a CMD_REGISTER_CLIENT was received
	 * @param is
	 */
	public static String getMacAddress(InputStream is) {
		byte[] buf = new byte[6 * 3 - 1];
		try {
			is.read(buf, 0, buf.length);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new String(buf);
	}
	
	/**
	 * Call after a CMD_SEND_DATA was received 
	 * @param is
	 * @return
	 */
	public static byte[] getData(InputStream is) {
		byte[] _buf = new byte[1024 * 1024];
		int len;
		int offset = 0;
		try {
			while((len = is.read(_buf, offset, _buf.length - offset)) >= 0) {
				offset += len;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return Arrays.copyOfRange(_buf, 0, offset);
	}
}
