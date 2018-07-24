package com.htdz.def.dbmodel;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TAlarmSetting {
	private Integer did;            
	private Short boundary;         
	private Short voltage;          
	private Short tow;              
	private Short clipping;         
	private Short speed;            
	private Short speedValue;       
	private Short speedTime;        
	private Short sos;              
	private Date create_time;      
	private Integer uid;              
	private Short vibration;        
	private Short vibrationAspeed;  
	private Short vibrationTime;    
	private Short takeOff;
}
