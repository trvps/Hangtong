package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceStep implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private String deviceSn;
	private Date createDate;
	private Date updateDate;
	private String step;

}
