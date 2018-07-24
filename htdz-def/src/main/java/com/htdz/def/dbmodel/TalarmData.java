package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalarmData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer id;
	private Integer did;
	private String deviceSn;
	private Date collectDatetime;
	private Date endTime;
	private Date rcvTime;
	private Integer type;
	private Integer alarmFlag;
	private String data;
	private Double lat;
	private Double lng;
	private Float speed;
	private Float direction;
	private Integer satelliteNum;
	private Integer locationId;
	private Integer cellId;
	private String gpsFlag;
	private String flag;
	private String battery;
	private Integer status;
	private Integer deletestatus;
	private Integer readstatus;
	private Integer isCount;
}
