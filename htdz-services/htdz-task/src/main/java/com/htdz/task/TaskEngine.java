package com.htdz.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.task.service.RailService;

@Service
public class TaskEngine {
	@Autowired
	private RailService railService;

	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----初始化 围栏分析线程----------");

		// 围栏分析线程
		railService.onContextInitCompleted();
	}

}
