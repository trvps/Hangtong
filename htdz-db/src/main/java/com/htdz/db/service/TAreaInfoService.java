package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.TAreaInfoMapper;
import com.htdz.def.dbmodel.TareaInfo;

@Service
@MapperScan("com.htdz.db.mapper")
public class TAreaInfoService {
	@Autowired
	TAreaInfoMapper tAreaInfoMapper;
	
	public List<TareaInfo> getAreaListById(Integer did)
	{
		return tAreaInfoMapper.getAreaListById(did);
	}
}
