package com.htdz.db.service;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.UserMapper;
import com.htdz.def.dbmodel.User;
import com.htdz.def.view.UserConn;

@Service
@MapperScan("com.htdz.db.mapper")
public class UserService {
	@Autowired
	private UserMapper userMapper;

	public User selectUser(String name) {
		return userMapper.selectUser(name);
	}

	public List<User> selectAllUser() {
		return userMapper.selectAllUser();
	}

	public List<User> selectPageUser(int pageIndex, int pageCount) {
		Page<User> page = PageHelper.startPage(pageIndex, pageCount);
		userMapper.selectAllUser();
		return page;
	}

	public boolean addUser(User user) {
		int result = userMapper.addUser(user);
		return result == 1 ? true : false;
	}

	public boolean modifyUser(User user) {
		int result = userMapper.modifyUser(user);
		return result == 1 ? true : false;
	}

	public User selectUserBySql(String sql) {
		return userMapper.selectUserBySql(sql);
	}

	/**
	 * 根据设备号取绑定和授权用户信息
	 * 
	 * @param deviceSn
	 * @return
	 */
	public List<UserConn> getUserDeviceInfo(String deviceSn) {
		return userMapper.getUserDeviceInfo(deviceSn);
	}

	/**
	 * 设备号用户信息
	 * 
	 * @param deviceSn
	 * @return
	 */
	public UserConn getDeviceAsUserInfo(String deviceSn) {
		return userMapper.getDeviceAsUserInfo(deviceSn);
	}

	public List<Map<String, Object>> select(String sql) {
		return userMapper.select(sql);
	}
}
