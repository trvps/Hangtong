package com.htdz.liteguardian;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.db.service.UserService;

@Service
public class LiteguardianEngine {
	@Autowired
	private UserService userService;

	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");

		// User user = userService.selectUserBySql("select * from user where
		// id=1");
		// if (user != null)
		// System.out.println(user);
	}
}
