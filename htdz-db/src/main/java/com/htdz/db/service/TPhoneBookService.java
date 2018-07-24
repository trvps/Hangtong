package com.htdz.db.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.TPhoneBookMapper;
import com.htdz.def.dbmodel.TPhoneBook;

@Service
@MapperScan("com.htdz.db.mapper")
public class TPhoneBookService {
	@Autowired
	TPhoneBookMapper tPhoneBookMapper;
	
	public TPhoneBook getPhoneBookByDeviceSn(String deviceSn)
	{
		return tPhoneBookMapper.getPhoneBookByDeviceSn(deviceSn);
	}
}
