package com.htdz.def.dbmodel;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TareaInfo generated by hbm2java
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TareaInfo implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer areaid;
	private Double lat;
	private Double lng;
	private Integer radius;
	private Long did;
	private Integer uid;
	private Integer enabled;
	private Date createTime;
	private String defencename;
	private Integer type;// 0.出围栏报警，1.出围栏，进围栏都报警
	private Integer isOut;// 0.围栏内 1.围栏外

}
