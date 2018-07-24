package com.htdz.reg;

import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;

@Service
public class RegEngine {

	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");
	}
}
