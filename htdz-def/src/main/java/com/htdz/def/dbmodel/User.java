package com.htdz.def.dbmodel;


import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	

	private Integer id;
	private Integer pid;
	private String path;
	private Integer orgId;
	private Integer roleId;
	private String username;
	private String name;
	private String password;
	private String email;
	private String phone;
	private String lang;
	private String img;
	private Integer status;
	private String mark;
	private Date createTime;
	private String mapCenter;
	private Integer mapZoom;
}
