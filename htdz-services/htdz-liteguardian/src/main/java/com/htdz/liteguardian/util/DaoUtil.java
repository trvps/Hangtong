package com.htdz.liteguardian.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.Base64;
import com.htdz.common.LogManager;
import com.htdz.common.utils.EnumUtils.Constanct;
import com.htdz.db.mapper.DaoMapper;

@Service
@MapperScan("com.htdz.db.mapper")
public class DaoUtil {
	@Autowired
	private DaoMapper daoMapper;

	// private DaoStatic ds = DaoStatic.getInstance();
	@Autowired
	private Environment env;

	/**
	 * 验证用户账号密码
	 * 
	 * @param username
	 * @param encryptPassword
	 * @return
	 */
	public int validateUserPasswordAndEmail(String username, String encryptPassword) {
		String sql = "SELECT is_email_verify FROM TUser WHERE `name` = '" + username + "' AND `password` = '"
				+ encryptPassword + "' AND enabled = 1";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.get("is_email_verify") == null)
			return -1;
		return Integer.parseInt(String.valueOf(map.get("is_email_verify")));
	}

	/**
	 * 更新用户登录时间
	 * 
	 * @param username
	 * @return
	 */
	public boolean updateUserLoginTime(String username) {
		String sql = "UPDATE TUser SET login_time = UTC_TIMESTAMP() WHERE `name` = '" + username
				+ "' AND `enabled` = 1";
		try {
			return daoMapper.modify(sql) > 0 ? true : false;
		} catch (Exception e) {
			LogManager.error("username:" + username + " error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 获取所有广告
	 * 
	 * @return
	 */
	public List<Map<String, Object>> getAdvertising(int adVersion, String username) {
		String sql = "SELECT page_code, page_code_index, image_url, ad_url FROM TAdvertising "
				+ "WHERE `status` = 1 AND `is_customized_app` =" + adVersion + " AND `end_time` > "
				+ "DATE_ADD(UTC_TIMESTAMP() , INTERVAL (SELECT `timezone` FROM `TUser` WHERE `name` = '" + username
				+ "') SECOND) ";
		return daoMapper.selectList(sql);
	}

	/**
	 * 获取用户在APP上的响铃方式和是否验证通过邮箱
	 * 
	 * @param username
	 * @return
	 */
	public Map<String, Object> getUserStatusForApp(String username) {
		String sql = "SELECT alert_mode,is_email_verify,timezone_id,timezone_check,`portrait`,`nickname` FROM TUser WHERE `name` = '"
				+ username + "' OR `mobile`='" + username + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 获取某个用户下的设备信息
	 * 
	 * @param username
	 * @return
	 */
	public List<Map<String, Object>> getDeviceInfo(String username) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT d.device_sn,d.tracker_sim,d.nickname,d.ranges,d.`product_type`,su.`name` AS super_user,")
				.append("d.`defensive`,d.head_portrait,d.gps_interval,").append("u.timezone,d.expired_time,")
				.append("CASE WHEN expired_time < UTC_TIMESTAMP() THEN \"true\" ELSE \"false\" END AS expired,")
				.append("CASE WHEN expired_time < DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 MONTH) THEN \"true\" ELSE \"false\" END AS one_month_expired,")
				.append("IFNULL(tb.`enable`,0) AS bt_enable,IFNULL(cdt.`enable`,0) AS cdt_enable ")
				.append("FROM TDevice d ").append("INNER JOIN TDeviceUser du ON d.id = du.`did` ")
				.append("INNER JOIN TUser u ON u.id = du.`uid` ")
				.append("LEFT JOIN TDeviceUser sdu ON d.id = sdu.`did` AND sdu.is_super_user = 1 ")
				.append("LEFT JOIN TUser su ON sdu.`uid` = su.`id` AND su.`enabled` = 1 ")
				.append("LEFT JOIN `TTimeBoot` AS tb ON d.`id` = tb.`did` ")
				.append("LEFT JOIN `TCourseDisableTime` AS cdt ON d.`id` = cdt.`did` ").append("WHERE u.`name` = '")
				.append(username).append("' OR u.`mobile` ='").append(username).append("'");

		return daoMapper.selectList(sb.toString());
	}

	/**
	 * 判断用户是否已存在
	 * 
	 * @param username
	 * @return
	 */
	public boolean isExistsUser(String username) {
		String sql = "select count(*) from TUser WHERE `name`='" + username + "' AND `enabled`=1";
		return daoMapper.count(sql) > 0;
	}

	/**
	 * 注册用户
	 * 
	 * @param username
	 * @param password
	 * @param serverno
	 * @param timezone
	 * @return
	 */
	public boolean registerUser(String username, String password, int serverno, int timezone, String timezoneid,
			int isCustomizedApp) {
		String sql = "INSERT INTO TUser(`name`, `password`, is_email_verify, `enabled`, create_time, connid, timezone,timezone_id,isCustomizedApp) "
				+ "VALUES('" + username + "','" + password + "',0,1,UTC_TIMESTAMP(),'" + serverno + "','" + timezone
				+ "','" + timezoneid + "','" + isCustomizedApp + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 往邮件发送表新增邮件发送记录
	 * 
	 * @param receiveEmail
	 *            接收者邮箱
	 * @param reveiveName
	 *            接收者名称
	 * @param title
	 *            邮件标题
	 * @param content
	 *            邮件内容
	 * @param attachment
	 *            附件
	 * @param utcDate
	 *            记录邮件新增时间
	 * @return
	 */
	public boolean sendEmail(String receiveEmail, String reveiveName, String title, String content, String attachment,
			String utcDate) {
		String sql = "INSERT INTO `TSendMail`(sm_toMail, sm_toName, sm_title, sm_content, sm_files, sm_createTime) "
				+ "VALUES('" + receiveEmail + "','" + reveiveName + "','" + title + "','" + content + "','" + attachment
				+ "','" + utcDate + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean saveVerifyCode(int userid, String verifycode) {
		String sql = "UPDATE `TUser` SET `email_verify_code`='" + verifycode + "' WHERE `id`='" + userid + "'";

		try {
			if (daoMapper.modify(sql) > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	public boolean changeUserVerifyStatus(int userid, String verifycode) {
		String sql = "UPDATE `TUser` SET `is_email_verify`=1 WHERE `id`='" + userid + "' AND `email_verify_code`='"
				+ verifycode + "'";

		try {
			if (daoMapper.modify(sql) > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	/**
	 * 获取User表ID
	 */
	public int getUserId(String username) {
		// 根据用户名获取当前系统的主见ID
		String sql = "SELECT `id` FROM `TUser` WHERE (`name`='" + username + "' OR `mobile`='" + username
				+ "')  AND `enabled`=1";
		return daoMapper.findBy(sql) == null ? 0 : Integer.valueOf(daoMapper.findBy(sql).toString());
	}

	/**
	 * 根据设备ID获取设备号
	 * 
	 * @param deviceId
	 * @return
	 */
	public String getDeviceSn(int deviceId) {
		String sql = "SELECT `device_sn` FROM `TDevice` WHERE `id`=" + deviceId;
		return daoMapper.findBy(sql) == null ? "" : daoMapper.findBy(sql).toString();
	}

	/**
	 * 获取用户信息
	 * 
	 * @param userId
	 * @return
	 */
	public Map<String, Object> getUserInfo(int userId) {
		try {
			String sql = "SELECT `id`,`name`,`pwd_is_done`,`pwd_verify_code` FROM `TUser` WHERE `id`=" + userId;
			return daoMapper.select(sql);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取用户是否为定制APP用户
	 * 
	 * @param username
	 * @return
	 */
	public boolean userIsCustomizedApp(String username) {
		try {
			String sql = "SELECT `isCustomizedApp` FROM `TUser` WHERE `name`='" + username + "' AND `enabled`=1";
			Object obj = daoMapper.findBy(sql);
			return obj.toString().equals("1");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取用户定制版本
	 * 
	 * @param username
	 * @return
	 */
	public String getUserCustomizedApp(String username) {
		try {
			String sql = "SELECT `isCustomizedApp` FROM `TUser` WHERE `name`='" + username + "' AND `enabled`=1";
			Object obj = daoMapper.findBy(sql);
			return obj.toString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 忘记密码，修改密码
	 * 
	 * @return
	 */
	public boolean changepassword(int userId, String password) {
		String sql = "UPDATE `TUser` SET `password`='" + password
				+ "',`pwd_is_done`=1,`pwd_verify_code`='' WHERE `id`='" + userId + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取User表时区
	 */
	public int getUserTimezone(int userId) {
		String sql = "SELECT timezone FROM TUser WHERE `id`='" + userId + "' AND `enabled`=1";
		return daoMapper.findBy(sql) == null ? 0 : Integer.valueOf(daoMapper.findBy(sql).toString());
	}

	/**
	 * 返回用户绑定设备的记录数
	 */
	public int quantityOfUserDevice(int userID) {
		String sql = "SELECT COUNT(*) FROM TDeviceUser WHERE uid = " + userID;
		return daoMapper.findBy(sql) == null ? 0 : Integer.valueOf(daoMapper.findBy(sql).toString());
	}

	/**
	 * 根据设备号获取设备ID
	 */
	public int getDeviceID(String deviceSN) {
		String sql = "SELECT `id` FROM `TDevice` WHERE `device_sn` = '" + deviceSN + "'";
		Object obj = daoMapper.findBy(sql);
		return obj == null ? 0 : Integer.valueOf(obj.toString());
	}

	public boolean addDevice(String device_sn, int productType, String ranges, String tracker_sim, String mobile1,
			String mobile2, String mobile3, int protocoltype) {
		String sql = "INSERT INTO `TDevice`(`device_sn`,`tracker_sim`,`product_type`,`ranges`,`create_time`,`mobile1`,`mobile2`,`mobile3`,`protocol_type`) "
				+ "VALUES ('" + device_sn + "','" + tracker_sim + "','" + productType + "','" + ranges
				+ "',utc_timestamp(),'" + mobile1 + "','" + mobile2 + "','" + mobile3 + "','" + protocoltype + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 判断是否有已经绑定的追踪器和 用户
	 */
	public int hasBinding(int trackerID, int userID) {
		String sql = "SELECT COUNT(*) FROM TDeviceUser WHERE did = " + trackerID;
		if (userID > 0)
			sql += " AND uid = " + userID;

		Object obj = daoMapper.findBy(sql);
		if (obj == null)
			return 0;

		return Integer.valueOf(obj.toString());
	}

	/**
	 * 更新设备
	 */
	public boolean updateDevice(int deviceID, int productType, String simNo, String ranges, String mobile1,
			String mobile2, String mobile3, int protocoltype, String nickname) {
		String sql = "UPDATE TDevice SET tracker_sim='" + simNo + "', `ranges`='" + ranges + "', `product_type`='"
				+ productType + "', " + "mobile1='" + mobile1 + "', mobile2='" + mobile2 + "', mobile3='" + mobile3
				+ "',`protocol_type`='" + protocoltype + "',`nickname`='" + nickname + "' WHERE id='" + deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean updateDeviceCN(int deviceID, int productType, String simNo, String ranges, String mobile1,
			String mobile2, String mobile3, int protocoltype, String nickname) {
		String sql = "UPDATE TDevice SET tracker_sim='" + simNo + "', `ranges`='" + ranges + "', `product_type`='"
				+ productType + "', " + "mobile1='" + mobile1 + "', mobile2='" + mobile2 + "', mobile3='" + mobile3
				+ "',`protocol_type`='" + protocoltype + "',`nickname`='" + nickname + "' WHERE id='" + deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean changDeviceRanges(int deviceId, String regions) {
		String sql = "UPDATE `TDevice` SET ranges='" + regions + "' WHERE `id`='" + deviceId + "'";

		try {
			int i = daoMapper.modify(sql);

			if (i > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	public boolean changDeviceHeadPortrait(int deviceId, String url) {
		String sql = "UPDATE `TDevice` SET head_portrait='" + url + "' WHERE `id`='" + deviceId + "'";

		try {
			int i = daoMapper.modify(sql);

			if (i > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	/**
	 * 更新用户头像
	 * 
	 * @param username
	 * @param url
	 * @return
	 */
	public boolean updateUserPortrait(String username, String url) {
		String sql = "UPDATE `TUser` SET portrait='" + url + "' WHERE `name`='" + username + "'";

		try {
			int i = daoMapper.modify(sql);

			if (i > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	/**
	 * 添加超级用户或者授权用户
	 */
	public boolean trackerBindUser(int trackerID, int userID, int isSuperUser, String nickname) {
		String sqlselect = "SELECT `conn_country` FROM `TUser` AS u INNER JOIN `TConnServer` AS cs ON u.`connid`=cs.`connid` WHERE `id`="
				+ userID;
		Object conncountry = daoMapper.findBy(sqlselect);

		String country = env.getProperty("default.country");
		if (conncountry != null && !conncountry.toString().isEmpty()) {
			country = conncountry.toString();
		}

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String curutcdate = format.format(new Date());

		String sqlupdate = "UPDATE `TDevice` SET `conn_country`='" + country + "',`first_bind_date`='" + curutcdate
				+ "' WHERE `id`='" + trackerID + "'";
		Object[] params = new Object[] { country, curutcdate, trackerID };

		String sqlselectorg = "SELECT `org_id` FROM `TDevice` WHERE `id`=" + trackerID;
		Object orgid = daoMapper.findBy(sqlselectorg);

		if (orgid == null || orgid.toString().isEmpty()) {
			String defaultOrg = env.getProperty("default.org.id");
			sqlupdate = "UPDATE `TDevice` SET `conn_country`='" + country + "',`org_id`='" + defaultOrg
					+ "',`first_bind_date`='" + nickname + "' WHERE `id`='" + nickname + "'";
			params = new Object[] { country, defaultOrg, curutcdate, trackerID };
		}

		String sql = "INSERT INTO TDeviceUser(did, uid, nickname,is_super_user, create_time) VALUES('" + trackerID
				+ "','" + userID + "','" + nickname + "','" + isSuperUser + "',UTC_TIMESTAMP())";
		try {
			if (isSuperUser == 1) {
				daoMapper.modify(sqlupdate);
			}
			int resulte = 0;
			resulte = daoMapper.modify(sql);

			if (isSuperUser == 1) {
				String bindingSql = "INSERT INTO `binding_log` (device_sn,user_name,bind_time) SELECT * FROM"
						+ " (SELECT d.device_sn,u.name,td.create_time FROM `TDevice` d "
						+ " LEFT JOIN `TDeviceUser` td ON d.id = td.did "
						+ " LEFT JOIN `TUser` u ON u.id = td.uid WHERE d.id = " + trackerID + " AND u.id = " + userID
						+ " ) AS tb";
				daoMapper.modify(bindingSql);
			}
			return resulte == 1;

		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 设置ExpirationTime
	 */
	public boolean setExpirationTime(int trackerID, Date d) {
		String sqlselct = "SELECT `expired_time` FROM `TDevice` WHERE `id`=" + trackerID;
		Object expiredtime = daoMapper.findBy(sqlselct);
		if (expiredtime != null && !expiredtime.toString().isEmpty()) {
			return true;
		} else {
			String sql = "UPDATE TDevice SET expired_time = '" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d)
					+ "' WHERE id = '" + trackerID + "'";
			try {
				return daoMapper.modify(sql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	/**
	 * 设置ExpirationTimeDe
	 */
	public boolean setExpirationTimeDe(int trackerID, Date d) {
		String sqlselct = "SELECT `expired_time_de` FROM `TDevice` WHERE `id`=" + trackerID;
		Object expiredtime = daoMapper.findBy(sqlselct);
		if (expiredtime != null && !expiredtime.toString().isEmpty()) {
			return true;
		} else {
			String sql = "UPDATE TDevice SET expired_time_de = '"
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d) + "' WHERE id = '" + trackerID + "'";
			try {
				return daoMapper.modify(sql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	/*
	 * 设置设备
	 */
	public boolean setDeviceExpirationTime(String deviceSn, String d) {
		String sql = "UPDATE TDevice SET expired_time_de = '" + d + "' WHERE device_sn = '" + deviceSn + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean addOrUpdateAlarm(int deviceID) {
		String sql = "REPLACE INTO `TAlarmSetting`(id) VALUES('" + deviceID + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 更新用户表里面的是否处理邮件和邮件验证吗
	 */
	public boolean updateUserForFindPassWord(String username, int pwd_is_done, String pwd_verify_code) {
		String sql = "UPDATE TUser SET pwd_is_done='" + pwd_is_done + "', pwd_verify_code='" + pwd_verify_code
				+ "' WHERE name='" + username + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取历史轨迹信息列表
	 */
	public List<Map<String, Object>> getHistoricalGPSData(int deviceID, String startTime, String endTime) {
		StringBuffer sb = new StringBuffer();
		sb.append(
				"SELECT d.`device_sn`, DATE_ADD(gd.`collect_datetime`, INTERVAL tu.`timezone` SECOND) AS collect_datetime,")
				.append("gd.lat, gd.lng, gd.`speed`, gd.`direction`, gd.`LBS_WIFI_Range`,gd.`steps`,gd.`calorie` ")
				.append("FROM TGpsData gd ").append("LEFT JOIN TDevice d ON gd.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE gd.did = ").append(deviceID)
				.append(" AND gd.lat is not null AND gd.lng is not null AND (gd.`gps_flag` = 3 or gd.`gps_flag` = 10  or gd.`gps_flag` = 2) ")
				// .append(" AND gd.lat is not null AND gd.lng is not null ")
				.append(" AND gd.collect_datetime BETWEEN DATE_ADD('").append(startTime)
				.append("', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('").append(endTime)
				.append("', INTERVAL -tu.`timezone` SECOND) ").append(" AND gd.deleteflag=0 ")
				.append("ORDER BY gd.collect_datetime ASC");
		return daoMapper.selectList(sb.toString());
	}

	/**
	 * 获取历史轨迹信息列表
	 */
	public List<Map<String, Object>> getHistoricalGPSDataOrderByTime(int deviceID, String startTime, String endTime) {
		StringBuffer sb = new StringBuffer();
		sb.append(
				"SELECT d.`device_sn`, DATE_ADD(gd.`collect_datetime`, INTERVAL tu.`timezone` SECOND) AS collect_datetime,")
				.append("gd.lat, gd.lng, gd.`speed`, gd.`direction`, gd.`LBS_WIFI_Range`,gd.`steps`,gd.`calorie` ")
				.append("FROM TGpsData gd ").append("LEFT JOIN TDevice d ON gd.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE gd.did = ").append(deviceID)
				.append(" AND gd.lat is not null AND gd.lng is not null AND (gd.`gps_flag` = 3 or gd.`gps_flag` = 10 or gd.`gps_flag` = 2) ")
				// .append(" AND gd.lat is not null AND gd.lng is not null ")
				.append(" AND gd.collect_datetime BETWEEN DATE_ADD('").append(startTime)
				.append("', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('").append(endTime)
				.append("', INTERVAL -tu.`timezone` SECOND) ").append(" AND gd.deleteflag=0 ")
				.append("ORDER BY gd.steps ASC limit 0,1");
		return daoMapper.selectList(sb.toString());
	}

	public boolean addRemind(int uid, String title, String weekly, String monthly, String yearly, String monday,
			String tuesday, String wednesday, String thursday, String friday, String saturday, String sunday,
			String specificYear, String specificMonth, String specificDay, String isEnd) {
		String sql = "INSERT INTO TClockRule(uid, title, weekly, monthly, yearly,"
				+ "monday, tuesday, wednesday, thursday, friday, saturday, sunday,"
				+ "repeat_year, repeat_month, repeat_day, lastday_flag, delete_flag, update_time) " + "values(" + uid
				+ ",'" + title + "'," + weekly + "," + monthly + "," + yearly + "," + monday + "," + tuesday + ","
				+ wednesday + "," + thursday + "," + friday + "," + saturday + "," + sunday + "," + specificYear + ","
				+ specificMonth + "," + specificDay + "," + isEnd + ",1,UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean modifyRemind(String id, String title, String weekly, String monthly, String yearly, String monday,
			String tuesday, String wednesday, String thursday, String friday, String saturday, String sunday,
			String specificYear, String specificMonth, String specificDay, String isEnd, String diabolo) {
		String sql = "UPDATE TClockRule SET title = '" + title + "', weekly = '" + weekly + "', monthly = '" + monthly
				+ "', yearly = '" + yearly + "', monday = '" + monday + "', tuesday = '" + tuesday + "', wednesday = '"
				+ wednesday + "', thursday = '" + thursday + "', friday = '" + friday + "'," + "saturday = '" + saturday
				+ "', sunday = '" + sunday + "', repeat_year = '" + specificYear + "', repeat_month = '" + specificMonth
				+ "', repeat_day = '" + specificDay + "', lastday_flag = '" + isEnd + "', update_time = UTC_TIMESTAMP()"
				+ "WHERE id = '" + id + "'";

		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public int getMaxRemindID() {
		String sql = "SELECT MAX(id) AS id FROM TClockRule";
		Object result = daoMapper.findBy(sql);
		if (result == null) {
			return 0;
		}
		return Integer.parseInt(String.valueOf(result));
	}

	public boolean addRemindTime(int remindID, String time) {
		String sql = "INSERT INTO TClockTime(cid, AlarmTime, update_time) " + "values('" + remindID + "', '" + time
				+ "', UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean deleteRemindTime(int remindID) {
		String sql = "delete from TClockTime where cid = " + remindID;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public List<Map<String, Object>> getRemind(int userID) {
		String sql = "SELECT id, title, weekly, monthly, yearly, monday, tuesday, wednesday, "
				+ "thursday, friday, saturday, sunday, repeat_year, repeat_month, repeat_day,lastday_flag as isEnd "
				+ "FROM TClockRule WHERE uid = " + userID;
		return daoMapper.selectList(sql);
	}

	public List<Map<String, Object>> getRemindTime(int remindID) {
		String sql = "SELECT cid, DATE_FORMAT(AlarmTime,'%H:%i') AS AlarmTime FROM TClockTime WHERE cid = " + remindID;
		return daoMapper.selectList(sql);
	}

	public boolean deleteRemind(int remindID) {
		String sql = "DELETE FROM TClockRule WHERE id =" + remindID;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean setGeoFence(String deviceID, String userID, String lat, String lng, String radius) {
		String sql = "INSERT INTO TAreaInfo(lat, lng, radius, did, uid, enabled, create_time) " + "VALUES('" + lat
				+ "', '" + lng + "', '" + radius + "', '" + deviceID + "', '" + userID + "', 1, UTC_TIMESTAMP())";
		try {
			// daoMapper.modify(updateDefenceSql);
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public int hasGeoFence(String deviceId) {
		String sql = "SELECT COUNT(*) FROM TAreaInfo WHERE did = " + deviceId;
		Object obj = daoMapper.findBy(sql);
		if (obj == null) {
			return 0;
		}
		return Integer.parseInt(obj.toString());
	}

	public boolean updateGeoFence(String deviceID, String userID, String lat, String lng, String radius) {
		String sql = "UPDATE TAreaInfo SET lat = '" + lat + "', lng = '" + lng + "', radius = '" + radius + "', uid = '"
				+ userID + "',enabled=1, create_time = UTC_TIMESTAMP() WHERE did = '" + deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public Map<String, Object> getGeoFence(String deviceID) {
		String sql = "SELECT lat, lng, radius,areaid,defencename FROM TAreaInfo WHERE did = '" + deviceID + "'";
		return daoMapper.select(sql);
	}

	public boolean deleteGeoFence(String deviceID) {
		String sql = "DELETE FROM TAreaInfo WHERE did = '" + deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public Map<String, Object> getPhoneVersion(String phoneSystem, String customizedAppStr) {
		String sql = "SELECT `os`, `version`, `url`, `url_apk`, `is_force_update`, `description` FROM TVersion WHERE os = '"
				+ phoneSystem + "' AND isCustomizedApp = '" + customizedAppStr + "' ORDER BY `version` DESC LIMIT 0,1";
		return daoMapper.select(sql);
	}

	public Map<String, Object> getLastFirmware(String product_type) {
		String sql = "SELECT `version_name`, `file` FROM `version` WHERE device_type = '" + product_type
				+ "' ORDER BY upload_date DESC LIMIT 0,1";
		return daoMapper.select(sql);
	}

	public Map<String, Object> getDeviceType(String deviceSn) {
		String sql = "SELECT firmware,ranges,product_type,expired_time,protocol_type FROM TDevice d WHERE `device_sn` = '"
				+ deviceSn + "'";
		return daoMapper.select(sql);
	}

	public Map<String, Object> getDeviceOrg(String deviceSn) {
		String sql = "SELECT product_type,ranges,o.name FROM TDevice d left join `org` o on d.`org_id`= o.id WHERE `device_sn` = '"
				+ deviceSn + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 是否存在已经绑定的用户和设备
	 */
	public boolean hasOperatingAuthority(int trackerID, int userID) {
		String sql = "SELECT COUNT(*) FROM TDeviceUser WHERE did = " + trackerID + " AND uid = " + userID;
		Object obj = daoMapper.findBy(sql);
		if (obj == null || Integer.valueOf(obj.toString()) == 0)
			return false;
		return true;
	}

	/**
	 * 是否存在已经绑定超级用户的用户名和设备
	 */
	public boolean isSuperUser(String superUserName, String trackerNo) {
		String sql = "SELECT COUNT(*) FROM TDeviceUser du " + " INNER JOIN TUser u ON du.`uid` = u.`id`"
				+ " INNER JOIN TDevice d ON du.`did` = d.`id`" + " WHERE du.`is_super_user` = 1 AND"
				+ "  d.`device_sn` = '" + trackerNo + "' AND (u.`name` = '" + superUserName + "' OR u.`mobile`='"
				+ superUserName + "')";

		Object obj = daoMapper.findBy(sql);
		if (obj == null || Integer.valueOf(obj.toString()) == 0)
			return false;
		return true;
	}

	/**
	 * 获取绑定该设备的用户名
	 */
	public List<Map<String, Object>> getTrackerUser(int trackerID) {
		String sql = "SELECT `name` FROM TUser WHERE id IN (SELECT uid FROM TDeviceUser WHERE did = " + trackerID
				+ " AND is_super_user = 0) AND `enabled` = 1";
		return daoMapper.selectList(sql);
	}

	/**
	 * 获取最后的GPS数据
	 */
	public Map<String, Object> getCurrentGPS(String deviceSn, int timezone) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT g.`did`, DATE_ADD(g.`collect_datetime`, INTERVAL ").append(timezone)
				.append(" SECOND) AS collect_datetime,")
				.append("g.lat, g.`lng`, g.`speed`,g.`gps_flag`, g.`direction`, g.`battery`, g.`steps`, g.`LBS_WIFI_Range` ")
				.append("FROM TGpsData g ").append("WHERE g.`device_sn` = '").append(deviceSn)
				.append("' AND g.lat is not null AND g.lng is not null AND (g.`gps_flag` = 3 or g.`gps_flag` = 10 or g.`gps_flag` = 2) ")
				.append(" AND g.deleteflag=0 ").append("ORDER BY g.`collect_datetime` DESC LIMIT 0,1");
		List<Map<String, Object>> list = daoMapper.selectList(sb.toString());
		return list == null || list.size() == 0 ? null : list.get(0);
	}

	public boolean setDeviceInfo(String deviceSn, String nickName, String mobile1, String mobile2, String mobile3) {
		String sql = "UPDATE TDevice SET nickname = '" + nickName + "', mobile1 = '" + mobile1 + "', mobile2 = '"
				+ mobile2 + "', mobile3 = '" + mobile3 + "' WHERE device_sn = '" + deviceSn + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean setDeviceInfo(String deviceSn, String nickName, String mobile1, String mobile2, String mobile3,
			String simNo) {
		String updateInfo = "UPDATE TDevice SET ";
		String sqlparam = "";

		if (nickName != null && !nickName.isEmpty()) {
			if (sqlparam.isEmpty()) {
				sqlparam += " `nickname`='" + nickName + "'";
			} else {
				sqlparam += " ,`nickname`='" + nickName + "'";
			}
		}
		if (mobile1 != null && !mobile1.isEmpty()) {
			if (sqlparam.isEmpty()) {
				sqlparam += " `mobile1`='" + mobile1 + "'";
			} else {
				sqlparam += " ,`mobile1`='" + mobile1 + "'";
			}
		}
		if (mobile2 != null && !mobile2.isEmpty()) {
			if (sqlparam.isEmpty()) {
				sqlparam += " `mobile2`='" + mobile2 + "'";
			} else {
				sqlparam += " ,`mobile2`='" + mobile2 + "'";
			}
		}
		if (mobile3 != null && !mobile3.isEmpty()) {
			if (sqlparam.isEmpty()) {
				sqlparam += " `mobile3`='" + mobile3 + "'";
			} else {
				sqlparam += " ,`mobile3`='" + mobile3 + "'";
			}
		}
		if (simNo != null && !simNo.isEmpty()) {
			if (sqlparam.isEmpty()) {
				sqlparam += " `tracker_sim`='" + simNo + "'";
			} else {
				sqlparam += " ,`tracker_sim`='" + simNo + "'";
			}
		}

		updateInfo += sqlparam + " WHERE `device_sn`='" + deviceSn + "'";

		try {
			if (!sqlparam.isEmpty()) {
				return daoMapper.modify(updateInfo) > 0;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean hasHumanInfo(int deviceId) {
		String sql = "SELECT COUNT(*) FROM TDeviceInfo WHERE did = " + deviceId;
		Object obj = daoMapper.findBy(sql);
		if (obj == null || Integer.valueOf(obj.toString()) == 0)
			return false;
		return true;
	}

	/** 防走失信息卡开始 **********************************************************************/
	// 人
	public boolean addHumanInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		String sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_age, human_height,human_weight, human_step, human_feature, human_addr, human_lost_addr) "
				+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanAge + "', '"
				+ humanHeight + "', '" + humanWeight + "', '" + humanStep + "', '" + humanFeature + "','" + humanAddr
				+ "','" + humanLostAddr + "');";
		Object[] arrObj = new Object[] { deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
				humanFeature, humanAddr, humanLostAddr };
		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_age, human_height,human_weight, human_step, human_feature, human_addr, human_lost_addr,human_birthday) "
					+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanAge + "', '"
					+ humanHeight + "', '" + humanWeight + "', '" + humanStep + "', '" + humanFeature + "','"
					+ humanAddr + "','" + humanLostAddr + "','" + humanBirthday + "');";
			arrObj = new Object[] { deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday };
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateHumanInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		String sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex
				+ "', human_age = '" + humanAge + "', human_height = '" + humanHeight + "',human_weight='" + humanWeight
				+ "', human_step = '" + humanStep + "', human_feature = '" + humanFeature + "', human_addr = '"
				+ humanAddr + "', human_lost_addr = '" + humanLostAddr + "' WHERE did = '" + deviceID + "'";

		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex + "', human_age = '"
					+ humanAge + "', human_height = '" + humanHeight + "',human_weight='" + humanWeight
					+ "', human_step = '" + humanStep + "', human_feature = '" + humanFeature + "', human_addr = '"
					+ humanAddr + "', human_lost_addr = '" + humanLostAddr + "',human_birthday = '" + humanBirthday
					+ "' WHERE did = '" + deviceID + "'";
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getHumanInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo, "
				+ "di.human_sex, di.human_age, di.human_height, di.human_weight, di.human_step, di.human_feature,"
				+ "di.human_addr, di.human_lost_addr,DATE_FORMAT(di.human_birthday, '%Y-%m-%d') AS human_birthday,cs.`conn_name`,cs.`conn_port` "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 狗
	public boolean addPetInfo(int deviceID, String pet_name, String pet_sex, String pet_breed, String pet_age,
			String pet_weight, String pet_feature, String pet_addr, String pet_lost_addr, String pet_birthday) {
		String sql = "INSERT INTO TDeviceInfo(did, pet_name,pet_sex,pet_breed,pet_age, pet_weight, pet_feature, pet_addr, pet_lost_addr) "
				+ "VALUES('" + deviceID + "', '" + pet_name + "', '" + pet_sex + "', '" + pet_breed + "', '" + pet_age
				+ "', '" + pet_weight + "', '" + pet_feature + "', '" + pet_addr + "','" + pet_lost_addr + "');";
		if (pet_birthday != null && !pet_birthday.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, pet_name,pet_sex,pet_breed,pet_age, pet_weight, pet_feature, pet_addr, pet_lost_addr,pet_birthday) "
					+ "VALUES('" + deviceID + "', '" + pet_name + "', '" + pet_sex + "', '" + pet_breed + "', '"
					+ pet_age + "', '" + pet_weight + "', '" + pet_feature + "', '" + pet_addr + "','" + pet_lost_addr
					+ "','" + pet_birthday + "');";
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updatePetInfo(int deviceID, String pet_name, String pet_sex, String pet_breed, String pet_age,
			String pet_weight, String pet_feature, String pet_addr, String pet_lost_addr, String pet_birthday) {
		String sql = "UPDATE TDeviceInfo SET pet_name='" + pet_name + "', pet_sex = '" + pet_sex + "', pet_breed = '"
				+ pet_breed + "', pet_age = '" + pet_age + "',pet_weight='" + pet_weight + "', pet_feature = '"
				+ pet_feature + "', pet_addr = '" + pet_addr + "', pet_lost_addr = '" + pet_lost_addr
				+ "' WHERE did = '" + deviceID + "'";
		Object[] arrObj = new Object[] { pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature, pet_addr,
				pet_lost_addr, deviceID };
		if (pet_birthday != null && !pet_birthday.equals("")) {
			sql = "UPDATE TDeviceInfo SET pet_name='" + pet_name + "', pet_sex = '" + pet_sex + "', pet_breed = '"
					+ pet_breed + "', pet_age = '" + pet_age + "',pet_weight='" + pet_weight + "', pet_feature = '"
					+ pet_feature + "', pet_addr = '" + pet_addr + "', pet_lost_addr = '" + pet_lost_addr
					+ "',pet_birthday = '" + pet_birthday + "' WHERE did = '" + deviceID + "'";
			arrObj = new Object[] { pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature, pet_addr,
					pet_lost_addr, pet_birthday, deviceID };
		}

		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/*
	 * 根据宠物保险修改宠物信息
	 */
	public boolean updatePetInfoByInsur(int deviceID, String pet_sex, String pet_age, String pet_breed,
			String pet_mobile1) {
		String sql = "UPDATE TDeviceInfo SET pet_sex = '" + pet_sex + "', pet_age = '" + pet_age + "',pet_breed='"
				+ pet_breed + "', pet_mobile1 = '" + pet_mobile1 + "' WHERE did = '" + deviceID + "'";

		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getPetInfo(int deviceId) {
		// insur_code 2:表示已激活 1：表示待激活 0：表示不能激活
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo, "
				+ "di.did,di.pet_sex,di.pet_breed,di.pet_age, di.pet_weight, di.pet_feature, di.pet_addr, "
				+ "di.pet_lost_addr,DATE_FORMAT(di.pet_birthday, '%Y-%m-%d') AS pet_birthday,cs.`conn_name`,"
				+ "cs.`conn_port`,CASE WHEN p.id IS NOT NULL AND p.id!='' THEN 2 ELSE CASE WHEN d.insur_code IS NULL OR d.insur_code='' THEN 0 ELSE 1 END END AS insur_code "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did LEFT JOIN `pet_insur` AS p ON d.`device_sn` = p.`device_sn` "
				+ "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 车
	public boolean addCarInfo(int deviceID, String car_no, String car_vin, String car_engin, String car_set,
			String car_brand, String car_year, String car_type, String car_oil_type, String car_mileage,
			String car_check_time, String car_buytime) {
		String sql = "INSERT INTO TDeviceInfo(did, car_no, car_vin, car_engine,car_set, car_brand, car_year, car_type, car_oil_type,car_mileage,car_check_time) "
				+ "VALUES('" + deviceID + "', '" + car_no + "', '" + car_vin + "', '" + car_engin + "', '" + car_set
				+ "', '" + car_brand + "', '" + car_year + "', '" + car_type + "','" + car_oil_type + "','"
				+ car_mileage + "','" + car_check_time + "');";
		if (car_buytime != null && !car_buytime.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, car_no, car_vin, car_engine,car_set, car_brand, car_year, car_type, car_oil_type,car_mileage,car_check_time,car_buytime) "
					+ "VALUES('" + deviceID + "', '" + car_no + "', '" + car_vin + "', '" + car_engin + "', '" + car_set
					+ "', '" + car_brand + "', '" + car_year + "', '" + car_type + "','" + car_oil_type + "','"
					+ car_mileage + "','" + car_check_time + "','" + car_buytime + "');";
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateCarInfo(int deviceID, String car_no, String car_vin, String car_engin, String car_set,
			String car_brand, String car_year, String car_type, String car_oil_type, String car_mileage,
			String car_check_time, String car_buytime) {
		String sql = "UPDATE TDeviceInfo SET car_no='" + car_no + "', car_vin = '" + car_vin + "', car_engine = '"
				+ car_engin + "', car_set = '" + car_set + "',car_brand='" + car_brand + "', car_year = '" + car_year
				+ "', car_type = '" + car_type + "', car_oil_type = '" + car_oil_type + "', car_mileage = '"
				+ car_mileage + "', car_check_time = '" + car_check_time + "' WHERE did = '" + deviceID + "'";

		if (car_buytime != null && !car_buytime.equals("")) {
			sql = "UPDATE TDeviceInfo SET car_no='" + car_no + "', car_vin = '" + car_vin + "', car_engine = '"
					+ car_engin + "', car_set = '" + car_set + "',car_brand='" + car_brand + "', car_year = '"
					+ car_year + "', car_type = '" + car_type + "', car_oil_type = '" + car_oil_type
					+ "', car_mileage = '" + car_mileage + "', car_check_time = '" + car_check_time
					+ "',car_buytime = '" + car_buytime + "' WHERE did = '" + deviceID + "'";

		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getCarInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo,"
				+ "di.car_no, di.car_vin, di.car_engine,di.car_set, di.car_brand, di.car_year, "
				+ "di.car_type, di.car_oil_type,di.car_mileage,"
				+ "DATE(di.car_check_time) AS car_check_time,DATE_FORMAT(di.car_buytime, '%Y-%m-%d') AS car_buytime,cs.`conn_name`,cs.`conn_port` "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 摩托
	public boolean addMotoInfo(int deviceID, String motor_no, String moto_type, String motor_cc, String motor_trademark,
			String motor_set, String motor_year, String motor_buytime) {
		String sql = "INSERT INTO TDeviceInfo(did, motor_no, moto_type, motor_cc,motor_trademark, motor_set, motor_year) "
				+ "VALUES('" + deviceID + "', '" + motor_no + "', '" + moto_type + "', '" + motor_cc + "', '"
				+ motor_trademark + "', '" + motor_set + "', '" + motor_year + "');";
		Object[] arrObj = new Object[] { deviceID, motor_no, moto_type, motor_cc, motor_trademark, motor_set,
				motor_year };
		if (motor_buytime != null && !motor_buytime.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, motor_no, moto_type, motor_cc,motor_trademark, motor_set, motor_year,motor_buytime) "
					+ "VALUES('" + deviceID + "', '" + motor_no + "', '" + moto_type + "', '" + motor_cc + "', '"
					+ motor_trademark + "', '" + motor_set + "', '" + motor_year + "','" + motor_buytime + "');";
			arrObj = new Object[] { deviceID, motor_no, moto_type, motor_cc, motor_trademark, motor_set, motor_year,
					motor_buytime };
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateMotoInfo(int deviceID, String motor_no, String moto_type, String motor_cc,
			String motor_trademark, String motor_set, String motor_year, String motor_buytime) {
		String sql = "UPDATE TDeviceInfo SET motor_no='" + motor_no + "', moto_type = '" + moto_type + "', motor_cc = '"
				+ moto_type + "', motor_trademark = '" + motor_cc + "',motor_set='" + motor_trademark
				+ "', motor_year = '" + motor_set + "' WHERE did = '" + motor_year + "'";
		Object[] arrObj = new Object[] { motor_no, moto_type, motor_cc, motor_trademark, motor_set, motor_year,
				deviceID };
		if (motor_buytime != null && !motor_buytime.equals("")) {
			sql = "UPDATE TDeviceInfo SET motor_no='" + motor_no + "', moto_type = '" + moto_type + "', motor_cc = '"
					+ motor_cc + "', motor_trademark = '" + motor_trademark + "',motor_set='" + motor_set
					+ "', motor_year = '" + motor_year + "',motor_buytime = '" + motor_buytime + "' WHERE did = '"
					+ deviceID + "'";
			arrObj = new Object[] { motor_no, moto_type, motor_cc, motor_trademark, motor_set, motor_year,
					motor_buytime, deviceID };
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getMotoInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo, "
				+ "di.motor_no,di.moto_type, di.motor_cc,di.motor_trademark, "
				+ "di.motor_set, di.motor_year,DATE_FORMAT(di.motor_buytime, '%Y-%m-%d') AS motor_buytime,cs.`conn_name`,cs.`conn_port` "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 手表
	public boolean addWatchInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		String sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_age, human_height,human_weight, human_step, human_feature, human_addr, human_lost_addr) "
				+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanAge + "', '"
				+ humanHeight + "', '" + humanWeight + "', '" + humanStep + "', '" + humanFeature + "','" + humanAddr
				+ "','" + humanLostAddr + "');";
		Object[] arrObj = new Object[] { deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
				humanFeature, humanAddr, humanLostAddr };
		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_age, human_height,human_weight, human_step, human_feature, human_addr, human_lost_addr,human_birthday) "
					+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanAge + "', '"
					+ humanHeight + "', '" + humanWeight + "', '" + humanStep + "', '" + humanFeature + "','"
					+ humanAddr + "','" + humanLostAddr + "','" + humanBirthday + "');";
			arrObj = new Object[] { deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday };
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateWatchInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		String sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex
				+ "', human_age = '" + humanAge + "', human_height = '" + humanHeight + "',human_weight='" + humanWeight
				+ "', human_step = '" + humanStep + "', human_feature = '" + humanFeature + "', human_addr = '"
				+ humanAddr + "', human_lost_addr = '" + humanLostAddr + "' WHERE did = '" + deviceID + "'";

		Object[] arrObj = new Object[] { humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
				humanFeature, humanAddr, humanLostAddr, deviceID };
		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex + "', human_age = '"
					+ humanAge + "', human_height = '" + humanHeight + "',human_weight='" + humanWeight
					+ "', human_step = '" + humanStep + "', human_feature = '" + humanFeature + "', human_addr = '"
					+ humanAddr + "', human_lost_addr = '" + humanLostAddr + "',human_birthday = '" + humanBirthday
					+ "' WHERE did = '" + deviceID + "'";
			arrObj = new Object[] { humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep, humanFeature,
					humanAddr, humanLostAddr, humanBirthday, deviceID };
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getWatchInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo, "
				+ "di.human_sex, di.human_age, di.human_height, di.human_weight, di.human_step, di.human_feature,"
				+ "di.human_addr, di.human_lost_addr,DATE_FORMAT(di.human_birthday, '%Y-%m-%d') AS human_birthday,cs.`conn_name`,cs.`conn_port` "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// obd
	public boolean addObdInfo(int deviceID, String obd_no, String obd_type, String obd_buytime, String car_vin) {
		String sql = "INSERT INTO TDeviceInfo(did, obd_no, obd_type, obd_buytime, car_vin) " + "VALUES('" + deviceID
				+ "', '" + obd_no + "', '" + obd_type + "', '" + obd_buytime + "','" + car_vin + "');";
		Object[] arrObj = new Object[] { deviceID, obd_no, obd_type, obd_buytime, car_vin };
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateObdInfo(int deviceID, String obd_no, String obd_type, String obd_buytime, String car_vin) {
		String sql = "UPDATE TDeviceInfo SET obd_no='" + obd_no + "', obd_type = '" + obd_type + "', obd_buytime = '"
				+ obd_buytime + "', car_vin = '" + car_vin + "' WHERE did = '" + deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getObdInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3,d.tracker_sim as simNo, "
				+ "di.obd_no, di.obd_type, DATE_FORMAT(di.obd_buytime, '%Y-%m-%d') AS obd_buytime,di.car_vin, "
				+ "cs.`conn_name`,cs.`conn_port` " + "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 手表
	public boolean addBluetoothWatchInfo(int deviceID, String humanName, String humanSex, String humanHeight,
			String humanWeight, String humanBirthday, String humanFeature) {
		String sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_height,human_weight,human_feature) "
				+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanHeight + "', '"
				+ humanWeight + "','" + humanFeature + "');";
		Object[] arrObj = new Object[] { deviceID, humanName, humanSex, humanHeight, humanWeight, humanFeature };
		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "INSERT INTO TDeviceInfo(did, human_name, human_sex, human_height,human_weight, human_birthday,human_feature) "
					+ "VALUES('" + deviceID + "', '" + humanName + "', '" + humanSex + "', '" + humanHeight + "', '"
					+ humanWeight + "', '" + humanBirthday + "','" + humanFeature + "');";
		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateBluetoothWatchInfo(int deviceID, String humanName, String humanSex, String humanHeight,
			String humanWeight, String humanBirthday, String humanFeature) {
		String sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex
				+ "', human_height = '" + humanHeight + "',human_weight='" + humanWeight + "',human_feature='"
				+ humanFeature + "' WHERE did = '" + deviceID + "'";

		if (humanBirthday != null && !humanBirthday.equals("")) {
			sql = "UPDATE TDeviceInfo SET human_name='" + humanName + "', human_sex = '" + humanSex
					+ "', human_height = '" + humanHeight + "',human_weight='" + humanWeight + "', human_birthday = '"
					+ humanBirthday + "',human_feature='" + humanFeature + "' WHERE did = '" + deviceID + "'";

		}
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getBluetoothWatchInfo(int deviceId) {
		String sql = "SELECT d.nickname,d.head_portrait, d.mobile1, d.mobile2, d.mobile3, "
				+ "di.human_sex, di.human_age, di.human_height, di.human_weight, di.human_step, di.human_feature,"
				+ "di.human_addr, di.human_lost_addr,DATE_FORMAT(di.human_birthday, '%Y-%m-%d') AS human_birthday,cs.`conn_name`,cs.`conn_port` "
				+ "FROM TDevice d INNER JOIN `TConnServer` AS cs ON "
				+ "cs.`conn_device`=2 AND cs.`conn_type`=2 AND d.`conn_country`=cs.`conn_country` "
				+ "LEFT JOIN TDeviceInfo di ON d.id = di.did " + "WHERE d.id = " + deviceId;
		return daoMapper.select(sql);
	}

	// 删除紧急联系人
	public boolean deleteEmergencyContactNo(int deviceId) {
		String sql = "UPDATE `TDevice` SET `mobile1`='',`mobile2`='',`mobile3`='' WHERE `id`=" + deviceId;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	/**************** 防走失信息卡玩 ************************************************************************/

	/**
	 * 获取用户的trackerList
	 */
	public List<Map<String, Object>> getTrackerBySuperUser(String username) {
		String sql = "SELECT `code`,tracker_sim,`product_type`,remark,contact,mobile1,mobile2,mobile3,satellite,"
				+ "gps_interval,speed,firmware,password,timezone,createtime,expired_time FROM TDevice WHERE id IN "
				+ "(SELECT did FROM TDeviceUser WHERE uid = (SELECT id FROM TUser WHERE name = " + username
				+ "  AND enabled = 1) AND is_super_user = 1) AND enabled = 1";
		List<Map<String, Object>> superUserList = daoMapper.selectList(sql);
		return superUserList != null && superUserList.size() != 0 ? superUserList : null;
	}

	/**
	 * 远程同步追踪器
	 */
	public Map<String, Object> remoteregisterbinding(String username, List<Map<String, String>> trackerlist) {
		Map<String, Object> map = new HashMap<>();

		int userId = getUserId(username);

		for (Map<String, String> tmpa : trackerlist) {
			String code = tmpa.get("code");
			String tracker_sim = tmpa.get("tracker_sim");
			String product_type = tmpa.get("product_type");
			String password = tmpa.get("password");

			String remark = tmpa.get("remark");
			String contact = tmpa.get("contact");
			String mobile1 = tmpa.get("mobile1");
			String mobile2 = tmpa.get("mobile2");
			String mobile3 = tmpa.get("mobile3");
			String satellite = tmpa.get("satellite");
			String gps_interval = tmpa.get("gps_interval");
			String speed = tmpa.get("speed");
			String firmware = tmpa.get("firmware");

			String timezone = tmpa.get("timezone");
			// String createtime = tmpa.get("createtime");
			String expired_time = tmpa.get("expired_time") == null ? "2099-01-01" : tmpa.get("expired_time");

			// 获取当前系统的设备ID
			int trackerId = getDeviceID(code);

			if (trackerId == 0) {
				// 添加记录
				String addSql = "INSERT INTO `TDevice`(`code`,`tracker_sim`,`product_type`,`password`,`remark`,`contact`,"
						+ " `mobile1`,`mobile2`,`mobile3`,`satellite`,`gps_interval`,`speed`,"
						+ " `firmware`,`timezone`,`createtime`,`expired_time`)" + " VALUES ('" + code + "','"
						+ tracker_sim + "','" + product_type + "','" + password + "','" + remark + "','" + contact
						+ "','" + mobile1 + "','" + mobile2 + "','" + mobile3 + "','" + satellite + "','" + gps_interval
						+ "','" + speed + "','" + firmware + "','" + timezone + "',NOW(),'" + expired_time + "')";
				try {
					daoMapper.modify(addSql);
				} catch (Exception e) {
					LogManager.error("error:" + e.getMessage());
				}
				trackerId = getDeviceID(code);
			} else {
				// 更新记录
				String updateSql = "UPDATE TDevice SET tracker_sim='" + tracker_sim + "',`product_type`='"
						+ product_type + "',`password`='" + password + "'," + "`remark`='" + remark + "',`contact`='"
						+ contact + "',`mobile1`='" + mobile1 + "'," + "`mobile2`='" + mobile2 + "',`mobile3`='"
						+ mobile3 + "',`satellite`='" + satellite + "',`gps_interval`='" + gps_interval + "',`speed`='"
						+ speed + "',`firmware`='" + firmware + "'," + "`timezone`='" + timezone
						+ "',`createtime`=NOW(),`expired_time`='" + expired_time + "' WHERE id ='" + trackerId + "'";
				try {
					daoMapper.modify(updateSql);
				} catch (Exception e) {
					LogManager.error("error:" + e.getMessage());
				}
			}

			if (hasBinding(trackerId, userId) <= 0) {
				String addTDeviceUserSql = "INSERT INTO TDeviceUser(did, uid, is_super_user) VALUES('" + trackerId
						+ "','" + userId + "',1)";
				try {
					daoMapper.modify(addTDeviceUserSql);
				} catch (Exception e) {
					LogManager.error("error:" + e.getMessage());
				}
			}
			map.put("success", true);
			return map;
		}
		map.put("success", false);
		return map;
	}

	// /**
	// * 再次封装注册
	// */
	// public Map<String, Object> register(HttpServletRequest req,
	// String username, String password, String serverno, String timezone) {
	// Map<String, Object> map = new HashMap<>();
	// // 判断用户是否存在
	// if (isExistsUser(username)) {
	// map.put("code", "100");
	// map.put("ret", "");
	// map.put("what", PropertyUtil.getValue(req, "forlogin.user_exist"));
	// return map;
	// }
	//
	// // 判断用户是否添加成功
	// if (!registerUser(username, password, serverno, timezone)) {
	// map.put("code", "200");
	// map.put("ret", "");
	// map.put("what",
	// PropertyUtil.getValue(req, "forlogin.save_user_failed"));
	// return map;
	// }
	//
	// // 发送邮件
	// Map<String, Object> result = sendEmail(req, username);
	// if (!"0".equals(result.get("code"))) {
	// map.put("code", "250");
	// map.put("ret", "");
	// map.put("what", PropertyUtil.getValue(req,
	// "forlogin.send_registeremail_failed"));
	// return map;
	// }
	//
	// map.put("code", "0");
	// map.put("ret", "");
	// map.put("what", PropertyUtil.getValue(req, "forlogin.register_success"));
	// return map;
	// }

	/**
	 * 获取is_email_verify
	 */
	public int getEmailVerify(String username, String password) {
		// 查询是否存在用户
		String sql = "select is_email_verify from TUser where name = '" + username + "' and password = '" + password
				+ "' and enabled = 1";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		int isEmailVerify = -1;
		if (list == null || list.size() == 0) {
			isEmailVerify = -1;
		} else {
			isEmailVerify = Integer.parseInt(String.valueOf(list.get(0).get("is_email_verify")));
		}

		return isEmailVerify;
	}

	/*
	 * 获取用户邮箱是否验证
	 */
	public int getUserEmailVerify(String username) {
		String sql = "SELECT is_email_verify FROM TUser WHERE `name` = '" + username + "' AND enabled = 1";
		Object obj = null;
		obj = daoMapper.findBy(sql);
		// 用户名或密码错误
		if (obj == null)
			return -1;

		return Integer.valueOf(obj.toString());

	}

	/**
	 * 更新用户登录时间
	 */
	public boolean updateLoginTime(String username) {
		String sql = "update TUser set login_time = utc_timestamp() where name = '" + username + "' and enabled = 1";
		try {
			daoMapper.modify(sql);
			return true;
		} catch (Exception e) {

			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 获取trackerList
	 */
	public List<Map<String, Object>> getTrackerList(String username) {
		// 返回trackerlist
		String listSql = "select d.defensive, su.name superusername, tlc.*, du.dname trackername, d.code trackerno, "
				+ " d.tracker_sim, d.head_portrait head_icon, d.contact, d.mobile1, "
				+ " d.mobile2, d.mobile3, d.remark, d.satellite, "
				+ " d.gps_interval, d.product_type, d.timezone, d.expired_time, d.conn_country,"
				+ " if(d.expired_time>now(),'false','true') expired,"
				+ " if(d.expired_time<NOW() + INTERVAL 1 MONTH,'true','false') onemonthexpired" + " from TDevice d "
				+ "     inner join TDeviceUser du on du.did = d.id and du.uid = (select id from TUser where name = '"
				+ username + "')" + "     left join TDeviceUser sdu on sdu.did = d.id and sdu.is_super_user = 1"
				+ "     left join TUser su on sdu.uid = su.id and su.enabled = 1"
				+ "     left join TLostCard tlc on d.id = tlc.id ";
		List<Map<String, Object>> trackerList = daoMapper.selectList(listSql);
		return trackerList;
	}

	/**
	 * 发送post请求获取结果
	 */
	public String getPost(RequestConfig requestConfig, String url, String encoding, List<NameValuePair> formparams,
			String language) {
		String result = "";
		// 创建默认的httpClient实例.
		CloseableHttpClient httpclient = HttpClients.createDefault();
		// 创建httppost
		HttpPost httppost = new HttpPost(url);
		httppost.setConfig(requestConfig);

		UrlEncodedFormEntity uefEntity;
		try {
			uefEntity = new UrlEncodedFormEntity(formparams, encoding);
			httppost.setEntity(uefEntity);
			// 设置头部信息，类似于K--V形式的
			httppost.setHeader("Accept-Language", language);
			// System.out.println("executing request " + httppost.getURI());
			// 执行post请求
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// 返回的结果
					result = EntityUtils.toString(entity, encoding);
					return result;
				}
			} finally {
				response.close();
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		} finally {
			// 关闭连接,释放资源
			try {
				httpclient.close();
			} catch (IOException e) {
				LogManager.error("error:" + e.getMessage());
			}
		}
		return result;
	}

	/**
	 * 获取session的值，通过key、并返回字符串类型
	 */
	public String getSession(HttpServletRequest req, String attribute) {
		Object o = req.getSession().getAttribute(attribute);
		return o == null ? "" : o.toString();
	}

	/**
	 * 设置自定义名字
	 */
	public boolean setCustomName(int trackerID, int userID, String name) {
		String sql = "UPDATE TDeviceUser SET dname='" + name + "' WHERE did ='" + trackerID + "' AND uid ='" + userID
				+ "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 添加绑定信息
	 */
	public boolean setTrackerInfoBySuperUser(int trackerID, String sim, String product_type, String remark,
			String mobile1, String mobile2, String mobile3) {
		String sql = "UPDATE TDevice SET tracker_sim ='" + sim + "',`product_type`='" + product_type + "',remark='"
				+ remark + "',mobile1='" + mobile1 + "',mobile2='" + mobile2 + "',mobile3='" + mobile3 + "' WHERE id='"
				+ trackerID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 添加绑定信息
	 */
	public boolean setTrackerInfo(int trackerID, String remark) {
		String sql = "UPDATE TDevice SET remark = '" + remark + "' WHERE id ='" + trackerID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 插入TCMD
	 */
	public boolean sendCmd(int trackerID, int userID, int type, int subtype, String cmdData) {
		String sql = "INSERT INTO TCmd(did, uid, `type`, subtype, `data`, updatetime) VALUES('" + trackerID + "','"
				+ userID + "','" + type + "','" + subtype + "','" + cmdData + "', CURRENT_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) == 1;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 判断设备是否有警情设置
	 */
	public boolean isHaveAlarmSet(int trackerID) {
		String sql = "SELECT did FROM `TAlarmSetting` WHERE `did`=" + trackerID;
		try {
			Object obj = daoMapper.findBy(sql);
			if (obj == null) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public boolean updateAlarmSet(String boundary, String voltage, String tow, String clipping, String speed,
			int speedValue, int speedTime, String sos, int userid, int deviceid, String vibration,
			Integer vibrationAspeed, Integer vibrationTime, String takeOff) {
		String sql = "UPDATE `TAlarmSetting` SET `boundary`='" + boundary + "',`voltage`='" + voltage + "',`tow`='"
				+ tow + "',`clipping`='" + clipping + "',`speed`='" + speed + "',`speedValue`='" + speedValue
				+ "',`speedTime`='" + speedTime + "',`sos`='" + sos + "',`uid`='" + userid + "' ,`vibration`='"
				+ vibration + "',`vibrationAspeed`='" + vibrationAspeed + "',`vibrationTime`='" + vibrationTime
				+ "',`takeOff`='" + takeOff + "' WHERE `did`='" + deviceid + "'";

		try {
			int result = daoMapper.modify(sql);

			if (result > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	public boolean insertAlarmSet(String boundary, String voltage, String tow, String clipping, String speed,
			int speedValue, int speedTime, String sos, int userid, int deviceid, String vibration,
			Integer vibrationAspeed, Integer vibrationTime, String takeOff) {
		String sql = "INSERT INTO `TAlarmSetting`(`did`,`boundary`,`voltage`,`tow`,`clipping`,`speed`,`speedValue`,`speedTime`,`sos`,`uid`,`vibration`,`vibrationAspeed`,`vibrationTime`,`takeOff`) VALUES ('"
				+ deviceid + "','" + boundary + "','" + voltage + "','" + tow + "','" + clipping + "','" + speed + "','"
				+ speedValue + "','" + speedTime + "','" + sos + "','" + userid + "','" + vibration + "','"
				+ vibrationAspeed + "','" + vibrationTime + "','" + takeOff + "')";

		try {
			int result = daoMapper.modify(sql);

			if (result > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	/**
	 * 判断上传的图像是否在一下标准格式 .jpg, .jpeg, .png, .gif, .bmp
	 */
	public boolean isHaedPortraitExtension(String fileExendName) {
		String[] allowExt = { ".jpg", ".jpeg", ".png", ".gif", ".bmp" };
		for (int i = 0; i < allowExt.length; i++) {
			if (allowExt[i].equalsIgnoreCase(fileExendName))
				return true;
		}
		return false;
	}

	/**
	 * 生成文件名
	 */
	public String createHeadPortraitName(String trackerNo) {
		Random rd = new Random();
		StringBuilder serial = new StringBuilder();
		serial.append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		serial.append(rd.nextInt(999999) % (999999 - 100000 + 1) + 100000);
		serial.append(trackerNo);
		return Base64.byteArrayToBase64(serial.toString().getBytes());
	}

	/**
	 * 保存广告
	 */
	public boolean saveAd(String relativePath, String url) {
		String sql = "INSERT INTO `TAd`(`img`,`url`)VALUES ('" + relativePath + "','" + url + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 更新用户表里面的密码
	 */
	public boolean updateUserForFindPassWord(String username, String password) {
		String sql = "UPDATE TUser SET password='" + password + "' WHERE name='" + username + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取用户记录的passwordverifycode, passwordishand
	 */
	public List<Map<String, Object>> getFindPass(String username) {
		String sql = "SELECT passwordverifycode , passwordishand FROM TUser WHERE `name`='" + username
				+ "' AND `enabled`=1";
		return daoMapper.selectList(sql);
	}

	/**
	 * 设置历史轨迹信息
	 * 
	 * public boolean setGEOfence(float lat, float lng, float radius, int
	 * userid, int trackerid) { String sql =
	 * "SELECT COUNT(*) FROM TAreaInfo WHERE did = " + trackerid; Object obj =
	 * daoMapper.findBy(sql); Object[] param = null; if (obj != null &&
	 * Integer.valueOf(obj.toString()) != 0) { sql =
	 * "update TAreaInfo set lat ='" + username + "', lng ='" + username + "', radius ='" + username + "', `enabled` = 1 where did ='" + username + "'"
	 * ; param = new Object[] { lat, lng, radius, trackerid }; } else { sql =
	 * "INSERT INTO `TAreaInfo`(`lat`,`lng`,`radius`,`did`,`uid`,`enabled`,`createtime`) VALUES('" + username + "','" + username + "','" + username + "','" + username + "','" + username + "', 1, NOW())"
	 * ; param = new Object[] { lat, lng, radius, trackerid, userid }; } try {
	 * return daoMapper.modify(sql, param) > 0; } catch (Exception e) {
	 * LogManager.error("error:" + e.getMessage()); return false; } }
	 */

	/**
	 * 获取电子围栏信息
	 */
	public List<Map<String, Object>> getGEOfence(String deviceCode, String userName) {
		String sql = "SELECT d.`code`,a.`lat`,a.`lng`,a.`radius` "
				+ " FROM TAreaInfo a INNER JOIN TDevice d ON a.`did` = d.`id`"
				+ " WHERE a.`enabled` = 1 AND d.`code` = '" + deviceCode + "' ORDER BY a.`createtime` DESC";
		return daoMapper.selectList(sql);
	}

	/**
	 * 取消电子围栏
	 */
	public int cancelGEOfence(String deviceCode) {
		String sql = "UPDATE TAreaInfo SET enabled = 0 " + "   WHERE did = (SELECT id FROM TDevice WHERE `code` ='"
				+ deviceCode + "')";

		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return 0;
		}
	}

	/**
	 * 删除提醒信息
	 */
	public boolean deleteClock(int id, int uid) {
		String sql = "DELETE FROM `TClock` WHERE id='" + id + "' and uid='" + uid + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 获取提醒信息
	 */
	public List<Map<String, Object>> getClock(int userid) {
		String sql = "SELECT * FROM `TClock` WHERE uid=" + userid + " AND isdelete=FALSE";
		return daoMapper.selectList(sql);
	}

	/**
	 * 获取提醒时间
	 */
	public List<Map<String, Object>> getClockTime(int cid) {
		String sql = "SELECT * FROM `TClockTime` WHERE cid=" + cid + " ORDER BY diabolo";
		return daoMapper.selectList(sql);
	}

	/**
	 * 保存走失卡信息
	 */
	public boolean saveLostCard(String mobile1, String mobile2, String mobile3, Map<String, Object> param) {
		int trackerid = Integer.valueOf(param.get("id").toString());
		String sql = "REPLACE INTO `TLostCard` " + " (`id`, " + " `height`, " + " `weight`, " + " `designation`, "
				+ " `sex`, " + " `steplength`, " + " `feature`, " + " `addr`, " + " `lostaddr`, " + " `age`, "
				+ " `petdesignation`, " + " `petsex`, " + " `petbreed`, " + " `petweight`, " + " `petage`, "
				+ " `petfeature`, " + " `petaddr`, " + " `petlinkmobile`, " + " `petothermobile`, " + " `petlostaddr`, "
				+ " `motorbikecode`, " + " `motorbiketype`, " + " `motorbikecc`, " + " `motorbikebrand`, "
				+ " `motorbikeset`, " + " `motorbikeyear`, "

				+ " `motorbikedesignation`, " + " `carcode`, " + " `carvin`, " + " `carengine`, " + " `carset`, "
				+ " `carbrand`, " + " `caryear`, " + " `cartype`, " + " `cargasoline`," + " `carmileage`,"
				+ " `caraudittime`," + " `cardesignation`) " + " VALUES ('" + param.get("id") + "','"
				+ param.get("height") + "','" + param.get("weight") + "','" + param.get("designation") + "','"
				+ param.get("sex") + "','" + param.get("steplength") + "','" + param.get("feature") + "','"
				+ param.get("addr") + "','" + param.get("lostaddr") + "','" + param.get("age") + "','"
				+ param.get("petdesignation") + "','" + param.get("petsex") + "','" + param.get("petbreed") + "','"
				+ param.get("petweight") + "','" + param.get("petage") + "','" + param.get("petfeature") + "','"
				+ param.get("petlostaddr") + "','" + param.get("motorbikecode") + "','" + param.get("motorbiketype")
				+ "','" + param.get("motorbikecc") + "','" + param.get("motorbikebrand") + "','"
				+ param.get("motorbikeset") + "','" + param.get("motorbikeyear") + "','"
				+ param.get("motorbikedesignation") + "','" + param.get("carcode") + "','" + param.get("carvin") + "','"
				+ param.get("carengine") + "','" + param.get("carset") + "','" + param.get("carbrand") + "','"
				+ param.get("caryear") + "','" + param.get("cartype") + "','" + param.get("cargasoline") + "','"
				+ param.get("carmileage") + "','" + param.get("caraudittime") + "','" + param.get("cardesignation")
				+ "')";
		try {
			daoMapper.modify(sql);
			sql = "UPDATE `TDevice` SET `mobile1` = '" + mobile1 + "',`mobile2` = '" + mobile2 + "',`mobile3` = '"
					+ mobile3 + "',`tracker_sim` = '" + param.get("sim") + "' WHERE `id` = '" + trackerid + "'";
			daoMapper.modify(sql);
			return true;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 获取走失卡信息
	 */
	public Map<String, Object> getLostCard(int trackerId) {
		// String sql =
		// "SELECT
		// t1.ranges,t1.`head_portrait`,t1.`gps_interval`,t1.`mobile1`,t1.`mobile2`,t1.`mobile3`,t2.*,t3.`lat`,t3.`lng`
		// FROM `TDevice` t1 "
		// +
		// "LEFT JOIN `TDeviceInfo` t2 ON t1.`id`=t2.`did` LEFT JOIN `TGpsData`
		// t3 ON t1.`id`=t3.`did` WHERE t1.`id`="
		// + trackerId + " ORDER BY t3.`collect_datetime` DESC LIMIT 1";

		String sql = "SELECT a.*,b.lat,b.lng  FROM ("
				+ " SELECT t1.`id`,t1.`ranges`,t1.`head_portrait`,t1.`gps_interval`,t1.`mobile1`,t1.`mobile2`,t1.`mobile3`,t2.* "
				+ "FROM `TDevice` t1 LEFT JOIN `TDeviceInfo` t2 ON t1.`id`=t2.`did` WHERE t1.`id`=" + trackerId
				+ ") AS a " + "LEFT JOIN (SELECT `did`,`lat`,`lng` FROM `TGpsData`  WHERE `did`= " + trackerId
				+ " ORDER BY `collect_datetime` DESC LIMIT 1 " + ") AS b ON a.id = b.did";

		return daoMapper.select(sql);
	}

	/**
	 * 获取app的最新的版本号
	 */
	public Map<String, Object> getLastVersion(String os) {
		String sql = "SELECT * FROM TVersion WHERE os = '" + os + "' ORDER BY `createtime` DESC LIMIT 0, 1";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list == null || list.size() == 0)
			return null;

		return list.get(0);
	}

	/**
	 * 获取设备信息
	 */
	public Map<String, Object> getTrackerInfo(int trackerID) {
		String sql = "SELECT * FROM TDevice WHERE id = " + trackerID;
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list == null || list.size() == 0)
			return null;

		return list.get(0);
	}

	/**
	 * 解除绑定
	 */
	public boolean cancelBinding(int trackerID) {
		String sql = "DELETE FROM TDeviceUser WHERE did ='" + trackerID + "' ";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 解除绑定
	 */
	public boolean cancelBinding(int trackerID, int userID) {
		String sql = "DELETE FROM TDeviceUser WHERE did ='" + trackerID + "' AND uid ='" + userID + "' ";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取TGpsData的数据
	 */
	public List<Map<String, Object>> getGPS(int did, String startDate, String endDate) {
		String sql = "SELECT * FROM TGpsData WHERE `did` = " + did + "  and  `collect_datetime` BETWEEN '" + startDate
				+ "' AND '" + endDate + "' AND deleteflag=0";
		return daoMapper.selectList(sql);
	}

	/**
	 * 查找可以登录的用户返回的结果，-1表示不存在这样的用户名和密码，1表示count（*）为0,0则表示存在
	 */
	public int login(String username, String password) {
		String sql = "SELECT COUNT(*) FROM TUser WHERE (`name` = '" + username + "' OR `mobile` ='" + username
				+ "') AND `password` = '" + password + "' AND enabled = 1";
		Object obj = daoMapper.findBy(sql);
		if (obj == null)
			return -1;

		int result = Integer.valueOf(obj.toString());
		if (result == 0)
			return 1;

		return 0;
	}

	/**
	 * 修改用户密码
	 */
	public boolean modifyUserPassword(String username, String password) {
		String sql = "UPDATE TUser SET `password` ='" + password + "' WHERE (`name` = '" + username
				+ "' OR `mobile` = '" + username + "' ) AND `enabled` = 1";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 修改设备sim卡号
	 */
	public boolean modifySim(String sim, String trackerNo, String username) {
		String sql = "UPDATE `TDevice` t1 SET t1.tracker_sim='" + sim + "' " + "WHERE t1.`device_sn`='" + trackerNo
				+ "' AND EXISTS(SELECT 1 FROM `TDeviceUser` t2 "
				+ "WHERE t2.`did`=t1.`id` AND EXISTS(SELECT 1 FROM `TUser` t3 WHERE t3.`id`=t2.`uid` AND t3.`name`='"
				+ username + "'))";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取我的设备，和授权设备
	 */
	public Map<String, Object> getAccount(String username) {
		Map<String, Object> map = new HashMap<>();
		String sql = "SELECT t1.device_sn,t1.tracker_sim FROM TDevice t1 WHERE EXISTS(SELECT 1 FROM TDeviceUser t2 JOIN TUser t3 ON t2.`uid`=t3.`id` WHERE t3.name='"
				+ username + "' AND t2.is_super_user=1 AND t1.`id`=t2.`did`)";
		List<Map<String, Object>> mytrackerList = daoMapper.selectList(sql);
		sql = "SELECT t1.device_sn,t1.tracker_sim FROM TDevice t1 WHERE EXISTS(SELECT 1 FROM TDeviceUser t2 JOIN TUser t3 ON t2.`uid`=t3.`id` WHERE t3.name='"
				+ username + "' AND t2.is_super_user=0 AND t1.`id`=t2.`did`)";
		List<Map<String, Object>> authList = daoMapper.selectList(sql);

		map.put("mytracker", mytrackerList);
		map.put("authtracker", authList);

		return map;
	}

	/**
	 * 保存报警开关设置
	 */
	public void saveToggle(int trackerId, String boundary, String voltage, String tow, String clipping, String speed,
			String sos) {
		String sql = "REPLACE INTO `TAlarmToggle`(`id`,`boundary`,`voltage`,`tow`,`clipping`,`speed`,`sos`) VALUES('"
				+ trackerId + "','" + boundary + "','" + voltage + "','" + tow + "','" + clipping + "','" + speed
				+ "','" + sos + "')";
		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
	}

	/**
	 * 获取报警开关设置
	 */
	public Map<String, Object> getToggle(int trackerId) {
		Map<String, Object> defaultmap = new HashMap<String, Object>();
		defaultmap.put("did", trackerId);
		defaultmap.put("boundary", 1);
		defaultmap.put("voltage", 1);
		defaultmap.put("tow", 1);
		defaultmap.put("clipping", 1);
		defaultmap.put("speed", 1);
		defaultmap.put("speedValue", 120);
		defaultmap.put("speedTime", 60);
		defaultmap.put("sos", 1);
		defaultmap.put("create_time", null);
		defaultmap.put("uid", null);
		defaultmap.put("vibration", 0);
		defaultmap.put("takeOff", 1);

		String sql = "SELECT * FROM TAlarmSetting WHERE did=" + trackerId;
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		return list != null && list.size() != 0 ? list.get(0) : defaultmap;
	}

	/**
	 * 获取警情
	 */
	public List<Map<String, Object>> getAlarmInfo(int trackerID, String startTime, String endTime, String type) {
		try {
			SimpleDateFormat fmt_String2Date = new SimpleDateFormat("yyyy-MM-dd");
			Date startDate = fmt_String2Date.parse(startTime);
			Date endDate = fmt_String2Date.parse(endTime);

			GregorianCalendar tmpCalendar = new GregorianCalendar();

			tmpCalendar.setTime(startDate);
			GregorianCalendar startCalendar = new GregorianCalendar(tmpCalendar.get(Calendar.YEAR),
					tmpCalendar.get(Calendar.MONTH), tmpCalendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

			tmpCalendar.setTime(endDate);
			GregorianCalendar endCalendar = new GregorianCalendar(tmpCalendar.get(Calendar.YEAR),
					tmpCalendar.get(Calendar.MONTH), tmpCalendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

			SimpleDateFormat fmt_Date2String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startTime = fmt_Date2String.format(startCalendar.getTime());
			endTime = fmt_Date2String.format(endCalendar.getTime());

			int ranges = getTypeOfTracker(trackerID);

			Object[] obj;
			String sql = "SELECT ad.id,ad.did, DATE_ADD(ad.collect_datetime, INTERVAL tu.`timezone` SECOND) AS dtime, ad.type, ad.data, "
					+ "FORMAT(ad.`lat`,5) lat, FORMAT(ad.`lng`,5) lng, ad.speed,ad.readstatus "
					+ "  FROM TAlarmData ad "
					+ "  LEFT JOIN TDevice d ON ad.did = d.`id` LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 LEFT JOIN TUser tu ON tu.`id`=tdu.`uid`"
					+ "  WHERE ad.did ='" + trackerID + "' AND ad.collect_datetime BETWEEN DATE_ADD('" + startTime
					+ "', INTERVAL -tu.`timezone` SECOND)  " + "  AND DATE_ADD('" + endTime
					+ "', INTERVAL -tu.`timezone` SECOND) AND ad.deletestatus=0 ";

			if (Integer.parseInt(type) == 0 && ranges == 6) {// 29越界报警 18低电报警，
																// 81
																// 超速告警，87:拖吊告警
																// 101:OBD剪线告警
				sql += " AND (ad.type=29 or ad.type=18 or ad.type=81 or ad.type=87 or ad.type=96 or ad.type=95 or ad.type=101)  ";
			}
			if (Integer.parseInt(type) != 0) {
				sql += " AND ad.type='" + type + "'  ";
			} else {
			}

			sql += " ORDER BY ad.collect_datetime DESC ";

			return daoMapper.selectList(sql);
		} catch (ParseException e) {
			LogManager.error("error:" + e.getMessage());
			return new ArrayList<Map<String, Object>>();
		}
	}

	/**
	 * 保存意见反馈
	 */
	public boolean saveIdea(String deviceSn, String username, String title, String content) {
		String sql = "INSERT INTO `TSuggestion`(`user_name`,`device_sn`,`titile`,`content`,`create_time`)VALUES ('"
				+ username + "','" + deviceSn + "','" + title + "','" + content + "',UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 保存用户TOKEN
	 */
	public boolean saveUserToken(String token, String username, int versions) {
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
	 * 
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> getGPSInterval(String deviceSn) {
		String sql = "SELECT d.gps_interval FROM TDevice d WHERE `device_sn` = '" + deviceSn + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 设置GPS上传频率
	 */
	public boolean saveGPSInterval(int trackerID, String interval) {
		String sql = "UPDATE TDevice SET gps_interval = '" + interval + "' WHERE id = '" + trackerID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 删除GPS上传频率(恢复出场设置)
	 */
	public boolean deleteGPSInterval(int trackerID) {
		String sql = "UPDATE TDevice SET gps_interval = '60' WHERE id = '" + trackerID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 设置时区
	 */
	public boolean setTimezone(int userId, String timezone, int timezoneId, int timezoneCheck) {
		String sql = "UPDATE `TUser` SET timezone = '" + timezone + "',timezone_id = '" + timezoneId
				+ "',timezone_check = '" + timezoneCheck + "' WHERE id='" + userId + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 设置推送提醒方式，在tuser表中，alert_mode，默认1，双开，2，双关，3，震动开，响铃关，4，震动关，响铃开
	 */
	public boolean setAlertMode(String alert_mode, int userId) {
		String sql = "UPDATE `TUser` SET alert_mode='" + alert_mode + "' WHERE id='" + userId + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取推送提醒方式
	 */
	public int getAlertWay(String username) {
		String sql = "SELECT alert_mode FROM TUser WHERE `name`='" + username + "'";
		Object obj = daoMapper.findBy(sql);
		return obj == null ? 0 : Integer.valueOf(obj.toString());
	}

	/**
	 * 获取设备所属类型 产品类型 以后暂停使用use字段 1.个人，2.宠物，3.汽车，4.摩托车
	 */
	public int getTypeOfTracker(int trackerID) {
		String sql = "SELECT `ranges` FROM TDevice WHERE id = " + trackerID;
		Object obj = null;
		obj = daoMapper.findBy(sql);
		return obj == null ? -1 : Integer.valueOf(obj.toString());
	}

	/**
	 * 锁定车辆
	 */
	public boolean lockVehicle(int trackerID, int p) {
		String sql = "UPDATE TDevice SET defensive ='" + p + "' WHERE id ='" + trackerID + "' ";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取锁定状态
	 */
	public Map<String, Object> getdefensivestatus(int trackerID) {
		String sql = "SELECT `defensive` FROM `TDevice` WHERE `id`=" + trackerID;
		return daoMapper.select(sql);
	}

	/**
	 * 获取注册时候的服务器编号
	 */
	public int getServerNo(String username) {
		String sql = "SELECT connid FROM `TUser` WHERE name= '" + username + "'";
		Object obj = null;
		obj = daoMapper.findBy(sql);
		return obj == null ? -1 : Integer.valueOf(obj.toString());
	}

	/**
	 * 保存订单
	 * 
	 * @param tradeNo
	 * @param deviceSn
	 * @param username
	 * @param payType
	 * @param payer
	 * @param price
	 * @param currencyUnit
	 * @param state
	 * @param orderpackageid
	 * @return
	 */
	public boolean saveOrder(String tradeNo, String deviceSn, String username, String payType, String payer,
			String price, String currencyUnit, String remark, Integer month, String orderpackageid) {
		// 获取设备号
		String devicesn = deviceSn;
		if (devicesn == null || devicesn.isEmpty()) {
			return false;
		}

		if (username == null || username.isEmpty()) {
			return false;
		}

		long orderNo = System.currentTimeMillis();
		Date createTime = new Date();
		String sqlsaveorder = "INSERT INTO `order`(`order_no`,`trade_no`,`device_sn`,`user`,"
				+ "`pay_type`,`payer`,`price`,`currency_unit`,`status`,`remark`,month,`create_time`,`order_package_id`) "
				+ "VALUES ('" + orderNo + "','" + tradeNo + "','" + deviceSn + "','" + username + "','" + payType
				+ "','" + payer + "','" + price + "','" + currencyUnit + "','1','" + remark + "','" + month + "','"
				+ createTime + "','" + orderpackageid + "')";

		try {
			return daoMapper.modify(sqlsaveorder) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 获取订单日志相关信息
	 * 
	 * @param tradeno
	 * @param orderpackageid
	 * @param devicesn
	 * @param userid
	 * @return
	 */
	public Map<String, Object> getOrderLogInfo(String tradeno, int orderpackageid, String devicesn, int userid) {
		if (tradeno == null || tradeno.isEmpty()) {
			return null;
		}

		if (devicesn == null || devicesn.isEmpty()) {
			return null;
		}

		if (orderpackageid <= 0) {
			return null;
		}

		// 获取套餐信息
		String sqlselectorderpack = "SELECT * FROM `order_package` WHERE `status`=0 AND `id`=" + orderpackageid;
		Map<String, Object> map = daoMapper.select(sqlselectorderpack);
		if (map == null || map.size() < 1) {
			return null;
		}

		// 返回结果
		Map<String, Object> reruenMap = new HashMap<String, Object>();
		reruenMap.put("trade_no", tradeno);

		// 获取设备免费过期时间或上次过期时间
		String sqlselectexpiredtime = "SELECT `expired_time` FROM `TDevice` WHERE `device_sn`='" + devicesn + "'";
		Object expiredtime = daoMapper.findBy(sqlselectexpiredtime);
		if (expiredtime == null) {
			// 添加默认过期日期
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MILLISECOND, -(c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)));

			int timezone = getUserTimezone(userid);
			c.add(Calendar.MILLISECOND, timezone * 1000);

			GregorianCalendar localdate = new GregorianCalendar();
			localdate.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

			localdate.add(Calendar.MILLISECOND, -timezone * 1000);
			localdate.add(Calendar.YEAR, Integer.valueOf(env.getProperty("default.expired.time")));

			expiredtime = localdate.getTime();
		}

		Date expiredtimedate = (Date) expiredtime;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		GregorianCalendar startdate = new GregorianCalendar();
		startdate.setTime(expiredtimedate);
		startdate.add(Calendar.SECOND, 1);
		String startdateString = fmt.format(startdate.getTime());
		reruenMap.put("fee_start_time", startdateString);

		int months = Integer.parseInt(map.get("month").toString());
		reruenMap.put("month", months);
		int days = months * 30;
		reruenMap.put("buy_days", days);

		GregorianCalendar enddate = new GregorianCalendar();
		enddate.setTime(expiredtimedate);
		enddate.add(Calendar.DAY_OF_YEAR, days);
		String enddateString = fmt.format(enddate.getTime());
		reruenMap.put("fee_end_time", enddateString);

		reruenMap.put("name", map.get("name"));
		reruenMap.put("communication_fee", map.get("communication_fee"));
		reruenMap.put("rent_fee", map.get("rent_fee"));
		reruenMap.put("serve_fee", map.get("serve_fee"));

		return reruenMap;
	}

	/**
	 * 更改订单当支付宝付款成功之后回调改变支付状态
	 */
	public boolean saveOrder(String ordernum, Integer ordertype) {
		String sql = "select status from order where trade_no='" + ordernum + "'";
		Object status = daoMapper.findBy(sql);
		if ((status != null && Integer.parseInt(status.toString()) == 1)
				|| (status != null && Integer.parseInt(status.toString()) == 3)) {
			return false;
		}

		sql = "UPDATE `order`  SET `status`='" + ordertype + "' WHERE trade_no='" + ordernum + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 保存订单当微信下单的时候
	 */
	public boolean saveOrderForWX(String ordernum, String trackerid, String body, String payinfo, Integer ordertype,
			String price) {
		String sql = "INSERT INTO `TOrderForWX`" + " (`ordernum`," + " `trackerid`," + "   `body`," + "   `payinfo`,"
				+ "   `orderdate`," + "   `ordertype`,price)" + " VALUES ('" + ordernum + "'," + "        '" + trackerid
				+ "'," + "        '" + body + "'," + "        '" + payinfo + "'," + "        now()," + "        '"
				+ ordertype + "','" + price + "');";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 更改订单当微信付款成功之后回调改变支付状态
	 */
	public String saveOrderForWX(String ordernum, Integer ordertype) {
		String sql = "select ordertype from TOrderForWX where ordernum='" + ordernum + "'";
		Object selectType = daoMapper.findBy(sql);
		if (selectType != null && Integer.parseInt(selectType.toString()) == Constanct.ORDER_SUCCESS)
			return "";
		sql = "UPDATE `TOrderForWX`  SET `ordertype`='" + ordertype + "' WHERE ordernum='" + ordernum + "'";
		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		sql = "select trackerid from TOrderForWX where ordernum='" + ordernum + "'";

		Object trackerid = daoMapper.findBy(sql);
		return trackerid != null ? trackerid.toString() : "";
	}

	/**
	 * 当用户购买成功的时候将过期时间往后面推迟一年
	 */
	public boolean addExpiredForDevice(String tradeno, Date expiredTimeDe) {

		String sql = "UPDATE `TDevice` SET expired_time_de='" + expiredTimeDe + "' WHERE device_sn='" + tradeno + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 根据设备ID 查询信息卡
	 * 
	 * @param deviceId
	 */
	public Map<String, Object> getDeviceInfo(int deviceId) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT d.* ").append("FROM TDeviceInfo d ").append("WHERE d.`did` = ").append(deviceId);
		return daoMapper.select(sb.toString());
	}

	public Map<String, Object> getDevice(int deviceId) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT d.* ").append("FROM TDevice d ").append("WHERE d.`id` = ").append(deviceId);
		return daoMapper.select(sb.toString());
	}

	public Map<String, Object> getDeviceInfoForShare(int deviceId) {
		String sql = "SELECT d.`nickname`,d.`mobile1`,d.`mobile2`,d.`mobile3`,d.`head_portrait`,d.`ranges`,"
				+ "di.* FROM `TDevice` AS d LEFT JOIN `TDeviceInfo` AS di ON di.`did`=d.`id` WHERE d.`id`=" + deviceId;

		return daoMapper.select(sql);
	}

	/**
	 * 统计总里程
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public float getSumMileage(String deviceSn, String startDate, String endDate) {
		StringBuffer str = new StringBuffer();
		str.append("select SUM(mileage) from `TTrip_Data` where `device_sn` =  '" + deviceSn + "'");

		if (null != startDate) {
			str.append(" and start_time > '" + startDate + "'");
		}

		if (null != endDate) {
			str.append(" and end_time < '" + endDate + "'");
		}

		Object obj = daoMapper.findBy(str.toString());
		if (obj == null) {
			return 0;
		}
		return Float.parseFloat(obj.toString());
	}

	/**
	 * 统计油耗
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public float getSumFuel(String deviceSn, String startDate, String endDate) {
		StringBuffer str = new StringBuffer();
		str.append("select SUM(fuel_consumption) from `TTrip_Data` where `device_sn` =  '" + deviceSn + "'");

		if (null != startDate) {
			str.append(" and start_time > '" + startDate + "'");
		}

		if (null != endDate) {
			str.append(" and end_time < '" + endDate + "'");
		}

		Object obj = daoMapper.findBy(str.toString());
		if (obj == null) {
			return 0;
		}
		return Float.parseFloat(obj.toString());
	}

	/**
	 * 统计时长
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public float getSumTime(String deviceSn, String startDate, String endDate) {
		StringBuffer str = new StringBuffer();
		str.append("select SUM(TIMESTAMPDIFF(MINUTE,start_time,end_time)) FROM `TTrip_Data` where `device_sn` =  '"
				+ deviceSn + "'");

		if (null != startDate) {
			str.append(" and start_time > '" + startDate + "'");
		}

		if (null != endDate) {
			str.append(" and end_time < '" + endDate + "'");
		}
		Object obj = daoMapper.findBy(str.toString());
		if (obj == null) {
			return 0;
		}
		return Float.parseFloat(obj.toString());
	}

	public void main(String[] args) throws Exception {

	}

	public List<Map<String, Object>> getorderpackage(Integer orgId) {
		String sql = "SELECT  id as orderPackageId,name,month,`serve_fee`,`content`,`currency_unit` FROM `order_package` WHERE `status`=0 and serve_fee > 0 and org_id="
				+ orgId + " ORDER BY `id` ASC";
		return daoMapper.selectList(sql);
	}

	public boolean addphonebook(int deviceID, String deviceSn, String phone, int adminindex, String photo) {
		String sqlselect = "SELECT * FROM `TPhoneBook` WHERE `did`=" + deviceID;
		Map<String, Object> map = daoMapper.select(sqlselect);

		if (map != null && !map.isEmpty()) {
			String sqlupdate = "UPDATE `TPhoneBook` SET `device_sn`='" + deviceSn + "',`phone`='" + phone
					+ "',`adminIndex`='" + adminindex + "',photo='" + photo + "' WHERE `did`='" + deviceID + "'";

			try {
				return daoMapper.modify(sqlupdate) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			String sqlinsert = "INSERT INTO `TPhoneBook`(`did`,`device_sn`,`phone`,`adminIndex`,photo) VALUES('"
					+ deviceID + "','" + deviceSn + "','" + phone + "','" + adminindex + "','" + photo + "')";

			try {
				return daoMapper.modify(sqlinsert) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	public void updateurgencytel(String phone1, String phone2, String phone3, String deviceSn) {
		String sql = "UPDATE `TDevice` SET `mobile1`='" + phone1 + "',`mobile2`='" + phone2 + "',`mobile3`='" + phone3
				+ "' WHERE `device_sn`='" + deviceSn + "'";

		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
	}

	public Map<String, Object> getallphone(int deviceID) {
		String sql = "SELECT IFNULL(B.`hardware`,'0') AS hardware,IFNULL(A.`did`,'" + deviceID + "') AS did,"
				+ "IFNULL(A.`device_sn`,B.`device_sn`) AS device_sn,"
				+ "IFNULL(A.`phone`,CASE WHEN B.hardware>=15 THEN ':,:,:,:,:,:,:,:,:,:' ELSE ',,,,,,,,,' END) AS phone,"
				+ " photo, " + "IFNULL(A.`adminIndex`,'1') AS adminIndex FROM `TDevice` AS B "
				+ "LEFT JOIN `TPhoneBook` AS A ON B.`id`=A.`did` WHERE B.`id`=" + deviceID;
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.isEmpty()) {
			return null;
		} else {
			return map;
		}
	}

	public boolean savetimeboot(int did, String deviceSn, String enable, String boottime, String shutdowntime,
			String repeatday) {
		String selectSql = "SELECT * FROM `TTimeBoot` WHERE `did`=" + did;
		List<Map<String, Object>> list = daoMapper.selectList(selectSql);
		if (list != null && !list.isEmpty()) {
			String updateSql = "UPDATE `TTimeBoot` SET `device_sn`='" + deviceSn + "',`enable`='" + enable
					+ "',`boottime`='" + boottime + "',`shutdowntime`='" + shutdowntime + "',`repeatday`='" + repeatday
					+ "' WHERE did='" + did + "'";
			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LogManager.error("error:" + e.getMessage());

				return false;
			}
		} else {
			String insertSql = "INSERT INTO `TTimeBoot`(`did`,`device_sn`,`enable`,`boottime`,`shutdowntime`,`repeatday`) VALUES('"
					+ did + "','" + deviceSn + "','" + enable + "','" + boottime + "','" + shutdowntime + "','"
					+ repeatday + "')";
			try {
				return daoMapper.modify(insertSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	public Map<String, Object> gettimeboot(int did) {
		String selectSql = "SELECT * FROM `TTimeBoot` WHERE `did`=" + did;
		Map<String, Object> map = daoMapper.select(selectSql);
		if (map != null && map.size() >= 1) {
			return map;
		} else {
			return null;
		}
	}

	public boolean savecoursedisabletime(int did, String deviceSn, String enable, String amstarttime, String amendtime,
			String tmstarttime, String tmendtime, String repeatday, String starttime3, String endtime3) {
		String selectSql = "SELECT * FROM `TCourseDisableTime` WHERE `did`=" + did;
		List<Map<String, Object>> list = daoMapper.selectList(selectSql);
		if (list != null && !list.isEmpty()) {
			String updateSql = "UPDATE `TCourseDisableTime` SET `device_sn` = '" + deviceSn + "',`enable` = '" + enable
					+ "',`amstarttime` = '" + amstarttime + "'," + "`amendtime` = '" + amendtime + "',`tmstarttime` = '"
					+ tmstarttime + "',`tmendtime` = '" + tmendtime + "',`repeatday` = '" + repeatday
					+ "',`starttime3` = '" + starttime3 + "',`endtime3` = '" + endtime3 + "' WHERE `did` = '" + did
					+ "'";
			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LogManager.error("error:" + e.getMessage());

				return false;
			}
		} else {
			String insertSql = "INSERT INTO `TCourseDisableTime`(`did`,`device_sn`,`enable`,`amstarttime`,`amendtime`,`tmstarttime`,`tmendtime`,`repeatday`,`starttime3`,`endtime3`) VALUES ('"
					+ did + "','" + deviceSn + "','" + enable + "','" + amstarttime + "','" + amendtime + "','"
					+ tmstarttime + "','" + tmendtime + "','" + repeatday + "','" + starttime3 + "','" + endtime3
					+ "')";
			try {
				return daoMapper.modify(insertSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}
	}

	public Map<String, Object> getcoursedisabletime(int did) {
		String selectSql = "SELECT * FROM `TCourseDisableTime` WHERE `did`=" + did;
		Map<String, Object> map = daoMapper.select(selectSql);
		if (map != null && map.size() >= 1) {
			return map;
		} else {
			return null;
		}
	}

	public Map<String, Object> getTsleepinfo(String deviceSn) {
		String selectSql = "SELECT * from Tsleepinfo where  device_sn ='" + deviceSn + "'";
		Map<String, Object> map = daoMapper.select(selectSql);
		if (map != null && map.size() >= 1) {
			return map;
		} else {
			return null;
		}
	}

	public int getusertimezone(int id) {
		String selectSql = "SELECT `timezone` FROM `TUser` WHERE `id`=" + id;
		Map<String, Object> map = daoMapper.select(selectSql);
		if (map == null || map.size() == 0) {
			return 0;
		} else {
			return (int) map.get("timezone");
		}
	}

	public int getdeviceprtocoltype(String deviceSn) {
		String sql = "SELECT `protocol_type` FROM `TDevice` WHERE `device_sn`='" + deviceSn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		if (map != null && !map.isEmpty()) {
			return map.get("protocol_type") == null ? 0 : Integer.parseInt(map.get("protocol_type").toString());
		} else {
			return 0;
		}
	}

	public boolean setDeviceTimezone(int trackerID, int timezone, int timezoneid, int language) {
		String sql = "UPDATE `TDevice` SET `timezone`=" + timezone + ",`timezoneid`=" + timezoneid + ",`language`="
				+ language + " WHERE `id`=" + trackerID;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public Map<String, Object> getDeviceTimezone(int trackerID) {
		String sql = "SELECT t.`timezone`,t.`timezoneid`,t.`language`,p.`language` as languageType FROM `TDevice` t "
				+ " left join `TDevice_Protocol` p on t.`product_type`=p.`product_type` WHERE t.`id`=" + trackerID;
		Map<String, Object> map = daoMapper.select(sql);
		if (map != null && map.size() >= 1) {
			return map;
		} else {
			return null;
		}
	}

	public void deleteotherdata(String deviceSn, int trackerID) {
		if (getdeviceprtocoltype(deviceSn) == 1 || getdeviceprtocoltype(deviceSn) == 5
				|| getdeviceprtocoltype(deviceSn) == 6 || getdeviceprtocoltype(deviceSn) == 7
				|| getdeviceprtocoltype(deviceSn) == 8) // 手表
		{
			try {
				String deleteTDeviceUser = "DELETE FROM TDeviceUser WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTDeviceUser);

				String deleteTPhoneBook = "DELETE FROM `TPhoneBook` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTPhoneBook);

				String deleteTTimeBoot = "DELETE FROM `TTimeBoot` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTTimeBoot);

				String deleteTCourseDisableTime = "DELETE FROM `TCourseDisableTime` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTCourseDisableTime);

				// 修改设备计步设置
				String updateDeviceStep = "update TDevice set step=0 where id=" + trackerID;
				daoMapper.modify(updateDeviceStep);

				// 删除770闹钟
				String deleteTDeviceClock = "DELETE FROM `TDevice_clock` WHERE `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteTDeviceClock);

				String deleteDeviceMobel = "UPDATE `TDevice` SET `tracker_sim`='',`head_portrait`='',"
						+ "`mobile1`='',`mobile2`='',`mobile3`='',`nickname`='',`timezone`=0,first_bind_date=NULL,`timezoneid`=31,`gps_interval`=null,`center_mobile`='' WHERE `id`="
						+ trackerID;
				daoMapper.modify(deleteDeviceMobel);

				// 删除围栏信息
				String deleteDefence = "DELETE FROM `TAreaInfo` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteDefence);

				// 删除警情开关数据
				String deleteTAlarmSetting = "DELETE FROM `TAlarmSetting` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTAlarmSetting);

				// 删除信息卡 add by ljl 20160520
				String deleteDeviceInfo = "DELETE FROM `TDeviceInfo` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteDeviceInfo);

				// 删除历史轨迹 add by ljl 20160520
				String deleteHistoricalGPSData = "UPDATE `TGpsData` SET `deleteflag`=1 WHERE `did`=" + trackerID
						+ " AND `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteHistoricalGPSData);

				// 逻辑删除警情信息 add by ljl 20160520
				String deleteAlarmData = "UPDATE `TAlarmData` SET `deletestatus`=1 WHERE `did`=" + trackerID
						+ " AND `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteAlarmData);

				// 更新解绑时间
				String unbindTime = "UPDATE `binding_log` b  SET b.`unbind_time` = UTC_TIMESTAMP() WHERE b.device_sn = '"
						+ deviceSn + "' " + "ORDER BY b.bind_time DESC LIMIT 1";
				daoMapper.modify(unbindTime);

				// 删除 990 吃药提醒
				String deleteMedicationClock = "DELETE FROM `medication_clock` WHERE `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteMedicationClock);

				// 修改990情景模式
				String updateMedication = "update TDevice set profile=1 where id=" + trackerID;
				daoMapper.modify(updateMedication);

				String deviceContact = "DELETE FROM `device_contact` where `device_sn` = '" + deviceSn + "'";
				daoMapper.modify(deviceContact);

			} catch (Exception ex) {
				ex.printStackTrace();
				//
			}
		} else {
			try {
				String deleteTDeviceUser = "DELETE FROM TDeviceUser WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTDeviceUser);

				// 删除信息卡 add by ljl 20160520
				String deleteDeviceInfo = "DELETE FROM `TDeviceInfo` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteDeviceInfo);

				// 删除警情开关数据
				String deleteTAlarmSetting = "DELETE FROM `TAlarmSetting` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteTAlarmSetting);

				// 删除历史轨迹 add by ljl 20160520
				String deleteHistoricalGPSData = "UPDATE `TGpsData` SET `deleteflag`=1 WHERE `did`=" + trackerID
						+ " AND `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteHistoricalGPSData);

				// 逻辑删除警情信息 add by ljl 20160520
				String deleteAlarmData = "UPDATE `TAlarmData` SET `deletestatus`=1 WHERE `did`=" + trackerID
						+ " AND `device_sn`='" + deviceSn + "'";
				daoMapper.modify(deleteAlarmData);
				// 更新设备信息
				String updateDevice = "UPDATE `TDevice` SET `tracker_sim`='',`head_portrait`='',`mobile1`='',`mobile2`='',`mobile3`='',`nickname`='',`timezone`=0,`timezoneid`=31,`gps_interval`=null WHERE `device_sn`='"
						+ deviceSn + "'";
				daoMapper.modify(updateDevice);
				// 删除围栏信息
				String deleteDefence = "DELETE FROM `TAreaInfo` WHERE `did`=" + trackerID;
				daoMapper.modify(deleteDefence);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public boolean setSleepInfo(String enable, String boottime, String shutdowntime, String device_sn) {
		String selectSql = "select * from Tsleepinfo where device_sn = '" + device_sn + "'";
		if (daoMapper.selectList(selectSql).size() > 0) {
			String updateSql = "update Tsleepinfo  set  enable = '" + enable + "' ,boottime='" + boottime
					+ "' ,shutdowntime ='" + shutdowntime + "' where device_sn = '" + device_sn + "' ";

			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}

		// 不存在则插入
		String insertSql = "insert into Tsleepinfo (device_sn,enable,boottime,shutdowntime) values ('" + device_sn
				+ "','" + enable + "','" + boottime + "','" + shutdowntime + "')";

		try {
			return daoMapper.modify(insertSql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public String getDeviceSuperUserName(String device_Sn) {
		String sql = "SELECT `name` FROM `TUser` AS A INNER JOIN `TDeviceUser` AS B"
				+ " ON A.`id`=B.`uid` AND B.`is_super_user`=1 INNER JOIN `TDevice` AS C"
				+ " ON B.`did`=C.`id` WHERE C.`device_sn`='" + device_Sn + "' ";
		Object ret = daoMapper.findBy(sql);

		if (ret == null) {
			return "";
		} else {
			return ret.toString();
		}
	}

	// ############################################################
	// 航通3.0国内版专用
	/**
	 * 验证用户账号密码和是否通过邮箱认证
	 * 
	 * @param username
	 * @param encryptPassword
	 * @return
	 */
	public int validateUserPasswordAndEmailCN(String username, String encryptPassword) {
		String sql = "SELECT is_email_verify FROM TUser WHERE (`name` = '" + username + "' OR `mobile`= '" + username
				+ "') AND `password` = '" + encryptPassword + "' AND enabled = 1";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.get("is_email_verify") == null)
			return -1;

		sql = "update TUser set login_err_count = 0 where name ='" + username + "'";
		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}

		return Integer.parseInt(String.valueOf(map.get("is_email_verify")));
	}

	/**
	 * 更新用户登录时间
	 */
	public boolean updateLoginTimeCN(String username) {
		String sql = "update TUser set login_time = utc_timestamp() where (name = '" + username + "' OR mobile = '"
				+ username + "' )and enabled = 1";
		try {
			return daoMapper.modify(sql) > 0 ? true : false;
		} catch (Exception e) {
			LogManager.error("username:" + username + " error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 判断用户是否已存在
	 * 
	 * @param username
	 * @return
	 */
	public boolean isExistsUserCN(String username) {
		String sql = "SELECT COUNT(*) from TUser WHERE (`mobile`='" + username + "' OR `name`='" + username
				+ "') AND `enabled`=1";
		return daoMapper.count(sql) > 0;
	}

	/**
	 * 注册用户
	 * 
	 * @param username
	 * @param password
	 * @param serverno
	 * @param timezone
	 * @return
	 */
	public boolean registerUserCN(String username, String password, int serverno, int timezone, String timezoneid,
			int isCustomizedApp) {
		String sql = "INSERT INTO TUser(`password`, is_email_verify, `enabled`, create_time, connid, timezone,timezone_id,isCustomizedApp,`mobile`) VALUES('"
				+ password + "',1,1,UTC_TIMESTAMP(),'" + serverno + "','" + timezone + "','" + timezoneid + "','"
				+ isCustomizedApp + "','" + username + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 获取User表ID
	 */
	public int getUserIdCN(String username) {
		// 根据用户名获取当前系统的主见ID
		String sql = "SELECT id FROM TUser WHERE (`mobile`='" + username + "' OR `name`='" + username
				+ "')  AND `enabled`=1";
		return daoMapper.findBy(sql) == null ? 0 : Integer.valueOf(daoMapper.findBy(sql).toString());
	}

	/**
	 * 更新用户密码
	 */
	public boolean updateUserPasswordCN(String username, String password) {
		// 根据用户名获取当前系统的主见ID
		String sql = "UPDATE `TUser` SET `password`='" + password + "' WHERE `mobile`='" + username + "' OR `name`='"
				+ username + "'";
		try {
			if (daoMapper.modify(sql) > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

			return false;
		}
	}

	// 获取设备围栏数
	public int getDeviceGeoFenceAmount(int deviceID) {
		String sql = "SELECT COUNT(areaid) FROM `TAreaInfo` WHERE `did` =" + deviceID;
		Object obj = daoMapper.findBy(sql);
		if (obj == null) {
			return 0;
		}
		return Integer.parseInt(obj.toString());
	}

	// 获取设备围栏ID
	public String getDeviceGeoFenceAreaid(int deviceID) {
		String sql = "SELECT areaid FROM `TAreaInfo` WHERE `did` =" + deviceID;
		Object obj = daoMapper.findBy(sql);
		if (obj == null) {
			return null;
		}
		return obj.toString();
	}

	public boolean deleteAlarmInfo(String alarmID) {
		// String sql = "DELETE FROM talarmdata WHERE id in("+alarmID+")";
		String sql = "UPDATE TAlarmData SET deletestatus=1 WHERE id in('" + alarmID + "')";
		try {
			return daoMapper.modify(sql) > 0 ? true : false;
		} catch (Exception e) {
			LogManager.error("username:" + alarmID + " error:" + e.getMessage());
		}
		return false;
	}

	public boolean setGeoFenceCN(String deviceID, String userID, String lat, String lng, String radius,
			String defencename, String defencestatus) {
		String sql = "INSERT INTO TAreaInfo(lat, lng, radius, did, uid, enabled, create_time,defencename) " + "VALUES('"
				+ lat + "', '" + lng + "', '" + radius + "', '" + deviceID + "', '" + userID + "', 1, UTC_TIMESTAMP(),'"
				+ defencename + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean updateGeoFenceCN(String areaid, String deviceID, String userID, String lat, String lng,
			String radius, String defencename, String defencestatus) {
		String sql = "UPDATE TAreaInfo SET lat = '" + lat + "', lng = '" + lng + "', radius = '" + radius + "', uid = '"
				+ userID + "', create_time = UTC_TIMESTAMP(),defencename = '" + defencename + "' WHERE did = '"
				+ deviceID + "' AND areaid = '" + areaid + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public List<Map<String, Object>> getGeoFenceCN(String deviceID) {
		String sql = "SELECT lat, lng, radius,areaid,defencename FROM TAreaInfo WHERE did = " + deviceID;
		return daoMapper.selectList(sql);
	}

	public boolean deleteGeoFenceCN(String deviceID, String areaid) {
		String sql = "DELETE FROM TAreaInfo WHERE areaid =" + areaid;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	// 获取油耗里程信息(obd)
	public Map<String, Object> getCarMileageAndFuelData(String device_sn, String startDate, String endDate) {
		String sql = "SELECT SUM(gps1.mileage/1000) AS totalmileage,SUM(gps1.`fuel_consumption`) AS totalfuel  FROM `TTrip_Data` gps1 "
				+ "WHERE gps1.device_sn = '" + device_sn + "'";// 今天以前的总里程 总油耗
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT  SUM(gd.`mileage`/1000) AS mileage,SUM(gd.`fuel_consumption`) AS fuel ")
				.append("FROM TTrip_Data gd ").append("LEFT JOIN TDevice d ON gd.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE gd.device_sn = '").append(device_sn)
				.append("' AND gd.start_time BETWEEN DATE_ADD('").append(startDate)
				.append("', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('").append(endDate)
				.append("', INTERVAL -tu.`timezone` SECOND) ");
		// 今天的路程和油耗
		Map<String, Object> map = daoMapper.select(sql);
		Map<String, Object> todayMileageAndFuelMap = daoMapper.select(sb.toString());
		if (map != null && map.size() >= 1) {
			double totalMileage = map.get("totalmileage") == null ? 0
					: Double.parseDouble(map.get("totalmileage").toString());// 里程
			double totalFuel = map.get("totalfuel") == null ? 0 : Double.parseDouble(map.get("totalfuel").toString());// 油耗
			if (todayMileageAndFuelMap != null && todayMileageAndFuelMap.size() > 0) {
				double mileage = todayMileageAndFuelMap.get("mileage") == null ? 0
						: Double.parseDouble(todayMileageAndFuelMap.get("mileage").toString());// 里程
				double fuel = todayMileageAndFuelMap.get("fuel") == null ? 0
						: Double.parseDouble(todayMileageAndFuelMap.get("fuel").toString());// 油耗
				map.put("mileage", mileage);
				map.put("fuel", fuel);
			}
			map.put("totalmileage", totalMileage);
			map.put("totalfuel", totalFuel);
			return map;
		} else {
			if (todayMileageAndFuelMap != null && todayMileageAndFuelMap.size() > 0) {
				Double mileage = todayMileageAndFuelMap.get("mileage") == null ? 0
						: Double.parseDouble(todayMileageAndFuelMap.get("mileage").toString());// 里程
				double fuel = todayMileageAndFuelMap.get("fuel") == null ? 0
						: Double.parseDouble(todayMileageAndFuelMap.get("fuel").toString());// 油耗
				map = new HashMap<String, Object>();
				map.put("mileage", mileage);
				map.put("fuel", fuel);
			}
			return map;
		}
	}

	// 获取车辆检测分数和故障信息
	public List<Map<String, Object>> getCarInspectionScoreAndTroubleData(int id) {

		StringBuffer sb = new StringBuffer();
		sb.append(
				"SELECT score,DATE_ADD(tts.`datetime`, INTERVAL tu.`timezone` SECOND) AS datetime,cts_id,item_type,item_status FROM TCarTestScore tts ")
				.append("LEFT JOIN TCarTestScoreDetail ttsd ON tts.id=ttsd.cts_id ")
				.append("LEFT JOIN TDevice d ON tts.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE tts.id = ").append(id);

		List<Map<String, Object>> listMap = daoMapper.selectList(sb.toString());
		if (listMap != null && listMap.size() >= 1) {
			return listMap;
		} else {
			return null;
		}
	}

	// 获取车辆检测id
	public int getCarInspectionId(String device_sn) {
		String sql = "SELECT id FROM TCarTestScore WHERE devicesn='" + device_sn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.get("id") == null)
			return -1;
		return Integer.parseInt(String.valueOf(map.get("id")));
	}

	public List<Map<String, Object>> getDriveTrail(int deviceID, String startDatetime, String endDatetime) {
		// 查询当天行程（行程距离大于0视为有效行程）
		StringBuffer sb = new StringBuffer();
		sb.append(
				"SELECT DATE_ADD(gd.`start_time`, INTERVAL tu.`timezone` SECOND) AS start_time,DATE_ADD(gd.`end_time`, INTERVAL tu.`timezone` SECOND) AS end_time,TIMESTAMPDIFF(MINUTE,gd.`start_time`,gd.`end_time`) AS spendtime ,"
						+ "gd.`mileage`,gd.`fuel_consumption`, "
						+ "gd.`start_latlon`,gd.`end_latlon`,gd.`start_addr`,gd.`end_addr` ")
				.append("FROM TTrip_Data gd ").append("LEFT JOIN TDevice d ON gd.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE gd.did = ").append(deviceID)
				.append(" AND gd.start_time BETWEEN DATE_ADD('").append(startDatetime)
				.append("', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('").append(endDatetime)
				.append("', INTERVAL -tu.`timezone` SECOND) ")
				/*
				 * .append(" AND gd.end_time BETWEEN DATE_ADD('")
				 * .append(startDatetime) .append(
				 * "', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('") .
				 * append(endDatetime).append(
				 * "', INTERVAL -tu.`timezone` SECOND) " )
				 */
				.append("AND gd.`mileage` > 0 ").append(" AND gd.start_latlon !='' ").append(" AND gd.end_latlon !='' ")
				.append("ORDER BY gd.start_time DESC");
		return daoMapper.selectList(sb.toString());
	}

	public boolean setAlarmStatus(String alarmID) {
		String sql = "UPDATE TAlarmData SET readstatus=1 WHERE id in ('" + alarmID + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	// 获取工况检测 TObd_Condition
	public List<Map<String, Object>> getWorkConditionInspectionItem(String device_sn, String starttime,
			String endtime) {
		StringBuffer sb = new StringBuffer();
		String con_keys = "'0x2104','0x2105','0x2106','0x2108','0x2107','0x2109','0x2110','0x210b','0x2111','0x2145','0x2147','0x2148','0x210c','0x210e','0x15c0','0x16e0','0x0084','0x1330','0x301c','0x1be0'";
		sb.append("SELECT con_key,con_value,MAX(`Device_UTC_Time`) FROM TObd_Condition tc ")
				.append("LEFT JOIN TDevice d ON tc.`did` = d.`id`  ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id`  ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid`  ").append("WHERE tc.device_sn = '").append(device_sn)
				// .append(" AND tc.rcv_time BETWEEN DATE_ADD('")
				// .append(starttime)
				// .append("', INTERVAL -tu.`timezone` SECOND) AND DATE_ADD('")
				// .append(endtime).append("', INTERVAL -tu.`timezone` SECOND)
				// ")
				.append("' AND tc.Device_UTC_Time BETWEEN '").append(starttime).append("' AND '").append(endtime + "'")
				.append("' AND con_key in(").append(con_keys).append(")")
				.append(" GROUP BY `con_key`,Device_UTC_Time ");

		return daoMapper.selectList(sb.toString());
	}

	// 获取故障检测 TObd_DTC
	public List<Map<String, Object>> getTroubleInspectionItem(String device_sn, String starttime, String endtime) {
		// String sql =
		// "SELECT COUNT(*) AS num FROM TObd_DTC WHERE device_sn="+device_sn+"
		// AND rcv_time > "+starttime;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT distinct fault_code FROM TObd_DTC td ").append("LEFT JOIN TDevice d ON td.`did` = d.`id`  ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id`  ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid`  ").append("WHERE td.device_sn = '").append(device_sn)
				.append("' AND td.rcv_time BETWEEN '").append(starttime).append("' AND '").append(endtime + "'");

		return daoMapper.selectList(sb.toString());

	}

	// 插入检测分数
	public boolean insertTCarTestScore(int deviceID, String devicesn, int score) {
		String sql = "INSERT INTO TCarTestScore(did, devicesn, score,datetime) " + "VALUES('" + deviceID + "', '"
				+ devicesn + "', '" + score + "',UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	// 更新检测分数
	public boolean updateTCarTestScore(int deviceID, String devicesn, String utdDate, int score) {
		String sql = "UPDATE TCarTestScore SET datetime = '" + utdDate + "',score=" + score + " WHERE `did` = '"
				+ deviceID + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	// 插入检测项
	public boolean insertTCarTestScoreDetail(int cts_id, int item_type, int item_status) {
		String sql = "INSERT INTO TCarTestScoreDetail(cts_id, item_type, item_status) " + "VALUES('" + cts_id + "', '"
				+ item_type + "', '" + item_status + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	// 更新检测项
	public boolean updateTCarTestScoreDetail(int cts_id, int item_type, int item_status) {
		String sql = "UPDATE TCarTestScoreDetail set item_status=" + item_status + " WHERE cts_id=" + cts_id
				+ " AND item_type=" + item_type;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	// 获取车辆检测id
	public int getCarInspectionItemId(int cts_id, int item_type) {
		String sql = "SELECT id FROM TCarTestScoreDetail WHERE cts_id=" + cts_id + " AND item_type=" + item_type;
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.get("id") == null)
			return -1;
		return Integer.parseInt(String.valueOf(map.get("id")));
	}

	public Map<String, Object> getDeviceInfoByProductType(int productType) {
		String sql = "SELECT id,product_type,ranges,protocol_type FROM TDevice_Protocol tdp WHERE `product_type` = '"
				+ productType + "'";
		return daoMapper.select(sql);
	}

	// 获取obd最近一次行程信息
	public Map<String, Object> getRecentlyTripData(String devicesn) {
		String sql = "SELECT start_time,end_time FROM TTrip_Data WHERE device_sn='" + devicesn + "'"
				+ " order by end_time desc";
		Map<String, Object> map = daoMapper.select(sql);
		return map;
	}

	// 获取这段行程的车辆安全驾驶数据
	public Map<String, Integer> getAlarmCount(String startDate, String endDate, String devicesn) {
		String sql = "SELECT a.`did`,a.`type`,a.`collect_datetime`,"
				+ " COUNT(a.type) AS alarmCount FROM `TAlarmData` a " + " WHERE a.`device_sn`='" + devicesn + "' "
				+ " and a.`collect_datetime` >= '" + startDate + "' and a.`collect_datetime` <= '" + endDate
				+ "' Group By a.`did`,a.`type` ";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		Map<String, Integer> map = new HashMap<String, Integer>();
		if (null != list && list.size() > 0) {
			for (Object o : list) {
				HashMap<String, Object> oMap = (HashMap<String, Object>) o;
				int alarmTimeLong = 0;
				if (null != oMap.get("alarmCount")) {
					alarmTimeLong = Integer.valueOf(String.valueOf(oMap.get("alarmCount")));
				}
				map.put(String.valueOf(String.valueOf(oMap.get("type"))), alarmTimeLong);
			}
		}
		return map;
	}

	// 获取这段行程的车辆经济驾驶数据
	public Map<String, Integer> getAlarmTimeLong(String startDate, String endDate, String devicesn) {
		String sql = "SELECT a.`did`,a.`type`,a.`collect_datetime`,"
				+ " SUM(TIMESTAMPDIFF(SECOND,a.`collect_datetime`,a.`end_time`)) AS alarmTimeLong FROM `TAlarmData` a "
				+ " WHERE a.`device_sn`='" + devicesn + "' " + " and a.`collect_datetime` >= '" + startDate
				+ "' and a.`collect_datetime` <= '" + endDate + "' Group By a.`did`,a.`type` ";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		Map<String, Integer> map = new HashMap<String, Integer>();
		if (null != list && list.size() > 0) {

			for (Object o : list) {
				HashMap<String, Object> oMap = (HashMap<String, Object>) o;
				int alarmTimeLong = 0;
				if (null != oMap.get("alarmTimeLong")) {
					alarmTimeLong = Integer.valueOf(String.valueOf(oMap.get("alarmTimeLong")));
				}
				map.put(String.valueOf(String.valueOf(oMap.get("type"))), alarmTimeLong);
			}
		}
		return map;
	}

	// 获取车辆分析数据
	public Map<String, Object> getOBDAlarmScoreStatistics(String startDate, String endDate, String score_type,
			String devicesn) {
		String sql = "SELECT SUM(p2) as p2,SUM(p4) as p4,SUM(p5) as p5,SUM(p6) as p6,SUM(p8) as p8,SUM(p9) as p9,"
				+ "SUM(p10) as p10,SUM(p11) as p11,SUM(p12) as p12,SUM(p13) as p13,SUM(p14) as p14,"
				+ "SUM(p15) as p15,AVG(score) as score FROM TDriverBehaviorAnalysis " + " WHERE device_sn = '"
				+ devicesn + "' AND  d_time > '" + startDate + "' AND d_time < '" + endDate + "' AND score_type ="
				+ score_type;

		Map<String, Object> map = daoMapper.select(sql);
		return map;
	}

	// 获取设备最新故障日期
	public Map<String, Object> getRecentlyTroubleOBD_TDC(String device_sn) {
		// String sql =
		// "SELECT COUNT(*) AS num FROM TObd_DTC WHERE device_sn="+device_sn+"
		// AND rcv_time > "+starttime;
		String sql = "SELECT device_sn,rcv_time FROM TObd_DTC WHERE device_sn='" + device_sn
				+ "' ORDER BY rcv_time DESC";
		return daoMapper.select(sql);
	}

	// 获取producttype与productname集合
	public List<Map<String, Object>> getProductInfo(String lang) {
		String sql = "select t1.*,t4.`lang_key_id` as lkid,t4.`id` as lid,t4.`value` as name from `enum` t1 "
				+ "left join `enum_type` t2 on t1.`enum_type_id` = t2.`id` "
				+ "left join `lang_key` t3 on CONCAT('@enum.', t2.`code`, '.', t1.`id`) = t3.`field` "
				+ "left join `lang` t4 on t3.`id` = t4.`lang_key_id` and t4.`code` =  'zh-cn' where  t2.`code`='deviceType' ";
		return daoMapper.selectList(sql);
	}

	// 获取公告信息
	public Map<String, Object> getSystemNotice(String language) {
		String sql = "";
		if (language != null && !language.equals("")) {
			sql = "select content from notice where enable=1 and lang_code='" + language + "'";
			Map<String, Object> map = daoMapper.select(sql);
			if (map != null && map.get("content") != null && !map.get("content").toString().trim().equals("")) {
				return map;
			}
		}
		// 默认查英文公告
		sql = "select content from notice where enable=1 and lang_code='en-us'";
		return daoMapper.select(sql);
	}

	// 激活邮件
	public int updateEmailVerifyStatus(String username) {
		String sql = "";
		if (username != null && !username.equals("")) {
			sql = "update TUser SET is_email_verify=1  where name='" + username + "'";
			try {
				return daoMapper.modify(sql);
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
			}
		}

		return -1;
	}

	// 插入遛狗记录
	public int insertTTrip_Data(int did, String device_sn, String start_latlon) {
		String sql = "INSERT INTO TTrip_Data(did, `device_sn`,`start_latlon`,start_addr,start_time) VALUES('" + did
				+ "','" + device_sn + "','" + start_latlon + "','" + start_latlon + "',UTC_TIMESTAMP())";

		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	// 获取刚插入的遛狗记录
	public int getTTrip_DataIdByDeviceSn(String device_sn) {
		String sqlMaxId = "select max(id) as id from TTrip_Data where device_sn='" + device_sn + "'";
		Map<String, Object> map = daoMapper.select(sqlMaxId);
		if (map != null && map.get("id") != null) {
			return Integer.parseInt(map.get("id").toString());
		}
		return -1;
	}

	// 更新遛狗记录
	public int updateTTrip_Data(int id, String end_latlon, int mileage, double calorie) {
		String sql = "update TTrip_Data set end_latlon='" + end_latlon + "',mileage=" + mileage + ",calorie=" + calorie
				+ "  where id=" + id;
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/*
	 * 获取遛狗记录(当月)
	 */
	public List<Map<String, Object>> walkDogTrail(String deviceSn, String startdate) {
		String sql = "SELECT td.id,td.did,td.device_sn,DATE_ADD(td.`start_time`, INTERVAL tu.`timezone` SECOND) AS start_time, "
				+ " DATE_ADD(td.`end_time`, INTERVAL tu.`timezone` SECOND) AS end_time,TIMESTAMPDIFF(SECOND,td.start_time,td.end_time) AS spendtime,td.mileage,td.calorie "
				+ " FROM TTrip_Data td LEFT JOIN TDevice d ON td.`did` = d.`id` "
				+ " LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 "
				+ " LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` " + " WHERE td.start_time LIKE '" + startdate
				+ "%' and td.device_sn='" + deviceSn
				+ "' AND TIMESTAMPDIFF(SECOND,td.start_time,td.end_time) >= 30 AND td.mileage>0 order by td.id desc";
		return daoMapper.selectList(sql);
	}

	/*
	 * 当月遛狗统计
	 */
	public Map<String, Object> walkDogDataStatistics(String deviceSn, String startdate) {
		String sql = "SELECT COUNT(id) AS times,SUM(mileage) as mileage,SUM(calorie) as calorie,"
				+ " SUM(TIMESTAMPDIFF(SECOND,start_time,end_time)) AS spendtime "
				+ " FROM TTrip_Data where start_time IS NOT NULL AND end_time IS NOT NULL "
				+ " AND end_time > start_time and start_time like '" + startdate + "%' AND device_sn='" + deviceSn
				+ "'";
		return daoMapper.select(sql);
	}

	public Map<String, Object> getWalkDogTrailByID(int id) {
		String sql = "SELECT td.id,td.did,td.device_sn,DATE_ADD(td.`start_time`, INTERVAL tu.`timezone` SECOND) AS start_time, "
				+ " DATE_ADD(td.`end_time`, INTERVAL tu.`timezone` SECOND) AS end_time,TIMESTAMPDIFF(MINUTE,td.start_time,td.end_time) AS spendtime,td.mileage,td.calorie,td.mileage,td.calorie "
				+ " FROM TTrip_Data td LEFT JOIN TDevice d ON td.`did` = d.`id` "
				+ " LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 "
				+ " LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` " + " WHERE td.id=" + id;
		return daoMapper.select(sql);
	}

	public int updateWalkDogTrailByID(int id) {
		String sql = "update TTrip_Data set end_time=UTC_TIMESTAMP() where id=" + id;
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/*
	 * 获取遛狗记录
	 */
	/*
	 * public List<Map<String, Object>> walkDogTrailDetail(String
	 * deviceSn,String startdate,String enddate) { String sql =
	 * "SELECT id,did,device_sn,start_time,end_time,start_latlon,end_latlon FROM TTrip_Data where start_time !='0000-00-00 00:00:00' AND end_time !='0000-00-00 00:00:00' start_time >'"
	 * +startdate+"'"; return daoMapper.selectList(sql); }
	 */

	/**
	 * 获取套餐数据
	 */
	public List<Map<String, Object>> getPackage() {
		String sql = "select id,name,month,serve_fee,content,create_time,status from order_package";
		return daoMapper.selectList(sql);
	}

	/**
	 * 订单生成
	 */
	public int generateOrder(String order_no, String trade_no, String device_sn, String user, int pay_type,
			String payer, double price, String currency_unit, int status, String remark, int order_package_id) {
		String sql = "insert into order(order_no,trade_no,device_sn,user,pay_type,payer,price,currency_unit,status,remark,order_package_id,create_time) values('"
				+ order_no + "','" + trade_no + "','" + device_sn + "','" + user + "','" + pay_type + "','" + payer
				+ "','" + price + "','" + currency_unit + "','" + status + "','" + remark + "','" + order_package_id
				+ "',UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/**
	 * 订单查询
	 */
	public Map<String, Object> getGenerateOrderByOrderno(String order_no) {
		String sql = "select * from order where order_no='" + order_no + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 更新订单
	 */
	public int updateOrderStatus(String order_no, String status) {
		String sql = "update order set status=" + status + " where order_no='" + order_no + "'";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/*
	 * 修改设备周边属性
	 */
	public int updateDeviceAroundRanges(String deviceSn, int aroundRanges, String simNo) {
		String sql = "update TDevice set around_ranges=" + aroundRanges + " where device_sn='" + deviceSn + "'";
		try {
			if (simNo != null && !simNo.equals("")) {
				sql = "update TDevice set around_ranges=" + aroundRanges + ",tracker_sim='" + simNo
						+ "' where device_sn='" + deviceSn + "'";
			}
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	/**
	 * 获取商品信息
	 * 
	 * @return
	 */
	public List<Map<String, Object>> getGoodsInfo() {
		String sql = "select * from goods where status=0";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		return list;
	}

	// 修改周边属性
	public int changeDeviceAroundRanges(int deviceId, int around_ranges) {
		String sql = "UPDATE `TDevice` SET around_ranges='" + around_ranges + "' WHERE `id`='" + deviceId + "'";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	public Map<String, Object> unfinishWalkDog(String deviceSn) {
		String sql = "select * from TTrip_Data where device_sn='" + deviceSn + "' and end_time IS NULL";
		Map<String, Object> map = daoMapper.select(sql);
		return map;
	}

	public List<Map<String, Object>> recentGpsData(int deviceID, String datetime) {
		StringBuffer sb = new StringBuffer();
		sb.append(
				"SELECT d.`device_sn`, DATE_ADD(gd.`collect_datetime`, INTERVAL tu.`timezone` SECOND) AS collect_datetime,")
				.append("gd.lat, gd.lng, gd.`speed`, gd.`direction`, gd.`LBS_WIFI_Range`,gd.`steps`,gd.`calorie` ")
				.append("FROM TGpsData gd ").append("LEFT JOIN TDevice d ON gd.`did` = d.`id` ")
				.append("LEFT JOIN TDeviceUser tdu ON tdu.did=d.`id` AND tdu.`is_super_user`=1 ")
				.append("LEFT JOIN TUser tu ON tu.`id`=tdu.`uid` ").append("WHERE gd.did = ").append(deviceID)
				.append(" AND gd.lat is not null AND gd.lng is not null AND (gd.`gps_flag` = 3 or gd.`gps_flag` = 10 or gd.`gps_flag` = 2) ")
				// .append(" AND gd.lat is not null AND gd.lng is not null ")
				.append(" AND gd.collect_datetime > DATE_ADD('").append(datetime)
				.append("', INTERVAL -tu.`timezone` SECOND) ").append(" AND gd.deleteflag=0 ")
				.append(" ORDER BY gd.collect_datetime ASC");
		return daoMapper.selectList(sb.toString());
	}

	public int deleteWalkDog(int id) {
		String sql = "DELETE FROM TTrip_Data WHERE `id`='" + id + "'";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	public int modifyAccountRemark(int uid, int did, String nickname) {
		String sql = "UPDATE TDeviceUser SET nickname='" + nickname + "' where uid='" + uid + "' and did='" + did + "'";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	public int uploadGpsData(int deviceID, String deviceSn, String collect_datetime, String lat, String lng) {
		String sql = "INSERT INTO TGpsData(did,device_sn,collect_datetime,rcv_time,lat,lng,gps_flag) values('"
				+ deviceID + "','" + deviceSn + "','" + collect_datetime + "','" + collect_datetime + "','" + lat
				+ "','" + lng + "','3')";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return -1;
	}

	public boolean setBluetoothWatchRemind(String user_id, String deviceSn, String week, String time, int ring,
			int alert_type, int flag, int type, int title_len, String title, int image_len, String image_name) {
		String sql = "INSERT INTO blue_watch_clock(user_id, device_sn, week, time, ring,"
				+ "alert_type, flag, type, title_len, title, image_len, image_name," + "version) " + "values('"
				+ user_id + "','" + deviceSn + "','" + week + "','" + time + "'," + ring + "," + alert_type + "," + flag
				+ "," + type + "," + title_len + ",'" + title + "'," + image_len + ",'" + image_name + "',1)";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public List<Map<String, Object>> getBluetoothWatchRemind(int id, String deviceSn) {
		String sql = "SELECT id,user_id,device_sn as deviceSn,week,time,ring,alert_type,flag,type,title_len ,"
				+ "title,image_len,image_name,version FROM blue_watch_clock WHERE  device_sn='" + deviceSn
				+ "' order by id ASC";
		if (id > 0) {
			sql = "SELECT id,user_id,device_sn as deviceSn,week,time,ring,alert_type,flag,type,title_len ,"
					+ "title,image_len,image_name,version FROM blue_watch_clock WHERE  id=" + id + " and device_sn='"
					+ deviceSn + "' order by id ASC";
		}
		return daoMapper.selectList(sql);

	}

	public boolean updateBluetoothWatchRemind(int id, String user_id, String deviceSn, String week, String time,
			int ring, int alert_type, int flag, int type, int title_len, String title, int image_len, String image_name,
			int version) {
		String sql = "UPDATE `blue_watch_clock` SET week='" + week + "',time='" + time + "',ring='" + ring
				+ "',alert_type='" + alert_type + "',flag='" + flag + "',type='" + type + "',title_len='" + title_len
				+ "',title='" + title + "',image_len='" + image_len + "',image_name='" + image_name + "',version='"
				+ version + "' WHERE id='" + id + "' ";

		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean deleteBluetoothWatchRemind(int id) {
		String sql = "DELETE FROM blue_watch_clock WHERE  id='" + id + "'";
		try {
			daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
		return true;
	}

	public Map<String, Object> isExistDeviceCode(String insur_code, String deviceSn) {
		String sql = "select id from TDevice where device_sn='" + deviceSn + "' and insur_code='" + insur_code + "'";
		return daoMapper.select(sql);
	}

	public boolean setPetInsurance(String no, String deviceSn, String user_name, String real_name, String mobile,
			String dog_name, String type, String colour, int tail_shape, int age, int sex, String start_time,
			String end_time) {
		String sql = "INSERT INTO pet_insur(`no`, `device_sn`, `user_name`, `real_name`, `mobile`, `dog_name`, `type`,`colour`,tail_shape,age,sex,`start_time`,`end_time`,`create_time`) VALUES('"
				+ no + "','" + deviceSn + "','" + user_name + "','" + real_name + "','" + mobile + "','" + dog_name
				+ "','" + type + "','" + colour + "','" + tail_shape + "','" + age + "','" + sex + "','" + start_time
				+ "','" + end_time + "',UTC_TIMESTAMP())";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getPetInsurance(String deviceSn, int timezone) {
		String sql = "select id,no,device_sn as deviceSn,user_name,real_name,mobile,dog_name,type,colour,tail_shape,age,sex,"
				+ "DATE_ADD(start_time, INTERVAL " + timezone + " SECOND) as start_time,"
				+ "DATE_ADD(end_time, INTERVAL " + timezone + " SECOND) as end_time,"
				+ "create_time from pet_insur where device_sn='" + deviceSn + "'";
		return daoMapper.select(sql);
	}

	public boolean updateDeviceInfo(String nickname, int deviceId) {
		String sql = "update TDevice set nickname='" + nickname + "' where id='" + deviceId + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/*
	 * 获取当前设备超级用户的timezone信息
	 */
	public int getDeviceSuperUserTimezone(String deviceSN) {
		String sql = "SELECT u.`timezone` FROM TDevice d LEFT JOIN TDeviceUser du ON du.did = d.`id` AND du.is_super_user = 1 LEFT JOIN TUser u ON u.`id`=du.`uid` WHERE d.`device_sn` ='"
				+ deviceSN + "'";
		Map<String, Object> map = daoMapper.select(sql);
		int timezone = 28800;// 默认北京时间
		if (map != null && map.size() > 0 && map.get("timezone") != null && !map.get("timezone").equals("")) {
			timezone = Integer.parseInt(map.get("timezone").toString());
		}
		return timezone;
	}

	public Map<String, Object> getDeviceInfoByDevicesn(String deviceSn) {
		String sql = "SELECT * FROM TDevice where device_sn='" + deviceSn + "'";
		return daoMapper.select(sql);
	}

	public int updateIsCustomizedApp(String username, int isCustomizedApp) {
		String sql = "UPDATE TUser SET isCustomizedApp='" + isCustomizedApp + "' where name='" + username + "' ";
		try {
			return daoMapper.modify(sql);
		} catch (Exception e) {
			LogManager.error("username:" + username + " error:" + e.getMessage());
		}
		return -1;
	}

	public List<Map<String, Object>> getDeviceRemind(String deviceSn) {
		String sql = "select * from TDevice_clock where device_sn='" + deviceSn + "' order by id asc";
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	public boolean insertDeviceRemind(String deviceSn, int index, String time) {
		String sql = "INSERT INTO TDevice_clock(`device_sn`, `index_value`, `time`) VALUES('" + deviceSn + "','" + index
				+ "','" + time + "')";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getDeviceRemindByDeviceSn(String deviceSn, int index) {
		String sql = "select * from TDevice_clock where device_sn='" + deviceSn + "' and index_value=" + index
				+ " order by id asc";
		Map<String, Object> remind = null;
		try {
			remind = daoMapper.select(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return remind;
	}

	public Map<Integer, Object> getDeviceAllRemindByDeviceSn(String deviceSn) {
		String sql = "select `index_value`,`time` from TDevice_clock where device_sn='" + deviceSn
				+ "' order by `index_value` asc";
		Map<Integer, Object> remind = new HashMap<Integer, Object>();
		remind.put(1, "11:30-0-3-0000000");
		remind.put(2, "11:30-0-3-0000000");
		remind.put(3, "11:30-0-3-0000000");

		try {
			List<Map<String, Object>> list = daoMapper.selectList(sql);
			if (list != null && !list.isEmpty() && list.size() > 0) {
				for (Map<String, Object> map : list) {
					remind.put(Integer.parseInt(map.get("index_value").toString()), map.get("time").toString());
				}
			}
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}

		return remind;
	}

	public boolean updateDeviceRemind(String deviceSn, int index, String time) {
		String sql = "update TDevice_clock set time='" + time + "' where device_sn='" + deviceSn + "' and index_value='"
				+ index + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateDeviceMonitoringPhone(String deviceSn, String center_mobile) {
		center_mobile = center_mobile == "0" ? "" : center_mobile;
		String sql = "update TDevice set center_mobile='" + center_mobile + "' where device_sn='" + deviceSn + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public boolean updateDeviceStep(String deviceSn, int step) {
		String sql = "update TDevice set step='" + step + "' where device_sn='" + deviceSn + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	public int getDeviceStep(String deviceSn) {
		String sql = "SELECT d.`step` FROM TDevice d  WHERE d.`device_sn` ='" + deviceSn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		int step = 0;// 默认0
		if (map != null && map.size() > 0 && map.get("step") != null && !map.get("step").equals("")) {
			step = Integer.parseInt(map.get("step").toString());
		}
		return step;
	}

	public boolean updateDeviceAPN(String deviceSn, String name, String user_name, String password, String user_data) {
		String selectSql = "select * from device_apn where device_sn = '" + deviceSn + "'";
		if (daoMapper.selectList(selectSql).size() > 0) {
			String updateSql = "update device_apn  set  name = '" + name + "' ,user_name='" + user_name
					+ "' ,password ='" + password + "',user_data ='" + user_data + "' where device_sn = '" + deviceSn
					+ "' ";

			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}

		// 不存在则插入
		String insertSql = "insert into device_apn (device_sn,name,user_name,password,user_data) values ('" + deviceSn
				+ "','" + name + "','" + user_name + "','" + password + "','" + user_data + "')";

		try {
			return daoMapper.modify(insertSql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}

	}

	public List<Map<String, Object>> getDeviceAPN(String deviceSn) {
		String sql = "select * from device_apn where device_sn='" + deviceSn + "'";
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	public List<Map<String, Object>> getDeviceWifi(String deviceSn) {
		String sql = "select * from device_wifi where device_sn='" + deviceSn + "'";
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	public boolean updateDeviceWifi(String deviceSn, String name, String password) {
		String selectSql = "select * from device_wifi where device_sn = '" + deviceSn + "'";
		if (daoMapper.selectList(selectSql).size() > 0) {
			String updateSql = "update device_wifi  set  name = '" + name + "',password ='" + password
					+ "' where device_sn = '" + deviceSn + "' ";

			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}

		// 不存在则插入
		String insertSql = "insert into device_wifi (device_sn,name,password) values ('" + deviceSn + "','" + name
				+ "','" + password + "')";

		try {
			return daoMapper.modify(insertSql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}

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
	public Map<String, Object> getChatToken(String username) {
		String sql = "SELECT `token` FROM `chat_token` WHERE name='" + username + "'";
		Map<String, Object> list = daoMapper.select(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 修改用户信息
	 * 
	 * @param username
	 * @param nickname
	 * @param portrait
	 * @return
	 */
	public boolean updateUserInfo(String username, String nickname, String sex, String age, String area, String mark) {
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
			if (sex != null && !sex.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `sex`='" + sex + "'";
				} else {
					sqlparam += " ,`sex`='" + sex + "'";
				}
			}
			if (age != null && !age.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `age`='" + age + "'";
				} else {
					sqlparam += " ,`age`='" + age + "'";
				}
			}
			if (area != null && !area.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `area`='" + area + "'";
				} else {
					sqlparam += " ,`area`='" + area + "'";
				}
			}
			if (mark != null && !mark.isEmpty()) {
				if (sqlparam.isEmpty()) {
					sqlparam += " `mark`='" + mark + "'";
				} else {
					sqlparam += " ,`mark`='" + mark + "'";
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

	public Map<String, Object> getUserInfo(String username) {

		String sql = "SELECT  `name`,`nickname`,`sex`,`age`,`area`,`mark` " + "FROM `TUser` WHERE `name` = '" + username
				+ "'";
		Map<String, Object> userInfo = null;
		try {
			userInfo = daoMapper.select(sql);
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}

		if (userInfo == null) {

			return null;
		} else {
			return userInfo;
		}
	}

	/**
	 * 获取用户头像
	 */
	public Map<String, Object> getUserPortraitInfo(String username) {
		String sql = "SELECT `portrait`,`nickname` FROM `TUser` WHERE name='" + username + "'";
		Map<String, Object> list = daoMapper.select(sql);
		if (list != null && list.size() >= 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 根据设备号查询设备到期时间和设备企业的收款ID
	 * 
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> getDeviceAndPayeeByDevicesn(String deviceSn) {
		String sql = "SELECT d.device_sn,d.org_id,d.expired_time_de,p.user_id as clientId,p.`secret_key`  as secret FROM TDevice d "
				+ "left join org o on d.org_id=o.id " + "left join org_payee p on o.id=p.org_id WHERE d.`device_sn` = '"
				+ deviceSn + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 查询单个设备套餐
	 * 
	 * @param id
	 * @return
	 */
	public Map<String, Object> getOrderpackageById(Integer id) {
		String sql = "SELECT id as orderPackageId,name,month,`serve_fee`,`content`,`currency_unit` FROM `order_package` WHERE `id`= '"
				+ id + "'";
		return daoMapper.select(sql);
	}

	/**
	 * 查询单个设备套餐
	 * 
	 * @param id
	 * @return
	 */
	public List<Map<String, Object>> getOrderpackage(Integer orgId) {
		String sql = "SELECT  id as orderPackageId,name,month,`serve_fee`,`content`,`currency_unit` FROM `order_package` WHERE `status`=2 and serve_fee > 0 and org_id="
				+ orgId + " ORDER BY `id` ASC";
		return daoMapper.selectList(sql);
	}

	/**
	 * 查询设备免费套餐月数
	 * 
	 * @param id
	 * @return
	 */
	public Map<String, Object> getOrderpackageByDeviceSn(String deviceSn) {
		String sql = "SELECT p.month FROM TDevice d " + "left join org o on d.org_id=o.id "
				+ "left join order_package p on o.id=p.org_id WHERE p.serve_fee = 0 and d.`device_sn` = '" + deviceSn
				+ "'";
		return daoMapper.select(sql);
	}

	/**
	 * 
	 */
	public boolean saveLatlng(String latlngKey, String bgl_code, String lat, String lng, String latWay, String lngWay,
			String type) {

		String selectSql = "select * from  `wifi_latlng` where latlng_key = '" + latlngKey + "'";
		if (daoMapper.selectList(selectSql).size() > 0) {
			return true;
		} else {

			String sql = "INSERT INTO `wifi_latlng`(latlng_key,bgl_code,lat,lng,lat_way,lng_way,type) " + "VALUES('"
					+ latlngKey + "','" + bgl_code + "','" + lat + "','" + lng + "','" + latWay + "','" + lngWay + "','"
					+ type + "')";
			try {
				daoMapper.modify(sql);

			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
			return true;
		}
	}

	/**
	 * 查询设备停车未熄火时间是否>=Gps表最后一次点火时间
	 * 
	 * @param deviceSn
	 * @return
	 */
	public boolean isIdling(String deviceSn) {
		try {
			String selectSql = " select COUNT(*) from ((select MAX(a.`collect_datetime`) as collect_datetime  from `TAlarmData` a  where a.type =86 and a.`device_sn`='"
					+ deviceSn + "' ) collect_datetime,"
					+ " (select MAX(Last_accon_time) as Last_accon_time from `TGpsData` g where g.`device_sn`='"
					+ deviceSn + "') Last_accon_time )  where collect_datetime >=Last_accon_time";
			if (daoMapper.count(selectSql) > 0)
				return true;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * 查询最后一次点火时间
	 * 
	 * @param deviceSn
	 */
	public Map<String, Object> getAcconTime(String deviceSn) {
		// 查询是否存在用户
		String sql = "select MAX(Last_accon_time) as Last_accon_time from `TGpsData` where device_sn = '" + deviceSn
				+ "'";
		return daoMapper.select(sql);
	}

	/**
	 * 设置情景模式
	 * 
	 * @param deviceSn
	 * @param profile
	 * @return
	 */
	public boolean updateDeviceProfile(String deviceSn, int profile) {
		String sql = "update TDevice set profile='" + profile + "' where device_sn='" + deviceSn + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
		}
		return false;
	}

	/**
	 * 查询情景模式
	 * 
	 * @param deviceSn
	 * @return
	 */
	public int getDeviceProfile(String deviceSn) {
		String sql = "SELECT d.`profile` FROM TDevice d  WHERE d.`device_sn` ='" + deviceSn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		int mode = 1;// 默认1
		if (map != null && map.size() > 0 && map.get("profile") != null && !map.get("profile").equals("")) {
			mode = Integer.parseInt(map.get("profile").toString());
		}
		return mode;
	}

	public boolean updateMedicationRemind(String deviceSn, String time, int index, String message) {
		String selectSql = "select * from medication_clock where device_sn = '" + deviceSn + "' AND `index_value` ="
				+ index;
		if (daoMapper.selectList(selectSql).size() > 0) {
			String updateSql = "update medication_clock  set  `time` = '" + time + "' ,`message`='" + message
					+ "'  where `device_sn` = '" + deviceSn + "' AND `index_value` ='" + index + "'";
			Object[] params = new Object[] { time, message, deviceSn, index };

			try {
				return daoMapper.modify(updateSql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		}

		// 不存在则插入
		String insertSql = "insert into medication_clock (`device_sn`,`time`,`index_value`,`message`) values ('"
				+ deviceSn + "','" + time + "','" + index + "','" + message + "')";
		try {
			return daoMapper.modify(insertSql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());
			return false;
		}
	}

	public List<Map<String, Object>> getMedicationRemind(String deviceSn) {
		String sql = "select * from medication_clock where device_sn='" + deviceSn + "' order by `index_value` asc";
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	public List<Map<String, Object>> getAroundStore(String storeType, Double maxLat, Double minLat, Double maxLon,
			Double minLon) {
		String sql = "select `name`,`address`,`lat`,`lng`,`pic`,`phone`,`start_time`,`end_time`,`profile` "
				+ "from `store` where poi LIKE '%," + storeType + ",%' AND lat > " + minLat + " AND lat <" + maxLat
				+ " AND lng > " + minLon + " AND lng < " + maxLon;
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	/**
	 * 更新用户登录错误状态
	 */
	public boolean updateLoginError(String username, Integer errCount) {
		String sql = "update TUser set login_limit_time = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 MINUTE), login_err_count = '"
				+ errCount + "' where (name = '" + username + "' OR mobile = '" + username + "' )and enabled = 1";
		try {
			return daoMapper.modify(sql) > 0 ? true : false;
		} catch (Exception e) {
			LogManager.error("username:" + username + " error:" + e.getMessage());
		}
		return false;
	}

	public Map<String, Object> getLoginError(String username) {

		String sql = "SELECT `login_err_count`,`login_limit_time` FROM `TUser` WHERE (name = '" + username
				+ "' OR mobile = '" + username + "' )and enabled = 1";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.isEmpty()) {
			return null;
		} else {
			return map;
		}
	}

	/**
	 * 根据设备号查询对应企业的APP定制类型
	 * 
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> getDeviceCustomized(String deviceSn) {
		String sql = "select  o.`is_customized` from `TDevice` d inner join `org` o on d.`org_id`=o.`id` where d.`device_sn`='"
				+ deviceSn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.isEmpty()) {
			return null;
		} else {
			return map;
		}
	}

	/**
	 * 根据设备号查询对应企业的APP定制类型
	 * 
	 * @param deviceSn
	 * @return
	 */
	public List<Map<String, Object>> getDeviceAtHomePage(String username, String address) {
		String sql = "SELECT g.`device_sn`,d.`ranges`,d.`product_type`,g.`lat`,g.`lng`,g.`online`,"
				+ "IF((ISNULL(d.`head_portrait`) || LENGTH(TRIM(d.`head_portrait`))<1),d.`head_portrait`,CONCAT('"
				+ address
				+ "', d.`head_portrait`)) AS head_portrait FROM `gps_data_last` g LEFT JOIN `TDevice` d ON g.`device_sn` = d.`device_sn` "
				+ "WHERE g.device_sn IN "
				+ "(SELECT device_sn FROM `TDevice` d LEFT JOIN `TDeviceUser` td ON d.id = td.did LEFT JOIN `TUser` tu ON tu.id = td.uid WHERE tu.name = '"
				+ username + "')";

		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	/**
	 * 获取最后的GPS数据
	 */
	public Map<String, Object> getLastGPS(String deviceSn, int timezone) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT DATE_ADD(g.`collect_datetime`, INTERVAL ").append(timezone)
				.append(" SECOND) AS collect_datetime,")
				.append("g.lat, g.`lng`, g.`speed`,g.`gps_flag`, g.`direction`, g.`battery`, g.`steps`, g.`LBS_WIFI_Range`, g.`calorie`,g.`online` AS onlinestatus,g.`car_status` ")
				.append("FROM gps_data_last g ").append("WHERE g.`device_sn` = '").append(deviceSn)
				.append("' AND g.lat is not null AND g.lng is not null AND (g.`gps_flag` = 3 or g.`gps_flag` = 10 or g.`gps_flag` = 2) ")
				.append(" AND g.deleteflag=0 ");
		List<Map<String, Object>> list = daoMapper.selectList(sb.toString());
		return list == null || list.size() == 0 ? null : list.get(0);
	}

	public boolean setHumanInsurance(String orderNum, String email, String insured, String amount, String orderTime) {

		String sqlselect = "SELECT * FROM `pet_insur` WHERE `no`= '" + orderNum + "'";
		Map<String, Object> map = daoMapper.select(sqlselect);
		if (map != null && !map.isEmpty()) {
			String sqlupdate = "UPDATE `pet_insur` SET `user_name`='" + email + "',`real_name`='" + insured
					+ "',`amount`='" + amount + "',`create_time`='" + orderTime + "'  WHERE `no`='" + orderNum + "'";
			Object[] param = new Object[] { email, insured, amount, orderTime, orderNum };

			try {
				return daoMapper.modify(sqlupdate) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
				return false;
			}
		} else {
			String sql = "INSERT INTO pet_insur(`no`, `user_name`, `real_name`, `amount`, `create_time`,`insured_type`) VALUES('"
					+ orderNum + "','" + email + "','" + insured + "','" + amount + "','" + orderTime + "',1)";
			try {
				return daoMapper.modify(sql) > 0;
			} catch (Exception e) {
				LogManager.error("error:" + e.getMessage());
			}
			return false;
		}

	}

	/**
	 * 获取广告
	 * 
	 * @return
	 */
	public List<Map<String, Object>> getAdvertising(String page_code, String currentTime, int adVersion) {
		String sql = "SELECT page_code_index, image_url, ad_url,end_time FROM TAdvertising "
				+ "WHERE `status` = 1 AND `is_customized_app` =" + adVersion + " AND `page_code` =" + page_code
				+ " AND `end_time` > '" + currentTime + "' " + "AND `start_time` < '" + currentTime + "'";
		return daoMapper.selectList(sql);
	}

	public int getDeviceProductType(String deviceSn) {
		String sql = "SELECT `product_type` FROM `TDevice` WHERE `device_sn`='" + deviceSn + "'";
		Map<String, Object> map = daoMapper.select(sql);
		if (map != null && !map.isEmpty()) {
			return map.get("product_type") == null ? 0 : Integer.parseInt(map.get("product_type").toString());
		} else {
			return 0;
		}
	}

	public List<Map<String, Object>> getDevicePhoto(String deviceSn) {
		String sql = "select id as devicePhotoID,url,url_thumbnail AS thumbnailUrl,create_time from device_photo where device_sn='"
				+ deviceSn + "' ORDER BY create_time ASC";
		List<Map<String, Object>> remindList = daoMapper.selectList(sql);
		return remindList;
	}

	public boolean deleteDevicePhoto(String remindID) {
		String sql = "DELETE FROM device_photo WHERE id IN (" + remindID + ")";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean setGeoFenceMulti(String deviceID, String userID, String lat, String lng, String radius,
			String defencename, String defencestatus, String isOut) {
		String sql = "INSERT INTO TAreaInfo(lat, lng, radius, did, uid, enabled, create_time, defencename, is_out, type) "
				+ "VALUES('" + lat + "', '" + lng + "', '" + radius + "', '" + deviceID + "', '" + userID + "', '"
				+ defencestatus + "', UTC_TIMESTAMP(), '" + defencename + "', '" + isOut + "', 1)";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

		}
		return false;
	}

	public boolean updateGeoFenceMulti(String areaid, String deviceID, String userID, String lat, String lng,
			String radius, String defencename, String defencestatus, String isOut) {
		String sql = "UPDATE TAreaInfo SET lat = '" + lat + "', lng = '" + lng + "', radius = '" + radius + "', uid = '"
				+ userID + "', enabled = '" + defencestatus + "',create_time = UTC_TIMESTAMP(),defencename = '"
				+ defencename + "',is_out='" + isOut + "' WHERE did = '" + deviceID + "' AND areaid = '" + areaid + "'";
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			LogManager.error("error:" + e.getMessage());

		}
		return false;
	}

	public Map<String, Object> getDeviceContactByID(String id) {
		String sql = "SELECT `id` as `index`,`nickname`,`phone_number` as `phoneNum`, device_sn, CONCAT(nickname,\":\",phone_number) AS `contact`, `photo` FROM `device_contact` WHERE id = "
				+ id;
		return daoMapper.select(sql);
	}

	public List<Map<String, Object>> getDeviceContactToDevice(String deviceSn) {
		String sql = "SELECT id,device_sn,CONCAT(nickname,\":\",phone_number) AS contact, nickname, phone_number, photo FROM `device_contact` WHERE device_sn = '"
				+ deviceSn + "' ORDER BY `id` ASC";
		return daoMapper.selectList(sql);
	}

	public List<Map<String, Object>> getDeviceContact(String deviceSn) {
		String sql = "SELECT `id` as `index`, `nickname`, `phone_number` as `phoneNum`, `photo` FROM `device_contact` WHERE device_sn = '"
				+ deviceSn + "' ORDER BY `id` ASC";
		return daoMapper.selectList(sql);
	}

	public Map<String, Object> getDeviceContactLast(String deviceSn) {
		String sql = "SELECT `id` as `index`, `nickname`, `phone_number` as `phoneNum`, `photo` FROM `device_contact` WHERE device_sn = '"
				+ deviceSn + "' ORDER BY `id` Desc LIMIT 0,1";
		return daoMapper.select(sql);
	}

	public boolean addDeviceContact(String deviceSn, String nickname, String phoneNum, String photo) {
		String sqlinsert = "INSERT INTO `device_contact`(`device_sn`, `nickname`, `phone_number`, `photo`) "
				+ "VALUES('" + deviceSn + "', '" + nickname + "', '" + phoneNum + "', '" + photo + "')";

		try {
			return daoMapper.modify(sqlinsert) > 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean modifyDeviceContact(String index, String nickname, String phoneNum, String photo) {

		String sql = "UPDATE `device_contact` SET `nickname` = '" + nickname + "', `phone_number` = '" + phoneNum
				+ "', photo = '" + photo + "' WHERE  `id` = " + index;
		try {
			return daoMapper.modify(sql) > 0 ? true : false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	public boolean deleteDeviceContact(String index) {
		String sql = "delete from device_contact where id = " + index;
		try {
			return daoMapper.modify(sql) > 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public List<Map<String, Object>> getDeviceSteps(String deviceSn, String startDate, String endDate) {
		String sql = "select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate  from device_step "
				+ "where device_sn='" + deviceSn + "' and DATE(create_date)>=DATE('" + startDate
				+ "') and DATE(create_date)<=DATE('" + endDate + "') order BY createDate ASC";
		List<Map<String, Object>> list = daoMapper.selectList(sql);
		if (list == null || list.size() == 0)
			return null;
		return list;
	}

	public Map<String, Object> getDeviceLastStep(String deviceSn) {
		String sql = "select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate from `device_step` where device_sn='"
				+ deviceSn + "' order by create_date desc limit 1";
		Map<String, Object> map = daoMapper.select(sql);
		if (map == null || map.isEmpty())
			return null;

		return map;
	}
}