package com.htdz.def.dbmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TPhoneBook {
	private Integer did;        
	private String device_sn;   
	private String phone;       
	private Integer adminIndex;  
	private String photo;       
}
