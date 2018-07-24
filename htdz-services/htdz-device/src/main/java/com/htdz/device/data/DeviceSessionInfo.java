package com.htdz.device.data;



import com.htdz.common.utils.ByteArrayBuffer;
import lombok.Data;


@Data
public class DeviceSessionInfo {
	public static final int TIMEOUT = 16*1000;
	private String key;
	private long uptimestamp;
	private ByteArrayBuffer baBuffer;
	
	public DeviceSessionInfo() {
		baBuffer = new ByteArrayBuffer();
		uptimestamp = System.currentTimeMillis();
	}
	
	public boolean isTimeout() {
		return isTimeout(TIMEOUT);
	}
	
	public boolean isTimeout(int milliTimeout) {
		return System.currentTimeMillis() - uptimestamp > milliTimeout;
	}
	
	public void updateTimeStame() {
		uptimestamp = System.currentTimeMillis();
	}
	
	
}
