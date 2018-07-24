package com.htdz.reg.util;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.db.mapper.DaoMapper;

@Service
@MapperScan("com.htdz.db.mapper")
public class DaoUtil {

	@Autowired
	private DaoMapper daoMapper;

	@Autowired
	private Environment env;

	/**
	 * 
	 * @param username
	 */
	public List<Map<String, Object>> getServerConnInfoByUser(String username) {
		String sql = "SELECT u.`name`,u.`conn_country`,cs.`connid`,cs.`conn_name`,cs.`conn_ext` as conn_dns,cs.`conn_port` FROM `TUser` AS u "
				+ "INNER JOIN `TConnServer` AS cs ON u.`conn_country`=cs.`conn_country` AND cs.`conn_device`=2 "
				+ "AND cs.`conn_type`=2 WHERE u.`name`='" + username + "'";
		return daoMapper.selectList(sql);
	}

	/**
	 * 
	 * @param serverNo
	 */
	public List<Map<String, Object>> getServerConnInfoByServerNo(String serverNo) {
		String sql = "SELECT `connid`,`conn_name`,`conn_ext` as conn_dns,`conn_port`,`conn_country` FROM `TConnServer` WHERE `conn_device`=2 AND `conn_type`=2 AND `connid`="
				+ serverNo;
		return daoMapper.selectList(sql);
	}

	/**
	 * 
	 */
	public List<Map<String, Object>> getServerConnInfo() {
		String sql = "SELECT `connid`,`conn_name`,`conn_ext` as conn_dns,`conn_port`,`conn_country` FROM `TConnServer` WHERE `conn_device`=2 AND `conn_type`=2 ";
		return daoMapper.selectList(sql);
	}

	public List<Map<String, Object>> getServerConnInfoByCountry(String Country) {
		String sql = "SELECT `connid`,`conn_name`,`conn_ext` as conn_dns,`conn_port`,`conn_country` FROM `TConnServer` WHERE `conn_device`=2 AND `conn_type`=2  AND `conn_country`= "
				+ "'" + Country + "'";
		return daoMapper.selectList(sql);
	}

