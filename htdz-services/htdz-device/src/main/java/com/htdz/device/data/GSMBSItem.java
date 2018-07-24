package com.htdz.device.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GSMBSItem implements Comparable<Object>{
	private String areaCode; // 区域码
	private String bscode; // 基站编号
	private int bsSignalStrength; // 基站信号强度

	@Override
	public int compareTo(Object o) {
		GSMBSItem gsm = (GSMBSItem) o;
		if (this.bsSignalStrength > gsm.getBsSignalStrength()) {
			return -1;
		} else if (this.bsSignalStrength < gsm.getBsSignalStrength()) {
			return 1;
		} else {
			return 0;
		}
	}
}
