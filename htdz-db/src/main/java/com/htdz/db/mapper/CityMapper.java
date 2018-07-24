package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.City;

public interface CityMapper {

	@Select("select map_code as mapCode,weather_code as weatherCode from city where map_code = #{0}")
	public City selectByMapCode(String mapCode);

	@Select("select * from city")
	public List<City> selectAll();

	@Insert("insert into city(name, token) values(#{name}, #{token})")
	@Options(useGeneratedKeys = true)
	public int add(City city);

	@Update("update city set name=#{name}, age=#{age} where id=#{id}")
	public int modify(City city);
}
