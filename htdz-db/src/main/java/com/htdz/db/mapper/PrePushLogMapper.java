package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.PrePushLog;

public interface PrePushLogMapper {

	@Select("select * from pre_push_log where city_code=#{0} order by w_time desc limit 1")
	public PrePushLog select(String cityCode);

	@Select("select * from pre_push_log")
	public List<PrePushLog> selectAll();

	@Insert("insert into pre_push_log(device_sn, msg, msg_type, push_user, push_state, create_time, over_time) values(#{deviceSn}, #{msg}, #{msgType}, #{pushUser}, #{pushState}, #{createTime}, #{overTime})")
	@Options(useGeneratedKeys = true)
	public int add(PrePushLog PrePushLog);

	@Update("update pre_push_log set push_state=#{pushState},over_time=#{overTime}  where id=#{id}")
	public int modify(PrePushLog PrePushLog);

}
