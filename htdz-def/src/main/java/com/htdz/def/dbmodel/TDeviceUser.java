package com.htdz.def.dbmodel;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TDeviceUser {
	private Integer did;
	private Integer uid;
	private Integer is_super_user;
	private String nickname;
	private Integer is_gps;
	private String dname;
	private Date create_time;
}
