package com.htdz.db.service;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.htdz.db.mapper.DeviceFriendMapper;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceFriendService {
	@Autowired
	private DeviceFriendMapper deviceFriendMapper;
	
	public boolean isFriend(String dsn1, String dsn2) {
		int result =  deviceFriendMapper.isFriend(dsn1, dsn2);
		return result >= 1 ? true : false;
	}
	
	public boolean addFriend(String dsn1, String dsn2) {
		int result = deviceFriendMapper.addFriend(dsn1, dsn2);
		return result == 1 ? true : false;
	}
	
	public boolean removeFriend(String dsn1, String dsn2) {
		int result = deviceFriendMapper.removeFriend(dsn1, dsn2);
		return result >= 1 ? true : false;
	}
}
