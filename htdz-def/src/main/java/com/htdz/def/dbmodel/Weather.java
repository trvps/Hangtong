package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Weather implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String cityCode;
	private String temperature;
	private String wCode;
	private Date wTime;
	private Date createTime;
	private String cityName;
	private String img;
}
