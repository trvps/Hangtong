package com.htdz.db.service;

import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.htdz.db.mapper.PFriendMapper;
import com.htdz.def.dbmodel.PFriend;

@Service
@MapperScan("com.htdz.db.mapper")
public class PFriendService {
	@Autowired
	private PFriendMapper PFriendMapper;

	public boolean Add(String deviceSn, String pdeviceSn) {
		int result = PFriendMapper.Add(deviceSn, pdeviceSn);
		return result > 0;
	}

	public List<PFriend> getFriendlist(String deviceSn) {
		return PFriendMapper.getFriendlist(deviceSn);
	}

	public boolean isFriend(String deviceSn, String pdeviceSn) {
		return PFriendMapper.isFriend(deviceSn, pdeviceSn)>0;
	}

	public boolean deleteAllFriend(String deviceSn) {
		int result = PFriendMapper.deleteAllFriend(deviceSn);
		return result > 0;
	}

	public boolean removeFriend(String deviceSn, String pdeviceSn) {
		int result = PFriendMapper.removeFriend(deviceSn, pdeviceSn);
		return result > 0;
	}
}
