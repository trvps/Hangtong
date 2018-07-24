package com.htdz.db.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.User;
import com.htdz.def.view.UserConn;

public interface UserMapper {
	@Select("select * from user where name=#{0}")
	public User selectUser(String name);

	@Select("select * from user")
	public List<User> selectAllUser();

	@Insert("insert into user(name) values(#{name})")
	@Options(useGeneratedKeys = true)
	public int addUser(User user);

	@Update("update user set name=#{name} where id=#{id}")
	public int modifyUser(User user);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	// @InsertProvider @UpdateProvider @DeleteProvider
	public User selectUserBySql(String sql);

	@Select("SELECT u.`name`, u.`timezone`, d.`device_sn` as deviceSn, d.`ranges`, d.`conn_country` as connCountry, u.`isCustomizedApp`, ut.`token`, ut.`versions`, ut.`certificate`  FROM `TUser` AS u INNER JOIN `TDeviceUser` AS du ON u.id=du.uid INNER JOIN `TDevice` AS d ON d.`id`=du.`did`INNER JOIN `TConnServer` AS cs ON u.`connid`=cs.`connid` AND cs.`conn_device`=2 AND cs.`conn_type`=2 left join  UserToken as ut on ut.`name`=u.`name` WHERE d.`device_sn`=#{0}")
	public List<UserConn> getUserDeviceInfo(String deviceSn);

	@Select("SELECT d.`device_sn` AS `name`, d.`timezone`, d.`device_sn` AS deviceSn,d.`ranges`,d.`conn_country` AS connCountry,ut.`token`,ut.`versions` FROM `TDevice` AS d LEFT JOIN  UserToken AS ut ON ut.`name`= #{0} WHERE d.`device_sn`=#{0}")
	public UserConn getDeviceAsUserInfo(String deviceSn);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public List<Map<String, Object>> select(String sql);
}
