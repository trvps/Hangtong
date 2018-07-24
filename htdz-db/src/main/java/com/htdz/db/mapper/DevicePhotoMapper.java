package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.DevicePhoto;

public interface DevicePhotoMapper {

	@Select("select * from device_photo where device_sn=#{0}")
	public DevicePhoto select(String deviceSn);

	@Select("select * from device_photo")
	public List<DevicePhoto> selectAll();

	@Insert("insert into device_photo(device_sn, url, url_thumbnail,create_time) values(#{deviceSn}, #{url}, #{urlThumbnail}, #{createTime})")
	@Options(useGeneratedKeys = true)
	public int add(DevicePhoto devicePhoto);

	@Update("update device_photo set url=#{url} where device_sn=#{deviceSn}")
	public int modify(DevicePhoto devicePhoto);

}
