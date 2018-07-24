package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.GroupUser;
import com.htdz.def.view.GroupUserInfo;

public interface GroupUserMapper {

	// @Select("select w_code as wCode,temperature from GroupUser where
	// city_code=#{0} order by w_time desc limit 1" )

	@Select("SELECT g.`name`,g.`type`,g.`device_sn`, "
			+ "CASE WHEN g.`type` = '0' THEN CONCAT('http://',B.`conn_name`,':',B.`conn_port`,u.`portrait`) "
			+ "WHEN g.`type` = '1' THEN CONCAT('http://',C.`conn_name`,':',C.`conn_port`,td.`head_portrait`) "
			+ "END AS portrait, "
			+ "CASE WHEN g.`type` = '0' THEN u.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS nickname, "
			+ "CASE WHEN g.`type` = '0' THEN du.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS remark "
			+ "FROM `group_user` AS g " + "LEFT JOIN `TUser` AS u ON u.`name`=g.`name` "
			+ "LEFT JOIN `TDevice` AS td ON td.`device_sn` =g.`device_sn` "
			+ "LEFT JOIN `TConnServer` AS B ON u.`conn_country`=B.`conn_country` AND B.`conn_device`=2 AND B.`conn_type`=2 "
			+ "LEFT JOIN `TConnServer` AS C ON td.`conn_country`=C.`conn_country` AND C.`conn_device`=2 AND C.`conn_type`=2 "
			+ "LEFT JOIN `TDeviceUser` AS du ON du.`name`=g.`name` AND g.`device_sn`=du.`device_sn` WHERE g.`device_sn`=#{0} ")
	public List<GroupUserInfo> getGroupUserInfo(String deviceSn);

	@Select("SELECT g.`name`,g.`type`,g.`device_sn`, "
			+ "CASE WHEN g.`type` = '0' THEN CONCAT('http://',B.`conn_name`,':',B.`conn_port`,u.`portrait`) "
			+ "WHEN g.`type` = '1' THEN CONCAT('http://',C.`conn_name`,':',C.`conn_port`,td.`head_portrait`) "
			+ "END AS portrait, "
			+ "CASE WHEN g.`type` = '0' THEN u.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS nickname, "
			+ "CASE WHEN g.`type` = '0' THEN du.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS remark "
			+ "FROM `group_user` AS g " + "LEFT JOIN `TUser` AS u ON u.`name`=g.`name` "
			+ "LEFT JOIN `TDevice` AS td ON td.`device_sn` =g.`device_sn` "
			+ "LEFT JOIN `TConnServer` AS B ON u.`conn_country`=B.`conn_country` AND B.`conn_device`=2 AND B.`conn_type`=2 "
			+ "LEFT JOIN `TConnServer` AS C ON td.`conn_country`=C.`conn_country` AND C.`conn_device`=2 AND C.`conn_type`=2 "
			+ "LEFT JOIN `TDeviceUser` AS du ON du.`name`=g.`name` AND g.`device_sn`=du.`device_sn` "
			+ "WHERE g.`device_sn`=#{0} AND g.`name`=#{1}")
	public GroupUserInfo getGroupUserByName(String deviceSn, String username);

	@Select("select * from GroupUser")
	public List<GroupUser> selectAll();

}
