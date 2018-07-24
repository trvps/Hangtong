package com.htdz.db.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Select;
import com.htdz.def.dbmodel.TDeviceUser;

public interface TDeviceUserMapper {
	@Select("SELECT * FROM `TDeviceUser` WHERE `uid`=#{0}")
	public List<TDeviceUser> getByUid(Integer uid);
	
	@Select("SELECT * FROM `TDeviceUser` WHERE `did`=#{0}")
	public List<TDeviceUser> getByDid(Integer did);
	
	@Select("SELECT A.* FROM `TDeviceUser` AS A INNER JOIN `TDevice` AS B ON A.`did`=B.`id` AND A.`is_super_user`=1 WHERE B.`device_sn`=#{0}")
	public List<TDeviceUser> getByDeviceSn(String DeviceSn);
}
