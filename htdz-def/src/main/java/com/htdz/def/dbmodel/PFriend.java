package com.htdz.def.dbmodel;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PFriend {
	private Integer pid;
	private String deviceSn;
	private String pdeviceSn;
	private Date createTime;
}
