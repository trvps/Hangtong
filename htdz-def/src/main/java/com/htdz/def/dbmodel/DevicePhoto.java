package com.htdz.def.dbmodel;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevicePhoto implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String deviceSn;
	private String url;
	private String urlThumbnail;
	private Date createTime;
}
