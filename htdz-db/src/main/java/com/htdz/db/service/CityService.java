package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.CityMapper;
import com.htdz.def.dbmodel.City;

@Service
@MapperScan("com.htdz.db.mapper")
public class CityService {
	@Autowired
	private CityMapper cityMapper;

	public City selectByMapCode(String mapCode) {
		return cityMapper.selectByMapCode(mapCode);
	}

	public List<City> selectAll() {
		return cityMapper.selectAll();
	}

	public List<City> selectPage(int pageIndex, int pageCount) {
		Page<City> page = PageHelper.startPage(pageIndex, pageCount);
		cityMapper.selectAll();
		return page;
	}

	public boolean add(City city) {
		int result = cityMapper.add(city);
		return result == 1 ? true : false;
	}

	public boolean modify(City city) {
		int result = cityMapper.modify(city);
		return result == 1 ? true : false;
	}
}
