package com.htdz.device.data;

import java.util.Comparator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WifiItem implements Comparable<Object> {
	private String name; // 名字
	private String macaddr; // mac地址
	private int signalStrength; // 信号强度

	@Override
	public int compareTo(Object o) {
		WifiItem wifi = (WifiItem) o;
		if (this.signalStrength > wifi.getSignalStrength()) {
			return -1;
		} else if (this.signalStrength < wifi.getSignalStrength()) {
			return 1;
		} else {
			return 0;
		}
	}
}
