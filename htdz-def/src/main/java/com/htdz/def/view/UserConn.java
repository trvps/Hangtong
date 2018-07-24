package com.htdz.def.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConn implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private Integer timezone;
	private String deviceSn;
	private Integer ranges;
	private String connCountry;
	private Integer isCustomizedApp;
	private String token;
	private String versions;
	private Integer certificate;// 证书 默认0：航通守护者用户。1：litefamily 用户
}
