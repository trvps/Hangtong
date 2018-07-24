package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.TareaInfo;

public interface TAreaInfoMapper {
	@Select("SELECT areaid,lat,lng,radius,did,uid,enabled,create_time as createTime,defencename,type,is_out as isOut FROM `TAreaInfo` WHERE `did`=#{0}")
	public List<TareaInfo> getAreaListById(Integer did);
}
