package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRemind implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private String deviceSn;
	private Date remindTime;
	private Date remindTimeUTC;
	private String title;
	private Integer type;
}
