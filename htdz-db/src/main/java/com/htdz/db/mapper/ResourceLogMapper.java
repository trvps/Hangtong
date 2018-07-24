package com.htdz.db.mapper;


import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.ResourceLog;


public interface ResourceLogMapper {

	@Select("select w_code as wCode,temperature from resource_log where city_code=#{0} order by w_time desc limit 1" )
	public ResourceLog select(String cityCode);
	
	@Select("select * from resource_log")
	public List<ResourceLog> selectAll();
	
	@Insert("insert into resource_log(name, address, type, creat_time) values(#{name}, #{address}, #{type}, #{creatTime})")
	@Options(useGeneratedKeys = true)
	public int add(ResourceLog ResourceLog);
	
	@Update("update resource_log set name=#{name}, age=#{age} where id=#{id}")
	public int modify(ResourceLog ResourceLog);
}

