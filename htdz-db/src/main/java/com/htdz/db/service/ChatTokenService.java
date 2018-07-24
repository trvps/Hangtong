package com.htdz.db.service;


import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.ChatTokenMapper;
import com.htdz.def.dbmodel.ChatToken;


@Service
@MapperScan("com.htdz.db.mapper")
public class ChatTokenService {
	@Autowired
	private ChatTokenMapper chatTokenMapper;
	
	public ChatToken select(String name) {
		return chatTokenMapper.select(name);
	}
	
	public List<ChatToken> selectAll() {
		return chatTokenMapper.selectAll();
	}
	
	public List<ChatToken> selectPage(int pageIndex, int pageCount) {
		Page<ChatToken> page = PageHelper.startPage(pageIndex, pageCount); 
		chatTokenMapper.selectAll();
		return page;
	}
	
	public boolean add(ChatToken chatToken) {
		int result = chatTokenMapper.add(chatToken);
		return result == 1 ? true : false;
	}
	
	public boolean modify(ChatToken chatToken) {
		int result = chatTokenMapper.modify(chatToken);
		return result == 1 ? true : false;
	}
}



