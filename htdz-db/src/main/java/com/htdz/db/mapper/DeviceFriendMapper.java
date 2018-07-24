package com.htdz.db.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface DeviceFriendMapper {
	@Select("select count(*) from device_friend where (dsn1=#{0} and dsn2=#{1}) or (dsn1=#{1} and dsn2=#{0})")
	public int isFriend(String dsn1, String dsn2);
	
	@Insert("insert into device_friend(dsn1, dsn2) values(#{0}, #{1})")
	public int addFriend(String dsn1, String dsn2);
	
	@Delete("delete from device_friend where (dsn1=#{0} and dsn2=#{1}) or (dsn1=#{1} and dsn2=#{0})")
	public int removeFriend(String dsn1, String dsn2);
}
