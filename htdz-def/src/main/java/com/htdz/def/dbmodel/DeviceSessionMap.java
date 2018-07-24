package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceSessionMap implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String deviceName;
	private String device_sn;
	private String deviceSession;
	private Date createTime;
}
