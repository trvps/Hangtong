package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.GroupUserMapper;
import com.htdz.def.dbmodel.GroupUser;
import com.htdz.def.view.GroupUserInfo;

@Service
@MapperScan("com.htdz.db.mapper")
public class GroupUserService {
	@Autowired
	private GroupUserMapper GroupUserMapper;

	public List<GroupUserInfo> getGroupUserInfo(String deviceSn) {
		return GroupUserMapper.getGroupUserInfo(deviceSn);
	}

	public GroupUserInfo getGroupUserByName(String deviceSn, String username) {
		return GroupUserMapper.getGroupUserByName(deviceSn, username);
	}

	public List<GroupUser> selectAll() {
		return GroupUserMapper.selectAll();
	}

	public List<GroupUser> selectPage(int pageIndex, int pageCount) {
		Page<GroupUser> page = PageHelper.startPage(pageIndex, pageCount);
		GroupUserMapper.selectAll();
		return page;
	}

}
