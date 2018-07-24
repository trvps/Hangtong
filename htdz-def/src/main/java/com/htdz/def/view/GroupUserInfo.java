package com.htdz.def.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupUserInfo implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private String name;
	private Integer type;
	private String deviceSn;
	private String Portrait;
	private String nickname;
	private String remark;

}
