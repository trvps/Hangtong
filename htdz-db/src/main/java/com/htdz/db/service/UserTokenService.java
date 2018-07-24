package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.UserTokenMapper;
import com.htdz.def.dbmodel.UserToken;

@Service
@MapperScan("com.htdz.db.mapper")
public class UserTokenService {
	@Autowired
	private UserTokenMapper userTokenMapper;

	public UserToken select(String name) {
		return userTokenMapper.select(name);
	}

	public List<UserToken> selectAll() {
		return userTokenMapper.selectAll();
	}

	public List<UserToken> selectPage(int pageIndex, int pageCount) {
		Page<UserToken> page = PageHelper.startPage(pageIndex, pageCount);
		userTokenMapper.selectAll();
		return page;
	}

	public boolean add(UserToken userToken) {
		int result = userTokenMapper.add(userToken);
		return result == 1 ? true : false;
	}

	public boolean modify(UserToken userToken) {
		int result = userTokenMapper.modify(userToken);
		return result == 1 ? true : false;
	}
}
