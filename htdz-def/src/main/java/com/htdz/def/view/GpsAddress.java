package com.htdz.def.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GpsAddress implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	// location 经度在前，纬度在后，经纬度间以“,”分割，经纬度小数点后不要超过 6 位
	private String location;
	// lang 语言
	private String lang;
	// 结构化地址信息包括：省份＋城市＋区县＋城镇＋乡村＋街道＋门牌号码
	private String formattedAddress;
	// 坐标点所在省名称
	private String province;
	// 坐标点所在城市名称
	private String city;
	// 城市编码 例如：010
	private String citycode;
	// 坐标点所在区
	private String district;

}
