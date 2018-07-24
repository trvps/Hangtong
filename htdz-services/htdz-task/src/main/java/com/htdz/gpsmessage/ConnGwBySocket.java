package com.htdz.gpsmessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.htdz.common.utils.PropertyUtil;

public class ConnGwBySocket {
	String config = PropertyUtil.getPropertyValue("application", "spring.profiles.active");
	int waitconnectiontime =Integer.parseInt(PropertyUtil.getPropertyValue("application-"+config, "waitconnectiontime"));
	int waitresponsetime =Integer.parseInt(PropertyUtil.getPropertyValue("application-"+config, "waitresponsetime"));
	
	Socket client = null;
	OutputStream OutPut = null;
	InputStream input = null;

	private static final int connLength = 30000;
	
	// 1.连接到网关
	public int Conn2GW(String strIP, String iPort) {
		try {			
			client = new Socket();
			SocketAddress remoteAddr = new InetSocketAddress(strIP, Integer.parseInt(iPort));
			client.connect(remoteAddr, waitconnectiontime*1000); //等待建立连接的超时时间为1分钟
		
			client.setReceiveBufferSize(connLength);
			client.setSendBufferSize(connLength);
			client.setSoTimeout(waitresponsetime*1000);// 设置1分钟超时时间
			
			OutPut = client.getOutputStream();
			input = client.getInputStream();

			return 1;
		} catch (IOException e) {
			release();
			return -1;
		}
	}

	public int Send2GW(byte[] arData) {
		if (arData == null) {
			return -1;
		}

		if (arData.length > connLength)// 最多可以发3000字节长度的数据，足够了
		{
			return -1;
		}

		try {
			if (client != null) {
				OutPut.write(arData);
				OutPut.flush();
				return 1;
			}
		} catch (IOException e) {
			return -1;
		}

		return -1;

	}

	public byte[] RcvFromGW() {
		try {
			if (client != null) {
				byte[] NewByte = new byte[connLength];
				int ilen = input.read(NewByte);
				if (ilen <= 0) {
					return null;
				}

				byte[] arData = new byte[ilen];
				System.arraycopy(NewByte, 0, arData, 0, ilen);
				return arData;
			}
		} catch (IOException e) {
			return null;
		}

		return null;

	}

	public void release() {
		try {
			if (client != null) {
				client.close();
				client = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (OutPut != null) {
				OutPut.close();
				OutPut = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (input != null) {
				input.close();
				input = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
