package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.TalarmDataMapper;
import com.htdz.def.dbmodel.TalarmData;

@Service
@MapperScan("com.htdz.db.mapper")
public class TalarmDataService {
	@Autowired
	private TalarmDataMapper talarmDataMapper;
	
	public TalarmData select(String name) {
		return talarmDataMapper.select(name);
	}
	
	public List<TalarmData> selectAll() {
		return talarmDataMapper.selectAll();
	}
	
	public List<TalarmData> selectPage(int pageIndex, int pageCount) {
		Page<TalarmData> page = PageHelper.startPage(pageIndex, pageCount); 
		talarmDataMapper.selectAll();
		return page;
	}
	
	public Integer add(TalarmData talarmData) {
		return talarmDataMapper.add(talarmData);
	}
	
	public boolean modify(TalarmData talarmData) {
		int result = talarmDataMapper.modify(talarmData);
		return result == 1 ? true : false;
	}
	
	public boolean insertAlarmDataBySql(String sql)
	{
		int result = talarmDataMapper.insertAlarmDataBySql(sql);
		return result == 1 ? true : false;
	}
}
