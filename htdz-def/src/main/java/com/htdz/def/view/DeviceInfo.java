package com.htdz.def.view;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInfo implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private Integer did;
	private String deviceSn;
	private String headPortrait;
	private String humanName;
	private Integer humanSex;
	private Date humanBirthday;
	private Float humanHeight;
	private Float humanWeight;
	private Float humanStep;
	private String humanFeature;
}
