package com.htdz.db.service;

import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.TConnServerMapper;
import com.htdz.def.dbmodel.TConnServer;

@Service
@MapperScan("com.htdz.db.mapper")
public class TConnServerService {

	@Autowired
	TConnServerMapper tConnServerMapper;

	public TConnServer getByUsername(String deviceSn) {
		return tConnServerMapper.getByUsername(deviceSn);
	}

	public Map<String, Object> select(String sql) {
		return tConnServerMapper.select(sql);
	}

}
