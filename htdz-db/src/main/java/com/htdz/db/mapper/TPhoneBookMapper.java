package com.htdz.db.mapper;

import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.TPhoneBook;

public interface TPhoneBookMapper {
	@Select("SELECT * FROM `TPhoneBook` WHERE `device_sn`=#{0}")
	public TPhoneBook getPhoneBookByDeviceSn(String deviceSn);
}
