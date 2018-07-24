package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.PushLogMapper;
import com.htdz.def.dbmodel.PushLog;

@Service
@MapperScan("com.htdz.db.mapper")
public class PushLogService {
	@Autowired
	private PushLogMapper pushLogMapper;

	public PushLog select(String name) {
		return pushLogMapper.select(name);
	}

	public List<PushLog> selectAll() {
		return pushLogMapper.selectAll();
	}

	public List<PushLog> selectPage(int pageIndex, int pageCount) {
		Page<PushLog> page = PageHelper.startPage(pageIndex, pageCount);
		pushLogMapper.selectAll();
		return page;
	}

	public Integer add(PushLog pushLog) {
		return pushLogMapper.add(pushLog);

	}

	public boolean modify(PushLog pushLog) {
		int result = pushLogMapper.modify(pushLog);
		return result == 1 ? true : false;
	}
}
