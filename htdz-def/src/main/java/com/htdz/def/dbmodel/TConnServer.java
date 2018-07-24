package com.htdz.def.dbmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TConnServer {
	private Integer connid;
	private Integer connDevice;
	private Integer connName;
	private Integer connExt;
	private Integer connType;
	private Integer connPort;
	private String connCountry;
	private Integer status;
	private Integer protocolType;
}
