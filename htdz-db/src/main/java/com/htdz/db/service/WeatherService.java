package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.WeatherMapper;
import com.htdz.def.dbmodel.Weather;

@Service
@MapperScan("com.htdz.db.mapper")
public class WeatherService {
	@Autowired
	private WeatherMapper weatherMapper;

	public Weather selectByCityCode(String CityCode, String Date) {
		return weatherMapper.selectByCityCode(CityCode, Date);
	}

	public List<Weather> selectAll() {
		return weatherMapper.selectAll();
	}

	public List<Weather> selectPage(int pageIndex, int pageCount) {
		Page<Weather> page = PageHelper.startPage(pageIndex, pageCount);
		weatherMapper.selectAll();
		return page;
	}

	public boolean add(Weather weather) {
		int result = weatherMapper.add(weather);
		return result == 1 ? true : false;
	}
}
