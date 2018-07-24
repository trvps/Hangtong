package com.htdz.db.service;


import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.TgpsDataMapper;
import com.htdz.def.dbmodel.TgpsData;


@Service
@MapperScan("com.htdz.db.mapper")
public class TgpsDataService {
	@Autowired
	private TgpsDataMapper tgpsDataMapper;
	
	public TgpsData select(String deviceSn) {
		return tgpsDataMapper.select(deviceSn);
	}
	
	public List<TgpsData> selectAll() {
		return tgpsDataMapper.selectAll();
	}
	
	public List<TgpsData> selectPage(int pageIndex, int pageCount) {
		Page<TgpsData> page = PageHelper.startPage(pageIndex, pageCount); 
		tgpsDataMapper.selectAll();
		return page;
	}
	
	public Integer add(TgpsData tgpsData) {
		return tgpsDataMapper.add(tgpsData);
		 
	}
	
	public boolean modify(TgpsData tgpsData) {
		int result = tgpsDataMapper.modify(tgpsData);
		return result == 1 ? true : false;
	}
	
	public boolean insertGpsDataBySql(String sql)
	{
		int result = tgpsDataMapper.insertGpsDataBySql(sql);
		return result == 1 ? true : false;
	}
}



