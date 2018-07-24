package com.htdz.device.data;


import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class GSMBSGroup {
	private int ta;			// GSM时延
	private String mcc;		// 国家码 460代表中国
	private String mnc;		// 网号 02代表中国移动
	
	// 基站信息列表
	List<GSMBSItem> bsItems = new ArrayList<GSMBSItem>();
}
