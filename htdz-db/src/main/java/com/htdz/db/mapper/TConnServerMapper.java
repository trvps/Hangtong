package com.htdz.db.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.TConnServer;

public interface TConnServerMapper {

	@Select("SELECT cs.`conn_country`,cs.`connid`,cs.`conn_name`,cs.`conn_ext` AS conn_dns,cs.`conn_port` FROM `TConnServer` AS cs INNER JOIN `TUser` AS u ON u.`conn_country`=cs.`conn_country` AND cs.`conn_device`=2 AND cs.`conn_type`=2 WHERE u.`name`=#{0}")
	public TConnServer getByUsername(String DeviceSn);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public Map<String, Object> select(String sql);
}
