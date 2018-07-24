package com.htdz.def.dbmodel;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceFriendData {
	private String deviceSn;
	private Double lat;
	private Double lng;
	private Date date;
	private Integer accuracy;
}
