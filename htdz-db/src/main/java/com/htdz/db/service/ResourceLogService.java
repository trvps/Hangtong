package com.htdz.db.service;


import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.ResourceLogMapper;
import com.htdz.def.dbmodel.ResourceLog;


@Service
@MapperScan("com.htdz.db.mapper")
public class ResourceLogService {
	@Autowired
	private ResourceLogMapper ResourceLogMapper;
	
	public ResourceLog select(String name) {
		return ResourceLogMapper.select(name);
	}
	
	public List<ResourceLog> selectAll() {
		return ResourceLogMapper.selectAll();
	}
	
	public List<ResourceLog> selectPage(int pageIndex, int pageCount) {
		Page<ResourceLog> page = PageHelper.startPage(pageIndex, pageCount); 
		ResourceLogMapper.selectAll();
		return page;
	}
	
	public boolean add(ResourceLog ResourceLog) {
		int result = ResourceLogMapper.add(ResourceLog);
		return result == 1 ? true : false;
	}
	
	public boolean modify(ResourceLog ResourceLog) {
		int result = ResourceLogMapper.modify(ResourceLog);
		return result == 1 ? true : false;
	}
}