	/**
	 * 检查用户
	 * @param username
	 * @param serverno
	 * @return
	 */
	public boolean checkUserExist(String username) {
		try {
			String sqlselectuser = "SELECT * FROM `TUser` WHERE `name`='" + username + "'";
			List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
			if (list != null && list.size() >= 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 注册用户
	 * @param username
	 * @param serverno
	 * @return
	 */
	public boolean registerUser(String username, int serverno, String timezone, String timezoneid,
			String isCustomizedApp) {
		String curcountry = env.getProperty("default.country");

		String sqlselctcountry = "SELECT `conn_country` FROM `TConnServer` WHERE `connid`=" + serverno;
		Object res = daoMapper.findBy(sqlselctcountry);
		if (res != null) {
			curcountry = res.toString();
		}

		String sqlselectuser = "SELECT * FROM `TUser` WHERE `name`='" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			return false;
		} else {
			String sql = "INSERT INTO TUser(`name`,`conn_country`,`timezone`,`timezone_id`,`isCustomizedApp`) VALUES('"
					+ username + "','" + curcountry + "','" + timezone + "','" + timezoneid + "','" + isCustomizedApp
					+ "')";
			try {
				return daoMapper.modify(sql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
			}
			return false;
		}
	}

	/**
	 * 绑定用户
	 */
	public boolean trackerBindUser(String deviceSn, String username, String protocoltype, String product_type,
			String enabled, String expired_time, String expired_time_de, String ranges, String tracker_sim,
			String nickname, int aroundRanges) {
		String curcountry = env.getProperty("default.country");

		String sqlselctcountry = "SELECT `conn_country` FROM `TUser` WHERE `name`='" + username + "'";
		Object res = daoMapper.findBy(sqlselctcountry);
		if (res != null) {
			curcountry = res.toString();
		}
		if (expired_time_de.equals("")) {
			expired_time_de = null;
		}
		String sqlselectdevice = "SELECT * FROM `TDevice` WHERE `device_sn`='" + deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			return false;
		} else {
			String sql = "INSERT INTO `TDevice`(`device_sn`,`conn_country`,`protocol_type`,`product_type`,`enabled`,expired_time,`expired_time_de`,`ranges`,`tracker_sim`,`nickname`,`around_ranges`) VALUES('"
					+ deviceSn + "','" + curcountry + "','" + protocoltype + "','" + product_type + "','" + enabled
					+ "','" + expired_time + "','" + expired_time_de + "','" + ranges + "','" + tracker_sim + "','"
					+ nickname + "','" + aroundRanges + "')";
			String sqluserdevice = "INSERT INTO `TDeviceUser`(`device_sn`,`name`,`is_super_user`) VALUES ('" + deviceSn
					+ "','" + username + "',1)";
			try {
				if (ranges.equals("6")) {
					sql = "INSERT INTO `TDevice`(`device_sn`,`conn_country`,`protocol_type`,`product_type`,`enabled`,expired_time,`expired_time_de`,`ranges`,`tracker_sim`,`nickname`,`around_ranges`,`defensive`)"
							+ " VALUES('" + deviceSn + "','" + curcountry + "','" + protocoltype + "','" + product_type
							+ "','" + enabled + "','" + expired_time + "','" + expired_time_de + "','" + ranges + "','"
							+ tracker_sim + "','" + nickname + "','" + aroundRanges + "',1)";
					daoMapper.modify(sql);
				} else {
					daoMapper.modify(sql);
				}
				return daoMapper.modify(sqluserdevice) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	/**
	 * 删除设备
	 */
	public boolean deletedevice(String deviceSn) {
		String sqlselectdevice = "SELECT * FROM `TDevice` WHERE `device_sn` IN (" + deviceSn + ")";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			String sqldeletedevicebind = "DELETE FROM `TDeviceUser` WHERE `device_sn` IN (" + deviceSn + ")";
			String sqldeletedevice = "DELETE FROM `TDevice` WHERE `device_sn` IN (" + deviceSn + ")";
			try {
				daoMapper.modify(sqldeletedevicebind);
				return daoMapper.modify(sqldeletedevice) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * 删除用户
	 */
	public boolean deleteuser(String username) {
		String sqlselectuser = "SELECT * FROM `TUser` WHERE `name` IN (" + username + ")";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			String sqldeleteuser = "DELETE FROM `TUser` WHERE `name` IN (" + username + ")";
			try {
				return daoMapper.modify(sqldeleteuser) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * 是否存在已经绑定超级用户的用户名和设备
	 */
	public boolean issuperuser(String userName, String deviceSn) {
		String sql = "SELECT COUNT(*) FROM `TDeviceUser` WHERE `is_super_user`=1 AND `device_sn`='" + deviceSn
				+ "' AND `name`='" + userName + "'";

		Object obj = daoMapper.findBy(sql);
		if (obj == null || Integer.valueOf(obj.toString()) == 0)
			return false;
		return true;
	}

	/**
	 * 获取设备所在服务器信息
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> getdeviceserverandusername(String deviceSn) {
		// add by ljl 20160505 获取conn_port端口信息
		String sql = "SELECT A.`conn_name`,A.`conn_port`,C.`name` FROM `TConnServer` AS A "
				+ "INNER JOIN `TDevice` AS B ON A.`conn_country`=B.`conn_country` "
				+ "AND A.`conn_device`=2 AND A.`conn_type`=2 INNER JOIN `TDeviceUser` "
				+ "AS C ON B.`device_sn`=C.`device_sn` AND C.`is_super_user`=1 AND C.`device_sn`='" + deviceSn + "'";

		return daoMapper.select(sql);
	}

	/**
	 * 获取授权用户相关信息
	 * @param deviceSn
	 * @param username
	 * @return
	 */
	public Map<String, Object> getauthorizationuserinfo(String deviceSn, String username) {
		String sql = "SELECT A.`name`,A.`conn_country`,B.`conn_name`,"
				+ "(SELECT COUNT(*) FROM TDeviceUser WHERE `name`='" + username + "') AS userbinddevice_count,"
				+ "(SELECT COUNT(*) FROM TDeviceUser WHERE `device_sn`='" + deviceSn + "') AS devicebinduser_count,"
				+ "(SELECT COUNT(*) FROM TDeviceUser WHERE `device_sn`='" + deviceSn + "' AND `name`='" + username
				+ "') AS userhavebinddevice_count "
				+ "FROM `TUser` AS A INNER JOIN `TConnServer` AS B ON A.`conn_country`=B.`conn_country` "
				+ "AND B.`conn_device`=2 AND B.`conn_type`=2 WHERE A.`name`='" + username + "'";

		return daoMapper.select(sql);
	}

	/**
	 * 授权用户
	 */
	public boolean authorization(String deviceSn, String username, String nickname, int isGps) {
		String sqlselectdevice = "SELECT * FROM `TDevice` WHERE `device_sn`='" + deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list == null || list.size() < 1) {
			return false;
		} else {
			String sqluserdevice = "INSERT INTO `TDeviceUser`(`device_sn`,`name`,`is_super_user`,`nickname`,is_gps) VALUES ('"
					+ deviceSn + "','" + username + "',0,'" + nickname + "'," + isGps + ")";
			try {
				return daoMapper.modify(sqluserdevice) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	/**
	 * 授权用户
	 */
	public boolean authorizationRemote(String deviceSn, String username, String nickname, int isGps) {

		String sqluserdevice = "INSERT INTO `TDeviceUser`(`device_sn`,`name`,`is_super_user`,`nickname`,is_gps) VALUES ('"
				+ deviceSn + "','" + username + "',0,'" + nickname + "'," + isGps + ")";
		try {
			return daoMapper.modify(sqluserdevice) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}

	}

	/**
	 * 取消授权
	 */
	public boolean unauthorization(String deviceSn, String username) {
		String sqlselectdevice = "SELECT * FROM `TDeviceUser` WHERE `device_sn`='" + deviceSn + "' AND `name`='"
				+ username + "' AND `is_super_user`=0";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			String sqluserdevice = "DELETE FROM `TDeviceUser` WHERE `device_sn`='" + deviceSn + "' AND `name`='"
					+ username + "' AND `is_super_user`=0";
			try {
				return daoMapper.modify(sqluserdevice) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * 更新设备信息
	 * @param nickname
	 * @param defensive
	 * @param head_portrait
	 * @param gps_interval
	 * @param bt_enable
	 * @param cdt_enable
	 * @param deviceSn
	 * @return
	 */
	public boolean updatedeviceinfo(String nickname, String defensive, String head_portrait, String gps_interval,
			String bt_enable, String cdt_enable, String tracker_sim, String ranges, String deviceSn, int aroundRanges) {
		if (nickname == null && defensive == null && head_portrait == null && gps_interval == null && bt_enable == null
				&& cdt_enable == null && tracker_sim == null && ranges == null) {
			return false;
		}

		String sqlselectdevice = "SELECT * FROM `TDevice` WHERE `device_sn`='" + deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			String sqlupdatedevice = "UPDATE `TDevice` SET ";
			String sqlparam = "";
			if (nickname != null && !nickname.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `nickname`='" + nickname + "'";
				} else {
					sqlparam += " ,`nickname`='" + nickname + "'";
				}
			}

			if (defensive != null && !defensive.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `defensive`=" + defensive;
				} else {
					sqlparam += " ,`defensive`=" + defensive;
				}
			}

			if (head_portrait != null && !head_portrait.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `head_portrait`='" + head_portrait + "'";
				} else {
					sqlparam += " ,`head_portrait`='" + head_portrait + "'";
				}
			}

			if (gps_interval != null && !gps_interval.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `gps_interval`=" + gps_interval;
				} else {
					sqlparam += " ,`gps_interval`=" + gps_interval;
				}
			}

			if (bt_enable != null && !bt_enable.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `bt_enable`=" + bt_enable;
				} else {
					sqlparam += " ,`bt_enable`=" + bt_enable;
				}
			}

			if (cdt_enable != null && !cdt_enable.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `cdt_enable`='" + cdt_enable + "'";
				} else {
					sqlparam += " ,`cdt_enable`='" + cdt_enable + "'";
				}
			}

			if (tracker_sim != null && !tracker_sim.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `tracker_sim`='" + tracker_sim + "'";
				} else {
					sqlparam += " ,`tracker_sim`='" + tracker_sim + "'";
				}
			}

			if (ranges != null && !ranges.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `ranges`=" + ranges;
				} else {
					sqlparam += " ,`ranges`=" + ranges;
				}
			}

			if (aroundRanges > 0) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `around_ranges`=" + aroundRanges;
				} else {
					sqlparam += " ,`around_ranges`=" + aroundRanges;
				}
			}

			sqlupdatedevice += sqlparam + " WHERE `device_sn`='" + deviceSn + "'";
			try {
				if (!sqlparam.isEmpty()) {
					return daoMapper.modify(sqlupdatedevice) > 0;
				} else {
					return false;
				}
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * 获取用户所有设备信息
	 */
	public List<Map<String, Object>> getDeviceInfo(String username) {
		String sqlselectdevice = "SELECT A.`device_sn`,A.`tracker_sim`,A.`nickname`,A.`ranges`,A.`product_type`,"
				+ "A.`defensive`,A.`head_portrait`,A.`gps_interval`,A.`expired_time`,A.`bt_enable`,A.`cdt_enable`,"
				// + "CASE WHEN A.expired_time < UTC_TIMESTAMP() THEN 'true'
				// ELSE 'false' END AS expired,"
				// + "CASE WHEN A.expired_time < DATE_ADD(UTC_TIMESTAMP(),
				// INTERVAL 7 DAY) THEN 'true' ELSE 'false' END AS
				// one_month_expired,A.`protocol_type`,"
				+ "CASE WHEN A.expired_time_de < UTC_TIMESTAMP() THEN 'true' ELSE 'false' END AS expired,"
				+ "CASE WHEN A.expired_time_de < DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 DAY) THEN 'true' ELSE 'false' END AS one_month_expired,A.`protocol_type`,"
				+ "D.`conn_name`,D.`conn_port`,DU.`name` AS super_user,C.`timezone`,B.`is_gps` FROM `TDevice` AS A "
				+ "INNER JOIN `TDeviceUser` AS B ON A.`device_sn`=B.`device_sn` "
				+ "INNER JOIN `TUser` AS C ON B.`name`= C.`name` "
				+ "INNER JOIN `TConnServer` AS D ON D.`conn_device`=2 AND D.`conn_type`=2 AND A.`conn_country`=D.`conn_country` "
				+ "LEFT JOIN `TDeviceUser` AS DU ON A.`device_sn`=DU.`device_sn` AND DU.`is_super_user`=1 "
				+ "WHERE C.`name` = '" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 设置用户时区
	 * @param username
	 * @param timezone
	 * @param timezoneId
	 * @param timezoneCheck
	 * @return
	 */
	public boolean settimezone(String username, String timezone, String timezoneId, String timezoneCheck) {
		String sqlselectuser = "SELECT * FROM `TUser` WHERE `name`='" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			String sqlupdateuser = "UPDATE `TUser` SET `timezone`=" + timezone + ",`timezone_id`=" + timezoneId
					+ ",`timezone_check`=" + timezoneCheck + " WHERE `name`='" + username + "'";
			try {
				return daoMapper.modify(sqlupdateuser) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * 超级用户查看某个追踪器绑定的其他用户
	 */
	public List<Map<String, Object>> getTrackerUser(String deviceSn) {
		// String sql = "SELECT `name`,`nickname`,`is_gps` as isGps FROM
		// TDeviceUser WHERE `device_sn` = '"+deviceSn+"' AND is_super_user =
		// 0";

		String sql = "SELECT D.`name`,D.`nickname`,D.`is_gps` AS isGps,CONCAT('http://',C.`conn_name`,':',C.`conn_port`,u.`portrait`) AS portrait FROM TDeviceUser D "
				+ "LEFT JOIN `TUser` AS u ON u.`name`=D.`name` "
				+ "LEFT JOIN `TDevice` AS td ON td.`device_sn`=D.`device_sn` "
				+ "LEFT JOIN `TConnServer` AS C ON u.`conn_country`=C.`conn_country` "
				+ "AND C.`conn_device`=2 AND C.`conn_type`=2 " + "WHERE D.`device_sn` = '" + deviceSn
				+ "' AND is_super_user = 0";

		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 获取设备授权用户信息(推送)
	 */
	public List<Map<String, Object>> getAuthorizationUserInfo(String deviceSn, String localIp) {
		String sql = "SELECT DISTINCT C.`conn_name` FROM `TDeviceUser` AS A "
				+ "INNER JOIN `TUser` AS B ON A.`name`=B.`name` AND A.`is_super_user`=0 "
				+ "INNER JOIN `TConnServer` AS C ON B.`conn_country`=C.`conn_country` "
				+ "AND C.`conn_device`=2 AND C.`conn_type`=2 WHERE A.`device_sn`='" + deviceSn
				+ "' AND C.`conn_name`!='" + localIp + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 获取设备信息(推送)
	 */
	public List<Map<String, Object>> getUserDeviceListByDeviceId(String deviceSn, String localIp) {
		String sql = "SELECT u.`name`,u.`timezone`,d.`device_sn`,d.`ranges`,d.`conn_country`,"
				+ "u.`isCustomizedApp`,d.`defensive` FROM `TUser` AS u "
				+ "INNER JOIN `TDeviceUser` AS du ON u.`name`=du.`name` "
				+ "INNER JOIN `TDevice` AS d ON d.`device_sn`=du.`device_sn` "
				+ "INNER JOIN `TConnServer` AS cs ON u.`conn_country`=cs.`conn_country` "
				+ "AND cs.`conn_device`=2 AND cs.`conn_type`=2 " + "WHERE cs.`conn_name`='" + localIp
				+ "' AND du.`device_sn`='" + deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 保存用户TOKEN
	 */
	public boolean saveUserToken(String username, String token, int versions) {
		// 根据用户名删除数据
		String deleteByUsername = "DELETE FROM UserToken WHERE `name`='" + username + "'";
		// 根据token删除数据
		String deleteByToken = "DELETE FROM UserToken WHERE `token`='" + token + "'";

		int result = 0;
		String insertUserToken = "INSERT INTO `UserToken`(`name`,`token`,`versions`) VALUES ('" + username + "','"
				+ token + "','" + versions + "')";
		try {
			daoMapper.modify(deleteByUsername);
			daoMapper.modify(deleteByToken);
			if (token == null || token.equals("")) {
				result = 1;
			} else {
				result = daoMapper.modify(insertUserToken);
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return result > 0;
	}

	/**
	 * 获取用户TOKEN
	 */
	public List<Map<String, Object>> getUserToken(String username) {
		String sql = "SELECT `token`,`versions` FROM `UserToken` WHERE name='" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	// #############################################################
	// 航通守护者国内3.0版专用
	/**
	 * 获取用户所有设备信息
	 */
	public List<Map<String, Object>> getDeviceInfoCN(String username) {
		String sqlselectdevice = "SELECT A.`device_sn`,A.`tracker_sim`,A.`nickname`,A.`ranges`,A.`product_type`,"
				+ "A.`defensive`,A.`head_portrait`,A.`gps_interval`,A.`expired_time`,A.`expired_time_de`,A.`bt_enable`,A.`cdt_enable`,A.`around_ranges`,"
				// + "CASE WHEN A.expired_time < UTC_TIMESTAMP() THEN 'true'
				// ELSE 'false' END AS expired,"
				// + "CASE WHEN A.expired_time < DATE_ADD(UTC_TIMESTAMP(),
				// INTERVAL 7 DAY) THEN 'true' ELSE 'false' END AS
				// one_month_expired,"
				+ "CASE WHEN (A.expired_time < UTC_TIMESTAMP() or A.expired_time is null) THEN 'true' ELSE 'false' END AS expiredTime,"
				+ "CASE WHEN (A.expired_time_de < UTC_TIMESTAMP() or A.expired_time_de is null) THEN 'true' ELSE 'false' END AS expiredTimeDe,"
				+ "CASE WHEN A.expired_time_de < DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY) THEN 'true' ELSE 'false' END AS one_month_expired,A.`disable`,"
				+ "A.`protocol_type`,D.`conn_name`,D.`conn_port`,DU.`name` AS super_user,C.`timezone`,B.`is_gps`,g.`device_sn` AS isExistGroup FROM `TDevice` AS A "
				+ "INNER JOIN `TDeviceUser` AS B ON A.`device_sn`=B.`device_sn` "
				+ "INNER JOIN `TUser` AS C ON B.`name`= C.`name` "
				+ "INNER JOIN `TConnServer` AS D ON D.`conn_device`=2 AND D.`conn_type`=2 AND A.`conn_country`=D.`conn_country` "
				+ "LEFT JOIN `TDeviceUser` AS DU ON A.`device_sn`=DU.`device_sn` AND DU.`is_super_user`=1 "
				+ "LEFT JOIN `group_user` g ON A.`device_sn`=g.`device_sn` AND g.`name`= '" + username + "' "
				+ "WHERE C.`name` = '" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 获取设备信息(通过设备)
	 */
	public List<Map<String, Object>> getDeviceInfoByDevice(String device) {
		String sqlselectdevice = "SELECT A.`device_sn`,A.`tracker_sim`,A.`nickname`,A.`ranges`,A.`product_type`,"
				+ "A.`defensive`,A.`head_portrait`,A.`gps_interval`,A.`expired_time`,A.`expired_time_de`,A.`bt_enable`,A.`cdt_enable`,A.`around_ranges`,"
				// + "CASE WHEN A.expired_time < UTC_TIMESTAMP() THEN 'true'
				// ELSE 'false' END AS expired,"
				// + "CASE WHEN A.expired_time < DATE_ADD(UTC_TIMESTAMP(),
				// INTERVAL 7 DAY) THEN 'true' ELSE 'false' END AS
				// one_month_expired,"
				+ "CASE WHEN (A.expired_time < UTC_TIMESTAMP() or A.expired_time is null) THEN 'true' ELSE 'false' END AS expiredTime,"
				+ "CASE WHEN (A.expired_time_de < UTC_TIMESTAMP() or A.expired_time_de is null) THEN 'true' ELSE 'false' END AS expiredTimeDe,"
				+ "CASE WHEN A.expired_time_de < DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY) THEN 'true' ELSE 'false' END AS one_month_expired,A.`disable`,"
				+ "A.`protocol_type`,D.`conn_name`,D.`conn_port`,DU.`name` AS super_user,C.`timezone`,B.`is_gps`,g.`device_sn` AS isExistGroup FROM `TDevice` AS A "
				+ "INNER JOIN `TDeviceUser` AS B ON A.`device_sn`=B.`device_sn` "
				+ "INNER JOIN `TUser` AS C ON B.`name`= C.`name` "
				+ "INNER JOIN `TConnServer` AS D ON D.`conn_device`=2 AND D.`conn_type`=2 AND A.`conn_country`=D.`conn_country` "
				+ "LEFT JOIN `TDeviceUser` AS DU ON A.`device_sn`=DU.`device_sn` AND DU.`is_super_user`=1 "
				+ "LEFT JOIN `group_user` g ON A.`device_sn`=g.`device_sn` " + "WHERE  A.`device_sn` IN (" + device
				+ ") GROUP BY A.`device_sn`";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectdevice);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 获取被远程授权的设备
	 */
	public List<Map<String, Object>> getDeviceUserNotINDevice(String username) {
		String sql = "SELECT `device_sn` FROM `TDeviceUser` WHERE device_sn NOT IN(SELECT device_sn FROM `TDevice`) "
				+ "AND `name` = '" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 检查用户
	 * @param username
	 * @param serverno
	 * @return
	 */
	public boolean checkUserExistCN(String username) {
		try {
			String sqlselectuser = "SELECT * FROM `TUser` WHERE `name`='" + username + "' OR `mobile`='" + username
					+ "'";
			List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
			if (list != null && list.size() >= 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 注册用户
	 * @param username
	 * @param serverno
	 * @return
	 */
	public boolean registerUserCN(String username, int serverno, String timezone, String timezoneid,
			String isCustomizedApp) {
		String curcountry = env.getProperty("default.country");

		String sqlselctcountry = "SELECT `conn_country` FROM `TConnServer` WHERE `connid`=" + serverno;
		Object res = daoMapper.findBy(sqlselctcountry);
		if (res != null) {
			curcountry = res.toString();
		}

		String sqlselectuser = "SELECT * FROM `TUser` WHERE `name`='" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			return false;
		} else {
			String sql = "INSERT INTO TUser(`conn_country`,`timezone`,`timezone_id`,`isCustomizedApp`,name) VALUES('"
					+ curcountry + "','" + timezone + "','" + timezoneid + "','" + isCustomizedApp + "','" + username
					+ "')";
			try {
				return daoMapper.modify(sql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
			}
			return false;
		}
	}

	public int modifyAccountRemark(String authorizedname, String deviceSn, String nickname, int isGps) {
		// 修改备注名
		String sql = "UPDATE TDeviceUser SET nickname='" + nickname + "',is_gps='" + isGps + "' where name='"
				+ authorizedname + "' and device_sn='" + deviceSn + "' ";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/*
	 * 获取gps信息
	 */
	public Map<String, Object> getGpsInfo(String deviceSn, String username) {
		String sql = "SELECT `name`,`nickname`,`is_gps` as isGps FROM TDeviceUser WHERE `device_sn` = '" + deviceSn
				+ "' AND name ='" + username + "' AND is_gps=0";
		Map<String, Object> list = daoMapper.select(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	public int updateDeviceExpirationTime(String deviceSn, String expired_time_de, String expired_time,
			String disable) {
		// 修改设备到期时间
		try {
			String str = null;
			String sql = null;
			if (null != expired_time_de) {
				str = expired_time_de;
				sql = "UPDATE TDevice SET expired_time_de='" + str + "' where device_sn in (" + deviceSn + ")";

			} else if (null != expired_time) {
				str = expired_time;
				sql = "UPDATE TDevice SET expired_time='" + str + "' where device_sn in (" + deviceSn + ")";
			} else if (null != disable) {
				str = disable;
				sql = "UPDATE TDevice SET disable='" + str + "' where device_sn in (" + deviceSn + ")";
			}

			return daoMapper.modify(sql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	public int updateIsCustomizedApp(String username, int isCustomizedApp) {
		String sql = "UPDATE TUser SET isCustomizedApp='" + isCustomizedApp + "' where name='" + username + "'";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/**
	 * 增加群聊成员
	 */
	public int addGroupUser(String username, int type, String deviceSn) {

		String sql = "INSERT INTO `group_user`(`name`,type,`device_sn`) VALUES('" + username + "','" + type + "','"
				+ deviceSn + "')";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/**
	 * 保存群聊用户TOKEN
	 */
	public boolean saveChatToken(String username, String token) {
		// 根据用户名删除数据
		String deleteByUsername = "DELETE FROM `chat_token` WHERE `name`='" + username + "'";
		// 根据token删除数据
		String deleteByToken = "DELETE FROM `chat_token` WHERE `token`='" + token + "'";

		int result = 0;
		String insertUserToken = "INSERT INTO `chat_token`(`name`,`token`) VALUES ('" + username + "','" + token + "')";
		try {
			daoMapper.modify(deleteByUsername);
			daoMapper.modify(deleteByToken);
			if (token == null || token.equals("")) {
				result = 1;
			} else {
				result = daoMapper.modify(insertUserToken);
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return result > 0;
	}

	/**
	 * 获取群聊用户TOKEN
	 */
	public List<Map<String, Object>> getChatToken(String username) {
		String sql = "SELECT `token` FROM `chat_token` WHERE name='" + username + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 检查group_user
	 * @param username
	 * @param serverno
	 * @return
	 */
	public boolean checkGroupUser(String name, String deviceSn) {
		try {
			String sqlselectuser = "SELECT * FROM `group_user` WHERE `name`='" + name + "' AND device_sn='" + deviceSn
					+ "'";
			List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
			if (list != null && list.size() >= 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 查询全部聊天成员
	 * @param deviceSn
	 * @return
	 */
	public List<Map<String, Object>> getGroupUser(String deviceSn) {
		String sql = "SELECT g.`name`,g.`type`,g.`device_sn`, "
				+ "CASE WHEN g.`type` = '0' THEN CONCAT('http://',B.`conn_name`,':',B.`conn_port`,u.`portrait`) "
				+ "WHEN g.`type` = '1' THEN CONCAT('http://',C.`conn_name`,':',C.`conn_port`,td.`head_portrait`) "
				+ "END AS portrait, "
				+ "CASE WHEN g.`type` = '0' THEN u.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS nickname, "
				+ "CASE WHEN g.`type` = '0' THEN du.`nickname` WHEN g.`type` = '1' THEN td.`nickname` END AS remark "
				+ "FROM `group_user` AS g " + "LEFT JOIN `TUser` AS u ON u.`name`=g.`name` "
				+ "LEFT JOIN `TDevice` AS td ON td.`device_sn` =g.`device_sn` "
				+ "LEFT JOIN `TConnServer` AS B ON u.`conn_country`=B.`conn_country` AND B.`conn_device`=2 AND B.`conn_type`=2 "
				+ "LEFT JOIN `TConnServer` AS C ON td.`conn_country`=C.`conn_country` AND C.`conn_device`=2 AND C.`conn_type`=2 "
				+ "LEFT JOIN `TDeviceUser` AS du ON du.`name`=g.`name` AND g.`device_sn`=du.`device_sn` WHERE g.`device_sn`='"
				+ deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 查询单个聊天成员
	 * @param name
	 * @param deviceSn
	 * @return
	 */
	public List<Map<String, Object>> getGroupUser(String name, String deviceSn) {
		String sql = "SELECT g.`name`,g.`type`,g.`device_sn`, "
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
				+ "WHERE g.`device_sn`='" + deviceSn + "' AND g.`name` = '" + name + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 查询设备可邀请授权用户
	 * @param deviceSn
	 * @return
	 */
	public List<Map<String, Object>> getInviteAuthUser(String deviceSn) {
		String sql = "SELECT du.`name`,du.`nickname` AS remark,CONCAT('http://',C.`conn_name`,':',C.`conn_port`,u.`portrait`) AS portrait,"
				+ "u.`nickname` FROM `TDeviceUser` AS du " + "LEFT JOIN `TUser` AS u ON du.`name`=u.`name` "
				+ "LEFT JOIN `TConnServer` AS C ON u.`conn_country`=C.`conn_country` AND C.`conn_device`=2 AND C.`conn_type`=2 "
				+ "WHERE du.`device_sn`='" + deviceSn + "' "
				+ "AND du.`name` NOT IN (SELECT g.`name` FROM `group_user` AS g " + "WHERE g.`device_sn`='" + deviceSn
				+ "' ) ";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 删除用户
	 */
	public boolean deleteGroupUser(String name, String deviceSn) {
		String sqlselectuser = "SELECT * FROM `group_user` WHERE `name` IN ('" + name + "') AND " + "device_sn ='"
				+ deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			String sqldeleteuser = "DELETE FROM `group_user` WHERE `name` IN ('" + name + "') AND " + "device_sn ='"
					+ deviceSn + "'";
			try {
				return daoMapper.modify(sqldeleteuser) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * 修改昵称或者头像
	 * @param username
	 * @param nickname
	 * @param portrait
	 * @return
	 */
	public boolean updateUserInfo(String username, String nickname, String portrait) {
		String updateUserInfo = "UPDATE `TUser` SET ";
		String sqlparam = "";
		try {
			if (nickname != null && !nickname.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `nickname`='" + nickname + "'";
				} else {
					sqlparam += " ,`nickname`='" + nickname + "'";
				}
			}
			if (portrait != null && !portrait.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `portrait`='" + portrait + "'";
				} else {
					sqlparam += " ,`portrait`='" + portrait + "'";
				}
			}

			updateUserInfo += sqlparam + " WHERE `name`='" + username + "'";
			if (!sqlparam.isEmpty()) {
				return daoMapper.modify(updateUserInfo) > 0;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 删除群组
	 */
	public boolean deleteGroup(String deviceSn) {
		String sqlselectuser = "SELECT * FROM `group_user` WHERE `device_sn` = '" + deviceSn + "'";
		List<Map<String, Object>> list = daoMapper.selectList(sqlselectuser);
		if (list != null && list.size() >= 1) {
			String sqldeleteuser = "DELETE FROM `group_user` WHERE `device_sn` = '" + deviceSn + "'";
			try {
				return daoMapper.modify(sqldeleteuser) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			return true;
		}

	}
}
