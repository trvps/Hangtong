package com.htdz.litefamily;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.litefamily.service.RemindServerManager;

@Service
public class LitefamilyEngine {
	@Autowired
	private RemindServerManager remindServerManager;

	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");
		remindServerManager.load();
	}
}
