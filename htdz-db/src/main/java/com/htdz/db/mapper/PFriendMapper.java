package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.PFriend;

public interface PFriendMapper {
	@Insert("INSERT INTO `PFriend`(`deviceSn`,`pdeviceSn`) VALUES (#{0},#{1})")
	public int Add(String deviceSn,String pdeviceSn);
	
	@Select("SELECT * FROM `PFriend` WHERE `deviceSn`=#{0}")
	public List<PFriend> getFriendlist(String deviceSn);
	
	@Select("select count(*) from PFriend where (deviceSn=#{0} and pdeviceSn=#{1}) or (deviceSn=#{1} and pdeviceSn=#{0})")
	public int isFriend(String deviceSn, String pdeviceSn);
	
	@Delete("DELETE FROM `PFriend` WHERE `deviceSn`=#{0}")
	public int deleteAllFriend(String deviceSn);
	
	@Delete("delete from PFriend where (deviceSn=#{0} and pdeviceSn=#{1}) or (deviceSn=#{1} and pdeviceSn=#{0})")
	public int removeFriend(String deviceSn, String pdeviceSn);
}
