package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.PrePushLogMapper;
import com.htdz.def.dbmodel.PrePushLog;

@Service
@MapperScan("com.htdz.db.mapper")
public class PrePushLogService {
	@Autowired
	private PrePushLogMapper prePushLogMapper;

	public PrePushLog select(String name) {
		return prePushLogMapper.select(name);
	}

	public List<PrePushLog> selectAll() {
		return prePushLogMapper.selectAll();
	}

	public List<PrePushLog> selectPage(int pageIndex, int pageCount) {
		Page<PrePushLog> page = PageHelper.startPage(pageIndex, pageCount);
		prePushLogMapper.selectAll();
		return page;
	}

	public Integer add(PrePushLog PrePushLog) {
		return prePushLogMapper.add(PrePushLog);

	}

	public boolean modify(PrePushLog PrePushLog) {
		int result = prePushLogMapper.modify(PrePushLog);
		return result == 1 ? true : false;
	}
}
