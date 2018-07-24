package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.PushLog;

public interface PushLogMapper {

	@Select("select * from push_log where city_code=#{0} order by w_time desc limit 1")
	public PushLog select(String cityCode);

	@Select("select * from push_log")
	public List<PushLog> selectAll();

	@Insert("insert into push_log(device_sn,msg,msg_type,push_user,phone_type,is_push,create_time) values(#{deviceSn}, #{msg}, #{msgType},#{pushUser}, #{phoneType}, #{isPush}, #{createTime})")
	@Options(useGeneratedKeys = true)
	public int add(PushLog pushlog);

	@Update("update push_log set is_push=#{isPush},update_time=#{updateTime}  where id=#{id}")
	public int modify(PushLog pushlog);

}
