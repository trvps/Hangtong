package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushLog implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String deviceSn;
	private Integer msgType;
	private String msg;
	private String pushUser;
	private Integer phoneType;
	private Integer isPush;
	private Date createTime;
	private Date updateTime;

}
